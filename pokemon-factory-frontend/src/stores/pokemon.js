import { defineStore } from 'pinia'
import { ref } from 'vue'
import { pokemonApi } from '../services/api'
import { normalizePokemonCollection, normalizePokemonDetail } from '../services/contracts/pokemonContract'

export const usePokemonStore = defineStore('pokemon', () => {
  // State
  const pokemon = ref(null)
  const moves = ref([])
  const abilities = ref([])
  const evolutions = ref([])
  const loading = ref(false)
  const error = ref(null)

  // Actions
  function reset() {
    pokemon.value = null
    moves.value = []
    abilities.value = []
    evolutions.value = []
    loading.value = false
    error.value = null
  }

  async function fetchMoves(id) {
    try {
      moves.value = normalizePokemonCollection(await pokemonApi.getMoves(id), [])
    } catch (err) {
      moves.value = []
      console.error('获取技能失败:', err)
    }
  }

  async function fetchAbilities(id) {
    try {
      abilities.value = normalizePokemonCollection(await pokemonApi.getAbilities(id), [])
    } catch (err) {
      abilities.value = []
      console.error('获取特性失败:', err)
    }
  }

  async function fetchEvolutions(id) {
    try {
      evolutions.value = normalizePokemonCollection(await pokemonApi.getEvolutionChain(id), [])
    } catch (err) {
      evolutions.value = []
      console.error('获取进化链失败:', err)
    }
  }

  async function fetchPokemonDetail(id) {
    reset()
    loading.value = true

    try {
      pokemon.value = normalizePokemonDetail(await pokemonApi.getDetail(id))
      await Promise.all([fetchMoves(id), fetchAbilities(id), fetchEvolutions(id)])
    } catch (err) {
      error.value = err?.message || '获取详情失败'
      console.error('获取宝可梦详情失败:', err)
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    pokemon,
    moves,
    abilities,
    evolutions,
    loading,
    error,
    // Actions
    fetchPokemonDetail,
    fetchMoves,
    fetchAbilities,
    fetchEvolutions,
    reset
  }
})
