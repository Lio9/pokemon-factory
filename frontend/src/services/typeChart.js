/**
 * 属性相克表服务
 *
 * 优先从后端 API 获取权威数据，启动时先使用内嵌的静态表作为初始值，
 * 异步请求完成后自动替换为服务器数据。
 *
 * @module services/typeChart
 */

import { dataCache } from './cache'

// 第1-9世代 静态属性相克表（初始 fallback）
const STATIC_TYPE_EFFECTIVENESS = {
  1: { 6: 50, 8: 0, 9: 50 },
  2: { 1: 200, 3: 50, 4: 50, 6: 200, 7: 50, 8: 0, 9: 200, 15: 200, 17: 200, 18: 50 },
  3: { 2: 200, 6: 50, 7: 200, 9: 50, 12: 200, 13: 50 },
  4: { 4: 50, 5: 50, 6: 50, 8: 50, 12: 200, 18: 200 },
  5: { 3: 0, 4: 200, 6: 200, 9: 200, 10: 200, 12: 50, 13: 200 },
  6: { 2: 50, 3: 200, 7: 200, 9: 50, 10: 200, 15: 200 },
  7: { 2: 50, 3: 50, 4: 50, 8: 50, 9: 50, 10: 50, 12: 200, 14: 200, 17: 200, 18: 50 },
  8: { 1: 0, 8: 200, 14: 200, 17: 50 },
  9: { 6: 200, 9: 50, 10: 50, 11: 50, 18: 200 },
  10: { 6: 50, 7: 200, 9: 200, 10: 50, 11: 50, 12: 200, 15: 200, 16: 50 },
  11: { 5: 200, 6: 200, 10: 200, 11: 50, 12: 50, 16: 50 },
  12: { 3: 50, 4: 50, 5: 200, 6: 200, 7: 50, 9: 50, 10: 50, 11: 200, 12: 50, 16: 50 },
  13: { 3: 200, 5: 0, 11: 200, 12: 50, 13: 50, 16: 50 },
  14: { 2: 200, 4: 200, 9: 50, 14: 50, 17: 0 },
  15: { 3: 200, 5: 200, 9: 50, 10: 50, 11: 50, 12: 200, 15: 50, 16: 200 },
  16: { 9: 50, 16: 200, 18: 0 },
  17: { 2: 50, 8: 200, 14: 200, 17: 50, 18: 50 },
  18: { 2: 200, 4: 50, 9: 50, 10: 50, 16: 200, 17: 200 }
}

/** @type {Object} 当前活跃的相克表（初始为静态表，可由服务器数据替换） */
let typeEffectiveness = STATIC_TYPE_EFFECTIVENESS

/**
 * 获取当前活跃的相克表
 * @returns {Object}
 */
export function getTypeEffectiveness() {
  return typeEffectiveness
}

/**
 * 从后端 API 加载权威相克表，成功后替换静态表
 * 若请求失败则静默回退到静态表
 */
export async function loadTypeEffectiveness() {
  try {
    const serverData = await dataCache.getOrFetch(
      'type:chart',
      {},
      async () => {
        const response = await fetch('/api/damage/type-efficacy')
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        const { data } = await response.json()
        return normalizeServerData(data)
      },
      'long' // 30 分钟缓存
    )

    if (serverData) {
      typeEffectiveness = serverData
    }
  } catch {
    // 服务器不可用时静默使用静态表
  }
}

/**
 * 将后端返回的相克数据归一化为前端查找表格式
 * 支持两种格式：
 *   1. 数组 [{ damage_type_id, target_type_id, damage_factor }]
 *   2. 嵌套 Map { damageTypeId: { targetTypeId: factor, ... }, ... }
 * @param {Array|Object} data
 * @returns {Object} { moveTypeId: { targetTypeId: factor, ... }, ... }
 */
function normalizeServerData(data) {
  if (data == null) return null

  // 格式2: 嵌套 Map { "1": { "6": 50, "8": 0 }, ... }
  if (!Array.isArray(data) && typeof data === 'object') {
    const lookup = {}
    for (const [moveTypeStr, targetMap] of Object.entries(data)) {
      const moveType = Number(moveTypeStr)
      lookup[moveType] = {}
      for (const [targetTypeStr, factor] of Object.entries(targetMap)) {
        lookup[moveType][Number(targetTypeStr)] = factor
      }
    }
    return Object.keys(lookup).length > 0 ? lookup : null
  }

  // 格式1: 数组 [{ damage_type_id, target_type_id, damage_factor }]
  if (!Array.isArray(data)) return null

  const lookup = {}
  for (const entry of data) {
    const moveType = entry.damage_type_id ?? entry.damageTypeId
    const targetType = entry.target_type_id ?? entry.targetTypeId
    const factor = entry.damage_factor ?? entry.damageFactor
    if (moveType == null || targetType == null || factor == null) continue

    if (!lookup[moveType]) lookup[moveType] = {}
    lookup[moveType][targetType] = factor
  }

  return Object.keys(lookup).length > 0 ? lookup : null
}

/**
 * 解析类型 ID（兼容 snake_case / camelCase）
 * @param {Object} type
 * @returns {number}
 */
export function resolveTypeId(type) {
  return Number(type?.type_id ?? type?.typeId ?? 0)
}
