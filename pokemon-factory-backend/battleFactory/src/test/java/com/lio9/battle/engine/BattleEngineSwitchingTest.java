package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEngineSwitchingTest {

    @Test
    void playRound_supportsPlayerSwitchAction() {
        BattleEngine engine = createEngine();

        String teamJson = "[" +
                pokemonJson("Lead-A", 120, 120) + "," +
                pokemonJson("Lead-B", 115, 110) + "," +
                pokemonJson("Bench-C", 110, 105) + "," +
                pokemonJson("Bench-D", 108, 100) +
                "]";

        Map<String, Object> initialState = engine.createBattleState(teamJson, teamJson, 12, 12345L);
        @SuppressWarnings("unchecked")
        List<Integer> beforeSlots = (List<Integer>) initialState.get("playerActiveSlots");
        assertEquals(List.of(0, 1), beforeSlots);

        Map<String, Object> updatedState = engine.playRound(initialState, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "2",
                "action-slot-1", "move",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Integer> afterSlots = (List<Integer>) updatedState.get("playerActiveSlots");
        assertEquals(List.of(2, 1), afterSlots);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("派出了 Bench-C")));
    }

    @Test
    void playRound_appliesFocusSashAndSitrusBerry() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Striker-A", 140, 130, "", 180) + "," +
                pokemonJson("Support-A", 120, 120) + "," +
                pokemonJson("Bench-A", 110, 100) + "," +
                pokemonJson("Bench-B", 110, 90) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Sash-Lead", 100, 80, "focus-sash", 60) + "," +
                pokemonJson("Berry-Lead", 120, 70, "sitrus-berry", 60) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 110, 50) +
                "]";

        Map<String, Object> initialState = engine.createBattleState(playerTeam, opponentTeam, 12, 24680L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> initialOpponentBattleTeam = (List<Map<String, Object>>) initialState.get("opponentTeam");
        initialOpponentBattleTeam.get(1).put("currentHp", 50);

        Map<String, Object> updatedState = engine.playRound(initialState, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "1"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(1, opponentBattleTeam.get(0).get("currentHp"));
        assertEquals(Boolean.TRUE, opponentBattleTeam.get(0).get("itemConsumed"));
        assertTrue((Integer) opponentBattleTeam.get(1).get("currentHp") > 0);
        assertEquals(Boolean.TRUE, opponentBattleTeam.get(1).get("itemConsumed"));
    }

    @Test
    void playRound_appliesLeftoversEndTurnHealing() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Leftovers-Lead", 160, 120, "leftovers", 60) + "," +
                pokemonJson("Partner-A", 120, 110) + "," +
                pokemonJson("Bench-A", 110, 100) + "," +
                pokemonJson("Bench-B", 110, 90) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Attacker-A", 120, 130, "", 70) + "," +
                pokemonJson("Attacker-B", 120, 100, "", 60) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 110, 70) +
                "]";

        Map<String, Object> initialState = engine.createBattleState(playerTeam, opponentTeam, 12, 13579L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> initialPlayerBattleTeam = (List<Map<String, Object>>) initialState.get("playerTeam");
        initialPlayerBattleTeam.get(0).put("currentHp", 140);

        Map<String, Object> updatedState = engine.playRound(initialState, Map.of(
                "slot-0", "protect",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerBattleTeam = (List<Map<String, Object>>) updatedState.get("playerTeam");
        int currentHp = (Integer) playerBattleTeam.get(0).get("currentHp");
        assertTrue(currentHp > 140);
        assertFalse(currentHp == 160);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("剩饭")));
    }

    @Test
    void applyReplacementSelection_sendsBenchMonToField() {
        BattleEngine engine = createEngine();

        String teamJson = "[" +
                pokemonJson("Lead-A", 120, 120) + "," +
                pokemonJson("Lead-B", 115, 110) + "," +
                pokemonJson("Bench-C", 110, 105) + "," +
                pokemonJson("Bench-D", 108, 100) +
                "]";

        Map<String, Object> state = engine.createBattleState(teamJson, teamJson, 12, 112233L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerBattleTeam = (List<Map<String, Object>>) state.get("playerTeam");
        playerBattleTeam.get(0).put("currentHp", 0);
        playerBattleTeam.get(0).put("status", "fainted");
        state.put("playerActiveSlots", List.of(1));
        state.put("phase", "replacement");
        state.put("playerPendingReplacementCount", 1);
        state.put("playerPendingReplacementOptions", List.of(2, 3));

        Map<String, Object> updatedState = engine.applyReplacementSelection(state, Map.of(
                "replacementIndexes", List.of(2)
        ));

        assertEquals("battle", updatedState.get("phase"));
        assertEquals(0, updatedState.get("playerPendingReplacementCount"));
        @SuppressWarnings("unchecked")
        List<Integer> activeSlots = (List<Integer>) updatedState.get("playerActiveSlots");
        assertEquals(List.of(1, 2), activeSlots);
    }

    @Test
    void createBattleState_appliesIntimidateOnEntry() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Intimidate-Lead", 120, 120, "", 60, "intimidate") + "," +
                pokemonJson("Partner-A", 115, 110) + "," +
                pokemonJson("Bench-C", 110, 105) + "," +
                pokemonJson("Bench-D", 108, 100) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-A", 120, 120) + "," +
                pokemonJson("Target-B", 115, 110) + "," +
                pokemonJson("Bench-E", 110, 105) + "," +
                pokemonJson("Bench-F", 108, 100) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 556677L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) state.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> firstStages = (Map<String, Object>) opponentBattleTeam.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> secondStages = (Map<String, Object>) opponentBattleTeam.get(1).get("statStages");
        assertEquals(-1, firstStages.get("attack"));
        assertEquals(-1, secondStages.get("attack"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(0).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("威吓")));
    }

    @Test
    void createBattleState_clearAmuletBlocksIntimidate() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Intimidate-Lead", 120, 120, "", 60, "intimidate") + "," +
                pokemonJson("Partner-A", 115, 110) + "," +
                pokemonJson("Bench-C", 110, 105) + "," +
                pokemonJson("Bench-D", 108, 100) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Protected-A", 120, 120, "clear-amulet", 60) + "," +
                pokemonJson("Target-B", 115, 110) + "," +
                pokemonJson("Bench-E", 110, 105) + "," +
                pokemonJson("Bench-F", 108, 100) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 778899L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) state.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> protectedStages = (Map<String, Object>) opponentBattleTeam.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> targetStages = (Map<String, Object>) opponentBattleTeam.get(1).get("statStages");
        assertEquals(0, protectedStages.get("attack"));
        assertEquals(-1, targetStages.get("attack"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(0).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("清净护符")));
    }

    @Test
    void playRound_spreadMoveHitsBothOpponentsAndLifeOrbTriggersOnce() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Spread-A", 120, 150, "life-orb", 80, "", 11) + "," +
                pokemonJson("Partner-A", 120, 140, "", 0, "", 7) + "," +
                pokemonJson("Bench-A", 110, 105) + "," +
                pokemonJson("Bench-B", 108, 100) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-A", 30, 80) + "," +
                pokemonJson("Target-B", 30, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> updatedState = engine.playRound(engine.createBattleState(playerTeam, opponentTeam, 12, 998877L), Map.of(
                "slot-0", "strike",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(0, opponentBattleTeam.get(0).get("currentHp"));
        assertEquals(0, opponentBattleTeam.get(1).get("currentHp"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerBattleTeam = (List<Map<String, Object>>) updatedState.get("playerTeam");
        assertEquals(108, playerBattleTeam.get(0).get("currentHp"));
    }

    @Test
    void playRound_tailwindMakesPlayerMoveFirstNextTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Tailwind-A", 120, 80) + "," +
                pokemonJson("Partner-A", 120, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Fast-Opp", 120, 110) + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 223344L);
        setMoves(state, true, 0, List.of(
                move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 13),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterTailwind = engine.playRound(state, Map.of(
                "slot-0", "tailwind",
                "slot-1", "protect"
        ));
        Map<String, Object> afterAttackRound = engine.playRound(afterTailwind, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) afterAttackRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("playerTailwindTurns") > 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterAttackRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> firstHit = actions.stream()
                .filter(action -> "hit".equals(action.get("result")) && "Strike".equals(action.get("move")))
                .findFirst()
                .orElseThrow();
        assertEquals("player", firstHit.get("side"));
    }

    @Test
    void playRound_tailwindBoostsWindRiderOnField() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Tailwind-A", 120, 80) + "," +
                pokemonJson("Partner-A", 120, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("WindRider-Opp", 120, 110, "", 70, "wind-rider") + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 223345L);
        setMoves(state, true, 0, List.of(move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 13), protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterTailwind = engine.playRound(state, Map.of(
                "slot-0", "tailwind",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) afterTailwind.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> statStages = (Map<String, Object>) opponentTeamState.get(0).get("statStages");
        assertEquals(1, statStages.get("attack"));
    }

    @Test
    void playRound_trickRoomMakesSlowerSideMoveFirstNextTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("TrickRoom-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 100) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Slow-Opp", 120, 70) + "," +
                pokemonJson("Partner-Opp", 120, 80) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 445566L);
        setMoves(state, true, 0, List.of(
                move("Trick Room", "trick-room", 0, 100, -7, 3, 1, 12),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterTrickRoom = engine.playRound(state, Map.of(
                "slot-0", "trick-room",
                "slot-1", "protect"
        ));
        Map<String, Object> afterAttackRound = engine.playRound(afterTrickRoom, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) afterAttackRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("trickRoomTurns") > 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterAttackRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        int opponentStrikeIndex = firstActionIndex(actions, "opponent", "Strike");
        int playerStrikeIndex = firstActionIndex(actions, "player", "Strike");
        assertTrue(opponentStrikeIndex >= 0);
        assertTrue(playerStrikeIndex >= 0);
        assertTrue(opponentStrikeIndex < playerStrikeIndex);
    }

    @Test
    void playRound_fakeOutCausesFlinchOnlyOnEntryTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-A", 110, 70) + "," +
                pokemonJson("Bench-B", 108, 60) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-A", 120, 100) + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 667788L);
        setMoves(state, true, 0, List.of(
                move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterFirstRound = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> firstRoundActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) afterFirstRound.get("rounds"))
                .get(((List<Map<String, Object>>) afterFirstRound.get("rounds")).size() - 1)
                .get("actions");
        assertTrue(firstRoundActions.stream().anyMatch(action -> "Target-A".equals(action.get("actor")) && "flinch".equals(action.get("result"))));

        Map<String, Object> afterSecondRound = engine.playRound(afterFirstRound, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secondRoundActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) afterSecondRound.get("rounds"))
                .get(((List<Map<String, Object>>) afterSecondRound.get("rounds")).size() - 1)
                .get("actions");
        assertFalse(secondRoundActions.stream().anyMatch(action -> "Target-A".equals(action.get("actor")) && "flinch".equals(action.get("result"))));
    }

    @Test
    void playRound_fakeOutBecomesAvailableAgainAfterSwitchBackIn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-C", 110, 90) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-A", 120, 100) + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-E", 110, 80) + "," +
                pokemonJson("Bench-F", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 778811L);
        setMoves(state, true, 0, List.of(
                move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, true, 2, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10), protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterOpen = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> afterSwitchOut = engine.playRound(afterOpen, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "2",
                "slot-1", "protect"
        ));
        Map<String, Object> afterSwitchBack = engine.playRound(afterSwitchOut, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> afterFakeOutAgain = engine.playRound(afterSwitchBack, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) ((List<Map<String, Object>>) afterFakeOutAgain.get("rounds"))
                .get(((List<Map<String, Object>>) afterFakeOutAgain.get("rounds")).size() - 1)
                .get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Target-A".equals(action.get("actor")) && "flinch".equals(action.get("result"))));
    }

    @Test
    void playRound_fakeOutDoesNotFlinchInnerFocusTarget() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-A", 110, 70) + "," +
                pokemonJson("Bench-B", 108, 60) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Focus-Opp", 120, 100, "", 60, "inner-focus") + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 778812L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updated.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertFalse(actions.stream().anyMatch(action -> "Focus-Opp".equals(action.get("actor")) && "flinch".equals(action.get("result"))));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertTrue((Integer) opponentState.get(0).get("currentHp") < 120);
    }

    @Test
    void playRound_icyWindDropsBothOpponentsSpeedAndChangesTurnOrder() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("IcyWind-A", 180, 90) + "," +
                pokemonJson("Partner-A", 120, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Fast-Opp", 220, 110) + "," +
                pokemonJson("Partner-Opp", 220, 105) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 889900L);
        setMoves(state, true, 0, List.of(
                move("Icy Wind", "icy-wind", 20, 100, 0, 2, 15, 11),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterIcyWind = engine.playRound(state, Map.of(
                "slot-0", "icy-wind",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) afterIcyWind.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> firstStages = (Map<String, Object>) opponentBattleTeam.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> secondStages = (Map<String, Object>) opponentBattleTeam.get(1).get("statStages");
        assertEquals(-1, firstStages.get("speed"));
        assertEquals(-1, secondStages.get("speed"));

        Map<String, Object> afterAttackRound = engine.playRound(afterIcyWind, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterAttackRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        int opponentStrikeIndex = firstActionIndex(actions, "opponent", "Strike");
        int playerStrikeIndex = firstActionIndex(actions, "player", "Strike");
        assertTrue(opponentStrikeIndex >= 0);
        assertTrue(playerStrikeIndex >= 0);
        assertTrue(playerStrikeIndex < opponentStrikeIndex);
    }

    @Test
    void playRound_icyWindIsBlockedByClearAmulet() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("IcyWind-A", 180, 90) + "," +
                pokemonJson("Partner-A", 120, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Protected-Opp", 220, 110, "clear-amulet", 60) + "," +
                pokemonJson("Partner-Opp", 220, 105) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 990011L);
        setMoves(state, true, 0, List.of(
                move("Icy Wind", "icy-wind", 20, 100, 0, 2, 15, 11),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> afterIcyWind = engine.playRound(state, Map.of(
                "slot-0", "icy-wind",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) afterIcyWind.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> protectedStages = (Map<String, Object>) opponentBattleTeam.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> targetStages = (Map<String, Object>) opponentBattleTeam.get(1).get("statStages");
        assertEquals(0, protectedStages.get("speed"));
        assertEquals(-1, targetStages.get("speed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterIcyWind.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("清净护符")));
    }

    @Test
    void playRound_snarlSecondaryEffectIsBlockedByCovertCloak() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Snarl-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Cloak-Opp", 220, 70, "covert-cloak", 60) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 991122L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Snarl", "snarl", 55, 100, 0, 2, DamageCalculatorUtil.TYPE_DARK, 11)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "snarl",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(0, stages.get("specialAttack"));
        assertTrue((Integer) opponentState.get(0).get("currentHp") < 220);
    }

    @Test
    void playRound_thunderWaveAppliesParalysisAndChangesTurnOrder() {
        BattleEngine engine = createEngine();

        boolean found = false;
        for (long seed = 100L; seed < 180L; seed++) {
            String playerTeam = "[" +
                    pokemonJson("ThunderWave-A", 220, 80) + "," +
                    pokemonJson("Partner-A", 220, 70) + "," +
                    pokemonJson("Bench-A", 110, 60) + "," +
                    pokemonJson("Bench-B", 108, 50) +
                    "]";
            String opponentTeam = "[" +
                    pokemonJson("Fast-Opp", 220, 110, "", 30) + "," +
                    pokemonJson("Partner-Opp", 220, 90, "", 30) + "," +
                    pokemonJson("Bench-C", 110, 80) + "," +
                    pokemonJson("Bench-D", 108, 70) +
                    "]";

            Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, seed);
            setMoves(state, true, 0, List.of(
                    move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                    move("Strike", "strike", 70, 100, 0, 1, 1, 10),
                    protectMove()
            ));
            setMoves(state, true, 1, List.of(protectMove()));
            setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
            setMoves(state, false, 1, List.of(protectMove()));

            Map<String, Object> afterThunderWave = engine.playRound(state, Map.of(
                    "slot-0", "thunder-wave",
                    "target-slot-0", "0",
                    "slot-1", "protect"
            ));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) afterThunderWave.get("opponentTeam");
            if (!"paralysis".equals(opponentBattleTeam.get(0).get("condition"))) {
                continue;
            }

            Map<String, Object> afterAttackRound = engine.playRound(afterThunderWave, Map.of(
                    "slot-0", "strike",
                    "target-slot-0", "0",
                    "slot-1", "protect"
            ));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterAttackRound.get("rounds");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
            int playerStrikeIndex = firstActionIndex(actions, "player", "Strike");
            int opponentStrikeIndex = firstActionIndex(actions, "opponent", "Strike");
            if (playerStrikeIndex >= 0 && opponentStrikeIndex >= 0 && playerStrikeIndex < opponentStrikeIndex) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void playRound_thunderWaveFailsAgainstElectricType() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("ThunderWave-A", 120, 80) + "," +
                pokemonJson("Partner-A", 120, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Electric-Opp", 120, 110) + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 271828L);
        setMoves(state, true, 0, List.of(
                move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) state.get("opponentTeam");
        opponentBattleTeam.get(0).put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_ELECTRIC, "name", "Electric")));

        Map<String, Object> afterThunderWave = engine.playRound(state, Map.of(
                "slot-0", "thunder-wave",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentBattleTeam = (List<Map<String, Object>>) afterThunderWave.get("opponentTeam");
        assertEquals(null, updatedOpponentBattleTeam.get(0).get("condition"));
    }

    @Test
    void playRound_paralysisCanStopAction() {
        BattleEngine engine = createEngine();

        boolean found = false;
        for (long seed = 1L; seed < 240L; seed++) {
            String playerTeam = "[" +
                    pokemonJson("ThunderWave-A", 240, 50) + "," +
                    pokemonJson("Partner-A", 240, 40) + "," +
                    pokemonJson("Bench-A", 110, 60) + "," +
                    pokemonJson("Bench-B", 108, 50) +
                    "]";
            String opponentTeam = "[" +
                    pokemonJson("Fast-Opp", 240, 140, "", 30) + "," +
                    pokemonJson("Partner-Opp", 240, 90, "", 30) + "," +
                    pokemonJson("Bench-C", 110, 80) + "," +
                    pokemonJson("Bench-D", 108, 70) +
                    "]";

            Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, seed);
            setMoves(state, true, 0, List.of(
                    move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                    protectMove()
            ));
            setMoves(state, true, 1, List.of(protectMove()));
            setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
            setMoves(state, false, 1, List.of(protectMove()));

            Map<String, Object> afterThunderWave = engine.playRound(state, Map.of(
                    "slot-0", "thunder-wave",
                    "target-slot-0", "0",
                    "slot-1", "protect"
            ));
            Map<String, Object> afterParalysisRound = engine.playRound(afterThunderWave, Map.of(
                    "slot-0", "protect",
                    "slot-1", "protect"
            ));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterParalysisRound.get("rounds");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
            if (actions.stream().anyMatch(action -> "Fast-Opp".equals(action.get("actor")) && "paralyzed".equals(action.get("result")))) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void playRound_willOWispAppliesBurnAndReducesPhysicalDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Burn-A", 220, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Physical-Opp", 220, 100, "", 80) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 314159L);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 80, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayerTeam = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        int baselineTotalHp = ((Integer) baselinePlayerTeam.get(0).get("currentHp")) + ((Integer) baselinePlayerTeam.get(1).get("currentHp"));

        Map<String, Object> burned = engine.createBattleState(playerTeam, opponentTeam, 12, 314159L);
        setMoves(burned, true, 0, List.of(
                move("Will-O-Wisp", "will-o-wisp", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_FIRE, 10),
                move("Strike", "strike", 20, 100, 0, 1, 1, 10)
        ));
        setMoves(burned, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(burned, false, 0, List.of(move("Strike", "strike", 80, 100, 0, 1, 1, 10)));
        setMoves(burned, false, 1, List.of(protectMove()));

        Map<String, Object> afterBurn = engine.playRound(burned, Map.of(
                "slot-0", "will-o-wisp",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> burnedOpponentTeam = (List<Map<String, Object>>) afterBurn.get("opponentTeam");
        assertEquals("burn", burnedOpponentTeam.get(0).get("condition"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> burnedPlayerTeam = (List<Map<String, Object>>) afterBurn.get("playerTeam");
        int burnedTotalHp = ((Integer) burnedPlayerTeam.get(0).get("currentHp")) + ((Integer) burnedPlayerTeam.get(1).get("currentHp"));
        assertTrue(burnedTotalHp > baselineTotalHp);
    }

    @Test
    void playRound_burnDealsEndTurnDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Burn-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 424242L);
        setMoves(state, true, 0, List.of(
                move("Will-O-Wisp", "will-o-wisp", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_FIRE, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> afterBurn = engine.playRound(state, Map.of(
                "slot-0", "will-o-wisp",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) afterBurn.get("opponentTeam");
        assertEquals(150, opponentTeamState.get(0).get("currentHp"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterBurn.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("灼伤")));
    }

    @Test
    void playRound_willOWispFailsAgainstFireType() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Burn-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Fire-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515151L);
        setMoves(state, true, 0, List.of(
                move("Will-O-Wisp", "will-o-wisp", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_FIRE, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) state.get("opponentTeam");
        opponentTeamState.get(0).put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_FIRE, "name", "Fire")));

        Map<String, Object> afterBurn = engine.playRound(state, Map.of(
                "slot-0", "will-o-wisp",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) afterBurn.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_scaldCanApplyBurnAsSecondaryEffect() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Scald-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515152L);
        setMoves(state, true, 0, List.of(
                withEffectChance(move("Scald", "scald", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10), 100),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "scald",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("burn", updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_sludgeBombCanApplyPoisonAsSecondaryEffect() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Sludge-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515153L);
        setMoves(state, true, 0, List.of(
                withEffectChance(move("Sludge Bomb", "sludge-bomb", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_POISON, 10), 100),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "sludge-bomb",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("poison", updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_iceBeamCanApplyFreezeAsSecondaryEffect() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("IceBeam-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515154L);
        setMoves(state, true, 0, List.of(
                withEffectChance(move("Ice Beam", "ice-beam", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_ICE, 10), 100),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "ice-beam",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("freeze", updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_fireMoveThawsFrozenTarget() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Fire-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Frozen-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515155L);
        setMoves(state, true, 0, List.of(
                move("Flamethrower", "flamethrower", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) state.get("opponentTeam");
        opponentTeamState.get(0).put("condition", "freeze");

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "flamethrower",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_moveMetadataCanApplyFlinch() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("MetaFlinch-A", 180, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515156L);
        setMoves(state, true, 0, List.of(
                withFlinchChance(move("Meta Flinch", "meta-flinch", 60, 100, 0, 1, 1, 10), 100),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "meta-flinch",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Target-Opp".equals(action.get("actor")) && "flinch".equals(action.get("result"))));
    }

    @Test
    void playRound_moveMetadataCanApplyBurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("MetaBurn-A", 180, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515256L);
        setMoves(state, true, 0, List.of(
                withAilmentMeta(move("Meta Burn", "meta-burn", 60, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10), 100, "burn"),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "meta-burn",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("burn", opponentTeamState.get(0).get("condition"));
    }

    @Test
    void playRound_moveMetadataCanHitMultipleTimes() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("MultiHit-A", 180, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515257L);
        setMoves(state, true, 0, List.of(
                withHitRange(move("Meta Multi", "meta-multi", 25, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10), 2, 5),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "meta-multi",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Meta Multi".equals(action.get("move")) && "Target-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertTrue((Integer) hit.get("hitCount") >= 2);
        assertTrue((Integer) hit.get("hitCount") <= 5);
        @SuppressWarnings("unchecked")
        List<Integer> hitDamages = (List<Integer>) hit.get("hitDamages");
        assertEquals(hit.get("hitCount"), hitDamages.size());
        int summedDamage = hitDamages.stream().mapToInt(Integer::intValue).sum();
        assertEquals(summedDamage, hit.get("damage"));
    }

    @Test
    void playRound_skillLinkForcesMaximumHitCount() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("SkillLink-A", 180, 130, "", 60, "skill-link") + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515258L);
        setMoves(state, true, 0, List.of(
                withHitRange(move("Skill Multi", "skill-multi", 20, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10), 2, 5),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "skill-multi",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Skill Multi".equals(action.get("move")) && "Target-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals(5, hit.get("hitCount"));
    }

    @Test
    void playRound_moveMetadataCanForceCriticalHit() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Crit-A", 180, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 515259L);
        setMoves(baseline, true, 0, List.of(
                move("Baseline Crit", "baseline-crit", 70, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10),
                protectMove()
        ));
        setMoves(baseline, true, 1, List.of(protectMove()));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineState = engine.playRound(baseline, Map.of(
                "slot-0", "baseline-crit",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> critical = engine.createBattleState(playerTeam, opponentTeam, 12, 515259L);
        setMoves(critical, true, 0, List.of(
                withCritRate(move("Critical Strike", "critical-strike", 70, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10), 3),
                protectMove()
        ));
        setMoves(critical, true, 1, List.of(protectMove()));
        setMoves(critical, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(critical, false, 1, List.of(protectMove()));
        Map<String, Object> criticalState = engine.playRound(critical, Map.of(
                "slot-0", "critical-strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) baselineState.get("rounds"))
                .get(((List<Map<String, Object>>) baselineState.get("rounds")).size() - 1)
                .get("actions");
        Map<String, Object> baselineHit = baselineActions.stream()
                .filter(action -> "Baseline Crit".equals(action.get("move")) && "Target-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> criticalActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) criticalState.get("rounds"))
                .get(((List<Map<String, Object>>) criticalState.get("rounds")).size() - 1)
                .get("actions");
        Map<String, Object> criticalHit = criticalActions.stream()
                .filter(action -> "Critical Strike".equals(action.get("move")) && "Target-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();

        assertEquals(Boolean.TRUE, criticalHit.get("critical"));
        assertTrue((Integer) criticalHit.get("damage") > (Integer) baselineHit.get("damage"));
    }

    @Test
    void playRound_rechargeMoveSkipsNextTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Recharge-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515260L);
        setMoves(state, true, 0, List.of(
                withEffectShort(move("Hyper Beam", "hyper-beam", 150, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10),
                        "User must recharge next turn."),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10)
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> afterBeam = engine.playRound(state, Map.of(
                "slot-0", "hyper-beam",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> afterRecharge = engine.playRound(afterBeam, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterRecharge.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Recharge-A".equals(action.get("actor")) && "recharge".equals(action.get("result"))));
    }

    @Test
    void playRound_chargeMoveChargesThenAttacksNextTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Charge-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515261L);
        setMoves(state, true, 0, List.of(
                withEffectShort(move("Solar Beam", "solar-beam", 120, 100, 0, 1, DamageCalculatorUtil.TYPE_GRASS, 10),
                        "User charges on first turn, attacks on second turn."),
                move("Strike", "strike", 70, 100, 0, 1, 1, 10)
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> afterCharge = engine.playRound(state, Map.of(
                "slot-0", "solar-beam",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> afterAttack = engine.playRound(afterCharge, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> firstRoundActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) afterCharge.get("rounds"))
                .get(((List<Map<String, Object>>) afterCharge.get("rounds")).size() - 1)
                .get("actions");
        assertTrue(firstRoundActions.stream().anyMatch(action -> "Charge-A".equals(action.get("actor")) && "charge".equals(action.get("result"))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secondRoundActions = (List<Map<String, Object>>) ((List<Map<String, Object>>) afterAttack.get("rounds"))
                .get(((List<Map<String, Object>>) afterAttack.get("rounds")).size() - 1)
                .get("actions");
        assertTrue(secondRoundActions.stream().anyMatch(action -> "Solar Beam".equals(action.get("move")) && "hit".equals(action.get("result"))));
    }

    @Test
    void playRound_soundproofBlocksSoundMovesByFlag() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Sound-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Soundproof-Opp", 220, 100, "", 60, "soundproof") + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515262L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(withFlags(move("Snarl", "snarl", 55, 100, 0, 2, DamageCalculatorUtil.TYPE_DARK, 10), "sound")));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "snarl",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Snarl".equals(action.get("move")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-immune", hit.get("result"));
        assertEquals("soundproof", hit.get("ability"));
    }

    @Test
    void playRound_bulletproofBlocksBulletMovesByFlag() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Bullet-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Bulletproof-Opp", 220, 100, "", 60, "bulletproof") + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515263L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(withFlags(move("Aura Sphere", "aura-sphere", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_FIGHTING, 10), "bullet")));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "aura-sphere",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Aura Sphere".equals(action.get("move")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-immune", hit.get("result"));
        assertEquals("bulletproof", hit.get("ability"));
    }

    @Test
    void playRound_powderFlagRespectsGrassPowderImmunity() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Powder-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Grass-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515264L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(withFlags(move("Powder Shot", "powder-shot", 60, 100, 0, 2, DamageCalculatorUtil.TYPE_BUG, 10), "powder")));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setTypes(state, false, 0, List.of(DamageCalculatorUtil.TYPE_GRASS));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "powder-shot",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Powder Shot".equals(action.get("move")))
                .findFirst()
                .orElseThrow();
        assertEquals("status-immune", hit.get("result"));
    }

    @Test
    void playRound_windRiderBlocksWindMovesAndRaisesAttack() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Wind-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("WindRider-Opp", 220, 100, "", 60, "wind-rider") + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515265L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(withFlags(move("Icy Wind", "icy-wind", 55, 100, 0, 2, DamageCalculatorUtil.TYPE_ICE, 11), "wind")));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "icy-wind",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> hit = actions.stream()
                .filter(action -> "Icy Wind".equals(action.get("move")) && "WindRider-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-immune", hit.get("result"));
        assertEquals("wind-rider", hit.get("ability"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> statStages = (Map<String, Object>) opponentTeamState.get(0).get("statStages");
        assertEquals(1, statStages.get("attack"));
    }

    @Test
    void playRound_armorTailBlocksPriorityMoveIntoProtectedSide() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-A", 110, 70) + "," +
                pokemonJson("Bench-B", 108, 60) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("ArmorTail-Opp", 120, 100, "", 60, "armor-tail") + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515266L);
        setMoves(state, true, 0, List.of(move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10), protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> block = actions.stream()
                .filter(action -> "Fake Out".equals(action.get("move")) && "ArmorTail-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-priority-blocked", block.get("result"));
        assertEquals("armor-tail", block.get("ability"));
    }

    @Test
    void playRound_queenlyMajestyBlocksPriorityMoveIntoAlly() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-A", 110, 70) + "," +
                pokemonJson("Bench-B", 108, 60) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Queenly-Opp", 120, 100, "", 60, "queenly-majesty") + "," +
                pokemonJson("Protected-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515267L);
        setMoves(state, true, 0, List.of(move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10), protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "1",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> block = actions.stream()
                .filter(action -> "Fake Out".equals(action.get("move")) && "Protected-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-priority-blocked", block.get("result"));
        assertEquals("queenly-majesty", block.get("ability"));
    }

    @Test
    void playRound_dazzlingBlocksPriorityMoveIntoProtectedSide() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("FakeOut-A", 120, 120) + "," +
                pokemonJson("Partner-A", 120, 80) + "," +
                pokemonJson("Bench-A", 110, 70) + "," +
                pokemonJson("Bench-B", 108, 60) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Dazzling-Opp", 120, 100, "", 60, "dazzling") + "," +
                pokemonJson("Partner-Opp", 120, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515268L);
        setMoves(state, true, 0, List.of(move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10), protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "fake-out",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> block = actions.stream()
                .filter(action -> "Fake Out".equals(action.get("move")) && "Dazzling-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-priority-blocked", block.get("result"));
        assertEquals("dazzling", block.get("ability"));
    }

    @Test
    void playRound_moveMetadataCanApplySpeedDrop() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("MetaDrop-A", 180, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515157L);
        setMoves(state, true, 0, List.of(
                withMetaStatChanges(move("Meta Drop", "meta-drop", 60, 100, 0, 2, 5, 10), 100, 6, -1),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "meta-drop",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> statStages = (Map<String, Object>) opponentTeamState.get(0).get("statStages");
        assertEquals(-1, statStages.get("speed"));
    }

    @Test
    void playRound_moveMetadataCanDrainHp() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Drain-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515158L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) state.get("playerTeam");
        playerTeamState.get(0).put("currentHp", 100);
        setMoves(state, true, 0, List.of(
                withDrain(move("Drain Move", "drain-move", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_GRASS, 10), 50),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "drain-move",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedPlayerTeam = (List<Map<String, Object>>) updatedState.get("playerTeam");
        assertTrue((Integer) updatedPlayerTeam.get(0).get("currentHp") > 100);
    }

    @Test
    void playRound_moveMetadataCanHealUser() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Recover-A", 220, 130) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515159L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) state.get("playerTeam");
        playerTeamState.get(0).put("currentHp", 80);
        setMoves(state, true, 0, List.of(
                withHealing(move("Recover", "recover", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_NORMAL, 7), 50),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "recover",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedPlayerTeam = (List<Map<String, Object>>) updatedState.get("playerTeam");
        assertTrue((Integer) updatedPlayerTeam.get(0).get("currentHp") > 80);
    }

    @Test
    void playRound_moveFlagsMarkContactForRockyHelmet() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Helmet-A", 220, 90, "rocky-helmet", 20) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Contact-Opp", 220, 120) + "," +
                pokemonJson("Partner-Opp", 180, 90) + "," +
                pokemonJson("Bench-C", 110, 80) + "," +
                pokemonJson("Bench-D", 108, 70) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515160L);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(withFlags(move("Flag Contact", "flag-contact", 70, 100, 0, 1, 1, 10), "contact")));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertTrue((Integer) opponentTeamState.get(0).get("currentHp") < 220);
    }

    @Test
    void playRound_followMeRedirectsSingleTargetMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Redirect-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Attacker-Opp", 220, 90, "", 90) + "," +
                pokemonJson("Partner-Opp", 180, 80) + "," +
                pokemonJson("Bench-C", 110, 70) + "," +
                pokemonJson("Bench-D", 108, 60) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 626262L);
        setMoves(state, true, 0, List.of(
                move("Follow Me", "follow-me", 0, 100, 2, 3, 1, 7),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "follow-me",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) updatedState.get("playerTeam");
        int redirectorHp = (Integer) playerTeamState.get(0).get("currentHp");
        int partnerHp = (Integer) playerTeamState.get(1).get("currentHp");
        assertTrue(redirectorHp < 220);
        assertEquals(220, partnerHp);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("吸引了对手的招式")));
    }

    @Test
    void playRound_followMeDoesNotRedirectSpreadMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Redirect-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Spread-Opp", 220, 90, "", 90, "", 11) + "," +
                pokemonJson("Partner-Opp", 180, 80) + "," +
                pokemonJson("Bench-C", 110, 70) + "," +
                pokemonJson("Bench-D", 108, 60) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 737373L);
        setMoves(state, true, 0, List.of(
                move("Follow Me", "follow-me", 0, 100, 2, 3, 1, 7),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Spread Strike", "spread-strike", 90, 100, 0, 1, 1, 11)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "follow-me",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) updatedState.get("playerTeam");
        assertTrue((Integer) playerTeamState.get(0).get("currentHp") < 220);
        assertTrue((Integer) playerTeamState.get(1).get("currentHp") < 220);
    }

    @Test
    void playRound_goodAsGoldBlocksStatusMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Status-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Gold-Opp", 220, 70, "", 60, "good-as-gold") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 565656L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "thunder-wave",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertEquals(null, opponentState.get(0).get("condition"));
    }

    @Test
    void playRound_magicBounceReflectsThunderWaveBackToUser() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Status-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Bounce-Opp", 220, 70, "", 60, "magic-bounce") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 565657L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "thunder-wave",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertEquals("paralysis", playerState.get(0).get("condition"));
        assertEquals(null, opponentState.get(0).get("condition"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updated.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> bounce = actions.stream()
                .filter(action -> "Status-A".equals(action.get("actor")) && "paralysis".equals(action.get("result")))
                .findFirst()
                .orElseThrow();
        assertEquals("magic-bounce", bounce.get("ability"));
        assertEquals("Status-A", bounce.get("target"));
    }

    @Test
    void playRound_magicBounceReflectsTauntBackToUser() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Taunt-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Bounce-Opp", 220, 70, "", 60, "magic-bounce") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 565658L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Taunt", "taunt", 0, 100, 0, 3, 17, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "taunt",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertEquals(2, playerState.get(0).get("tauntTurns"));
        assertEquals(0, opponentState.get(0).get("tauntTurns"));
    }

    @Test
    void playRound_obliviousBlocksTaunt() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Taunt-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Oblivious-Opp", 220, 70, "", 60, "oblivious") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 565660L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Taunt", "taunt", 0, 100, 0, 3, 17, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "taunt",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertEquals(0, opponentState.get(0).get("tauntTurns"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updated.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> block = actions.stream()
                .filter(action -> "oblivious".equals(action.get("ability")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-immune", block.get("result"));
        assertEquals("Oblivious-Opp", block.get("target"));
    }

    @Test
    void playRound_magicBounceDoesNotReflectPartingShot() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Pivot-A", 220, 120) + "," +
                pokemonJson("Switch-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Bounce-Opp", 220, 70, "", 60, "magic-bounce") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 565659L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Parting Shot", "parting-shot", 0, 100, 0, 3, 17, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "parting-shot",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> playerStages = (Map<String, Object>) playerState.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> opponentStages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(0, playerStages.get("attack"));
        assertEquals(0, playerStages.get("specialAttack"));
        assertEquals(-1, opponentStages.get("attack"));
        assertEquals(-1, opponentStages.get("specialAttack"));
    }

    @Test
    void playRound_helpingHandBoostsAllyDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Support-A", 220, 120) + "," +
                pokemonJson("Striker-A", 220, 80, "", 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 90) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 848484L);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineRound.get("opponentTeam");
        int baselineHp = (Integer) baselineOpponent.get(0).get("currentHp");

        Map<String, Object> boosted = engine.createBattleState(playerTeam, opponentTeam, 12, 848484L);
        setMoves(boosted, true, 0, List.of(
                move("Helping Hand", "helping-hand", 0, 100, 5, 3, 1, 3),
                protectMove()
        ));
        setMoves(boosted, true, 1, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(boosted, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(boosted, false, 1, List.of(protectMove()));
        Map<String, Object> boostedRound = engine.playRound(boosted, Map.of(
                "slot-0", "helping-hand",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedOpponent = (List<Map<String, Object>>) boostedRound.get("opponentTeam");
        int boostedHp = (Integer) boostedOpponent.get(0).get("currentHp");
        assertTrue(boostedHp < baselineHp);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) boostedRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("帮助了 Striker-A")));
    }

    @Test
    void playRound_tauntBlocksStatusMoveOnSameTurn() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Taunt-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("TrickRoom-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 303030L);
        setMoves(state, true, 0, List.of(
                move("Taunt", "taunt", 0, 100, 0, 3, 17, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(
                move("Trick Room", "trick-room", 0, 100, -7, 3, 1, 12),
                move("Strike", "strike", 20, 100, 0, 1, 1, 10)
        ));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "taunt",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(2, opponentTeamState.get(0).get("tauntTurns"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) updatedState.get("fieldEffects");
        assertEquals(0, fieldEffects.get("trickRoomTurns"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Taunt-A".equals(action.get("actor")) && "taunt".equals(action.get("result"))));
        assertTrue(actions.stream().anyMatch(action -> "TrickRoom-Opp".equals(action.get("actor")) && "taunted".equals(action.get("result"))));
    }

    @Test
    void playRound_aromaVeilBlocksTauntOnAlly() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("AromaVeil-A", 220, 80, "", 60, "aroma-veil") + "," +
                pokemonJson("Protected-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Taunt-Opp", 220, 120) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 303032L);
        setMoves(state, true, 0, List.of(protectMove()));
        setMoves(state, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10), protectMove()));
        setMoves(state, false, 0, List.of(move("Taunt", "taunt", 0, 100, 0, 3, 17, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "protect",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) updatedState.get("playerTeam");
        assertEquals(0, playerTeamState.get(1).get("tauntTurns"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> block = actions.stream()
                .filter(action -> "aroma-veil".equals(action.get("ability")))
                .findFirst()
                .orElseThrow();
        assertEquals("ability-immune", block.get("result"));
        assertEquals("aroma-veil", block.get("ability"));
        assertEquals("Protected-A", block.get("target"));
    }

    @Test
    void playRound_pranksterLetsTauntMoveBeforeRegularStatusMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Prankster-A", 220, 80, "", 60, "prankster") + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("TrickRoom-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 303031L);
        setMoves(state, true, 0, List.of(move("Taunt", "taunt", 0, 100, 0, 3, 17, 10), protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Trick Room", "trick-room", 0, 100, -7, 3, 1, 12)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "taunt",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        int tauntIndex = firstActionIndex(actions, "player", "Taunt");
        int trickRoomIndex = firstActionIndex(actions, "opponent", "Trick Room");
        assertTrue(tauntIndex >= 0);
        assertTrue(trickRoomIndex >= 0);
        assertTrue(tauntIndex < trickRoomIndex);
    }

    @Test
    void playRound_pranksterStatusMoveFailsAgainstOpposingDarkType() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Prankster-A", 220, 80, "", 60, "prankster") + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Dark-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 303032L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setTypes(state, false, 0, List.of(DamageCalculatorUtil.TYPE_DARK));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "thunder-wave",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, opponentTeamState.get(0).get("condition"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        Map<String, Object> fail = actions.stream()
                .filter(action -> "Thunder Wave".equals(action.get("move")) && "Dark-Opp".equals(action.get("target")))
                .findFirst()
                .orElseThrow();
        assertEquals("dark-prankster-immune", fail.get("result"));
    }

    @Test
    void playRound_sporeAppliesSleepAndStopsTargetAction() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Spore-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 515253L);
        setMoves(state, true, 0, List.of(
                move("Spore", "spore", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "spore",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("sleep", opponentTeamState.get(0).get("condition"));
        assertTrue((Integer) opponentTeamState.get(0).get("sleepTurns") >= 1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Target-Opp".equals(action.get("actor")) && "asleep".equals(action.get("result"))));
    }

    @Test
    void playRound_sporeFailsAgainstGrassType() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Spore-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Grass-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 525354L);
        setMoves(state, true, 0, List.of(
                move("Spore", "spore", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) state.get("opponentTeam");
        opponentTeamState.get(0).put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_GRASS, "name", "Grass")));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "spore",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_toxicDealsIncreasingEndTurnDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Toxic-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 90) + "," +
                pokemonJson("Partner-Opp", 180, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 616263L);
        setMoves(state, true, 0, List.of(
                move("Toxic", "toxic", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_POISON, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> afterToxic = engine.playRound(state, Map.of(
                "slot-0", "toxic",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) afterToxic.get("opponentTeam");
        assertEquals("toxic", opponentTeamState.get(0).get("condition"));
        assertEquals(150, opponentTeamState.get(0).get("currentHp"));

        setMoves(afterToxic, true, 0, List.of(protectMove()));
        setMoves(afterToxic, true, 1, List.of(protectMove()));
        setMoves(afterToxic, false, 0, List.of(protectMove()));
        setMoves(afterToxic, false, 1, List.of(protectMove()));

        Map<String, Object> afterSecondTurn = engine.playRound(afterToxic, Map.of(
                "slot-0", "protect",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) afterSecondTurn.get("opponentTeam");
        assertEquals(130, updatedOpponentTeam.get(0).get("currentHp"));
        assertEquals(3, updatedOpponentTeam.get(0).get("toxicCounter"));
    }

    @Test
    void playRound_poisonPowderAppliesPoison() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Powder-A", 180, 120) + "," +
                pokemonJson("Partner-A", 180, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 160, 90) + "," +
                pokemonJson("Partner-Opp", 180, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 616264L);
        setMoves(state, true, 0, List.of(
                move("Poison Powder", "poison-powder", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_POISON, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "poison-powder",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals("poison", opponentTeamState.get(0).get("condition"));
    }

    @Test
    void playRound_sporeFailsAgainstSafetyGoggles() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Spore-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Goggles-Opp", 220, 90, "safety-goggles", 20) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 525355L);
        setMoves(state, true, 0, List.of(
                move("Spore", "spore", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "spore",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_sporeFailsAgainstOvercoat() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Spore-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Overcoat-Opp", 220, 90, "", 20, "overcoat") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 525356L);
        setMoves(state, true, 0, List.of(
                move("Spore", "spore", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 10),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "spore",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));
    }

    @Test
    void playRound_ragePowderDoesNotRedirectSafetyGogglesAttacker() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Redirect-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Goggles-Opp", 220, 90, "safety-goggles", 90) + "," +
                pokemonJson("Partner-Opp", 220, 80) + "," +
                pokemonJson("Bench-C", 110, 70) + "," +
                pokemonJson("Bench-D", 108, 60) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 626263L);
        setMoves(state, true, 0, List.of(
                move("Rage Powder", "rage-powder", 0, 100, 2, 3, DamageCalculatorUtil.TYPE_GRASS, 7),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "rage-powder",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) updatedState.get("playerTeam");
        int redirectorHp = (Integer) playerTeamState.get(0).get("currentHp");
        int partnerHp = (Integer) playerTeamState.get(1).get("currentHp");
        assertEquals(220, redirectorHp);
        assertTrue(partnerHp < 220);
    }

    @Test
    void playRound_reflectReducesPhysicalDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Reflect-A", 240, 120) + "," +
                pokemonJson("Partner-A", 240, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Physical-Opp", 220, 90, "", 90) + "," +
                pokemonJson("Partner-Opp", 220, 80) + "," +
                pokemonJson("Bench-C", 110, 70) + "," +
                pokemonJson("Bench-D", 108, 60) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 717171L);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayer = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        int baselineTotalHp = ((Integer) baselinePlayer.get(0).get("currentHp")) + ((Integer) baselinePlayer.get(1).get("currentHp"));

        Map<String, Object> screened = engine.createBattleState(playerTeam, opponentTeam, 12, 717171L);
        setMoves(screened, true, 0, List.of(
                move("Reflect", "reflect", 0, 100, 0, 3, 14, 13),
                move("Strike", "strike", 20, 100, 0, 1, 1, 10)
        ));
        setMoves(screened, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(screened, false, 0, List.of(move("Strike", "strike", 90, 100, 0, 1, 1, 10)));
        setMoves(screened, false, 1, List.of(protectMove()));
        Map<String, Object> screenedRound = engine.playRound(screened, Map.of(
                "slot-0", "reflect",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> screenedPlayer = (List<Map<String, Object>>) screenedRound.get("playerTeam");
        int screenedTotalHp = ((Integer) screenedPlayer.get(0).get("currentHp")) + ((Integer) screenedPlayer.get(1).get("currentHp"));
        assertTrue(screenedTotalHp > baselineTotalHp);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) screenedRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("playerReflectTurns") > 0);
    }

    @Test
    void playRound_lightScreenReducesSpecialDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Screen-A", 240, 120) + "," +
                pokemonJson("Partner-A", 240, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Special-Opp", 220, 90, "", 90) + "," +
                pokemonJson("Partner-Opp", 220, 80) + "," +
                pokemonJson("Bench-C", 110, 70) + "," +
                pokemonJson("Bench-D", 108, 60) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 727272L);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 0, List.of(move("Special Strike", "special-strike", 90, 100, 0, 2, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpp = (List<Map<String, Object>>) baseline.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> baselineOppStats = (Map<String, Object>) baselineOpp.get(0).get("stats");
        baselineOppStats.put("specialAttack", 140);
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayer = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        int baselineTotalHp = ((Integer) baselinePlayer.get(0).get("currentHp")) + ((Integer) baselinePlayer.get(1).get("currentHp"));

        Map<String, Object> screened = engine.createBattleState(playerTeam, opponentTeam, 12, 727272L);
        setMoves(screened, true, 0, List.of(
                move("Light Screen", "light-screen", 0, 100, 0, 3, 14, 13),
                move("Strike", "strike", 20, 100, 0, 1, 1, 10)
        ));
        setMoves(screened, true, 1, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(screened, false, 0, List.of(move("Special Strike", "special-strike", 90, 100, 0, 2, 1, 10)));
        setMoves(screened, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> screenedOpp = (List<Map<String, Object>>) screened.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> screenedOppStats = (Map<String, Object>) screenedOpp.get(0).get("stats");
        screenedOppStats.put("specialAttack", 140);
        Map<String, Object> screenedRound = engine.playRound(screened, Map.of(
                "slot-0", "light-screen",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> screenedPlayer = (List<Map<String, Object>>) screenedRound.get("playerTeam");
        int screenedTotalHp = ((Integer) screenedPlayer.get(0).get("currentHp")) + ((Integer) screenedPlayer.get(1).get("currentHp"));
        assertTrue(screenedTotalHp > baselineTotalHp);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) screenedRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("playerLightScreenTurns") > 0);
    }

    @Test
    void playRound_sleepEndsAndPokemonActsAfterWaking() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Player-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Sleeper-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 535455L);
        setMoves(state, true, 0, List.of(protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) state.get("opponentTeam");
        opponentTeamState.get(0).put("condition", "sleep");
        opponentTeamState.get(0).put("sleepTurns", 1);
        opponentTeamState.get(0).put("sleepAppliedRound", 0);

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "protect",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedOpponentTeam = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertEquals(null, updatedOpponentTeam.get(0).get("condition"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "Sleeper-Opp".equals(action.get("actor")) && "Strike".equals(action.get("move"))));
    }

    @Test
    void playRound_confuseRayCanCauseSelfHit() {
        BattleEngine engine = createEngine();

        boolean found = false;
        for (long seed = 1L; seed < 240L; seed++) {
            String playerTeam = "[" +
                    pokemonJson("Confuse-A", 220, 120) + "," +
                    pokemonJson("Partner-A", 220, 70) + "," +
                    pokemonJson("Bench-A", 110, 60) + "," +
                    pokemonJson("Bench-B", 108, 50) +
                    "]";
            String opponentTeam = "[" +
                    pokemonJson("Target-Opp", 220, 90) + "," +
                    pokemonJson("Partner-Opp", 220, 60) + "," +
                    pokemonJson("Bench-C", 110, 50) + "," +
                    pokemonJson("Bench-D", 108, 40) +
                    "]";

            Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, seed);
            setMoves(state, true, 0, List.of(
                    move("Confuse Ray", "confuse-ray", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GHOST, 10),
                    protectMove()
            ));
            setMoves(state, true, 1, List.of(protectMove()));
            setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
            setMoves(state, false, 1, List.of(protectMove()));

            Map<String, Object> afterConfuse = engine.playRound(state, Map.of(
                    "slot-0", "confuse-ray",
                    "target-slot-0", "0",
                    "slot-1", "protect"
            ));
            Map<String, Object> afterConfusionRound = engine.playRound(afterConfuse, Map.of(
                    "slot-0", "protect",
                    "slot-1", "protect"
            ));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) afterConfusionRound.get("rounds");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
            if (actions.stream().anyMatch(action -> "Target-Opp".equals(action.get("actor")) && "confused-self-hit".equals(action.get("result")))) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void playRound_frozenPokemonCanLoseAction() {
        BattleEngine engine = createEngine();

        boolean found = false;
        for (long seed = 1L; seed < 240L; seed++) {
            String playerTeam = "[" +
                    pokemonJson("Player-A", 220, 120) + "," +
                    pokemonJson("Partner-A", 220, 70) + "," +
                    pokemonJson("Bench-A", 110, 60) + "," +
                    pokemonJson("Bench-B", 108, 50) +
                    "]";
            String opponentTeam = "[" +
                    pokemonJson("Frozen-Opp", 220, 90) + "," +
                    pokemonJson("Partner-Opp", 220, 60) + "," +
                    pokemonJson("Bench-C", 110, 50) + "," +
                    pokemonJson("Bench-D", 108, 40) +
                    "]";

            Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, seed);
            setMoves(state, true, 0, List.of(protectMove()));
            setMoves(state, true, 1, List.of(protectMove()));
            setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
            setMoves(state, false, 1, List.of(protectMove()));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> opponentTeamState = (List<Map<String, Object>>) state.get("opponentTeam");
            opponentTeamState.get(0).put("condition", "freeze");

            Map<String, Object> updatedState = engine.playRound(state, Map.of(
                    "slot-0", "protect",
                    "slot-1", "protect"
            ));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) updatedState.get("rounds");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
            if (actions.stream().anyMatch(action -> "Frozen-Opp".equals(action.get("actor")) && "frozen".equals(action.get("result")))) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void playRound_switchingOutClearsTaunt() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Target-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 70) + "," +
                pokemonJson("Bench-A", 220, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 90) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 404040L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerTeamState = (List<Map<String, Object>>) state.get("playerTeam");
        playerTeamState.get(0).put("tauntTurns", 2);
        setMoves(state, true, 0, List.of(protectMove()));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> afterSwitch = engine.playRound(state, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "2",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> switchedPlayer = (List<Map<String, Object>>) afterSwitch.get("playerTeam");
        assertEquals(0, switchedPlayer.get(0).get("tauntTurns"));
    }

    @Test
    void createBattleState_drizzleStartsRainAndBoostsWaterDamage() {
        BattleEngine engine = createEngine();

        String baselineTeam = "[" +
                pokemonJson("Lead-A", 220, 120) + "," +
                pokemonJson("Water-A", 220, 90, "", 80) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String drizzleTeam = "[" +
                pokemonJson("Drizzle-A", 220, 120, "", 60, "drizzle") + "," +
                pokemonJson("Water-A", 220, 90, "", 80) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(baselineTeam, opponentTeam, 12, 919191L);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Water Strike", "water-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_WATER, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "water-strike",
                "target-slot-1", "0"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineRound.get("opponentTeam");
        int baselineHp = (Integer) baselineOpponent.get(0).get("currentHp");

        Map<String, Object> drizzle = engine.createBattleState(drizzleTeam, opponentTeam, 12, 919191L);
        setMoves(drizzle, true, 0, List.of(protectMove()));
        setMoves(drizzle, true, 1, List.of(move("Water Strike", "water-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_WATER, 10)));
        setMoves(drizzle, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(drizzle, false, 1, List.of(protectMove()));
        Map<String, Object> drizzleRound = engine.playRound(drizzle, Map.of(
                "slot-0", "protect",
                "slot-1", "water-strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> drizzleOpponent = (List<Map<String, Object>>) drizzleRound.get("opponentTeam");
        int drizzleHp = (Integer) drizzleOpponent.get(0).get("currentHp");
        assertTrue(drizzleHp < baselineHp);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) drizzleRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("rainTurns") > 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> openingRounds = (List<Map<String, Object>>) drizzle.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> openingEvents = (List<String>) openingRounds.get(0).get("events");
        assertTrue(openingEvents.stream().anyMatch(event -> event.contains("大雨")));
    }

    @Test
    void playRound_sunnyDayBoostsFireDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Sun-A", 220, 120) + "," +
                pokemonJson("Fire-A", 220, 90, "", 80) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 929292L);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Fire Strike", "fire-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_FIRE, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAfterWait = engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "protect"
        ));
        Map<String, Object> baselineRound = engine.playRound(baselineAfterWait, Map.of(
                "slot-0", "protect",
                "slot-1", "fire-strike",
                "target-slot-1", "0"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineRound.get("opponentTeam");
        int baselineHp = (Integer) baselineOpponent.get(0).get("currentHp");

        Map<String, Object> sunny = engine.createBattleState(playerTeam, opponentTeam, 12, 929292L);
        setMoves(sunny, true, 0, List.of(
                move("Sunny Day", "sunny-day", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_FIRE, 7),
                protectMove()
        ));
        setMoves(sunny, true, 1, List.of(move("Fire Strike", "fire-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_FIRE, 10)));
        setMoves(sunny, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(sunny, false, 1, List.of(protectMove()));
        Map<String, Object> sunnyAfterSetup = engine.playRound(sunny, Map.of(
                "slot-0", "sunny-day",
                "slot-1", "protect"
        ));
        Map<String, Object> sunnyRound = engine.playRound(sunnyAfterSetup, Map.of(
                "slot-0", "protect",
                "slot-1", "fire-strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sunnyOpponent = (List<Map<String, Object>>) sunnyRound.get("opponentTeam");
        int sunnyHp = (Integer) sunnyOpponent.get(0).get("currentHp");
        assertTrue(sunnyHp < baselineHp);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) sunnyRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("sunTurns") > 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) sunnyRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) rounds.get(1).get("events");
        assertTrue(events.stream().anyMatch(event -> event.contains("阳光")));
    }

    @Test
    void playRound_electricTerrainBlocksSleep() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Spore-Opp", 220, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 939393L);
        setMoves(state, true, 0, List.of(
                move("Electric Terrain", "electric-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 12),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(
                move("Spore", "spore", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 10),
                protectMove()
        ));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> setupRound = engine.playRound(state, Map.of(
                "slot-0", "electric-terrain",
                "slot-1", "protect"
        ));
        Map<String, Object> blockedRound = engine.playRound(setupRound, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerBattleTeam = (List<Map<String, Object>>) blockedRound.get("playerTeam");
        assertEquals(null, playerBattleTeam.get(0).get("condition"));
        assertEquals(null, playerBattleTeam.get(1).get("condition"));

        @SuppressWarnings("unchecked")
        Map<String, Object> electricFieldEffects = (Map<String, Object>) blockedRound.get("fieldEffects");
        assertTrue((Integer) electricFieldEffects.get("electricTerrainTurns") > 0);
    }

    @Test
    void playRound_electricTerrainBoostsElectricDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Electric-A", 220, 90, "", 80) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 949494L);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Electric Strike", "electric-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAfterWait = engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "protect"
        ));
        Map<String, Object> baselineRound = engine.playRound(baselineAfterWait, Map.of(
                "slot-0", "protect",
                "slot-1", "electric-strike",
                "target-slot-1", "0"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineRound.get("opponentTeam");
        int baselineHp = (Integer) baselineOpponent.get(0).get("currentHp");

        Map<String, Object> terrain = engine.createBattleState(playerTeam, opponentTeam, 12, 949494L);
        setMoves(terrain, true, 0, List.of(
                move("Electric Terrain", "electric-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 12),
                protectMove()
        ));
        setMoves(terrain, true, 1, List.of(move("Electric Strike", "electric-strike", 80, 100, 0, 1, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(terrain, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(terrain, false, 1, List.of(protectMove()));
        Map<String, Object> terrainSetup = engine.playRound(terrain, Map.of(
                "slot-0", "electric-terrain",
                "slot-1", "protect"
        ));
        Map<String, Object> terrainRound = engine.playRound(terrainSetup, Map.of(
                "slot-0", "protect",
                "slot-1", "electric-strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> terrainOpponent = (List<Map<String, Object>>) terrainRound.get("opponentTeam");
        int terrainHp = (Integer) terrainOpponent.get(0).get("currentHp");
        assertTrue(terrainHp < baselineHp);
    }

    @Test
    void playRound_psychicTerrainBlocksPriorityMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Priority-Opp", 220, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 959595L);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(protectMove()));
        setMoves(baseline, false, 0, List.of(move("Quick Attack", "quick-attack", 70, 100, 1, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAfterWait = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> baselineRound = engine.playRound(baselineAfterWait, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayer = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        int baselineHp = ((Integer) baselinePlayer.get(0).get("currentHp")) + ((Integer) baselinePlayer.get(1).get("currentHp"));

        Map<String, Object> terrain = engine.createBattleState(playerTeam, opponentTeam, 12, 959595L);
        setMoves(terrain, true, 0, List.of(
                move("Psychic Terrain", "psychic-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_PSYCHIC, 12),
                move("Strike", "strike", 20, 100, 0, 1, 1, 10)
        ));
        setMoves(terrain, true, 1, List.of(protectMove()));
        setMoves(terrain, false, 0, List.of(move("Quick Attack", "quick-attack", 70, 100, 1, 1, 1, 10)));
        setMoves(terrain, false, 1, List.of(protectMove()));
        Map<String, Object> terrainSetup = engine.playRound(terrain, Map.of(
                "slot-0", "psychic-terrain",
                "slot-1", "protect"
        ));
        Map<String, Object> terrainRound = engine.playRound(terrainSetup, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> terrainPlayer = (List<Map<String, Object>>) terrainRound.get("playerTeam");
        int terrainHp = ((Integer) terrainPlayer.get(0).get("currentHp")) + ((Integer) terrainPlayer.get(1).get("currentHp"));
        assertTrue(terrainHp >= baselineHp);

        @SuppressWarnings("unchecked")
        Map<String, Object> psychicFieldEffects = (Map<String, Object>) terrainRound.get("fieldEffects");
        assertTrue((Integer) psychicFieldEffects.get("psychicTerrainTurns") > 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) terrainRound.get("rounds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
        assertTrue(actions.stream().anyMatch(action -> "psychic-terrain-blocked".equals(action.get("result"))));
    }

    @Test
    void playRound_psychicTerrainBoostsPsychicDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Psychic-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 260, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 969696L);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Psychic Strike", "psychic-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_PSYCHIC, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAfterWait = engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "protect"
        ));
        Map<String, Object> baselineRound = engine.playRound(baselineAfterWait, Map.of(
                "slot-0", "protect",
                "slot-1", "psychic-strike",
                "target-slot-1", "0"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineRound.get("opponentTeam");
        int baselineHp = (Integer) baselineOpponent.get(0).get("currentHp");

        Map<String, Object> terrain = engine.createBattleState(playerTeam, opponentTeam, 12, 969696L);
        setMoves(terrain, true, 0, List.of(
                move("Psychic Terrain", "psychic-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_PSYCHIC, 12),
                protectMove()
        ));
        setMoves(terrain, true, 1, List.of(move("Psychic Strike", "psychic-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_PSYCHIC, 10)));
        setMoves(terrain, false, 0, List.of(move("Strike", "strike", 20, 100, 0, 1, 1, 10)));
        setMoves(terrain, false, 1, List.of(protectMove()));
        Map<String, Object> terrainSetup = engine.playRound(terrain, Map.of(
                "slot-0", "psychic-terrain",
                "slot-1", "protect"
        ));
        Map<String, Object> terrainRound = engine.playRound(terrainSetup, Map.of(
                "slot-0", "protect",
                "slot-1", "psychic-strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> terrainOpponent = (List<Map<String, Object>>) terrainRound.get("opponentTeam");
        int terrainHp = (Integer) terrainOpponent.get(0).get("currentHp");
        assertTrue(terrainHp < baselineHp);
    }

    @Test
    void createBattleState_electricSurgeActivatesTerrain() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Surge-A", 220, 120, "", 60, "electric-surge") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 80) + "," +
                pokemonJson("Partner-Opp", 220, 70) + "," +
                pokemonJson("Bench-C", 110, 60) + "," +
                pokemonJson("Bench-D", 108, 50) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 979797L);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) state.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("electricTerrainTurns") > 0);
    }

    @Test
    void chooseAISwitch_switchesToTypeResistantCandidateWhenThreatened() {
        // Use a TypeEfficacyMapper where Normal moves are resisted by Grass-type (factor 50).
        // This lets us verify that the AI picks the Grass-type bench mon when the active
        // Normal-type mon is critically threatened by the player's Normal-type attacks.
        BattleEngine engine = new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), new TypeEfficacyMapper() {
            private static final int TYPE_NORMAL = 1;
            private static final int TYPE_GRASS = 12;

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
                // Normal vs Grass = 50 (resisted – Grass-type bench is the safest switch-in)
                if (damageTypeId == TYPE_NORMAL && targetTypeId == TYPE_GRASS) return 50;
                return 100;
            }
                }, new ObjectMapper());

        // Player has two Normal-type attackers on the field using Normal-type moves.
        // Opponent has a low-HP Normal-type lead and a Grass-type bench that resists
        // Normal moves (factor 50). The AI should prefer Grass-Defender as a switch target.
        String playerTeam = buildPlayerTeamForSwitchTestJson();
        String opponentTeam = buildOpponentTeamForSwitchTestJson();

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 11111L);

        // Manually set the active opponent's HP to 20% (critical threshold) to guarantee switch
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentBattleTeam = (List<Map<String, Object>>) state.get("opponentTeam");
        Map<String, Object> activeMon = opponentBattleTeam.get(0);
        int maxHp = (Integer) ((Map<?, ?>) activeMon.get("stats")).get("hp");
        activeMon.put("currentHp", maxHp / 5); // 20% HP – critical

        // Play many rounds to observe that the AI eventually switches in the grass-type defender
        boolean switchedToGrass = false;
        Map<String, Object> currentState = state;
        for (int i = 0; i < 20 && !switchedToGrass; i++) {
            currentState = engine.playRound(currentState, Map.of());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) currentState.get("rounds");
            @SuppressWarnings("unchecked")
            List<String> events = (List<String>) rounds.get(rounds.size() - 1).get("events");
            if (events.stream().anyMatch(e -> e.contains("Grass-Defender"))) {
                switchedToGrass = true;
            }
            if ("completed".equals(currentState.get("status"))) {
                break;
            }
            // Reset the active mon HP to keep it in critical zone for subsequent rounds
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> oppTeam = (List<Map<String, Object>>) currentState.get("opponentTeam");
            if (!oppTeam.isEmpty() && (Integer) oppTeam.get(0).get("currentHp") > 0) {
                oppTeam.get(0).put("currentHp", (Integer) ((Map<?, ?>) oppTeam.get(0).get("stats")).get("hp") / 5);
            }
        }

        assertTrue(switchedToGrass, "AI should have switched in Grass-Defender as a type-resistant cover when active mon was in critical HP");
    }

    @Test
    void createBattleState_sandStreamStartsSandstormAndBoostsRockSpecialDefense() {
        BattleEngine engine = createEngine();

        String baselinePlayer = "[" +
                pokemonJson("Lead-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String sandPlayer = "[" +
                pokemonJson("Sand-A", 220, 120, "", 60, "sand-stream") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Special-Opp", 260, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(baselinePlayer, opponentTeam, 12, 515151L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayerTeam = (List<Map<String, Object>>) baseline.get("playerTeam");
        baselinePlayerTeam.get(0).put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_ROCK, "name", "Rock")));
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(protectMove()));
        setMoves(baseline, false, 0, List.of(move("Special Strike", "special-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> sand = engine.createBattleState(sandPlayer, opponentTeam, 12, 515151L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sandPlayerTeam = (List<Map<String, Object>>) sand.get("playerTeam");
        sandPlayerTeam.get(0).put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_ROCK, "name", "Rock")));
        setMoves(sand, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, 1, 10)));
        setMoves(sand, true, 1, List.of(protectMove()));
        setMoves(sand, false, 0, List.of(move("Special Strike", "special-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10)));
        setMoves(sand, false, 1, List.of(protectMove()));
        Map<String, Object> sandRound = engine.playRound(sand, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineAfter = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sandAfter = (List<Map<String, Object>>) sandRound.get("playerTeam");
        assertTrue((Integer) sandAfter.get(0).get("currentHp") > (Integer) baselineAfter.get(0).get("currentHp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) sand.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("sandTurns") > 0);
    }

    @Test
    void playRound_sandstormDamagesNonImmuneTargetsButNotOvercoat() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Sand-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 70) + "," +
                pokemonJson("Overcoat-Opp", 220, 60, "", 60, "overcoat") + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 525252L);
        setMoves(state, true, 0, List.of(
                move("Sandstorm", "sandstorm", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ROCK, 13),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(protectMove()));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updatedState = engine.playRound(state, Map.of(
                "slot-0", "sandstorm",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updatedState.get("opponentTeam");
        assertTrue((Integer) opponentState.get(0).get("currentHp") < 220);
        assertEquals(220, opponentState.get(1).get("currentHp"));
    }

    @Test
    void playRound_grassyTerrainHealsGroundedPokemonAndBoostsGrassDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Grass-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 400, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 535353L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayerState = (List<Map<String, Object>>) baseline.get("playerTeam");
        baselinePlayerState.get(0).put("currentHp", 180);
        setMoves(baseline, true, 0, List.of(protectMove()));
        setMoves(baseline, true, 1, List.of(move("Grass Strike", "grass-strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_GRASS, 10)));
        setMoves(baseline, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, 1, 10)));
        setMoves(baseline, false, 1, List.of(protectMove()));
        engine.playRound(baseline, Map.of(
                "slot-0", "protect",
                "slot-1", "grass-strike",
                "target-slot-1", "0"
        ));

        Map<String, Object> grassy = engine.createBattleState(playerTeam, opponentTeam, 12, 535353L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grassyPlayerState = (List<Map<String, Object>>) grassy.get("playerTeam");
        grassyPlayerState.get(0).put("currentHp", 180);
        setMoves(grassy, true, 0, List.of(
                move("Grassy Terrain", "grassy-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_GRASS, 13),
                protectMove()
        ));
        setMoves(grassy, true, 1, List.of(move("Grass Strike", "grass-strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_GRASS, 10)));
        setMoves(grassy, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, 1, 10)));
        setMoves(grassy, false, 1, List.of(protectMove()));
        Map<String, Object> grassySetupRound = engine.playRound(grassy, Map.of(
                "slot-0", "grassy-terrain",
                "slot-1", "protect"
        ));
        Map<String, Object> grassyRound = engine.playRound(grassySetupRound, Map.of(
                "slot-0", "protect",
                "slot-1", "grass-strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grassyOpponent = (List<Map<String, Object>>) grassyRound.get("opponentTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grassyPlayerAfter = (List<Map<String, Object>>) grassySetupRound.get("playerTeam");
        assertTrue((Integer) grassyOpponent.get(0).get("currentHp") < 400);
        assertTrue((Integer) grassyPlayerAfter.get(0).get("currentHp") > 180);

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldEffects = (Map<String, Object>) grassyRound.get("fieldEffects");
        assertTrue((Integer) fieldEffects.get("grassyTerrainTurns") > 0);
    }

    @Test
    void playRound_mistyTerrainBlocksStatusAndWeakensDragonDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Terrain-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Status-Opp", 220, 70) + "," +
                pokemonJson("Dragon-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 545454L);
        setMoves(state, true, 0, List.of(
                move("Misty Terrain", "misty-terrain", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_FAIRY, 13),
                protectMove()
        ));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Thunder Wave", "thunder-wave", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        setMoves(state, false, 1, List.of(move("Dragon Strike", "dragon-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_DRAGON, 10)));

        Map<String, Object> afterTerrain = engine.playRound(state, Map.of(
                "slot-0", "misty-terrain",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerAfterTerrain = (List<Map<String, Object>>) afterTerrain.get("playerTeam");
        assertEquals(null, playerAfterTerrain.get(0).get("condition"));

        Map<String, Object> baseline = engine.createBattleState(playerTeam, opponentTeam, 12, 545454L);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1, 1, 10)));
        setMoves(baseline, true, 1, List.of(protectMove()));
        setMoves(baseline, false, 0, List.of(protectMove()));
        setMoves(baseline, false, 1, List.of(move("Dragon Strike", "dragon-strike", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_DRAGON, 10)));
        Map<String, Object> baselineRound = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> terrainDamageRound = engine.playRound(afterTerrain, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayer = (List<Map<String, Object>>) baselineRound.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> terrainPlayer = (List<Map<String, Object>>) terrainDamageRound.get("playerTeam");
        assertTrue((Integer) terrainPlayer.get(0).get("currentHp") > (Integer) baselinePlayer.get(0).get("currentHp"));
    }

    @Test
    void playRound_flashFireAbsorbsFireAndBoostsNextFireAttack() {
        BattleEngine engine = createEngine();

        String boostedPlayer = "[" +
                pokemonJson("Flash-A", 220, 120, "", 60, "flash-fire") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String baselinePlayer = "[" +
                pokemonJson("Flash-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 320, 70) + "," +
                pokemonJson("Fire-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> boosted = engine.createBattleState(boostedPlayer, opponentTeam, 12, 565656L);
        keepOnlyActiveSlot(boosted, true, 0);
        keepOnlyActiveSlot(boosted, false, 1);
        setMoves(boosted, true, 0, List.of(
                move("Fire Strike", "fire-strike", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10),
                move("Strike", "strike", 30, 100, 0, 1, 1, 10)
        ));
        setMoves(boosted, false, 1, List.of(move("Fire Strike", "fire-strike", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10)));
        Map<String, Object> boostedSetup = engine.playRound(boosted, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedPlayerAfterSetup = (List<Map<String, Object>>) boostedSetup.get("playerTeam");
        assertEquals(Boolean.TRUE, boostedPlayerAfterSetup.get(0).get("flashFireBoost"));
        Map<String, Object> boostedAttack = engine.playRound(boostedSetup, Map.of(
                "slot-0", "fire-strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> baseline = engine.createBattleState(baselinePlayer, opponentTeam, 12, 565656L);
        keepOnlyActiveSlot(baseline, true, 0);
        keepOnlyActiveSlot(baseline, false, 1);
        setMoves(baseline, true, 0, List.of(
                move("Fire Strike", "fire-strike", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10),
                protectMove()
        ));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAttack = engine.playRound(baseline, Map.of(
                "slot-0", "fire-strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedOpponent = (List<Map<String, Object>>) boostedAttack.get("opponentTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineAttack.get("opponentTeam");
        assertTrue((Integer) boostedOpponent.get(1).get("currentHp") < (Integer) baselineOpponent.get(1).get("currentHp"));
    }

    @Test
    void playRound_stormDrainAbsorbsWaterAndBoostsSpecialAttack() {
        BattleEngine engine = createEngine();

        String boostedPlayer = "[" +
                pokemonJson("Drain-A", 220, 120, "", 60, "storm-drain") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String baselinePlayer = "[" +
                pokemonJson("Drain-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 320, 70) + "," +
                pokemonJson("Water-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> boosted = engine.createBattleState(boostedPlayer, opponentTeam, 12, 575757L);
        keepOnlyActiveSlot(boosted, true, 0);
        keepOnlyActiveSlot(boosted, false, 1);
        setMoves(boosted, true, 0, List.of(
                move("Water Pulse", "water-pulse", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10),
                move("Strike", "strike", 30, 100, 0, 1, 1, 10)
        ));
        setMoves(boosted, false, 1, List.of(move("Water Pulse", "water-pulse", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10)));
        Map<String, Object> boostedSetup = engine.playRound(boosted, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedPlayerAfterSetup = (List<Map<String, Object>>) boostedSetup.get("playerTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> boostedStages = (Map<String, Object>) boostedPlayerAfterSetup.get(0).get("statStages");
        assertEquals(1, boostedStages.get("specialAttack"));
        Map<String, Object> boostedAttack = engine.playRound(boostedSetup, Map.of(
                "slot-0", "water-pulse",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> baseline = engine.createBattleState(baselinePlayer, opponentTeam, 12, 575757L);
        keepOnlyActiveSlot(baseline, true, 0);
        keepOnlyActiveSlot(baseline, false, 1);
        setMoves(baseline, true, 0, List.of(
                move("Water Pulse", "water-pulse", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10),
                protectMove()
        ));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAttack = engine.playRound(baseline, Map.of(
                "slot-0", "water-pulse",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedOpponent = (List<Map<String, Object>>) boostedAttack.get("opponentTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineAttack.get("opponentTeam");
        assertTrue((Integer) boostedOpponent.get(1).get("currentHp") < (Integer) baselineOpponent.get(1).get("currentHp"));
    }

    @Test
    void playRound_sapSipperAbsorbsGrassAndBoostsAttack() {
        BattleEngine engine = createEngine();

        String boostedPlayer = "[" +
                pokemonJson("Sap-A", 220, 120, "", 60, "sap-sipper") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String baselinePlayer = "[" +
                pokemonJson("Sap-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 320, 70) + "," +
                pokemonJson("Grass-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> boosted = engine.createBattleState(boostedPlayer, opponentTeam, 12, 585858L);
        keepOnlyActiveSlot(boosted, true, 0);
        keepOnlyActiveSlot(boosted, false, 1);
        setMoves(boosted, true, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10), protectMove()));
        setMoves(boosted, false, 1, List.of(move("Leaf Hit", "leaf-hit", 70, 100, 0, 1, DamageCalculatorUtil.TYPE_GRASS, 10)));
        Map<String, Object> boostedSetup = engine.playRound(boosted, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedPlayerAfterSetup = (List<Map<String, Object>>) boostedSetup.get("playerTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> boostedStages = (Map<String, Object>) boostedPlayerAfterSetup.get(0).get("statStages");
        assertEquals(1, boostedStages.get("attack"));
        Map<String, Object> boostedAttack = engine.playRound(boostedSetup, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        Map<String, Object> baseline = engine.createBattleState(baselinePlayer, opponentTeam, 12, 585858L);
        keepOnlyActiveSlot(baseline, true, 0);
        keepOnlyActiveSlot(baseline, false, 1);
        setMoves(baseline, true, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10), protectMove()));
        setMoves(baseline, false, 1, List.of(protectMove()));
        Map<String, Object> baselineAttack = engine.playRound(baseline, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> boostedOpponent = (List<Map<String, Object>>) boostedAttack.get("opponentTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineAttack.get("opponentTeam");
        assertTrue((Integer) boostedOpponent.get(1).get("currentHp") < (Integer) baselineOpponent.get(1).get("currentHp"));
    }

    @Test
    void playRound_regeneratorHealsOnManualSwitch() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Regen-A", 240, 120, "", 60, "regenerator") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 180, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 595959L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) state.get("playerTeam");
        playerState.get(0).put("currentHp", 120);

        Map<String, Object> switched = engine.playRound(state, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "2",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> switchedPlayer = (List<Map<String, Object>>) switched.get("playerTeam");
        assertTrue((Integer) switchedPlayer.get(0).get("currentHp") > 120);
    }

    @Test
    void playRound_rockyHelmetDamagesContactAttacker() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Helmet-A", 220, 120, "rocky-helmet", 60) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Contact-Opp", 240, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 606060L);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertTrue((Integer) opponentState.get(0).get("currentHp") < 240);
    }

    @Test
    void playRound_mentalHerbClearsTauntImmediately() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Herb-A", 220, 120, "mental-herb", 60) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Taunt-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 616161L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Taunt", "taunt", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_DARK, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        assertEquals(0, playerState.get(0).get("tauntTurns"));
        assertEquals(Boolean.TRUE, playerState.get(0).get("itemConsumed"));
    }

    @Test
    void playRound_lightningRodAbsorbsElectricAndBoostsSpecialAttack() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Rod-A", 220, 120, "", 60, "lightning-rod") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Shock-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 626262L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Thunderbolt", "thunderbolt", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) playerState.get(0).get("statStages");
        assertEquals(1, stages.get("specialAttack"));
        assertEquals(220, playerState.get(0).get("currentHp"));
    }

    @Test
    void playRound_waterAbsorbHealsOnWaterHit() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Water-A", 240, 120, "", 60, "water-absorb") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Water-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 636363L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) state.get("playerTeam");
        playerState.get(0).put("currentHp", 120);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Water Pulse", "water-pulse", 70, 100, 0, 2, DamageCalculatorUtil.TYPE_WATER, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedPlayer = (List<Map<String, Object>>) updated.get("playerTeam");
        assertTrue((Integer) updatedPlayer.get(0).get("currentHp") > 120);
    }

    @Test
    void playRound_voltAbsorbHealsOnElectricHit() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Volt-A", 240, 120, "", 60, "volt-absorb") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Shock-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 646464L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) state.get("playerTeam");
        playerState.get(0).put("currentHp", 120);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Thunderbolt", "thunderbolt", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedPlayer = (List<Map<String, Object>>) updated.get("playerTeam");
        assertTrue((Integer) updatedPlayer.get(0).get("currentHp") > 120);
    }

    @Test
    void playRound_motorDriveAbsorbsElectricAndBoostsSpeed() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Motor-A", 220, 120, "", 60, "motor-drive") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Shock-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 656565L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Thunderbolt", "thunderbolt", 80, 100, 0, 2, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) playerState.get(0).get("statStages");
        assertEquals(1, stages.get("speed"));
        assertEquals(220, playerState.get(0).get("currentHp"));
    }

    @Test
    void applyIntimidate_triggersDefiantBoost() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Intimidate-A", 220, 120, "", 60, "intimidate") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Defiant-Opp", 220, 70, "", 60, "defiant") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 666666L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) state.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(1, stages.get("attack"));
    }

    @Test
    void applyIntimidate_isBlockedByClearBody() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Intimidate-A", 220, 120, "", 60, "intimidate") + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Body-Opp", 220, 70, "", 60, "clear-body") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 666667L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) state.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(0, stages.get("attack"));
    }

    @Test
    void playRound_icyWindTriggersCompetitiveBoost() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Wind-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Competitive-Opp", 220, 70, "", 60, "competitive") + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 676767L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Icy Wind", "icy-wind", 55, 100, 0, 2, DamageCalculatorUtil.TYPE_ICE, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "icy-wind",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(2, stages.get("specialAttack"));
        assertEquals(-1, stages.get("speed"));
    }

    @Test
    void playRound_weaknessPolicyBoostsAfterSuperEffectiveHit() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Fire-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Policy-Opp", 240, 70, "weakness-policy", 60) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 686868L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setTypes(state, false, 0, List.of(DamageCalculatorUtil.TYPE_GRASS));
        setMoves(state, true, 0, List.of(move("Flamethrower", "flamethrower", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "flamethrower",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        assertEquals(2, stages.get("attack"));
        assertEquals(2, stages.get("specialAttack"));
        assertEquals(Boolean.TRUE, opponentState.get(0).get("itemConsumed"));
    }

    @Test
    void playRound_playerTerastallizationReducesSuperEffectiveDamage() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Tera-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Fire-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> baselineState = engine.createBattleState(playerTeam, opponentTeam, 12, 696969L);
        keepOnlyActiveSlot(baselineState, true, 0);
        keepOnlyActiveSlot(baselineState, false, 0);
        setTypes(baselineState, true, 0, List.of(DamageCalculatorUtil.TYPE_GRASS));
        setTeraType(baselineState, true, 0, DamageCalculatorUtil.TYPE_WATER, "Water");
        setMoves(baselineState, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(baselineState, false, 0, List.of(move("Flamethrower", "flamethrower", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10)));

        Map<String, Object> terastallizedState = engine.createBattleState(playerTeam, opponentTeam, 12, 696969L);
        keepOnlyActiveSlot(terastallizedState, true, 0);
        keepOnlyActiveSlot(terastallizedState, false, 0);
        setTypes(terastallizedState, true, 0, List.of(DamageCalculatorUtil.TYPE_GRASS));
        setTeraType(terastallizedState, true, 0, DamageCalculatorUtil.TYPE_WATER, "Water");
        setMoves(terastallizedState, true, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));
        setMoves(terastallizedState, false, 0, List.of(move("Flamethrower", "flamethrower", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10)));

        Map<String, Object> baselineUpdated = engine.playRound(baselineState, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "protect"
        ));
        Map<String, Object> teraUpdated = engine.playRound(terastallizedState, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "tera-slot-0", "true",
                "slot-1", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselinePlayer = (List<Map<String, Object>>) baselineUpdated.get("playerTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> teraPlayer = (List<Map<String, Object>>) teraUpdated.get("playerTeam");
        assertTrue((Integer) teraPlayer.get(0).get("currentHp") > (Integer) baselinePlayer.get(0).get("currentHp"));
        assertEquals(Boolean.TRUE, teraPlayer.get(0).get("terastallized"));
        assertEquals(Boolean.TRUE, teraUpdated.get("playerTeraUsed"));
    }

    @Test
    void playRound_megaEvolutionConsumesSpecialSystemAndBlocksTeraLater() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Mega-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 697070L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 5, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 7)));
        setMegaEligible(state, true, 0);
        setTeraType(state, true, 0, DamageCalculatorUtil.TYPE_WATER, "Water");

        Map<String, Object> megaRound = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "special-slot-0", "mega"
        ));
        Map<String, Object> blockedTeraRound = engine.playRound(megaRound, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "special-slot-0", "tera"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) blockedTeraRound.get("playerTeam");
        assertEquals(Boolean.TRUE, playerState.get(0).get("megaEvolved"));
        assertEquals(Boolean.FALSE, playerState.get(0).get("terastallized"));
        assertEquals("mega", blockedTeraRound.get("playerSpecialType"));
    }

    @Test
    void playRound_zMoveBoostsDamageOnce() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Z-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 240, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> baselineState = engine.createBattleState(playerTeam, opponentTeam, 12, 697171L);
        keepOnlyActiveSlot(baselineState, true, 0);
        keepOnlyActiveSlot(baselineState, false, 0);
        setMoves(baselineState, true, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(baselineState, false, 0, List.of(move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 7)));

        Map<String, Object> zState = engine.createBattleState(playerTeam, opponentTeam, 12, 697171L);
        keepOnlyActiveSlot(zState, true, 0);
        keepOnlyActiveSlot(zState, false, 0);
        setMoves(zState, true, 0, List.of(move("Strike", "strike", 70, 100, 0, 1, 1, 10)));
        setMoves(zState, false, 0, List.of(move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 7)));
        setZMoveEligible(zState, true, 0);

        Map<String, Object> baselineUpdated = engine.playRound(baselineState, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0"
        ));
        Map<String, Object> zUpdated = engine.playRound(zState, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "special-slot-0", "z-move"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baselineOpponent = (List<Map<String, Object>>) baselineUpdated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> zOpponent = (List<Map<String, Object>>) zUpdated.get("opponentTeam");
        assertTrue((Integer) zOpponent.get(0).get("currentHp") < (Integer) baselineOpponent.get(0).get("currentHp"));
        assertEquals("z-move", zUpdated.get("playerSpecialType"));
    }

    @Test
    void playRound_dynamaxDoublesHpOnActivation() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Dyna-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Support-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 697272L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Strike", "strike", 50, 100, 0, 1, 1, 10)));
        setMoves(state, false, 0, List.of(move("Tailwind", "tailwind", 0, 100, 0, 3, 1, 7)));
        setDynamaxEligible(state, true, 0);

        Map<String, Object> roundOne = engine.playRound(state, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "special-slot-0", "dynamax"
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> roundOnePlayer = (List<Map<String, Object>>) roundOne.get("playerTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> roundOneStats = (Map<String, Object>) roundOnePlayer.get(0).get("stats");
        assertEquals(Boolean.TRUE, roundOnePlayer.get(0).get("dynamaxed"));
        assertTrue((Integer) roundOneStats.get("hp") > 220);
        assertEquals("dynamax", roundOne.get("playerSpecialType"));
    }

    @Test
    void playRound_opponentAiUsesMatchingTerastallization() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Grass-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Fire-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 797979L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setTypes(state, true, 0, List.of(DamageCalculatorUtil.TYPE_GRASS));
        setMoves(state, true, 0, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Flamethrower", "flamethrower", 90, 100, 0, 2, DamageCalculatorUtil.TYPE_FIRE, 10)));
        setTeraType(state, false, 0, DamageCalculatorUtil.TYPE_FIRE, "Fire");

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "protect"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertEquals(Boolean.TRUE, opponentState.get(0).get("terastallized"));
        assertEquals(Boolean.TRUE, updated.get("opponentTeraUsed"));
    }

    @Test
    void playRound_wideGuardBlocksSpreadMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Guard-A", 220, 120) + "," +
                pokemonJson("Guard-B", 220, 110) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Wind-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 707070L);
        setMoves(state, true, 0, List.of(move("Wide Guard", "wide-guard", 0, 100, 3, 3, 1, 7)));
        setMoves(state, true, 1, List.of(protectMove()));
        setMoves(state, false, 0, List.of(move("Icy Wind", "icy-wind", 55, 100, 0, 2, DamageCalculatorUtil.TYPE_ICE, 11)));
        setMoves(state, false, 1, List.of(protectMove()));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "wide-guard",
                "slot-1", "protect",
                "target-slot-1", "1"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        assertEquals(220, playerState.get(0).get("currentHp"));
        assertEquals(220, playerState.get(1).get("currentHp"));
    }

    @Test
    void playRound_quickGuardBlocksPriorityMove() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Quick-A", 220, 120) + "," +
                pokemonJson("Quick-B", 220, 110) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("FakeOut-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 717171L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Quick Guard", "quick-guard", 0, 100, 3, 3, 1, 7)));
        setMoves(state, false, 0, List.of(move("Fake Out", "fake-out", 40, 100, 3, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "quick-guard",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> playerState = (List<Map<String, Object>>) updated.get("playerTeam");
        assertEquals(220, playerState.get(0).get("currentHp"));
        assertFalse(Boolean.TRUE.equals(playerState.get(0).get("flinched")));
    }

    @Test
    void playRound_feintBreaksProtect() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Feint-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Protect-Opp", 220, 100) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 727272L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Feint", "feint", 30, 100, 2, 1, 1, 10)));
        setMoves(state, false, 0, List.of(protectMove()));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "feint",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        assertTrue((Integer) opponentState.get(0).get("currentHp") < 220);
    }

    @Test
    void playRound_partingShotDropsStatsAndSwitchesUser() {
        BattleEngine engine = createEngine();

        String playerTeam = "[" +
                pokemonJson("Parting-A", 220, 120) + "," +
                pokemonJson("Partner-A", 220, 90) + "," +
                pokemonJson("Bench-A", 110, 60) + "," +
                pokemonJson("Bench-B", 108, 50) +
                "]";
        String opponentTeam = "[" +
                pokemonJson("Target-Opp", 220, 70) + "," +
                pokemonJson("Partner-Opp", 220, 60) + "," +
                pokemonJson("Bench-C", 110, 50) + "," +
                pokemonJson("Bench-D", 108, 40) +
                "]";

        Map<String, Object> state = engine.createBattleState(playerTeam, opponentTeam, 12, 737373L);
        keepOnlyActiveSlot(state, true, 0);
        keepOnlyActiveSlot(state, false, 0);
        setMoves(state, true, 0, List.of(move("Parting Shot", "parting-shot", 0, 100, 0, 3, 17, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 30, 100, 0, 1, 1, 10)));

        Map<String, Object> updated = engine.playRound(state, Map.of(
                "slot-0", "parting-shot",
                "target-slot-0", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> opponentState = (List<Map<String, Object>>) updated.get("opponentTeam");
        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) opponentState.get(0).get("statStages");
        @SuppressWarnings("unchecked")
        List<Integer> playerActiveSlots = (List<Integer>) updated.get("playerActiveSlots");
        assertEquals(-1, stages.get("attack"));
        assertEquals(-1, stages.get("specialAttack"));
        assertEquals(List.of(1), playerActiveSlots);
    }

    private static String buildPlayerTeamForSwitchTestJson() {
        // Player team uses normal-type damaging moves (type_id=1)
        return "[" +
                "{\"name\":\"Normal-Striker\",\"name_en\":\"normal-striker\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":200,\"attack\":120,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":100}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":80,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}," +
                "{\"name\":\"Normal-Partner\",\"name_en\":\"normal-partner\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":200,\"attack\":110,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":90}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":80,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}," +
                "{\"name\":\"Bench-P1\",\"name_en\":\"bench-p1\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":180,\"attack\":90,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":80}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":60,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}," +
                "{\"name\":\"Bench-P2\",\"name_en\":\"bench-p2\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":180,\"attack\":90,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":75}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":60,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}" +
                "]";
    }

    private static String buildOpponentTeamForSwitchTestJson() {
        // Opponent: two normal-type leads (vulnerable to nothing in mock), a grass-type bench (resists Normal moves factor=50)
        return "[" +
                "{\"name\":\"Normal-Lead\",\"name_en\":\"normal-lead\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":200,\"attack\":100,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":85}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":60,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}," +
                "{\"name\":\"Normal-Support\",\"name_en\":\"normal-support\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":200,\"attack\":90,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":80}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":60,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}," +
                "{\"name\":\"Grass-Defender\",\"name_en\":\"grass-defender\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":12,\"name\":\"Grass\"}]," +
                "\"stats\":{\"hp\":220,\"attack\":95,\"defense\":90,\"specialAttack\":80,\"specialDefense\":90,\"speed\":70}," +
                "\"moves\":[{\"name\":\"Vine Whip\",\"name_en\":\"vine-whip\",\"power\":45,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":12,\"target_id\":10}]}," +
                "{\"name\":\"Bench-Opp2\",\"name_en\":\"bench-opp2\",\"battleScore\":200,\"heldItem\":\"\"," +
                "\"ability\":{\"name_en\":\"\",\"name\":\"\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":180,\"attack\":85,\"defense\":75,\"specialAttack\":70,\"specialDefense\":70,\"speed\":65}," +
                "\"moves\":[{\"name\":\"Tackle\",\"name_en\":\"tackle\",\"power\":50,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]}" +
                "]";
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
                                if (damageTypeId == null || targetTypeId == null) {
                                        return 100;
                                }
                                if (damageTypeId == DamageCalculatorUtil.TYPE_FIRE && targetTypeId == DamageCalculatorUtil.TYPE_GRASS) {
                                        return 200;
                                }
                                if (damageTypeId == DamageCalculatorUtil.TYPE_NORMAL && targetTypeId == DamageCalculatorUtil.TYPE_GRASS) {
                                        return 50;
                                }
                return 100;
            }
                }, new ObjectMapper());
    }

    private static String pokemonJson(String name, int hp, int speed) {
        return pokemonJson(name, hp, speed, "", 60, "", 10);
    }

    private static String pokemonJson(String name, int hp, int speed, String heldItem, int power) {
        return pokemonJson(name, hp, speed, heldItem, power, "", 10);
    }

    private static String pokemonJson(String name, int hp, int speed, String heldItem, int power, String abilityNameEn) {
        return pokemonJson(name, hp, speed, heldItem, power, abilityNameEn, 10);
    }

    private static String pokemonJson(String name, int hp, int speed, String heldItem, int power, String abilityNameEn, int targetId) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"name_en\":\"" + name.toLowerCase() + "\"," +
                "\"battleScore\":200," +
                "\"heldItem\":\"" + heldItem + "\"," +
                "\"ability\":{\"name_en\":\"" + abilityNameEn + "\",\"name\":\"" + abilityNameEn + "\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":" + hp + ",\"attack\":90,\"defense\":80,\"specialAttack\":70,\"specialDefense\":70,\"speed\":" + speed + "}," +
                "\"moves\":[" +
                "{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":" + power + ",\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":" + targetId + "}," +
                "{\"name\":\"Guard\",\"name_en\":\"protect\",\"power\":0,\"accuracy\":100,\"priority\":4,\"damage_class_id\":1,\"type_id\":1,\"target_id\":7}" +
                "]" +
                "}";
    }

    @SuppressWarnings("unchecked")
    private static void setMoves(Map<String, Object> state, boolean player, int teamIndex, List<Map<String, Object>> moves) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        team.get(teamIndex).put("moves", moves);
    }

        @SuppressWarnings("unchecked")
    private static void setTypes(Map<String, Object> state, boolean player, int teamIndex, List<Integer> typeIds) {
                List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
                List<Map<String, Object>> types = typeIds.stream()
                                .map(typeId -> Map.<String, Object>of("type_id", typeId, "name", String.valueOf(typeId)))
                                .toList();
                team.get(teamIndex).put("types", types);
        }

    @SuppressWarnings("unchecked")
    private static void setTeraType(Map<String, Object> state, boolean player, int teamIndex, int typeId, String typeName) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        team.get(teamIndex).put("teraTypeId", typeId);
        team.get(teamIndex).put("teraType", Map.of("type_id", typeId, "name", typeName, "name_en", typeName.toLowerCase()));
        team.get(teamIndex).put("terastallized", false);
    }

    @SuppressWarnings("unchecked")
    private static void setMegaEligible(Map<String, Object> state, boolean player, int teamIndex) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        Map<String, Object> mon = team.get(teamIndex);
        mon.put("megaEligible", true);
        Map<String, Object> stats = (Map<String, Object>) mon.get("stats");
        mon.put("megaStats", Map.of(
                "hp", stats.get("hp"),
                "attack", 120,
                "defense", 100,
                "specialAttack", 100,
                "specialDefense", 90,
                "speed", 130
        ));
    }

    @SuppressWarnings("unchecked")
    private static void setZMoveEligible(Map<String, Object> state, boolean player, int teamIndex) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        team.get(teamIndex).put("zMoveEligible", true);
        team.get(teamIndex).put("zMoveUsed", false);
    }

    @SuppressWarnings("unchecked")
    private static void setDynamaxEligible(Map<String, Object> state, boolean player, int teamIndex) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        team.get(teamIndex).put("dynamaxEligible", true);
        team.get(teamIndex).put("dynamaxed", false);
        team.get(teamIndex).put("dynamaxTurnsRemaining", 0);
    }

        private static void keepOnlyActiveSlot(Map<String, Object> state, boolean player, int teamIndex) {
                state.put(player ? "playerActiveSlots" : "opponentActiveSlots", List.of(teamIndex));
        }

    private static int firstActionIndex(List<Map<String, Object>> actions, String side, String moveName) {
        for (int index = 0; index < actions.size(); index++) {
            Map<String, Object> action = actions.get(index);
            if (side.equals(action.get("side")) && moveName.equals(action.get("move"))) {
                return index;
            }
        }
        return -1;
    }

    private static Map<String, Object> move(String name, String nameEn, int power, int accuracy, int priority, int damageClassId, int typeId, int targetId) {
        return Map.of(
                "name", name,
                "name_en", nameEn,
                "power", power,
                "accuracy", accuracy,
                "priority", priority,
                "damage_class_id", damageClassId,
                "type_id", typeId,
                "target_id", targetId
        );
    }

    private static Map<String, Object> withEffectChance(Map<String, Object> move, int effectChance) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("effect_chance", effectChance);
        return copied;
    }

    private static Map<String, Object> withFlinchChance(Map<String, Object> move, int flinchChance) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("flinch_chance", flinchChance);
        return copied;
    }

    private static Map<String, Object> withAilmentMeta(Map<String, Object> move, int ailmentChance, String ailmentNameEn) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("ailment_chance", ailmentChance);
        copied.put("ailment_name_en", ailmentNameEn);
        return copied;
    }

    private static Map<String, Object> withHitRange(Map<String, Object> move, int minHits, int maxHits) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("min_hits", minHits);
        copied.put("max_hits", maxHits);
        return copied;
    }

    private static Map<String, Object> withCritRate(Map<String, Object> move, int critRate) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("crit_rate", critRate);
        return copied;
    }

    private static Map<String, Object> withEffectShort(Map<String, Object> move, String effectShort) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("effect_short", effectShort);
        return copied;
    }

    private static Map<String, Object> withMetaStatChanges(Map<String, Object> move, int statChance, int statId, int change) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("stat_chance", statChance);
        copied.put("metaStatChanges", List.of(Map.of("stat_id", statId, "change", change)));
        return copied;
    }

    private static Map<String, Object> withDrain(Map<String, Object> move, int drain) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("drain", drain);
        return copied;
    }

    private static Map<String, Object> withHealing(Map<String, Object> move, int healing) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("healing", healing);
        return copied;
    }

    private static Map<String, Object> withFlags(Map<String, Object> move, String... flags) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>(move);
        copied.put("flags", List.of(flags));
        return copied;
    }

        private static Map<String, Object> contactMove(String name, String nameEn, int power, int typeId) {
                Map<String, Object> move = new java.util.LinkedHashMap<>(move(name, nameEn, power, 100, 0, 1, typeId, 10));
                move.put("contact", true);
                return move;
        }

    private static Map<String, Object> protectMove() {
        return move("Guard", "protect", 0, 100, 4, 3, 1, 7);
    }
}
