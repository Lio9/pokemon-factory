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
    <div
      v-if="viewMode === 'bar'"
      class="space-y-4"
    >
      <StatBar
        label="HP"
        :value="stats.hp"
        color="#FF6B6B"
      />
      <StatBar
        label="攻击"
        :value="stats.attack"
        color="#FFA94D"
      />
      <StatBar
        label="防御"
        :value="stats.defense"
        color="#FFD43B"
      />
      <StatBar
        label="特攻"
        :value="stats.spAttack"
        color="#4DABF7"
      />
      <StatBar
        label="特防"
        :value="stats.spDefense"
        color="#69DB7C"
      />
      <StatBar
        label="速度"
        :value="stats.speed"
        color="#F783AC"
      />
    </div>

    <!-- 雷达图视图 -->
    <div
      v-else
      class="flex justify-center py-4"
    >
      <RadarChart :stats="stats" />
    </div>

    <div class="mt-8 text-center bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-4 border border-blue-100">
      <span class="text-4xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">{{ stats.total }}</span>
      <span class="text-gray-600 ml-3 text-lg">种族值总和</span>
    </div>
  </div>
</template>

<script>
import { BarChart3 } from 'lucide-vue-next'

// 种族值条组件
const StatBar = {
  props: ['label', 'value', 'color'],
  template: `
    <div class="flex items-center gap-4">
      <span class="w-14 text-sm font-bold text-gray-700">{{ label }}</span>
      <div class="flex-1 bg-gray-200 rounded-full h-4 overflow-hidden shadow-inner">
        <div
          class="h-full rounded-full transition-all duration-700 ease-out relative"
          :style="{ width: (value / 255 * 100) + '%', backgroundColor: color }"
        >
          <div class="absolute inset-0 bg-gradient-to-r from-white/30 to-transparent"></div>
        </div>
      </div>
      <span class="w-12 text-sm font-bold text-right text-gray-900">{{ value || 0 }}</span>
    </div>
  `
}

// 雷达图组件
const RadarChart = {
  props: ['stats'],
  template: `
    <svg viewBox="0 0 300 300" class="w-full max-w-md">
      <!-- 背景网格 -->
      <polygon
        v-for="level in 5"
        :points="getPolygonPoints(level / 5 * 200)"
        fill="none"
        stroke="#E5E7EB"
        stroke-width="1"
      />
      <!-- 轴线 -->
      <line
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :x1="150"
        :y1="150"
        :x2="150 + (i % 2 === 0 ? 1 : -1) * 100 * Math.cos(Math.PI * i / 3)"
        :y2="150 - 100 * Math.sin(Math.PI * i / 3)"
        stroke="#E5E7EB"
        stroke-width="1"
      />
      <!-- 数据区域 -->
      <polygon
        :points="getDataPoints()"
        fill="rgba(59, 130, 246, 0.3)"
        stroke="#3B82F6"
        stroke-width="2"
      />
      <!-- 数据点 -->
      <circle
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :cx="150 + (i % 2 === 0 ? 1 : -1) * getStatValue(i) / 255 * 100 * Math.cos(Math.PI * i / 3)"
        :cy="150 - getStatValue(i) / 255 * 100 * Math.sin(Math.PI * i / 3)"
        r="5"
        fill="#3B82F6"
      />
      <!-- 标签 -->
      <text
        v-for="(label, i) in ['HP', '攻击', '防御', '特攻', '特防', '速度']"
        :x="150 + (i % 2 === 0 ? 1 : -1) * 120 * Math.cos(Math.PI * i / 3)"
        :y="150 - 120 * Math.sin(Math.PI * i / 3)"
        text-anchor="middle"
        dominant-baseline="middle"
        class="text-xs font-bold fill-gray-600"
      >
        {{ label }}
      </text>
    </svg>
  `,
  methods: {
    getPolygonPoints(radius) {
      const points = []
      for (let i = 0; i < 6; i++) {
        const x = 150 + (i % 2 === 0 ? 1 : -1) * radius * Math.cos(Math.PI * i / 3)
        const y = 150 - radius * Math.sin(Math.PI * i / 3)
        points.push(`${x},${y}`)
      }
      return points.join(' ')
    },
    getStatValue(index) {
      const values = [
        this.stats?.hp || 0,
        this.stats?.attack || 0,
        this.stats?.defense || 0,
        this.stats?.spAttack || 0,
        this.stats?.spDefense || 0,
        this.stats?.speed || 0
      ]
      return values[index] || 0
    },
    getDataPoints() {
      const points = []
      for (let i = 0; i < 6; i++) {
        const value = this.getStatValue(i)
        const x = 150 + (i % 2 === 0 ? 1 : -1) * value / 255 * 100 * Math.cos(Math.PI * i / 3)
        const y = 150 - value / 255 * 100 * Math.sin(Math.PI * i / 3)
        points.push(`${x},${y}`)
      }
      return points.join(' ')
    }
  }
}

export default {
  name: 'PokemonStatsPanel',
  components: { BarChart3, StatBar, RadarChart },
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
  emits: ['toggle-view']
}
</script>
