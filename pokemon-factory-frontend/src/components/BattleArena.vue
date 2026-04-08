<template>
  <div class="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-slate-900">
          战场
        </h2>
        <p class="mt-1 text-sm text-slate-500">
          当前展示双方双打在场信息和逐回合事件。
        </p>
      </div>
      <div
        v-if="summary"
        class="grid gap-2 text-sm sm:grid-cols-2"
      >
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-slate-500">
            规则
          </div>
          <div class="font-semibold text-slate-900">
            {{ summary.format || 'vgc-doubles' }}
          </div>
        </div>
        <div class="rounded-xl bg-slate-50 px-4 py-3">
          <div class="text-slate-500">
            当前回合
          </div>
          <div class="font-semibold text-slate-900">
            {{ summary.currentRound || 0 }} / {{ summary.roundLimit || '-' }}
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
        我方顺风 {{ summary.fieldEffects.playerTailwindTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.opponentTailwindTurns > 0"
        class="rounded-full bg-rose-50 px-3 py-1 text-xs text-rose-700"
      >
        对手顺风 {{ summary.fieldEffects.opponentTailwindTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.trickRoomTurns > 0"
        class="rounded-full bg-violet-50 px-3 py-1 text-xs text-violet-700"
      >
        戏法空间 {{ summary.fieldEffects.trickRoomTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.rainTurns > 0"
        class="rounded-full bg-cyan-50 px-3 py-1 text-xs text-cyan-700"
      >
        下雨 {{ summary.fieldEffects.rainTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.sunTurns > 0"
        class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
      >
        大晴天 {{ summary.fieldEffects.sunTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.electricTerrainTurns > 0"
        class="rounded-full bg-yellow-50 px-3 py-1 text-xs text-yellow-700"
      >
        电气场地 {{ summary.fieldEffects.electricTerrainTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.psychicTerrainTurns > 0"
        class="rounded-full bg-purple-50 px-3 py-1 text-xs text-purple-700"
      >
        精神场地 {{ summary.fieldEffects.psychicTerrainTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.playerReflectTurns > 0"
        class="rounded-full bg-sky-50 px-3 py-1 text-xs text-sky-700"
      >
        我方反射壁 {{ summary.fieldEffects.playerReflectTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.opponentReflectTurns > 0"
        class="rounded-full bg-rose-50 px-3 py-1 text-xs text-rose-700"
      >
        对手反射壁 {{ summary.fieldEffects.opponentReflectTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.playerLightScreenTurns > 0"
        class="rounded-full bg-indigo-50 px-3 py-1 text-xs text-indigo-700"
      >
        我方光墙 {{ summary.fieldEffects.playerLightScreenTurns }} 回合
      </span>
      <span
        v-if="summary.fieldEffects.opponentLightScreenTurns > 0"
        class="rounded-full bg-fuchsia-50 px-3 py-1 text-xs text-fuchsia-700"
      >
        对手光墙 {{ summary.fieldEffects.opponentLightScreenTurns }} 回合
      </span>
    </div>

    <div
      v-if="typeof highlightIndex !== 'undefined' && highlightIndex >= 0"
      class="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700"
    >
      已替换玩家队伍中的第 {{ highlightIndex + 1 }} 只宝可梦
    </div>

    <template v-if="summary">
      <div class="grid gap-4 lg:grid-cols-2">
        <section class="rounded-2xl border border-slate-200 p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="font-semibold text-slate-900">
              玩家队伍
            </h3>
            <span class="text-sm text-slate-500">剩余 {{ summary.playerRemaining || 0 }}</span>
          </div>
          <div class="space-y-3">
            <article
              v-for="pokemon in buildCards(summary.playerTeam, summary.playerActiveSlots)"
              :key="`player-${pokemon.index}`"
              :class="['rounded-xl border p-4', pokemon.active ? 'border-blue-400 bg-blue-50' : 'border-slate-200 bg-slate-50']"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <div class="font-semibold text-slate-900">
                    {{ pokemon.name }}
                  </div>
                  <div class="text-xs text-slate-500">
                    HP {{ pokemon.currentHp }}/{{ pokemon.maxHp }} · {{ pokemon.statusText }}
                  </div>
                </div>
                <div class="text-xs text-slate-500">
                  {{ formatTypes(pokemon.types) }}
                </div>
              </div>
              <div
                v-if="pokemon.conditionLabels.length"
                class="mt-2 flex flex-wrap gap-2"
              >
                <span class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700">
                  {{ pokemon.conditionLabels[0] }}
                </span>
                <span
                  v-for="label in pokemon.conditionLabels.slice(1)"
                  :key="label"
                  class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
                >
                  {{ label }}
                </span>
              </div>
              <div class="mt-2 h-2 overflow-hidden rounded-full bg-slate-200">
                <div
                  class="h-full rounded-full bg-blue-500"
                  :style="{ width: hpWidth(pokemon) }"
                />
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <span
                  v-for="move in pokemon.moves"
                  :key="move.name_en || move.name"
                  class="rounded-full bg-white px-3 py-1 text-xs text-slate-600 shadow-sm"
                >
                  {{ move.name || move.name_en }}
                </span>
              </div>
            </article>
          </div>
        </section>

        <section class="rounded-2xl border border-slate-200 p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="font-semibold text-slate-900">
              对手队伍
            </h3>
            <span class="text-sm text-slate-500">剩余 {{ summary.opponentRemaining || 0 }}</span>
          </div>
          <div class="space-y-3">
            <article
              v-for="pokemon in buildCards(summary.opponentTeam, summary.opponentActiveSlots)"
              :key="`opponent-${pokemon.index}`"
              :class="['rounded-xl border p-4', pokemon.active ? 'border-rose-400 bg-rose-50' : 'border-slate-200 bg-slate-50']"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <div class="font-semibold text-slate-900">
                    {{ pokemon.name }}
                  </div>
                  <div class="text-xs text-slate-500">
                    HP {{ pokemon.currentHp }}/{{ pokemon.maxHp }} · {{ pokemon.statusText }}
                  </div>
                </div>
                <div class="text-xs text-slate-500">
                  {{ formatTypes(pokemon.types) }}
                </div>
              </div>
              <div
                v-if="pokemon.conditionLabels.length"
                class="mt-2 flex flex-wrap gap-2"
              >
                <span class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700">
                  {{ pokemon.conditionLabels[0] }}
                </span>
                <span
                  v-for="label in pokemon.conditionLabels.slice(1)"
                  :key="label"
                  class="rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-700"
                >
                  {{ label }}
                </span>
              </div>
              <div class="mt-2 h-2 overflow-hidden rounded-full bg-slate-200">
                <div
                  class="h-full rounded-full bg-rose-500"
                  :style="{ width: hpWidth(pokemon) }"
                />
              </div>
            </article>
          </div>
        </section>
      </div>

      <section class="rounded-2xl border border-slate-200 p-4">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="font-semibold text-slate-900">
            回合日志
          </h3>
          <span class="text-sm text-slate-500">
            {{ summary.status === 'completed' ? `胜者：${summary.winner}` : '战斗进行中' }}
          </span>
        </div>
        <div
          v-if="summary.rounds?.length"
          class="space-y-3"
        >
          <article
            v-for="(round, roundIndex) in summary.rounds"
            :key="`${round.round}-${roundIndex}`"
            class="rounded-xl bg-slate-50 p-4"
          >
            <div class="font-semibold text-slate-900">
              {{ round.round === 0 ? '入场阶段' : `第 ${round.round} 回合` }}
            </div>
            <div class="mt-2 space-y-2">
              <div
                v-for="event in round.events || []"
                :key="event"
                class="text-sm text-slate-700"
              >
                {{ event }}
              </div>
            </div>
          </article>
        </div>
        <div
          v-else
          class="text-sm text-slate-500"
        >
          暂无回合日志
        </div>
      </section>
    </template>

    <div
      v-else
      class="rounded-2xl bg-slate-50 px-4 py-8 text-center text-sm text-slate-500"
    >
      暂无对战数据
    </div>
  </div>
</template>

<script setup>
defineProps({
  summary: {
    type: Object,
    default: null
  },
  highlightIndex: {
    type: Number,
    default: -1
  }
})

function buildCards(team = [], activeSlots = []) {
  return (team || []).map((pokemon, index) => ({
    ...pokemon,
    index,
    active: (activeSlots || []).includes(index),
    name: pokemon.name || pokemon.name_en || `宝可梦 ${index + 1}`,
    currentHp: pokemon.currentHp || 0,
    maxHp: pokemon?.stats?.hp || pokemon.currentHp || 1,
    statusText: pokemon.currentHp > 0 ? '可战斗' : '已倒下',
    conditionLabels: conditionLabels(pokemon)
  }))
}

function hpWidth(pokemon) {
  const maxHp = pokemon.maxHp || 1
  const currentHp = Math.max(0, pokemon.currentHp || 0)
  return `${Math.max(0, Math.min(100, (currentHp / maxHp) * 100))}%`
}

function formatTypes(types = []) {
  return (types || []).map((type) => type.name || type.name_zh || `属性${type.type_id}`).join(' / ') || '未知属性'
}

function conditionLabels(pokemon = {}) {
  if (!pokemon || pokemon.currentHp <= 0) {
    return []
  }
  const labels = []
  if (pokemon.condition === 'paralysis') {
    labels.push('麻痹')
  }
  if (pokemon.condition === 'burn') {
    labels.push('灼伤')
  }
  if (pokemon.condition === 'sleep') {
    labels.push(`睡眠 ${pokemon.sleepTurns || 0}`)
  }
  if ((pokemon.tauntTurns || 0) > 0) {
    labels.push(`挑衅 ${pokemon.tauntTurns}`)
  }
  return labels
}
</script>
