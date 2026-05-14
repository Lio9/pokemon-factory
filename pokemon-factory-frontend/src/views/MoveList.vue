<template>
  <div ref="listContainer" class="move-list">
    <!-- 搜索和筛选 -->
    <div class="glass-card mb-6 p-4 sticky top-[4.25rem] z-10 sm:top-[4.75rem]">
      <div class="flex flex-col gap-4">
        <div class="flex flex-wrap gap-3">

          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              :placeholder="tr('搜索技能名称...', 'Search moves...')"
              clearable
              size="large"
              @input="handleSearchInput"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button class="!bg-gradient-to-r !from-rose-500 !to-orange-500 !text-white !border-none hover:!from-rose-600 hover:!to-orange-600" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="w-full sm:w-32">
            <el-select v-model="selectedType" :placeholder="tr('属性', 'Type')" clearable size="large" class="w-full" @change="handleSearch">
              <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id">
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded-full" :style="{ backgroundColor: t.color || '#888' }" />
                  <span>{{ t.name }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
          <div class="w-full sm:w-32">
            <el-select v-model="selectedDamageClass" :placeholder="tr('分类', 'Category')" clearable size="large" class="w-full" @change="handleSearch">
              <el-option :label="tr('物理', 'Physical')" value="physical" />
              <el-option :label="tr('特殊', 'Special')" value="special" />
              <el-option :label="tr('变化', 'Status')" value="status" />
            </el-select>
          </div>
          <div class="flex gap-2">
            <el-button
              size="large"
              :class="viewMode === 'grid' ? 'bg-gradient-to-r from-rose-500 to-orange-500 text-white border-none' : 'bg-white text-slate-600 border-slate-300'"
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
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('威力范围', 'Power range') }}</label>
              <el-select v-model="powerRange" :placeholder="tr('威力', 'Power')" clearable size="default" class="w-full" @change="handleSearch">
                <el-option :label="tr('无威力', 'No power')" value="none" />
                <el-option label="1-40" value="1-40" />
                <el-option label="41-70" value="41-70" />
                <el-option label="71-100" value="71-100" />
                <el-option label="101-150" value="101-150" />
                <el-option label="151+" value="151+" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('命中范围', 'Accuracy') }}</label>
              <el-select v-model="accuracyRange" :placeholder="tr('命中', 'Accuracy')" clearable size="default" class="w-full" @change="handleSearch">
                <el-option :label="tr('必中', 'Always')" value="100" />
                <el-option label="75-99" value="75-99" />
                <el-option label="50-74" value="50-74" />
                <el-option label="<50" value="0-49" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">PP</label>
              <el-select v-model="ppRange" :placeholder="tr('PP', 'PP')" clearable size="default" class="w-full" @change="handleSearch">
                <el-option label="1-5" value="1-5" />
                <el-option label="6-10" value="6-10" />
                <el-option label="11-20" value="11-20" />
                <el-option label="21+" value="21+" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs font-semibold text-slate-500 mb-1.5 block">{{ tr('排序', 'Sort') }}</label>
              <el-select v-model="sortBy" size="default" class="w-full" @change="handleSort">
                <el-option :label="tr('默认', 'Default')" value="default" />
                <el-option label="A-Z" value="name-asc" />
                <el-option label="Z-A" value="name-desc" />
                <el-option :label="tr('威力↑', 'Power ↑')" value="power-asc" />
                <el-option :label="tr('威力↓', 'Power ↓')" value="power-desc" />
              </el-select>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- 统计栏 -->
    <div class="flex items-center justify-between mb-4 px-1">
      <div class="text-sm text-slate-500">
        {{ tr('共 {total} 个技能', '{total} moves total', { total }) }}
        <span v-if="isShowFavorites" class="ml-2 text-amber-600 font-medium">· {{ tr('收藏', 'Favorites') }}</span>
      </div>
      <div class="text-xs text-slate-400">{{ tr('已加载 {count}', 'Loaded {count}', { count: loadedCount }) }}</div>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading && moves.length === 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
      <div v-for="i in 12" :key="i" class="glass-card p-4 animate-pulse">
        <div class="h-4 bg-slate-200 rounded w-3/4 mb-3" />
        <div class="flex gap-2 mb-3"><div class="h-5 bg-slate-200 rounded-full w-14" /><div class="h-5 bg-slate-100 rounded-full w-14" /></div>
        <div class="h-3 bg-slate-100 rounded w-1/2" />
      </div>
    </div>

    <!-- 网格视图 -->
    <template v-if="viewMode === 'grid'">
      <div v-if="moves.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
        <div
          v-for="(move, index) in moves"
          :key="move.id"
          class="shine-effect glass-card-interactive glass-card p-4 cursor-pointer animate-slide-up relative overflow-hidden group"
          :style="{ animationDelay: `${index * 25}ms` }"
          @click="showMoveDetail(move)"
        >
          <!-- 渐变背景遮罩 -->
          <div 
            class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
            :style="{ background: `linear-gradient(135deg, ${move.typeColor || '#888'}20, ${move.typeColor || '#888'}40)` }"
          ></div>
          
          <div class="relative z-10">
            <div class="flex items-start justify-between mb-2">
              <h3 class="font-semibold text-slate-800 text-sm leading-tight truncate flex-1 group-hover:text-blue-700 transition-colors">{{ move.name }}</h3>
              <button
                class="ml-1 flex-shrink-0 transition-all duration-200 hover:scale-125"
                :class="favorites.has(move.id) ? 'text-amber-500 fav-bounce' : 'text-slate-300 hover:text-amber-400'"
                @click.stop="toggleFavorite(move)"
              >
                <component :is="favorites.has(move.id) ? 'StarFilled' : 'Star'" class="w-4 h-4" />
              </button>
            </div>
            <div class="flex flex-wrap gap-1.5 mb-2">
              <span v-if="move.typeName" class="type-badge type-badge-sm shadow-md" :style="{ backgroundColor: move.typeColor || '#888' }">{{ move.typeName }}</span>
              <span
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold shadow-sm"
                :class="{
                  'bg-rose-100 text-rose-700': move.damageClass === 'physical',
                  'bg-blue-100 text-blue-700': move.damageClass === 'special',
                  'bg-purple-100 text-purple-700': move.damageClass === 'status'
                }"
              >
                {{ move.damageClass === 'physical' ? tr('物理', 'Phys') : move.damageClass === 'special' ? tr('特殊', 'Spec') : tr('变化', 'Sts') }}
              </span>
            </div>
            <div class="flex items-center gap-3 text-xs text-slate-500">
              <span v-if="move.power != null" class="font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">{{ tr('威力', 'Pwr') }} {{ move.power }}</span>
              <span v-if="move.accuracy != null" class="font-medium">{{ move.accuracy }}%</span>
              <span>PP {{ move.pp }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 列表视图 -->
    <template v-else>
      <div v-if="moves.length" class="space-y-2">
        <div
          v-for="(move, index) in moves"
          :key="move.id"
          class="glass-card-interactive glass-card p-3 flex items-center gap-4 cursor-pointer animate-slide-up"
          :style="{ animationDelay: `${index * 15}ms` }"
          @click="showMoveDetail(move)"
        >
          <span class="text-xs text-slate-400 w-10 font-mono">#{{ move.id }}</span>
          <div class="flex-1 flex items-center gap-3">
            <h3 class="font-semibold text-slate-800 text-sm">{{ move.name }}</h3>
            <span v-if="move.typeName" class="type-badge type-badge-sm" :style="{ backgroundColor: move.typeColor || '#888' }">{{ move.typeName }}</span>
            <span
              class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold"
              :class="{
                'bg-rose-100 text-rose-700': move.damageClass === 'physical',
                'bg-blue-100 text-blue-700': move.damageClass === 'special',
                'bg-purple-100 text-purple-700': move.damageClass === 'status'
              }"
            >
              {{ move.damageClass === 'physical' ? tr('物理', 'Phys') : move.damageClass === 'special' ? tr('特殊', 'Spec') : tr('变化', 'Sts') }}
            </span>
          </div>
          <div class="flex items-center gap-4 text-xs text-slate-500">
            <span v-if="move.power != null" class="font-semibold text-slate-700">{{ move.power }}</span>
            <span>{{ move.accuracy != null ? `${move.accuracy}%` : '-' }}</span>
            <span>PP {{ move.pp }}</span>
          </div>
          <button
            class="flex-shrink-0 transition-transform duration-200 hover:scale-110"
            :class="favorites.has(move.id) ? 'text-amber-500' : 'text-slate-300 hover:text-amber-400'"
            @click.stop="toggleFavorite(move)"
          >
            <component :is="favorites.has(move.id) ? 'StarFilled' : 'Star'" class="w-5 h-5" />
          </button>
        </div>
      </div>
    </template>

    <div v-if="!loading && moves.length === 0" class="text-center py-16">
      <div class="text-4xl mb-4">🔍</div>
      <p class="text-slate-500">{{ tr('没有找到技能', 'No moves found') }}</p>
    </div>

    <!-- 加载更多 -->
    <div ref="loadMoreTrigger" class="text-center py-8">
      <div v-if="loadingMore" class="flex items-center justify-center gap-3">
        <div class="loading-dots"><span /><span /><span /></div>
        <span class="text-sm text-slate-400">{{ tr('加载中...', 'Loading...') }}</span>
      </div>
      <div v-else-if="!hasMore && moves.length > 0" class="text-sm text-slate-400">
        {{ tr('已加载全部 {total} 个技能', 'All {total} moves loaded', { total }) }}
      </div>
    </div>

    <!-- 技能详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedMove?.name"
      width="520px"
      :close-on-click-modal="true"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="selectedMove" class="space-y-5">
        <div class="flex items-center gap-3">
          <span v-if="selectedMove.typeName" class="type-badge" :style="{ backgroundColor: selectedMove.typeColor || '#888' }">{{ selectedMove.typeName }}</span>
          <span
            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold"
            :class="{
              'bg-rose-100 text-rose-700': selectedMove.damageClass === 'physical',
              'bg-blue-100 text-blue-700': selectedMove.damageClass === 'special',
              'bg-purple-100 text-purple-700': selectedMove.damageClass === 'status'
            }"
          >
            {{ selectedMove.damageClass === 'physical' ? tr('物理', 'Physical') : selectedMove.damageClass === 'special' ? tr('特殊', 'Special') : tr('变化', 'Status') }}
          </span>
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div class="glass-card p-3 text-center">
            <div class="text-xs text-slate-500 mb-1">{{ tr('威力', 'Power') }}</div>
            <div class="text-xl font-bold text-slate-800">{{ selectedMove.power ?? '-' }}</div>
          </div>
          <div class="glass-card p-3 text-center">
            <div class="text-xs text-slate-500 mb-1">{{ tr('命中', 'Accuracy') }}</div>
            <div class="text-xl font-bold text-slate-800">{{ selectedMove.accuracy != null ? `${selectedMove.accuracy}%` : '-' }}</div>
          </div>
          <div class="glass-card p-3 text-center">
            <div class="text-xs text-slate-500 mb-1">PP</div>
            <div class="text-xl font-bold text-slate-800">{{ selectedMove.pp ?? '-' }}</div>
          </div>
        </div>

        <div v-if="selectedMove.description" class="rounded-xl bg-slate-50 p-4 text-sm text-slate-700 leading-relaxed">
          {{ selectedMove.description }}
        </div>

        <div v-if="selectedMove.effect" class="rounded-xl bg-indigo-50 p-4 text-sm text-indigo-700 leading-relaxed">
          <div class="text-xs font-semibold uppercase tracking-wider text-indigo-500 mb-1">{{ tr('追加效果', 'Additional effect') }}</div>
          {{ selectedMove.effect }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { moveApi, typeApi } from '../services/api.js'
import { dataCache } from '../services/cache.js'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const FAVORITES_KEY = 'pokemon-factory-move-favorites'
const SEARCH_HISTORY_KEY = 'pokemon-factory-move-search'

export default {
  name: 'MoveList',
  components: { Search },
  setup() {
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)

    // 数据
    const keyword = ref('')
    const selectedType = ref('')
    const selectedDamageClass = ref('')
    const powerRange = ref('')
    const accuracyRange = ref('')
    const ppRange = ref('')
    const sortBy = ref('default')
    const viewMode = ref('grid')
    const showFilters = ref(false)

    // 收藏
    const isShowFavorites = ref(false)
    const favorites = ref(new Set())

    // 分页
    const moves = ref([])
    const types = ref([])
    const currentPage = ref(0)
    const pageSize = ref(48)
    const total = ref(0)

    // 状态
    const loading = ref(false)
    const loadingMore = ref(false)
    const showDetailDialog = ref(false)
    const selectedMove = ref(null)

    // 收藏相关
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

    const toggleFavorite = (move) => {
      if (favorites.value.has(move.id)) {
        favorites.value.delete(move.id)
      } else {
        favorites.value.add(move.id)
      }
      saveFavorites()
    }

    const toggleFavorites = () => {
      isShowFavorites.value = !isShowFavorites.value
      applyFilters()
    }

    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => moves.value.length)

    // 注意: moves 模板中使用的是 filteredMoves 由下方 applyFilters 提供
    const filteredMoves = ref([])

    // 筛选和排序
    const applyFilters = () => {
      let result = [...moves.value]

      // 收藏筛选
      if (isShowFavorites.value) {
        result = result.filter(m => favorites.value.has(m.id))
      }

      // 属性筛选
      if (selectedType.value) {
        result = result.filter(m => m.typeId === selectedType.value)
      }

      // 分类筛选
      if (selectedDamageClass.value) {
        result = result.filter(m => m.damageClass === selectedDamageClass.value)
      }

      // 威力筛选
      if (powerRange.value) {
        if (powerRange.value === 'none') {
          result = result.filter(m => m.power === null || m.power === 0)
        } else if (powerRange.value === '151+') {
          result = result.filter(m => m.power >= 151)
        } else {
          const [min, max] = powerRange.value.split('-').map(Number)
          result = result.filter(m => m.power >= min && m.power <= max)
        }
      }

      // 命中筛选
      if (accuracyRange.value) {
        if (accuracyRange.value === '100') {
          result = result.filter(m => m.accuracy === 100 || m.accuracy === null)
        } else {
          const [min, max] = accuracyRange.value.split('-').map(Number)
          result = result.filter(m => m.accuracy >= min && m.accuracy <= max)
        }
      }

      // PP筛选
      if (ppRange.value) {
        if (ppRange.value === '21+') {
          result = result.filter(m => m.pp >= 21)
        } else {
          const [min, max] = ppRange.value.split('-').map(Number)
          result = result.filter(m => m.pp >= min && m.pp <= max)
        }
      }

      // 排序
      if (sortBy.value !== 'default') {
        const [field, order] = sortBy.value.split('-')
        result.sort((a, b) => {
          const valA = a[field] ?? (field === 'name' ? '' : 0)
          const valB = b[field] ?? (field === 'name' ? '' : 0)
          if (typeof valA === 'string') {
            return order === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA)
          }
          return order === 'asc' ? valA - valB : valB - valA
        })
      }

      filteredMoves.value = result
    }

    // 排序处理
    const handleSort = () => {
      applyFilters()
    }

    const fetchTypes = async () => {
      try {
        const result = await dataCache.getOrFetch('types', {}, async () => {
          return await typeApi.getAll()
        })
        if (result.code === 200) {
          types.value = result.data
        }
      } catch (error) {
        console.error('获取属性失败:', error)
      }
    }

    const fetchMoves = async (isLoadMore = false) => {
      if (loading.value || loadingMore.value) return
      if (isLoadMore && !hasMore.value) return

      if (isLoadMore) {
        loadingMore.value = true
      } else {
        loading.value = true
        currentPage.value = 0
        moves.value = []
      }

      try {
        const nextPage = currentPage.value + 1
        const result = await moveApi.getList({
          current: nextPage,
          size: pageSize.value,
          typeId: selectedType.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          moves.value = [...moves.value, ...(result.data.records || [])]
          total.value = result.data.total || 0
          currentPage.value = nextPage
          applyFilters()
        }
      } catch (error) {
        console.error('获取技能列表失败:', error)
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
      fetchMoves(false)
    }

    const showMoveDetail = (move) => {
      selectedMove.value = move
      showDetailDialog.value = true
    }

    const setupObserver = () => {
      if (observer) observer.disconnect()
      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
              fetchMoves(true)
            }
          })
        },
        { rootMargin: '200px', threshold: 0 }
      )
      if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value)
      }
    }

    onMounted(async () => {
      loadFavorites()
      await fetchTypes()
      await fetchMoves(false)
      nextTick(() => setupObserver())
    })

    onUnmounted(() => {
      if (observer) observer.disconnect()
      if (searchTimer) clearTimeout(searchTimer)
    })

    watch(() => moves.value.length, () => {
      nextTick(() => {
        if (loadMoreTrigger.value && observer) {
          observer.disconnect()
          observer.observe(loadMoreTrigger.value)
        }
      })
    })

    // 监听筛选条件变化
    watch([keyword, selectedType, selectedDamageClass, powerRange, accuracyRange, ppRange, isShowFavorites], () => {
      applyFilters()
    })

    return {
      listContainer,
      loadMoreTrigger,
      loading,
      loadingMore,
      moves: filteredMoves,
      types,
      keyword,
      selectedType,
      selectedDamageClass,
      powerRange,
      accuracyRange,
      ppRange,
      sortBy,
      viewMode,
      showFilters,
      isShowFavorites,
      favorites,
      showDetailDialog,
      selectedMove,
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
      showMoveDetail
    }
  }
}
</script>

<style scoped>
.move-list {
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
  background: linear-gradient(135deg, #f43f5e, #f97316);
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

/* 详情弹窗样式覆盖 */
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
  .move-list { padding-bottom: 0.5rem; }
}
</style>
