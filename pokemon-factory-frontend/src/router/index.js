/*
 * index 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端路由配置文件。
 * 核心职责：负责页面路由注册、导航入口和访问组织方式。
 * 阅读建议：建议结合页面文件一起理解路由层级。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { translate } from '../composables/useLocale'

// 路由懒加载 - 按需加载组件，减少首屏加载时间
const routes = [
  { 
    path: '/', 
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: { zh: '项目概览', en: 'Project Overview' } }
  },
  { 
    path: '/pokemon', 
    name: 'PokemonList', 
    component: () => import('../views/PokemonList.vue'),
    meta: { title: { zh: '宝可梦图鉴', en: 'Pokemon Dex' } }
  },
  { 
    path: '/pokemon/:id', 
    name: 'PokemonDetail', 
    component: () => import('../views/PokemonDetail.vue'), 
    props: true,
    meta: { title: { zh: '宝可梦详情', en: 'Pokemon Details' } }
  },
  { 
    path: '/moves', 
    name: 'MoveList', 
    component: () => import('../views/MoveList.vue'),
    meta: { title: { zh: '技能列表', en: 'Moves' } }
  },
  { 
    path: '/abilities', 
    name: 'AbilityList', 
    component: () => import('../views/AbilityList.vue'),
    meta: { title: { zh: '特性列表', en: 'Abilities' } }
  },
  { 
    path: '/items', 
    name: 'ItemList', 
    component: () => import('../views/ItemList.vue'),
    meta: { title: { zh: '物品列表', en: 'Items' } }
  },
  { 
    path: '/damage-calculator', 
    name: 'DamageCalculator', 
    component: () => import('../views/DamageCalculator.vue'),
    meta: { title: { zh: '伤害计算器', en: 'Damage Calculator' } }
  },
  {
    path: '/import',
    name: 'ImportManager',
    component: () => import('../views/ImportManager.vue'),
    meta: { title: { zh: '导入管理', en: 'Import Manager' } }
  },
  { 
    path: '/battle', 
    name: 'Battle', 
    component: () => import('../views/Battle.vue'),
    meta: { title: { zh: '对战工厂', en: 'Battle Factory' }, requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: { zh: '登录', en: 'Login' } }
  },
  {
    path: '/auth/verify',
    name: 'EmailVerification',
    component: () => import('../views/EmailVerification.vue'),
    meta: { title: { zh: '邮箱验证', en: 'Email Verification' } }
  },
  {
    path: '/auth/callback',
    name: 'AuthCallback',
    component: () => import('../views/EmailVerification.vue'),
    meta: { title: { zh: '认证回调', en: 'Authentication Callback' } }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/pokemon'
  }
]

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
 * 全局前置守卫：
 * 1) 同步页面标题；
 * 2) 首次导航时按需恢复会话；
 * 3) 拦截 requiresAuth 页面并重定向登录；
 * 4) 已登录访问登录页时回跳业务页。
 */
router.beforeEach(async (to) => {
  const pageTitle = typeof to.meta.title === 'object' && to.meta.title
    ? translate(to.meta.title.zh, to.meta.title.en)
    : 'Pokemon Factory'
  document.title = pageTitle ? `${pageTitle} - Pokemon Factory` : 'Pokemon Factory'

  // 只有本地存在 token 时才向后端恢复会话，避免普通页面反复请求 /me。
  if (!auth.state.initialized && auth.state.token) {
    await auth.restoreSession()
  }

  if (to.meta?.requiresAuth && !auth.isAuthenticated.value) {
    // 未登录访问受保护页面时，带上原始目标地址，登录成功后可原路跳回。
    return {
      name: 'Login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (to.name === 'Login' && auth.isAuthenticated.value) {
    // 已登录用户再次访问登录页时，直接送回业务页，避免停留在无意义的登录页面。
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/battle'
  }
})

export default router
