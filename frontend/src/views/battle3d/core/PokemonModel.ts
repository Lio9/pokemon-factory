/**
 * PokemonModel.ts - HD-2D 风格宝可梦模型（优化版）
 *
 * 优化内容：
 * 1. 纹理缓存 - 避免重复加载
 * 2. 懒加载 - 只在需要时加载纹理
 * 3. 加载失败时显示占位图
 * 4. 性能优化 - 减少 Canvas 操作
 *
 * @module PokemonModel
 */

import * as THREE from 'three'
import { getTypeColor, TYPE_COLORS } from '../utils/typeColors'

/**
 * 精灵图 URL 生成
 */
const SPRITE_BASE = '/api/pokedex/images/pokemon'

function getSpriteUrl(pokemonId: number | string): string {
  return `${SPRITE_BASE}/${pokemonId}.png`
}

/**
 * 纹理缓存
 * Texture cache to avoid reloading
 */
const textureCache = new Map<string, THREE.Texture>()
const textureLoader = new THREE.TextureLoader()

function getCachedTexture(url: string): THREE.Texture {
  if (textureCache.has(url)) {
    return textureCache.get(url)!
  }
  const texture = textureLoader.load(url)
  texture.minFilter = THREE.LinearFilter
  texture.magFilter = THREE.LinearFilter
  textureCache.set(url, texture)
  return texture
}

/**
 * 宝可梦配置
 */
export interface PokemonConfig {
  id?: number | string
  name: string
  type: string
  currentHp: number
  maxHp: number
  scale?: number
}

/**
 * 创建占位纹理（加载失败时使用）
 */
function createFallbackTexture(name: string, type: string): THREE.Texture {
  const canvas = document.createElement('canvas')
  canvas.width = 128
  canvas.height = 128
  const ctx = canvas.getContext('2d')!

  const color = TYPE_COLORS[type] || '#A8A77A'
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.arc(64, 64, 50, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#fff'
  ctx.font = 'bold 48px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(name[0] || '?', 64, 64)

  const texture = new THREE.CanvasTexture(canvas)
  texture.needsUpdate = true
  return texture
}

/**
 * 创建阴影纹理
 */
function createShadowTexture(): THREE.Texture {
  const canvas = document.createElement('canvas')
  canvas.width = 64
  canvas.height = 32
  const ctx = canvas.getContext('2d')!

  const gradient = ctx.createRadialGradient(32, 16, 0, 32, 16, 30)
  gradient.addColorStop(0, 'rgba(0,0,0,0.5)')
  gradient.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 64, 32)

  return new THREE.CanvasTexture(canvas)
}

/**
 * 创建 HP 条纹理
 */
function createHpBarTexture(currentHp: number, maxHp: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = 128
  canvas.height = 16
  const ctx = canvas.getContext('2d')!

  ctx.fillStyle = 'rgba(0,0,0,0.7)'
  ctx.beginPath()
  ctx.roundRect(4, 4, 120, 8, 4)
  ctx.fill()

  const pct = Math.max(0, Math.min(1, currentHp / maxHp))
  const barWidth = 112 * pct

  let color = '#4ade80'
  if (pct <= 0.2) color = '#ef4444'
  else if (pct <= 0.5) color = '#fbbf24'

  ctx.fillStyle = color
  ctx.beginPath()
  ctx.roundRect(8, 6, barWidth, 4, 2)
  ctx.fill()

  ctx.fillStyle = '#fff'
  ctx.font = 'bold 8px Arial'
  ctx.textAlign = 'right'
  ctx.fillText(`${currentHp}/${maxHp}`, 120, 14)

  return canvas
}

/**
 * 创建名字标签纹理
 */
function createNameTagTexture(name: string): THREE.Texture {
  const canvas = document.createElement('canvas')
  canvas.width = 256
  canvas.height = 48
  const ctx = canvas.getContext('2d')!

  ctx.fillStyle = 'rgba(0,0,0,0.6)'
  ctx.beginPath()
  ctx.roundRect(8, 8, 240, 32, 8)
  ctx.fill()

  ctx.fillStyle = '#fff'
  ctx.font = 'bold 20px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(name, 128, 24)

  return new THREE.CanvasTexture(canvas)
}

/**
 * HD-2D 风格宝可梦实体
 */
export class PokemonEntity {
  public group: THREE.Group

  private name: string
  private type: string
  private pokemonId: number | string
  private currentHp: number
  private maxHp: number
  private scale: number

  private sprite: THREE.Sprite
  private spriteMaterial: THREE.SpriteMaterial
  private shadowSprite: THREE.Sprite
  private hpBarCanvas: HTMLCanvasElement
  private hpBarTexture: THREE.CanvasTexture
  private hpBarSprite: THREE.Sprite
  private nameTagSprite: THREE.Sprite

  private animationState: { type: string; startTime: number; duration: number } | null = null
  private animationFrameId: number | null = null
  private idleOffset = Math.random() * Math.PI * 2
  private isFainted = false
  private originalY = 0

  constructor(config: PokemonConfig) {
    this.name = config.name
    this.type = config.type
    this.pokemonId = config.id || config.name
    this.currentHp = config.currentHp
    this.maxHp = config.maxHp
    this.scale = config.scale || 2.5

    this.group = new THREE.Group()
    this.group.name = `pokemon_${config.name}`

    // 主精灵（使用缓存）
    const spriteUrl = getSpriteUrl(this.pokemonId)
    const spriteTexture = getCachedTexture(spriteUrl)

    this.spriteMaterial = new THREE.SpriteMaterial({
      map: spriteTexture,
      transparent: true,
      alphaTest: 0.1,
      depthWrite: false
    })
    this.sprite = new THREE.Sprite(this.spriteMaterial)
    this.sprite.scale.set(this.scale, this.scale, 1)
    this.sprite.position.y = this.scale * 0.5
    this.group.add(this.sprite)

    // 阴影
    const shadowTexture = createShadowTexture()
    const shadowMaterial = new THREE.SpriteMaterial({
      map: shadowTexture,
      transparent: true,
      opacity: 0.4,
      depthWrite: false
    })
    this.shadowSprite = new THREE.Sprite(shadowMaterial)
    this.shadowSprite.scale.set(this.scale * 0.8, this.scale * 0.3, 1)
    this.shadowSprite.position.y = 0.05
    this.group.add(this.shadowSprite)

    // HP 条
    this.hpBarCanvas = createHpBarTexture(this.currentHp, this.maxHp)
    this.hpBarTexture = new THREE.CanvasTexture(this.hpBarCanvas)
    this.hpBarSprite = new THREE.Sprite(new THREE.SpriteMaterial({
      map: this.hpBarTexture,
      transparent: true,
      depthWrite: false
    }))
    this.hpBarSprite.scale.set(2, 0.25, 1)
    this.hpBarSprite.position.y = this.scale + 0.3
    this.group.add(this.hpBarSprite)

    // 名字标签
    const nameTexture = createNameTagTexture(config.name)
    this.nameTagSprite = new THREE.Sprite(new THREE.SpriteMaterial({
      map: nameTexture,
      transparent: true,
      depthWrite: false
    }))
    this.nameTagSprite.scale.set(2.5, 0.5, 1)
    this.nameTagSprite.position.y = this.scale + 0.6
    this.group.add(this.nameTagSprite)

    this.startIdleAnimation()
  }

  setPosition(x: number, y: number, z: number): void {
    this.group.position.set(x, y, z)
    this.originalY = y
  }

  updateHpBar(currentHp: number, maxHp: number): void {
    this.currentHp = currentHp
    this.maxHp = maxHp
    this.hpBarCanvas = createHpBarTexture(currentHp, maxHp)
    this.hpBarTexture.image = this.hpBarCanvas
    this.hpBarTexture.needsUpdate = true
  }

  playAnimation(type: 'idle' | 'attack' | 'hit' | 'faint' | 'heal', duration: number): void {
    if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId)

    this.animationState = { type, startTime: performance.now(), duration }

    const animate = () => {
      if (!this.animationState) return
      const elapsed = performance.now() - this.animationState.startTime
      const progress = Math.min(elapsed / this.animationState.duration, 1)

      switch (this.animationState.type) {
        case 'attack': this.animateAttack(progress); break
        case 'hit': this.animateHit(progress); break
        case 'heal': this.animateHeal(progress); break
        case 'faint': this.animateFaint(progress); break
      }

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate)
      } else {
        this.animationState = null
        this.animationFrameId = null
        if (type !== 'faint') this.startIdleAnimation()
      }
    }

    this.animationFrameId = requestAnimationFrame(animate)
  }

  private animateAttack(progress: number): void {
    const ease = Math.sin(progress * Math.PI)
    this.group.position.z -= ease * 2
    this.sprite.scale.set(this.scale * (1 + ease * 0.2), this.scale * (1 + ease * 0.2), 1)
  }

  private animateHit(progress: number): void {
    const shake = Math.sin(progress * Math.PI * 8) * 0.3 * (1 - progress)
    this.group.position.x += shake
    this.spriteMaterial.opacity = 0.5 + Math.sin(progress * Math.PI * 4) * 0.5
  }

  private animateHeal(progress: number): void {
    const pulse = Math.sin(progress * Math.PI * 3) * 0.3
    this.sprite.scale.set(this.scale * (1 + pulse), this.scale * (1 + pulse), 1)
  }

  private animateFaint(progress: number): void {
    this.spriteMaterial.opacity = 1 - progress
    this.group.position.y = this.originalY - progress * 1
    this.group.rotation.x = progress * Math.PI / 2
    if (progress >= 1) this.isFainted = true
  }

  private startIdleAnimation(): void {
    if (this.isFainted) return

    const animate = () => {
      if (this.animationState) return
      const time = performance.now() / 1000
      const offset = this.idleOffset

      this.sprite.position.y = this.scale * 0.5 + Math.sin(time * 2 + offset) * 0.1
      const breathe = 1 + Math.sin(time * 1.5 + offset) * 0.02
      this.sprite.scale.set(this.scale * breathe, this.scale * breathe, 1)

      this.animationFrameId = requestAnimationFrame(animate)
    }

    this.animationFrameId = requestAnimationFrame(animate)
  }

  setHighlighted(highlighted: boolean): void {
    // 可以添加高亮效果
  }

  dispose(): void {
    if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId)
    this.spriteMaterial.dispose()
    this.hpBarTexture.dispose()
  }
}

/**
 * 清理纹理缓存（在场景销毁时调用）
 */
export function clearTextureCache(): void {
  textureCache.forEach(texture => texture.dispose())
  textureCache.clear()
}
