<template>
  <div class="log-section">
    <div class="log-header" @click="toggleLog">
      📜 {{ t('日志', 'Log') }}
      <span v-if="currentRound" class="log-round-badge">T{{ currentRound }}</span>
      <span class="log-toggle">{{ isExpanded ? '▼' : '▲' }}</span>
    </div>
    <div v-if="isExpanded" class="log-body" ref="logContainer">
      <div v-if="!rounds || rounds.length === 0" class="log-empty">
        {{ t('等待战斗开始...', 'Waiting for battle...') }}
      </div>
      <div v-for="(round, ri) in rounds" :key="ri" class="log-round">
        <div class="log-round-h" @click="toggleRound(ri)">
          <span class="log-arrow" :class="{ open: expandedRounds.has(ri) }">▶</span>
          {{ t('第', 'T') }}{{ ri + 1 }}{{ t('回合', '') }}
          <span class="log-count">{{ (round.events || []).length }}</span>
        </div>
        <div v-if="expandedRounds.has(ri)" class="log-events">
          <div
            v-for="(evt, ei) in (round.events || [])"
            :key="ei"
            class="log-evt"
            :class="getEventClass(evt)"
          >
            {{ evt }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

interface Props {
  rounds: any[]
  currentRound?: number
  autoExpand?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentRound: 0,
  autoExpand: true
})

const isExpanded = ref(false)
const logContainer = ref<HTMLElement | null>(null)
const expandedRounds = ref(new Set<number>())

function toggleLog() {
  isExpanded.value = !isExpanded.value
}

function toggleRound(index: number) {
  const s = new Set(expandedRounds.value)
  s.has(index) ? s.delete(index) : s.add(index)
  expandedRounds.value = s
}

function getEventClass(event: string): string {
  if (/收回|派出|换人/.test(event)) return 'e-switch'
  if (/伤害/.test(event)) return 'e-damage'
  if (/回复|治愈/.test(event)) return 'e-heal'
  if (/下降|降低/.test(event)) return 'e-debuff'
  return ''
}

// 自动展开最新回合
watch(() => props.rounds?.length, (n) => {
  if (!n || !props.autoExpand) return
  expandedRounds.value.add(n - 1)
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.log-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: rgba(0,0,0,0.3);
  border-radius: 8px;
  overflow: hidden;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: bold;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
  transition: background 0.2s;
}

.log-header:hover {
  background: rgba(255,255,255,0.05);
}

.log-round-badge {
  margin-left: auto;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(59,130,246,0.5);
  font-size: 10px;
}

.log-toggle {
  font-size: 10px;
  margin-left: 4px;
}

.log-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
  font-size: 11px;
}

.log-body::-webkit-scrollbar {
  width: 3px;
}

.log-body::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
}

.log-empty {
  text-align: center;
  color: rgba(255,255,255,0.4);
  padding: 20px;
  font-size: 12px;
}

.log-round {
  margin-bottom: 4px;
}

.log-round-h {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 0;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  font-weight: bold;
  transition: color 0.2s;
}

.log-round-h:hover {
  color: #fff;
}

.log-arrow {
  font-size: 8px;
  transition: transform 0.2s;
}

.log-arrow.open {
  transform: rotate(90deg);
}

.log-count {
  font-size: 9px;
  background: rgba(255,255,255,0.2);
  padding: 0 4px;
  border-radius: 3px;
  margin-left: auto;
}

.log-events {
  padding: 2px 0 4px 14px;
}

.log-evt {
  padding: 2px 0;
  color: rgba(255,255,255,0.7);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  font-size: 11px;
  line-height: 1.4;
}

.log-evt:last-child {
  border-bottom: none;
}

.e-switch {
  color: #60a5fa;
  font-weight: bold;
}

.e-damage {
  color: #f87171;
}

.e-heal {
  color: #4ade80;
}

.e-debuff {
  color: #fbbf24;
}
</style>
