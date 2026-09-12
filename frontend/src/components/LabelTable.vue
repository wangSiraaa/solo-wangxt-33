<template>
  <table>
    <thead>
      <tr>
        <th>营养素</th>
        <th class="num">每 100 g</th>
        <th class="num">每份（{{ fmt(label.servingSizeG) }} g）</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <template v-for="n in label.nutrients" :key="n.code">
        <tr>
          <td>
            {{ n.name }}
            <span class="muted">({{ n.displayUnit }})</span>
            <span v-if="!n.dataComplete" class="pill gap"
                  :title="'未知值原料：' + n.missingIngredients.join('、')">
              数据缺口
            </span>
          </td>
          <td class="num">
            {{ n.per100g.display }}
            <StatePill :state="n.per100g.state" />
          </td>
          <td class="num">
            {{ n.perServing.display }}
            <StatePill :state="n.perServing.state" />
          </td>
          <td class="num">
            <button @click="toggle(n.code)">
              {{ expanded.has(n.code) ? '收起' : '依据' }}
            </button>
          </td>
        </tr>
        <tr v-if="expanded.has(n.code)">
          <td colspan="4">
            <div class="mono">
              <div>
                未舍入总量（储存单位）：{{ n.totalUnrounded }}
                <template v-if="!n.dataComplete">（仅已知原料的部分合计）</template>
              </div>
              <div>
                每 100 g 未舍入：{{ n.per100g.unrounded }} → 显示 {{ n.per100g.display }}
                ｜ 每份未舍入：{{ n.perServing.unrounded }} → 显示 {{ n.perServing.display }}
              </div>
              <div v-if="n.rule">
                规则参数：修约间隔 {{ n.rule.roundingIncrement }} {{ n.displayUnit }}
                ｜ 零阈值 {{ n.rule.zeroThreshold }}
                <template v-if="n.rule.traceThreshold != null">｜ 微量阈值 {{ n.rule.traceThreshold }}</template>
              </div>
              <div v-if="!n.dataComplete" style="color: var(--gap)">
                缺口原料：{{ n.missingIngredients.join('、') }}（未知值未按零计入）
              </div>
              <table>
                <thead>
                  <tr>
                    <th>原料</th>
                    <th class="num">用量 g</th>
                    <th class="num">含量 /100 g</th>
                    <th class="num">对总量贡献</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="c in n.contributions" :key="c.ingredientName">
                    <td>{{ c.ingredientName }}</td>
                    <td class="num">{{ c.amountG }}</td>
                    <td class="num">{{ c.valuePer100g ?? '未知' }}</td>
                    <td class="num">{{ c.contributionToTotal ?? '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </td>
        </tr>
      </template>
    </tbody>
  </table>
</template>

<script setup>
import { ref, h } from 'vue'

defineProps({ label: { type: Object, required: true } })

const expanded = ref(new Set())
function toggle(code) {
  const next = new Set(expanded.value)
  if (next.has(code)) next.delete(code)
  else next.add(code)
  expanded.value = next
}

const fmt = (v) => (v == null ? '—' : Number(v).toString())

const StatePill = (props) => {
  if (props.state === 'TRACE') return h('span', { class: 'pill trace' }, '微量')
  if (props.state === 'ZERO') return h('span', { class: 'pill zero' }, '低于零阈值')
  if (props.state === 'GAP') return h('span', { class: 'pill gap' }, '缺口')
  return null
}
StatePill.props = { state: String }
</script>
