<template>
  <div class="move-list">
    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6">
      <div class="flex gap-4">
        <div class="flex-1">
          <el-input
            v-model="keyword"
            placeholder="搜索技能名称..."
            clearable
            @keyup.enter="handleSearch"
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

    <!-- 列表 -->
    <div v-if="loading" class="text-center py-12">
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
    </div>

    <div v-else class="text-center py-12 text-gray-500">
      没有找到技能
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
import { moveApi, typeApi } from '../services/api.js'

export default {
  name: 'MoveList',
  setup() {
    const loading = ref(false)
    const moves = ref([])
    const types = ref([])
    const keyword = ref('')
    const selectedType = ref(null)
    const currentPage = ref(1)
    const pageSize = ref(30)
    const total = ref(0)

    const fetchTypes = async () => {
      try {
        const result = await typeApi.getAll()
        if (result.code === 200) {
          types.value = result.data
        }
      } catch (error) {
        console.error('获取属性失败:', error)
      }
    }

    const fetchMoves = async () => {
      loading.value = true
      try {
        const result = await moveApi.getList({
          current: currentPage.value,
          size: pageSize.value,
          typeId: selectedType.value,
          keyword: keyword.value || undefined
        })
        if (result.code === 200) {
          moves.value = result.data.records || []
          total.value = result.data.total || 0
        }
      } catch (error) {
        console.error('获取技能列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchMoves()
    }

    const handlePageChange = () => {
      fetchMoves()
    }

    onMounted(() => {
      fetchTypes()
      fetchMoves()
    })

    return {
      loading,
      moves,
      types,
      keyword,
      selectedType,
      currentPage,
      pageSize,
      total,
      handleSearch,
      handlePageChange
    }
  }
}
</script>
