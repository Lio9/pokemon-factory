<template>
  <div class="ability-list" ref="listContainer">
    <!-- 搜索 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6 sticky top-0 z-10">
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

    <!-- 列表 -->
    <div v-if="loading && abilities.length === 0" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="abilities.length" class="grid md:grid-cols-2 gap-4">
      <div 
        v-for="ability in abilities" 
        :key="ability.id"
        class="bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow"
      >
        <div class="flex items-start justify-between mb-2">
          <h3 class="font-semibold text-gray-900">{{ ability.name }}</h3>
          <span class="text-xs text-gray-400">{{ ability.nameEn }}</span>
        </div>
        <p class="text-gray-600 text-sm">{{ ability.description || '暂无描述' }}</p>
      </div>
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      没有找到特性
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
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
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
    const keyword = ref('')
    const currentPage = ref(0)
    const pageSize = ref(20)
    const total = ref(0)
    
    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => abilities.value.length)

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

    return {
      listContainer,
      loadMoreTrigger,
      loading,
      loadingMore,
      abilities,
      keyword,
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