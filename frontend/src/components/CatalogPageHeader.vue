<template>
  <div
    class="relative overflow-hidden rounded-3xl border border-white/60 p-6 shadow-[0_20px_60px_-30px_rgba(15,23,42,0.4)] sm:p-8"
    :style="heroStyle"
  >
    <!-- 装饰圆 -->
    <div class="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
    <div class="pointer-events-none absolute -bottom-20 -left-10 h-48 w-48 rounded-full bg-white/5" />

    <div class="relative flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-start gap-4">
        <!-- 图标 -->
        <div
          class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl text-2xl shadow-lg"
          :style="{ background: 'linear-gradient(135deg, ' + color + ', ' + colorLight + ')', boxShadow: '0 4px 0 0 rgba(0,0,0,0.15), inset 0 1px 0 1px rgba(255,255,255,0.35)' }"
        >
          <span class="drop-shadow">{{ icon }}</span>
        </div>
        <div>
          <div
            class="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.2em] text-white/90 backdrop-blur-sm"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-white/80" />
            {{ badge }}
          </div>
          <h1 class="mt-2 text-2xl font-black tracking-tight text-white sm:text-3xl">
            {{ title }}
          </h1>
          <p
            v-if="subtitle"
            class="mt-1.5 max-w-2xl text-sm leading-6 text-white/80"
          >
            {{ subtitle }}
          </p>
        </div>
      </div>

      <!-- 右侧插槽：统计等 -->
      <div
        v-if="$slots.actions"
        class="flex shrink-0 items-center gap-3"
      >
        <slot name="actions" />
      </div>
    </div>

    <!-- 底部渐变线 -->
    <div
      class="absolute inset-x-0 bottom-0 h-1"
      :style="{ background: 'linear-gradient(90deg, ' + color + ', ' + colorLight + ', ' + color + ')' }"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: '⭐' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  badge: { type: String, default: '' },
  color: { type: String, default: '#dc2626' },
  colorLight: { type: String, default: '#f59e0b' }
})

const heroStyle = computed(() => ({
  background: `linear-gradient(135deg, ${props.color} 0%, ${props.colorLight} 100%)`
}))
</script>
