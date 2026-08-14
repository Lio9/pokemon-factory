<template>
  <div
    ref="listContainer"
    class="pokemon-list"
  >
    <!-- 页面头部 -->
    <CatalogPageHeader
      icon="📖"
      :title="tr('宝可梦图鉴', 'Pokemon Dex')"
      :subtitle="tr('浏览全部宝可梦，按属性、世代与传说分类检索，收藏心仪的宝可梦。', 'Browse every Pokemon, filter by type, generation, and rarity, and favorite your picks.')"
      :badge="tr('全国图鉴', 'National Dex')"
      color="#DC2626"
      color-light="#F87171"
      class="mb-6"
    >
      <template #actions>
        <div class="rounded-2xl bg-white/15 px-4 py-2 text-center backdrop-blur-sm">
          <div class="text-xl font-black text-white">
            {{ allRecords.length }}
          </div>
          <div class="text-[10px] font-bold uppercase tracking-wider text-white/70">
            {{ tr('宝可梦总数', 'Total Pokemon') }}
          </div>
        </div>
      </template>
    </CatalogPageHeader>

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
      :pokemons="displayPokemons"
      :view-mode="viewMode"
      :loading="loading"
      :loading-more="loadingMore"
      :has-more="hasMore"
      :total="allRecords.length"
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
import CatalogPageHeader from '../components/CatalogPageHeader.vue'

const listContainer = ref(null)
const cardGridRef = ref(null)

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
const favorites = ref([])

// 客户端分页：allRecords 存后端返回的全部数据，pokemons 只放当前页要显示的
const pageSize = ref(24)
const displayCount = ref(pageSize.value)
const allRecords = ref([])

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

const hasMore = computed(() => displayCount.value < allRecords.value.length)
const displayPokemons = computed(() => allRecords.value.slice(0, displayCount.value))

// ---- Favorite helpers ----
const loadFavorites = () => {
  try {
    const saved = localStorage.getItem('pokemon-favorites')
    if (saved) favorites.value = JSON.parse(saved)
  } catch { /* ignore */ }
}

const saveFavorites = () => {
  localStorage.setItem('pokemon-favorites', JSON.stringify(favorites.value))
}

const toggleFavorite = (pokemon) => {
  const id = pokemon.id
  const idx = favorites.value.indexOf(id)
  if (idx >= 0) {
    favorites.value.splice(idx, 1)
    ElMessage.success(`已取消收藏 ${pokemon.name}`)
  } else {
    favorites.value.push(id)
    ElMessage.success(`已收藏 ${pokemon.name}`)
  }
  saveFavorites()
}

const isFavorite = (id) => favorites.value.includes(id)

// ---- Filters ----
const toggleQuickFilter = (filterKey) => {
  const index = activeQuickFilters.value.indexOf(filterKey)
  if (index > -1) {
    activeQuickFilters.value.splice(index, 1)
  } else {
    activeQuickFilters.value.push(filterKey)
  }
  applyFilters()
}

const resetFilters = () => {
  searchKeyword.value = ''
  selectedType.value = null
  selectedGeneration.value = null
  activeQuickFilters.value = []
  applyFilters()
}

const applyFilters = () => {
  // 从 allRecords 中筛选，然后重置显示
  let filtered = allRecords.value

  // 类型筛选
  if (selectedType.value) {
    filtered = filtered.filter(p => p.types?.some?.(t => t.id === selectedType.value))
  }

  // 传说/神话/幼崽
  if (activeQuickFilters.value.length > 0) {
    filtered = filtered.filter(p =>
      activeQuickFilters.value.every(f => {
        if (f === 'legendary') return p.isLegendary
        if (f === 'mythical') return p.isMythical
        if (f === 'baby') return p.isBaby
        return true
      })
    )
  }

  // 搜索关键字
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    filtered = filtered.filter(p =>
      p.name?.toLowerCase().includes(kw) ||
      p.nameEn?.toLowerCase().includes(kw)
    )
  }

  // 排序
  filtered = [...filtered]
  switch (sortBy.value) {
    case 'name':
      filtered.sort((a, b) => a.name?.localeCompare(b.name) || 0)
      break
    case 'attack':
      filtered.sort((a, b) => (b.formStats?.attack || 0) - (a.formStats?.attack || 0))
      break
    case 'speed':
      filtered.sort((a, b) => (b.formStats?.speed || 0) - (a.formStats?.speed || 0))
      break
    case 'id':
    default:
      filtered.sort((a, b) => a.id - b.id)
  }

  pokemons.value = filtered
  allRecords.value = filtered
  displayCount.value = pageSize.value
}

// ---- Sort ----
watch(() => sortBy.value, () => applyFilters())

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

const fetchPokemons = async (isLoadMore = false) => {
  if (loading.value || loadingMore.value) return

  if (isLoadMore) {
    displayCount.value += pageSize.value
    return
  }

  loading.value = true
  pokemons.value = []
  allRecords.value = []

  try {
    const result = await pokemonApi.getList({
      current: 1,
      size: 10000,
      typeId: selectedType.value,
      generationId: selectedGeneration.value,
      keyword: searchKeyword.value || undefined
    })

    if (result.code === 200) {
      let records = result.data.records || []
      const processed = processPokemonData(records)
      allRecords.value = processed
      pokemons.value = processed
      displayCount.value = pageSize.value
    } else {
      ElMessage.error(result.message || '获取数据失败')
    }
  } catch (error) {
    console.error('获取宝可梦列表失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
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

// 在 setup 阶段发起首次数据加载，让首次渲染直接显示骨架屏
loading.value = true
loadFavorites()
fetchTypes()

// 直接发起首次加载（不经过 fetchPokemons 的 loading 守卫）
;(async () => {
  perfMonitor.recordPageLoad('PokemonList')
  dataCache.clearType('pokemon')
  try {
    const result = await pokemonApi.getList({
      current: 1, size: 10000,
      typeId: null, generationId: null
    })
    if (result.code === 200) {
      const records = result.data.records || []
      allRecords.value = processPokemonData(records)
      pokemons.value = allRecords.value
      displayCount.value = pageSize.value
    }
  } catch (e) {
    console.error('初始加载失败:', e)
  }
  loading.value = false
  nextTick(() => setupObserver())
})()

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })

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
  padding-bottom: 2rem;
}

@media (max-width: 768px) {
  .pokemon-list {
    padding-bottom: 1rem;
  }
}

.overflow-y-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
