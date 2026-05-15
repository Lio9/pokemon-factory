/**
 * 会话存储管理模块
 *
 * 本模块负责管理用户认证会话数据的持久化存储。
 * 使用 localStorage 存储 JWT token 和用户信息。
 *
 * @module services/sessionStorage
 */

// localStorage 存储键名常量
const TOKEN_KEY = 'jwt_token'
const USER_KEY = 'auth_user'
const LEGACY_USERNAME_KEY = 'username'

/**
 * 获取存储的 JWT 认证令牌
 *
 * @returns {string} JWT token，若不存在则返回空字符串
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 获取存储的用户信息
 *
 * @returns {Object|null} 用户信息对象，解析失败或不存在时返回 null
 */
export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

/**
 * 持久化保存用户会话
 *
 * 同时保存 token 和用户信息到 localStorage。
 * 当 token 为空时清除存储，用户信息为 null 时也清除存储。
 *
 * @param {string} token - JWT 认证令牌
 * @param {Object|null} user - 用户信息对象
 */
export function persistSession(token, user) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    if (user.username) localStorage.setItem(LEGACY_USERNAME_KEY, user.username)
  } else {
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(LEGACY_USERNAME_KEY)
  }
}

/**
 * 清除所有会话存储
 *
 * 重置为未登录状态，移除 token 和用户信息。
 */
export function clearSessionStorage() {
  persistSession('', null)
}

/**
 * 会话管理器接口
 *
 * 提供给 httpClient 使用的轻量会话访问接口，
 * 避免与 Pinia store 形成循环依赖。
 *
 * @readonly
 * @property {Function} getToken - 获取当前 token
 * @property {Function} clearSession - 清除会话
 */
export const sessionManager = {
  getToken: () => getToken(),
  clearSession: () => clearSessionStorage()
}

// 导出存储键名常量供其他模块使用
export { TOKEN_KEY, USER_KEY, LEGACY_USERNAME_KEY }
