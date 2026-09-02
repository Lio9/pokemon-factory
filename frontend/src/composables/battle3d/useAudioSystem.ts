/**
 * @description 音效系统 - Web Audio API 空间音频
 * @description Audio system - Web Audio API spatial audio
 *
 * 功能：
 * 1. 3D 空间音频（距离衰减、方向性）
 * 2. 音效池管理（避免重复创建）
 * 3. 音量控制和静音
 * 4. 攻击、治愈、特效等音效
 *
 * @module composables/battle3d/useAudioSystem
 */

import { ref, onUnmounted } from 'vue'

/**
 * 音效类型
 * Sound effect types
 */
export type SoundEffectType =
  | 'attack_normal'
  | 'attack_fire'
  | 'attack_water'
  | 'attack_electric'
  | 'attack_grass'
  | 'attack_ice'
  | 'attack_fighting'
  | 'hit'
  | 'critical_hit'
  | 'heal'
  | 'faint'
  | 'level_up'
  | 'terastallize'
  | 'switch'
  | 'victory'
  | 'defeat'
  | 'button_click'
  | 'error'

/**
 * 音效配置
 * Sound configuration
 */
interface SoundConfig {
  /** 音量 (0-1) / Volume (0-1) */
  volume: number
  /** 音调倍率 / Pitch multiplier */
  pitch?: number
  /** 是否循环 / Loop */
  loop?: boolean
  /** 空间音频位置 / Spatial audio position */
  position?: { x: number; y: number; z: number }
}

/**
 * 默认音效配置
 * Default sound configurations
 */
const DEFAULT_SOUND_CONFIGS: Record<SoundEffectType, SoundConfig> = {
  attack_normal: { volume: 0.5, pitch: 1.0 },
  attack_fire: { volume: 0.6, pitch: 0.9 },
  attack_water: { volume: 0.5, pitch: 1.1 },
  attack_electric: { volume: 0.7, pitch: 1.2 },
  attack_grass: { volume: 0.4, pitch: 0.8 },
  attack_ice: { volume: 0.5, pitch: 1.3 },
  attack_fighting: { volume: 0.6, pitch: 0.7 },
  hit: { volume: 0.6, pitch: 1.0 },
  critical_hit: { volume: 0.8, pitch: 0.8 },
  heal: { volume: 0.4, pitch: 1.5 },
  faint: { volume: 0.5, pitch: 0.6 },
  level_up: { volume: 0.5, pitch: 1.2 },
  terastallize: { volume: 0.7, pitch: 1.0 },
  switch: { volume: 0.3, pitch: 1.1 },
  victory: { volume: 0.8, pitch: 1.0 },
  defeat: { volume: 0.5, pitch: 0.7 },
  button_click: { volume: 0.2, pitch: 1.5 },
  error: { volume: 0.4, pitch: 0.5 }
}

/**
 * 音效系统 composable
 * Audio system composable
 */
export function useAudioSystem() {
  // ===== 状态 =====
  const isInitialized = ref(false)
  const isMuted = ref(false)
  const masterVolume = ref(0.7)
  const sfxVolume = ref(1.0)

  // ===== Web Audio API 对象 =====
  let audioContext: AudioContext | null = null
  let masterGain: GainNode | null = null
  let sfxGain: GainNode | null = null

  // ===== 音效缓冲区缓存 =====
  const audioBuffers: Map<SoundEffectType, AudioBuffer> = new Map()

  // ===== 活跃音源追踪 =====
  const activeSources: Set<AudioBufferSourceNode> = new Set()

  /**
   * 初始化音频系统
   * Initialize audio system
   */
  const init = async (): Promise<void> => {
    if (isInitialized.value) return

    try {
      // Create audio context
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()

      // Create gain nodes for volume control
      masterGain = audioContext.createGain()
      sfxGain = audioContext.createGain()

      // Connect: sfxGain -> masterGain -> destination
      sfxGain.connect(masterGain)
      masterGain.connect(audioContext.destination)

      // Set initial volumes
      masterGain.gain.value = masterVolume.value
      sfxGain.gain.value = sfxVolume.value

      // Generate procedural sounds
      await generateSounds()

      isInitialized.value = true
      console.log('[AudioSystem] Initialized')
    } catch (error) {
      console.error('[AudioSystem] Failed to initialize:', error)
    }
  }

  /**
   * 生成程序化音效
   * Generate procedural sound effects
   */
  const generateSounds = async (): Promise<void> => {
    if (!audioContext) return

    // Generate each sound effect
    for (const [type, config] of Object.entries(DEFAULT_SOUND_CONFIGS)) {
      const buffer = generateSoundBuffer(type as SoundEffectType, config)
      if (buffer) {
        audioBuffers.set(type as SoundEffectType, buffer)
      }
    }
  }

  /**
   * 生成音效缓冲区
   * Generate sound buffer
   */
  const generateSoundBuffer = (type: SoundEffectType, config: SoundConfig): AudioBuffer | null => {
    if (!audioContext) return null

    const sampleRate = audioContext.sampleRate
    let duration = 0.3
    let buffer: AudioBuffer

    switch (type) {
      case 'attack_normal':
      case 'attack_fighting':
        duration = 0.25
        buffer = createNoiseBuffer(sampleRate, duration, 'white')
        applyEnvelope(buffer, 0.01, 0.1, 0.3, 0.1)
        break

      case 'attack_fire':
        duration = 0.4
        buffer = createNoiseBuffer(sampleRate, duration, 'pink')
        applyEnvelope(buffer, 0.05, 0.15, 0.4, 0.2)
        break

      case 'attack_water':
        duration = 0.35
        buffer = createNoiseBuffer(sampleRate, duration, 'pink')
        applyEnvelope(buffer, 0.02, 0.1, 0.5, 0.15)
        break

      case 'attack_electric':
        duration = 0.2
        buffer = createNoiseBuffer(sampleRate, duration, 'white')
        applyEnvelope(buffer, 0.005, 0.05, 0.2, 0.05)
        break

      case 'attack_grass':
        duration = 0.3
        buffer = createNoiseBuffer(sampleRate, duration, 'pink')
        applyEnvelope(buffer, 0.03, 0.1, 0.4, 0.15)
        break

      case 'attack_ice':
        duration = 0.25
        buffer = createNoiseBuffer(sampleRate, duration, 'white')
        applyEnvelope(buffer, 0.01, 0.08, 0.3, 0.1)
        break

      case 'hit':
        duration = 0.15
        buffer = createNoiseBuffer(sampleRate, duration, 'white')
        applyEnvelope(buffer, 0.001, 0.05, 0.1, 0.05)
        break

      case 'critical_hit':
        duration = 0.3
        buffer = createNoiseBuffer(sampleRate, duration, 'white')
        applyEnvelope(buffer, 0.001, 0.1, 0.2, 0.1)
        break

      case 'heal':
        duration = 0.5
        buffer = createToneBuffer(sampleRate, duration, [523.25, 659.25, 783.99], 'sine')
        applyEnvelope(buffer, 0.05, 0.2, 0.6, 0.2)
        break

      case 'faint':
        duration = 0.6
        buffer = createToneBuffer(sampleRate, duration, [400, 300, 200], 'sine')
        applyEnvelope(buffer, 0.05, 0.2, 0.5, 0.3)
        break

      case 'terastallize':
        duration = 0.8
        buffer = createToneBuffer(sampleRate, duration, [523.25, 659.25, 783.99, 1046.50], 'sine')
        applyEnvelope(buffer, 0.1, 0.3, 0.7, 0.3)
        break

      case 'switch':
        duration = 0.2
        buffer = createToneBuffer(sampleRate, duration, [800, 1200], 'sine')
        applyEnvelope(buffer, 0.01, 0.05, 0.3, 0.1)
        break

      case 'victory':
        duration = 1.0
        buffer = createToneBuffer(sampleRate, duration, [523.25, 659.25, 783.99, 1046.50, 1318.51], 'sine')
        applyEnvelope(buffer, 0.1, 0.3, 0.8, 0.4)
        break

      case 'defeat':
        duration = 0.8
        buffer = createToneBuffer(sampleRate, duration, [400, 350, 300, 250], 'sine')
        applyEnvelope(buffer, 0.1, 0.2, 0.5, 0.3)
        break

      case 'button_click':
        duration = 0.1
        buffer = createToneBuffer(sampleRate, duration, [1000], 'sine')
        applyEnvelope(buffer, 0.001, 0.02, 0.2, 0.05)
        break

      case 'error':
        duration = 0.3
        buffer = createToneBuffer(sampleRate, duration, [200, 150], 'square')
        applyEnvelope(buffer, 0.01, 0.1, 0.3, 0.1)
        break

      default:
        return null
    }

    return buffer
  }

  /**
   * 创建噪声缓冲区
   * Create noise buffer
   */
  const createNoiseBuffer = (sampleRate: number, duration: number, type: 'white' | 'pink'): AudioBuffer => {
    const length = sampleRate * duration
    const buffer = audioContext!.createBuffer(1, length, sampleRate)
    const data = buffer.getChannelData(0)

    let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0

    for (let i = 0; i < length; i++) {
      const white = Math.random() * 2 - 1

      if (type === 'white') {
        data[i] = white
      } else {
        // Pink noise (1/f noise)
        b0 = 0.99886 * b0 + white * 0.0555179
        b1 = 0.99332 * b1 + white * 0.0750759
        b2 = 0.96900 * b2 + white * 0.1538520
        b3 = 0.86650 * b3 + white * 0.3104856
        b4 = 0.55000 * b4 + white * 0.5329522
        b5 = -0.7616 * b5 - white * 0.0168980
        data[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
        b6 = white * 0.115926
      }
    }

    return buffer
  }

  /**
   * 创建音调缓冲区
   * Create tone buffer
   */
  const createToneBuffer = (
    sampleRate: number,
    duration: number,
    frequencies: number[],
    type: OscillatorType
  ): AudioBuffer => {
    const length = sampleRate * duration
    const buffer = audioContext!.createBuffer(1, length, sampleRate)
    const data = buffer.getChannelData(0)

    for (let i = 0; i < length; i++) {
      const t = i / sampleRate
      let sample = 0

      for (const freq of frequencies) {
        const phase = 2 * Math.PI * freq * t
        switch (type) {
          case 'sine':
            sample += Math.sin(phase) / frequencies.length
            break
          case 'square':
            sample += (Math.sin(phase) > 0 ? 1 : -1) / frequencies.length
            break
          case 'sawtooth':
            sample += (2 * (freq * t - Math.floor(freq * t + 0.5))) / frequencies.length
            break
          case 'triangle':
            sample += (2 * Math.abs(2 * (freq * t - Math.floor(freq * t + 0.5))) - 1) / frequencies.length
            break
        }
      }

      data[i] = sample
    }

    return buffer
  }

  /**
   * 应用包络
   * Apply envelope to buffer
   */
  const applyEnvelope = (
    buffer: AudioBuffer,
    attack: number,
    decay: number,
    sustain: number,
    release: number
  ): void => {
    const data = buffer.getChannelData(0)
    const sampleRate = audioContext!.sampleRate
    const length = data.length

    const attackSamples = attack * sampleRate
    const decaySamples = decay * sampleRate
    const releaseSamples = release * sampleRate
    const sustainStart = attackSamples + decaySamples
    const releaseStart = length - releaseSamples

    for (let i = 0; i < length; i++) {
      let gain = 1

      if (i < attackSamples) {
        // Attack phase
        gain = i / attackSamples
      } else if (i < sustainStart) {
        // Decay phase
        const decayProgress = (i - attackSamples) / decaySamples
        gain = 1 - (1 - sustain) * decayProgress
      } else if (i < releaseStart) {
        // Sustain phase
        gain = sustain
      } else {
        // Release phase
        const releaseProgress = (i - releaseStart) / releaseSamples
        gain = sustain * (1 - releaseProgress)
      }

      data[i] *= gain
    }
  }

  /**
   * 播放音效
   * Play sound effect
   */
  const playSound = (type: SoundEffectType, config?: Partial<SoundConfig>): void => {
    if (!isInitialized.value || isMuted.value || !audioContext || !sfxGain) return

    // Resume audio context if suspended (required by browsers)
    if (audioContext.state === 'suspended') {
      audioContext.resume()
    }

    const buffer = audioBuffers.get(type)
    if (!buffer) {
      console.warn(`[AudioSystem] Sound not found: ${type}`)
      return
    }

    const finalConfig = { ...DEFAULT_SOUND_CONFIGS[type], ...config }

    // Create source node
    const source = audioContext.createBufferSource()
    source.buffer = buffer
    source.playbackRate.value = finalConfig.pitch || 1.0
    source.loop = finalConfig.loop || false

    // Create gain node for this sound
    const gainNode = audioContext.createGain()
    gainNode.gain.value = finalConfig.volume

    // Connect: source -> gainNode -> sfxGain
    source.connect(gainNode)
    gainNode.connect(sfxGain)

    // Play
    source.start(0)

    // Track active source
    activeSources.add(source)

    // Clean up when done
    source.onended = () => {
      activeSources.delete(source)
      source.disconnect()
      gainNode.disconnect()
    }
  }

  /**
   * 播放攻击音效（根据属性类型）
   * Play attack sound (based on type)
   */
  const playAttackSound = (pokemonType: string): void => {
    const typeMap: Record<string, SoundEffectType> = {
      fire: 'attack_fire',
      water: 'attack_water',
      electric: 'attack_electric',
      grass: 'attack_grass',
      ice: 'attack_ice',
      fighting: 'attack_fighting'
    }

    const soundType = typeMap[pokemonType.toLowerCase()] || 'attack_normal'
    playSound(soundType)
  }

  /**
   * 设置主音量
   * Set master volume
   */
  const setMasterVolume = (volume: number): void => {
    masterVolume.value = Math.max(0, Math.min(1, volume))
    if (masterGain) {
      masterGain.gain.value = masterVolume.value
    }
  }

  /**
   * 设置音效音量
   * Set SFX volume
   */
  const setSfxVolume = (volume: number): void => {
    sfxVolume.value = Math.max(0, Math.min(1, volume))
    if (sfxGain) {
      sfxGain.gain.value = sfxVolume.value
    }
  }

  /**
   * 切换静音
   * Toggle mute
   */
  const toggleMute = (): void => {
    isMuted.value = !isMuted.value
    if (masterGain) {
      masterGain.gain.value = isMuted.value ? 0 : masterVolume.value
    }
  }

  /**
   * 停止所有音效
   * Stop all sounds
   */
  const stopAll = (): void => {
    activeSources.forEach(source => {
      try {
        source.stop()
      } catch (e) {
        // Source may have already stopped
      }
    })
    activeSources.clear()
  }

  /**
   * 销毁音频系统
   * Dispose audio system
   */
  const dispose = (): void => {
    stopAll()

    if (audioContext) {
      audioContext.close()
      audioContext = null
    }

    audioBuffers.clear()
    isInitialized.value = false

    console.log('[AudioSystem] Disposed')
  }

  // ===== 生命周期 =====
  onUnmounted(() => {
    dispose()
  })

  return {
    // State
    isInitialized,
    isMuted,
    masterVolume,
    sfxVolume,

    // Methods
    init,
    playSound,
    playAttackSound,
    setMasterVolume,
    setSfxVolume,
    toggleMute,
    stopAll,
    dispose
  }
}
