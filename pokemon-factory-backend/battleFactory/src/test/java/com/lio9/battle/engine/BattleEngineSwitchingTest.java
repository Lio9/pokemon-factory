package com.lio9.battle.engine;

import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.common.mapper.TypeEfficacyMapper;
import com.lio9.common.util.DamageCalculatorUtil;
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

        Map<String, Object> burnedRound = engine.playRound(afterBurn, Map.of(
                "slot-0", "strike",
                "target-slot-0", "0",
                "slot-1", "strike",
                "target-slot-1", "0"
        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> burnedPlayerTeam = (List<Map<String, Object>>) burnedRound.get("playerTeam");
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
        });
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

    private static Map<String, Object> protectMove() {
        return move("Guard", "protect", 0, 100, 4, 3, 1, 7);
    }
}
