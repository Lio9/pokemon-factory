<template>
  <div
    v-if="stats"
    class="bg-white rounded-3xl shadow-xl p-8 mb-6 border border-gray-100"
  >
    <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-3">
      <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center">
        <BarChart3 class="w-5 h-5 text-white" />
      </div>
      种族值
      <button
        class="ml-auto px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-xl text-sm font-medium transition-colors"
        @click="$emit('toggle-view')"
      >
        {{ viewMode === 'bar' ? '雷达图' : '进度条' }}
      </button>
    </h2>

    <!-- 进度条视图 -->
    <div v-if="viewMode === 'bar'" class="space-y-4">
      <div v-for="bar in bars" :key="bar.label" class="flex items-center gap-4">
        <span class="w-14 text-sm font-bold text-gray-700">{{ bar.label }}</span>
        <div class="flex-1 bg-gray-200 rounded-full h-4 overflow-hidden shadow-inner">
          <div
            class="h-full rounded-full transition-all duration-700 ease-out relative"
            :style="{ width: (bar.value / 255 * 100) + '%', backgroundColor: bar.color }"
          >
            <div class="absolute inset-0 bg-gradient-to-r from-white/30 to-transparent"></div>
          </div>
        </div>
        <span class="w-12 text-sm font-bold text-right text-gray-900">{{ bar.value || 0 }}</span>
      </div>
    </div>

    <!-- 雷达图视图 -->
    <div v-else class="flex justify-center py-4">
      <svg viewBox="0 0 300 300" class="w-full max-w-md">
        <polygon
          v-for="level in 5"
          :key="'grid-' + level"
          :points="getPolygonPoints(level / 5 * 200)"
          fill="none"
          stroke="#E5E7EB"
          stroke-width="1"
        />
        <line
          v-for="(axis, i) in axisLabels"
          :key="'axis-' + i"
          :x1="150"
          :y1="150"
          :x2="radarX(100, i)"
          :y2="radarY(100, i)"
          stroke="#E5E7EB"
          stroke-width="1"
        />
        <polygon
          :points="getDataPoints()"
          fill="rgba(59, 130, 246, 0.3)"
          stroke="#3B82F6"
          stroke-width="2"
        />
        <circle
          v-for="i in 6"
          :key="'dot-' + i"
          :cx="radarX(statValues[i-1] / 255 * 100, i-1)"
          :cy="radarY(statValues[i-1] / 255 * 100, i-1)"
          r="5"
          fill="#3B82F6"
        />
        <text
          v-for="(label, i) in axisLabels"
          :key="'label-' + i"
          :x="radarX(120, i)"
          :y="radarY(120, i)"
          text-anchor="middle"
          dominant-baseline="middle"
          class="text-xs font-bold fill-gray-600"
        >
          {{ label }}
        </text>
      </svg>
    </div>

    <div class="mt-8 text-center bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-4 border border-blue-100">
      <span class="text-4xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">{{ stats.total }}</span>
      <span class="text-gray-600 ml-3 text-lg">种族值总和</span>
    </div>
  </div>
</template>

<script>
import { BarChart3 } from 'lucide-vue-next'

export default {
  name: 'PokemonStatsPanel',
  components: { BarChart3 },
  props: {
    stats: {
      type: Object,
      default: null
    },
    viewMode: {
      type: String,
      default: 'bar'
    }
  },
  emits: ['toggle-view'],
  computed: {
    bars() {
      const s = this.stats || {}
      return [
        { label: 'HP',     value: s.hp,         color: '#FF6B6B' },
        { label: '攻击',  value: s.attack,     color: '#FFA94D' },
        { label: '防御',  value: s.defense,    color: '#FFD43B' },
        { label: '特攻',  value: s.spAttack,  color: '#4DABF7' },
        { label: '特防',  value: s.spDefense,  color: '#69DB7C' },
        { label: '速度',  value: s.speed,      color: '#F783AC' }
      ]
    },
    statValues() {
      const s = this.stats || {}
      return [
        s.hp || 0, s.attack || 0, s.defense || 0,
        s.spAttack || 0, s.spDefense || 0, s.speed || 0
      ]
    },
    axisLabels() {
      return ['HP', '攻击', '防御', '特攻', '特防', '速度']
    }
  },
  methods: {
    radarX(radius, i) {
      const angle = Math.PI * i / 3 - Math.PI / 2
      return 150 + radius * Math.cos(angle)
    },
    radarY(radius, i) {
      const angle = Math.PI * i / 3 - Math.PI / 2
      return 150 + radius * Math.sin(angle)
    },
    getPolygonPoints(radius) {
      const points = []
      for (let i = 0; i < 6; i++) {
        points.push(`${this.radarX(radius, i)},${this.radarY(radius, i)}`)
      }
      return points.join(' ')
    },
    getDataPoints() {
      const points = []
      for (let i = 0; i < 6; i++) {
        const r = this.statValues[i] / 255 * 100
        points.push(`${this.radarX(r, i)},${this.radarY(r, i)}`)
      }
      return points.join(' ')
    }
  }
}
</script>
