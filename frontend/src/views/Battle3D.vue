<template>
  <div class="battle3d-container">
    <!-- 3D 场景画布 -->
    <div ref="canvasContainer" class="battle3d-canvas" />

    <!-- 加载屏幕 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-icon">⚔️</div>
        <h2 class="loading-title">{{ t('正在加载 3D 对战', 'Loading 3D Battle') }}</h2>
        <div class="loading-bar">
          <div class="loading-fill" :style="{ width: loadingProgress + '%' }" />
        </div>
        <p class="loading-message">{{ loadingMessage }}</p>
        <p class="loading-tip">{{ t('提示：使用鼠标拖拽旋转视角', 'Tip: Drag to rotate view') }}</p>
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

    <!-- 顶部状态栏 -->
    <div class="battle3d-topbar">
      <div class="topbar-left">
        <span class="topbar-title">⚔️ {{ t('3D 对战工厂', '3D Battle Factory') }}</span>
        <span v-if="summary" class="topbar-status" :class="statusClass">
          {{ statusText }}
        </span>
        <span v-if="isMobile" class="topbar-mobile-badge">📱</span>
      </div>
      <div class="topbar-right">
        <!-- 音频控制 -->
        <button class="topbar-btn" @click="toggleMute" :title="isMuted ? t('开启音效', 'Unmute') : t('关闭音效', 'Mute')">
          {{ isMuted ? '🔇' : '🔊' }}
        </button>
        <!-- 性能等级 -->
        <button class="topbar-btn perf-btn" @click="cyclePerformanceLevel" :title="t('性能等级', 'Performance')">
          ⚡ {{ performanceLevels[currentPerfLevelIndex] }}
        </button>
        <button class="topbar-btn" @click="showDebugPanel = !showDebugPanel">
          🐛 {{ t('调试', 'Debug') }}
        </button>
        <button class="topbar-btn" @click="toggleCameraMode">
          📷 {{ t('视角', 'Camera') }}
        </button>
      </div>
    </div>

    <!-- 左侧：战斗信息面板 -->
    <div v-if="summary" class="battle3d-sidebar battle3d-sidebar-left">
      <!-- 对手信息 -->
      <div class="sidebar-section">
        <div class="section-title">🔴 {{ t('对手', 'Opponent') }}</div>
        <div v-for="(mon, i) in opponentActiveMons" :key="'opp-' + i" class="pokemon-info-card opponent">
          <div class="pokemon-name">{{ mon.name || mon.name_en }}</div>
          <div class="pokemon-level">Lv.{{ mon.level || 50 }}</div>
          <div class="hp-bar-container">
            <div class="hp-bar" :style="{ width: hpPercent(mon) + '%', background: hpColor(mon) }" />
          </div>
          <div class="hp-text">{{ mon.currentHp }} / {{ mon.stats?.hp || mon.currentHp }}</div>
          <div class="pokemon-types">
            <span
              v-for="type in (mon.types || [])"
              :key="type.type_id"
              class="type-badge"
              :style="{ background: getTypeColor(type.name_en || type.name) }"
            >
              {{ type.name || type.name_en }}
            </span>
          </div>
        </div>
      </div>

      <!-- 场地效果 -->
      <div v-if="fieldChips.length" class="sidebar-section">
        <div class="section-title">🌍 {{ t('场地', 'Field') }}</div>
        <div class="field-chips">
          <span v-for="(chip, i) in fieldChips" :key="i" class="field-chip" :class="chip.cls">
            {{ chip.l }}
          </span>
        </div>
      </div>
    </div>

    <!-- 右侧：玩家操作面板 -->
    <div v-if="summary" class="battle3d-sidebar battle3d-sidebar-right">
      <!-- 玩家宝可梦信息 -->
      <div class="sidebar-section">
        <div class="section-title">🟢 {{ t('我方', 'Player') }}</div>
        <div v-for="(mon, i) in playerActiveMons" :key="'plr-' + i" class="pokemon-info-card player">
          <div class="pokemon-name">{{ mon.name || mon.name_en }}</div>
          <div class="pokemon-level">Lv.{{ mon.level || 50 }}</div>
          <div class="hp-bar-container">
            <div class="hp-bar" :style="{ width: hpPercent(mon) + '%', background: hpColor(mon) }" />
          </div>
          <div class="hp-text">{{ mon.currentHp }} / {{ mon.stats?.hp || mon.currentHp }}</div>
          <div class="status-badges">
            <span v-if="mon.condition" class="status-badge" :class="'status-' + mon.condition">
              {{ statusLabel(mon.condition) }}
            </span>
            <span v-if="mon.terastallized" class="status-badge status-tera">Tera</span>
          </div>
        </div>
      </div>

      <!-- 招式选择（行动选择阶段） -->
      <div v-if="isActionPhase" class="sidebar-section">
        <div class="section-title">⚔️ {{ t('招式', 'Moves') }}</div>
        <div v-for="(mon, slotIdx) in playerActiveMons" :key="'moves-' + slotIdx" class="move-panel">
          <div class="move-panel-header">
            {{ mon.name || mon.name_en }}
          </div>
          <div class="move-grid">
            <button
              v-for="(move, moveIdx) in (mon.moves || [])"
              :key="moveIdx"
              class="move-btn"
              :class="{ 'move-selected': isMoveSelected(slotIdx, moveIdx) }"
              :style="{ '--type-color': getTypeColor(move.type_name || move.name_en) }"
              :disabled="isBusy"
              @click="selectMove(slotIdx, moveIdx, move)"
            >
              <div class="move-name">{{ move.name || move.name_en }}</div>
              <div class="move-info">
                <span class="move-power" v-if="move.power">威力 {{ move.power }}</span>
                <span class="move-pp">PP {{ move.current_pp ?? move.pp }}/{{ move.pp }}</span>
              </div>
            </button>
          </div>

          <!-- 目标选择 -->
          <div v-if="needsTarget(slotIdx)" class="target-select">
            <div class="target-label">{{ t('选择目标:', 'Select target:') }}</div>
            <div class="target-buttons">
              <button
                v-for="(opp, oppIdx) in opponentActiveMons"
                :key="'tgt-' + oppIdx"
                class="target-btn"
                :class="{ 'target-selected': selectedTargets[`target-slot-${slotIdx}`] === opp.fieldSlot }"
                @click="selectTarget(slotIdx, opp.fieldSlot)"
              >
                {{ opp.name || opp.name_en }}
              </button>
            </div>
          </div>

          <!-- 特殊系统 -->
          <div v-if="hasSpecialSystem(mon)" class="special-systems">
            <button
              v-for="sys in availableSpecialSystems(mon)"
              :key="sys"
              class="special-btn"
              :class="{ 'special-selected': selectedSpecialSystems[`special-slot-${slotIdx}`] === sys }"
              @click="toggleSpecialSystem(slotIdx, sys)"
            >
              {{ specialSystemLabel(sys) }}
            </button>
          </div>
        </div>
      </div>

      <!-- 替补选择（替补阶段） -->
      <div v-if="isReplacementPhase" class="sidebar-section">
        <div class="section-title">🔄 {{ t('选择替补', 'Choose Replacement') }}</div>
        <div class="replacement-list">
          <button
            v-for="option in replacementBenchOptions"
            :key="option.value"
            class="replacement-btn"
            :class="{ 'replacement-selected': selectedReplacementIndexes.includes(option.value) }"
            @click="toggleReplacement(option.value)"
          >
            {{ option.label }} (HP: {{ option.hp }}/{{ option.maxHp }})
          </button>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="sidebar-section action-buttons">
        <button
          v-if="canSubmitMove"
          class="action-btn action-primary"
          :disabled="isBusy"
          @click="submitMove"
        >
          {{ isBusy ? t('提交中...', 'Submitting...') : t('提交回合', 'Submit Turn') }}
        </button>
        <button
          v-if="isPreviewPhase"
          class="action-btn action-primary"
          :disabled="isBusy || !canConfirmPreview"
          @click="confirmPreview"
        >
          {{ t('确认预览', 'Confirm Preview') }}
        </button>
        <button
          v-if="isReplacementPhase"
          class="action-btn action-primary"
          :disabled="isBusy || !canConfirmReplacement"
          @click="confirmReplacement"
        >
          {{ t('确认替补', 'Confirm Replacement') }}
        </button>
        <button class="action-btn action-secondary" :disabled="isBusy" @click="refreshStatus">
          🔄 {{ t('刷新', 'Refresh') }}
        </button>
        <button
          v-if="summary?.status === 'running'"
          class="action-btn action-danger"
          :disabled="isBusy"
          @click="forfeitBattle"
        >
          🏳️ {{ t('认输', 'Forfeit') }}
        </button>
      </div>
    </div>

    <!-- 底部：战斗日志 -->
    <div v-if="summary" class="battle3d-logbar">
      <div class="logbar-header" @click="showLog = !showLog">
        📜 {{ t('战斗日志', 'Battle Log') }}
        <span v-if="summary.currentRound">| {{ t('回合', 'Turn') }} {{ summary.currentRound }}</span>
        <span class="logbar-toggle">{{ showLog ? '▼' : '▲' }}</span>
      </div>
      <div v-if="showLog" class="logbar-body" ref="logContainer">
        <div v-for="(round, ri) in (summary.rounds || [])" :key="ri" class="log-round">
          <div class="log-round-header" @click="toggleRound(ri)">
            <span class="log-arrow" :class="{ 'log-arrow-open': expandedRounds.has(Number(ri)) }">▶</span>
            {{ t('第', 'Turn') }} {{ Number(ri) + 1 }} {{ t('回合', '') }}
            <span class="log-event-count">{{ (round.events || []).length }}</span>
          </div>
          <div v-if="expandedRounds.has(Number(ri))" class="log-events">
            <div
              v-for="(event, ei) in (round.events || [])"
              :key="ei"
              class="log-event"
              :class="logEventClass(event)"
            >
              {{ event }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 开始面板（无战斗时） -->
    <div v-if="!summary" class="battle3d-start-panel">
      <div class="start-panel-content">
        <h1 class="start-title">⚔️ {{ t('3D 对战工厂', '3D Battle Factory') }}</h1>
        <p class="start-subtitle">{{ t('全新 3D 对战体验', 'All-new 3D Battle Experience') }}</p>

        <div class="format-selector">
          <button
            v-for="f in formats"
            :key="f.id"
            class="format-btn"
            :class="{ 'format-active': battleFormat === f.id }"
            @click="setBattleFormat(f.id)"
          >
            {{ f.label }}
          </button>
        </div>

        <div class="start-buttons">
          <button class="start-btn start-btn-primary" :disabled="isBusy" @click="startBattle">
            ⚔️ {{ busyAction === 'start-manual' ? t('创建中...', 'Starting...') : t('手动对战', 'Manual Battle') }}
          </button>
          <button
            v-if="isAuthenticated"
            class="start-btn start-btn-purple"
            :disabled="isBusy"
            @click="startFactoryChallenge"
          >
            🏟️ {{ t('工厂挑战', 'Factory Run') }}
          </button>
          <button
            v-if="isAuthenticated"
            class="start-btn start-btn-green"
            :disabled="isBusy"
            @click="startAsyncBattle"
          >
            ⏩ {{ t('异步模拟', 'Async Sim') }}
          </button>
        </div>

        <p v-if="!isAuthenticated" class="start-hint">
          {{ t('游客模式：可直接手动对战', 'Guest mode: manual battle available') }}
        </p>
      </div>
    </div>

    <!-- 结算弹窗 -->
    <SettlementModal
      v-if="settlement"
      :settlement="settlement"
      :factory-run="factoryRun"
      @close="onSettlementClose"
      @continue="nextFactoryBattle"
      @reset="resetBattleState({ keepFactoryRun: false })"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * Battle3D.vue - 3D 对战主组件
 *
 * 整合 Three.js 3D 场景与对战逻辑的主入口组件。
 * 将原有 DOM-based 对战升级为沉浸式 3D 体验。
 *
 * 增强功能：
 * - 自适应性能等级
 * - 音效系统
 * - 移动端触摸支持
 * - 增强光照和雾效
 */
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useThreeSceneEnhanced } from '../composables/battle3d/useThreeSceneEnhanced'
import { useAudioSystem } from '../composables/battle3d/useAudioSystem'
import { useMobileInteraction } from '../composables/battle3d/useMobileInteraction'
import { useBattleEngine } from '../composables/battle3d/useBattleEngine'
import { useBattlePageState } from '../composables/useBattlePageState'
import { useLocale } from '../composables/useLocale'
import { debugLogger } from './battle3d/utils/debug'
import DebugPanel from './battle3d/components/DebugPanel.vue'
import SettlementModal from './battle3d/components/SettlementModal.vue'
import type { PerformanceLevel } from '../composables/battle3d/useThreeSceneEnhanced'

// ===== 国际化 =====
const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string, params?: any) => tr(zh, en, '', params || {})

// ===== DOM 引用 =====
const canvasContainer = ref<HTMLElement | null>(null)
const logContainer = ref<HTMLElement | null>(null)

// ===== UI 状态 =====
const showDebugPanel = ref(false)
const showLog = ref(false)
const expandedRounds = ref(new Set<number>())

// ===== 格式选项 =====
const formats = [
  { id: 'vgc-doubles', label: t('双打 (64)', 'Doubles (64)') },
  { id: 'vgc63', label: t('63 单打', 'Singles (63)') },
  { id: 'gen9singles', label: t('9代单打', 'Gen9 Singles') }
]

// ===== 战斗状态管理 =====
const battleState = useBattlePageState() as any
const {
  summary, battleFormat, busyAction, isBusy, isAuthenticated,
  factoryRun, settlement,
  startBattle, startAsyncBattle, startFactoryChallenge,
  setBattleFormat, confirmPreview, confirmReplacement,
  submitMove, refreshStatus, forfeitBattle,
  resetBattleState, nextFactoryBattle, onSettlementClose,
  playerActiveMons, opponentActiveMons, replacementBenchOptions,
  selectedActions, selectedMoves, selectedTargets: rawSelectedTargets, selectedSpecialSystems: rawSelectedSpecialSystems,
  selectedReplacementIndexes, setSelectedAction, setSelectedMove,
  setSelectedTarget, setSelectedSpecialSystem, toggleReplacement,
  canSubmitMove, canConfirmPreview, canConfirmReplacement,
  isPreviewPhase, isReplacementPhase,
  openLeaderboard, abandonFactoryRun, prepareNextFactoryStage,
  availableSpecialSystems: getAvailableSpecialSystems
} = battleState

// 类型安全的目标和特殊系统选择
const selectedTargets = rawSelectedTargets as Record<string, any>
const selectedSpecialSystems = rawSelectedSpecialSystems as Record<string, any>

// ===== 增强版 Three.js 场景 =====
const {
  scene, camera, renderer, controls, fps, performanceStats,
  addToScene, removeFromScene, startRenderLoop, stopRenderLoop, dispose: disposeScene,
  isReady: sceneReady, setPerformanceLevel, getPerformanceLevel
} = useThreeSceneEnhanced(canvasContainer, {
  enableShadows: true,
  fov: 60,
  enableFog: true,
  enableSkybox: true,
  performanceLevel: 'high'
})

// ===== 音效系统 =====
const {
  isInitialized: audioReady, isMuted, masterVolume,
  init: initAudio, playSound, playAttackSound,
  setMasterVolume, toggleMute
} = useAudioSystem()

// ===== 移动端交互 =====
const {
  isMobile, hasTouch, orientation,
  triggerHaptic, onGesture, requestFullscreen
} = useMobileInteraction()

// ===== 加载状态 =====
const isLoading = ref(true)
const loadingProgress = ref(0)
const loadingMessage = ref(t('正在初始化...', 'Initializing...'))

// ===== 3D 战场实例 =====
import { Battlefield } from './battle3d/core/BattleField'
const battlefield = ref<Battlefield | null>(null)

// ===== 战斗引擎 =====
const {
  currentPhase, isEngineReady, entities,
  initEngine, dispose: disposeEngine,
  playAttackAnimation, playHealAnimation, playTerastallizeAnimation,
  updateEffects
} = useBattleEngine({ scene, battlefield }, summary)

// ===== 调试日志 =====
const debugLogs = computed(() => debugLogger.getRecentLogs(50))
const debugStats = computed(() => ({
  ...debugLogger.getStats(),
  fps: fps.value,
  drawCalls: performanceStats.value.drawCalls,
  triangles: performanceStats.value.triangles,
  memoryUsage: performanceStats.value.memoryUsage,
  performanceLevel: getPerformanceLevel(),
  audioReady: audioReady.value,
  isMobile: isMobile.value,
  orientation: orientation.value
}))

// ===== 状态文本 =====
const statusText = computed(() => {
  if (!summary.value) return t('未开始', 'Not started')
  if (isPreviewPhase.value) return t('队伍预览中', 'Team preview')
  if (isReplacementPhase.value) return t('补位选择中', 'Choosing replacements')
  if (summary.value.status === 'completed') {
    return summary.value.winner === 'player'
      ? t('已结束 · 玩家胜利', 'Finished · Player won')
      : t('已结束 · 对手胜利', 'Finished · Opponent won')
  }
  return t('进行中 · 第 {round} 回合', 'In progress · Turn {round}', { round: summary.value.currentRound || 0 })
})

const statusClass = computed(() => {
  if (!summary.value) return ''
  if (summary.value.status === 'completed') {
    return summary.value.winner === 'player' ? 'status-win' : 'status-lose'
  }
  if (isPreviewPhase.value) return 'status-preview'
  if (isReplacementPhase.value) return 'status-replacement'
  return 'status-running'
})

const isActionPhase = computed(() => {
  return summary.value?.status === 'running'
    && summary.value?.phase !== 'team-preview'
    && summary.value?.phase !== 'replacement'
})

// ===== 辅助函数 =====
function hpPercent(mon: any): number {
  const maxHp = mon?.stats?.hp || mon?.currentHp || 1
  return Math.max(0, Math.min(100, ((mon?.currentHp || 0) / maxHp) * 100))
}

function hpColor(mon: any): string {
  const pct = hpPercent(mon)
  if (pct <= 0) return '#999'
  if (pct <= 20) return '#ef4444'
  if (pct <= 50) return '#fbbf24'
  return '#4ade80'
}

function getTypeColor(typeName: string): string {
  const colorMap: Record<string, string> = {
    Normal: '#A8A77A', Fire: '#EE8130', Water: '#6390F0', Electric: '#F7D02C',
    Grass: '#7AC74C', Ice: '#96D9D6', Fighting: '#C22E28', Poison: '#A33EA1',
    Ground: '#E2BF65', Flying: '#A98FF3', Psychic: '#F95587', Bug: '#A6B91A',
    Rock: '#B6A136', Ghost: '#735797', Dragon: '#6F35FC', Dark: '#705746',
    Steel: '#B7B7CE', Fairy: '#D685AD'
  }
  return colorMap[typeName] || '#A8A77A'
}

function statusLabel(condition: string): string {
  const labels: Record<string, string> = {
    paralysis: 'PAR', burn: 'BRN', freeze: 'FRZ', sleep: 'SLP',
    poison: 'PSN', toxic: 'TOX', confusion: 'CNF'
  }
  return labels[condition] || condition
}

function specialSystemLabel(sys: string): string {
  const m: Record<string, string> = {
    tera: t('太晶化', 'Terastallize'),
    mega: t('Mega进化', 'Mega Evolution'),
    'z-move': t('Z招式', 'Z-Move'),
    dynamax: t('极巨化', 'Dynamax')
  }
  return m[sys] || sys
}

function hasSpecialSystem(mon: any): boolean {
  return (mon?.specialSystems || []).length > 0
}

function availableSpecialSystems(mon: any): string[] {
  return getAvailableSpecialSystems ? getAvailableSpecialSystems(mon) : []
}

// ===== 场地效果 =====
const fieldChips = computed(() => {
  const fe = summary.value?.fieldEffects || {}
  const chips: Array<{ l: string; cls: string }> = []
  const p = (l: string, k: string, cls: string) => {
    const v = Number(fe[k] || 0)
    if (v > 0) chips.push({ l: v > 1 ? `${l} ${v}T` : l, cls })
  }
  p('TW', 'playerTailwindTurns', 'chip-blue'); p('TW', 'opponentTailwindTurns', 'chip-red')
  p('TR', 'trickRoomTurns', 'chip-purple'); p('Rain', 'rainTurns', 'chip-cyan')
  p('Sun', 'sunTurns', 'chip-amber'); p('Sand', 'sandTurns', 'chip-orange')
  return chips
})

// ===== 招式选择逻辑 =====
const selectedMoveIndexes = ref<Record<number, number>>({})
const selectedMoveObjects = ref<Record<number, any>>({})

function isMoveSelected(slotIdx: number | string, moveIdx: number | string): boolean {
  return selectedMoveIndexes.value[Number(slotIdx)] === Number(moveIdx)
}

function selectMove(slotIdx: number | string, moveIdx: number | string, move: any) {
  const slot = Number(slotIdx)
  const mIdx = Number(moveIdx)
  selectedMoveIndexes.value[slot] = mIdx
  selectedMoveObjects.value[slot] = move
  setSelectedMove(`slot-${slot}`, move.name_en || move.name)

  // 如果招式不需要选择目标，直接设置行动类型
  const targetId = Number(move.target_id || 10)
  if (targetId !== 10) {
    setSelectedAction(`action-slot-${slot}`, 'move')
  }

  debugLogger.log('debug', 'interaction', `选择招式: ${move.name || move.name_en} for slot ${slot}`)
}

function needsTarget(slotIdx: number | string): boolean {
  const move = selectedMoveObjects.value[Number(slotIdx)]
  if (!move) return false
  return Number(move.target_id || 10) === 10
}

function selectTarget(slotIdx: number | string, targetSlot: number | string) {
  const slot = Number(slotIdx)
  const target = Number(targetSlot)
  setSelectedTarget(`target-slot-${slot}`, target)
  setSelectedAction(`action-slot-${slot}`, 'move')
  debugLogger.log('debug', 'interaction', `选择目标: slot ${target} for player slot ${slot}`)
}

function toggleSpecialSystem(slotIdx: number | string, sys: string) {
  const slot = Number(slotIdx)
  const current = selectedSpecialSystems.value[`special-slot-${slot}`]
  if (current === sys) {
    setSelectedSpecialSystem(`special-slot-${slot}`, null)
  } else {
    setSelectedSpecialSystem(`special-slot-${slot}`, sys)
  }
}

// ===== 相机模式 =====
let cameraMode = 0
function toggleCameraMode() {
  cameraMode = (cameraMode + 1) % 3
  if (!camera.value) return

  const cam = camera.value
  switch (cameraMode) {
    case 0: // 默认视角
      cam.position.set(0, 12, 14)
      break
    case 1: // 俯视
      cam.position.set(0, 20, 0.1)
      break
    case 2: // 侧视
      cam.position.set(18, 8, 0)
      break
  }
  cam.lookAt(0, 0, 0)
  controls.value?.update()

  debugLogger.log('info', 'scene', `相机模式切换: ${cameraMode}`)
}

// ===== 日志辅助 =====
function toggleRound(index: number | string) {
  const numIndex = Number(index)
  const s = new Set(expandedRounds.value)
  s.has(numIndex) ? s.delete(numIndex) : s.add(numIndex)
  expandedRounds.value = s
}

function logEventClass(event: string): string {
  if (/收回|派出|换人/.test(event)) return 'log-evt-switch'
  if (/造成了.*点伤害|伤害/.test(event)) return 'log-evt-damage'
  if (/回复|恢复|治愈/.test(event)) return 'log-evt-heal'
  if (/下降|降低/.test(event)) return 'log-evt-debuff'
  return ''
}

// ===== 渲染循环集成 =====
let animationFrameId: number | null = null
let lastTime = 0

function gameLoop(time: number) {
  const delta = (time - lastTime) / 1000
  lastTime = time

  // 更新特效
  updateEffects(delta)

  // 更新调试统计
  debugLogger.updateStats('activeObjects', Object.keys(entities).length)
  debugLogger.updateStats('activeEffects', currentPhase.value ? 1 : 0)

  animationFrameId = requestAnimationFrame(gameLoop)
}

// ===== 音效触发 =====
function playAttackSfx(moveType?: string) {
  if (!audioReady.value) return
  playAttackSound(moveType || 'normal')
  triggerHaptic('medium')
}

function playHealSfx() {
  if (!audioReady.value) return
  playSound('heal')
  triggerHaptic('light')
}

function playFaintSfx() {
  if (!audioReady.value) return
  playSound('faint')
  triggerHaptic('heavy')
}

function playVictorySfx() {
  if (!audioReady.value) return
  playSound('victory')
  triggerHaptic('success')
}

function playDefeatSfx() {
  if (!audioReady.value) return
  playSound('defeat')
  triggerHaptic('warning')
}

// ===== 移动端手势 =====
function setupMobileGestures() {
  if (!hasTouch.value) return

  onGesture('battle3d', (gesture, data) => {
    switch (gesture) {
      case 'double_tap':
        // 双击提交回合
        if (canSubmitMove.value) {
          submitMove()
        }
        break
      case 'swipe_up':
        // 上滑显示日志
        showLog.value = !showLog.value
        break
      case 'swipe_down':
        // 下滑隐藏日志
        showLog.value = false
        break
      case 'swipe_left':
        // 左滑切换招式
        break
      case 'swipe_right':
        // 右滑切换招式
        break
      case 'long_press':
        // 长按显示详情
        break
    }
  })
}

// ===== 性能等级切换 =====
const performanceLevels: PerformanceLevel[] = ['low', 'medium', 'high', 'ultra']
const currentPerfLevelIndex = ref(2) // Default: high

function cyclePerformanceLevel() {
  currentPerfLevelIndex.value = (currentPerfLevelIndex.value + 1) % performanceLevels.length
  const level = performanceLevels[currentPerfLevelIndex.value]
  setPerformanceLevel(level)
  debugLogger.log('info', 'performance', `性能等级切换: ${level}`)
}

// ===== 生命周期 =====
onMounted(async () => {
  debugLogger.log('info', 'scene', 'Battle3D 组件挂载')

  // 更新加载状态
  loadingMessage.value = t('正在初始化 3D 场景...', 'Initializing 3D scene...')
  loadingProgress.value = 20

  // 等待场景就绪
  await nextTick()

  // 初始化 3D 场景
  if (scene.value) {
    // 创建战场
    loadingMessage.value = t('正在创建战场...', 'Creating battlefield...')
    loadingProgress.value = 40
    battlefield.value = new Battlefield(scene.value)

    // 初始化战斗引擎
    loadingMessage.value = t('正在初始化战斗引擎...', 'Initializing battle engine...')
    loadingProgress.value = 60
    initEngine()

    // 初始化音效系统
    loadingMessage.value = t('正在加载音效...', 'Loading audio...')
    loadingProgress.value = 80
    try {
      await initAudio()
    } catch (e) {
      debugLogger.log('warn', 'audio', '音效初始化失败（用户未交互）')
    }

    // 设置移动端手势
    setupMobileGestures()

    // 启动渲染循环
    loadingMessage.value = t('正在启动渲染...', 'Starting renderer...')
    loadingProgress.value = 90
    startRenderLoop()

    // 启动游戏循环
    lastTime = performance.now()
    animationFrameId = requestAnimationFrame(gameLoop)

    // 加载完成
    loadingProgress.value = 100
    setTimeout(() => {
      isLoading.value = false
    }, 500)

    debugLogger.log('info', 'scene', '3D 场景初始化完成')
  }
})

onBeforeUnmount(() => {
  debugLogger.log('info', 'scene', 'Battle3D 组件卸载')

  // 停止游戏循环
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }

  // 清理引擎
  disposeEngine()

  // 清理战场
  if (battlefield.value) {
    battlefield.value.dispose()
  }

  // 清理场景
  disposeScene()
})

// ===== 监听回合计数变化触发动画 =====
watch(() => summary.value?.rounds?.length, (newLen, oldLen) => {
  if (!newLen || newLen <= (oldLen || 0)) return

  const latest = summary.value.rounds[newLen - 1]
  if (!latest?.events) return

  const text = latest.events.join(' ')

  // 检测伤害事件播放攻击动画
  if (/伤害|damage|造成了/.test(text)) {
    const oppKeys = Object.keys(entities).filter(k => k.startsWith('opponent'))
    const plrKeys = Object.keys(entities).filter(k => k.startsWith('player'))

    if (oppKeys.length && plrKeys.length) {
      const attacker = plrKeys[Math.floor(Math.random() * plrKeys.length)]
      const target = oppKeys[Math.floor(Math.random() * oppKeys.length)]
      playAttackAnimation(attacker, target)
    }
  }

  // 检测治愈事件
  if (/回复|恢复|治愈|heal/.test(text)) {
    const plrKeys = Object.keys(entities).filter(k => k.startsWith('player'))
    if (plrKeys.length) {
      playHealAnimation(plrKeys[0])
    }
  }

  // 自动展开最新回合
  expandedRounds.value.add(newLen - 1)
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
})

// ===== 监听场景就绪 =====
watch(sceneReady, (ready) => {
  if (ready && scene.value && !battlefield.value) {
    battlefield.value = new Battlefield(scene.value)
    initEngine()
    startRenderLoop()
  }
})
</script>

<style scoped>
/* ===== 容器 ===== */
.battle3d-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #1a1a2e;
}

.battle3d-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

/* ===== 顶部状态栏 ===== */
.battle3d-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  background: linear-gradient(180deg, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0) 100%);
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-title {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.topbar-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.status-win { background: #4ade80; color: #000; }
.status-lose { background: #ef4444; color: #fff; }
.status-running { background: #60a5fa; color: #fff; }
.status-preview { background: #fbbf24; color: #000; }
.status-replacement { background: #f97316; color: #fff; }

.topbar-right {
  display: flex;
  gap: 8px;
}

.topbar-btn {
  padding: 4px 12px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 6px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.topbar-btn:hover {
  background: rgba(255,255,255,0.2);
}

/* ===== 侧边栏 ===== */
.battle3d-sidebar {
  position: absolute;
  top: 56px;
  width: 280px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.3) transparent;
}

.battle3d-sidebar-left {
  left: 8px;
}

.battle3d-sidebar-right {
  right: 8px;
}

.sidebar-section {
  background: rgba(0,0,0,0.75);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px;
  padding: 12px;
}

.section-title {
  font-size: 13px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255,255,255,0.15);
}

/* ===== 宝可梦信息卡片 ===== */
.pokemon-info-card {
  padding: 8px;
  border-radius: 8px;
  margin-bottom: 6px;
}

.pokemon-info-card.opponent {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.pokemon-info-card.player {
  background: rgba(74, 222, 128, 0.15);
  border: 1px solid rgba(74, 222, 128, 0.3);
}

.pokemon-name {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.pokemon-level {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
}

.hp-bar-container {
  height: 8px;
  background: rgba(0,0,0,0.5);
  border-radius: 4px;
  overflow: hidden;
  margin: 4px 0;
}

.hp-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.hp-text {
  font-size: 11px;
  color: rgba(255,255,255,0.7);
}

.pokemon-types {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  color: #fff;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}

.status-badges {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.status-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: bold;
}

.status-PAR { background: #a16207; color: #fff; }
.status-BRN { background: #c2410c; color: #fff; }
.status-FRZ { background: #0369a1; color: #fff; }
.status-SLP { background: #6d28d9; color: #fff; }
.status-PSN, .status-TOX { background: #7e22ce; color: #fff; }
.status-tera { background: #6366f1; color: #fff; }

/* ===== 场地效果 ===== */
.field-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.field-chip {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
}

.chip-blue { background: #ddeeff; color: #246; }
.chip-red { background: #ffe0e0; color: #822; }
.chip-purple { background: #e8ddff; color: #446; }
.chip-cyan { background: #ddffff; color: #145; }
.chip-amber { background: #fff3dd; color: #653; }
.chip-orange { background: #ffe8dd; color: #642; }

/* ===== 招式面板 ===== */
.move-panel {
  margin-bottom: 10px;
}

.move-panel-header {
  font-size: 12px;
  font-weight: bold;
  color: rgba(255,255,255,0.8);
  margin-bottom: 6px;
}

.move-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.move-btn {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 8px;
  border: 2px solid rgba(255,255,255,0.15);
  border-radius: 6px;
  background: var(--type-color, #888);
  color: #fff;
  cursor: pointer;
  text-align: left;
  font-size: 11px;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  transition: all 0.15s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2), inset 0 1px 0 rgba(255,255,255,0.15);
}

.move-btn:hover:not(:disabled) {
  filter: brightness(1.15);
  transform: translateY(-1px);
}

.move-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.move-selected {
  border-color: #fbbf24 !important;
  box-shadow: 0 0 12px rgba(212,160,23,0.5), 0 2px 4px rgba(0,0,0,0.2);
  transform: scale(1.02);
}

.move-name {
  font-weight: bold;
  font-size: 12px;
}

.move-info {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  opacity: 0.9;
}

/* ===== 目标选择 ===== */
.target-select {
  margin-top: 6px;
  padding: 6px;
  background: rgba(0,0,0,0.3);
  border-radius: 6px;
}

.target-label {
  font-size: 11px;
  color: rgba(255,255,255,0.7);
  margin-bottom: 4px;
}

.target-buttons {
  display: flex;
  gap: 4px;
}

.target-btn {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
}

.target-btn:hover {
  background: rgba(255,255,255,0.2);
}

.target-selected {
  background: rgba(239, 68, 68, 0.4) !important;
  border-color: #ef4444 !important;
}

/* ===== 特殊系统 ===== */
.special-systems {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.special-btn {
  padding: 3px 8px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.special-btn:hover {
  background: rgba(255,255,255,0.2);
}

.special-selected {
  background: rgba(99, 102, 241, 0.5) !important;
  border-color: #6366f1 !important;
}

/* ===== 替补选择 ===== */
.replacement-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.replacement-btn {
  padding: 6px 10px;
  border: 2px solid rgba(255,255,255,0.15);
  border-radius: 6px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}

.replacement-btn:hover {
  background: rgba(255,255,255,0.1);
}

.replacement-selected {
  background: rgba(74, 222, 128, 0.3) !important;
  border-color: #4ade80 !important;
}

/* ===== 操作按钮 ===== */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.action-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.5);
}

.action-secondary {
  background: rgba(255,255,255,0.15);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.2);
}

.action-secondary:hover:not(:disabled) {
  background: rgba(255,255,255,0.25);
}

.action-danger {
  background: rgba(239, 68, 68, 0.3);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.4);
}

.action-danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.5);
}

/* ===== 底部日志栏 ===== */
.battle3d-logbar {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(640px, calc(100% - 600px));
  max-height: 200px;
  background: rgba(0,0,0,0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  z-index: 10;
  overflow: hidden;
}

.logbar-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: bold;
  color: #fff;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logbar-toggle {
  font-size: 10px;
}

.logbar-body {
  max-height: 150px;
  overflow-y: auto;
  padding: 4px 8px;
}

.log-round {
  margin-bottom: 4px;
}

.log-round-header {
  font-size: 11px;
  font-weight: bold;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  padding: 3px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.log-arrow {
  font-size: 8px;
  transition: transform 0.2s;
  color: rgba(255,255,255,0.5);
}

.log-arrow-open {
  transform: rotate(90deg);
}

.log-event-count {
  font-size: 9px;
  background: rgba(255,255,255,0.2);
  padding: 0 4px;
  border-radius: 3px;
}

.log-events {
  padding: 2px 0 4px 18px;
}

.log-event {
  font-size: 11px;
  color: rgba(255,255,255,0.8);
  padding: 2px 0;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.log-evt-switch { color: #60a5fa; font-weight: bold; }
.log-evt-damage { color: #f87171; }
.log-evt-heal { color: #4ade80; }
.log-evt-debuff { color: #fbbf24; }

/* ===== 开始面板 ===== */
.battle3d-start-panel {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20;
  background: radial-gradient(ellipse at center, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.7) 100%);
}

.start-panel-content {
  text-align: center;
  padding: 40px;
  background: rgba(0,0,0,0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
  max-width: 500px;
  width: 90%;
}

.start-title {
  font-size: 32px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8px;
  text-shadow: 0 4px 8px rgba(0,0,0,0.5);
}

.start-subtitle {
  font-size: 14px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 24px;
}

.format-selector {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  justify-content: center;
}

.format-btn {
  padding: 8px 20px;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.7);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.format-btn:first-child { border-radius: 8px 0 0 8px; }
.format-btn:last-child { border-radius: 0 8px 8px 0; }

.format-btn:hover {
  background: rgba(255,255,255,0.1);
}

.format-active {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: #fff !important;
  border-color: #3b82f6 !important;
}

.start-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.start-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}

.start-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.start-btn-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.start-btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.5);
}

.start-btn-purple {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: #fff;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.4);
}

.start-btn-green {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.4);
}

.start-hint {
  margin-top: 16px;
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

/* ===== 加载屏幕 ===== */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #0a0a1a 0%, #1a1a3e 50%, #0a0a2a 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.loading-content {
  text-align: center;
  padding: 40px;
}

.loading-icon {
  font-size: 80px;
  margin-bottom: 24px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.loading-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 24px;
}

.loading-bar {
  width: 300px;
  height: 8px;
  background: rgba(255,255,255,0.1);
  border-radius: 4px;
  overflow: hidden;
  margin: 0 auto 16px;
}

.loading-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.loading-message {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  margin-bottom: 12px;
}

.loading-tip {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
}

/* ===== 移动端标识 ===== */
.topbar-mobile-badge {
  font-size: 12px;
  margin-left: 8px;
}

/* ===== 性能按钮 ===== */
.perf-btn {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .battle3d-sidebar {
    width: 220px;
    font-size: 11px;
  }

  .battle3d-logbar {
    width: calc(100% - 460px);
  }
}

@media (max-width: 700px) {
  .battle3d-sidebar {
    display: none;
  }

  .battle3d-logbar {
    width: calc(100% - 16px);
    left: 8px;
    transform: none;
  }
}
</style>
