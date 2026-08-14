<template>
  <button
    type="button"
    class="move-button group relative overflow-hidden rounded-xl border-2 px-3 py-2.5 text-left transition-all duration-150"
    :class="[
      selected ? 'border-indigo-500 bg-indigo-50 shadow-md ring-2 ring-indigo-200' : 'border-slate-200 bg-white hover:border-slate-400 hover:shadow-sm',
      disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
    ]"
    :disabled="disabled"
    @click="$emit('select')"
  >
    <!-- 属性色带 -->
    <div
      class="absolute inset-y-0 left-0 w-1.5 rounded-l-xl transition-colors"
      :style="{ backgroundColor: typeColor }"
    />

    <div class="flex items-start justify-between gap-2 pl-1">
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <!-- Showdown 风格编号 -->
          <span
            v-if="moveIndex != null"
            class="flex h-5 w-5 shrink-0 items-center justify-center rounded-md border text-[10px] font-black"
            :class="selected ? 'border-indigo-300 bg-indigo-500 text-white' : 'border-slate-300 bg-slate-100 text-slate-500'"
          >{{ moveIndex + 1 }}</span>
          <span class="text-sm font-bold text-slate-900 truncate">{{ name }}</span>
          <span
            class="shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-bold text-white"
            :style="{ backgroundColor: typeColor }"
          >
            {{ typeName }}
          </span>
        </div>
        <div class="mt-0.5 flex items-center gap-3 text-[11px] text-slate-500">
          <span v-if="move.power">{{ tr('威力', 'Pwr') }} {{ move.power }}</span>
          <span v-if="move.accuracy != null">{{ tr('命中', 'Acc') }} {{ move.accuracy }}%</span>
          <span
            v-if="move.priority > 0"
            class="font-bold text-blue-600"
          >+{{ move.priority }}</span>
          <span
            v-else-if="move.priority < 0"
            class="font-bold text-red-500"
          >{{ move.priority }}</span>
          <span
            v-if="ppLabel"
            class="font-semibold"
            :class="ppClass"
          >PP {{ ppLabel }}</span>
        </div>
      </div>
      <div
        class="shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-semibold"
        :class="damageClassClass"
      >
        {{ damageClassLabel }}
      </div>
    </div>

    <!-- 选中指示 -->
    <div
      v-if="selected"
      class="absolute top-1.5 right-1.5 h-2.5 w-2.5 rounded-full bg-indigo-500 ring-2 ring-white"
    />
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const props = defineProps({
  move: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  moveIndex: { type: Number, default: null }
})

defineEmits(['select'])

// PP 显示：优先 currentPp / maxPp，回退到 move.pp（Showdown 风格 PP x/y）
const ppLabel = computed(() => {
  const cur = props.move?.currentPp
  const max = props.move?.maxPp ?? props.move?.pp
  if (cur != null && max != null) return `${cur}/${max}`
  if (cur != null) return String(cur)
  if (max != null) return `—/${max}`
  return ''
})

const ppClass = computed(() => {
  const cur = props.move?.currentPp
  const max = props.move?.maxPp ?? props.move?.pp
  if (cur == null || max == null || max <= 0) return 'text-slate-500'
  const ratio = cur / max
  if (cur <= 0) return 'text-rose-600 font-bold'
  if (ratio <= 0.25) return 'text-rose-500'
  if (ratio <= 0.5) return 'text-amber-600'
  return 'text-emerald-600'
})

// PokeAPI / 后端 type 表属性编号（1=normal, 2=fighting, 3=flying ...）
const TYPE_COLORS = {
  1: '#A8A77A', 2: '#C03028', 3: '#A890F0', 4: '#A040A0', 5: '#E0C068',
  6: '#B8A038', 7: '#A8B820', 8: '#705898', 9: '#B8B8D0', 10: '#F08030',
  11: '#6890F0', 12: '#78C850', 13: '#F8D030', 14: '#F85888', 15: '#98D8D8',
  16: '#7038F8', 17: '#705848', 18: '#EE99AC'
}

const TYPE_NAMES = {
  1: '一般', 2: '格斗', 3: '飞行', 4: '毒', 5: '地面',
  6: '岩石', 7: '虫', 8: '幽灵', 9: '钢', 10: '火',
  11: '水', 12: '草', 13: '电', 14: '超能力', 15: '冰',
  16: '龙', 17: '恶', 18: '妖精'
}

const TYPE_NAMES_EN = {
  1: 'Normal', 2: 'Fighting', 3: 'Flying', 4: 'Poison', 5: 'Ground',
  6: 'Rock', 7: 'Bug', 8: 'Ghost', 9: 'Steel', 10: 'Fire',
  11: 'Water', 12: 'Grass', 13: 'Electric', 14: 'Psychic', 15: 'Ice',
  16: 'Dragon', 17: 'Dark', 18: 'Fairy'
}

const name = computed(() => props.move.name || props.move.name_en || '?')
const typeName = computed(() => TYPE_NAMES[props.move.type_id] || TYPE_NAMES_EN[props.move.type_id] || '?')
const typeColor = computed(() => TYPE_COLORS[props.move.type_id] || '#777')

const damageClassLabel = computed(() => {
  const id = props.move.damage_class_id
  if (id === 2) return tr('物理', 'Phys')
  if (id === 3) return tr('特殊', 'Spec')
  if (id === 1) return tr('变化', 'Stat')
  return '?'
})

const damageClassClass = computed(() => {
  const id = props.move.damage_class_id
  if (id === 2) return 'bg-rose-100 text-rose-700'
  if (id === 3) return 'bg-blue-100 text-blue-700'
  if (id === 1) return 'bg-purple-100 text-purple-700'
  return 'bg-slate-100 text-slate-600'
})
</script>
