import { computed, reactive, readonly } from 'vue'
import api from '../services/api'
import { normalizeAuthSession } from '../services/contracts/authContract'
import { getToken, getStoredUser, persistSession, sessionManager } from '../services/sessionStorage'
import { setSessionManager } from '../services/httpClient'
import { translate } from './useLocale'

// 将会话管理器注入 httpClient（401 自动清会话）
setSessionManager(sessionManager)

const initialToken = getToken()

const state = reactive({
  token: initialToken,
  user: getStoredUser(),
  restoring: false,
  initialized: !initialToken
})

function setSession(session) {
  state.token = session?.token || ''
  state.user = session?.user || null
  state.initialized = true
  persistSession(state.token, state.user)
}

function clearSession() {
  setSession(null)
}

let restorePromise = null

async function restoreSession() {
  if (state.restoring) return restorePromise
  if (!state.token) {
    state.initialized = true
    state.user = null
    return null
  }
  state.restoring = true
  restorePromise = (async () => {
    try {
      const response = await api.user.me()
      setSession(normalizeAuthSession({ token: state.token, ...response }))
      return state.user
    } catch {
      clearSession()
      return null
    } finally {
      state.restoring = false
      state.initialized = true
      restorePromise = null
    }
  })()
  return restorePromise
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

const isAuthenticated = computed(() => Boolean(state.token && state.user))
const displayName = computed(() => state.user?.displayName || state.user?.username || translate('游客', 'Guest'))

export function useAuth() {
  return {
    state: readonly(state),
    isAuthenticated,
    displayName,
    login,
    register,
    restoreSession,
    clearSession,
    logout: clearSession
  }
}
