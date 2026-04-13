export const TIER_NAMES = ['精灵球', '超级球', '高级球', '大师球']

export function normalizeFactoryRun(raw) {
  if (!raw) {
    return null
  }

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

export function formatPokemonTypes(types) {
  return (types || []).map((type) => type.name || type.name_zh || `属性${type.type_id}`).join(' / ') || '未知属性'
}

export function moveNeedsOpponentTarget(move) {
  const targetId = Number(move?.target_id || 10)
  return targetId === 10
}

export function moveTargetText(move) {
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

export function normalizeBattlePayload(payload) {
  const nextSummary = payload?.summary || payload?.battle?.summary || null
  let summary = nextSummary
  if (!summary && payload?.battle?.summary_json) {
    try {
      summary = typeof payload.battle.summary_json === 'string'
        ? JSON.parse(payload.battle.summary_json)
        : payload.battle.summary_json
    } catch {
      summary = null
    }
  }

  return {
    summary,
    battleId: payload?.battleId || payload?.battle?.id || null
  }
}

export function buildBattleSettlement(summary, payload) {
  if (summary?.status !== 'completed') {
    return null
  }

  const factory = summary?.factory || {}
  const meta = payload?.factoryMeta || factory
  const won = summary.winner === 'player'

  let tierChange = null
  let newTierName = null
  if (meta.promoted) {
    tierChange = 'promoted'
    newTierName = meta.playerTierName || TIER_NAMES[meta.playerTier] || ''
  } else if (meta.demoted) {
    tierChange = 'demoted'
    newTierName = meta.playerTierName || TIER_NAMES[meta.playerTier] || ''
  }

  return {
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
}

export function buildMoveSubmission({
  playerActiveMons,
  selectedActions,
  selectedMoves,
  selectedSwitchTargets,
  selectedTargets,
  selectedMoveObject
}) {
  const playerMoveMap = {}
  for (const mon of playerActiveMons) {
    const actionType = selectedActions[`action-slot-${mon.fieldSlot}`] || 'move'
    playerMoveMap[`action-slot-${mon.fieldSlot}`] = actionType
    if (actionType === 'switch') {
      playerMoveMap[`switch-slot-${mon.fieldSlot}`] = String(selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`])
      continue
    }

    playerMoveMap[`slot-${mon.fieldSlot}`] = selectedMoves[`slot-${mon.fieldSlot}`]
    if (moveNeedsOpponentTarget(selectedMoveObject(mon))) {
      playerMoveMap[`target-slot-${mon.fieldSlot}`] = String(selectedTargets[`target-slot-${mon.fieldSlot}`])
    }
  }
  return playerMoveMap
}