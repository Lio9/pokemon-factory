/**
 * 统一 HTTP 客户端
 *
 * 约定：
 * - 所有请求返回 `{ code, data, message, success }` 标准化结构
 * - `request()` 返回原始 payload
 * - `requestData()` 自动拆包拿到 data
 * - 401 自动清除本地会话，触发全局回调
 * - 非 2xx 统一包装为 Error 抛出
 */
import { appEnv } from '../config/env'

export const API_BASE = appEnv.apiBase
export const BATTLE_API_BASE = appEnv.battleApiBase
export const DAMAGE_API_BASE = appEnv.damageApiBase
export const SPRITES_BASE = appEnv.spritesBase
export const API_ROOT = API_BASE.replace(/\/api\/pokedex$/, '/api')

// 会话存储引用（延迟引入，避免循环依赖）
let _sessionManager = null
let _onUnauthorized = null

export function setSessionManager(manager) {
  _sessionManager = manager
}

export function setOnUnauthorized(cb) {
  _onUnauthorized = cb
}

function getToken() {
  return _sessionManager?.getToken() ?? ''
}

export function isStandardResponse(payload) {
  return Boolean(
    payload &&
    typeof payload === 'object' &&
    typeof payload.code === 'number' &&
    ('data' in payload || 'message' in payload)
  )
}

async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }
  const text = await response.text()
  return text ? { code: response.status, data: null, message: text } : null
}

function buildError(payload, status) {
  const msg = payload?.message || payload?.error || `请求失败 (${status})`
  const err = new Error(msg)
  err.status = status
  err.code = payload?.code ?? status
  err.data = payload?.data ?? null
  return err
}

export async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(url, { ...options, headers })
  } catch (netErr) {
    const err = new Error(netErr.message || '网络连接失败，请检查网络')
    err.status = 0
    err.code = 0
    err.isNetworkError = true
    throw err
  }

  const payload = await parseResponseBody(response)
  if (!payload) {
    throw buildError({ message: `服务器无响应 (${response.status})` }, response.status)
  }

  // 401 → 自动清除会话
  if (response.status === 401) {
    _sessionManager?.clearSession()
    _onUnauthorized?.()
    throw buildError(payload, 401)
  }

  if (!response.ok) {
    throw buildError(payload, response.status)
  }

  // 业务错误码（>= 400）
  if (payload.code != null && payload.code >= 400) {
    throw buildError(payload, payload.code)
  }

  return {
    success: payload.code === 200,
    code: payload.code ?? 200,
    data: payload.data ?? null,
    message: payload.message ?? ''
  }
}

export async function requestData(url, options = {}) {
  const payload = await request(url, options)
  return payload.data
}
