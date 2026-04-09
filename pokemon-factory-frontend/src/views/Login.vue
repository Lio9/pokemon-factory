<template>
  <div class="mx-auto max-w-5xl px-1 py-4 sm:px-4 sm:py-8">
    <div class="grid gap-4 lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,420px)] lg:gap-6">
      <section class="overflow-hidden rounded-[28px] border border-slate-200/80 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(99,102,241,0.12),rgba(255,255,255,0.95))] p-5 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] sm:p-7">
        <div class="inline-flex rounded-full border border-white/70 bg-white/75 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-sky-700 shadow-sm">
          Trainer Access
        </div>
        <h1 class="mt-4 text-[clamp(1.8rem,5vw,3rem)] font-black tracking-tight text-slate-950">
          登录战斗工厂
        </h1>
        <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          登录后可恢复会话、进入工厂挑战、查看排行榜和保留你的挑战进度。移动端和桌面端都会复用同一套会话状态。
        </p>
        <div class="mt-6 grid gap-3 sm:grid-cols-3">
          <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-sm backdrop-blur">
            <div class="text-xs uppercase tracking-[0.16em] text-slate-400">工厂挑战</div>
            <div class="mt-2 text-sm font-semibold text-slate-900">9 连战流程</div>
          </div>
          <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-sm backdrop-blur">
            <div class="text-xs uppercase tracking-[0.16em] text-slate-400">会话恢复</div>
            <div class="mt-2 text-sm font-semibold text-slate-900">刷新后自动恢复</div>
          </div>
          <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-sm backdrop-blur">
            <div class="text-xs uppercase tracking-[0.16em] text-slate-400">对战模式</div>
            <div class="mt-2 text-sm font-semibold text-slate-900">手动与异步模拟</div>
          </div>
        </div>
      </section>

      <section class="rounded-[28px] border border-slate-200/80 bg-white/95 p-5 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:p-6">
        <h2 class="text-xl font-black tracking-tight text-slate-950">
          账号登录
        </h2>
        <p class="mt-2 text-sm leading-6 text-slate-500">
          没有账号也可以直接注册，注册成功后会自动登录并跳转到目标页面。
        </p>
        <div class="mt-5 space-y-3">
          <div>
            <label class="mb-2 block text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">用户名</label>
            <input
              v-model="username"
              placeholder="输入用户名"
              class="w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-sky-400 focus:bg-white"
              autocomplete="username"
            >
          </div>
          <div>
            <label class="mb-2 block text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">密码</label>
            <input
              v-model="password"
              placeholder="输入密码"
              type="password"
              class="w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-sky-400 focus:bg-white"
              autocomplete="current-password"
            >
          </div>
        </div>
        <div class="mt-5 grid gap-3 sm:grid-cols-2">
          <button
            class="rounded-2xl bg-sky-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            :disabled="loading"
            @click="login"
          >
            {{ loading ? '提交中...' : '登录' }}
          </button>
          <button
            class="rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-slate-100"
            :disabled="loading"
            @click="register"
          >
            {{ loading ? '提交中...' : '注册并登录' }}
          </button>
        </div>
        <p
          v-if="error"
          class="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600"
        >
          {{ error }}
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

// 登录页只维护表单态和跳转逻辑，真正的会话建立统一交给 useAuth。
const username = ref('')
const password = ref('')
const error = ref(null)
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const auth = useAuth()
const redirectTarget = computed(() => typeof route.query.redirect === 'string' ? route.query.redirect : '/battle')

onMounted(async () => {
  // 如果本地已经带 token，先尝试恢复会话；恢复成功则不用再显示登录页。
  if (auth.state.token && !auth.state.initialized) {
    await auth.restoreSession()
  }

  if (auth.isAuthenticated.value) {
    router.replace(redirectTarget.value)
  }
})

// 统一构造登录/注册使用的入参，并在前端先做最基本的非空校验。
function buildCredentials() {
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码'
    return null
  }

  return {
    username: username.value.trim(),
    password: password.value
  }
}

async function login() {
  error.value = null
  const credentials = buildCredentials()
  if (!credentials) {
    return
  }

  loading.value = true
  try {
    await auth.login(credentials)
    router.replace(redirectTarget.value)
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function register() {
  error.value = null
  const credentials = buildCredentials()
  if (!credentials) {
    return
  }

  loading.value = true
  try {
    // 注册成功后后端会直接返回登录态，因此这里的跳转逻辑和 login 完全一致。
    await auth.register(credentials)
    router.replace(redirectTarget.value)
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
</style>
