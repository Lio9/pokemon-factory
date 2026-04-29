

<template>
  <div class="space-y-6">
    <section class="battle-hero overflow-hidden rounded-[24px] border border-slate-200/70 p-4 shadow-[0_24px_80px_-48px_rgba(15,23,42,0.55)] sm:rounded-[28px] sm:p-7">
      <div class="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 rounded-full border border-white/60 bg-white/70 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-sky-700 shadow-sm backdrop-blur">
            Factory Battle Lab
          </div>
          <h1 class="mt-4 text-[clamp(1.8rem,5vw,2.6rem)] font-black tracking-tight text-slate-950">
            {{ tr('对战工厂', 'Battle Factory') }}
          </h1>
          <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
            {{ tr('把 6 选 4、双打回合操作、补位、胜利交换和 9 连战进度统一收进一条清晰流程里。当前页面会根据战斗状态直接提示你下一步该做什么。', 'This page brings 6v4 preview, doubles turn decisions, replacements, exchange rewards, and 9-battle run progress into one clear flow. It always tells you what to do next based on the current battle state.') }}
          </p>
          <div class="mt-5 flex flex-wrap gap-3 text-xs font-semibold text-slate-600">
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">{{ tr('VGC 双打', 'VGC Doubles') }}</span>
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">{{ tr('段位积分', 'Ladder points') }}</span>
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">{{ tr('工厂 9 连战', '9-battle run') }}</span>
            <span
              v-if="pollingActive"
              class="rounded-full bg-emerald-100 px-3 py-1.5 text-emerald-700 shadow-sm"
            >
              {{ tr('异步模拟轮询中', 'Polling async simulation') }}
            </span>
          </div>
        </div>

        <div class="grid gap-3 sm:grid-cols-2 xl:min-w-[360px] xl:max-w-[420px]">
          <div class="rounded-2xl bg-slate-950 px-4 py-4 text-white shadow-lg shadow-slate-950/10">
            <div class="text-xs uppercase tracking-[0.2em] text-slate-300">
              {{ tr('当前流程', 'Current flow') }}
            </div>
            <div class="mt-2 text-lg font-bold">
              {{ actionHeadline }}
            </div>
            <div class="mt-2 text-sm leading-6 text-slate-300">
              {{ actionDescription }}
            </div>
          </div>
          <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-lg shadow-sky-900/5 backdrop-blur">
            <div class="text-xs uppercase tracking-[0.2em] text-slate-400">
              {{ tr('实时状态', 'Live status') }}
            </div>
            <div class="mt-2 text-lg font-bold text-slate-900">
              {{ statusText }}
            </div>
            <div class="mt-2 text-sm leading-6 text-slate-500">
              {{ progressSummary }}
            </div>
            <div
              v-if="lastUpdatedLabel"
              class="mt-3 text-xs font-medium text-slate-400"
            >
              {{ tr('最近刷新', 'Last refreshed') }}：{{ lastUpdatedLabel }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="flex flex-col gap-4 rounded-[24px] border border-slate-200/80 bg-white/90 p-4 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:rounded-3xl sm:p-6 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <h2 class="text-lg font-bold text-slate-900">
          {{ tr('训练师面板', 'Trainer panel') }}
        </h2>
        <p class="mt-2 text-sm leading-6 text-slate-500">
          {{ tr('段位、积分和挑战轮次会在战斗结算后自动刷新。手动对战、异步模拟和工厂挑战共用同一套战斗摘要。', 'Rank, points, and challenge progress refresh automatically after settlement. Manual battles, async simulation, and factory runs all share the same battle summary.') }}
        </p>
      </div>
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            {{ tr('玩家', 'Player') }}
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ currentUser }}
          </div>
        </div>
        <div
          class="rounded-xl px-4 py-3"
          :class="tierBgClass"
        >
          <div
            class="text-xs"
            :class="tierTextClass"
          >
            {{ tr('段位', 'Tier') }}
          </div>
          <div
            class="mt-1 font-semibold"
            :class="tierTextClass"
          >
            {{ tierDisplayName }}
          </div>
          <div
            class="mt-0.5 text-xs"
            :class="tierTextClass"
          >
            {{ playerProfile?.tierPoints ?? 0 }} / 2000 {{ tr('分', 'pts') }}
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            {{ tr('总积分', 'Total points') }}
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ playerProfile?.totalPoints ?? 0 }}
          </div>
          <div class="mt-0.5 text-xs text-slate-500">
            {{ playerProfile?.wins ?? 0 }}{{ tr('胜', 'W') }} / {{ playerProfile?.losses ?? 0 }}{{ tr('负', 'L') }}
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            {{ tr('当前状态', 'Current status') }}
          </div>
          <div
            class="mt-1 font-semibold"
            :class="summary?.status === 'completed' ? 'text-emerald-600' : summary?.status === 'preview' ? 'text-amber-600' : 'text-blue-600'"
          >
            {{ statusText }}
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="factoryRun"
      class="rounded-[24px] border border-sky-200 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(99,102,241,0.16),rgba(244,244,245,0.6))] p-4 shadow-[0_18px_60px_-46px_rgba(14,165,233,0.55)] sm:rounded-3xl"
    >
      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div class="text-sm font-semibold text-indigo-900">
          {{ tr('工厂挑战', 'Factory challenge') }} · {{ tr('第 {current} / {max} 轮', 'Round {current} / {max}', { current: factoryRun.current_battle || 0, max: factoryRun.max_battles || 9 }) }}
        </div>
        <div class="text-sm text-indigo-700">
          {{ factoryRun.wins || 0 }}{{ tr('胜', 'W') }} {{ factoryRun.losses || 0 }}{{ tr('负', 'L') }}
        </div>
      </div>
      <div class="mt-2 flex gap-1">
        <div
          v-for="index in (factoryRun.max_battles || 9)"
          :key="index"
          class="h-2 flex-1 rounded-full"
          :class="factoryRoundClass(index)"
        />
      </div>
    </div>

    <div
      v-if="requestError"
      class="rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700 shadow-sm"
    >
      {{ requestError }}
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

defineProps({
  actionHeadline: { type: String, default: '' },
  actionDescription: { type: String, default: '' },
  currentUser: { type: String, default: '' },
  factoryRoundClass: { type: Function, required: true },
  factoryRun: { type: Object, default: null },
  lastUpdatedLabel: { type: String, default: '' },
  playerProfile: { type: Object, default: null },
  pollingActive: { type: Boolean, default: false },
  progressSummary: { type: String, default: '' },
  requestError: { type: String, default: '' },
  statusText: { type: String, default: '' },
  summary: { type: Object, default: null },
  tierBgClass: { type: String, default: '' },
  tierDisplayName: { type: String, default: '' },
  tierTextClass: { type: String, default: '' }
})
</script>
