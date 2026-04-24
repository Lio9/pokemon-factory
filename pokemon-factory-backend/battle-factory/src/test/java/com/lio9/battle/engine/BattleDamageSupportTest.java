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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleDamageSupportTest {

    @Test
    void speedValue_appliesStageScarfAndTailwind() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> mon = new LinkedHashMap<>();
        mon.put("stats", new LinkedHashMap<>(Map.of("speed", 100)));
        mon.put("heldItem", "choice-scarf");
        mon.put("statStages", new LinkedHashMap<>(Map.of("speed", 1)));

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>(Map.of("playerTailwindTurns", 4)));

        assertEquals(450, support.speedValue(mon, state, true));
    }

    @Test
    void applyIncomingDamage_usesFocusSashOnLethalHit() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = new LinkedHashMap<>();
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", "Sash-Mon");
        target.put("heldItem", "focus-sash");
        target.put("itemConsumed", false);
        target.put("currentHp", 100);
        target.put("stats", new LinkedHashMap<>(Map.of("hp", 100)));

        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        int remainingHp = support.applyIncomingDamage(attacker, target, 180, actionLog, events);

        assertEquals(1, remainingHp);
        assertEquals(99, actionLog.get("damage"));
        assertEquals(Boolean.TRUE, actionLog.get("focusSash"));
        assertEquals(Boolean.TRUE, target.get("itemConsumed"));
        assertEquals("", target.get("heldItem"));
        assertTrue(events.stream().anyMatch(event -> event.contains("气势披带")));
    }

    @Test
    void applyIncomingDamage_sturdySurvivesLethalHitAtOneHp() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = new LinkedHashMap<>();
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", "Sturdy-Mon");
        target.put("ability", "sturdy");
        target.put("heldItem", "");
        target.put("itemConsumed", false);
        target.put("currentHp", 100);
        target.put("stats", new LinkedHashMap<>(Map.of("hp", 100)));

        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        int remainingHp = support.applyIncomingDamage(attacker, target, 180, actionLog, events);

        assertEquals(1, remainingHp);
        assertEquals(99, actionLog.get("damage"));
        assertEquals(Boolean.TRUE, actionLog.get("sturdy"));
        assertTrue(events.stream().anyMatch(event -> event.contains("结实")));
    }

    @Test
    void applyIncomingDamage_moldBreakerBypassesSturdy() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = new LinkedHashMap<>(Map.of("ability", "mold-breaker"));
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", "Sturdy-Mon");
        target.put("ability", "sturdy");
        target.put("heldItem", "");
        target.put("itemConsumed", false);
        target.put("currentHp", 100);
        target.put("stats", new LinkedHashMap<>(Map.of("hp", 100)));

        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        int remainingHp = support.applyIncomingDamage(attacker, target, 180, actionLog, events);

        assertEquals(0, remainingHp);
        assertEquals(180, actionLog.get("damage"));
        assertEquals(null, actionLog.get("sturdy"));
        assertTrue(events.isEmpty());
    }

    @Test
    void calculateDamage_rainAndHelpingHandIncreaseWaterDamage() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Attacker", 120, 100, 110, 90, "", DamageCalculatorUtil.TYPE_WATER);
        Map<String, Object> defender = pokemon("Defender", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> move = move("Surf", "surf", 90, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10);

        Map<String, Object> dryState = new LinkedHashMap<>();
        dryState.put("fieldEffects", new LinkedHashMap<>());
        int baselineDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), dryState);

        Map<String, Object> rainState = new LinkedHashMap<>();
        rainState.put("fieldEffects", new LinkedHashMap<>(Map.of("rainTurns", 4)));
        int boostedDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(attacker, true), rainState);

        assertTrue(boostedDamage > baselineDamage);
    }

    @Test
    void calculateDamage_multiscaleAndShadowShieldHalveDamageAtFullHp() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Attacker", 120, 100, 110, 90, "", DamageCalculatorUtil.TYPE_WATER);
        Map<String, Object> normalDefender = pokemon("Normal-Def", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> multiscaleDefender = pokemon("Multi-Def", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        multiscaleDefender.put("ability", "multiscale");
        Map<String, Object> shadowShieldDefender = pokemon("Shield-Def", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        shadowShieldDefender.put("ability", "shadow-shield");
        Map<String, Object> move = move("Surf", "surf", 90, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>());

        int baselineDamage = support.calculateDamage(attacker, normalDefender, move, new Random(42), Map.of(), state);
        int multiscaleDamage = support.calculateDamage(attacker, multiscaleDefender, move, new Random(42), Map.of(), state);
        int shadowShieldDamage = support.calculateDamage(attacker, shadowShieldDefender, move, new Random(42), Map.of(), state);

        assertTrue(multiscaleDamage < baselineDamage);
        assertEquals(multiscaleDamage, shadowShieldDamage);

        multiscaleDefender.put("currentHp", 100);
        int chippedDamage = support.calculateDamage(attacker, multiscaleDefender, move, new Random(42), Map.of(), state);
        assertEquals(baselineDamage, chippedDamage);
    }

    @Test
    void calculateDamage_moldBreakerBypassesMultiscale() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Breaker", 120, 100, 110, 90, "", DamageCalculatorUtil.TYPE_WATER);
        attacker.put("ability", "mold-breaker");
        Map<String, Object> normalDefender = pokemon("Normal-Def", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> multiscaleDefender = pokemon("Multi-Def", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        multiscaleDefender.put("ability", "multiscale");
        Map<String, Object> move = move("Surf", "surf", 90, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>());

        int baselineDamage = support.calculateDamage(attacker, normalDefender, move, new Random(42), Map.of(), state);
        int ignoredDamage = support.calculateDamage(attacker, multiscaleDefender, move, new Random(42), Map.of(), state);

        assertEquals(baselineDamage, ignoredDamage);
    }

    @Test
    void calculateDamage_unawareIgnoresRelevantStatStages() {
        BattleDamageSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Attacker", 120, 100, 110, 90, "", DamageCalculatorUtil.TYPE_WATER);
        Map<String, Object> defender = pokemon("Defender", 120, 80, 90, 90, "", DamageCalculatorUtil.TYPE_NORMAL);
        attacker.put("statStages", new LinkedHashMap<>(Map.of("attack", 4, "specialAttack", 0, "defense", 0, "specialDefense", 0, "speed", 0)));
        defender.put("statStages", new LinkedHashMap<>(Map.of("attack", 0, "specialAttack", 0, "defense", 4, "specialDefense", 0, "speed", 0)));
        Map<String, Object> move = move("Strike", "strike", 90, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 10);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>());

        int boostedDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), state);

        attacker.put("ability", "unaware");
        int attackerUnawareDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), state);

        attacker.put("ability", "");
        defender.put("ability", "unaware");
        int defenderUnawareDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), state);

        attacker.put("statStages", new LinkedHashMap<>(Map.of("attack", 0, "specialAttack", 0, "defense", 0, "specialDefense", 0, "speed", 0)));
        defender.put("ability", "");
        int defenderOnlyBoostedDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), state);

        defender.put("statStages", new LinkedHashMap<>(Map.of("attack", 0, "specialAttack", 0, "defense", 0, "specialDefense", 0, "speed", 0)));
        defender.put("ability", "");
        int neutralDamage = support.calculateDamage(attacker, defender, move, new Random(42), Map.of(), state);

        assertTrue(boostedDamage < attackerUnawareDamage);
        assertTrue(attackerUnawareDamage > neutralDamage);
        assertEquals(defenderOnlyBoostedDamage, defenderUnawareDamage);
    }

    private static BattleDamageSupport createSupport() {
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleEngine engine = createEngine();
        return new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
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
                if (damageTypeId == DamageCalculatorUtil.TYPE_FIRE && targetTypeId == DamageCalculatorUtil.TYPE_GRASS) {
                    return 200;
                }
                if (damageTypeId == DamageCalculatorUtil.TYPE_WATER && targetTypeId == DamageCalculatorUtil.TYPE_FIRE) {
                    return 200;
                }
                if (damageTypeId == DamageCalculatorUtil.TYPE_NORMAL && targetTypeId == DamageCalculatorUtil.TYPE_GHOST) {
                    return 0;
                }
                return 100;
            }
        };
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, int attack, int specialAttack,
                                               String heldItem, int typeId) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("heldItem", heldItem);
        pokemon.put("itemConsumed", false);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", attack,
                "defense", 90,
                "specialAttack", specialAttack,
                "specialDefense", 95,
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
