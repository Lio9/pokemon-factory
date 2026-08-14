package com.lio9.battle.engine;


import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 回合决策构建：将玩家/AI 的动作选择转换为引擎可执行的指令。
 */
final class BattleDecisionSupport {
    private final BattleEngine engine;
    private final BattleAISupport aiSupport;

    BattleDecisionSupport(BattleEngine engine, BattleAISupport aiSupport) {
        this.engine = engine;
        this.aiSupport = aiSupport;
    }

    Map<String, Object> selectPlayerMove(Map<String, Object> mon, Map<String, String> playerMoveMap, int fieldSlot, int currentRound) {
        Map<String, Object> lockedMove = forcedChoiceSelection(mon, currentRound);
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
        Map<String, Object> lockedMove = forcedChoiceSelection(mon, currentRound);
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
        Map<String, Object> best = selectBestDamageMove(mon, state, playerSide, currentRound);
        if (best == null) {
            best = defaultMoveSelection(mon, currentRound);
        }
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

    /**
     * 基于伤害估算选择期望伤害最高的攻击招式（Showdown AI 风格）。
     * 遍历所有可用攻击招式，对每个在场目标估算伤害（含 STAB 与属性克制），
     * 取所有目标伤害之和作为该招式得分。若没有可用的攻击招式则返回 null。
     */
    Map<String, Object> selectBestDamageMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        List<Map<String, Object>> moves = engine.moves(mon);
        Map<String, Object> bestMove = null;
        double bestScore = -1.0d;
        for (Map<String, Object> move : moves) {
            if (engine.cooldown(mon, move) != 0 || !engine.canUseMove(mon, move, currentRound)) {
                continue;
            }
            int power = engine.toInt(move.get("power"), 0);
            if (power <= 0) {
                continue; // 只评估攻击招式；变化技由其他启发式处理
            }
            double score = estimateMoveScore(mon, move, state, playerSide);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /** 估算单个招式对敌方在场目标的伤害得分（STAB × 属性克制 × 基础伤害，累加所有目标） */
    private double estimateMoveScore(Map<String, Object> attacker, Map<String, Object> move,
            Map<String, Object> state, boolean playerSide) {
        int power = engine.toInt(move.get("power"), 0);
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        boolean isSpecial = engine.toInt(move.get("damage_class_id"), 0) == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL;
        Map<String, Object> stats = engine.castMap(attacker.get("stats"));
        double attackStat = isSpecial
                ? engine.toInt(stats.get("specialAttack"), 100)
                : engine.toInt(stats.get("attack"), 100);

        // STAB
        double stab = 1.0d;
        for (Map<String, Object> t : engine.activeTypes(attacker)) {
            if (engine.toInt(t.get("type_id"), 0) == moveTypeId) {
                stab = 1.5d;
                break;
            }
        }

        double total = 0.0d;
        for (Integer slot : engine.activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = engine.team(state, !playerSide).get(slot);
            if (engine.toInt(target.get("currentHp"), 0) <= 0) {
                continue;
            }
            double typeMod = engine.typeModifier(target, moveTypeId);
            if (typeMod <= 0.0d) {
                continue;
            }
            Map<String, Object> targetStats = engine.castMap(target.get("stats"));
            double defenseStat = isSpecial
                    ? engine.toInt(targetStats.get("specialDefense"), 100)
                    : engine.toInt(targetStats.get("defense"), 100);
            double base = (2.0d * 50 / 5.0d + 2.0d) * power * attackStat / Math.max(1.0d, defenseStat) / 50.0d + 2.0d;
            total += base * stab * typeMod;
        }
        return total;
    }

    Map<String, Object> defaultMoveSelection(Map<String, Object> mon, int currentRound) {
        Map<String, Object> lockedMove = forcedChoiceSelection(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = engine.moves(mon);
        for (Map<String, Object> move : moves) {
            if (engine.cooldown(mon, move) == 0 && !MoveRegistry.isProtect(move) && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        for (Map<String, Object> move : moves) {
            if (engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return struggleMove();
    }

    private Map<String, Object> forcedChoiceSelection(Map<String, Object> mon, int currentRound) {
        Map<String, Object> lockedMove = engine.lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            if (!engine.canUseMove(mon, lockedMove, currentRound)) {
                return struggleMove();
            }
            return lockedMove;
        }
        Object lockedMoveName = mon.get("choiceLockedMove");
        if (lockedMoveName != null && !String.valueOf(lockedMoveName).isBlank()) {
            return struggleMove();
        }
        return null;
    }

    private Map<String, Object> struggleMove() {
        // PS 规范：挣扎无属性(type_id=0)，始终 1 倍，不会被幽灵免疫
        return Map.of("name", "Struggle", "name_en", "struggle", "power", 50, "accuracy", 100, "priority", 0, "damage_class_id", 1, "type_id", 0);
    }

}
