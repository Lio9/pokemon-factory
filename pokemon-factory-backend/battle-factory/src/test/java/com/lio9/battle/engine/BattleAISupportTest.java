package com.lio9.battle.engine;



/**
 * BattleAISupportTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleAISupportTest 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
 * 阅读建议：建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
import static org.junit.jupiter.api.Assertions.assertNull;

class BattleAISupportTest {

    @Test
    void selectAISleepMove_returnsSporeWhenTargetCanBeSlept() {
        BattleAISupport support = createSupport();
        Map<String, Object> mon = pokemon("Spore-A", 220, 100, DamageCalculatorUtil.TYPE_GRASS,
                List.of(move("Spore", "spore", 0, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_GRASS, 10)));
        mon.put("entryRound", 1);
        Map<String, Object> state = battleState(
                List.of(mon),
                List.of(pokemon("Target-Opp", 220, 80, DamageCalculatorUtil.TYPE_NORMAL, List.of())));

        Map<String, Object> selected = support.selectAISleepMove(mon, state, true, 1);

        assertEquals("spore", selected.get("name_en"));
    }

    @Test
    void selectAITerrainMove_prefersPsychicTerrainAgainstPriority() {
        BattleAISupport support = createSupport();
        Map<String, Object> mon = pokemon("Terrain-A", 220, 100, DamageCalculatorUtil.TYPE_NORMAL,
                List.of(move("Psychic Terrain", "psychic-terrain", 0, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_PSYCHIC, 12)));
        Map<String, Object> opponent = pokemon("Priority-Opp", 220, 80, DamageCalculatorUtil.TYPE_NORMAL,
                List.of(move("Quick Attack", "quick-attack", 40, 100, 1,
                        DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 10)));
        Map<String, Object> state = battleState(List.of(mon), List.of(opponent));

        Map<String, Object> selected = support.selectAITerrainMove(mon, state, true, 1);

        assertEquals("psychic-terrain", selected.get("name_en"));
    }

    @Test
    void selectAIHelpingHandMove_requiresTwoActiveAllies() {
        BattleAISupport support = createSupport();
        Map<String, Object> mon = pokemon("Support-A", 220, 90, DamageCalculatorUtil.TYPE_NORMAL,
                List.of(move("Helping Hand", "helping-hand", 0, 100, 5,
                        DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_NORMAL, 3)));
        Map<String, Object> ally = pokemon("Ally-A", 220, 80, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> state = battleState(List.of(mon, ally), List.of());

        assertNull(support.selectAIHelpingHandMove(mon, state, true, 1));

        state.put("playerActiveSlots", List.of(0, 1));

        Map<String, Object> selected = support.selectAIHelpingHandMove(mon, state, true, 1);

        assertEquals("helping-hand", selected.get("name_en"));
    }

    @Test
    void selectAIWeatherMove_prefersSandstormForRockTeammates() {
        BattleAISupport support = createSupport();
        Map<String, Object> weatherSetter = pokemon("Weather-A", 220, 90, DamageCalculatorUtil.TYPE_NORMAL,
                List.of(move("Sandstorm", "sandstorm", 0, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_ROCK, 12)));
        Map<String, Object> rockAlly = pokemon("Rock-A", 220, 70, DamageCalculatorUtil.TYPE_ROCK, List.of());
        Map<String, Object> state = battleState(List.of(weatherSetter, rockAlly), List.of());
        state.put("playerActiveSlots", List.of(0, 1));

        Map<String, Object> selected = support.selectAIWeatherMove(weatherSetter, state, true, 1);

        assertEquals("sandstorm", selected.get("name_en"));
    }

    private static BattleAISupport createSupport() {
        BattleEngine engine = createEngine();
        BattleAnalysisSupport analysisSupport = new BattleAnalysisSupport(engine);
        return new BattleAISupport(engine, analysisSupport);
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

    private static Map<String, Object> battleState(List<Map<String, Object>> playerTeam, List<Map<String, Object>> opponentTeam) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("playerTeam", playerTeam);
        state.put("playerActiveSlots", playerTeam.isEmpty() ? List.of() : List.of(0));
        state.put("opponentTeam", opponentTeam);
        state.put("opponentActiveSlots", opponentTeam.isEmpty() ? List.of() : List.of(0));
        state.put("fieldEffects", new BattleFieldEffectSupport().defaultFieldEffects());
        return state;
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, int typeId, List<Map<String, Object>> moves) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("heldItem", "");
        pokemon.put("ability", "");
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
}