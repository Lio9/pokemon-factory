<template>
  <div class="debug-panel">
    <div class="debug-header">
      <span class="debug-title">🐛 {{ t('调试面板', 'Debug Panel') }}</span>
      <button class="debug-close" @click="$emit('close')">✕</button>
    </div>

    <!-- 性能统计 -->
    <div class="debug-section">
      <div class="section-label">📊 {{ t('性能', 'Performance') }}</div>
      <div class="stat-grid">
        <div class="stat-item">
          <span class="stat-label">FPS</span>
          <span class="stat-value" :class="fpsClass">{{ fps }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">{{ t('阶段', 'Phase') }}</span>
          <span class="stat-value">{{ phase || 'idle' }}</span>
        </div>
        <div v-for="(value, key) in filteredStats" :key="key" class="stat-item">
          <span class="stat-label">{{ key }}</span>
          <span class="stat-value">{{ formatStatValue(value) }}</span>
        </div>
      </div>
    </div>

    <!-- 日志 -->
    <div class="debug-section">
      <div class="section-label">
        📝 {{ t('日志', 'Logs') }}
        <span class="log-count">({{ logs.length }})</span>
        <button class="clear-btn" @click="clearLogs">{{ t('清空', 'Clear') }}</button>
      </div>
      <div class="log-filters">
        <button
          v-for="level in logLevels"
          :key="level"
          class="filter-btn"
          :class="{ 'filter-active': activeFilter === level }"
          @click="toggleFilter(level)"
        >
          {{ level }}
        </button>
      </div>
      <div class="log-list" ref="logListRef">
        <div
          v-for="(log, index) in filteredLogs"
          :key="index"
          class="log-entry"
          :class="'log-' + log.level"
        >
          <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          <span class="log-level">{{ log.level.toUpperCase() }}</span>
          <span class="log-category">[{{ log.category }}]</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * DebugPanel.vue - 调试面板组件
 *
 * 显示 FPS、战斗阶段、统计信息和日志。
 * 用于开发和测试阶段监控 3D 场景状态。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

interface LogEntry {
  timestamp: Date | number
  level: string
  category: string
  message: string
  data?: any
}

const props = defineProps<{
  fps: number
  phase: string
  logs: LogEntry[]
  stats: Record<string, any>
}>()

defineEmits<{
  close: []
}>()

// ===== 日志过滤 =====
const logLevels = ['all', 'info', 'warn', 'error', 'debug']
const activeFilter = ref('all')
const logListRef = ref<HTMLElement | null>(null)

function toggleFilter(level: string) {
  activeFilter.value = activeFilter.value === level ? 'all' : level
}

const filteredLogs = computed(() => {
  if (activeFilter.value === 'all') return props.logs
  return props.logs.filter(l => l.level === activeFilter.value)
})

// ===== 统计过滤（排除内部键） =====
const filteredStats = computed(() => {
  const result: Record<string, any> = {}
  for (const [key, value] of Object.entries(props.stats)) {
    if (key !== 'fps' && key !== 'phase') {
      result[key] = value
    }
  }
  return result
})

// ===== FPS 样式 =====
const fpsClass = computed(() => {
  if (props.fps >= 55) return 'fps-good'
  if (props.fps >= 30) return 'fps-ok'
  return 'fps-bad'
})

// ===== 辅助函数 =====
function formatTime(timestamp: Date | number): string {
  const date = timestamp instanceof Date ? timestamp : new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function formatStatValue(value: any): string {
  if (typeof value === 'number') return value.toFixed(1)
  return String(value)
}

function clearLogs() {
  // 通过父组件处理
}

// ===== 自动滚动到底部 =====
watch(() => props.logs.length, () => {
  nextTick(() => {
    if (logListRef.value) {
      logListRef.value.scrollTop = logListRef.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.debug-panel {
  position: absolute;
  top: 56px;
  right: 8px;
  width: 380px;
  max-height: calc(100vh - 120px);
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  z-index: 100;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.debug-title {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.debug-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.debug-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.debug-section {
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.section-label {
  font-size: 12px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.log-count {
  font-weight: normal;
  color: rgba(255, 255, 255, 0.4);
}

.clear-btn {
  margin-left: auto;
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  cursor: pointer;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.stat-value {
  font-size: 12px;
  font-weight: bold;
  color: #fff;
}

.fps-good { color: #4ade80; }
.fps-ok { color: #fbbf24; }
.fps-bad { color: #ef4444; }

.log-filters {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.filter-btn {
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.filter-active {
  background: rgba(59, 130, 246, 0.3) !important;
  border-color: #3b82f6 !important;
  color: #fff !important;
}

.log-list {
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
}

.log-entry {
  padding: 3px 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.log-time {
  color: rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.log-level {
  font-weight: bold;
  flex-shrink: 0;
  min-width: 40px;
}

.log-info .log-level { color: #60a5fa; }
.log-warn .log-level { color: #fbbf24; }
.log-error .log-level { color: #ef4444; }
.log-debug .log-level { color: #a78bfa; }

.log-category {
  color: rgba(255, 255, 255, 0.4);
  flex-shrink: 0;
}

.log-message {
  color: rgba(255, 255, 255, 0.8);
  word-break: break-all;
}
</style>
