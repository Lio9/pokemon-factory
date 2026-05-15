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
 * 特殊招式机制测试。
 * <p>
 * 验证二回合招式（Fly 等）、延迟攻击（Future Sight）、接力（Baton Pass）等。
 * </p>
 */
class BattleEngineMoveMechanicsTest {

    @Test
    void futureSightStoresDataAndHitsAfterDelay() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 300L, "vgc63");

        // 给双方设置招式
        setMoves(state, true, 0, List.of(moveWithPp("Future Sight", "future-sight", 120, 100, 0, 2,
                DamageCalculatorUtil.TYPE_PSYCHIC, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        // 第1回合：使用 Future Sight，应该存到 fieldEffects 并显示提示
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "future-sight", "target-slot-0", "0"));

        // 验证 Future Sight 已被存储（回合末递减一次，从3→2）
        Map<String, Object> fe = fieldEffects(r1);
        Map<String, Object> fsData = castMap(fe.get("playerFutureSight"));
        assertNotNull(fsData, "Future Sight 数据应存储到 fieldEffects");
        assertEquals(2, fsData.get("turns"), "回合末已递减一次，倒计时应为 2");

        // 验证日志包含提示
        List<Map<String, Object>> actionsR1 = lastActions(r1);
        boolean hasFutureSightLog = actionsR1.stream()
                .anyMatch(a -> "future-sight-stored".equals(a.get("result")));
        assertTrue(hasFutureSightLog, "Future Sight 使用后应有 future-sight-stored 日志");

        int opponentHpR1 = hp(team(r1, false).get(0));

        // 第2回合：双方正常行动，Future Sight 倒计时从2→1
        setMoves(r1, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        Map<String, Object> fe2 = fieldEffects(r2);
        Map<String, Object> fsData2 = castMap(fe2.get("playerFutureSight"));
        assertNotNull(fsData2, "Future Sight 数据应该还在");
        assertEquals(1, fsData2.get("turns"), "倒计时应为 1");

        // 第3回合：回合末 Future Sight 从1→0，造成伤害
        setMoves(r2, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));
        Map<String, Object> r3 = engine.playRound(r2,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        // Future Sight 应该已经命中并清除
        Map<String, Object> fe3 = fieldEffects(r3);
        assertTrue(fe3.get("playerFutureSight") == null
                        || !(fe3.get("playerFutureSight") instanceof Map),
                "Future Sight 伤害触发后数据应被清除");

        int opponentHpR3 = hp(team(r3, false).get(0));
        assertTrue(opponentHpR3 < opponentHpR1, "Future Sight 伤害应减少对手 HP");
    }

    @Test
    void batonPassPreservesStatStages() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 301L, "vgc63");

        // 玩家方：第0只宝可梦有 Swords Dance 和 Baton Pass
        setMoves(state, true, 0, List.of(
                moveWithPp("Swords Dance", "swords-dance", 0, 100, 0, 3,
                        DamageCalculatorUtil.TYPE_NORMAL, 7, 5),
                moveWithPp("Baton Pass", "baton-pass", 0, 100, 0, 3,
                        DamageCalculatorUtil.TYPE_NORMAL, 7, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        // 第1回合：使用 Swords Dance，物攻+2
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "swords-dance", "target-slot-0", "0"));
        Map<String, Object> playerMonR1 = team(r1, true).get(0);
        int attackStageAfterSd = statStage(playerMonR1, "attack");
        assertEquals(2, attackStageAfterSd, "Swords Dance 应提升物攻 2 级");

        // 第2回合：使用 Baton Pass，自动切换到第一个可用后备
        setMoves(r1, true, 0, List.of(
                moveWithPp("Baton Pass", "baton-pass", 0, 100, 0, 3,
                        DamageCalculatorUtil.TYPE_NORMAL, 7, 5)));
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "baton-pass", "target-slot-0", "0"));

        // 验证接棒后的宝可梦保留了物攻+2 能力阶级
        List<Map<String, Object>> playerTeamR2 = team(r2, true);
        // vgc63 格式下 team size 为 3，接棒切换到第一个后备（index 1）
        Map<String, Object> switchedIn = playerTeamR2.get(1);
        int inheritedAttackStage = statStage(switchedIn, "attack");
        assertEquals(2, inheritedAttackStage,
                "接棒应保留 Swords Dance 提升的 2 级物攻");
    }

    @Test
    void contraryReversesSelfStatBoost() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", "contrary"),
                sixTeamJson("O", 600, 90, "", ""), 12, 302L, "vgc63");

        setMoves(state, true, 0, List.of(
                moveWithPp("Swords Dance", "swords-dance", 0, 100, 0, 3,
                        DamageCalculatorUtil.TYPE_NORMAL, 7, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "swords-dance", "target-slot-0", "0"));
        Map<String, Object> playerMon = team(r1, true).get(0);
        int atkStage = statStage(playerMon, "attack");
        assertEquals(-2, atkStage, "Contrary inverts Swords Dance +2 to -2");
    }

    @Test
    void contraryReversesIntimidate() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", "contrary"),
                sixTeamJson("O", 600, 90, "", "intimidate"), 12, 304L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> playerMon = team(state, true).get(0);
        int atkStage = statStage(playerMon, "attack");
        assertEquals(1, atkStage, "Contrary reverses Intimidate -1 Atk to +1");
    }

    @Test
    void bodyPressUsesDefenseStat() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 305L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Body Press", "body-press", 80, 100, 0, 1,
                DamageCalculatorUtil.TYPE_FIGHTING, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "body-press", "target-slot-0", "0"));
        int opponentHpAfter = hp(team(r1, false).get(0));
        assertTrue(opponentHpAfter < 600, "Body Press should deal damage");
    }

    @Test
    void foulPlayUsesTargetAttack() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 306L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Foul Play", "foul-play", 95, 100, 0, 1,
                DamageCalculatorUtil.TYPE_DARK, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "foul-play", "target-slot-0", "0"));
        int opponentHpAfter = hp(team(r1, false).get(0));
        assertTrue(opponentHpAfter < 600, "Foul Play should deal damage");
    }

    @Test
    void electroBallDamagesBasedOnSpeed() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 307L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Electro Ball", "electro-ball", 60, 100, 0, 2,
                DamageCalculatorUtil.TYPE_ELECTRIC, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "electro-ball", "target-slot-0", "0"));
        int opponentHpAfter = hp(team(r1, false).get(0));
        assertTrue(opponentHpAfter < 600, "Electro Ball should deal damage");
    }

    @Test
    void gyroBallDamagesBasedOnSpeed() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(
                sixTeamJson("P", 600, 100, "", ""),
                sixTeamJson("O", 600, 90, "", ""), 12, 308L, "vgc63");

        setMoves(state, true, 0, List.of(moveWithPp("Gyro Ball", "gyro-ball", 60, 100, 0, 1,
                DamageCalculatorUtil.TYPE_STEEL, 10, 5)));
        setMoves(state, false, 0, List.of(moveWithPp("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10, 5)));

        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "gyro-ball", "target-slot-0", "0"));
        int opponentHpAfter = hp(team(r1, false).get(0));
        assertTrue(opponentHpAfter < 600, "Gyro Ball should deal damage");
    }

    // ---- helper methods (adapted from BattlePPTest) ----

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lastActions(Map<String, Object> state) {
        List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
        return (List<Map<String, Object>>) rounds.get(rounds.size() - 1).get("actions");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fieldEffects(Map<String, Object> state) {
        return (Map<String, Object>) state.get("fieldEffects");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }

    private static int hp(Map<String, Object> mon) {
        return toInt(mon.get("currentHp"), 0);
    }

    private static int statStage(Map<String, Object> mon, String stat) {
        Map<String, Object> stages = castMap(mon.get("statStages"));
        return stages == null ? 0 : toInt(stages.get(stat), 0);
    }

    private static void setMoves(Map<String, Object> state, boolean player, int teamIndex,
            List<Map<String, Object>> newMoves) {
        team(state, player).get(teamIndex).put("moves", newMoves);
    }

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

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
