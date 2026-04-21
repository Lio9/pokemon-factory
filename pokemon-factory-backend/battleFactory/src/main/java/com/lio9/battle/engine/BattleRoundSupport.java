package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleRoundSupport {
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
                       Map<Map<String, Object>, Boolean> helpingHandBoosts,
                       List<Map<String, Object>> actionLogs, List<String> events) {
        boolean playerSide = "player".equals(action.side());
        List<Map<String, Object>> actingTeam = engine.team(state, playerSide);
        if (!engine.isAvailableMon(actingTeam, action.actorIndex())) {
            return;
        }

        Map<String, Object> actor = actingTeam.get(action.actorIndex());
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
        if (Boolean.TRUE.equals(actor.get("flinched"))) {
            Map<String, Object> flinchLog = new LinkedHashMap<>();
            flinchLog.put("side", action.side());
            flinchLog.put("actor", actor.get("name"));
            flinchLog.put("actionType", "flinch");
            flinchLog.put("result", "flinch");
            actionLogs.add(flinchLog);
            events.add(actor.get("name") + " 畏缩了，无法行动");
            actor.put("flinched", false);
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

        Map<String, Object> actionLog = new LinkedHashMap<>();
        actionLog.put("side", action.side());
        actionLog.put("actor", actor.get("name"));

        if (forcedChargeMove == null && action.isSwitch()) {
            handleSwitch(state, action, actingTeam, actor, playerSide, actionLogs, events, actionLog);
            return;
        }

        Map<String, Object> move = forcedChargeMove != null ? forcedChargeMove : action.move();
        actionLog.put("move", move.get("name"));
        if (forcedChargeMove == null && action.specialSystemRequested() != null) {
            engine.activateSpecialSystem(state, playerSide, actor, move, action.specialSystemRequested(), round, actionLog, events);
        }
        if (forcedChargeMove == null && shouldStartCharging(actor, move, state)) {
            startCharging(actor, move, actionLog, actionLogs, events);
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
        if (!engine.isProtectionMove(move)) {
            actor.put("protectionStreak", 0);
            actor.put("lastProtectionRound", 0);
        }

        if (handleSupportMove(state, action, actor, move, round, random, protectedTargets, wideGuardSides, quickGuardSides,
                redirectionTargets, helpingHandBoosts, actionLogs, events, actionLog, playerSide)) {
            return;
        }

        List<BattleEngine.TargetRef> targets = targetSupport.resolveMoveTargets(state, action, move, random, redirectionTargets);
        if (targets.isEmpty()) {
            return;
        }

        int totalDamage = 0;
        boolean anyHit = false;
        for (BattleEngine.TargetRef targetRef : targets) {
            List<Map<String, Object>> targetSideTeam = engine.team(state, targetRef.playerSide());
            if (!engine.isAvailableMon(targetSideTeam, targetRef.teamIndex())) {
                continue;
            }
            Map<String, Object> target = targetSideTeam.get(targetRef.teamIndex());
            Map<String, Object> targetLog = new LinkedHashMap<>(actionLog);
            targetLog.put("target", target.get("name"));
            targetLog.put("targetFieldSlot", targetRef.fieldSlot());

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

            String protectionKey = engine.protectionKey(targetRef.side(), targetRef.teamIndex());
            if (protectedTargets.getOrDefault(protectionKey, false)) {
                if (engine.isFeint(move)) {
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

            int accuracy = Math.max(1, engine.toInt(move.get("accuracy"), 100));
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

            if (conditionSupport.applyDefenderAbilityImmunity(target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }

            int hitCount = resolveHitCount(actor, move, random);
            int totalActualDamage = 0;
            int criticalHits = 0;
            int remainingHp = engine.toInt(target.get("currentHp"), 0);
            List<Integer> hitDamages = new ArrayList<>();
            for (int hitIndex = 0; hitIndex < hitCount && remainingHp > 0; hitIndex++) {
                boolean criticalHit = resolveCriticalHit(actor, move, random);
                Map<String, Object> resolvedMove = move;
                if (criticalHit) {
                    resolvedMove = new LinkedHashMap<>(move);
                    resolvedMove.put("criticalHit", true);
                    criticalHits += 1;
                }
                int damage = engine.calculateDamage(actor, target, resolvedMove, random, helpingHandBoosts, state);
                if (targets.size() > 1 && engine.isSpreadMove(move)) {
                    damage = Math.max(1, (int) Math.floor(damage * 0.75d));
                }
                remainingHp = engine.applyIncomingDamage(target, damage, targetLog, events);
                int actualDamage = engine.toInt(targetLog.get("damage"), damage);
                target.put("currentHp", remainingHp);
                conditionSupport.thawFromFireHit(target, move, targetLog, events);
                hitDamages.add(actualDamage);
                totalActualDamage += actualDamage;
                engine.applyDefenderItemEffects(target, move, actualDamage, targetLog, events);
                conditionSupport.applyReactiveContactEffects(actor, target, move, targetLog, events);
                if (remainingHp == 0) {
                    target.put("status", "fainted");
                }
            }

            targetLog.put("result", "hit");
            targetLog.put("damage", totalActualDamage);
            targetLog.put("hitCount", hitDamages.size());
            targetLog.put("hitDamages", hitDamages);
            targetLog.put("critical", criticalHits > 0);
            targetLog.put("criticalHits", criticalHits);
            targetLog.put("targetHpAfter", remainingHp);
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
                    target.put("flinched", true);
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

        if (anyHit) {
            engine.applyAttackerItemEffects(actor, totalDamage, actionLog, events);
        }
        if (anyHit && engine.isRechargeMove(move)) {
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
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
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
        actor.put("chargingMove", null);
        actor.put("chargingTurns", 0);
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

    private void startCharging(Map<String, Object> actor, Map<String, Object> move, Map<String, Object> actionLog,
                               List<Map<String, Object>> actionLogs, List<String> events) {
        actor.put("chargingMove", move.get("name_en"));
        actor.put("chargingTurns", 1);
        actionLog.put("result", "charge");
        actionLog.put("charging", true);
        actionLogs.add(actionLog);
        events.add(actor.get("name") + " 正在蓄力");
    }

    private void clearCharging(Map<String, Object> actor) {
        actor.put("chargingMove", null);
        actor.put("chargingTurns", 0);
    }

    private boolean resolveCriticalHit(Map<String, Object> actor, Map<String, Object> move, Random random) {
        int critStage = Math.max(0, engine.toInt(move.get("crit_rate"), 0));
        String ability = engine.abilityName(actor);
        if ("super-luck".equalsIgnoreCase(ability) || "super luck".equalsIgnoreCase(ability)) {
            critStage += 1;
        }
        String heldItem = engine.heldItem(actor);
        if ("scope-lens".equalsIgnoreCase(heldItem) || "scope lens".equalsIgnoreCase(heldItem)
                || "razor-claw".equalsIgnoreCase(heldItem) || "razor claw".equalsIgnoreCase(heldItem)) {
            critStage += 1;
        }
        return critStage >= 3;
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
        switchedIn.put("flinched", false);
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
        if (engine.isProtectionMove(move)) {
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
        if (engine.isPartingShot(move)) {
            boolean succeeded = conditionSupport.applyAttackAndSpecialAttackDrop(actor, target, targetLog, events);
            targetLog.put("result", succeeded ? "parting-shot" : "failed");
            actionLogs.add(targetLog);
            if (succeeded) {
                autoSwitchAfterPartingShot(state, action, actor, events, actionLogs);
            }
            return true;
        }
        return false;
    }

    private boolean finishNonDamagingMove(Map<String, Object> actor, Map<String, Object> move,
                                          Map<String, Object> actionLog, List<Map<String, Object>> actionLogs) {
        actionLogs.add(actionLog);
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
        if (engine.isWideGuard(move)) {
            wideGuardSides.put(side, true);
            actionLog.put("result", "wide-guard");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 Wide Guard");
        } else if (engine.isQuickGuard(move)) {
            quickGuardSides.put(side, true);
            actionLog.put("result", "quick-guard");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 Quick Guard");
        } else {
            protectedTargets.put(engine.protectionKey(side, actorIndex), true);
            actionLog.put("result", "protect");
            events.add(engine.sideName(side) + " 的 " + actor.get("name") + " 使用了 " + move.get("name"));
        }
        actionLogs.add(actionLog);
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

    private void autoSwitchAfterPartingShot(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                                            List<String> events, List<Map<String, Object>> actionLogs) {
        boolean playerSide = "player".equals(action.side());
        List<Map<String, Object>> team = engine.team(state, playerSide);
        List<Integer> activeSlots = engine.activeSlots(state, playerSide);
        int switchToIndex = engine.firstAvailableBench(team, activeSlots);
        if (switchToIndex < 0) {
            return;
        }
        List<Integer> previousSlots = new ArrayList<>(activeSlots);
        Map<String, Object> switchedIn = team.get(switchToIndex);
        conditionSupport.applySwitchOutEffects(actor, events);
        actor.put("choiceLockedMove", null);
        conditionSupport.resetBattleStages(actor);
        switchedIn.put("entryRound", engine.toInt(state.get("currentRound"), 0) + 1);
        switchedIn.put("flinched", false);
        engine.replaceActiveSlot(state, playerSide, action.actorFieldSlot(), switchToIndex);
        Map<String, Object> switchLog = new LinkedHashMap<>();
        switchLog.put("side", action.side());
        switchLog.put("actor", actor.get("name"));
        switchLog.put("actionType", "switch");
        switchLog.put("switchTo", switchedIn.get("name"));
        switchLog.put("result", "parting-shot-switch");
        actionLogs.add(switchLog);
        events.add(engine.sideName(action.side()) + " 收回了 " + actor.get("name") + "，派出了 " + switchedIn.get("name"));
        conditionSupport.applyEntryAbilities(state, playerSide, previousSlots, events);
    }

    private boolean isFakeOut(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "fake-out".equalsIgnoreCase(nameEn) || "fake out".equalsIgnoreCase(nameEn);
    }
}
