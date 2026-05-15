/**
 * 宝可梦数据状态管理模块
 *
 * 本模块使用 Pinia 管理宝可梦详情页的数据状态。
 * 包括宝可梦基本信息、技能、特性、进化链等。
 *
 * @module stores/pokemon
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { pokemonApi } from '../services/api'
import { normalizePokemonCollection, normalizePokemonDetail } from '../services/contracts/pokemonContract'

/**
 * 宝可梦数据 Store
 *
 * 管理宝可梦详情页的数据获取和缓存。
 */
export const usePokemonStore = defineStore('pokemon', () => {
  // ========== 状态定义 ==========

  /** @type {Ref<Object|null>} 当前宝可梦详情 */
  const pokemon = ref(null)

  /** @type {Ref<Array>} 当前宝可梦可学技能列表 */
  const moves = ref([])

  /** @type {Ref<Array>} 当前宝可梦特性列表 */
  const abilities = ref([])

  /** @type {Ref<Array>} 当前宝可梦进化链 */
  const evolutions = ref([])

  /** @type {Ref<boolean>} 数据加载状态 */
  const loading = ref(false)

  /** @type {Ref<string|null>} 错误信息 */
  const error = ref(null)

  // ========== 操作方法 ==========

  /**
   * 重置所有状态
   */
  function reset() {
    pokemon.value = null
    moves.value = []
    abilities.value = []
    evolutions.value = []
    loading.value = false
    error.value = null
  }

  /**
   * 获取宝可梦技能列表
   *
   * @param {number|string} id - 宝可梦 ID
   */
  async function fetchMoves(id) {
    try {
      moves.value = normalizePokemonCollection(await pokemonApi.getMoves(id), [])
    } catch (err) {
      moves.value = []
      console.error('获取技能失败:', err)
    }
  }

  /**
   * 获取宝可梦特性列表
   *
   * @param {number|string} id - 宝可梦 ID
   */
  async function fetchAbilities(id) {
    try {
      abilities.value = normalizePokemonCollection(await pokemonApi.getAbilities(id), [])
    } catch (err) {
      abilities.value = []
      console.error('获取特性失败:', err)
    }
  }

  /**
   * 获取宝可梦进化链
   *
   * @param {number|string} id - 宝可梦 ID
   */
  async function fetchEvolutions(id) {
    try {
      evolutions.value = normalizePokemonCollection(await pokemonApi.getEvolutionChain(id), [])
    } catch (err) {
      evolutions.value = []
      console.error('获取进化链失败:', err)
    }
  }

  /**
   * 获取宝可梦完整详情
   *
   * 同时获取详情、技能、特性、进化链，
   * 使用 Promise.all 并行请求提升性能。
   *
   * @param {number|string} id - 宝可梦 ID
   */
  async function fetchPokemonDetail(id) {
    reset()
    loading.value = true

    try {
      pokemon.value = normalizePokemonDetail(await pokemonApi.getDetail(id))
      await Promise.all([fetchMoves(id), fetchAbilities(id), fetchEvolutions(id)])
    } catch (err) {
      error.value = err?.message || '获取详情失败'
      console.error('获取宝可梦详情失败:', err)
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    pokemon,
    moves,
    abilities,
    evolutions,
    loading,
    error,
    // 操作方法
    fetchPokemonDetail,
    fetchMoves,
    fetchAbilities,
    fetchEvolutions,
    reset
  }
})
