/**
 * 宝可梦 API 服务 - 优化版
 * 
 * 集成了高性能缓存系统，减少重复请求
 * 
 * @module services/modules/pokemonApi
 */

import { API_BASE, API_ROOT, request } from '../httpClient'
import dataCache from '../cache'

/**
 * 缓存键前缀
 */
const CACHE_PREFIX = 'pokemon'

/**
 * 获取宝可梦列表（带缓存）
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {number} params.typeId - 属性过滤
 * @param {number} params.generationId - 世代过滤
 * @param {string} params.keyword - 搜索关键词
 * @returns {Promise<Object>}
 */
export const pokemonApi = {
  getList(params = {}) {
    const queryParams = new URLSearchParams({
      current: params.current || 1,
      size: params.size || 24,
      ...(params.typeId && { typeId: params.typeId }),
      ...(params.generationId && { generationId: params.generationId }),
      ...(params.keyword && { keyword: params.keyword })
    })
    
    return dataCache.getOrFetch(
      `${CACHE_PREFIX}:list`,
      params,
      () => request(`${API_BASE}/pokemon/list?${queryParams}`),
      'normal'
    )
  },

  /**
   * 获取宝可梦详情（带长期缓存）
   * @param {number|string} id - 宝可梦ID
   * @returns {Promise<Object>}
   */
  getDetail(id) {
    return dataCache.getOrFetch(
      `${CACHE_PREFIX}:detail`,
      { id },
      () => request(`${API_BASE}/pokemon/${id}`),
      'long'
    )
  },

  /**
   * 获取宝可梦技能（带缓存）
   * @param {number|string} id - 宝可梦ID
   * @returns {Promise<Object>}
   */
  getMoves(id) {
    return dataCache.getOrFetch(
      `${CACHE_PREFIX}:moves`,
      { id },
      () => request(`${API_ROOT}/pokemon/${id}/moves`),
      'long'
    )
  },

  /**
   * 获取进化链（带长期缓存）
   * @param {number|string} id - 宝可梦ID
   * @returns {Promise<Object>}
   */
  getEvolutionChain(id) {
    return dataCache.getOrFetch(
      `${CACHE_PREFIX}:evolution`,
      { id },
      () => request(`${API_ROOT}/pokemon/${id}/evolution`),
      'long'
    )
  },

  /**
   * 获取特性（从详情中提取）
   * @param {number|string} id - 宝可梦ID
   * @returns {Promise<Object>}
   */
  getAbilities(id) {
    return this.getDetail(id).then((result) => ({
      code: result.code,
      message: result.message,
      data: result.data?.abilities || []
    }))
  },

  /**
   * 获取形态技能（带缓存）
   */
  getFormMoves(formId, versionGroupId) {
    const params = { formId, versionGroupId }
    const queryParams = new URLSearchParams({
      ...(versionGroupId && { versionGroupId })
    })
    
    return dataCache.getOrFetch(
      `${CACHE_PREFIX}:formMoves`,
      params,
      () => request(`${API_BASE}/form/${formId}/moves?${queryParams}`),
      'long'
    )
  }
}

export default pokemonApi
