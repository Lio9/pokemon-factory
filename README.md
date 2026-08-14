# Pokemon Factory 宝可梦工厂

> 一体化宝可梦对战平台 — 图鉴 + 对战引擎 + AI 对手

---

## 快速启动

**前置：** JDK 21、Node.js 20+、Maven 3.9+、Python 3.10+

```powershell
# 1️⃣ 初始化数据库（schema + 静态数据）
python scripts/setup.py

# 2️⃣ 编译后端（首次或代码变更后）
cd backend
mvn package -pl battle -am -DskipTests "-Dmaven.test.skip=true" -q

# 3️⃣ 启动后端（端口 8084）
Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList '/c','cd /d D:\learn\pokemon-factory && java -jar backend\battle\target\battle-0.0.1-SNAPSHOT.jar'

# 4️⃣ 启动前端（端口 7894）
cd frontend
npm run dev

# 5️⃣ 打开 http://localhost:7894
```

等后端日志出现 `Started BattleFactoryApplication` 后即可访问。

### 补充完整数据（含中文名、描述、效果，需联网，约 20-40 分钟）

```powershell
# 从 PokeAPI v2 补充完整数据（含 zh-Hans 中文名）
python scripts/init_data.py --online

# 补全技能/道具的描述、效果、分类
python scripts/data_maintenance.py
```

> **注意：** PokeAPI 国际服请求限速，首次下载耗时较长。数据会缓存到 `data/pokeapi-cache/`，再次运行会跳过已下载内容。
> `data_maintenance.py` 专用于补全离线种子缺少的描述、效果、分类数据。

---

## 架构

```
┌──────────────┐     ┌──────────────────────────────────────┐
│  前端(7894)  │────▶│  battle (8084)                       │
│  Vue 3+Vite  │     │  ├── battle: 对战引擎、AI、访客模式   │
│  Element Plus│     │  ├── pokedex: 图鉴查询、伤害计算      │
│  Tailwind    │     │  ├── user: 登录注册、JWT 认证          │
│  PWA + 缓存  │     │  └── common: 数据库、CSV 导入          │
└──────────────┘     └──────────────────────────────────────┘
                         SQLite (backend/pokemon-factory.db)
```

- **单 JAR** 启动全部功能（端口 8084）
- **SQLite 单文件数据库**，无需安装数据库服务
- **Vite 反向代理** 把 `/api/*` 转发到 `localhost:8084`
- **Python 初始化** 数据库无需启动 Java 后端

---

## 项目结构

```
pokemon-factory/
├── backend/                          # Java 后端（多模块 Maven）
│   ├── common/                       # 数据库、CSV导入、速率限制
│   ├── user/                         # 认证、JWT
│   ├── pokedex/                      # 图鉴 CRUD、伤害计算
│   ├── battle/                       # ★ 主入口（编译此模块，端口 8084）
│   │   ├── controller/               #   BattleController(对战/工厂/游客)
│   │   ├── engine/                   #   回合制对战引擎、AI 决策
│   │   ├── service/                  #   对战编排、天梯、对手池
│   │   └── effect/                   #   特性/道具效果系统
│   └── config/                       # JWT 密钥
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── views/                    # 12 个页面组件
│       ├── components/               # 16+ 通用组件
│       ├── composables/              # 组合式逻辑（状态机、轮询、派生状态）
│       ├── services/                 # HTTP 客户端、缓存、PWA、对战 API
│       └── stores/                   # Pinia 状态管理
├── scripts/                          # ★ 工具脚本（见下）
├── data/                             # 缓存数据
│   ├── image/                        # 宝可梦精灵图
│   └── pokeapi-cache/                # PokeAPI v2 JSON 缓存
└── docker-compose.yml                # Docker 部署
```

---

## 脚本参考

### 数据库初始化

| 命令 | 说明 |
|------|------|
| `python scripts/setup.py` | **总控**：建表 → 静态数据 → 验证 |
| `python scripts/setup.py --verify` | 仅验证数据完整性 |
| `python scripts/init_data.py` | 离线种子数据（34 特性/46 道具/77 技能，中文名） |
| `python scripts/init_data.py --online` | **在线下载** 完整数据（含中文名，需联网 20-40 分钟） |
| `python scripts/init_data.py --force` | 清空数据后重新下载 |
| `python scripts/init_data.py --clear-cache` | 清除 PokeAPI 缓存 |
| `python scripts/fix_move_data.py` | **从本地缓存重放招式字段**（type_id/power/pp 等修复） |
| `python scripts/rebuild_move_table.py` | **完全重建 move 表**（离线种子字段错位时使用，自动备份） |

### 数据维护

| 命令 | 说明 |
|------|------|
| `python scripts/data_maintenance.py` | 补全技能/道具的描述、效果、分类（需联网） |
| `python scripts/data_maintenance.py --verify` | 检查数据完整性 |
| `python scripts/data_maintenance.py --fix moves` | 仅修复技能数据 |
| `python scripts/data_maintenance.py --fix items` | 仅修复道具数据 |
| `python scripts/verify_sqlite.py` | 校验 SQLite 完整性和示例数据 |
| `python scripts/backup_db.py` | 自动备份数据库（保留 30 天） |
| `python scripts/download_sprites.py` | 下载精灵图到 `data/image/` |
| `python scripts/generate_effect_seeds.py` | 生成特性/道具效果种子 JSON |

### 数据状态

| 数据 | 数量 | 中文名 | 描述/效果 | 分类 |
|------|------|--------|-----------|------|
| 宝可梦 | 1025 个物种 | ✅ | ✅ | — |
| 技能 | 874 个 | ✅ | ✅ 785/874 | ✅ |
| 特性 | 358 个 | ✅ | ✅ 252/358 | — |
| 道具 | 2135 个 | ✅ | ⚠️ 需运行 `data_maintenance.py` | ⚠️ 同上 |
| 属性/相克 | 18 种 | ✅ | ✅ | — |

> 运行 `python scripts/data_maintenance.py` 可补全道具描述/效果/分类。

---

## 常见问题

### 宝可梦列表为空 / 数据不全

运行 `python scripts/init_data.py --online` 从 PokeAPI 下载完整数据。
首次下载需 20-40 分钟（网络限速），之后本地缓存可复用。

### 图片不显示

后端返回的 sprite URL 基于 `image-base-url` 配置，默认使用 PokeAPI 远程源。
可运行 `python scripts/download_sprites.py` 下载到本地。

### 首页统计显示 "—"

首页会调用 `/api/pokedex/summary` 获取统计数据，数据下载完成后自动恢复。

### 端口冲突

后端默认 8084，前端默认 7894。可在各自配置文件中修改。

---

## 开发指引

**后端测试：**
```powershell
cd backend
mvn test -pl battle -am
```

**前端校验：**
```powershell
cd frontend
npm run lint                      # ESLint
npx vue-tsc --noEmit              # TypeScript 检查
```

**API 文档：** 后端启动后 http://localhost:8084/swagger-ui.html

---

## Docker 部署

```powershell
docker compose up -d
```

同时启动后端一体服务 + 前端 Nginx。

---

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| JVM | OpenJDK | 21 |
| 后端框架 | Spring Boot | 4.0.5 |
| ORM | MyBatis + MyBatis-Plus | 4.0 + 3.5.9 |
| 数据库 | SQLite (xerial JDBC) | 3.47 |
| 前端 | Vue 3 + Vite | 6.x |
| UI | Element Plus + Tailwind CSS | 2.x + 3.x |
| 状态管理 | Pinia + Vue Router | — |
| 认证 | JWT (HS256) | — |
| 图片 | 本地优先 → PokeAPI 回退 → 默认图 | — |
| 部署 | Docker Compose (Nginx + JAR) | — |

---

## 数据初始化说明

### 架构变迁

- **v1.x** — 三个独立后端服务（pokedex:8082 / user:8083 / battle:8084）
- **v2.0** — 合并为 `one-server`（8081），CSV 离线数据导入
- **v2.1+** — `one-server` 移除，`BattleFactoryApplication`（8084）统一入口
  - 数据库初始化从 Java 迁移到 Python 脚本
  - 数据来源从 PokeAPI GitHub CSV 迁移到 PokeAPI v2 JSON（含中文名）

### 数据恢复

如果数据库被清空或损坏：

```powershell
# 1. 删除旧数据库
Remove-Item backend\pokemon-factory.db

# 2. 重建 schema + 静态数据 + 离线种子
python scripts/init_data.py

# 3. 在线补充完整数据（可选，含中文名）
python scripts/init_data.py --online
```
