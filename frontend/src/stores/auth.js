/**
 * 认证状态管理模块
 *
 * 本模块使用 Pinia 管理用户认证状态。
 * 提供登录、注册、登出、会话恢复等核心功能。
 *
 * 状态持久化：
 * - Token 和用户信息存储在 localStorage
 * - 页面刷新后自动恢复会话
 * - 401 响应自动清除本地会话
 *
 * @module stores/auth
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../services/api'
import { normalizeAuthSession } from '../services/contracts/authContract'
import { getToken, getStoredUser, persistSession, sessionManager } from '../services/sessionStorage'
import { setSessionManager } from '../services/httpClient'
import { translate } from '../composables/useLocale'

// 将会话管理器注入 httpClient（实现 401 自动清会话）
setSessionManager(sessionManager)

/**
 * 认证状态存储
 *
 * @typedef {Object} AuthState
 * @property {Ref<string>} token - JWT 认证令牌
 * @property {Ref<Object|null>} user - 用户信息对象
 * @property {Ref<boolean>} restoring - 会话恢复中标志
 * @property {Ref<boolean>} initialized - 初始化完成标志
 */

/**
 * 认证状态管理 Store
 *
 * @returns {AuthState}
 */
export const useAuthStore = defineStore('auth', () => {
  // ========== 状态定义 ==========

  /** @type {Ref<string>} JWT 认证令牌 */
  const token = ref(getToken() || '')

  /** @type {Ref<Object|null>} 用户信息对象 */
  const user = ref(getStoredUser())

  /** @type {Ref<boolean>} 会话恢复中标志，防止重复恢复请求 */
  const restoring = ref(false)

  /** @type {Ref<boolean>} 初始化完成标志 */
  const initialized = ref(!getToken())

  // ========== 计算属性 ==========

  /**
   * 是否已认证
   * @returns {boolean} 同时拥有 token 和 user 时返回 true
   */
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  /**
   * 显示名称
   * 优先级：displayName > username > 游客/Guest
   * @returns {string} 用户的显示名称
   */
  const displayName = computed(() =>
    user.value?.displayName || user.value?.username || translate('游客', 'Guest')
  )

  // ========== 操作方法 ==========

  /**
   * 设置会话信息
   *
   * @param {Object|null} session - 会话对象，包含 token 和 user
   */
  function setSession(session) {
    token.value = session?.token || ''
    user.value = session?.user || null
    initialized.value = true
    persistSession(token.value, user.value)
  }

  /**
   * 清除会话
   * 重置为未登录状态
   */
  function clearSession() {
    setSession(null)
  }

  /**
   * 恢复用户会话
   *
   * 向后端验证 token 有效性，
   * 有效则更新用户信息，无效则清除会话。
   *
   * @returns {Promise<Object|null>} 用户信息或 null
   */
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

  /**
   * 用户登录
   *
   * @param {Object} credentials - 登录凭证 { username, password }
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
   * @param {Object} credentials - 注册信息 { username, email, password }
   * @returns {Promise<Object>} 用户信息
   */
  async function register(credentials) {
    const response = await api.user.register(credentials)
    const session = normalizeAuthSession(response)
    setSession(session)
    return session.user
  }

  /**
   * 用户登出
   */
  function logout() {
    clearSession()
  }

  // ========== 导出 ==========

  return {
    // 状态
    token,
    user,
    restoring,
    initialized,
    // 计算属性
    isAuthenticated,
    displayName,
    // 操作方法
    setSession,
    clearSession,
    restoreSession,
    login,
    register,
    logout
  }
})
