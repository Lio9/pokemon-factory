package com.lio9.battle.engine;



/**
 * BattleActionBuilderTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleActionBuilderTest 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
 * 阅读建议：建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleActionBuilderTest {

    @Test
    void buildPlayerActions_createsSwitchActionWhenRequested() {
        BattleEngine engine = createEngine();
        BattleActionBuilder builder = new BattleActionBuilder(engine, new BattleAiSwitchSupport(engine));
        Map<String, Object> state = battleState(
                List.of(
                        pokemon("Lead-A", 220, 100, 1, List.of(move("Strike", "strike", 80, 100, 0, 1, 1, 10))),
                        pokemon("Bench-A", 220, 70, 1, List.of())
                ),
                List.of(pokemon("Target-Opp", 220, 90, 1, List.of())),
                List.of(0),
                List.of(0)
        );

        List<BattleEngine.Action> actions = builder.buildPlayerActions(state, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "1"
        ));

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).isSwitch());
        assertEquals(1, actions.get(0).switchToTeamIndex());
    }

    @Test
    void buildOpponentActions_usesAiSwitchSupportWhenThreatened() {
        BattleEngine engine = createEngine();
        BattleActionBuilder builder = new BattleActionBuilder(engine, new BattleAiSwitchSupport(engine));
        Map<String, Object> activeOpponent = pokemon("Lead-Opp", 200, 80, 1,
                List.of(move("Strike", "strike", 60, 100, 0, 1, 1, 10)));
        activeOpponent.put("currentHp", 40);
        Map<String, Object> state = battleState(
                List.of(
                        pokemon("Player-A", 220, 100, 1, List.of(move("Tackle", "tackle", 80, 100, 0, 1, 1, 10))),
                        pokemon("Player-B", 220, 95, 1, List.of(move("Body Slam", "body-slam", 85, 100, 0, 1, 1, 10)))
                ),
                List.of(
                        activeOpponent,
                        pokemon("Grass-Defender", 220, 70, 12, List.of()),
                        pokemon("Bench-Normal", 220, 70, 1, List.of())
                ),
                List.of(0, 1),
                List.of(0)
        );
        state.put("currentRound", 1);

        List<BattleEngine.Action> actions = builder.buildOpponentActions(state, new FixedRandom(0.0d, true));

        assertEquals(1, actions.size());
        assertTrue(actions.get(0).isSwitch());
        assertEquals(1, actions.get(0).switchToTeamIndex());
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
        pokemon.put("name_en", name.toLowerCase());
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("heldItem", "");
        pokemon.put("ability", "");
        pokemon.put("entryRound", 1);
        pokemon.put("moves", moves);
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
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
        private final boolean nextBoolean;

        private FixedRandom(double value, boolean nextBoolean) {
            this.value = value;
            this.nextBoolean = nextBoolean;
        }

        @Override
        public double nextDouble() {
            return value;
        }

        @Override
        public boolean nextBoolean() {
            return nextBoolean;
        }
    }
}