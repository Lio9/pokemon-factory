<template>
  <div class="mx-auto max-w-6xl">
    <!-- Hero Section - Pokemon 正作风格 -->
    <div class="mb-8 rounded-3xl overflow-hidden relative shadow-2xl">
      <!-- 背景层 -->
      <div class="absolute inset-0 bg-gradient-to-br from-red-600 via-red-500 to-orange-400" />
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-0 right-0 w-96 h-96 -translate-y-1/2 translate-x-1/3">
          <svg
            viewBox="0 0 400 400"
            class="w-full h-full"
          >
            <circle
              cx="200"
              cy="200"
              r="180"
              fill="none"
              stroke="#fff"
              stroke-width="8"
              opacity="0.3"
            />
            <line
              x1="20"
              y1="200"
              x2="380"
              y2="200"
              stroke="#fff"
              stroke-width="8"
              opacity="0.3"
            />
            <circle
              cx="200"
              cy="200"
              r="40"
              fill="none"
              stroke="#fff"
              stroke-width="8"
              opacity="0.3"
            />
            <circle
              cx="200"
              cy="200"
              r="20"
              fill="#fff"
              opacity="0.2"
            />
          </svg>
        </div>
      </div>
      <div class="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent" />

      <div class="relative z-10 p-8 sm:p-12">
        <div class="flex flex-col lg:flex-row items-center gap-8">
          <div class="flex-1 text-white">
            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/20 backdrop-blur-sm text-sm font-bold mb-4 border border-white/20">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              {{ tr('v1.0 已上线', 'v1.0 Live') }}
            </div>
            <h2 class="text-3xl font-extrabold sm:text-5xl mb-3 drop-shadow-lg leading-tight">
              {{ tr('欢迎来到', 'Welcome to') }}<br>
              <span class="text-yellow-300">Pokemon Factory</span>
            </h2>
            <p class="text-lg text-red-100 sm:text-xl mb-6 max-w-lg">
              {{ tr('宝可梦图鉴、技能查询、伤害计算与对战模拟平台', 'Pokemon Dex, Move Lookup, Damage Calc & Battle Simulator') }}
            </p>
            <div class="flex flex-wrap gap-3">
              <router-link
                to="/pokemon"
                class="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 font-extrabold text-red-600 shadow-lg transition-all hover:scale-105 hover:shadow-xl border-2 border-white/50"
              >
                <svg
                  viewBox="0 0 100 100"
                  class="w-5 h-5"
                >
                  <circle
                    cx="50"
                    cy="50"
                    r="46"
                    fill="#DC2626"
                    stroke="#1a1a1a"
                    stroke-width="4"
                  />
                  <rect
                    x="4"
                    y="48"
                    width="92"
                    height="4"
                    fill="#1a1a1a"
                  />
                  <circle
                    cx="50"
                    cy="50"
                    r="12"
                    fill="#fff"
                    stroke="#1a1a1a"
                    stroke-width="4"
                  />
                </svg>
                {{ tr('浏览图鉴', 'Browse Dex') }}
              </router-link>
              <router-link
                to="/battle"
                class="inline-flex items-center gap-2 rounded-xl border-2 border-white/60 px-6 py-3 font-bold text-white transition-all hover:bg-white/15 hover:border-white"
              >
                <span class="text-xl">⚔️</span>
                {{ tr('开始对战', 'Start Battle') }}
              </router-link>
            </div>
          </div>

          <!-- 右侧装饰宝可梦精灵球 -->
          <div class="hidden lg:flex items-center justify-center">
            <div class="relative w-48 h-48 float-animation">
              <svg
                viewBox="0 0 200 200"
                class="w-full h-full drop-shadow-2xl"
              >
                <circle
                  cx="100"
                  cy="100"
                  r="90"
                  fill="#DC2626"
                  stroke="#1a1a1a"
                  stroke-width="6"
                />
                <rect
                  x="10"
                  y="97"
                  width="180"
                  height="6"
                  fill="#1a1a1a"
                />
                <circle
                  cx="100"
                  cy="100"
                  r="90"
                  fill="none"
                  stroke="#1a1a1a"
                  stroke-width="6"
                />
                <circle
                  cx="100"
                  cy="100"
                  r="22"
                  fill="#fff"
                  stroke="#1a1a1a"
                  stroke-width="6"
                />
                <circle
                  cx="100"
                  cy="100"
                  r="10"
                  fill="#fff"
                  stroke="#1a1a1a"
                  stroke-width="3"
                />
                <!-- 高光 -->
                <ellipse
                  cx="70"
                  cy="50"
                  rx="25"
                  ry="15"
                  fill="rgba(255,255,255,0.2)"
                  transform="rotate(-30 70 50)"
                />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能卡片网格 - Pokemon 正作风格 -->
    <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <router-link
        v-for="card in cards"
        :key="card.path"
        :to="card.path"
        class="group relative overflow-hidden rounded-2xl border-3 bg-white p-6 shadow-poke-card transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
        :style="{ borderColor: card.color + '40' }"
      >
        <!-- 顶部属性色条 -->
        <div
          class="absolute top-0 left-0 right-0 h-1.5 rounded-t-2xl"
          :style="{ background: `linear-gradient(90deg, ${card.color}, ${card.colorLight})` }"
        />

        <!-- Pokeball 纹理 -->
        <div class="absolute -right-8 -bottom-8 w-32 h-32 opacity-[0.03] group-hover:opacity-[0.06] transition-opacity">
          <svg
            viewBox="0 0 100 100"
            class="w-full h-full"
          >
            <circle
              cx="50"
              cy="50"
              r="46"
              fill="none"
              :stroke="card.color"
              stroke-width="4"
            />
            <line
              x1="4"
              y1="50"
              x2="96"
              y2="50"
              :stroke="card.color"
              stroke-width="4"
            />
            <circle
              cx="50"
              cy="50"
              r="12"
              fill="none"
              :stroke="card.color"
              stroke-width="4"
            />
          </svg>
        </div>

        <div class="relative z-10 flex items-start gap-4">
          <div
            class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl text-2xl shadow-poke transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3 border-2"
            :style="{ background: `${card.color}12`, borderColor: `${card.color}30`, color: card.color }"
          >
            {{ card.icon }}
          </div>
          <div class="flex-1">
            <h3 class="text-lg font-extrabold text-slate-800 transition-colors group-hover:text-slate-900">
              {{ card.name }}
            </h3>
            <p class="mt-1 text-sm text-slate-500 font-medium">
              {{ card.desc }}
            </p>
            <div
              class="mt-3 flex items-center gap-1 text-xs font-bold"
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
                  stroke-width="2.5"
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </div>
          </div>
        </div>
      </router-link>
    </div>

    <!-- 数据统计卡片 - Pokeball 配色 -->
    <div class="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
      <div
        v-for="(stat, index) in stats"
        :key="stat.label"
        class="group relative overflow-hidden rounded-2xl border-3 border-slate-200/60 bg-white p-6 shadow-poke-card transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
      >
        <div
          class="absolute -right-6 -top-6 h-24 w-24 rounded-full opacity-10 transition-transform duration-500 group-hover:scale-150"
          :style="{ background: statColors[index] }"
        />
        <div class="relative z-10">
          <div class="text-3xl font-extrabold text-slate-800">
            {{ stat.value }}
          </div>
          <div class="mt-1 text-sm font-bold text-slate-500 uppercase tracking-wide">
            {{ stat.label }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'
import { API_BASE, request } from '../services/httpClient'
import { onMounted, ref, computed } from 'vue'

const { translate: tr } = useLocale()

// 统计卡片渐变色
const statColors = [
  'linear-gradient(135deg, #DC2626, #F59E0B)',
  'linear-gradient(135deg, #F08030, #F8D030)',
  'linear-gradient(135deg, #6890F0, #A890F0)',
  'linear-gradient(135deg, #78C850, #A8B820)'
]

const cards = computed(() => [
  { name: tr('宝可梦图鉴', 'Pokemon Dex'), desc: tr('浏览和搜索宝可梦', 'Browse and search Pokemon'), path: '/pokemon', icon: '📖', color: '#DC2626', colorLight: '#F87171' },
  { name: tr('技能列表', 'Moves'), desc: tr('查询招式数据', 'Move data lookup'), path: '/moves', icon: '🔥', color: '#F08030', colorLight: '#FDBA74' },
  { name: tr('特性列表', 'Abilities'), desc: tr('查看特性效果', 'Ability details'), path: '/abilities', icon: '✨', color: '#705898', colorLight: '#A78BFA' },
  { name: tr('物品列表', 'Items'), desc: tr('道具数据一览', 'Item catalog'), path: '/items', icon: '🎒', color: '#E0C068', colorLight: '#FDE68A' },
  { name: tr('伤害计算器', 'Damage Calc'), desc: tr('模拟招式伤害', 'Simulate move damage'), path: '/damage-calculator', icon: '📊', color: '#6890F0', colorLight: '#93C5FD' },
  { name: tr('对战工厂', 'Battle'), desc: tr('3D 对战体验', '3D battle experience'), path: '/battle', icon: '🏟️', color: '#78C850', colorLight: '#86EFAC' },
])

const stats = ref([
  { label: tr('宝可梦', 'Pokemon'), value: '-' },
  { label: tr('招式', 'Moves'), value: '-' },
  { label: tr('特性', 'Abilities'), value: '-' },
  { label: tr('物品', 'Items'), value: '-' },
])

onMounted(async () => {
  try {
    const data = await request(`${API_BASE}/summary`)
    const d = data.data ?? data
      stats.value = [
        { label: tr('宝可梦', 'Pokemon'), value: d.pokemonCount ?? d.pokemon ?? '-' },
        { label: tr('招式', 'Moves'), value: d.moveCount ?? d.moves ?? '-' },
        { label: tr('特性', 'Abilities'), value: d.abilityCount ?? d.abilities ?? '-' },
        { label: tr('物品', 'Items'), value: d.itemCount ?? d.items ?? '-' },
      ]
  } catch {
    // stats stay as "-"
  }
})
</script>
