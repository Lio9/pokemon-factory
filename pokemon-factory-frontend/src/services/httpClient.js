import { appEnv } from '../config/env'
import { getToken } from './sessionStorage'

export const API_BASE = appEnv.apiBase
export const DAMAGE_API_BASE = appEnv.damageApiBase
export const SPRITES_BASE = appEnv.spritesBase
export const API_ROOT = API_BASE.replace(/\/api\/pokedex$/, '/api')

export function isStandardResponse(payload) {
  return Boolean(
    payload
    && typeof payload === 'object'
    && typeof payload.code === 'number'
    && ('data' in payload || 'error' in payload)
  )
}

export async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }

  const text = await response.text()
  return text ? { message: text } : null
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

export async function requestData(url, options = {}) {
  const payload = await request(url, options)
  return isStandardResponse(payload) ? payload.data : payload
}