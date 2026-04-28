# Pokemon Showdown 对齐路线图 (A -> B -> C)

## 1. 目标与边界

### 1.1 目标
- 主目标: 对战结果与 Pokemon Showdown 行为高度一致。
- 执行路径: 先完成 A，再扩展 B，最后融合为 C。
- 验收口径:
  - 结果一致率 (对照样例) >= 99%
  - 关键机制覆盖率 >= 99%
  - P0 回归缺陷 = 0

### 1.2 边界
- 当前对齐对象: Gen9 Doubles 规则集合。
- 实施方式: 行为对齐，不直接拷贝外部源码。
- 变更策略: 小步迭代 + 每阶段回归基线。

### 1.3 非目标
- 不在单个迭代内覆盖跨世代全格式。
- 不在无回归基线时进行大规模一次性重写。

---

## 2. 三阶段主计划

## A 阶段: 核心对战链路对齐 (先做)

### A.1 范围
- 回合主流程对齐: `BattleEngine.playRound`。
- 行动顺序对齐: 优先级、速度层、抢先/延后层、同速裁定。
- 伤害主链对齐: STAB、天气、场地、屏障、关键道具/特性修正顺序。
- 基础状态门控: 睡眠/麻痹/混乱/挑衅/封印类招式可用性判定。

### A.2 关键文件
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleEngine.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleRoundSupport.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleDamageSupport.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleConditionSupport.java`

### A.3 A 阶段 DoD
- 对照用例结果一致率 >= 95%。
- 关键机制 P0 问题清零。
- 回归基线测试可稳定复现。

---

## B 阶段: 扩展机制覆盖 (再做)

### B.1 范围
- 扩展特性/道具/招式交互覆盖到主流双打场景。
- 完整化入场触发与回合结束触发链。
- 补齐场地陷阱、屏障、天气/地形联动边界。
- 推进 `MoveRegistry` 与效果处理层的一致数据驱动配置。

### B.2 关键文件
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/MoveRegistry.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleTurnCleanupSupport.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleFieldEffectSupport.java`
- `pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleFlowSupport.java`

### B.3 B 阶段 DoD
- 机制覆盖率 >= 90%。
- 主流对局场景一致率 >= 97%。
- 新增机制均有回归用例。

---

## C 阶段: 融合与收敛 (最后做)

### C.1 范围
- 将 A/B 产物统一为稳定规则层。
- 消除跨模块重复逻辑与冲突判定。
- 建立长期对照测试与发布门禁。
- 面向后续扩格式能力预留统一扩展点。

### C.2 关键动作
- 统一判定优先级文档与实现。
- 建立差异分析报表 (失败样例 -> 原因归类 -> 修复闭环)。
- 固化发布阈值: 一致率、性能、回归数。

### C.3 C 阶段 DoD
- 对照样例结果一致率 >= 99%。
- 机制覆盖率 >= 99%。
- 连续两个迭代无 P0/P1 回归。

---

## 3. 测试与验收体系

### 3.1 基线测试
- 核心回归入口:
  - `pokemon-factory-backend/battle-factory/src/test/java/com/lio9/battle/engine/BattleEngineRegressionBaselineTest.java`
- 每次机制变更必须:
  - 增加或更新对应用例。
  - 记录与 Showdown 预期差异。

### 3.2 指标
- 一致率: 对照样例通过数 / 总样例数。
- 覆盖率: 已实现机制 / 计划机制。
- 稳定性: 回归失败数、重复失败比例。
- 性能: 关键回合推进耗时趋势。

### 3.3 发布门禁
- 未达到当前阶段 DoD，不进入下一阶段。
- 出现 P0 回归时，先修复后继续扩功能。

---

## 4. 执行节奏

### 4.1 周节奏
- 周初: 明确当周机制清单与样例。
- 周中: 规则实现 + 回归对齐。
- 周末: 更新进度文档、冻结基线。

### 4.2 里程碑
- M1: A 阶段完成并冻结。
- M2: B 阶段完成并冻结。
- M3: C 阶段验收通过。

---

## 5. 文档联动规则

- 本文档负责路线和阶段边界。
- 进度只在 `docs/battle_optimization_progress.md` 维护。
- 避免重复维护同一信息于多个优化文档。

---

## 6. 更新记录

- 2026-04-28: 按要求重构为 A -> B -> C 主计划，并精简为可执行版本。
