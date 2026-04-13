import { ref } from 'vue'
import { pokemonApi } from '../services/api'
import { normalizePokemonCollection, normalizePokemonDetail } from '../services/contracts/pokemonContract'

/**
 * 宝可梦数据获取组合式函数。
 *
 * 这里只消费后端真实接口，不再在请求失败时注入示例数据，
 * 避免页面把“接口异常”误展示为“正常但内容看起来不真实”的状态。
 */
export function usePokemonData() {
  const pokemon = ref(null)
  const moves = ref([])
  const abilities = ref([])
  const evolutions = ref([])
  const loading = ref(false)
  const error = ref(null)

  const reset = () => {
    pokemon.value = null
    moves.value = []
    abilities.value = []
    evolutions.value = []
    loading.value = false
    error.value = null
  }

  const fetchMoves = async (id) => {
    try {
      moves.value = normalizePokemonCollection(await pokemonApi.getMoves(id), [])
    } catch (err) {
      moves.value = []
      console.error('获取技能失败:', err)
    }
  }

  const fetchAbilities = async (id) => {
    try {
      abilities.value = normalizePokemonCollection(await pokemonApi.getAbilities(id), [])
    } catch (err) {
      abilities.value = []
      console.error('获取特性失败:', err)
    }
  }

  const fetchEvolutions = async (id) => {
    try {
      evolutions.value = normalizePokemonCollection(await pokemonApi.getEvolutionChain(id), [])
    } catch (err) {
      evolutions.value = []
      console.error('获取进化链失败:', err)
    }
  }

  const fetchPokemonDetail = async (id) => {
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
    pokemon,
    moves,
    abilities,
    evolutions,
    loading,
    error,
    fetchPokemonDetail,
    reset
  }
}