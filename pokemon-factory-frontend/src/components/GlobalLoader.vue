<template>
  <transition name="fade">
    <div v-if="visible" class="global-loader-overlay">
      <div class="loader-content">
        <div class="spinner">
          <div class="double-bounce1"></div>
          <div class="double-bounce2"></div>
        </div>
        <p v-if="message" class="loader-message">{{ message }}</p>
      </div>
    </div>
  </transition>
</template>

<script>
import { ref, computed } from 'vue'

const isVisible = ref(false)
const message = ref('')

export default {
  name: 'GlobalLoader',
  setup() {
    const visible = computed(() => isVisible.value)
    
    return {
      visible,
      message
    }
  },
  methods: {
    show(msg = '加载中...') {
      message.value = msg
      isVisible.value = true
    },
    hide() {
      isVisible.value = false
    }
  }
}

export const useGlobalLoader = () => {
  return {
    show: (msg = '加载中...') => {
      message.value = msg
      isVisible.value = true
    },
    hide: () => {
      isVisible.value = false
    }
  }
}
</script>

<style scoped>
.global-loader-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loader-content {
  text-align: center;
  color: white;
}

.spinner {
  width: 60px;
  height: 60px;
  position: relative;
  margin: 0 auto 20px;
}

.double-bounce1,
.double-bounce2 {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #ffffff;
  opacity: 0.6;
  position: absolute;
  top: 0;
  left: 0;
  animation: bounce 2.0s infinite ease-in-out;
}

.double-bounce2 {
  animation-delay: -1.0s;
}

@keyframes bounce {
  0%,
  100% {
    transform: scale(0);
  }
  50% {
    transform: scale(1);
  }
}

.loader-message {
  font-size: 16px;
  font-weight: 500;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>