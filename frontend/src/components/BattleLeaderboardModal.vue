

<template>
  <div
    class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-3 sm:items-center sm:p-6"
    @click.self="emit('close')"
  >
    <div class="w-full max-w-lg rounded-[24px] bg-white p-4 shadow-xl sm:rounded-2xl sm:p-6">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold text-slate-900">
          {{ tr('大师球段位排行榜', 'Master Ball leaderboard') }}
        </h2>
        <button
          class="text-slate-400 hover:text-slate-600"
          @click="emit('close')"
        >
          ✕
        </button>
      </div>
      <div class="mt-4 max-h-96 space-y-2 overflow-y-auto">
        <div
          v-if="loading"
          class="py-8 text-center text-sm text-slate-500"
        >
          {{ tr('正在加载排行榜...', 'Loading leaderboard...') }}
        </div>
        <template v-else-if="leaderboardData.length">
          <div
            v-for="(entry, index) in leaderboardData"
            :key="entry.username"
            class="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-4 py-3"
          >
            <div class="flex items-center gap-3">
              <span
                class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold"
                :class="index < 3 ? 'bg-amber-400 text-white' : 'bg-slate-200 text-slate-600'"
              >
                {{ index + 1 }}
              </span>
              <span class="font-semibold text-slate-900">{{ entry.username }}</span>
            </div>
            <div class="text-right">
              <div class="font-semibold text-indigo-600">
                {{ entry.totalPoints }} {{ tr('分', 'pts') }}
              </div>
              <div class="text-xs text-slate-500">
                {{ entry.wins }}{{ tr('胜', 'W') }} {{ entry.losses }}{{ tr('负', 'L') }}
              </div>
            </div>
          </div>
        </template>
        <div
          v-else
          class="py-8 text-center text-sm text-slate-500"
        >
          {{ tr('暂无大师球段位玩家', 'No Master Ball players yet') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const emit = defineEmits(['close'])

defineProps({
  leaderboardData: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>
