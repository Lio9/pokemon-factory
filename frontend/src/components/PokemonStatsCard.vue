<template>
  <router-link
    :to="`/pokemon/${pokemon.id}`"
    class="pokemon-card relative overflow-hidden cursor-pointer group"
    :class="[
      'rounded-2xl border-3 bg-white shadow-poke-card transition-all duration-500',
      'hover:shadow-xl',
      isFav ? 'border-red-300' : 'border-slate-200/80'
    ]"
    :style="cardStyle"
    @click="onClick"
  >
    <!-- 顶部属性色条 -->
    <div
      class="absolute top-0 left-0 right-0 h-1.5 z-10"
      :style="{ background: typeGradient }"
    />

    <!-- 收藏按钮 -->
    <button
      class="absolute top-3.5 right-3 z-20 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110 border-2"
      :class="isFav ? 'bg-red-500 text-white shadow-lg border-red-400 fav-bounce' : 'bg-white/90 text-gray-400 hover:text-red-500 border-white/80 shadow-poke'"
      @click.prevent="onToggleFavorite"
    >
      <span class="text-sm">{{ isFav ? '❤️' : '🤍' }}</span>
    </button>

    <!-- 图片区域 - Pokeball 纹理背景 -->
    <div
      class="relative p-4 pokeball-bg"
      :style="{ background: typeBgGradient }"
    >
      <div class="aspect-square flex items-center justify-center relative z-10">
        <!-- 懒加载占位 -->
        <div
          v-if="!pokemon._imageLoaded"
          class="w-full h-full flex items-center justify-center skeleton rounded-xl"
        >
          <div class="pokeball-spinner" />
        </div>
        <img
          v-show="pokemon._imageLoaded"
          :src="pokemon._imageUrl"
          :alt="pokemon.name"
          class="w-full h-full object-contain group-hover:scale-110 group-hover:drop-shadow-2xl transition-all duration-500"
          loading="lazy"
          @load="onImageLoad"
          @error="onImageError"
        >
      </div>
    </div>

    <!-- 图鉴编号 - 游戏风格 -->
    <div class="absolute top-3.5 left-3 z-20 bg-slate-800 text-white text-[10px] font-extrabold px-2.5 py-1 rounded-lg shadow-poke tracking-wider">
      Nº {{ String(pokemon.id).padStart(4, '0') }}
    </div>

    <!-- 特殊标记 -->
    <div
      v-if="pokemon.isLegendary"
      class="absolute top-3.5 left-16 z-20"
    >
      <div class="w-7 h-7 bg-gradient-to-br from-yellow-400 to-amber-500 rounded-lg flex items-center justify-center shadow-poke border border-yellow-300">
        <span class="text-white text-xs font-extrabold">★</span>
      </div>
    </div>
    <div
      v-else-if="pokemon.isMythical"
      class="absolute top-3.5 left-16 z-20"
    >
      <div class="w-7 h-7 bg-gradient-to-br from-purple-400 to-pink-500 rounded-lg flex items-center justify-center shadow-poke border border-purple-300">
        <span class="text-white text-xs font-extrabold">◆</span>
      </div>
    </div>

    <!-- 信息区域 -->
    <div class="p-4 pt-3">
      <h3 class="font-extrabold text-gray-900 truncate text-base group-hover:text-red-600 transition-colors">
        {{ pokemon.name }}
      </h3>
      <p class="text-gray-400 text-xs truncate font-medium mt-0.5">
        {{ pokemon.genus }}
      </p>

      <!-- 属性标签 - 正作风格 -->
      <div class="flex flex-wrap gap-1.5 mt-2.5">
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
  </router-link>
</template>

<script>
export default {
  name: 'PokemonStatsCard',
  props: {
    pokemon: { type: Object, required: true },
    isFav: { type: Boolean, default: false }
  },
  emits: ['toggleFavorite', 'image-load', 'image-error', 'click'],
  computed: {
    primaryType() {
      return this.pokemon.types?.[0]?.nameEn?.toLowerCase() || 'normal'
    },
    typeGradient() {
      const colors = {
        normal: '#A8A878', fire: '#F08030', water: '#6890F0', electric: '#F8D030',
        grass: '#78C850', ice: '#98D8D8', fighting: '#C03028', poison: '#A040A0',
        ground: '#E0C068', flying: '#A890F0', psychic: '#F85888', bug: '#A8B820',
        rock: '#B8A038', ghost: '#705898', dragon: '#7038F8', dark: '#705848',
        steel: '#B8B8D0', fairy: '#EE99AC'
      }
      const c = colors[this.primaryType] || '#A8A878'
      return `linear-gradient(90deg, ${c}, ${c}cc)`
    },
    typeBgGradient() {
      const gradients = {
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
      return gradients[this.primaryType] || gradients.normal
    },
    cardStyle() {
      const colors = {
        normal: '#A8A878', fire: '#F08030', water: '#6890F0', electric: '#F8D030',
        grass: '#78C850', ice: '#98D8D8', fighting: '#C03028', poison: '#A040A0',
        ground: '#E0C068', flying: '#A890F0', psychic: '#F85888', bug: '#A8B820',
        rock: '#B8A038', ghost: '#705898', dragon: '#7038F8', dark: '#705848',
        steel: '#B8B8D0', fairy: '#EE99AC'
      }
      const c = colors[this.primaryType] || '#A8A878'
      return {
        '--card-accent': `linear-gradient(90deg, ${c}, ${c}cc)`,
        '--card-accent-color': c + '60'
      }
    }
  },
  methods: {
    onToggleFavorite() {
      this.$emit('toggleFavorite', this.pokemon)
    },
    onImageLoad() {
      this.$emit('image-load', this.pokemon)
    },
    onImageError() {
      this.$emit('image-error', this.pokemon)
    },
    onClick() {
      this.$emit('click', this.pokemon)
    }
  }
}
</script>

<style scoped>
.pokemon-card {
  animation: fadeInUp 0.45s ease-out both;
  will-change: transform;
  transition: transform 0.45s cubic-bezier(0.2,0.8,0.2,1), box-shadow 0.35s ease, border-color 0.35s ease;
}

.pokemon-card:hover {
  transform: translateY(-6px) scale(1.01);
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

/* Pokeball 纹理 */
.pokeball-bg::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  opacity: 0.06;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='96' fill='none' stroke='%23000' stroke-width='8'/%3E%3Cline x1='4' y1='100' x2='196' y2='100' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='24' fill='none' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='12' fill='%23fff'/%3E%3C/svg%3E");
  background-size: contain;
  pointer-events: none;
  z-index: 1;
}

.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
