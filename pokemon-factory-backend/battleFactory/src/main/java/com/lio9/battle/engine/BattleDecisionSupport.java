package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleDecisionSupport {
    private final BattleEngine engine;
    private final BattleAISupport aiSupport;

    BattleDecisionSupport(BattleEngine engine, BattleAISupport aiSupport) {
        this.engine = engine;
        this.aiSupport = aiSupport;
    }

    Map<String, Object> selectPlayerMove(Map<String, Object> mon, Map<String, String> playerMoveMap, int fieldSlot, int currentRound) {
        Map<String, Object> lockedMove = engine.lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = engine.moves(mon);
        if (playerMoveMap != null) {
            String desiredMove = playerMoveMap.get(mon.get("name_en"));
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get(mon.get("name"));
            }
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get("slot-" + fieldSlot);
            }
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get("__active");
            }
            if (desiredMove != null) {
                for (Map<String, Object> move : moves) {
                    if ((String.valueOf(move.get("name_en")).equalsIgnoreCase(desiredMove)
                            || String.valueOf(move.get("name")).equalsIgnoreCase(desiredMove))
                            && engine.cooldown(mon, move) == 0
                            && engine.canUseMove(mon, move, currentRound)) {
                        return move;
                    }
                }
            }
        }
        return defaultMoveSelection(mon, currentRound);
    }

    Map<String, Object> selectAIMove(Map<String, Object> mon, Random random, Map<String, Object> state, boolean playerSide, int currentRound) {
        Map<String, Object> lockedMove = engine.lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = engine.moves(mon);
        Map<String, Object> fakeOutMove = aiSupport.selectAIFakeOutMove(mon, currentRound);
        if (fakeOutMove != null && random.nextDouble() < 0.65d) {
            return fakeOutMove;
        }
        Map<String, Object> sleepMove = aiSupport.selectAISleepMove(mon, state, playerSide, currentRound);
        if (sleepMove != null && random.nextDouble() < 0.4d) {
            return sleepMove;
        }
        Map<String, Object> terrainMove = aiSupport.selectAITerrainMove(mon, state, playerSide, currentRound);
        if (terrainMove != null && random.nextDouble() < 0.4d) {
            return terrainMove;
        }
        Map<String, Object> screenMove = aiSupport.selectAIScreenMove(mon, state, playerSide, currentRound);
        if (screenMove != null && random.nextDouble() < 0.35d) {
            return screenMove;
        }
        Map<String, Object> tauntMove = aiSupport.selectAITauntMove(mon, state, playerSide, currentRound);
        if (tauntMove != null && random.nextDouble() < 0.4d) {
            return tauntMove;
        }
        Map<String, Object> helpingHandMove = aiSupport.selectAIHelpingHandMove(mon, state, playerSide, currentRound);
        if (helpingHandMove != null && random.nextDouble() < 0.30d) {
            return helpingHandMove;
        }
        Map<String, Object> redirectionMove = aiSupport.selectAIRedirectionMove(mon, state, playerSide, currentRound);
        if (redirectionMove != null && random.nextDouble() < 0.35d) {
            return redirectionMove;
        }
        Map<String, Object> burnMove = aiSupport.selectAIBurnMove(mon, state, playerSide, currentRound);
        if (burnMove != null && random.nextDouble() < 0.35d) {
            return burnMove;
        }
        Map<String, Object> speedControlMove = aiSupport.selectAISpeedControlMove(mon, state, playerSide, currentRound);
        if (speedControlMove != null && random.nextDouble() < 0.35d) {
            return speedControlMove;
        }
        Map<String, Object> weatherMove = aiSupport.selectAIWeatherMove(mon, state, playerSide, currentRound);
        if (weatherMove != null && random.nextDouble() < 0.35d) {
            return weatherMove;
        }
        Map<String, Object> best = defaultMoveSelection(mon, currentRound);
        if (random.nextDouble() < 0.15d) {
            List<Map<String, Object>> available = new ArrayList<>();
            for (Map<String, Object> move : moves) {
                if (engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                    available.add(move);
                }
            }
            if (!available.isEmpty()) {
                return available.get(random.nextInt(available.size()));
            }
        }
        return best;
    }

    Map<String, Object> defaultMoveSelection(Map<String, Object> mon, int currentRound) {
        Map<String, Object> lockedMove = engine.lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = engine.moves(mon);
        for (Map<String, Object> move : moves) {
            if (engine.cooldown(mon, move) == 0 && !engine.isProtect(move) && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        for (Map<String, Object> move : moves) {
            if (engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return Map.of("name", "Struggle", "name_en", "struggle", "power", 50, "accuracy", 100, "priority", 0, "damage_class_id", 1, "type_id", 1);
    }

}
