import { createRouter, createWebHistory } from 'vue-router'
import PokemonList from '../components/PokemonList.vue'
import PokemonDetail from '../components/PokemonDetail.vue'
import MoveList from '../components/MoveList.vue'
import AbilityList from '../components/AbilityList.vue'
import ItemList from '../components/ItemList.vue'
import ImportManager from '../components/ImportManager.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    redirect: '/pokemon'
  },
  {
    path: '/pokemon',
    name: 'Pokemon',
    component: PokemonList
  },
  {
    path: '/pokemon/:id',
    name: 'PokemonDetail',
    component: PokemonDetail,
    props: true
  },
  {
    path: '/moves',
    name: 'Moves',
    component: MoveList
  },
  {
    path: '/abilities',
    name: 'Abilities',
    component: AbilityList
  },
  {
    path: '/items',
    name: 'Items',
    component: ItemList
  },
  {
    path: '/admin/import',
    name: 'ImportManager',
    component: ImportManager,
    meta: {
      hidden: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router