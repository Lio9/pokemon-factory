<template>
  <div class="space-y-6 pb-24 md:pb-0">
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

      <div class="space-y-4 rounded-[24px] border border-slate-200/80 bg-white/95 p-4 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:rounded-3xl sm:p-6">
        <BattleActionPanel
          :action-headline="actionHeadline"
          :action-description="actionDescription"
          :available-action-count="availableActionCount"
          :available-action-description="availableActionDescription"
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
          @prepare-next="prepareNextFactoryStage"
          @refresh-status="refreshStatus"
          @reset-battle="resetBattleState({ keepFactoryRun: false })"
          @start-async="startAsyncBattle"
          @start-factory="startFactoryChallenge"
          @start-manual="startBattle"
        />

        <BattleDecisionPanel
          :busy-action="busyAction"
          :can-confirm-preview="canConfirmPreview"
          :can-confirm-replacement="canConfirmReplacement"
          :can-submit-move="canSubmitMove"
          :confirm-preview="confirmPreview"
          :confirm-replacement="confirmReplacement"
          :format-types="formatTypes"
          :is-busy="isBusy"
          :is-lead="isLead"
          :is-picked="isPicked"
          :is-preview-phase="isPreviewPhase"
          :is-replacement-phase="isReplacementPhase"
          :lead-roster-indexes="leadRosterIndexes"
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
          :selected-actions="selectedActions"
          :selected-move-object="selectedMoveObject"
          :selected-moves="selectedMoves"
          :selected-replacement-indexes="selectedReplacementIndexes"
          :selected-roster-indexes="selectedRosterIndexes"
          :selected-switch-targets="selectedSwitchTargets"
          :selected-targets="selectedTargets"
          :show-debug-panel="showDebugPanel"
          :submit-move="submitMove"
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
import { useBattlePageState } from '../composables/useBattlePageState'

const {
  actionDescription,
  actionHeadline,
  abandonFactoryRun,
  availableActionCount,
  availableActionDescription,
  busyAction,
  canConfirmPreview,
  canConfirmReplacement,
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
  leadRosterIndexes,
  leaderboardData,
  leaderboardLoading,
  mobileActionButtons,
  modeDescription,
  modeSummary,
  moveNeedsOpponentTarget,
  moveTargetText,
  nextFactoryBattle,
  onConfirmExchange,
  onSettlementClose,
  openLeaderboard,
  opponentActiveOptions,
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
  selectedActions,
  selectedMoveObject,
  selectedMoves,
  selectedReplacementIndexes,
  selectedRosterIndexes,
  selectedSwitchTargets,
  selectedTargets,
  settlement,
  setShowDebugPanel,
  showContinueFactoryButton,
  showDebugPanel,
  showExchange,
  showLeaderboard,
  showMobileActionDock,
  showResetBattleButton,
  startAsyncBattle,
  startBattle,
  startFactoryChallenge,
  statusText,
  statusTone,
  submitMove,
  summary,
  tierBgClass,
  tierDisplayName,
  tierTextClass,
  prepareNextFactoryStage,
  toggleLead,
  toggleReplacement,
  toggleRoster
} = useBattlePageState()
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
