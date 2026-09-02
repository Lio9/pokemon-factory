/**
 * @description 战斗引擎桥接层 - 连接 Vue 状态管理与 3D 战斗系统
 * @description Battle engine bridge - connects Vue state management with 3D battle system
 *
 * 本模块负责：
 * 1. 将后端 API 返回的 battle summary 同步到 3D 场景
 * 2. 将 3D 交互事件转换为后端 API 调用
 * 3. 管理 3D 场景中的宝可梦实体生命周期
 * 4. 协调动画播放与状态更新
 *
 * @module composables/battle3d/useBattleEngine
 */

import { ref, watch, type Ref, computed } from 'vue'
import * as THREE from 'three'
import { PokemonEntity } from '../../views/battle3d/core/PokemonModel'
import { BattleStateMachine, type BattlePhase } from '../../views/battle3d/core/BattleStateMachine'
import { EffectsManager } from '../../views/battle3d/core/EffectsManager'
import { debugLogger } from '../../views/battle3d/utils/debug'

/**
 * 战斗引擎配置
 * Battle engine configuration
 */
interface BattleEngineConfig {
  /** Three.js 场景引用 */
  scene: Ref<THREE.Scene | null>
  /** 战场实例 */
  battlefield: Ref<any | null>
}

/**
 * 宝可梦实体映射
 * Pokemon entity map: key = "side-slot" (e.g., "player-0", "opponent-1")
 */
interface EntityMap {
  [key: string]: PokemonEntity
}

/**
 * useBattleEngine 组合式函数
 * useBattleEngine composable
 *
 * @param config - 战斗引擎配置
 * @param summary - 后端返回的战斗摘要 Ref
 * @returns 战斗引擎控制接口
 */
export function useBattleEngine(
  config: BattleEngineConfig,
  summary: Ref<any | null>
) {
  const { scene, battlefield } = config

  // ===== 状态机 =====
  const stateMachine = new BattleStateMachine()

  // ===== 特效管理器 =====
  const effectsManager = ref<EffectsManager | null>(null)

  // ===== 宝可梦实体映射 =====
  const entities: EntityMap = {}

  // ===== 当前阶段 =====
  const currentPhase = ref<BattlePhase>('idle')

  // ===== 战斗引擎就绪状态 =====
  const isEngineReady = ref(false)

  // ===== 初始化引擎 =====
  function initEngine() {
    if (!scene.value) {
      debugLogger.log('warn', 'battle', '场景未就绪，无法初始化引擎')
      return
    }

    // 创建特效管理器
    effectsManager.value = new EffectsManager(scene.value)

    // 监听状态机变化（使用事件发射器模式）
    stateMachine.on('change', (event: any) => {
      currentPhase.value = event.to
      debugLogger.log('info', 'battle', `战斗阶段切换: ${event.from} -> ${event.to}`)
    })

    isEngineReady.value = true
    debugLogger.log('info', 'battle', '战斗引擎初始化完成')
  }

  // ===== 从后端 summary 同步宝可梦实体到 3D 场景 =====
  function syncEntitiesFromSummary() {
    const s = summary.value
    if (!s || !scene.value || !battlefield.value) return

    const bf = battlefield.value

    // 同步玩家侧宝可梦
    const playerTeam = s.playerTeam || []
    const playerActiveSlots = s.playerActiveSlots || []
    playerActiveSlots.forEach((teamIndex: number, fieldSlot: number) => {
      const pokemon = playerTeam[teamIndex]
      if (!pokemon) return

      const entityKey = `player-${fieldSlot}`
      if (!entities[entityKey]) {
        // 创建新实体
        const entity = createPokemonEntity(pokemon, 'player', fieldSlot)
        if (entity) {
          entities[entityKey] = entity
          scene.value!.add(entity.group)

          // 设置位置
          const pos = bf.getSlotWorldPosition('player', fieldSlot)
          entity.setPosition(pos.x, pos.y, pos.z)

          debugLogger.log('debug', 'battle', `创建玩家宝可梦: ${pokemon.name || pokemon.name_en} @ slot ${fieldSlot}`)
        }
      } else {
        // 更新已有实体状态
        const entity = entities[entityKey]
        entity.updateHpBar(pokemon.currentHp || 0, pokemon.stats?.hp || pokemon.currentHp || 100)
      }
    })

    // 同步对手侧宝可梦
    const opponentTeam = s.opponentTeam || []
    const opponentActiveSlots = s.opponentActiveSlots || []
    opponentActiveSlots.forEach((teamIndex: number, fieldSlot: number) => {
      const pokemon = opponentTeam[teamIndex]
      if (!pokemon) return

      const entityKey = `opponent-${fieldSlot}`
      if (!entities[entityKey]) {
        const entity = createPokemonEntity(pokemon, 'opponent', fieldSlot)
        if (entity) {
          entities[entityKey] = entity
          scene.value!.add(entity.group)

          const pos = bf.getSlotWorldPosition('opponent', fieldSlot)
          entity.setPosition(pos.x, pos.y, pos.z)

          debugLogger.log('debug', 'battle', `创建对手宝可梦: ${pokemon.name || pokemon.name_en} @ slot ${fieldSlot}`)
        }
      } else {
        const entity = entities[entityKey]
        entity.updateHpBar(pokemon.currentHp || 0, pokemon.stats?.hp || pokemon.currentHp || 100)
      }
    })

    // 处理倒下的宝可梦
    processFaintedPokemon(s)
  }

  /**
   * 创建宝可梦实体
   * Create a Pokemon entity from battle data
   */
  function createPokemonEntity(pokemon: any, side: 'player' | 'opponent', slot: number): PokemonEntity | null {
    try {
      const entity = new PokemonEntity({
        name: pokemon.name || pokemon.name_en || `Pokemon ${slot + 1}`,
        type: (pokemon.types?.[0]?.name_en || pokemon.types?.[0]?.name || 'Normal'),
        currentHp: pokemon.currentHp || 0,
        maxHp: pokemon.stats?.hp || pokemon.currentHp || 100
      })

      return entity
    } catch (err) {
      debugLogger.log('error', 'battle', `创建宝可梦实体失败: ${err}`)
      return null
    }
  }

  /**
   * 处理倒下的宝可梦
   * Process fainted Pokemon
   */
  function processFaintedPokemon(s: any) {
    const allTeam = [...(s.playerTeam || []), ...(s.opponentTeam || [])]
    const allActive = [...(s.playerActiveSlots || []), ...(s.opponentActiveSlots || [])]

    // 检查不在场上的宝可梦是否倒下
    for (const [key, entity] of Object.entries(entities)) {
      const [side, slotStr] = key.split('-')
      const slot = parseInt(slotStr)
      const team = side === 'player' ? s.playerTeam : s.opponentTeam
      const activeSlots = side === 'player' ? s.playerActiveSlots : s.opponentActiveSlots

      if (!activeSlots.includes(slot)) {
        // 宝可梦不在场上，播放倒下动画并移除
        entity.playAnimation('faint', 1500)
        setTimeout(() => {
          if (scene.value) {
            scene.value.remove(entity.group)
          }
          entity.dispose()
          delete entities[key]
        }, 1500)
      } else {
        // 检查在场上的宝可梦是否倒下
        const pokemon = team?.[slot]
        if (pokemon && (pokemon.currentHp <= 0 || pokemon.fainted)) {
          entity.playAnimation('faint', 1500)
        }
      }
    }
  }

  /**
   * 播放攻击动画
   * Play attack animation
   */
  function playAttackAnimation(attackerKey: string, targetKey: string, moveType?: string) {
    const attacker = entities[attackerKey]
    const target = entities[targetKey]
    if (!attacker || !target) return

    // 攻击者冲刺动画
    attacker.playAnimation('attack', 600)

    // 延迟后播放受击效果
    setTimeout(() => {
      target.playAnimation('hit', 400)

      // 播放粒子特效
      if (effectsManager.value) {
        const attackerPos = attacker.group.position
        const targetPos = target.group.position
        effectsManager.value.attackHit(attackerPos, targetPos, moveType || 'Normal', 5, 800)
      }

      // 相机震动
      if (effectsManager.value) {
        effectsManager.value.shakeCamera(0.3, 300)
      }
    }, 300)
  }

  /**
   * 播放治愈动画
   * Play heal animation
   */
  function playHealAnimation(targetKey: string) {
    const target = entities[targetKey]
    if (!target) return

    target.playAnimation('heal', 800)

    if (effectsManager.value) {
      const pos = target.group.position
      effectsManager.value.heal(pos, '#4ade80', 4, 1000)
    }
  }

  /**
   * 播放太晶化动画
   * Play terastallize animation
   */
  function playTerastallizeAnimation(targetKey: string) {
    const target = entities[targetKey]
    if (!target) return

    if (effectsManager.value) {
      const pos = target.group.position
      effectsManager.value.terastallize(pos, '#a78bfa', 8, 2000)
    }
  }

  /**
   * 获取属性颜色
   * Get type color
   */
  function getTypeColor(typeName: string): string {
    const colorMap: Record<string, string> = {
      Normal: '#A8A77A', Fire: '#EE8130', Water: '#6390F0', Electric: '#F7D02C',
      Grass: '#7AC74C', Ice: '#96D9D6', Fighting: '#C22E28', Poison: '#A33EA1',
      Ground: '#E2BF65', Flying: '#A98FF3', Psychic: '#F95587', Bug: '#A6B91A',
      Rock: '#B6A136', Ghost: '#735797', Dragon: '#6F35FC', Dark: '#705746',
      Steel: '#B7B7CE', Fairy: '#D685AD'
    }
    return colorMap[typeName] || '#A8A77A'
  }

  /**
   * 清除所有实体
   * Clear all entities
   */
  function clearAllEntities() {
    for (const [key, entity] of Object.entries(entities)) {
      if (scene.value) {
        scene.value.remove(entity.group)
      }
      entity.dispose()
      delete entities[key]
    }
    debugLogger.log('info', 'battle', '已清除所有宝可梦实体')
  }

  /**
   * 更新特效管理器
   * Update effects manager
   */
  function updateEffects(delta: number) {
    if (effectsManager.value) {
      effectsManager.value.update(delta)
    }
  }

  // ===== 监听 summary 变化同步实体 =====
  watch(summary, () => {
    syncEntitiesFromSummary()
  }, { deep: true })

  // ===== 清理 =====
  function dispose() {
    clearAllEntities()
    if (effectsManager.value) {
      effectsManager.value.dispose()
    }
    stateMachine.reset()
    isEngineReady.value = false
    debugLogger.log('info', 'battle', '战斗引擎已销毁')
  }

  return {
    // 状态
    currentPhase,
    isEngineReady,
    entities,

    // 方法
    initEngine,
    syncEntitiesFromSummary,
    clearAllEntities,
    playAttackAnimation,
    playHealAnimation,
    playTerastallizeAnimation,
    updateEffects,
    dispose,

    // 子系统
    stateMachine,
    effectsManager
  }
}
