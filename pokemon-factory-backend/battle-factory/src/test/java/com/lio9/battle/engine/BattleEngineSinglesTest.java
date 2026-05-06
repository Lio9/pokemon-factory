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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VGC 63 单打（6 选 3，1 个首发）的完整流程测试。
 * <p>
 * 验证 format 参数是否正确传递到引擎各个层面：
 * <ul>
 *   <li>createPreviewState / createBattleState 存储格式参数</li>
 *   <li>autoSelect / normalizeSelection 按 battleTeamSize=3 选人</li>
 *   <li>activeSlotsLimit=1 只激活 1 个槽位</li>
 *   <li>补位流程也按 1 个槽位处理</li>
 *   <li>格式别名也生效</li>
 *   <li>单打中的轮换、胜利条件正常</li>
 * </ul>
 */
class BattleEngineSinglesTest {

    @Test
    void createPreviewState_storesSinglesFormatConfig() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createPreviewState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 42L, "vgc63");

        assertEquals("preview", state.get("status"));
        assertEquals("team-preview", state.get("phase"));
        assertEquals("vgc63", state.get("format"));
        assertEquals(3, state.get("battleTeamSize"));
        assertEquals(1, state.get("activeSlotsLimit"));
        assertEquals(6, ((List<?>) state.get("playerRoster")).size());
    }

    @Test
    void createBattleState_singlesFormat_producesSingleActiveSlot() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 43L, "vgc63");

        assertEquals("running", state.get("status"));
        assertEquals("battle", state.get("phase"));
        assertEquals(3, state.get("battleTeamSize"));
        assertEquals(1, state.get("activeSlotsLimit"));

        // 只有 1 个活跃槽位
        assertEquals(1, ((List<?>) state.get("playerActiveSlots")).size());
        assertEquals(1, ((List<?>) state.get("opponentActiveSlots")).size());

        // 队伍只有 3 只宝可梦（6 选 3）
        assertEquals(3, ((List<?>) state.get("playerTeam")).size());
        assertEquals(3, ((List<?>) state.get("opponentTeam")).size());
    }

    @Test
    void formatAlias_gen9singles_works() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 49L, "gen9singles");

        assertEquals(3, state.get("battleTeamSize"));
        assertEquals(1, state.get("activeSlotsLimit"));
        assertEquals(1, ((List<?>) state.get("playerActiveSlots")).size());
    }

    @Test
    void formatAlias_vgcSingles_works() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 50L, "vgc-singles");

        assertEquals(3, state.get("battleTeamSize"));
        assertEquals(1, state.get("activeSlotsLimit"));
        assertEquals(1, ((List<?>) state.get("playerActiveSlots")).size());
    }

    @Test
    void singlesFormat_applyTeamPreviewSelection_respects3pick1lead() {
        BattleEngine engine = createEngine();
        Map<String, Object> preview = engine.createPreviewState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 44L, "vgc63");

        // 手动选 3 只（索引 0,1,2），首发 1 只（索引 0）
        Map<String, Object> selection = Map.of(
                "pickedRosterIndexes", List.of(0, 1, 2),
                "leadRosterIndexes", List.of(0));
        Map<String, Object> state = engine.applyTeamPreviewSelection(preview, selection, selection);

        assertEquals(3, ((List<?>) state.get("playerTeam")).size());
        assertEquals(1, ((List<?>) state.get("playerActiveSlots")).size());
        assertEquals(3, ((List<?>) state.get("opponentTeam")).size());
        assertEquals(1, ((List<?>) state.get("opponentActiveSlots")).size());
    }

    @Test
    void singlesFormat_playRound_worksWithOneActiveMon() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 45L, "vgc63");

        setMoves(state, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));

        // 单打中双方各 1 个活跃宝可梦，共 2 个 actions
        List<Map<String, Object>> actions = lastActions(updated);
        assertEquals(2, actions.size());
        assertTrue(actions.stream().anyMatch(a -> "player".equals(a.get("side"))));
        assertTrue(actions.stream().anyMatch(a -> "opponent".equals(a.get("side"))));
    }

    @Test
    void singlesFormat_playRound_twoTurns_bothSidesActEachTurn() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 300, 100, "", ""),
                sixTeamJson("O", 300, 90, "", ""), 12, 51L, "vgc63");

        setMoves(state, true, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));

        // 第 1 回合 — playRound 返回新状态
        Map<String, Object> r1 = engine.playRound(state,
                Map.of("slot-0", "strike", "target-slot-0", "0"));
        assertEquals(2, lastActions(r1).size());
        assertEquals(1, toInt(r1.get("currentRound")));

        // 第 2 回合 — 用 r1 继续，引擎自动推进回合
        Map<String, Object> r2 = engine.playRound(r1,
                Map.of("slot-0", "strike", "target-slot-0", "0"));
        assertEquals(2, lastActions(r2).size());
        assertEquals(2, toInt(r2.get("currentRound")));
    }

    @Test
    void singlesFormat_switch_activeMonSwitchesToBench() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 52L, "vgc63");

        // 给对手招式，避免 playRound 报错
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));

        // 当前 active slot 是 0（索引 0），换成板凳上的索引 1
        Map<String, Object> updated = engine.playRound(state, Map.of(
                "action-slot-0", "switch",
                "switch-slot-0", "1"));

        assertEquals("battle", updated.get("phase"));
        assertEquals(List.of(1), updated.get("playerActiveSlots"));
    }

    @Test
    void singlesFormat_replacement_requiresOneReplacement() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 46L, "vgc63");

        // 让首发倒下
        Map<String, Object> mon = team(state, true).get(0);
        mon.put("currentHp", 0);
        mon.put("status", "fainted");
        state.put("playerActiveSlots", List.of());
        state.put("phase", "replacement");
        state.put("playerPendingReplacementCount", 1);
        state.put("playerPendingReplacementOptions", List.of(1, 2));

        Map<String, Object> updated = engine.applyReplacementSelection(state,
                Map.of("replacementIndexes", List.of(1)));

        assertEquals("battle", updated.get("phase"));
        assertEquals(1, ((List<?>) updated.get("playerActiveSlots")).size());
    }

    @Test
    void singlesFormat_noAliveMons_opponentRemainingZero() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 200, 100, "", ""),
                sixTeamJson("O", 200, 90, "", ""), 12, 53L, "vgc63");

        // 让对手全部 3 只倒下
        for (int i = 0; i < 3; i++) {
            team(state, false).get(i).put("currentHp", 0);
            team(state, false).get(i).put("status", "fainted");
        }
        state.put("opponentActiveSlots", List.of());

        // 进入 replacement → 检测到对手无存活 → status 改为 completed
        state.put("phase", "replacement");
        state.put("playerPendingReplacementCount", 0);
        state.put("playerPendingReplacementOptions", List.of());
        state.put("opponentPendingReplacementCount", 0);
        state.put("opponentPendingReplacementOptions", List.of());

        // 调用 applyReplacementSelection 触发 refreshDerivedState
        Map<String, Object> updated = engine.applyReplacementSelection(state,
                Map.of("replacementIndexes", List.of()));

        assertEquals(0, toInt(updated.get("opponentRemaining")));
    }

    @Test
    void singlesFormat_spreadMove_noPenalty() {
        BattleEngine engine = createEngine();
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 200, 100, "", ""),
                sixTeamJson("O", 200, 90, "", ""), 12, 47L, "vgc63");

        // 使用 spread 招式（target_id=13 即所有对手）
        setMoves(state, true, 0, List.of(move("Spread", "spread", 100, 100, 0, 2,
                DamageCalculatorUtil.TYPE_NORMAL, 13)));
        setMoves(state, false, 0, List.of(move("Strike", "strike", 40, 100, 0, 1,
                DamageCalculatorUtil.TYPE_NORMAL, 10)));

        Map<String, Object> updated = engine.playRound(state,
                Map.of("slot-0", "spread", "target-slot-0", "0"));

        // 单打中 spread 目标数=1，不应触发 0.75x 惩罚，招式正常命中
        Map<String, Object> spreadAction = findAction(lastActions(updated), "player", "Spread");
        assertNotNull(spreadAction, "Spread action should exist");
        assertEquals("hit", spreadAction.get("result"));
        // 确认有伤害且不为 0
        int damage = toInt(spreadAction.get("damage"));
        assertTrue(damage > 0, "Spread move should deal damage in singles, got: " + damage);
    }

    @Test
    void factoryFormat_stillDoubles() {
        BattleEngine engine = createEngine();
        // 工厂默认使用 vgc-doubles（不传 format）
        Map<String, Object> state = engine.createBattleState(sixTeamJson("P", 180, 100, "", ""),
                sixTeamJson("O", 180, 90, "", ""), 12, 48L);

        assertEquals(4, state.get("battleTeamSize"));
        assertEquals(2, state.get("activeSlotsLimit"));
        assertEquals(2, ((List<?>) state.get("playerActiveSlots")).size());
        assertEquals(4, ((List<?>) state.get("playerTeam")).size());
    }

    // ---- helper methods ----

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
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
            List<Map<String, Object>> moves) {
        team(state, player).get(teamIndex).put("moves", moves);
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

    private static Map<String, Object> move(String name, String nameEn, int power, int accuracy,
            int priority, int damageClassId, int typeId, int targetId) {
        Map<String, Object> move = new LinkedHashMap<>();
        move.put("name", name);
        move.put("name_en", nameEn);
        move.put("power", power);
        move.put("accuracy", accuracy);
        move.put("priority", priority);
        move.put("damage_class_id", damageClassId);
        move.put("type_id", typeId);
        move.put("target_id", targetId);
        // PP 给个足够大的值避免回合中 PP 耗尽
        move.put("pp", 99);
        return move;
    }

    /** 构造 6 只宝可梦的 JSON 字符串用于单打测试。 */
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
