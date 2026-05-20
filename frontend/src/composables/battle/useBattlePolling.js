/**
 * 对战轮询管理模块
 *
 * 封装对战状态轮询的起停逻辑，与具体的刷新回调解耦。
 *
 * @module composables/battle/useBattlePolling
 */

import { ref, onBeforeUnmount } from 'vue'

/**
 * 创建轮询管理器
 * @param {Function} onPoll - 每次轮询触发的回调
 * @param {number} intervalMs - 轮询间隔（默认 2000ms）
 * @returns {Object} { pollingActive, startPolling, stopPolling }
 */
export function useBattlePolling(onPoll, intervalMs = 2000) {
  const pollingActive = ref(false)
  let pollTimer = null

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    pollingActive.value = false
  }

  function startPolling() {
    stopPolling()
    pollingActive.value = true
    pollTimer = setInterval(async () => {
      await onPoll()
    }, intervalMs)
  }

  // 组件卸载时自动清理
  onBeforeUnmount(stopPolling)

  return {
    pollingActive,
    startPolling,
    stopPolling
  }
}
