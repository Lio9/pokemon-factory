/**
 * 环境配置模块
 *
 * 本模块负责管理前端应用的环境变量和配置参数。
 * 配置来源优先级：
 * 1. Vite 环境变量（VITE_*）
 * 2. 默认值（DEFAULT_ENV）
 *
 * @module config/env
 */

const DEFAULT_ENV = Object.freeze({
  apiBase: '/api/pokedex',
  damageApiBase: '/api/damage',
  spritesBase: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'
})

/**
 * 规范化 URL 基础路径
 * - 去除尾部斜杠
 * - 处理空值回退
 *
 * @param {string} value - 原始 URL 值
 * @param {string} fallback - 默认回退值
 * @returns {string} 规范化后的 URL
 */
function normalizeBaseUrl(value, fallback) {
  const normalized = value && String(value).trim()
  return normalized ? normalized.replace(/\/$/, '') : fallback
}

/**
 * 应用环境配置对象
 *
 * 包含所有 API 端点和资源路径的配置：
 * - apiBase: 图鉴服务 API 地址
 * - battleApiBase: 对战服务 API 地址
 * - damageApiBase: 伤害计算 API 地址
 * - spritesBase: 宝可梦精灵图资源基础路径
 *
 * @readonly
 * @constant {Object}
 */
export const appEnv = Object.freeze({
  apiBase: normalizeBaseUrl(import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_BASE_URL, DEFAULT_ENV.apiBase),
  battleApiBase: normalizeBaseUrl(import.meta.env.VITE_BATTLE_API_BASE, '/api'),
  damageApiBase: normalizeBaseUrl(import.meta.env.VITE_DAMAGE_API_BASE, DEFAULT_ENV.damageApiBase),
  spritesBase: normalizeBaseUrl(import.meta.env.VITE_SPRITES_BASE, DEFAULT_ENV.spritesBase)
})

export { normalizeBaseUrl, DEFAULT_ENV }
