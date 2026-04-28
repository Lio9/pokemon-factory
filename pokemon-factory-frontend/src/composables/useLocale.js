/*
 * useLocale 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端组合式逻辑文件。
 * 核心职责：负责抽离可复用状态、派生数据和副作用处理流程。
 * 阅读建议：建议结合调用它的页面或组件一起理解数据流。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { computed, readonly, ref } from 'vue'

const LOCALE_STORAGE_KEY = 'pokemon-factory-locale'
const SUPPORTED_LOCALES = ['zh-CN', 'en-US', 'ja-JP']

// 简单的核心词汇映射表示例
const DICTIONARY = {
  'battle': { 'zh-CN': '对战', 'en-US': 'Battle', 'ja-JP': 'バトル' },
  'tera': { 'zh-CN': '太晶', 'en-US': 'Tera', 'ja-JP': 'テラスタル' },
  'max': { 'zh-CN': '极巨', 'en-US': 'Max', 'ja-JP': 'ダイマックス' },
  'z-move': { 'zh-CN': 'Z招式', 'en-US': 'Z-Move', 'ja-JP': 'Zワザ' }
}

function resolveInitialLocale() {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (SUPPORTED_LOCALES.includes(stored)) {
    return stored
  }
  const lang = navigator.language?.toLowerCase()
  if (lang.startsWith('ja')) return 'ja-JP'
  return lang.startsWith('zh') ? 'zh-CN' : 'en-US'
}

const locale = ref(resolveInitialLocale())

function formatTemplate(template, params = {}) {
  return String(template).replace(/\{(\w+)\}/g, (_, key) => String(params[key] ?? ''))
}

export function translate(zh, en, ja = '', params = {}) {
  const key = zh.toLowerCase().replace(/\s+/g, '-')
  let text = locale.value === 'ja-JP' ? (ja || en) : (locale.value === 'en-US' ? en : zh)

  // 如果字典中有定义，优先使用字典中的标准翻译
  if (DICTIONARY[key]) {
    text = DICTIONARY[key][locale.value] || text
  }

  return formatTemplate(text, params)
}

export function setLocale(nextLocale) {
  if (!SUPPORTED_LOCALES.includes(nextLocale)) {
    return
  }
  locale.value = nextLocale
  localStorage.setItem(LOCALE_STORAGE_KEY, nextLocale)
}

export function useLocale() {
  return {
    locale: readonly(locale),
    isEnglish: computed(() => locale.value === 'en-US'),
    setLocale,
    translate
  }
}
