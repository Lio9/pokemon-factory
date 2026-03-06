<template>
  <div class="item-list">
    <!-- 搜索 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6">
      <el-input
        v-model="keyword"
        placeholder="搜索物品名称..."
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="items.length" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
      <div 
        v-for="item in items" 
        :key="item.id"
        class="bg-white rounded-xl shadow-sm p-4 hover:shadow-md transition-shadow"
      >
        <div class="aspect-square flex items-center justify-center mb-2">
          <img 
            :src="item.spriteUrl || getItemImage(item)"
            :alt="item.name"
            class="w-16 h-16 object-contain"
            @error="handleImageError"
          >
        </div>
        <h3 class="font-medium text-gray-900 text-sm truncate">{{ item.name }}</h3>
        <p class="text-xs text-gray-500">¥{{ item.cost || '-' }}</p>
        <p class="text-xs text-gray-400 mt-1 line-clamp-2">{{ item.description || '暂无描述' }}</p>
      </div>
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      没有找到物品
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-6 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { itemApi, sprites } from '../services/api.js'

export default {
  name: 'ItemList',
  setup() {
    const loading = ref(false)
    const items = ref([])
    const keyword = ref('')
    const currentPage = ref(1)
    const pageSize = ref(48)
    const total = ref(0)

    const fetchItems = async () => {
      loading.value = true
      try {
        const result = await itemApi.getList({
          current: currentPage.value,
          size: pageSize.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          items.value = result.data.records || []
          total.value = result.data.total || 0
        }
      } catch (error) {
        console.error('获取物品列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    const getItemImage = (item) => {
      if (item.nameEn) {
        return sprites.item(item.nameEn.toLowerCase().replace(/[^a-z0-9]/g, '-'))
      }
      return sprites.default
    }

    const handleImageError = (event) => {
      event.target.src = sprites.default
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchItems()
    }

    const handlePageChange = () => {
      fetchItems()
    }

    onMounted(() => fetchItems())

    return {
      loading,
      items,
      keyword,
      currentPage,
      pageSize,
      total,
      getItemImage,
      handleImageError,
      handleSearch,
      handlePageChange
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
