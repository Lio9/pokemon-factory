/**
 * 对战轮询管理模块
 *
 * 封装对战状态轮询的起停逻辑，支持错误边界和指数退避。
 *
 * @module composables/battle/useBattlePolling
 */

import { ref, onBeforeUnmount } from 'vue'

const MAX_CONSECUTIVE_FAILURES = 5
const BASE_INTERVAL_MS = 2000
const MAX_BACKOFF_MS = 16000

/**
 * 创建轮询管理器
 * @param {Function} onPoll - 每次轮询触发的回调
 * @param {number} intervalMs - 基础轮询间隔（默认 2000ms）
 * @returns {Object} { pollingActive, startPolling, stopPolling }
 */
export function useBattlePolling(onPoll, intervalMs = BASE_INTERVAL_MS) {
  const pollingActive = ref(false)
  let pollTimer = null
  let consecutiveFailures = 0

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    pollingActive.value = false
    consecutiveFailures = 0
  }

  function startPolling() {
    stopPolling()
    pollingActive.value = true
    scheduleNext(intervalMs)
  }

  function scheduleNext(delay) {
    if (!pollingActive.value) return
    pollTimer = setTimeout(async () => {
      if (!pollingActive.value) return
      try {
        await onPoll()
        consecutiveFailures = 0
        // 成功后恢复基础间隔
        scheduleNext(intervalMs)
      } catch (e) {
        consecutiveFailures++
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
          // 连续失败过多，停止轮询
          stopPolling()
          return
        }
        // 指数退避：失败后逐步增大间隔
        const backoff = Math.min(intervalMs * Math.pow(2, consecutiveFailures), MAX_BACKOFF_MS)
        scheduleNext(backoff)
      }
    }, delay)
  }

  // 组件卸载时自动清理
  onBeforeUnmount(stopPolling)

  return {
    pollingActive,
    startPolling,
    stopPolling
  }
}
