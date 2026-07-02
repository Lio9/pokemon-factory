<template>
  <el-dialog
    :model-value="visible"
    :title="item?.name"
    width="480px"
    :close-on-click-modal="true"
    destroy-on-close
    class="detail-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="item" class="space-y-6">
      <div class="flex items-center gap-5">
        <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center shadow-inner border border-indigo-100">
          <img
            :src="item._imageUrl || itemImage"
            :alt="item.name"
            class="w-14 h-14 object-contain"
            @error="onImgError"
          >
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-xs font-mono text-slate-400 bg-slate-100 px-2 py-0.5 rounded">#{{ item.id }}</span>
            <span
              v-if="item.categoryName"
              class="text-xs font-medium text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded"
            >{{ item.categoryName }}</span>
          </div>
          <h3 class="text-lg font-bold text-slate-800">{{ item.name }}</h3>
          <p class="text-sm text-slate-400">{{ item.nameEn }}</p>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">价格</div>
          <div class="text-xl font-bold text-slate-800">{{ item.cost != null ? '¥' + item.cost : '-' }}</div>
        </div>
        <div class="rounded-xl bg-gradient-to-b from-slate-50 to-white border border-slate-100 p-4 text-center">
          <div class="text-xs text-slate-400 mb-1 font-medium">分类</div>
          <div class="text-xl font-bold text-slate-800">{{ item.categoryName || '-' }}</div>
        </div>
      </div>

      <div
        v-if="item.description"
        class="rounded-xl bg-slate-50 border border-slate-100 p-4"
      >
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">物品描述</div>
        <p class="text-sm text-slate-700 leading-relaxed">{{ item.description }}</p>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { sprites } from '../services/api'

defineProps({
  visible: { type: Boolean, default: false },
  item: { type: Object, default: null }
})
defineEmits(['update:visible'])

const itemImage = ref('')

function onImgError(e) {
  e.target.src = sprites.default
}
</script>

<style scoped>
.detail-dialog :deep(.el-dialog) { border-radius: 1.5rem !important; }
.detail-dialog :deep(.el-dialog__header) { padding: 1.5rem 1.5rem 0; }
.detail-dialog :deep(.el-dialog__body) { padding: 1.5rem; }
.detail-dialog :deep(.el-dialog__title) { font-weight: 700; font-size: 1.25rem; }
</style>
