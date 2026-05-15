/**
 * 国际化组合式函数
 *
 * 本模块提供多语言切换的响应式状态和翻译功能。
 * 支持简体中文、英文、日语三种语言。
 *
 * @module composables/useLocale
 */

import { computed, readonly, ref } from 'vue'

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

// 当前语言设置（响应式）
const locale = ref(resolveInitialLocale())

/**
 * 模板格式化函数
 *
 * @param {string} template - 模板字符串
 * @param {Object} params - 参数对象
 * @returns {string} 格式化后的字符串
 */
function formatTemplate(template, params = {}) {
  return String(template).replace(/\{(\w+)\}/g, (_, key) => String(params[key] ?? ''))
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
export function translate(zh, en, ja = '', params = {}) {
  const key = zh.toLowerCase().replace(/\s+/g, '-')
  let text = locale.value === 'ja-JP' ? (ja || en) : (locale.value === 'en-US' ? en : zh)

  if (DICTIONARY[key]) {
    text = DICTIONARY[key][locale.value] || text
  }

  return formatTemplate(text, params)
}

/**
 * 设置语言
 *
 * @param {string} nextLocale - 目标语言代码
 */
export function setLocale(nextLocale) {
  if (!SUPPORTED_LOCALES.includes(nextLocale)) {
    return
  }
  locale.value = nextLocale
  localStorage.setItem(LOCALE_STORAGE_KEY, nextLocale)
}

/**
 * 国际化组合式函数
 *
 * @returns {Object} 语言相关的状态和方法
 */
export function useLocale() {
  return {
    locale: readonly(locale),
    isEnglish: computed(() => locale.value === 'en-US'),
    setLocale,
    translate
  }
}
