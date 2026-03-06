<template>
  <div class="ability-list">
    <!-- 搜索 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6">
      <el-input
        v-model="keyword"
        placeholder="搜索特性名称..."
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
import { abilityApi } from '../services/api.js'

export default {
  name: 'AbilityList',
  setup() {
    const loading = ref(false)
    const abilities = ref([])
    const keyword = ref('')
    const currentPage = ref(1)
    const pageSize = ref(20)
    const total = ref(0)

    const fetchAbilities = async () => {
      loading.value = true
      try {
        const result = await abilityApi.getList({
          current: currentPage.value,
          size: pageSize.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          abilities.value = result.data.records || []
          total.value = result.data.total || 0
        }
      } catch (error) {
        console.error('获取特性列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchAbilities()
    }

    const handlePageChange = () => {
      fetchAbilities()
    }

    onMounted(() => fetchAbilities())

    return {
      loading,
      abilities,
      keyword,
      currentPage,
      pageSize,
      total,
      handleSearch,
      handlePageChange
    }
  }
}
</script>
