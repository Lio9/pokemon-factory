/**
 * 数据缓存服务 - 优化版
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
      maxCacheSize: 200, // 增加缓存容量
      cacheExpiry: {
        short: 2 * 60 * 1000,    // 2分钟 - 频繁变化数据
        normal: 10 * 60 * 1000, // 10分钟 - 常规数据
        long: 30 * 60 * 1000    // 30分钟 - 静态数据
      },
      maxImagePreload: 8, // 图片并发预加载数
      enableIndexedDB: false, // 可选持久化缓存
      hitLogEnabled: true // 缓存命中统计
    }
    
    // 统计
    this.stats = {
      hits: 0,
      misses: 0,
      requests: 0
    }
    
    // IndexedDB 相关（可选）
    this.db = null
    this.dbName = 'PokemonFactoryCache'
    this.initIndexedDB()
  }

  /**
   * 初始化 IndexedDB（可选）
   */
  async initIndexedDB() {
    if (!this.config.enableIndexedDB) return
    
    try {
      const request = indexedDB.open(this.dbName, 1)
      
      request.onupgradeneeded = (event) => {
        const db = event.target.result
        if (!db.objectStoreNames.contains('cache')) {
          db.createObjectStore('cache')
        }
      }
      
      this.db = await new Promise((resolve, reject) => {
        request.onsuccess = () => resolve(request.result)
        request.onerror = () => reject(request.error)
      })
    } catch (e) {
      console.warn('IndexedDB not available, using memory-only cache')
    }
  }

  /**
   * 生成缓存键
   * @param {string} type - 数据类型
   * @param {object} params - 参数
   * @returns {string} 缓存键
   */
  generateKey(type, params) {
    return `${type}:${JSON.stringify(params)}`
  }

  /**
   * 更新访问顺序（LRU）
   */
  updateAccessOrder(key) {
    // 删除旧位置
    this.accessOrder.delete(key)
    // 添加到最新位置
    this.accessOrder.set(key, Date.now())
  }

  /**
   * 获取缓存数据
   * @param {string} type - 数据类型
   * @param {object} params - 参数
   * @param {string} expiryMode - 过期模式: 'short' | 'normal' | 'long'
   * @returns {any} 缓存数据
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
      // 过期清理
      this.cache.delete(key)
      this.accessOrder.delete(key)
    }
    
    this.stats.misses++
    return null
  }

  /**
   * 设置缓存数据
   * @param {string} type - 数据类型
   * @param {object} params - 参数
   * @param {any} data - 数据
   */
  set(type, params, data) {
    const key = this.generateKey(type, params)
    
    // LRU 淘汰策略
    if (this.cache.size >= this.config.maxCacheSize) {
      const oldestKey = this.findOldestKey()
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
   * 找出最久未使用的键 — O(1)
   * Map 保持插入顺序，最早的键即为第一个
   */
  findOldestKey() {
    return this.accessOrder.keys().next().value || null
  }

  /**
   * 获取或请求数据 - 高性能版本
   * @param {string} type - 数据类型
   * @param {object} params - 参数
   * @param {Function} fetchFn - 获取函数
   * @param {string} expiryMode - 过期模式
   * @returns {Promise<any>}
   */
  async getOrFetch(type, params, fetchFn, expiryMode = 'normal') {
    this.stats.requests++
    
    const cached = this.get(type, params, expiryMode)
    if (cached) {
      return cached
    }

    // 检查并等待相同的进行中请求
    const key = this.generateKey(type, params)
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key)
    }

    // 发起新请求
    const promise = fetchFn()
      .then(data => {
        this.set(type, params, data)
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
   * 批量获取或请求数据
   * @param {Array} items - 数组项 { type, params, fetchFn }
   * @param {string} expiryMode - 过期模式
   * @returns {Promise<Array>}
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

    // 并发执行剩余请求
    if (pendingBatches.length > 0) {
      const pendingResults = await Promise.all(
        pendingBatches.map(item => this.getOrFetch(item.type, item.params, item.fetchFn, expiryMode))
      )
      results.push(...pendingResults)
    }

    return results
  }

  /**
   * 预加载单个图片
   * @param {string} url - 图片URL
   * @returns {Promise<HTMLImageElement>}
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
      img.onerror = (error) => {
        this.imageCache.delete(url)
        reject(error)
      }
      img.src = url
    })
  }

  /**
   * 批量预加载图片 - 智能控制并发
   * @param {Array} urls - 图片URL数组
   * @returns {Promise<Array>}
   */
  async preloadImages(urls) {
    // 过滤已缓存的
    const uncachedUrls = urls.filter(url => !this.imageCache.has(url))
    if (uncachedUrls.length === 0) {
      return urls.map(url => this.imageCache.get(url))
    }

    // 分批次并发加载
    const results = []
    const maxConcurrent = this.config.maxImagePreload
    
    for (let i = 0; i < uncachedUrls.length; i += maxConcurrent) {
      const batch = uncachedUrls.slice(i, i + maxConcurrent)
      const batchResults = await Promise.allSettled(
        batch.map(url => this.preloadImage(url))
      )
      results.push(...batchResults)
    }

    // 返回所有（包括已缓存的）
    return urls.map(url => this.imageCache.get(url))
  }

  /**
   * 获取缓存统计信息
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
   * 清除所有缓存
   */
  clear() {
    this.cache.clear()
    this.imageCache.clear()
    this.pendingRequests.clear()
    this.accessOrder.clear()
    this.stats = { hits: 0, misses: 0, requests: 0 }
  }

  /**
   * 清除特定类型的缓存
   */
  clearType(type) {
    for (const key of this.cache.keys()) {
      if (key.startsWith(`${type}:`)) {
        this.cache.delete(key)
        this.accessOrder.delete(key)
      }
    }
  }

  /**
   * 清除过期缓存
   */
  cleanExpired() {
    const now = Date.now()
    for (const [key, item] of this.cache.entries()) {
      const maxExpiry = Math.max(...Object.values(this.config.cacheExpiry))
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
}