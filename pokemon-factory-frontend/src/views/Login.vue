<template>
  <div class="p-4 max-w-md mx-auto">
    <h1 class="text-2xl font-bold mb-4">登录</h1>
    <div class="mb-2">
      <input v-model="username" placeholder="用户名" class="w-full p-2 border rounded" />
    </div>
    <div class="mb-2">
      <input v-model="password" placeholder="密码" type="password" class="w-full p-2 border rounded" />
    </div>
    <div class="flex gap-2">
      <button @click="login" class="bg-blue-500 text-white px-4 py-2 rounded">登录</button>
      <button @click="register" class="bg-gray-500 text-white px-4 py-2 rounded">注册</button>
    </div>
    <p class="mt-4 text-sm text-red-600" v-if="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const error = ref(null)
const router = useRouter()

async function login() {
  error.value = null
  try {
    const res = await api.user.login({ username: username.value, password: password.value })
    if (res && res.token) {
      localStorage.setItem('jwt_token', res.token)
      router.push('/')
    }
  } catch (e) {
    error.value = e.message || '登录失败'
  }
}

async function register() {
  error.value = null
  try {
    await api.user.register({ username: username.value, password: password.value })
    await login()
  } catch (e) {
    error.value = e.message || '注册失败'
  }
}
</script>

<style scoped>
</style>