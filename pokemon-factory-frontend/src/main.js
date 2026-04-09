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
