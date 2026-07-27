<template>
  <div
    v-if="pokemon"
    class="pokemon-detail"
  >
    <!-- 面包屑导航 -->
    <nav class="mb-6 flex items-center gap-2 text-sm">
      <router-link
        to="/"
        class="text-gray-500 hover:text-blue-600 transition-colors"
      >
        首页
      </router-link>
      <ChevronRight class="w-4 h-4 text-gray-400" />
      <router-link
        to="/pokemon"
        class="text-gray-500 hover:text-blue-600 transition-colors"
      >
        图鉴
      </router-link>
      <ChevronRight class="w-4 h-4 text-gray-400" />
      <span class="text-gray-900 font-medium">{{ pokemon.name }}</span>
    </nav>

    <!-- 快捷操作栏 -->
    <div class="fixed top-4 right-4 z-50 flex gap-2">
      <button 
        class="w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-all duration-300 hover:scale-110"
        :class="isFavorite ? 'bg-gradient-to-r from-pink-500 to-rose-500 text-white' : 'bg-white text-gray-600 hover:text-pink-500'"
        title="收藏"
        @click="toggleFavorite"
      >
        <Heart :class="['w-6 h-6 transition-all duration-300', isFavorite ? 'fill-current scale-110' : '']" />
      </button>
      <button 
        class="w-12 h-12 rounded-full bg-white text-gray-600 hover:text-blue-600 shadow-lg transition-all duration-300 hover:scale-110 flex items-center justify-center"
        title="比较"
        @click="showCompareModal = true"
      >
        <Scale class="w-6 h-6" />
      </button>
      <button 
        class="w-12 h-12 rounded-full bg-white text-gray-600 hover:text-green-600 shadow-lg transition-all duration-300 hover:scale-110 flex items-center justify-center"
        title="分享"
        @click="sharePokemon"
      >
        <Share2 class="w-6 h-6" />
      </button>
    </div>

    <!-- 比较模态框 -->
    <PokemonCompareModal
      :visible="showCompareModal"
      :pokemon1="pokemon"
      :pokemon2="comparePokemon"
      @close="showCompareModal = false"
      @select-compare="selectComparePokemon"
    />

    <!-- 基本信息卡片 -->
    <PokemonHeaderDetail
      :pokemon="pokemon"
      :current-form="currentForm"
    />

    <!-- 种族值 -->
    <PokemonStatsPanel
      :stats="currentForm?.stats || null"
      :view-mode="statsViewMode"
      @toggle-view="statsViewMode = statsViewMode === 'bar' ? 'radar' : 'bar'"
    />

    <!-- 特性 -->
    <PokemonAbilitiesPanel
      :abilities="currentForm?.abilities || []"
    />

    <!-- 形态 -->
    <div
      v-if="pokemon.forms?.length > 1"
      class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80"
    >
      <h2 class="text-xl font-extrabold text-slate-800 mb-6 flex items-center gap-3">
        <div class="w-9 h-9 bg-gradient-to-br from-amber-500 to-orange-600 rounded-xl flex items-center justify-center shadow-poke border-2 border-amber-400">
          <Layers class="w-4.5 h-4.5 text-white" />
        </div>
        形态
      </h2>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
        <div
          v-for="form in pokemon.forms"
          :key="form.id"
          class="rounded-2xl p-4 text-center cursor-pointer transition-all duration-300 group border-3"
          :class="selectedFormId === form.id
            ? 'bg-gradient-to-br from-poke-red to-red-600 text-white shadow-poke-lg border-red-400 scale-105'
            : 'bg-slate-50 border-slate-200 hover:border-red-300 hover:shadow-poke-card'"
          @click="selectedFormId = form.id"
        >
          <div class="relative">
            <img 
              :src="form.spriteUrl || getPokemonImage(pokemon.id)"
              :alt="form.formName"
              class="w-20 h-20 mx-auto object-contain"
              loading="lazy"
            >
            <!-- 形态标记 -->
            <div class="absolute -top-2 -right-2 flex gap-1">
              <span
                v-if="form.isMega"
                class="w-5 h-5 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold rounded-full flex items-center justify-center"
              >M</span>
              <span
                v-if="form.isGigantamax"
                class="w-5 h-5 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full flex items-center justify-center"
              >G</span>
            </div>
          </div>
          <p
            class="mt-3 font-medium"
            :class="selectedFormId === form.id ? 'text-white' : 'text-gray-900'"
          >
            {{ form.formName || '默认形态' }}
          </p>
        </div>
      </div>
    </div>

    <!-- 进化链 -->
    <EvolutionChainPanel
      :evolution-chain="pokemon.evolutionChain || []"
    />

    <!-- 技能列表 -->
    <PokemonMovesPanel
      :moves="moves"
      :loading="loadingMoves"
    />
  </div>

  <!-- 加载中骨架屏 -->
  <div
    v-else
    class="pokemon-detail"
  >
    <!-- 面包屑骨架 -->
    <div class="mb-6 flex items-center gap-2">
      <el-skeleton-item
        variant="text"
        style="width: 60px"
      />
      <el-skeleton-item
        variant="text"
        style="width: 16px"
      />
      <el-skeleton-item
        variant="text"
        style="width: 60px"
      />
      <el-skeleton-item
        variant="text"
        style="width: 16px"
      />
      <el-skeleton-item
        variant="text"
        style="width: 80px"
      />
    </div>

    <!-- 主卡片骨架 -->
    <div class="bg-white rounded-3xl shadow-poke-card overflow-hidden mb-6 border-3 border-slate-200/80">
      <div class="md:flex">
        <div class="md:w-1/3 bg-gradient-to-br from-slate-50 via-red-50 to-orange-50 p-8 flex items-center justify-center">
          <div class="pokeball-spinner" />
        </div>
        <div class="md:w-2/3 p-8">
          <el-skeleton
            :rows="8"
            animated
          />
        </div>
      </div>
    </div>

    <div class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80">
      <el-skeleton
        :rows="10"
        animated
      />
    </div>

    <div class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80">
      <el-skeleton
        :rows="6"
        animated
      />
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChevronRight, Heart, Scale, Share2, Layers } from 'lucide-vue-next'
import { pokemonApi, sprites } from '../services/api.js'
import { dataCache } from '../services/cache.js'

import PokemonStatsPanel from '../components/PokemonStatsPanel.vue'
import PokemonAbilitiesPanel from '../components/PokemonAbilitiesPanel.vue'
import PokemonMovesPanel from '../components/PokemonMovesPanel.vue'
import EvolutionChainPanel from '../components/EvolutionChainPanel.vue'
import PokemonCompareModal from '../components/PokemonCompareModal.vue'
import PokemonHeaderDetail from '../components/PokemonHeaderDetail.vue'

export default {
  name: 'PokemonDetail',
  components: { ChevronRight, Heart, Scale, Share2, Layers, PokemonStatsPanel, PokemonAbilitiesPanel, PokemonMovesPanel, EvolutionChainPanel, PokemonCompareModal, PokemonHeaderDetail },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const pokemon = ref(null)
    const moves = ref([])
    const loadingMoves = ref(false)
    const selectedFormId = ref(null)

    // 新增状态
    const isFavorite = ref(false)
    const showCompareModal = ref(false)
    const comparePokemon = ref(null)
    const statsViewMode = ref('bar')
    const favorites = ref([])

    // 加载收藏列表
    const loadFavorites = () => {
      try {
        const stored = localStorage.getItem('pokemon-favorites')
        if (stored) {
          favorites.value = JSON.parse(stored)
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
      }
    }

    // 检查是否已收藏
    const checkFavorite = () => {
      isFavorite.value = favorites.value.includes(pokemon.value?.id)
    }

    // 切换收藏状态
    const toggleFavorite = () => {
      if (!pokemon.value) return

      const index = favorites.value.indexOf(pokemon.value.id)
      if (index > -1) {
        favorites.value.splice(index, 1)
        ElMessage.success('已取消收藏')
      } else {
        favorites.value.push(pokemon.value.id)
        ElMessage.success('已添加收藏')
      }

      try {
        localStorage.setItem('pokemon-favorites', JSON.stringify(favorites.value))
      } catch (error) {
        console.error('保存收藏失败:', error)
      }

      isFavorite.value = !isFavorite.value
    }

    // 分享功能
    const sharePokemon = async () => {
      if (!pokemon.value) return

      const url = window.location.href

      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(url)
          ElMessage.success('链接已复制到剪贴板')
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = url
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
          ElMessage.success('链接已复制到剪贴板')
        }
      } catch (error) {
        console.error('复制失败:', error)
        ElMessage.error('复制失败，请手动复制链接')
      }
    }

    // 选择比较宝可梦
    const selectComparePokemon = (id) => {
      ElMessage.info('选择比较宝可梦功能待实现')
    }

    // 快捷键支持
    const handleKeydown = (event) => {
      if (!pokemon.value) return

      switch (event.key) {
        case 'ArrowLeft':
          event.preventDefault()
          navigatePokemon(-1)
          break
        case 'ArrowRight':
          event.preventDefault()
          navigatePokemon(1)
          break
        case 'f':
        case 'F':
          if (!event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault()
            toggleFavorite()
          }
          break
        case 's':
        case 'S':
          if (!event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault()
            sharePokemon()
          }
          break
      }
    }

    // 导航到相邻宝可梦
    const navigatePokemon = (direction) => {
      const newId = pokemon.value.id + direction
      if (newId > 0) {
        router.push(`/pokemon/${newId}`)
      }
    }

    // 当前形态
    const currentForm = computed(() => {
      if (!pokemon.value?.forms) return null
      return pokemon.value.forms.find(f => f.id === selectedFormId.value) || pokemon.value.forms[0]
    })

    // 获取宝可梦详情 - 使用缓存
    const fetchPokemonDetail = async () => {
      try {
        const result = await dataCache.getOrFetch('pokemon-detail', { id: route.params.id }, async () => {
          return await pokemonApi.getDetail(route.params.id)
        })
        if (result.code === 200) {
          pokemon.value = result.data
          selectedFormId.value = result.data.forms?.[0]?.id

          // 检查收藏状态
          checkFavorite()

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
          moves.value = result.data.slice(0, 100)
        }
      } catch (error) {
        console.error('获取技能失败:', error)
      } finally {
        loadingMoves.value = false
      }
    }

    // 获取图片
    const getPokemonImage = (id) => sprites.pokemon(id)

    // 监听形态变化
    watch(selectedFormId, (newId) => {
      if (newId && pokemon.value) {
        fetchMoves(newId)
      }
    })

    onMounted(() => {
      fetchPokemonDetail()
      loadFavorites()
      window.addEventListener('keydown', handleKeydown)
    })

    onUnmounted(() => {
      window.removeEventListener('keydown', handleKeydown)
    })

    return {
      pokemon,
      moves,
      loadingMoves,
      selectedFormId,
      currentForm,
      getPokemonImage,
      isFavorite,
      showCompareModal,
      comparePokemon,
      statsViewMode,
      toggleFavorite,
      sharePokemon,
      selectComparePokemon
    }
  }
}
</script>

<style scoped>
.pokemon-detail {
  padding-bottom: 2rem;
}

/* 模态框动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}

/* 改进的动画效果 */
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

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(59, 130, 246, 0);
  }
}

.pokemon-detail > div {
  animation: fadeInUp 0.5s ease-out;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .pokemon-detail {
    padding-bottom: 1rem;
  }

  .fixed.top-4.right-4 {
    top: auto;
    bottom: 4rem;
    right: 1rem;
  }

  .bg-white.rounded-3xl.shadow-xl {
    border-radius: 1.5rem;
  }

  .text-4xl {
    font-size: 2rem;
  }
}

@media (max-width: 480px) {
  .w-72.h-72 {
    width: 180px;
    height: 180px;
  }

  .w-28.h-28 {
    width: 80px;
    height: 80px;
  }

  .w-20.h-20 {
    width: 60px;
    height: 60px;
  }

  .text-2xl {
    font-size: 1.25rem;
  }

  .px-4.py-2 {
    padding: 0.5rem 0.75rem;
  }
}

/* 滚动条美化 */
.overflow-y-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 改进的过渡效果 */
.group:hover .group-hover\:scale-110 {
  transform: scale(1.1);
}

.transition-all.duration-300 {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>