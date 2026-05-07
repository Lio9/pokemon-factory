# Pokémon Showdown 战斗系统完整复刻计划

> **目标：100% 复刻 Pokémon Showdown 战斗系统**
> 更新日期：2026-05-07 | 版本：4.0

---

## 一、总览

### 1.1 当前状态

| 维度 | 已完成 | 缺失 | 完成度 |
|------|--------|------|--------|
| 核心对战链路 | 排序/伤害/命中/状态门禁 | — | 100% |
| 特性 (Abilities) | ~100 | ~200 | ~33% |
| 招式 (Moves) | ~220 (VGC 主流) | ~700 | ~24% |
| 道具 (Items) | ~80 | ~920 | ~8% |
| 状态系统 (Status) | 5 异常 + 12 volatile | ~15 交互缺失 | ~85% |
| 场地效果 (Field) | 天气/场地/墙/钉/空间/顺风/重力 | G-Max 持续伤害草稿 | 95% |
| 伤害公式 (Damage) | 接近 Showdown | 少量边缘情况 | 90% |
| 特殊系统 (Gimmick) | Mega/Z/Dyna/Tera/太晶化 | 部分 G-Max 招式效果 | 80% |
| 招式分类 (MoveRegistry) | 80+ Set 分类 | sound/bullet 混用标记 | 90% |
| 对战格式 (Formats) | 单打/双打/工厂 | 三打/轮盘/皇家 | 60% |
| AI 系统 | 基础 AI + 换人 AI | 竞技级决策 | 40% |
| 测试覆盖 | 435 项 | 大量场景缺失 | 30% |

### 1.2 目标架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     Battle Game State                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ Pokémon  │ │  Field   │ │  Battle  │ │   Action Queue   │  │
│  │  State   │ │ Effects  │ │  Config  │ │   (per-round)    │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                       Effect Engine                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ Ability  │ │   Item   │ │   Move   │ │   Status/Volatile│  │
│  │ Handler  │ │  Handler │ │  Effect  │ │     Handler      │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                     Move Resolution Pipeline                    │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────────┐  │
│  │Pre-Move│ │Target  │ │  Hit   │ │Damage  │ │ Post-Move  │  │
│  │Checks  │ │Select  │ │ Check  │ │ Calc   │ │  Effects   │  │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                       Event System                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │TryHit    │ │ModifyMove│ │BasePower │ │  AfterDamage    │  │
│  │  Event   │ │  Event   │ │  Event   │ │     Event       │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 Showdown 源码参考映射

| Showdown 文件 | 我们的对应 | 说明 |
|---------------|-----------|------|
| `sim/battle.ts` | `BattleEngine.java` | 主战斗循环 |
| `sim/battle-actions.ts` | `BattleRoundSupport.java` | 招式结算管道 |
| `sim/battle-dex.ts` | `PokedexService.java` | 数据查询 |
| `data/abilities.ts` | `EffectRegistry.java` + `AbilityHandler.java` | 特性系统 |
| `data/items.ts` | `EffectRegistry.java` + `ItemHandler.java` | 道具系统 |
| `data/moves.ts` | `MoveRegistry.java` + 引擎分散逻辑 | 招式效果 |
| `data/conditions.ts` | `BattleConditionSupport.java` | 状态系统 |
| `sim/battle-queue.ts` | 未实现 | 动作队列 |
| `sim/field.ts` | `BattleFieldEffectSupport.java` | 场地效果 |
| `sim/pokemon.ts` | `Map<String, Object>` (模型) | 宝可梦状态模型 |
| `sim/side.ts` | `BattleStateSupport.java` | 队伍/半场管理 |
| `sim/dex.ts` | `PokedexServiceImpl.java` | Pokédex 查询 |

---

## 二、实施阶段总览

```
Phase 0:  基础设施重构      (3 周)  ← 先做，影响所有后续工作
Phase 1:  特性系统          (4 周)  ← 最大的单一模块
Phase 2:  招式效果系统      (6 周)  ← 最复杂、最多样化的模块
Phase 3:  道具系统          (3 周)  ← 数量多但模式固定
Phase 4:  状态与挥发状态     (2 周)  ← 交互链条复杂
Phase 5:  伤害公式精确化     (1 周)  ← 已有基础，精细对齐
Phase 6:  场地效果补全       (1 周)  ← 已 95% 完成
Phase 7:  特殊系统完善       (2 周)  ← Mega/Z/Dyna/Tera/G-Max
Phase 8:  对战格式扩展       (2 周)  ← 三打/轮盘/皇家
Phase 9:  竞技 AI            (3 周)  ← Alpha-Beta + 蒙特卡洛
Phase 10: 测试基础设施       (贯穿)  ← 每阶段都需要
────────────────────────────────────
合计: ~27 周 (约 7 个月)
```

---

## 三、Phase 0：基础设施重构（3 周）

> **优先级：🔴 最高** — 后续所有工作都依赖此阶段

### 3.1 POJO 状态模型替换 Map

**当前问题**：整个战斗状态使用 `Map<String, Object>`，无类型安全、无编译时检查、大量 `@SuppressWarnings("unchecked")`、调试困难。

**目标**：创建强类型 POJO 状态模型。

```
新建包: com.lio9.battle.model

Pokemon.java          → 取代 Map<String, Object> (pokemon)
  - int currentHp, maxHp
  - Stats stats (hp, atk, def, spa, spd, spe)
  - List<Type> types
  - List<Move> moves
  - Ability ability
  - Item heldItem
  - StatStages statStages (-6..6 × 5)
  - Condition condition (enum: BURN/PARALYSIS/SLEEP/POISON/TOXIC/FREEZE/NONE)
  - Map<String, VolatileStatus> volatiles
  - boolean itemConsumed, flashFireBoost
  - boolean terastallized, megaEvolved, dynamaxed, zMoveUsed
  - Integer choiceLockedMove
  - String lastMoveUsed
  - int metronomeCount, protectionStreak, lastProtectionRound
  - 等等...

Move.java              → 取代 Map<String, Object> (move)
  - String name, nameEn
  - int power, accuracy, priority
  - int typeId, damageClassId
  - int targetId, pp, maxPp
  - int effectChance
  - Set<String> flags (contact, sound, bullet, punch, bite, etc.)
  - MoveEffect effect

FieldEffects.java      → 取代 Map<String, Object> (fieldEffects)
  - int rainTurns, sunTurns, sandTurns, snowTurns
  - int electricTerrainTurns, psychicTerrainTurns, grassyTerrainTurns, mistyTerrainTurns
  - int trickRoomTurns, gravityTurns, magicRoomTurns
  - SideField playerSide, opponentSide

SideField.java         → 取代 playerXxx/opponentXxx 字段
  - int tailwindTurns, reflectTurns, lightScreenTurns, auroraVeilTurns, safeguardTurns
  - boolean stealthRock
  - int spikesLayers (0-3), toxicSpikesLayers (0-2)
  - boolean stickyWeb
  - FutureSightData futureSight

BattleState.java       → 取代顶层 Map<String, Object>
  - BattleFormat format
  - BattlePhase phase
  - int currentRound, roundLimit
  - List<Pokemon> playerTeam, opponentTeam
  - List<Integer> playerActiveSlots, opponentActiveSlots
  - FieldEffects fieldEffects
  - String winner
  - List<RoundLog> rounds
```

**文件变更**：
- 新增：`model/` 包下 15+ POJO 类
- 修改：所有 engine 文件（将 `Map.get()` 替换为 getter）— 范围极大，涉及约 50 个文件
- 删除：`BattleStateSupport.java` 中的深拷贝逻辑（用 Bean Copy 替代）

**策略**：渐进式迁移，先建 POJO，再从外向内替换（先从新功能使用 POJO，旧代码逐步迁移）。

**风险**：改动范围太大。建议先保持 Map 接口兼容（POJO 实现 Map 接口），逐步替换调用点。

**预估工时**：15 天

### 3.2 事件系统完善

**当前问题**：已经有 `BattleEventBus` / `BattleEventType` / `DamageEvent` / `ModifyPowerEvent` / `TryHitEvent` 框架，但大部分引擎代码仍使用直接调用而非事件驱动。

**目标**：让引擎通过事件总线通信，实现真正的可插拔效果系统。

```
事件类型映射 (与 Showdown 对齐):

BeforeMoveEvent        → 出招前（Protean/Libero 在此触发）
TryHitEvent            → 命中判定前（保护/免疫检测）（已存在）
TryImmunityEvent       → 免疫检测
ModifyMoveEvent        → 招式修正（-ate 特性等）
ModifyPowerEvent       → 威力修正（已存在）
TryPrimaryHitEvent     → 主效果命中
TrySecondaryHitEvent   → 追加效果命中
DamageEvent            → 伤害计算（已存在）
AfterDamageEvent       → 伤害后触发
ModifyStatStageEvent   → 能力阶级变化
TrySwitchInEvent       → 换入尝试
SwitchInEvent          → 换入成功
ResidualEvent          → 回合末残留效果
FaintEvent             → 击倒事件
EndOfTurnEvent         → 回合末
```

**具体实现**：
1. 完善 `BattleEventBus` — 支持事件优先级排序、事件取消
2. 为每种事件类型创建 Event 子类
3. 修改 `BattleRoundSupport.processAction()` 在关键节点发布事件
4. 将 `EffectRegistry` 中的 handler 注册为事件监听器
5. 保持向后兼容：现有的直接调用仍工作，新特性通过事件注册

**文件变更**：
- 完善：`event/` 包下 10+ Event 类
- 修改：`BattleRoundSupport.java`（在管道节点发布事件）
- 修改：`EffectRegistry.java`（将 handler 注册为监听器）

**预估工时**：6 天

### 3.3 数据加载层完善

**当前问题**：招式/特性/道具从 CSV 加载但很多数据不完整，部分硬编码。

**目标**：
1. 确保 `pokedex` 数据库包含完整 Showdown 数据（~1000 宝可梦、~900 招式、~300 特性、~1000 道具）
2. 实现按需加载（通过 `PokedexService` 查询，而非全量预载）
3. 招式数据包含完整的 `flags`、`secondary`、`volatileStatus` 等 JSON 字段

**文件变更**：
- 修改：`scripts/init_db.py`（导入完整 Showdown 数据）
- 修改：`PokedexServiceImpl.java`（增加查询方法）
- 修改：`MoveRegistry.java`（使用数据库 flags 字段替代硬编码 Set）

**预估工时**：4 天

### Phase 0 总预估：25 天（~5 周）

---

## 四、Phase 1：特性系统（4 周）

> **优先级：🔴 最高** — 特性是战斗深度来源

### 4.1 特性分类清单

Showdown 有 ~298 个特性。按效果类别分：

#### 4.1.1 伤害修正类（~50 个）— 当前完成度 60%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~30 | Technician, Iron Fist, Reckless, Hustle, Strong Jaw, Mega Launcher, Sharpness, Punk Rock, Steely Spirit, Transistor, Dragon's Maw, Sand Force, Tinted Lens, Guts, Flare Boost, Toxic Boost, Sheer Force, Solar Power, Analytic, Supreme Overlord, Protosynthesis, Quark Drive, Orichalcum Pulse, Hadron Engine, Normalize, Thick Fat, Heatproof, Water Bubble, Dry Skin, Filter/Solid Rock/Prism Armor, Ice Scales, Fur Coat, Purifying Salt, Sword/Vessel/Tablets/Beads of Ruin |
| ❌ 缺失 | ~20 | Adaptability, Rivalry, Mold Breaker variants, Neuroforce, Sniper, Stakeout, Water Bubble (fire resist half), Gale Wings, Parental Bond, Fluffy, Battery, Power Spot, Steely Spirit (ally), Dark/Fairy Aura, Aura Break, Neuroforce, Sniper (crit calc), Tough Claws, Sheer Force (life orb negate), Stakeout, Water Compaction, Sand Force (full), Overgrow/Blaze/Torrent/Swarm |

**待实现**：
- **适应力** (Porygon-Z)：STAB 加成从 1.5x → 2.0x（需要修正伤害公式中的 STAB 计算）
- **硬爪**：接触招式 1.3x（需要标记招式是否有 `contact` flag）
- **亲子爱** (Kangaskhan-Mega)：第二次攻击 0.25x（需要修改多次攻击管道）
- **暗黑/妖精气场** (Yveltal/Xerneas)：场上所有 PM 对应属性增伤（需要光环系统）
- **气象锁** (Rayquaza)：无视天气（需要修正天气影响计算）
- **无天气** (Golduck)：无视天气效果（同上）
- **破格/涡轮高温/兆级电压/Teravolt/Turboblaze**：无视对方防御特性（需要特性穿透链）
- **狙击手**：会心一击 2.25x（需要在 damageFormula 添加修正）
- **猛火/茂盛/激流/虫之预感**：HP < 1/3 时对应属性 1.5x

#### 4.1.2 属性/类型交互类（~30 个）— 当前完成度 70%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~20 | Sap Sipper, Storm Drain, Lightning Rod, Volt Absorb, Water Absorb, Flash Fire, Motor Drive, Well-Baked Body, Earth Eater, Wind Rider, Levitate, Dry Skin, Thick Fat, Heatproof, Purifying Salt, Steely Spirit, Transistor, Dragon's Maw, Gale Wings (还原 Gen 7), -ate abilities (Pixilate, Refrigerate, Galvanize, Aerilate) |
| ❌ 缺失 | ~10 | Steelworker (ally Steel boost), Scrappy (Normal/Fighting hits Ghost — 仅 Mind's Eye 实现), Corrosion (Poison hits Steel, Poison doesn't block Toxic), Galvanize 等 -ate 系列 (1.2x 加成), Normalize (完整版), Liquid Voice, Dazzling/Queenly Majesty/Armor Tail (block priority) |

**待实现**：
- **腐蚀** (Salazzle)：毒系招式可中毒钢系，毒系状态免疫被绕过
- **胆量** (Kangaskhan)：一般/格斗系可打幽灵系
- **鲜艳之躯/女王威严/尾甲**：阻挡对手先制招式

#### 4.1.3 能力阶级类（~30 个）— 当前完成度 60%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~18 | Intimidate, Contrary, Defiant, Competitive, Guard Dog, Opportunist, Clear Body/White Smoke/Full Metal Body, Weak Armor, Stamina, Justified, Rattled, Steam Engine, Berserk, Anger Shell, Anger Point, Moxie, Beast Boost (部分) |
| ❌ 缺失 | ~12 | Simple, Moody (已实现), Speed Boost (已实现引擎但未注册), Download, Charm, Dauntless Shield, Intrepid Sword, Costar (已实现), Mirror Armor, Tangling Hair/Gooey (已实现接触), Unaware, Mycelium Might (已实现) |

**待实现**：
- **单纯** (Bibarel)：能力变化量翻倍（需要修改 `applyAbilityStageChange`）
- **纯朴** (Clefable/Quagsire)：无视对手能力变化（需要修改伤害公式中的 stat 读取）
- **异兽提升** (Ultra Beasts)：击倒后 +1 最高能力（需要计算各能力值并选最高）
- **镜甲** (Corviknight)：反弹能力阶级下降（类似 Magic Bounce 但针对 stat drops）
- **不服输/好胜** 完整版：对手每次能力下降都触发（不仅是威吓）
- **下载** (Porygon2)：根据对手防御/特防较低的选加攻/特攻
- **不挠之剑/不屈之盾** (Zacian/Zamazenta)：已实现

#### 4.1.4 状态/治愈类（~40 个）— 当前完成度 55%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~22 | Limber, Water Veil, Insomnia, Vital Spirit, Immunity, Own Tempo, Magma Armor, Oblivious, Aroma Veil, Shield Dust, Serene Grace, Healer, Hydration, Shed Skin, Natural Cure, Regenerator, Rain Dish, Ice Body, Shed Skin |
| ❌ 缺失 | ~18 | Comatose, Sweet Veil, Pastel Veil, Leaf Guard, Flower Veil, Aroma Veil (ally), Misty Surge, Electric Surge (block sleep), Grassy Surge (block), Psychic Surge (block priority), Cute Charm (已实现接触), Effect Spore (已实现), Poison Heal, Guts (已实现), Marvel Scale (已实现), Quick Feet (已实现), Flare Boost (已实现), Toxic Boost (已实现), Poison Touch |

**待实现**：
- **毒疗** (Breloom/Gliscor)：中毒/剧毒时每回合回复 1/8 HP（需要修改回合末处理）
- **自然回复** (Blissey/Chansey)：换下时治愈状态（需要修改切换逻辑）
- **蜕皮**：每回合 30% 治愈状态（需要回合末处理，已部分实现）
- **治愈之心**：每回合 30% 治愈队友状态（需要回合末处理）
- **清淋/湿润之躯**：雨中治愈状态
- **冰冻之躯/雨盘**：对应天气每回合回复

#### 4.1.5 上场触发类（~25 个）— 当前完成度 75%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~18 | Drizzle, Drought, Sand Stream, Snow Warning, Electric Surge, Psychic Surge, Grassy Surge, Misty Surge, Intimidate, Screen Cleaner, Supersweet Syrup, Hospitaly, Costar, Dauntless Shield, Intrepid Sword, Commander (已实现), Embody Aspect (已实现), Protosynthesis, Quark Drive |
| ❌ 缺失 | ~7 | Anticipation (无游戏影响，仅 UI), Forewarn (同上), Trace, Imposter, Download (已列), Frisk, Power of Alchemy/Receiver, As One (复合特性) |

**待实现**：
- **复制** (Gardevoir/Porygon2)：入场时复制对手特性（需要特性拷贝机制）
- **变身者** (Ditto)：入场时变成对手（需要完整变身系统）
- **察觉**：识别对手携带道具
- **危险预知/预知梦**：显示对手最高威力招式（仅 UI 提示）
- **化学之力/接球手**：队友倒下后继承其特性
- **力能融合** (Calyrex)：复合特性（Unnerve + Chilling Neigh）

#### 4.1.6 防御/生存类（~25 个）— 当前完成度 50%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~12 | Sturdy, Magic Guard (部分), Ice Face, Disguise, Multiscale, Shadow Shield, Battle Armor/Shell Armor (block crits, 未实现), Bulletproof, Soundproof, Overcoat, Wonder Guard, Magic Bounce |
| ❌ 缺失 | ~13 | Solid Rock (已注册), Filter (已注册), Prism Armor (已注册), Fur Coat (已注册), Fluffy (接触招式半减, 火系 2x), Friend Guard, Triage (回复招式先制 +3), Stakeout, Emergency Exit/Wimp Out, Innards Out, Perish Body, Cursed Body (已部分实现), Suction Cups |

**待实现**：
- **毛茸茸** (Bewear)：接触招式半减，火系 2x（需要标记接触判定和属性判定）
- **先行治疗** (Comfey)：回复招式先制度 +3（需要修改优先度排序）
- **友情防守** (Clefairy）：队友受到伤害 0.75x（需要在伤害公式中检查）
- **紧急逃生/懦弱** (Golisopod/Wimpod)：HP < 50% 后强制换人（需要修改血量检查后触发换人）
- **科学之力/战斗盔甲**：阻挡会心一击（需要修改会心判定）
- **穿透**：无视反射壁/光墙/极光幕/替身（需要修改屏障计算）

#### 4.1.7 速度控制类（~15 个）— 当前完成度 70%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~10 | Swift Swim, Chlorophyll, Sand Rush, Slush Rush, Surge Surfer, Unburden, Quick Feet, Chlorophyll 等天气速度翻倍 |
| ❌ 缺失 | ~5 | Gale Wings (完整版，仅满HP时飞行系先制+1), Prankster (恶系免疫，已部分实现), Stall, Mycelium Might (已实现), Quick Draw (30% 先行动) |

#### 4.1.8 其他特殊类（~30 个）— 当前完成度 30%

| 状态 | 数量 | 示例 |
|------|------|------|
| ✅ 已实现 | ~10 | Normalize, Protean/Libero, Mycelium Might, Moody, Unseen Fist, Mind's Eye, Scrappy |
| ❌ 缺失 | ~20 | Bad Dreams, Pickpocket, Magician, Cheek Pouch, Ripen, Neutralizing Gas, Stalwart/Propeller Tail, Perish Body, Wandering Spirit, Stench (flinch chance), Long Reach, Liquid Ooze, Magic Guard (完整), Klutz, Unnerve, Pressure, Ball Fetch, Sand Veil/Snow Cloak |

**待实现**：
- **紧张感/压迫感**：对手招式消耗 2 PP（需要完整 PP 系统）
- **化学变化气体** (Weezing-Galar)：场上所有特性失效（需要特性空气系统）
- **魔法防守** 完整版：免疫间接伤害（天气/状态/道具/生命宝珠反伤/钉子/寄生种子/诅咒等）
- **笨拙**：道具失效
- **偷盗/魔术师**：接触时偷取对方道具
- **贪吃鬼/壶壶**：树果提前触发（HP < 50% 而非 < 25%）
- **熟成** (Appletun)：树果效果翻倍
- **不眠** 完整版：噩梦/食梦阻止
- **恶梦** (Darkrai）：对睡眠目标造成每回合 1/8 伤害
- **沙隐/雪隐**：对应天气中闪避率提升

### 4.2 实现计划

**第 1 周**：伤害修正类补齐（Adaptability, Tough Claws, Sniper, Overgrow 系, Stakeout, Fluffy, Neuroforce）
**第 2 周**：能力阶级类补齐（Simple, Unaware, Beast Boost, Mirror Armor, Competitive/Defiant 完整版）
**第 3 周**：状态/治愈/防御类补齐（Poison Heal, Natural Cure, Triage, Emergency Exit, Perish Body）
**第 4 周**：特殊类 + 复合特性（Neutralizing Gas, Pressure, Magic Guard 完整, Magic Bounce 完整, Trace, Imposter）

### 4.3 各特性 Handler 注册

所有特性通过统一的 `EffectRegistry.registerAll()` 注册：

```java
// 新增特性只需添加一个匿名类
regAbility(new Ab() {
    public String id() { return "adaptability"; }
    public double onSourceModifyDamage(AttackContext ctx, double mod) {
        // STAB 2.0x 而不是 1.5x — 引擎在 STAB 计算时检查此标记
        ctx.attacker.put("adaptabilityActive", true);
        return mod;
    }
});
```

### Phase 1 总预估：20 天（~4 周）

---

## 五、Phase 2：招式效果系统（6 周）

> **优先级：🔴 最高** — 最复杂的模块

### 5.1 招式效果分类

Showdown 有 ~900 个招式。许多招式共享相同的效果模板。

#### 5.1.1 招式效果模板（~80 种效果模式）

```
A. 伤害 + 无附加效果 (Tackle, Pound, etc.)
B. 伤害 + 状态附加 (Thunderbolt → 10% paralysis)
C. 伤害 + 能力变化 (Psychic → 10% SpD -1, Play Rough → 10% Atk -1)
D. 伤害 + 回复 (Giga Drain → 50% 吸收)
E. 伤害 + 固定伤害 (Dragon Rage → 40 HP, Night Shade → level HP)
F. 伤害 + 体重相关 (Low Kick → 体重分段威力, Heavy Slam → 体重比对)
G. 伤害 + 速度相关 (Gyro Ball → 速度比对) ← 已实现
H. 伤害 + HP 相关 (Eruption → HP 比例威力, Reversal → HP 反比威力)
I. 多段攻击 (Bullet Seed → 2-5 hits, Beat Up → 队伍参与)
J. 自爆类 (Explosion → 附加 50% 防御无视)
K. 反伤类 (Flare Blitz → 1/3 反伤, Head Smash → 1/2 反伤)
L. 反制类 (Counter/Mirror Coat → 2x 反伤, Metal Burst → 1.5x 反伤)
M. 2 段蓄力类 (Fly/Dig/Bounce/Dive/Shadow Force → charge + launch)
N. 半无敌类 (Protect/Detect → block all, Endure → survive at 1 HP)
O. 场地效果类 (Rain Dance → weather, Light Screen → barrier)
P. 钉子类 (Stealth Rock/Spikes/Toxic Spikes/Sticky Web)
Q. 状态纯粹类 (Thunder Wave → paralysis, Will-O-Wisp → burn)
R. 能力变化类 (Swords Dance → +2 Atk, Dragon Dance → +1 Atk/Speed)
S. 切换类 (U-turn/Volt Switch → switch after damage, Baton Pass → pass boosts)
T. 交换类 (Trick/Switcheroo → swap items) ← 已实现
U. 抢夺类 (Thief/Knock Off → steal/remove item)
V. 陷阱类 (Mean Look/Block/Spider Web → prevent switch) ← 已实现
W. 灭亡之歌类 (Perish Song → 3 turn countdown) ← 已实现
X. 预知未来类 (Future Sight/Doom Desire → delayed damage) ← 已实现
Y. 接棒类 (Baton Pass → switch + pass boosts)
Z. 防守平分/力量平分 (split stats) ← 未实现
AA. 戏法空间/重力/魔法空间/奇妙空间 (room effects) ← 部分
AB. 混乱/着迷/再来一次/挑衅/定身法 (mental effects) ← 已实现
AC. 替身 (Substitute → create substitute) ← 已实现
AD. 诅咒 (Curse, ghost vs non-ghost) ← 已实现
AE. 同命 (Destiny Bond) ← 未实现
AF. 变身 (Transform) ← 未实现
AG. 忍耐/挺住 (Endure → survive at 1 HP)
AH. 抢先一步/仿效/模仿/抢夺 (Me First/Mimic/Copycat/Snatch)
AI. 镜面反射/金属爆炸/反击 (Counter/Mirror Coat/Metal Burst) ← 部分
AJ. 天气球/气合弹/觉醒力量 (variable type/power based on conditions)
AK. 礼物/自然之力/秘密之力 (environment-dependent)
```

#### 5.1.2 缺失的 VGC 常用招式效果（~50 个效果模式）

| 效果模式 | 示例招式 | 实现难度 | 优先度 |
|----------|---------|---------|--------|
| 伤害 + 能力变化 100% (次要效果) | Mystical Fire, Icy Wind, Electroweb, Snarl, Struggle Bug, Lunge, Fire Lash, Apple Acid, Grav Apple | 🟢 已部分 | P0 |
| 分担痛楚 | Pain Split | 🟡 | P0 |
| 吸收力量/汲取之吻 | Strength Sap, Draining Kiss | 🟡 | P0 |
| 同命 | Destiny Bond | 🟡 | P0 |
| 变身 | Transform | 🔴 | P1 |
| 忍耐 | Endure | 🟢 | P0 |
| 抢先一步 | Me First | 🟡 | P1 |
| 仿效 | Copycat | 🟡 | P1 |
| 防守平分/力量平分 | Guard Split/Power Split | 🟡 | P1 |
| 黑雾 | Haze (清除所有能力变化) | 🟢 | P0 |
| 清除之烟 | Clear Smog (伤害 + 清除对方能力变化) | 🟢 | P0 |
| 吸取 | Strength Sap (吸取对方攻击 + 回复) | 🟡 | P0 |
| 高速旋转 | Rapid Spin (伤害 + 清钉 + 提速) | 🟡 已部分 | P1 |
| 清除浓雾 | Defog (清钉 + 清场地 + 降闪避) | 🟢 已实现 | P1 |
| 击掌奇袭 + 再动标记 | Fake Out (先制+3, 仅出场回合, 畏缩) | 🟢 已部分 | P1 |
| 打草结/踢倒 | Grass Knot/Low Kick (体重分段威力) | 🟡 | P0 |
| 重磅冲撞/高温重压 | Heavy Slam/Heat Crash (体重比对威力) | 🟡 已实现 | P1 |
| 电光双击 | Electro Ball (速度比对威力) | 🟡 已实现 | P1 |
| 陀螺球 | Gyro Ball (速度比对威力) | 🟡 已实现 | P1 |
| 喷火/喷水/龙怒/逆鳞 | Eruption/Water Spout (HP 比例威力) | 🟢 | P0 |
| 起死回生/抓狂 | Reversal/Flail (HP < 25% 最大威力) | 🟢 | P0 |
| 以牙还牙 | Payback (被先攻击则 2x) | 🟢 | P0 |
| 报复 | Avalanche (被攻击则 2x) | 🟢 | P0 |
| 元气弹/能量球 | Focus Blast/Energy Ball (标准) | 🟢 已 | — |
| 气象球 | Weather Ball (天气中威力+属性变) | 🟡 | P1 |
| 地形球 | Terrain Pulse (场地中威力翻倍) | 🟡 | P1 |
| 觉醒力量 | Hidden Power (可变的属性/威力) | 🔴 | P2 |
| 报恩/迁怒 | Return/Frustration (亲密度相关) | 🔴 | P2 |
| 制裁光砾 | Judgement (携带石板时属性变化) | 🟡 | P2 |
| 破晓之光/月爆 | Photon Geyser/Moongeist Beam (无视特性) | 🟢 | P1 |
| 圣剑 | Sacred Sword (无视防御能力变化) | 🟢 | P1 |
| 灭歌 | Perish Song (3 回合灭亡) | 🟡 已实现 | — |
| 接棒 | Baton Pass | 🟡 | P1 |
| 防守互换/力量互换 | Guard Swap/Power Swap | 🟢 | P1 |
| 心之眼/锁定 | Mind Reader/Lock-On (下回必中) | 🟢 | P1 |
| 礼物/自然之恩 | Present/Natural Gift | 🔴 | P3 |
| 拼命/垂死挣扎 | Endeavor (目标 HP = 使用者 HP) | 🟢 | P1 |
| 最终手段 | Last Resort (仅能当其他招式用完时用) | 🔴 | P2 |
| 投球/螺旋球 | Gyro Ball/Electro Ball (已实现) | — | — |
| 大声咆哮/电网/冰风/泥巴射击 | Snarl/Electroweb/Icy Wind/Mud Shot | 🟢 已 | — |
| 岩崩/泥浆喷射/大地之力 | Rock Slide/Earth Power (标准追加效果) | 🟢 已 | — |
| 浸水/魔法粉/森林咒术 | Soak/Magic Powder/Forest's Curse | 🔴 | P2 |

#### 5.1.3 连续招式完整处理

**缺失**：
- 连续招式 PP 计算（2-5 次攻击消耗 1 PP 还是多次 PP？）
- 王者之证 + 连续招式的每段畏缩判定
- 贝壳之铃 + 连续招式的每段回复
- 生命宝珠 + 连续招式的每段反伤

#### 5.1.4 先制度完整处理

**缺失**：
- `Quick Claw` (先制之爪 — 20% 概率先行)
- `Quick Draw` (快速射击 — 30% 概率先行，Slowbro-Galar 特性)
- `Stall` (慢起步 — 总是最后出手)
- `Lagging Tail` / `Full Incense` (道具后手)
- 先制招式在戏法空间中的行为（确认与 Showdown 一致）

### 5.2 实现计划

**第 1 周**：P0 招式效果模式（能力变化 100%、分担痛楚、同命、忍耐、黑雾/清除之烟、体重招式补全、HP 比例招式、以牙还牙）

**第 2 周**：P0 招式效果模式续（圣剑/无视防守效果、防御平分/力量平分、防守互换/力量互换、高速旋转完善、拍落完善、起死回生/抓狂、吸收力量/汲取之吻）

**第 3 周**：P1 招式（击掌奇袭先制+再动、变身、接棒、气象球/地形球、破格招式）

**第 4 周**：P1 招式续（自然之力/秘密之力变体、最终手段、抢先一步/仿效/模仿/抢夺、制裁光砾/多变属性招式、投球、垂死挣扎）

**第 5 周**：多段攻击完整处理（PP 消耗、每段触发检查、王者之证每段畏缩）

**第 6 周**：先制度系统完善（重新排序、多个先制修正叠加、满腹 incense/后攻之尾处理）

### Phase 2 总预估：30 天（~6 周）

---

## 六、Phase 3：道具系统（3 周）

### 6.1 道具分类

Showdown 有 ~1000 个道具。按效果分类：

| 类别 | 总数 | 已实现 | 典型例子 |
|------|------|--------|----------|
| 属性增伤道具 | ~20 | ✅ 全部 | Miracle Seed, Charcoal 等 |
| 属性石板 | 18 | ✅ 全部 | Flame Plate, Splash Plate 等 |
| 属性记忆 | 18 | ✅ 全部 | Fire Memory 等 |
| 属性宝石 | 18 | ✅ 全部 | Fire Gem 等 |
| 能力修正道具 | ~15 | ✅ 10 | Choice Band/Specs/Scarf, Eviolite, Assault Vest, Light Ball, Thick Club, Deep Sea Tooth, Soul Dew, Metal Powder, Quick Powder |
| 速度修正道具 | ~10 | ✅ 5 | Iron Ball, Room Service, Choice Scarf, Quick Powder, Float Stone |
| 生存道具 | ~20 | ✅ 5 | Focus Sash, Focus Band, Sturdy (特性), Air Balloon, Heavy-Duty Boots |
| 回复道具 | ~15 | ✅ 3 | Leftovers, Black Sludge, Shell Bell |
| 树果 | ~60 | ✅ 5 | Sitrus, Oran, 各属性抗性果, 星桃果, Lansat 等 |
| 进化道具 | ~50 | — | 不在战斗中使用 |
| Mega/Z 石 | ~60 | ✅ 已框架 | Mega Stones, Z-Crystals |
| 专属道具 | ~40 | ✅ 5 | Light Ball, Thick Club, Soul Dew, Adamant/Lustrous/Griseous Orb |
| 一次性消耗 | ~30 | ✅ 8 | Focus Sash, Air Balloon, Weakness Policy, Eject Button, Blunder Policy, Adrenaline Orb, Room Service, Eject Pack |
| 变化/控制道具 | ~20 | ✅ 3 | Mental Herb, Power Herb, Shed Shell |
| 硬币/弹药 | — | — | 不适用于非 GO 对战 |

### 6.2 待实现道具（按对战重要性）

#### P0 — VGC 2024 必需 (~15 个)

| 道具 | 效果 | 实现难度 |
|------|------|----------|
| **Safety Goggles** (防尘护目镜) | 免疫天气伤害和粉末招式 | 🟢 |
| **Protective Pads** (保护垫) | 无接触效果触发 | 🟢 |
| **Clear Amulet** (洁净护符) | 能力不被下降 | 🟢 |
| **Covert Cloak** (密探斗篷) ✅ | 防追加效果 | 已实现 |
| **Mirror Herb** (模仿香草) | 对方能力提升时复制 | 🟡 |
| **Loaded Dice** (充电池骰子) ✅ | 多段攻击保底 4 次 | 已实现 |
| **Punching Glove** (拳击手套) ✅ | 拳类威力 1.1x，无接触 | 已实现 |
| **Utility Umbrella** (万能伞) ✅ | 无视天气 | 已实现 |
| **Booster Energy** (驱劲能量) ✅ | 悖论 PM 首回合触发 | 已实现 |
| **Throat Spray** (喉喷) ✅ | 声音招式后 SpA +1 | 已实现 |
| **Red Card** (逃脱按键) ✅ | 被攻击后强制对方换人 | 已实现 |
| **Weakness Policy** (弱点保险) ✅ | 被克制攻击后 +2 Atk/SpA | 已实现 |
| **Blunder Policy** (打空保险) ✅ | 招式 miss 后 Speed +2 | 已实现 |
| **Adrenaline Orb** (肾上腺素珠) ✅ | 被威吓后提速 | 已实现 |
| **Room Service** (客房服务) ✅ | 戏法空间中速度 -1 | 已实现 |

#### P1 — 对战重要 (~20 个)

| 道具 | 效果 | 实现难度 |
|------|------|----------|
| **Rocky Helmet** (凸凸头盔) ✅ | 接触反伤 1/6 | 已实现 |
| **Sitrus Berry** (文柚果) ✅ | HP<50% 回复 1/4 | 已实现 |
| **Resistance Berries** (各种抗性果) | 受对应属性攻击时减半 | 🟢 |
| **Mental Herb** (心灵香草) ✅ | 解 taunt/encore/disable 等 | 已实现 |
| **Power Herb** (强力香草) | 跳过蓄力回合 | 🟢 |
| **White Herb** (白色香草) ✅ | 能力下降时立即回复 | 已实现 |
| **Lum Berry** (万能果) | 任何异常状态时治愈 | 🟢 |
| **Chesto Berry** (涩栗) | 睡眠时治愈 | 🟢 |
| **Rawst Berry** (蓝莓) | 灼伤时治愈 | 🟢 |
| **Aspear Berry** (酸梨) | 冰冻时治愈 | 🟢 |
| **Cheri Berry** (辣樱) | 麻痹时治愈 | 🟢 |
| **Pecha Berry** (桃桃果) | 中毒时治愈 | 🟢 |
| **Persim Berry** (绿姆果) | 混乱时治愈 | 🟢 |
| **Apicot/Ganlon/Liechi/Petaya/Salac Berry** | HP<25% 对应能力 +1 | 🟢 |
| **Starf Berry** (星桃果) ✅ | HP<25% 随机能力 +2 | 已实现 |
| **Lansat Berry** (兰萨果) ✅ | HP<25% 会心率 +2 | 已实现 |
| **Custap Berry** (卡托果) | HP<25% 下回先制 | 🟡 |
| **Micle Berry** (米库果) | HP<25% 下回命中 +1 | 🟢 |
| **Jaboca/Rowap Berry** (嘉宝/罗子果) | 被物理/特殊招式攻击后对方反伤 | 🟡 |
| **Enigma Berry** (谜芝果) | 被克制招式攻击后回复 1/4 | 🟢 |

#### P2 — 大量低使用率道具（~100 个）

```
Absorb Bulb ✅, Cell Battery ✅, Luminous Moss ✅, Snowball ✅
Air Balloon ✅, Binding Band ✅, Bright Powder, Choice Specs ✅
Damp Rock, Heat Rock, Icy Rock, Smooth Rock ✅
Destiny Knot, Float Stone ✅, Full Incense, Grip Claw
King's Rock, Lagging Tail, Lax Incense, Leftovers ✅
Light Clay ✅, Lucky Punch, Magnet, Metal Coat ✅
Muscle Band ✅, Never-Melt Ice ✅, Odd Incense, Razor Claw
Razor Fang, Reaper Cloth, Ring Target, Rocky Helmet ✅
Scope Lens, Sharp Beak ✅, Shed Shell ✅, Silk Scarf ✅
Silver Powder ✅, Smoke Ball, Soft Sand ✅, Soothe Bell
Soul Dew ✅, Spell Tag ✅, Sticky Barb, Terrain Extender ✅
Thick Club ✅, Twisted Spoon ✅, Wave Incense, Wide Lens
Wise Glasses ✅, Zoom Lens
```

### 6.3 实现计划

**第 1 周**：P0 道具补齐（防护目镜、保护垫、洁净护符、模仿香草、强力香草）
**第 2 周**：P1 属性果、状态果补齐（~15 个树果）
**第 3 周**：P2 低使用率道具补齐 + 完整道具消耗逻辑

### Phase 3 总预估：15 天（~3 周）

---

## 七、Phase 4：状态与挥发状态系统（2 周）

### 7.1 当前状态

✅ **已实现的状态**：
- 5 种异常状态：sleep, paralysis, burn, poison, toxic, freeze
- 10+ volatile 状态：confusion, taunt, encore, disable, torment, heal block, leech seed, substitute, perish song, curse, aqua ring, ingrain, infatuation, bound

❌ **缺失的状态交互**：

| 交互 | Showdown 行为 | 说明 |
|------|--------------|------|
| 灼伤降物攻 | 物理招式伤害减半 | ✅ 已实现 |
| 麻痹降速度 | 速度 1/2 | ✅ 已实现 |
| 冰冻破冰 | 被火系招式命中解冻 | ✅ 已实现 |
| 睡眠回合制 | 1-3 回合随机 | ✅ 已实现 |
| 剧毒递增 | 每回合递增 dmg = (1/16) * N | ✅ 已实现 |
| 睡眠讲话 | Sleep Talk 随机使用一个招式 | ❌ 未实现 |
| 打鼾 | Snore 只能在睡眠时用 | ❌ 未实现 |
| 噩梦 | Nightmare — 燃烧睡眠方 1/4 HP | ❌ 未实现 |
| 食梦 | Dream Eater — 只能在睡眠时用 | ❌ 未实现 |
| 烦恼种子 | Worry Seed — 改变对方特性为不眠 | ❌ 未实现 |
| 胃液 | Gastro Acid — 消除对方特性 | ❌ 未实现 |
| 心灵交换 | Heart Swap — 交换能力变化 | ❌ 未实现 |
| 精神转移 | Psycho Shift — 传递状态给目标 | ❌ 未实现 |
| 治愈铃声/芳香治疗 | 治愈全队状态 | ❌ 未实现 |
| 净化之水 | Refresh — 治愈自身状态 | ❌ 未实现 |

### 7.2 挥发状态补全

❌ **缺失的挥发状态**：

| 挥发状态 | 效果 | Showdown 处理 |
|---------|------|---------------|
| **Miracle Eye** (奇迹之眼) | 超能系可打恶系，重置闪避 | 回合限定 volatile |
| **Foresight** (识破) | 一般/格斗可打幽灵，重置闪避 | 同上 |
| **Smack Down** (击落) | 飞行系/漂浮落地，对地系弱化 | volatile 标记 |
| **Telekinesis** (心灵传动) | 浮在空中，3 回合内必中 | volatile |
| **Throat Chop** (地狱突刺) | 2 回合不能使用声音招式 | volatile |
| **Tar Shot** (沥青射击) | 火系弱点加倍 | volatile |
| **Octolock** (八爪锁) | 每回合下降防御和特防 | volatile |
| **Jaw Lock** (颌锁) | 双方都不能换人 | volatile |
| **Fairy Lock** (妖精锁) | 下回合不能换人 | volatile |
| **No Retreat** (背水一战) | 全能力 +1 但不能换人 | volatile |
| **G-Max 效果** | Wildfire/Cannonade/Vine Lash | ✅ 已实现框架 |

### 7.3 状态交互链

完善以下交互链条：

```
Move → Accuracy Check → Secondary Effect → Status Application →
→ Status Immunity Check (Ability, Type, Field) → Status Applied →
→ Item Trigger (Lum Berry) → Status Duration Set

Turn End:
→ Status Damage/Effect → Item Recovery → Status Duration Decay
→ Natural Cure on Switch → Hydration in Rain → Shed Skin chance
```

### 7.4 实现计划

**第 1 周**：挥发状态补全（Miracle Eye, Foresight, Smack Down, Telekinesis, Throat Chop, Tar Shot, Octolock, Jaw Lock, Fairy Lock, No Retreat）+ 状态招式补全（Sleep Talk, Snore, Nightmare, Dream Eater, Worry Seed, Gastro Acid, Heart Swap, Psycho Shift, Heal Bell, Aromatherapy, Refresh）

**第 2 周**：状态交互链完善（自然回复、蜕皮、治愈之心、清淋、湿润之躯）+ 状态阻断逻辑

### Phase 4 总预估：10 天（~2 周）

---

## 八、Phase 5：伤害公式精确对齐（1 周）

### 8.1 与 Showdown 的差异点

当前伤害公式接近 Showdown 但有简化。需要逐项对齐：

```
Showdown 伤害公式 (Gen 7/8/9):

1. Base Damage = floor((2 * Level / 5 + 2) * Power * Atk / Def / 50) + 2

2. Modifier Chain (乘法链，每步都 floor):
   × SpreadMoveModifier (群体招式 0.75x)
   × WeatherModifier (天气 1.5x / 0.5x)
   × CriticalHitModifier (会心 1.5x / Sniper 2.25x)
   × RandomFactor [0.85, 0.86, ..., 1.00]
   × STAB (1.5x / Adaptability 2.0x / Tera 2.0x / Tera+Adaptability 2.25x)
   × TypeEffectiveness (×4 / ×2 / ×1 / ×0.5 / ×0.25 / ×0)
   × BurnModifier (灼伤物理 0.5x / Guts 无视)
   × OtherModifiers (特性 + 道具 + 场地 + 墙 + 状态):
     - Sniper 2.25x
     - Tinted Lens 2x (效果不好)
     - Filter/Solid Rock/Prism Armor 0.75x
     - Friend Guard 0.75x
     - Fluffy 0.5x (接触) / 2x (火)
     - Multiscale/Shadow Shield 0.5x (满 HP)
     - Reflect/Light Screen/Aurora Veil (2/3 或 1/2 取决于格式)
     - Neuroforce 1.25x (克制)
     - Analytic 1.3x (后手)
     - Stakeout 2x (对方换入)
     - Battery/Power Spot 1.3x
     - Aura abilities (Dark/Fairy Aura) 4/3x
     - Helping Hand 1.5x
     - Z-Move 1.5x (基础 + 特殊加成)
     - Dynamax 1.3x
     - Glaive Rush 2x (对手下回合)
     - Collision Course/Electro Drift (克制效果的 1.3333x 修正)

3. Final Damage = max(1, floor(base * modifier))
```

### 8.2 需要修改的地方

1. **会心计算**：Sniper 从 1.5x → 2.25x（已在 EffectRegistry 注册但未在 damageCalc 中使用）
2. **Neuroforce**：克制伤害 1.25x
3. **Stakeout**：对方换入时 2x
4. **多段攻击修正顺序**：Parental Bond 第二击 0.25x
5. **Tera 修正**：Tera + Adaptability = 2.25x 而非 2.0x
6. **Fluffy 修正**：接触招式 0.5x + 火系 2x
7. **Z-Move 修正**：基础 1.5x + 招式特定系数
8. **Collision Course/Electro Drift**：克制时 1.3333x
9. **确伤处理**：Guard Spec 保护、Magic Guard 不挡、Sturdy 残血 1 等

**预估工时**：5 天

---

## 九、Phase 6：场地效果补全（1 周）

### 9.1 已完成场地效果（95%）

✅ 天气：Rain, Sun, Sand, Snow
✅ 场地：Electric, Psychic, Grassy, Misty
✅ 房间：Trick Room, Gravity, Magic Room
✅ 墙：Reflect, Light Screen, Aurora Veil
✅ 顺风：Tailwind (player/opponent)
✅ 钉子：Stealth Rock, Spikes, Toxic Spikes, Sticky Web
✅ 神秘守护：Safeguard
✅ 预知未来/破灭之愿：Future Sight/Doom Desire

### 9.2 待补全

| 缺失 | 说明 |
|------|------|
| **Ion Deluge** (等离子雨) | 所有一般系招式变电系（场地效果） |
| **Fairy Lock** (妖精锁) | 下回合禁止切换（已列在挥发状态中） |
| **Magic Room 完整** | 禁止道具使用（当前已设置 turns 字段，但未在使用中检查） |
| **Wonder Room 奇妙空间** | 交换防御和特防 |
| **Mud Sport/Water Sport** | 电系/火系威力减半（Gen 6 以前，低优先） |
| **G-Max Wildfire/Cannonade/Vine Lash** | 已实现框架，待完整测试 |
| **G-Max Steelsurge** | 撒钉+钢属性（Stonesurge 同） |
| **天气球/地形球招式** | 天气/场地中威力翻倍且属性变化（在招式阶段处理） |

**预估工时**：5 天

---

## 十、Phase 7：特殊系统完善（2 周）

### 10.1 Mega 进化（已完成 ✅）

- 进化/退化状态管理 ✅
- 属性/特性变化 ✅

### 10.2 Z 招式（已完成 ✅）

- Z-Status 效果矩阵 ✅
- 基于基础招式决定 Z 招式名称 ✅
- 按 Z 招式类别修正伤害 ✅
- 一次性消耗限制 ✅

### 10.3 极巨化（基本完成）

✅ 已实现：
- 极巨化/超极巨化切换
- HP 翻倍
- 招式变极巨招式
- 属性不变（确认与 Showdown 一致）

❌ 待补全：
- 各极巨招式的追加效果（Max Airstream +1 Speed 等）— 已部分
- G-Max 专属招式的完整伤害和效果
- 极巨化回合数限制和弹回
- Eject Button/Red Card 在极巨化时的交互

### 10.4 太晶化（已完成 ✅）

- 属性变更 ✅
- 太晶爆发动态类型 ✅
- STAB 加成调整 ✅
- Tera + Adaptability = 2.25x：待实现

### 10.5 G-Max 招式效果矩阵

| G-Max 招式 | 效果 | 状态 |
|------------|------|------|
| G-Max Wildfire | 火系持续伤害 | ✅ 框架已实现 |
| G-Max Cannonade | 水系持续伤害 | ✅ 框架已实现 |
| G-Max Vine Lash | 草系持续伤害 | ✅ 框架已实现 |
| G-Max Stonesurge | 撒钉 (Stealth Rock) | ✅ 已实现 |
| G-Max Steelsurge | 撒钉 (Steel-entry) | ✅ 已实现 |
| G-Max Befuddle | 毒/麻痹/睡眠随机 | ❌ |
| G-Max Chi Strike | 会心率 +1 | ❌ |
| G-Max Cuddle | 着迷 | ❌ |
| G-Max Finale | 回复我方 HP 1/6 | ❌ |
| G-Max Foam Burst | 降对方 Speed 2 级 | ❌ |
| G-Max Gold Rush | 撒钱 (拾取) | ❌ |
| G-Max Gravitas | 重力 | ❌ |
| G-Max Malodor | 中毒 | ❌ |
| G-Max Meltdown | 降对方 Torment/无法连用 | ❌ |
| G-Max Replenish | 50% 概率回复已消耗道具 | ❌ |
| G-Max Resonance | Aurora Veil | ❌ |
| G-Max Sandblast | 束缚伤害 | ❌ |
| G-Max Smite | 混乱 | ❌ |
| G-Max Snooze | 睡眠或回复 (50%) | ❌ |
| G-Max Sweetness | 治愈我方状态 | ❌ |
| G-Max Tartness | 降对方闪避 | ❌ |
| G-Max Terror | 无法切换 | ❌ |
| G-Max Volcalith | 岩系持续伤害 | ❌ |
| G-Max Volt Crash | 麻痹 | ❌ |
| G-Max Wind Rage | 清除场地效果 (Defog 等效) | ❌ |

**预估工时**：5 天（Max 招式效果）+ 5 天（G-Max 效果矩阵）= 10 天

---

## 十一、Phase 8：对战格式扩展（2 周）

### 11.1 当前格式

✅ Singles (6→3 选人，1 个活动位)
✅ Doubles (6→4 选人，2 个活动位) — VGC 规则
✅ Factory Challenge (9 回合连续对战)

### 11.2 需求格式

| 格式 | Showdown 等效 | 说明 |
|------|-------------|------|
| Triples | 三打对战 (6→3 选人，3 个活动位) | Gen 6-7 |
| Rotation | 轮盘对战 (6→4 选人，1 个活动位可旋转) | Gen 5-6 |
| Battle Royal | 皇家对战 (6→3 选人，每人 1 个活动位, 4 人对战) | Gen 7-8 |
| Multi Battle | 多人对战 (2v2，每人 3 只，2 个活动位) | 多玩家 |
| Inverse Battle | 反转对战 (属性相克反转) | Gen 6 |
| Sky Battle | 空中对战 (仅限飞行系/漂浮) | Gen 6 |
| Gen-specific | 分代规则 (无 Mega/Z/Dyna/Tera) | 全部 |

**预估工时**：10 天（三打 + 轮盘 + 皇家 + 格式选择系统）

---

## 十二、Phase 9：竞技 AI（3 周）

### 12.1 当前 AI

当前 AI 系统（`BattleAISupport.java` + `BattleAiSwitchSupport.java`）提供基础决策：
- 随机招式选择（带简单加权）
- 基础换人逻辑（被克制时换人）
- 基础道具使用

### 12.2 目标 AI

需要实现 **3 层 AI 系统**：

```
Layer 1: 规则 AI (当前)
  - 总是使用克制招式
  - 避免使用无效招式
  - 基础换人判断
  - 道具使用逻辑

Layer 2: Minimax AI (2-3 回合前瞻)
  - 搜索树：深度 2-3 回合的 minmax
  - 评估函数：基于 HP 差、PM 数量、能力阶级、场地状态
  - Alpha-Beta 剪枝

Layer 3: Monte Carlo Tree Search (可选，高计算量)
  - 大量随机模拟
  - 适合工厂挑战 Boss
```

**实现步骤**：

1. 构建状态评估器（`BattleEvaluator.java`）：
   - HP 多寡加权
   - 属性克制矩阵
   - 能力阶级加成
   - 场地控制（天气/场地/钉子）
   - 队伍配合系数

2. 构建搜索树：
   - 每回合生成合法动作列表
   - 对每个动作评估状态切换
   - 深度搜索 N 层

3. 实现 Alpha-Beta 剪枝：
   - 剪枝深度：2-3
   - 评估函数耗时 < 10ms

4. 对战知识库：
   - 常见套路识别
   - 保护/先读预测
   - PM 角色分类（坦克/炮台/扫钉/辅助）

**预估工时**：15 天

---

## 十三、Phase 10：测试基础设施（贯穿）

### 13.1 测试金字塔

```
         ╱ 集成测试 ╲        50-100 项
        ╱  对战场景   ╲       (端到端战斗)
       ╱───────────────╲
      ╱  功能测试        ╲    200-300 项
     ╱   单个模块/系统    ╲    (特性/道具/招式效果)
    ╱─────────────────────╲
   ╱   单元测试              ╲  500-800 项
  ╱    纯函数/公式           ╲  (伤害公式/命中率/速度排序)
 ╱─────────────────────────────╲
```

### 13.2 测试场景矩阵

应覆盖以下对战场景（从 Showdown 标准测试集中提取）：

```
基础对战:
  - 单打正常对战（克制/非克制/免疫）
  - 双打正常对战（双目标/单目标/群体招式）
  - 场地效果（天气 + 场地 + 房间同时存在）
  - 属性交互（双属性克制、4x 克制、多重免疫）

状态交互:
  - 麻痹 + 先制度
  - 灼伤 + 物理减半
  - 剧毒递增伤害
  - Sleep Talk 选择
  - Confusion 自伤
  - 冰冻破冰

特性交互:
  - Magic Bounce + Taunt
  - 威吓 + Defiant/Guard Dog/Clear Body
  - 亲子爱 + 多段攻击
  - Magic Guard + 生命宝珠
  - Unaware + Swords Dance
  - Neutralizing Gas 出入场

道具交互:
  - 讲究围巾 + 挑衅（只能 Struggle）
  - 树果 + Unnerve
  - Trick/Switcheroo + 各种道具
  - 红牌 + 极巨化

特殊系统:
  - Mega 进化 + 特性变化
  - Z 招式 + Z-Status 效果
  - 极巨化 + 极巨招式副效果
  - 太晶化 + Tera Blast 属性变化
  - 多个 G-Max 同时存在

边缘情况:
  - 最后一只 PM 倒下
  - 倒计时平局
  - 双方同时倒下
  - 挣扎
  - PP 耗尽
  - 替身 + 各种效果
```

### 13.3 测试数据

从 Showdown 源码中提取标准测试用例：
- `test/sim/moves/` 中的招式测试
- `test/sim/abilities/` 中的特性测试
- `test/sim/items/` 中的道具测试

**预估工时**：贯穿全程，额外 5 天用于编写测试基础设施

---

## 十四、实施路线图（按时间）

```
月份 1:  Phase 0 (基础设施) + Phase 4 (状态系统)
月份 2:  Phase 1 (特性系统) 前半 (伤害/能力阶级)
月份 3:  Phase 1 (特性系统) 后半 (状态/治愈/特殊)
月份 4:  Phase 2 (招式效果) 前半 (P0 招式效果)
月份 5:  Phase 2 (招式效果) 后半 (P1/P2 + 多段/先制)
月份 6:  Phase 3 (道具系统) + Phase 5 (伤害公式) + Phase 6 (场地补全)
月份 7:  Phase 7 (特殊系统) + Phase 8 (对战格式) + Phase 9 (AI)
─────────────────────────────────────────────────────────
月份 7 末: 100% Showdown 兼容
```

### 14.1 里程碑

| 里程碑 | 完成度 | 特性数 | 道具数 | 招式数 | 测试数 | 工时 |
|--------|--------|--------|--------|--------|--------|------|
| M0: 当前 | ~50% | ~100 | ~80 | ~220 | 435 | — |
| M1: Phase 0+4 完成 | ~55% | ~100 | ~80 | ~220 | 500 | 5 周 |
| M2: Phase 1 完成 | ~70% | ~250 | ~80 | ~220 | 700 | 4 周 |
| M3: Phase 2 完成 | ~85% | ~250 | ~80 | ~600 | 1000 | 6 周 |
| M4: Phase 3+5+6 完成 | ~92% | ~250 | ~400 | ~600 | 1200 | 4 周 |
| M5: Phase 7+8+9 完成 | ~100% | ~298 | ~1000 | ~900 | 1500 | 7 周 |

### 14.2 每个 Phase 的检查点

开发完每个 Phase 后：
1. ✅ 全部现有测试通过
2. ✅ 新增测试覆盖新功能
3. ✅ 手动测试 5 个典型对战场景
4. ✅ 代码重构成 EffectRegistry 模式
5. ✅ 文档更新

---

## 十五、风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| POJO 迁移导致大规模回退 | Phase 0 延期 | 渐进式迁移，保持 Map 兼容 |
| 招式系统过于庞大 | Phase 2 延期 | 按效果模板批量实现，非逐个 |
| 测试覆盖不足导致回退 | 质量下降 | 每阶段必须加测试 |
| 单个开发者工作量大 | 进度慢 | 先核心后周边，按 VGC 重要性排序 |
| Showdown 机制变更 | 对齐失效 | 定期查看 Showdown changelog |

---

## 十六、附录

### A. 文件结构最终形态

```
battle-factory/src/main/java/com/lio9/battle/
├── model/                         # 强类型状态模型 (Phase 0)
│   ├── Pokemon.java
│   ├── Move.java
│   ├── BattleState.java
│   ├── FieldEffects.java
│   ├── SideField.java
│   └── ...
├── effect/                        # 效果注册与处理 (Phase 1-3)
│   ├── EffectRegistry.java        # ~300 abilities + ~1000 items
│   ├── AbilityHandler.java
│   ├── ItemHandler.java
│   ├── MoveHandler.java           # NEW: 招式效果处理器
│   ├── handler/                   # 特殊 handler (独立类)
│   │   ├── ContraryAbility.java
│   │   ├── DancerAbility.java
│   │   └── ...
│   └── context/                   # 上下文类
│       ├── AttackContext.java
│       ├── ContactContext.java
│       ├── DamageReceivedContext.java
│       └── ...
├── engine/                        # 战斗引擎 (已有)
│   ├── BattleEngine.java
│   ├── BattleRoundSupport.java    # 招式结算管道
│   ├── BattleDamageSupport.java   # 伤害公式
│   ├── BattleConditionSupport.java
│   ├── BattleFieldEffectSupport.java
│   ├── BattleTurnCleanupSupport.java
│   ├── MoveRegistry.java
│   └── ...
├── event/                         # 事件系统 (Phase 0)
│   ├── BattleEventBus.java
│   ├── events/
│   │   ├── BeforeMoveEvent.java
│   │   ├── TryHitEvent.java
│   │   ├── DamageEvent.java
│   │   └── ...
│   └── handlers/
├── ai/                            # AI 系统 (Phase 9)
│   ├── BattleEvaluator.java
│   ├── MinimaxSearch.java
│   ├── MonteCarloSearch.java
│   └── AIDecisionMaker.java
├── format/                        # 对战格式 (Phase 8)
│   ├── BattleFormat.java
│   ├── FormatValidator.java
│   └── formats/
│       ├── SinglesFormat.java
│       ├── DoublesFormat.java
│       ├── TriplesFormat.java
│       └── ...
├── service/
├── controller/
└── ...
```

### B. 数据导入脚本

```python
# scripts/import_showdown_data.py
# 从 Showdown data/ 目录解析 JSON/JS 文件
# 导入 pokemon, abilities, items, moves, learnsets 到 SQLite
```

### C. 版本支持矩阵

| Gen | 特性 | 招式 | 道具 | 系统 |
|-----|------|------|------|------|
| Gen 5 | 164 | 559 | — | Triple/Rotation |
| Gen 6 | 191 | 621 | — | Mega Evolution |
| Gen 7 | 233 | 728 | — | Z-Moves |
| Gen 8 | 272 | 859 | — | Dynamax/G-Max |
| Gen 9 | 298 | 915 | — | Terastallization |

---

> **下一步**：确认 Phase 0 启动 — 从 POJO 状态模型开始？
