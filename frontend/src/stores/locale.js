/**
 * 国际化状态管理模块
 *
 * 本模块管理应用的多语言切换功能。
 * 支持简体中文、英文、日语三种语言。
 *
 * @module stores/locale
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// localStorage 存储键名
const LOCALE_STORAGE_KEY = 'pokemon-factory-locale'

// 支持的语言列表
const SUPPORTED_LOCALES = ['zh-CN', 'en-US', 'ja-JP']

/**
 * 核心词汇映射表
 *
 * 用于统一游戏中特定术语的翻译，
 * 确保同一术语在不同语言下保持一致的表达。
 *
 * @constant {Object}
 */
const DICTIONARY = {
  'battle': { 'zh-CN': '对战', 'en-US': 'Battle', 'ja-JP': 'バトル' },
  'tera': { 'zh-CN': '太晶', 'en-US': 'Tera', 'ja-JP': 'テラスタル' },
  'max': { 'zh-CN': '极巨', 'en-US': 'Max', 'ja-JP': 'ダイマックス' },
  'z-move': { 'zh-CN': 'Z招式', 'en-US': 'Z-Move', 'ja-JP': 'Zワザ' }
}

/**
 * 解析初始语言设置
 *
 * 优先级：
 * 1. localStorage 中存储的用户偏好
 * 2. 浏览器语言设置
 *
 * @returns {string} 初始语言代码
 */
function resolveInitialLocale() {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (SUPPORTED_LOCALES.includes(stored)) {
    return stored
  }
  const lang = navigator.language?.toLowerCase()
  if (lang.startsWith('ja')) return 'ja-JP'
  return lang.startsWith('zh') ? 'zh-CN' : 'en-US'
}

/**
 * 语言状态 Store
 */
export const useLocaleStore = defineStore('locale', () => {
  // ========== 状态定义 ==========

  /** @type {Ref<string>} 当前语言设置 */
  const locale = ref(resolveInitialLocale())

  // ========== 计算属性 ==========

  /**
   * 是否为英文界面
   * @returns {boolean}
   */
  const isEnglish = computed(() => locale.value === 'en-US')

  // ========== 操作方法 ==========

  /**
   * 设置语言
   *
   * @param {string} nextLocale - 目标语言代码
   */
  function setLocale(nextLocale) {
    if (!SUPPORTED_LOCALES.includes(nextLocale)) {
      return
    }
    locale.value = nextLocale
    localStorage.setItem(LOCALE_STORAGE_KEY, nextLocale)
  }

  /**
   * 翻译函数
   *
   * @param {string} zh - 中文文本
   * @param {string} en - 英文文本
   * @param {string} [ja=''] - 日语文本
   * @param {Object} [params={}] - 模板参数
   * @returns {string} 当前语言的翻译文本
   */
  function translate(zh, en, ja = '', params = {}) {
    const key = zh.toLowerCase().replace(/\s+/g, '-')
    let text = locale.value === 'ja-JP' ? (ja || en) : (locale.value === 'en-US' ? en : zh)

    if (DICTIONARY[key]) {
      text = DICTIONARY[key][locale.value] || text
    }

    const formatTemplate = (template, p = {}) => {
      return String(template).replace(/\{(\w+)\}/g, (_, k) => String(p[k] ?? ''))
    }

    return formatTemplate(text, params)
  }

  return {
    // 状态
    locale,
    // 计算属性
    isEnglish,
    // 操作方法
    setLocale,
    translate
  }
})
