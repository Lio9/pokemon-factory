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
  </div>
</template>

<script>
import { Menu } from 'lucide-vue-next'

export default {
  name: 'App',
  components: { Menu },
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

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

::-webkit-scrollbar-thumb {
  background: #c5c5c5;
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}
</style>
