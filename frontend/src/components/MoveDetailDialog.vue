<template>
  <el-dialog
    :model-value="visible"
    :title="move?.name"
    width="520px"
    :close-on-click-modal="true"
    destroy-on-close
    class="detail-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div
      v-if="move"
      class="space-y-5"
    >
      <div class="flex items-center gap-3">
        <span
          v-if="move.typeName"
          class="type-badge"
          :style="{ backgroundColor: move.typeColor || '#888' }"
        >{{ move.typeName }}</span>
        <span
          class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
          :class="{
            'bg-rose-100 text-rose-700': move.damageClass === 'physical',
            'bg-blue-100 text-blue-700': move.damageClass === 'special',
            'bg-purple-100 text-purple-700': move.damageClass === 'status'
          }"
        >
          {{ damageClassLabel }}
        </span>
      </div>

      <div class="grid grid-cols-3 gap-3">
        <div class="glass-card p-3 text-center">
          <div class="text-xs text-slate-500 mb-1">
            {{ tr('威力', 'Power') }}
          </div>
          <div class="text-xl font-bold text-slate-800">
            {{ move.power ?? '-' }}
          </div>
        </div>
        <div class="glass-card p-3 text-center">
          <div class="text-xs text-slate-500 mb-1">
            {{ tr('命中', 'Accuracy') }}
          </div>
          <div class="text-xl font-bold text-slate-800">
            {{ move.accuracy != null ? `${move.accuracy}%` : '-' }}
          </div>
        </div>
        <div class="glass-card p-3 text-center">
          <div class="text-xs text-slate-500 mb-1">
            PP
          </div>
          <div class="text-xl font-bold text-slate-800">
            {{ move.pp ?? '-' }}
          </div>
        </div>
      </div>

      <div
        v-if="move.description"
        class="rounded-xl bg-slate-50 p-4 text-sm text-slate-700 leading-relaxed"
      >
        {{ move.description }}
      </div>

      <div
        v-if="move.effect"
        class="rounded-xl bg-indigo-50 p-4 text-sm text-indigo-700 leading-relaxed"
      >
        <div class="text-xs font-semibold uppercase tracking-wider text-indigo-500 mb-1">
          {{ tr('追加效果', 'Additional effect') }}
        </div>
        {{ move.effect }}
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
  move: { type: Object, default: null }
})

defineEmits(['update:visible'])

const damageClassLabel = computed(() => {
  if (!props.move) return ''
  const dc = props.move.damageClass
  if (dc === 'physical') return tr('物理', 'Physical')
  if (dc === 'special') return tr('特殊', 'Special')
  if (dc === 'status') return tr('变化', 'Status')
  return dc
})
</script>
