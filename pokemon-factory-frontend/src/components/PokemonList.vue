<template>
  <div class="pokemon-list" ref="listContainer">
    <!-- 搜索和筛选区域 -->
    <div class="search-section mb-8 bg-white/80 backdrop-blur-md rounded-2xl shadow-lg p-6 border border-gray-100 sticky top-0 z-10">
      <div class="flex flex-col lg:flex-row gap-4">
        <!-- 搜索框 -->
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索宝可梦名称、编号..."
            prefix-icon="Search"
            clearable
            size="large"
            class="search-input"
            @input="handleSearchInput"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch" class="!bg-gradient-to-r !from-blue-500 !to-indigo-600 !text-white !border-none hover:!from-blue-600 hover:!to-indigo-700">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 属性筛选 -->
        <div class="w-48">
          <el-select
            v-model="selectedType"
            placeholder="属性筛选"
            clearable
            size="large"
            class="w-full"
            @change="handleFilter"
          >
            <el-option
              v-for="type in types"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            >
              <div class="flex items-center gap-2">
                <span 
                  class="w-4 h-4 rounded-full shadow-sm"
                  :style="{ backgroundColor: type.color }"
                />
                <span class="font-medium">{{ type.name }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        
        <!-- 世代筛选 -->
        <div class="w-40">
          <el-select
            v-model="selectedGeneration"
            placeholder="世代"
            clearable
            size="large"
            class="w-full"
            @change="handleFilter"
          >
            <el-option
              v-for="gen in generations"
              :key="gen.id"
              :label="gen.name"
              :value="gen.id"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-8">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-gradient-to-br from-blue-500 via-blue-600 to-indigo-600 rounded-2xl p-5 text-white shadow-xl hover:shadow-2xl transition-shadow">
          <div class="text-4xl font-bold">{{ total }}</div>
          <div class="text-blue-100 text-sm font-medium mt-1">总数</div>
        </div>
        <div class="bg-gradient-to-br from-green-500 via-green-600 to-emerald-600 rounded-2xl p-5 text-white shadow-xl hover:shadow-2xl transition-shadow">
          <div class="text-4xl font-bold">{{ totalPages }}</div>
          <div class="text-green-100 text-sm font-medium mt-1">页数</div>
        </div>
        <div class="bg-gradient-to-br from-purple-500 via-purple-600 to-pink-600 rounded-2xl p-5 text-white shadow-xl hover:shadow-2xl transition-shadow">
          <div class="text-4xl font-bold">{{ currentPage }}</div>
          <div class="text-purple-100 text-sm font-medium mt-1">当前页</div>
        </div>
        <div class="bg-gradient-to-br from-orange-500 via-orange-600 to-red-600 rounded-2xl p-5 text-white shadow-xl hover:shadow-2xl transition-shadow">
          <div class="text-4xl font-bold">{{ loadedCount }}</div>
          <div class="text-orange-100 text-sm font-medium mt-1">已加载</div>
        </div>
      </div>
    </div>

    <!-- 加载中 - 首次加载 -->
    <div v-if="loading && pokemons.length === 0" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 宝可梦列表 -->
    <div v-else-if="pokemons.length > 0">
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5">
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card bg-white rounded-2xl shadow-lg hover:shadow-2xl transition-all duration-500 cursor-pointer overflow-hidden group border-2 border-transparent hover:border-blue-200"
        >
          <!-- 图片区域 -->
          <div class="relative bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 p-4">
            <div class="aspect-square flex items-center justify-center">
              <!-- 懒加载占位 -->
              <div 
                v-if="!pokemon._imageLoaded" 
                class="w-full h-full flex items-center justify-center"
              >
                <div class="w-16 h-16 rounded-full bg-gradient-to-br from-blue-100 to-indigo-100 animate-pulse flex items-center justify-center">
                  <svg class="w-8 h-8 text-blue-300 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                </div>
              </div>
              <img 
                v-show="pokemon._imageLoaded"
                :src="pokemon._imageUrl"
                :alt="pokemon.name"
                class="w-full h-full object-contain group-hover:scale-110 group-hover:drop-shadow-2xl transition-all duration-500"
                @load="handleImageLoad(pokemon)"
                @error="handleImageError(pokemon)"
                loading="lazy"
              >
            </div>
            <!-- 图鉴编号 -->
            <div class="absolute top-3 left-3 bg-gradient-to-r from-gray-900 to-gray-700 text-white text-xs font-bold px-3 py-1 rounded-full shadow-lg">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
            <!-- 特殊标记 -->
            <div v-if="pokemon.isLegendary" class="absolute top-3 right-3">
              <div class="w-8 h-8 bg-gradient-to-br from-yellow-400 to-amber-500 rounded-full flex items-center justify-center shadow-lg">
                <span class="text-white text-sm font-bold">★</span>
              </div>
            </div>
            <div v-else-if="pokemon.isMythical" class="absolute top-3 right-3">
              <div class="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center shadow-lg">
                <span class="text-white text-sm font-bold">◆</span>
              </div>
            </div>
          </div>
          
          <!-- 信息区域 -->
          <div class="p-4">
            <h3 class="font-bold text-gray-900 truncate text-lg group-hover:text-blue-600 transition-colors">
              {{ pokemon.name }}
            </h3>
            <p class="text-gray-500 text-sm truncate">{{ pokemon.genus }}</p>
            
            <!-- 属性标签 -->
            <div class="flex flex-wrap gap-2 mt-3">
              <span 
                v-for="type in pokemon.types"
                :key="type.id"
                class="px-3 py-1 rounded-full text-xs font-bold text-white shadow-md"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 加载更多指示器 -->
      <div 
        ref="loadMoreTrigger" 
        class="text-center py-8"
      >
        <div v-if="loadingMore">
          <el-icon class="is-loading text-4xl text-blue-500"><Loading /></el-icon>
          <p class="text-gray-500 mt-2">加载中...</p>
        </div>
        <div v-else-if="!hasMore" class="text-gray-400">
          已加载全部 {{ total }} 只宝可梦
        </div>
        <div v-else class="text-gray-400">
          下拉加载更多...
        </div>
      </div>

      <!-- 快速返回顶部 -->
      <transition name="fade">
        <button 
          v-show="showBackTop"
          @click="scrollToTop"
          class="fixed bottom-8 right-8 w-12 h-12 bg-blue-500 text-white rounded-full shadow-lg hover:bg-blue-600 transition-colors z-20"
        >
          <el-icon class="text-xl"><ArrowUp /></el-icon>
        </button>
      </transition>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-12">
      <div class="text-gray-300 mb-4">
        <svg class="w-20 h-20 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 15c-2.34 0-4.47-.881-6.08-2.334M15 10a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <p class="text-gray-500 text-lg">没有找到宝可梦</p>
      <p class="text-gray-400 text-sm mt-2">试试其他搜索条件</p>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Loading, ArrowUp } from '@element-plus/icons-vue'
import { pokemonApi, typeApi, sprites } from '../services/api.js'
import { dataCache } from '../services/cache.js'

export default {
  name: 'PokemonList',
  components: { Search, Loading, ArrowUp },
  setup() {
    // DOM引用
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)
    
    // 响应式数据
    const loading = ref(false)
    const loadingMore = ref(false)
    const pokemons = ref([])
    const types = ref([])
    const searchKeyword = ref('')
    const selectedType = ref(null)
    const selectedGeneration = ref(null)
    
    // 防抖定时器
    let searchTimer = null
    
    // 分页数据 - 使用较小页面大小实现无限滚动
    const currentPage = ref(0)
    const pageSize = ref(24)
    const total = ref(0)
    
    // 滚动状态
    const showBackTop = ref(false)
    let observer = null
    
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
    
    // 计算属性
    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => pokemons.value.length)

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

    // 处理宝可梦数据 - 添加图片URL
    const processPokemonData = (data) => {
      return data.map(p => ({
        ...p,
        _imageUrl: p.spriteUrl || sprites.pokemon(p.id),
        _imageLoaded: false
      }))
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
          const records = result.data.records || []
          total.value = result.data.total || 0
          currentPage.value = nextPage
          
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

    // 搜索输入处理 - 防抖
    const handleSearchInput = () => {
      if (searchTimer) clearTimeout(searchTimer)
      searchTimer = setTimeout(() => {
        handleSearch()
      }, 300)
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

    // 返回顶部
    const scrollToTop = () => {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    // 监听滚动
    const handleScroll = () => {
      showBackTop.value = window.scrollY > 300
    }

    // 设置Intersection Observer监听加载更多
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
        {
          root: null,
          rootMargin: '200px', // 提前200px开始加载
          threshold: 0
        }
      )
      
      if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value)
      }
    }

    // 初始化
    onMounted(async () => {
      window.addEventListener('scroll', handleScroll)
      await fetchTypes()
      await fetchPokemons(false)
      
      nextTick(() => {
        setupObserver()
      })
    })

    // 清理
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll)
      if (observer) observer.disconnect()
      if (searchTimer) clearTimeout(searchTimer)
    })

    // 监听数据变化重新设置observer
    watch(() => pokemons.value.length, () => {
      nextTick(() => {
        if (loadMoreTrigger.value && observer) {
          observer.disconnect()
          observer.observe(loadMoreTrigger.value)
        }
      })
    })

    return {
      listContainer,
      loadMoreTrigger,
      loading,
      loadingMore,
      pokemons,
      types,
      generations,
      searchKeyword,
      selectedType,
      selectedGeneration,
      currentPage,
      pageSize,
      total,
      totalPages,
      hasMore,
      loadedCount,
      showBackTop,
      handleImageLoad,
      handleImageError,
      handleSearchInput,
      handleSearch,
      handleFilter,
      scrollToTop
    }
  }
}
</script>

<style scoped>
.pokemon-card {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
