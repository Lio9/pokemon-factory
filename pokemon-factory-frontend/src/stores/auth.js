import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../services/api'
import { normalizeAuthSession } from '../services/contracts/authContract'
import { getToken, getStoredUser, persistSession, sessionManager } from '../services/sessionStorage'
import { setSessionManager } from '../services/httpClient'
import { translate } from '../composables/useLocale'

// 将会话管理器注入 httpClient（401 自动清会话）
setSessionManager(sessionManager)

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(getToken() || '')
  const user = ref(getStoredUser())
  const restoring = ref(false)
  const initialized = ref(!getToken())

  // Getters
  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const displayName = computed(() => 
    user.value?.displayName || user.value?.username || translate('游客', 'Guest')
  )

  // Actions
  function setSession(session) {
    token.value = session?.token || ''
    user.value = session?.user || null
    initialized.value = true
    persistSession(token.value, user.value)
  }

  function clearSession() {
    setSession(null)
  }

  async function restoreSession() {
    if (restoring.value) return Promise.resolve(user.value)
    if (!token.value) {
      initialized.value = true
      user.value = null
      return null
    }
    
    restoring.value = true
    try {
      const response = await api.user.me()
      setSession(normalizeAuthSession({ token: token.value, ...response }))
      return user.value
    } catch {
      clearSession()
      return null
    } finally {
      restoring.value = false
      initialized.value = true
    }
  }

  async function login(credentials) {
    const response = await api.user.login(credentials)
    const session = normalizeAuthSession(response)
    setSession(session)
    return session.user
  }

  async function register(credentials) {
    const response = await api.user.register(credentials)
    const session = normalizeAuthSession(response)
    setSession(session)
    return session.user
  }

  function logout() {
    clearSession()
  }

  return {
    // State
    token,
    user,
    restoring,
    initialized,
    // Getters
    isAuthenticated,
    displayName,
    // Actions
    setSession,
    clearSession,
    restoreSession,
    login,
    register,
    logout
  }
})
