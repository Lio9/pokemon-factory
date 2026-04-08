<template>
  <div
    id="app"
    class="min-h-screen bg-slate-50 text-slate-800 font-sans"
  >
    <el-container>
      <!-- Header -->
      <el-header class="sticky top-0 z-30 bg-white/90 backdrop-blur-xl border-b border-gray-100 shadow-sm !p-0">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <router-link
            to="/"
            class="flex items-center gap-3 group"
          >
            <div class="w-10 h-10 bg-gradient-to-br from-red-500 via-orange-500 to-amber-500 rounded-xl shadow-lg flex items-center justify-center text-white font-bold text-sm transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
              P
            </div>
            <h1 class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-slate-800 to-slate-700">
              宝可梦图鉴
            </h1>
          </router-link>
          
          <!-- 导航菜单 + 主题切换 -->
          <div class="hidden md:flex items-center gap-2">
            <nav class="flex items-center gap-2">
              <router-link 
                v-for="item in navItems" 
                :key="item.path"
                :to="item.path"
                class="px-5 py-2.5 rounded-xl text-sm font-bold transition-all duration-300 relative overflow-hidden"
                :class="route.path === item.path || (item.path !== '/' && route.path.startsWith(item.path)) 
                  ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-lg shadow-blue-500/30' 
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
              >
                {{ item.name }}
              </router-link>
            </nav>

            <div class="ml-2 flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
              <span class="text-sm font-semibold text-slate-700">{{ authDisplayName }}</span>
              <button
                v-if="isAuthenticated"
                class="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-700"
                @click="handleLogout"
              >
                退出登录
              </button>
              <router-link
                v-else
                to="/login"
                class="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-700"
              >
                去登录
              </router-link>
            </div>

            <!-- 主题切换按钮 (桌面) -->
            <button
              class="px-3 py-2 rounded-lg text-gray-600 bg-white/60 dark:bg-transparent border border-gray-100 dark:border-gray-700 hover:shadow-sm transition-colors"
              @click="toggleTheme"
            >
              <component
                :is="theme === 'dark' ? Moon : Sun"
                class="w-5 h-5"
              />
            </button>
          </div>

          <!-- 移动端菜单 & 主题切换 -->
          <div class="md:hidden flex items-center gap-2">
            <button
              class="p-2 rounded-lg text-gray-600 hover:bg-gray-100 transition-colors"
              @click="toggleTheme"
            >
              <component
                :is="theme === 'dark' ? Moon : Sun"
                class="w-6 h-6"
              />
            </button>
            <el-dropdown trigger="click">
              <button class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
                <Menu class="w-6 h-6" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in navItems"
                    :key="item.path"
                  >
                    <router-link
                      :to="item.path"
                      class="w-full"
                    >
                      {{ item.name }}
                    </router-link>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isAuthenticated">
                    <button
                      class="w-full text-left"
                      @click="handleLogout"
                    >
                      退出登录（{{ authDisplayName }}）
                    </button>
                  </el-dropdown-item>
                  <el-dropdown-item v-else>
                    <router-link
                      to="/login"
                      class="w-full"
                    >
                      登录
                    </router-link>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 !pb-0 w-full">
        <div class="content bg-white rounded-2xl shadow-sm p-4 sm:p-6 min-h-[calc(100vh-180px)]">
          <div class="hero card-glass mb-6 p-6 rounded-2xl flex items-center justify-between gap-4">
            <div>
              <h2 class="text-2xl sm:text-3xl font-extrabold text-slate-900">
                探索宝可梦世界
              </h2>
              <p class="mt-2 text-sm text-slate-500">
                快速浏览、收藏与比较。使用筛选提高搜索效率。
              </p>
            </div>
            <div class="hidden sm:flex items-center">
              <svg
                class="w-20 h-20 opacity-80"
                viewBox="0 0 64 64"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden
              >
                <circle
                  cx="32"
                  cy="32"
                  r="30"
                  fill="url(#g)"
                  stroke="#fff"
                  stroke-width="2"
                />
                <circle
                  cx="32"
                  cy="32"
                  r="14"
                  fill="#fff"
                />
                <defs>
                  <linearGradient
                    id="g"
                    x1="0"
                    x2="1"
                    y1="0"
                    y2="1"
                  >
                    <stop
                      offset="0%"
                      stop-color="#ff6b6b"
                    />
                    <stop
                      offset="100%"
                      stop-color="#f59e0b"
                    />
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>
          <router-view />
        </div>
      </el-main>
      
      <!-- Footer -->
      <el-footer class="text-center py-6 text-gray-400 text-sm">
        <p>© 2024 宝可梦图鉴 - Pokemon Factory</p>
      </el-footer>
    </el-container>

    <!-- 全局组件 -->
    <GlobalLoader />
    <ErrorHandler />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, Sun, Moon } from 'lucide-vue-next'
import GlobalLoader from './components/GlobalLoader.vue'
import ErrorHandler from './components/ErrorHandler.vue'
import { useAuth } from './composables/useAuth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
// 顶部导航集中定义，方便桌面端和移动端共用同一份菜单数据。
const navItems = [
  { name: '宝可梦', path: '/pokemon' },
  { name: '技能', path: '/moves' },
  { name: '特性', path: '/abilities' },
  { name: '物品', path: '/items' },
  { name: '伤害计算', path: '/damage-calculator' },
  { name: '导入管理', path: '/import' },
  { name: '对战工厂', path: '/battle' }
]
const theme = ref(localStorage.getItem('theme') || 'light')
const isAuthenticated = computed(() => auth.isAuthenticated.value)
const authDisplayName = computed(() => auth.displayName.value)

onMounted(async () => {
  document.documentElement.classList.toggle('dark', theme.value === 'dark')

  // 应用初始化时恢复一次会话，保证刷新后顶部导航和受保护页面状态一致。
  if (auth.state.token && !auth.state.initialized) {
    await auth.restoreSession()
  }
})

function toggleTheme() {
  // 主题状态同时写入 DOM class 和 localStorage，保证刷新后仍保持用户选择。
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  document.documentElement.classList.toggle('dark', theme.value === 'dark')
  localStorage.setItem('theme', theme.value)
}

async function handleLogout() {
  // 退出后如果当前页本身需要登录，立即跳回登录页，避免继续停留在受保护页面。
  auth.logout()
  if (route.meta?.requiresAuth) {
    await router.push('/login')
  }
}
</script>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

html, body {
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #2563eb, #7c3aed);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes slideInUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

@keyframes slideInDown {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(0);
  }
}

@keyframes slideInLeft {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  10%, 30%, 50%, 70%, 90% {
    transform: translateX(-10px);
  }
  20%, 40%, 60%, 80% {
    transform: translateX(10px);
  }
}

/* 工具类 */
.animate-fade-in {
  animation: fadeIn 0.3s ease-out;
}

.animate-fade-in-up {
  animation: fadeInUp 0.4s ease-out;
}

.animate-fade-in-down {
  animation: fadeInDown 0.4s ease-out;
}

.animate-fade-in-left {
  animation: fadeInLeft 0.4s ease-out;
}

.animate-fade-in-right {
  animation: fadeInRight 0.4s ease-out;
}

.animate-scale-in {
  animation: scaleIn 0.3s ease-out;
}

.animate-bounce {
  animation: bounce 1s infinite;
}

.animate-pulse {
  animation: pulse 2s infinite;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

.animate-shake {
  animation: shake 0.5s ease-in-out;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .el-main {
    padding-left: 12px !important;
    padding-right: 12px !important;
  }
  
  .content {
    padding: 12px !important;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  #app {
    background: #0f172a;
    color: #e2e8f0;
  }
  
  ::-webkit-scrollbar-track {
    background: #1e293b;
  }
}
</style>
