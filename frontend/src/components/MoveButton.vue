<template>
  <!-- Showdown 风格招式按钮：属性色块背景、紧凑布局 -->
  <button
    type="button"
    class="move-btn"
    :class="[selected ? 'move-btn-selected' : '', disabled ? 'move-btn-disabled' : '', effectivenessClass]"
    :style="{ '--type-color': typeColor }"
    :disabled="disabled"
    @click="$emit('select')"
  >
    <div class="move-btn-top">
      <span class="move-btn-name">{{ name }}</span>
      <span v-if="targetLabel" class="move-btn-target">{{ targetLabel }}</span>
    </div>
    <div class="move-btn-bottom">
      <span v-if="move.power" class="move-btn-stat">{{ move.power }}</span>
      <span v-if="move.accuracy != null && move.accuracy > 0" class="move-btn-stat">{{ move.accuracy }}%</span>
      <span class="move-btn-pp" :class="ppClass">{{ ppLabel }}</span>
      <span class="move-btn-type">{{ typeName }}</span>
    </div>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'
import { typeColor as typeColorById, typeNameZh, typeNameEn } from '../services/typeChart'

const { translate: tr } = useLocale()

const props = defineProps({
  move: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  moveIndex: { type: Number, default: null },
  effectiveness: { type: Number, default: null }
})

defineEmits(['select'])

const name = computed(() => props.move.name || props.move.name_en || '?')
const typeName = computed(() => tr(typeNameZh(props.move.type_id), typeNameEn(props.move.type_id)))
const typeColor = computed(() => typeColorById(props.move.type_id))

const targetLabel = computed(() => {
  const tid = Number(props.move?.target_id || 10)
  switch (tid) {
    case 4: case 7: return 'Self'
    case 8: return 'Rnd'
    case 9: return 'All'
    case 11: return 'Foes'
    case 13: return 'Field'
    case 14: return 'All'
    default: return ''
  }
})

const ppLabel = computed(() => {
  const cur = props.move?.currentPp
  const max = props.move?.maxPp ?? props.move?.pp
  if (cur != null && max != null) return `${cur}/${max}`
  if (cur != null) return `${cur}`
  if (max != null) return `--/${max}`
  return ''
})

const ppClass = computed(() => {
  const cur = props.move?.currentPp
  const max = props.move?.maxPp ?? props.move?.pp
  if (cur == null || max == null || max <= 0) return ''
  if (cur <= 0) return 'pp-empty'
  if (cur / max <= 0.25) return 'pp-low'
  if (cur / max <= 0.5) return 'pp-mid'
  return ''
})

const effectivenessClass = computed(() => {
  const e = props.effectiveness
  if (e == null || e === 1) return ''
  if (e >= 4) return 'eff-super4'
  if (e >= 2) return 'eff-super'
  if (e <= 0) return 'eff-immune'
  if (e < 1) return 'eff-resist'
  return ''
})
</script>

<style scoped>
.move-btn {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 8px;
  border: 2px solid #555;
  border-radius: 4px;
  background: var(--type-color, #666);
  color: #fff;
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  transition: all 0.15s;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
  min-height: 48px;
}
.move-btn:hover:not(:disabled) {
  filter: brightness(1.15);
  border-color: #888;
}
.move-btn-selected {
  border-color: #fbbf24;
  box-shadow: 0 0 8px rgba(251,191,36,0.5);
}
.move-btn-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.move-btn-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.move-btn-name {
  font-weight: 700;
  font-size: 13px;
}
.move-btn-target {
  font-size: 9px;
  background: rgba(0,0,0,0.3);
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: 600;
}

.move-btn-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  opacity: 0.85;
}
.move-btn-stat {
  font-weight: 600;
}
.move-btn-pp {
  margin-left: auto;
  font-weight: 600;
}
.move-btn-pp.pp-low { color: #fbbf24; }
.move-btn-pp.pp-mid { color: #fcd34d; }
.move-btn-pp.pp-empty { color: #f87171; }
.move-btn-type {
  background: rgba(0,0,0,0.3);
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: 700;
  font-size: 9px;
  text-transform: uppercase;
}

/* 克制关系边框 */
.eff-super4 { border-color: #ef4444 !important; }
.eff-super { border-color: #f59e0b !important; }
.eff-resist { border-color: #06b6d4 !important; }
.eff-immune { border-color: #6b7280 !important; opacity: 0.5; }
</style>
