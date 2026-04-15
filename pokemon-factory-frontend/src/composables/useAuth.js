import { computed, reactive, readonly } from 'vue'
import api from '../services/api'
import { normalizeAuthSession } from '../services/contracts/authContract'
import { getToken, getStoredUser, persistSession } from '../services/sessionStorage'

const initialToken = getToken()

const state = reactive({
  token: initialToken,
  user: getStoredUser(),
  restoring: false,
  initialized: !initialToken
})

// 用统一入口更新内存状态和本地缓存，避免 login/register/restoreSession 各自维护不同逻辑。
function setSession(session) {
  state.token = session?.token || ''
  state.user = session?.user || null
  state.initialized = true
  persistSession(state.token, state.user)
}

function clearSession() {
  setSession(null)
}

// 页面刷新后优先用 /me 校验 token 是否仍有效，防止本地残留的过期 token 误导页面展示。
// 用 restorePromise 记录正在进行中的恢复请求，让并发调用者共享同一个 Promise，避免重复发起请求。
let restorePromise = null

async function restoreSession() {
  if (state.restoring) {
    return restorePromise
  }

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
      // token 失效时直接清掉本地会话，避免页面继续展示过期用户信息。
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

// 登录成功后直接刷新统一会话状态，页面其他位置只读 useAuth 暴露的只读状态即可。
async function login(credentials) {
  const response = await api.user.login(credentials)
  const session = normalizeAuthSession(response)
  setSession(session)
  return session.user
}

// 注册成功后立即建立会话，和后端“注册即登录”的返回语义保持一致。
async function register(credentials) {
  const response = await api.user.register(credentials)
  const session = normalizeAuthSession(response)
  setSession(session)
  return session.user
}

const isAuthenticated = computed(() => Boolean(state.token && state.user))
const displayName = computed(() => state.user?.displayName || state.user?.username || '游客')

// 对外只暴露只读状态和受控操作，避免任意组件直接改内部 state。
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
