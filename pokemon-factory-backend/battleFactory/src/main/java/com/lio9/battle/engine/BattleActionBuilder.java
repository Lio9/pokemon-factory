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
            Map<String, Object> move = engine.selectPlayerMove(mon, playerMoveMap, fieldSlot, currentRound);
            int targetFieldSlot = selectTargetFieldSlot(playerMoveMap, fieldSlot);
            int targetTeamIndex = engine.targetIndex(state, false, targetFieldSlot);
            actions.add(BattleEngine.Action.moveAction("player", monIndex, fieldSlot, targetTeamIndex,
                    targetFieldSlot, move, engine.speedValue(mon, state, true)));
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
            Map<String, Object> move = engine.selectAIMove(mon, random, state, false, currentRound);
            int targetFieldSlot = random.nextBoolean() ? fieldSlot : (fieldSlot == 0 ? 1 : 0);
            int targetTeamIndex = engine.targetIndex(state, true, targetFieldSlot);
            actions.add(BattleEngine.Action.moveAction("opponent", monIndex, fieldSlot, targetTeamIndex,
                    targetFieldSlot, move, engine.speedValue(mon, state, false)));
        }
        return actions;
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
        return Math.max(0, Math.min(1, target));
    }
}