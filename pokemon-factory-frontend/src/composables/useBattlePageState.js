import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../services/api'
import { useAuth } from './useAuth'
import { useBattleDerivedState } from './battle/useBattleDerivedState'
import {
  buildBattleSettlement,
  buildMoveSubmission,
  formatPokemonTypes,
  moveNeedsOpponentTarget,
  moveTargetText,
  normalizeBattlePayload,
  normalizeFactoryRun
} from '../services/contracts/battleContract'

export function useBattlePageState() {
  const auth = useAuth()

  const resultText = ref('等待开始对战')
  const summary = ref(null)
  const selectedActions = ref({})
  const selectedMoves = ref({})
  const selectedSpecialSystems = ref({})
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
  const factoryRun = ref(null)
  const playerProfile = ref(null)
  const settlement = ref(null)
  const showLeaderboard = ref(false)
  const leaderboardData = ref([])
  let pollTimer = null
  const pendingReplacementCount = computed(() => Number(summary.value?.playerPendingReplacementCount || 0))
  const {
    actionDescription,
    actionHeadline,
    availableActionCount,
    availableActionDescription,
    availableSpecialSystems,
    activeSpecialSystemLabel,
    canConfirmPreview,
    canConfirmReplacement,
    canUseSpecialSystem,
    canTerastallize,
    canSubmitMove,
    currentUser,
    exchangeCandidates,
    isBusy,
    isPreviewPhase,
    isReplacementPhase,
    lastUpdatedLabel,
    mobileActionButtons,
    modeDescription,
    modeSummary,
    opponentActiveOptions,
    opponentRoster,
    playerActiveMons,
    playerBenchOptions,
    playerRoster,
    playerTeam,
    progressSummary,
    recommendedMode,
    replacementBenchOptions,
    selectedMoveObject,
    showContinueFactoryButton,
    showMobileActionDock,
    showResetBattleButton,
    specialSystemLabel,
    statusText,
    statusTone,
    tierBgClass,
    tierDisplayName,
    teraTypeLabel,
    tierTextClass
  } = useBattleDerivedState({
    busyAction,
    currentBattleId,
    displayName: auth.displayName,
    factoryRun,
    lastUpdatedAt,
    leadRosterIndexes,
    pendingReplacementCount,
    playerProfile,
    pollingActive,
    selectedActions,
    selectedMoves,
    selectedReplacementIndexes,
    selectedRosterIndexes,
    selectedSwitchTargets,
    selectedTargets,
    settlement,
    summary
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
    selectedSpecialSystems.value = {}
    selectedTargets.value = {}
    selectedSwitchTargets.value = {}
    selectedRosterIndexes.value = []
    leadRosterIndexes.value = []
    selectedReplacementIndexes.value = []
  }

  function markUpdated() {
    lastUpdatedAt.value = Date.now()
  }

  function setRequestErrorMessage(message) {
    requestError.value = message || ''
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

  function updateSelectionState(targetRef, key, value) {
    const next = { ...targetRef.value }
    if (value === undefined || value === null || value === '') {
      delete next[key]
    } else {
      next[key] = value
    }
    targetRef.value = next
  }

  function setSelectedAction(fieldSlot, value) {
    updateSelectionState(selectedActions, `action-slot-${fieldSlot}`, value)
  }

  function setSelectedMove(fieldSlot, value) {
    updateSelectionState(selectedMoves, `slot-${fieldSlot}`, value)
  }

  function setSelectedSpecialSystem(fieldSlot, value) {
    updateSelectionState(selectedSpecialSystems, `special-slot-${fieldSlot}`, value)
  }

  function setSelectedTarget(fieldSlot, value) {
    updateSelectionState(selectedTargets, `target-slot-${fieldSlot}`, value)
  }

  function setSelectedSwitchTarget(fieldSlot, value) {
    updateSelectionState(selectedSwitchTargets, `switch-slot-${fieldSlot}`, value)
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

  function initializePreviewSelections() {
    if (!playerRoster.value.length) return
    if (selectedRosterIndexes.value.length !== 4) {
      selectedRosterIndexes.value = playerRoster.value.slice(0, 4).map((_, index) => index)
    }
    if (leadRosterIndexes.value.length !== 2) {
      leadRosterIndexes.value = selectedRosterIndexes.value.slice(0, 2)
    }
  }

  function ensureReplacementSelections() {
    const available = replacementBenchOptions.value.map((option) => option.value)
    selectedReplacementIndexes.value = selectedReplacementIndexes.value.filter((index) => available.includes(index))
    if (selectedReplacementIndexes.value.length > pendingReplacementCount.value) {
      selectedReplacementIndexes.value = selectedReplacementIndexes.value.slice(0, pendingReplacementCount.value)
    }
  }

  function ensureMoveSelections() {
    const actionNext = { ...selectedActions.value }
    const moveNext = { ...selectedMoves.value }
    const specialNext = { ...selectedSpecialSystems.value }
    const targetNext = { ...selectedTargets.value }
    const switchNext = { ...selectedSwitchTargets.value }

    for (const mon of playerActiveMons.value) {
      const actionKey = `action-slot-${mon.fieldSlot}`
      const moveKey = `slot-${mon.fieldSlot}`
      const specialKey = `special-slot-${mon.fieldSlot}`
      const targetKey = `target-slot-${mon.fieldSlot}`
      const switchKey = `switch-slot-${mon.fieldSlot}`

      if (!['move', 'switch'].includes(actionNext[actionKey])) {
        actionNext[actionKey] = 'move'
      }
      if (actionNext[actionKey] === 'switch' || !availableSpecialSystems(mon).includes(specialNext[specialKey])) {
        delete specialNext[specialKey]
      }

      const moveNames = (mon.moves || []).map((move) => move.name_en || move.name)
      if (!moveNames.length) {
        delete moveNext[moveKey]
      } else if (!moveNames.includes(moveNext[moveKey])) {
        moveNext[moveKey] = moveNames[0]
      }

      const selectedMove = (mon.moves || []).find((move) => (move.name_en || move.name) === moveNext[moveKey])
      const targetValues = opponentActiveOptions.value.map((target) => target.value)
      if (!moveNeedsOpponentTarget(selectedMove) || !targetValues.length) {
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
    selectedSpecialSystems.value = specialNext
    selectedTargets.value = targetNext
    selectedSwitchTargets.value = switchNext
  }

  function applyBattlePayload(payload) {
    const normalized = normalizeBattlePayload(payload)
    summary.value = normalized.summary
    if (normalized.battleId) {
      currentBattleId.value = normalized.battleId
    }

    markUpdated()

    if (isPreviewPhase.value) {
      initializePreviewSelections()
    } else if (isReplacementPhase.value) {
      ensureReplacementSelections()
    } else {
      ensureMoveSelections()
    }

    if (summary.value?.status === 'completed' && summary.value?.winner === 'player' && summary.value?.exchangeAvailable) {
      replacedIndex.value = 0
      showExchange.value = true
    }

    if (summary.value?.status === 'completed') {
      settlement.value = buildBattleSettlement(summary.value, payload)
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
      const playerMoveMap = buildMoveSubmission({
        playerActiveMons: playerActiveMons.value,
        selectedActions: selectedActions.value,
        selectedMoves: selectedMoves.value,
        selectedSpecialSystems: selectedSpecialSystems.value,
        selectedSwitchTargets: selectedSwitchTargets.value,
        selectedTargets: selectedTargets.value,
        selectedMoveObject
      })
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

  function factoryRoundClass(index) {
    if (!factoryRun.value) return 'bg-slate-200'
    const done = factoryRun.value.current_battle || 0
    if (index <= done) {
      return 'bg-indigo-500'
    }
    if (index === done + 1 && currentBattleId.value) return 'bg-indigo-300 animate-pulse'
    return 'bg-slate-200'
  }

  async function loadProfile() {
    try {
      const res = await api.battle.profile()
      playerProfile.value = res?.profile || res
      return playerProfile.value
    } catch (error) {
      setRequestErrorMessage(error?.message || '加载玩家资料失败')
      throw error
    }
  }

  async function loadFactoryStatus() {
    try {
      const res = await api.battle.factoryStatus()
      const run = normalizeFactoryRun(res?.activeRun || res)
      if (run && run.id) {
        factoryRun.value = run
      } else {
        factoryRun.value = null
      }
      return factoryRun.value
    } catch (error) {
      factoryRun.value = null
      setRequestErrorMessage(error?.message || '加载工厂挑战状态失败')
      throw error
    }
  }

  async function loadLeaderboard() {
    try {
      leaderboardLoading.value = true
      leaderboardData.value = await api.battle.leaderboard() || []
      return leaderboardData.value
    } catch (error) {
      leaderboardData.value = []
      setRequestErrorMessage(error?.message || '加载排行榜失败')
      throw error
    } finally {
      leaderboardLoading.value = false
    }
  }

  async function openLeaderboard() {
    showLeaderboard.value = true
    await runBusy('open-leaderboard', loadLeaderboard)
  }

  async function startFactoryChallenge() {
    await runBusy('factory-start', async () => {
      stopPolling()
      resultText.value = '正在开始工厂挑战...'
      const res = await api.battle.factoryStart()
      const nextRun = normalizeFactoryRun(res.run || res)
      factoryRun.value = nextRun

      if (res.battleId || res.battle?.id) {
        currentBattleId.value = res.battleId || res.battle?.id
        if (normalizeBattlePayload(res).summary) {
          applyBattlePayload(res)
        } else {
          await refreshStatus(true)
        }
      } else if (nextRun?.id) {
        const nextBattleRes = await api.battle.factoryNext(nextRun.id)
        factoryRun.value = normalizeFactoryRun(nextBattleRes.run || nextBattleRes) || nextRun
        if (nextBattleRes.battleId || nextBattleRes.battle?.id) {
          currentBattleId.value = nextBattleRes.battleId || nextBattleRes.battle?.id
          if (normalizeBattlePayload(nextBattleRes).summary) {
            applyBattlePayload(nextBattleRes)
          } else {
            await refreshStatus(true)
          }
        }
        resultText.value = JSON.stringify(nextBattleRes, null, 2)
        await loadProfile().catch(() => null)
        return
      }

      resultText.value = JSON.stringify(res, null, 2)
      await loadProfile().catch(() => null)
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
      factoryRun.value = normalizeFactoryRun(res.run || res) || factoryRun.value
      if (res.battleId || res.battle?.id) {
        currentBattleId.value = res.battleId || res.battle?.id
        if (normalizeBattlePayload(res).summary) {
          applyBattlePayload(res)
        } else {
          await refreshStatus(true)
        }
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
      await loadProfile().catch(() => null)
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

  function onSettlementClose() {
    const wasRunFinished = settlement.value?.runFinished
    settlement.value = null
    if (wasRunFinished || !factoryRun.value) {
      resetBattleState({ keepFactoryRun: false, keepResultText: true })
      resultText.value = '等待开始对战'
      loadProfile().catch(() => null)
    } else if (summary.value?.status === 'completed') {
      resultText.value = '本轮已完成，可以查看战场详情或继续下一轮。'
    }
  }

  onMounted(async () => {
    await Promise.allSettled([loadProfile(), loadFactoryStatus()])
    if (factoryRun.value?.current_battle_id) {
      currentBattleId.value = factoryRun.value.current_battle_id
      await refreshStatus(true)
    }
  })

  onBeforeUnmount(() => {
    stopPolling()
  })

  function setShowDebugPanel(value) {
    showDebugPanel.value = Boolean(value)
  }

  return {
    actionDescription,
    actionHeadline,
    availableActionCount,
    availableActionDescription,
    abandonFactoryRun,
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
    formatTypes: formatPokemonTypes,
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
    prepareNextFactoryStage,
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
    resultText,
    selectedActions,
    setSelectedAction,
    availableSpecialSystems,
    activeSpecialSystemLabel,
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
    settlement,
    showContinueFactoryButton,
    showDebugPanel,
    showExchange,
    showLeaderboard,
    showMobileActionDock,
    showResetBattleButton,
    specialSystemLabel,
    resetBattleState,
    setShowDebugPanel,
    startAsyncBattle,
    startBattle,
    startFactoryChallenge,
    statusText,
    statusTone,
    submitMove,
    summary,
    tierBgClass,
    tierDisplayName,
    canUseSpecialSystem,
    teraTypeLabel,
    tierTextClass,
    toggleLead,
    toggleReplacement,
    toggleRoster,
    refreshStatus
  }
}
