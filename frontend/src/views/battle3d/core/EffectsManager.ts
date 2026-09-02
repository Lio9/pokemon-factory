/**
 * @description Particle effects and animation management system for battle visualization
 * @description 粒子特效和动画管理系统，用于增强战斗的视觉冲击力
 * 
 * @author MiMo-v2.5-pro
 * @version 1.0.0
 */

import * as THREE from 'three'

/**
 * Effect configuration options
 * 特效配置选项
 */
interface EffectConfig {
  /** Effect intensity (1-10) / 特效强度（1-10） */
  intensity: number
  /** Effect duration in milliseconds / 持续时间（毫秒） */
  duration: number
  /** Whether effect is active / 特效是否激活 */
  active: boolean
  /** Start time of effect / 特效开始时间 */
  startTime: number
  /** Position of effect / 特效位置 */
  position: THREE.Vector3
  /** Main color of effect / 特效主颜色 */
  color: THREE.Color
}

/**
 * Particle pool configuration
 * 粒子池配置
 */
interface ParticlePool {
  /** Available particles / 可用粒子 */
  available: THREE.Points[]
  /** Active particles / 活跃粒子 */
  active: THREE.Points[]
  /** Pool size / 池大小 */
  size: number
}

/**
 * Camera shake configuration
 * 相机抖动配置
 */
interface CameraShake {
  /** Whether shake is active / 抖动是否激活 */
  active: boolean
  /** Shake intensity / 抖动强度 */
  intensity: number
  /** Shake duration in milliseconds / 抖动持续时间（毫秒） */
  duration: number
  /** Start time of shake / 抖动开始时间 */
  startTime: number
  /** Original camera position / 相机原始位置 */
  originalPosition: THREE.Vector3
}

/**
 * EffectsManager class for managing particle effects and animations
 * EffectsManager 类，用于管理粒子特效和动画
 * 
 * @example
 * ```typescript
 * const effectsManager = new EffectsManager(scene)
 * 
 * // Trigger attack effect
 * effectsManager.attackHit(attackerPosition, targetPosition, 'fire', 5, 1000)
 * 
 * // Update every frame
 * function animate() {
 *   effectsManager.update(clock.getDelta())
 *   renderer.render(scene, camera)
 * }
 * 
 * // Cleanup when done
 * effectsManager.dispose()
 * ```
 */
export class EffectsManager {
  private scene: THREE.Scene
  private camera: THREE.Camera | null = null
  private particlePools: Map<string, ParticlePool> = new Map()
  private activeEffects: Map<string, EffectConfig> = new Map()
  private cameraShake: CameraShake = {
    active: false,
    intensity: 0,
    duration: 0,
    startTime: 0,
    originalPosition: new THREE.Vector3()
  }
  
  // Color mappings for Pokemon types
  // 宝可梦属性类型颜色映射
  private readonly typeColors: Map<string, THREE.Color> = new Map([
    ['normal', new THREE.Color(0xa8a878)],
    ['fire', new THREE.Color(0xf08030)],
    ['water', new THREE.Color(0x6890f0)],
    ['electric', new THREE.Color(0xf8d030)],
    ['grass', new THREE.Color(0x78c850)],
    ['ice', new THREE.Color(0x98d8d8)],
    ['fighting', new THREE.Color(0xc03028)],
    ['poison', new THREE.Color(0xa040a0)],
    ['ground', new THREE.Color(0xe0c068)],
    ['flying', new THREE.Color(0xa890f0)],
    ['psychic', new THREE.Color(0xf85888)],
    ['bug', new THREE.Color(0xa8b820)],
    ['rock', new THREE.Color(0xb8a038)],
    ['ghost', new THREE.Color(0x705898)],
    ['dragon', new THREE.Color(0x7038f8)],
    ['dark', new THREE.Color(0x705848)],
    ['steel', new THREE.Color(0xb8b8d0)],
    ['fairy', new THREE.Color(0xee99ac)]
  ])

  /**
   * Create an EffectsManager instance
   * 创建 EffectsManager 实例
   * 
   * @param scene - Three.js scene / Three.js 场景
   * @param camera - Optional camera for shake effects / 可选相机用于抖动效果
   */
  constructor(scene: THREE.Scene, camera?: THREE.Camera) {
    this.scene = scene
    this.camera = camera || null
    
    // Initialize particle pools for different effect types
    // 为不同特效类型初始化粒子池
    this.initializeParticlePools()
  }

  /**
   * Initialize particle pools for object reuse
   * 初始化粒子池以实现对象复用
   */
  private initializeParticlePools(): void {
    // Pool for general particles (attack, damage, heal, etc.)
    // 通用粒子池（攻击、伤害、治愈等）
    this.createParticlePool('general', 50)
    
    // Pool for status effect particles
    // 状态效果粒子池
    this.createParticlePool('status', 30)
    
    // Pool for terastallize particles
    // 太晶化粒子池
    this.createParticlePool('terastallize', 20)
    
    // Pool for faint particles
    // 倒下特效粒子池
    this.createParticlePool('faint', 10)
  }

  /**
   * Create a particle pool with specified size
   * 创建指定大小的粒子池
   * 
   * @param poolName - Name of the pool / 池名称
   * @param size - Number of particles in pool / 池中粒子数量
   */
  private createParticlePool(poolName: string, size: number): void {
    const available: THREE.Points[] = []
    
    for (let i = 0; i < size; i++) {
      const geometry = new THREE.BufferGeometry()
      const material = new THREE.PointsMaterial({
        size: 0.1,
        vertexColors: true,
        transparent: true,
        opacity: 1,
        blending: THREE.AdditiveBlending,
        depthWrite: false
      })
      
      // Create default positions (will be updated when used)
      // 创建默认位置（使用时将更新）
      const positions = new Float32Array(100 * 3) // 100 particles per effect
      const colors = new Float32Array(100 * 3)
      
      geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
      geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))
      
      const points = new THREE.Points(geometry, material)
      points.visible = false
      this.scene.add(points)
      available.push(points)
    }
    
    this.particlePools.set(poolName, {
      available,
      active: [],
      size
    })
  }

  /**
   * Get a particle system from the pool
   * 从池中获取粒子系统
   * 
   * @param poolName - Name of the pool / 池名称
   * @returns Particle system or null if pool is empty / 粒子系统或 null 如果池为空
   */
  private getParticleSystem(poolName: string): THREE.Points | null {
    const pool = this.particlePools.get(poolName)
    if (!pool || pool.available.length === 0) {
      console.warn(`Particle pool '${poolName}' is empty`)
      return null
    }
    
    const particleSystem = pool.available.pop()!
    pool.active.push(particleSystem)
    particleSystem.visible = true
    return particleSystem
  }

  /**
   * Return a particle system to the pool
   * 将粒子系统返回到池中
   * 
   * @param poolName - Name of the pool / 池名称
   * @param particleSystem - Particle system to return / 要返回的粒子系统
   */
  private returnParticleSystem(poolName: string, particleSystem: THREE.Points): void {
    const pool = this.particlePools.get(poolName)
    if (!pool) return
    
    // Remove from active list
    // 从活跃列表中移除
    const index = pool.active.indexOf(particleSystem)
    if (index > -1) {
      pool.active.splice(index, 1)
    }
    
    // Reset particle system
    // 重置粒子系统
    particleSystem.visible = false
    particleSystem.position.set(0, 0, 0)
    
    // Return to available pool
    // 返回到可用池
    pool.available.push(particleSystem)
  }

  /**
   * Create attack hit effect
   * 创建攻击命中特效
   * 
   * @param attackerPosition - Attacker position / 攻击者位置
   * @param targetPosition - Target position / 被攻击者位置
   * @param type - Attack type (fire, water, etc.) / 攻击类型（火、水等）
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public attackHit(
    attackerPosition: THREE.Vector3,
    targetPosition: THREE.Vector3,
    type: string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `attack_${Date.now()}`
    const color = this.typeColors.get(type) || new THREE.Color(0xffffff)
    
    // Create particle flow from attacker to target
    // 创建从攻击者到被攻击者的粒子流
    const particleSystem = this.getParticleSystem('general')
    if (!particleSystem) return
    
    // Calculate direction and distance
    // 计算方向和距离
    const direction = new THREE.Vector3().subVectors(targetPosition, attackerPosition)
    const distance = direction.length()
    direction.normalize()
    
    // Update particle positions for flow effect
    // 更新粒子位置以实现流动效果
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 10))
    
    for (let i = 0; i < particleCount; i++) {
      const t = i / particleCount
      const x = attackerPosition.x + direction.x * distance * t
      const y = attackerPosition.y + direction.y * distance * t
      const z = attackerPosition.z + direction.z * distance * t
      
      // Add some randomness
      // 添加一些随机性
      positions[i * 3] = x + (Math.random() - 0.5) * 0.5
      positions[i * 3 + 1] = y + (Math.random() - 0.5) * 0.5
      positions[i * 3 + 2] = z + (Math.random() - 0.5) * 0.5
      
      // Set color with slight variation
      // 设置颜色并添加轻微变化
      colors[i * 3] = color.r + (Math.random() - 0.5) * 0.1
      colors[i * 3 + 1] = color.g + (Math.random() - 0.5) * 0.1
      colors[i * 3 + 2] = color.b + (Math.random() - 0.5) * 0.1
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: targetPosition.clone(),
      color: color.clone()
    })
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'general')
    }, duration)
  }

  /**
   * Create damage hit effect
   * 创建受击特效
   * 
   * @param position - Target position / 目标位置
   * @param color - Main color / 主颜色
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public damageHit(
    position: THREE.Vector3,
    color: THREE.Color | string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `damage_${Date.now()}`
    const effectColor = typeof color === 'string' ? new THREE.Color(color) : color
    
    // Create flash effect at target position
    // 在目标位置创建闪光效果
    const particleSystem = this.getParticleSystem('general')
    if (!particleSystem) return
    
    // Create explosion effect
    // 创建爆炸效果
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 15))
    
    for (let i = 0; i < particleCount; i++) {
      // Random direction from center
      // 从中心随机方向
      const theta = Math.random() * Math.PI * 2
      const phi = Math.random() * Math.PI
      const r = Math.random() * intensity * 0.2
      
      positions[i * 3] = position.x + r * Math.sin(phi) * Math.cos(theta)
      positions[i * 3 + 1] = position.y + r * Math.sin(phi) * Math.sin(theta)
      positions[i * 3 + 2] = position.z + r * Math.cos(phi)
      
      // Flash white/red
      // 闪白/闪红
      const flashIntensity = Math.random()
      if (flashIntensity > 0.5) {
        // White flash
        // 白色闪光
        colors[i * 3] = 1
        colors[i * 3 + 1] = 1
        colors[i * 3 + 2] = 1
      } else {
        // Red flash
        // 红色闪光
        colors[i * 3] = effectColor.r
        colors[i * 3 + 1] = effectColor.g * 0.3
        colors[i * 3 + 2] = effectColor.b * 0.3
      }
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: position.clone(),
      color: effectColor.clone()
    })
    
    // Trigger camera shake
    // 触发相机抖动
    this.shakeCamera(intensity * 0.5, duration * 0.8)
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'general')
    }, duration)
  }

  /**
   * Create heal effect
   * 创建治愈特效
   * 
   * @param position - Target position / 目标位置
   * @param color - Main color / 主颜色
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public heal(
    position: THREE.Vector3,
    color: THREE.Color | string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `heal_${Date.now()}`
    const healColor = new THREE.Color(0x00ff00) // Green for healing
    const effectColor = typeof color === 'string' ? new THREE.Color(color) : color
    
    // Use provided color or default to green
    // 使用提供的颜色或默认为绿色
    const finalColor = effectColor.equals(new THREE.Color(0xffffff)) ? healColor : effectColor
    
    // Create rising particle effect
    // 创建上升粒子效果
    const particleSystem = this.getParticleSystem('general')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 8))
    
    for (let i = 0; i < particleCount; i++) {
      // Rising particles
      // 上升粒子
      const t = i / particleCount
      positions[i * 3] = position.x + (Math.random() - 0.5) * 0.5
      positions[i * 3 + 1] = position.y + t * intensity * 0.5
      positions[i * 3 + 2] = position.z + (Math.random() - 0.5) * 0.5
      
      // Green color with variation
      // 绿色并添加变化
      colors[i * 3] = finalColor.r * (0.8 + Math.random() * 0.2)
      colors[i * 3 + 1] = finalColor.g * (0.8 + Math.random() * 0.2)
      colors[i * 3 + 2] = finalColor.b * (0.8 + Math.random() * 0.2)
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Create cross symbol (simplified as two intersecting lines of particles)
    // 创建十字符号（简化为两条相交的粒子线）
    this.createHealCross(position, intensity)
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: position.clone(),
      color: finalColor.clone()
    })
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'general')
    }, duration)
  }

  /**
   * Create heal cross symbol
   * 创建治愈十字符号
   * 
   * @param position - Center position / 中心位置
   * @param intensity - Effect intensity / 特效强度
   */
  private createHealCross(position: THREE.Vector3, intensity: number): void {
    const crossParticle = this.getParticleSystem('general')
    if (!crossParticle) return
    
    const positions = crossParticle.geometry.attributes.position.array as Float32Array
    const colors = crossParticle.geometry.attributes.color.array as Float32Array
    const green = new THREE.Color(0x00ff00)
    
    // Create cross shape (40 particles for two lines)
    // 创建十字形状（40个粒子用于两条线）
    const particleCount = 40
    
    for (let i = 0; i < particleCount; i++) {
      const t = (i / particleCount) * 2 - 1 // -1 to 1
      
      if (i < particleCount / 2) {
        // Horizontal line
        // 水平线
        positions[i * 3] = position.x + t * intensity * 0.3
        positions[i * 3 + 1] = position.y + intensity * 0.2
        positions[i * 3 + 2] = position.z
      } else {
        // Vertical line
        // 垂直线
        positions[i * 3] = position.x
        positions[i * 3 + 1] = position.y + t * intensity * 0.3 + intensity * 0.2
        positions[i * 3 + 2] = position.z
      }
      
      colors[i * 3] = green.r
      colors[i * 3 + 1] = green.g
      colors[i * 3 + 2] = green.b
    }
    
    crossParticle.geometry.attributes.position.needsUpdate = true
    crossParticle.geometry.attributes.color.needsUpdate = true
    
    // Animate cross rotation
    // 动画十字旋转
    const startTime = Date.now()
    const animateCross = () => {
      const elapsed = Date.now() - startTime
      if (elapsed > 1000) {
        this.returnParticleSystem('general', crossParticle)
        return
      }
      
      crossParticle.rotation.y += 0.05
      requestAnimationFrame(animateCross)
    }
    
    requestAnimationFrame(animateCross)
  }

  /**
   * Create status effect
   * 创建状态效果
   * 
   * @param position - Target position / 目标位置
   * @param statusType - Type of status effect / 状态效果类型
   * @param color - Main color / 主颜色
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public statusEffect(
    position: THREE.Vector3,
    statusType: 'burn' | 'poison' | 'paralysis' | 'sleep' | 'freeze',
    color: THREE.Color | string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `status_${statusType}_${Date.now()}`
    const effectColor = typeof color === 'string' ? new THREE.Color(color) : color
    
    // Get status-specific color if not provided
    // 如果未提供则获取状态特定颜色
    const statusColors: Record<string, THREE.Color> = {
      burn: new THREE.Color(0xff4500),
      poison: new THREE.Color(0x9400d3),
      paralysis: new THREE.Color(0xffd700),
      sleep: new THREE.Color(0x8b4513),
      freeze: new THREE.Color(0x00bfff)
    }
    
    const finalColor = effectColor.equals(new THREE.Color(0xffffff)) ? 
      (statusColors[statusType] || new THREE.Color(0xffffff)) : effectColor
    
    // Create status-specific effect
    // 创建状态特定效果
    switch (statusType) {
      case 'burn':
        this.createBurnEffect(position, finalColor, intensity)
        break
      case 'poison':
        this.createPoisonEffect(position, finalColor, intensity)
        break
      case 'paralysis':
        this.createParalysisEffect(position, finalColor, intensity)
        break
      case 'sleep':
        this.createSleepEffect(position, finalColor, intensity)
        break
      case 'freeze':
        this.createFreezeEffect(position, finalColor, intensity)
        break
    }
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: position.clone(),
      color: finalColor.clone()
    })
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'status')
    }, duration)
  }

  /**
   * Create burn effect (fire particles)
   * 创建燃烧效果（火焰粒子）
   * 
   * @param position - Effect position / 特效位置
   * @param color - Effect color / 特效颜色
   * @param intensity - Effect intensity / 特效强度
   */
  private createBurnEffect(position: THREE.Vector3, color: THREE.Color, intensity: number): void {
    const particleSystem = this.getParticleSystem('status')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 12))
    
    for (let i = 0; i < particleCount; i++) {
      // Fire particles rising and swirling
      // 火焰粒子上升和旋转
      const t = i / particleCount
      const angle = t * Math.PI * 2 * 3 // Multiple rotations
      const radius = 0.3 + Math.random() * 0.2
      
      positions[i * 3] = position.x + Math.cos(angle) * radius
      positions[i * 3 + 1] = position.y + t * intensity * 0.4
      positions[i * 3 + 2] = position.z + Math.sin(angle) * radius
      
      // Fire colors (orange to yellow)
      // 火焰颜色（橙色到黄色）
      const heat = Math.random()
      colors[i * 3] = color.r * (0.8 + heat * 0.2)
      colors[i * 3 + 1] = color.g * (0.6 + heat * 0.4)
      colors[i * 3 + 2] = color.b * heat * 0.2
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate fire effect
    // 动画火焰效果
    this.animateParticles(particleSystem, 'status', 0.02)
  }

  /**
   * Create poison effect (purple bubbles)
   * 创建中毒效果（紫色气泡）
   * 
   * @param position - Effect position / 特效位置
   * @param color - Effect color / 特效颜色
   * @param intensity - Effect intensity / 特效强度
   */
  private createPoisonEffect(position: THREE.Vector3, color: THREE.Color, intensity: number): void {
    const particleSystem = this.getParticleSystem('status')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 10))
    
    for (let i = 0; i < particleCount; i++) {
      // Bubbles rising
      // 气泡上升
      positions[i * 3] = position.x + (Math.random() - 0.5) * 0.6
      positions[i * 3 + 1] = position.y + Math.random() * intensity * 0.5
      positions[i * 3 + 2] = position.z + (Math.random() - 0.5) * 0.6
      
      // Purple color
      // 紫色
      colors[i * 3] = color.r
      colors[i * 3 + 1] = color.g * 0.5
      colors[i * 3 + 2] = color.b
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate bubble effect
    // 动画气泡效果
    this.animateParticles(particleSystem, 'status', 0.01)
  }

  /**
   * Create paralysis effect (electric arcs)
   * 创建麻痹效果（电弧）
   * 
   * @param position - Effect position / 特效位置
   * @param color - Effect color / 特效颜色
   * @param intensity - Effect intensity / 特效强度
   */
  private createParalysisEffect(position: THREE.Vector3, color: THREE.Color, intensity: number): void {
    const particleSystem = this.getParticleSystem('status')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 15))
    
    // Create electric arc pattern
    // 创建电弧图案
    for (let i = 0; i < particleCount; i++) {
      const t = i / particleCount
      const arc = Math.sin(t * Math.PI * 4) * intensity * 0.2
      
      positions[i * 3] = position.x + arc
      positions[i * 3 + 1] = position.y + t * intensity * 0.3
      positions[i * 3 + 2] = position.z + (Math.random() - 0.5) * 0.3
      
      // Yellow/electric color
      // 黄色/电光色
      colors[i * 3] = color.r
      colors[i * 3 + 1] = color.g
      colors[i * 3 + 2] = color.b * 0.3
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate with flickering
    // 闪烁动画
    this.animateParticles(particleSystem, 'status', 0.05, true)
  }

  /**
   * Create sleep effect (floating Z characters)
   * 创建睡眠效果（飘浮的Z字符）
   * 
   * @param position - Effect position / 特效位置
   * @param color - Effect color / 特效颜色
   * @param intensity - Effect intensity / 特效强度
   */
  private createSleepEffect(position: THREE.Vector3, color: THREE.Color, intensity: number): void {
    const particleSystem = this.getParticleSystem('status')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    
    // Create Z-shaped pattern (simplified as points)
    // 创建Z形图案（简化为点）
    const zPoints = [
      // First Z
      [0, 0, 0], [0.1, 0, 0], [0.2, 0, 0],
      [0.2, 0.1, 0], [0.1, 0.1, 0], [0, 0.1, 0],
      [0, 0.2, 0], [0.1, 0.2, 0], [0.2, 0.2, 0],
      // Second Z (offset)
      [0.3, 0.3, 0], [0.4, 0.3, 0], [0.5, 0.3, 0],
      [0.5, 0.4, 0], [0.4, 0.4, 0], [0.3, 0.4, 0],
      [0.3, 0.5, 0], [0.4, 0.5, 0], [0.5, 0.5, 0]
    ]
    
    const scale = intensity * 0.1
    
    for (let i = 0; i < zPoints.length; i++) {
      const [x, y, z] = zPoints[i]
      
      positions[i * 3] = position.x + x * scale
      positions[i * 3 + 1] = position.y + y * scale + intensity * 0.2
      positions[i * 3 + 2] = position.z + z * scale
      
      // Brown/sleep color
      // 棕色/睡眠颜色
      colors[i * 3] = color.r
      colors[i * 3 + 1] = color.g
      colors[i * 3 + 2] = color.b
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate floating
    // 飘浮动画
    this.animateParticles(particleSystem, 'status', 0.005)
  }

  /**
   * Create freeze effect (ice crystals)
   * 创建冰冻效果（冰晶）
   * 
   * @param position - Effect position / 特效位置
   * @param color - Effect color / 特效颜色
   * @param intensity - Effect intensity / 特效强度
   */
  private createFreezeEffect(position: THREE.Vector3, color: THREE.Color, intensity: number): void {
    const particleSystem = this.getParticleSystem('status')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 10))
    
    // Create crystal pattern
    // 创建晶体图案
    for (let i = 0; i < particleCount; i++) {
      const t = i / particleCount
      const angle = t * Math.PI * 2
      const radius = 0.2 + Math.random() * 0.3
      
      // Hexagonal crystal shape
      // 六边形晶体形状
      positions[i * 3] = position.x + Math.cos(angle) * radius
      positions[i * 3 + 1] = position.y + Math.random() * intensity * 0.3
      positions[i * 3 + 2] = position.z + Math.sin(angle) * radius
      
      // Ice blue color
      // 冰蓝色
      colors[i * 3] = color.r * 0.8
      colors[i * 3 + 1] = color.g * 0.9
      colors[i * 3 + 2] = color.b
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate crystal effect
    // 动画晶体效果
    this.animateParticles(particleSystem, 'status', 0.01)
  }

  /**
   * Create terastallize effect
   * 创建太晶化特效
   * 
   * @param position - Target position / 目标位置
   * @param color - Main color / 主颜色
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public terastallize(
    position: THREE.Vector3,
    color: THREE.Color | string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `terastallize_${Date.now()}`
    const effectColor = typeof color === 'string' ? new THREE.Color(color) : color
    
    // Create crystal shards
    // 创建水晶碎片
    const particleSystem = this.getParticleSystem('terastallize')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 12))
    
    // Create rainbow crystal effect
    // 创建彩虹水晶效果
    for (let i = 0; i < particleCount; i++) {
      const t = i / particleCount
      const angle = t * Math.PI * 2 * 2 // Two rotations
      const radius = 0.5 + Math.random() * 0.3
      const height = t * intensity * 0.6
      
      positions[i * 3] = position.x + Math.cos(angle) * radius
      positions[i * 3 + 1] = position.y + height
      positions[i * 3 + 2] = position.z + Math.sin(angle) * radius
      
      // Rainbow colors based on position
      // 基于位置的彩虹颜色
      const hue = t * 360
      const crystalColor = new THREE.Color()
      crystalColor.setHSL(hue / 360, 0.8, 0.6)
      
      colors[i * 3] = crystalColor.r
      colors[i * 3 + 1] = crystalColor.g
      colors[i * 3 + 2] = crystalColor.b
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Create light burst effect
    // 创建光芒爆发效果
    this.createLightBurst(position, intensity)
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: position.clone(),
      color: effectColor.clone()
    })
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'terastallize')
    }, duration)
  }

  /**
   * Create light burst effect
   * 创建光芒爆发效果
   * 
   * @param position - Center position / 中心位置
   * @param intensity - Effect intensity / 特效强度
   */
  private createLightBurst(position: THREE.Vector3, intensity: number): void {
    const burstParticle = this.getParticleSystem('terastallize')
    if (!burstParticle) return
    
    const positions = burstParticle.geometry.attributes.position.array as Float32Array
    const colors = burstParticle.geometry.attributes.color.array as Float32Array
    const particleCount = 50
    
    for (let i = 0; i < particleCount; i++) {
      const t = i / particleCount
      const angle = t * Math.PI * 2
      const radius = t * intensity * 0.4
      
      positions[i * 3] = position.x + Math.cos(angle) * radius
      positions[i * 3 + 1] = position.y + intensity * 0.3
      positions[i * 3 + 2] = position.z + Math.sin(angle) * radius
      
      // White/golden light
      // 白色/金色光芒
      colors[i * 3] = 1
      colors[i * 3 + 1] = 0.9 + Math.random() * 0.1
      colors[i * 3 + 2] = 0.7 + Math.random() * 0.3
    }
    
    burstParticle.geometry.attributes.position.needsUpdate = true
    burstParticle.geometry.attributes.color.needsUpdate = true
    
    // Animate burst expansion
    // 动画爆发扩展
    const startTime = Date.now()
    const animateBurst = () => {
      const elapsed = Date.now() - startTime
      if (elapsed > 800) {
        this.returnParticleSystem('terastallize', burstParticle)
        return
      }
      
      burstParticle.scale.setScalar(1 + elapsed * 0.003)
      requestAnimationFrame(animateBurst)
    }
    
    requestAnimationFrame(animateBurst)
  }

  /**
   * Create faint effect
   * 创建倒下特效
   * 
   * @param position - Target position / 目标位置
   * @param color - Main color / 主颜色
   * @param intensity - Effect intensity (1-10) / 特效强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public faint(
    position: THREE.Vector3,
    color: THREE.Color | string,
    intensity: number,
    duration: number
  ): void {
    const effectId = `faint_${Date.now()}`
    const faintColor = new THREE.Color(0xffffff) // White for faint
    const effectColor = typeof color === 'string' ? new THREE.Color(color) : color
    
    // Use white color for faint effect
    // 使用白色作为倒下效果
    const finalColor = effectColor.equals(new THREE.Color(0xffffff)) ? faintColor : effectColor
    
    // Create dissolving particles
    // 创建消散粒子
    const particleSystem = this.getParticleSystem('faint')
    if (!particleSystem) return
    
    const positions = particleSystem.geometry.attributes.position.array as Float32Array
    const colors = particleSystem.geometry.attributes.color.array as Float32Array
    const particleCount = Math.min(100, Math.floor(intensity * 10))
    
    for (let i = 0; i < particleCount; i++) {
      // Particles rising and fading
      // 粒子上升并消失
      positions[i * 3] = position.x + (Math.random() - 0.5) * 0.5
      positions[i * 3 + 1] = position.y + Math.random() * intensity * 0.4
      positions[i * 3 + 2] = position.z + (Math.random() - 0.5) * 0.5
      
      // White color with slight blue tint
      // 白色带轻微蓝色色调
      colors[i * 3] = finalColor.r
      colors[i * 3 + 1] = finalColor.g
      colors[i * 3 + 2] = finalColor.b * 1.1
    }
    
    particleSystem.geometry.attributes.position.needsUpdate = true
    particleSystem.geometry.attributes.color.needsUpdate = true
    
    // Animate faint effect with fading
    // 带消失的倒下效果动画
    this.animateParticles(particleSystem, 'faint', 0.01, false, true)
    
    // Store effect configuration
    // 存储特效配置
    this.activeEffects.set(effectId, {
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      active: true,
      startTime: Date.now(),
      position: position.clone(),
      color: finalColor.clone()
    })
    
    // Schedule effect end
    // 计划特效结束
    setTimeout(() => {
      this.endEffect(effectId, 'faint')
    }, duration)
  }

  /**
   * Animate particles with various effects
   * 使用各种效果动画粒子
   * 
   * @param particleSystem - Particle system to animate / 要动画的粒子系统
   * @param poolName - Pool name for returning particles / 用于返回粒子的池名称
   * @param speed - Animation speed / 动画速度
   * @param flicker - Whether to add flickering effect / 是否添加闪烁效果
   * @param fade - Whether to add fade effect / 是否添加消失效果
   */
  private animateParticles(
    particleSystem: THREE.Points,
    poolName: string,
    speed: number,
    flicker: boolean = false,
    fade: boolean = false
  ): void {
    const startTime = Date.now()
    const duration = 2000 // 2 seconds animation
    const material = particleSystem.material as THREE.PointsMaterial
    
    const animate = () => {
      const elapsed = Date.now() - startTime
      
      if (elapsed > duration) {
        this.returnParticleSystem(poolName, particleSystem)
        return
      }
      
      const progress = elapsed / duration
      
      // Apply fade effect
      // 应用消失效果
      if (fade) {
        material.opacity = 1 - progress
      }
      
      // Apply flickering effect
      // 应用闪烁效果
      if (flicker) {
        material.opacity = 0.5 + Math.sin(elapsed * 0.01) * 0.5
      }
      
      // Move particles
      // 移动粒子
      particleSystem.position.y += speed
      
      // Rotate particles
      // 旋转粒子
      particleSystem.rotation.y += 0.02
      
      requestAnimationFrame(animate)
    }
    
    requestAnimationFrame(animate)
  }

  /**
   * Trigger camera shake effect
   * 触发相机抖动效果
   * 
   * @param intensity - Shake intensity (1-10) / 抖动强度（1-10）
   * @param duration - Duration in milliseconds / 持续时间（毫秒）
   */
  public shakeCamera(intensity: number, duration: number): void {
    if (!this.camera) {
      console.warn('Camera not set for shake effect')
      return
    }
    
    this.cameraShake = {
      active: true,
      intensity: Math.max(1, Math.min(10, intensity)),
      duration,
      startTime: Date.now(),
      originalPosition: this.camera.position.clone()
    }
  }

  /**
   * Update camera shake effect
   * 更新相机抖动效果
   */
  private updateCameraShake(): void {
    if (!this.cameraShake.active || !this.camera) return
    
    const elapsed = Date.now() - this.cameraShake.startTime
    const progress = elapsed / this.cameraShake.duration
    
    if (progress >= 1) {
      // Reset camera position
      // 重置相机位置
      this.camera.position.copy(this.cameraShake.originalPosition)
      this.cameraShake.active = false
      return
    }
    
    // Calculate shake offset
    // 计算抖动偏移
    const decay = 1 - progress
    const shakeIntensity = this.cameraShake.intensity * 0.01 * decay
    
    const offsetX = (Math.random() - 0.5) * shakeIntensity
    const offsetY = (Math.random() - 0.5) * shakeIntensity
    const offsetZ = (Math.random() - 0.5) * shakeIntensity
    
    // Apply shake to camera
    // 将抖动应用到相机
    this.camera.position.set(
      this.cameraShake.originalPosition.x + offsetX,
      this.cameraShake.originalPosition.y + offsetY,
      this.cameraShake.originalPosition.z + offsetZ
    )
  }

  /**
   * End an effect and return particles to pool
   * 结束特效并将粒子返回到池中
   * 
   * @param effectId - Effect identifier / 特效标识符
   * @param poolName - Pool name / 池名称
   */
  private endEffect(effectId: string, poolName: string): void {
    const effect = this.activeEffects.get(effectId)
    if (!effect) return
    
    effect.active = false
    this.activeEffects.delete(effectId)
  }

  /**
   * Update all active effects (call this every frame)
   * 更新所有活跃特效（每帧调用此方法）
   * 
   * @param delta - Time delta in seconds / 时间增量（秒）
   */
  public update(delta: number): void {
    // Update camera shake
    // 更新相机抖动
    this.updateCameraShake()
    
    // Update all active effects
    // 更新所有活跃特效
    this.activeEffects.forEach((effect, effectId) => {
      if (!effect.active) return
      
      const elapsed = Date.now() - effect.startTime
      const progress = elapsed / effect.duration
      
      if (progress >= 1) {
        this.activeEffects.delete(effectId)
      }
    })
  }

  /**
   * Set camera for shake effects
   * 设置相机用于抖动效果
   * 
   * @param camera - Three.js camera / Three.js 相机
   */
  public setCamera(camera: THREE.Camera): void {
    this.camera = camera
  }

  /**
   * Dispose all resources and clean up
   * 销毁所有资源并清理
   */
  public dispose(): void {
    // Return all active particles to pools
    // 将所有活跃粒子返回到池中
    this.particlePools.forEach((pool, poolName) => {
      pool.active.forEach(particleSystem => {
        particleSystem.visible = false
        particleSystem.geometry.dispose()
        if (particleSystem.material instanceof THREE.Material) {
          particleSystem.material.dispose()
        }
        this.scene.remove(particleSystem)
      })
      
      pool.available.forEach(particleSystem => {
        particleSystem.geometry.dispose()
        if (particleSystem.material instanceof THREE.Material) {
          particleSystem.material.dispose()
        }
        this.scene.remove(particleSystem)
      })
    })
    
    // Clear pools
    // 清空池
    this.particlePools.clear()
    
    // Clear active effects
    // 清空活跃特效
    this.activeEffects.clear()
    
    // Reset camera shake
    // 重置相机抖动
    this.cameraShake.active = false
    
    console.log('EffectsManager disposed')
  }
}