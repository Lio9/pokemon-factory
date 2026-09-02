<template>
  <div class="settlement-overlay" @click.self="$emit('close')">
    <div class="settlement-modal" :class="{ 'settlement-win': settlement.won, 'settlement-lose': !settlement.won }">
      <!-- 结果标题 -->
      <div class="settlement-header">
        <div class="result-icon">{{ settlement.won ? '🏆' : '💀' }}</div>
        <h2 class="result-title">
          {{ settlement.won ? t('胜利！', 'Victory!') : t('失败...', 'Defeat...') }}
        </h2>
        <p class="result-subtitle">
          {{ settlement.won
            ? t('恭喜你赢得了这场对战！', 'Congratulations on winning this battle!')
            : t('不要灰心，再接再厉！', 'Don\'t give up, try again!')
          }}
        </p>
      </div>

      <!-- 积分变化 -->
      <div v-if="settlement.pointsDelta !== null && settlement.pointsDelta !== undefined" class="settlement-section">
        <div class="section-title">📊 {{ t('积分变化', 'Rating Change') }}</div>
        <div class="points-change" :class="settlement.pointsDelta >= 0 ? 'points-up' : 'points-down'">
          {{ settlement.pointsDelta >= 0 ? '+' : '' }}{{ settlement.pointsDelta }}
        </div>
      </div>

      <!-- 段位变化 -->
      <div v-if="settlement.tierChange" class="settlement-section">
        <div class="section-title">🏅 {{ t('段位变化', 'Tier Change') }}</div>
        <div class="tier-change" :class="'tier-' + settlement.tierChange">
          <span v-if="settlement.tierChange === 'promoted'">⬆️ {{ t('晋级成功！', 'Promoted!') }}</span>
          <span v-else>⬇️ {{ t('段位下降', 'Demoted') }}</span>
          <span v-if="settlement.newTierName" class="tier-name">{{ settlement.newTierName }}</span>
        </div>
      </div>

      <!-- 工厂挑战信息 -->
      <div v-if="settlement.factoryRound" class="settlement-section">
        <div class="section-title">🏭 {{ t('工厂挑战', 'Factory Run') }}</div>
        <div class="factory-info">
          <div class="factory-stat">
            <span class="stat-label">{{ t('当前轮次', 'Current Round') }}</span>
            <span class="stat-value">{{ settlement.factoryRound }}</span>
          </div>
          <div v-if="settlement.runWins !== null" class="factory-stat">
            <span class="stat-label">{{ t('胜场', 'Wins') }}</span>
            <span class="stat-value win">{{ settlement.runWins }}</span>
          </div>
          <div v-if="settlement.runLosses !== null" class="factory-stat">
            <span class="stat-label">{{ t('败场', 'Losses') }}</span>
            <span class="stat-value lose">{{ settlement.runLosses }}</span>
          </div>
        </div>
      </div>

      <!-- 奖励 -->
      <div v-if="settlement.runReward" class="settlement-section">
        <div class="section-title">🎁 {{ t('奖励', 'Reward') }}</div>
        <div class="reward-info">{{ settlement.runReward }}</div>
      </div>

      <!-- 操作按钮 -->
      <div class="settlement-actions">
        <button
          v-if="!settlement.runFinished && factoryRun"
          class="action-btn action-primary"
          @click="$emit('continue')"
        >
          ➡️ {{ t('下一轮', 'Next Round') }}
        </button>
        <button class="action-btn action-secondary" @click="$emit('reset')">
          🔄 {{ t('重新开始', 'New Battle') }}
        </button>
        <button class="action-btn action-ghost" @click="$emit('close')">
          {{ t('关闭', 'Close') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * SettlementModal.vue - 结算弹窗组件
 *
 * 显示战斗结束后的结算信息，包括：
 * - 胜负结果
 * - 积分变化
 * - 段位变化
 * - 工厂挑战进度
 * - 奖励信息
 */
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

interface Settlement {
  won: boolean
  pointsDelta: number | null
  tierChange: string | null
  newTierName: string | null
  factoryRound: number | null
  runFinished: boolean
  runWins: number | null
  runLosses: number | null
  runReward: string | null
}

defineProps<{
  settlement: Settlement
  factoryRun: any
}>()

defineEmits<{
  close: []
  continue: []
  reset: []
}>()
</script>

<style scoped>
.settlement-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.settlement-modal {
  width: min(480px, 90vw);
  max-height: 85vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #1e1e2e, #141422);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.4s ease;
}

.settlement-win {
  border-color: rgba(74, 222, 128, 0.3);
}

.settlement-lose {
  border-color: rgba(239, 68, 68, 0.3);
}

.settlement-header {
  text-align: center;
  padding: 32px 24px 24px;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 12px;
  animation: bounceIn 0.5s ease;
}

.result-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
}

.settlement-win .result-title {
  color: #4ade80;
}

.settlement-lose .result-title {
  color: #f87171;
}

.result-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.settlement-section {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 12px;
}

.points-change {
  font-size: 48px;
  font-weight: bold;
  text-align: center;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.points-up {
  color: #4ade80;
}

.points-down {
  color: #ef4444;
}

.tier-change {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  padding: 12px;
  border-radius: 12px;
}

.tier-promoted {
  background: rgba(74, 222, 128, 0.15);
  color: #4ade80;
}

.tier-demoted {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.tier-name {
  display: block;
  font-size: 14px;
  margin-top: 4px;
  opacity: 0.8;
}

.factory-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.factory-stat {
  text-align: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}

.stat-value.win { color: #4ade80; }
.stat-value.lose { color: #ef4444; }

.reward-info {
  text-align: center;
  font-size: 16px;
  color: #fbbf24;
  padding: 12px;
  background: rgba(251, 191, 36, 0.1);
  border-radius: 10px;
}

.settlement-actions {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}

.action-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.action-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.5);
}

.action-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.action-secondary:hover {
  background: rgba(255, 255, 255, 0.2);
}

.action-ghost {
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
}

.action-ghost:hover {
  color: #fff;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes bounceIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
