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

class BattleTurnCleanupSupportTest {

    @Test
    void applyEndTurnEffects_magicGuardIgnoresBurnAndSandstormDamage() {
        BattleTurnCleanupSupport support = createSupport();
        Map<String, Object> guardMon = pokemon("Guard-A", 160, "magic-guard", "burn", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> normalMon = pokemon("Normal-A", 160, "", "burn", DamageCalculatorUtil.TYPE_NORMAL);

        Map<String, Object> state = battleState(guardMon, normalMon);
        Map<String, Object> fieldEffects = new LinkedHashMap<>();
        fieldEffects.put("sandTurns", 3);
        state.put("fieldEffects", fieldEffects);

        List<String> events = new ArrayList<>();
        support.applyEndTurnEffects(state, new LinkedHashMap<>(), events, new Random(42), 1);

        assertEquals(150, guardMon.get("currentHp"));
        assertEquals(140, normalMon.get("currentHp"));
    }

    private static BattleTurnCleanupSupport createSupport() {
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
        BattleConditionSupport conditionSupport = new BattleConditionSupport(engine, damageSupport, fieldEffectSupport);
        return new BattleTurnCleanupSupport(engine, fieldEffectSupport, conditionSupport);
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

    private static Map<String, Object> battleState(Map<String, Object> playerMon, Map<String, Object> opponentMon) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("playerTeam", List.of(playerMon));
        state.put("playerActiveSlots", List.of(0));
        state.put("opponentTeam", List.of(opponentMon));
        state.put("opponentActiveSlots", List.of(0));
        state.put("currentRound", 1);
        return state;
    }

    private static Map<String, Object> pokemon(String name, int hp, String ability, String condition, int typeId) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("ability", ability);
        pokemon.put("condition", condition);
        pokemon.put("heldItem", "");
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
                "specialDefense", 90,
                "speed", 80
        )));
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        return pokemon;
    }
}

