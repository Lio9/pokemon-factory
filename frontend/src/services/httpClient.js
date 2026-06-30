/**
 * ============================================================
 * HTTP 统一客户端 / HTTP Unified Client
 * ============================================================
 *
 * 前端与后端所有 API 通信的唯一入口。
 * Single entry point for ALL frontend-backend API communication.
 *
 * ## 响应格式约定 / Response Contract
 *
 *   { "code": 200, "data": {...}, "message": "success" }
 *   code=200 → 成功 / Success
 *   code>=400 → 业务错误 / Business error
 *   HTTP 401 → 自动清除会话 / Auto-clear session
 *
 * ## 增强特性 / Enhanced Features
 * - 自动重试（5xx/网络错误 ×2，指数退避）
 *   Auto retry (2 retries for 5xx/network, exponential backoff)
 * - 请求超时（默认 30s，AbortController 实现）
 *   Request timeout (30s default, via AbortController)
 * - 请求去重（相同 method+url+body 复用 Promise）
 *   Request dedup (same method+url+body share one Promise)
 *
 * 约定：
 * - 所有请求返回 `{ code, data, message, success }` 标准化结构
 * - `request()` 返回原始 payload
 * - `requestData()` 自动拆包拿到 data
 * - 401 自动清除本地会话，触发全局回调
 * - 非 2xx 统一包装为 Error 抛出
 * - 支持自动重试、超时取消、请求去重
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

// ---- 请求去重缓存 ----
const inflightRequests = new Map()

// ---- 默认配置 ----
const DEFAULTS = {
  timeout: 30000,       // 30s 超时
  retries: 2,           // 最多重试 2 次
  retryDelay: 1000,     // 重试间隔 1s
  deduplicate: false    // 是否去重（调用方按需开启）
}

function shouldRetry(status) {
  // 网络错误 (0) 或 服务端错误 (5xx) 才重试
  return status === 0 || (status >= 500 && status < 600)
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
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

/**
 * 核心请求函数，支持超时、重试、去重
 */
export async function request(url, options = {}) {
  const {
    timeout = DEFAULTS.timeout,
    retries = DEFAULTS.retries,
    retryDelay = DEFAULTS.retryDelay,
    deduplicate = DEFAULTS.deduplicate,
    ...fetchOptions
  } = options

  // 去重：相同 URL+body 的进行中请求只发一次（注意 body 已被解构到 fetchOptions）
  if (deduplicate) {
    const dedupKey = `${fetchOptions.method || 'GET'}:${url}:${JSON.stringify(fetchOptions.body || '')}`
    if (inflightRequests.has(dedupKey)) {
      return inflightRequests.get(dedupKey)
    }
    const promise = executeRequest(url, { timeout, retries, retryDelay, ...fetchOptions })
      .finally(() => inflightRequests.delete(dedupKey))
    inflightRequests.set(dedupKey, promise)
    return promise
  }

  return executeRequest(url, { timeout, retries, retryDelay, ...fetchOptions })
}

/**
 * 实际执行请求（含重试逻辑）
 */
async function executeRequest(url, options) {
  const { timeout, retries, retryDelay, ...fetchOptions } = options
  let lastError = null

  for (let attempt = 0; attempt <= retries; attempt++) {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), timeout)

    try {
      const token = getToken()
      const headers = {
        'Content-Type': 'application/json',
        ...(fetchOptions.headers || {})
      }
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const response = await fetch(url, {
        ...fetchOptions,
        headers,
        signal: fetchOptions.signal || controller.signal
      })

      clearTimeout(timeoutId)
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
    } catch (error) {
      clearTimeout(timeoutId)
      lastError = error

      // AbortError → 包装为超时错误
      if (error.name === 'AbortError' && !fetchOptions.signal?.aborted) {
        lastError = new Error(`请求超时 (${timeout}ms): ${url}`)
        lastError.status = 0
        lastError.code = 0
        lastError.isTimeout = true
      } else {
        lastError.isTimeout = false
      }

      // 不是需要重试的错误类型，直接抛出
      if (!shouldRetry(lastError.status ?? lastError.code ?? 0)) {
        throw lastError
      }

      // 最后一次尝试也失败了，抛出
      if (attempt >= retries) {
        lastError.isNetworkError = lastError.status === 0 && !lastError.isTimeout
        throw lastError
      }

      // 重试前等待
      await sleep(retryDelay * (attempt + 1))
    }
  }

  throw lastError || new Error('请求失败')
}

/**
 * 快捷获取 data 字段
 */
export async function requestData(url, options = {}) {
  const payload = await request(url, options)
  return payload.data
}
