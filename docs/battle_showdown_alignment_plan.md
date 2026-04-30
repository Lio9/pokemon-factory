# 对战引擎 Showdown 对齐计划

更新日期：2026-04-30

## 当前状态

### 测试指标

| 项目 | 结果 |
|------|------|
| 单元测试 | 319/319 全通过 ✅ |
| 后端启动 | battle-factory 正常 (8090)、poke-dex 正常 (8081) |
| 数据库初始化 | 可正常工作（需 POKEMON_FACTORY_DATABASE_INITIALIZE_ON_STARTUP=true） |
| API 路由 | 路由正确，数据库有数据后返回正常 |
| 前端构建 | Vite 构建成功 |

### 已知问题

| # | 问题 | 状态 |
|---|------|------|
| 1 | 父 POM mainClass 错误 PokedexApplication -> PokeDexApplication | ✅ 已修复 |
| 2 | 数据库初始化需环境变量，一键启动流程未整合 | ⏳ 待处理 |
| 3 | actuator/shutdown 未启用，不方便开发调试 | ⏳ 待处理 |

### Showdown 对齐进度

**综合完成度：~72%**（与 Pokemon Showdown Gen 9 Doubles 对比）

### 模块完成度

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 核心对战链路 | 100% | 顺序/伤害/命中/基础状态门禁完全对齐 |
| Move Registry | 95% | 70+ 分类集合，体系完整 |
| 场地/天气 | 90% | 四种天气 + 四种场地齐全，缺特定招式交互 |
| 切换机制 | 85% | 入场钉 + 特性 + 换人全链，缺捕获 |
| 通用状态 | 85% | 五种主要异常全覆盖 |
| 保护招式 | 80% | Protect/Detect/Wide/Quick 全实现，缺变种 |
| 特殊系统 | 70% | Mega/Z/Dynamax/Tera 全实现，缺 G-Max 细节 |
| 特性 | 65% | ~60 个有逻辑，缺 Gen8/9 新特性 |
| 道具 | 60% | ~50 个有逻辑，缺 Boots/Helmet 等对战核心 |
| 招式分类 | 50% | 硬编码 name.contains 脆弱，需迁至 Registry |
| 挥发状态 | 40% | 替身/寄生种子/灭亡歌仅为枚举定义，无逻辑 |

## 推进路线

### Phase A — 挥发状态 + 基础完整性（~5 天）✅ 已完成

| 项目 | 状态 |
|------|------|
| 寄生种子回合末吸血 | ✅ |
| 替身挡伤害/状态 | ✅ |
| 灭亡歌/同命倒计时 | ✅ |
| 着迷/诅咒/祈愿/扎根 | ✅ |

### Phase B — 关键道具补齐（~2 天）✅ 已完成

| 项目 | 状态 |
|------|------|
| Heavy-Duty Boots 防钉 | ✅ |
| Rocky Helmet 接触反伤 | ✅（此前已实现） |
| Air Balloon 地面免疫 + 破球 | ✅ |
| Eject Button / Red Card | ✅ 触发逻辑 |

### Phase C — 特性补齐（~5 天）✅ 已完成

| 项目 | 状态 |
|------|------|
| Gen8/9 特性补充 (Sharpness/Purifying Salt/Guard Dog/Ruin abilities) | ✅ |
| 招式分类从硬编码迁至 MoveRegistry | ⏳ 部分完成 |

### Phase D — 边缘机制补完（~3 天）🔄 进行中

| 项目 | 状态 |
|------|------|
| 保护招式变种 (King's Shield/Obstruct/Silk Trap) | MoveRegistry 已注册 |
| 捕获/换人限制 (Mean Look/Arena Trap/Shadow Tag) | ⬜ |
| 钉子清除 (Rapid Spin/Defog) | MoveRegistry 已注册 |

**Phase A+B = ~7 天 → ~85%**
**A+B+C = ~12 天 → ~92%**
**A+B+C+D = ~15 天 → ~95%**

### 交付标准

- 每个机制配对至少 1 个回归测试
- 全项目测试通过率 >= 99%
- 新增 volatile 状态需在 `StandardVolatile` 注册 + 实现端到端逻辑
