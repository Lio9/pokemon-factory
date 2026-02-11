<template>
  <div class="item-list">
    <!-- 搜索区域 -->
    <div class="search-section mb-6 bg-gray-50 rounded-xl p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索物品名称..."
            prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <Search class="w-4 h-4" />
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="w-48">
          <el-select
            v-model="selectedCategory"
            placeholder="选择分类"
            clearable
            @change="handleCategoryChange"
          >
            <el-option
              label="药水"
              value="药水"
            />
            <el-option
              label="宝石"
              value="宝石"
            />
            <el-option
              label="其他"
              value="其他"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-blue-600">
            {{ totalItems }}
          </div>
          <div class="text-gray-600 text-sm">
            总物品数
          </div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-green-600">
            {{ totalPages }}
          </div>
          <div class="text-gray-600 text-sm">
            总页数
          </div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-purple-600">
            {{ currentPage }}
          </div>
          <div class="text-gray-600 text-sm">
            当前页
          </div>
        </div>
      </div>
    </div>

    <!-- 物品列表 -->
    <div
      v-if="loading"
      class="text-center py-12"
    >
      <el-skeleton
        :rows="5"
        animated
      />
    </div>
    
    <div v-else-if="items.length > 0">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div
          v-for="item in items"
          :key="item.id"
          class="item-card bg-white rounded-xl border border-gray-200 hover:border-purple-300 hover:shadow-lg transition-all duration-200 p-4"
        >
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0">
              <div class="w-16 h-16 bg-gradient-to-br from-purple-100 to-pink-100 rounded-lg flex items-center justify-center text-lg font-bold text-purple-600 border-2 border-dashed border-purple-200">
                {{ item.name.charAt(0) }}
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-lg font-semibold text-gray-900 truncate">
                {{ item.name }}
              </h3>
              <div class="flex items-center gap-2 mt-1">
                <span class="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">{{ item.category }}</span>
              </div>
              <p class="text-gray-600 text-sm mt-2 line-clamp-2">
                {{ item.description || '暂无描述' }}
              </p>
            </div>
          </div>
          <div class="mt-4 flex justify-between items-center">
            <div class="text-sm text-gray-500">
              #{{
                item.id
              }}
            </div>
            <el-button
              type="primary"
              size="small"
              @click="viewDetails(item)"
            >
              查看详情
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div
      v-else
      class="text-center py-12"
    >
      <div class="text-gray-400 mb-4">
        <svg
          class="w-16 h-16 mx-auto"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          />
        </svg>
      </div>
      <p class="text-gray-500 text-lg">
        没有找到相关物品
      </p>
      <p class="text-gray-400 text-sm mt-2">
        试试其他搜索条件
      </p>
    </div>

    <!-- 分页 -->
    <div class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48, 96]"
        :total="totalItems"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 物品详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="selectedItem?.name"
      width="50%"
    >
      <div v-if="selectedItem">
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="中文名">
            {{ selectedItem.name }}
          </el-descriptions-item>
          <el-descriptions-item label="英文名">
            {{ selectedItem.nameEn }}
          </el-descriptions-item>
          <el-descriptions-item label="日文名">
            {{ selectedItem.nameJp }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            {{ selectedItem.category }}
          </el-descriptions-item>
          <el-descriptions-item label="价格">
            {{ selectedItem.price }}
          </el-descriptions-item>
          <el-descriptions-item label="效果">
            {{ selectedItem.effect }}
          </el-descriptions-item>
          <el-descriptions-item
            label="描述"
            :span="2"
          >
            {{ selectedItem.description }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { itemApi } from '../services/api.js'

export default {
  name: 'ItemList',
  components: {
    Search
  },
  setup() {
    const loading = ref(false)
    const items = ref([])
    const searchKeyword = ref('')
    const selectedCategory = ref('')
    const currentPage = ref(1)
    const pageSize = ref(12)
    const totalItems = ref(0)
    const totalPages = ref(0)
    const dialogVisible = ref(false)
    const selectedItem = ref(null)

    const fetchItems = async () => {
      loading.value = true
      try {
        let result
        if (searchKeyword.value) {
          result = await itemApi.search(searchKeyword.value, currentPage.value, pageSize.value)
        } else {
          result = await itemApi.getList({
            current: currentPage.value,
            size: pageSize.value,
            category: selectedCategory.value || undefined
          })
        }

        if (result.code === 200) {
          items.value = result.data.records || result.data
          totalItems.value = result.data.total || items.value.length
          totalPages.value = Math.ceil(totalItems.value / pageSize.value)
        } else {
          ElMessage.error(result.message || '获取数据失败')
        }
      } catch (error) {
        console.error('获取物品列表失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchItems()
    }

    const handleCategoryChange = () => {
      currentPage.value = 1
      fetchItems()
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      fetchItems()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchItems()
    }

    const viewDetails = async (item) => {
      try {
        const result = await itemApi.getDetail(item.id)
        if (result.code === 200) {
          selectedItem.value = result.data
          dialogVisible.value = true
        } else {
          ElMessage.error('获取详情失败')
        }
      } catch (error) {
        console.error('获取物品详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    onMounted(() => {
      fetchItems()
    })

    return {
      loading,
      items,
      searchKeyword,
      selectedCategory,
      currentPage,
      pageSize,
      totalItems,
      totalPages,
      dialogVisible,
      selectedItem,
      handleSearch,
      handleCategoryChange,
      handleSizeChange,
      handleCurrentChange,
      viewDetails
    }
  }
}
</script>

<style scoped>
.item-card {
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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>