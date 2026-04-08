# Pokemon Factory 宝可梦工厂

一个基于 **Spring Boot + Vue 3 + SQLite** 的宝可梦工厂项目，支持图鉴数据导入、图鉴查询、用户认证和对战工厂。

## 项目简介

Pokemon Factory 当前由以下几部分组成：
- **common**：共享数据库配置、SQL 初始化脚本、数据库初始化入口
- **pokeDex**：图鉴查询、导入相关业务接口
- **user-module**：用户注册、登录、当前用户信息
- **battleFactory**：对战工厂业务与鉴权链路
- **pokemon-factory-frontend**：Vue 3 前端界面
- **scripts**：数据库初始化、CSV 导入、校验等辅助脚本

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

### 🧱 统一基础设施
- 所有数据库相关配置统一下沉到 `common`
- 所有后端模块共享同一份 SQLite 数据库
- 必须先启动 `common` 完成数据库初始化，业务模块只保留业务逻辑

### 🎨 现代化界面
- 响应式设计，支持多设备访问
- 实时数据展示和搜索
- 交互式宝可梦详情查看

## 技术栈

### 后端技术
- **框架**：Spring Boot 4.0.5
- **数据库**：SQLite
- **数据访问**：MyBatis / MyBatis-Plus
- **API**：RESTful API
- **认证**：JWT

### 前端技术
- **框架**：Vue.js 3.x
- **构建工具**：Vite
- **路由**：Vue Router 4
- **UI框架**：Tailwind CSS
- **组件库**：Element Plus

### 数据导入
- **语言**：Python 3.8+
- **并发**：asyncio + aiohttp
- **数据库目标**：SQLite

## 环境要求

### 系统要求
- Windows/Linux/macOS
- Java 17+ (后端)
- Node.js 16+ (前端)
- Python 3.8+ (导入脚本)

### 网络要求
- 稳定的互联网连接
- 访问PokeAPI (https://pokeapi.co/)

## 后端模块与启动顺序

### 模块职责

| 模块 | 作用 |
|------|------|
| `common` | 统一数据源、数据库初始化脚本、数据库初始化入口 |
| `pokeDex` | 图鉴查询与导入业务 |
| `user-module` | 用户认证与当前用户信息 |
| `battleFactory` | 对战工厂业务、受保护接口 |

### 启动原则

1. **先启动 `common`**：负责初始化 SQLite 数据库结构。
2. **再启动业务模块**：`pokeDex` / `battleFactory` 复用 `common` 的共享数据库配置。
3. **前端最后启动**：通过 `VITE_API_BASE_URL` 指向后端接口。

说明：
- `common` 启动时会检查当前数据库是否已经存在业务表。
- 如果已有数据，`common` 会跳过带有冷启动语义的核心初始化脚本，避免覆盖现有库。
- `battleFactory` 默认端口为 **8090**，`pokeDex` 默认端口为 **8081**。

## 快速开始

### 1. 初始化数据库（推荐）

本项目当前由 `common` 统一负责数据库初始化，并提供一键脚本：

步骤：
1. 构建后端 JAR（若尚未构建）

```powershell
cd pokemon-factory-backend\common
mvn -DskipTests package
```

2. 运行初始化脚本（会：备份并删除旧 sqlite、启动 common、初始化数据库、导入 CSV、校验数据）

```bash
python scripts\init_db.py
```

可选环境变量：
- SQLITE_DB_PATH - 指定 sqlite 文件路径（默认：pokemon-factory-backend/pokemon-factory.db）
- JAR_PATH - 指定后端 jar 路径（默认 target 下的 common-0.0.1-SNAPSHOT.jar）
- MIGRATION_TIMEOUT - 等待迁移完成超时，单位秒（默认 120）

脚本运行完成后会自动调用 scripts/import_to_sqlite.py 进行数据导入，并运行 scripts/verify_sqlite.py 做基本校验。

### 2. 手动初始化（替代）

- 使用提供的 PowerShell 启动脚本初始化数据库：

```powershell
.\scripts\start-backend.ps1
```

- 或手动启动 common JAR：

```bash
java -jar pokemon-factory-backend\common\target\common-0.0.1-SNAPSHOT.jar
```

然后运行导入脚本：

```bash
python scripts\import_to_sqlite.py
python scripts\verify_sqlite.py
```

注意：仓库已将 sqlite 文件从主分支移除（不应将生成的数据库提交）。初始化脚本会在本地生成并备份旧文件。

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

开发时请设置 `VITE_API_BASE_URL` 指向后端接口地址。

## 数据初始化与导入脚本说明

### 脚本特性

- **智能重试**：最多重试8次，指数退避延迟
- **并发处理**：支持50个并发连接
- **批量导入**：每批200个宝可梦
- **错误处理**：详细的日志记录和错误恢复

### 使用方法

```bash
# 一键初始化数据库 + 导入 CSV + 校验
python scripts\init_db.py

# 只执行 CSV 导入（默认假定 common 已完成建库）
python scripts\import_to_sqlite.py

# 校验 SQLite 数据
python scripts\verify_sqlite.py
```

### 配置说明

关键环境变量：

| 变量名 | 说明 |
|------|------|
| `SQLITE_DB_PATH` | 指定 SQLite 文件路径 |
| `JAR_PATH` | 指定 `common` 启动 jar 路径 |
| `MIGRATION_TIMEOUT` | 等待 `common` 初始化完成的超时时间 |
| `JWT_SECRET` | JWT 签名密钥；未提供时会使用开发期临时密钥 |

## API接口

### 用户认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 注册并直接返回登录态 |
| `/api/user/login` | POST | 登录并返回 token 与用户资料 |
| `/api/user/me` | GET | 获取当前登录用户 |

### 图鉴接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pokemon` | GET | 获取宝可梦列表 |
| `/api/pokemon/{id}` | GET | 获取宝可梦详情 |
| `/api/types` | GET | 获取类型列表 |
| `/api/moves` | GET | 获取技能列表 |

### 对战接口

`/api/battle/**` 由 `battleFactory` 提供，需携带有效 JWT 访问受保护资源。

## 性能优化

### 数据库优化

1. **索引优化**：为常用查询字段添加索引
2. **批量操作**：使用批量插入减少数据库交互
3. **连接池**：合理配置数据库连接池大小

### 网络优化

1. **并发控制**：限制并发连接数避免服务器压力
2. **超时设置**：合理设置连接和读取超时
3. **重试策略**：指数退避重试提高成功率

### 缓存策略

1. **数据缓存**：缓存常用数据减少API调用
2. **内存管理**：及时清理不需要的缓存数据
3. **缓存失效**：设置合理的缓存过期时间

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 `common` 是否已经先启动
   - 检查 `SQLITE_DB_PATH` 是否指向正确位置
   - 检查 SQLite 文件所在目录是否有写权限

2. **导入失败**
   - 检查网络连接
   - 查看日志文件了解具体错误
   - 确认PokeAPI服务可用性

3. **前端无法访问接口**
   - 确认 `pokeDex` / `battleFactory` 已正常启动
   - 检查 `VITE_API_BASE_URL` 是否配置正确
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
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 致谢

- [PokeAPI](https://pokeapi.co/) - 宝可梦数据来源

## 联系方式

- 项目维护者：Lio9
- 邮箱：[Lio9@qq.com]
- GitHub：[https://github.com/Lio9/pokemon-factory](https://github.com/Lio9/pokemon-factory)

---

**注意**：本项目仅供学习和研究使用，请勿用于商业用途。
