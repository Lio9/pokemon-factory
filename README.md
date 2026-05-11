# Pokemon Factory 宝可梦工厂

一个基于 Spring Boot + Vue 3 + SQLite 的宝可梦工厂项目，支持图鉴数据导入、图鉴查询、用户认证和对战工厂。

## English Overview

Pokemon Factory is a Spring Boot + Vue 3 + SQLite project focused on three core product surfaces:

- **Dex data**: import, browse, and inspect Pokemon / moves / abilities / items.
- **Account flow**: register, sign in, restore session, and query the current user.
- **Battle Factory**: VGC-style doubles battle flow, factory runs, ranking points, and battle simulation.

### Current delivery state

- Backend expanded beyond a simple prototype with competitive battle mechanics (status interactions, side conditions, special system exclusivity, factory progression).
- Frontend features a lightweight locale layer (Chinese / English switching) and a consistent glass-morphism design system across all pages.
- The battle system is aligned with **Pokemon Showdown Gen9 Doubles**. Core chain: 100% consistent. Extended mechanics: 89.5%.
- API integration layer unified: 401 auto-clear session, centralized error handling, consistent response envelope.

### Repository structure

| Module | Artifact | Purpose |
|--------|----------|---------|
| `common` | `common` | Shared datasource, MyBatis config, SQLite connection pool |
| `poke-dex` | `pokedex` | Dex query and import business APIs |
| `user-module` | `user` | Register, login, current user APIs |
| `battle-factory` | `battle` | Battle factory, protected APIs, battle engine |
| `pokemon-factory-frontend` | — | Vue 3 frontend |
| `scripts` | — | Database init, CSV import, validation helpers |
| `docs` | — | Technical documentation and roadmaps |

### Quick start

1. Initialize the SQLite database:

```powershell
cd pokemon-factory-backend\common
mvn -DskipTests package
python ..\..\scripts\init_db.py
```

2. Start backend services:

```powershell
cd pokemon-factory-backend\poke-dex
mvn spring-boot:run

cd ..\battle-factory
mvn spring-boot:run
```

3. Start the frontend:

```powershell
cd pokemon-factory-frontend
npm install
npm run dev
```

### Validation baseline

- Frontend: `npm run lint && npm run build`
- Backend: `mvn -q -pl common,poke-dex,battle-factory,user-module test`
- **Core battle regression**: `mvn --% -q -pl battle-factory -am -Dtest=BattleEngineRegressionBaselineTest test`

### Acknowledgements

- [Pokemon Showdown](https://github.com/smogon/pokemon-showdown): this repository has been an important reference while hardening battle rules, status-control behavior, and competitive system design. Thanks to the Smogon community for building and maintaining such a high-quality open source battle simulator.

## 项目简介

- **common**：共享数据源、MyBatis 配置、SQLite 连接池（库模块，无独立启动入口）
- **poke-dex**：图鉴查询与导入业务接口（端口 8081）
- **user-module**：用户注册、登录、当前用户信息
- **battle-factory**：对战工厂业务与鉴权链路（端口 8090）
- **pokemon-factory-frontend**：Vue 3 前端界面
- **scripts**：数据库初始化、CSV 导入、校验等辅助脚本
- **docs**：技术文档与优化路线图

## 技术栈

### 后端

- 框架：Spring Boot 4.0.5
- 数据库：SQLite
- 数据访问：MyBatis / MyBatis-Plus
- API：RESTful API
- 认证：JWT

### 前端

- 框架：Vue.js 3.x
- 构建工具：Vite
- 路由：Vue Router 4
- UI 框架：Tailwind CSS + Element Plus
- 设计：毛玻璃卡片体系（glass-card）、宝可梦属性色标签、页面过渡动画

### 数据导入

- 语言：Python 3.8+
- 并发：asyncio + aiohttp
- 数据库目标：SQLite

## 环境要求

- Windows / Linux / macOS
- Java 17+
- Node.js 16+
- Python 3.8+

## 后端模块与启动顺序

| 模块 | 作用 | 端口 |
|------|------|------|
| common | 统一数据源、共享配置（库模块，不独立运行） | — |
| poke-dex | 图鉴查询与导入业务 | 8081 |
| battle-factory | 对战工厂、受保护接口 | 8090 |
| user-module | 用户认证（作为依赖被 battle-factory 使用） | — |

### 启动顺序

1. 先初始化数据库：`python scripts/init_db.py`
2. 启动 `poke-dex`：`cd pokemon-factory-backend\poke-dex && mvn spring-boot:run`
3. 启动 `battle-factory`：`cd pokemon-factory-backend\battle-factory && mvn spring-boot:run`
4. 启动前端：`cd pokemon-factory-frontend && npm run dev`

说明：

- 数据库初始化已交给 `scripts/init_db.py`，common 模块不再需要独立启动
- `battle-factory` 默认端口 8090，`poke-dex` 默认端口 8081

## 初始化数据库

```powershell
# 构建后端
cd pokemon-factory-backend\common
mvn -DskipTests package

# 运行初始化脚本
python scripts/init_db.py
```

初始化脚本会自动创建 SQLite 数据库、建表、导入 CSV 数据，并进行基本校验。

### 环境变量

| 变量 | 说明 |
|------|------|
| `SQLITE_DB_PATH` | SQLite 文件路径 |
| `JWT_SECRET` | JWT 签名密钥 |
| `REMOTE_CSV_BASE_URL` | CSV 数据源地址 |

## API 接口

### 用户认证（battle-factory → /api/user/**）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 注册并返回登录态 |
| `/api/user/login` | POST | 登录 |
| `/api/user/me` | GET | 当前用户信息 |

### 图鉴（poke-dex → /api/pokedex/**）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pokedex/pokemon/list` | GET | 宝可梦列表（分页+搜索+筛选） |
| `/api/pokedex/pokemon/{id}` | GET | 宝可梦详情 |
| `/api/pokedex/types` | GET | 属性列表 |
| `/api/pokedex/moves/list` | GET | 技能列表 |
| `/api/pokedex/abilities/list` | GET | 特性列表 |
| `/api/pokedex/items/list` | GET | 物品列表 |
| `/api/import-optimized/all-fast` | POST | 全量导入任务 |
| `/api/import-optimized/import-status/{taskId}` | GET | 导入任务状态 |

### 对战（battle-factory → /api/battle/**）

需携带有效 JWT 访问受保护资源。

## 前端设计系统

- **毛玻璃卡片**：`.glass-card` / `.glass-card-interactive` 统一容器风格
- **属性色标签**：`.type-badge` 覆盖 18 种宝可梦属性色
- **品牌色系**：每种列表页使用不同渐变主色
- **入场动画**：交错 slide-up 动画 + 页面切换过渡
- **骨架屏**：加载时脉冲骨架占位
- **本地化**：中文 / English 双语言切换，支持日语字典拓展

## 当前约束

- 数据库保持嵌入式 SQLite 方案
- 优先收口接口契约、状态管理、测试和运维基线
- 后续如需转向 Android，优先复用 battle、auth、pokemon 的接口层和状态层

## 对战系统对齐程度

- **核心回归基线**：15/15 (100%) — 回合顺序、伤害计算、基础状态门控完全对齐 Showdown
- **扩展功能测试**：196/219 (89.5%)
- **P0 级错误**：0

详见 [docs/battle_showdown_gap_analysis.md](./docs/battle_showdown_gap_analysis.md) 和 [docs/battle_system_optimization_roadmap.md](./docs/battle_system_optimization_roadmap.md).

## 贡献指南

1. Fork 项目
2. 创建特性分支（`git checkout -b feature/xxx`）
3. 提交更改（`git commit -m 'Add xxx'`）
4. 推送到分支（`git push origin feature/xxx`）
5. 开启 Pull Request

## 许可证

MIT License.

## 致谢

- [PokeAPI](https://pokeapi.co/)：宝可梦数据来源
- [Pokemon Showdown](https://github.com/smogon/pokemon-showdown)：battle 规则参考
