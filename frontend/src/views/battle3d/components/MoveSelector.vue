<template>
  <div class="moves-section">
    <div class="section-header">
      <span class="section-icon">⚔️</span>
      <span class="section-title">{{ t('选择招式', 'Select Move') }}</span>
    </div>
    <div v-for="(mon, slotIdx) in playerActiveMons" :key="'m'+slotIdx" class="move-block">
      <div class="move-block-header">
        <span class="block-name">{{ mon.name || mon.name_en }}</span>
        <span class="block-hp">{{ mon.currentHp }}/{{ mon.stats?.hp || mon.currentHp }} HP</span>
      </div>
      <div class="move-grid">
        <button
          v-for="(move, moveIdx) in (mon.moves || [])"
          :key="moveIdx"
          class="move-btn"
          :class="{ selected: selectedMoveIndex === Number(moveIdx) && selectedSlot === Number(slotIdx) }"
          :style="{ '--tc': getTypeColor(move.type_name || move.name_en) }"
          :disabled="disabled"
          @click="$emit('select-move', Number(slotIdx), Number(moveIdx), move)"
        >
          <span class="mv-shortcut" v-if="Number(slotIdx) === 0">{{ Number(moveIdx) + 1 }}</span>
          <span class="mv-name">{{ move.name || move.name_en }}</span>
          <span class="mv-type" :style="{ background: getTypeColor(move.type_name || move.name_en) }">
            {{ move.type_name || '?' }}
          </span>
          <span class="mv-stats">
            <span v-if="move.power" class="mv-power">⚡{{ move.power }}</span>
            <span class="mv-pp">PP {{ move.current_pp ?? move.pp }}/{{ move.pp }}</span>
          </span>
        </button>
      </div>

      <!-- 目标选择 -->
      <div v-if="needsTarget && needsTarget(slotIdx)" class="target-row">
        <span class="target-label">🎯 {{ t('选择目标', 'Select Target') }}:</span>
        <div class="target-buttons">
          <button
            v-for="(opp, oi) in opponentActiveMons"
            :key="oi"
            class="target-btn"
            :class="{ active: selectedTargets[`target-slot-${slotIdx}`] === opp.fieldSlot }"
            @click="$emit('select-target', Number(slotIdx), opp.fieldSlot)"
          >
            {{ opp.name || opp.name_en }}
          </button>
        </div>
      </div>

      <!-- 特殊系统 -->
      <div v-if="hasSpecialSystem && hasSpecialSystem(mon)" class="special-row">
        <button
          v-for="sys in (availableSpecialSystems ? availableSpecialSystems(mon) : [])"
          :key="sys"
          class="special-btn"
          :class="{ active: selectedSpecialSystems[`special-slot-${slotIdx}`] === sys }"
          @click="$emit('toggle-special', Number(slotIdx), sys)"
        >
          {{ specialSystemLabel ? specialSystemLabel(sys) : sys }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getTypeColor } from '../utils/typeColors'
import { useLocale } from '../../../composables/useLocale'

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

interface Props {
  playerActiveMons: any[]
  opponentActiveMons: any[]
  selectedSlot?: number
  selectedMoveIndex?: number
  selectedTargets?: Record<string, any>
  selectedSpecialSystems?: Record<string, any>
  disabled?: boolean
  needsTarget?: ((slot: number) => boolean) | null
  hasSpecialSystem?: ((mon: any) => boolean) | null
  availableSpecialSystems?: ((mon: any) => string[]) | null
  specialSystemLabel?: ((sys: string) => string) | null
}

withDefaults(defineProps<Props>(), {
  selectedSlot: -1,
  selectedMoveIndex: -1,
  selectedTargets: () => ({}),
  selectedSpecialSystems: () => ({}),
  disabled: false,
  needsTarget: null,
  hasSpecialSystem: null,
  availableSpecialSystems: null,
  specialSystemLabel: null
})

defineEmits<{
  'select-move': [slot: number, moveIdx: number, move: any]
  'select-target': [slot: number, targetSlot: number]
  'toggle-special': [slot: number, sys: string]
}>()
</script>

<style scoped>
.moves-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.section-icon { font-size: 16px; }

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255,255,255,0.8);
}

.move-block {
  background: rgba(255,255,255,0.02);
  border-radius: 14px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.04);
}

.move-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.block-name {
  font-size: 14px;
  font-weight: 800;
  color: #fff;
}

.block-hp {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  font-weight: 600;
}

.move-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.move-btn {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 2px solid rgba(0,0,0,0.2);
  border-radius: 12px;
  background: var(--tc, #555);
  color: #fff;
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.15);
}

.move-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255,255,255,0.12) 0%, transparent 100%);
  pointer-events: none;
}

.move-btn:hover:not(:disabled) {
  filter: brightness(1.15);
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.2);
}

.move-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  transform: none;
}

.move-btn.selected {
  border-color: #fbbf24 !important;
  box-shadow: 0 0 20px rgba(251,191,36,0.5), 0 8px 20px rgba(0,0,0,0.3);
  transform: scale(1.03);
}

.mv-shortcut {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: rgba(0,0,0,0.6);
  color: rgba(255,255,255,0.8);
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255,255,255,0.1);
}

.mv-name {
  font-weight: 800;
  font-size: 13px;
  line-height: 1.3;
}

.mv-type {
  display: inline-block;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  color: #fff;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  width: fit-content;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.mv-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  opacity: 0.8;
}

.mv-power { font-weight: 700; }
.mv-pp { color: rgba(255,255,255,0.6); }

.target-row {
  margin-top: 10px;
  padding: 10px;
  background: rgba(0,0,0,0.2);
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.05);
}

.target-label {
  font-size: 12px;
  font-weight: 700;
  color: rgba(255,255,255,0.6);
  margin-bottom: 8px;
  display: block;
}

.target-buttons {
  display: flex;
  gap: 6px;
}

.target-btn {
  flex: 1;
  padding: 8px 12px;
  border: 2px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.target-btn:hover {
  border-color: rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.1);
}

.target-btn.active {
  background: rgba(239,68,68,0.3);
  border-color: #ef4444;
  box-shadow: 0 0 16px rgba(239,68,68,0.3);
}

.special-row {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.special-btn {
  padding: 6px 12px;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.7);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.special-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.special-btn.active {
  background: rgba(99,102,241,0.3);
  border-color: #6366f1;
  color: #fff;
  box-shadow: 0 0 12px rgba(99,102,241,0.3);
}

@media (max-width: 768px) {
  .move-grid { gap: 6px; }
  .move-btn { padding: 10px; }
}

@media (max-width: 480px) {
  .move-grid { grid-template-columns: 1fr; }
}
</style>
