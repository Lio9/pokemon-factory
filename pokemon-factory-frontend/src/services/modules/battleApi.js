/*
 * battleApi 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口模块文件。
 * 核心职责：负责按业务域封装请求入口，统一屏蔽接口路径与调用细节。
 * 阅读建议：建议对照 contracts 与 httpClient 一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { BATTLE_API_BASE, requestData } from '../httpClient'

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