const API_BASE = normalizeBaseUrl(import.meta.env.VITE_API_BASE, '/api/pokedex')
const DAMAGE_API_BASE = normalizeBaseUrl(import.meta.env.VITE_DAMAGE_API_BASE, '/api/damage')
const SPRITES_BASE = normalizeBaseUrl(import.meta.env.VITE_SPRITES_BASE, 'http://127.0.0.1:8080')
const API_ROOT = API_BASE.replace(/\/api\/pokedex$/, '/api')

function normalizeBaseUrl(value, fallback) {
  const normalized = value && String(value).trim()
  return normalized ? normalized.replace(/\/$/, '') : fallback
}

async function request(url, options = {}) {
  const token = localStorage.getItem('jwt_token')
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  const payload = await parseResponseBody(response)

  if (!response.ok) {
    throw new Error(payload?.message || payload?.error || `HTTP error! status: ${response.status}`)
  }

  return payload
}

async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }

  const text = await response.text()
  return text ? { message: text } : null
}

export const pokemonApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 24,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generationId && { generationId: params.generationId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/pokemon/list?${queryParams}`)
  },

  getDetail: (id) => request(`${API_BASE}/pokemon/${id}`),

  getMoves: (id) => request(`${API_ROOT}/pokemon/${id}/moves`),

  getEvolutionChain: (id) => request(`${API_ROOT}/pokemon/${id}/evolution`),

  getAbilities: (id) => request(`${API_BASE}/pokemon/${id}`).then((result) => ({
    code: result.code,
    message: result.message,
    data: result.data?.abilities || []
  })),

  getFormMoves: (formId, versionGroupId) => {
    const queryParams = new URLSearchParams({
      ...(versionGroupId && { versionGroupId })
    })
    return request(`${API_BASE}/form/${formId}/moves?${queryParams}`)
  }
}

export const typeApi = {
  getAll: () => request(`${API_BASE}/types`)
}

export const abilityApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/abilities/list?${queryParams}`)
  }
}

export const moveApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/moves/list?${queryParams}`)
  }
}

export const itemApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.categoryId && { categoryId: params.categoryId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/items/list?${queryParams}`)
  },

  getBattleItems: () => {
    const queryParams = new URLSearchParams({
      current: 1,
      size: 200
    })
    return request(`${API_BASE}/items/list?${queryParams}`)
  }
}

export const importApi = {
  startAll: () => request(`${API_ROOT}/import-optimized/all-fast`, { method: 'POST' }),
  getStatus: (taskId) => request(`${API_ROOT}/import-optimized/import-status/${taskId}`)
}

export const damageApi = {
  calculate: (params) => request(`${DAMAGE_API_BASE}/calculate`, {
    method: 'POST',
    body: JSON.stringify(params)
  }),

  getTypeEfficacy: () => request(`${DAMAGE_API_BASE}/type-efficacy`),

  getTypeEfficacyByType: (typeId) => request(`${DAMAGE_API_BASE}/type-efficacy/${typeId}`)
}

export const userApi = {
  login: (body) => request(`${API_ROOT}/user/login`, { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => request(`${API_ROOT}/user/register`, { method: 'POST', body: JSON.stringify(body) })
}

export const battleApi = {
  start: (body) => request(`${API_ROOT}/battle/start`, { method: 'POST', body: JSON.stringify(body) }),
  startAsync: (body) => request(`${API_ROOT}/battle/start-async`, { method: 'POST', body: JSON.stringify(body) }),
  status: (battleId) => request(`${API_ROOT}/battle/status/${battleId}`),
  pool: (rank) => request(`${API_ROOT}/battle/pool?rank=${rank || ''}`),
  preview: (battleId, body) => request(`${API_ROOT}/battle/${battleId}/preview`, { method: 'POST', body: JSON.stringify(body) }),
  replacement: (battleId, body) => request(`${API_ROOT}/battle/${battleId}/replacement`, { method: 'POST', body: JSON.stringify(body) }),
  exchange: (body) => request(`${API_ROOT}/battle/exchange`, { method: 'POST', body: JSON.stringify(body) }),
  move: (battleId, body) => request(`${API_ROOT}/battle/${battleId}/move`, { method: 'POST', body: JSON.stringify(body) })
}

export const sprites = {
  pokemon: (id) => `${SPRITES_BASE}/pokemon/${id}.png`,
  official: (id) => `${SPRITES_BASE}/pokemon/other/official-artwork/${id}.png`,
  type: (id) => `${SPRITES_BASE}/types/${id}.png`,
  item: (name) => `${SPRITES_BASE}/items/${name}.png`,
  default: `${SPRITES_BASE}/pokemon/0.png`
}

export default {
  pokemon: pokemonApi,
  types: typeApi,
  abilities: abilityApi,
  moves: moveApi,
  items: itemApi,
  import: importApi,
  damage: damageApi,
  user: userApi,
  battle: battleApi,
  sprites
}
