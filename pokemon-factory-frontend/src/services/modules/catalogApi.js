import { API_BASE, DAMAGE_API_BASE, API_ROOT, request } from '../httpClient'

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
    const queryParams = new URLSearchParams({ current: 1, size: 200 })
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