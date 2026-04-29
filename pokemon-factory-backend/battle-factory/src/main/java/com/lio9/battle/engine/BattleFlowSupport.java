package com.lio9.battle.engine;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BattleFlowSupport {
    /**
     * 战斗流程状态维护支持类。
     * <p>
     * 这里不直接处理招式结算，而是负责：
     * 胜负判定、活动位修剪、补位阶段切换、派出日志补充，以及回合后派生状态刷新。
     * 对双打来说，这一层可以保证“有人倒下后要不要立刻补位、是否进入 replacement phase”保持统一。
     * </p>
     */
    private final BattleEngine engine;
    private final BattleConditionSupport conditionSupport;
    private final int activeSlotsLimit;

    BattleFlowSupport(BattleEngine engine, BattleConditionSupport conditionSupport, int activeSlotsLimit) {
        this.engine = engine;
        this.conditionSupport = conditionSupport;
        this.activeSlotsLimit = activeSlotsLimit;
    }

    void resolveBattleResult(Map<String, Object> state) {
        pruneActiveSlots(state);
        boolean playerAlive = hasAvailableMon(engine.team(state, true));
        boolean opponentAlive = hasAvailableMon(engine.team(state, false));
        int currentRound = engine.toInt(state.get("currentRound"), 0);
        int roundLimit = engine.toInt(state.get("roundLimit"), 12);
        boolean roundLimitReached = currentRound >= roundLimit;

        // 极端情况处理：如果双方都无存活宝可梦，或者达到回合上限
        if (!playerAlive || !opponentAlive || roundLimitReached) {
            state.put("status", "completed");
            state.put("phase", "completed");

            String winner;
            if (!playerAlive && opponentAlive) {
                winner = "opponent";
            } else if (!opponentAlive && playerAlive) {
                winner = "player";
            } else {
                // 双方同时倒下或平局判定：比较剩余总 HP
                int playerHp = totalRemainingHp(engine.team(state, true));
                int opponentHp = totalRemainingHp(engine.team(state, false));
                winner = (playerHp >= opponentHp) ? "player" : "opponent";
            }

            state.put("winner", winner);
            state.put("exchangeAvailable", "player".equals(winner) && !Boolean.TRUE.equals(state.get("exchangeUsed")));
        }
    }

    void refreshDerivedState(Map<String, Object> state) {
        state.put("playerRemaining", aliveCount(engine.team(state, true)));
        state.put("opponentRemaining", aliveCount(engine.team(state, false)));
        state.put("playerStrength", totalRemainingHp(engine.team(state, true)));
        state.put("opponentStrength", totalRemainingHp(engine.team(state, false)));
    }

    void pruneActiveSlots(Map<String, Object> state) {
        state.put("playerActiveSlots", pruneSideActiveSlots(engine.team(state, true), engine.activeSlots(state, true)));
        state.put("opponentActiveSlots", pruneSideActiveSlots(engine.team(state, false), engine.activeSlots(state, false)));
    }

    void prepareReplacementPhase(Map<String, Object> state, List<String> events) {
        pruneActiveSlots(state);
        // 对手补位可以自动完成；玩家侧如果仍缺活动位，则进入 replacement 阶段等待选择。
        autoFillSideActiveSlotsWithEvents(state, false, events);
        int playerReplacementCount = replacementNeededCount(state, true);
        if (playerReplacementCount > 0) {
            state.put("phase", "replacement");
            state.put("playerPendingReplacementCount", playerReplacementCount);
            state.put("playerPendingReplacementOptions", availableBenchIndexes(state, true));
            events.add("我方有宝可梦倒下，请选择替补上场。");
            return;
        }
        clearReplacementState(state);
        state.put("phase", "battle");
    }

    List<Integer> availableBenchIndexes(Map<String, Object> state, boolean player) {
        return availableBenchIndexes(engine.team(state, player), engine.activeSlots(state, player));
    }

    int replacementNeededCount(Map<String, Object> state, boolean player) {
        List<Map<String, Object>> sideTeam = engine.team(state, player);
        int targetActiveCount = Math.min(activeSlotsLimit, aliveCount(sideTeam));
        return Math.max(0, targetActiveCount - engine.activeSlots(state, player).size());
    }

    List<Integer> autoReplacementIndexes(Map<String, Object> state, boolean player) {
        int needed = replacementNeededCount(state, player);
        List<Integer> available = availableBenchIndexes(state, player);
        return new ArrayList<>(available.subList(0, Math.min(needed, available.size())));
    }

    void clearReplacementState(Map<String, Object> state) {
        state.put("playerPendingReplacementCount", 0);
        state.put("playerPendingReplacementOptions", new ArrayList<>());
    }

    List<String> activeNames(Map<String, Object> state, boolean player) {
        List<String> names = new ArrayList<>();
        List<Map<String, Object>> team = engine.team(state, player);
        for (Integer slot : engine.activeSlots(state, player)) {
            if (slot != null && slot >= 0 && slot < team.size()) {
                names.add(String.valueOf(team.get(slot).get("name")));
            }
        }
        return names;
    }

    void appendSendOutEvents(Map<String, Object> state, boolean player, List<Integer> previousSlots, List<String> events) {
        List<Integer> currentSlots = engine.activeSlots(state, player);
        List<Map<String, Object>> sideTeam = engine.team(state, player);
        for (Integer slot : currentSlots) {
            if (!previousSlots.contains(slot) && slot != null && slot >= 0 && slot < sideTeam.size()) {
                events.add(engine.sideName(player ? "player" : "opponent") + " 派出了 " + sideTeam.get(slot).get("name"));
            }
        }
    }

    private void autoFillSideActiveSlotsWithEvents(Map<String, Object> state, boolean player, List<String> events) {
        List<Integer> previousSlots = new ArrayList<>(engine.activeSlots(state, player));
        List<Integer> refreshed = fillSideActiveSlots(engine.team(state, player), previousSlots);
        state.put(player ? "playerActiveSlots" : "opponentActiveSlots", refreshed);
        for (Integer slot : refreshed) {
            if (slot != null && !previousSlots.contains(slot) && slot >= 0 && slot < engine.team(state, player).size()) {
                Map<String, Object> switchedIn = engine.team(state, player).get(slot);
                // 自动补位也要补齐入场轮次和 flinch 清理，否则会污染下一回合行为。
                switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
                engine.setVolatile(switchedIn, "flinch", false);
            }
        }
        appendSendOutEvents(state, player, previousSlots, events);
        conditionSupport.applyEntryAbilities(state, player, previousSlots, events);
    }

    private List<Integer> pruneSideActiveSlots(List<Map<String, Object>> team, List<Integer> currentSlots) {
        List<Integer> refreshed = new ArrayList<>();
        for (Integer slot : currentSlots) {
            if (slot != null && engine.isAvailableMon(team, slot) && !refreshed.contains(slot)) {
                refreshed.add(slot);
            }
        }
        return refreshed;
    }

    private List<Integer> fillSideActiveSlots(List<Map<String, Object>> team, List<Integer> currentSlots) {
        List<Integer> refreshed = pruneSideActiveSlots(team, currentSlots);
        for (int index = 0; index < team.size() && refreshed.size() < activeSlotsLimit; index++) {
            if (engine.isAvailableMon(team, index) && !refreshed.contains(index)) {
                refreshed.add(index);
            }
        }
        return refreshed;
    }

    private boolean hasAvailableMon(List<Map<String, Object>> team) {
        return aliveCount(team) > 0;
    }

    private int aliveCount(List<Map<String, Object>> team) {
        int count = 0;
        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) > 0) {
                count++;
            }
        }
        return count;
    }

    private int totalRemainingHp(List<Map<String, Object>> team) {
        int total = 0;
        for (Map<String, Object> mon : team) {
            total += Math.max(0, engine.toInt(mon.get("currentHp"), 0));
        }
        return total;
    }

    private List<Integer> availableBenchIndexes(List<Map<String, Object>> team, List<Integer> activeSlots) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < team.size(); index++) {
            if (engine.isAvailableMon(team, index) && !activeSlots.contains(index)) {
                indexes.add(index);
            }
        }
        return indexes;
    }
}
