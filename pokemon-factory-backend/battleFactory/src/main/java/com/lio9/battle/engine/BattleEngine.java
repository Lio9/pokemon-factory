package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
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

    private final ObjectMapper mapper;
    private final SkillService skillService;
    private final TypeEfficacyMapper typeEfficacyMapper;
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
        this.typeEfficacyMapper = typeEfficacyMapper;
        this.mapper = mapper;
        this.stateSupport = new BattleStateSupport(mapper);
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
        this.turnCleanupSupport = new BattleTurnCleanupSupport(this, fieldEffectSupport);
    }

    /**
     * 创建“队伍预览阶段”的初始状态。
     */
    public Map<String, Object> createPreviewState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        return setupSupport.createPreviewState(playerTeamJson, opponentTeamJson, maxRounds, seed);
    }

    /**
     * 直接创建可进入战斗的状态。
     * <p>
     * 该方法会在内部自动完成 6 选 4 和首发选择，主要用于异步自动模拟。
     * </p>
     */
    public Map<String, Object> createBattleState(String playerTeamJson, String opponentTeamJson, int maxRounds, long seed) {
        return setupSupport.createBattleState(playerTeamJson, opponentTeamJson, maxRounds, seed);
    }

    /**
     * 应用队伍预览阶段的选择，并把状态切到 running/battle。
     */
    public Map<String, Object> applyTeamPreviewSelection(Map<String, Object> rawState, Map<String, Object> playerSelectionInput, Map<String, Object> opponentSelectionInput) {
        return setupSupport.applyTeamPreviewSelection(rawState, playerSelectionInput, opponentSelectionInput);
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
        flowSupport.pruneActiveSlots(state);

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
            roundSupport.processAction(state, action, round, random, protectedTargets, redirectionTargets,
                    helpingHandBoosts, actionLogs, events);
        }

        turnCleanupSupport.applyEndTurnEffects(state, fieldSnapshot, events);
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
     * </p>
     */
    public Map<String, Object> autoPlay(Map<String, Object> rawState, Map<String, String> playerMoveMap) {
        Map<String, Object> state = cloneState(rawState);
        while ("running".equals(state.get("status"))) {
            if ("replacement".equals(state.get("phase"))) {
                state = applyReplacementSelection(state, Map.of("replacementIndexes", flowSupport.autoReplacementIndexes(state, true)));
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
        return setupSupport.applyReplacementSelection(rawState, selectionInput);
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
        return setupSupport.replacePlayerTeamMember(rawState, replacedIndex, newMember);
    }

    private List<Action> buildPlayerActions(Map<String, Object> state, Map<String, String> playerMoveMap) {
        return actionBuilder.buildPlayerActions(state, playerMoveMap);
    }

    private List<Action> buildOpponentActions(Map<String, Object> state, Random random) {
        return actionBuilder.buildOpponentActions(state, random);
    }

    Map<String, Object> selectPlayerMove(Map<String, Object> mon, Map<String, String> playerMoveMap, int fieldSlot, int currentRound) {
        return decisionSupport.selectPlayerMove(mon, playerMoveMap, fieldSlot, currentRound);
    }

    boolean canSwitch(List<Map<String, Object>> team, List<Integer> activeSlots, int actorFieldSlot, int switchToIndex) {
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

    Map<String, Object> selectAIMove(Map<String, Object> mon, Random random, Map<String, Object> state, boolean playerSide, int currentRound) {
        return decisionSupport.selectAIMove(mon, random, state, playerSide, currentRound);
    }

    Map<String, Object> defaultMoveSelection(Map<String, Object> mon, int currentRound) {
        return decisionSupport.defaultMoveSelection(mon, currentRound);
    }

    int calculateDamage(Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move, Random random,
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
        int cooldown = skillService.getCooldown(String.valueOf(move.get("name_en")), isProtect(move) ? 2 : 0);
        if (cooldown > 0) {
            cooldowns(mon).put(String.valueOf(move.get("name_en")), cooldown + 1);
        }
    }

    boolean isProtect(Map<String, Object> move) {
        return "protect".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean isFakeOut(Map<String, Object> move) {
        return "fake-out".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "fake out".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    boolean isTaunt(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "taunt".equalsIgnoreCase(nameEn);
    }

    boolean isSpore(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "spore".equalsIgnoreCase(nameEn);
    }

    boolean isReflect(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "reflect".equalsIgnoreCase(nameEn);
    }

    boolean isLightScreen(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "light-screen".equalsIgnoreCase(nameEn) || "light screen".equalsIgnoreCase(nameEn);
    }

    boolean canUseMove(Map<String, Object> mon, Map<String, Object> move, int currentRound) {
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

    boolean isStatusMove(Map<String, Object> move) {
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

    boolean isRedirectionMove(Map<String, Object> move) {
        return isFollowMe(move) || isRagePowder(move);
    }

    boolean isHelpingHand(Map<String, Object> move) {
        return "helping-hand".equalsIgnoreCase(String.valueOf(move.get("name_en")))
                || "helping hand".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    int targetIndex(Map<String, Object> state, boolean playerTarget, int targetFieldSlot) {
        return targetSupport.targetIndex(state, playerTarget, targetFieldSlot);
    }

    private int targetId(Map<String, Object> move) {
        return toInt(move.get("target_id"), 10);
    }

    boolean isTailwind(Map<String, Object> move) {
        return "tailwind".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    boolean isTrickRoom(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "trick-room".equalsIgnoreCase(nameEn) || "trick room".equalsIgnoreCase(nameEn);
    }

    boolean isRainDance(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "rain-dance".equalsIgnoreCase(nameEn) || "rain dance".equalsIgnoreCase(nameEn);
    }

    boolean isSunnyDay(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "sunny-day".equalsIgnoreCase(nameEn) || "sunny day".equalsIgnoreCase(nameEn);
    }

    boolean isSandstorm(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "sandstorm".equalsIgnoreCase(nameEn);
    }

    boolean isSnowWeather(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "snowscape".equalsIgnoreCase(nameEn) || "hail".equalsIgnoreCase(nameEn);
    }

    boolean isElectricTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "electric-terrain".equalsIgnoreCase(nameEn) || "electric terrain".equalsIgnoreCase(nameEn);
    }

    boolean isPsychicTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "psychic-terrain".equalsIgnoreCase(nameEn) || "psychic terrain".equalsIgnoreCase(nameEn);
    }

    boolean isGrassyTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "grassy-terrain".equalsIgnoreCase(nameEn) || "grassy terrain".equalsIgnoreCase(nameEn);
    }

    boolean isMistyTerrain(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "misty-terrain".equalsIgnoreCase(nameEn) || "misty terrain".equalsIgnoreCase(nameEn);
    }

    boolean isIcyWind(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "icy-wind".equalsIgnoreCase(nameEn) || "icy wind".equalsIgnoreCase(nameEn);
    }

    boolean isThunderWave(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "thunder-wave".equalsIgnoreCase(nameEn) || "thunder wave".equalsIgnoreCase(nameEn);
    }

    boolean isWillOWisp(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "will-o-wisp".equalsIgnoreCase(nameEn) || "will o wisp".equalsIgnoreCase(nameEn);
    }

    boolean isSpreadMove(Map<String, Object> move) {
        return switch (targetId(move)) {
            case 9, 11, 12, 13, 14 -> true;
            default -> false;
        };
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> cloneState(Map<String, Object> state) {
        return stateSupport.cloneState(state);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> castMap(Object value) {
        return stateSupport.castMap(value);
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> castList(Object value) {
        return stateSupport.castList(value);
    }

    private Map<String, Object> cloneMap(Map<String, Object> value) {
        return stateSupport.cloneMap(value);
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> team(Map<String, Object> state, boolean player) {
        return stateSupport.team(state, player);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rounds(Map<String, Object> state) {
        return stateSupport.rounds(state);
    }

    List<Integer> activeSlots(Map<String, Object> state, boolean player) {
        return stateSupport.activeSlots(state, player);
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> moves(Map<String, Object> mon) {
        return stateSupport.moves(mon);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cooldowns(Map<String, Object> mon) {
        return stateSupport.cooldowns(mon);
    }

    int cooldown(Map<String, Object> mon, Map<String, Object> move) {
        return toInt(cooldowns(mon).get(move.get("name_en")), 0);
    }

    private int modifiedAttackStat(Map<String, Object> mon, int baseStat, int damageClassId) {
        return damageSupport.modifiedAttackStat(mon, baseStat, damageClassId);
    }

    private int modifiedDefenseStat(Map<String, Object> mon, int baseStat, int damageClassId, Map<String, Object> state) {
        return damageSupport.modifiedDefenseStat(mon, baseStat, damageClassId, state);
    }

    private double itemDamageModifier(Map<String, Object> mon, int moveTypeId) {
        return damageSupport.itemDamageModifier(mon, moveTypeId);
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

    boolean itemConsumed(Map<String, Object> mon) {
        return Boolean.TRUE.equals(mon.get("itemConsumed"));
    }

    void consumeItem(Map<String, Object> mon) {
        mon.put("itemConsumed", true);
    }

    int applyIncomingDamage(Map<String, Object> target, int damage, Map<String, Object> actionLog, List<String> events) {
        return damageSupport.applyIncomingDamage(target, damage, actionLog, events);
    }

    void applyDefenderItemEffects(Map<String, Object> target, Map<String, Object> move, int actualDamage,
                                  Map<String, Object> actionLog, List<String> events) {
        if (toInt(target.get("currentHp"), 0) <= 0) {
            return;
        }
        if ("weakness-policy".equals(heldItem(target))
                && !itemConsumed(target)
                && actualDamage > 0
                && typeModifier(target, toInt(move.get("type_id"), 0)) > 1.0d) {
            int attackStage = Math.min(6, statStage(target, "attack") + 2);
            int specialAttackStage = Math.min(6, statStage(target, "specialAttack") + 2);
            statStages(target).put("attack", attackStage);
            statStages(target).put("specialAttack", specialAttackStage);
            consumeItem(target);
            actionLog.put("weaknessPolicy", true);
            events.add(target.get("name") + " 的弱点保险发动了，攻击和特攻大幅提升");
        }
        if (!"sitrus-berry".equals(heldItem(target)) || itemConsumed(target)) {
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

    void applyAttackerItemEffects(Map<String, Object> attacker, int damage, Map<String, Object> actionLog, List<String> events) {
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

    void rememberChoiceMove(Map<String, Object> mon, Map<String, Object> move) {
        String item = heldItem(mon);
        if ("choice-band".equals(item) || "choice-specs".equals(item) || "choice-scarf".equals(item)) {
            mon.put("choiceLockedMove", move.get("name_en"));
        }
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

    private int applyStageModifier(int baseStat, int stage) {
        return damageSupport.applyStageModifier(baseStat, stage);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> statStages(Map<String, Object> mon) {
        return damageSupport.statStages(mon);
    }

    private int statStage(Map<String, Object> mon, String stat) {
        return damageSupport.statStage(mon, stat);
    }

    int tauntTurns(Map<String, Object> mon) {
        return toInt(mon.get("tauntTurns"), 0);
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
        for (Map<String, Object> type : castList(target.get("types"))) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> fieldEffects(Map<String, Object> state) {
        return fieldEffectSupport.fieldEffects(state);
    }

    private int tailwindTurns(Map<String, Object> state, boolean playerSide) {
        return fieldEffectSupport.tailwindTurns(state, playerSide);
    }

    private int trickRoomTurns(Map<String, Object> state) {
        return fieldEffectSupport.trickRoomTurns(state);
    }

    private int rainTurns(Map<String, Object> state) {
        return fieldEffectSupport.rainTurns(state);
    }

    private int sunTurns(Map<String, Object> state) {
        return fieldEffectSupport.sunTurns(state);
    }

    private int sandTurns(Map<String, Object> state) {
        return fieldEffectSupport.sandTurns(state);
    }

    private int snowTurns(Map<String, Object> state) {
        return fieldEffectSupport.snowTurns(state);
    }

    private int electricTerrainTurns(Map<String, Object> state) {
        return fieldEffectSupport.electricTerrainTurns(state);
    }

    private int psychicTerrainTurns(Map<String, Object> state) {
        return fieldEffectSupport.psychicTerrainTurns(state);
    }

    private int grassyTerrainTurns(Map<String, Object> state) {
        return fieldEffectSupport.grassyTerrainTurns(state);
    }

    private int mistyTerrainTurns(Map<String, Object> state) {
        return fieldEffectSupport.mistyTerrainTurns(state);
    }

    private int weatherTurns(Map<String, Object> state) {
        return fieldEffectSupport.weatherTurns(state);
    }

    private int terrainTurns(Map<String, Object> state) {
        return fieldEffectSupport.terrainTurns(state);
    }

    private int reflectTurns(Map<String, Object> state, boolean playerSide) {
        return fieldEffectSupport.reflectTurns(state, playerSide);
    }

    private int lightScreenTurns(Map<String, Object> state, boolean playerSide) {
        return fieldEffectSupport.lightScreenTurns(state, playerSide);
    }

    void activateTailwind(Map<String, Object> state, boolean playerSide, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateTailwind(state, playerSide, actor, actionLog, events);
    }

    void toggleTrickRoom(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.toggleTrickRoom(state, actor, actionLog, events);
    }

    void activateWeather(Map<String, Object> state, String weather, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateWeather(state, weather, actor, actionLog, events);
    }

    void activateTerrain(Map<String, Object> state, String terrain, Map<String, Object> actor, Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateTerrain(state, terrain, actor, actionLog, events);
    }

    void activateScreen(Map<String, Object> state, String screen, boolean playerSide, Map<String, Object> actor,
                        Map<String, Object> actionLog, List<String> events) {
        fieldEffectSupport.activateScreen(state, screen, playerSide, actor, actionLog, events);
    }

    private double weatherDamageModifier(Map<String, Object> state, int moveTypeId) {
        return damageSupport.weatherDamageModifier(state, moveTypeId);
    }

    private double terrainDamageModifier(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender, int moveTypeId) {
        return damageSupport.terrainDamageModifier(state, attacker, defender, moveTypeId);
    }

    private double screenDamageModifier(Map<String, Object> state, Map<String, Object> defender, int damageClassId) {
        return damageSupport.screenDamageModifier(state, defender, damageClassId);
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

    record Action(String side, String actionType, int actorIndex, int actorFieldSlot, int targetTeamIndex, int targetFieldSlot, int switchToTeamIndex, Map<String, Object> move, int speed) {
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

    record RedirectionEffect(int actorIndex, boolean powder) {
    }

    record TargetRef(String side, boolean playerSide, int teamIndex, int fieldSlot) {
    }
}
