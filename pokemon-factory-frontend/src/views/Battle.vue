<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-4">对战工厂</h1>
    <div class="grid grid-cols-2 gap-4">
      <div>
        <BattleArena :summary="summary" />
      </div>
      <div>
              <div class="mb-4">
          <label class="block">当前用户</label>
          <div class="p-2 bg-gray-100 rounded">{{ currentUser || '未登录' }}</div>
        </div>
        <div class="mb-4">
          <label class="block">出招映射 (JSON，例如 {"Pikachu":"Thunderbolt"})</label>
          <textarea v-model="playerMoveText" class="w-full p-2 rounded bg-white" rows="3" placeholder='{"Pikachu":"Thunderbolt"}'></textarea>
        </div>
        <button @click="start" class="bg-blue-500 text-white px-4 py-2 rounded">开始匹配</button>
        <button @click="startAsync" class="bg-green-500 text-white px-4 py-2 rounded ml-2">异步匹配</button>
        <pre class="mt-4 bg-gray-100 p-2 rounded">{{ resultText }}</pre>

        <ExchangeModal v-if="showExchange" :opponentTeam="opponentTeam" v-model:replacedIndex="replacedIndex" @close="showExchange=false" @confirm="onConfirmExchange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BattleArena from '../components/BattleArena.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import api from '../services/api'

const username = ref(localStorage.getItem('username') || 'guest')
const currentUser = localStorage.getItem('username') || null
const resultText = ref('')
const summary = ref(null)
const opponentTeam = ref([])

const showExchange = ref(false)
const replacedIndex = ref(0)
const replacedHighlight = ref(-1)
const playerMoveText = ref('{}')
let currentBattleId = null

async function start() {
  resultText.value = '匹配中...'
  try {
    let pm = null
      try {
        pm = playerMoveText.value ? JSON.parse(playerMoveText.value) : null
      } catch (e) {
        resultText.value = '出招映射 JSON 有误'
        return
      }
      const res = await api.battle.start({ username: username.value, playerMoveMap: pm })
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
    let pm = null
      try {
        pm = playerMoveText.value ? JSON.parse(playerMoveText.value) : null
      } catch (e) {
        resultText.value = '出招映射 JSON 有误'
        return
      }
      const res = await api.battle.startAsync({ username: username.value, playerMoveMap: pm })
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
            // if player won, open exchange modal automatically
            try {
              if (summary.value && summary.value.winner === 'player') {
                replacedIndex.value = 0
                showExchange.value = true
              }
            } catch(e) {}
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
  try {
    const res = await api.battle.exchange({ battleId: currentBattleId, replacedIndex: replacedIndex.value, newPokemonJson })
    showExchange.value = false
    resultText.value = '已交换: ' + JSON.stringify(res)
    // refresh status and update arena
    if (currentBattleId) {
      try {
        const status = await api.battle.status(currentBattleId)
        if (status && status.battle) {
          const summaryJson = status.battle.summary_json || null
          if (summaryJson) {
            try { summary.value = typeof summaryJson === 'string' ? JSON.parse(summaryJson) : summaryJson } catch(e){ summary.value = summaryJson }
          }
          if (status.battle.opponent_team_json) {
            try { opponentTeam.value = JSON.parse(status.battle.opponent_team_json) } catch(e){}
          }
        }
      } catch(e){}
    }
    replacedHighlight.value = replacedIndex.value
    setTimeout(()=> replacedHighlight.value = -1, 4000)
  } catch (e) {
    resultText.value = '交换失败: ' + (e.message || e)
  }
}
</script>

<style scoped>
</style>
