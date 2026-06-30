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
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  server: {
    host: '0.0.0.0',
    port: 7894,
    historyApiFallback: true,
    proxy: {
      // one-server (8081): 全部 API 由单个后端提供
      '/api/pokedex': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api': {
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
          'vue-vendor': ['vue', 'vue-router']
        }
      }
    },
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        // 只丢弃 console.log/debug，保留 warn/error 用于生产诊断
        drop_console: ['log', 'debug'],
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
    include: ['vue', 'vue-router', 'lucide-vue-next']
  }
})

