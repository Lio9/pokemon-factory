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
  disabled: { type: Boolean, default: false }
})

defineEmits(['select'])

const TYPE_COLORS = {
  1: '#A8A77A', 2: '#C62828', 3: '#456AE4', 4: '#A040A0', 5: '#F7D02C',
  6: '#B69F37', 7: '#A6B91A', 8: '#74C8E2', 9: '#B7B7CE', 10: '#EE8130',
  11: '#6390F0', 12: '#7AC74C', 13: '#F95587', 14: '#A98FF3', 15: '#98D8D8',
  16: '#705746', 17: '#6F35FC', 18: '#D685AD'
}

const TYPE_NAMES = {
  1: '一般', 2: '火', 3: '水', 4: '草', 5: '电',
  6: '冰', 7: '格斗', 8: '地面', 9: '飞行', 10: '超能力',
  11: '龙', 12: '虫', 13: '毒', 14: '岩石', 15: '幽灵',
  16: '钢', 17: '恶', 18: '妖精'
}

const TYPE_NAMES_EN = {
  1: 'Normal', 2: 'Fire', 3: 'Water', 4: 'Grass', 5: 'Electric',
  6: 'Ice', 7: 'Fighting', 8: 'Ground', 9: 'Flying', 10: 'Psychic',
  11: 'Dragon', 12: 'Bug', 13: 'Poison', 14: 'Rock', 15: 'Ghost',
  16: 'Steel', 17: 'Dark', 18: 'Fairy'
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
