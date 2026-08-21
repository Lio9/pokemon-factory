<template>
  <PokeHoverCard
    :move="move"
    wrap-class="w-full"
  >
    <button
      type="button"
      class="move-button group relative overflow-hidden rounded-xl border-2 px-3 py-2.5 text-left transition-all duration-150"
      :class="[
        selected ? 'border-indigo-500 bg-indigo-50 shadow-md ring-2 ring-indigo-200' : 'border-slate-200 bg-white hover:border-slate-400 hover:shadow-sm',
        disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer',
        effectivenessClass
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
              class="flex h-5 w-5 shrink-0 items-center justify-center rounded-md border text-[11px] font-black"
              :class="selected ? 'border-indigo-300 bg-indigo-500 text-white' : 'border-slate-300 bg-slate-100 text-slate-500'"
            >{{ moveIndex + 1 }}</span>
            <span class="text-sm font-bold text-slate-900 truncate">{{ name }}</span>
            <span
              class="shrink-0 rounded-md px-1.5 py-0.5 text-[11px] font-bold text-white"
              :style="{ backgroundColor: typeColor }"
            >
              {{ typeName }}
            </span>
            <span
              v-if="targetLabel"
              class="shrink-0 rounded-md px-1 py-0.5 text-[10px] font-semibold bg-slate-100 text-slate-500"
            >{{ targetLabel }}</span>
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
          class="shrink-0 rounded-md px-1.5 py-0.5 text-[11px] font-semibold"
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
  </PokeHoverCard>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'
import { typeColor as typeColorById, typeNameZh, typeNameEn } from '../services/typeChart'
import PokeHoverCard from './PokeHoverCard.vue'

const { translate: tr } = useLocale()

const props = defineProps({
  move: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  moveIndex: { type: Number, default: null },
  effectiveness: { type: Number, default: null }
})

defineEmits(['select'])

// G8: 目标类型短标签（Showdown 风格）
const targetLabel = computed(() => {
  const tid = Number(props.move?.target_id || 10)
  switch (tid) {
    case 4: return '自身'
    case 7: return '自身'
    case 8: return '随机'
    case 9: return '全体'
    case 10: return ''
    case 11: return '群'
    case 13: return '己方'
    case 14: return '全场'
    default: return ''
  }
})

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

const name = computed(() => props.move.name || props.move.name_en || '?')
const typeName = computed(() => tr(typeNameZh(props.move.type_id), typeNameEn(props.move.type_id)))
const typeColor = computed(() => typeColorById(props.move.type_id))

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

// G9: 克制关系着色（Showdown 风格：绿色边框=有效，红色=抵抗）
const effectivenessClass = computed(() => {
  const e = props.effectiveness
  if (e == null || e === 1) return ''
  if (e >= 4) return '!border-rose-500 !bg-rose-50/80'
  if (e >= 2) return '!border-amber-500 !bg-amber-50/60'
  if (e <= 0.25) return '!border-sky-400 !bg-sky-50/60'
  if (e < 1) return '!border-cyan-400 !bg-cyan-50/50'
  return ''
})
</script>
