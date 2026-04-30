package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    private final SkillService skillService;
    private final BattleStateSupport stateSupport;
    private final BattlePreviewSupport previewSupport;
    private final BattleFieldEffectSupport fieldEffectSupport;
    private final BattleAnalysisSupport analysisSupport;
    private final BattleAISupport aiSupport;
    private final BattleAiSwitchSupport aiSwitchSupport;
    private final BattleActionBuilder actionBuilder;
    private final BattleTargetSupport targetSupport;
    private final BattleDecisionSupport decisionSupport;
    private final BattleDamageSupport damageSupport;
    private final BattleConditionSupport conditionSupport;
    private final BattleFlowSupport flowSupport;
    private final BattleSetupSupport setupSupport;
    private final BattleRoundSupport roundSupport;
    private final BattleTurnCleanupSupport turnCleanupSupport;

    public BattleEngine(SkillService skillService, TypeEfficacyMapper typeEfficacyMapper, ObjectMapper mapper) {
        this.skillService = skillService;
        this.stateSupport = new BattleStateSupport();
        this.previewSupport = new BattlePreviewSupport(mapper, stateSupport, BATTLE_TEAM_SIZE, ACTIVE_SLOTS);
        this.fieldEffectSupport = new BattleFieldEffectSupport();
        this.analysisSupport = new BattleAnalysisSupport(this);
        this.aiSupport = new BattleAISupport(this, analysisSupport);
        this.aiSwitchSupport = new BattleAiSwitchSupport(this);
        this.actionBuilder = new BattleActionBuilder(this, aiSwitchSupport);
        this.targetSupport = new BattleTargetSupport(this);
        this.decisionSupport = new BattleDecisionSupport(this, aiSupport);
        this.damageSupport = new BattleDamageSupport(this, typeEfficacyMapper, fieldEffectSupport, LEVEL);
        this.conditionSupport = new BattleConditionSupport(this, damageSupport, fieldEffectSupport);
        this.flowSupport = new BattleFlowSupport(this, conditionSupport, ACTIVE_SLOTS);
        this.setupSupport = new BattleSetupSupport(previewSupport, stateSupport, fieldEffectSupport, conditionSupport,
                flowSupport, LEVEL, BATTLE_TEAM_SIZE);
        this.roundSupport = new BattleRoundSupport(this, conditionSupport, targetSupport);
        this.turnCleanupSupport = new BattleTurnCleanupSupport(this, fieldEffectSupport, conditionSupport);
    }

    /**
     * 创建“队伍预览阶段”的初始状态。
     *
     * @param playerTeamJson   玩家 6 人队伍 JSON
     * @param opponentTeamJson 对手 6 人队伍 JSON
     * @param maxRounds        最大回合上限
     * @param seed             随机种子（影响后续随机行为）
     * @return 可直接用于 preview UI 的状态对象
     */
    public Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds,
            long seed) {
        return setupSupport.createPreviewState(playerTeamJson, opponentTeamJson, maxRounds, seed);
    }

    /**
     * 直接创建可进入战斗的状态。
     * <p>
     * 该方法会在内部自动完成 6 选 4 和首发选择，主要用于异步自动模拟。
     * 调用方无需再执行 preview 确认流程。
     * </p>
     *
     * @param playerTeamJson   玩家队伍 JSON
     * @param opponentTeamJson 对手队伍 JSON
     * @param maxRounds        最大回合上限
     * @param seed             随机种子
     * @return 处于 running/battle 阶段的状态对象
     */
    public Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds,
            long seed) {
        return setupSupport.createBattleState(playerTeamJson, opponentTeamJson, maxRounds, seed);
    }

    /**
     * 应用队伍预览阶段的选择，并把状态切到 running/battle。
     */
    public Map<String, Object> applyTeamPreviewSelection(Map<String, Object> rawState,
            Map<String, Object> playerSelectionInput, Map<String, Object> opponentSelectionInput) {
        return setupSupport.applyTeamPreviewSelection(rawState, playerSelectionInput, opponentSelectionInput);
    }

    /**
     * 推进一整个回合。
     * <p>
     * 核心流程：预处理 -> 收集动作 -> 顺序修正 -> 执行动作 -> 回合结算 -> 结果刷新。
     * 该方法返回新状态副本，不直接修改调用方传入的 rawState 引用。
     * </p>
     *
     * @param rawState      旧状态
     * @param playerMoveMap 玩家动作映射（可为空）
     * @return 已推进 1 回合后的新状态；若状态不在 running/battle 则原样返回
     */
    public Map<String, Object> playRound(Map<String, Object> rawState, Map<String, String> playerMoveMap) {
        Map<String, Object> state = cloneState(rawState);
        if (!"running".equals(state.get("status")) || !"battle".equals(state.getOrDefault("phase", "battle"))) {
            return state;
        }

        // 进入新回合前，先处理跨回合残留状态：技能冷却、空槽修剪等。
        decrementCooldowns(team(state, true));
        decrementCooldowns(team(state, false));
        flowSupport.pruneActiveSlots(state);

        int round = toInt(state.get("currentRound"), 0) + 1;
        state.put("currentRound", round);
        Random random = new Random(toLong(state.get("seed"), 0L) + (round * 97L));
        Map<String, Object> fieldSnapshot = cloneMap(fieldEffects(state));

        Map<String, Boolean> protectedTargets = new HashMap<>();
        Map<String, Boolean> wideGuardSides = new HashMap<>();
        Map<String, Boolean> quickGuardSides = new HashMap<>();
        Map<String, RedirectionEffect> redirectionTargets = new HashMap<>();
        Map<Map<String, Object>, Boolean> helpingHandBoosts = new IdentityHashMap<>();
        List<Action> actions = new ArrayList<>();
        // 先收集双方本回合声明动作，再统一套用顺序层修正，避免先后手判定分散在多个模块中。
        actions.addAll(buildPlayerActions(state, playerMoveMap));
        actions.addAll(buildOpponentActions(state, random));
        applyActionOrderEffects(state, actions, random);
        Map<String, Action> plannedActions = new HashMap<>();
        for (Action action : actions) {
            plannedActions.put(actionKey(action.side(), action.actorIndex()), action);
        }
        sortActions(actions, trickRoomTurns(state) > 0, random);

        Map<String, Object> roundLog = new LinkedHashMap<>();
        roundLog.put("round", round);
        List<String> events = new ArrayList<>();
        List<Map<String, Object>> actionLogs = new ArrayList<>();

        for (Action action : actions) {
            roundSupport.processAction(state, action, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                    redirectionTargets,
                    plannedActions,
                    helpingHandBoosts, actionLogs, events);
        }

        turnCleanupSupport.applyEndTurnEffects(state, fieldSnapshot, events, random, round);
        flowSupport.prepareReplacementPhase(state, events);
        turnCleanupSupport.clearFlinch(state);
        roundLog.put("actions", actionLogs);
        roundLog.put("events", events);
        roundLog.put("playerActive", flowSupport.activeNames(state, true));
        roundLog.put("opponentActive", flowSupport.activeNames(state, false));
        rounds(state).add(roundLog);
        state.put("roundsCount", rounds(state).size());

        flowSupport.resolveBattleResult(state);
        flowSupport.refreshDerivedState(state);
        return state;
    }

    /**
     * 自动把一场战斗从当前状态推进到结束。
     * <p>
     * 如果中途进入 replacement 阶段，会自动替玩家补位后继续推进。
     * 该方法用于离线模拟，不适合需要逐回合交互的前端链路。
     * </p>
     *
     * @param rawState      当前状态
     * @param playerMoveMap 玩家动作策略（为空时由决策模块兜底）
     * @return 终局状态（status=completed）
     */
    public Map<String, Object> autoPlay(Map<String, Object> rawState, Map<String, String> playerMoveMap) {
        Map<String, Object> state = cloneState(rawState);
        while ("running".equals(state.get("status"))) {
            if ("replacement".equals(state.get("phase"))) {
                state = applyReplacementSelection(state,
                        Map.of("replacementIndexes", flowSupport.autoReplacementIndexes(state, true)));
                continue;
            }
            state = playRound(state, playerMoveMap);
        }
        return state;
    }

    /**
     * 应用玩家补位选择。
     */
    public Map<String, Object> applyReplacementSelection(Map<String, Object> rawState,
            Map<String, Object> selectionInput) {
        return setupSupport.applyReplacementSelection(rawState, selectionInput);
    }

    /**
     * 直接执行一场完整自动模拟。
     *
     * @param playerTeamJson   玩家队伍 JSON
     * @param opponentTeamJson 对手队伍 JSON
     * @param maxRounds        最大回合上限
     * @param playerMoveMap    玩家动作策略
     * @return 完整战斗结果
     */
    public Map<String, Object> simulate(String playerTeamJson, String opponentTeamJson, int maxRounds,
            Map<String, String> playerMoveMap) {
        long seed = Math.abs((playerTeamJson + opponentTeamJson).hashCode()) + maxRounds;
        return autoPlay(createBattleState(playerTeamJson, opponentTeamJson, maxRounds, seed), playerMoveMap);
    }

    /**
     * 在胜利交换奖励后，用新成员替换玩家原队伍中的一名成员。
     */
    public Map<String, Object> replacePlayerTeamMember(Map<String, Object> rawState, int replacedIndex,
            Map<String, Object> newMember) {
        return setupSupport.replacePlayerTeamMember(rawState, replacedIndex, newMember);
    }

    private List<Action> buildPlayerActions(Map<String, Object> state, Map<String, String> playerMoveMap) {
        return actionBuilder.buildPlayerActions(state, playerMoveMap);
    }

    private List<Action> buildOpponentActions(Map<String, Object> state, Random random) {
        return actionBuilder.buildOpponentActions(state, random);
    }

    Map<String, Object> selectPlayerMove(Map<String, Object> mon, Map<String, String> playerMoveMap, int fieldSlot,
            int currentRound) {
        return decisionSupport.selectPlayerMove(mon, playerMoveMap, fieldSlot, currentRound);
    }

    boolean canSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int actorFieldSlot,
            int switchToIndex) {
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

    int firstAvailableBench(List<Map<String, Object>> team, List<Integer> activeSlots) {
        for (int index = 0; index < team.size(); index++) {
            if (isAvailableMon(team, index) && !activeSlots.contains(index)) {
                return index;
            }
        }
        return -1;
    }

    Map<String, Object> selectAIMove(Map<String, Object> mon, Random random, Map<String, Object> state,
            boolean playerSide, int currentRound) {
        return decisionSupport.selectAIMove(mon, random, state, playerSide, currentRound);
    }

    Map<String, Object> defaultMoveSelection(Map<String, Object> mon, int currentRound) {
        return decisionSupport.defaultMoveSelection(mon, currentRound);
    }

    int calculateDamage(Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move,
            Random random,
            Map<Map<String, Object>, Boolean> helpingHandBoosts, Map<String, Object> state) {
        return damageSupport.calculateDamage(attacker, defender, move, random, helpingHandBoosts, state);
    }

    int typeFactor(int attackingTypeId, int defendingTypeId) {
        return damageSupport.typeFactor(attackingTypeId, defendingTypeId);
    }

    private double typeModifier(Map<String, Object> defender, int moveTypeId) {
        return damageSupport.typeModifier(defender, moveTypeId);
    }

    void applyCooldown(Map<String, Object> mon, Map<String, Object> move) {
        if (MoveRegistry.isProtectionMove(move)) {
            return;
        }
        int cooldown = skillService.getCooldown(String.valueOf(move.get("name_en")), 0);
        if (cooldown > 0) {
            cooldowns(mon).put(String.valueOf(move.get("name_en")), cooldown + 1);
        }
    }

    // Delegating to MoveRegistry for move classification

    // BattleEngine-specific helper: check if healing move (based on healing
    // property)
    boolean isHealingMove(Map<String, Object> move) {
        return MoveRegistry.isHealingMove(move);
    }

    // BattleEngine-specific helper: check if status move
    boolean isStatusMove(Map<String, Object> move) {
        return MoveRegistry.isStatusMove(move);
    }

    // BattleEngine-specific helper: check if redirection move
    boolean isRedirectionMove(Map<String, Object> move) {
        return MoveRegistry.isRedirectionMove(move);
    }

    // BattleEngine-specific helper: check if helping hand
    boolean isHelpingHand(Map<String, Object> move) {
        return MoveRegistry.isHelpingHand(move);
    }

    // BattleEngine-specific helper: check if ally switch
    boolean isAllySwitch(Map<String, Object> move) {
        return MoveRegistry.isAllySwitch(move);
    }

    // Delegating to MoveRegistry for field/weather/terrain moves
    boolean isTailwind(Map<String, Object> move) {
        return MoveRegistry.isTailwind(move);
    }

    boolean isTrickRoom(Map<String, Object> move) {
        return MoveRegistry.isTrickRoom(move);
    }

    boolean isRainDance(Map<String, Object> move) {
        return MoveRegistry.isRainDance(move);
    }

    boolean isSunnyDay(Map<String, Object> move) {
        return MoveRegistry.isSunnyDay(move);
    }

    boolean isSandstorm(Map<String, Object> move) {
        return MoveRegistry.isSandstorm(move);
    }

    boolean isSnowWeather(Map<String, Object> move) {
        return MoveRegistry.isSnowWeather(move);
    }

    boolean isElectricTerrain(Map<String, Object> move) {
        return MoveRegistry.isElectricTerrain(move);
    }

    boolean isPsychicTerrain(Map<String, Object> move) {
        return MoveRegistry.isPsychicTerrain(move);
    }

    boolean isGrassyTerrain(Map<String, Object> move) {
        return MoveRegistry.isGrassyTerrain(move);
    }

    boolean isMistyTerrain(Map<String, Object> move) {
        return MoveRegistry.isMistyTerrain(move);
    }

    // Delegating to MoveRegistry for stat-lowering moves
    boolean isIcyWind(Map<String, Object> move) {
        return MoveRegistry.isIcyWind(move);
    }

    boolean isThunderWave(Map<String, Object> move) {
        return MoveRegistry.isThunderWave(move);
    }

    boolean isWillOWisp(Map<String, Object> move) {
        return MoveRegistry.isWillOWisp(move);
    }

    boolean isElectroweb(Map<String, Object> move) {
        return MoveRegistry.isElectroweb(move);
    }

    boolean isSnarl(Map<String, Object> move) {
        return MoveRegistry.isSnarl(move);
    }

    boolean isFakeTears(Map<String, Object> move) {
        return MoveRegistry.isFakeTears(move);
    }

    boolean isLeechSeed(Map<String, Object> move) {
        return MoveRegistry.isLeechSeed(move);
    }

    boolean isSubstitute(Map<String, Object> move) {
        return MoveRegistry.isSubstitute(move);
    }

    boolean isAttract(Map<String, Object> move) {
        return MoveRegistry.isAttract(move);
    }

    boolean isPerishSong(Map<String, Object> move) {
        return MoveRegistry.isPerishSong(move);
    }

    boolean isRapidSpin(Map<String, Object> move) {
        return MoveRegistry.isRapidSpin(move);
    }

    boolean isGrassyGlide(Map<String, Object> move) {
        return "grassy-glide".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "grassy glide".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    boolean isDefog(Map<String, Object> move) {
        return MoveRegistry.isDefog(move);
    }

    // Delegating to MoveRegistry for pivot/switch moves
    boolean isPartingShot(Map<String, Object> move) {
        return MoveRegistry.isPartingShot(move);
    }

    boolean isUTurn(Map<String, Object> move) {
        return MoveRegistry.isUTurn(move);
    }

    boolean isVoltSwitch(Map<String, Object> move) {
        return MoveRegistry.isVoltSwitch(move);
    }

    boolean isFlipTurn(Map<String, Object> move) {
        return MoveRegistry.isFlipTurn(move);
    }

    boolean isPivotSwitchMove(Map<String, Object> move) {
        return MoveRegistry.isPivotSwitchMove(move);
    }

    // Delegating to MoveRegistry for special moves
    boolean isTeraBlast(Map<String, Object> move) {
        return MoveRegistry.isTeraBlast(move);
    }

    boolean isSuckerPunch(Map<String, Object> move) {
        return MoveRegistry.isSuckerPunch(move);
    }

    // Delegating to MoveRegistry for self-boosting moves
    boolean isSwordsDance(Map<String, Object> move) {
        return MoveRegistry.isSwordsDance(move);
    }

    boolean isNastyPlot(Map<String, Object> move) {
        return MoveRegistry.isNastyPlot(move);
    }

    boolean isDragonDance(Map<String, Object> move) {
        return MoveRegistry.isDragonDance(move);
    }

    boolean isCalmMind(Map<String, Object> move) {
        return MoveRegistry.isCalmMind(move);
    }

    boolean isAgility(Map<String, Object> move) {
        return MoveRegistry.isAgility(move);
    }

    boolean isAutotomize(Map<String, Object> move) {
        return MoveRegistry.isAutotomize(move);
    }

    boolean isBulkUp(Map<String, Object> move) {
        return MoveRegistry.isBulkUp(move);
    }

    boolean isWorkUp(Map<String, Object> move) {
        return MoveRegistry.isWorkUp(move);
    }

    boolean isQuiverDance(Map<String, Object> move) {
        return MoveRegistry.isQuiverDance(move);
    }

    boolean isCoil(Map<String, Object> move) {
        return MoveRegistry.isCoil(move);
    }

    boolean isShellSmash(Map<String, Object> move) {
        return MoveRegistry.isShellSmash(move);
    }

    // Delegating to MoveRegistry for entry hazard moves
    boolean isStealthRock(Map<String, Object> move) {
        return MoveRegistry.isStealthRock(move);
    }

    boolean isSpikes(Map<String, Object> move) {
        return MoveRegistry.isSpikes(move);
    }

    boolean isToxicSpikes(Map<String, Object> move) {
        return MoveRegistry.isToxicSpikes(move);
    }

    boolean isStickyWeb(Map<String, Object> move) {
        return MoveRegistry.isStickyWeb(move);
    }

    // Delegating to MoveRegistry for recovery moves
    boolean isRecover(Map<String, Object> move) {
        return MoveRegistry.isRecover(move);
    }

    boolean isRoost(Map<String, Object> move) {
        return MoveRegistry.isRoost(move);
    }

    boolean isRest(Map<String, Object> move) {
        return MoveRegistry.isRest(move);
    }

    boolean isSoftBoiled(Map<String, Object> move) {
        return MoveRegistry.isSoftBoiled(move);
    }

    boolean isMilkDrink(Map<String, Object> move) {
        return MoveRegistry.isMilkDrink(move);
    }

    boolean isSynthesis(Map<String, Object> move) {
        return MoveRegistry.isSynthesis(move);
    }

    boolean isMoonlight(Map<String, Object> move) {
        return MoveRegistry.isMoonlight(move);
    }

    boolean isMorningSun(Map<String, Object> move) {
        return MoveRegistry.isMorningSun(move);
    }

    // Delegating to MoveRegistry for recharge/charge moves
    boolean isRechargeMove(Map<String, Object> move) {
        return MoveRegistry.isRechargeMove(move);
    }

    boolean isChargeMove(Map<String, Object> move) {
        return MoveRegistry.isChargeMove(move);
    }

    // Delegating to MoveRegistry for spread moves
    boolean isSpreadMove(Map<String, Object> move) {
        return MoveRegistry.isSpreadMove(move);
    }

    // Delegating to MoveRegistry for Fake Out (used in canUseMove)
    private boolean isFakeOut(Map<String, Object> move) {
        return MoveRegistry.isFakeOut(move);
    }

    // Delegating to MoveRegistry for Taunt (used in canUseMove)
    boolean isTaunt(Map<String, Object> move) {
        return MoveRegistry.isTaunt(move);
    }

    // Delegating to MoveRegistry for Disable (used in canUseMove)
    boolean isDisable(Map<String, Object> move) {
        return MoveRegistry.isDisable(move);
    }

    // Delegating to MoveRegistry for Torment (used in canUseMove)
    boolean isTorment(Map<String, Object> move) {
        return MoveRegistry.isTorment(move);
    }

    // Delegating to MoveRegistry for Heal Block (used in applyDefenderItemEffects)
    boolean isHealBlock(Map<String, Object> move) {
        return MoveRegistry.isHealBlock(move);
    }

    boolean isTrappingMove(Map<String, Object> move) {
        return MoveRegistry.isTrappingMove(move);
    }

    // Delegating to MoveRegistry for Encore (used by other components)
    boolean isEncore(Map<String, Object> move) {
        return MoveRegistry.isEncore(move);
    }

    // Delegating to MoveRegistry for Reflect/Light Screen/Aurora Veil/Safeguard
    boolean isReflect(Map<String, Object> move) {
        return MoveRegistry.isReflect(move);
    }

    boolean isLightScreen(Map<String, Object> move) {
        return MoveRegistry.isLightScreen(move);
    }

    boolean isAuroraVeil(Map<String, Object> move) {
        return MoveRegistry.isAuroraVeil(move);
    }

    boolean isSafeguard(Map<String, Object> move) {
        return MoveRegistry.isSafeguard(move);
    }

    // Delegating to MoveRegistry for status-inflicting moves
    boolean isSpore(Map<String, Object> move) {
        return MoveRegistry.isSpore(move);
    }

    boolean isYawn(Map<String, Object> move) {
        return MoveRegistry.isYawn(move);
    }

    boolean isToxic(Map<String, Object> move) {
        return MoveRegistry.isToxic(move);
    }

    boolean isPoisonPowder(Map<String, Object> move) {
        return MoveRegistry.isPoisonPowder(move);
    }

    boolean isConfuseRay(Map<String, Object> move) {
        return MoveRegistry.isConfuseRay(move);
    }

    // Delegating to MoveRegistry for Feint and Knock Off
    boolean isFeint(Map<String, Object> move) {
        return MoveRegistry.isFeint(move);
    }

    boolean isKnockOff(Map<String, Object> move) {
        return MoveRegistry.isKnockOff(move);
    }

    boolean isContactMove(Map<String, Object> move) {
        return MoveRegistry.isContactMove(move);
    }

    boolean canUseMove(Map<String, Object> mon, Map<String, Object> move, int currentRound) {
        String item = heldItem(mon);
        // 统一通过 volatile 访问器读取控制类状态，确保与旧字段保持同步。
        if (healBlockTurns(mon) > 0 && isHealingMove(move)) {
            return false;
        }
        Object lastMoveUsed = mon.get("lastMoveUsed");
        if (tormentTurns(mon) > 0
                && lastMoveUsed != null
                && !String.valueOf(lastMoveUsed).isBlank()
                && String.valueOf(move.get("name_en")).equalsIgnoreCase(String.valueOf(lastMoveUsed))) {
            return false;
        }
        Object disabledMove = disableMove(mon);
        if (disableTurns(mon) > 0
                && disabledMove != null
                && !String.valueOf(disabledMove).isBlank()
                && String.valueOf(move.get("name_en")).equalsIgnoreCase(String.valueOf(disabledMove))) {
            return false;
        }
        Object encoredMove = encoreMove(mon);
        if (encoreTurns(mon) > 0
                && encoredMove != null
                && !String.valueOf(encoredMove).isBlank()
                && !String.valueOf(move.get("name_en")).equalsIgnoreCase(String.valueOf(encoredMove))) {
            return false;
        }
        if (toInt(mon.get("rechargeTurns"), 0) > 0) {
            return false;
        }
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

    Map<String, Object> withEffectivePriority(Map<String, Object> mon, Map<String, Object> move) {
        int effectivePriority = effectivePriority(mon, move);
        int currentPriority = toInt(move.get("priority"), 0);
        if (effectivePriority == currentPriority) {
            return move;
        }
        Map<String, Object> copied = cloneMap(move);
        copied.put("priority", effectivePriority);
        copied.put("pranksterBoosted", effectivePriority > currentPriority);
        return copied;
    }

    int effectivePriority(Map<String, Object> mon, Map<String, Object> move) {
        int priority = toInt(move.get("priority"), 0);

        // Prankster: Status moves get +1 priority
        if (priority >= 0 && isStatusMove(move) && "prankster".equalsIgnoreCase(abilityName(mon))) {
            priority += 1;
        }

        // Gale Wings: Flying-type moves get +1 priority when at full HP (Gen 7+)
        if ("gale-wings".equalsIgnoreCase(abilityName(mon)) &&
                toInt(move.get("type_id"), 0) == DamageCalculatorUtil.TYPE_FLYING &&
                priority >= 0) {
            int currentHp = toInt(mon.get("currentHp"), 0);
            int maxHp = toInt(castMap(mon.get("stats")).get("hp"), 1);
            if (currentHp >= maxHp) {
                priority += 1;
            }
        }

        // Triage: Healing moves get +3 priority
        if ("triage".equalsIgnoreCase(abilityName(mon)) && isHealingMove(move) && priority >= 0) {
            priority += 3;
        }

        // Mycelium Might: status moves go last in priority bracket
        if ("mycelium-might".equalsIgnoreCase(abilityName(mon)) || "mycelium might".equalsIgnoreCase(abilityName(mon))) {
            if (isStatusMove(move)) priority -= 1;
        }

        // Grassy Glide: +1 priority in Grassy Terrain (requires state; handled in buildPlayerActions)
        // Note: full implementation needs grassy terrain check which requires state

        // Note: Quick Claw and Custap Berry provide random priority boosts
        // They are handled separately in action building, not here

        return priority;
    }

    int targetIndex(Map<String, Object> state, boolean playerTarget, int targetFieldSlot) {
        return targetSupport.targetIndex(state, playerTarget, targetFieldSlot);
    }

    String protectionKey(String side, int index) {
        return side + "-" + index;
    }

    String sideName(String side) {
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

    private Map<String, Object> cloneState(Map<String, Object> state) {
        return stateSupport.cloneState(state);
    }

    Map<String, Object> castMap(Object value) {
        return stateSupport.castMap(value);
    }

    List<Map<String, Object>> castList(Object value) {
        return stateSupport.castList(value);
    }

    private Map<String, Object> cloneMap(Map<String, Object> value) {
        return stateSupport.cloneMap(value);
    }

    List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return stateSupport.team(state, player);
    }

    private List<Map<String, Object>> rounds(Map<String, Object> state) {
        return stateSupport.rounds(state);
    }

    List<Integer> activeSlots(Map<String, Object> state, boolean player) {
        return stateSupport.activeSlots(state, player);
    }

    List<Map<String, Object>> moves(Map<String, Object> mon) {
        return stateSupport.moves(mon);
    }

    private Map<String, Object> cooldowns(Map<String, Object> mon) {
        return stateSupport.cooldowns(mon);
    }

    int cooldown(Map<String, Object> mon, Map<String, Object> move) {
        return toInt(cooldowns(mon).get(move.get("name_en")), 0);
    }

    int speedValue(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        return damageSupport.speedValue(mon, state, playerSide);
    }

    String heldItem(Map<String, Object> mon) {
        Object item = mon.get("heldItem");
        return item == null ? "" : String.valueOf(item);
    }

    String abilityName(Map<String, Object> mon) {
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

    boolean hasAbility(Map<String, Object> mon, String... names) {
        String ability = abilityName(mon);
        for (String name : names) {
            if (name.equalsIgnoreCase(ability)) {
                return true;
            }
        }
        return false;
    }

    boolean isMagicGuard(Map<String, Object> mon) {
        return hasAbility(mon, "magic-guard", "magic guard");
    }

    boolean itemConsumed(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("itemConsumed"));
    }

    void consumeItem(Map<String, Object> mon) {
        String previousItem = heldItem(mon);
        if (Boolean.TRUE.equals(mon.get("itemConsumed"))) {
            return; // already consumed
        }
        mon.put("itemConsumed", true);
        // Cud Chew: berry will be consumed again at end of next turn
        if (hasAbility(mon, "cud-chew", "cud chew") && isBerry(previousItem)) {
            mon.put("cudChewPending", true);
        }
        if (!previousItem.isBlank() && hasAbility(mon, "unburden")) {
            mon.put("unburdenActive", true);
        }
    }

    boolean isCudChewPending(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("cudChewPending"));
    }

    boolean isBerry(String item) {
        return item != null && item.endsWith("-berry");
    }

    void removeHeldItem(Map<String, Object> mon) {
        String item = heldItem(mon);
        if (item.isBlank()) {
            return;
        }
        mon.put("heldItem", "");
        if (hasAbility(mon, "unburden")) {
            mon.put("unburdenActive", true);
        }
        if ("choice-band".equals(item) || "choice-specs".equals(item) || "choice-scarf".equals(item)) {
            mon.put("choiceLockedMove", null);
        }
    }

    int applyIncomingDamage(Map<String, Object> attacker, Map<String, Object> target, int damage,
            Map<String, Object> actionLog, List<String> events) {
        return damageSupport.applyIncomingDamage(attacker, target, damage, actionLog, events);
    }

    boolean rollCriticalHit(Map<String, Object> attacker, Map<String, Object> move, Random random) {
        return damageSupport.calculateCriticalHitChance(attacker, move, random);
    }

    void applyDefenderItemEffects(Map<String, Object> target, Map<String, Object> move, int actualDamage,
            Map<String, Object> actionLog, List<String> events) {
        if (toInt(target.get("currentHp"), 0) <= 0)
            return;

        String item = heldItem(target);

        // Air Balloon pops when holder takes damage
        if ("air-balloon".equals(item) && actualDamage > 0) {
            consumeItem(target);
            actionLog.put("airBalloonPopped", true);
            events.add(target.get("name") + " 的气球被打破了");
        }

        // Eject Button: holder switches out when hit
        if ("eject-button".equals(item) && actualDamage > 0 && !itemConsumed(target)) {
            actionLog.put("ejectButton", true);
            events.add(target.get("name") + " 的逃脱按钮被触发");
        }

        // Red Card: forces attacker to switch when holder is hit
        if ("red-card".equals(item) && actualDamage > 0 && !itemConsumed(target)) {
            consumeItem(target);
            actionLog.put("redCard", true);
            events.add(target.get("name") + " 的红牌迫使对方交换宝可梦");
        }

        // Weakness Policy
        if ("weakness-policy".equals(item) && !itemConsumed(target) && actualDamage > 0
                && typeModifier(target, toInt(move.get("type_id"), 0)) > 1.0d) {
            int attackStage = Math.min(6, statStage(target, "attack") + 2);
            int specialAttackStage = Math.min(6, statStage(target, "specialAttack") + 2);
            statStages(target).put("attack", attackStage);
            statStages(target).put("specialAttack", specialAttackStage);
            consumeItem(target);
            actionLog.put("weaknessPolicy", true);
            events.add(target.get("name") + " 的弱点保险发动了，攻击和特攻大幅提升");
        }

        // Berries (Sitrus, Type-resist)
        if (!itemConsumed(target)) {
            int maxHp = toInt(castMap(target.get("stats")).get("hp"), 1);
            int currentHp = toInt(target.get("currentHp"), 0);

            // Sitrus Berry: Heal 25% when HP drops to 50% or below
            if ("sitrus-berry".equals(item) && currentHp * 2 <= maxHp) {
                if (healBlockTurns(target) > 0) {
                    actionLog.put("berryHealBlocked", true);
                    events.add(target.get("name") + " 受到回复封锁，文柚果无法生效");
                    return;
                }
                int heal = Math.max(1, maxHp / 4);
                target.put("currentHp", Math.min(maxHp, currentHp + heal));
                consumeItem(target);
                actionLog.put("berryHeal", heal);
                events.add(target.get("name") + " 食用了文柚果，回复了 " + heal + " 点 HP");
            }

            // Resist Berries (e.g., Babiri Berry for Steel)
            double resistFactor = getBerryResistFactor(item, toInt(move.get("type_id"), 0));
            if (resistFactor < 1.0 && actualDamage > 0) {
                // Note: In a real engine, we'd need to retroactively adjust the damage taken.
                // For now, we just log the trigger and consume the berry.
                consumeItem(target);
                actionLog.put("berryResist", item);
                events.add(target.get("name") + " 的 " + item + " 削弱了受到的伤害");
            }
        }
    }

    private double getBerryResistFactor(String item, int moveTypeId) {
        // Mapping of berries to their corresponding types
        if (moveTypeId == DamageCalculatorUtil.TYPE_STEEL && "babiri-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_FIRE && "charti-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_WATER && "passho-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC && "wacan-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_GRASS && "rindo-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_ICE && "yache-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_FIGHTING && "chople-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_POISON && "kebia-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_GROUND && "tanga-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_FLYING && "colbur-berry".equals(item))
            return 0.5; // Actually Colbur is Dark, but placeholder
        if (moveTypeId == DamageCalculatorUtil.TYPE_PSYCHIC && "payapa-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_BUG && "tangia-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_ROCK && "haban-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_GHOST && "kasib-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_DRAGON && "haban-berry".equals(item))
            return 0.5; // Haban is Dragon
        if (moveTypeId == DamageCalculatorUtil.TYPE_DARK && "colbur-berry".equals(item))
            return 0.5;
        if (moveTypeId == DamageCalculatorUtil.TYPE_FAIRY && "roseli-berry".equals(item))
            return 0.5;
        return 1.0;
    }

    void applyAttackerItemEffects(Map<String, Object> attacker, int damage, Map<String, Object> actionLog,
            List<String> events) {
        if (!"life-orb".equals(heldItem(attacker)) || damage <= 0 || toInt(attacker.get("currentHp"), 0) <= 0) {
            return;
        }
        // Pokemon Showdown behavior: Magic Guard ignores Life Orb recoil while keeping
        // the power boost.
        if (isMagicGuard(attacker)) {
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

    void rememberChoiceMove(Map<String, Object> mon, Map<String, Object> move) {
        String item = heldItem(mon);
        if ("choice-band".equals(item) || "choice-specs".equals(item) || "choice-scarf".equals(item)) {
            mon.put("choiceLockedMove", move.get("name_en"));
        }
    }

    void rememberLastMove(Map<String, Object> mon, Map<String, Object> move) {
        mon.put("lastMoveUsed", move.get("name_en"));
    }

    Map<String, Object> lockedChoiceMove(Map<String, Object> mon, int currentRound) {
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

    Map<String, Object> statStages(Map<String, Object> mon) {
        return damageSupport.statStages(mon);
    }

    private int statStage(Map<String, Object> mon, String stat) {
        return damageSupport.statStage(mon, stat);
    }

    int tauntTurns(Map<String, Object> mon) {
        // 先读 volatiles，再回退旧字段，兼容历史状态结构。
        return toInt(volatileValue(mon, "tauntTurns", mon.get("tauntTurns")), 0);
    }

    int healBlockTurns(Map<String, Object> mon) {
        return toInt(volatileValue(mon, "healBlockTurns", mon.get("healBlockTurns")), 0);
    }

    int tormentTurns(Map<String, Object> mon) {
        return toInt(volatileValue(mon, "tormentTurns", mon.get("tormentTurns")), 0);
    }

    int disableTurns(Map<String, Object> mon) {
        // Disable 持续回合与被封印招式统一走 volatile，避免部分逻辑仍直接读取旧字段导致不一致。
        return toInt(volatileValue(mon, "disableTurns", mon.get("disableTurns")), 0);
    }

    Object disableMove(Map<String, Object> mon) {
        return volatileValue(mon, "disableMove", mon.get("disableMove"));
    }

    int encoreTurns(Map<String, Object> mon) {
        return toInt(volatileValue(mon, "encoreTurns", mon.get("encoreTurns")), 0);
    }

    Object encoreMove(Map<String, Object> mon) {
        return volatileValue(mon, "encoreMove", mon.get("encoreMove"));
    }

    int yawnTurns(Map<String, Object> mon) {
        return toInt(volatileValue(mon, "yawnTurns", mon.get("yawnTurns")), 0);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> volatiles(Map<String, Object> mon) {
        Object value = mon.get("volatiles");
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        // 兼容旧存档/旧测试：如果尚未初始化 volatiles，则在首次访问时补齐容器。
        Map<String, Object> created = new LinkedHashMap<>();
        mon.put("volatiles", created);
        return created;
    }

    Object volatileValue(Map<String, Object> mon, String key, Object fallback) {
        // volatile 为新标准存储；fallback 为旧字段回退，便于渐进迁移。
        Map<String, Object> volatileState = volatiles(mon);
        return volatileState.containsKey(key) ? volatileState.get(key) : fallback;
    }

    boolean volatileFlag(Map<String, Object> mon, String key) {
        Object fallback = switch (key) {
            case "flinch" -> mon.get("flinched");
            case "confused" -> mon.get("confused");
            default -> false;
        };
        Object raw = volatileValue(mon, key, fallback);
        if (raw instanceof Boolean flag) {
            return flag;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    void setVolatile(Map<String, Object> mon, String key, Object value) {
        // 统一写入口：先写新结构，再把仍被旧逻辑依赖的平铺字段同步回去。
        volatiles(mon).put(key, value);
        if ("flinch".equals(key)) {
            mon.put("flinched", Boolean.TRUE.equals(value));
        } else if ("confused".equals(key)) {
            mon.put("confused", Boolean.TRUE.equals(value));
        } else if ("confusionTurns".equals(key)) {
            mon.put("confusionTurns", toInt(value, 0));
        } else if ("yawnTurns".equals(key)) {
            mon.put("yawnTurns", toInt(value, 0));
        } else if ("tauntTurns".equals(key)) {
            mon.put("tauntTurns", toInt(value, 0));
        } else if ("healBlockTurns".equals(key)) {
            mon.put("healBlockTurns", toInt(value, 0));
        } else if ("tormentTurns".equals(key)) {
            mon.put("tormentTurns", toInt(value, 0));
        } else if ("disableTurns".equals(key)) {
            mon.put("disableTurns", toInt(value, 0));
        } else if ("disableMove".equals(key)) {
            mon.put("disableMove", value);
        } else if ("encoreTurns".equals(key)) {
            mon.put("encoreTurns", toInt(value, 0));
        } else if ("encoreMove".equals(key)) {
            mon.put("encoreMove", value);
		} else if ("leechSeed".equals(key)) {
			mon.put("leechSeed", Boolean.TRUE.equals(value));
		} else if ("infatuated".equals(key)) {
			mon.put("infatuated", Boolean.TRUE.equals(value));
		} else if ("aquaRing".equals(key)) {
			mon.put("aquaRing", Boolean.TRUE.equals(value));
		} else if ("ingrain".equals(key)) {
			mon.put("ingrain", Boolean.TRUE.equals(value));
		} else if ("cursed".equals(key)) {
			mon.put("cursed", Boolean.TRUE.equals(value));
		} else if ("trapped".equals(key)) {
			mon.put("trapped", Boolean.TRUE.equals(value));
		} else if ("iceFaceActive".equals(key)) {
			mon.put("iceFaceActive", Boolean.TRUE.equals(value));
		} else if ("disguiseActive".equals(key)) {
			mon.put("disguiseActive", Boolean.TRUE.equals(value));
		} else if ("bound".equals(key)) {
			mon.put("bound", Boolean.TRUE.equals(value));
        }
    }

    void clearVolatile(Map<String, Object> mon, String key) {
        // 统一清理入口：删除 volatile 后同步重置旧字段，避免双来源状态漂移。
        volatiles(mon).remove(key);
        if ("flinch".equals(key)) {
            mon.put("flinched", false);
        } else if ("confused".equals(key)) {
            mon.put("confused", false);
        } else if ("disableMove".equals(key) || "encoreMove".equals(key)) {
            mon.put(key, null);
        } else if ("confusionTurns".equals(key)
                || "yawnTurns".equals(key)
                || "tauntTurns".equals(key)
                || "healBlockTurns".equals(key)
                || "tormentTurns".equals(key)
                || "disableTurns".equals(key)
                || "encoreTurns".equals(key)) {
            mon.put(key, 0);
        }
    }

    boolean isSleepingThisTurn(Map<String, Object> mon, int currentRound) {
        int appliedRound = toInt(mon.get("sleepAppliedRound"), 0);
        if (appliedRound == currentRound) {
            return true;
        }
        int remaining = Math.max(0, toInt(mon.get("sleepTurns"), 0) - 1);
        mon.put("sleepTurns", remaining);
        return remaining > 0;
    }

    boolean targetHasType(Map<String, Object> target, int typeId) {
        for (Map<String, Object> type : activeTypes(target)) {
            if (toInt(type.get("type_id"), 0) == typeId) {
                return true;
            }
        }
        return false;
    }

    boolean isPowderImmune(Map<String, Object> mon) {
        return targetHasType(mon, DamageCalculatorUtil.TYPE_GRASS)
                || "safety-goggles".equals(heldItem(mon))
                || "overcoat".equalsIgnoreCase(abilityName(mon));
    }

    boolean isGrounded(Map<String, Object> mon) {
        return !targetHasType(mon, DamageCalculatorUtil.TYPE_FLYING)
                && !"levitate".equalsIgnoreCase(abilityName(mon));
    }

    List<Map<String, Object>> activeTypes(Map<String, Object> mon) {
        if (Boolean.TRUE.equals(mon.get("terastallized"))) {
            Map<String, Object> teraType = castMap(mon.get("teraType"));
            if (toInt(teraType.get("type_id"), 0) > 0) {
                return List.of(teraType);
            }
        }
        if (Boolean.TRUE.equals(mon.get("megaEvolved"))) {
            List<Map<String, Object>> megaTypes = castList(mon.get("megaTypes"));
            if (!megaTypes.isEmpty()) {
                return megaTypes;
            }
        }
        return castList(mon.get("types"));
    }

    boolean canTerastallize(Map<String, Object> state, boolean playerSide, Map<String, Object> mon) {
        return canUseSpecialSystem(state, playerSide, mon, "tera", null);
    }

    void activateTerastallization(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            Map<String, Object> actionLog, List<String> events) {
        if (!canUseSpecialSystem(state, playerSide, mon, "tera", null)) {
            return;
        }
        mon.put("terastallized", true);
        mon.put("specialSystemActivated", "tera");
        markSpecialSystemUsed(state, playerSide, "tera");
        Map<String, Object> teraType = castMap(mon.get("teraType"));
        actionLog.put("terastallized", true);
        actionLog.put("teraTypeId", toInt(teraType.get("type_id"), 0));
        String teraName = String.valueOf(teraType.getOrDefault("name", teraType.getOrDefault("name_en", "未知属性")));
        // Embody Aspect: Ogerpon's mask triggers stat boost on tera
        String maskItem = heldItem(mon);
        if ("wellspring-mask".equals(maskItem) || "wellspring mask".equals(maskItem)) {
            statStages(mon).put("specialDefense", Math.min(6, statStage(mon, "specialDefense") + 1));
            events.add(mon.get("name") + " 的面具提升了特防");
        } else if ("hearthflame-mask".equals(maskItem) || "hearthflame mask".equals(maskItem)) {
            statStages(mon).put("attack", Math.min(6, statStage(mon, "attack") + 1));
            events.add(mon.get("name") + " 的面具提升了攻击");
        } else if ("cornerstone-mask".equals(maskItem) || "cornerstone mask".equals(maskItem)) {
            statStages(mon).put("defense", Math.min(6, statStage(mon, "defense") + 1));
            events.add(mon.get("name") + " 的面具提升了防御");
        }
        events.add(mon.get("name") + " 太晶化为了 " + teraName + " 属性");
    }

    boolean canUseSpecialSystem(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            String system, Map<String, Object> move) {
        // 1. 严格互斥性校验：一旦某方使用了任何一种特殊系统，其他三种在该场战斗中永久锁定
        // 但太晶化作为 Gen9 规则的特例，允许与其他系统共存，不过每队限一次
        if (Boolean.TRUE.equals(state.get(playerSide ? "playerSpecialUsed" : "opponentSpecialUsed"))) {
            if (!"tera".equals(system))
                return false; // 其他系统永久锁定
            if (Boolean.TRUE.equals(state.get(playerSide ? "playerTeraUsed" : "opponentTeraUsed")))
                return false; // 太晶化用过也不能再用
        }

        return switch (system) {
            case "tera" -> !Boolean.TRUE.equals(mon.get("terastallized")) && toInt(mon.get("teraTypeId"), 0) > 0;
            case "mega" -> Boolean.TRUE.equals(mon.get("megaEligible")) && !Boolean.TRUE.equals(mon.get("megaEvolved"));
            case "z-move" -> Boolean.TRUE.equals(mon.get("zMoveEligible"))
                    && !Boolean.TRUE.equals(mon.get("zMoveUsed"))
                    && move != null
                    && !isStatusMove(move)
                    && toInt(move.get("power"), 0) > 0;
            case "dynamax" ->
                Boolean.TRUE.equals(mon.get("dynamaxEligible")) && !Boolean.TRUE.equals(mon.get("dynamaxed"));
            default -> false;
        };
    }

    void activateSpecialSystem(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            Map<String, Object> move,
            String system, int round, Map<String, Object> actionLog, List<String> events) {
        if (!canUseSpecialSystem(state, playerSide, mon, system, move)) {
            return;
        }
        switch (system) {
            case "tera" -> activateTerastallization(state, playerSide, mon, actionLog, events);
            case "mega" -> activateMegaEvolution(state, playerSide, mon, actionLog, events);
            case "z-move" -> activateZMove(state, playerSide, mon, move, round, actionLog, events);
            case "dynamax" -> activateDynamax(state, playerSide, mon, actionLog, events);
            default -> {
            }
        }
    }

    boolean isZMoveActive(Map<String, Object> mon, int round, Map<String, Object> move) {
        return "z-move".equals(mon.get("specialSystemActivated"))
                && toInt(mon.get("zMoveRound"), 0) == round
                && move != null
                && String.valueOf(move.get("name_en")).equalsIgnoreCase(String.valueOf(mon.get("zMoveBase")));
    }

    boolean isDynamaxed(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("dynamaxed"));
    }

    private void activateMegaEvolution(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            Map<String, Object> actionLog, List<String> events) {
        mon.put("megaEvolved", true);
        mon.put("specialSystemActivated", "mega");
        applyMegaTemplate(mon);
        markSpecialSystemUsed(state, playerSide, "mega");
        actionLog.put("megaEvolved", true);
        events.add(mon.get("name") + " 完成了 Mega 进化");
    }

    private void activateZMove(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            Map<String, Object> move,
            int round, Map<String, Object> actionLog, List<String> events) {
        mon.put("zMoveUsed", true);
        mon.put("specialSystemActivated", "z-move");
        mon.put("zMoveRound", round);
        mon.put("zMoveBase", move == null ? "" : move.get("name_en"));
        markSpecialSystemUsed(state, playerSide, "z-move");

        // Identify Z-Crystal and map to exclusive Z-Move
        String item = heldItem(mon);
        String zMoveName = getZMoveName(move, item);
        if (zMoveName != null && move != null) {
            move.put("name", zMoveName); // Temporarily rename for logging/effect purposes
            actionLog.put("exclusiveZMove", zMoveName);
        }

        // Calculate Z-Move power boost (Pokemon Showdown standard)
        int basePower = move != null ? toInt(move.get("power"), 0) : 0;
        int zPower = basePower;
        if (basePower >= 140)
            zPower = 200;
        else if (basePower >= 130)
            zPower = 195;
        else if (basePower >= 120)
            zPower = 190;
        else if (basePower >= 110)
            zPower = 185;
        else if (basePower >= 100)
            zPower = 180;
        else if (basePower >= 90)
            zPower = 175;
        else if (basePower >= 80)
            zPower = 160;
        else if (basePower >= 75)
            zPower = 140;
        else if (basePower >= 70)
            zPower = 140;
        else if (basePower >= 65)
            zPower = 130;
        else if (basePower >= 60)
            zPower = 120;

        // Z-Status effect: for status moves, apply special Z-Move bonus
        if (move != null && isStatusMove(move)) {
            int heal = toInt(move.get("healing"), 0);
            if (heal > 0) {
                int maxHp = toInt(castMap(mon.get("stats")).get("hp"), 1);
                mon.put("currentHp", maxHp);
                actionLog.put("zStatusHeal", true);
                events.add(mon.get("name") + " 的 Z 力量完全回复了 HP！");
            } else {
                statStages(mon).put("attack", Math.min(6, statStage(mon, "attack") + 1));
                statStages(mon).put("defense", Math.min(6, statStage(mon, "defense") + 1));
                statStages(mon).put("specialAttack", Math.min(6, statStage(mon, "specialAttack") + 1));
                statStages(mon).put("specialDefense", Math.min(6, statStage(mon, "specialDefense") + 1));
                statStages(mon).put("speed", Math.min(6, statStage(mon, "speed") + 1));
                actionLog.put("zStatusBoost", true);
                events.add(mon.get("name") + " 的 Z 力量让全能力提升了！");
            }
        }

        actionLog.put("zMove", true);
        actionLog.put("zPower", zPower);
        events.add(mon.get("name") + " 释放了 Z 招式能量，威力提升至 " + zPower);
    }

    private String getZMoveName(Map<String, Object> move, String item) {
        if (move == null)
            return null;
        String moveName = String.valueOf(move.get("name_en")).toLowerCase();

        // Exclusive Z-Moves mapping (simplified for baseline)
        if ("snorlium-z".equals(item) && moveName.contains("giga-impact"))
            return "Pulverizing Pancake";
        if ("pikanium-z".equals(item) && moveName.contains("thunderbolt"))
            return "Catastropika";
        if ("incinium-z".equals(item) && moveName.contains("darkest-lariat"))
            return "Malicious Moonsault";
        if ("decidium-z".equals(item) && moveName.contains("spirit-shackle"))
            return "Sinister Arrow Raid";

        // Generic Z-Moves don't change name but get power boost
        return null;
    }

    private void activateDynamax(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            Map<String, Object> actionLog, List<String> events) {
        Map<String, Object> stats = castMap(mon.get("stats"));
        int baseHp = toInt(mon.get("dynamaxBaseHp"), toInt(stats.get("hp"), 1));
        int maxHp = toInt(stats.get("hp"), baseHp);
        if (maxHp <= baseHp) {
            stats.put("hp", baseHp * 2);
            mon.put("currentHp", toInt(mon.get("currentHp"), baseHp) * 2);
        }
        mon.put("dynamaxed", true);
        mon.put("dynamaxTurnsRemaining", 3);
        mon.put("specialSystemActivated", "dynamax");
        markSpecialSystemUsed(state, playerSide, "dynamax");
        actionLog.put("dynamaxed", true);
        events.add(mon.get("name") + " 进行了极巨化");
    }

    void endDynamax(Map<String, Object> mon, List<String> events) {
        if (!Boolean.TRUE.equals(mon.get("dynamaxed"))) {
            return;
        }
        Map<String, Object> stats = castMap(mon.get("stats"));
        int baseHp = toInt(mon.get("dynamaxBaseHp"), toInt(stats.get("hp"), 1));
        int currentMaxHp = Math.max(1, toInt(stats.get("hp"), baseHp));
        int currentHp = toInt(mon.get("currentHp"), 0);
        int restoredHp = Math.max(1, (int) Math.ceil((currentHp / (double) currentMaxHp) * baseHp));
        stats.put("hp", baseHp);
        mon.put("currentHp", Math.min(baseHp, restoredHp));
        mon.put("dynamaxed", false);
        mon.put("dynamaxTurnsRemaining", 0);
        events.add(mon.get("name") + " 的极巨化结束了");
    }

    private void applyMegaTemplate(Map<String, Object> mon) {
        Object statsValue = mon.get("megaStats");
        if (statsValue instanceof Map<?, ?> megaStats) {
            Map<String, Object> stats = castMap(mon.get("stats"));
            for (Map.Entry<?, ?> entry : megaStats.entrySet()) {
                stats.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        Object abilityValue = mon.get("megaAbility");
        if (abilityValue instanceof Map<?, ?> megaAbility) {
            mon.put("ability", new LinkedHashMap<>(castMap(megaAbility)));
        } else if (abilityValue != null) {
            mon.put("ability", abilityValue);
        }
    }

    private void markSpecialSystemUsed(Map<String, Object> state, boolean playerSide, String system) {
        state.put(playerSide ? "playerSpecialUsed" : "opponentSpecialUsed", true);
        state.put(playerSide ? "playerSpecialType" : "opponentSpecialType", system);
        if ("tera".equals(system)) {
            state.put(playerSide ? "playerTeraUsed" : "opponentTeraUsed", true);
        }
    }

    boolean isOnSide(Map<String, Object> state, Map<String, Object> mon, boolean playerSide) {
        for (Map<String, Object> candidate : team(state, playerSide)) {
            if (candidate == mon) {
                return true;
            }
        }
        return false;
    }

    boolean isAvailableMon(List<Map<String, Object>> team, int index) {
        return index >= 0 && index < team.size() && toInt(team.get(index).get("currentHp"), 0) > 0;
    }

    private Map<String, Object> fieldEffects(Map<String, Object> state) {
        return fieldEffectSupport.fieldEffects(state);
    }

    private int trickRoomTurns(Map<String, Object> state) {
        return fieldEffectSupport.trickRoomTurns(state);
    }

    int snowTurns(Map<String, Object> state) {
        return fieldEffectSupport.snowTurns(state);
    }

    int auroraVeilTurns(Map<String, Object> state, boolean playerSide) {
        return fieldEffectSupport.auroraVeilTurns(state, playerSide);
    }

    void activateTailwind(Map<String, Object> state, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateTailwind(state, playerSide, actor, actionLog, events);
        conditionSupport.applyTailwindWindRiderBoosts(state, actionLog, events);
    }

    void toggleTrickRoom(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> actionLog,
            List<String> events) {
        fieldEffectSupport.toggleTrickRoom(state, actor, actionLog, events);
    }

    void activateWeather(Map<String, Object> state, String weather, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateWeather(state, weather, actor, actionLog, events);
    }

    void activateTerrain(Map<String, Object> state, String terrain, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateTerrain(state, terrain, actor, actionLog, events);
    }

    // Entry hazard methods (public wrappers)
    void setStealthRock(Map<String, Object> state, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.setStealthRock(state, playerSide, actor, actionLog, events);
    }

    void addSpikesLayer(Map<String, Object> state, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.addSpikesLayer(state, playerSide, actor, actionLog, events);
    }

    void addToxicSpikesLayer(Map<String, Object> state, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.addToxicSpikesLayer(state, playerSide, actor, actionLog, events);
    }

    void setStickyWeb(Map<String, Object> state, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.setStickyWeb(state, playerSide, actor, actionLog, events);
    }

    void clearSideHazards(Map<String, Object> state, boolean playerSide) {
        fieldEffectSupport.clearSideHazards(state, playerSide);
    }

    void activateScreen(Map<String, Object> state, String screen, boolean playerSide, Map<String, Object> actor,
            Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateScreen(state, screen, playerSide, actor, actionLog, events);
    }

    Map<String, Object> resolveMoveForUse(Map<String, Object> actor, Map<String, Object> move) {
        if (!isTeraBlast(move)) {
            return move;
        }
        Map<String, Object> resolved = new LinkedHashMap<>(move);
        int teraTypeId = toInt(actor.get("teraTypeId"), 0);
        resolved.put("type_id", Boolean.TRUE.equals(actor.get("terastallized")) && teraTypeId > 0
                ? teraTypeId
                : DamageCalculatorUtil.TYPE_NORMAL);
        resolved.put("damage_class_id", teraBlastUsesPhysicalCategory(actor)
                ? DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL
                : DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL);
        return resolved;
    }

    private boolean teraBlastUsesPhysicalCategory(Map<String, Object> actor) {
        Map<String, Object> stats = castMap(actor.get("stats"));
        int attack = damageSupport.applyStageModifier(toInt(stats.get("attack"), 100),
                damageSupport.statStage(actor, "attack"));
        int specialAttack = damageSupport.applyStageModifier(toInt(stats.get("specialAttack"), 100),
                damageSupport.statStage(actor, "specialAttack"));
        return attack > specialAttack;
    }

    /**
     * 对本回合动作进行稳定排序。
     * <p>
     * 顺序层级：优先级 > 抢先/延后修正 > 速度(受戏法空间影响) > 随机同速打破 > side/index。
     * 通过每回合随机 tie-break 更接近 Showdown 的同速行为。
     * </p>
     */
    private void sortActions(List<Action> actions, boolean trickRoomActive, Random random) {
        Map<Action, Integer> speedTieBreakers = new IdentityHashMap<>();
        for (Action action : actions) {
            speedTieBreakers.put(action, random.nextInt());
        }
        actions.sort((left, right) -> {
            int byPriority = Integer.compare(right.priority(), left.priority());
            if (byPriority != 0) {
                return byPriority;
            }
            // 第二层顺序：Quick Claw / Quick Draw / Custap 之类的“抢先”与 Stall / Lagging Tail 之类的“延后”。
            int byOrderBoost = Integer.compare(right.orderBoost(), left.orderBoost());
            if (byOrderBoost != 0) {
                return byOrderBoost;
            }
            // 第三层顺序：正常按速度，戏法空间下反转速度比较方向。
            int bySpeed = trickRoomActive
                    ? Integer.compare(left.speed(), right.speed())
                    : Integer.compare(right.speed(), left.speed());
            if (bySpeed != 0) {
                return bySpeed;
            }
            // 同速不再固定按 side/index，而是每回合随机决定，更贴近 PS 的 tie-break 行为。
            int byRandomTie = Integer.compare(speedTieBreakers.get(left), speedTieBreakers.get(right));
            if (byRandomTie != 0) {
                return byRandomTie;
            }
            int bySide = left.side().compareTo(right.side());
            if (bySide != 0) {
                return bySide;
            }
            return Integer.compare(left.actorIndex(), right.actorIndex());
        });
    }

    void replaceActiveSlot(Map<String, Object> state, boolean player, int fieldSlot, int switchToIndex) {
        List<Integer> slots = new ArrayList<>(activeSlots(state, player));
        if (fieldSlot >= 0 && fieldSlot < slots.size()) {
            slots.set(fieldSlot, switchToIndex);
            state.put(player ? "playerActiveSlots" : "opponentActiveSlots", slots);
        }
    }

    int toInt(Object value, int fallback) {
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

    String actionKey(String side, int actorIndex) {
        return side + ":" + actorIndex;
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

    /**
     * 应用动作顺序修正层。
     * <p>
     * 该阶段只改变执行先后（orderBoost），不改变动作语义本身。
     * 复杂度为 O(n)，其中 n 为本回合声明动作数量。
     * </p>
     */
    private void applyActionOrderEffects(Map<String, Object> state, List<Action> actions, Random random) {
        for (int index = 0; index < actions.size(); index++) {
            Action action = actions.get(index);
            if (action.isSwitch()) {
                continue;
            }
            List<Map<String, Object>> sideTeam = team(state, "player".equals(action.side()));
            if (!isAvailableMon(sideTeam, action.actorIndex())) {
                continue;
            }
            Map<String, Object> actor = sideTeam.get(action.actorIndex());
            String item = heldItem(actor);
            // 顺序加成层：先处理“抢先行动”来源，再处理“延后行动”来源。
            if (isQuickDrawTriggered(actor, random)) {
                actions.set(index, action.withOrderBoost(1, "quick-draw"));
                continue;
            }
            if (isQuickClawTriggered(item, random)) {
                actions.set(index, action.withOrderBoost(1, "quick-claw"));
                continue;
            }
            if (isCustapBerryTriggered(actor, item)) {
                // Custap Berry 触发后立即标记已消耗，保证同一场战斗内不会重复触发。
                consumeItem(actor);
                actions.set(index, action.withOrderBoost(1, "custap-berry"));
                continue;
            }
            if (isDelayOrderItem(item)) {
                String source = "lagging-tail".equalsIgnoreCase(item) || "lagging tail".equalsIgnoreCase(item)
                        ? "lagging-tail"
                        : "full-incense";
                actions.set(index, action.withOrderBoost(-1, source));
                continue;
            }
            if (hasAbility(actor, "stall")) {
                actions.set(index, action.withOrderBoost(-1, "stall"));
            }
        }
    }

    private boolean isQuickDrawTriggered(Map<String, Object> mon, Random random) {
        // Quick Draw：30% 概率在同优先级内抢先行动。
        return hasAbility(mon, "quick-draw", "quick draw") && random.nextInt(100) < 30;
    }

    private boolean isQuickClawTriggered(String item, Random random) {
        return ("quick-claw".equalsIgnoreCase(item) || "quick claw".equalsIgnoreCase(item))
                && random.nextInt(100) < 20;
    }

    private boolean isCustapBerryTriggered(Map<String, Object> mon, String item) {
        if (!"custap-berry".equalsIgnoreCase(item) && !"custap berry".equalsIgnoreCase(item)) {
            return false;
        }
        if (itemConsumed(mon)) {
            return false;
        }
        int maxHp = toInt(castMap(mon.get("stats")).get("hp"), 1);
        int currentHp = toInt(mon.get("currentHp"), 0);
        return currentHp > 0 && currentHp * 4 <= maxHp;
    }

    private boolean isDelayOrderItem(String item) {
        // Lagging Tail / Full Incense：在同优先级内后手。
        return "lagging-tail".equalsIgnoreCase(item)
                || "lagging tail".equalsIgnoreCase(item)
                || "full-incense".equalsIgnoreCase(item)
                || "full incense".equalsIgnoreCase(item);
    }

    record Action(String side, String actionType, int actorIndex, int actorFieldSlot, int targetTeamIndex,
            int targetFieldSlot,
            int switchToTeamIndex, Map<String, Object> move, int speed, String specialSystemRequested,
            int orderBoost, String orderSource) {
        /**
         * 单个声明动作的不可变快照。
         * <p>
         * 这里显式携带 orderBoost/orderSource，目的是把“动作构建”和“顺序层修正”解耦：
         * 构建阶段只关心玩家/AI想做什么，顺序阶段再决定是否因特性或道具改变执行先后。
         * </p>
         */
        static Action moveAction(String side, int actorIndex, int actorFieldSlot, int targetTeamIndex,
                int targetFieldSlot,
                Map<String, Object> move, int speed, boolean terastallizeRequested) {
            return new Action(side, "move", actorIndex, actorFieldSlot, targetTeamIndex, targetFieldSlot, -1, move,
                    speed,
                    terastallizeRequested ? "tera" : null, 0, null);
        }

        static Action moveAction(String side, int actorIndex, int actorFieldSlot, int targetTeamIndex,
                int targetFieldSlot,
                Map<String, Object> move, int speed, String specialSystemRequested) {
            return new Action(side, "move", actorIndex, actorFieldSlot, targetTeamIndex, targetFieldSlot, -1, move,
                    speed,
                    specialSystemRequested, 0, null);
        }

        static Action switchAction(String side, int actorIndex, int actorFieldSlot, int switchToTeamIndex, int speed) {
            return new Action(side, "switch", actorIndex, actorFieldSlot, -1, -1, switchToTeamIndex, null, speed,
                    null, 0, null);
        }

        Action withOrderBoost(int boost, String source) {
            // 通过复制生成新 Action，避免原始动作在排序前后被可变修改污染。
            return new Action(side, actionType, actorIndex, actorFieldSlot, targetTeamIndex, targetFieldSlot,
                    switchToTeamIndex, move, speed, specialSystemRequested, boost, source);
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

        boolean terastallizeRequested() {
            return "tera".equals(specialSystemRequested);
        }
    }

    record RedirectionEffect(int actorIndex, boolean powder) {
    }

    record TargetRef(String side, boolean playerSide, int teamIndex, int fieldSlot) {
    }
}
