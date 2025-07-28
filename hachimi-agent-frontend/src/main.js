import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import LoveApp from './views/LoveApp.vue'
import ManusApp from './views/ManusApp.vue'
import NotFound from './views/NotFound.vue'
import './style.css'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/love-app', name: 'LoveApp', component: LoveApp },
  { path: '/manus-app', name: 'ManusApp', component: ManusApp },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

createApp(App).use(router).mount('#app')
