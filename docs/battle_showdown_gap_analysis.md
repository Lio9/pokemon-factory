# 对战逻辑与 Pokemon Showdown 差距分析报告

- **创建日期**: 2026-04-28
- **基准版本**: Pokemon Showdown Gen9 Doubles
- **当前一致率**: 核心回归基线 15/15 (100%)，扩展测试 196/219 (89.5%)
- **本文用途**: 详细记录与 Showdown 的行为差异、根因分析及优化优先级

---

## 1. 执行摘要

### 1.1 整体状况
- ✅ **核心链路**（顺序/伤害/基础状态）：已对齐，回归基线 100% 通过
- ⚠️ **扩展机制**（特性联动/道具/场地）：存在 11 个已知失败用例
- ❌ **应用层依赖**：1 个上下文加载错误（TeamBalanceEvaluator Bean 缺失）

### 1.2 关键指标
| 维度 | 目标 | 当前 | 差距 |
|------|------|------|------|
| 核心回归基线 | 100% | 100% (15/15) | ✅ 达标 |
| 扩展功能测试 | 100% | 89.5% (196/219) | ⚠️ 10.5% |
| P0 级逻辑错误 | 0 | 0 | ✅ 达标 |
| P1 级行为偏差 | 0 | 11 | ❌ 需修复 |

---

## 2. 已知差距清单（按优先级排序）

### 🔴 P0 - 阻断性问题（无）
> 当前无 P0 级问题，核心对战链路稳定。

---

### 🟡 P1 - 高优先级偏差（11 项）

#### P1-1: 威吓特性触发反抗特性异常
- **失败用例**: 
  - `BattleConditionSupportTest.applyIntimidate_triggersDefiantBoost`
  - `BattleEngineSwitchingTest.applyIntimidate_triggersDefiantBoost`
- **期望行为**: 威吓降低攻击时，拥有 Defiant 特性的宝可梦应提升 2 级攻击
- **实际行为**: 攻击等级变为 -1（仅被降低，未触发提升）
- **影响范围**: 所有涉及 Intimidate + Defiant 的双打对局
- **根因推测**: 
  - 威吓触发时机与 Defiant 检测逻辑未正确串联
  - 可能在 `BattleConditionSupport.applyIntimidate` 中缺少对对手 Defiant 特性的遍历检查
- **Showdown 参考**: 威吓作为入场效果，应在降低攻击后立即检查所有对手的 Defiant/Competitive 等特性
- **修复优先级**: 🔴 高（影响双打核心策略）
- **预计工作量**: 2-4 小时

#### P1-2: 引雷/储水特性未正确吸收招式并提升特攻
- **失败用例**:
  - `BattleEngineSwitchingTest.playRound_lightningRodAbsorbsElectricAndBoostsSpecialAttack`
  - `BattleEngineSwitchingTest.playRound_stormDrainAbsorbsWaterAndBoostsSpecialAttack`
- **期望行为**: 电系/水系单体招式被 Lightning Rod/Storm Drain 吸收，吸收者特攻提升 1 级
- **实际行为**: 特攻等级未提升（保持 0 级）
- **影响范围**: 所有涉及属性吸收特性的场景
- **根因推测**:
  - 重定向逻辑已实现（从日志看招式被重定向）
  - 但吸收后的 stat boost 未在 `BattleDamageSupport` 或 `BattleConditionSupport` 中触发
- **Showdown 参考**: 吸收特性在免疫伤害后应立即触发特攻提升
- **修复优先级**: 🔴 高（双打常见战术）
- **预计工作量**: 2-3 小时

#### P1-3: 引雷/储水重定向后伤害计算错误
- **失败用例**:
  - `BattleEngineSwitchingTest.playRound_lightningRodRedirectsSingleTargetElectricMove`
  - `BattleEngineSwitchingTest.playRound_stormDrainRedirectsSingleTargetWaterMove`
  - `BattleEngineSwitchingTest.playRound_powderImmuneAttackerFallsBackToStormDrainRedirection`
- **期望行为**: 招式重定向到吸收特性持有者后，应造成 0 伤害（完全免疫）
- **实际行为**: 
  - Lightning Rod 案例：目标 HP 从 220 降至 185（受到 35 点伤害）
  - Storm Drain 案例：目标 HP 从 220 降至 189（受到 31 点伤害）
  - Powder + Storm Drain 案例：目标 HP 从 220 降至 184（受到 36 点伤害）
- **影响范围**: 所有属性吸收重定向场景
- **根因推测**:
  - 重定向逻辑可能仅在目标选择阶段生效
  - 但在伤害计算阶段未正确应用"免疫"标志
  - 或者重定向后的目标未被标记为"具有吸收特性"
- **Showdown 参考**: 吸收特性不仅重定向，还完全免疫该属性伤害
- **修复优先级**: 🔴 高（严重影响平衡性）
- **预计工作量**: 3-5 小时

#### P1-4: 回复封锁未阻止被动回血
- **失败用例**: `BattleEngineSwitchingTest.playRound_healBlockStopsPassiveAndAbsorbHealing`
- **期望行为**: Heal Block 状态下，Leftovers 等被动回血应被阻止
- **实际行为**: Leftovers 仍然回血（HP 从 135 变化，预期保持不变）
- **影响范围**: 所有涉及 Heal Block 的持久战
- **根因推测**:
  - `BattleTurnCleanupSupport` 中的回合结束回血逻辑未检查 Heal Block volatile
  - 或者 Heal Block 的检测条件不完整
- **Showdown 参考**: Heal Block 阻止所有形式的 HP 恢复（除特定例外如 Ingrain）
- **修复优先级**: 🟡 中（影响特定战术）
- **预计工作量**: 1-2 小时

#### P1-5: 强制暴击招式未正确触发暴击
- **失败用例**: `BattleEngineSwitchingTest.playRound_moveMetadataCanForceCriticalHit`
- **期望行为**: 某些招式（如 Frost Breath、Storm Throw）应始终暴击
- **实际行为**: 未触发暴击
- **影响范围**: 所有强制暴击招式
- **根因推测**:
  - `BattleDamageSupport` 中的暴击判定逻辑可能未读取 move metadata 的 `alwaysCrit` 标志
  - 或者 MoveRegistry 中未正确配置这些招式的元数据
- **Showdown 参考**: 强制暴击招式忽略命中率和暴击率计算，直接判定为暴击
- **修复优先级**: 🟡 中（影响特定招式）
- **预计工作量**: 1-2 小时

#### P1-6: 讲究系列锁招 + 挑衅导致挣扎的逻辑异常
- **失败用例**: `BattleEngineSwitchingTest.playRound_choiceLockedStatusMoveUsesStruggleWhenTauntBlocksLockedMove`
- **期望行为**: 当讲究头巾锁定的招式被 Taunt 禁止时，应使用 Struggle
- **实际行为**: 日志显示 Thunder Wave 仍然尝试使用但失败，未切换到 Struggle
- **影响范围**: 讲究系列道具与 Taunt 的交互
- **根因推测**:
  - `BattleActionBuilder` 或 `BattleDecisionSupport` 中未正确处理"锁招被禁用"的边界情况
  - 可能在招式可用性检查后未回退到 Struggle
- **Showdown 参考**: 当唯一可用招式被禁止时，自动使用 Struggle
- **修复优先级**: 🟡 中（边缘场景）
- **预计工作量**: 2-3 小时

#### P1-7: 白草药恢复威吓下降并保留反抗增益
- **失败用例**: `BattleConditionSupportTest.applyIntimidate_whiteHerbRestoresDropAndPreservesDefiantBoost`
- **期望行为**: 白草药应在威吓降低攻击后恢复等级，同时 Defiant 的提升应保留
- **实际行为**: 攻击等级为 -1（既未恢复也未提升）
- **影响范围**: White Herb + Defiant 组合
- **根因推测**: 
  - 与 P1-1 相关，Defiant 未触发
  - White Herb 的触发时机可能在 Defiant 之前，导致逻辑冲突
- **Showdown 参考**: 入场效果顺序：先降低统计 → 触发 Defiant → White Herb 恢复下降的等级
- **修复优先级**: 🟡 中（依赖 P1-1 修复）
- **预计工作量**: 1-2 小时（在 P1-1 修复后）

---

### 🟢 P2 - 中低优先级偏差（待发现）
> 当前未识别出 P2 级问题，但随着测试覆盖扩大可能会出现。

---

### 🔵 P3 - 应用层问题（1 项）

#### P3-1: BattleFactoryApplicationTests 上下文加载失败
- **失败用例**: `BattleFactoryApplicationTests.contextLoads`
- **错误信息**: `No qualifying bean of type 'TeamBalanceEvaluator' available`
- **影响范围**: 集成测试环境
- **根因**: 
  - `AIService` 构造函数依赖 `TeamBalanceEvaluator`
  - 但该 Bean 未在测试配置中注册
- **修复方案**: 
  - 在测试配置中添加 `@MockBean TeamBalanceEvaluator`
  - 或者调整 `AIService` 的依赖为可选
- **修复优先级**: 🟢 低（不影响核心逻辑）
- **预计工作量**: 0.5 小时

---

## 3. 差距分类统计

| 类别 | 数量 | 占比 | 主要问题 |
|------|------|------|----------|
| 特性联动 | 4 | 36% | Intimidate/Defiant, Lightning Rod, Storm Drain |
| 状态门控 | 2 | 18% | Heal Block, Taunt + Choice Lock |
| 招式元数据 | 1 | 9% | Always Critical Hit |
| 道具交互 | 1 | 9% | White Herb 时序 |
| 应用配置 | 1 | 9% | Bean 依赖注入 |
| 伤害计算 | 2 | 18% | 吸收特性免疫逻辑 |

---

## 4. 根因深度分析

### 4.1 特性触发链断裂（P1-1, P1-2, P1-7）
**现象**: 入场特性（Intimidate）未能正确触发响应特性（Defiant）

**可能原因**:
1. **触发时机问题**: 
   - `applyIntimidate` 在哪个阶段调用？
   - Defiant 的检测是在同一阶段还是下一阶段？
   
2. **作用域问题**:
   - Intimidate 是否遍历了所有对手？
   - Defiant 检查是否考虑了"刚被降低统计"的状态？

3. **状态同步问题**:
   - stat changes 是否在特性触发前已持久化？
   - White Herb 的触发是否在 Defiant 之后？

**建议修复路径**:
```java
// 在 BattleConditionSupport.applyIntimidate 中
for (Pokemon opponent : getOpponents(attacker)) {
    // 1. 降低攻击等级
    applyStatChange(opponent, "atk", -1);
    
    // 2. 立即检查 Defiant/Competitive 等特性
    if (hasAbility(opponent, "defiant") && wasStatLowered(opponent)) {
        applyStatChange(opponent, "atk", +2);
    }
}

// 3. 最后处理 White Herb（在回合结束或下一个阶段）
```

### 4.2 吸收特性免疫逻辑缺失（P1-3）
**现象**: 招式被重定向但未免疫伤害

**可能原因**:
1. **重定向与免疫分离**:
   - `BattleTargetSupport` 处理重定向
   - 但 `BattleDamageSupport` 未检查目标是否具有吸收特性
   
2. **属性匹配问题**:
   - 重定向时记录了"这是电系招式"
   - 但伤害计算时未再次验证属性

**建议修复路径**:
```java
// 在 BattleDamageSupport.calculateDamage 中
if (isTypeAbsorbed(moveType, defender)) {
    return DamageResult.immune(); // 返回 0 伤害
}

private boolean isTypeAbsorbed(Type moveType, Pokemon defender) {
    if (moveType == ELECTRIC && hasAbility(defender, "lightning-rod")) {
        applyStatBoost(defender, "spa", +1); // 同时提升特攻
        return true;
    }
    if (moveType == WATER && hasAbility(defender, "storm-drain")) {
        applyStatBoost(defender, "spa", +1);
        return true;
    }
    return false;
}
```

### 4.3 回合结束回血未检查封锁状态（P1-4）
**现象**: Heal Block 未阻止 Leftovers 回血

**可能原因**:
- `BattleTurnCleanupSupport` 中的回血逻辑未查询 volatile 状态

**建议修复路径**:
```java
// 在 BattleTurnCleanupSupport.applyEndOfTurnHealing 中
if (hasVolatile(pokemon, "heal-block")) {
    log.debug("Heal Block prevents healing for {}", pokemon.getName());
    return; // 跳过回血
}
```

---

## 5. 优化路线图

### 阶段 B.1: 特性联动修复（预计 1-2 天）
**目标**: 解决 P1-1, P1-2, P1-3, P1-7

**任务清单**:
- [ ] 修复 Intimidate + Defiant 触发链
- [ ] 实现 Lightning Rod/Storm Drain 吸收并提升特攻
- [ ] 确保吸收特性完全免疫对应属性伤害
- [ ] 调整 White Herb 触发时序
- [ ] 新增 5-8 个回归测试用例覆盖这些场景

**验收标准**:
- 相关 7 个失败用例全部通过
- 新增用例 100% 通过
- 核心回归基线保持 100%

### 阶段 B.2: 状态门控完善（预计 0.5-1 天）
**目标**: 解决 P1-4, P1-6

**任务清单**:
- [ ] Heal Block 阻止所有形式的回血
- [ ] Choice Lock + Taunt 正确回退到 Struggle
- [ ] 新增 3-5 个边界场景测试

**验收标准**:
- 相关 2 个失败用例通过
- 不引入新的回归偏差

### 阶段 B.3: 招式元数据补全（预计 0.5 天）
**目标**: 解决 P1-5

**任务清单**:
- [ ] 在 MoveRegistry 中标记强制暴击招式
- [ ] 在伤害计算中读取 alwaysCrit 标志
- [ ] 新增 2-3 个强制暴击招式测试

**验收标准**:
- 相关 1 个失败用例通过

### 阶段 C.1: 应用层修复（预计 0.5 天）
**目标**: 解决 P3-1

**任务清单**:
- [ ] 修复测试环境的 Bean 依赖
- [ ] 确保所有集成测试可运行

---

## 6. 风险与缓解

### 6.1 主要风险
1. **修复引入新偏差**: 修改特性触发链可能影响其他已通过的用例
   - **缓解**: 每次修复后运行完整回归套件（219 个用例）
   
2. **Showdown 版本差异**: Showdown 可能在不同时期调整了某些机制
   - **缓解**: 记录测试时的 Showdown commit hash，定期同步更新

3. **双打复杂度**: 某些特性在单打和双打中行为不同
   - **缓解**: 明确标注每个测试是单打还是双打场景

### 6.2 质量门禁
- 任何修复不得降低核心回归基线的一致率（必须保持 100%）
- 新增功能必须配对至少 1 个回归测试用例
- P1 级问题修复后需在真实对局中验证（人工测试或 AI 对战）

---

## 7. 下一步行动（Next 5）

1. **立即**: 修复 P1-1（Intimidate + Defiant），这是多个失败用例的根因
2. **本周内**: 完成阶段 B.1（特性联动修复），解决 7 个高优先级问题
3. **下周**: 完成阶段 B.2 和 B.3（状态门控和招式元数据）
4. **持续**: 每周运行回归测试并更新本文档
5. **长期**: 建立自动化对照测试，直接从 Showdown 服务器拉取 replay 进行比对

---

## 8. 附录

### 8.1 测试环境信息
- **Java 版本**: 17
- **Spring Boot 版本**: 3.x
- **测试框架**: JUnit 5 + Mockito
- **回归基线用例数**: 15
- **扩展测试用例数**: 204（其中 196 通过，8 失败）

### 8.2 Showdown 参考资源
- 官方仓库: https://github.com/smogon/pokemon-showdown
- 规则文档: https://pokemonshowdown.com/rules
- 模拟器源码: `sim/` 目录下的 `battle.ts`, `pokemon.ts`, `moves.ts`

### 8.3 相关文档
- [战斗系统优化路线图](./battle_system_optimization_roadmap.md)
- [战斗系统优化进度](./battle_optimization_progress.md)

---

**文档维护**: 每次修复 P1/P2 问题后，需更新本文档的"已知差距清单"和"优化路线图"部分。
