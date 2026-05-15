package com.lio9.battle.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BattleFieldEffectSupportTest {

    private final BattleFieldEffectSupport fe = new BattleFieldEffectSupport();

    private Map<String, Object> state() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("fieldEffects", new LinkedHashMap<>(fe.defaultFieldEffects()));
        return s;
    }

    private static Map<String, Object> actionLog() {
        return new LinkedHashMap<>();
    }

    @Test
    void defaultFieldEffects_allKeysPresent() {
        Map<String, Object> def = fe.defaultFieldEffects();
        assertEquals(0, def.get("rainTurns"));
        assertEquals(0, def.get("sunTurns"));
        assertEquals(0, def.get("sandTurns"));
        assertEquals(0, def.get("snowTurns"));
        assertEquals(0, def.get("electricTerrainTurns"));
        assertEquals(0, def.get("psychicTerrainTurns"));
        assertEquals(0, def.get("grassyTerrainTurns"));
        assertEquals(0, def.get("mistyTerrainTurns"));
        assertEquals(0, def.get("trickRoomTurns"));
        assertEquals(0, def.get("playerTailwindTurns"));
        assertEquals(0, def.get("opponentTailwindTurns"));
        assertEquals(0, def.get("playerReflectTurns"));
        assertEquals(0, def.get("opponentReflectTurns"));
        assertEquals(0, def.get("playerLightScreenTurns"));
        assertEquals(0, def.get("opponentLightScreenTurns"));
        assertEquals(0, def.get("playerAuroraVeilTurns"));
        assertEquals(0, def.get("opponentAuroraVeilTurns"));
        assertEquals(0, def.get("playerSafeguardTurns"));
        assertEquals(0, def.get("opponentSafeguardTurns"));
        assertFalse((Boolean) def.get("playerStealthRock"));
        assertFalse((Boolean) def.get("opponentStealthRock"));
        assertEquals(0, def.get("playerSpikesLayers"));
        assertEquals(0, def.get("opponentSpikesLayers"));
        assertEquals(0, def.get("playerToxicSpikesLayers"));
        assertEquals(0, def.get("opponentToxicSpikesLayers"));
        assertFalse((Boolean) def.get("playerStickyWeb"));
        assertFalse((Boolean) def.get("opponentStickyWeb"));
        assertEquals(0, def.get("gmaxWildfireTurns"));
        assertEquals(0, def.get("gmaxCannonadeTurns"));
        assertEquals(0, def.get("gmaxVineLashTurns"));
        assertEquals(0, def.get("gravityTurns"));
        assertEquals(0, def.get("magicRoomTurns"));
    }

    @Test
    void fieldEffects_returnsExisting() {
        Map<String, Object> s = state();
        assertSame(s.get("fieldEffects"), fe.fieldEffects(s));
    }

    @Test
    void fieldEffects_createsNewWhenMissing() {
        Map<String, Object> s = new LinkedHashMap<>();
        Map<String, Object> effects = fe.fieldEffects(s);
        assertNotNull(effects);
        assertSame(effects, s.get("fieldEffects"));
    }

    @Test
    void activateWeather_rain() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.activateWeather(s, "rain", actor("Pelipper"), actionLog(), events);
        assertEquals(5, fe.rainTurns(s));
        assertEquals(0, fe.sunTurns(s));
        assertTrue(events.stream().anyMatch(e -> e.contains("大雨")));
    }

    @Test
    void activateWeather_clearsPreviousWeather() {
        Map<String, Object> s = state();
        fe.activateWeather(s, "sun", actor("Torkoal"), actionLog(), new ArrayList<>());
        fe.activateWeather(s, "rain", actor("Pelipper"), actionLog(), new ArrayList<>());
        assertEquals(0, fe.sunTurns(s));
        assertTrue(fe.rainTurns(s) > 0);
    }

    @Test
    void activateWeather_durationExtendedByRock() {
        Map<String, Object> s = state();
        Map<String, Object> actor = actor("Tyranitar");
        actor.put("heldItem", "smooth-rock");
        fe.activateWeather(s, "sand", actor, actionLog(), new ArrayList<>());
        assertEquals(8, fe.sandTurns(s));
    }

    @Test
    void activateTerrain_grassy() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.activateTerrain(s, "grassy", actor("Rillaboom"), actionLog(), events);
        assertTrue(fe.grassyTerrainTurns(s) > 0);
        assertTrue(events.stream().anyMatch(e -> e.contains("青草场地")));
    }

    @Test
    void activateTerrain_clearsPreviousTerrain() {
        Map<String, Object> s = state();
        fe.activateTerrain(s, "electric", actor("Regieleki"), actionLog(), new ArrayList<>());
        fe.activateTerrain(s, "grassy", actor("Rillaboom"), actionLog(), new ArrayList<>());
        assertEquals(0, fe.electricTerrainTurns(s));
        assertTrue(fe.grassyTerrainTurns(s) > 0);
    }

    @Test
    void activateTerrain_terrainExtender() {
        Map<String, Object> s = state();
        Map<String, Object> actor = actor("Pincurchin");
        actor.put("heldItem", "terrain-extender");
        fe.activateTerrain(s, "electric", actor, actionLog(), new ArrayList<>());
        assertEquals(8, fe.electricTerrainTurns(s));
    }

    @Test
    void activateScreen_reflect() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.activateScreen(s, "reflect", true, actor("Mr. Mime"), actionLog(), events);
        assertTrue(fe.reflectTurns(s, true) > 0);
        assertEquals(0, fe.reflectTurns(s, false));
        assertTrue(events.stream().anyMatch(e -> e.contains("反射壁")));
    }

    @Test
    void activateScreen_lightScreen() {
        Map<String, Object> s = state();
        fe.activateScreen(s, "light-screen", false, actor("Mr. Mime"), actionLog(), new ArrayList<>());
        assertTrue(fe.lightScreenTurns(s, false) > 0);
        assertEquals(0, fe.lightScreenTurns(s, true));
    }

    @Test
    void activateScreen_auroraVeil() {
        Map<String, Object> s = state();
        fe.activateScreen(s, "aurora-veil", true, actor("Mr. Mime"), actionLog(), new ArrayList<>());
        assertTrue(fe.auroraVeilTurns(s, true) > 0);
    }

    @Test
    void activateScreen_safeguard() {
        Map<String, Object> s = state();
        fe.activateScreen(s, "safeguard", false, actor("Mr. Mime"), actionLog(), new ArrayList<>());
        assertTrue(fe.safeguardTurns(s, false) > 0);
    }

    @Test
    void activateScreen_lightClayExtendsDuration() {
        Map<String, Object> s = state();
        Map<String, Object> actor = actor("Mr. Mime");
        actor.put("heldItem", "light-clay");
        fe.activateScreen(s, "reflect", true, actor, actionLog(), new ArrayList<>());
        assertEquals(8, fe.reflectTurns(s, true));
    }

    @Test
    void activateTailwind() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.activateTailwind(s, true, actor("Tornadus"), actionLog(), events);
        assertEquals(4, fe.tailwindTurns(s, true));
        assertEquals(0, fe.tailwindTurns(s, false));
        assertTrue(events.stream().anyMatch(e -> e.contains("顺风")));
    }

    @Test
    void toggleTrickRoom_activates() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.toggleTrickRoom(s, actor("Bronzong"), actionLog(), events);
        assertTrue(fe.trickRoomTurns(s) > 0);
        assertTrue(events.stream().anyMatch(e -> e.contains("戏法空间")));
    }

    @Test
    void toggleTrickRoom_deactivates() {
        Map<String, Object> s = state();
        fe.toggleTrickRoom(s, actor("Bronzong"), actionLog(), new ArrayList<>());
        List<String> events = new ArrayList<>();
        fe.toggleTrickRoom(s, actor("Bronzong"), actionLog(), events);
        assertEquals(0, fe.trickRoomTurns(s));
        assertTrue(events.stream().anyMatch(e -> e.contains("恢复了正常")));
    }

    @Test
    void setStealthRock() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.setStealthRock(s, true, actor("Hippowdon"), actionLog(), events);
        assertTrue(fe.hasStealthRock(s, true));
        assertFalse(fe.hasStealthRock(s, false));
        assertTrue(events.stream().anyMatch(e -> e.contains("隐形岩")));
    }

    @Test
    void setStealthRock_failsIfAlreadyPresent() {
        Map<String, Object> s = state();
        fe.setStealthRock(s, true, actor("Hippowdon"), actionLog(), new ArrayList<>());
        List<String> events = new ArrayList<>();
        fe.setStealthRock(s, true, actor("Hippowdon"), actionLog(), events);
        assertTrue(events.stream().anyMatch(e -> e.contains("已存在")));
    }

    @Test
    void addSpikesLayer_upTo3() {
        Map<String, Object> s = state();
        for (int i = 1; i <= 3; i++) {
            fe.addSpikesLayer(s, false, actor("Ferrothorn"), actionLog(), new ArrayList<>());
            assertEquals(i, fe.getSpikesLayers(s, false));
        }
        // 超过 3 层失败
        fe.addSpikesLayer(s, false, actor("Ferrothorn"), actionLog(), new ArrayList<>());
        assertEquals(3, fe.getSpikesLayers(s, false));
    }

    @Test
    void addToxicSpikesLayer_upTo2() {
        Map<String, Object> s = state();
        fe.addToxicSpikesLayer(s, true, actor("Toxapex"), actionLog(), new ArrayList<>());
        assertEquals(1, fe.getToxicSpikesLayers(s, true));
        fe.addToxicSpikesLayer(s, true, actor("Toxapex"), actionLog(), new ArrayList<>());
        assertEquals(2, fe.getToxicSpikesLayers(s, true));
        fe.addToxicSpikesLayer(s, true, actor("Toxapex"), actionLog(), new ArrayList<>());
        assertEquals(2, fe.getToxicSpikesLayers(s, true));
    }

    @Test
    void setStickyWeb() {
        Map<String, Object> s = state();
        fe.setStickyWeb(s, false, actor("Araquanid"), actionLog(), new ArrayList<>());
        assertTrue(fe.hasStickyWeb(s, false));
        assertFalse(fe.hasStickyWeb(s, true));
    }

    @Test
    void clearScreens_removesAllScreensForSide() {
        Map<String, Object> s = state();
        fe.activateScreen(s, "reflect", true, actor("Mr Mime"), actionLog(), new ArrayList<>());
        fe.activateScreen(s, "light-screen", true, actor("Mr Mime"), actionLog(), new ArrayList<>());
        fe.activateScreen(s, "aurora-veil", true, actor("Mr Mime"), actionLog(), new ArrayList<>());
        fe.clearScreens(s, true);
        assertEquals(0, fe.reflectTurns(s, true));
        assertEquals(0, fe.lightScreenTurns(s, true));
        assertEquals(0, fe.auroraVeilTurns(s, true));
    }

    @Test
    void clearScreens_onlyClearsSpecifiedSide() {
        Map<String, Object> s = state();
        fe.activateScreen(s, "reflect", true, actor("A"), actionLog(), new ArrayList<>());
        fe.activateScreen(s, "reflect", false, actor("B"), actionLog(), new ArrayList<>());
        fe.clearScreens(s, true);
        assertTrue(fe.reflectTurns(s, false) > 0);
    }

    @Test
    void clearSideHazards_clearsAll() {
        Map<String, Object> s = state();
        fe.setStealthRock(s, true, actor("Hippowdon"), actionLog(), new ArrayList<>());
        fe.addSpikesLayer(s, true, actor("Ferrothorn"), actionLog(), new ArrayList<>());
        fe.addToxicSpikesLayer(s, true, actor("Toxapex"), actionLog(), new ArrayList<>());
        fe.clearSideHazards(s, true);
        assertFalse(fe.hasStealthRock(s, true));
        assertEquals(0, fe.getSpikesLayers(s, true));
        assertEquals(0, fe.getToxicSpikesLayers(s, true));
    }

    @Test
    void decrementFieldEffects_decrements() {
        Map<String, Object> s = state();
        fe.activateWeather(s, "rain", actor("Pelipper"), actionLog(), new ArrayList<>());
        fe.activateTerrain(s, "electric", actor("Regieleki"), actionLog(), new ArrayList<>());
        int rainBefore = fe.rainTurns(s);
        int terrainBefore = fe.electricTerrainTurns(s);

        // 使用当前 fieldEffects 作为快照（模拟引擎行为）
        Map<String, Object> snapshot = new LinkedHashMap<>(fe.fieldEffects(s));
        List<String> events = new ArrayList<>();
        fe.decrementFieldEffects(s, snapshot, events);
        assertEquals(rainBefore - 1, fe.rainTurns(s));
        assertEquals(terrainBefore - 1, fe.electricTerrainTurns(s));
    }

    @Test
    void decrementFieldEffects_sendsEndMessageWhenReachesZero() {
        Map<String, Object> s = state();
        fe.fieldEffects(s).put("rainTurns", 1);
        Map<String, Object> snapshot = new LinkedHashMap<>(fe.fieldEffects(s));
        List<String> events = new ArrayList<>();
        fe.decrementFieldEffects(s, snapshot, events);
        assertEquals(0, fe.rainTurns(s));
        assertTrue(events.stream().anyMatch(e -> e.contains("雨停了")));
    }

    @Test
    void gmaxPersistentDamageSetters() {
        Map<String, Object> s = state();
        fe.setGMaxWildfire(s, true);
        assertEquals(4, fe.fieldEffects(s).get("gmaxWildfireTurns"));
        fe.setGMaxCannonade(s, false);
        assertEquals(4, fe.fieldEffects(s).get("gmaxCannonadeTurns"));
        fe.setGMaxVineLash(s, true);
        assertEquals(4, fe.fieldEffects(s).get("gmaxVineLashTurns"));
    }

    @Test
    void gravity() {
        Map<String, Object> s = state();
        List<String> events = new ArrayList<>();
        fe.activateGravity(s, actor("Clefable"), actionLog(), events);
        assertTrue(fe.gravityTurns(s) > 0);
        assertTrue(events.stream().anyMatch(e -> e.contains("重力")));
    }

    @Test
    void magicRoom() {
        Map<String, Object> s = state();
        fe.fieldEffects(s).put("magicRoomTurns", 5);
        assertEquals(5, fe.magicRoomTurns(s));
    }

    @Test
    void futureSight_setGet() {
        Map<String, Object> s = state();
        Map<String, Object> data = new LinkedHashMap<>(Map.of("turns", 3, "damage", 100, "moveName", "预知未来"));
        fe.setFutureSight(s, true, data);
        Map<String, Object> retrieved = fe.getFutureSight(s, true);
        assertNotNull(retrieved);
        assertEquals(3, retrieved.get("turns"));
        assertNull(fe.getFutureSight(s, false));
    }

    @Test
    void futureSight_damageTriggersWhenTurnsReachZero() {
        Map<String, Object> s = state();
        Map<String, Object> mon = new LinkedHashMap<>(Map.of("name", "Target", "currentHp", 500,
                "stats", Map.of("hp", 500)));
        // Future Sight 由对手方释放，伤害目标为我方
        s.put("playerTeam", List.of(mon));
        s.put("playerActiveSlots", List.of(0));

        Map<String, Object> data = new LinkedHashMap<>(Map.of("turns", 2, "damage", 100,
                "moveName", "预知未来", "attackerName", "Alakazam"));
        fe.setFutureSight(s, false, data);

        // turns 2→1, still >0, no trigger
        assertFalse(fe.processFutureSightDamage(s, false, new ArrayList<>()));
        // turns 1→0, triggers damage
        List<String> events = new ArrayList<>();
        assertTrue(fe.processFutureSightDamage(s, false, events));
        assertTrue(events.stream().anyMatch(e -> e.contains("预知未来")));
    }

    @Test
    void weatherTurns_returnsMax() {
        Map<String, Object> s = state();
        fe.activateWeather(s, "rain", actor("Pelipper"), actionLog(), new ArrayList<>());
        assertEquals(0, fe.sunTurns(s));
        assertEquals(0, fe.sandTurns(s));
        assertEquals(0, fe.snowTurns(s));
        assertEquals(fe.rainTurns(s), fe.weatherTurns(s));
    }

    @Test
    void terrainTurns_returnsMax() {
        Map<String, Object> s = state();
        fe.activateTerrain(s, "grassy", actor("Rillaboom"), actionLog(), new ArrayList<>());
        assertEquals(fe.grassyTerrainTurns(s), fe.terrainTurns(s));
    }

    private static Map<String, Object> actor(String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("heldItem", "");
        m.put("currentHp", 100);
        return m;
    }
}
