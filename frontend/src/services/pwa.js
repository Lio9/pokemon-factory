/**
 * ============================================================
 * PWA Service Worker / PWA Service Worker
 * ============================================================
 *
 * 提供 PWA Service Worker 的注册、更新管理和通知权限请求。
 * Manages PWA Service Worker registration, update handling, and notification permissions.
 *
 * @module services/pwa
 * Service Worker 注册工具
 */

export function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      const swUrl = '/sw.js'
      
      navigator.serviceWorker
        .register(swUrl)
        .then((registration) => {
          console.log('[PWA] Service Worker registered:', registration.scope)
          
          // 检查更新
          registration.addEventListener('updatefound', () => {
            const newWorker = registration.installing
            console.log('[PWA] New Service Worker installing...')
            
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                // 有新版本可用，提示用户刷新
                showUpdateNotification()
              }
            })
          })
        })
        .catch((error) => {
          console.error('[PWA] Service Worker registration failed:', error)
        })
    })
  }
}

export function unregisterServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.ready.then((registration) => {
      registration.unregister()
      console.log('[PWA] Service Worker unregistered')
    })
  }
}

function showUpdateNotification() {
  // 检查是否有通知权限
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('Pokemon Factory', {
      body: '新版本已就绪！刷新页面以获取最新功能。',
      icon: '/icon-192.png',
      badge: '/badge-72.png'
    })
  } else {
    // 显示应用内通知
    const toast = document.createElement('div')
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: #42b983;
      color: white;
      padding: 1rem 2rem;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: 9999;
      cursor: pointer;
      animation: slideIn 0.3s ease-out;
    `
    toast.textContent = '🎉 新版本可用！点击刷新'
    toast.onclick = () => window.location.reload()
    
    document.body.appendChild(toast)
    
    setTimeout(() => {
      toast.remove()
    }, 5000)
  }
}

// 请求通知权限
export function requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().then((permission) => {
      console.log('[PWA] Notification permission:', permission)
    })
  }
}

// 检查是否支持PWA
export function isPWASupported() {
  return (
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  )
}

// 检查是否在独立模式（已安装）
export function isStandalone() {
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    window.navigator.standalone === true
  )
}
