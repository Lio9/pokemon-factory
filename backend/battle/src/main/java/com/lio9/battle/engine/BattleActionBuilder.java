package com.lio9.battle.engine;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleActionBuilder {
    private final BattleEngine engine;
    private final BattleAiSwitchSupport aiSwitchSupport;

    BattleActionBuilder(BattleEngine engine, BattleAiSwitchSupport aiSwitchSupport) {
        this.engine = engine;
        this.aiSwitchSupport = aiSwitchSupport;
    }

    List<BattleEngine.Action> buildPlayerActions(Map<String, Object> state, Map<String, String> playerMoveMap) {
        List<BattleEngine.Action> actions = new ArrayList<>();
        List<Integer> activeSlots = engine.activeSlots(state, true);
        List<Map<String, Object>> playerTeam = engine.team(state, true);
        int currentRound = engine.toInt(state.get("currentRound"), 0);
        for (int fieldSlot = 0; fieldSlot < activeSlots.size(); fieldSlot++) {
            int monIndex = activeSlots.get(fieldSlot);
            if (!engine.isAvailableMon(playerTeam, monIndex)) {
                continue;
            }
            if (isSwitchRequested(playerMoveMap, fieldSlot)) {
                int switchToIndex = selectSwitchTarget(playerMoveMap, playerTeam, activeSlots, fieldSlot);
                if (switchToIndex >= 0) {
                    actions.add(BattleEngine.Action.switchAction("player", monIndex, fieldSlot, switchToIndex,
                            engine.speedValue(playerTeam.get(monIndex), state, true)));
                    continue;
                }
            }
            Map<String, Object> mon = playerTeam.get(monIndex);
            Map<String, Object> move = engine.withEffectivePriority(mon, engine.selectPlayerMove(mon, playerMoveMap, fieldSlot, currentRound));
            move = boostGrassyGlide(state, move);
            int targetFieldSlot = selectTargetFieldSlot(playerMoveMap, fieldSlot);
            int targetTeamIndex = engine.targetIndex(state, false, targetFieldSlot);
            String specialSystemRequested = selectedSpecialSystem(playerMoveMap, fieldSlot);
            actions.add(BattleEngine.Action.moveAction("player", monIndex, fieldSlot, targetTeamIndex,
                    targetFieldSlot, move, engine.speedValue(mon, state, true), specialSystemRequested));
        }
        return actions;
    }

    List<BattleEngine.Action> buildOpponentActions(Map<String, Object> state, Random random) {
        List<BattleEngine.Action> actions = new ArrayList<>();
        List<Integer> activeSlots = engine.activeSlots(state, false);
        List<Map<String, Object>> opponentTeam = engine.team(state, false);
        int currentRound = engine.toInt(state.get("currentRound"), 0);
        for (int fieldSlot = 0; fieldSlot < activeSlots.size(); fieldSlot++) {
            int monIndex = activeSlots.get(fieldSlot);
            if (!engine.isAvailableMon(opponentTeam, monIndex)) {
                continue;
            }
            int switchToIndex = aiSwitchSupport.chooseAISwitch(opponentTeam, activeSlots, monIndex, fieldSlot, random, state);
            if (switchToIndex >= 0) {
                actions.add(BattleEngine.Action.switchAction("opponent", monIndex, fieldSlot, switchToIndex,
                        engine.speedValue(opponentTeam.get(monIndex), state, false)));
                continue;
            }
            Map<String, Object> mon = opponentTeam.get(monIndex);
            Map<String, Object> move = engine.withEffectivePriority(mon, engine.selectAIMove(mon, random, state, false, currentRound));
            move = boostGrassyGlide(state, move);
            List<Integer> playerActive = engine.activeSlots(state, true);
            int targetFieldSlot = selectBestTargetSlot(mon, move, state, playerActive, random);
            int targetTeamIndex = engine.targetIndex(state, true, targetFieldSlot);
            String specialSystemRequested = shouldAIUseSpecialSystem(state, mon, move, currentRound);
            actions.add(BattleEngine.Action.moveAction("opponent", monIndex, fieldSlot, targetTeamIndex,
                    targetFieldSlot, move, engine.speedValue(mon, state, false), specialSystemRequested));
        }
        return actions;
    }

    private Map<String, Object> boostGrassyGlide(Map<String, Object> state, Map<String, Object> move) {
        if (isGrassyGlide(move) && grassyTerrainActive(state)) {
            Map<String, Object> boosted = new java.util.LinkedHashMap<>(move);
            boosted.put("priority", engine.toInt(move.get("priority"), 0) + 1);
            return boosted;
        }
        return move;
    }

    private boolean isGrassyGlide(Map<String, Object> move) {
        String name = String.valueOf(move.get("name_en"));
        return "grassy-glide".equalsIgnoreCase(name) || "grassy glide".equalsIgnoreCase(name);
    }

    private boolean grassyTerrainActive(Map<String, Object> state) {
        Object fe = state.get("fieldEffects");
        if (fe instanceof Map<?, ?> effects) {
            return engine.toInt(((Map<String, Object>) effects).get("grassyTerrainTurns"), 0) > 0;
        }
        return false;
    }

    private boolean isSwitchRequested(Map<String, String> playerMoveMap, int fieldSlot) {
        return playerMoveMap != null && "switch".equalsIgnoreCase(String.valueOf(playerMoveMap.get("action-slot-" + fieldSlot)));
    }

    private int selectSwitchTarget(Map<String, String> playerMoveMap, List<Map<String, Object>> team,
                                   List<Integer> activeSlots, int fieldSlot) {
        int switchToIndex = engine.toInt(playerMoveMap.get("switch-slot-" + fieldSlot), -1);
        if (engine.canSwitch(team, activeSlots, fieldSlot, switchToIndex)) {
            return switchToIndex;
        }
        return engine.firstAvailableBench(team, activeSlots);
    }

    private int selectTargetFieldSlot(Map<String, String> playerMoveMap, int fieldSlot) {
        if (playerMoveMap == null) {
            return fieldSlot;
        }
        int target = engine.toInt(playerMoveMap.get("target-slot-" + fieldSlot), fieldSlot);
        // 不硬编码上限，交给下游 targetIndex 做实际校验
        return Math.max(0, target);
    }

    private String selectedSpecialSystem(Map<String, String> playerMoveMap, int fieldSlot) {
        if (playerMoveMap == null) {
            return null;
        }
        String requested = playerMoveMap.get("special-slot-" + fieldSlot);
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return Boolean.parseBoolean(String.valueOf(playerMoveMap.getOrDefault("tera-slot-" + fieldSlot, "false"))) ? "tera" : null;
    }

    private String shouldAIUseSpecialSystem(Map<String, Object> state, Map<String, Object> mon, Map<String, Object> move, int currentRound) {
        if (engine.isStatusMove(move) || engine.toInt(move.get("power"), 0) <= 0 || currentRound > 3) {
            return null;
        }
        if (engine.canUseSpecialSystem(state, false, mon, "z-move", move) && hasAdvantageousTarget(state, move, "z-move")) {
            return "z-move";
        }
        if (engine.canUseSpecialSystem(state, false, mon, "mega", move) && currentRound <= 2) {
            return "mega";
        }
        if (engine.canUseSpecialSystem(state, false, mon, "dynamax", move) && currentRound <= 2) {
            return "dynamax";
        }
        if (engine.canUseSpecialSystem(state, false, mon, "tera", move) && hasAdvantageousTarget(state, move, "tera")) {
            return "tera";
        }
        return null;
    }

    private boolean hasAdvantageousTarget(Map<String, Object> state, Map<String, Object> move, String system) {
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        if (moveTypeId <= 0) {
            return false;
        }
        for (Integer slot : engine.activeSlots(state, true)) {
            List<Map<String, Object>> playerTeam = engine.team(state, true);
            if (!engine.isAvailableMon(playerTeam, slot)) {
                continue;
            }
            for (Map<String, Object> type : engine.activeTypes(playerTeam.get(slot))) {
                if (engine.typeFactor(moveTypeId, engine.toInt(type.get("type_id"), 0)) > 100) {
                    return true;
                }
            }
        }
        return "mega".equals(system) || "dynamax".equals(system);
    }

    /**
     * AI 智能选目标（H3）：按类型克制 × 剩余HP 选最优攻击目标。
     * 替代之前随机选择，让双打 AI 打弱点、补刀更精准。
     */
    private int selectBestTargetSlot(Map<String, Object> mon, Map<String, Object> move,
            Map<String, Object> state, List<Integer> playerActive, Random random) {
        if (playerActive.size() <= 1) {
            return 0;
        }
        List<Map<String, Object>> playerTeam = engine.team(state, true);
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        int power = engine.toInt(move.get("power"), 0);

        int bestSlot = 0;
        double bestScore = -1.0d;

        for (int fieldSlot = 0; fieldSlot < playerActive.size(); fieldSlot++) {
            int teamIdx = playerActive.get(fieldSlot);
            if (teamIdx < 0 || teamIdx >= playerTeam.size()) continue;
            Map<String, Object> target = playerTeam.get(teamIdx);
            if (engine.toInt(target.get("currentHp"), 0) <= 0) continue;

            double score = 0.0d;
            if (power > 0 && moveTypeId > 0) {
                // 类型克制系数
                double typeMod = engine.typeModifier(target, moveTypeId);
                score = typeMod * power;
                // 低血量加分（补刀优先）
                int hp = engine.toInt(target.get("currentHp"), 0);
                int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1);
                if (maxHp > 0) {
                    double hpRatio = (double) hp / maxHp;
                    if (hpRatio < 0.3) score += 30.0d; // 低血量目标优先
                }
            } else {
                // 变化招式：默认选第一个
                score = fieldSlot == 0 ? 1.0d : 0.5d;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = fieldSlot;
            }
        }
        return bestSlot;
    }
}
