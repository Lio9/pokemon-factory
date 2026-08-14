<template>
  <div class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm">
    <!-- 头部 -->
    <div
      class="flex items-center gap-2 px-4 py-2.5"
      :style="{ background: headerGradient }"
    >
      <span class="text-sm font-bold text-white">{{ headerIcon }} {{ sideLabel }}</span>
      <span
        v-if="pokemon"
        class="ml-auto text-xs font-medium text-white/80"
      >#{{ pokemon.id }} {{ pokemon.name }}</span>
    </div>

    <div class="space-y-3 p-4">
      <!-- 选择宝可梦 -->
      <el-select
        :model-value="form[pokemonIdField]"
        filterable
        remote
        reserve-keyword
        default-first-option
        :placeholder="tr('搜索宝可梦...', 'Search Pokemon...')"
        class="w-full"
        :loading="pokemonLoading"
        :remote-method="searchPokemon"
        size="large"
        @change="handlePokemonChange"
      >
        <el-option
          v-for="p in pokemonOptions"
          :key="side + '-' + p.id"
          :label="pokemonOptionLabel(p)"
          :value="p.id"
        >
          <div class="flex items-center gap-2">
            <img
              v-if="p.spriteUrl"
              :src="p.spriteUrl"
              class="h-8 w-8 object-contain"
              @error="$event.target.style.display='none'"
            >
            <span class="font-medium">{{ p.name || p.nameEn }}</span>
            <span class="ml-auto text-xs text-slate-400">#{{ p.id }}</span>
          </div>
        </el-option>
      </el-select>

      <!-- 属性标签 -->
      <div
        v-if="types.length"
        class="flex gap-1.5"
      >
        <span
          v-for="t in types"
          :key="t.type_id"
          class="rounded-full px-2.5 py-0.5 text-[11px] font-bold text-white shadow-sm"
          :style="{ background: typeColorById(t.type_id) }"
        >{{ t.name }}</span>
      </div>

      <!-- 招式选择（仅攻击方通过 slot 注入） -->
      <slot name="moves" />

      <!-- 特性 & 道具 -->
      <div class="grid grid-cols-2 gap-2">
        <div>
          <label class="mb-1 block text-[11px] font-bold text-slate-400">{{ tr('特性', 'Ability') }}</label>
          <el-select
            :model-value="form[abilityIdField]"
            filterable
            :placeholder="tr('可选', 'Optional')"
            clearable
            class="w-full"
            size="default"
            @change="setForm(abilityIdField, $event)"
          >
            <el-option
              v-for="a in filteredAbilities"
              :key="a.id"
              :label="a.name"
              :value="a.id"
            />
          </el-select>
        </div>
        <div>
          <label class="mb-1 block text-[11px] font-bold text-slate-400">{{ tr('道具', 'Item') }}</label>
          <el-select
            :model-value="form[itemIdField]"
            filterable
            remote
            :remote-method="searchItems"
            :placeholder="tr('可选', 'Optional')"
            clearable
            class="w-full"
            :loading="itemLoading"
            size="default"
            @change="setForm(itemIdField, $event)"
          >
            <el-option
              v-for="i in itemOptions"
              :key="i.id"
              :label="i.name"
              :value="i.id"
            />
          </el-select>
        </div>
      </div>

      <!-- 能力阶级 / HP% -->
      <div
        class="grid gap-2"
        :class="boostFields.length === 1 ? 'grid-cols-2' : 'grid-cols-3'"
      >
        <div v-for="boost in boostFields">
          <label class="mb-1 block text-[11px] font-bold text-slate-400">{{ boost.label }}</label>
          <el-select
            :model-value="form[boost.key]"
            class="w-full"
            size="default"
            @change="setForm(boost.key, $event)"
          >
            <el-option
              v-for="i in boostOptions"
              :key="i"
              :label="boostLabel(i)"
              :value="i"
            />
          </el-select>
        </div>
        <div>
          <label class="mb-1 block text-[11px] font-bold text-slate-400">HP%</label>
          <el-input-number
            :model-value="form[hpPercentField]"
            :min="1"
            :max="100"
            class="w-full"
            size="default"
            @update:model-value="setForm(hpPercentField, $event)"
          />
        </div>
      </div>

      <!-- 状态 -->
      <div
        v-if="statuses.length"
        class="flex flex-wrap gap-1.5"
      >
        <button
          v-for="s in statuses"
          :key="s.key"
          type="button"
          class="rounded-lg border px-2.5 py-1 text-[11px] font-bold transition-all"
          :class="form[s.key] ? 'bg-red-50 border-red-300 text-red-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
          :aria-pressed="String(Boolean(form[s.key]))"
          @click="setForm(s.key, !form[s.key])"
        >
          {{ s.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useLocale } from '../composables/useLocale'
import { typeColor as typeColorById } from '../services/typeChart'

const { translate: tr } = useLocale()

const props = defineProps({
  side: { type: String, required: true }, // 'attacker' | 'defender'
  form: { type: Object, required: true },
  pokemon: { type: Object, default: null },
  pokemonOptions: { type: Array, default: () => [] },
  pokemonLoading: { type: Boolean, default: false },
  types: { type: Array, default: () => [] },
  abilities: { type: Array, default: () => [] },
  abilityIds: { type: Array, default: () => [] },
  itemOptions: { type: Array, default: () => [] },
  itemLoading: { type: Boolean, default: false },
  statuses: { type: Array, default: () => [] },
  boostFields: { type: Array, required: true },
  searchPokemon: { type: Function, required: true },
  searchItems: { type: Function, required: true },
  onPokemonChange: { type: Function, required: true }
})

const isAttacker = computed(() => props.side === 'attacker')
const sideLabel = computed(() => tr(isAttacker.value ? '攻击方' : '防御方', isAttacker.value ? 'Attacker' : 'Defender'))
const headerIcon = computed(() => isAttacker.value ? '⚔️' : '🛡️')
const headerGradient = computed(() => isAttacker.value
  ? 'linear-gradient(90deg, #3b82f6, #6366f1)'
  : 'linear-gradient(90deg, #f43f5e, #ec4899)')

const pokemonIdField = computed(() => `${props.side}PokemonId`)
const abilityIdField = computed(() => `${props.side}AbilityId`)
const itemIdField = computed(() => `${props.side}ItemId`)
const hpPercentField = computed(() => `${props.side}HpPercent`)

const boostOptions = [-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6]

const filteredAbilities = computed(() =>
  props.abilityIds.length === 0 ? props.abilities : props.abilities.filter(a => props.abilityIds.includes(a.id))
)

function boostLabel(i) { return i > 0 ? `+${i}` : `${i}` }
function pokemonOptionLabel(p) { return `${p.name || p.nameEn || '#' + p.id}` }

function setForm(key, value) {
  props.form[key] = value
}

function handlePokemonChange(id) {
  props.form[pokemonIdField.value] = id
  props.onPokemonChange(id)
}
</script>
