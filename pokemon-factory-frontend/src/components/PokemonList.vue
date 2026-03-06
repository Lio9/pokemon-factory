<template>
  <div class="pokemon-list">
    <!-- 搜索和筛选区域 -->
    <div class="search-section mb-6 bg-white rounded-xl shadow-sm p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <!-- 搜索框 -->
        <div class="flex-1">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索宝可梦名称、编号..."
            prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <Search class="w-4 h-4" />
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 属性筛选 -->
        <div class="w-40">
          <el-select
            v-model="selectedType"
            placeholder="属性筛选"
            clearable
            @change="handleFilter"
          >
            <el-option
              v-for="type in types"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            >
              <div class="flex items-center gap-2">
                <span 
                  class="w-3 h-3 rounded-full"
                  :style="{ backgroundColor: type.color }"
                />
                {{ type.name }}
              </div>
            </el-option>
          </el-select>
        </div>
        
        <!-- 世代筛选 -->
        <div class="w-32">
          <el-select
            v-model="selectedGeneration"
            placeholder="世代"
            clearable
            @change="handleFilter"
          >
            <el-option
              v-for="gen in generations"
              :key="gen.id"
              :label="gen.name"
              :value="gen.id"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section mb-6">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-4 text-white">
          <div class="text-3xl font-bold">{{ total }}</div>
          <div class="text-blue-100 text-sm">总数</div>
        </div>
        <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-4 text-white">
          <div class="text-3xl font-bold">{{ totalPages }}</div>
          <div class="text-green-100 text-sm">页数</div>
        </div>
        <div class="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-4 text-white">
          <div class="text-3xl font-bold">{{ currentPage }}</div>
          <div class="text-purple-100 text-sm">当前页</div>
        </div>
        <div class="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-4 text-white">
          <div class="text-3xl font-bold">{{ pageSize }}</div>
          <div class="text-orange-100 text-sm">每页</div>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="text-center py-12">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 宝可梦列表 -->
    <div v-else-if="pokemons.length > 0">
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card bg-white rounded-xl shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer overflow-hidden group"
        >
          <!-- 图片区域 -->
          <div class="relative bg-gradient-to-br from-gray-50 to-gray-100 p-4">
            <div class="aspect-square flex items-center justify-center">
              <img 
                :src="getPokemonImage(pokemon)"
                :alt="pokemon.name"
                class="w-full h-full object-contain group-hover:scale-110 transition-transform duration-300"
                @error="handleImageError"
                loading="lazy"
              >
            </div>
            <!-- 图鉴编号 -->
            <div class="absolute top-2 left-2 bg-black/50 text-white text-xs px-2 py-0.5 rounded-full">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
            <!-- 特殊标记 -->
            <div v-if="pokemon.isLegendary" class="absolute top-2 right-2">
              <span class="text-yellow-500 text-lg">★</span>
            </div>
            <div v-else-if="pokemon.isMythical" class="absolute top-2 right-2">
              <span class="text-purple-500 text-lg">◆</span>
            </div>
          </div>
          
          <!-- 信息区域 -->
          <div class="p-3">
            <h3 class="font-semibold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
              {{ pokemon.name }}
            </h3>
            <p class="text-gray-500 text-xs truncate">{{ pokemon.genus }}</p>
            
            <!-- 属性标签 -->
            <div class="flex flex-wrap gap-1 mt-2">
              <span 
                v-for="type in pokemon.types"
                :key="type.id"
                class="px-2 py-0.5 rounded-full text-xs text-white"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 分页 -->
      <div class="mt-8 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[24, 48, 96]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-12">
      <div class="text-gray-300 mb-4">
        <svg class="w-20 h-20 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 15c-2.34 0-4.47-.881-6.08-2.334M15 10a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <p class="text-gray-500 text-lg">没有找到宝可梦</p>
      <p class="text-gray-400 text-sm mt-2">试试其他搜索条件</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { pokemonApi, typeApi, sprites } from '../services/api.js'

export default {
  name: 'PokemonList',
  components: { Search },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const pokemons = ref([])
    const types = ref([])
    const searchKeyword = ref('')
    const selectedType = ref(null)
    const selectedGeneration = ref(null)
    
    // 分页数据
    const currentPage = ref(1)
    const pageSize = ref(24)
    const total = ref(0)
    
    // 世代列表
    const generations = [
      { id: 1, name: '第一世代' },
      { id: 2, name: '第二世代' },
      { id: 3, name: '第三世代' },
      { id: 4, name: '第四世代' },
      { id: 5, name: '第五世代' },
      { id: 6, name: '第六世代' },
      { id: 7, name: '第七世代' },
      { id: 8, name: '第八世代' },
      { id: 9, name: '第九世代' }
    ]
    
    const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

    // 获取属性列表
    const fetchTypes = async () => {
      try {
        const result = await typeApi.getAll()
        if (result.code === 200) {
          types.value = result.data
        }
      } catch (error) {
        console.error('获取属性列表失败:', error)
      }
    }

    // 获取宝可梦列表
    const fetchPokemons = async () => {
      loading.value = true
      try {
        const result = await pokemonApi.getList({
          current: currentPage.value,
          size: pageSize.value,
          typeId: selectedType.value,
          generationId: selectedGeneration.value,
          keyword: searchKeyword.value || undefined
        })
        
        if (result.code === 200) {
          pokemons.value = result.data.records || []
          total.value = result.data.total || 0
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

    // 获取宝可梦图片
    const getPokemonImage = (pokemon) => {
      if (pokemon.spriteUrl) return pokemon.spriteUrl
      return sprites.pokemon(pokemon.id)
    }

    // 图片加载失败处理
    const handleImageError = (event) => {
      event.target.src = sprites.default
    }

    // 搜索
    const handleSearch = () => {
      currentPage.value = 1
      fetchPokemons()
    }

    // 筛选
    const handleFilter = () => {
      currentPage.value = 1
      fetchPokemons()
    }

    // 分页
    const handleSizeChange = () => {
      currentPage.value = 1
      fetchPokemons()
    }

    const handlePageChange = () => {
      fetchPokemons()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    // 初始化
    onMounted(() => {
      fetchTypes()
      fetchPokemons()
    })

    return {
      loading,
      pokemons,
      types,
      generations,
      searchKeyword,
      selectedType,
      selectedGeneration,
      currentPage,
      pageSize,
      total,
      totalPages,
      getPokemonImage,
      handleImageError,
      handleSearch,
      handleFilter,
      handleSizeChange,
      handlePageChange
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
</style>
