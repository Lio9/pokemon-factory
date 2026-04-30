package com.lio9.battle.engine;



import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleRoundSupport {
    /**
     * 单回合动作执行器。
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
        Map<String, Object> forcedChargeMove = chargingMove(actor);
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
            if (engine.isSleepingThisTurn(actor, round)) {
                Map<String, Object> sleepLog = new LinkedHashMap<>();
                sleepLog.put("side", action.side());
                sleepLog.put("actor", actor.get("name"));
                sleepLog.put("actionType", "sleep");
                sleepLog.put("result", "asleep");
                actionLogs.add(sleepLog);
                events.add(actor.get("name") + " 正在睡觉，无法行动");
                return;
            }
            actor.put("condition", null);
            actor.put("sleepTurns", 0);
            actor.put("sleepAppliedRound", 0);
            events.add(actor.get("name") + " 醒来了");
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

        if (forcedChargeMove == null && action.isSwitch()) {
            handleSwitch(state, action, actingTeam, actor, playerSide, actionLogs, events, actionLog);
            return;
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
        if (!MoveRegistry.isProtectionMove(move)) {
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
        }

        if (handleSupportMove(state, action, actor, move, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                redirectionTargets, helpingHandBoosts, actionLogs, events, actionLog, playerSide)) {
            return;
        }

        List<BattleEngine.TargetRef> targets = targetSupport.resolveMoveTargets(state, action, move, random, redirectionTargets);
        if (targets.isEmpty()) {
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
            Map<String, Object> target = targetSideTeam.get(targetRef.teamIndex());
            Map<String, Object> targetLog = new LinkedHashMap<>(actionLog);
            targetLog.put("target", target.get("name"));
            targetLog.put("targetFieldSlot", targetRef.fieldSlot());
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
                    actionLogs.add(targetLog);
                    events.add(target.get("name") + " 通过 Protect 挡住了攻击");
                    continue;
                }
            }

            // Calculate accuracy with stages (Pokemon Showdown standard)
            int accuracy = calculateAccuracyWithStages(state, actor, target, move);
            if (random.nextInt(100) + 1 > accuracy) {
                targetLog.put("result", "miss");
                targetLog.put("damage", 0);
                actionLogs.add(targetLog);
                events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 攻击落空");
                continue;
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
            if (engine.isStatusMove(move)
                    && handleStatusMove(state, action, statusSource, statusTarget, move, targetLog, events, random, round, actionLogs)) {
                continue;
            }

            selfStatChangeTriggered = true;
            if (conditionSupport.applyDefenderAbilityImmunity(actor, target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }

            // Substitute: 替身在场时，伤害先扣替身 HP，替身消失后剩余伤害继续
            Object subHpObj = engine.volatileValue(target, "substitute", null);
            int subHp = subHpObj instanceof Integer ? (Integer) subHpObj : 0;

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
            for (int hitIndex = 0; hitIndex < hitCount && remainingHp > 0; hitIndex++) {
                int hpBeforeDamage = engine.toInt(target.get("currentHp"), 0);
                // 暴击在执行层预先解析，再传给伤害层消费，避免重复判定导致日志与数值不一致。
                boolean criticalHit = resolveCriticalHit(actor, move, random);
                Map<String, Object> resolvedMove = new LinkedHashMap<>(baseDamageMove);
                resolvedMove.put("criticalHit", criticalHit);
                if (criticalHit) {
                    criticalHits += 1;
                }
                int damage = engine.calculateDamage(actor, target, resolvedMove, random, helpingHandBoosts, state);

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

                remainingHp = engine.applyIncomingDamage(actor, target, damage, targetLog, events);
                int actualDamage = engine.toInt(targetLog.get("damage"), damage);
                target.put("currentHp", remainingHp);
                conditionSupport.thawFromFireHit(target, move, targetLog, events);
                if (actualDamage > 0 && engine.isKnockOff(move)) {
                    conditionSupport.applyKnockOff(target, targetLog, events);
                }
                hitDamages.add(actualDamage);
                totalActualDamage += actualDamage;
                conditionSupport.applyReactiveDamageAbilities(actor, target, move, hpBeforeDamage, remainingHp, actualDamage, targetLog, events);
                engine.applyDefenderItemEffects(target, move, actualDamage, targetLog, events);
                // Eject Button / Red Card 触发换人标记
                if (Boolean.TRUE.equals(targetLog.get("ejectButton"))) {
                    autoSwitchAfterMove(state, action, target, move, "eject-button", events, actionLogs);
                } else if (Boolean.TRUE.equals(targetLog.get("redCard")) && actualDamage > 0
                        && engine.toInt(actor.get("currentHp"), 0) > 0) {
                    autoSwitchAfterMove(state, action, actor, move, "red-card", events, actionLogs);
                }
                conditionSupport.applyReactiveContactEffects(state, actor, target, move, targetLog, events, random);
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
            // Rapid Spin: 清除己方场地钉
            if (MoveRegistry.isRapidSpin(move) && totalActualDamage > 0) {
                engine.clearSideHazards(state, "player".equals(action.side()));
                events.add(actor.get("name") + " 用高速旋转清除了场地钉");
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

            conditionSupport.applyDrainHealing(actor, move, totalActualDamage, targetLog, events);
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
            }
            if (remainingHp == 0) {
                events.add(target.get("name") + " 倒下了");
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
        if (selfStatChangeTriggered && engine.toInt(actor.get("currentHp"), 0) > 0) {
            conditionSupport.applyDamagingSelfStatChanges(actor, move, actionLog, events, random);
        }
        if (anyHit) {
            engine.applyAttackerItemEffects(actor, totalDamage, actionLog, events);
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
        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
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
    }

    private boolean resolveCriticalHit(Map<String, Object> actor, Map<String, Object> move, Random random) {
        return engine.rollCriticalHit(actor, move, random);
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
                return true; // Shadow Tag traps everything except Ghost
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

    private boolean matches(String value, String... names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void handleSwitch(Map<String, Object> state, BattleEngine.Action action, List<Map<String, Object>> actingTeam,
                              Map<String, Object> actor, boolean playerSide, List<Map<String, Object>> actionLogs,
                              List<String> events, Map<String, Object> actionLog) {
        // 捕获特性检查
        if (isBlockedByTrappingAbility(state, playerSide, actor)) {
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
            return handleProtectionMove(actor, move, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                    action.side(), action.actorIndex(), actionLog, actionLogs, events);
        }
        if (engine.isTailwind(move)) {
            engine.activateTailwind(state, playerSide, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isTrickRoom(move)) {
            engine.toggleTrickRoom(state, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isRainDance(move)) {
            engine.activateWeather(state, "rain", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isSunnyDay(move)) {
            engine.activateWeather(state, "sun", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isSandstorm(move)) {
            engine.activateWeather(state, "sand", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isSnowWeather(move)) {
            engine.activateWeather(state, "snow", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isElectricTerrain(move)) {
            engine.activateTerrain(state, "electric", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isPsychicTerrain(move)) {
            engine.activateTerrain(state, "psychic", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isGrassyTerrain(move)) {
            engine.activateTerrain(state, "grassy", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isMistyTerrain(move)) {
            engine.activateTerrain(state, "misty", actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isReflect(move)) {
            engine.activateScreen(state, "reflect", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isLightScreen(move)) {
            engine.activateScreen(state, "light-screen", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isAuroraVeil(move)) {
            if (engine.snowTurns(state) <= 0) {
                actionLog.put("result", "failed");
                actionLogs.add(actionLog);
                events.add(actor.get("name") + " 想展开极光幕，但当前没有雪天");
                return true;
            }
            engine.activateScreen(state, "aurora-veil", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isSafeguard(move)) {
            engine.activateScreen(state, "safeguard", playerSide, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isRedirectionMove(move)) {
            targetSupport.activateRedirection(redirectionTargets, action.side(), action.actorIndex(), move, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isHelpingHand(move)) {
            targetSupport.applyHelpingHand(state, action, actor, move, actionLog, events, helpingHandBoosts);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isAllySwitch(move)) {
            targetSupport.applyAllySwitch(state, action, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (conditionSupport.applyMoveHealing(actor, move, actionLog, events)) {
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
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
            conditionSupport.applyPerishSong(state, actor, target, targetLog, events);
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
        
        // Self-boosting moves
        if (engine.isSwordsDance(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(actor, "attack", 2, "剑舞", events);
            targetLog.put("result", succeeded ? "swords-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isNastyPlot(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(actor, "specialAttack", 2, "诡计", events);
            targetLog.put("result", succeeded ? "nasty-plot" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isDragonDance(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor, 
                Map.of("attack", 1, "speed", 1), "龙舞", events);
            targetLog.put("result", succeeded ? "dragon-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isCalmMind(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor,
                Map.of("specialAttack", 1, "specialDefense", 1), "冥想", events);
            targetLog.put("result", succeeded ? "calm-mind" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isAgility(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(actor, "speed", 2, "高速移动", events);
            targetLog.put("result", succeeded ? "agility" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isAutotomize(move)) {
            boolean succeeded = conditionSupport.applySelfStatBoost(actor, "speed", 2, "轻量化", events);
            targetLog.put("result", succeeded ? "autotomize" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isBulkUp(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor,
                Map.of("attack", 1, "defense", 1), "健美", events);
            targetLog.put("result", succeeded ? "bulk-up" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isWorkUp(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor,
                Map.of("attack", 1, "specialAttack", 1), "振作", events);
            targetLog.put("result", succeeded ? "work-up" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isQuiverDance(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor,
                Map.of("specialAttack", 1, "specialDefense", 1, "speed", 1), "蝶舞", events);
            targetLog.put("result", succeeded ? "quiver-dance" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isCoil(move)) {
            boolean succeeded = conditionSupport.applyMultiStatBoost(actor,
                Map.of("attack", 1, "defense", 1, "accuracy", 1), "盘蜷", events);
            targetLog.put("result", succeeded ? "coil" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        if (engine.isShellSmash(move)) {
            // Shell Smash: Atk +2, SpA +2, Spe +2, Def -1, SpD -1
            boolean anyChange = conditionSupport.applyMultiStatBoost(actor,
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
        if (engine.isMorningSun(move)) {
            boolean succeeded = conditionSupport.applyRecoveryMove(actor, move, "晨光", events);
            targetLog.put("result", succeeded ? "morning-sun" : "failed");
            actionLogs.add(targetLog);
            return true;
        }
        
        return false;
    }

    private boolean finishNonDamagingMove(Map<String, Object> actor, Map<String, Object> move,
                                          Map<String, Object> actionLog, List<Map<String, Object>> actionLogs) {
        actionLogs.add(actionLog);
        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
        return true;
    }

    private boolean handleProtectionMove(Map<String, Object> actor, Map<String, Object> move, int round, Random random,
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
        engine.applyCooldown(actor, move);
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

    private void autoSwitchAfterMove(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                     Map<String, Object> move, String switchResult,
                                     List<String> events, List<Map<String, Object>> actionLogs) {
        boolean playerSide = "player".equals(action.side());
        List<Map<String, Object>> team = engine.team(state, playerSide);
        List<Integer> activeSlots = engine.activeSlots(state, playerSide);
        int switchToIndex = engine.firstAvailableBench(team, activeSlots);
        if (switchToIndex < 0) {
            engine.rememberLastMove(actor, move);
            engine.rememberChoiceMove(actor, move);
            engine.applyCooldown(actor, move);
            return;
        }
        List<Integer> previousSlots = new ArrayList<>(activeSlots);
        Map<String, Object> switchedIn = team.get(switchToIndex);
        engine.rememberLastMove(actor, move);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
        conditionSupport.applySwitchOutEffects(actor, events);
        actor.put("choiceLockedMove", null);
        conditionSupport.resetBattleStages(actor);
        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
        engine.setVolatile(switchedIn, "flinch", false);
        engine.replaceActiveSlot(state, playerSide, action.actorFieldSlot(), switchToIndex);
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
        finalAccuracy = applyAbilityAccuracyModifier(attacker, defender, move, finalAccuracy);
        finalAccuracy = applyItemAccuracyModifier(attacker, defender, finalAccuracy);
        finalAccuracy = applyWeatherAccuracyRule(state, move, finalAccuracy);
        
        // Clamp to 1-100 range
        return Math.max(1, Math.min(100, (int) Math.floor(finalAccuracy)));
    }

    private double applyAbilityAccuracyModifier(Map<String, Object> attacker, Map<String, Object> defender,
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
        return currentAccuracy;
    }

    private double applyItemAccuracyModifier(Map<String, Object> attacker, Map<String, Object> defender,
                                             double currentAccuracy) {
        String attackerItem = engine.heldItem(attacker);
        if (matches(attackerItem, "wide-lens", "wide lens")) {
            currentAccuracy *= 1.1d;
        }
        String defenderItem = engine.heldItem(defender);
        if (matches(defenderItem, "bright-powder", "bright powder")) {
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
}
