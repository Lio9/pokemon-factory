/**
 * 性能优化工具库
 * 
 * 提供组件优化、请求优化、渲染优化等工具
 * 
 * @module utils/performance
 */

/**
 * 防抖函数 - 优化版
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 等待毫秒数
 * @param {boolean} immediate - 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, wait, immediate = false) {
  let timeout
  
  const debounced = function(...args) {
    const context = this
    
    const later = function() {
      timeout = null
      if (!immediate) func.apply(context, args)
    }
    
    const callNow = immediate && !timeout
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
    
    if (callNow) func.apply(context, args)
  }
  
  debounced.cancel = function() {
    clearTimeout(timeout)
    timeout = null
  }
  
  return debounced
}

/**
 * 节流函数 - 优化版
 * @param {Function} func - 要执行的函数
 * @param {number} limit - 时间间隔
 * @returns {Function} 节流后的函数
 */
export function throttle(func, limit) {
  let inThrottle = false
  let lastFunc
  let lastRan
  
  return function(...args) {
    const context = this
    
    if (!inThrottle) {
      func.apply(context, args)
      lastRan = Date.now()
      inThrottle = true
    } else {
      clearTimeout(lastFunc)
      lastFunc = setTimeout(function() {
        if ((Date.now() - lastRan) >= limit) {
          func.apply(context, args)
          lastRan = Date.now()
        }
      }, limit - (Date.now() - lastRan))
    }
  }
}

/**
 * 批处理工具
 * 将频繁的调用收集到批量处理
 */
export class BatchProcessor {
  constructor(handler, delay = 100) {
    this.handler = handler
    this.delay = delay
    this.queue = []
    this.timer = null
  }
  
  add(item) {
    this.queue.push(item)
    this.schedule()
  }
  
  addAll(items) {
    this.queue.push(...items)
    this.schedule()
  }
  
  schedule() {
    if (this.timer) return
    
    this.timer = setTimeout(() => {
      this.flush()
    }, this.delay)
  }
  
  flush() {
    if (this.queue.length === 0) return
    
    const items = [...this.queue]
    this.queue = []
    this.timer = null
    
    this.handler(items)
  }
  
  clear() {
    this.queue = []
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
  }
}

/**
 * 虚拟滚动 - 简单实现
 * 用于处理大型列表渲染
 */
export class VirtualScroller {
  constructor(options) {
    this.itemHeight = options.itemHeight || 50
    this.bufferSize = options.bufferSize || 5
    this.containerHeight = options.containerHeight || 500
    this.totalItems = options.totalItems || 0
    
    this.scrollTop = 0
    this.startIndex = 0
  }
  
  updateScrollTop(scrollTop) {
    this.scrollTop = scrollTop
  }
  
  getVisibleRange() {
    const visibleCount = Math.ceil(this.containerHeight / this.itemHeight)
    const start = Math.max(0, Math.floor(this.scrollTop / this.itemHeight) - this.bufferSize)
    const end = Math.min(this.totalItems, start + visibleCount + this.bufferSize * 2)
    
    return { start, end }
  }
  
  getOffsetY() {
    return this.getVisibleRange().start * this.itemHeight
  }
}

/**
 * 懒加载观察器
 * @param {Function} onIntersect - 交叉回调
 * @param {Object} options - IntersectionObserver 选项
 * @returns {IntersectionObserver}
 */
export function createLazyLoadObserver(onIntersect, options = {}) {
  return new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        onIntersect(entry.target, entry)
      }
    })
  }, {
    rootMargin: '100px',
    threshold: 0.1,
    ...options
  })
}

/**
 * 图片懒加载
 */
export class LazyImageLoader {
  constructor() {
    this.observer = null
    this.init()
  }
  
  init() {
    this.observer = createLazyLoadObserver((img) => {
      if (img.dataset.src) {
        img.src = img.dataset.src
        if (img.dataset.srcset) {
          img.srcset = img.dataset.srcset
        }
        img.classList.add('loaded')
        this.observer.unobserve(img)
      }
    })
  }
  
  observe(img) {
    this.observer.observe(img)
  }
  
  observeAll(selector = 'img[data-src]') {
    document.querySelectorAll(selector).forEach(img => {
      this.observe(img)
    })
  }
  
  destroy() {
    this.observer.disconnect()
  }
}

/**
 * 请求并发控制器
 */
export class RequestController {
  constructor(maxConcurrent = 6) {
    this.maxConcurrent = maxConcurrent
    this.activeRequests = 0
    this.queue = []
  }
  
  async add(requestFn) {
    return new Promise((resolve, reject) => {
      this.queue.push({ fn: requestFn, resolve, reject })
      this.processQueue()
    })
  }
  
  async processQueue() {
    if (this.activeRequests >= this.maxConcurrent) return
    if (this.queue.length === 0) return
    
    const { fn, resolve, reject } = this.queue.shift()
    this.activeRequests++
    
    try {
      const result = await fn()
      resolve(result)
    } catch (error) {
      reject(error)
    } finally {
      this.activeRequests--
      this.processQueue()
    }
  }
  
  clear() {
    this.queue = []
  }
}

/**
 * 记忆化函数缓存
 */
export function memoize(fn, keyFn = JSON.stringify) {
  const cache = new Map()
  
  return function(...args) {
    const key = keyFn(args)
    if (cache.has(key)) {
      return cache.get(key)
    }
    const result = fn.apply(this, args)
    cache.set(key, result)
    return result
  }
}

/**
 * 性能监控 - 简单实现
 */
export class PerformanceMonitor {
  constructor() {
    this.marks = {}
    this.measures = []
  }
  
  mark(name) {
    this.marks[name] = performance.now()
  }
  
  measure(name, startMark, endMark) {
    const start = this.marks[startMark]
    const end = this.marks[endMark]
    
    if (start && end) {
      this.measures.push({
        name,
        duration: end - start
      })
    }
  }
  
  getReport() {
    return {
      measures: this.measures,
      summary: this.measures.reduce((acc, m) => {
        acc[m.name] = (acc[m.name] || 0) + m.duration
        return acc
      }, {})
    }
  }
  
  reset() {
    this.marks = {}
    this.measures = []
  }
}

// 全局性能监控实例
export const perfMonitor = new PerformanceMonitor()

/**
 * 预获取策略 - 预测用户行为提前加载
 */
export class PrefetchManager {
  constructor() {
    this.prefetched = new Set()
  }
  
  prefetch(url) {
    if (this.prefetched.has(url)) return
    
    this.prefetched.add(url)
    
    // 使用 link prefetch
    const link = document.createElement('link')
    link.rel = 'prefetch'
    link.href = url
    document.head.appendChild(link)
  }
  
  prefetchUrls(urls) {
    urls.forEach(url => this.prefetch(url))
  }
}

export const prefetchManager = new PrefetchManager()
