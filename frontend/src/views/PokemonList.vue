<template>
  <div
    ref="listContainer"
    class="pokemon-list"
  >
    <PokemonSearchFilters
      :search-keyword="searchKeyword"
      :selected-type="selectedType"
      :selected-generation="selectedGeneration"
      :sort-by="sortBy"
      :view-mode="viewMode"
      :types="types"
      :generations="generations"
      :quick-filters="quickFilters"
      :active-quick-filters="activeQuickFilters"
      @update:search-keyword="searchKeyword = $event"
      @update:selected-type="selectedType = $event"
      @update:selected-generation="selectedGeneration = $event"
      @update:sort-by="sortBy = $event"
      @update:view-mode="viewMode = $event"
      @search="handleSearch"
      @filter="handleFilter"
      @toggle-quick-filter="toggleQuickFilter"
    />

    <PokemonCardGrid
      ref="cardGridRef"
      :pokemons="pokemons"
      :view-mode="viewMode"
      :loading="loading"
      :loading-more="loadingMore"
      :has-more="hasMore"
      :total="total"
      :show-back-top="showBackTop"
      :is-favorite="isFavorite"
      @toggle-favorite="toggleFavorite"
      @image-load="handleImageLoad"
      @image-error="handleImageError"
      @scroll-to-top="scrollToTop"
      @reset-filters="resetFilters"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { pokemonApi, typeApi, sprites } from '../services/api'
import { dataCache } from '../services/cache'
import { registerShortcuts } from '../services/keyboard'
import { perfMonitor } from '../services/performance'
import PokemonSearchFilters from '../components/PokemonSearchFilters.vue'
import PokemonCardGrid from '../components/PokemonCardGrid.vue'

// DOM references
const listContainer = ref(null)
const cardGridRef = ref(null)

// Reactive state
const loading = ref(false)
const loadingMore = ref(false)
const pokemons = ref([])
const types = ref([])
const searchKeyword = ref('')
const selectedType = ref(null)
const selectedGeneration = ref(null)
const sortBy = ref('id')
const viewMode = ref('grid')
const activeQuickFilters = ref([])
const favorites = ref(new Set())

// Pagination
const currentPage = ref(0)
const pageSize = ref(24)
const total = ref(0)

// Scroll state
const showBackTop = ref(false)
let observer = null
let scrollThrottleTimer = null

const generations = [
  { id: 1, name: '第一世代' },
  { id: 2, name: '第二世代' },
  { id: 3, name: '第三世代' },
  { id: 4, name: '第四世代' },
  { id: 5, name: '第五世代' },
  { id: 6, name: '第六世代' },
  { id: 7, name: '第七世代' },
  { id: 8, name: '第八世代' },
  { id: 9, name: '第九世代' }
]

const quickFilters = [
  { key: 'legendary', label: '传说', icon: '⭐' },
  { key: 'mythical', label: '神话', icon: '◆' },
  { key: 'baby', label: '幼崽', icon: '👶' }
]

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const hasMore = computed(() => currentPage.value < totalPages.value)

// ---- Favorite helpers ----
const loadFavorites = () => {
  try {
    const saved = localStorage.getItem('pokemon-favorites')
    if (saved) favorites.value = new Set(JSON.parse(saved))
  } catch { /* ignore */ }
}

const saveFavorites = () => {
  localStorage.setItem('pokemon-favorites', JSON.stringify([...favorites.value]))
}

const toggleFavorite = (pokemon) => {
  const id = pokemon.id
  if (favorites.value.has(id)) {
    favorites.value.delete(id)
    ElMessage.success(`已取消收藏 ${pokemon.name}`)
  } else {
    favorites.value.add(id)
    ElMessage.success(`已收藏 ${pokemon.name}`)
  }
  saveFavorites()
}

const isFavorite = (id) => favorites.value.has(id)

// ---- Filters ----
const toggleQuickFilter = (filterKey) => {
  const index = activeQuickFilters.value.indexOf(filterKey)
  if (index > -1) {
    activeQuickFilters.value.splice(index, 1)
  } else {
    activeQuickFilters.value.push(filterKey)
  }
  handleFilter()
}

const resetFilters = () => {
  searchKeyword.value = ''
  selectedType.value = null
  selectedGeneration.value = null
  activeQuickFilters.value = []
  handleFilter()
}

// ---- Sort ----
const sortPokemons = (data) => {
  const sorted = [...data]
  switch (sortBy.value) {
    case 'name':
      sorted.sort((a, b) => a.name.localeCompare(b.name))
      break
    case 'attack':
      sorted.sort((a, b) => (b.formStats?.attack || 0) - (a.formStats?.attack || 0))
      break
    case 'speed':
      sorted.sort((a, b) => (b.formStats?.speed || 0) - (a.formStats?.speed || 0))
      break
    case 'id':
    default:
      sorted.sort((a, b) => a.id - b.id)
  }
  return sorted
}

// ---- Data fetching ----
const fetchTypes = async () => {
  try {
    const result = await dataCache.getOrFetch('catalog:types', {}, () => typeApi.getAll())
    if (result.code === 200) types.value = result.data
  } catch (error) {
    console.error('获取属性列表失败:', error)
  }
}

const processPokemonData = (data) => {
  return data.map(p => ({
    ...p,
    _imageUrl: p.spriteUrl || sprites.pokemon(p.id),
    _imageLoaded: false,
    formStats: p.forms?.[0]?.stats || {}
  }))
}

const applyQuickFilters = (data) => {
  if (activeQuickFilters.value.length === 0) return data
  return data.filter(pokemon =>
    activeQuickFilters.value.every(filter => {
      switch (filter) {
        case 'legendary': return pokemon.isLegendary
        case 'mythical': return pokemon.isMythical
        case 'baby': return pokemon.isBaby
        default: return true
      }
    })
  )
}

const fetchPokemons = async (isLoadMore = false) => {
  if (loading.value || loadingMore.value) return
  if (isLoadMore && !hasMore.value) return

  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
    currentPage.value = 0
    pokemons.value = []
  }

  try {
    const nextPage = currentPage.value + 1
    const params = {
      current: nextPage,
      size: pageSize.value,
      typeId: selectedType.value,
      generationId: selectedGeneration.value,
      keyword: searchKeyword.value || undefined
    }

    const result = await pokemonApi.getList(params)

    if (result.code === 200) {
      let records = result.data.records || []
      total.value = result.data.total || 0
      currentPage.value = nextPage

      records = applyQuickFilters(records)
      const processedData = processPokemonData(records)
      pokemons.value = [...pokemons.value, ...processedData]

      preloadNextPageData(nextPage + 1)
    } else {
      ElMessage.error(result.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取宝可梦列表失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const preloadNextPageData = async (page) => {
  if (page > totalPages.value) return
  const params = {
    current: page,
    size: pageSize.value,
    typeId: selectedType.value,
    generationId: selectedGeneration.value,
    keyword: searchKeyword.value || undefined
  }
  dataCache.getOrFetch('pokemon:list', params, () => pokemonApi.getList(params)).catch(() => {})
}

const handleImageLoad = (pokemon) => { pokemon._imageLoaded = true }
const handleImageError = (pokemon) => {
  pokemon._imageLoaded = true
  pokemon._imageUrl = sprites.default
}

const handleSearch = () => {
  dataCache.clearType('pokemon')
  fetchPokemons(false)
}

const handleFilter = () => {
  dataCache.clearType('pokemon')
  fetchPokemons(false)
}

const handleSort = () => {
  pokemons.value = sortPokemons(pokemons.value)
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ---- Scroll handler ----
const handleScroll = () => {
  if (scrollThrottleTimer) return
  scrollThrottleTimer = setTimeout(() => {
    showBackTop.value = window.scrollY > 300
    scrollThrottleTimer = null
  }, 100)
}

// ---- Intersection Observer for infinite scroll ----
const setupObserver = () => {
  if (observer) observer.disconnect()

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
          fetchPokemons(true)
        }
      })
    },
    { root: null, rootMargin: '300px 0px 500px 0px', threshold: 0.1 }
  )

  const trigger = cardGridRef.value?.loadMoreTrigger
  if (trigger) observer.observe(trigger)
}

// ---- Lifecycle ----
let cleanupShortcuts = null

onMounted(async () => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  loadFavorites()
  await fetchTypes()

  perfMonitor.recordPageLoad('PokemonList')
  await fetchPokemons(false)

  nextTick(() => setupObserver())

  cleanupShortcuts = registerShortcuts({
    '/': {
      handler: () => {
        const searchInput = document.querySelector('.search-input input')
        if (searchInput) {
          searchInput.focus()
          ElMessage.info('已聚焦搜索框')
        }
      },
      options: { preventDefault: true }
    },
    Escape: {
      handler: () => {
        if (searchKeyword.value) {
          searchKeyword.value = ''
          handleSearch()
          ElMessage.info('已清空搜索')
        }
      }
    },
    'Alt+Home': {
      handler: () => {
        resetFilters()
        ElMessage.info('已重置所有筛选')
      }
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (observer) observer.disconnect()
  if (scrollThrottleTimer) clearTimeout(scrollThrottleTimer)
  if (cleanupShortcuts) {
    cleanupShortcuts()
    cleanupShortcuts = null
  }
})

watch(() => sortBy.value, () => handleSort())

watch(() => pokemons.value.length, () => {
  nextTick(() => {
    const trigger = cardGridRef.value?.loadMoreTrigger
    if (trigger && observer) {
      observer.disconnect()
      observer.observe(trigger)
    }
  })
})
</script>

<style scoped>
.pokemon-list {
  scroll-behavior: smooth;
}

.pokemon-list::-webkit-scrollbar {
  width: 8px;
}

.pokemon-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.pokemon-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
}

.pokemon-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #2563eb, #7c3aed);
}
</style>
