<template>
  <div class="import-manager space-y-6">
    <!-- 页面头部 -->
    <section class="glass-card p-5 sm:p-7">
      <div class="inline-flex rounded-full border border-white/70 bg-white/75 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-sky-700 shadow-sm">
        {{ tr('数据管理', 'Data Management') }}
      </div>
      <h1 class="mt-4 text-[clamp(1.5rem,4vw,2rem)] font-black tracking-tight text-slate-950">
        {{ tr('数据导入管理', 'Import Manager') }}
      </h1>
      <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
        {{ tr('这里用于启动后端已有的全量导入任务并查看任务状态；当前系统没有提供"清空数据库"的管理接口，因此页面只支持清空本地任务历史记录。', 'Use this page to start the existing backend full import task and inspect its status. The system does not expose a database wipe endpoint, so this page only clears local task history.') }}
      </p>
    </section>

    <!-- 导入控制区 -->
    <section class="glass-card p-5 sm:p-6">
      <h2 class="text-lg font-bold text-slate-900 mb-4">
        {{ tr('导入控制', 'Import Controls') }}
      </h2>
      <div class="flex flex-wrap gap-3">
        <button
          :disabled="currentTask && currentTask.status === 'running'"
          class="btn-primary"
          @click="startImport"
        >
          <svg v-if="currentTask && currentTask.status === 'running'" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" /><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
          <span>{{ currentTask && currentTask.status === 'running' ? tr('导入中...', 'Importing...') : tr('开始全量导入', 'Start Full Import') }}</span>
        </button>

        <button
          class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-rose-300 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
          @click="clearAllData"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
          {{ tr('清空本地任务历史', 'Clear Local History') }}
        </button>

        <button
          class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-300 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          @click="refreshStatus"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
          {{ tr('刷新状态', 'Refresh Status') }}
        </button>
      </div>
    </section>

    <!-- 当前任务 -->
    <section v-if="currentTask" class="glass-card p-5 sm:p-6">
      <h2 class="text-lg font-bold text-slate-900 mb-4">
        {{ tr('当前任务', 'Current Task') }}
      </h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="space-y-3">
          <div class="flex justify-between items-center py-2 border-b border-slate-100">
            <span class="text-sm text-slate-500">{{ tr('任务ID', 'Task ID') }}</span>
            <span class="text-sm font-mono font-semibold text-slate-800">{{ currentTask.taskId }}</span>
          </div>
          <div class="flex justify-between items-center py-2 border-b border-slate-100">
            <span class="text-sm text-slate-500">{{ tr('任务类型', 'Type') }}</span>
            <span class="text-sm font-semibold text-slate-800">{{ getTaskTypeText(currentTask.taskType) }}</span>
          </div>
          <div class="flex justify-between items-center py-2 border-b border-slate-100">
            <span class="text-sm text-slate-500">{{ tr('状态', 'Status') }}</span>
            <span class="text-sm font-semibold" :class="getStatusClass(currentTask.status)">{{ getStatusText(currentTask.status) }}</span>
          </div>
        </div>
        <div class="space-y-3">
          <div class="flex justify-between items-center py-2 border-b border-slate-100">
            <span class="text-sm text-slate-500">{{ tr('进度', 'Progress') }}</span>
            <span class="text-sm font-semibold text-slate-800">{{ currentTask.progress }}%</span>
          </div>
          <div class="py-2">
            <div class="hp-bar">
              <div
                class="hp-bar-fill"
                :class="currentTask.progress < 40 ? 'low' : currentTask.progress < 80 ? 'mid' : 'high'"
                :style="{ width: currentTask.progress + '%' }"
              />
            </div>
          </div>
          <div class="flex justify-between items-center py-2 border-b border-slate-100">
            <span class="text-sm text-slate-500">{{ tr('消息', 'Message') }}</span>
            <span class="text-sm text-slate-700 truncate max-w-[200px]" :title="currentTask.message">{{ currentTask.message }}</span>
          </div>
        </div>
      </div>

      <!-- 导入结果 -->
      <div v-if="currentTask.data && Object.keys(currentTask.data).length > 0" class="mt-4 rounded-xl bg-slate-50 p-4">
        <h3 class="text-sm font-semibold text-slate-700 mb-3">{{ tr('导入结果', 'Import Result') }}</h3>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div v-for="(value, key) in currentTask.data" :key="key" class="glass-card p-3 text-center">
            <div class="text-lg font-bold text-slate-800">{{ value }}</div>
            <div class="text-xs text-slate-500 mt-0.5">{{ key }}</div>
          </div>
        </div>
      </div>

      <!-- 时间信息 -->
      <div class="mt-4 flex flex-wrap gap-4 text-xs text-slate-400">
        <span>{{ tr('开始时间', 'Start') }}: {{ formatTime(currentTask.startTime) }}</span>
        <span v-if="currentTask.endTime">{{ tr('结束时间', 'End') }}: {{ formatTime(currentTask.endTime) }}</span>
      </div>
    </section>

    <!-- 任务历史 -->
    <section v-if="taskList.length > 1" class="glass-card p-5 sm:p-6">
      <h2 class="text-lg font-bold text-slate-900 mb-4">
        {{ tr('任务历史', 'Task History') }}
      </h2>
      <div class="space-y-2">
        <div v-for="(task, index) in taskList.slice(0, 10)" :key="index" class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3">
          <div class="flex items-center gap-3">
            <div class="w-2 h-2 rounded-full" :class="task.status === 'completed' ? 'bg-emerald-500' : task.status === 'running' ? 'bg-blue-500 animate-pulse' : task.status === 'failed' ? 'bg-rose-500' : 'bg-slate-300'" />
            <span class="text-sm text-slate-600">{{ getTaskTypeText(task.taskType) }}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs text-slate-400">{{ formatTime(task.startTime) }}</span>
            <span class="text-xs font-semibold" :class="getStatusClass(task.status)">{{ getStatusText(task.status) }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { importApi } from '../services/api.js'
import { useLocale } from '../composables/useLocale'
import { ElMessage } from 'element-plus'

const { translate: tr } = useLocale()

const STORAGE_KEY = 'pokemon-factory-import-tasks'
const IMPORT_SUCCESS_CODES = new Set([200, 201, 10000])

const currentTask = ref(null)
const taskList = ref([])
let refreshTimer = null

function loadStoredTasks() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

function persistTasks(tasks) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks))
}

function normalizeTask(task) {
  return {
    ...task,
    progress: task.progress ?? 0,
    message: task.message ?? '',
    status: task.status ?? 'pending',
    data: task.data ?? {}
  }
}

function formatTime(timestamp) {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString(locale.value === 'zh-CN' ? 'zh-CN' : 'en-US')
}

function getStatusText(status) {
  const map = {
    pending: tr('等待中', 'Pending'),
    running: tr('运行中', 'Running'),
    completed: tr('已完成', 'Completed'),
    failed: tr('失败', 'Failed')
  }
  return map[status] || status
}

function getStatusClass(status) {
  const map = {
    pending: 'text-slate-500',
    running: 'text-blue-600',
    completed: 'text-emerald-600',
    failed: 'text-rose-600'
  }
  return map[status] || 'text-slate-500'
}

function getTaskTypeText(taskType) {
  const map = {
    IMPORT_ALL: tr('全量导入', 'Full Import'),
    IMPORT_POKEMON_RANGE: tr('范围导入', 'Range Import')
  }
  return map[taskType] || taskType
}

async function startImport() {
  try {
    const result = await importApi.start()
    if (IMPORT_SUCCESS_CODES.has(result.code)) {
      ElMessage.success(tr('导入任务已启动', 'Import task started'))
      const task = normalizeTask({
        ...result.data,
        taskId: result.data.taskId || result.data.id || Date.now().toString(),
        startTime: Date.now(),
        status: 'running'
      })
      taskList.value.unshift(task)
      currentTask.value = task
      persistTasks(taskList.value)
    } else {
      ElMessage.error(result.message || tr('启动导入失败', 'Failed to start import'))
    }
  } catch (error) {
    ElMessage.error(error.message || tr('启动导入失败', 'Failed to start import'))
  }
}

async function clearAllData() {
  taskList.value = []
  currentTask.value = null
  localStorage.removeItem(STORAGE_KEY)
  ElMessage.success(tr('已清空本地任务历史', 'Local task history cleared'))
}

async function refreshStatus() {
  if (!taskList.value.length) {
    currentTask.value = null
    return
  }

  try {
    const tasks = await Promise.all(
      taskList.value.map(async (task) => {
        if (task.status !== 'running' && task.status !== 'pending') {
          return task
        }

        const statusResult = await importApi.getStatus(task.taskId)
        if (!IMPORT_SUCCESS_CODES.has(statusResult.code)) {
          return {
            ...task,
            status: 'failed',
            endTime: Date.now(),
            message: statusResult.message || tr('获取导入状态失败', 'Failed to fetch import status')
          }
        }

        const statusData = normalizeTask({
          ...task,
          ...statusResult.data,
          taskId: task.taskId,
          startTime: task.startTime
        })

        if (statusData.status === 'completed' || statusData.status === 'failed') {
          statusData.endTime = Date.now()
          if (statusData.progress < 100 && statusData.status === 'completed') {
            statusData.progress = 100
          }
        }

        return statusData
      })
    )

    taskList.value = tasks
    persistTasks(tasks)

    const runningTask = tasks.find(task => task.status === 'running' || task.status === 'pending')
    currentTask.value = runningTask || tasks[0] || null
  } catch (error) {
    console.error('刷新状态失败:', error)
    const latestTask = taskList.value[0]
    if (latestTask) {
      currentTask.value = {
        ...latestTask,
        message: tr('状态刷新失败，请稍后重试', 'Status refresh failed, please try again later')
      }
    }
  }
}

onMounted(() => {
  taskList.value = loadStoredTasks()
  currentTask.value = taskList.value[0] || null
  refreshStatus()
  refreshTimer = setInterval(refreshStatus, 3000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.import-manager {
  padding-bottom: 1rem;
}
</style>
