/*
 * env 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端配置文件。
 * 核心职责：负责环境变量、运行参数或构建期配置映射。
 * 阅读建议：建议关注不同环境下的取值来源。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

const DEFAULT_ENV = Object.freeze({
  apiBase: '/api/pokedex',
  damageApiBase: '/api/damage',
  spritesBase: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'
})

function normalizeBaseUrl(value, fallback) {
  const normalized = value && String(value).trim()
  return normalized ? normalized.replace(/\/$/, '') : fallback
}

export const appEnv = Object.freeze({
  apiBase: normalizeBaseUrl(import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_BASE_URL, DEFAULT_ENV.apiBase),
  damageApiBase: normalizeBaseUrl(import.meta.env.VITE_DAMAGE_API_BASE, DEFAULT_ENV.damageApiBase),
  spritesBase: normalizeBaseUrl(import.meta.env.VITE_SPRITES_BASE, DEFAULT_ENV.spritesBase)
})

export { normalizeBaseUrl, DEFAULT_ENV }
