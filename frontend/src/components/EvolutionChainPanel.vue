<template>
  <div
    v-if="evolutionChain && evolutionChain.length"
    class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80"
  >
    <h2 class="text-xl font-extrabold text-slate-800 mb-6 flex items-center gap-3">
      <div class="w-9 h-9 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-poke border-2 border-purple-400">
        <GitBranch class="w-5 h-5 text-white" />
      </div>
      进化链
    </h2>
    <div class="flex items-center justify-center flex-wrap gap-4">
      <template
        v-for="(evo, index) in evolutionChain"
        :key="evo.speciesId"
      >
        <router-link
          :to="`/pokemon/${evo.speciesId}`"
          class="flex flex-col items-center p-6 rounded-2xl transition-all duration-300 group"
          :class="evo.isCurrent ? 'bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-xl ring-4 ring-blue-300 scale-105' : 'bg-gradient-to-br from-slate-50 to-slate-50 border-2 border-slate-200 hover:border-purple-300 hover:shadow-xl'"
        >
          <div class="relative">
            <img
              :src="evo.spriteUrl || getPokemonImage(evo.speciesId)"
              :alt="evo.pokemonName || evo.name"
              class="w-28 h-28 object-contain transition-transform duration-300 group-hover:scale-110"
              loading="lazy"
              @error="handleImageError"
            >
            <span
              v-if="evo.isCurrent"
              class="absolute -top-2 -left-2 w-6 h-6 bg-white text-blue-600 rounded-full flex items-center justify-center shadow-lg text-xs font-bold"
            >
              ✓
            </span>
          </div>
          <span
            class="mt-3 font-bold"
            :class="evo.isCurrent ? 'text-white' : 'text-slate-900'"
          >{{ evo.pokemonName || evo.name }}</span>
          <span
            class="text-xs mt-1"
            :class="evo.isCurrent ? 'text-blue-100' : 'text-slate-500'"
          >#{{ String(evo.speciesId).padStart(4, '0') }}</span>
        </router-link>
        <div
          v-if="index < evolutionChain.length - 1"
          class="flex flex-col items-center text-slate-400"
        >
          <ArrowRight class="w-8 h-8 transition-transform duration-300 group-hover:translate-x-1" />
          <span class="text-xs mt-1 px-3 py-1 bg-slate-100 rounded-full">{{ evo.trigger }}</span>
          <span
            v-if="evo.minLevel"
            class="text-xs mt-1 px-3 py-1 bg-blue-100 text-blue-700 rounded-full font-medium"
          >Lv.{{ evo.minLevel }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { GitBranch, ArrowRight } from 'lucide-vue-next'
import { sprites } from '../services/api.js'

export default {
  name: 'EvolutionChainPanel',
  components: { GitBranch, ArrowRight },
  props: {
    evolutionChain: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    getPokemonImage(id) {
      return sprites.pokemon(id)
    },
    handleImageError(event) {
      if (event.target) {
        event.target.src = sprites.default
      }
    }
  }
}
</script>
