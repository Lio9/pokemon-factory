import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import './index.css'

// 配置全局axios
axios.defaults.baseURL = '/api'

const app = createApp(App)
app.config.globalProperties.$http = axios
app.use(ElementPlus)
app.use(router)

// 无限滚动指令
app.directive('infinite-scroll', {
  mounted(el, binding) {
    const callback = binding.value
    let isLoading = false
    
    const onScroll = async () => {
      const { scrollTop, scrollHeight, clientHeight } = el
      if (scrollTop + clientHeight >= scrollHeight - 10 && !isLoading) {
        isLoading = true
        await callback()
        isLoading = false
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