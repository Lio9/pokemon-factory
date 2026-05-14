/**
 * PWA 离线数据管理
 * 提供离线缓存、后台同步等功能
 */

const CACHE_VERSION = 'v1'
const OFFLINE_DATA_KEY = 'pokemon-factory-offline-data'

/**
 * 离线数据存储
 */
export class OfflineDataManager {
  constructor() {
    this.dbName = 'PokemonFactoryDB'
    this.version = 1
    this.db = null
  }
  
  /**
   * 初始化IndexedDB
   */
  async init() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.version)
      
      request.onerror = () => reject(request.error)
      request.onsuccess = () => {
        this.db = request.result
        resolve(this.db)
      }
      
      request.onupgradeneeded = (event) => {
        const db = event.target.result
        
        // 创建宝可梦缓存表
        if (!db.objectStoreNames.contains('pokemon')) {
          db.createObjectStore('pokemon', { keyPath: 'id' })
        }
        
        // 创建招式缓存表
        if (!db.objectStoreNames.contains('moves')) {
          db.createObjectStore('moves', { keyPath: 'id' })
        }
        
        // 创建特性缓存表
        if (!db.objectStoreNames.contains('abilities')) {
          db.createObjectStore('abilities', { keyPath: 'id' })
        }
        
        // 创建对战记录表
        if (!db.objectStoreNames.contains('battles')) {
          const battleStore = db.createObjectStore('battles', { keyPath: 'id', autoIncrement: true })
          battleStore.createIndex('timestamp', 'timestamp', { unique: false })
          battleStore.createIndex('synced', 'synced', { unique: false })
        }
      }
    })
  }
  
  /**
   * 缓存宝可梦数据
   */
  async cachePokemon(pokemonList) {
    if (!this.db) await this.init()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['pokemon'], 'readwrite')
      const store = transaction.objectStore('pokemon')
      
      pokemonList.forEach(pokemon => {
        store.put({
          ...pokemon,
          cachedAt: Date.now()
        })
      })
      
      transaction.oncomplete = () => resolve()
      transaction.onerror = () => reject(transaction.error)
    })
  }
  
  /**
   * 获取缓存的宝可梦数据
   */
  async getCachedPokemon(id) {
    if (!this.db) await this.init()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['pokemon'], 'readonly')
      const store = transaction.objectStore('pokemon')
      const request = store.get(id)
      
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }
  
  /**
   * 缓存对战记录（离线时）
   */
  async queueBattle(battleData) {
    if (!this.db) await this.init()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['battles'], 'readwrite')
      const store = transaction.objectStore('battles')
      
      const record = {
        ...battleData,
        timestamp: Date.now(),
        synced: false
      }
      
      const request = store.add(record)
      
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }
  
  /**
   * 获取待同步的对战记录
   */
  async getPendingBattles() {
    if (!this.db) await this.init()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['battles'], 'readonly')
      const store = transaction.objectStore('battles')
      const index = store.index('synced')
      const request = index.getAll(false)
      
      request.onsuccess = () => resolve(request.result || [])
      request.onerror = () => reject(request.error)
    })
  }
  
  /**
   * 标记对战记录为已同步
   */
  async markBattleSynced(battleId) {
    if (!this.db) await this.init()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['battles'], 'readwrite')
      const store = transaction.objectStore('battles')
      const request = store.get(battleId)
      
      request.onsuccess = () => {
        const data = request.result
        if (data) {
          data.synced = true
          store.put(data)
        }
        resolve()
      }
      request.onerror = () => reject(request.error)
    })
  }
  
  /**
   * 清理过期缓存
   */
  async cleanExpiredCache(maxAge = 7 * 24 * 60 * 60 * 1000) { // 默认7天
    if (!this.db) await this.init()
    
    const cutoffTime = Date.now() - maxAge
    
    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction(['pokemon'], 'readwrite')
      const store = transaction.objectStore('pokemon')
      const request = store.getAll()
      
      request.onsuccess = () => {
        const records = request.result || []
        records.forEach(record => {
          if (record.cachedAt < cutoffTime) {
            store.delete(record.id)
          }
        })
        resolve()
      }
      request.onerror = () => reject(request.error)
    })
  }
}

/**
 * 后台同步管理器
 */
export class BackgroundSyncManager {
  constructor() {
    this.offlineData = new OfflineDataManager()
  }
  
  /**
   * 注册后台同步
   */
  async registerSync(tag = 'battle-sync') {
    if ('serviceWorker' in navigator && 'SyncManager' in window) {
      try {
        const registration = await navigator.serviceWorker.ready
        await registration.sync.register(tag)
        console.log('[PWA] Background sync registered:', tag)
        return true
      } catch (error) {
        console.error('[PWA] Failed to register sync:', error)
        return false
      }
    }
    return false
  }
  
  /**
   * 同步待处理的对战记录
   */
  async syncPendingBattles(apiClient) {
    try {
      const pendingBattles = await this.offlineData.getPendingBattles()
      
      if (pendingBattles.length === 0) {
        console.log('[PWA] No pending battles to sync')
        return
      }
      
      console.log(`[PWA] Syncing ${pendingBattles.length} pending battles...`)
      
      for (const battle of pendingBattles) {
        try {
          // 调用API同步数据
          await apiClient.syncBattle(battle)
          
          // 标记为已同步
          await this.offlineData.markBattleSynced(battle.id)
          
          console.log(`[PWA] Battle ${battle.id} synced successfully`)
        } catch (error) {
          console.error(`[PWA] Failed to sync battle ${battle.id}:`, error)
        }
      }
      
      console.log('[PWA] Sync completed')
    } catch (error) {
      console.error('[PWA] Sync failed:', error)
      throw error
    }
  }
}

/**
 * 网络状态监听
 */
export class NetworkStatusMonitor {
  constructor() {
    this.listeners = []
    this.isOnline = navigator.onLine
    
    this.setupListeners()
  }
  
  setupListeners() {
    window.addEventListener('online', () => {
      this.isOnline = true
      this.notifyListeners(true)
      console.log('[PWA] Connection restored')
    })
    
    window.addEventListener('offline', () => {
      this.isOnline = false
      this.notifyListeners(false)
      console.log('[PWA] Connection lost')
    })
  }
  
  onStatusChange(callback) {
    this.listeners.push(callback)
    
    // 返回取消订阅函数
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback)
    }
  }
  
  notifyListeners(isOnline) {
    this.listeners.forEach(callback => callback(isOnline))
  }
  
  getStatus() {
    return this.isOnline
  }
}

/**
 * 推送通知管理器
 */
export class PushNotificationManager {
  constructor() {
    this.vapidPublicKey = null
  }
  
  /**
   * 设置VAPID公钥
   */
  setVapidPublicKey(key) {
    this.vapidPublicKey = key
  }
  
  /**
   * 请求通知权限
   */
  async requestPermission() {
    if (!('Notification' in window)) {
      console.warn('[PWA] Notifications not supported')
      return false
    }
    
    const permission = await Notification.requestPermission()
    return permission === 'granted'
  }
  
  /**
   * 订阅推送
   */
  async subscribe() {
    if (!this.vapidPublicKey) {
      throw new Error('VAPID public key not set')
    }
    
    const registration = await navigator.serviceWorker.ready
    
    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey)
    })
    
    console.log('[PWA] Push subscription:', subscription)
    return subscription
  }
  
  /**
   * 取消订阅
   */
  async unsubscribe() {
    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.getSubscription()
    
    if (subscription) {
      await subscription.unsubscribe()
      console.log('[PWA] Unsubscribed from push notifications')
    }
  }
  
  /**
   * 显示本地通知
   */
  showNotification(title, options = {}) {
    if ('Notification' in window && Notification.permission === 'granted') {
      const defaultOptions = {
        icon: '/icon-192.png',
        badge: '/badge-72.png',
        vibrate: [200, 100, 200],
        ...options
      }
      
      return new Notification(title, defaultOptions)
    }
  }
  
  /**
   * Base64转Uint8Array
   */
  urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4)
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
    
    const rawData = window.atob(base64)
    const outputArray = new Uint8Array(rawData.length)
    
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i)
    }
    
    return outputArray
  }
}

// 导出单例
export const offlineData = new OfflineDataManager()
export const backgroundSync = new BackgroundSyncManager()
export const networkMonitor = new NetworkStatusMonitor()
export const pushNotifications = new PushNotificationManager()
