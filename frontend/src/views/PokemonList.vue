

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

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, ArrowUp, CircleCheck, ArrowDown } from '@element-plus/icons-vue'
import { pokemonApi, typeApi, sprites } from '../services/api.js'
import { dataCache } from '../services/cache.js'
import { registerShortcuts, COMMON_SHORTCUTS } from '../services/keyboard'
import { perfMonitor } from '../services/performance'
import PokemonSearchFilters from '../components/PokemonSearchFilters.vue'
import PokemonCardGrid from '../components/PokemonCardGrid.vue'

export default {
  name: 'PokemonList',
  components: { Loading, ArrowUp, CircleCheck, ArrowDown, PokemonSearchFilters, PokemonCardGrid },
  setup() {
    // DOM引用
    const listContainer = ref(null)
    const cardGridRef = ref(null)
    
    // 响应式数据
    const loading = ref(false)
    const loadingMore = ref(false)
    const pokemons = ref([])
    const types = ref([])
    const searchKeyword = ref('')
    const selectedType = ref(null)
    const selectedGeneration = ref(null)
    const sortBy = ref('id')
    const viewMode = ref('grid') // 'grid' or 'list'
    const activeQuickFilters = ref([])
    const favorites = ref(new Set())
    
    
    // 分页数据 - 使用较小页面大小实现无限滚动
    const currentPage = ref(0)
    const pageSize = ref(24)
    const total = ref(0)
    
    // 滚动状态
    const showBackTop = ref(false)
    let observer = null
    let scrollThrottleTimer = null
    
    // 世代列表
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

    // 快速筛选选项
    const quickFilters = [
      { key: 'legendary', label: '传说', icon: '⭐' },
      { key: 'mythical', label: '神话', icon: '◆' },
      { key: 'baby', label: '幼崽', icon: '👶' }
    ]
    
    // 计算属性
    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)

    // 从localStorage加载收藏
    const loadFavorites = () => {
      const saved = localStorage.getItem('pokemon-favorites')
      if (saved) {
        favorites.value = new Set(JSON.parse(saved))
      }
    }

    // 保存收藏到localStorage
    const saveFavorites = () => {
      localStorage.setItem('pokemon-favorites', JSON.stringify([...favorites.value]))
    }

    // 切换收藏状态
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

    // 检查是否已收藏
    const isFavorite = (id) => favorites.value.has(id)

    // 切换快速筛选
    const toggleQuickFilter = (filterKey) => {
      const index = activeQuickFilters.value.indexOf(filterKey)
      if (index > -1) {
        activeQuickFilters.value.splice(index, 1)
      } else {
        activeQuickFilters.value.push(filterKey)
      }
      handleFilter()
    }

    // 重置筛选
    const resetFilters = () => {
      searchKeyword.value = ''
      selectedType.value = null
      selectedGeneration.value = null
      activeQuickFilters.value = []
      handleFilter()
    }

    // 排序逻辑
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

    // 获取属性列表 - 使用缓存
    const fetchTypes = async () => {
      try {
        const result = await dataCache.getOrFetch('types', {}, async () => {
          return await typeApi.getAll()
        })
        if (result.code === 200) {
          types.value = result.data
        }
      } catch (error) {
        console.error('获取属性列表失败:', error)
      }
    }

    // 处理宝可梦数据 - 添加图片URL和排序信息
    const processPokemonData = (data) => {
      return data.map(p => ({
        ...p,
        _imageUrl: p.spriteUrl || sprites.pokemon(p.id),
        _imageLoaded: false,
        formStats: p.forms?.[0]?.stats || {}
      }))
    }

    // 应用快速筛选
    const applyQuickFilters = (data) => {
      if (activeQuickFilters.value.length === 0) return data
      
      return data.filter(pokemon => {
        return activeQuickFilters.value.every(filter => {
          switch (filter) {
            case 'legendary':
              return pokemon.isLegendary
            case 'mythical':
              return pokemon.isMythical
            case 'baby':
              return pokemon.isBaby
            default:
              return true
          }
        })
      })
    }

    // 获取宝可梦列表
    const fetchPokemons = async (isLoadMore = false) => {
      if (loading.value || loadingMore.value) return
      
      // 检查是否还有更多数据
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
        
        // 不使用缓存，确保获取最新数据
        const result = await pokemonApi.getList(params)
        
        if (result.code === 200) {
          let records = result.data.records || []
          total.value = result.data.total || 0
          currentPage.value = nextPage
          
          // 应用快速筛选
          records = applyQuickFilters(records)
          
          // 追加数据
          const processedData = processPokemonData(records)
          pokemons.value = [...pokemons.value, ...processedData]
          
          // 预加载下一页数据
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

    // 预加载下一页数据到缓存
    const preloadNextPageData = async (page) => {
      if (page > totalPages.value) return
      
      const params = {
        current: page,
        size: pageSize.value,
        typeId: selectedType.value,
        generationId: selectedGeneration.value,
        keyword: searchKeyword.value || undefined
      }
      
      dataCache.getOrFetch('pokemon', params, async () => {
        return await pokemonApi.getList(params)
      }).catch(() => {})
    }

    // 图片加载完成
    const handleImageLoad = (pokemon) => {
      pokemon._imageLoaded = true
    }

    // 图片加载失败处理
    const handleImageError = (pokemon) => {
      pokemon._imageLoaded = true
      pokemon._imageUrl = sprites.default
    }

    // 搜索
    const handleSearch = () => {
      dataCache.clearType('pokemon')
      fetchPokemons(false)
    }

    // 筛选
    const handleFilter = () => {
      dataCache.clearType('pokemon')
      fetchPokemons(false)
    }

    // 排序
    const handleSort = () => {
      pokemons.value = sortPokemons(pokemons.value)
    }

    // 返回顶部
    const scrollToTop = () => {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    // 监听滚动 - 使用节流
    const handleScroll = () => {
      if (scrollThrottleTimer) return
      
      scrollThrottleTimer = setTimeout(() => {
        showBackTop.value = window.scrollY > 300
        scrollThrottleTimer = null
      }, 100)
    }

    // 设置Intersection Observer监听加载更多
    const setupObserver = () => {
      if (observer) observer.disconnect()
      
      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            // 当触发器进入视口时加载更多
            if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
              fetchPokemons(true)
            }
          })
        },
        {
          root: null,
          rootMargin: '300px 0px 500px 0px', // 提前300px开始加载，底部缓冲500px
          threshold: 0.1 // 10%可见时触发
        }
      )
      
      const trigger = cardGridRef.value?.loadMoreTrigger
      if (trigger) {
        observer.observe(trigger)
      }
    }

    // 初始化
    onMounted(async () => {
      window.addEventListener('scroll', handleScroll, { passive: true })
      loadFavorites()
      await fetchTypes()
      
      // 记录页面加载性能
      perfMonitor.recordPageLoad('PokemonList')
      
      await fetchPokemons(false)
      
      nextTick(() => {
        setupObserver()
      })
      
      // 注册键盘快捷键
      const cleanupShortcuts = registerShortcuts({
        '/': {
          handler: () => {
            // 聚焦搜索框
            const searchInput = document.querySelector('.search-input input')
            if (searchInput) {
              searchInput.focus()
              ElMessage.info('已聚焦搜索框')
            }
          },
          options: { preventDefault: true }
        },
        'Escape': {
          handler: () => {
            // 清空搜索
            if (searchKeyword.value) {
              searchKeyword.value = ''
              handleSearch()
              ElMessage.info('已清空搜索')
            }
          }
        },
        'Alt+Home': {
          handler: () => {
            // 重置筛选
            resetFilters()
            ElMessage.info('已重置所有筛选')
          }
        }
      })
      
      // 保存清理函数以便组件卸载时调用
      window.__pokemonListCleanup = cleanupShortcuts
    })

    // 清理
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll)
      if (observer) observer.disconnect()
      if (scrollThrottleTimer) clearTimeout(scrollThrottleTimer)
      
      // 清理快捷键
      if (window.__pokemonListCleanup) {
        window.__pokemonListCleanup()
      }
    })

    // 监听排序变化
    watch(() => sortBy.value, () => {
      handleSort()
    })

    // 监听数据变化重新设置observer
    watch(() => pokemons.value.length, () => {
      nextTick(() => {
        const trigger = cardGridRef.value?.loadMoreTrigger
        if (trigger && observer) {
          observer.disconnect()
          observer.observe(trigger)
        }
      })
    })

    return {
      listContainer,
      cardGridRef,
      loading,
      loadingMore,
      pokemons,
      types,
      generations,
      searchKeyword,
      selectedType,
      selectedGeneration,
      sortBy,
      viewMode,
      activeQuickFilters,
      quickFilters,
      currentPage,
      pageSize,
      total,
      hasMore,
      showBackTop,
      handleImageLoad,
      handleImageError,
      handleSearch,
      handleFilter,
      handleSort,
      scrollToTop,
      toggleFavorite,
      isFavorite,
      toggleQuickFilter,
      resetFilters
    }
  }
}
</script>

<style scoped>
.pokemon-list {
  scroll-behavior: smooth;
}

.pokemon-card {
  animation: fadeInUp 0.45s ease-out;
  will-change: transform;
  transition: transform 0.45s cubic-bezier(0.2,0.8,0.2,1), box-shadow 0.35s ease;
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(255,255,255,0.9), rgba(255,255,255,0.8));
}

.pokemon-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 18px 40px rgba(59,130,246,0.12), 0 6px 20px rgba(99,102,241,0.06);
  border-color: rgba(59,130,246,0.12);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 滚动条美化 */
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

/* 加载指示器动画 */
.loading-dots {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #3b82f6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 骨架屏动画 */
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
