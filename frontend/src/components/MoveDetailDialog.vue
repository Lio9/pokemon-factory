<template>
  <el-dialog
    :model-value="visible"
    :title="move?.name"
    width="580px"
    :close-on-click-modal="true"
    destroy-on-close
    class="detail-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div
      v-if="move"
      class="space-y-6"
    >
      <!-- 头部：编号 + 属性 + 分类 -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-xs font-mono text-slate-400 bg-slate-100 px-2 py-1 rounded">#{{ move.id }}</span>
          <span
            v-if="move.typeName"
            class="type-badge shadow-md"
            :style="{ backgroundColor: move.typeColor || '#888' }"
          >{{ move.typeName }}</span>
          <span
            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold shadow-sm"
            :class="{
              'bg-rose-100 text-rose-700': move.damageClass === '物理',
              'bg-blue-100 text-blue-700': move.damageClass === '特殊',
              'bg-purple-100 text-purple-700': move.damageClass === '变化'
            }"
          >
            {{ move.damageClass || '-' }}
          </span>
        </div>
        <span class="text-xs text-slate-400">{{ move.nameEn || '' }}</span>
      </div>

      <!-- 基础数值 -->
      <div class="grid grid-cols-4 gap-3">
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-3 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">
            威力
          </div>
          <div
            class="text-2xl font-bold"
            :class="powerColor"
          >
            {{ move.power ?? '-' }}
          </div>
        </div>
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-3 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">
            命中
          </div>
          <div class="text-2xl font-bold text-slate-800">
            {{ move.accuracy != null ? move.accuracy + '%' : '-' }}
          </div>
        </div>
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-3 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">
            PP
          </div>
          <div class="text-2xl font-bold text-slate-800">
            {{ move.pp ?? '-' }}
          </div>
        </div>
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-3 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">
            优先度
          </div>
          <div
            class="text-2xl font-bold"
            :class="priorityColor"
          >
            {{ move.priority ?? 0 }}
          </div>
        </div>
      </div>

      <!-- 效果描述 -->
      <div
        v-if="move.description || move.effect"
        class="space-y-3"
      >
        <div
          v-if="move.description"
          class="rounded-xl bg-slate-50 border border-slate-100 p-4"
        >
          <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
            技能介绍
          </div>
          <p class="text-sm text-slate-700 leading-relaxed">
            {{ move.description }}
          </p>
        </div>
        <div
          v-if="move.effect"
          class="rounded-xl bg-indigo-50 border border-indigo-100 p-4"
        >
          <div class="text-xs font-bold uppercase tracking-wider text-indigo-400 mb-2">
            追加效果
          </div>
          <p class="text-sm text-indigo-700 leading-relaxed">
            {{ move.effect }}
          </p>
        </div>
      </div>

      <!-- 属性相克 -->
      <div
        v-if="typeEffectiveness.length"
        class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4"
      >
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          属性相克
        </div>
        <div class="flex flex-wrap gap-1.5">
          <div
            v-for="t in typeEffectiveness"
            :key="t.name"
            class="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium"
            :style="{ backgroundColor: t.color + '20', color: t.color }"
          >
            <span
              class="w-2 h-2 rounded-full"
              :style="{ backgroundColor: t.color }"
            />
            <span>{{ t.name }}</span>
            <span class="font-bold ml-0.5">×{{ t.multiplier }}</span>
          </div>
        </div>
      </div>

      <!-- 可学习宝可梦 -->
      <div
        v-if="learners.length"
        class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4"
      >
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          可学习宝可梦
          <span class="font-normal ml-1">({{ learners.length }})</span>
        </div>
        <div class="flex flex-wrap gap-2 max-h-32 overflow-y-auto">
          <router-link
            v-for="p in learners.slice(0, 50)"
            :key="p.id"
            :to="`/pokemon/${p.id}`"
            class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-xs text-slate-700 hover:bg-blue-50 hover:border-blue-200 hover:text-blue-700 transition-colors"
          >
            <img
              :src="p.sprite"
              class="w-5 h-5 object-contain"
              loading="lazy"
            >
            <span>{{ p.name }}</span>
          </router-link>
          <span
            v-if="learners.length > 50"
            class="text-xs text-slate-400 self-center ml-1"
          >
            +{{ learners.length - 50 }} 更多
          </span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import { useLocale } from '../composables/useLocale'
import { getTypeEffectiveness, typeColor, typeNameZh, typeNameEn } from '../services/typeChart'

const { translate: tr } = useLocale()

const props = defineProps({
  visible: { type: Boolean, default: false },
  move: { type: Object, default: null }
})

defineEmits(['update:visible'])

// 稍后加载的属性相克数据（需要从API获取）
const typeEffectiveness = ref([])
const learners = ref([])

const powerColor = computed(() => {
  const p = props.move?.power
  if (!p) return 'text-slate-400'
  if (p >= 120) return 'text-red-600'
  if (p >= 80) return 'text-orange-600'
  if (p >= 60) return 'text-amber-600'
  return 'text-slate-700'
})

const priorityColor = computed(() => {
  const p = props.move?.priority || 0
  if (p > 0) return 'text-green-600'
  if (p < 0) return 'text-red-600'
  return 'text-slate-800'
})

watch(() => props.move, (move) => {
  if (!move) return
  // 根据技能类型从 typeChart 相克表生成属性相克展示（单一数据源、双语支持）
  typeEffectiveness.value = generateTypeEffectiveness(move)
}, { immediate: true })

function generateTypeEffectiveness(move) {
  const moveTypeId = Number(move.type_id || 0)
  if (moveTypeId <= 0) return []
  const chart = getTypeEffectiveness()
  const targetIds = chart[moveTypeId] || {}
  const result = []
  const sorted = Object.entries(targetIds)
    .map(([tid, factor]) => ({ tid: Number(tid), factor: Number(factor) }))
    .filter((item) => item.factor !== 100)
    .sort((a, b) => a.factor - b.factor)
  for (const item of sorted) {
    result.push({
      name: tr(typeNameZh(item.tid), typeNameEn(item.tid)),
      color: typeColor(item.tid),
      multiplier: item.factor / 100
    })
  }
  return result
}
</script>

<style scoped>
.detail-dialog :deep(.el-dialog) { border-radius: 1.5rem !important; }
.detail-dialog :deep(.el-dialog__header) { padding: 1.5rem 1.5rem 0; }
.detail-dialog :deep(.el-dialog__body) { padding: 1.5rem; }
.detail-dialog :deep(.el-dialog__title) { font-weight: 700; font-size: 1.25rem; }

/* 属性标签样式 */
.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: 0.5rem;
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

/* 滚动条 */
.overflow-y-auto::-webkit-scrollbar { width: 4px; }
.overflow-y-auto::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }
</style>
