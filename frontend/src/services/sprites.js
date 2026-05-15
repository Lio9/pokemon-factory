/**
 * 精灵图资源管理模块
 *
 * 本模块负责管理宝可梦相关图片资源的 URL 构建。
 * 支持主站资源（配置化）和 GitHub 备用源（硬编码）。
 * 主站资源加载失败时自动回退到备用源。
 *
 * @module services/sprites
 */

import { SPRITES_BASE } from './httpClient'

// GitHub 备用源（当主站不可用时使用）
const REMOTE_FALLBACK_BASE = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'

/**
 * 构建主站资源路径
 *
 * @param {string} path - 资源相对路径
 * @returns {string} 完整的主站资源 URL
 */
function buildPrimary(path) {
  return `${SPRITES_BASE}${path}`
}

/**
 * 构建备用资源路径
 *
 * @param {string} path - 资源相对路径
 * @returns {string} 完整的备用资源 URL
 */
function buildFallback(path) {
  return `${REMOTE_FALLBACK_BASE}${path}`
}

/**
 * 精灵图资源服务
 *
 * 提供各类宝可梦相关图片的 URL 生成函数。
 * 每个函数同时提供主站和备用源 URL。
 *
 * @namespace sprites
 */
export const sprites = {
  /**
   * 宝可梦正面/背面精灵图
   * @param {number|string} id - 宝可梦全国图鉴编号
   * @returns {string} 主站资源 URL
   */
  pokemon: (id) => buildPrimary(`/pokemon/${id}.png`),

  /**
   * 官方艺术作品图（大图）
   * @param {number|string} id - 宝可梦全国图鉴编号
   * @returns {string} 主站资源 URL
   */
  official: (id) => buildPrimary(`/pokemon/other/official-artwork/${id}.png`),

  /**
   * 属性类型图标
   * @param {number|string} id - 属性类型 ID
   * @returns {string} 主站资源 URL
   */
  type: (id) => buildPrimary(`/types/${id}.png`),

  /**
   * 道具图标
   * @param {string} name - 道具名称
   * @returns {string} 主站资源 URL
   */
  item: (name) => buildPrimary(`/items/${name}.png`),

  /**
   * 默认/占位精灵图
   * @returns {string} 主站资源 URL
   */
  default: buildPrimary('/pokemon/0.png'),

  // ========== 备用源（当主站加载失败时使用）==========

  /**
   * 宝可梦精灵图（备用源）
   * @param {number|string} id - 宝可梦全国图鉴编号
   * @returns {string} 备用资源 URL
   */
  fallbackPokemon: (id) => buildFallback(`/pokemon/${id}.png`),

  /**
   * 官方艺术作品图（备用源）
   * @param {number|string} id - 宝可梦全国图鉴编号
   * @returns {string} 备用资源 URL
   */
  fallbackOfficial: (id) => buildFallback(`/pokemon/other/official-artwork/${id}.png`),

  /**
   * 道具图标（备用源）
   * @param {string} name - 道具名称
   * @returns {string} 备用资源 URL
   */
  fallbackItem: (name) => buildFallback(`/items/${name}.png`),

  /**
   * 默认精灵图（备用源）
   * @returns {string} 备用资源 URL
   */
  fallbackDefault: buildFallback('/pokemon/0.png')
}
