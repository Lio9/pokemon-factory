package com.lio9.battle.engine;

/**
 * BattleAISupport 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleAISupport 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
 * 阅读建议：建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;

final class BattleAISupport {
    private final BattleEngine engine;
    private final BattleAnalysisSupport analysisSupport;

    BattleAISupport(BattleEngine engine, BattleAnalysisSupport analysisSupport) {
        this.engine = engine;
        this.analysisSupport = analysisSupport;
    }

    Map<String, Object> selectAIFakeOutMove(Map<String, Object> mon, int currentRound) {
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isFakeOut(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAISleepMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isSpore(move)
                    && analysisSupport.opposingSideCanBeSlept(state, playerSide)
                    && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
            if (isYawn(move)
                    && analysisSupport.opposingSideCanBeYawed(state, playerSide)
                    && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAITerrainMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        if (terrainTurns(state) > 0) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (engine.cooldown(mon, move) != 0 || !engine.canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isElectricTerrain(move)
                    && (analysisSupport.sidePrefersTerrain(state, playerSide, DamageCalculatorUtil.TYPE_ELECTRIC)
                            || analysisSupport.opposingSideCanSleepAlly(state, playerSide))) {
                return move;
            }
            if (isPsychicTerrain(move)
                    && (analysisSupport.sidePrefersTerrain(state, playerSide, DamageCalculatorUtil.TYPE_PSYCHIC)
                            || analysisSupport.opposingSideLikelyUsingPriority(state, playerSide))) {
                return move;
            }
            if (isGrassyTerrain(move)
                    && (analysisSupport.sidePrefersTerrain(state, playerSide, DamageCalculatorUtil.TYPE_GRASS)
                            || analysisSupport.sideHasGroundedType(state, playerSide,
                                    DamageCalculatorUtil.TYPE_GRASS))) {
                return move;
            }
            if (isMistyTerrain(move)
                    && (analysisSupport.sideHasGroundedType(state, playerSide, DamageCalculatorUtil.TYPE_FAIRY)
                            || analysisSupport.opposingSideLikelyUsingStatus(state, playerSide)
                            || analysisSupport.opposingSideLikelyUsingType(state, playerSide,
                                    DamageCalculatorUtil.TYPE_DRAGON))) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAIScreenMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        for (Map<String, Object> move : engine.moves(mon)) {
            if (engine.cooldown(mon, move) != 0 || !engine.canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isReflect(move) && reflectTurns(state, playerSide) == 0
                    && analysisSupport.opposingSideLikelyPhysical(state, playerSide)) {
                return move;
            }
            if (isLightScreen(move) && lightScreenTurns(state, playerSide) == 0
                    && analysisSupport.opposingSideLikelySpecial(state, playerSide)) {
                return move;
            }
            if (isSafeguard(move) && safeguardTurns(state, playerSide) == 0
                    && analysisSupport.opposingSideLikelyUsingStatus(state, playerSide)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAITauntMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        boolean statusThreat = analysisSupport.opposingSideLikelyUsingStatus(state, playerSide);
        boolean healingThreat = analysisSupport.opposingSideLikelyHealing(state, playerSide);
        if (!statusThreat && !healingThreat) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (statusThreat && isTaunt(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
            if (statusThreat && isEncore(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
            if (statusThreat && isDisable(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
            if (statusThreat && isTorment(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
            if (healingThreat && isHealBlock(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAISpeedControlMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        for (Map<String, Object> move : engine.moves(mon)) {
            if (engine.cooldown(mon, move) != 0 || !engine.canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isTailwind(move) && tailwindTurns(state, playerSide) == 0) {
                return move;
            }
            if (isTrickRoom(move) && trickRoomTurns(state) == 0) {
                return move;
            }
            if (isThunderWave(move) && analysisSupport.opposingSideLikelyFaster(state, playerSide)) {
                return move;
            }
            if (isIcyWind(move) && analysisSupport.opposingSideLikelyFaster(state, playerSide)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAIBurnMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        if (!analysisSupport.opposingSideLikelyPhysical(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isWillOWisp(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAIWeatherMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        for (Map<String, Object> move : engine.moves(mon)) {
            if (engine.cooldown(mon, move) != 0 || !engine.canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isRainDance(move) && weatherTurns(state) == 0
                    && analysisSupport.sidePrefersWeather(state, playerSide, DamageCalculatorUtil.TYPE_WATER)) {
                return move;
            }
            if (isSunnyDay(move) && weatherTurns(state) == 0
                    && analysisSupport.sidePrefersWeather(state, playerSide, DamageCalculatorUtil.TYPE_FIRE)) {
                return move;
            }
            if (isSandstorm(move) && weatherTurns(state) == 0
                    && (analysisSupport.sideHasType(state, playerSide, DamageCalculatorUtil.TYPE_ROCK)
                            || analysisSupport.sideHasType(state, playerSide, DamageCalculatorUtil.TYPE_GROUND)
                            || analysisSupport.sideHasType(state, playerSide, DamageCalculatorUtil.TYPE_STEEL))) {
                return move;
            }
            if (isSnowWeather(move) && weatherTurns(state) == 0
                    && analysisSupport.sideHasType(state, playerSide, DamageCalculatorUtil.TYPE_ICE)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAIRedirectionMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        if (engine.activeSlots(state, playerSide).size() < 2) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isRedirectionMove(move) && engine.cooldown(mon, move) == 0
                    && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    Map<String, Object> selectAIHelpingHandMove(Map<String, Object> mon, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        if (engine.activeSlots(state, playerSide).size() < 2) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isHelpingHand(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    /**
     * AI 决定是否使用特殊系统（太晶化/Z 招式/极巨化）。
     * 采用基于伤害预计算的决策树，确保资源利用效率最大化。
     */
    String selectAISpecialSystem(Map<String, Object> mon, Map<String, Object> state, boolean playerSide,
            Map<String, Object> selectedMove, int currentRound) {
        // 1. Z 招式收割判定：仅在能确保护杀（OHKO）时使用
        if (Boolean.TRUE.equals(mon.get("zMoveEligible")) && !Boolean.TRUE.equals(mon.get("zMoveUsed"))) {
            if (selectedMove != null && engine.toInt(selectedMove.get("power"), 0) > 60) {
                List<Map<String, Object>> opponentTeam = engine.team(state, !playerSide);
                List<Integer> activeSlots = engine.activeSlots(state, !playerSide);
                for (Integer slot : activeSlots) {
                    if (slot == null || slot >= opponentTeam.size())
                        continue;
                    Map<String, Object> target = opponentTeam.get(slot);
                    if (engine.toInt(target.get("currentHp"), 0) <= 0)
                        continue;

                    // 计算 Z 招式威力阶梯
                    int basePower = engine.toInt(selectedMove.get("power"), 0);
                    int zPower = basePower >= 140 ? 200
                            : (basePower >= 130 ? 195
                                    : (basePower >= 120 ? 190
                                            : (basePower >= 110 ? 185
                                                    : (basePower >= 100 ? 180
                                                            : (basePower >= 90 ? 175
                                                                    : (basePower >= 80 ? 160
                                                                            : (basePower >= 70 ? 140 : 100)))))));

                    Map<String, Object> zMove = new java.util.LinkedHashMap<>(selectedMove);
                    zMove.put("power", zPower);

                    // 模拟伤害计算
                    int predictedDamage = engine.calculateDamage(mon, target, zMove, new java.util.Random(),
                            new java.util.HashMap<>(), state);
                    if (predictedDamage >= engine.toInt(target.get("currentHp"), 0)) {
                        return "z-move"; // 确认 OHKO，立即执行收割
                    }
                }
            }
        }

        // 2. 太晶化属性克制与 STAB 强化：面临劣势或需要爆发时触发
        if (Boolean.TRUE.equals(mon.get("teraEligible")) && !Boolean.TRUE.equals(mon.get("terastallized"))) {
            if (selectedMove != null) {
                int moveTypeId = engine.toInt(selectedMove.get("type_id"), 0);
                int teraTypeId = engine.toInt(mon.get("teraTypeId"), 0);

                // 战术意图 A：通过太晶化获得同属性加成（STAB），确保击杀关键目标
                if (moveTypeId == teraTypeId) {
                    List<Map<String, Object>> opponentTeam = engine.team(state, !playerSide);
                    for (Map<String, Object> target : opponentTeam) {
                        if (engine.toInt(target.get("currentHp"), 0) <= 0)
                            continue;

                        // 模拟太晶化后的伤害（简化处理：假设太晶化后该招式威力提升 50%）
                        Map<String, Object> teraMove = new java.util.LinkedHashMap<>(selectedMove);
                        teraMove.put("power", engine.toInt(teraMove.get("power"), 0) * 1.5);

                        int predictedDamage = engine.calculateDamage(mon, target, teraMove, new java.util.Random(),
                                new java.util.HashMap<>(), state);
                        if (predictedDamage >= engine.toInt(target.get("currentHp"), 0)) {
                            return "tera"; // 太晶化后可实现击杀
                        }
                    }
                }

                // 战术意图 B：改变属性以规避敌方强攻属性的克制
                // （此处可进一步扩展：分析敌方场上所有招式的属性分布）
            }
        }

        // 3. 极巨化时机选择：血量健康且需要突破保护或开启场地/天气
        if (Boolean.TRUE.equals(mon.get("dynamaxEligible")) && !Boolean.TRUE.equals(mon.get("dynamaxed"))) {
            int hpPercent = (engine.toInt(mon.get("currentHp"), 0) * 100)
                    / engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);

            // 优先在 HP > 50% 且场上没有极巨化状态时使用
            if (hpPercent > 50) {
                // 检查是否需要极巨招式带来的副效果（如开启天气/场地）
                if (selectedMove != null && isMaxMoveWithBeneficialEffect(selectedMove, state, playerSide)) {
                    return "dynamax";
                }
                // 或者当对手使用了 Protect 等保护类招式时，极巨化可以无视保护
                if (isOpponentUsingProtect(state, playerSide)) {
                    return "dynamax";
                }
            }
        }

        return null;
    }

    /**
     * 判断极巨招式是否能带来关键的战场收益（如天气、场地变更）
     */
    private boolean isMaxMoveWithBeneficialEffect(Map<String, Object> move, Map<String, Object> state,
            boolean playerSide) {
        String typeName = String.valueOf(move.get("type_name_en"));
        Map<String, Object> fieldEffects = fieldEffects(state);

        // 如果当前没有对应的天气/场地，且极巨招式能开启它，则视为有益
        if ("water".equalsIgnoreCase(typeName) && engine.toInt(fieldEffects.get("rainTurns"), 0) == 0)
            return true;
        if ("fire".equalsIgnoreCase(typeName) && engine.toInt(fieldEffects.get("sunTurns"), 0) == 0)
            return true;
        if ("electric".equalsIgnoreCase(typeName) && engine.toInt(fieldEffects.get("electricTerrainTurns"), 0) == 0)
            return true;
        if ("grass".equalsIgnoreCase(typeName) && engine.toInt(fieldEffects.get("grassyTerrainTurns"), 0) == 0)
            return true;

        return false;
    }

    /**
     * 简单判定对手是否处于保护状态
     */
    private boolean isOpponentUsingProtect(Map<String, Object> state, boolean playerSide) {
        // 这里可以通过分析上一回合的动作日志来判断，目前先返回 false 占位
        return false;
    }

    private Map<String, Object> fieldEffects(Map<String, Object> state) {
        return engine.castMap(state.get("fieldEffects"));
    }

    private int tailwindTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerTailwindTurns" : "opponentTailwindTurns"), 0);
    }

    private int trickRoomTurns(Map<String, Object> state) {
        return engine.toInt(fieldEffects(state).get("trickRoomTurns"), 0);
    }

    private int weatherTurns(Map<String, Object> state) {
        Map<String, Object> effects = fieldEffects(state);
        return Math.max(
                Math.max(engine.toInt(effects.get("rainTurns"), 0), engine.toInt(effects.get("sunTurns"), 0)),
                Math.max(engine.toInt(effects.get("sandTurns"), 0), engine.toInt(effects.get("snowTurns"), 0)));
    }

    private int terrainTurns(Map<String, Object> state) {
        Map<String, Object> effects = fieldEffects(state);
        return Math.max(
                Math.max(engine.toInt(effects.get("electricTerrainTurns"), 0),
                        engine.toInt(effects.get("psychicTerrainTurns"), 0)),
                Math.max(engine.toInt(effects.get("grassyTerrainTurns"), 0),
                        engine.toInt(effects.get("mistyTerrainTurns"), 0)));
    }

    private int reflectTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerReflectTurns" : "opponentReflectTurns"), 0);
    }

    private int lightScreenTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns"),
                0);
    }

    private int safeguardTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerSafeguardTurns" : "opponentSafeguardTurns"), 0);
    }

    private boolean isFakeOut(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "fake-out".equalsIgnoreCase(nameEn) || "fake out".equalsIgnoreCase(nameEn);
    }

    private boolean isTaunt(Map<String, Object> move) {
        return "taunt".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isYawn(Map<String, Object> move) {
        return "yawn".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isEncore(Map<String, Object> move) {
        return "encore".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isDisable(Map<String, Object> move) {
        return "disable".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isTorment(Map<String, Object> move) {
        return "torment".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isHealBlock(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "heal-block".equalsIgnoreCase(nameEn) || "heal block".equalsIgnoreCase(nameEn);
    }

    private boolean isSpore(Map<String, Object> move) {
        return "spore".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isReflect(Map<String, Object> move) {
        return "reflect".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isLightScreen(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "light-screen".equalsIgnoreCase(nameEn) || "light screen".equalsIgnoreCase(nameEn);
    }

    private boolean isSafeguard(Map<String, Object> move) {
        return "safeguard".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isFollowMe(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "follow-me".equalsIgnoreCase(nameEn) || "follow me".equalsIgnoreCase(nameEn);
    }

    private boolean isRagePowder(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "rage-powder".equalsIgnoreCase(nameEn) || "rage powder".equalsIgnoreCase(nameEn);
    }

    private boolean isRedirectionMove(Map<String, Object> move) {
        return isFollowMe(move) || isRagePowder(move);
    }

    private boolean isHelpingHand(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "helping-hand".equalsIgnoreCase(nameEn) || "helping hand".equalsIgnoreCase(nameEn);
    }

    private boolean isTailwind(Map<String, Object> move) {
        return "tailwind".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isTrickRoom(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "trick-room".equalsIgnoreCase(nameEn) || "trick room".equalsIgnoreCase(nameEn);
    }

    private boolean isRainDance(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "rain-dance".equalsIgnoreCase(nameEn) || "rain dance".equalsIgnoreCase(nameEn);
    }

    private boolean isSunnyDay(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "sunny-day".equalsIgnoreCase(nameEn) || "sunny day".equalsIgnoreCase(nameEn);
    }

    private boolean isSandstorm(Map<String, Object> move) {
        return "sandstorm".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isSnowWeather(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "snowscape".equalsIgnoreCase(nameEn) || "hail".equalsIgnoreCase(nameEn);
    }

    private boolean isElectricTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "electric-terrain".equalsIgnoreCase(nameEn) || "electric terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isPsychicTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "psychic-terrain".equalsIgnoreCase(nameEn) || "psychic terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isGrassyTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "grassy-terrain".equalsIgnoreCase(nameEn) || "grassy terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isMistyTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "misty-terrain".equalsIgnoreCase(nameEn) || "misty terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isIcyWind(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "icy-wind".equalsIgnoreCase(nameEn) || "icy wind".equalsIgnoreCase(nameEn);
    }

    private boolean isThunderWave(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "thunder-wave".equalsIgnoreCase(nameEn) || "thunder wave".equalsIgnoreCase(nameEn);
    }

    private boolean isWillOWisp(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "will-o-wisp".equalsIgnoreCase(nameEn) || "will o wisp".equalsIgnoreCase(nameEn);
    }
}
