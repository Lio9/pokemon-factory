<template>
  <!-- Showdown 风格决策面板：招式 + 换人 + 确认 -->
  <div class="sd-panel">

    <!-- ===== 预览阶段 ===== -->
    <section v-if="isPreviewPhase" class="sd-preview">
      <div class="sd-preview-header">
        <span class="sd-preview-title">{{ tr('队伍预览', 'Team Preview') }}</span>
        <span class="sd-preview-info">
          {{ tr(`选 ${rosterLimit} 出战，${leadLimit} 首发`, `Pick ${rosterLimit}, ${leadLimit} lead(s)`) }}
          · {{ selectedRosterIndexes.length }}/{{ rosterLimit }} {{ tr('已选', 'picked') }}
          · {{ leadRosterIndexes.length }}/{{ leadLimit }} {{ tr('首发', 'lead') }}
        </span>
      </div>

      <!-- 对手队伍 -->
      <div v-if="opponentRoster.length" class="sd-roster-row">
        <span class="sd-roster-label sd-roster-opp">{{ tr('对手', 'Foe') }}</span>
        <div class="sd-roster-cards">
          <div
            v-for="(pokemon, i) in opponentRoster"
            :key="`opp-${i}`"
            class="sd-roster-card sd-roster-card-opp"
            @click="openDetail(pokemon)"
          >
            <img :src="previewSprite(pokemon)" :alt="pokemon.name" class="sd-roster-img" @error="onSpriteError($event, pokemon)">
            <span class="sd-roster-name">{{ pokemon.name || pokemon.name_en }}</span>
          </div>
        </div>
      </div>

      <!-- 我方队伍 -->
      <div class="sd-roster-row">
        <span class="sd-roster-label sd-roster-player">{{ tr('你的队伍', 'Your team') }}</span>
        <div class="sd-roster-cards">
          <button
            v-for="(pokemon, i) in playerRoster"
            :key="`plr-${i}`"
            type="button"
            class="sd-roster-card"
            :class="[isPicked(i) ? 'sd-picked' : 'sd-unpicked', isLead(i) ? 'sd-lead' : '']"
            @click="toggleRoster(i)"
            @contextmenu.prevent="toggleLead(i)"
          >
            <img :src="previewSprite(pokemon)" :alt="pokemon.name" class="sd-roster-img" :class="isPicked(i) ? '' : 'sd-img-dim'" @error="onSpriteError($event, pokemon)">
            <span class="sd-roster-name">{{ pokemon.name || pokemon.name_en }}</span>
            <span v-if="isLead(i)" class="sd-lead-mark">★</span>
          </button>
        </div>
      </div>

      <!-- 确认按钮 -->
      <button
        class="sd-confirm-btn"
        :disabled="!canConfirmPreview || isBusy"
        @click="confirmPreview"
      >
        {{ busyAction === 'confirm-preview' ? tr('确认中...', 'Confirming...') : tr('确认出战', 'Confirm Team') }}
      </button>
    </section>

    <!-- ===== 补位阶段 ===== -->
    <section v-if="isReplacementPhase" class="sd-replacement">
      <div class="sd-replacement-header">
        {{ tr('补位：选 {count} 只后备上场', 'Choose {count} replacements', { count: pendingReplacementCount }) }}
      </div>
      <div class="sd-replacement-options">
        <button
          v-for="opt in replacementBenchOptions"
          :key="opt.value"
          type="button"
          class="sd-replacement-btn"
          :class="selectedReplacementIndexes.includes(opt.value) ? 'sd-replacement-selected' : ''"
          @click="toggleReplacement(opt.value)"
        >
          <img :src="previewSprite(opt.pokemon || opt)" :alt="opt.label" class="sd-replacement-img" @error="onSpriteError($event, opt.pokemon || opt)">
          <div class="sd-replacement-info">
            <span class="sd-replacement-name">{{ opt.label }}</span>
            <div class="sd-replacement-hp-bar">
              <div class="sd-replacement-hp-fill" :style="{ width: opt.maxHp > 0 ? Math.max(2, (opt.hp / opt.maxHp) * 100) + '%' : '100%', background: opt.maxHp > 0 && opt.hp / opt.maxHp <= 0.25 ? '#ef4444' : opt.maxHp > 0 && opt.hp / opt.maxHp <= 0.5 ? '#fbbf24' : '#4ade80' }" />
            </div>
            <span class="sd-replacement-hp-text">{{ opt.hp }}/{{ opt.maxHp || '?' }}</span>
          </div>
        </button>
      </div>
      <button
        class="sd-confirm-btn"
        :disabled="!canConfirmReplacement || isBusy"
        @click="confirmReplacement"
      >
        {{ tr('确认替补', 'Confirm') }}
      </button>
    </section>

    <!-- ===== 战斗操作阶段 ===== -->
    <section v-if="!isPreviewPhase && !isReplacementPhase" class="sd-battle">
      <div v-if="playerActiveMons.length" class="sd-battle-grid">
        <!-- 每只在场宝可梦的招式/换人 -->
        <div
          v-for="mon in playerActiveMons"
          :key="mon.fieldSlot"
          class="sd-mon-section"
        >
          <!-- 操作切换 -->
          <div class="sd-action-toggle">
            <button
              class="sd-toggle-btn"
              :class="(selectedActions[`action-slot-${mon.fieldSlot}`] || 'move') === 'move' ? 'sd-toggle-active' : ''"
              @click="setSelectedAction(mon.fieldSlot, 'move')"
            >{{ tr('招式', 'Moves') }}</button>
            <button
              class="sd-toggle-btn"
              :class="selectedActions[`action-slot-${mon.fieldSlot}`] === 'switch' ? 'sd-toggle-active' : ''"
              :disabled="!playerBenchOptions.length"
              @click="setSelectedAction(mon.fieldSlot, 'switch')"
            >{{ tr('换人', 'Switch') }}</button>
          </div>

          <!-- 招式面板 -->
          <template v-if="(selectedActions[`action-slot-${mon.fieldSlot}`] || 'move') === 'move'">
            <div class="sd-moves-grid">
              <MoveButton
                v-for="(move, mi) in mon.moves"
                :key="move.name_en || move.name"
                :move="move"
                :move-index="mi"
                :selected="selectedMoves[`slot-${mon.fieldSlot}`] === (move.name_en || move.name)"
                :effectiveness="moveEffectivenessFor(mon, move)"
                @select="setSelectedMove(mon.fieldSlot, move.name_en || move.name)"
              />
            </div>
            <!-- 目标选择 -->
            <div v-if="moveNeedsOpponentTarget(selectedMoveObject(mon)) && opponentActiveMons.length" class="sd-target-row">
              <span class="sd-target-label">{{ tr('目标:', 'Target:') }}</span>
              <button
                v-for="t in opponentActiveMons"
                :key="`t-${t.fieldSlot}`"
                type="button"
                class="sd-target-btn"
                :class="selectedTargets[`target-slot-${mon.fieldSlot}`] === t.fieldSlot ? 'sd-target-selected' : ''"
                @click="setSelectedTarget(mon.fieldSlot, t.fieldSlot)"
              >{{ t.name || t.name_en }}</button>
            </div>
            <!-- 特殊系统 -->
            <div v-if="availableSpecialSystems(mon).length" class="sd-special-row">
              <button
                class="sd-special-btn"
                :class="!selectedSpecialSystems[`special-slot-${mon.fieldSlot}`] ? 'sd-special-active' : ''"
                @click="setSelectedSpecialSystem(mon.fieldSlot, undefined)"
              >{{ tr('无', 'None') }}</button>
              <button
                v-for="sys in availableSpecialSystems(mon)"
                :key="sys"
                class="sd-special-btn"
                :class="selectedSpecialSystems[`special-slot-${mon.fieldSlot}`] === sys ? 'sd-special-active' : ''"
                @click="setSelectedSpecialSystem(mon.fieldSlot, sys)"
              >{{ specialSystemLabel(sys) }}<template v-if="sys === 'tera'"> · {{ teraTypeLabel(mon) }}</template></button>
            </div>
          </template>

          <!-- 换人面板 -->
          <template v-else>
            <div class="sd-switch-grid">
              <button
                v-for="target in playerBenchOptions"
                :key="`sw-${mon.fieldSlot}-${target.value}`"
                type="button"
                class="sd-switch-btn"
                :class="selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`] === target.value ? 'sd-switch-selected' : ''"
                @click="setSelectedSwitchTarget(mon.fieldSlot, target.value)"
              >
                <img :src="previewSprite(target.pokemon || target)" :alt="target.label" class="sd-switch-img" @error="onSpriteError($event, target.pokemon || target)">
                <div class="sd-switch-info">
                  <span class="sd-switch-name">{{ target.label }}</span>
                  <div class="sd-switch-hp-bar">
                    <div class="sd-switch-hp-fill" :style="{ width: target.maxHp > 0 ? Math.max(2, (target.hp / target.maxHp) * 100) + '%' : '100%', background: target.maxHp > 0 && target.hp / target.maxHp <= 0.25 ? '#ef4444' : '#4ade80' }" />
                  </div>
                </div>
              </button>
            </div>
          </template>
        </div>
      </div>

      <!-- 提交按钮 -->
      <button
        class="sd-submit-btn"
        :disabled="!canSubmitMove || isBusy"
        @click="submitMove"
      >
        {{ busyAction === 'submit-move' ? tr('提交中...', 'Submitting...') : tr('提交回合', 'End Turn') }}
      </button>

      <div v-if="!playerActiveMons.length" class="sd-empty">
        {{ tr('开始一场对战后这里会显示招式选择', 'Start a battle to see move options') }}
      </div>
    </section>

    <!-- 详情弹窗 -->
    <PokemonDetailPopover v-model:visible="showDetailDialog" :pokemon="detailPokemon" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useLocale } from '../composables/useLocale'
import MoveButton from './MoveButton.vue'
import PokemonDetailPopover from './PokemonDetailPopover.vue'
import { sprites } from '../services/sprites'
import { typeColor, getTypeEffectiveness, resolveTypeId } from '../services/typeChart'

const { translate: tr } = useLocale()

const emit = defineEmits(['toggle-debug-panel'])

const showDetailDialog = ref(false)
const detailPokemon = ref(null)
function openDetail(pokemon) {
  detailPokemon.value = pokemon
  showDetailDialog.value = true
}

function previewSprite(pokemon) {
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  return id ? sprites.pokemon(id) : sprites.default
}

function onSpriteError(event, pokemon) {
  const img = event.target
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  img.src = sprites.fallbackPokemon(id)
}

function moveEffectivenessFor(mon, move) {
  if (!move || Number(move?.power || 0) <= 0) return null
  const moveTypeId = Number(move?.type_id || 0)
  if (moveTypeId <= 0) return null
  const targets = opponentActiveMons.value || []
  if (!targets.length) return null
  const eff = getTypeEffectiveness()
  let maxMult = 0
  for (const t of targets) {
    const types = t?.types || []
    let mult = 1
    for (const tp of types) {
      const tid = typeof tp === 'object' ? Number(tp.type_id) : Number(tp)
      mult *= (eff[moveTypeId]?.[resolveTypeId(tid)] ?? 100) / 100
    }
    if (mult > maxMult) maxMult = mult
  }
  return maxMult > 0 ? maxMult : null
}

defineProps({
  rosterLimit: { type: Number, default: 4 },
  leadLimit: { type: Number, default: 2 },
  busyAction: { type: String, default: '' },
  availableSpecialSystems: { type: Function, required: true },
  activeSpecialSystemLabel: { type: Function, required: true },
  canConfirmPreview: { type: Boolean, default: false },
  canConfirmReplacement: { type: Boolean, default: false },
  canUseSpecialSystem: { type: Function, required: true },
  canTerastallize: { type: Function, required: true },
  canSubmitMove: { type: Boolean, default: false },
  confirmPreview: { type: Function, required: true },
  confirmReplacement: { type: Function, required: true },
  formatTypes: { type: Function, required: true },
  isBusy: { type: Boolean, default: false },
  isLead: { type: Function, required: true },
  isPicked: { type: Function, required: true },
  isPreviewPhase: { type: Boolean, default: false },
  isReplacementPhase: { type: Boolean, default: false },
  leadRosterIndexes: { type: Array, default: () => [] },
  moveEffectivenessHints: { type: Function, required: true },
  moveNeedsOpponentTarget: { type: Function, required: true },
  opponentActiveMons: { type: Array, default: () => [] },
  opponentActiveOptions: { type: Array, default: () => [] },
  opponentRoster: { type: Array, default: () => [] },
  pendingReplacementCount: { type: Number, default: 0 },
  playerActiveMons: { type: Array, default: () => [] },
  playerBenchOptions: { type: Array, default: () => [] },
  playerRoster: { type: Array, default: () => [] },
  previewCardClass: { type: Function, required: true },
  replacementBenchOptions: { type: Array, default: () => [] },
  resultText: { type: String, default: '' },
  selectedActions: { type: Object, default: () => ({}) },
  setSelectedAction: { type: Function, required: true },
  selectedMoveObject: { type: Function, required: true },
  selectedMoves: { type: Object, default: () => ({}) },
  setSelectedMove: { type: Function, required: true },
  selectedSpecialSystems: { type: Object, default: () => ({}) },
  setSelectedSpecialSystem: { type: Function, required: true },
  selectedReplacementIndexes: { type: Array, default: () => [] },
  selectedRosterIndexes: { type: Array, default: () => [] },
  selectedSwitchTargets: { type: Object, default: () => ({}) },
  setSelectedSwitchTarget: { type: Function, required: true },
  selectedTargets: { type: Object, default: () => ({}) },
  setSelectedTarget: { type: Function, required: true },
  showDebugPanel: { type: Boolean, default: false },
  specialSystemLabel: { type: Function, required: true },
  submitMove: { type: Function, required: true },
  teraTypeLabel: { type: Function, required: true },
  toggleLead: { type: Function, required: true },
  toggleReplacement: { type: Function, required: true },
  toggleRoster: { type: Function, required: true }
})
</script>

<style scoped>
/* ===== Showdown 风格决策面板 ===== */
.sd-panel {
  background: #2d2d2d;
  border-radius: 4px;
  font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

/* 预览阶段 */
.sd-preview {
  padding: 12px;
}
.sd-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #444;
}
.sd-preview-title {
  font-size: 14px;
  font-weight: 700;
  color: #e2e8f0;
}
.sd-preview-info {
  font-size: 11px;
  color: #94a3b8;
}

.sd-roster-row {
  margin-bottom: 10px;
}
.sd-roster-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
  color: #94a3b8;
}
.sd-roster-opp { color: #f87171; }
.sd-roster-player { color: #4ade80; }

.sd-roster-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.sd-roster-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 6px;
  border: 2px solid #444;
  border-radius: 3px;
  background: #1a1a1a;
  cursor: pointer;
  min-width: 60px;
  transition: all 0.15s;
  position: relative;
}
.sd-roster-card:hover { border-color: #666; }
.sd-roster-card-opp { cursor: default; opacity: 0.7; }
.sd-picked { border-color: #4ade80; background: #1a2e1a; }
.sd-unpicked { opacity: 0.4; }
.sd-lead { border-color: #fbbf24; }
.sd-lead-mark {
  position: absolute;
  top: 2px;
  right: 4px;
  font-size: 10px;
  color: #fbbf24;
}
.sd-roster-img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  image-rendering: pixelated;
}
.sd-img-dim { filter: grayscale(0.7) brightness(0.5); }
.sd-roster-name {
  font-size: 9px;
  color: #cbd5e1;
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.sd-confirm-btn {
  width: 100%;
  padding: 8px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 3px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
}
.sd-confirm-btn:hover:not(:disabled) { background: #2563eb; }
.sd-confirm-btn:disabled { background: #374151; color: #6b7280; cursor: not-allowed; }

/* 补位阶段 */
.sd-replacement { padding: 12px; }
.sd-replacement-header {
  font-size: 13px;
  font-weight: 700;
  color: #fbbf24;
  margin-bottom: 8px;
}
.sd-replacement-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.sd-replacement-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 2px solid #444;
  border-radius: 3px;
  background: #1a1a1a;
  cursor: pointer;
  transition: all 0.15s;
}
.sd-replacement-btn:hover { border-color: #666; }
.sd-replacement-selected { border-color: #f87171; background: #2a1a1a; }
.sd-replacement-img { width: 32px; height: 32px; object-fit: contain; image-rendering: pixelated; }
.sd-replacement-info { display: flex; flex-direction: column; gap: 2px; }
.sd-replacement-name { font-size: 12px; color: #e2e8f0; font-weight: 600; }
.sd-replacement-hp-bar { height: 6px; background: #333; border-radius: 2px; overflow: hidden; width: 80px; }
.sd-replacement-hp-fill { height: 100%; transition: width 0.3s; }
.sd-replacement-hp-text { font-size: 10px; color: #94a3b8; }

/* 战斗操作阶段 */
.sd-battle { padding: 10px; }
.sd-battle-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sd-mon-section {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 3px;
  padding: 8px;
}

.sd-action-toggle {
  display: flex;
  gap: 2px;
  margin-bottom: 6px;
}
.sd-toggle-btn {
  flex: 1;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 700;
  border: 1px solid #444;
  background: #252525;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s;
}
.sd-toggle-btn:first-child { border-radius: 3px 0 0 3px; }
.sd-toggle-btn:last-child { border-radius: 0 3px 3px 0; }
.sd-toggle-active { background: #3b82f6; color: #fff; border-color: #3b82f6; }
.sd-toggle-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.sd-moves-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.sd-target-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}
.sd-target-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}
.sd-target-btn {
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #555;
  border-radius: 3px;
  background: #252525;
  color: #e2e8f0;
  cursor: pointer;
}
.sd-target-btn:hover { border-color: #888; }
.sd-target-selected { border-color: #f87171; background: #3a1a1a; color: #fca5a5; }

.sd-special-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}
.sd-special-btn {
  padding: 3px 8px;
  font-size: 10px;
  font-weight: 600;
  border: 1px solid #555;
  border-radius: 3px;
  background: #252525;
  color: #fcd34d;
  cursor: pointer;
}
.sd-special-active { background: #92400e; border-color: #f59e0b; color: #fef3c7; }

/* 换人面板 */
.sd-switch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 4px;
  margin-top: 6px;
}
.sd-switch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border: 2px solid #444;
  border-radius: 3px;
  background: #252525;
  cursor: pointer;
  transition: all 0.15s;
}
.sd-switch-btn:hover { border-color: #666; }
.sd-switch-selected { border-color: #3b82f6; background: #1a2a3a; }
.sd-switch-img { width: 32px; height: 32px; object-fit: contain; image-rendering: pixelated; }
.sd-switch-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.sd-switch-name { font-size: 11px; color: #e2e8f0; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-switch-hp-bar { height: 5px; background: #333; border-radius: 2px; overflow: hidden; }
.sd-switch-hp-fill { height: 100%; transition: width 0.3s; }

.sd-submit-btn {
  width: 100%;
  padding: 10px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 3px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
}
.sd-submit-btn:hover:not(:disabled) { background: #2563eb; }
.sd-submit-btn:disabled { background: #374151; color: #6b7280; cursor: not-allowed; }

.sd-empty {
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
}
</style>
