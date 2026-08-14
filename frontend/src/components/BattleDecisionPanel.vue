
<template>
  <!-- 队伍预览阶段（Showdown 风格） -->
  <section
    v-if="isPreviewPhase"
    class="overflow-hidden rounded-2xl border-2 border-slate-800 bg-[linear-gradient(180deg,#1e293b_0%,#0f172a_100%)] shadow-xl"
  >
    <!-- 顶部横幅 -->
    <div class="flex items-center justify-between gap-3 border-b border-slate-700/60 bg-slate-900/60 px-4 py-3">
      <div class="flex items-center gap-2.5">
        <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/20 text-lg">⚔️</span>
        <div>
          <div class="text-sm font-extrabold tracking-wide text-white">
            {{ tr('队伍预览', 'Team Preview') }}
          </div>
          <div class="text-[11px] text-slate-400">
            {{ tr(`选择 ${rosterLimit} 只出战，并指定 ${leadLimit} 只首发`, `Pick ${rosterLimit} Pokemon and choose ${leadLimit} lead(s)`) }}
          </div>
        </div>
      </div>
      <!-- 选择进度 -->
      <div class="flex items-center gap-1.5 rounded-full bg-slate-800/80 px-3 py-1.5">
        <span class="text-[11px] font-bold text-slate-400">{{ tr('已选', 'Picked') }}</span>
        <span class="text-sm font-black text-white">{{ selectedRosterIndexes.length }}</span>
        <span class="text-[11px] text-slate-500">/</span>
        <span class="text-sm font-black text-slate-300">{{ rosterLimit }}</span>
        <span class="mx-1 text-slate-700">|</span>
        <span class="text-[11px] font-bold text-slate-400">{{ tr('首发', 'Leads') }}</span>
        <span class="text-sm font-black text-amber-400">{{ leadRosterIndexes.length }}</span>
        <span class="text-[11px] text-slate-500">/</span>
        <span class="text-sm font-black text-slate-300">{{ leadLimit }}</span>
      </div>
    </div>

    <div class="space-y-4 p-4">
      <!-- 对手队伍（不可选，hover 查看） -->
      <div v-if="opponentRoster.length">
        <div class="mb-1.5 flex items-center gap-2">
          <span class="h-1.5 w-1.5 rounded-full bg-rose-400" />
          <span class="text-[11px] font-bold uppercase tracking-widest text-rose-300/90">
            {{ tr('对手队伍', 'Opponent team') }}
          </span>
        </div>
        <div class="flex flex-wrap gap-2">
          <PokeHoverCard
            v-for="(pokemon, index) in opponentRoster"
            :key="`opponent-roster-${index}`"
            :pokemon="pokemon"
            wrap-class="cursor-help"
          >
            <div class="group relative w-[76px] rounded-xl border border-slate-700/70 bg-slate-800/50 p-2 text-center transition-colors hover:border-slate-500 hover:bg-slate-800">
              <img
                :src="previewSprite(pokemon)"
                :alt="pokemon.name"
                class="mx-auto h-12 w-12 object-contain transition-transform group-hover:scale-110"
                @error="onPreviewSpriteError($event, pokemon)"
              >
              <div class="mt-1 truncate text-[10px] font-semibold text-slate-300">
                {{ pokemon.name || pokemon.name_en }}
              </div>
            </div>
          </PokeHoverCard>
        </div>
      </div>

      <!-- 我的队伍（点击选择） -->
      <div>
        <div class="mb-1.5 flex items-center gap-2">
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
          <span class="text-[11px] font-bold uppercase tracking-widest text-emerald-300/90">
            {{ tr('我的队伍 · 点击选择', 'Your team · click to pick') }}
          </span>
        </div>
        <div class="flex flex-wrap gap-2">
          <PokeHoverCard
            v-for="(pokemon, index) in playerRoster"
            :key="`player-roster-${index}`"
            :pokemon="pokemon"
            wrap-class="group relative"
          >
            <button
              type="button"
              :class="previewCardClass(index)"
              class="flex w-[76px] flex-col items-center rounded-xl border-2 p-2 text-center transition-all duration-150"
              @click="toggleRoster(index)"
            >
              <!-- 精灵图：未选中变暗 -->
              <div class="relative">
                <img
                  :src="previewSprite(pokemon)"
                  :alt="pokemon.name"
                  class="h-12 w-12 object-contain transition-all duration-150 group-hover:scale-110"
                  :class="isPicked(index) ? '' : 'opacity-35 grayscale'"
                  @error="onPreviewSpriteError($event, pokemon)"
                >
                <!-- 首发角标 -->
                <span
                  v-if="isLead(index)"
                  class="absolute -top-1.5 -right-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-amber-400 text-[10px] font-black text-amber-950 shadow ring-2 ring-slate-900"
                >首</span>
              </div>
              <!-- 名字 + 等级 -->
              <div
                class="mt-1 w-full truncate text-[10px] font-bold"
                :class="isPicked(index) ? 'text-white' : 'text-slate-400'"
              >{{ pokemon.name || pokemon.name_en }}</div>
              <div
                class="mt-0.5 rounded-full px-1.5 py-px text-[9px] font-bold"
                :class="isPicked(index) ? 'bg-slate-700/80 text-slate-300' : 'bg-slate-800 text-slate-500'"
              >Lv.{{ pokemon.level || 50 }}</div>
            </button>
          </PokeHoverCard>
        </div>
      </div>

      <!-- 首发选择条（Showdown 风格） -->
      <div
        v-if="selectedRosterIndexes.length"
        class="rounded-xl border border-slate-700/60 bg-slate-800/40 p-2.5"
      >
        <div class="mb-1.5 flex items-center gap-2">
          <span class="text-[10px] font-bold uppercase tracking-widest text-slate-400">
            {{ tr('首发阵容', 'Lead picks') }}
          </span>
          <span class="text-[10px] text-slate-500">
            {{ tr('点击已选精灵设为首发', 'Click a picked Pokemon to make it lead') }}
          </span>
        </div>
        <div class="flex flex-wrap items-center gap-1.5">
          <button
            v-for="index in selectedRosterIndexes"
            :key="`lead-${index}`"
            type="button"
            class="flex items-center gap-1.5 rounded-lg border px-2 py-1 text-[11px] font-semibold transition-all"
            :class="isLead(index)
              ? 'border-amber-400/70 bg-amber-400/15 text-amber-300 shadow-[0_0_12px_rgba(251,191,36,0.15)]'
              : 'border-slate-600/70 bg-slate-800/60 text-slate-300 hover:border-slate-400 hover:text-white'"
            @click="toggleLead(index)"
          >
            <img
              :src="previewSprite(playerRoster[index])"
              :alt="playerRoster[index]?.name"
              class="h-5 w-5 object-contain"
              @error="onPreviewSpriteError($event, playerRoster[index])"
            >
            <span class="max-w-[80px] truncate">{{ playerRoster[index]?.name || playerRoster[index]?.name_en }}</span>
            <span v-if="isLead(index)" class="text-amber-400">★</span>
          </button>
        </div>
      </div>

      <!-- 确认按钮 -->
      <button
        class="w-full rounded-xl bg-indigo-500 px-4 py-2.5 text-sm font-extrabold text-white shadow-lg shadow-indigo-500/25 transition-all hover:bg-indigo-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-500 disabled:shadow-none"
        :disabled="!canConfirmPreview || isBusy"
        @click="confirmPreview"
      >
        {{ busyAction === 'confirm-preview' ? tr('正在确认阵容...', 'Confirming team...') : tr(`确认出战 · 6 选 ${rosterLimit}`, `Confirm · pick ${rosterLimit}`) }}
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
      <PokeHoverCard
        v-for="option in replacementBenchOptions"
        :key="`replacement-${option.value}`"
        :pokemon="option.pokemon || option"
        wrap-class="w-full"
      >
        <button
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
      </PokeHoverCard>
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
          <PokeHoverCard
            :pokemon="mon"
            wrap-class="flex items-center gap-2.5 cursor-help"
          >
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
          </PokeHoverCard>
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
            <PokeHoverCard
              v-for="target in playerBenchOptions"
              :key="`switch-${mon.fieldSlot}-${target.value}`"
              :pokemon="target.pokemon || target"
              wrap-class="w-full"
            >
              <button
                type="button"
                class="w-full rounded-xl border-2 px-3 py-2.5 text-left transition-all"
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
            </PokeHoverCard>
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
              <PokeHoverCard
                v-for="target in opponentActiveMons"
                :key="`target-${mon.fieldSlot}-${target.fieldSlot}`"
                :pokemon="target"
                wrap-class="flex-1"
              >
                <button
                  type="button"
                  class="w-full rounded-xl border-2 px-3 py-2 text-left transition-all"
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
              </PokeHoverCard>
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
import PokeHoverCard from './PokeHoverCard.vue'
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
