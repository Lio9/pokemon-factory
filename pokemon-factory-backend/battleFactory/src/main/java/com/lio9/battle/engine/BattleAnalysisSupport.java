package com.lio9.battle.engine;

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

    boolean opposingSideLikelyUsingPriority(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            for (Map<String, Object> move : engine.moves(target)) {
                if (engine.toInt(move.get("priority"), 0) > 0 && !engine.isProtect(move)) {
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
