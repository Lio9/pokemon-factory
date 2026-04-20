<template>
  <div class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-3 sm:items-center sm:p-6">
    <div class="w-full max-w-md rounded-[24px] bg-white p-4 shadow-xl sm:rounded-2xl sm:p-6">
      <div class="text-center">
        <div
          class="text-3xl font-bold"
          :class="settlement.won ? 'text-emerald-600' : 'text-rose-600'"
        >
          {{ settlement.won ? '胜利！' : '失败' }}
        </div>
        <div
          v-if="settlement.factoryRound"
          class="mt-2 text-sm text-slate-500"
        >
          工厂挑战第 {{ settlement.factoryRound }} / 9 轮
        </div>
      </div>
      <div class="mt-5 space-y-3">
        <div
          v-if="settlement.pointsDelta != null"
          class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3"
        >
          <span class="text-sm text-slate-600">积分变动</span>
          <span
            class="font-semibold"
            :class="settlement.pointsDelta >= 0 ? 'text-emerald-600' : 'text-rose-600'"
          >
            {{ settlement.pointsDelta >= 0 ? '+' : '' }}{{ settlement.pointsDelta }}
          </span>
        </div>
        <div
          v-if="settlement.tierChange"
          class="flex items-center justify-between rounded-xl px-4 py-3"
          :class="settlement.tierChange === 'promoted' ? 'bg-amber-50' : 'bg-rose-50'"
        >
          <span
            class="text-sm"
            :class="settlement.tierChange === 'promoted' ? 'text-amber-700' : 'text-rose-600'"
          >
            {{ settlement.tierChange === 'promoted' ? '段位晋升！' : '段位下降' }}
          </span>
          <span
            class="font-semibold"
            :class="settlement.tierChange === 'promoted' ? 'text-amber-700' : 'text-rose-600'"
          >
            {{ settlement.newTierName }}
          </span>
        </div>
        <div
          v-if="settlement.runFinished"
          class="rounded-xl bg-indigo-50 px-4 py-3 text-center text-sm text-indigo-700"
        >
          工厂挑战结束 · {{ settlement.runWins }}胜{{ settlement.runLosses }}负
          <span v-if="settlement.runReward"> · 奖励 +{{ settlement.runReward }} 分</span>
        </div>
      </div>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row">
        <button
          v-if="factoryRun && !settlement.runFinished"
          class="flex-1 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700"
          @click="emit('continue')"
        >
          继续下一轮
        </button>
        <button
          class="flex-1 rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          @click="emit('close')"
        >
          {{ settlement.runFinished || !factoryRun ? '返回' : '查看战场' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(['close', 'continue'])

defineProps({
  factoryRun: { type: Object, default: null },
  settlement: { type: Object, required: true }
})
</script>