export function unwrapApiData(payload, fallback = null) {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data ?? fallback
  }
  return payload ?? fallback
}

export function assertSuccessResponse(payload, fallbackMessage = '请求失败') {
  if (payload && typeof payload === 'object' && 'code' in payload && payload.code >= 400) {
    throw new Error(payload.message || fallbackMessage)
  }
  return payload
}