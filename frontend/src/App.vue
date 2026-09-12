<template>
  <div>
    <header class="topbar">
      <router-link to="/" class="brand">营养标签试算</router-link>
      <span class="badge">研发用 · 示例规则</span>
    </header>
    <div class="disclaimer">
      本应用使用随项目提供的<strong>示例规则集</strong>进行试算，仅用于研发阶段配方比较，
      <strong>不宣称符合任何国家或地区现行法规</strong>。原料未知值显示为数据缺口，不会按零处理。
    </div>
    <main class="container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
</script>
