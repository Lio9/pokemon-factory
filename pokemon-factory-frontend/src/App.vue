<template>
  <div id="app" class="app-shell min-h-screen text-slate-800 font-sans">
    <el-container>
      <el-header class="sticky top-0 z-30 border-b border-white/60 bg-white/75 backdrop-blur-xl !p-0">
        <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-3 sm:h-16 sm:px-6 lg:px-8 w-full">
          <router-link to="/" class="flex items-center gap-2.5 sm:gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-900 text-sm font-bold text-white sm:h-10 sm:w-10">
              P
            </div>
            <div>
              <h1 class="text-base font-bold tracking-tight text-slate-800 sm:text-lg">Pokemon Factory</h1>
            </div>
          </router-link>

          <nav class="hidden md:flex items-center gap-1">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
              :class="route.path.startsWith(item.path) && item.path !== '/'
                ? 'bg-blue-50 text-blue-700'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'"
            >
              {{ item.name }}
            </router-link>
          </nav>

          <div class="flex items-center gap-2">
            <div class="flex items-center rounded-lg border border-slate-200 bg-white/80 p-0.5">
              <button
                v-for="option in localeOptions"
                :key="option.value"
                class="rounded-md px-2 py-1 text-xs font-medium transition"
                :class="locale === option.value ? 'bg-slate-800 text-white' : 'text-slate-500 hover:text-slate-800'"
                @click="setLocale(option.value)"
              >
                {{ option.shortLabel }}
              </button>
            </div>

            <router-link
              v-if="!isAuthenticated"
              to="/login"
              class="rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-slate-700"
            >
              {{ tr('登录', 'Login') }}
            </router-link>
            <div v-else class="flex items-center gap-2">
              <span class="text-sm text-slate-600">{{ authDisplayName }}</span>
              <button
                class="rounded-lg bg-slate-100 px-2.5 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-200"
                @click="handleLogout"
              >
                {{ tr('退出', 'Logout') }}
              </button>
            </div>

            <el-dropdown trigger="click" class="md:hidden">
              <button class="p-2 text-slate-600 hover:bg-slate-100 rounded-lg">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="item in navItems" :key="item.path">
                    <router-link :to="item.path" class="block w-full text-sm">{{ item.name }}</router-link>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-main class="mx-auto w-full max-w-7xl px-3 py-4 sm:px-6 sm:py-6 lg:px-8">
        <div class="min-h-[calc(100vh-120px)] rounded-2xl border border-white/60 bg-white/72 p-4 backdrop-blur-xl sm:p-6">
          <router-view />
        </div>
      </el-main>

      <el-footer class="px-4 py-4 text-center text-xs text-slate-400">
        &copy; 2024-{{ new Date().getFullYear() }} Pokemon Factory
      </el-footer>
    </el-container>

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
#app {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-shell {
  background: radial-gradient(circle at top left, rgba(14, 165, 233, 0.12), transparent 50%),
              radial-gradient(circle at top right, rgba(249, 115, 22, 0.08), transparent 50%),
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

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
</style>
