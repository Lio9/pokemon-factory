/**
 * ============================================================
 * 路由配置 / Router Configuration
 * ============================================================
 *
 * ## 路由表 / Route Table
 *
 *   /                首页概览 / Home
 *   /pokemon         宝可梦图鉴 / Pokemon Dex (列表 / List)
 *   /pokemon/:id     宝可梦详情 / Pokemon Detail
 *   /moves           技能列表 / Moves
 *   /abilities       特性列表 / Abilities
 *   /items           物品列表 / Items
 *   /damage-calc     伤害计算 / Damage Calculator
 *   /battle          对战工厂 / Battle (需认证 / Auth Required)
 *   /login           登录 / Login
 *   /import          导入管理 / Import Manager
 *
 * ## 导航守卫 / Navigation Guards
 *
 *   beforeEach 负责：
 *   1. 同步更新页面标题 / Sync page title
 *   2. 恢复用户会话 / Restore user session
 *   3. 受保护路由的登录拦截 / Redirect to login for protected routes
 *   4. 已登录用户访问登录页时自动跳转 / Skip login for authenticated users
 *
 * ## 代码分割 / Code Splitting
 *
 * 所有页面组件使用动态 import()，路由级自动分包。
 * All views use dynamic import() for route-level code splitting.
 *
 * 本模块负责定义应用的所有路由规则和导航行为。
 * 使用 Vue Router 4 的动态导入实现路由级代码分割。
 *
 * 路由约定：
 * - 路径命名： kebab-case（如 /battle-factory）
 * - 组件命名： PascalCase（如 BattlePage）
 * - 路由懒加载：所有页面组件使用动态 import
 *
 * @module router
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { translate } from '../composables/useLocale'

/**
 * 应用路由配置表
 *
 * @constant {Array<RouteConfig>}
 */
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: { zh: '项目概览', en: 'Project Overview' }
    }
  },
  {
    path: '/pokemon',
    name: 'PokemonList',
    component: () => import('../views/PokemonList.vue'),
    meta: {
      title: { zh: '宝可梦图鉴', en: 'Pokemon Dex' }
    }
  },
  {
    path: '/pokemon/:id',
    name: 'PokemonDetail',
    component: () => import('../views/PokemonDetail.vue'),
    props: true,
    meta: {
      title: { zh: '宝可梦详情', en: 'Pokemon Details' }
    }
  },
  {
    path: '/moves',
    name: 'MoveList',
    component: () => import('../views/MoveList.vue'),
    meta: {
      title: { zh: '技能列表', en: 'Moves' }
    }
  },
  {
    path: '/abilities',
    name: 'AbilityList',
    component: () => import('../views/AbilityList.vue'),
    meta: {
      title: { zh: '特性列表', en: 'Abilities' }
    }
  },
  {
    path: '/items',
    name: 'ItemList',
    component: () => import('../views/ItemList.vue'),
    meta: {
      title: { zh: '物品列表', en: 'Items' }
    }
  },
  {
    path: '/damage-calculator',
    name: 'DamageCalculator',
    component: () => import('../views/DamageCalculator.vue'),
    meta: {
      title: { zh: '伤害计算器', en: 'Damage Calculator' }
    }
  },
  {
    path: '/import',
    name: 'ImportManager',
    component: () => import('../views/ImportManager.vue'),
    meta: {
      title: { zh: '导入管理', en: 'Import Manager' }
    }
  },
    {
      path: '/battle',
      name: 'Battle',
      component: () => import('../views/Battle.vue'),
      meta: {
        title: { zh: '对战工厂', en: 'Battle Factory' }
      }
    },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: {
      title: { zh: '登录', en: 'Login' }
    }
  },
  {
    path: '/auth/verify',
    name: 'EmailVerification',
    component: () => import('../views/EmailVerification.vue'),
    meta: {
      title: { zh: '邮箱验证', en: 'Email Verification' }
    }
  },
  {
    path: '/auth/callback',
    name: 'AuthCallback',
    component: () => import('../views/EmailVerification.vue'),
    meta: {
      title: { zh: '认证回调', en: 'Authentication Callback' }
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/pokemon'
  }
]

/**
 * 创建 Vue Router 实例
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

const auth = useAuth()

/**
 * 全局前置守卫
 *
 * 执行时机：每次路由导航之前
 *
 * 处理逻辑：
 * 1. 同步更新页面标题
 * 2. 未初始化时恢复用户会话
 * 3. 受保护路由的登录拦截
 * 4. 已登录用户访问登录页的自动跳转
 */
router.beforeEach(async (to) => {
  const pageTitle = typeof to.meta.title === 'object' && to.meta.title
    ? translate(to.meta.title.zh, to.meta.title.en)
    : 'Pokemon Factory'
  document.title = pageTitle ? `${pageTitle} - Pokemon Factory` : 'Pokemon Factory'

  if (!auth.state.initialized && auth.state.token) {
    await auth.restoreSession()
  }

  if (to.meta?.requiresAuth && !auth.isAuthenticated.value) {
    return {
      name: 'Login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (to.name === 'Login' && auth.isAuthenticated.value) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/battle'
  }
})

export default router
