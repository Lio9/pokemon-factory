/*
 * httpClient 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端基础服务文件。
 * 核心职责：负责请求、缓存、会话或资源地址等基础能力封装。
 * 阅读建议：建议关注对上层暴露的统一调用入口。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { appEnv } from '../config/env'
import { getToken } from './sessionStorage'

export const API_BASE = appEnv.apiBase
export const DAMAGE_API_BASE = appEnv.damageApiBase
export const SPRITES_BASE = appEnv.spritesBase
export const API_ROOT = API_BASE.replace(/\/api\/pokedex$/, '/api')

/**
 * 判断是否为项目统一响应壳结构（code/data/error）。
 * @param {any} payload 响应体
 * @returns {boolean}
 */
export function isStandardResponse(payload) {
  return Boolean(
    payload
    && typeof payload === 'object'
    && typeof payload.code === 'number'
    && ('data' in payload || 'error' in payload)
  )
}

/**
 * 解析 fetch 响应体，JSON 优先，非 JSON 退化为 message 文本。
 * @param {Response} response fetch 响应对象
 * @returns {Promise<any>}
 */
export async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }

  const text = await response.text()
  return text ? { message: text } : null
}

/**
 * 统一请求入口：自动注入 token、标准化错误并返回原始 payload。
 * @param {string} url 请求地址
 * @param {RequestInit} [options={}] fetch 选项
 * @returns {Promise<any>}
 */
export async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  const payload = await parseResponseBody(response)

  if (!response.ok) {
    const error = new Error(payload?.message || payload?.error || `HTTP error! status: ${response.status}`)
    error.status = response.status
    if (payload && typeof payload === 'object') {
      error.code = payload.code
      error.error = payload.error
      error.data = payload.data
    }
    throw error
  }

  return payload
}

/**
 * 请求并自动拆出 data 字段；非标准响应则原样返回。
 * @param {string} url 请求地址
 * @param {RequestInit} [options={}] fetch 选项
 * @returns {Promise<any>}
 */
export async function requestData(url, options = {}) {
  const payload = await request(url, options)
  return isStandardResponse(payload) ? payload.data : payload
}