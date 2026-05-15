/**
 * 键盘快捷键管理工具
 * 提供全局键盘快捷键注册和管理功能
 */

const shortcuts = new Map()
let isEnabled = true

/**
 * 注册键盘快捷键
 * @param {string} key - 快捷键组合，如 'Ctrl+S', 'Escape', '/'
 * @param {Function} handler - 处理函数
 * @param {Object} options - 配置选项
 */
export function registerShortcut(key, handler, options = {}) {
  const {
    preventDefault = true,
    stopPropagation = false,
    target = document // 监听的目标元素
  } = options

  const keys = key.toLowerCase().split('+').map(k => k.trim())
  
  const listener = (event) => {
    if (!isEnabled) return

    const pressedKeys = []
    if (event.ctrlKey || event.metaKey) pressedKeys.push('ctrl')
    if (event.shiftKey) pressedKeys.push('shift')
    if (event.altKey) pressedKeys.push('alt')
    pressedKeys.push(event.key.toLowerCase())

    const match = keys.every(k => pressedKeys.includes(k)) && pressedKeys.length === keys.length

    if (match) {
      if (preventDefault) event.preventDefault()
      if (stopPropagation) event.stopPropagation()
      handler(event)
    }
  }

  target.addEventListener('keydown', listener)
  
  // 存储以便后续移除
  shortcuts.set(key, { listener, target })
  
  return () => unregisterShortcut(key)
}

/**
 * 取消注册快捷键
 */
export function unregisterShortcut(key) {
  const shortcut = shortcuts.get(key)
  if (shortcut) {
    shortcut.target.removeEventListener('keydown', shortcut.listener)
    shortcuts.delete(key)
  }
}

/**
 * 批量注册快捷键
 */
export function registerShortcuts(shortcutMap) {
  const cleanupFunctions = []
  
  Object.entries(shortcutMap).forEach(([key, config]) => {
    if (typeof config === 'function') {
      cleanupFunctions.push(registerShortcut(key, config))
    } else {
      cleanupFunctions.push(registerShortcut(key, config.handler, config.options))
    }
  })
  
  return () => cleanupFunctions.forEach(cleanup => cleanup())
}

/**
 * 启用/禁用所有快捷键
 */
export function setShortcutsEnabled(enabled) {
  isEnabled = enabled
}

/**
 * 清除所有快捷键
 */
export function clearAllShortcuts() {
  shortcuts.forEach(({ listener, target }, key) => {
    target.removeEventListener('keydown', listener)
  })
  shortcuts.clear()
}

/**
 * 常用快捷键预设
 */
export const COMMON_SHORTCUTS = {
  SEARCH: '/',
  ESCAPE: 'Escape',
  SAVE: 'Ctrl+S',
  UNDO: 'Ctrl+Z',
  REDO: 'Ctrl+Y',
  REFRESH: 'F5',
  HOME: 'Alt+Home'
}
