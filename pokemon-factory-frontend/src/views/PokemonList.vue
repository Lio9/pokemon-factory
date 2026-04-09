<template>
  <div
    ref="listContainer"
    class="pokemon-list"
  >
    <!-- 搜索和筛选区域 -->
    <div class="search-section mb-6 rounded-2xl border border-transparent p-4 shadow-lg transition-all duration-300 hover:shadow-xl sticky top-[4.25rem] z-10 sm:mb-8 sm:p-6 sm:top-[4.75rem] card-glass">
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
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button
                class="!bg-gradient-to-r !from-blue-500 !to-indigo-600 !text-white !border-none hover:!from-blue-600 hover:!to-indigo-700"
                @click="handleSearch"
              >
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 属性筛选 -->
        <div class="w-full sm:w-48">
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
        <div class="w-full sm:w-40">
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

        <!-- 排序 -->
        <div class="w-full sm:w-40">
          <el-select
            v-model="sortBy"
            placeholder="排序"
            size="large"
            class="w-full"
            @change="handleSort"
          >
            <el-option
              label="图鉴编号"
              value="id"
            />
            <el-option
              label="名称"
              value="name"
            />
            <el-option
              label="攻击"
              value="attack"
            />
            <el-option
              label="速度"
              value="speed"
            />
          </el-select>
        </div>

        <!-- 视图切换 -->
        <div class="flex w-full items-center justify-center gap-2 rounded-xl bg-gray-100 p-1 sm:w-auto sm:justify-start">
          <button 
            class="p-2 rounded-lg transition-all duration-300"
            :class="viewMode === 'grid' ? 'bg-white shadow-md' : 'hover:bg-gray-200'"
            title="网格视图"
            @click="viewMode = 'grid'"
          >
            <Grid class="w-5 h-5" />
          </button>
          <button 
            class="p-2 rounded-lg transition-all duration-300"
            :class="viewMode === 'list' ? 'bg-white shadow-md' : 'hover:bg-gray-200'"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <List class="w-5 h-5" />
          </button>
        </div>
      </div>

      <!-- 快速筛选标签 -->
      <div class="mt-4 flex flex-wrap gap-2">
        <button
          v-for="quickFilter in quickFilters"
          :key="quickFilter.key"
          class="px-3 py-1.5 rounded-full text-sm font-medium transition-all duration-300"
          :class="activeQuickFilters.includes(quickFilter.key)
            ? 'bg-gradient-to-r from-purple-500 to-pink-500 text-white shadow-md'
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
          @click="toggleQuickFilter(quickFilter.key)"
        >
          {{ quickFilter.icon }} {{ quickFilter.label }}
        </button>
      </div>
    </div>

    <!-- 加载中 - 首次加载 -->
    <div
      v-if="loading && pokemons.length === 0"
      class="text-center py-12"
    >
      <div :class="viewMode === 'grid' ? 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5' : 'space-y-4'">
        <div
          v-for="i in 12"
          :key="i"
          :class="viewMode === 'grid' ? 'bg-white rounded-2xl shadow-lg p-4 overflow-hidden' : 'bg-white rounded-2xl shadow-lg p-4 flex items-center gap-4'"
        >
          <div :class="viewMode === 'grid' ? 'aspect-square mb-4 rounded-xl skeleton' : 'w-20 h-20 rounded-xl skeleton flex-shrink-0'" />
          <div :class="viewMode === 'grid' ? '' : 'flex-1'">
            <div
              class="h-6 mb-2 rounded skeleton"
              :class="viewMode === 'list' ? 'w-32' : ''"
            />
            <div class="h-4 w-3/4 rounded skeleton" />
            <div
              v-if="viewMode === 'grid'"
              class="flex gap-2 mt-3"
            >
              <div class="h-6 w-16 rounded-full skeleton" />
              <div class="h-6 w-16 rounded-full skeleton" />
            </div>
          </div>
        </div>
      </div>
      <div class="loading-dots mt-8">
        <span />
        <span />
        <span />
      </div>
    </div>
    
    <!-- 宝可梦列表 -->
    <div v-else-if="pokemons.length > 0">
      <!-- 网格视图 -->
      <div
        v-if="viewMode === 'grid'"
        class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-5 md:grid-cols-4 lg:grid-cols-6"
      >
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card bg-white rounded-2xl shadow-lg hover:shadow-2xl transition-all duration-500 cursor-pointer overflow-hidden group border-2 border-transparent hover:border-blue-200 relative"
        >
          <!-- 收藏按钮 -->
          <button 
            class="absolute top-3 right-3 z-10 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
            :class="isFavorite(pokemon.id) ? 'bg-red-500 text-white shadow-lg fav-bounce' : 'bg-white/90 text-gray-400 hover:text-red-500'"
            @click.prevent="toggleFavorite(pokemon)"
          >
            <span
              class="w-4 h-4 text-sm"
              :class="isFavorite(pokemon.id) ? 'text-red-500' : 'text-gray-400'"
            >❤️</span>
          </button>
          
          <!-- 图片区域 -->
          <div class="relative bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 p-4">
            <div class="aspect-square flex items-center justify-center">
              <!-- 懒加载占位 -->
              <div 
                v-if="!pokemon._imageLoaded" 
                class="w-full h-full flex items-center justify-center skeleton rounded-xl"
              >
                <div class="text-center">
                  <div class="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center mb-2">
                    <svg
                      class="w-8 h-8 text-blue-300 animate-spin"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        class="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        stroke-width="4"
                      />
                      <path
                        class="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                  </div>
                  <span class="text-xs text-gray-400">#{{ String(pokemon.id).padStart(4, '0') }}</span>
                </div>
              </div>
              <img 
                v-show="pokemon._imageLoaded"
                :src="pokemon._imageUrl"
                :alt="pokemon.name"
                class="w-full h-full object-contain group-hover:scale-110 group-hover:drop-shadow-2xl transition-all duration-500"
                loading="lazy"
                @load="handleImageLoad(pokemon)"
                @error="handleImageError(pokemon)"
              >
            </div>
            <!-- 图鉴编号 -->
            <div class="absolute top-3 left-3 bg-gradient-to-r from-gray-900 to-gray-700 text-white text-xs font-bold px-3 py-1 rounded-full shadow-lg">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
            <!-- 特殊标记 -->
            <div
              v-if="pokemon.isLegendary"
              class="absolute top-3 right-12"
            >
              <div class="w-8 h-8 bg-gradient-to-br from-yellow-400 to-amber-500 rounded-full flex items-center justify-center shadow-lg animate-pulse">
                <span class="text-white text-sm font-bold">★</span>
              </div>
            </div>
            <div
              v-else-if="pokemon.isMythical"
              class="absolute top-3 right-12"
            >
              <div class="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center shadow-lg animate-pulse">
                <span class="text-white text-sm font-bold">◆</span>
              </div>
            </div>
          </div>
          
          <!-- 信息区域 -->
          <div class="p-4">
            <h3 class="font-bold text-gray-900 truncate text-lg group-hover:text-blue-600 transition-colors">
              {{ pokemon.name }}
            </h3>
            <p class="text-gray-500 text-sm truncate">
              {{ pokemon.genus }}
            </p>
            
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

      <!-- 列表视图 -->
      <div
        v-else
        class="space-y-3 sm:space-y-4"
      >
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card-list relative flex flex-col gap-4 overflow-hidden rounded-2xl border-2 border-transparent bg-white p-4 shadow-lg transition-all duration-300 hover:border-blue-200 hover:shadow-2xl sm:flex-row sm:items-center"
        >
          <!-- 收藏按钮 -->
          <button 
            class="absolute top-4 right-4 z-10 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
            :class="isFavorite(pokemon.id) ? 'bg-red-500 text-white shadow-lg fav-bounce' : 'bg-white/90 text-gray-400 hover:text-red-500'"
            @click.prevent="toggleFavorite(pokemon)"
          >
            <span
              class="w-4 h-4 text-sm"
              :class="isFavorite(pokemon.id) ? 'text-red-500' : 'text-gray-400'"
            >❤️</span>
          </button>
          
          <!-- 图片 -->
          <div class="relative bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 rounded-xl p-3 flex-shrink-0">
            <div class="w-20 h-20 flex items-center justify-center">
              <div 
                v-if="!pokemon._imageLoaded" 
                class="w-full h-full flex items-center justify-center skeleton rounded-lg"
              />
              <img 
                v-show="pokemon._imageLoaded"
                :src="pokemon._imageUrl"
                :alt="pokemon.name"
                class="w-full h-full object-contain group-hover:scale-110 transition-transform duration-300"
                loading="lazy"
                @load="handleImageLoad(pokemon)"
                @error="handleImageError(pokemon)"
              >
            </div>
            <!-- 图鉴编号 -->
            <div class="absolute -top-2 -left-2 bg-gradient-to-r from-gray-900 to-gray-700 text-white text-xs font-bold px-2 py-0.5 rounded-full shadow-lg">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
          </div>
          
          <!-- 信息 -->
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="font-bold text-gray-900 text-lg truncate group-hover:text-blue-600 transition-colors">
                {{ pokemon.name }}
              </h3>
              <span
                v-if="pokemon.isLegendary"
                class="text-yellow-500"
              >★</span>
              <span
                v-if="pokemon.isMythical"
                class="text-purple-500"
              >◆</span>
            </div>
            <p class="text-gray-500 text-sm truncate">
              {{ pokemon.genus }}
            </p>
            
            <!-- 属性标签 -->
            <div class="flex flex-wrap gap-2 mt-2">
              <span 
                v-for="type in pokemon.types"
                :key="type.id"
                class="px-2 py-0.5 rounded-full text-xs font-bold text-white"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>

          <!-- 种族值预览 -->
          <div
            v-if="pokemon.formStats"
            class="hidden sm:flex gap-2 flex-shrink-0"
          >
            <div class="text-center">
              <div class="text-xs text-gray-500">
                攻击
              </div>
              <div class="text-sm font-bold text-gray-900">
                {{ pokemon.formStats.attack }}
              </div>
            </div>
            <div class="text-center">
              <div class="text-xs text-gray-500">
                速度
              </div>
              <div class="text-sm font-bold text-gray-900">
                {{ pokemon.formStats.speed }}
              </div>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 加载更多指示器 -->
      <div 
        ref="loadMoreTrigger" 
        class="text-center py-8 min-h-[120px] flex items-center justify-center"
      >
        <div
          v-if="loadingMore"
          class="flex flex-col items-center gap-3"
        >
          <div class="loading-dots">
            <span />
            <span />
            <span />
          </div>
          <p class="text-gray-500 text-sm font-medium">
            加载更多宝可梦中...
          </p>
        </div>
        <div
          v-else-if="!hasMore"
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-gray-400">
            <el-icon class="text-xl">
              <CircleCheck />
            </el-icon>
            <span class="text-sm font-medium">已加载全部 {{ total }} 只宝可梦</span>
          </div>
        </div>
        <div
          v-else
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-gray-400">
            <el-icon class="text-xl animate-bounce">
              <ArrowDown />
            </el-icon>
            <span class="text-sm">继续下拉加载更多...</span>
          </div>
        </div>
      </div>

      <!-- 快速返回顶部 -->
      <transition name="fade">
        <button 
          v-show="showBackTop"
          class="fixed bottom-8 right-8 w-14 h-14 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-full shadow-xl hover:shadow-2xl hover:from-blue-600 hover:to-indigo-700 transition-all duration-300 z-20 flex items-center justify-center group transform hover:scale-110"
          @click="scrollToTop"
        >
          <el-icon class="text-2xl group-hover:-translate-y-1 transition-transform">
            <ArrowUp />
          </el-icon>
        </button>
      </transition>
    </div>

    <!-- 空状态 -->
    <div
      v-else
      class="text-center py-12"
    >
      <div class="text-gray-300 mb-4">
        <svg
          class="w-20 h-20 mx-auto"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 15c-2.34 0-4.47-.881-6.08-2.334M15 10a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
      </div>
      <p class="text-gray-500 text-lg">
        没有找到宝可梦
      </p>
      <p class="text-gray-400 text-sm mt-2">
        试试其他搜索条件
      </p>
      <button 
        class="mt-4 px-6 py-2 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-indigo-700 transition-all"
        @click="resetFilters"
      >
        重置筛选
      </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Loading, ArrowUp, CircleCheck, ArrowDown, Grid, List } from '@element-plus/icons-vue'
import { pokemonApi, typeApi, sprites } from '../services/api.js'
import { dataCache } from '../services/cache.js'

export default {
  name: 'PokemonList',
  components: { Search, Loading, ArrowUp, CircleCheck, ArrowDown, Grid, List },
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
    const sortBy = ref('id')
    const viewMode = ref('grid') // 'grid' or 'list'
    const activeQuickFilters = ref([])
    const favorites = ref(new Set())
    
    // 防抖定时器
    let searchTimer = null
    
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
      
      if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value)
      }
    }

    // 初始化
    onMounted(async () => {
      window.addEventListener('scroll', handleScroll, { passive: true })
      loadFavorites()
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
      if (scrollThrottleTimer) clearTimeout(scrollThrottleTimer)
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
      handleSearchInput,
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
