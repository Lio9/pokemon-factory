<template>
  <div class="border p-4 rounded">
    <h2 class="text-lg font-semibold">战场</h2>
    <p class="text-sm text-gray-500">回合日志</p>
    <div v-if="typeof highlightIndex !== 'undefined' && highlightIndex >= 0" class="mb-2 text-sm text-blue-600">已替换槽: {{ highlightIndex + 1 }}（高亮 4 秒）</div>
    <div v-if="summary">
      <div class="mt-2">
        <div>胜者: <strong>{{ summary.winner }}</strong></div>
        <div>玩家强度: {{ summary.playerStrength }}, 对手强度: {{ summary.opponentStrength }}</div>
      </div>
      <ul class="mt-2 space-y-2">
        <li v-for="r in summary.rounds" :key="r.round" class="p-2 bg-white rounded shadow-sm">
          <div class="text-sm">回合 {{ r.round }}: 玩家受伤 {{ r.dmgToPlayer }}, 对手受伤 {{ r.dmgToOpponent }}</div>
          <div class="text-xs text-gray-500">玩家HP: {{ r.playerHP }}, 对手HP: {{ r.opponentHP }}</div>
        </li>
      </ul>
    </div>
    <div v-else class="text-gray-500 mt-2">暂无回合数据</div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'
const props = defineProps({ summary: Object, highlightIndex: { type: Number, default: -1 } })
</script>

<style scoped>
</style>
