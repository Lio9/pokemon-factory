/**
 * PokemonModel.ts - HD-2D 风格宝可梦模型
 *
 * 使用 Sprite 实现 2D 精灵始终面向相机的效果，
 * 类似八方旅人 (Octopath Traveler) 的 HD-2D 风格。
 *
 * 特性：
 * - 2D 精灵始终面向相机（Sprite 自动实现）
 * - 带阴影和漂浮动画
 * - 血条和名字标签
 * - 攻击/受击/治愈动画
 *
 * @module PokemonModel
 */

import * as THREE from 'three'

/**
 * 精灵图 URL 生成
 * Sprite URL generator
 */
const SPRITE_BASE = '/api/pokedex/images/pokemon'

function getSpriteUrl(pokemonId: number | string): string {
  return `${SPRITE_BASE}/${pokemonId}.png`
}

/**
 * 属性颜色映射
 * Type color mapping
 */
const TYPE_COLORS: Record<string, string> = {
  Normal: '#A8A77A', Fire: '#EE8130', Water: '#6390F0', Electric: '#F7D02C',
  Grass: '#7AC74C', Ice: '#96D9D6', Fighting: '#C22E28', Poison: '#A33EA1',
  Ground: '#E2BF65', Flying: '#A98FF3', Psychic: '#F95587', Bug: '#A6B91A',
  Rock: '#B6A136', Ghost: '#735797', Dragon: '#6F35FC', Dark: '#705746',
  Steel: '#B7B7CE', Fairy: '#D685AD'
}

/**
 * 宝可梦配置
 * Pokemon configuration
 */
export interface PokemonConfig {
  /** Pokemon ID (for sprite) / 宝可梦 ID（用于精灵图） */
  id?: number | string
  /** Pokemon name / 宝可梦名称 */
  name: string
  /** Pokemon type / 属性类型 */
  type: string
  /** Current HP / 当前 HP */
  currentHp: number
  /** Max HP / 最大 HP */
  maxHp: number
  /** Sprite scale / 精灵缩放 */
  scale?: number
}

/**
 * HD-2D 风格宝可梦实体
 * HD-2D style Pokemon entity
 *
 * 使用 Sprite 实现 2D 精灵始终面向相机。
 * 血条和名字标签也是 Sprite，始终保持正面朝向。
 */
export class PokemonEntity {
  /** 主组 / Main group */
  public group: THREE.Group

  /** Pokemon name */
  private name: string
  /** Pokemon type */
  private type: string
  /** Pokemon ID for sprite */
  private pokemonId: number | string
  /** Current HP */
  private currentHp: number
  /** Max HP */
  private maxHp: number
  /** Scale */
  private scale: number

  /** Main sprite (2D image) / 主精灵（2D 图片） */
  private sprite: THREE.Sprite
  /** Sprite material */
  private spriteMaterial: THREE.SpriteMaterial
  /** Shadow sprite / 阴影精灵 */
  private shadowSprite: THREE.Sprite
  /** HP bar canvas */
  private hpBarCanvas: HTMLCanvasElement
  private hpBarCtx: CanvasRenderingContext2D
  private hpBarTexture: THREE.CanvasTexture
  private hpBarSprite: THREE.Sprite
  /** Name tag sprite */
  private nameTagSprite: THREE.Sprite

  /** Animation state */
  private animationState: {
    type: string
    startTime: number
    duration: number
  } | null = null
  private animationFrameId: number | null = null

  /** Idle animation offset */
  private idleOffset: number = Math.random() * Math.PI * 2

  /** Is fainted */
  private isFainted: boolean = false

  /** Original position for animations */
  private originalY: number = 0

  /**
   * Create Pokemon entity
   * @param config - Pokemon configuration
   */
  constructor(config: PokemonConfig) {
    this.name = config.name
    this.type = config.type
    this.pokemonId = config.id || config.name
    this.currentHp = config.currentHp
    this.maxHp = config.maxHp
    this.scale = config.scale || 2.5

    // Create main group
    this.group = new THREE.Group()
    this.group.name = `pokemon_${config.name}`

    // Create main sprite
    this.spriteMaterial = new THREE.SpriteMaterial({
      map: this.loadSpriteTexture(),
      transparent: true,
      alphaTest: 0.1,
      depthWrite: false
    })
    this.sprite = new THREE.Sprite(this.spriteMaterial)
    this.sprite.scale.set(this.scale, this.scale, 1)
    this.sprite.position.y = this.scale * 0.5
    this.group.add(this.sprite)

    // Create shadow
    const shadowMaterial = new THREE.SpriteMaterial({
      map: this.createShadowTexture(),
      transparent: true,
      opacity: 0.4,
      depthWrite: false
    })
    this.shadowSprite = new THREE.Sprite(shadowMaterial)
    this.shadowSprite.scale.set(this.scale * 0.8, this.scale * 0.3, 1)
    this.shadowSprite.position.y = 0.05
    this.group.add(this.shadowSprite)

    // Create HP bar
    this.hpBarCanvas = document.createElement('canvas')
    this.hpBarCanvas.width = 128
    this.hpBarCanvas.height = 16
    this.hpBarCtx = this.hpBarCanvas.getContext('2d')!
    this.hpBarTexture = new THREE.CanvasTexture(this.hpBarCanvas)
    this.hpBarSprite = new THREE.Sprite(new THREE.SpriteMaterial({
      map: this.hpBarTexture,
      transparent: true,
      depthWrite: false
    }))
    this.hpBarSprite.scale.set(2, 0.25, 1)
    this.hpBarSprite.position.y = this.scale + 0.3
    this.group.add(this.hpBarSprite)
    this.updateHpBar(this.currentHp, this.maxHp)

    // Create name tag
    this.nameTagSprite = this.createNameTag(config.name)
    this.nameTagSprite.position.y = this.scale + 0.6
    this.group.add(this.nameTagSprite)

    // Start idle animation
    this.startIdleAnimation()
  }

  /**
   * Load sprite texture
   */
  private loadSpriteTexture(): THREE.Texture {
    const url = getSpriteUrl(this.pokemonId)
    const texture = new THREE.TextureLoader().load(url, undefined, undefined, () => {
      // Fallback: create colored placeholder
      this.spriteMaterial.map = this.createFallbackTexture()
      this.spriteMaterial.needsUpdate = true
    })
    texture.minFilter = THREE.LinearFilter
    texture.magFilter = THREE.LinearFilter
    return texture
  }

  /**
   * Create fallback texture (colored circle)
   */
  private createFallbackTexture(): THREE.Texture {
    const canvas = document.createElement('canvas')
    canvas.width = 128
    canvas.height = 128
    const ctx = canvas.getContext('2d')!

    // Draw colored circle
    const color = TYPE_COLORS[this.type] || '#A8A77A'
    ctx.fillStyle = color
    ctx.beginPath()
    ctx.arc(64, 64, 50, 0, Math.PI * 2)
    ctx.fill()

    // Draw name initial
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 40px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(this.name[0], 64, 64)

    const texture = new THREE.CanvasTexture(canvas)
    texture.needsUpdate = true
    return texture
  }

  /**
   * Create shadow texture
   */
  private createShadowTexture(): THREE.Texture {
    const canvas = document.createElement('canvas')
    canvas.width = 64
    canvas.height = 32
    const ctx = canvas.getContext('2d')!

    const gradient = ctx.createRadialGradient(32, 16, 0, 32, 16, 30)
    gradient.addColorStop(0, 'rgba(0,0,0,0.6)')
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 64, 32)

    const texture = new THREE.CanvasTexture(canvas)
    texture.needsUpdate = true
    return texture
  }

  /**
   * Create name tag sprite
   */
  private createNameTag(name: string): THREE.Sprite {
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 48
    const ctx = canvas.getContext('2d')!

    // Background
    ctx.fillStyle = 'rgba(0,0,0,0.6)'
    ctx.roundRect(8, 8, 240, 32, 8)
    ctx.fill()

    // Text
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 20px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(name, 128, 24)

    const texture = new THREE.CanvasTexture(canvas)
    const material = new THREE.SpriteMaterial({
      map: texture,
      transparent: true,
      depthWrite: false
    })
    const sprite = new THREE.Sprite(material)
    sprite.scale.set(2.5, 0.5, 1)
    return sprite
  }

  /**
   * Set position
   */
  setPosition(x: number, y: number, z: number): void {
    this.group.position.set(x, y, z)
    this.originalY = y
  }

  /**
   * Update HP bar
   */
  updateHpBar(currentHp: number, maxHp: number): void {
    this.currentHp = currentHp
    this.maxHp = maxHp

    const ctx = this.hpBarCtx
    const canvas = this.hpBarCanvas

    // Clear
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // Background
    ctx.fillStyle = 'rgba(0,0,0,0.7)'
    ctx.roundRect(4, 4, 120, 8, 4)
    ctx.fill()

    // HP bar
    const pct = Math.max(0, Math.min(1, currentHp / maxHp))
    const barWidth = 112 * pct

    let color = '#4ade80' // Green
    if (pct <= 0.2) color = '#ef4444' // Red
    else if (pct <= 0.5) color = '#fbbf24' // Yellow

    ctx.fillStyle = color
    ctx.roundRect(8, 6, barWidth, 4, 2)
    ctx.fill()

    // HP text
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 8px Arial'
    ctx.textAlign = 'right'
    ctx.fillText(`${currentHp}/${maxHp}`, 120, 14)

    this.hpBarTexture.needsUpdate = true
  }

  /**
   * Play animation
   */
  playAnimation(type: 'idle' | 'attack' | 'hit' | 'faint' | 'heal', duration: number): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
    }

    this.animationState = {
      type,
      startTime: performance.now(),
      duration
    }

    const animate = () => {
      if (!this.animationState) return

      const elapsed = performance.now() - this.animationState.startTime
      const progress = Math.min(elapsed / this.animationState.duration, 1)

      switch (this.animationState.type) {
        case 'attack':
          this.animateAttack(progress)
          break
        case 'hit':
          this.animateHit(progress)
          break
        case 'heal':
          this.animateHeal(progress)
          break
        case 'faint':
          this.animateFaint(progress)
          break
      }

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate)
      } else {
        this.animationState = null
        this.animationFrameId = null
        if (type !== 'faint') {
          this.startIdleAnimation()
        }
      }
    }

    this.animationFrameId = requestAnimationFrame(animate)
  }

  /**
   * Attack animation - lunge forward
   */
  private animateAttack(progress: number): void {
    const ease = Math.sin(progress * Math.PI)
    this.group.position.z -= ease * 2
    this.sprite.scale.set(
      this.scale * (1 + ease * 0.2),
      this.scale * (1 + ease * 0.2),
      1
    )
  }

  /**
   * Hit animation - shake and flash
   */
  private animateHit(progress: number): void {
    const shake = Math.sin(progress * Math.PI * 8) * 0.3 * (1 - progress)
    this.group.position.x += shake
    this.spriteMaterial.opacity = 0.5 + Math.sin(progress * Math.PI * 4) * 0.5
  }

  /**
   * Heal animation - glow green
   */
  private animateHeal(progress: number): void {
    const pulse = Math.sin(progress * Math.PI * 3) * 0.3
    this.sprite.scale.set(
      this.scale * (1 + pulse),
      this.scale * (1 + pulse),
      1
    )
  }

  /**
   * Faint animation - fall over and fade
   */
  private animateFaint(progress: number): void {
    this.spriteMaterial.opacity = 1 - progress
    this.group.position.y = this.originalY - progress * 1
    this.group.rotation.x = progress * Math.PI / 2
    if (progress >= 1) {
      this.isFainted = true
    }
  }

  /**
   * Start idle animation (gentle floating)
   */
  private startIdleAnimation(): void {
    if (this.isFainted) return

    const animate = () => {
      if (this.animationState) return

      const time = performance.now() / 1000
      const offset = this.idleOffset

      // Gentle floating
      this.sprite.position.y = this.scale * 0.5 + Math.sin(time * 2 + offset) * 0.1

      // Subtle scale breathing
      const breathe = 1 + Math.sin(time * 1.5 + offset) * 0.02
      this.sprite.scale.set(this.scale * breathe, this.scale * breathe, 1)

      this.animationFrameId = requestAnimationFrame(animate)
    }

    this.animationFrameId = requestAnimationFrame(animate)
  }

  /**
   * Set highlighted (not used for sprite, but kept for compatibility)
   */
  setHighlighted(highlighted: boolean): void {
    // Could add glow effect here if needed
  }

  /**
   * Dispose resources
   */
  dispose(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
    }
    this.spriteMaterial.dispose()
    this.sprite.material.dispose()
    this.shadowSprite.material.dispose()
    this.hpBarTexture.dispose()
    this.hpBarSprite.material.dispose()
    this.nameTagSprite.material.dispose()
  }
}
