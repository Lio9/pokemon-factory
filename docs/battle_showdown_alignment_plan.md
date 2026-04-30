# 对战引擎 Showdown 对齐计划

更新日期：2026-04-30

## 当前状态

### 测试指标

| 项目 | 结果 |
|------|------|
| 全项目测试 | 324/324 全通过 ✅ |
| 后端启动 | battle-factory 正常 (8090)、poke-dex 正常 (8081) |
| 数据库初始化 | 可正常工作（需设置环境变量） |
| API 路由 | 路由正确，数据库有数据后返回正常 |
| 前端构建 | Vite 构建成功 |

### 待处理问题

| # | 问题 | 状态 | 影响 |
|---|------|------|------|
| 1 | 数据库初始化需环境变量 | ⏳ | 首次启动需手动设置 |
| 2 | actuator/shutdown 未启用 | ⏳ | 开发调试不便 |
| 3 | 父 POM mainClass 错误 | ✅ 已修 | — |

### Showdown 对齐进度

**综合完成度：~80%**（与 Pokemon Showdown Gen 9 Doubles 对比）

| 模块 | 之前 | 现在 | 变化 |
|------|------|------|------|
| 核心对战链路 | 100% | 100% | — |
| Move Registry | 95% | 96% | +Rapid Spin/Defog |
| 场地/天气 | 90% | 90% | — |
| Entry Hazards | 95% | 98% | +Heavy-Duty Boots |
| 切换机制 | 85% | 85% | — |
| 通用状态 | 85% | 85% | — |
| 保护招式 | 80% | 80% | — |
| 特殊系统 | 70% | 70% | — |
| 特性 | 65% | 68% | +Sharpness/Guard Dog/Purifying Salt/Ruin |
| 道具 | 60% | 65% | +Boots/Air Balloon/Button |
| 挥发状态 | 40% | 65% | +7 种全链路实现 |
| 招式分类 | 50% | 50% | — |

## 推进路线

### Phase A — 挥发状态 + 基础完整性 ✅ 已完成

| 项目 | 文件 | 状态 |
|------|------|------|
| 寄生种子回合末吸血 | `BattleTurnCleanupSupport` | ✅ |
| 替身挡伤害/状态 | `BattleRoundSupport` | ✅ |
| 灭亡歌倒计时 | `BattleTurnCleanupSupport` | ✅ |
| 着迷 50% 封行动 | `BattleRoundSupport` | ✅ |
| 诅咒扣血、水流环/扎根回血 | `BattleTurnCleanupSupport` | ✅ |
| MoveRegistry 注册 | `MoveRegistry` | ✅ |

### Phase B — 关键道具补齐 ✅ 已完成

| 项目 | 文件 | 状态 |
|------|------|------|
| Heavy-Duty Boots 防钉 | `BattleConditionSupport.applyEntryHazards` | ✅ |
| Rocky Helmet 接触反伤 | `BattleConditionSupport` | ✅（此前已实现） |
| Air Balloon 地面免疫 + 破球 | `BattleConditionSupport` + `BattleEngine` | ✅ |
| Eject Button / Red Card 触发钩子 | `BattleEngine.applyDefenderItemEffects` | ✅ |

### Phase C — Gen8/9 特性补齐 ✅ 已完成

| 项目 | 文件 | 状态 |
|------|------|------|
| Sharpness 切割 1.5x | `BattleDamageSupport.abilityDamageModifier` | ✅ |
| Purifying Salt 状态免疫 + 幽灵抗性 | `BattleConditionSupport` + `BattleDamageSupport` | ✅ |
| Guard Dog 威吓免疫 + 攻击提升 | `BattleConditionSupport` | ✅ |
| Sword/Beads of Ruin 防御削弱 | `BattleDamageSupport.abilityDamageModifier` | ✅ |
| 进度文档更新 | — | ✅ |

---

### Phase E — 剩余差距补齐（~10 天）

| 项目 | 模块 | 文件 | 预估 | 优先级 |
|------|------|------|------|--------|
| **E.1** 招式分类重构 | 招式分类 50%→80% | `MoveRegistry` + `BattleDamageSupport` | 2d | 🔴 高 |
| ✅ 7 种分类从 name.contains 迁至 MoveRegistry Set，删除 80 行硬编码 | | | | |
| **E.2** 热门 VGC 特性补齐 | 特性 68%→78% | `BattleDamageSupport` + `BattleConditionSupport` | 3d | 🔴 高 |
| ✅ Supreme Overlord/Unseen Fist/Protosynthesis/Quark Drive 已实现 | | | | |
| **E.3** 道具换人逻辑完整 | 道具 65%→72% | `BattleFlowSupport` + `BattleRoundSupport` | 1d | 🟡 中 |
| ✅ Eject Button 触发换出 + Red Card 强制换入 | | | | |
| **E.4** 捕获/换人限制 | 切换 85%→95% | `BattleRoundSupport` | 1.5d | 🟡 中 |
| ✅ Arena Trap/Shadow Tag/Magnet Pull 阻止换人 | | | | |
| **E.5** 保护招式变种 | 保护 80%→90% | `MoveRegistry` + `BattleRoundSupport` | 1d | 🟢 低 |
| ✅ King's Shield/Obstruct/Silk Trap/Burning Bulwark 注册 | | | | |
| **E.6** 钉子清除逻辑 | Entry Hazards 98%→100% | `BattleFieldEffectSupport` + `BattleRoundSupport` | 1d | 🟢 低 |
| ✅ Rapid Spin/Defog 实际清除场地钉 | | | | |
| **E.7** 一键启动整合 | 基础设施 | `scripts/start-backend.py` + `docker-compose` | 0.5d | 🟢 低 |
| 数据库初始化 + 模块启动一步到位 | | | | |

**完成 E.1~E.6 = ~8 天 → ~93%**
**全部完成 E.1~E.7 = ~8.5 天 → ~95%**

### 交付标准

- 每个新特性匹配至少 1 个回归测试
- 全项目测试通过率 = 100%
- 不破坏现有 324 个测试用例
