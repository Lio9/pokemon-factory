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

        Map<String, Object> actionLog = new LinkedHashMap<>();
        actionLog.put("side", action.side());
        actionLog.put("actor", actor.get("name"));

        if (action.isSwitch()) {
            handleSwitch(state, action, actingTeam, actor, playerSide, actionLogs, events, actionLog);
            return;
        }

        Map<String, Object> move = action.move();
        actionLog.put("move", move.get("name"));
        if (action.specialSystemRequested() != null) {
            engine.activateSpecialSystem(state, playerSide, actor, move, action.specialSystemRequested(), round, actionLog, events);
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

            if (engine.isStatusMove(move) && conditionSupport.isStatusMoveBlockedByAbility(actor, target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }
            if (engine.isStatusMove(move) && handleStatusMove(state, action, actor, target, move, targetLog, events, random, round, actionLogs)) {
                continue;
            }

            if (conditionSupport.applyDefenderAbilityImmunity(target, move, targetLog, events)) {
                actionLogs.add(targetLog);
                continue;
            }

            int damage = engine.calculateDamage(actor, target, move, random, helpingHandBoosts, state);
            if (targets.size() > 1 && engine.isSpreadMove(move)) {
                damage = Math.max(1, (int) Math.floor(damage * 0.75d));
            }
            int remainingHp = engine.applyIncomingDamage(target, damage, targetLog, events);
            int actualDamage = engine.toInt(targetLog.get("damage"), damage);
            target.put("currentHp", remainingHp);
            if (remainingHp == 0) {
                target.put("status", "fainted");
            }

            targetLog.put("result", "hit");
            targetLog.put("damage", actualDamage);
            targetLog.put("targetHpAfter", remainingHp);
            actionLogs.add(targetLog);
            events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 使用 " + move.get("name")
                    + " 对 " + target.get("name") + " 造成了 " + actualDamage + " 点伤害");

            engine.applyDefenderItemEffects(target, move, actualDamage, targetLog, events);
            conditionSupport.applyReactiveContactEffects(actor, target, move, targetLog, events);
            totalDamage += actualDamage;
            anyHit = true;
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
            if (remainingHp == 0) {
                events.add(target.get("name") + " 倒下了");
            }
        }

        if (anyHit) {
            engine.applyAttackerItemEffects(actor, totalDamage, actionLog, events);
        }
        actor.remove("zMoveRound");
        actor.remove("zMoveBase");
        if ("z-move".equals(actor.get("specialSystemActivated"))) {
            actor.put("specialSystemActivated", null);
        }
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
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
