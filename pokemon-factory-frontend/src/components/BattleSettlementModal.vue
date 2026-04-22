<template>
  <div class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-3 sm:items-center sm:p-6">
    <div class="w-full max-w-md rounded-[24px] bg-white p-4 shadow-xl sm:rounded-2xl sm:p-6">
      <div class="text-center">
        <div
          class="text-3xl font-bold"
          :class="settlement.won ? 'text-emerald-600' : 'text-rose-600'"
        >
          {{ settlement.won ? tr('胜利！', 'Victory!') : tr('失败', 'Defeat') }}
        </div>
        <div
          v-if="settlement.factoryRound"
          class="mt-2 text-sm text-slate-500"
        >
          {{ tr('工厂挑战第 {round} / 9 轮', 'Factory challenge round {round} / 9', { round: settlement.factoryRound }) }}
        </div>
      </div>
      <div class="mt-5 space-y-3">
        <div
          v-if="settlement.pointsDelta != null"
          class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3"
        >
          <span class="text-sm text-slate-600">{{ tr('积分变动', 'Point change') }}</span>
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
            {{ settlement.tierChange === 'promoted' ? tr('段位晋升！', 'Tier promoted!') : tr('段位下降', 'Tier demoted') }}
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
          {{ tr('工厂挑战结束', 'Factory run finished') }} · {{ settlement.runWins }}{{ tr('胜', 'W') }}{{ settlement.runLosses }}{{ tr('负', 'L') }}
          <span v-if="settlement.runReward"> · {{ tr('奖励', 'Reward') }} +{{ settlement.runReward }} {{ tr('分', 'pts') }}</span>
        </div>
      </div>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row">
        <button
          v-if="factoryRun && !settlement.runFinished"
          class="flex-1 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700"
          @click="emit('continue')"
        >
          {{ tr('继续下一轮', 'Continue next round') }}
        </button>
        <button
          class="flex-1 rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          @click="emit('close')"
        >
          {{ settlement.runFinished || !factoryRun ? tr('返回', 'Back') : tr('查看战场', 'View battlefield') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const emit = defineEmits(['close', 'continue'])

defineProps({
  factoryRun: { type: Object, default: null },
  settlement: { type: Object, required: true }
})
</script>
