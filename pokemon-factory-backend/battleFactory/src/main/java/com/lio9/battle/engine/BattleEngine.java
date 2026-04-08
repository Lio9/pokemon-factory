package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.common.mapper.TypeEfficacyMapper;
import com.lio9.common.util.DamageCalculatorUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 对战引擎。
 * <p>
 * 该类只关心战斗规则本身，不直接处理 HTTP 或数据库：
 * 负责创建初始状态、执行队伍预览、推进回合、处理补位和交换后的状态修正。
 * </p>
 */
@Component
public class BattleEngine {
    private static final int LEVEL = 50;
    private static final int BATTLE_TEAM_SIZE = 4;
    private static final int ACTIVE_SLOTS = 2;

    private final ObjectMapper mapper;
    private final SkillService skillService;
    private final TypeEfficacyMapper typeEfficacyMapper;

    public BattleEngine(SkillService skillService, TypeEfficacyMapper typeEfficacyMapper, ObjectMapper mapper) {
        this.skillService = skillService;
        this.typeEfficacyMapper = typeEfficacyMapper;
        this.mapper = mapper;
    }

    /**
     * 创建“队伍预览阶段”的初始状态。
     */
    public Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        Map<String, Object> state = new LinkedHashMap<>();
        List<Map<String, Object>> playerRoster = normalizeRoster(parseTeam(playerTeamJson));
        List<Map<String, Object>> opponentRoster = normalizeRoster(parseTeam(opponentTeamJson));

        state.put("status", "preview");
        state.put("phase", "team-preview");
        state.put("format", "vgc-doubles");
        state.put("seed", seed);
        state.put("level", LEVEL);
        state.put("teamSize", 6);
        state.put("battleTeamSize", BATTLE_TEAM_SIZE);
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
        state.put("fieldEffects", defaultFieldEffects());
        state.put("rounds", new ArrayList<>());
        refreshDerivedState(state);
        return state;
    }

    /**
     * 直接创建可进入战斗的状态。
     * <p>
     * 该方法会在内部自动完成 6 选 4 和首发选择，主要用于异步自动模拟。
     * </p>
     */
    public Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        Map<String, Object> preview = createPreviewState(playerTeamJson, opponentTeamJson, maxRounds, seed);
        return applyTeamPreviewSelection(preview, autoSelect(roster(preview, true), seed), autoSelect(roster(preview, false), seed + 31L));
    }

    /**
     * 应用队伍预览阶段的选择，并把状态切到 running/battle。
     */
    public Map<String, Object> applyTeamPreviewSelection(Map<String, Object> rawState, Map<String, Object> playerSelectionInput, Map<String, Object> opponentSelectionInput) {
        Map<String, Object> state = cloneState(rawState);
        List<Map<String, Object>> playerRoster = roster(state, true);
        List<Map<String, Object>> opponentRoster = roster(state, false);
        long seed = toLong(state.get("seed"), System.currentTimeMillis());

        Map<String, Object> playerSelection = normalizeSelection(playerSelectionInput, playerRoster, seed);
        Map<String, Object> opponentSelection = normalizeSelection(opponentSelectionInput, opponentRoster, seed + 31L);

        state.put("playerSelection", playerSelection);
        state.put("opponentSelection", opponentSelection);
        state.put("playerTeam", buildBattleTeam(playerRoster, playerSelection));
        state.put("opponentTeam", buildBattleTeam(opponentRoster, opponentSelection));
        state.put("playerActiveSlots", initialActiveSlots(team(state, true)));
        state.put("opponentActiveSlots", initialActiveSlots(team(state, false)));
        state.put("status", "running");
        state.put("phase", "battle");
        state.put("currentRound", 0);
        state.put("roundsCount", 0);
        state.put("winner", null);
        state.put("rounds", new ArrayList<>());
        clearReplacementState(state);
        List<String> openingEvents = new ArrayList<>();
        appendSendOutEvents(state, true, List.of(), openingEvents);
        appendSendOutEvents(state, false, List.of(), openingEvents);
        applyEntryAbilities(state, true, List.of(), openingEvents);
        applyEntryAbilities(state, false, List.of(), openingEvents);
        if (!openingEvents.isEmpty()) {
            Map<String, Object> openingRound = new LinkedHashMap<>();
            openingRound.put("round", 0);
            openingRound.put("actions", new ArrayList<>());
            openingRound.put("events", openingEvents);
            openingRound.put("playerActive", activeNames(state, true));
            openingRound.put("opponentActive", activeNames(state, false));
            rounds(state).add(openingRound);
            state.put("roundsCount", rounds(state).size());
        }
        refreshDerivedState(state);
        return state;
    }

    /**
     * 推进一整个回合。
     */
    public Map<String, Object> playRound(Map<String, Object> rawState, Map<String, String> playerMoveMap) {
        Map<String, Object> state = cloneState(rawState);
        if (!"running".equals(state.get("status")) || !"battle".equals(state.getOrDefault("phase", "battle"))) {
            return state;
        }

        decrementCooldowns(team(state, true));
        decrementCooldowns(team(state, false));
        pruneActiveSlots(state);

        int round = toInt(state.get("currentRound"), 0) + 1;
        state.put("currentRound", round);
        Random random = new Random(toLong(state.get("seed"), 0L) + (round * 97L));
        Map<String, Object> fieldSnapshot = cloneMap(fieldEffects(state));

        Map<String, Boolean> protectedTargets = new HashMap<>();
        Map<String, RedirectionEffect> redirectionTargets = new HashMap<>();
        Map<Map<String, Object>, Boolean> helpingHandBoosts = new IdentityHashMap<>();
        List<Action> actions = new ArrayList<>();
        actions.addAll(buildPlayerActions(state, playerMoveMap));
        actions.addAll(buildOpponentActions(state, random));
        sortActions(actions, trickRoomTurns(state) > 0);

        Map<String, Object> roundLog = new LinkedHashMap<>();
        roundLog.put("round", round);
        List<String> events = new ArrayList<>();
        List<Map<String, Object>> actionLogs = new ArrayList<>();

        for (Action action : actions) {
            List<Map<String, Object>> actingTeam = team(state, "player".equals(action.side));
            List<Map<String, Object>> targetTeam = team(state, !"player".equals(action.side));
            if (!isAvailableMon(actingTeam, action.actorIndex)) {
                continue;
            }

            Map<String, Object> actor = actingTeam.get(action.actorIndex);
            if (Boolean.TRUE.equals(actor.get("flinched"))) {
                Map<String, Object> flinchLog = new LinkedHashMap<>();
                flinchLog.put("side", action.side);
                flinchLog.put("actor", actor.get("name"));
                flinchLog.put("actionType", "flinch");
                flinchLog.put("result", "flinch");
                actionLogs.add(flinchLog);
                events.add(actor.get("name") + " 畏缩了，无法行动");
                actor.put("flinched", false);
                continue;
            }
            if ("paralysis".equals(actor.get("condition")) && random.nextInt(4) == 0) {
                Map<String, Object> paralysisLog = new LinkedHashMap<>();
                paralysisLog.put("side", action.side);
                paralysisLog.put("actor", actor.get("name"));
                paralysisLog.put("actionType", "paralysis");
                paralysisLog.put("result", "paralyzed");
                actionLogs.add(paralysisLog);
                events.add(actor.get("name") + " 因为麻痹而无法行动");
                continue;
            }
            if ("sleep".equals(actor.get("condition"))) {
                if (isSleepingThisTurn(actor, round)) {
                    Map<String, Object> sleepLog = new LinkedHashMap<>();
                    sleepLog.put("side", action.side);
                    sleepLog.put("actor", actor.get("name"));
                    sleepLog.put("actionType", "sleep");
                    sleepLog.put("result", "asleep");
                    actionLogs.add(sleepLog);
                    events.add(actor.get("name") + " 正在睡觉，无法行动");
                    continue;
                }
                actor.put("condition", null);
                actor.put("sleepTurns", 0);
                actor.put("sleepAppliedRound", 0);
                events.add(actor.get("name") + " 醒来了");
            }
            Map<String, Object> actionLog = new LinkedHashMap<>();
            actionLog.put("side", action.side);
            actionLog.put("actor", actor.get("name"));

            if (action.isSwitch()) {
                if (!canSwitch(actingTeam, activeSlots(state, "player".equals(action.side)), action.actorFieldSlot, action.switchToTeamIndex)) {
                    continue;
                }
                List<Integer> previousSlots = new ArrayList<>(activeSlots(state, "player".equals(action.side)));
                Map<String, Object> switchedIn = actingTeam.get(action.switchToTeamIndex);
                actionLog.put("actionType", "switch");
                actionLog.put("switchTo", switchedIn.get("name"));
                actor.put("choiceLockedMove", null);
                resetBattleStages(actor);
                switchedIn.put("entryRound", toInt(state.get("currentRound"), 0) + 1);
                switchedIn.put("flinched", false);
                replaceActiveSlot(state, "player".equals(action.side), action.actorFieldSlot, action.switchToTeamIndex);
                actionLogs.add(actionLog);
                events.add(sideName(action.side) + " 收回了 " + actor.get("name") + "，派出了 " + switchedIn.get("name"));
                applyEntryAbilities(state, "player".equals(action.side), previousSlots, events);
                continue;
            }

            Map<String, Object> move = action.move;
            actionLog.put("move", move.get("name"));
            if (tauntTurns(actor) > 0 && isStatusMove(move)) {
                actionLog.put("result", "taunted");
                actionLogs.add(actionLog);
                events.add(actor.get("name") + " 因挑衅无法使出变化招式");
                continue;
            }

            if (isProtect(move)) {
                protectedTargets.put(protectionKey(action.side, action.actorIndex), true);
                actionLog.put("result", "protect");
                events.add(sideName(action.side) + " 的 " + actor.get("name") + " 使用了 Protect");
                actionLogs.add(actionLog);
                applyCooldown(actor, move);
                continue;
            }

            if (isTailwind(move)) {
                activateTailwind(state, "player".equals(action.side), actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isTrickRoom(move)) {
                toggleTrickRoom(state, actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isRainDance(move)) {
                activateWeather(state, "rain", actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isSunnyDay(move)) {
                activateWeather(state, "sun", actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isElectricTerrain(move)) {
                activateTerrain(state, "electric", actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isPsychicTerrain(move)) {
                activateTerrain(state, "psychic", actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isReflect(move)) {
                activateScreen(state, "reflect", "player".equals(action.side), actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isLightScreen(move)) {
                activateScreen(state, "light-screen", "player".equals(action.side), actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isRedirectionMove(move)) {
                activateRedirection(redirectionTargets, action.side, action.actorIndex, move, actor, actionLog, events);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            if (isHelpingHand(move)) {
                applyHelpingHand(state, action, actor, move, actionLog, events, helpingHandBoosts);
                actionLogs.add(actionLog);
                rememberChoiceMove(actor, move);
                applyCooldown(actor, move);
                continue;
            }

            List<TargetRef> targets = resolveMoveTargets(state, action, move, random, redirectionTargets);
            if (targets.isEmpty()) {
                continue;
            }

            int totalDamage = 0;
            boolean anyHit = false;
            for (TargetRef targetRef : targets) {
                List<Map<String, Object>> targetSideTeam = team(state, targetRef.playerSide());
                if (!isAvailableMon(targetSideTeam, targetRef.teamIndex())) {
                    continue;
                }
                Map<String, Object> target = targetSideTeam.get(targetRef.teamIndex());
                Map<String, Object> targetLog = new LinkedHashMap<>(actionLog);
                targetLog.put("target", target.get("name"));
                targetLog.put("targetFieldSlot", targetRef.fieldSlot());

                if (isBlockedByPsychicTerrain(state, action.side, target, move)) {
                    targetLog.put("result", "psychic-terrain-blocked");
                    targetLog.put("damage", 0);
                    events.add(target.get("name") + " 受到精神场地保护，挡住了先制招式");
                    actionLogs.add(targetLog);
                    continue;
                }

                if (protectedTargets.getOrDefault(protectionKey(targetRef.side(), targetRef.teamIndex()), false)) {
                    targetLog.put("result", "blocked");
                    targetLog.put("damage", 0);
                    events.add(target.get("name") + " 通过 Protect 挡住了攻击");
                    actionLogs.add(targetLog);
                    continue;
                }

                int accuracy = Math.max(1, toInt(move.get("accuracy"), 100));
                if (random.nextInt(100) + 1 > accuracy) {
                    targetLog.put("result", "miss");
                    targetLog.put("damage", 0);
                    events.add(sideName(action.side) + " 的 " + actor.get("name") + " 攻击落空");
                    actionLogs.add(targetLog);
                    continue;
                }

                if (isThunderWave(move)) {
                    applyParalysis(actor, target, move, targetLog, events);
                    actionLogs.add(targetLog);
                    continue;
                }
                if (isWillOWisp(move)) {
                    applyBurn(actor, target, move, targetLog, events);
                    actionLogs.add(targetLog);
                    continue;
                }
                if (isTaunt(move)) {
                    applyTaunt(actor, target, targetLog, events);
                    actionLogs.add(targetLog);
                    continue;
                }
                if (isSpore(move)) {
                    applySleep(state, actor, target, move, targetLog, events, random, round);
                    actionLogs.add(targetLog);
                    continue;
                }

                int damage = calculateDamage(actor, target, move, random, helpingHandBoosts, state);
                if (targets.size() > 1 && isSpreadMove(move)) {
                    damage = Math.max(1, (int) Math.floor(damage * 0.75d));
                }
                int remainingHp = applyIncomingDamage(target, damage, targetLog, events);
                int actualDamage = toInt(targetLog.get("damage"), damage);
                target.put("currentHp", remainingHp);
                if (remainingHp == 0) {
                    target.put("status", "fainted");
                }

                targetLog.put("result", "hit");
                targetLog.put("damage", actualDamage);
                targetLog.put("targetHpAfter", remainingHp);
                actionLogs.add(targetLog);
                events.add(sideName(action.side) + " 的 " + actor.get("name") + " 使用 " + move.get("name") + " 对 " + target.get("name") + " 造成了 " + actualDamage + " 点伤害");

                applyDefenderItemEffects(target, targetLog, events);
                totalDamage += actualDamage;
                anyHit = true;
                if (isFakeOut(move) && remainingHp > 0) {
                    target.put("flinched", true);
                    targetLog.put("flinch", true);
                    events.add(target.get("name") + " 畏缩了");
                }
                if (isIcyWind(move) && remainingHp > 0) {
                    applySpeedDrop(state, action.side, actor, target, targetLog, events);
                }
                if (remainingHp == 0) {
                    events.add(target.get("name") + " 倒下了");
                }
            }

            if (anyHit) {
                applyAttackerItemEffects(actor, totalDamage, actionLog, events);
            }
            rememberChoiceMove(actor, move);
            applyCooldown(actor, move);
        }

        applyEndTurnItemEffects(state, events);
        decrementTauntEffects(state, events);
        decrementFieldEffects(state, fieldSnapshot, events);
        prepareReplacementPhase(state, events);
        clearFlinch(team(state, true));
        clearFlinch(team(state, false));
        roundLog.put("actions", actionLogs);
        roundLog.put("events", events);
        roundLog.put("playerActive", activeNames(state, true));
        roundLog.put("opponentActive", activeNames(state, false));
        rounds(state).add(roundLog);
        state.put("roundsCount", rounds(state).size());

        resolveBattleResult(state);
        refreshDerivedState(state);
        return state;
    }

    /**
     * 自动把一场战斗从当前状态推进到结束。
     * <p>
     * 如果中途进入 replacement 阶段，会自动替玩家补位后继续推进。
     * </p>
     */
    public Map<String, Object> autoPlay(Map<String, Object> rawState, Map<String, String> playerMoveMap) {
        Map<String, Object> state = cloneState(rawState);
        while ("running".equals(state.get("status"))) {
            if ("replacement".equals(state.get("phase"))) {
                state = applyReplacementSelection(state, Map.of("replacementIndexes", autoReplacementIndexes(state, true)));
                continue;
            }
            state = playRound(state, playerMoveMap);
        }
        return state;
    }

    /**
     * 应用玩家补位选择。
     */
    public Map<String, Object> applyReplacementSelection(Map<String, Object> rawState, Map<String, Object> selectionInput) {
        Map<String, Object> state = cloneState(rawState);
        if (!"replacement".equals(state.getOrDefault("phase", "battle"))) {
            return state;
        }

        int needed = replacementNeededCount(state, true);
        if (needed <= 0) {
            clearReplacementState(state);
            state.put("phase", "battle");
            refreshDerivedState(state);
            return state;
        }

        List<Integer> requested = uniqueIndexes(selectionInput == null ? null : selectionInput.get("replacementIndexes"));
        List<Integer> available = availableBenchIndexes(team(state, true), activeSlots(state, true));
        if (requested.size() != needed) {
            throw new IllegalArgumentException("replacement_count_mismatch");
        }
        for (Integer index : requested) {
            if (!available.contains(index)) {
                throw new IllegalArgumentException("invalid_replacement_choice");
            }
        }

        List<Integer> updatedSlots = new ArrayList<>(activeSlots(state, true));
        List<Integer> previousSlots = new ArrayList<>(updatedSlots);
        updatedSlots.addAll(requested);
        state.put("playerActiveSlots", updatedSlots);
        for (Integer index : requested) {
            if (index != null && index >= 0 && index < team(state, true).size()) {
                Map<String, Object> switchedIn = team(state, true).get(index);
                switchedIn.put("entryRound", toInt(state.get("currentRound"), 0) + 1);
                switchedIn.put("flinched", false);
            }
        }
        List<String> replacementEvents = new ArrayList<>();
        appendSendOutEvents(state, true, previousSlots, replacementEvents);
        applyEntryAbilities(state, true, previousSlots, replacementEvents);
        if (!replacementEvents.isEmpty()) {
            Map<String, Object> roundLog = new LinkedHashMap<>();
            roundLog.put("round", toInt(state.get("currentRound"), 0));
            roundLog.put("actions", new ArrayList<>());
            roundLog.put("events", replacementEvents);
            roundLog.put("playerActive", activeNames(state, true));
            roundLog.put("opponentActive", activeNames(state, false));
            rounds(state).add(roundLog);
            state.put("roundsCount", rounds(state).size());
        }
        clearReplacementState(state);
        state.put("phase", "battle");
        refreshDerivedState(state);
        return state;
    }

    /**
     * 直接执行一场完整自动模拟。
     */
    public Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds, Map<String, String> playerMoveMap) {
        long seed = Math.abs((playerTeamJson + opponentTeamJson).hashCode()) + maxRounds;
        return autoPlay(createBattleState(playerTeamJson, opponentTeamJson, maxRounds, seed), playerMoveMap);
    }

    /**
     * 在胜利交换奖励后，用新成员替换玩家原队伍中的一名成员。
     */
    public Map<String, Object> replacePlayerTeamMember(Map<String, Object> rawState, int replacedIndex, Map<String, Object> newMember) {
        Map<String, Object> state = cloneState(rawState);
        List<Map<String, Object>> playerRoster = roster(state, true);
        if (replacedIndex < 0 || replacedIndex >= playerRoster.size()) {
            return state;
        }

        playerRoster.set(replacedIndex, normalizePokemon(newMember));
        Map<String, Object> selection = castMap(state.get("playerSelection"));
        state.put("playerTeam", buildBattleTeam(playerRoster, selection.isEmpty() ? autoSelect(playerRoster, toLong(state.get("seed"), 0L)) : selection));
        state.put("playerActiveSlots", initialActiveSlots(team(state, true)));
        state.put("exchangeUsed", true);
        state.put("exchangeAvailable", false);
        refreshDerivedState(state);
        return state;
    }

    private List<Action> buildPlayerActions(Map<String, Object> state, Map<String, String> playerMoveMap) {
        List<Action> actions = new ArrayList<>();
        List<Integer> activeSlots = activeSlots(state, true);
        List<Map<String, Object>> playerTeam = team(state, true);
        int currentRound = toInt(state.get("currentRound"), 0);
        for (int fieldSlot = 0; fieldSlot < activeSlots.size(); fieldSlot++) {
            int monIndex = activeSlots.get(fieldSlot);
            if (!isAvailableMon(playerTeam, monIndex)) {
                continue;
            }
            if (isSwitchRequested(playerMoveMap, fieldSlot)) {
                int switchToIndex = selectSwitchTarget(playerMoveMap, playerTeam, activeSlots, fieldSlot);
                if (switchToIndex >= 0) {
                    actions.add(Action.switchAction("player", monIndex, fieldSlot, switchToIndex, speedValue(playerTeam.get(monIndex), state, true)));
                    continue;
                }
            }
            Map<String, Object> mon = playerTeam.get(monIndex);
            Map<String, Object> move = selectPlayerMove(mon, playerMoveMap, fieldSlot, currentRound);
            int targetFieldSlot = selectTargetFieldSlot(playerMoveMap, fieldSlot);
            int targetTeamIndex = targetIndex(state, false, targetFieldSlot);
            actions.add(Action.moveAction("player", monIndex, fieldSlot, targetTeamIndex, targetFieldSlot, move, speedValue(mon, state, true)));
        }
        return actions;
    }

    private List<Action> buildOpponentActions(Map<String, Object> state, Random random) {
        List<Action> actions = new ArrayList<>();
        List<Integer> activeSlots = activeSlots(state, false);
        List<Map<String, Object>> opponentTeam = team(state, false);
        int currentRound = toInt(state.get("currentRound"), 0);
        for (int fieldSlot = 0; fieldSlot < activeSlots.size(); fieldSlot++) {
            int monIndex = activeSlots.get(fieldSlot);
            if (!isAvailableMon(opponentTeam, monIndex)) {
                continue;
            }
            int switchToIndex = chooseAISwitch(opponentTeam, activeSlots, monIndex, fieldSlot, random, state);
            if (switchToIndex >= 0) {
                actions.add(Action.switchAction("opponent", monIndex, fieldSlot, switchToIndex, speedValue(opponentTeam.get(monIndex), state, false)));
                continue;
            }
            Map<String, Object> mon = opponentTeam.get(monIndex);
            Map<String, Object> move = selectAIMove(mon, random, state, false, currentRound);
            int targetFieldSlot = random.nextBoolean() ? fieldSlot : (fieldSlot == 0 ? 1 : 0);
            int targetTeamIndex = targetIndex(state, true, targetFieldSlot);
            actions.add(Action.moveAction("opponent", monIndex, fieldSlot, targetTeamIndex, targetFieldSlot, move, speedValue(mon, state, false)));
        }
        return actions;
    }

    private boolean isSwitchRequested(Map<String, String> playerMoveMap, int fieldSlot) {
        if (playerMoveMap == null) {
            return false;
        }
        return "switch".equalsIgnoreCase(String.valueOf(playerMoveMap.get("action-slot-" + fieldSlot)));
    }

    private int selectSwitchTarget(Map<String, String> playerMoveMap, List<Map<String, Object>> team, List<Integer> activeSlots, int fieldSlot) {
        int switchToIndex = toInt(playerMoveMap.get("switch-slot-" + fieldSlot), -1);
        if (canSwitch(team, activeSlots, fieldSlot, switchToIndex)) {
            return switchToIndex;
        }
        return firstAvailableBench(team, activeSlots);
    }

    private int chooseAISwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int activeTeamIndex, int fieldSlot, Random random, Map<String, Object> state) {
        Map<String, Object> mon = team.get(activeTeamIndex);
        int currentHp = toInt(mon.get("currentHp"), 0);
        int maxHp = toInt(castMap(mon.get("stats")).get("hp"), Math.max(1, currentHp));
        if (currentHp <= 0 || maxHp <= 0) {
            return -1;
        }

        int hpPercent = currentHp * 100 / maxHp;
        List<Integer> playerDamageTypeIds = activePlayerDamageTypeIds(state, true);
        double currentVulnerabilityFactor = maxTypeFactorAgainst(mon, playerDamageTypeIds);
        boolean criticalHp = hpPercent <= 30;
        boolean lowHp = hpPercent <= 50;
        boolean superEffectiveVulnerable = currentVulnerabilityFactor > 1.0;

        double switchProbability;
        if (criticalHp) {
            switchProbability = 0.60;
        } else if (lowHp && superEffectiveVulnerable) {
            switchProbability = 0.45;
        } else if (superEffectiveVulnerable) {
            switchProbability = 0.25;
        } else {
            return -1;
        }

        if (random.nextDouble() >= switchProbability) {
            return -1;
        }

        return findBestDefensiveSwitch(team, activeSlots, fieldSlot, playerDamageTypeIds);
    }

    private List<Integer> activePlayerDamageTypeIds(Map<String, Object> state, boolean playerSide) {
        List<Integer> typeIds = new ArrayList<>();
        for (Integer slot : activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, playerSide).size()) {
                continue;
            }
            for (Map<String, Object> move : moves(team(state, playerSide).get(slot))) {
                if (toInt(move.get("power"), 0) > 0) {
                    typeIds.add(toInt(move.get("type_id"), 0));
                }
            }
        }
        return typeIds;
    }

    private double maxTypeFactorAgainst(Map<String, Object> mon, List<Integer> moveTypeIds) {
        if (moveTypeIds.isEmpty()) {
            return 1.0;
        }
        List<Map<String, Object>> monTypes = castList(mon.get("types"));
        double maxFactor = 0.0;
        for (int moveTypeId : moveTypeIds) {
            double moveFactor = 1.0;
            for (Map<String, Object> monType : monTypes) {
                moveFactor *= typeFactor(moveTypeId, toInt(monType.get("type_id"), 0)) / 100.0;
            }
            maxFactor = Math.max(maxFactor, moveFactor);
        }
        return maxFactor;
    }

    private int findBestDefensiveSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int fieldSlot, List<Integer> playerMoveTypeIds) {
        int bestCandidate = -1;
        double bestScore = Double.MAX_VALUE;
        int bestHp = -1;
        for (int candidate = 0; candidate < team.size(); candidate++) {
            if (!canSwitch(team, activeSlots, fieldSlot, candidate)) {
                continue;
            }
            Map<String, Object> candidateMon = team.get(candidate);
            double score = maxTypeFactorAgainst(candidateMon, playerMoveTypeIds);
            int candidateHp = toInt(candidateMon.get("currentHp"), 0);
            if (score < bestScore || (score == bestScore && candidateHp > bestHp)) {
                bestScore = score;
                bestCandidate = candidate;
                bestHp = candidateHp;
            }
        }
        return bestCandidate;
    }

    private Map<String, Object> selectPlayerMove(Map<String, Object> mon, Map<String, String> playerMoveMap, int fieldSlot, int currentRound) {
        Map<String, Object> lockedMove = lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = moves(mon);
        if (playerMoveMap != null) {
            String desiredMove = playerMoveMap.get(mon.get("name_en"));
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get(mon.get("name"));
            }
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get("slot-" + fieldSlot);
            }
            if (desiredMove == null) {
                desiredMove = playerMoveMap.get("__active");
            }
            if (desiredMove != null) {
                for (Map<String, Object> move : moves) {
                    if (String.valueOf(move.get("name_en")).equalsIgnoreCase(desiredMove) || String.valueOf(move.get("name")).equalsIgnoreCase(desiredMove)) {
                        if (cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                            return move;
                        }
                    }
                }
            }
        }
        return defaultMoveSelection(mon, currentRound);
    }

    private int selectTargetFieldSlot(Map<String, String> playerMoveMap, int fieldSlot) {
        if (playerMoveMap == null) {
            return fieldSlot;
        }
        int target = toInt(playerMoveMap.get("target-slot-" + fieldSlot), fieldSlot);
        return Math.max(0, Math.min(1, target));
    }

    private boolean canSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int actorFieldSlot, int switchToIndex) {
        if (switchToIndex < 0 || switchToIndex >= team.size()) {
            return false;
        }
        if (!isAvailableMon(team, switchToIndex)) {
            return false;
        }
        if (activeSlots.contains(switchToIndex)) {
            return false;
        }
        return actorFieldSlot >= 0 && actorFieldSlot < activeSlots.size();
    }

    private int firstAvailableBench(List<Map<String, Object>> team, List<Integer> activeSlots) {
        for (int index = 0; index < team.size(); index++) {
            if (isAvailableMon(team, index) && !activeSlots.contains(index)) {
                return index;
            }
        }
        return -1;
    }

    private Map<String, Object> selectAIMove(Map<String, Object> mon, Random random, Map<String, Object> state, boolean playerSide, int currentRound) {
        Map<String, Object> lockedMove = lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = moves(mon);
        Map<String, Object> fakeOutMove = selectAIFakeOutMove(mon, currentRound);
        if (fakeOutMove != null && random.nextDouble() < 0.65d) {
            return fakeOutMove;
        }
        Map<String, Object> sleepMove = selectAISleepMove(mon, state, playerSide, currentRound);
        if (sleepMove != null && random.nextDouble() < 0.4d) {
            return sleepMove;
        }
        Map<String, Object> terrainMove = selectAITerrainMove(mon, state, playerSide, currentRound);
        if (terrainMove != null && random.nextDouble() < 0.4d) {
            return terrainMove;
        }
        Map<String, Object> screenMove = selectAIScreenMove(mon, state, playerSide, currentRound);
        if (screenMove != null && random.nextDouble() < 0.35d) {
            return screenMove;
        }
        Map<String, Object> tauntMove = selectAITauntMove(mon, state, playerSide, currentRound);
        if (tauntMove != null && random.nextDouble() < 0.4d) {
            return tauntMove;
        }
        Map<String, Object> helpingHandMove = selectAIHelpingHandMove(mon, state, playerSide, currentRound);
        if (helpingHandMove != null && random.nextDouble() < 0.30d) {
            return helpingHandMove;
        }
        Map<String, Object> redirectionMove = selectAIRedirectionMove(mon, state, playerSide, currentRound);
        if (redirectionMove != null && random.nextDouble() < 0.35d) {
            return redirectionMove;
        }
        Map<String, Object> burnMove = selectAIBurnMove(mon, state, playerSide, currentRound);
        if (burnMove != null && random.nextDouble() < 0.35d) {
            return burnMove;
        }
        Map<String, Object> speedControlMove = selectAISpeedControlMove(mon, state, playerSide, currentRound);
        if (speedControlMove != null && random.nextDouble() < 0.35d) {
            return speedControlMove;
        }
        Map<String, Object> weatherMove = selectAIWeatherMove(mon, state, playerSide, currentRound);
        if (weatherMove != null && random.nextDouble() < 0.35d) {
            return weatherMove;
        }
        Map<String, Object> best = defaultMoveSelection(mon, currentRound);
        if (random.nextDouble() < 0.15d) {
            List<Map<String, Object>> available = new ArrayList<>();
            for (Map<String, Object> move : moves) {
                if (cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                    available.add(move);
                }
            }
            if (!available.isEmpty()) {
                return available.get(random.nextInt(available.size()));
            }
        }
        return best;
    }

    private Map<String, Object> selectAIFakeOutMove(Map<String, Object> mon, int currentRound) {
        for (Map<String, Object> move : moves(mon)) {
            if (isFakeOut(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAISleepMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (!opposingSideCanBeSlept(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (isSpore(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAITerrainMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (terrainTurns(state) > 0) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (cooldown(mon, move) != 0 || !canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isElectricTerrain(move)
                    && (sidePrefersTerrain(state, playerSide, DamageCalculatorUtil.TYPE_ELECTRIC)
                    || opposingSideCanSleepAlly(state, playerSide))) {
                return move;
            }
            if (isPsychicTerrain(move)
                    && (sidePrefersTerrain(state, playerSide, DamageCalculatorUtil.TYPE_PSYCHIC)
                    || opposingSideLikelyUsingPriority(state, playerSide))) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAIScreenMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        for (Map<String, Object> move : moves(mon)) {
            if (cooldown(mon, move) != 0 || !canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isReflect(move) && reflectTurns(state, playerSide) == 0 && opposingSideLikelyPhysical(state, playerSide)) {
                return move;
            }
            if (isLightScreen(move) && lightScreenTurns(state, playerSide) == 0 && opposingSideLikelySpecial(state, playerSide)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAITauntMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (!opposingSideLikelyUsingStatus(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (isTaunt(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAISpeedControlMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        for (Map<String, Object> move : moves(mon)) {
            if (cooldown(mon, move) != 0 || !canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isTailwind(move) && tailwindTurns(state, playerSide) == 0) {
                return move;
            }
            if (isTrickRoom(move) && trickRoomTurns(state) == 0) {
                return move;
            }
            if (isThunderWave(move) && opposingSideLikelyFaster(state, playerSide)) {
                return move;
            }
            if (isIcyWind(move) && opposingSideLikelyFaster(state, playerSide)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAIBurnMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (!opposingSideLikelyPhysical(state, playerSide)) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (isWillOWisp(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAIWeatherMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        for (Map<String, Object> move : moves(mon)) {
            if (cooldown(mon, move) != 0 || !canUseMove(mon, move, currentRound)) {
                continue;
            }
            if (isRainDance(move) && rainTurns(state) == 0 && sidePrefersWeather(state, playerSide, DamageCalculatorUtil.TYPE_WATER)) {
                return move;
            }
            if (isSunnyDay(move) && sunTurns(state) == 0 && sidePrefersWeather(state, playerSide, DamageCalculatorUtil.TYPE_FIRE)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAIRedirectionMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (activeSlots(state, playerSide).size() < 2) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (isRedirectionMove(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private Map<String, Object> selectAIHelpingHandMove(Map<String, Object> mon, Map<String, Object> state, boolean playerSide, int currentRound) {
        if (activeSlots(state, playerSide).size() < 2) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (isHelpingHand(move) && cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private boolean opposingSideLikelyFaster(Map<String, Object> state, boolean playerSide) {
        int allyFastest = 0;
        for (Integer slot : activeSlots(state, playerSide)) {
            if (slot != null && slot >= 0 && slot < team(state, playerSide).size()) {
                allyFastest = Math.max(allyFastest, speedValue(team(state, playerSide).get(slot), state, playerSide));
            }
        }
        int opponentFastest = 0;
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot != null && slot >= 0 && slot < team(state, !playerSide).size()) {
                opponentFastest = Math.max(opponentFastest, speedValue(team(state, !playerSide).get(slot), state, !playerSide));
            }
        }
        return opponentFastest >= allyFastest;
    }

    private boolean opposingSideLikelyPhysical(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = team(state, !playerSide).get(slot);
            Map<String, Object> stats = castMap(target.get("stats"));
            if (toInt(stats.get("attack"), 0) >= toInt(stats.get("specialAttack"), 0)
                    && !"burn".equals(target.get("condition"))
                    && !targetHasType(target, DamageCalculatorUtil.TYPE_FIRE)) {
                return true;
            }
        }
        return false;
    }

    private boolean opposingSideLikelySpecial(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = team(state, !playerSide).get(slot);
            Map<String, Object> stats = castMap(target.get("stats"));
            if (toInt(stats.get("specialAttack"), 0) > toInt(stats.get("attack"), 0)
                    && !"sleep".equals(target.get("condition"))) {
                return true;
            }
        }
        return false;
    }

    private boolean sidePrefersWeather(Map<String, Object> state, boolean playerSide, int moveTypeId) {
        for (Integer slot : activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> activeMon = team(state, playerSide).get(slot);
            for (Map<String, Object> move : moves(activeMon)) {
                if (toInt(move.get("type_id"), 0) == moveTypeId && toInt(move.get("power"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sidePrefersTerrain(Map<String, Object> state, boolean playerSide, int moveTypeId) {
        for (Integer slot : activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> activeMon = team(state, playerSide).get(slot);
            if (!isGrounded(activeMon)) {
                continue;
            }
            for (Map<String, Object> move : moves(activeMon)) {
                if (toInt(move.get("type_id"), 0) == moveTypeId && toInt(move.get("power"), 0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opposingSideLikelyUsingStatus(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = team(state, !playerSide).get(slot);
            if (tauntTurns(target) > 0) {
                continue;
            }
            for (Map<String, Object> move : moves(target)) {
                if (isStatusMove(move) && !isTaunt(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opposingSideLikelyUsingPriority(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = team(state, !playerSide).get(slot);
            for (Map<String, Object> move : moves(target)) {
                if (toInt(move.get("priority"), 0) > 0 && !isProtect(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opposingSideCanBeSlept(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            Map<String, Object> target = team(state, !playerSide).get(slot);
            if (toInt(target.get("currentHp"), 0) <= 0) {
                continue;
            }
            if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
                continue;
            }
            if (electricTerrainTurns(state) > 0 && isGrounded(target)) {
                continue;
            }
            if (!targetHasType(target, DamageCalculatorUtil.TYPE_GRASS)) {
                return true;
            }
        }
        return false;
    }

    private boolean opposingSideCanSleepAlly(Map<String, Object> state, boolean playerSide) {
        for (Integer slot : activeSlots(state, !playerSide)) {
            if (slot == null || slot < 0 || slot >= team(state, !playerSide).size()) {
                continue;
            }
            for (Map<String, Object> move : moves(team(state, !playerSide).get(slot))) {
                if (isSpore(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Object> defaultMoveSelection(Map<String, Object> mon, int currentRound) {
        Map<String, Object> lockedMove = lockedChoiceMove(mon, currentRound);
        if (lockedMove != null) {
            return lockedMove;
        }
        List<Map<String, Object>> moves = moves(mon);
        for (Map<String, Object> move : moves) {
            if (cooldown(mon, move) == 0 && !isProtect(move) && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        for (Map<String, Object> move : moves) {
            if (cooldown(mon, move) == 0 && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return moves.isEmpty() ? Map.of("name", "Struggle", "name_en", "struggle", "power", 50, "accuracy", 100, "priority", 0, "damage_class_id", 1, "type_id", 1) : moves.get(0);
    }

    private int calculateDamage(Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move, Random random,
                                Map<Map<String, Object>, Boolean> helpingHandBoosts, Map<String, Object> state) {
        int damageClassId = toInt(move.get("damage_class_id"), DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL);
        Map<String, Object> attackerStats = castMap(attacker.get("stats"));
        Map<String, Object> defenderStats = castMap(defender.get("stats"));

        int attackStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? modifiedAttackStat(attacker, toInt(attackerStats.get("attack"), 100), damageClassId)
                : modifiedAttackStat(attacker, toInt(attackerStats.get("specialAttack"), 100), damageClassId);
        int defenseStat = damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                ? Math.max(1, toInt(defenderStats.get("defense"), 100))
                : Math.max(1, modifiedDefenseStat(defender, toInt(defenderStats.get("specialDefense"), 100), damageClassId));

        int power = Math.max(1, toInt(move.get("power"), 1));
        int baseDamage = DamageCalculatorUtil.calculateBaseDamage(LEVEL, power, attackStat, defenseStat);

        double modifier = 1.0d;
        List<Map<String, Object>> attackerTypes = castList(attacker.get("types"));
        if (attackerTypes.stream().anyMatch(type -> toInt(type.get("type_id"), 0) == toInt(move.get("type_id"), -1))) {
            modifier *= DamageCalculatorUtil.STAB_MULTIPLIER;
        }

        List<Map<String, Object>> defenderTypes = castList(defender.get("types"));
        int moveTypeId = toInt(move.get("type_id"), 0);
        for (Map<String, Object> defenderType : defenderTypes) {
            int factor = typeFactor(moveTypeId, toInt(defenderType.get("type_id"), 0));
            modifier *= factor / 100.0d;
        }

        modifier *= itemDamageModifier(attacker, moveTypeId);
        if (Boolean.TRUE.equals(helpingHandBoosts.get(attacker))) {
            modifier *= 1.5d;
        }
        modifier *= weatherDamageModifier(state, moveTypeId);
        modifier *= terrainDamageModifier(state, attacker, moveTypeId);
        modifier *= screenDamageModifier(state, defender, damageClassId);

        modifier *= (0.85d + (random.nextDouble() * 0.15d));
        return Math.max(1, (int) Math.floor(baseDamage * modifier));
    }

    private int typeFactor(int attackingTypeId, int defendingTypeId) {
        Integer factor = typeEfficacyMapper.selectDamageFactor(attackingTypeId, defendingTypeId);
        return factor == null ? 100 : factor;
    }

    private void applyCooldown(Map<String, Object> mon, Map<String, Object> move) {
        int cooldown = skillService.getCooldown(String.valueOf(move.get("name_en")), isProtect(move) ? 2 : 0);
        if (cooldown > 0) {
            cooldowns(mon).put(String.valueOf(move.get("name_en")), cooldown + 1);
        }
    }

    private boolean isProtect(Map<String, Object> move) {
        return "protect".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isFakeOut(Map<String, Object> move) {
        return "fake-out".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "fake out".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isTaunt(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "taunt".equalsIgnoreCase(nameEn);
    }

    private boolean isSpore(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "spore".equalsIgnoreCase(nameEn);
    }

    private boolean isReflect(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "reflect".equalsIgnoreCase(nameEn);
    }

    private boolean isLightScreen(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "light-screen".equalsIgnoreCase(nameEn) || "light screen".equalsIgnoreCase(nameEn);
    }

    private boolean canUseMove(Map<String, Object> mon, Map<String, Object> move, int currentRound) {
        String item = heldItem(mon);
        if ("assault-vest".equals(item) && isStatusMove(move)) {
            return false;
        }
        if (tauntTurns(mon) > 0 && isStatusMove(move)) {
            return false;
        }
        if (isFakeOut(move) && toInt(mon.get("entryRound"), 1) != currentRound) {
            return false;
        }
        return true;
    }

    private boolean isStatusMove(Map<String, Object> move) {
        return toInt(move.get("damage_class_id"), 0) == 3 || toInt(move.get("power"), 0) == 0;
    }

    private boolean isFollowMe(Map<String, Object> move) {
        return "follow-me".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "follow me".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isRagePowder(Map<String, Object> move) {
        return "rage-powder".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "rage powder".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isRedirectionMove(Map<String, Object> move) {
        return isFollowMe(move) || isRagePowder(move);
    }

    private boolean isPowderMove(Map<String, Object> move) {
        return isRagePowder(move) || isSpore(move);
    }

    private boolean isHelpingHand(Map<String, Object> move) {
        return "helping-hand".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "helping hand".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private List<TargetRef> resolveMoveTargets(Map<String, Object> state, Action action, Map<String, Object> move, Random random,
                                               Map<String, RedirectionEffect> redirectionTargets) {
        int targetId = targetId(move);
        return switch (targetId) {
            case 7, 4 -> List.of(new TargetRef(action.side, "player".equals(action.side), action.actorIndex, action.actorFieldSlot));
            case 11, 6 -> activeTargetRefs(state, !"player".equals(action.side));
            case 8 -> randomOpponentTargetRefs(state, !"player".equals(action.side), random);
            case 9 -> allOtherActiveTargetRefs(state, action);
            case 13, 5 -> activeTargetRefs(state, "player".equals(action.side));
            case 14, 12 -> allActiveTargetRefs(state);
            case 3 -> allyTargetRefs(state, action);
            default -> singleOpponentTargetRefs(state, !"player".equals(action.side), action.targetFieldSlot, redirectionTargets,
                    team(state, "player".equals(action.side)).get(action.actorIndex));
        };
    }

    private int resolveTargetIndex(List<Map<String, Object>> targetTeam, int preferredIndex) {
        if (isAvailableMon(targetTeam, preferredIndex)) {
            return preferredIndex;
        }
        for (int index = 0; index < targetTeam.size(); index++) {
            if (isAvailableMon(targetTeam, index)) {
                return index;
            }
        }
        return -1;
    }

    private int targetIndex(Map<String, Object> state, boolean playerTarget, int targetFieldSlot) {
        List<Integer> targets = activeSlots(state, playerTarget);
        if (targets.isEmpty()) {
            return -1;
        }
        int normalizedSlot = Math.max(0, Math.min(targets.size() - 1, targetFieldSlot));
        return targets.get(normalizedSlot);
    }

    private int fieldSlotForTeamIndex(Map<String, Object> state, boolean playerSide, int teamIndex) {
        List<Integer> active = activeSlots(state, playerSide);
        for (int fieldSlot = 0; fieldSlot < active.size(); fieldSlot++) {
            if (active.get(fieldSlot) == teamIndex) {
                return fieldSlot;
            }
        }
        return 0;
    }

    private int redirectedTargetIndex(Map<String, Object> state, boolean playerTarget, Map<String, RedirectionEffect> redirectionTargets,
                                      Map<String, Object> attacker) {
        RedirectionEffect redirected = redirectionTargets.get(playerTarget ? "player" : "opponent");
        if (redirected == null) {
            return -1;
        }
        if (redirected.powder() && isPowderImmune(attacker)) {
            return -1;
        }
        return isAvailableMon(team(state, playerTarget), redirected.actorIndex()) ? redirected.actorIndex() : -1;
    }

    private List<TargetRef> singleOpponentTargetRefs(Map<String, Object> state, boolean playerTarget, int targetFieldSlot,
                                                     Map<String, RedirectionEffect> redirectionTargets, Map<String, Object> attacker) {
        int redirected = redirectedTargetIndex(state, playerTarget, redirectionTargets, attacker);
        int teamIndex = redirected >= 0 ? redirected : targetIndex(state, playerTarget, targetFieldSlot);
        if (teamIndex < 0) {
            return List.of();
        }
        return List.of(new TargetRef(playerTarget ? "player" : "opponent", playerTarget, teamIndex, fieldSlotForTeamIndex(state, playerTarget, teamIndex)));
    }

    private List<TargetRef> randomOpponentTargetRefs(Map<String, Object> state, boolean playerTarget, Random random) {
        List<TargetRef> targets = activeTargetRefs(state, playerTarget);
        if (targets.isEmpty()) {
            return List.of();
        }
        return List.of(targets.get(random.nextInt(targets.size())));
    }

    private List<TargetRef> activeTargetRefs(Map<String, Object> state, boolean playerSide) {
        List<TargetRef> targets = new ArrayList<>();
        List<Integer> active = activeSlots(state, playerSide);
        for (int fieldSlot = 0; fieldSlot < active.size(); fieldSlot++) {
            int teamIndex = active.get(fieldSlot);
            if (isAvailableMon(team(state, playerSide), teamIndex)) {
                targets.add(new TargetRef(playerSide ? "player" : "opponent", playerSide, teamIndex, fieldSlot));
            }
        }
        return targets;
    }

    private List<TargetRef> allOtherActiveTargetRefs(Map<String, Object> state, Action action) {
        List<TargetRef> targets = new ArrayList<>();
        for (TargetRef target : activeTargetRefs(state, true)) {
            if (!("player".equals(action.side) && target.teamIndex() == action.actorIndex)) {
                targets.add(target);
            }
        }
        for (TargetRef target : activeTargetRefs(state, false)) {
            if (!("opponent".equals(action.side) && target.teamIndex() == action.actorIndex)) {
                targets.add(target);
            }
        }
        return targets;
    }

    private List<TargetRef> allActiveTargetRefs(Map<String, Object> state) {
        List<TargetRef> targets = new ArrayList<>();
        targets.addAll(activeTargetRefs(state, true));
        targets.addAll(activeTargetRefs(state, false));
        return targets;
    }

    private List<TargetRef> allyTargetRefs(Map<String, Object> state, Action action) {
        List<TargetRef> targets = new ArrayList<>();
        for (TargetRef target : activeTargetRefs(state, "player".equals(action.side))) {
            if (target.teamIndex() != action.actorIndex) {
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            return List.of(new TargetRef(action.side, "player".equals(action.side), action.actorIndex, action.actorFieldSlot));
        }
        return List.of(targets.get(0));
    }

    private int targetId(Map<String, Object> move) {
        return toInt(move.get("target_id"), 10);
    }

    private boolean isTailwind(Map<String, Object> move) {
        return "tailwind".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isTrickRoom(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "trick-room".equalsIgnoreCase(nameEn) || "trick room".equalsIgnoreCase(nameEn);
    }

    private boolean isRainDance(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "rain-dance".equalsIgnoreCase(nameEn) || "rain dance".equalsIgnoreCase(nameEn);
    }

    private boolean isSunnyDay(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "sunny-day".equalsIgnoreCase(nameEn) || "sunny day".equalsIgnoreCase(nameEn);
    }

    private boolean isElectricTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "electric-terrain".equalsIgnoreCase(nameEn) || "electric terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isPsychicTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "psychic-terrain".equalsIgnoreCase(nameEn) || "psychic terrain".equalsIgnoreCase(nameEn);
    }

    private boolean isIcyWind(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "icy-wind".equalsIgnoreCase(nameEn) || "icy wind".equalsIgnoreCase(nameEn);
    }

    private boolean isThunderWave(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "thunder-wave".equalsIgnoreCase(nameEn) || "thunder wave".equalsIgnoreCase(nameEn);
    }

    private boolean isWillOWisp(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "will-o-wisp".equalsIgnoreCase(nameEn) || "will o wisp".equalsIgnoreCase(nameEn);
    }

    private boolean isSpreadMove(Map<String, Object> move) {
        return switch (targetId(move)) {
            case 9, 11, 12, 13, 14 -> true;
            default -> false;
        };
    }

    private void activateRedirection(Map<String, RedirectionEffect> redirectionTargets, String side, int actorIndex, Map<String, Object> move,
                                     Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        redirectionTargets.put(side, new RedirectionEffect(actorIndex, isRagePowder(move)));
        actionLog.put("result", "redirection");
        actionLog.put("actionType", "redirection");
        events.add(actor.get("name") + " 使用了 " + move.get("name") + "，吸引了对手的招式");
    }

    private void applyHelpingHand(Map<String, Object> state, Action action, Map<String, Object> actor, Map<String, Object> move,
                                  Map<String, Object> actionLog, List<String> events,
                                  Map<Map<String, Object>, Boolean> helpingHandBoosts) {
        List<TargetRef> targets = allyTargetRefs(state, action);
        if (targets.isEmpty()) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了 " + move.get("name") + "，但失败了");
            return;
        }
        TargetRef targetRef = targets.get(0);
        Map<String, Object> target = team(state, targetRef.playerSide()).get(targetRef.teamIndex());
        helpingHandBoosts.put(target, true);
        actionLog.put("result", "helping-hand");
        actionLog.put("target", target.get("name"));
        events.add(actor.get("name") + " 使用了 " + move.get("name") + "，帮助了 " + target.get("name"));
    }

    private String protectionKey(String side, int index) {
        return side + "-" + index;
    }

    private String oppositeSide(String side) {
        return "player".equals(side) ? "opponent" : "player";
    }

    private String sideName(String side) {
        return "player".equals(side) ? "玩家" : "对手";
    }

    private void decrementCooldowns(List<Map<String, Object>> team) {
        for (Map<String, Object> mon : team) {
            Map<String, Object> cooldowns = cooldowns(mon);
            for (String key : new ArrayList<>(cooldowns.keySet())) {
                cooldowns.put(key, Math.max(0, toInt(cooldowns.get(key), 0) - 1));
            }
        }
    }

    private void resolveBattleResult(Map<String, Object> state) {
        pruneActiveSlots(state);
        boolean playerAlive = hasAvailableMon(team(state, true));
        boolean opponentAlive = hasAvailableMon(team(state, false));
        boolean roundLimitReached = toInt(state.get("currentRound"), 0) >= toInt(state.get("roundLimit"), 12);

        if (!playerAlive || !opponentAlive || roundLimitReached) {
            state.put("status", "completed");
            state.put("phase", "completed");
            if (!playerAlive && opponentAlive) {
                state.put("winner", "opponent");
            } else if (!opponentAlive && playerAlive) {
                state.put("winner", "player");
            } else {
                state.put("winner", totalRemainingHp(team(state, true)) >= totalRemainingHp(team(state, false)) ? "player" : "opponent");
            }
            state.put("exchangeAvailable", "player".equals(state.get("winner")) && !Boolean.TRUE.equals(state.get("exchangeUsed")));
        }
    }

    private void refreshDerivedState(Map<String, Object> state) {
        state.put("playerRemaining", aliveCount(team(state, true)));
        state.put("opponentRemaining", aliveCount(team(state, false)));
        state.put("playerStrength", totalRemainingHp(team(state, true)));
        state.put("opponentStrength", totalRemainingHp(team(state, false)));
    }

    private Map<String, Object> autoSelect(List<Map<String, Object>> roster, long seed) {
        List<Integer> picked = new ArrayList<>();
        List<Map<String, Object>> sorted = new ArrayList<>();
        for (int index = 0; index < roster.size(); index++) {
            Map<String, Object> entry = new LinkedHashMap<>(roster.get(index));
            entry.put("rosterIndex", index);
            sorted.add(entry);
        }
        sorted.sort(Comparator.comparingInt((Map<String, Object> mon) -> toInt(mon.get("battleScore"), 0)).reversed()
                .thenComparingInt(mon -> toInt(castMap(mon.get("stats")).get("speed"), 0)).reversed());
        for (int index = 0; index < Math.min(BATTLE_TEAM_SIZE, sorted.size()); index++) {
            picked.add(toInt(sorted.get(index).get("rosterIndex"), index));
        }
        Map<String, Object> tempSelection = new LinkedHashMap<>();
        tempSelection.put("pickedRosterIndexes", picked);
        List<Integer> leads = new ArrayList<>();
        List<Integer> bySpeed = new ArrayList<>(picked);
        bySpeed.sort(Comparator.comparingInt((Integer index) -> toInt(castMap(roster.get(index).get("stats")).get("speed"), 0)).reversed());
        for (int index = 0; index < Math.min(ACTIVE_SLOTS, bySpeed.size()); index++) {
            leads.add(bySpeed.get(index));
        }
        if (leads.size() < ACTIVE_SLOTS && !picked.isEmpty()) {
            Random random = new Random(seed);
            List<Integer> shuffled = new ArrayList<>(picked);
            java.util.Collections.shuffle(shuffled, random);
            for (Integer candidate : shuffled) {
                if (!leads.contains(candidate)) {
                    leads.add(candidate);
                    if (leads.size() >= ACTIVE_SLOTS) {
                        break;
                    }
                }
            }
        }
        tempSelection.put("leadRosterIndexes", leads);
        return tempSelection;
    }

    private Map<String, Object> normalizeSelection(Map<String, Object> rawSelection, List<Map<String, Object>> roster, long seed) {
        List<Integer> picked = uniqueIndexes(rawSelection == null ? null : rawSelection.get("pickedRosterIndexes"));
        List<Integer> leads = uniqueIndexes(rawSelection == null ? null : rawSelection.get("leadRosterIndexes"));

        boolean validPicked = picked.size() == Math.min(BATTLE_TEAM_SIZE, roster.size()) && picked.stream().allMatch(index -> index >= 0 && index < roster.size());
        boolean validLeads = leads.size() == Math.min(ACTIVE_SLOTS, picked.size()) && leads.stream().allMatch(picked::contains);
        if (!validPicked || !validLeads) {
            return autoSelect(roster, seed);
        }

        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("pickedRosterIndexes", picked);
        selection.put("leadRosterIndexes", leads);
        return selection;
    }

    private List<Map<String, Object>> buildBattleTeam(List<Map<String, Object>> roster, Map<String, Object> selection) {
        List<Integer> picked = uniqueIndexes(selection.get("pickedRosterIndexes"));
        List<Integer> leads = uniqueIndexes(selection.get("leadRosterIndexes"));
        List<Integer> ordered = new ArrayList<>(leads);
        for (Integer index : picked) {
            if (!ordered.contains(index)) {
                ordered.add(index);
            }
        }

        List<Map<String, Object>> selected = new ArrayList<>();
        for (Integer rosterIndex : ordered) {
            if (rosterIndex == null || rosterIndex < 0 || rosterIndex >= roster.size()) {
                continue;
            }
            Map<String, Object> mon = normalizePokemon(roster.get(rosterIndex));
            mon.put("rosterIndex", rosterIndex);
            mon.put("currentHp", toInt(castMap(mon.get("stats")).get("hp"), 1));
            mon.put("status", "ready");
            mon.put("condition", null);
            mon.put("cooldowns", new LinkedHashMap<>());
            mon.put("entryRound", 1);
            mon.put("flinched", false);
            mon.put("sleepTurns", 0);
            mon.put("sleepAppliedRound", 0);
            mon.put("tauntTurns", 0);
            mon.putIfAbsent("itemConsumed", false);
            mon.putIfAbsent("choiceLockedMove", null);
            selected.add(mon);
        }
        return selected;
    }

    private List<Integer> initialActiveSlots(List<Map<String, Object>> team) {
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < team.size() && slots.size() < ACTIVE_SLOTS; index++) {
            if (isAvailableMon(team, index)) {
                slots.add(index);
            }
        }
        return slots;
    }

    private void pruneActiveSlots(Map<String, Object> state) {
        state.put("playerActiveSlots", pruneSideActiveSlots(team(state, true), activeSlots(state, true)));
        state.put("opponentActiveSlots", pruneSideActiveSlots(team(state, false), activeSlots(state, false)));
    }

    private void prepareReplacementPhase(Map<String, Object> state, List<String> events) {
        pruneActiveSlots(state);
        autoFillSideActiveSlotsWithEvents(state, false, events);
        int playerReplacementCount = replacementNeededCount(state, true);
        if (playerReplacementCount > 0) {
            state.put("phase", "replacement");
            state.put("playerPendingReplacementCount", playerReplacementCount);
            state.put("playerPendingReplacementOptions", availableBenchIndexes(team(state, true), activeSlots(state, true)));
            events.add("我方有宝可梦倒下，请选择替补上场。");
            return;
        }
        clearReplacementState(state);
        state.put("phase", "battle");
    }

    private void autoFillSideActiveSlotsWithEvents(Map<String, Object> state, boolean player, List<String> events) {
        List<Integer> previousSlots = new ArrayList<>(activeSlots(state, player));
        List<Integer> refreshed = fillSideActiveSlots(team(state, player), previousSlots);
        state.put(player ? "playerActiveSlots" : "opponentActiveSlots", refreshed);
        for (Integer slot : refreshed) {
            if (slot != null && !previousSlots.contains(slot) && slot >= 0 && slot < team(state, player).size()) {
                Map<String, Object> switchedIn = team(state, player).get(slot);
                switchedIn.put("entryRound", toInt(state.get("currentRound"), 0) + 1);
                switchedIn.put("flinched", false);
            }
        }
        appendSendOutEvents(state, player, previousSlots, events);
        applyEntryAbilities(state, player, previousSlots, events);
    }

    private List<Integer> pruneSideActiveSlots(List<Map<String, Object>> team, List<Integer> currentSlots) {
        List<Integer> refreshed = new ArrayList<>();
        for (Integer slot : currentSlots) {
            if (slot != null && isAvailableMon(team, slot) && !refreshed.contains(slot)) {
                refreshed.add(slot);
            }
        }
        return refreshed;
    }

    private List<Integer> fillSideActiveSlots(List<Map<String, Object>> team, List<Integer> currentSlots) {
        List<Integer> refreshed = pruneSideActiveSlots(team, currentSlots);
        for (int index = 0; index < team.size() && refreshed.size() < ACTIVE_SLOTS; index++) {
            if (isAvailableMon(team, index) && !refreshed.contains(index)) {
                refreshed.add(index);
            }
        }
        return refreshed;
    }

    private void appendSendOutEvents(Map<String, Object> state, boolean player, List<Integer> previousSlots, List<String> events) {
        List<Integer> currentSlots = activeSlots(state, player);
        List<Map<String, Object>> sideTeam = team(state, player);
        for (Integer slot : currentSlots) {
            if (!previousSlots.contains(slot) && slot != null && slot >= 0 && slot < sideTeam.size()) {
                events.add(sideName(player ? "player" : "opponent") + " 派出了 " + sideTeam.get(slot).get("name"));
            }
        }
    }

    private boolean hasAvailableMon(List<Map<String, Object>> team) {
        return aliveCount(team) > 0;
    }

    private int aliveCount(List<Map<String, Object>> team) {
        int count = 0;
        for (Map<String, Object> mon : team) {
            if (toInt(mon.get("currentHp"), 0) > 0) {
                count++;
            }
        }
        return count;
    }

    private int totalRemainingHp(List<Map<String, Object>> team) {
        int total = 0;
        for (Map<String, Object> mon : team) {
            total += Math.max(0, toInt(mon.get("currentHp"), 0));
        }
        return total;
    }

    private List<String> activeNames(Map<String, Object> state, boolean player) {
        List<String> names = new ArrayList<>();
        List<Map<String, Object>> team = team(state, player);
        for (Integer slot : activeSlots(state, player)) {
            if (slot != null && slot >= 0 && slot < team.size()) {
                names.add(String.valueOf(team.get(slot).get("name")));
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTeam(String teamJson) {
        if (teamJson == null || teamJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            Object parsed = mapper.readValue(teamJson, Object.class);
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            if (parsed instanceof Map) {
                return new ArrayList<>(List.of((Map<String, Object>) parsed));
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> normalizeRoster(List<Map<String, Object>> roster) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> pokemon : roster) {
            normalized.add(normalizePokemon(pokemon));
        }
        return normalized;
    }

    private Map<String, Object> normalizePokemon(Map<String, Object> pokemon) {
        Map<String, Object> normalized = cloneMap(pokemon);
        normalized.put("stats", castMap(normalized.get("stats")));
        normalized.put("types", castList(normalized.get("types")));
        normalized.put("moves", normalizeMoves(castList(normalized.get("moves"))));
        normalized.putIfAbsent("currentHp", toInt(castMap(normalized.get("stats")).get("hp"), 1));
        normalized.putIfAbsent("cooldowns", new LinkedHashMap<>());
        normalized.putIfAbsent("statStages", new LinkedHashMap<>(Map.of("attack", 0, "speed", 0)));
        normalized.putIfAbsent("condition", null);
        normalized.putIfAbsent("entryRound", 1);
        normalized.putIfAbsent("flinched", false);
        normalized.putIfAbsent("sleepTurns", 0);
        normalized.putIfAbsent("sleepAppliedRound", 0);
        normalized.putIfAbsent("tauntTurns", 0);
        normalized.putIfAbsent("itemConsumed", false);
        normalized.putIfAbsent("choiceLockedMove", null);
        return normalized;
    }

    private List<Map<String, Object>> normalizeMoves(List<Map<String, Object>> moves) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> move : moves) {
            Map<String, Object> copied = cloneMap(move);
            copied.put("power", toInt(copied.get("power"), 0));
            copied.put("accuracy", toInt(copied.get("accuracy"), 100));
            copied.put("priority", toInt(copied.get("priority"), 0));
            copied.put("damage_class_id", toInt(copied.get("damage_class_id"), 0));
            copied.put("type_id", toInt(copied.get("type_id"), 0));
            copied.put("target_id", toInt(copied.get("target_id"), 0));
            normalized.add(copied);
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cloneState(Map<String, Object> state) {
        try {
            return mapper.readValue(mapper.writeValueAsString(state), Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>(state);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    private Map<String, Object> cloneMap(Map<String, Object> value) {
        return new LinkedHashMap<>(value == null ? Map.of() : value);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerTeam" : "opponentTeam");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> roster(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerRoster" : "opponentRoster");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rounds(Map<String, Object> state) {
        Object value = state.get("rounds");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        List<Map<String, Object>> created = new ArrayList<>();
        state.put("rounds", created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> activeSlots(Map<String, Object> state, boolean player) {
        Object value = state.get(player ? "playerActiveSlots" : "opponentActiveSlots");
        if (value instanceof List<?> list) {
            List<Integer> slots = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    slots.add(number.intValue());
                }
            }
            return slots;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> moves(Map<String, Object> mon) {
        Object value = mon.get("moves");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cooldowns(Map<String, Object> mon) {
        Object value = mon.get("cooldowns");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        mon.put("cooldowns", created);
        return created;
    }

    private int cooldown(Map<String, Object> mon, Map<String, Object> move) {
        return toInt(cooldowns(mon).get(move.get("name_en")), 0);
    }

    private int modifiedAttackStat(Map<String, Object> mon, int baseStat, int damageClassId) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            baseStat = applyStageModifier(baseStat, statStage(mon, "attack"));
            if ("burn".equals(mon.get("condition"))) {
                baseStat = Math.max(1, (int) Math.floor(baseStat * 0.5d));
            }
        }
        String item = heldItem(mon);
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL && "choice-band".equals(item)) {
            return (int) Math.floor(baseStat * 1.5d);
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && "choice-specs".equals(item)) {
            return (int) Math.floor(baseStat * 1.5d);
        }
        return baseStat;
    }

    private int modifiedDefenseStat(Map<String, Object> mon, int baseStat, int damageClassId) {
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && "assault-vest".equals(heldItem(mon))) {
            return (int) Math.floor(baseStat * 1.5d);
        }
        return baseStat;
    }

    private double itemDamageModifier(Map<String, Object> mon, int moveTypeId) {
        String item = heldItem(mon);
        return switch (item) {
            case "life-orb" -> 1.3d;
            case "mystic-water" -> moveTypeId == DamageCalculatorUtil.TYPE_WATER ? 1.2d : 1.0d;
            case "charcoal" -> moveTypeId == DamageCalculatorUtil.TYPE_FIRE ? 1.2d : 1.0d;
            case "miracle-seed" -> moveTypeId == DamageCalculatorUtil.TYPE_GRASS ? 1.2d : 1.0d;
            default -> 1.0d;
        };
    }

    private int speedValue(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        int speed = toInt(castMap(mon.get("stats")).get("speed"), 0);
        speed = applyStageModifier(speed, statStage(mon, "speed"));
        if ("paralysis".equals(mon.get("condition"))) {
            speed = Math.max(1, speed / 2);
        }
        if ("choice-scarf".equals(heldItem(mon))) {
            speed = (int) Math.floor(speed * 1.5d);
        }
        if (tailwindTurns(state, playerSide) > 0) {
            speed *= 2;
        }
        return speed;
    }

    private String heldItem(Map<String, Object> mon) {
        Object item = mon.get("heldItem");
        return item == null ? "" : String.valueOf(item);
    }

    private String abilityName(Map<String, Object> mon) {
        Object ability = mon.get("ability");
        if (ability instanceof Map<?, ?> abilityMap) {
            Object nameEn = abilityMap.get("name_en");
            if (nameEn != null && !String.valueOf(nameEn).isBlank()) {
                return String.valueOf(nameEn);
            }
            Object name = abilityMap.get("name");
            if (name != null) {
                return String.valueOf(name);
            }
        }
        return ability == null ? "" : String.valueOf(ability);
    }

    private boolean itemConsumed(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("itemConsumed"));
    }

    private void applyParalysis(Map<String, Object> source, Map<String, Object> target, Map<String, Object> move,
                                Map<String, Object> actionLog, List<String> events) {
        if (targetHasType(target, DamageCalculatorUtil.TYPE_ELECTRIC)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫麻痹");
            return;
        }
        if (isTypeImmune(move, target)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫 " + move.get("name"));
            return;
        }
        if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于异常状态");
            return;
        }
        target.put("condition", "paralysis");
        actionLog.put("result", "paralysis");
        actionLog.put("condition", "paralysis");
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了麻痹状态");
    }

    private void applyBurn(Map<String, Object> source, Map<String, Object> target, Map<String, Object> move,
                           Map<String, Object> actionLog, List<String> events) {
        if (targetHasType(target, DamageCalculatorUtil.TYPE_FIRE)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫灼伤");
            return;
        }
        if (isTypeImmune(move, target)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫 " + move.get("name"));
            return;
        }
        if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于异常状态");
            return;
        }
        target.put("condition", "burn");
        actionLog.put("result", "burn");
        actionLog.put("condition", "burn");
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了灼伤状态");
    }

    private void applySleep(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target, Map<String, Object> move,
                            Map<String, Object> actionLog, List<String> events, Random random, int currentRound) {
        if (electricTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到电气场地保护，无法陷入睡眠");
            return;
        }
        if (isPowderImmune(target)) {
            actionLog.put("result", "status-immune");
            events.add(powderImmunityMessage(target, move));
            return;
        }
        if (isTypeImmune(move, target)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫 " + move.get("name"));
            return;
        }
        if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于异常状态");
            return;
        }
        int turns = random.nextInt(3) + 1;
        target.put("condition", "sleep");
        target.put("sleepTurns", turns);
        target.put("sleepAppliedRound", currentRound);
        actionLog.put("result", "sleep");
        actionLog.put("condition", "sleep");
        actionLog.put("sleepTurns", turns);
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了睡眠");
    }

    private void applyTaunt(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog, List<String> events) {
        if (tauntTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于挑衅状态");
            return;
        }
        target.put("tauntTurns", 3);
        actionLog.put("result", "taunt");
        actionLog.put("tauntTurns", 3);
        events.add(source.get("name") + " 挑衅了 " + target.get("name"));
    }

    private void consumeItem(Map<String, Object> mon) {
        mon.put("itemConsumed", true);
    }

    private int applyIncomingDamage(Map<String, Object> target, int damage, Map<String, Object> actionLog, List<String> events) {
        int currentHp = toInt(target.get("currentHp"), 0);
        int maxHp = toInt(castMap(target.get("stats")).get("hp"), Math.max(1, currentHp));
        int actualDamage = damage;
        if ("focus-sash".equals(heldItem(target)) && !itemConsumed(target) && currentHp == maxHp && damage >= currentHp) {
            actualDamage = Math.max(0, currentHp - 1);
            consumeItem(target);
            events.add(target.get("name") + " 靠气势披带撑住了攻击");
            actionLog.put("focusSash", true);
        }
        actionLog.put("damage", actualDamage);
        return Math.max(0, currentHp - actualDamage);
    }

    private void applyDefenderItemEffects(Map<String, Object> target, Map<String, Object> actionLog, List<String> events) {
        if (!"sitrus-berry".equals(heldItem(target)) || itemConsumed(target) || toInt(target.get("currentHp"), 0) <= 0) {
            return;
        }
        int maxHp = toInt(castMap(target.get("stats")).get("hp"), 1);
        int currentHp = toInt(target.get("currentHp"), 0);
        if (currentHp * 2 <= maxHp) {
            int heal = Math.max(1, maxHp / 4);
            target.put("currentHp", Math.min(maxHp, currentHp + heal));
            consumeItem(target);
            actionLog.put("berryHeal", heal);
            events.add(target.get("name") + " 食用了文柚果，回复了 " + heal + " 点 HP");
        }
    }

    private void applyAttackerItemEffects(Map<String, Object> attacker, int damage, Map<String, Object> actionLog, List<String> events) {
        if (!"life-orb".equals(heldItem(attacker)) || damage <= 0 || toInt(attacker.get("currentHp"), 0) <= 0) {
            return;
        }
        int maxHp = toInt(castMap(attacker.get("stats")).get("hp"), 1);
        int recoil = Math.max(1, maxHp / 10);
        int currentHp = toInt(attacker.get("currentHp"), 0);
        int remainingHp = Math.max(0, currentHp - recoil);
        attacker.put("currentHp", remainingHp);
        if (remainingHp == 0) {
            attacker.put("status", "fainted");
        }
        actionLog.put("lifeOrbRecoil", recoil);
        events.add(attacker.get("name") + " 受生命宝珠影响损失了 " + recoil + " 点 HP");
    }

    private void applyEndTurnItemEffects(Map<String, Object> state, List<String> events) {
        applyEndTurnStatusEffects(team(state, true), events);
        applyEndTurnStatusEffects(team(state, false), events);
        applyEndTurnHealing(team(state, true), events);
        applyEndTurnHealing(team(state, false), events);
    }

    private void decrementTauntEffects(Map<String, Object> state, List<String> events) {
        decrementTauntEffects(team(state, true), events);
        decrementTauntEffects(team(state, false), events);
    }

    private void decrementTauntEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = tauntTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            mon.put("tauntTurns", after);
            if (after == 0 && toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 不再处于挑衅状态");
            }
        }
    }

    private void applyEndTurnStatusEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (toInt(mon.get("currentHp"), 0) <= 0 || !"burn".equals(mon.get("condition"))) {
                continue;
            }
            int maxHp = toInt(castMap(mon.get("stats")).get("hp"), 1);
            int damage = Math.max(1, maxHp / 16);
            int currentHp = toInt(mon.get("currentHp"), 0);
            int remainingHp = Math.max(0, currentHp - damage);
            mon.put("currentHp", remainingHp);
            if (remainingHp == 0) {
                mon.put("status", "fainted");
                events.add(mon.get("name") + " 因灼伤倒下了");
            } else {
                events.add(mon.get("name") + " 受到灼伤影响，损失了 " + damage + " 点 HP");
            }
        }
    }

    private void applyEndTurnHealing(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (toInt(mon.get("currentHp"), 0) <= 0 || !"leftovers".equals(heldItem(mon))) {
                continue;
            }
            int maxHp = toInt(castMap(mon.get("stats")).get("hp"), 1);
            int currentHp = toInt(mon.get("currentHp"), 0);
            if (currentHp >= maxHp) {
                continue;
            }
            int heal = Math.max(1, maxHp / 16);
            mon.put("currentHp", Math.min(maxHp, currentHp + heal));
            events.add(mon.get("name") + " 通过剩饭回复了 " + heal + " 点 HP");
        }
    }

    private void rememberChoiceMove(Map<String, Object> mon, Map<String, Object> move) {
        String item = heldItem(mon);
        if ("choice-band".equals(item) || "choice-specs".equals(item) || "choice-scarf".equals(item)) {
            mon.put("choiceLockedMove", move.get("name_en"));
        }
    }

    private Map<String, Object> lockedChoiceMove(Map<String, Object> mon, int currentRound) {
        Object locked = mon.get("choiceLockedMove");
        if (locked == null || String.valueOf(locked).isBlank()) {
            return null;
        }
        for (Map<String, Object> move : moves(mon)) {
            if (String.valueOf(move.get("name_en")).equalsIgnoreCase(String.valueOf(locked))
                    && cooldown(mon, move) == 0
                    && canUseMove(mon, move, currentRound)) {
                return move;
            }
        }
        return null;
    }

    private void applyEntryAbilities(Map<String, Object> state, boolean player, List<Integer> previousSlots, List<String> events) {
        List<Integer> currentSlots = activeSlots(state, player);
        List<Map<String, Object>> enteringTeam = team(state, player);
        for (Integer slot : currentSlots) {
            if (previousSlots.contains(slot) || slot == null || slot < 0 || slot >= enteringTeam.size()) {
                continue;
            }
            Map<String, Object> source = enteringTeam.get(slot);
            String ability = abilityName(source);
            if ("intimidate".equalsIgnoreCase(ability)) {
                applyIntimidate(state, player, source, events);
                continue;
            }
            if ("drizzle".equalsIgnoreCase(ability)) {
                activateWeather(state, "rain", source, null, events);
                continue;
            }
            if ("drought".equalsIgnoreCase(ability)) {
                activateWeather(state, "sun", source, null, events);
                continue;
            }
            if ("electric-surge".equalsIgnoreCase(ability) || "electric surge".equalsIgnoreCase(ability)) {
                activateTerrain(state, "electric", source, null, events);
                continue;
            }
            if ("psychic-surge".equalsIgnoreCase(ability) || "psychic surge".equalsIgnoreCase(ability)) {
                activateTerrain(state, "psychic", source, null, events);
            }
        }
    }

    private void applyIntimidate(Map<String, Object> state, boolean player, Map<String, Object> source, List<String> events) {
        List<Map<String, Object>> opposingTeam = team(state, !player);
        boolean activated = false;
        for (Integer targetSlot : activeSlots(state, !player)) {
            if (targetSlot == null || targetSlot < 0 || targetSlot >= opposingTeam.size()) {
                continue;
            }
            Map<String, Object> target = opposingTeam.get(targetSlot);
            if (!isAvailableMon(opposingTeam, targetSlot)) {
                continue;
            }
            if ("clear-amulet".equals(heldItem(target))) {
                events.add(target.get("name") + " 的清净护符挡住了威吓");
                continue;
            }
            int previousStage = statStage(target, "attack");
            int nextStage = Math.max(-6, previousStage - 1);
            statStages(target).put("attack", nextStage);
            if (nextStage != previousStage) {
                events.add(source.get("name") + " 的威吓使 " + target.get("name") + " 的攻击下降了");
                activated = true;
            }
        }
        if (activated) {
            source.put("intimidateActivated", true);
        }
    }

    private void applySpeedDrop(Map<String, Object> state, String actingSide, Map<String, Object> source, Map<String, Object> target,
                                Map<String, Object> actionLog, List<String> events) {
        if ("clear-amulet".equals(heldItem(target))) {
            actionLog.put("speedDropBlocked", true);
            events.add(target.get("name") + " 的清净护符挡住了降速");
            return;
        }
        int previousStage = statStage(target, "speed");
        int nextStage = Math.max(-6, previousStage - 1);
        statStages(target).put("speed", nextStage);
        if (nextStage != previousStage) {
            actionLog.put("speedStageChange", -1);
            events.add(source.get("name") + " 使 " + target.get("name") + " 的速度下降了");
        }
    }

    private int applyStageModifier(int baseStat, int stage) {
        int normalized = Math.max(-6, Math.min(6, stage));
        double multiplier = normalized >= 0
                ? (2.0d + normalized) / 2.0d
                : 2.0d / (2.0d - normalized);
        return Math.max(1, (int) Math.floor(baseStat * multiplier));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> statStages(Map<String, Object> mon) {
        Object value = mon.get("statStages");
        if (value instanceof Map) {
            Map<String, Object> existing = (Map<String, Object>) value;
            existing.putIfAbsent("attack", 0);
            existing.putIfAbsent("speed", 0);
            return existing;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("attack", 0);
        created.put("speed", 0);
        mon.put("statStages", created);
        return created;
    }

    private int statStage(Map<String, Object> mon, String stat) {
        return toInt(statStages(mon).get(stat), 0);
    }

    private void resetBattleStages(Map<String, Object> mon) {
        statStages(mon).put("attack", 0);
        statStages(mon).put("speed", 0);
        mon.put("tauntTurns", 0);
    }

    private int tauntTurns(Map<String, Object> mon) {
        return toInt(mon.get("tauntTurns"), 0);
    }

    private boolean isSleepingThisTurn(Map<String, Object> mon, int currentRound) {
        int appliedRound = toInt(mon.get("sleepAppliedRound"), 0);
        if (appliedRound == currentRound) {
            return true;
        }
        int remaining = Math.max(0, toInt(mon.get("sleepTurns"), 0) - 1);
        mon.put("sleepTurns", remaining);
        return remaining > 0;
    }

    private boolean targetHasType(Map<String, Object> target, int typeId) {
        for (Map<String, Object> type : castList(target.get("types"))) {
            if (toInt(type.get("type_id"), 0) == typeId) {
                return true;
            }
        }
        return false;
    }

    private boolean isPowderImmune(Map<String, Object> mon) {
        return targetHasType(mon, DamageCalculatorUtil.TYPE_GRASS)
                || "safety-goggles".equals(heldItem(mon))
                || "overcoat".equalsIgnoreCase(abilityName(mon));
    }

    private boolean isGrounded(Map<String, Object> mon) {
        return !targetHasType(mon, DamageCalculatorUtil.TYPE_FLYING)
                && !"levitate".equalsIgnoreCase(abilityName(mon));
    }

    private boolean electricTerrainActiveFor(Map<String, Object> mon, Map<String, Object> state) {
        return state != null && electricTerrainTurns(state) > 0 && isGrounded(mon);
    }

    private boolean isBlockedByPsychicTerrain(Map<String, Object> state, String actingSide, Map<String, Object> target, Map<String, Object> move) {
        if (psychicTerrainTurns(state) <= 0 || toInt(move.get("priority"), 0) <= 0 || !isGrounded(target)) {
            return false;
        }
        boolean targetOnPlayerSide = isOnSide(state, target, true);
        return ("player".equals(actingSide) && !targetOnPlayerSide) || ("opponent".equals(actingSide) && targetOnPlayerSide);
    }

    private String powderImmunityMessage(Map<String, Object> mon, Map<String, Object> move) {
        if (targetHasType(mon, DamageCalculatorUtil.TYPE_GRASS)) {
            return mon.get("name") + " 免疫 " + move.get("name");
        }
        if ("safety-goggles".equals(heldItem(mon))) {
            return mon.get("name") + " 的防尘护目镜挡住了 " + move.get("name");
        }
        if ("overcoat".equalsIgnoreCase(abilityName(mon))) {
            return mon.get("name") + " 的防尘使其不受 " + move.get("name") + " 影响";
        }
        return mon.get("name") + " 免疫 " + move.get("name");
    }

    private boolean isOnSide(Map<String, Object> state, Map<String, Object> mon, boolean playerSide) {
        for (Map<String, Object> candidate : team(state, playerSide)) {
            if (candidate == mon) {
                return true;
            }
        }
        return false;
    }

    private boolean isTypeImmune(Map<String, Object> move, Map<String, Object> target) {
        int moveTypeId = toInt(move.get("type_id"), 0);
        if (moveTypeId <= 0) {
            return false;
        }
        for (Map<String, Object> type : castList(target.get("types"))) {
            if (typeFactor(moveTypeId, toInt(type.get("type_id"), 0)) == 0) {
                return true;
            }
        }
        return false;
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

    private boolean isAvailableMon(List<Map<String, Object>> team, int index) {
        return index >= 0 && index < team.size() && toInt(team.get(index).get("currentHp"), 0) > 0;
    }

    private List<Integer> availableBenchIndexes(List<Map<String, Object>> team, List<Integer> activeSlots) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < team.size(); index++) {
            if (isAvailableMon(team, index) && !activeSlots.contains(index)) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private int replacementNeededCount(Map<String, Object> state, boolean player) {
        List<Map<String, Object>> sideTeam = team(state, player);
        int targetActiveCount = Math.min(ACTIVE_SLOTS, aliveCount(sideTeam));
        return Math.max(0, targetActiveCount - activeSlots(state, player).size());
    }

    private List<Integer> autoReplacementIndexes(Map<String, Object> state, boolean player) {
        int needed = replacementNeededCount(state, player);
        List<Integer> available = availableBenchIndexes(team(state, player), activeSlots(state, player));
        return new ArrayList<>(available.subList(0, Math.min(needed, available.size())));
    }

    private void clearReplacementState(Map<String, Object> state) {
        state.put("playerPendingReplacementCount", 0);
        state.put("playerPendingReplacementOptions", new ArrayList<>());
    }

    private Map<String, Object> defaultFieldEffects() {
        Map<String, Object> effects = new LinkedHashMap<>();
        effects.put("playerTailwindTurns", 0);
        effects.put("opponentTailwindTurns", 0);
        effects.put("trickRoomTurns", 0);
        effects.put("rainTurns", 0);
        effects.put("sunTurns", 0);
        effects.put("electricTerrainTurns", 0);
        effects.put("psychicTerrainTurns", 0);
        effects.put("playerReflectTurns", 0);
        effects.put("opponentReflectTurns", 0);
        effects.put("playerLightScreenTurns", 0);
        effects.put("opponentLightScreenTurns", 0);
        return effects;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fieldEffects(Map<String, Object> state) {
        Object value = state.get("fieldEffects");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> created = defaultFieldEffects();
        state.put("fieldEffects", created);
        return created;
    }

    private int tailwindTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerTailwindTurns" : "opponentTailwindTurns"), 0);
    }

    private int trickRoomTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("trickRoomTurns"), 0);
    }

    private int rainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("rainTurns"), 0);
    }

    private int sunTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("sunTurns"), 0);
    }

    private int electricTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("electricTerrainTurns"), 0);
    }

    private int psychicTerrainTurns(Map<String, Object> state) {
        return toInt(fieldEffects(state).get("psychicTerrainTurns"), 0);
    }

    private int terrainTurns(Map<String, Object> state) {
        return Math.max(electricTerrainTurns(state), psychicTerrainTurns(state));
    }

    private int reflectTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerReflectTurns" : "opponentReflectTurns"), 0);
    }

    private int lightScreenTurns(Map<String, Object> state, boolean playerSide) {
        return toInt(fieldEffects(state).get(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns"), 0);
    }

    private void activateTailwind(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffects(state).put(playerSide ? "playerTailwindTurns" : "opponentTailwindTurns", 4);
        actionLog.put("result", "tailwind");
        events.add(actor.get("name") + " 刮起了顺风");
    }

    private void toggleTrickRoom(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        int current = trickRoomTurns(state);
        if (current > 0) {
            fieldEffects(state).put("trickRoomTurns", 0);
            actionLog.put("result", "trick-room-ended");
            events.add(actor.get("name") + " 让戏法空间恢复了正常");
            return;
        }
        fieldEffects(state).put("trickRoomTurns", 5);
        actionLog.put("result", "trick-room");
        events.add(actor.get("name") + " 扭曲了空间，戏法空间生效了");
    }

    private void activateWeather(Map<String, Object> state, String weather, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        Map<String, Object> effects = fieldEffects(state);
        if ("rain".equals(weather)) {
            effects.put("rainTurns", 5);
            effects.put("sunTurns", 0);
            if (actionLog != null) {
                actionLog.put("result", "rain");
            }
            events.add(actor.get("name") + " 让大雨落了下来");
            return;
        }
        effects.put("sunTurns", 5);
        effects.put("rainTurns", 0);
        if (actionLog != null) {
            actionLog.put("result", "sun");
        }
        events.add(actor.get("name") + " 让阳光变得炽烈了");
    }

    private void activateTerrain(Map<String, Object> state, String terrain, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        Map<String, Object> effects = fieldEffects(state);
        effects.put("electricTerrainTurns", 0);
        effects.put("psychicTerrainTurns", 0);
        if ("electric".equals(terrain)) {
            effects.put("electricTerrainTurns", 5);
            if (actionLog != null) {
                actionLog.put("result", "electric-terrain");
            }
            events.add(actor.get("name") + " 让电气场地展开了");
            return;
        }
        effects.put("psychicTerrainTurns", 5);
        if (actionLog != null) {
            actionLog.put("result", "psychic-terrain");
        }
        events.add(actor.get("name") + " 让精神场地展开了");
    }

    private void activateScreen(Map<String, Object> state, String screen, boolean playerSide, Map<String, Object> actor,
                                Map<String, Object> actionLog, List<String> events) {
        if ("reflect".equals(screen)) {
            fieldEffects(state).put(playerSide ? "playerReflectTurns" : "opponentReflectTurns", 5);
            actionLog.put("result", "reflect");
            events.add(actor.get("name") + " 展开了反射壁");
            return;
        }
        fieldEffects(state).put(playerSide ? "playerLightScreenTurns" : "opponentLightScreenTurns", 5);
        actionLog.put("result", "light-screen");
        events.add(actor.get("name") + " 展开了光墙");
    }

    private void decrementFieldEffects(Map<String, Object> state, Map<String, Object> fieldSnapshot, List<String> events) {
        decrementFieldEffect(state, fieldSnapshot, "playerTailwindTurns", "我方顺风结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentTailwindTurns", "对手顺风结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "trickRoomTurns", "戏法空间结束了", events);
        decrementFieldEffect(state, fieldSnapshot, "rainTurns", "雨停了", events);
        decrementFieldEffect(state, fieldSnapshot, "sunTurns", "炽烈的阳光消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "electricTerrainTurns", "电气场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "psychicTerrainTurns", "精神场地消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "playerReflectTurns", "我方反射壁消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentReflectTurns", "对手反射壁消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "playerLightScreenTurns", "我方光墙消失了", events);
        decrementFieldEffect(state, fieldSnapshot, "opponentLightScreenTurns", "对手光墙消失了", events);
    }

    private void clearFlinch(List<Map<String, Object>> team) {
        for (Map<String, Object> mon : team) {
            mon.put("flinched", false);
        }
    }

    private void decrementFieldEffect(Map<String, Object> state, Map<String, Object> fieldSnapshot, String key, String endMessage, List<String> events) {
        int before = toInt(fieldSnapshot.get(key), 0);
        if (before <= 0) {
            return;
        }
        int after = Math.max(0, before - 1);
        fieldEffects(state).put(key, after);
        if (after == 0) {
            events.add(endMessage);
        }
    }

    private double weatherDamageModifier(Map<String, Object> state, int moveTypeId) {
        if (rainTurns(state) > 0) {
            if (moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
                return 1.5d;
            }
            if (moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
                return 0.5d;
            }
        }
        if (sunTurns(state) > 0) {
            if (moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
                return 1.5d;
            }
            if (moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
                return 0.5d;
            }
        }
        return 1.0d;
    }

    private double terrainDamageModifier(Map<String, Object> state, Map<String, Object> attacker, int moveTypeId) {
        if (!isGrounded(attacker)) {
            return 1.0d;
        }
        if (electricTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            return 1.3d;
        }
        if (psychicTerrainTurns(state) > 0 && moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC) {
            return 1.3d;
        }
        return 1.0d;
    }

    private double screenDamageModifier(Map<String, Object> state, Map<String, Object> defender, int damageClassId) {
        boolean playerSide = isOnSide(state, defender, true);
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL && reflectTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        if (damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL && lightScreenTurns(state, playerSide) > 0) {
            return 2.0d / 3.0d;
        }
        return 1.0d;
    }

    private void sortActions(List<Action> actions, boolean trickRoomActive) {
        actions.sort((left, right) -> {
            int byPriority = Integer.compare(right.priority(), left.priority());
            if (byPriority != 0) {
                return byPriority;
            }
            int bySpeed = trickRoomActive
                    ? Integer.compare(left.speed(), right.speed())
                    : Integer.compare(right.speed(), left.speed());
            if (bySpeed != 0) {
                return bySpeed;
            }
            int bySide = left.side().compareTo(right.side());
            if (bySide != 0) {
                return bySide;
            }
            return Integer.compare(left.actorIndex(), right.actorIndex());
        });
    }

    private void replaceActiveSlot(Map<String, Object> state, boolean player, int fieldSlot, int switchToIndex) {
        List<Integer> slots = new ArrayList<>(activeSlots(state, player));
        if (fieldSlot >= 0 && fieldSlot < slots.size()) {
            slots.set(fieldSlot, switchToIndex);
            state.put(player ? "playerActiveSlots" : "opponentActiveSlots", slots);
        }
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

    private record Action(String side, String actionType, int actorIndex, int actorFieldSlot, int targetTeamIndex, int targetFieldSlot, int switchToTeamIndex, Map<String, Object> move, int speed) {
        static Action moveAction(String side, int actorIndex, int actorFieldSlot, int targetTeamIndex, int targetFieldSlot, Map<String, Object> move, int speed) {
            return new Action(side, "move", actorIndex, actorFieldSlot, targetTeamIndex, targetFieldSlot, -1, move, speed);
        }

        static Action switchAction(String side, int actorIndex, int actorFieldSlot, int switchToTeamIndex, int speed) {
            return new Action(side, "switch", actorIndex, actorFieldSlot, -1, -1, switchToTeamIndex, null, speed);
        }

        int priority() {
            if (isSwitch()) {
                return 6;
            }
            return move == null ? 0 : ((Number) move.getOrDefault("priority", 0)).intValue();
        }

        boolean isSwitch() {
            return "switch".equals(actionType);
        }
    }

    private record RedirectionEffect(int actorIndex, boolean powder) {
    }

    private record TargetRef(String side, boolean playerSide, int teamIndex, int fieldSlot) {
    }
}
