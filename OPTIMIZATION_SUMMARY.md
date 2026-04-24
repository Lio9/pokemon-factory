# 项目结构和对战功能优化总结

## 已完成的优化

### 1. ✅ 创建统一配置类 (BattleConfig.java)

**位置**: `battleFactory/src/main/java/com/lio9/battle/config/BattleConfig.java`

**功能**:
- 集中管理所有战斗相关的魔法数字
- 支持通过 `application.yml` 外部配置
- 包含以下配置组:
  - 基础配置 (等级、队伍大小、回合数等)
  - 积分配置 (胜利/失败积分、段位所需积分)
  - 特殊系统概率 (Mega/Z招式/极巨化/太晶化)
  - Mega进化数值加成
  - 匹配配置 (对手池窗口、抽样数量等)

**使用示例**:
```java
@Autowired
private BattleConfig config;

int level = config.getLevel(); // 50
int winPoints = config.getPoints().getWinPoints(); // 10
```

### 2. ✅ 创建通用工具类 (BattleUtils.java)

**位置**: `common/src/main/java/com/lio9/common/util/BattleUtils.java`

**提供的工具方法**:
- `toInt()` - 安全类型转换
- `toLong()` / `toDouble()` / `toString()` - 其他类型转换
- `toMap()` - Map 转换
- `normalizeMoveName()` - 招式名称规范化
- `matchesMovePattern()` - 招式模式匹配
- `calculateHpPercentage()` - HP百分比计算
- `isFullHp()` - 满HP判断
- `clamp()` - 值范围限制

**优势**:
- 消除重复的 `toInt()` 方法定义
- 统一的异常处理
- 提高代码可读性

### 3. ✅ 创建招式注册表 (MoveRegistry.java)

**位置**: `battleFactory/src/main/java/com/lio9/battle/engine/MoveRegistry.java`

**解决的问题**:
- 替代 BattleEngine 中的 78+ 个 `isXxx()` 方法
- 使用 Set 集合存储招式名称，O(1) 查找效率
- 统一的名称规范化逻辑（处理连字符和空格）

**提供的分类**:
- 保护类招式 (Protect, Detect, Wide Guard, Quick Guard)
- 引导类招式 (Follow Me, Rage Powder)
- 辅助类招式 (Helping Hand, Ally Switch)
- 场地效果类 (Tailwind, Trick Room)
- 天气类 (Rain Dance, Sunny Day, Sandstorm, Snow)
- 地形类 (4种场地)
- 屏风类 (Reflect, Light Screen, Aurora Veil, Safeguard)
- 状态异常类 (Thunder Wave, Will-O-Wisp, Toxic, Spore等)
- 封锁类 (Taunt, Encore, Disable, Torment, Heal Block)
- 先制攻击类 (Fake Out, Sucker Punch, Feint)
- 速度控制类 (Icy Wind, Electroweb, Snarl)
- 轮换类 (U-Turn, Volt Switch, Flip Turn, Parting Shot)
- 自我强化类 (Swords Dance, Nasty Plot, Dragon Dance等12种)
- 入场 hazards 类 (Stealth Rock, Spikes, Toxic Spikes, Sticky Web)
- 回复类 (Recover, Roost, Rest等8种)
- 蓄力/硬直类招式
- 特殊招式 (Tera Blast, Knock Off)

**使用方法**:
```java
// 旧方式
if (engine.isProtect(move)) { ... }

// 新方式
if (MoveRegistry.isProtect(move)) { ... }
```

---

## 待实施的优化 (下一步)

### 4. 🔄 重构宝可梦生成系统

**问题**:
- 当前完全随机生成，无强度评估
- EV分配过于简化 (都是 4/252/252)
- 特性总是选第一个
- 道具选择与定位无关

**改进方案**:

#### 4.1 添加队伍强度评估算法

```java
public class TeamBalanceEvaluator {

    /**
     * 评估单只宝可梦的战斗力
     */
    public double evaluatePokemonStrength(Map<String, Object> pokemon) {
        double score = 0.0;

        // 1. 种族值总和 (BST)
        Map<String, Object> stats = (Map<String, Object>) pokemon.get("stats");
        int bst = calculateBST(stats);
        score += bst * 0.5; // BST贡献50%权重

        // 2. 招式威力
        List<Map<String, Object>> moves = (List<Map<String, Object>>) pokemon.get("moves");
        double avgPower = calculateAverageMovePower(moves);
        score += avgPower * 2.0; // 平均威力贡献

        // 3. 属性覆盖度
        Set<Integer> coveredTypes = getCoveredTypes(moves);
        score += coveredTypes.size() * 5.0; // 每多一个属性+5分

        // 4. 是否有强化技能
        boolean hasSetupMove = moves.stream()
            .anyMatch(m -> isSetupMove(m));
        if (hasSetupMove) score += 20;

        // 5. 是否有回复技能
        boolean hasRecovery = moves.stream()
            .anyMatch(m -> isRecoveryMove(m));
        if (hasRecovery) score += 15;

        return score;
    }

    /**
     * 评估整个队伍的平衡性
     */
    public TeamBalanceScore evaluateTeamBalance(List<Map<String, Object>> team) {
        TeamBalanceScore score = new TeamBalanceScore();

        // 1. 总战斗力
        double totalStrength = team.stream()
            .mapToDouble(this::evaluatePokemonStrength)
            .sum();
        score.setTotalStrength(totalStrength);

        // 2. 属性多样性
        Set<Integer> allTypes = new HashSet<>();
        for (Map<String, Object> mon : team) {
            allTypes.addAll(getPokemonTypes(mon));
        }
        score.setTypeCoverage(allTypes.size());

        // 3. 角色分布
        Map<String, Integer> roles = analyzeTeamRoles(team);
        score.setRoleDistribution(roles);

        // 4. 速度层次
        List<Integer> speeds = team.stream()
            .map(m -> ((Map<String, Object>)m.get("stats")).get("speed"))
            .map(Number.class::cast)
            .map(Number::intValue)
            .sorted()
            .collect(Collectors.toList());
        score.setSpeedLayers(speeds);

        return score;
    }
}
```

#### 4.2 智能EV分配

```java
public class EVAllocator {

    /**
     * 根据宝可梦定位智能分配EV
     */
    public Map<String, Integer> allocateEVs(Map<String, Object> pokemon) {
        Map<String, Object> baseStats = (Map<String, Object>) pokemon.get("baseStats");
        List<Map<String, Object>> moves = (List<Map<String, Object>>) pokemon.get("moves"));

        PokemonRole role = determineRole(pokemon, moves);

        return switch (role) {
            case PHYSICAL_SWEEPER -> allocatePhysicalSweeper(baseStats);
            case SPECIAL_SWEEPER -> allocateSpecialSweeper(baseStats);
            case PHYSICAL_TANK -> allocatePhysicalTank(baseStats);
            case SPECIAL_TANK -> allocateSpecialTank(baseStats);
            case SUPPORT -> allocateSupport(baseStats);
            case MIXED_ATTACKER -> allocateMixedAttacker(baseStats);
            default -> allocateBalanced(baseStats);
        };
    }

    private Map<String, Integer> allocatePhysicalSweeper(Map<String, Object> stats) {
        int hp = Math.min(252, Math.max(4, getInt(stats, "hp") > 100 ? 252 : 4));
        int atk = 252;
        int spe = 252;
        // 剩余4点给防御较高的那项
        int def = getInt(stats, "defense");
        int spd = getInt(stats, "specialDefense");
        Map<String, Integer> evs = new HashMap<>();
        evs.put("hp", hp == 252 ? 4 : hp);
        evs.put("atk", atk);
        evs.put("def", def >= spd ? 4 : 0);
        evs.put("spa", 0);
        evs.put("spd", spd > def ? 4 : 0);
        evs.put("spe", spe);
        return evs;
    }

    private PokemonRole determineRole(Map<String, Object> pokemon, List<Map<String, Object>> moves) {
        int atk = getInt(pokemon.get("baseStats"), "attack");
        int spa = getInt(pokemon.get("baseStats"), "specialAttack");
        int def = getInt(pokemon.get("baseStats"), "defense");
        int spd = getInt(pokemon.get("baseStats"), "specialDefense");
        int spe = getInt(pokemon.get("baseStats"), "speed");

        long physicalMoves = moves.stream().filter(m -> isPhysical(m)).count();
        long specialMoves = moves.stream().filter(m -> isSpecial(m)).count();
        long statusMoves = moves.stream().filter(m -> isStatus(m)).count();

        if (statusMoves >= 3) return SUPPORT;
        if (physicalMoves > specialMoves && atk > spa) {
            return atk > 100 && spe > 90 ? PHYSICAL_SWEEPER : PHYSICAL_TANK;
        }
        if (specialMoves > physicalMoves && spa > atk) {
            return spa > 100 && spe > 90 ? SPECIAL_SWEEPER : SPECIAL_TANK;
        }
        return MIXED_ATTACKER;
    }
}
```

#### 4.3 智能特性选择

```java
public class AbilitySelector {

    /**
     * 根据招式池和队伍需求选择最佳特性
     */
    public String selectBestAbility(Map<String, Object> pokemon,
                                     List<Map<String, Object>> moves,
                                     List<Map<String, Object>> team) {
        List<String> abilities = (List<String>) pokemon.get("abilities");
        if (abilities.isEmpty()) return null;
        if (abilities.size() == 1) return abilities.get(0);

        // 为每个特性评分
        Map<String, Double> scores = new HashMap<>();
        for (String ability : abilities) {
            double score = evaluateAbility(ability, pokemon, moves, team);
            scores.put(ability, score);
        }

        // 返回最高分的特性
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(abilities.get(0));
    }

    private double evaluateAbility(String ability, Map<String, Object> pokemon,
                                    List<Map<String, Object>> moves,
                                    List<Map<String, Object>> team) {
        double score = 0.0;

        // 1. 检查是否与招式有协同
        if (ability.equals("Technician") && hasLowPowerMoves(moves)) {
            score += 30;
        }
        if (ability.equals("Sheer Force") && hasSecondaryEffectMoves(moves)) {
            score += 25;
        }
        if (ability.equals("Guts") && hasStatusMove(moves)) {
            score += 20;
        }

        // 2. 检查是否弥补队伍弱点
        if (ability.equals("Levitate") && teamHasGroundWeakness(team)) {
            score += 15;
        }

        // 3. 隐藏特性优先
        if (isHiddenAbility(ability)) {
            score += 10;
        }

        return score;
    }
}
```

#### 4.4 智能道具选择

```java
public class ItemSelector {

    /**
     * 根据宝可梦定位选择最佳道具
     */
    public String selectBestItem(Map<String, Object> pokemon,
                                  List<Map<String, Object>> moves) {
        PokemonRole role = determineRole(pokemon, moves);

        return switch (role) {
            case PHYSICAL_SWEEPER -> {
                if (hasSetupMove(moves)) yield "Life Orb";
                if (needsSpeedBoost(pokemon)) yield "Choice Scarf";
                yield "Choice Band";
            }
            case SPECIAL_SWEEPER -> {
                if (hasSetupMove(moves)) yield "Life Orb";
                yield "Choice Specs";
            }
            case PHYSICAL_TANK -> "Leftovers";
            case SPECIAL_TANK -> "Leftovers";
            case SUPPORT -> {
                if (isLead(pokemon)) yield "Focus Sash";
                yield "Light Clay";
            }
            default -> "Life Orb";
        };
    }
}
```

### 5. 🔄 优化对手池匹配

**当前问题**:
- 抽样窗口太小 (±2)
- 无强度过滤

**改进方案**:

```java
@Service
public class ImprovedMatchmakingService {

    @Autowired
    private BattleConfig config;

    /**
     * 基于ELO rating的匹配系统
     */
    public List<Map<String, Object>> findOpponents(int playerRank, int playerTierPoints) {
        int window = config.getMatchmaking().getPoolWindow(); // 从配置读取
        int sampleSize = config.getMatchmaking().getPoolSampleSize();

        // 扩大搜索范围，确保找到合适的对手
        List<Map<String, Object>> candidates = poolMapper.findByRankRange(
            Math.max(0, playerRank - window),
            playerRank + window,
            sampleSize * 3 // 多取一些用于筛选
        );

        // 按实力排序，选择最接近的对手
        return candidates.stream()
            .sorted(Comparator.comparingInt(c -> {
                int opponentPoints = toInt(c.get("tier_points"), 0);
                return Math.abs(opponentPoints - playerTierPoints);
            }))
            .limit(sampleSize)
            .collect(Collectors.toList());
    }

    /**
     * ELO rating 计算
     */
    public int calculateNewRating(int currentRating, boolean won, int opponentRating) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - currentRating) / 400.0));
        double actualScore = won ? 1.0 : 0.0;
        int K = 32; // K因子，决定变化幅度
        return (int) Math.round(currentRating + K * (actualScore - expectedScore));
    }
}
```

### 6. 🔄 清理 BattleEngine 中的重复代码

**计划**:
1. 将所有 `isXxx()` 方法调用替换为 `MoveRegistry.isXxx()`
2. 删除 BattleEngine 中的 78+ 个私有方法
3. 将 `toInt()` 等方法调用替换为 `BattleUtils.toInt()`

**预期效果**:
- BattleEngine.java 从 1400+ 行减少到 ~800 行
- 代码更清晰，易于维护
- 招式判断逻辑统一管理

---

## 配置文件示例

在 `application.yml` 中添加:

```yaml
pokemon:
  battle:
    level: 50
    factory-team-size: 6
    battle-team-size: 4
    active-slots: 2
    max-rounds: 12
    max-factory-battles: 9

    points:
      win-points: 10
      lose-points: 3
      points-per-tier: 2000

    special-systems:
      mega-chance: 18
      z-move-chance: 16
      dynamax-chance: 16
      tera-chance: 0

    mega-evolution:
      attack-multiplier: 1.18
      defense-multiplier: 1.12
      speed-multiplier: 1.1

    matchmaking:
      pool-window: 5
      pool-sample-size: 3
      ai-opponent-rank-bonus: 1
```

---

## 实施优先级

### P0 - 立即执行 (已完成)
- ✅ 创建 BattleConfig 配置类
- ✅ 创建 BattleUtils 工具类
- ✅ 创建 MoveRegistry 招式注册表

### P1 - 短期 (本周内)
- [ ] 重构 AIService 的宝可梦生成逻辑
- [ ] 实现 TeamBalanceEvaluator
- [ ] 实现 EVAllocator
- [ ] 实现 AbilitySelector
- [ ] 实现 ItemSelector

### P2 - 中期 (本月内)
- [ ] 优化对手池匹配算法
- [ ] 实现 ELO rating 系统
- [ ] 清理 BattleEngine 中的重复代码
- [ ] 将所有 Support 类迁移到使用新的工具类

### P3 - 长期
- [ ] BattleEngine 进一步模块化
- [ ] 添加 WebSocket 实时对战
- [ ] 完善测试覆盖

---

## 预期收益

1. **代码质量提升**:
   - 减少 40%+ 的重复代码
   - BattleEngine 行数减少 40%+
   - 配置集中管理，易于调整

2. **游戏平衡性提升**:
   - 宝可梦生成分布更合理
   - 队伍强度差异控制在 ±20% 以内
   - 对手匹配更公平

3. **可维护性提升**:
   - 新增招式只需在 MoveRegistry 添加一行
   - 调整平衡性只需修改配置文件
   - 工具方法复用，减少bug

4. **扩展性提升**:
   - 易于添加新的特殊系统
   - 易于调整匹配算法
   - 易于集成新的战斗机制
