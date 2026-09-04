<template>
  <div class="battle3d-container">
    <!-- 左侧：3D 场景 -->
    <div class="battle3d-scene-area">
      <div ref="canvasContainer" class="battle3d-canvas" />
      
      <!-- 加载屏幕 -->
      <div v-if="isLoading" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-icon">⚔️</div>
          <h2 class="loading-title">{{ t('正在加载', 'Loading') }}...</h2>
          <div class="loading-bar">
            <div class="loading-fill" :style="{ width: loadingProgress + '%' }" />
          </div>
          <p class="loading-message">{{ loadingMessage }}</p>
        </div>
      </div>

      <!-- 场景内浮动信息 -->
      <div v-if="summary" class="scene-float-info">
        <div class="float-status" :class="statusClass">{{ statusText }}</div>
        <div v-if="summary.currentRound" class="float-round">{{ t('回合', 'Turn') }} {{ summary.currentRound }}</div>
      </div>

      <!-- 场景控制按钮 -->
      <div class="scene-controls">
        <button class="scene-btn" @click="toggleCameraMode" title="切换视角">📷</button>
        <button class="scene-btn" @click="cyclePerformanceLevel" title="性能等级">⚡</button>
        <button class="scene-btn" @click="showDebugPanel = !showDebugPanel" title="调试">🐛</button>
      </div>
    </div>

    <!-- 右侧：控制面板 -->
    <div class="battle3d-panel">
      <!-- 面板头部 -->
      <div class="panel-header">
        <span class="panel-title">⚔️ {{ t('对战控制', 'Battle Control') }}</span>
        <div class="panel-actions">
          <button class="icon-btn" @click="toggleMute">{{ isMuted ? '🔇' : '🔊' }}</button>
        </div>
      </div>

      <!-- 无战斗时：开始面板 -->
      <div v-if="!summary" class="panel-content start-panel">
        <div class="start-section">
          <h3>{{ t('选择模式', 'Select Mode') }}</h3>
          <div class="format-row">
            <button
              v-for="f in formats"
              :key="f.id"
              class="format-btn"
              :class="{ active: battleFormat === f.id }"
              @click="setBattleFormat(f.id)"
            >
              {{ f.label }}
            </button>
          </div>
        </div>
        <div class="start-actions">
          <button class="btn btn-primary btn-large" :disabled="isBusy" @click="startBattle">
            ⚔️ {{ busyAction === 'start-manual' ? t('创建中...', 'Starting...') : t('手动对战', 'Manual') }}
          </button>
          <button v-if="isAuthenticated" class="btn btn-purple" :disabled="isBusy" @click="startFactoryChallenge">
            🏟️ {{ t('工厂挑战', 'Factory') }}
          </button>
          <button v-if="isAuthenticated" class="btn btn-green" :disabled="isBusy" @click="startAsyncBattle">
            ⏩ {{ t('异步模拟', 'Async') }}
          </button>
        </div>
        <p v-if="!isAuthenticated" class="hint">{{ t('游客模式可用', 'Guest mode available') }}</p>
      </div>

      <!-- 有战斗时：信息和操作 -->
      <div v-if="summary" class="panel-content battle-panel">
        <!-- 双方信息（紧凑并排） -->
        <div class="teams-row">
          <!-- 对手 -->
          <div class="team-card opponent">
            <div class="team-label">🔴 {{ t('对手', 'Opponent') }}</div>
            <div v-for="(mon, i) in opponentActiveMons" :key="'o'+i" class="mon-mini">
              <span class="mon-name">{{ mon.name || mon.name_en }}</span>
              <div class="mini-hp">
                <div class="mini-hp-bar" :style="{ width: hpPercent(mon)+'%', background: hpColor(mon) }" />
              </div>
              <span class="mon-hp-num">{{ mon.currentHp }}</span>
            </div>
          </div>
          <!-- 我方 -->
          <div class="team-card player">
            <div class="team-label">🟢 {{ t('我方', 'Player') }}</div>
            <div v-for="(mon, i) in playerActiveMons" :key="'p'+i" class="mon-mini">
              <span class="mon-name">{{ mon.name || mon.name_en }}</span>
              <div class="mini-hp">
                <div class="mini-hp-bar" :style="{ width: hpPercent(mon)+'%', background: hpColor(mon) }" />
              </div>
              <span class="mon-hp-num">{{ mon.currentHp }}</span>
            </div>
          </div>
        </div>

        <!-- 场地效果 -->
        <div v-if="fieldChips.length" class="field-row">
          <span v-for="(chip, i) in fieldChips" :key="i" class="chip" :class="chip.cls">{{ chip.l }}</span>
        </div>

        <!-- 招式选择 -->
        <div v-if="isActionPhase" class="moves-section">
          <div class="section-label">⚔️ {{ t('选择招式', 'Select Move') }}</div>
          <div v-for="(mon, slotIdx) in playerActiveMons" :key="'m'+slotIdx" class="move-block">
            <div class="move-block-name">{{ mon.name || mon.name_en }}</div>
            <div class="move-grid">
              <button
                v-for="(move, moveIdx) in (mon.moves || [])"
                :key="moveIdx"
                class="move-btn"
                :class="{ selected: isMoveSelected(slotIdx, moveIdx) }"
                :style="{ '--tc': getTypeColor(move.type_name || move.name_en) }"
                :disabled="isBusy"
                @click="selectMove(slotIdx, moveIdx, move)"
              >
                <span class="mv-name">{{ move.name || move.name_en }}</span>
                <span class="mv-type-badge" :style="{ background: getTypeColor(move.type_name || move.name_en) }">
                  {{ move.type_name || '?' }}
                </span>
                <span class="mv-info">
                  <span v-if="move.power">威力 {{ move.power }}</span>
                  <span>PP {{ move.current_pp ?? move.pp }}/{{ move.pp }}</span>
                </span>
              </button>
            </div>
            <!-- 目标选择 -->
            <div v-if="needsTarget(slotIdx)" class="target-row">
              <span class="target-label">🎯</span>
              <button
                v-for="(opp, oi) in opponentActiveMons"
                :key="oi"
                class="target-btn"
                :class="{ active: selectedTargets[`target-slot-${slotIdx}`] === opp.fieldSlot }"
                @click="selectTarget(slotIdx, opp.fieldSlot)"
              >
                {{ opp.name || opp.name_en }}
              </button>
            </div>
            <!-- 特殊系统 -->
            <div v-if="hasSpecialSystem(mon)" class="special-row">
              <button
                v-for="sys in availableSpecialSystems(mon)"
                :key="sys"
                class="special-btn"
                :class="{ active: selectedSpecialSystems[`special-slot-${slotIdx}`] === sys }"
                @click="toggleSpecialSystem(slotIdx, sys)"
              >
                {{ specialSystemLabel(sys) }}
              </button>
            </div>
          </div>
        </div>

        <!-- 替补选择 -->
        <div v-if="isReplacementPhase" class="replacement-section">
          <div class="section-label">🔄 {{ t('选择替补', 'Replacement') }}</div>
          <button
            v-for="option in replacementBenchOptions"
            :key="option.value"
            class="replace-btn"
            :class="{ active: selectedReplacementIndexes.includes(option.value) }"
            @click="toggleReplacement(option.value)"
          >
            {{ option.label }} ({{ option.hp }}/{{ option.maxHp }})
          </button>
        </div>

        <!-- 操作按钮 -->
        <div class="action-row">
          <button v-if="canSubmitMove" class="btn btn-primary" :disabled="isBusy" @click="submitMove">
            {{ isBusy ? '...' : t('提交回合', 'Submit') }}
          </button>
          <button v-if="isPreviewPhase" class="btn btn-primary" :disabled="isBusy || !canConfirmPreview" @click="confirmPreview">
            {{ t('确认预览', 'Confirm') }}
          </button>
          <button v-if="isReplacementPhase" class="btn btn-primary" :disabled="isBusy || !canConfirmReplacement" @click="confirmReplacement">
            {{ t('确认替补', 'Confirm') }}
          </button>
          <button class="btn btn-secondary" :disabled="isBusy" @click="refreshStatus">🔄</button>
          <button v-if="summary?.status === 'running'" class="btn btn-danger" :disabled="isBusy" @click="forfeitBattle">🏳️</button>
        </div>

        <!-- 战斗日志（右侧底部） -->
        <div class="log-section">
          <div class="log-header" @click="showLog = !showLog">
            📜 {{ t('日志', 'Log') }}
            <span class="log-toggle">{{ showLog ? '▼' : '▲' }}</span>
          </div>
          <div v-if="showLog" class="log-body" ref="logContainer">
            <div v-for="(round, ri) in (summary.rounds || [])" :key="ri" class="log-round">
              <div class="log-round-h" @click="toggleRound(ri)">
                <span class="log-arrow" :class="{ open: expandedRounds.has(Number(ri)) }">▶</span>
                {{ t('第', 'T') }}{{ Number(ri)+1 }}{{ t('回合', '') }}
                <span class="log-count">{{ (round.events || []).length }}</span>
              </div>
              <div v-if="expandedRounds.has(Number(ri))" class="log-events">
                <div v-for="(evt, ei) in (round.events || [])" :key="ei" class="log-evt" :class="logEventClass(evt)">
                  {{ evt }}
                </div>
              </div>
            </div>
          </div>
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
import DebugPanel from './battle3d/components/DebugPanel.vue'
import SettlementModal from './battle3d/components/SettlementModal.vue'
import { Battlefield } from './battle3d/core/BattleField'
import type { PerformanceLevel } from '../composables/battle3d/useThreeSceneEnhanced'

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

// ===== 格式 =====
const formats = [
  { id: 'vgc-doubles', label: t('双打64', 'Doubles') },
  { id: 'vgc63', label: t('单打63', 'Singles') },
  { id: 'gen9singles', label: t('9代单打', 'Gen9') }
]

// ===== 战斗状态 =====
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
    return summary.value.winner === 'player' ? t('胜利!', 'Win!') : t('失败', 'Lose')
  }
  return t('第{r}回合', 'Turn {r}', { r: summary.value.currentRound || 0 })
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
  const chips: Array<{ l: string; cls: string }> = []
  const p = (l: string, k: string, cls: string) => {
    const v = Number(fe[k] || 0)
    if (v > 0) chips.push({ l: v > 1 ? `${l}${v}T` : l, cls })
  }
  p('TW', 'playerTailwindTurns', 'c-blue'); p('TW', 'opponentTailwindTurns', 'c-red')
  p('TR', 'trickRoomTurns', 'c-purple'); p('Rain', 'rainTurns', 'c-cyan')
  p('Sun', 'sunTurns', 'c-amber'); p('Sand', 'sandTurns', 'c-orange')
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

function getTypeColor(typeName: string): string {
  const m: Record<string, string> = {
    Normal: '#A8A77A', Fire: '#EE8130', Water: '#6390F0', Electric: '#F7D02C',
    Grass: '#7AC74C', Ice: '#96D9D6', Fighting: '#C22E28', Poison: '#A33EA1',
    Ground: '#E2BF65', Flying: '#A98FF3', Psychic: '#F95587', Bug: '#A6B91A',
    Rock: '#B6A136', Ghost: '#735797', Dragon: '#6F35FC', Dark: '#705746',
    Steel: '#B7B7CE', Fairy: '#D685AD'
  }
  return m[typeName] || '#A8A77A'
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

function isMoveSelected(s: number | string, m: number | string) { return selectedMoveIndexes.value[Number(s)] === Number(m) }

function selectMove(s: number | string, m: number | string, move: any) {
  const slot = Number(s)
  selectedMoveIndexes.value[slot] = Number(m)
  selectedMoveObjects.value[slot] = move
  setSelectedMove(`slot-${slot}`, move.name_en || move.name)
  if (Number(move.target_id || 10) !== 10) setSelectedAction(`action-slot-${slot}`, 'move')
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

// ===== 生命周期 =====
onMounted(() => {
  // 快速初始化 - 不阻塞
  loadingMessage.value = t('初始化场景...', 'Init scene...')
  loadingProgress.value = 30

  // 初始化 Three.js 场景
  initScene()
  loadingProgress.value = 50

  if (scene.value) {
    // 创建战场
    battlefield.value = new Battlefield(scene.value)
    loadingProgress.value = 70

    // 初始化战斗引擎
    initEngine()
    loadingProgress.value = 85

    // 启动渲染循环
    startRenderLoop()
    lastTime = performance.now()
    animId = requestAnimationFrame(gameLoop)
    loadingProgress.value = 100

    // 音频延迟初始化（非阻塞）
    initAudio().catch(() => { /* 用户未交互时正常 */ })

    // 立即隐藏加载屏幕
    isLoading.value = false
  }
})

onBeforeUnmount(() => {
  if (animId !== null) cancelAnimationFrame(animId)
  disposeEngine()
  if (battlefield.value) battlefield.value.dispose()
  disposeScene()
})

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
/* ===== 全局容器：左右布局 ===== */
.battle3d-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #0a0a1a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
  z-index: 10;
}

.float-status {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: bold;
  backdrop-filter: blur(8px);
}

.float-status.win { background: rgba(74,222,128,0.8); color: #000; }
.float-status.lose { background: rgba(239,68,68,0.8); color: #fff; }
.float-status.running { background: rgba(59,130,246,0.8); color: #fff; }
.float-status.preview { background: rgba(251,191,36,0.8); color: #000; }
.float-status.replace { background: rgba(249,115,22,0.8); color: #fff; }

.float-round {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  backdrop-filter: blur(8px);
}

/* 场景控制按钮 */
.scene-controls {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  gap: 6px;
  z-index: 10;
}

.scene-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background 0.2s;
}

.scene-btn:hover { background: rgba(255,255,255,0.2); }

/* ===== 右侧：控制面板 ===== */
.battle3d-panel {
  width: 360px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #111827;
  border-left: 1px solid rgba(255,255,255,0.1);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(0,0,0,0.3);
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.panel-actions { display: flex; gap: 6px; }

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.icon-btn:hover { background: rgba(255,255,255,0.2); }

/* 面板内容区 */
.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-content::-webkit-scrollbar { width: 4px; }
.panel-content::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 2px; }

/* ===== 开始面板 ===== */
.start-panel {
  justify-content: center;
  align-items: center;
  text-align: center;
}

.start-section h3 {
  color: #fff;
  font-size: 16px;
  margin-bottom: 12px;
}

.format-row {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
}

.format-btn {
  flex: 1;
  padding: 8px;
  border: 1px solid rgba(255,255,255,0.2);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.7);
  font-size: 12px;
  cursor: pointer;
}

.format-btn:first-child { border-radius: 6px 0 0 6px; }
.format-btn:last-child { border-radius: 0 6px 6px 0; }
.format-btn.active { background: #3b82f6; color: #fff; border-color: #3b82f6; }

.start-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.hint { color: rgba(255,255,255,0.4); font-size: 11px; margin-top: 8px; }

/* ===== 战斗面板 ===== */
.battle-panel { gap: 8px; }

/* 双方信息并排 */
.teams-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.team-card {
  padding: 8px;
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
}

.team-card.opponent { border: 1px solid rgba(239,68,68,0.3); }
.team-card.player { border: 1px solid rgba(74,222,128,0.3); }

.team-label {
  font-size: 11px;
  font-weight: bold;
  color: rgba(255,255,255,0.7);
  margin-bottom: 6px;
}

.mon-mini { margin-bottom: 4px; }
.mon-mini:last-child { margin-bottom: 0; }

.mon-name {
  font-size: 12px;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 2px;
}

.mini-hp {
  height: 6px;
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
  overflow: hidden;
}

.mini-hp-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s;
}

.mon-hp-num {
  font-size: 10px;
  color: rgba(255,255,255,0.5);
}

/* 场地效果 */
.field-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.chip {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
}

.c-blue { background: #1e40af; color: #93c5fd; }
.c-red { background: #991b1b; color: #fca5a5; }
.c-purple { background: #581c87; color: #d8b4fe; }
.c-cyan { background: #155e75; color: #67e8f9; }
.c-amber { background: #92400e; color: #fcd34d; }
.c-orange { background: #7c2d12; color: #fdba74; }

/* 招式选择 */
.moves-section { display: flex; flex-direction: column; gap: 6px; }

.section-label {
  font-size: 12px;
  font-weight: bold;
  color: rgba(255,255,255,0.7);
}

.move-block {
  background: rgba(255,255,255,0.03);
  border-radius: 6px;
  padding: 6px;
}

.move-block-name {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 4px;
}

.move-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.move-btn {
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  border: 2px solid rgba(0,0,0,0.2);
  border-radius: 8px;
  background: var(--tc, #555);
  color: #fff;
  cursor: pointer;
  text-align: left;
  font-size: 11px;
  transition: all 0.15s;
  position: relative;
  overflow: hidden;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2), inset 0 1px 0 rgba(255,255,255,0.2);
}

.move-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255,255,255,0.15), transparent);
  pointer-events: none;
}

.move-btn:hover:not(:disabled) {
  filter: brightness(1.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.2);
}

.move-btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }

.move-btn.selected {
  border-color: #fbbf24 !important;
  box-shadow: 0 0 12px rgba(251,191,36,0.5), 0 4px 8px rgba(0,0,0,0.3);
  transform: scale(1.02);
}

.mv-name {
  font-weight: bold;
  font-size: 12px;
  line-height: 1.2;
}

.mv-info {
  font-size: 10px;
  opacity: 0.9;
  margin-top: 2px;
  display: flex;
  justify-content: space-between;
}

.mv-type-badge {
  display: inline-block;
  font-size: 9px;
  padding: 1px 6px;
  border-radius: 4px;
  margin-top: 3px;
  background: rgba(0,0,0,0.3);
  color: #fff;
  font-weight: bold;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  width: fit-content;
}

/* 目标选择 */
.target-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.target-label { font-size: 12px; }

.target-btn {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
}

.target-btn.active { background: rgba(239,68,68,0.4); border-color: #ef4444; }

/* 特殊系统 */
.special-row {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.special-btn {
  padding: 3px 8px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 10px;
  cursor: pointer;
}

.special-btn.active { background: rgba(99,102,241,0.5); border-color: #6366f1; }

/* 替补选择 */
.replacement-section { display: flex; flex-direction: column; gap: 4px; }

.replace-btn {
  padding: 6px 10px;
  border: 2px solid rgba(255,255,255,0.1);
  border-radius: 6px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  text-align: left;
}

.replace-btn.active { background: rgba(74,222,128,0.3); border-color: #4ade80; }

/* 操作按钮 */
.action-row {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.btn {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: #3b82f6; color: #fff; flex: 1; }
.btn-primary:hover:not(:disabled) { background: #2563eb; }
.btn-secondary { background: rgba(255,255,255,0.1); color: #fff; }
.btn-danger { background: rgba(239,68,68,0.3); color: #ef4444; }
.btn-purple { background: #7c3aed; color: #fff; flex: 1; }
.btn-green { background: #16a34a; color: #fff; flex: 1; }
.btn-large { padding: 12px; font-size: 15px; }

/* 战斗日志 */
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
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: bold;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
}

.log-toggle { font-size: 10px; }

.log-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
  font-size: 11px;
}

.log-body::-webkit-scrollbar { width: 3px; }
.log-body::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 2px; }

.log-round { margin-bottom: 4px; }

.log-round-h {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 0;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  font-weight: bold;
}

.log-arrow { font-size: 8px; transition: transform 0.2s; }
.log-arrow.open { transform: rotate(90deg); }

.log-count {
  font-size: 9px;
  background: rgba(255,255,255,0.2);
  padding: 0 4px;
  border-radius: 3px;
  margin-left: auto;
}

.log-events { padding: 2px 0 4px 14px; }

.log-evt {
  padding: 2px 0;
  color: rgba(255,255,255,0.7);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  font-size: 11px;
  line-height: 1.4;
}

.log-evt:last-child { border-bottom: none; }
.e-switch { color: #60a5fa; font-weight: bold; }
.e-damage { color: #f87171; }
.e-heal { color: #4ade80; }

/* ===== 加载屏幕 ===== */
.loading-overlay {
  position: absolute;
  inset: 0;
  background: #0a0a1a;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.loading-content { text-align: center; }

.loading-icon {
  font-size: 48px;
  margin-bottom: 16px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.loading-title {
  font-size: 20px;
  color: #fff;
  margin-bottom: 16px;
}

.loading-bar {
  width: 200px;
  height: 4px;
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
  overflow: hidden;
  margin: 0 auto 10px;
}

.loading-fill {
  height: 100%;
  background: #3b82f6;
  transition: width 0.3s;
}

.loading-message {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .battle3d-container {
    flex-direction: column;
  }

  .battle3d-scene-area {
    height: 50vh;
  }

  .battle3d-panel {
    width: 100%;
    height: 50vh;
  }
}
</style>
