import { computed } from 'vue'
import { formatPokemonTypes, getTierName, moveNeedsOpponentTarget } from '../../services/contracts/battleContract'
import { translate } from '../useLocale'

const TYPE_EFFECTIVENESS = {
  1: { 6: 50, 8: 0, 9: 50 },
  2: { 1: 200, 3: 50, 4: 50, 6: 200, 7: 50, 8: 0, 9: 200, 15: 200, 17: 200, 18: 50 },
  3: { 2: 200, 6: 50, 7: 200, 9: 50, 12: 200, 13: 50 },
  4: { 4: 50, 5: 50, 6: 50, 8: 50, 12: 200, 18: 200 },
  5: { 3: 0, 4: 200, 6: 200, 9: 200, 10: 200, 12: 50, 13: 200 },
  6: { 2: 50, 3: 200, 7: 200, 9: 50, 10: 200, 15: 200 },
  7: { 2: 50, 3: 50, 4: 50, 8: 50, 9: 50, 10: 50, 12: 200, 14: 200, 17: 200, 18: 50 },
  8: { 1: 0, 8: 200, 14: 200, 17: 50 },
  9: { 6: 200, 9: 50, 10: 50, 11: 50, 18: 200 },
  10: { 6: 50, 7: 200, 9: 200, 10: 50, 11: 50, 12: 200, 15: 200, 16: 50 },
  11: { 5: 200, 6: 200, 10: 200, 11: 50, 12: 50, 16: 50 },
  12: { 3: 50, 4: 50, 5: 200, 6: 200, 7: 50, 9: 50, 10: 50, 11: 200, 12: 50, 16: 50 },
  13: { 3: 200, 5: 0, 11: 200, 12: 50, 13: 50, 16: 50 },
  14: { 2: 200, 4: 200, 9: 50, 14: 50, 17: 0 },
  15: { 3: 200, 5: 200, 9: 50, 10: 50, 11: 50, 12: 200, 15: 50, 16: 200 },
  16: { 9: 50, 16: 200, 18: 0 },
  17: { 2: 50, 8: 200, 14: 200, 17: 50, 18: 50 },
  18: { 2: 200, 4: 50, 9: 50, 10: 50, 16: 200, 17: 200 }
}

function resolveTypeId(type) {
  return Number(type?.type_id ?? type?.typeId ?? 0)
}

function typeMultiplier(moveTypeId, defendingTypes) {
  return (defendingTypes || []).reduce((multiplier, type) => {
    const factor = TYPE_EFFECTIVENESS[moveTypeId]?.[resolveTypeId(type)] ?? 100
    return multiplier * (factor / 100)
  }, 1)
}

function effectivenessDescriptor(multiplier) {
  if (multiplier === 0) {
    return { label: translate('无效', 'No effect'), className: 'border-slate-300 bg-slate-100 text-slate-700' }
  }
  if (multiplier >= 4) {
    return { label: translate('极其有效 · 4倍', 'Super effective · 4x'), className: 'border-rose-300 bg-rose-50 text-rose-700' }
  }
  if (multiplier >= 2) {
    return { label: translate('有效 · 2倍', 'Effective · 2x'), className: 'border-amber-300 bg-amber-50 text-amber-700' }
  }
  if (multiplier <= 0.25) {
    return { label: translate('抵抗 · 0.25倍', 'Resisted · 0.25x'), className: 'border-sky-300 bg-sky-50 text-sky-700' }
  }
  if (multiplier < 1) {
    return { label: translate('抵抗 · 0.5倍', 'Resisted · 0.5x'), className: 'border-cyan-300 bg-cyan-50 text-cyan-700' }
  }
  return { label: translate('等倍 · 1倍', 'Neutral · 1x'), className: 'border-emerald-300 bg-emerald-50 text-emerald-700' }
}

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
  const tierDisplayName = computed(() => getTierName(playerProfile.value?.tier ?? 0))
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
      return factoryRun.value ? translate('准备进入下一轮', 'Prepare for the next round') : translate('选择一种开始方式', 'Choose how to start')
    }
    if (isPreviewPhase.value) return translate('先完成 6 选 4 与首发', 'Finish 6v4 selection and leads first')
    if (isReplacementPhase.value) return translate('需要补位 {count} 只', 'Need {count} replacements', { count: pendingReplacementCount.value })
    if (summary.value?.status === 'completed') {
      return summary.value?.winner === 'player'
        ? translate('本场获胜，检查结算与奖励', 'Battle won, review rewards and settlement')
        : translate('本场结束，准备下一步', 'Battle finished, prepare for the next step')
    }
    if (pollingActive.value) return translate('等待异步模拟推进', 'Waiting for async simulation')
    return translate('为当前回合配置动作', 'Set actions for this turn')
  })

  const actionDescription = computed(() => {
    if (!currentBattleId.value) {
      return factoryRun.value
        ? translate('当前已有一个激活中的工厂挑战，但还未进入下一战。可以继续推进，也可以直接放弃这次挑战。', 'An active factory run already exists, but the next battle has not started yet. You can continue it or abandon the run directly.')
        : translate('如果你想完整体验工厂流程，直接开始 9 连战；如果只是验证战斗逻辑，单场手动或异步模拟更快。', 'Start the 9-battle run if you want the full factory flow. If you only want to verify battle logic, a single manual battle or async simulation is faster.')
    }
    if (isPreviewPhase.value) {
      return translate('从你的 6 只宝可梦中选 4 只参战，再从中指定 2 只作为首发。只有完成这一阶段后，回合操作区才会解锁。', 'Pick 4 Pokemon from your roster of 6, then assign 2 of them as leads. The action panel unlocks only after this step is complete.')
    }
    if (isReplacementPhase.value) {
      return translate('当前场上有宝可梦倒下，必须先补位才能继续推进战斗。前端已经按后端允许列表限制了可选替补。', 'A Pokemon has fainted on the field. You must send in replacements before the battle can continue. The UI already limits choices to the backend-approved bench list.')
    }
    if (summary.value?.status === 'completed') {
      return factoryRun.value
        ? translate('查看积分变化、段位浮动和本轮结果。若挑战尚未结束，可以继续推进下一轮。', 'Review point changes, rank movement, and the round result. If the run is still alive, you can continue straight into the next battle.')
        : translate('可以复盘战斗日志，也可以直接开启下一场。胜利时如果触发交换奖励，会自动弹出交换面板。', 'Review the battle log or jump into the next match. If you earned an exchange reward, the swap panel opens automatically.')
    }
    if (pollingActive.value) {
      return translate('当前是后台自动模拟，页面会持续轮询并在结束时自动停下。你仍然可以手动点刷新，或直接等待结果。', 'The battle is running in async mode. This page keeps polling and stops automatically when the match finishes. You can still refresh manually or just wait for the result.')
    }
    return translate('为场上的两只宝可梦分别选择出招或换人；如果招式需要指定单体目标，目标选择会自动出现。', 'Choose a move or switch for each active Pokemon. If a move needs a single target, target selection appears automatically.')
  })

  const progressSummary = computed(() => {
    if (factoryRun.value) {
      return translate('工厂挑战 {wins} 胜 {losses} 负，第 {current} / {max} 轮。', 'Factory run: {wins} wins, {losses} losses, round {current} / {max}.', {
        wins: factoryRun.value.wins || 0,
        losses: factoryRun.value.losses || 0,
        current: factoryRun.value.current_battle || 0,
        max: factoryRun.value.max_battles || 9
      })
    }
    if (summary.value?.status === 'completed') {
      return summary.value?.winner === 'player'
        ? translate('本场已结束，玩家取得胜利。', 'This battle is over. The player won.')
        : translate('本场已结束，对手取得胜利。', 'This battle is over. The opponent won.')
    }
    if (currentBattleId.value) {
      return translate('当前战斗编号 #{id}，已推进到第 {round} 回合。', 'Current battle #{id}, now at round {round}.', {
        id: currentBattleId.value,
        round: summary.value?.currentRound || 0
      })
    }
    return translate('当前没有进行中的战斗，可以从右侧操作区直接开始。', 'There is no active battle. You can start one directly from the action panel.')
  })

  const recommendedMode = computed(() => {
    if (factoryRun.value) return translate('继续工厂挑战', 'Continue the factory run')
    if (currentBattleId.value && pollingActive.value) return translate('观察异步模拟', 'Watch async simulation')
    if (currentBattleId.value) return translate('继续当前对战', 'Continue current battle')
    return translate('开始 9 连战', 'Start the 9-battle run')
  })

  const modeSummary = computed(() => {
    if (factoryRun.value) return translate('工厂挑战', 'Factory challenge')
    if (pollingActive.value) return translate('异步模拟', 'Async simulation')
    if (currentBattleId.value) return translate('手动对战', 'Manual battle')
    return translate('待开始', 'Not started')
  })

  const modeDescription = computed(() => {
    if (factoryRun.value) return translate('工厂模式会保留挑战进度，并在每轮结束后结算积分。', 'Factory mode preserves run progress and settles rating after each round.')
    if (pollingActive.value) return translate('后台自动推进战斗，适合快速检验队伍生成和结算链路。', 'Async mode auto-advances the battle and is useful for quickly validating generation and resolution flow.')
    if (currentBattleId.value) return translate('每个回合都需要你手动提交动作，适合检查细节。', 'Manual mode requires explicit choices every turn and is better for inspecting details.')
    return translate('选择开始方式后，这里会自动显示当前战斗模式。', 'The active battle mode will appear here once you choose how to start.')
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
       label: opponentTeam?.[teamIndex]?.name || opponentTeam?.[teamIndex]?.name_en || translate('对手 {slot}', 'Opponent {slot}', { slot: fieldSlot + 1 })
    }))
  })

  const opponentActiveMons = computed(() => {
    const activeSlots = summary.value?.opponentActiveSlots || []
    const opponentTeam = summary.value?.opponentTeam || []
    return activeSlots
      .map((teamIndex, fieldSlot) => {
        const mon = opponentTeam?.[teamIndex]
        if (!mon) return null
        return {
          ...mon,
          teamIndex,
          fieldSlot
        }
      })
      .filter(Boolean)
  })

  const playerBenchOptions = computed(() => {
    const activeSlots = summary.value?.playerActiveSlots || []
    return playerTeam.value
      .map((pokemon, teamIndex) => ({
        value: teamIndex,
         label: pokemon?.name || pokemon?.name_en || translate('替补 {slot}', 'Bench {slot}', { slot: teamIndex + 1 }),
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

  function canUseSpecialSystem(mon, system) {
    if (!mon || summary.value?.playerSpecialUsed) {
      return false
    }
    switch (system) {
      case 'tera':
        return !mon.terastallized && Number(mon.teraTypeId || mon?.teraType?.type_id || 0) > 0
      case 'mega':
        return Boolean(mon.megaEligible) && !mon.megaEvolved
      case 'z-move':
        return Boolean(mon.zMoveEligible) && !mon.zMoveUsed
      case 'dynamax':
        return Boolean(mon.dynamaxEligible) && !mon.dynamaxed
      default:
        return false
    }
  }

  function availableSpecialSystems(mon) {
    const systems = Array.isArray(mon?.specialSystems) ? mon.specialSystems : []
    return systems.filter((system) => canUseSpecialSystem(mon, system))
  }

  function canTerastallize(mon) {
    return canUseSpecialSystem(mon, 'tera')
  }

  function teraTypeLabel(mon) {
    const teraType = mon?.teraType || {}
    return teraType.name || teraType.name_en || `属性${teraType.type_id || mon?.teraTypeId || ''}`
  }

  function specialSystemLabel(system) {
    switch (system) {
      case 'tera':
        return '太晶化'
      case 'mega':
        return 'Mega 进化'
      case 'z-move':
        return 'Z 招式'
      case 'dynamax':
        return '极巨化'
      default:
        return system || '特殊系统'
    }
  }

  function activeSpecialSystemLabel(mon) {
    const system = mon?.specialSystemActivated
      || (mon?.terastallized ? 'tera' : null)
      || (mon?.megaEvolved ? 'mega' : null)
      || (mon?.dynamaxed ? 'dynamax' : null)
    return system ? specialSystemLabel(system) : ''
  }

  function moveEffectivenessHints(mon) {
    const move = selectedMoveObject(mon)
    if (!move || Number(move?.power || 0) <= 0) {
      return []
    }

    const moveTypeId = Number(move?.type_id || 0)
    if (moveTypeId <= 0) {
      return []
    }

    const targetId = Number(move?.target_id || 10)
    if (targetId === 7 || targetId === 13) {
      return []
    }

    let targets = opponentActiveMons.value
    if (moveNeedsOpponentTarget(move)) {
      const selectedTargetSlot = selectedTargets.value[`target-slot-${mon.fieldSlot}`]
      if (selectedTargetSlot !== undefined && selectedTargetSlot !== null) {
        targets = opponentActiveMons.value.filter((target) => target.fieldSlot === Number(selectedTargetSlot))
      }
    }

    return targets.map((target) => {
      const multiplier = typeMultiplier(moveTypeId, target.types || [])
      const descriptor = effectivenessDescriptor(multiplier)
      return {
        key: `${mon.fieldSlot}-${target.fieldSlot}`,
        targetLabel: `对手槽位 ${target.fieldSlot + 1} · ${target.name || target.name_en || `目标 ${target.fieldSlot + 1}`}`,
        label: descriptor.label,
        className: descriptor.className
      }
    })
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
    if (!currentBattleId.value) return translate('开始、继续、放弃、查看排行榜都在当前面板可直接完成。', 'Start, continue, abandon, and leaderboard actions are all available from this panel.')
    if (isPreviewPhase.value) {
      return canConfirmPreview.value
        ? translate('阵容已满足条件，可以直接确认。', 'The preview setup is complete and ready to confirm.')
        : translate('还未满足 4 只参战与 2 只首发的条件。', 'You still need 4 selected Pokemon and 2 leads.')
    }
    if (isReplacementPhase.value) {
      return canConfirmReplacement.value
        ? translate('补位人数已满足条件，可以继续战斗。', 'Replacement requirements are satisfied. You can continue the battle.')
        : translate('需要先补齐替补人数。', 'You still need to choose all required replacements.')
    }
    if (summary.value?.status === 'completed') return translate('可以查看结算、继续下一轮或重新开始。', 'You can review settlement, continue to the next round, or reset.')
    return canSubmitMove.value
      ? translate('当前回合动作已完整，可以提交或刷新状态。', 'This turn is fully configured. You can submit or refresh the state.')
      : translate('还需要先补齐行动、目标或换人对象。', 'You still need to complete actions, targets, or switch choices.')
  })

  const showContinueFactoryButton = computed(() => Boolean(factoryRun.value && currentBattleId.value && summary.value?.status === 'completed' && !settlement.value?.runFinished))
  const showResetBattleButton = computed(() => Boolean(currentBattleId.value && summary.value?.status === 'completed'))
  const mobileActionButtons = computed(() => {
    if (!currentBattleId.value && !factoryRun.value) {
      return [
        { key: 'start-factory', label: translate('开始挑战', 'Start run'), tone: 'primary', disabled: isBusy.value },
        { key: 'start-manual', label: translate('手动对战', 'Manual'), tone: 'secondary', disabled: isBusy.value },
        { key: 'start-async', label: translate('异步模拟', 'Async'), tone: 'secondary', disabled: isBusy.value }
      ]
    }
    if (factoryRun.value && !currentBattleId.value) {
      return [
        { key: 'next-factory', label: translate('进入下一轮', 'Next round'), tone: 'primary', disabled: isBusy.value },
        { key: 'abandon-factory', label: translate('放弃挑战', 'Abandon run'), tone: 'danger', disabled: isBusy.value }
      ]
    }
    if (isPreviewPhase.value) {
      return [
        { key: 'confirm-preview', label: translate('确认预览选择', 'Confirm preview'), tone: 'primary', disabled: isBusy.value || !canConfirmPreview.value },
        { key: 'refresh-status', label: translate('刷新状态', 'Refresh'), tone: 'secondary', disabled: isBusy.value }
      ]
    }
    if (isReplacementPhase.value) {
      return [
        { key: 'confirm-replacement', label: translate('确认替补', 'Confirm replacement'), tone: 'primary', disabled: isBusy.value || !canConfirmReplacement.value },
        { key: 'refresh-status', label: translate('刷新状态', 'Refresh'), tone: 'secondary', disabled: isBusy.value }
      ]
    }
    if (summary.value?.status === 'completed') {
      if (showContinueFactoryButton.value) {
        return [
           { key: 'prepare-next', label: translate('准备下一轮', 'Prepare next'), tone: 'primary', disabled: isBusy.value },
           { key: 'reset-battle', label: translate('清空战场', 'Reset arena'), tone: 'secondary', disabled: isBusy.value }
        ]
      }
      return [
         { key: 'reset-battle', label: translate('清空战场', 'Reset arena'), tone: 'primary', disabled: isBusy.value },
         { key: 'open-leaderboard', label: translate('看排行榜', 'Leaderboard'), tone: 'secondary', disabled: isBusy.value }
      ]
    }
    return [
       { key: 'submit-move', label: translate('提交回合', 'Submit turn'), tone: 'primary', disabled: isBusy.value || !canSubmitMove.value },
       { key: 'refresh-status', label: translate('刷新状态', 'Refresh'), tone: 'secondary', disabled: isBusy.value }
    ]
  })

  const showMobileActionDock = computed(() => mobileActionButtons.value.length > 0)
  const lastUpdatedLabel = computed(() => {
    if (!lastUpdatedAt.value) return ''
    return new Date(lastUpdatedAt.value).toLocaleTimeString(translate('zh-CN', 'en-US'), {
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
    if (!summary.value) return translate('未开始', 'Not started')
    if (isPreviewPhase.value) return translate('队伍预览中', 'Team preview')
    if (isReplacementPhase.value) return translate('补位选择中', 'Choosing replacements')
    if (summary.value.status === 'completed') {
      return summary.value.winner === 'player'
        ? translate('已结束 · 玩家胜利', 'Finished · Player won')
        : translate('已结束 · 对手胜利', 'Finished · Opponent won')
    }
    return translate('进行中 · 第 {round} 回合', 'In progress · Round {round}', { round: summary.value.currentRound || 0 })
  })

  return {
    actionDescription,
    actionHeadline,
    availableActionCount,
    availableActionDescription,
    canConfirmPreview,
    canConfirmReplacement,
    availableSpecialSystems,
    activeSpecialSystemLabel,
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
    moveEffectivenessHints,
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
  }
}
