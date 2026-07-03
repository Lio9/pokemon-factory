<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="relative overflow-hidden rounded-[28px] bg-gradient-to-br from-indigo-600 via-blue-600 to-sky-500 p-6 sm:p-8 shadow-2xl">
      <div class="absolute -top-20 -right-20 w-60 h-60 bg-white/5 rounded-full blur-3xl" />
      <div class="absolute -bottom-10 -left-10 w-40 h-40 bg-white/5 rounded-full blur-2xl" />
      <div class="relative flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="text-white">
          <div class="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-xs font-bold uppercase tracking-[0.22em] text-white/80 backdrop-blur-sm">Damage Lab</div>
          <h1 class="mt-4 text-3xl sm:text-4xl font-black tracking-tight">伤害计算器</h1>
          <p class="mt-2 text-sm text-white/70">选择宝可梦、招式和条件，一键计算伤害区间</p>
        </div>
        <div class="grid gap-2 sm:grid-cols-3 shrink-0">
          <div v-for="s in [{l:'攻击方',v:selectedAttackerLabel},{l:'招式',v:selectedMoveLabel},{l:'防御方',v:selectedDefenderLabel}]" :key="s.l" class="rounded-2xl bg-white/10 backdrop-blur-md px-4 py-3 min-w-[120px]">
            <div class="text-[10px] uppercase tracking-widest text-white/50">{{ s.l }}</div>
            <div class="mt-0.5 text-sm font-bold text-white truncate">{{ s.v || '未选择' }}</div>
          </div>
        </div>
      </div>
    </div>

    <section class="grid gap-6 xl:grid-cols-[1.25fr_0.9fr]">
      <!-- 左侧 -->
      <div class="space-y-5">
        <!-- 基础选择 -->
        <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-5">
          <div class="flex items-center gap-2.5 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-sm shadow-sm">⚔</div>
            <h2 class="text-base font-bold text-slate-800">基础选择</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">攻击方宝可梦</label>
              <el-select v-model="form.attackerPokemonId" filterable remote reserve-keyword default-first-option placeholder="选择攻击方" class="w-full" :loading="pokemonLoading" :remote-method="searchPokemonOptions" @change="handleAttackerChange">
                <el-option v-for="p in pokemonOptions" :key="'a-'+p.id" :label="pokemonOptionLabel(p)" :value="p.id" />
              </el-select>
            </div>
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">防御方宝可梦</label>
              <el-select v-model="form.defenderPokemonId" filterable remote reserve-keyword default-first-option placeholder="选择防御方" class="w-full" :loading="pokemonLoading" :remote-method="searchPokemonOptions" @change="handleDefenderChange">
                <el-option v-for="p in pokemonOptions" :key="'d-'+p.id" :label="pokemonOptionLabel(p)" :value="p.id" />
              </el-select>
            </div>
            <div class="lg:col-span-2"><label class="block text-xs font-bold text-slate-500 mb-1.5">攻击招式</label>
              <el-select v-model="form.moveId" filterable placeholder="先选攻击方，再选招式" class="w-full" :loading="moveLoading" :disabled="!attackerFormId">
                <el-option v-for="m in attackerMoves" :key="m.id" :label="moveOptionLabel(m)" :value="m.id" />
              </el-select>
            </div>
            <div class="lg:col-span-2 flex gap-2">
              <el-button size="small" plain :disabled="!form.attackerPokemonId || !form.defenderPokemonId" @click="swapPokemonSides">⇄ 交换</el-button>
            </div>
          </div>
        </div>

        <!-- 战斗条件 -->
        <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-5">
          <div class="flex items-center gap-2.5 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-500 to-orange-500 flex items-center justify-center text-white text-sm shadow-sm">⚡</div>
            <h2 class="text-base font-bold text-slate-800">战斗条件</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-3">
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">等级</label><el-input-number v-model="form.attackerLevel" :min="1" :max="100" class="w-full" size="default" /></div>
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">天气</label>
              <el-select v-model="form.weather" placeholder="无" clearable class="w-full">
                <el-option v-for="w in [{v:'sun',l:'☀ 晴天'},{v:'rain',l:'🌧 下雨'},{v:'sand',l:'🌪 沙暴'},{v:'snow',l:'❄ 冰雹'}]" :key="w.v" :label="w.l" :value="w.v" />
              </el-select>
            </div>
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">场地</label>
              <el-select v-model="form.terrain" placeholder="无" clearable class="w-full">
                <el-option v-for="t in [{v:'electric',l:'⚡电气'},{v:'psychic',l:'🔮精神'},{v:'grassy',l:'🌿青草'},{v:'misty',l:'🌫薄雾'}]" :key="t.v" :label="t.l" :value="t.v" />
              </el-select>
            </div>
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">攻击阶级</label>
              <el-select v-model="form.attackerAttackBoost" class="w-full">
                <el-option v-for="i in [-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6]" :key="i" :label="`${i>0?'+':''}${i}`" :value="i" />
              </el-select>
            </div>
            <div><label class="block text-xs font-bold text-slate-500 mb-1.5">防御阶级</label>
              <el-select v-model="form.defenderDefenseBoost" class="w-full">
                <el-option v-for="i in [-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6]" :key="i" :label="`${i>0?'+':''}${i}`" :value="i" />
              </el-select>
            </div>
          </div>
          <div class="mt-4 pt-4 border-t border-slate-100 flex flex-wrap gap-x-4 gap-y-2">
            <el-switch v-for="s in switches" :key="s.key" v-model="form[s.key]" inline-prompt :active-text="s.on" :inactive-text="s.off" size="small" />
          </div>
        </div>

        <!-- 特性道具 -->
        <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-5">
          <div class="flex items-center gap-2.5 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white text-sm shadow-sm">✦</div>
            <h2 class="text-base font-bold text-slate-800">特性与道具</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div class="rounded-xl bg-indigo-50/60 border border-indigo-100 p-4">
              <div class="text-xs font-bold mb-3 text-indigo-500 uppercase tracking-wider">攻击方</div>
              <div class="space-y-3">
                <div><label class="block text-[11px] font-bold text-slate-500 mb-1">特性</label>
                  <el-select v-model="form.attackerAbilityId" filterable placeholder="可选" clearable class="w-full" size="default">
                    <el-option v-for="a in filteredAttackerAbilities" :key="'aa-'+a.id" :label="a.name" :value="a.id" />
                  </el-select>
                </div>
                <div><label class="block text-[11px] font-bold text-slate-500 mb-1">道具</label>
                  <el-select v-model="form.attackerItemId" filterable remote :remote-method="searchItems" placeholder="可选" clearable class="w-full" :loading="itemLoading" size="default">
                    <el-option v-for="i in itemOptions" :key="'ai-'+i.id" :label="i.name" :value="i.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div class="rounded-xl bg-rose-50/60 border border-rose-100 p-4">
              <div class="text-xs font-bold mb-3 text-rose-500 uppercase tracking-wider">防御方</div>
              <div class="space-y-3">
                <div><label class="block text-[11px] font-bold text-slate-500 mb-1">特性</label>
                  <el-select v-model="form.defenderAbilityId" filterable placeholder="可选" clearable class="w-full" size="default">
                    <el-option v-for="a in filteredDefenderAbilities" :key="'da-'+a.id" :label="a.name" :value="a.id" />
                  </el-select>
                </div>
                <div><label class="block text-[11px] font-bold text-slate-500 mb-1">道具</label>
                  <el-select v-model="form.defenderItemId" filterable remote :remote-method="searchItems" placeholder="可选" clearable class="w-full" :loading="itemLoading" size="default">
                    <el-option v-for="i in itemOptions" :key="'di-'+i.id" :label="i.name" :value="i.id" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加点 -->
        <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-5">
          <div class="flex items-center gap-2.5 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-500 flex items-center justify-center text-white text-sm shadow-sm">📊</div>
            <div><h2 class="text-base font-bold text-slate-800">加点调整</h2><p class="text-[11px] text-slate-400">个体值默认 31 满 · 填入数值直接覆盖</p></div>
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div class="rounded-xl bg-indigo-50/60 border border-indigo-100 p-5">
              <div class="text-xs font-bold mb-4 text-indigo-500 uppercase tracking-wider">攻击方</div>
              <div class="flex gap-4">
                <div v-for="s in [{k:'attackerAtkOv',l:'攻击'},{k:'attackerSpAOv',l:'特攻'},{k:'attackerSpeOv',l:'速度'}]" :key="s.k" class="flex-1">
                  <label class="block text-center text-[11px] font-bold text-slate-400 mb-1.5">{{ s.l }}</label>
                  <el-input-number v-model="form[s.k]" :min="0" :max="999" :step="5" size="default" class="!w-full" controls-position="right" />
                </div>
              </div>
            </div>
            <div class="rounded-xl bg-rose-50/60 border border-rose-100 p-5">
              <div class="text-xs font-bold mb-4 text-rose-500 uppercase tracking-wider">防御方</div>
              <div class="grid grid-cols-2 gap-x-6 gap-y-3">
                <div v-for="s in [{k:'defenderHpOv',l:'HP'},{k:'defenderAtkOv',l:'攻击'},{k:'defenderDefOv',l:'防御'},{k:'defenderSpAOv',l:'特攻'},{k:'defenderSpDOv',l:'特防'},{k:'defenderSpeOv',l:'速度'}]" :key="s.k">
                  <label class="block text-[11px] font-bold text-slate-400 mb-1">{{ s.l }}</label>
                  <el-input-number v-model="form[s.k]" :min="0" :max="999" :step="5" size="default" class="!w-full" controls-position="right" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 按钮 -->
        <div class="flex items-center gap-3 px-1">
          <el-button type="primary" size="large" :loading="calculating" :disabled="!canCalculate" @click="calculateDamage" class="!px-8 !font-bold !shadow-lg !bg-gradient-to-r !from-blue-600 !to-indigo-600 !border-none hover:!from-blue-700 hover:!to-indigo-700">计算伤害</el-button>
          <el-button size="large" @click="resetCalculator">重置</el-button>
          <span class="text-xs text-slate-400 ml-auto">{{ helperText }}</span>
        </div>
      </div>

      <!-- 右侧：结果 -->
      <div class="space-y-5">
        <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-5">
          <div class="flex items-center gap-2.5 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-rose-500 to-pink-500 flex items-center justify-center text-white text-sm shadow-sm">📋</div>
            <h2 class="text-base font-bold text-slate-800">计算结果</h2>
          </div>

          <div v-if="result" class="space-y-4">
            <!-- 伤害 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-xl bg-gradient-to-br from-slate-900 to-slate-800 px-5 py-4 text-white shadow-inner">
                <div class="text-[10px] uppercase tracking-widest text-slate-400">伤害区间</div>
                <div class="mt-1.5 text-2xl font-black tracking-tight">{{ result.minDamage }} ~ {{ result.maxDamage }}</div>
                <div class="mt-0.5 text-xs text-slate-400">平均 {{ formatNumber(result.avgDamage) }}</div>
              </div>
              <div class="rounded-xl bg-gradient-to-br from-emerald-50 to-emerald-100/60 border border-emerald-200/60 px-5 py-4">
                <div class="text-[10px] uppercase tracking-widest text-emerald-600">伤害占比</div>
                <div class="mt-1 text-lg font-bold text-emerald-900">{{ result.koEstimate?.koPercentRange || '-' }}</div>
                <div class="mt-0.5 text-xs text-emerald-600/70">防御方 HP {{ result.koEstimate?.defenderHp ?? '-' }}</div>
                <div v-if="result.koEstimate?.koChance" class="mt-2 inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-200/80 text-emerald-800 text-[11px] font-bold">OHKO {{ (result.koEstimate.koChance * 100).toFixed(1) }}%</div>
              </div>
            </div>

            <!-- 属性命中 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
                <div class="text-[10px] uppercase tracking-widest text-slate-400">属性相性</div>
                <div class="mt-1 text-base font-bold text-slate-800">{{ result.effectivenessDesc || '-' }}</div>
                <div class="text-xs text-slate-400 mt-0.5">倍率 {{ formatNumber(result.typeEffectiveness) }}x</div>
              </div>
              <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
                <div class="text-[10px] uppercase tracking-widest text-slate-400">命中率</div>
                <div class="mt-1 text-base font-bold text-slate-800">{{ result.accuracyDesc || '-' }}</div>
                <div class="text-xs text-slate-400 mt-0.5">基础 {{ result.baseAccuracy ?? '-' }} · 最终 {{ formatPercent(result.finalAccuracy) }}</div>
              </div>
            </div>

            <!-- 能力 + 招式 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
                <div class="text-[10px] uppercase tracking-widest text-slate-400">使用能力</div>
                <div class="mt-1 text-base font-bold text-slate-800">{{ result.usedAttackType === 'attack' ? '攻击' : '特攻' }} vs {{ result.usedAttackType === 'attack' ? '防御' : '特防' }}</div>
                <div class="text-xs text-slate-400 mt-0.5">{{ result.usedAttackStat ?? '-' }} vs {{ result.usedDefenseStat ?? '-' }}</div>
              </div>
              <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
                <div class="text-[10px] uppercase tracking-widest text-slate-400">招式</div>
                <div class="mt-1 text-base font-bold text-slate-800">{{ result.damageClass || '-' }}</div>
                <div class="text-xs text-slate-400 mt-0.5">威力 {{ result.effectivePower ?? '-' }} · 优先度 {{ result.priority ?? 0 }}</div>
              </div>
            </div>

            <!-- 倍率 -->
            <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
              <div class="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2.5">修正倍率</div>
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                <div v-for="(v, k) in result.allMultipliers" :key="k" class="flex justify-between bg-white rounded-lg px-3 py-1.5 text-xs">
                  <span class="text-slate-500">{{ k }}</span>
                  <span class="font-bold text-slate-700">{{ formatNumber(v) }}x</span>
                </div>
              </div>
            </div>

            <!-- 特性道具效果 -->
            <div v-if="result.attackerAbilityEffect || result.defenderAbilityEffect || result.attackerItemEffect || result.defenderItemEffect" class="space-y-1.5">
              <div v-if="result.attackerAbilityEffect" class="rounded-xl bg-indigo-50 border border-indigo-100 px-4 py-2.5 text-xs text-indigo-700">{{ result.attackerAbilityEffect }}</div>
              <div v-if="result.defenderAbilityEffect" class="rounded-xl bg-rose-50 border border-rose-100 px-4 py-2.5 text-xs text-rose-700">{{ result.defenderAbilityEffect }}</div>
              <div v-if="result.attackerItemEffect" class="rounded-xl bg-amber-50 border border-amber-100 px-4 py-2.5 text-xs text-amber-700">{{ result.attackerItemEffect }}</div>
              <div v-if="result.defenderItemEffect" class="rounded-xl bg-teal-50 border border-teal-100 px-4 py-2.5 text-xs text-teal-700">{{ result.defenderItemEffect }}</div>
            </div>

            <!-- 步骤 -->
            <div class="rounded-xl bg-slate-50 border border-slate-200/60 px-4 py-3.5">
              <div class="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-2.5">计算步骤</div>
              <div class="space-y-1 max-h-48 overflow-y-auto text-xs">
                <div v-for="(v,k) in result.allMultipliers" :key="k" class="flex justify-between bg-white rounded-lg px-3 py-2 border border-slate-100/60">
                  <span class="text-slate-600">{{ k }}修正</span>
                  <span class="font-bold text-slate-800">{{ formatNumber(v) }}x</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="py-12 text-center text-sm text-slate-400">
            <div class="text-3xl mb-2">📊</div>
            <p>选择攻击方、招式和防御方后<br>点击「计算伤害」查看结果</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../services/api'

const switches = [
  { key: 'isCritical', on: '🎯 暴击', off: '普通' },
  { key: 'isDoubleBattle', on: '双打', off: '单打' },
  { key: 'attackerBurned', on: '🔥 烧伤', off: '未烧伤' },
  { key: 'reflectActive', on: '🛡 反射壁', off: '无' },
  { key: 'lightScreenActive', on: '✨ 光墙', off: '无' },
  { key: 'auroraVeilActive', on: '🌈 极光幕', off: '无' }
]

const DEFAULT_FORM = () => ({
  attackerPokemonId: null, defenderPokemonId: null, moveId: null,
  attackerLevel: 50, weather: '', terrain: '',
  isCritical: false, isDoubleBattle: false, attackerBurned: false,
  reflectActive: false, lightScreenActive: false, auroraVeilActive: false,
  attackerAbilityId: null, defenderAbilityId: null,
  attackerItemId: null, defenderItemId: null,
  attackerAttackBoost: 0, defenderDefenseBoost: 0,
  attackerAtkOv: null, attackerSpAOv: null, attackerSpeOv: null,
  defenderHpOv: null, defenderAtkOv: null, defenderDefOv: null,
  defenderSpAOv: null, defenderSpDOv: null, defenderSpeOv: null
})

const form = reactive(DEFAULT_FORM())
const pokemonOptions = ref([])
const attackerMoves = ref([])
const abilityOptions = ref([])
const attackerAbilityIds = ref([])
const defenderAbilityIds = ref([])
const itemOptions = ref([])
const result = ref(null)
const pokemonLoading = ref(false)
const moveLoading = ref(false)
const abilityLoading = ref(false)
const itemLoading = ref(false)
const calculating = ref(false)
let latestPokemonSearchToken = 0

const filteredAttackerAbilities = computed(() =>
  abilityOptions.value.filter(a => attackerAbilityIds.value.length === 0 || attackerAbilityIds.value.includes(a.id))
)
const filteredDefenderAbilities = computed(() =>
  abilityOptions.value.filter(a => defenderAbilityIds.value.length === 0 || defenderAbilityIds.value.includes(a.id))
)

const attackerPokemon = computed(() => pokemonOptions.value.find(p => p.id === form.attackerPokemonId) || null)
const defenderPokemon = computed(() => pokemonOptions.value.find(p => p.id === form.defenderPokemonId) || null)
const selectedMove = computed(() => attackerMoves.value.find(m => m.id === form.moveId) || null)
const attackerFormId = computed(() => attackerPokemon.value?.defaultFormId || null)
const defenderFormId = computed(() => defenderPokemon.value?.defaultFormId || null)
const canCalculate = computed(() => Boolean(attackerFormId.value && defenderFormId.value && form.moveId))
const selectedAttackerLabel = computed(() => attackerPokemon.value ? pokemonOptionLabel(attackerPokemon.value) : '')
const selectedDefenderLabel = computed(() => defenderPokemon.value ? pokemonOptionLabel(defenderPokemon.value) : '')
const selectedMoveLabel = computed(() => selectedMove.value ? moveOptionLabel(selectedMove.value) : '')
const helperText = computed(() => {
  if (!form.attackerPokemonId) return '先选择攻击方宝可梦'
  if (!form.moveId) return '再选择招式'
  if (!form.defenderPokemonId) return '最后选择防御方'
  return '条件齐备，点击计算'
})

function pokemonOptionLabel(p) { return `${p.name || p.nameEn || '#' + p.id} · #${p.id}` }
function moveOptionLabel(m) { return `${m.name || m.nameEn || '#' + m.id} · ${m.typeName || '?'} · ${m.power ?? 0}` }
function formatNumber(v) { return (v === null || v === undefined || Number.isNaN(Number(v))) ? '-' : Number(v).toFixed(Number(v) % 1 === 0 ? 0 : 2) }
function formatPercent(v) { return v === null || v === undefined ? '-' : (v * 100).toFixed(1) + '%' }

function mergePokemonOptions(records) {
  const merged = new Map()
  for (const p of [...pokemonOptions.value, ...records]) if (p?.id) merged.set(p.id, p)
  pokemonOptions.value = Array.from(merged.values()).sort((a, b) => a.id - b.id)
}

async function searchPokemonOptions(keyword = '') {
  const token = ++latestPokemonSearchToken
  try {
    pokemonLoading.value = true
    const res = await api.pokemon.getList({ current: 1, size: keyword ? 30 : 24, ...(keyword ? { keyword } : {}) })
    if (token !== latestPokemonSearchToken) return
    mergePokemonOptions(res.data?.records || [])
  } catch (e) { ElMessage.error(e?.message || '加载宝可梦列表失败') }
  finally { if (token === latestPokemonSearchToken) pokemonLoading.value = false }
}

async function searchAbilities(keyword = '') {
  try {
    abilityLoading.value = true
    const res = await api.abilities.getList({ current: 1, size: 200, ...(keyword ? { keyword } : {}) })
    abilityOptions.value = res.data?.records || []
  } catch { /* ignore */ } finally { abilityLoading.value = false }
}

async function searchItems(keyword = '') {
  try {
    itemLoading.value = true
    const res = await api.items.getList({ current: 1, size: 200, ...(keyword ? { keyword } : {}) })
    itemOptions.value = res.data?.records || []
  } catch { /* ignore */ } finally { itemLoading.value = false }
}

async function loadAttackerMoves() {
  if (!attackerFormId.value) { attackerMoves.value = []; form.moveId = null; return }
  try {
    moveLoading.value = true
    const res = await api.pokemon.getFormMoves(attackerFormId.value)
    const moves = res.data || []
    attackerMoves.value = moves
    if (!moves.some(m => m.id === form.moveId)) form.moveId = moves[0]?.id || null
  } catch { attackerMoves.value = []; form.moveId = null }
  finally { moveLoading.value = false }
}

async function fetchPokemonFormAbilities(pokemonId, isAttacker) {
  try {
    const res = await api.pokemon.getDetail(pokemonId)
    if (res.code === 200) {
      const allAb = []
      for (const f of res.data?.forms || [])
        if (f.abilities) for (const a of f.abilities)
          if (!allAb.find(x => x.id === a.id)) allAb.push(a)
      if (isAttacker) attackerAbilityIds.value = allAb.map(a => a.id)
      else defenderAbilityIds.value = allAb.map(a => a.id)
    }
  } catch { /* ignore */ }
}

async function handleAttackerChange() {
  result.value = null; attackerAbilityIds.value = []; form.attackerAbilityId = null
  if (form.attackerPokemonId) await fetchPokemonFormAbilities(form.attackerPokemonId, true)
  await loadAttackerMoves()
}

function handleDefenderChange() {
  result.value = null; defenderAbilityIds.value = []; form.defenderAbilityId = null
  if (form.defenderPokemonId) fetchPokemonFormAbilities(form.defenderPokemonId, false)
}

async function swapPokemonSides() {
  if (!form.attackerPokemonId || !form.defenderPokemonId) return
  const tmp = form.attackerPokemonId; form.attackerPokemonId = form.defenderPokemonId; form.defenderPokemonId = tmp
  result.value = null; await loadAttackerMoves()
}

async function calculateDamage() {
  if (!canCalculate.value) { ElMessage.warning('请先完整选择'); return }
  try {
    calculating.value = true
    const res = await api.damage.calculate({
      attackerFormId: attackerFormId.value, defenderFormId: defenderFormId.value,
      moveId: form.moveId, attackerLevel: form.attackerLevel,
      weather: form.weather || null, terrain: form.terrain || null,
      isCritical: form.isCritical, isDoubleBattle: form.isDoubleBattle,
      attackerBurned: form.attackerBurned,
      reflectActive: form.reflectActive, lightScreenActive: form.lightScreenActive,
      auroraVeilActive: form.auroraVeilActive,
      attackerAbilityId: form.attackerAbilityId, defenderAbilityId: form.defenderAbilityId,
      attackerItemId: form.attackerItemId, defenderItemId: form.defenderItemId,
      attackerAttackBoost: form.attackerAttackBoost,
      attackerSpAttackBoost: form.attackerAttackBoost,
      defenderDefenseBoost: form.defenderDefenseBoost,
      defenderSpDefenseBoost: form.defenderDefenseBoost,
      attackerAttack: form.attackerAtkOv || undefined,
      attackerSpAttack: form.attackerSpAOv || undefined,
      defenderHp: form.defenderHpOv || undefined,
      defenderDefense: form.defenderDefOv || undefined,
      defenderSpDefense: form.defenderSpDOv || undefined
    })
    result.value = res.data || null
  } catch (e) { result.value = null; ElMessage.error(e?.message || '计算失败') }
  finally { calculating.value = false }
}

function resetCalculator() {
  Object.assign(form, DEFAULT_FORM()); attackerMoves.value = []; result.value = null
}

onMounted(async () => {
  await searchPokemonOptions()
  if (pokemonOptions.value[0]) {
    form.attackerPokemonId = pokemonOptions.value[0].id
    await fetchPokemonFormAbilities(pokemonOptions.value[0].id, true)
  }
  if (pokemonOptions.value[1]) {
    form.defenderPokemonId = pokemonOptions.value[1].id
    fetchPokemonFormAbilities(pokemonOptions.value[1].id, false)
  } else if (pokemonOptions.value[0]) {
    form.defenderPokemonId = pokemonOptions.value[0].id
    fetchPokemonFormAbilities(pokemonOptions.value[0].id, false)
  }
  await loadAttackerMoves()
  searchAbilities()
  searchItems()
})
</script>
