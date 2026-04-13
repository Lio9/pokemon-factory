package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleFlowSupportTest {

    @Test
    void prepareReplacementPhase_setsPlayerReplacementPromptAndOptions() {
        BattleFlowSupport support = createSupport();
        Map<String, Object> state = battleState(
                List.of(
                        pokemon("Fainted-A", 0, 90),
                        pokemon("Active-A", 180, 80),
                        pokemon("Bench-A", 160, 70)
                ),
                List.of(pokemon("Opp-A", 180, 70)),
                List.of(0, 1),
                List.of(0)
        );
        state.put("currentRound", 3);
        List<String> events = new ArrayList<>();

        support.prepareReplacementPhase(state, events);

        assertEquals("replacement", state.get("phase"));
        assertEquals(1, state.get("playerPendingReplacementCount"));
        assertEquals(List.of(2), state.get("playerPendingReplacementOptions"));
        assertTrue(events.stream().anyMatch(event -> event.contains("请选择替补上场")));
    }

    @Test
    void prepareReplacementPhase_autoFillsOpponentBenchSlots() {
        BattleFlowSupport support = createSupport();
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 180, 90)),
                List.of(pokemon("Fainted-Opp", 0, 80), pokemon("Bench-Opp", 160, 70)),
                List.of(0),
                List.of(0)
        );
        state.put("currentRound", 4);
        List<String> events = new ArrayList<>();

        support.prepareReplacementPhase(state, events);

        assertEquals("battle", state.get("phase"));
        assertEquals(List.of(1), state.get("opponentActiveSlots"));
        assertTrue(events.stream().anyMatch(event -> event.contains("对手 派出了 Bench-Opp")));
    }

    @Test
    void resolveBattleResult_marksPlayerWinnerByRemainingHpOnRoundLimit() {
        BattleFlowSupport support = createSupport();
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 120, 90)),
                List.of(pokemon("Opp-A", 60, 80)),
                List.of(0),
                List.of(0)
        );
        state.put("currentRound", 12);
        state.put("roundLimit", 12);
        state.put("exchangeUsed", false);

        support.resolveBattleResult(state);

        assertEquals("completed", state.get("status"));
        assertEquals("completed", state.get("phase"));
        assertEquals("player", state.get("winner"));
        assertEquals(Boolean.TRUE, state.get("exchangeAvailable"));
    }

    private static BattleFlowSupport createSupport() {
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
        BattleConditionSupport conditionSupport = new BattleConditionSupport(engine, damageSupport, fieldEffectSupport);
        return new BattleFlowSupport(engine, conditionSupport, 2);
    }

    private static BattleEngine createEngine() {
        return new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), createTypeMapper(), new ObjectMapper());
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

    private static Map<String, Object> battleState(List<Map<String, Object>> playerTeam, List<Map<String, Object>> opponentTeam,
                                                   List<Integer> playerSlots, List<Integer> opponentSlots) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("playerTeam", playerTeam);
        state.put("playerActiveSlots", playerSlots);
        state.put("opponentTeam", opponentTeam);
        state.put("opponentActiveSlots", opponentSlots);
        state.put("phase", "battle");
        state.put("status", "running");
        return state;
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("heldItem", "");
        pokemon.put("ability", "");
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", Math.max(1, hp),
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
                "specialDefense", 90,
                "speed", speed
        )));
        pokemon.put("types", List.of(Map.of("type_id", 1, "name", "normal")));
        return pokemon;
    }
}