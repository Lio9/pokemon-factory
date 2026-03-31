<template>
  <div class="damage-calculator">
    <div class="calculator-header">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">伤害计算器</h1>
      <p class="text-gray-600">基于宝可梦标准伤害公式，支持特性、道具、天气、能力等级等完整计算</p>
    </div>

    <div class="calculator-grid">
      <!-- 攻击方配置 -->
      <div class="attacker-section card-section">
        <h2 class="section-title"><span class="icon">⚔️</span> 攻击方</h2>
        
        <div class="form-group">
          <label class="form-label">选择宝可梦</label>
          <el-select 
            v-model="attacker.speciesId" 
            placeholder="搜索宝可梦..."
            filterable
            class="w-full"
            @change="onAttackerSpeciesChange"
          >
            <el-option
              v-for="pokemon in pokemonList"
              :key="pokemon.id"
              :label="`${pokemon.name} (#${pokemon.id})`"
              :value="pokemon.id"
            >
              <div class="flex items-center gap-2">
                <img :src="getSpriteUrl(pokemon.id)" class="w-6 h-6" @error="handleImageError">
                <span>{{ pokemon.name }}</span>
                <span class="text-gray-400 text-sm">#{{ pokemon.id }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 形态选择 -->
        <div v-if="attackerForms.length > 1" class="form-group">
          <label class="form-label">选择形态</label>
          <el-select v-model="attacker.formId" class="w-full" @change="onAttackerFormChange">
            <el-option v-for="form in attackerForms" :key="form.id" 
              :label="form.formName || '默认形态'" :value="form.id" />
          </el-select>
        </div>

        <!-- 攻击方信息卡片 -->
        <div v-if="attackerForm" class="pokemon-card attacker-card">
          <div class="pokemon-header">
            <img :src="attackerForm.spriteUrl || getSpriteUrl(attacker.speciesId)" 
              class="pokemon-sprite" @error="handleImageError">
            <div class="pokemon-info">
              <h3 class="pokemon-name">{{ attackerPokemon?.name }}</h3>
              <div class="pokemon-types">
                <span v-for="type in attackerForm.types" :key="type.id"
                  class="type-badge" :style="{ backgroundColor: type.color + '20', borderColor: type.color, color: type.color }">
                  {{ type.name }}
                </span>
              </div>
            </div>
          </div>

          <!-- 特性选择 -->
          <div class="ability-section" v-if="attackerForm.abilities?.length">
            <label class="sub-label">特性</label>
            <el-select v-model="attacker.abilityId" placeholder="选择特性" class="w-full" size="small">
              <el-option v-for="ability in attackerForm.abilities" :key="ability.id"
                :label="ability.name + (ability.isHidden ? ' (隐藏)' : '')" :value="ability.id" />
            </el-select>
          </div>
          
          <!-- 道具选择 -->
          <div class="item-section">
            <label class="sub-label">道具</label>
            <el-select v-model="attacker.itemId" placeholder="无道具" clearable filterable class="w-full" size="small">
              <el-option label="无道具" :value="null" />
              <el-option-group label="伤害提升道具">
                <el-option v-for="item in battleItems.damage" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
              <el-option-group label="属性强化道具">
                <el-option v-for="item in battleItems.typeBoost" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
              <el-option-group label="其他道具">
                <el-option v-for="item in battleItems.other" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
            </el-select>
          </div>
        </div>

        <!-- 能力值设置 -->
        <div v-if="attackerForm" class="stats-section">
          <h4 class="stats-title">能力值设置</h4>
          
          <div class="stat-row">
            <span class="stat-label">等级</span>
            <el-input-number v-model="attacker.level" :min="1" :max="100" size="small" />
          </div>
          
          <div class="stat-row">
            <span class="stat-label">攻击</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="attacker.attackBoost = Math.min(6, (attacker.attackBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(attacker.attackBoost)">
                  {{ formatBoost(attacker.attackBoost) }}
                </span>
                <button class="boost-btn" @click="attacker.attackBoost = Math.max(-6, (attacker.attackBoost || 0) - 1)">-</button>
              </div>
              <el-input-number v-model="attacker.attack" :min="1" size="small" class="stat-input" />
              <span class="stat-base">种族: {{ attackerForm.stats?.attack || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">特攻</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="attacker.spAttackBoost = Math.min(6, (attacker.spAttackBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(attacker.spAttackBoost)">
                  {{ formatBoost(attacker.spAttackBoost) }}
                </span>
                <button class="boost-btn" @click="attacker.spAttackBoost = Math.max(-6, (attacker.spAttackBoost || 0) - 1)">-</button>
              </div>
              <el-input-number v-model="attacker.spAttack" :min="1" size="small" class="stat-input" />
              <span class="stat-base">种族: {{ attackerForm.stats?.spAttack || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">速度</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="attacker.speedBoost = Math.min(6, (attacker.speedBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(attacker.speedBoost)">
                  {{ formatBoost(attacker.speedBoost) }}
                </span>
                <button class="boost-btn" @click="attacker.speedBoost = Math.max(-6, (attacker.speedBoost || 0) - 1)">-</button>
              </div>
              <span class="stat-base">种族: {{ attackerForm.stats?.speed || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">命中</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="attacker.accuracyBoost = Math.min(6, (attacker.accuracyBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(attacker.accuracyBoost)">
                  {{ formatBoost(attacker.accuracyBoost) }}
                </span>
                <button class="boost-btn" @click="attacker.accuracyBoost = Math.max(-6, (attacker.accuracyBoost || 0) - 1)">-</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 状态异常 -->
        <div v-if="attackerForm" class="status-section">
          <h4 class="stats-title">状态异常</h4>
          <div class="status-toggles">
            <label class="status-toggle" :class="{ active: attacker.burned }">
              <input type="checkbox" v-model="attacker.burned">
              <span class="status-icon">🔥</span>
              <span>灼伤</span>
            </label>
            <label class="status-toggle" :class="{ active: attacker.poisoned }">
              <input type="checkbox" v-model="attacker.poisoned">
              <span class="status-icon">☠️</span>
              <span>中毒</span>
            </label>
            <label class="status-toggle" :class="{ active: attacker.paralyzed }">
              <input type="checkbox" v-model="attacker.paralyzed">
              <span class="status-icon">⚡</span>
              <span>麻痹</span>
            </label>
          </div>
        </div>

        <!-- 技能选择 -->
        <div v-if="attackerForm" class="form-group">
          <label class="form-label">选择技能</label>
          <el-select v-model="attacker.moveId" placeholder="搜索技能..." filterable class="w-full" @change="onMoveChange">
            <el-option v-for="move in attackerMoves" :key="move.id" :label="move.name" :value="move.id">
              <div class="flex items-center justify-between w-full">
                <span>{{ move.name }}</span>
                <div class="flex items-center gap-2">
                  <span class="type-badge-mini" :style="{ backgroundColor: move.typeColor + '20', color: move.typeColor }">
                    {{ move.typeName }}
                  </span>
                  <span class="text-gray-400">{{ move.power || '-' }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 技能信息 -->
        <div v-if="selectedMove" class="move-card">
          <div class="move-header">
            <h4>{{ selectedMove.name }}</h4>
            <div class="move-badges">
              <span class="damage-class" :class="selectedMove.damageClass">{{ selectedMove.damageClass }}</span>
              <span v-if="selectedMove.priority !== 0" class="priority-badge" :class="{ positive: selectedMove.priority > 0, negative: selectedMove.priority < 0 }">
                优先度 {{ selectedMove.priority > 0 ? '+' : '' }}{{ selectedMove.priority }}
              </span>
              <span v-if="selectedMove.isContact" class="contact-badge">接触</span>
              <span v-if="selectedMove.multiHit > 1" class="multi-hit-badge">{{ selectedMove.multiHit }}次攻击</span>
            </div>
          </div>
          <div class="move-details">
            <div class="detail-item">
              <span class="label">属性</span>
              <span class="type-badge-mini" :style="{ backgroundColor: selectedMove.typeColor + '20', color: selectedMove.typeColor }">
                {{ selectedMove.typeName }}
              </span>
            </div>
            <div class="detail-item">
              <span class="label">威力</span>
              <span class="value">{{ selectedMove.power || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">命中</span>
              <span class="value">{{ selectedMove.accuracy || '-' }}%</span>
            </div>
            <div class="detail-item">
              <span class="label">PP</span>
              <span class="value">{{ selectedMove.pp || '-' }}</span>
            </div>
          </div>
          <div v-if="selectedMove.description" class="move-description">
            {{ selectedMove.description }}
          </div>
        </div>
      </div>

      <!-- 防御方配置 -->
      <div class="defender-section card-section">
        <h2 class="section-title"><span class="icon">🛡️</span> 防御方</h2>
        
        <div class="form-group">
          <label class="form-label">选择宝可梦</label>
          <el-select v-model="defender.speciesId" placeholder="搜索宝可梦..." filterable class="w-full" @change="onDefenderSpeciesChange">
            <el-option v-for="pokemon in pokemonList" :key="pokemon.id"
              :label="`${pokemon.name} (#${pokemon.id})`" :value="pokemon.id">
              <div class="flex items-center gap-2">
                <img :src="getSpriteUrl(pokemon.id)" class="w-6 h-6" @error="handleImageError">
                <span>{{ pokemon.name }}</span>
                <span class="text-gray-400 text-sm">#{{ pokemon.id }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 形态选择 -->
        <div v-if="defenderForms.length > 1" class="form-group">
          <label class="form-label">选择形态</label>
          <el-select v-model="defender.formId" class="w-full" @change="onDefenderFormChange">
            <el-option v-for="form in defenderForms" :key="form.id" 
              :label="form.formName || '默认形态'" :value="form.id" />
          </el-select>
        </div>

        <!-- 防御方信息卡片 -->
        <div v-if="defenderForm" class="pokemon-card defender-card">
          <div class="pokemon-header">
            <img :src="defenderForm.spriteUrl || getSpriteUrl(defender.speciesId)" 
              class="pokemon-sprite" @error="handleImageError">
            <div class="pokemon-info">
              <h3 class="pokemon-name">{{ defenderPokemon?.name }}</h3>
              <div class="pokemon-types">
                <span v-for="type in defenderForm.types" :key="type.id"
                  class="type-badge" :style="{ backgroundColor: type.color + '20', borderColor: type.color, color: type.color }">
                  {{ type.name }}
                </span>
              </div>
            </div>
          </div>

          <!-- 特性选择 -->
          <div class="ability-section" v-if="defenderForm.abilities?.length">
            <label class="sub-label">特性</label>
            <el-select v-model="defender.abilityId" placeholder="选择特性" class="w-full" size="small">
              <el-option v-for="ability in defenderForm.abilities" :key="ability.id"
                :label="ability.name + (ability.isHidden ? ' (隐藏)' : '')" :value="ability.id" />
            </el-select>
          </div>
          
          <!-- 道具选择 -->
          <div class="item-section">
            <label class="sub-label">道具</label>
            <el-select v-model="defender.itemId" placeholder="无道具" clearable filterable class="w-full" size="small">
              <el-option label="无道具" :value="null" />
              <el-option-group label="防御道具">
                <el-option v-for="item in battleItems.defense" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
              <el-option-group label="减伤树果">
                <el-option v-for="item in battleItems.berries" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
              <el-option-group label="其他道具">
                <el-option v-for="item in battleItems.other" :key="item.id"
                  :label="item.name" :value="item.id" />
              </el-option-group>
            </el-select>
          </div>
        </div>

        <!-- 能力值设置 -->
        <div v-if="defenderForm" class="stats-section">
          <h4 class="stats-title">能力值设置</h4>
          
          <div class="stat-row">
            <span class="stat-label">HP</span>
            <div class="stat-controls">
              <el-input-number v-model="defender.hp" :min="1" size="small" class="stat-input" />
              <span class="stat-base">种族: {{ defenderForm.stats?.hp || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">防御</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="defender.defenseBoost = Math.min(6, (defender.defenseBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(defender.defenseBoost)">
                  {{ formatBoost(defender.defenseBoost) }}
                </span>
                <button class="boost-btn" @click="defender.defenseBoost = Math.max(-6, (defender.defenseBoost || 0) - 1)">-</button>
              </div>
              <el-input-number v-model="defender.defense" :min="1" size="small" class="stat-input" />
              <span class="stat-base">种族: {{ defenderForm.stats?.defense || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">特防</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="defender.spDefenseBoost = Math.min(6, (defender.spDefenseBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(defender.spDefenseBoost)">
                  {{ formatBoost(defender.spDefenseBoost) }}
                </span>
                <button class="boost-btn" @click="defender.spDefenseBoost = Math.max(-6, (defender.spDefenseBoost || 0) - 1)">-</button>
              </div>
              <el-input-number v-model="defender.spDefense" :min="1" size="small" class="stat-input" />
              <span class="stat-base">种族: {{ defenderForm.stats?.spDefense || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">速度</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="defender.speedBoost = Math.min(6, (defender.speedBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(defender.speedBoost)">
                  {{ formatBoost(defender.speedBoost) }}
                </span>
                <button class="boost-btn" @click="defender.speedBoost = Math.max(-6, (defender.speedBoost || 0) - 1)">-</button>
              </div>
              <span class="stat-base">种族: {{ defenderForm.stats?.speed || '-' }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <span class="stat-label">闪避</span>
            <div class="stat-controls">
              <div class="boost-control">
                <button class="boost-btn" @click="defender.evasionBoost = Math.min(6, (defender.evasionBoost || 0) + 1)">+</button>
                <span class="boost-value" :class="getBoostClass(defender.evasionBoost)">
                  {{ formatBoost(defender.evasionBoost) }}
                </span>
                <button class="boost-btn" @click="defender.evasionBoost = Math.max(-6, (defender.evasionBoost || 0) - 1)">-</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 战斗设置 -->
      <div class="battle-settings card-section">
        <h2 class="section-title"><span class="icon">⚙️</span> 战斗设置</h2>
        
        <div class="settings-grid">
          <!-- 天气 -->
          <div class="setting-item">
            <label class="setting-label">天气</label>
            <el-select v-model="battleSettings.weather" placeholder="无天气" clearable size="small">
              <el-option label="无" value="" />
              <el-option label="☀️ 晴天" value="晴天" />
              <el-option label="🌧️ 雨天" value="雨天" />
              <el-option label="🏜️ 沙暴" value="沙暴" />
              <el-option label="🌨️ 冰雹" value="冰雹" />
            </el-select>
          </div>
          
          <!-- 场地 -->
          <div class="setting-item">
            <label class="setting-label">场地</label>
            <el-select v-model="battleSettings.terrain" placeholder="无场地" clearable size="small">
              <el-option label="无" value="" />
              <el-option label="⚡ 电气场地" value="电气场地" />
              <el-option label="🌿 草地场地" value="草地场地" />
              <el-option label="🔮 超能力场地" value="超能力场地" />
              <el-option label="🌫️ 薄雾场地" value="薄雾场地" />
            </el-select>
          </div>
          
          <!-- 战斗模式 -->
          <div class="setting-item">
            <label class="setting-label">双打模式</label>
            <el-switch v-model="battleSettings.isDoubleBattle" />
          </div>
          
          <!-- 暴击 -->
          <div class="setting-item">
            <label class="setting-label">暴击</label>
            <el-switch v-model="battleSettings.isCritical" />
          </div>
        </div>

        <!-- 屏幕效果 -->
        <div class="screen-settings">
          <h4 class="sub-title">屏幕效果</h4>
          <div class="screen-toggles">
            <label class="screen-toggle" :class="{ active: battleSettings.reflectActive }">
              <input type="checkbox" v-model="battleSettings.reflectActive">
              <span>反射壁</span>
            </label>
            <label class="screen-toggle" :class="{ active: battleSettings.lightScreenActive }">
              <input type="checkbox" v-model="battleSettings.lightScreenActive">
              <span>光墙</span>
            </label>
            <label class="screen-toggle" :class="{ active: battleSettings.auroraVeilActive }">
              <input type="checkbox" v-model="battleSettings.auroraVeilActive">
              <span>极光幕</span>
            </label>
          </div>
        </div>

        <!-- 计算按钮 -->
        <button class="calculate-btn" :disabled="!canCalculate || calculating" @click="calculateDamage">
          <span v-if="calculating" class="loading-spinner"></span>
          <span v-else>计算伤害</span>
        </button>
      </div>
    </div>

    <!-- 计算结果 -->
    <div v-if="result" class="result-section">
      <h2 class="section-title"><span class="icon">📊</span> 计算结果</h2>
      
      <div class="result-grid">
        <!-- 伤害范围 -->
        <div class="result-card damage-result">
          <h3>伤害范围</h3>
          <div class="damage-display">
            <div class="damage-value min">
              <span class="label">最小</span>
              <span class="value">{{ result.minDamage }}</span>
            </div>
            <div class="damage-value avg">
              <span class="label">平均</span>
              <span class="value">{{ Math.round(result.avgDamage) }}</span>
            </div>
            <div class="damage-value max">
              <span class="label">最大</span>
              <span class="value">{{ result.maxDamage }}</span>
            </div>
          </div>
          <div v-if="result.koEstimate" class="ko-percent">
            {{ result.koEstimate.koPercentRange }}
          </div>
        </div>

        <!-- 属性相性 -->
        <div class="result-card effectiveness-result">
          <h3>属性相性</h3>
          <div class="effectiveness-display" :class="getEffectivenessClass(result.typeEffectiveness)">
            <span class="multiplier">{{ result.typeEffectiveness }}x</span>
            <span class="desc">{{ result.effectivenessDesc }}</span>
          </div>
        </div>

        <!-- 本系加成 -->
        <div class="result-card stab-result">
          <h3>本系加成</h3>
          <div class="stab-display" :class="{ active: result.isStab }">
            <span class="status">{{ result.isStab ? '✓' : '✗' }}</span>
            <span class="multiplier">{{ result.stabMultiplier }}x</span>
          </div>
        </div>

        <!-- 击杀预估 -->
        <div v-if="result.koEstimate" class="result-card ko-result">
          <h3>击杀预估</h3>
          <div class="ko-display">
            <div class="ko-hp">
              <span class="label">防御方HP</span>
              <span class="value">{{ result.koEstimate.defenderHp }}</span>
            </div>
            <div class="ko-hits">
              <span class="label">攻击次数</span>
              <span class="value">{{ result.koEstimate.minHits }} ~ {{ result.koEstimate.maxHits }}次</span>
            </div>
          </div>
        </div>
        
        <!-- 命中率 -->
        <div v-if="result.accuracy !== null" class="result-card accuracy-result">
          <h3>命中率</h3>
          <div class="accuracy-display">
            <span class="value" :class="{ high: result.accuracy >= 90, medium: result.accuracy >= 70, low: result.accuracy < 70 }">
              {{ result.accuracy }}%
            </span>
            <span class="label" v-if="result.baseAccuracy">基础: {{ result.baseAccuracy }}%</span>
          </div>
        </div>
      </div>

      <!-- 修正因子汇总 -->
      <div v-if="result.allMultipliers" class="multipliers-summary">
        <h3>修正因子汇总</h3>
        <div class="multipliers-grid">
          <div v-for="(value, key) in result.allMultipliers" :key="key" class="multiplier-item" 
            :class="{ highlight: value !== 1.0 }">
            <span class="name">{{ key }}</span>
            <span class="value">{{ value }}x</span>
          </div>
        </div>
      </div>

      <!-- 特性/道具效果 -->
      <div v-if="result.attackerAbilityEffect || result.defenderAbilityEffect || 
                result.attackerItemEffect || result.defenderItemEffect" 
        class="effects-section">
        <h3>特殊效果</h3>
        <div class="effects-list">
          <div v-if="result.attackerAbilityEffect" class="effect-item attacker">
            <span class="tag">攻击方特性</span>
            {{ result.attackerAbilityEffect }}
          </div>
          <div v-if="result.defenderAbilityEffect" class="effect-item defender">
            <span class="tag">防御方特性</span>
            {{ result.defenderAbilityEffect }}
          </div>
          <div v-if="result.attackerItemEffect" class="effect-item attacker">
            <span class="tag">攻击方道具</span>
            {{ result.attackerItemEffect }}
          </div>
          <div v-if="result.defenderItemEffect" class="effect-item defender">
            <span class="tag">防御方道具</span>
            {{ result.defenderItemEffect }}
          </div>
        </div>
      </div>

      <!-- 计算详情 -->
      <div class="calculation-details">
        <h3>计算过程</h3>
        <div class="steps-list">
          <div v-for="(step, index) in result.calculationSteps" :key="index" 
            class="step-item" :class="step.category">
            <span class="step-category">{{ getCategoryLabel(step.category) }}</span>
            <span class="step-name">{{ step.name }}</span>
            <span v-if="step.formula" class="step-formula">{{ step.formula }}</span>
            <span class="step-value">{{ step.value }}</span>
            <span v-if="step.description" class="step-desc">{{ step.description }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { ElSelect, ElOption, ElOptionGroup, ElSwitch, ElInputNumber } from 'element-plus'
import { pokemonApi, damageApi, itemApi, sprites } from '../services/api.js'

export default {
  name: 'DamageCalculator',
  components: { ElSelect, ElOption, ElOptionGroup, ElSwitch, ElInputNumber },
  setup() {
    const pokemonList = ref([])
    const loading = ref(false)
    const calculating = ref(false)
    
    // 道具列表（按类型分组）
    const battleItems = ref({
      damage: [],      // 伤害提升道具
      typeBoost: [],   // 属性强化道具
      defense: [],     // 防御道具
      berries: [],     // 减伤树果
      other: []        // 其他道具
    })

    // 攻击方数据
    const attacker = ref({
      speciesId: null,
      formId: null,
      moveId: null,
      abilityId: null,
      itemId: null,
      level: 50,
      attack: null,
      spAttack: null,
      speedBoost: 0,
      accuracyBoost: 0,
      attackBoost: 0,
      spAttackBoost: 0,
      burned: false,
      poisoned: false,
      paralyzed: false
    })
    const attackerDetail = ref(null)
    const attackerForms = ref([])
    const attackerMoves = ref([])

    // 防御方数据
    const defender = ref({
      speciesId: null,
      formId: null,
      abilityId: null,
      itemId: null,
      hp: null,
      defense: null,
      spDefense: null,
      speedBoost: 0,
      evasionBoost: 0,
      defenseBoost: 0,
      spDefenseBoost: 0,
      burned: false,
      poisoned: false,
      paralyzed: false
    })
    const defenderDetail = ref(null)
    const defenderForms = ref([])

    // 战斗设置
    const battleSettings = ref({
      weather: '',
      terrain: '',
      isDoubleBattle: false,
      isCritical: false,
      reflectActive: false,
      lightScreenActive: false,
      auroraVeilActive: false
    })

    // 计算结果
    const result = ref(null)

    // 计算属性
    const attackerPokemon = computed(() => pokemonList.value.find(p => p.id === attacker.value.speciesId))
    const defenderPokemon = computed(() => pokemonList.value.find(p => p.id === defender.value.speciesId))
    const attackerForm = computed(() => {
      if (!attackerDetail.value?.forms) return null
      return attackerDetail.value.forms.find(f => f.id === attacker.value.formId) || attackerDetail.value.forms[0]
    })
    const defenderForm = computed(() => {
      if (!defenderDetail.value?.forms) return null
      return defenderDetail.value.forms.find(f => f.id === defender.value.formId) || defenderDetail.value.forms[0]
    })
    const selectedMove = computed(() => attackerMoves.value.find(m => m.id === attacker.value.moveId))
    const canCalculate = computed(() => attacker.value.formId && defender.value.formId && attacker.value.moveId)

    // 方法
    const getSpriteUrl = (id) => sprites.pokemon(id)
    const handleImageError = (e) => { e.target.src = sprites.default }

    const fetchPokemonList = async () => {
      loading.value = true
      try {
        const res = await pokemonApi.getList({ size: 1025 })
        if (res.code === 200) pokemonList.value = res.data.records
      } catch (error) {
        console.error('获取宝可梦列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取道具列表并分类
    const fetchBattleItems = async () => {
      try {
        const res = await itemApi.getBattleItems()
        if (res.code === 200) {
          const items = res.data.records
          // 道具分类
          const damageIds = [217, 218, 247, 261, 262, 263, 287] // 讲究围巾、眼镜、生命宝珠等
          const defenseIds = [233, 234, 235, 236, 237, 251, 923] // 粗骨头、深海之牙、进化奇石等
          const berryIds = [149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164] // 减伤树果
          const typeBoostIds = [265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282,
            254, 255, 256, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313,
            656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673] // 属性强化道具和宝石
          
          items.forEach(item => {
            if (damageIds.includes(item.id)) {
              battleItems.value.damage.push(item)
            } else if (defenseIds.includes(item.id)) {
              battleItems.value.defense.push(item)
            } else if (berryIds.includes(item.id)) {
              battleItems.value.berries.push(item)
            } else if (typeBoostIds.includes(item.id)) {
              battleItems.value.typeBoost.push(item)
            } else if (item.cost > 0) {
              // 过滤掉一些非战斗道具
              battleItems.value.other.push(item)
            }
          })
        }
      } catch (error) {
        console.error('获取道具列表失败:', error)
      }
    }

    const fetchAttackerDetail = async (speciesId) => {
      try {
        const res = await pokemonApi.getDetail(speciesId)
        if (res.code === 200) {
          attackerDetail.value = res.data
          attackerForms.value = res.data.forms || []
          const defaultForm = res.data.forms?.find(f => f.isDefault) || res.data.forms?.[0]
          if (defaultForm) {
            attacker.value.formId = defaultForm.id
            if (defaultForm.stats) {
              attacker.value.attack = defaultForm.stats.attack
              attacker.value.spAttack = defaultForm.stats.spAttack
            }
            if (defaultForm.abilities?.length) {
              attacker.value.abilityId = defaultForm.abilities.find(a => !a.isHidden)?.id || defaultForm.abilities[0].id
            }
            await fetchAttackerMoves(defaultForm.id)
          }
        }
      } catch (error) {
        console.error('获取攻击方详情失败:', error)
      }
    }

    const fetchDefenderDetail = async (speciesId) => {
      try {
        const res = await pokemonApi.getDetail(speciesId)
        if (res.code === 200) {
          defenderDetail.value = res.data
          defenderForms.value = res.data.forms || []
          const defaultForm = res.data.forms?.find(f => f.isDefault) || res.data.forms?.[0]
          if (defaultForm) {
            defender.value.formId = defaultForm.id
            if (defaultForm.stats) {
              defender.value.hp = defaultForm.stats.hp
              defender.value.defense = defaultForm.stats.defense
              defender.value.spDefense = defaultForm.stats.spDefense
            }
            if (defaultForm.abilities?.length) {
              defender.value.abilityId = defaultForm.abilities.find(a => !a.isHidden)?.id || defaultForm.abilities[0].id
            }
          }
        }
      } catch (error) {
        console.error('获取防御方详情失败:', error)
      }
    }

    const fetchAttackerMoves = async (formId) => {
      try {
        const res = await pokemonApi.getFormMoves(formId)
        if (res.code === 200) {
          attackerMoves.value = res.data.filter(m => m.power && m.power > 0)
        }
      } catch (error) {
        console.error('获取技能列表失败:', error)
        attackerMoves.value = []
      }
    }

    const onAttackerSpeciesChange = (speciesId) => {
      attacker.value.formId = null
      attacker.value.moveId = null
      attackerMoves.value = []
      result.value = null
      if (speciesId) fetchAttackerDetail(speciesId)
    }

    const onAttackerFormChange = (formId) => {
      attacker.value.moveId = null
      result.value = null
      if (formId) {
        const form = attackerForms.value.find(f => f.id === formId)
        if (form?.stats) {
          attacker.value.attack = form.stats.attack
          attacker.value.spAttack = form.stats.spAttack
        }
        if (form?.abilities?.length) {
          attacker.value.abilityId = form.abilities.find(a => !a.isHidden)?.id || form.abilities[0].id
        }
        fetchAttackerMoves(formId)
      }
    }

    const onDefenderSpeciesChange = (speciesId) => {
      defender.value.formId = null
      result.value = null
      if (speciesId) fetchDefenderDetail(speciesId)
    }

    const onDefenderFormChange = (formId) => {
      result.value = null
      if (formId) {
        const form = defenderForms.value.find(f => f.id === formId)
        if (form?.stats) {
          defender.value.hp = form.stats.hp
          defender.value.defense = form.stats.defense
          defender.value.spDefense = form.stats.spDefense
        }
        if (form?.abilities?.length) {
          defender.value.abilityId = form.abilities.find(a => !a.isHidden)?.id || form.abilities[0].id
        }
      }
    }

    const onMoveChange = () => { result.value = null }

    const calculateDamage = async () => {
      if (!canCalculate.value) return
      calculating.value = true
      result.value = null

      try {
        const requestData = {
          attackerFormId: attacker.value.formId,
          defenderFormId: defender.value.formId,
          moveId: attacker.value.moveId,
          attackerLevel: attacker.value.level,
          attackerAbilityId: attacker.value.abilityId,
          attackerItemId: attacker.value.itemId,
          defenderAbilityId: defender.value.abilityId,
          defenderItemId: defender.value.itemId,
          attackerAttack: attacker.value.attack,
          attackerSpAttack: attacker.value.spAttack,
          attackerAttackBoost: attacker.value.attackBoost,
          attackerSpAttackBoost: attacker.value.spAttackBoost,
          attackerSpeedBoost: attacker.value.speedBoost,
          attackerAccuracyBoost: attacker.value.accuracyBoost,
          defenderHp: defender.value.hp,
          defenderDefense: defender.value.defense,
          defenderSpDefense: defender.value.spDefense,
          defenderDefenseBoost: defender.value.defenseBoost,
          defenderSpDefenseBoost: defender.value.spDefenseBoost,
          defenderSpeedBoost: defender.value.speedBoost,
          defenderEvasionBoost: defender.value.evasionBoost,
          defenderHp: defender.value.hp,
          defenderDefense: defender.value.defense,
          defenderSpDefense: defender.value.spDefense,
          defenderDefenseBoost: defender.value.defenseBoost,
          defenderSpDefenseBoost: defender.value.spDefenseBoost,
          attackerBurned: attacker.value.burned,
          attackerPoisoned: attacker.value.poisoned,
          attackerParalyzed: attacker.value.paralyzed,
          weather: battleSettings.value.weather || null,
          terrain: battleSettings.value.terrain || null,
          isCritical: battleSettings.value.isCritical,
          isDoubleBattle: battleSettings.value.isDoubleBattle,
          reflectActive: battleSettings.value.reflectActive,
          lightScreenActive: battleSettings.value.lightScreenActive,
          auroraVeilActive: battleSettings.value.auroraVeilActive
        }

        const res = await damageApi.calculate(requestData)
        if (res.code === 200) result.value = res.data
      } catch (error) {
        console.error('计算伤害失败:', error)
      } finally {
        calculating.value = false
      }
    }

    const getEffectivenessClass = (effectiveness) => {
      if (effectiveness >= 2) return 'super-effective'
      if (effectiveness > 1) return 'effective'
      if (effectiveness === 1) return 'neutral'
      if (effectiveness > 0) return 'not-effective'
      return 'no-effect'
    }

    const getBoostClass = (boost) => {
      if (boost > 0) return 'positive'
      if (boost < 0) return 'negative'
      return 'neutral'
    }

    const formatBoost = (boost) => {
      if (boost > 0) return '+' + boost
      return boost?.toString() || '0'
    }

    const getCategoryLabel = (category) => {
      const labels = {
        base: '基础',
        type: '属性',
        stab: '本系',
        weather: '天气',
        terrain: '场地',
        ability: '特性',
        item: '道具',
        boost: '等级',
        critical: '暴击',
        screen: '屏幕',
        status: '状态',
        final: '结果'
      }
      return labels[category] || category
    }

    onMounted(() => { fetchPokemonList(); fetchBattleItems() })

    return {
      pokemonList, loading, calculating,
      attacker, attackerDetail, attackerForms, attackerMoves,
      defender, defenderDetail, defenderForms,
      battleSettings, battleItems, result,
      attackerPokemon, defenderPokemon, attackerForm, defenderForm, selectedMove, canCalculate,
      getSpriteUrl, handleImageError,
      onAttackerSpeciesChange, onAttackerFormChange, onDefenderSpeciesChange, onDefenderFormChange, onMoveChange,
      calculateDamage, getEffectivenessClass, getBoostClass, formatBoost, getCategoryLabel
    }
  }
}
</script>

<style scoped>
/* 保留原有样式并添加新样式 */
.damage-calculator { max-width: 1400px; margin: 0 auto; padding: 20px; }
.calculator-header { text-align: center; margin-bottom: 30px; }
.calculator-grid { display: grid; grid-template-columns: 1fr 1fr 320px; gap: 20px; margin-bottom: 30px; }

@media (max-width: 1200px) {
  .calculator-grid { grid-template-columns: 1fr 1fr; }
  .battle-settings { grid-column: 1 / -1; }
}
@media (max-width: 768px) {
  .calculator-grid { grid-template-columns: 1fr; }
}

.card-section { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
.section-title { display: flex; align-items: center; gap: 8px; font-size: 1.25rem; font-weight: 600; color: #1f2937; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid #e5e7eb; }
.section-title .icon { font-size: 1.5rem; }
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 0.875rem; font-weight: 500; color: #374151; margin-bottom: 8px; }
.sub-label { font-size: 0.75rem; font-weight: 500; color: #6b7280; margin-bottom: 6px; display: block; }
.sub-title { font-size: 0.875rem; font-weight: 500; color: #374151; margin: 16px 0 8px; }
.w-full { width: 100%; }

/* 宝可梦卡片 */
.pokemon-card { background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-radius: 12px; padding: 16px; margin-bottom: 16px; border: 1px solid #e2e8f0; }
.attacker-card { border-left: 4px solid #3b82f6; }
.defender-card { border-left: 4px solid #ef4444; }
.pokemon-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.pokemon-sprite { width: 80px; height: 80px; object-fit: contain; image-rendering: pixelated; }
.pokemon-info { flex: 1; }
.pokemon-name { font-size: 1.125rem; font-weight: 600; color: #111827; margin-bottom: 8px; }
.pokemon-types { display: flex; flex-wrap: wrap; gap: 6px; }
.type-badge { padding: 4px 12px; border-radius: 16px; font-size: 0.75rem; font-weight: 500; border: 1px solid; }
.type-badge-mini { padding: 2px 8px; border-radius: 10px; font-size: 0.7rem; font-weight: 500; }

/* 能力值设置 */
.stats-section { border-top: 1px solid #e2e8f0; padding-top: 12px; }
.stats-title { font-size: 0.875rem; font-weight: 500; color: #6b7280; margin-bottom: 12px; }
.stat-row { display: flex; align-items: center; margin-bottom: 10px; gap: 12px; }
.stat-label { font-size: 0.8rem; color: #6b7280; min-width: 40px; }
.stat-controls { display: flex; align-items: center; gap: 8px; flex: 1; }
.boost-control { display: flex; align-items: center; gap: 4px; }
.boost-btn { width: 24px; height: 24px; border: 1px solid #d1d5db; background: #fff; border-radius: 4px; cursor: pointer; font-size: 14px; }
.boost-btn:hover { background: #f3f4f6; }
.boost-value { min-width: 24px; text-align: center; font-weight: 600; font-size: 0.875rem; }
.boost-value.positive { color: #22c55e; }
.boost-value.negative { color: #ef4444; }
.boost-value.neutral { color: #6b7280; }
.stat-input { width: 100px; }
.stat-base { font-size: 0.7rem; color: #9ca3af; }

/* 状态异常 */
.status-section { border-top: 1px solid #e2e8f0; padding-top: 12px; margin-top: 12px; }
.status-toggles { display: flex; gap: 8px; flex-wrap: wrap; }
.status-toggle { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; font-size: 0.75rem; transition: all 0.2s; }
.status-toggle input { display: none; }
.status-toggle:hover { background: #f3f4f6; }
.status-toggle.active { background: #fef3c7; border-color: #f59e0b; color: #92400e; }
.status-icon { font-size: 1rem; }

/* 特性选择 */
.ability-section { margin-top: 12px; }

/* 技能卡片 */
.move-card { background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%); border-radius: 12px; padding: 16px; margin-top: 16px; }
.move-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.move-header h4 { font-size: 1rem; font-weight: 600; color: #92400e; }
.damage-class { padding: 4px 12px; border-radius: 16px; font-size: 0.75rem; font-weight: 500; }
.damage-class.物理 { background: #dc2626; color: white; }
.damage-class.特殊 { background: #2563eb; color: white; }
.damage-class.变化 { background: #6b7280; color: white; }
.move-details { display: flex; gap: 16px; }
.detail-item { display: flex; align-items: center; gap: 6px; }
.detail-item .label { font-size: 0.75rem; color: #78716c; }
.detail-item .value { font-size: 0.875rem; font-weight: 600; color: #57534e; }

/* 战斗设置 */
.settings-grid { display: flex; flex-direction: column; gap: 12px; }
.setting-item { display: flex; justify-content: space-between; align-items: center; }
.setting-label { font-size: 0.875rem; font-weight: 500; color: #374151; }

/* 屏幕效果 */
.screen-settings { margin-top: 16px; border-top: 1px solid #e5e7eb; padding-top: 16px; }
.screen-toggles { display: flex; gap: 8px; flex-wrap: wrap; }
.screen-toggle { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; font-size: 0.75rem; transition: all 0.2s; }
.screen-toggle input { display: none; }
.screen-toggle:hover { background: #f3f4f6; }
.screen-toggle.active { background: #dbeafe; border-color: #3b82f6; color: #1d4ed8; }

.calculate-btn { width: 100%; margin-top: 20px; padding: 14px 24px; background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: white; border: none; border-radius: 12px; font-size: 1rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.calculate-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4); }
.calculate-btn:disabled { background: #9ca3af; cursor: not-allowed; }
.loading-spinner { display: inline-block; width: 16px; height: 16px; border: 2px solid #fff; border-top-color: transparent; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 结果区域 */
.result-section { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
.result-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
@media (max-width: 1024px) { .result-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .result-grid { grid-template-columns: 1fr; } }

.result-card { background: #f8fafc; border-radius: 12px; padding: 16px; text-align: center; }
.result-card h3 { font-size: 0.875rem; font-weight: 500; color: #6b7280; margin-bottom: 12px; }
.damage-display { display: flex; justify-content: space-around; }
.damage-value { display: flex; flex-direction: column; align-items: center; }
.damage-value .label { font-size: 0.7rem; color: #9ca3af; margin-bottom: 4px; }
.damage-value .value { font-size: 1.5rem; font-weight: 700; }
.damage-value.min .value { color: #22c55e; }
.damage-value.avg .value { color: #3b82f6; }
.damage-value.max .value { color: #ef4444; }
.ko-percent { margin-top: 8px; font-size: 0.75rem; color: #6b7280; }

/* 属性相性 */
.effectiveness-display { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.effectiveness-display .multiplier { font-size: 1.5rem; font-weight: 700; }
.effectiveness-display .desc { font-size: 0.75rem; }
.effectiveness-display.super-effective { color: #22c55e; }
.effectiveness-display.effective { color: #84cc16; }
.effectiveness-display.neutral { color: #6b7280; }
.effectiveness-display.not-effective { color: #f59e0b; }
.effectiveness-display.no-effect { color: #ef4444; }

/* 本系加成 */
.stab-display { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stab-display .status { font-size: 1.5rem; color: #9ca3af; }
.stab-display.active .status { color: #22c55e; }
.stab-display .multiplier { font-size: 1.25rem; font-weight: 700; color: #6b7280; }
.stab-display.active .multiplier { color: #3b82f6; }

/* 击杀预估 */
.ko-display { display: flex; flex-direction: column; gap: 8px; }
.ko-hp, .ko-hits { display: flex; justify-content: space-between; align-items: center; }
.ko-hp .label, .ko-hits .label { font-size: 0.75rem; color: #9ca3af; }
.ko-hp .value, .ko-hits .value { font-size: 0.875rem; font-weight: 600; color: #374151; }

/* 修正因子汇总 */
.multipliers-summary { background: #f8fafc; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.multipliers-summary h3 { font-size: 1rem; font-weight: 600; color: #374151; margin-bottom: 12px; }
.multipliers-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.multiplier-item { display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: white; border-radius: 8px; font-size: 0.8rem; border: 1px solid #e5e7eb; }
.multiplier-item.highlight { background: #fef3c7; border-color: #f59e0b; }
.multiplier-item .name { color: #6b7280; }
.multiplier-item .value { font-weight: 600; color: #374151; }

/* 特殊效果 */
.effects-section { background: #f8fafc; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.effects-section h3 { font-size: 1rem; font-weight: 600; color: #374151; margin-bottom: 12px; }
.effects-list { display: flex; flex-direction: column; gap: 8px; }
.effect-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: white; border-radius: 8px; font-size: 0.8rem; }
.effect-item .tag { padding: 2px 8px; background: #e5e7eb; border-radius: 4px; font-size: 0.7rem; font-weight: 500; }
.effect-item.attacker .tag { background: #dbeafe; color: #1d4ed8; }
.effect-item.defender .tag { background: #fee2e2; color: #dc2626; }

/* 计算详情 */
.calculation-details { background: #f8fafc; border-radius: 12px; padding: 16px; }
.calculation-details h3 { font-size: 1rem; font-weight: 600; color: #374151; margin-bottom: 12px; }
.steps-list { display: flex; flex-direction: column; gap: 8px; }
.step-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: white; border-radius: 8px; font-size: 0.8rem; }
.step-category { padding: 2px 8px; background: #e5e7eb; border-radius: 4px; font-size: 0.7rem; font-weight: 500; min-width: 40px; text-align: center; }
.step-item.base .step-category { background: #dbeafe; color: #1d4ed8; }
.step-item.type .step-category { background: #fef3c7; color: #92400e; }
.step-item.ability .step-category { background: #dcfce7; color: #166534; }
.step-item.weather .step-category { background: #e0f2fe; color: #0369a1; }
.step-item.critical .step-category { background: #fee2e2; color: #dc2626; }
.step-item.final .step-category { background: #f3e8ff; color: #7c3aed; }
.step-name { font-weight: 500; color: #374151; min-width: 80px; }
.step-formula { color: #6b7280; font-family: monospace; font-size: 0.75rem; }
.step-value { font-weight: 600; color: #3b82f6; margin-left: auto; }
.step-desc { color: #9ca3af; font-size: 0.75rem; }
</style>