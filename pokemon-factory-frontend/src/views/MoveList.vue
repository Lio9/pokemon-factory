<template>
  <div
    ref="listContainer"
    class="move-list"
  >
    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6 sticky top-0 z-10">
      <div class="flex flex-col gap-4">
        <!-- 搜索和基本筛选 -->
        <div class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              placeholder="搜索技能名称..."
              clearable
              @input="handleSearchInput"
              @clear="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </div>
          <div class="w-32">
            <el-select
              v-model="selectedType"
              placeholder="属性"
              clearable
              @change="handleSearch"
            >
              <el-option
                v-for="t in types"
                :key="t.id"
                :label="t.name"
                :value="t.id"
              />
            </el-select>
          </div>
          <div class="w-32">
            <el-select
              v-model="selectedDamageClass"
              placeholder="分类"
              clearable
              @change="handleSearch"
            >
              <el-option
                label="物理"
                value="physical"
              />
              <el-option
                label="特殊"
                value="special"
              />
              <el-option
                label="变化"
                value="status"
              />
            </el-select>
          </div>
          <el-button
            :icon="viewMode === 'grid' ? 'List' : 'Grid'"
            :type="viewMode === 'grid' ? 'primary' : 'default'"
            @click="toggleViewMode"
          >
            {{ viewMode === 'grid' ? '列表' : '网格' }}
          </el-button>
          <el-button
            :icon="isShowFavorites ? 'StarFilled' : 'Star'"
            :type="isShowFavorites ? 'warning' : 'default'"
            @click="toggleFavorites"
          >
            {{ isShowFavorites ? '全部' : `收藏 (${favorites.size})` }}
          </el-button>
        </div>

        <!-- 高级筛选面板 -->
        <el-collapse-transition>
          <div
            v-if="showFilters"
            class="flex flex-wrap gap-3 items-end"
          >
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">威力范围</label>
              <el-select
                v-model="powerRange"
                placeholder="威力"
                clearable
                @change="handleSearch"
              >
                <el-option
                  label="无威力"
                  value="none"
                />
                <el-option
                  label="1-40"
                  value="1-40"
                />
                <el-option
                  label="41-70"
                  value="41-70"
                />
                <el-option
                  label="71-100"
                  value="71-100"
                />
                <el-option
                  label="101-150"
                  value="101-150"
                />
                <el-option
                  label="151+"
                  value="151+"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">命中范围</label>
              <el-select
                v-model="accuracyRange"
                placeholder="命中"
                clearable
                @change="handleSearch"
              >
                <el-option
                  label="必中"
                  value="100"
                />
                <el-option
                  label="90-99"
                  value="90-99"
                />
                <el-option
                  label="80-89"
                  value="80-89"
                />
                <el-option
                  label="70-79"
                  value="70-79"
                />
                <el-option
                  label="<70"
                  value="<70"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">PP范围</label>
              <el-select
                v-model="ppRange"
                placeholder="PP"
                clearable
                @change="handleSearch"
              >
                <el-option
                  label="≤5"
                  value="≤5"
                />
                <el-option
                  label="6-10"
                  value="6-10"
                />
                <el-option
                  label="11-20"
                  value="11-20"
                />
                <el-option
                  label="21-30"
                  value="21-30"
                />
                <el-option
                  label=">30"
                  value=">30"
                />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">排序</label>
              <el-select
                v-model="sortBy"
                @change="handleSort"
              >
                <el-option
                  label="默认"
                  value="default"
                />
                <el-option
                  label="威力 高→低"
                  value="power-desc"
                />
                <el-option
                  label="威力 低→高"
                  value="power-asc"
                />
                <el-option
                  label="命中 高→低"
                  value="accuracy-desc"
                />
                <el-option
                  label="命中 低→高"
                  value="accuracy-asc"
                />
                <el-option
                  label="PP 高→低"
                  value="pp-desc"
                />
                <el-option
                  label="PP 低→高"
                  value="pp-asc"
                />
                <el-option
                  label="名称 A-Z"
                  value="name-asc"
                />
                <el-option
                  label="名称 Z-A"
                  value="name-desc"
                />
              </el-select>
            </div>
          </div>
        </el-collapse-transition>

        <!-- 展开/收起筛选按钮 -->
        <div class="flex justify-center">
          <el-button
            link
            type="primary"
            @click="showFilters = !showFilters"
          >
            {{ showFilters ? '收起筛选' : '展开筛选 ▼' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">
          {{ total }}
        </div>
        <div class="text-red-100 text-sm">
          总数
        </div>
      </div>
      <div class="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">
          {{ loadedCount }}
        </div>
        <div class="text-orange-100 text-sm">
          已加载
        </div>
      </div>
      <div class="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">
          {{ selectedType ? types.find(t => t.id === selectedType)?.name : '全部' }}
        </div>
        <div class="text-yellow-100 text-sm">
          属性筛选
        </div>
      </div>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading && moves.length === 0">
      <div
        v-if="viewMode === 'grid'"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
      >
        <el-skeleton
          v-for="i in 6"
          :key="i"
          animated
        >
          <template #template>
            <el-card class="mb-4">
              <el-skeleton-item
                variant="h3"
                style="width: 50%"
              />
              <el-skeleton-item
                variant="text"
                style="width: 70%"
              />
              <el-skeleton-item
                variant="text"
                style="width: 40%"
              />
              <el-skeleton-item
                variant="rect"
                style="width: 100%; height: 60px; margin-top: 10px"
              />
            </el-card>
          </template>
        </el-skeleton>
      </div>
      <div
        v-else
        class="bg-white rounded-xl shadow-sm overflow-hidden"
      >
        <el-skeleton
          :rows="5"
          animated
        />
      </div>
    </div>

    <!-- 网格视图 -->
    <div
      v-else-if="moves.length && viewMode === 'grid'"
      class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
    >
      <div
        v-for="move in moves"
        :key="move.id"
        class="bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow cursor-pointer group"
        @click="showMoveDetail(move)"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-2">
            <h3 class="font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
              {{ move.name }}
            </h3>
            <span class="text-xs text-gray-400">{{ move.nameEn }}</span>
          </div>
          <el-button
            :icon="favorites.has(move.id) ? 'StarFilled' : 'Star'"
            :type="favorites.has(move.id) ? 'warning' : 'default'"
            text
            size="small"
            @click.stop="toggleFavorite(move.id)"
          />
        </div>
        <div class="flex flex-wrap gap-2 mb-3">
          <span
            class="px-2 py-1 rounded text-white text-xs font-medium"
            :style="{ backgroundColor: move.typeColor }"
          >
            {{ move.typeName }}
          </span>
          <span class="px-2 py-1 rounded bg-gray-100 text-gray-700 text-xs">
            {{ move.damageClass }}
          </span>
        </div>
        <div class="grid grid-cols-3 gap-2 text-center text-sm">
          <div class="bg-red-50 rounded-lg py-2">
            <div class="font-semibold text-red-600">
              {{ move.power || '-' }}
            </div>
            <div class="text-xs text-red-400">
              威力
            </div>
          </div>
          <div class="bg-blue-50 rounded-lg py-2">
            <div class="font-semibold text-blue-600">
              {{ move.accuracy || '-' }}
            </div>
            <div class="text-xs text-blue-400">
              命中
            </div>
          </div>
          <div class="bg-green-50 rounded-lg py-2">
            <div class="font-semibold text-green-600">
              {{ move.pp || '-' }}
            </div>
            <div class="text-xs text-green-400">
              PP
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div
      v-else-if="moves.length"
      class="bg-white rounded-xl shadow-sm overflow-hidden"
    >
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">
                技能
              </th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">
                属性
              </th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">
                分类
              </th>
              <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">
                威力
              </th>
              <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">
                命中
              </th>
              <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">
                PP
              </th>
              <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr
              v-for="move in moves"
              :key="move.id"
              class="hover:bg-gray-50"
            >
              <td class="py-3 px-4">
                <div class="font-medium text-gray-900">
                  {{ move.name }}
                </div>
                <div class="text-xs text-gray-400">
                  {{ move.nameEn }}
                </div>
              </td>
              <td class="py-3 px-4">
                <span
                  class="px-2 py-1 rounded text-white text-xs"
                  :style="{ backgroundColor: move.typeColor }"
                >
                  {{ move.typeName }}
                </span>
              </td>
              <td class="py-3 px-4 text-sm text-gray-500">
                {{ move.damageClass }}
              </td>
              <td class="py-3 px-4 text-center font-medium">
                {{ move.power || '-' }}
              </td>
              <td class="py-3 px-4 text-center">
                {{ move.accuracy || '-' }}
              </td>
              <td class="py-3 px-4 text-center">
                {{ move.pp || '-' }}
              </td>
              <td class="py-3 px-4 text-center">
                <el-button
                  :icon="favorites.has(move.id) ? 'StarFilled' : 'Star'"
                  :type="favorites.has(move.id) ? 'warning' : 'default'"
                  text
                  size="small"
                  @click="toggleFavorite(move.id)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 加载更多 -->
      <div
        ref="loadMoreTrigger"
        class="text-center py-6"
      >
        <div v-if="loadingMore">
          <el-icon class="is-loading text-2xl text-blue-500">
            <Loading />
          </el-icon>
          <span class="text-gray-500 ml-2">加载中...</span>
        </div>
        <div
          v-else-if="!hasMore"
          class="text-gray-400"
        >
          已加载全部 {{ total }} 个技能
        </div>
        <div
          v-else
          class="text-gray-400"
        >
          下拉加载更多...
        </div>
      </div>
    </div>

    <div
      v-else
      class="text-center py-12 text-gray-500"
    >
      没有找到技能
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedMove?.name"
      width="600px"
    >
      <div v-if="selectedMove">
        <div class="mb-4">
          <span class="text-gray-500 text-sm">{{ selectedMove.nameEn }}</span>
        </div>
        <div class="flex gap-3 mb-4">
          <span
            class="px-3 py-1 rounded text-white text-sm"
            :style="{ backgroundColor: selectedMove.typeColor }"
          >
            {{ selectedMove.typeName }}
          </span>
          <span class="px-3 py-1 rounded bg-gray-100 text-gray-700 text-sm">
            {{ selectedMove.damageClass }}
          </span>
        </div>
        <div class="grid grid-cols-3 gap-4 mb-4">
          <div class="text-center">
            <div class="text-2xl font-bold text-red-600">
              {{ selectedMove.power || '-' }}
            </div>
            <div class="text-sm text-gray-500">
              威力
            </div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-blue-600">
              {{ selectedMove.accuracy || '-' }}
            </div>
            <div class="text-sm text-gray-500">
              命中
            </div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-green-600">
              {{ selectedMove.pp || '-' }}
            </div>
            <div class="text-sm text-gray-500">
              PP
            </div>
          </div>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <h4 class="font-semibold mb-2">
            效果说明
          </h4>
          <p class="text-gray-600 text-sm">
            {{ selectedMove.effect || '暂无效果说明' }}
          </p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Loading, Star, StarFilled, List, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { moveApi, typeApi } from '../services/api.js'
import { dataCache } from '../services/cache.js'

export default {
  name: 'MoveList',
  components: { Loading },
  setup() {
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)

    const loading = ref(false)
    const loadingMore = ref(false)
    const moves = ref([])
    const filteredMoves = ref([])
    const types = ref([])
    const keyword = ref('')
    const selectedType = ref(null)
    const selectedDamageClass = ref(null)
    const powerRange = ref('')
    const accuracyRange = ref('')
    const ppRange = ref('')
    const sortBy = ref('default')
    const viewMode = ref('grid')
    const showFilters = ref(false)
    const isShowFavorites = ref(false)
    const favorites = ref(new Set())
    const showDetailDialog = ref(false)
    const selectedMove = ref(null)

    const currentPage = ref(0)
    const pageSize = ref(30)
    const total = ref(0)

    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => moves.value.length)

    // 加载收藏
    const loadFavorites = () => {
      try {
        const saved = localStorage.getItem('pokemon-moves-favorites')
        if (saved) {
          favorites.value = new Set(JSON.parse(saved))
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
      }
    }

    // 保存收藏
    const saveFavorites = () => {
      try {
        localStorage.setItem('pokemon-moves-favorites', JSON.stringify([...favorites.value]))
      } catch (error) {
        console.error('保存收藏失败:', error)
      }
    }

    // 切换收藏
    const toggleFavorite = (moveId) => {
      if (favorites.value.has(moveId)) {
        favorites.value.delete(moveId)
        ElMessage.success('已取消收藏')
      } else {
        favorites.value.add(moveId)
        ElMessage.success('已添加收藏')
      }
      saveFavorites()
      applyFilters()
    }

    // 切换收藏视图
    const toggleFavorites = () => {
      isShowFavorites.value = !isShowFavorites.value
      applyFilters()
    }

    // 切换视图模式
    const toggleViewMode = () => {
      viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid'
    }

    // 显示技能详情
    const showMoveDetail = (move) => {
      selectedMove.value = move
      showDetailDialog.value = true
    }

    // 应用筛选
    const applyFilters = () => {
      let result = [...moves.value]

      // 关键字搜索
      if (keyword.value) {
        const kw = keyword.value.toLowerCase()
        result = result.filter(m =>
          m.name.toLowerCase().includes(kw) ||
          m.nameEn?.toLowerCase().includes(kw)
        )
      }

      // 属性筛选
      if (selectedType.value) {
        result = result.filter(m => m.typeId === selectedType.value)
      }

      // 伤害分类筛选
      if (selectedDamageClass.value) {
        result = result.filter(m => {
          const damageClassMap = {
            'physical': '物理',
            'special': '特殊',
            'status': '变化'
          }
          return m.damageClass === damageClassMap[selectedDamageClass.value]
        })
      }

      // 威力范围筛选
      if (powerRange.value) {
        result = result.filter(m => {
          if (powerRange.value === 'none') return m.power === null || m.power === 0
          if (powerRange.value === '151+') return m.power >= 151
          const [min, max] = powerRange.value.split('-').map(Number)
          return m.power >= min && m.power <= max
        })
      }

      // 命中范围筛选
      if (accuracyRange.value) {
        result = result.filter(m => {
          if (accuracyRange.value === '100') return m.accuracy === 100
          if (accuracyRange.value === '<70') return m.accuracy !== null && m.accuracy < 70
          const [min, max] = accuracyRange.value.split('-').map(Number)
          return m.accuracy >= min && m.accuracy <= max
        })
      }

      // PP范围筛选
      if (ppRange.value) {
        result = result.filter(m => {
          if (ppRange.value === '≤5') return m.pp <= 5
          if (ppRange.value === '>30') return m.pp > 30
          const [min, max] = ppRange.value.split('-').map(Number)
          return m.pp >= min && m.pp <= max
        })
      }

      // 收藏筛选
      if (isShowFavorites.value) {
        result = result.filter(m => favorites.value.has(m.id))
      }

      // 排序
      if (sortBy.value !== 'default') {
        const [field, order] = sortBy.value.split('-')
        result.sort((a, b) => {
          let valA, valB
          if (field === 'name') {
            valA = a.name
            valB = b.name
          } else {
            valA = a[field] || 0
            valB = b[field] || 0
          }
          if (order === 'asc') return valA > valB ? 1 : -1
          return valA < valB ? 1 : -1
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
      showMoveDetail,
      Star,
      StarFilled,
      List,
      Grid
    }
  }
}
</script>

<style scoped>
.move-list {
  padding: 20px;
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

/* 移动端优化 */
@media (max-width: 640px) {
  .move-list {
    padding: 10px;
  }

  .grid-cols-3 {
    grid-template-columns: 1fr !important;
  }
}
</style>