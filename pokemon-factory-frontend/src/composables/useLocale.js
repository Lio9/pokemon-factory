import { computed, readonly, ref } from 'vue'

const LOCALE_STORAGE_KEY = 'pokemon-factory-locale'
const SUPPORTED_LOCALES = ['zh-CN', 'en-US']

function resolveInitialLocale() {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (SUPPORTED_LOCALES.includes(stored)) {
    return stored
  }
  return navigator.language?.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en-US'
}

const locale = ref(resolveInitialLocale())

function formatTemplate(template, params = {}) {
  return String(template).replace(/\{(\w+)\}/g, (_, key) => String(params[key] ?? ''))
}

export function translate(zh, en, params = {}) {
  return formatTemplate(locale.value === 'en-US' ? en : zh, params)
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
