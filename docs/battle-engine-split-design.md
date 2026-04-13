# BattleEngine 拆分设计稿

最后更新：2026-04-13
范围：battleFactory 结构性拆分设计，不调整数据库方案，不修改 battle summary 持久化格式

## 1. 设计目标

当前 `BattleEngine` 已经同时承担以下职责：

- 预览态和战斗态初始化
- 6 选 4 与首发选择
- 玩家与 AI 行动构建
- 回合执行与事件日志拼装
- 伤害、属性克制、道具、特性、状态效果结算
- 天气、场地、墙、顺风、戏法空间等场地效果维护
- 倒下补位、交换、派出触发能力
- 原始 `Map<String, Object>` 状态的 clone / cast / normalize

这一坨逻辑继续堆下去，后续做新特性、补测试、给 Android 迁移准备 API 合同时，成本都会持续上升。

这份设计的目标不是“一次性重写引擎”，而是给出一条低风险、可分步提交的拆分顺序：

- 先把职责边界切开
- 再把依赖和测试一起迁走
- 每一步都保持 battle summary 结构兼容

当前进展：

- `BattleStateSupport`、`BattlePreviewSupport`、`BattleFieldEffectSupport`、`BattleDecisionSupport` 已完成首批抽离
- `BattleDamageSupport` 已进一步接管伤害计算、速度计算、能力阶段修正与受伤结算
- `BattleConditionSupport` 已接管异常状态、入场特性、能力免疫与反应式效果
- `BattleAnalysisSupport` 已接管 AI 读状态判断（速度、物特倾向、属性/场地/睡眠判断）
- `BattleAISupport` 已接管 AI 特殊招式选择辅助（Fake Out、睡眠、场地、墙、挑衅、速度控制、天气、掩护与帮助）
- `BattleAiSwitchSupport` 已接管 AI 换人建议与抗性评估
- `BattleActionBuilder` 已接管玩家/AI action 构建与换人/目标槽位解析，`BattleDecisionSupport` 只保留选招策略
- `BattleTargetSupport` 已接管目标解析、指向与 Helping Hand / Redirection 的目标决议，并修正 Helping Hand 在无队友时不再错误回退给自己加成
- `BattleRoundSupport` 已接管 `playRound` 中单个 action 的执行逻辑，`BattleEngine` 只保留回合级编排
- `BattleFlowSupport` 已接管 replacement 阶段判定、对手自动补位、胜负判定与 derived state 刷新
- `BattleTurnCleanupSupport` 已接管回合末灼伤/剩饭/沙暴/青草场地/挑衅递减与 flinch 清理
- `BattleSetupSupport` 已接管 preview state 创建、预览确认开场、补位提交与交换替换后的队伍重建，`BattleEngine` 中这些入口已收口为 façade 转发
- `battleFactory` 模块链测试已在上述抽离后重新通过（当前 `mvn -pl battleFactory -am test` 为 93/93 通过）

## 2. 当前热点分区

按照当前 `BattleEngine` 的方法分布，主要可以切成 7 个热点：

### 2.1 状态装配与标准化

典型方法：

- `createPreviewState`
- `createBattleState`
- `applyTeamPreviewSelection`
- `parseTeam`
- `normalizeRoster`
- `normalizePokemon`
- `normalizeMoves`
- `cloneState`
- `castMap`
- `castList`

问题：

- 状态初始化和回合执行混在同一个类里
- `Map<String, Object>` 的 shape 约束没有集中管理

建议拆为：

- `BattleStateFactory`
- `BattleStateCloner`
- `BattleStateView` 或 `BattleStateAccessor`

### 2.2 队伍预览与补位流程

典型方法：

- `normalizeSelection`
- `buildBattleTeam`
- `initialActiveSlots`
- `applyReplacementSelection`
- `prepareReplacementPhase`
- `autoReplacementIndexes`
- `clearReplacementState`

问题：

- 预览选人和战斗内补位是两套流程，但共享大量底层状态操作
- 现在这些流程只能靠整类回归验证

建议拆为：

- `TeamPreviewService`
- `ReplacementService`

### 2.3 行动构建与 AI 决策

典型方法：

- `buildPlayerActions`
- `buildOpponentActions`
- `selectPlayerMove`
- `selectAIMove`
- `selectAIFakeOutMove`
- `selectAISleepMove`
- `selectAITerrainMove`
- `selectAIScreenMove`
- `selectAITauntMove`
- `selectAISpeedControlMove`
- `selectAIWeatherMove`
- `selectAIRedirectionMove`
- `selectAIHelpingHandMove`
- `chooseAISwitch`
- `findBestDefensiveSwitch`

问题：

- AI 策略判断和行动对象构建强耦合
- 任何 AI 小改动都要触碰核心引擎

建议拆为：

- `BattleActionBuilder`
- `BattleAiPolicy`
- `BattleAiSwitchAdvisor`

### 2.4 目标解析与回合执行

典型方法：

- `playRound`
- `resolveMoveTargets`
- `redirectedTargetIndex`
- `singleOpponentTargetRefs`
- `randomOpponentTargetRefs`
- `allOtherActiveTargetRefs`
- `allyTargetRefs`

问题：

- `playRound` 目前是最大热点，既做调度也做规则分发
- 目标解析逻辑和特殊招式分发都塞在主流程里

建议拆为：

- `RoundExecutor`
- `TargetResolver`
- `ActionResolutionContext`

### 2.5 伤害、状态与反应式效果

典型方法：

- `calculateDamage`
- `typeFactor`
- `typeModifier`
- `applyParalysis`
- `applyBurn`
- `applySleep`
- `applyTaunt`
- `applyDefenderAbilityImmunity`
- `applyReactiveContactEffects`
- `applyIncomingDamage`
- `applyDefenderItemEffects`
- `applyAttackerItemEffects`
- `triggerStatDropAbilities`

问题：

- 伤害和状态结算共享了大量分支判断，代码已明显失去局部性
- 新增一个能力或道具，经常需要同时改多个远距离代码块

建议拆为：

- `DamageResolutionService`
- `StatusEffectService`
- `AbilityResolutionService`
- `ItemResolutionService`

### 2.6 场地与回合末结算

典型方法：

- `activateTailwind`
- `toggleTrickRoom`
- `activateWeather`
- `activateTerrain`
- `activateScreen`
- `applyEndTurnFieldEffects`
- `applySandstormDamage`
- `applyGrassyTerrainHealing`
- `decrementTauntEffects`
- `applyEndTurnStatusEffects`
- `applyEndTurnHealing`
- `applyEntryAbilities`
- `applyIntimidate`
- `applySpeedDrop`

问题：

- 入场、场地、天气、墙、回合末收尾逻辑全部混在一起
- 很难单独验证某一类 field effect 的正确性

建议拆为：

- `FieldEffectService`
- `EntryEffectService`
- `EndTurnEffectService`

### 2.7 规则辅助与派生状态

典型方法：

- `refreshDerivedState`
- `resolveBattleResult`
- `speedValue`
- `modifiedAttackStat`
- `modifiedDefenseStat`
- `statStages`
- `applyStageModifier`
- 各类 `tailwindTurns` / `weatherTurns` / `terrainTurns`

问题：

- 引擎各处都在直接读取原始 map key，缺少统一入口
- 派生规则一旦变动，修改点分散

建议拆为：

- `BattleRuleSupport`
- `BattleDerivedStateService`

## 3. 推荐的目标结构

建议把引擎重构为“门面 + 子服务”的形式，而不是继续横向堆私有方法。

建议目录：

```text
battleFactory/src/main/java/com/lio9/battle/engine/
  BattleEngine.java                 // 保留门面，负责编排
  state/
    BattleStateFactory.java
    BattleStateCloner.java
    BattleStateAccessor.java
  selection/
    TeamPreviewService.java
    ReplacementService.java
  action/
    BattleActionBuilder.java
    ActionResolutionContext.java
    TargetResolver.java
  ai/
    BattleAiPolicy.java
    BattleAiSwitchAdvisor.java
  resolution/
    RoundExecutor.java
    DamageResolutionService.java
    StatusEffectService.java
    AbilityResolutionService.java
    ItemResolutionService.java
  field/
    FieldEffectService.java
    EntryEffectService.java
    EndTurnEffectService.java
  support/
    BattleRuleSupport.java
    BattleDerivedStateService.java
```

说明：

- `BattleEngine` 最终只暴露现有公共入口，避免 controller / service 层大面积改签名
- 第一阶段不强推把 `Map<String, Object>` 全改成强类型对象
- 先把“规则代码”从超大类里移出去，再考虑是否引入强类型 state wrapper

## 4. 建议拆分顺序

### 第一步：先抽状态访问和复制层

优先原因：

- 风险最低
- 能立刻减少 `BattleEngine` 里到处 cast / clone 的噪音

动作：

- 提取 `BattleStateCloner`
- 提取 `BattleStateAccessor`
- 把 `team`、`roster`、`rounds`、`activeSlots`、`fieldEffects` 这类访问器先迁出去

验收标准：

- `BattleEngine` 不再直接维护大部分 cast helper
- 现有测试不改行为只改依赖位置

### 第二步：拆队伍预览与补位流程

优先原因：

- 对外行为清晰，最适合先拆出完整子域
- 前端现在已经把 preview / replacement 状态分离，后端跟上后边界会更稳

动作：

- 提取 `TeamPreviewService`
- 提取 `ReplacementService`

验收标准：

- `applyTeamPreviewSelection` 和 `applyReplacementSelection` 只做参数转发
- preview / replacement 有独立测试类

### 第三步：拆 AI 和行动构建

优先原因：

- 这是目前回合逻辑里最容易继续膨胀的一层

动作：

- 已提取 `BattleAnalysisSupport`
- 已提取 `BattleAISupport`
- 已提取 `BattleAiSwitchSupport`
- 已提取 `BattleActionBuilder`
- 已提取 `BattleTargetSupport`
- 已提取 `BattleRoundSupport`
- 已提取 `BattleFlowSupport`
- 已提取 `BattleTurnCleanupSupport`
- 已提取 `BattleSetupSupport`
- 下一步如果继续拆，引擎里更值得评估的是 action 排序和 shared rule helper；开场初始化、预览确认、补位提交与交换替换路径已经基本收口

验收标准：

- AI 选择逻辑已经可以单独构造 state 做断言
- action 构建、目标解析、单 action 执行与回合末/残局编排已经脱离 `BattleEngine` 主类
- 预览确认、开场派出、补位提交与交换替换路径现在也只保留 façade 转发；后续是否继续拆分应更关注共享规则 helper 的收益/噪音比

### 第四步：拆目标解析和回合执行

优先原因：

- `playRound` 是当前主复杂度中心，必须最终被压缩

动作：

- 提取 `RoundExecutor`
- 提取 `TargetResolver`
- 把 `playRound` 缩成高层流程：准备上下文、执行动作、处理回合末、刷新摘要

验收标准：

- `playRound` 规模降到 200 行以内
- 目标解析相关逻辑不再散落在主类中

### 第五步：拆伤害 / 状态 / 道具 / 特性

优先原因：

- 这是新增规则时最容易回归的区域

动作：

- 提取 `DamageResolutionService`
- 提取 `StatusEffectService`
- 提取 `AbilityResolutionService`
- 提取 `ItemResolutionService`

验收标准：

- 新增一个能力或道具时，不再需要修改 `BattleEngine` 超大方法

### 第六步：拆场地和回合末效果

优先原因：

- 当前 weather / terrain / screen / end-turn 全挤在主引擎后半段

动作：

- 提取 `FieldEffectService`
- 提取 `EntryEffectService`
- 提取 `EndTurnEffectService`

验收标准：

- 天气、场地、入场触发和回合末触发具备独立测试入口

## 5. 测试跟进建议

拆分时不要等所有重构都结束再补测试，应该跟着子服务逐步搬运。

建议测试层次：

- 保留现有 `BattleEngine*Test` 作为回归保护网
- 为 `TeamPreviewService` 增加 selection / lead / replacement 专项测试
- 为 `BattleAiPolicy` 增加策略断言测试
- 为 `TargetResolver` 增加双打目标解析测试
- 为 `DamageResolutionService` 增加克制、天气、道具、特性联动测试
- 为 `FieldEffectService` 增加 weather / terrain / screen 生命周期测试

## 6. 这轮不做的事情

这份设计明确不包含以下改造：

- 不改数据库
- 不改 `summary_json` 的存储方式
- 不把 battleFactory 拆成独立服务群
- 不在这一轮强推全量强类型 BattleState

原因：

- 当前主要问题是类内复杂度，不是存储模型本身
- 先把职责边界切开，收益更直接，风险更低

## 7. 建议的首个提交范围

如果下一步直接开始落代码，建议首个 PR 只做下面三件事：

- 提取 `BattleStateCloner` + `BattleStateAccessor`
- 提取 `TeamPreviewService`
- 为 preview / replacement 补一组针对性测试

这样能先从最低风险的状态访问和预览流程开刀，同时不影响 battle summary、数据库、controller 和 service 层接口。
