# Pokemon Factory 对战工厂 — 优化与拓展计划

> 核心定位：**对战工厂（Battle Factory）肉鸽模式**
> - 9 轮连续对战，随机组队，胜后可交换宝可梦
> - 难度由段位决定（精灵球→超级球→高级球→大师球），不是手动选择
> - 不需要队伍编辑器、不需要回放系统
> - **需要中断回归**（刷新/关浏览器后能恢复对战）

---

## 一、中断回归（P0 最高优先级）

### 1.1 当前状态

| 场景 | 当前行为 | 问题 |
|------|----------|------|
| 手动对战 + 刷新页面 | sessionStorage 存了 battleId，`onMounted` 恢复 | ✅ 基本可用，但后端重启后 battleId 失效会卡死 |
| 工厂挑战 + 刷新页面 | `factoryRun.current_battle_id` 恢复 | ✅ 可用 |
| 手动对战 + 关闭浏览器 | sessionStorage 丢失（不是 localStorage） | ❌ 对战丢失 |
| 工厂挑战 + 关闭浏览器 | factory_run 表记录了 run，但当前 battle 的 summary 可能不完整 | ⚠️ 部分恢复 |
| 后端重启 | 所有内存中的 battle state 丢失，但 SQLite 有历史 rounds | ❌ 无法恢复进行中的对战 |

### 1.2 优化方案

**A. 手动对战：sessionStorage → localStorage**
- `sessionStorage` 在标签页关闭后丢失，改为 `localStorage`
- key 加上用户标识（游客用 guestId，登录用 username），避免多标签冲突
- 恢复时检查 battle 是否已 completed，如果已完成则清除

**B. 工厂挑战：battle state 落库**
- 当前 `battle` 表存了 `summary_json`，但只在回合结束时更新
- 改为**每次 submit move 后立即落库**最新的完整 state
- 恢复时从 `battle.summary_json` 重建 `summary`，跳过已执行的回合

**C. 后端重启恢复**
- 启动时扫描 `battle` 表中 `status != 'completed'` 的记录
- 对于手动对战：标记为 stale，前端收到 stale 标记后提示"对战已中断"
- 对于工厂挑战：从 `factory_run` 恢复 run 状态，当前 battle 标记为需要重打

**D. 前端中断提示**
- 恢复时如果 battle 已过期，显示友好提示："对战已中断，是否重新开始？"
- 工厂挑战中断时显示："工厂挑战已恢复到第 N 轮，当前战斗需要重新开始"

### 1.3 实现细节

```
前端 localStorage 结构：
  pokemon-factory-battle-{username} = battleId
  pokemon-factory-run-{username} = runId

后端 battle 表新增字段（可选）：
  last_sync_at DATETIME — 最后一次 state 同步时间
  stale_timeout_minutes INT DEFAULT 30 — 超过此时间视为 stale
```

---

## 二、对战体验优化（P1）

### 2.1 段位驱动的对手强度

当前问题：对手队伍用 `rank` 参数生成，但段位 0-3 的差异不明显。

优化：
- **精灵球段**：对手 BST 范围 400-500，无强化招式，AI 难度 Easy
- **超级球段**：对手 BST 范围 450-550，有基础辅助招式，AI 难度 Normal
- **高级球段**：对手 BST 范围 500-600，有强化+保护，AI 难度 Hard
- **大师球段**：对手 BST 范围 550-620，完整对战套路，AI 难度 Expert

实现：在 `AIService.generateFactoryTeamJson` 中根据 `rank` 调整 BST 范围和招式池。

### 2.2 胜后交换 UI 优化

当前问题：`ExchangeModal` 组件已存在但可能未被 Battle.vue 正确引用（死组件清理后）。

优化：
- 胜利后弹出交换面板，显示对手 6 只宝可梦的详细信息
- 点击替换己方某只宝可梦
- 显示交换前后的队伍对比
- 交换动画（被替换的精灵飞走，新精灵飞入）

### 2.3 工厂挑战结算面板

当前问题：`BattleSettlementModal` 组件未被引用。

优化：
- 9 轮结束后弹出结算面板
- 显示：胜/负数、积分变化、段位变化（晋级/降级动画）
- 显示本轮最佳表现宝可梦
- "再来一局"按钮

### 2.4 回合结束后的自动推进

当前问题：每回合需要手动提交，工厂模式体验不够流畅。

优化：
- 工厂模式下，AI 自动选择招式（基于当前队伍的 AI 策略）
- 玩家可以选择"自动战斗"模式，AI 代替操作
- 每回合显示 AI 的选择理由（可选）

---

## 三、肉鸽随机性增强（P1）

### 3.1 胜后交换的随机奖励

当前问题：胜后只能从对手队伍中选择替换。

优化（参考原版 Battle Factory）：
- 胜利后随机提供 3 只备选宝可梦（从对手队伍 + 随机池中选）
- 段位越高，备选宝可梦质量越高
- 可以选择不交换（保留当前队伍）

### 3.2 队伍健康度系统

当前问题：队伍中倒下的宝可梦在下一轮直接恢复。

优化（增加肉鸽紧张感）：
- HP 不在轮次间自动恢复（或只恢复 50%）
- 倒下的宝可梦需要消耗"复活币"才能重新上场
- 复活币通过连胜奖励获得
- 增加"治疗站"选项（牺牲一次交换机会来恢复队伍）

### 3.3 随机事件系统

每轮开始前可能出现随机事件：
- **道具雨**：本回合所有宝可梦获得随机道具
- **属性强化**：某个属性的招式威力 +20%
- **天气锁定**：本回合天气固定为某种
- **双倍积分**：本回合胜利积分翻倍

---

## 四、代码清理（P0）

### 4.1 删除 12 个死组件（共 3,280 行）

Battle.vue 重写为单文件后，以下组件不再被引用：

| 文件 | 行数 | 说明 |
|------|------|------|
| `BattleArena.vue` | 640 | 旧战场（已内联到 Battle.vue） |
| `BattleDecisionPanel.vue` | 579 | 旧决策面板（已内联） |
| `BattleActionPanel.vue` | 262 | 旧操作面板（已内联） |
| `BattleHeaderPanel.vue` | 195 | 旧头部（已删除） |
| `MoveButton.vue` | 166 | 旧招式按钮（已内联） |
| `PokeHoverCard.vue` | 442 | 旧悬停卡片（已内联） |
| `ExchangeModal.vue` | 160 | 需要恢复为内联或新组件 |
| `BattleLeaderboardModal.vue` | 74 | 需要恢复 |
| `BattleSettlementModal.vue` | 89 | 需要恢复 |
| `BanModal.vue` | 311 | 需要恢复 |
| `ErrorHandler.vue` | 495 | 全局未引用 |
| `PokemonStatsCard.vue` | 226 | 全局未引用 |

**操作**：确认无引用后删除，`npm run build` 验证。

### 4.2 恢复必要的弹窗组件

以下组件虽然被标记为"死组件"，但功能仍然需要：
- **ExchangeModal**：胜后交换（工厂核心功能）
- **BattleSettlementModal**：结算面板
- **BanModal**：Ban 系统
- **BattleLeaderboardModal**：排行榜

需要将这些组件的功能内联到 Battle.vue，或重新引入。

### 4.3 清理未使用的导出

`useBattlePageState` 导出 ~80 个字段，~30 个不再被使用。

---

## 五、段位系统增强（P2）

### 5.1 段位晋级动画

- 晋级时播放特效动画（精灵球→超级球→高级球→大师球）
- 段位图标在对战界面常驻显示
- 段位边框颜色区分（铜/银/金/紫）

### 5.2 段位保护机制

- 连续失败 3 次后触发"段位保护"，下次挑战不扣分
- 新手段位（精灵球）不会降级
- 每赛季重置段位（可选）

### 5.3 段位专属奖励

- 大师球段位解锁"Ban 3 只宝可梦"特权
- 高级球段位解锁"查看对手队伍"特权
- 超级球段位解锁"跳过一轮"特权

---

## 六、UI 细节优化（P2）

### 6.1 对战动画

- 攻击动画：精灵向前冲刺 + 受击抖动
- 状态变化动画：能力提升/下降闪烁
- 倒下动画：精灵旋转倒地
- 回复动画：绿色光晕

### 6.2 招式 Tooltip

- 悬停招式按钮显示：属性、威力、命中率、PP、附加效果
- 显示对当前目标的克制倍率（已在做，优化显示）

### 6.3 精灵详情面板优化

- 点击在场精灵显示：种族值雷达图、招式列表、特性描述、道具效果
- 对手精灵只显示已知信息（不显示具体招式，只显示已使用的）

### 6.4 回合日志增强

- 日志按阶段分组（换人 → 优先招式 → 普通招式 → 回合结束效果）
- 点击日志中的宝可梦名字弹出详情
- 伤害数值动画（数字飞出效果）

---

## 七、性能优化（P2）

### 7.1 SQLite 索引

```sql
CREATE INDEX idx_pokemon_species_evolves ON pokemon_species(evolves_from_species_id);
CREATE INDEX idx_battle_status ON battle(status, player_id);
CREATE INDEX idx_factory_run_active ON factory_run(player_id, status);
```

### 7.2 前端 bundle 优化

- 删除 12 个死组件后减少 ~50KB
- `useBattlePageState` 拆分为更小的模块
- 日志组件按需加载

### 7.3 后端查询优化

- `selectRandomDefaultForms` 的 NOT EXISTS 子查询在大数据量时可能慢
- 考虑添加 `is_fully_evolved` 标记列预计算

---

## 八、执行顺序

| 阶段 | 任务 | 预估时间 |
|------|------|----------|
| **Phase 1** | 中断回归（localStorage + stale 检测） | 1-2 天 |
| **Phase 2** | 代码清理（删死组件 + 恢复必要弹窗） | 1 天 |
| **Phase 3** | 段位驱动对手强度 + 交换 UI | 2-3 天 |
| **Phase 4** | 肉鸽随机性增强（HP 保留 + 随机事件） | 3-5 天 |
| **Phase 5** | UI 细节优化（动画 + tooltip + 日志） | 2-3 天 |
| **Phase 6** | 性能优化 + 测试 | 1-2 天 |

---

## 九、不做的事情（明确排除）

- ❌ 队伍编辑器（队伍是随机生成的，这是核心玩法）
- ❌ AI 难度选择（难度由段位自动决定）
- ❌ 回放系统（肉鸽模式不需要）
- ❌ 天梯排位（段位系统已经实现，不需要 ELO 匹配）
- ❌ 多语言完善（优先级太低）
- ❌ TypeScript 迁移（工作量太大，收益不明显）
- ❌ 音效系统（优先级太低）

---

*文档生成时间：2026-08-24*
*分析基于：81 个 Java 文件（27,090 行）+ 41 个 Vue 文件 + 27 个 JS 文件（16,804 行）*
*核心玩法：对战工厂肉鸽模式，9 轮连战，随机组队，胜后交换，段位晋级*
