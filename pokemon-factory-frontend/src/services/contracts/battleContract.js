import { translate } from '../../composables/useLocale'

const TIER_NAMES_ZH = ['精灵球', '超级球', '高级球', '大师球']
const TIER_NAMES_EN = ['Poke Ball', 'Great Ball', 'Ultra Ball', 'Master Ball']

export function getTierName(tier, fallbackName = '') {
  const fallbackByTier = translate(TIER_NAMES_ZH[tier] || TIER_NAMES_ZH[0], TIER_NAMES_EN[tier] || TIER_NAMES_EN[0])
  return translate(fallbackName || fallbackByTier, fallbackByTier)
}

export function normalizeFactoryRun(raw) {
  if (!raw) {
    return null
  }

  return {
    id: raw.id ?? raw.runId ?? null,
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
  return (types || [])
    .map((type) => translate(type.name || type.name_zh || `属性${type.type_id}`, type.name_en || type.name || `Type ${type.type_id}`))
    .join(' / ') || translate('未知属性', 'Unknown Type')
}

export function moveNeedsOpponentTarget(move) {
  const targetId = Number(move?.target_id || 10)
  return targetId === 10
}

export function moveTargetText(move) {
  const targetId = Number(move?.target_id || 10)
  switch (targetId) {
    case 7:
      return translate('目标：自身', 'Target: Self')
    case 8:
      return translate('目标：随机对手', 'Target: Random foe')
    case 9:
      return translate('目标：场上其他宝可梦', 'Target: Other active Pokemon')
    case 11:
      return translate('目标：对手全体', 'Target: All foes')
    case 13:
      return translate('目标：自身与队友', 'Target: Self and ally')
    case 14:
      return translate('目标：场上全体', 'Target: All active Pokemon')
    default:
      return translate('目标：单体', 'Target: Single foe')
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
    newTierName = getTierName(meta.playerTier, meta.playerTierName || '')
  } else if (meta.demoted) {
    tierChange = 'demoted'
    newTierName = getTierName(meta.playerTier, meta.playerTierName || '')
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
  selectedSpecialSystems,
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
    const specialSystem = selectedSpecialSystems?.[`special-slot-${mon.fieldSlot}`]
    if (specialSystem) {
      playerMoveMap[`special-slot-${mon.fieldSlot}`] = String(specialSystem)
      if (specialSystem === 'tera') {
        playerMoveMap[`tera-slot-${mon.fieldSlot}`] = 'true'
      }
    }
    if (moveNeedsOpponentTarget(selectedMoveObject(mon))) {
      playerMoveMap[`target-slot-${mon.fieldSlot}`] = String(selectedTargets[`target-slot-${mon.fieldSlot}`])
    }
  }
  return playerMoveMap
}
