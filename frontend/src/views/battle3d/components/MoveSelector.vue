<template>
  <div class="moves-section">
    <div class="section-label">⚔️ {{ t('选择招式', 'Select Move') }}</div>
    <div v-for="(mon, slotIdx) in playerActiveMons" :key="'m'+slotIdx" class="move-block">
      <div class="move-block-name">{{ mon.name || mon.name_en }}</div>
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
      <div v-if="needsTarget && needsTarget(slotIdx)" class="target-row">
        <span class="target-label">🎯</span>
        <button
          v-for="(opp, oi) in opponentActiveMons"
          :key="oi"
          class="target-btn"
          :class="{ active: selectedTargets[`target-slot-${slotIdx}`] === opp.fieldSlot }"
          @click="$emit('select-target', slotIdx, opp.fieldSlot)"
        >
          {{ opp.name || opp.name_en }}
        </button>
      </div>

      <!-- 特殊系统 -->
      <div v-if="hasSpecialSystem && hasSpecialSystem(mon)" class="special-row">
        <button
          v-for="sys in (availableSpecialSystems ? availableSpecialSystems(mon) : [])"
          :key="sys"
          class="special-btn"
          :class="{ active: selectedSpecialSystems[`special-slot-${slotIdx}`] === sys }"
          @click="$emit('toggle-special', slotIdx, sys)"
        >
          {{ specialSystemLabel ? specialSystemLabel(sys) : sys }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getTypeColor } from '../utils/typeColors'

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

const props = withDefaults(defineProps<Props>(), {
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

const localeResult = (await import('../../../composables/useLocale')).useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)
</script>

<style scoped>
.moves-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

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

.move-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.move-btn.selected {
  border-color: #fbbf24 !important;
  box-shadow: 0 0 12px rgba(251,191,36,0.5), 0 4px 8px rgba(0,0,0,0.3);
  transform: scale(1.02);
}

.mv-shortcut {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: rgba(0,0,0,0.5);
  color: rgba(255,255,255,0.8);
  font-size: 10px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mv-name {
  font-weight: bold;
  font-size: 12px;
  line-height: 1.2;
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

.mv-info {
  font-size: 10px;
  opacity: 0.9;
  margin-top: 2px;
  display: flex;
  justify-content: space-between;
}

.target-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.target-label {
  font-size: 12px;
}

.target-btn {
  flex: 1;
  padding: 4px 8px;
  border: 1px solid rgba(255,255,255,0.2);
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

.target-btn.active {
  background: rgba(239,68,68,0.4);
  border-color: #ef4444;
}

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
  transition: all 0.15s;
}

.special-btn:hover {
  background: rgba(255,255,255,0.2);
}

.special-btn.active {
  background: rgba(99,102,241,0.5);
  border-color: #6366f1;
}

@media (max-width: 768px) {
  .move-grid {
    grid-template-columns: 1fr 1fr;
    gap: 4px;
  }

  .move-btn {
    padding: 6px 8px;
    font-size: 10px;
  }

  .mv-name {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .move-grid {
    grid-template-columns: 1fr;
  }
}
</style>
