<template>
  <div class="fixed inset-0 flex items-center justify-center bg-black bg-opacity-40">
    <div class="bg-white p-4 rounded w-2/3">
      <h3 class="font-bold mb-2">交换宝可梦</h3>
      <p class="text-sm text-gray-500">从对手队伍中选择一只替换你的第 {{ replacedIndex + 1 }} 只宝可梦</p>
      <div class="grid grid-cols-3 gap-2 mt-2">
        <div v-for="(p, idx) in opponentTeam" :key="idx" class="p-2 border rounded cursor-pointer" @click="select(idx)">
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
import { ref } from 'vue'
const props = defineProps({ opponentTeam: Array, replacedIndex: Number })
const emits = defineEmits(['close','confirm'])
let sel = ref(null)
function select(i){ sel.value = i }
function confirm(){ if(sel.value===null) return; emits('confirm', sel.value) }
</script>

<style scoped>
</style>
