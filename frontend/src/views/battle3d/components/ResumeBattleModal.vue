<template>
  <div class="modal-overlay" @click.self="$emit('new')">
    <div class="modal-content">
      <div class="modal-icon">⚔️</div>
      <h2 class="modal-title">{{ t('发现未完成的对战', 'Unfinished Battle Found') }}</h2>
      <p class="modal-desc">
        {{ t('检测到上次有未完成的对战记录，是否继续？', 'An unfinished battle was detected. Continue?') }}
      </p>
      <div class="modal-actions">
        <button class="btn btn-primary" @click="$emit('resume')">
          🔄 {{ t('继续对战', 'Resume Battle') }}
        </button>
        <button class="btn btn-secondary" @click="$emit('new')">
          ✨ {{ t('开始新对战', 'New Battle') }}
        </button>
      </div>
      <p class="modal-hint">
        {{ t('游客模式下对战记录仅在当前浏览器有效', 'Guest battles are browser-local only') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

defineEmits<{
  resume: []
  new: []
}>()
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.modal-content {
  background: #1e1e2e;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
  animation: slideUp 0.3s ease;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 12px;
}

.modal-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 24px;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.5);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-hint {
  margin-top: 16px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
