<template>
  <div
    v-if="abilities && abilities.length"
    class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80"
  >
    <h2 class="text-xl font-extrabold text-slate-800 mb-6 flex items-center gap-3">
      <div class="w-9 h-9 bg-gradient-to-br from-green-500 to-emerald-600 rounded-xl flex items-center justify-center shadow-poke border-2 border-green-400">
        <Sparkles class="w-5 h-5 text-white" />
      </div>
      特性
    </h2>
    <div class="grid md:grid-cols-2 gap-4">
      <div
        v-for="ability in abilities"
        :key="ability.id"
        class="rounded-2xl p-5 hover:shadow-xl transition-all duration-300 cursor-default group"
        :class="ability.isHidden ? 'bg-gradient-to-br from-purple-50 to-pink-50 border-2 border-purple-200' : 'bg-gradient-to-br from-slate-50 to-slate-50 border-2 border-slate-100 hover:border-green-200'"
      >
        <div class="flex items-center justify-between mb-3">
          <span class="font-bold text-lg text-slate-900 group-hover:text-green-600 transition-colors">{{ ability.name }}</span>
          <span
            v-if="ability.isHidden"
            class="px-3 py-1 bg-gradient-to-r from-purple-500 to-pink-500 text-white text-xs font-bold rounded-full shadow-sm"
          >
            隐藏
          </span>
        </div>
        <p class="text-slate-600 leading-relaxed">
          {{ ability.description || '暂无描述' }}
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { Sparkles } from 'lucide-vue-next'

export default {
  name: 'PokemonAbilitiesPanel',
  components: { Sparkles },
  props: {
    abilities: {
      type: Array,
      default: () => []
    }
  }
}
</script>
