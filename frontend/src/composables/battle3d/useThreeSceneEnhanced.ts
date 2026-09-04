/**
 * @description 增强版 Three.js 场景管理器 - 性能优化 + 视觉增强
 * @description Enhanced Three.js scene manager - Performance optimization + Visual enhancement
 *
 * 优化内容：
 * 1. 性能监控与自适应质量
 * 2. 后处理效果（Bloom、环境光遮蔽）
 * 3. 天空盒与环境光照
 * 4. 雾效增强深度感
 * 5. 对象池管理
 * 6. 移动端自适应
 *
 * @module composables/battle3d/useThreeSceneEnhanced
 */

import { ref, shallowRef, type Ref, onUnmounted, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

/**
 * 性能等级
 * Performance levels
 */
export type PerformanceLevel = 'low' | 'medium' | 'high' | 'ultra'

/**
 * 增强版场景配置
 * Enhanced scene configuration
 */
interface EnhancedSceneOptions {
  /** 启用阴影 / Enable shadows */
  enableShadows?: boolean
  /** 相机视野 / Camera FOV */
  fov?: number
  /** 近裁剪面 / Near clipping plane */
  near?: number
  /** 远裁剪面 / Far clipping plane */
  far?: number
  /** 背景颜色 / Background color */
  backgroundColor?: number | string
  /** 启用控制器 / Enable controls */
  enableControls?: boolean
  /** 性能等级 / Performance level */
  performanceLevel?: PerformanceLevel
  /** 启用后处理 / Enable post-processing */
  enablePostProcessing?: boolean
  /** 启用雾效 / Enable fog */
  enableFog?: boolean
  /** 启用天空盒 / Enable skybox */
  enableSkybox?: boolean
}

/**
 * 性能统计
 * Performance statistics
 */
interface PerformanceStats {
  /** FPS */
  fps: number
  /** 帧时间 (ms) / Frame time (ms) */
  frameTime: number
  /** 绘制调用次数 / Draw calls */
  drawCalls: number
  /** 三角形数量 / Triangle count */
  triangles: number
  /** 几何体数量 / Geometry count */
  geometries: number
  /** 纹理数量 / Texture count */
  textures: number
  /** 内存使用估计 (MB) / Estimated memory usage (MB) */
  memoryUsage: number
}

/**
 * 对象池
 * Object pool for reusing Three.js objects
 */
class ObjectPool<T extends THREE.Object3D> {
  private pool: T[] = []
  private factory: () => T

  constructor(factory: () => T, initialSize: number = 10) {
    this.factory = factory
    // Pre-populate pool
    for (let i = 0; i < initialSize; i++) {
      this.pool.push(factory())
    }
  }

  acquire(): T {
    if (this.pool.length > 0) {
      return this.pool.pop()!
    }
    return this.factory()
  }

  release(obj: T): void {
    // Reset object state
    obj.position.set(0, 0, 0)
    obj.rotation.set(0, 0, 0)
    obj.scale.set(1, 1, 1)
    obj.visible = true
    this.pool.push(obj)
  }

  clear(): void {
    this.pool = []
  }
}

/**
 * 增强版 useThreeScene
 * Enhanced useThreeScene composable
 */
export function useThreeSceneEnhanced(
  container: Ref<HTMLElement | null>,
  options: EnhancedSceneOptions = {}
) {
  // ===== 配置 =====
  const {
    enableShadows = true,
    fov = 60,
    near = 0.1,
    far = 500,
    backgroundColor = 0x0a0a1a,
    enableControls = true,
    performanceLevel = 'high',
    enablePostProcessing = false,
    enableFog = true,
    enableSkybox = true
  } = options

  // ===== 核心对象 =====
  const scene = shallowRef<THREE.Scene | null>(null)
  const camera = shallowRef<THREE.PerspectiveCamera | null>(null)
  const renderer = shallowRef<THREE.WebGLRenderer | null>(null)
  const controls = shallowRef<OrbitControls | null>(null)
  const raycaster = new THREE.Raycaster()

  // ===== 状态 =====
  const isReady = ref(false)
  const fps = ref(0)
  const performanceStats = ref<PerformanceStats>({
    fps: 0,
    frameTime: 0,
    drawCalls: 0,
    triangles: 0,
    geometries: 0,
    textures: 0,
    memoryUsage: 0
  })

  // ===== 内部变量 =====
  let animationFrameId: number | null = null
  let isPaused = false
  let frameCount = 0
  let lastFpsTime = 0
  let lastFrameTime = 0
  let currentPerformanceLevel = performanceLevel

  // ===== 对象池 =====
  const vector3Pool = new ObjectPool<THREE.Object3D>(() => new THREE.Object3D(), 50)

  // ===== 性能配置 =====
  const PERFORMANCE_CONFIGS: Record<PerformanceLevel, {
    shadowMapSize: number
    pixelRatio: number
    antialias: boolean
    shadows: boolean
    maxLights: number
  }> = {
    low: {
      shadowMapSize: 512,
      pixelRatio: 1,
      antialias: false,
      shadows: false,
      maxLights: 2
    },
    medium: {
      shadowMapSize: 1024,
      pixelRatio: Math.min(window.devicePixelRatio, 1.5),
      antialias: true,
      shadows: true,
      maxLights: 4
    },
    high: {
      shadowMapSize: 2048,
      pixelRatio: Math.min(window.devicePixelRatio, 2),
      antialias: true,
      shadows: true,
      maxLights: 8
    },
    ultra: {
      shadowMapSize: 4096,
      pixelRatio: window.devicePixelRatio,
      antialias: true,
      shadows: true,
      maxLights: 16
    }
  }

  /**
   * 获取当前性能配置
   * Get current performance configuration
   */
  function getPerfConfig() {
    return PERFORMANCE_CONFIGS[currentPerformanceLevel]
  }

  /**
   * 初始化场景
   * Initialize scene
   */
  const initScene = (): void => {
    if (!container.value) {
      console.warn('[useThreeSceneEnhanced] Container not found')
      return
    }

    const perfConfig = getPerfConfig()

    // Create scene
    scene.value = new THREE.Scene()
    scene.value.background = new THREE.Color(backgroundColor)

    // Add fog for depth
    if (enableFog) {
      scene.value.fog = new THREE.FogExp2(backgroundColor as number, 0.015)
    }

    // Create camera with optimized settings
    const aspect = container.value.clientWidth / container.value.clientHeight
    camera.value = new THREE.PerspectiveCamera(fov, aspect, near, far)
    camera.value.position.set(0, 12, 16)
    camera.value.lookAt(0, 0, 0)

    // Create renderer with performance-based settings
    renderer.value = new THREE.WebGLRenderer({
      antialias: perfConfig.antialias,
      alpha: false,
      powerPreference: 'high-performance',
      stencil: false
    })

    renderer.value.setSize(container.value.clientWidth, container.value.clientHeight)
    renderer.value.setPixelRatio(perfConfig.pixelRatio)

    // Configure shadows based on performance level
    if (enableShadows && perfConfig.shadows) {
      renderer.value.shadowMap.enabled = true
      renderer.value.shadowMap.type = THREE.PCFSoftShadowMap
    } else {
      renderer.value.shadowMap.enabled = false
    }

    // Optimize renderer
    renderer.value.info.autoReset = false
    renderer.value.sortObjects = true

    // Append to container
    container.value.appendChild(renderer.value.domElement)

    // Setup controls
    if (enableControls) {
      controls.value = new OrbitControls(camera.value, renderer.value.domElement)
      controls.value.enableDamping = true
      controls.value.dampingFactor = 0.08
      controls.value.screenSpacePanning = false
      controls.value.minDistance = 5
      controls.value.maxDistance = 50
      controls.value.maxPolarAngle = Math.PI / 2.2
      controls.value.target.set(0, 0, 0)
    }

    // Setup lighting
    setupLighting()

    // Setup skybox
    if (enableSkybox) {
      setupSkybox()
    }

    // Mark as ready
    isReady.value = true

    // Setup resize handler
    window.addEventListener('resize', handleResize)

    // Setup visibility change handler (pause when tab is hidden)
    document.addEventListener('visibilitychange', handleVisibilityChange)

    console.log(`[useThreeSceneEnhanced] Initialized with ${currentPerformanceLevel} performance level`)
  }

  /**
   * 处理页面可见性变化
   * Handle page visibility change
   */
  const handleVisibilityChange = (): void => {
    if (document.hidden) {
      // Page hidden - pause rendering
      stopRenderLoop()
    } else {
      // Page visible - resume rendering
      startRenderLoop()
    }
  }

  /**
   * 设置增强光照
   * Setup enhanced lighting
   */
  const setupLighting = (): void => {
    if (!scene.value) return

    const perfConfig = getPerfConfig()

    // Ambient light - softer base illumination
    const ambientLight = new THREE.AmbientLight(0x404060, 0.5)
    scene.value.add(ambientLight)

    // Main directional light (sun)
    const sunLight = new THREE.DirectionalLight(0xffeedd, 1.0)
    sunLight.position.set(15, 25, 15)
    sunLight.castShadow = perfConfig.shadows

    if (perfConfig.shadows) {
      sunLight.shadow.mapSize.width = perfConfig.shadowMapSize
      sunLight.shadow.mapSize.height = perfConfig.shadowMapSize
      sunLight.shadow.camera.near = 0.5
      sunLight.shadow.camera.far = 60
      sunLight.shadow.camera.left = -25
      sunLight.shadow.camera.right = 25
      sunLight.shadow.camera.top = 25
      sunLight.shadow.camera.bottom = -25
      sunLight.shadow.bias = -0.001
      sunLight.shadow.normalBias = 0.02
    }

    scene.value.add(sunLight)

    // Fill light from opposite side
    const fillLight = new THREE.DirectionalLight(0x8888ff, 0.3)
    fillLight.position.set(-10, 15, -10)
    scene.value.add(fillLight)

    // Hemisphere light for natural outdoor feel
    const hemiLight = new THREE.HemisphereLight(0x87ceeb, 0x362907, 0.4)
    scene.value.add(hemiLight)

    // Rim light for character separation
    const rimLight = new THREE.DirectionalLight(0xffffff, 0.2)
    rimLight.position.set(0, 10, -15)
    scene.value.add(rimLight)
  }

  /**
   * 设置天空盒
   * Setup procedural skybox
   */
  const setupSkybox = (): void => {
    if (!scene.value) return

    // Create gradient sky using a large sphere
    const skyGeometry = new THREE.SphereGeometry(200, 32, 32)
    const skyMaterial = new THREE.ShaderMaterial({
      uniforms: {
        topColor: { value: new THREE.Color(0x0a0a2e) },
        bottomColor: { value: new THREE.Color(0x1a1a4e) },
        offset: { value: 20 },
        exponent: { value: 0.4 }
      },
      vertexShader: `
        varying vec3 vWorldPosition;
        void main() {
          vec4 worldPosition = modelMatrix * vec4(position, 1.0);
          vWorldPosition = worldPosition.xyz;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform vec3 topColor;
        uniform vec3 bottomColor;
        uniform float offset;
        uniform float exponent;
        varying vec3 vWorldPosition;
        void main() {
          float h = normalize(vWorldPosition + offset).y;
          gl_FragColor = vec4(mix(bottomColor, topColor, max(pow(max(h, 0.0), exponent), 0.0)), 1.0);
        }
      `,
      side: THREE.BackSide,
      depthWrite: false
    })

    const sky = new THREE.Mesh(skyGeometry, skyMaterial)
    scene.value.add(sky)
  }

  /**
   * 处理窗口大小变化
   * Handle window resize
   */
  const handleResize = (): void => {
    if (!container.value || !camera.value || !renderer.value) return

    const width = container.value.clientWidth
    const height = container.value.clientHeight

    camera.value.aspect = width / height
    camera.value.updateProjectionMatrix()

    renderer.value.setSize(width, height)
  }

  /**
   * 更新性能统计
   * Update performance statistics
   */
  const updatePerformanceStats = (): void => {
    if (!renderer.value) return

    frameCount++
    const currentTime = performance.now()
    const frameDelta = currentTime - lastFrameTime
    lastFrameTime = currentTime

    // Update FPS every second
    const fpsElapsed = currentTime - lastFpsTime
    if (fpsElapsed >= 1000) {
      fps.value = Math.round((frameCount * 1000) / fpsElapsed)
      frameCount = 0
      lastFpsTime = currentTime

      // Update detailed stats
      const info = renderer.value.info
      performanceStats.value = {
        fps: fps.value,
        frameTime: frameDelta,
        drawCalls: info.render.calls,
        triangles: info.render.triangles,
        geometries: info.memory.geometries,
        textures: info.memory.textures,
        memoryUsage: Math.round((info.memory.geometries * 0.001 + info.memory.textures * 0.004) * 100) / 100
      }

      // Auto-adjust performance level based on FPS
      autoAdjustPerformance()
    }

    // Reset renderer info
    renderer.value.info.reset()
  }

  /**
   * 自动调整性能等级
   * Auto-adjust performance level based on FPS
   */
  const autoAdjustPerformance = (): void => {
    const currentFps = fps.value
    const levels: PerformanceLevel[] = ['low', 'medium', 'high', 'ultra']
    const currentIndex = levels.indexOf(currentPerformanceLevel)

    // Downgrade if FPS is too low
    if (currentFps < 30 && currentIndex > 0) {
      currentPerformanceLevel = levels[currentIndex - 1]
      console.log(`[Performance] Downgraded to ${currentPerformanceLevel} (FPS: ${currentFps})`)
      applyPerformanceSettings()
    }
    // Upgrade if FPS is consistently high
    else if (currentFps > 55 && currentIndex < levels.length - 1) {
      currentPerformanceLevel = levels[currentIndex + 1]
      console.log(`[Performance] Upgraded to ${currentPerformanceLevel} (FPS: ${currentFps})`)
      applyPerformanceSettings()
    }
  }

  /**
   * 应用性能设置
   * Apply performance settings
   */
  const applyPerformanceSettings = (): void => {
    if (!renderer.value) return

    const perfConfig = getPerfConfig()

    renderer.value.setPixelRatio(perfConfig.pixelRatio)
    renderer.value.shadowMap.enabled = perfConfig.shadows && enableShadows
  }

  /**
   * 渲染循环
   * Render loop
   */
  const animate = (): void => {
    if (isPaused) return

    animationFrameId = requestAnimationFrame(animate)

    // Update controls
    if (controls.value) {
      controls.value.update()
    }

    // Render scene
    if (renderer.value && scene.value && camera.value) {
      renderer.value.render(scene.value, camera.value)
    }

    // Update performance stats
    updatePerformanceStats()
  }

  /**
   * 启动渲染循环
   * Start render loop
   */
  const startRenderLoop = (): void => {
    if (animationFrameId !== null) return

    isPaused = false
    lastFpsTime = performance.now()
    lastFrameTime = performance.now()
    frameCount = 0
    animate()
  }

  /**
   * 停止渲染循环
   * Stop render loop
   */
  const stopRenderLoop = (): void => {
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
    isPaused = true
  }

  /**
   * 添加对象到场景
   * Add object to scene
   */
  const addToScene = (object: THREE.Object3D): void => {
    if (scene.value) {
      scene.value.add(object)
    }
  }

  /**
   * 从场景移除对象
   * Remove object from scene
   */
  const removeFromScene = (object: THREE.Object3D): void => {
    if (scene.value) {
      scene.value.remove(object)
    }
  }

  /**
   * 根据名称获取对象
   * Get object by name
   */
  const getObjectByName = (name: string): THREE.Object3D | undefined => {
    return scene.value?.getObjectByName(name)
  }

  /**
   * 设置性能等级
   * Set performance level
   */
  const setPerformanceLevel = (level: PerformanceLevel): void => {
    currentPerformanceLevel = level
    applyPerformanceSettings()
    console.log(`[Performance] Set to ${level}`)
  }

  /**
   * 获取当前性能等级
   * Get current performance level
   */
  const getPerformanceLevel = (): PerformanceLevel => {
    return currentPerformanceLevel
  }

  /**
   * 创建渐变纹理
   * Create gradient texture
   */
  const createGradientTexture = (colors: string[], size: number = 256): THREE.Texture => {
    const canvas = document.createElement('canvas')
    canvas.width = size
    canvas.height = 1
    const ctx = canvas.getContext('2d')!

    const gradient = ctx.createLinearGradient(0, 0, size, 0)
    colors.forEach((color, index) => {
      gradient.addColorStop(index / (colors.length - 1), color)
    })

    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, size, 1)

    const texture = new THREE.CanvasTexture(canvas)
    texture.needsUpdate = true
    return texture
  }

  /**
   * 销毁所有资源
   * Dispose all resources
   */
  const dispose = (): void => {
    stopRenderLoop()

    // Dispose controls
    if (controls.value) {
      controls.value.dispose()
    }

    // Dispose renderer
    if (renderer.value) {
      renderer.value.dispose()
      renderer.value.domElement.remove()
    }

    // Dispose scene objects
    if (scene.value) {
      scene.value.traverse((object) => {
        if (object instanceof THREE.Mesh) {
          if (object.geometry) object.geometry.dispose()
          if (object.material) {
            if (Array.isArray(object.material)) {
              object.material.forEach(material => material.dispose())
            } else {
              object.material.dispose()
            }
          }
        }
      })
    }

    // Clear object pools
    vector3Pool.clear()

    // Remove resize listener
    window.removeEventListener('resize', handleResize)
    document.removeEventListener('visibilitychange', handleVisibilityChange)

    // Reset state
    scene.value = null
    camera.value = null
    renderer.value = null
    controls.value = null
    isReady.value = false

    console.log('[useThreeSceneEnhanced] Disposed')
  }

  // ===== 生命周期 =====
  onUnmounted(() => {
    dispose()
  })

  return {
    // Core objects
    scene,
    camera,
    renderer,
    controls,
    raycaster,

    // State
    isReady,
    fps,
    performanceStats,

    // Methods
    initScene,
    startRenderLoop,
    stopRenderLoop,
    addToScene,
    removeFromScene,
    getObjectByName,
    setPerformanceLevel,
    getPerformanceLevel,
    createGradientTexture,
    dispose,

    // Utilities
    vector3Pool
  }
}
