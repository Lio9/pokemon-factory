<template>
  <div v-if="show" class="tutorial-overlay" @click.self="close">
    <div class="tutorial-content">
      <div class="tutorial-header">
        <h2>{{ t('🎮 3D 对战操作指南', '🎮 3D Battle Controls') }}</h2>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div class="tutorial-body">
        <div class="tutorial-section">
          <h3>🖱️ {{ t('鼠标操作', 'Mouse Controls') }}</h3>
          <ul>
            <li>{{ t('拖拽：旋转视角', 'Drag: Rotate view') }}</li>
            <li>{{ t('滚轮：缩放视角', 'Scroll: Zoom view') }}</li>
            <li>{{ t('点击招式：选择出招', 'Click move: Select move') }}</li>
            <li>{{ t('点击目标：选择攻击对象', 'Click target: Select target') }}</li>
          </ul>
        </div>

        <div class="tutorial-section">
          <h3>⌨️ {{ t('键盘快捷键', 'Keyboard Shortcuts') }}</h3>
          <div class="shortcut-grid">
            <div class="shortcut-item">
              <kbd>1</kbd><kbd>2</kbd><kbd>3</kbd><kbd>4</kbd>
              <span>{{ t('选择招式', 'Select move') }}</span>
            </div>
            <div class="shortcut-item">
              <kbd>Enter</kbd>
              <span>{{ t('提交回合', 'Submit turn') }}</span>
            </div>
            <div class="shortcut-item">
              <kbd>R</kbd>
              <span>{{ t('刷新状态', 'Refresh') }}</span>
            </div>
            <div class="shortcut-item">
              <kbd>F</kbd>
              <span>{{ t('认输', 'Forfeit') }}</span>
            </div>
            <div class="shortcut-item">
              <kbd>Esc</kbd>
              <span>{{ t('取消选择', 'Cancel') }}</span>
            </div>
          </div>
        </div>

        <div class="tutorial-section">
          <h3>📱 {{ t('移动端手势', 'Mobile Gestures') }}</h3>
          <ul>
            <li>{{ t('单指拖拽：旋转视角', 'One finger drag: Rotate view') }}</li>
            <li>{{ t('双指缩放：缩放视角', 'Pinch: Zoom view') }}</li>
            <li>{{ t('双击：提交回合', 'Double tap: Submit turn') }}</li>
            <li>{{ t('上滑：显示日志', 'Swipe up: Show log') }}</li>
          </ul>
        </div>

        <div class="tutorial-section">
          <h3>💡 {{ t('小贴士', 'Tips') }}</h3>
          <ul>
            <li>{{ t('招式颜色代表属性，选择时注意属性克制', 'Move colors indicate type - watch for type advantages') }}</li>
            <li>{{ t('血条颜色：绿色>50%，黄色20-50%，红色<20%', 'HP bar: green>50%, yellow 20-50%, red<20%') }}</li>
            <li>{{ t('右上角可切换性能等级和音效', 'Top right: switch performance and audio') }}</li>
          </ul>
        </div>
      </div>

      <div class="tutorial-footer">
        <label class="dont-show">
          <input type="checkbox" v-model="dontShowAgain" />
          {{ t('不再显示', "Don't show again") }}
        </label>
        <button class="btn-start" @click="close">
          {{ t('开始对战！', 'Start Battle!') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

const TUTORIAL_KEY = 'pokemon-battle3d-tutorial-seen'

interface Props {
  show: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  close: []
}>()

const dontShowAgain = ref(false)

function close() {
  if (dontShowAgain.value) {
    try {
      localStorage.setItem(TUTORIAL_KEY, 'true')
    } catch { /* ignore */ }
  }
  emit('close')
}

// 检查是否应该显示教程
export function shouldShowTutorial(): boolean {
  try {
    return !localStorage.getItem(TUTORIAL_KEY)
  } catch {
    return true
  }
}
</script>

<style scoped>
.tutorial-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

.tutorial-content {
  background: #1e1e2e;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

.tutorial-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tutorial-header h2 {
  font-size: 20px;
  color: #fff;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.tutorial-body {
  padding: 20px 24px;
}

.tutorial-section {
  margin-bottom: 20px;
}

.tutorial-section:last-child {
  margin-bottom: 0;
}

.tutorial-section h3 {
  font-size: 16px;
  color: #fff;
  margin: 0 0 12px 0;
}

.tutorial-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tutorial-section li {
  padding: 6px 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.tutorial-section li:last-child {
  border-bottom: none;
}

.tutorial-section li::before {
  content: '•';
  color: #3b82f6;
  margin-right: 8px;
}

.shortcut-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shortcut-item kbd {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  font-family: monospace;
}

.shortcut-item span {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.tutorial-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.dont-show {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  cursor: pointer;
}

.dont-show input {
  cursor: pointer;
}

.btn-start {
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn-start:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.5);
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
