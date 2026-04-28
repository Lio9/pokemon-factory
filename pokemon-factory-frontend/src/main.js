/*
 * main 文件说明
 * 所属模块：前端应用。
 * 文件类型：前端启动入口文件。
 * 核心职责：负责创建应用实例并挂载全局依赖、样式和路由。
 * 阅读建议：建议从这里把握前端启动链路。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import VueVirtualScroller from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import './index.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.use(VueVirtualScroller)

// 无限滚动指令
app.directive('infinite-scroll', {
  mounted(el, binding) {
    const callback = binding.value
    let isLoading = false
    
    const onScroll = async () => {
      const { scrollTop, scrollHeight, clientHeight } = el
      if (scrollTop + clientHeight >= scrollHeight - 10 && !isLoading) {
        isLoading = true
        try {
          await callback()
        } finally {
          isLoading = false
        }
      }
    }
    
    el.addEventListener('scroll', onScroll)
    el._onScroll = onScroll
  },
  
  unmounted(el) {
    if (el._onScroll) {
      el.removeEventListener('scroll', el._onScroll)
    }
  }
})

app.mount('#app')
