import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { installGlobalErrorHandling } from './utils/logger'

installGlobalErrorHandling()

createApp(App).use(router).mount('#app')
