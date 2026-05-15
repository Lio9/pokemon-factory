/*
 * vite.config 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端工程配置文件。
 * 核心职责：负责构建、校验或样式工具链的项目级配置。
 * 阅读建议：建议在修改工程能力前先理解这里的默认规则。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 7890,
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
      // pokeDex (8081): 图鉴数据
      '/api/pokedex': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/pokemon': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/moves': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/abilities': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/items': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/damage': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/types': {
        target: 'http://localhost:8081',
        changeOrigin: true
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
