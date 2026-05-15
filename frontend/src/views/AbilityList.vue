<template>
  <div ref="listContainer" class="ability-list">
    <!-- 搜索和筛选 -->
    <div class="glass-card mb-6 p-4 sticky top-[4.25rem] z-10 sm:top-[4.75rem]">
      <div class="flex flex-col gap-4">
        <div class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              :placeholder="tr('搜索特性名称...', 'Search abilities...')"
              clearable
              size="large"
              @input="handleSearchInput"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button class="!bg-gradient-to-r !from-violet-500 !to-purple-600 !text-white !border-none hover:!from-violet-600 hover:!to-purple-700" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="flex gap-2">
            <el-button
              size="large"
              :class="viewMode === 'grid' ? 'bg-gradient-to-r from-violet-500 to-purple-600 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
              @click="toggleViewMode"
            >
              <component :is="viewMode === 'grid' ? 'List' : 'Grid'" class="w-4 h-4" />
              <span class="ml-1">{{ viewMode === 'grid' ? tr('列表', 'List') : tr('网格', 'Grid') }}</span>
            </el-button>
            <el-button
              size="large"
              :class="isShowFavorites ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
              @click="toggleFavorites"
            >
              <component :is="isShowFavorites ? 'StarFilled' : 'Star'" class="w-4 h-4" />
              <span class="ml-1">{{ isShowFavorites ? tr('全部', 'All') : `${tr('收藏', 'Fav')} (${favorites.size})` }}</span>
            </el-button>
            <el-button size="large" class="bg-white text-slate-600 border-slate-300" @click="showFilters = !showFilters">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" /></svg>
              <span class="ml-1">{{ tr('筛选', 'Filter') }}</span>
            </el-button>
          </div>
        </div>

        <!-- 高级筛选 -->
        <el-collapse-transition>
          <div v-if="showFilters" class="flex flex-wrap gap-3 items-end p-4 rounded-xl bg-slate-50/80 border border-slate-100">
            <div class="flex-1 min-w-[160px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('代数筛选', 'Generation') }}</label>
              <el-select v-model="selectedGeneration" :placeholder="tr('世代', 'Gen')" clearable size="default" class="w-full" @change="applyFilters">
                <el-option v-for="g in 9" :key="g" :label="`${tr('第', 'Gen')} ${g} ${tr('世代', '')}`" :value="String(g)" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('描述长度', 'Desc length') }}</label>
              <el-select v-model="descriptionLength" :placeholder="tr('描述长度', 'Length')" clearable size="default" class="w-full" @change="applyFilters">
                <el-option :label="tr('简短 (<50字)', 'Short (<50)')" value="short" />
                <el-option :label="tr('中等 (50-100字)', 'Medium (50-100)')" value="medium" />
                <el-option :label="tr('详细 (>100字)', 'Long (>100)')" value="long" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('排序', 'Sort') }}</label>
              <el-select v-model="sortBy" size="default" class="w-full" @change="handleSort">
                <el-option :label="tr('默认', 'Default')" value="default" />
                <el-option label="A-Z" value="name-asc" />
                <el-option label="Z-A" value="name-desc" />
              </el-select>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- 统计栏 -->
    <div class="flex items-center justify-between mb-4 px-1">
      <div class="text-sm text-slate-500">
        {{ tr('共 {total} 个特性', '{total} abilities total', { total }) }}
        <span v-if="isShowFavorites" class="ml-2 text-amber-600 font-medium">· {{ tr('收藏', 'Favorites') }}</span>
      </div>
      <div class="text-xs text-slate-400">{{ tr('已加载 {count}', 'Loaded {count}', { count: loadedCount }) }}</div>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading && abilities.length === 0" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="glass-card p-4 animate-pulse">
        <div class="h-5 bg-slate-200 rounded w-2/3 mb-3" />
        <div class="h-12 bg-slate-100 rounded w-full" />
      </div>
    </div>

    <!-- 网格视图 -->
    <template v-if="viewMode === 'grid'">
      <div v-if="abilities.length" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div
          v-for="(ability, index) in abilities"
          :key="ability.id"
          class="shine-effect glass-card-interactive glass-card p-5 cursor-pointer animate-slide-up relative overflow-hidden group"
          :style="{ animationDelay: `${index * 30}ms` }"
          @click="showAbilityDetail(ability)"
        >
          <!-- 渐变背景遮罩 -->
          <div 
            class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
            style="background: linear-gradient(135deg, #8b5cf6 20, #a855f7 40)"
          ></div>
          
          <div class="relative z-10">
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-sm shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">✦</div>
                <h3 class="font-semibold text-slate-800 group-hover:text-purple-700 transition-colors">{{ ability.name }}</h3>
              </div>
              <button
                class="flex-shrink-0 transition-all duration-200 hover:scale-125"
                :class="favorites.has(ability.id) ? 'text-amber-500 fav-bounce' : 'text-slate-300 hover:text-amber-400'"
                @click.stop="toggleFavorite(ability)"
              >
                <component :is="favorites.has(ability.id) ? 'StarFilled' : 'Star'" class="w-4 h-4" />
              </button>
            </div>
            <p class="text-sm text-slate-600 leading-relaxed line-clamp-3">
              {{ ability.description || ability.effect || '' }}
            </p>
            <div v-if="ability.generation" class="mt-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-gradient-to-r from-violet-100 to-purple-100 text-violet-600 shadow-sm">
                {{ tr('第 {gen} 世代', 'Gen {gen}', { gen: ability.generation }) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 列表视图 -->
    <template v-else>
      <div v-if="abilities.length" class="space-y-2">
        <div
          v-for="(ability, index) in abilities"
          :key="ability.id"
          class="glass-card-interactive glass-card p-4 flex items-center gap-4 cursor-pointer animate-slide-up"
          :style="{ animationDelay: `${index * 20}ms` }"
          @click="showAbilityDetail(ability)"
        >
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-sm shadow-sm flex-shrink-0">✦</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-slate-800">{{ ability.name }}</h3>
              <span v-if="ability.generation" class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-500">{{ tr('Gen {gen}', 'Gen {gen}', { gen: ability.generation }) }}</span>
            </div>
            <p class="text-sm text-slate-500 mt-1 truncate">{{ ability.description || ability.effect || '' }}</p>
          </div>
          <button
            class="flex-shrink-0 transition-transform duration-200 hover:scale-110"
            :class="favorites.has(ability.id) ? 'text-amber-500' : 'text-slate-300 hover:text-amber-400'"
            @click.stop="toggleFavorite(ability)"
          >
            <component :is="favorites.has(ability.id) ? 'StarFilled' : 'Star'" class="w-5 h-5" />
          </button>
        </div>
      </div>
    </template>

    <div v-if="!loading && abilities.length === 0" class="text-center py-16">
      <div class="text-4xl mb-4">🔍</div>
      <p class="text-slate-500">{{ tr('没有找到特性', 'No abilities found') }}</p>
    </div>

    <!-- 加载更多 -->
    <div ref="loadMoreTrigger" class="text-center py-8">
      <div v-if="loadingMore" class="flex items-center justify-center gap-3">
        <div class="loading-dots"><span /><span /><span /></div>
        <span class="text-sm text-slate-400">{{ tr('加载中...', 'Loading...') }}</span>
      </div>
      <div v-else-if="!hasMore && abilities.length > 0" class="text-sm text-slate-400">
        {{ tr('已加载全部 {total} 个特性', 'All {total} abilities loaded', { total }) }}
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedAbility?.name"
      width="520px"
      :close-on-click-modal="true"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="selectedAbility" class="space-y-5">
        <div class="flex items-center gap-2">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white text-lg shadow-sm">✦</div>
          <div>
            <h3 class="font-semibold text-slate-800 text-lg">{{ selectedAbility.name }}</h3>
            <span v-if="selectedAbility.generation" class="text-xs text-slate-400">{{ tr('第 {gen} 世代引入', 'Introduced in Gen {gen}', { gen: selectedAbility.generation }) }}</span>
          </div>
        </div>

        <div class="rounded-xl bg-slate-50 p-4 text-sm text-slate-700 leading-relaxed">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">{{ tr('效果', 'Effect') }}</div>
          {{ selectedAbility.description || selectedAbility.effect || tr('暂无描述', 'No description') }}
        </div>

        <div v-if="selectedAbility.effect && selectedAbility.effect !== selectedAbility.description" class="rounded-xl bg-violet-50 p-4 text-sm text-violet-700 leading-relaxed">
          <div class="text-xs font-semibold uppercase tracking-wider text-violet-500 mb-2">{{ tr('详细效果', 'Detailed effect') }}</div>
          {{ selectedAbility.effect }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { abilityApi } from '../services/api.js'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const FAVORITES_KEY = 'pokemon-factory-ability-favorites'

export default {
  name: 'AbilityList',
  components: { Search },
  setup() {
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)

    const keyword = ref('')
    const selectedGeneration = ref('')
    const descriptionLength = ref('')
    const sortBy = ref('default')
    const viewMode = ref('grid')
    const showFilters = ref(false)

    const isShowFavorites = ref(false)
    const favorites = ref(new Set())

    const abilities = ref([])
    const currentPage = ref(0)
    const pageSize = ref(36)
    const total = ref(0)

    const loading = ref(false)
    const loadingMore = ref(false)
    const showDetailDialog = ref(false)
    const selectedAbility = ref(null)

    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => abilities.value.length)

    const filteredAbilities = ref([])

    const loadFavorites = () => {
      try {
        const saved = localStorage.getItem(FAVORITES_KEY)
        if (saved) {
          favorites.value = new Set(JSON.parse(saved))
        }
      } catch { /* ignore */ }
    }

    const saveFavorites = () => {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify([...favorites.value]))
    }

    const toggleFavorite = (ability) => {
      if (favorites.value.has(ability.id)) {
        favorites.value.delete(ability.id)
      } else {
        favorites.value.add(ability.id)
      }
      saveFavorites()
    }

    const toggleFavorites = () => {
      isShowFavorites.value = !isShowFavorites.value
      applyFilters()
    }

    const applyFilters = () => {
      let result = [...abilities.value]

      // 关键字搜索
      if (keyword.value) {
        const kw = keyword.value.toLowerCase()
        result = result.filter(a =>
          a.name.toLowerCase().includes(kw) ||
          a.nameEn?.toLowerCase().includes(kw) ||
          a.description?.toLowerCase().includes(kw)
        )
      }

      // 世代筛选
      if (selectedGeneration.value) {
        result = result.filter(a => String(a.generation) === selectedGeneration.value)
      }

      // 描述长度筛选
      if (descriptionLength.value) {
        result = result.filter(a => {
          const text = (a.description || a.effect || '').length
          if (descriptionLength.value === 'short') return text < 50
          if (descriptionLength.value === 'medium') return text >= 50 && text <= 100
          if (descriptionLength.value === 'long') return text > 100
          return true
        })
      }

      // 收藏筛选
      if (isShowFavorites.value) {
        result = result.filter(a => favorites.value.has(a.id))
      }

      // 排序
      if (sortBy.value !== 'default') {
        const [field, order] = sortBy.value.split('-')
        result.sort((a, b) => {
          const valA = a[field]
          const valB = b[field]
          if (order === 'asc') return valA > valB ? 1 : -1
          return valA < valB ? 1 : -1
        })
      }

      filteredAbilities.value = result
    }

    const handleSort = () => {
      applyFilters()
    }

    const fetchAbilities = async (isLoadMore = false) => {
      if (loading.value || loadingMore.value) return
      if (isLoadMore && !hasMore.value) return

      if (isLoadMore) {
        loadingMore.value = true
      } else {
        loading.value = true
        currentPage.value = 0
        abilities.value = []
      }

      try {
        const nextPage = currentPage.value + 1
        const result = await abilityApi.getList({
          current: nextPage,
          size: pageSize.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          abilities.value = [...abilities.value, ...(result.data.records || [])]
          total.value = result.data.total || 0
          currentPage.value = nextPage
          applyFilters()
        }
      } catch (error) {
        console.error('获取特性列表失败:', error)
      } finally {
        loading.value = false
        loadingMore.value = false
      }
    }

    const handleSearchInput = () => {
      if (searchTimer) clearTimeout(searchTimer)
      searchTimer = setTimeout(() => handleSearch(), 300)
    }

    const handleSearch = () => {
      fetchAbilities(false)
    }

    const showAbilityDetail = (ability) => {
      selectedAbility.value = ability
      showDetailDialog.value = true
    }

    const setupObserver = () => {
      if (observer) observer.disconnect()
      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
              fetchAbilities(true)
            }
          })
        },
        { rootMargin: '200px', threshold: 0 }
      )
      if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value)
      }
    }

    onMounted(() => {
      loadFavorites()
      fetchAbilities(false)
      nextTick(() => setupObserver())
    })

    onUnmounted(() => {
      if (observer) observer.disconnect()
      if (searchTimer) clearTimeout(searchTimer)
    })

    watch(() => abilities.value.length, () => {
      nextTick(() => {
        if (loadMoreTrigger.value && observer) {
          observer.disconnect()
          observer.observe(loadMoreTrigger.value)
        }
      })
    })

    watch([keyword, selectedGeneration, descriptionLength, isShowFavorites], () => {
      applyFilters()
    })

    return {
      listContainer,
      loadMoreTrigger,
      loading,
      loadingMore,
      abilities: filteredAbilities,
      keyword,
      selectedGeneration,
      descriptionLength,
      sortBy,
      viewMode,
      showFilters,
      isShowFavorites,
      favorites,
      showDetailDialog,
      selectedAbility,
      currentPage,
      pageSize,
      total,
      hasMore,
      loadedCount,
      handleSearchInput,
      handleSearch,
      handleSort,
      toggleViewMode,
      toggleFavorite,
      toggleFavorites,
      showAbilityDetail
    }
  }
}
</script>

<style scoped>
.ability-list {
  padding-bottom: 1rem;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 骨架屏 */
.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-slide-up {
  animation: slideUp 0.4s ease-out both;
}

/* 详情弹窗 */
:deep(.detail-dialog .el-dialog) {
  border-radius: 1.5rem !important;
}

:deep(.detail-dialog .el-dialog__header) {
  padding: 1.5rem 1.5rem 0;
}

:deep(.detail-dialog .el-dialog__body) {
  padding: 1.5rem;
}

:deep(.detail-dialog .el-dialog__title) {
  font-weight: 700;
  font-size: 1.25rem;
}

@media (max-width: 640px) {
  .ability-list { padding-bottom: 0.5rem; }
}
</style>
