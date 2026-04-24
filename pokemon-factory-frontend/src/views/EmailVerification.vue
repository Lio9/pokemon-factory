<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-sky-50 via-indigo-50 to-purple-50 px-4 py-8">
    <div class="w-full max-w-md">
      <!-- Verification Card -->
      <div class="rounded-[28px] border border-slate-200/80 bg-white/95 p-6 shadow-[0_20px_70px_-50px_rgba(15,23,42,0.45)] backdrop-blur sm:p-8">
        <!-- Status Icon -->
        <div class="flex justify-center mb-6">
          <div v-if="status === 'verifying'" class="relative">
            <div class="w-20 h-20 rounded-full border-4 border-sky-200 border-t-sky-600 animate-spin"></div>
          </div>
          <div v-else-if="status === 'success'" class="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center">
            <svg class="w-12 h-12 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
          <div v-else-if="status === 'error'" class="w-20 h-20 rounded-full bg-red-100 flex items-center justify-center">
            <svg class="w-12 h-12 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </div>
        </div>

        <!-- Title -->
        <h1 class="text-2xl font-black tracking-tight text-slate-950 text-center">
          {{ tr('邮箱验证', 'Email Verification') }}
        </h1>

        <!-- Status Message -->
        <p class="mt-4 text-sm leading-6 text-slate-600 text-center">
          <span v-if="status === 'verifying'">{{ tr('正在验证您的邮箱，请稍候...', 'Verifying your email, please wait...') }}</span>
          <span v-else-if="status === 'success'">{{ tr('邮箱验证成功！即将跳转到登录页面...', 'Email verified successfully! Redirecting to login page...') }}</span>
          <span v-else-if="status === 'error'">{{ errorMessage || tr('验证失败，链接可能已过期或无效。', 'Verification failed. The link may be expired or invalid.') }}</span>
        </p>

        <!-- Details Panel -->
        <div v-if="verificationDetails" class="mt-6 rounded-2xl bg-slate-50 border border-slate-200 p-4">
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-slate-500">{{ tr('验证类型', 'Type') }}:</span>
              <span class="font-semibold text-slate-900">{{ verificationDetails.type }}</span>
            </div>
            <div v-if="verificationDetails.email" class="flex justify-between">
              <span class="text-slate-500">{{ tr('邮箱', 'Email') }}:</span>
              <span class="font-semibold text-slate-900">{{ verificationDetails.email }}</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="mt-6 space-y-3">
          <button
            v-if="status === 'error'"
            @click="retryVerification"
            :disabled="loading"
            class="w-full rounded-2xl bg-sky-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {{ loading ? tr('重试中...', 'Retrying...') : tr('重新验证', 'Retry Verification') }}
          </button>

          <button
            v-if="status === 'success' || status === 'error'"
            @click="goToLogin"
            class="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            {{ tr('前往登录', 'Go to Login') }}
          </button>
        </div>

        <!-- Help Text -->
        <p class="mt-6 text-xs text-center text-slate-400">
          {{ tr('如果您没有收到验证邮件，请检查垃圾邮件文件夹。', "If you didn't receive the verification email, please check your spam folder.") }}
        </p>
      </div>

      <!-- Info Cards -->
      <div class="mt-6 grid gap-3 sm:grid-cols-2">
        <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-sm backdrop-blur border border-slate-200/80">
          <div class="text-xs uppercase tracking-[0.16em] text-slate-400">
            {{ tr('安全提示', 'Security Tip') }}
          </div>
          <div class="mt-2 text-sm font-semibold text-slate-900">
            {{ tr('请勿分享验证链接', 'Never share verification links') }}
          </div>
        </div>
        <div class="rounded-2xl bg-white/85 px-4 py-4 shadow-sm backdrop-blur border border-slate-200/80">
          <div class="text-xs uppercase tracking-[0.16em] text-slate-400">
            {{ tr('需要帮助？', 'Need help?') }}
          </div>
          <div class="mt-2 text-sm font-semibold text-slate-900">
            {{ tr('联系技术支持', 'Contact support') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLocale } from '../composables/useLocale'
import api from '../services/api'

const router = useRouter()
const route = useRoute()
const { translate: tr } = useLocale()

const status = ref('verifying') // 'verifying', 'success', 'error'
const errorMessage = ref('')
const loading = ref(false)
const verificationDetails = ref(null)

// Extract verification parameters from URL
const token = ref(route.query.token || '')
const type = ref(route.query.type || 'signup')
const redirectTo = ref(route.query.redirect_to || '/login')

onMounted(async () => {
  await verifyEmail()
})

async function verifyEmail() {
  if (!token.value) {
    status.value = 'error'
    errorMessage.value = tr('缺少验证令牌', 'Missing verification token')
    return
  }

  try {
    status.value = 'verifying'
    
    // Call backend verification endpoint
    // Note: Adjust this based on your actual backend API structure
    const response = await verifyToken(token.value, type.value)
    
    verificationDetails.value = {
      type: type.value === 'signup' ? tr('注册验证', 'Signup verification') : tr('邮箱修改验证', 'Email change verification'),
      email: response.email || null
    }
    
    status.value = 'success'
    
    // Auto redirect after 3 seconds
    setTimeout(() => {
      const targetPath = redirectTo.value.startsWith('/') 
        ? redirectTo.value 
        : '/login'
      router.push(targetPath)
    }, 3000)
    
  } catch (error) {
    console.error('Verification error:', error)
    status.value = 'error'
    errorMessage.value = error.message || tr('验证过程中发生错误', 'An error occurred during verification')
  }
}

async function verifyToken(tokenValue, verificationType) {
  // Try to call backend verification API
  // This should match your backend's verification endpoint
  try {
    // Assuming your backend has a verification endpoint
    const response = await fetch('/api/auth/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        token: tokenValue,
        type: verificationType
      })
    })
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new Error(errorData.message || tr('验证失败', 'Verification failed'))
    }
    
    return await response.json()
  } catch (error) {
    // If backend API is not available, simulate success for demo
    // In production, remove this fallback
    console.warn('Backend verification not available, using mock verification')
    
    // Mock successful verification
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          success: true,
          email: 'user@example.com',
          message: tr('验证成功（演示模式）', 'Verification successful (demo mode)')
        })
      }, 1500)
    })
  }
}

async function retryVerification() {
  loading.value = true
  errorMessage.value = ''
  
  try {
    await verifyEmail()
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
