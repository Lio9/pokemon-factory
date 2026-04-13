<template>
  <section
    v-if="isPreviewPhase"
    class="rounded-2xl border border-amber-200 bg-[linear-gradient(180deg,rgba(254,243,199,0.7),rgba(255,255,255,0.95))] p-4"
  >
    <div class="mb-3 text-sm font-semibold text-slate-800">
      队伍预览：从 6 只里选择 4 只，并指定 2 只首发
    </div>
    <div class="grid gap-4 lg:grid-cols-2">
      <div>
        <div class="mb-2 text-xs font-semibold text-slate-500">
          你的队伍
        </div>
        <div class="space-y-2">
          <button
            v-for="(pokemon, index) in playerRoster"
            :key="`player-roster-${index}`"
            type="button"
            :class="previewCardClass(index)"
            @click="toggleRoster(index)"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="text-left">
                <div class="font-semibold text-slate-900">
                  {{ pokemon.name || pokemon.name_en || `宝可梦 ${index + 1}` }}
                </div>
                <div class="text-xs text-slate-500">
                  {{ formatTypes(pokemon.types) }}
                </div>
              </div>
              <div class="text-right text-xs text-slate-500">
                <div>{{ isPicked(index) ? '已选入' : '未选入' }}</div>
                <div>{{ isLead(index) ? '首发' : '后备' }}</div>
              </div>
            </div>
          </button>
        </div>
      </div>

      <div>
        <div class="mb-2 text-xs font-semibold text-slate-500">
          对手公开队伍
        </div>
        <div class="space-y-2">
          <div
            v-for="(pokemon, index) in opponentRoster"
            :key="`opponent-roster-${index}`"
            class="rounded-xl border border-slate-200 bg-white p-3"
          >
            <div class="font-semibold text-slate-900">
              {{ pokemon.name || pokemon.name_en || `宝可梦 ${index + 1}` }}
            </div>
            <div class="text-xs text-slate-500">
              {{ formatTypes(pokemon.types) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-4 rounded-xl bg-white p-4">
      <div class="text-sm text-slate-700">
        已选择 {{ selectedRosterIndexes.length }}/4 只；首发 {{ leadRosterIndexes.length }}/2 只
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
          {{ playerRoster[index]?.name || playerRoster[index]?.name_en || `宝可梦 ${index + 1}` }}{{ isLead(index) ? ' · 首发' : '' }}
        </button>
      </div>
      <button
        class="mt-4 w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="!canConfirmPreview || isBusy"
        @click="confirmPreview"
      >
        {{ busyAction === 'confirm-preview' ? '正在确认阵容...' : '确认 6 选 4 与首发' }}
      </button>
    </div>
  </section>

  <section
    v-if="isReplacementPhase"
    class="rounded-2xl border border-rose-200 bg-[linear-gradient(180deg,rgba(255,228,230,0.72),rgba(255,255,255,0.96))] p-4"
  >
    <div class="mb-3 text-sm font-semibold text-slate-800">
      倒下补位：请选择 {{ pendingReplacementCount }} 只后备宝可梦上场
    </div>
    <div class="space-y-2">
      <button
        v-for="option in replacementBenchOptions"
        :key="`replacement-${option.value}`"
        type="button"
        class="w-full rounded-xl border p-3 text-left"
        :class="selectedReplacementIndexes.includes(option.value) ? 'border-rose-500 bg-white' : 'border-slate-200 bg-white hover:border-slate-300'"
        @click="toggleReplacement(option.value)"
      >
        <div class="flex items-center justify-between gap-3">
          <div>
            <div class="font-semibold text-slate-900">
              {{ option.label }}
            </div>
            <div class="text-xs text-slate-500">
              {{ option.types }}
            </div>
          </div>
          <div class="text-xs text-slate-500">
            HP {{ option.hp }}
          </div>
        </div>
      </button>
    </div>
    <button
      class="mt-4 w-full rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-slate-300"
      :disabled="!canConfirmReplacement || isBusy"
      @click="confirmReplacement"
    >
      {{ busyAction === 'confirm-replacement' ? '正在确认替补...' : '确认替补上场' }}
    </button>
  </section>

  <section class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
    <div class="mb-3 text-sm font-semibold text-slate-800">
      {{ isReplacementPhase ? '当前回合已暂停，等待补位' : '当前可选招式' }}
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
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
          <div>
            <div class="font-semibold text-slate-900">
              {{ mon.name }}
            </div>
            <div class="text-xs text-slate-500">
              槽位 {{ mon.fieldSlot + 1 }} · HP {{ mon.currentHp }}/{{ mon.maxHp }}
            </div>
          </div>
          <div class="text-xs text-slate-500">
            {{ formatTypes(mon.types) }}
          </div>
        </div>

        <select
          v-model="selectedActions[`action-slot-${mon.fieldSlot}`]"
          class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
        >
          <option value="move">
            使用招式
          </option>
          <option
            value="switch"
            :disabled="!playerBenchOptions.length"
          >
            换人
          </option>
        </select>

        <template v-if="selectedActions[`action-slot-${mon.fieldSlot}`] === 'switch'">
          <select
            v-model="selectedSwitchTargets[`switch-slot-${mon.fieldSlot}`]"
            class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          >
            <option
              v-for="target in playerBenchOptions"
              :key="`switch-${mon.fieldSlot}-${target.value}`"
              :value="target.value"
            >
              换上：{{ target.label }} · HP {{ target.hp }}
            </option>
          </select>
        </template>
        <template v-else>
          <select
            v-model="selectedMoves[`slot-${mon.fieldSlot}`]"
            class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          >
            <option
              v-for="move in mon.moves"
              :key="move.name_en || move.name"
              :value="move.name_en || move.name"
            >
              {{ move.name || move.name_en }} · 威力 {{ move.power || 0 }} · 优先度 {{ move.priority || 0 }} · {{ moveTargetText(move) }}
            </option>
          </select>

          <select
            v-if="moveNeedsOpponentTarget(selectedMoveObject(mon))"
            v-model="selectedTargets[`target-slot-${mon.fieldSlot}`]"
            class="mt-3 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          >
            <option
              v-for="target in opponentActiveOptions"
              :key="`target-${mon.fieldSlot}-${target.value}`"
              :value="target.value"
            >
              目标：对手槽位 {{ target.value + 1 }} · {{ target.label }}
            </option>
          </select>
        </template>
      </div>

      <button
        class="w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        :disabled="!canSubmitMove || isBusy"
        @click="submitMove"
      >
        {{ busyAction === 'submit-move' ? '正在提交回合...' : '提交当前回合' }}
      </button>
    </div>
    <div
      v-else
      class="text-sm text-slate-500"
    >
      {{ isPreviewPhase ? '先完成队伍预览后，才能提交回合操作。' : isReplacementPhase ? '有宝可梦倒下时，必须先完成替补上场。' : '先开始一场手动对战后，这里会显示你当前两只在场宝可梦的出招选择。' }}
    </div>
  </section>

  <details class="rounded-2xl border border-slate-200 bg-slate-50 p-4" :open="showDebugPanel" @toggle="emit('toggle-debug-panel', $event.target.open)">
    <summary class="cursor-pointer list-none text-sm font-semibold text-slate-800">
      调试响应
    </summary>
    <div class="mt-2 text-xs leading-5 text-slate-500">
      日常使用时可以收起；排查接口返回时再展开。
    </div>
    <pre class="mt-3 max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-slate-950 p-4 text-xs text-slate-100">{{ resultText }}</pre>
  </details>
</template>

<script setup>
const emit = defineEmits(['toggle-debug-panel'])

defineProps({
  busyAction: { type: String, default: '' },
  canConfirmPreview: { type: Boolean, default: false },
  canConfirmReplacement: { type: Boolean, default: false },
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
  moveNeedsOpponentTarget: { type: Function, required: true },
  moveTargetText: { type: Function, required: true },
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
  selectedMoveObject: { type: Function, required: true },
  selectedMoves: { type: Object, default: () => ({}) },
  selectedReplacementIndexes: { type: Array, default: () => [] },
  selectedRosterIndexes: { type: Array, default: () => [] },
  selectedSwitchTargets: { type: Object, default: () => ({}) },
  selectedTargets: { type: Object, default: () => ({}) },
  showDebugPanel: { type: Boolean, default: false },
  submitMove: { type: Function, required: true },
  toggleLead: { type: Function, required: true },
  toggleReplacement: { type: Function, required: true },
  toggleRoster: { type: Function, required: true }
})
</script>