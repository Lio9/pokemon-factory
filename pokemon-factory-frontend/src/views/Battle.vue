<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-4">对战工厂</h1>
    <div class="grid grid-cols-2 gap-4">
      <div>
        <BattleArena :summary="summary" />
      </div>
      <div>
        <div class="mb-4">
          <label class="block">用户名</label>
          <input v-model="username" class="border p-2 w-full" placeholder="输入用户名或使用 guest" />
        </div>
        <button @click="start" class="bg-blue-500 text-white px-4 py-2 rounded">开始匹配 (演示)</button>
        <button @click="startAsync" class="bg-green-500 text-white px-4 py-2 rounded ml-2">异步匹配</button>
        <pre class="mt-4 bg-gray-100 p-2 rounded">{{ resultText }}</pre>

        <ExchangeModal v-if="showExchange" :opponentTeam="opponentTeam" :replacedIndex="replacedIndex" @close="showExchange=false" @confirm="onConfirmExchange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BattleArena from '../components/BattleArena.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import api from '../services/api'

const username = ref('guest')
const resultText = ref('')
const summary = ref(null)
const opponentTeam = ref([])

const showExchange = ref(false)
const replacedIndex = ref(0)
let currentBattleId = null

async function start() {
  resultText.value = '匹配中...'
  try {
    const res = await api.battle.start({ username: username.value })
    resultText.value = '匹配完成'
    summary.value = res.summary || res
    opponentTeam.value = JSON.parse(res.opponentTeamJson || '[]')
    currentBattleId = res.battleId
    // if player won, open exchange modal
    if ((res.summary || {}).winner === 'player') {
      replacedIndex.value = 0
      showExchange.value = true
    }
  } catch (e) {
    resultText.value = '请求失败: ' + e.message
  }
}

async function startAsync(){
  resultText.value = '提交异步对战...'
  try {
    const res = await api.battle.startAsync({ username: username.value })
    currentBattleId = res.battleId
    resultText.value = '异步对战已提交. battleId=' + currentBattleId + '，开始轮询结果'
    // poll for status
    let attempts = 0
    const maxAttempts = 60
    const intervalMs = 2000
    const poll = setInterval(async () => {
      attempts++
      try {
        const status = await api.battle.status(currentBattleId)
        if (status && status.battle) {
          // finished when ended_at or summary exists
          if (status.battle.ended_at || status.battle.summary_json) {
            clearInterval(poll)
            resultText.value = '对战完成'
            const summaryJson = status.battle.summary_json || null
            if (summaryJson) {
              try { summary.value = typeof summaryJson === 'string' ? JSON.parse(summaryJson) : summaryJson } catch(e){ summary.value = summaryJson }
            }
            // load opponent team if provided
            if (status.battle.opponent_team_json) {
              try { opponentTeam.value = JSON.parse(status.battle.opponent_team_json) } catch(e){ opponentTeam.value = [] }
            }
            return
          }
        }
      } catch (e) {
        // ignore transient errors
      }
      if (attempts >= maxAttempts) {
        clearInterval(poll)
        resultText.value = '轮询超时，请稍后手动查询 status.'
      }
    }, intervalMs)
  } catch (e) {
    resultText.value = '提交失败: ' + e.message
  }
}

async function onConfirmExchange(pickedIdx){
  // send exchange request
  const picked = opponentTeam.value[pickedIdx]
  const newPokemonJson = JSON.stringify(picked)
  const res = await api.battle.exchange({ battleId: currentBattleId, replacedIndex: replacedIndex.value, newPokemonJson })
  showExchange.value = false
  resultText.value = '已交换: ' + JSON.stringify(res)
}
</script>

<style scoped>
</style>
