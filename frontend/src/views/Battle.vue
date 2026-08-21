

<template>
  <div class="sd-page">
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
      <!-- Showdown 风格布局：战场 + 操作面板 + 日志 -->

      <!-- 开始/模式选择（无战斗时显示） -->
      <div v-if="!currentBattleId && !factoryRun" class="sd-start-panel">
        <div class="sd-start-modes">
          <button
            v-for="fmt in [['vgc-doubles', '双打 (64)'], ['vgc63', '63 单打'], ['gen9singles', '9代单打']]"
            :key="fmt[0]"
            class="sd-format-btn"
            :class="battleFormat === fmt[0] ? 'sd-format-active' : ''"
            @click="setBattleFormat(fmt[0])"
          >{{ fmt[1] }}</button>
        </div>
        <div class="sd-start-actions">
          <button class="sd-start-btn" :disabled="isBusy" @click="startBattle">
            ⚔️ {{ busyAction === 'start-manual' ? '...' : tr('手动对战', 'Manual Battle') }}
          </button>
          <button v-if="isAuthenticated" class="sd-start-btn sd-start-factory" :disabled="isBusy" @click="startFactoryChallenge">
            🏟️ {{ busyAction === 'factory-start' ? '...' : tr('工厂挑战', 'Factory Run') }}
          </button>
          <button v-if="isAuthenticated" class="sd-start-btn sd-start-async" :disabled="isBusy" @click="startAsyncBattle">
            ⏩ {{ tr('异步模拟', 'Async') }}
          </button>
        </div>
        <div v-if="!isAuthenticated" class="sd-guest-note">
          {{ tr('游客模式：可直接手动对战，登录后解锁工厂挑战', 'Guest: manual battle available. Login for factory.') }}
        </div>
      </div>

      <!-- 工厂挑战进行中（无当前战斗时） -->
      <div v-if="factoryRun && !currentBattleId" class="sd-factory-bar">
        <span class="sd-factory-info">{{ tr('工厂挑战', 'Factory') }} {{ factoryRun.wins || 0 }}W/{{ factoryRun.losses || 0 }}L · {{ factoryRun.current_battle || 0 }}/{{ factoryRun.max_battles || 9 }}</span>
        <button class="sd-start-btn" :disabled="isBusy" @click="nextFactoryBattle">{{ tr('进入下一轮', 'Next Round') }}</button>
        <button class="sd-forfeit-btn" :disabled="isBusy" @click="abandonFactoryRun">{{ tr('放弃', 'Abandon') }}</button>
      </div>

      <!-- 战斗进行中 -->
      <template v-if="summary">
        <!-- 战场 -->
        <BattleArena
          :summary="summary"
          :highlight-index="replacedHighlight"
          :status-text="statusText"
          :status-tone="statusTone"
          :target-field-slot="arenaTargetSlot"
          :can-target="arenaCanTarget"
          @target-select="onArenaTargetSelect"
        />

        <!-- 操作面板（预览/招式/换人） -->
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
          :opponent-active-mons="opponentActiveMons"
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

        <!-- 操作栏 -->
        <div class="sd-action-bar">
          <button v-if="currentBattleId && summary.status !== 'completed'" class="sd-action-btn" :disabled="isBusy" @click="refreshStatus">
            🔄 {{ tr('刷新', 'Refresh') }}
          </button>
          <button v-if="currentBattleId && summary.status === 'running'" class="sd-forfeit-btn" :disabled="isBusy" @click="forfeitBattle">
            {{ tr('投降', 'Forfeit') }}
          </button>
          <button v-if="showContinueFactoryButton" class="sd-start-btn" :disabled="isBusy" @click="prepareNextFactoryStage">
            {{ tr('准备下一轮', 'Next') }}
          </button>
          <button v-if="showResetBattleButton" class="sd-action-btn" :disabled="isBusy" @click="resetBattleState({ keepFactoryRun: false })">
            {{ tr('重置', 'Reset') }}
          </button>
          <button class="sd-action-btn" @click="openLeaderboard">📊 {{ tr('排行', 'Rank') }}</button>
          <span v-if="lastUpdatedLabel" class="sd-updated">{{ lastUpdatedLabel }}</span>
        </div>
      </template>

      <!-- ExchangeModal / SettlementModal / LeaderboardModal / BanModal -->
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
import { ref, watch, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
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
  isAuthenticated,
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
  nextFactoryBattle,
  onConfirmExchange,
  onSettlementClose,
  openLeaderboard,
  opponentActiveMons,
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

// ===== 战场点击选目标（Showdown 风格） =====
const arenaTargetSlot = ref(null)

const arenaCanTarget = computed(() => {
  if (isPreviewPhase.value || isReplacementPhase.value || summary.value?.status !== 'running') return false
  return playerActiveMons.value.some((mon) => {
    const actionKey = `action-slot-${mon.fieldSlot}`
    if ((selectedActions[actionKey] || 'move') !== 'move') return false
    const move = selectedMoveObject(mon)
    if (!moveNeedsOpponentTarget(move)) return false
    return selectedTargets[`target-slot-${mon.fieldSlot}`] === undefined
  })
})

// 点击战场对手精灵 → 设置目标
function onArenaTargetSelect(fieldSlot) {
  // 找到第一个需要目标选择的在场宝可梦，设置目标
  const needTarget = playerActiveMons.value.find((mon) => {
    const actionKey = `action-slot-${mon.fieldSlot}`
    if ((selectedActions[actionKey] || 'move') !== 'move') return false
    const move = selectedMoveObject(mon)
    if (!moveNeedsOpponentTarget(move)) return false
    return selectedTargets[`target-slot-${mon.fieldSlot}`] === undefined
  })
  if (!needTarget) return
  setSelectedTarget(needTarget.fieldSlot, fieldSlot)
  // 短暂高亮选中的目标精灵
  arenaTargetSlot.value = fieldSlot
  setTimeout(() => { arenaTargetSlot.value = null }, 1200)
}

function toggleTextMode() {
  isTextMode.value = !isTextMode.value
  // 切换模式时重置日志增量游标，重新生成完整日志
  if (isTextMode.value) {
    lastTextLogRound = -1
    textLogs.value = []
    generateTextLogs(summary.value)
  }
}

function getLogClass(log) {
  if (log.type === 'header') return 'text-indigo-400 font-bold mt-4 mb-2'
  if (log.type === 'event') return 'text-slate-300'
  if (log.type === 'damage') return 'text-rose-400'
  if (log.type === 'status') return 'text-emerald-400'
  return 'text-slate-400'
}

// 生成文字日志（增量追加，保留历史回合）
let lastTextLogRound = -1
function generateTextLogs(newSummary) {
  if (!isTextMode.value || !newSummary) return

  const newLogs = []
  
  // 仅在状态变化或首次时输出头部/状态/在场信息
  if (textLogs.value.length === 0) {
    newLogs.push({ type: 'header', content: `=== ${tr('对战状态', 'Battle Status')} ===` })
    newLogs.push({ type: 'info', content: `${tr('回合', 'Round')}: ${newSummary.currentRound || 0} / ${newSummary.roundLimit || 50}` })
    newLogs.push({ type: 'info', content: `${tr('状态', 'Status')}: ${newSummary.status || 'waiting'}` })
  }

  // 追加新回合的事件（只处理比上次更新的回合）
  const rounds = newSummary.rounds || []
  if (rounds.length > lastTextLogRound + 1) {
    const startIndex = Math.max(0, lastTextLogRound + 1)
    for (let i = startIndex; i < rounds.length; i++) {
      const round = rounds[i]
      newLogs.push({ type: 'header', content: `>>> ${round.round === 0 ? tr('入场阶段', 'Entry phase') : tr('第', 'Round') + ' ' + round.round + ' ' + tr('回合记录', 'Log')} <<<` })
      ;(round.events || []).forEach((event) => {
        let type = 'event'
        // 伤害判定：优先按事件文本中的常见伤害表述（中/英），避免误判
        const damageKeywords = [tr('造成了', 'dealt'), tr('造成', 'dealt'), 'damage', '伤害', '点伤害']
        if (damageKeywords.some((kw) => event.includes(kw))) type = 'damage'
        newLogs.push({ type, content: event })
      })
    }
    lastTextLogRound = rounds.length - 1
  }

  // 战斗结束追加胜负
  if (newSummary.status === 'completed' && textLogs.value.length > 0) {
    const last = textLogs.value[textLogs.value.length - 1]
    if (!last.content.includes(tr('胜者', 'Winner'))) {
      newLogs.push({
        type: 'status',
        content: newSummary.winner === 'player'
          ? `>>> ${tr('战斗胜利！', 'Victory!')} <<<`
          : `>>> ${tr('战斗失败', 'Defeat')} <<<`
      })
    }
  }

  if (newLogs.length) {
    textLogs.value = [...textLogs.value, ...newLogs]
  }

  // 自动滚动到底部
  nextTick(() => {
    if (textLogContainer.value) {
      textLogContainer.value.scrollTop = textLogContainer.value.scrollHeight
    }
  })
}

// 监听 summary 变化，自动生成文字日志
watch(summary, (newSummary) => {
  generateTextLogs(newSummary)
}, { deep: true })

// Ban 系统
const showBanModal = ref(false)
const playerPoints = ref(0)
const allPokemon = ref([])

// ===== Showdown 风格键盘操作 =====
// 回合操作阶段：数字键 1-4 选择招式，Enter/Space 提交回合
// 预览阶段：数字键选择参战/首发（先选参战 6 选 N，再按 L 标记首发）
let keyboardHandler = null

function isEditableField() {
  const el = document.activeElement
  return el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.isContentEditable)
}

function handleBattleKeydown(event) {
  if (isTextMode.value) return
  if (isEditableField()) return

  const key = event.key

  // 预览阶段：数字键选参战，L 标记首发
  if (isPreviewPhase.value && /^[1-6]$/.test(key)) {
    const index = Number(key) - 1
    if (index < playerRoster.value.length) {
      toggleRoster(index)
    }
    event.preventDefault()
    return
  }
  if (isPreviewPhase.value && (key === 'l' || key === 'L')) {
    // L 键：将下一个已选中的宝可梦标记为首发（循环切换）
    event.preventDefault()
    const picked = selectedRosterIndexes.value
    if (!picked.length) return
    const leads = leadRosterIndexes.value
    if (leads.length === 0) {
      // 没有首发时，标记第一个已选中的
      toggleLead(picked[0])
    } else {
      // 找到当前最后一个首发在 picked 中的索引，标记下一个
      const lastLead = leads[leads.length - 1]
      const idx = picked.indexOf(lastLead)
      const nextIdx = (idx + 1) % picked.length
      toggleLead(picked[nextIdx])
    }
    return
  }

  // 回合操作阶段：数字键选招式（按槽位顺序，从第一个未完成动作的槽位开始）
  if (!isPreviewPhase.value && !isReplacementPhase.value && summary.value?.status === 'running' && /^[1-4]$/.test(key)) {
    const idx = Number(key) - 1
    const targetMon = playerActiveMons.value.find((m) => {
      const actionKey = `action-slot-${m.fieldSlot}`
      return (selectedActions[actionKey] || 'move') === 'move'
    })
    if (targetMon && targetMon.moves?.[idx]) {
      setSelectedMove(targetMon.fieldSlot, targetMon.moves[idx].name_en || targetMon.moves[idx].name)
    }
    event.preventDefault()
    return
  }

  // Enter/Space：根据阶段触发主操作
  if (key === 'Enter' || key === ' ') {
    if (isPreviewPhase.value && canConfirmPreview.value) {
      confirmPreview()
      event.preventDefault()
    } else if (isReplacementPhase.value && canConfirmReplacement.value) {
      confirmReplacement()
      event.preventDefault()
    } else if (summary.value?.status === 'running' && canSubmitMove.value) {
      submitMove()
      event.preventDefault()
    }
    return
  }

  // S：切换 招式/换人（作用于第一个未完成的槽位）
  if (key === 's' || key === 'S') {
    if (isPreviewPhase.value || summary.value?.status !== 'running') return
    const targetMon = playerActiveMons.value[0]
    if (!targetMon) return
    const actionKey = `action-slot-${targetMon.fieldSlot}`
    const next = (selectedActions[actionKey] || 'move') === 'move' ? 'switch' : 'move'
    if (next === 'switch' && !playerBenchOptions.value.length) return
    setSelectedAction(targetMon.fieldSlot, next)
    event.preventDefault()
    return
  }

  // R：刷新状态
  if (key === 'r' || key === 'R') {
    if (currentBattleId.value) {
      refreshStatus(true)
      event.preventDefault()
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleBattleKeydown)
  keyboardHandler = true
})

onBeforeUnmount(() => {
  if (keyboardHandler) {
    window.removeEventListener('keydown', handleBattleKeydown)
    keyboardHandler = null
  }
})

async function openBanModal() {
  try {
    // 加载玩家积分
    const profile = await api.battle.profile()
    playerPoints.value = profile?.profile?.totalPoints || 0

    // 加载宝可梦列表（全量覆盖全国图鉴）
    if (allPokemon.value.length === 0) {
      const res = await api.pokemon.getList({ current: 1, size: 1100 })
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
    const nextRun = normalizeFactoryRun(res?.run || res)
    if (nextRun && nextRun.id) {
      factoryRun.value = nextRun

      if (res?.battleId || res?.battle?.id) {
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
/* ===== Showdown 风格全局布局 ===== */
.sd-page {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 640px;
  margin: 0 auto;
  padding: 8px;
  background: #2d2d2d;
  min-height: 100vh;
  font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

/* ===== Showdown 风格全局布局 ===== */
.sd-start-panel {
  background: #2d2d2d;
  border-radius: 4px;
  padding: 16px;
}
.sd-start-modes {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
}
.sd-format-btn {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #555;
  border-radius: 3px;
  background: #1a1a1a;
  color: #94a3b8;
  cursor: pointer;
}
.sd-format-active { background: #3b82f6; color: #fff; border-color: #3b82f6; }
.sd-start-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sd-start-btn {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 700;
  border: none;
  border-radius: 3px;
  background: #3b82f6;
  color: #fff;
  cursor: pointer;
}
.sd-start-btn:hover:not(:disabled) { background: #2563eb; }
.sd-start-btn:disabled { background: #374151; color: #6b7280; cursor: not-allowed; }
.sd-start-factory { background: #6366f1; }
.sd-start-factory:hover:not(:disabled) { background: #4f46e5; }
.sd-start-async { background: #10b981; }
.sd-start-async:hover:not(:disabled) { background: #059669; }
.sd-guest-note {
  margin-top: 8px;
  font-size: 11px;
  color: #f59e0b;
}
.sd-factory-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #2d2d2d;
  border-radius: 4px;
}
.sd-factory-info {
  font-size: 12px;
  color: #e2e8f0;
  font-weight: 600;
}
.sd-forfeit-btn {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #7f1d1d;
  border-radius: 3px;
  background: #451a1a;
  color: #fca5a5;
  cursor: pointer;
}
.sd-forfeit-btn:hover:not(:disabled) { background: #7f1d1d; }
.sd-forfeit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.sd-action-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #252525;
  border-radius: 4px;
  margin-top: 4px;
}
.sd-action-btn {
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #444;
  border-radius: 3px;
  background: #1a1a1a;
  color: #94a3b8;
  cursor: pointer;
}
.sd-action-btn:hover { border-color: #666; color: #e2e8f0; }
.sd-action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.sd-updated {
  margin-left: auto;
  font-size: 10px;
  color: #475569;
}
</style>
