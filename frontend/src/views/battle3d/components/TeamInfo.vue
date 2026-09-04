<template>
  <div class="teams-row">
    <!-- 对手 -->
    <div class="team-card opponent">
      <div class="team-label">🔴 {{ t('对手', 'Opponent') }}</div>
      <div v-for="(mon, i) in opponentMons" :key="'o'+i" class="mon-mini">
        <div class="mon-header">
          <span class="mon-name">{{ mon.name || mon.name_en }}</span>
          <span class="mon-level">Lv.{{ mon.level || 50 }}</span>
        </div>
        <div class="mini-hp">
          <div class="mini-hp-bar" :style="{ width: getHpPercent(mon)+'%', background: getHpColor(mon) }" />
        </div>
        <div class="mon-hp-num">{{ mon.currentHp || 0 }} / {{ mon.stats?.hp || mon.currentHp || 0 }}</div>
        <div class="mon-types">
          <span
            v-for="type in (mon.types || []).slice(0, 2)"
            :key="type.type_id"
            class="type-mini"
            :style="{ background: getTypeColor(type.name_en || type.name) }"
          >
            {{ type.name || type.name_en }}
          </span>
        </div>
      </div>
    </div>

    <!-- 我方 -->
    <div class="team-card player">
      <div class="team-label">🟢 {{ t('我方', 'Player') }}</div>
      <div v-for="(mon, i) in playerMons" :key="'p'+i" class="mon-mini">
        <div class="mon-header">
          <span class="mon-name">{{ mon.name || mon.name_en }}</span>
          <span class="mon-level">Lv.{{ mon.level || 50 }}</span>
        </div>
        <div class="mini-hp">
          <div class="mini-hp-bar" :style="{ width: getHpPercent(mon)+'%', background: getHpColor(mon) }" />
        </div>
        <div class="mon-hp-num">{{ mon.currentHp || 0 }} / {{ mon.stats?.hp || mon.currentHp || 0 }}</div>
        <div class="mon-types">
          <span
            v-for="type in (mon.types || []).slice(0, 2)"
            :key="type.type_id"
            class="type-mini"
            :style="{ background: getTypeColor(type.name_en || type.name) }"
          >
            {{ type.name || type.name_en }}
          </span>
        </div>
        <div class="mon-status">
          <span v-if="mon.condition" class="status-badge" :class="'status-' + mon.condition">
            {{ getStatusLabel(mon.condition) }}
          </span>
          <span v-if="mon.terastallized" class="status-badge status-tera">Tera</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getTypeColor } from '../utils/typeColors'

interface Props {
  playerMons: any[]
  opponentMons: any[]
}

defineProps<Props>()

const localeResult = (await import('../../../composables/useLocale')).useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

function getHpPercent(mon: any): number {
  const max = mon?.stats?.hp || mon?.currentHp || 1
  return Math.max(0, Math.min(100, ((mon?.currentHp || 0) / max) * 100))
}

function getHpColor(mon: any): string {
  const pct = getHpPercent(mon)
  if (pct <= 20) return '#ef4444'
  if (pct <= 50) return '#fbbf24'
  return '#4ade80'
}

function getStatusLabel(condition: string): string {
  const labels: Record<string, string> = {
    paralysis: 'PAR', burn: 'BRN', freeze: 'FRZ', sleep: 'SLP',
    poison: 'PSN', toxic: 'TOX'
  }
  return labels[condition] || condition
}
</script>

<style scoped>
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

.team-card.opponent {
  border: 1px solid rgba(239,68,68,0.3);
}

.team-card.player {
  border: 1px solid rgba(74,222,128,0.3);
}

.team-label {
  font-size: 11px;
  font-weight: bold;
  color: rgba(255,255,255,0.7);
  margin-bottom: 6px;
}

.mon-mini {
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.mon-mini:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.mon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.mon-name {
  font-size: 12px;
  font-weight: bold;
  color: #fff;
}

.mon-level {
  font-size: 10px;
  color: rgba(255,255,255,0.5);
}

.mini-hp {
  height: 6px;
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 2px;
}

.mini-hp-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.mon-hp-num {
  font-size: 10px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 2px;
}

.mon-types {
  display: flex;
  gap: 3px;
  flex-wrap: wrap;
}

.type-mini {
  font-size: 9px;
  padding: 1px 4px;
  border-radius: 3px;
  color: #fff;
  font-weight: bold;
}

.mon-status {
  display: flex;
  gap: 3px;
  margin-top: 3px;
}

.status-badge {
  font-size: 9px;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: bold;
}

.status-PAR { background: #a16207; color: #fff; }
.status-BRN { background: #c2410c; color: #fff; }
.status-FRZ { background: #0369a1; color: #fff; }
.status-SLP { background: #6d28d9; color: #fff; }
.status-PSN, .status-TOX { background: #7e22ce; color: #fff; }
.status-tera { background: #6366f1; color: #fff; }

@media (max-width: 768px) {
  .teams-row {
    gap: 6px;
  }

  .team-card {
    padding: 6px;
  }

  .mon-name {
    font-size: 11px;
  }
}
</style>
