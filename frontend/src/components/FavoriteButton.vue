<template>
  <button
    class="flex-shrink-0 transition-all duration-200 hover:scale-125"
    :class="[isFavorited ? 'text-amber-500 fav-bounce' : 'text-slate-300 hover:text-amber-400']"
    @click.stop="$emit('toggle')"
  >
    <component
      :is="isFavorited ? 'StarFilled' : 'Star'"
      :class="sizeClass"
    />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  isFavorited: { type: Boolean, default: false },
  size: { type: String, default: 'sm' }
})

defineEmits(['toggle'])

const sizeClass = computed(() => {
  return props.size === 'lg' ? 'w-5 h-5' : 'w-4 h-4'
})
</script>
