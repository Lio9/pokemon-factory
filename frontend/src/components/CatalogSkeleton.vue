<template>
  <div
    class="grid gap-4"
    :class="gridClass"
  >
    <div
      v-for="i in count"
      :key="i"
      class="glass-card p-4 animate-pulse"
    >
      <div class="h-5 bg-slate-200 rounded w-2/3 mb-3" />
      <div class="h-12 bg-slate-100 rounded w-full" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  count: { type: Number, default: 8 },
  viewMode: { type: String, default: 'grid' }
})

const gridClass = computed(() => {
  if (props.viewMode === 'list') return 'space-y-2'
  return 'grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4'
})
</script>
