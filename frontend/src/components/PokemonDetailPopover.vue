<template>
  <el-dialog
    :model-value="visible"
    :title="null"
    width="400px"
    :close-on-click-modal="true"
    :lock-scroll="false"
    :show-close="true"
    append-to-body
    destroy-on-close
    class="poke-detail-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div
      v-if="pokemon"
      class="poke-detail"
    >
      <!-- ===== Showdown 风格头部：精灵图 + 名字/等级 ===== -->
      <div class="detail-hero">
        <div class="relative shrink-0">
          <img
            :src="detailSprite"
            :alt="pokemon.name || pokemon.name_en"
            class="detail-sprite"
            @error="onSpriteError"
          >
          <span
            v-if="pokemon.teraType"
            class="tera-chip"
            :style="{ backgroundColor: typeIdToColor(pokemon.teraType?.type_id) }"
          >{{ tr('太晶', 'Tera') }}</span>
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2">
            <h3 class="truncate text-lg font-extrabold text-slate-900">
              {{ pokemon.name || pokemon.name_en }}
            </h3>
            <span class="shrink-0 rounded-md bg-slate-800 px-1.5 py-0.5 text-[11px] font-bold text-white">
              Lv.{{ pokemon.level || 50 }}
            </span>
          </div>
          <div class="mt-1.5 flex flex-wrap gap-1">
            <span
              v-for="t in pokemon.types || []"
              :key="t.type_id || t.id"
              class="type-badge"
              :style="{ backgroundColor: typeIdToColor(t.type_id || t.id) }"
            >
              {{ t.name || t.name_en || `属性${t.type_id}` }}
            </span>
          </div>
          <div class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600">
            <span class="flex items-center gap-1">
              <span class="text-slate-400">{{ tr('特性', 'Ability') }}:</span>
              <span class="font-semibold text-slate-800">{{ abilityName }}</span>
            </span>
            <span class="flex items-center gap-1">
              <span class="text-slate-400">{{ tr('道具', 'Item') }}:</span>
              <span class="font-semibold text-slate-800">{{ itemName }}</span>
            </span>
          </div>
          <!-- 特性效果 -->
          <div
            v-if="abilityDescription"
            class="mt-2 rounded-lg border border-indigo-100 bg-indigo-50/70 px-2.5 py-1.5 text-[11px] leading-5 text-indigo-800"
          >
            <span class="font-bold text-indigo-500">{{ tr('特性效果', 'Ability effect') }}：</span>{{ abilityDescription }}
          </div>
          <!-- 道具效果 -->
          <div
            v-if="itemEffect"
            class="mt-1.5 rounded-lg border border-emerald-100 bg-emerald-50/70 px-2.5 py-1.5 text-[11px] leading-5 text-emerald-800"
          >
            <span class="font-bold text-emerald-600">{{ tr('道具效果', 'Item effect') }}：</span>{{ itemEffect }}
          </div>
        </div>
      </div>

      <!-- ===== 种族值条（Showdown 风格） ===== -->
      <div class="detail-section">
        <div class="section-title">
          {{ tr('种族值', 'Base Stats') }}
        </div>
        <div class="space-y-1.5">
          <div
            v-for="stat in statRows"
            :key="stat.key"
            class="stat-row"
          >
            <span class="stat-label">{{ stat.label }}</span>
            <span class="stat-bar">
              <span
                class="stat-bar-fill"
                :style="{ width: statBarWidth(stat.base), backgroundColor: statBarColor(stat.base) }"
              />
            </span>
            <span class="stat-num">{{ stat.actual }}</span>
          </div>
        </div>
      </div>

      <!-- ===== 努力值 ===== -->
      <div
        v-if="hasEvs"
        class="detail-section"
      >
        <div class="section-title">
          {{ tr('努力值', 'EVs') }}
        </div>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-for="ev in evRows"
            :key="ev.key"
            class="ev-chip"
            :class="ev.value > 0 ? 'ev-chip-on' : 'ev-chip-off'"
          >
            {{ ev.label }} {{ ev.value }}
          </span>
        </div>
      </div>

      <!-- ===== 招式列表（Showdown 风格，紧凑） ===== -->
      <div class="detail-section">
        <div class="section-title">
          {{ tr('招式', 'Moves') }}
        </div>
        <div
          v-if="(pokemon.moves || []).length"
          class="grid grid-cols-1 gap-1.5"
        >
          <div
            v-for="move in pokemon.moves"
            :key="move.name_en || move.name"
            class="move-row"
          >
            <span
              class="move-type-dot"
              :style="{ backgroundColor: typeIdToColor(move.type_id) }"
            />
            <span class="min-w-0 flex-1 truncate text-xs font-semibold text-slate-800">
              {{ move.name || move.name_en }}
            </span>
            <span class="shrink-0 text-[11px] font-medium text-slate-500">
              {{ moveCategory(move) }}
            </span>
            <span class="shrink-0 w-14 text-right text-[11px] font-medium text-slate-500">
              <span class="font-bold text-slate-700">{{ move.power || '—' }}</span>
              / {{ move.accuracy ?? '—' }}
            </span>
            <span
              v-if="moveEffectText(move)"
              class="col-span-4 mt-0.5 pl-2.5 text-[10.5px] leading-4 text-slate-500"
            >{{ moveEffectText(move) }}</span>
          </div>
        </div>
        <div
          v-else
          class="rounded-lg bg-slate-50 px-3 py-4 text-center text-xs text-slate-400"
        >
          {{ tr('未配置招式', 'No moves') }}
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'
import { itemEffectZh } from '../services/itemEffectsZh'
import { typeColor } from '../services/typeChart'

const { translate: tr } = useLocale()

const props = defineProps({
  visible: { type: Boolean, default: false },
  pokemon: { type: Object, default: null }
})

defineEmits(['update:visible'])

function typeIdToColor(typeId) {
  return typeColor(typeId)
}

const abilityName = computed(() => {
  const ab = props.pokemon?.ability
  if (!ab) return tr('无特性', '—')
  if (typeof ab === 'string') return ab
  return ab.name || ab.name_en || tr('无特性', '—')
})

/** 特性描述（优先中文，回退英文） */
const abilityDescription = computed(() => {
  const ab = props.pokemon?.ability
  if (!ab || typeof ab === 'string') return ''
  return ab.description || ab.description_en || ''
})

/** 道具名（优先 heldItemInfo 中文名） */
const itemName = computed(() => {
  const info = props.pokemon?.heldItemInfo
  if (info && typeof info === 'object' && info.name) return info.name
  return formatItemName(props.pokemon?.heldItem)
})

/** 道具效果（heldItemInfo 对象） */
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

function formatItemName(itemId) {
  if (!itemId) return tr('无道具', 'None')
  return itemId.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
}

const detailSprite = computed(() => {
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

const STAT_LABELS = {
  hp: 'HP', attack: 'Atk', defense: 'Def',
  specialAttack: 'SpA', specialDefense: 'SpD', speed: 'Spe'
}

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

const EV_LABELS = {
  hp: 'HP', atk: 'Atk', def: 'Def',
  spa: 'SpA', spd: 'SpD', spe: 'Spe'
}

const evRows = computed(() => {
  const ev = props.pokemon?.evSpread || {}
  return Object.keys(EV_LABELS).map(key => ({
    key,
    label: EV_LABELS[key],
    value: ev[key] || ev[EV_LABELS[key].toLowerCase()] || 0
  }))
})

const hasEvs = computed(() => evRows.value.some(e => e.value > 0))

const MOVE_CATEGORIES = {
  physical: tr('物理', 'Phys'),
  special: tr('特殊', 'Spec'),
  status: tr('变化', 'Stat')
}

function moveCategory(move) {
  if (!move) return ''
  if (move.categoryName) return move.categoryName
  const cat = String(move.damage_class_id || move.damageClassId || '')
  if (MOVE_CATEGORIES[cat]) return MOVE_CATEGORIES[cat]
  if (move.damageClass) return move.damageClass
  return ''
}
</script>

<style scoped>
/* ===== 弹窗外框：不锁滚动、紧凑高度 =====
   el-dialog 经 Teleport 渲染到 body，不携带本组件 scoped 的
   data-v 属性，因此必须用 :global() 才能命中。 */
:global(.poke-detail-dialog) {
  border-radius: 1rem !important;
  max-height: min(86vh, 680px);
  margin: 6vh auto !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 24px 64px -24px rgba(15, 23, 42, 0.4) !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
:global(.poke-detail-dialog .el-dialog__header) {
  display: none;
}
:global(.poke-detail-dialog .el-dialog__body) {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  -webkit-overflow-scrolling: touch;
}
:global(.poke-detail-dialog .el-dialog__headerbtn) {
  top: 0.75rem;
  right: 0.75rem;
  z-index: 10;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 9999px;
  border: 1px solid #e2e8f0;
  width: 26px;
  height: 26px;
}

/* ===== 内容区 ===== */
.poke-detail {
  max-height: calc(min(86vh, 680px) - 0px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.detail-hero {
  display: flex;
  gap: 1rem;
  padding: 1.25rem 1.25rem 1rem;
  background: linear-gradient(160deg, #f8fafc 0%, #eef2ff 100%);
  border-bottom: 1px solid #eef2f7;
  position: sticky;
  top: 0;
  z-index: 2;
}

.detail-sprite {
  width: 96px;
  height: 96px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 6px 12px rgba(15, 23, 42, 0.18));
}

.tera-chip {
  position: absolute;
  bottom: -2px;
  right: -4px;
  padding: 0.1rem 0.45rem;
  border-radius: 0.375rem;
  color: white;
  font-size: 0.6rem;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(0,0,0,0.25);
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}

.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.55rem;
  border-radius: 0.375rem;
  color: white;
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 2px rgba(0,0,0,0.25);
}

.detail-section {
  padding: 0.9rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}
.detail-section:last-child {
  border-bottom: none;
}

.section-title {
  margin-bottom: 0.6rem;
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #94a3b8;
}

/* 种族值行 */
.stat-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.stat-label {
  width: 2rem;
  flex-shrink: 0;
  text-align: right;
  font-size: 0.68rem;
  font-weight: 800;
  color: #64748b;
}
.stat-bar {
  position: relative;
  flex: 1;
  height: 0.5rem;
  border-radius: 9999px;
  background: #e2e8f0;
  overflow: hidden;
}
.stat-bar-fill {
  display: block;
  height: 100%;
  border-radius: 9999px;
  transition: width 0.3s ease;
}
.stat-num {
  width: 1.75rem;
  flex-shrink: 0;
  text-align: right;
  font-size: 0.72rem;
  font-weight: 800;
  color: #334155;
}

/* 努力值 */
.ev-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.55rem;
  border-radius: 0.5rem;
  font-size: 0.68rem;
  font-weight: 700;
}
.ev-chip-on {
  background: #eef2ff;
  color: #4f46e5;
  border: 1px solid #c7d2fe;
}
.ev-chip-off {
  background: #f8fafc;
  color: #cbd5e1;
  border: 1px solid #e2e8f0;
}

/* 招式行 */
.move-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.45rem 0.65rem;
  border-radius: 0.6rem;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.move-row:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}
.move-type-dot {
  width: 0.6rem;
  height: 0.6rem;
  flex-shrink: 0;
  border-radius: 9999px;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.7);
}
</style>
