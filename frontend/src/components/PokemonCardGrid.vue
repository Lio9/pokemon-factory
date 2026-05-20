<template>
  <div>
    <!-- 加载中 - 首次加载 -->
    <div
      v-if="loading && pokemons.length === 0"
      class="text-center py-12"
    >
      <div :class="viewMode === 'grid' ? 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-5' : 'space-y-4'">
        <div
          v-for="i in 12"
          :key="i"
          :class="viewMode === 'grid' ? 'bg-white rounded-2xl shadow-lg p-4 overflow-hidden' : 'bg-white rounded-2xl shadow-lg p-4 flex items-center gap-4'"
        >
          <div :class="viewMode === 'grid' ? 'aspect-square mb-4 rounded-xl skeleton' : 'w-20 h-20 rounded-xl skeleton flex-shrink-0'" />
          <div :class="viewMode === 'grid' ? '' : 'flex-1'">
            <div
              class="h-6 mb-2 rounded skeleton"
              :class="viewMode === 'list' ? 'w-32' : ''"
            />
            <div class="h-4 w-3/4 rounded skeleton" />
            <div
              v-if="viewMode === 'grid'"
              class="flex gap-2 mt-3"
            >
              <div class="h-6 w-16 rounded-full skeleton" />
              <div class="h-6 w-16 rounded-full skeleton" />
            </div>
          </div>
        </div>
      </div>
      <div class="loading-dots mt-8">
        <span />
        <span />
        <span />
      </div>
    </div>

    <!-- 宝可梦列表 -->
    <div v-else-if="pokemons.length > 0">
      <!-- 网格视图 -->
      <div
        v-if="viewMode === 'grid'"
        class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-5 md:grid-cols-4 lg:grid-cols-6"
      >
        <PokemonStatsCard
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :pokemon="pokemon"
          :is-fav="isFavorite(pokemon.id)"
          @toggle-favorite="onToggleFavorite"
          @image-load="onImageLoad"
          @image-error="onImageError"
        />
      </div>

      <!-- 列表视图 -->
      <div
        v-else
        class="space-y-3 sm:space-y-4"
      >
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card-list relative flex flex-col gap-4 overflow-hidden rounded-2xl border-2 border-transparent bg-white p-4 shadow-lg transition-all duration-300 hover:border-blue-200 hover:shadow-2xl sm:flex-row sm:items-center"
        >
          <!-- 收藏按钮 -->
          <button
            class="absolute top-4 right-4 z-10 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
            :class="isFavorite(pokemon.id) ? 'bg-red-500 text-white shadow-lg fav-bounce' : 'bg-white/90 text-gray-400 hover:text-red-500'"
            @click.prevent="onToggleFavorite(pokemon)"
          >
            <span
              class="w-4 h-4 text-sm"
              :class="isFavorite(pokemon.id) ? 'text-red-500' : 'text-gray-400'"
            >❤️</span>
          </button>

          <!-- 图片 -->
          <div class="relative bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 rounded-xl p-3 flex-shrink-0">
            <div class="w-20 h-20 flex items-center justify-center">
              <div
                v-if="!pokemon._imageLoaded"
                class="w-full h-full flex items-center justify-center skeleton rounded-lg"
              />
              <img
                v-show="pokemon._imageLoaded"
                :src="pokemon._imageUrl"
                :alt="pokemon.name"
                class="w-full h-full object-contain group-hover:scale-110 transition-transform duration-300"
                loading="lazy"
                @load="onImageLoad(pokemon)"
                @error="onImageError(pokemon)"
              >
            </div>
            <!-- 图鉴编号 -->
            <div class="absolute -top-2 -left-2 bg-gradient-to-r from-gray-900 to-gray-700 text-white text-xs font-bold px-2 py-0.5 rounded-full shadow-lg">
              #{{ String(pokemon.id).padStart(4, '0') }}
            </div>
          </div>

          <!-- 信息 -->
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="font-bold text-gray-900 text-lg truncate group-hover:text-blue-600 transition-colors">
                {{ pokemon.name }}
              </h3>
              <span
                v-if="pokemon.isLegendary"
                class="text-yellow-500"
              >★</span>
              <span
                v-if="pokemon.isMythical"
                class="text-purple-500"
              >◆</span>
            </div>
            <p class="text-gray-500 text-sm truncate">
              {{ pokemon.genus }}
            </p>

            <!-- 属性标签 -->
            <div class="flex flex-wrap gap-2 mt-2">
              <span
                v-for="type in pokemon.types"
                :key="type.id"
                class="type-badge type-badge-sm"
                :style="{ backgroundColor: type.color }"
              >
                {{ type.name }}
              </span>
            </div>
          </div>

          <!-- 种族值预览 -->
          <div
            v-if="pokemon.formStats"
            class="hidden sm:flex gap-2 flex-shrink-0"
          >
            <div class="text-center">
              <div class="text-xs text-gray-500">
                攻击
              </div>
              <div class="text-sm font-bold text-gray-900">
                {{ pokemon.formStats.attack }}
              </div>
            </div>
            <div class="text-center">
              <div class="text-xs text-gray-500">
                速度
              </div>
              <div class="text-sm font-bold text-gray-900">
                {{ pokemon.formStats.speed }}
              </div>
            </div>
          </div>
        </router-link>
      </div>

      <!-- 加载更多指示器 -->
      <div
        ref="loadMoreTrigger"
        class="text-center py-8 min-h-[120px] flex items-center justify-center"
      >
        <div
          v-if="loadingMore"
          class="flex flex-col items-center gap-3"
        >
          <div class="loading-dots">
            <span />
            <span />
            <span />
          </div>
          <p class="text-gray-500 text-sm font-medium">
            加载更多宝可梦中...
          </p>
        </div>
        <div
          v-else-if="!hasMore"
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-gray-400">
            <el-icon class="text-xl">
              <CircleCheck />
            </el-icon>
            <span class="text-sm font-medium">已加载全部 {{ total }} 只宝可梦</span>
          </div>
        </div>
        <div
          v-else
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-gray-400">
            <el-icon class="text-xl animate-bounce">
              <ArrowDown />
            </el-icon>
            <span class="text-sm">继续下拉加载更多...</span>
          </div>
        </div>
      </div>

      <!-- 快速返回顶部 -->
      <transition name="fade">
        <button
          v-show="showBackTop"
          class="fixed bottom-8 right-8 w-14 h-14 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-full shadow-xl hover:shadow-2xl hover:from-blue-600 hover:to-indigo-700 transition-all duration-300 z-20 flex items-center justify-center group transform hover:scale-110"
          @click="onScrollToTop"
        >
          <el-icon class="text-2xl group-hover:-translate-y-1 transition-transform">
            <ArrowUp />
          </el-icon>
        </button>
      </transition>
    </div>

    <!-- 空状态 -->
    <div
      v-else-if="!loading"
      class="text-center py-12"
    >
      <div class="text-gray-300 mb-4">
        <svg
          class="w-20 h-20 mx-auto"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h6m2 5.291A7.962 7.962 0 0112 15c-2.34 0-4.47-.881-6.08-2.334M15 10a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
      </div>
      <p class="text-gray-500 text-lg">
        没有找到宝可梦
      </p>
      <p class="text-gray-400 text-sm mt-2">
        试试其他搜索条件
      </p>
      <button
        class="mt-4 px-6 py-2 bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-indigo-700 transition-all"
        @click="onResetFilters"
      >
        重置筛选
      </button>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { CircleCheck, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import PokemonStatsCard from './PokemonStatsCard.vue'

export default {
  name: 'PokemonCardGrid',
  components: { CircleCheck, ArrowDown, ArrowUp, PokemonStatsCard },
  props: {
    pokemons: { type: Array, default: () => [] },
    viewMode: { type: String, default: 'grid' },
    loading: { type: Boolean, default: false },
    loadingMore: { type: Boolean, default: false },
    hasMore: { type: Boolean, default: true },
    total: { type: Number, default: 0 },
    showBackTop: { type: Boolean, default: false },
    isFavorite: { type: Function, default: () => () => false }
  },
  emits: [
    'toggle-favorite',
    'image-load',
    'image-error',
    'scroll-to-top',
    'reset-filters'
  ],
  setup() {
    const loadMoreTrigger = ref(null)
    return { loadMoreTrigger }
  },
  expose: ['loadMoreTrigger'],
  methods: {
    onToggleFavorite(pokemon) {
      this.$emit('toggle-favorite', pokemon)
    },
    onImageLoad(pokemon) {
      this.$emit('image-load', pokemon)
    },
    onImageError(pokemon) {
      this.$emit('image-error', pokemon)
    },
    onScrollToTop() {
      this.$emit('scroll-to-top')
    },
    onResetFilters() {
      this.$emit('reset-filters')
    }
  }
}
</script>

<style scoped>
/* 加载指示器动画 */
.loading-dots {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #3b82f6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 骨架屏动画 */
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
