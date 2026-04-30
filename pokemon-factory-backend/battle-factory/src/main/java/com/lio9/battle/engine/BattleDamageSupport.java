package com.lio9.battle.engine;

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

        Map<String, Object> attackerStats = engine.castMap(attacker.get("stats"));
        Map<String, Object> defenderStats = engine.castMap(defender.get("stats"));

        int attackStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? modifiedAttackStat(attacker, defender, engine.toInt(attackerStats.get("attack"), 100), damageClassId,
                        criticalHit)
                : modifiedAttackStat(attacker, defender, engine.toInt(attackerStats.get("specialAttack"), 100),
                        damageClassId, criticalHit);
        int baseDefenseStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? engine.toInt(defenderStats.get("defense"), 100)
                : engine.toInt(defenderStats.get("specialDefense"), 100);
        int defenseStat = Math.max(1,
                modifiedDefenseStat(attacker, defender, baseDefenseStat, damageClassId, state, criticalHit));

        int power = Math.max(1, engine.toInt(move.get("power"), 1));
        int baseDamage = DamageCalculatorUtil.calculateBaseDamage(level, power, attackStat, defenseStat);

        double modifier = 1.0d;
        int moveTypeId = engine.toInt(move.get("type_id"), 0);

        // STAB (Same Type Attack Bonus) - Pokemon Showdown standard
        modifier *= stabModifier(attacker, moveTypeId);

        // Type effectiveness
        double typeModifier = typeModifier(defender, moveTypeId);
        modifier *= typeModifier;
        if (typeModifier <= 0.0d) {
            return 0;
        }

        // Item modifiers
        modifier *= itemDamageModifier(attacker, defender, moveTypeId, move, state);

        // Ability modifiers
        modifier *= abilityDamageModifier(attacker, defender, move, moveTypeId);

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

        // Critical hit multiplier
        if (criticalHit) {
            // Sniper ability increases crit multiplier from 1.5x to 2.25x
            modifier *= "sniper".equalsIgnoreCase(engine.abilityName(attacker)) ? 2.25d : 1.5d;
        }

        // Other modifiers
        modifier *= fullHpDefenseModifier(attacker, defender);
        modifier *= weatherDamageModifier(state, moveTypeId);
        modifier *= terrainDamageModifier(state, attacker, defender, moveTypeId);
        modifier *= screenDamageModifier(state, defender, damageClassId);

        // 群攻修正必须在最终乘区统一处理，才能和 Helping Hand、天气、屏障等倍率保持同一链路。
        modifier *= spreadMoveModifier(move, state);

        // Partner ability modifiers (Friend Guard, Battery)
        modifier *= partnerAbilityModifier(attacker, defender, state, damageClassId);

        // Random factor (0.85 - 1.00) - Pokemon Showdown standard
        modifier *= (0.85d + (random.nextDouble() * 0.15d));

        return Math.max(1, (int) Math.floor(baseDamage * modifier));
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

        // Check for abilities
        String ability = engine.abilityName(attacker);
        if ("super-luck".equalsIgnoreCase(ability)) {
            critStage += 1;
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
            boolean criticalHit) {
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

        String item = heldItem(mon);
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL && "choice-band".equals(item)) {
            return (int) Math.floor(baseStat * 1.5d);
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && "choice-specs".equals(item)) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }

        // Flash Fire boost
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL
                && Boolean.TRUE.equals(mon.get("flashFireBoost"))) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }

        // Solar Power: 1.5x special attack in sun (but loses HP each turn - handled
        // elsewhere)
        String ability = engine.abilityName(mon);
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL &&
                "solar-power".equalsIgnoreCase(ability) && fieldEffectSupport.sunTurns(null) > 0) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }

        return baseStat;
    }

    int modifiedDefenseStat(Map<String, Object> attacker, Map<String, Object> mon, int baseStat, int damageClassId,
            Map<String, Object> state, boolean criticalHit) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            int defenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "defense");
            if (criticalHit && defenseStage > 0) {
                defenseStage = 0;
            }
            baseStat = applyStageModifier(baseStat, defenseStage);
        } else if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            int specialDefenseStage = hasAbility(attacker, "unaware") ? 0 : statStage(mon, "specialDefense");
            if (criticalHit && specialDefenseStage > 0) {
                specialDefenseStage = 0;
            }
            baseStat = applyStageModifier(baseStat, specialDefenseStage);
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && "assault-vest".equals(heldItem(mon))) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }
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
     * Comprehensive item damage modifier (Pokemon Showdown standard)
     */
    double itemDamageModifier(Map<String, Object> attacker, Map<String, Object> defender, int moveTypeId,
            Map<String, Object> move, Map<String, Object> state) {
        String item = heldItem(attacker);
        double modifier = 1.0d;

        // === Damage Boosting Items ===

        // Life Orb: 1.3x all damage
        if ("life-orb".equals(item)) {
            modifier *= 1.3d;
        }

        // Choice Band/Specs: 1.5x physical/special damage (already applied in stat
        // calculation)
        // Note: These are handled in modifiedAttackStat

        // Type-boosting items: 1.2x for matching type
        modifier *= switch (item) {
            case "mystic-water", "sea-incense" -> moveTypeId == DamageCalculatorUtil.TYPE_WATER ? 1.2d : 1.0d;
            case "charcoal", "heat-rock" -> moveTypeId == DamageCalculatorUtil.TYPE_FIRE ? 1.2d : 1.0d;
            case "miracle-seed", "rose-incense" -> moveTypeId == DamageCalculatorUtil.TYPE_GRASS ? 1.2d : 1.0d;
            case "never-melt-ice" -> moveTypeId == DamageCalculatorUtil.TYPE_ICE ? 1.2d : 1.0d;
            case "black-belt", "fighting-gem" -> moveTypeId == DamageCalculatorUtil.TYPE_FIGHTING ? 1.2d : 1.0d;
            case "poison-barb", "black-sludge" -> moveTypeId == DamageCalculatorUtil.TYPE_POISON ? 1.2d : 1.0d;
            case "soft-sand" -> moveTypeId == DamageCalculatorUtil.TYPE_GROUND ? 1.2d : 1.0d;
            case "sharp-beak" -> moveTypeId == DamageCalculatorUtil.TYPE_FLYING ? 1.2d : 1.0d;
            case "twisted-spoon", "odd-incense" -> moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC ? 1.2d : 1.0d;
            case "silver-powder" -> moveTypeId == DamageCalculatorUtil.TYPE_BUG ? 1.2d : 1.0d;
            case "hard-stone", "rock-incense" -> moveTypeId == DamageCalculatorUtil.TYPE_ROCK ? 1.2d : 1.0d;
            case "spell-tag" -> moveTypeId == DamageCalculatorUtil.TYPE_GHOST ? 1.2d : 1.0d;
            case "dragon-fang", "dragon-scale" -> moveTypeId == DamageCalculatorUtil.TYPE_DRAGON ? 1.2d : 1.0d;
            case "black-glasses" -> moveTypeId == DamageCalculatorUtil.TYPE_DARK ? 1.2d : 1.0d;
            case "metal-coat", "steel-incense" -> moveTypeId == DamageCalculatorUtil.TYPE_STEEL ? 1.2d : 1.0d;
            case "silk-scarf" -> moveTypeId == DamageCalculatorUtil.TYPE_NORMAL ? 1.2d : 1.0d;
            case "magnet" -> moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC ? 1.2d : 1.0d;
            default -> 1.0d;
        };

        // Type Gem: 1.3x for first use of matching type move (consumed after use)
        // Would need to track gem usage
        modifier *= switch (item) {
            case "fire-gem" -> moveTypeId == DamageCalculatorUtil.TYPE_FIRE && !itemConsumed(attacker) ? 1.3d : 1.0d;
            case "water-gem" -> moveTypeId == DamageCalculatorUtil.TYPE_WATER && !itemConsumed(attacker) ? 1.3d : 1.0d;
            case "electric-gem" ->
                moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC && !itemConsumed(attacker) ? 1.3d : 1.0d;
            case "grass-gem" -> moveTypeId == DamageCalculatorUtil.TYPE_GRASS && !itemConsumed(attacker) ? 1.3d : 1.0d;
            case "fighting-gem" ->
                moveTypeId == DamageCalculatorUtil.TYPE_FIGHTING && !itemConsumed(attacker) ? 1.3d : 1.0d;
            default -> 1.0d;
        };

        // Expert Belt: 1.2x for super effective moves
        if ("expert-belt".equals(item)) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod > 1.0d) {
                modifier *= 1.2d;
            }
        }

        // Muscle Band: 1.1x for physical moves
        if ("muscle-band".equals(item)) {
            int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);
            if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
                modifier *= 1.1d;
            }
        }

        // Wise Glasses: 1.1x for special moves
        if ("wise-glasses".equals(item)) {
            int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL);
            if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
                modifier *= 1.1d;
            }
        }

        // Plate items (for Arceus): 1.2x for matching type
        if (item.endsWith("-plate")) {
            int plateType = getPlateType(item);
            if (plateType == moveTypeId) {
                modifier *= 1.2d;
            }
        }

        // Memory items (for Silvally): 1.2x for matching type
        if (item.endsWith("-memory")) {
            int memoryType = getMemoryType(item);
            if (memoryType == moveTypeId) {
                modifier *= 1.2d;
            }
        }

        return modifier;
    }

    /**
     * Get type ID from plate item name
     */
    private int getPlateType(String plateName) {
        return switch (plateName) {
            case "flame-plate" -> DamageCalculatorUtil.TYPE_FIRE;
            case "splash-plate" -> DamageCalculatorUtil.TYPE_WATER;
            case "zap-plate" -> DamageCalculatorUtil.TYPE_ELECTRIC;
            case "meadow-plate" -> DamageCalculatorUtil.TYPE_GRASS;
            case "icicle-plate" -> DamageCalculatorUtil.TYPE_ICE;
            case "fist-plate" -> DamageCalculatorUtil.TYPE_FIGHTING;
            case "toxic-plate" -> DamageCalculatorUtil.TYPE_POISON;
            case "earth-plate" -> DamageCalculatorUtil.TYPE_GROUND;
            case "sky-plate" -> DamageCalculatorUtil.TYPE_FLYING;
            case "mind-plate" -> DamageCalculatorUtil.TYPE_PSYCHIC;
            case "insect-plate" -> DamageCalculatorUtil.TYPE_BUG;
            case "stone-plate" -> DamageCalculatorUtil.TYPE_ROCK;
            case "spooky-plate" -> DamageCalculatorUtil.TYPE_GHOST;
            case "draco-plate" -> DamageCalculatorUtil.TYPE_DRAGON;
            case "dread-plate" -> DamageCalculatorUtil.TYPE_DARK;
            case "iron-plate" -> DamageCalculatorUtil.TYPE_STEEL;
            default -> DamageCalculatorUtil.TYPE_NORMAL;
        };
    }

    /**
     * Get type ID from memory item name
     */
    private int getMemoryType(String memoryName) {
        return switch (memoryName) {
            case "fire-memory" -> DamageCalculatorUtil.TYPE_FIRE;
            case "water-memory" -> DamageCalculatorUtil.TYPE_WATER;
            case "electric-memory" -> DamageCalculatorUtil.TYPE_ELECTRIC;
            case "grass-memory" -> DamageCalculatorUtil.TYPE_GRASS;
            case "ice-memory" -> DamageCalculatorUtil.TYPE_ICE;
            case "fighting-memory" -> DamageCalculatorUtil.TYPE_FIGHTING;
            case "poison-memory" -> DamageCalculatorUtil.TYPE_POISON;
            case "ground-memory" -> DamageCalculatorUtil.TYPE_GROUND;
            case "flying-memory" -> DamageCalculatorUtil.TYPE_FLYING;
            case "psychic-memory" -> DamageCalculatorUtil.TYPE_PSYCHIC;
            case "bug-memory" -> DamageCalculatorUtil.TYPE_BUG;
            case "rock-memory" -> DamageCalculatorUtil.TYPE_ROCK;
            case "ghost-memory" -> DamageCalculatorUtil.TYPE_GHOST;
            case "dragon-memory" -> DamageCalculatorUtil.TYPE_DRAGON;
            case "dark-memory" -> DamageCalculatorUtil.TYPE_DARK;
            case "steel-memory" -> DamageCalculatorUtil.TYPE_STEEL;
            default -> DamageCalculatorUtil.TYPE_NORMAL;
        };
    }

    /**
     * Ability-based damage modifiers (Pokemon Showdown standard)
     */
    double abilityDamageModifier(Map<String, Object> attacker, Map<String, Object> defender,
            Map<String, Object> move, int moveTypeId) {
        double modifier = 1.0d;
        String attackerAbility = engine.abilityName(attacker);
        String defenderAbility = engine.abilityName(defender);
        int power = engine.toInt(move.get("power"), 0);
        int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);

        // === Attacker Ability Boosts ===

        // Technician: 1.5x boost for moves with base power <= 60
        if ("technician".equalsIgnoreCase(attackerAbility) && power > 0 && power <= 60) {
            modifier *= 1.5d;
        }

        // Sheer Force: 1.3x for moves with secondary effects (removes them)
        if ("sheer-force".equalsIgnoreCase(attackerAbility) && hasSecondaryEffect(move)) {
            modifier *= 1.3d;
        }

        // Iron Fist: 1.2x for punching moves
        if ("iron-fist".equalsIgnoreCase(attackerAbility) && isPunchingMove(move)) {
            modifier *= 1.2d;
        }

        // Reckless: 1.2x for recoil/crash moves
        if ("reckless".equalsIgnoreCase(attackerAbility) && hasRecoil(move)) {
            modifier *= 1.2d;
        }

        // Sand Force: 1.3x for Rock/Ground/Steel moves in sandstorm
        if ("sand-force".equalsIgnoreCase(attackerAbility) && fieldEffectSupport.sandTurns(null) > 0) {
            if (moveTypeId == DamageCalculatorUtil.TYPE_ROCK ||
                    moveTypeId == DamageCalculatorUtil.TYPE_GROUND ||
                    moveTypeId == DamageCalculatorUtil.TYPE_STEEL) {
                modifier *= 1.3d;
            }
        }

        // Hustle: 1.5x physical attack but lower accuracy
        if ("hustle".equalsIgnoreCase(attackerAbility) && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            modifier *= 1.5d;
        }

        // Flare Boost: 1.5x special attack when burned
        if ("flare-boost".equalsIgnoreCase(attackerAbility) && "burn".equals(attacker.get("condition"))
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            modifier *= 1.5d;
        }

        // Toxic Boost: 1.5x physical attack when poisoned
        if ("toxic-boost".equalsIgnoreCase(attackerAbility) &&
                ("poison".equals(attacker.get("condition")) || "toxic".equals(attacker.get("condition")))
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            modifier *= 1.5d;
        }

        // Guts: 1.5x physical attack when statused
        if ("guts".equalsIgnoreCase(attackerAbility) && attacker.get("condition") != null
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            modifier *= 1.5d;
        }

        // Analytic: 1.3x if moving last
        if ("analytic".equalsIgnoreCase(attackerAbility)) {
            // Would check if moving last in turn order
            modifier *= 1.3d;
        }

        // Tinted Lens: 2x for not very effective moves
        if ("tinted-lens".equalsIgnoreCase(attackerAbility)) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod < 1.0d && typeMod > 0.0d) {
                modifier *= 2.0d;
            }
        }

        // Normalize: All moves become Normal-type and get 1.2x boost
        if ("normalize".equalsIgnoreCase(attackerAbility) && moveTypeId != DamageCalculatorUtil.TYPE_NORMAL) {
            modifier *= 1.2d;
        }

        // Aerilate/Pixilate/Refrigerate/Galvanize: -1 type moves get boosted
        if (("aerilate".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_NORMAL) ||
                ("pixilate".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_NORMAL) ||
                ("refrigerate".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_NORMAL) ||
                ("galvanize".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_NORMAL)) {
            modifier *= 1.2d;
        }

        // Strong Jaw: 1.5x for biting moves
        if ("strong-jaw".equalsIgnoreCase(attackerAbility) && isBitingMove(move)) {
            modifier *= 1.5d;
        }

        // Mega Launcher: 1.5x for aura/pulse moves
        if ("mega-launcher".equalsIgnoreCase(attackerAbility) && isPulseMove(move)) {
            modifier *= 1.5d;
        }

        // Steely Spirit: 1.5x for Steel moves
        if ("steely-spirit".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_STEEL) {
            modifier *= 1.5d;
        }

        // Punk Rock: 1.3x for sound-based moves
        if ("punk-rock".equalsIgnoreCase(attackerAbility) && isSoundMove(move)) {
            modifier *= 1.3d;
        }

        // Transistor: 1.3x for Electric moves
        if ("transistor".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            modifier *= 1.3d;
        }

        // Dragon's Maw: 1.5x for Dragon moves
        if ("dragons-maw".equalsIgnoreCase(attackerAbility) && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON) {
            modifier *= 1.5d;
        }

        // === Defender Ability Reductions ===

        // Thick Fat: 0.5x for Fire/Ice moves
        if ("thick-fat".equalsIgnoreCase(defenderAbility) &&
                (moveTypeId == DamageCalculatorUtil.TYPE_FIRE || moveTypeId == DamageCalculatorUtil.TYPE_ICE)) {
            modifier *= 0.5d;
        }

        // Heatproof: 0.5x for Fire moves
        if ("heatproof".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            modifier *= 0.5d;
        }

        // Water Bubble: 0.5x for Fire moves
        if ("water-bubble".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            modifier *= 0.5d;
        }

        // Dry Skin: 1.25x for Fire moves
        if ("dry-skin".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            modifier *= 1.25d;
        }

        // Filter/Solid Rock: 0.75x for super effective moves
        if (("filter".equalsIgnoreCase(defenderAbility) || "solid-rock".equalsIgnoreCase(defenderAbility))) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod > 1.0d) {
                modifier *= 0.75d;
            }
        }

        // Prism Armor: 0.75x for super effective moves
        if ("prism-armor".equalsIgnoreCase(defenderAbility)) {
            double typeMod = typeModifier(defender, moveTypeId);
            if (typeMod > 1.0d) {
                modifier *= 0.75d;
            }
        }

        // Ice Scales: 0.5x for special moves
        if ("ice-scales".equalsIgnoreCase(defenderAbility) &&
                damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            modifier *= 0.5d;
        }

        // Fur Coat: 0.5x for physical moves
        if ("fur-coat".equalsIgnoreCase(defenderAbility) &&
                damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            modifier *= 0.5d;
        }

        // Friend Guard: 0.75x in doubles (ally has this ability)
        // Would need to check ally abilities

        // Sap Sipper: Immune to Grass, boosts Attack
        if ("sap-sipper".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            return 0.0d; // Immune
        }

        // Storm Drain: Immune to Water, boosts Sp. Atk
        if ("storm-drain".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            return 0.0d; // Immune
        }

        // Lightning Rod/Volt Absorb: Immune to Electric
        if (("lightning-rod".equalsIgnoreCase(defenderAbility) || "volt-absorb".equalsIgnoreCase(defenderAbility))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return 0.0d; // Immune
        }

        // Water Absorb/Dry Skin: Immune to Water, heals
        if (("water-absorb".equalsIgnoreCase(defenderAbility) || "dry-skin".equalsIgnoreCase(defenderAbility))
                && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            return 0.0d; // Immune
        }

        // Flash Fire: Immune to Fire (already handled in stat calculation)
        if ("flash-fire".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            return 0.0d; // Immune
        }

        // Motor Drive: Immune to Electric, boosts Speed
        if ("motor-drive".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return 0.0d; // Immune
        }

        // Well-Baked Body: Immune to Fire, boosts Defense
        if ("well-baked-body".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            return 0.0d; // Immune
        }

        // Earth Eater: Immune to Ground, heals
        if ("earth-eater".equalsIgnoreCase(defenderAbility) && moveTypeId == DamageCalculatorUtil.TYPE_GROUND) {
            return 0.0d; // Immune
        }

        // Wind Rider: Immune to wind moves, boosts Attack
        if ("wind-rider".equalsIgnoreCase(defenderAbility) && isWindMove(move)) {
            return 0.0d; // Immune
        }

        // Sharpness: slicing moves get 1.5x boost
        if ("sharpness".equalsIgnoreCase(attackerAbility) && isSlicingMove(move)) {
            modifier *= 1.5d;
        }

        // Purifying Salt: Ghost resistance (takes 1/2 damage from Ghost)
        if (("purifying-salt".equalsIgnoreCase(defenderAbility) || "purifying salt".equalsIgnoreCase(defenderAbility))
                && moveTypeId == DamageCalculatorUtil.TYPE_GHOST) {
            modifier *= 0.5d;
        }

        // Sword of Ruin: Defender's Defense reduced by 25%
        if (("sword-of-ruin".equalsIgnoreCase(defenderAbility) || "sword of ruin".equalsIgnoreCase(defenderAbility))
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            modifier /= 0.75d; // reverse the actual 0.75x def multiplier
        }

        // Beads of Ruin: Defender's SpD reduced by 25%
        if (("beads-of-ruin".equalsIgnoreCase(defenderAbility) || "beads of ruin".equalsIgnoreCase(defenderAbility))
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            modifier /= 0.75d;
        }

        return modifier;
    }

    private boolean hasSecondaryEffect(Map<String, Object> move) {
        // Check if move has additional effects (status, stat changes, etc.)
        Integer effectChance = engine.toInt(move.get("effect_chance"), 0);
        return effectChance > 0;
    }

    private boolean isPunchingMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("punch") || nameEn.contains("hammer");
    }

    private boolean hasRecoil(Map<String, Object> move) {
        // Check for recoil moves (would check move flags in full implementation)
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("double-edge") || nameEn.contains("flare blitz") ||
                nameEn.contains("wood hammer") || nameEn.contains("head smash") ||
                nameEn.contains("brave bird") || nameEn.contains("take down");
    }

    private boolean isBitingMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("bite") || nameEn.contains("crunch") || nameEn.contains("fire fang") ||
                nameEn.contains("ice fang") || nameEn.contains("thunder fang") || nameEn.contains("poison fang");
    }

    private boolean isPulseMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("pulse") || nameEn.contains("aura sphere") || nameEn.contains("dragon pulse");
    }

    private boolean isSoundMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("boomburst") || nameEn.contains("hypervoice") || nameEn.contains("bug buzz") ||
                nameEn.contains("snarl") || nameEn.contains("overdrive") || nameEn.contains("clang");
    }

    private boolean isSlicingMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("slash") || nameEn.contains("cut") || nameEn.contains("blade")
                || nameEn.contains("razor") || nameEn.contains("claw") || nameEn.contains("axe")
                || nameEn.contains("leaf") || nameEn.contains("night") || nameEn.contains("psycho")
                || nameEn.contains("cross") || nameEn.contains("slic") || nameEn.contains("karate");
    }

    private boolean isWindMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return nameEn.contains("gust") || nameEn.contains("twister") || nameEn.contains("hurricane") ||
                nameEn.contains("bleakwind") || nameEn.contains("springtide") || nameEn.contains("wildbolt");
    }

    int speedValue(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        // 速度值统一在这里收口，供行动排序层直接比较，避免不同模块各自叠倍率。
        int speed = engine.toInt(engine.castMap(mon.get("stats")).get("speed"), 0);
        speed = applyStageModifier(speed, statStage(mon, "speed"));

        // Paralysis reduces speed by 50%
        if ("paralysis".equals(mon.get("condition"))) {
            speed = Math.max(1, speed / 2);
        }

        // Choice Scarf: 1.5x speed
        if ("choice-scarf".equals(heldItem(mon))) {
            speed = (int) Math.floor(speed * 1.5d);
        }

        // Tailwind: 2x speed
        if (fieldEffectSupport.tailwindTurns(state, playerSide) > 0) {
            speed *= 2;
        }

        // Weather-based speed abilities
        String ability = engine.abilityName(mon);
        if (fieldEffectSupport.rainTurns(state) > 0 && "swift-swim".equalsIgnoreCase(ability)) {
            speed *= 2; // Swift Swim doubles speed in rain
        }
        if (fieldEffectSupport.sunTurns(state) > 0 && "chlorophyll".equalsIgnoreCase(ability)) {
            speed *= 2; // Chlorophyll doubles speed in sun
        }
        if (fieldEffectSupport.sandTurns(state) > 0 && "sand-rush".equalsIgnoreCase(ability)) {
            speed *= 2; // Sand Rush doubles speed in sandstorm
        }
        if (fieldEffectSupport.snowTurns(state) > 0 && "slush-rush".equalsIgnoreCase(ability)) {
            speed *= 2; // Slush Rush doubles speed in snow/hail
        }

        // Terrain-based speed (Surge Surfer in Electric Terrain)
        if (fieldEffectSupport.electricTerrainTurns(state) > 0 && "surge-surfer".equalsIgnoreCase(ability)) {
            speed *= 2; // Surge Surfer doubles speed in Electric Terrain
        }

        // Unburden: doubles speed after the Pokemon loses/consumes its held item.
        if ("unburden".equalsIgnoreCase(ability) && Boolean.TRUE.equals(mon.get("unburdenActive"))) {
            speed *= 2;
        }

        return speed;
    }

    int applyIncomingDamage(Map<String, Object> attacker, Map<String, Object> target, int damage,
            Map<String, Object> actionLog, List<String> events) {
        int currentHp = engine.toInt(target.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), Math.max(1, currentHp));
        int actualDamage = damage;
        if (!ignoresTargetAbility(attacker)
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
        actionLog.put("damage", actualDamage);
        return Math.max(0, currentHp - actualDamage);
    }

    private double fullHpDefenseModifier(Map<String, Object> attacker, Map<String, Object> defender) {
        int currentHp = engine.toInt(defender.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(defender.get("stats")).get("hp"), Math.max(1, currentHp));
        if (currentHp <= 0 || currentHp != maxHp || ignoresTargetAbility(attacker)) {
            return 1.0d;
        }
        String ability = engine.abilityName(defender);
        if ("multiscale".equalsIgnoreCase(ability) || "shadow-shield".equalsIgnoreCase(ability)
                || "shadow shield".equalsIgnoreCase(ability)) {
            return 0.5d;
        }
        return 1.0d;
    }

    private boolean ignoresTargetAbility(Map<String, Object> attacker) {
        return hasAbility(attacker, "mold-breaker", "mold breaker", "teravolt", "turboblaze");
    }

    double weatherDamageModifier(Map<String, Object> state, int moveTypeId) {
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
            int moveTypeId) {
        if (!isGrounded(attacker)) {
            return fieldEffectSupport.mistyTerrainTurns(state) > 0
                    && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON
                    && isGrounded(defender)
                            ? 0.5d
                            : 1.0d;
        }
        if (fieldEffectSupport.electricTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return 1.3d;
        }
        if (fieldEffectSupport.psychicTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC) {
            return 1.3d;
        }
        if (fieldEffectSupport.grassyTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            return 1.3d;
        }
        if (fieldEffectSupport.mistyTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON
                && isGrounded(defender)) {
            return 0.5d;
        }
        return 1.0d;
    }

    double screenDamageModifier(Map<String, Object> state, Map<String, Object> defender, int damageClassId) {
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
            }
        }

        return modifier;
    }
}
