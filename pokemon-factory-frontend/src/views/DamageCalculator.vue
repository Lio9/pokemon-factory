<template>
  <div class="space-y-6">
    <section class="rounded-[28px] border border-slate-200 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(99,102,241,0.1),rgba(255,255,255,0.92))] p-5 shadow-[0_24px_80px_-56px_rgba(14,165,233,0.55)] sm:p-7">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="inline-flex items-center rounded-full bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-sky-700 shadow-sm">
            Damage Lab
          </div>
          <h1 class="mt-4 text-[clamp(1.75rem,4vw,2.4rem)] font-black tracking-tight text-slate-950">
            {{ tr('伤害计算器', 'Damage Calculator') }}
          </h1>
          <p class="mt-3 text-sm leading-6 text-slate-600 sm:text-base">
            {{ tr('直接基于当前后端伤害计算接口，选择攻击方、招式和防御方后即可得到伤害区间、属性相性、命中率和击倒估算。', 'Powered directly by the backend damage API. Pick the attacker, move, and defender to get damage range, type matchup, accuracy, and KO estimates.') }}
          </p>
        </div>
        <div class="grid gap-3 sm:grid-cols-3">
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
              {{ tr('攻击方', 'Attacker') }}
            </div>
            <div class="mt-2 text-sm font-semibold text-slate-900">
              {{ selectedAttackerLabel || tr('未选择', 'Not selected') }}
            </div>
          </div>
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
              {{ tr('招式', 'Move') }}
            </div>
            <div class="mt-2 text-sm font-semibold text-slate-900">
              {{ selectedMoveLabel || tr('未选择', 'Not selected') }}
            </div>
          </div>
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
              {{ tr('防御方', 'Defender') }}
            </div>
            <div class="mt-2 text-sm font-semibold text-slate-900">
              {{ selectedDefenderLabel || tr('未选择', 'Not selected') }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(360px,0.9fr)]">
      <div class="space-y-6">
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 class="text-lg font-bold text-slate-900">
            {{ tr('基础选择', 'Base selection') }}
          </h2>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="space-y-2">
              <label class="text-sm font-semibold text-slate-700">{{ tr('攻击方宝可梦', 'Attacker Pokemon') }}</label>
              <el-select
                v-model="form.attackerPokemonId"
                filterable
                remote
                reserve-keyword
                default-first-option
                :placeholder="tr('选择攻击方', 'Choose attacker')"
                class="w-full"
                :loading="pokemonLoading"
                :remote-method="searchPokemonOptions"
                @change="handleAttackerChange"
              >
                <el-option
                  v-for="pokemon in pokemonOptions"
                  :key="`attacker-${pokemon.id}`"
                  :label="pokemonOptionLabel(pokemon)"
                  :value="pokemon.id"
                />
              </el-select>
            </div>

            <div class="space-y-2">
              <label class="text-sm font-semibold text-slate-700">{{ tr('防御方宝可梦', 'Defender Pokemon') }}</label>
              <el-select
                v-model="form.defenderPokemonId"
                filterable
                remote
                reserve-keyword
                default-first-option
                :placeholder="tr('选择防御方', 'Choose defender')"
                class="w-full"
                :loading="pokemonLoading"
                :remote-method="searchPokemonOptions"
                @change="handleDefenderChange"
              >
                <el-option
                  v-for="pokemon in pokemonOptions"
                  :key="`defender-${pokemon.id}`"
                  :label="pokemonOptionLabel(pokemon)"
                  :value="pokemon.id"
                />
              </el-select>
            </div>

            <div class="lg:col-span-2">
              <el-button
                plain
                :disabled="!form.attackerPokemonId || !form.defenderPokemonId"
                @click="swapPokemonSides"
              >
                {{ tr('交换攻防方', 'Swap attacker and defender') }}
              </el-button>
            </div>

            <div class="space-y-2 lg:col-span-2">
              <label class="text-sm font-semibold text-slate-700">{{ tr('攻击招式', 'Attack move') }}</label>
              <el-select
                v-model="form.moveId"
                filterable
                :placeholder="tr('先选择攻击方，再选择该形态可学招式', 'Choose the attacker first, then pick a learnable move for that form')"
                class="w-full"
                :loading="moveLoading"
                :disabled="!attackerFormId"
              >
                <el-option
                  v-for="move in attackerMoves"
                  :key="move.id"
                  :label="moveOptionLabel(move)"
                  :value="move.id"
                />
              </el-select>
            </div>
          </div>
        </div>

        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 class="text-lg font-bold text-slate-900">
            {{ tr('计算条件', 'Calculation conditions') }}
          </h2>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="space-y-2">
              <label class="text-sm font-semibold text-slate-700">{{ tr('攻击方等级', 'Attacker level') }}</label>
              <el-input-number
                v-model="form.attackerLevel"
                :min="1"
                :max="100"
                class="w-full"
              />
            </div>

            <div class="space-y-2">
              <label class="text-sm font-semibold text-slate-700">{{ tr('天气', 'Weather') }}</label>
              <el-select
                v-model="form.weather"
                :placeholder="tr('无天气', 'No weather')"
                clearable
                class="w-full"
              >
                <el-option
                  :label="tr('晴天', 'Sun')"
                  value="sun"
                />
                <el-option
                  :label="tr('下雨', 'Rain')"
                  value="rain"
                />
                <el-option
                  :label="tr('沙暴', 'Sandstorm')"
                  value="sand"
                />
                <el-option
                  :label="tr('冰雹 / 雪天', 'Hail / Snow')"
                  value="snow"
                />
              </el-select>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-sm font-semibold text-slate-800">
                {{ tr('攻击侧状态', 'Attacker state') }}
              </div>
              <div class="mt-3 space-y-3">
                <el-switch
                  v-model="form.isCritical"
                  inline-prompt
                  :active-text="tr('暴击', 'Crit')"
                  :inactive-text="tr('普通', 'Normal')"
                />
                <el-switch
                  v-model="form.attackerBurned"
                  inline-prompt
                  :active-text="tr('灼伤', 'Burned')"
                  :inactive-text="tr('未灼伤', 'Healthy')"
                />
                <el-switch
                  v-model="form.isDoubleBattle"
                  inline-prompt
                  :active-text="tr('双打', 'Doubles')"
                  :inactive-text="tr('单打', 'Singles')"
                />
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-sm font-semibold text-slate-800">
                {{ tr('防御侧场地', 'Defender side field') }}
              </div>
              <div class="mt-3 space-y-3">
                <el-switch
                  v-model="form.reflectActive"
                  inline-prompt
                  :active-text="tr('反射壁', 'Reflect')"
                  :inactive-text="tr('无反射壁', 'No Reflect')"
                />
                <el-switch
                  v-model="form.lightScreenActive"
                  inline-prompt
                  :active-text="tr('光墙', 'Light Screen')"
                  :inactive-text="tr('无光墙', 'No Light Screen')"
                />
                <el-switch
                  v-model="form.auroraVeilActive"
                  inline-prompt
                  :active-text="tr('极光幕', 'Aurora Veil')"
                  :inactive-text="tr('无极光幕', 'No Aurora Veil')"
                />
              </div>
            </div>
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-3">
            <el-button
              type="primary"
              size="large"
              :loading="calculating"
              :disabled="!canCalculate"
              @click="calculateDamage"
            >
              {{ tr('计算伤害', 'Calculate damage') }}
            </el-button>
            <el-button
              size="large"
              @click="resetCalculator"
            >
              {{ tr('重置条件', 'Reset conditions') }}
            </el-button>
            <span class="text-sm text-slate-500">
              {{ helperText }}
            </span>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 class="text-lg font-bold text-slate-900">
            {{ tr('计算结果', 'Calculation result') }}
          </h2>

          <div
            v-if="result"
            class="mt-4 space-y-4"
          >
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl bg-slate-950 px-4 py-4 text-white">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-300">
                  {{ tr('伤害区间', 'Damage range') }}
                </div>
                <div class="mt-2 text-2xl font-black">
                  {{ result.minDamage }} - {{ result.maxDamage }}
                </div>
                <div class="mt-2 text-sm text-slate-300">
                  {{ tr('平均伤害', 'Average damage') }} {{ formatNumber(result.avgDamage) }}
                </div>
              </div>
              <div class="rounded-2xl bg-emerald-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-emerald-600">
                  {{ tr('击倒估算', 'KO estimate') }}
                </div>
                <div class="mt-2 text-lg font-bold text-emerald-900">
                  {{ result.koEstimate?.koPercentRange || tr('暂无', 'N/A') }}
                </div>
                <div class="mt-2 text-sm text-emerald-700">
                  {{ tr('最少', 'Min') }} {{ result.koEstimate?.minHits ?? '-' }} {{ tr('次', 'hits') }}，{{ tr('最多', 'Max') }} {{ result.koEstimate?.maxHits ?? '-' }} {{ tr('次', 'hits') }}
                </div>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {{ tr('属性相性', 'Type matchup') }}
                </div>
                <div class="mt-2 text-base font-bold text-slate-900">
                  {{ result.effectivenessDesc || tr('未知', 'Unknown') }}
                </div>
                <div class="mt-1 text-sm text-slate-500">
                  {{ tr('倍率', 'Multiplier') }} {{ formatNumber(result.typeEffectiveness) }}
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {{ tr('命中率', 'Accuracy') }}
                </div>
                <div class="mt-2 text-base font-bold text-slate-900">
                  {{ result.accuracyDesc || tr('暂无', 'N/A') }}
                </div>
                <div class="mt-1 text-sm text-slate-500">
                  {{ tr('基础', 'Base') }} {{ result.baseAccuracy ?? '-' }} · {{ tr('最终', 'Final') }} {{ formatPercent(result.finalAccuracy) }}
                </div>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {{ tr('招式信息', 'Move info') }}
                </div>
                <div class="mt-2 text-base font-bold text-slate-900">
                  {{ result.moveTypeName || selectedMoveLabel || tr('未命名招式', 'Unnamed move') }}
                </div>
                <div class="mt-1 text-sm text-slate-500">
                  {{ result.damageClass || tr('未知分类', 'Unknown class') }} · {{ tr('有效威力', 'Effective power') }} {{ result.effectivePower ?? '-' }} · {{ tr('优先度', 'Priority') }} {{ result.priority ?? '-' }}
                </div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {{ tr('关键修正', 'Key modifiers') }}
                </div>
                <div class="mt-2 space-y-1 text-sm text-slate-600">
                  <div>{{ tr('本系加成', 'STAB') }}：{{ result.isStab ? tr(`是（${formatNumber(result.stabMultiplier)}x）`, `Yes (${formatNumber(result.stabMultiplier)}x)`) : tr('否', 'No') }}</div>
                  <div>{{ tr('天气修正', 'Weather') }}：{{ formatNumber(result.weatherMultiplier) }}x</div>
                  <div>{{ tr('烧伤修正', 'Burn') }}：{{ formatNumber(result.burnMultiplier) }}x</div>
                  <div>{{ tr('双打修正', 'Doubles') }}：{{ formatNumber(result.multiTargetMultiplier) }}x</div>
                </div>
              </div>
            </div>

            <div
              v-if="result.calculationSteps?.length"
              class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
            >
              <div class="text-sm font-semibold text-slate-800">
                {{ tr('计算步骤', 'Calculation steps') }}
              </div>
              <div class="mt-3 space-y-3">
                <div
                  v-for="(step, index) in result.calculationSteps"
                  :key="`${step.name}-${index}`"
                  class="rounded-xl bg-white px-4 py-3 shadow-sm"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <div class="font-semibold text-slate-900">
                        {{ step.name }}
                      </div>
                      <div class="mt-1 text-sm text-slate-500">
                        {{ step.description || step.formula }}
                      </div>
                    </div>
                    <div class="text-sm font-semibold text-slate-700">
                      {{ formatNumber(step.value) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else
            class="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-sm leading-6 text-slate-500"
          >
            {{ tr('先选择攻击方、招式和防御方，再点击“计算伤害”。结果会展示真实接口返回的伤害区间、击倒概率和修正步骤。', 'Choose the attacker, move, and defender first, then click “Calculate damage”. The result shows real API output including damage range, KO odds, and modifier steps.') }}
          </div>
        </div>

        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 class="text-lg font-bold text-slate-900">
            {{ tr('当前选择摘要', 'Current selection summary') }}
          </h2>
          <div class="mt-4 space-y-3 text-sm text-slate-600">
            <div class="rounded-2xl bg-slate-50 px-4 py-3">
              <div class="font-semibold text-slate-800">
                {{ tr('攻击方', 'Attacker') }}
              </div>
              <div class="mt-1">
                {{ selectedAttackerLabel || tr('未选择', 'Not selected') }}
              </div>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-3">
              <div class="font-semibold text-slate-800">
                {{ tr('招式', 'Move') }}
              </div>
              <div class="mt-1">
                {{ selectedMoveLabel || tr('未选择', 'Not selected') }}
              </div>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-3">
              <div class="font-semibold text-slate-800">
                {{ tr('防御方', 'Defender') }}
              </div>
              <div class="mt-1">
                {{ selectedDefenderLabel || tr('未选择', 'Not selected') }}
              </div>
            </div>
            <div class="rounded-2xl bg-slate-50 px-4 py-3">
              <div class="font-semibold text-slate-800">
                {{ tr('额外条件', 'Extra conditions') }}
              </div>
              <div class="mt-1">
                {{ tr('等级', 'Level') }} {{ form.attackerLevel }} · {{ form.isCritical ? tr('暴击', 'Crit') : tr('普通命中', 'Normal hit') }} · {{ form.isDoubleBattle ? tr('双打', 'Doubles') : tr('单打', 'Singles') }} · {{ weatherLabel }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../services/api'
import { useLocale } from '../composables/useLocale'

const { translate: tr } = useLocale()

const DEFAULT_FORM = () => ({
  attackerPokemonId: null,
  defenderPokemonId: null,
  moveId: null,
  attackerLevel: 50,
  weather: '',
  isCritical: false,
  isDoubleBattle: false,
  attackerBurned: false,
  reflectActive: false,
  lightScreenActive: false,
  auroraVeilActive: false
})

const form = reactive(DEFAULT_FORM())
const pokemonOptions = ref([])
const attackerMoves = ref([])
const result = ref(null)
const pokemonLoading = ref(false)
const moveLoading = ref(false)
const calculating = ref(false)
let latestPokemonSearchToken = 0

const attackerPokemon = computed(() => pokemonOptions.value.find((pokemon) => pokemon.id === form.attackerPokemonId) || null)
const defenderPokemon = computed(() => pokemonOptions.value.find((pokemon) => pokemon.id === form.defenderPokemonId) || null)
const selectedMove = computed(() => attackerMoves.value.find((move) => move.id === form.moveId) || null)
const attackerFormId = computed(() => attackerPokemon.value?.defaultFormId || null)
const defenderFormId = computed(() => defenderPokemon.value?.defaultFormId || null)
const canCalculate = computed(() => Boolean(attackerFormId.value && defenderFormId.value && form.moveId))

const selectedAttackerLabel = computed(() => attackerPokemon.value ? pokemonOptionLabel(attackerPokemon.value) : '')
const selectedDefenderLabel = computed(() => defenderPokemon.value ? pokemonOptionLabel(defenderPokemon.value) : '')
const selectedMoveLabel = computed(() => selectedMove.value ? moveOptionLabel(selectedMove.value) : '')
const weatherLabel = computed(() => {
  if (!form.weather) {
    return tr('无天气', 'No weather')
  }
  return {
    sun: tr('晴天', 'Sun'),
    rain: tr('下雨', 'Rain'),
    sand: tr('沙暴', 'Sandstorm'),
    snow: tr('冰雹 / 雪天', 'Hail / Snow')
  }[form.weather] || form.weather
})
const helperText = computed(() => {
  if (!form.attackerPokemonId) {
    return tr('先搜索并选择攻击方宝可梦。', 'Search for and choose the attacking Pokemon first.')
  }
  if (!form.moveId) {
    return tr('再从攻击方可学招式里选择本次使用的技能。', 'Then choose the move to use from the attacker’s learnset.')
  }
  if (!form.defenderPokemonId) {
    return tr('最后选择防御方宝可梦即可开始计算。', 'Finally choose the defending Pokemon to start the calculation.')
  }
  return tr('条件已齐备，可以直接发起伤害计算。', 'Everything is ready. You can calculate damage now.')
})

function pokemonOptionLabel(pokemon) {
  return `${pokemon.name || pokemon.nameEn || tr(`宝可梦 #${pokemon.id}`, `Pokemon #${pokemon.id}`)} · #${pokemon.id}`
}

function moveOptionLabel(move) {
  return `${move.name || move.nameEn || tr(`招式 #${move.id}`, `Move #${move.id}`)} · ${move.typeName || tr('未知属性', 'Unknown type')} · ${tr('威力', 'Power')} ${move.power ?? 0}`
}

function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toFixed(Number(value) % 1 === 0 ? 0 : 2)
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(2)}%`
}

function mergePokemonOptions(records) {
  const merged = new Map()
  for (const pokemon of [...pokemonOptions.value, ...records]) {
    if (pokemon?.id) {
      merged.set(pokemon.id, pokemon)
    }
  }
  pokemonOptions.value = Array.from(merged.values()).sort((left, right) => left.id - right.id)
}

async function searchPokemonOptions(keyword = '') {
  const searchToken = ++latestPokemonSearchToken
  try {
    pokemonLoading.value = true
    const response = await api.pokemon.getList({
      current: 1,
      size: keyword ? 30 : 24,
      ...(keyword ? { keyword } : {})
    })
    if (searchToken !== latestPokemonSearchToken) {
      return
    }
    mergePokemonOptions(response.data?.records || [])
  } catch (error) {
    ElMessage.error(error?.message || tr('加载宝可梦列表失败', 'Failed to load Pokemon list'))
  } finally {
    if (searchToken === latestPokemonSearchToken) {
      pokemonLoading.value = false
    }
  }
}

async function loadAttackerMoves() {
  if (!attackerFormId.value) {
    attackerMoves.value = []
    form.moveId = null
    return
  }

  try {
    moveLoading.value = true
    const response = await api.pokemon.getFormMoves(attackerFormId.value)
    const moves = response.data || []
    attackerMoves.value = moves
    if (!moves.some((move) => move.id === form.moveId)) {
      form.moveId = moves[0]?.id || null
    }
  } catch (error) {
    attackerMoves.value = []
    form.moveId = null
    ElMessage.error(error?.message || tr('加载招式列表失败', 'Failed to load move list'))
  } finally {
    moveLoading.value = false
  }
}

async function handleAttackerChange() {
  result.value = null
  await loadAttackerMoves()
}

function handleDefenderChange() {
  result.value = null
}

async function swapPokemonSides() {
  if (!form.attackerPokemonId || !form.defenderPokemonId) {
    return
  }
  const attackerPokemonId = form.attackerPokemonId
  form.attackerPokemonId = form.defenderPokemonId
  form.defenderPokemonId = attackerPokemonId
  result.value = null
  await loadAttackerMoves()
}

async function calculateDamage() {
  if (!canCalculate.value) {
    ElMessage.warning(tr('请先完整选择攻击方、招式和防御方', 'Choose attacker, move, and defender first'))
    return
  }

  try {
    calculating.value = true
    const response = await api.damage.calculate({
      attackerFormId: attackerFormId.value,
      defenderFormId: defenderFormId.value,
      moveId: form.moveId,
      attackerLevel: form.attackerLevel,
      weather: form.weather || null,
      isCritical: form.isCritical,
      isDoubleBattle: form.isDoubleBattle,
      attackerBurned: form.attackerBurned,
      reflectActive: form.reflectActive,
      lightScreenActive: form.lightScreenActive,
      auroraVeilActive: form.auroraVeilActive
    })
    result.value = response.data || null
    ElMessage.success(tr('伤害计算完成', 'Damage calculation finished'))
  } catch (error) {
    result.value = null
    ElMessage.error(error?.message || tr('伤害计算失败', 'Damage calculation failed'))
  } finally {
    calculating.value = false
  }
}

function resetCalculator() {
  Object.assign(form, DEFAULT_FORM())
  attackerMoves.value = []
  result.value = null
}

onMounted(async () => {
  await searchPokemonOptions()
  if (pokemonOptions.value[0]) {
    form.attackerPokemonId = pokemonOptions.value[0].id
  }
  if (pokemonOptions.value[1]) {
    form.defenderPokemonId = pokemonOptions.value[1].id
  } else if (pokemonOptions.value[0]) {
    form.defenderPokemonId = pokemonOptions.value[0].id
  }
  await loadAttackerMoves()
})
</script>
