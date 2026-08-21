package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI 替换决策逻辑：计算替换收益并选择最优上场宝可梦。
 */
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
        } else if (trapped && lowHp) {
            // Trapped and low HP - try to escape if possible
            switchProbability = 0.50;
        } else if (trapped) {
            // Just trapped - lower priority to switch
            switchProbability = 0.20;
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
        } else if (trapped) {
            // When trapped, prefer switching to a Pokemon with higher HP or better
            // defensive typing
            return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerMoveTypeIds(state, true));
        }
        return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerMoveTypeIds(state, true));
    }

    /**
     * Count total negative stat stages (H4: weight attack/speed drops more)
     */
    private int countNegativeStatStages(Map<String, Object> mon) {
        int total = 0;
        java.util.Map<String, Object> stages = engine.castMap(mon.get("statStages"));
        if (stages != null) {
            for (var entry : stages.entrySet()) {
                int stage = engine.toInt(entry.getValue(), 0);
                if (stage < 0) {
                    // Attack and speed drops are more impactful
                    String key = String.valueOf(entry.getKey());
                    int weight = ("attack".equals(key) || "specialAttack".equals(key)
                            || "speed".equals(key)) ? 2 : 1;
                    total += stage * weight;
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
        // 用 activeTypes（处理太晶化/属性变化后的实际属性），而非 base types
        List<Map<String, Object>> monTypes = engine.activeTypes(mon);
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

    /**
     * Find best defensive switch - switch to resist opponent's moves.
     * H4 improvement: multi-dimensional scoring (type resist + HP + offensive pressure)
     * with OHKO bailout (skip candidates that would be OHKO'd).
     */
    private int findBestDefensiveSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int fieldSlot,
            List<Integer> playerMoveTypeIds) {
        int bestCandidate = -1;
        double bestScore = Double.MIN_VALUE;
        for (int candidate = 0; candidate < team.size(); candidate++) {
            if (!engine.canSwitch(team, activeSlots, fieldSlot, candidate)) {
                continue;
            }
            Map<String, Object> candidateMon = team.get(candidate);
            int candidateHp = engine.toInt(candidateMon.get("currentHp"), 0);
            if (candidateHp <= 0) continue;

            double typeResist = maxTypeFactorAgainst(candidateMon, playerMoveTypeIds);

            // H4: OHKO bailout — if this mon would be OHKO'd by the strongest
            // opponent move, heavily penalize (but still consider if ALL candidates
            // would be OHKO'd, pick the one with highest HP)
            int maxHp = engine.toInt(engine.castMap(candidateMon.get("stats")).get("hp"), 1);
            double hpRatio = maxHp > 0 ? (double) candidateHp / maxHp : 0;

            // Composite score: lower type vulnerability is better, higher HP is better
            // Type resist: 0.0 = immune (best), 0.25 = quad resist, 1.0 = neutral, 2.0 = weak, 4.0 = quad weak
            // Convert to a score where lower vulnerability = higher score
            double typeScore = 4.0 - typeResist; // 4.0 for immune, 0.0 for quad-weak

            // HP contribution (0-30 points)
            double hpScore = hpRatio * 30;

            // Offensive presence: prefer switches that threaten back (0-20 points)
            Map<String, Object> stats = engine.castMap(candidateMon.get("stats"));
            int atk = engine.toInt(stats.get("attack"), 0);
            int spa = engine.toInt(stats.get("specialAttack"), 0);
            int spe = engine.toInt(stats.get("speed"), 0);
            double offenseScore = Math.min(20, Math.max(atk, spa) / 10.0 + spe / 20.0);

            // OHKO penalty: if type vulnerability is very high and HP is low
            double ohkoPenalty = 0;
            if (typeResist >= 2.0 && hpRatio < 0.7) {
                ohkoPenalty = -50; // severe penalty for switching into a likely OHKO
            }

            double totalScore = typeScore * 10 + hpScore + offenseScore + ohkoPenalty;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }
}