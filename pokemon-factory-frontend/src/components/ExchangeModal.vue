<template>
  <div class="fixed inset-0 z-[60] flex items-end justify-center bg-slate-950/50 p-2 backdrop-blur-sm sm:items-center sm:p-4">
    <div class="w-full max-w-5xl rounded-t-[28px] border border-white/70 bg-white/95 p-4 shadow-[0_28px_120px_-50px_rgba(15,23,42,0.8)] backdrop-blur sm:rounded-[28px] sm:p-6 max-h-[92vh] overflow-y-auto">
      <div class="flex flex-col gap-4 border-b border-slate-200 pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
            Exchange Reward
          </div>
          <h3 class="mt-2 text-2xl font-black tracking-tight text-slate-950">
            {{ tr('交换宝可梦', 'Exchange Pokemon') }}
          </h3>
          <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
            {{ tr('你赢下了这场战斗。现在可以从对手队伍中挑一只，替换你自己队伍中的一名成员，为后续工厂轮次做准备。', 'You won this battle. Pick one Pokemon from the opponent team to replace a member of your own roster and prepare for later factory rounds.') }}
          </p>
        </div>

        <label class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 shadow-sm">
          {{ tr('替换我方第', 'Replace my slot') }}
          <input
            v-model.number="localReplaced"
            type="number"
            min="1"
            :max="maxSlot"
            class="mx-2 inline-block w-16 rounded-xl border border-slate-300 bg-white px-2 py-1 text-center font-semibold text-slate-900 outline-none ring-0"
          >
          {{ tr('只', '') }}（1-{{ maxSlot }}）
        </label>
      </div>

      <div class="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3 sm:gap-4">
        <div
          v-for="(p, idx) in opponentTeam"
          :key="idx"
          :class="['cursor-pointer rounded-[24px] border p-4 transition shadow-sm', sel === idx ? 'border-sky-500 bg-[linear-gradient(180deg,rgba(224,242,254,0.8),rgba(255,255,255,0.96))] shadow-sky-100' : 'border-slate-200 bg-white hover:-translate-y-0.5 hover:border-slate-300']"
          @click="select(idx)"
        >
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="text-base font-bold text-slate-900 sm:text-lg">
                {{ p.name || p.id }}
              </div>
              <div class="mt-1 text-xs text-slate-500">
                {{ formatTypes(p.types) }}
              </div>
            </div>
            <span
              v-if="sel === idx"
              class="rounded-full bg-sky-100 px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.18em] text-sky-700"
            >
              {{ tr('已选择', 'Selected') }}
            </span>
          </div>
          <div class="mt-3 grid grid-cols-2 gap-2 rounded-2xl bg-slate-50 p-3 text-xs text-slate-500">
            <div>
              <div class="text-[11px] uppercase tracking-[0.16em] text-slate-400">
                {{ tr('持有物', 'Held item') }}
              </div>
              <div class="mt-1 font-semibold text-slate-700">
                {{ p.heldItem || tr('无', 'None') }}
              </div>
            </div>
            <div>
              <div class="text-[11px] uppercase tracking-[0.16em] text-slate-400">
                {{ tr('基础面板', 'Base stats') }}
              </div>
              <div class="mt-1 font-semibold text-slate-700">
                HP {{ p?.stats?.hp || '-' }} / {{ tr('速度', 'Speed') }} {{ p?.stats?.speed || '-' }}
              </div>
            </div>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="move in p.moves || []"
              :key="move.name_en || move.name"
              class="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600"
            >
              {{ move.name || move.name_en }}
            </span>
          </div>
        </div>
      </div>

      <div class="mt-6 flex flex-col gap-3 border-t border-slate-200 pt-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="text-sm text-slate-500">
          {{ sel === null ? tr('先选中一只想带走的对手宝可梦。', 'Select the opponent Pokemon you want to take first.') : tr('将把选中的宝可梦换入我方第 {slot} 号位。', 'The selected Pokemon will replace your slot {slot}.', { slot: Math.max(1, Math.min(maxSlot, localReplaced || 1)) }) }}
        </div>
        <div class="grid grid-cols-2 gap-3 sm:flex sm:gap-3">
          <button
            class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            @click="$emit('close')"
          >
            {{ tr('取消', 'Cancel') }}
          </button>
          <button
            class="rounded-xl bg-sky-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="sel === null || submitting"
            @click="confirm"
          >
            {{ submitting ? tr('交换中...', 'Exchanging...') : tr('确认交换', 'Confirm exchange') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const props = defineProps({
  opponentTeam: {
    type: Array,
    default: () => []
  },
  replacedIndex: Number,
  maxSlot: {
    type: Number,
    default: 6
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emits = defineEmits(['close', 'confirm', 'update:replacedIndex'])

let sel = ref(null)
const localReplaced = ref((props.replacedIndex || 0) + 1)
const maxSlot = props.maxSlot

watch(() => props.replacedIndex, (value) => {
  localReplaced.value = (value || 0) + 1
})

function select(index) {
  sel.value = index
}

function confirm() {
  if (sel.value === null) return
  const newIndex = Math.max(0, Math.min(maxSlot - 1, (localReplaced.value || 1) - 1))
  emits('update:replacedIndex', newIndex)
  emits('confirm', sel.value)
}

function formatTypes(types = []) {
  return (types || []).map((type) => tr(type.name || type.name_zh || `属性${type.type_id}`, type.name_en || type.name || `Type ${type.type_id}`)).join(' / ') || tr('未知属性', 'Unknown type')
}
</script>
