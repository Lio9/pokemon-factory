<template>
  <el-dialog
    :model-value="visible"
    :title="pokemon?.name || pokemon?.name_en || tr('宝可梦详情', 'Pokemon Detail')"
    width="520px"
    :close-on-click-modal="true"
    destroy-on-close
    class="detail-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <!-- 顶部属性标签固定在滚动区域外 -->
    <template #header>
      <div class="flex items-center gap-3">
        <span class="text-lg font-bold">{{ pokemon?.name || pokemon?.name_en || tr('宝可梦详情', 'Pokemon Detail') }}</span>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-for="t in pokemon?.types || []"
            :key="t.type_id"
            class="type-badge"
            :style="{ backgroundColor: typeIdToColor(t.type_id) }"
          >
            {{ t.name || t.name_en || `属性${t.type_id}` }}
          </span>
          <span
            v-if="pokemon?.teraType"
            class="type-badge"
            :style="{ backgroundColor: typeIdToColor(pokemon.teraType?.type_id) }"
          >
            {{ tr('太晶', 'Tera') }}: {{ pokemon.teraType.name || pokemon.teraType.name_en }}
          </span>
        </div>
      </div>
    </template>
    <div
      v-if="pokemon"
      class="dialog-body space-y-5"
    >
      <!-- 核心配置 -->
      <div class="grid grid-cols-2 gap-3">
        <div class="stat-card">
          <div class="stat-label">
            {{ tr('特性', 'Ability') }}
          </div>
          <div class="stat-value">
            {{ abilityName }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">
            {{ tr('道具', 'Item') }}
          </div>
          <div class="stat-value">
            {{ formatItemName(pokemon.heldItem) }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">
            {{ tr('性格', 'Nature') }}
          </div>
          <div class="stat-value">
            {{ formatNature(pokemon.nature) }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">
            {{ tr('等级', 'Level') }}
          </div>
          <div class="stat-value">
            Lv.{{ pokemon.level || 50 }}
          </div>
        </div>
      </div>

      <!-- 种族值 -->
      <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          {{ tr('种族值 / 实际值', 'Base Stats / Actual') }}
        </div>
        <div class="space-y-2">
          <div
            v-for="stat in statRows"
            :key="stat.key"
            class="flex items-center gap-3"
          >
            <div class="w-8 text-xs font-semibold text-slate-500 text-right">
              {{ stat.label }}
            </div>
            <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :style="{ width: statBarWidth(stat.base), backgroundColor: statBarColor(stat.base) }"
              />
            </div>
            <div class="shrink-0 w-20 text-xs text-right whitespace-nowrap">
              <span class="font-bold text-slate-700">{{ stat.actual }}</span>
              <span class="text-slate-400 ml-0.5">({{ stat.base }})</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 努力值 -->
      <div
        v-if="pokemon.evSpread"
        class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4"
      >
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          {{ tr('努力值分配', 'EV Spread') }}
        </div>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="ev in evRows"
            :key="ev.key"
            class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium"
            :class="ev.value > 0 ? 'bg-indigo-50 text-indigo-700' : 'bg-slate-100 text-slate-400'"
          >
            {{ ev.label }} {{ ev.value }}
          </span>
        </div>
      </div>

      <!-- 招式列表 -->
      <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          {{ tr('携带招式', 'Moves') }}
        </div>
        <div class="grid grid-cols-2 gap-2">
          <div
            v-for="move in pokemon.moves || []"
            :key="move.name_en || move.name"
            class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2"
          >
            <span
              class="shrink-0 w-3 h-3 rounded-full"
              :style="{ backgroundColor: typeIdToColor(move.type_id) }"
            />
            <div class="min-w-0 flex-1">
              <div class="text-xs font-semibold text-slate-800 truncate">
                {{ move.name || move.name_en }}
              </div>
              <div class="text-[10px] text-slate-400">
                {{ tr('威力', 'Pwr') }} {{ move.power || 0 }} · {{ tr('命中', 'Acc') }} {{ move.accuracy ?? '-' }}%
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const props = defineProps({
  visible: { type: Boolean, default: false },
  pokemon: { type: Object, default: null }
})

defineEmits(['update:visible'])

const TYPE_COLORS_BY_ID = {
  1: '#A8A77A', 2: '#C62828', 3: '#456AE4', 4: '#A040A0', 5: '#F7D02C',
  6: '#B69F37', 7: '#A6B91A', 8: '#74C8E2', 9: '#B7B7CE', 10: '#EE8130',
  11: '#6390F0', 12: '#7AC74C', 13: '#F95587', 14: '#A98FF3', 15: '#98D8D8',
  16: '#705746', 17: '#6F35FC', 18: '#D685AD'
}

function typeIdToColor(typeId) {
  return TYPE_COLORS_BY_ID[typeId] || '#777'
}

const abilityName = computed(() => {
  const ab = props.pokemon?.ability
  if (!ab) return '-'
  if (typeof ab === 'string') return ab
  return ab.name || ab.name_en || '-'
})

function formatItemName(itemId) {
  if (!itemId) return tr('无道具', 'None')
  return itemId.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
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
  quirky: null, serious: null, Hardy: null, Docile: null, Bashful: null
}

const STAT_LABELS = {
  hp: 'HP', attack: 'Atk', defense: 'Def',
  specialAttack: 'SpA', specialDefense: 'SpD', speed: 'Spe'
}

const EV_LABELS = {
  hp: 'HP', atk: 'Atk', def: 'Def',
  spa: 'SpA', spd: 'SpD', spe: 'Spe'
}

function formatNature(nature) {
  if (!nature) return '-'
  const key = String(nature).toLowerCase()
  const effect = NATURE_EFFECTS[key]
  if (!effect) return key.charAt(0).toUpperCase() + key.slice(1)
  const upLabel = STAT_LABELS[effect.up] || effect.up
  const downLabel = STAT_LABELS[effect.down] || effect.down
  const name = key.charAt(0).toUpperCase() + key.slice(1)
  return `${name} (+${upLabel}, -${downLabel})`
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

const evRows = computed(() => {
  const ev = props.pokemon?.evSpread || {}
  return Object.keys(EV_LABELS).map(key => ({
    key,
    label: EV_LABELS[key],
    value: ev[key] || ev[EV_LABELS[key].toLowerCase()] || 0
  }))
})
</script>

<style scoped>
.detail-dialog :deep(.el-dialog) {
  border-radius: 1.5rem !important;
  max-height: 85vh;
  margin: 5vh auto;
}
.detail-dialog :deep(.el-dialog__header) {
  padding: 1.25rem 1.5rem;
  margin-right: 0;
  border-bottom: 1px solid #f1f5f9;
}
.detail-dialog :deep(.el-dialog__body) {
  padding: 1.25rem 1.5rem 1.5rem;
  max-height: calc(85vh - 80px);
  overflow-y: auto;
}
.detail-dialog :deep(.el-dialog__title) { font-weight: 700; font-size: 1.125rem; }
.detail-dialog :deep(.el-dialog__headerbtn) { top: 1.25rem; right: 1.25rem; }

.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  border-radius: 0.375rem;
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.stat-card {
  @apply rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-3;
}
.stat-label {
  @apply text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1;
}
.stat-value {
  @apply text-sm font-bold text-slate-800;
}
</style>
