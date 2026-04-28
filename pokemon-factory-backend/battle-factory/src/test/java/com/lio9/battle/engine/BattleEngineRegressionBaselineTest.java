package com.lio9.battle.engine;

/**
 * BattleEngineRegressionBaselineTest 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战引擎文件。
 * 核心职责：负责 BattleEngineRegressionBaselineTest 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责。
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEngineRegressionBaselineTest {

        /**
         * 这组测试不是做全量对战验证，而是针对“近期容易回归的关键机制”建立稳定基线：
         * <ul>
         * <li>天气/特性/道具对命中率的修正</li>
         * <li>行动顺序层中的抢先/延后来源</li>
         * <li>volatile 状态镜像是否稳定</li>
         * <li>固定 seed 下多回合日志是否可复现</li>
         * </ul>
         */

        @Test
        void playRound_thunderInRainUsesWeatherAccuracyRule() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(singleTeamJson("Rain-A", 180, 80, "", ""),
                                singleTeamJson("Target-O", 180, 100, "", ""), 8, 11L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Thunder", "thunder", 110, 50, 0, 2, DamageCalculatorUtil.TYPE_ELECTRIC,
                                                10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 20, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setWeatherTurns(state, "rainTurns", 5);

                Map<String, Object> updated = engine.playRound(state,
                                Map.of("slot-0", "thunder", "target-slot-0", "0"));
                Map<String, Object> thunder = findAction(lastActions(updated), "player", "Thunder");
                assertEquals("hit", thunder.get("result"));
        }

        @Test
        void playRound_noGuardIgnoresLowAccuracy() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(
                                singleTeamJson("NoGuard-A", 180, 80, "", "no-guard"),
                                singleTeamJson("Target-O", 180, 100, "", ""), 8, 12L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Wild Swing", "wild-swing", 70, 1, 0, 1, DamageCalculatorUtil.TYPE_NORMAL,
                                                10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 20, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state,
                                Map.of("slot-0", "wild-swing", "target-slot-0", "0"));
                Map<String, Object> hit = findAction(lastActions(updated), "player", "Wild Swing");
                assertEquals("hit", hit.get("result"));
        }

        @Test
        void playRound_quickClawCanOverrideActionOrder() {
                BattleEngine engine = createEngine();
                boolean triggered = false;
                for (long seed = 1; seed <= 300; seed++) {
                        // 这里不把触发概率写死成单一 seed，而是在有限样本内确认“存在可触发路径”。
                        Map<String, Object> state = engine.createBattleState(
                                        singleTeamJson("Claw-A", 180, 50, "quick-claw", ""),
                                        singleTeamJson("Fast-O", 180, 140, "", ""), 8, seed);
                        keepSinglesOnly(state);
                        setMoves(state, true, 0,
                                        List.of(move("Strike", "strike", 40, 100, 0, 1,
                                                        DamageCalculatorUtil.TYPE_NORMAL, 10)));
                        setMoves(state, false, 0,
                                        List.of(move("Strike", "strike", 40, 100, 0, 1,
                                                        DamageCalculatorUtil.TYPE_NORMAL, 10)));
                        Map<String, Object> updated = engine.playRound(state,
                                        Map.of("slot-0", "strike", "target-slot-0", "0"));
                        List<Map<String, Object>> actions = lastActions(updated);
                        if (actions.isEmpty()) {
                                continue;
                        }
                        Map<String, Object> first = actions.get(0);
                        if ("player".equals(first.get("side")) && "quick-claw".equals(first.get("orderSource"))) {
                                triggered = true;
                                break;
                        }
                }
                assertTrue(triggered);
        }

        @Test
        void playRound_custapBerryBoostsOrderAndIsConsumed() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(
                                singleTeamJson("Custap-A", 200, 60, "custap-berry", ""),
                                singleTeamJson("Fast-O", 200, 120, "", ""), 8, 21L);
                keepSinglesOnly(state);
                setCurrentHp(state, true, 0, 40);
                setMoves(state, true, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
                List<Map<String, Object>> actions = lastActions(updated);
                assertEquals("player", actions.get(0).get("side"));
                assertEquals("custap-berry", actions.get(0).get("orderSource"));
                assertTrue(Boolean.TRUE.equals(team(updated, true).get(0).get("itemConsumed")));
        }

        @Test
        void playRound_quickDrawCanOverrideActionOrder() {
                BattleEngine engine = createEngine();
                boolean triggered = false;
                for (long seed = 1; seed <= 300; seed++) {
                        Map<String, Object> state = engine.createBattleState(
                                        singleTeamJson("QuickDraw-A", 180, 50, "", "quick-draw"),
                                        singleTeamJson("Fast-O", 180, 140, "", ""), 8, seed);
                        keepSinglesOnly(state);
                        setMoves(state, true, 0,
                                        List.of(move("Strike", "strike", 40, 100, 0, 1,
                                                        DamageCalculatorUtil.TYPE_NORMAL, 10)));
                        setMoves(state, false, 0,
                                        List.of(move("Strike", "strike", 40, 100, 0, 1,
                                                        DamageCalculatorUtil.TYPE_NORMAL, 10)));
                        Map<String, Object> updated = engine.playRound(state,
                                        Map.of("slot-0", "strike", "target-slot-0", "0"));
                        List<Map<String, Object>> actions = lastActions(updated);
                        if (actions.isEmpty()) {
                                continue;
                        }
                        Map<String, Object> first = actions.get(0);
                        if ("player".equals(first.get("side")) && "quick-draw".equals(first.get("orderSource"))) {
                                triggered = true;
                                break;
                        }
                }
                assertTrue(triggered);
        }

        @Test
        void playRound_stallDelaysActionWithinPriorityBracket() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(singleTeamJson("Stall-A", 180, 140, "", "stall"),
                                singleTeamJson("Mid-O", 180, 100, "", ""), 8, 18L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
                List<Map<String, Object>> actions = lastActions(updated);
                assertEquals("opponent", actions.get(0).get("side"));
                assertEquals("stall", actions.get(1).get("orderSource"));
        }

        @Test
        void playRound_laggingTailDelaysActionWithinPriorityBracket() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(
                                singleTeamJson("Lagging-A", 180, 140, "lagging-tail", ""),
                                singleTeamJson("Mid-O", 180, 100, "", ""), 8, 19L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
                List<Map<String, Object>> actions = lastActions(updated);
                assertEquals("opponent", actions.get(0).get("side"));
                assertEquals("lagging-tail", actions.get(1).get("orderSource"));
        }

        @Test
        void playRound_fullIncenseDelaysActionWithinPriorityBracket() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(
                                singleTeamJson("Incense-A", 180, 140, "full-incense", ""),
                                singleTeamJson("Mid-O", 180, 100, "", ""), 8, 20L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
                List<Map<String, Object>> actions = lastActions(updated);
                assertEquals("opponent", actions.get(0).get("side"));
                assertEquals("full-incense", actions.get(1).get("orderSource"));
        }

        @Test
        void playRound_fixedSeedMultiRoundSnapshot_matchesGoldenBaseline() {
                BattleEngine engine = createEngine();
                String snapshotA = runFixedSeedSnapshot(engine);
                String snapshotB = runFixedSeedSnapshot(engine);

                // 固定 seed 下两次完整回放必须得到同一份日志快照，作为 golden baseline 的稳定性约束。
                assertEquals(snapshotA, snapshotB);
                assertTrue(snapshotA.contains("round=1"));
                assertTrue(snapshotA.contains("round=2"));
                assertTrue(snapshotA.contains("round=3"));
                assertTrue(snapshotA.contains("stall") || snapshotA.contains("lagging-tail"));
        }

        @Test
        void playRound_confusionIsMirroredToVolatiles() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(singleTeamJson("Confuser-A", 180, 100, "", ""),
                                singleTeamJson("Target-O", 180, 80, "", ""), 8, 31L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Confuse Ray", "confuse-ray", 0, 100, 0, 3,
                                                DamageCalculatorUtil.TYPE_GHOST, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state,
                                Map.of("slot-0", "confuse-ray", "target-slot-0", "0"));
                Map<String, Object> target = team(updated, false).get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> volatiles = (Map<String, Object>) target.get("volatiles");
                assertTrue(Boolean.TRUE.equals(target.get("confused")));
                assertTrue(Boolean.TRUE.equals(volatiles.get("confused")));
                assertEquals(target.get("confusionTurns"), volatiles.get("confusionTurns"));
        }

        @Test
        void playRound_yawnUsesVolatileBackfillAfterEndTurnTick() {
                BattleEngine engine = createEngine();
                Map<String, Object> state = engine.createBattleState(singleTeamJson("Yawn-A", 180, 100, "", ""),
                                singleTeamJson("Target-O", 180, 80, "", ""), 8, 32L);
                keepSinglesOnly(state);
                setMoves(state, true, 0,
                                List.of(move("Yawn", "yawn", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                Map<String, Object> updated = engine.playRound(state, Map.of("slot-0", "yawn", "target-slot-0", "0"));
                Map<String, Object> target = team(updated, false).get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> volatiles = (Map<String, Object>) target.get("volatiles");
                assertEquals(1, target.get("yawnTurns"));
                assertEquals(1, volatiles.get("yawnTurns"));
        }

        @Test
        void specialSystem_mutualExclusion_isEnforced() {
                BattleEngine engine = createEngine();
                // 构造一个同时具备 Mega 和 Z 招式资格的宝可梦
                String teamJson = "[{\"name\":\"MegaZ\",\"name_en\":\"megaz\",\"heldItem\":\"z-crystal\",\"megaEligible\":true,\"zMoveEligible\":true,\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]}]";
                Map<String, Object> state = engine.createBattleState(teamJson, teamJson, 8, 99L);
                keepSinglesOnly(state);

                // 激活 Mega 进化
                Map<String, Object> mon = team(state, true).get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> moves = (List<Map<String, Object>>) mon.get("moves");
                engine.activateSpecialSystem(state, true, mon,
                                moves.isEmpty() ? null : moves.get(0),
                                "mega", 1, new java.util.LinkedHashMap<>(), new java.util.ArrayList<>());

                assertTrue(Boolean.TRUE.equals(state.get("playerSpecialUsed")));
                // 尝试使用 Z 招式应被拒绝
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> movesForZCheck = (List<Map<String, Object>>) mon.get("moves");
                assertFalse(engine.canUseSpecialSystem(state, true, mon, "z-move",
                                movesForZCheck.isEmpty() ? null : movesForZCheck.get(0)));
        }

        @Test
        void dynamax_lifecycle_cannotSwitch() {
                BattleEngine engine = createEngine();
                // 构造三只宝可梦的队伍：前两只在场上（双打），第三只在替补席
                String teamJson = "[{\"name\":\"Max\",\"name_en\":\"max\",\"dynamaxEligible\":true,\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]},{\"name\":\"Ally\",\"name_en\":\"ally\",\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]},{\"name\":\"Bench\",\"name_en\":\"bench\",\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]}]";
                Map<String, Object> state = engine.createBattleState(teamJson, teamJson, 8, 99L);

                // 激活第一只宝可梦的极巨化状态
                Map<String, Object> mon = team(state, true).get(0);
                mon.put("dynamaxed", true);
                mon.put("dynamaxTurnsRemaining", 3);

                // 确保 activeSlots 里有两只宝可梦（双打）
                state.put("playerActiveSlots", List.of(0, 1));

                // 手动让第二只（队友）倒下，触发 replacement 阶段的需求
                team(state, true).get(1).put("currentHp", 0);

                // 先修剪 activeSlots，移除倒下的宝可梦
                state.put("playerActiveSlots", List.of(0)); // 只剩下极巨化的那只

                // 手动设置 phase 为 replacement，模拟战斗中有宝可梦倒下后的状态
                state.put("phase", "replacement");
                state.put("playerPendingReplacementCount", 1);
                state.put("playerPendingReplacementOptions", List.of(2));

                // 此时场上有一只极巨化的宝可梦，尝试轮换应抛出异常
                try {
                        engine.applyReplacementSelection(state, Map.of("replacementIndexes", List.of(2)));
                        throw new AssertionError("Expected IllegalArgumentException for switching while dynamaxed");
                } catch (IllegalArgumentException e) {
                        if (!e.getMessage().contains("dynamaxed")) {
                                throw new AssertionError(
                                                "Expected exception message to contain 'dynamaxed', but got: "
                                                                + e.getMessage());
                        }
                }
        }

        @Test
        void mega_and_tera_states_persist_after_switch() {
                BattleEngine engine = createEngine();
                // 构造三只宝可梦的队伍
                String teamJson = "[{\"name\":\"MegaTera\",\"name_en\":\"megatera\",\"megaEligible\":true,\"teraTypeId\":2,\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]},{\"name\":\"Ally\",\"name_en\":\"ally\",\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]},{\"name\":\"Bench\",\"name_en\":\"bench\",\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]}]";
                Map<String, Object> state = engine.createBattleState(teamJson, teamJson, 8, 99L);

                Map<String, Object> mon = team(state, true).get(0);

                // 激活 Mega 进化
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> movesForMega = (List<Map<String, Object>>) mon.get("moves");
                engine.activateSpecialSystem(state, true, mon, movesForMega.isEmpty() ? null : movesForMega.get(0),
                                "mega", 1, new java.util.LinkedHashMap<>(), new java.util.ArrayList<>());
                assertTrue(Boolean.TRUE.equals(mon.get("megaEvolved")), "Mega evolution should be activated");

                // 激活太晶化
                engine.activateSpecialSystem(state, true, mon, null, "tera", 1, new java.util.LinkedHashMap<>(),
                                new java.util.ArrayList<>());
                assertTrue(Boolean.TRUE.equals(mon.get("terastallized")), "Terastallization should be activated");

                // 模拟轮换：让第一只倒下，换上第三只，然后再换回第一只
                team(state, true).get(0).put("currentHp", 0);
                state.put("playerActiveSlots", List.of(1)); // 只剩第二只

                // 进入 replacement 阶段
                state.put("phase", "replacement");
                state.put("playerPendingReplacementCount", 1);
                state.put("playerPendingReplacementOptions", List.of(2));

                // 换上第三只
                state = engine.applyReplacementSelection(state, Map.of("replacementIndexes", List.of(2)));
                assertEquals("battle", state.get("phase"), "Should return to battle phase");

                // 现在让第二只倒下，准备换回第一只（它应该仍然保持 Mega 和 Tera 状态）
                team(state, true).get(1).put("currentHp", 0);
                state.put("playerActiveSlots", List.of(2));
                state.put("phase", "replacement");
                state.put("playerPendingReplacementCount", 1);
                state.put("playerPendingReplacementOptions", List.of(0));

                // 换回第一只
                state = engine.applyReplacementSelection(state, Map.of("replacementIndexes", List.of(0)));

                // 验证 Mega 和 Tera 状态仍然保留
                Map<String, Object> switchedBackMon = team(state, true).get(0);
                assertTrue(Boolean.TRUE.equals(switchedBackMon.get("megaEvolved")),
                                "Mega evolution should persist after switch out");
                assertTrue(Boolean.TRUE.equals(switchedBackMon.get("terastallized")),
                                "Terastallization should persist after switch out");
        }

        @Test
        void z_move_is_consumed_after_use() {
                BattleEngine engine = createEngine();
                // 构造具备 Z 招式资格的宝可梦
                String teamJson = "[{\"name\":\"ZUser\",\"name_en\":\"zuser\",\"zMoveEligible\":true,\"heldItem\":\"normalium-z\",\"stats\":{\"hp\":200,\"attack\":100,\"defense\":100,\"specialAttack\":100,\"specialDefense\":100,\"speed\":100},\"types\":[{\"type_id\":1,\"name\":\"Normal\"}],\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":100,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1}]}]";
                Map<String, Object> state = engine.createBattleState(teamJson, teamJson, 8, 99L);
                keepSinglesOnly(state);

                Map<String, Object> mon = team(state, true).get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> movesForZ = (List<Map<String, Object>>) mon.get("moves");
                Map<String, Object> move = movesForZ.isEmpty() ? null : movesForZ.get(0);

                // 验证初始状态下可以使用 Z 招式
                assertTrue(engine.canUseSpecialSystem(state, true, mon, "z-move", move),
                                "Should be able to use Z-move initially");

                // 激活 Z 招式
                engine.activateSpecialSystem(state, true, mon, move, "z-move", 1, new java.util.LinkedHashMap<>(),
                                new java.util.ArrayList<>());

                // 验证 Z 招式已被标记为已使用
                assertTrue(Boolean.TRUE.equals(mon.get("zMoveUsed")), "Z-move should be marked as used");

                // 验证之后不能再使用 Z 招式
                assertFalse(engine.canUseSpecialSystem(state, true, mon, "z-move", move),
                                "Should not be able to use Z-move again after consumption");
        }

        @SuppressWarnings("unchecked")
        private static List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
                return (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        }

        private static void keepSinglesOnly(Map<String, Object> state) {
                state.put("playerActiveSlots", List.of(0));
                state.put("opponentActiveSlots", List.of(0));
        }

        private static void setCurrentHp(Map<String, Object> state, boolean player, int teamIndex, int hp) {
                team(state, player).get(teamIndex).put("currentHp", hp);
        }

        private static void setWeatherTurns(Map<String, Object> state, String weatherKey, int turns) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldEffects = (Map<String, Object>) state.get("fieldEffects");
                fieldEffects.put(weatherKey, turns);
        }

        private static void setMoves(Map<String, Object> state, boolean player, int teamIndex,
                        List<Map<String, Object>> moves) {
                team(state, player).get(teamIndex).put("moves", moves);
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
                                .orElseThrow();
        }

        @SuppressWarnings("unchecked")
        private static String snapshotTail(Map<String, Object> state, int roundsCount) {
                List<Map<String, Object>> rounds = (List<Map<String, Object>>) state.get("rounds");
                int from = Math.max(0, rounds.size() - roundsCount);
                StringBuilder builder = new StringBuilder();
                for (int index = from; index < rounds.size(); index++) {
                        Map<String, Object> round = rounds.get(index);
                        List<Map<String, Object>> actions = (List<Map<String, Object>>) round.get("actions");
                        // 这里故意把 action 压平成紧凑字符串，降低 Map 字段顺序变化对 baseline 的干扰。
                        List<String> compactActions = actions.stream()
                                        .map(action -> action.get("side") + ":"
                                                        + String.valueOf(action.getOrDefault("move",
                                                                        action.getOrDefault("actionType", "")))
                                                        + ":"
                                                        + action.get("result") + ":"
                                                        + String.valueOf(action.getOrDefault("damage", "null")) + ":"
                                                        + String.valueOf(action.getOrDefault("orderSource", "null")))
                                        .toList();
                        // round 维度同时保留 events 与 actions，便于人工比对“为什么顺序变化”和“结算结果是否变化”。
                        builder.append("round=").append(round.get("round"))
                                        .append("|events=").append(round.get("events"))
                                        .append("|actions=").append(compactActions)
                                        .append(System.lineSeparator());
                }
                return builder.toString().trim();
        }

        private static String runFixedSeedSnapshot(BattleEngine engine) {
                Map<String, Object> state = engine.createBattleState(singleTeamJson("Snapshot-A", 200, 95, "", "stall"),
                                singleTeamJson("Snapshot-O", 200, 100, "lagging-tail", ""), 8, 233L);
                // 不再强制单槽，让引擎在双打环境下自然运行
                setMoves(state, true, 0, List.of(
                                move("Taunt", "taunt", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_DARK, 10),
                                move("Disable", "disable", 0, 100, 0, 3, DamageCalculatorUtil.TYPE_NORMAL, 10),
                                move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));
                setMoves(state, false, 0,
                                List.of(move("Strike", "strike", 40, 100, 0, 1, DamageCalculatorUtil.TYPE_NORMAL, 10)));

                state = engine.playRound(state, Map.of("slot-0", "taunt", "target-slot-0", "0"));
                state = engine.playRound(state, Map.of("slot-0", "disable", "target-slot-0", "0"));
                state = engine.playRound(state, Map.of("slot-0", "strike", "target-slot-0", "0"));
                return snapshotTail(state, 3);
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
                // Ensure target_id 10 is treated as single target for baseline tests unless
                // specified otherwise
                move.put("target_id", targetId == 10 ? 1 : targetId);
                return move;
        }

        private static String singleTeamJson(String name, int hp, int speed, String heldItem, String abilityNameEn) {
                return "[" + pokemonJson(name, hp, speed, heldItem, abilityNameEn) + ","
                                + pokemonJson(name + "-B", hp, speed - 10, "", "") + ","
                                + pokemonJson(name + "-C", hp, speed - 20, "", "") + ","
                                + pokemonJson(name + "-D", hp, speed - 30, "", "") + "]";
        }

        private static String pokemonJson(String name, int hp, int speed, String heldItem, String abilityNameEn) {
                return "{" +
                                "\"name\":\"" + name + "\"," +
                                "\"name_en\":\"" + name.toLowerCase() + "\"," +
                                "\"battleScore\":200," +
                                "\"heldItem\":\"" + heldItem + "\"," +
                                "\"ability\":{\"name_en\":\"" + abilityNameEn + "\",\"name\":\"" + abilityNameEn
                                + "\"}," +
                                "\"types\":[{\"type_id\":1,\"name\":\"Normal\"}]," +
                                "\"stats\":{\"hp\":" + hp
                                + ",\"attack\":110,\"defense\":90,\"specialAttack\":90,\"specialDefense\":90,\"speed\":"
                                + speed + "},"
                                +
                                "\"moves\":[{\"name\":\"Strike\",\"name_en\":\"strike\",\"power\":40,\"accuracy\":100,\"priority\":0,\"damage_class_id\":1,\"type_id\":1,\"target_id\":10}]"
                                +
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
