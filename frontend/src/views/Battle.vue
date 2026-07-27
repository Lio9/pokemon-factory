

<template>
  <div class="space-y-6 pb-24 md:pb-0">
    <!-- 纯文字模式开关 -->
    <div class="flex justify-end">
      <button 
        class="px-4 py-1.5 text-xs font-bold rounded-xl border-2 transition-all shadow-poke" 
        :class="isTextMode ? 'bg-slate-800 text-white border-slate-700' : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'"
        @click="toggleTextMode"
      >
        {{ isTextMode ? tr('切换回图形模式', 'Switch to Graphic Mode') : tr('开启纯文字摸鱼模式', 'Enable Text-only Mode') }}
      </button>
    </div>

    <!-- 纯文字显示区域 -->
    <div
      v-if="isTextMode"
      ref="textLogContainer"
      class="rounded-3xl border-3 border-slate-200 bg-black p-6 shadow-poke-card font-mono text-sm leading-relaxed overflow-auto max-h-[80vh]"
    >
      <div
        v-for="(log, index) in textLogs"
        :key="index"
        class="mb-1"
        :class="getLogClass(log)"
      >
        {{ log.content }}
      </div>
      <div
        v-if="!textLogs.length"
        class="text-slate-500"
      >
        {{ tr('等待战斗开始...', 'Waiting for battle to start...') }}
      </div>
    </div>

    <!-- 原有图形界面 -->
    <template v-else>
      <BattleHeaderPanel
        :action-headline="actionHeadline"
        :action-description="actionDescription"
        :current-user="currentUser"
        :factory-round-class="factoryRoundClass"
        :factory-run="factoryRun"
        :last-updated-label="lastUpdatedLabel"
        :player-profile="playerProfile"
        :polling-active="pollingActive"
        :progress-summary="progressSummary"
        :request-error="requestError"
        :status-text="statusText"
        :summary="summary"
        :tier-bg-class="tierBgClass"
        :tier-display-name="tierDisplayName"
        :tier-text-class="tierTextClass"
      />

      <div class="grid gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(420px,0.9fr)] xl:gap-6">
        <BattleArena
          :summary="summary"
          :highlight-index="replacedHighlight"
          :status-text="statusText"
          :status-tone="statusTone"
        />

        <div class="space-y-4 rounded-3xl border-3 border-slate-200/80 bg-white/95 p-4 shadow-poke-card backdrop-blur sm:p-6">
          <BattleActionPanel
            :action-headline="actionHeadline"
            :action-description="actionDescription"
            :available-action-count="availableActionCount"
            :available-action-description="availableActionDescription"
            :battle-format="battleFormat"
            :busy-action="busyAction"
            :current-battle-id="currentBattleId"
            :factory-run="factoryRun"
            :is-busy="isBusy"
            :mode-description="modeDescription"
            :mode-summary="modeSummary"
            :polling-active="pollingActive"
            :recommended-mode="recommendedMode"
            :show-continue-factory-button="showContinueFactoryButton"
            :show-reset-battle-button="showResetBattleButton"
            :summary="summary"
            @abandon-factory="abandonFactoryRun"
            @forfeit-battle="forfeitBattle"
            @next-factory="nextFactoryBattle"
            @open-leaderboard="openLeaderboard"
            @open-ban="openBanModal"
            @prepare-next="prepareNextFactoryStage"
            @refresh-status="refreshStatus"
            @reset-battle="resetBattleState({ keepFactoryRun: false })"
            @start-async="startAsyncBattle"
            @start-factory="startFactoryChallenge"
            @start-manual="startBattle"
            @update-format="setBattleFormat"
          />

          <BattleDecisionPanel
            :busy-action="busyAction"
            :can-confirm-preview="canConfirmPreview"
            :can-confirm-replacement="canConfirmReplacement"
            :available-special-systems="availableSpecialSystems"
            :active-special-system-label="activeSpecialSystemLabel"
            :can-use-special-system="canUseSpecialSystem"
            :can-terastallize="canTerastallize"
            :can-submit-move="canSubmitMove"
            :confirm-preview="confirmPreview"
            :confirm-replacement="confirmReplacement"
            :format-types="formatTypes"
            :is-busy="isBusy"
            :is-lead="isLead"
            :is-picked="isPicked"
            :is-preview-phase="isPreviewPhase"
            :is-replacement-phase="isReplacementPhase"
            :lead-limit="leadLimit"
            :lead-roster-indexes="leadRosterIndexes"
            :move-effectiveness-hints="moveEffectivenessHints"
            :move-needs-opponent-target="moveNeedsOpponentTarget"
            :move-target-text="moveTargetText"
            :opponent-active-options="opponentActiveOptions"
            :opponent-roster="opponentRoster"
            :pending-replacement-count="pendingReplacementCount"
            :player-active-mons="playerActiveMons"
            :player-bench-options="playerBenchOptions"
            :player-roster="playerRoster"
            :preview-card-class="previewCardClass"
            :replacement-bench-options="replacementBenchOptions"
            :result-text="resultText"
            :roster-limit="rosterLimit"
            :selected-actions="selectedActions"
            :set-selected-action="setSelectedAction"
            :selected-move-object="selectedMoveObject"
            :selected-moves="selectedMoves"
            :set-selected-move="setSelectedMove"
            :selected-special-systems="selectedSpecialSystems"
            :set-selected-special-system="setSelectedSpecialSystem"
            :selected-replacement-indexes="selectedReplacementIndexes"
            :selected-roster-indexes="selectedRosterIndexes"
            :selected-switch-targets="selectedSwitchTargets"
            :set-selected-switch-target="setSelectedSwitchTarget"
            :selected-targets="selectedTargets"
            :set-selected-target="setSelectedTarget"
            :show-debug-panel="showDebugPanel"
            :special-system-label="specialSystemLabel"
            :submit-move="submitMove"
            :tera-type-label="teraTypeLabel"
            :toggle-lead="toggleLead"
            :toggle-replacement="toggleReplacement"
            :toggle-roster="toggleRoster"
            @toggle-debug-panel="setShowDebugPanel"
          />
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
          <div
            class="grid gap-2"
            :class="mobileActionButtons.length > 1 ? 'grid-cols-2' : 'grid-cols-1'"
          >
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

      <BattleSettlementModal
        v-if="settlement"
        :factory-run="factoryRun"
        :settlement="settlement"
        @close="onSettlementClose"
        @continue="prepareNextFactoryStage"
      />

      <BattleLeaderboardModal
        v-if="showLeaderboard"
        :leaderboard-data="leaderboardData"
        :loading="leaderboardLoading"
        @close="showLeaderboard = false"
      />

      <BanModal
        :show="showBanModal"
        :player-points="playerPoints"
        :pokemon-list="allPokemon"
        @close="showBanModal = false"
        @confirm="handleBanConfirm"
      />
    </template>
  </div>
</template>

<script setup>
import BattleArena from '../components/BattleArena.vue'
import BattleActionPanel from '../components/BattleActionPanel.vue'
import BattleDecisionPanel from '../components/BattleDecisionPanel.vue'
import BattleHeaderPanel from '../components/BattleHeaderPanel.vue'
import BattleLeaderboardModal from '../components/BattleLeaderboardModal.vue'
import BattleSettlementModal from '../components/BattleSettlementModal.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import BanModal from '../components/BanModal.vue'
import { useBattlePageState } from '../composables/useBattlePageState'
import { normalizeFactoryRun } from '../services/contracts/battleContract'
import { ref, watch, nextTick } from 'vue'
import { useLocale } from '../composables/useLocale'
import api from '../services/api'

const { translate: tr } = useLocale()

const {
  actionDescription,
  actionHeadline,
  abandonFactoryRun,
  availableActionCount,
  availableActionDescription,
  battleFormat,
  busyAction,
  canConfirmPreview,
  canConfirmReplacement,
  availableSpecialSystems,
  activeSpecialSystemLabel,
  canUseSpecialSystem,
  canTerastallize,
  canSubmitMove,
  confirmPreview,
  confirmReplacement,
  currentBattleId,
  currentUser,
  exchangeCandidates,
  factoryRoundClass,
  factoryRun,
  forfeitBattle,
  formatTypes,
  handleMobileAction,
  isBusy,
  isLead,
  isPicked,
  isPreviewPhase,
  isReplacementPhase,
  lastUpdatedLabel,
  leadLimit,
  leadRosterIndexes,
  leaderboardData,
  leaderboardLoading,
  mobileActionButtons,
  modeDescription,
  modeSummary,
  moveEffectivenessHints,
  moveNeedsOpponentTarget,
  moveTargetText,
  nextFactoryBattle,
  onConfirmExchange,
  onSettlementClose,
  openLeaderboard,
  opponentActiveOptions,
  opponentRoster,
  pendingReplacementCount,
  playerActiveMons,
  playerBenchOptions,
  playerProfile,
  playerRoster,
  pollingActive,
  previewCardClass,
  progressSummary,
  recommendedMode,
  replacedHighlight,
  replacedIndex,
  replacementBenchOptions,
  requestError,
  resetBattleState,
  refreshStatus,
  resultText,
  rosterLimit,
  selectedActions,
  setSelectedAction,
  selectedMoveObject,
  selectedMoves,
  setSelectedMove,
  selectedSpecialSystems,
  setSelectedSpecialSystem,
  selectedReplacementIndexes,
  selectedRosterIndexes,
  selectedSwitchTargets,
  setSelectedSwitchTarget,
  selectedTargets,
  setSelectedTarget,
  setBattleFormat,
  settlement,
  setShowDebugPanel,
  showContinueFactoryButton,
  showDebugPanel,
  showExchange,
  showLeaderboard,
  showMobileActionDock,
  showResetBattleButton,
  specialSystemLabel,
  startAsyncBattle,
  startBattle,
  startFactoryChallenge,
  statusText,
  statusTone,
  submitMove,
  summary,
  tierBgClass,
  tierDisplayName,
  teraTypeLabel,
  tierTextClass,
  prepareNextFactoryStage,
  toggleLead,
  toggleReplacement,
  toggleRoster
} = useBattlePageState()

// 纯文字模式逻辑
const isTextMode = ref(false)
const textLogs = ref([])
const textLogContainer = ref(null)

function toggleTextMode() {
  isTextMode.value = !isTextMode.value
}

function getLogClass(log) {
  if (log.type === 'header') return 'text-indigo-400 font-bold mt-4 mb-2'
  if (log.type === 'event') return 'text-slate-300'
  if (log.type === 'damage') return 'text-rose-400'
  if (log.type === 'status') return 'text-emerald-400'
  return 'text-slate-400'
}

// 监听 summary 变化，自动生成文字日志
watch(summary, (newSummary) => {
  if (!isTextMode.value || !newSummary) return

  const newLogs = []
  
  // 基础信息
  newLogs.push({ type: 'header', content: `=== ${tr('对战状态', 'Battle Status')} ===` })
  newLogs.push({ type: 'info', content: `${tr('回合', 'Round')}: ${newSummary.currentRound || 0} / ${newSummary.roundLimit || 50}` })
  newLogs.push({ type: 'info', content: `${tr('状态', 'Status')}: ${newSummary.status || 'waiting'}` })

  // 场上宝可梦
  newLogs.push({ type: 'header', content: `--- ${tr('当前场上', 'Active Pokemon')} ---` })
  
  const playerActive = (newSummary.playerTeam || []).filter((_, i) => (newSummary.playerActiveSlots || []).includes(i))
  const opponentActive = (newSummary.opponentTeam || []).filter((_, i) => (newSummary.opponentActiveSlots || []).includes(i))

  playerActive.forEach(mon => {
    newLogs.push({ type: 'status', content: `[${tr('玩家', 'Player')}] ${mon.name} - HP: ${mon.currentHp}/${mon.stats?.hp || '?'}` })
  })
  opponentActive.forEach(mon => {
    newLogs.push({ type: 'status', content: `[${tr('对手', 'Foe')}] ${mon.name} - HP: ${mon.currentHp}/${mon.stats?.hp || '?'}` })
  })

  // 最新回合事件
  if (newSummary.rounds && newSummary.rounds.length > 0) {
    const lastRound = newSummary.rounds[newSummary.rounds.length - 1]
    newLogs.push({ type: 'header', content: `>>> ${tr('第', 'Round')} ${lastRound.round} ${tr('回合记录', 'Log')} <<<` })
    ;(lastRound.events || []).forEach(event => {
      let type = 'event'
      if (event.includes(tr('造成了', 'dealt')) || event.includes('damage')) type = 'damage'
      newLogs.push({ type, content: event })
    })
  }

  textLogs.value = newLogs

  // 自动滚动到底部
  nextTick(() => {
    if (textLogContainer.value) {
      textLogContainer.value.scrollTop = textLogContainer.value.scrollHeight
    }
  })
}, { deep: true })

// Ban 系统
const showBanModal = ref(false)
const playerPoints = ref(0)
const allPokemon = ref([])

async function openBanModal() {
  try {
    // 加载玩家积分
    const profile = await api.battle.profile()
    playerPoints.value = profile?.profile?.totalPoints || 0

    // 加载宝可梦列表
    if (allPokemon.value.length === 0) {
      const res = await api.pokemon.getList({ current: 1, size: 200 })
      allPokemon.value = res.data?.records || []
    }

    showBanModal.value = true
  } catch (e) {
    console.error('加载 Ban 数据失败:', e)
  }
}

async function handleBanConfirm({ bannedPokemon, cost }) {
  showBanModal.value = false

  try {
    // 调用带 ban 的工厂挑战开始接口
    const res = await api.battle.factoryStartWithBan({ bannedPokemon })

    if (res.error) {
      alert(res.message || '开始挑战失败')
      return
    }

    // 更新玩家积分
    playerPoints.value = res.remainingPoints || playerPoints.value - cost

    // 处理返回结果（与 startFactoryChallenge 保持一致的解析逻辑）
    const nextRun = normalizeFactoryRun(res.run || res)
    if (nextRun && nextRun.id) {
      factoryRun.value = nextRun

      if (res.battleId || res.battle?.id) {
        currentBattleId.value = res.battleId || res.battle?.id
        await refreshStatus(true)
      }
    }

    resultText.value = JSON.stringify(res, null, 2)
  } catch (e) {
    alert('开始挑战失败: ' + (e.message || e))
  }
}
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
