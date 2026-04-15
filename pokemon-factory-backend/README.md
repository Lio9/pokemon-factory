# Pokemon Factory Backend

当前后端是一个 **Maven 多模块项目**，围绕本地 SQLite、共享基础设施和分模块业务能力组织。

## 当前模块

```text
pokemon-factory-backend/
├── common/         # 共享数据源、数据库初始化、公共配置与通用能力
├── pokeDex/        # 图鉴查询、导入、伤害计算等公开查询接口
├── user-module/    # 注册、登录、当前用户信息、JWT 相关能力
└── battleFactory/  # 对战工厂业务与受保护接口
```

## 模块职责

### common
- 统一 SQLite 数据源配置
- 提供数据库初始化入口
- 下沉公共配置与公共测试支撑

### pokeDex
- 图鉴查询接口
- 技能 / 道具 / 特性查询
- 伤害计算接口
- 导入任务相关接口

### user-module
- 用户注册、登录
- 当前用户信息查询
- JWT 认证基础能力

### battleFactory
- 对战工厂主流程
- 依赖 JWT 的受保护接口
- 当前测试覆盖最集中的业务模块

## 当前技术栈

- Spring Boot 4.0.5
- Java 17
- SQLite
- MyBatis / MyBatis-Plus
- JWT

## 当前推荐启动方式

### 1. 初始化数据库

在仓库根目录执行：

```bash
python scripts/init_db.py
```

### 2. 启动图鉴模块

```bash
cd pokemon-factory-backend/pokeDex
mvn spring-boot:run
```

默认端口：`8081`

### 3. 启动对战模块

```bash
cd pokemon-factory-backend/battleFactory
mvn spring-boot:run
```

默认端口：`8090`

## 说明

- 当前项目**不以 Docker 作为主开发方式**。
- 仓库中的 Docker 文件暂时仅保留为历史草案/备用参考。
- 后续优化重点放在：模块边界、配置统一、接口契约、测试与文档同步。