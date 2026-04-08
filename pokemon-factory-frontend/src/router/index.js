import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'

// 路由懒加载 - 按需加载组件，减少首屏加载时间
const routes = [
  { 
    path: '/', 
    redirect: '/pokemon' 
  },
  { 
    path: '/pokemon', 
    name: 'PokemonList', 
    component: () => import('../views/PokemonList.vue'),
    meta: { title: '宝可梦图鉴' }
  },
  { 
    path: '/pokemon/:id', 
    name: 'PokemonDetail', 
    component: () => import('../views/PokemonDetail.vue'), 
    props: true,
    meta: { title: '宝可梦详情' }
  },
  { 
    path: '/moves', 
    name: 'MoveList', 
    component: () => import('../views/MoveList.vue'),
    meta: { title: '技能列表' }
  },
  { 
    path: '/abilities', 
    name: 'AbilityList', 
    component: () => import('../views/AbilityList.vue'),
    meta: { title: '特性列表' }
  },
  { 
    path: '/items', 
    name: 'ItemList', 
    component: () => import('../views/ItemList.vue'),
    meta: { title: '物品列表' }
  },
  { 
    path: '/damage-calculator', 
    name: 'DamageCalculator', 
    component: () => import('../views/DamageCalculator.vue'),
    meta: { title: '伤害计算器' }
  },
  {
    path: '/import',
    name: 'ImportManager',
    component: () => import('../views/ImportManager.vue'),
    meta: { title: '导入管理' }
  },
  { 
    path: '/battle', 
    name: 'Battle', 
    component: () => import('../views/Battle.vue'),
    meta: { title: '对战工厂', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
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

// 全局前置守卫统一处理页面标题、登录恢复和受保护页面跳转。
router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - Pokemon Factory` : 'Pokemon Factory'

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
