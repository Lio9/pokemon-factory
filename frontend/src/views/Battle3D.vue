<template>
  <div class="battle3d-container">
    <!-- 左侧：3D 场景 -->
    <div class="battle3d-scene-area">
      <div ref="canvasContainer" class="battle3d-canvas" />
      
      <!-- 加载屏幕 -->
      <div v-if="isLoading" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-pokeball">
            <div class="pokeball-top"></div>
            <div class="pokeball-center"></div>
            <div class="pokeball-bottom"></div>
          </div>
          <h2 class="loading-title">{{ t('正在加载对战', 'Loading Battle') }}</h2>
          <div class="loading-bar">
            <div class="loading-fill" :style="{ width: loadingProgress + '%' }" />
          </div>
          <p class="loading-message">{{ loadingMessage }}</p>
        </div>
      </div>

      <!-- 场景内浮动信息 -->
      <div v-if="summary" class="scene-float-info">
        <div class="float-status" :class="statusClass">
          <span class="status-icon">{{ statusIcon }}</span>
          {{ statusText }}
        </div>
        <div v-if="summary.currentRound" class="float-round">
          <span class="round-label">{{ t('回合', 'Turn') }}</span>
          <span class="round-number">{{ summary.currentRound }}</span>
        </div>
      </div>

      <!-- 场景控制按钮 -->
      <div class="scene-controls">
        <button class="scene-btn" @click="toggleCameraMode" :title="t('切换视角', 'Toggle Camera')">
          <span class="btn-icon">📷</span>
        </button>
        <button class="scene-btn" @click="cyclePerformanceLevel" :title="t('性能等级', 'Performance')">
          <span class="btn-icon">⚡</span>
        </button>
        <button class="scene-btn" @click="showDebugPanel = !showDebugPanel" :title="t('调试面板', 'Debug')">
          <span class="btn-icon">🐛</span>
        </button>
      </div>
    </div>

    <!-- 右侧：控制面板 -->
    <div class="battle3d-panel">
      <!-- 面板头部 -->
      <div class="panel-header">
        <div class="panel-title-area">
          <span class="panel-icon">⚔️</span>
          <span class="panel-title">{{ t('对战控制', 'Battle Control') }}</span>
        </div>
        <div class="panel-actions">
          <button class="icon-btn" :class="{ active: !isMuted }" @click="toggleMute">
            {{ isMuted ? '🔇' : '🔊' }}
          </button>
        </div>
      </div>

      <!-- 无战斗时：开始面板 -->
      <div v-if="!summary" class="panel-content start-panel">
        <div class="start-hero">
          <div class="hero-icon">⚔️</div>
          <h2 class="hero-title">{{ t('准备对战', 'Ready to Battle') }}</h2>
          <p class="hero-subtitle">{{ t('选择模式开始战斗', 'Choose mode to start') }}</p>
        </div>

        <div class="format-selector">
          <div class="format-label">{{ t('对战格式', 'Battle Format') }}</div>
          <div class="format-row">
            <button
              v-for="f in formats"
              :key="f.id"
              class="format-btn"
              :class="{ active: battleFormat === f.id }"
              @click="setBattleFormat(f.id)"
            >
              <span class="format-icon">{{ f.icon }}</span>
              <span class="format-name">{{ f.label }}</span>
            </button>
          </div>
        </div>

        <div class="start-actions">
          <button class="btn btn-primary btn-large" :disabled="isBusy" @click="handleStartBattle">
            <span class="btn-content">
              <span class="btn-icon">⚔️</span>
              <span>{{ busyAction === 'start-manual' ? t('创建中...', 'Starting...') : t('手动对战', 'Manual Battle') }}</span>
            </span>
          </button>
          <button v-if="isAuthenticated" class="btn btn-purple" :disabled="isBusy" @click="startFactoryChallenge">
            <span class="btn-content">
              <span class="btn-icon">🏟️</span>
              <span>{{ t('工厂挑战', 'Factory Run') }}</span>
            </span>
          </button>
          <button v-if="isAuthenticated" class="btn btn-green" :disabled="isBusy" @click="startAsyncBattle">
            <span class="btn-content">
              <span class="btn-icon">⏩</span>
              <span>{{ t('异步模拟', 'Async Sim') }}</span>
            </span>
          </button>
        </div>

        <div class="start-tips">
          <div class="tip-item" v-if="!isAuthenticated">
            <span class="tip-icon">💡</span>
            <span>{{ t('游客模式可直接对战', 'Guest mode available') }}</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">⌨️</span>
            <span>{{ t('Enter 提交 · R 刷新 · F 认输', 'Enter submit · R refresh · F forfeit') }}</span>
          </div>
        </div>
      </div>

      <!-- 有战斗时：信息和操作 -->
      <div v-if="summary" class="panel-content battle-panel">
        <!-- 预览阶段：选择宝可梦 -->
        <div v-if="isPreviewPhase" class="preview-section">
          <!-- 步骤指示器 -->
          <div class="step-indicator">
            <div class="step" :class="{ active: selectedRosterIndexes.length < rosterLimit, done: selectedRosterIndexes.length >= rosterLimit }">
              <span class="step-num">1</span>
              <span class="step-text">{{ t('选', 'Pick ') }}{{ rosterLimit }}{{ t('只参战', ' to battle') }}</span>
            </div>
            <div class="step-divider"></div>
            <div class="step" :class="{ active: selectedRosterIndexes.length >= rosterLimit && leadRosterIndexes.length < 2 }">
              <span class="step-num">2</span>
              <span class="step-text">{{ t('选2只首发', 'Pick 2 leads') }}</span>
            </div>
          </div>

          <!-- 当前操作提示 -->
          <div class="action-hint">
            <template v-if="selectedRosterIndexes.length < rosterLimit">
              👆 {{ t('点击宝可梦卡片选择参战', 'Click cards to select for battle') }}
              <span class="hint-count">({{ selectedRosterIndexes.length }}/{{ rosterLimit }})</span>
            </template>
            <template v-else-if="leadRosterIndexes.length < 2">
              ⭐ {{ t('点击已选中的宝可梦设为首发', 'Click selected cards to set as lead') }}
              <span class="hint-count">({{ leadRosterIndexes.length }}/2)</span>
            </template>
            <template v-else>
              ✅ {{ t('选择完成！点击确认开始对战', 'Ready! Click confirm to start') }}
            </template>
          </div>

          <div class="roster-grid">
            <button
              v-for="(p, i) in playerRoster"
              :key="i"
              class="roster-card"
              :class="{
                selected: selectedRosterIndexes.includes(i),
                lead: leadRosterIndexes.includes(i),
                selectable: selectedRosterIndexes.length < rosterLimit || selectedRosterIndexes.includes(i)
              }"
              @click="handleRosterClick(Number(i))"
            >
              <div class="card-glow" v-if="selectedRosterIndexes.includes(i)"></div>
              <img :src="getSpriteUrl(p)" :alt="p.name" class="roster-img" @error="handleImgError" />
              <div class="roster-name">{{ p.name || p.name_en }}</div>
              <div class="roster-types">
                <span
                  v-for="type in (p.types || []).slice(0, 2)"
                  :key="type.type_id"
                  class="type-badge"
                  :style="{ background: getTypeColor(type.name_en || type.name) }"
                >
                  {{ type.name || type.name_en }}
                </span>
              </div>
              <!-- 状态指示 -->
              <div v-if="leadRosterIndexes.includes(i)" class="lead-badge">
                <span>⭐</span> {{ t('首发', 'Lead') }}
              </div>
              <div v-else-if="selectedRosterIndexes.includes(i)" class="selected-badge">
                ✓ {{ t('参战', 'Battle') }}
              </div>
            </button>
          </div>
        </div>

        <!-- 双方信息（使用子组件） -->
        <TeamInfo v-if="!isPreviewPhase" :player-mons="playerActiveMons" :opponent-mons="opponentActiveMons" />

        <!-- 场地效果 -->
        <div v-if="fieldChips.length" class="field-row">
          <span v-for="(chip, i) in fieldChips" :key="i" class="chip" :class="chip.cls">
            <span class="chip-icon">{{ chip.icon }}</span>
            {{ chip.l }}
          </span>
        </div>

        <!-- 招式选择（使用子组件） -->
        <MoveSelector
          v-if="isActionPhase"
          :player-active-mons="playerActiveMons"
          :opponent-active-mons="opponentActiveMons"
          :selected-slot="selectedSlot"
          :selected-move-index="selectedMoveIdx"
          :selected-targets="selectedTargets"
          :selected-special-systems="selectedSpecialSystems"
          :disabled="isBusy"
          :needs-target="needsTarget"
          :has-special-system="hasSpecialSystem"
          :available-special-systems="availableSpecialSystems"
          :special-system-label="specialSystemLabel"
          @select-move="selectMove"
          @select-target="selectTarget"
          @toggle-special="toggleSpecialSystem"
        />

        <!-- 替补选择 -->
        <div v-if="isReplacementPhase" class="replacement-section">
          <div class="section-header">
            <span class="section-icon">🔄</span>
            <span class="section-title">{{ t('选择替补', 'Choose Replacement') }}</span>
          </div>
          <div class="replacement-grid">
            <button
              v-for="option in replacementBenchOptions"
              :key="option.value"
              class="replace-card"
              :class="{ active: selectedReplacementIndexes.includes(option.value) }"
              @click="toggleReplacement(option.value)"
            >
              <div class="replace-name">{{ option.label }}</div>
              <div class="replace-hp">
                <div class="hp-bar-mini">
                  <div class="hp-fill-mini" :style="{ width: (option.hp / option.maxHp * 100) + '%' }"></div>
                </div>
                <span class="hp-text">{{ option.hp }}/{{ option.maxHp }}</span>
              </div>
            </button>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-row">
          <button v-if="canSubmitMove" class="btn btn-primary action-main" :disabled="isBusy" @click="handleSubmitMove">
            <span class="btn-content">
              <span>{{ isBusy ? '...' : t('提交回合', 'Submit Turn') }}</span>
              <span class="shortcut-badge">Enter</span>
            </span>
          </button>
          <button v-if="isPreviewPhase" class="btn btn-primary action-main" :disabled="isBusy || !canConfirmPreview" @click="confirmPreview">
            <span class="btn-content">
              <span>{{ t('确认预览', 'Confirm') }}</span>
              <span class="shortcut-badge">Enter</span>
            </span>
          </button>
          <button v-if="isReplacementPhase" class="btn btn-primary action-main" :disabled="isBusy || !canConfirmReplacement" @click="confirmReplacement">
            <span class="btn-content">
              <span>{{ t('确认替补', 'Confirm') }}</span>
              <span class="shortcut-badge">Enter</span>
            </span>
          </button>
          <button class="btn btn-secondary" :disabled="isBusy" @click="refreshStatus" :title="t('刷新 (R)', 'Refresh (R)')">🔄</button>
          <button v-if="summary?.status === 'running'" class="btn btn-danger" :disabled="isBusy" @click="handleForfeit" :title="t('认输 (F)', 'Forfeit (F)')">🏳️</button>
        </div>

        <!-- 战斗日志 -->
        <div class="log-section">
          <BattleLog
            :rounds="summary.rounds || []"
            :current-round="summary.currentRound"
            :auto-expand="true"
          />
        </div>
      </div>
    </div>

    <!-- 调试面板 -->
    <DebugPanel
      v-if="showDebugPanel"
      :fps="fps"
      :phase="currentPhase"
      :logs="debugLogs"
      :stats="debugStats"
      @close="showDebugPanel = false"
    />

    <!-- 结算弹窗 -->
    <SettlementModal
      v-if="settlement"
      :settlement="settlement"
      :factory-run="factoryRun"
      @close="onSettlementClose"
      @continue="nextFactoryBattle"
      @reset="resetBattleState({ keepFactoryRun: false })"
    />

    <!-- 恢复对战弹窗 -->
    <ResumeBattleModal
      v-if="showResumeModal"
      @resume="handleResumeBattle"
      @new="handleNewBattle"
    />

    <!-- 首次教程 -->
    <BattleTutorial
      :show="showTutorial"
      @close="showTutorial = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useThreeSceneEnhanced } from '../composables/battle3d/useThreeSceneEnhanced'
import { useAudioSystem } from '../composables/battle3d/useAudioSystem'
import { useMobileInteraction } from '../composables/battle3d/useMobileInteraction'
import { useBattleEngine } from '../composables/battle3d/useBattleEngine'
import { useBattlePageState } from '../composables/useBattlePageState'
import { useLocale } from '../composables/useLocale'
import { debugLogger } from './battle3d/utils/debug'
import { getTypeColor } from './battle3d/utils/typeColors'
import DebugPanel from './battle3d/components/DebugPanel.vue'
import SettlementModal from './battle3d/components/SettlementModal.vue'
import ResumeBattleModal from './battle3d/components/ResumeBattleModal.vue'
import BattleTutorial, { shouldShowTutorial } from './battle3d/components/BattleTutorial.vue'
import TeamInfo from './battle3d/components/TeamInfo.vue'
import MoveSelector from './battle3d/components/MoveSelector.vue'
import BattleLog from './battle3d/components/BattleLog.vue'
import { Battlefield } from './battle3d/core/BattleField'
import type { PerformanceLevel } from '../composables/battle3d/useThreeSceneEnhanced'

// 精灵图 URL
function getSpriteUrl(pokemon: any): string {
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  return id ? `/api/pokedex/images/pokemon/${id}.png` : '/pokemon-placeholder.svg'
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img) img.src = '/pokemon-placeholder.svg'
}

// ===== 国际化 =====
const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string, params?: any) => tr(zh, en, '', params || {})

// ===== DOM =====
const canvasContainer = ref<HTMLElement | null>(null)
const logContainer = ref<HTMLElement | null>(null)

// ===== UI 状态 =====
const showDebugPanel = ref(false)
const showLog = ref(false)
const isLoading = ref(true)
const loadingProgress = ref(0)
const loadingMessage = ref('')
const expandedRounds = ref(new Set<number>())
const showResumeModal = ref(false)
const pendingBattleId = ref<string | null>(null)
const showTutorial = ref(shouldShowTutorial())

// ===== 格式 =====
const formats = [
  { id: 'vgc-doubles', label: t('双打64', 'Doubles'), icon: '👥' },
  { id: 'vgc63', label: t('单打63', 'Singles'), icon: '👤' },
  { id: 'gen9singles', label: t('9代单打', 'Gen9'), icon: '🎮' }
]

// ===== 战斗状态 =====
const battleState = useBattlePageState() as any
const {
  summary, battleFormat, busyAction, isBusy, isAuthenticated, currentBattleId,
  factoryRun, settlement,
  startBattle, startAsyncBattle, startFactoryChallenge,
  setBattleFormat, confirmPreview, confirmReplacement,
  submitMove, refreshStatus, forfeitBattle,
  resetBattleState, nextFactoryBattle, onSettlementClose,
  playerActiveMons, opponentActiveMons, playerRoster, replacementBenchOptions,
  selectedActions, selectedMoves, selectedTargets: rawSelectedTargets, selectedSpecialSystems: rawSelectedSpecialSystems,
  selectedRosterIndexes, leadRosterIndexes, selectedReplacementIndexes,
  setSelectedAction, setSelectedMove, setSelectedTarget, setSelectedSpecialSystem,
  toggleRoster, toggleLead, toggleReplacement,
  canSubmitMove, canConfirmPreview, canConfirmReplacement,
  isPreviewPhase, isReplacementPhase, rosterLimit,
  availableSpecialSystems: getAvailableSpecialSystems
} = battleState

const selectedTargets = rawSelectedTargets as Record<string, any>
const selectedSpecialSystems = rawSelectedSpecialSystems as Record<string, any>

// ===== 3D 场景 =====
const {
  scene, camera, renderer, controls, fps, performanceStats,
  initScene, addToScene, removeFromScene, startRenderLoop, stopRenderLoop, dispose: disposeScene,
  isReady: sceneReady, setPerformanceLevel, getPerformanceLevel
} = useThreeSceneEnhanced(canvasContainer, {
  enableShadows: true, fov: 60, enableFog: true, enableSkybox: true, performanceLevel: 'high'
})

// ===== 音效 =====
const { isInitialized: audioReady, isMuted, init: initAudio, playSound, playAttackSound, toggleMute } = useAudioSystem()

// ===== 移动端 =====
const { isMobile, hasTouch, triggerHaptic, onGesture } = useMobileInteraction()

// ===== 战场 =====
const battlefield = ref<Battlefield | null>(null)

// ===== 战斗引擎 =====
const {
  currentPhase, isEngineReady, entities,
  initEngine, dispose: disposeEngine,
  playAttackAnimation, playHealAnimation, playTerastallizeAnimation,
  updateEffects
} = useBattleEngine({ scene, battlefield }, summary)

// ===== 调试 =====
const debugLogs = computed(() => debugLogger.getRecentLogs(30))
const debugStats = computed(() => ({
  ...debugLogger.getStats(),
  fps: fps.value,
  drawCalls: performanceStats.value.drawCalls,
  triangles: performanceStats.value.triangles,
  memoryUsage: performanceStats.value.memoryUsage,
  perfLevel: getPerformanceLevel()
}))

// ===== 状态文本 =====
const statusText = computed(() => {
  if (!summary.value) return ''
  if (isPreviewPhase.value) return t('预览中', 'Preview')
  if (isReplacementPhase.value) return t('补位中', 'Replace')
  if (summary.value.status === 'completed') {
    return summary.value.winner === 'player' ? t('胜利!', 'Victory!') : t('失败', 'Defeat')
  }
  return t('第 {r} 回合', 'Turn {r}', { r: summary.value.currentRound || 0 })
})

const statusIcon = computed(() => {
  if (!summary.value) return ''
  if (isPreviewPhase.value) return '👀'
  if (isReplacementPhase.value) return '🔄'
  if (summary.value.status === 'completed') {
    return summary.value.winner === 'player' ? '🏆' : '💀'
  }
  return '⚔️'
})

const statusClass = computed(() => {
  if (!summary.value) return ''
  if (summary.value.status === 'completed') return summary.value.winner === 'player' ? 'win' : 'lose'
  if (isPreviewPhase.value) return 'preview'
  if (isReplacementPhase.value) return 'replace'
  return 'running'
})

const isActionPhase = computed(() => {
  return summary.value?.status === 'running' && summary.value?.phase !== 'team-preview' && summary.value?.phase !== 'replacement'
})

// ===== 场地效果 =====
const fieldChips = computed(() => {
  const fe = summary.value?.fieldEffects || {}
  const chips: Array<{ l: string; cls: string; icon: string }> = []
  const p = (l: string, k: string, cls: string, icon: string) => {
    const v = Number(fe[k] || 0)
    if (v > 0) chips.push({ l: v > 1 ? `${l} ${v}T` : l, cls, icon })
  }
  p('顺风', 'playerTailwindTurns', 'chip-blue', '💨')
  p('顺风', 'opponentTailwindTurns', 'chip-red', '💨')
  p('戏法空间', 'trickRoomTurns', 'chip-purple', '🌀')
  p('雨天', 'rainTurns', 'chip-cyan', '🌧️')
  p('晴天', 'sunTurns', 'chip-amber', '☀️')
  p('沙暴', 'sandTurns', 'chip-orange', '🏜️')
  return chips
})

// ===== 辅助函数 =====
function hpPercent(mon: any): number {
  const max = mon?.stats?.hp || mon?.currentHp || 1
  return Math.max(0, Math.min(100, ((mon?.currentHp || 0) / max) * 100))
}

function hpColor(mon: any): string {
  const pct = hpPercent(mon)
  if (pct <= 20) return '#ef4444'
  if (pct <= 50) return '#fbbf24'
  return '#4ade80'
}

function statusLabel(c: string): string {
  const m: Record<string, string> = { paralysis: 'PAR', burn: 'BRN', freeze: 'FRZ', sleep: 'SLP', poison: 'PSN', toxic: 'TOX' }
  return m[c] || c
}

function specialSystemLabel(sys: string): string {
  const m: Record<string, string> = { tera: '太晶', mega: 'Mega', 'z-move': 'Z', dynamax: '极巨' }
  return m[sys] || sys
}

function hasSpecialSystem(mon: any): boolean { return (mon?.specialSystems || []).length > 0 }
function availableSpecialSystems(mon: any): string[] { return getAvailableSpecialSystems ? getAvailableSpecialSystems(mon) : [] }

// ===== 招式选择 =====
const selectedMoveIndexes = ref<Record<number, number>>({})
const selectedMoveObjects = ref<Record<number, any>>({})
const selectedSlot = computed(() => {
  const slots = Object.keys(selectedMoveIndexes.value)
  return slots.length > 0 ? Number(slots[0]) : -1
})
const selectedMoveIdx = computed(() => {
  return selectedSlot.value >= 0 ? selectedMoveIndexes.value[selectedSlot.value] : -1
})

function isMoveSelected(s: number | string, m: number | string) { return selectedMoveIndexes.value[Number(s)] === Number(m) }

function selectMove(s: number | string, m: number | string, move: any) {
  const slot = Number(s)
  selectedMoveIndexes.value[slot] = Number(m)
  selectedMoveObjects.value[slot] = move
  setSelectedMove(`slot-${slot}`, move.name_en || move.name)
  if (Number(move.target_id || 10) !== 10) setSelectedAction(`action-slot-${slot}`, 'move')
  playSound('button_click')
}

function needsTarget(s: number | string): boolean {
  const move = selectedMoveObjects.value[Number(s)]
  return move ? Number(move.target_id || 10) === 10 : false
}

function selectTarget(s: number | string, t: number | string) {
  setSelectedTarget(`target-slot-${Number(s)}`, Number(t))
  setSelectedAction(`action-slot-${Number(s)}`, 'move')
}

function toggleSpecialSystem(s: number | string, sys: string) {
  const slot = Number(s)
  const cur = selectedSpecialSystems.value[`special-slot-${slot}`]
  setSelectedSpecialSystem(`special-slot-${slot}`, cur === sys ? null : sys)
}

// ===== 相机 =====
let cameraMode = 0
function toggleCameraMode() {
  cameraMode = (cameraMode + 1) % 3
  if (!camera.value) return
  const cam = camera.value
  if (cameraMode === 0) cam.position.set(0, 12, 14)
  else if (cameraMode === 1) cam.position.set(0, 20, 0.1)
  else cam.position.set(18, 8, 0)
  cam.lookAt(0, 0, 0)
  controls.value?.update()
}

// ===== 性能 =====
const performanceLevels: PerformanceLevel[] = ['low', 'medium', 'high', 'ultra']
const currentPerfLevelIndex = ref(2)
function cyclePerformanceLevel() {
  currentPerfLevelIndex.value = (currentPerfLevelIndex.value + 1) % performanceLevels.length
  setPerformanceLevel(performanceLevels[currentPerfLevelIndex.value])
}

// ===== 日志 =====
function toggleRound(i: number | string) {
  const n = Number(i)
  const s = new Set(expandedRounds.value)
  s.has(n) ? s.delete(n) : s.add(n)
  expandedRounds.value = s
}

function logEventClass(e: string): string {
  if (/收回|派出|换人/.test(e)) return 'e-switch'
  if (/伤害/.test(e)) return 'e-damage'
  if (/回复|治愈/.test(e)) return 'e-heal'
  return ''
}

// ===== 游戏循环 =====
let animId: number | null = null
let lastTime = 0

function gameLoop(time: number) {
  const delta = (time - lastTime) / 1000
  lastTime = time
  updateEffects(delta)
  animId = requestAnimationFrame(gameLoop)
}

// ===== 恢复对战逻辑 =====
const BATTLE_STORAGE_KEY = 'pokemon-factory-battle'

function checkExistingBattle() {
  try {
    const savedBattleId = localStorage.getItem(BATTLE_STORAGE_KEY)
    if (savedBattleId && isAuthenticated.value) {
      pendingBattleId.value = savedBattleId
      showResumeModal.value = true
      return true
    } else if (savedBattleId) {
      localStorage.removeItem(BATTLE_STORAGE_KEY)
    }
  } catch { /* ignore */ }
  return false
}

function handleResumeBattle() {
  showResumeModal.value = false
  if (pendingBattleId.value) {
    currentBattleId.value = pendingBattleId.value
    refreshStatus(true)
    playSound('switch')
  }
}

function handleNewBattle() {
  showResumeModal.value = false
  pendingBattleId.value = null
  try { localStorage.removeItem(BATTLE_STORAGE_KEY) } catch { /* ignore */ }
  resetBattleState({ keepFactoryRun: false })
}

// ===== 预览选人逻辑 =====
function handleRosterClick(index: number) {
  const isSelected = selectedRosterIndexes.value.includes(index)
  const isLead = leadRosterIndexes.value.includes(index)

  if (!isSelected) {
    // 未选中：选中参战（如果还没选满）
    if (selectedRosterIndexes.value.length < rosterLimit.value) {
      toggleRoster(index)
      playSound('button_click')
    }
  } else if (!isLead) {
    // 已选中但不是首发：设为首发（如果还没选满2个首发）
    if (leadRosterIndexes.value.length < 2) {
      toggleLead(index)
      playSound('switch')
    } else {
      // 已经有2个首发了，替换掉第一个
      toggleLead(index)
      playSound('switch')
    }
  } else {
    // 已经是首发：取消首发
    toggleLead(index)
    playSound('button_click')
  }
}

function handleStartBattle() {
  startBattle()
  playSound('switch')
}

function handleSubmitMove() {
  submitMove()
  playSound('attack_normal')
}

function handleForfeit() {
  forfeitBattle()
  playSound('faint')
}

// ===== 生命周期 =====
onMounted(() => {
  loadingMessage.value = t('初始化场景...', 'Init scene...')
  loadingProgress.value = 30

  initScene()
  loadingProgress.value = 50

  if (scene.value) {
    battlefield.value = new Battlefield(scene.value)
    loadingProgress.value = 70

    initEngine()
    loadingProgress.value = 85

    startRenderLoop()
    lastTime = performance.now()
    animId = requestAnimationFrame(gameLoop)
    loadingProgress.value = 100

    initAudio().catch(() => { /* 用户未交互时正常 */ })

    isLoading.value = false
    checkExistingBattle()
    window.addEventListener('keydown', handleKeydown)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (animId !== null) cancelAnimationFrame(animId)
  disposeEngine()
  if (battlefield.value) battlefield.value.dispose()
  disposeScene()
})

// ===== 键盘快捷键 =====
function handleKeydown(e: KeyboardEvent) {
  if ((e.target as HTMLElement).tagName === 'INPUT') return

  switch (e.key) {
    case '1':
    case '2':
    case '3':
    case '4':
      if (isActionPhase.value && playerActiveMons.value.length > 0) {
        const moveIdx = parseInt(e.key) - 1
        const mon = playerActiveMons.value[0]
        if (mon?.moves?.[moveIdx]) {
          selectMove(0, moveIdx, mon.moves[moveIdx])
        }
      }
      break
    case 'Enter':
    case ' ':
      e.preventDefault()
      if (canSubmitMove.value) handleSubmitMove()
      else if (isPreviewPhase.value && canConfirmPreview.value) { confirmPreview(); playSound('switch') }
      else if (isReplacementPhase.value && canConfirmReplacement.value) { confirmReplacement(); playSound('switch') }
      break
    case 'r':
    case 'R':
      refreshStatus()
      playSound('button_click')
      break
    case 'f':
    case 'F':
      if (summary.value?.status === 'running') handleForfeit()
      break
    case 'Escape':
      selectedMoveIndexes.value = {}
      playSound('button_click')
      break
  }
}

// ===== 监听回合计数 =====
watch(() => summary.value?.rounds?.length, (n, o) => {
  if (!n || n <= (o || 0)) return
  const latest = summary.value.rounds[n - 1]
  if (!latest?.events) return
  const text = latest.events.join(' ')
  if (/伤害/.test(text)) {
    const ok = Object.keys(entities).filter(k => k.startsWith('opponent'))
    const pk = Object.keys(entities).filter(k => k.startsWith('player'))
    if (ok.length && pk.length) playAttackAnimation(pk[0], ok[0])
  }
  if (/回复|治愈/.test(text)) {
    const pk = Object.keys(entities).filter(k => k.startsWith('player'))
    if (pk.length) playHealAnimation(pk[0])
  }
  expandedRounds.value.add(n - 1)
  nextTick(() => { if (logContainer.value) logContainer.value.scrollTop = logContainer.value.scrollHeight })
})
</script>

<style scoped>
/* ===== 全局容器 ===== */
.battle3d-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d0d2b 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #fff;
}

/* ===== 左侧：3D 场景 ===== */
.battle3d-scene-area {
  flex: 1;
  position: relative;
  min-width: 0;
}

.battle3d-canvas {
  width: 100%;
  height: 100%;
}

/* 场景浮动信息 */
.scene-float-info {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 10px;
  z-index: 10;
  animation: slideIn 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.float-status {
  padding: 8px 18px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 700;
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.status-icon {
  font-size: 16px;
}

.float-status.win { 
  background: linear-gradient(135deg, rgba(34,197,94,0.95), rgba(22,163,74,0.95)); 
  box-shadow: 0 8px 32px rgba(34,197,94,0.4), 0 0 0 1px rgba(255,255,255,0.1);
  animation: pulseGlow 2s ease-in-out infinite;
}

.float-status.lose { 
  background: linear-gradient(135deg, rgba(239,68,68,0.95), rgba(220,38,38,0.95)); 
  box-shadow: 0 8px 32px rgba(239,68,68,0.4);
}

.float-status.running { 
  background: linear-gradient(135deg, rgba(59,130,246,0.95), rgba(37,99,235,0.95)); 
  box-shadow: 0 8px 32px rgba(59,130,246,0.4);
}

.float-status.preview { 
  background: linear-gradient(135deg, rgba(251,191,36,0.95), rgba(245,158,11,0.95)); 
  color: #1a1a2e;
  box-shadow: 0 8px 32px rgba(251,191,36,0.4);
}

.float-status.replace { 
  background: linear-gradient(135deg, rgba(249,115,22,0.95), rgba(234,88,12,0.95)); 
  box-shadow: 0 8px 32px rgba(249,115,22,0.4);
}

@keyframes pulseGlow {
  0%, 100% { box-shadow: 0 8px 32px rgba(34,197,94,0.4); }
  50% { box-shadow: 0 8px 48px rgba(34,197,94,0.6); }
}

.float-round {
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 14px;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.round-label {
  color: rgba(255,255,255,0.6);
  font-size: 12px;
}

.round-number {
  font-weight: 800;
  font-size: 18px;
}

/* 场景控制按钮 */
.scene-controls {
  position: absolute;
  bottom: 16px;
  left: 16px;
  display: flex;
  gap: 8px;
  z-index: 10;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.scene-btn {
  width: 44px;
  height: 44px;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  cursor: pointer;
  backdrop-filter: blur(16px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.scene-btn:hover {
  background: rgba(255,255,255,0.15);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
}

.btn-icon {
  font-size: 20px;
}

/* ===== 右侧：控制面板 ===== */
.battle3d-panel {
  width: 400px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #1e1e3a 0%, #15152d 50%, #12122a 100%);
  border-left: 1px solid rgba(255,255,255,0.06);
  overflow: hidden;
  box-shadow: -12px 0 48px rgba(0,0,0,0.4);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, transparent 100%);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.panel-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-icon {
  font-size: 20px;
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.panel-actions { display: flex; gap: 8px; }

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
  background: rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.6);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.12);
  color: #fff;
}

.icon-btn.active {
  background: rgba(59,130,246,0.2);
  border-color: rgba(59,130,246,0.3);
  color: #60a5fa;
}

/* 面板内容区 */
.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-content::-webkit-scrollbar { width: 6px; }
.panel-content::-webkit-scrollbar-track { background: transparent; }
.panel-content::-webkit-scrollbar-thumb { 
  background: rgba(255,255,255,0.1); 
  border-radius: 3px;
}
.panel-content::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.2); }

/* ===== 开始面板 ===== */
.start-panel {
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 24px;
}

.start-hero {
  margin-bottom: 8px;
}

.hero-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #fff 0%, #a0a0ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.5);
}

.format-selector {
  width: 100%;
}

.format-label {
  font-size: 12px;
  font-weight: 700;
  color: rgba(255,255,255,0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.format-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.format-btn {
  flex: 1;
  padding: 14px 12px;
  border: 2px solid rgba(255,255,255,0.08);
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.format-btn:hover {
  border-color: rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.08);
  color: #fff;
  transform: translateY(-2px);
}

.format-btn.active {
  border-color: #3b82f6;
  background: linear-gradient(180deg, rgba(59,130,246,0.2) 0%, rgba(59,130,246,0.05) 100%);
  color: #fff;
  box-shadow: 0 0 24px rgba(59,130,246,0.3);
}

.format-icon {
  font-size: 24px;
}

.format-name {
  font-size: 12px;
  font-weight: 700;
}

.start-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.start-tips {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.4);
}

.tip-icon {
  font-size: 14px;
}

/* ===== 战斗面板 ===== */
.battle-panel { gap: 12px; }

/* ===== 预览阶段选人 ===== */
.preview-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: fadeIn 0.3s ease;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(255,255,255,0.03);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.06);
}

.step {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: all 0.3s ease;
}

.step.active {
  opacity: 1;
}

.step.done {
  opacity: 0.7;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.6);
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step.active .step-num {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 0 12px rgba(59,130,246,0.4);
}

.step.done .step-num {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
}

.step-text {
  font-size: 12px;
  font-weight: 700;
  color: rgba(255,255,255,0.7);
}

.step.active .step-text {
  color: #fff;
}

.step-divider {
  flex: 1;
  height: 2px;
  background: rgba(255,255,255,0.1);
  margin: 0 4px;
}

/* 操作提示 */
.action-hint {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
  text-align: center;
  padding: 10px;
  background: rgba(59,130,246,0.1);
  border-radius: 10px;
  border: 1px solid rgba(59,130,246,0.2);
}

.hint-count {
  font-weight: 700;
  color: #60a5fa;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.section-icon {
  font-size: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255,255,255,0.8);
}

.section-count {
  margin-left: auto;
  font-size: 14px;
  font-weight: 800;
  color: #60a5fa;
}

.roster-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.roster-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px;
  border: 2px solid rgba(255,255,255,0.06);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  inset: -1px;
  background: radial-gradient(ellipse at center, rgba(74,222,128,0.15) 0%, transparent 70%);
  pointer-events: none;
  animation: glowPulse 2s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.roster-card:hover {
  border-color: rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.08);
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0,0,0,0.4);
}

.roster-card.selected {
  border-color: #4ade80;
  background: linear-gradient(180deg, rgba(74,222,128,0.15) 0%, rgba(74,222,128,0.03) 100%);
  box-shadow: 0 0 24px rgba(74,222,128,0.3);
}

.roster-card.lead {
  border-color: #fbbf24;
  background: linear-gradient(180deg, rgba(251,191,36,0.15) 0%, rgba(251,191,36,0.03) 100%);
  box-shadow: 0 0 24px rgba(251,191,36,0.3);
}

.roster-card:not(.selectable) {
  opacity: 0.5;
  cursor: not-allowed;
}

.roster-card:not(.selectable):hover {
  transform: none;
  box-shadow: none;
}

.selected-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, #4ade80, #22c55e);
  color: #fff;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(74,222,128,0.5);
  display: flex;
  align-items: center;
  gap: 4px;
}

.roster-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 6px 12px rgba(0,0,0,0.4));
  transition: transform 0.3s ease;
}

.roster-card:hover .roster-img {
  transform: scale(1.15) translateY(-4px);
}

.roster-name {
  font-size: 13px;
  font-weight: 800;
  color: #fff;
  margin-top: 8px;
  text-align: center;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.roster-types {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.type-badge {
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 6px;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 1px 3px rgba(0,0,0,0.4);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.lead-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #1a1a2e;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(251,191,36,0.5);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== 替补选择 ===== */
.replacement-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.replacement-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.replace-card {
  padding: 12px 14px;
  border: 2px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.replace-card:hover {
  border-color: rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.06);
}

.replace-card.active {
  border-color: #4ade80;
  background: rgba(74,222,128,0.1);
}

.replace-name {
  font-size: 13px;
  font-weight: 700;
}

.replace-hp {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hp-bar-mini {
  flex: 1;
  height: 6px;
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
  overflow: hidden;
}

.hp-fill-mini {
  height: 100%;
  background: linear-gradient(90deg, #4ade80, #22c55e);
  border-radius: 3px;
  transition: width 0.4s ease;
}

.hp-text {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  font-weight: 600;
}

/* ===== 操作按钮 ===== */
.action-row {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-main {
  flex: 1;
}

.btn {
  padding: 12px 18px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.btn:disabled { 
  opacity: 0.4; 
  cursor: not-allowed; 
  transform: none !important;
  box-shadow: none !important;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 6px 20px rgba(59,130,246,0.4);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(59,130,246,0.5);
}

.shortcut-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(0,0,0,0.4);
  font-weight: 600;
  margin-left: 4px;
}

.btn-secondary {
  background: rgba(255,255,255,0.08);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.1);
  min-width: 48px;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(255,255,255,0.15);
  transform: translateY(-2px);
}

.btn-danger {
  background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(220,38,38,0.2));
  color: #f87171;
  border: 1px solid rgba(239,68,68,0.2);
  min-width: 48px;
}

.btn-danger:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(239,68,68,0.4), rgba(220,38,38,0.4));
  transform: translateY(-2px);
}

.btn-purple {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: #fff;
  box-shadow: 0 6px 20px rgba(139,92,246,0.4);
}

.btn-green {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 6px 20px rgba(34,197,94,0.4);
}

.btn-large {
  padding: 16px 24px;
  font-size: 16px;
  border-radius: 14px;
}

/* ===== 场地效果 ===== */
.field-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 8px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid;
}

.chip-icon {
  font-size: 12px;
}

.chip-blue { background: rgba(30,64,175,0.3); color: #93c5fd; border-color: rgba(30,64,175,0.4); }
.chip-red { background: rgba(153,27,27,0.3); color: #fca5a5; border-color: rgba(153,27,27,0.4); }
.chip-purple { background: rgba(88,28,135,0.3); color: #d8b4fe; border-color: rgba(88,28,135,0.4); }
.chip-cyan { background: rgba(21,94,117,0.3); color: #67e8f9; border-color: rgba(21,94,117,0.4); }
.chip-amber { background: rgba(146,64,14,0.3); color: #fcd34d; border-color: rgba(146,64,14,0.4); }
.chip-orange { background: rgba(124,45,18,0.3); color: #fdba74; border-color: rgba(124,45,18,0.4); }

/* ===== 日志区域 ===== */
.log-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ===== 加载屏幕 ===== */
.loading-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0d0d2b 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.loading-content { 
  text-align: center;
  animation: scaleIn 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.loading-pokeball {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  position: relative;
  animation: pokeballSpin 2s linear infinite;
}

@keyframes pokeballSpin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pokeball-top {
  width: 80px;
  height: 40px;
  background: linear-gradient(180deg, #ef4444, #dc2626);
  border-radius: 40px 40px 0 0;
  position: absolute;
  top: 0;
}

.pokeball-center {
  width: 80px;
  height: 8px;
  background: #1a1a2e;
  position: absolute;
  top: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pokeball-center::after {
  content: '';
  width: 20px;
  height: 20px;
  background: #fff;
  border: 4px solid #1a1a2e;
  border-radius: 50%;
}

.pokeball-bottom {
  width: 80px;
  height: 40px;
  background: #fff;
  border-radius: 0 0 40px 40px;
  position: absolute;
  bottom: 0;
}

.loading-title {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #fff 0%, #a0a0ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.loading-bar {
  width: 260px;
  height: 8px;
  background: rgba(255,255,255,0.08);
  border-radius: 4px;
  overflow: hidden;
  margin: 0 auto 16px;
  border: 1px solid rgba(255,255,255,0.05);
}

.loading-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899);
  border-radius: 4px;
  transition: width 0.3s ease;
  box-shadow: 0 0 20px rgba(59,130,246,0.5);
}

.loading-message {
  font-size: 14px;
  color: rgba(255,255,255,0.4);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .battle3d-container {
    flex-direction: column;
  }

  .battle3d-scene-area {
    height: 45vh;
  }

  .battle3d-panel {
    width: 100%;
    height: 55vh;
  }

  .panel-content {
    padding: 12px;
    gap: 10px;
  }

  .roster-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .roster-card {
    padding: 10px 8px;
  }

  .roster-img {
    width: 48px;
    height: 48px;
  }

  .btn {
    padding: 10px 14px;
    font-size: 13px;
  }

  .scene-controls {
    bottom: 12px;
    left: 12px;
  }

  .scene-btn {
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .battle3d-scene-area {
    height: 40vh;
  }

  .battle3d-panel {
    height: 60vh;
  }

  .roster-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .panel-header {
    padding: 12px 16px;
  }

  .panel-title {
    font-size: 14px;
  }

  .hero-title {
    font-size: 22px;
  }
}
</style>
