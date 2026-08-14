<template>
  <div
    id="app"
    class="app-shell min-h-screen text-slate-800"
  >
    <!-- 导航栏 -->
    <header
      class="sticky top-0 z-30 border-b-2 border-poke-red/20 bg-white/90 shadow-sm backdrop-blur-xl"
    >
      <div class="mx-auto flex h-14 max-w-7xl items-center justify-between px-3 sm:h-16 sm:px-6 lg:px-8">
        <!-- Logo -->
        <router-link
          to="/"
          class="flex items-center gap-2.5 sm:gap-3 group"
        >
          <!-- Pokeball 图标 -->
          <div class="relative h-9 w-9 sm:h-10 sm:w-10">
            <svg
              viewBox="0 0 100 100"
              class="h-full w-full drop-shadow-sm transition-transform duration-300 group-hover:scale-110"
            >
              <circle
                cx="50"
                cy="50"
                r="46"
                fill="#DC2626"
                stroke="#1a1a1a"
                stroke-width="4"
              />
              <rect
                x="4"
                y="48"
                width="92"
                height="4"
                fill="#1a1a1a"
              />
              <circle
                cx="50"
                cy="50"
                r="46"
                fill="none"
                stroke="#1a1a1a"
                stroke-width="4"
              />
              <circle
                cx="50"
                cy="50"
                r="12"
                fill="#fff"
                stroke="#1a1a1a"
                stroke-width="4"
              />
              <circle
                cx="50"
                cy="50"
                r="6"
                fill="#fff"
                stroke="#1a1a1a"
                stroke-width="2"
              />
              <circle
                cx="50"
                cy="50"
                r="48"
                fill="none"
                stroke="#fff"
                stroke-width="1"
                opacity="0.3"
              />
              <path
                d="M4,50 A46,46 0 0,1 50,4"
                fill="none"
                stroke="rgba(255,255,255,0.2)"
                stroke-width="2"
              />
            </svg>
          </div>
          <div>
            <h1 class="text-base font-extrabold tracking-tight sm:text-lg">
              <span class="text-poke-red">Pokemon</span>
              <span class="text-slate-700"> Factory</span>
            </h1>
            <p class="text-[11px] font-medium text-slate-400 -mt-0.5 tracking-wider hidden sm:block">
              {{ tr('宝可梦图鉴与对战模拟', 'DEX & BATTLE SIM') }}
            </p>
          </div>
        </router-link>

        <!-- 桌面导航 -->
        <nav class="hidden md:flex items-center gap-1">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="relative px-3 py-2 rounded-xl text-sm font-semibold transition-all duration-200"
            :class="isActiveRoute(item.path)
              ? 'text-poke-red bg-red-50 shadow-[inset_0_-2px_0_0_rgba(220,38,38,0.8)]'
              : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100/80'"
          >
            <span class="relative z-10">{{ item.name }}</span>
          </router-link>
        </nav>

        <!-- 右侧操作区 -->
        <div class="flex items-center gap-2">
          <!-- 语言切换 -->
          <div class="flex items-center rounded-xl border-2 border-slate-200 bg-white p-0.5 shadow-poke">
            <button
              v-for="option in localeOptions"
              :key="option.value"
              class="rounded-lg px-2.5 py-1 text-xs font-bold transition-all duration-200"
              :class="locale === option.value
                ? 'bg-poke-red text-white shadow-sm'
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
            class="btn-poke !py-1.5 !px-3 !text-xs !rounded-xl"
          >
            {{ tr('登录', 'Login') }}
          </router-link>
          <div
            v-else
            class="flex items-center gap-2"
          >
            <span class="text-sm font-semibold text-slate-600">{{ authDisplayName }}</span>
            <button
              class="rounded-xl border-2 border-slate-200 bg-white px-2.5 py-1.5 text-xs font-bold text-slate-600 transition-all duration-200 hover:bg-slate-50 hover:border-slate-300 shadow-poke"
              @click="handleLogout"
            >
              {{ tr('退出', 'Logout') }}
            </button>
          </div>

          <!-- 移动端菜单 -->
          <el-dropdown
            trigger="click"
            class="md:hidden"
          >
            <button class="flex items-center justify-center rounded-xl border-2 border-slate-200 p-2 text-slate-600 transition-colors hover:bg-slate-50 shadow-poke">
              <svg
                class="h-5 w-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2.5"
                  d="M4 6h16M4 12h16M4 18h16"
                />
              </svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in navItems"
                  :key="item.path"
                >
                  <router-link
                    :to="item.path"
                    class="block w-full text-sm font-semibold"
                  >
                    {{ item.name }}
                  </router-link>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 导航栏底部渐变线 -->
      <div class="h-[2px] bg-gradient-to-r from-poke-red via-poke-yellow to-poke-red opacity-30" />
    </header>

    <!-- 主内容区 -->
    <main class="mx-auto w-full max-w-7xl px-3 py-4 sm:px-6 sm:py-6 lg:px-8">
      <router-view v-slot="{ Component }">
        <transition
          name="page"
          mode="out-in"
          appear
        >
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="px-4 py-6 text-center text-xs text-slate-400 border-t border-slate-100">
      <div class="mx-auto max-w-7xl flex flex-col items-center gap-2">
        <!-- Pokeball 装饰 -->
        <svg
          viewBox="0 0 100 100"
          class="w-6 h-6 opacity-30"
        >
          <circle
            cx="50"
            cy="50"
            r="46"
            fill="none"
            stroke="currentColor"
            stroke-width="3"
          />
          <line
            x1="4"
            y1="50"
            x2="96"
            y2="50"
            stroke="currentColor"
            stroke-width="3"
          />
          <circle
            cx="50"
            cy="50"
            r="10"
            fill="none"
            stroke="currentColor"
            stroke-width="3"
          />
        </svg>
        <p>
          &copy; 2024-{{ new Date().getFullYear() }} Pokemon Factory
          <span class="mx-1 text-slate-300">·</span>
          <span>{{ tr('宝可梦图鉴与对战模拟平台', 'Pokemon Dex & Battle Simulator') }}</span>
        </p>
      </div>
    </footer>

    <ErrorHandler />
  </div>
</template>

<script setup>
/**
 * ============================================================
 * 应用主入口组件 / App Shell Component
 * ============================================================
 *
 * Pokemon 正作风格导航栏 + 全局布局
 *
 * @component AppShell
 */
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
    radial-gradient(circle at 0% 0%, rgba(220, 38, 38, 0.04), transparent 40%),
    radial-gradient(circle at 100% 0%, rgba(245, 158, 11, 0.03), transparent 40%),
    radial-gradient(circle at 50% 100%, rgba(59, 130, 246, 0.03), transparent 40%),
    linear-gradient(180deg, #f8fafc 0%, #fef7ee 50%, #fff7ed 100%);
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
