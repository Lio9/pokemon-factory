/*
 * catalogApi 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端接口模块文件。
 * 核心职责：负责按业务域封装请求入口，统一屏蔽接口路径与调用细节。
 * 阅读建议：建议对照 contracts 与 httpClient 一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
      ...(params.generation && { generation: params.generation }), // 增加世代筛选
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/moves/list?${queryParams}`)
  }
}

export const pokemonApi = {
  getList: (params = {}) => {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 50,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generation && { generation: params.generation }), // 增加世代筛选
      ...(params.keyword && { keyword: params.keyword })
    })
    return request(`${API_BASE}/pokemon/list?${queryParams}`)
  },
  getDetail: (id) => request(`${API_BASE}/pokemon/${id}`),
  getEvolutionChain: (speciesId) => request(`${API_BASE}/pokemon/evolution/${speciesId}`)
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