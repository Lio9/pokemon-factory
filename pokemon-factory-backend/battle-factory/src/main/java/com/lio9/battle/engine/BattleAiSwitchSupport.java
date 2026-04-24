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
        
        // Check for status conditions that might warrant switching
        boolean badlyStatused = "toxic".equals(mon.get("condition")) || 
                               "burn".equals(mon.get("condition"));
        boolean trapped = Boolean.TRUE.equals(mon.get("trapped"));
        
        // Check stat stages - negative stages might warrant switching
        int totalNegativeStages = countNegativeStatStages(mon);
        boolean heavilyDebuffed = totalNegativeStages <= -4;

        double switchProbability;
        if (criticalHp && superEffectiveVulnerable) {
            // Critical HP + type weakness = high priority switch
            switchProbability = 0.75;
        } else if (criticalHp) {
            switchProbability = 0.60;
        } else if (lowHp && superEffectiveVulnerable) {
            switchProbability = 0.45;
        } else if (superEffectiveVulnerable && badlyStatused) {
            // Type weakness + bad status = moderate priority
            switchProbability = 0.40;
        } else if (superEffectiveVulnerable) {
            switchProbability = 0.25;
        } else if (badlyStatused && !hasHealingMove(mon)) {
            // Bad status with no healing option
            switchProbability = 0.30;
        } else if (heavilyDebuffed) {
            // Heavily debuffed Pokemon should consider switching
            switchProbability = 0.35;
        } else {
            return -1;
        }

        if (random.nextDouble() >= switchProbability) {
            return -1;
        }
        
        // Choose best switch based on situation
        if (superEffectiveVulnerable) {
            return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerMoveTypeIds(state, true));
        } else if (badlyStatused || heavilyDebuffed) {
            return findBestPivotSwitch(team, activeSlots, fieldSlot, mon);
        }
        return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerMoveTypeIds(state, true));
    }
    
    /**
     * Count total negative stat stages
     */
    private int countNegativeStatStages(Map<String, Object> mon) {
        int total = 0;
        java.util.Map<String, Object> stages = engine.castMap(mon.get("statStages"));
        if (stages != null) {
            for (Object value : stages.values()) {
                int stage = engine.toInt(value, 0);
                if (stage < 0) {
                    total += stage;
                }
            }
        }
        return total;
    }
    
    /**
     * Check if mon has a healing move
     */
    private boolean hasHealingMove(Map<String, Object> mon) {
        for (java.util.Map<String, Object> move : engine.moves(mon)) {
            if (engine.toInt(move.get("healing"), 0) > 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all damaging move type IDs from a side
     */
    private List<Integer> playerMoveTypeIds(Map<String, Object> state, boolean playerSide) {
        return activeDamageTypeIds(state, playerSide);
    }
    
    /**
     * Find best pivot switch - switch to maintain momentum
     * Prefer Pokemon that can set up or have good offensive presence
     */
    private int findBestPivotSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, 
                                     int fieldSlot, Map<String, Object> currentMon) {
        int bestCandidate = -1;
        double bestScore = -1;
        
        for (int candidate = 0; candidate < team.size(); candidate++) {
            if (!engine.canSwitch(team, activeSlots, fieldSlot, candidate)) {
                continue;
            }
            
            Map<String, Object> candidateMon = team.get(candidate);
            double score = evaluatePivotPotential(candidateMon);
            
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }
        
        return bestCandidate;
    }
    
    /**
     * Evaluate pivot potential of a Pokemon
     * Higher score = better pivot choice
     */
    private double evaluatePivotPotential(Map<String, Object> mon) {
        double score = 0.0;
        
        // Prefer healthy Pokemon
        int currentHp = engine.toInt(mon.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
        double hpRatio = currentHp / (double) maxHp;
        score += hpRatio * 30; // Up to 30 points for full HP
        
        // Prefer Pokemon with setup moves
        for (Map<String, Object> move : engine.moves(mon)) {
            String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
            if (nameEn.contains("swords dance") || nameEn.contains("nasty plot") ||
                nameEn.contains("dragon dance") || nameEn.contains("calm mind") ||
                nameEn.contains("bulk up") || nameEn.contains("quiver dance")) {
                score += 20;
                break;
            }
        }
        
        // Prefer Pokemon with good offensive stats
        Map<String, Object> stats = engine.castMap(mon.get("stats"));
        int attack = engine.toInt(stats.get("attack"), 0);
        int spAttack = engine.toInt(stats.get("specialAttack"), 0);
        int speed = engine.toInt(stats.get("speed"), 0);
        
        // Add offensive presence score
        score += Math.max(attack, spAttack) / 10.0;
        score += speed / 15.0; // Speed is valuable
        
        // Prefer Pokemon without status
        if (mon.get("condition") == null || String.valueOf(mon.get("condition")).isBlank()) {
            score += 15;
        }
        
        return score;
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