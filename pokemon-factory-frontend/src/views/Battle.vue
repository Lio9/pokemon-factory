<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-4">对战工厂</h1>
    <div class="grid grid-cols-2 gap-4">
      <div>
        <BattleArena />
      </div>
      <div>
        <div class="mb-4">
          <label class="block">用户名</label>
          <input v-model="username" class="border p-2 w-full" placeholder="输入用户名或使用 guest" />
        </div>
        <button @click="start" class="bg-blue-500 text-white px-4 py-2 rounded">开始匹配 (演示)</button>
        <pre class="mt-4 bg-gray-100 p-2 rounded">{{ result }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BattleArena from '../components/BattleArena.vue'
import api from '../services/api'

const username = ref('guest')
const result = ref('')

async function start() {
  const res = await api.battle.start({ username: username.value })
  result.value = JSON.stringify(res, null, 2)
}
</script>

<style scoped>
</style>
