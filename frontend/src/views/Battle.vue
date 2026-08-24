<template>
  <div class="sd-page">
    <!-- ===== 无战斗时：开始面板 ===== -->
    <div v-if="!summary && !factoryRun" class="sd-start">
      <div style="text-align:center;margin-bottom:12px;">
        <div style="font-size:16pt;font-weight:bold;color:#333;letter-spacing:0.02em;">⚔️ 对战工厂</div>
        <div style="font-size:9pt;color:#888;margin-top:2px;">Battle Factory</div>
      </div>
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
          ⚔️ {{ busyAction === 'start-manual' ? '创建中...' : '手动对战' }}
        </button>
        <button v-if="isAuthenticated" class="sd-btn sd-btn-purple" :disabled="isBusy" @click="startFactoryChallenge">
          🏟️ {{ busyAction === 'factory-start' ? '创建中...' : '工厂挑战' }}
        </button>
        <button v-if="isAuthenticated" class="sd-btn sd-btn-green" :disabled="isBusy" @click="startAsyncBattle">
          ⏩ 异步模拟
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
          <span class="roster-label roster-label-red">⚔️ 对手队伍</span>
          <div class="roster-cards">
            <div v-for="(p,i) in opponentRoster" :key="'or'+i" class="r-card r-card-opp" @click="showDetail(p)">
              <img :src="spr(p)" class="r-img" @error="imgErr2($event,p)">
              <span class="r-name">{{ p.name || p.name_en }}</span>
              <span class="r-types">{{ (p.types||[]).map(t=>t.name||t.name_en).join('/') }}</span>
            </div>
          </div>
        </div>
        <!-- 我方 -->
        <div class="roster-row">
          <span class="roster-label roster-label-green">🟢 你的队伍 · 点击选择，右键标记首发</span>
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
            <!-- 精灵信息头（Showdown 风格：sprite + 名字 + HP） -->
            <div class="act-header">
              <img :src="sprite(mon, true)" class="act-sprite" :class="{fainted:mon.fainted}" @error="imgErr($event,mon,true)">
              <div class="act-info">
                <div class="act-name">{{ mon.name || mon.name_en }}</div>
                <div class="act-hp-bar"><div class="act-hp-fill" :class="hpColor(mon)" :style="{width: hpPct(mon)}" /></div>
                <div class="act-hp-text">HP {{ mon.currentHp }}/{{ mon.maxHp }}</div>
              </div>
              <div class="act-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="bf-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>

            <!-- 招式/换人 切换 -->
            <div class="act-toggle">
              <button class="act-tog" :class="(selectedActions['action-slot-'+mon.fieldSlot]||'move')==='move'?'act-tog-on':''" @click="setSelectedAction(mon.fieldSlot,'move')">⚔️ 招式</button>
              <button class="act-tog" :class="selectedActions['action-slot-'+mon.fieldSlot]==='switch'?'act-tog-on':''" :disabled="!playerBenchOptions.length" @click="setSelectedAction(mon.fieldSlot,'switch')">🔄 换人</button>
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
                    <span v-if="mv.power" class="mv-stat">威力 {{ mv.power }}</span>
                    <span v-if="mv.accuracy && mv.accuracy < 100" class="mv-stat">命中 {{ mv.accuracy }}%</span>
                    <span class="mv-pp">PP {{ mv.currentPp!=null ? mv.currentPp+'/'+(mv.maxPp||mv.pp||'?') : '--' }}</span>
                    <span class="mv-type">{{ typeName(mv.type_id) }}</span>
                  </div>
                </button>
              </div>
              <!-- 目标选择 -->
              <div v-if="moveNeedsOpponentTarget(selMoveObj(mon)) && oppMons.length" class="tgt-row">
                <span class="tgt-label">选择目标 →</span>
                <button v-for="t in oppMons" :key="t.fieldSlot" type="button"
                  class="tgt-btn" :class="selectedTargets['target-slot-'+mon.fieldSlot]===t.fieldSlot?'tgt-on':''"
                  @click="setSelectedTarget(mon.fieldSlot, t.fieldSlot)">
                  {{ t.name || t.name_en }}
                </button>
              </div>
              <!-- 特殊系统 -->
              <div v-if="availableSpecialSystems(mon).length" class="sp-row">
                <button class="sp-btn" :class="!selectedSpecialSystems['special-slot-'+mon.fieldSlot]?'sp-on':''" @click="setSelectedSpecialSystem(mon.fieldSlot,undefined)">不发动</button>
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
                  <div class="sw-hp"><div class="sw-hp-bar" :style="{width:hpPct2(t),background:hpCol2(t)}" /></div>
                  <span class="sw-hp-num">{{ t.hp }}/{{ t.maxHp || '?' }}</span></div>
                </button>
              </div>
            </template>
          </div>

          <!-- 提交 -->
          <button class="sd-btn sd-btn-blue sd-btn-full" :disabled="!canSubmitMove||isBusy" @click="submitMove">
            {{ busyAction==='submit-move' ? '提交中...' : '✅ 提交回合' }}
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
              <div v-if="expRounds.has(ri)" class="log-evts"><div v-for="(e,ei) in r.events||[]" :key="ei" class="log-evt" :class="logEvtClass(e)">{{ e }}</div></div>
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
function selMoveObj(mon) {
  const moveName = selectedMoves.value[`slot-${mon.fieldSlot}`]
  return (mon?.moves || []).find((m) => (m.name_en || m.name) === moveName) || null
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
function logEvtClass(e) {
  if (/收回|派出|换人/.test(e)) return 'log-evt-switch'
  if (/造成了.*点伤害|伤害/.test(e)) return 'log-evt-damage'
  if (/回复|恢复|治愈/.test(e)) return 'log-evt-heal'
  if (/下降|降低/.test(e)) return 'log-evt-debuff'
  if (/展开了|场地/.test(e)) return 'log-evt-field'
  return ''
}
watch(() => summary.value?.rounds?.length, (n) => {
  if (!n) return
  const s = new Set(); for (let i = Math.max(0, n - 2); i < n; i++) s.add(i); expRounds.value = s
  nextTick(() => { if (logEl.value) logEl.value.scrollTop = logEl.value.scrollHeight })
})
</script>

<style>
/* ===== Pokemon Showdown 精致风格 ===== */
:root {
  --bg-page: #d8d8d0;
  --bg-panel: #ececec;
  --bg-control: #e4e4e4;
  --border: #b8b8b8;
  --border-light: #d0d0d0;
  --text: #333;
  --text-dim: #888;
  --accent: #488fce;
  --accent-dark: #3774af;
  --green: #5a9e3c;
  --green-dark: #4a8430;
  --red: #c44;
  --red-dark: #a33;
  --gold: #d4a017;
}

* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: var(--bg-page); color: var(--text); }
.sd-page { max-width: 956px; margin: 0 auto; padding: 8px; font-family: Verdana, Geneva, Tahoma, sans-serif; font-size: 10pt; }

/* ===== 开始面板 ===== */
.sd-start {
  background: linear-gradient(180deg, #f0f0f0, #e4e4e4);
  border: 1px solid var(--border);
  border-radius: 5px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.sd-format-row { display: flex; gap: 0; margin-bottom: 10px; }
.sd-fmt-btn {
  padding: 5px 14px; font-size: 9pt; font-family: inherit;
  border: 1px solid var(--border); border-right: 0;
  background: linear-gradient(180deg, #f8f8f8, #e8e8e8);
  color: #666; cursor: pointer; transition: all 0.12s;
}
.sd-fmt-btn:last-child { border-right: 1px solid var(--border); border-radius: 0 4px 4px 0; }
.sd-fmt-btn:first-child { border-radius: 4px 0 0 4px; }
.sd-fmt-btn:hover { background: linear-gradient(180deg, #fff, #f0f0f0); color: #333; }
.sd-fmt-on { background: linear-gradient(180deg, #5a9ee0, #3a7cc0) !important; color: #fff !important; border-color: #3a7cc0 !important; text-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.sd-btn-row { display: flex; flex-wrap: wrap; gap: 6px; }
.sd-btn {
  padding: 7px 14px; font-size: 9pt; font-family: inherit;
  border: 1px solid var(--border); border-radius: 5px; cursor: pointer;
  background: linear-gradient(180deg, #f8f8f8, #e4e4e4);
  color: var(--text); transition: all 0.12s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.sd-btn:hover { background: linear-gradient(180deg, #fff, #eee); box-shadow: 0 2px 4px rgba(0,0,0,0.1); transform: translateY(-1px); }
.sd-btn:active { transform: translateY(0); box-shadow: 0 1px 1px rgba(0,0,0,0.08); }
.sd-btn:disabled { background: #e8e8e8; color: #aaa; cursor: not-allowed; transform: none; box-shadow: none; }
.sd-btn-blue { background: linear-gradient(180deg, #5a9ee0, #3a7cc0); color: #fff; border-color: #3a7cc0; text-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.sd-btn-blue:hover { background: linear-gradient(180deg, #6aaef0, #4a8cd0); }
.sd-btn-purple { background: linear-gradient(180deg, #9090d0, #7070b0); color: #fff; border-color: #6060a0; }
.sd-btn-purple:hover { background: linear-gradient(180deg, #a0a0e0, #8080c0); }
.sd-btn-green { background: linear-gradient(180deg, #6a9e40, #588430); color: #fff; border-color: #4a7420; text-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.sd-btn-green:hover { background: linear-gradient(180deg, #7aae50, #689440); }
.sd-btn-red { background: linear-gradient(180deg, #d45, #b33); color: #fff; border-color: #a22; }
.sd-btn-red:hover { background: linear-gradient(180deg, #e56, #c44); }
.sd-btn-sm { padding: 4px 10px; font-size: 9pt; }
.sd-btn-full { width: 100%; margin-top: 8px; }
.sd-hint { margin-top: 8px; font-size: 9pt; color: var(--text-dim); }
.sd-error { padding: 8px 12px; background: #ffe5e0; border: 1px solid #c44; border-radius: 4px; color: #c44; font-size: 9pt; }
.sd-empty { padding: 16px; text-align: center; font-size: 9pt; color: var(--text-dim); }
.sd-factory-bar {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  background: linear-gradient(180deg, #f0f0f0, #e4e4e4);
  border: 1px solid var(--border); border-radius: 5px; font-size: 9pt;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.sd-bar {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  background: linear-gradient(180deg, #f0f0f0, #e4e4e4);
  border: 1px solid var(--border); border-top: 0; border-radius: 0 0 5px 5px; font-size: 9pt;
}

/* ===== 战场 ===== */
.bf {
  background: linear-gradient(180deg, #87b56b 0%, #6a9e50 30%, #588a44 60%, #4a7a3a 100%);
  border: 2px solid #3a5a2a;
  border-radius: 6px;
  width: 640px; height: 360px;
  overflow: hidden; position: relative;
  margin: 0 auto;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2), inset 0 2px 8px rgba(255,255,255,0.15);
}
.bf-row { display: flex; position: absolute; gap: 10px; }
.bf-opp-row { top: 24px; left: 24px; }
.bf-my-row { bottom: 24px; right: 24px; }
.bf-mon { display: flex; align-items: flex-start; gap: 8px; }
.bf-my-row .bf-mon { align-items: flex-end; flex-direction: row-reverse; }
.bf-info { min-width: 155px; }
.bf-name { font-size: 11pt; font-weight: bold; color: #222; text-shadow: #fff 1px 1px 0, #fff 1px -1px 0, #fff -1px 1px 0, #fff -1px -1px 0; }
.bf-lv { font-size: 9pt; font-weight: normal; color: #555; margin-left: 2px; }

/* Showdown 3D HP 条 */
.bf-hp { position: relative; border: 1px solid #666; background: #f8f8f8; padding: 1px; height: 10px; width: 155px; border-radius: 5px; margin: 3px 0; box-shadow: inset 0 1px 2px rgba(0,0,0,0.1); }
.bf-hp-bar {
  height: 5px; border-radius: 4px; transition: width 0.6s ease;
  background: linear-gradient(180deg, #44dd77, #22aa55);
  box-shadow: 0 1px 2px rgba(0,0,0,0.15), inset 0 1px 0 rgba(255,255,255,0.3);
}
.hp-l {
  background: linear-gradient(180deg, #ffe066, #ddaa22) !important;
  box-shadow: 0 1px 2px rgba(0,0,0,0.15), inset 0 1px 0 rgba(255,255,255,0.3);
}
.hp-c {
  background: linear-gradient(180deg, #ff6644, #dd3322) !important;
  box-shadow: 0 1px 2px rgba(0,0,0,0.15), inset 0 1px 0 rgba(255,255,255,0.2);
}
.hp-e { background: linear-gradient(180deg, #bbb, #999) !important; }

.bf-hp-num {
  position: absolute; background: linear-gradient(180deg, #666, #555);
  color: #eee; text-shadow: #000 0 1px 0; font-size: 9px;
  width: 36px; height: 14px; top: -2px; text-align: center;
  border-radius: 0 5px 5px 0; right: -37px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
  border: 1px solid #444;
}
.bf-my-row .bf-hp-num { right: auto; left: -37px; border-radius: 5px 0 0 5px; }

.bf-tags { display: flex; flex-wrap: wrap; gap: 2px; margin-top: 3px; }
.bf-tag { font-size: 8px; padding: 1px 3px; border-radius: 3px; color: #fff; font-weight: bold; box-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.bf-sprite-btn { background: none; border: none; cursor: default; padding: 0; }
.bf-sprite { width: 96px; height: 96px; object-fit: contain; image-rendering: pixelated; transition: all 0.3s; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); }
.bf-sprite-opp { animation: float 3.5s ease-in-out infinite; }
@keyframes float { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
.bf-sprite.fainted { filter: grayscale(1) brightness(0.3); opacity: 0.4; transform: translateY(20px) rotate(70deg); }
.bf-field { position: absolute; top: 6px; left: 50%; transform: translateX(-50%); display: flex; gap: 4px; flex-wrap: wrap; }
.bf-chip {
  font-size: 9px; font-weight: bold; padding: 2px 6px; border-radius: 3px;
  border: 1px solid; box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.chip-blue { background: #ddeeff; color: #246; border-color: #88bbee; }
.chip-red { background: #ffe0e0; color: #822; border-color: #eeaaaa; }
.chip-purple { background: #e8ddff; color: #446; border-color: #bbaadd; }
.chip-cyan { background: #ddffff; color: #145; border-color: #99ccee; }
.chip-amber { background: #fff3dd; color: #653; border-color: #ddbb88; }
.chip-orange { background: #ffe8dd; color: #642; border-color: #ddaa88; }
.chip-sky { background: #ddeeff; color: #236; border-color: #88aadd; }
.chip-yellow { background: #ffffdd; color: #553; border-color: #dddd88; }
.chip-green { background: #ddffdd; color: #262; border-color: #88cc88; }

/* ===== 面板 ===== */
.panel {
  background: linear-gradient(180deg, #f4f4f4, #e8e8e8);
  border: 1px solid var(--border); border-top: 0;
  padding: 10px 12px; font-size: 10pt;
  border-radius: 0 0 5px 5px;
}
.panel-hdr { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 2px solid var(--border-light); }
.panel-title { font-size: 11pt; font-weight: bold; color: #222; }
.panel-sub { font-size: 9pt; color: var(--text-dim); }

/* 队伍预览 */
.roster-row { margin-bottom: 10px; }
.roster-label { display: block; font-size: 9pt; font-weight: bold; margin-bottom: 5px; letter-spacing: 0.02em; }
.roster-label-red { color: #c44; } .roster-label-green { color: var(--green); }
.roster-cards { display: flex; flex-wrap: wrap; gap: 4px; }
.r-card {
  display: flex; flex-direction: column; align-items: center; padding: 5px 6px;
  border: 2px solid var(--border-light); border-radius: 5px;
  background: linear-gradient(180deg, #fff, #f4f4f4);
  cursor: pointer; min-width: 60px; transition: all 0.12s; position: relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.r-card:hover { border-color: #999; box-shadow: 0 2px 6px rgba(0,0,0,0.12); transform: translateY(-1px); }
.r-card-opp { cursor: default; opacity: 0.5; }
.r-card-opp:hover { transform: none; box-shadow: none; }
.r-picked { border-color: var(--green) !important; background: linear-gradient(180deg, #e8ffe0, #d4f5cc) !important; box-shadow: 0 2px 8px rgba(90,158,60,0.2); }
.r-dim { opacity: 0.3; }
.r-star { position: absolute; top: 1px; right: 3px; font-size: 10px; color: var(--gold); text-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.r-img { width: 42px; height: 32px; object-fit: contain; image-rendering: pixelated; }
.r-img-dim { filter: grayscale(0.7) brightness(0.4); }
.r-name { font-size: 8pt; color: #555; max-width: 54px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }
.r-types { font-size: 7px; color: #999; max-width: 54px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 补位/换人 */
.sw-grid { display: flex; flex-wrap: wrap; gap: 4px; }
.sw-btn {
  display: flex; align-items: center; gap: 6px; padding: 5px 10px;
  border: 2px solid var(--border-light); border-radius: 5px;
  background: linear-gradient(180deg, #fff, #f4f4f4);
  cursor: pointer; transition: all 0.12s; font-size: 9pt;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.sw-btn:hover { border-color: #999; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.sw-on { border-color: var(--accent) !important; background: linear-gradient(180deg, #e8f0ff, #d0e0ff) !important; }
.sw-img { width: 32px; height: 24px; object-fit: contain; image-rendering: pixelated; }
.sw-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.sw-name { font-size: 9pt; color: #333; font-weight: bold; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sw-hp { height: 5px; background: #ddd; border-radius: 3px; overflow: hidden; width: 64px; box-shadow: inset 0 1px 1px rgba(0,0,0,0.1); }
.sw-hp-bar { height: 100%; transition: width 0.3s; background: linear-gradient(180deg, #44dd77, #22aa55); border-radius: 3px; }
.sw-hp-num { font-size: 8pt; color: var(--text-dim); }

/* 操作区 */
.act-section {
  background: linear-gradient(180deg, #f8f8f8, #f0f0f0);
  border: 1px solid var(--border-light); border-radius: 5px;
  padding: 10px; margin-bottom: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.act-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; padding-bottom: 8px; border-bottom: 2px solid var(--border-light); }
.act-sprite { width: 44px; height: 33px; object-fit: contain; image-rendering: pixelated; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.15)); }
.act-sprite.fainted { filter: grayscale(1) brightness(0.3); opacity: 0.5; }
.act-info { flex: 1; min-width: 0; }
.act-name { font-size: 11pt; font-weight: bold; color: #222; }
.act-hp-bar { position: relative; border: 1px solid #666; background: #f8f8f8; padding: 1px; height: 10px; border-radius: 5px; margin: 3px 0; box-shadow: inset 0 1px 2px rgba(0,0,0,0.1); }
.act-hp-fill { height: 5px; border-radius: 4px; transition: width 0.4s; background: linear-gradient(180deg, #44dd77, #22aa55); box-shadow: 0 1px 2px rgba(0,0,0,0.1), inset 0 1px 0 rgba(255,255,255,0.3); }
.act-hp-text { font-size: 9pt; color: #555; }
.act-tags { display: flex; flex-wrap: wrap; gap: 2px; align-self: flex-start; }
.act-toggle { display: flex; gap: 0; margin-bottom: 8px; }
.act-tog {
  flex: 1; padding: 5px 10px; font-size: 10pt; font-family: inherit; font-weight: bold;
  border: 1px solid var(--border); background: linear-gradient(180deg, #f8f8f8, #e8e8e8);
  color: #555; cursor: pointer; transition: all 0.12s;
}
.act-tog:first-child { border-radius: 5px 0 0 5px; }
.act-tog:last-child { border-radius: 0 5px 5px 0; border-left: 0; }
.act-tog-on { background: linear-gradient(180deg, #5a9ee0, #3a7cc0) !important; color: #fff !important; border-color: #3a7cc0 !important; text-shadow: 0 1px 1px rgba(0,0,0,0.2); }
.act-tog:disabled { opacity: 0.3; cursor: not-allowed; }

/* 招式按钮 */
.mv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.mv-btn {
  display: flex; flex-direction: column; gap: 2px; padding: 6px 8px;
  border: 2px solid rgba(255,255,255,0.2); border-radius: 5px;
  background: var(--tc, #888); color: #fff; cursor: pointer; text-align: left;
  font-size: 10pt; font-family: inherit; font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  min-height: 44px; transition: all 0.12s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2), inset 0 1px 0 rgba(255,255,255,0.15);
}
.mv-btn:hover:not(:disabled) { filter: brightness(1.15); transform: translateY(-1px); box-shadow: 0 3px 8px rgba(0,0,0,0.3); }
.mv-btn:active:not(:disabled) { transform: translateY(0); }
.mv-sel { border-color: var(--gold) !important; box-shadow: 0 0 10px rgba(212,160,23,0.5), 0 2px 4px rgba(0,0,0,0.2); }
.mv-top { display: flex; justify-content: space-between; align-items: center; }
.mv-name { font-weight: bold; font-size: 11pt; }
.mv-tgt { font-size: 8px; background: rgba(0,0,0,0.3); padding: 1px 4px; border-radius: 3px; }
.mv-bot { display: flex; align-items: center; gap: 5px; font-size: 9px; opacity: 0.9; }
.mv-stat { font-weight: bold; }
.mv-pp { margin-left: auto; font-weight: normal; opacity: 0.7; }
.mv-type { background: rgba(0,0,0,0.3); padding: 1px 4px; border-radius: 3px; font-size: 8px; text-transform: uppercase; letter-spacing: 0.03em; }

/* 目标/特殊系统 */
.tgt-row { display: flex; align-items: center; gap: 5px; margin-top: 8px; }
.tgt-label { font-size: 9pt; color: #555; font-weight: bold; }
.tgt-btn {
  padding: 4px 10px; font-size: 9pt; font-family: inherit; font-weight: bold;
  border: 1px solid var(--border); border-radius: 4px;
  background: linear-gradient(180deg, #f8f8f8, #e8e8e8); color: #333; cursor: pointer;
  transition: all 0.1s;
}
.tgt-btn:hover { background: linear-gradient(180deg, #fff, #f0f0f0); }
.tgt-on { border-color: #c44; background: #ffe5e0; color: #c44; }
.sp-row { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.sp-btn {
  padding: 3px 10px; font-size: 9pt; font-family: inherit; font-weight: bold;
  border: 1px solid var(--border); border-radius: 4px;
  background: linear-gradient(180deg, #f8f8f8, #e8e8e8); color: #555; cursor: pointer;
  transition: all 0.1s;
}
.sp-btn:hover { background: linear-gradient(180deg, #fff, #f0f0f0); }
.sp-on { background: linear-gradient(180deg, #5a9ee0, #3a7cc0) !important; color: #fff !important; border-color: #3a7cc0 !important; }

/* ===== 日志 ===== */
.log {
  background: linear-gradient(180deg, #f0f0f0, #e4e4e4);
  border: 1px solid var(--border); border-top: 0;
  border-radius: 0 0 5px 5px;
}
.log-hdr {
  display: flex; justify-content: space-between; padding: 5px 10px;
  background: linear-gradient(180deg, #ddd, #d0d0d0);
  border-bottom: 1px solid var(--border); font-size: 9pt; font-weight: bold; color: #555;
}
.log-body { max-height: 220px; overflow-y: auto; padding: 4px 10px; }
.log-rhdr {
  display: flex; align-items: center; gap: 5px; padding: 3px 0;
  font-size: 9pt; font-weight: bold; color: #444; cursor: pointer;
  border-bottom: 1px solid transparent; transition: all 0.1s;
}
.log-rhdr:hover { background: #e8e8e8; border-bottom-color: #ddd; }
.log-arw { font-size: 8px; transition: transform 0.2s; color: #888; }
.log-arw.open { transform: rotate(90deg); }
.log-cnt { font-size: 8px; color: #999; background: #ddd; padding: 0 4px; border-radius: 3px; }
.log-evts { padding: 2px 0 6px 18px; }
.log-evt { font-size: 9pt; color: #333; padding: 2px 0; line-height: 1.5; border-bottom: 1px solid #e8e8e8; }
.log-evt:last-child { border-bottom: 0; }
.log-evt-switch { color: #2277bb; font-weight: bold; }
.log-evt-damage { color: #c44; }
.log-evt-heal { color: #393; }
.log-evt-debuff { color: #963; }
.log-evt-field { color: #66a; font-style: italic; }

/* 响应式 */
@media (max-width: 660px) {
  .bf { width: 100%; height: auto; min-height: 300px; }
  .bf-sprite { width: 72px; height: 72px; }
  .bf-hp { width: 120px; }
  .bf-hp-num { right: -30px; width: 28px; }
  .bf-my-row .bf-hp-num { left: -30px; }
}

/* 入场动画 */
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.bf { animation: fadeSlideIn 0.4s ease-out; }
.panel { animation: fadeSlideIn 0.3s ease-out; }
.log { animation: fadeSlideIn 0.3s ease-out 0.1s both; }

/* 招式按钮入场动画 */
.mv-btn { animation: fadeSlideIn 0.2s ease-out both; }
.mv-grid .mv-btn:nth-child(1) { animation-delay: 0.05s; }
.mv-grid .mv-btn:nth-child(2) { animation-delay: 0.1s; }
.mv-grid .mv-btn:nth-child(3) { animation-delay: 0.15s; }
.mv-grid .mv-btn:nth-child(4) { animation-delay: 0.2s; }
</style>
