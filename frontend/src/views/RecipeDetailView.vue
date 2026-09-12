<template>
  <div>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="recipe">
      <div class="card">
        <h2>{{ recipe.name }}</h2>
        <p class="muted">{{ recipe.description }}</p>
        <p>成品重量 {{ fmt(recipe.totalWeightG) }} g · 默认份量 {{ fmt(recipe.defaultServingSizeG) }} g</p>
        <h4>配料</h4>
        <table>
          <thead>
            <tr><th>原料</th><th class="num">用量 g</th><th>未知营养素</th></tr>
          </thead>
          <tbody>
            <tr v-for="ing in recipe.ingredients" :key="ing.ingredientName">
              <td>{{ ing.ingredientName }}</td>
              <td class="num">{{ fmt(ing.amountG) }}</td>
              <td>
                <span v-if="ing.unknownNutrients.length" class="pill gap">
                  {{ ing.unknownNutrients.join('、') }}
                </span>
                <span v-else class="muted">无</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>试算方案（可对比不同份量与规则版本）</h3>
        <div v-for="(s, i) in scenarios" :key="i" style="display:flex; gap:12px; align-items:center; margin-bottom:8px;">
          <label>
            规则版本
            <select v-model="s.ruleVersionId">
              <option v-for="v in ruleVersions" :key="v.id" :value="v.id">
                {{ v.label }}
              </option>
            </select>
          </label>
          <label>
            份量 g
            <input type="number" min="0.1" step="any" v-model.number="s.servingSizeG" style="width:100px" />
          </label>
          <button v-if="scenarios.length > 1" @click="scenarios.splice(i, 1)">删除</button>
        </div>
        <div style="display:flex; gap:12px;">
          <button class="primary" @click="run">计算</button>
          <button :disabled="scenarios.length >= 3" @click="addScenario">添加对比方案</button>
        </div>
      </div>

      <div v-if="results.length" class="scenario-grid">
        <div v-for="(label, i) in results" :key="i" class="card">
          <h3>
            {{ label.ruleSetCode }} v{{ label.ruleVersion }} · 每份 {{ fmt(label.servingSizeG) }} g
          </h3>
          <LabelTable :label="label" />
          <div style="margin-top:12px;">
            <button @click="exportLabel(i)">导出标签 JSON（含计算依据）</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import LabelTable from '../components/LabelTable.vue'

const props = defineProps({ id: { type: String, required: true } })

const recipe = ref(null)
const ruleSets = ref([])
const scenarios = ref([])
const results = ref([])
const error = ref('')

const ruleVersions = computed(() =>
  ruleSets.value.flatMap((rs) =>
    rs.versions.map((v) => ({ id: v.id, label: `${rs.code} v${v.version}` }))
  )
)

const fmt = (v) => (v == null ? '—' : Number(v).toString())

function addScenario() {
  scenarios.value.push({
    ruleVersionId: ruleVersions.value[0]?.id ?? null,
    servingSizeG: recipe.value?.defaultServingSizeG ?? 100
  })
}

async function run() {
  error.value = ''
  results.value = []
  try {
    results.value = await api.calculate(Number(props.id), scenarios.value)
  } catch (e) {
    error.value = '计算失败：' + e.message
  }
}

async function exportLabel(i) {
  try {
    const res = await api.exportLabel(Number(props.id), scenarios.value[i])
    if (!res.ok) throw new Error(res.statusText)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `label-recipe${props.id}-scenario${i + 1}.json`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = '导出失败：' + e.message
  }
}

onMounted(async () => {
  try {
    ;[recipe.value, ruleSets.value] = await Promise.all([
      api.getRecipe(props.id),
      api.listRuleSets()
    ])
    addScenario()
    await run()
  } catch (e) {
    error.value = '加载失败：' + e.message
  }
})
</script>
