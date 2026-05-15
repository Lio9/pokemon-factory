/**
 * 性能监控工具
 * 提供页面加载时间、资源加载、用户交互等性能指标监控
 */

class PerformanceMonitor {
  constructor() {
    this.metrics = {}
    this.observers = []
  }
  
  /**
   * 记录页面加载时间
   */
  recordPageLoad(pageName) {
    if ('performance' in window) {
      const timing = performance.getEntriesByType('navigation')[0]
      
      if (timing) {
        this.metrics[pageName] = {
          dns: timing.domainLookupEnd - timing.domainLookupStart,
          tcp: timing.connectEnd - timing.connectStart,
          ttfb: timing.responseStart - timing.requestStart,
          domContentLoaded: timing.domContentLoadedEventEnd - timing.navigationStart,
          loadComplete: timing.loadEventEnd - timing.navigationStart,
          timestamp: Date.now()
        }
        
        console.log(`[Performance] ${pageName} loaded in ${this.metrics[pageName].loadComplete}ms`)
        
        // 发送到分析服务（如果配置了）
        this.sendToAnalytics(pageName, this.metrics[pageName])
      }
    }
  }
  
  /**
   * 记录组件渲染时间
   */
  recordComponentRender(componentName, startTime) {
    const renderTime = performance.now() - startTime
    
    if (!this.metrics.components) {
      this.metrics.components = {}
    }
    
    this.metrics.components[componentName] = {
      renderTime,
      timestamp: Date.now()
    }
    
    // 警告慢渲染
    if (renderTime > 100) {
      console.warn(`[Performance] Component ${componentName} took ${renderTime.toFixed(2)}ms to render`)
    }
  }
  
  /**
   * 记录API请求时间
   */
  recordApiCall(endpoint, duration, success = true) {
    if (!this.metrics.apiCalls) {
      this.metrics.apiCalls = []
    }
    
    this.metrics.apiCalls.push({
      endpoint,
      duration,
      success,
      timestamp: Date.now()
    })
    
    // 警告慢请求
    if (duration > 1000) {
      console.warn(`[Performance] API call to ${endpoint} took ${duration}ms`)
    }
  }
  
  /**
   * 监控长任务
   */
  observeLongTasks(threshold = 50) {
    if ('PerformanceObserver' in window) {
      try {
        const observer = new PerformanceObserver((list) => {
          list.getEntries().forEach((entry) => {
            console.warn(`[Performance] Long task detected: ${entry.duration.toFixed(2)}ms`)
            
            if (!this.metrics.longTasks) {
              this.metrics.longTasks = []
            }
            
            this.metrics.longTasks.push({
              duration: entry.duration,
              startTime: entry.startTime,
              timestamp: Date.now()
            })
          })
        })
        
        observer.observe({ entryTypes: ['longtask'] })
        this.observers.push(observer)
        
        console.log('[Performance] Long task monitoring enabled')
      } catch (error) {
        console.warn('[Performance] Long task monitoring not supported:', error)
      }
    }
  }
  
  /**
   * 监控资源加载
   */
  observeResourceLoading() {
    if ('PerformanceObserver' in window) {
      try {
        const observer = new PerformanceObserver((list) => {
          list.getEntries().forEach((entry) => {
            // 只关注慢资源
            if (entry.duration > 500) {
              console.warn(`[Performance] Slow resource: ${entry.name} (${entry.duration.toFixed(2)}ms)`)
            }
          })
        })
        
        observer.observe({ entryTypes: ['resource'] })
        this.observers.push(observer)
      } catch (error) {
        console.warn('[Performance] Resource monitoring not supported:', error)
      }
    }
  }
  
  /**
   * 获取性能报告
   */
  getReport() {
    return {
      pageLoads: this.metrics,
      memory: 'memory' in performance ? performance.memory : null,
      navigation: performance.getEntriesByType('navigation'),
      resources: performance.getEntriesByType('resource').filter(r => r.duration > 500),
      longTasks: this.metrics.longTasks || []
    }
  }
  
  /**
   * 清除监控数据
   */
  clear() {
    this.metrics = {}
    this.observers.forEach(obs => obs.disconnect())
    this.observers = []
  }
  
  /**
   * 发送到分析服务
   */
  sendToAnalytics(pageName, metrics) {
    // 这里可以集成Google Analytics、Sentry等
    // 示例：
    // if (window.gtag) {
    //   window.gtag('event', 'page_load', {
    //     page: pageName,
    //     load_time: metrics.loadComplete
    //   })
    // }
  }
}

// 导出单例
export const perfMonitor = new PerformanceMonitor()

/**
 * 图片懒加载优化
 */
export class ImageLazyLoader {
  constructor(options = {}) {
    this.options = {
      rootMargin: '50px 0px',
      threshold: 0.01,
      placeholder: '/placeholder.png',
      ...options
    }
    
    this.observer = null
    this.init()
  }
  
  init() {
    if ('IntersectionObserver' in window) {
      this.observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.loadImage(entry.target)
            this.observer.unobserve(entry.target)
          }
        })
      }, this.options)
      
      console.log('[Performance] Image lazy loading enabled')
    }
  }
  
  observe(imgElement) {
    if (this.observer && imgElement.dataset.src) {
      this.observer.observe(imgElement)
    }
  }
  
  loadImage(imgElement) {
    const src = imgElement.dataset.src
    if (!src) return
    
    const img = new Image()
    
    img.onload = () => {
      imgElement.src = src
      imgElement.classList.add('loaded')
    }
    
    img.onerror = () => {
      imgElement.src = this.options.placeholder
      imgElement.classList.add('error')
    }
    
    img.src = src
  }
  
  disconnect() {
    if (this.observer) {
      this.observer.disconnect()
    }
  }
}

/**
 * 虚拟滚动辅助
 */
export class VirtualScrollHelper {
  constructor(container, options = {}) {
    this.container = container
    this.itemHeight = options.itemHeight || 50
    this.buffer = options.buffer || 5
    this.visibleItems = []
    this.scrollTop = 0
    
    this.setupScrollListener()
  }
  
  setupScrollListener() {
    this.container.addEventListener('scroll', () => {
      this.scrollTop = this.container.scrollTop
      this.updateVisibleItems()
    })
  }
  
  updateVisibleItems() {
    const containerHeight = this.container.clientHeight
    const startIndex = Math.floor(this.scrollTop / this.itemHeight)
    const endIndex = Math.min(
      startIndex + Math.ceil(containerHeight / this.itemHeight) + this.buffer,
      this.totalItems || 0
    )
    
    this.visibleItems = {
      start: Math.max(0, startIndex - this.buffer),
      end: endIndex,
      offset: startIndex * this.itemHeight
    }
  }
  
  setTotalItems(count) {
    this.totalItems = count
    this.container.style.height = `${count * this.itemHeight}px`
  }
  
  getVisibleRange() {
    return this.visibleItems
  }
}

/**
 * 代码分割预加载
 */
export function preloadRoute(routePath) {
  // Vue Router会自动处理动态import的预加载
  // 这里可以添加额外的预加载逻辑
  
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      // 预加载关键资源
      const link = document.createElement('link')
      link.rel = 'prefetch'
      link.href = routePath
      document.head.appendChild(link)
    })
  }
}

/**
 * Web Worker性能优化
 */
export function createWorker(workerUrl) {
  if ('Worker' in window) {
    return new Worker(workerUrl)
  } else {
    console.warn('[Performance] Web Workers not supported')
    return null
  }
}
