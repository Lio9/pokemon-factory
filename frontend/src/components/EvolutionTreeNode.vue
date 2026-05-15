<template>
  <div class="evolution-node">
    <!-- 当前节点 -->
    <div class="flex flex-col items-center gap-2 min-w-[100px]">
      <router-link
        :to="'/pokemon/' + node.speciesId"
        class="group flex flex-col items-center gap-2"
        :class="{ 'scale-110 ring-2 ring-indigo-400 ring-offset-2 rounded-2xl': node.isCurrent }"
      >
        <div class="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center p-2 transition-all duration-300 group-hover:shadow-lg group-hover:-translate-y-1">
          <img
            :src="node.spriteUrl || '/images/pokemon-placeholder.svg'"
            :alt="node.pokemonName"
            class="w-full h-full object-contain"
          >
        </div>
        <span class="text-sm font-semibold text-slate-800 text-center group-hover:text-indigo-600 transition-colors">
          {{ node.pokemonName }}
        </span>
      </router-link>

      <!-- 进化条件 -->
      <div v-if="node.trigger" class="flex flex-col items-center gap-0.5">
        <span class="text-xs font-medium text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full whitespace-nowrap">
          {{ node.trigger }}
        </span>
        <span v-if="node.minLevel" class="text-xs text-slate-500">
          Lv.{{ node.minLevel }}
        </span>
        <span v-if="node.item" class="text-xs text-slate-500">
          {{ node.item }}
        </span>
      </div>
    </div>

    <!-- 子节点（分支进化） -->
    <div v-if="node.children && node.children.length > 0" class="evolution-children">
      <!-- 连接箭头 -->
      <div class="flex justify-center items-center py-2">
        <svg class="w-6 h-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
        </svg>
      </div>

      <!-- 多个分支：水平排列 -->
      <div class="flex flex-wrap justify-center gap-4 sm:gap-6 lg:gap-8">
        <EvolutionTreeNode
          v-for="child in node.children"
          :key="child.speciesId"
          :node="child"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  node: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.evolution-node {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.evolution-children {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

@media (max-width: 640px) {
  .evolution-children > div {
    gap: 0.75rem;
  }
}
</style>
