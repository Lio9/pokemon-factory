<template>
  <div class="pokemon-list">
    <!-- 搜索和筛选区域 -->
    <div class="search-section mb-6 bg-gray-50 rounded-xl p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索宝可梦名称、编号..."
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
            v-model="selectedType"
            placeholder="选择属性"
            clearable
            @change="handleTypeChange"
          >
            <el-option
              v-for="type in types"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-6">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-blue-600">{{ totalPokemons }}</div>
          <div class="text-gray-600 text-sm">总宝可梦数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-green-600">{{ totalPages }}</div>
          <div class="text-gray-600 text-sm">总页数</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-purple-600">{{ currentPage }}</div>
          <div class="text-gray-600 text-sm">当前页</div>
        </div>
        <div class="bg-white rounded-lg border border-gray-200 p-4 text-center">
          <div class="text-2xl font-bold text-orange-600">{{ pageSize }}</div>
          <div class="text-gray-600 text-sm">每页显示</div>
        </div>
      </div>
    </div>

    <!-- 宝可梦列表 -->
    <div v-if="loading" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="pokemons.length > 0">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card bg-white rounded-xl border border-gray-200 hover:border-blue-300 hover:shadow-lg transition-all duration-200 cursor-pointer overflow-hidden group"
        >
          <div class="p-4">
            <div class="flex items-start gap-3">
              <div class="flex-shrink-0">
                <div class="w-16 h-16 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-lg flex items-center justify-center text-lg font-bold text-blue-600 border-2 border-dashed border-blue-200 group-hover:scale-110 transition-transform overflow-hidden">
                  <img 
                    :src="`/images/home/${pokemon.indexNumber}.png`" 
                    :alt="pokemon.name"
                    class="w-full h-full object-cover"
                    @error="handleImageError"
                  />
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
                  {{ pokemon.name }}
                </h3>
                <p class="text-gray-500 text-sm truncate">{{ pokemon.genus }}</p>
                <p class="text-gray-600 text-xs mt-1 line-clamp-2">{{ pokemon.profile || '暂无描述' }}</p>
              </div>
            </div>
          </div>
          <div class="px-4 pb-4">
            <div class="flex items-center justify-between text-xs text-gray-500">
              <span>点击查看详情</span>
              <ChevronRight class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        </router-link>
      </div>

      <!-- 分页 -->
      <div class="mt-8 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 48, 96]"
          :total="totalPokemons"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
        />
      </div>
    </div>

    <div v-else class="text-center py-12">
      <div class="text-gray-400 mb-4">
        <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 15c-2.34 0-4.47-.881-6.08-2.334M15 10a3 3 0 11-6 0 3 3 0 016 0z"></path>
        </svg>
      </div>
      <p class="text-gray-500 text-lg">没有找到相关宝可梦</p>
      <p class="text-gray-400 text-sm mt-2">试试其他搜索条件</p>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="currentPokemon?.name || '宝可梦详情'"
      width="800px"
      destroy-on-close
    >
      <PokemonDetail 
        :pokemon="currentPokemon" 
        @close="showDetailDialog = false"
      />
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ChevronRight } from 'lucide-vue-next'
import PokemonDetail from './PokemonDetail.vue'
import { useRoute, useRouter } from 'vue-router'
import { pokemonApi } from '../services/api.js'

export default {
  name: 'PokemonList',
  components: {
    Search,
    ChevronRight,
    PokemonDetail
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const pokemons = ref([])
    const types = ref([])
    const searchKeyword = ref('')
    const selectedType = ref('')
    
    // 分页数据
    const currentPage = ref(1)
    const pageSize = ref(12)
    const totalPokemons = ref(0)
    const totalPages = ref(0)
    
    // 详情弹窗
    const showDetailDialog = ref(false)
    const currentPokemon = ref(null)

    // 获取宝可梦列表
    const fetchPokemons = async () => {
      loading.value = true
      try {
        const params = {
          current: currentPage.value,
          size: pageSize.value,
          name: searchKeyword.value || undefined,
          typeId: selectedType.value || undefined
        }
        
        const result = await pokemonApi.getList(params)
        
        if (result.code === 200) {
          pokemons.value = result.data.records
          totalPokemons.value = result.data.total
          totalPages.value = result.data.pages
        } else {
          ElMessage.error(result.message || '获取数据失败')
        }
      } catch (error) {
        console.error('获取宝可梦列表失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    // 获取属性列表
    const fetchTypes = async () => {
      try {
        const result = await pokemonApi.getTypeStatistics()
        
        if (result.code === 200) {
          types.value = result.data.types || []
        }
      } catch (error) {
        console.error('获取属性列表失败:', error)
      }
    }

    // 显示详情
    const showDetail = async (id) => {
      try {
        const result = await pokemonApi.getDetail(id)
        
        if (result.code === 200) {
          currentPokemon.value = result.data
          showDetailDialog.value = true
        } else {
          ElMessage.error(result.message || '获取详情失败')
        }
      } catch (error) {
        console.error('获取宝可梦详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    // 搜索处理
    const handleSearch = () => {
      currentPage.value = 1
      fetchPokemons()
    }

    // 类型筛选处理
    const handleTypeChange = () => {
      currentPage.value = 1
      fetchPokemons()
    }

    // 分页处理
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      fetchPokemons()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchPokemons()
    }

    const handleImageError = (event) => {
      // 图片加载失败时显示默认占位符
      event.target.style.display = 'none'
    }

    // 获取路由和路由器实例
    const route = useRoute()
    const router = useRouter()

    // 监听路由变化，如果路由参数变化则刷新数据
    watch(() => route.params.id, (newId) => {
      if (newId) {
        showDetail(newId)
      }
    }, { immediate: true })

    // 组件挂载时获取数据
    onMounted(() => {
      fetchPokemons()
      fetchTypes()
    })

    return {
      // 数据
      loading,
      pokemons,
      types,
      searchKeyword,
      selectedType,
      currentPage,
      pageSize,
      totalPokemons,
      totalPages,
      showDetailDialog,
      currentPokemon,
      
      // 方法
      handleSearch,
      handleTypeChange,
      handleSizeChange,
      handleCurrentChange,
      showDetail,
      handleImageError
    }
  }
}
</script>

<style scoped>
.pokemon-card {
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