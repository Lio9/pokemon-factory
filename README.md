# Pokemon Factory 宝可梦工厂

> 一体化宝可梦对战平台 — 图鉴 + 对战引擎 + AI 对手

---

## 快速启动

**前置：** JDK 21、Node.js 20+、Maven 3.9+

```powershell
# 1️⃣ 初始化数据库（15 秒，无需启动后端）
python scripts/setup.py

# 2️⃣ 编译后端（首次或代码变更后）
cd backend & mvn package -pl one-server -am -Dmaven.test.skip=true -q

# 3️⃣ 启动后端（新窗口）
java -jar one-server\target\one-server-0.0.1-SNAPSHOT.jar

# 4️⃣ 启动前端（新窗口）
cd frontend & npx vite --port 7894

# 5️⃣ 打开 http://localhost:7894
```

等后端窗口出现 `Started OneServerApplication in X seconds` 后即可访问。

> **注意：** 数据库初始化现在完全由 Python 脚本处理，后端不再自动建表。
> 即使数据库已经初始化，`CommonDatabaseInitializer` 也会自动补齐缺失的列和索引。

**前置：** JDK 21、Node.js 20+、Maven 3.9+

```powershell
# 1️⃣ 初始化数据库（15 秒，无需启动后端）
python scripts/setup.py --quick

# 2️⃣ 编译后端（首次或代码变更后）
cd backend
mvn package -pl one-server -am -Dmaven.test.skip=true -q

# 3️⃣ 启动后端（新窗口）
java -jar one-server\target\one-server-0.0.1-SNAPSHOT.jar

# 4️⃣ 启动前端（新窗口）
cd frontend
npx vite --port 7894

# 5️⃣ 打开 http://localhost:7894
```

等后端窗口出现 `Started OneServerApplication in X seconds` 后即可访问。

---

## 架构

```
┌──────────────┐     ┌──────────────────────────────────────┐
│  前端(7894)  │────▶│  one-server (8081)                    │
│  Vue 3+Vite  │     │  ├── battle: 对战引擎、访客模式、AI  │
│  Element Plus│     │  ├── pokedex: 图鉴查询、伤害计算      │
│  Tailwind    │     │  ├── user: 登录注册、JWT 认证          │
│  PWA + 缓存  │     │  └── common: 数据库、CSV 导入          │
└──────────────┘     └──────────────────────────────────────┘
                         SQLite (pokemon-factory.db)
```

- **一个后端 JAR** 替代原来的三个独立服务
- **SQLite 单文件数据库**，无需安装数据库服务
- **Vite 反向代理** 把全部 `/api/*` 转发到 `localhost:8081`
- **Python 初始化** 数据库无需启动 Java 后端

---

## 项目结构

```
pokemon-factory/
├── backend/                          # Java 后端（多模块 Maven）
│   ├── common/                       # 数据库、CSV导入、速率限制
│   ├── user/                         # 认证、JWT
│   ├── pokedex/                      # 图鉴 CRUD
│   ├── battle/                       # 对战引擎、AI、访客对战
│   └── one-server/                   # ★ 一体化入口（编译此模块）
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── views/                    # 12 个页面组件
│       ├── components/               # 16 个通用组件
│       ├── services/                 # HTTP 客户端、缓存、PWA
│       └── stores/                   # Pinia 状态管理
├── scripts/                          # ★ 工具脚本（见下）
├── data/image/                       # 宝可梦精灵图（可选下载）
└── docker-compose.yml               # Docker 部署
```

---

## 脚本参考

### 数据库初始化（无需后端）

| 命令 | 说明 |
|------|------|
| `python scripts/setup.py` | **总控**：schema → data import → verify |
| `python scripts/setup.py --quick` | 快速：schema + effects + verify（跳过数据导入） |
| `python scripts/setup.py --verify` | 仅验证 52 个表的完整性 |
| `python scripts/db_schema.py` | 建表/补列（幂等） |
| `python scripts/db_effects.py` | 加载特性/道具 JSON 效果种子 |
| `python scripts/init_data.py` | 离线数据导入（PokeAPI 备用） |

### 数据维护

| 命令 | 说明 |
|------|------|
| `python scripts/verify_sqlite.py` | 校验 SQLite 完整性和示例数据 |
| `python scripts/backup_db.py` | 自动备份数据库（保留 30 天） |
| `python scripts/download_sprites.py` | 从 PokeAPI 下载精灵图到 `data/image/` |
| `python scripts/generate_effect_seeds.py` | 生成特性/道具效果种子 JSON |

```powershell
# 数据库初始化（首次使用）
python scripts/setup.py

# 可选：下载宝可梦精灵图（前端自动回退远程源）
python scripts/download_sprites.py --range 1 151
```

---

## 开发指引

**后端测试（511 个用例）：**
```powershell
cd backend
mvn test -pl battle -am
```

**前端校验：**
```powershell
cd frontend
npm run lint                      # ESLint
npx vue-tsc --noEmit              # TypeScript
```

**API 文档：** 后端启动后 http://localhost:8081/swagger-ui.html

**图片下载：**
```powershell
# 全部宝可梦（1–1025 号）
python scripts/download_sprites.py

# 仅第一世代
python scripts/download_sprites.py --range 1 151

# 校验已下载图片
python scripts/download_sprites.py --verify
```

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
| 会话 | Pinia + JWT | — |
| 图片 | 本地优先 → PokeAPI 回退 → 默认图 | — |
| 图片 | 本地优先 → PokeAPI 回退 → 默认图 | — |
| 部署 | Docker Compose (Nginx + JAR) | — |

---

## 数据库初始化迁移说明

> **2025-06-30 重构：** 数据库全量初始化从 Java 后端剥离到 Python 脚本。

### 变更内容

- **移除** `CommonDatabaseInitializer` 中的 SQL 脚本执行和 CSV 数据导入
- **简化** `application-common.yml`，移除 `initialize-on-startup` 和 `bootstrap-scripts`
- **新增** `scripts/setup.py` 作为一键初始化入口
- **保留** `CommonDatabaseInitializer` 仅做幂等的 schema 迁移（补充缺失列/索引）

### 为什么这样做

1. **解耦**：建表不用启动 Java 后端，调试更快
2. **幂等**：后端只做安全的结构迁移，不破坏已有数据
3. **脚本化**：CI/CD 流程中可独立执行初始化

### 主类修复

修复了 `Start-Class` 为 `com.lio9.pokedex.PokeDexApplication`（只扫描 pokedex + common 包）
导致 battle 和 user 的 Bean 未注册的问题。现在 `Start-Class` 为 `com.lio9.server.OneServerApplication`
（扫描 battle + pokedex + user + common 全部包）。
