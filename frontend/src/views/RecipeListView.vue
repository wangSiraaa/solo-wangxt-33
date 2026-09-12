<template>
  <div>
    <h2>配方列表</h2>
    <p v-if="error" class="error">{{ error }}</p>
    <div v-for="r in recipes" :key="r.id" class="card">
      <h3>
        <router-link :to="`/recipes/${r.id}`">{{ r.name }}</router-link>
      </h3>
      <p class="muted">{{ r.description }}</p>
      <p>
        成品重量 {{ fmt(r.totalWeightG) }} g · 默认份量 {{ fmt(r.defaultServingSizeG) }} g
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const recipes = ref([])
const error = ref('')

const fmt = (v) => (v == null ? '—' : Number(v).toString())

onMounted(async () => {
  try {
    recipes.value = await api.listRecipes()
  } catch (e) {
    error.value = '加载配方失败：' + e.message
  }
})
</script>
