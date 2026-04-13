export function normalizeAuthSession(payload) {
  const session = payload && typeof payload === 'object' ? payload : {}
  return {
    token: session.token || '',
    user: session.user || null
  }
}