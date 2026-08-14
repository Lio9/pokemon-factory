<template>
  <div class="battle-arena space-y-4 rounded-[24px] border border-slate-200/80 bg-white/95 p-4 shadow-[0_24px_90px_-54px_rgba(15,23,42,0.5)] backdrop-blur sm:rounded-[28px] sm:p-6">
    <!-- 顶部信息 -->
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h2 class="text-xl font-black tracking-tight text-slate-900">
          {{ tr('战场', 'Battlefield') }}
        </h2>
        <p class="mt-1 text-sm leading-6 text-slate-500">
          {{ tr('战斗中的宝可梦、状态与逐回合事件。', 'Active Pokemon, statuses, and round-by-round events.') }}
        </p>
      </div>
      <div
        v-if="summary"
        class="grid gap-2 text-sm sm:grid-cols-3"
      >
        <div class="rounded-2xl bg-slate-50 px-4 py-3 shadow-sm">
          <div class="text-slate-500">{{ tr('规则', 'Format') }}</div>
          <div class="font-semibold text-slate-900">{{ summary.format || 'vgc-doubles' }}</div>
        </div>
        <div class="rounded-2xl bg-slate-50 px-4 py-3 shadow-sm">
          <div class="text-slate-500">{{ tr('回合', 'Round') }}</div>
          <div class="font-semibold text-slate-900">{{ summary.currentRound || 0 }} / {{ summary.roundLimit || '-' }}</div>
        </div>
        <div
          class="rounded-2xl px-4 py-3 shadow-sm"
          :class="statusChipClass"
        >
          <div class="text-slate-500">{{ tr('状态', 'Status') }}</div>
          <div
            class="font-semibold"
            :class="statusTextClass"
          >{{ statusText || tr('未开始', 'Not started') }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 正作风格战场 ===== -->
    <template v-if="summary">
      <div class="battle-stage relative overflow-hidden rounded-[24px] border-2 border-slate-700/60 select-none">
        <!-- 背景层（正作风格天空+草地） -->
        <div class="battle-bg absolute inset-0" />
        <!-- 天气视觉层 -->
        <div
          v-if="activeWeather"
          class="pointer-events-none absolute inset-0"
          :class="weatherClass"
        >
          <div class="weather-overlay absolute inset-0" />
          <!-- 雨滴动画 -->
          <div
            v-if="activeWeather === 'rain'"
            class="rain-layer absolute inset-0"
          />
        </div>

        <!-- 场地效果层 -->
        <div
          v-if="hasFieldEffects"
          class="absolute inset-x-0 top-2 z-20 flex flex-wrap justify-center gap-1.5 px-2"
        >
          <span
            v-for="(effect, key) in fieldEffectChips"
            :key="key"
            class="field-chip"
            :class="effect.tone"
          >{{ effect.label }}</span>
        </div>

        <!-- ===== 对手区（右上） ===== -->
        <div class="relative z-10 flex justify-end pt-6 pr-4 sm:pt-8 sm:pr-8">
          <div
            v-for="(mon, idx) in opponentActiveMons"
            :key="`opp-${mon.index}-${idx}`"
            class="opponent-zone ml-3"
          >
            <!-- 对手信息框 -->
            <div class="opp-info-box mb-2">
              <div class="flex items-center gap-2">
                <span class="text-[13px] font-extrabold text-white drop-shadow">{{ mon.name || mon.name_en }}</span>
                <span
                  v-if="mon.level"
                  class="text-[11px] font-bold text-sky-200"
                >Lv.{{ mon.level }}</span>
                <span
                  v-if="mon.gender === 'female'"
                  class="text-[12px] font-bold text-pink-300"
                >♀</span>
                <span
                  v-else-if="mon.gender === 'male'"
                  class="text-[12px] font-bold text-sky-300"
                >♂</span>
              </div>
              <!-- HP 条（Showdown 分段色） -->
              <div class="opp-hp-track mt-1">
                <div
                  class="opp-hp-fill"
                  :class="hpTone(mon)"
                  :style="{ width: hpWidth(mon) }"
                />
              </div>
              <!-- 对手 HP 百分比（正作风格，隐藏精确值） -->
              <div class="mt-0.5 text-right text-[10px] font-bold tabular-nums text-white/80">
                {{ mon.hpPercent }}%
              </div>
              <div class="mt-0.5 flex items-center justify-between">
                <span
                  v-if="mon.abilityName"
                  class="text-[10px] font-semibold text-white/70 truncate max-w-[80px]"
                >{{ mon.abilityName }}</span>
                <span
                  v-if="mon.heldItem"
                  class="text-[10px] font-semibold text-amber-200/80 truncate max-w-[80px]"
                >{{ mon.heldItem }}</span>
              </div>
              <!-- 状态徽章 -->
              <div
                v-if="conditionBadges(mon).length"
                class="mt-1 flex flex-wrap gap-1"
              >
                <span
                  v-for="badge in conditionBadges(mon)"
                  :key="badge.label"
                  class="rounded px-1 py-px text-[9px] font-bold text-white"
                  :style="{ backgroundColor: badge.color }"
                >{{ badge.label }}</span>
              </div>
            </div>

            <!-- 对手精灵（正面，可点击作为目标） -->
            <div class="relative">
              <button
                type="button"
                class="block cursor-pointer transition-transform hover:scale-105 focus:outline-none"
                :class="canTarget ? '' : 'cursor-default pointer-events-none'"
                :title="canTarget ? tr('点击选择为目标', 'Click to target') : ''"
                @click="onTargetClick(mon)"
              >
                <img
                  :src="spriteUrl(mon, false)"
                  :alt="mon.name"
                  class="opp-sprite h-28 w-28 object-contain sm:h-36 sm:w-36"
                  :class="[mon.fainted ? 'sprite-fainted' : 'sprite-idle', targetHighlight(mon) ? 'sprite-target' : '']"
                  @error="onSpriteError($event, mon, false)"
                >
              </button>
              <!-- 倒下动画 -->
              <div
                v-if="mon.fainted"
                class="absolute inset-0 flex items-end justify-center pb-2 pointer-events-none"
              >
                <div class="text-xl font-black text-white/80 drop-shadow">✕</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ===== 中央战场分割线 + 我方区（左下） ===== -->
        <div class="relative z-10 mt-2 flex items-end justify-start px-4 pb-4 sm:px-8 sm:pb-6">
          <div
            v-for="(mon, idx) in playerActiveMons"
            :key="`player-${mon.index}-${idx}`"
            class="player-zone mr-4"
          >
            <!-- 我方精灵（背面） -->
            <div class="flex flex-col items-center">
              <div class="relative">
                <img
                  :src="spriteUrl(mon, true)"
                  :alt="mon.name"
                  class="player-sprite h-28 w-28 object-contain sm:h-36 sm:w-36"
                  :class="[mon.fainted ? 'sprite-fainted' : 'sprite-idle']"
                  @error="onSpriteError($event, mon, true)"
                >
                <div
                  v-if="mon.fainted"
                  class="absolute inset-0 flex items-end justify-center pb-2"
                >
                  <div class="text-xl font-black text-white/80 drop-shadow">✕</div>
                </div>
              </div>
              <!-- 我方信息框 -->
              <div class="player-info-box mt-2">
                <div class="flex items-center gap-2">
                  <span class="text-[13px] font-extrabold text-white drop-shadow">{{ mon.name || mon.name_en }}</span>
                  <span
                    v-if="mon.level"
                    class="text-[11px] font-bold text-sky-200"
                  >Lv.{{ mon.level }}</span>
                  <span
                    v-if="mon.gender === 'female'"
                    class="text-[12px] font-bold text-pink-300"
                  >♀</span>
                  <span
                    v-else-if="mon.gender === 'male'"
                    class="text-[12px] font-bold text-sky-300"
                  >♂</span>
                </div>
                <div class="player-hp-track mt-1">
                  <div
                    class="player-hp-fill"
                    :class="hpTone(mon)"
                    :style="{ width: hpWidth(mon) }"
                  />
                </div>
                <!-- 我方 HP 精确数值（正作风格） -->
                <div class="mt-0.5 text-center text-[11px] font-black tabular-nums text-white drop-shadow">
                  {{ mon.currentHp }} / {{ mon.maxHp || '?' }}
                </div>
                <div class="mt-0.5 flex items-center justify-between gap-2">
                  <span
                    v-if="mon.abilityName"
                    class="text-[10px] font-semibold text-white/70 truncate max-w-[90px]"
                  >{{ mon.abilityName }}</span>
                  <span
                    v-if="mon.heldItem"
                    class="text-[10px] font-semibold text-amber-200/80 truncate max-w-[90px]"
                  >{{ mon.heldItem }}</span>
                </div>
                <!-- 状态徽章 -->
                <div
                  v-if="conditionBadges(mon).length"
                  class="mt-1 flex flex-wrap gap-1"
                >
                  <span
                    v-for="badge in conditionBadges(mon)"
                    :key="badge.label"
                    class="rounded px-1 py-px text-[9px] font-bold text-white"
                    :style="{ backgroundColor: badge.color }"
                  >{{ badge.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部阴影 -->
        <div class="pointer-events-none absolute inset-x-0 bottom-0 h-10 bg-gradient-to-t from-black/25 to-transparent" />
      </div>

      <!-- 替补区 -->
      <div class="grid gap-3 lg:grid-cols-2">
        <div class="rounded-2xl border border-slate-200 bg-slate-50/80 p-3">
          <div class="mb-2 text-[11px] font-semibold text-slate-500">
            {{ tr('对手后备', 'Opponent bench') }} · {{ tr('剩余 {count}', 'Remaining {count}', { count: summary.opponentRemaining || 0 }) }}
          </div>
          <div class="flex flex-wrap gap-1.5">
            <div
              v-for="mon in opponentBench"
              :key="`opp-bench-${mon.index}`"
              class="bench-pill"
              :class="mon.fainted ? 'bench-pill-fainted' : 'bench-pill-alive'"
              :title="mon.name"
            >
              <img
                :src="spriteUrl(mon, false)"
                :alt="mon.name"
                class="h-7 w-7 object-contain"
                :class="mon.fainted ? 'grayscale opacity-50' : ''"
                @error="onSpriteError($event, mon, false)"
              >
              <span class="bench-pill-name">{{ mon.name }}</span>
              <span class="bench-pill-hp">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</span>
            </div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-slate-50/80 p-3">
          <div class="mb-2 text-[11px] font-semibold text-slate-500">
            {{ tr('我方后备', 'Your bench') }} · {{ tr('剩余 {count}', 'Remaining {count}', { count: summary.playerRemaining || 0 }) }}
          </div>
          <div class="flex flex-wrap gap-1.5">
            <div
              v-for="mon in playerBench"
              :key="`player-bench-${mon.index}`"
              class="bench-pill"
              :class="mon.fainted ? 'bench-pill-fainted' : 'bench-pill-alive'"
              :title="mon.name"
            >
              <img
                :src="spriteUrl(mon, true)"
                :alt="mon.name"
                class="h-7 w-7 object-contain"
                :class="mon.fainted ? 'grayscale opacity-50' : ''"
                @error="onSpriteError($event, mon, true)"
              >
              <span class="bench-pill-name">{{ mon.name }}</span>
              <span class="bench-pill-hp">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 替换高亮提示 -->
      <div
        v-if="typeof highlightIndex !== 'undefined' && highlightIndex >= 0"
        class="rounded-2xl bg-blue-50 px-4 py-3 text-sm text-blue-700"
      >
        {{ tr('已替换玩家队伍中的第 {slot} 只宝可梦', 'Replaced Pokemon in player slot {slot}', { slot: highlightIndex + 1 }) }}
      </div>
    </template>

    <!-- ===== 正作风格最新消息对话框 ===== -->
    <div
      v-if="latestEvent"
      class="relative overflow-hidden rounded-2xl border-2 border-slate-300 bg-[linear-gradient(180deg,#ffffff,#f8fafc)] p-4 shadow-[0_6px_16px_-8px_rgba(0,0,0,0.25)]"
    >
      <div class="flex items-center gap-2">
        <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-800 text-white shadow">
          <svg
            viewBox="0 0 24 24"
            class="h-4 w-4"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          ><path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
          /></svg>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[10px] font-bold uppercase tracking-widest text-slate-400">
            {{ tr('最新消息', 'Latest') }}
          </div>
          <div class="text-sm font-semibold leading-6 text-slate-800">
            {{ latestEvent }}
          </div>
        </div>
        <span
          v-if="summary?.currentRound"
          class="shrink-0 rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-500"
        >{{ tr('第 {n} 回合', 'Round {n}', { n: summary.currentRound }) }}</span>
      </div>
    </div>

    <!-- ===== 回合消息流 ===== -->
    <section class="rounded-[24px] border border-slate-200/80 bg-slate-50/70 p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="font-semibold text-slate-900">{{ tr('回合日志', 'Round log') }}</h3>
        <span class="text-sm text-slate-500">
          {{ summary?.status === 'completed' ? winnerLabel : tr('战斗进行中', 'Battle in progress') }}
        </span>
      </div>
      <div
        v-if="summary?.rounds?.length"
        class="space-y-3"
      >
        <article
          v-for="(round, roundIndex) in summary.rounds"
          :key="`${round.round}-${roundIndex}`"
          class="rounded-2xl bg-white p-4 shadow-sm"
          :class="roundIndex === summary.rounds.length - 1 && summary.status !== 'completed' ? 'round-current border-2' : ''"
        >
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="font-semibold text-slate-900">
                {{ round.round === 0 ? tr('入场阶段', 'Entry phase') : tr(`第 ${round.round} 回合`, `Round ${round.round}`) }}
              </div>
              <span
                v-if="roundTime(round)"
                class="text-[11px] font-medium text-slate-400 tabular-nums"
              >{{ roundTime(round) }}</span>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
              {{ tr('{count} 条事件', '{count} events', { count: (round.events || []).length }) }}
            </span>
          </div>

          <!-- 行动摘要 -->
          <div
            v-if="round.actions?.length"
            class="mt-3 space-y-1"
          >
            <div
              v-for="(action, ai) in round.actions"
              :key="`action-${ai}`"
              class="flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-xs"
              :class="action.side === 'player' ? 'bg-blue-50/60' : 'bg-rose-50/60'"
            >
              <span
                class="h-1.5 w-1.5 shrink-0 rounded-full"
                :class="action.side === 'player' ? 'bg-blue-500' : 'bg-rose-500'"
              />
              <span
                class="shrink-0 font-bold"
                :class="action.side === 'player' ? 'text-blue-700' : 'text-rose-700'"
              >{{ action.side === 'player' ? tr('我方', 'You') : tr('对手', 'Foe') }}</span>
              <span class="font-semibold text-slate-800">{{ action.actor }}</span>
              <template v-if="action.actionType === 'switch'">
                <span class="text-slate-400">→</span>
                <span class="font-semibold text-slate-700">{{ action.switchTo }}</span>
              </template>
              <template v-else-if="action.move">
                <span class="text-slate-400">{{ tr('使用', 'used') }}</span>
                <span class="font-semibold text-slate-800">{{ action.move }}</span>
                <template v-if="action.target && action.targetFieldSlot != null">
                  <span class="text-slate-400">→</span>
                  <span class="text-slate-600">{{ action.target }}</span>
                </template>
                <span
                  v-if="action.damage > 0"
                  class="ml-auto font-bold tabular-nums"
                  :class="action.critical ? 'text-amber-600' : 'text-rose-600'"
                >-{{ action.damage }}{{ action.critical ? ' 💥' : '' }}</span>
                <span
                  v-else-if="action.result === 'failed' || action.result === 'miss'"
                  class="ml-auto font-semibold text-slate-400"
                >{{ action.result === 'miss' ? tr('落空', 'Miss') : tr('失败', 'Failed') }}</span>
                <span
                  v-else-if="action.damage === 0 && action.result === 'hit'"
                  class="ml-auto font-semibold text-slate-400"
                >0</span>
              </template>
              <span
                v-if="action.hitCount > 1"
                class="rounded-full bg-slate-100 px-1.5 py-0.5 text-[10px] font-bold text-slate-500"
              >×{{ action.hitCount }}</span>
            </div>
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

    <div
      v-if="!summary"
      class="rounded-2xl bg-slate-50 px-4 py-8 text-center text-sm text-slate-500"
    >
      {{ tr('暂无对战数据', 'No battle data yet') }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'
import { sprites } from '../services/sprites'

const { translate: tr } = useLocale()

const emit = defineEmits(['target-select'])

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
  },
  targetFieldSlot: {
    type: [Number, String],
    default: null
  },
  canTarget: {
    type: Boolean,
    default: false
  }
})

// 点击对手精灵选择目标（由父组件决定是否启用）
function onTargetClick(mon) {
  if (!props.canTarget) return
  emit('target-select', mon.fieldSlot)
}

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

// 胜利方标签（本地化）
const winnerLabel = computed(() => {
  if (props.summary?.winner === 'player') return tr('胜者：我方', 'Winner: You')
  if (props.summary?.winner === 'opponent') return tr('胜者：对手', 'Winner: Opponent')
  return tr(`胜者：${props.summary?.winner}`, `Winner: ${props.summary?.winner}`)
})

// 回合时间戳
function roundTime(round) {
  const ts = round?.ts || round?.created_at || round?.timestamp
  if (ts) {
    const date = new Date(ts)
    if (!Number.isNaN(date.getTime())) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
  }
  return ''
}

// 最新一条战斗消息（正作对话框）
const latestEvent = computed(() => {
  const rounds = props.summary?.rounds
  if (!rounds?.length) return ''
  const lastRound = rounds[rounds.length - 1]
  const events = lastRound?.events || []
  return events.length ? events[events.length - 1] : ''
})

// 当前天气（用于背景视觉）
const activeWeather = computed(() => {
  const fe = props.summary?.fieldEffects || {}
  if (Number(fe.rainTurns || 0) > 0) return 'rain'
  if (Number(fe.sunTurns || 0) > 0) return 'sun'
  if (Number(fe.sandTurns || 0) > 0) return 'sand'
  if (Number(fe.snowTurns || 0) > 0) return 'snow'
  return null
})

const weatherClass = computed(() => {
  switch (activeWeather.value) {
    case 'rain': return 'weather-rain'
    case 'sun': return 'weather-sun'
    case 'sand': return 'weather-sand'
    case 'snow': return 'weather-snow'
    default: return ''
  }
})

// ===== 数据加工 =====

// 精灵图：我方用背面图（正作风格），对手用正面图；本地优先，失败回退
function spriteUrl(mon, isPlayer) {
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  if (!id) return sprites.default
  return isPlayer ? sprites.pokemonBack(id) : sprites.pokemon(id)
}

// 精灵图加载失败回退：本地 → 远程（背面/正面分别回退）→ 默认
function onSpriteError(event, mon, isPlayer) {
  const img = event.target
  const src = img?.getAttribute('src') || ''
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  if (src.includes('/api/pokedex/images')) {
    img.src = isPlayer ? sprites.fallbackPokemonBack(id) : sprites.fallbackPokemon(id)
  } else if (src.includes('raw.githubusercontent')) {
    img.src = sprites.default
  }
}

// 目标高亮（供点击选目标使用）
function targetHighlight(mon) {
  const target = props.targetFieldSlot
  return target !== null && target !== undefined && Number(target) === Number(mon.fieldSlot)
}

// 宝可梦对象加工：补全展示字段
function buildMons(team = [], activeSlots = []) {
  return (team || []).map((pokemon, index) => {
    const maxHp = pokemon?.stats?.hp || pokemon?.maxHp || null
    const currentHp = Math.max(0, Number(pokemon?.currentHp ?? 0))
    const ability = pokemon?.ability
    return {
      ...pokemon,
      index,
      active: (activeSlots || []).includes(index),
      name: pokemon.name || pokemon.name_en || tr(`宝可梦 ${index + 1}`, `Pokemon ${index + 1}`),
      level: pokemon.level || 50,
      abilityName: typeof ability === 'string' ? ability : (ability?.name_en || ability?.name || ''),
      currentHp,
      maxHp,
      fainted: currentHp <= 0,
      hpPercent: (() => {
        if (!maxHp || maxHp <= 0) return currentHp > 0 ? 100 : 0
        return Math.max(0, Math.min(100, Math.round((currentHp / maxHp) * 100)))
      })()
    }
  })
}

const playerMons = computed(() => buildMons(props.summary?.playerTeam, props.summary?.playerActiveSlots))
const opponentMons = computed(() => buildMons(props.summary?.opponentTeam, props.summary?.opponentActiveSlots))
const playerActiveMons = computed(() => playerMons.value.filter((m) => m.active))
const opponentActiveMons = computed(() => opponentMons.value.filter((m) => m.active))
const playerBench = computed(() => playerMons.value.filter((m) => !m.active))
const opponentBench = computed(() => opponentMons.value.filter((m) => !m.active))

function hpWidth(mon) {
  if (!mon.maxHp || mon.maxHp <= 0) return `${mon.fainted ? 0 : 100}%`
  return `${Math.max(0, Math.min(100, (mon.currentHp / mon.maxHp) * 100))}%`
}

// HP 条颜色（正作/Showdown 风格）
function hpTone(mon) {
  const pct = mon.hpPercent
  if (pct <= 0) return 'empty'
  if (pct <= 20) return 'low'
  if (pct <= 50) return 'mid'
  return 'high'
}

// 状态徽章（Showdown 状态色）
const CONDITION_COLORS = {
  paralysis: '#d9a619',
  burn: '#e87a2b',
  freeze: '#7dc4e8',
  sleep: '#7b6f9e',
  poison: '#9b4f9b',
  toxic: '#9b4f9b',
  confusion: '#e87a2b',
  taunt: '#d9a619'
}

const CONDITION_LABELS = {
  paralysis: tr('麻痹', 'Par'),
  burn: tr('灼伤', 'BRN'),
  freeze: tr('冰冻', 'FRZ'),
  sleep: tr('睡眠', 'SLP'),
  poison: tr('中毒', 'PSN'),
  toxic: tr('剧毒', 'TOX'),
  confusion: tr('混乱', 'Conf'),
  taunt: tr('挑衅', 'Taunt')
}

function conditionBadges(mon) {
  if (mon.fainted || !mon) return []
  const badges = []
  const cond = mon.condition || mon.status
  if (cond && CONDITION_COLORS[cond]) {
    badges.push({ label: CONDITION_LABELS[cond] || cond, color: CONDITION_COLORS[cond] })
  }
  if ((mon.tauntTurns || 0) > 0) {
    badges.push({ label: tr('挑衅', 'Taunt'), color: CONDITION_COLORS.taunt })
  }
  if (mon.confused) {
    badges.push({ label: tr('混乱', 'Conf'), color: CONDITION_COLORS.confusion })
  }
  if (mon.terastallized) {
    badges.push({ label: tr('太晶', 'Tera'), color: '#6366f1' })
  }
  if (mon.dynamaxed) {
    badges.push({ label: tr('极巨', 'Max'), color: '#dc2626' })
  }
  if (mon.megaEvolved) {
    badges.push({ label: tr('Mega', 'Mega'), color: '#7c3aed' })
  }
  return badges
}

// ===== 场地效果 =====
const hasFieldEffects = computed(() => {
  const fe = props.summary?.fieldEffects
  if (!fe) return false
  return Object.keys(fe).some((k) => Number(fe[k] || 0) > 0 || fe[k] === true)
})

const fieldEffectChips = computed(() => {
  const fe = props.summary?.fieldEffects || {}
  const chips = []
  const push = (label, key, tone) => {
    if (Number(fe[key] || 0) > 0 || fe[key] === true) {
      chips.push({ label, tone })
    }
  }
  push(tr('顺风', 'Tailwind'), 'playerTailwindTurns', 'tone-blue')
  push(tr('对手顺风', 'Opp. TW'), 'opponentTailwindTurns', 'tone-rose')
  push(tr('戏法空间', 'Trick Room'), 'trickRoomTurns', 'tone-violet')
  push(tr('雨天', 'Rain'), 'rainTurns', 'tone-cyan')
  push(tr('晴天', 'Sun'), 'sunTurns', 'tone-amber')
  push(tr('沙暴', 'Sand'), 'sandTurns', 'tone-orange')
  push(tr('雪天', 'Snow'), 'snowTurns', 'tone-sky')
  push(tr('电气场地', 'E-Terrain'), 'electricTerrainTurns', 'tone-yellow')
  push(tr('精神场地', 'P-Terrain'), 'psychicTerrainTurns', 'tone-purple')
  push(tr('青草场地', 'G-Terrain'), 'grassyTerrainTurns', 'tone-green')
  push(tr('薄雾场地', 'M-Terrain'), 'mistyTerrainTurns', 'tone-pink')
  push(tr('我方反射壁', 'Reflect'), 'playerReflectTurns', 'tone-blue')
  push(tr('对手反射壁', 'Opp. Ref'), 'opponentReflectTurns', 'tone-rose')
  push(tr('我方光墙', 'L.Screen'), 'playerLightScreenTurns', 'tone-blue')
  push(tr('对手光墙', 'Opp. LS'), 'opponentLightScreenTurns', 'tone-rose')
  push(tr('我方极光幕', 'Aurora'), 'playerAuroraVeilTurns', 'tone-blue')
  push(tr('对手极光幕', 'Opp. AV'), 'opponentAuroraVeilTurns', 'tone-rose')
  push(tr('我方隐形岩', 'S.Rock'), 'playerStealthRock', 'tone-gray')
  push(tr('对手隐形岩', 'Opp. SR'), 'opponentStealthRock', 'tone-gray')
  if (Number(fe.playerSpikesLayers || 0) > 0) chips.push({ label: `撒菱 ${fe.playerSpikesLayers}/3`, tone: 'tone-green' })
  if (Number(fe.opponentSpikesLayers || 0) > 0) chips.push({ label: `对手撒菱 ${fe.opponentSpikesLayers}/3`, tone: 'tone-red' })
  if (Number(fe.playerToxicSpikesLayers || 0) > 0) chips.push({ label: `毒菱 ${fe.playerToxicSpikesLayers}/2`, tone: 'tone-purple' })
  if (Number(fe.opponentToxicSpikesLayers || 0) > 0) chips.push({ label: `对手毒菱 ${fe.opponentToxicSpikesLayers}/2`, tone: 'tone-pink' })
  return chips
})
</script>

<style scoped>
/* ===== 战场背景（正作风格：天空渐变 + 草地） ===== */
.battle-bg {
  background:
    radial-gradient(circle at 20% 18%, rgba(255,255,255,0.35) 0, transparent 30%),
    radial-gradient(circle at 80% 12%, rgba(135,206,250,0.5) 0, transparent 35%),
    linear-gradient(180deg,
      #6eb5e8 0%,
      #8ec9f0 32%,
      #a8d8a8 52%,
      #6fae6f 72%,
      #4a8a4a 100%);
}

/* 战斗舞台 */
.battle-stage {
  min-height: 380px;
  box-shadow: inset 0 2px 12px rgba(0,0,0,0.15), 0 10px 30px -12px rgba(0,0,0,0.3);
}

@media (max-width: 640px) {
  .battle-stage {
    min-height: 320px;
  }
}

/* ===== 精灵动画 ===== */
.sprite-idle {
  image-rendering: pixelated;
  animation: sprite-enter 0.45s ease-out;
  transition: transform 0.3s ease, filter 0.3s ease, opacity 0.3s ease;
}

@keyframes sprite-enter {
  from { opacity: 0; transform: translateY(24px) scale(0.9); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.sprite-fainted {
  image-rendering: pixelated;
  filter: grayscale(1) brightness(0.5);
  transform: translateY(30px) rotate(75deg);
  opacity: 0.55;
  transition: all 0.8s ease;
}

.sprite-target {
  filter: drop-shadow(0 0 14px rgba(249, 115, 22, 0.9));
  animation: target-pulse 1s ease-in-out infinite;
}

@keyframes target-pulse {
  0%, 100% { filter: drop-shadow(0 0 8px rgba(249, 115, 22, 0.7)); }
  50% { filter: drop-shadow(0 0 20px rgba(249, 115, 22, 1)); }
}

/* 对手精灵略微漂浮 */
.opp-sprite {
  animation: float-soft 3.5s ease-in-out infinite;
}

@keyframes float-soft {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* ===== 正作信息框 ===== */
.opp-info-box,
.player-info-box {
  background: linear-gradient(160deg, rgba(30, 58, 138, 0.82), rgba(30, 41, 82, 0.88));
  border: 2px solid rgba(255,255,255,0.28);
  border-radius: 12px;
  padding: 6px 10px;
  min-width: 150px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.15);
}

.player-info-box {
  background: linear-gradient(160deg, rgba(3, 105, 161, 0.82), rgba(12, 74, 110, 0.88));
}

/* HP 条（正作/Showdown 分段色） */
.opp-hp-track,
.player-hp-track {
  position: relative;
  height: 12px;
  border-radius: 4px;
  background: #1e293b;
  border: 2px solid #334155;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.3), inset 0 1px 2px rgba(0,0,0,0.4);
}

.opp-hp-fill,
.player-hp-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.6s ease, background 0.6s ease;
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #4ade80, #16a34a);
}

.opp-hp-fill.high,
.player-hp-fill.high {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #4ade80, #16a34a);
}

.opp-hp-fill.mid,
.player-hp-fill.mid {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #fbbf24, #d97706);
}

.opp-hp-fill.low,
.player-hp-fill.low {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #f87171, #dc2626);
}

.opp-hp-fill.empty,
.player-hp-fill.empty {
  background: #64748b;
}

/* 场地效果徽章 */
.field-chip {
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 700;
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 1px 3px rgba(0,0,0,0.25);
}

.tone-blue { background: #dbeafe; color: #1d4ed8; }
.tone-rose { background: #ffe4e6; color: #be123c; }
.tone-violet { background: #ede9fe; color: #6d28d9; }
.tone-cyan { background: #cffafe; color: #0e7490; }
.tone-amber { background: #fef3c7; color: #b45309; }
.tone-orange { background: #ffedd5; color: #c2410c; }
.tone-sky { background: #e0f2fe; color: #0369a1; }
.tone-yellow { background: #fef9c3; color: #a16207; }
.tone-purple { background: #f3e8ff; color: #7e22ce; }
.tone-green { background: #dcfce7; color: #15803d; }
.tone-pink { background: #fce7f3; color: #be185d; }
.tone-emerald { background: #d1fae5; color: #047857; }
.tone-teal { background: #ccfbf1; color: #0f766e; }
.tone-gray { background: #f1f5f9; color: #475569; }
.tone-red { background: #fecaca; color: #b91c1c; }

/* 后备宝可梦胶囊 */
.bench-pill {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.6rem 0.25rem 0.3rem;
  border-radius: 9999px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.bench-pill-alive {
  box-shadow: 0 1px 2px rgba(0,0,0,0.08);
}

.bench-pill-fainted {
  opacity: 0.6;
  background: #f1f5f9;
}

.bench-pill-name {
  font-size: 0.7rem;
  font-weight: 700;
  color: #334155;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bench-pill-hp {
  font-size: 0.65rem;
  font-weight: 600;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

/* ===== 天气视觉层 ===== */
.weather-overlay {
  opacity: 0.35;
}

.weather-rain .weather-overlay {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.35), rgba(30, 64, 175, 0.4));
}

.weather-sun .weather-overlay {
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.3), rgba(249, 115, 22, 0.2));
}

.weather-sand .weather-overlay {
  background: linear-gradient(180deg, rgba(180, 120, 60, 0.4), rgba(146, 90, 45, 0.45));
}

.weather-snow .weather-overlay {
  background: linear-gradient(180deg, rgba(226, 240, 254, 0.5), rgba(203, 230, 248, 0.45));
}

/* 雨滴动画（CSS 生成斜雨） */
.rain-layer {
  background-image:
    repeating-linear-gradient(105deg, transparent 0 8px, rgba(200, 220, 255, 0.7) 8px 9px);
  animation: rain-fall 0.5s linear infinite;
  opacity: 0.5;
}

@keyframes rain-fall {
  from { background-position: 0 0; }
  to { background-position: -30px 60px; }
}

/* 当前回合高亮 */
.round-current {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.2), 0 8px 24px -8px rgba(99, 102, 241, 0.35);
}
</style>
