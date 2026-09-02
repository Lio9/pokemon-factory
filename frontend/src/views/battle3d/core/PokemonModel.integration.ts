/**
 * @description Integration example for PokemonModel with battle system
 * @description PokemonModel 与战斗系统集成示例
 */

import * as THREE from 'three'
import { PokemonEntity, typeColorMap } from './PokemonModel'
import { Battlefield } from './BattleField'

/**
 * Pokemon battle manager
 * 宝可梦战斗管理器
 */
export class PokemonBattleManager {
  private scene: THREE.Scene
  private battlefield: Battlefield
  private pokemonEntities: { [key: string]: PokemonEntity } = {}
  
  constructor(scene: THREE.Scene) {
    this.scene = scene
    this.battlefield = new Battlefield(scene)
  }
  
  /**
   * Add a Pokemon to the battlefield
   * 将宝可梦添加到战场
   * 
   * @param config - Pokemon configuration / 宝可梦配置
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-2) / 位置索引（0-2）
   */
  public addPokemon(
    config: {
      name: string
      type: string
      currentHp: number
      maxHp: number
    },
    side: 'player' | 'opponent',
    slot: 0 | 1
  ): PokemonEntity {
    // Create Pokemon entity
    // 创建宝可梦实体
    const pokemon = new PokemonEntity(config)
    
    // Get world position from battlefield
    // 从战场获取世界坐标
    const position = this.battlefield.getSlotWorldPosition(side, slot as 0 | 1)
    
    // Set position
    // 设置位置
    pokemon.setPosition(position.x, position.y, position.z)
    
    // Add to scene
    // 添加到场景
    this.scene.add(pokemon.group)
    
    // Store reference
    // 存储引用
    const key = `${side}_${slot}`
    this.pokemonEntities[key] = pokemon
    
    return pokemon
  }
  
  /**
   * Get Pokemon entity by side and slot
   * 根据阵营和位置获取宝可梦实体
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-2) / 位置索引（0-2）
   * @returns Pokemon entity or undefined / 宝可梦实体或 undefined
   */
  public getPokemon(side: 'player' | 'opponent', slot: 0 | 1): PokemonEntity | undefined {
    const key = `${side}_${slot}`
    return this.pokemonEntities[key]
  }
  
  /**
   * Remove Pokemon from battlefield
   * 从战场移除宝可梦
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-2) / 位置索引（0-2）
   */
  public removePokemon(side: 'player' | 'opponent', slot: 0 | 1): void {
    const key = `${side}_${slot}`
    const pokemon = this.pokemonEntities[key]
    
    if (pokemon) {
      pokemon.dispose()
      delete this.pokemonEntities[key]
    }
  }
  
  /**
   * Play attack animation
   * 播放攻击动画
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param duration - Animation duration in milliseconds / 动画持续时间（毫秒）
   */
  public playAttackAnimation(side: 'player' | 'opponent', slot: 0 | 1, duration: number = 1000): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.playAnimation('attack', duration)
    }
  }
  
  /**
   * Play hit animation
   * 播放受击动画
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param duration - Animation duration in milliseconds / 动画持续时间（毫秒）
   */
  public playHitAnimation(side: 'player' | 'opponent', slot: 0 | 1, duration: number = 500): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.playAnimation('hit', duration)
    }
  }
  
  /**
   * Play faint animation
   * 播放倒下动画
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param duration - Animation duration in milliseconds / 动画持续时间（毫秒）
   */
  public playFaintAnimation(side: 'player' | 'opponent', slot: 0 | 1, duration: number = 1500): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.playAnimation('faint', duration)
    }
  }
  
  /**
   * Play heal animation
   * 播放治愈动画
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param duration - Animation duration in milliseconds / 动画持续时间（毫秒）
   */
  public playHealAnimation(side: 'player' | 'opponent', slot: 0 | 1, duration: number = 1500): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.playAnimation('heal', duration)
    }
  }
  
  /**
   * Update Pokemon HP
   * 更新宝可梦生命值
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param currentHp - Current HP / 当前生命值
   * @param maxHp - Max HP / 最大生命值
   */
  public updatePokemonHP(
    side: 'player' | 'opponent',
    slot: 0 | 1,
    currentHp: number,
    maxHp: number
  ): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.updateHpBar(currentHp, maxHp)
    }
  }
  
  /**
   * Highlight Pokemon
   * 高亮宝可梦
   * 
   * @param side - 'player' or 'opponent' / '玩家' 或 '对手'
   * @param slot - Slot index (0-1) / 位置索引（0-1）
   * @param highlighted - Whether to highlight / 是否高亮
   */
  public highlightPokemon(
    side: 'player' | 'opponent',
    slot: 0 | 1,
    highlighted: boolean
  ): void {
    const pokemon = this.getPokemon(side, slot)
    if (pokemon) {
      pokemon.setHighlighted(highlighted)
    }
  }
  
  /**
   * Dispose all resources
   * 清理所有资源
   */
  public dispose(): void {
    // Dispose all Pokemon entities
    // 清理所有宝可梦实体
    for (const key in this.pokemonEntities) {
      if (this.pokemonEntities.hasOwnProperty(key)) {
        this.pokemonEntities[key].dispose()
      }
    }
    this.pokemonEntities = {}
    
    // Dispose battlefield
    // 清理战场
    this.battlefield.dispose()
  }
}

/**
 * Example usage
 * 使用示例
 */
export function exampleUsage(): void {
  // Create scene
  // 创建场景
  const scene = new THREE.Scene()
  
  // Create battle manager
  // 创建战斗管理器
  const battleManager = new PokemonBattleManager(scene)
  
  // Add player Pokemon
  // 添加玩家宝可梦
  const pikachu = battleManager.addPokemon(
    {
      name: '皮卡丘',
      type: '电',
      currentHp: 100,
      maxHp: 100
    },
    'player',
    0
  )
  
  // Add opponent Pokemon
  // 添加对手宝可梦
  const charizard = battleManager.addPokemon(
    {
      name: '喷火龙',
      type: '火',
      currentHp: 150,
      maxHp: 150
    },
    'opponent',
    0
  )
  
  // Simulate battle
  // 模拟战斗
  setTimeout(() => {
    battleManager.playAttackAnimation('player', 0 as 0 | 1, 1000)
  }, 1000)
  
  setTimeout(() => {
    battleManager.playHitAnimation('opponent', 0 as 0 | 1, 500)
    battleManager.updatePokemonHP('opponent', 0 as 0 | 1, 120, 150)
  }, 2000)
  
  setTimeout(() => {
    battleManager.playAttackAnimation('opponent', 0 as 0 | 1, 1000)
  }, 3000)
  
  setTimeout(() => {
    battleManager.playHitAnimation('player', 0 as 0 | 1, 500)
    battleManager.updatePokemonHP('player', 0 as 0 | 1, 70, 100)
  }, 4000)
  
  setTimeout(() => {
    battleManager.playHealAnimation('player', 0 as 0 | 1, 1500)
    battleManager.updatePokemonHP('player', 0 as 0 | 1, 100, 100)
  }, 5000)
  
  setTimeout(() => {
    battleManager.highlightPokemon('player', 0 as 0 | 1, true)
  }, 6000)
  
  setTimeout(() => {
    battleManager.highlightPokemon('player', 0 as 0 | 1, false)
  }, 7000)
  
  // Cleanup after 10 seconds
  // 10 秒后清理
  setTimeout(() => {
    battleManager.dispose()
  }, 10000)
}