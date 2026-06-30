<template>
  <div class="mx-auto max-w-6xl">
    <!-- Hero Section -->
    <div class="mb-8 rounded-3xl bg-gradient-to-br from-blue-600 via-indigo-600 to-purple-700 p-8 text-white shadow-2xl sm:p-12 relative overflow-hidden">
      <!-- 装饰性背景元素 -->
      <div class="absolute top-0 right-0 -mt-10 -mr-10 h-40 w-40 rounded-full bg-white/10 blur-3xl" />
      <div class="absolute bottom-0 left-0 -mb-10 -ml-10 h-40 w-40 rounded-full bg-white/10 blur-3xl" />
      
      <div class="relative z-10">
        <h2 class="text-3xl font-bold sm:text-4xl mb-2">
          {{ tr('欢迎使用 Pokemon Factory', 'Welcome to Pokemon Factory') }}
        </h2>
        <p class="text-lg text-blue-100 sm:text-xl">
          {{ tr('宝可梦图鉴与对战模拟平台', 'Pokemon Dex & Battle Simulator') }}
        </p>
        <div class="mt-6 flex flex-wrap gap-3">
          <router-link
            to="/battle"
            class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 font-semibold text-blue-600 shadow-lg transition-all hover:scale-105 hover:shadow-xl"
          >
            <span>⚔️</span>
            <span>{{ tr('开始对战', 'Start Battle') }}</span>
          </router-link>
          <router-link
            to="/pokemon"
            class="inline-flex items-center gap-2 rounded-xl border-2 border-white/50 px-6 py-3 font-semibold text-white transition-all hover:bg-white/10 hover:border-white"
          >
            <span>📖</span>
            <span>{{ tr('浏览图鉴', 'Browse Dex') }}</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 功能卡片网格 -->
    <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <router-link
        v-for="card in cards"
        :key="card.path"
        :to="card.path"
        class="group relative overflow-hidden rounded-2xl border border-slate-200/60 bg-white/80 p-6 shadow-lg backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-blue-300/50 hover:shadow-2xl"
      >
        <!-- 渐变背景遮罩 -->
        <div 
          class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-10"
          :style="{ background: `linear-gradient(135deg, ${card.color}20, ${card.color}40)` }"
        />
        
        <div class="relative z-10 flex items-start gap-4">
          <div
            class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl text-2xl shadow-md transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3"
            :style="{ background: `${card.color}15`, color: card.color }"
          >
            {{ card.icon }}
          </div>
          <div class="flex-1">
            <h3 class="text-lg font-bold text-slate-800 transition-colors group-hover:text-slate-900">
              {{ card.name }}
            </h3>
            <p class="mt-1 text-sm text-slate-500">
              {{ card.desc }}
            </p>
            <div
              class="mt-3 flex items-center gap-1 text-xs font-medium"
              :style="{ color: card.color }"
            >
              <span>{{ tr('查看详情', 'View Details') }}</span>
              <svg
                class="h-4 w-4 transition-transform group-hover:translate-x-1"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </div>
          </div>
        </div>
      </router-link>
    </div>

    <!-- 数据统计卡片 -->
    <div class="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <div 
        v-for="(stat, index) in stats" 
        :key="stat.label"
        class="group relative overflow-hidden rounded-2xl border border-slate-200/60 bg-white/80 p-6 shadow-lg backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
      >
        <div 
          class="absolute -right-6 -top-6 h-24 w-24 rounded-full opacity-10 transition-transform duration-500 group-hover:scale-150"
          :style="{ background: statColors[index] }"
        />
        <div class="relative z-10">
          <div class="text-3xl font-bold text-slate-800">
            {{ stat.value }}
          </div>
          <div class="mt-1 text-sm font-medium text-slate-500">
            {{ stat.label }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'
import { API_BASE } from '../services/httpClient'
import { onMounted, ref } from 'vue'

const { translate: tr } = useLocale()

// 统计卡片渐变色
const statColors = [
  'linear-gradient(135deg, #3b82f6, #8b5cf6)',
  'linear-gradient(135deg, #ef4444, #f59e0b)',
  'linear-gradient(135deg, #8b5cf6, #ec4899)',
  'linear-gradient(135deg, #f59e0b, #10b981)'
]

const cards = [
  { name: tr('宝可梦图鉴', 'Pokemon Dex'), desc: tr('浏览和搜索宝可梦', 'Browse and search Pokemon'), path: '/pokemon', icon: '⚡', color: '#3b82f6' },
  { name: tr('技能列表', 'Moves'), desc: tr('查询招式数据', 'Move data lookup'), path: '/moves', icon: '🔥', color: '#ef4444' },
  { name: tr('特性列表', 'Abilities'), desc: tr('查看特性效果', 'Ability details'), path: '/abilities', icon: '✨', color: '#8b5cf6' },
  { name: tr('物品列表', 'Items'), desc: tr('道具数据一览', 'Item catalog'), path: '/items', icon: '🎒', color: '#f59e0b' },
  { name: tr('伤害计算器', 'Damage Calc'), desc: tr('模拟招式伤害', 'Simulate move damage'), path: '/damage-calculator', icon: '📊', color: '#06b6d4' },
  { name: tr('对战工厂', 'Battle'), desc: tr('双打对战模拟', 'Doubles battle sim'), path: '/battle', icon: '🏟️', color: '#10b981' },
]

const stats = ref([
  { label: tr('宝可梦', 'Pokemon'), value: '-' },
  { label: tr('招式', 'Moves'), value: '-' },
  { label: tr('特性', 'Abilities'), value: '-' },
  { label: tr('物品', 'Items'), value: '-' },
])

onMounted(async () => {
  try {
    const res = await fetch(`${API_BASE}/summary`)
    if (res.ok) {
      const data = await res.json()
      stats.value = [
        { label: tr('宝可梦', 'Pokemon'), value: data.pokemon ?? '-' },
        { label: tr('招式', 'Moves'), value: data.moves ?? '-' },
        { label: tr('特性', 'Abilities'), value: data.abilities ?? '-' },
        { label: tr('物品', 'Items'), value: data.items ?? '-' },
      ]
    }
  } catch {
    // stats stay as "-"
  }
})
</script>
