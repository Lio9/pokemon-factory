<template>
  <div class="p-4 max-w-md mx-auto">
    <h1 class="text-2xl font-bold mb-4">
      登录
    </h1>
    <p class="mb-4 text-sm text-slate-500">
      登录后可进入对战工厂，并自动恢复你的会话状态。
    </p>
    <div class="mb-2">
      <input
        v-model="username"
        placeholder="用户名"
        class="w-full p-2 border rounded"
        autocomplete="username"
      >
    </div>
    <div class="mb-2">
      <input
        v-model="password"
        placeholder="密码"
        type="password"
        class="w-full p-2 border rounded"
        autocomplete="current-password"
      >
    </div>
    <div class="flex gap-2">
      <button
        class="bg-blue-500 text-white px-4 py-2 rounded"
        :disabled="loading"
        @click="login"
      >
        {{ loading ? '提交中...' : '登录' }}
      </button>
      <button
        class="bg-gray-500 text-white px-4 py-2 rounded"
        :disabled="loading"
        @click="register"
      >
        {{ loading ? '提交中...' : '注册并登录' }}
      </button>
    </div>
    <p
      v-if="error"
      class="mt-4 text-sm text-red-600"
    >
      {{ error }}
    </p>
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
