<template>
  <div id="app" class="app-shell min-h-screen text-slate-800">
    <!-- 导航栏 -->
    <el-header
      class="sticky top-0 z-30 h-auto border-b border-white/70 bg-white/80 px-0 shadow-sm backdrop-blur-xl"
      style="padding: 0;"
    >
      <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-3 sm:h-16 sm:px-6 lg:px-8">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-2.5 sm:gap-3 group">
          <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-sm font-bold text-white shadow-md transition-transform duration-300 group-hover:scale-105 sm:h-10 sm:w-10">
            PF
          </div>
          <div>
            <h1 class="text-base font-bold tracking-tight text-slate-800 sm:text-lg">
              <span class="bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">Pokemon</span>
              <span class="text-slate-600"> Factory</span>
            </h1>
          </div>
        </router-link>

        <!-- 桌面导航 -->
        <nav class="hidden md:flex items-center gap-0.5">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="relative px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-200"
            :class="isActiveRoute(item.path)
              ? 'text-blue-700 bg-blue-50/80'
              : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100/60'"
          >
            {{ item.name }}
          </router-link>
        </nav>

        <!-- 右侧操作区 -->
        <div class="flex items-center gap-2">
          <!-- 语言切换 -->
          <div class="flex items-center rounded-lg border border-slate-200 bg-white/80 p-0.5 shadow-sm">
            <button
              v-for="option in localeOptions"
              :key="option.value"
              class="rounded-md px-2 py-1 text-xs font-medium transition-all duration-200"
              :class="locale === option.value
                ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-800'"
              @click="setLocale(option.value)"
            >
              {{ option.shortLabel }}
            </button>
          </div>

          <!-- 登录/用户 -->
          <router-link
            v-if="!isAuthenticated"
            to="/login"
            class="rounded-lg bg-gradient-to-r from-blue-600 to-indigo-600 px-3 py-1.5 text-xs font-medium text-white transition-all duration-200 hover:shadow-md hover:from-blue-700 hover:to-indigo-700"
          >
            {{ tr('登录', 'Login') }}
          </router-link>
          <div v-else class="flex items-center gap-2">
            <span class="text-sm text-slate-600">{{ authDisplayName }}</span>
            <button
              class="rounded-lg bg-slate-100 px-2.5 py-1.5 text-xs font-medium text-slate-600 transition-all duration-200 hover:bg-slate-200"
              @click="handleLogout"
            >
              {{ tr('退出', 'Logout') }}
            </button>
          </div>

          <!-- 移动端菜单 -->
          <el-dropdown trigger="click" class="md:hidden">
            <button class="flex items-center justify-center rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in navItems" :key="item.path">
                  <router-link :to="item.path" class="block w-full text-sm font-medium">{{ item.name }}</router-link>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 导航栏底部渐变线 -->
      <div class="h-[1px] bg-gradient-to-r from-transparent via-blue-200/60 to-transparent" />
    </el-header>

    <!-- 主内容区 -->
    <el-main class="mx-auto w-full max-w-7xl px-3 py-4 sm:px-6 sm:py-6 lg:px-8">
      <router-view v-slot="{ Component }">
        <transition
          name="page"
          mode="out-in"
          appear
        >
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <!-- 页脚 -->
    <el-footer class="px-4 py-4 text-center text-xs text-slate-400">
      <div class="mx-auto max-w-7xl">
        &copy; 2024-{{ new Date().getFullYear() }} Pokemon Factory
        <span class="mx-2 text-slate-300">·</span>
        <span>{{ tr('宝可梦图鉴与对战模拟平台', 'Pokemon Dex & Battle Simulator') }}</span>
      </div>
    </el-footer>

    <ErrorHandler />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorHandler from './components/ErrorHandler.vue'
import { useAuth } from './composables/useAuth'
import { useLocale } from './composables/useLocale'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const { locale, setLocale, translate: tr } = useLocale()

const localeOptions = [
  { value: 'zh-CN', shortLabel: '中' },
  { value: 'en-US', shortLabel: 'EN' }
]

const navItems = computed(() => [
  { name: tr('宝可梦', 'Pokemon'), path: '/pokemon' },
  { name: tr('技能', 'Moves'), path: '/moves' },
  { name: tr('特性', 'Abilities'), path: '/abilities' },
  { name: tr('物品', 'Items'), path: '/items' },
  { name: tr('伤害计算', 'Calc'), path: '/damage-calculator' },
  { name: tr('对战工厂', 'Battle'), path: '/battle' }
])

function isActiveRoute(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const isAuthenticated = computed(() => auth.isAuthenticated.value)
const authDisplayName = computed(() => auth.displayName.value)

onMounted(async () => {
  if (auth.state.token && !auth.state.initialized) {
    await auth.restoreSession()
  }
})

async function handleLogout() {
  auth.logout()
  if (route.meta?.requiresAuth) {
    await router.push('/login')
  }
}
</script>

<style>
.app-shell {
  background:
    radial-gradient(circle at 0% 0%, rgba(59, 130, 246, 0.08), transparent 50%),
    radial-gradient(circle at 100% 0%, rgba(139, 92, 246, 0.06), transparent 50%),
    radial-gradient(circle at 50% 100%, rgba(6, 182, 212, 0.04), transparent 50%),
    linear-gradient(180deg, #f8fafc 0%, #f0f5ff 25%, #f5f3ff 50%, #f0fdfa 100%);
  min-height: 100vh;
}

/* 页面切换动画 */
.page-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
