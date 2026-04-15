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
