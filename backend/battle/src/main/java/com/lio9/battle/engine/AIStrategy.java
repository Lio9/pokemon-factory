package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI策略基类
 * 所有难度级别的AI策略都继承此类
 */
abstract class AIStrategy {
    protected final BattleEngine engine;
    protected final AIDifficulty difficulty;

    AIStrategy(BattleEngine engine, AIDifficulty difficulty) {
        this.engine = engine;
        this.difficulty = difficulty;
    }

    /**
     * 选择最佳招式
     */
    abstract Map<String, Object> selectMove(Map<String, Object> mon,
                                            List<Map<String, Object>> opponents,
                                            Map<String, Object> state,
                                            int currentRound);

    /**
     * 选择换人目标
     */
    abstract Map<String, Object> selectSwitch(Map<String, Object> currentMon,
                                              List<Map<String, Object>> team,
                                              List<Map<String, Object>> opponents,
                                              Map<String, Object> state);

    /**
     * 获取可用的招式列表
     */
    protected List<Map<String, Object>> getAvailableMoves(Map<String, Object> mon, int currentRound) {
        List<Map<String, Object>> available = new ArrayList<>();
        for (Map<String, Object> move : engine.moves(mon)) {
            if (engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                available.add(move);
            }
        }
        return available;
    }

    /**
     * 计算类型克制系数（使用引擎真实的 typeModifier）
     */
    protected double calculateTypeEffectiveness(Map<String, Object> defender, int moveTypeId) {
        if (moveTypeId <= 0) return 1.0;
        return engine.typeModifier(defender, moveTypeId);
    }

    /**
     * 预估伤害（使用引擎的 calculateDamage 或简化版 STAB+克制+攻防比）
     */
    protected int estimateDamage(Map<String, Object> attacker, Map<String, Object> defender,
                                 Map<String, Object> move, Map<String, Object> state) {
        int power = engine.toInt(move.get("power"), 0);
        if (power <= 0) return 0;

        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        boolean isSpecial = engine.toInt(move.get("damage_class_id"), 0) == 2;
        Map<String, Object> atkStats = engine.castMap(attacker.get("stats"));
        Map<String, Object> defStats = engine.castMap(defender.get("stats"));
        int atkStat = isSpecial ? engine.toInt(atkStats.get("specialAttack"), 100) : engine.toInt(atkStats.get("attack"), 100);
        int defStat = isSpecial ? engine.toInt(defStats.get("specialDefense"), 100) : engine.toInt(defStats.get("defense"), 100);

        // STAB
        double stab = 1.0;
        for (Map<String, Object> t : engine.activeTypes(attacker)) {
            if (engine.toInt(t.get("type_id"), 0) == moveTypeId) { stab = 1.5; break; }
        }

        double typeMod = calculateTypeEffectiveness(defender, moveTypeId);
        if (typeMod <= 0) return 0;

        double base = (2.0 * 50 / 5.0 + 2.0) * power * atkStat / Math.max(1, defStat) / 50.0 + 2.0;
        return (int) Math.floor(base * stab * typeMod);
    }
}

/**
 * EASY难度策略：随机选择
 */
class EasyAIStrategy extends AIStrategy {
    EasyAIStrategy(BattleEngine engine) {
        super(engine, AIDifficulty.EASY);
    }

    @Override
    public Map<String, Object> selectMove(Map<String, Object> mon,
                                          List<Map<String, Object>> opponents,
                                          Map<String, Object> state,
                                          int currentRound) {
        List<Map<String, Object>> available = getAvailableMoves(mon, currentRound);
        if (available.isEmpty()) return null;

        // 随机选择一个招式
        return available.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(available.size()));
    }

    @Override
    public Map<String, Object> selectSwitch(Map<String, Object> currentMon,
                                            List<Map<String, Object>> team,
                                            List<Map<String, Object>> opponents,
                                            Map<String, Object> state) {
        // 随机选择一个未濒死的队友
        List<Map<String, Object>> aliveTeam = new ArrayList<>();
        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) > 0 && mon != currentMon) {
                aliveTeam.add(mon);
            }
        }

        if (aliveTeam.isEmpty()) return null;
        return aliveTeam.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(aliveTeam.size()));
    }
}

/**
 * NORMAL难度策略：基于伤害预测和类型克制
 */
class NormalAIStrategy extends AIStrategy {
    NormalAIStrategy(BattleEngine engine) {
        super(engine, AIDifficulty.NORMAL);
    }

    @Override
    public Map<String, Object> selectMove(Map<String, Object> mon,
                                          List<Map<String, Object>> opponents,
                                          Map<String, Object> state,
                                          int currentRound) {
        List<Map<String, Object>> available = getAvailableMoves(mon, currentRound);
        if (available.isEmpty()) return null;

        Map<String, Object> bestMove = null;
        double bestScore = -1;

        for (Map<String, Object> move : available) {
            // 对每个对手评估该招式
            for (Map<String, Object> opponent : opponents) {
                if (engine.toInt(opponent.get("currentHp"), 0) <= 0) continue;

                ThreatAssessment assessment = evaluateThreat(mon, opponent, move, state);
                if (assessment.getScore() > bestScore) {
                    bestScore = assessment.getScore();
                    bestMove = move;
                }
            }
        }

        return bestMove != null ? bestMove : available.get(0);
    }

    @Override
    public Map<String, Object> selectSwitch(Map<String, Object> currentMon,
                                            List<Map<String, Object>> team,
                                            List<Map<String, Object>> opponents,
                                            Map<String, Object> state) {
        // 简单策略：选择HP比例最高的队友
        Map<String, Object> bestCandidate = null;
        double bestHpRatio = 0;

        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) <= 0 || mon == currentMon) continue;

            int hp = engine.toInt(mon.get("currentHp"), 0);
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            double ratio = (double) hp / maxHp;

            if (ratio > bestHpRatio) {
                bestHpRatio = ratio;
                bestCandidate = mon;
            }
        }

        return bestCandidate;
    }

    /**
     * 评估招式威胁
     */
    private ThreatAssessment evaluateThreat(Map<String, Object> attacker,
                                           Map<String, Object> defender,
                                           Map<String, Object> move,
                                           Map<String, Object> state) {
        int damage = estimateDamage(attacker, defender, move, state);
        int defenderHp = engine.toInt(defender.get("currentHp"), 0);
        int defenderMaxHp = engine.toInt(engine.castMap(defender.get("stats")).get("hp"), 1);

        double damagePotential = defenderMaxHp > 0 ? (double) damage / defenderMaxHp : 0;
        boolean canKO = damage >= defenderHp;
        int priority = engine.toInt(move.get("priority"), 0);

        ThreatAssessment.Builder builder = new ThreatAssessment.Builder()
                .damagePotential(damagePotential)
                .typeAdvantage(0.0) // TODO: 实现类型优势计算
                .canKO(canKO)
                .priority(priority);

        // 添加优势和风险
        if (canKO) {
            builder.addAdvantage("Can KO target");
        }
        if (damagePotential > 0.5) {
            builder.addAdvantage("High damage potential");
        }

        return builder.build();
    }
}

/**
 * HARD难度策略：多步预测（简化版）
 */
class HardAIStrategy extends AIStrategy {
    HardAIStrategy(BattleEngine engine) {
        super(engine, AIDifficulty.HARD);
    }

    @Override
    public Map<String, Object> selectMove(Map<String, Object> mon,
                                          List<Map<String, Object>> opponents,
                                          Map<String, Object> state,
                                          int currentRound) {
        // 使用NORMAL策略作为基础，增加简单的lookahead
        NormalAIStrategy normalStrategy = new NormalAIStrategy(engine);
        return normalStrategy.selectMove(mon, opponents, state, currentRound);
    }

    @Override
    public Map<String, Object> selectSwitch(Map<String, Object> currentMon,
                                            List<Map<String, Object>> team,
                                            List<Map<String, Object>> opponents,
                                            Map<String, Object> state) {
        // 智能换人：考虑类型克制
        Map<String, Object> bestCandidate = null;
        double bestScore = -1;

        for (Map<String, Object> candidate : team) {
            if (engine.toInt(candidate.get("currentHp"), 0) <= 0 || candidate == currentMon) continue;

            double score = evaluateSwitchCandidate(candidate, opponents);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    /**
     * 评估换人候选者
     */
    private double evaluateSwitchCandidate(Map<String, Object> candidate,
                                           List<Map<String, Object>> opponents) {
        // 简化评分：基于HP比例和存活数量
        int hp = engine.toInt(candidate.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(candidate.get("stats")).get("hp"), 1);
        double hpRatio = (double) hp / maxHp;

        return hpRatio;
    }
}

/**
 * EXPERT难度策略：Minimax算法（占位实现）
 */
class ExpertAIStrategy extends AIStrategy {
    ExpertAIStrategy(BattleEngine engine) {
        super(engine, AIDifficulty.EXPERT);
    }

    @Override
    public Map<String, Object> selectMove(Map<String, Object> mon,
                                          List<Map<String, Object>> opponents,
                                          Map<String, Object> state,
                                          int currentRound) {
        // 使用HARD策略作为基础
        HardAIStrategy hardStrategy = new HardAIStrategy(engine);
        return hardStrategy.selectMove(mon, opponents, state, currentRound);
    }

    @Override
    public Map<String, Object> selectSwitch(Map<String, Object> currentMon,
                                            List<Map<String, Object>> team,
                                            List<Map<String, Object>> opponents,
                                            Map<String, Object> state) {
        HardAIStrategy hardStrategy = new HardAIStrategy(engine);
        return hardStrategy.selectSwitch(currentMon, team, opponents, state);
    }
}
