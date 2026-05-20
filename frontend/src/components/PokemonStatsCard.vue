<template>
  <router-link
    :to="`/pokemon/${pokemon.id}`"
    class="pokemon-card bg-white rounded-2xl shadow-lg hover:shadow-2xl transition-all duration-500 cursor-pointer overflow-hidden group border-2 border-transparent hover:border-blue-200 relative"
    @click="onClick"
  >
    <!-- 收藏按钮 -->
    <button
      class="absolute top-3 right-3 z-10 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 hover:scale-110"
      :class="isFav ? 'bg-red-500 text-white shadow-lg fav-bounce' : 'bg-white/90 text-gray-400 hover:text-red-500'"
      @click.prevent="onToggleFavorite"
    >
      <span
        class="w-4 h-4 text-sm"
        :class="isFav ? 'text-red-500' : 'text-gray-400'"
      >❤️</span>
    </button>

    <!-- 图片区域 -->
    <div class="relative bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 p-4">
      <div class="aspect-square flex items-center justify-center">
        <!-- 懒加载占位 -->
        <div
          v-if="!pokemon._imageLoaded"
          class="w-full h-full flex items-center justify-center skeleton rounded-xl"
        >
          <div class="text-center">
            <div class="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center mb-2">
              <svg
                class="w-8 h-8 text-blue-300 animate-spin"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                />
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
            </div>
          </div>
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
    <!-- 图鉴编号 -->
    <div class="absolute top-3 left-3 bg-gradient-to-r from-gray-900 to-gray-700 text-white text-xs font-bold px-3 py-1 rounded-full shadow-lg">
      #{{ String(pokemon.id).padStart(4, '0') }}
    </div>
    <!-- 特殊标记 -->
    <div
      v-if="pokemon.isLegendary"
      class="absolute top-3 right-12"
    >
      <div class="w-8 h-8 bg-gradient-to-br from-yellow-400 to-amber-500 rounded-full flex items-center justify-center shadow-lg animate-pulse">
        <span class="text-white text-sm font-bold">★</span>
      </div>
    </div>
    <div
      v-else-if="pokemon.isMythical"
      class="absolute top-3 right-12"
    >
      <div class="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center shadow-lg animate-pulse">
        <span class="text-white text-sm font-bold">◆</span>
      </div>
    </div>

    <!-- 信息区域 -->
    <div class="p-4">
      <h3 class="font-bold text-gray-900 truncate text-lg group-hover:text-blue-600 transition-colors">
        {{ pokemon.name }}
      </h3>
      <p class="text-gray-500 text-sm truncate">
        {{ pokemon.genus }}
      </p>

      <!-- 属性标签 -->
      <div class="flex flex-wrap gap-2 mt-3">
        <span
          v-for="type in pokemon.types"
          :key="type.id"
          class="px-3 py-1 rounded-full text-xs font-bold text-white shadow-md"
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
  animation: fadeInUp 0.45s ease-out;
  will-change: transform;
  transition: transform 0.45s cubic-bezier(0.2,0.8,0.2,1), box-shadow 0.35s ease;
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(255,255,255,0.9), rgba(255,255,255,0.8));
}

.pokemon-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 18px 40px rgba(59,130,246,0.12), 0 6px 20px rgba(99,102,241,0.06);
  border-color: rgba(59,130,246,0.12);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
