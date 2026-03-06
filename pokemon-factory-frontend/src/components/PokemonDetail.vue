<template>
  <div v-if="pokemon" class="pokemon-detail">
    <!-- 返回按钮 -->
    <button @click="goBack" class="mb-4 flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors">
      <ChevronLeft class="w-5 h-5" />
      返回列表
    </button>

    <!-- 基本信息卡片 -->
    <div class="bg-white rounded-2xl shadow-lg overflow-hidden mb-6">
      <div class="md:flex">
        <!-- 图片区域 -->
        <div class="md:w-1/3 bg-gradient-to-br from-blue-50 to-indigo-100 p-8 flex items-center justify-center">
          <div class="relative">
            <img 
              :src="pokemon.forms?.[0]?.spriteUrl || getPokemonImage(pokemon.id)"
              :alt="pokemon.name"
              class="w-64 h-64 object-contain"
              @error="handleImageError"
            >
            <!-- 图鉴编号 -->
            <div class="absolute -top-2 -left-2 bg-blue-600 text-white text-lg font-bold px-4 py-1 rounded-full shadow-lg">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
          </div>
        </div>
        
        <!-- 信息区域 -->
        <div class="md:w-2/3 p-6">
          <div class="flex items-start justify-between mb-4">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">{{ pokemon.name }}</h1>
              <p class="text-gray-500">{{ pokemon.nameEn }} / {{ pokemon.nameJp }}</p>
              <p class="text-gray-400 text-sm mt-1">{{ pokemon.genus }}</p>
            </div>
            <div class="flex gap-2">
              <span v-if="pokemon.isLegendary" class="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">
                传说
              </span>
              <span v-if="pokemon.isMythical" class="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm font-medium">
                神话
              </span>
            </div>
          </div>
          
          <!-- 属性 -->
          <div class="mb-4">
            <span class="text-gray-500 text-sm">属性：</span>
            <div class="inline-flex gap-2 ml-2">
              <span 
                v-for="type in pokemon.forms?.[0]?.types || []"
                :key="type.id"
                class="px-3 py-1 rounded-full text-white text-sm font-medium"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>
          
          <!-- 基本信息 -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-gray-500 text-xs">身高</div>
              <div class="text-lg font-semibold">{{ pokemon.forms?.[0]?.height || '-' }}m</div>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-gray-500 text-xs">体重</div>
              <div class="text-lg font-semibold">{{ pokemon.forms?.[0]?.weight || '-' }}kg</div>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-gray-500 text-xs">捕获率</div>
              <div class="text-lg font-semibold">{{ pokemon.captureRate }}</div>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <div class="text-gray-500 text-xs">基础亲密度</div>
              <div class="text-lg font-semibold">{{ pokemon.baseHappiness }}</div>
            </div>
          </div>
          
          <!-- 描述 -->
          <p class="text-gray-600 leading-relaxed">{{ pokemon.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <!-- 种族值 -->
    <div v-if="pokemon.forms?.[0]?.stats" class="bg-white rounded-2xl shadow-lg p-6 mb-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
        <BarChart3 class="w-5 h-5 text-blue-500" />
        种族值
      </h2>
      <div class="space-y-3">
        <StatBar label="HP" :value="pokemon.forms[0].stats.hp" color="#FF5959" />
        <StatBar label="攻击" :value="pokemon.forms[0].stats.attack" color="#F5AC78" />
        <StatBar label="防御" :value="pokemon.forms[0].stats.defense" color="#FAE078" />
        <StatBar label="特攻" :value="pokemon.forms[0].stats.spAttack" color="#9DB7F5" />
        <StatBar label="特防" :value="pokemon.forms[0].stats.spDefense" color="#A7DB8D" />
        <StatBar label="速度" :value="pokemon.forms[0].stats.speed" color="#FA92B2" />
      </div>
      <div class="mt-4 text-center">
        <span class="text-2xl font-bold text-gray-900">{{ pokemon.forms[0].stats.total }}</span>
        <span class="text-gray-500 ml-2">种族值总和</span>
      </div>
    </div>

    <!-- 特性 -->
    <div v-if="pokemon.forms?.[0]?.abilities?.length" class="bg-white rounded-2xl shadow-lg p-6 mb-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
        <Sparkles class="w-5 h-5 text-green-500" />
        特性
      </h2>
      <div class="grid md:grid-cols-2 gap-4">
        <div 
          v-for="ability in pokemon.forms[0].abilities" 
          :key="ability.id"
          class="border rounded-xl p-4 hover:shadow-md transition-shadow"
          :class="ability.isHidden ? 'border-purple-200 bg-purple-50' : 'border-gray-200'"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-semibold text-gray-900">{{ ability.name }}</span>
            <span v-if="ability.isHidden" class="px-2 py-0.5 bg-purple-500 text-white text-xs rounded-full">
              隐藏
            </span>
          </div>
          <p class="text-gray-600 text-sm">{{ ability.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>

    <!-- 形态 -->
    <div v-if="pokemon.forms?.length > 1" class="bg-white rounded-2xl shadow-lg p-6 mb-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
        <Layers class="w-5 h-5 text-orange-500" />
        形态
      </h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div 
          v-for="form in pokemon.forms" 
          :key="form.id"
          class="border rounded-xl p-4 text-center cursor-pointer hover:shadow-md transition-shadow"
          :class="selectedFormId === form.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200'"
          @click="selectedFormId = form.id"
        >
          <img 
            :src="form.spriteUrl || getPokemonImage(pokemon.id)"
            :alt="form.formName"
            class="w-20 h-20 mx-auto object-contain"
          >
          <p class="mt-2 font-medium text-gray-900">{{ form.formName || '默认形态' }}</p>
        </div>
      </div>
    </div>

    <!-- 进化链 -->
    <div v-if="pokemon.evolutionChain?.length" class="bg-white rounded-2xl shadow-lg p-6 mb-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
        <GitBranch class="w-5 h-5 text-blue-500" />
        进化链
      </h2>
      <div class="flex items-center justify-center flex-wrap gap-4">
        <template v-for="(evo, index) in pokemon.evolutionChain" :key="evo.speciesId">
          <router-link 
            :to="`/pokemon/${evo.speciesId}`"
            class="flex flex-col items-center p-4 rounded-xl transition-all"
            :class="evo.isCurrent ? 'bg-blue-100 ring-2 ring-blue-500' : 'hover:bg-gray-100'"
          >
            <img 
              :src="evo.spriteUrl || getPokemonImage(evo.speciesId)"
              :alt="evo.name"
              class="w-24 h-24 object-contain"
              @error="handleImageError"
            >
            <span class="mt-2 font-medium text-gray-900">{{ evo.name }}</span>
            <span class="text-xs text-gray-500">#{{ String(evo.speciesId).padStart(4, '0') }}</span>
          </router-link>
          <div v-if="index < pokemon.evolutionChain.length - 1" class="flex flex-col items-center text-gray-400">
            <ArrowRight class="w-6 h-6" />
            <span class="text-xs mt-1">{{ evo.trigger }}</span>
            <span v-if="evo.minLevel" class="text-xs">Lv.{{ evo.minLevel }}</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 技能列表 -->
    <div class="bg-white rounded-2xl shadow-lg p-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4 flex items-center gap-2">
        <Zap class="w-5 h-5 text-red-500" />
        可学技能
      </h2>
      
      <!-- 加载中 -->
      <div v-if="loadingMoves" class="text-center py-8">
        <el-skeleton :rows="3" animated />
      </div>
      
      <!-- 技能表格 -->
      <div v-else-if="moves.length" class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200">
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">技能</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">属性</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">分类</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">威力</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">命中</th>
              <th class="py-3 px-4 text-left text-xs font-medium text-gray-500 uppercase">PP</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="move in moves" :key="move.id" class="hover:bg-gray-50">
              <td class="py-3 px-4">
                <div class="font-medium text-gray-900">{{ move.name }}</div>
                <div class="text-xs text-gray-400">{{ move.learnMethod }} {{ move.level ? `Lv.${move.level}` : '' }}</div>
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
              <td class="py-3 px-4 text-sm font-medium">{{ move.power || '-' }}</td>
              <td class="py-3 px-4 text-sm">{{ move.accuracy || '-' }}</td>
              <td class="py-3 px-4 text-sm">{{ move.pp || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-else class="text-center py-8 text-gray-500">
        暂无技能数据
      </div>
    </div>
  </div>
  
  <!-- 加载中 -->
  <div v-else class="text-center py-20">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChevronLeft, BarChart3, Sparkles, Layers, GitBranch, ArrowRight, Zap } from 'lucide-vue-next'
import { pokemonApi, sprites } from '../services/api.js'

// 种族值条组件
const StatBar = {
  props: ['label', 'value', 'color'],
  template: `
    <div class="flex items-center gap-3">
      <span class="w-12 text-sm font-medium text-gray-600">{{ label }}</span>
      <div class="flex-1 bg-gray-200 rounded-full h-3 overflow-hidden">
        <div 
          class="h-full rounded-full transition-all duration-500"
          :style="{ width: (value / 255 * 100) + '%', backgroundColor: color }"
        />
      </div>
      <span class="w-10 text-sm font-bold text-right">{{ value }}</span>
    </div>
  `
}

export default {
  name: 'PokemonDetail',
  components: { ChevronLeft, BarChart3, Sparkles, Layers, GitBranch, ArrowRight, Zap, StatBar },
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    const pokemon = ref(null)
    const moves = ref([])
    const loadingMoves = ref(false)
    const selectedFormId = ref(null)

    // 获取宝可梦详情
    const fetchPokemonDetail = async () => {
      try {
        const result = await pokemonApi.getDetail(route.params.id)
        if (result.code === 200) {
          pokemon.value = result.data
          selectedFormId.value = result.data.forms?.[0]?.id
          
          // 获取技能
          if (selectedFormId.value) {
            fetchMoves(selectedFormId.value)
          }
        } else {
          ElMessage.error(result.message || '获取详情失败')
          router.push('/pokemon')
        }
      } catch (error) {
        console.error('获取宝可梦详情失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    // 获取技能列表
    const fetchMoves = async (formId) => {
      loadingMoves.value = true
      try {
        const result = await pokemonApi.getFormMoves(formId)
        if (result.code === 200) {
          moves.value = result.data.slice(0, 50) // 只显示前50个
        }
      } catch (error) {
        console.error('获取技能失败:', error)
      } finally {
        loadingMoves.value = false
      }
    }

    // 获取图片
    const getPokemonImage = (id) => sprites.pokemon(id)
    
    // 图片加载失败
    const handleImageError = (event) => {
      event.target.src = sprites.default
    }

    // 返回
    const goBack = () => router.push('/pokemon')

    // 监听形态变化
    watch(selectedFormId, (newId) => {
      if (newId && pokemon.value) {
        fetchMoves(newId)
      }
    })

    onMounted(() => {
      fetchPokemonDetail()
    })

    return {
      pokemon,
      moves,
      loadingMoves,
      selectedFormId,
      getPokemonImage,
      handleImageError,
      goBack
    }
  }
}
</script>
