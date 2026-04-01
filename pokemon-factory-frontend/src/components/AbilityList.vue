<template>
  <div class="ability-list" ref="listContainer">
    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6 sticky top-0 z-10">
      <div class="flex flex-col gap-4">
        <!-- 搜索和基本操作 -->
        <div class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-[200px]">
            <el-input
              v-model="keyword"
              placeholder="搜索特性名称..."
              clearable
              @input="handleSearchInput"
              @clear="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>
          <el-button
            :icon="viewMode === 'grid' ? 'List' : 'Grid'"
            @click="toggleViewMode"
            :type="viewMode === 'grid' ? 'primary' : 'default'"
          >
            {{ viewMode === 'grid' ? '列表' : '网格' }}
          </el-button>
          <el-button
            :icon="isShowFavorites ? 'StarFilled' : 'Star'"
            @click="toggleFavorites"
            :type="isShowFavorites ? 'warning' : 'default'"
          >
            {{ isShowFavorites ? '全部' : `收藏 (${favorites.size})` }}
          </el-button>
        </div>

        <!-- 高级筛选面板 -->
        <el-collapse-transition>
          <div v-if="showFilters" class="flex flex-wrap gap-3 items-end">
            <div class="flex-1 min-w-[200px]">
              <label class="text-xs text-gray-500 mb-1 block">代数筛选</label>
              <el-select v-model="selectedGeneration" placeholder="世代" clearable @change="applyFilters">
                <el-option label="第一世代" value="1" />
                <el-option label="第二世代" value="2" />
                <el-option label="第三世代" value="3" />
                <el-option label="第四世代" value="4" />
                <el-option label="第五世代" value="5" />
                <el-option label="第六世代" value="6" />
                <el-option label="第七世代" value="7" />
                <el-option label="第八世代" value="8" />
                <el-option label="第九世代" value="9" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">描述长度</label>
              <el-select v-model="descriptionLength" placeholder="描述长度" clearable @change="applyFilters">
                <el-option label="简短 (<50字)" value="short" />
                <el-option label="中等 (50-100字)" value="medium" />
                <el-option label="详细 (>100字)" value="long" />
              </el-select>
            </div>
            <div class="flex-1 min-w-[150px]">
              <label class="text-xs text-gray-500 mb-1 block">排序</label>
              <el-select v-model="sortBy" @change="handleSort">
                <el-option label="默认" value="default" />
                <el-option label="名称 A-Z" value="name-asc" />
                <el-option label="名称 Z-A" value="name-desc" />
                <el-option label="ID 升序" value="id-asc" />
                <el-option label="ID 降序" value="id-desc" />
              </el-select>
            </div>
          </div>
        </el-collapse-transition>

        <!-- 展开/收起筛选按钮 -->
        <div class="flex justify-center">
          <el-button link @click="showFilters = !showFilters" type="primary">
            {{ showFilters ? '收起筛选' : '展开筛选 ▼' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="grid grid-cols-2 gap-4 mb-6">
      <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">{{ total }}</div>
        <div class="text-green-100 text-sm">总数</div>
      </div>
      <div class="bg-gradient-to-br from-teal-500 to-teal-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">{{ loadedCount }}</div>
        <div class="text-teal-100 text-sm">已加载</div>
      </div>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading && abilities.length === 0">
      <div v-if="viewMode === 'grid'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <el-skeleton v-for="i in 6" :key="i" animated>
          <template #template>
            <el-card class="mb-4">
              <el-skeleton-item variant="h3" style="width: 50%" />
              <el-skeleton-item variant="text" style="width: 70%" />
              <el-skeleton-item variant="rect" style="width: 100%; height: 40px; margin-top: 10px" />
            </el-card>
          </template>
        </el-skeleton>
      </div>
      <div v-else class="bg-white rounded-xl shadow-sm overflow-hidden">
        <el-skeleton :rows="5" animated />
      </div>
    </div>

    <!-- 网格视图 -->
    <div v-else-if="abilities.length && viewMode === 'grid'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="ability in abilities"
        :key="ability.id"
        class="bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow cursor-pointer group"
        @click="showAbilityDetail(ability)"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-2">
            <h3 class="font-semibold text-gray-900 group-hover:text-green-600 transition-colors">
              {{ ability.name }}
            </h3>
            <span class="text-xs text-gray-400">#{{ ability.id }}</span>
          </div>
          <el-button
            :icon="favorites.has(ability.id) ? 'StarFilled' : 'Star'"
            :type="favorites.has(ability.id) ? 'warning' : 'default'"
            text
            size="small"
            @click.stop="toggleFavorite(ability.id)"
          />
        </div>
        <div class="flex items-center gap-2 mb-3">
          <span class="text-xs text-gray-400">{{ ability.nameEn }}</span>
          <span v-if="ability.generation" class="px-2 py-0.5 rounded bg-purple-100 text-purple-700 text-xs">
            第{{ ability.generation }}世代
          </span>
        </div>
        <p class="text-gray-600 text-sm line-clamp-3 mb-3">
          {{ ability.description || '暂无描述' }}
        </p>
        <div class="flex items-center justify-between text-xs text-gray-400">
          <span>{{ (ability.description || '').length }} 字</span>
          <span class="text-green-500">查看详情 →</span>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else-if="abilities.length" class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">特性</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">世代</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">描述</th>
              <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="ability in abilities" :key="ability.id" class="hover:bg-gray-50">
              <td class="py-3 px-4">
                <div class="font-medium text-gray-900">{{ ability.name }}</div>
                <div class="text-xs text-gray-400">{{ ability.nameEn }}</div>
              </td>
              <td class="py-3 px-4">
                <span v-if="ability.generation" class="px-2 py-1 rounded bg-purple-100 text-purple-700 text-xs">
                  第{{ ability.generation }}世代
                </span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="py-3 px-4">
                <p class="text-sm text-gray-600 line-clamp-2">{{ ability.description || '暂无描述' }}</p>
              </td>
              <td class="py-3 px-4 text-center">
                <el-button
                  :icon="favorites.has(ability.id) ? 'StarFilled' : 'Star'"
                  :type="favorites.has(ability.id) ? 'warning' : 'default'"
                  text
                  size="small"
                  @click="toggleFavorite(ability.id)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 加载更多 -->
      <div ref="loadMoreTrigger" class="text-center py-6">
        <div v-if="loadingMore">
          <el-icon class="is-loading text-2xl text-green-500"><Loading /></el-icon>
          <span class="text-gray-500 ml-2">加载中...</span>
        </div>
        <div v-else-if="!hasMore" class="text-gray-400">
          已加载全部 {{ total }} 个特性
        </div>
        <div v-else class="text-gray-400">
          下拉加载更多...
        </div>
      </div>
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      没有找到特性
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="selectedAbility?.name"
      width="600px"
    >
      <div v-if="selectedAbility">
        <div class="mb-4">
          <span class="text-gray-500 text-sm">{{ selectedAbility.nameEn }}</span>
          <span v-if="selectedAbility.generation" class="ml-3 px-2 py-1 rounded bg-purple-100 text-purple-700 text-xs">
            第{{ selectedAbility.generation }}世代
          </span>
        </div>
        <div class="bg-gray-50 rounded-lg p-4 mb-4">
          <h4 class="font-semibold mb-2">效果说明</h4>
          <p class="text-gray-600 text-sm leading-relaxed">
            {{ selectedAbility.description || '暂无描述' }}
          </p>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-blue-50 rounded-lg p-3 text-center">
            <div class="text-lg font-bold text-blue-600">#{{ selectedAbility.id }}</div>
            <div class="text-sm text-blue-400">ID</div>
          </div>
          <div class="bg-green-50 rounded-lg p-3 text-center">
            <div class="text-lg font-bold text-green-600">{{ (selectedAbility.description || '').length }}</div>
            <div class="text-sm text-green-400">描述字数</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Loading, Star, StarFilled, List, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { abilityApi } from '../services/api.js'

export default {
  name: 'AbilityList',
  components: { Loading },
  setup() {
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)

    const loading = ref(false)
    const loadingMore = ref(false)
    const abilities = ref([])
    const filteredAbilities = ref([])
    const keyword = ref('')
    const selectedGeneration = ref(null)
    const descriptionLength = ref('')
    const sortBy = ref('default')
    const viewMode = ref('grid')
    const showFilters = ref(false)
    const isShowFavorites = ref(false)
    const favorites = ref(new Set())
    const showDetailDialog = ref(false)
    const selectedAbility = ref(null)

    const currentPage = ref(0)
    const pageSize = ref(20)
    const total = ref(0)

    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => abilities.value.length)

    // 加载收藏
    const loadFavorites = () => {
      try {
        const saved = localStorage.getItem('pokemon-abilities-favorites')
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
        localStorage.setItem('pokemon-abilities-favorites', JSON.stringify([...favorites.value]))
      } catch (error) {
        console.error('保存收藏失败:', error)
      }
    }

    // 切换收藏
    const toggleFavorite = (abilityId) => {
      if (favorites.value.has(abilityId)) {
        favorites.value.delete(abilityId)
        ElMessage.success('已取消收藏')
      } else {
        favorites.value.add(abilityId)
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

    // 显示特性详情
    const showAbilityDetail = (ability) => {
      selectedAbility.value = ability
      showDetailDialog.value = true
    }

    // 应用筛选
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
        result = result.filter(a => a.generation === parseInt(selectedGeneration.value))
      }

      // 描述长度筛选
      if (descriptionLength.value) {
        result = result.filter(a => {
          const len = (a.description || '').length
          if (descriptionLength.value === 'short') return len < 50
          if (descriptionLength.value === 'medium') return len >= 50 && len <= 100
          if (descriptionLength.value === 'long') return len > 100
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

    // 排序处理
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

    // 监听筛选条件变化
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
      showAbilityDetail,
      Star,
      StarFilled,
      List,
      Grid
    }
  }
}
</script>

<style scoped>
.ability-list {
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
  .ability-list {
    padding: 10px;
  }

  .grid-cols-3 {
    grid-template-columns: 1fr !important;
  }

  .grid-cols-2 {
    grid-template-columns: 1fr !important;
  }
}
</style>