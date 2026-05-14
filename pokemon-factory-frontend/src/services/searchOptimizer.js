/**
 * 搜索优化工具
 * 提供防抖、高亮、模糊搜索等功能
 */

/**
 * 防抖函数
 * @param {Function} func - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, delay = 300) {
  let timer = null
  
  return function (...args) {
    const context = this
    
    if (timer) clearTimeout(timer)
    
    timer = setTimeout(() => {
      func.apply(context, args)
    }, delay)
  }
}

/**
 * 节流函数
 * @param {Function} func - 需要节流的函数
 * @param {number} interval - 间隔时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, interval = 300) {
  let lastTime = 0
  
  return function (...args) {
    const now = Date.now()
    const context = this
    
    if (now - lastTime >= interval) {
      func.apply(context, args)
      lastTime = now
    }
  }
}

/**
 * 高亮搜索关键词
 * @param {string} text - 原始文本
 * @param {string} keyword - 搜索关键词
 * @param {string} highlightClass - 高亮样式类名
 * @returns {string} 带高亮的HTML
 */
export function highlightText(text, keyword, highlightClass = 'bg-yellow-200 px-1 rounded') {
  if (!keyword || !text) return text
  
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKeyword})`, 'gi')
  
  return text.replace(regex, `<span class="${highlightClass}">$1</span>`)
}

/**
 * 模糊搜索匹配
 * @param {string} text - 待搜索文本
 * @param {string} keyword - 关键词
 * @returns {boolean} 是否匹配
 */
export function fuzzyMatch(text, keyword) {
  if (!keyword || !text) return true
  
  const lowerText = text.toLowerCase()
  const lowerKeyword = keyword.toLowerCase()
  
  // 精确匹配
  if (lowerText.includes(lowerKeyword)) return true
  
  // 模糊匹配：每个字符都按顺序出现
  let keywordIndex = 0
  for (let i = 0; i < lowerText.length && keywordIndex < lowerKeyword.length; i++) {
    if (lowerText[i] === lowerKeyword[keywordIndex]) {
      keywordIndex++
    }
  }
  
  return keywordIndex === lowerKeyword.length
}

/**
 * 计算搜索相关性分数
 * @param {string} text - 待搜索文本
 * @param {string} keyword - 关键词
 * @returns {number} 相关性分数（0-100）
 */
export function calculateRelevanceScore(text, keyword) {
  if (!keyword || !text) return 0
  
  const lowerText = text.toLowerCase()
  const lowerKeyword = keyword.toLowerCase()
  
  let score = 0
  
  // 完全匹配
  if (lowerText === lowerKeyword) return 100
  
  // 开头匹配
  if (lowerText.startsWith(lowerKeyword)) {
    score += 80
  }
  
  // 包含匹配
  if (lowerText.includes(lowerKeyword)) {
    score += 60
    
    // 匹配位置越靠前分数越高
    const position = lowerText.indexOf(lowerKeyword)
    score += Math.max(0, 20 - position)
  } else {
    // 模糊匹配
    let keywordIndex = 0
    let consecutiveMatches = 0
    let maxConsecutive = 0
    
    for (let i = 0; i < lowerText.length && keywordIndex < lowerKeyword.length; i++) {
      if (lowerText[i] === lowerKeyword[keywordIndex]) {
        keywordIndex++
        consecutiveMatches++
        maxConsecutive = Math.max(maxConsecutive, consecutiveMatches)
      } else {
        consecutiveMatches = 0
      }
    }
    
    if (keywordIndex === lowerKeyword.length) {
      score += 40 + maxConsecutive * 5
    }
  }
  
  // 文本长度惩罚（过长的文本相关性降低）
  if (text.length > 100) {
    score *= 0.9
  }
  
  return Math.min(100, score)
}

/**
 * 搜索历史记录管理
 */
export class SearchHistory {
  constructor(key = 'search-history', maxSize = 20) {
    this.key = key
    this.maxSize = maxSize
    this.history = this.load()
  }
  
  load() {
    try {
      const stored = localStorage.getItem(this.key)
      return stored ? JSON.parse(stored) : []
    } catch {
      return []
    }
  }
  
  save() {
    localStorage.setItem(this.key, JSON.stringify(this.history))
  }
  
  /**
   * 添加搜索记录
   */
  add(keyword) {
    if (!keyword || keyword.trim() === '') return
    
    const trimmed = keyword.trim()
    
    // 移除重复项
    this.history = this.history.filter(k => k !== trimmed)
    
    // 添加到开头
    this.history.unshift(trimmed)
    
    // 限制大小
    if (this.history.length > this.maxSize) {
      this.history = this.history.slice(0, this.maxSize)
    }
    
    this.save()
  }
  
  /**
   * 获取搜索历史
   */
  get() {
    return [...this.history]
  }
  
  /**
   * 清空历史
   */
  clear() {
    this.history = []
    localStorage.removeItem(this.key)
  }
  
  /**
   * 删除单条记录
   */
  remove(keyword) {
    this.history = this.history.filter(k => k !== keyword)
    this.save()
  }
}

/**
 * 创建优化的搜索hook
 */
export function useOptimizedSearch(options = {}) {
  const {
    onSearch,
    debounceDelay = 300,
    minKeywordLength = 1,
    searchHistory = new SearchHistory()
  } = options
  
  let currentKeyword = ''
  
  const debouncedSearch = debounce((keyword) => {
    if (keyword.length >= minKeywordLength) {
      searchHistory.add(keyword)
      onSearch?.(keyword)
    }
  }, debounceDelay)
  
  return {
    search(keyword) {
      currentKeyword = keyword
      debouncedSearch(keyword)
    },
    
    getHistory() {
      return searchHistory.get()
    },
    
    clearHistory() {
      searchHistory.clear()
    },
    
    getCurrentKeyword() {
      return currentKeyword
    }
  }
}
