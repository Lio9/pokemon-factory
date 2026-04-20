package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTargetSupportTest {

    @Test
    void resolveMoveTargets_powderImmuneAttackerIgnoresRagePowder() {
        BattleTargetSupport support = new BattleTargetSupport(createEngine());
        Map<String, Object> attacker = pokemon("Grass-A", 220, DamageCalculatorUtil.TYPE_GRASS, List.of());
        Map<String, Object> mainTarget = pokemon("Target-Opp", 220, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> powderTarget = pokemon("Powder-Opp", 220, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> state = battleState(
                List.of(attacker),
                List.of(mainTarget, powderTarget),
                List.of(0),
                List.of(0, 1)
        );
        BattleEngine.Action action = BattleEngine.Action.moveAction(
                "player", 0, 0, 0, 0,
                move("Strike", "strike", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL,
                        DamageCalculatorUtil.TYPE_NORMAL, 10),
                100,
                false
        );
        Map<String, BattleEngine.RedirectionEffect> redirectionTargets = Map.of(
                "opponent", new BattleEngine.RedirectionEffect(1, true)
        );

        List<BattleEngine.TargetRef> targets = support.resolveMoveTargets(state, action, action.move(), new Random(0), redirectionTargets);

        assertEquals(1, targets.size());
        assertEquals(0, targets.get(0).teamIndex());
    }

    @Test
    void applyHelpingHand_marksAllyBoost() {
        BattleTargetSupport support = new BattleTargetSupport(createEngine());
        Map<String, Object> supporter = pokemon("Support-A", 220, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> ally = pokemon("Striker-A", 220, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> state = battleState(
                List.of(supporter, ally),
                List.of(),
                List.of(0, 1),
                List.of()
        );
        BattleEngine.Action action = BattleEngine.Action.moveAction(
                "player", 0, 0, -1, 0,
                move("Helping Hand", "helping-hand", 0, 100, 5, DamageCalculatorUtil.DAMAGE_CLASS_STATUS,
                        DamageCalculatorUtil.TYPE_NORMAL, 3),
                100,
                false
        );
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new java.util.ArrayList<>();
        Map<Map<String, Object>, Boolean> boosts = new IdentityHashMap<>();

        support.applyHelpingHand(state, action, supporter, action.move(), actionLog, events, boosts);

        assertTrue(boosts.containsKey(ally));
        assertEquals("helping-hand", actionLog.get("result"));
        assertEquals("Striker-A", actionLog.get("target"));
        assertTrue(events.stream().anyMatch(event -> event.contains("帮助了 Striker-A")));
    }

        @Test
        void applyHelpingHand_withoutAllyFailsInsteadOfBoostingSelf() {
        BattleTargetSupport support = new BattleTargetSupport(createEngine());
        Map<String, Object> supporter = pokemon("Support-A", 220, DamageCalculatorUtil.TYPE_NORMAL, List.of());
        Map<String, Object> state = battleState(
            List.of(supporter),
            List.of(),
            List.of(0),
            List.of()
        );
        BattleEngine.Action action = BattleEngine.Action.moveAction(
            "player", 0, 0, -1, 0,
            move("Helping Hand", "helping-hand", 0, 100, 5, DamageCalculatorUtil.DAMAGE_CLASS_STATUS,
                DamageCalculatorUtil.TYPE_NORMAL, 3),
            100,
            false
        );
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new java.util.ArrayList<>();
        Map<Map<String, Object>, Boolean> boosts = new IdentityHashMap<>();

        support.applyHelpingHand(state, action, supporter, action.move(), actionLog, events, boosts);

        assertTrue(boosts.isEmpty());
        assertEquals("failed", actionLog.get("result"));
        assertTrue(events.stream().anyMatch(event -> event.contains("失败")));
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
        return state;
    }

    private static Map<String, Object> pokemon(String name, int hp, int typeId, List<Map<String, Object>> moves) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", hp);
        pokemon.put("heldItem", "");
        pokemon.put("ability", "");
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("moves", moves);
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
                "specialDefense", 90,
                "speed", 80
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
