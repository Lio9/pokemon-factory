<template>
  <div
    ref="triggerRef"
    class="text-center py-8"
  >
    <div
      v-if="loadingMore"
      class="flex items-center justify-center gap-3"
    >
      <div class="loading-dots">
        <span /><span /><span />
      </div>
      <span class="text-sm text-slate-400">{{ $slots.loading?.() || tr('加载中...', 'Loading...') }}</span>
    </div>
    <div
      v-else-if="!hasMore && loadedCount > 0"
      class="text-sm text-slate-400"
    >
      {{ $slots.allLoaded?.() || tr('已加载全部 {total} 个', 'All {total} loaded', { total }) }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

defineProps({
  loadingMore: Boolean,
  hasMore: Boolean,
  loadedCount: Number,
  total: Number
})

const emit = defineEmits(['load-more'])

const triggerRef = ref(null)
let observer = null

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          emit('load-more')
        }
      })
    },
    { rootMargin: '200px', threshold: 0 }
  )
  if (triggerRef.value) {
    observer.observe(triggerRef.value)
  }
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>
