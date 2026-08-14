

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

    <!-- ===== Showdown 式战场主舞台 ===== -->
    <template v-if="summary">
      <div class="showdown-field relative overflow-hidden rounded-[24px] border border-slate-200 bg-[linear-gradient(180deg,#dfece4_0%,#c3d9c6_42%,#a8c4ad_100%)] p-4 sm:p-6">
        <!-- 场地纹理点缀 -->
        <div
          class="pointer-events-none absolute inset-0 opacity-40"
          style="background-image: radial-gradient(circle at 20% 25%, rgba(255,255,255,0.5) 0, transparent 26%), radial-gradient(circle at 78% 70%, rgba(255,255,255,0.45) 0, transparent 30%), radial-gradient(circle at 60% 15%, rgba(120,150,110,0.25) 0, transparent 22%);"
        />

        <!-- 对手信息（右上） -->
        <div class="relative flex items-end justify-end">
          <div
            v-for="(mon, idx) in opponentActiveMons"
            :key="`opp-sprite-${mon.index}-${idx}`"
            class="mb-1 flex flex-col items-center"
          >
            <div class="mb-1 flex items-center gap-1 rounded-full bg-black/25 px-2 py-0.5 text-[10px] font-bold text-white">
              <span
                v-if="mon.level"
                class="text-emerald-200"
              >Lv.{{ mon.level }}</span>
              <span>{{ mon.name }}</span>
            </div>
            <div class="sprite-wrap relative h-24 w-24 sm:h-28 sm:w-28">
              <img
                :src="spriteUrl(mon)"
                :alt="mon.name"
                class="h-full w-full object-contain drop-shadow-[0_6px_8px_rgba(0,0,0,0.35)] transition-all"
                :class="mon.fainted ? 'grayscale opacity-40' : ''"
                @error="onSpriteError"
              >
              <!-- 倒下标记 -->
              <div
                v-if="mon.fainted"
                class="absolute inset-0 flex items-center justify-center rounded-full"
              >
                <svg
                  viewBox="0 0 24 24"
                  class="h-8 w-8 text-rose-600 drop-shadow"
                ><path
                  fill="currentColor"
                  d="M7 7h10v10H7z"
                  transform="rotate(45 12 12)"
                /></svg>
              </div>
            </div>
            <!-- 状态徽章 -->
            <div
              v-if="conditionBadges(mon).length"
              class="mt-1 flex flex-wrap justify-center gap-1"
            >
              <span
                v-for="badge in conditionBadges(mon)"
                :key="badge.label"
                class="rounded-md px-1.5 py-0.5 text-[9px] font-bold text-white"
                :style="{ backgroundColor: badge.color }"
              >
                {{ badge.label }}
              </span>
            </div>
          </div>
        </div>

        <!-- 对手信息条（HP） -->
        <div class="relative mt-2 space-y-2">
          <div
            v-for="mon in opponentActiveMons"
            :key="`opp-hp-${mon.index}`"
            class="showdown-hp-row"
          >
            <span class="showdown-hp-label">{{ mon.name }}</span>
            <div class="showdown-hp-bar">
              <div
                class="showdown-hp-fill"
                :class="hpTone(mon)"
                :style="{ width: hpWidth(mon) }"
              />
            </div>
            <span class="showdown-hp-num">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</span>
          </div>
        </div>

        <!-- 场地效果（中间） -->
        <div
          v-if="hasFieldEffects"
          class="relative mt-3 flex flex-wrap justify-center gap-1.5"
        >
          <span
            v-for="(effect, key) in fieldEffectChips"
            :key="key"
            class="field-chip"
            :class="effect.tone"
          >
            {{ effect.label }}
          </span>
        </div>

        <!-- 我方信息条（HP，左下） -->
        <div class="relative mt-4 space-y-2">
          <div
            v-for="mon in playerActiveMons"
            :key="`player-hp-${mon.index}`"
            class="showdown-hp-row justify-end"
          >
            <span class="showdown-hp-num">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</span>
            <div class="showdown-hp-bar">
              <div
                class="showdown-hp-fill"
                :class="hpTone(mon)"
                :style="{ width: hpWidth(mon) }"
              />
            </div>
            <span class="showdown-hp-label">{{ mon.name }}</span>
          </div>
        </div>

        <!-- 我方信息（左下） -->
        <div class="relative mt-2 flex items-end justify-start">
          <div
            v-for="(mon, idx) in playerActiveMons"
            :key="`player-sprite-${mon.index}-${idx}`"
            class="mr-4 flex flex-col items-center"
          >
            <div class="sprite-wrap relative h-24 w-24 sm:h-28 sm:w-28">
              <img
                :src="spriteUrl(mon)"
                :alt="mon.name"
                class="h-full w-full object-contain drop-shadow-[0_6px_8px_rgba(0,0,0,0.35)]"
                :class="mon.fainted ? 'grayscale opacity-40' : ''"
                @error="onSpriteError"
              >
              <div
                v-if="mon.fainted"
                class="absolute inset-0 flex items-center justify-center rounded-full"
              >
                <svg
                  viewBox="0 0 24 24"
                  class="h-8 w-8 text-rose-600 drop-shadow"
                ><path
                  fill="currentColor"
                  d="M7 7h10v10H7z"
                  transform="rotate(45 12 12)"
                /></svg>
              </div>
            </div>
            <div class="mt-1 flex items-center gap-1 rounded-full bg-black/25 px-2 py-0.5 text-[10px] font-bold text-white">
              <span>{{ mon.name }}</span>
              <span
                v-if="mon.level"
                class="text-emerald-200"
              >Lv.{{ mon.level }}</span>
            </div>
            <!-- 状态徽章 -->
            <div
              v-if="conditionBadges(mon).length"
              class="mt-1 flex flex-wrap justify-center gap-1"
            >
              <span
                v-for="badge in conditionBadges(mon)"
                :key="badge.label"
                class="rounded-md px-1.5 py-0.5 text-[9px] font-bold text-white"
                :style="{ backgroundColor: badge.color }"
              >
                {{ badge.label }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 替补区：双方后备队伍横条 -->
      <div class="grid gap-3 lg:grid-cols-2">
        <!-- 对手后备 -->
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
                :src="spriteUrl(mon)"
                :alt="mon.name"
                class="h-7 w-7 object-contain"
                :class="mon.fainted ? 'grayscale opacity-50' : ''"
                @error="onSpriteError"
              >
              <span class="bench-pill-name">{{ mon.name }}</span>
              <span class="bench-pill-hp">{{ mon.currentHp }}/{{ mon.maxHp || '?' }}</span>
            </div>
          </div>
        </div>

        <!-- 我方后备 -->
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
                :src="spriteUrl(mon)"
                :alt="mon.name"
                class="h-7 w-7 object-contain"
                :class="mon.fainted ? 'grayscale opacity-50' : ''"
                @error="onSpriteError"
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

    <!-- ===== 回合消息流（Showdown 风格） ===== -->
    <section class="rounded-[24px] border border-slate-200/80 bg-slate-50/70 p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="font-semibold text-slate-900">
          {{ tr('回合日志', 'Round log') }}
        </h3>
        <span class="text-sm text-slate-500">
          {{ summary?.status === 'completed' ? tr(`胜者：${summary.winner}`, `Winner: ${summary.winner}`) : tr('战斗进行中', 'Battle in progress') }}
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
            <div class="font-semibold text-slate-900">
              {{ round.round === 0 ? tr('入场阶段', 'Entry phase') : tr(`第 ${round.round} 回合`, `Round ${round.round}`) }}
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
              {{ tr('{count} 条事件', '{count} events', { count: (round.events || []).length }) }}
            </span>
          </div>

          <!-- 行动摘要（Showdown 风格：出手顺序 + 伤害） -->
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

// ===== 数据加工 =====

// 精灵图 URL（本地优先，失败时 onSpriteError 回退）
function spriteUrl(mon) {
  const id = mon?.form_id || mon?.species_id || mon?.pokemon_id || mon?.id
  return id ? sprites.pokemon(id) : sprites.default
}

// 精灵图加载失败时的二级回退：本地 -> 远程
function onSpriteError(event) {
  const img = event.target
  const src = img?.getAttribute('src') || ''
  if (src.includes('/api/pokedex/images')) {
    const id = src.split('/').pop().replace('.png', '')
    img.src = sprites.fallbackPokemon(id)
  } else {
    img.src = sprites.default
  }
}

// 宝可梦对象加工：补全展示字段
function buildMons(team = [], activeSlots = []) {
  return (team || []).map((pokemon, index) => {
    const maxHp = pokemon?.stats?.hp || pokemon?.maxHp || null
    const currentHp = Math.max(0, Number(pokemon?.currentHp ?? 0))
    return {
      ...pokemon,
      index,
      active: (activeSlots || []).includes(index),
      name: pokemon.name || pokemon.name_en || tr(`宝可梦 ${index + 1}`, `Pokemon ${index + 1}`),
      level: pokemon.level || 50,
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

function conditionBadges(mon) {
  if (mon.fainted || !mon) return []
  const badges = []
  const cond = mon.condition || mon.status
  if (cond && CONDITION_COLORS[cond]) {
    badges.push({ label: tr(CONDITION_LABELS[cond] || cond, cond), color: CONDITION_COLORS[cond] })
  }
  if ((mon.tauntTurns || 0) > 0) {
    badges.push({ label: tr('挑衅', 'Taunt'), color: CONDITION_COLORS.taunt })
  }
  if (mon.confused) {
    badges.push({ label: tr('混乱', 'Confusion'), color: CONDITION_COLORS.confusion })
  }
  if (mon.terastallized) {
    badges.push({ label: tr('太晶', 'Tera'), color: '#6366f1' })
  }
  if (mon.dynamaxed) {
    badges.push({ label: tr('极巨', 'Max'), color: '#dc2626' })
  }
  return badges
}

const CONDITION_LABELS = {
  paralysis: '麻痹',
  burn: '灼伤',
  freeze: '冰冻',
  sleep: '睡眠',
  poison: '中毒',
  toxic: '剧毒',
  confusion: '混乱',
  taunt: '挑衅'
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
  push(tr('对手顺风', 'Opp. Tailwind'), 'opponentTailwindTurns', 'tone-rose')
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
  push(tr('对手反射壁', 'Opp. Reflect'), 'opponentReflectTurns', 'tone-rose')
  push(tr('我方光墙', 'L.Screen'), 'playerLightScreenTurns', 'tone-blue')
  push(tr('对手光墙', 'Opp. L.Screen'), 'opponentLightScreenTurns', 'tone-rose')
  push(tr('我方极光幕', 'Aurora Veil'), 'playerAuroraVeilTurns', 'tone-blue')
  push(tr('对手极光幕', 'Opp. Aurora'), 'opponentAuroraVeilTurns', 'tone-rose')
  push(tr('我方神秘守护', 'Safeguard'), 'playerSafeguardTurns', 'tone-emerald')
  push(tr('对手神秘守护', 'Opp. Safeguard'), 'opponentSafeguardTurns', 'tone-teal')
  push(tr('我方隐形岩', 'S.Rock'), 'playerStealthRock', 'tone-gray')
  push(tr('对手隐形岩', 'Opp. S.Rock'), 'opponentStealthRock', 'tone-gray')
  if (Number(fe.playerSpikesLayers || 0) > 0) chips.push({ label: `撒菱 ${fe.playerSpikesLayers}/3`, tone: 'tone-green' })
  if (Number(fe.opponentSpikesLayers || 0) > 0) chips.push({ label: `对手撒菱 ${fe.opponentSpikesLayers}/3`, tone: 'tone-red' })
  if (Number(fe.playerToxicSpikesLayers || 0) > 0) chips.push({ label: `毒菱 ${fe.playerToxicSpikesLayers}/2`, tone: 'tone-purple' })
  if (Number(fe.opponentToxicSpikesLayers || 0) > 0) chips.push({ label: `对手毒菱 ${fe.opponentToxicSpikesLayers}/2`, tone: 'tone-pink' })
  if (fe.playerStickyWeb) chips.push({ label: '我方黏黏网', tone: 'tone-yellow' })
  if (fe.opponentStickyWeb) chips.push({ label: '对手黏黏网', tone: 'tone-orange' })
  return chips
})
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

/* ===== Showdown 式战场 ===== */
.showdown-field {
  box-shadow: inset 0 2px 8px rgba(0,0,0,0.08);
}

.sprite-wrap img {
  image-rendering: pixelated;
}

/* 精灵图入场动画 */
.sprite-wrap img {
  animation: sprite-enter 0.4s ease-out;
}

@keyframes sprite-enter {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.92);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* HP 数字变化闪烁 */
.showdown-hp-num {
  transition: color 0.3s;
}

/* 当前回合高亮 */
.round-current {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.2), 0 8px 24px -8px rgba(99, 102, 241, 0.35);
}

/* HP 条（Showdown 风格：白底、色块、分段） */
.showdown-hp-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.showdown-hp-label {
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 700;
  color: #1e293b;
  text-shadow: 0 1px 0 rgba(255,255,255,0.6);
}

.showdown-hp-bar {
  position: relative;
  height: 14px;
  flex: 1;
  max-width: 260px;
  border-radius: 4px;
  background: #1e293b;
  border: 2px solid #334155;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.25);
}

.showdown-hp-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease, background 0.5s ease;
  background: linear-gradient(180deg, #4ade80, #16a34a);
  background-image:
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 7px,
      rgba(0,0,0,0.18) 7px,
      rgba(0,0,0,0.18) 8px
    ),
    linear-gradient(180deg, #4ade80, #16a34a);
}

.showdown-hp-fill.high {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #4ade80, #16a34a);
}

.showdown-hp-fill.mid {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #fbbf24, #d97706);
}

.showdown-hp-fill.low {
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 7px, rgba(0,0,0,0.18) 7px, rgba(0,0,0,0.18) 8px),
    linear-gradient(180deg, #f87171, #dc2626);
}

.showdown-hp-fill.empty {
  background: #64748b;
}

.showdown-hp-num {
  flex-shrink: 0;
  font-size: 0.7rem;
  font-weight: 700;
  color: #334155;
  font-variant-numeric: tabular-nums;
}

/* 场地效果徽章 */
.field-chip {
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 700;
  border: 1px solid rgba(255,255,255,0.7);
  box-shadow: 0 1px 2px rgba(0,0,0,0.12);
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
</style>
