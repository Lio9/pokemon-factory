<template>
  <div id="app" class="min-h-screen bg-slate-50 text-slate-800 font-sans">
    <el-container>
      <!-- Header -->
      <el-header class="sticky top-0 z-30 bg-white/80 backdrop-blur-md border-b border-slate-200 !p-0">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 bg-gradient-to-tr from-red-500 to-orange-500 rounded-lg shadow-lg flex items-center justify-center text-white font-bold text-sm" @click="goToImportManager">
              P
            </div>
            <h1 class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
              宝可梦图鉴 <span class="text-purple-600 text-sm font-medium px-2 py-0.5 bg-purple-50 rounded-full border border-purple-100">宝可梦数据库</span>
            </h1>
          </div>
          
          <div class="flex items-center gap-4">
            <a href="https://github.com" target="_blank" rel="noreferrer" class="text-slate-400 hover:text-slate-800 transition-colors">
              <Github class="w-5 h-5" />
            </a>
          </div>
        </div>
      </el-header>
      
      <el-main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 !pb-0 w-full">
        <div class="nav-menu mb-8" v-if="!isImportManager">
          <el-menu 
            mode="horizontal" 
            :default-active="$route.path" 
            @select="handleMenuSelect"
            class="!border-0 !rounded-xl bg-white p-1 shadow-sm"
          >
            <el-menu-item index="/pokemon" class="!rounded-lg">宝可梦</el-menu-item>
            <el-menu-item index="/moves" class="!rounded-lg">招式</el-menu-item>
            <el-menu-item index="/abilities" class="!rounded-lg">特性</el-menu-item>
          </el-menu>
        </div>
        <div class="content bg-white rounded-2xl shadow-sm p-4 sm:p-6 min-h-[calc(100vh-180px)]">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import PokemonList from './components/PokemonList.vue'
import MoveList from './components/MoveList.vue'
import AbilityList from './components/AbilityList.vue'
import { Github } from 'lucide-vue-next'

export default {
  name: 'App',
  components: {
    PokemonList,
    MoveList,
    AbilityList,
    Github
  },
  data() {
    return {
      activeMenu: 'pokemon'
    }
  },
  computed: {
    isImportManager() {
      return this.$route.path === '/admin/import'
    }
  },
  methods: {
    handleMenuSelect(index) {
      this.$router.push(index)
    },
    goToImportManager() {
      // 隐藏的访问方式：点击Logo 3次进入导入管理页面
      if (!this.importClickCount) {
        this.importClickCount = 0
      }
      this.importClickCount++
      
      if (this.importClickCount >= 3) {
        this.$router.push('/admin/import')
        this.importClickCount = 0
      }
      
      // 3秒后重置计数
      setTimeout(() => {
        this.importClickCount = 0
      }, 3000)
    }
  }
}
</script>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
}

html, body {
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 自定义滚动条样式 */
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

/* 响应式改进 */
@media (min-width: 640px) {
  .content {
    padding: 1.5rem;
  }
}

@media (min-width: 1024px) {
  .content {
    padding: 1.5rem;
  }
}
</style>