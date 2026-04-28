<!--
  ItemList 文件说明
  所属模块：前端应用。
  文件类型：页面视图文件。
  核心职责：负责页面级状态编排、接口调用结果承接以及子组件协同展示。
  阅读建议：建议优先关注页面状态来源、事件分发与子组件依赖关系。
  项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
-->

<template>
  <div
    ref="listContainer"
    class="item-list"
  >
    <!-- 搜索 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6 sticky top-0 z-10">
      <el-input
        v-model="keyword"
        placeholder="搜索物品名称..."
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

    <!-- 统计信息 -->
    <div class="grid grid-cols-2 gap-4 mb-6">
      <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">
          {{ total }}
        </div>
        <div class="text-purple-100 text-sm">
          总数
        </div>
      </div>
      <div class="bg-gradient-to-br from-pink-500 to-pink-600 rounded-xl p-4 text-white">
        <div class="text-2xl font-bold">
          {{ loadedCount }}
        </div>
        <div class="text-pink-100 text-sm">
          已加载
        </div>
      </div>
    </div>

    <!-- 列表 -->
    <div
      v-if="loading && items.length === 0"
      class="text-center py-12"
    >
      <el-skeleton
        :rows="5"
        animated
      />
    </div>

    <div
      v-else-if="items.length"
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4"
    >
      <div 
        v-for="item in items" 
        :key="item.id"
        class="bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow group"
      >
        <div class="aspect-square flex items-center justify-center mb-2">
          <!-- 懒加载占位 -->
          <div 
            v-if="!item._imageLoaded" 
            class="w-16 h-16 flex items-center justify-center"
          >
            <div class="w-10 h-10 rounded-full bg-gray-200 animate-pulse" />
          </div>
          <img 
            v-show="item._imageLoaded"
            :src="item._imageUrl"
            :alt="item.name"
            class="w-16 h-16 object-contain group-hover:scale-110 transition-transform"
            loading="lazy"
            @load="item._imageLoaded = true"
            @error="handleImageError(item)"
          >
        </div>
        <h3 class="font-medium text-gray-900 text-sm truncate">
          {{ item.name }}
        </h3>
        <p class="text-xs text-gray-500">
          ¥{{ item.cost || '-' }}
        </p>
        <p class="text-xs text-gray-400 mt-1 line-clamp-2">
          {{ item.description || '暂无描述' }}
        </p>
      </div>
    </div>

    <div
      v-else
      class="text-center py-12 text-gray-500"
    >
      没有找到物品
    </div>

    <!-- 加载更多 -->
    <div
      ref="loadMoreTrigger"
      class="text-center py-6"
    >
      <div v-if="loadingMore">
        <el-icon class="is-loading text-2xl text-purple-500">
          <Loading />
        </el-icon>
        <span class="text-gray-500 ml-2">加载中...</span>
      </div>
      <div
        v-else-if="!hasMore"
        class="text-gray-400"
      >
        已加载全部 {{ total }} 个物品
      </div>
      <div
        v-else
        class="text-gray-400"
      >
        下拉加载更多...
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { itemApi, sprites } from '../services/api.js'

export default {
  name: 'ItemList',
  components: { Loading },
  setup() {
    const listContainer = ref(null)
    const loadMoreTrigger = ref(null)
    
    const loading = ref(false)
    const loadingMore = ref(false)
    const items = ref([])
    const keyword = ref('')
    const currentPage = ref(0)
    const pageSize = ref(48)
    const total = ref(0)
    
    let searchTimer = null
    let observer = null

    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
    const hasMore = computed(() => currentPage.value < totalPages.value)
    const loadedCount = computed(() => items.value.length)

    const processItemData = (data) => {
      return data.map(item => ({
        ...item,
        _imageUrl: item.spriteUrl || getItemImage(item),
        _imageLoaded: false
      }))
    }

    const getItemImage = (item) => {
      if (item.nameEn) {
        return sprites.item(item.nameEn.toLowerCase().replace(/[^a-z0-9]/g, '-'))
      }
      return sprites.default
    }

    const handleImageError = (item) => {
      item._imageLoaded = true
      item._imageUrl = sprites.default
    }

    const fetchItems = async (isLoadMore = false) => {
      if (loading.value || loadingMore.value) return
      if (isLoadMore && !hasMore.value) return

      if (isLoadMore) {
        loadingMore.value = true
      } else {
        loading.value = true
        currentPage.value = 0
        items.value = []
      }

      try {
        const nextPage = currentPage.value + 1
        const result = await itemApi.getList({
          current: nextPage,
          size: pageSize.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          const processedData = processItemData(result.data.records || [])
          items.value = [...items.value, ...processedData]
          total.value = result.data.total || 0
          currentPage.value = nextPage
        }
      } catch (error) {
        console.error('获取物品列表失败:', error)
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
      fetchItems(false)
    }

    const setupObserver = () => {
      if (observer) observer.disconnect()
      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
              fetchItems(true)
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
      fetchItems(false)
      nextTick(() => setupObserver())
    })

    onUnmounted(() => {
      if (observer) observer.disconnect()
      if (searchTimer) clearTimeout(searchTimer)
    })

    watch(() => items.value.length, () => {
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
      items,
      keyword,
      currentPage,
      pageSize,
      total,
      hasMore,
      loadedCount,
      handleImageError,
      handleSearchInput,
      handleSearch
    }
  }
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>