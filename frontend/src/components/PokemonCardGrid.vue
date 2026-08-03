<template>
  <div>
    <!-- 加载中 - 首次加载 -->
    <div
      v-if="loading && pokemons.length === 0"
      class="text-center py-12"
    >
      <div :class="viewMode === 'grid' ? 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4' : 'space-y-4'">
        <div
          v-for="i in 12"
          :key="i"
          :class="viewMode === 'grid'
            ? 'bg-white rounded-2xl shadow-poke-card overflow-hidden border-3 border-slate-200/80'
            : 'bg-white rounded-2xl shadow-poke-card p-4 flex items-center gap-4 border-3 border-slate-200/80'"
        >
          <div :class="viewMode === 'grid' ? 'aspect-square skeleton-pulse' : 'w-20 h-20 rounded-xl skeleton-pulse flex-shrink-0'" />
          <div :class="viewMode === 'list' ? 'flex-1' : 'p-4'">
            <div
              class="h-5 mb-2 rounded-lg skeleton-pulse"
              :class="viewMode === 'list' ? 'w-32' : ''"
            />
            <div class="h-3 w-3/4 rounded-lg skeleton-pulse" />
            <div
              v-if="viewMode === 'grid'"
              class="flex gap-2 mt-3"
            >
              <div class="h-5 w-14 rounded-full skeleton-pulse" />
              <div class="h-5 w-14 rounded-full skeleton-pulse" />
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
        class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-6"
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
        class="space-y-3"
      >
        <router-link
          v-for="pokemon in pokemons"
          :key="pokemon.id"
          :to="`/pokemon/${pokemon.id}`"
          class="pokemon-card-list relative flex flex-col gap-4 overflow-hidden rounded-2xl border-3 border-slate-200/80 bg-white p-4 shadow-poke-card transition-all duration-300 hover:border-red-200 hover:shadow-xl sm:flex-row sm:items-center"
        >
          <!-- 收藏按钮 -->
          <button
            class="absolute top-4 right-4 z-10 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110 border-2"
            :class="isFavorite(pokemon.id)
              ? 'bg-red-500 text-white shadow-lg border-red-400 fav-bounce'
              : 'bg-white/90 text-gray-400 hover:text-red-500 border-white/80'"
            @click.prevent="onToggleFavorite(pokemon)"
          >
            <span class="text-sm">{{ isFavorite(pokemon.id) ? '❤️' : '🤍' }}</span>
          </button>

          <!-- 图片 -->
          <div
            class="relative rounded-xl p-3 flex-shrink-0 pokeball-bg"
            :style="{ background: getListTypeBg(pokemon) }"
          >
            <div class="w-20 h-20 flex items-center justify-center relative z-10">
              <div
                v-if="!pokemon._imageLoaded"
                class="w-full h-full flex items-center justify-center skeleton-pulse rounded-lg"
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
            <div class="absolute -top-2 -left-2 bg-slate-800 text-white text-[10px] font-extrabold px-2.5 py-1 rounded-lg shadow-poke tracking-wider z-10">
              Nº {{ String(pokemon.id).padStart(4, '0') }}
            </div>
          </div>

          <!-- 信息 -->
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="font-extrabold text-gray-900 text-lg truncate group-hover:text-red-600 transition-colors">
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
            <p class="text-gray-400 text-sm truncate font-medium">
              {{ pokemon.genus }}
            </p>

            <!-- 属性标签 -->
            <div class="flex flex-wrap gap-1.5 mt-2">
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
            class="hidden sm:flex gap-3 flex-shrink-0"
          >
            <div class="text-center">
              <div class="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                攻击
              </div>
              <div class="text-sm font-extrabold text-slate-700">
                {{ pokemon.formStats.attack }}
              </div>
            </div>
            <div class="text-center">
              <div class="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                速度
              </div>
              <div class="text-sm font-extrabold text-slate-700">
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
          <p class="text-slate-400 text-sm font-bold">
            加载更多宝可梦中...
          </p>
        </div>
        <div
          v-else-if="!hasMore"
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-slate-300">
            <svg
              viewBox="0 0 100 100"
              class="w-6 h-6 opacity-40"
            >
              <circle
                cx="50"
                cy="50"
                r="46"
                fill="none"
                stroke="currentColor"
                stroke-width="3"
              />
              <line
                x1="4"
                y1="50"
                x2="96"
                y2="50"
                stroke="currentColor"
                stroke-width="3"
              />
              <circle
                cx="50"
                cy="50"
                r="10"
                fill="none"
                stroke="currentColor"
                stroke-width="3"
              />
            </svg>
            <span class="text-sm font-bold">已加载全部 {{ total }} 只宝可梦</span>
          </div>
        </div>
        <div
          v-else
          class="text-center py-4"
        >
          <div class="flex items-center justify-center gap-2 text-slate-300">
            <el-icon class="text-xl animate-bounce">
              <ArrowDown />
            </el-icon>
            <span class="text-sm font-medium">继续下拉加载更多...</span>
          </div>
        </div>
      </div>

      <!-- 快速返回顶部 -->
      <transition name="fade">
        <button
          v-show="showBackTop"
          class="fixed bottom-8 right-8 w-14 h-14 bg-poke-red text-white rounded-full shadow-xl hover:shadow-2xl hover:bg-red-700 transition-all duration-300 z-20 flex items-center justify-center group transform hover:scale-110 border-3 border-red-400"
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
      class="text-center py-16"
    >
      <div class="mb-6">
        <svg
          viewBox="0 0 200 200"
          class="w-24 h-24 mx-auto opacity-20"
        >
          <circle
            cx="100"
            cy="100"
            r="90"
            fill="none"
            stroke="currentColor"
            stroke-width="6"
          />
          <line
            x1="10"
            y1="100"
            x2="190"
            y2="100"
            stroke="currentColor"
            stroke-width="6"
          />
          <circle
            cx="100"
            cy="100"
            r="22"
            fill="none"
            stroke="currentColor"
            stroke-width="6"
          />
          <circle
            cx="100"
            cy="100"
            r="10"
            fill="currentColor"
            opacity="0.3"
          />
        </svg>
      </div>
      <p class="text-slate-400 text-lg font-bold mb-2">
        没有找到宝可梦
      </p>
      <p class="text-slate-300 text-sm mb-6">
        试试其他搜索条件
      </p>
      <button
        class="btn-poke !rounded-xl"
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
  expose: ['loadMoreTrigger'],
  setup() {
    const loadMoreTrigger = ref(null)
    return { loadMoreTrigger }
  },
  methods: {
    getListTypeBg(pokemon) {
      const typeGradients = {
        normal: 'linear-gradient(135deg, #f5f5eb, #ececea)',
        fire: 'linear-gradient(135deg, #fef0e6, #fde0c8)',
        water: 'linear-gradient(135deg, #e8f0fe, #d0e4fd)',
        electric: 'linear-gradient(135deg, #fef8e0, #fdf0b8)',
        grass: 'linear-gradient(135deg, #eaf6e2, #d8eece)',
        ice: 'linear-gradient(135deg, #eaf6f6, #d4eded)',
        fighting: 'linear-gradient(135deg, #fde8e7, #fbd0ce)',
        poison: 'linear-gradient(135deg, #f4e8f4, #e8d0e8)',
        ground: 'linear-gradient(135deg, #faf3e2, #f5e8c4)',
        flying: 'linear-gradient(135deg, #f0ecfe, #e0d8fc)',
        psychic: 'linear-gradient(135deg, #feeaef, #fdd4de)',
        bug: 'linear-gradient(135deg, #f2f4dc, #e6eab8)',
        rock: 'linear-gradient(135deg, #f5f0dc, #ede4b8)',
        ghost: 'linear-gradient(135deg, #ede8f4, #dcd0e8)',
        dragon: 'linear-gradient(135deg, #ece6fe, #dcd0fc)',
        dark: 'linear-gradient(135deg, #ede8e5, #dcd0ca)',
        steel: 'linear-gradient(135deg, #f0f0f6, #e0e0ec)',
        fairy: 'linear-gradient(135deg, #fdf0f3, #fce0e8)'
      }
      const primaryType = pokemon.types?.[0]?.nameEn?.toLowerCase() || 'normal'
      return typeGradients[primaryType] || typeGradients.normal
    },
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
/* Pokeball 纹理 */
.pokeball-bg::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  opacity: 0.05;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='96' fill='none' stroke='%23000' stroke-width='8'/%3E%3Cline x1='4' y1='100' x2='196' y2='100' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='24' fill='none' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='12' fill='%23fff'/%3E%3C/svg%3E");
  background-size: contain;
  pointer-events: none;
  z-index: 1;
}

/* 加载指示器动画 */
.loading-dots {
  display: flex;
  gap: 10px;
  justify-content: center;
  align-items: center;
}

.loading-dots span {
  width: 10px;
  height: 10px;
  background: #DC2626;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #DC2626;
  animation: pokebounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
  background: #1a1a1a;
  box-shadow: 0 0 0 2px #1a1a1a;
}

@keyframes pokebounce {
  0%, 80%, 100% {
    transform: scale(0.6);
  }
  40% {
    transform: scale(1.2);
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
