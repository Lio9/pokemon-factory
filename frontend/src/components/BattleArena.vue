<template>
  <!-- Showdown 风格战场：深色背景、简洁 HP 条、紧凑布局 -->
  <div class="battle-showdown">
    <!-- 战场区域 -->
    <div v-if="summary" class="battlefield">
      <!-- 对手区：左侧 -->
      <div class="bf-side bf-opp">
        <div
          v-for="(mon, idx) in opponentActiveMons"
          :key="`opp-${idx}`"
          class="bf-mon"
        >
          <!-- HP 条 + 信息 -->
          <div class="bf-info">
            <div class="bf-name-row">
              <span class="bf-name">{{ mon.name || mon.name_en }}</span>
              <span v-if="mon.level" class="bf-level">L{{ mon.level }}</span>
              <span v-if="mon.terastallized" class="bf-tera">Tera</span>
            </div>
            <div class="bf-hp-bar">
              <div
                class="bf-hp-fill"
                :class="hpTone(mon)"
                :style="{ width: hpWidth(mon) }"
              />
            </div>
            <div class="bf-hp-text">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</div>
            <!-- 状态徽章 -->
            <div class="bf-status-row">
              <span
                v-for="badge in conditionBadges(mon)"
                :key="badge.label"
                class="bf-status-badge"
                :style="{ background: badge.color }"
              >{{ badge.label }}</span>
            </div>
          </div>
          <!-- 精灵图 -->
          <button
            type="button"
            class="bf-sprite-wrap"
            :class="canTarget ? 'bf-targetable' : ''"
            @click="onTargetClick(mon)"
            @contextmenu.prevent="openMonDetail(mon)"
          >
            <img
              :src="spriteUrl(mon, false)"
              :alt="mon.name"
              class="bf-sprite bf-sprite-opp"
              :class="[mon.fainted ? 'bf-fainted' : '', getAnimClass('opp', idx)]"
              @error="onSpriteError($event, mon, false)"
            >
          </button>
        </div>
      </div>

      <!-- 我方区：右侧 -->
      <div class="bf-side bf-player">
        <div
          v-for="(mon, idx) in playerActiveMons"
          :key="`plr-${idx}`"
          class="bf-mon"
        >
          <!-- 精灵图 -->
          <button
            type="button"
            class="bf-sprite-wrap"
            @contextmenu.prevent="openMonDetail(mon)"
          >
            <img
              :src="spriteUrl(mon, true)"
              :alt="mon.name"
              class="bf-sprite bf-sprite-player"
              :class="[mon.fainted ? 'bf-fainted' : '', getAnimClass('player', idx)]"
              @error="onSpriteError($event, mon, true)"
            >
          </button>
          <!-- HP 条 + 信息 -->
          <div class="bf-info">
            <div class="bf-name-row">
              <span class="bf-name">{{ mon.name || mon.name_en }}</span>
              <span v-if="mon.level" class="bf-level">L{{ mon.level }}</span>
              <span v-if="mon.terastallized" class="bf-tera">Tera</span>
            </div>
            <div class="bf-hp-bar">
              <div
                class="bf-hp-fill"
                :class="hpTone(mon)"
                :style="{ width: hpWidth(mon) }"
              />
            </div>
            <div class="bf-hp-text">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</div>
            <!-- 状态 + 道具 -->
            <div class="bf-status-row">
              <span
                v-for="badge in conditionBadges(mon)"
                :key="badge.label"
                class="bf-status-badge"
                :style="{ background: badge.color }"
              >{{ badge.label }}</span>
              <span v-if="mon.heldItem" class="bf-item">{{ mon.heldItem }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 场地效果条 -->
      <div v-if="fieldEffectChips.length" class="bf-field-effects">
        <span
          v-for="chip in fieldEffectChips"
          :key="chip.label"
          class="bf-field-chip"
          :class="chip.tone"
        >{{ chip.label }}</span>
      </div>
    </div>

    <!-- 战斗日志（Showdown 风格：紧凑、深色背景、可折叠） -->
    <div class="bf-log">
      <div class="bf-log-header">
        <span class="bf-log-title">{{ tr('战斗日志', 'Battle Log') }}</span>
        <span class="bf-log-round" v-if="summary?.currentRound">
          {{ tr('回合', 'Turn') }} {{ summary.currentRound }}
        </span>
      </div>
      <div class="bf-log-body" ref="logContainer">
        <template v-if="summary?.rounds?.length">
          <div
            v-for="(round, ri) in summary.rounds"
            :key="ri"
            class="bf-log-round"
          >
            <div
              class="bf-log-round-header"
              @click="toggleRound(ri)"
            >
              <span class="bf-log-arrow" :class="expandedRounds.has(ri) ? 'open' : ''">▶</span>
              {{ round.round === 0 ? tr('开场', 'Start') : `Turn ${round.round}` }}
              <span class="bf-log-count">{{ (round.events || []).length }}</span>
            </div>
            <div v-if="expandedRounds.has(ri)" class="bf-log-events">
              <div
                v-for="(event, ei) in round.events || []"
                :key="ei"
                class="bf-log-event"
              >{{ event }}</div>
            </div>
          </div>
        </template>
        <div v-else class="bf-log-empty">{{ tr('等待战斗开始...', 'Waiting...') }}</div>
      </div>
    </div>

    <!-- 精灵详情弹窗 -->
    <PokemonDetailPopover
      v-model:visible="showDetailDialog"
      :pokemon="detailPokemon"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'
import PokemonDetailPopover from './PokemonDetailPopover.vue'

const { translate: tr } = useLocale()

const emit = defineEmits(['target-select'])

const props = defineProps({
  summary: { type: Object, default: null },
  highlightIndex: { type: Number, default: -1 },
  statusText: { type: String, default: '' },
  statusTone: { type: String, default: 'neutral' },
  targetFieldSlot: { type: [Number, String], default: null },
  canTarget: { type: Boolean, default: false }
})

// === 精灵数据 ===
const playerTeam = computed(() => props.summary?.playerTeam || [])
const opponentTeam = computed(() => props.summary?.opponentTeam || [])

const playerActiveMons = computed(() => {
  const slots = props.summary?.playerActiveSlots || []
  return slots.map((teamIdx, fieldSlot) => {
    const mon = playerTeam.value?.[teamIdx]
    if (!mon) return null
    return { ...mon, teamIndex: teamIdx, fieldSlot, maxHp: mon?.stats?.hp || mon?.currentHp || 0 }
  }).filter(Boolean)
})

const opponentActiveMons = computed(() => {
  const slots = props.summary?.opponentActiveSlots || []
  return slots.map((teamIdx, fieldSlot) => {
    const mon = opponentTeam.value?.[teamIdx]
    if (!mon) return null
    return { ...mon, teamIndex: teamIdx, fieldSlot }
  }).filter(Boolean)
})

// === HP ===
function hpWidth(mon) {
  if (!mon.maxHp || mon.maxHp <= 0) return mon.fainted ? '0%' : '100%'
  return Math.max(0, Math.min(100, (mon.currentHp / mon.maxHp) * 100)) + '%'
}

function hpTone(mon) {
  const pct = mon.maxHp > 0 ? (mon.currentHp / mon.maxHp) * 100 : 100
  if (pct <= 0) return 'hp-empty'
  if (pct <= 20) return 'hp-critical'
  if (pct <= 50) return 'hp-low'
  return 'hp-high'
}

// === 状态 ===
const CONDITION_COLORS = {
  paralysis: '#a16207', burn: '#c2410c', freeze: '#0369a1',
  sleep: '#6d28d9', poison: '#7e22ce', toxic: '#7e22ce',
  confusion: '#b45309', taunt: '#a16207'
}
const CONDITION_LABELS = {
  paralysis: 'PAR', burn: 'BRN', freeze: 'FRZ',
  sleep: 'SLP', poison: 'PSN', toxic: 'TOX',
  confusion: 'CNF', taunt: 'TNT'
}

function conditionBadges(mon) {
  if (mon.fainted || !mon) return []
  const badges = []
  const cond = mon.condition || mon.status
  if (cond && CONDITION_COLORS[cond]) {
    badges.push({ label: CONDITION_LABELS[cond] || cond, color: CONDITION_COLORS[cond] })
  }
  if (mon.confused) badges.push({ label: 'CNF', color: CONDITION_COLORS.confusion })
  if ((mon.tauntTurns || 0) > 0) badges.push({ label: 'TNT', color: CONDITION_COLORS.taunt })
  // 能力阶级
  const stages = mon.statStages || {}
  for (const [key, name] of [['attack','Atk'],['specialAttack','SpA'],['defense','Def'],['specialDefense','SpD'],['speed','Spe']]) {
    const val = Number(stages[key] || 0)
    if (val > 0) badges.push({ label: `+${val}${name}`, color: '#1d4ed8' })
    else if (val < 0) badges.push({ label: `${val}${name}`, color: '#b91c1c' })
  }
  return badges
}

// === 场地效果 ===
const fieldEffectChips = computed(() => {
  const fe = props.summary?.fieldEffects || {}
  const chips = []
  const push = (label, key, tone) => {
    const val = Number(fe[key] || 0)
    if (val > 0) chips.push({ label: val > 1 ? `${label} ${val}T` : label, tone })
  }
  push('TW', 'playerTailwindTurns', 'chip-blue')
  push('TW', 'opponentTailwindTurns', 'chip-red')
  push('TR', 'trickRoomTurns', 'chip-purple')
  push('Rain', 'rainTurns', 'chip-cyan')
  push('Sun', 'sunTurns', 'chip-amber')
  push('Sand', 'sandTurns', 'chip-orange')
  push('Snow', 'snowTurns', 'chip-sky')
  push('E-Terrain', 'electricTerrainTurns', 'chip-yellow')
  push('P-Terrain', 'psychicTerrainTurns', 'chip-purple')
  push('G-Terrain', 'grassyTerrainTurns', 'chip-green')
  push('M-Terrain', 'mistyTerrainTurns', 'chip-pink')
  push('Reflect', 'playerReflectTurns', 'chip-blue')
  push('LightScreen', 'playerLightScreenTurns', 'chip-blue')
  if (Number(fe.playerSpikesLayers || 0) > 0) chips.push({ label: `Spikes ${fe.playerSpikesLayers}`, tone: 'chip-green' })
  if (Number(fe.opponentSpikesLayers || 0) > 0) chips.push({ label: `Spikes ${fe.opponentSpikesLayers}`, tone: 'chip-red' })
  return chips
})

// === 日志 ===
const expandedRounds = ref(new Set())
const logContainer = ref(null)

function toggleRound(ri) {
  const s = new Set(expandedRounds.value)
  if (s.has(ri)) s.delete(ri)
  else s.add(ri)
  expandedRounds.value = s
}

watch(() => props.summary?.rounds?.length, (len) => {
  if (len) {
    // 展开最后 2 回合
    const s = new Set()
    for (let i = Math.max(0, len - 2); i < len; i++) s.add(i)
    expandedRounds.value = s
    nextTick(() => {
      if (logContainer.value) logContainer.value.scrollTop = logContainer.value.scrollHeight
    })
  }
})

// === 点击目标 ===
function onTargetClick(mon) {
  if (!props.canTarget) return
  emit('target-select', mon.fieldSlot)
}

// === Sprite ===
function spriteUrl(mon, isBack) {
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  if (!id) return sprites.default
  return isBack ? sprites.pokemonBack(id) : sprites.pokemon(id)
}

function onSpriteError(event, mon, isBack) {
  const img = event.target
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  if (isBack) img.src = sprites.fallbackPokemonBack(id)
  else img.src = sprites.fallbackPokemon(id)
}

// === 详情弹窗 ===
const showDetailDialog = ref(false)
const detailPokemon = ref(null)
function openMonDetail(mon) {
  detailPokemon.value = mon
  showDetailDialog.value = true
}

// === 动画 ===
const animClasses = ref({})
function applyAnim(key, cls, duration = 600) {
  animClasses.value = { ...animClasses.value, [key]: cls }
  setTimeout(() => {
    const next = { ...animClasses.value }
    delete next[key]
    animClasses.value = next
  }, duration)
}
function getAnimClass(side, slot) {
  return animClasses.value[`${side}-${slot}`] || ''
}

watch(() => props.summary?.rounds?.length, (newLen, oldLen) => {
  if (!newLen || newLen <= (oldLen || 0)) return
  const latest = props.summary.rounds[newLen - 1]
  if (!latest?.events) return
  const text = latest.events.join(' ')
  const hasDmg = /伤害|damage|造成了/.test(text)
  const hasHeal = /回复|恢复|治愈|heal/.test(text)
  const opp = props.summary?.opponentActiveSlots || []
  const plr = props.summary?.playerActiveSlots || []
  if (hasDmg) {
    opp.forEach((_, i) => { if (Math.random() < 0.5) applyAnim(`opp-${i}`, 'bf-anim-hit') })
    plr.forEach((_, i) => { if (Math.random() < 0.5) applyAnim(`player-${i}`, 'bf-anim-hit') })
  }
  if (hasHeal) {
    plr.forEach((_, i) => applyAnim(`player-${i}`, 'bf-anim-heal'))
  }
})
</script>

<style scoped>
/* ===== Showdown 风格战场 ===== */
.battle-showdown {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: #2d2d2d;
  border-radius: 4px;
  overflow: hidden;
  font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

.battlefield {
  position: relative;
  background: linear-gradient(180deg, #6b8f5e 0%, #5a7a4f 40%, #4a6940 100%);
  min-height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12px 16px;
}

/* 对手区（上方） */
.bf-opp {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

/* 我方区（下方） */
.bf-player {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.bf-mon {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.bf-opp .bf-mon {
  flex-direction: row; /* info left, sprite right */
  align-items: flex-start;
}
.bf-player .bf-mon {
  flex-direction: row-reverse; /* sprite left, info right */
  align-items: flex-end;
}

/* HP 条 */
.bf-info {
  min-width: 140px;
}
.bf-name-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 2px;
}
.bf-name {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.7);
}
.bf-level {
  font-size: 11px;
  color: #a7f3d0;
  font-weight: 600;
}
.bf-tera {
  font-size: 10px;
  background: #6366f1;
  color: #fff;
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: 700;
}

.bf-hp-bar {
  height: 10px;
  background: #1a1a1a;
  border-radius: 2px;
  overflow: hidden;
  border: 1px solid #333;
}
.bf-hp-fill {
  height: 100%;
  transition: width 0.5s ease;
  border-radius: 1px;
}
.bf-hp-fill.hp-high { background: #4ade80; }
.bf-hp-fill.hp-low { background: #fbbf24; }
.bf-hp-fill.hp-critical { background: #f87171; }
.bf-hp-fill.hp-empty { background: #666; }

.bf-hp-text {
  font-size: 11px;
  color: #ddd;
  margin-top: 1px;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
}

.bf-status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 3px;
}
.bf-status-badge {
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  padding: 1px 4px;
  border-radius: 2px;
  text-transform: uppercase;
}
.bf-item {
  font-size: 9px;
  color: #fcd34d;
  font-weight: 600;
}

/* 精灵图 */
.bf-sprite-wrap {
  cursor: default;
  background: none;
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bf-targetable {
  cursor: pointer;
}
.bf-sprite {
  image-rendering: pixelated;
  width: 96px;
  height: 96px;
  object-fit: contain;
  transition: transform 0.3s ease, filter 0.3s ease, opacity 0.3s ease;
}
.bf-sprite-opp {
  animation: bf-float 3s ease-in-out infinite;
}
@keyframes bf-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.bf-fainted {
  filter: grayscale(1) brightness(0.4);
  opacity: 0.4;
  transform: translateY(20px) rotate(60deg);
}

/* 动画 */
.bf-anim-hit {
  animation: bf-hit 0.4s ease-out;
}
@keyframes bf-hit {
  0% { filter: brightness(1); transform: translateX(0); }
  20% { filter: brightness(2.5); transform: translateX(-6px); }
  40% { filter: brightness(1.2); transform: translateX(6px); }
  60% { filter: brightness(2); transform: translateX(-3px); }
  100% { filter: brightness(1); transform: translateX(0); }
}
.bf-anim-heal {
  animation: bf-heal 0.5s ease-out;
}
@keyframes bf-heal {
  0% { filter: brightness(1); }
  40% { filter: brightness(1.4) hue-rotate(90deg); }
  100% { filter: brightness(1) hue-rotate(0deg); }
}

/* 场地效果 */
.bf-field-effects {
  position: absolute;
  top: 4px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}
.bf-field-chip {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 2px;
  color: #fff;
  text-shadow: 1px 1px 1px rgba(0,0,0,0.5);
}
.chip-blue { background: rgba(59,130,246,0.7); }
.chip-red { background: rgba(239,68,68,0.7); }
.chip-purple { background: rgba(147,51,234,0.7); }
.chip-cyan { background: rgba(6,182,212,0.7); }
.chip-amber { background: rgba(245,158,11,0.7); }
.chip-orange { background: rgba(249,115,22,0.7); }
.chip-sky { background: rgba(14,165,233,0.7); }
.chip-yellow { background: rgba(234,179,8,0.7); }
.chip-green { background: rgba(34,197,94,0.7); }
.chip-pink { background: rgba(236,72,153,0.7); }

/* ===== 战斗日志（Showdown 风格） ===== */
.bf-log {
  background: #1a1a1a;
  border-top: 2px solid #333;
}
.bf-log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #252525;
  border-bottom: 1px solid #333;
}
.bf-log-title {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.bf-log-round {
  font-size: 11px;
  color: #64748b;
}
.bf-log-body {
  max-height: 200px;
  overflow-y: auto;
  padding: 4px 0;
}
.bf-log-round-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  cursor: pointer;
  user-select: none;
}
.bf-log-round-header:hover {
  background: #252525;
}
.bf-log-arrow {
  font-size: 8px;
  transition: transform 0.2s;
  color: #64748b;
}
.bf-log-arrow.open {
  transform: rotate(90deg);
}
.bf-log-count {
  font-size: 10px;
  color: #475569;
  background: #333;
  padding: 0 4px;
  border-radius: 2px;
}
.bf-log-events {
  padding: 0 12px 4px 24px;
}
.bf-log-event {
  font-size: 12px;
  color: #cbd5e1;
  padding: 2px 0;
  line-height: 1.4;
  border-bottom: 1px solid #1e1e1e;
}
.bf-log-empty {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
}
</style>
