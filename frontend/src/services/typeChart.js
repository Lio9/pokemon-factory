/**
 * 属性相克表服务
 *
 * 优先从后端 API 获取权威数据，启动时先使用内嵌的静态表作为初始值，
 * 异步请求完成后自动替换为服务器数据。
 *
 * 同时提供类型色/类型名的单一数据源，避免各组件重复拷贝。
 *
 * @module services/typeChart
 */

import { dataCache } from './cache'

// ===== 类型元数据（单一数据源：PokeAPI/后端 type 表编号）=====
// 1=normal, 2=fighting, 3=flying, 4=poison, 5=ground, 6=rock,
// 7=bug, 8=ghost, 9=steel, 10=fire, 11=water, 12=grass,
// 13=electric, 14=psychic, 15=ice, 16=dragon, 17=dark, 18=fairy
export const TYPE_COLORS = {
  1: '#A8A77A', 2: '#C03028', 3: '#A890F0', 4: '#A040A0', 5: '#E0C068',
  6: '#B8A038', 7: '#A8B820', 8: '#705898', 9: '#B8B8D0', 10: '#F08030',
  11: '#6890F0', 12: '#78C850', 13: '#F8D030', 14: '#F85888', 15: '#98D8D8',
  16: '#7038F8', 17: '#705848', 18: '#EE99AC'
}

export const TYPE_NAMES_ZH = {
  1: '一般', 2: '格斗', 3: '飞行', 4: '毒', 5: '地面',
  6: '岩石', 7: '虫', 8: '幽灵', 9: '钢', 10: '火',
  11: '水', 12: '草', 13: '电', 14: '超能力', 15: '冰',
  16: '龙', 17: '恶', 18: '妖精'
}

export const TYPE_NAMES_EN = {
  1: 'Normal', 2: 'Fighting', 3: 'Flying', 4: 'Poison', 5: 'Ground',
  6: 'Rock', 7: 'Bug', 8: 'Ghost', 9: 'Steel', 10: 'Fire',
  11: 'Water', 12: 'Grass', 13: 'Electric', 14: 'Psychic', 15: 'Ice',
  16: 'Dragon', 17: 'Dark', 18: 'Fairy'
}

/** 类型色（按 PokeAPI 编号） */
export function typeColor(typeId) {
  return TYPE_COLORS[Number(typeId)] || '#777'
}

/** 类型中文名（按 PokeAPI 编号） */
export function typeNameZh(typeId) {
  return TYPE_NAMES_ZH[Number(typeId)] || '?'
}

/** 类型英文名（按 PokeAPI 编号） */
export function typeNameEn(typeId) {
  return TYPE_NAMES_EN[Number(typeId)] || '?'
}

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
