package com.lio9.battle.engine;


import com.lio9.battle.engine.event.BattleEvent;
import com.lio9.battle.engine.event.BattleEventType;
import com.lio9.battle.engine.event.DamageEvent;
import com.lio9.battle.engine.event.EventResult;
import com.lio9.battle.engine.event.ModifyPowerEvent;
import com.lio9.battle.engine.event.TryHitEvent;
import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class BattleRoundSupport {
/**
 * ============================================================
 * 单回合动作执行器 / Single Round Action Executor
 * ============================================================
 *
 * ## 核心职责 / Core Responsibility
 *
 * 将排好序的 Action 逐一落地执行，管理整个"动作 → 判定 → 结算"管线。
 * Executes sorted actions one by one, managing the action → resolution pipeline.
 *
 * ## 执行管线 / Execution Pipeline
 *
 *   processAction() 对每个 Action 执行以下检查链：
 *   for each action, the following check chain is executed:
 *
 *   1. 蓄力回复检查 / Recharge cooldown check
 *   2. 畏缩检查 / Flinch check
 *   3. 冰冻检查 / Freeze check (handleFrozenBeforeAction)
 *   4. 麻痹检查（1/4 概率跳过）/ Paralysis check (25% skip)
 *   5. 睡眠检查 / Sleep check (Sleep Talk / Snore 可以行动)
 *   6. 混乱检查（1/3 概率自伤）/ Confusion check (33% self-hit)
 *   7. 着迷检查（50% 概率无法行动）/ Infatuation check (50% skip)
 *   8. 蓄力招式继续执行 / Charging move continuation
 *   9. 换人处理 / Switch handling
 *   10. 太晶化/Z招式/极巨化触发 / Special system activation
 *   11. 挑衅检查（封变化招式）/ Taunt check (blocks status)
 *   12. 定身法检查 / Disable check
 *   13. 封印检查 / Imprison check
 *   14. 喉斩检查（封声音招式）/ Throat Chop check (blocks sound)
 *   15. 保护类招式处理 / Protection move handling
 *   16. 变化招式处理 / Status move handling
 *   17. 伤害目标检查 + 命中判定 / Target resolution + accuracy check
 *   18. 伤害计算与结算 / Damage calculation and application
 *   19. 追加效果处理 / Secondary effect resolution
 *   20. 换人后处理（快速折返等）/ Post-move switching (U-turn etc.)
 *
 * ## 关键设计决策 / Key Design Decisions
 *
 * - 所有"行动前阻断"状态集中在此处理，不分散到各个模块
 *   All pre-action blocking states are centralized here
 * - 保护成功率使用 PS 公式：连续使用 n 次后成功率 = 1/3^n
 *   Protection success uses PS formula: success after n uses = 1/3^n
 * - Protean/Libero 在命中前就改变属性
 *   Protean/Libero changes type before the move hits
 *
 * @see BattleEngine#playRound() 调用此类的入口
 * @see BattleConditionSupport 异常状态和特性交互
 * <p>
 * 该类负责把已经排好序的 Action 真正落地到战斗状态中，包括：
 * 行动前阻断（畏缩/睡眠/混乱等）、保护类判定、命中判定、伤害结算、追加效果与回合日志记录。
     * </p>
     */
    private final BattleEngine engine;
    private final BattleConditionSupport conditionSupport;
    private final BattleTargetSupport targetSupport;

    BattleRoundSupport(BattleEngine engine, BattleConditionSupport conditionSupport, BattleTargetSupport targetSupport) {
        this.engine = engine;
        this.conditionSupport = conditionSupport;
        this.targetSupport = targetSupport;
    }

    void processAction(Map<String, Object> state, BattleEngine.Action action, int round, Random random,
                       Map<String, Boolean> protectedTargets,
                       Map<String, Boolean> wideGuardSides,
                       Map<String, Boolean> quickGuardSides,
                       Map<String, BattleEngine.RedirectionEffect> redirectionTargets,
                       Map<String, BattleEngine.Action> plannedActions,
                       Map<Map<String, Object>, Boolean> helpingHandBoosts,
                       List<Map<String, Object>> actionLogs, List<String> events) {
        boolean playerSide = "player".equals(action.side());
        List<Map<String, Object>> actingTeam = engine.team(state, playerSide);
        if (!engine.isAvailableMon(actingTeam, action.actorIndex())) {
            return;
        }

        Map<String, Object> actor = actingTeam.get(action.actorIndex());
        Map<String, Object> forcedChargeMove = chargingMove(actor);
        // 换人动作不受 flinch/麻痹/睡眠/混乱/冰冻/着迷等"行动前阻断"影响
        // （正作/Showdown：状态只阻止出招，主动换人仅受束缚类效果限制）
        // 但蓄力中（二回合招式第一回合）不可换人。
        if (forcedChargeMove == null && action.isSwitch()) {
            Map<String, Object> actionLog = new LinkedHashMap<>();
            actionLog.put("side", action.side());
            actionLog.put("actor", actor.get("name"));
            if (action.orderSource() != null && !action.orderSource().isBlank()) {
                actionLog.put("orderSource", action.orderSource());
                events.add(actor.get("name") + orderSourceMessage(action.orderSource()));
            }
            handleSwitch(state, action, actingTeam, actor, playerSide, actionLogs, events, actionLog);
            return;
        }
        // 所有“行动前就会阻断本次出手”的状态都在这里集中处理，避免分散到各个伤害/状态模块里。
        if (engine.toInt(actor.get("rechargeTurns"), 0) > 0) {
            Map<String, Object> rechargeLog = new LinkedHashMap<>();
            rechargeLog.put("side", action.side());
            rechargeLog.put("actor", actor.get("name"));
            rechargeLog.put("actionType", "recharge");
            rechargeLog.put("result", "recharge");
            actionLogs.add(rechargeLog);
            actor.put("rechargeTurns", Math.max(0, engine.toInt(actor.get("rechargeTurns"), 0) - 1));
            events.add(actor.get("name") + " 正在回复，无法行动");
            return;
        }
        if (engine.volatileFlag(actor, "flinch")) {
            Map<String, Object> flinchLog = new LinkedHashMap<>();
            flinchLog.put("side", action.side());
            flinchLog.put("actor", actor.get("name"));
            flinchLog.put("actionType", "flinch");
            flinchLog.put("result", "flinch");
            actionLogs.add(flinchLog);
            events.add(actor.get("name") + " 畏缩了，无法行动");
            engine.setVolatile(actor, "flinch", false);
            return;
        }
        if (conditionSupport.handleFrozenBeforeAction(actor, action.side(), actionLogs, events, random)) {
            return;
        }
        if ("paralysis".equals(actor.get("condition")) && random.nextInt(4) == 0) {
            Map<String, Object> paralysisLog = new LinkedHashMap<>();
            paralysisLog.put("side", action.side());
            paralysisLog.put("actor", actor.get("name"));
            paralysisLog.put("actionType", "paralysis");
            paralysisLog.put("result", "paralyzed");
            actionLogs.add(paralysisLog);
            events.add(actor.get("name") + " 因为麻痹而无法行动");
            return;
        }
        if ("sleep".equals(actor.get("condition"))) {
            Map<String, Object> currentMove = action.move();
            boolean isSleepUsableMove = currentMove != null
                && (engine.isSleepTalk(currentMove) || engine.isSnore(currentMove));

            if (isSleepUsableMove) {
                // Sleep Talk/Snore 在睡眠中可用，但仍需递减睡眠回合数
                int remaining = Math.max(0, engine.toInt(actor.get("sleepTurns"), 0) - 1);
                actor.put("sleepTurns", remaining);
                if (remaining <= 0) {
                    // 睡眠结束，醒来（下一回合才能行动）
                    actor.put("condition", null);
                    actor.put("sleepAppliedRound", 0);
                    engine.setVolatile(actor, "nightmare", false);
                    events.add(actor.get("name") + " 在睡梦中使用了招式后醒来了！");
                } else {
                    events.add(actor.get("name") + " 在睡梦中使用了招式！");
                }
            } else if (engine.isSleepingThisTurn(actor, round)) {
                // 仍在睡眠中，不能行动
                Map<String, Object> sleepLog = new LinkedHashMap<>();
                sleepLog.put("side", action.side());
                sleepLog.put("actor", actor.get("name"));
                sleepLog.put("actionType", "sleep");
                sleepLog.put("result", "asleep");
                actionLogs.add(sleepLog);
                events.add(actor.get("name") + " 正在睡觉，无法行动");
                return;
            } else {
                // 睡眠结束，醒来
                actor.put("condition", null);
                actor.put("sleepTurns", 0);
                actor.put("sleepAppliedRound", 0);
                engine.setVolatile(actor, "nightmare", false);
                events.add(actor.get("name") + " 醒来了");
            }
        }
        if (conditionSupport.handleConfusionBeforeAction(actor, action.side(), actionLogs, events, random)) {
            return;
        }

        if (isInfatuatedAndBlocked(actor, actionLogs, events, random)) {
            return;
        }

        Map<String, Object> actionLog = new LinkedHashMap<>();
        actionLog.put("side", action.side());
        actionLog.put("actor", actor.get("name"));
        if (action.orderSource() != null && !action.orderSource().isBlank()) {
            // 把顺序来源写入日志，便于固定 seed 回归时直接观察先后手原因。
            actionLog.put("orderSource", action.orderSource());
            events.add(actor.get("name") + orderSourceMessage(action.orderSource()));
        }

        if (forcedChargeMove != null) {
            Map<String, Object> trackedChargeMove = new LinkedHashMap<>(forcedChargeMove);
            trackedChargeMove.put("tracksTarget", true);
            action = BattleEngine.Action.moveAction(action.side(), action.actorIndex(), action.actorFieldSlot(),
                engine.toInt(actor.get("chargingTargetTeamIndex"), action.targetTeamIndex()),
                engine.toInt(actor.get("chargingTargetFieldSlot"), action.targetFieldSlot()),
                trackedChargeMove, action.speed(), action.specialSystemRequested());
        }
        Map<String, Object> move = forcedChargeMove != null ? forcedChargeMove : action.move();
        actionLog.put("move", move.get("name"));
        if (forcedChargeMove == null && action.specialSystemRequested() != null) {
            engine.activateSpecialSystem(state, playerSide, actor, move, action.specialSystemRequested(), round, actionLog, events);
        }
        move = engine.resolveMoveForUse(actor, move);
        if (forcedChargeMove == null && shouldStartCharging(actor, move, state)) {
            startCharging(actor, action, move, actionLog, actionLogs, events);
            return;
        }
        if (engine.tauntTurns(actor) > 0 && engine.isStatusMove(move)) {
            actionLog.put("result", "taunted");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 因挑衅无法使出变化招式");
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            return;
        }
        Object disabledMove = engine.disableMove(actor);
        if (engine.disableTurns(actor) > 0 && disabledMove != null
                && String.valueOf(disabledMove).equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
            actionLog.put("result", "disabled");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 的 " + move.get("name") + " 被定身法封住了，无法使用");
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            return;
        }
        // 封印（Imprison）：对方场上有使用者拥有该招式时，无法使用
        if (isBlockedByImprison(state, actor, move)) {
            actionLog.put("result", "imprisoned");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 的" + move.get("name") + " 被封印了，无法使用！");
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            return;
        }

        // Throat Chop: 喉斩沉默，无法使用声音类招式
        if (engine.toInt(engine.volatileValue(actor, "throatChopTurns", 0), 0) > 0
                && MoveRegistry.isSoundMove(move)) {
            actionLog.put("result", "throat-chop-blocked");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 因喉斩效果无法使用声音类招式！");
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            return;
        }
        if (!MoveRegistry.isProtectionMove(move)) {
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
        }

        // 最终手段：必须所有已知招式都已使用过至少一次才能使用
        if (MoveRegistry.isLastResort(move) && !canUseLastResort(actor)) {
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 的最终手段失败了——并非所有招式都已用过");
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            return;
        }

        if (handleSupportMove(state, action, actor, move, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                redirectionTargets, helpingHandBoosts, actionLogs, events, actionLog, playerSide)) {
            return;
        }

        List<BattleEngine.TargetRef> targets = targetSupport.resolveMoveTargets(state, action, move, random, redirectionTargets);
        if (targets.isEmpty()) {
            // Fire ON_MOVE_FAIL — 无目标时招式失败
            engine.getEventBus().fireEvent(BattleEventType.ON_MOVE_FAIL,
                new BattleEvent(BattleEventType.ON_MOVE_FAIL) {},
                Map.of("source", actor, "move", move, "state", state));
            actionLog.put("result", "failed");
            actionLog.put("damage", 0);
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了 " + move.get("name") + "，但失败了");
            engine.rememberLastMove(actor, move);
            engine.rememberChoiceMove(actor, move);
            engine.applyCooldown(actor, move);
            return;
        }

        int totalDamage = 0;
        boolean anyHit = false;
        boolean selfStatChangeTriggered = false;
        boolean suckerPunchAttempted = false;
        boolean liveTargetFound = false;
        int spreadTargetCount = 0;
        if (engine.isSpreadMove(move)) {
            for (BattleEngine.TargetRef targetRef : targets) {
                List<Map<String, Object>> targetSideTeam = engine.team(state, targetRef.playerSide());
                if (engine.isAvailableMon(targetSideTeam, targetRef.teamIndex())) {
                    spreadTargetCount += 1;
                }
            }
        }
        Map<String, Object> moveForDamage = move;
        if (engine.isSpreadMove(move)) {
            // 群攻招式是否吃 0.75 修正，应基于“实际命中的存活目标数”，而不是仅凭 target_id 推断。
            moveForDamage = new LinkedHashMap<>(move);
            moveForDamage.put("spreadTargetCount", spreadTargetCount);
        }
        for (BattleEngine.TargetRef targetRef : targets) {
            List<Map<String, Object>> targetSideTeam = engine.team(state, targetRef.playerSide());
            if (!engine.isAvailableMon(targetSideTeam, targetRef.teamIndex())) {
                continue;
            }
            liveTargetFound = true;
            // 讲究系列锁招：只要招式被"使用"（无论命中、落空、被守住、免疫），即被锁定
            // （Gen 3+ 锁招语义，对齐 Showdown）
            engine.rememberLastMove(actor, move);
            engine.rememberChoiceMove(actor, move);
            Map<String, Object> target = targetSideTeam.get(targetRef.teamIndex());
            Map<String, Object> targetLog = new LinkedHashMap<>(actionLog);
            targetLog.put("target", target.get("name"));
            targetLog.put("targetFieldSlot", targetRef.fieldSlot());

            // Future Sight / Doom Desire：存储延迟攻击数据，跳过正常伤害流程
            if (MoveRegistry.isDelayedAttackMove(move)) {
                int typeId = engine.toInt(move.get("type_id"), DamageCalculatorUtil.TYPE_PSYCHIC);
                int power = engine.toInt(move.get("power"), 120);
                int calculatedDamage = engine.calculateDamage(actor, target, moveForDamage, random, helpingHandBoosts, state);
                if (calculatedDamage <= 0) {
                    calculatedDamage = power; // fallback
                }
                Map<String, Object> fsData = new LinkedHashMap<>();
                fsData.put("turns", 3); // 回合末递减：3→2→1→0 命中（2回合后）
                fsData.put("damage", calculatedDamage);
                fsData.put("moveName", move.get("name"));
                fsData.put("moveNameEn", move.get("name_en"));
                fsData.put("attackerName", actor.get("name"));
                fsData.put("typeId", typeId);
                fsData.put("power", power);
                engine.setFutureSight(state, "player".equals(action.side()), fsData);
                targetLog.put("result", "future-sight-stored");
                String fsName = String.valueOf(move.getOrDefault("name", "预知未来"));
                events.add(actor.get("name") + " 使用了 " + fsName + "！攻击将在 2 回合后降临");
                actionLogs.add(targetLog);
                continue;
            }

            if (engine.isSuckerPunch(move)) {
                suckerPunchAttempted = true;
                if (!canSuckerPunchTarget(plannedActions, targetRef, move)) {
                    targetLog.put("result", "failed");
                    targetLog.put("damage", 0);
                    actionLogs.add(targetLog);
                    events.add(actor.get("name") + " 使用了 Sucker Punch，但失败了");
                    continue;
                }
            }

            if (isBlockedByQuickGuard(move, targetRef.side(), quickGuardSides)) {
                targetLog.put("result", "quick-guard-blocked");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(target.get("name") + " 受快速防守保护，挡住了先制招式");
                continue;
            }
            if (conditionSupport.isBlockedByPsychicTerrain(state, action.side(), target, move)) {
                targetLog.put("result", "psychic-terrain-blocked");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(target.get("name") + " 受到精神场地保护，挡住了先制招式");
                continue;
            }
            if (conditionSupport.isBlockedByPriorityBlockingAbility(state, action.side(), target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }
            if (engine.isSpreadMove(move) && wideGuardSides.getOrDefault(targetRef.side(), false)) {
                targetLog.put("result", "wide-guard-blocked");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(target.get("name") + " 受广域防守保护，挡住了群体招式");
                continue;
            }

            // Unseen Fist: contact moves bypass Protect
            boolean unseenFistBreak = "unseen-fist".equalsIgnoreCase(engine.abilityName(actor))
                    && engine.isContactMove(move);

            String protectionKey = engine.protectionKey(targetRef.side(), targetRef.teamIndex());
            if (protectedTargets.getOrDefault(protectionKey, false)) {
                if (engine.isFeint(move) || unseenFistBreak) {
                    protectedTargets.put(protectionKey, false);
                    targetLog.put("protectionBroken", true);
                    events.add(target.get("name") + " 的守护被佯攻击破了");
                } else {
                    targetLog.put("result", "blocked");
                    targetLog.put("damage", 0);
                    // Protection variant contact effects - logged but require full protection name tracking
                    // for complete implementation (needs defender's protection move name)
                    actionLogs.add(targetLog);
                    events.add(target.get("name") + " 通过 Protect 挡住了攻击");
                    continue;
                }
            }

            // 半无敌状态检查：目标处于蓄力期半无敌（飞空/挖洞等），且招式不能穿透时自动失败
            if (engine.volatileFlag(target, "semiInvulnerable")
                    && !MoveRegistry.canHitSemiInvulnerable(move)
                    && !"struggle".equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                targetLog.put("result", "semi-invulnerable-miss");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(target.get("name") + " 处于半无敌状态，攻击无效");
                continue;
            }

            // Calculate accuracy with stages (Pokemon Showdown standard)
            int accuracy = calculateAccuracyWithStages(state, actor, target, move);
            if (random.nextInt(100) + 1 > accuracy) {
                targetLog.put("result", "miss");
                targetLog.put("damage", 0);
                // Blunder Policy: miss triggers Speed +2
                if ("blunder-policy".equalsIgnoreCase(engine.heldItem(actor)) && !engine.itemConsumed(actor)) {
                    engine.consumeItem(actor);
                    targetLog.put("blunderPolicy", true);
                    events.add(actor.get("name") + " 的打空保险触发了，速度大幅提升");
                }
                actionLogs.add(targetLog);
                events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 攻击落空");
                continue;
            }

            // Protean/Libero: change type to match move before using it
            String proteanAbility = engine.abilityName(actor);
            if (("protean".equalsIgnoreCase(proteanAbility) || "libero".equalsIgnoreCase(proteanAbility))
                    && engine.toInt(move.get("power"), 0) > 0) {
                int moveType = engine.toInt(move.get("type_id"), 0);
                if (moveType > 0) {
                    actor.put("proteanType", moveType);
                    targetLog.put("proteanType", moveType);
                    events.add(actor.get("name") + " 的" + (proteanAbility.contains("libero") ? "自由者" : "变隐龙")
                            + "特性发动，变成了 " + moveType + " 属性");
                }
            }

            // Fire ON_HIT — 检测类型免疫/特性阻挡
            {
                EventResult evtResult = engine.getEventBus().fireEvent(BattleEventType.ON_HIT,
                    TryHitEvent.normal(), Map.of("source", actor, "target", target, "move", move, "state", state));
                if (evtResult.isImmune()) {
                    targetLog.put("result", "immune");
                    targetLog.put("damage", 0);
                    actionLogs.add(targetLog);
                    if (evtResult.getMessage() != null) events.add(evtResult.getMessage());
                    continue;
                }
            }

            Map<String, Object> statusSource = actor;
            Map<String, Object> statusTarget = target;
            String statusActingSide = action.side();
            if (engine.isStatusMove(move)
                    && conditionSupport.shouldMagicBounceStatusMove(state, action.side(), actor, target, move, targetLog, events)) {
                statusSource = target;
                statusTarget = actor;
                statusActingSide = engine.isOnSide(state, statusSource, true) ? "player" : "opponent";
                targetLog.put("target", statusTarget.get("name"));
                targetLog.put("targetFieldSlot", action.actorFieldSlot());
            }

            if (engine.isStatusMove(move)
                    && conditionSupport.isStatusMoveBlockedByAbility(state, statusActingSide, statusSource, statusTarget, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }
            // Substitute blocks status moves targeting the defender
            if (engine.isStatusMove(move) && actor != target
                    && isSubstituteActive(statusTarget) && !MoveRegistry.isSoundMove(move)) {
                targetLog.put("result", "blocked");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(statusTarget.get("name") + " 的替身挡住了变化招式");
                continue;
            }
            if (engine.isStatusMove(move)
                    && handleStatusMove(state, action, statusSource, statusTarget, move, targetLog, events, random, round, actionLogs)) {
                continue;
            }

            selfStatChangeTriggered = true;
            if (conditionSupport.applyDefenderAbilityImmunity(actor, target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }

            // Counter / Mirror Coat / Metal Burst：读取本回合受击记录并返伤
            if (MoveRegistry.isReverseDamageMove(move)) {
                if (handleReverseDamageMove(actor, target, move, targetLog, events)) {
                    actionLogs.add(targetLog);
                    continue;
                }
                // 如果没有受击记录，招式失败
                targetLog.put("result", "failed");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(actor.get("name") + " 的 " + move.get("name") + " 失败了（本回合未受对应伤害）");
                continue;
            }

            // Substitute: 替身在场时，伤害先扣替身 HP，替身消失后剩余伤害继续
            Object subHpObj = engine.volatileValue(target, "substitute", null);
            int subHp = subHpObj instanceof Integer ? (Integer) subHpObj : 0;

            // 垂死挣扎：伤害 = 目标 HP - 使用者 HP（至少 1）
            if (MoveRegistry.isEndeavor(move)) {
                int actorHp = engine.toInt(actor.get("currentHp"), 0);
                int targetHp = engine.toInt(target.get("currentHp"), 0);
                if (targetHp <= actorHp) {
                    targetLog.put("result", "failed");
                    targetLog.put("damage", 0);
                    actionLogs.add(targetLog);
                    events.add(actor.get("name") + " 使用了垂死挣扎，但失败了");
                    continue;
                }
                int endeavorDamage = Math.max(1, targetHp - actorHp);
                int endeavorRemainingHp = Math.max(0, targetHp - endeavorDamage);
                target.put("currentHp", endeavorRemainingHp);
                targetLog.put("damage", endeavorDamage);
                targetLog.put("result", "hit");
                targetLog.put("hitCount", 1);
                targetLog.put("critical", false);
                totalDamage = endeavorDamage;
                anyHit = true;
                targetLog.put("targetHpAfter", endeavorRemainingHp);
                actionLogs.add(targetLog);
                events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 使用垂死挣扎"
                        + " 对 " + target.get("name") + " 造成了 " + endeavorDamage + " 点伤害");
                if (endeavorRemainingHp == 0) {
                    target.put("status", "fainted");
                    applyOnKOTargetAbility(state, actor, target, targetLog, events);
                }
                continue;
            }

            // Dream Eater: 目标必须在睡眠中，否则失败
            if (MoveRegistry.isDreamEater(move) && !"sleep".equals(target.get("condition"))) {
                targetLog.put("result", "failed");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(actor.get("name") + " 使用了食梦，但 " + target.get("name") + " 没有睡着");
                continue;
            }
            int hitCount = resolveHitCount(actor, move, random);
            int totalActualDamage = 0;
            int criticalHits = 0;
            int remainingHp = engine.toInt(target.get("currentHp"), 0);
            List<Integer> hitDamages = new ArrayList<>();
            Map<String, Object> baseDamageMove = moveForDamage;
            if (engine.isKnockOff(move) && conditionSupport.knockOffGetsBoost(target)) {
                baseDamageMove = new LinkedHashMap<>(moveForDamage);
                baseDamageMove.put("power", (int) Math.floor(engine.toInt(move.get("power"), 1) * 1.5d));
                targetLog.put("knockOffBoosted", true);
            }

            // 杂技（Acrobatics）：无道具时威力翻倍
            if (engine.isAcrobatics(move)) {
                String actorItem = engine.heldItem(actor);
                if (actorItem.isBlank() || Boolean.TRUE.equals(actor.get("itemConsumed"))) {
                    baseDamageMove = new LinkedHashMap<>(baseDamageMove);
                    baseDamageMove.put("power", engine.toInt(move.get("power"), 55) * 2);
                    targetLog.put("acrobaticsBoosted", true);
                    events.add(actor.get("name") + " 的杂技因为无道具而威力翻倍！");
                }
            }

            // Fire ON_BEFORE_MOVE — 允许特性/道具修改招式威力
            {
                Map<String, Object> ctx = Map.of(
                    "source", actor, "target", target, "move", move, "state", state
                );
                ModifyPowerEvent powerEvent = new ModifyPowerEvent(engine.toInt(baseDamageMove.get("power"), 0));
                EventResult evtResult = engine.getEventBus().fireEvent(BattleEventType.ON_BEFORE_MOVE, powerEvent, ctx);
                if (!evtResult.shouldContinue()) {
                    targetLog.put("result", "blocked");
                    targetLog.put("damage", 0);
                    actionLogs.add(targetLog);
                    events.add(actor.get("name") + " 的招式被特性阻止了");
                    continue;
                }
                int finalPower = evtResult.isModified() ? evtResult.getModifiedValue() : powerEvent.getModifiedPower();
                if (finalPower != engine.toInt(baseDamageMove.get("power"), 0) && finalPower > 0) {
                    baseDamageMove = new LinkedHashMap<>(baseDamageMove);
                    baseDamageMove.put("power", finalPower);
                }
            }

            for (int hitIndex = 0; hitIndex < hitCount && remainingHp > 0; hitIndex++) {
                // 逐次命中招式：每段独立判定命中（Population Bomb/Triple Axel/Triple Kick）
                if (isPerHitAccuracyMove(move)) {
                    int hitAcc = calculateAccuracyWithStages(state, actor, target, move);
                    if (random.nextInt(100) + 1 > hitAcc) {
                        events.add(move.get("name") + " 在第 " + (hitIndex + 1) + " 段落空了");
                        break; // miss 终止后续段数
                    }
                }

                int hpBeforeDamage = engine.toInt(target.get("currentHp"), 0);
                // 暴击在执行层预先解析，再传给伤害层消费，避免重复判定导致日志与数值不一致。
                boolean criticalHit = resolveCriticalHit(actor, move, random);
                Map<String, Object> resolvedMove = new LinkedHashMap<>(baseDamageMove);
                resolvedMove.put("criticalHit", criticalHit);

                // 逐段威力递增招式（Triple Axel: 20/40/60, Triple Kick: 10/20/30）
                if (isIncrementalPowerMove(move)) {
                    int basePow = engine.toInt(baseDamageMove.get("power"), 0);
                    resolvedMove.put("power", basePow * (hitIndex + 1));
                }

                if (criticalHit) {
                    criticalHits += 1;
                }
                int damage = engine.calculateDamage(actor, target, resolvedMove, random, helpingHandBoosts, state);

                // 威力为 0 的招式应该失败（如 Spit-up 无蓄力、Natural Gift 无树果等）
                if (damage == 0) {
                    targetLog.put("result", "failed");
                    targetLog.put("damage", 0);
                    actionLogs.add(targetLog);
                    events.add(actor.get("name") + " 的 " + move.get("name") + " 失败了");
                    continue;
                }

                // Fire ON_DAMAGE — 允许特性/道具修改伤害量
                {
                    EventResult dr = engine.getEventBus().fireEvent(BattleEventType.ON_DAMAGE,
                        new DamageEvent(damage, damage, criticalHit),
                        Map.of("attacker", actor, "defender", target, "move", move, "state", state));
                    if (!dr.shouldContinue()) {
                        damage = 0;
                    } else if (dr.isModified()) {
                        damage = dr.getModifiedValue();
                    } else {
                        damage = Math.max(0, damage);
                    }
                }

                // Ice Face / Disguise: block first hit per switch-in（光子喷涌/暗影之光无视）
                if (damage > 0) {
                    boolean ignoresAbility = MoveRegistry.isUnignorableMove(move);
                    String blockAbility = engine.abilityName(target);
                    if (!ignoresAbility && ("ice-face".equalsIgnoreCase(blockAbility) || "ice face".equalsIgnoreCase(blockAbility))
                            && target.get("iceFaceActive") != Boolean.FALSE) {
                        target.put("iceFaceActive", false);
                        targetLog.put("result", "blocked");
                        targetLog.put("damage", 0);
                        events.add(target.get("name") + " 的冰鳞粉挡住了攻击");
                        continue;
                    }
                    if (!ignoresAbility && "disguise".equalsIgnoreCase(blockAbility)
                            && target.get("disguiseActive") != Boolean.FALSE) {
                        target.put("disguiseActive", false);
                        targetLog.put("result", "blocked");
                        targetLog.put("damage", 0);
                        events.add(target.get("name") + " 的画皮挡住了攻击");
                        continue;
                    }
                }

                // Substitute absorbs damage
                if (subHp > 0 && damage > 0) {
                    int absorbed = Math.min(subHp, damage);
                    subHp -= absorbed;
                    damage -= absorbed;
                    targetLog.put("subHp", subHp);
                    if (subHp <= 0) {
                        engine.setVolatile(target, "substitute", 0);
                        events.add(target.get("name") + " 的替身消失了");
                    } else {
                        engine.setVolatile(target, "substitute", subHp);
                    }
                }

                boolean subActiveDuringHit = subHp > 0;
                remainingHp = engine.applyIncomingDamage(actor, target, damage, targetLog, events, move);
                int actualDamage = engine.toInt(targetLog.get("damage"), damage);
                target.put("currentHp", remainingHp);
                // 气势头带：10% 概率撑住致命一击（不消耗道具）
                if (remainingHp <= 0 && "focus-band".equalsIgnoreCase(engine.heldItem(target))
                        && random.nextInt(100) < 10) {
                    remainingHp = 1;
                    target.put("currentHp", 1);
                    // 气势头带只阻止濒死，不清除状态异常
                    targetLog.put("damage", Math.min(actualDamage, engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1) - 1));
                    events.add(target.get("name") + " 靠气势头带撑住了攻击");
                }
                conditionSupport.thawFromFireHit(target, move, targetLog, events);
                // 跃跃欲逃/懦弱：HP降至50%以下时自动换下（仅伤害后触发，不致死时）
                String eeAbility = engine.abilityName(target);
                boolean hasEmergencyExit = "emergency-exit".equalsIgnoreCase(eeAbility)
                        || "emergency exit".equalsIgnoreCase(eeAbility)
                        || "wimp-out".equalsIgnoreCase(eeAbility)
                        || "wimp out".equalsIgnoreCase(eeAbility);
                if (actualDamage > 0 && remainingHp > 0 && hasEmergencyExit) {
                    int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1);
                    if (remainingHp * 2 <= maxHp) {
                        boolean eePlayerSide = targetRef.playerSide();
                        List<Map<String, Object>> eeTeam = engine.team(state, eePlayerSide);
                        List<Integer> eeSlots = engine.activeSlots(state, eePlayerSide);
                        int eeBenchIdx = engine.firstAvailableBench(eeTeam, eeSlots);
                        if (eeBenchIdx >= 0) {
                            Map<String, Object> eeSwitchedIn = eeTeam.get(eeBenchIdx);
                            conditionSupport.applySwitchOutEffects(target, events);
                            target.put("choiceLockedMove", null);
                            conditionSupport.resetBattleStages(target);
                            eeSwitchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
                            engine.setVolatile(eeSwitchedIn, "flinch", false);
                            List<Integer> eeNewSlots = new ArrayList<>(eeSlots);
                            eeNewSlots.set(targetRef.fieldSlot(), eeBenchIdx);
                            state.put(eePlayerSide ? "playerActiveSlots" : "opponentActiveSlots", eeNewSlots);
                            targetLog.put("emergencyExit", true);
                            targetLog.put("switchTo", eeSwitchedIn.get("name"));
                            events.add(target.get("name") + " 的" + (eeAbility.contains("wimp") ? "懦弱" : "跃跃欲逃")
                                    + "特性发动，" + eeSwitchedIn.get("name") + " 被换上场！");
                            conditionSupport.applyEntryAbilities(state, eePlayerSide, eeSlots, events);
                            remainingHp = engine.toInt(target.get("currentHp"), 0); // sync remainingHp after switch
                        }
                    }
                }
                if (actualDamage > 0 && engine.isKnockOff(move)) {
                    conditionSupport.applyKnockOff(target, targetLog, events);
                }
                // Thief/Covet: 伤害后偷取目标道具（攻击者无道具且目标有可移除道具时）
                if (actualDamage > 0 && engine.isThiefMove(move)) {
                    String actorItem = engine.heldItem(actor);
                    String targetItem = engine.heldItem(target);
                    if (actorItem.isBlank() && !targetItem.isBlank() && !engine.itemConsumed(target)) {
                        target.put("heldItem", "");
                        actor.put("heldItem", targetItem);
                        targetLog.put("thiefItem", targetItem);
                        events.add(actor.get("name") + " 从 " + target.get("name") + " 身上偷走了 " + targetItem + "！");
                    }
                }
                // 虫灾（Bug Bite）：吃掉对方树果并获得效果
                if (actualDamage > 0 && engine.isBugBite(move)) {
                    String targetItem = engine.heldItem(target);
                    if (engine.isBerry(targetItem) && !engine.itemConsumed(target)) {
                        engine.consumeItem(target);
                        targetLog.put("bugBite", targetItem);
                        events.add(actor.get("name") + " 的虫灾吃掉了 " + target.get("name") + " 的" + targetItem + "！");
                        // 触发树果效果（对攻击者生效）
                        applyBerryEffect(actor, targetItem, events);
                    }
                }
                // 形态变化检查（zen-mode/schooling/shields-down）
                // 盐腌（Salt Cure）：命中后附加盐腌 volatile
                if (actualDamage > 0 && engine.isSaltCure(move) && remainingHp > 0) {
                    engine.setVolatile(target, "saltCured", true);
                    events.add(target.get("name") + " 被撒上了盐！");
                }
                conditionSupport.checkFormChange(target, state, events);
                if (subActiveDuringHit) events.add(target.get("name") + " 的替身承受了伤害");

                hitDamages.add(actualDamage);
                totalActualDamage += actualDamage;
                conditionSupport.applyReactiveDamageAbilities(state, actor, target, move, hpBeforeDamage, remainingHp, actualDamage, targetLog, events);
                // Throat Spray: sound move boosts SpA
                if ("throat-spray".equalsIgnoreCase(engine.heldItem(actor)) && !engine.itemConsumed(actor)
                        && actualDamage > 0 && MoveRegistry.isSoundMove(move)) {
                    engine.consumeItem(actor);
                    targetLog.put("throatSpray", true);
                    events.add(actor.get("name") + " 的喉喷触发了，特攻提升");
                }
                engine.applyDefenderItemEffects(state, target, move, actualDamage, targetLog, events, random);
                // Eject Button / Red Card 触发换人标记
                if (Boolean.TRUE.equals(targetLog.get("ejectButton"))) {
                    autoSwitchAfterMove(state, action, target, move, "eject-button", events, actionLogs);
                } else if (Boolean.TRUE.equals(targetLog.get("redCard")) && actualDamage > 0
                        && engine.toInt(actor.get("currentHp"), 0) > 0) {
                    autoSwitchAfterMove(state, action, actor, move, "red-card", events, actionLogs);
                }
                // Jaboca Berry: 受物理招式伤害时反伤攻击者 1/8 最大 HP
                if (actualDamage > 0 && !engine.itemConsumed(target) && remainingHp > 0) {
                    String targetItem = engine.heldItem(target);
                    int dmgClass = engine.toInt(move.get("damage_class_id"), 0);
                    if ("jaboca-berry".equalsIgnoreCase(targetItem)
                            && dmgClass == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
                        engine.consumeItem(target);
                        int atkMaxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
                        int recoil = Math.max(1, atkMaxHp / 8);
                        int atkHp = engine.toInt(actor.get("currentHp"), 0);
                        actor.put("currentHp", Math.max(0, atkHp - recoil));
                        events.add(target.get("name") + " 的嘉宝果反击了 " + actor.get("name") + "，" + recoil + " 点 HP 损伤");
                    }
                    // Rowap Berry: 受特殊招式伤害时反伤攻击者 1/8 最大 HP
                    if ("rowap-berry".equalsIgnoreCase(targetItem)
                            && dmgClass == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
                        engine.consumeItem(target);
                        int atkMaxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
                        int recoil = Math.max(1, atkMaxHp / 8);
                        int atkHp = engine.toInt(actor.get("currentHp"), 0);
                        actor.put("currentHp", Math.max(0, atkHp - recoil));
                        events.add(target.get("name") + " 的罗子果反击了 " + actor.get("name") + "，" + recoil + " 点 HP 损伤");
                    }
                }
                // 龙尾/巴投：伤害后强制目标换人（仅攻击招式，目标存活且对方有后备时触发）
                if (actualDamage > 0 && engine.isForcedSwitchMove(move) && !engine.isStatusMove(move)
                        && engine.toInt(target.get("currentHp"), 0) > 0) {
                    boolean tgtPS = targetRef.playerSide();
                    List<Map<String, Object>> tgtTeam = engine.team(state, tgtPS);
                    List<Integer> tgtSlots = engine.activeSlots(state, tgtPS);
                    int benchIdx = engine.firstAvailableBench(tgtTeam, tgtSlots);
                    if (benchIdx >= 0) {
                        Map<String, Object> switchedIn = tgtTeam.get(benchIdx);
                        conditionSupport.applySwitchOutEffects(target, events);
                        target.put("choiceLockedMove", null);
                        conditionSupport.resetBattleStages(target);
                        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
                        engine.setVolatile(switchedIn, "flinch", false);
                        List<Integer> newSlots = new ArrayList<>(tgtSlots);
                        newSlots.set(targetRef.fieldSlot(), benchIdx);
                        state.put(tgtPS ? "playerActiveSlots" : "opponentActiveSlots", newSlots);
                        targetLog.put("forcedSwitch", true);
                        targetLog.put("switchTo", switchedIn.get("name"));
                        events.add(target.get("name") + " 被 " + move.get("name") + " 击退了，" + switchedIn.get("name") + " 上场！");
                        conditionSupport.applyEntryAbilities(state, tgtPS, tgtSlots, events);
                    }
                }
                // Enigma Berry: 受效果绝佳攻击时回复 1/4 最大 HP（非克制或已消耗跳过）
                if (actualDamage > 0 && !engine.itemConsumed(target) && remainingHp > 0
                        && ("enigma-berry".equalsIgnoreCase(engine.heldItem(target)) || "enigma berry".equalsIgnoreCase(engine.heldItem(target)))
                        && engine.typeModifier(target, engine.toInt(move.get("type_id"), 0)) > 1.0d) {
                    int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1);
                    int currentHp = engine.toInt(target.get("currentHp"), 0);
                    int heal = Math.max(1, maxHp / 4);
                    if (currentHp < maxHp) {
                        engine.consumeItem(target);
                        int newHp = Math.min(maxHp, currentHp + heal);
                        target.put("currentHp", newHp);
                        remainingHp = newHp;
                        events.add(target.get("name") + " 的谜芝果回复了 " + heal + " 点 HP");
                    }
                }
                // Micle Berry: HP < 25% 时下一招式命中率 +1 阶级（标记挥发状态）
                if (actualDamage > 0 && !engine.itemConsumed(target) && remainingHp > 0
                        && engine.toInt(target.get("currentHp"), 0) * 4 <= engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1)
                        && ("micle-berry".equalsIgnoreCase(engine.heldItem(target)) || "micle berry".equalsIgnoreCase(engine.heldItem(target)))) {
                    engine.consumeItem(target);
                    engine.setVolatile(target, "micleBerryBoosted", true);
                    events.add(target.get("name") + " 的米库果提高了下一招式的命中率");
                }
                // Struggle recoil: 1/4 max HP
                if (isStruggleMove(move) && actualDamage > 0) {
                    int struggleRecoil = Math.max(1, engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1) / 4);
                    int actorHp = engine.toInt(actor.get("currentHp"), 0);
                    actor.put("currentHp", Math.max(0, actorHp - struggleRecoil));
                    targetLog.put("struggleRecoil", struggleRecoil);
                    events.add(actor.get("name") + " 因挣扎受到了 " + struggleRecoil + " 点反伤");
                    if (actorHp - struggleRecoil <= 0) {
                        actor.put("status", "fainted");
                    }
                }
                conditionSupport.applyReactiveContactEffects(state, actor, target, move, targetLog, events, random);
                // 多段攻击：每段独立判定王者之证/锐利之牙畏缩（仅招式无自然畏缩概率时生效）
                if (actualDamage > 0 && remainingHp > 0 && engine.toInt(move.get("flinch_chance"), 0) <= 0) {
                    String heldItem = engine.heldItem(actor);
                    if (("king's-rock".equalsIgnoreCase(heldItem) || "king's rock".equalsIgnoreCase(heldItem)
                            || "razor-fang".equalsIgnoreCase(heldItem) || "razor fang".equalsIgnoreCase(heldItem))
                            && random.nextInt(100) < 10
                            && !"inner-focus".equalsIgnoreCase(engine.abilityName(target))
                            && !"inner focus".equalsIgnoreCase(engine.abilityName(target))
                            && !conditionSupport.blocksSecondaryEffects(target, "flinch", targetLog, events)) {
                        engine.setVolatile(target, "flinch", true);
                        targetLog.put("flinch", true);
                        events.add(target.get("name") + " 因为" + heldItem + "畏缩了");
                        conditionSupport.checkSteadfast(target, targetLog, events);
                    }
                }
                // 多段攻击：每段独立判定恶臭特性畏缩
                if (actualDamage > 0 && remainingHp > 0 && engine.hasAbility(actor, "stench")
                        && !conditionSupport.blocksSecondaryEffects(target, "flinch", targetLog, events)
                        && random.nextInt(100) < 10) {
                    engine.setVolatile(target, "flinch", true);
                    targetLog.put("flinch", true);
                    events.add(target.get("name") + " 因为" + engine.abilityName(actor) + "特性而畏缩了");
                    conditionSupport.checkSteadfast(target, targetLog, events);
                }
                if (remainingHp == 0) {
                    target.put("status", "fainted");
                }
                if (engine.toInt(actor.get("currentHp"), 0) <= 0) {
                    break;
                }
            }

            targetLog.put("result", "hit");
            targetLog.put("damage", totalActualDamage);
            targetLog.put("hitCount", hitDamages.size());
            targetLog.put("hitDamages", hitDamages);
            targetLog.put("critical", criticalHits > 0);
            targetLog.put("criticalHits", criticalHits);
            targetLog.put("targetHpAfter", remainingHp);
            // Mortal Spin: 清除己方场地钉 + 中毒目标
            if (engine.isMortalSpin(move) && totalActualDamage > 0) {
                engine.clearSideHazards(state, "player".equals(action.side()));
                events.add(actor.get("name") + " 用毒旋陀螺清除了场地钉");
                if (engine.toInt(target.get("currentHp"), 0) > 0) {
                    conditionSupport.applyPoison(state, actor, target, move, targetLog, events, false);
                }
                engine.setVolatile(actor, "bound", false);
                engine.setVolatile(actor, "boundTurns", 0);
            }
            // Rapid Spin: 清除己方场地钉 + 速度提升 1 级
            if (MoveRegistry.isRapidSpin(move) && totalActualDamage > 0) {
                engine.clearSideHazards(state, "player".equals(action.side()));
                events.add(actor.get("name") + " 用高速旋转清除了场地钉");
                Map<String, Object> stages = engine.statStages(actor);
                int prevSpeed = engine.toInt(stages.get("speed"), 0);
                if (prevSpeed < 6) {
                    stages.put("speed", prevSpeed + 1);
                    targetLog.put("rapidSpinSpeedBoost", true);
                    events.add(actor.get("name") + " 的高速旋转提升了速度！");
                }
            }
            // Defog: 清除双方场地钉
            if (MoveRegistry.isDefog(move) && totalActualDamage > 0) {
                engine.clearSideHazards(state, true);
                engine.clearSideHazards(state, false);
                events.add(actor.get("name") + " 用清除浓雾清除了双方的场地钉");
            }
            actionLogs.add(targetLog);
            events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 使用 " + move.get("name")
                    + " 对 " + target.get("name") + " 造成了 " + totalActualDamage + " 点伤害");
            if (hitDamages.size() > 1) {
                events.add(move.get("name") + " 连续命中了 " + hitDamages.size() + " 次");
            }
            if (criticalHits > 0) {
                events.add(criticalHits == 1 ? "击中了要害" : "其中 " + criticalHits + " 次击中了要害");
            }

            // 记录本回合受击伤害（供 Counter/Mirror Coat/Metal Burst 使用）
            if (totalActualDamage > 0) {
                int dmgClass = engine.toInt(move.get("damage_class_id"), 0);
                if (dmgClass == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
                    engine.setVolatile(target, "lastTakenPhysDmg", totalActualDamage);
                } else if (dmgClass == DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL) {
                    engine.setVolatile(target, "lastTakenSpecDmg", totalActualDamage);
                }
            }

            conditionSupport.applyDrainHealing(actor, target, move, totalActualDamage, targetLog, events);
            totalDamage += totalActualDamage;
            anyHit = anyHit || totalActualDamage > 0;
            if (isFakeOut(move) && remainingHp > 0) {
                if ("inner-focus".equalsIgnoreCase(engine.abilityName(target)) || "inner focus".equalsIgnoreCase(engine.abilityName(target))) {
                    targetLog.put("flinchBlocked", true);
                    targetLog.put("ability", engine.abilityName(target));
                    events.add(target.get("name") + " 的特性让其不会畏缩");
                } else if (!conditionSupport.blocksSecondaryEffects(target, "flinch", targetLog, events)) {
                    engine.setVolatile(target, "flinch", true);
                    targetLog.put("flinch", true);
                    events.add(target.get("name") + " 畏缩了");
                    conditionSupport.checkSteadfast(target, targetLog, events);
                }
            }
            if ((engine.isIcyWind(move) || engine.isElectroweb(move)) && remainingHp > 0) {
                if (!conditionSupport.blocksSecondaryEffects(target, "speed-drop", targetLog, events)) {
                    conditionSupport.applySpeedDrop(actor, target, targetLog, events);
                }
            }
            if (engine.isSnarl(move) && remainingHp > 0) {
                if (!conditionSupport.blocksSecondaryEffects(target, "special-attack-drop", targetLog, events)) {
                    conditionSupport.applySpecialAttackDrop(actor, target, 1, targetLog, events);
                }
            }
            if (remainingHp > 0) {
                conditionSupport.applyDamagingSecondaryEffects(state, actor, target, move, targetLog, events, random);
                // Throat Chop: 命中后施加喉斩沉默效果 2 回合
                if (totalActualDamage > 0 && MoveRegistry.isThroatChop(move)) {
                    engine.setVolatile(target, "throatChopTurns", 2);
                    events.add(target.get("name") + " 被喉斩命中，无法使用声音类招式了！");
                }
                // Smack Down: 命中后强制目标地面化（无视飞行属性/飘浮特性）
                if (totalActualDamage > 0 && MoveRegistry.isSmackDown(move)) {
                    engine.setVolatile(target, "grounded", true);
                    events.add(target.get("name") + " 被击落了！");
                }
                // Jaw Lock: 命中后束缚双方，均无法换人
                if (totalActualDamage > 0 && MoveRegistry.isJawLock(move)) {
                    engine.setVolatile(target, "trapped", true);
                    engine.setVolatile(actor, "trapped", true);
                    events.add(actor.get("name") + " 和 " + target.get("name") + " 被大嚼咬咬住了，无法换人！");
                }
            }
            if (remainingHp == 0) {
                events.add(target.get("name") + " 倒下了");
                // 同命：如果目标使用了同命且被击倒，攻击者也一同倒下
                if (Boolean.TRUE.equals(engine.volatileValue(target, "destinyBond", false))
                        && engine.toInt(actor.get("currentHp"), 0) > 0) {
                    actor.put("currentHp", 0);
                    actor.put("status", "fainted");
                    events.add(target.get("name") + " 的同命带走了 " + actor.get("name") + "！");
                }
                // 击倒触发特性：moxie/beast-boost/soul-heart/grim-neigh/chilling-neigh
                applyOnKOTargetAbility(state, actor, target, targetLog, events);
            }
        }

        if (!liveTargetFound) {
            actionLog.put("result", "failed");
            actionLog.put("damage", 0);
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了 " + move.get("name") + "，但失败了");
            engine.rememberLastMove(actor, move);
            engine.rememberChoiceMove(actor, move);
            engine.applyCooldown(actor, move);
            return;
        }

        if (engine.isSuckerPunch(move) && !suckerPunchAttempted) {
            actionLog.put("result", "failed");
            actionLog.put("damage", 0);
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了 Sucker Punch，但失败了");
            return;
        }
        if (selfStatChangeTriggered && anyHit && engine.toInt(actor.get("currentHp"), 0) > 0) {
            conditionSupport.applyDamagingSelfStatChanges(actor, move, actionLog, events, random);
        }
        if (anyHit) {
            engine.applyAttackerItemEffects(state, actor, totalDamage, actionLog, events);
        }
        if (anyHit && engine.isRechargeMove(move) && engine.toInt(actor.get("currentHp"), 0) > 0) {
            actor.put("rechargeTurns", 1);
            actionLog.put("rechargeNextTurn", true);
            events.add(actor.get("name") + " 下回合需要回复，无法行动");
        }
        if (forcedChargeMove != null) {
            clearCharging(actor);
        }
        actor.remove("zMoveRound");
        actor.remove("zMoveBase");
        if ("z-move".equals(actor.get("specialSystemActivated"))) {
            actor.put("specialSystemActivated", null);
        }
        if (anyHit && engine.isPivotSwitchMove(move) && engine.toInt(actor.get("currentHp"), 0) > 0) {
            autoSwitchAfterMove(state, action, actor, move, "pivot-switch", events, actionLogs);
            return;
        }

        // Eject Pack: 扫描双方所有活跃宝可梦的逃脱背包触发标记，执行换人
        for (boolean scanSide : new boolean[]{true, false}) {
            for (Integer slot : new ArrayList<>(engine.activeSlots(state, scanSide))) {
                List<Map<String, Object>> scanTeam = engine.team(state, scanSide);
                if (slot < 0 || slot >= scanTeam.size()) continue;
                Map<String, Object> scanned = scanTeam.get(slot);
                if (Boolean.TRUE.equals(scanned.get("ejectPackTriggered"))) {
                    scanned.put("ejectPackTriggered", false);
                    String scanSideStr = scanSide ? "player" : "opponent";
                    List<Integer> scanSlots = engine.activeSlots(state, scanSide);
                    int benchIdx = engine.firstAvailableBench(scanTeam, scanSlots);
                    if (benchIdx < 0) continue;
                    Map<String, Object> switchedIn = scanTeam.get(benchIdx);
                    conditionSupport.applySwitchOutEffects(scanned, events);
                    scanned.put("choiceLockedMove", null);
                    conditionSupport.resetBattleStages(scanned);
                    switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
                    engine.setVolatile(switchedIn, "flinch", false);
                    List<Integer> newSlots = new ArrayList<>(scanSlots);
                    newSlots.set(scanSlots.indexOf(slot), benchIdx);
                    state.put(scanSide ? "playerActiveSlots" : "opponentActiveSlots", newSlots);
                    Map<String, Object> ejectSwitchLog = new LinkedHashMap<>();
                    ejectSwitchLog.put("side", scanSideStr);
                    ejectSwitchLog.put("actor", scanned.get("name"));
                    ejectSwitchLog.put("actionType", "switch");
                    ejectSwitchLog.put("switchTo", switchedIn.get("name"));
                    ejectSwitchLog.put("result", "eject-pack");
                    actionLogs.add(ejectSwitchLog);
                    events.add("逃脱背包触发，" + engine.sideName(scanSideStr) + " 收回了 "
                            + scanned.get("name") + "，派出了 " + switchedIn.get("name"));
                    conditionSupport.applyEntryAbilities(state, scanSide, scanSlots, events);
                    break; // 每边最多处理一个
                }
            }
        }

        // Stance Change: 坚盾剑怪使用招式后切换形态
        boolean isAttackerAlive = engine.toInt(actor.get("currentHp"), 0) > 0;
        if (isAttackerAlive && !action.isSwitch()) {
            conditionSupport.checkStanceChangeAfterMove(actor, move);
        }
        // 驱动能量（Booster Energy）：发动后消耗
        if (isAttackerAlive && "booster-energy".equals(engine.heldItem(actor)) && !engine.itemConsumed(actor)) {
            engine.consumeItem(actor);
            events.add(actor.get("name") + " 的驱动能量发动了并被消耗！");
        }
        // 攻击者形态变化检查
        if (isAttackerAlive) conditionSupport.checkFormChange(actor, state, events);

        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move, state);
    }

    private boolean canSuckerPunchTarget(Map<String, BattleEngine.Action> plannedActions,
                                         BattleEngine.TargetRef targetRef,
                                         Map<String, Object> move) {
        BattleEngine.Action plannedTargetAction = plannedActions.get(engine.actionKey(targetRef.side(), targetRef.teamIndex()));
        if (plannedTargetAction == null || plannedTargetAction.isSwitch() || plannedTargetAction.move() == null) {
            return false;
        }
        Map<String, Object> plannedMove = plannedTargetAction.move();
        return engine.toInt(plannedMove.get("power"), 0) > 0
                && !engine.isStatusMove(plannedMove)
                && !MoveRegistry.isProtectionMove(plannedMove);
    }

    private int resolveHitCount(Map<String, Object> actor, Map<String, Object> move, Random random) {
        int minHits = Math.max(1, engine.toInt(move.get("min_hits"), 0));
        int maxHits = Math.max(minHits, engine.toInt(move.get("max_hits"), 0));
        if (engine.toInt(move.get("max_hits"), 0) <= 0) {
            maxHits = minHits;
        }
        // Population Bomb 数据补偿：meta 为 null 时 min_hits/max_hits 为 0
        String moveNameEn = String.valueOf(move.getOrDefault("name_en", "")).toLowerCase();
        if ("population-bomb".equals(moveNameEn) && maxHits <= 1) {
            minHits = 1;
            maxHits = 10;
        }
        if (maxHits <= 1) {
            return 1;
        }
        String ability = engine.abilityName(actor);
        if ("skill-link".equalsIgnoreCase(ability) || "skill link".equalsIgnoreCase(ability)) {
            return maxHits;
        }
        String heldItem = engine.heldItem(actor);
        if (("loaded-dice".equalsIgnoreCase(heldItem) || "loaded dice".equalsIgnoreCase(heldItem))
                && minHits == 2 && maxHits == 5) {
            return random.nextBoolean() ? 4 : 5;
        }
        if (minHits == 2 && maxHits == 5) {
            int roll = random.nextInt(100);
            if (roll < 35) {
                return 2;
            }
            if (roll < 70) {
                return 3;
            }
            if (roll < 85) {
                return 4;
            }
            return 5;
        }
        return minHits + random.nextInt(maxHits - minHits + 1);
    }

    /** 逐次命中招式：每段独立判定命中 */
    private boolean isPerHitAccuracyMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.getOrDefault("name_en", "")).toLowerCase();
        return "population-bomb".equals(nameEn)
            || "triple-axel".equals(nameEn)
            || "triple-kick".equals(nameEn);
    }

    /** 逐段威力递增招式 */
    private boolean isIncrementalPowerMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.getOrDefault("name_en", "")).toLowerCase();
        return "triple-axel".equals(nameEn) || "triple-kick".equals(nameEn);
    }

    /** 虫灾吞食树果后触发效果 */
    private void applyBerryEffect(Map<String, Object> mon, String berryName, List<String> events) {
        if (berryName == null || berryName.isBlank()) return;
        String name = berryName.toLowerCase();

        // 回复类树果
        if (name.contains("sitrus") || name.contains("文柚")) {
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int curHp = engine.toInt(mon.get("currentHp"), 0);
            int heal = Math.max(1, maxHp / 4);
            mon.put("currentHp", Math.min(maxHp, curHp + heal));
            events.add(mon.get("name") + " 通过树果回复了 " + heal + " 点 HP");
        } else if (name.contains("lum") || name.contains("木子")) {
            // 木子果：治愈所有状态异常
            if (mon.get("condition") != null && !"fainted".equals(mon.get("condition"))) {
                mon.put("condition", null);
                mon.put("status", "");
                events.add(mon.get("name") + " 的状态异常被治愈了");
            }
        } else if (name.contains("oran") || name.contains("橙")) {
            // 橙橙果：回复 10 HP
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int curHp = engine.toInt(mon.get("currentHp"), 0);
            mon.put("currentHp", Math.min(maxHp, curHp + 10));
            events.add(mon.get("name") + " 通过树果回复了 10 点 HP");
        }
        // 能力提升类树果可在此扩展
    }

    private Map<String, Object> chargingMove(Map<String, Object> actor) {
        String chargingMoveName = String.valueOf(actor.getOrDefault("chargingMove", ""));
        if (chargingMoveName.isBlank()) {
            return null;
        }
        for (Map<String, Object> move : engine.moves(actor)) {
            if (chargingMoveName.equalsIgnoreCase(String.valueOf(move.get("name_en")))) {
                return move;
            }
        }
        clearCharging(actor);
        return null;
    }

    private boolean shouldStartCharging(Map<String, Object> actor, Map<String, Object> move, Map<String, Object> state) {
        if (!engine.isChargeMove(move)) {
            return false;
        }
        String heldItem = engine.heldItem(actor);
        if ("power-herb".equalsIgnoreCase(heldItem) || "power herb".equalsIgnoreCase(heldItem)) {
            engine.consumeItem(actor);
            return false;
        }
        String nameEn = String.valueOf(move.get("name_en"));
        if (matches(nameEn, "solar-beam", "solar beam", "solar-blade", "solar blade")
                && engine.toInt(engine.castMap(state.get("fieldEffects")).get("sunTurns"), 0) > 0) {
            return false;
        }
        return true;
    }

    private void startCharging(Map<String, Object> actor, BattleEngine.Action action, Map<String, Object> move,
                               Map<String, Object> actionLog,
                               List<Map<String, Object>> actionLogs, List<String> events) {
        actor.put("chargingMove", move.get("name_en"));
        actor.put("chargingTurns", 1);
        actor.put("chargingTargetTeamIndex", action.targetTeamIndex());
        actor.put("chargingTargetFieldSlot", action.targetFieldSlot());

        // 半无敌蓄力招式（飞空/挖洞等）：蓄力期间进入半无敌状态
        if (MoveRegistry.isSemiInvulnerableChargeMove(move)) {
            engine.setVolatile(actor, "semiInvulnerable", true);
            events.add(actor.get("name") + " 进入了半无敌状态");
        }

        // 流星光束：蓄力时特攻提升 1 级
        String nameEn = String.valueOf(move.get("name_en"));
        if ("meteor-beam".equalsIgnoreCase(nameEn) || "meteor beam".equalsIgnoreCase(nameEn)) {
            Map<String, Object> stages = engine.statStages(actor);
            int previousStage = engine.toInt(stages.get("specialAttack"), 0);
            int nextStage = Math.min(6, previousStage + 1);
            stages.put("specialAttack", nextStage);
            actionLog.put("spaBoostedByCharge", true);
            events.add(actor.get("name") + " 正在蓄力，特攻提升了！");
        }

        actionLog.put("result", "charge");
        actionLog.put("charging", true);
        actionLogs.add(actionLog);
        events.add(actor.get("name") + " 正在蓄力");
    }

    private void clearCharging(Map<String, Object> actor) {
        actor.put("chargingMove", null);
        actor.put("chargingTurns", 0);
        actor.put("chargingTargetTeamIndex", -1);
        actor.put("chargingTargetFieldSlot", -1);
        engine.setVolatile(actor, "semiInvulnerable", false);
    }

    private boolean resolveCriticalHit(Map<String, Object> actor, Map<String, Object> move, Random random) {
        return engine.rollCriticalHit(actor, move, random);
    }

    /** 封印（Imprison）：检查对方场上是否有宝可梦激活了封印且拥有相同招式 */
    private boolean isBlockedByImprison(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> move) {
        boolean isPlayer = engine.isOnSide(state, actor, true);
        for (Integer slot : engine.activeSlots(state, !isPlayer)) {
            if (slot == null || slot < 0) continue;
            List<Map<String, Object>> team = engine.team(state, !isPlayer);
            if (slot >= team.size()) continue;
            Map<String, Object> mon = team.get(slot);
            if (!Boolean.TRUE.equals(engine.volatileValue(mon, "imprisonActive", false))) continue;
            // 检查对方是否拥有相同招式
            for (Map<String, Object> m : engine.moves(mon)) {
                String monMoveName = String.valueOf(m.get("name_en")).toLowerCase();
                String actorMoveName = String.valueOf(move.get("name_en")).toLowerCase();
                if (monMoveName.equals(actorMoveName)) return true;
            }
        }
        return false;
    }

    private boolean isBlockedByTrappingAbility(Map<String, Object> state, boolean playerSide, Map<String, Object> actor) {
        if (engine.hasAbility(actor, "shadow-tag", "shadow tag")) {
            return false; // trapper can switch freely
        }
        boolean isFlying = engine.targetHasType(actor, DamageCalculatorUtil.TYPE_FLYING);
        boolean isGhost = engine.targetHasType(actor, DamageCalculatorUtil.TYPE_GHOST);
        for (Map<String, Object> opp : engine.team(state, !playerSide)) {
            if (engine.toInt(opp.get("currentHp"), 0) <= 0) continue;
            String ab = engine.abilityName(opp);
            if ("shadow-tag".equalsIgnoreCase(ab) || "shadow tag".equalsIgnoreCase(ab)) {
                if (!isGhost) return true; // 幽灵属性不受 Shadow Tag 影响
            }
            if (("arena-trap".equalsIgnoreCase(ab) || "arena trap".equalsIgnoreCase(ab)) && !isFlying) {
                return true;
            }
            if (("magnet-pull".equalsIgnoreCase(ab) || "magnet pull".equalsIgnoreCase(ab))
                    && engine.targetHasType(actor, DamageCalculatorUtil.TYPE_STEEL)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInfatuatedAndBlocked(Map<String, Object> actor, List<Map<String, Object>> actionLogs,
                                           List<String> events, Random random) {
        if (Boolean.TRUE.equals(actor.get("infatuated")) && random.nextInt(2) == 0) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("actor", actor.get("name"));
            log.put("actionType", "infatuation");
            log.put("result", "infatuated");
            actionLogs.add(log);
            events.add(actor.get("name") + " 因着迷而无法行动");
            return true;
        }
        return false;
    }

    private boolean isSubstituteActive(Map<String, Object> mon) {
        Object sub = engine.volatileValue(mon, "substitute", null);
        return sub instanceof Integer i && i > 0;
    }

    private boolean isStruggleMove(Map<String, Object> move) {
        return "struggle".equalsIgnoreCase(String.valueOf(move.get("name_en")));
    }

    private boolean matches(String value, String... names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 击倒触发特性：击倒目标时，根据特性提升对应能力
     */
    private void applyOnKOTargetAbility(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target, Map<String, Object> actionLog, List<String> events) {
        if (engine.toInt(actor.get("currentHp"), 0) <= 0) {
            return;
        }
        String ability = engine.abilityName(actor);
        Map<String, Object> stages = engine.statStages(actor);
        boolean anyBoosted = false;

        // Moxie / 自信过度：击倒对手攻击 +1
        if ("moxie".equalsIgnoreCase(ability)) {
            int prev = engine.toInt(stages.get("attack"), 0);
            if (prev < 6) {
                stages.put("attack", prev + 1);
                actionLog.put("moxie", true);
                events.add(actor.get("name") + " 的自信过度触发了，攻击提升！");
                anyBoosted = true;
            }
        }
        // Beast Boost / 异兽提升：击倒后最高能力 +1
        if ("beast-boost".equalsIgnoreCase(ability) || "beast boost".equalsIgnoreCase(ability)) {
            String[] statNames = {"attack", "defense", "specialAttack", "specialDefense", "speed"};
            String highestStat = "attack";
            int highestVal = Integer.MIN_VALUE;
            Map<String, Object> stats = engine.castMap(actor.get("stats"));
            for (String s : statNames) {
                int val = engine.toInt(stats.get(s), 0);
                if (val > highestVal) {
                    highestVal = val;
                    highestStat = s;
                }
            }
            int prev = engine.toInt(stages.get(highestStat), 0);
            if (prev < 6) {
                stages.put(highestStat, prev + 1);
                actionLog.put("beastBoost", highestStat);
                events.add(actor.get("name") + " 的异兽提升了" + highestStat + "！");
                anyBoosted = true;
            }
        }
        // Soul-Heart / 魂心：任何击倒特攻 +1
        if ("soul-heart".equalsIgnoreCase(ability) || "soul heart".equalsIgnoreCase(ability)) {
            int prev = engine.toInt(stages.get("specialAttack"), 0);
            if (prev < 6) {
                stages.put("specialAttack", prev + 1);
                actionLog.put("soulHeart", true);
                events.add(actor.get("name") + " 的魂心触发了，特攻提升！");
                anyBoosted = true;
            }
        }
        // Grim Neigh / 漆黑嘶鸣：击倒后特攻 +1
        if ("grim-neigh".equalsIgnoreCase(ability) || "grim neigh".equalsIgnoreCase(ability)) {
            int prev = engine.toInt(stages.get("specialAttack"), 0);
            if (prev < 6) {
                stages.put("specialAttack", prev + 1);
                actionLog.put("grimNeigh", true);
                events.add(actor.get("name") + " 的漆黑嘶鸣触发了，特攻提升！");
                anyBoosted = true;
            }
        }
        // Chilling Neigh / 苍白嘶鸣：击倒后攻击 +1
        if ("chilling-neigh".equalsIgnoreCase(ability) || "chilling neigh".equalsIgnoreCase(ability)) {
            int prev = engine.toInt(stages.get("attack"), 0);
            if (prev < 6) {
                stages.put("attack", prev + 1);
                actionLog.put("chillingNeigh", true);
                events.add(actor.get("name") + " 的苍白嘶鸣触发了，攻击提升！");
                anyBoosted = true;
            }
        }
        // Mirror Herb: 复制击倒触发的特性提升
        if (anyBoosted) {
            conditionSupport.tryMirrorHerb(state, actor, events);
        }

        // Battle Bond (牵绊变身): 甲贺忍蛙击倒对手后形态变化
        if (target != null && !Boolean.TRUE.equals(actor.get("battleBondActivated"))) {
            String actorAb = engine.abilityName(actor);
            if ("battle-bond".equalsIgnoreCase(actorAb) || "battle bond".equalsIgnoreCase(actorAb)) {
                actor.put("battleBondActivated", true);
                engine.setVolatile(actor, "battleBondForm", true);
                events.add(actor.get("name") + " 的牵绊变身发动了，变成了小智版甲贺忍蛙！");
            }
        }

        // Innards Out: dealt equal damage to attacker when fainted
        if (target != null) {
            String tgtAb = engine.abilityName(target);
            if ("innards-out".equalsIgnoreCase(tgtAb) || "innards out".equalsIgnoreCase(tgtAb)) {
                int fatalDmg = engine.toInt(actionLog.get("damage"), 0);
                if (fatalDmg > 0 && engine.toInt(actor.get("currentHp"), 0) > 0) {
                    int newHp = Math.max(0, engine.toInt(actor.get("currentHp"), 0) - fatalDmg);
                    actor.put("currentHp", newHp);
                    events.add(target.get("name") + " 的飞出的内在物对 " + actor.get("name") + " 造成了 " + fatalDmg + " 点伤害！");
                    if (newHp <= 0) { actor.put("status", "fainted"); events.add(actor.get("name") + " 被反杀了！"); }
                }
            }
        }
    }

    private void handleSwitch(Map<String, Object> state, BattleEngine.Action action, List<Map<String, Object>> actingTeam,
                              Map<String, Object> actor, boolean playerSide, List<Map<String, Object>> actionLogs,
                              List<String> events, Map<String, Object> actionLog) {
        // 极巨化期间禁止主动换人（正作规则）
        if (Boolean.TRUE.equals(actor.get("dynamaxed"))) {
            actionLog.put("result", "dynamax-block-switch");
            events.add(actor.get("name") + " 正处于极巨化状态，无法换人");
            return;
        }
        // Shed Shell 绕过所有捕获效果
        boolean hasShedShell = "shed-shell".equalsIgnoreCase(engine.heldItem(actor));
        // 捕获招式检查（Mean Look/Block 等）
        if (!hasShedShell && Boolean.TRUE.equals(actor.get("trapped"))) {
            actionLog.put("result", "trapped");
            events.add(actor.get("name") + " 被困住了，无法换人");
            return;
        }
        // 捕获特性检查
        if (!hasShedShell && isBlockedByTrappingAbility(state, playerSide, actor)) {
            actionLog.put("result", "trapped");
            events.add(actor.get("name") + " 被对手的特性困住了，无法换人");
            return;
        }
        if (!engine.canSwitch(actingTeam, engine.activeSlots(state, playerSide), action.actorFieldSlot(), action.switchToTeamIndex())) {
            return;
        }
        List<Integer> previousSlots = new ArrayList<>(engine.activeSlots(state, playerSide));
        Map<String, Object> switchedIn = actingTeam.get(action.switchToTeamIndex());
        actionLog.put("actionType", "switch");
        actionLog.put("switchTo", switchedIn.get("name"));
        conditionSupport.applySwitchOutEffects(actor, events);
        actor.put("choiceLockedMove", null);
        conditionSupport.resetBattleStages(actor);
        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
        engine.setVolatile(switchedIn, "flinch", false);
        engine.replaceActiveSlot(state, playerSide, action.actorFieldSlot(), action.switchToTeamIndex());

        // Fire ON_SWITCH_OUT / ON_SWITCH_IN
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_OUT,
            new BattleEvent(BattleEventType.ON_SWITCH_OUT) {},
            Map.of("source", actor, "playerSide", playerSide, "state", state));
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_IN,
            new BattleEvent(BattleEventType.ON_SWITCH_IN) {},
            Map.of("source", switchedIn, "playerSide", playerSide, "state", state));

        // 标记换入用于监查特性（Stakeout 等）
        switchedIn.put("justSwitchedIn", true);

        actionLogs.add(actionLog);
        events.add(engine.sideName(action.side()) + " 收回了 " + actor.get("name") + "，派出了 " + switchedIn.get("name"));
        conditionSupport.applyEntryAbilities(state, playerSide, previousSlots, events);
    }

    private boolean handleSupportMove(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                      Map<String, Object> move, int round, Random random,
                                      Map<String, Boolean> protectedTargets,
                                      Map<String, Boolean> wideGuardSides,
                                      Map<String, Boolean> quickGuardSides,
                                      Map<String, BattleEngine.RedirectionEffect> redirectionTargets,
                                      Map<Map<String, Object>, Boolean> helpingHandBoosts,
                                      List<Map<String, Object>> actionLogs, List<String> events,
                                      Map<String, Object> actionLog, boolean playerSide) {
          if (MoveRegistry.isProtectionMove(move)) {
            return handleProtectionMove(state, actor, move, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                      action.side(), action.actorIndex(), actionLog, actionLogs, events);
          }
        if (engine.isTailwind(move)) {
            engine.activateTailwind(state, playerSide, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isTrickRoom(move)) {
            engine.toggleTrickRoom(state, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 魔法空间（Magic Room）：5 回合内所有道具效果失效
        if (engine.isMagicRoom(move)) {
            engine.toggleMagicRoom(state, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 奇妙空间（Wonder Room）：5 回合物防/特防互换
        if (engine.isWonderRoom(move)) {
            engine.toggleWonderRoom(state, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        if (MoveRegistry.isGravity(move)) {
            engine.activateGravity(state, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isRainDance(move)) {
            engine.activateWeather(state, "rain", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isSunnyDay(move)) {
            engine.activateWeather(state, "sun", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isSandstorm(move)) {
            engine.activateWeather(state, "sand", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isSnowWeather(move)) {
            engine.activateWeather(state, "snow", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isElectricTerrain(move)) {
            engine.activateTerrain(state, "electric", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isPsychicTerrain(move)) {
            engine.activateTerrain(state, "psychic", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isGrassyTerrain(move)) {
            engine.activateTerrain(state, "grassy", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isMistyTerrain(move)) {
            engine.activateTerrain(state, "misty", actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isReflect(move)) {
            engine.activateScreen(state, "reflect", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isLightScreen(move)) {
            engine.activateScreen(state, "light-screen", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isAuroraVeil(move)) {
            if (engine.snowTurns(state) <= 0) {
                actionLog.put("result", "failed");
                actionLogs.add(actionLog);
                events.add(actor.get("name") + " 想展开极光幕，但当前没有雪天");
                return true;
            }
            engine.activateScreen(state, "aurora-veil", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isSafeguard(move)) {
            engine.activateScreen(state, "safeguard", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isRedirectionMove(move)) {
            targetSupport.activateRedirection(redirectionTargets, action.side(), action.actorIndex(), move, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isHelpingHand(move)) {
            targetSupport.applyHelpingHand(state, action, actor, move, actionLog, events, helpingHandBoosts);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (engine.isAllySwitch(move)) {
            targetSupport.applyAllySwitch(state, action, actor, actionLog, events);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        if (MoveRegistry.isBatonPass(move)) {
            return handleBatonPass(state, action, actor, move, actionLog, actionLogs, events, playerSide);
        }
        // 挺住：本回合受到致命攻击时保留 1 HP
        if (MoveRegistry.isEndure(move)) {
            engine.setVolatile(actor, "endured", true);
            events.add(actor.get("name") + " 使用了忍耐！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        // 黑雾：清除双方所有能力变化
        if (MoveRegistry.isHaze(move)) {
            for (boolean side : new boolean[]{true, false}) {
                for (Integer slot : engine.activeSlots(state, side)) {
                    if (slot == null || slot < 0) continue;
                    Map<String, Object> mon = engine.team(state, side).get(slot);
                    if (engine.toInt(mon.get("currentHp"), 0) <= 0) continue;
                    mon.put("statStages", new LinkedHashMap<String, Object>());
                }
            }
            events.add(actor.get("name") + " 使用了黑雾，所有能力变化恢复了！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        // 封印（Imprison）：只要使用者在场，目标不能使用与使用者相同的招式
        if (MoveRegistry.isImprison(move)) {
            engine.setVolatile(actor, "imprisonActive", true);
            actionLog.put("result", "imprison");
            events.add(actor.get("name") + " 使用了封印！双方无法使用相同的招式！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        // 同命：如果本回合使用者被击倒，攻击者也一同倒下
        if (MoveRegistry.isDestinyBond(move)) {
            engine.setVolatile(actor, "destinyBond", true);
            events.add(actor.get("name") + " 使用了同命！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        // 换场（Court Change）：交换双方场地/钉子/墙壁等效果
        if (engine.isCourtChange(move)) {
            java.util.Map<String, Object> fe = engine.castMap(state.get("fieldEffects"));
            String[] swapKeys = {"playerReflectTurns", "opponentReflectTurns",
                "playerLightScreenTurns", "opponentLightScreenTurns",
                "playerAuroraVeilTurns", "opponentAuroraVeilTurns",
                "playerSafeguardTurns", "opponentSafeguardTurns",
                "playerStealthRock", "opponentStealthRock",
                "playerSpikesLayers", "opponentSpikesLayers",
                "playerToxicSpikesLayers", "opponentToxicSpikesLayers",
                "playerStickyWeb", "opponentStickyWeb",
                "playerTailwindTurns", "opponentTailwindTurns"};
            for (int i = 0; i < swapKeys.length; i += 2) {
                Object pVal = fe.get(swapKeys[i]);
                Object oVal = fe.get(swapKeys[i + 1]);
                // 交换双方值，null 处理为 0/false
                if (pVal instanceof Number pn && oVal instanceof Number on) {
                    fe.put(swapKeys[i], on);
                    fe.put(swapKeys[i + 1], pn);
                } else {
                    fe.put(swapKeys[i], oVal);
                    fe.put(swapKeys[i + 1], pVal);
                }
            }
            events.add(actor.get("name") + " 使用了换场！双方场地效果交换了！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 分担痛楚：使用者和目标 HP 相加平均分配
        // 临别礼物/治愈之愿/新月舞：自杀式效果
        if (engine.isSuicideMove(move)) {
            String moveNameEn = String.valueOf(move.get("name_en")).toLowerCase();
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            actor.put("currentHp", 0);
            actor.put("status", "fainted");

            if (moveNameEn.contains("memento")) {
                // 临别礼物：目标攻/特攻 -2
                for (BattleEngine.TargetRef tr : targetSupport.resolveMoveTargets(state, action, move, random,
                        new java.util.HashMap<>())) {
                    List<Map<String, Object>> tTeam = engine.team(state, tr.playerSide());
                    if (tr.teamIndex() >= 0 && tr.teamIndex() < tTeam.size()) {
                        Map<String, Object> tMon = tTeam.get(tr.teamIndex());
                        if (engine.isAvailableMon(tTeam, tr.teamIndex())) {
                            conditionSupport.applyMultiStatBoost(state, tMon,
                                    Map.of("attack", -2, "specialAttack", -2), "临别礼物", events);
                        }
                    }
                }
                actionLog.put("result", "memento");
                events.add(actor.get("name") + " 使用了临别礼物，攻击和特攻大幅降低了！");
            } else if (moveNameEn.contains("healing wish") || moveNameEn.contains("healing-wish")) {
                // 治愈之愿：后备上场时满状态
                engine.setVolatile(actor, "healingWishPending", true);
                actionLog.put("result", "healing-wish");
                events.add(actor.get("name") + " 使用了治愈之愿！下只上场的宝可梦将满状态复活！");
            } else if (moveNameEn.contains("lunar dance") || moveNameEn.contains("lunar-dance")) {
                // 新月舞：后备上场时满血 + PP 全恢复
                engine.setVolatile(actor, "lunarDancePending", true);
                actionLog.put("result", "lunar-dance");
                events.add(actor.get("name") + " 使用了新月舞！下只上场的宝可梦将全回复！");
            }
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 复活祈愿（Revival Blessing）：复活己方一名倒下的队友，回复一半 HP
        if (engine.isRevivalBlessing(move)) {
            boolean pSide = "player".equals(action.side());
            java.util.List<Map<String, Object>> team = engine.team(state, pSide);
            int reviveTarget = -1;
            for (int i = 0; i < team.size(); i++) {
                // 跳过场上活跃的
                if (engine.activeSlots(state, pSide).contains(i)) continue;
                if (engine.toInt(team.get(i).get("currentHp"), 0) <= 0) {
                    reviveTarget = i;
                    break;
                }
            }
            if (reviveTarget < 0) {
                events.add(actor.get("name") + " 的复活祈愿失败了——没有倒下的队友");
                return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
            }
            Map<String, Object> revived = team.get(reviveTarget);
            int maxHp = engine.toInt(engine.castMap(revived.get("stats")).get("hp"), 1);
            revived.put("currentHp", Math.max(1, maxHp / 2));
            revived.put("status", "");
            revived.remove("condition");
            actionLog.put("result", "revival-blessing");
            actionLog.put("revived", revived.get("name"));
            events.add(actor.get("name") + " 使用了复活祈愿，" + revived.get("name") + " 复活了！");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 蜕尾（Shed Tail）：消耗 50% HP 制造替身后换入后备
        if (engine.isShedTail(move)) {
            boolean pSide = "player".equals(action.side());
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            int cost = Math.max(1, maxHp / 2);
            int curHp = engine.toInt(actor.get("currentHp"), 0);
            if (curHp <= cost) {
                events.add(actor.get("name") + " 的蜕尾失败了——HP 不足");
                return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
            }
            actor.put("currentHp", curHp - cost);
            // 制造替身（1/4 最大 HP，上限 1/2 当前 HP）
            int subHp = Math.max(1, Math.min(maxHp / 4, (curHp - cost) / 2));
            engine.setVolatile(actor, "substitute", subHp);
            events.add(actor.get("name") + " 使用了蜕尾，消耗了 " + cost + " HP 制造了替身！");
            // 触发换人
            java.util.List<Map<String, Object>> shedTeam = engine.team(state, pSide);
            java.util.List<Integer> shedSlots = engine.activeSlots(state, pSide);
            int benchIdx = engine.firstAvailableBench(shedTeam, shedSlots);
            if (benchIdx >= 0) {
                Map<String, Object> switchedIn = shedTeam.get(benchIdx);
                conditionSupport.applySwitchOutEffects(actor, events);
                switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
                engine.setVolatile(switchedIn, "flinch", false);
                java.util.List<Integer> newSlots = new java.util.ArrayList<>(shedSlots);
                newSlots.set(action.actorFieldSlot(), benchIdx);
                state.put(pSide ? "playerActiveSlots" : "opponentActiveSlots", newSlots);
                actionLog.put("switchTo", switchedIn.get("name"));
                events.add(switchedIn.get("name") + " 被换上场了");
                conditionSupport.applyEntryAbilities(state, pSide, shedSlots, events);
            }
            actionLog.put("result", "shed-tail");
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        if (MoveRegistry.isPainSplit(move)) {
            return handlePainSplit(state, action, actor, move, actionLog, actionLogs, events, playerSide);
        }
        // 蓄力（Stockpile）：物防+1 特防+1，最多蓄力 3 次
        if (engine.isStockpile(move)) {
            int stockpileCount = engine.toInt(engine.volatileValue(actor, "stockpileCount", 0), 0);
            if (stockpileCount >= 3) {
                actionLog.put("result", "failed");
                events.add(actor.get("name") + " 的蓄力已到最大次数");
                return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
            }
            conditionSupport.applyMultiStatBoost(state, actor, Map.of("defense", 1, "specialDefense", 1), "蓄力", events);
            engine.setVolatile(actor, "stockpileCount", stockpileCount + 1);
            actionLog.put("result", "stockpile");
            events.add(actor.get("name") + " 开始蓄力！当前蓄力层数：" + (stockpileCount + 1));
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        // 吞下（Swallow）：根据蓄力量回复 HP，然后解除蓄力
        if (engine.isSwallow(move)) {
            int stockpileCount = engine.toInt(engine.volatileValue(actor, "stockpileCount", 0), 0);
            if (stockpileCount <= 0) {
                actionLog.put("result", "failed");
                events.add(actor.get("name") + " 没有蓄力，吞下失败了");
                return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
            }
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            int currentHp = engine.toInt(actor.get("currentHp"), 0);
            int healPct = stockpileCount == 1 ? 25 : stockpileCount == 2 ? 50 : 100;
            int heal = Math.max(1, maxHp * healPct / 100);
            int actualHeal = Math.min(heal, maxHp - currentHp);
            if (actualHeal > 0) {
                actor.put("currentHp", currentHp + actualHeal);
                events.add(actor.get("name") + " 吞下了积蓄，回复了 " + actualHeal + " 点 HP！");
            } else {
                events.add(actor.get("name") + " 吞下了积蓄，但 HP 已满");
            }
            engine.setVolatile(actor, "stockpileCount", 0);
            actionLog.put("result", "swallow");
            actionLog.put("heal", actualHeal);
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }

        if (conditionSupport.applyMoveHealing(actor, move, actionLog, events)) {
            return finishNonDamagingMove(state, actor, move, actionLog, actionLogs);
        }
        return false;
    }

    private boolean handleStatusMove(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                     Map<String, Object> target, Map<String, Object> move, Map<String, Object> targetLog,
                                     List<String> events, Random random, int round, List<Map<String, Object>> actionLogs) {
        if (engine.isThunderWave(move)) {
            conditionSupport.applyParalysis(state, actor, target, move, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isWillOWisp(move)) {
            conditionSupport.applyBurn(state, actor, target, move, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isToxic(move)) {
            conditionSupport.applyPoison(state, actor, target, move, targetLog, events, true);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isPoisonPowder(move)) {
            conditionSupport.applyPoison(state, actor, target, move, targetLog, events, false);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isTaunt(move)) {
            conditionSupport.applyTaunt(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isEncore(move)) {
            conditionSupport.applyEncore(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isDisable(move)) {
            conditionSupport.applyDisable(state, actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isHealBlock(move)) {
            conditionSupport.applyHealBlock(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isTorment(move)) {
            conditionSupport.applyTorment(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isYawn(move)) {
            conditionSupport.applyYawn(state, actor, target, move, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSpore(move)) {
            conditionSupport.applySleep(state, actor, target, move, targetLog, events, random, round);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isFakeTears(move)) {
            targetLog.put("result", conditionSupport.applySpecialDefenseDrop(actor, target, 2, targetLog, events) ? "fake-tears" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isConfuseRay(move)) {
            conditionSupport.applyConfusion(actor, target, targetLog, events, random);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isTrappingMove(move)) {
            engine.setVolatile(target, "trapped", true);
            // 束缚招式（火焰旋涡/潮旋等）额外造成回合末持续伤害
            if (engine.isBindingMove(move)) {
                engine.setVolatile(target, "bound", true);
                // 基础持续 4-5 回合；紧缠钩爪延长至 7 回合
                int boundTurns = "grip-claw".equals(engine.heldItem(actor)) ? 7 : 4 + random.nextInt(2);
                engine.setVolatile(target, "boundTurns", boundTurns);
                // 紧缠绑带：伤害从 1/8 提升至 1/6
                int divisor = "binding-band".equals(engine.heldItem(actor)) ? 6 : 8;
                engine.setVolatile(target, "boundDivisor", divisor);
            }
            // 八爪束缚：额外标记，回合末降低防御和特防
            if (MoveRegistry.isOctolock(move)) {
                engine.setVolatile(target, "octolockTurns", 1);
                events.add(target.get("name") + " 被八爪束缚抓住了，双防每回合都会下降！");
            }
            targetLog.put("result", "trap");
            actionLogs.add(targetLog);
            events.add(actor.get("name") + " 困住了 " + target.get("name"));
            return true;
        }
        // 吸取力量：降低目标攻击 1 级，回复等量于目标攻击数值的 HP
        if (MoveRegistry.isStrengthSap(move)) {
            Map<String, Object> targetStats = engine.castMap(target.get("stats"));
            int baseAtk = engine.toInt(targetStats.get("attack"), 0);
            int prevAtkStage = engine.toInt(engine.statStages(target).get("attack"), 0);
            // 降低攻击前先计算回复量（使用降低前的攻击力）
            int healAmount = effectiveStat(baseAtk, prevAtkStage);
            // 降低攻击 1 级
            int nextAtkStage = Math.max(-6, prevAtkStage - 1);
            if (nextAtkStage != prevAtkStage) {
                engine.statStages(target).put("attack", nextAtkStage);
                targetLog.put("attackStageChange", -1);
                events.add(target.get("name") + " 的攻击下降了！");
            }
            // 回复使用者
            int actorHp = engine.toInt(actor.get("currentHp"), 0);
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            int actualHeal = Math.min(healAmount, maxHp - actorHp);
            if (actualHeal > 0) {
                actor.put("currentHp", actorHp + actualHeal);
                targetLog.put("strengthSapHeal", actualHeal);
                events.add(actor.get("name") + " 吸收了 " + target.get("name") + " 的力量，回复了 " + actualHeal + " 点 HP！");
            } else {
                events.add(actor.get("name") + " 使用了吸取力量，但 HP 已满");
            }
            targetLog.put("result", "strength-sap");
            actionLogs.add(targetLog);
            return true;
        }
        // 防守平分：双方防御/特防（含阶级修正后）相加平均，设为双方新基础值并清除阶级
        if (MoveRegistry.isGuardSplit(move)) {
            if (!handleGuardPowerSplit(actor, target, move, targetLog, events, true)) {
                targetLog.put("result", "failed");
            } else {
                targetLog.put("result", "guard-split");
                events.add(actor.get("name") + " 和 " + target.get("name") + " 平分了防御和特防！");
            }
            actionLogs.add(targetLog);
            return true;
        }
        // 力量平分：双方攻击/特攻（含阶级修正后）相加平均，设为双方新基础值并清除阶级
        if (MoveRegistry.isPowerSplit(move)) {
            if (!handleGuardPowerSplit(actor, target, move, targetLog, events, false)) {
                targetLog.put("result", "failed");
            } else {
                targetLog.put("result", "power-split");
                events.add(actor.get("name") + " 和 " + target.get("name") + " 平分了攻击和特攻！");
            }
            actionLogs.add(targetLog);
            return true;
        }
        // 防守互换：交换双方防御/特防的阶级
        if (MoveRegistry.isGuardSwap(move)) {
            handleGuardPowerSwap(actor, target, true);
            targetLog.put("result", "guard-swap");
            actionLogs.add(targetLog);
            events.add(actor.get("name") + " 和 " + target.get("name") + " 互换了防御和特防的阶级！");
            return true;
        }
        // 力量互换：交换双方攻击/特攻的阶级
        if (MoveRegistry.isPowerSwap(move)) {
            handleGuardPowerSwap(actor, target, false);
            targetLog.put("result", "power-swap");
            actionLogs.add(targetLog);
            events.add(actor.get("name") + " 和 " + target.get("name") + " 互换了攻击和特攻的阶级！");
            return true;
        }
        // 变身（Transform）：复制目标状态
        if (engine.isTransform(move)) {
            if (engine.volatileFlag(target, "substitute")) {
                targetLog.put("result", "failed");
                events.add(actor.get("name") + " 的变身失败了——目标有替身");
            } else if (Boolean.TRUE.equals(engine.volatileValue(actor, "transformed", false))) {
                targetLog.put("result", "failed");
                events.add(actor.get("name") + " 已经变身过了");
            } else {
                conditionSupport.applyTransform(actor, target, events);
                targetLog.put("result", "transform");
            }
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isTrickMove(move)) {
            String actorItem = engine.heldItem(actor);
            String targetItem = engine.heldItem(target);
            if (actorItem.isBlank() && targetItem.isBlank()) {
                targetLog.put("result", "failed");
                events.add(actor.get("name") + " 使用了交换道具，但双方都没有道具");
            } else {
                actor.put("heldItem", targetItem);
                target.put("heldItem", actorItem);
                targetLog.put("result", "trick");
                events.add(actor.get("name") + " 与 " + target.get("name") + " 交换了道具");
            }
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isLeechSeed(move)) {
            conditionSupport.applyLeechSeed(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSubstitute(move)) {
            conditionSupport.applySubstitute(actor, target, move, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isAttract(move)) {
            conditionSupport.applyAttract(actor, target, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isPerishSong(move)) {
            conditionSupport.applyPerishSongAll(state, actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isPartingShot(move)) {
            boolean succeeded = conditionSupport.applyAttackAndSpecialAttackDrop(actor, target, targetLog, events);
            targetLog.put("result", succeeded ? "parting-shot" : "failed");
            actionLogs.add(targetLog);
            if (succeeded) {
                autoSwitchAfterMove(state, action, actor, move, "parting-shot-switch", events, actionLogs);
            }
            return true;
        }

        // Nightmare: 恶梦，目标睡眠中每回合损失 1/8 最大 HP
        if (engine.isNightmare(move)) {
            if (!"sleep".equals(target.get("condition"))) {
                targetLog.put("result", "failed");
                events.add(actor.get("name") + " 使用了恶梦，但目标没有睡着");
            } else {
                engine.setVolatile(target, "nightmare", true);
                targetLog.put("result", "nightmare");
                events.add(actor.get("name") + " 让 " + target.get("name") + " 陷入了恶梦状态");
            }
            actionLogs.add(targetLog);
            return true;
        }
        // Heal Bell: 治愈铃声，治愈己方全队异常状态（声音类招式）
        if (engine.isHealBell(move)) {
            conditionSupport.applyHealBell(state, actor, targetLog, events,
                "player".equals(action.side()));
            targetLog.put("result", "heal-bell");
            actionLogs.add(targetLog);
            return true;
        }
        // Aromatherapy: 芳香治疗，治愈己方全队异常状态（粉末类，靠 move flag 阻隔）
        if (engine.isAromatherapy(move)) {
            conditionSupport.applyHealBell(state, actor, targetLog, events,
                "player".equals(action.side()));
            targetLog.put("result", "aromatherapy");
            actionLogs.add(targetLog);
            return true;
        }
        // Refresh: 净化之水，治愈自身的中毒/灼伤/麻痹
        if (engine.isRefresh(move)) {
            conditionSupport.applyRefresh(actor, targetLog, events);
            targetLog.put("result", "refresh");
            actionLogs.add(targetLog);
            return true;
        }

        // Self-boosting moves
        if (engine.isSwordsDance(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "attack", 2, "剑舞", events);
            targetLog.put("result", succeeded ? "swords-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isNastyPlot(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "specialAttack", 2, "诡计", events);
            targetLog.put("result", succeeded ? "nasty-plot" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isDragonDance(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 1, "speed", 1), "龙舞", events);
            targetLog.put("result", succeeded ? "dragon-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isCalmMind(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("specialAttack", 1, "specialDefense", 1), "冥想", events);
            targetLog.put("result", succeeded ? "calm-mind" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isAgility(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "speed", 2, "高速移动", events);
            targetLog.put("result", succeeded ? "agility" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isAutotomize(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "speed", 2, "轻量化", events);
            targetLog.put("result", succeeded ? "autotomize" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isBulkUp(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 1, "defense", 1), "健美", events);
            targetLog.put("result", succeeded ? "bulk-up" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isWorkUp(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 1, "specialAttack", 1), "振作", events);
            targetLog.put("result", succeeded ? "work-up" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isQuiverDance(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("specialAttack", 1, "specialDefense", 1, "speed", 1), "蝶舞", events);
            targetLog.put("result", succeeded ? "quiver-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isCoil(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 1, "defense", 1, "accuracy", 1), "盘蜷", events);
            targetLog.put("result", succeeded ? "coil" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isShellSmash(move)) {
            // Shell Smash: Atk +2, SpA +2, Spe +2, Def -1, SpD -1
            boolean anyChange = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 2, "specialAttack", 2, "speed", 2), "破壳", events);
            // Apply defense drops using engine's statStages
            int prevDef = engine.toInt(engine.castMap(actor.get("statStages")).get("defense"), 0);
            int nextDef = Math.max(-6, prevDef - 1);
            engine.castMap(actor.get("statStages")).put("defense", nextDef);
            int prevSpD = engine.toInt(engine.castMap(actor.get("statStages")).get("specialDefense"), 0);
            int nextSpD = Math.max(-6, prevSpD - 1);
            engine.castMap(actor.get("statStages")).put("specialDefense", nextSpD);
            if (anyChange || nextDef != prevDef || nextSpD != prevSpD) {
                events.add(actor.get("name") + " 使用了破壳，大幅提升了攻击、特攻和速度，但防御和特防下降了！");
            }
            targetLog.put("result", anyChange ? "shell-smash" : "failed");
            actionLogs.add(targetLog);
            return true;
        }

        // 背水一战：全能力 +1（除命中/闪避），自身束缚
        if (MoveRegistry.isNoRetreat(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(state, actor,
                Map.of("attack", 1, "defense", 1, "specialAttack", 1, "specialDefense", 1, "speed", 1),
                "背水一战", events);
            if (succeeded) {
                engine.setVolatile(actor, "trapped", true);
                events.add(actor.get("name") + " 使用了背水一战，无法换人了！");
            }
            targetLog.put("result", succeeded ? "no-retreat" : "failed");
            actionLogs.add(targetLog);
            return true;
        }

        // 焦油覆盖：使目标火系弱点 ×2，速度 -1
        if (MoveRegistry.isTarShot(move)) {
            engine.setVolatile(target, "tarShot", true);
            conditionSupport.applySpeedDrop(actor, target, targetLog, events);
            targetLog.put("result", "tar-shot");
            actionLogs.add(targetLog);
            events.add(target.get("name") + " 被焦油覆盖，火系招式将造成更大伤害！");
            return true;
        }

        // 强制换人招式（吼叫/吹飞）：将目标替换为随机后备，对方无后备时失败
        if (engine.isForcedSwitchMove(move) && engine.isStatusMove(move)) {
            // 目标选择
            List<BattleEngine.TargetRef> switchTargets = targetSupport.resolveMoveTargets(state, action, move, random, new java.util.HashMap<>());
            if (switchTargets.isEmpty()) {
                targetLog.put("result", "failed");
                actionLogs.add(targetLog);
                events.add(actor.get("name") + " 使用了 " + move.get("name") + "，但没有目标");
                return true;
            }
            BattleEngine.TargetRef switchTargetRef = switchTargets.get(0);
            List<Map<String, Object>> switchTargetTeam = engine.team(state, switchTargetRef.playerSide());
            if (!engine.isAvailableMon(switchTargetTeam, switchTargetRef.teamIndex())) {
                targetLog.put("result", "failed");
                actionLogs.add(targetLog);
                return true;
            }
            Map<String, Object> forcedTarget = switchTargetTeam.get(switchTargetRef.teamIndex());
            targetLog.put("target", forcedTarget.get("name"));

            // 失败条件检查
            boolean switchBlocked = false;
            // Suction Cups 免疫强制换人
            if ("suction-cups".equalsIgnoreCase(engine.abilityName(forcedTarget))) {
                targetLog.put("result", "blocked");
                events.add(forcedTarget.get("name") + " 的吸盘特性让它无法被强制换下");
                switchBlocked = true;
            }
            // Ingrain 扎根不能换下
            if (!switchBlocked && Boolean.TRUE.equals(engine.volatileValue(forcedTarget, "ingrain", false))) {
                targetLog.put("result", "blocked");
                events.add(forcedTarget.get("name") + " 扎根了，无法被强制换下");
                switchBlocked = true;
            }
            // Substitute 挡住变化招式
            if (!switchBlocked && isSubstituteActive(forcedTarget)) {
                targetLog.put("result", "blocked");
                events.add(forcedTarget.get("name") + " 的替身挡住了" + move.get("name"));
                switchBlocked = true;
            }
            // 检查对方是否有后备
            if (!switchBlocked) {
                List<Integer> switchSlots = engine.activeSlots(state, switchTargetRef.playerSide());
                if (engine.firstAvailableBench(switchTargetTeam, switchSlots) < 0) {
                    targetLog.put("result", "failed");
                    events.add(move.get("name") + " 失败了——对方没有后备宝可梦");
                    switchBlocked = true;
                }
            }
            if (switchBlocked) {
                actionLogs.add(targetLog);
                return true;
            }

            // 执行强制换人
            int benchIdx = engine.firstAvailableBench(switchTargetTeam, engine.activeSlots(state, switchTargetRef.playerSide()));
            Map<String, Object> switchedIn = switchTargetTeam.get(benchIdx);
            conditionSupport.applySwitchOutEffects(forcedTarget, events);
            forcedTarget.put("choiceLockedMove", null);
            conditionSupport.resetBattleStages(forcedTarget);
            switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
            engine.setVolatile(switchedIn, "flinch", false);
            List<Integer> oldSlots = new ArrayList<>(engine.activeSlots(state, switchTargetRef.playerSide()));
            oldSlots.set(switchTargetRef.fieldSlot(), benchIdx);
            state.put(switchTargetRef.playerSide() ? "playerActiveSlots" : "opponentActiveSlots", oldSlots);
            targetLog.put("result", "forced-switch");
            targetLog.put("switchTo", switchedIn.get("name"));
            events.add(actor.get("name") + " 使用了 " + move.get("name") + "，" + forcedTarget.get("name") + " 被强制换下，" + switchedIn.get("name") + " 上场了");
            actionLogs.add(targetLog);
            conditionSupport.applyEntryAbilities(state, switchTargetRef.playerSide(), oldSlots, events);
            return true;
        }

        // 长嚎（Howl）：物攻 +1（声音招式，场上所有己方 PM 有效）
        if (engine.isHowl(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "attack", 1, "长嚎", events);
            targetLog.put("result", succeeded ? "howl" : "failed");
            actionLogs.add(targetLog);
            return true;
        }

        // 磨爪（Hone Claws）：物攻 +1，命中 +1
        if (engine.isHoneClaws(move)) {
            boolean atkSucc = conditionSupport.applySelfStatBoost(state, actor, "attack", 1, "磨爪", events);
            targetLog.put("result", atkSucc ? "hone-claws" : "failed");
            actionLogs.add(targetLog);
            // 命中率 +1 阶级写入 statStages.accuracy（calculateAccuracyWithStages 读取该键）
            int accStage = engine.toInt(engine.statStages(actor).get("accuracy"), 0);
            engine.statStages(actor).put("accuracy", Math.min(6, accStage + 1));
            events.add(actor.get("name") + " 的命中率提升了");
            return true;
        }

        // 铁壁（Iron Defense）：防御 +2
        if (engine.isIronDefense(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(state, actor, "defense", 2, "铁壁", events);
            targetLog.put("result", succeeded ? "iron-defense" : "failed");
            actionLogs.add(targetLog);
            return true;
        }

        // 生长（Growth）：物攻 +1，特攻 +1（大晴天时各+2）
        if (engine.isGrowth(move)) {
            int boost = engine.sunTurns(state) > 0 ? 2 : 1;
            conditionSupport.applyMultiStatBoost(state, actor, Map.of("attack", boost, "specialAttack", boost), "生长", events);
            targetLog.put("result", "growth");
            actionLogs.add(targetLog);
            return true;
        }

        // 腹鼓（Belly Drum）：消耗 50% 最大 HP，物攻升至 +6 阶级
        if (engine.isBellyDrum(move)) {
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            int cost = Math.max(1, maxHp / 2);
            int currentHp = engine.toInt(actor.get("currentHp"), 0);
            // HP 不足时腹鼓失败
            if (currentHp <= cost) {
                targetLog.put("result", "failed");
                events.add(actor.get("name") + " HP不足，无法使用腹鼓！");
                actionLogs.add(targetLog);
                return true;
            }
            actor.put("currentHp", Math.max(0, currentHp - cost));
            engine.castMap(actor.get("statStages")).put("attack", 6);
            events.add(actor.get("name") + " 使用了腹鼓，消耗了 " + cost + " HP，攻击力提升到了极致！");
            targetLog.put("result", "belly-drum");
            actionLogs.add(targetLog);
            return true;
        }

        // Entry hazard moves
        if (engine.isStealthRock(move)) {
            engine.setStealthRock(state, "player".equals(action.side()), actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSpikes(move)) {
            engine.addSpikesLayer(state, "player".equals(action.side()), actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isToxicSpikes(move)) {
            engine.addToxicSpikesLayer(state, "player".equals(action.side()), actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isStickyWeb(move)) {
            engine.setStickyWeb(state, "player".equals(action.side()), actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }

        // Weather moves
        if (engine.isRainDance(move)) {
            engine.activateWeather(state, "rain", actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSunnyDay(move)) {
            engine.activateWeather(state, "sun", actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSandstorm(move)) {
            engine.activateWeather(state, "sand", actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSnowWeather(move)) {
            engine.activateWeather(state, "snow", actor, targetLog, events);
            actionLogs.add(targetLog);
            return true;
        }

        // Recovery moves
        if (engine.isRecover(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "自我再生", events);
            targetLog.put("result", succeeded ? "recover" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isRoost(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "羽栖", events);
            targetLog.put("result", succeeded ? "roost" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isRest(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "睡觉", events);
            targetLog.put("result", succeeded ? "rest" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSoftBoiled(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "生蛋", events);
            targetLog.put("result", succeeded ? "soft-boiled" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isMilkDrink(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "喝牛奶", events);
            targetLog.put("result", succeeded ? "milk-drink" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isSynthesis(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "光合作用", events);
            targetLog.put("result", succeeded ? "synthesis" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isMoonlight(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "月光", events);
            targetLog.put("result", succeeded ? "moonlight" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        // Worry Seed: 将目标特性改为不眠
        if (MoveRegistry.isWorrySeed(move)) {
            target.put("ability", new java.util.LinkedHashMap<>(Map.of("name_en", "insomnia", "name", "不眠")));
            targetLog.put("result", "worry-seed");
            actionLogs.add(targetLog);
            events.add(target.get("name") + " 的特性被变成了不眠！");
            return true;
        }

        // Gastro Acid: 抑制目标特性
        if (MoveRegistry.isGastroAcid(move)) {
            engine.setVolatile(target, "abilitySuppressed", true);
            targetLog.put("result", "gastro-acid");
            actionLogs.add(targetLog);
            events.add(target.get("name") + " 的特性被抑制了！");
            return true;
        }

        // Heart Swap: 交换双方所有能力阶级
        if (MoveRegistry.isHeartSwap(move)) {
            java.util.Map<String, Object> actorStages = engine.castMap(actor.get("statStages"));
            java.util.Map<String, Object> targetStages = engine.castMap(target.get("statStages"));
            String[] statKeys = {"attack", "defense", "specialAttack", "specialDefense", "speed", "accuracy", "evasion"};
            for (String key : statKeys) {
                int tmp = engine.toInt(actorStages.get(key), 0);
                actorStages.put(key, engine.toInt(targetStages.get(key), 0));
                targetStages.put(key, tmp);
            }
            targetLog.put("result", "heart-swap");
            actionLogs.add(targetLog);
            events.add(actor.get("name") + " 和 " + target.get("name") + " 交换了能力变化！");
            return true;
        }

        if (MoveRegistry.isPsychoShift(move)) {
            String actorCondition = String.valueOf(actor.get("condition"));
            if ("healthy".equals(actorCondition) || "fainted".equals(actorCondition)) {
                targetLog.put("result", "failed");
                actionLogs.add(targetLog);
                events.add(actor.get("name") + " 没有异常状态可以转移");
                return true;
            }
            target.put("condition", actorCondition);
            if ("toxic".equals(actorCondition)) {
                target.put("toxicCounter", engine.toInt(actor.get("toxicCounter"), 0));
                actor.put("toxicCounter", 0);
            }
            actor.put("condition", "healthy");
            engine.clearVolatile(actor, "nightmare");
            targetLog.put("result", "psycho-shift");
            actionLogs.add(targetLog);
            events.add(actor.get("name") + " 将 " + actorCondition + " 转移给了 " + target.get("name") + "！");
            return true;
        }

        if (engine.isMorningSun(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "晨光", events);
            targetLog.put("result", succeeded ? "morning-sun" : "failed");
            actionLogs.add(targetLog);
            return true;
        }

        return false;
    }

    private boolean finishNonDamagingMove(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> move,
                                          Map<String, Object> actionLog, List<Map<String, Object>> actionLogs) {
        actionLogs.add(actionLog);
        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move, state);
        return true;
    }

    private boolean handleProtectionMove(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> move, int round, Random random,
                                         Map<String, Boolean> protectedTargets,
                                         Map<String, Boolean> wideGuardSides,
                                         Map<String, Boolean> quickGuardSides,
                                         String side, int actorIndex,
                                         Map<String, Object> actionLog, List<Map<String, Object>> actionLogs, List<String> events) {
        int streak = engine.toInt(actor.get("protectionStreak"), 0);
        int lastRound = engine.toInt(actor.get("lastProtectionRound"), 0);
        double successChance = lastRound == round - 1 && streak > 0 ? Math.pow(1.0d / 3.0d, streak) : 1.0d;
        if (random.nextDouble() > successChance) {
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 的守护招式失败了");
            return true;
        }

        actor.put("protectionStreak", lastRound == round - 1 ? streak + 1 : 1);
        actor.put("lastProtectionRound", round);
        if (MoveRegistry.isWideGuard(move)) {
            wideGuardSides.put(side, true);
            actionLog.put("result", "wide-guard");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 Wide Guard");
        } else if (MoveRegistry.isQuickGuard(move)) {
            quickGuardSides.put(side, true);
            actionLog.put("result", "quick-guard");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 Quick Guard");
        } else {
            protectedTargets.put(engine.protectionKey(side, actorIndex), true);
            actionLog.put("result", "protect");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 " + move.get("name"));
        }
        actionLogs.add(actionLog);
        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move, state);
        return true;
    }

    private boolean isBlockedByQuickGuard(Map<String, Object> move, String targetSide, Map<String, Boolean> quickGuardSides) {
        if (!quickGuardSides.getOrDefault(targetSide, false)) {
            return false;
        }
        if (engine.toInt(move.get("priority"), 0) <= 0) {
            return false;
        }
        int targetId = engine.toInt(move.get("target_id"), 10);
        return targetId != 4 && targetId != 7;
    }

    private boolean handleBatonPass(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                    Map<String, Object> move, Map<String, Object> actionLog,
                                    List<Map<String, Object>> actionLogs, List<String> events, boolean playerSide) {
        List<Map<String, Object>> actingTeam = engine.team(state, playerSide);
        List<Integer> activeSlots = engine.activeSlots(state, playerSide);
        int switchToIndex = engine.firstAvailableBench(actingTeam, activeSlots);
        if (switchToIndex < 0) {
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了接棒，但没有可以替换的宝可梦");
            return true;
        }

        // 保存接力状态：能力阶级、替身、混乱、寄生种子、诅咒、灭亡之歌等
        Map<String, Object> savedStages = new LinkedHashMap<>();
        Object stages = actor.get("statStages");
        if (stages instanceof Map) {
            savedStages.putAll((Map<String, Object>) stages);
        }
        Object subHp = engine.volatileValue(actor, "substitute", null);
        boolean isConfused = Boolean.TRUE.equals(actor.get("confused"));
        boolean hasLeechSeed = Boolean.TRUE.equals(actor.get("leechSeed"));
        boolean isCursed = Boolean.TRUE.equals(actor.get("cursed"));
        int perishSongTurns = engine.toInt(actor.get("perishSongTurns"), 0);
        boolean hasAquaRing = Boolean.TRUE.equals(actor.get("aquaRing"));
        boolean hasIngrain = Boolean.TRUE.equals(actor.get("ingrain"));

        // 执行切换
        List<Integer> previousSlots = new ArrayList<>(activeSlots);
        Map<String, Object> switchedIn = actingTeam.get(switchToIndex);
        actor.put("choiceLockedMove", null);
        // 接棒不重置能力阶级，不清除连接状态
        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
        engine.setVolatile(switchedIn, "flinch", false);
        // Stance Change: 坚盾剑怪使用招式后切换形态
        boolean isAttackerAlive = engine.toInt(actor.get("currentHp"), 0) > 0;
        if (isAttackerAlive && !action.isSwitch()) {
            conditionSupport.checkStanceChangeAfterMove(actor, move);
        }
        // 驱动能量（Booster Energy）：发动后消耗
        if (isAttackerAlive && "booster-energy".equals(engine.heldItem(actor)) && !engine.itemConsumed(actor)) {
            engine.consumeItem(actor);
            events.add(actor.get("name") + " 的驱动能量发动了并被消耗！");
        }
        // 攻击者形态变化检查
        if (isAttackerAlive) conditionSupport.checkFormChange(actor, state, events);

        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move, state);
        engine.replaceActiveSlot(state, playerSide, action.actorFieldSlot(), switchToIndex);

        // Fire ON_SWITCH_OUT / ON_SWITCH_IN
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_OUT,
            new BattleEvent(BattleEventType.ON_SWITCH_OUT) {},
            Map.of("source", actor, "playerSide", playerSide, "state", state));
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_IN,
            new BattleEvent(BattleEventType.ON_SWITCH_IN) {},
            Map.of("source", switchedIn, "playerSide", playerSide, "state", state));

        // 标记换入用于监查特性（Stakeout 等）
        switchedIn.put("justSwitchedIn", true);

        // 应用接力状态到换上来的宝可梦
        if (!savedStages.isEmpty()) {
            switchedIn.put("statStages", savedStages);
        }
        if (subHp != null) {
            engine.setVolatile(switchedIn, "substitute", subHp);
        }
        if (isConfused) {
            switchedIn.put("confused", true);
        }
        if (hasLeechSeed) {
            switchedIn.put("leechSeed", true);
        }
        if (isCursed) {
            switchedIn.put("cursed", true);
        }
        if (perishSongTurns > 0) {
            engine.setVolatile(switchedIn, "perishSongTurns", perishSongTurns);
        }
        if (hasAquaRing) {
            switchedIn.put("aquaRing", true);
        }
        if (hasIngrain) {
            switchedIn.put("ingrain", true);
        }

        actionLog.put("actionType", "switch");
        actionLog.put("switchTo", switchedIn.get("name"));
        actionLog.put("result", "baton-pass");
        actionLogs.add(actionLog);
        events.add(engine.sideName(action.side()) + " 使用了接棒，" + actor.get("name") + " 回到了队伍，" + switchedIn.get("name") + " 上场了！");
        conditionSupport.applyEntryAbilities(state, playerSide, previousSlots, events);
        return true;
    }

    /**
     * 分担痛楚：使用者和目标 HP 相加平均分配。
     */
    private boolean handlePainSplit(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                    Map<String, Object> move, Map<String, Object> actionLog,
                                    List<Map<String, Object>> actionLogs, List<String> events, boolean playerSide) {
        // 分担痛楚：选择目标，使用者和目标 HP 相加平均分配
        List<BattleEngine.TargetRef> targets = targetSupport.resolveMoveTargets(state, action, move, new Random(),
                new java.util.HashMap<>());
        if (targets.isEmpty()) {
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了分担痛楚，但没有目标");
            return true;
        }
        BattleEngine.TargetRef targetRef = targets.get(0);
        List<Map<String, Object>> targetTeam = engine.team(state, targetRef.playerSide());
        if (!engine.isAvailableMon(targetTeam, targetRef.teamIndex())) {
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 使用了分担痛楚，但目标无法使用");
            return true;
        }
        Map<String, Object> target = targetTeam.get(targetRef.teamIndex());
        if (target == actor) {
            actionLog.put("result", "failed");
            actionLogs.add(actionLog);
            return true;
        }

        int actorHp = engine.toInt(actor.get("currentHp"), 0);
        int targetHp = engine.toInt(target.get("currentHp"), 0);
        int totalHp = actorHp + targetHp;
        int avgHp = Math.max(1, totalHp / 2);

        if (actorHp != avgHp) {
            int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), 1);
            actor.put("currentHp", Math.min(avgHp, maxHp));
        }
        if (targetHp != avgHp) {
            int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1);
            target.put("currentHp", Math.min(avgHp, maxHp));
        }

        actionLog.put("target", target.get("name"));
        actionLog.put("result", "pain-split");
        actionLogs.add(actionLog);
        events.add(actor.get("name") + " 和 " + target.get("name") + " 分享了痛楚！双方 HP 变为 " + avgHp);
        return true;
    }

    private boolean handleReverseDamageMove(Map<String, Object> actor, Map<String, Object> target,
                                            Map<String, Object> move, Map<String, Object> targetLog,
                                            List<String> events) {
        int dmgClass = engine.toInt(move.get("damage_class_id"), 0);
        Integer physicalDmg = (Integer) engine.volatileValue(actor, "lastTakenPhysDmg", null);
        Integer specialDmg = (Integer) engine.volatileValue(actor, "lastTakenSpecDmg", null);
        int multiplier = 2;

        int takenDamage = 0;
        String counterType = "";
        if (MoveRegistry.isCounter(move)) {
            takenDamage = physicalDmg != null ? physicalDmg : 0;
            counterType = "物理";
        } else if (MoveRegistry.isMirrorCoat(move)) {
            takenDamage = specialDmg != null ? specialDmg : 0;
            counterType = "特殊";
        } else if (MoveRegistry.isMetalBurst(move)) {
            takenDamage = Math.max(
                    physicalDmg != null ? physicalDmg : 0,
                    specialDmg != null ? specialDmg : 0);
            // Metal Burst 返还 1.5 倍伤害，单独处理
            if (takenDamage <= 0) return false;
            int returnDamage = (int) Math.floor(takenDamage * 1.5);
            int targetHp = engine.toInt(target.get("currentHp"), 0);
            int actualReturnDamage = Math.min(returnDamage, targetHp);
            target.put("currentHp", Math.max(0, targetHp - actualReturnDamage));
            targetLog.put("damage", actualReturnDamage);
            targetLog.put("result", "metal-burst");
            events.add(actor.get("name") + " 使用了金属爆炸！返还了 " + actualReturnDamage + " 伤害");
            return true;
        }

        if (takenDamage <= 0) {
            return false; // 未受对应类型伤害
        }

        int returnDamage = takenDamage * multiplier;
        int targetHp = engine.toInt(target.get("currentHp"), 0);
        int actualReturnDamage = Math.min(returnDamage, targetHp);
        target.put("currentHp", targetHp - actualReturnDamage);
        targetLog.put("result", "counter-hit");
        targetLog.put("damage", actualReturnDamage);
        events.add(actor.get("name") + " 的 " + move.get("name") + " 将 " + counterType + "伤害"
                + (multiplier > 1 ? "加倍" : "") + "返还给了 " + target.get("name") + "，造成 " + actualReturnDamage + " 点伤害");
        if (targetHp - actualReturnDamage <= 0) {
            target.put("status", "fainted");
            events.add(target.get("name") + " 倒下了");
        }
        return true;
    }

    private void autoSwitchAfterMove(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                     Map<String, Object> move, String switchResult,
                                     List<String> events, List<Map<String, Object>> actionLogs) {
        boolean playerSide = "player".equals(action.side());
        List<Map<String, Object>> team = engine.team(state, playerSide);
        List<Integer> activeSlots = engine.activeSlots(state, playerSide);
        int switchToIndex = engine.firstAvailableBench(team, activeSlots);
        if (switchToIndex < 0) {
            // 无可替换宝可梦：红牌/逃跑按钮不应消耗道具。
            // 由调用方（applyDefenderItemEffects）在标记道具消耗前已检查后备，
            // 此处兜底：若道具已被消耗但换人失败，恢复道具。
            if ("eject-button".equals(engine.heldItem(actor)) || "red-card".equals(engine.heldItem(actor))) {
                // 道具仍持有（未消耗）→ 无事发生；若已消耗则回滚
                if (Boolean.TRUE.equals(actor.get("itemConsumed"))) {
                    actor.put("itemConsumed", false);
                    events.add(actor.get("name") + " 因为没有可替换的宝可梦，" 
                            + ("eject-button".equals(engine.heldItem(actor)) ? "逃脱按钮" : "红牌") + "没有发动");
                }
            }
            engine.rememberLastMove(actor, move);
            engine.rememberChoiceMove(actor, move);
            engine.applyCooldown(actor, move);
            return;
        }
        List<Integer> previousSlots = new ArrayList<>(activeSlots);
        Map<String, Object> switchedIn = team.get(switchToIndex);
        // Stance Change: 坚盾剑怪使用招式后切换形态
        boolean isAttackerAlive = engine.toInt(actor.get("currentHp"), 0) > 0;
        if (isAttackerAlive && !action.isSwitch()) {
            conditionSupport.checkStanceChangeAfterMove(actor, move);
        }
        // 驱动能量（Booster Energy）：发动后消耗
        if (isAttackerAlive && "booster-energy".equals(engine.heldItem(actor)) && !engine.itemConsumed(actor)) {
            engine.consumeItem(actor);
            events.add(actor.get("name") + " 的驱动能量发动了并被消耗！");
        }
        // 攻击者形态变化检查
        if (isAttackerAlive) conditionSupport.checkFormChange(actor, state, events);

        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move, state);
        conditionSupport.applySwitchOutEffects(actor, events);
        actor.put("choiceLockedMove", null);
        conditionSupport.resetBattleStages(actor);
        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
        engine.setVolatile(switchedIn, "flinch", false);
        engine.replaceActiveSlot(state, playerSide, action.actorFieldSlot(), switchToIndex);

        // Fire ON_SWITCH_OUT / ON_SWITCH_IN
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_OUT,
            new BattleEvent(BattleEventType.ON_SWITCH_OUT) {},
            Map.of("source", actor, "playerSide", playerSide, "state", state));
        engine.getEventBus().fireEvent(BattleEventType.ON_SWITCH_IN,
            new BattleEvent(BattleEventType.ON_SWITCH_IN) {},
            Map.of("source", switchedIn, "playerSide", playerSide, "state", state));

        // 标记换入用于监查特性（Stakeout 等）
        switchedIn.put("justSwitchedIn", true);

        Map<String, Object> switchLog = new LinkedHashMap<>();
        switchLog.put("side", action.side());
        switchLog.put("actor", actor.get("name"));
        switchLog.put("actionType", "switch");
        switchLog.put("switchTo", switchedIn.get("name"));
        switchLog.put("result", switchResult);
        actionLogs.add(switchLog);
        events.add(engine.sideName(action.side()) + " 收回了 " + actor.get("name") + "，派出了 " + switchedIn.get("name"));
        conditionSupport.applyEntryAbilities(state, playerSide, previousSlots, events);
    }

    private boolean isFakeOut(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "fake-out".equalsIgnoreCase(nameEn) || "fake out".equalsIgnoreCase(nameEn);
    }

    /**
     * 按照接近 Pokemon Showdown 的顺序计算最终命中率。
     * <p>
     * 计算链路为：基础命中 → 命中/闪避阶段 → 特性修正 → 道具修正 → 天气特判。
     * 这里返回的是 1~100 的最终百分比整数，供上层直接做随机判定。
     * </p>
     */
    int calculateAccuracyWithStages(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender, Map<String, Object> move) {
        // 永不中技能、No Guard 这类规则优先短路，避免再参与后续阶段修正。
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        String attackerAbility = engine.abilityName(attacker);
        String defenderAbility = engine.abilityName(defender);
        if (matches(attackerAbility, "no-guard", "no guard") || matches(defenderAbility, "no-guard", "no guard")) {
            return 100;
        }
        if (nameEn.contains("aerial ace") || nameEn.contains("swift") ||
            nameEn.contains("shock wave") || nameEn.contains("magnet bomb") ||
            nameEn.contains("aura sphere") || nameEn.contains("fissure") ||
            nameEn.contains("horn drill") || nameEn.contains("guillotine") ||
            nameEn.contains("sheer cold")) {
            return 100;
        }

        // 读取招式基础命中率；accuracy=0 在这套数据里视为“必定命中”。
        int baseAccuracy = engine.toInt(move.get("accuracy"), 0);
        if (baseAccuracy == 0) {
            // Accuracy 0 means it never misses (like Swift)
            return 100;
        }

        // 读取命中/闪避阶段。当前实现仍从 statStages 取值，但输出会统一折算为 PS 风格倍率。
        int accuracyStage = getStatStage(attacker, "accuracy");
        int evasionStage = getStatStage(defender, "evasion");

        // 阶段必须被限制在 [-6, +6]，否则脏数据会把倍率放大到不合理区间。
        accuracyStage = Math.max(-6, Math.min(6, accuracyStage));
        evasionStage = Math.max(-6, Math.min(6, evasionStage));

        // Calculate stage multiplier using Pokemon Showdown formula
        // Stage: -6=-3/9, -5=-3/8, -4=-3/7, -3=-3/6, -2=-3/5, -1=-3/4, 0=3/3, +1=4/3, +2=5/3, +3=6/3, +4=7/3, +5=8/3, +6=9/3
        double accuracyMultiplier = getStageMultiplier(accuracyStage);
        double evasionMultiplier = getStageMultiplier(evasionStage);

        // Final accuracy = base * accuracy_mult / evasion_mult
        double finalAccuracy = baseAccuracy * accuracyMultiplier / evasionMultiplier;
        // 按 PS 风格在阶段修正后继续叠加特性、道具、天气等额外命中修正。
        finalAccuracy = applyAbilityAccuracyModifier(state, attacker, defender, move, finalAccuracy);
        finalAccuracy = applyItemAccuracyModifier(state, attacker, defender, finalAccuracy);
        finalAccuracy = applyWeatherAccuracyRule(state, move, finalAccuracy);
        // Gravity: 所有招式命中率 +2 阶段等价（约 5/3 倍）
        if (engine.gravityTurns(state) > 0) {
            finalAccuracy = finalAccuracy * 5.0 / 3.0;
        }

        // Clamp to 1-100 range
        return Math.max(1, Math.min(100, (int) Math.floor(finalAccuracy)));
    }

    private double applyAbilityAccuracyModifier(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender,
                                                Map<String, Object> move, double currentAccuracy) {
        String attackerAbility = engine.abilityName(attacker);
        String defenderAbility = engine.abilityName(defender);
        if (matches(attackerAbility, "compound-eyes", "compound eyes")) {
            currentAccuracy *= 1.3d;
        }
        if (matches(attackerAbility, "hustle")
                && engine.toInt(move.get("damage_class_id"), 0) == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            currentAccuracy *= 0.8d;
        }
        if (matches(defenderAbility, "tangled-feet", "tangled feet")
                && engine.volatileFlag(defender, "confused")) {
            currentAccuracy *= 0.5d;
        }
        // 沙隐：沙暴中对手命中率 ×0.8
        if (matches(defenderAbility, "sand-veil", "sand veil")) {
            Map<String, Object> fieldEffects = engine.castMap(state.get("fieldEffects"));
            if (fieldEffects != null) {
                int sandTurns = engine.toInt(fieldEffects.get("sandTurns"), 0);
                if (sandTurns > 0) currentAccuracy *= 0.8d;
            }
        }
        // 雪隐：雪天中对手命中率 ×0.8
        if (matches(defenderAbility, "snow-cloak", "snow cloak")) {
            Map<String, Object> fieldEffects = engine.castMap(state.get("fieldEffects"));
            if (fieldEffects != null) {
                int snowTurns = engine.toInt(fieldEffects.get("snowTurns"), 0);
                if (snowTurns > 0) currentAccuracy *= 0.8d;
            }
        }
        // 奇迹皮肤：变化招式命中率 ×0.5
        if (matches(defenderAbility, "wonder-skin", "wonder skin")) {
            int dmgClass = engine.toInt(move.get("damage_class_id"), 0);
            if (dmgClass == 0) { // 变化招式
                currentAccuracy *= 0.5d;
            }
        }
        return currentAccuracy;
    }

    private double applyItemAccuracyModifier(Map<String, Object> state, Map<String, Object> attacker, Map<String, Object> defender,
                                             double currentAccuracy) {
        String attackerItem = engine.heldItem(attacker);
        if (matches(attackerItem, "wide-lens", "wide lens")) {
            currentAccuracy *= 1.1d;
        }
        // Zoom Lens: 比目标快则 +20% 命中（比较有效速度）
        if (matches(attackerItem, "zoom-lens", "zoom lens")) {
            boolean attackerPlayerSide = engine.isOnSide(state, attacker, true);
            int atkSpeed = engine.speedValue(attacker, state, attackerPlayerSide);
            int defSpeed = engine.speedValue(defender, state, !attackerPlayerSide);
            if (defSpeed > atkSpeed) {
                currentAccuracy *= 1.2d;
            }
        }
        // Micle Berry: 消耗米库果提升下一招命中率（相当于 +1 命中阶段）
        if (engine.volatileFlag(attacker, "micleBerryBoosted")) {
            currentAccuracy *= getStageMultiplier(1);
            engine.setVolatile(attacker, "micleBerryBoosted", false);
        }
        String defenderItem = engine.heldItem(defender);
        if (matches(defenderItem, "bright-powder", "bright powder", "lax-incense", "lax incense")) {
            currentAccuracy *= 0.9d;
        }
        return currentAccuracy;
    }

    private double applyWeatherAccuracyRule(Map<String, Object> state, Map<String, Object> move, double currentAccuracy) {
        int rainTurns = engine.toInt(engine.castMap(state.get("fieldEffects")).get("rainTurns"), 0);
        int sunTurns = engine.toInt(engine.castMap(state.get("fieldEffects")).get("sunTurns"), 0);
        int snowTurns = engine.toInt(engine.castMap(state.get("fieldEffects")).get("snowTurns"), 0);
        if (MoveRegistry.isThunder(move) || MoveRegistry.isHurricane(move)) {
            if (rainTurns > 0) {
                return 100;
            }
            if (sunTurns > 0) {
                return 50;
            }
        }
        if (MoveRegistry.isBlizzard(move) && snowTurns > 0) {
            return 100;
        }
        return currentAccuracy;
    }

    private String orderSourceMessage(String orderSource) {
        if ("quick-claw".equals(orderSource)) {
            return " 的先制之爪发动了";
        }
        if ("custap-berry".equals(orderSource)) {
            return " 的释陀果发动了";
        }
        if ("quick-draw".equals(orderSource)) {
            return " 的速击特性发动了";
        }
        if ("stall".equals(orderSource)) {
            return " 因特性慢出而延后行动";
        }
        if ("lagging-tail".equals(orderSource)) {
            return " 因后攻之尾而延后行动";
        }
        if ("full-incense".equals(orderSource)) {
            return " 因满腹熏香而延后行动";
        }
        return " 获得了行动顺序加成";
    }

    /**
     * Get stat stage multiplier for accuracy/evasion
     * Pokemon Showdown uses: multiplier = (3 + stage) / 3 for stage >= 0
     *                        multiplier = 3 / (3 - stage) for stage < 0
     */
    private double getStageMultiplier(int stage) {
        if (stage >= 0) {
            return (3.0 + stage) / 3.0;
        } else {
            return 3.0 / (3.0 - stage);
        }
    }

    /**
     * Get stat stage value from mon's statStages map
     */
    private int getStatStage(Map<String, Object> mon, String stat) {
        Map<String, Object> statStages = engine.castMap(mon.get("statStages"));
        if (statStages == null) {
            return 0;
        }
        return engine.toInt(statStages.get(stat), 0);
    }

    /**
     * 计算基础能力值经阶级修正后的有效值，公式与 damageSupport.applyStageModifier 一致。
     */
    private int effectiveStat(int baseStat, int stage) {
        int normalized = Math.max(-6, Math.min(6, stage));
        double multiplier = normalized >= 0
                ? (2.0d + normalized) / 2.0d
                : 2.0d / (2.0d - normalized);
        return Math.max(1, (int) Math.floor(baseStat * multiplier));
    }

    /**
     * 防守平分/力量平分：双方对应能力（含阶级修正后）相加平均，
     * 设为双方新基础值并清除阶级。isGuard=true 表示防守平分，false 表示力量平分。
     */
    private boolean handleGuardPowerSplit(Map<String, Object> actor, Map<String, Object> target,
                                           Map<String, Object> move, Map<String, Object> targetLog,
                                           List<String> events, boolean isGuard) {
        Map<String, Object> actorStats = engine.castMap(actor.get("stats"));
        Map<String, Object> targetStats = engine.castMap(target.get("stats"));

        if (isGuard) {
            // 防守平分：防御/特防
            int actDefBase = engine.toInt(actorStats.get("defense"), 0);
            int actSpDBase = engine.toInt(actorStats.get("specialDefense"), 0);
            int tgtDefBase = engine.toInt(targetStats.get("defense"), 0);
            int tgtSpDBase = engine.toInt(targetStats.get("specialDefense"), 0);

            int actDefStage = engine.toInt(engine.statStages(actor).get("defense"), 0);
            int actSpDStage = engine.toInt(engine.statStages(actor).get("specialDefense"), 0);
            int tgtDefStage = engine.toInt(engine.statStages(target).get("defense"), 0);
            int tgtSpDStage = engine.toInt(engine.statStages(target).get("specialDefense"), 0);

            int avgDef = Math.max(1, (effectiveStat(actDefBase, actDefStage) + effectiveStat(tgtDefBase, tgtDefStage)) / 2);
            int avgSpD = Math.max(1, (effectiveStat(actSpDBase, actSpDStage) + effectiveStat(tgtSpDBase, tgtSpDStage)) / 2);

            // 保存原始基础值（仅首次），用于切出恢复
            if (!engine.volatiles(actor).containsKey("guardSplitOrigDef")) {
                engine.setVolatile(actor, "guardSplitOrigDef", actDefBase);
                engine.setVolatile(actor, "guardSplitOrigSpD", actSpDBase);
            }
            if (!engine.volatiles(target).containsKey("guardSplitOrigDef")) {
                engine.setVolatile(target, "guardSplitOrigDef", tgtDefBase);
                engine.setVolatile(target, "guardSplitOrigSpD", tgtSpDBase);
            }

            actorStats.put("defense", avgDef);
            actorStats.put("specialDefense", avgSpD);
            engine.statStages(actor).put("defense", 0);
            engine.statStages(actor).put("specialDefense", 0);
            targetStats.put("defense", avgDef);
            targetStats.put("specialDefense", avgSpD);
            engine.statStages(target).put("defense", 0);
            engine.statStages(target).put("specialDefense", 0);
        } else {
            // 力量平分：攻击/特攻
            int actAtkBase = engine.toInt(actorStats.get("attack"), 0);
            int actSpABase = engine.toInt(actorStats.get("specialAttack"), 0);
            int tgtAtkBase = engine.toInt(targetStats.get("attack"), 0);
            int tgtSpABase = engine.toInt(targetStats.get("specialAttack"), 0);

            int actAtkStage = engine.toInt(engine.statStages(actor).get("attack"), 0);
            int actSpAStage = engine.toInt(engine.statStages(actor).get("specialAttack"), 0);
            int tgtAtkStage = engine.toInt(engine.statStages(target).get("attack"), 0);
            int tgtSpAStage = engine.toInt(engine.statStages(target).get("specialAttack"), 0);

            int avgAtk = Math.max(1, (effectiveStat(actAtkBase, actAtkStage) + effectiveStat(tgtAtkBase, tgtAtkStage)) / 2);
            int avgSpA = Math.max(1, (effectiveStat(actSpABase, actSpAStage) + effectiveStat(tgtSpABase, tgtSpAStage)) / 2);

            if (!engine.volatiles(actor).containsKey("powerSplitOrigAtk")) {
                engine.setVolatile(actor, "powerSplitOrigAtk", actAtkBase);
                engine.setVolatile(actor, "powerSplitOrigSpA", actSpABase);
            }
            if (!engine.volatiles(target).containsKey("powerSplitOrigAtk")) {
                engine.setVolatile(target, "powerSplitOrigAtk", tgtAtkBase);
                engine.setVolatile(target, "powerSplitOrigSpA", tgtSpABase);
            }

            actorStats.put("attack", avgAtk);
            actorStats.put("specialAttack", avgSpA);
            engine.statStages(actor).put("attack", 0);
            engine.statStages(actor).put("specialAttack", 0);
            targetStats.put("attack", avgAtk);
            targetStats.put("specialAttack", avgSpA);
            engine.statStages(target).put("attack", 0);
            engine.statStages(target).put("specialAttack", 0);
        }
        return true;
    }

    /**
     * 防守互换/力量互换：交换双方对应能力的阶级。
     * isGuard=true 表示防守互换（防御/特防），false 表示力量互换（攻击/特攻）。
     */
    private void handleGuardPowerSwap(Map<String, Object> actor, Map<String, Object> target, boolean isGuard) {
        Map<String, Object> actorStages = engine.statStages(actor);
        Map<String, Object> targetStages = engine.statStages(target);

        if (isGuard) {
            // 防守互换
            int actDef = engine.toInt(actorStages.get("defense"), 0);
            int actSpD = engine.toInt(actorStages.get("specialDefense"), 0);
            int tgtDef = engine.toInt(targetStages.get("defense"), 0);
            int tgtSpD = engine.toInt(targetStages.get("specialDefense"), 0);
            actorStages.put("defense", tgtDef);
            actorStages.put("specialDefense", tgtSpD);
            targetStages.put("defense", actDef);
            targetStages.put("specialDefense", actSpD);
        } else {
            // 力量互换
            int actAtk = engine.toInt(actorStages.get("attack"), 0);
            int actSpA = engine.toInt(actorStages.get("specialAttack"), 0);
            int tgtAtk = engine.toInt(targetStages.get("attack"), 0);
            int tgtSpA = engine.toInt(targetStages.get("specialAttack"), 0);
            actorStages.put("attack", tgtAtk);
            actorStages.put("specialAttack", tgtSpA);
            targetStages.put("attack", actAtk);
            targetStages.put("specialAttack", actSpA);
        }
    }

    /**
     * 检查最终手段是否可以使用：所有非最终手段的已知招式都必须已使用过至少一次。
     * 使用时检查 mon 的 usedMoves 集合，在 rememberLastMove 中自动填充。
     */
    @SuppressWarnings("unchecked")
    private boolean canUseLastResort(Map<String, Object> actor) {
        List<Map<String, Object>> knownMoves = engine.castList(actor.get("moves"));
        Set<String> usedMoves = (Set<String>) actor.get("usedMoves");
        if (knownMoves.isEmpty() || usedMoves == null || usedMoves.isEmpty()) {
            return false;
        }
        for (Map<String, Object> knownMove : knownMoves) {
            String moveName = String.valueOf(knownMove.get("name_en")).toLowerCase();
            if (moveName.isBlank()) continue;
            // 跳过最终手段自身
            if (MoveRegistry.isLastResort(knownMove)) continue;
            // 如果还有招式没用过，最终手段不能使用
            if (!usedMoves.contains(moveName)) {
                return false;
            }
        }
        return true;
    }
}
