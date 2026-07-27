<template>
  <div
    v-if="stats"
    class="rounded-3xl shadow-poke-card p-8 mb-6 border-3 border-slate-200/80 bg-white"
  >
    <h2 class="text-xl font-extrabold text-slate-800 mb-6 flex items-center gap-3">
      <div class="w-9 h-9 bg-gradient-to-br from-poke-red to-red-600 rounded-xl flex items-center justify-center shadow-poke border-2 border-red-400">
        <BarChart3 class="w-4.5 h-4.5 text-white" />
      </div>
      种族值
      <button
        class="ml-auto px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-xl text-sm font-bold transition-colors border-2 border-slate-200 shadow-poke"
        @click="$emit('toggle-view')"
      >
        {{ viewMode === 'bar' ? '📊 雷达图' : '📈 进度条' }}
      </button>
    </h2>

    <!-- 进度条视图 - Pokemon 正作风格 -->
    <div
      v-if="viewMode === 'bar'"
      class="space-y-3"
    >
      <div
        v-for="bar in bars"
        :key="bar.label"
        class="flex items-center gap-3"
      >
        <span class="w-12 text-xs font-extrabold text-slate-500 uppercase tracking-wider text-right">{{ bar.label }}</span>
        <div class="flex-1 bg-slate-100 rounded-full h-5 overflow-hidden shadow-poke-inset relative">
          <div
            class="h-full rounded-full transition-all duration-700 ease-out relative overflow-hidden"
            :style="{ width: Math.max(2, bar.value / 255 * 100) + '%', backgroundColor: bar.color }"
          >
            <!-- 高光 -->
            <div class="absolute inset-0 bg-gradient-to-b from-white/30 to-transparent" />
            <!-- 条纹 -->
            <div class="absolute inset-0 opacity-20" style="background-image: repeating-linear-gradient(90deg, transparent, transparent 4px, rgba(0,0,0,0.1) 4px, rgba(0,0,0,0.1) 8px);" />
          </div>
          <!-- 数字 -->
          <div class="absolute inset-0 flex items-center justify-end pr-2">
            <span class="text-[10px] font-extrabold text-slate-700 drop-shadow-[0_1px_0_rgba(255,255,255,0.8)]">
              {{ bar.value || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 雷达图视图 -->
    <div
      v-else
      class="flex justify-center py-4"
    >
      <svg
        viewBox="0 0 300 300"
        class="w-full max-w-md"
      >
        <!-- 网格 -->
        <polygon
          v-for="level in 5"
          :key="'grid-' + level"
          :points="getPolygonPoints(level / 5 * 200)"
          fill="none"
          stroke="#e2e8f0"
          stroke-width="1"
          :stroke-dasharray="level === 5 ? 'none' : '4 4'"
        />
        <!-- 轴线 -->
        <line
          v-for="(axis, i) in axisLabels"
          :key="'axis-' + i"
          :x1="150"
          :y1="150"
          :x2="radarX(100, i)"
          :y2="radarY(100, i)"
          stroke="#e2e8f0"
          stroke-width="1"
        />
        <!-- 数据区域 -->
        <polygon
          :points="getDataPoints()"
          fill="rgba(220, 38, 38, 0.15)"
          stroke="#DC2626"
          stroke-width="2.5"
        />
        <!-- 数据点 -->
        <circle
          v-for="i in 6"
          :key="'dot-' + i"
          :cx="radarX(statValues[i-1] / 255 * 100, i-1)"
          :cy="radarY(statValues[i-1] / 255 * 100, i-1)"
          r="5"
          fill="#DC2626"
          stroke="#fff"
          stroke-width="2"
        />
        <!-- 标签 -->
        <text
          v-for="(label, i) in axisLabels"
          :key="'label-' + i"
          :x="radarX(125, i)"
          :y="radarY(125, i)"
          text-anchor="middle"
          dominant-baseline="middle"
          class="text-[11px] font-bold fill-slate-600"
        >
          {{ label }}
        </text>
      </svg>
    </div>

    <!-- 种族值总和 -->
    <div class="mt-6 text-center bg-gradient-to-r from-red-50 to-orange-50 rounded-2xl p-4 border-2 border-red-100 shadow-poke">
      <span class="text-3xl font-extrabold text-poke-red">{{ stats.total }}</span>
      <span class="text-slate-500 ml-2 text-sm font-bold uppercase tracking-wider">种族值总和</span>
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
        { label: 'HP',     value: s.hp,         color: '#ef4444' },
        { label: '攻击',  value: s.attack,     color: '#F08030' },
        { label: '防御',  value: s.defense,    color: '#F8D030' },
        { label: '特攻',  value: s.spAttack,  color: '#6890F0' },
        { label: '特防',  value: s.spDefense,  color: '#78C850' },
        { label: '速度',  value: s.speed,      color: '#F85888' }
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
