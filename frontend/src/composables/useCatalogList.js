import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'

export function useCatalogList(options) {
  const { fetchFn, favoritesKey, pageSize = 36, debounceMs = 300 } = options

  const loadMoreTrigger = ref(null)
  let observer = null
  let searchTimer = null

  // items = 全量数据（视图层用于筛选/排序）
  // displayItems = 当前页显示的数据切片
  const items = ref([])
  const displayCount = ref(pageSize)
  const currentPage = ref(0)
  const total = ref(0)

  const keyword = ref('')
  const favorites = ref(new Set())
  const isShowFavorites = ref(false)
  const viewMode = ref('grid')
  const loading = ref(false)
  const loadingMore = ref(false)

  const displayItems = computed(() => items.value.slice(0, displayCount.value))
  const hasMore = computed(() => displayCount.value < items.value.length)
  const loadedCount = computed(() => items.value.length)

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

  function toggleViewMode() {
    viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid'
  }

  async function fetchItems(isLoadMore = false, force = false) {
    if (!force && (loading.value || loadingMore.value)) return

    if (isLoadMore) {
      displayCount.value += pageSize
      return
    }

    loading.value = true
    items.value = []

    try {
      const result = await fetchFn({
        current: 1,
        size: 10000,
        keyword: keyword.value || undefined
      })

      if (result.code === 200) {
        const records = result.data.records || result.data || []
        items.value = records
        total.value = result.data.total || records.length
        displayCount.value = pageSize
        // 先让 watch 执行完（如各视图的 applyFilters 等），再关闭 loading
        await nextTick()
      }
    } catch (error) {
      console.error('获取列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  function handleSearchInput() {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => handleSearch(), debounceMs)
  }

  function handleSearch() {
    items.value = []
    fetchItems(false)
  }

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

  // 在 setup 阶段提前触发首次加载，避免首次渲染显示空状态
  loadFavorites()
  fetchItems(false, true).then(() => {
    nextTick(() => setupObserver())
  })

  onMounted(() => {})

  onUnmounted(() => {
    if (observer) observer.disconnect()
    if (searchTimer) clearTimeout(searchTimer)
  })

  watch(() => items.value.length, () => {
    nextTick(() => {
      if (loadMoreTrigger.value && observer) {
        observer.disconnect()
        observer.observe(loadMoreTrigger.value)
      }
    })
  })

  return {
    items,
    displayItems,
    total,
    currentPage,
    keyword,
    handleSearchInput,
    handleSearch,
    favorites,
    toggleFavorite,
    toggleFavorites,
    isShowFavorites,
    viewMode,
    toggleViewMode,
    loading,
    loadingMore,
    hasMore,
    loadedCount,
    displayCount,
    displayItems,
    loadMoreTrigger,
    fetchItems
  }
}
