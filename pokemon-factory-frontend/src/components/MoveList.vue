<template>
  <div class="move-list" ref="listContainer">
    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6 sticky top-0 z-10">
      <div class="flex gap-4">
        <div class="flex-1">
          <el-input
            v-model="keyword"
            placeholder="搜索技能名称..."
            clearable
            @input="handleSearchInput"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="w-32">
          <el-select v-model="selectedType" placeholder="属性" clearable @change="handleSearch">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">{{ total }}</div>
        <div class="text-red-100 text-sm">总数</div>
      </div>
      <div class="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">{{ loadedCount }}</div>
        <div class="text-orange-100 text-sm">已加载</div>
      </div>
      <div class="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">{{ selectedType ? types.find(t => t.id === selectedType)?.name : '全部' }}</div>
        <div class="text-yellow-100 text-sm">属性筛选</div>
      </div>
    </div>

    <!-- 列表 -->
    <div v-if="loading && moves.length === 0" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="moves.length" class="bg-white rounded-xl shadow-sm overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">技能</th>
            <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">属性</th>
            <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
            <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">威力</th>
            <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">命中</th>
            <th class="py-3 px-4 text-center text-xs font-medium text-gray-500 uppercase">PP</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="move in moves" :key="move.id" class="hover:bg-gray-50">
            <td class="py-3 px-4">
              <div class="font-medium text-gray-900">{{ move.name }}</div>
              <div class="text-xs text-gray-400">{{ move.nameEn }}</div>
            </td>
            <td class="py-3 px-4">
              <span 
                class="px-2 py-1 rounded text-white text-xs"
                :style="{ backgroundColor: move.typeColor }"
              >
                {{ move.typeName }}
              </span>
            </td>
            <td class="py-3 px-4 text-sm text-gray-500">{{ move.damageClass }}</td>
            <td class="py-3 px-4 text-center font-medium">{{ move.power || '-' }}</td>
            <td class="py-3 px-4 text-center">{{ move.accuracy || '-' }}</td>
            <td class="py-3 px-4 text-center">{{ move.pp || '-' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 加载更多 -->
      <div ref="loadMoreTrigger" class="text-center py-6">
        <div v-if="loadingMore">
          <el-icon class="is-loading text-2xl text-blue-500"><Loading /></el-icon>
          <span class="text-gray-500 ml-2">加载中...</span>
        </div>
        <div v-else-if="!hasMore" class="text-gray-400">
          已加载全部 {{ total }} 个技能
        </div>
        <div v-else class="text-gray-400">
          下拉加载更多...
        </div>
      </div>
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      没有找到技能
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
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
    const types = ref([])
    const keyword = ref('')
    const selectedType = ref(null)
    const currentPage = ref(0)
    const pageSize = ref(30)
    const total = ref(0)
    
    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => moves.value.length)

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

    return {
      listContainer,
      loadMoreTrigger,
      loading,
      loadingMore,
      moves,
      types,
      keyword,
      selectedType,
      currentPage,
      pageSize,
      total,
      hasMore,
      loadedCount,
      handleSearchInput,
      handleSearch
    }
  }
}
</script>