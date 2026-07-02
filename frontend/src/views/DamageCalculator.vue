<template>
  <div class="space-y-6">
    <!-- Header -->
    <section class="rounded-[28px] border border-slate-200 bg-gradient-to-br from-sky-50/80 via-indigo-50/60 to-white p-5 shadow-lg sm:p-7">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div class="max-w-3xl">
          <div class="inline-flex items-center rounded-full bg-white/80 px-3 py-1 text-xs font-bold uppercase tracking-[0.22em] text-sky-700 shadow-sm">Damage Lab</div>
          <h1 class="mt-4 text-[clamp(1.75rem,4vw,2.4rem)] font-black tracking-tight text-slate-950">伤害计算器</h1>
          <p class="mt-3 text-sm leading-6 text-slate-600 sm:text-base">选择攻击方、招式和防御方，查看完整伤害计算过程。</p>
        </div>
        <div class="grid gap-2 sm:grid-cols-3">
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">攻击方</div>
            <div class="mt-1 text-sm font-bold text-slate-900">{{ selectedAttackerLabel || '未选择' }}</div>
          </div>
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">招式</div>
            <div class="mt-1 text-sm font-bold text-slate-900">{{ selectedMoveLabel || '未选择' }}</div>
          </div>
          <div class="rounded-2xl bg-white/90 px-4 py-3 shadow-sm">
            <div class="text-xs uppercase tracking-[0.18em] text-slate-400">防御方</div>
            <div class="mt-1 text-sm font-bold text-slate-900">{{ selectedDefenderLabel || '未选择' }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.25fr_0.9fr]">
      <!-- 左侧：选择区 -->
      <div class="space-y-6">
        <!-- 基础选择 -->
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold">⚔</div>
            <h2 class="text-lg font-bold text-slate-900">基础选择</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">攻击方宝可梦</label>
              <el-select v-model="form.attackerPokemonId" filterable remote reserve-keyword default-first-option placeholder="选择攻击方" class="w-full" :loading="pokemonLoading" :remote-method="searchPokemonOptions" @change="handleAttackerChange">
                <el-option v-for="p in pokemonOptions" :key="'a-'+p.id" :label="pokemonOptionLabel(p)" :value="p.id" />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">防御方宝可梦</label>
              <el-select v-model="form.defenderPokemonId" filterable remote reserve-keyword default-first-option placeholder="选择防御方" class="w-full" :loading="pokemonLoading" :remote-method="searchPokemonOptions" @change="handleDefenderChange">
                <el-option v-for="p in pokemonOptions" :key="'d-'+p.id" :label="pokemonOptionLabel(p)" :value="p.id" />
              </el-select>
            </div>
            <div class="lg:col-span-2 flex gap-2">
              <el-button plain :disabled="!form.attackerPokemonId || !form.defenderPokemonId" @click="swapPokemonSides">⇄ 交换攻防方</el-button>
            </div>
            <div class="space-y-2 lg:col-span-2">
              <label class="text-sm font-bold text-slate-700">攻击招式</label>
              <el-select v-model="form.moveId" filterable placeholder="先选攻击方，再选招式" class="w-full" :loading="moveLoading" :disabled="!attackerFormId">
                <el-option v-for="m in attackerMoves" :key="m.id" :label="moveOptionLabel(m)" :value="m.id" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 战斗条件 -->
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-white text-xs font-bold">⚡</div>
            <h2 class="text-lg font-bold text-slate-900">战斗条件</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-3">
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">等级</label>
              <el-input-number v-model="form.attackerLevel" :min="1" :max="100" class="w-full" />
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">天气</label>
              <el-select v-model="form.weather" placeholder="无天气" clearable class="w-full">
                <el-option label="☀ 晴天" value="sun" />
                <el-option label="🌧 下雨" value="rain" />
                <el-option label="🌪 沙暴" value="sand" />
                <el-option label="❄ 冰雹/雪天" value="snow" />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">场地</label>
              <el-select v-model="form.terrain" placeholder="无场地" clearable class="w-full">
                <el-option label="⚡电气场地" value="electric" />
                <el-option label="🔮精神场地" value="psychic" />
                <el-option label="🌿青草场地" value="grassy" />
                <el-option label="🌫薄雾场地" value="misty" />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">攻击方攻击/特攻阶级</label>
              <el-select v-model="form.attackerAttackBoost" placeholder="±0" class="w-full">
                <el-option v-for="i in [-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6]" :key="i" :label="`${i>0?'+':''}${i}`" :value="i" />
              </el-select>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-bold text-slate-700">防御方防御/特防阶级</label>
              <el-select v-model="form.defenderDefenseBoost" placeholder="±0" class="w-full">
                <el-option v-for="i in [-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6]" :key="i" :label="`${i>0?'+':''}${i}`" :value="i" />
              </el-select>
            </div>
          </div>
          <div class="mt-5 pt-4 border-t border-slate-100 flex flex-wrap gap-3">
            <el-switch v-model="form.isCritical" inline-prompt :active-text="'🎯 暴击'" :inactive-text="'普通命中'" />
            <el-switch v-model="form.isDoubleBattle" inline-prompt :active-text="'双打'" :inactive-text="'单打'" />
            <el-switch v-model="form.attackerBurned" inline-prompt :active-text="'🔥 攻击方烧伤'" :inactive-text="'未烧伤'" />
            <el-switch v-model="form.reflectActive" inline-prompt :active-text="'🛡 反射壁'" :inactive-text="'无反射壁'" />
            <el-switch v-model="form.lightScreenActive" inline-prompt :active-text="'✨ 光墙'" :inactive-text="'无光墙'" />
            <el-switch v-model="form.auroraVeilActive" inline-prompt :active-text="'🌈 极光幕'" :inactive-text="'无极光幕'" />
          </div>
        </div>

        <!-- 特性与道具 -->
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-purple-500 to-pink-600 flex items-center justify-center text-white text-xs font-bold">✦</div>
            <h2 class="text-lg font-bold text-slate-900">特性与道具</h2>
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div class="rounded-2xl bg-gradient-to-br from-indigo-50/60 to-white border border-indigo-100 p-4">
              <div class="text-xs font-bold uppercase tracking-wider text-indigo-500 mb-3">攻击方</div>
              <div class="space-y-3">
                <div>
                  <label class="text-xs font-bold text-slate-600">特性</label>
                  <el-select v-model="form.attackerAbilityId" filterable placeholder="选特性（可选）" clearable class="w-full mt-1">
                    <el-option v-for="a in filteredAttackerAbilities" :key="'aa-'+a.id" :label="a.name" :value="a.id" />
                  </el-select>
                </div>
                <div>
                  <label class="text-xs font-bold text-slate-600">道具</label>
                  <el-select v-model="form.attackerItemId" filterable remote :remote-method="searchItems" placeholder="选道具（可选）" clearable class="w-full mt-1" :loading="itemLoading">
                    <el-option v-for="i in itemOptions" :key="'ai-'+i.id" :label="i.name" :value="i.id" />
                  </el-select>
                </div>
              </div>
            </div>
            <div class="rounded-2xl bg-gradient-to-br from-rose-50/60 to-white border border-rose-100 p-4">
              <div class="text-xs font-bold uppercase tracking-wider text-rose-500 mb-3">防御方</div>
              <div class="space-y-3">
                <div>
                  <label class="text-xs font-bold text-slate-600">特性</label>
                  <el-select v-model="form.defenderAbilityId" filterable placeholder="选特性（可选）" clearable class="w-full mt-1">
                    <el-option v-for="a in filteredDefenderAbilities" :key="'da-'+a.id" :label="a.name" :value="a.id" />
                  </el-select>
                </div>
                <div>
                  <label class="text-xs font-bold text-slate-600">道具</label>
                  <el-select v-model="form.defenderItemId" filterable remote :remote-method="searchItems" placeholder="选道具（可选）" clearable class="w-full mt-1" :loading="itemLoading">
                    <el-option v-for="i in itemOptions" :key="'di-'+i.id" :label="i.name" :value="i.id" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 能力值覆盖 -->
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-5">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white text-xs font-bold">📊</div>
            <div>
              <h2 class="text-lg font-bold text-slate-900">能力值覆盖</h2>
              <p class="text-xs text-slate-400">留空则使用数据库默认种族值</p>
            </div>
          </div>
          <div class="grid gap-5 lg:grid-cols-2">
            <div class="rounded-2xl bg-gradient-to-br from-indigo-50/60 to-white border border-indigo-100 p-4">
              <div class="text-xs font-bold uppercase tracking-wider text-indigo-500 mb-3">攻击方</div>
              <div class="grid grid-cols-3 gap-2">
                <div><label class="text-[10px] text-slate-500">攻击</label><el-input-number v-model="form.attackerAtkOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">特攻</label><el-input-number v-model="form.attackerSpAOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">速度</label><el-input-number v-model="form.attackerSpeOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
              </div>
            </div>
            <div class="rounded-2xl bg-gradient-to-br from-rose-50/60 to-white border border-rose-100 p-4">
              <div class="text-xs font-bold uppercase tracking-wider text-rose-500 mb-3">防御方</div>
              <div class="grid grid-cols-3 gap-2">
                <div><label class="text-[10px] text-slate-500">HP</label><el-input-number v-model="form.defenderHpOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">攻击</label><el-input-number v-model="form.defenderAtkOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">防御</label><el-input-number v-model="form.defenderDefOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">特攻</label><el-input-number v-model="form.defenderSpAOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">特防</label><el-input-number v-model="form.defenderSpDOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
                <div><label class="text-[10px] text-slate-500">速度</label><el-input-number v-model="form.defenderSpeOv" :min="0" :max="999" :step="1" size="small" class="w-full" /></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 计算按钮 -->
        <div class="flex flex-wrap items-center gap-3 p-2">
          <el-button type="primary" size="large" :loading="calculating" :disabled="!canCalculate" @click="calculateDamage" class="!px-8 !text-base !font-bold !shadow-lg !bg-gradient-to-r !from-blue-600 !to-indigo-600 !border-none hover:!from-blue-700 hover:!to-indigo-700">🎯 计算伤害</el-button>
          <el-button size="large" @click="resetCalculator">↺ 重置条件</el-button>
          <span class="text-sm text-slate-500">{{ helperText }}</span>
        </div>
      </div>

      <!-- 右侧：结果区 -->
      <div class="space-y-6">
        <!-- 计算结果 -->
        <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 class="text-lg font-bold text-slate-900">计算结果</h2>
          <div v-if="result" class="mt-4 space-y-4">
            <!-- 伤害区间 + 击倒估算 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl bg-slate-950 px-4 py-4 text-white">
                <div class="text-xs uppercase tracking-[0.2em] text-slate-300">伤害区间</div>
                <div class="mt-2 text-2xl font-black">{{ result.minDamage }} - {{ result.maxDamage }}</div>
                <div class="mt-1 text-sm text-slate-300">平均 {{ formatNumber(result.avgDamage) }}</div>
              </div>
              <div class="rounded-2xl bg-emerald-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.2em] text-emerald-600">伤害占比</div>
                <div class="mt-2 text-lg font-bold text-emerald-900">{{ result.koEstimate?.koPercentRange || '暂无' }}</div>
                <div class="mt-1 text-sm text-emerald-700">防御方 HP: {{ result.koEstimate?.defenderHp ?? '-' }}</div>
                <div v-if="result.koEstimate?.koChance" class="mt-2 inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-200 text-emerald-800 text-xs font-bold">
                   OHKO {{ (result.koEstimate.koChance * 100).toFixed(1) }}%
                </div>
              </div>
            </div>

            <!-- 属性相性 + 命中率 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">属性相性</div>
                <div class="mt-2 text-base font-bold text-slate-900">{{ result.effectivenessDesc || '未知' }}</div>
                <div class="mt-1 text-sm text-slate-500">倍率 {{ formatNumber(result.typeEffectiveness) }}</div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">命中率</div>
                <div class="mt-2 text-base font-bold text-slate-900">{{ result.accuracyDesc || '暂无' }}</div>
                <div class="mt-1 text-sm text-slate-500">基础 {{ result.baseAccuracy ?? '-' }} · 最终 {{ formatPercent(result.finalAccuracy) }}</div>
              </div>
            </div>

            <!-- 招式信息 + 关键修正 -->
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">招式信息</div>
                <div class="mt-2 text-base font-bold text-slate-900">{{ result.damageClass || '未知分类' }}</div>
                <div class="mt-1 text-sm text-slate-500">有效威力 {{ result.effectivePower ?? '-' }} · 优先度 {{ result.priority ?? '-' }} · 连续攻击 {{ result.hits ?? 1 }} 次</div>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div class="text-xs uppercase tracking-[0.18em] text-slate-400">使用的能力</div>
                <div class="mt-2 text-base font-bold text-slate-900">{{ result.usedAttackType === 'attack' ? '攻击' : '特攻' }} vs {{ result.usedAttackType === 'attack' ? '防御' : '特防' }}</div>
                <div class="mt-1 text-sm text-slate-500">{{ result.usedAttackStat ?? '-' }} vs {{ result.usedDefenseStat ?? '-' }}</div>
              </div>
            </div>

            <!-- 修正倍率 -->
            <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
              <div class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400 mb-2">修正倍率</div>
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 text-sm">
                <div v-for="(v, k) in result.allMultipliers" :key="k" class="flex justify-between bg-white rounded-lg px-3 py-1.5">
                  <span class="text-slate-500">{{ k }}</span>
                  <span class="font-bold text-slate-800">{{ formatNumber(v) }}x</span>
                </div>
              </div>
            </div>

            <!-- 特性/道具效果 -->
            <div v-if="result.attackerAbilityEffect || result.defenderAbilityEffect || result.attackerItemEffect || result.defenderItemEffect" class="space-y-2">
              <div v-if="result.attackerAbilityEffect" class="rounded-xl bg-indigo-50 border border-indigo-100 px-4 py-3 text-sm text-indigo-700">{{ result.attackerAbilityEffect }}</div>
              <div v-if="result.defenderAbilityEffect" class="rounded-xl bg-rose-50 border border-rose-100 px-4 py-3 text-sm text-rose-700">{{ result.defenderAbilityEffect }}</div>
              <div v-if="result.attackerItemEffect" class="rounded-xl bg-amber-50 border border-amber-100 px-4 py-3 text-sm text-amber-700">{{ result.attackerItemEffect }}</div>
              <div v-if="result.defenderItemEffect" class="rounded-xl bg-teal-50 border border-teal-100 px-4 py-3 text-sm text-teal-700">{{ result.defenderItemEffect }}</div>
            </div>

            <!-- 计算步骤 -->
            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-sm font-bold text-slate-800 mb-3">计算步骤</div>
              <div class="space-y-2 max-h-64 overflow-y-auto">
                <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
                  <div class="flex items-start justify-between gap-3">
                    <div><div class="font-bold text-slate-900 text-sm">基础威力</div><div class="mt-0.5 text-xs text-slate-500">技能原始威力</div></div>
                    <div class="text-sm font-bold text-slate-700">{{ result.movePower ?? '-' }}</div>
                  </div>
                </div>
                <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
                  <div class="flex items-start justify-between gap-3">
                    <div><div class="font-bold text-slate-900 text-sm">有效威力</div><div class="mt-0.5 text-xs text-slate-500">特性/道具修正后</div></div>
                    <div class="text-sm font-bold text-slate-700">{{ result.effectivePower ?? '-' }}</div>
                  </div>
                </div>
                <div class="rounded-xl bg-white px-4 py-3 shadow-sm">
                  <div class="flex items-start justify-between gap-3">
                    <div><div class="font-bold text-slate-900 text-sm">攻击 × 防御</div><div class="mt-0.5 text-xs text-slate-500">{{ result.usedAttackType === 'attack' ? '攻击' : '特攻' }} vs {{ result.usedAttackType === 'attack' ? '防御' : '特防' }}</div></div>
                    <div class="text-sm font-bold text-slate-700">{{ result.usedAttackStat ?? '-' }} vs {{ result.usedDefenseStat ?? '-' }}</div>
                  </div>
                </div>
                <div v-for="(v, k) in result.allMultipliers" :key="k" class="rounded-xl bg-white px-4 py-3 shadow-sm">
                  <div class="flex items-start justify-between gap-3">
                    <div><div class="font-bold text-slate-900 text-sm">{{ k }}修正</div><div class="mt-0.5 text-xs text-slate-500">倍率</div></div>
                    <div class="text-sm font-bold text-slate-700">{{ formatNumber(v) }}x</div>
                  </div>
                </div>
                <div class="rounded-xl bg-indigo-50 border border-indigo-100 px-4 py-3 shadow-sm">
                  <div class="flex items-start justify-between gap-3">
                    <div><div class="font-bold text-indigo-900 text-sm">伤害区间</div><div class="mt-0.5 text-xs text-indigo-500">随机因子 0.85-1.00</div></div>
                    <div class="text-sm font-bold text-indigo-700">{{ result.minDamage }} - {{ result.maxDamage }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-sm text-center text-slate-500">
            选择攻击方、招式和防御方后点击"计算伤害"
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

const DEFAULT_FORM = () => ({
  attackerPokemonId: null,
  defenderPokemonId: null,
  moveId: null,
  attackerLevel: 50,
  weather: '',
  terrain: '',
  isCritical: false,
  isDoubleBattle: false,
  attackerBurned: false,
  reflectActive: false,
  lightScreenActive: false,
  auroraVeilActive: false,
  attackerAbilityId: null,
  defenderAbilityId: null,
  attackerItemId: null,
  defenderItemId: null,
  attackerAttackBoost: 0,
  attackerSpAttackBoost: 0,
  defenderDefenseBoost: 0,
  defenderSpDefenseBoost: 0,
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
  if (!form.attackerPokemonId) return '先搜索并选择攻击方宝可梦。'
  if (!form.moveId) return '再从攻击方可学招式里选择本次使用的技能。'
  if (!form.defenderPokemonId) return '最后选择防御方宝可梦即可开始计算。'
  return '条件已齐备，可以直接发起伤害计算。'
})

function pokemonOptionLabel(p) {
  return `${p.name || p.nameEn || '宝可梦 #' + p.id} · #${p.id}`
}
function moveOptionLabel(m) {
  return `${m.name || m.nameEn || '招式 #' + m.id} · ${m.typeName || '未知'} · 威力 ${m.power ?? 0}`
}
function formatNumber(v) {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return '-'
  return Number(v).toFixed(Number(v) % 1 === 0 ? 0 : 2)
}
function formatPercent(v) {
  if (v === null || v === undefined) return '-'
  return (v * 100).toFixed(1) + '%'
}

function mergePokemonOptions(records) {
  const merged = new Map()
  for (const p of [...pokemonOptions.value, ...records]) {
    if (p?.id) merged.set(p.id, p)
  }
  pokemonOptions.value = Array.from(merged.values()).sort((a, b) => a.id - b.id)
}

async function searchPokemonOptions(keyword = '') {
  const token = ++latestPokemonSearchToken
  try {
    pokemonLoading.value = true
    const res = await api.pokemon.getList({ current: 1, size: keyword ? 30 : 24, ...(keyword ? { keyword } : {}) })
    if (token !== latestPokemonSearchToken) return
    mergePokemonOptions(res.data?.records || [])
  } catch (e) {
    ElMessage.error(e?.message || '加载宝可梦列表失败')
  } finally {
    if (token === latestPokemonSearchToken) pokemonLoading.value = false
  }
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
  } catch {
    attackerMoves.value = []; form.moveId = null
  } finally { moveLoading.value = false }
}

async function fetchPokemonFormAbilities(pokemonId, isAttacker) {
  try {
    const res = await api.pokemon.getDetail(pokemonId)
    if (res.code === 200) {
      const forms = res.data?.forms || []
      const allAbilities = []
      for (const f of forms) {
        if (f.abilities) {
          for (const a of f.abilities) {
            if (!allAbilities.find(x => x.id === a.id)) allAbilities.push(a)
          }
        }
      }
      const ids = allAbilities.map(a => a.id)
      if (isAttacker) attackerAbilityIds.value = ids
      else defenderAbilityIds.value = ids
    }
  } catch { /* ignore */ }
}

async function handleAttackerChange() {
  result.value = null
  attackerAbilityIds.value = []
  form.attackerAbilityId = null
  if (form.attackerPokemonId) await fetchPokemonFormAbilities(form.attackerPokemonId, true)
  await loadAttackerMoves()
}

function handleDefenderChange() {
  result.value = null
  defenderAbilityIds.value = []
  form.defenderAbilityId = null
  if (form.defenderPokemonId) fetchPokemonFormAbilities(form.defenderPokemonId, false)
}

async function swapPokemonSides() {
  if (!form.attackerPokemonId || !form.defenderPokemonId) return
  const tmp = form.attackerPokemonId; form.attackerPokemonId = form.defenderPokemonId; form.defenderPokemonId = tmp
  result.value = null; await loadAttackerMoves()
}

async function calculateDamage() {
  if (!canCalculate.value) { ElMessage.warning('请先完整选择攻击方、招式和防御方'); return }
  try {
    calculating.value = true
    const res = await api.damage.calculate({
      attackerFormId: attackerFormId.value,
      defenderFormId: defenderFormId.value,
      moveId: form.moveId,
      attackerLevel: form.attackerLevel,
      weather: form.weather || null,
      terrain: form.terrain || null,
      isCritical: form.isCritical,
      isDoubleBattle: form.isDoubleBattle,
      attackerBurned: form.attackerBurned,
      reflectActive: form.reflectActive,
      lightScreenActive: form.lightScreenActive,
      auroraVeilActive: form.auroraVeilActive,
      attackerAbilityId: form.attackerAbilityId,
      defenderAbilityId: form.defenderAbilityId,
      attackerItemId: form.attackerItemId,
      defenderItemId: form.defenderItemId,
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
    ElMessage.success('伤害计算完成')
  } catch (e) {
    result.value = null; ElMessage.error(e?.message || '伤害计算失败')
  } finally { calculating.value = false }
}

function resetCalculator() {
  Object.assign(form, DEFAULT_FORM()); attackerMoves.value = []; result.value = null
}

onMounted(async () => {
  await searchPokemonOptions()
  if (pokemonOptions.value[0]) form.attackerPokemonId = pokemonOptions.value[0].id
  if (pokemonOptions.value[1]) form.defenderPokemonId = pokemonOptions.value[1].id
  else if (pokemonOptions.value[0]) form.defenderPokemonId = pokemonOptions.value[0].id
  await loadAttackerMoves()
  searchAbilities()
  searchItems()
})
</script>
