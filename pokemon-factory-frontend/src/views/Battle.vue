<template>
  <div class="space-y-6 pb-24 md:pb-0">
    <section class="battle-hero overflow-hidden rounded-[24px] border border-slate-200/70 p-4 shadow-[0_24px_80px_-48px_rgba(15,23,42,0.55)] sm:rounded-[28px] sm:p-7">
      <div class="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 rounded-full border border-white/60 bg-white/70 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-sky-700 shadow-sm backdrop-blur">
            Factory Battle Lab
          </div>
          <h1 class="mt-4 text-[clamp(1.8rem,5vw,2.6rem)] font-black tracking-tight text-slate-950">
            对战工厂
          </h1>
          <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
            把 6 选 4、双打回合操作、补位、胜利交换和 9 连战进度统一收进一条清晰流程里。当前页面会根据战斗状态直接提示你下一步该做什么。
          </p>
          <div class="mt-5 flex flex-wrap gap-3 text-xs font-semibold text-slate-600">
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">VGC 双打</span>
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">段位积分</span>
            <span class="rounded-full bg-white/80 px-3 py-1.5 shadow-sm">工厂 9 连战</span>
            <span
              v-if="pollingActive"
              class="rounded-full bg-emerald-100 px-3 py-1.5 text-emerald-700 shadow-sm"
            >
              异步模拟轮询中
            </span>
          </div>
        </div>

        <div class="grid gap-3 sm:grid-cols-2 xl:min-w-[360px] xl:max-w-[420px]">
          <div class="rounded-2xl bg-slate-950 px-4 py-4 text-white shadow-lg shadow-slate-950/10">
            <div class="text-xs uppercase tracking-[0.2em] text-slate-300">
              当前流程
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
              实时状态
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
              最近刷新：{{ lastUpdatedLabel }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 顶部：玩家信息 + 段位 + 工厂挑战进度 -->
    <div class="flex flex-col gap-4 rounded-[24px] border border-slate-200/80 bg-white/90 p-4 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:rounded-3xl sm:p-6 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <h2 class="text-lg font-bold text-slate-900">
          训练师面板
        </h2>
        <p class="mt-2 text-sm leading-6 text-slate-500">
          段位、积分和挑战轮次会在战斗结算后自动刷新。手动对战、异步模拟和工厂挑战共用同一套战斗摘要。
        </p>
      </div>
      <div class="grid gap-3 grid-cols-2 lg:grid-cols-4">
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            玩家
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ currentUser }}
          </div>
        </div>
        <div class="rounded-xl px-4 py-3" :class="tierBgClass">
          <div class="text-xs" :class="tierTextClass">
            段位
          </div>
          <div class="mt-1 font-semibold" :class="tierTextClass">
            {{ tierDisplayName }}
          </div>
          <div class="mt-0.5 text-xs" :class="tierTextClass">
            {{ playerProfile?.tierPoints ?? 0 }} / 2000 分
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            总积分
          </div>
          <div class="mt-1 font-semibold text-slate-900">
            {{ playerProfile?.totalPoints ?? 0 }}
          </div>
          <div class="mt-0.5 text-xs text-slate-500">
            {{ playerProfile?.wins ?? 0 }}胜 / {{ playerProfile?.losses ?? 0 }}负
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-xs text-slate-500">
            当前状态
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

    <!-- 工厂挑战进度条 -->
    <div
      v-if="factoryRun"
      class="rounded-[24px] border border-sky-200 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(99,102,241,0.16),rgba(244,244,245,0.6))] p-4 shadow-[0_18px_60px_-46px_rgba(14,165,233,0.55)] sm:rounded-3xl"
    >
      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div class="text-sm font-semibold text-indigo-900">
          工厂挑战 · 第 {{ factoryRun.current_battle || 0 }} / {{ factoryRun.max_battles || 9 }} 轮
        </div>
        <div class="text-sm text-indigo-700">
          {{ factoryRun.wins || 0 }}胜 {{ factoryRun.losses || 0 }}负
        </div>
      </div>
      <div class="mt-2 flex gap-1">
        <div
          v-for="i in (factoryRun.max_battles || 9)"
          :key="i"
          class="h-2 flex-1 rounded-full"
          :class="factoryRoundClass(i)"
        />
      </div>
    </div>

    <div
      v-if="requestError"
      class="rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700 shadow-sm"
    >
      {{ requestError }}
    </div>

    <div class="grid gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(420px,0.9fr)] xl:gap-6">
      <BattleArena
        :summary="summary"
        :highlight-index="replacedHighlight"
        :status-text="statusText"
        :status-tone="statusTone"
      />

      <div class="space-y-4 rounded-[24px] border border-slate-200/80 bg-white/95 p-4 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:rounded-3xl sm:p-6">
        <section class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
                下一步
              </div>
              <div class="mt-2 text-base font-bold text-slate-900">
                {{ actionHeadline }}
              </div>
              <div class="mt-1 text-sm leading-6 text-slate-500">
                {{ actionDescription }}
              </div>
            </div>
            <div class="grid gap-2 text-sm sm:min-w-[220px] w-full sm:w-auto">
              <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
                <div class="text-xs text-slate-400">当前战斗</div>
                <div class="mt-1 font-semibold text-slate-900">{{ currentBattleId ? `#${currentBattleId}` : '未创建' }}</div>
              </div>
              <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
                <div class="text-xs text-slate-400">推荐模式</div>
                <div class="mt-1 font-semibold text-slate-900">{{ recommendedMode }}</div>
              </div>
            </div>
          </div>
        </section>

        <div class="grid grid-cols-1 gap-3 sm:flex sm:flex-wrap">
          <template v-if="!factoryRun && !currentBattleId">
            <button
              class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="isBusy"
              @click="startFactoryChallenge"
            >
              {{ busyAction === 'factory-start' ? '正在创建挑战...' : '开始工厂挑战（9 轮）' }}
            </button>
            <button
              class="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="isBusy"
              @click="startBattle"
            >
              {{ busyAction === 'start-manual' ? '正在创建战斗...' : '单场手动对战' }}
            </button>
            <button
              class="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="isBusy"
              @click="startAsyncBattle"
            >
              {{ busyAction === 'start-async' ? '正在提交模拟...' : '异步模拟' }}
            </button>
          </template>
          <template v-else-if="factoryRun && !currentBattleId">
            <button
              class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="isBusy"
              @click="nextFactoryBattle"
            >
              {{ busyAction === 'factory-next' ? '正在进入下一轮...' : `进入第 ${(factoryRun.current_battle || 0) + 1} 轮` }}
            </button>
            <button
              class="rounded-xl border border-rose-300 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
              :disabled="isBusy"
              @click="abandonFactoryRun"
            >
              放弃本次挑战
            </button>
          </template>
          <template v-else>
            <button
              class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
              :disabled="!currentBattleId || isBusy"
              @click="refreshStatus"
            >
              {{ busyAction === 'refresh-status' ? '刷新中...' : '刷新状态' }}
            </button>
            <button
              v-if="currentBattleId && summary?.status === 'running'"
              class="rounded-xl border border-rose-300 px-4 py-2 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
              :disabled="isBusy"
              @click="forfeitBattle"
            >
              {{ busyAction === 'forfeit-battle' ? '投降处理中...' : '投降' }}
            </button>
          </template>
          <button
            class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            :disabled="isBusy"
            @click="openLeaderboard"
          >
            排行榜
          </button>
          <button
            v-if="showContinueFactoryButton"
            class="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="isBusy"
            @click="prepareNextFactoryStage"
          >
            准备下一轮
          </button>
          <button
            v-if="showResetBattleButton"
            class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="isBusy"
            @click="resetBattleState({ keepFactoryRun: false })"
          >
            清空当前战场
          </button>
        </div>

        <section class="grid gap-3 md:grid-cols-3">
          <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">当前模式</div>
            <div class="mt-2 text-base font-bold text-slate-900">{{ modeSummary }}</div>
            <div class="mt-1 text-sm text-slate-500">{{ modeDescription }}</div>
          </div>
          <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">回合推进</div>
            <div class="mt-2 text-base font-bold text-slate-900">{{ summary?.currentRound || 0 }} / {{ summary?.roundLimit || '-' }}</div>
            <div class="mt-1 text-sm text-slate-500">{{ pollingActive ? '异步模式会自动刷新状态。' : '手动模式需要提交本回合动作。' }}</div>
          </div>
          <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">可执行操作</div>
            <div class="mt-2 text-base font-bold text-slate-900">{{ availableActionCount }}</div>
            <div class="mt-1 text-sm text-slate-500">{{ availableActionDescription }}</div>
          </div>
        </section>

        <section
          v-if="isPreviewPhase"
          class="rounded-2xl border border-amber-200 bg-[linear-gradient(180deg,rgba(254,243,199,0.7),rgba(255,255,255,0.95))] p-4"
        >
          <div class="mb-3 text-sm font-semibold text-slate-800">
            队伍预览：从 6 只里选择 4 只，并指定 2 只首发
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div>
              <div class="mb-2 text-xs font-semibold text-slate-500">
                你的队伍
              </div>
              <div class="space-y-2">
                <button
                  v-for="(pokemon, index) in playerRoster"
                  :key="`player-roster-${index}`"
                  type="button"
                  :class="previewCardClass(index)"
                  @click="toggleRoster(index)"
                >
                  <div class="flex items-center justify-between gap-3">
                    <div class="text-left">
                      <div class="font-semibold text-slate-900">
                        {{ pokemon.name || pokemon.name_en || `宝可梦 ${index + 1}` }}
                      </div>
                      <div class="text-xs text-slate-500">
                        {{ formatTypes(pokemon.types) }}
                      </div>
                    </div>
                    <div class="text-right text-xs text-slate-500">
                      <div>{{ isPicked(index) ? '已选入' : '未选入' }}</div>
                      <div>{{ isLead(index) ? '首发' : '后备' }}</div>
                    </div>
                  </div>
                </button>
              </div>
            </div>

            <div>
              <div class="mb-2 text-xs font-semibold text-slate-500">
                对手公开队伍
              </div>
              <div class="space-y-2">
                <div
                  v-for="(pokemon, index) in opponentRoster"
                  :key="`opponent-roster-${index}`"
                  class="rounded-xl border border-slate-200 bg-white p-3"
                >
                  <div class="font-semibold text-slate-900">
                    {{ pokemon.name || pokemon.name_en || `宝可梦 ${index + 1}` }}
                  </div>
                  <div class="text-xs text-slate-500">
                    {{ formatTypes(pokemon.types) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-xl bg-white p-4">
            <div class="text-sm text-slate-700">
              已选择 {{ selectedRosterIndexes.length }}/4 只；首发 {{ leadRosterIndexes.length }}/2 只
            </div>
            <div class="mt-2 flex flex-wrap gap-2">
              <button
                v-for="index in selectedRosterIndexes"
                :key="`lead-${index}`"
                type="button"
                class="rounded-full px-3 py-1 text-xs font-semibold"
                :class="isLead(index) ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-700'"
                @click="toggleLead(index)"
              >
                {{ playerRoster[index]?.name || playerRoster[index]?.name_en || `宝可梦 ${index + 1}` }}{{ isLead(index) ? ' · 首发' : '' }}
              </button>
            </div>
            <button
              class="mt-4 w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!canConfirmPreview || isBusy"
              @click="confirmPreview"
            >
              {{ busyAction === 'confirm-preview' ? '正在确认阵容...' : '确认 6 选 4 与首发' }}
            </button>
          </div>
        </section>

        <section
          v-if="isReplacementPhase"
          class="rounded-2xl border border-rose-200 bg-[linear-gradient(180deg,rgba(255,228,230,0.72),rgba(255,255,255,0.96))] p-4"
        >
          <div class="mb-3 text-sm font-semibold text-slate-800">
            倒下补位：请选择 {{ pendingReplacementCount }} 只后备宝可梦上场
          </div>
          <div class="space-y-2">
            <button
              v-for="option in replacementBenchOptions"
              :key="`replacement-${option.value}`"
              type="button"
              class="w-full rounded-xl border p-3 text-left"
              :class="selectedReplacementIndexes.includes(option.value) ? 'border-rose-500 bg-white' : 'border-slate-200 bg-white hover:border-slate-300'"
              @click="toggleReplacement(option.value)"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <div class="font-semibold text-slate-900">
                    {{ option.label }}
                  </div>
                  <div class="text-xs text-slate-500">
                    {{ option.types }}
                  </div>
                </div>
                <div class="text-xs text-slate-500">
                  HP {{ option.hp }}
                </div>
              </div>
            </button>
          </div>
          <button
            class="mt-4 w-full rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="!canConfirmReplacement || isBusy"
            @click="confirmReplacement"
          >
            {{ busyAction === 'confirm-replacement' ? '正在确认替补...' : '确认替补上场' }}
          </button>
        </section>

        <section class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div class="mb-3 text-sm font-semibold text-slate-800">
            {{ isReplacementPhase ? '当前回合已暂停，等待补位' : '当前可选招式' }}
          </div>
          <div
            v-if="playerActiveMons.length && !isPreviewPhase && !isReplacementPhase"
            class="space-y-4"
          >
            <div
              v-for="mon in playerActiveMons"
              :key="mon.fieldSlot"
              class="rounded-xl border border-slate-200 bg-white p-4"
            >
              <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
                <div>
                  <div class="font-semibold text-slate-900">
                    {{ mon.name }}
                  </div>
                  <div class="text-xs text-slate-500">
                    槽位 {{ mon.fieldSlot + 1 }} · HP {{ mon.currentHp }}/{{ mon.maxHp }}
                  </div>
                </div>
                <div class="text-xs text-slate-500">
                  {{ formatTypes(mon.types) }}
                </div>
              </div>

              <select
                v-model="selectedActions[`action-slot-${mon.fieldSlot}`]"
                class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
              >
                <option value="move">
                  使用招式
                </option>
                <option
                  value="switch"
                  :disabled="!playerBenchOptions.length"
                >
                  换人
                </option>
              </select>

              <template v-if="selectedActions[`action-slot-${mon.fieldSlot}`] === 'switch'">
                <select
                  v-model="selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`]"
                  class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  <option
                    v-for="target in playerBenchOptions"
                    :key="`switch-${mon.fieldSlot}-${target.value}`"
                    :value="target.value"
                  >
                    换上：{{ target.label }} · HP {{ target.hp }}
                  </option>
                </select>
              </template>
              <template v-else>
                <select
                  v-model="selectedMoves[`slot-${mon.fieldSlot}`]"
                  class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  <option
                    v-for="move in mon.moves"
                    :key="move.name_en || move.name"
                    :value="move.name_en || move.name"
                  >
                    {{ move.name || move.name_en }} · 威力 {{ move.power || 0 }} · 优先度 {{ move.priority || 0 }} · {{ moveTargetText(move) }}
                  </option>
                </select>

                <select
                  v-if="moveNeedsOpponentTarget(selectedMoveObject(mon))"
                  v-model="selectedTargets[`target-slot-${mon.fieldSlot}`]"
                  class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  <option
                    v-for="target in opponentActiveOptions"
                    :key="`target-${mon.fieldSlot}-${target.value}`"
                    :value="target.value"
                  >
                    目标：对手槽位 {{ target.value + 1 }} · {{ target.label }}
                  </option>
                </select>
              </template>
            </div>

            <button
              class="w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!canSubmitMove || isBusy"
              @click="submitMove"
            >
              {{ busyAction === 'submit-move' ? '正在提交回合...' : '提交当前回合' }}
            </button>
          </div>
          <div
            v-else
            class="text-sm text-slate-500"
          >
            {{ isPreviewPhase ? '先完成队伍预览后，才能提交回合操作。' : isReplacementPhase ? '有宝可梦倒下时，必须先完成替补上场。' : '先开始一场手动对战后，这里会显示你当前两只在场宝可梦的出招选择。' }}
          </div>
        </section>

        <details class="rounded-2xl border border-slate-200 bg-slate-50 p-4" :open="showDebugPanel" @toggle="showDebugPanel = $event.target.open">
          <summary class="cursor-pointer list-none text-sm font-semibold text-slate-800">
            调试响应
          </summary>
          <div class="mt-2 text-xs leading-5 text-slate-500">
            日常使用时可以收起；排查接口返回时再展开。
          </div>
          <pre class="mt-3 max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-slate-950 p-4 text-xs text-slate-100">{{ resultText }}</pre>
        </details>
      </div>
    </div>

    <div
      v-if="showMobileActionDock"
      class="fixed inset-x-0 bottom-0 z-40 border-t border-white/70 bg-white/90 px-3 pb-[calc(env(safe-area-inset-bottom,0px)+0.75rem)] pt-3 shadow-[0_-18px_60px_-34px_rgba(15,23,42,0.4)] backdrop-blur-xl md:hidden"
    >
      <div class="mx-auto flex max-w-7xl flex-col gap-2">
        <div class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
          快捷操作
        </div>
        <div class="grid gap-2" :class="mobileActionButtons.length > 1 ? 'grid-cols-2' : 'grid-cols-1'">
          <button
            v-for="action in mobileActionButtons"
            :key="action.key"
            class="rounded-2xl px-4 py-3 text-sm font-semibold transition disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400"
            :class="action.tone === 'primary' ? 'bg-slate-950 text-white hover:bg-slate-800' : action.tone === 'danger' ? 'bg-rose-600 text-white hover:bg-rose-700' : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50'"
            :disabled="action.disabled"
            @click="handleMobileAction(action.key)"
          >
            {{ action.label }}
          </button>
        </div>
      </div>
    </div>

    <ExchangeModal
      v-if="showExchange"
      v-model:replaced-index="replacedIndex"
      :opponent-team="exchangeCandidates"
      :max-slot="playerRoster.length || 6"
      :submitting="busyAction === 'confirm-exchange'"
      @close="showExchange = false"
      @confirm="onConfirmExchange"
    />

    <!-- 对战结算面板 -->
    <div
      v-if="settlement"
      class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-3 sm:items-center sm:p-6"
    >
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
            <span class="text-sm" :class="settlement.tierChange === 'promoted' ? 'text-amber-700' : 'text-rose-600'">
              {{ settlement.tierChange === 'promoted' ? '段位晋升！' : '段位下降' }}
            </span>
            <span class="font-semibold" :class="settlement.tierChange === 'promoted' ? 'text-amber-700' : 'text-rose-600'">
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
            @click="prepareNextFactoryStage"
          >
            继续下一轮
          </button>
          <button
            class="flex-1 rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            @click="onSettlementClose"
          >
            {{ settlement.runFinished || !factoryRun ? '返回' : '查看战场' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 排行榜 -->
    <div
      v-if="showLeaderboard"
      class="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-3 sm:items-center sm:p-6"
      @click.self="showLeaderboard = false"
    >
      <div class="w-full max-w-lg rounded-[24px] bg-white p-4 shadow-xl sm:rounded-2xl sm:p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-slate-900">
            大师球段位排行榜
          </h2>
          <button
            class="text-slate-400 hover:text-slate-600"
            @click="showLeaderboard = false"
          >
            ✕
          </button>
        </div>
        <div class="mt-4 max-h-96 space-y-2 overflow-y-auto">
          <div
            v-if="leaderboardLoading"
            class="py-8 text-center text-sm text-slate-500"
          >
            正在加载排行榜...
          </div>
          <div
            v-else-if="leaderboardData.length"
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
                {{ entry.totalPoints }} 分
              </div>
              <div class="text-xs text-slate-500">
                {{ entry.wins }}胜 {{ entry.losses }}负
              </div>
            </div>
          </div>
          <div
            v-else
            class="py-8 text-center text-sm text-slate-500"
          >
            暂无大师球段位玩家
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BattleArena from '../components/BattleArena.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import api from '../services/api'
import { useAuth } from '../composables/useAuth'

// 对战页本身不维护登录逻辑，只消费 useAuth 暴露的当前用户展示名。
const auth = useAuth()
const currentUser = computed(() => auth.displayName.value)

// 以下状态分别对应：后端原始返回、当前战斗摘要、玩家本回合操作、队伍预览选择、补位选择等 UI 状态。
const resultText = ref('等待开始对战')
const summary = ref(null)
const selectedActions = ref({})
const selectedMoves = ref({})
const selectedTargets = ref({})
const selectedSwitchTargets = ref({})
const selectedRosterIndexes = ref([])
const leadRosterIndexes = ref([])
const selectedReplacementIndexes = ref([])
const showExchange = ref(false)
const replacedIndex = ref(0)
const replacedHighlight = ref(-1)
const currentBattleId = ref(null)
const busyAction = ref('')
const requestError = ref('')
const pollingActive = ref(false)
const lastUpdatedAt = ref(null)
const showDebugPanel = ref(false)
const leaderboardLoading = ref(false)
let pollTimer = null

// 工厂挑战 & 玩家信息
const factoryRun = ref(null)
const playerProfile = ref(null)
const settlement = ref(null)
const showLeaderboard = ref(false)
const leaderboardData = ref([])

const TIER_NAMES = ['精灵球', '超级球', '高级球', '大师球']
const tierDisplayName = computed(() => TIER_NAMES[playerProfile.value?.tier ?? 0] || '精灵球')
const isBusy = computed(() => Boolean(busyAction.value))
const statusTone = computed(() => {
  if (summary.value?.status === 'completed') {
    return summary.value?.winner === 'player' ? 'success' : 'danger'
  }
  if (isPreviewPhase.value) return 'warning'
  if (isReplacementPhase.value) return 'danger'
  if (summary.value?.status === 'running') return 'info'
  return 'neutral'
})
const actionHeadline = computed(() => {
  if (!currentBattleId.value) {
    return factoryRun.value ? '准备进入下一轮' : '选择一种开始方式'
  }
  if (isPreviewPhase.value) return '先完成 6 选 4 与首发'
  if (isReplacementPhase.value) return `需要补位 ${pendingReplacementCount.value} 只`
  if (summary.value?.status === 'completed') {
    return summary.value?.winner === 'player' ? '本场获胜，检查结算与奖励' : '本场结束，准备下一步'
  }
  if (pollingActive.value) return '等待异步模拟推进'
  return '为当前回合配置动作'
})
const actionDescription = computed(() => {
  if (!currentBattleId.value) {
    return factoryRun.value
      ? '当前已有一个激活中的工厂挑战，但还未进入下一战。可以继续推进，也可以直接放弃这次挑战。'
      : '如果你想完整体验工厂流程，直接开始 9 连战；如果只是验证战斗逻辑，单场手动或异步模拟更快。'
  }
  if (isPreviewPhase.value) {
    return '从你的 6 只宝可梦中选 4 只参战，再从中指定 2 只作为首发。只有完成这一阶段后，回合操作区才会解锁。'
  }
  if (isReplacementPhase.value) {
    return '当前场上有宝可梦倒下，必须先补位才能继续推进战斗。前端已经按后端允许列表限制了可选替补。'
  }
  if (summary.value?.status === 'completed') {
    return factoryRun.value
      ? '查看积分变化、段位浮动和本轮结果。若挑战尚未结束，可以继续推进下一轮。'
      : '可以复盘战斗日志，也可以直接开启下一场。胜利时如果触发交换奖励，会自动弹出交换面板。'
  }
  if (pollingActive.value) {
    return '当前是后台自动模拟，页面会持续轮询并在结束时自动停下。你仍然可以手动点刷新，或直接等待结果。'
  }
  return '为场上的两只宝可梦分别选择出招或换人；如果招式需要指定单体目标，目标选择会自动出现。'
})
const progressSummary = computed(() => {
  if (factoryRun.value) {
    return `工厂挑战 ${factoryRun.value.wins || 0} 胜 ${factoryRun.value.losses || 0} 负，第 ${factoryRun.value.current_battle || 0} / ${factoryRun.value.max_battles || 9} 轮。`
  }
  if (summary.value?.status === 'completed') {
    return `本场已结束，${summary.value?.winner === 'player' ? '玩家取得胜利。' : '对手取得胜利。'}`
  }
  if (currentBattleId.value) {
    return `当前战斗编号 #${currentBattleId.value}，${summary.value?.currentRound || 0} 回合已推进。`
  }
  return '当前没有进行中的战斗，可以从右侧操作区直接开始。'
})
const recommendedMode = computed(() => {
  if (factoryRun.value) return '继续工厂挑战'
  if (currentBattleId.value && pollingActive.value) return '观察异步模拟'
  if (currentBattleId.value) return '继续当前对战'
  return '开始 9 连战'
})
const modeSummary = computed(() => {
  if (factoryRun.value) return '工厂挑战'
  if (pollingActive.value) return '异步模拟'
  if (currentBattleId.value) return '手动对战'
  return '待开始'
})
const modeDescription = computed(() => {
  if (factoryRun.value) return '工厂模式会保留挑战进度，并在每轮结束后结算积分。'
  if (pollingActive.value) return '后台自动推进战斗，适合快速检验队伍生成和结算链路。'
  if (currentBattleId.value) return '每个回合都需要你手动提交动作，适合检查细节。'
  return '选择开始方式后，这里会自动显示当前战斗模式。'
})
const availableActionCount = computed(() => {
  if (!currentBattleId.value) return factoryRun.value ? 2 : 4
  if (isPreviewPhase.value) return canConfirmPreview.value ? 1 : 0
  if (isReplacementPhase.value) return canConfirmReplacement.value ? 1 : 0
  if (summary.value?.status === 'completed') return factoryRun.value ? 2 : 1
  return canSubmitMove.value ? 2 : 1
})
const availableActionDescription = computed(() => {
  if (!currentBattleId.value) return '开始、继续、放弃、查看排行榜都在当前面板可直接完成。'
  if (isPreviewPhase.value) return canConfirmPreview.value ? '阵容已满足条件，可以直接确认。' : '还未满足 4 只参战与 2 只首发的条件。'
  if (isReplacementPhase.value) return canConfirmReplacement.value ? '补位人数已满足条件，可以继续战斗。' : '需要先补齐替补人数。'
  if (summary.value?.status === 'completed') return '可以查看结算、继续下一轮或重新开始。'
  return canSubmitMove.value ? '当前回合动作已完整，可以提交或刷新状态。' : '还需要先补齐行动、目标或换人对象。'
})
const showContinueFactoryButton = computed(() => Boolean(factoryRun.value && currentBattleId.value && summary.value?.status === 'completed' && !settlement.value?.runFinished))
const showResetBattleButton = computed(() => Boolean(currentBattleId.value && summary.value?.status === 'completed'))
const mobileActionButtons = computed(() => {
  if (!currentBattleId.value && !factoryRun.value) {
    return [
      { key: 'start-factory', label: '开始挑战', tone: 'primary', disabled: isBusy.value },
      { key: 'start-manual', label: '手动对战', tone: 'secondary', disabled: isBusy.value },
      { key: 'start-async', label: '异步模拟', tone: 'secondary', disabled: isBusy.value }
    ]
  }
  if (factoryRun.value && !currentBattleId.value) {
    return [
      { key: 'next-factory', label: '进入下一轮', tone: 'primary', disabled: isBusy.value },
      { key: 'abandon-factory', label: '放弃挑战', tone: 'danger', disabled: isBusy.value }
    ]
  }
  if (isPreviewPhase.value) {
    return [
      { key: 'confirm-preview', label: '确认预览选择', tone: 'primary', disabled: isBusy.value || !canConfirmPreview.value },
      { key: 'refresh-status', label: '刷新状态', tone: 'secondary', disabled: isBusy.value }
    ]
  }
  if (isReplacementPhase.value) {
    return [
      { key: 'confirm-replacement', label: '确认替补', tone: 'primary', disabled: isBusy.value || !canConfirmReplacement.value },
      { key: 'refresh-status', label: '刷新状态', tone: 'secondary', disabled: isBusy.value }
    ]
  }
  if (summary.value?.status === 'completed') {
    if (showContinueFactoryButton.value) {
      return [
        { key: 'prepare-next', label: '准备下一轮', tone: 'primary', disabled: isBusy.value },
        { key: 'reset-battle', label: '清空战场', tone: 'secondary', disabled: isBusy.value }
      ]
    }
    return [
      { key: 'reset-battle', label: '清空战场', tone: 'primary', disabled: isBusy.value },
      { key: 'open-leaderboard', label: '看排行榜', tone: 'secondary', disabled: isBusy.value }
    ]
  }
  return [
    { key: 'submit-move', label: '提交回合', tone: 'primary', disabled: isBusy.value || !canSubmitMove.value },
    { key: 'refresh-status', label: '刷新状态', tone: 'secondary', disabled: isBusy.value }
  ]
})
const showMobileActionDock = computed(() => mobileActionButtons.value.length > 0)
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return ''
  return new Date(lastUpdatedAt.value).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

// 统一工厂挑战对象字段名（后端两处返回格式不同：startRun 用 camelCase，findActiveRun 用 snake_case）
function normalizeRun(raw) {
  if (!raw) return null
  return {
    id: raw.id,
    current_battle: raw.current_battle ?? raw.currentBattle ?? 0,
    max_battles: raw.max_battles ?? raw.maxBattles ?? 9,
    current_battle_id: raw.current_battle_id ?? raw.currentBattleId ?? null,
    wins: raw.wins ?? 0,
    losses: raw.losses ?? 0,
    status: raw.status,
    team_json: raw.team_json ?? raw.teamJson
  }
}
const tierBgClass = computed(() => {
  switch (playerProfile.value?.tier) {
    case 3: return 'bg-purple-50'
    case 2: return 'bg-amber-50'
    case 1: return 'bg-blue-50'
    default: return 'bg-slate-50'
  }
})
const tierTextClass = computed(() => {
  switch (playerProfile.value?.tier) {
    case 3: return 'text-purple-700'
    case 2: return 'text-amber-700'
    case 1: return 'text-blue-700'
    default: return 'text-slate-700'
  }
})

// 对战当前处于哪个阶段，完全以后端 summary 返回的状态为准。
const isPreviewPhase = computed(() => summary.value?.status === 'preview' || summary.value?.phase === 'team-preview')
const isReplacementPhase = computed(() => summary.value?.phase === 'replacement')
const playerTeam = computed(() => summary.value?.playerTeam || [])
const playerRoster = computed(() => summary.value?.playerRoster || [])
const opponentRoster = computed(() => summary.value?.opponentRoster || [])
const exchangeCandidates = computed(() => opponentRoster.value.length ? opponentRoster.value : (summary.value?.opponentTeam || []))
const pendingReplacementCount = computed(() => Number(summary.value?.playerPendingReplacementCount || 0))

// 顶部状态文案根据当前阶段动态生成，避免模板里堆过多条件分支。
const statusText = computed(() => {
  if (!summary.value) return '未开始'
  if (isPreviewPhase.value) return '队伍预览中'
  if (isReplacementPhase.value) return '补位选择中'
  if (summary.value.status === 'completed') {
    return `已结束 · ${summary.value.winner === 'player' ? '玩家胜利' : '对手胜利'}`
  }
  return `进行中 · 第 ${summary.value.currentRound || 0} 回合`
})

// 把后端给出的当前在场槽位映射成前端更容易渲染的结构，顺便补上 fieldSlot / maxHp。
const playerActiveMons = computed(() => {
  const activeSlots = summary.value?.playerActiveSlots || []
  return activeSlots.map((teamIndex, fieldSlot) => {
    const mon = playerTeam.value?.[teamIndex]
    if (!mon) return null
    return {
      ...mon,
      teamIndex,
      fieldSlot,
      maxHp: mon?.stats?.hp || mon?.currentHp || 0
    }
  }).filter(Boolean)
})

// 可被选中的对手目标只来自当前仍在场的敌方槽位。
const opponentActiveOptions = computed(() => {
  const activeSlots = summary.value?.opponentActiveSlots || []
  const opponentTeam = summary.value?.opponentTeam || []
  return activeSlots.map((teamIndex, fieldSlot) => ({
    value: fieldSlot,
    label: opponentTeam?.[teamIndex]?.name || opponentTeam?.[teamIndex]?.name_en || `对手 ${fieldSlot + 1}`
  }))
})

// 换人候选只能从存活且当前不在场的己方宝可梦里选。
const playerBenchOptions = computed(() => {
  const activeSlots = summary.value?.playerActiveSlots || []
  return playerTeam.value
    .map((pokemon, teamIndex) => ({
      value: teamIndex,
      label: pokemon?.name || pokemon?.name_en || `替补 ${teamIndex + 1}`,
      hp: pokemon?.currentHp || 0
    }))
    .filter((pokemon) => pokemon.hp > 0 && !activeSlots.includes(pokemon.value))
})

// 补位阶段进一步受后端允许列表约束，避免前端展示出后端不接受的替补对象。
const replacementBenchOptions = computed(() => {
  const allowed = new Set(summary.value?.playerPendingReplacementOptions || [])
  return playerBenchOptions.value
    .filter((pokemon) => allowed.has(pokemon.value))
    .map((pokemon) => ({
      ...pokemon,
      types: formatTypes(playerTeam.value?.[pokemon.value]?.types)
    }))
})

const canConfirmPreview = computed(() => {
  return selectedRosterIndexes.value.length === 4
    && leadRosterIndexes.value.length === 2
    && leadRosterIndexes.value.every((index) => selectedRosterIndexes.value.includes(index))
})

const canSubmitMove = computed(() => {
  if (!currentBattleId.value || summary.value?.status !== 'running' || !playerActiveMons.value.length || isReplacementPhase.value) {
    return false
  }
  const usedSwitchTargets = new Set()
  return playerActiveMons.value.every((mon) => {
    const actionType = selectedActions.value[`action-slot-${mon.fieldSlot}`] || 'move'
    if (actionType === 'switch') {
      const switchTarget = selectedSwitchTargets.value[`switch-slot-${mon.fieldSlot}`]
      if (switchTarget === undefined || switchTarget === null || usedSwitchTargets.has(switchTarget)) {
        return false
      }
      usedSwitchTargets.add(switchTarget)
      return true
    }
    const move = selectedMoveObject(mon)
    if (!move) {
      return false
    }
    if (!moveNeedsOpponentTarget(move)) {
      return true
    }
    return selectedTargets.value[`target-slot-${mon.fieldSlot}`] !== undefined
  })
})

const canConfirmReplacement = computed(() => {
  return currentBattleId.value
    && isReplacementPhase.value
    && selectedReplacementIndexes.value.length === pendingReplacementCount.value
})

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  pollingActive.value = false
}

function resetLocalSelections() {
  selectedActions.value = {}
  selectedMoves.value = {}
  selectedTargets.value = {}
  selectedSwitchTargets.value = {}
  selectedRosterIndexes.value = []
  leadRosterIndexes.value = []
  selectedReplacementIndexes.value = []
}

function resetBattleState(options = {}) {
  const { keepFactoryRun = true, keepResultText = false } = options
  stopPolling()
  currentBattleId.value = null
  summary.value = null
  settlement.value = null
  showExchange.value = false
  replacedHighlight.value = -1
  requestError.value = ''
  if (!keepFactoryRun) {
    factoryRun.value = null
  }
  if (!keepResultText) {
    resultText.value = '等待开始对战'
  }
  resetLocalSelections()
  markUpdated()
}

function prepareNextFactoryStage() {
  settlement.value = null
  currentBattleId.value = null
  summary.value = null
  showExchange.value = false
  resetLocalSelections()
  requestError.value = ''
  resultText.value = '当前轮次已完成，可以进入下一轮。'
  markUpdated()
}

function handleMobileAction(actionKey) {
  switch (actionKey) {
    case 'start-factory':
      startFactoryChallenge()
      break
    case 'start-manual':
      startBattle()
      break
    case 'start-async':
      startAsyncBattle()
      break
    case 'next-factory':
      nextFactoryBattle()
      break
    case 'abandon-factory':
      abandonFactoryRun()
      break
    case 'confirm-preview':
      confirmPreview()
      break
    case 'confirm-replacement':
      confirmReplacement()
      break
    case 'submit-move':
      submitMove()
      break
    case 'prepare-next':
      prepareNextFactoryStage()
      break
    case 'reset-battle':
      resetBattleState({ keepFactoryRun: false })
      break
    case 'open-leaderboard':
      openLeaderboard()
      break
    case 'refresh-status':
      refreshStatus()
      break
    default:
      break
  }
}

function markUpdated() {
  lastUpdatedAt.value = Date.now()
}

async function runBusy(actionKey, handler) {
  if (busyAction.value) {
    return
  }
  busyAction.value = actionKey
  requestError.value = ''
  try {
    await handler()
  } catch (error) {
    requestError.value = error?.message || String(error)
    throw error
  } finally {
    busyAction.value = ''
  }
}

// 队伍预览阶段，如果后端已进入 preview 但前端还没有本地选择，就给出默认的 4 选 2 初始值。
function initializePreviewSelections() {
  if (!playerRoster.value.length) return
  if (selectedRosterIndexes.value.length !== 4) {
    selectedRosterIndexes.value = playerRoster.value.slice(0, 4).map((_, index) => index)
  }
  if (leadRosterIndexes.value.length !== 2) {
    leadRosterIndexes.value = selectedRosterIndexes.value.slice(0, 2)
  }
}

// 补位阶段每次刷新状态后，都需要把已经失效的选择从本地状态里剔除掉。
function ensureReplacementSelections() {
  const available = replacementBenchOptions.value.map((option) => option.value)
  selectedReplacementIndexes.value = selectedReplacementIndexes.value.filter((index) => available.includes(index))
  if (selectedReplacementIndexes.value.length > pendingReplacementCount.value) {
    selectedReplacementIndexes.value = selectedReplacementIndexes.value.slice(0, pendingReplacementCount.value)
  }
}

// 根据当前在场宝可梦、可选目标和替补席情况，自动修正本地动作选择，避免界面保留过期值。
function ensureMoveSelections() {
  const actionNext = { ...selectedActions.value }
  const moveNext = { ...selectedMoves.value }
  const targetNext = { ...selectedTargets.value }
  const switchNext = { ...selectedSwitchTargets.value }
  for (const mon of playerActiveMons.value) {
    const actionKey = `action-slot-${mon.fieldSlot}`
    const moveKey = `slot-${mon.fieldSlot}`
    const targetKey = `target-slot-${mon.fieldSlot}`
    const switchKey = `switch-slot-${mon.fieldSlot}`
    if (!['move', 'switch'].includes(actionNext[actionKey])) {
      actionNext[actionKey] = 'move'
    }
    const moveNames = (mon.moves || []).map((move) => move.name_en || move.name)
    if (!moveNames.length) {
      delete moveNext[moveKey]
    } else if (!moveNames.includes(moveNext[moveKey])) {
      moveNext[moveKey] = moveNames[0]
    }

    const selectedMove = (mon.moves || []).find((move) => (move.name_en || move.name) === moveNext[moveKey])
    const targetValues = opponentActiveOptions.value.map((target) => target.value)
    if (!moveNeedsOpponentTarget(selectedMove)) {
      delete targetNext[targetKey]
    } else if (!targetValues.length) {
      delete targetNext[targetKey]
    } else if (!targetValues.includes(targetNext[targetKey])) {
      targetNext[targetKey] = targetValues[Math.min(mon.fieldSlot, targetValues.length - 1)]
    }

    const switchValues = playerBenchOptions.value.map((target) => target.value)
    if (!switchValues.length) {
      delete switchNext[switchKey]
      actionNext[actionKey] = 'move'
    } else if (!switchValues.includes(switchNext[switchKey])) {
      switchNext[switchKey] = switchValues[0]
    }
  }
  selectedActions.value = actionNext
  selectedMoves.value = moveNext
  selectedTargets.value = targetNext
  selectedSwitchTargets.value = switchNext
}

function selectedMoveObject(mon) {
  const moveName = selectedMoves.value[`slot-${mon.fieldSlot}`]
  return (mon?.moves || []).find((move) => (move.name_en || move.name) === moveName) || null
}

// 当前只把需要明确指定对手单体的招式视为“必须额外选择目标”。
function moveNeedsOpponentTarget(move) {
  const targetId = Number(move?.target_id || 10)
  return targetId === 10
}

function moveTargetText(move) {
  const targetId = Number(move?.target_id || 10)
  switch (targetId) {
    case 7:
      return '目标：自身'
    case 8:
      return '目标：随机对手'
    case 9:
      return '目标：场上其他宝可梦'
    case 11:
      return '目标：对手全体'
    case 13:
      return '目标：自身与队友'
    case 14:
      return '目标：场上全体'
    default:
      return '目标：单体'
  }
}

// 后端在不同接口里返回 summary 的层级略有差异，这里统一规整成 Battle.vue 使用的一份状态。
function applyBattlePayload(payload) {
  const nextSummary = payload?.summary || payload?.battle?.summary || null
  if (!nextSummary && payload?.battle?.summary_json) {
    try {
      summary.value = typeof payload.battle.summary_json === 'string' ? JSON.parse(payload.battle.summary_json) : payload.battle.summary_json
    } catch {
      summary.value = null
    }
  } else {
    summary.value = nextSummary
  }

  if (payload?.battleId) {
    currentBattleId.value = payload.battleId
  } else if (payload?.battle?.id) {
    currentBattleId.value = payload.battle.id
  }

  markUpdated()

  if (isPreviewPhase.value) {
    initializePreviewSelections()
  } else if (isReplacementPhase.value) {
    ensureReplacementSelections()
  } else {
    ensureMoveSelections()
  }

  // 胜利且满足交换条件时，弹出交换面板，作为当前流程的额外奖励环节。
  if (summary.value?.status === 'completed' && summary.value?.winner === 'player' && summary.value?.exchangeAvailable) {
    replacedIndex.value = 0
    showExchange.value = true
  }

  // 对战结束时生成结算信息
  if (summary.value?.status === 'completed') {
    const factory = summary.value?.factory || {}
    const meta = payload?.factoryMeta || factory
    const won = summary.value.winner === 'player'

    // 判断段位变化
    let tierChange = null
    let newTierName = null
    if (meta.promoted) {
      tierChange = 'promoted'
      newTierName = meta.playerTierName || TIER_NAMES[meta.playerTier] || ''
    } else if (meta.demoted) {
      tierChange = 'demoted'
      newTierName = meta.playerTierName || TIER_NAMES[meta.playerTier] || ''
    }

    settlement.value = {
      won,
      pointsDelta: meta.pointsDelta ?? null,
      tierChange,
      newTierName,
      factoryRound: meta.runBattleNumber ?? null,
      runFinished: meta.runFinished ?? false,
      runWins: meta.runWins ?? null,
      runLosses: meta.runLosses ?? null,
      runReward: meta.runReward ?? null
    }
    // 刷新工厂挑战状态
    loadFactoryStatus()
    loadProfile()
  }
}

async function startBattle() {
  await runBusy('start-manual', async () => {
    stopPolling()
    resultText.value = '正在开始手动对战...'
    const res = await api.battle.start({})
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `开始失败: ${error.message || error}`
  })
}

async function startAsyncBattle() {
  await runBusy('start-async', async () => {
    stopPolling()
    resultText.value = '正在提交异步模拟...'
    const res = await api.battle.startAsync({})
    currentBattleId.value = res.battleId
    resultText.value = JSON.stringify(res, null, 2)
    await refreshStatus(true)
    startPolling()
  }).catch((error) => {
    resultText.value = `提交失败: ${error.message || error}`
  })
}

async function refreshStatus(silent = false) {
  if (!currentBattleId.value) {
    resultText.value = '请先开始对战'
    return
  }
  const task = async () => {
    const res = await api.battle.status(currentBattleId.value)
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
    if (summary.value?.status === 'completed') {
      stopPolling()
    }
  }
  if (silent) {
    try {
      await task()
    } catch (error) {
      requestError.value = error?.message || String(error)
      resultText.value = `刷新失败: ${error.message || error}`
    }
    return
  }
  await runBusy('refresh-status', task).catch((error) => {
    resultText.value = `刷新失败: ${error.message || error}`
  })
}

// 异步模拟模式下定时刷新状态；一旦结束就停止轮询，避免页面持续空转请求。
function startPolling() {
  stopPolling()
  pollingActive.value = true
  pollTimer = setInterval(async () => {
    await refreshStatus(true)
  }, 2000)
}

async function confirmPreview() {
  if (!canConfirmPreview.value) {
    resultText.value = '请选择 4 只宝可梦，并从中指定 2 只首发'
    return
  }
  await runBusy('confirm-preview', async () => {
    const res = await api.battle.preview(currentBattleId.value, {
      pickedRosterIndexes: selectedRosterIndexes.value,
      leadRosterIndexes: leadRosterIndexes.value
    })
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `确认失败: ${error.message || error}`
  })
}

async function submitMove() {
  if (!canSubmitMove.value) {
    resultText.value = '请为两只在场宝可梦分别选择行动；若使用招式还需指定目标'
    return
  }
  await runBusy('submit-move', async () => {
    // battleFactory 当前使用 playerMoveMap 这一扁平结构承载双打两只上场宝可梦的动作。
    const playerMoveMap = {}
    for (const mon of playerActiveMons.value) {
      const actionType = selectedActions.value[`action-slot-${mon.fieldSlot}`] || 'move'
      playerMoveMap[`action-slot-${mon.fieldSlot}`] = actionType
      if (actionType === 'switch') {
        playerMoveMap[`switch-slot-${mon.fieldSlot}`] = String(selectedSwitchTargets.value[`switch-slot-${mon.fieldSlot}`])
      } else {
        playerMoveMap[`slot-${mon.fieldSlot}`] = selectedMoves.value[`slot-${mon.fieldSlot}`]
        if (moveNeedsOpponentTarget(selectedMoveObject(mon))) {
          playerMoveMap[`target-slot-${mon.fieldSlot}`] = String(selectedTargets.value[`target-slot-${mon.fieldSlot}`])
        }
      }
    }
    const res = await api.battle.move(currentBattleId.value, {
      playerMoveMap
    })
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `提交失败: ${error.message || error}`
  })
}

async function confirmReplacement() {
  if (!canConfirmReplacement.value) {
    resultText.value = `请选择 ${pendingReplacementCount.value} 只后备宝可梦上场`
    return
  }
  await runBusy('confirm-replacement', async () => {
    const res = await api.battle.replacement(currentBattleId.value, {
      replacementIndexes: selectedReplacementIndexes.value
    })
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `补位失败: ${error.message || error}`
  })
}

async function onConfirmExchange(pickedIdx) {
  const picked = exchangeCandidates.value[pickedIdx]
  if (!picked) {
    resultText.value = '未找到可交换的宝可梦'
    return
  }
  await runBusy('confirm-exchange', async () => {
    const res = await api.battle.exchange({
      battleId: currentBattleId.value,
      replacedIndex: replacedIndex.value,
      newPokemonJson: JSON.stringify(picked)
    })
    showExchange.value = false
    replacedHighlight.value = replacedIndex.value
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
    setTimeout(() => {
      replacedHighlight.value = -1
    }, 4000)
  }).catch((error) => {
    resultText.value = `交换失败: ${error.message || error}`
  })
}

// 队伍预览阶段最多选 4 只；如果取消选择，也同步把首发标记移除。
function toggleRoster(index) {
  if (selectedRosterIndexes.value.includes(index)) {
    selectedRosterIndexes.value = selectedRosterIndexes.value.filter((item) => item !== index)
    leadRosterIndexes.value = leadRosterIndexes.value.filter((item) => item !== index)
    return
  }
  if (selectedRosterIndexes.value.length >= 4) {
    return
  }
  selectedRosterIndexes.value = [...selectedRosterIndexes.value, index]
}

// 首发必须从已选的 4 只里产生，且最多保留 2 只。
function toggleLead(index) {
  if (!selectedRosterIndexes.value.includes(index)) {
    return
  }
  if (leadRosterIndexes.value.includes(index)) {
    leadRosterIndexes.value = leadRosterIndexes.value.filter((item) => item !== index)
    return
  }
  if (leadRosterIndexes.value.length >= 2) {
    leadRosterIndexes.value = [leadRosterIndexes.value[1], index]
    return
  }
  leadRosterIndexes.value = [...leadRosterIndexes.value, index]
}

// 补位阶段按后端要求数量精确选择替补。
function toggleReplacement(index) {
  if (!replacementBenchOptions.value.some((option) => option.value === index)) {
    return
  }
  if (selectedReplacementIndexes.value.includes(index)) {
    selectedReplacementIndexes.value = selectedReplacementIndexes.value.filter((item) => item !== index)
    return
  }
  if (selectedReplacementIndexes.value.length >= pendingReplacementCount.value) {
    return
  }
  selectedReplacementIndexes.value = [...selectedReplacementIndexes.value, index]
}

function isPicked(index) {
  return selectedRosterIndexes.value.includes(index)
}

function isLead(index) {
  return leadRosterIndexes.value.includes(index)
}

function previewCardClass(index) {
  if (isLead(index)) {
    return 'w-full rounded-xl border border-indigo-500 bg-indigo-50 p-3 text-left'
  }
  if (isPicked(index)) {
    return 'w-full rounded-xl border border-blue-500 bg-blue-50 p-3 text-left'
  }
  return 'w-full rounded-xl border border-slate-200 bg-white p-3 text-left hover:border-slate-300'
}

function formatTypes(types) {
  return (types || []).map((type) => type.name || type.name_zh || `属性${type.type_id}`).join(' / ') || '未知属性'
}

// ---------- 工厂挑战进度条颜色 ----------
function factoryRoundClass(i) {
  if (!factoryRun.value) return 'bg-slate-200'
  const done = factoryRun.value.current_battle || 0
  if (i <= done) {
    // 已完成的轮次根据胜负记录着色（简单方式：已打完的都标一个颜色）
    return 'bg-indigo-500'
  }
  if (i === done + 1 && currentBattleId.value) return 'bg-indigo-300 animate-pulse'
  return 'bg-slate-200'
}

// ---------- 玩家信息加载 ----------
async function loadProfile() {
  try {
    const res = await api.battle.profile()
    // 后端返回 { profile: { tier, tierPoints, ... }, activeRun, ... }
    playerProfile.value = res?.profile || res
  } catch {
    // 忽略——可能还没打过
  }
}

async function loadFactoryStatus() {
  try {
    const res = await api.battle.factoryStatus()
    const run = normalizeRun(res?.activeRun || res)
    if (run && run.id) {
      factoryRun.value = run
    } else {
      factoryRun.value = null
    }
  } catch {
    factoryRun.value = null
  }
}

async function loadLeaderboard() {
  try {
    leaderboardLoading.value = true
    leaderboardData.value = await api.battle.leaderboard() || []
  } catch {
    leaderboardData.value = []
  } finally {
    leaderboardLoading.value = false
  }
}

async function openLeaderboard() {
  showLeaderboard.value = true
  await runBusy('open-leaderboard', loadLeaderboard).catch(() => {})
}

// ---------- 工厂挑战流程 ----------
async function startFactoryChallenge() {
  await runBusy('factory-start', async () => {
    stopPolling()
    resultText.value = '正在开始工厂挑战...'
    const res = await api.battle.factoryStart()
    factoryRun.value = normalizeRun(res.run || res)
    if (res.battleId || res.battle?.id) {
      currentBattleId.value = res.battleId || res.battle?.id
      applyBattlePayload(res)
    }
    resultText.value = JSON.stringify(res, null, 2)
    await loadProfile()
  }).catch((error) => {
    resultText.value = `开始挑战失败: ${error.message || error}`
  })
}

async function nextFactoryBattle() {
  if (!factoryRun.value?.id) return
  await runBusy('factory-next', async () => {
    stopPolling()
    resultText.value = '正在进入下一轮...'
    const res = await api.battle.factoryNext(factoryRun.value.id)
    if (res.run) factoryRun.value = normalizeRun(res.run)
    if (res.battleId || res.battle?.id) {
      currentBattleId.value = res.battleId || res.battle?.id
      applyBattlePayload(res)
    }
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `进入下一轮失败: ${error.message || error}`
  })
}

async function abandonFactoryRun() {
  await runBusy('factory-abandon', async () => {
    await api.battle.factoryAbandon()
    resetBattleState({ keepFactoryRun: false, keepResultText: true })
    resultText.value = '已放弃本次工厂挑战'
    await loadProfile()
  }).catch((error) => {
    resultText.value = `放弃失败: ${error.message || error}`
  })
}

async function forfeitBattle() {
  if (!currentBattleId.value) return
  await runBusy('forfeit-battle', async () => {
    const res = await api.battle.forfeit(currentBattleId.value)
    applyBattlePayload(res)
    resultText.value = JSON.stringify(res, null, 2)
  }).catch((error) => {
    resultText.value = `投降失败: ${error.message || error}`
  })
}

// 结算面板关闭后的善后操作
function onSettlementClose() {
  const wasRunFinished = settlement.value?.runFinished
  settlement.value = null
  if (wasRunFinished || !factoryRun.value) {
    resetBattleState({ keepFactoryRun: false, keepResultText: true })
    resultText.value = '等待开始对战'
    loadProfile()
  } else if (summary.value?.status === 'completed') {
    resultText.value = '本轮已完成，可以查看战场详情或继续下一轮。'
  }
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadFactoryStatus()])
  // 如果有进行中的工厂挑战，尝试恢复
  if (factoryRun.value?.current_battle_id) {
    currentBattleId.value = factoryRun.value.current_battle_id
    await refreshStatus()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.battle-hero {
  background:
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.46), transparent 28%),
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.24), transparent 24%),
    linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.86) 48%, rgba(224, 242, 254, 0.95));
}

@media (max-width: 640px) {
  .battle-hero {
    background:
      radial-gradient(circle at top left, rgba(125, 211, 252, 0.4), transparent 34%),
      linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(255, 255, 255, 0.92) 55%, rgba(224, 242, 254, 0.94));
  }
}
</style>
