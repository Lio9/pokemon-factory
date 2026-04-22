<template>
  <section class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
          {{ tr('下一步', 'Next step') }}
        </div>
        <div class="mt-2 text-base font-bold text-slate-900">
          {{ actionHeadline }}
        </div>
        <div class="mt-1 text-sm leading-6 text-slate-500">
          {{ actionDescription }}
        </div>
      </div>
      <div class="grid w-full gap-2 text-sm sm:min-w-[220px] sm:w-auto">
        <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">
            {{ tr('当前战斗', 'Current battle') }}
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ currentBattleId ? `#${currentBattleId}` : tr('未创建', 'Not created') }}
          </div>
        </div>
        <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">
            {{ tr('推荐模式', 'Recommended mode') }}
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ recommendedMode }}
          </div>
        </div>
      </div>
    </div>
  </section>

  <div class="grid grid-cols-1 gap-3 sm:flex sm:flex-wrap">
    <template v-if="!factoryRun && !currentBattleId">
      <button
        class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="isBusy"
        @click="emit('start-factory')"
      >
        {{ busyAction === 'factory-start' ? tr('正在创建挑战...', 'Creating run...') : tr('开始工厂挑战（9 轮）', 'Start factory challenge (9 rounds)') }}
      </button>
      <button
        class="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="isBusy"
        @click="emit('start-manual')"
      >
        {{ busyAction === 'start-manual' ? tr('正在创建战斗...', 'Creating battle...') : tr('单场手动对战', 'Single manual battle') }}
      </button>
      <button
        class="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="isBusy"
        @click="emit('start-async')"
      >
        {{ busyAction === 'start-async' ? tr('正在提交模拟...', 'Submitting simulation...') : tr('异步模拟', 'Async simulation') }}
      </button>
    </template>
    <template v-else-if="factoryRun && !currentBattleId">
      <button
        class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="isBusy"
        @click="emit('next-factory')"
      >
        {{ busyAction === 'factory-next' ? tr('正在进入下一轮...', 'Entering next round...') : tr('进入第 {round} 轮', 'Enter round {round}', { round: (factoryRun.current_battle || 0) + 1 }) }}
      </button>
      <button
        class="rounded-xl border border-rose-300 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
        :disabled="isBusy"
        @click="emit('abandon-factory')"
      >
        {{ tr('放弃本次挑战', 'Abandon this run') }}
      </button>
    </template>
    <template v-else>
      <button
        class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
        :disabled="!currentBattleId || isBusy"
        @click="emit('refresh-status')"
      >
        {{ busyAction === 'refresh-status' ? tr('刷新中...', 'Refreshing...') : tr('刷新状态', 'Refresh status') }}
      </button>
      <button
        v-if="currentBattleId && summary?.status === 'running'"
        class="rounded-xl border border-rose-300 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
        :disabled="isBusy"
        @click="emit('forfeit-battle')"
      >
        {{ busyAction === 'forfeit-battle' ? tr('投降处理中...', 'Forfeiting...') : tr('投降', 'Forfeit') }}
      </button>
    </template>
    <button
      class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
      :disabled="isBusy"
      @click="emit('open-leaderboard')"
    >
      {{ tr('排行榜', 'Leaderboard') }}
    </button>
    <button
      v-if="showContinueFactoryButton"
      class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
      :disabled="isBusy"
      @click="emit('prepare-next')"
    >
      {{ tr('准备下一轮', 'Prepare next round') }}
    </button>
    <button
      v-if="showResetBattleButton"
      class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
      :disabled="isBusy"
      @click="emit('reset-battle')"
    >
      {{ tr('清空当前战场', 'Reset battlefield') }}
    </button>
  </div>

  <section class="grid gap-3 md:grid-cols-3">
    <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
      <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
        {{ tr('当前模式', 'Current mode') }}
      </div>
      <div class="mt-2 text-base font-bold text-slate-900">
        {{ modeSummary }}
      </div>
      <div class="mt-1 text-sm text-slate-500">
        {{ modeDescription }}
      </div>
    </div>
    <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
      <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
        {{ tr('回合推进', 'Round progress') }}
      </div>
      <div class="mt-2 text-base font-bold text-slate-900">
        {{ summary?.currentRound || 0 }} / {{ summary?.roundLimit || '-' }}
      </div>
      <div class="mt-1 text-sm text-slate-500">
        {{ pollingActive ? tr('异步模式会自动刷新状态。', 'Async mode refreshes status automatically.') : tr('手动模式需要提交本回合动作。', 'Manual mode requires you to submit this turn.') }}
      </div>
    </div>
    <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
      <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
        {{ tr('可执行操作', 'Available actions') }}
      </div>
      <div class="mt-2 text-base font-bold text-slate-900">
        {{ availableActionCount }}
      </div>
      <div class="mt-1 text-sm text-slate-500">
        {{ availableActionDescription }}
      </div>
    </div>
  </section>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const emit = defineEmits([
  'abandon-factory',
  'forfeit-battle',
  'next-factory',
  'open-leaderboard',
  'prepare-next',
  'refresh-status',
  'reset-battle',
  'start-async',
  'start-factory',
  'start-manual'
])

defineProps({
  actionDescription: { type: String, default: '' },
  actionHeadline: { type: String, default: '' },
  availableActionCount: { type: Number, default: 0 },
  availableActionDescription: { type: String, default: '' },
  busyAction: { type: String, default: '' },
  currentBattleId: { type: [Number, String], default: null },
  factoryRun: { type: Object, default: null },
  isBusy: { type: Boolean, default: false },
  modeDescription: { type: String, default: '' },
  modeSummary: { type: String, default: '' },
  pollingActive: { type: Boolean, default: false },
  recommendedMode: { type: String, default: '' },
  showContinueFactoryButton: { type: Boolean, default: false },
  showResetBattleButton: { type: Boolean, default: false },
  summary: { type: Object, default: null }
})
</script>
