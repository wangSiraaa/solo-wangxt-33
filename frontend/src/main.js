import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import RecipeListView from './views/RecipeListView.vue'
import RecipeDetailView from './views/RecipeDetailView.vue'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: RecipeListView },
    { path: '/recipes/:id', component: RecipeDetailView, props: true }
  ]
})

createApp(App).use(router).mount('#app')
