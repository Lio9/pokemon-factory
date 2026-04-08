import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    historyApiFallback: true,
    proxy: {
      // battleFactory (8090): 对战、用户认证
      '/api/battle': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      '/api/user': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      // pokeDex (8081): 图鉴数据，后端路由不含 /api 前缀
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    // 代码分割策略
    rollupOptions: {
      output: {
        // 手动分包，避免单个chunk过大
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          'element-plus': ['element-plus'],
          'lucide': ['lucide-vue-next']
        }
      }
    },
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    // 生成source map用于调试(生产环境可关闭)
    sourcemap: false,
    // chunk大小警告阈值
    chunkSizeWarningLimit: 1000
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: ['vue', 'vue-router', 'element-plus', 'lucide-vue-next']
  }
})
