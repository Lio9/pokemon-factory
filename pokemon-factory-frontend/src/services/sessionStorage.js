const TOKEN_KEY = 'jwt_token'
const USER_KEY = 'auth_user'
const LEGACY_USERNAME_KEY = 'username'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function persistSession(token, user) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }

  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    if (user.username) {
      localStorage.setItem(LEGACY_USERNAME_KEY, user.username)
    }
  } else {
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(LEGACY_USERNAME_KEY)
  }
}

export function clearSessionStorage() {
  persistSession('', null)
}

export { TOKEN_KEY, USER_KEY, LEGACY_USERNAME_KEY }