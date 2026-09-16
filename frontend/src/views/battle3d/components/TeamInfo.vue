<template>
  <div class="teams-row">
    <!-- 对手 -->
    <div class="team-card opponent">
      <div class="team-header">
        <span class="team-icon">🔴</span>
        <span class="team-label">{{ t('对手', 'Opponent') }}</span>
      </div>
      <div v-for="(mon, i) in opponentMons" :key="'o'+i" class="mon-card">
        <div class="mon-main">
          <img :src="getSpriteUrl(mon)" :alt="mon.name" class="mon-sprite" @error="handleImgError" />
          <div class="mon-info">
            <div class="mon-header">
              <span class="mon-name">{{ mon.name || mon.name_en }}</span>
              <span class="mon-level">Lv.{{ mon.level || 50 }}</span>
            </div>
            <div class="hp-bar">
              <div class="hp-fill" :style="{ width: getHpPercent(mon)+'%', background: getHpGradient(mon) }" />
            </div>
            <div class="mon-stats">
              <span class="hp-text">{{ mon.currentHp || 0 }}/{{ mon.stats?.hp || mon.currentHp || 0 }}</span>
              <div class="mon-types">
                <span
                  v-for="type in (mon.types || []).slice(0, 2)"
                  :key="type.type_id"
                  class="type-badge"
                  :style="{ background: getTypeColor(type.name_en || type.name) }"
                >
                  {{ type.name || type.name_en }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 我方 -->
    <div class="team-card player">
      <div class="team-header">
        <span class="team-icon">🟢</span>
        <span class="team-label">{{ t('我方', 'Player') }}</span>
      </div>
      <div v-for="(mon, i) in playerMons" :key="'p'+i" class="mon-card">
        <div class="mon-main">
          <img :src="getSpriteUrl(mon)" :alt="mon.name" class="mon-sprite" @error="handleImgError" />
          <div class="mon-info">
            <div class="mon-header">
              <span class="mon-name">{{ mon.name || mon.name_en }}</span>
              <span class="mon-level">Lv.{{ mon.level || 50 }}</span>
            </div>
            <div class="hp-bar">
              <div class="hp-fill" :style="{ width: getHpPercent(mon)+'%', background: getHpGradient(mon) }" />
            </div>
            <div class="mon-stats">
              <span class="hp-text">{{ mon.currentHp || 0 }}/{{ mon.stats?.hp || mon.currentHp || 0 }}</span>
              <div class="mon-types">
                <span
                  v-for="type in (mon.types || []).slice(0, 2)"
                  :key="type.type_id"
                  class="type-badge"
                  :style="{ background: getTypeColor(type.name_en || type.name) }"
                >
                  {{ type.name || type.name_en }}
                </span>
              </div>
            </div>
            <div class="mon-status" v-if="mon.condition || mon.terastallized">
              <span v-if="mon.condition" class="status-badge" :class="'status-' + mon.condition">
                {{ getStatusLabel(mon.condition) }}
              </span>
              <span v-if="mon.terastallized" class="status-badge status-tera">✨ Tera</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getTypeColor } from '../utils/typeColors'
import { useLocale } from '../../../composables/useLocale'

interface Props {
  playerMons: any[]
  opponentMons: any[]
}

defineProps<Props>()

const localeResult = useLocale() as any
const tr = localeResult.translate
const t = (zh: string, en: string) => tr(zh, en)

// 精灵图 URL
function getSpriteUrl(pokemon: any): string {
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  return id ? `/api/pokedex/images/pokemon/${id}.png` : '/pokemon-placeholder.svg'
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img) img.src = '/pokemon-placeholder.svg'
}

function getHpPercent(mon: any): number {
  const max = mon?.stats?.hp || mon?.currentHp || 1
  return Math.max(0, Math.min(100, ((mon?.currentHp || 0) / max) * 100))
}

function getHpGradient(mon: any): string {
  const pct = getHpPercent(mon)
  if (pct <= 20) return 'linear-gradient(90deg, #ef4444, #dc2626)'
  if (pct <= 50) return 'linear-gradient(90deg, #fbbf24, #f59e0b)'
  return 'linear-gradient(90deg, #4ade80, #22c55e)'
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
  gap: 10px;
}

.team-card {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.06);
}

.team-card.opponent {
  border-color: rgba(239,68,68,0.2);
  background: linear-gradient(180deg, rgba(239,68,68,0.05) 0%, transparent 100%);
}

.team-card.player {
  border-color: rgba(74,222,128,0.2);
  background: linear-gradient(180deg, rgba(74,222,128,0.05) 0%, transparent 100%);
}

.team-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.team-icon {
  font-size: 12px;
}

.team-label {
  font-size: 13px;
  font-weight: 800;
  color: rgba(255,255,255,0.7);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mon-card {
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.mon-card:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.mon-main {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.mon-sprite {
  width: 56px;
  height: 56px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.3));
  flex-shrink: 0;
  background: rgba(255,255,255,0.03);
  border-radius: 10px;
  padding: 4px;
}

.mon-info {
  flex: 1;
  min-width: 0;
}

.mon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.mon-name {
  font-size: 13px;
  font-weight: 800;
  color: #fff;
}

.mon-level {
  font-size: 11px;
  color: rgba(255,255,255,0.4);
  font-weight: 600;
}

.hp-bar {
  height: 8px;
  background: rgba(255,255,255,0.08);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
  border: 1px solid rgba(255,255,255,0.04);
}

.hp-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 8px rgba(74,222,128,0.3);
}

.mon-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hp-text {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  font-weight: 600;
}

.mon-types {
  display: flex;
  gap: 4px;
}

.type-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 5px;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.mon-status {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.status-badge {
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.status-PAR { background: rgba(161,98,7,0.4); color: #fcd34d; border: 1px solid rgba(161,98,7,0.5); }
.status-BRN { background: rgba(194,65,12,0.4); color: #fdba74; border: 1px solid rgba(194,65,12,0.5); }
.status-FRZ { background: rgba(3,105,161,0.4); color: #67e8f9; border: 1px solid rgba(3,105,161,0.5); }
.status-SLP { background: rgba(109,40,217,0.4); color: #d8b4fe; border: 1px solid rgba(109,40,217,0.5); }
.status-PSN, .status-TOX { background: rgba(126,34,206,0.4); color: #d8b4fe; border: 1px solid rgba(126,34,206,0.5); }
.status-tera { background: rgba(99,102,241,0.4); color: #a5b4fc; border: 1px solid rgba(99,102,241,0.5); }

@media (max-width: 768px) {
  .teams-row {
    gap: 8px;
  }

  .team-card {
    padding: 10px;
  }

  .mon-name {
    font-size: 12px;
  }
}
</style>
