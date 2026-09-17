<template>
  <div class="battle-page" :class="{ 'dark-mode': isDarkMode }">
    <!-- 主题切换按钮 -->
    <button class="theme-toggle" @click="toggleTheme" :title="isDarkMode ? '切换到浅色模式' : '切换到深色模式'">
      {{ isDarkMode ? '☀️' : '🌙' }}
    </button>

    <!-- ===== 无战斗时：开始面板 ===== -->
    <div v-if="!summary && !factoryRun" class="battle-start-card">
      <div class="battle-header">
        <div class="battle-title">
          <span class="battle-icon">⚔️</span>
          <h1>{{ t('对战工厂', 'Battle Factory') }}</h1>
        </div>
        <p class="battle-subtitle">{{ t('选择你的战斗模式，开始挑战！', 'Choose your battle mode and start challenging!') }}</p>
      </div>

      <div class="battle-formats">
        <button
          v-for="f in formats"
          :key="f.id"
          class="format-btn"
          :class="{ active: battleFormat === f.id }"
          @click="setBattleFormat(f.id)"
        >
          <span class="format-icon">{{ f.icon }}</span>
          <span class="format-label">{{ f.label }}</span>
        </button>
      </div>

      <div class="battle-actions">
        <button class="action-btn primary" :disabled="isBusy" @click="startBattle">
          <span class="btn-icon">⚔️</span>
          <span class="btn-text">{{ busyAction === 'start-manual' ? t('创建中...','Starting...') : t('手动对战','Manual Battle') }}</span>
          <span class="btn-loading" v-if="busyAction === 'start-manual'"></span>
        </button>
        
        <button v-if="isAuthenticated" class="action-btn secondary" :disabled="isBusy" @click="startFactoryChallenge">
          <span class="btn-icon">🏟️</span>
          <span class="btn-text">{{ busyAction === 'factory-start' ? t('创建中...','Starting...') : t('工厂挑战','Factory Run') }}</span>
          <span class="btn-loading" v-if="busyAction === 'factory-start'"></span>
        </button>
        
        <button v-if="isAuthenticated" class="action-btn tertiary" :disabled="isBusy" @click="startAsyncBattle">
          <span class="btn-icon">⏩</span>
          <span class="btn-text">{{ t('异步模拟','Async Sim') }}</span>
        </button>
      </div>

      <div v-if="!isAuthenticated" class="guest-notice">
        <span class="notice-icon">👤</span>
        <span class="notice-text">{{ t('游客模式：可直接手动对战', 'Guest mode: manual battle available') }}</span>
      </div>
    </div>

    <!-- ===== 工厂挑战：无当前战斗 ===== -->
    <div v-if="factoryRun && !summary" class="factory-progress">
      <div class="factory-info">
        <span class="factory-label">{{ t('工厂挑战','Factory Challenge') }}</span>
        <div class="factory-stats">
          <span class="stat wins">{{ factoryRun.wins||0 }}W</span>
          <span class="stat losses">{{ factoryRun.losses||0 }}L</span>
          <span class="stat progress">{{ factoryRun.current_battle||0 }}/{{ factoryRun.max_battles||9 }}</span>
        </div>
      </div>
      <div class="factory-actions">
        <button class="action-btn primary small" :disabled="isBusy" @click="nextFactoryBattle">
          {{ t('下一轮','Next Round') }}
        </button>
        <button class="action-btn danger small" :disabled="isBusy" @click="abandonFactoryRun">
          {{ t('放弃','Abandon') }}
        </button>
      </div>
    </div>

    <!-- ===== 错误提示 ===== -->
    <div v-if="requestError" class="error-banner">
      <span class="error-icon">⚠️</span>
      <span class="error-text">{{ requestError }}</span>
      <button class="error-dismiss" @click="requestError = null">×</button>
    </div>

    <!-- ===== 战斗界面 ===== -->
    <template v-if="summary">
      <!-- 战场 -->
      <div class="battlefield">
        <div class="battlefield-bg">
          <div class="battlefield-grass"></div>
          <div class="battlefield-sky"></div>
        </div>
        
        <!-- 对手区域 -->
        <div class="opponent-zone">
          <div v-for="(mon, i) in oppMons" :key="'o'+i" class="pokemon-slot opponent">
            <div class="pokemon-info">
              <div class="pokemon-name">
                <span class="name-text">{{ mon.name || mon.name_en }}</span>
                <span class="level-badge">L{{ mon.level }}</span>
              </div>
              <div class="hp-container">
                <div class="hp-bar">
                  <div class="hp-fill" :class="hpColor(mon)" :style="{width: hpPct(mon)}"></div>
                </div>
                <div class="hp-text">{{ mon.currentHp }}/{{ mon.maxHp }}</div>
              </div>
              <div class="status-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="status-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>
            <button class="pokemon-sprite-btn" @click="onOppClick(mon)" @contextmenu.prevent="showDetail(mon)">
              <img :src="sprite(mon, false)" class="pokemon-sprite opponent-sprite" 
                   :class="[mon.fainted?'fainted':'', getAnimClass('opp',i)]"
                   @error="imgErr($event,mon,false)">
            </button>
          </div>
        </div>

        <!-- 玩家区域 -->
        <div class="player-zone">
          <div v-for="(mon, i) in myMons" :key="'m'+i" class="pokemon-slot player">
            <button class="pokemon-sprite-btn" @contextmenu.prevent="showDetail(mon)">
              <img :src="sprite(mon, true)" class="pokemon-sprite player-sprite"
                   :class="[mon.fainted?'fainted':'', getAnimClass('player',i)]"
                   @error="imgErr($event,mon,true)">
            </button>
            <div class="pokemon-info">
              <div class="pokemon-name">
                <span class="name-text">{{ mon.name || mon.name_en }}</span>
                <span class="level-badge">L{{ mon.level }}</span>
              </div>
              <div class="hp-container">
                <div class="hp-bar">
                  <div class="hp-fill" :class="hpColor(mon)" :style="{width: hpPct(mon)}"></div>
                </div>
                <div class="hp-text">{{ mon.currentHp }}/{{ mon.maxHp }}</div>
              </div>
              <div class="status-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="status-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 场地效果 -->
        <div v-if="fieldChips.length" class="field-effects">
          <span v-for="c in fieldChips" :key="c.l" class="field-chip" :class="c.cls">{{ c.l }}</span>
        </div>
      </div>

      <!-- ===== 预览阶段 ===== -->
      <div v-if="isPreviewPhase" class="preview-panel">
        <div class="panel-header">
          <h2 class="panel-title">{{ t('队伍预览','Team Preview') }}</h2>
          <p class="panel-subtitle">{{ t('选{r}出战 · {l}首发 · 已选','Pick {r} · {l} leads · Selected',{r:rosterLimit,l:leadLimit}) }} {{ selectedRosterIndexes.length }}/{{ rosterLimit }} · {{ t('首发','Lead') }} {{ leadRosterIndexes.length }}/{{ leadLimit }}</p>
        </div>
        
        <div class="team-section">
          <h3 class="section-title opponent">{{ t('对手队伍','Opponent team') }}</h3>
          <div class="team-grid">
            <div v-for="(p,i) in opponentRoster" :key="'or'+i" class="pokemon-card opponent" @click="showDetail(p)">
              <img :src="spr(p)" class="card-sprite" @error="imgErr2($event,p)">
              <div class="card-info">
                <span class="card-name">{{ p.name || p.name_en }}</span>
                <span class="card-types">{{ (p.types||[]).map(t=>t.name||t.name_en).join('/') }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="team-section">
          <h3 class="section-title player">{{ t('你的队伍 · 点击选择，右键标记首发','Your team · click to pick, right-click for lead') }}</h3>
          <div class="team-grid">
            <button v-for="(p,i) in playerRoster" :key="'pr'+i" type="button"
                    class="pokemon-card player" :class="{'selected': isPicked(i), 'lead': isLead(i)}"
                    @click="toggleRoster(i)" @contextmenu.prevent="toggleLead(i)">
              <img :src="spr(p)" class="card-sprite" :class="{'dimmed': !isPicked(i)}" @error="imgErr2($event,p)">
              <div class="card-info">
                <span class="card-name">{{ p.name || p.name_en }}</span>
                <span v-if="isLead(i)" class="lead-star">★</span>
              </div>
            </button>
          </div>
        </div>
        
        <button class="action-btn primary full-width" :disabled="!canConfirmPreview||isBusy" @click="confirmPreview">
          {{ busyAction==='confirm-preview' ? t('确认中...','Confirming...') : t('确认出战','Confirm Team') }}
        </button>
      </div>

      <!-- ===== 补位阶段 ===== -->
      <div v-if="isReplacementPhase" class="replacement-panel">
        <div class="panel-header">
          <h2 class="panel-title">{{ t('补位','Replacement') }}</h2>
          <p class="panel-subtitle">{{ t('选{n}只上场','Choose {n} to send in',{n:pendingReplacementCount}) }}</p>
        </div>
        
        <div class="replacement-grid">
          <button v-for="o in replacementBenchOptions" :key="o.value" type="button"
                  class="replacement-btn" :class="{'selected': selectedReplacementIndexes.includes(o.value)}"
                  @click="toggleReplacement(o.value)">
            <img :src="spr(o.pokemon||o)" class="replacement-sprite" @error="imgErr2($event,o.pokemon||o)">
            <div class="replacement-info">
              <span class="replacement-name">{{ o.label }}</span>
              <div class="replacement-hp">
                <div class="hp-bar small">
                  <div class="hp-fill" :style="{width:hpPct2(o),background:hpCol2(o)}"></div>
                </div>
              </div>
            </div>
          </button>
        </div>
        
        <button class="action-btn danger full-width" :disabled="!canConfirmReplacement||isBusy" @click="confirmReplacement">
          {{ t('确认替补','Confirm') }}
        </button>
      </div>

      <!-- ===== 战斗操作阶段 ===== -->
      <div v-if="!isPreviewPhase && !isReplacementPhase" class="action-panel">
        <div v-if="myMons.length">
          <div v-for="mon in myMons" :key="'act'+mon.fieldSlot" class="action-section">
            <div class="action-header">
              <img :src="sprite(mon, true)" class="action-sprite" :class="{fainted:mon.fainted}" @error="imgErr($event,mon,true)">
              <div class="action-info">
                <div class="action-name">{{ mon.name || mon.name_en }}</div>
                <div class="action-hp">
                  <div class="hp-bar">
                    <div class="hp-fill" :class="hpColor(mon)" :style="{width: hpPct(mon)}"></div>
                  </div>
                  <div class="hp-text">HP {{ mon.currentHp }}/{{ mon.maxHp }}</div>
                </div>
              </div>
              <div class="action-tags">
                <span v-for="b in badges(mon)" :key="b.t" class="status-tag" :style="{background:b.c}">{{ b.t }}</span>
              </div>
            </div>
            
            <div class="action-toggle">
              <button class="toggle-btn" :class="{'active': (selectedActions['action-slot-'+mon.fieldSlot]||'move')==='move'}"
                      @click="setSelectedAction(mon.fieldSlot,'move')">
                ⚔️ {{ t('招式','Moves') }}
              </button>
              <button class="toggle-btn" :class="{'active': selectedActions['action-slot-'+mon.fieldSlot]==='switch'}"
                      :disabled="!playerBenchOptions.length" @click="setSelectedAction(mon.fieldSlot,'switch')">
                🔄 {{ t('换人','Switch') }}
              </button>
            </div>
            
            <template v-if="(selectedActions['action-slot-'+mon.fieldSlot]||'move')==='move'">
              <div class="moves-grid">
                <button v-for="(mv,mi) in mon.moves" :key="mv.name_en||mv.name" type="button"
                        class="move-btn" :class="{'selected': selectedMoves['slot-'+mon.fieldSlot]===(mv.name_en||mv.name)}"
                        :style="{'--type-color': typeCol(mv.type_id)}"
                        @click="setSelectedMove(mon.fieldSlot, mv.name_en||mv.name)">
                  <div class="move-header">
                    <span class="move-name">{{ mv.name || mv.name_en }}</span>
                    <span v-if="mv.target_id&&mv.target_id!==10" class="move-target">{{ tgtLabel(mv.target_id) }}</span>
                  </div>
                  <div class="move-stats">
                    <span v-if="mv.power" class="move-stat">威力 {{ mv.power }}</span>
                    <span v-if="mv.accuracy && mv.accuracy < 100" class="move-stat">命中 {{ mv.accuracy }}%</span>
                    <span class="move-pp">PP {{ mv.currentPp!=null ? mv.currentPp+'/'+(mv.maxPp||mv.pp||'?') : '--' }}</span>
                    <span class="move-type">{{ typeName(mv.type_id) }}</span>
                  </div>
                </button>
              </div>
              
              <div v-if="moveNeedsOpponentTarget(selMoveObj(mon)) && oppMons.length" class="target-selection">
                <span class="target-label">{{ t('选择目标 →','Select target →') }}</span>
                <button v-for="t in oppMons" :key="t.fieldSlot" type="button"
                        class="target-btn" :class="{'selected': selectedTargets['target-slot-'+mon.fieldSlot]===t.fieldSlot}"
                        @click="setSelectedTarget(mon.fieldSlot, t.fieldSlot)">
                  {{ t.name || t.name_en }}
                </button>
              </div>
              
              <div v-if="availableSpecialSystems(mon).length" class="special-systems">
                <button class="special-btn" :class="{'active': !selectedSpecialSystems['special-slot-'+mon.fieldSlot]}"
                        @click="setSelectedSpecialSystem(mon.fieldSlot,undefined)">
                  {{ t('不发动','None') }}
                </button>
                <button v-for="s in availableSpecialSystems(mon)" :key="s" class="special-btn"
                        :class="{'active': selectedSpecialSystems['special-slot-'+mon.fieldSlot]===s}"
                        @click="setSelectedSpecialSystem(mon.fieldSlot,s)">
                  {{ specialSystemLabel(s) }}<template v-if="s==='tera'"> · {{ teraTypeLabel(mon) }}</template>
                </button>
              </div>
            </template>
            
            <template v-else>
              <div class="switch-grid">
                <button v-for="bt in playerBenchOptions" :key="bt.value" type="button"
                        class="switch-btn" :class="{'selected': selectedSwitchTargets['switch-slot-'+mon.fieldSlot]===bt.value}"
                        @click="setSelectedSwitchTarget(mon.fieldSlot, bt.value)">
                  <img :src="spr(bt.pokemon||bt)" class="switch-sprite" @error="imgErr2($event,bt.pokemon||bt)">
                  <div class="switch-info">
                    <span class="switch-name">{{ bt.label }}</span>
                    <div class="switch-hp">
                      <div class="hp-bar small">
                        <div class="hp-fill" :style="{width:hpPct2(bt),background:hpCol2(bt)}"></div>
                      </div>
                    </div>
                    <span class="switch-hp-text">{{ bt.hp }}/{{ bt.maxHp || '?' }}</span>
                  </div>
                </button>
              </div>
            </template>
          </div>
          
          <button class="action-btn primary full-width" :disabled="!canSubmitMove||isBusy" @click="submitMove">
            {{ busyAction==='submit-move' ? t('提交中...','Submitting...') : '✅ ' + t('提交回合','End Turn') }}
          </button>
        </div>
        
        <div v-else class="empty-state">
          <span class="empty-icon">⚔️</span>
          <span class="empty-text">{{ t('开始对战后这里会显示招式选择','Start a battle to see move options') }}</span>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="battle-toolbar">
        <button v-if="summary.status!=='completed'" class="toolbar-btn" :disabled="isBusy" @click="refreshStatus">
          🔄 {{ t('刷新','Refresh') }}
        </button>
        <button v-if="summary.status==='running'" class="toolbar-btn danger" :disabled="isBusy" @click="forfeitBattle">
          {{ t('投降','Forfeit') }}
        </button>
        <button v-if="showContinueFactoryButton" class="toolbar-btn primary" :disabled="isBusy" @click="prepareNextFactoryStage">
          {{ t('下一轮','Next Round') }}
        </button>
        <button v-if="showResetBattleButton" class="toolbar-btn" :disabled="isBusy" @click="resetBattleState({keepFactoryRun:false})">
          {{ t('重置','Reset') }}
        </button>
      </div>

      <!-- ===== 数据可视化面板 ===== -->
      <div v-if="summary.status==='running' || summary.status==='completed'" class="stats-panel">
        <div class="panel-header">
          <h2 class="panel-title">📊 {{ t('对战统计','Battle Stats') }}</h2>
        </div>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">🔄</div>
            <div class="stat-label">{{ t('回合','Round') }}</div>
            <div class="stat-value">{{ summary.currentRound || 0 }}/{{ summary.roundLimit || 12 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">💚</div>
            <div class="stat-label">{{ t('我方存活','Your alive') }}</div>
            <div class="stat-value" :style="{color: summary.playerRemaining > 0 ? '#10b981' : '#ef4444'}">
              {{ summary.playerRemaining || 0 }}
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">❤️</div>
            <div class="stat-label">{{ t('对方存活','Foe alive') }}</div>
            <div class="stat-value" :style="{color: summary.opponentRemaining > 0 ? '#ef4444' : '#10b981'}">
              {{ summary.opponentRemaining || 0 }}
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">⚡</div>
            <div class="stat-label">{{ t('我方战力','Your power') }}</div>
            <div class="stat-value">{{ summary.playerStrength || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🔥</div>
            <div class="stat-label">{{ t('对方战力','Foe power') }}</div>
            <div class="stat-value">{{ summary.opponentStrength || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📋</div>
            <div class="stat-label">{{ t('格式','Format') }}</div>
            <div class="stat-value small">{{ summary.format || 'vgc-doubles' }}</div>
          </div>
        </div>
      </div>

      <!-- 日志 -->
      <div class="battle-log">
        <div class="log-header">
          <span class="log-title">{{ t('战斗日志','Battle Log') }}</span>
          <span v-if="summary.currentRound" class="log-round">{{ t('回合','Turn') }} {{ summary.currentRound }}</span>
        </div>
        <div ref="logEl" class="log-content">
          <template v-if="summary.rounds?.length">
            <div v-for="(r,ri) in summary.rounds" :key="ri" class="log-round-section">
              <div class="log-round-header" @click="togRound(ri)">
                <span class="log-arrow" :class="{'expanded': expRounds.has(ri)}">▶</span>
                <span class="log-round-title">{{ r.round===0?t('开场','Start'):t('第 {n} 回合','Turn {n}',{n:r.round}) }}</span>
                <span class="log-event-count">{{ (r.events||[]).length }}</span>
              </div>
              <div v-if="expRounds.has(ri)" class="log-events">
                <div v-for="(e,ei) in r.events||[]" :key="ei" class="log-event" :class="logEvtClass(e)">
                  {{ e }}
                </div>
              </div>
            </div>
          </template>
          <div v-else class="empty-state">
            <span class="empty-icon">⏳</span>
            <span class="empty-text">{{ t('等待战斗开始...','Waiting for battle...') }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 胜后交换面板 ===== -->
    <div v-if="showExchange && exchangeCandidates.length" class="exchange-panel">
      <div class="panel-header">
        <h2 class="panel-title">🎁 {{ t('胜利奖励：交换宝可梦','Victory reward: exchange Pokemon') }}</h2>
      </div>
      <div class="exchange-section">
        <h3 class="section-title">{{ t('可交换的宝可梦','Available to exchange') }}</h3>
        <div class="exchange-grid">
          <button v-for="(p,i) in exchangeCandidates" :key="'ex'+i" type="button"
                  class="exchange-card" :class="{'selected': replacedIndex===i}"
                  @click="replacedIndex = i">
            <img :src="spr(p)" class="exchange-sprite" @error="imgErr2($event,p)">
            <div class="exchange-info">
              <span class="exchange-name">{{ p.name || p.name_en }}</span>
              <span class="exchange-types">{{ (p.types||[]).map(t=>t.name||t.name_en).join('/') }}</span>
            </div>
          </button>
        </div>
      </div>
      <div class="exchange-actions">
        <button class="action-btn primary" :disabled="isBusy" @click="onConfirmExchange">
          {{ busyAction==='confirm-exchange' ? t('交换中...','Exchanging...') : t('确认交换','Confirm Exchange') }}
        </button>
        <button class="action-btn" @click="showExchange = false">
          {{ t('跳过','Skip') }}
        </button>
      </div>
    </div>

    <!-- ===== 结算面板 ===== -->
    <div v-if="settlement" class="settlement-panel">
      <div class="panel-header">
        <h2 class="panel-title">🏆 {{ t('挑战结算','Challenge Results') }}</h2>
      </div>
      <div class="settlement-content">
        <div v-if="settlement.won" class="result victory">
          <span class="result-icon">🎉</span>
          <span class="result-text">{{ t('胜利！','Victory!') }}</span>
        </div>
        <div v-else class="result defeat">
          <span class="result-icon">😢</span>
          <span class="result-text">{{ t('失败','Defeat') }}</span>
        </div>
        
        <div v-if="settlement.pointsDelta != null" class="points-change">
          <span class="points-label">{{ t('积分','Points') }}：</span>
          <span class="points-value" :style="{color: settlement.pointsDelta >= 0 ? '#10b981' : '#ef4444'}">
            {{ settlement.pointsDelta >= 0 ? '+' : '' }}{{ settlement.pointsDelta }}
          </span>
        </div>
        
        <div v-if="settlement.tierChange" class="tier-change">
          <span class="tier-label">{{ t('段位','Tier') }}：</span>
          <span class="tier-value">{{ settlement.newTierName }}</span>
        </div>
      </div>
      <div class="settlement-actions">
        <button v-if="factoryRun" class="action-btn primary" :disabled="isBusy" @click="prepareNextFactoryStage">
          {{ t('继续下一轮','Continue') }}
        </button>
        <button class="action-btn" @click="settlement = null">
          {{ t('关闭','Close') }}
        </button>
      </div>
    </div>

    <!-- ===== 排行榜 ===== -->
    <div v-if="showLeaderboard" class="leaderboard-panel">
      <div class="panel-header">
        <h2 class="panel-title">📊 {{ t('排行榜','Leaderboard') }}</h2>
        <button class="close-btn" @click="showLeaderboard = false">×</button>
      </div>
      <div v-if="leaderboardLoading" class="loading-state">
        <span class="loading-spinner"></span>
        <span class="loading-text">{{ t('加载中...','Loading...') }}</span>
      </div>
      <div v-else-if="leaderboardData.length" class="leaderboard-list">
        <div v-for="(entry, i) in leaderboardData" :key="i" class="leaderboard-item" :class="{'top-3': i < 3}">
          <span class="rank" :class="{'gold': i === 0, 'silver': i === 1, 'bronze': i === 2}">#{{ i + 1 }}</span>
          <span class="username">{{ entry.username || entry.name }}</span>
          <span class="points">{{ entry.totalPoints || entry.points || 0 }} pts</span>
        </div>
      </div>
      <div v-else class="empty-state">
        <span class="empty-icon">📊</span>
        <span class="empty-text">{{ t('暂无数据','No data') }}</span>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <PokemonDetailPopover v-model:visible="detailVis" :pokemon="detailMon" />
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useBattlePageState } from '../composables/useBattlePageState'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'
import { typeColor as typeColFn, typeNameZh, typeNameEn, getTypeEffectiveness, resolveTypeId } from '../services/typeChart'
import { normalizeFactoryRun } from '../services/contracts/battleContract'
import api from '../services/api'
import PokemonDetailPopover from '../components/PokemonDetailPopover.vue'

const { translate: tr, locale } = useLocale()
const t = (zh, en, params) => tr(zh, en, '', params || {})

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
  prepareNextFactoryStage, toggleLead, toggleReplacement, toggleRoster,
  leaderboardData, leaderboardLoading
} = useBattlePageState()

// 主题切换
const isDarkMode = ref(false)
function toggleTheme() {
  isDarkMode.value = !isDarkMode.value
  localStorage.setItem('battle-theme', isDarkMode.value ? 'dark' : 'light')
}

// 初始化主题
onMounted(() => {
  const savedTheme = localStorage.getItem('battle-theme')
  if (savedTheme === 'dark') {
    isDarkMode.value = true
  }
})

// ===== 格式选项 =====
const formats = [
  { id: 'vgc-doubles', label: t('双打 (64)', 'Doubles (64)'), icon: '👥' },
  { id: 'vgc63', label: t('63 单打', 'Singles (63)'), icon: '👤' },
  { id: 'gen9singles', label: t('9代单打', 'Gen9 Singles'), icon: '🎮' }
]

// ===== 特殊系统 =====
function teraTypeLabel(mon) { const tt = mon?.teraType || {}; return tt.name || tt.name_en || `Type ${tt.type_id || mon?.teraTypeId || '?'}` }
function specialSystemLabel(sys) { const m = { tera: t('太晶化','Terastallize'), mega: t('Mega进化','Mega Evolution'), 'z-move': t('Z招式','Z-Move'), dynamax: t('极巨化','Dynamax') }; return m[sys] || sys || '' }
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
const myMons = computed(() => { const s = summary.value?.playerActiveSlots || []; return s.map((ti, fs) => { const m = myTeam.value?.[ti]; return m ? { ...m, teamIndex: ti, fieldSlot: fs, maxHp: m?.stats?.hp || m?.currentHp || 0 } : null }).filter(Boolean) })
const oppMons = computed(() => { const s = summary.value?.opponentActiveSlots || []; return s.map((ti, fs) => { const m = oppTeam.value?.[ti]; return m ? { ...m, teamIndex: ti, fieldSlot: fs, maxHp: m?.stats?.hp || m?.currentHp || 0 } : null }).filter(Boolean) })

// ===== HP =====
function hpPct(mon) { if (!mon.maxHp) return mon.fainted ? '0%' : '100%'; return Math.max(0, Math.min(100, (mon.currentHp / mon.maxHp) * 100)) + '%' }
function hpColor(mon) { const p = mon.maxHp > 0 ? (mon.currentHp / mon.maxHp) * 100 : 100; if (p <= 0) return 'hp-empty'; if (p <= 20) return 'hp-critical'; if (p <= 50) return 'hp-low'; return 'hp-high' }
function hpPct2(o) { return o.maxHp > 0 ? Math.max(2, (o.hp / o.maxHp) * 100) + '%' : '100%' }
function hpCol2(o) { return o.maxHp > 0 && o.hp / o.maxHp <= 0.25 ? '#ef4444' : o.maxHp > 0 && o.hp / o.maxHp <= 0.5 ? '#f59e0b' : '#10b981' }

// ===== 状态 =====
const COND = { paralysis:'PAR', burn:'BRN', freeze:'FRZ', sleep:'SLP', poison:'PSN', toxic:'TOX', confusion:'CNF', taunt:'TNT' }
const CONDC = { paralysis:'#d97706', burn:'#dc2626', freeze:'#2563eb', sleep:'#7c3aed', poison:'#9333ea', toxic:'#9333ea', confusion:'#d97706', taunt:'#d97706' }
function badges(mon) {
  if (!mon || mon.fainted) return []
  const bs = []
  const c = mon.condition || mon.status
  if (c && CONDC[c]) bs.push({ t: COND[c] || c, c: CONDC[c] })
  if (mon.confused) bs.push({ t: 'CNF', c: CONDC.confusion })
  if ((mon.tauntTurns || 0) > 0) bs.push({ t: 'TNT', c: CONDC.taunt })
  if (mon.terastallized) bs.push({ t: 'Tera', c: '#8b5cf6' })
  const st = mon.statStages || {}
  for (const [k, n] of [['attack','Atk'],['specialAttack','SpA'],['defense','Def'],['specialDefense','SpD'],['speed','Spe']]) {
    const v = Number(st[k] || 0)
    if (v > 0) bs.push({ t: '+' + v + n, c: '#059669' })
    else if (v < 0) bs.push({ t: v + n, c: '#dc2626' })
  }
  return bs
}

// ===== 场地效果 =====
const fieldChips = computed(() => {
  const fe = summary.value?.fieldEffects || {}
  const cs = []
  const p = (l, k, cls) => { const v = Number(fe[k] || 0); if (v > 0) cs.push({ l: v > 1 ? `${l} ${v}T` : l, cls }) }
  p('TW', 'playerTailwindTurns', 'chip-blue'); p('TW', 'opponentTailwindTurns', 'chip-red')
  p('TR', 'trickRoomTurns', 'chip-purple'); p('Rain', 'rainTurns', 'chip-cyan')
  p('Sun', 'sunTurns', 'chip-amber'); p('Sand', 'sandTurns', 'chip-orange')
  p('Snow', 'snowTurns', 'chip-sky'); p('E-T', 'electricTerrainTurns', 'chip-yellow')
  p('P-T', 'psychicTerrainTurns', 'chip-purple'); p('G-T', 'grassyTerrainTurns', 'chip-green')
  return cs
})

// ===== Sprite =====
function sprite(mon, back) { const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id; if (!id) return sprites.default; return back ? sprites.pokemonBack(id) : sprites.pokemon(id) }
function spr(p) { const id = p?.form_id || p?.species_id || p?.pokemon_id || p?.id; return id ? sprites.pokemon(id) : sprites.default }
function imgErr(e, mon, back) { const id = mon?.form_id || mon?.species_id || mon?.id; e.target.src = back ? sprites.fallbackPokemonBack(id) : sprites.fallbackPokemon(id) }
function imgErr2(e, p) { const id = p?.form_id || p?.species_id || p?.id; e.target.src = sprites.fallbackPokemon(id) }

// ===== 招式 =====
function typeCol(id) { return typeColFn(id) }
function typeName(id) { return tr(typeNameZh(id), typeNameEn(id)) }
function tgtLabel(tid) { const m = {4:'Self',7:'Self',8:'Rnd',9:'All',11:'Foes',13:'Field',14:'All'}; return m[tid] || '' }
function selMoveObj(mon) { const n = selectedMoves.value[`slot-${mon.fieldSlot}`]; return (mon?.moves || []).find(m => (m.name_en || m.name) === n) || null }

// ===== 详情弹窗 =====
const detailVis = ref(false)
const detailMon = ref(null)
function showDetail(p) { detailMon.value = p; detailVis.value = true }
function onOppClick(mon) { /* target selection */ }

// ===== 日志 =====
const expRounds = ref(new Set())
const logEl = ref(null)
function togRound(i) { const s = new Set(expRounds.value); s.has(i) ? s.delete(i) : s.add(i); expRounds.value = s }
function logEvtClass(e) {
  if (/收回|派出|换人/.test(e)) return 'log-switch'
  if (/造成了.*点伤害|伤害/.test(e)) return 'log-damage'
  if (/回复|恢复|治愈/.test(e)) return 'log-heal'
  if (/下降|降低/.test(e)) return 'log-debuff'
  if (/展开了|场地/.test(e)) return 'log-field'
  return ''
}
watch(() => summary.value?.rounds?.length, (n) => {
  if (!n) return
  const s = new Set(); for (let i = Math.max(0, n - 2); i < n; i++) s.add(i); expRounds.value = s
  nextTick(() => { if (logEl.value) logEl.value.scrollTop = logEl.value.scrollHeight })
})

// ===== G11: 战斗动画 =====
const animClasses = ref({})
function applyAnim(key, cls, dur = 600) { animClasses.value = { ...animClasses.value, [key]: cls }; setTimeout(() => { const n = { ...animClasses.value }; delete n[key]; animClasses.value = n }, dur) }
function getAnimClass(side, slot) { return animClasses.value[`${side}-${slot}`] || '' }

// ===== 音效系统 =====
const SFX = {
  hit: 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=',
  heal: 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=',
  faint: 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA='
}
const sfxEnabled = ref(true)
function playSfx(name) {
  if (!sfxEnabled.value || !SFX[name]) return
  try { const a = new Audio(SFX[name]); a.volume = 0.3; a.play().catch(() => {}) } catch {}
}

// 监听回合变化触发动画+音效
watch(() => summary.value?.rounds?.length, (n, o) => {
  if (!n || n <= (o || 0)) return
  const latest = summary.value.rounds[n - 1]
  if (!latest?.events) return
  const text = latest.events.join(' ')
  const hasDmg = /伤害|damage|造成了/.test(text)
  const hasHeal = /回复|恢复|治愈|heal/.test(text)
  const hasFaint = /倒下了|fainted/.test(text)
  const opp = summary.value?.opponentActiveSlots || []
  const plr = summary.value?.playerActiveSlots || []
  if (hasDmg) {
    opp.forEach((_, i) => { if (Math.random() < 0.5) applyAnim(`opp-${i}`, 'anim-hit') })
    plr.forEach((_, i) => { if (Math.random() < 0.5) applyAnim(`player-${i}`, 'anim-hit') })
    playSfx('hit')
  }
  if (hasHeal) { plr.forEach((_, i) => applyAnim(`player-${i}`, 'anim-heal')); playSfx('heal') }
  if (hasFaint) { playSfx('faint') }
})
</script>

<style scoped>
/* ===== 现代化对战界面样式 ===== */
:root {
  --primary: #3b82f6;
  --primary-dark: #2563eb;
  --secondary: #8b5cf6;
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
  --info: #06b6d4;
  
  --bg-primary: #ffffff;
  --bg-secondary: #f8fafc;
  --bg-tertiary: #f1f5f9;
  --bg-dark: #1e293b;
  --bg-dark-secondary: #334155;
  
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-light: #94a3b8;
  --text-dark: #f8fafc;
  
  --border-light: #e2e8f0;
  --border-dark: #475569;
  
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  
  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
  --radius-xl: 1rem;
  
  --transition-fast: 150ms ease;
  --transition-normal: 200ms ease;
  --transition-slow: 300ms ease;
}

/* 深色模式 */
.dark-mode {
  --bg-primary: #1e293b;
  --bg-secondary: #334155;
  --bg-tertiary: #475569;
  --text-primary: #f8fafc;
  --text-secondary: #94a3b8;
  --text-light: #64748b;
  --border-light: #475569;
  --border-dark: #64748b;
}

.battle-page {
  min-height: 100vh;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  transition: background-color var(--transition-slow), color var(--transition-slow);
  position: relative;
}

/* 主题切换按钮 */
.theme-toggle {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-normal);
}

.theme-toggle:hover {
  transform: scale(1.1);
  box-shadow: var(--shadow-lg);
}

/* 开始面板 */
.battle-start-card {
  max-width: 48rem;
  margin: 2rem auto;
  background: var(--bg-primary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  padding: 2rem;
  animation: slideUp 0.5s ease-out;
}

.battle-header {
  text-align: center;
  margin-bottom: 2rem;
}

.battle-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.battle-icon {
  font-size: 2rem;
}

.battle-title h1 {
  font-size: 1.875rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.battle-subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
}

/* 格式选择 */
.battle-formats {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 2rem;
  justify-content: center;
}

.format-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 120px;
}

.format-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.format-btn.active {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  border-color: var(--primary);
  color: white;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
}

.format-icon {
  font-size: 1.5rem;
}

.format-label {
  font-size: 0.875rem;
  font-weight: 600;
}

/* 操作按钮 */
.battle-actions {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.action-btn:hover::before {
  left: 100%;
}

.action-btn.primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: white;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
}

.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.action-btn.secondary {
  background: linear-gradient(135deg, var(--secondary), #7c3aed);
  color: white;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
}

.action-btn.secondary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(139, 92, 246, 0.5);
}

.action-btn.tertiary {
  background: linear-gradient(135deg, var(--info), #0891b2);
  color: white;
  box-shadow: 0 4px 14px rgba(6, 182, 212, 0.4);
}

.action-btn.tertiary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(6, 182, 212, 0.5);
}

.action-btn.danger {
  background: linear-gradient(135deg, var(--danger), #dc2626);
  color: white;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4);
}

.action-btn.danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.5);
}

.action-btn.small {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
}

.action-btn.full-width {
  width: 100%;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.btn-icon {
  font-size: 1.25rem;
}

.btn-text {
  flex: 1;
}

.btn-loading {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 游客提示 */
.guest-notice {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  border-left: 4px solid var(--info);
}

.notice-icon {
  font-size: 1.25rem;
}

.notice-text {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

/* 工厂进度 */
.factory-progress {
  max-width: 48rem;
  margin: 2rem auto;
  background: var(--bg-primary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  animation: slideUp 0.3s ease-out;
}

.factory-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.factory-label {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.factory-stats {
  display: flex;
  gap: 1rem;
}

.stat {
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
}

.stat.wins {
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
}

.stat.losses {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger);
}

.stat.progress {
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary);
}

.factory-actions {
  display: flex;
  gap: 0.75rem;
}

/* 错误横幅 */
.error-banner {
  max-width: 48rem;
  margin: 1rem auto;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid var(--danger);
  border-radius: var(--radius-lg);
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  animation: slideDown 0.3s ease-out;
}

.error-icon {
  font-size: 1.25rem;
}

.error-text {
  flex: 1;
  color: var(--danger);
  font-size: 0.875rem;
}

.error-dismiss {
  background: none;
  border: none;
  color: var(--danger);
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-fast);
}

.error-dismiss:hover {
  background: rgba(239, 68, 68, 0.2);
}

/* 战场 */
.battlefield {
  max-width: 640px;
  margin: 2rem auto;
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
  animation: fadeIn 0.5s ease-out;
}

.battlefield-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #87CEEB 0%, #98FB98 100%);
}

.battlefield-grass {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40%;
  background: linear-gradient(180deg, #4ade80 0%, #22c55e 100%);
  border-top: 2px solid #16a34a;
}

.battlefield-sky {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60%;
  background: linear-gradient(180deg, #87CEEB 0%, #B0E0E6 100%);
}

.opponent-zone,
.player-zone {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-around;
  padding: 1rem;
}

.opponent-zone {
  padding-top: 2rem;
}

.player-zone {
  padding-bottom: 2rem;
}

.pokemon-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-md);
  transition: transform var(--transition-normal);
  min-width: 180px;
}

.pokemon-slot:hover {
  transform: translateY(-4px);
}

.pokemon-slot.opponent {
  border: 2px solid rgba(239, 68, 68, 0.3);
}

.pokemon-slot.player {
  border: 2px solid rgba(16, 185, 129, 0.3);
}

.pokemon-info {
  text-align: center;
  width: 100%;
}

.pokemon-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.name-text {
  font-weight: 600;
  font-size: 1rem;
  color: var(--text-primary);
}

.level-badge {
  background: var(--primary);
  color: white;
  padding: 0.125rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 600;
}

.hp-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.hp-bar {
  flex: 1;
  height: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-full);
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.hp-bar.small {
  height: 0.5rem;
}

.hp-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.5s ease;
  position: relative;
}

.hp-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.3), transparent);
  border-radius: var(--radius-full) var(--radius-full) 0 0;
}

.hp-fill.hp-high {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.hp-fill.hp-low {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.hp-fill.hp-critical {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.hp-fill.hp-empty {
  background: #94a3b8;
}

.hp-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 3rem;
  text-align: right;
}

.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  justify-content: center;
}

.status-tag {
  padding: 0.125rem 0.375rem;
  border-radius: var(--radius-sm);
  font-size: 0.625rem;
  font-weight: 700;
  color: white;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.pokemon-sprite-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: transform var(--transition-normal);
}

.pokemon-sprite-btn:hover {
  transform: scale(1.05);
}

.pokemon-sprite {
  width: 96px;
  height: 96px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  transition: all var(--transition-normal);
}

.pokemon-sprite:hover {
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.3)) brightness(1.1);
}

.pokemon-sprite.opponent-sprite {
  animation: float 3s ease-in-out infinite;
}

.pokemon-sprite.player-sprite {
  animation: float 3s ease-in-out infinite reverse;
}

.pokemon-sprite.fainted {
  filter: grayscale(1) brightness(0.3);
  opacity: 0.4;
  transform: translateY(20px) rotate(70deg);
}

/* 场地效果 */
.field-effects {
  position: absolute;
  top: 0.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  z-index: 2;
}

.field-chip {
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  backdrop-filter: blur(10px);
  box-shadow: var(--shadow-sm);
}

.chip-blue {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.chip-red {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.chip-purple {
  background: rgba(139, 92, 246, 0.2);
  color: #8b5cf6;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.chip-cyan {
  background: rgba(6, 182, 212, 0.2);
  color: #06b6d4;
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.chip-amber {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.chip-orange {
  background: rgba(249, 115, 22, 0.2);
  color: #f97316;
  border: 1px solid rgba(249, 115, 22, 0.3);
}

.chip-sky {
  background: rgba(14, 165, 233, 0.2);
  color: #0ea5e9;
  border: 1px solid rgba(14, 165, 233, 0.3);
}

.chip-yellow {
  background: rgba(234, 179, 8, 0.2);
  color: #eab308;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.chip-green {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

/* 面板通用样式 */
.preview-panel,
.replacement-panel,
.action-panel,
.stats-panel,
.battle-log,
.exchange-panel,
.settlement-panel,
.leaderboard-panel {
  max-width: 48rem;
  margin: 1rem auto;
  background: var(--bg-primary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: slideUp 0.3s ease-out;
}

.panel-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-subtitle {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-top: 0.25rem;
}

/* 队伍预览 */
.team-section {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-light);
}

.team-section:last-child {
  border-bottom: none;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-title.opponent {
  color: var(--danger);
}

.section-title.player {
  color: var(--success);
}

.team-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.pokemon-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 80px;
}

.pokemon-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.pokemon-card.opponent {
  opacity: 0.7;
  cursor: default;
}

.pokemon-card.opponent:hover {
  transform: none;
  box-shadow: none;
}

.pokemon-card.player.selected {
  border-color: var(--success);
  background: rgba(16, 185, 129, 0.1);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.pokemon-card.player.lead {
  border-color: var(--warning);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
}

.card-sprite {
  width: 48px;
  height: 48px;
  object-fit: contain;
  image-rendering: pixelated;
}

.card-sprite.dimmed {
  filter: grayscale(0.7) brightness(0.4);
}

.card-info {
  text-align: center;
}

.card-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
}

.card-types {
  font-size: 0.625rem;
  color: var(--text-secondary);
  display: block;
  margin-top: 0.25rem;
}

.lead-star {
  color: var(--warning);
  font-size: 1rem;
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
}

/* 补位界面 */
.replacement-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1.5rem;
}

.replacement-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 160px;
}

.replacement-btn:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
}

.replacement-btn.selected {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.replacement-sprite {
  width: 40px;
  height: 40px;
  object-fit: contain;
  image-rendering: pixelated;
}

.replacement-info {
  flex: 1;
  min-width: 0;
}

.replacement-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.replacement-hp {
  margin-top: 0.25rem;
}

/* 操作界面 */
.action-section {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-light);
}

.action-section:last-child {
  border-bottom: none;
}

.action-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-light);
}

.action-sprite {
  width: 56px;
  height: 56px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.action-sprite.fainted {
  filter: grayscale(1) brightness(0.3);
  opacity: 0.5;
}

.action-info {
  flex: 1;
  min-width: 0;
}

.action-name {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.action-hp {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  align-self: flex-start;
}

/* 操作切换 */
.action-toggle {
  display: flex;
  gap: 0;
  margin-bottom: 1rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: 0.25rem;
}

.toggle-btn {
  flex: 1;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.toggle-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.5);
}

.toggle-btn.active {
  background: var(--primary);
  color: white;
  box-shadow: var(--shadow-sm);
}

.toggle-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 招式网格 */
.moves-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.move-btn {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--type-color, var(--bg-tertiary));
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  text-align: left;
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
}

.move-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent);
  pointer-events: none;
}

.move-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.4);
}

.move-btn.selected {
  border-color: var(--warning);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.5), 0 4px 12px rgba(0, 0, 0, 0.3);
}

.move-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.move-name {
  font-size: 1rem;
}

.move-target {
  font-size: 0.625rem;
  background: rgba(0, 0, 0, 0.3);
  padding: 0.125rem 0.375rem;
  border-radius: var(--radius-sm);
}

.move-stats {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  opacity: 0.9;
}

.move-stat {
  font-weight: 600;
}

.move-pp {
  margin-left: auto;
  opacity: 0.7;
}

.move-type {
  background: rgba(0, 0, 0, 0.3);
  padding: 0.125rem 0.375rem;
  border-radius: var(--radius-sm);
  font-size: 0.625rem;
  text-transform: uppercase;
}

/* 目标选择 */
.target-selection {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
}

.target-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.target-btn {
  padding: 0.5rem 1rem;
  background: var(--bg-primary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.target-btn:hover {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.1);
}

.target-btn.selected {
  border-color: var(--danger);
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger);
}

/* 特殊系统 */
.special-systems {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.special-btn {
  padding: 0.5rem 1rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.special-btn:hover {
  border-color: var(--secondary);
  background: rgba(139, 92, 246, 0.1);
  color: var(--secondary);
}

.special-btn.active {
  border-color: var(--secondary);
  background: var(--secondary);
  color: white;
}

/* 换人界面 */
.switch-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 160px;
}

.switch-btn:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
}

.switch-btn.selected {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.1);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.switch-sprite {
  width: 40px;
  height: 40px;
  object-fit: contain;
  image-rendering: pixelated;
}

.switch-info {
  flex: 1;
  min-width: 0;
}

.switch-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.switch-hp {
  margin-top: 0.25rem;
}

.switch-hp-text {
  font-size: 0.75rem;
  color: var(--text-secondary);
  display: block;
  margin-top: 0.25rem;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-text {
  font-size: 1rem;
}

/* 工具栏 */
.battle-toolbar {
  max-width: 48rem;
  margin: 1rem auto;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 0.75rem;
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.toolbar-btn {
  padding: 0.5rem 1rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.toolbar-btn:hover {
  background: var(--bg-secondary);
  border-color: var(--border-dark);
  color: var(--text-primary);
}

.toolbar-btn.primary {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.toolbar-btn.primary:hover {
  background: var(--primary-dark);
}

.toolbar-btn.danger {
  background: var(--danger);
  border-color: var(--danger);
  color: white;
}

.toolbar-btn.danger:hover {
  background: #dc2626;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 统计面板 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  padding: 1.5rem;
}

.stat-card {
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: 1rem;
  text-align: center;
  transition: transform var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-value.small {
  font-size: 0.875rem;
}

/* 战斗日志 */
.log-header {
  padding: 1rem 1.5rem;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.log-round {
  font-size: 0.875rem;
  color: var(--text-secondary);
  background: var(--bg-primary);
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-full);
}

.log-content {
  max-height: 240px;
  overflow-y: auto;
  padding: 1rem;
}

.log-round-section {
  margin-bottom: 0.5rem;
}

.log-round-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.log-round-header:hover {
  background: var(--bg-secondary);
}

.log-arrow {
  font-size: 0.75rem;
  color: var(--text-secondary);
  transition: transform var(--transition-fast);
}

.log-arrow.expanded {
  transform: rotate(90deg);
}

.log-round-title {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.log-event-count {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--bg-primary);
  padding: 0.125rem 0.5rem;
  border-radius: var(--radius-full);
}

.log-events {
  padding: 0.5rem 0 0.5rem 1.5rem;
}

.log-event {
  font-size: 0.875rem;
  color: var(--text-secondary);
  padding: 0.25rem 0;
  border-bottom: 1px solid var(--border-light);
  line-height: 1.5;
}

.log-event:last-child {
  border-bottom: none;
}

.log-switch {
  color: var(--primary);
  font-weight: 600;
}

.log-damage {
  color: var(--danger);
}

.log-heal {
  color: var(--success);
}

.log-debuff {
  color: var(--warning);
}

.log-field {
  color: var(--secondary);
  font-style: italic;
}

/* 交换面板 */
.exchange-section {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-light);
}

.exchange-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.exchange-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  min-width: 100px;
}

.exchange-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.exchange-card.selected {
  border-color: var(--success);
  background: rgba(16, 185, 129, 0.1);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.exchange-sprite {
  width: 56px;
  height: 56px;
  object-fit: contain;
  image-rendering: pixelated;
}

.exchange-info {
  text-align: center;
}

.exchange-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
}

.exchange-types {
  font-size: 0.75rem;
  color: var(--text-secondary);
  display: block;
  margin-top: 0.25rem;
}

.exchange-actions {
  padding: 1.5rem;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

/* 结算面板 */
.settlement-content {
  padding: 2rem;
  text-align: center;
}

.result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.result-icon {
  font-size: 4rem;
}

.result-text {
  font-size: 2rem;
  font-weight: 700;
}

.result.victory .result-text {
  color: var(--success);
}

.result.defeat .result-text {
  color: var(--danger);
}

.points-change,
.tier-change {
  font-size: 1.125rem;
  margin-bottom: 0.75rem;
}

.points-label,
.tier-label {
  color: var(--text-secondary);
}

.points-value {
  font-weight: 700;
}

.tier-value {
  font-weight: 700;
  color: var(--primary);
}

.settlement-actions {
  padding: 1.5rem;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

/* 排行榜 */
.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 1rem;
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--border-light);
  border-top: 3px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  color: var(--text-secondary);
  font-size: 1rem;
}

.leaderboard-list {
  padding: 1rem;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border-light);
  transition: background-color var(--transition-fast);
}

.leaderboard-item:hover {
  background: var(--bg-tertiary);
}

.leaderboard-item:last-child {
  border-bottom: none;
}

.leaderboard-item.top-3 {
  background: rgba(245, 158, 11, 0.05);
}

.rank {
  font-weight: 700;
  min-width: 2rem;
  color: var(--text-secondary);
}

.rank.gold {
  color: #f59e0b;
}

.rank.silver {
  color: #94a3b8;
}

.rank.bronze {
  color: #d97706;
}

.username {
  flex: 1;
  font-weight: 600;
  color: var(--text-primary);
}

.points {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

/* 动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
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

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.anim-hit {
  animation: hit 0.4s ease-out;
}

@keyframes hit {
  0% {
    filter: brightness(1);
    transform: translateX(0);
  }
  20% {
    filter: brightness(2.5);
    transform: translateX(-8px);
  }
  40% {
    filter: brightness(1.2);
    transform: translateX(8px);
  }
  60% {
    filter: brightness(2);
    transform: translateX(-4px);
  }
  100% {
    filter: brightness(1);
    transform: translateX(0);
  }
}

.anim-heal {
  animation: heal 0.5s ease-out;
}

@keyframes heal {
  0% {
    filter: brightness(1);
  }
  40% {
    filter: brightness(1.4) hue-rotate(90deg);
  }
  100% {
    filter: brightness(1) hue-rotate(0deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .battle-start-card {
    margin: 1rem;
    padding: 1.5rem;
  }
  
  .battle-formats {
    flex-direction: column;
    align-items: center;
  }
  
  .format-btn {
    width: 100%;
    max-width: 200px;
  }
  
  .battlefield {
    margin: 1rem;
    border-radius: var(--radius-lg);
  }
  
  .pokemon-slot {
    min-width: 140px;
    padding: 0.75rem;
  }
  
  .pokemon-sprite {
    width: 72px;
    height: 72px;
  }
  
  .moves-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .team-grid {
    justify-content: center;
  }
  
  .pokemon-card {
    min-width: 70px;
  }
  
  .card-sprite {
    width: 40px;
    height: 40px;
  }
  
  .factory-progress {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }
  
  .factory-actions {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .battle-title h1 {
    font-size: 1.5rem;
  }
  
  .battle-subtitle {
    font-size: 0.875rem;
  }
  
  .action-btn {
    padding: 0.75rem 1rem;
    font-size: 0.875rem;
  }
  
  .pokemon-slot {
    min-width: 120px;
  }
  
  .pokemon-sprite {
    width: 56px;
    height: 56px;
  }
  
  .name-text {
    font-size: 0.875rem;
  }
  
  .level-badge {
    font-size: 0.625rem;
    padding: 0.0625rem 0.375rem;
  }
  
  .hp-text {
    font-size: 0.625rem;
  }
  
  .status-tag {
    font-size: 0.5rem;
    padding: 0.0625rem 0.25rem;
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
}

::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: var(--radius-sm);
}

::-webkit-scrollbar-thumb:hover {
  background: var(--border-dark);
}

/* 选择样式 */
::selection {
  background: rgba(59, 130, 246, 0.3);
  color: var(--text-primary);
}

/* 焦点样式 */
:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

/* 过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all var(--transition-normal);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>