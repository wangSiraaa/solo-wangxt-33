# 营养标签试算应用（研发用）

食品研发配方试验阶段的营养标签试算工具：Vue 3 前端展示配方 / 份量 / 标签预览，
Spring Boot 后端负责计算，PostgreSQL 保存原料数据与规则版本。
所有换算与舍入均使用 `BigDecimal`。

> **免责声明**：本应用使用随项目提供的**示例规则集**（`SAMPLE-RULESET`），
> 仅用于研发阶段配方比较，**不宣称符合任何国家或地区现行法规**。

## 核心规则

1. **未舍入总量**：每种营养素先按配方总量累计未舍入值（BigDecimal，DECIMAL128 精度）。
2. **双列独立修约**：每 100 g 与每份展示值分别从**未舍入总量**计算并独立舍入，
   绝不把已舍入结果继续换算（有单测 `perServingNeverDerivesFromRoundedPer100g` 守护）。
3. **数据缺口**：原料某营养素值为 `NULL`（未知）时，该营养素整行标记为缺口
   （显示「—」与缺口原料清单），**不会默认为零**；已知零值与未知值严格区分。
   派生营养素（如盐当量）的缺口随源营养素传播。
4. **规则版本化**：舍入间隔、零阈值、微量阈值按规则版本存储，
   同一配方可并排对比不同份量与不同规则版本。
5. **可追查**：标签导出 JSON 包含计算依据——未舍入总量、两列未舍入值、
   每个原料的逐项贡献、应用的规则参数与舍入约定。

### 显示语义

| 条件（展示单位下的未舍入值 v） | 显示 |
| --- | --- |
| 数据缺口 | `—` + 缺口标记 |
| `v < 零阈值` | `0` |
| `零阈值 ≤ v < 微量阈值` | `<微量阈值`（微量） |
| 其他 | 按舍入间隔 HALF_UP 修约后的数值 |

派生营养素示例：盐当量 = 钠(mg) × 2.5 ÷ 1000（克毫克换算），
能量(kJ) = 能量(kcal) × 4.184，均作用于未舍入总量。

## 目录结构

```
backend/    Spring Boot 3（Java 21）+ JPA + Flyway
  src/main/java/com/example/nutrilabel/calc/   纯 Java 计算引擎（不依赖 JPA，可单测）
  src/main/resources/db/migration/             schema、示例规则集、边界测试数据
frontend/   Vue 3 + Vite + vue-router
docker-compose.yml                             PostgreSQL + 后端 + 前端
```

## 运行

### Docker（推荐）

```bash
docker compose up --build
# 前端 http://localhost:8081 ，后端 http://localhost:8080
```

### 本地开发

```bash
# 1. 启动 PostgreSQL（或 docker compose up db），建库 nutrilabel / 用户 nutri / 密码 nutri
# 2. 后端（Flyway 自动建表并写入示例数据）
cd backend && mvn spring-boot:run
# 3. 前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## 测试

```bash
cd backend && mvn test
```

- `LabelCalculatorTest`：纯引擎测试——四舍五入临界（2.575→2.6、0.25→0.3）、
  零阈值边界包含性（0.5 mg → `<5` 而非 `0`）、克毫克换算（盐当量）、
  kcal→kJ 换算、微量显示、缺口非零、已知零非缺口、双列独立修约。
- `LabelApiSmokeTest`：H2 内存库端到端——迁移、接口、缺口配方、规则版本差异、导出依据。

## 内置测试数据

| 配方 | 用途 |
| --- | --- |
| 黄油曲奇 | 常规演示；`yield_weight_g` 体现烘焙失水 |
| 微量测试配方 | 小用量香草精/泡打粉 → 钠、糖落入微量区间 |
| 舍入临界配方 | 25 g 份量下蛋白质 2.575 g、脂肪 0.25 g、钠 0.5 mg，落在修约/阈值边界 |
| 数据缺口配方 | 全脂奶粉钠未知 → 钠与盐当量显示缺口 |

规则集含 v1.0 / v1.1 两个版本（能量、蛋白质、钠等修约间隔不同），用于演示版本对比。

## API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/recipes` | 配方列表 |
| GET | `/api/recipes/{id}` | 配方详情（含各原料未知营养素清单） |
| GET | `/api/rulesets` | 规则集与版本 |
| POST | `/api/labels/calculate` | `{recipeId, scenarios:[{ruleVersionId, servingSizeG}]}` → 多方案标签 |
| POST | `/api/labels/export` | 单方案导出 JSON（含完整计算依据，带下载头） |
