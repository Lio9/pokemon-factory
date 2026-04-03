import { createRouter, createWebHistory } from 'vue-router'

// 路由懒加载 - 按需加载组件，减少首屏加载时间
const routes = [
  { 
    path: '/', 
    redirect: '/pokemon' 
  },
  { 
    path: '/pokemon', 
    name: 'PokemonList', 
    component: () => import('../components/PokemonList.vue'),
    meta: { title: '宝可梦图鉴' }
  },
  { 
    path: '/pokemon/:id', 
    name: 'PokemonDetail', 
    component: () => import('../components/PokemonDetail.vue'), 
    props: true,
    meta: { title: '宝可梦详情' }
  },
  { 
    path: '/moves', 
    name: 'MoveList', 
    component: () => import('../components/MoveList.vue'),
    meta: { title: '技能列表' }
  },
  { 
    path: '/abilities', 
    name: 'AbilityList', 
    component: () => import('../components/AbilityList.vue'),
    meta: { title: '特性列表' }
  },
  { 
    path: '/items', 
    name: 'ItemList', 
    component: () => import('../components/ItemList.vue'),
    meta: { title: '物品列表' }
  },
  { 
    path: '/damage-calculator', 
    name: 'DamageCalculator', 
    component: () => import('../components/DamageCalculator.vue'),
    meta: { title: '伤害计算器' }
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

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - Pokemon Factory` : 'Pokemon Factory'
  const requiresAuth = to.meta && to.meta.requiresAuth
  if (requiresAuth) {
    const token = localStorage.getItem('jwt_token')
    if (!token) return next({ name: 'Login' })
  }
  next()
})

export default router
