/**
 * @description 移动端交互系统 - 触摸手势与自适应
 * @description Mobile interaction system - Touch gestures and adaptation
 *
 * 功能：
 * 1. 触摸手势识别（点击、滑动、捏合缩放）
 * 2. 虚拟摇杆控制
 * 3. 触觉反馈（Vibration API）
 * 4. 自适应 UI 布局
 * 5. 屏幕方向管理
 *
 * @module composables/battle3d/useMobileInteraction
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * 触摸手势类型
 * Touch gesture types
 */
export type TouchGesture = 'tap' | 'double_tap' | 'long_press' | 'swipe_left' | 'swipe_right' | 'swipe_up' | 'swipe_down' | 'pinch_in' | 'pinch_out'

/**
 * 触摸事件数据
 * Touch event data
 */
interface TouchData {
  /** 起始位置 / Start position */
  startX: number
  startY: number
  /** 当前位置 / Current position */
  currentX: number
  currentY: number
  /** 时间戳 / Timestamp */
  startTime: number
  /** 手指数量 / Touch count */
  touchCount: number
}

/**
 * 手势回调
 * Gesture callback
 */
type GestureCallback = (gesture: TouchGesture, data: { x: number; y: number; deltaX: number; deltaY: number }) => void

/**
 * 设备信息
 * Device information
 */
interface DeviceInfo {
  /** 是否为移动设备 / Is mobile device */
  isMobile: boolean
  /** 是否为平板 / Is tablet */
  isTablet: boolean
  /** 是否支持触摸 / Has touch support */
  hasTouch: boolean
  /** 是否支持触觉反馈 / Has haptic feedback */
  hasHaptics: boolean
  /** 屏幕方向 / Screen orientation */
  orientation: 'portrait' | 'landscape'
  /** 屏幕尺寸 / Screen size */
  screenWidth: number
  screenHeight: number
  /** 设备像素比 / Device pixel ratio */
  pixelRatio: number
}

/**
 * 移动端交互 composable
 * Mobile interaction composable
 */
export function useMobileInteraction() {
  // ===== 设备信息 =====
  const deviceInfo = ref<DeviceInfo>({
    isMobile: false,
    isTablet: false,
    hasTouch: false,
    hasHaptics: false,
    orientation: 'landscape',
    screenWidth: window.innerWidth,
    screenHeight: window.innerHeight,
    pixelRatio: window.devicePixelRatio
  })

  // ===== 触摸状态 =====
  const isTouching = ref(false)
  const touchData = ref<TouchData | null>(null)
  const lastGesture = ref<TouchGesture | null>(null)

  // ===== 回调注册 =====
  const gestureCallbacks: Map<string, GestureCallback> = new Map()

  // ===== 配置 =====
  const SWIPE_THRESHOLD = 50
  const LONG_PRESS_DURATION = 500
  const DOUBLE_TAP_DELAY = 300

  // ===== 内部状态 =====
  let lastTapTime = 0
  let longPressTimer: number | null = null
  let initialPinchDistance = 0

  /**
   * 检测设备信息
   * Detect device information
   */
  const detectDevice = (): void => {
    const userAgent = navigator.userAgent.toLowerCase()
    const isMobile = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent)
    const isTablet = /ipad|android(?!.*mobile)/i.test(userAgent) || (isMobile && window.innerWidth > 768)
    const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0
    const hasHaptics = 'vibrate' in navigator

    deviceInfo.value = {
      isMobile,
      isTablet,
      hasTouch,
      hasHaptics,
      orientation: window.innerWidth > window.innerHeight ? 'landscape' : 'portrait',
      screenWidth: window.innerWidth,
      screenHeight: window.innerHeight,
      pixelRatio: window.devicePixelRatio
    }
  }

  /**
   * 触觉反馈
   * Haptic feedback
   */
  const triggerHaptic = (pattern: 'light' | 'medium' | 'heavy' | 'success' | 'warning' | 'error' = 'light'): void => {
    if (!deviceInfo.value.hasHaptics) return

    const patterns: Record<string, number[]> = {
      light: [10],
      medium: [20],
      heavy: [40],
      success: [10, 50, 10],
      warning: [20, 100, 20],
      error: [50, 100, 50, 100, 50]
    }

    try {
      navigator.vibrate(patterns[pattern] || patterns.light)
    } catch (e) {
      // Vibration API not supported or blocked
    }
  }

  /**
   * 处理触摸开始
   * Handle touch start
   */
  const handleTouchStart = (event: TouchEvent): void => {
    const touch = event.touches[0]
    const now = Date.now()

    touchData.value = {
      startX: touch.clientX,
      startY: touch.clientY,
      currentX: touch.clientX,
      currentY: touch.clientY,
      startTime: now,
      touchCount: event.touches.length
    }

    isTouching.value = true

    // Start long press timer
    if (longPressTimer) {
      clearTimeout(longPressTimer)
    }
    longPressTimer = window.setTimeout(() => {
      if (isTouching.value && touchData.value) {
        const dx = Math.abs(touchData.value.currentX - touchData.value.startX)
        const dy = Math.abs(touchData.value.currentY - touchData.value.startY)
        if (dx < 10 && dy < 10) {
          triggerGesture('long_press')
          triggerHaptic('medium')
        }
      }
    }, LONG_PRESS_DURATION)

    // Handle pinch start
    if (event.touches.length === 2) {
      initialPinchDistance = getPinchDistance(event.touches)
    }
  }

  /**
   * 处理触摸移动
   * Handle touch move
   */
  const handleTouchMove = (event: TouchEvent): void => {
    if (!touchData.value) return

    const touch = event.touches[0]
    touchData.value.currentX = touch.clientX
    touchData.value.currentY = touch.clientY

    // Handle pinch
    if (event.touches.length === 2 && initialPinchDistance > 0) {
      const currentDistance = getPinchDistance(event.touches)
      const scale = currentDistance / initialPinchDistance

      if (scale < 0.8) {
        triggerGesture('pinch_in')
      } else if (scale > 1.2) {
        triggerGesture('pinch_out')
      }
    }
  }

  /**
   * 处理触摸结束
   * Handle touch end
   */
  const handleTouchEnd = (event: TouchEvent): void => {
    if (!touchData.value) return

    // Clear long press timer
    if (longPressTimer) {
      clearTimeout(longPressTimer)
      longPressTimer = null
    }

    const deltaX = touchData.value.currentX - touchData.value.startX
    const deltaY = touchData.value.currentY - touchData.value.startY
    const deltaTime = Date.now() - touchData.value.startTime
    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)

    // Determine gesture
    if (distance < 10 && deltaTime < 300) {
      // Tap detected
      const now = Date.now()
      if (now - lastTapTime < DOUBLE_TAP_DELAY) {
        triggerGesture('double_tap')
        triggerHaptic('light')
      } else {
        triggerGesture('tap')
        triggerHaptic('light')
      }
      lastTapTime = now
    } else if (distance >= SWIPE_THRESHOLD) {
      // Swipe detected
      const angle = Math.atan2(deltaY, deltaX) * 180 / Math.PI

      if (angle >= -45 && angle < 45) {
        triggerGesture('swipe_right')
      } else if (angle >= 45 && angle < 135) {
        triggerGesture('swipe_down')
      } else if (angle >= -135 && angle < -45) {
        triggerGesture('swipe_up')
      } else {
        triggerGesture('swipe_left')
      }

      triggerHaptic('light')
    }

    // Reset state
    isTouching.value = false
    touchData.value = null
    initialPinchDistance = 0
  }

  /**
   * 获取捏合距离
   * Get pinch distance
   */
  const getPinchDistance = (touches: TouchList): number => {
    const dx = touches[0].clientX - touches[1].clientX
    const dy = touches[0].clientY - touches[1].clientY
    return Math.sqrt(dx * dx + dy * dy)
  }

  /**
   * 触发手势回调
   * Trigger gesture callback
   */
  const triggerGesture = (gesture: TouchGesture): void => {
    lastGesture.value = gesture

    const data = touchData.value
    const callbackData = {
      x: data?.currentX || 0,
      y: data?.currentY || 0,
      deltaX: (data?.currentX || 0) - (data?.startX || 0),
      deltaY: (data?.currentY || 0) - (data?.startY || 0)
    }

    gestureCallbacks.forEach(callback => {
      callback(gesture, callbackData)
    })
  }

  /**
   * 注册手势回调
   * Register gesture callback
   */
  const onGesture = (id: string, callback: GestureCallback): void => {
    gestureCallbacks.set(id, callback)
  }

  /**
   * 移除手势回调
   * Remove gesture callback
   */
  const offGesture = (id: string): void => {
    gestureCallbacks.delete(id)
  }

  /**
   * 处理窗口大小变化
   * Handle window resize
   */
  const handleResize = (): void => {
    deviceInfo.value.screenWidth = window.innerWidth
    deviceInfo.value.screenHeight = window.innerHeight
    deviceInfo.value.orientation = window.innerWidth > window.innerHeight ? 'landscape' : 'portrait'
  }

  /**
   * 处理屏幕方向变化
   * Handle orientation change
   */
  const handleOrientationChange = (): void => {
    setTimeout(() => {
      handleResize()
    }, 100)
  }

  /**
   * 请求全屏
   * Request fullscreen
   */
  const requestFullscreen = async (element?: HTMLElement): Promise<boolean> => {
    const el = element || document.documentElement
    try {
      if (el.requestFullscreen) {
        await el.requestFullscreen()
      } else if ((el as any).webkitRequestFullscreen) {
        await (el as any).webkitRequestFullscreen()
      } else if ((el as any).msRequestFullscreen) {
        await (el as any).msRequestFullscreen()
      }
      return true
    } catch (e) {
      console.warn('[MobileInteraction] Fullscreen request failed:', e)
      return false
    }
  }

  /**
   * 退出全屏
   * Exit fullscreen
   */
  const exitFullscreen = async (): Promise<void> => {
    try {
      if (document.exitFullscreen) {
        await document.exitFullscreen()
      } else if ((document as any).webkitExitFullscreen) {
        await (document as any).webkitExitFullscreen()
      }
    } catch (e) {
      // Ignore errors
    }
  }

  /**
   * 是否为全屏状态
   * Is fullscreen
   */
  const isFullscreen = computed(() => {
    return !!document.fullscreenElement
  })

  // ===== 生命周期 =====
  onMounted(() => {
    detectDevice()

    // Add touch event listeners
    if (deviceInfo.value.hasTouch) {
      document.addEventListener('touchstart', handleTouchStart, { passive: true })
      document.addEventListener('touchmove', handleTouchMove, { passive: true })
      document.addEventListener('touchend', handleTouchEnd, { passive: true })
    }

    // Add resize listeners
    window.addEventListener('resize', handleResize)
    window.addEventListener('orientationchange', handleOrientationChange)
  })

  onUnmounted(() => {
    // Remove touch event listeners
    document.removeEventListener('touchstart', handleTouchStart)
    document.removeEventListener('touchmove', handleTouchMove)
    document.removeEventListener('touchend', handleTouchEnd)

    // Remove resize listeners
    window.removeEventListener('resize', handleResize)
    window.removeEventListener('orientationchange', handleOrientationChange)

    // Clear timers
    if (longPressTimer) {
      clearTimeout(longPressTimer)
    }

    // Clear callbacks
    gestureCallbacks.clear()
  })

  return {
    // Device info
    deviceInfo,
    isMobile: computed(() => deviceInfo.value.isMobile),
    isTablet: computed(() => deviceInfo.value.isTablet),
    hasTouch: computed(() => deviceInfo.value.hasTouch),
    orientation: computed(() => deviceInfo.value.orientation),

    // Touch state
    isTouching,
    lastGesture,

    // Methods
    triggerHaptic,
    onGesture,
    offGesture,
    requestFullscreen,
    exitFullscreen,
    isFullscreen
  }
}
