<template>
  <div class="bg-white rounded-3xl shadow-xl overflow-hidden mb-6 border border-gray-100">
    <div class="md:flex">
      <!-- 图片区域 -->
      <div class="md:w-1/3 bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-8 flex flex-col items-center justify-center relative">
        <!-- 图片切换按钮 -->
        <div class="absolute top-4 right-4 flex gap-2">
          <button
            v-for="(label, type) in imageTypes"
            :key="type"
            class="w-10 h-10 rounded-full flex items-center justify-center transition-all"
            :class="imageMode === type ? 'bg-blue-600 text-white shadow-lg scale-110' : 'bg-white/70 text-gray-600 hover:bg-white'"
            :title="label"
            @click="imageMode = type"
          >
            <component
              :is="getImageIcon(type)"
              class="w-5 h-5"
            />
          </button>
        </div>

        <div class="relative">
          <!-- 懒加载占位 -->
          <div
            v-if="!imageLoaded"
            class="w-72 h-72 flex items-center justify-center"
          >
            <div class="w-40 h-40 rounded-full bg-gradient-to-br from-blue-200 to-indigo-200 animate-pulse flex items-center justify-center">
              <Loader2 class="w-12 h-12 text-blue-400 animate-spin" />
            </div>
          </div>
          <img
            v-show="imageLoaded"
            :src="currentImageUrl"
            :alt="pokemon.name"
            class="w-72 h-72 object-contain drop-shadow-2xl transition-transform duration-300 hover:scale-105"
            @load="imageLoaded = true"
            @error="handleImageError"
          >
          <!-- 图鉴编号 -->
          <div class="absolute -top-3 -left-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-lg font-bold px-5 py-2 rounded-2xl shadow-lg flex items-center gap-2">
            <Hash class="w-4 h-4" />
            {{ String(pokemon.id).padStart(4, '0') }}
          </div>
          <!-- 形态标记 -->
          <div class="absolute -top-3 -right-3 flex gap-1">
            <span
              v-if="currentForm?.isMega"
              class="px-3 py-1 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold rounded-full shadow-lg"
            >
              MEGA
            </span>
            <span
              v-if="currentForm?.isGigantamax"
              class="px-3 py-1 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full shadow-lg"
            >
              极巨化
            </span>
          </div>
        </div>

        <!-- 图片类型说明 -->
        <p class="mt-4 text-sm text-gray-500 font-medium">
          {{ imageTypes[imageMode] }}
        </p>
      </div>

      <!-- 信息区域 -->
      <div class="md:w-2/3 p-8">
        <div class="flex items-start justify-between mb-6">
          <div>
            <h1 class="text-4xl font-bold bg-gradient-to-r from-gray-900 via-gray-800 to-gray-700 bg-clip-text text-transparent">
              {{ pokemon.name }}
            </h1>
            <p class="text-gray-500 mt-1 text-lg">
              {{ pokemon.nameEn }} / {{ pokemon.nameJp }}
            </p>
            <p class="text-blue-600 font-medium mt-2">
              {{ pokemon.genus }}
            </p>
          </div>
          <div class="flex gap-2">
            <span
              v-if="pokemon.isLegendary"
              class="px-4 py-2 bg-gradient-to-r from-amber-100 to-yellow-100 text-amber-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1"
            >
              <Star class="w-4 h-4" />
              传说
            </span>
            <span
              v-if="pokemon.isMythical"
              class="px-4 py-2 bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1"
            >
              <Sparkles class="w-4 h-4" />
              神话
            </span>
            <span
              v-if="pokemon.isBaby"
              class="px-4 py-2 bg-gradient-to-r from-pink-100 to-rose-100 text-pink-800 rounded-full text-sm font-bold shadow-sm flex items-center gap-1"
            >
              <Baby class="w-4 h-4" />
              幼崽
            </span>
          </div>
        </div>

        <!-- 属性 -->
        <div class="mb-6">
          <span class="text-gray-500 text-sm font-medium">属性</span>
          <div class="inline-flex gap-2 ml-2">
            <span
              v-for="type in currentForm?.types || []"
              :key="type.id"
              class="px-4 py-2 rounded-xl text-white text-sm font-bold shadow-md hover:shadow-lg transition-shadow"
              :style="{ backgroundColor: type.color }"
            >
              {{ type.name }}
            </span>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
            <div class="text-gray-500 text-xs font-medium mb-1">
              身高
            </div>
            <div class="text-xl font-bold text-gray-900">
              {{ currentForm?.height || '-' }}m
            </div>
          </div>
          <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
            <div class="text-gray-500 text-xs font-medium mb-1">
              体重
            </div>
            <div class="text-xl font-bold text-gray-900">
              {{ currentForm?.weight || '-' }}kg
            </div>
          </div>
          <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
            <div class="text-gray-500 text-xs font-medium mb-1">
              捕获率
            </div>
            <div class="text-xl font-bold text-gray-900">
              {{ pokemon.captureRate || '-' }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-gray-50 to-slate-50 rounded-2xl p-4 text-center border border-gray-100 hover:shadow-md transition-shadow">
            <div class="text-gray-500 text-xs font-medium mb-1">
              亲密度
            </div>
            <div class="text-xl font-bold text-gray-900">
              {{ pokemon.baseHappiness || '-' }}
            </div>
          </div>
        </div>

        <!-- 补充信息 -->
        <div class="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
          <div class="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-3 border border-blue-100">
            <div class="text-blue-600 text-xs font-medium mb-1">
              性别比例
            </div>
            <div class="text-base font-semibold text-gray-900">
              {{ getGenderRatioText(pokemon.genderRate) }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl p-3 border border-purple-100">
            <div class="text-purple-600 text-xs font-medium mb-1">
              蛋群
            </div>
            <div class="text-base font-semibold text-gray-900">
              {{ (pokemon.eggGroups || []).join(' / ') || '-' }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-3 border border-green-100">
            <div class="text-green-600 text-xs font-medium mb-1">
              孵化步数
            </div>
            <div class="text-base font-semibold text-gray-900">
              {{ pokemon.hatchCounter ? `${pokemon.hatchCounter * 255}` : '-' }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-amber-50 to-orange-50 rounded-xl p-3 border border-amber-100">
            <div class="text-amber-600 text-xs font-medium mb-1">
              成长类型
            </div>
            <div class="text-base font-semibold text-gray-900">
              {{ pokemon.growthRate || '-' }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-rose-50 to-red-50 rounded-xl p-3 border border-rose-100">
            <div class="text-rose-600 text-xs font-medium mb-1">
              基础经验
            </div>
            <div class="text-base font-semibold text-gray-900">
              {{ currentForm?.baseExperience || '-' }}
            </div>
          </div>
          <div class="bg-gradient-to-br from-cyan-50 to-sky-50 rounded-xl p-3 border border-cyan-100">
            <div class="text-cyan-600 text-xs font-medium mb-1">
              世代
            </div>
            <div class="text-base font-semibold text-gray-900">
              第 {{ pokemon.generationId }} 世代
            </div>
          </div>
        </div>

        <!-- 描述 -->
        <div class="bg-gradient-to-r from-slate-50 to-gray-50 rounded-2xl p-5 border border-gray-100">
          <p class="text-gray-700 leading-relaxed">
            {{ pokemon.description || '暂无描述' }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { Hash, Star, Sparkles, Baby, Loader2, Image, RotateCcw, Sparkles as Shiny } from 'lucide-vue-next'
import { sprites } from '../services/api.js'

export default {
  name: 'PokemonHeaderDetail',
  components: { Hash, Star, Sparkles, Baby, Loader2 },
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

    const getImageIcon = (type) => {
      const icons = {
        front: Image,
        back: RotateCcw,
        shiny: Shiny,
        official: Star
      }
      return icons[type] || Image
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
      getImageIcon,
      getGenderRatioText,
      handleImageError
    }
  }
}
</script>
