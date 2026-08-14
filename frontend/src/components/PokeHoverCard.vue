<template>
  <span
    class="inline-flex"
    :class="wrapClass"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @mousemove="onMove"
  >
    <slot />
    <Teleport to="body">
      <div
        v-if="visible"
        ref="cardEl"
        class="poke-tip fixed z-[3000] pointer-events-none"
        :style="cardStyle"
        @mouseenter="onEnter"
      >
        <!-- ===== 精灵详情模式 ===== -->
        <template v-if="pokemon">
          <div class="poke-tip-head">
            <img
              :src="monSprite"
              :alt="pokemon.name || pokemon.name_en"
              class="poke-tip-sprite"
              @error="onSpriteError"
            >
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-1.5">
                <span class="truncate text-[13px] font-extrabold text-slate-900">{{ pokemon.name || pokemon.name_en }}</span>
                <span class="shrink-0 rounded bg-slate-800 px-1 py-px text-[10px] font-bold text-white">Lv.{{ pokemon.level || 50 }}</span>
              </div>
              <div class="mt-1 flex flex-wrap gap-1">
                <span
                  v-for="t in pokemon.types || []"
                  :key="t.type_id || t.id"
                  class="tip-type"
                  :style="{ backgroundColor: typeIdToColor(t.type_id || t.id) }"
                >{{ t.name || t.name_en }}</span>
              </div>
            </div>
          </div>
          <div class="px-3 py-2 text-[11px] leading-5 text-slate-600">
            <div class="flex flex-wrap gap-x-3 gap-y-0.5">
              <span><span class="text-slate-400">{{ tr('特性', 'Ability') }}:</span> <b class="text-slate-800">{{ abilityName }}</b></span>
              <span><span class="text-slate-400">{{ tr('道具', 'Item') }}:</span> <b class="text-slate-800">{{ itemName }}</b></span>
              <span v-if="natureName !== '-'"><span class="text-slate-400">{{ tr('性格', 'Nature') }}:</span> <b class="text-slate-800">{{ natureName }}</b></span>
            </div>
            <!-- 特性描述 -->
            <div
              v-if="abilityDescription"
              class="mt-1.5 rounded-lg bg-indigo-50/70 border border-indigo-100 px-2.5 py-1.5 text-[10.5px] leading-4.5 text-indigo-800"
            >
              <span class="font-bold text-indigo-500">{{ tr('特性效果', 'Ability effect') }}：</span>{{ abilityDescription }}
            </div>
            <!-- 道具效果 -->
            <div
              v-if="itemEffect"
              class="mt-1.5 rounded-lg bg-emerald-50/70 border border-emerald-100 px-2.5 py-1.5 text-[10.5px] leading-4.5 text-emerald-800"
            >
              <span class="font-bold text-emerald-600">{{ tr('道具效果', 'Item effect') }}：</span>{{ itemEffect }}
            </div>
          </div>
          <div class="px-3 pb-2">
            <div
              v-for="stat in statRows"
              :key="stat.key"
              class="flex items-center gap-1.5 py-px"
            >
              <span class="w-7 shrink-0 text-right text-[10px] font-bold text-slate-400">{{ stat.label }}</span>
              <span class="h-1 flex-1 overflow-hidden rounded-full bg-slate-200">
                <span
                  class="block h-full rounded-full"
                  :style="{ width: statBarWidth(stat.base), backgroundColor: statBarColor(stat.base) }"
                />
              </span>
              <span class="w-6 shrink-0 text-right text-[10px] font-bold text-slate-600">{{ stat.actual }}</span>
            </div>
          </div>
          <div
            v-if="(pokemon.moves || []).length"
            class="border-t border-slate-100 px-3 py-2"
          >
            <div class="mb-1 text-[9px] font-black uppercase tracking-widest text-slate-400">
              {{ tr('招式', 'Moves') }}
            </div>
            <div class="grid grid-cols-1 gap-1">
              <div
                v-for="move in pokemon.moves.slice(0, 4)"
                :key="move.name_en || move.name"
                class="rounded-md border border-slate-100 bg-slate-50/60 px-1.5 py-1"
              >
                <div class="flex items-center gap-1 text-[10px]">
                  <span
                    class="h-1.5 w-1.5 shrink-0 rounded-full"
                    :style="{ backgroundColor: typeIdToColor(move.type_id) }"
                  />
                  <span class="truncate font-semibold text-slate-700">{{ move.name || move.name_en }}</span>
                  <span class="ml-auto shrink-0 text-slate-400">{{ move.power || '—' }}</span>
                </div>
                <div
                  v-if="moveEffectText(move)"
                  class="mt-0.5 pl-2.5 text-[9.5px] leading-4 text-slate-500"
                >{{ moveEffectText(move) }}</div>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== 招式详情模式 ===== -->
        <template v-else-if="move">
          <div class="poke-tip-head">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-1.5">
                <span class="truncate text-[13px] font-extrabold text-slate-900">{{ move.name || move.name_en }}</span>
                <span
                  class="shrink-0 rounded px-1.5 py-px text-[10px] font-bold text-white"
                  :style="{ backgroundColor: moveTypeColor }"
                >{{ moveTypeName }}</span>
              </div>
              <div class="mt-1 flex items-center gap-1.5 text-[11px] text-slate-500">
                <span
                  class="rounded px-1 py-px text-[10px] font-bold"
                  :class="damageClassClass"
                >{{ damageClassLabel }}</span>
                <span>{{ targetText }}</span>
              </div>
            </div>
          </div>
          <div class="px-3 py-2">
            <div class="grid grid-cols-4 gap-2 text-center">
              <div>
                <div class="text-[9px] font-bold uppercase text-slate-400">{{ tr('威力', 'Pwr') }}</div>
                <div class="text-[13px] font-extrabold text-slate-800">{{ move.power || '—' }}</div>
              </div>
              <div>
                <div class="text-[9px] font-bold uppercase text-slate-400">{{ tr('命中', 'Acc') }}</div>
                <div class="text-[13px] font-extrabold text-slate-800">{{ move.accuracy ?? '—' }}<span v-if="move.accuracy != null">%</span></div>
              </div>
              <div>
                <div class="text-[9px] font-bold uppercase text-slate-400">PP</div>
                <div class="text-[13px] font-extrabold text-slate-800">{{ move.pp ?? '—' }}</div>
              </div>
              <div>
                <div class="text-[9px] font-bold uppercase text-slate-400">{{ tr('先制', 'Pri') }}</div>
                <div class="text-[13px] font-extrabold" :class="move.priority > 0 ? 'text-blue-600' : move.priority < 0 ? 'text-rose-500' : 'text-slate-800'">
                  {{ move.priority > 0 ? '+' : '' }}{{ move.priority || 0 }}
                </div>
              </div>
            </div>
            <div
              v-if="move.effect_short || move.effectShort || move.description"
              class="mt-2 border-t border-slate-100 pt-2 text-[11px] leading-5 text-slate-600"
            >
              {{ move.effect_short || move.effectShort || move.description }}
            </div>
            <div
              v-if="effectChanceText"
              class="mt-1 text-[10px] font-semibold text-indigo-600"
            >
              {{ effectChanceText }}
            </div>
          </div>
        </template>
      </div>
    </Teleport>
  </span>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'
import { typeColor, typeNameZh, typeNameEn } from '../services/typeChart'
import { itemEffectZh } from '../services/itemEffectsZh'

const { translate: tr } = useLocale()

const props = defineProps({
  /** 精灵对象（与 move 二选一） */
  pokemon: { type: Object, default: null },
  /** 招式对象（与 pokemon 二选一） */
  move: { type: Object, default: null },
  /** 外层容器类名（用于控制宽度/布局） */
  wrapClass: { type: String, default: '' }
})

const visible = ref(false)
const cardEl = ref(null)
const pos = ref({ x: 0, y: 0 })
const flip = ref({ x: false, y: false })
const cardSize = ref({ w: 300, h: 320 })
const CARD_W = 300

function onEnter() {
  visible.value = true
  nextTick(() => {
    if (cardEl.value) {
      const r = cardEl.value.getBoundingClientRect()
      cardSize.value = { w: r.width || CARD_W, h: r.height || 320 }
      updateFlip()
    }
  })
}

function onLeave() {
  visible.value = false
}

function onMove(e) {
  pos.value = { x: e.clientX, y: e.clientY }
  updateFlip()
}

function updateFlip() {
  const { x, y } = pos.value
  const { w, h } = cardSize.value
  flip.value.x = x + w + 14 > window.innerWidth - 8
  flip.value.y = y + h + 14 > window.innerHeight - 8
}

const cardStyle = computed(() => {
  const { x, y } = pos.value
  const { w, h } = cardSize.value
  const left = flip.value.x ? x - w - 12 : x + 14
  const top = flip.value.y ? y - h - 12 : y + 14
  return {
    left: `${Math.max(4, Math.min(left, window.innerWidth - w - 4))}px`,
    top: `${Math.max(4, Math.min(top, window.innerHeight - 40))}px`,
    width: `${w}px`
  }
})

// ===== 精灵模式 =====
const monSprite = computed(() => {
  const id = props.pokemon?.form_id || props.pokemon?.species_id || props.pokemon?.pokemon_id || props.pokemon?.id
  return id ? sprites.pokemon(id) : sprites.default
})

function onSpriteError(event) {
  const img = event.target
  const src = img?.getAttribute('src') || ''
  const id = props.pokemon?.form_id || props.pokemon?.species_id || props.pokemon?.pokemon_id || props.pokemon?.id
  if (src.includes('/api/pokedex/images')) {
    img.src = id ? sprites.fallbackPokemon(id) : sprites.default
  } else {
    img.src = sprites.default
  }
}

const abilityName = computed(() => {
  const ab = props.pokemon?.ability
  if (!ab) return tr('无特性', '—')
  if (typeof ab === 'string') return ab
  return ab.name || ab.name_en || tr('无特性', '—')
})

const itemName = computed(() => {
  const item = props.pokemon?.heldItem
  if (!item) return tr('无道具', 'None')
  // 优先显示 heldItemInfo 里的中文名
  const info = props.pokemon?.heldItemInfo
  if (info && typeof info === 'object' && info.name) return info.name
  if (typeof item === 'object' && item.name) return item.name
  return String(item).split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
})

/** 特性描述（优先中文，回退英文） */
const abilityDescription = computed(() => {
  const ab = props.pokemon?.ability
  if (!ab || typeof ab === 'string') return ''
  return ab.description || ab.description_en || ''
})

/** 道具效果（heldItemInfo 对象，引擎兼容的 heldItem 字符串不变） */
const itemEffect = computed(() => {
  const info = props.pokemon?.heldItemInfo
  if (info && typeof info === 'object') {
    const en = info.effect_short || info.description || ''
    return itemEffectZh(en, info.name_en)
  }
  return ''
})

/** 招式效果文本（优先中文 description，回退英文 effect_short） */
function moveEffectText(move) {
  if (!move) return ''
  const zh = move.description || ''
  if (/[\u4e00-\u9fff]/.test(zh)) return zh
  return move.effect_short || zh || ''
}

const NATURE_EFFECTS = {
  lonely: { up: 'attack', down: 'defense' },
  brave: { up: 'attack', down: 'speed' },
  adamant: { up: 'attack', down: 'specialAttack' },
  naughty: { up: 'attack', down: 'specialDefense' },
  bold: { up: 'defense', down: 'attack' },
  relaxed: { up: 'defense', down: 'speed' },
  impish: { up: 'defense', down: 'specialAttack' },
  lax: { up: 'defense', down: 'specialDefense' },
  timid: { up: 'speed', down: 'attack' },
  hasty: { up: 'speed', down: 'defense' },
  jolly: { up: 'speed', down: 'specialAttack' },
  naive: { up: 'speed', down: 'specialDefense' },
  modest: { up: 'specialAttack', down: 'attack' },
  mild: { up: 'specialAttack', down: 'defense' },
  quiet: { up: 'specialAttack', down: 'speed' },
  rash: { up: 'specialAttack', down: 'specialDefense' },
  calm: { up: 'specialDefense', down: 'attack' },
  gentle: { up: 'specialDefense', down: 'defense' },
  sassy: { up: 'specialDefense', down: 'speed' },
  careful: { up: 'specialDefense', down: 'specialAttack' },
  quirky: null, serious: null, hardy: null, docile: null, bashful: null
}

const STAT_LABELS = {
  hp: 'HP', attack: 'Atk', defense: 'Def',
  specialAttack: 'SpA', specialDefense: 'SpD', speed: 'Spe'
}

const natureName = computed(() => {
  const n = props.pokemon?.nature
  if (!n) return '-'
  const key = String(n).toLowerCase()
  const eff = NATURE_EFFECTS[key]
  if (!eff) return key.charAt(0).toUpperCase() + key.slice(1)
  return `${key.charAt(0).toUpperCase() + key.slice(1)} (+${STAT_LABELS[eff.up] || eff.up}, -${STAT_LABELS[eff.down] || eff.down})`
})

const statRows = computed(() => {
  const stats = props.pokemon?.stats || {}
  return Object.keys(STAT_LABELS).map(key => ({
    key,
    label: STAT_LABELS[key],
    base: stats[key] || 0,
    actual: stats[key] || 0
  }))
})

function statBarWidth(base) {
  return `${Math.min(100, (base / 255) * 100)}%`
}

function statBarColor(base) {
  if (base >= 150) return '#6366f1'
  if (base >= 120) return '#3b82f6'
  if (base >= 90) return '#22c55e'
  if (base >= 60) return '#eab308'
  return '#ef4444'
}

// ===== 招式模式 =====
function typeIdToColor(typeId) {
  return typeColor(typeId)
}

const moveTypeColor = computed(() => typeIdToColor(props.move?.type_id))
const moveTypeName = computed(() => {
  const t = props.move?.typeName
  if (t) return t
  const id = props.move?.type_id
  if (id != null) return tr(typeNameZh(id), typeNameEn(id))
  return tr('未知', 'Unknown')
})

const damageClassLabel = computed(() => {
  const id = props.move?.damage_class_id
  if (id === 2) return tr('物理', 'Phys')
  if (id === 3) return tr('特殊', 'Spec')
  if (id === 1) return tr('变化', 'Stat')
  return ''
})

const damageClassClass = computed(() => {
  const id = props.move?.damage_class_id
  if (id === 2) return 'bg-rose-100 text-rose-700'
  if (id === 3) return 'bg-blue-100 text-blue-700'
  if (id === 1) return 'bg-purple-100 text-purple-700'
  return 'bg-slate-100 text-slate-600'
})

const TARGET_TEXTS = {
  7: tr('目标：自身', 'Target: self'),
  8: tr('目标：随机对手', 'Target: random foe'),
  9: tr('目标：其他在场宝可梦', 'Target: other active'),
  10: tr('目标：单体', 'Target: single foe'),
  11: tr('目标：对手全体', 'Target: all foes'),
  13: tr('目标：自身与队友', 'Target: self and ally'),
  14: tr('目标：全场', 'Target: all')
}

const targetText = computed(() => {
  return TARGET_TEXTS[Number(props.move?.target_id || 10)] || tr('目标：单体', 'Target: single foe')
})

const effectChanceText = computed(() => {
  const m = props.move
  const chance = m?.effect_chance
  if (!chance) return ''
  return tr('附加效果几率 {chance}%', 'Effect chance {chance}%', '', { chance })
})
</script>

<style scoped>
.poke-tip {
  padding: 0;
  border-radius: 0.9rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 16px 40px -12px rgba(15, 23, 42, 0.35);
  overflow: hidden;
  transition: opacity 0.12s ease;
}

.poke-tip-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.85rem;
  background: linear-gradient(160deg, #f8fafc 0%, #eef2ff 100%);
  border-bottom: 1px solid #eef2f7;
}

.poke-tip-sprite {
  width: 64px;
  height: 64px;
  object-fit: contain;
  image-rendering: pixelated;
  flex-shrink: 0;
}

.tip-type {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  border-radius: 0.3rem;
  color: white;
  font-size: 0.6rem;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(0,0,0,0.25);
}
</style>
