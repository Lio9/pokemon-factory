import { API_ROOT, requestData } from '../httpClient'

export const battleApi = {
  start: (body) => requestData(`${API_ROOT}/battle/start`, { method: 'POST', body: JSON.stringify(body) }),
  startAsync: (body) => requestData(`${API_ROOT}/battle/start-async`, { method: 'POST', body: JSON.stringify(body) }),
  status: (battleId) => requestData(`${API_ROOT}/battle/status/${battleId}`),
  pool: (rank) => requestData(`${API_ROOT}/battle/pool?rank=${rank || ''}`),
  preview: (battleId, body) => requestData(`${API_ROOT}/battle/${battleId}/preview`, { method: 'POST', body: JSON.stringify(body) }),
  replacement: (battleId, body) => requestData(`${API_ROOT}/battle/${battleId}/replacement`, { method: 'POST', body: JSON.stringify(body) }),
  exchange: (body) => requestData(`${API_ROOT}/battle/exchange`, { method: 'POST', body: JSON.stringify(body) }),
  move: (battleId, body) => requestData(`${API_ROOT}/battle/${battleId}/move`, { method: 'POST', body: JSON.stringify(body) }),
  forfeit: (battleId) => requestData(`${API_ROOT}/battle/${battleId}/forfeit`, { method: 'POST' }),
  factoryStart: () => requestData(`${API_ROOT}/battle/factory/start`, { method: 'POST' }),
  factoryNext: (runId) => requestData(`${API_ROOT}/battle/factory/${runId}/next`, { method: 'POST' }),
  factoryAbandon: () => requestData(`${API_ROOT}/battle/factory/abandon`, { method: 'POST' }),
  factoryStatus: () => requestData(`${API_ROOT}/battle/factory/status`),
  profile: () => requestData(`${API_ROOT}/battle/profile`),
  leaderboard: (limit = 50) => requestData(`${API_ROOT}/battle/leaderboard?limit=${limit}`)
}