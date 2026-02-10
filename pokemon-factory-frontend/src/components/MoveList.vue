<template>
  <div class="move-list">
    <!-- 搜索区域 -->
    <div class="search-section mb-6 bg-gray-50 rounded-xl p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索招式名称..."
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
            <el-option label="物理" value="物理" />
            <el-option label="特殊" value="特殊" />
            <el-option label="变化" value="变化" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-blue-600">{{ totalMoves }}</div>
          <div class="text-gray-600 text-sm">总招式数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-green-600">{{ totalPages }}</div>
          <div class="text-gray-600 text-sm">总页数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-purple-600">{{ currentPage }}</div>
          <div class="text-gray-600 text-sm">当前页</div>
        </div>
      </div>
    </div>

    <!-- 招式列表 -->
    <div v-if="loading" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="moves.length > 0">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div
          v-for="move in moves"
          :key="move.id"
          class="move-card bg-white rounded-xl border border-gray-200 hover:border-green-300 hover:shadow-lg transition-all duration-200 p-4"
        >
          <div class="flex justify-between items-start mb-3">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ move.name }}</h3>
              <div class="flex items-center gap-2 mt-1">
                <span class="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">{{ move.type }}</span>
                <span class="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">{{ move.category }}</span>
              </div>
            </div>
            <div class="text-right">
              <div class="text-sm text-gray-500">#{{
                move.id
              }}</div>
            </div>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <div class="text-sm text-gray-500">威力: {{ move.power }}</div>
              <div class="text-sm text-gray-500">命中: {{ move.accuracy }}</div>
              <div class="text-sm text-gray-500">PP: {{ move.pp }}</div>
            </div>
            <div>
              <el-button type="primary" @click="viewDetails(move)">查看详情</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-600">没有找到相关招式</p>
    </div>

    <!-- 分页 -->
    <div class="pagination-section mt-6">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalMoves"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 招式详情对话框 -->
    <el-dialog :title="selectedMove?.name" v-model="dialogVisible" width="50%">
      <div v-if="selectedMove">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="中文名">{{ selectedMove.name }}</el-descriptions-item>
          <el-descriptions-item label="英文名">{{ selectedMove.nameEn }}</el-descriptions-item>
          <el-descriptions-item label="日文名">{{ selectedMove.nameJp }}</el-descriptions-item>
          <el-descriptions-item label="属性">{{ selectedMove.type }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ selectedMove.category }}</el-descriptions-item>
          <el-descriptions-item label="威力">{{ selectedMove.power }}</el-descriptions-item>
          <el-descriptions-item label="命中">{{ selectedMove.accuracy }}</el-descriptions-item>
          <el-descriptions-item label="PP">{{ selectedMove.pp }}</el-descriptions-item>
          <el-descriptions-item label="优先度">{{ selectedMove.priority }}</el-descriptions-item>
          <el-descriptions-item label="击中要害概率">{{ selectedMove.critRate }}</el-descriptions-item>
          <el-descriptions-item label="作用目标">{{ selectedMove.target }}</el-descriptions-item>
          <el-descriptions-item label="华丽大赛属性">{{ selectedMove.contestType }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ selectedMove.description }}</el-descriptions-item>
          <el-descriptions-item label="效果" :span="2">{{ selectedMove.effect }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { moveApi } from '../services/api.js'

export default {
  name: 'MoveList',
  components: {
    Search
  },
  setup() {
    const loading = ref(false)
    const moves = ref([])
    const searchKeyword = ref('')
    const selectedCategory = ref('')
    const currentPage = ref(1)
    const pageSize = ref(20)
    const totalMoves = ref(0)
    const totalPages = ref(0)
    const dialogVisible = ref(false)
    const selectedMove = ref(null)

    const fetchMoveList = async () => {
      loading.value = true
      try {
        let result
        if (searchKeyword.value) {
          result = await moveApi.search(searchKeyword.value, currentPage.value, pageSize.value)
        } else {
          result = await moveApi.getList({
            current: currentPage.value,
            size: pageSize.value,
            category: selectedCategory.value || undefined
          })
        }

        if (result.code === 200) {
          moves.value = result.data.records || result.data
          totalMoves.value = result.data.total || moves.value.length
          totalPages.value = Math.ceil(totalMoves.value / pageSize.value)
        } else {
          ElMessage.error(result.message || '获取数据失败')
        }
      } catch (error) {
        console.error('获取招式列表失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchMoveList()
    }

    const handleCategoryChange = () => {
      currentPage.value = 1
      fetchMoveList()
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      fetchMoveList()
    }

    const viewDetails = async (move) => {
      try {
        const result = await moveApi.getDetail(move.id)
        if (result.code === 200) {
          selectedMove.value = result.data
          dialogVisible.value = true
        } else {
          ElMessage.error('获取详情失败')
        }
      } catch (error) {
        console.error('获取招式详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    onMounted(() => {
      fetchMoveList()
    })

    return {
      loading,
      moves,
      searchKeyword,
      selectedCategory,
      currentPage,
      pageSize,
      totalMoves,
      totalPages,
      dialogVisible,
      selectedMove,
      handleSearch,
      handleCategoryChange,
      handlePageChange,
      viewDetails
    }
  }
}
</script>

<style scoped>
.move-list {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-box {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>