/**
 * 认证状态管理组合式函数
 *
 * 本模块提供认证相关的响应式状态和操作方法。
 * 用于 Vue 组件中管理用户登录、会话恢复等功能。
 *
 * 与 useAuthStore 的区别：
 * - useAuthStore: Pinia Store 版本，完整的响应式系统
 * - useAuth: 组合式函数版本，轻量级使用
 *
 * @module composables/useAuth
 */

import { computed, reactive, readonly } from 'vue'
import api from '../services/api'
import { normalizeAuthSession } from '../services/contracts/authContract'
import { getToken, getStoredUser, persistSession, sessionManager } from '../services/sessionStorage'
import { setSessionManager } from '../services/httpClient'
import { translate } from './useLocale'

// 将会话管理器注入 httpClient（401 自动清会话）
setSessionManager(sessionManager)

// 从 localStorage 恢复初始 token
const initialToken = getToken()

/**
 * 响应式状态对象
 */
const state = reactive({
  token: initialToken,
  user: getStoredUser(),
  restoring: false,
  initialized: !initialToken
})

/**
 * 设置会话信息
 */
function setSession(session) {
  state.token = session?.token || ''
  state.user = session?.user || null
  state.initialized = true
  persistSession(state.token, state.user)
}

/**
 * 清除会话
 */
function clearSession() {
  setSession(null)
}

/**
 * 恢复会话 Promise（防止重复请求）
 */
let restorePromise = null

/**
 * 恢复用户会话
 *
 * 向后端验证 token 有效性，
 * 有效则更新用户信息，无效则清除会话。
 *
 * @returns {Promise<Object|null>} 用户信息或 null
 */
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

/**
 * 用户登录
 *
 * @param {Object} credentials - 登录凭证
 * @returns {Promise<Object>} 用户信息
 */
async function login(credentials) {
  const response = await api.user.login(credentials)
  const session = normalizeAuthSession(response)
  setSession(session)
  return session.user
}

/**
 * 用户注册
 *
 * @param {Object} credentials - 注册信息
 * @returns {Promise<Object>} 用户信息
 */
async function register(credentials) {
  const response = await api.user.register(credentials)
  const session = normalizeAuthSession(response)
  setSession(session)
  return session.user
}

/**
 * 是否已认证（计算属性）
 */
const isAuthenticated = computed(() => Boolean(state.token && state.user))

/**
 * 显示名称（计算属性）
 */
const displayName = computed(() =>
  state.user?.displayName || state.user?.username || translate('游客', 'Guest')
)

/**
 * 认证状态管理组合式函数
 *
 * @returns {Object} 认证相关的状态和方法
 */
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
