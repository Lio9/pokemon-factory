
<template>
  <!-- 队伍预览阶段 -->
  <section
    v-if="isPreviewPhase"
    class="glass-card border border-amber-200/60 p-4"
  >
    <div class="mb-3 text-sm font-semibold text-slate-800">
      {{ tr(`队伍预览：从 6 只里选择 ${rosterLimit} 只，并指定 ${leadLimit} 只首发`, `Team preview: pick ${rosterLimit} of 6 Pokemon and choose ${leadLimit} lead(s)`) }}
    </div>
    <div class="grid gap-4 lg:grid-cols-2">
      <div>
        <div class="mb-2 text-xs font-semibold text-slate-500">
          {{ tr('你的队伍（点击选择/取消）', 'Your roster (click to toggle)') }}
        </div>
        <div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
          <div
            v-for="(pokemon, index) in playerRoster"
            :key="`player-roster-${index}`"
            class="group relative"
          >
            <button
              type="button"
              :class="previewCardClass(index)"
              class="!flex !flex-col !items-center !p-3 !text-center"
              @click="toggleRoster(index)"
            >
              <div class="relative">
                <img
                  :src="previewSprite(pokemon)"
                  :alt="pokemon.name"
                  class="h-16 w-16 object-contain transition-transform group-hover:scale-110"
                  :class="isPicked(index) ? '' : 'opacity-40 grayscale'"
                  @error="onPreviewSpriteError($event, pokemon)"
                >
                <!-- 首发标记 -->
                <span
                  v-if="isLead(index)"
                  class="absolute -top-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-amber-500 text-[10px] font-black text-white shadow-lg ring-2 ring-white"
                >首</span>
              </div>
              <div class="mt-1.5 w-full">
                <div
                  class="truncate text-xs font-bold"
                  :class="isPicked(index) ? 'text-slate-900' : 'text-slate-400'"
                >{{ pokemon.name || pokemon.name_en }}</div>
                <div class="mt-1 flex flex-wrap justify-center gap-1">
                  <span
                    v-for="type in pokemon.types || []"
                    :key="type.id || type.type_id"
                    class="type-badge type-badge-sm"
                    :style="{ backgroundColor: type.color || typeColor(type.type_id) }"
                  >{{ type.name }}</span>
                </div>
              </div>
            </button>
            <!-- 查看详情按钮 -->
            <button
              type="button"
              class="absolute top-1 right-1 z-10 h-6 w-6 rounded-full bg-white/90 border border-slate-200 text-slate-400 shadow-sm hover:bg-indigo-50 hover:text-indigo-600 transition-all opacity-70 group-hover:opacity-100 flex items-center justify-center"
              :title="tr('查看配置', 'View details')"
              @click.stop="openDetail(pokemon)"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-3 w-3"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              ><circle
                cx="11"
                cy="11"
                r="8"
              /><line
                x1="21"
                y1="21"
                x2="16.65"
                y2="16.65"
              /></svg>
            </button>
          </div>
        </div>
      </div>

      <div>
        <div class="mb-2 text-xs font-semibold text-slate-500">
          {{ tr('对手公开队伍', 'Opponent preview') }}
        </div>
        <div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
          <div
            v-for="(pokemon, index) in opponentRoster"
            :key="`opponent-roster-${index}`"
            class="group relative rounded-xl border border-slate-200 bg-white p-2.5 text-center"
          >
            <img
              :src="previewSprite(pokemon)"
              :alt="pokemon.name"
              class="mx-auto h-14 w-14 object-contain transition-transform group-hover:scale-110"
              @error="onPreviewSpriteError($event, pokemon)"
            >
            <div class="mt-1 truncate text-xs font-bold text-slate-800">
              {{ pokemon.name || pokemon.name_en }}
            </div>
            <div class="mt-1 flex flex-wrap justify-center gap-1">
              <span
                v-for="type in pokemon.types || []"
                :key="type.id || type.type_id"
                class="type-badge type-badge-sm"
                :style="{ backgroundColor: type.color || typeColor(type.type_id) }"
              >{{ type.name }}</span>
            </div>
            <button
              type="button"
              class="absolute top-1 right-1 z-10 h-6 w-6 rounded-full bg-white/90 border border-slate-200 text-slate-400 shadow-sm hover:bg-indigo-50 hover:text-indigo-600 transition-all opacity-70 group-hover:opacity-100 flex items-center justify-center"
              :title="tr('查看配置', 'View details')"
              @click.stop="openDetail(pokemon)"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-3 w-3"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              ><circle
                cx="11"
                cy="11"
                r="8"
              /><line
                x1="21"
                y1="21"
                x2="16.65"
                y2="16.65"
              /></svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-4 rounded-xl bg-white p-4">
      <div class="text-sm text-slate-700">
        {{ tr(`已选择 {selected}/${rosterLimit} 只；首发 {lead}/${leadLimit} 只`, `{selected}/${rosterLimit} selected; {lead}/${leadLimit} leads`, { selected: selectedRosterIndexes.length, lead: leadRosterIndexes.length }) }}
      </div>
      <div class="mt-2 flex flex-wrap gap-2">
        <button
          v-for="index in selectedRosterIndexes"
          :key="`lead-${index}`"
          type="button"
          class="rounded-full px-3 py-1 text-xs font-semibold"
          :class="isLead(index) ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-700'"
          @click="toggleLead(index)"
        >
          {{ playerRoster[index]?.name || playerRoster[index]?.name_en || tr(`宝可梦 ${index + 1}`, `Pokemon ${index + 1}`) }}{{ isLead(index) ? tr(' · 首发', ' · Lead') : '' }}
        </button>
      </div>
      <button
        class="mt-4 w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="!canConfirmPreview || isBusy"
        @click="confirmPreview"
      >
        {{ busyAction === 'confirm-preview' ? tr('正在确认阵容...', 'Confirming team...') : tr(`确认 6 选 ${rosterLimit} 与首发`, `Confirm 6v${rosterLimit} and leads`) }}
      </button>
    </div>
  </section>

  <!-- 补位阶段 -->
  <section
    v-if="isReplacementPhase"
    class="rounded-2xl border border-rose-200 bg-[linear-gradient(180deg,rgba(255,228,230,0.72),rgba(255,255,255,0.96))] p-4"
  >
    <div class="mb-3 text-sm font-semibold text-slate-800">
      {{ tr('倒下补位：请选择 {count} 只后备宝可梦上场', 'Replacement: choose {count} bench Pokemon to send in', { count: pendingReplacementCount }) }}
    </div>
    <div class="space-y-2">
      <button
        v-for="option in replacementBenchOptions"
        :key="`replacement-${option.value}`"
        type="button"
        class="w-full rounded-xl border-2 p-3 text-left transition-all"
        :class="selectedReplacementIndexes.includes(option.value) ? 'border-rose-500 bg-white shadow-md ring-2 ring-rose-200' : 'border-slate-200 bg-white hover:border-slate-400 hover:shadow-sm'"
        @click="toggleReplacement(option.value)"
      >
        <div class="flex items-center gap-3">
          <img
            :src="previewSprite(option.pokemon || option)"
            :alt="option.label"
            class="h-11 w-11 object-contain"
            @error="onPreviewSpriteError($event, option.pokemon || option)"
          >
          <div class="min-w-0 flex-1">
            <div class="truncate font-semibold text-slate-900">
              {{ option.label }}
            </div>
            <div class="text-xs text-slate-500">
              {{ option.types }}
            </div>
            <div class="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                class="h-full rounded-full transition-all"
                :style="{ width: option.maxHp > 0 ? Math.max(2, (option.hp / option.maxHp) * 100) + '%' : '100%', backgroundColor: option.maxHp > 0 && option.hp / option.maxHp <= 0.25 ? '#ef4444' : option.maxHp > 0 && option.hp / option.maxHp <= 0.5 ? '#f59e0b' : '#22c55e' }"
              />
            </div>
          </div>
          <div class="shrink-0 text-xs text-slate-500">
            HP {{ option.hp }}/{{ option.maxHp || '?' }}
          </div>
        </div>
      </button>
    </div>
    <button
      class="mt-4 w-full rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-slate-300"
      :disabled="!canConfirmReplacement || isBusy"
      @click="confirmReplacement"
    >
      {{ busyAction === 'confirm-replacement' ? tr('正在确认替补...', 'Confirming replacements...') : tr('确认替补上场', 'Confirm replacement') }}
    </button>
  </section>

  <!-- 对战操作阶段 -->
  <section
    v-if="!isPreviewPhase"
    class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
  >
    <div class="mb-3 text-sm font-semibold text-slate-800">
      {{ isReplacementPhase ? tr('当前回合已暂停，等待补位', 'The turn is paused until replacements are chosen') : tr('当前可选招式', 'Available actions this turn') }}
    </div>
    <div
      v-if="playerActiveMons.length && !isPreviewPhase && !isReplacementPhase"
      class="space-y-4"
    >
      <div
        v-for="mon in playerActiveMons"
        :key="mon.fieldSlot"
        class="rounded-xl border border-slate-200 bg-white p-4"
      >
        <!-- 宝可梦信息头 -->
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
          <div class="flex items-center gap-2.5">
            <img
              :src="previewSprite(mon)"
              :alt="mon.name"
              class="h-11 w-11 object-contain"
              @error="onPreviewSpriteError($event, mon)"
            >
            <div>
              <div class="font-semibold text-slate-900">
                {{ mon.name }}
              </div>
              <div class="text-xs text-slate-500">
                {{ tr('槽位 {slot} · HP {current}/{max}', 'Slot {slot} · HP {current}/{max}', { slot: mon.fieldSlot + 1, current: mon.currentHp, max: mon.maxHp }) }}
              </div>
            </div>
          </div>
          <div class="text-xs text-slate-500">
            {{ formatTypes(mon.types) }}
          </div>
        </div>

        <!-- 操作类型切换：招式 / 换人 -->
        <div class="mt-3 flex gap-2">
          <button
            type="button"
            class="flex-1 rounded-lg px-3 py-2 text-sm font-semibold transition-all"
            :class="(selectedActions[`action-slot-${mon.fieldSlot}`] || 'move') === 'move' ? 'bg-indigo-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            @click="setSelectedAction(mon.fieldSlot, 'move')"
          >
            {{ tr('使用招式', 'Move') }}
          </button>
          <button
            type="button"
            class="flex-1 rounded-lg px-3 py-2 text-sm font-semibold transition-all"
            :class="selectedActions[`action-slot-${mon.fieldSlot}`] === 'switch' ? 'bg-indigo-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            :disabled="!playerBenchOptions.length"
            @click="setSelectedAction(mon.fieldSlot, 'switch')"
          >
            {{ tr('换人', 'Switch') }}
          </button>
        </div>

        <!-- 换人面板（Showdown 风格：精灵 + HP 条） -->
        <template v-if="selectedActions[`action-slot-${mon.fieldSlot}`] === 'switch'">
          <div class="mt-3 grid grid-cols-2 gap-2">
            <button
              v-for="target in playerBenchOptions"
              :key="`switch-${mon.fieldSlot}-${target.value}`"
              type="button"
              class="rounded-xl border-2 px-3 py-2.5 text-left transition-all"
              :class="selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`] === target.value ? 'border-indigo-500 bg-indigo-50 shadow-sm ring-2 ring-indigo-200' : 'border-slate-200 bg-white hover:border-slate-400 hover:shadow-sm'"
              @click="setSelectedSwitchTarget(mon.fieldSlot, target.value)"
            >
              <div class="flex items-center gap-2">
                <img
                  :src="previewSprite(target.pokemon || target)"
                  :alt="target.label"
                  class="h-10 w-10 object-contain"
                  @error="onPreviewSpriteError($event, target.pokemon || target)"
                >
                <div class="min-w-0 flex-1">
                  <div class="truncate text-sm font-semibold text-slate-900">
                    {{ target.label }}
                  </div>
                  <!-- HP 条 -->
                  <div class="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                    <div
                      class="h-full rounded-full transition-all"
                      :style="{ width: target.maxHp > 0 ? Math.max(2, (target.hp / target.maxHp) * 100) + '%' : '100%', backgroundColor: target.maxHp > 0 && target.hp / target.maxHp <= 0.25 ? '#ef4444' : target.maxHp > 0 && target.hp / target.maxHp <= 0.5 ? '#f59e0b' : '#22c55e' }"
                    />
                  </div>
                  <div class="mt-0.5 text-[11px] text-slate-500">
                    {{ tr('HP', 'HP') }} {{ target.hp }}/{{ target.maxHp || '?' }}
                  </div>
                </div>
              </div>
            </button>
          </div>
          <div class="mt-1.5 text-[10px] text-slate-400">
            {{ tr('提示：S 键快速切换 招式/换人', 'Tip: press S to toggle move/switch') }}
          </div>
        </template>

        <!-- 招式面板 -->
        <template v-else>
          <!-- 特殊系统 -->
          <div
            v-if="activeSpecialSystemLabel(mon) || availableSpecialSystems(mon).length"
            class="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800"
          >
            <div
              v-if="activeSpecialSystemLabel(mon)"
              class="font-semibold"
            >
              {{ tr('已发动', 'Activated') }}：{{ activeSpecialSystemLabel(mon) }}<span v-if="mon.terastallized"> · {{ teraTypeLabel(mon) }}</span>
            </div>
            <template v-if="availableSpecialSystems(mon).length">
              <div class="font-semibold">
                {{ tr('本回合可发动特殊系统', 'Special systems available this turn') }}
              </div>
              <div class="mt-2 flex flex-wrap gap-1.5">
                <button
                  type="button"
                  class="rounded-lg px-2.5 py-1 text-xs font-semibold transition-all"
                  :class="!selectedSpecialSystems[`special-slot-${mon.fieldSlot}`] ? 'bg-amber-500 text-white' : 'bg-white border border-amber-300 text-amber-700 hover:bg-amber-50'"
                  @click="setSelectedSpecialSystem(mon.fieldSlot, undefined)"
                >
                  {{ tr('不发动', 'None') }}
                </button>
                <button
                  v-for="system in availableSpecialSystems(mon)"
                  :key="`special-${mon.fieldSlot}-${system}`"
                  type="button"
                  class="rounded-lg px-2.5 py-1 text-xs font-semibold transition-all"
                  :class="selectedSpecialSystems[`special-slot-${mon.fieldSlot}`] === system ? 'bg-amber-500 text-white' : 'bg-white border border-amber-300 text-amber-700 hover:bg-amber-50'"
                  @click="setSelectedSpecialSystem(mon.fieldSlot, system)"
                >
                  {{ specialSystemLabel(system) }}<template v-if="system === 'tera'">
                    · {{ teraTypeLabel(mon) }}
                  </template>
                </button>
              </div>
            </template>
          </div>

          <!-- 招式按钮 -->
          <div class="mt-3 grid grid-cols-2 gap-2">
            <MoveButton
              v-for="(move, moveIdx) in mon.moves"
              :key="move.name_en || move.name"
              :move="move"
              :move-index="moveIdx"
              :selected="selectedMoves[`slot-${mon.fieldSlot}`] === (move.name_en || move.name)"
              @select="setSelectedMove(mon.fieldSlot, move.name_en || move.name)"
            />
          </div>
          <div
            v-if="mon.moves.length"
            class="mt-1.5 text-[11px] text-slate-400"
          >
            {{ tr('提示：按数字键 1-4 快速选择招式', 'Tip: press keys 1-4 to pick a move') }}
          </div>

          <!-- 目标选择（Showdown 风格：点击场上精灵） -->
          <div
            v-if="moveNeedsOpponentTarget(selectedMoveObject(mon)) && opponentActiveMons.length"
            class="mt-3"
          >
            <div class="text-xs font-semibold text-slate-500 mb-1.5">
              {{ tr('选择目标', 'Select target') }} · {{ tr('点击场上宝可梦', 'Click an active Pokemon') }}
            </div>
            <div class="flex gap-2">
              <button
                v-for="target in opponentActiveMons"
                :key="`target-${mon.fieldSlot}-${target.fieldSlot}`"
                type="button"
                class="flex-1 rounded-xl border-2 px-3 py-2 text-left transition-all"
                :class="selectedTargets[`target-slot-${mon.fieldSlot}`] === target.fieldSlot ? 'border-rose-500 bg-rose-50 shadow-sm ring-2 ring-rose-200' : 'border-slate-200 bg-white hover:border-slate-400 hover:shadow-sm'"
                @click="setSelectedTarget(mon.fieldSlot, target.fieldSlot)"
              >
                <div class="flex items-center gap-2">
                  <img
                    :src="targetSprite(target)"
                    :alt="target.name"
                    class="h-9 w-9 object-contain"
                    @error="onTargetSpriteError($event, target)"
                  >
                  <div class="min-w-0">
                    <div class="truncate text-sm font-semibold text-slate-900">
                      {{ target.name || target.name_en }}
                    </div>
                    <div class="text-[11px] text-slate-500">
                      {{ tr('槽位 {slot}', 'Slot {slot}', { slot: target.fieldSlot + 1 }) }} · HP {{ target.currentHp }}/{{ target.maxHp || target.stats?.hp || '?' }}
                    </div>
                  </div>
                </div>
              </button>
            </div>
          </div>

          <!-- 克制关系提示 -->
          <div
            v-if="moveEffectivenessHints(mon).length"
            class="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2"
          >
            <div class="text-xs font-semibold text-slate-600">
              {{ tr('技能克制关系', 'Type matchup hints') }}
            </div>
            <div class="mt-2 flex flex-wrap gap-2">
              <span
                v-for="hint in moveEffectivenessHints(mon)"
                :key="hint.key"
                class="rounded-full border px-2.5 py-1 text-xs font-semibold"
                :class="hint.className"
              >
                {{ hint.targetLabel }} · {{ hint.label }}
              </span>
            </div>
          </div>
        </template>
      </div>

      <!-- 行动总览（Showdown 风格：提交前一眼确认） -->
      <div
        v-if="playerActiveMons.length > 0 && !isPreviewPhase && !isReplacementPhase"
        class="mt-1 rounded-xl border border-indigo-200 bg-indigo-50/70 px-3 py-2"
      >
        <div class="mb-1 text-[10px] font-bold uppercase tracking-widest text-indigo-500">
          {{ tr('本回合行动', 'This turn') }}
        </div>
        <div class="space-y-1">
          <div
            v-for="mon in playerActiveMons"
            :key="`overview-${mon.fieldSlot}`"
            class="flex items-center gap-2 text-xs"
          >
            <span class="font-bold text-slate-800">{{ mon.name || mon.name_en }}</span>
            <span class="text-slate-400">→</span>
            <template v-if="(selectedActions[`action-slot-${mon.fieldSlot}`] || 'move') === 'switch'">
              <span class="font-semibold text-indigo-600">{{ tr('换人', 'Switch') }}</span>
              <span
                v-if="selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`] !== undefined"
                class="font-semibold text-slate-600"
              >→ {{ benchName(selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`]) }}</span>
              <span
                v-else
                class="text-amber-600 font-semibold"
              >{{ tr('请选目标', 'Pick target') }}</span>
            </template>
            <template v-else>
              <span class="font-semibold text-slate-600">{{ moveName(selectedMoves[`slot-${mon.fieldSlot}`], mon) }}</span>
              <template v-if="moveNeedsOpponentTarget(selectedMoveObject(mon))">
                <span class="text-slate-400">→</span>
                <span
                  v-if="selectedTargets[`target-slot-${mon.fieldSlot}`] !== undefined"
                  class="font-semibold text-rose-600"
                >{{ targetName(selectedTargets[`target-slot-${mon.fieldSlot}`]) }}</span>
                <span
                  v-else
                  class="text-amber-600 font-semibold"
                >{{ tr('请选目标', 'Pick target') }}</span>
              </template>
              <span
                v-if="selectedSpecialSystems[`special-slot-${mon.fieldSlot}`]"
                class="ml-auto rounded-full bg-amber-200/80 px-1.5 py-px text-[10px] font-bold text-amber-800"
              >{{ specialSystemLabel(selectedSpecialSystems[`special-slot-${mon.fieldSlot}`]) }}</span>
            </template>
          </div>
        </div>
      </div>

      <button
        class="w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="!canSubmitMove || isBusy"
        @click="submitMove"
      >
        {{ busyAction === 'submit-move' ? tr('正在提交回合...', 'Submitting turn...') : tr('提交当前回合', 'Submit turn') }}
      </button>
    </div>
    <div
      v-else
      class="text-sm text-slate-500"
    >
      {{ isPreviewPhase ? tr('先完成队伍预览后，才能提交回合操作。', 'Complete team preview before submitting turn actions.') : isReplacementPhase ? tr('有宝可梦倒下时，必须先完成替补上场。', 'When a Pokemon faints, you must finish replacements first.') : tr('先开始一场手动对战后，这里会显示你当前两只在场宝可梦的出招选择。', 'Start a manual battle first, then this panel will show the move choices for your two active Pokemon.') }}
    </div>
  </section>

  <!-- 调试面板 -->
  <details
    class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
    :open="showDebugPanel"
    @toggle="emit('toggle-debug-panel', $event.target.open)"
  >
    <summary class="cursor-pointer list-none text-sm font-semibold text-slate-800">
      {{ tr('调试响应', 'Debug response') }}
    </summary>
    <div class="mt-2 text-xs leading-5 text-slate-500">
      {{ tr('日常使用时可以收起；排查接口返回时再展开。', 'Keep this collapsed for normal play and expand it when you need to inspect API responses.') }}
    </div>
    <pre class="mt-3 max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-slate-950 p-4 text-xs text-slate-100">{{ resultText }}</pre>
  </details>

  <!-- 宝可梦详情弹窗 -->
  <PokemonDetailPopover
    v-model:visible="showDetailDialog"
    :pokemon="detailPokemon"
  />
</template>

<script setup>
import { ref } from 'vue'
import { useLocale } from '../composables/useLocale'
import MoveButton from './MoveButton.vue'
import PokemonDetailPopover from './PokemonDetailPopover.vue'
import { sprites } from '../services/sprites'
import { typeColor } from '../services/typeChart'

const { translate: tr } = useLocale()

const emit = defineEmits(['toggle-debug-panel'])

// 详情弹窗状态
const showDetailDialog = ref(false)
const detailPokemon = ref(null)

function openDetail(pokemon) {
  detailPokemon.value = pokemon
  showDetailDialog.value = true
}

// 目标精灵图
function targetSprite(target) {
  const id = target?.form_id || target?.species_id || target?.pokemon_id || target?.id
  return id ? sprites.pokemon(id) : sprites.default
}

function onTargetSpriteError(event, target) {
  const img = event.target
  const src = img?.getAttribute('src') || ''
  if (src.includes('/api/pokedex/images')) {
    const id = src.split('/').pop().replace('.png', '')
    img.src = sprites.fallbackPokemon(id)
  } else {
    img.src = sprites.default
  }
}

// 预览精灵图
function previewSprite(pokemon) {
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  return id ? sprites.pokemon(id) : sprites.default
}

function onPreviewSpriteError(event, pokemon) {
  const img = event.target
  const src = img?.getAttribute('src') || ''
  const id = pokemon?.form_id || pokemon?.species_id || pokemon?.pokemon_id || pokemon?.id
  if (src.includes('/api/pokedex/images')) {
    img.src = sprites.fallbackPokemon(id)
  } else {
    img.src = sprites.default
  }
}

// 行动总览辅助函数
function benchName(teamIndex) {
  const mon = playerBenchOptions.value?.find((o) => o.value === Number(teamIndex))
  return mon?.label || tr('宝可梦 {n}', 'Pokemon {n}', { n: Number(teamIndex) + 1 })
}

function moveName(moveKey, mon) {
  const mv = (mon?.moves || []).find((m) => (m.name_en || m.name) === moveKey)
  return mv?.name || mv?.name_en || moveKey || tr('未选', 'Pick a move')
}

function targetName(fieldSlot) {
  const t = opponentActiveMons.value?.find((o) => Number(o.fieldSlot) === Number(fieldSlot))
  return t?.name || t?.name_en || tr('目标 {n}', 'Target {n}', { n: Number(fieldSlot) + 1 })
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
