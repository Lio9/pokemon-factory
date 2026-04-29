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
        effects.put("playerAuroraVeilTurns", 0);
        effects.put("opponentAuroraVeilTurns", 0);
        effects.put("playerSafeguardTurns", 0);
        effects.put("opponentSafeguardTurns", 0);
        
        // Entry hazards (Pokemon Showdown standard)
        effects.put("playerStealthRock", false);
        effects.put("opponentStealthRock", false);
        effects.put("playerSpikesLayers", 0);  // 0-3 layers
        effects.put("opponentSpikesLayers", 0);
        effects.put("playerToxicSpikesLayers", 0);  // 0-2 layers
        effects.put("opponentToxicSpikesLayers", 0);
        effects.put("playerStickyWeb", false);
        effects.put("opponentStickyWeb", false);
        
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

    int auroraVeilTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerAuroraVeilTurns" : "opponentAuroraVeilTurns"), 0);
    }

    int safeguardTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerSafeguardTurns" : "opponentSafeguardTurns"), 0);
    }

    // Entry hazards getters
    boolean hasStealthRock(Map<String, Object> state, boolean playerSide) {
        return Boolean.TRUE.equals(fieldEffects(state).get(playerSide ? "playerStealthRock" : "opponentStealthRock"));
    }

    int getSpikesLayers(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerSpikesLayers" : "opponentSpikesLayers"), 0);
    }

    int getToxicSpikesLayers(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerToxicSpikesLayers" : "opponentToxicSpikesLayers"), 0);
    }

    boolean hasStickyWeb(Map<String, Object> state, boolean playerSide) {
        return Boolean.TRUE.equals(fieldEffects(state).get(playerSide ? "playerStickyWeb" : "opponentStickyWeb"));
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
        int duration = screenDuration(actor);
        if ("reflect".equals(screen)) {
            fieldEffects(state).put(playerSide ? "playerReflectTurns" : "opponentReflectTurns", duration);
            actionLog.put("result", "reflect");
            events.add(actor.get("name") + " 展开了反射壁");
            return;
        }
        if ("aurora-veil".equals(screen)) {
            fieldEffects(state).put(playerSide ? "playerAuroraVeilTurns" : "opponentAuroraVeilTurns", duration);
            actionLog.put("result", "aurora-veil");
            events.add(actor.get("name") + " 展开了极光幕");
            return;
        }
        if ("safeguard".equals(screen)) {
            fieldEffects(state).put(playerSide ? "playerSafeguardTurns" : "opponentSafeguardTurns", duration);
            actionLog.put("result", "safeguard");
            events.add(actor.get("name") + " 展开了神秘守护");
            return;
        }
        fieldEffects(state).put(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns", duration);
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
        decrementFieldEffect(state, fieldSnapshot, "playerAuroraVeilTurns", "我方极光幕消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentAuroraVeilTurns", "对手极光幕消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "playerSafeguardTurns", "我方神秘守护消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentSafeguardTurns", "对手神秘守护消失了", events);
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

    private int screenDuration(Map<String, Object> actor) {
        return "light-clay".equals(heldItem(actor)) ? 8 : 5;
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

    // Entry hazards setters (Pokemon Showdown standard)
    void setStealthRock(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        if (hasStealthRock(state, playerSide)) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了隐形岩，但失败了（已存在）");
            return;
        }
        fieldEffects(state).put(playerSide ? "playerStealthRock" : "opponentStealthRock", true);
        actionLog.put("result", "stealth-rock");
        events.add(actor.get("name") + " 撒下了隐形岩！");
    }

    void addSpikesLayer(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        int currentLayers = getSpikesLayers(state, playerSide);
        if (currentLayers >= 3) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了撒菱，但失败了（已达最大层数）");
            return;
        }
        int newLayers = currentLayers + 1;
        fieldEffects(state).put(playerSide ? "playerSpikesLayers" : "opponentSpikesLayers", newLayers);
        actionLog.put("result", "spikes");
        actionLog.put("layers", newLayers);
        events.add(actor.get("name") + " 撒下了撒菱！（" + newLayers + "/3层）");
    }

    void addToxicSpikesLayer(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        int currentLayers = getToxicSpikesLayers(state, playerSide);
        if (currentLayers >= 2) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了毒菱，但失败了（已达最大层数）");
            return;
        }
        int newLayers = currentLayers + 1;
        fieldEffects(state).put(playerSide ? "playerToxicSpikesLayers" : "opponentToxicSpikesLayers", newLayers);
        actionLog.put("result", "toxic-spikes");
        actionLog.put("layers", newLayers);
        String layerText = newLayers == 1 ? "普通中毒" : "剧毒";
        events.add(actor.get("name") + " 撒下了毒菱！（" + newLayers + "/2层，" + layerText + "）");
    }

    void setStickyWeb(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        if (hasStickyWeb(state, playerSide)) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了黏黏网，但失败了（已存在）");
            return;
        }
        fieldEffects(state).put(playerSide ? "playerStickyWeb" : "opponentStickyWeb", true);
        actionLog.put("result", "sticky-web");
        events.add(actor.get("name") + " 布下了黏黏网！");
    }
}
