import { createRouter, createWebHistory } from 'vue-router'
import PokemonList from '../components/PokemonList.vue'
import PokemonDetail from '../components/PokemonDetail.vue'
import MoveList from '../components/MoveList.vue'
import AbilityList from '../components/AbilityList.vue'
import ItemList from '../components/ItemList.vue'

const routes = [
  { path: '/', redirect: '/pokemon' },
  { path: '/pokemon', name: 'PokemonList', component: PokemonList },
  { path: '/pokemon/:id', name: 'PokemonDetail', component: PokemonDetail, props: true },
  { path: '/moves', name: 'MoveList', component: MoveList },
  { path: '/abilities', name: 'AbilityList', component: AbilityList },
  { path: '/items', name: 'ItemList', component: ItemList }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
