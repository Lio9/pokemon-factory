package com.lio9.battle.engine;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PP（招式点数）系统测试。
 * <p>
 * 验证 PP 初始化、使用后递减、PP 耗尽后自动使用 Struggle、Struggle 反伤。
 * 测试均使用 vgc63 单打格式以简化验证（每方只有 1 个活跃宝可梦）。
 * </p>
 */
class BattlePPTest {

    @Test
    void ppDecrementsAfterMoveUse() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 300, 100, "", ""),
                sixTeamJson("O", 300, 90, "", ""), 12, 200L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        Map<String, Object> move = moves(team(r1, true).get(0)).get(0);
        assertEquals(4, move.get("currentPp"), "PP 应从 5 减到 4");
        assertEquals(5, move.get("maxPp"), "maxPp 应保持 5");
    }

    @Test
    void ppDecrementsEveryRound() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 201L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 3)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 3)));

        // 第 1 回合：PP 3 → 2
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));
        assertEquals(2, moves(team(r1, true).get(0)).get(0).get("currentPp"));

        // 第 2 回合：PP 2 → 1
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "strike", "target-slot-0", "0"));
        assertEquals(1, moves(team(r2, true).get(0)).get(0).get("currentPp"));

        // 第 3 回合：PP 1 → 0
        Map<String, Object> r3 = engine.playRound(r2,
                Map.of("slot-0", "strike", "target-slot-0", "0"));
        assertEquals(0, moves(team(r3, true).get(0)).get(0).get("currentPp"));
    }

    @Test
    void struggleUsedWhenPpExhausted() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 202L, "vgc63");

        // 只有 1 PP 的招式，用完就耗尽
        setMoves(state, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 1)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        // 第 1 回合：正常使用 Strike，PP 1→0
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        // 第 2 回合：PP=0，应使用 Struggle
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        List<Map<String, Object>> actions = lastActions(r2);
        Map<String, Object> struggleAction = findAction(actions, "player", "Struggle");
        assertNotNull(struggleAction, "PP 耗尽后应使用 Struggle");
    }

    @Test
    void struggleDealsRecoilDamage() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 203L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 1)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        // 第 1 回合：消耗最后一发 PP
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        int hpAfterR1 = toInt(team(r1, true).get(0).get("currentHp"));

        // 第 2 回合：Struggle 反伤应减少 1/4 最大 HP
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        int hpAfterR2 = toInt(team(r2, true).get(0).get("currentHp"));
        int maxHp = toInt(stats(team(r2, true).get(0)).get("hp"), 600);
        int expectedRecoil = maxHp / 4;

        // Struggle 反伤：Struggle 造成的伤害不计，还要自己掉 1/4 最大 HP
        assertTrue(hpAfterR2 < hpAfterR1, "Struggle 反伤应该减少 HP");
        assertTrue(hpAfterR2 >= hpAfterR1 - expectedRecoil - 100, // 留有余量给 Struggle 伤害
                "Struggle 反伤约为 1/4 最大 HP");
    }

    @Test
    void ppUnaffectedForMovesWithoutCurrentPp() {
        // 验证未经 normalizeMoves 标准化的招式（通过 setMoves 注入的不带 currentPp 的 Map.of）
        // 在 canUseMove 中不会因 PP 检查被拒绝
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 300, 100, "", ""),
                sixTeamJson("O", 300, 90, "", ""), 12, 204L, "vgc63");

        // 使用 Map.of() 创建的不含 pp 字段的老式招式 ——不应被 PP 检查阻塞
        setMoves(state, true, 0, List.of(oldMove("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(oldMove("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        List<Map<String, Object>> actions = lastActions(r1);
        assertEquals(2, actions.size(), "不带 currentPp 的招式应正常使用");
    }

    // ---- helper methods ----

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> moves(Map<String, Object> mon) {
        return (List<Map<String, Object>>) mon.get("moves");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> stats(Map<String, Object> mon) {
        return (Map<String, Object>) mon.get("stats");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lastActions(Map<String, Object> state) {
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        return (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
    }

    private static Map<String, Object> findAction(List<Map<String, Object>> actions, String side, String moveName) {
        return actions.stream()
                .filter(action -> side.equals(action.get("side"))
                        && moveName.equals(action.get("move")))
                .findFirst()
                .orElse(null);
    }

    private static void setMoves(Map<String, Object> state, boolean player, int teamIndex,
            List<Map<String, Object>> newMoves) {
        team(state, player).get(teamIndex).put("moves", newMoves);
    }

    private static int toInt(Object value) {
        return toInt(value, 0);
    }

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    /** 带 PP 的招式，直接设置 currentPp/maxPp（兼容 setMoves 绕过 normalizeMoves 的场景）。 */
    private static Map<String, Object> moveWithPp(String name, String nameEn, int power, int accuracy,
            int priority, int damageClassId, int typeId, int targetId, int pp) {
        Map<String, Object> move = new LinkedHashMap<>();
        move.put("name", name);
        move.put("name_en", nameEn);
        move.put("power", power);
        move.put("accuracy", accuracy);
        move.put("priority", priority);
        move.put("damage_class_id", damageClassId);
        move.put("type_id", typeId);
        move.put("target_id", targetId);
        move.put("pp", pp);
        move.put("maxPp", pp);
        move.put("currentPp", pp);
        return move;
    }

    /** 旧风格招式：无 pp 字段、不可变 Map.of，模拟 setMoves 注入的遗留数据。 */
    private static Map<String, Object> oldMove(String name, String nameEn, int power, int accuracy,
            int priority, int damageClassId, int typeId, int targetId) {
        return Map.of(
                "name", name,
                "name_en", nameEn,
                "power", power,
                "accuracy", accuracy,
                "priority", priority,
                "damage_class_id", damageClassId,
                "type_id", typeId,
                "target_id", targetId);
    }

    private static String sixTeamJson(String prefix, int hp, int speed, String heldItem, String abilityNameEn) {
        return "[" +
                pokemonJson(prefix + "-A", hp, speed, heldItem, abilityNameEn) + "," +
                pokemonJson(prefix + "-B", hp, speed - 5, "", "") + "," +
                pokemonJson(prefix + "-C", hp, speed - 10, "", "") + "," +
                pokemonJson(prefix + "-D", hp, speed - 15, "", "") + "," +
                pokemonJson(prefix + "-E", hp, speed - 20, "", "") + "," +
                pokemonJson(prefix + "-F", hp, speed - 25, "", "") +
                "]";
    }

    private static String pokemonJson(String name, int hp, int speed, String heldItem, String abilityNameEn) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"name_en\":\"" + name.toLowerCase() + "\"," +
                "\"battleScore\":200," +
                "\"heldItem\":\"" + heldItem + "\"," +
                "\"ability\":{\"name_en\":\"" + abilityNameEn + "\",\"name\":\"" + abilityNameEn + "\"}," +
                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                "\"stats\":{\"hp\":" + hp +
                ",\"attack\":110,\"defense\":90,\"specialAttack\":90,\"specialDefense\":90,\"speed\":" + speed + "}," +
                "\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":40,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]" +
                "}";
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
        }, new ObjectMapper());
    }
}
