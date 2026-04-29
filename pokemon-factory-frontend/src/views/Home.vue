<template>
  <div class="mx-auto max-w-5xl">
    <div class="mb-6">
      <h2 class="text-xl font-bold text-slate-800 sm:text-2xl">{{ tr('欢迎使用 Pokemon Factory', 'Welcome to Pokemon Factory') }}</h2>
      <p class="mt-1 text-sm text-slate-500">{{ tr('宝可梦图鉴与对战模拟平台', 'Pokemon Dex & Battle Simulator') }}</p>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <router-link
        v-for="card in cards"
        :key="card.path"
        :to="card.path"
        class="group rounded-xl border border-slate-200 bg-white p-5 transition hover:border-slate-300 hover:shadow-md"
      >
        <div class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-lg text-lg"
            :style="{ background: card.color + '15', color: card.color }"
          >
            {{ card.icon }}
          </div>
          <div>
            <h3 class="font-semibold text-slate-800 group-hover:text-slate-900">{{ card.name }}</h3>
            <p class="text-xs text-slate-500">{{ card.desc }}</p>
          </div>
        </div>
      </router-link>
    </div>

    <div class="mt-10 rounded-xl border border-slate-200 bg-white p-5">
      <h3 class="font-semibold text-slate-700">{{ tr('数据统计', 'Data Stats') }}</h3>
      <div class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div v-for="stat in stats" :key="stat.label">
          <div class="text-2xl font-bold text-slate-800">{{ stat.value }}</div>
          <div class="text-xs text-slate-500">{{ stat.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useLocale } from '../composables/useLocale'
import { onMounted, ref } from 'vue'

const { translate: tr } = useLocale()

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
    const res = await fetch('/api/pokedex/summary')
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
