/**
 * ============================================================
 * 精灵图资源管理 / Sprite Resource Manager
 * ============================================================
 *
 * ## 图片加载策略 / Image Loading Strategy
 *
 *   优先级链 / Priority Chain:
 *   1. 本地: /api/pokedex/images/pokemon/{id}.png
 *      (由 PokeDexImageConfig 从 data/image/ 提供)
 *   2. 远程备用: https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{id}.png
 *      (PokeAPI GitHub 原始资源)
 *   3. 默认: /api/pokedex/images/Unown_QU.png
 *      (data/image/Unown_QU.png，已由用户提供)
 *
 * ## 使用方式 / Usage
 *
 *   <img :src="sprites.pokemon(id)" @error="handleImageError">
 *
 *   handleImageError 中实现三级回退：
 *   - 当前为本地URL → 切换为远程备用
 *   - 当前为远程备用 → 切换为默认图片
 *   - 当前为默认图片 → 保持（不再重试）
 *
 * @module services/sprites
 */

// 本地图片基础路径（后端 PokeDexImageConfig 映射）
// Local image base path (served by PokeDexImageConfig)
const LOCAL_BASE = "/api/pokedex/images"

// 远程备用基础路径（PokeAPI GitHub）
// Remote fallback base path (PokeAPI GitHub)
const REMOTE_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites"

/**
 * 构建资源路径 / Build resource path
 * @param {string} base - 基础路径 / Base URL
 * @param {string} path - 资源路径 / Resource path
 * @returns {string} 完整 URL / Full URL
 */
function url(base, path) {
  return base + path
}

/**
 * 精灵图资源服务 / Sprite Resource Service
 *
 * 提供本地优先、远程备用的图片 URL 生成。
 * Provides local-first, remote-fallback image URL generation.
 *
 * @namespace sprites
 */
export const sprites = {
  /** 宝可梦正面图（本地）/ Pokemon front sprite (local) */
  pokemon: (id) => url(LOCAL_BASE, "/pokemon/" + id + ".png"),

  /** 宝可梦正面图（远程备用）/ Pokemon front sprite (remote fallback) */
  fallbackPokemon: (id) => url(REMOTE_BASE, "/pokemon/" + id + ".png"),

  /** 官方艺术图（本地）/ Official artwork (local) */
  official: (id) => url(LOCAL_BASE, "/pokemon/other/official-artwork/" + id + ".png"),

  /** 属性图标（本地）/ Type icon (local) */
  type: (id) => url(LOCAL_BASE, "/types/" + id + ".png"),

  /** 道具图标（本地）/ Item icon (local) */
  item: (name) => url(LOCAL_BASE, "/items/" + name + ".png"),

  /** 默认图片（Unown_QU）/ Default image */
  default: "/images/Unown_QU.png",

  // ========== 远程备用 / Remote Fallbacks ==========
  fallbackOfficial: (id) => url(REMOTE_BASE, "/pokemon/other/official-artwork/" + id + ".png"),
  fallbackItem: (name) => url(REMOTE_BASE, "/items/" + name + ".png"),
}
