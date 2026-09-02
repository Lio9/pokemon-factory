/**
 * @description Vue 3 composable for managing Three.js scene lifecycle
 * @description Vue 3 组合式函数，用于管理 Three.js 场景生命周期
 * 
 * @author MiMo-v2.5-pro
 * @version 1.0.0
 */

import { ref, type Ref, onUnmounted, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

/**
 * Scene configuration options
 * 场景配置选项
 */
interface SceneOptions {
  /** Enable shadow mapping / 启用阴影映射 */
  enableShadows?: boolean
  /** Camera field of view / 相机视野角度 */
  fov?: number
  /** Camera near clipping plane / 相机近裁剪面 */
  near?: number
  /** Camera far clipping plane / 相机远裁剪面 */
  far?: number
  /** Background color / 背景颜色 */
  backgroundColor?: number | string
  /** Enable orbit controls / 启用轨道控制器 */
  enableControls?: boolean
}

/**
 * useThreeScene composable return type
 * useThreeScene 组合式函数返回类型
 */
interface UseThreeSceneReturn {
  /** Three.js scene / Three.js 场景 */
  scene: Ref<THREE.Scene | null>
  /** Perspective camera / 透视相机 */
  camera: Ref<THREE.PerspectiveCamera | null>
  /** WebGL renderer / WebGL 渲染器 */
  renderer: Ref<THREE.WebGLRenderer | null>
  /** Orbit controls / 轨道控制器 */
  controls: Ref<OrbitControls | null>
  /** Raycaster for mouse picking / 射线投射器，用于鼠标拾取 */
  raycaster: THREE.Raycaster
  /** FPS counter / FPS 计数器 */
  fps: Ref<number>
  /** Add object to scene / 向场景添加对象 */
  addToScene: (object: THREE.Object3D) => void
  /** Remove object from scene / 从场景移除对象 */
  removeFromScene: (object: THREE.Object3D) => void
  /** Get object by name / 根据名称获取对象 */
  getObjectByName: (name: string) => THREE.Object3D | undefined
  /** Start render loop / 启动渲染循环 */
  startRenderLoop: () => void
  /** Stop render loop / 停止渲染循环 */
  stopRenderLoop: () => void
  /** Dispose all resources / 销毁所有资源 */
  dispose: () => void
  /** Whether scene is ready / 场景是否就绪 */
  isReady: Ref<boolean>
}

/**
 * useThreeScene composable
 * useThreeScene 组合式函数
 * 
 * @param container - DOM container reference / DOM 容器引用
 * @param options - Scene configuration / 场景配置
 * @returns Scene management methods and references / 场景管理方法和引用
 * 
 * @example
 * ```typescript
 * const containerRef = ref<HTMLElement | null>(null)
 * const { scene, camera, addToScene, startRenderLoop } = useThreeScene(containerRef)
 * ```
 */
export function useThreeScene(
  container: Ref<HTMLElement | null>,
  options: SceneOptions = {}
): UseThreeSceneReturn {
  // Refs
  const scene = ref<THREE.Scene | null>(null)
  const camera = ref<THREE.PerspectiveCamera | null>(null)
  const renderer = ref<THREE.WebGLRenderer | null>(null)
  const controls = ref<OrbitControls | null>(null)
  const fps = ref<number>(0)
  const isReady = ref<boolean>(false)

  // Raycaster
  const raycaster = new THREE.Raycaster()

  // Internal state
  let animationFrameId: number | null = null
  let isPaused = false
  let frameCount = 0
  let lastFpsTime = performance.now()

  // Default options
  const {
    enableShadows = true,
    fov = 75,
    near = 0.1,
    far = 1000,
    backgroundColor = 0x1a1a2e,
    enableControls = true
  } = options

  /**
   * Initialize Three.js scene
   * 初始化 Three.js 场景
   */
  const initScene = (): void => {
    if (!container.value) {
      console.warn('Container element not found')
      return
    }

    // Create scene
    // 创建场景
    scene.value = new THREE.Scene()
    scene.value.background = new THREE.Color(backgroundColor)

    // Create camera
    // 创建相机
    const aspect = container.value.clientWidth / container.value.clientHeight
    camera.value = new THREE.PerspectiveCamera(fov, aspect, near, far)
    camera.value.position.set(5, 5, 10)
    camera.value.lookAt(0, 0, 0)

    // Create renderer
    // 创建渲染器
    renderer.value = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true
    })
    renderer.value.setSize(container.value.clientWidth, container.value.clientHeight)
    renderer.value.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    
    // Configure shadows
    // 配置阴影
    if (enableShadows) {
      renderer.value.shadowMap.enabled = true
      renderer.value.shadowMap.type = THREE.PCFSoftShadowMap
    }

    // Append renderer to container
    // 将渲染器添加到容器
    container.value.appendChild(renderer.value.domElement)

    // Setup controls
    // 设置控制器
    if (enableControls && renderer.value && camera.value) {
      controls.value = new OrbitControls(camera.value, renderer.value.domElement)
      controls.value.enableDamping = true
      controls.value.dampingFactor = 0.05
      controls.value.screenSpacePanning = false
      controls.value.minDistance = 1
      controls.value.maxDistance = 100
    }

    // Setup lighting
    // 设置光照
    setupLighting()

    // Mark as ready
    // 标记为就绪
    isReady.value = true

    // Setup resize handler
    // 设置窗口大小变化处理
    window.addEventListener('resize', handleResize)
  }

  /**
   * Setup scene lighting
   * 设置场景光照
   */
  const setupLighting = (): void => {
    if (!scene.value) return

    // Ambient light for overall illumination
    // 环境光用于整体照明
    const ambientLight = new THREE.AmbientLight(0x404040, 0.6)
    scene.value.add(ambientLight)

    // Directional light for shadows and depth
    // 方向光用于阴影和深度感
    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
    directionalLight.position.set(10, 20, 10)
    directionalLight.castShadow = enableShadows
    
    if (enableShadows) {
      directionalLight.shadow.mapSize.width = 2048
      directionalLight.shadow.mapSize.height = 2048
      directionalLight.shadow.camera.near = 0.5
      directionalLight.shadow.camera.far = 50
      directionalLight.shadow.camera.left = -20
      directionalLight.shadow.camera.right = 20
      directionalLight.shadow.camera.top = 20
      directionalLight.shadow.camera.bottom = -20
    }
    
    scene.value.add(directionalLight)

    // Hemisphere light for natural outdoor lighting
    // 半球光用于自然室外光照
    const hemisphereLight = new THREE.HemisphereLight(0x87ceeb, 0x362907, 0.4)
    scene.value.add(hemisphereLight)
  }

  /**
   * Handle window resize
   * 处理窗口大小变化
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
   * Update FPS counter
   * 更新 FPS 计数器
   */
  const updateFps = (): void => {
    frameCount++
    const currentTime = performance.now()
    const elapsed = currentTime - lastFpsTime

    // Update FPS every second
    // 每秒更新 FPS
    if (elapsed >= 1000) {
      fps.value = Math.round((frameCount * 1000) / elapsed)
      frameCount = 0
      lastFpsTime = currentTime
    }
  }

  /**
   * Render loop
   * 渲染循环
   */
  const animate = (): void => {
    if (isPaused) return

    animationFrameId = requestAnimationFrame(animate)

    // Update controls
    // 更新控制器
    if (controls.value) {
      controls.value.update()
    }

    // Render scene
    // 渲染场景
    if (renderer.value && scene.value && camera.value) {
      renderer.value.render(scene.value, camera.value)
    }

    // Update FPS
    // 更新 FPS
    updateFps()
  }

  /**
   * Start render loop
   * 启动渲染循环
   */
  const startRenderLoop = (): void => {
    if (animationFrameId !== null) {
      console.warn('Render loop already running')
      return
    }

    isPaused = false
    lastFpsTime = performance.now()
    frameCount = 0
    animate()
  }

  /**
   * Stop render loop
   * 停止渲染循环
   */
  const stopRenderLoop = (): void => {
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
    isPaused = true
  }

  /**
   * Add object to scene
   * 向场景添加对象
   * 
   * @param object - Three.js object to add / 要添加的 Three.js 对象
   */
  const addToScene = (object: THREE.Object3D): void => {
    if (!scene.value) {
      console.warn('Scene not initialized')
      return
    }
    scene.value.add(object)
  }

  /**
   * Remove object from scene
   * 从场景移除对象
   * 
   * @param object - Three.js object to remove / 要移除的 Three.js 对象
   */
  const removeFromScene = (object: THREE.Object3D): void => {
    if (!scene.value) {
      console.warn('Scene not initialized')
      return
    }
    scene.value.remove(object)
  }

  /**
   * Get object by name
   * 根据名称获取对象
   * 
   * @param name - Object name / 对象名称
   * @returns Found object or undefined / 找到的对象或 undefined
   */
  const getObjectByName = (name: string): THREE.Object3D | undefined => {
    if (!scene.value) {
      console.warn('Scene not initialized')
      return undefined
    }
    return scene.value.getObjectByName(name)
  }

  /**
   * Dispose all Three.js resources
   * 销毁所有 Three.js 资源
   */
  const dispose = (): void => {
    // Stop render loop
    // 停止渲染循环
    stopRenderLoop()

    // Remove event listener
    // 移除事件监听器
    window.removeEventListener('resize', handleResize)

    // Dispose controls
    // 销毁控制器
    if (controls.value) {
      controls.value.dispose()
      controls.value = null
    }

    // Dispose renderer
    // 销毁渲染器
    if (renderer.value) {
      renderer.value.dispose()
      if (container.value && renderer.value.domElement.parentNode === container.value) {
        container.value.removeChild(renderer.value.domElement)
      }
      renderer.value = null
    }

    // Dispose scene
    // 销毁场景
    if (scene.value) {
      scene.value.clear()
      scene.value = null
    }

    // Reset camera
    // 重置相机
    camera.value = null

    // Mark as not ready
    // 标记为未就绪
    isReady.value = false
  }

  // Watch for container changes
  // 监听容器变化
  watch(
    container,
    (newContainer) => {
      if (newContainer) {
        // Dispose existing scene if any
        // 如果存在则销毁现有场景
        if (scene.value) {
          dispose()
        }
        // Initialize new scene
        // 初始化新场景
        initScene()
        startRenderLoop()
      }
    },
    { immediate: true }
  )

  // Cleanup on unmount
  // 组件卸载时清理
  onUnmounted(() => {
    dispose()
  })

  return {
    scene,
    camera,
    renderer,
    controls,
    raycaster,
    fps,
    addToScene,
    removeFromScene,
    getObjectByName,
    startRenderLoop,
    stopRenderLoop,
    dispose,
    isReady
  }
}
