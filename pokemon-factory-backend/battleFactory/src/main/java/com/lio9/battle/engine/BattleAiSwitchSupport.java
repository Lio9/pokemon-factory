package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleAiSwitchSupport {
    private final BattleEngine engine;

    BattleAiSwitchSupport(BattleEngine engine) {
        this.engine = engine;
    }

    int chooseAISwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int activeTeamIndex,
                       int fieldSlot, Random random, Map<String, Object> state) {
        Map<String, Object> mon = team.get(activeTeamIndex);
        int currentHp = engine.toInt(mon.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), Math.max(1, currentHp));
        if (currentHp <= 0 || maxHp <= 0) {
            return -1;
        }

        int hpPercent = currentHp * 100 / maxHp;
        List<Integer> playerDamageTypeIds = activeDamageTypeIds(state, true);
        double currentVulnerabilityFactor = maxTypeFactorAgainst(mon, playerDamageTypeIds);
        boolean criticalHp = hpPercent <= 30;
        boolean lowHp = hpPercent <= 50;
        boolean superEffectiveVulnerable = currentVulnerabilityFactor > 1.0;

        double switchProbability;
        if (criticalHp) {
            switchProbability = 0.60;
        } else if (lowHp && superEffectiveVulnerable) {
            switchProbability = 0.45;
        } else if (superEffectiveVulnerable) {
            switchProbability = 0.25;
        } else {
            return -1;
        }

        if (random.nextDouble() >= switchProbability) {
            return -1;
        }
        return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerDamageTypeIds);
    }

    private List<Integer> activeDamageTypeIds(Map<String, Object> state, boolean playerSide) {
        List<Integer> typeIds = new ArrayList<>();
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            for (Map<String, Object> move : engine.moves(engine.team(state, playerSide).get(slot))) {
                if (engine.toInt(move.get("power"), 0) > 0) {
                    typeIds.add(engine.toInt(move.get("type_id"), 0));
                }
            }
        }
        return typeIds;
    }

    private double maxTypeFactorAgainst(Map<String, Object> mon, List<Integer> moveTypeIds) {
        if (moveTypeIds.isEmpty()) {
            return 1.0;
        }
        List<Map<String, Object>> monTypes = engine.castList(mon.get("types"));
        double maxFactor = 0.0;
        for (int moveTypeId : moveTypeIds) {
            double moveFactor = 1.0;
            for (Map<String, Object> monType : monTypes) {
                moveFactor *= engine.typeFactor(moveTypeId, engine.toInt(monType.get("type_id"), 0)) / 100.0;
            }
            maxFactor = Math.max(maxFactor, moveFactor);
        }
        return maxFactor;
    }

    private int findBestDefensiveSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int fieldSlot,
                                        List<Integer> playerMoveTypeIds) {
        int bestCandidate = -1;
        double bestScore = Double.MAX_VALUE;
        int bestHp = -1;
        for (int candidate = 0; candidate < team.size(); candidate++) {
            if (!engine.canSwitch(team, activeSlots, fieldSlot, candidate)) {
                continue;
            }
            Map<String, Object> candidateMon = team.get(candidate);
            double score = maxTypeFactorAgainst(candidateMon, playerMoveTypeIds);
            int candidateHp = engine.toInt(candidateMon.get("currentHp"), 0);
            if (score < bestScore || (score == bestScore && candidateHp > bestHp)) {
                bestScore = score;
                bestCandidate = candidate;
                bestHp = candidateHp;
            }
        }
        return bestCandidate;
    }
}