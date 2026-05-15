/**
 * 目录 API 服务 - 优化版
 * 
 * 技能、特性、物品、属性等静态数据API
 * 
 * @module services/modules/catalogApi
 */

import { API_BASE, DAMAGE_API_BASE, API_ROOT, request } from '../httpClient'
import dataCache from '../cache'

/**
 * 属性 API（长期缓存，很少变化）
 */
export const typeApi = {
  getAll() {
    return dataCache.getOrFetch(
      'catalog:types',
      {},
      () => request(`${API_BASE}/types`),
      'long'
    )
  }
}

/**
 * 特性 API
 */
export const abilityApi = {
  getList(params = {}) {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.keyword && { keyword: params.keyword })
    })
    
    return dataCache.getOrFetch(
      'catalog:abilities',
      params,
      () => request(`${API_BASE}/abilities/list?${queryParams}`),
      'normal'
    )
  }
}

/**
 * 技能 API
 */
export const moveApi = {
  getList(params = {}) {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generation && { generation: params.generation }),
      ...(params.keyword && { keyword: params.keyword })
    })
    
    return dataCache.getOrFetch(
      'catalog:moves',
      params,
      () => request(`${API_BASE}/moves/list?${queryParams}`),
      'normal'
    )
  }
}

/**
 * 物品 API
 */
export const itemApi = {
  getList(params = {}) {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 20,
      ...(params.categoryId && { categoryId: params.categoryId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    
    return dataCache.getOrFetch(
      'catalog:items',
      params,
      () => request(`${API_BASE}/items/list?${queryParams}`),
      'normal'
    )
  },

  /**
   * 获取对战常用物品（长期缓存）
   */
  getBattleItems() {
    const params = { current: 1, size: 200 }
    const queryParams = new URLSearchParams(params)
    
    return dataCache.getOrFetch(
      'catalog:battleItems',
      {},
      () => request(`${API_BASE}/items/list?${queryParams}`),
      'long'
    )
  }
}

/**
 * 导入 API（不缓存）
 */
export const importApi = {
  start() {
    return request(`${API_ROOT}/import-optimized/all-fast`, { method: 'POST' })
  },
  startAll() {
    return request(`${API_ROOT}/import-optimized/all-fast`, { method: 'POST' })
  },
  getStatus(taskId) {
    return request(`${API_ROOT}/import-optimized/import-status/${taskId}`)
  }
}

/**
 * 伤害计算 API
 */
export const damageApi = {
  calculate(params) {
    // 伤害计算不缓存，每次都重新计算
    return request(`${DAMAGE_API_BASE}/calculate`, {
      method: 'POST',
      body: JSON.stringify(params)
    })
  },
  
  getTypeEfficacy() {
    return dataCache.getOrFetch(
      'damage:typeEfficacy',
      {},
      () => request(`${DAMAGE_API_BASE}/type-efficacy`),
      'long'
    )
  },
  
  getTypeEfficacyByType(typeId) {
    return dataCache.getOrFetch(
      'damage:typeEfficacyByType',
      { typeId },
      () => request(`${DAMAGE_API_BASE}/type-efficacy/${typeId}`),
      'long'
    )
  }
}
