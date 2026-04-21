package com.lio9.battle.engine;

import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleDamageSupport {
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

    int calculateDamage(Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move, Random random,
                        Map<Map<String, Object>, Boolean> helpingHandBoosts, Map<String, Object> state) {
        int damageClassId = engine.toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);
        boolean criticalHit = Boolean.TRUE.equals(move.get("criticalHit"));
        Map<String, Object> attackerStats = engine.castMap(attacker.get("stats"));
        Map<String, Object> defenderStats = engine.castMap(defender.get("stats"));

        int attackStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? modifiedAttackStat(attacker, engine.toInt(attackerStats.get("attack"), 100), damageClassId, criticalHit)
                : modifiedAttackStat(attacker, engine.toInt(attackerStats.get("specialAttack"), 100), damageClassId, criticalHit);
        int baseDefenseStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? engine.toInt(defenderStats.get("defense"), 100)
                : engine.toInt(defenderStats.get("specialDefense"), 100);
        int defenseStat = Math.max(1, modifiedDefenseStat(defender, baseDefenseStat, damageClassId, state, criticalHit));

        int power = Math.max(1, engine.toInt(move.get("power"), 1));
        int baseDamage = DamageCalculatorUtil.calculateBaseDamage(level, power, attackStat, defenseStat);

        double modifier = 1.0d;
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        modifier *= stabModifier(attacker, moveTypeId);
        modifier *= typeModifier(defender, moveTypeId);
        modifier *= itemDamageModifier(attacker, moveTypeId);
        if (Boolean.TRUE.equals(helpingHandBoosts.get(attacker))) {
            modifier *= 1.5d;
        }
        if (engine.isDynamaxed(attacker)) {
            modifier *= 1.3d;
        }
        if (engine.isZMoveActive(attacker, engine.toInt(state.get("currentRound"), 0), move)) {
            modifier *= 1.5d;
        }
        if (criticalHit) {
            modifier *= "sniper".equalsIgnoreCase(engine.abilityName(attacker))
                    ? DamageCalculatorUtil.CRITICAL_MULTIPLIER * 1.5d
                    : DamageCalculatorUtil.CRITICAL_MULTIPLIER;
        }
        modifier *= weatherDamageModifier(state, moveTypeId);
        modifier *= terrainDamageModifier(state, attacker, defender, moveTypeId);
        modifier *= screenDamageModifier(state, defender, damageClassId);
        modifier *= (0.85d + (random.nextDouble() * 0.15d));

        return Math.max(1, (int) Math.floor(baseDamage * modifier));
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

    int modifiedAttackStat(Map<String, Object> mon, int baseStat, int damageClassId, boolean criticalHit) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            int attackStage = statStage(mon, "attack");
            if (criticalHit && attackStage < 0) {
                attackStage = 0;
            }
            baseStat = applyStageModifier(baseStat, attackStage);
            if ("burn".equals(mon.get("condition"))) {
                baseStat = Math.max(1, (int) Math.floor(baseStat * 0.5d));
            }
        } else if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            int specialAttackStage = statStage(mon, "specialAttack");
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
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && Boolean.TRUE.equals(mon.get("flashFireBoost"))) {
            baseStat = (int) Math.floor(baseStat * 1.5d);
        }
        return baseStat;
    }

    int modifiedDefenseStat(Map<String, Object> mon, int baseStat, int damageClassId, Map<String, Object> state, boolean criticalHit) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            int defenseStage = statStage(mon, "defense");
            if (criticalHit && defenseStage > 0) {
                defenseStage = 0;
            }
            baseStat = applyStageModifier(baseStat, defenseStage);
        } else if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
            int specialDefenseStage = statStage(mon, "specialDefense");
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

    double itemDamageModifier(Map<String, Object> mon, int moveTypeId) {
        String item = heldItem(mon);
        return switch (item) {
            case "life-orb" -> 1.3d;
            case "mystic-water" -> moveTypeId == DamageCalculatorUtil.TYPE_WATER ? 1.2d : 1.0d;
            case "charcoal" -> moveTypeId == DamageCalculatorUtil.TYPE_FIRE ? 1.2d : 1.0d;
            case "miracle-seed" -> moveTypeId == DamageCalculatorUtil.TYPE_GRASS ? 1.2d : 1.0d;
            default -> 1.0d;
        };
    }

    int speedValue(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        int speed = engine.toInt(engine.castMap(mon.get("stats")).get("speed"), 0);
        speed = applyStageModifier(speed, statStage(mon, "speed"));
        if ("paralysis".equals(mon.get("condition"))) {
            speed = Math.max(1, speed / 2);
        }
        if ("choice-scarf".equals(heldItem(mon))) {
            speed = (int) Math.floor(speed * 1.5d);
        }
        if (fieldEffectSupport.tailwindTurns(state, playerSide) > 0) {
            speed *= 2;
        }
        return speed;
    }

    int applyIncomingDamage(Map<String, Object> target, int damage, Map<String, Object> actionLog, List<String> events) {
        int currentHp = engine.toInt(target.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), Math.max(1, currentHp));
        int actualDamage = damage;
        if ("focus-sash".equals(heldItem(target)) && !itemConsumed(target) && currentHp == maxHp && damage >= currentHp) {
            actualDamage = Math.max(0, currentHp - 1);
            consumeItem(target);
            events.add(target.get("name") + " 靠气势披带撑住了攻击");
            actionLog.put("focusSash", true);
        }
        actionLog.put("damage", actualDamage);
        return Math.max(0, currentHp - actualDamage);
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

    double terrainDamageModifier(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender, int moveTypeId) {
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
        if (fieldEffectSupport.mistyTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_DRAGON && isGrounded(defender)) {
            return 0.5d;
        }
        return 1.0d;
    }

    double screenDamageModifier(Map<String, Object> state, Map<String, Object> defender, int damageClassId) {
        boolean playerSide = isOnSide(state, defender, true);
        if (fieldEffectSupport.auroraVeilTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL && fieldEffectSupport.reflectTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && fieldEffectSupport.lightScreenTurns(state, playerSide) > 0) {
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

    private double stabModifier(Map<String, Object> attacker, int moveTypeId) {
        if (moveTypeId <= 0) {
            return 1.0d;
        }
        boolean originalTypeMatch = engine.castList(attacker.get("types")).stream()
                .anyMatch(type -> engine.toInt(type.get("type_id"), 0) == moveTypeId);
        int teraTypeId = engine.toInt(attacker.get("teraTypeId"), 0);
        boolean teraMatch = Boolean.TRUE.equals(attacker.get("terastallized")) && teraTypeId > 0 && teraTypeId == moveTypeId;
        boolean adaptability = "adaptability".equalsIgnoreCase(abilityName(attacker));
        if (teraMatch && originalTypeMatch) {
            return adaptability ? DamageCalculatorUtil.ADAPTABILITY_STAB_MULTIPLIER * 1.125d : 2.0d;
        }
        if (teraMatch || originalTypeMatch) {
            return adaptability ? DamageCalculatorUtil.ADAPTABILITY_STAB_MULTIPLIER : DamageCalculatorUtil.STAB_MULTIPLIER;
        }
        return 1.0d;
    }

    private boolean isOnSide(Map<String, Object> state, Map<String, Object> mon, boolean playerSide) {
        for (Map<String, Object> candidate : engine.team(state, playerSide)) {
            if (candidate == mon) {
                return true;
            }
        }
        return false;
    }

    private String abilityName(Map<String, Object> mon) {
        Object ability = mon.get("ability");
        if (ability instanceof Map<?, ?> abilityMap) {
            Object nameEn = abilityMap.get("name_en");
            if (nameEn != null && !String.valueOf(nameEn).isBlank()) {
                return String.valueOf(nameEn);
            }
            Object name = abilityMap.get("name");
            if (name != null) {
                return String.valueOf(name);
            }
        }
        return ability == null ? "" : String.valueOf(ability);
    }
}
