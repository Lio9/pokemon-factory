<template>
  <div class="import-manager">
    <div class="container mx-auto px-4 py-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-8">
        数据导入管理
      </h1>

      <!-- 导入控制区 -->
      <div class="bg-white rounded-xl shadow-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">
          导入控制
        </h2>
        <div class="flex flex-wrap gap-4">
          <button
            :disabled="currentTask && currentTask.status === 'running'"
            class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
            @click="startImport"
          >
            <span v-if="currentTask && currentTask.status === 'running'">导入中...</span>
            <span v-else>开始全量导入</span>
          </button>

          <button
            class="px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            @click="clearAllData"
          >
            清空所有数据
          </button>

          <button
            class="px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
            @click="refreshStatus"
          >
            刷新状态
          </button>
        </div>
      </div>

      <!-- 当前任务状态 -->
      <div
        v-if="currentTask"
        class="bg-white rounded-xl shadow-lg p-6 mb-6"
      >
        <h2 class="text-xl font-semibold mb-4">
          当前任务
        </h2>
        <div class="space-y-4">
          <div class="flex justify-between items-center">
            <span class="text-gray-600">任务ID:</span>
            <span class="font-mono text-sm">{{ currentTask.taskId }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">任务类型:</span>
            <span>{{ getTaskTypeText(currentTask.taskType) }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">状态:</span>
            <span :class="getStatusClass(currentTask.status)">
              {{ getStatusText(currentTask.status) }}
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">进度:</span>
            <span>{{ currentTask.progress }}%</span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-4">
            <div
              class="bg-blue-600 h-4 rounded-full transition-all duration-300"
              :style="{ width: currentTask.progress + '%' }"
            />
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">消息:</span>
            <span class="text-sm text-gray-800">{{ currentTask.message }}</span>
          </div>

          <!-- 导入结果统计 -->
          <div
            v-if="currentTask.data && Object.keys(currentTask.data).length > 0"
            class="mt-4 p-4 bg-gray-50 rounded-lg"
          >
            <h3 class="font-semibold mb-2">
              导入结果
            </h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div v-if="currentTask.data.pokemonCount !== undefined">
                <div class="text-gray-600">
                  宝可梦
                </div>
                <div class="font-bold text-blue-600">
                  {{ currentTask.data.pokemonCount }}
                </div>
              </div>
              <div v-if="currentTask.data.moveCount !== undefined">
                <div class="text-gray-600">
                  技能
                </div>
                <div class="font-bold text-green-600">
                  {{ currentTask.data.moveCount }}
                </div>
              </div>
              <div v-if="currentTask.data.itemCount !== undefined">
                <div class="text-gray-600">
                  物品
                </div>
                <div class="font-bold text-purple-600">
                  {{ currentTask.data.itemCount }}
                </div>
              </div>
              <div v-if="currentTask.data.abilityCount !== undefined">
                <div class="text-gray-600">
                  特性
                </div>
                <div class="font-bold text-orange-600">
                  {{ currentTask.data.abilityCount }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 任务列表 -->
      <div class="bg-white rounded-xl shadow-lg p-6">
        <h2 class="text-xl font-semibold mb-4">
          任务历史
        </h2>
        <div
          v-if="taskList.length === 0"
          class="text-gray-500 text-center py-8"
        >
          暂无任务记录
        </div>
        <div
          v-else
          class="space-y-3"
        >
          <div
            v-for="task in taskList"
            :key="task.taskId"
            class="p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="font-mono text-sm text-gray-600">{{ task.taskId }}</span>
                  <span
                    :class="getStatusClass(task.status)"
                    class="px-2 py-1 rounded text-xs"
                  >
                    {{ getStatusText(task.status) }}
                  </span>
                </div>
                <div class="text-sm text-gray-600">
                  {{ task.message }}
                </div>
                <div class="text-xs text-gray-500 mt-1">
                  开始时间: {{ formatTime(task.startTime) }}
                  <span v-if="task.endTime">
                    | 结束时间: {{ formatTime(task.endTime) }}
                  </span>
                </div>
              </div>
              <div class="text-right">
                <div class="text-2xl font-bold">
                  {{ task.progress }}%
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { importApi } from '../services/api'

const IMPORT_TASKS_STORAGE_KEY = 'pokemon-factory-import-tasks'
const IMPORT_SUCCESS_CODES = new Set([200, 2002, 2003])

export default {
  name: 'ImportManager',
  setup() {
    const currentTask = ref(null)
    const taskList = ref([])
    let refreshTimer = null

    const loadStoredTasks = () => {
      try {
        const stored = localStorage.getItem(IMPORT_TASKS_STORAGE_KEY)
        return stored ? JSON.parse(stored) : []
      } catch (error) {
        console.error('读取导入任务缓存失败:', error)
        return []
      }
    }

    const persistTasks = (tasks) => {
      localStorage.setItem(IMPORT_TASKS_STORAGE_KEY, JSON.stringify(tasks))
    }

    const mergeTask = (task) => {
      const nextTasks = [
        task,
        ...taskList.value.filter(item => item.taskId !== task.taskId)
      ].slice(0, 20)

      taskList.value = nextTasks
      persistTasks(nextTasks)
    }

    const normalizeProgress = (progress) => {
      if (typeof progress === 'number') {
        return Math.min(100, Math.max(0, progress))
      }

      if (!progress || typeof progress !== 'object') {
        return 0
      }

      const totalData = Number(progress.totalData || 0)
      if (totalData <= 0) {
        return progress.error ? 0 : 5
      }

      const estimatedTarget = 15000
      return Math.min(99, Math.round((totalData / estimatedTarget) * 100))
    }

    const normalizeTask = (task) => {
      const data = task.data && typeof task.data === 'object'
        ? task.data
        : task.progress && typeof task.progress === 'object'
          ? task.progress
          : {}

      return {
        taskId: task.taskId,
        taskType: task.taskType || task.importType || 'IMPORT_ALL',
        status: task.status || 'pending',
        progress: normalizeProgress(task.progress),
        rawProgress: task.progress,
        message: task.message || '等待中',
        data,
        startTime: task.startTime || Date.now(),
        endTime: task.status === 'completed' || task.status === 'failed' ? (task.endTime || Date.now()) : null
      }
    }

    // 开始导入
    const startImport = async () => {
      try {
        const result = await importApi.startAll()

        if (IMPORT_SUCCESS_CODES.has(result.code)) {
          const taskId = result.data?.taskId
          if (taskId) {
            const task = normalizeTask({
              ...result.data,
              taskId,
              taskType: 'IMPORT_ALL',
              startTime: Date.now()
            })

            currentTask.value = task
            mergeTask(task)
            ElMessage.success(`导入任务已启动，任务ID: ${taskId}`)
            setTimeout(() => {
              refreshStatus()
            }, 500)
          } else {
            ElMessage.error('未获取到任务ID')
          }
        } else {
          ElMessage.error(result.message || '启动导入失败')
        }
      } catch (error) {
        console.error('启动导入失败:', error)
        ElMessage.error('网络错误，请稍后重试')
      }
    }

    // 清空数据
    const clearAllData = async () => {
      try {
        await ElMessageBox.confirm(
          '当前后端没有提供清空所有数据的接口，这个按钮先改为清空本地任务历史。是否继续？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        taskList.value = []
        currentTask.value = null
        localStorage.removeItem(IMPORT_TASKS_STORAGE_KEY)
        ElMessage.success('本地任务历史已清空')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清空本地任务历史失败:', error)
          ElMessage.error('清空本地任务历史失败')
        }
      }
    }

    // 刷新状态
    const refreshStatus = async () => {
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
                message: statusResult.message || '获取导入状态失败'
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
        if (runningTask) {
          currentTask.value = runningTask
        } else {
          currentTask.value = tasks[0] || null
        }
      } catch (error) {
        console.error('刷新状态失败:', error)
        const latestTask = taskList.value[0]
        if (latestTask) {
          currentTask.value = {
            ...latestTask,
            message: '状态刷新失败，请稍后重试'
          }
        }
      }
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString('zh-CN')
    }

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'pending': '等待中',
        'running': '运行中',
        'completed': '已完成',
        'failed': '失败'
      }
      return statusMap[status] || status
    }

    // 获取状态样式类
    const getStatusClass = (status) => {
      const classMap = {
        'pending': 'text-gray-600',
        'running': 'text-blue-600',
        'completed': 'text-green-600',
        'failed': 'text-red-600'
      }
      return classMap[status] || 'text-gray-600'
    }

    // 获取任务类型文本
    const getTaskTypeText = (taskType) => {
      const typeMap = {
        'IMPORT_ALL': '全量导入',
        'IMPORT_POKEMON_RANGE': '范围导入'
      }
      return typeMap[taskType] || taskType
    }

    // 组件挂载时开始刷新
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

    return {
      currentTask,
      taskList,
      startImport,
      clearAllData,
      refreshStatus,
      formatTime,
      getStatusText,
      getStatusClass,
      getTaskTypeText
    }
  }
}
</script>

<style scoped>
.import-manager {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1rem;
  margin: 2rem auto;
  max-width: 1200px;
}
</style>
