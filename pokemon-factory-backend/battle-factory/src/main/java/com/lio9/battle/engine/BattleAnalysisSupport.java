package com.lio9.battle.engine;



/**
 * BattleAnalysisSupport 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleAnalysisSupport 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
 * 阅读建议：建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.Map;

final class BattleAnalysisSupport {
    private final BattleEngine engine;

    BattleAnalysisSupport(BattleEngine engine) {
        this.engine = engine;
    }

    boolean opposingSideLikelyFaster(Map<String, Object> state, boolean playerSide) {
        int allyFastest = 0;
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot != null && slot >= 0 && slot < engine.team(state, playerSide).size()) {
                allyFastest = Math.max(allyFastest, engine.speedValue(engine.team(state, playerSide).get(slot), state, playerSide));
            }
        }
        int opponentFastest = 0;
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot != null && slot >= 0 && slot < engine.team(state, !playerSide).size()) {
                opponentFastest = Math.max(opponentFastest, engine.speedValue(engine.team(state, !playerSide).get(slot), state, !playerSide));
            }
        }
        return opponentFastest >= allyFastest;
    }

    boolean opposingSideLikelyPhysical(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            Map<String, Object> stats = engine.castMap(target.get("stats"));
            if (engine.toInt(stats.get("attack"), 0) >= engine.toInt(stats.get("specialAttack"), 0)
                    && !"burn".equals(target.get("condition"))
                    && !engine.targetHasType(target, DamageCalculatorUtil.TYPE_FIRE)) {
                return true;
            }
        }
        return false;
    }

    boolean opposingSideLikelySpecial(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            Map<String, Object> stats = engine.castMap(target.get("stats"));
            if (engine.toInt(stats.get("specialAttack"), 0) > engine.toInt(stats.get("attack"), 0)
                    && !"sleep".equals(target.get("condition"))) {
                return true;
            }
        }
        return false;
    }

    boolean sidePrefersWeather(Map<String, Object> state, boolean playerSide, int moveTypeId) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> activeMon = engine.team(state, playerSide).get(slot);
            for (Map<String, Object> move : engine.moves(activeMon)) {
                if (engine.toInt(move.get("type_id"), 0) == moveTypeId && engine.toInt(move.get("power"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean sidePrefersTerrain(Map<String, Object> state, boolean playerSide, int moveTypeId) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> activeMon = engine.team(state, playerSide).get(slot);
            if (!engine.isGrounded(activeMon)) {
                continue;
            }
            for (Map<String, Object> move : engine.moves(activeMon)) {
                if (engine.toInt(move.get("type_id"), 0) == moveTypeId && engine.toInt(move.get("power"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean sideHasType(Map<String, Object> state, boolean playerSide, int typeId) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            if (engine.targetHasType(engine.team(state, playerSide).get(slot), typeId)) {
                return true;
            }
        }
        return false;
    }

    boolean sideHasGroundedType(Map<String, Object> state, boolean playerSide, int typeId) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> activeMon = engine.team(state, playerSide).get(slot);
            if (engine.isGrounded(activeMon) && engine.targetHasType(activeMon, typeId)) {
                return true;
            }
        }
        return false;
    }

    boolean opposingSideLikelyUsingType(Map<String, Object> state, boolean playerSide, int typeId) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            for (Map<String, Object> move : engine.moves(target)) {
                if (engine.toInt(move.get("type_id"), 0) == typeId && engine.toInt(move.get("power"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean opposingSideLikelyUsingStatus(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            if (engine.tauntTurns(target) > 0) {
                continue;
            }
            for (Map<String, Object> move : engine.moves(target)) {
                if (isStatusMove(move) && !isTaunt(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean opposingSideLikelyHealing(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            if ("leftovers".equals(engine.heldItem(target)) || "sitrus-berry".equals(engine.heldItem(target))) {
                return true;
            }
            if (hasHealingAbility(target)) {
                return true;
            }
            for (Map<String, Object> move : engine.moves(target)) {
                if (engine.toInt(move.get("healing"), 0) > 0 || engine.toInt(move.get("drain"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if opposing side has a setup sweeper (boosted stats)
     */
    boolean opposingSideHasSetupSweeper(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            
            // Check for positive stat stages
            int totalBoosts = countPositiveStages(target);
            if (totalBoosts >= 3) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Count total positive stat stages
     */
    private int countPositiveStages(Map<String, Object> mon) {
        int total = 0;
        java.util.Map<String, Object> stages = engine.castMap(mon.get("statStages"));
        if (stages != null) {
            for (Object value : stages.values()) {
                int stage = engine.toInt(value, 0);
                if (stage > 0) {
                    total += stage;
                }
            }
        }
        return total;
    }
    
    /**
     * Evaluate threat level of opposing active Pokemon
     * Returns 0-100 score (higher = more threatening)
     */
    double evaluateThreatLevel(Map<String, Object> state, boolean playerSide, Map<String, Object> opponent) {
        double threatScore = 0.0;
        
        // HP factor - healthier Pokemon are more threatening
        int currentHp = engine.toInt(opponent.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(opponent.get("stats")).get("hp"), 1);
        double hpRatio = currentHp / (double) maxHp;
        threatScore += hpRatio * 25;
        
        // Offensive stats
        Map<String, Object> stats = engine.castMap(opponent.get("stats"));
        int attack = engine.toInt(stats.get("attack"), 0);
        int spAttack = engine.toInt(stats.get("specialAttack"), 0);
        int speed = engine.toInt(stats.get("speed"), 0);
        
        threatScore += Math.max(attack, spAttack) / 5.0; // Up to ~30 points
        threatScore += speed / 10.0; // Speed is valuable
        
        // Stat boosts increase threat significantly
        int totalBoosts = countPositiveStages(opponent);
        threatScore += totalBoosts * 8; // Each boost stage adds 8 points
        
        // Status conditions reduce threat
        String condition = String.valueOf(opponent.get("condition"));
        if ("sleep".equals(condition) || "freeze".equals(condition)) {
            threatScore *= 0.3;
        } else if ("paralysis".equals(condition) || "burn".equals(condition)) {
            threatScore *= 0.6;
        } else if ("poison".equals(condition) || "toxic".equals(condition)) {
            threatScore *= 0.7;
        }
        
        return Math.min(100.0, threatScore);
    }

    boolean opposingSideLikelyUsingPriority(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            for (Map<String, Object> move : engine.moves(target)) {
                if (engine.toInt(move.get("priority"), 0) > 0 && !MoveRegistry.isProtect(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean opposingSideCanBeSlept(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            if (engine.toInt(target.get("currentHp"), 0) <= 0) {
                continue;
            }
            if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
                continue;
            }
            if (electricTerrainTurns(state) > 0 && engine.isGrounded(target)) {
                continue;
            }
            if (!engine.targetHasType(target, DamageCalculatorUtil.TYPE_GRASS)) {
                return true;
            }
        }
        return false;
    }

    boolean opposingSideCanBeYawed(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            if (engine.toInt(target.get("currentHp"), 0) <= 0) {
                continue;
            }
            if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
                continue;
            }
            if (engine.yawnTurns(target) > 0) {
                continue;
            }
            boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
            if (engine.toInt(engine.castMap(state.get("fieldEffects")).get(targetOnPlayerSide ? "playerSafeguardTurns" : "opponentSafeguardTurns"), 0) > 0) {
                continue;
            }
            if (engine.isGrounded(target)
                    && (electricTerrainTurns(state) > 0 || mistyTerrainTurns(state) > 0)) {
                continue;
            }
            return true;
        }
        return false;
    }

    boolean opposingSideCanSleepAlly(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            for (Map<String, Object> move : engine.moves(engine.team(state, !playerSide).get(slot))) {
                if (isSpore(move) || isYawn(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int electricTerrainTurns(Map<String, Object> state) {
        return engine.toInt(engine.castMap(state.get("fieldEffects")).get("electricTerrainTurns"), 0);
    }

    private int mistyTerrainTurns(Map<String, Object> state) {
        return engine.toInt(engine.castMap(state.get("fieldEffects")).get("mistyTerrainTurns"), 0);
    }

    private boolean isStatusMove(Map<String, Object> move) {
        return engine.toInt(move.get("damage_class_id"), 0) == 3 || engine.toInt(move.get("power"), 0) == 0;
    }

    private boolean isTaunt(Map<String, Object> move) {
        return "taunt".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean hasHealingAbility(Map<String, Object> mon) {
        String ability = engine.abilityName(mon);
        return "regenerator".equalsIgnoreCase(ability)
                || "water-absorb".equalsIgnoreCase(ability)
                || "water absorb".equalsIgnoreCase(ability)
                || "volt-absorb".equalsIgnoreCase(ability)
                || "volt absorb".equalsIgnoreCase(ability);
    }

    private boolean isSpore(Map<String, Object> move) {
        return "spore".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isYawn(Map<String, Object> move) {
        return "yawn".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }
}
