package com.lio9.battle.engine;

import com.lio9.pokedex.util.DamageCalculatorUtil;

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
        if (!analysisSupport.opposingSideCanBeSlept(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isSpore(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
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
                    || analysisSupport.sideHasGroundedType(state, playerSide, DamageCalculatorUtil.TYPE_GRASS))) {
                return move;
            }
            if (isMistyTerrain(move)
                    && (analysisSupport.sideHasGroundedType(state, playerSide, DamageCalculatorUtil.TYPE_FAIRY)
                    || analysisSupport.opposingSideLikelyUsingStatus(state, playerSide)
                    || analysisSupport.opposingSideLikelyUsingType(state, playerSide, DamageCalculatorUtil.TYPE_DRAGON))) {
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
        }
        return null;
    }

    Map<String, Object> selectAITauntMove(Map<String, Object> mon, Map<String, Object> state,
                                          boolean playerSide, int currentRound) {
        if (!analysisSupport.opposingSideLikelyUsingStatus(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(mon)) {
            if (isTaunt(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
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
            if (isRedirectionMove(move) && engine.cooldown(mon, move) == 0 && engine.canUseMove(mon, move, currentRound)) {
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
                Math.max(engine.toInt(effects.get("sandTurns"), 0), engine.toInt(effects.get("snowTurns"), 0))
        );
    }

    private int terrainTurns(Map<String, Object> state) {
        Map<String, Object> effects = fieldEffects(state);
        return Math.max(
                Math.max(engine.toInt(effects.get("electricTerrainTurns"), 0), engine.toInt(effects.get("psychicTerrainTurns"), 0)),
                Math.max(engine.toInt(effects.get("grassyTerrainTurns"), 0), engine.toInt(effects.get("mistyTerrainTurns"), 0))
        );
    }

    private int reflectTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerReflectTurns" : "opponentReflectTurns"), 0);
    }

    private int lightScreenTurns(Map<String, Object> state, boolean playerSide) {
        return engine.toInt(fieldEffects(state).get(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns"), 0);
    }

    private boolean isFakeOut(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "fake-out".equalsIgnoreCase(nameEn) || "fake out".equalsIgnoreCase(nameEn);
    }

    private boolean isTaunt(Map<String, Object> move) {
        return "taunt".equalsIgnoreCase(String.valueOf(move.get("name_en")));
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