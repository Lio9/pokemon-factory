<template>
  <div class="fixed inset-0 flex items-center justify-center bg-black bg-opacity-40">
    <div class="bg-white p-4 rounded w-2/3">
      <h3 class="font-bold mb-2">交换宝可梦</h3>
      <p class="text-sm text-gray-500">从对手队伍中选择一只替换你的第
        <input type="number" v-model.number="localReplaced" min="1" :max="maxSlot" class="w-16 border p-1 inline-block mx-1" />
        只宝可梦（输入1-{{ maxSlot }}）
      </p>
      <div class="grid grid-cols-3 gap-2 mt-2">
        <div v-for="(p, idx) in opponentTeam" :key="idx" :class="['p-2 border rounded cursor-pointer', sel===idx? 'border-blue-500':'']" @click="select(idx)">
          <div class="font-semibold">{{ p.name || p.id }}</div>
          <div class="text-xs text-gray-500">EXP: {{ p.base_experience }}</div>
        </div>
      </div>
      <div class="mt-4 text-right">
        <button class="px-3 py-1 mr-2" @click="$emit('close')">取消</button>
        <button class="px-3 py-1 bg-blue-500 text-white" @click="confirm">确认交换</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ opponentTeam: Array, replacedIndex: Number, maxSlot: { type: Number, default: 6 } })
const emits = defineEmits(['close','confirm','update:replacedIndex'])
let sel = ref(null)
const localReplaced = ref((props.replacedIndex || 0) + 1)
const maxSlot = props.maxSlot
watch(() => props.replacedIndex, (v) => { localReplaced.value = (v||0) + 1 })
function select(i){ sel.value = i }
function confirm(){ if(sel.value===null) return; const newIndex = Math.max(0, Math.min(maxSlot-1, (localReplaced.value||1)-1)); emits('update:replacedIndex', newIndex); emits('confirm', sel.value) }
</script>

<style scoped>
</style>
