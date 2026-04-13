package com.lio9.battle.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleFieldEffectSupport {
    Map<String, Object> defaultFieldEffects() {
        Map<String, Object> effects = new LinkedHashMap<>();
        effects.put("playerTailwindTurns", 0);
        effects.put("opponentTailwindTurns", 0);
        effects.put("trickRoomTurns", 0);
        effects.put("rainTurns", 0);
        effects.put("sunTurns", 0);
        effects.put("sandTurns", 0);
        effects.put("snowTurns", 0);
        effects.put("electricTerrainTurns", 0);
        effects.put("psychicTerrainTurns", 0);
        effects.put("grassyTerrainTurns", 0);
        effects.put("mistyTerrainTurns", 0);
        effects.put("playerReflectTurns", 0);
        effects.put("opponentReflectTurns", 0);
        effects.put("playerLightScreenTurns", 0);
        effects.put("opponentLightScreenTurns", 0);
        return effects;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> fieldEffects(Map<String, Object> state) {
        Object value = state.get("fieldEffects");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> created = defaultFieldEffects();
        state.put("fieldEffects", created);
        return created;
    }

    int tailwindTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerTailwindTurns" : "opponentTailwindTurns"), 0);
    }

    int trickRoomTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("trickRoomTurns"), 0);
    }

    int rainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("rainTurns"), 0);
    }

    int sunTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("sunTurns"), 0);
    }

    int sandTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("sandTurns"), 0);
    }

    int snowTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("snowTurns"), 0);
    }

    int electricTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("electricTerrainTurns"), 0);
    }

    int psychicTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("psychicTerrainTurns"), 0);
    }

    int grassyTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("grassyTerrainTurns"), 0);
    }

    int mistyTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("mistyTerrainTurns"), 0);
    }

    int weatherTurns(Map<String, Object> state) {
        return Math.max(Math.max(rainTurns(state), sunTurns(state)), Math.max(sandTurns(state), snowTurns(state)));
    }

    int terrainTurns(Map<String, Object> state) {
        return Math.max(Math.max(electricTerrainTurns(state), psychicTerrainTurns(state)), Math.max(grassyTerrainTurns(state), mistyTerrainTurns(state)));
    }

    int reflectTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerReflectTurns" : "opponentReflectTurns"), 0);
    }

    int lightScreenTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns"), 0);
    }

    void activateTailwind(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffects(state).put(playerSide ? "playerTailwindTurns" : "opponentTailwindTurns", 4);
        actionLog.put("result", "tailwind");
        events.add(actor.get("name") + " 刮起了顺风");
    }

    void toggleTrickRoom(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        int current = trickRoomTurns(state);
        if (current > 0) {
            fieldEffects(state).put("trickRoomTurns", 0);
            actionLog.put("result", "trick-room-ended");
            events.add(actor.get("name") + " 让戏法空间恢复了正常");
            return;
        }
        fieldEffects(state).put("trickRoomTurns", 5);
        actionLog.put("result", "trick-room");
        events.add(actor.get("name") + " 扭曲了空间，戏法空间生效了");
    }

    void activateWeather(Map<String, Object> state, String weather, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        Map<String, Object> effects = fieldEffects(state);
        effects.put("rainTurns", 0);
        effects.put("sunTurns", 0);
        effects.put("sandTurns", 0);
        effects.put("snowTurns", 0);
        int duration = weatherDuration(actor, weather);
        switch (weather) {
            case "rain" -> {
                effects.put("rainTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "rain");
                }
                events.add(actor.get("name") + " 让大雨落了下来");
            }
            case "sun" -> {
                effects.put("sunTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "sun");
                }
                events.add(actor.get("name") + " 让阳光变得炽烈了");
            }
            case "sand" -> {
                effects.put("sandTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "sandstorm");
                }
                events.add(actor.get("name") + " 掀起了沙暴");
            }
            default -> {
                effects.put("snowTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "snow");
                }
                events.add(actor.get("name") + " 让雪景覆盖了场地");
            }
        }
    }

    void activateTerrain(Map<String, Object> state, String terrain, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        Map<String, Object> effects = fieldEffects(state);
        effects.put("electricTerrainTurns", 0);
        effects.put("psychicTerrainTurns", 0);
        effects.put("grassyTerrainTurns", 0);
        effects.put("mistyTerrainTurns", 0);
        int duration = terrainDuration(actor);
        switch (terrain) {
            case "electric" -> {
                effects.put("electricTerrainTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "electric-terrain");
                }
                events.add(actor.get("name") + " 让电气场地展开了");
            }
            case "psychic" -> {
                effects.put("psychicTerrainTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "psychic-terrain");
                }
                events.add(actor.get("name") + " 让精神场地展开了");
            }
            case "grassy" -> {
                effects.put("grassyTerrainTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "grassy-terrain");
                }
                events.add(actor.get("name") + " 让青草场地展开了");
            }
            default -> {
                effects.put("mistyTerrainTurns", duration);
                if (actionLog != null) {
                    actionLog.put("result", "misty-terrain");
                }
                events.add(actor.get("name") + " 让薄雾场地展开了");
            }
        }
    }

    void activateScreen(Map<String, Object> state, String screen, boolean playerSide, Map<String, Object> actor,
                        Map<String, Object> actionLog, List<String> events) {
        if ("reflect".equals(screen)) {
            fieldEffects(state).put(playerSide ? "playerReflectTurns" : "opponentReflectTurns", 5);
            actionLog.put("result", "reflect");
            events.add(actor.get("name") + " 展开了反射壁");
            return;
        }
        fieldEffects(state).put(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns", 5);
        actionLog.put("result", "light-screen");
        events.add(actor.get("name") + " 展开了光墙");
    }

    void decrementFieldEffects(Map<String, Object> state, Map<String, Object> fieldSnapshot, List<String> events) {
        decrementFieldEffect(state, fieldSnapshot, "playerTailwindTurns", "我方顺风结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentTailwindTurns", "对手顺风结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "trickRoomTurns", "戏法空间结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "rainTurns", "雨停了", events);
        decrementFieldEffect(state, fieldSnapshot, "sunTurns", "炽烈的阳光消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "sandTurns", "沙暴平息了", events);
        decrementFieldEffect(state, fieldSnapshot, "snowTurns", "雪景消散了", events);
        decrementFieldEffect(state, fieldSnapshot, "electricTerrainTurns", "电气场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "psychicTerrainTurns", "精神场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "grassyTerrainTurns", "青草场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "mistyTerrainTurns", "薄雾场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "playerReflectTurns", "我方反射壁消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentReflectTurns", "对手反射壁消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "playerLightScreenTurns", "我方光墙消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentLightScreenTurns", "对手光墙消失了", events);
    }

    private void decrementFieldEffect(Map<String, Object> state, Map<String, Object> fieldSnapshot, String key, String endMessage, List<String> events) {
        int before = toInt(fieldSnapshot.get(key), 0);
        if (before <= 0) {
            return;
        }
        int after = Math.max(0, before - 1);
        fieldEffects(state).put(key, after);
        if (after == 0) {
            events.add(endMessage);
        }
    }

    private int weatherDuration(Map<String, Object> actor, String weather) {
        String item = heldItem(actor);
        return switch (weather) {
            case "rain" -> "damp-rock".equals(item) ? 8 : 5;
            case "sun" -> "heat-rock".equals(item) ? 8 : 5;
            case "sand" -> "smooth-rock".equals(item) ? 8 : 5;
            case "snow" -> "icy-rock".equals(item) ? 8 : 5;
            default -> 5;
        };
    }

    private int terrainDuration(Map<String, Object> actor) {
        return "terrain-extender".equals(heldItem(actor)) ? 8 : 5;
    }

    private String heldItem(Map<String, Object> actor) {
        Object item = actor.get("heldItem");
        return item == null ? "" : String.valueOf(item);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}