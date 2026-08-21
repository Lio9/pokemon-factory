<template>
  <div class="sd-page">
    <!-- ===== 无战斗时：开始面板 ===== -->
    <div v-if="!summary && !factoryRun" class="sd-start">
      <div class="sd-format-row">
        <button
          v-for="f in formats"
          :key="f.id"
          class="sd-fmt-btn"
          :class="battleFormat === f.id ? 'sd-fmt-on' : ''"
          @click="setBattleFormat(f.id)"
        >{{ f.label }}</button>
      </div>
      <div class="sd-btn-row">
        <button class="sd-btn sd-btn-blue" :disabled="isBusy" @click="startBattle">
          {{ busyAction === 'start-manual' ? '...' : '手动对战' }}
        </button>
        <button v-if="isAuthenticated" class="sd-btn sd-btn-purple" :disabled="isBusy" @click="startFactoryChallenge">
          {{ busyAction === 'factory-start' ? '...' : '工厂挑战' }}
        </button>
        <button v-if="isAuthenticated" class="sd-btn sd-btn-green" :disabled="isBusy" @click="startAsyncBattle">
          异步模拟
        </button>
      </div>
      <p v-if="!isAuthenticated" class="sd-hint">游客模式：可直接手动对战</p>
    </div>

    <!-- ===== 工厂挑战：无当前战斗 ===== -->
    <div v-if="factoryRun && !summary" class="sd-factory-bar">
      <span>工厂 {{ factoryRun.wins || 0 }}W/{{ factoryRun.losses || 0 }}L · {{ factoryRun.current_battle || 0 }}/{{ factoryRun.max_battles || 9 }}</span>
      <button class="sd-btn sd-btn-blue sd-btn-sm" :disabled="isBusy" @click="nextFactoryBattle">下一轮</button>
      <button class="sd-btn sd-btn-red sd-btn-sm" :disabled="isBusy" @click="abandonFactoryRun">放弃</button>
    </div>

    <!-- ===== 错误提示 ===== -->
    <div v-if="requestError" class="sd-error">{{ requestError }}</div>

    <!-- ===== 战斗界面 ===== -->
    <template v-if="summary">
      <!-- 战场 -->
      <div class="bf">
        <!-- 对手区 -->
        <div class="bf-row bf-opp-row">
          <div v-for="(mon, i) in oppMons" :key="'o'+i" class="bf-mon">
            <div class="bf-info">
              <div class="bf-name">{{ mon.name || mon.name_en }} <span class="bf-lv">L{{ mon.level }}</span></div>
              <div class="bf-hp"><div class="bf-hp-bar" :class="hpColor(mon)" :style="{width: hpPct(mon)}" /></div>
              <div class="bf-hp-num">{{ mon.currentHp }}/{{ mon.maxHp }}</div>
              <div class="bf-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="bf-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>
            <button class="bf-sprite-btn" @click="onOppClick(mon)" @contextmenu.prevent="showDetail(mon)">
              <img :src="sprite(mon, false)" class="bf-sprite bf-sprite-opp" :class="{fainted:mon.fainted}" @error="imgErr($event,mon,false)">
            </button>
          </div>
        </div>
        <!-- 我方区 -->
        <div class="bf-row bf-my-row">
          <div v-for="(mon, i) in myMons" :key="'m'+i" class="bf-mon">
            <button class="bf-sprite-btn" @contextmenu.prevent="showDetail(mon)">
              <img :src="sprite(mon, true)" class="bf-sprite bf-sprite-my" :class="{fainted:mon.fainted}" @error="imgErr($event,mon,true)">
            </button>
            <div class="bf-info">
              <div class="bf-name">{{ mon.name || mon.name_en }} <span class="bf-lv">L{{ mon.level }}</span></div>
              <div class="bf-hp"><div class="bf-hp-bar" :class="hpColor(mon)" :style="{width: hpPct(mon)}" /></div>
              <div class="bf-hp-num">{{ mon.currentHp }}/{{ mon.maxHp }}</div>
              <div class="bf-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="bf-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>
          </div>
        </div>
        <!-- 场地效果 -->
        <div v-if="fieldChips.length" class="bf-field">
          <span v-for="c in fieldChips" :key="c.l" class="bf-chip" :class="c.cls">{{ c.l }}</span>
        </div>
      </div>

      <!-- ===== 预览阶段 ===== -->
      <div v-if="isPreviewPhase" class="panel">
        <div class="panel-hdr">
          <span class="panel-title">队伍预览</span>
          <span class="panel-sub">选{{ rosterLimit }}出战 · {{ leadLimit }}首发 · 已选{{ selectedRosterIndexes.length }}/{{ rosterLimit }} · 首发{{ leadRosterIndexes.length }}/{{ leadLimit }}</span>
        </div>
        <!-- 对手 -->
        <div class="roster-row">
          <span class="roster-label roster-label-red">对手</span>
          <div class="roster-cards">
            <div v-for="(p,i) in opponentRoster" :key="'or'+i" class="r-card r-card-opp" @click="showDetail(p)">
              <img :src="spr(p)" class="r-img" @error="imgErr2($event,p)">
              <span class="r-name">{{ p.name || p.name_en }}</span>
            </div>
          </div>
        </div>
        <!-- 我方 -->
        <div class="roster-row">
          <span class="roster-label roster-label-green">你的队伍</span>
          <div class="roster-cards">
            <button v-for="(p,i) in playerRoster" :key="'pr'+i" type="button"
              class="r-card" :class="isPicked(i)?'r-picked':'r-dim'" :style="isLead(i)?'border-color:#fbbf24':''"
              @click="toggleRoster(i)" @contextmenu.prevent="toggleLead(i)">
              <img :src="spr(p)" class="r-img" :class="isPicked(i)?'':'r-img-dim'" @error="imgErr2($event,p)">
              <span class="r-name">{{ p.name || p.name_en }}</span>
              <span v-if="isLead(i)" class="r-star">★</span>
            </button>
          </div>
        </div>
        <button class="sd-btn sd-btn-blue sd-btn-full" :disabled="!canConfirmPreview||isBusy" @click="confirmPreview">
          {{ busyAction==='confirm-preview' ? '确认中...' : '确认出战' }}
        </button>
      </div>

      <!-- ===== 补位阶段 ===== -->
      <div v-if="isReplacementPhase" class="panel">
        <div class="panel-hdr"><span class="panel-title">补位</span><span class="panel-sub">选{{ pendingReplacementCount }}只上场</span></div>
        <div class="sw-grid">
          <button v-for="o in replacementBenchOptions" :key="o.value" type="button"
            class="sw-btn" :class="selectedReplacementIndexes.includes(o.value)?'sw-on':''"
            @click="toggleReplacement(o.value)">
            <img :src="spr(o.pokemon||o)" class="sw-img" @error="imgErr2($event,o.pokemon||o)">
            <div class="sw-info"><span class="sw-name">{{ o.label }}</span>
            <div class="sw-hp"><div class="sw-hp-bar" :style="{width:hpPct2(o),background:hpCol2(o)}" /></div></div>
          </button>
        </div>
        <button class="sd-btn sd-btn-red sd-btn-full" :disabled="!canConfirmReplacement||isBusy" @click="confirmReplacement">确认替补</button>
      </div>

      <!-- ===== 战斗操作阶段 ===== -->
      <div v-if="!isPreviewPhase && !isReplacementPhase" class="panel">
        <div v-if="myMons.length">
          <div v-for="mon in myMons" :key="'act'+mon.fieldSlot" class="act-section">
            <!-- 招式/换人 切换 -->
            <div class="act-toggle">
              <button class="act-tog" :class="(selectedActions['action-slot-'+mon.fieldSlot]||'move')==='move'?'act-tog-on':''" @click="setSelectedAction(mon.fieldSlot,'move')">招式</button>
              <button class="act-tog" :class="selectedActions['action-slot-'+mon.fieldSlot]==='switch'?'act-tog-on':''" :disabled="!playerBenchOptions.length" @click="setSelectedAction(mon.fieldSlot,'switch')">换人</button>
            </div>

            <!-- 招式面板 -->
            <template v-if="(selectedActions['action-slot-'+mon.fieldSlot]||'move')==='move'">
              <div class="mv-grid">
                <button v-for="(mv,mi) in mon.moves" :key="mv.name_en||mv.name" type="button"
                  class="mv-btn" :class="selectedMoves['slot-'+mon.fieldSlot]===(mv.name_en||mv.name)?'mv-sel':''"
                  :style="{'--tc': typeCol(mv.type_id)}"
                  @click="setSelectedMove(mon.fieldSlot, mv.name_en||mv.name)">
                  <div class="mv-top"><span class="mv-name">{{ mv.name || mv.name_en }}</span><span v-if="mv.target_id&&mv.target_id!==10" class="mv-tgt">{{ tgtLabel(mv.target_id) }}</span></div>
                  <div class="mv-bot">
                    <span v-if="mv.power" class="mv-stat">{{ mv.power }}</span>
                    <span v-if="mv.accuracy" class="mv-stat">{{ mv.accuracy }}%</span>
                    <span class="mv-pp">{{ mv.currentPp!=null ? mv.currentPp+'/'+(mv.maxPp||mv.pp||'?') : '' }}</span>
                    <span class="mv-type">{{ typeName(mv.type_id) }}</span>
                  </div>
                </button>
              </div>
              <!-- 目标选择 -->
              <div v-if="moveNeedsOpponentTarget(selMoveObj(mon)) && oppMons.length" class="tgt-row">
                <span class="tgt-label">目标:</span>
                <button v-for="t in oppMons" :key="t.fieldSlot" type="button"
                  class="tgt-btn" :class="selectedTargets['target-slot-'+mon.fieldSlot]===t.fieldSlot?'tgt-on':''"
                  @click="setSelectedTarget(mon.fieldSlot, t.fieldSlot)">{{ t.name || t.name_en }}</button>
              </div>
              <!-- 特殊系统 -->
              <div v-if="availableSpecialSystems(mon).length" class="sp-row">
                <button class="sp-btn" :class="!selectedSpecialSystems['special-slot-'+mon.fieldSlot]?'sp-on':''" @click="setSelectedSpecialSystem(mon.fieldSlot,undefined)">无</button>
                <button v-for="s in availableSpecialSystems(mon)" :key="s" class="sp-btn" :class="selectedSpecialSystems['special-slot-'+mon.fieldSlot]===s?'sp-on':''" @click="setSelectedSpecialSystem(mon.fieldSlot,s)">{{ specialSystemLabel(s) }}<template v-if="s==='tera'"> · {{ teraTypeLabel(mon) }}</template></button>
              </div>
            </template>

            <!-- 换人面板 -->
            <template v-else>
              <div class="sw-grid">
                <button v-for="t in playerBenchOptions" :key="t.value" type="button"
                  class="sw-btn" :class="selectedSwitchTargets['switch-slot-'+mon.fieldSlot]===t.value?'sw-on':''"
                  @click="setSelectedSwitchTarget(mon.fieldSlot, t.value)">
                  <img :src="spr(t.pokemon||t)" class="sw-img" @error="imgErr2($event,t.pokemon||t)">
                  <div class="sw-info"><span class="sw-name">{{ t.label }}</span>
                  <div class="sw-hp"><div class="sw-hp-bar" :style="{width:hpPct2(t),background:hpCol2(t)}" /></div></div>
                </button>
              </div>
            </template>
          </div>

          <!-- 提交 -->
          <button class="sd-btn sd-btn-blue sd-btn-full" :disabled="!canSubmitMove||isBusy" @click="submitMove">
            {{ busyAction==='submit-move' ? '提交中...' : '提交回合' }}
          </button>
        </div>
        <div v-else class="sd-empty">开始对战后这里会显示招式选择</div>
      </div>

      <!-- 操作栏 -->
      <div class="sd-bar">
        <button v-if="summary.status!=='completed'" class="sd-btn sd-btn-sm" :disabled="isBusy" @click="refreshStatus">🔄 刷新</button>
        <button v-if="summary.status==='running'" class="sd-btn sd-btn-red sd-btn-sm" :disabled="isBusy" @click="forfeitBattle">投降</button>
        <button v-if="showContinueFactoryButton" class="sd-btn sd-btn-blue sd-btn-sm" :disabled="isBusy" @click="prepareNextFactoryStage">下一轮</button>
        <button v-if="showResetBattleButton" class="sd-btn sd-btn-sm" :disabled="isBusy" @click="resetBattleState({keepFactoryRun:false})">重置</button>
      </div>

      <!-- 日志 -->
      <div class="log">
        <div class="log-hdr"><span>战斗日志</span><span v-if="summary.currentRound">回合 {{ summary.currentRound }}</span></div>
        <div class="log-body" ref="logEl">
          <template v-if="summary.rounds?.length">
            <div v-for="(r,ri) in summary.rounds" :key="ri" class="log-round">
              <div class="log-rhdr" @click="togRound(ri)"><span class="log-arw" :class="expRounds.has(ri)?'open':''">▶</span> {{ r.round===0?'开场':'Turn '+r.round }} <span class="log-cnt">{{ (r.events||[]).length }}</span></div>
              <div v-if="expRounds.has(ri)" class="log-evts"><div v-for="(e,ei) in r.events||[]" :key="ei" class="log-evt">{{ e }}</div></div>
            </div>
          </template>
          <div v-else class="sd-empty">等待战斗开始...</div>
        </div>
      </div>
    </template>

    <!-- 详情弹窗 -->
    <PokemonDetailPopover v-model:visible="detailVis" :pokemon="detailMon" />
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { useBattlePageState } from '../composables/useBattlePageState'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'
import { typeColor as typeColFn, typeNameZh, typeNameEn, getTypeEffectiveness, resolveTypeId } from '../services/typeChart'
import { normalizeFactoryRun } from '../services/contracts/battleContract'
import api from '../services/api'
import PokemonDetailPopover from '../components/PokemonDetailPopover.vue'

const { translate: tr } = useLocale()

const {
  actionHeadline, actionDescription, abandonFactoryRun, availableActionCount,
  battleFormat, busyAction, canConfirmPreview, canConfirmReplacement,
  canTerastallize, canSubmitMove, confirmPreview, confirmReplacement,
  currentBattleId, currentUser, exchangeCandidates, factoryRun, forfeitBattle,
  handleMobileAction, isAuthenticated, isBusy, isLead, isPicked,
  isPreviewPhase, isReplacementPhase, lastUpdatedLabel, leadLimit,
  leadRosterIndexes, moveEffectivenessHints, moveNeedsOpponentTarget,
  nextFactoryBattle, onConfirmExchange, onSettlementClose, openLeaderboard,
  opponentActiveMons, opponentActiveOptions, opponentRoster,
  pendingReplacementCount, playerActiveMons, playerBenchOptions, playerRoster,
  pollingActive, previewCardClass, replacementBenchOptions, requestError,
  resetBattleState, refreshStatus, resultText, rosterLimit, selectedActions,
  setSelectedAction, selectedMoveObject, selectedMoves, setSelectedMove,
  selectedSpecialSystems, setSelectedSpecialSystem, selectedReplacementIndexes,
  selectedRosterIndexes, selectedSwitchTargets, setSelectedSwitchTarget,
  selectedTargets, setSelectedTarget, setBattleFormat, settlement,
  setShowDebugPanel, showContinueFactoryButton, showExchange, showLeaderboard,
  showResetBattleButton, startAsyncBattle, startBattle,
  startFactoryChallenge, statusText, statusTone, submitMove, summary,
  prepareNextFactoryStage, toggleLead, toggleReplacement, toggleRoster
} = useBattlePageState()
const formats = [
  { id: 'vgc-doubles', label: '双打 (64)' },
  { id: 'vgc63', label: '63 单打' },
  { id: 'gen9singles', label: '9代单打' }
]

// ===== 特殊系统标签 =====
function teraTypeLabel(mon) {
  const tt = mon?.teraType || {}
  return tt.name || tt.name_en || `Type ${tt.type_id || mon?.teraTypeId || '?'}`
}
function specialSystemLabel(sys) {
  switch (sys) {
    case 'tera': return '太晶化'
    case 'mega': return 'Mega'
    case 'z-move': return 'Z招式'
    case 'dynamax': return '极巨化'
    default: return sys || ''
  }
}
function availableSpecialSystems(mon) {
  return (mon?.specialSystems || []).filter((sys) => {
    if (sys === 'tera') return !mon?.terastallized && Number(mon?.teraTypeId || mon?.teraType?.type_id || 0) > 0 && !summary.value?.playerTeraUsed
    if (summary.value?.playerSpecialUsed) return false
    if (sys === 'mega') return !!mon?.megaEligible && !mon?.megaEvolved
    if (sys === 'z-move') return !!mon?.zMoveEligible && !mon?.zMoveUsed
    if (sys === 'dynamax') return !!mon?.dynamaxEligible && !mon?.dynamaxed
    return false
  })
}

// ===== 精灵数据 =====
const myTeam = computed(() => summary.value?.playerTeam || [])
const oppTeam = computed(() => summary.value?.opponentTeam || [])
const myMons = computed(() => {
  const slots = summary.value?.playerActiveSlots || []
  return slots.map((ti, fs) => {
    const m = myTeam.value?.[ti]
    return m ? { ...m, teamIndex: ti, fieldSlot: fs, maxHp: m?.stats?.hp || m?.currentHp || 0 } : null
  }).filter(Boolean)
})
const oppMons = computed(() => {
  const slots = summary.value?.opponentActiveSlots || []
  return slots.map((ti, fs) => {
    const m = oppTeam.value?.[ti]
    return m ? { ...m, teamIndex: ti, fieldSlot: fs, maxHp: m?.stats?.hp || m?.currentHp || 0 } : null
  }).filter(Boolean)
})

// ===== HP =====
function hpPct(mon) {
  if (!mon.maxHp) return mon.fainted ? '0%' : '100%'
  return Math.max(0, Math.min(100, (mon.currentHp / mon.maxHp) * 100)) + '%'
}
function hpColor(mon) {
  const p = mon.maxHp > 0 ? (mon.currentHp / mon.maxHp) * 100 : 100
  if (p <= 0) return 'hp-e'
  if (p <= 20) return 'hp-c'
  if (p <= 50) return 'hp-l'
  return 'hp-h'
}
function hpPct2(o) { return o.maxHp > 0 ? Math.max(2, (o.hp / o.maxHp) * 100) + '%' : '100%' }
function hpCol2(o) { return o.maxHp > 0 && o.hp / o.maxHp <= 0.25 ? '#ef4444' : o.maxHp > 0 && o.hp / o.maxHp <= 0.5 ? '#fbbf24' : '#4ade80' }

// ===== 状态徽章 =====
const COND = { paralysis:'PAR', burn:'BRN', freeze:'FRZ', sleep:'SLP', poison:'PSN', toxic:'TOX', confusion:'CNF', taunt:'TNT' }
const CONDC = { paralysis:'#a16207', burn:'#c2410c', freeze:'#0369a1', sleep:'#6d28d9', poison:'#7e22ce', toxic:'#7e22ce', confusion:'#b45309', taunt:'#a16207' }
function badges(mon) {
  if (!mon || mon.fainted) return []
  const bs = []
  const c = mon.condition || mon.status
  if (c && CONDC[c]) bs.push({ t: COND[c] || c, c: CONDC[c] })
  if (mon.confused) bs.push({ t: 'CNF', c: CONDC.confusion })
  if ((mon.tauntTurns || 0) > 0) bs.push({ t: 'TNT', c: CONDC.taunt })
  if (mon.terastallized) bs.push({ t: 'Tera', c: '#6366f1' })
  const st = mon.statStages || {}
  for (const [k, n] of [['attack','Atk'],['specialAttack','SpA'],['defense','Def'],['specialDefense','SpD'],['speed','Spe']]) {
    const v = Number(st[k] || 0)
    if (v > 0) bs.push({ t: '+' + v + n, c: '#1d4ed8' })
    else if (v < 0) bs.push({ t: v + n, c: '#b91c1c' })
  }
  return bs
}

// ===== 场地效果 =====
const fieldChips = computed(() => {
  const fe = summary.value?.fieldEffects || {}
  const cs = []
  const p = (l, k, cls) => { const v = Number(fe[k] || 0); if (v > 0) cs.push({ l: v > 1 ? l + ' ' + v + 'T' : l, cls }) }
  p('TW', 'playerTailwindTurns', 'chip-blue'); p('TW', 'opponentTailwindTurns', 'chip-red')
  p('TR', 'trickRoomTurns', 'chip-purple'); p('Rain', 'rainTurns', 'chip-cyan')
  p('Sun', 'sunTurns', 'chip-amber'); p('Sand', 'sandTurns', 'chip-orange')
  p('Snow', 'snowTurns', 'chip-sky'); p('E-T', 'electricTerrainTurns', 'chip-yellow')
  p('P-T', 'psychicTerrainTurns', 'chip-purple'); p('G-T', 'grassyTerrainTurns', 'chip-green')
  p('Reflect', 'playerReflectTurns', 'chip-blue'); p('LS', 'playerLightScreenTurns', 'chip-blue')
  return cs
})

// ===== Sprite =====
function sprite(mon, back) {
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  if (!id) return sprites.default
  return back ? sprites.pokemonBack(id) : sprites.pokemon(id)
}
function spr(p) { const id = p?.form_id || p?.species_id || p?.pokemon_id || p?.id; return id ? sprites.pokemon(id) : sprites.default }
function imgErr(e, mon, back) { const id = mon?.form_id || mon?.species_id || mon?.id; e.target.src = back ? sprites.fallbackPokemonBack(id) : sprites.fallbackPokemon(id) }
function imgErr2(e, p) { const id = p?.form_id || p?.species_id || p?.id; e.target.src = sprites.fallbackPokemon(id) }

// ===== 招式辅助 =====
function typeCol(id) { return typeColFn(id) }
function typeName(id) { return tr(typeNameZh(id), typeNameEn(id)) }
function tgtLabel(tid) {
  switch (tid) { case 4: case 7: return '自身'; case 8: return '随机'; case 9: return '全体'; case 11: return '群'; case 13: return '己方'; case 14: return '全场'; default: return '' }
}
function selMoveObj(mon) { return selectedMoveObject(mon) }

// ===== 目标点击 =====
function onOppClick(mon) { /* could be used for target selection */ }

// ===== 详情弹窗 =====
const detailVis = ref(false)
const detailMon = ref(null)
function showDetail(p) { detailMon.value = p; detailVis.value = true }

// ===== 日志 =====
const expRounds = ref(new Set())
const logEl = ref(null)
function togRound(i) { const s = new Set(expRounds.value); s.has(i) ? s.delete(i) : s.add(i); expRounds.value = s }
watch(() => summary.value?.rounds?.length, (n) => {
  if (!n) return
  const s = new Set(); for (let i = Math.max(0, n - 2); i < n; i++) s.add(i); expRounds.value = s
  nextTick(() => { if (logEl.value) logEl.value.scrollTop = logEl.value.scrollHeight })
})
</script>

<style>
/* ===== 全局 ===== */
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: #1a1a1a; }
.sd-page { display: flex; flex-direction: column; gap: 4px; max-width: 640px; margin: 0 auto; padding: 8px; font-family: 'Segoe UI', Arial, sans-serif; color: #e2e8f0; }

/* ===== 开始面板 ===== */
.sd-start { background: #2d2d2d; border-radius: 4px; padding: 16px; }
.sd-format-row { display: flex; gap: 4px; margin-bottom: 12px; }
.sd-fmt-btn { padding: 6px 14px; font-size: 12px; font-weight: 600; border: 1px solid #555; border-radius: 3px; background: #1a1a1a; color: #94a3b8; cursor: pointer; }
.sd-fmt-on { background: #3b82f6; color: #fff; border-color: #3b82f6; }
.sd-btn-row { display: flex; flex-wrap: wrap; gap: 8px; }
.sd-btn { padding: 8px 16px; font-size: 13px; font-weight: 700; border: none; border-radius: 3px; cursor: pointer; color: #fff; }
.sd-btn:disabled { background: #374151 !important; color: #6b7280; cursor: not-allowed; }
.sd-btn-blue { background: #3b82f6; } .sd-btn-blue:hover:not(:disabled) { background: #2563eb; }
.sd-btn-purple { background: #6366f1; } .sd-btn-purple:hover:not(:disabled) { background: #4f46e5; }
.sd-btn-green { background: #10b981; } .sd-btn-green:hover:not(:disabled) { background: #059669; }
.sd-btn-red { background: #dc2626; } .sd-btn-red:hover:not(:disabled) { background: #b91c1c; }
.sd-btn-sm { padding: 4px 10px; font-size: 11px; }
.sd-btn-full { width: 100%; margin-top: 8px; }
.sd-hint { margin-top: 8px; font-size: 11px; color: #f59e0b; }
.sd-error { padding: 8px 12px; background: #451a1a; border: 1px solid #7f1d1d; border-radius: 3px; color: #fca5a5; font-size: 12px; }
.sd-empty { padding: 16px; text-align: center; font-size: 12px; color: #64748b; }
.sd-factory-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #2d2d2d; border-radius: 4px; font-size: 12px; font-weight: 600; }
.sd-bar { display: flex; align-items: center; gap: 6px; padding: 6px 8px; background: #252525; border-radius: 4px; }

/* ===== 战场 ===== */
.bf { background: linear-gradient(180deg, #6b8f5e 0%, #5a7a4f 40%, #4a6940 100%); border-radius: 4px; padding: 12px 16px; min-height: 300px; display: flex; flex-direction: column; justify-content: space-between; position: relative; }
.bf-row { display: flex; gap: 12px; }
.bf-opp-row { justify-content: flex-start; }
.bf-my-row { justify-content: flex-end; }
.bf-mon { display: flex; align-items: flex-start; gap: 8px; }
.bf-my-row .bf-mon { align-items: flex-end; flex-direction: row-reverse; }
.bf-info { min-width: 130px; }
.bf-name { font-size: 13px; font-weight: 700; color: #fff; text-shadow: 1px 1px 2px rgba(0,0,0,0.7); }
.bf-lv { font-size: 11px; color: #a7f3d0; font-weight: 600; }
.bf-hp { height: 10px; background: #1a1a1a; border-radius: 2px; overflow: hidden; border: 1px solid #333; margin: 2px 0; }
.bf-hp-bar { height: 100%; transition: width 0.5s; border-radius: 1px; }
.hp-h { background: #4ade80; } .hp-l { background: #fbbf24; } .hp-c { background: #f87171; } .hp-e { background: #666; }
.bf-hp-num { font-size: 11px; color: #ddd; font-weight: 600; text-shadow: 1px 1px 2px rgba(0,0,0,0.5); }
.bf-tags { display: flex; flex-wrap: wrap; gap: 3px; margin-top: 3px; }
.bf-tag { font-size: 9px; font-weight: 700; color: #fff; padding: 1px 4px; border-radius: 2px; }
.bf-sprite-btn { background: none; border: none; cursor: default; padding: 0; }
.bf-sprite { width: 96px; height: 96px; object-fit: contain; image-rendering: pixelated; transition: all 0.3s; }
.bf-sprite-opp { animation: float 3s ease-in-out infinite; }
@keyframes float { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
.bf-sprite.fainted { filter: grayscale(1) brightness(0.4); opacity: 0.4; transform: translateY(20px) rotate(60deg); }
.bf-field { position: absolute; top: 4px; left: 50%; transform: translateX(-50%); display: flex; gap: 4px; flex-wrap: wrap; justify-content: center; }
.bf-chip { font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 2px; color: #fff; text-shadow: 1px 1px 1px rgba(0,0,0,0.5); }
.chip-blue { background: rgba(59,130,246,0.7); } .chip-red { background: rgba(239,68,68,0.7); } .chip-purple { background: rgba(147,51,234,0.7); }
.chip-cyan { background: rgba(6,182,212,0.7); } .chip-amber { background: rgba(245,158,11,0.7); } .chip-orange { background: rgba(249,115,22,0.7); }
.chip-sky { background: rgba(14,165,233,0.7); } .chip-yellow { background: rgba(234,179,8,0.7); } .chip-green { background: rgba(34,197,94,0.7); }

/* ===== 面板（预览/操作） ===== */
.panel { background: #2d2d2d; border-radius: 4px; padding: 12px; }
.panel-hdr { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid #444; }
.panel-title { font-size: 14px; font-weight: 700; color: #e2e8f0; }
.panel-sub { font-size: 11px; color: #94a3b8; }

/* 队伍预览 */
.roster-row { margin-bottom: 10px; }
.roster-label { display: block; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 4px; }
.roster-label-red { color: #f87171; } .roster-label-green { color: #4ade80; }
.roster-cards { display: flex; flex-wrap: wrap; gap: 4px; }
.r-card { display: flex; flex-direction: column; align-items: center; padding: 4px 6px; border: 2px solid #444; border-radius: 3px; background: #1a1a1a; cursor: pointer; min-width: 60px; transition: all 0.15s; position: relative; }
.r-card:hover { border-color: #666; }
.r-card-opp { cursor: default; opacity: 0.7; }
.r-picked { border-color: #4ade80; background: #1a2e1a; }
.r-dim { opacity: 0.4; }
.r-star { position: absolute; top: 2px; right: 4px; font-size: 10px; color: #fbbf24; }
.r-img { width: 40px; height: 40px; object-fit: contain; image-rendering: pixelated; }
.r-img-dim { filter: grayscale(0.7) brightness(0.5); }
.r-name { font-size: 9px; color: #cbd5e1; max-width: 56px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }

/* 补位/换人 */
.sw-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 4px; }
.sw-btn { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border: 2px solid #444; border-radius: 3px; background: #252525; cursor: pointer; transition: all 0.15s; }
.sw-btn:hover { border-color: #666; }
.sw-on { border-color: #3b82f6; background: #1a2a3a; }
.sw-img { width: 32px; height: 32px; object-fit: contain; image-rendering: pixelated; }
.sw-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.sw-name { font-size: 11px; color: #e2e8f0; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sw-hp { height: 5px; background: #333; border-radius: 2px; overflow: hidden; }
.sw-hp-bar { height: 100%; transition: width 0.3s; }

/* 操作切换 */
.act-section { background: #1a1a1a; border: 1px solid #333; border-radius: 3px; padding: 8px; margin-bottom: 8px; }
.act-toggle { display: flex; gap: 2px; margin-bottom: 6px; }
.act-tog { flex: 1; padding: 4px 8px; font-size: 11px; font-weight: 700; border: 1px solid #444; background: #252525; color: #94a3b8; cursor: pointer; }
.act-tog:first-child { border-radius: 3px 0 0 3px; }
.act-tog:last-child { border-radius: 0 3px 3px 0; }
.act-tog-on { background: #3b82f6; color: #fff; border-color: #3b82f6; }
.act-tog:disabled { opacity: 0.3; cursor: not-allowed; }

/* 招式按钮 */
.mv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.mv-btn { display: flex; flex-direction: column; gap: 2px; padding: 6px 8px; border: 2px solid #555; border-radius: 4px; background: var(--tc, #666); color: #fff; cursor: pointer; text-align: left; font-size: 12px; text-shadow: 1px 1px 2px rgba(0,0,0,0.5); min-height: 48px; transition: all 0.15s; }
.mv-btn:hover:not(:disabled) { filter: brightness(1.15); border-color: #888; }
.mv-sel { border-color: #fbbf24; box-shadow: 0 0 8px rgba(251,191,36,0.5); }
.mv-top { display: flex; justify-content: space-between; align-items: center; }
.mv-name { font-weight: 700; font-size: 13px; }
.mv-tgt { font-size: 9px; background: rgba(0,0,0,0.3); padding: 1px 4px; border-radius: 2px; }
.mv-bot { display: flex; align-items: center; gap: 6px; font-size: 10px; opacity: 0.85; }
.mv-stat { font-weight: 600; }
.mv-pp { margin-left: auto; font-weight: 600; }
.mv-type { background: rgba(0,0,0,0.3); padding: 1px 4px; border-radius: 2px; font-weight: 700; font-size: 9px; text-transform: uppercase; }

/* 目标/特殊系统 */
.tgt-row { display: flex; align-items: center; gap: 4px; margin-top: 6px; }
.tgt-label { font-size: 11px; color: #94a3b8; font-weight: 600; }
.tgt-btn { padding: 4px 10px; font-size: 11px; font-weight: 600; border: 1px solid #555; border-radius: 3px; background: #252525; color: #e2e8f0; cursor: pointer; }
.tgt-on { border-color: #f87171; background: #3a1a1a; color: #fca5a5; }
.sp-row { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.sp-btn { padding: 3px 8px; font-size: 10px; font-weight: 600; border: 1px solid #555; border-radius: 3px; background: #252525; color: #fcd34d; cursor: pointer; }
.sp-on { background: #92400e; border-color: #f59e0b; color: #fef3c7; }

/* ===== 日志 ===== */
.log { background: #1a1a1a; border-top: 2px solid #333; border-radius: 0 0 4px 4px; }
.log-hdr { display: flex; justify-content: space-between; padding: 6px 12px; background: #252525; border-bottom: 1px solid #333; font-size: 12px; font-weight: 700; color: #94a3b8; }
.log-body { max-height: 200px; overflow-y: auto; padding: 4px 0; }
.log-rhdr { display: flex; align-items: center; gap: 6px; padding: 3px 12px; font-size: 11px; font-weight: 700; color: #94a3b8; cursor: pointer; }
.log-rhdr:hover { background: #252525; }
.log-arw { font-size: 8px; transition: transform 0.2s; color: #64748b; }
.log-arw.open { transform: rotate(90deg); }
.log-cnt { font-size: 10px; color: #475569; background: #333; padding: 0 4px; border-radius: 2px; }
.log-evts { padding: 0 12px 4px 24px; }
.log-evt { font-size: 12px; color: #cbd5e1; padding: 2px 0; line-height: 1.4; border-bottom: 1px solid #1e1e1e; }
</style>
