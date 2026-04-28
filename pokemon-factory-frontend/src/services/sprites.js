/*
 * sprites 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端基础服务文件。
 * 核心职责：负责请求、缓存、会话或资源地址等基础能力封装。
 * 阅读建议：建议关注对上层暴露的统一调用入口。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { SPRITES_BASE } from './httpClient'

const REMOTE_FALLBACK_BASE = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites'

function buildPrimary(path) {
  return `${SPRITES_BASE}${path}`
}

function buildFallback(path) {
  return `${REMOTE_FALLBACK_BASE}${path}`
}

export const sprites = {
  pokemon: (id) => buildPrimary(`/pokemon/${id}.png`),
  official: (id) => buildPrimary(`/pokemon/other/official-artwork/${id}.png`),
  type: (id) => buildPrimary(`/types/${id}.png`),
  item: (name) => buildPrimary(`/items/${name}.png`),
  default: buildPrimary('/pokemon/0.png'),
  fallbackPokemon: (id) => buildFallback(`/pokemon/${id}.png`),
  fallbackOfficial: (id) => buildFallback(`/pokemon/other/official-artwork/${id}.png`),
  fallbackItem: (name) => buildFallback(`/items/${name}.png`),
  fallbackDefault: buildFallback('/pokemon/0.png')
}