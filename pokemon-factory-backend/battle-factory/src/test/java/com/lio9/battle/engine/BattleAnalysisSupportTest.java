package com.lio9.battle.engine;



/**
 * BattleAnalysisSupportTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleAnalysisSupportTest 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleAnalysisSupportTest {

    @Test
    void opposingSideLikelyFaster_countsOpponentTailwindBoost() {
        BattleAnalysisSupport support = createSupport();
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 220, 100, 100, 90, DamageCalculatorUtil.TYPE_NORMAL, List.of())),
                List.of(pokemon("Tailwind-Opp", 220, 60, 100, 90, DamageCalculatorUtil.TYPE_NORMAL, List.of()))) ;
        state.put("fieldEffects", new LinkedHashMap<>(Map.of("opponentTailwindTurns", 4)));

        assertTrue(support.opposingSideLikelyFaster(state, true));
    }

    @Test
    void opposingSideLikelyUsingStatus_ignoresTauntedTargets() {
        BattleAnalysisSupport support = createSupport();
        Map<String, Object> tauntedTarget = pokemon(
                "Taunted-Opp",
                220,
                80,
                100,
                110,
                DamageCalculatorUtil.TYPE_NORMAL,
                List.of(move("Spore", "spore", 0, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_GRASS, 10)));
        tauntedTarget.put("tauntTurns", 2);

        Map<String, Object> state = battleState(List.of(pokemon("Player-A", 220, 100, 100, 90,
                        DamageCalculatorUtil.TYPE_NORMAL, List.of())), List.of(tauntedTarget));

        assertFalse(support.opposingSideLikelyUsingStatus(state, true));
    }

    @Test
    void sidePrefersTerrain_requiresGroundedUser() {
        BattleAnalysisSupport support = createSupport();
        Map<String, Object> airborneUser = pokemon(
                "Airborne-A",
                220,
                100,
                100,
                110,
                DamageCalculatorUtil.TYPE_FLYING,
                List.of(move("Thunderbolt", "thunderbolt", 90, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_ELECTRIC, 10)));
        Map<String, Object> state = battleState(List.of(airborneUser), List.of());

        assertFalse(support.sidePrefersTerrain(state, true, DamageCalculatorUtil.TYPE_ELECTRIC));

        airborneUser.put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_NORMAL, "name", "normal")));

        assertTrue(support.sidePrefersTerrain(state, true, DamageCalculatorUtil.TYPE_ELECTRIC));
    }

    @Test
    void opposingSideCanBeSlept_skipsGrassTargets() {
        BattleAnalysisSupport support = createSupport();
        Map<String, Object> grassTarget = pokemon("Grass-Opp", 220, 80, 100, 90,
                DamageCalculatorUtil.TYPE_GRASS, List.of());
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 220, 100, 100, 90, DamageCalculatorUtil.TYPE_NORMAL, List.of())),
                List.of(grassTarget));

        assertFalse(support.opposingSideCanBeSlept(state, true));

        grassTarget.put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_NORMAL, "name", "normal")));

        assertTrue(support.opposingSideCanBeSlept(state, true));
    }

    @Test
    void opposingSideCanSleepAlly_detectsSporeUser() {
        BattleAnalysisSupport support = createSupport();
        Map<String, Object> state = battleState(
                List.of(pokemon("Player-A", 220, 100, 100, 90, DamageCalculatorUtil.TYPE_NORMAL, List.of())),
                List.of(pokemon("Spore-Opp", 220, 80, 100, 90, DamageCalculatorUtil.TYPE_GRASS,
                        List.of(move("Spore", "spore", 0, 100, 0,
                                DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_GRASS, 10)))));

        assertTrue(support.opposingSideCanSleepAlly(state, true));
    }

    private static BattleAnalysisSupport createSupport() {
        return new BattleAnalysisSupport(createEngine());
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
        state.put("playerActiveSlots", activeSlots(playerTeam));
        state.put("opponentTeam", opponentTeam);
        state.put("opponentActiveSlots", activeSlots(opponentTeam));
        state.put("fieldEffects", new BattleFieldEffectSupport().defaultFieldEffects());
        return state;
    }

    private static List<Integer> activeSlots(List<Map<String, Object>> team) {
        return team.isEmpty() ? List.of() : List.of(0);
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, int attack, int specialAttack,
                                               int typeId, List<Map<String, Object>> moves) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("moves", moves);
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", attack,
                "defense", 90,
                "specialAttack", specialAttack,
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