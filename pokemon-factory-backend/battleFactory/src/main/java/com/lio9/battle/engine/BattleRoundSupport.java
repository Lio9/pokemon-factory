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
        if (engine.tauntTurns(actor) > 0 && engine.isStatusMove(move)) {
            actionLog.put("result", "taunted");
            actionLogs.add(actionLog);
            events.add(actor.get("name") + " 因挑衅无法使出变化招式");
            return;
        }

        if (handleSupportMove(state, action, actor, move, random, protectedTargets, redirectionTargets,
                helpingHandBoosts, actionLogs, events, actionLog, playerSide)) {
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

            if (conditionSupport.isBlockedByPsychicTerrain(state, action.side(), target, move)) {
                targetLog.put("result", "psychic-terrain-blocked");
                targetLog.put("damage", 0);
                events.add(target.get("name") + " 受到精神场地保护，挡住了先制招式");
                actionLogs.add(targetLog);
                continue;
            }

            if (protectedTargets.getOrDefault(engine.protectionKey(targetRef.side(), targetRef.teamIndex()), false)) {
                targetLog.put("result", "blocked");
                targetLog.put("damage", 0);
                events.add(target.get("name") + " 通过 Protect 挡住了攻击");
                actionLogs.add(targetLog);
                continue;
            }

            int accuracy = Math.max(1, engine.toInt(move.get("accuracy"), 100));
            if (random.nextInt(100) + 1 > accuracy) {
                targetLog.put("result", "miss");
                targetLog.put("damage", 0);
                events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 攻击落空");
                actionLogs.add(targetLog);
                continue;
            }

            if (handleStatusMove(state, actor, target, move, targetLog, events, random, round, actionLogs)) {
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
                target.put("flinched", true);
                targetLog.put("flinch", true);
                events.add(target.get("name") + " 畏缩了");
            }
            if (engine.isIcyWind(move) && remainingHp > 0) {
                conditionSupport.applySpeedDrop(actor, target, targetLog, events);
            }
            if (remainingHp == 0) {
                events.add(target.get("name") + " 倒下了");
            }
        }

        if (anyHit) {
            engine.applyAttackerItemEffects(actor, totalDamage, actionLog, events);
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
                                      Map<String, Object> move, Random random,
                                      Map<String, Boolean> protectedTargets,
                                      Map<String, BattleEngine.RedirectionEffect> redirectionTargets,
                                      Map<Map<String, Object>, Boolean> helpingHandBoosts,
                                      List<Map<String, Object>> actionLogs, List<String> events,
                                      Map<String, Object> actionLog, boolean playerSide) {
        if (engine.isProtect(move)) {
            protectedTargets.put(engine.protectionKey(action.side(), action.actorIndex()), true);
            actionLog.put("result", "protect");
            events.add(engine.sideName(action.side()) + " 的 " + actor.get("name") + " 使用了 Protect");
            actionLogs.add(actionLog);
            engine.applyCooldown(actor, move);
            return true;
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
        if (engine.isRedirectionMove(move)) {
            targetSupport.activateRedirection(redirectionTargets, action.side(), action.actorIndex(), move, actor, actionLog, events);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        if (engine.isHelpingHand(move)) {
            targetSupport.applyHelpingHand(state, action, actor, move, actionLog, events, helpingHandBoosts);
            return finishNonDamagingMove(actor, move, actionLog, actionLogs);
        }
        return false;
    }

    private boolean handleStatusMove(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                     Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                     Random random, int round, List<Map<String, Object>> actionLogs) {
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
        return false;
    }

    private boolean finishNonDamagingMove(Map<String, Object> actor, Map<String, Object> move,
                                          Map<String, Object> actionLog, List<Map<String, Object>> actionLogs) {
        actionLogs.add(actionLog);
        engine.rememberChoiceMove(actor, move);
        engine.applyCooldown(actor, move);
        return true;
    }

    private boolean isFakeOut(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "fake-out".equalsIgnoreCase(nameEn) || "fake out".equalsIgnoreCase(nameEn);
    }
}