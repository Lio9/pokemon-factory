/**
 * 搜索优化组合式函数
 * 
 * 集成防抖、缓存等优化策略
 * 
 * @module composables/useSearchOptimization
 */

import { ref, computed, watch } from 'vue'
import { debounce } from '@/utils/performance'
import dataCache from '@/services/cache'

export function useSearchOptimization(options = {}) {
  const {
    debounceMs = 300,
    cacheEnabled = true,
    cacheKeyPrefix = 'search',
    minSearchLength = 1,
    fetchFn
  } = options

  // 搜索状态
  const query = ref('')
  const results = ref([])
  const loading = ref(false)
  const error = ref(null)

  // 是否正在搜索
  const isSearching = computed(() => loading.value)

  // 是否应该搜索
  const shouldSearch = computed(() => {
    return query.value.length >= minSearchLength
  })

  // 缓存键生成
  const getCacheKey = (searchQuery) => {
    return `${cacheKeyPrefix}:${searchQuery.toLowerCase().trim()}`
  }

  // 搜索执行函数
  const executeSearch = async (searchQuery) => {
    if (!fetchFn || !shouldSearch.value) {
      results.value = []
      return
    }

    const cacheKey = getCacheKey(searchQuery)
    
    // 尝试从缓存获取
    if (cacheEnabled) {
      const cached = dataCache.get(cacheKey, {})
      if (cached) {
        results.value = cached
        return
      }
    }

    loading.value = true
    error.value = null

    try {
      const data = await fetchFn(searchQuery)
      results.value = data

      // 缓存结果
      if (cacheEnabled) {
        dataCache.set(cacheKey, {}, data)
      }
    } catch (err) {
      error.value = err
      results.value = []
    } finally {
      loading.value = false
    }
  }

  // 防抖搜索
  const debouncedSearch = debounce(executeSearch, debounceMs)

  // 监听查询变化
  watch(query, (newQuery) => {
    if (!newQuery || newQuery.length < minSearchLength) {
      results.value = []
      return
    }
    debouncedSearch(newQuery)
  })

  // 强制刷新搜索（忽略缓存
  const forceRefresh = async () => {
    if (!query.value) return
    
    // 清除缓存
    const cacheKey = getCacheKey(query.value)
    dataCache.clearType(cacheKeyPrefix)
    
    await executeSearch(query.value)
  }

  // 清除搜索
  const clearSearch = () => {
    query.value = ''
    results.value = []
    error.value = null
    loading.value = false
  }

  return {
    // 状态
    query,
    results,
    loading,
    error,
    isSearching,
    
    // 方法
    executeSearch,
    forceRefresh,
    clearSearch
  }
}

/**
 * 批量搜索优化
 * 用于需要搜索多个数据源的场景
 */
export function useBatchSearchOptimization(options = {}) {
  const {
    sources = [],
    debounceMs = 300
  } = options

  const query = ref('')
  const loading = ref(false)
  const results = ref({})
  const error = ref(null)

  const executeBatchSearch = async (searchQuery) => {
    if (!searchQuery) {
      results.value = {}
      return
    }

    loading.value = true
    error.value = null

    try {
      // 并行搜索所有源
      const searchPromises = sources.map(async (source) => {
        const cacheKey = `batch-search:${source.name}:${searchQuery}`
        const cached = dataCache.get(cacheKey, {})
        
        if (cached) {
          return { source: source.name, data: cached }
        }

        const data = await source.fetchFn(searchQuery)
        dataCache.set(cacheKey, {}, data)
        return { source: source.name, data }
      })

      const searchResults = await Promise.all(searchPromises)
      
      // 合并结果
      results.value = searchResults.reduce((acc, { source, data }) => {
        acc[source] = data
        return acc
      }, {})
    } catch (err) {
      error.value = err
    } finally {
      loading.value = false
    }
  }

  const debouncedBatchSearch = debounce(executeBatchSearch, debounceMs)

  watch(query, (newQuery) => {
    debouncedBatchSearch(newQuery)
  })

  return {
    query,
    results,
    loading,
    error,
    executeBatchSearch
  }
}
