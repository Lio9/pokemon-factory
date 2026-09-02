/**
 * 调试工具模块
 * Debug utility module
 * @description 提供分级日志记录、性能统计和调试信息管理
 * @module debug
 * @version 1.0.0
 * @author MiMo
 * 
 * @changelog
 * v1.0.0 - 初始版本
 * - 单例模式 DebugLogger 类
 * - 环形缓冲区日志存储
 * - 性能统计功能
 * - 分级日志系统
 */

/**
 * 日志级别类型
 * Log level type
 * @typedef {'info' | 'warn' | 'error' | 'debug'} LogLevel
 */
type LogLevel = 'info' | 'warn' | 'error' | 'debug'

/**
 * 日志分类类型
 * Log category type
 * @typedef {'scene' | 'battle' | 'interaction' | 'effects' | 'api' | 'performance' | 'audio'} LogCategory
 */
type LogCategory = 'scene' | 'battle' | 'interaction' | 'effects' | 'api' | 'performance' | 'audio'

/**
 * 日志条目接口
 * Log entry interface
 */
interface LogEntry {
  /** 唯一标识符 / Unique identifier */
  id: number
  /** 时间戳 / Timestamp */
  timestamp: Date
  /** 日志级别 / Log level */
  level: LogLevel
  /** 日志分类 / Log category */
  category: LogCategory
  /** 日志消息 / Log message */
  message: string
  /** 附加数据 / Additional data */
  data?: unknown
}

/**
 * 统计信息接口
 * Statistics interface
 */
interface Stats {
  /** 帧率 / Frames per second */
  fps: number
  /** 内存使用量（MB）/ Memory usage (MB) */
  memoryUsage: number
  /** 活跃特效数量 / Active effects count */
  activeEffects: number
  /** 活跃对象数量 / Active objects count */
  activeObjects: number
  /** 绘制调用次数 / Draw calls */
  drawCalls: number
  /** 三角形数量 / Triangles count */
  triangles: number
  /** 最后更新时间 / Last update time */
  lastUpdate: Date
}

/**
 * DebugLogger 类 - 调试日志管理器
 * DebugLogger class - Debug log manager
 * @description 单例模式的调试日志管理器，支持分级日志和性能统计
 * 
 * @example
 * // 获取单例实例 / Get singleton instance
 * const logger = DebugLogger.getInstance()
 * 
 * // 记录日志 / Log messages
 * logger.log('info', 'scene', 'Scene initialized successfully')
 * logger.log('error', 'api', 'Failed to fetch data', { status: 500 })
 * 
 * // 获取日志 / Get logs
 * const recentLogs = logger.getRecentLogs(10)
 * 
 * // 更新统计 / Update stats
 * logger.updateStats('fps', 60)
 * logger.updateStats('memoryUsage', 256)
 */
class DebugLogger {
  /** 单例实例 / Singleton instance */
  private static instance: DebugLogger | null = null

  /** 日志存储（环形缓冲区）/ Log storage (ring buffer) */
  private logs: LogEntry[] = []

  /** 最大日志数量 / Maximum log count */
  private readonly maxLogs: number = 500

  /** 当前日志 ID / Current log ID */
  private currentId: number = 0

  /** 统计信息 / Statistics */
  private stats: Stats = {
    fps: 0,
    memoryUsage: 0,
    activeEffects: 0,
    activeObjects: 0,
    drawCalls: 0,
    triangles: 0,
    lastUpdate: new Date()
  }

  /**
   * 私有构造函数 - 单例模式
   * Private constructor - Singleton pattern
   */
  private constructor() {
    // 初始化统计信息更新定时器 / Initialize stats update timer
    this.startStatsUpdater()
  }

  /**
   * 获取 DebugLogger 单例实例
   * Get DebugLogger singleton instance
   * @returns DebugLogger 实例 / DebugLogger instance
   * 
   * @example
   * const logger = DebugLogger.getInstance()
   */
  static getInstance(): DebugLogger {
    if (!DebugLogger.instance) {
      DebugLogger.instance = new DebugLogger()
    }
    return DebugLogger.instance
  }

  /**
   * 记录日志
   * Log a message
   * @param level - 日志级别 / Log level
   * @param category - 日志分类 / Log category
   * @param message - 日志消息 / Log message
   * @param data - 附加数据 / Additional data
   * 
   * @example
   * logger.log('info', 'scene', 'Object created', { id: 'pokemon-1' })
   * logger.log('error', 'api', 'Request failed', { error: 'timeout' })
   * logger.log('debug', 'interaction', 'Mouse clicked', { x: 100, y: 200 })
   */
  log(level: LogLevel, category: LogCategory, message: string, data?: unknown): void {
    const entry: LogEntry = {
      id: ++this.currentId,
      timestamp: new Date(),
      level,
      category,
      message,
      data
    }

    // 添加到环形缓冲区 / Add to ring buffer
    if (this.logs.length >= this.maxLogs) {
      this.logs.shift() // 移除最旧的日志 / Remove oldest log
    }
    this.logs.push(entry)

    // 根据级别输出到控制台 / Output to console based on level
    const consoleMessage = `[${category.toUpperCase()}] ${message}`
    const consoleData = data ? `\nData: ${JSON.stringify(data, null, 2)}` : ''

    switch (level) {
      case 'info':
        console.info(`%c${consoleMessage}`, 'color: #2196F3', consoleData)
        break
      case 'warn':
        console.warn(`%c${consoleMessage}`, 'color: #FF9800', consoleData)
        break
      case 'error':
        console.error(`%c${consoleMessage}`, 'color: #F44336', consoleData)
        break
      case 'debug':
        console.debug(`%c${consoleMessage}`, 'color: #9E9E9E', consoleData)
        break
    }
  }

  /**
   * 获取最近的日志
   * Get recent logs
   * @param count - 获取的日志数量（默认 50）/ Number of logs to get (default 50)
   * @returns 日志条目数组 / Array of log entries
   * 
   * @example
   * const recentLogs = logger.getRecentLogs(20)
   * console.log(`Found ${recentLogs.length} recent logs`)
   */
  getRecentLogs(count: number = 50): LogEntry[] {
    const startIndex = Math.max(0, this.logs.length - count)
    return this.logs.slice(startIndex).reverse()
  }

  /**
   * 获取指定级别的日志
   * Get logs by level
   * @param level - 日志级别 / Log level
   * @param count - 获取的数量（默认 50）/ Number to get (default 50)
   * @returns 日志条目数组 / Array of log entries
   * 
   * @example
   * const errorLogs = logger.getLogsByLevel('error', 10)
   */
  getLogsByLevel(level: LogLevel, count: number = 50): LogEntry[] {
    const filtered = this.logs.filter(log => log.level === level)
    const startIndex = Math.max(0, filtered.length - count)
    return filtered.slice(startIndex).reverse()
  }

  /**
   * 获取指定分类的日志
   * Get logs by category
   * @param category - 日志分类 / Log category
   * @param count - 获取的数量（默认 50）/ Number to get (default 50)
   * @returns 日志条目数组 / Array of log entries
   * 
   * @example
   * const sceneLogs = logger.getLogsByCategory('scene', 30)
   */
  getLogsByCategory(category: LogCategory, count: number = 50): LogEntry[] {
    const filtered = this.logs.filter(log => log.category === category)
    const startIndex = Math.max(0, filtered.length - count)
    return filtered.slice(startIndex).reverse()
  }

  /**
   * 清空日志
   * Clear all logs
   * 
   * @example
   * logger.clearLogs()
   * console.log('Logs cleared')
   */
  clearLogs(): void {
    this.logs = []
    this.currentId = 0
    console.info('%c[DEBUG] Logs cleared', 'color: #4CAF50')
  }

  /**
   * 获取统计信息
   * Get statistics
   * @returns 统计信息对象 / Statistics object
   * 
   * @example
   * const stats = logger.getStats()
   * console.log(`FPS: ${stats.fps}`)
   * console.log(`Memory: ${stats.memoryUsage} MB`)
   */
  getStats(): Stats {
    return { ...this.stats }
  }

  /**
   * 更新统计信息
   * Update statistics
   * @param key - 统计项键名 / Statistics key
   * @param value - 统计项值 / Statistics value
   * 
   * @example
   * logger.updateStats('fps', 60)
   * logger.updateStats('memoryUsage', 512)
   * logger.updateStats('activeEffects', 5)
   * logger.updateStats('activeObjects', 150)
   * logger.updateStats('drawCalls', 30)
   * logger.updateStats('triangles', 50000)
   */
  updateStats(key: keyof Stats, value: number | Date): void {
    if (key in this.stats) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (this.stats as any)[key] = value
      this.stats.lastUpdate = new Date()
    }
  }

  /**
   * 获取日志总数
   * Get total log count
   * @returns 日志数量 / Log count
   * 
   * @example
   * const count = logger.getLogCount()
   */
  getLogCount(): number {
    return this.logs.length
  }

  /**
   * 检查是否有错误日志
   * Check if there are error logs
   * @returns 是否有错误 / Whether there are errors
   * 
   * @example
   * if (logger.hasErrors()) {
   *   console.warn('There are errors in the log')
   * }
   */
  hasErrors(): boolean {
    return this.logs.some(log => log.level === 'error')
  }

  /**
   * 启动统计信息更新器
   * Start statistics updater
   * @description 每秒更新一次性能统计信息
   */
  private startStatsUpdater(): void {
    // 使用 requestAnimationFrame 计算 FPS
    // Use requestAnimationFrame to calculate FPS
    let frameCount = 0
    let lastTime = performance.now()

    const updateFPS = () => {
      frameCount++
      const currentTime = performance.now()

      if (currentTime - lastTime >= 1000) {
        this.stats.fps = Math.round(frameCount * 1000 / (currentTime - lastTime))
        frameCount = 0
        lastTime = currentTime

        // 尝试获取内存使用信息（如果可用）
        // Try to get memory usage info (if available)
        if (performance && 'memory' in performance) {
          const memory = (performance as { memory?: { usedJSHeapSize?: number } }).memory
          if (memory && memory.usedJSHeapSize) {
            this.stats.memoryUsage = Math.round(memory.usedJSHeapSize / 1024 / 1024)
          }
        }
      }

      requestAnimationFrame(updateFPS)
    }

    // 仅在浏览器环境中启动 / Only start in browser environment
    if (typeof window !== 'undefined') {
      requestAnimationFrame(updateFPS)
    }
  }

  /**
   * 导出日志为 JSON
   * Export logs as JSON
   * @returns JSON 字符串 / JSON string
   * 
   * @example
   * const jsonString = logger.exportLogs()
   * // 保存到文件或发送到服务器 / Save to file or send to server
   */
  exportLogs(): string {
    return JSON.stringify({
      exportTime: new Date().toISOString(),
      totalLogs: this.logs.length,
      stats: this.stats,
      logs: this.logs
    }, null, 2)
  }

  /**
   * 获取统计摘要
   * Get statistics summary
   * @returns 统计摘要字符串 / Statistics summary string
   * 
   * @example
   * const summary = logger.getStatsSummary()
   * console.log(summary)
   */
  getStatsSummary(): string {
    const stats = this.getStats()
    return `
=== Debug Statistics Summary ===
FPS: ${stats.fps}
Memory Usage: ${stats.memoryUsage} MB
Active Effects: ${stats.activeEffects}
Active Objects: ${stats.activeObjects}
Draw Calls: ${stats.drawCalls}
Triangles: ${stats.triangles}
Last Update: ${stats.lastUpdate.toLocaleString()}
Total Logs: ${this.logs.length}
Error Logs: ${this.logs.filter(l => l.level === 'error').length}
===============================
    `.trim()
  }
}

/**
 * 导出 DebugLogger 单例实例
 * Export DebugLogger singleton instance
 * 
 * @example
 * import { debugLogger } from './debug'
 * 
 * // 记录日志 / Log messages
 * debugLogger.log('info', 'scene', 'Scene loaded')
 * debugLogger.log('error', 'api', 'Request failed', { status: 404 })
 * 
 * // 获取统计 / Get stats
 * const stats = debugLogger.getStats()
 * console.log(`FPS: ${stats.fps}`)
 */
export const debugLogger = DebugLogger.getInstance()

/**
 * 快捷日志函数 - 信息级别
 * Quick log function - Info level
 * @param category - 日志分类 / Log category
 * @param message - 日志消息 / Log message
 * @param data - 附加数据 / Additional data
 * 
 * @example
 * import { logInfo } from './debug'
 * logInfo('scene', 'Object initialized')
 */
export const logInfo = (category: LogCategory, message: string, data?: unknown) => {
  debugLogger.log('info', category, message, data)
}

/**
 * 快捷日志函数 - 警告级别
 * Quick log function - Warning level
 * @param category - 日志分类 / Log category
 * @param message - 日志消息 / Log message
 * @param data - 附加数据 / Additional data
 * 
 * @example
 * import { logWarn } from './debug'
 * logWarn('battle', 'Low HP warning')
 */
export const logWarn = (category: LogCategory, message: string, data?: unknown) => {
  debugLogger.log('warn', category, message, data)
}

/**
 * 快捷日志函数 - 错误级别
 * Quick log function - Error level
 * @param category - 日志分类 / Log category
 * @param message - 日志消息 / Log message
 * @param data - 附加数据 / Additional data
 * 
 * @example
 * import { logError } from './debug'
 * logError('api', 'Failed to fetch data', { error: 'timeout' })
 */
export const logError = (category: LogCategory, message: string, data?: unknown) => {
  debugLogger.log('error', category, message, data)
}

/**
 * 快捷日志函数 - 调试级别
 * Quick log function - Debug level
 * @param category - 日志分类 / Log category
 * @param message - 日志消息 / Log message
 * @param data - 附加数据 / Additional data
 * 
 * @example
 * import { logDebug } from './debug'
 * logDebug('interaction', 'Mouse position', { x: 100, y: 200 })
 */
export const logDebug = (category: LogCategory, message: string, data?: unknown) => {
  debugLogger.log('debug', category, message, data)
}

/**
 * 类型导出
 * Type exports
 */
export type { LogLevel, LogCategory, LogEntry, Stats }