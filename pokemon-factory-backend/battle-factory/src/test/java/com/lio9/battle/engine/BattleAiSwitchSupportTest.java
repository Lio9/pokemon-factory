package com.lio9.battle.engine;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleAiSwitchSupportTest {

    @Test
    void chooseAISwitch_prefersMostResistantBenchWhenThreatened() {
        BattleAiSwitchSupport support = new BattleAiSwitchSupport(createEngine());
        Map<String, Object> activeOpponent = pokemon("Lead-Opp", 200, 80, 1,
                List.of(move("Strike", "strike", 60, 100, 0, 1, 1, 10)));
        activeOpponent.put("currentHp", 40);
        List<Map<String, Object>> opponentTeam = List.of(
                activeOpponent,
                pokemon("Grass-Defender", 220, 70, 12, List.of()),
                pokemon("Bench-Normal", 220, 70, 1, List.of())
        );
        Map<String, Object> state = battleState(
                List.of(
                        pokemon("Player-A", 220, 100, 1, List.of(move("Tackle", "tackle", 80, 100, 0, 1, 1, 10))),
                        pokemon("Player-B", 220, 95, 1, List.of(move("Body Slam", "body-slam", 85, 100, 0, 1, 1, 10)))
                ),
                opponentTeam,
                List.of(0, 1),
                List.of(0)
        );

        int selected = support.chooseAISwitch(opponentTeam, List.of(0), 0, 0, new FixedRandom(0.0d), state);

        assertEquals(1, selected);
    }

    @Test
    void chooseAISwitch_skipsHealthyTargetsWithoutThreat() {
        BattleAiSwitchSupport support = new BattleAiSwitchSupport(createEngine());
        List<Map<String, Object>> opponentTeam = List.of(
                pokemon("Lead-Opp", 200, 80, 1, List.of()),
                pokemon("Grass-Defender", 220, 70, 12, List.of())
        );
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 220, 100, 1, List.of(move("Tackle", "tackle", 80, 100, 0, 1, 1, 10)))),
                opponentTeam,
                List.of(0),
                List.of(0)
        );

        int selected = support.chooseAISwitch(opponentTeam, List.of(0), 0, 0, new FixedRandom(0.0d), state);

        assertEquals(-1, selected);
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
                if (damageTypeId != null && targetTypeId != null && damageTypeId == 1 && targetTypeId == 12) {
                    return 50;
                }
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
        return state;
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, int typeId, List<Map<String, Object>> moves) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("moves", moves);
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 90,
                "specialDefense", 90,
                "speed", speed
        )));
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
                "target_id", targetId
        );
    }

    private static final class FixedRandom extends Random {
        private final double value;

        private FixedRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}