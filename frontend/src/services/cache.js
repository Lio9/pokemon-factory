/**
 * ============================================================
 * 数据缓存服务 / Data Cache Service
 * ============================================================
 *
 * ## 架构定位 / Architecture
 *
 *   Views ──getOrFetch()──▶ DataCache ──get/set──▶ Memory (LRU)
 *                             │
 *                             ├── IndexedDB (Persistent)
 *                             └── fetch() (Network fallback)
 *
 * ## 缓存层次 / Cache Levels (L1 → L2 → L3)
 *   L1: 内存 LRU (最快 / Fastest, in-process)
 *   L2: IndexedDB (跨会话持久 / Cross-session)
 *   L3: fetchFn (网络 / Network, only when L1+L2 miss)
 *
 * ## 过期策略 / Expiry
 *   short=2min normal=10min long=30min
 *
 * ## 并发控制 / Concurrency
 *   相同 key 的并发 getOrFetch 自动合并为一次请求
 *   Concurrent getOrFetch with same key auto-merge
 * 
 * 核心功能：
 * - 智能多级缓存（内存 + IndexedDB）
 * - 请求去重，避免重复并发请求
 * - LRU 缓存淘汰策略
 * - 图片预加载和缓存
 * - 批量请求优化
 * 
 * @module services/cache
 */

class DataCache {
  constructor() {
    // 内存缓存
    this.cache = new Map()
    this.imageCache = new Map()
    this.pendingRequests = new Map()
    
    // LRU 追踪
    this.accessOrder = new Map()
    
    // 配置
    this.config = {
      maxCacheSize: 200,
      cacheExpiry: {
        short: 2 * 60 * 1000,
        normal: 10 * 60 * 1000,
        long: 30 * 60 * 1000
      },
      maxImagePreload: 8,
      enableIndexedDB: true,
      hitLogEnabled: true
    }
    
    // 统计
    this.stats = {
      hits: 0,
      misses: 0,
      requests: 0
    }
    
    // IndexedDB 持久化缓存
    this.db = null
    this.dbName = 'PokemonFactoryCache'
    this.dbVersion = 2
    this.initIndexedDB()
  }

  /**
   * 初始化 IndexedDB — 用于跨会话持久化缓存
   */
  async initIndexedDB() {
    if (!this.config.enableIndexedDB) return
    
    try {
      const request = indexedDB.open(this.dbName, this.dbVersion)
      
      request.onupgradeneeded = (event) => {
        const db = event.target.result
        if (!db.objectStoreNames.contains('cache')) {
          const store = db.createObjectStore('cache', { keyPath: 'key' })
          store.createIndex('timestamp', 'timestamp', { unique: false })
          store.createIndex('expiry', 'expiry', { unique: false })
        }
      }
      
      this.db = await new Promise((resolve, reject) => {
        request.onsuccess = () => resolve(request.result)
        request.onerror = () => {
          console.warn('IndexedDB not available, using memory-only cache')
          resolve(null)
        }
      })

      // 定期清理过期 IndexedDB 条目
      if (this.db) {
        this.cleanupIndexedDB()
      }
    } catch (e) {
      console.warn('IndexedDB init failed, using memory-only cache')
    }
  }

  /**
   * 从 IndexedDB 读取 (如果内存未命中)
   */
  async readFromIndexedDB(key) {
    if (!this.db) return null
    try {
      const tx = this.db.transaction('cache', 'readonly')
      const store = tx.objectStore('cache')
      const result = await new Promise((resolve, reject) => {
        const request = store.get(key)
        request.onsuccess = () => resolve(request.result)
        request.onerror = () => reject(null)
      })
      if (result && result.expiry > Date.now()) {
        return result.data
      }
      // 过期则删除
      if (result) {
        this.deleteFromIndexedDB(key)
      }
      return null
    } catch {
      return null
    }
  }

  /**
   * 写入 IndexedDB (异步，不阻塞)
   */
  writeToIndexedDB(key, data, expiryMs) {
    if (!this.db) return
    try {
      const tx = this.db.transaction('cache', 'readwrite')
      const store = tx.objectStore('cache')
      store.put({
        key,
        data,
        timestamp: Date.now(),
        expiry: Date.now() + expiryMs
      })
    } catch { /* silent */ }
  }

  /**
   * 从 IndexedDB 删除
   */
  deleteFromIndexedDB(key) {
    if (!this.db) return
    try {
      const tx = this.db.transaction('cache', 'readwrite')
      const store = tx.objectStore('cache')
      store.delete(key)
    } catch { /* silent */ }
  }

  /**
   * 清理过期 IndexedDB 条目
   */
  async cleanupIndexedDB() {
    if (!this.db) return
    try {
      const tx = this.db.transaction('cache', 'readwrite')
      const store = tx.objectStore('cache')
      const index = store.index('expiry')
      const now = Date.now()
      const range = IDBKeyRange.upperBound(now)
      const request = index.openCursor(range)
      request.onsuccess = (event) => {
        const cursor = event.target.result
        if (cursor) {
          store.delete(cursor.primaryKey)
          cursor.continue()
        }
      }
    } catch { /* silent */ }
  }

  /**
   * 生成缓存键
   */
  generateKey(type, params) {
    return `${type}:${JSON.stringify(params)}`
  }

  /**
   * 更新访问顺序（LRU）
   */
  updateAccessOrder(key) {
    this.accessOrder.delete(key)
    this.accessOrder.set(key, Date.now())
  }

  /**
   * 从内存获取
   */
  get(type, params, expiryMode = 'normal') {
    const key = this.generateKey(type, params)
    const item = this.cache.get(key)
    
    if (item) {
      const expiry = this.config.cacheExpiry[expiryMode]
      if (Date.now() - item.timestamp < expiry) {
        this.stats.hits++
        this.updateAccessOrder(key)
        return item.data
      }
      this.cache.delete(key)
      this.accessOrder.delete(key)
    }
    
    this.stats.misses++
    return null
  }

  /**
   * 设置内存缓存
   */
  set(type, params, data) {
    const key = this.generateKey(type, params)
    
    if (this.cache.size >= this.config.maxCacheSize) {
      const oldestKey = this.accessOrder.keys().next().value
      if (oldestKey) {
        this.cache.delete(oldestKey)
        this.accessOrder.delete(oldestKey)
      }
    }
    
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
    this.updateAccessOrder(key)
  }

  /**
   * 获取或请求 — 内存 → IndexedDB → 网络
   */
  async getOrFetch(type, params, fetchFn, expiryMode = 'normal') {
    this.stats.requests++
    const key = this.generateKey(type, params)
    const expiry = this.config.cacheExpiry[expiryMode]

    // 1. 内存缓存
    const memCached = this.cache.get(key)
    if (memCached && Date.now() - memCached.timestamp < expiry) {
      this.stats.hits++
      this.updateAccessOrder(key)
      return memCached.data
    }

    // 2. IndexedDB 持久缓存
    if (this.config.enableIndexedDB) {
      const idbCached = await this.readFromIndexedDB(key)
      if (idbCached !== null) {
        // 回填到内存
        this.cache.set(key, { data: idbCached, timestamp: Date.now() })
        this.updateAccessOrder(key)
        this.stats.hits++
        return idbCached
      }
    }

    this.stats.misses++

    // 3. 等待进行中的相同请求
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key)
    }

    // 4. 发起新请求
    const promise = fetchFn()
      .then(data => {
        this.set(type, params, data)
        // 异步写入 IndexedDB
        if (this.config.enableIndexedDB) {
          this.writeToIndexedDB(key, data, expiry)
        }
        this.pendingRequests.delete(key)
        return data
      })
      .catch(error => {
        this.pendingRequests.delete(key)
        throw error
      })

    this.pendingRequests.set(key, promise)
    return promise
  }

  /**
   * 批量获取或请求
   */
  async batchGetOrFetch(items, expiryMode = 'normal') {
    const results = []
    const pendingBatches = []

    for (const item of items) {
      const cached = this.get(item.type, item.params, expiryMode)
      if (cached) {
        results.push(cached)
      } else {
        pendingBatches.push(item)
      }
    }

    if (pendingBatches.length > 0) {
      const pendingResults = await Promise.all(
        pendingBatches.map(item =>
          this.getOrFetch(item.type, item.params, item.fetchFn, expiryMode)
        )
      )
      results.push(...pendingResults)
    }

    return results
  }

  /**
   * 预加载单个图片
   */
  preloadImage(url) {
    if (this.imageCache.has(url)) {
      return Promise.resolve(this.imageCache.get(url))
    }

    return new Promise((resolve, reject) => {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      
      img.onload = () => {
        this.imageCache.set(url, img)
        resolve(img)
      }
      img.onerror = () => {
        this.imageCache.delete(url)
        reject(new Error(`Image load failed: ${url}`))
      }
      img.src = url
    })
  }

  /**
   * 批量预加载图片 — 并发控制
   */
  async preloadImages(urls) {
    const uncachedUrls = urls.filter(url => !this.imageCache.has(url))
    if (uncachedUrls.length === 0) {
      return urls.map(url => this.imageCache.get(url))
    }

    const results = []
    const maxConcurrent = this.config.maxImagePreload
    
    for (let i = 0; i < uncachedUrls.length; i += maxConcurrent) {
      const batch = uncachedUrls.slice(i, i + maxConcurrent)
      const batchResults = await Promise.allSettled(
        batch.map(url => this.preloadImage(url))
      )
      results.push(...batchResults)
    }

    return urls.map(url => this.imageCache.get(url))
  }

  /**
   * 缓存统计
   */
  getStats() {
    const hitRate = this.stats.requests > 0
      ? (this.stats.hits / this.stats.requests * 100).toFixed(2)
      : '0.00'
    
    return {
      ...this.stats,
      hitRate: hitRate + '%',
      currentSize: this.cache.size,
      maxSize: this.config.maxCacheSize,
      imageCacheSize: this.imageCache.size
    }
  }

  /**
   * 清除所有缓存（内存 + IndexedDB）
   */
  clear() {
    this.cache.clear()
    this.imageCache.clear()
    this.accessOrder.clear()
    this.stats = { hits: 0, misses: 0, requests: 0 }

    if (this.db) {
      const tx = this.db.transaction('cache', 'readwrite')
      const store = tx.objectStore('cache')
      store.clear()
    }
  }

  /**
   * 清除特定类型缓存
   */
  clearType(type) {
    for (const key of this.cache.keys()) {
      if (key.startsWith(`${type}:`)) {
        this.cache.delete(key)
        this.accessOrder.delete(key)
        this.deleteFromIndexedDB(key)
      }
    }
  }

  /**
   * 清除过期缓存
   */
  cleanExpired() {
    const now = Date.now()
    const maxExpiry = Math.max(...Object.values(this.config.cacheExpiry))
    for (const [key, item] of this.cache.entries()) {
      if (now - item.timestamp > maxExpiry) {
        this.cache.delete(key)
        this.accessOrder.delete(key)
      }
    }
  }
}

// 导出单例
export const dataCache = new DataCache()
export default dataCache

// 定期清理过期缓存（每5分钟）
if (typeof window !== 'undefined') {
  setInterval(() => {
    dataCache.cleanExpired()
  }, 5 * 60 * 1000)

  // 页面卸载前关闭 IndexedDB 连接
  window.addEventListener('beforeunload', () => {
    if (dataCache.db) {
      dataCache.db.close()
    }
  })
}
