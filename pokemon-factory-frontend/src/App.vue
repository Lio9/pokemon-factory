<!--
  App 文件说明
  所属模块：前端应用。
  文件类型：前端单文件组件。
  核心职责：负责把模板、脚本与样式聚合在同一文件中完成一块可维护的界面能力。
  阅读建议：建议按 template、script、style 三部分顺序阅读。
  项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
-->

<template>
  <div
    id="app"
    class="app-shell min-h-screen text-slate-800 font-sans"
  >
    <el-container>
      <!-- Header -->
      <el-header class="sticky top-0 z-30 border-b border-white/60 bg-white/75 backdrop-blur-xl shadow-[0_16px_40px_-28px_rgba(15,23,42,0.35)] !p-0">
        <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-3 sm:h-16 sm:px-6 lg:px-8 w-full">
          <router-link
            to="/"
            class="group flex min-w-0 items-center gap-2.5 sm:gap-3"
          >
            <div class="flex h-9 w-9 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#0f172a,#0ea5e9,#f97316)] text-sm font-bold text-white shadow-lg shadow-sky-500/20 transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3 sm:h-10 sm:w-10">
              P
            </div>
            <div class="min-w-0">
              <h1 class="truncate text-base font-black tracking-tight text-transparent bg-[linear-gradient(135deg,#0f172a,#0f766e,#0284c7)] bg-clip-text sm:text-xl">
                Pokemon Factory
              </h1>
              <div class="hidden text-[10px] font-semibold uppercase tracking-[0.22em] text-slate-400 sm:block">
                {{ tr('图鉴数据 + 对战实验室', 'Data Dex + Battle Lab') }}
              </div>
            </div>
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

            <div class="flex items-center gap-1 rounded-xl border border-slate-200 bg-white/80 p-1">
              <button
                v-for="option in localeOptions"
                :key="option.value"
                class="rounded-lg px-2.5 py-1.5 text-xs font-semibold transition"
                :class="locale === option.value ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'"
                @click="setLocale(option.value)"
              >
                {{ option.label }}
              </button>
            </div>

            <div class="ml-2 flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
              <span class="text-sm font-semibold text-slate-700">{{ authDisplayName }}</span>
              <button
                v-if="isAuthenticated"
                class="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-700"
                @click="handleLogout"
              >
                {{ tr('退出登录', 'Sign out') }}
              </button>
              <router-link
                v-else
                to="/login"
                class="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-700"
              >
                {{ tr('去登录', 'Log in') }}
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
          <div class="md:hidden flex items-center gap-1.5">
            <div class="flex items-center gap-1 rounded-full bg-white/80 p-1 shadow-sm">
              <button
                v-for="option in localeOptions"
                :key="`mobile-${option.value}`"
                class="rounded-full px-2 py-1 text-[10px] font-semibold transition"
                :class="locale === option.value ? 'bg-slate-900 text-white' : 'text-slate-600'"
                @click="setLocale(option.value)"
              >
                {{ option.shortLabel }}
              </button>
            </div>
            <div class="max-w-[110px] truncate rounded-full bg-white/80 px-2.5 py-1 text-[11px] font-semibold text-slate-600 shadow-sm">
              {{ authDisplayName }}
            </div>
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
                      {{ tr('退出登录', 'Sign out') }}（{{ authDisplayName }}）
                    </button>
                  </el-dropdown-item>
                  <el-dropdown-item v-else>
                    <router-link
                      to="/login"
                      class="w-full"
                    >
                      {{ tr('登录', 'Login') }}
                    </router-link>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main class="mx-auto w-full max-w-7xl px-3 py-4 !pb-0 sm:px-6 sm:py-8 lg:px-8">
        <div class="content min-h-[calc(100vh-156px)] rounded-[24px] border border-white/60 bg-white/72 p-3 shadow-[0_30px_120px_-80px_rgba(15,23,42,0.65)] backdrop-blur-xl sm:min-h-[calc(100vh-180px)] sm:rounded-[32px] sm:p-6">
          <div
            v-if="showHero"
            class="hero card-glass mb-5 flex flex-col justify-between gap-4 rounded-[22px] border border-white/65 p-4 sm:mb-6 sm:flex-row sm:items-center sm:rounded-[28px] sm:p-6"
          >
            <div>
              <h2 class="text-[clamp(1.4rem,4vw,2rem)] font-black tracking-tight text-slate-950">
                {{ tr('探索图鉴，进入战斗实验室', 'Explore the dex and enter the battle lab') }}
              </h2>
              <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
                {{ tr('现在的首页不只是查询入口，也承担战斗工厂的导航壳层。筛选、查看、模拟和对战被放进同一套更统一的视觉语言里。', 'The homepage is no longer just a dex entry point. It also serves as the navigation shell for the battle factory, unifying search, inspection, simulation, and battling in one visual language.') }}
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
      <el-footer class="px-4 py-5 text-center text-xs text-gray-400 sm:py-6 sm:text-sm">
        <p>© 2024-{{ new Date().getFullYear() }} {{ tr('宝可梦图鉴', 'Pokemon Dex') }} - Pokemon Factory</p>
      </el-footer>
    </el-container>

    <!-- 全局组件 -->
    <ErrorHandler />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, Sun, Moon } from 'lucide-vue-next'
import ErrorHandler from './components/ErrorHandler.vue'
import { useAuth } from './composables/useAuth'
import { useLocale } from './composables/useLocale'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const { locale, setLocale, translate: tr } = useLocale()
const localeOptions = [
  { value: 'zh-CN', label: '中文', shortLabel: '中' },
  { value: 'en-US', label: 'English', shortLabel: 'EN' }
]
// 顶部导航集中定义，方便桌面端和移动端共用同一份菜单数据。
const navItems = computed(() => [
  { name: tr('宝可梦', 'Pokemon'), path: '/pokemon' },
  { name: tr('技能', 'Moves'), path: '/moves' },
  { name: tr('特性', 'Abilities'), path: '/abilities' },
  { name: tr('物品', 'Items'), path: '/items' },
  { name: tr('伤害计算', 'Damage Calc'), path: '/damage-calculator' },
  { name: tr('导入管理', 'Imports'), path: '/import' },
  { name: tr('对战工厂', 'Battle Factory'), path: '/battle' }
])
const theme = ref(localStorage.getItem('theme') || 'light')
const isAuthenticated = computed(() => auth.isAuthenticated.value)
const authDisplayName = computed(() => auth.displayName.value)
const showHero = computed(() => !['/battle', '/login'].includes(route.path))

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
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-shell {
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 18%),
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.12), transparent 20%),
    linear-gradient(180deg, #eef6ff 0%, #f8fafc 24%, #f4f7fb 100%);
}

html {
  font-size: clamp(14px, 0.82vw + 11px, 16px);
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

@media (max-width: 640px) {
  .content {
    box-shadow: 0 20px 70px -60px rgba(15, 23, 42, 0.45);
  }
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
