<template>
  <div v-if="pokemon" class="pokemon-detail">
    <!-- 头部信息 -->
    <div class="detail-header bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-6 mb-6">
      <div class="flex flex-col md:flex-row items-start gap-6">
        <div class="flex-shrink-0">
          <div class="w-32 h-32 bg-white rounded-2xl shadow-lg flex items-center justify-center text-6xl font-bold text-gray-300 border-4 border-dashed border-gray-200">
            {{ pokemon.indexNumber }}
          </div>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-3xl font-bold text-gray-900">{{ pokemon.name }}</h1>
            <span class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
              #{{ pokemon.indexNumber }}
            </span>
          </div>
          <p class="text-gray-600 mb-3">{{ pokemon.genus }}</p>
          <p class="text-gray-700 leading-relaxed">{{ pokemon.profile }}</p>
        </div>
      </div>
    </div>

    <!-- 形态信息 -->
    <div v-if="pokemon.forms && pokemon.forms.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
        </svg>
        形态信息
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="form in pokemon.forms" :key="form.id" class="bg-white rounded-lg border border-gray-200 p-4">
          <h3 class="font-semibold text-gray-900 mb-2">{{ form.name }}</h3>
          <div class="grid grid-cols-2 gap-2 text-sm">
            <div>
              <span class="text-gray-500">生命值:</span>
              <span class="ml-2 font-medium">{{ form.hp }}</span>
            </div>
            <div>
              <span class="text-gray-500">攻击:</span>
              <span class="ml-2 font-medium">{{ form.attack }}</span>
            </div>
            <div>
              <span class="text-gray-500">防御:</span>
              <span class="ml-2 font-medium">{{ form.defense }}</span>
            </div>
            <div>
              <span class="text-gray-500">特攻:</span>
              <span class="ml-2 font-medium">{{ form.spAttack }}</span>
            </div>
            <div>
              <span class="text-gray-500">特防:</span>
              <span class="ml-2 font-medium">{{ form.spDefense }}</span>
            </div>
            <div>
              <span class="text-gray-500">速度:</span>
              <span class="ml-2 font-medium">{{ form.speed }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 进化链 -->
    <div v-if="pokemon.evolutionChain && pokemon.evolutionChain.length > 0" class="section mb-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
        </svg>
        进化链
      </h2>
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div v-for="(evolution, index) in pokemon.evolutionChain" :key="index" class="flex items-center gap-4 mb-3 last:mb-0">
          <div v-if="evolution.prevPokemonName" class="flex items-center gap-2">
            <span class="px-3 py-1 bg-gray-100 rounded-full text-sm">{{ evolution.prevPokemonName }}</span>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
            </svg>
          </div>
          <div class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm">
            {{ evolution.evolutionMethod }}
          </div>
          <div v-if="evolution.evolutionValue" class="text-sm text-gray-600">
            ({{ evolution.evolutionValue }})
          </div>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div class="text-center mt-8">
      <button @click="goBack" class="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors">
        返回列表
      </button>
    </div>
  </div>
  
  <div v-else class="text-center py-12">
    <div class="text-gray-400 mb-4">
      <svg class="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
      </svg>
    </div>
    <p class="text-gray-500">加载中...</p>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'PokemonDetail',
  props: {
    pokemon: {
      type: Object,
      default: null
    }
  },
  emits: ['close'],
  setup() {
    const router = useRouter()
    
    const goBack = () => {
      router.push('/pokemon')
    }
    
    return {
      goBack
    }
  },
  data() {
    return {
      types: [],
      abilities: [],
      eggGroups: []
    }
  },
  methods: {
    getTypeName(typeId) {
      // 这里应该从API获取类型名称，暂时返回ID
      return `类型${typeId}`;
    },
    getAbilityName(abilityId) {
      // 这里应该从API获取特性名称，暂时返回ID
      return `特性${abilityId}`;
    },
    getEggGroupName(eggGroupId) {
      // 这里应该从API获取蛋群名称，暂时返回ID
      return `蛋群${eggGroupId}`;
    }
  }
}
</script>

<style scoped>
.section {
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