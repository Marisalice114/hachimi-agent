import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import LoveApp from './views/LoveApp.vue'
import ManusApp from './views/ManusApp.vue'
import './style.css'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/love-app', name: 'LoveApp', component: LoveApp },
  { path: '/manus-app', name: 'ManusApp', component: ManusApp }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp(App).use(router).mount('#app')
