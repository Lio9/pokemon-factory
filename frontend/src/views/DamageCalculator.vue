<template>
  <div class="max-w-7xl mx-auto space-y-5 pb-8">
    <!-- 顶部标题栏 -->
    <div class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-poke-red via-red-600 to-orange-500 p-5 sm:p-7 shadow-[0_20px_60px_-30px_rgba(220,38,38,0.55)]">
      <div class="absolute inset-0 opacity-10">
        <svg
          viewBox="0 0 400 400"
          class="absolute -right-20 -top-24 h-96 w-96"
        >
          <circle
            cx="200"
            cy="200"
            r="180"
            fill="none"
            stroke="#fff"
            stroke-width="8"
          />
          <line
            x1="20"
            y1="200"
            x2="380"
            y2="200"
            stroke="#fff"
            stroke-width="8"
          />
          <circle
            cx="200"
            cy="200"
            r="40"
            fill="none"
            stroke="#fff"
            stroke-width="8"
          />
          <circle
            cx="200"
            cy="200"
            r="20"
            fill="#fff"
          />
        </svg>
      </div>
      <div class="relative flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <div class="inline-flex items-center gap-1.5 rounded-full bg-white/20 px-3 py-1 text-[11px] font-bold uppercase tracking-widest text-white">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-300 animate-pulse" />
            Damage Calculator
          </div>
          <h1 class="mt-2 text-xl sm:text-2xl font-black text-white tracking-tight drop-shadow">
            伤害计算器
          </h1>
          <p class="mt-1 text-xs text-white/80 sm:text-sm">
            模拟招式伤害，包含属性克制、天气、场地、能力阶级与 KO 概率估算
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            class="px-3 py-1.5 rounded-lg bg-white/15 hover:bg-white/25 text-white text-xs font-semibold transition backdrop-blur-sm"
            @click="resetCalculator"
          >
            重置
          </button>
          <button
            :disabled="!form.attackerPokemonId || !form.defenderPokemonId"
            class="px-3 py-1.5 rounded-lg bg-white/15 hover:bg-white/25 disabled:opacity-40 text-white text-xs font-semibold transition backdrop-blur-sm"
            @click="swapSides"
          >
            ⇄ 交换
          </button>
        </div>
      </div>
    </div>

    <!-- 主体：攻击方 / 防御方 并排 -->
    <div class="grid gap-4 lg:grid-cols-2">
      <!-- 攻击方卡片 -->
      <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm overflow-hidden">
        <div class="bg-gradient-to-r from-blue-500 to-indigo-500 px-4 py-2.5 flex items-center gap-2">
          <span class="text-white text-sm font-bold">⚔️ 攻击方</span>
          <span
            v-if="attackerPokemon"
            class="ml-auto text-white/80 text-xs font-medium"
          >#{{ attackerPokemon.id }} {{ attackerPokemon.name }}</span>
        </div>
        <div class="p-4 space-y-3">
          <!-- 选择宝可梦 -->
          <el-select
            v-model="form.attackerPokemonId"
            filterable
            remote
            reserve-keyword
            default-first-option
            placeholder="搜索宝可梦..."
            class="w-full"
            :loading="pokemonLoading"
            :remote-method="searchPokemonOptions"
            size="large"
            @change="handleAttackerChange"
          >
            <el-option
              v-for="p in pokemonOptions"
              :key="'a-'+p.id"
              :label="pokemonOptionLabel(p)"
              :value="p.id"
            >
              <div class="flex items-center gap-2">
                <img
                  v-if="p.spriteUrl"
                  :src="p.spriteUrl"
                  class="w-8 h-8 object-contain"
                  @error="$event.target.style.display='none'"
                >
                <span class="font-medium">{{ p.name || p.nameEn }}</span>
                <span class="ml-auto text-xs text-slate-400">#{{ p.id }}</span>
              </div>
            </el-option>
          </el-select>

          <!-- 属性标签 -->
          <div
            v-if="attackerTypes.length"
            class="flex gap-1.5"
          >
            <span
              v-for="t in attackerTypes"
              :key="t.type_id"
              class="px-2.5 py-0.5 rounded-full text-[11px] font-bold text-white shadow-sm"
              :style="{ background: typeColor(t.type_id) }"
            >{{ t.name }}</span>
          </div>

          <!-- 招式选择 -->
          <div>
            <label class="block text-[11px] font-bold text-slate-400 mb-1 uppercase tracking-wider">招式</label>
            <el-select
              v-model="form.moveId"
              filterable
              placeholder="先选攻击方"
              class="w-full"
              :loading="moveLoading"
              :disabled="!attackerFormId"
              size="large"
            >
              <el-option
                v-for="m in attackerMoves"
                :key="m.id"
                :label="moveOptionLabel(m)"
                :value="m.id"
              >
                <div class="flex items-center gap-2">
                  <span
                    class="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold text-white"
                    :style="{ background: typeColor(m.typeId) }"
                  >{{ (m.typeName || '?')[0] }}</span>
                  <span class="font-medium">{{ m.name || m.nameEn }}</span>
                  <span class="ml-auto text-xs text-slate-400">威力 {{ m.power ?? '—' }}</span>
                </div>
              </el-option>
            </el-select>
            <!-- 招式信息 -->
            <div
              v-if="selectedMove"
              class="mt-2 flex flex-wrap gap-1.5 text-[11px]"
            >
              <span
                class="px-2 py-0.5 rounded-full font-bold text-white"
                :style="{ background: typeColor(selectedMove.typeId) }"
              >{{ selectedMove.typeName }}</span>
              <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold">{{ selectedMove.damageClassName || '物理' }}</span>
              <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold">威力 {{ selectedMove.power ?? '—' }}</span>
              <span class="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold">命中 {{ selectedMove.accuracy ?? '—' }}</span>
            </div>
          </div>

          <!-- 特性 & 道具 -->
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">特性</label>
              <el-select
                v-model="form.attackerAbilityId"
                filterable
                placeholder="可选"
                clearable
                class="w-full"
                size="default"
              >
                <el-option
                  v-for="a in filteredAttackerAbilities"
                  :key="a.id"
                  :label="a.name"
                  :value="a.id"
                />
              </el-select>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">道具</label>
              <el-select
                v-model="form.attackerItemId"
                filterable
                remote
                :remote-method="searchItems"
                placeholder="可选"
                clearable
                class="w-full"
                :loading="itemLoading"
                size="default"
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

          <!-- 能力等级 -->
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">{{ selectedMove?.damageClassId === 2 ? '特攻' : '攻击' }}阶级</label>
              <el-select
                v-model="form.attackerAttackBoost"
                class="w-full"
                size="default"
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
              <label class="block text-[11px] font-bold text-slate-400 mb-1">HP%</label>
              <el-input-number
                v-model="form.attackerHpPercent"
                :min="1"
                :max="100"
                class="w-full"
                size="default"
              />
            </div>
          </div>

          <!-- 状态 -->
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="s in attackerStatuses"
              :key="s.key"
              class="px-2.5 py-1 rounded-lg text-[11px] font-bold border transition-all"
              :class="form[s.key] ? 'bg-red-50 border-red-300 text-red-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
              @click="form[s.key] = !form[s.key]"
            >
              {{ s.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 防御方卡片 -->
      <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm overflow-hidden">
        <div class="bg-gradient-to-r from-rose-500 to-pink-500 px-4 py-2.5 flex items-center gap-2">
          <span class="text-white text-sm font-bold">🛡️ 防御方</span>
          <span
            v-if="defenderPokemon"
            class="ml-auto text-white/80 text-xs font-medium"
          >#{{ defenderPokemon.id }} {{ defenderPokemon.name }}</span>
        </div>
        <div class="p-4 space-y-3">
          <!-- 选择宝可梦 -->
          <el-select
            v-model="form.defenderPokemonId"
            filterable
            remote
            reserve-keyword
            default-first-option
            placeholder="搜索宝可梦..."
            class="w-full"
            :loading="pokemonLoading"
            :remote-method="searchPokemonOptions"
            size="large"
            @change="handleDefenderChange"
          >
            <el-option
              v-for="p in pokemonOptions"
              :key="'d-'+p.id"
              :label="pokemonOptionLabel(p)"
              :value="p.id"
            >
              <div class="flex items-center gap-2">
                <img
                  v-if="p.spriteUrl"
                  :src="p.spriteUrl"
                  class="w-8 h-8 object-contain"
                  @error="$event.target.style.display='none'"
                >
                <span class="font-medium">{{ p.name || p.nameEn }}</span>
                <span class="ml-auto text-xs text-slate-400">#{{ p.id }}</span>
              </div>
            </el-option>
          </el-select>

          <!-- 属性标签 -->
          <div
            v-if="defenderTypes.length"
            class="flex gap-1.5"
          >
            <span
              v-for="t in defenderTypes"
              :key="t.type_id"
              class="px-2.5 py-0.5 rounded-full text-[11px] font-bold text-white shadow-sm"
              :style="{ background: typeColor(t.type_id) }"
            >{{ t.name }}</span>
          </div>

          <!-- 特性 & 道具 -->
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">特性</label>
              <el-select
                v-model="form.defenderAbilityId"
                filterable
                placeholder="可选"
                clearable
                class="w-full"
                size="default"
              >
                <el-option
                  v-for="a in filteredDefenderAbilities"
                  :key="a.id"
                  :label="a.name"
                  :value="a.id"
                />
              </el-select>
            </div>
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">道具</label>
              <el-select
                v-model="form.defenderItemId"
                filterable
                remote
                :remote-method="searchItems"
                placeholder="可选"
                clearable
                class="w-full"
                :loading="itemLoading"
                size="default"
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

          <!-- 能力等级 -->
          <div class="grid grid-cols-3 gap-2">
            <div>
              <label class="block text-[11px] font-bold text-slate-400 mb-1">防御阶级</label>
              <el-select
                v-model="form.defenderDefenseBoost"
                class="w-full"
                size="default"
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
              <label class="block text-[11px] font-bold text-slate-400 mb-1">特防阶级</label>
              <el-select
                v-model="form.defenderSpDefenseBoost"
                class="w-full"
                size="default"
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
              <label class="block text-[11px] font-bold text-slate-400 mb-1">HP%</label>
              <el-input-number
                v-model="form.defenderHpPercent"
                :min="1"
                :max="100"
                class="w-full"
                size="default"
              />
            </div>
          </div>

          <!-- 状态 -->
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="s in defenderStatuses"
              :key="s.key"
              class="px-2.5 py-1 rounded-lg text-[11px] font-bold border transition-all"
              :class="form[s.key] ? 'bg-red-50 border-red-300 text-red-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
              @click="form[s.key] = !form[s.key]"
            >
              {{ s.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 环境条件 -->
    <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-4">
      <div class="flex items-center gap-2 mb-3">
        <span class="text-sm">🌍</span>
        <span class="text-sm font-bold text-slate-700">环境条件</span>
      </div>
      <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2">
        <div>
          <label class="block text-[10px] font-bold text-slate-400 mb-1">天气</label>
          <el-select
            v-model="form.weather"
            placeholder="无"
            clearable
            size="small"
            class="w-full"
          >
            <el-option
              v-for="w in weathers"
              :key="w.v"
              :label="w.l"
              :value="w.v"
            />
          </el-select>
        </div>
        <div>
          <label class="block text-[10px] font-bold text-slate-400 mb-1">场地</label>
          <el-select
            v-model="form.terrain"
            placeholder="无"
            clearable
            size="small"
            class="w-full"
          >
            <el-option
              v-for="t in terrains"
              :key="t.v"
              :label="t.l"
              :value="t.v"
            />
          </el-select>
        </div>
        <div>
          <label class="block text-[10px] font-bold text-slate-400 mb-1">等级</label>
          <el-input-number
            v-model="form.attackerLevel"
            :min="1"
            :max="100"
            size="small"
            class="w-full"
          />
        </div>
        <div class="flex items-end">
          <button
            class="w-full px-2 py-1.5 rounded-lg text-[11px] font-bold border transition-all"
            :class="form.isCritical ? 'bg-amber-50 border-amber-300 text-amber-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
            @click="form.isCritical = !form.isCritical"
          >
            🎯 暴击
          </button>
        </div>
        <div class="flex items-end">
          <button
            class="w-full px-2 py-1.5 rounded-lg text-[11px] font-bold border transition-all"
            :class="form.isDoubleBattle ? 'bg-blue-50 border-blue-300 text-blue-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
            @click="form.isDoubleBattle = !form.isDoubleBattle"
          >
            👥 双打
          </button>
        </div>
        <div class="flex items-end">
          <button
            class="w-full px-2 py-1.5 rounded-lg text-[11px] font-bold border transition-all"
            :class="form.reflectActive ? 'bg-purple-50 border-purple-300 text-purple-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
            @click="form.reflectActive = !form.reflectActive"
          >
            反射壁
          </button>
        </div>
        <div class="flex items-end">
          <button
            class="w-full px-2 py-1.5 rounded-lg text-[11px] font-bold border transition-all"
            :class="form.lightScreenActive ? 'bg-purple-50 border-purple-300 text-purple-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
            @click="form.lightScreenActive = !form.lightScreenActive"
          >
            光墙
          </button>
        </div>
        <div class="flex items-end">
          <button
            class="w-full px-2 py-1.5 rounded-lg text-[11px] font-bold border transition-all"
            :class="form.auroraVeilActive ? 'bg-purple-50 border-purple-300 text-purple-700' : 'bg-white border-slate-200 text-slate-400 hover:border-slate-300'"
            @click="form.auroraVeilActive = !form.auroraVeilActive"
          >
            极光幕
          </button>
        </div>
      </div>
    </div>

    <!-- 能力值覆盖（高级） -->
    <details class="rounded-2xl bg-white border border-slate-200/80 shadow-sm overflow-hidden group">
      <summary class="px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-slate-50 transition">
        <span class="text-sm">📊</span>
        <span class="text-sm font-bold text-slate-700">能力值覆盖（高级）</span>
        <span class="ml-auto text-xs text-slate-400 group-open:rotate-180 transition-transform">▼</span>
      </summary>
      <div class="px-4 pb-4 pt-1 grid gap-4 lg:grid-cols-2">
        <div class="rounded-xl bg-blue-50/50 border border-blue-100 p-3">
          <div class="text-[11px] font-bold text-blue-500 mb-2">
            攻击方
          </div>
          <div class="grid grid-cols-3 gap-2">
            <div
              v-for="s in [{k:'attackerAtkOv',l:'攻击'},{k:'attackerSpAOv',l:'特攻'},{k:'attackerSpeOv',l:'速度'}]"
              :key="s.k"
            >
              <label class="block text-[10px] text-slate-400 mb-0.5">{{ s.l }}</label>
              <el-input-number
                v-model="form[s.k]"
                :min="0"
                :max="999"
                size="small"
                class="w-full"
                controls-position="right"
              />
            </div>
          </div>
        </div>
        <div class="rounded-xl bg-rose-50/50 border border-rose-100 p-3">
          <div class="text-[11px] font-bold text-rose-500 mb-2">
            防御方
          </div>
          <div class="grid grid-cols-3 gap-2">
            <div
              v-for="s in [{k:'defenderHpOv',l:'HP'},{k:'defenderDefOv',l:'防御'},{k:'defenderSpDOv',l:'特防'}]"
              :key="s.k"
            >
              <label class="block text-[10px] text-slate-400 mb-0.5">{{ s.l }}</label>
              <el-input-number
                v-model="form[s.k]"
                :min="0"
                :max="999"
                size="small"
                class="w-full"
                controls-position="right"
              />
            </div>
          </div>
        </div>
      </div>
    </details>

    <!-- 计算按钮 -->
    <div class="flex items-center justify-center gap-3">
      <button
        :disabled="!canCalculate || calculating"
        class="btn-poke !px-10 !py-3.5 !text-base disabled:!opacity-50 !rounded-2xl !border-2 !border-red-700"
        @click="calculateDamage"
      >
        <span
          v-if="calculating"
          class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"
        />
        {{ calculating ? '计算中...' : '计算伤害' }}
      </button>
    </div>

    <!-- 结果展示 -->
    <div
      v-if="result"
      class="space-y-4"
    >
      <!-- 核心结果卡片 -->
      <div class="rounded-2xl bg-white border border-slate-200/80 shadow-sm overflow-hidden">
        <!-- 属性相性指示条 -->
        <div
          class="h-1.5"
          :style="{ background: effectivenessGradient }"
        />

        <div class="p-5">
          <!-- 伤害数字 -->
          <div class="grid gap-4 sm:grid-cols-3">
            <!-- 最小伤害 -->
            <div class="text-center p-4 rounded-xl bg-slate-50 border border-slate-100">
              <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold">
                最小伤害
              </div>
              <div class="mt-1 text-3xl font-black text-slate-800 tabular-nums">
                {{ result.minDamage }}
              </div>
              <div class="text-xs text-slate-400 mt-0.5">
                {{ minDamagePercent }}%
              </div>
            </div>
            <!-- 平均伤害 -->
            <div
              class="text-center p-4 rounded-xl border-2"
              :class="effectivenessBorderClass"
            >
              <div
                class="text-[10px] uppercase tracking-widest font-bold"
                :class="effectivenessTextClass"
              >
                平均伤害
              </div>
              <div
                class="mt-1 text-4xl font-black tabular-nums"
                :class="effectivenessTextClass"
              >
                {{ formatNumber(result.avgDamage) }}
              </div>
              <div
                class="text-xs mt-0.5 font-semibold"
                :class="effectivenessTextClass"
              >
                {{ avgDamagePercent }}%
              </div>
            </div>
            <!-- 最大伤害 -->
            <div class="text-center p-4 rounded-xl bg-slate-50 border border-slate-100">
              <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold">
                最大伤害
              </div>
              <div class="mt-1 text-3xl font-black text-slate-800 tabular-nums">
                {{ result.maxDamage }}
              </div>
              <div class="text-xs text-slate-400 mt-0.5">
                {{ maxDamagePercent }}%
              </div>
            </div>
          </div>

          <!-- HP 条 -->
          <div class="mt-4">
            <div class="flex items-center justify-between text-xs text-slate-400 mb-1.5">
              <span>防御方 HP</span>
              <span>{{ result.koEstimate?.defenderHp ?? '—' }}</span>
            </div>
            <div class="h-6 bg-slate-100 rounded-full overflow-hidden relative">
              <!-- 最大伤害区域 -->
              <div
                class="absolute inset-y-0 left-0 bg-red-200/60 rounded-full transition-all"
                :style="{ width: maxDamagePercent + '%' }"
              />
              <!-- 最小伤害区域 -->
              <div
                class="absolute inset-y-0 left-0 bg-red-400 rounded-full transition-all"
                :style="{ width: minDamagePercent + '%' }"
              />
              <!-- 文字 -->
              <div class="absolute inset-0 flex items-center justify-center text-xs font-bold text-slate-700">
                {{ minDamagePercent }}% ~ {{ maxDamagePercent }}%
              </div>
            </div>
          </div>

          <!-- KO 判定 -->
          <div
            v-if="result.koEstimate"
            class="mt-4"
          >
            <!-- OHKO 概率大字 -->
            <div class="text-center mb-3">
              <span
                v-if="ohkoChance >= 0.9999"
                class="inline-block px-5 py-2 rounded-xl bg-red-500 text-white text-base font-black shadow-lg shadow-red-500/30"
              >
                💀 确定 OHKO
              </span>
              <span
                v-else-if="ohkoChance > 0"
                class="inline-block px-5 py-2 rounded-xl text-base font-black shadow-lg"
                :class="ohkoChance >= 0.5 ? 'bg-red-100 text-red-700 shadow-red-500/10' : 'bg-amber-100 text-amber-700 shadow-amber-500/10'"
              >
                OHKO {{ formatPercent(ohkoChance) }}
              </span>
              <span
                v-else-if="isGuaranteed2HKO"
                class="inline-block px-5 py-2 rounded-xl bg-emerald-100 text-emerald-700 text-base font-black shadow-lg shadow-emerald-500/10"
              >
                ✅ 确定 2HKO
              </span>
            </div>

            <!-- 各回合 KO 概率条 -->
            <div
              v-if="koChances.length > 0"
              class="space-y-1.5"
            >
              <div
                v-for="kc in koChances"
                :key="kc.label"
                class="flex items-center gap-2 text-xs"
              >
                <span
                  class="w-14 text-right font-bold"
                  :class="kc.prob > 0 ? 'text-slate-700' : 'text-slate-300'"
                >{{ kc.label }}</span>
                <div class="flex-1 h-4 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="kc.prob >= 0.999 ? 'bg-emerald-500' : kc.prob >= 0.5 ? 'bg-blue-500' : kc.prob > 0 ? 'bg-amber-400' : 'bg-transparent'"
                    :style="{ width: (kc.prob * 100) + '%' }"
                  />
                </div>
                <span
                  class="w-12 text-right font-mono font-bold tabular-nums"
                  :class="kc.prob > 0 ? 'text-slate-600' : 'text-slate-300'"
                >
                  {{ formatPercent(kc.prob) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 详细信息网格 -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <!-- 属性相性 -->
        <div class="rounded-xl bg-white border border-slate-200/80 shadow-sm p-4">
          <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold mb-2">
            属性相性
          </div>
          <div class="flex items-center gap-2">
            <span
              class="text-2xl"
              :class="effectivenessEmoji"
            >{{ effectivenessIcon }}</span>
            <div>
              <div
                class="text-lg font-black"
                :class="effectivenessTextClass"
              >
                {{ result.effectivenessDesc || '—' }}
              </div>
              <div class="text-[11px] text-slate-400">
                倍率 {{ formatNumber(result.typeEffectiveness) }}x
              </div>
            </div>
          </div>
        </div>

        <!-- STAB -->
        <div class="rounded-xl bg-white border border-slate-200/80 shadow-sm p-4">
          <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold mb-2">
            本系加成
          </div>
          <div
            class="text-lg font-black"
            :class="result.isStab ? 'text-emerald-600' : 'text-slate-400'"
          >
            {{ result.isStab ? '✓ STAB' : '✗ 无' }}
          </div>
          <div class="text-[11px] text-slate-400">
            {{ formatNumber(result.stabMultiplier) }}x
          </div>
        </div>

        <!-- 使用能力 -->
        <div class="rounded-xl bg-white border border-slate-200/80 shadow-sm p-4">
          <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold mb-2">
            能力对比
          </div>
          <div class="text-sm font-bold text-slate-700">
            {{ result.usedAttackStat ?? '—' }} vs {{ result.usedDefenseStat ?? '—' }}
          </div>
          <div class="text-[11px] text-slate-400">
            {{ result.usedAttackType === 'attack' ? '物攻 vs 物防' : '特攻 vs 特防' }}
          </div>
        </div>

        <!-- 命中率 -->
        <div class="rounded-xl bg-white border border-slate-200/80 shadow-sm p-4">
          <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold mb-2">
            命中率
          </div>
          <div class="text-lg font-black text-slate-700">
            {{ formatPercent(result.finalAccuracy) }}
          </div>
          <div class="text-[11px] text-slate-400">
            基础 {{ result.baseAccuracy ?? '—' }}
          </div>
        </div>
      </div>

      <!-- 修正倍率详情 -->
      <div class="rounded-xl bg-white border border-slate-200/80 shadow-sm p-4">
        <div class="text-[10px] uppercase tracking-widest text-slate-400 font-bold mb-3">
          修正倍率明细
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2">
          <div
            v-for="(v, k) in result.allMultipliers"
            :key="k"
            class="flex flex-col items-center p-2.5 rounded-lg border transition-all"
            :class="v !== 1 ? (v > 1 ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200') : 'bg-slate-50 border-slate-100'"
          >
            <span class="text-[10px] font-bold text-slate-400 uppercase">{{ k }}</span>
            <span
              class="text-sm font-black tabular-nums mt-0.5"
              :class="v !== 1 ? (v > 1 ? 'text-emerald-700' : 'text-red-700') : 'text-slate-500'"
            >
              {{ formatNumber(v) }}x
            </span>
          </div>
        </div>
      </div>

      <!-- 特性/道具效果说明 -->
      <div
        v-if="hasEffects"
        class="space-y-1.5"
      >
        <div
          v-if="result.attackerAbilityEffect"
          class="rounded-lg bg-blue-50 border border-blue-100 px-4 py-2.5 text-xs text-blue-700 flex items-start gap-2"
        >
          <span class="font-bold shrink-0">攻击方特性：</span>{{ result.attackerAbilityEffect }}
        </div>
        <div
          v-if="result.defenderAbilityEffect"
          class="rounded-lg bg-rose-50 border border-rose-100 px-4 py-2.5 text-xs text-rose-700 flex items-start gap-2"
        >
          <span class="font-bold shrink-0">防御方特性：</span>{{ result.defenderAbilityEffect }}
        </div>
        <div
          v-if="result.attackerItemEffect"
          class="rounded-lg bg-amber-50 border border-amber-100 px-4 py-2.5 text-xs text-amber-700 flex items-start gap-2"
        >
          <span class="font-bold shrink-0">攻击方道具：</span>{{ result.attackerItemEffect }}
        </div>
        <div
          v-if="result.defenderItemEffect"
          class="rounded-lg bg-teal-50 border border-teal-100 px-4 py-2.5 text-xs text-teal-700 flex items-start gap-2"
        >
          <span class="font-bold shrink-0">防御方道具：</span>{{ result.defenderItemEffect }}
        </div>
      </div>

      <!-- 计算步骤 -->
      <details
        v-if="result.calculationSteps?.length"
        class="rounded-xl bg-white border border-slate-200/80 shadow-sm overflow-hidden"
      >
        <summary class="px-4 py-3 cursor-pointer flex items-center gap-2 hover:bg-slate-50 transition">
          <span class="text-xs">📝</span>
          <span class="text-xs font-bold text-slate-600">计算步骤</span>
          <span class="ml-auto text-xs text-slate-400">▼</span>
        </summary>
        <div class="px-4 pb-4 space-y-1 max-h-64 overflow-y-auto">
          <div
            v-for="(step, i) in result.calculationSteps"
            :key="i"
            class="flex items-start gap-3 p-2 rounded-lg bg-slate-50 border border-slate-100 text-xs"
          >
            <span class="text-slate-300 font-mono shrink-0">{{ i + 1 }}</span>
            <div class="flex-1">
              <span class="font-bold text-slate-700">{{ step.name }}</span>
              <span
                v-if="step.description"
                class="text-slate-400 ml-2"
              >{{ step.description }}</span>
            </div>
            <span
              v-if="step.value"
              class="font-mono font-bold text-slate-600 shrink-0"
            >{{ formatNumber(step.value) }}</span>
          </div>
        </div>
      </details>
    </div>

    <!-- 空状态 -->
    <div
      v-else
      class="rounded-2xl bg-white border border-slate-200/80 shadow-sm p-12 text-center"
    >
      <div class="text-4xl mb-3">
        ⚔️
      </div>
      <p class="text-sm text-slate-400">
        选择攻击方、招式和防御方后<br>点击「计算伤害」查看结果
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../services/api'

// ── 常量 ──
// PokeAPI / 后端 type 表属性编号（1=normal, 2=fighting, 3=flying ...）
const TYPE_COLORS = {
  1: '#A8A77A', 2: '#C03028', 3: '#A890F0', 4: '#A040A0', 5: '#E0C068',
  6: '#B8A038', 7: '#A8B820', 8: '#705898', 9: '#B8B8D0', 10: '#F08030',
  11: '#6890F0', 12: '#78C850', 13: '#F8D030', 14: '#F85888', 15: '#98D8D8',
  16: '#7038F8', 17: '#705848', 18: '#EE99AC'
}

const weathers = [
  { v: 'sun', l: '☀ 晴天' }, { v: 'rain', l: '🌧 下雨' },
  { v: 'sand', l: '🌪 沙暴' }, { v: 'snow', l: '❄ 雪天' }
]
const terrains = [
  { v: 'electric', l: '⚡ 电气' }, { v: 'psychic', l: '🔮 精神' },
  { v: 'grassy', l: '🌿 青草' }, { v: 'misty', l: '🌫 薄雾' }
]
const boostOptions = [-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6]

const attackerStatuses = [
  { key: 'attackerBurned', label: '🔥 烧伤' },
  { key: 'attackerPoisoned', label: '☣ 中毒' },
  { key: 'attackerParalyzed', label: '⚡ 麻痹' }
]
const defenderStatuses = [
  { key: 'defenderBurned', label: '🔥 烧伤' },
  { key: 'defenderPoisoned', label: '☣ 中毒' },
  { key: 'defenderParalyzed', label: '⚡ 麻痹' },
  { key: 'defenderAsleep', label: '💤 睡眠' },
  { key: 'defenderFrozen', label: '🧊 冰冻' }
]

// ── 表单 ──
const DEFAULT_FORM = () => ({
  attackerPokemonId: null, defenderPokemonId: null, moveId: null,
  attackerLevel: 50, weather: '', terrain: '',
  isCritical: false, isDoubleBattle: false,
  attackerBurned: false, attackerPoisoned: false, attackerParalyzed: false,
  defenderBurned: false, defenderPoisoned: false, defenderParalyzed: false,
  defenderAsleep: false, defenderFrozen: false,
  reflectActive: false, lightScreenActive: false, auroraVeilActive: false,
  attackerAbilityId: null, defenderAbilityId: null,
  attackerItemId: null, defenderItemId: null,
  attackerAttackBoost: 0, defenderDefenseBoost: 0, defenderSpDefenseBoost: 0,
  attackerHpPercent: 100, defenderHpPercent: 100,
  attackerAtkOv: null, attackerSpAOv: null, attackerSpeOv: null,
  defenderHpOv: null, defenderDefOv: null, defenderSpDOv: null
})

const form = reactive(DEFAULT_FORM())

// 表单状态持久化：刷新页面后恢复上次的配置
const CALC_STORAGE_KEY = 'pokemon-factory-calc-form'
function restoreForm() {
  try {
    const saved = localStorage.getItem(CALC_STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      Object.assign(form, DEFAULT_FORM(), parsed)
    }
  } catch { /* ignore */ }
}
restoreForm()
function persistForm() {
  try {
    localStorage.setItem(CALC_STORAGE_KEY, JSON.stringify(form))
  } catch { /* ignore */ }
}
// 防抖持久化
let persistTimer = null
watch(form, () => {
  if (persistTimer) clearTimeout(persistTimer)
  persistTimer = setTimeout(persistForm, 500)
}, { deep: true })

const pokemonOptions = ref([])
const attackerMoves = ref([])
const abilityOptions = ref([])
const attackerAbilityIds = ref([])
const defenderAbilityIds = ref([])
const itemOptions = ref([])
const result = ref(null)
const pokemonLoading = ref(false)
const moveLoading = ref(false)
const itemLoading = ref(false)
const calculating = ref(false)
const attackerTypes = ref([])
const defenderTypes = ref([])
let latestPokemonSearchToken = 0

// ── 计算属性 ──
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

const minDamagePercent = computed(() => {
  if (!result.value?.koEstimate?.defenderHp) return '0'
  return ((result.value.minDamage / result.value.koEstimate.defenderHp) * 100).toFixed(1)
})
const avgDamagePercent = computed(() => {
  if (!result.value?.koEstimate?.defenderHp) return '0'
  return ((result.value.avgDamage / result.value.koEstimate.defenderHp) * 100).toFixed(1)
})
const maxDamagePercent = computed(() => {
  if (!result.value?.koEstimate?.defenderHp) return '0'
  return Math.min(100, (result.value.maxDamage / result.value.koEstimate.defenderHp) * 100).toFixed(1)
})
const ohkoChance = computed(() => result.value?.koEstimate?.koChance ?? 0)
const isGuaranteed2HKO = computed(() => {
  const chances = result.value?.debugInfo?.koChances
  if (!chances) return false
  return (chances['2hko'] ?? 0) >= 0.999
})
const koChances = computed(() => {
  const chances = result.value?.debugInfo?.koChances
  if (!chances) return []
  return Object.entries(chances).map(([key, prob]) => ({
    label: key.replace('hko', 'HKO').toUpperCase(),
    prob: prob ?? 0
  }))
})

const effectivenessTextClass = computed(() => {
  const eff = result.value?.typeEffectiveness
  if (!eff || eff === 1) return 'text-slate-600'
  if (eff > 1) return 'text-emerald-600'
  if (eff === 0) return 'text-slate-400'
  return 'text-red-600'
})
const effectivenessBorderClass = computed(() => {
  const eff = result.value?.typeEffectiveness
  if (!eff || eff === 1) return 'border-slate-200'
  if (eff > 1) return 'border-emerald-300'
  if (eff === 0) return 'border-slate-200'
  return 'border-red-300'
})
const effectivenessGradient = computed(() => {
  const eff = result.value?.typeEffectiveness
  if (!eff || eff === 1) return 'linear-gradient(90deg, #94a3b8, #94a3b8)'
  if (eff > 1) return 'linear-gradient(90deg, #10b981, #34d399)'
  if (eff === 0) return 'linear-gradient(90deg, #64748b, #94a3b8)'
  return 'linear-gradient(90deg, #ef4444, #f87171)'
})
const effectivenessIcon = computed(() => {
  const eff = result.value?.typeEffectiveness
  if (!eff || eff === 1) return '➡️'
  if (eff >= 2) return '💥'
  if (eff === 0) return '❌'
  return '🛡️'
})
const effectivenessEmoji = computed(() => {
  const eff = result.value?.typeEffectiveness
  if (!eff || eff === 1) return ''
  if (eff >= 2) return 'text-2xl'
  if (eff === 0) return 'text-2xl grayscale'
  return 'text-2xl'
})
const hasEffects = computed(() =>
  result.value && (result.value.attackerAbilityEffect || result.value.defenderAbilityEffect || result.value.attackerItemEffect || result.value.defenderItemEffect)
)

// ── 方法 ──
function typeColor(typeId) { return TYPE_COLORS[typeId] || '#777' }
function boostLabel(i) { return i > 0 ? `+${i}` : `${i}` }
function pokemonOptionLabel(p) { return `${p.name || p.nameEn || '#' + p.id}` }
function moveOptionLabel(m) { return `${m.name || m.nameEn || '#' + m.id} · ${m.typeName || '?'} · 威力${m.power ?? '—'}` }
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
    const records = res.data?.records || []
    // 添加精灵图URL
    for (const p of records) {
      if (p.id && !p.spriteUrl) {
        p.spriteUrl = `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${p.id}.png`
      }
    }
    mergePokemonOptions(records)
  } catch (e) { ElMessage.error(e?.message || '加载失败') }
  finally { if (token === latestPokemonSearchToken) pokemonLoading.value = false }
}

async function searchAbilities() {
  try {
    const res = await api.abilities.getList({ current: 1, size: 200 })
    abilityOptions.value = res.data?.records || []
  } catch { /* ignore */ }
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

async function fetchPokemonDetail(pokemonId, isAttacker) {
  try {
    const res = await api.pokemon.getDetail(pokemonId)
    if (res.code === 200) {
      const forms = res.data?.forms || []
      const types = forms[0]?.types || []
      if (isAttacker) attackerTypes.value = types
      else defenderTypes.value = types

      const allAb = []
      for (const f of forms)
        if (f.abilities) for (const a of f.abilities)
          if (!allAb.find(x => x.id === a.id)) allAb.push(a)
      if (isAttacker) attackerAbilityIds.value = allAb.map(a => a.id)
      else defenderAbilityIds.value = allAb.map(a => a.id)
    }
  } catch { /* ignore */ }
}

async function handleAttackerChange() {
  result.value = null; attackerAbilityIds.value = []; form.attackerAbilityId = null; attackerTypes.value = []
  if (form.attackerPokemonId) await fetchPokemonDetail(form.attackerPokemonId, true)
  await loadAttackerMoves()
}

function handleDefenderChange() {
  result.value = null; defenderAbilityIds.value = []; form.defenderAbilityId = null; defenderTypes.value = []
  if (form.defenderPokemonId) fetchPokemonDetail(form.defenderPokemonId, false)
}

async function swapSides() {
  if (!form.attackerPokemonId || !form.defenderPokemonId) return
  const tmp = form.attackerPokemonId; form.attackerPokemonId = form.defenderPokemonId; form.defenderPokemonId = tmp
  result.value = null
  await handleAttackerChange()
  handleDefenderChange()
}

async function calculateDamage() {
  if (!canCalculate.value) { ElMessage.warning('请先完整选择'); return }
  try {
    calculating.value = true
    const move = selectedMove.value
    const isSpAtk = move?.damageClassId === 2
    const res = await api.damage.calculate({
      attackerFormId: attackerFormId.value, defenderFormId: defenderFormId.value,
      moveId: form.moveId, attackerLevel: form.attackerLevel,
      weather: form.weather || null, terrain: form.terrain || null,
      isCritical: form.isCritical, isDoubleBattle: form.isDoubleBattle,
      attackerBurned: form.attackerBurned, attackerPoisoned: form.attackerPoisoned, attackerParalyzed: form.attackerParalyzed,
      defenderBurned: form.defenderBurned, defenderPoisoned: form.defenderPoisoned, defenderParalyzed: form.defenderParalyzed,
      defenderAsleep: form.defenderAsleep, defenderFrozen: form.defenderFrozen,
      reflectActive: form.reflectActive, lightScreenActive: form.lightScreenActive, auroraVeilActive: form.auroraVeilActive,
      attackerAbilityId: form.attackerAbilityId, defenderAbilityId: form.defenderAbilityId,
      attackerItemId: form.attackerItemId, defenderItemId: form.defenderItemId,
      attackerAttackBoost: isSpAtk ? 0 : form.attackerAttackBoost,
      attackerSpAttackBoost: isSpAtk ? form.attackerAttackBoost : 0,
      defenderDefenseBoost: form.defenderDefenseBoost,
      defenderSpDefenseBoost: form.defenderSpDefenseBoost,
      attackerHpPercent: form.attackerHpPercent, defenderHpPercent: form.defenderHpPercent,
      attackerAttack: form.attackerAtkOv || undefined,
      attackerSpAttack: form.attackerSpAOv || undefined,
      attackerSpeed: form.attackerSpeOv || undefined,
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
  attackerTypes.value = []; defenderTypes.value = []
}

onMounted(async () => {
  await searchPokemonOptions()
  // 若已恢复持久化选择，则按选择加载详情；否则使用默认前两只
  if (form.attackerPokemonId && pokemonOptions.value.some(p => p.id === form.attackerPokemonId)) {
    await fetchPokemonDetail(form.attackerPokemonId, true)
    await loadAttackerMoves()
  } else if (pokemonOptions.value[0]) {
    form.attackerPokemonId = pokemonOptions.value[0].id
    await fetchPokemonDetail(pokemonOptions.value[0].id, true)
    await loadAttackerMoves()
  }
  if (form.defenderPokemonId && pokemonOptions.value.some(p => p.id === form.defenderPokemonId)) {
    fetchPokemonDetail(form.defenderPokemonId, false)
  } else if (pokemonOptions.value[1]) {
    form.defenderPokemonId = pokemonOptions.value[1].id
    fetchPokemonDetail(pokemonOptions.value[1].id, false)
  } else if (pokemonOptions.value[0]) {
    form.defenderPokemonId = pokemonOptions.value[0].id
    fetchPokemonDetail(pokemonOptions.value[0].id, false)
  }
  searchAbilities()
  searchItems()
})
</script>
