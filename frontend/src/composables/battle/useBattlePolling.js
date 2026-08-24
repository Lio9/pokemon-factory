/**
 * 对战状态更新管理模块
 *
 * G: 优先使用 WebSocket 实时推送，回退到轮询。
 * WebSocket 连接失败或断开时自动降级为 2 秒轮询。
 *
 * @module composables/battle/useBattlePolling
 */

import { ref, onBeforeUnmount } from 'vue'

const MAX_CONSECUTIVE_FAILURES = 5
const BASE_INTERVAL_MS = 2000
const MAX_BACKOFF_MS = 16000

/**
 * 创建状态更新管理器
 * @param {Function} onUpdate - 收到更新时触发的回调
 * @param {number} intervalMs - 轮询间隔（WebSocket 不可用时使用）
 * @returns {Object} { pollingActive, startPolling, stopPolling }
 */
export function useBattlePolling(onUpdate, intervalMs = BASE_INTERVAL_MS) {
  const pollingActive = ref(false)
  let pollTimer = null
  let consecutiveFailures = 0
  let wsConnection = null

  function stopPolling() {
    if (pollTimer) {
      clearTimeout(pollTimer)
      pollTimer = null
    }
    pollingActive.value = false
    consecutiveFailures = 0
    if (wsConnection) {
      try { wsConnection.close() } catch {}
      wsConnection = null
    }
  }

  function startPolling() {
    stopPolling()
    pollingActive.value = true
    // 尝试 WebSocket，失败则回退到轮询
    tryWebSocket()
  }

  function tryWebSocket() {
    if (!pollingActive.value) return
    try {
      const wsUrl = window.location.protocol === 'https:' ? 'wss:' : 'ws:' + '//' + window.location.host + '/ws-battle'
      const ws = new WebSocket(wsUrl)
      wsConnection = ws

      ws.onopen = () => {
        consecutiveFailures = 0
      }

      ws.onmessage = async (event) => {
        if (!pollingActive.value) return
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'battle_update' && data.summary) {
            await onUpdate(data.summary)
          }
        } catch {}
      }

      ws.onerror = () => {
        ws.close()
      }

      ws.onclose = () => {
        wsConnection = null
        if (pollingActive.value) {
          // WebSocket 失败，回退到轮询
          schedulePoll(intervalMs)
        }
      }
    } catch {
      // WebSocket 不支持，直接轮询
      schedulePoll(intervalMs)
    }
  }

  function schedulePoll(delay) {
    if (!pollingActive.value) return
    pollTimer = setTimeout(async () => {
      if (!pollingActive.value) return
      try {
        await onUpdate()
        consecutiveFailures = 0
        schedulePoll(intervalMs)
      } catch (e) {
        consecutiveFailures++
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
          stopPolling()
          return
        }
        const backoff = Math.min(intervalMs * Math.pow(2, consecutiveFailures), MAX_BACKOFF_MS)
        schedulePoll(backoff)
      }
    }, delay)
  }

  onBeforeUnmount(stopPolling)

  return {
    pollingActive,
    startPolling,
    stopPolling
  }
}
