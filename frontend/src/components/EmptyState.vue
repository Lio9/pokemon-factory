<template>
  <div class="text-center py-16">
    <div class="text-4xl mb-4">
      {{ icon || '🔍' }}
    </div>
    <p class="text-slate-500">
      {{ message || tr('没有找到结果', 'No results found') }}
    </p>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

defineProps({
  icon: { type: String, default: '🔍' },
  message: { type: String, default: '' }
})
</script>
