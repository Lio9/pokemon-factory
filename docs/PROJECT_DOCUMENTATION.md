# Pokemon Factory 宝可梦工厂

> **最后更新**: 2026-05-15
> **版本**: v2.0
> **状态**: 生产就绪
> **对战系统对齐度**: 与 Pokemon Showdown Gen9 Doubles 对齐度 99%

---

## 项目简介

Pokemon Factory 是一个基于 Spring Boot + Vue 3 + SQLite 的宝可梦工厂项目，支持：

- **宝可梦图鉴**：数据导入、查询、详情展示
- **用户系统**：注册、登录、会话恢复
- **对战工厂**：VGC 风格双打对战、AI 对战、段位系统

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.0.5 |
| 数据库 | SQLite (支持 MySQL/PostgreSQL 扩展) |
| 数据访问 | MyBatis / MyBatis-Plus |
| API | RESTful API + JWT 认证 |
| 前端框架 | Vue.js 3.x + Vite |
| UI | Tailwind CSS + Element Plus |
| 状态管理 | Pinia |
| PWA | Service Worker + IndexedDB |

---

## 项目结构

```
pokemon-factory/
├── backend/                          # 后端项目
│   ├── common/                       # 共享模块（数据源、工具类）
│   │   └── src/main/java/            # 数据库方言、CSV导入、通用工具
│   ├── pokedex/                      # 图鉴模块（端口 8081）
│   │   └── src/main/java/            # 宝可梦/技能/特性/物品查询
│   ├── battle/                       # 对战模块（端口 8090）
│   │   └── src/main/java/            # 战斗引擎、AI、场地说明
│   │       └── engine/               # 核心战斗逻辑
│   │       └── effect/               # 特性/道具效果处理
│   │       └── event/                # 战斗事件系统
│   └── user/                         # 用户模块
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── views/                    # 页面组件
│   │   ├── components/               # 通用组件
│   │   ├── composables/              # Vue 组合式函数
│   │   ├── services/                 # API 服务层
│   │   └── stores/                   # Pinia 状态管理
│   └── public/                       # 静态资源
├── scripts/                          # 工具脚本
│   ├── init_db.py                    # 数据库初始化
│   ├── backup_db.py                  # 数据库备份
│   └── start-backend.py              # 后端启动脚本
└── docs/                             # 文档目录
```

---

## 快速启动

### 1. 初始化数据库

```powershell
# 构建后端
cd backend\common
mvn -DskipTests package

# 运行初始化脚本
python scripts\init_db.py
```

### 2. 启动后端服务

```powershell
# 启动图鉴服务（端口 8081）
cd backend\pokedex
mvn spring-boot:run

# 启动对战服务（端口 8090）
cd backend\battle
mvn spring-boot:run
```

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

---

## API 接口

### 用户认证

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 注册并返回登录态 |
| `/api/user/login` | POST | 登录 |
| `/api/user/me` | GET | 当前用户信息 |

### 图鉴（poke-dex）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pokedex/pokemon/list` | GET | 宝可梦列表（分页+搜索） |
| `/api/pokedex/pokemon/{id}` | GET | 宝可梦详情 |
| `/api/pokedex/moves/list` | GET | 技能列表 |
| `/api/pokedex/abilities/list` | GET | 特性列表 |
| `/api/pokedex/items/list` | GET | 物品列表 |
| `/api/import-optimized/all-fast` | POST | 全量数据导入 |

### 对战（battle-factory）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/battle/start` | POST | 开始对战 |
| `/api/battle/{id}/action` | POST | 执行对战动作 |
| `/api/battle/{id}/state` | GET | 获取对战状态 |

---

## 核心模块说明

### 战斗引擎 (BattleEngine)

核心对战逻辑实现，位于 `backend/battle/src/main/java/com/lio9/battle/engine/`：

- `BattleEngine.java` - 主引擎，协调战斗流程
- `BattleFlowSupport.java` - 回合流程控制
- `BattleDamageSupport.java` - 伤害计算
- `BattleConditionSupport.java` - 状态条件处理
- `BattleFieldEffectSupport.java` - 场地效果（天气、地形）
- `BattleAISupport.java` - AI 决策支持
- `MoveRegistry.java` - 招式分类注册表

### 效果系统

特性/道具效果处理，位于 `backend/battle/src/main/java/com/lio9/battle/effect/`：

- `EffectRegistry.java` - 效果注册中心
- `AbilityHandler.java` - 特性效果处理
- `ItemHandler.java` - 道具效果处理
- `StatusContext.java` - 状态异常上下文
- `StatStageContext.java` - 能力等级上下文

### 事件系统

战斗事件驱动，位于 `backend/battle/src/main/java/com/lio9/battle/engine/event/`：

- `BattleEventBus.java` - 事件总线
- `BattleEvent.java` - 事件定义
- `BattleEventType.java` - 事件类型枚举

---

## 前端架构

### 目录结构

```
src/
├── views/                    # 页面级组件
│   ├── Home.vue             # 首页
│   ├── PokemonList.vue      # 宝可梦列表
│   ├── PokemonDetail.vue     # 宝可梦详情
│   ├── MoveList.vue          # 技能列表
│   ├── AbilityList.vue       # 特性列表
│   ├── ItemList.vue          # 物品列表
│   ├── Battle.vue            # 对战页面
│   └── DamageCalculator.vue  # 伤害计算器
├── components/              # 通用组件
│   ├── BattleArena.vue       # 对战场景
│   ├── BattleActionPanel.vue # 动作面板
│   └── ExchangeModal.vue     # 交换窗口
├── composables/             # 组合式函数
│   ├── usePokemonData.js    # 宝可梦数据获取
│   ├── useAuth.js           # 认证状态管理
│   └── useLocale.js         # 国际化
├── services/                # API 服务层
│   ├── api.js               # 统一导出
│   ├── httpClient.js        # HTTP 客户端
│   └── modules/             # 分模块 API
└── stores/                  # Pinia 状态
    ├── auth.js              # 认证状态
    ├── pokemon.js           # 宝可梦缓存
    └── locale.js            # 语言设置
```

### 设计系统

- **毛玻璃卡片**: `.glass-card` / `.glass-card-interactive`
- **属性标签**: `.type-badge` 覆盖 18 种宝可梦属性
- **动画效果**: 3D 倾斜、光效扫过、脉冲发光
- **快捷键**: `/` 搜索、`Escape` 清空

---

## 数据库

### 核心表

- `pokemon_species` - 宝可梦种族数据
- `pokemon_forms` - 形态变体
- `move` - 技能数据
- `ability` - 特性数据
- `item` - 道具数据
- `type` - 属性类型
- `type_efficacy` - 属性克制关系

### 备份

```powershell
# 手动备份
python scripts\backup_db.py

# 定时任务（需管理员权限）
.\scripts\setup_backup_task.ps1
```

---

## 开发指南

### 后端开发

```powershell
# 运行测试
cd pokemon-factory-backend
mvn test

# 运行回归测试
mvn -pl battle-factory -am -Dtest=BattleEngineRegressionBaselineTest test
```

### 前端开发

```powershell
cd pokemon-factory-frontend

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint
```

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v2.0 | 2026-05-14 | 全面优化：性能监控、键盘快捷键、PWA、界面美化 |
| v1.x | 2026-05 | 基础功能完成：图鉴、对战、用户系统 |

---

## 致谢

- [PokeAPI](https://pokeapi.co/) - 宝可梦数据来源
- [Pokemon Showdown](https://github.com/smogon/pokemon-showdown) - 战斗规则参考

---

## 许可证

MIT License
