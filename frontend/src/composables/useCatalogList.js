/**
 * ============================================================
 * 通用列表页组合式函数 / Catalog List Composable
 * ============================================================
 *
 * 封装 AbilityList / MoveList / ItemList 的通用逻辑：
 * - 搜索防抖 / Search debounce
 * - 收藏管理（localStorage 持久化）/ Favorites (localStorage)
 * - 分页加载 / Pagination
 * - IntersectionObserver 自动加载更多 / Auto load-more
 * - 视图模式切换 / View mode toggle
 * - 生命周期清理 / Lifecycle cleanup
 *
 * ## 使用方式 / Usage
 *
 * ```js
 * const {
 *   keyword, handleSearchInput, handleSearch,
 *   favorites, toggleFavorite, isShowFavorites, toggleFavorites,
 *   viewMode, toggleViewMode,
 *   items, loading, loadingMore, hasMore, total, loadedCount,
 *   loadMoreTrigger, fetchItems
 * } = useCatalogList({
 *   fetchFn: abilityApi.getList,
 *   favoritesKey: 'ability-favorites',
 *   pageSize: 36
 * })
 * ```
 *
 * @module composables/useCatalogList
 */

import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'

/**
 * @typedef {Object} CatalogListOptions
 * @property {Function} fetchFn - API 获取函数，接收 { current, size, keyword }
 * @property {string} favoritesKey - localStorage 收藏键名
 * @property {number} [pageSize=36] - 每页数量
 * @property {number} [debounceMs=300] - 搜索防抖延迟 (ms)
 */

/**
 * 通用列表页组合式函数
 * @param {CatalogListOptions} options
 */
export function useCatalogList(options) {
  const { fetchFn, favoritesKey, pageSize = 36, debounceMs = 300 } = options

  // ---- DOM References ----
  const loadMoreTrigger = ref(null)
  let observer = null
  let searchTimer = null

  // ---- Data ----
  const items = ref([])
  const currentPage = ref(0)
  const total = ref(0)

  // ---- Search ----
  const keyword = ref('')

  // ---- Favorites ----
  const favorites = ref(new Set())
  const isShowFavorites = ref(false)

  // ---- View Mode ----
  const viewMode = ref('grid')

  // ---- UI State ----
  const loading = ref(false)
  const loadingMore = ref(false)

  // ---- Computed ----
  const totalPages = computed(() => Math.ceil(total.value / pageSize))
  const hasMore = computed(() => currentPage.value < totalPages.value)
  const loadedCount = computed(() => items.value.length)

  // ============================================================
  // 收藏管理
  // ============================================================

  function loadFavorites() {
    try {
      const saved = localStorage.getItem(`pokemon-factory-${favoritesKey}`)
      if (saved) favorites.value = new Set(JSON.parse(saved))
    } catch { /* ignore */ }
  }

  function saveFavorites() {
    localStorage.setItem(
      `pokemon-factory-${favoritesKey}`,
      JSON.stringify([...favorites.value])
    )
  }

  function toggleFavorite(item) {
    if (favorites.value.has(item.id)) {
      favorites.value.delete(item.id)
    } else {
      favorites.value.add(item.id)
    }
    saveFavorites()
  }

  function toggleFavorites() {
    isShowFavorites.value = !isShowFavorites.value
  }

  // ============================================================
  // 视图模式
  // ============================================================

  function toggleViewMode() {
    viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid'
  }

  // ============================================================
  // 数据加载
  // ============================================================

  async function fetchItems(isLoadMore = false) {
    if (loading.value || loadingMore.value) return
    if (isLoadMore && !hasMore.value) return

    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
      currentPage.value = 0
      items.value = []
    }

    try {
      const nextPage = currentPage.value + 1
      const result = await fetchFn({
        current: nextPage,
        size: pageSize,
        keyword: keyword.value || undefined
      })

      if (result.code === 200) {
        const records = result.data.records || result.data || []
        items.value = isLoadMore ? [...items.value, ...records] : records
        total.value = result.data.total || records.length
        currentPage.value = nextPage
      }
    } catch (error) {
      console.error('获取列表失败:', error)
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  // ============================================================
  // 搜索防抖
  // ============================================================

  function handleSearchInput() {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => handleSearch(), debounceMs)
  }

  function handleSearch() {
    // 重置并重新加载
    currentPage.value = 0
    items.value = []
    fetchItems(false)
  }

  // ============================================================
  // IntersectionObserver
  // ============================================================

  function setupObserver() {
    if (observer) observer.disconnect()
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
            fetchItems(true)
          }
        })
      },
      { rootMargin: '200px', threshold: 0 }
    )
    if (loadMoreTrigger.value) {
      observer.observe(loadMoreTrigger.value)
    }
  }

  // ============================================================
  // 生命周期（自动注册 onMounted / onUnmounted）
  // ============================================================

  onMounted(() => {
    loadFavorites()
    fetchItems(false)
    nextTick(() => setupObserver())
  })

  onUnmounted(() => {
    if (observer) observer.disconnect()
    if (searchTimer) clearTimeout(searchTimer)
  })

  // 当 items 变化时重新连接 Observer
  watch(() => items.value.length, () => {
    nextTick(() => {
      if (loadMoreTrigger.value && observer) {
        observer.disconnect()
        observer.observe(loadMoreTrigger.value)
      }
    })
  })

  return {
    // Data
    items,
    total,
    currentPage,
    // Search
    keyword,
    handleSearchInput,
    handleSearch,
    // Favorites
    favorites,
    toggleFavorite,
    toggleFavorites,
    isShowFavorites,
    // View
    viewMode,
    toggleViewMode,
    // UI
    loading,
    loadingMore,
    hasMore,
    loadedCount,
    // DOM
    loadMoreTrigger,
    // Actions
    fetchItems
  }
}
