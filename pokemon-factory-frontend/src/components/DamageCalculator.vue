<template>
  <div class="damage-calculator">
    <div class="calculator-header">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">
        伤害计算器
      </h1>
      <p class="text-gray-600">
        计算宝可梦战斗中的伤害值
      </p>
    </div>

    <div class="calculator-container">
      <!-- 攻击方选择 -->
      <div class="attacker-section">
        <h2 class="section-title">
          攻击方
        </h2>
        <div class="form-group">
          <label class="form-label">宝可梦</label>
          <el-select 
            v-model="attacker.pokemonId" 
            placeholder="选择宝可梦"
            filterable
            class="w-full"
            @change="onAttackerPokemonChange"
          >
            <el-option
              v-for="pokemon in pokemonList"
              :key="pokemon.id"
              :label="`${pokemon.name} (#${pokemon.indexNumber})`"
              :value="pokemon.id"
            />
          </el-select>
        </div>

        <div
          v-if="attackerPokemon"
          class="pokemon-info"
        >
          <div class="pokemon-basic-info">
            <div class="pokemon-image">
              <div class="w-20 h-20 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-xl flex items-center justify-center text-3xl font-bold text-blue-600 border-2 border-dashed border-blue-200">
                {{ attackerPokemon.indexNumber }}
              </div>
            </div>
            <div class="pokemon-details">
              <h3 class="pokemon-name">
                {{ attackerPokemon.name }}
              </h3>
              <div class="pokemon-types">
                <span 
                  v-for="type in attackerPokemon.types" 
                  :key="type.id"
                  class="type-badge"
                  :style="{ backgroundColor: type.color + '20', color: type.color }"
                >
                  {{ type.name }}
                </span>
              </div>
            </div>
          </div>

          <div class="pokemon-stats">
            <div class="stat-row">
              <span class="stat-label">HP:</span>
              <input 
                v-model.number="attacker.hp" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.hp }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">攻击:</span>
              <input 
                v-model.number="attacker.attack" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.attack }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">防御:</span>
              <input 
                v-model.number="attacker.defense" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.defense }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">特攻:</span>
              <input 
                v-model.number="attacker.spAttack" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.spAttack }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">特防:</span>
              <input 
                v-model.number="attacker.spDefense" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.spDefense }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">速度:</span>
              <input 
                v-model.number="attacker.speed" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ attacker.speed }}</span>
            </div>
          </div>
        </div>

        <div class="skill-section">
          <label class="form-label">技能</label>
          <el-select 
            v-model="attacker.skillId" 
            placeholder="选择技能"
            filterable
            class="w-full"
            @change="onAttackerSkillChange"
          >
            <el-option
              v-for="skill in attackerSkills"
              :key="skill.id"
              :label="`${skill.name} (${skill.type}) - ${skill.power || '-'}威力`"
              :value="skill.id"
            />
          </el-select>
        </div>

        <div
          v-if="attackerSkill"
          class="skill-info"
        >
          <div class="skill-basic-info">
            <h4>{{ attackerSkill.name }}</h4>
            <div class="skill-details">
              <span class="detail-item">
                <span class="label">属性:</span>
                <span class="value">{{ attackerSkill.type }}</span>
              </span>
              <span class="detail-item">
                <span class="label">分类:</span>
                <span class="value">{{ attackerSkill.category }}</span>
              </span>
              <span class="detail-item">
                <span class="label">威力:</span>
                <span class="value">{{ attackerSkill.power || '-' }}</span>
              </span>
              <span class="detail-item">
                <span class="label">命中:</span>
                <span class="value">{{ attackerSkill.accuracy || '-' }}</span>
              </span>
              <span class="detail-item">
                <span class="label">PP:</span>
                <span class="value">{{ attackerSkill.pp || '-' }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 防御方选择 -->
      <div class="defender-section">
        <h2 class="section-title">
          防御方
        </h2>
        <div class="form-group">
          <label class="form-label">宝可梦</label>
          <el-select 
            v-model="defender.pokemonId" 
            placeholder="选择宝可梦"
            filterable
            class="w-full"
            @change="onDefenderPokemonChange"
          >
            <el-option
              v-for="pokemon in pokemonList"
              :key="pokemon.id"
              :label="`${pokemon.name} (#${pokemon.indexNumber})`"
              :value="pokemon.id"
            />
          </el-select>
        </div>

        <div
          v-if="defenderPokemon"
          class="pokemon-info"
        >
          <div class="pokemon-basic-info">
            <div class="pokemon-image">
              <div class="w-20 h-20 bg-gradient-to-br from-red-100 to-pink-100 rounded-xl flex items-center justify-center text-3xl font-bold text-red-600 border-2 border-dashed border-red-200">
                {{ defenderPokemon.indexNumber }}
              </div>
            </div>
            <div class="pokemon-details">
              <h3 class="pokemon-name">
                {{ defenderPokemon.name }}
              </h3>
              <div class="pokemon-types">
                <span 
                  v-for="type in defenderPokemon.types" 
                  :key="type.id"
                  class="type-badge"
                  :style="{ backgroundColor: type.color + '20', color: type.color }"
                >
                  {{ type.name }}
                </span>
              </div>
            </div>
          </div>

          <div class="pokemon-stats">
            <div class="stat-row">
              <span class="stat-label">HP:</span>
              <input 
                v-model.number="defender.hp" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.hp }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">攻击:</span>
              <input 
                v-model.number="defender.attack" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.attack }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">防御:</span>
              <input 
                v-model.number="defender.defense" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.defense }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">特攻:</span>
              <input 
                v-model.number="defender.spAttack" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.spAttack }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">特防:</span>
              <input 
                v-model.number="defender.spDefense" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.spDefense }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">速度:</span>
              <input 
                v-model.number="defender.speed" 
                type="number" 
                min="0" 
                max="999"
                class="stat-input"
              >
              <span class="stat-value">{{ defender.speed }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 计算结果 -->
      <div class="result-section">
        <h2 class="section-title">
          计算结果
        </h2>
        
        <div
          v-if="!result"
          class="result-placeholder"
        >
          <p>请先选择攻击方和技能</p>
        </div>

        <div
          v-else
          class="result-content"
        >
          <div class="result-summary">
            <div class="damage-range">
              <h3>伤害范围</h3>
              <div class="damage-values">
                <div class="damage-min">
                  <span class="label">最小伤害:</span>
                  <span class="value">{{ result.minDamage }}</span>
                </div>
                <div class="damage-max">
                  <span class="label">最大伤害:</span>
                  <span class="value">{{ result.maxDamage }}</span>
                </div>
                <div class="damage-avg">
                  <span class="label">平均伤害:</span>
                  <span class="value">{{ result.avgDamage }}</span>
                </div>
              </div>
            </div>

            <div class="critical-chance">
              <h3>暴击率</h3>
              <div class="chance-value">
                {{ result.criticalChance }}%
              </div>
            </div>

            <div class="type-effectiveness">
              <h3>属性相性</h3>
              <div class="effectiveness-value">
                {{ result.typeEffectiveness }}
              </div>
            </div>
          </div>

          <div class="detailed-results">
            <h3>详细计算过程</h3>
            <div class="calculation-steps">
              <div class="step">
                <span class="step-label">基础伤害:</span>
                <span class="step-value">{{ result.baseDamage }}</span>
              </div>
              <div class="step">
                <span class="step-label">随机值:</span>
                <span class="step-value">{{ result.randomFactor }}</span>
              </div>
              <div class="step">
                <span class="step-label">能力修正:</span>
                <span class="step-value">{{ result.stabModifier }}</span>
              </div>
              <div class="step">
                <span class="step-label">属性相性:</span>
                <span class="step-value">{{ result.typeEffectiveness }}</span>
              </div>
              <div class="step">
                <span class="step-label">其他修正:</span>
                <span class="step-value">{{ result.otherModifier }}</span>
              </div>
            </div>
          </div>

          <div class="additional-info">
            <h3>额外信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">命中率:</span>
                <span class="info-value">{{ result.accuracy }}%</span>
              </div>
              <div class="info-item">
                <span class="info-label">优先级:</span>
                <span class="info-value">{{ result.priority }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">是否必中:</span>
                <span class="info-value">{{ result.isAlwaysHit ? '是' : '否' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">是否暴击:</span>
                <span class="info-value">{{ result.isCritical ? '是' : '否' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { ElSelect, ElOption } from 'element-plus'
import { usePokemonData } from '../composables/usePokemonData'

export default {
  name: 'DamageCalculator',
  components: {
    ElSelect,
    ElOption
  },
  setup() {
    const { pokemonApi } = usePokemonData()
    const pokemonList = ref([])
    const attackerSkills = ref([])

    // 攻击方数据
    const attacker = ref({
      pokemonId: null,
      skillId: null,
      hp: 100,
      attack: 100,
      defense: 100,
      spAttack: 100,
      spDefense: 100,
      speed: 100
    })

    // 防御方数据
    const defender = ref({
      pokemonId: null,
      hp: 100,
      attack: 100,
      defense: 100,
      spAttack: 100,
      spDefense: 100,
      speed: 100
    })

    // 计算结果
    const result = ref(null)

    // 计算伤害的函数
    const calculateDamage = () => {
      if (!attacker.value.pokemonId || !attacker.value.skillId || !defender.value.pokemonId) {
        result.value = null
        return
      }

      // 获取宝可梦和技能数据
      const attackerPokemon = pokemonList.value.find(p => p.id === attacker.value.pokemonId)
      const defenderPokemon = pokemonList.value.find(p => p.id === defender.value.pokemonId)
      const skill = attackerSkills.value.find(s => s.id === attacker.value.skillId)

      if (!attackerPokemon || !defenderPokemon || !skill) {
        result.value = null
        return
      }

      // 基础伤害计算（简化版）
      const baseDamage = calculateBaseDamage(attackerPokemon, defenderPokemon, skill)
      
      // 随机值（85% - 100%）
      const randomFactor = Math.random() * 0.15 + 0.85
      
      // STAB（相同属性加成）
      const stabModifier = skill.type && attackerPokemon.types.some(t => t.name === skill.type) ? 1.5 : 1
      
      // 属性相性（简化版）
      const typeEffectiveness = calculateTypeEffectiveness(skill.type, defenderPokemon.types)
      
      // 其他修正（暴击、天气等）
      const otherModifier = 1.0
      
      // 最终伤害
      const damage = Math.floor(baseDamage * randomFactor * stabModifier * typeEffectiveness * otherModifier)
      
      // 暴击率（简化版）
      const criticalChance = 6.25
      
      // 命中率
      const accuracy = skill.accuracy || 100
      
      // 优先级
      const priority = 0
      
      // 暴击判定
      const isCritical = Math.random() * 100 < criticalChance
      
      // 重新计算暴击伤害
      let finalDamage = damage
      if (isCritical) {
        finalDamage = Math.floor(damage * 1.5)
      }

      result.value = {
        baseDamage: Math.floor(baseDamage),
        randomFactor: randomFactor.toFixed(2),
        stabModifier: stabModifier,
        typeEffectiveness: typeEffectiveness,
        otherModifier: otherModifier,
        minDamage: Math.floor(finalDamage * 0.85),
        maxDamage: Math.floor(finalDamage * 1.00),
        avgDamage: Math.floor(finalDamage * 0.925),
        criticalChance: criticalChance,
        accuracy: accuracy,
        priority: priority,
        isAlwaysHit: accuracy >= 100,
        isCritical: isCritical
      }
    }

    // 基础伤害计算
    const calculateBaseDamage = (attackerPokemon, defenderPokemon, skill) => {
      // 基础公式（简化版）
      let baseDamage = 0
      
      if (skill.category === '物理') {
        // 物理攻击
        baseDamage = Math.floor(
          ((attacker.value.attack * skill.power) / defender.value.defense) * 0.5 + 2
        )
      } else if (skill.category === '特殊') {
        // 特殊攻击
        baseDamage = Math.floor(
          ((attacker.value.spAttack * skill.power) / defender.value.spDefense) * 0.5 + 2
        )
      } else {
        // 变化技能
        baseDamage = 0
      }
      
      // 等级修正
      baseDamage = Math.floor(baseDamage * (attacker.value.level || 50) / 50)
      
      // 类型修正
      baseDamage = Math.floor(baseDamage * 1.0)
      
      return Math.max(1, baseDamage)
    }

    // 属性相性计算
    const calculateTypeEffectiveness = (skillType, defenderTypes) => {
      // 简化的属性相性表
      const effectivenessChart = {
        '一般': { '一般': 1, '格斗': 1, '飞行': 1, '毒': 1, '地面': 1, '岩石': 0.5, '虫': 1, '幽灵': 0, '钢': 0.5, '火': 1, '水': 1, '草': 1, '电': 1, '超能力': 1, '冰': 1, '龙': 1, '恶': 1 },
        '格斗': { '一般': 2, '格斗': 1, '飞行': 0.5, '毒': 0.5, '地面': 1, '岩石': 2, '虫': 0.5, '幽灵': 0, '钢': 2, '火': 1, '水': 1, '草': 1, '电': 1, '超能力': 0.5, '冰': 2, '龙': 1, '恶': 2 },
        '飞行': { '一般': 1, '格斗': 2, '飞行': 1, '毒': 1, '地面': 1, '岩石': 0.5, '虫': 2, '幽灵': 1, '钢': 0.5, '火': 1, '水': 1, '草': 2, '电': 0.5, '超能力': 1, '冰': 1, '龙': 1, '恶': 1 },
        '毒': { '一般': 1, '格斗': 1, '飞行': 1, '毒': 1, '地面': 0.5, '岩石': 0.5, '虫': 1, '幽灵': 0.5, '钢': 0, '火': 1, '水': 1, '草': 2, '电': 1, '超能力': 1, '冰': 1, '龙': 1, '恶': 1 },
        '地面': { '一般': 1, '格斗': 1, '飞行': 2, '毒': 2, '地面': 1, '岩石': 1, '虫': 0.5, '幽灵': 1, '钢': 2, '火': 2, '水': 1, '草': 0.5, '电': 1, '超能力': 1, '冰': 1, '龙': 1, '恶': 1 },
        '岩石': { '一般': 1, '格斗': 0.5, '飞行': 2, '毒': 1, '地面': 0.5, '岩石': 1, '虫': 2, '幽灵': 1, '钢': 0.5, '火': 2, '水': 1, '草': 1, '电': 1, '超能力': 1, '冰': 2, '龙': 1, '恶': 1 },
        '虫': { '一般': 1, '格斗': 0.5, '飞行': 0.5, '毒': 0.5, '地面': 1, '岩石': 1, '虫': 1, '幽灵': 0.5, '钢': 0.5, '火': 0.5, '水': 1, '草': 2, '电': 1, '超能力': 1, '冰': 1, '龙': 1, '恶': 1 },
        '幽灵': { '一般': 0, '格斗': 1, '飞行': 1, '毒': 1, '地面': 1, '岩石': 1, '虫': 1, '幽灵': 2, '钢': 1, '火': 1, '水': 1, '草': 1, '电': 1, '超能力': 2, '冰': 1, '龙': 1, '恶': 0 },
        '钢': { '一般': 1, '格斗': 1, '飞行': 1, '毒': 1, '地面': 1, '岩石': 2, '虫': 1, '幽灵': 1, '钢': 0.5, '火': 0.5, '水': 0.5, '草': 1, '电': 0.5, '超能力': 1, '冰': 2, '龙': 1, '恶': 1 },
        '火': { '一般': 1, '格斗': 1, '飞行': 1, '毒': 1, '地面': 1, '岩石': 0.5, '虫': 2, '幽灵': 1, '钢': 2, '火': 0.5, '水': 0.5, '草': 2, '电': 1, '超能力': 1, '冰': 2, '龙': 0.5, '恶': 1 },
        '水': { '一般': 1, '格斗': 1, '飞行': 1, '毒': 1, '地面': 2, '岩石': 2, '虫': 1, '幽灵': 1, '钢': 1, '火': 2, 'water': 0.5, '草': 0.5, '电': 1, '超能力': 1, '冰': 1, '龙': 0.5, '恶': 1 },
        '草': { '一般': 1, '格斗': 1, '飞行': 0.5, '毒': 0.5, 'ground': 2, 'rock': 0.5, 'bug': 0.5, 'ghost': 1, 'steel': 0.5, 'fire': 0.5, 'water': 2, 'grass': 0.5, 'electric': 1, 'psychic': 1, 'ice': 1, 'dragon': 0.5, 'dark': 1 },
        '电': { '一般': 1, 'fighting': 1, 'flying': 2, 'poison': 1, 'ground': 0, 'rock': 1, 'bug': 1, 'ghost': 1, 'steel': 1, 'fire': 1, 'water': 2, 'grass': 0.5, 'electric': 0.5, 'psychic': 1, 'ice': 1, 'dragon': 0.5, 'dark': 1 },
        '超能力': { 'normal': 1, 'fighting': 2, 'flying': 1, 'poison': 2, 'ground': 1, 'rock': 1, 'bug': 1, 'ghost': 1, 'steel': 0.5, 'fire': 1, 'water': 1, 'grass': 1, 'electric': 1, 'psychic': 0.5, 'ice': 1, 'dragon': 1, 'dark': 0 },
        '冰': { 'normal': 1, 'fighting': 1, 'flying': 1, 'poison': 1, 'ground': 1, 'rock': 1, 'bug': 1, 'ghost': 1, 'steel': 0.5, 'fire': 0.5, 'water': 0.5, 'grass': 2, 'electric': 1, 'psychic': 1, 'ice': 0.5, 'dragon': 2, 'dark': 1 },
        '龙': { 'normal': 1, 'fighting': 1, 'flying': 1, 'poison': 1, 'ground': 1, 'rock': 1, 'bug': 1, 'ghost': 1, 'steel': 0.5, 'fire': 1, 'water': 1, 'grass': 1, 'electric': 1, 'psychic': 1, 'ice': 1, 'dragon': 2, 'dark': 1 },
        '恶': { 'normal': 1, 'fighting': 0.5, 'flying': 1, 'poison': 1, 'ground': 1, 'rock': 1, 'bug': 1, 'ghost': 1, 'steel': 0.5, 'fire': 1, 'water': 1, 'grass': 1, 'electric': 1, 'psychic': 2, 'ice': 1, 'dragon': 1, 'dark': 0.5 }
      }

      let effectiveness = 1
      for (const defenderType of defenderTypes) {
        const typeEffectiveness = effectivenessChart[skillType]?.[defenderType.name] || 1
        effectiveness *= typeEffectiveness
      }

      return effectiveness
    }

    // 获取宝可梦列表
    const fetchPokemonList = async () => {
      try {
        const result = await pokemonApi.getList()
        if (result.code === 200) {
          pokemonList.value = result.data
        }
      } catch (error) {
        console.error('获取宝可梦列表失败:', error)
      }
    }

    // 获取攻击方技能
    const fetchAttackerSkills = async () => {
      if (!attacker.value.pokemonId) return
      
      try {
        const result = await pokemonApi.getMoves(attacker.value.pokemonId)
        if (result.code === 200) {
          attackerSkills.value = result.data
        }
      } catch (error) {
        console.error('获取技能失败:', error)
      }
    }

    // 攻击方宝可梦变化
    const onAttackerPokemonChange = () => {
      fetchAttackerSkills()
      calculateDamage()
    }

    // 攻击方技能变化
    const onAttackerSkillChange = () => {
      calculateDamage()
    }

    // 防御方宝可梦变化
    const onDefenderPokemonChange = () => {
      calculateDamage()
    }

    // 监听数据变化，自动计算伤害
    const watch = (source, callback) => {
      const oldValue = source.value
      const newValue = source.value
      if (oldValue !== newValue) {
        callback()
      }
    }

    onMounted(() => {
      fetchPokemonList()
    })

    return {
      pokemonList,
      attacker,
      attackerSkills,
      attackerPokemon: computed(() => pokemonList.value.find(p => p.id === attacker.value.pokemonId)),
      attackerSkill: computed(() => attackerSkills.value.find(s => s.id === attacker.value.skillId)),
      defender,
      defenderPokemon: computed(() => pokemonList.value.find(p => p.id === defender.value.pokemonId)),
      result,
      calculateDamage,
      onAttackerPokemonChange,
      onAttackerSkillChange,
      onDefenderPokemonChange
    }
  }
}
</script>

<style scoped>
.damage-calculator {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.calculator-header {
  text-align: center;
  margin-bottom: 30px;
}

.calculator-container {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e5e7eb;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.pokemon-info {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.pokemon-basic-info {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.pokemon-image {
  margin-right: 12px;
}

.pokemon-details {
  flex: 1;
}

.pokemon-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.pokemon-types {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.type-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  display: inline-block;
}

.skill-section {
  margin-bottom: 16px;
}

.skill-info {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.skill-basic-info {
  margin-bottom: 8px;
}

.skill-basic-info h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.skill-details {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 8px;
  background: white;
  border-radius: 4px;
  min-width: 60px;
}

.detail-item .label {
  font-size: 0.75rem;
  color: #6b7280;
  margin-bottom: 2px;
}

.detail-item .value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
}

.pokemon-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  background: white;
  border-radius: 4px;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.stat-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.875rem;
  text-align: center;
}

.stat-value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
  margin-left: 8px;
}

.result-section {
  background: #f9fafb;
  border-radius: 8px;
  padding: 20px;
  grid-column: 1 / -1;
}

.result-placeholder {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.damage-range,
.critical-chance,
.type-effectiveness {
  background: white;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.damage-range h3,
.critical-chance h3,
.type-effectiveness h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.damage-values {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.damage-min,
.damage-max,
.damage-avg {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 4px;
}

.damage-min .label,
.damage-max .label,
.damage-avg .label {
  font-size: 0.875rem;
  color: #6b7280;
}

.damage-min .value,
.damage-max .value,
.damage-avg .value {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
}

.chance-value,
.effectiveness-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ef4444;
}

.detailed-results {
  background: white;
  border-radius: 8px;
  padding: 16px;
}

.detailed-results h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.calculation-steps {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.step {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 4px;
}

.step-label {
  font-size: 0.875rem;
  color: #6b7280;
}

.step-value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
}

.additional-info {
  background: white;
  border-radius: 8px;
  padding: 16px;
}

.additional-info h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: #f3f4f6;
  border-radius: 4px;
}

.info-label {
  font-size: 0.875rem;
  color: #6b7280;
}

.info-value {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .calculator-container {
    grid-template-columns: 1fr 1fr;
  }
  
  .result-section {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .calculator-container {
    grid-template-columns: 1fr;
  }
  
  .result-summary {
    grid-template-columns: 1fr;
  }
  
  .calculation-steps,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>