import { computed } from 'vue'
import { TIER_NAMES, formatPokemonTypes, moveNeedsOpponentTarget } from '../../services/contracts/battleContract'

export function useBattleDerivedState(state) {
  const {
    busyAction,
    currentBattleId,
    displayName,
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
  } = state

  const currentUser = computed(() => displayName.value)
  const tierDisplayName = computed(() => TIER_NAMES[playerProfile.value?.tier ?? 0] || '精灵球')
  const isBusy = computed(() => Boolean(busyAction.value))
  const isPreviewPhase = computed(() => summary.value?.status === 'preview' || summary.value?.phase === 'team-preview')
  const isReplacementPhase = computed(() => summary.value?.phase === 'replacement')
  const playerTeam = computed(() => summary.value?.playerTeam || [])
  const playerRoster = computed(() => summary.value?.playerRoster || [])
  const opponentRoster = computed(() => summary.value?.opponentRoster || [])
  const exchangeCandidates = computed(() => (opponentRoster.value.length ? opponentRoster.value : (summary.value?.opponentTeam || [])))

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

  const playerActiveMons = computed(() => {
    const activeSlots = summary.value?.playerActiveSlots || []
    return activeSlots
      .map((teamIndex, fieldSlot) => {
        const mon = playerTeam.value?.[teamIndex]
        if (!mon) return null
        return {
          ...mon,
          teamIndex,
          fieldSlot,
          maxHp: mon?.stats?.hp || mon?.currentHp || 0
        }
      })
      .filter(Boolean)
  })

  const opponentActiveOptions = computed(() => {
    const activeSlots = summary.value?.opponentActiveSlots || []
    const opponentTeam = summary.value?.opponentTeam || []
    return activeSlots.map((teamIndex, fieldSlot) => ({
      value: fieldSlot,
      label: opponentTeam?.[teamIndex]?.name || opponentTeam?.[teamIndex]?.name_en || `对手 ${fieldSlot + 1}`
    }))
  })

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

  const replacementBenchOptions = computed(() => {
    const allowed = new Set(summary.value?.playerPendingReplacementOptions || [])
    return playerBenchOptions.value
      .filter((pokemon) => allowed.has(pokemon.value))
      .map((pokemon) => ({
        ...pokemon,
        types: formatPokemonTypes(playerTeam.value?.[pokemon.value]?.types)
      }))
  })

  const canConfirmPreview = computed(() => {
    return selectedRosterIndexes.value.length === 4
      && leadRosterIndexes.value.length === 2
      && leadRosterIndexes.value.every((index) => selectedRosterIndexes.value.includes(index))
  })

  function selectedMoveObject(mon) {
    const moveName = selectedMoves.value[`slot-${mon.fieldSlot}`]
    return (mon?.moves || []).find((move) => (move.name_en || move.name) === moveName) || null
  }

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

  const tierBgClass = computed(() => {
    switch (playerProfile.value?.tier) {
      case 3:
        return 'bg-purple-50'
      case 2:
        return 'bg-amber-50'
      case 1:
        return 'bg-blue-50'
      default:
        return 'bg-slate-50'
    }
  })

  const tierTextClass = computed(() => {
    switch (playerProfile.value?.tier) {
      case 3:
        return 'text-purple-700'
      case 2:
        return 'text-amber-700'
      case 1:
        return 'text-blue-700'
      default:
        return 'text-slate-700'
    }
  })

  const statusText = computed(() => {
    if (!summary.value) return '未开始'
    if (isPreviewPhase.value) return '队伍预览中'
    if (isReplacementPhase.value) return '补位选择中'
    if (summary.value.status === 'completed') {
      return `已结束 · ${summary.value.winner === 'player' ? '玩家胜利' : '对手胜利'}`
    }
    return `进行中 · 第 ${summary.value.currentRound || 0} 回合`
  })

  return {
    actionDescription,
    actionHeadline,
    availableActionCount,
    availableActionDescription,
    canConfirmPreview,
    canConfirmReplacement,
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
    statusText,
    statusTone,
    tierBgClass,
    tierDisplayName,
    tierTextClass
  }
}