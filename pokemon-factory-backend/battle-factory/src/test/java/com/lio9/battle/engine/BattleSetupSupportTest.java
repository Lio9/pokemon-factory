package com.lio9.battle.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSetupSupportTest {

    @Test
    void createPreviewState_initializesPreviewMetadataAndEmptyBattleTeams() {
        BattleSetupSupport support = createSupport();

        Map<String, Object> state = support.createPreviewState(teamJson(List.of(
                pokemon("Player-A", 120, 110, ""),
                pokemon("Player-B", 118, 100, ""))), teamJson(
                        List.of(
                                pokemon("Opp-A", 120, 90, ""),
                                pokemon("Opp-B", 118, 80, ""))),
                12, 4242L);

        assertEquals("preview", state.get("status"));
        assertEquals("team-preview", state.get("phase"));
        assertEquals(50, state.get("level"));
        assertEquals(4, state.get("battleTeamSize"));
        assertTrue(((List<?>) state.get("playerTeam")).isEmpty());
        assertTrue(((List<?>) state.get("opponentTeam")).isEmpty());
        assertEquals(2, ((List<?>) state.get("playerRoster")).size());
        assertEquals(0, ((Map<?, ?>) state.get("fieldEffects")).get("playerTailwindTurns"));
    }

    @Test
    void applyTeamPreviewSelection_buildsBattleTeamsAndOpeningRoundLog() {
        BattleSetupSupport support = createSupport();
        Map<String, Object> previewState = support.createPreviewState(teamJson(List.of(
                pokemon("Player-A", 120, 95, ""),
                pokemon("Player-B", 120, 125, "intimidate"),
                pokemon("Player-C", 120, 105, ""),
                pokemon("Player-D", 120, 85, ""))), teamJson(
                        List.of(
                                pokemon("Opp-A", 120, 120, ""),
                                pokemon("Opp-B", 120, 90, ""),
                                pokemon("Opp-C", 120, 80, ""),
                                pokemon("Opp-D", 120, 70, ""))),
                12, 5151L);

        Map<String, Object> state = support.applyTeamPreviewSelection(
                previewState,
                Map.of("pickedRosterIndexes", List.of(0, 1, 2, 3), "leadRosterIndexes", List.of(1, 0)),
                Map.of("pickedRosterIndexes", List.of(0, 1, 2, 3), "leadRosterIndexes", List.of(0, 1)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeam = (List<Map<String, Object>>) state.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(0).get("events");

        assertEquals("running", state.get("status"));
        assertEquals("battle", state.get("phase"));
        assertEquals(1, state.get("roundsCount"));
        assertEquals(1, playerTeam.get(0).get("rosterIndex"));
        assertEquals(0, playerTeam.get(1).get("rosterIndex"));
        assertTrue(events.stream().anyMatch(event -> event.contains("玩家 派出了 Player-B")));
        assertTrue(events.stream().anyMatch(event -> event.contains("威吓")));
    }

    @Test
    void applyReplacementSelection_addsReplacementAndAppendsRoundEvents() {
        BattleSetupSupport support = createSupport();
        Map<String, Object> state = support.createBattleState(teamJson(List.of(
                pokemon("Lead-A", 120, 120, ""),
                pokemon("Lead-B", 115, 110, ""),
                pokemon("Bench-C", 110, 105, ""),
                pokemon("Bench-D", 108, 100, ""))), teamJson(
                        List.of(
                                pokemon("Opp-A", 120, 90, ""),
                                pokemon("Opp-B", 120, 80, ""),
                                pokemon("Opp-C", 120, 70, ""),
                                pokemon("Opp-D", 120, 60, ""))),
                12, 6161L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeam = (List<Map<String, Object>>) state.get("playerTeam");
        playerTeam.get(0).put("currentHp", 0);
        playerTeam.get(0).put("status", "fainted");
        state.put("playerActiveSlots", List.of(1));
        state.put("phase", "replacement");
        state.put("playerPendingReplacementCount", 1);
        state.put("playerPendingReplacementOptions", List.of(2, 3));

        Map<String, Object> updated = support.applyReplacementSelection(state,
                Map.of("replacementIndexes", List.of(2)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updated.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");

        assertEquals("battle", updated.get("phase"));
        assertEquals(0, updated.get("playerPendingReplacementCount"));
        assertEquals(List.of(1, 2), updated.get("playerActiveSlots"));
        assertTrue(events.stream().anyMatch(event -> event.contains("Bench-C")));
    }

    @Test
    void replacePlayerTeamMember_updatesRosterAndDisablesExchangeReward() {
        BattleSetupSupport support = createSupport();
        Map<String, Object> state = support.createBattleState(teamJson(List.of(
                pokemon("Lead-A", 120, 120, ""),
                pokemon("Lead-B", 115, 110, ""),
                pokemon("Bench-C", 110, 105, ""),
                pokemon("Bench-D", 108, 100, ""))), teamJson(
                        List.of(
                                pokemon("Opp-A", 120, 90, ""),
                                pokemon("Opp-B", 120, 80, ""),
                                pokemon("Opp-C", 120, 70, ""),
                                pokemon("Opp-D", 120, 60, ""))),
                12, 7171L);
        state.put("exchangeAvailable", true);

        Map<String, Object> updated = support.replacePlayerTeamMember(state, 0, pokemon("Reward-Mon", 130, 140, ""));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerRoster = (List<Map<String, Object>>) updated.get("playerRoster");

        assertEquals("Reward-Mon", playerRoster.get(0).get("name"));
        assertEquals(Boolean.TRUE, updated.get("exchangeUsed"));
        assertEquals(Boolean.FALSE, updated.get("exchangeAvailable"));
        assertFalse(((List<?>) updated.get("playerActiveSlots")).isEmpty());
    }

    private static BattleSetupSupport createSupport() {
        ObjectMapper mapper = new ObjectMapper();
        BattleEngine engine = createEngine(mapper);
        BattleStateSupport stateSupport = new BattleStateSupport();
        BattlePreviewSupport previewSupport = new BattlePreviewSupport(mapper, stateSupport, 4, 2);
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleDamageSupport damageSupport = new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
        BattleConditionSupport conditionSupport = new BattleConditionSupport(engine, damageSupport, fieldEffectSupport);
        BattleFlowSupport flowSupport = new BattleFlowSupport(engine, conditionSupport, 2);
        return new BattleSetupSupport(previewSupport, stateSupport, fieldEffectSupport, conditionSupport, flowSupport,
                50, 4);
    }

    private static BattleEngine createEngine(ObjectMapper mapper) {
        return new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), createTypeMapper(), mapper);
    }

    private static TypeEfficacyMapper createTypeMapper() {
        return new TypeEfficacyMapper() {
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
        };
    }

    private static String teamJson(List<Map<String, Object>> pokemon) {
        try {
            return new ObjectMapper().writeValueAsString(pokemon);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize team json failed", exception);
        }
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, String ability) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("name_en", name.toLowerCase());
        pokemon.put("ability", ability);
        pokemon.put("heldItem", "");
        pokemon.put("types", List.of(Map.of("type_id", 1, "name", "normal")));
        pokemon.put("moves", List.of(
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                move("Protect", "protect", 0, 100, 4, 3, 1, 7)));
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
                "specialDefense", 90,
                "speed", speed)));
        return pokemon;
    }

    private static Map<String, Object> move(String name, String nameEn, int power, int accuracy, int priority,
            int damageClassId, int typeId, int targetId) {
        return Map.of(
                "name", name,
                "name_en", nameEn,
                "power", power,
                "accuracy", accuracy,
                "priority", priority,
                "damage_class_id", damageClassId,
                "type_id", typeId,
                "target_id", targetId);
    }
}