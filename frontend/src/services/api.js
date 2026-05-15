/**
 * 统一 API 导出模块
 *
 * 本模块作为 API 服务的统一出口，整合所有后端 API 模块。
 * 提供一致的接口供前端组件和服务使用。
 *
 * 使用方式：
 * ```javascript
 * import api from '@/services/api'
 * // 或者解构导入
 * import { pokemonApi, battleApi } from '@/services/api'
 * ```
 *
 * @module services/api
 */

// 导入各模块 API
import { pokemonApi } from './modules/pokemonApi'
import { typeApi, abilityApi, moveApi, itemApi, importApi, damageApi } from './modules/catalogApi'
import { userApi } from './modules/userApi'
import { battleApi } from './modules/battleApi'
import { sprites } from './sprites'

/**
 * API 模块命名空间导出（解构方式使用）
 * @namespace api
 */
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

/**
 * API 模块默认导出（点号方式使用）
 *
 * 使用示例：
 * ```javascript
 * import api from '@/services/api'
 * api.pokemon.getList()
 * api.battle.start()
 * ```
 */
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
