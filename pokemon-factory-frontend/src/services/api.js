/*
 * api 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端基础服务文件。
 * 核心职责：负责请求、缓存、会话或资源地址等基础能力封装。
 * 阅读建议：建议关注对上层暴露的统一调用入口。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { pokemonApi } from './modules/pokemonApi'
import { typeApi, abilityApi, moveApi, itemApi, importApi, damageApi } from './modules/catalogApi'
import { userApi } from './modules/userApi'
import { battleApi } from './modules/battleApi'
import { sprites } from './sprites'

export {
  pokemonApi,
  typeApi,
  abilityApi,
  moveApi,
  itemApi,
  importApi,
  damageApi,
  userApi,
  battleApi,
  sprites
}

export default {
  pokemon: pokemonApi,
  types: typeApi,
  abilities: abilityApi,
  moves: moveApi,
  items: itemApi,
  import: importApi,
  damage: damageApi,
  user: userApi,
  battle: battleApi,
  sprites
}
