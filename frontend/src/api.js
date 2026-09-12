const BASE = '/api'

async function request(path, options) {
  const res = await fetch(BASE + path, options)
  if (!res.ok) {
    let message = res.statusText
    try {
      const body = await res.json()
      if (body.error) message = body.error
    } catch { /* keep status text */ }
    throw new Error(message)
  }
  return res.json()
}

export const api = {
  listRecipes: () => request('/recipes'),
  getRecipe: (id) => request(`/recipes/${id}`),
  listRuleSets: () => request('/rulesets'),
  calculate: (recipeId, scenarios) =>
    request('/labels/calculate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipeId, scenarios })
    }),
  exportLabel: (recipeId, scenario) =>
    fetch(`${BASE}/labels/export`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipeId, scenarios: [scenario] })
    })
}
