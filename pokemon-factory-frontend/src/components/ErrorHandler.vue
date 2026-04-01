<template>
  <div class="error-handler">
    <!-- Toast 通知 -->
    <transition-group name="toast" tag="div" class="toast-container">
      <div
        v-for="error in errors"
        :key="error.id"
        class="toast"
        :class="`toast-${error.type}`"
      >
        <div class="toast-icon">
          <component :is="getIcon(error.type)" class="w-5 h-5" />
        </div>
        <div class="toast-content">
          <div class="toast-title">{{ error.title }}</div>
          <div v-if="error.message" class="toast-message">{{ error.message }}</div>
        </div>
        <button @click="removeError(error.id)" class="toast-close">
          <X class="w-4 h-4" />
        </button>
      </div>
    </transition-group>

    <!-- 全局错误边界 -->
    <transition name="fade">
      <div v-if="globalError" class="error-boundary">
        <div class="error-boundary-content">
          <div class="error-icon">
            <AlertOctagon class="w-16 h-16" />
          </div>
          <h2 class="error-title">发生错误</h2>
          <p class="error-message">{{ globalError.message }}</p>
          <div v-if="globalError.details" class="error-details">
            <details>
              <summary>错误详情</summary>
              <pre>{{ globalError.details }}</pre>
            </details>
          </div>
          <div class="error-actions">
            <button @click="reload" class="btn btn-primary">
              <RefreshCw class="w-4 h-4 mr-2" />
              重新加载
            </button>
            <button @click="goHome" class="btn btn-secondary">
              <Home class="w-4 h-4 mr-2" />
              返回首页
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 网络错误提示 -->
    <transition name="slide-down">
      <div v-if="isOffline" class="offline-banner">
        <WifiOff class="w-5 h-5" />
        <span>网络连接已断开，请检查网络设置</span>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { AlertOctagon, RefreshCw, Home, WifiOff, CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const errors = ref([])
const globalError = ref(null)
const isOffline = ref(false)
let errorIdCounter = 0

export const useErrorHandler = () => {
  const showError = (message, title = '错误', type = 'error') => {
    const id = ++errorIdCounter
    errors.value.push({ id, title, message, type })
    // 自动移除（10秒后）
    setTimeout(() => {
      removeError(id)
    }, 10000)
    return id
  }

  const removeError = (id) => {
    const index = errors.value.findIndex(e => e.id === id)
    if (index > -1) {
      errors.value.splice(index, 1)
    }
  }

  const showSuccess = (message, title = '成功') => {
    return showError(message, title, 'success')
  }

  const showWarning = (message, title = '警告') => {
    return showError(message, title, 'warning')
  }

  const showInfo = (message, title = '提示') => {
    return showError(message, title, 'info')
  }

  const setGlobalError = (error) => {
    globalError.value = error
  }

  const clearGlobalError = () => {
    globalError.value = null
  }

  const setOffline = (offline) => {
    isOffline.value = offline
  }

  return {
    errors: computed(() => errors.value),
    globalError: computed(() => globalError.value),
    isOffline: computed(() => isOffline.value),
    showError,
    showSuccess,
    showWarning,
    showInfo,
    removeError,
    setGlobalError,
    clearGlobalError,
    setOffline
  }
}

export default {
  name: 'ErrorHandler',
  components: { AlertOctagon, RefreshCw, Home, WifiOff, CheckCircle, XCircle, AlertTriangle, Info, X },
  setup() {
    const router = useRouter()

    const getIcon = (type) => {
      const icons = {
        success: CheckCircle,
        error: XCircle,
        warning: AlertTriangle,
        info: Info
      }
      return icons[type] || Info
    }

    const removeError = (id) => {
      const index = errors.value.findIndex(e => e.id === id)
      if (index > -1) {
        errors.value.splice(index, 1)
      }
    }

    const reload = () => {
      window.location.reload()
    }

    const goHome = () => {
      globalError.value = null
      router.push('/')
    }

    // 监听网络状态
    const handleOnline = () => {
      isOffline.value = false
      showSuccess('网络已恢复')
    }

    const handleOffline = () => {
      isOffline.value = true
      showWarning('网络连接已断开')
    }

    onMounted(() => {
      window.addEventListener('online', handleOnline)
      window.addEventListener('offline', handleOffline)
      isOffline.value = !navigator.onLine
    })

    onUnmounted(() => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    })

    return {
      errors,
      globalError,
      isOffline,
      getIcon,
      removeError,
      reload,
      goHome
    }
  }
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
}

.toast {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  min-width: 320px;
  max-width: 480px;
  animation: slideInRight 0.3s ease-out;
}

.toast-success {
  border-left: 4px solid #10b981;
}

.toast-success .toast-icon {
  color: #10b981;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-warning {
  border-left: 4px solid #f59e0b;
}

.toast-warning .toast-icon {
  color: #f59e0b;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

.toast-icon {
  flex-shrink: 0;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.toast-message {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.toast-close {
  flex-shrink: 0;
  padding: 4px;
  color: #9ca3af;
  transition: color 0.2s;
}

.toast-close:hover {
  color: #4b5563;
}

.error-boundary {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9998;
}

.error-boundary-content {
  max-width: 500px;
  padding: 40px;
  text-align: center;
}

.error-icon {
  color: #ef4444;
  margin-bottom: 24px;
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16px;
}

.error-message {
  font-size: 16px;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 24px;
}

.error-details {
  margin-bottom: 32px;
  text-align: left;
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}

.error-details details {
  cursor: pointer;
}

.error-details summary {
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.error-details pre {
  overflow-x: auto;
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 12px;
  border-radius: 4px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.offline-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-weight: 500;
  z-index: 9999;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-100%);
}
</style>