<template>
  <div class="battle-arena space-y-4 rounded-[24px] border border-slate-200/80 bg-white/95 p-4 shadow-[0_24px_90px_-54px_rgba(15,23,42,0.5)] backdrop-blur sm:rounded-[28px] sm:p-6">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 class="text-xl font-black tracking-tight text-slate-900">
          {{ tr('战场', 'Battlefield') }}
        </h2>
        <p class="mt-1 text-sm leading-6 text-slate-500">
          {{ tr('聚焦当前场上态势、场地效果和逐回合事件，方便你复盘决策链。', 'Focus on the active board state, field effects, and round-by-round events so you can review the decision flow.') }}
        </p>
      </div>
      <div
        v-if="summary"
        class="grid gap-2 text-sm sm:grid-cols-3"
      >
        <div class="rounded-2xl bg-slate-50 px-4 py-3 shadow-sm">
          <div class="text-slate-500">
            {{ tr('规则', 'Format') }}
          </div>
          <div class="font-semibold text-slate-900">
            {{ summary.format || 'vgc-doubles' }}
          </div>
        </div>
        <div class="rounded-2xl bg-slate-50 px-4 py-3 shadow-sm">
          <div class="text-slate-500">
            {{ tr('当前回合', 'Current round') }}
          </div>
          <div class="font-semibold text-slate-900">
            {{ summary.currentRound || 0 }} / {{ summary.roundLimit || '-' }}
          </div>
        </div>
        <div
          class="rounded-2xl px-4 py-3 shadow-sm"
          :class="statusChipClass"
        >
          <div class="text-slate-500">
            {{ tr('状态', 'Status') }}
          </div>
          <div
            class="font-semibold"
            :class="statusTextClass"
          >
            {{ statusText || tr('未开始', 'Not started') }}
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="summary?.fieldEffects"
      class="flex flex-wrap gap-2"
    >
      <span
        v-if="summary.fieldEffects.playerTailwindTurns > 0"
        class="rounded-full bg-blue-50 px-3 py-1 text-xs text-blue-700"
      >
        {{ tr('我方顺风 {turns} 回合', 'Your Tailwind: {turns} turns', { turns: summary.fieldEffects.playerTailwindTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.opponentTailwindTurns > 0"
        class="rounded-full bg-rose-50 px-3 py-1 text-xs text-rose-700"
      >
        {{ tr('对手顺风 {turns} 回合', 'Opponent Tailwind: {turns} turns', { turns: summary.fieldEffects.opponentTailwindTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.trickRoomTurns > 0"
        class="rounded-full bg-violet-50 px-3 py-1 text-xs text-violet-700"
      >
        {{ tr('戏法空间 {turns} 回合', 'Trick Room: {turns} turns', { turns: summary.fieldEffects.trickRoomTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.rainTurns > 0"
        class="rounded-full bg-cyan-50 px-3 py-1 text-xs text-cyan-700"
      >
        {{ tr('下雨 {turns} 回合', 'Rain: {turns} turns', { turns: summary.fieldEffects.rainTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.sunTurns > 0"
        class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
      >
        {{ tr('大晴天 {turns} 回合', 'Sun: {turns} turns', { turns: summary.fieldEffects.sunTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.electricTerrainTurns > 0"
        class="rounded-full bg-yellow-50 px-3 py-1 text-xs text-yellow-700"
      >
        {{ tr('电气场地 {turns} 回合', 'Electric Terrain: {turns} turns', { turns: summary.fieldEffects.electricTerrainTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.psychicTerrainTurns > 0"
        class="rounded-full bg-purple-50 px-3 py-1 text-xs text-purple-700"
      >
        {{ tr('精神场地 {turns} 回合', 'Psychic Terrain: {turns} turns', { turns: summary.fieldEffects.psychicTerrainTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.playerReflectTurns > 0"
        class="rounded-full bg-sky-50 px-3 py-1 text-xs text-sky-700"
      >
        {{ tr('我方反射壁 {turns} 回合', 'Your Reflect: {turns} turns', { turns: summary.fieldEffects.playerReflectTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.opponentReflectTurns > 0"
        class="rounded-full bg-rose-50 px-3 py-1 text-xs text-rose-700"
      >
        {{ tr('对手反射壁 {turns} 回合', 'Opponent Reflect: {turns} turns', { turns: summary.fieldEffects.opponentReflectTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.playerLightScreenTurns > 0"
        class="rounded-full bg-indigo-50 px-3 py-1 text-xs text-indigo-700"
      >
        {{ tr('我方光墙 {turns} 回合', 'Your Light Screen: {turns} turns', { turns: summary.fieldEffects.playerLightScreenTurns }) }}
      </span>
      <span
        v-if="summary.fieldEffects.opponentLightScreenTurns > 0"
        class="rounded-full bg-fuchsia-50 px-3 py-1 text-xs text-fuchsia-700"
      >
        {{ tr('对手光墙 {turns} 回合', 'Opponent Light Screen: {turns} turns', { turns: summary.fieldEffects.opponentLightScreenTurns }) }}
      </span>
    </div>

    <div
      v-if="typeof highlightIndex !== 'undefined' && highlightIndex >= 0"
      class="rounded-2xl bg-blue-50 px-4 py-3 text-sm text-blue-700"
    >
      {{ tr('已替换玩家队伍中的第 {slot} 只宝可梦', 'Replaced Pokemon in player slot {slot}', { slot: highlightIndex + 1 }) }}
    </div>

    <template v-if="summary">
      <div class="grid gap-4 lg:grid-cols-2">
        <section class="rounded-[24px] border border-slate-200/80 bg-[linear-gradient(180deg,rgba(239,246,255,0.55),rgba(255,255,255,0.95))] p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="font-semibold text-slate-900">
              {{ tr('玩家队伍', 'Player team') }}
            </h3>
            <span class="text-sm text-slate-500">{{ tr('剩余 {count}', 'Remaining {count}', { count: summary.playerRemaining || 0 }) }}</span>
          </div>
          <div class="space-y-3">
            <article
              v-for="pokemon in buildCards(summary.playerTeam, summary.playerActiveSlots)"
              :key="`player-${pokemon.index}`"
              :class="['rounded-2xl border p-4 shadow-sm transition', pokemon.active ? 'border-sky-400 bg-white shadow-sky-100' : 'border-slate-200 bg-white/80']"
            >
              <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
                <div>
                  <div class="flex items-center gap-2">
                    <div class="font-semibold text-slate-900">
                      {{ pokemon.name }}
                    </div>
                    <span
                      v-if="pokemon.active"
                      class="rounded-full bg-sky-100 px-2 py-0.5 text-[11px] font-bold uppercase tracking-[0.18em] text-sky-700"
                    >
                      {{ tr('在场', 'Active') }}
                    </span>
                  </div>
                  <div class="text-xs text-slate-500">
                    HP {{ pokemon.currentHp }}/{{ pokemon.maxHp }} · {{ pokemon.statusText }}
                  </div>
                </div>
                <div class="text-right text-xs text-slate-500">
                  <div>{{ formatTypes(pokemon.types) }}</div>
                  <div class="mt-1 font-semibold text-slate-700">
                    {{ pokemon.hpPercent }}%
                  </div>
                </div>
              </div>
              <div
                v-if="pokemon.conditionLabels.length"
                class="mt-2 flex flex-wrap gap-2"
              >
                <span
                  v-for="label in pokemon.conditionLabels"
                  :key="label"
                  class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
                >
                  {{ label }}
                </span>
              </div>
              <div class="mt-3 h-2.5 overflow-hidden rounded-full bg-slate-200">
                <div
                  class="h-full rounded-full bg-gradient-to-r from-sky-400 to-blue-500"
                  :style="{ width: hpWidth(pokemon) }"
                />
              </div>
              <div class="mt-3 grid gap-2 sm:grid-cols-2">
                <span
                  v-for="move in pokemon.moves"
                  :key="move.name_en || move.name"
                  class="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600"
                >
                  {{ move.name || move.name_en }}
                </span>
              </div>
            </article>
          </div>
        </section>

        <section class="rounded-[24px] border border-slate-200/80 bg-[linear-gradient(180deg,rgba(255,241,242,0.56),rgba(255,255,255,0.95))] p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="font-semibold text-slate-900">
              {{ tr('对手队伍', 'Opponent team') }}
            </h3>
            <span class="text-sm text-slate-500">{{ tr('剩余 {count}', 'Remaining {count}', { count: summary.opponentRemaining || 0 }) }}</span>
          </div>
          <div class="space-y-3">
            <article
              v-for="pokemon in buildCards(summary.opponentTeam, summary.opponentActiveSlots)"
              :key="`opponent-${pokemon.index}`"
              :class="['rounded-2xl border p-4 shadow-sm transition', pokemon.active ? 'border-rose-400 bg-white shadow-rose-100' : 'border-slate-200 bg-white/80']"
            >
              <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
                <div>
                  <div class="flex items-center gap-2">
                    {{ pokemon.name }}
                    <span
                      v-if="pokemon.active"
                      class="rounded-full bg-rose-100 px-2 py-0.5 text-[11px] font-bold uppercase tracking-[0.18em] text-rose-700"
                    >
                      {{ tr('在场', 'Active') }}
                    </span>
                  </div>
                  <div class="text-xs text-slate-500">
                    HP {{ pokemon.currentHp }}/{{ pokemon.maxHp }} · {{ pokemon.statusText }}
                  </div>
                </div>
                <div class="text-right text-xs text-slate-500">
                  <div>{{ formatTypes(pokemon.types) }}</div>
                  <div class="mt-1 font-semibold text-slate-700">
                    {{ pokemon.hpPercent }}%
                  </div>
                </div>
              </div>
              <div
                v-if="pokemon.conditionLabels.length"
                class="mt-2 flex flex-wrap gap-2"
              >
                <span
                  v-for="label in pokemon.conditionLabels"
                  :key="label"
                  class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
                >
                  {{ label }}
                </span>
              </div>
              <div class="mt-3 h-2.5 overflow-hidden rounded-full bg-slate-200">
                <div
                  class="h-full rounded-full bg-gradient-to-r from-rose-400 to-red-500"
                  :style="{ width: hpWidth(pokemon) }"
                />
              </div>
              <div class="mt-3 grid gap-2 sm:grid-cols-2">
                <span
                  v-for="move in pokemon.moves"
                  :key="move.name_en || move.name"
                  class="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600"
                >
                  {{ move.name || move.name_en }}
                </span>
              </div>
            </article>
          </div>
        </section>
      </div>

      <section class="rounded-[24px] border border-slate-200/80 bg-slate-50/70 p-4">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="font-semibold text-slate-900">
            {{ tr('回合日志', 'Round log') }}
          </h3>
          <span class="text-sm text-slate-500">
            {{ summary.status === 'completed' ? tr(`胜者：${summary.winner}`, `Winner: ${summary.winner}`) : tr('战斗进行中', 'Battle in progress') }}
          </span>
        </div>
        <div
          v-if="summary.rounds?.length"
          class="space-y-3"
        >
          <article
            v-for="(round, roundIndex) in summary.rounds"
            :key="`${round.round}-${roundIndex}`"
            class="rounded-2xl bg-white p-4 shadow-sm"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="font-semibold text-slate-900">
                {{ round.round === 0 ? tr('入场阶段', 'Entry phase') : tr(`第 ${round.round} 回合`, `Round ${round.round}`) }}
              </div>
              <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
                {{ tr('{count} 条事件', '{count} events', { count: (round.events || []).length }) }}
              </span>
            </div>
            <div class="mt-2 space-y-2">
              <div
                v-for="event in round.events || []"
                :key="event"
                class="flex items-start gap-3 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700"
              >
                <span class="mt-1 h-2 w-2 rounded-full bg-slate-300" />
                <span>{{ event }}</span>
              </div>
            </div>
          </article>
        </div>
        <div
          v-else
          class="text-sm text-slate-500"
        >
          {{ tr('暂无回合日志', 'No round log yet') }}
        </div>
      </section>
    </template>

    <div
      v-else
      class="rounded-2xl bg-slate-50 px-4 py-8 text-center text-sm text-slate-500"
    >
      {{ tr('暂无对战数据', 'No battle data yet') }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const props = defineProps({
  summary: {
    type: Object,
    default: null
  },
  highlightIndex: {
    type: Number,
    default: -1
  },
  statusText: {
    type: String,
    default: ''
  },
  statusTone: {
    type: String,
    default: 'neutral'
  }
})

const statusChipClass = computed(() => {
  switch (props.statusTone) {
    case 'success': return 'bg-emerald-50'
    case 'danger': return 'bg-rose-50'
    case 'warning': return 'bg-amber-50'
    case 'info': return 'bg-sky-50'
    default: return 'bg-slate-50'
  }
})

const statusTextClass = computed(() => {
  switch (props.statusTone) {
    case 'success': return 'text-emerald-700'
    case 'danger': return 'text-rose-700'
    case 'warning': return 'text-amber-700'
    case 'info': return 'text-sky-700'
    default: return 'text-slate-900'
  }
})

function buildCards(team = [], activeSlots = []) {
  return (team || []).map((pokemon, index) => ({
    ...pokemon,
    index,
    active: (activeSlots || []).includes(index),
    name: pokemon.name || pokemon.name_en || tr(`宝可梦 ${index + 1}`, `Pokemon ${index + 1}`),
    currentHp: pokemon.currentHp || 0,
    maxHp: pokemon?.stats?.hp || pokemon.currentHp || 1,
    hpPercent: Math.max(0, Math.min(100, Math.round(((pokemon.currentHp || 0) / (pokemon?.stats?.hp || pokemon.currentHp || 1)) * 100))),
    statusText: pokemon.currentHp > 0 ? tr('可战斗', 'Ready') : tr('已倒下', 'Fainted'),
    conditionLabels: conditionLabels(pokemon)
  }))
}

function hpWidth(pokemon) {
  const maxHp = pokemon.maxHp || 1
  const currentHp = Math.max(0, pokemon.currentHp || 0)
  return `${Math.max(0, Math.min(100, (currentHp / maxHp) * 100))}%`
}

function formatTypes(types = []) {
  return (types || []).map((type) => tr(type.name || type.name_zh || `属性${type.type_id}`, type.name_en || type.name || `Type ${type.type_id}`)).join(' / ') || tr('未知属性', 'Unknown type')
}

function conditionLabels(pokemon = {}) {
  if (!pokemon || pokemon.currentHp <= 0) {
    return []
  }
  const labels = []
  if (pokemon.condition === 'paralysis') {
    labels.push(tr('麻痹', 'Paralysis'))
  }
  if (pokemon.condition === 'burn') {
    labels.push(tr('灼伤', 'Burn'))
  }
  if (pokemon.condition === 'sleep') {
    labels.push(tr(`睡眠 ${pokemon.sleepTurns || 0}`, `Sleep ${pokemon.sleepTurns || 0}`))
  }
  if ((pokemon.tauntTurns || 0) > 0) {
    labels.push(tr(`挑衅 ${pokemon.tauntTurns}`, `Taunt ${pokemon.tauntTurns}`))
  }
  return labels
}
</script>

<style scoped>
.battle-arena {
  background:
    radial-gradient(circle at top left, rgba(224, 242, 254, 0.7), transparent 20%),
    radial-gradient(circle at top right, rgba(254, 226, 226, 0.5), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
}

@media (max-width: 640px) {
  .battle-arena {
    background:
      radial-gradient(circle at top left, rgba(224, 242, 254, 0.5), transparent 25%),
      linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  }
}
</style>
