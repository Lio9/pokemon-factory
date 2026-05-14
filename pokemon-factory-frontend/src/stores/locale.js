import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

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

export const useLocaleStore = defineStore('locale', () => {
  // State
  const locale = ref(resolveInitialLocale())

  // Getters
  const isEnglish = computed(() => locale.value === 'en-US')

  // Actions
  function setLocale(nextLocale) {
    if (!SUPPORTED_LOCALES.includes(nextLocale)) {
      return
    }
    locale.value = nextLocale
    localStorage.setItem(LOCALE_STORAGE_KEY, nextLocale)
  }

  function translate(zh, en, ja = '', params = {}) {
    const key = zh.toLowerCase().replace(/\s+/g, '-')
    let text = locale.value === 'ja-JP' ? (ja || en) : (locale.value === 'en-US' ? en : zh)

    // 如果字典中有定义，优先使用字典中的标准翻译
    if (DICTIONARY[key]) {
      text = DICTIONARY[key][locale.value] || text
    }

    const formatTemplate = (template, p = {}) => {
      return String(template).replace(/\{(\w+)\}/g, (_, k) => String(p[k] ?? ''))
    }

    return formatTemplate(text, params)
  }

  return {
    // State
    locale,
    // Getters
    isEnglish,
    // Actions
    setLocale,
    translate
  }
})
