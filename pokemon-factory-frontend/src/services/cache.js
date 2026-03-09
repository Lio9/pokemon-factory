/**
 * 数据缓存服务
 * 用于缓存API响应数据，减少重复请求
 */

class DataCache {
  constructor() {
    this.cache = new Map()
    this.imageCache = new Map()
    this.pendingRequests = new Map()
    this.maxCacheSize = 100
    this.cacheExpiry = 5 * 60 * 1000 // 5分钟过期
  }

  /**
   * 生成缓存key
   */
  generateKey(type, params) {
    return `${type}:${JSON.stringify(params)}`
  }

  /**
   * 获取缓存数据
   */
  get(type, params) {
    const key = this.generateKey(type, params)
    const item = this.cache.get(key)
    
    if (item && Date.now() - item.timestamp < this.cacheExpiry) {
      return item.data
    }
    
    // 清理过期缓存
    if (item) {
      this.cache.delete(key)
    }
    
    return null
  }

  /**
   * 设置缓存数据
   */
  set(type, params, data) {
    const key = this.generateKey(type, params)
    
    // 限制缓存大小
    if (this.cache.size >= this.maxCacheSize) {
      const firstKey = this.cache.keys().next().value
      this.cache.delete(firstKey)
    }
    
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  }

  /**
   * 获取或请求数据
   */
  async getOrFetch(type, params, fetchFn) {
    // 先检查缓存
    const cached = this.get(type, params)
    if (cached) {
      return cached
    }

    // 检查是否有相同的请求正在进行
    const key = this.generateKey(type, params)
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key)
    }

    // 发起新请求
    const promise = fetchFn().then(data => {
      this.set(type, params, data)
      this.pendingRequests.delete(key)
      return data
    }).catch(error => {
      this.pendingRequests.delete(key)
      throw error
    })

    this.pendingRequests.set(key, promise)
    return promise
  }

  /**
   * 预加载图片
   */
  preloadImage(url) {
    if (this.imageCache.has(url)) {
      return Promise.resolve(this.imageCache.get(url))
    }

    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => {
        this.imageCache.set(url, img)
        resolve(img)
      }
      img.onerror = reject
      img.src = url
    })
  }

  /**
   * 批量预加载图片
   */
  async preloadImages(urls, maxConcurrent = 6) {
    const batches = []
    for (let i = 0; i < urls.length; i += maxConcurrent) {
      batches.push(urls.slice(i, i + maxConcurrent))
    }

    const results = []
    for (const batch of batches) {
      const batchResults = await Promise.allSettled(
        batch.map(url => this.preloadImage(url))
      )
      results.push(...batchResults)
    }
    
    return results
  }

  /**
   * 清除所有缓存
   */
  clear() {
    this.cache.clear()
    this.imageCache.clear()
    this.pendingRequests.clear()
  }

  /**
   * 清除特定类型的缓存
   */
  clearType(type) {
    for (const key of this.cache.keys()) {
      if (key.startsWith(type)) {
        this.cache.delete(key)
      }
    }
  }
}

// 导出单例
export const dataCache = new DataCache()
export default dataCache
