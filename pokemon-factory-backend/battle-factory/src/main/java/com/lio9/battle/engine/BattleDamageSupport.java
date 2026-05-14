package com.lio9.battle.engine;

import com.lio9.battle.effect.AttackContext;
import com.lio9.battle.effect.EffectRegistry;
import com.lio9.battle.effect.SpeedContext;
import com.lio9.battle.effect.WeightContext;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleDamageSupport {
    /**
     * 伤害与速度计算支持类。
     * <p>
     * 这里封装了对战引擎中最容易与 PS 标准产生偏差的数值链路：
     * 基础伤害、STAB、属性克制、特性/道具修正、群攻衰减、暴击、速度比较等。
     * 本轮关键点包括：
     * <ul>
     * <li>暴击可以由上层预解析后传入，避免重复随机</li>
     * <li>Spread 修正按“实际命中目标数”生效</li>
     * <li>Unburden 等速度修正纳入统一速度计算</li>
     * </ul>
     * </p>
     */
    private final BattleEngine engine;
    private final TypeEfficacyMapper typeEfficacyMapper;
    private final BattleFieldEffectSupport fieldEffectSupport;
    private final int level;

    BattleDamageSupport(BattleEngine engine, TypeEfficacyMapper typeEfficacyMapper,
            BattleFieldEffectSupport fieldEffectSupport, int level) {
        this.engine = engine;
        this.typeEfficacyMapper = typeEfficacyMapper;
        this.fieldEffectSupport = fieldEffectSupport;
        this.level = level;
    }

    int calculateDamage(Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move,
            Random random,
            Map<Map<String, Object>, Boolean> helpingHandBoosts, Map<String, Object> state) {
        int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);

        // Critical hits can be pre-resolved by the action pipeline to keep RNG/logging
        // consistent.
        Object preResolvedCritical = move.get("criticalHit");
        boolean criticalHit = preResolvedCritical instanceof Boolean
                ? (Boolean) preResolvedCritical
                : calculateCriticalHitChance(attacker, move, random);

        // Merciless: 攻击中毒目标必中要害
        if (!criticalHit) {
            String attAbility = engine.abilityName(attacker);
            if ("merciless".equalsIgnoreCase(attAbility)) {
                String defCondition = String.valueOf(defender.getOrDefault("condition", ""));
                if ("poison".equals(defCondition) || "toxic".equals(defCondition)) {
                    criticalHit = true;
                }
            }
        }

        // Battle Armor / Shell Armor: 阻挡会心一击（光子喷涌/暗影之光无视）
        if (criticalHit && !ignoresTargetAbility(attacker, move)) {
            String defAbility = engine.abilityName(defender);
            if ("battle-armor".equalsIgnoreCase(defAbility) || "battle armor".equalsIgnoreCase(defAbility)
                    || "shell-armor".equalsIgnoreCase(defAbility) || "shell armor".equalsIgnoreCase(defAbility)) {
                criticalHit = false;
            }
        }

        Map<String, Object> attackerStats = engine.castMap(attacker.get("stats"));
        Map<String, Object> defenderStats = engine.castMap(defender.get("stats"));

        int attackStat;
        if (MoveRegistry.isBodyPress(move)) {
            // 扑击：使用攻击者的防御代替攻击
            int bodyPressDef = engine.toInt(attackerStats.get("defense"), 100);
            attackStat = modifiedDefenseStat(defender, attacker, bodyPressDef, damageClassId, state, criticalHit, move);
        } else if (MoveRegistry.isFoulPlay(move)) {
            // 欺诈：使用目标的攻击属性
            int targetAtk = engine.toInt(defenderStats.get("attack"), 100);
            attackStat = modifiedAttackStat(defender, attacker, targetAtk, damageClassId, criticalHit, move, state);
        } else {
            attackStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                    ? modifiedAttackStat(attacker, defender, engine.toInt(attackerStats.get("attack"), 100), damageClassId,
                            criticalHit, move, state)
                    : modifiedAttackStat(attacker, defender, engine.toInt(attackerStats.get("specialAttack"), 100),
                            damageClassId, criticalHit, move, state);
        }
        // 奇妙空间（Wonder Room）：5 回合内物防/特防互换
        boolean wonderRoomActive = fieldEffectSupport.wonderRoomTurns(state) > 0;
        int baseDefenseStat;
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            baseDefenseStat = wonderRoomActive
                    ? engine.toInt(defenderStats.get("specialDefense"), 100)
                    : engine.toInt(defenderStats.get("defense"), 100);
        } else {
            baseDefenseStat = wonderRoomActive
                    ? engine.toInt(defenderStats.get("defense"), 100)
                    : engine.toInt(defenderStats.get("specialDefense"), 100);
        }
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        int defenseStat = Math.max(1,
                modifiedDefenseStat(attacker, defender, baseDefenseStat, damageClassId, state, criticalHit, move));

        int power = calculateMovePower(move, attacker, defender, state);
        // 威力为 0 表示招式应该失败（如 Spit-up 无蓄力、Natural Gift 无树果等）
        if (power <= 0) {
            return 0;
        }
        int baseDamage = DamageCalculatorUtil.calculateBaseDamage(level, power, attackStat, defenseStat);

        double modifier = 1.0d;

        // -ate 特性：将一般系招式转为对应属性（在 STAB 前转换，使 STAB 正确生效），1.3x 增伤
        if (moveTypeId == DamageCalculatorUtil.TYPE_NORMAL) {
            String atkAbility = engine.abilityName(attacker);
            if ("aerilate".equalsIgnoreCase(atkAbility)) {
                moveTypeId = DamageCalculatorUtil.TYPE_FLYING;
                modifier *= 1.3d;
            } else if ("pixilate".equalsIgnoreCase(atkAbility)) {
                moveTypeId = DamageCalculatorUtil.TYPE_FAIRY;
                modifier *= 1.3d;
            } else if ("refrigerate".equalsIgnoreCase(atkAbility)) {
                moveTypeId = DamageCalculatorUtil.TYPE_ICE;
                modifier *= 1.3d;
            } else if ("galvanize".equalsIgnoreCase(atkAbility)) {
                moveTypeId = DamageCalculatorUtil.TYPE_ELECTRIC;
                modifier *= 1.3d;
            }
        }

        // 气象球：有天气时属性随天气变化
        if (MoveRegistry.isWeatherBall(move) && fieldEffectSupport.weatherTurns(state) > 0) {
            if (fieldEffectSupport.rainTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_WATER;
            } else if (fieldEffectSupport.sunTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_FIRE;
            } else if (fieldEffectSupport.snowTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_ICE;
            } else if (fieldEffectSupport.sandTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_ROCK;
            }
        }
        // 地形脉冲：有场地时属性随场地变化
        if (MoveRegistry.isTerrainPulse(move) && fieldEffectSupport.terrainTurns(state) > 0) {
            if (fieldEffectSupport.electricTerrainTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_ELECTRIC;
            } else if (fieldEffectSupport.psychicTerrainTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_PSYCHIC;
            } else if (fieldEffectSupport.grassyTerrainTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_GRASS;
            } else if (fieldEffectSupport.mistyTerrainTurns(state) > 0) {
                moveTypeId = DamageCalculatorUtil.TYPE_FAIRY;
            }
        }

        // 制裁光砾：根据 Arceus 携带的石板改变属性
        if (MoveRegistry.isJudgment(move)) {
            int plateType = typeIdFromPlate(heldItem(attacker));
            if (plateType > 0) moveTypeId = plateType;
        }
        // 多属性攻击：根据 Silvally 携带的记忆改变属性
        if (MoveRegistry.isMultiAttack(move)) {
            int memoryType = typeIdFromMemory(heldItem(attacker));
            if (memoryType > 0) moveTypeId = memoryType;
        }

        // STAB (Same Type Attack Bonus) - Pokemon Showdown standard
        modifier *= stabModifier(attacker, moveTypeId);

        // Type effectiveness
        double typeModifier = typeModifier(defender, moveTypeId);
        // Ring Target: 持有者招式无视属性免疫（但不改变正常/效果绝佳/效果不佳）
        boolean ringTargetActive = "ring-target".equalsIgnoreCase(heldItem(attacker))
                || "ring target".equalsIgnoreCase(heldItem(attacker));
        if (typeModifier <= 0.0d && ringTargetActive) {
            typeModifier = 1.0d;
        }
        // Scrappy & Mind's Eye: Normal-type moves hit Ghost types
        if (typeModifier <= 0.0d && moveTypeId == DamageCalculatorUtil.TYPE_NORMAL
                && (hasAbility(attacker, "scrappy", "mind's-eye", "mind's eye"))) {
            typeModifier = 1.0d;
        }
        // Tar Shot: 被焦油覆盖的目标受到火系招式时属性克制 ×2
        if (Boolean.TRUE.equals(engine.volatileValue(defender, "tarShot", false))
                && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            typeModifier *= 2.0d;
        }
        modifier *= typeModifier;
        if (typeModifier <= 0.0d) {
            return 0;
        }

        // Item modifiers
        modifier *= itemDamageModifier(attacker, defender, moveTypeId, move, state);

        // Ability modifiers
        modifier *= abilityDamageModifier(attacker, defender, move, moveTypeId, state);

        // Helping Hand boost
        if (Boolean.TRUE.equals(helpingHandBoosts.get(attacker))) {
            modifier *= 1.5d;
        }

        // Special system modifiers
        if (engine.isDynamaxed(attacker)) {
            modifier *= 1.3d;
        }
        if (engine.isZMoveActive(attacker, engine.toInt(state.get("currentRound"), 0), move)) {
            modifier *= 1.5d;
        }

        // Wind Power: 充电状态下电系招式伤害 x2
        if (engine.volatileFlag(attacker, "windPowerCharged") && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            modifier *= 2.0d;
        }

        // Critical hit multiplier
        if (criticalHit) {
            // Sniper ability increases crit multiplier from 1.5x to 2.25x
            modifier *= "sniper".equalsIgnoreCase(engine.abilityName(attacker)) ? 2.25d : 1.5d;
        }

        // Other modifiers
        modifier *= fullHpDefenseModifier(attacker, defender, move);
        modifier *= weatherDamageModifier(attacker, state, moveTypeId);
        modifier *= terrainDamageModifier(state, attacker, defender, move, moveTypeId);
        modifier *= screenDamageModifier(state, attacker, defender, damageClassId);

        // 群攻修正必须在最终乘区统一处理，才能和 Helping Hand、天气、屏障等倍率保持同一链路。
        modifier *= spreadMoveModifier(move, state);

        // Partner ability modifiers (Friend Guard, Battery)
        modifier *= partnerAbilityModifier(attacker, defender, state, damageClassId);

        // Random factor (0.85 - 1.00) - Pokemon Showdown standard
        modifier *= (0.85d + (random.nextDouble() * 0.15d));

        return Math.max(1, (int) Math.floor(baseDamage * modifier));
    }

    /**
     * 计算招式威力（支持 Electro Ball / Gyro Ball 等基于速度的动态威力）。
     */
    int calculateMovePower(Map<String, Object> move, Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> state) {
        int basePower = engine.toInt(move.get("power"), 0);
        if (basePower <= 0) return basePower;

        if (MoveRegistry.isElectroBall(move)) {
            // 电球：威力取决于攻击方速度 / 防御方速度的比值
            boolean attackerPlayerSide = engine.isOnSide(state, attacker, true);
            int atkSpeed = speedValue(attacker, state, attackerPlayerSide);
            int defSpeed = speedValue(defender, state, !attackerPlayerSide);
            double ratio = (double) atkSpeed / Math.max(1, defSpeed);
            if (ratio >= 4.0d) return 150;
            if (ratio >= 3.0d) return 120;
            if (ratio >= 2.0d) return 80;
            if (ratio >= 1.0d) return 60;
            return 40; // 慢于对方时威力最低（PS 中实际会失败，但保留保底）
        }

        if (MoveRegistry.isGyroBall(move)) {
            // 陀螺球：威力 = 25 * 防御方速度 / 攻击方速度，上限 150
            boolean attackerPlayerSide = engine.isOnSide(state, attacker, true);
            int atkSpeed = speedValue(attacker, state, attackerPlayerSide);
            int defSpeed = speedValue(defender, state, !attackerPlayerSide);
            return Math.min(150, (25 * defSpeed) / Math.max(1, atkSpeed));
        }

        // 体重相关威力（打草结/踢倒/重磅冲撞/高温重压）
        if (MoveRegistry.isWeightBasedMove(move)) {
            int targetWeight = getWeight(defender);
            String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
            if (nameEn.equals("heavy slam") || nameEn.equals("heavy-slam") || nameEn.equals("heat crash") || nameEn.equals("heat-crash")) {
                // 重磅冲撞/高温重压：基于攻击方/防御方体重比
                int atkWeight = getWeight(attacker);
                double ratio = (double) atkWeight / Math.max(1, targetWeight);
                if (ratio >= 5.0d) return 120;
                if (ratio >= 4.0d) return 100;
                if (ratio >= 3.0d) return 80;
                if (ratio >= 2.0d) return 60;
                return 40;
            } else {
                // 打草结/踢倒：目标体重决定威力
                if (targetWeight >= 2000) return 120;  // 200kg+
                if (targetWeight >= 1000) return 100;  // 100-199.9kg
                if (targetWeight >= 500)  return 80;   // 50-99.9kg
                if (targetWeight >= 250)  return 60;   // 25-49.9kg
                if (targetWeight >= 100)  return 40;   // 10-24.9kg
                return 20;                              // <10kg
            }
        }

        // HP 比例威力（喷火/喷水）：HP 越多威力越大
        if (MoveRegistry.isHpRatioMove(move)) {
            int currentHp = engine.toInt(attacker.get("currentHp"), 0);
            int maxHp = engine.toInt(engine.castMap(attacker.get("stats")).get("hp"), 1);
            int ratio = Math.max(0, Math.min(100, (currentHp * 100) / maxHp));
            if (ratio >= 68) return 150;
            if (ratio >= 35) return 100;
            if (ratio >= 1)  return 50;
            return 0; // HP 为 0 时无法使用
        }

        // 反比 HP 威力（起死回生/抓狂）：HP 越少威力越大
        if (MoveRegistry.isReversalMove(move)) {
            int currentHp = engine.toInt(attacker.get("currentHp"), 0);
            int maxHp = engine.toInt(engine.castMap(attacker.get("stats")).get("hp"), 1);
            int ratio = Math.max(0, (currentHp * 100) / Math.max(1, maxHp));
            if (ratio >= 70) return 20;
            if (ratio >= 35) return 40;
            if (ratio >= 20) return 80;
            if (ratio >= 10) return 100;
            if (ratio >= 5)  return 120;
            if (ratio >= 2)  return 140;
            return 150; // HP < 2%
        }

        // 报恩/迁怒：满亲密时 102 威力（无亲密系统则默认 102）
        if (MoveRegistry.isFriendshipMove(move)) {
            return 102;
        }

        // 喷出（Spit Up）：威力 = 100 × 蓄力层数，无蓄力时失败
        if (MoveRegistry.isSpitUp(move)) {
            int stockpileCount = engine.toInt(engine.volatileValue(attacker, "stockpileCount", 0), 0);
            if (stockpileCount <= 0) return 0; // 调用方应处理失败
            engine.setVolatile(attacker, "stockpileCount", 0); // 喷出后解除蓄力
            return 100 * stockpileCount;
        }

        // 气象球：有天气时威力翻倍
        if (MoveRegistry.isWeatherBall(move)) {
            return fieldEffectSupport.weatherTurns(state) > 0 ? basePower * 2 : basePower;
        }

        // 地形脉冲：有场地时威力翻倍
        if (MoveRegistry.isTerrainPulse(move)) {
            return fieldEffectSupport.terrainTurns(state) > 0 ? basePower * 2 : basePower;
        }

        // 自然之恩（Natural Gift）：根据持有树果决定威力和属性
        if (MoveRegistry.isNaturalGift(move)) {
            String held = heldItem(attacker);
            if (held.isBlank() || !engine.isBerry(held)) return 0; // 无树果则失败
            // 简化：所有树果默认 60 威力、普通属性；实际应查表
            return 60;
        }

        return basePower;
    }

    /**
     * Calculate critical hit chance (Pokemon Showdown standard)
     * Base rate: 1/24 (Gen 6+)
     * Stages: +1 = 1/8, +2 = 1/2, +3 = 1/1 (guaranteed)
     */
    boolean calculateCriticalHitChance(Map<String, Object> attacker, Map<String, Object> move, Random random) {
        // Moves that always crit
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        if (nameEn.contains("frost breath") || nameEn.contains("storm throw") ||
                nameEn.contains("wicked blow") || nameEn.contains("flower trick")) {
            return true;
        }

        // Calculate crit stage
        int critStage = 0;

        // Check for held items
        String item = heldItem(attacker);
        if ("razor-claw".equals(item) || "scope-lens".equals(item)) {
            critStage += 1;
        }
        // 大葱（大葱鸭/葱游兵）：CT +2
        String species = String.valueOf(attacker.get("name_en"));
        if ("leek".equals(item) && ("farfetchd".equalsIgnoreCase(species) || "sirfetchd".equalsIgnoreCase(species))) {
            critStage += 2;
        }
        // 吉利拳（吉利蛋）：CT +2
        if ("lucky-punch".equals(item) && "chansey".equalsIgnoreCase(species)) {
            critStage += 2;
        }

        // Check for abilities
        String ability = engine.abilityName(attacker);
        if ("super-luck".equalsIgnoreCase(ability)) {
            critStage += 1;
        }

        // Lansat Berry: 会心一击率 +2 阶级（HP < 1/4 时已消耗）
        if (engine.volatileFlag(attacker, "lansatBerryBoosted")) {
            critStage += 2;
        }

        // Check for move crit stage (supports both crit_stage and crit_rate keys)
        int moveCritStage = engine.toInt(move.get("crit_stage"), 0);
        if (moveCritStage == 0) {
            moveCritStage = engine.toInt(move.get("crit_rate"), 0);
        }
        critStage += moveCritStage;

        // Calculate crit chance based on stage
        double critChance = switch (Math.min(critStage, 3)) {
            case 0 -> 1.0 / 24.0; // Base: ~4.17%
            case 1 -> 1.0 / 8.0; // ~12.5%
            case 2 -> 1.0 / 2.0; // 50%
            default -> 1.0; // Guaranteed
        };

        return random.nextDouble() < critChance;
    }

    int typeFactor(int attackingTypeId, int defendingTypeId) {
        Integer factor = typeEfficacyMapper.selectDamageFactor(attackingTypeId, defendingTypeId);
        return factor == null ? 100 : factor;
    }

    double typeModifier(Map<String, Object> defender, int moveTypeId) {
        double modifier = 1.0d;
        for (Map<String, Object> defenderType : engine.activeTypes(defender)) {
            int factor = typeFactor(moveTypeId, engine.toInt(defenderType.get("type_id"), 0));
            modifier *= factor / 100.0d;
        }
        return modifier;
    }

    int modifiedAttackStat(Map<String, Object> mon, Map<String, Object> defender, int baseStat, int damageClassId,
            boolean criticalHit, Map<String, Object> move, Map<String, Object> state) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            int attackStage = hasAbility(defender, "unaware") ? 0 : statStage(mon, "attack");
            if (criticalHit && attackStage < 0) {
                attackStage = 0;
            }
            baseStat = applyStageModifier(baseStat, attackStage);

            // Burn reduces physical attack by 50% (unless has Guts)
            if ("burn".equals(mon.get("condition")) && !"guts".equalsIgnoreCase(engine.abilityName(mon))) {
                baseStat = Math.max(1, (int) Math.floor(baseStat * 0.5d));
            }
        } else if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            int specialAttackStage = hasAbility(defender, "unaware") ? 0 : statStage(mon, "specialAttack");
            if (criticalHit && specialAttackStage < 0) {
                specialAttackStage = 0;
            }
            baseStat = applyStageModifier(baseStat, specialAttackStage);
        }

        // Slow Start: 5 回合内物攻减半
        int slowTurns = engine.toInt(engine.volatileValue(mon, "slowStartTurns", 0), 0);
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL && slowTurns > 0) {
            baseStat = Math.max(1, (int) Math.floor(baseStat * 0.5d));
        }
        // Defeatist: HP≤50% 时攻击减半
        if (engine.hasAbility(mon, "defeatist")) {
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int curHp = engine.toInt(mon.get("currentHp"), 0);
            if (curHp > 0 && curHp * 2 <= maxHp) baseStat = Math.max(1, (int) Math.floor(baseStat * 0.5d));
        }

        // 道具修正（Choice Band/Specs, Light Ball, Thick Club, Deep Sea Tooth, Soul Dew 等）
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        AttackContext ctx = new AttackContext(mon, defender, move, state, moveTypeId, damageClassId, criticalHit);
        baseStat = EffectRegistry.dispatchItemSourceAttack(mon, ctx, baseStat);

        // Flash Fire boost（标记型效果，非道具）
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL
                && Boolean.TRUE.equals(mon.get("flashFireBoost"))) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }

        return baseStat;
    }

    int modifiedDefenseStat(Map<String, Object> attacker, Map<String, Object> mon, int baseStat, int damageClassId,
            Map<String, Object> state, boolean criticalHit, Map<String, Object> move) {
        boolean wonderRoomActive = fieldEffectSupport.wonderRoomTurns(state) > 0;
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            int defenseStage;
            if (wonderRoomActive) {
                // 奇妙空间：物理招式使用特防基础值和特防阶级
                defenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "specialDefense");
            } else {
                defenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "defense");
            }
            // 圣剑：无视防御方防御阶级的正值变化
            if (MoveRegistry.isSacredSword(move) && defenseStage > 0) {
                defenseStage = 0;
            }
            if (criticalHit && defenseStage > 0) {
                defenseStage = 0;
            }
            baseStat = applyStageModifier(baseStat, defenseStage);
        } else if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            int specialDefenseStage;
            if (wonderRoomActive) {
                // 奇妙空间：特殊招式使用物防基础值和物防阶级
                specialDefenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "defense");
            } else {
                specialDefenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "specialDefense");
            }
            if (criticalHit && specialDefenseStage > 0) {
                specialDefenseStage = 0;
            }
            baseStat = applyStageModifier(baseStat, specialDefenseStage);
        }

        // 道具/特性防御修正（Eviolite, Assault Vest, Metal Powder, Deep Sea Scale, Soul Dew, Marvel Scale 等）
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        // 构造上下文时 defender=mon，使 handler 中 ctx.defenderCondition() 正确取到 mon 的状态
        AttackContext ctx = new AttackContext(attacker, mon, move, state, moveTypeId, damageClassId, criticalHit);
        baseStat = EffectRegistry.dispatchTargetDefense(mon, ctx, baseStat);
        baseStat = EffectRegistry.dispatchItemTargetDefense(mon, ctx, baseStat);

        // 沙暴岩石系特防 x1.5 / 雪暴冰系物防 x1.5（天气效果，非特性/道具）
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL
                && fieldEffectSupport.sandTurns(state) > 0
                && targetHasType(mon, DamageCalculatorUtil.TYPE_ROCK)) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                && fieldEffectSupport.snowTurns(state) > 0
                && targetHasType(mon, DamageCalculatorUtil.TYPE_ICE)) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }
        return baseStat;
    }

    private boolean hasAbility(Map<String, Object> mon, String... names) {
        String ability = engine.abilityName(mon);
        for (String name : names) {
            if (name.equalsIgnoreCase(ability)) {
                return true;
            }
        }
        return false;
    }

    /**
     * STAB modifier with Tera support (Pokemon Showdown standard)
     * - Normal STAB: 1.5x
     * - Tera STAB (matches original type): 2.0x
     * - Tera STAB (doesn't match original type): 1.5x
     */
    double stabModifier(Map<String, Object> attacker, int moveTypeId) {
        double stab = 1.0d;

        // Check if move matches any of the attacker's types
        boolean matchesOriginalType = false;
        for (Map<String, Object> attackerType : engine.activeTypes(attacker)) {
            if (engine.toInt(attackerType.get("type_id"), 0) == moveTypeId) {
                matchesOriginalType = true;
                break;
            }
        }

        // Check Tera enhancement
        if (Boolean.TRUE.equals(attacker.get("terastallized"))) {
            int teraTypeId = engine.toInt(attacker.get("teraTypeId"), 0);
            if (teraTypeId == moveTypeId) {
                // Tera STAB: 2.0x if also matches original type, otherwise 1.5x
                stab = matchesOriginalType ? 2.0d : 1.5d;
            }
        } else if (matchesOriginalType) {
            // Normal STAB: 1.5x
            stab = 1.5d;
        }

        // Adaptability ability boosts STAB to 2.0x
        if ("adaptability".equalsIgnoreCase(engine.abilityName(attacker)) && stab > 1.0d) {
            stab = 2.0d;
        }

        return stab;
    }

    /**
     * Spread move modifier for Doubles battles
     * Multi-target moves deal 75% damage in Pokemon Showdown
     */
    double spreadMoveModifier(Map<String, Object> move, Map<String, Object> state) {
        // Check if this is a spread move (targets multiple opponents)
        Integer targetId = engine.toInt(move.get("target_id"), 10);
        // Target IDs: 10 = all adjacent foes, 11 = all adjacent, etc.
        boolean isSpreadMove = (targetId == 10 || targetId == 11 || targetId == 12);

        if (isSpreadMove) {
            // 默认回退值给 2，是为了与双打群攻的常见情况对齐；但真正由上层按存活目标数覆盖时才最准确。
            int spreadTargetCount = engine.toInt(move.get("spreadTargetCount"), 2);
            return spreadTargetCount > 1 ? 0.75d : 1.0d;
        }
        return 1.0d;
    }

    /**
     * Item damage modifier → dispatch + Expert Belt 残留在引擎层
     */
    double itemDamageModifier(Map<String, Object> attacker, Map<String, Object> defender, int moveTypeId,
            Map<String, Object> move, Map<String, Object> state) {
        int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);
        AttackContext ctx = new AttackContext(attacker, defender, move, state, moveTypeId, damageClassId, false);
        double modifier = 1.0;

        // 注册在 EffectRegistry 中的道具：Life Orb, 属性增伤, Muscle Band, Wise Glasses, 石板/卡带/宝石, 物种球, Metronome ...
        modifier = EffectRegistry.dispatchItemSourceDamage(attacker, ctx, modifier);

        // Expert Belt: 克制时 x1.2（需要 typeEfficacyMapper，留在引擎层）
        String item = heldItem(attacker);
        if ("expert-belt".equals(item)) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod > 1.0d) {
                modifier *= 1.2d;
            }
        }

        return modifier;
    }

    /** 石板 → 属性ID（Arceus 制裁光砾用） */
    private static int typeIdFromPlate(String item) {
        return switch (item) {
            case "fist-plate", "fist plate" -> DamageCalculatorUtil.TYPE_FIGHTING;
            case "sky-plate", "sky plate" -> DamageCalculatorUtil.TYPE_FLYING;
            case "poison-plate", "poison plate" -> DamageCalculatorUtil.TYPE_POISON;
            case "earth-plate", "earth plate" -> DamageCalculatorUtil.TYPE_GROUND;
            case "stone-plate", "stone plate" -> DamageCalculatorUtil.TYPE_ROCK;
            case "insect-plate", "insect plate" -> DamageCalculatorUtil.TYPE_BUG;
            case "spooky-plate", "spooky plate" -> DamageCalculatorUtil.TYPE_GHOST;
            case "iron-plate", "iron plate" -> DamageCalculatorUtil.TYPE_STEEL;
            case "flame-plate", "flame plate" -> DamageCalculatorUtil.TYPE_FIRE;
            case "splash-plate", "splash plate" -> DamageCalculatorUtil.TYPE_WATER;
            case "meadow-plate", "meadow plate" -> DamageCalculatorUtil.TYPE_GRASS;
            case "zap-plate", "zap plate" -> DamageCalculatorUtil.TYPE_ELECTRIC;
            case "mind-plate", "mind plate" -> DamageCalculatorUtil.TYPE_PSYCHIC;
            case "icicle-plate", "icicle plate" -> DamageCalculatorUtil.TYPE_ICE;
            case "dragon-plate", "dragon plate" -> DamageCalculatorUtil.TYPE_DRAGON;
            case "dread-plate", "dread plate" -> DamageCalculatorUtil.TYPE_DARK;
            case "pixie-plate", "pixie plate" -> DamageCalculatorUtil.TYPE_FAIRY;
            default -> 0; // 无石板 → Normal（招式原始属性）
        };
    }

    /** 记忆 → 属性ID（Silvally 多属性攻击用） */
    private static int typeIdFromMemory(String item) {
        return switch (item) {
            case "fighting-memory", "fighting memory" -> DamageCalculatorUtil.TYPE_FIGHTING;
            case "flying-memory", "flying memory" -> DamageCalculatorUtil.TYPE_FLYING;
            case "poison-memory", "poison memory" -> DamageCalculatorUtil.TYPE_POISON;
            case "ground-memory", "ground memory" -> DamageCalculatorUtil.TYPE_GROUND;
            case "rock-memory", "rock memory" -> DamageCalculatorUtil.TYPE_ROCK;
            case "bug-memory", "bug memory" -> DamageCalculatorUtil.TYPE_BUG;
            case "ghost-memory", "ghost memory" -> DamageCalculatorUtil.TYPE_GHOST;
            case "steel-memory", "steel memory" -> DamageCalculatorUtil.TYPE_STEEL;
            case "fire-memory", "fire memory" -> DamageCalculatorUtil.TYPE_FIRE;
            case "water-memory", "water memory" -> DamageCalculatorUtil.TYPE_WATER;
            case "grass-memory", "grass memory" -> DamageCalculatorUtil.TYPE_GRASS;
            case "electric-memory", "electric memory" -> DamageCalculatorUtil.TYPE_ELECTRIC;
            case "psychic-memory", "psychic memory" -> DamageCalculatorUtil.TYPE_PSYCHIC;
            case "ice-memory", "ice memory" -> DamageCalculatorUtil.TYPE_ICE;
            case "dragon-memory", "dragon memory" -> DamageCalculatorUtil.TYPE_DRAGON;
            case "dark-memory", "dark memory" -> DamageCalculatorUtil.TYPE_DARK;
            case "fairy-memory", "fairy memory" -> DamageCalculatorUtil.TYPE_FAIRY;
            default -> 0; // 无记忆 → Normal
        };
    }

    /**
     * Ability-based damage modifiers → 全部走 EffectRegistry dispatch
     */
    double abilityDamageModifier(Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> move, int moveTypeId, Map<String, Object> state) {
        int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);
        AttackContext ctx = new AttackContext(attacker, defender, move, state, moveTypeId, damageClassId, false);

        double modifier = 1.0;

        // 攻击方特性伤害修正（Technician, Sheer Force, Hustle, Guts 等）
        modifier = EffectRegistry.dispatchSourceDamage(attacker, ctx, modifier);

        // 光子喷涌/暗影之光：无视防御方特性（免疫类特性如 Levitate、倍率减半等均无效）
        boolean ignoreDefAbilities = MoveRegistry.isUnignorableMove(move);

        // 防御方特性伤害修正（含免疫返回 0，如 Levitate, Sap Sipper, Flash Fire 等）
        if (!ignoreDefAbilities) {
            modifier = EffectRegistry.dispatchTargetDamage(defender, ctx, modifier);
            if (modifier <= 0) return 0;
        }

        // Dark Aura / Fairy Aura: 全场 Dark/Fairy 招式 ×4/3（Aura Break 反转至 ×3/4）
        if ((moveTypeId == EffectRegistry.DARK && hasFieldAbility(state, "dark-aura"))
                || (moveTypeId == EffectRegistry.FAIRY && hasFieldAbility(state, "fairy-aura"))) {
            modifier *= hasFieldAbility(state, "aura-break") ? 0.75 : 4.0 / 3.0;
        }

        // Neuroforce: 效果绝佳时 ×1.25
        if ("neuroforce".equalsIgnoreCase(engine.abilityName(attacker))) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod > 1.0) {
                modifier *= 1.25;
            }
        }

        // Stakeout: 目标本回合换入时伤害 ×2
        if ("stakeout".equalsIgnoreCase(engine.abilityName(attacker))
                && Boolean.TRUE.equals(defender.get("justSwitchedIn"))) {
            modifier *= 2.0;
        }

        return modifier;
    }

    private boolean isRisingVoltage(Map<String, Object> move) {
        return "rising-voltage".equals(String.valueOf(move.get("name_en")))
                || "rising voltage".equals(String.valueOf(move.get("name_en")));
    }

    private boolean isExpandingForce(Map<String, Object> move) {
        return "expanding-force".equals(String.valueOf(move.get("name_en")))
                || "expanding force".equals(String.valueOf(move.get("name_en")));
    }

    private boolean isEarthquake(Map<String, Object> move) {
        String name = String.valueOf(move.get("name_en"));
        return "earthquake".equals(name) || "bulldoze".equals(name) || "magnitude".equals(name);
    }

    int speedValue(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        // 速度值统一在这里收口，供行动排序层直接比较，避免不同模块各自叠倍率。
        int speed = engine.toInt(engine.castMap(mon.get("stats")).get("speed"), 0);
        speed = applyStageModifier(speed, statStage(mon, "speed"));

        // Paralysis reduces speed by 50%
        if ("paralysis".equals(mon.get("condition"))) {
            speed = Math.max(1, speed / 2);
        }

        // 缓慢启动（Slow Start）：5 回合内速度减半
        int slowTurns = engine.toInt(engine.volatileValue(mon, "slowStartTurns", 0), 0);
        if (slowTurns > 0) speed = Math.max(1, (int) Math.floor(speed * 0.5d));

        // 特性速度修正（Swift Swim, Chlorophyll, Sand Rush, Slush Rush, Surge Surfer, Unburden, Quick Feet）
        SpeedContext spCtx = new SpeedContext(mon, state, playerSide);
        speed = EffectRegistry.dispatchSpeed(mon, spCtx, speed);

        // 道具速度修正（Choice Scarf, Iron Ball, Quick Powder, Room Service）
        speed = EffectRegistry.dispatchItemSpeed(mon, spCtx, speed);

        // Tailwind: 2x speed（场地效果，非特性/道具）
        if (fieldEffectSupport.tailwindTurns(state, playerSide) > 0) {
            speed *= 2;
        }

        return speed;
    }

    int applyIncomingDamage(Map<String, Object> attacker, Map<String, Object> target, int damage,
            Map<String, Object> actionLog, List<String> events) {
        return applyIncomingDamage(attacker, target, damage, actionLog, events, null);
    }

    int applyIncomingDamage(Map<String, Object> attacker, Map<String, Object> target, int damage,
            Map<String, Object> actionLog, List<String> events, Map<String, Object> move) {
        int currentHp = engine.toInt(target.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), Math.max(1, currentHp));
        int actualDamage = damage;
        if (!ignoresTargetAbility(attacker, move)
                && "sturdy".equalsIgnoreCase(engine.abilityName(target))
                && currentHp == maxHp && damage >= currentHp) {
            actualDamage = Math.max(0, currentHp - 1);
            actionLog.put("sturdy", true);
            events.add(target.get("name") + " 靠结实撑住了攻击");
        }
        if ("focus-sash".equals(heldItem(target)) && !itemConsumed(target) && currentHp == maxHp
                && damage >= currentHp) {
            actualDamage = Math.max(0, currentHp - 1);
            consumeItem(target);
            events.add(target.get("name") + " 靠气势披带撑住了攻击");
            actionLog.put("focusSash", true);
        }
        // 忍耐：本回合受到致命攻击时保留 1 HP（与结实/气势披带不同，不要求满 HP）
        if (Boolean.TRUE.equals(engine.volatileValue(target, "endured", false)) && damage >= currentHp) {
            actualDamage = Math.max(0, currentHp - 1);
            events.add(target.get("name") + " 靠忍耐撑住了攻击！");
            actionLog.put("endured", true);
        }
        actionLog.put("damage", actualDamage);
        return Math.max(0, currentHp - actualDamage);
    }

    private double fullHpDefenseModifier(Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> move) {
        int currentHp = engine.toInt(defender.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(defender.get("stats")).get("hp"), Math.max(1, currentHp));
        if (currentHp <= 0 || currentHp != maxHp || ignoresTargetAbility(attacker, move)) {
            return 1.0d;
        }
        String ability = engine.abilityName(defender);
        if ("multiscale".equalsIgnoreCase(ability) || "shadow-shield".equalsIgnoreCase(ability)
                || "shadow shield".equalsIgnoreCase(ability)) {
            return 0.5d;
        }
        return 1.0d;
    }

    /** 无视目标特性：Mold Breaker 系列特性 或 特定无视特性招式 */
    private boolean ignoresTargetAbility(Map<String, Object> attacker) {
        return hasAbility(attacker, "mold-breaker", "mold breaker", "teravolt", "turboblaze");
    }

    /** 无视目标特性（带招式检测：如光子喷涌/暗影之光无视目标特性） */
    private boolean ignoresTargetAbility(Map<String, Object> attacker, Map<String, Object> move) {
        return ignoresTargetAbility(attacker) || (move != null && MoveRegistry.isUnignorableMove(move));
    }

    double weatherDamageModifier(Map<String, Object> attacker, Map<String, Object> state, int moveTypeId) {
        // Utility Umbrella: ignore weather effects
        if ("utility-umbrella".equalsIgnoreCase(engine.heldItem(attacker))
                || "utility umbrella".equalsIgnoreCase(engine.heldItem(attacker))) {
            return 1.0d;
        }
        if (fieldEffectSupport.rainTurns(state) > 0) {
            if (moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
                return 1.5d;
            }
            if (moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
                return 0.5d;
            }
        }
        if (fieldEffectSupport.sunTurns(state) > 0) {
            if (moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
                return 1.5d;
            }
            if (moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
                return 0.5d;
            }
        }
        return 1.0d;
    }

    double terrainDamageModifier(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> move, int moveTypeId) {
        if (!isGrounded(attacker)) {
            return fieldEffectSupport.mistyTerrainTurns(state) > 0
                    && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON
                    && isGrounded(defender)
                            ? 0.5d
                            : 1.0d;
        }
        if (fieldEffectSupport.electricTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return isRisingVoltage(move) ? 2.0d : 1.3d;
        }
        if (fieldEffectSupport.psychicTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC) {
            return isExpandingForce(move) ? 2.0d : 1.3d;
        }
        if (fieldEffectSupport.grassyTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            if (isEarthquake(move)) return 0.5d;
            return 1.3d;
        }
        if (fieldEffectSupport.mistyTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON
                && isGrounded(defender)) {
            return 0.5d;
        }
        return 1.0d;
    }

    double screenDamageModifier(Map<String, Object> state, Map<String, Object> attacker,
            Map<String, Object> defender, int damageClassId) {
        // Infiltrator: 无视反射壁/光墙/极光幕
        if (hasAbility(attacker, "infiltrator")) {
            return 1.0d;
        }
        boolean playerSide = isOnSide(state, defender, true);
        if (fieldEffectSupport.auroraVeilTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                && fieldEffectSupport.reflectTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL
                && fieldEffectSupport.lightScreenTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        return 1.0d;
    }

    int applyStageModifier(int baseStat, int stage) {
        int normalized = Math.max(-6, Math.min(6, stage));
        double multiplier = normalized >= 0
                ? (2.0d + normalized) / 2.0d
                : 2.0d / (2.0d - normalized);
        return Math.max(1, (int) Math.floor(baseStat * multiplier));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> statStages(Map<String, Object> mon) {
        Object value = mon.get("statStages");
        if (value instanceof Map) {
            Map<String, Object> existing = (Map<String, Object>) value;
            existing.putIfAbsent("attack", 0);
            existing.putIfAbsent("defense", 0);
            existing.putIfAbsent("specialAttack", 0);
            existing.putIfAbsent("specialDefense", 0);
            existing.putIfAbsent("speed", 0);
            return existing;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("attack", 0);
        created.put("defense", 0);
        created.put("specialAttack", 0);
        created.put("specialDefense", 0);
        created.put("speed", 0);
        mon.put("statStages", created);
        return created;
    }

    int statStage(Map<String, Object> mon, String stat) {
        return engine.toInt(statStages(mon).get(stat), 0);
    }

    private String heldItem(Map<String, Object> mon) {
        Object item = mon.get("heldItem");
        return item == null ? "" : String.valueOf(item);
    }

    private boolean itemConsumed(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("itemConsumed"));
    }

    private void consumeItem(Map<String, Object> mon) {
        mon.put("itemConsumed", true);
        mon.put("heldItem", "");
    }

    /**
     * 获取宝可梦的当前重量（已考虑重金属/轻金属/浮石修正）。
     * 用于重磅冲撞/高温重压等依赖重量的招式伤害计算。
     */
    int getWeight(Map<String, Object> mon) {
        int baseWeight = engine.toInt(mon.get("weight"), engine.toInt(mon.get("weight_kg"), 100));
        WeightContext wCtx = new WeightContext(mon);
        baseWeight = EffectRegistry.dispatchWeight(mon, wCtx, baseWeight);
        baseWeight = EffectRegistry.dispatchItemWeight(mon, wCtx, baseWeight);
        return Math.max(1, baseWeight);
    }

    private boolean targetHasType(Map<String, Object> target, int typeId) {
        for (Map<String, Object> type : engine.activeTypes(target)) {
            if (engine.toInt(type.get("type_id"), 0) == typeId) {
                return true;
            }
        }
        return false;
    }

    private boolean isGrounded(Map<String, Object> mon) {
        return engine.isGrounded(mon);
    }

    /** 检查场上是否有活跃宝可梦拥有指定特性（双打适用） */
    private boolean hasFieldAbility(Map<String, Object> state, String ability) {
        if (state == null) return false;
        for (boolean isPlayer : new boolean[]{true, false}) {
            List<Map<String, Object>> team = engine.team(state, isPlayer);
            List<Integer> activeSlots = engine.activeSlots(state, isPlayer);
            for (Integer slot : activeSlots) {
                if (slot != null && slot >= 0 && slot < team.size()) {
                    if (ability.equalsIgnoreCase(engine.abilityName(team.get(slot)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSide(Map<String, Object> state, Map<String, Object> mon, boolean playerSide) {
        for (Map<String, Object> candidate : engine.team(state, playerSide)) {
            if (candidate == mon) {
                return true;
            }
        }
        return false;
    }

    /**
     * Partner ability modifiers for Doubles battles
     * - Friend Guard: 0.75x damage to ally
     * - Battery: 1.3x special attack from ally
     */
    double partnerAbilityModifier(Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> state, int damageClassId) {
        double modifier = 1.0d;

        // Determine which side the defender is on
        boolean defenderIsPlayer = engine.isOnSide(state, defender, true);
        List<Map<String, Object>> defenderTeam = engine.team(state, defenderIsPlayer);
        List<Integer> activeSlots = engine.activeSlots(state, defenderIsPlayer);

        // Find defender's index in team
        int defenderIndex = -1;
        for (int i = 0; i < defenderTeam.size(); i++) {
            if (defenderTeam.get(i) == defender) {
                defenderIndex = i;
                break;
            }
        }

        // Check defender's partner for Friend Guard
        for (Integer slot : activeSlots) {
            if (slot != null && slot >= 0 && slot < defenderTeam.size() && slot != defenderIndex) {
                Map<String, Object> partner = defenderTeam.get(slot);
                String partnerAbility = engine.abilityName(partner).toLowerCase();

                // Friend Guard: Reduces damage taken by ally by 25%
                if ("friend-guard".equalsIgnoreCase(partnerAbility)) {
                    modifier *= 0.75d;
                }
            }
        }

        // Check attacker's partner for Battery
        boolean attackerIsPlayer = engine.isOnSide(state, attacker, true);
        List<Map<String, Object>> attackerTeam = engine.team(state, attackerIsPlayer);
        List<Integer> attackerActiveSlots = engine.activeSlots(state, attackerIsPlayer);

        int attackerIndex = -1;
        for (int i = 0; i < attackerTeam.size(); i++) {
            if (attackerTeam.get(i) == attacker) {
                attackerIndex = i;
                break;
            }
        }

        for (Integer slot : attackerActiveSlots) {
            if (slot != null && slot >= 0 && slot < attackerTeam.size() && slot != attackerIndex) {
                Map<String, Object> partner = attackerTeam.get(slot);
                String partnerAbility = engine.abilityName(partner).toLowerCase();

                // Battery: Boosts ally's special attacks by 30%
                if ("battery".equalsIgnoreCase(partnerAbility) &&
                        damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
                    modifier *= 1.3d;
                }
                // Power Spot: Boosts ally's attacks by 10%
                if ("power-spot".equalsIgnoreCase(partnerAbility) || "power spot".equalsIgnoreCase(partnerAbility)) {
                    modifier *= 1.1d;
                }
            }
        }

        // Plus/Minus: 队友有对应特性时特攻 x1.5
        String aa = engine.abilityName(attacker);
        if (("plus".equalsIgnoreCase(aa) || "minus".equalsIgnoreCase(aa))
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            boolean attackerIsPlus = "plus".equalsIgnoreCase(aa);
            for (Integer slot : attackerActiveSlots) {
                if (slot != null && slot >= 0 && slot < attackerTeam.size() && slot != attackerIndex) {
                    Map<String, Object> partner = attackerTeam.get(slot);
                    String pa = engine.abilityName(partner).toLowerCase();
                    if ((attackerIsPlus && "minus".equalsIgnoreCase(pa))
                            || (!attackerIsPlus && "plus".equalsIgnoreCase(pa))) {
                        modifier *= 1.5d;
                        break;
                    }
                }
            }
        }

        return modifier;
    }
}
