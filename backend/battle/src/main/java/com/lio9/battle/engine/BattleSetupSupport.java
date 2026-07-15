package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 战斗初始状态构建：初始化队伍、场地下标映射与基础属性。
 */
final class BattleSetupSupport {
    private final BattlePreviewSupport previewSupport;
    private final BattleStateSupport stateSupport;
    private final BattleFieldEffectSupport fieldEffectSupport;
    private final BattleConditionSupport conditionSupport;
    private final BattleFlowSupport flowSupport;
    private final int level;
    private final int battleTeamSize;

    BattleSetupSupport(BattlePreviewSupport previewSupport, BattleStateSupport stateSupport,
            BattleFieldEffectSupport fieldEffectSupport, BattleConditionSupport conditionSupport,
            BattleFlowSupport flowSupport, int level, int battleTeamSize) {
        this.previewSupport = previewSupport;
        this.stateSupport = stateSupport;
        this.fieldEffectSupport = fieldEffectSupport;
        this.conditionSupport = conditionSupport;
        this.flowSupport = flowSupport;
        this.level = level;
        this.battleTeamSize = battleTeamSize;
    }

    Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        return createPreviewState(playerTeamJson, opponentTeamJson, maxRounds, seed, "vgc-doubles");
    }

    Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed,
            String format) {
        int activeSlotsLimit = switch (format.toLowerCase()) {
            case "gen9singles", "vgc-singles", "vgc63" -> 1;
            default -> 2;
        };
        int battleTeamSizeVal = switch (format.toLowerCase()) {
            case "gen9singles", "vgc-singles", "vgc63" -> 3;
            default -> 4;
        };
        Map<String, Object> state = new LinkedHashMap<>();
        List<Map<String, Object>> playerRoster = previewSupport
                .normalizeRoster(previewSupport.parseTeam(playerTeamJson));
        List<Map<String, Object>> opponentRoster = previewSupport
                .normalizeRoster(previewSupport.parseTeam(opponentTeamJson));

        state.put("status", "preview");
        state.put("phase", "team-preview");
        state.put("format", format);
        state.put("seed", seed);
        state.put("level", level);
        state.put("teamSize", 6);
        state.put("battleTeamSize", battleTeamSizeVal);
        state.put("activeSlotsLimit", activeSlotsLimit);
        state.put("currentRound", 0);
        state.put("roundLimit", Math.max(1, maxRounds));
        state.put("roundsCount", 0);
        state.put("winner", null);
        state.put("exchangeAvailable", false);
        state.put("exchangeUsed", false);
        state.put("playerRoster", playerRoster);
        state.put("opponentRoster", opponentRoster);
        state.put("playerTeam", new ArrayList<>());
        state.put("opponentTeam", new ArrayList<>());
        state.put("playerSelection", new LinkedHashMap<>());
        state.put("opponentSelection", new LinkedHashMap<>());
        state.put("playerActiveSlots", new ArrayList<>());
        state.put("opponentActiveSlots", new ArrayList<>());
        state.put("playerPendingReplacementCount", 0);
        state.put("playerPendingReplacementOptions", new ArrayList<>());
        state.put("playerSpecialUsed", false);
        state.put("opponentSpecialUsed", false);
        state.put("playerSpecialType", null);
        state.put("opponentSpecialType", null);
        state.put("playerTeraUsed", false);
        state.put("opponentTeraUsed", false);
        state.put("fieldEffects", fieldEffectSupport.defaultFieldEffects());
        state.put("rounds", new ArrayList<>());
        flowSupport.refreshDerivedState(state);
        return state;
    }

    Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        return createBattleState(playerTeamJson, opponentTeamJson, maxRounds, seed, "vgc-doubles");
    }

    Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed,
            String format) {
        Map<String, Object> preview = createPreviewState(playerTeamJson, opponentTeamJson, maxRounds, seed, format);
        int activeSlotsLimit = toInt(preview.get("activeSlotsLimit"), 2);
        int battleTeamSizeVal = toInt(preview.get("battleTeamSize"), 4);
        return applyTeamPreviewSelection(
                preview,
                previewSupport.autoSelect(stateSupport.roster(preview, true), seed, battleTeamSizeVal, activeSlotsLimit),
                previewSupport.autoSelect(stateSupport.roster(preview, false), seed + 31L, battleTeamSizeVal, activeSlotsLimit));
    }

    Map<String, Object> applyTeamPreviewSelection(Map<String, Object> rawState,
            Map<String, Object> playerSelectionInput,
            Map<String, Object> opponentSelectionInput) {
        Map<String, Object> state = stateSupport.cloneState(rawState);
        List<Map<String, Object>> playerRoster = stateSupport.roster(state, true);
        List<Map<String, Object>> opponentRoster = stateSupport.roster(state, false);
        long seed = toLong(state.get("seed"), System.currentTimeMillis());
        int activeSlotsLimit = toInt(state.get("activeSlotsLimit"), 2);
        int battleTeamSizeVal = toInt(state.get("battleTeamSize"), 4);

        Map<String, Object> playerSelection = previewSupport.normalizeSelection(playerSelectionInput, playerRoster,
                seed, battleTeamSizeVal, activeSlotsLimit);
        Map<String, Object> opponentSelection = previewSupport.normalizeSelection(opponentSelectionInput,
                opponentRoster, seed + 31L, battleTeamSizeVal, activeSlotsLimit);

        state.put("playerSelection", playerSelection);
        state.put("opponentSelection", opponentSelection);
        state.put("playerTeam", previewSupport.buildBattleTeam(playerRoster, playerSelection));
        state.put("opponentTeam", previewSupport.buildBattleTeam(opponentRoster, opponentSelection));
        state.put("playerActiveSlots", previewSupport.initialActiveSlots(stateSupport.team(state, true), activeSlotsLimit));
        state.put("opponentActiveSlots", previewSupport.initialActiveSlots(stateSupport.team(state, false), activeSlotsLimit));
        state.put("status", "running");
        state.put("phase", "battle");
        state.put("currentRound", 0);
        state.put("roundsCount", 0);
        state.put("winner", null);
        state.put("playerSpecialUsed", false);
        state.put("opponentSpecialUsed", false);
        state.put("playerSpecialType", null);
        state.put("opponentSpecialType", null);
        state.put("playerTeraUsed", false);
        state.put("opponentTeraUsed", false);
        state.put("rounds", new ArrayList<>());
        flowSupport.clearReplacementState(state);

        List<String> openingEvents = new ArrayList<>();
        flowSupport.appendSendOutEvents(state, true, List.of(), openingEvents);
        flowSupport.appendSendOutEvents(state, false, List.of(), openingEvents);
        conditionSupport.applyEntryAbilities(state, true, List.of(), openingEvents);
        conditionSupport.applyEntryAbilities(state, false, List.of(), openingEvents);
        appendRoundEvents(state, 0, openingEvents);
        flowSupport.refreshDerivedState(state);
        return state;
    }

    Map<String, Object> applyReplacementSelection(Map<String, Object> rawState, Map<String, Object> selectionInput) {
        Map<String, Object> state = stateSupport.cloneState(rawState);
        if (!"replacement".equals(state.getOrDefault("phase", "battle"))) {
            return state;
        }

        int needed = flowSupport.replacementNeededCount(state, true);
        if (needed <= 0) {
            flowSupport.clearReplacementState(state);
            state.put("phase", "battle");
            flowSupport.refreshDerivedState(state);
            return state;
        }

        List<Integer> requested = uniqueIndexes(
                selectionInput == null ? null : selectionInput.get("replacementIndexes"));
        List<Integer> available = flowSupport.availableBenchIndexes(state, true);
        if (requested.size() != needed) {
            throw new IllegalArgumentException("replacement_count_mismatch");
        }
        for (Integer index : requested) {
            if (!available.contains(index)) {
                throw new IllegalArgumentException("invalid_replacement_choice");
            }
        }

        List<Integer> updatedSlots = new ArrayList<>(stateSupport.activeSlots(state, true));
        List<Integer> previousSlots = new ArrayList<>(updatedSlots);

        // 3. 极巨化生命周期管理：极巨化期间禁止轮换
        for (Integer slot : previousSlots) {
            if (slot != null && slot < stateSupport.team(state, true).size()) {
                Map<String, Object> mon = stateSupport.team(state, true).get(slot);
                if (Boolean.TRUE.equals(mon.get("dynamaxed"))) {
                    throw new IllegalArgumentException("cannot_switch_while_dynamaxed");
                }
            }
        }

        // 检查替补是否也处于极巨化状态（虽然逻辑上替补不应该极巨化，但做一层防护）
        for (Integer index : requested) {
            if (index != null && index < stateSupport.team(state, true).size()) {
                Map<String, Object> benchMon = stateSupport.team(state, true).get(index);
                if (Boolean.TRUE.equals(benchMon.get("dynamaxed"))) {
                    throw new IllegalArgumentException("cannot_send_in_dynamaxed_mon");
                }
            }
        }

        updatedSlots.addAll(requested);
        state.put("playerActiveSlots", updatedSlots);
        for (Integer index : requested) {
            if (index != null && index >= 0 && index < stateSupport.team(state, true).size()) {
                Map<String, Object> switchedIn = stateSupport.team(state, true).get(index);
                switchedIn.put("entryRound", toInt(state.get("currentRound"), 0) + 1);
                switchedIn.put("flinch", false); // 与 BattleFlowSupport 保持一致
            }
        }

        List<String> replacementEvents = new ArrayList<>();
        flowSupport.appendSendOutEvents(state, true, previousSlots, replacementEvents);
        conditionSupport.applyEntryAbilities(state, true, previousSlots, replacementEvents);
        appendRoundEvents(state, toInt(state.get("currentRound"), 0), replacementEvents);
        flowSupport.clearReplacementState(state);
        state.put("phase", "battle");
        flowSupport.refreshDerivedState(state);
        return state;
    }

    Map<String, Object> replacePlayerTeamMember(Map<String, Object> rawState, int replacedIndex,
            Map<String, Object> newMember) {
        Map<String, Object> state = stateSupport.cloneState(rawState);
        List<Map<String, Object>> playerRoster = stateSupport.roster(state, true);
        if (replacedIndex < 0 || replacedIndex >= playerRoster.size()) {
            return state;
        }

        playerRoster.set(replacedIndex, previewSupport.normalizePokemon(newMember));
        Map<String, Object> selection = stateSupport.castMap(state.get("playerSelection"));
        state.put(
                "playerTeam",
                previewSupport.buildBattleTeam(
                        playerRoster,
                        selection.isEmpty() ? previewSupport.autoSelect(playerRoster, toLong(state.get("seed"), 0L))
                                : selection));
        state.put("playerActiveSlots", previewSupport.initialActiveSlots(stateSupport.team(state, true),
                toInt(state.get("activeSlotsLimit"), 2)));
        state.put("exchangeUsed", true);
        state.put("exchangeAvailable", false);
        flowSupport.refreshDerivedState(state);
        return state;
    }

    private void appendRoundEvents(Map<String, Object> state, int round, List<String> events) {
        if (events.isEmpty()) {
            return;
        }
        Map<String, Object> roundLog = new LinkedHashMap<>();
        roundLog.put("round", round);
        roundLog.put("actions", new ArrayList<>());
        roundLog.put("events", events);
        roundLog.put("playerActive", flowSupport.activeNames(state, true));
        roundLog.put("opponentActive", flowSupport.activeNames(state, false));
        stateSupport.rounds(state).add(roundLog);
        state.put("roundsCount", stateSupport.rounds(state).size());
    }

    private List<Integer> uniqueIndexes(Object value) {
        Set<Integer> indexes = new LinkedHashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    indexes.add(number.intValue());
                } else if (item != null) {
                    try {
                        indexes.add(Integer.parseInt(item.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return new ArrayList<>(indexes);
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private long toLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}
