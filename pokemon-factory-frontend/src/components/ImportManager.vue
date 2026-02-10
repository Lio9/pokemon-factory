<template>
  <div class="import-manager">
    <div class="container mx-auto px-4 py-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-8">数据导入管理</h1>

      <!-- 导入控制区 -->
      <div class="bg-white rounded-xl shadow-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">导入控制</h2>
        <div class="flex flex-wrap gap-4">
          <button
            @click="startImport"
            :disabled="currentTask && currentTask.status === 'running'"
            class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            <span v-if="currentTask && currentTask.status === 'running'">导入中...</span>
            <span v-else>开始全量导入</span>
          </button>

          <button
            @click="clearAllData"
            class="px-6 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            清空所有数据
          </button>

          <button
            @click="refreshStatus"
            class="px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
          >
            刷新状态
          </button>
        </div>
      </div>

      <!-- 当前任务状态 -->
      <div v-if="currentTask" class="bg-white rounded-xl shadow-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4">当前任务</h2>
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
            ></div>
          </div>
          <div class="flex justify-between items-center">
            <span class="text-gray-600">消息:</span>
            <span class="text-sm text-gray-800">{{ currentTask.message }}</span>
          </div>

          <!-- 导入结果统计 -->
          <div v-if="currentTask.data && Object.keys(currentTask.data).length > 0" class="mt-4 p-4 bg-gray-50 rounded-lg">
            <h3 class="font-semibold mb-2">导入结果</h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div v-if="currentTask.data.pokemonCount !== undefined">
                <div class="text-gray-600">宝可梦</div>
                <div class="font-bold text-blue-600">{{ currentTask.data.pokemonCount }}</div>
              </div>
              <div v-if="currentTask.data.moveCount !== undefined">
                <div class="text-gray-600">技能</div>
                <div class="font-bold text-green-600">{{ currentTask.data.moveCount }}</div>
              </div>
              <div v-if="currentTask.data.itemCount !== undefined">
                <div class="text-gray-600">物品</div>
                <div class="font-bold text-purple-600">{{ currentTask.data.itemCount }}</div>
              </div>
              <div v-if="currentTask.data.abilityCount !== undefined">
                <div class="text-gray-600">特性</div>
                <div class="font-bold text-orange-600">{{ currentTask.data.abilityCount }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 任务列表 -->
      <div class="bg-white rounded-xl shadow-lg p-6">
        <h2 class="text-xl font-semibold mb-4">任务历史</h2>
        <div v-if="taskList.length === 0" class="text-gray-500 text-center py-8">
          暂无任务记录
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="task in taskList"
            :key="task.taskId"
            class="p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="font-mono text-sm text-gray-600">{{ task.taskId }}</span>
                  <span :class="getStatusClass(task.status)" class="px-2 py-1 rounded text-xs">
                    {{ getStatusText(task.status) }}
                  </span>
                </div>
                <div class="text-sm text-gray-600">{{ task.message }}</div>
                <div class="text-xs text-gray-500 mt-1">
                  开始时间: {{ formatTime(task.startTime) }}
                  <span v-if="task.endTime">
                    | 结束时间: {{ formatTime(task.endTime) }}
                  </span>
                </div>
              </div>
              <div class="text-right">
                <div class="text-2xl font-bold">{{ task.progress }}%</div>
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

export default {
  name: 'ImportManager',
  setup() {
    const currentTask = ref(null)
    const taskList = ref([])
    let refreshTimer = null

    // API基础URL
    const API_BASE = 'http://localhost:8080/api'

    // 开始导入
    const startImport = async () => {
      try {
        const response = await fetch(`${API_BASE}/import/all`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        const result = await response.json()

        if (result.code === 200) {
          const taskId = result.data?.taskId
          if (taskId) {
            ElMessage.success(`导入任务已启动，任务ID: ${taskId}`)
            // 延迟刷新，确保任务状态已更新
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
          '确定要清空所有数据吗？此操作不可恢复！',
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await fetch(`${API_BASE}/import/all`, {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        const result = await response.json()

        if (result.code === 200) {
          ElMessage.success('数据已清空')
          refreshStatus()
        } else {
          ElMessage.error(result.message || '清空数据失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清空数据失败:', error)
          ElMessage.error('网络错误，请稍后重试')
        }
      }
    }

    // 刷新状态
    const refreshStatus = async () => {
      try {
        // 获取任务列表
        const tasksResponse = await fetch(`${API_BASE}/import/tasks`)
        const tasksResult = await tasksResponse.json()

        console.log('任务列表响应:', tasksResult)

        if (tasksResult.code === 200) {
          const tasks = tasksResult.data || []
          taskList.value = tasks

          // 找到当前正在运行的任务
          const runningTask = tasks.find(t => t.status === 'running')
          if (runningTask) {
            console.log('找到运行中的任务:', runningTask)
            // 获取当前任务的详细状态
            const statusResponse = await fetch(`${API_BASE}/import/status/${runningTask.taskId}`)
            const statusResult = await statusResponse.json()

            console.log('任务状态响应:', statusResult)

            if (statusResult.code === 200) {
              currentTask.value = statusResult.data
            }
          } else if (tasks.length > 0) {
            // 如果没有运行中的任务，显示最近的一个
            currentTask.value = tasks[0]
            console.log('显示最新任务:', currentTask.value)
          } else {
            currentTask.value = null
          }
        }
      } catch (error) {
        console.error('刷新状态失败:', error)
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
      refreshStatus()
      // 每3秒自动刷新一次状态
      refreshTimer = setInterval(refreshStatus, 3000)
    })

    // 组件卸载时清除定时器
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