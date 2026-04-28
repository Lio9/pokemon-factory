# Pokemon Factory 宝可梦工厂

一个基于 Spring Boot + Vue 3 + SQLite 的宝可梦工厂项目，支持图鉴数据导入、图鉴查询、用户认证和对战工厂。

## English Overview

Pokemon Factory is a Spring Boot + Vue 3 + SQLite project focused on three core product surfaces:

- **Dex data**: import, browse, and inspect Pokemon / moves / abilities / items.
- **Account flow**: register, sign in, restore session, and query the current user.
- **Battle Factory**: VGC-style doubles battle flow, factory runs, ranking points, and battle simulation.

### Current delivery state

- Backend battle logic has already been expanded beyond a simple prototype and now includes a growing set of competitive battle mechanics such as status interactions, side conditions, special system exclusivity, and factory progression.
- Frontend now includes a **lightweight locale layer** with Chinese / English switching for the app shell and core pages such as **Battle Factory**, **Login**, **Damage Calculator**, and **Import Manager**.
- The battle system is actively being aligned with **Pokemon Showdown Gen9 Doubles**. Core battle chain (turn order, damage calculation, basic status gates) is **100% consistent** with Showdown baseline. Extended mechanics coverage is at **89.5%** with detailed gap analysis documented.

### Repository structure

| Module | Purpose |
| --- | --- |
| `common` | Shared datasource config, database initialization scripts, bootstrap entry |
| `pokeDex` | Dex query and import business APIs |
| `user-module` | Register, login, current user APIs |
| `battleFactory` | Battle factory business logic, protected APIs, battle engine |
| `pokemon-factory-frontend` | Vue 3 frontend |
| `scripts` | Database init, CSV import, validation helpers |
| `docs` | Technical documentation and optimization roadmaps |

### Documentation

- [Battle System Optimization Roadmap](./docs/battle_system_optimization_roadmap.md) - A/B/C phase planning
- [Battle Optimization Progress](./docs/battle_optimization_progress.md) - Current progress tracking
- [Showdown Gap Analysis](./docs/battle_showdown_gap_analysis.md) - Detailed behavior differences and fix plans

### Quick start

1. Initialize the SQLite database through `common`:

```powershell
cd pokemon-factory-backend\common
mvn -DskipTests package
python ..\..\scripts\init_db.py
```

2. Start backend services in order:

```powershell
cd pokemon-factory-backend\common
mvn spring-boot:run

cd ..\pokeDex
mvn spring-boot:run

cd ..\battleFactory
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
- Backend: `mvn -q -pl common,user-module,pokeDex,battleFactory test`
- **Core battle regression**: `mvn --% -q -pl battleFactory -am -Dtest=BattleEngineRegressionBaselineTest test` (15/15 passing, 100% consistent)
- **Extended battle tests**: `mvn --% -q -pl battleFactory -am -Dtest=BattleEngineSwitchingTest test` (196/219 passing, 89.5% coverage)

### Internationalization note

The current internationalization work is intentionally lightweight: locale switching is handled in the frontend without introducing a heavy i18n dependency, which keeps the existing Vue structure stable while making the main user-facing flows bilingual. Some backend-generated battle logs and legacy pages may still emit Chinese text and can be expanded later.

### Acknowledgements

- [Pokemon Showdown](https://github.com/smogon/pokemon-showdown): this repository has been an important reference while hardening battle rules, status-control behavior, and competitive system design. Thanks to the Smogon community for building and maintaining such a high-quality open source battle simulator.

## 项目简介

Pokemon Factory 当前由以下几部分组成：

- common：共享数据库配置、SQL 初始化脚本、数据库初始化入口
- pokeDex：图鉴查询、导入相关业务接口
- user-module：用户注册、登录、当前用户信息
- battleFactory：对战工厂业务与鉴权链路
- pokemon-factory-frontend：Vue 3 前端界面
- scripts：数据库初始化、CSV 导入、校验等辅助脚本

## 当前约束

- 数据库保持当前嵌入式 SQLite 方案，不在当前优化阶段调整数据库引擎、表结构重写或迁移策略
- 当前优化优先收口接口契约、状态管理、测试和运维基线，而不是直接拆微服务
- 后续如需转向 Android，优先复用 battle、auth、pokemon 的接口层和状态层，不把页面编排逻辑直接带过去

## 功能特性

### 🚀 高性能导入

- 支持并发导入，大幅提升数据导入速度
- 智能重试机制，提高导入成功率
- 批量处理优化，减少数据库压力

### 📊 完整数据支持

- 宝可梦基础信息（名称、身高、体重等）
- 宝可梦形态和特性
- 类型、技能、道具等完整数据
- 进化链信息

### 🔐 用户与对战

- 支持注册、登录、当前用户信息查询
- 前端支持会话恢复、登录回跳和退出登录
- 对战工厂复用统一 JWT 鉴权链路
- **对战系统对齐 Pokemon Showdown Gen9 Doubles**：核心链路 100% 一致，扩展机制覆盖 89.5%
- 详细差距分析见 [battle_showdown_gap_analysis.md](./docs/battle_showdown_gap_analysis.md)

### 🧱 统一基础设施

- 所有数据库相关配置统一下沉到 common
- 所有后端模块共享同一份 SQLite 数据库
- 必须先启动 common 完成数据库初始化，业务模块只保留业务逻辑

### 🎨 现代化界面

- 响应式设计，支持多设备访问
- 实时数据展示和搜索
- 交互式宝可梦详情查看

### ⚔️ 对战系统状态

**当前对齐程度**：
- ✅ **核心回归基线**：15/15 (100%) - 回合顺序、伤害计算、基础状态门控完全对齐
- ⚠️ **扩展功能测试**：196/219 (89.5%) - 存在 11 个已知 P1 级偏差
- ✅ **P0 级错误**：0 - 无阻断性问题

**主要差距类别**：
- 特性联动（36%）：Intimidate+Defiant, Lightning Rod, Storm Drain 等
- 状态门控（18%）：Heal Block, Taunt+Choice Lock 等
- 伤害计算（18%）：吸收特性免疫逻辑
- 招式元数据（9%）：强制暴击招式
- 道具交互（9%）：White Herb 时序

**优化路线图**：
- **阶段 A**（已完成）：核心对战链路对齐
- **阶段 B**（进行中）：扩展机制覆盖（特性/道具/场地/回合结束链路）
- **阶段 C**（规划中）：融合收敛与发布门禁

详见：
- [对战系统优化路线图](./docs/battle_system_optimization_roadmap.md)
- [对战系统优化进度](./docs/battle_optimization_progress.md)
- [Showdown 差距分析](./docs/battle_showdown_gap_analysis.md)

## 技术栈

### 后端技术

- 框架：Spring Boot 4.0.5
- 数据库：SQLite
- 数据访问：MyBatis / MyBatis-Plus
- API：RESTful API
- 认证：JWT

### 前端技术

- 框架：Vue.js 3.x
- 构建工具：Vite
- 路由：Vue Router 4
- UI 框架：Tailwind CSS
- 组件库：Element Plus

### 数据导入

- 语言：Python 3.8+
- 并发：asyncio + aiohttp
- 数据库目标：SQLite

## 环境要求

### 系统要求

- Windows/Linux/macOS
- Java 17+（后端）
- Node.js 16+（前端）
- Python 3.8+（导入脚本）

### 网络要求

- 稳定的互联网连接
- 可访问 [PokeAPI](https://pokeapi.co/)

## 后端模块与启动顺序

### 模块职责

| 模块 | 作用 |
| --- | --- |
| common | 统一数据源、数据库初始化脚本、数据库初始化入口 |
| pokeDex | 图鉴查询与导入业务 |
| user-module | 用户认证与当前用户信息 |
| battleFactory | 对战工厂业务、受保护接口 |

### 启动原则

1. 先启动 common：负责初始化 SQLite 数据库结构。
1. 再启动业务模块：pokeDex / battleFactory 复用 common 的共享数据库配置。
1. 前端最后启动：通过 `VITE_API_BASE` / `VITE_DAMAGE_API_BASE` 指向后端接口；旧变量名 `VITE_API_BASE_URL` 仍兼容但不再作为主文档口径。

说明：

- common 启动时会检查当前数据库是否已经存在业务表。
- 如果已有数据，common 会跳过带有冷启动语义的核心初始化脚本，避免覆盖现有库。
- battleFactory 默认端口为 8090，pokeDex 默认端口为 8081。

## 快速开始

### 1. 初始化数据库（推荐）

本项目当前由 common 统一负责数据库初始化，并提供一键脚本。

步骤：

1. 构建后端 JAR（若尚未构建）

```powershell
cd pokemon-factory-backend\common
mvn -DskipTests package
```

1. 运行初始化脚本（会备份并删除旧 sqlite、启动 common、由 Java 初始化链自动建表并导入 CSV、最后校验数据）

```bash
python scripts/init_db.py
```

可选环境变量：

- SQLITE_DB_PATH：指定 sqlite 文件路径，默认值为 pokemon-factory-backend/pokemon-factory.db
- JAR_PATH：指定后端 jar 路径，默认值为 target 下的 common-0.0.1-SNAPSHOT-exec.jar
- MIGRATION_TIMEOUT：等待迁移完成超时，单位秒，默认值为 120

脚本运行完成后会由 common 的 Java 初始化链直接完成 CSV 导入，并运行 scripts/verify_sqlite.py 做基本校验。

### 2. 手动初始化（替代）

- 推荐使用跨平台启动脚本初始化数据库：

```bash
python scripts/start-backend.py
```

- 或手动启动 common JAR：

```bash
python scripts/start-backend.py
```

common 启动完成后，会统一从 `REMOTE_CSV_BASE_URL`（默认指向 PokeAPI CSV 源）按需下载 CSV 到本地缓存目录后再导入，并在导入过程中校验关键文件表头与核心数据完整性。旧的本地 `csv/` 目录配置已废弃，即使保留也只会被忽略并打印警告。当前也可以手动运行校验脚本：

```bash
python scripts/verify_sqlite.py
```

注意：仓库已将 sqlite 文件从主分支移除，不应将生成的数据库提交。初始化脚本会在本地生成并备份旧文件。

### 3. 启动图鉴后端服务

```powershell
cd pokemon-factory-backend\pokeDex
mvn spring-boot:run
```

### 4. 启动对战工厂后端服务

```powershell
cd pokemon-factory-backend\battleFactory
mvn spring-boot:run
```

### 5. 启动前端服务

```bash
cd pokemon-factory-frontend
npm install
npm run dev
```

当前临时开发端口统一使用 `http://localhost:7890`；`npm run serve` 的预览端口与 `docker-compose.local.yml` 里的前端暴露端口也已同步为 7890。

开发时请确认前端环境变量：

- `VITE_API_BASE=/api/pokedex`
- `VITE_DAMAGE_API_BASE=/api/damage`
- `VITE_SPRITES_BASE=https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites`（默认直接走线上 sprites 资源）
- `REMOTE_CSV_BASE_URL=https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv`（common 初始化时按需拉取 CSV）
- `CSV_CACHE_DIR=/tmp/pokemon-factory/csv-cache`（可选，自定义远程 CSV 缓存目录）

### 6. 健康检查

- pokeDex：[http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- battleFactory：[http://localhost:8090/actuator/health](http://localhost:8090/actuator/health)
- battleFactory 也会公开：[http://localhost:8090/actuator/info](http://localhost:8090/actuator/info)

### 7. 当前推荐的本地开发方式

当前仓库**不把 Docker 作为主开发路径**。推荐流程仍然是：

1. 用 `python scripts/init_db.py` 初始化本地 SQLite 数据库
2. 分别启动 `common`、`pokeDex`、`battleFactory`
3. 启动前端 `npm run dev`

仓库里虽然保留了容器相关文件，但它们当前仅作为历史草案/备用参考，**不作为主要维护对象**。

## 数据初始化与导入脚本说明

### 当前脚本职责

- scripts/init_db.py：一键删库、启动 common、等待 Java 初始化链建表并导入 CSV、最后校验
- scripts/start-backend.py：跨平台直接启动 common 的 exec jar，手动观察初始化日志
- scripts/verify_sqlite.py：校验 SQLite 关键表和核心数据量

### 使用方法

```bash
# 一键初始化数据库 + 导入 CSV + 校验
python scripts/init_db.py

# 跨平台手动启动 common
python scripts/start-backend.py

# 校验 SQLite 数据
python scripts/verify_sqlite.py
```

### 配置说明

关键环境变量：

| 变量名 | 说明 |
| --- | --- |
| SQLITE_DB_PATH | 指定 SQLite 文件路径 |
| JAR_PATH | 指定 common 启动 jar 路径 |
| MIGRATION_TIMEOUT | 等待 common 初始化完成的超时时间 |
| JWT_SECRET | JWT 签名密钥；未提供时会使用开发期临时密钥 |

## API 接口

### 用户认证接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /api/user/register | POST | 注册并直接返回登录态 |
| /api/user/login | POST | 登录并返回 token 与用户资料 |
| /api/user/me | GET | 获取当前登录用户 |

### 图鉴接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /api/pokedex/pokemon/list | GET | 获取宝可梦列表 |
| /api/pokedex/pokemon/{id} | GET | 获取宝可梦详情 |
| /api/pokedex/types | GET | 获取类型列表 |
| /api/pokedex/moves/list | GET | 获取技能列表 |
| /api/pokedex/abilities/list | GET | 获取特性列表 |
| /api/pokedex/items/list | GET | 获取物品列表 |
| /api/import-optimized/all-fast | POST | 启动全量导入任务 |
| /api/import-optimized/import-status/{taskId} | GET | 查询导入任务状态 |

说明：

- `pokeDex` 模块中仍保留部分旧路径（如 `/api/pokemon/**`）用于兼容，但前端与文档默认口径统一使用 `/api/pokedex/**`。
- 前端“导入管理”页当前只负责**启动导入任务与查看任务状态**，不会执行删库或清空后端数据。

### 对战接口

/api/battle/** 由 battleFactory 提供，需携带有效 JWT 访问受保护资源。

## 性能优化

### 数据库优化

1. 索引优化：为常用查询字段添加索引
1. 批量操作：使用批量插入减少数据库交互
1. 连接池：合理配置数据库连接池大小

### 网络优化

1. 并发控制：限制并发连接数避免服务器压力
1. 超时设置：合理设置连接和读取超时
1. 重试策略：指数退避重试提高成功率

### 缓存策略

1. 数据缓存：缓存常用数据减少 API 调用
1. 内存管理：及时清理不需要的缓存数据
1. 缓存失效：设置合理的缓存过期时间

## 故障排除

### 常见问题

1. 数据库连接失败

   - 检查 common 是否已经先启动
   - 检查 SQLITE_DB_PATH 是否指向正确位置
   - 检查 SQLite 文件所在目录是否有写权限

1. 导入失败

   - 检查网络连接
   - 查看日志文件了解具体错误
   - 确认 PokeAPI 服务可用性

1. 前端无法访问接口

   - 确认 pokeDex / battleFactory 已正常启动
   - 检查 `VITE_API_BASE` / `VITE_DAMAGE_API_BASE` 是否配置正确
   - 检查端口是否与 README 中说明一致

### 日志查看

```bash
# 查看导入日志
tail -f logs/efficient_pokemon_import.log

# 查看后端日志
tail -f logs/pokemon-factory.log

# 查看前端日志
# 在浏览器控制台查看
```

## 贡献指南

1. Fork 项目
1. 创建特性分支（git checkout -b feature/AmazingFeature）
1. 提交更改（git commit -m 'Add some AmazingFeature'）
1. 推送到分支（git push origin feature/AmazingFeature）
1. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证，详情见 LICENSE。

## 致谢

- [PokeAPI](https://pokeapi.co/)：宝可梦数据来源
- [Pokemon Showdown](https://github.com/smogon/pokemon-showdown)：battle 规则完善过程中参考了其开源实现思路与机制研究沉淀，感谢 Smogon 社区的长期贡献。本项目正在积极对齐 Showdown Gen9 Doubles 的行为，详细差距分析见 [docs/battle_showdown_gap_analysis.md](./docs/battle_showdown_gap_analysis.md)

## 联系方式

- 项目维护者：Lio9
- 邮箱：[Lio9@qq.com]
- GitHub：[https://github.com/Lio9/pokemon-factory](https://github.com/Lio9/pokemon-factory)

---
