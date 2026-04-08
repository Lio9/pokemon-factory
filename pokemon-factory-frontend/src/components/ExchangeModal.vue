<template>
  <div class="fixed inset-0 flex items-center justify-center bg-black bg-opacity-40">
    <div class="w-11/12 max-w-4xl rounded-2xl bg-white p-6 shadow-xl">
      <h3 class="mb-2 font-bold">
        交换宝可梦
      </h3>
      <p class="text-sm text-gray-500">
        从对手队伍中选择一只替换你队伍中的第
        <input
          v-model.number="localReplaced"
          type="number"
          min="1"
          :max="maxSlot"
          class="mx-1 inline-block w-16 rounded border p-1"
        >
        只宝可梦（输入1-{{ maxSlot }}）
      </p>
      <div class="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="(p, idx) in opponentTeam"
          :key="idx"
          :class="['cursor-pointer rounded-xl border p-4 transition', sel === idx ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:border-slate-300']"
          @click="select(idx)"
        >
          <div class="font-semibold">
            {{ p.name || p.id }}
          </div>
          <div class="mt-1 text-xs text-gray-500">
            {{ formatTypes(p.types) }}
          </div>
          <div class="mt-2 text-xs text-gray-500">
            持有物：{{ p.heldItem || '无' }}
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="move in p.moves || []"
              :key="move.name_en || move.name"
              class="rounded-full bg-white px-2 py-1 text-xs text-slate-600"
            >
              {{ move.name || move.name_en }}
            </span>
          </div>
          <div class="mt-3 text-xs text-gray-500">
            HP {{ p?.stats?.hp || '-' }} / 速度 {{ p?.stats?.speed || '-' }}
          </div>
        </div>
      </div>
      <div class="mt-6 text-right">
        <button
          class="mr-2 rounded-lg px-3 py-1"
          @click="$emit('close')"
        >
          取消
        </button>
        <button
          class="rounded-lg bg-blue-500 px-3 py-1 text-white"
          @click="confirm"
        >
          确认交换
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  opponentTeam: {
    type: Array,
    default: () => []
  },
  replacedIndex: Number,
  maxSlot: {
    type: Number,
    default: 6
  }
})

const emits = defineEmits(['close', 'confirm', 'update:replacedIndex'])

let sel = ref(null)
const localReplaced = ref((props.replacedIndex || 0) + 1)
const maxSlot = props.maxSlot

watch(() => props.replacedIndex, (value) => {
  localReplaced.value = (value || 0) + 1
})

function select(index) {
  sel.value = index
}

function confirm() {
  if (sel.value === null) return
  const newIndex = Math.max(0, Math.min(maxSlot - 1, (localReplaced.value || 1) - 1))
  emits('update:replacedIndex', newIndex)
  emits('confirm', sel.value)
}

function formatTypes(types = []) {
  return (types || []).map((type) => type.name || type.name_zh || `属性${type.type_id}`).join(' / ') || '未知属性'
}
</script>
