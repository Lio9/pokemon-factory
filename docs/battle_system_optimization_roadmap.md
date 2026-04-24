# Pokemon Showdown 风格战斗系统优化路线图

## 📋 概述
本文档详细规划将当前战斗系统向 Pokemon Showdown 标准靠拢的优化路径，确保战斗机制的准确性和完整性。

---

## 🔍 当前系统差距分析

### 已实现的核心功能
- ✅ 双打对战系统 (2v2 Doubles)
- ✅ 基础伤害计算公式
- ✅ 属性克制关系
- ✅ 异常状态系统 (麻痹/灼伤/睡眠/中毒/冰冻)
- ✅ 场地效果 (顺风/戏法空间/精神场地等)
- ✅ 特殊系统 (Tera/Mega/Dynamax/Z-Move)
- ✅ 基础特性免疫
- ✅ 换人机制和队伍预览
- ✅ AI 对手逻辑

### 需要增强的关键领域

#### 1. **伤害计算精度** (优先级: 🔴 高)
**现状**: 基础公式已实现，但缺少许多修正因子

**Pokemon Showdown 标准**:
```
damage = ((((2 * Level / 5 + 2) * Power * A / D) / 50) + 2) * Modifier
Modifier = Target * Weather * Critical * Random * STAB * Type * Item * Ability * Burn * Screen * Terrain
```

**缺失项**:
- [ ] 多目标攻击的伤害衰减 (Spread Move: ×0.75)
- [ ] 更完整的道具加成列表 (所有属性增强道具)
- [ ] 特性对伤害的影响 (Technician, Sheer Force, Adaptability等)
- [ ] 太晶化类型适配度 (Tera STAB: ×1.5 或 ×2)
- [ ] 极巨化 HP 加成
- [ ] Z 招式威力提升规则
- [ ] 愤怒穴位 (Anger Point) 等临界触发

**优化建议**:
```java
// BattleDamageSupport.java 需要补充
double spreadMoveModifier(List<BattleEngine.TargetRef> targets) {
    return targets.size() > 1 ? 0.75 : 1.0;
}

double teraStabModifier(Map<String, Object> attacker, int moveTypeId) {
    if (!Boolean.TRUE.equals(attacker.get("terastallized"))) return 1.0;
    boolean matchesTeraType = engine.toInt(attacker.get("teraTypeId"), 0) == moveTypeId;
    boolean matchesOriginalType = hasOriginalType(attacker, moveTypeId);
    if (matchesTeraType && matchesOriginalType) return 2.0; // Tera STAB boost
    if (matchesTeraType) return 1.5;
    return 1.0;
}
```

---

#### 2. **特性系统完善** (优先级: 🔴 高)
**现状**: 仅实现部分免疫类特性

**Pokemon Showdown 标准**: 超过 200+ 特性，包含复杂互动

**缺失的关键特性类别**:

**A. 伤害修正类**:
- [ ] Technician (技术高手): 威力≤60的招式×1.5
- [ ] Sheer Force (强行): 有追加效果的招式×1.3，移除追加效果
- [ ] Adaptability (适应力): STAB从×1.5提升到×2
- [ ] Iron Fist (铁拳): 拳击类招式×1.2
- [ ] Reckless (舍身): 有反伤的招式×1.2
- [ ] Normalize (一般皮肤): 所有招式变为一般系×1.2

**B. 防御类**:
- [ ] Multiscale (多重鳞片): 满HP时受到的伤害减半
- [ ] Filter/Solid Rock (过滤/坚岩岩石): 效果绝佳招式伤害×0.75
- [ ] Fluffy (毛茸茸): 接触类招式伤害减半，火系×2
- [ ] Heatproof (耐热): 火系伤害减半，灼伤伤害减半

**C. 速度类**:
- [ ] Speed Boost (加速): 每回合速度+1
- [ ] Unburden (轻装): 失去道具后速度翻倍
- [ ] Swift Swim (悠游自如): 雨天速度翻倍
- [ ] Chlorophyll (叶绿素): 晴天速度翻倍
- [ ] Sand Rush (拨沙): 沙暴天气速度翻倍
- [ ] Slush Rush (拨雪): 雪天速度翻倍

**D. 天气/场地互动**:
- [ ] Drought (日照): 出场时召唤晴天
- [ ] Drizzle (降雨): 出场时召唤雨天
- [ ] Sand Stream (扬沙): 出场时召唤沙暴
- [ ] Snow Warning (降雪): 出场时召唤雪天
- [ ] Electric Surge (电气制造者): 出场时召唤电气场地
- [ ] Psychic Surge (精神制造者): 出场时召唤精神场地
- [ ] Misty Surge (薄雾制造者): 出场时召唤薄雾场地
- [ ] Grassy Surge (青草制造者): 出场时召唤青草场地

**E. 入场效果**:
- [ ] Intimidate (威吓): 出场时降低对手攻击1级
- [ ] Download (下载): 出场时提升较低防御对应的攻击或特攻
- [ ] Pressure (压迫感): 对手使用招式时额外消耗1PP
- [ ] Trace (追踪): 出场时复制对手特性

**F. 回合结束效果**:
- [ ] Poison Touch (毒手): 接触类招式30%概率使目标中毒
- [ ] Flame Body (火焰之躯): 接触类招式30%概率使目标灼伤
- [ ] Static (静电): 接触类招式30%概率使目标麻痹
- [ ] Cute Charm (迷人之躯): 接触类招式30%概率使异性目标着迷

**G. 特殊机制**:
- [ ] Wonder Guard (神奇守护): 只有效果绝佳的招式能命中
- [ ] Magic Guard (魔法防守): 不受间接伤害影响
- [ ] Regenerator (再生力): 下场时恢复1/3HP
- [ ] Imposter (变身者): 出场时自动变身为对手
- [ ] Comatose (绝对睡眠): 始终处于睡眠状态但不受影响

**优化建议**:
创建特性配置表，支持声明式定义:
```sql
CREATE TABLE ability_effects (
    id INTEGER PRIMARY KEY,
    ability_name_en TEXT NOT NULL,
    effect_type TEXT NOT NULL, -- 'damage_modifier', 'stat_change', 'weather', etc.
    trigger_condition TEXT, -- 'on_entry', 'on_hit', 'on_turn_end', etc.
    effect_data TEXT, -- JSON with effect parameters
    priority INTEGER DEFAULT 0
);
```

---

#### 3. **道具系统扩展** (优先级: 🟡 中)
**现状**: 基础道具已实现

**缺失的重要道具**:

**A. 选择类道具**:
- [x] Choice Band (讲究头带) - 已实现
- [x] Choice Specs (讲究眼镜) - 已实现
- [ ] Choice Scarf (讲究围巾) - 已实现但需验证
- [ ] Assault Vest (突击背心) - 已实现但需验证

**B. 生命类道具**:
- [x] Leftovers (吃剩的东西) - 已实现
- [ ] Black Sludge (黑色污泥): 毒系每回合恢复1/16，其他系损失1/8
- [ ] Shell Bell (贝壳之铃): 恢复造成伤害的1/8
- [ ] Big Root (大根茎): 吸取类招式恢复量×1.3

**C. 树果类**:
- [ ] Sitrus Berry (文柚果): HP低于1/2时恢复1/4
- [ ] Oran Berry (橙橙果): HP低于1/2时恢复10
- [ ] Lum Berry (木子果): 陷入异常状态时自动解除
- [ ] Focus Sash (气势披带) - 已实现
- [ ] Weakness Policy (弱点保险): 受到效果绝佳攻击时攻击和特攻+2
- [ ] Custap Berry (释陀果): HP低于1/4时下回合先制

**D. 属性增强道具** (需要完整列表):
- [ ] Mystic Water (神秘水滴) - 已实现
- [ ] Charcoal (木炭) - 已实现
- [ ] Miracle Seed (奇迹种子) - 已实现
- [ ] 补充所有18种属性的增强道具

**E. 进化石和专属道具**:
- [ ] 各种进化石的动态进化支持
- [ ] 专属道具效果 (如 Light Ball 让皮卡丘攻击翻倍)

---

#### 4. **招式系统增强** (优先级: 🔴 高)
**现状**: 基础招式框架已建立

**需要补充的招式类别**:

**A. 变化招式**:
- [ ] Swords Dance (剑舞): 攻击+2
- [ ] Nasty Plot (诡计): 特攻+2
- [ ] Dragon Dance (龙舞): 攻击和速度+1
- [ ] Calm Mind (冥想): 特攻和特防+1
- [ ] Stealth Rock (隐形岩): 设置场地陷阱
- [ ] Spikes (撒菱): 设置场地陷阱
- [ ] Toxic Spikes (毒菱): 设置场地陷阱
- [ ] Sticky Web (黏黏网): 设置场地陷阱

**B. 先制招式**:
- [ ] Quick Attack (电光一闪): priority +1
- [ ] Extreme Speed (神速): priority +2
- [ ] Fake Out (假动作): priority +3, 仅第一回合有效，必定畏缩
- [ ] Protect (守住) - 已实现
- [ ] Detect (看穿) - 需验证

**C. 连续攻击招式**:
- [ ] Fury Swipes (疯狂乱抓): 2-5次攻击
- [ ] Bullet Seed (种子机关枪): 2-5次攻击
- [ ] Rock Blast (岩石爆破): 2-5次攻击
- [x] Skill Multi - 已实现

**D. 自爆/牺牲招式**:
- [ ] Explosion (大爆炸): 超高威力，使用者濒死
- [ ] Self-Destruct (自爆): 高威力，使用者濒死
- [ ] Memento (临别礼物): 攻击和特攻-2，使用者濒死

**E. 天气招式**:
- [ ] Sunny Day (大晴天): 召唤晴天5回合
- [ ] Rain Dance (求雨): 召唤雨天5回合
- [ ] Sandstorm (沙暴): 召唤沙暴5回合
- [ ] Hail (冰雹)/Snowscape (雪景): 召唤雪天5回合

**F. 地形招式**:
- [ ] Electric Terrain (电气场地): 5回合，地面宝可梦不会睡眠，电系招式×1.3
- [ ] Psychic Terrain (精神场地): 5回合，地面宝可梦不受先制招式影响
- [ ] Grassy Terrain (青草场地): 5回合，地面宝可梦每回合恢复1/16HP，草系招式×1.3
- [ ] Misty Terrain (薄雾场地): 5回合，地面宝可梦不会陷入异常状态

**G. 高风险高回报招式**:
- [ ] Hyper Beam (破坏光线): 高威力，下回合无法行动
- [ ] Giga Impact (终极冲击): 高威力，下回合无法行动
- [ ] Close Combat (近身战): 高威力，使用后防御和特防-1
- [ ] Overheat (过热) - 已实现
- [ ] Leaf Storm (飞叶风暴): 高威力，使用后特攻-2

**H. 状态招式**:
- [ ] Will-O-Wisp (鬼火) - 已实现
- [ ] Thunder Wave (电磁波) - 已实现
- [ ] Toxic (剧毒): 施加剧毒状态
- [ ] Hypnosis (催眠术): 使目标睡眠
- [ ] Glare (瞪眼): 使目标麻痹
- [ ] Sing (唱歌): 使目标睡眠

**I. 回复招式**:
- [ ] Recover (自我再生): 恢复1/2HP
- [ ] Roost (羽栖): 恢复1/2HP，本回合失去飞行系
- [ ] Rest (睡觉): 恢复全部HP，睡眠2回合
- [ ] Soft-Boiled (生蛋): 恢复1/2HP

**J. 强制换人招式**:
- [ ] Whirlwind (吹飞): 强制对手随机换人
- [ ] Roar (吼叫): 强制对手随机换人
- [ ] Dragon Tail (龙尾): 强制对手随机换人并造成伤害
- [ ] Circle Throw (巴投): 强制对手随机换人并造成伤害

**K. 复制/变身招式**:
- [ ] Transform (变身): 变身为目标宝可梦
- [ ] Role Play (扮演): 复制目标特性
- [ ] Skill Swap (技能交换): 与目标交换特性

**L. 封印/限制招式**:
- [ ] Taunt (挑衅) - 已实现
- [ ] Torment (无理取闹): 目标不能连续使用同一招式
- [ ] Encore (再来一次): 目标必须连续使用上一回合的招式
- [ ] Disable (禁用): 禁用目标最近使用的招式
- [ ] Imprison (封印): 对手无法使用使用者也会的招式

---

#### 5. **战斗流程精确性** (优先级: 🔴 高)
**现状**: 基本流程正确，但细节需要调整

**Pokemon Showdown 标准回合流程**:
```
1. 回合开始阶段
   - 天气伤害/恢复 (Sandstorm/Hail damage, Rain/Sun healing)
   - 场地效果 (Grassy Terrain recovery)
   - 特性触发 (Speed Boost, Dry Skin, etc.)
   - 道具效果 (Leftovers, Black Sludge, etc.)
   - 持续状态伤害 (Poison, Burn, Toxic)

2. 行动前阶段
   - 特性入场效果 (Intimidate, Download, etc.)
   - 招式失败检测 (Taunt, Sleep, Paralysis, Freeze, Confusion)

3. 行动阶段 (按速度排序)
   - 先制等级比较
   - 速度值比较 (考虑Trick Room反转)
   - 同速度随机决定
   
4. 招式执行阶段
   - 准确性判定
   - 保护检测 (Protect, Wide Guard, Quick Guard)
   - 重定向检测 (Follow Me, Rage Powder)
   - 特性免疫检测
   - 伤害计算和应用
   - 附加效果触发 (Status conditions, Stat changes)
   - 道具消耗 (Berries, Air Balloon, etc.)

5. 回合结束阶段
   - 持续性效果清理
   - 场地/天气倒计时
   - 状态持续时间更新
   - Fainted Pokémon 检测
```

**需要优化的细节**:

**A. 行动顺序优先级**:
```java
// 当前可能缺少的优先级层次
int getActionPriority(Action action) {
    // 1. 先制等级 (priority)
    // 2. 是否被麻痹 (paralysis reduces speed)
    // 3. 场地影响 (Tailwind, Trick Room)
    // 4. 道具影响 (Quick Claw, Custap Berry)
    // 5. 特性影响 (Prankster gives +1 to status moves)
    // 6. 招式类型 (Protect/Detect have increasing failure rate)
}
```

**B. 保护成功率递减**:
```java
// Pokemon Showdown 标准
double getProtectionSuccessRate(int consecutiveUses) {
    // 第1次: 100%, 第2次: 50%, 第3次: 25%, 之后越来越低
    return 1.0 / Math.pow(2, consecutiveUses - 1);
}
```

**C. 命中率修正**:
```java
// 需要考虑的因素
- 命中等级变化 (accuracy stage)
- 闪避等级变化 (evasion stage) 
- 特性影响 (Compound Eyes, Victory Star boost accuracy)
- 道具影响 (Wide Lens, Zoom Lens)
- 天气影响 (Hurricane in rain is 100% accurate)
- 招式影响 (Lock-On ensures next move hits)
```

---

#### 6. **状态和条件系统** (优先级: 🟡 中)
**现状**: 基础异常状态已实现

**需要补充的状态**:

**A. 自愿状态 (Volatile Status)**:
- [ ] Confusion (混乱) - 已实现
- [ ] Infatuation (着迷): 异性宝可梦50%概率无法行动
- [ ] Leech Seed (寄生种子): 每回合吸取1/8HP给对手
- [ ] Curse (诅咒): 幽灵系使用时自身每回合损失1/4HP，目标每回合损失1/4HP
- [ ] Nightmare (噩梦): 睡眠的宝可梦每回合损失1/4HP
- [ ] Heal Block (愈合封锁): 5回合内无法恢复HP
- [ ] Embargo (征收): 5回合内无法使用道具
- [ ] Smack Down (击落): 使飞行系/漂浮特性变为可被地面系击中
- [ ] Aqua Ring (水流环): 每回合恢复1/16HP
- [ ] Ingrain (扎根): 每回合恢复1/16HP，但不能被换下
- [ ] Perish Song (灭亡之歌): 3回合后双方都濒死，除非换人

**B. 场地陷阱 (Hazards)**:
- [ ] Stealth Rock (隐形岩): 入场时根据4倍克制受到伤害
- [ ] Spikes (撒菱): 最多3层，入场时受到伤害
- [ ] Toxic Spikes (毒菱): 最多2层，1层中毒，2层剧毒
- [ ] Sticky Web (黏黏网): 入场时速度-1

**C. 屏幕效果**:
- [x] Reflect (反射壁) - 需验证
- [x] Light Screen (光墙) - 需验证
- [ ] Aurora Veil (极光幕): 冰雹/雪天时同时具有Reflect和Light Screen效果

---

#### 7. **前端用户体验** (优先级: 🟢 低)
**现状**: 基础UI已完成

**Pokemon Showdown 风格的改进**:

**A. 战斗日志增强**:
```javascript
// 更详细的战斗描述
- 显示伤害百分比: "造成了 45% 的伤害"
- 显示效果提示: "效果拔群!", "效果不好...", "没有效果..."
- 显示能力触发: "威吓发动了! 对手的攻击降低了!"
- 显示道具触发: "吃剩的东西恢复了HP!"
- 显示天气/场地变化: "阳光变得强烈了!"
```

**B. 实时数据展示**:
```vue
<!-- 添加HP条动画 -->
<div class="hp-bar">
  <div class="hp-fill" :style="{ width: hpPercent + '%', transition: 'width 0.5s' }"></div>
</div>

<!-- 添加能力/道具提示 -->
<div class="pokemon-info">
  <span v-if="pokemon.ability" class="ability-tag">{{ pokemon.ability }}</span>
  <span v-if="pokemon.item" class="item-tag">{{ pokemon.item }}</span>
</div>

<!-- 添加状态图标 -->
<div class="status-icons">
  <img v-if="pokemon.condition === 'burn'" src="/icons/burn.png" title="灼伤" />
  <img v-if="pokemon.condition === 'paralysis'" src="/icons/paralysis.png" title="麻痹" />
  <!-- ... -->
</div>
```

**C. 预测和建议**:
```javascript
// 添加招式效果预测
function predictDamage(move, target) {
  // 显示预估伤害范围
  // 显示击杀概率 (OHKO, 2HKO, 3HKO等)
  // 显示风险提示
}

// 添加队伍威胁分析
function analyzeThreats(opponentTeam) {
  // 显示对手的潜在威胁
  // 建议保留的宝可梦
  // 推荐的操作策略
}
```

**D. 回放和分享**:
```javascript
// 保存战斗回放
function saveReplay(battleLog) {
  // 生成可分享的replay代码
  // 支持重新观看战斗
}
```

---

## 📅 实施计划

### 第一阶段: 核心机制完善 (2-3周)
**目标**: 达到 Pokemon Showdown 80% 的战斗准确性

**任务清单**:
1. ✅ 完成伤害计算公式的所有修正因子
2. ✅ 实现至少30个常用特性
3. ✅ 补充所有属性增强道具
4. ✅ 完善招式数据库 (至少200个常用招式)
5. ✅ 修正战斗流程的优先级问题

**验收标准**:
- 伤害计算误差 < 5%
- 常见特性都能正确触发
- 战斗日志清晰准确

---

### 第二阶段: 高级特性实现 (2-3周)
**目标**: 支持复杂的特性互动和连锁反应

**任务清单**:
1. ✅ 实现特性优先级系统
2. ✅ 支持特性覆盖和替换 (Role Play, Skill Swap, etc.)
3. ✅ 实现场地陷阱系统
4. ✅ 完善所有天气和场地效果
5. ✅ 添加更多变化招式

**验收标准**:
- 特性互动正确处理
- 场地陷阱正确触发
- 天气/场地效果完整

---

### 第三阶段: 用户体验优化 (1-2周)
**目标**: 提供接近 Pokemon Showdown 的用户体验

**任务清单**:
1. ✅ 美化战斗日志
2. ✅ 添加HP条动画
3. ✅ 实现伤害预测
4. ✅ 添加状态图标
5. ✅ 支持战斗回放

**验收标准**:
- UI流畅美观
- 信息展示清晰
- 支持回放功能

---

### 第四阶段: 测试和优化 (持续)
**目标**: 确保系统稳定性和准确性

**任务清单**:
1. ✅ 编写单元测试 (覆盖所有特性/道具/招式)
2. ✅ 与 Pokemon Showdown 进行对比测试
3. ✅ 性能优化
4. ✅ Bug修复

**验收标准**:
- 测试覆盖率 > 90%
- 与 Pokemon Showdown 结果一致率 > 95%
- 响应时间 < 100ms

---

## 🧪 测试策略

### 单元测试示例
```java
@Test
void testTechnicianAbilityBoostsLowPowerMoves() {
    BattleEngine engine = createEngine();
    
    String playerTeam = "[" +
        pokemonJson("Technician-Mon", 200, 100, "", 60, "technician") +
        "]";
    String opponentTeam = "[" +
        pokemonJson("Target", 200, 100) +
        "]";
    
    Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 10, 12345L);
    setMoves(state, true, 0, List.of(
        move("Bullet Punch", "bullet-punch", 40, 100, 1, 1, DamageCalculatorUtil.TYPE_STEEL, 10)
    ));
    setMoves(state, false, 0, List.of(protectMove()));
    
    Map<String, Object> result = engine.playRound(state, Map.of(
        "slot-0", "bullet-punch",
        "target-slot-0", "0"
    ));
    
    // Technician should boost 40 power move by 1.5x
    // Expected damage should be higher than without the ability
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rounds = (List<Map<String, Object>>) result.get("rounds");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(0).get("actions");
    int damage = (Integer) actions.get(0).get("damage");
    
    assertTrue(damage > 20, "Technician should boost low-power moves");
}
```

### 集成测试
```java
@Test
void testFullBattleWithComplexInteractions() {
    // 测试包含多种特性、道具、天气的复杂对战
    // 与 Pokemon Showdown 的结果进行对比
}
```

---

## 📚 参考资料

### Pokemon Showdown 源代码
- GitHub: https://github.com/smogon/pokemon-showdown
- 关键文件:
  - `sim/battle.ts` - 战斗引擎核心
  - `data/moves.ts` - 招式数据
  - `data/abilities.ts` - 特性数据
  - `data/items.ts` - 道具数据
  - `calc.ts` - 伤害计算器

### Bulbapedia (宝可梦百科)
- 伤害计算公式: https://bulbapedia.bulbagarden.net/wiki/Damage
- 特性列表: https://bulbapedia.bulbagarden.net/wiki/Ability
- 招式列表: https://bulbapedia.bulbagarden.net/wiki/List_of_moves

### Serebii.net
- 游戏机制详解
- 最新世代变化

---

## 💡 实施建议

### 1. 渐进式开发
不要试图一次性实现所有功能。优先实现:
1. 最常用的特性 (Intimidate, Levitate, etc.)
2. 最常用的道具 (Leftovers, Choice items, etc.)
3. 最常用的招式 (Protect, U-turn, etc.)

### 2. 数据驱动
将特性、道具、招式的效果配置化，而不是硬编码:
```json
{
  "ability": "technician",
  "effect": {
    "type": "damage_multiplier",
    "condition": "move.power <= 60",
    "multiplier": 1.5
  }
}
```

### 3. 测试先行
为每个新特性/道具/招式编写测试用例，确保行为正确。

### 4. 性能考虑
- 缓存常用的计算结果
- 避免不必要的对象创建
- 使用更高效的数据结构

### 5. 文档维护
- 保持代码注释更新
- 记录特殊情况的处理逻辑
- 维护API文档

---

## 🎯 成功指标

### 技术指标
- [ ] 与 Pokemon Showdown 的战斗结果一致性 > 95%
- [ ] 单元测试覆盖率 > 90%
- [ ] API 响应时间 < 100ms
- [ ] 无内存泄漏

### 用户体验指标
- [ ] 战斗日志清晰易懂
- [ ] UI 响应流畅
- [ ] 支持主流浏览器
- [ ] 移动端友好

### 功能完整性指标
- [ ] 支持 Gen 8/9 的所有核心机制
- [ ] 实现至少 100 个常用特性
- [ ] 实现至少 300 个常用招式
- [ ] 实现所有重要道具

---

## 🔄 持续改进

### 定期审查
- 每月审查一次实现进度
- 对比 Pokemon Showdown 的更新
- 收集用户反馈

### 社区参与
- 关注 Pokemon Showdown 的更新
- 参与相关社区讨论
- 学习最新的对战环境变化

---

**最后更新**: 2026-04-23  
**版本**: 1.0  
**作者**: AI Assistant
