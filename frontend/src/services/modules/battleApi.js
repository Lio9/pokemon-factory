/**
 * ============================================================
 * 对战 API / Battle API Module
 * ============================================================
 *
 * 封装对战工厂相关的所有后端接口调用。
 * Wraps all Battle Factory backend API calls.
 *
 * 接口 / Endpoints:
 * - start / startAsync / status / action / state / forfeit
 * - pool / leaderboard / factory: start/next/abandon
 * - guest: start
 *
 * @module services/modules/battleApi
 */
import { BATTLE_API_BASE, API_ROOT, requestData } from '../httpClient'

/**
 * 游客对战 API / Guest Battle API
 *
 * 无需 JWT 认证，适合未登录用户快速体验。
 * No JWT required — for quick try-before-login experience.
 *
 * 使用流程 / Usage Flow:
 *   guestStart(team) → { battleId, guestId }
 *   guestPreview(battleId, selection) → state
 *   guestMove(battleId, moves) → state
 *   guestStatus(battleId) → state
 */
export const guestApi = {
  /** 开始游客对战 / Start guest battle */
  start: (body) => requestData(`${BATTLE_API_BASE}/battle/guest/start`, { method: 'POST', body: JSON.stringify(body) }),
  /** 确认首发 / Confirm team preview */
  preview: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/guest/${battleId}/preview`, { method: 'POST', body: JSON.stringify(body) }),
  /** 确认替补 / Confirm replacement */
  replacement: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/guest/${battleId}/replacement`, { method: 'POST', body: JSON.stringify(body) }),
  /** 提交出招 / Submit move */
  move: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/guest/${battleId}/move`, { method: 'POST', body: JSON.stringify(body) }),
  /** 认输 / Forfeit */
  forfeit: (battleId) => requestData(`${BATTLE_API_BASE}/battle/guest/${battleId}/forfeit`, { method: 'POST', body: JSON.stringify({}) }),
  /** 查询状态 / Get battle status */
  status: (battleId) => requestData(`${BATTLE_API_BASE}/battle/guest/status/${battleId}`),
}

export const battleApi = {
  start: (body) => requestData(`${BATTLE_API_BASE}/battle/start`, { method: 'POST', body: JSON.stringify(body) }),
  startAsync: (body) => requestData(`${BATTLE_API_BASE}/battle/start-async`, { method: 'POST', body: JSON.stringify(body) }),
  status: (battleId) => requestData(`${BATTLE_API_BASE}/battle/status/${battleId}`),
  pool: (rank) => requestData(`${BATTLE_API_BASE}/battle/pool?rank=${rank || ''}`),
  preview: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/${battleId}/preview`, { method: 'POST', body: JSON.stringify(body) }),
  replacement: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/${battleId}/replacement`, { method: 'POST', body: JSON.stringify(body) }),
  exchange: (body) => requestData(`${BATTLE_API_BASE}/battle/exchange`, { method: 'POST', body: JSON.stringify(body) }),
  move: (battleId, body) => requestData(`${BATTLE_API_BASE}/battle/${battleId}/move`, { method: 'POST', body: JSON.stringify(body) }),
  forfeit: (battleId) => requestData(`${API_ROOT}/battle/${battleId}/forfeit`, { method: 'POST' }),
  factoryStart: () => requestData(`${API_ROOT}/battle/factory/start`, { method: 'POST' }),
  factoryNext: (runId) => requestData(`${API_ROOT}/battle/factory/${runId}/next`, { method: 'POST' }),
  factoryAbandon: () => requestData(`${API_ROOT}/battle/factory/abandon`, { method: 'POST' }),
  factoryStatus: () => requestData(`${API_ROOT}/battle/factory/status`),
  profile: () => requestData(`${API_ROOT}/battle/profile`),
  leaderboard: (limit = 50) => requestData(`${API_ROOT}/battle/leaderboard?limit=${limit}`)
}
