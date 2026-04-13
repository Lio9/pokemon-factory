package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleConditionSupportTest {

    @Test
    void applySleep_electricTerrainBlocksGroundedTargets() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>(Map.of("electricTerrainTurns", 4)));

        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();
        Map<String, Object> move = move("Spore", "spore", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_GRASS, 10);
        Map<String, Object> target = pokemon("Grounded-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL);

        support.applySleep(
                state,
                pokemon("Spore-A", 220, 120, "", "", DamageCalculatorUtil.TYPE_GRASS),
            target,
                move,
                actionLog,
                events,
                new Random(42),
                1
        );

        assertEquals("status-immune", actionLog.get("result"));
        assertTrue(events.stream().anyMatch(event -> event.contains("电气场地")));
        assertNull(target.get("condition"));
    }

    @Test
    void applyTaunt_mentalHerbClearsTauntImmediately() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> target = pokemon("Herb-A", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyTaunt(
                pokemon("Taunt-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_DARK),
                target,
                actionLog,
                events
        );

        assertEquals(0, target.get("tauntTurns"));
        assertEquals(Boolean.TRUE, target.get("itemConsumed"));
        assertEquals(Boolean.TRUE, actionLog.get("mentalHerb"));
        assertTrue(events.stream().anyMatch(event -> event.contains("心灵香草")));
    }

    @Test
    void applyDefenderAbilityImmunity_lightningRodBoostsSpecialAttack() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> target = pokemon("Rod-A", 220, 120, "", "lightning-rod", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        boolean blocked = support.applyDefenderAbilityImmunity(
                target,
                move("Thunderbolt", "thunderbolt", 80, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                actionLog,
                events
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) target.get("statStages");
        assertTrue(blocked);
        assertEquals(1, stages.get("specialAttack"));
        assertEquals("ability-immune", actionLog.get("result"));
        assertEquals("lightning-rod", actionLog.get("ability"));
        assertTrue(events.stream().anyMatch(event -> event.contains("避雷针")));
    }

    @Test
    void applyIntimidate_triggersDefiantBoost() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>();
        Map<String, Object> target = pokemon("Defiant-Opp", 220, 70, "", "defiant", DamageCalculatorUtil.TYPE_NORMAL);
        state.put("playerTeam", List.of());
        state.put("playerActiveSlots", List.of());
        state.put("opponentTeam", List.of(target));
        state.put("opponentActiveSlots", List.of(0));

        Map<String, Object> source = pokemon("Intimidate-A", 220, 120, "", "intimidate", DamageCalculatorUtil.TYPE_NORMAL);
        List<String> events = new ArrayList<>();

        support.applyIntimidate(state, true, source, events);

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) target.get("statStages");
        assertEquals(1, stages.get("attack"));
        assertEquals(Boolean.TRUE, source.get("intimidateActivated"));
        assertTrue(events.stream().anyMatch(event -> event.contains("不服输")));
    }

    private static BattleConditionSupport createSupport() {
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
        return new BattleConditionSupport(engine, damageSupport, fieldEffectSupport);
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
                if (damageTypeId == null || targetTypeId == null) {
                    return 100;
                }
                if (damageTypeId == DamageCalculatorUtil.TYPE_NORMAL && targetTypeId == DamageCalculatorUtil.TYPE_GHOST) {
                    return 0;
                }
                return 100;
            }
        };
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, String heldItem, String ability, int typeId) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("heldItem", heldItem);
        pokemon.put("itemConsumed", false);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("ability", ability);
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