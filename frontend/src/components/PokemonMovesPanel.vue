<template>
  <div class="bg-white rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80">
    <h2 class="text-xl font-extrabold text-slate-800 mb-6 flex items-center gap-3">
      <div class="w-9 h-9 bg-gradient-to-br from-red-500 to-orange-600 rounded-xl flex items-center justify-center shadow-poke border-2 border-red-400">
        <Zap class="w-5 h-5 text-white" />
      </div>
      可学技能
      <span class="text-lg font-normal text-slate-400 ml-2">({{ moves.length }} 个)</span>
    </h2>

    <!-- 筛选器 -->
    <div class="flex flex-wrap gap-3 mb-6">
      <button
        v-for="filter in moveFilters"
        :key="filter.key"
        class="px-4 py-2 rounded-xl font-medium transition-all duration-300"
        :class="selectedFilter === filter.key
          ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-lg scale-105'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="selectedFilter = filter.key"
      >
        {{ filter.label }}
      </button>
    </div>

    <!-- 加载中 -->
    <div
      v-if="loading"
      class="text-center py-12"
    >
      <el-skeleton
        :rows="5"
        animated
      />
    </div>

    <!-- 技能表格 -->
    <div
      v-else-if="filteredMoves.length"
      class="overflow-x-auto"
    >
      <table class="w-full">
        <thead>
          <tr class="border-b-2 border-gray-100">
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              技能
            </th>
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              属性
            </th>
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              分类
            </th>
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              威力
            </th>
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              命中
            </th>
            <th class="py-4 px-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">
              PP
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="move in filteredMoves"
            :key="move.id"
            class="hover:bg-gradient-to-r from-gray-50 to-blue-50 transition-colors"
          >
            <td class="py-4 px-4">
              <div class="font-bold text-gray-900">
                {{ move.name }}
              </div>
              <div class="text-xs text-gray-500 mt-1">
                <span class="px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-medium">{{ move.learnMethod }}</span>
                <span
                  v-if="move.level"
                  class="ml-2 px-2 py-0.5 bg-green-100 text-green-700 rounded-full text-xs font-medium"
                >Lv.{{ move.level }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <span
                class="px-3 py-1.5 rounded-lg text-white text-sm font-bold shadow-sm"
                :style="{ backgroundColor: move.typeColor }"
              >
                {{ move.typeName }}
              </span>
            </td>
            <td class="py-4 px-4">
              <span
                class="px-3 py-1.5 rounded-lg text-sm font-medium"
                :class="getDamageClassColor(move.damageClass)"
              >
                {{ move.damageClass }}
              </span>
            </td>
            <td class="py-4 px-4">
              <span
                class="text-base font-bold"
                :class="move.power >= 80 ? 'text-red-600' : move.power >= 40 ? 'text-orange-600' : 'text-gray-700'"
              >
                {{ move.power || '-' }}
              </span>
            </td>
            <td class="py-4 px-4 text-base font-medium text-gray-700">
              {{ move.accuracy || '-' }}
            </td>
            <td class="py-4 px-4 text-base font-medium text-gray-700">
              {{ move.pp || '-' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div
      v-else
      class="text-center py-12 text-gray-500 bg-gradient-to-r from-gray-50 to-slate-50 rounded-2xl border-2 border-dashed border-gray-200"
    >
      暂无技能数据
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { Zap } from 'lucide-vue-next'

export default {
  name: 'PokemonMovesPanel',
  components: { Zap },
  props: {
    moves: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    const selectedFilter = ref('all')

    const moveFilters = [
      { key: 'all', label: '全部' },
      { key: '升级', label: '升级' },
      { key: '机器', label: '学习机' },
      { key: '蛋', label: '遗传' },
      { key: '教学', label: '教学' }
    ]

    const filteredMoves = computed(() => {
      if (selectedFilter.value === 'all') return props.moves
      return props.moves.filter(move => {
        const method = move.learnMethod || ''
        return method === selectedFilter.value || method.includes(selectedFilter.value)
      })
    })

    const getDamageClassColor = (damageClass) => {
      const colors = {
        '物理': 'bg-red-100 text-red-700',
        '特殊': 'bg-blue-100 text-blue-700',
        '变化': 'bg-green-100 text-green-700'
      }
      return colors[damageClass] || 'bg-gray-100 text-gray-700'
    }

    return {
      selectedFilter,
      moveFilters,
      filteredMoves,
      getDamageClassColor
    }
  }
}
</script>
