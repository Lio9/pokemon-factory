<template>
  <div
    v-if="show"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
  >
    <div class="relative w-full max-w-2xl max-h-[90vh] overflow-hidden rounded-2xl bg-white shadow-2xl">
      <!-- 头部 -->
      <div class="bg-gradient-to-r from-rose-600 to-pink-600 px-6 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-bold text-white">
              🚫 Ban 宝可梦
            </h2>
            <p class="text-sm text-white/80">
              选择不想在对手队伍中遇到的宝可梦
            </p>
          </div>
          <button
            class="rounded-lg p-2 text-white/80 hover:bg-white/20 hover:text-white transition"
            @click="$emit('close')"
          >
            <svg
              class="h-5 w-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <!-- 积分信息 -->
      <div class="border-b border-slate-200 bg-slate-50 px-6 py-3">
        <div class="flex items-center justify-between">
          <div class="text-sm text-slate-600">
            当前积分：<span class="font-bold text-slate-900">{{ playerPoints }}</span>
          </div>
          <div class="text-sm text-slate-600">
            已 Ban：<span class="font-bold text-rose-600">{{ bannedList.length }}</span> / {{ maxSlots }}
          </div>
          <div class="text-sm text-slate-600">
            本次费用：<span
              class="font-bold"
              :class="totalCost > 0 ? 'text-amber-600' : 'text-emerald-600'"
            >{{ totalCost > 0 ? totalCost + ' 积分' : '免费' }}</span>
          </div>
        </div>
      </div>

      <!-- Ban 槽位 -->
      <div class="border-b border-slate-200 px-6 py-4">
        <div class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
          Ban 槽位
        </div>
        <div class="grid grid-cols-3 sm:grid-cols-6 gap-2">
          <div
            v-for="slot in slots"
            :key="slot.index"
            class="relative flex flex-col items-center justify-center rounded-xl border-2 p-3 transition-all cursor-pointer"
            :class="getSlotClass(slot)"
            @click="handleSlotClick(slot)"
          >
            <div class="text-lg mb-1">
              {{ slot.banned ? '🚫' : '➕' }}
            </div>
            <div class="text-[10px] font-bold text-center">
              {{ slot.banned ? slot.pokemonName : `第${slot.index +1}只` }}
            </div>
            <div
              v-if="slot.cost > 0"
              class="mt-1 text-[10px] px-1.5 py-0.5 rounded-full"
              :class="slot.affordable ? 'bg-amber-100 text-amber-700' : 'bg-red-100 text-red-700'"
            >
              {{ slot.cost }}分
            </div>
            <div
              v-else
              class="mt-1 text-[10px] px-1.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700"
            >
              免费
            </div>
          </div>
        </div>
      </div>

      <!-- 宝可梦选择列表 -->
      <div
        class="overflow-y-auto px-6 py-4"
        style="max-height: 40vh;"
      >
        <div class="mb-3 flex items-center gap-2">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索宝可梦..."
            class="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
          >
          <select
            v-model="rarityFilter"
            class="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">
              全部稀有度
            </option>
            <option value="common">
              普通
            </option>
            <option value="rare">
              稀有
            </option>
            <option value="epic">
              史话
            </option>
            <option value="legend">
              传说
            </option>
          </select>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
          <button
            v-for="pokemon in filteredPokemon"
            :key="pokemon.name_en"
            class="flex items-center gap-2 rounded-xl border p-2 text-left text-sm transition-all"
            :class="isBanned(pokemon) ? 'border-rose-300 bg-rose-50' : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'"
            @click="toggleBan(pokemon)"
          >
            <img
              :src="getSpriteUrl(pokemon)"
              class="h-8 w-8 object-contain"
              @error="$event.target.src='/placeholder.png'"
            >
            <div class="flex-1 min-w-0">
              <div class="font-medium text-slate-800 truncate">
                {{ pokemon.name || pokemon.name_en }}
              </div>
              <div class="text-[10px] text-slate-400">
                #{{ pokemon.id }}
              </div>
            </div>
            <div
              v-if="isBanned(pokemon)"
              class="text-rose-500 text-xs font-bold"
            >
              已Ban
            </div>
          </button>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="border-t border-slate-200 bg-slate-50 px-6 py-4">
        <div class="flex items-center justify-between">
          <button
            class="text-sm text-slate-500 hover:text-slate-700 transition"
            @click="clearAllBans"
          >
            清空所有 Ban
          </button>
          <div class="flex gap-2">
            <button
              class="rounded-lg px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-200 transition"
              @click="$emit('close')"
            >
              取消
            </button>
            <button
              class="rounded-lg bg-rose-600 px-6 py-2 text-sm font-bold text-white hover:bg-rose-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!canConfirm"
              @click="confirmBans"
            >
              确认 Ban ({{ bannedList.length }}只)
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  playerPoints: { type: Number, default: 0 },
  pokemonList: { type: Array, default: () => [] },
  initialBans: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'confirm'])

const maxSlots = 6
const bannedNames = ref(new Set(props.initialBans))
const searchQuery = ref('')
const rarityFilter = ref('')

// Ban 槽位费用梯度
const banCosts = [0, 100, 300, 600, 1000, 1500]

// 计算属性
const bannedList = computed(() => Array.from(bannedNames.value))

const totalCost = computed(() => {
  let cost = 0
  for (let i = 0; i < bannedList.value.length && i < maxSlots; i++) {
    cost += banCosts[i]
  }
  return cost
})

const slots = computed(() => {
  return Array.from({ length: maxSlots }, (_, i) => ({
    index: i,
    banned: i < bannedList.value.length,
    pokemonName: bannedList.value[i] || '',
    cost: banCosts[i],
    affordable: props.playerPoints >= banCosts[i]
  }))
})

const filteredPokemon = computed(() => {
  let list = props.pokemonList
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    list = list.filter(p =>
      (p.name || '').toLowerCase().includes(query) ||
      (p.name_en || '').toLowerCase().includes(query)
    )
  }
  // 按 ID 排序
  return list.sort((a, b) => (a.id || 0) - (b.id || 0))
})

const canConfirm = computed(() => {
  return bannedList.value.length > 0 && totalCost.value <= props.playerPoints
})

// 方法
function getSpriteUrl(pokemon) {
  return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.id}.png`
}

function isBanned(pokemon) {
  return bannedNames.value.has(pokemon.name_en || pokemon.name)
}

function getSlotClass(slot) {
  if (slot.banned) return 'border-rose-400 bg-rose-50'
  if (slot.index < bannedList.value.length) return 'border-slate-300 bg-white'
  return 'border-dashed border-slate-300 bg-slate-50 hover:border-slate-400'
}

function toggleBan(pokemon) {
  const name = pokemon.name_en || pokemon.name
  if (bannedNames.value.has(name)) {
    bannedNames.value.delete(name)
  } else {
    if (bannedList.value.length >= maxSlots) return
    bannedNames.value.add(name)
  }
  // 触发响应式更新
  bannedNames.value = new Set(bannedNames.value)
}

function handleSlotClick(slot) {
  if (slot.banned) {
    // 点击已 ban 的槽位，取消 ban
    const name = slot.pokemonName
    bannedNames.value.delete(name)
    bannedNames.value = new Set(bannedNames.value)
  }
}

function clearAllBans() {
  bannedNames.value = new Set()
}

function confirmBans() {
  emit('confirm', {
    bannedPokemon: bannedList.value,
    cost: totalCost.value
  })
}

// 监听 show 变化，重置状态
watch(() => props.show, (newShow) => {
  if (newShow) {
    bannedNames.value = new Set(props.initialBans)
  }
})
</script>
