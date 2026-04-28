package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEngineRegressionBaselineTest {

    @Test
    void playRound_thunderInRainUsesWeatherAccuracyRule() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(singleTeamJson("Rain-A", 180, 80, "", ""),
                singleTeamJson("Target-O", 180, 100, "", ""), 8, 11L);
        keepSinglesOnly(state);
        setMoves(state, true, 0, List.of(move("Thunder", "thunder", 110, 50, 0, 2, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setWeatherTurns(state, "rainTurns", 5);

        Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "thunder", "target-slot-0", "0"));
        Map<String, Object> thunder = findAction(lastActions(updated), "player", "Thunder");
        assertEquals("hit", thunder.get("result"));
    }

    @Test
    void playRound_noGuardIgnoresLowAccuracy() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(singleTeamJson("NoGuard-A", 180, 80, "", "no-guard"),
                singleTeamJson("Target-O", 180, 100, "", ""), 8, 12L);
        keepSinglesOnly(state);
        setMoves(state, true, 0, List.of(move("Wild Swing", "wild-swing", 70, 1, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "wild-swing", "target-slot-0", "0"));
        Map<String, Object> hit = findAction(lastActions(updated), "player", "Wild Swing");
        assertEquals("hit", hit.get("result"));
    }

    @Test
    void playRound_quickClawCanOverrideActionOrder() {
        BattleEngine engine = createEngine();
        boolean triggered = false;
        for (long seed = 1; seed <= 300; seed++) {
            Map<String, Object> state = engine.createBattleState(singleTeamJson("Claw-A", 180, 50, "quick-claw", ""),
                    singleTeamJson("Fast-O", 180, 140, "", ""), 8, seed);
            keepSinglesOnly(state);
            setMoves(state, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
            setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
            Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
            List<Map<String, Object>> actions = lastActions(updated);
            if (actions.isEmpty()) {
                continue;
            }
            Map<String, Object> first = actions.get(0);
            if ("player".equals(first.get("side")) && "quick-claw".equals(first.get("orderSource"))) {
                triggered = true;
                break;
            }
        }
        assertTrue(triggered);
    }

    @Test
    void playRound_custapBerryBoostsOrderAndIsConsumed() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(singleTeamJson("Custap-A", 200, 60, "custap-berry", ""),
                singleTeamJson("Fast-O", 200, 120, "", ""), 8, 21L);
        keepSinglesOnly(state);
        setCurrentHp(state, true, 0, 40);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
        List<Map<String, Object>> actions = lastActions(updated);
        assertEquals("player", actions.get(0).get("side"));
        assertEquals("custap-berry", actions.get(0).get("orderSource"));
        assertTrue(Boolean.TRUE.equals(team(updated, true).get(0).get("itemConsumed")));
    }

    @Test
    void playRound_confusionIsMirroredToVolatiles() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(singleTeamJson("Confuser-A", 180, 100, "", ""),
                singleTeamJson("Target-O", 180, 80, "", ""), 8, 31L);
        keepSinglesOnly(state);
        setMoves(state, true, 0, List.of(move("Confuse Ray", "confuse-ray", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GHOST, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "confuse-ray", "target-slot-0", "0"));
        Map<String, Object> target = team(updated, false).get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> volatiles = (Map<String, Object>) target.get("volatiles");
        assertTrue(Boolean.TRUE.equals(target.get("confused")));
        assertTrue(Boolean.TRUE.equals(volatiles.get("confused")));
        assertEquals(target.get("confusionTurns"), volatiles.get("confusionTurns"));
    }

    @Test
    void playRound_yawnUsesVolatileBackfillAfterEndTurnTick() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(singleTeamJson("Yawn-A", 180, 100, "", ""),
                singleTeamJson("Target-O", 180, 80, "", ""), 8, 32L);
        keepSinglesOnly(state);
        setMoves(state, true, 0, List.of(move("Yawn", "yawn", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "yawn", "target-slot-0", "0"));
        Map<String, Object> target = team(updated, false).get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> volatiles = (Map<String, Object>) target.get("volatiles");
        assertEquals(1, target.get("yawnTurns"));
        assertEquals(1, volatiles.get("yawnTurns"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
    }

    private static void keepSinglesOnly(Map<String, Object> state) {
        state.put("playerActiveSlots", List.of(0));
        state.put("opponentActiveSlots", List.of(0));
    }

    private static void setCurrentHp(Map<String, Object> state, boolean player, int teamIndex, int hp) {
        team(state, player).get(teamIndex).put("currentHp", hp);
    }

    private static void setWeatherTurns(Map<String, Object> state, String weatherKey, int turns) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) state.get("fieldEffects");
        fieldEffects.put(weatherKey, turns);
    }

    private static void setMoves(Map<String, Object> state, boolean player, int teamIndex, List<Map<String, Object>> moves) {
        team(state, player).get(teamIndex).put("moves", moves);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lastActions(Map<String, Object> state) {
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        return (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
    }

    private static Map<String, Object> findAction(List<Map<String, Object>> actions, String side, String moveName) {
        return actions.stream()
                .filter(action -> side.equals(action.get("side")) && moveName.equals(action.get("move")))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Object> move(String name, String nameEn, int power, int accuracy,
                                            int priority, int damageClassId, int typeId, int targetId) {
        Map<String, Object> move = new LinkedHashMap<>();
        move.put("name", name);
        move.put("name_en", nameEn);
        move.put("power", power);
        move.put("accuracy", accuracy);
        move.put("priority", priority);
        move.put("damage_class_id", damageClassId);
        move.put("type_id", typeId);
        move.put("target_id", targetId);
        return move;
    }

    private static String singleTeamJson(String name, int hp, int speed, String heldItem, String abilityNameEn) {
        return "[" + pokemonJson(name, hp, speed, heldItem, abilityNameEn) + ","
                + pokemonJson(name + "-B", hp, speed - 10, "", "") + ","
                + pokemonJson(name + "-C", hp, speed - 20, "", "") + ","
                + pokemonJson(name + "-D", hp, speed - 30, "", "") + "]";
    }

    private static String pokemonJson(String name, int hp, int speed, String heldItem, String abilityNameEn) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"name_en\":\"" + name.toLowerCase() + "\"," +
                "\"battleScore\":200," +
                "\"heldItem\":\"" + heldItem + "\"," +
                "\"ability\":{\"name_en\":\"" + abilityNameEn + "\",\"name\":\"" + abilityNameEn + "\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":" + hp + ",\"attack\":110,\"defense\":90,\"specialAttack\":90,\"specialDefense\":90,\"speed\":" + speed + "}," +
                "\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":40,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]" +
                "}";
    }

    private static BattleEngine createEngine() {
        return new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), new TypeEfficacyMapper() {
            @Override
            public List<Map<String, Object>> selectAllTypeEfficacy() {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> selectByDamageTypeId(Integer damageTypeId) {
                return List.of();
            }

            @Override
            public Integer selectDamageFactor(Integer damageTypeId, Integer targetTypeId) {
                return 100;
            }
        }, new ObjectMapper());
    }
}

