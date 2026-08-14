<template>
  <div class="rounded-3xl shadow-poke-card overflow-hidden mb-6 border-3 border-slate-200/80 bg-white">
    <div class="md:flex">
      <!-- 图片区域 - Pokedex 风格 -->
      <div class="md:w-1/3 relative p-8 flex flex-col items-center justify-center min-h-[320px]">
        <!-- 背景 - 属性色渐变 + Pokeball 纹理 -->
        <div
          class="absolute inset-0 pokeball-bg"
          :style="{ background: typeBgGradient }"
        />

        <!-- 图片切换按钮 -->
        <div class="absolute top-4 right-4 flex gap-1.5 z-10">
          <button
            v-for="(label, type) in imageTypes"
            :key="type"
            class="w-9 h-9 rounded-xl flex items-center justify-center transition-all border-2"
            :class="imageMode === type
              ? 'bg-poke-red text-white shadow-poke border-red-400'
              : 'bg-white/80 text-slate-500 hover:bg-white border-white/60 shadow-sm'"
            :title="label"
            @click="imageMode = type"
          >
            <component
              :is="getImageIcon(type)"
              class="w-4 h-4"
            />
          </button>
        </div>

        <div class="relative z-10">
          <!-- 懒加载占位 -->
          <div
            v-if="!imageLoaded"
            class="w-64 h-64 flex items-center justify-center"
          >
            <div class="pokeball-spinner" />
          </div>
          <img
            v-show="imageLoaded"
            :src="currentImageUrl"
            :alt="pokemon.name"
            class="w-64 h-64 object-contain drop-shadow-2xl transition-transform duration-300 hover:scale-105"
            @load="imageLoaded = true"
            @error="handleImageError"
          >
          <!-- 图鉴编号 -->
          <div class="absolute -top-2 -left-2 bg-slate-800 text-white text-sm font-extrabold px-4 py-1.5 rounded-xl shadow-poke flex items-center gap-1.5 tracking-wider z-10">
            <Hash class="w-3.5 h-3.5" />
            Nº {{ String(pokemon.id).padStart(4, '0') }}
          </div>
          <!-- 形态标记 -->
          <div class="absolute -top-2 -right-2 flex gap-1 z-10">
            <span
              v-if="currentForm?.isMega"
              class="px-2.5 py-1 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-[11px] font-extrabold rounded-lg shadow-poke border border-amber-400"
            >
              MEGA
            </span>
            <span
              v-if="currentForm?.isGigantamax"
              class="px-2.5 py-1 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-[11px] font-extrabold rounded-lg shadow-poke border border-purple-400"
            >
              G-MAX
            </span>
          </div>
        </div>

        <!-- 图片类型说明 -->
        <p class="mt-3 text-xs text-slate-500 font-bold uppercase tracking-wider relative z-10">
          {{ imageTypes[imageMode] }}
        </p>
      </div>

      <!-- 信息区域 - Pokedex 风格 -->
      <div class="md:w-2/3 p-8">
        <div class="flex items-start justify-between mb-5">
          <div>
            <h1 class="text-3xl font-extrabold text-slate-900 leading-tight">
              {{ pokemon.name }}
            </h1>
            <p class="text-slate-400 mt-1 text-sm font-medium tracking-wide">
              {{ pokemon.nameEn }} / {{ pokemon.nameJp }}
            </p>
            <p class="text-poke-red font-bold mt-1.5 text-sm">
              {{ pokemon.genus }}
            </p>
          </div>
          <div class="flex gap-1.5">
            <span
              v-if="pokemon.isLegendary"
              class="px-3 py-1.5 bg-gradient-to-r from-amber-100 to-yellow-100 text-amber-700 rounded-xl text-xs font-extrabold shadow-poke border border-amber-200 flex items-center gap-1"
            >
              <Star class="w-3.5 h-3.5" />
              传说
            </span>
            <span
              v-if="pokemon.isMythical"
              class="px-3 py-1.5 bg-gradient-to-r from-purple-100 to-pink-100 text-purple-700 rounded-xl text-xs font-extrabold shadow-poke border border-purple-200 flex items-center gap-1"
            >
              <Sparkles class="w-3.5 h-3.5" />
              神话
            </span>
            <span
              v-if="pokemon.isBaby"
              class="px-3 py-1.5 bg-gradient-to-r from-pink-100 to-rose-100 text-pink-700 rounded-xl text-xs font-extrabold shadow-poke border border-pink-200 flex items-center gap-1"
            >
              <Baby class="w-3.5 h-3.5" />
              幼崽
            </span>
          </div>
        </div>

        <!-- 属性 -->
        <div class="mb-5">
          <span class="text-slate-400 text-xs font-bold uppercase tracking-wider">属性</span>
          <div class="inline-flex gap-2 ml-2">
            <span
              v-for="type in currentForm?.types || []"
              :key="type.id"
              class="type-badge !text-sm !px-4 !py-1.5"
              :style="{ backgroundColor: type.color }"
            >
              {{ type.name }}
            </span>
          </div>
        </div>

        <!-- 基本信息 - Pokedex 表格风格 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
          <div class="bg-slate-50 rounded-xl p-3.5 text-center border border-slate-200/80 shadow-poke-inset">
            <div class="text-slate-400 text-[11px] font-bold uppercase tracking-wider mb-1">
              身高
            </div>
            <div class="text-lg font-extrabold text-slate-800">
              {{ currentForm?.height || '-' }}m
            </div>
          </div>
          <div class="bg-slate-50 rounded-xl p-3.5 text-center border border-slate-200/80 shadow-poke-inset">
            <div class="text-slate-400 text-[11px] font-bold uppercase tracking-wider mb-1">
              体重
            </div>
            <div class="text-lg font-extrabold text-slate-800">
              {{ currentForm?.weight || '-' }}kg
            </div>
          </div>
          <div class="bg-slate-50 rounded-xl p-3.5 text-center border border-slate-200/80 shadow-poke-inset">
            <div class="text-slate-400 text-[11px] font-bold uppercase tracking-wider mb-1">
              捕获率
            </div>
            <div class="text-lg font-extrabold text-slate-800">
              {{ pokemon.captureRate || '-' }}
            </div>
          </div>
          <div class="bg-slate-50 rounded-xl p-3.5 text-center border border-slate-200/80 shadow-poke-inset">
            <div class="text-slate-400 text-[11px] font-bold uppercase tracking-wider mb-1">
              亲密度
            </div>
            <div class="text-lg font-extrabold text-slate-800">
              {{ pokemon.baseHappiness || '-' }}
            </div>
          </div>
        </div>

        <!-- 补充信息 -->
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3 mb-5">
          <div class="bg-red-50 rounded-xl p-3 border border-red-100">
            <div class="text-poke-red text-[11px] font-bold uppercase tracking-wider mb-1">
              性别比例
            </div>
            <div class="text-sm font-bold text-slate-800">
              {{ getGenderRatioText(pokemon.genderRate) }}
            </div>
          </div>
          <div class="bg-purple-50 rounded-xl p-3 border border-purple-100">
            <div class="text-purple-600 text-[11px] font-bold uppercase tracking-wider mb-1">
              蛋群
            </div>
            <div class="text-sm font-bold text-slate-800">
              {{ (pokemon.eggGroups || []).join(' / ') || '-' }}
            </div>
          </div>
          <div class="bg-green-50 rounded-xl p-3 border border-green-100">
            <div class="text-green-600 text-[11px] font-bold uppercase tracking-wider mb-1">
              孵化步数
            </div>
            <div class="text-sm font-bold text-slate-800">
              {{ pokemon.hatchCounter ? `${pokemon.hatchCounter * 255}` : '-' }}
            </div>
          </div>
          <div class="bg-amber-50 rounded-xl p-3 border border-amber-100">
            <div class="text-amber-600 text-[11px] font-bold uppercase tracking-wider mb-1">
              成长类型
            </div>
            <div class="text-sm font-bold text-slate-800">
              {{ pokemon.growthRate || '-' }}
            </div>
          </div>
          <div class="bg-rose-50 rounded-xl p-3 border border-rose-100">
            <div class="text-rose-600 text-[11px] font-bold uppercase tracking-wider mb-1">
              基础经验
            </div>
            <div class="text-sm font-bold text-slate-800">
              {{ currentForm?.baseExperience || '-' }}
            </div>
          </div>
          <div class="bg-blue-50 rounded-xl p-3 border border-blue-100">
            <div class="text-blue-600 text-[11px] font-bold uppercase tracking-wider mb-1">
              世代
            </div>
            <div class="text-sm font-bold text-slate-800">
              第 {{ pokemon.generationId }} 世代
            </div>
          </div>
        </div>

        <!-- 描述 -->
        <div class="bg-slate-50 rounded-2xl p-5 border border-slate-200/80">
          <p class="text-slate-600 leading-relaxed text-sm">
            {{ pokemon.description || '暂无描述' }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { Hash, Star, Sparkles, Baby } from 'lucide-vue-next'
import { sprites } from '../services/api.js'

export default {
  name: 'PokemonHeaderDetail',
  components: { Hash, Star, Sparkles, Baby },
  props: {
    pokemon: {
      type: Object,
      required: true
    },
    currentForm: {
      type: Object,
      default: null
    }
  },
  setup(props) {
    const imageMode = ref('front')
    const imageLoaded = ref(false)

    const imageTypes = {
      front: '正面',
      back: '背面',
      shiny: '闪光',
      official: '官方立绘'
    }

    const currentImageUrl = computed(() => {
      const form = props.currentForm
      if (!form) return sprites.pokemon(props.pokemon?.id)

      switch (imageMode.value) {
        case 'back':
          return form.spriteBackUrl || sprites.pokemon(props.pokemon?.id)
        case 'shiny':
          return form.spriteShinyUrl || sprites.pokemon(props.pokemon?.id)
        case 'official':
          return form.officialArtworkUrl || sprites.pokemon(props.pokemon?.id)
        default:
          return form.spriteUrl || sprites.pokemon(props.pokemon?.id)
      }
    })

    const primaryType = computed(() => {
      return props.currentForm?.types?.[0]?.nameEn?.toLowerCase() || 'normal'
    })

    const typeBgGradient = computed(() => {
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
      return gradients[primaryType.value] || gradients.normal
    })

    const getImageIcon = (type) => {
      const icons = {
        front: Hash,
        back: Sparkles,
        shiny: Star,
        official: Baby
      }
      return icons[type] || Hash
    }

    const getGenderRatioText = (rate) => {
      if (rate === null || rate === undefined) return '未知'
      if (rate === -1) return '无性别'
      if (rate === 0) return '全雄'
      if (rate === 8) return '全雌'
      const female = (rate / 8) * 100
      const male = 100 - female
      return `♂${male}% / ♀${female}%`
    }

    const handleImageError = (event) => {
      if (event.target) {
        event.target.src = sprites.default
        imageLoaded.value = true
      }
    }

    return {
      imageMode,
      imageLoaded,
      imageTypes,
      currentImageUrl,
      typeBgGradient,
      getImageIcon,
      getGenderRatioText,
      handleImageError
    }
  }
}
</script>

<style scoped>
.pokeball-bg::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  opacity: 0.05;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='96' fill='none' stroke='%23000' stroke-width='8'/%3E%3Cline x1='4' y1='100' x2='196' y2='100' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='24' fill='none' stroke='%23000' stroke-width='8'/%3E%3Ccircle cx='100' cy='100' r='12' fill='%23fff'/%3E%3C/svg%3E");
  background-size: contain;
  pointer-events: none;
  z-index: 0;
}
</style>
