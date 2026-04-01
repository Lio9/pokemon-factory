<template>
  <div id="app" class="min-h-screen bg-slate-50 text-slate-800 font-sans">
    <el-container>
      <!-- Header -->
      <el-header class="sticky top-0 z-30 bg-white/90 backdrop-blur-xl border-b border-gray-100 shadow-sm !p-0">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <router-link to="/" class="flex items-center gap-3 group">
            <div class="w-10 h-10 bg-gradient-to-br from-red-500 via-orange-500 to-amber-500 rounded-xl shadow-lg flex items-center justify-center text-white font-bold text-sm transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
              P
            </div>
            <h1 class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-slate-800 to-slate-700">
              宝可梦图鉴
            </h1>
          </router-link>
          
          <!-- 导航菜单 -->
          <nav class="hidden md:flex items-center gap-2">
            <router-link 
              v-for="item in navItems" 
              :key="item.path"
              :to="item.path"
              class="px-5 py-2.5 rounded-xl text-sm font-bold transition-all duration-300 relative overflow-hidden"
              :class="$route.path === item.path || (item.path !== '/' && $route.path.startsWith(item.path)) 
                ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-lg shadow-blue-500/30' 
                : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
            >
              {{ item.name }}
            </router-link>
          </nav>

          <!-- 移动端菜单 -->
          <div class="md:hidden">
            <el-dropdown trigger="click">
              <button class="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
                <Menu class="w-6 h-6" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="item in navItems" :key="item.path">
                    <router-link :to="item.path" class="w-full">{{ item.name }}</router-link>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 !pb-0 w-full">
        <div class="content bg-white rounded-2xl shadow-sm p-4 sm:p-6 min-h-[calc(100vh-180px)]">
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

<script>
import { Menu } from 'lucide-vue-next'
import GlobalLoader from './components/GlobalLoader.vue'
import ErrorHandler from './components/ErrorHandler.vue'

export default {
  name: 'App',
  components: { Menu, GlobalLoader, ErrorHandler },
  data() {
    return {
      navItems: [
        { name: '宝可梦', path: '/pokemon' },
        { name: '技能', path: '/moves' },
        { name: '特性', path: '/abilities' },
        { name: '物品', path: '/items' },
        { name: '伤害计算', path: '/damage-calculator' }
      ]
    }
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
