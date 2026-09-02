/**
 * @description Pokemon 3D model management module
 * @description 宝可梦 3D 模型管理模块
 * 
 * @author MiMo-v2.5-pro
 * @version 1.0.0
 * 
 * @description Responsible for creating and managing Pokemon 3D models
 * @description 负责创建和管理宝可梦 3D 模型
 * 
 * @description Since there are no glTF models initially, use geometry combinations to represent Pokemon
 * @description 由于初期没有 glTF 模型，使用几何体组合来代表宝可梦
 */

import * as THREE from 'three'

/**
 * Pokemon type to color mapping
 * 宝可梦属性类型到颜色的映射
 */
export const typeColorMap: Record<string, string> = {
  普通: '#A8A77A',
  火: '#EE8130',
  水: '#6390F0',
  电: '#F7D02C',
  草: '#7AC74C',
  冰: '#96D9D6',
  格斗: '#C22E28',
  毒: '#A33EA1',
  地面: '#E2BF65',
  飞行: '#A98FF3',
  超能: '#F95587',
  虫: '#A6B91A',
  岩石: '#B6A136',
  幽灵: '#735797',
  龙: '#6F35FC',
  恶: '#705746',
  钢: '#B7B7CE',
  妖精: '#D685AD'
}

/**
 * Animation types
 * 动画类型
 */
export type AnimationType = 'idle' | 'attack' | 'hit' | 'faint' | 'heal'

/**
 * Animation state
 * 动画状态
 */
interface AnimationState {
  /** Animation type / 动画类型 */
  type: AnimationType
  /** Start time / 开始时间 */
  startTime: number
  /** Animation duration / 动画持续时间 */
  duration: number
  /** Whether animation is playing / 动画是否正在播放 */
  isPlaying: boolean
  /** Original position / 原始位置 */
  originalPosition: THREE.Vector3
  /** Original scale / 原始缩放 */
  originalScale: THREE.Vector3
  /** Original rotation / 原始旋转 */
  originalRotation: THREE.Euler
}

/**
 * Pokemon entity configuration
 * 宝可梦实体配置
 */
export interface PokemonConfig {
  /** Pokemon name / 宝可梦名称 */
  name: string
  /** Pokemon type (e.g., '火', '水') / 宝可梦属性类型 */
  type: string
  /** Current HP / 当前生命值 */
  currentHp: number
  /** Max HP / 最大生命值 */
  maxHp: number
  /** Model size scale / 模型大小缩放 */
  scale?: number
}

/**
 * PokemonEntity class representing a Pokemon on the battlefield
 * PokemonEntity 类，代表一个战场上的宝可梦实体
 * 
 * @example
 * ```typescript
 * const pokemon = new PokemonEntity({
 *   name: '皮卡丘',
 *   type: '电',
 *   currentHp: 100,
 *   maxHp: 100
 * })
 * 
 * pokemon.setPosition(0, 0, 0)
 * pokemon.playAnimation('attack', 1000)
 * ```
 */
export class PokemonEntity {
  /** Three.js group containing all model parts / 包含所有模型部件的 Three.js 组 */
  public group: THREE.Group
  
  /** Pokemon name / 宝可梦名称 */
  private name: string
  
  /** Pokemon type / 宝可梦属性类型 */
  private type: string
  
  /** Current HP / 当前生命值 */
  private currentHp: number
  
  /** Max HP / 最大生命值 */
  private maxHp: number
  
  /** Model scale / 模型缩放 */
  private scale: number
  
  /** Body mesh / 身体网格 */
  private body: THREE.Mesh
  
  /** Head mesh / 头部网格 */
  private head: THREE.Mesh
  
  /** Left eye white / 左眼白 */
  private leftEyeWhite: THREE.Mesh
  
  /** Right eye white / 右眼白 */
  private rightEyeWhite: THREE.Mesh
  
  /** Left pupil / 左瞳孔 */
  private leftPupil: THREE.Mesh
  
  /** Right pupil / 右瞳孔 */
  private rightPupil: THREE.Mesh
  
  /** Type indicator ring / 属性指示光环 */
  private typeRing: THREE.Mesh
  
  /** HP bar sprite / 血条精灵 */
  private hpBar: THREE.Sprite
  
  /** HP bar canvas for texture / 血条画布用于纹理 */
  private hpBarCanvas: HTMLCanvasElement
  
  /** HP bar context / 血条上下文 */
  private hpBarCtx: CanvasRenderingContext2D
  
  /** Name tag sprite / 名字标签精灵 */
  private nameTag: THREE.Sprite
  
  /** Highlight state / 高亮状态 */
  private isHighlighted: boolean = false
  
  /** Original materials for highlight effect / 用于高亮效果的原始材质 */
  private originalMaterials: { [key: string]: THREE.Material | THREE.Material[] } = {}
  
  /** Highlight material / 高亮材质 */
  private highlightMaterial: THREE.MeshStandardMaterial
  
  /** Animation state / 动画状态 */
  private animationState: AnimationState | null = null
  
  /** Animation frame ID / 动画帧 ID */
  private animationFrameId: number | null = null
  
  /** Idle animation offset / 待机动画偏移 */
  private idleOffset: number = 0
  
  /** Whether Pokemon has fainted / 宝可梦是否已倒下 */
  private isFainted: boolean = false
  
  /** Heal animation particles / 治愈动画粒子 */
  private healParticles: THREE.Points | null = null
  
  /** Hit flash timer / 受击闪红计时器 */
  private hitFlashTimer: number = 0

  /**
   * Create a Pokemon entity
   * 创建一个宝可梦实体
   * 
   * @param config - Pokemon configuration / 宝可梦配置
   */
  constructor(config: PokemonConfig) {
    this.name = config.name
    this.type = config.type
    this.currentHp = config.currentHp
    this.maxHp = config.maxHp
    this.scale = config.scale || 1
    
    // Create group
    // 创建组
    this.group = new THREE.Group()
    this.group.name = `pokemon_${config.name}`
    
    // Get type color
    // 获取属性颜色
    const typeColor = typeColorMap[config.type] || '#A8A77A'
    
    // Create body
    // 创建身体
    const bodyGeometry = new THREE.CapsuleGeometry(0.5, 1, 8, 16)
    const bodyMaterial = new THREE.MeshStandardMaterial({
      color: new THREE.Color(typeColor),
      roughness: 0.6,
      metalness: 0.1
    })
    this.body = new THREE.Mesh(bodyGeometry, bodyMaterial)
    this.body.castShadow = true
    this.body.receiveShadow = true
    this.body.name = 'body'
    this.group.add(this.body)
    
    // Create head
    // 创建头部
    const headGeometry = new THREE.SphereGeometry(0.35, 16, 16)
    const headMaterial = new THREE.MeshStandardMaterial({
      color: new THREE.Color(typeColor),
      roughness: 0.6,
      metalness: 0.1
    })
    this.head = new THREE.Mesh(headGeometry, headMaterial)
    this.head.position.set(0, 0.8, 0)
    this.head.castShadow = true
    this.head.name = 'head'
    this.group.add(this.head)
    
    // Create eyes
    // 创建眼睛
    const eyeWhiteGeometry = new THREE.SphereGeometry(0.1, 8, 8)
    const eyeWhiteMaterial = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      roughness: 0.3,
      metalness: 0.0
    })
    
    // Left eye white
    // 左眼白
    this.leftEyeWhite = new THREE.Mesh(eyeWhiteGeometry, eyeWhiteMaterial)
    this.leftEyeWhite.position.set(-0.15, 0.85, 0.3)
    this.leftEyeWhite.name = 'leftEyeWhite'
    this.group.add(this.leftEyeWhite)
    
    // Right eye white
    // 右眼白
    this.rightEyeWhite = new THREE.Mesh(eyeWhiteGeometry, eyeWhiteMaterial)
    this.rightEyeWhite.position.set(0.15, 0.85, 0.3)
    this.rightEyeWhite.name = 'rightEyeWhite'
    this.group.add(this.rightEyeWhite)
    
    // Pupils
    // 瞳孔
    const pupilGeometry = new THREE.SphereGeometry(0.05, 8, 8)
    const pupilMaterial = new THREE.MeshStandardMaterial({
      color: 0x000000,
      roughness: 0.2,
      metalness: 0.0
    })
    
    // Left pupil
    // 左瞳孔
    this.leftPupil = new THREE.Mesh(pupilGeometry, pupilMaterial)
    this.leftPupil.position.set(-0.15, 0.85, 0.38)
    this.leftPupil.name = 'leftPupil'
    this.group.add(this.leftPupil)
    
    // Right pupil
    // 右瞳孔
    this.rightPupil = new THREE.Mesh(pupilGeometry, pupilMaterial)
    this.rightPupil.position.set(0.15, 0.85, 0.38)
    this.rightPupil.name = 'rightPupil'
    this.group.add(this.rightPupil)
    
    // Create type indicator ring
    // 创建属性指示光环
    const ringGeometry = new THREE.TorusGeometry(0.6, 0.05, 8, 32)
    const ringMaterial = new THREE.MeshStandardMaterial({
      color: new THREE.Color(typeColor),
      emissive: new THREE.Color(typeColor),
      emissiveIntensity: 0.5,
      roughness: 0.3,
      metalness: 0.5
    })
    this.typeRing = new THREE.Mesh(ringGeometry, ringMaterial)
    this.typeRing.position.set(0, 1.3, 0)
    this.typeRing.rotation.x = Math.PI / 2
    this.typeRing.name = 'typeRing'
    this.group.add(this.typeRing)
    
    // Create HP bar
    // 创建血条
    this.hpBarCanvas = document.createElement('canvas')
    this.hpBarCanvas.width = 256
    this.hpBarCanvas.height = 32
    this.hpBarCtx = this.hpBarCanvas.getContext('2d')!
    
    const hpBarTexture = new THREE.CanvasTexture(this.hpBarCanvas)
    const hpBarMaterial = new THREE.SpriteMaterial({
      map: hpBarTexture,
      transparent: true
    })
    this.hpBar = new THREE.Sprite(hpBarMaterial)
    this.hpBar.position.set(0, 1.8, 0)
    this.hpBar.scale.set(1.5, 0.2, 1)
    this.hpBar.name = 'hpBar'
    this.group.add(this.hpBar)
    
    // Draw initial HP bar
    // 绘制初始血条
    this.drawHpBar()
    
    // Create name tag
    // 创建名字标签
    this.nameTag = this.createNameTag(config.name)
    this.nameTag.position.set(0, 2.1, 0)
    this.nameTag.name = 'nameTag'
    this.group.add(this.nameTag)
    
    // Create highlight material
    // 创建高亮材质
    this.highlightMaterial = new THREE.MeshStandardMaterial({
      color: 0x00ff00,
      emissive: 0x00ff00,
      emissiveIntensity: 0.3,
      roughness: 0.4,
      metalness: 0.5
    })
    
    // Store original materials
    // 存储原始材质
    this.group.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        this.originalMaterials[child.uuid] = child.material
      }
    })
    
    // Apply scale
    // 应用缩放
    this.group.scale.set(this.scale, this.scale, this.scale)
    
    // Start idle animation
    // 开始待机动画
    this.startIdleAnimation()
  }

  /**
   * Create name tag sprite
   * 创建名字标签精灵
   * 
   * @param name - Pokemon name / 宝可梦名称
   * @returns Sprite with name text / 带有名字文字的精灵
   */
  private createNameTag(name: string): THREE.Sprite {
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 64
    const ctx = canvas.getContext('2d')!
    
    // Draw background
    // 绘制背景
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)'
    ctx.roundRect(0, 0, 256, 64, 10)
    ctx.fill()
    
    // Draw text
    // 绘制文字
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 32px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(name, 128, 32)
    
    const texture = new THREE.CanvasTexture(canvas)
    const material = new THREE.SpriteMaterial({
      map: texture,
      transparent: true
    })
    
    const sprite = new THREE.Sprite(material)
    sprite.scale.set(2, 0.5, 1)
    
    return sprite
  }

  /**
   * Draw HP bar on canvas
   * 在画布上绘制血条
   */
  private drawHpBar(): void {
    const ctx = this.hpBarCtx
    const width = this.hpBarCanvas.width
    const height = this.hpBarCanvas.height
    
    // Clear canvas
    // 清除画布
    ctx.clearRect(0, 0, width, height)
    
    // Draw background
    // 绘制背景
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)'
    ctx.roundRect(0, 0, width, height, 8)
    ctx.fill()
    
    // Draw HP bar background
    // 绘制血条背景
    ctx.fillStyle = '#333333'
    ctx.roundRect(10, 8, width - 20, height - 16, 4)
    ctx.fill()
    
    // Calculate HP percentage
    // 计算生命值百分比
    const hpPercentage = this.currentHp / this.maxHp
    
    // Determine HP bar color
    // 确定血条颜色
    let hpColor: string
    if (hpPercentage > 0.5) {
      hpColor = '#4caf50' // Green / 绿色
    } else if (hpPercentage > 0.2) {
      hpColor = '#ff9800' // Orange / 橙色
    } else {
      hpColor = '#f44336' // Red / 红色
    }
    
    // Draw HP bar
    // 绘制血条
    ctx.fillStyle = hpColor
    ctx.roundRect(10, 8, (width - 20) * hpPercentage, height - 16, 4)
    ctx.fill()
    
    // Draw HP text
    // 绘制生命值文字
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 16px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${this.currentHp}/${this.maxHp}`, width / 2, height / 2)
    
    // Update texture
    // 更新纹理
    if (this.hpBar.material instanceof THREE.SpriteMaterial) {
      const material = this.hpBar.material as THREE.SpriteMaterial
      if (material.map) {
        material.map.needsUpdate = true
      }
    }
  }

  /**
   * Start idle animation
   * 开始待机动画
   */
  private startIdleAnimation(): void {
    const animate = () => {
      if (this.isFainted) return
      
      this.idleOffset += 0.02
      
      // Float up and down
      // 上下浮动
      this.group.position.y += Math.sin(this.idleOffset) * 0.002
      
      // Slight rotation
      // 微旋转
      this.group.rotation.y += 0.005
      
      // Animate type ring
      // 动画属性光环
      this.typeRing.rotation.z += 0.01
      
      this.animationFrameId = requestAnimationFrame(animate)
    }
    
    animate()
  }

  /**
   * Set position
   * 设置位置
   * 
   * @param x - X coordinate / X 坐标
   * @param y - Y coordinate / Y 坐标
   * @param z - Z coordinate / Z 坐标
   */
  public setPosition(x: number, y: number, z: number): void {
    this.group.position.set(x, y, z)
  }

  /**
   * Play animation
   * 播放动画
   * 
   * @param type - Animation type / 动画类型
   * @param duration - Animation duration in milliseconds / 动画持续时间（毫秒）
   */
  public playAnimation(type: AnimationType, duration: number = 1000): void {
    // Stop current animation
    // 停止当前动画
    if (this.animationState?.isPlaying) {
      this.stopAnimation()
    }
    
    // Store original state
    // 存储原始状态
    this.animationState = {
      type,
      startTime: performance.now(),
      duration,
      isPlaying: true,
      originalPosition: this.group.position.clone(),
      originalScale: this.group.scale.clone(),
      originalRotation: this.group.rotation.clone()
    }
    
    // Start animation
    // 开始动画
    this.animate()
  }

  /**
   * Animate based on type
   * 根据类型进行动画
   */
  private animate(): void {
    if (!this.animationState?.isPlaying) return
    
    const elapsed = performance.now() - this.animationState.startTime
    const progress = Math.min(elapsed / this.animationState.duration, 1)
    
    switch (this.animationState.type) {
      case 'idle':
        this.animateIdle(progress)
        break
      case 'attack':
        this.animateAttack(progress)
        break
      case 'hit':
        this.animateHit(progress)
        break
      case 'faint':
        this.animateFaint(progress)
        break
      case 'heal':
        this.animateHeal(progress)
        break
    }
    
    // Continue animation if not complete
    // 如果动画未完成则继续
    if (progress < 1) {
      requestAnimationFrame(() => this.animate())
    } else {
      // Animation complete
      // 动画完成
      this.animationState.isPlaying = false
      
      // Reset to original state
      // 重置到原始状态
      if (this.animationState.type !== 'faint') {
        this.group.position.copy(this.animationState.originalPosition)
        this.group.scale.copy(this.animationState.originalScale)
        this.group.rotation.copy(this.animationState.originalRotation)
      }
    }
  }

  /**
   * Idle animation
   * 待机动画
   * 
   * @param progress - Animation progress (0-1) / 动画进度（0-1）
   */
  private animateIdle(progress: number): void {
    // Float up and down
    // 上下浮动
    const floatOffset = Math.sin(progress * Math.PI * 2) * 0.1
    this.group.position.y = this.animationState!.originalPosition.y + floatOffset
    
    // Slight rotation
    // 微旋转
    this.group.rotation.y = this.animationState!.originalRotation.y + progress * Math.PI * 2
  }

  /**
   * Attack animation
   * 攻击动画
   * 
   * @param progress - Animation progress (0-1) / 动画进度（0-1）
   */
  private animateAttack(progress: number): void {
    // Dash forward
    // 向前冲刺
    const dashDistance = Math.sin(progress * Math.PI) * 2
    this.group.position.z = this.animationState!.originalPosition.z - dashDistance
    
    // Scale up then down
    // 先放大后缩小
    const scaleMultiplier = 1 + Math.sin(progress * Math.PI) * 0.3
    this.group.scale.set(
      this.animationState!.originalScale.x * scaleMultiplier,
      this.animationState!.originalScale.y * scaleMultiplier,
      this.animationState!.originalScale.z * scaleMultiplier
    )
  }

  /**
   * Hit animation
   * 受击动画
   * 
   * @param progress - Animation progress (0-1) / 动画进度（0-1）
   */
  private animateHit(progress: number): void {
    // Shake effect
    // 抖动效果
    const shakeIntensity = Math.sin(progress * Math.PI * 4) * 0.2
    this.group.position.x = this.animationState!.originalPosition.x + shakeIntensity
    
    // Flash red
    // 闪红
    if (progress < 0.3) {
      this.setBodyColor(new THREE.Color(0xff0000))
    } else {
      this.setBodyColor(new THREE.Color(typeColorMap[this.type] || '#A8A77A'))
    }
  }

  /**
   * Faint animation
   * 倒下动画
   * 
   * @param progress - Animation progress (0-1) / 动画进度（0-1）
   */
  private animateFaint(progress: number): void {
    // Rotate and fall
    // 旋转倒下
    this.group.rotation.x = progress * Math.PI / 2
    
    // Move down
    // 向下移动
    this.group.position.y = this.animationState!.originalPosition.y - progress * 0.5
    
    // Fade out
    // 透明度降低
    this.group.traverse((child) => {
      if (child instanceof THREE.Mesh && child.material instanceof THREE.MeshStandardMaterial) {
        child.material.transparent = true
        child.material.opacity = 1 - progress
      }
    })
    
    // Mark as fainted at end
    // 结束时标记为已倒下
    if (progress >= 1) {
      this.isFainted = true
    }
  }

  /**
   * Heal animation
   * 治愈动画
   * 
   * @param progress - Animation progress (0-1) / 动画进度（0-1）
   */
  private animateHeal(progress: number): void {
    // Green glow pulse
    // 绿色光晕脉冲
    const glowIntensity = Math.sin(progress * Math.PI * 3) * 0.5 + 0.5
    
    // Create temporary green glow
    // 创建临时绿色光晕
    if (!this.healParticles) {
      const geometry = new THREE.BufferGeometry()
      const positions = new Float32Array(50 * 3)
      
      for (let i = 0; i < 50; i++) {
        positions[i * 3] = (Math.random() - 0.5) * 2
        positions[i * 3 + 1] = Math.random() * 2
        positions[i * 3 + 2] = (Math.random() - 0.5) * 2
      }
      
      geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
      
      const material = new THREE.PointsMaterial({
        color: 0x00ff00,
        size: 0.1,
        transparent: true,
        opacity: 0.8
      })
      
      this.healParticles = new THREE.Points(geometry, material)
      this.group.add(this.healParticles)
    }
    
    // Animate particles
    // 动画粒子
    if (this.healParticles) {
      this.healParticles.rotation.y += 0.05
      const material = this.healParticles.material as THREE.PointsMaterial
      material.opacity = glowIntensity * 0.8
    }
    
    // Pulse body color
    // 脉冲身体颜色
    const greenIntensity = glowIntensity * 0.5
    this.setBodyColor(new THREE.Color(greenIntensity, 1, greenIntensity))
    
    // Clean up at end
    // 结束时清理
    if (progress >= 1 && this.healParticles) {
      this.group.remove(this.healParticles)
      this.healParticles.geometry.dispose()
      ;(this.healParticles.material as THREE.PointsMaterial).dispose()
      this.healParticles = null
      
      // Restore original color
      // 恢复原始颜色
      this.setBodyColor(new THREE.Color(typeColorMap[this.type] || '#A8A77A'))
    }
  }

  /**
   * Set body color
   * 设置身体颜色
   * 
   * @param color - Color to set / 要设置的颜色
   */
  private setBodyColor(color: THREE.Color): void {
    if (this.body.material instanceof THREE.MeshStandardMaterial) {
      this.body.material.color.copy(color)
    }
    if (this.head.material instanceof THREE.MeshStandardMaterial) {
      this.head.material.color.copy(color)
    }
  }

  /**
   * Stop current animation
   * 停止当前动画
   */
  private stopAnimation(): void {
    if (this.animationState) {
      this.animationState.isPlaying = false
      
      // Reset to original state
      // 重置到原始状态
      this.group.position.copy(this.animationState.originalPosition)
      this.group.scale.copy(this.animationState.originalScale)
      this.group.rotation.copy(this.animationState.originalRotation)
      
      // Clean up heal particles if any
      // 清理治愈粒子（如果有）
      if (this.healParticles) {
        this.group.remove(this.healParticles)
        this.healParticles.geometry.dispose()
        ;(this.healParticles.material as THREE.PointsMaterial).dispose()
        this.healParticles = null
      }
      
      // Restore body color
      // 恢复身体颜色
      this.setBodyColor(new THREE.Color(typeColorMap[this.type] || '#A8A77A'))
      
      this.animationState = null
    }
  }

  /**
   * Update HP bar
   * 更新头顶血条
   * 
   * @param currentHp - Current HP / 当前生命值
   * @param maxHp - Max HP / 最大生命值
   */
  public updateHpBar(currentHp: number, maxHp: number): void {
    this.currentHp = currentHp
    this.maxHp = maxHp
    this.drawHpBar()
  }

  /**
   * Set name tag
   * 设置名字标签
   * 
   * @param name - Pokemon name / 宝可梦名称
   */
  public setNameTag(name: string): void {
    this.name = name
    
    // Remove old name tag
    // 移除旧名字标签
    this.group.remove(this.nameTag)
    if (this.nameTag.material instanceof THREE.SpriteMaterial) {
      if (this.nameTag.material.map) {
        this.nameTag.material.map.dispose()
      }
      this.nameTag.material.dispose()
    }
    
    // Create new name tag
    // 创建新名字标签
    this.nameTag = this.createNameTag(name)
    this.nameTag.position.set(0, 2.1, 0)
    this.nameTag.name = 'nameTag'
    this.group.add(this.nameTag)
  }

  /**
   * Set highlighted state
   * 设置高亮状态
   * 
   * @param highlighted - Whether to highlight / 是否高亮
   */
  public setHighlighted(highlighted: boolean): void {
    this.isHighlighted = highlighted
    
    this.group.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        if (highlighted) {
          // Apply highlight material
          // 应用高亮材质
          child.material = this.highlightMaterial
        } else {
          // Restore original material
          // 恢复原始材质
          const originalMaterial = this.originalMaterials[child.uuid]
          if (originalMaterial) {
            child.material = originalMaterial
          }
        }
      }
    })
  }

  /**
   * Dispose all resources
   * 清理所有资源
   */
  public dispose(): void {
    // Stop animations
    // 停止动画
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
      this.animationFrameId = null
    }
    
    this.stopAnimation()
    
    // Dispose geometries and materials
    // 销毁几何体和材质
    this.group.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry.dispose()
        if (Array.isArray(child.material)) {
          child.material.forEach(material => material.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
    
    // Dispose HP bar
    // 销毁血条
    if (this.hpBar.material instanceof THREE.SpriteMaterial) {
      if (this.hpBar.material.map) {
        this.hpBar.material.map.dispose()
      }
      this.hpBar.material.dispose()
    }
    
    // Dispose name tag
    // 销毁名字标签
    if (this.nameTag.material instanceof THREE.SpriteMaterial) {
      if (this.nameTag.material.map) {
        this.nameTag.material.map.dispose()
      }
      this.nameTag.material.dispose()
    }
    
    // Dispose highlight material
    // 销毁高亮材质
    this.highlightMaterial.dispose()
    
    // Clear maps
    // 清除映射
    this.originalMaterials = {}
    
    // Remove from parent
    // 从父对象移除
    if (this.group.parent) {
      this.group.parent.remove(this.group)
    }
  }
}