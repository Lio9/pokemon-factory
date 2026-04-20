package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleTargetSupport {
    private final BattleEngine engine;

    BattleTargetSupport(BattleEngine engine) {
        this.engine = engine;
    }

    List<BattleEngine.TargetRef> resolveMoveTargets(Map<String, Object> state, BattleEngine.Action action,
                                                    Map<String, Object> move, Random random,
                                                    Map<String, BattleEngine.RedirectionEffect> redirectionTargets) {
        int targetId = targetId(move);
        return switch (targetId) {
            case 7, 4 -> List.of(new BattleEngine.TargetRef(action.side(), "player".equals(action.side()),
                    action.actorIndex(), action.actorFieldSlot()));
            case 11, 6 -> activeTargetRefs(state, !"player".equals(action.side()));
            case 8 -> randomOpponentTargetRefs(state, !"player".equals(action.side()), random);
            case 9 -> allOtherActiveTargetRefs(state, action);
            case 13, 5 -> activeTargetRefs(state, "player".equals(action.side()));
            case 14, 12 -> allActiveTargetRefs(state);
            case 3 -> allyTargetRefs(state, action);
            default -> singleOpponentTargetRefs(state, !"player".equals(action.side()), action.targetFieldSlot(),
                    redirectionTargets, engine.team(state, "player".equals(action.side())).get(action.actorIndex()));
        };
    }

    int targetIndex(Map<String, Object> state, boolean playerTarget, int targetFieldSlot) {
        List<Integer> targets = engine.activeSlots(state, playerTarget);
        if (targets.isEmpty()) {
            return -1;
        }
        int normalizedSlot = Math.max(0, Math.min(targets.size() - 1, targetFieldSlot));
        return targets.get(normalizedSlot);
    }

    void activateRedirection(Map<String, BattleEngine.RedirectionEffect> redirectionTargets, String side, int actorIndex,
                             Map<String, Object> move, Map<String, Object> actor,
                             Map<String, Object> actionLog, List<String> events) {
        redirectionTargets.put(side, new BattleEngine.RedirectionEffect(actorIndex, isRagePowder(move)));
        actionLog.put("result", "redirection");
        actionLog.put("actionType", "redirection");
        events.add(actor.get("name") + " 使用了 " + move.get("name") + "，吸引了对手的招式");
    }

    void applyHelpingHand(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                          Map<String, Object> move, Map<String, Object> actionLog, List<String> events,
                          Map<Map<String, Object>, Boolean> helpingHandBoosts) {
        List<BattleEngine.TargetRef> targets = allyAssistTargetRefs(state, action);
        if (targets.isEmpty()) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了 " + move.get("name") + "，但失败了");
            return;
        }
        BattleEngine.TargetRef targetRef = targets.get(0);
        Map<String, Object> target = engine.team(state, targetRef.playerSide()).get(targetRef.teamIndex());
        helpingHandBoosts.put(target, true);
        actionLog.put("result", "helping-hand");
        actionLog.put("target", target.get("name"));
        events.add(actor.get("name") + " 使用了 " + move.get("name") + "，帮助了 " + target.get("name"));
    }

    boolean applyAllySwitch(Map<String, Object> state, BattleEngine.Action action, Map<String, Object> actor,
                            Map<String, Object> actionLog, List<String> events) {
        boolean playerSide = "player".equals(action.side());
        List<Integer> active = new ArrayList<>(engine.activeSlots(state, playerSide));
        if (active.size() < 2 || action.actorFieldSlot() < 0 || action.actorFieldSlot() >= active.size()) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了 Ally Switch，但失败了");
            return false;
        }
        int otherFieldSlot = action.actorFieldSlot() == 0 ? 1 : 0;
        if (otherFieldSlot >= active.size()) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 使用了 Ally Switch，但失败了");
            return false;
        }
        Integer current = active.get(action.actorFieldSlot());
        Integer other = active.get(otherFieldSlot);
        active.set(action.actorFieldSlot(), other);
        active.set(otherFieldSlot, current);
        state.put(playerSide ? "playerActiveSlots" : "opponentActiveSlots", active);
        actionLog.put("result", "ally-switch");
        events.add(actor.get("name") + " 与队友交换了位置");
        return true;
    }

    private int fieldSlotForTeamIndex(Map<String, Object> state, boolean playerSide, int teamIndex) {
        List<Integer> active = engine.activeSlots(state, playerSide);
        for (int fieldSlot = 0; fieldSlot < active.size(); fieldSlot++) {
            if (active.get(fieldSlot) == teamIndex) {
                return fieldSlot;
            }
        }
        return 0;
    }

    private int redirectedTargetIndex(Map<String, Object> state, boolean playerTarget,
                                      Map<String, BattleEngine.RedirectionEffect> redirectionTargets,
                                      Map<String, Object> attacker) {
        BattleEngine.RedirectionEffect redirected = redirectionTargets.get(playerTarget ? "player" : "opponent");
        if (redirected == null) {
            return -1;
        }
        if (redirected.powder() && engine.isPowderImmune(attacker)) {
            return -1;
        }
        return engine.isAvailableMon(engine.team(state, playerTarget), redirected.actorIndex()) ? redirected.actorIndex() : -1;
    }

    private List<BattleEngine.TargetRef> singleOpponentTargetRefs(Map<String, Object> state, boolean playerTarget,
                                                                  int targetFieldSlot,
                                                                  Map<String, BattleEngine.RedirectionEffect> redirectionTargets,
                                                                  Map<String, Object> attacker) {
        int redirected = redirectedTargetIndex(state, playerTarget, redirectionTargets, attacker);
        int teamIndex = redirected >= 0 ? redirected : targetIndex(state, playerTarget, targetFieldSlot);
        if (teamIndex < 0) {
            return List.of();
        }
        return List.of(new BattleEngine.TargetRef(playerTarget ? "player" : "opponent", playerTarget, teamIndex,
                fieldSlotForTeamIndex(state, playerTarget, teamIndex)));
    }

    private List<BattleEngine.TargetRef> randomOpponentTargetRefs(Map<String, Object> state, boolean playerTarget,
                                                                  Random random) {
        List<BattleEngine.TargetRef> targets = activeTargetRefs(state, playerTarget);
        if (targets.isEmpty()) {
            return List.of();
        }
        return List.of(targets.get(random.nextInt(targets.size())));
    }

    private List<BattleEngine.TargetRef> activeTargetRefs(Map<String, Object> state, boolean playerSide) {
        List<BattleEngine.TargetRef> targets = new ArrayList<>();
        List<Integer> active = engine.activeSlots(state, playerSide);
        for (int fieldSlot = 0; fieldSlot < active.size(); fieldSlot++) {
            int teamIndex = active.get(fieldSlot);
            if (engine.isAvailableMon(engine.team(state, playerSide), teamIndex)) {
                targets.add(new BattleEngine.TargetRef(playerSide ? "player" : "opponent", playerSide, teamIndex, fieldSlot));
            }
        }
        return targets;
    }

    private List<BattleEngine.TargetRef> allOtherActiveTargetRefs(Map<String, Object> state, BattleEngine.Action action) {
        List<BattleEngine.TargetRef> targets = new ArrayList<>();
        for (BattleEngine.TargetRef target : activeTargetRefs(state, true)) {
            if (!("player".equals(action.side()) && target.teamIndex() == action.actorIndex())) {
                targets.add(target);
            }
        }
        for (BattleEngine.TargetRef target : activeTargetRefs(state, false)) {
            if (!("opponent".equals(action.side()) && target.teamIndex() == action.actorIndex())) {
                targets.add(target);
            }
        }
        return targets;
    }

    private List<BattleEngine.TargetRef> allActiveTargetRefs(Map<String, Object> state) {
        List<BattleEngine.TargetRef> targets = new ArrayList<>();
        targets.addAll(activeTargetRefs(state, true));
        targets.addAll(activeTargetRefs(state, false));
        return targets;
    }

    private List<BattleEngine.TargetRef> allyTargetRefs(Map<String, Object> state, BattleEngine.Action action) {
        List<BattleEngine.TargetRef> targets = new ArrayList<>();
        for (BattleEngine.TargetRef target : activeTargetRefs(state, "player".equals(action.side()))) {
            if (target.teamIndex() != action.actorIndex()) {
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            return List.of(new BattleEngine.TargetRef(action.side(), "player".equals(action.side()),
                    action.actorIndex(), action.actorFieldSlot()));
        }
        return List.of(targets.get(0));
    }

    private List<BattleEngine.TargetRef> allyAssistTargetRefs(Map<String, Object> state, BattleEngine.Action action) {
        List<BattleEngine.TargetRef> targets = new ArrayList<>();
        for (BattleEngine.TargetRef target : activeTargetRefs(state, "player".equals(action.side()))) {
            if (target.teamIndex() != action.actorIndex()) {
                targets.add(target);
            }
        }
        return targets;
    }

    private int targetId(Map<String, Object> move) {
        return engine.toInt(move.get("target_id"), 10);
    }

    private boolean isRagePowder(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return "rage-powder".equalsIgnoreCase(nameEn) || "rage powder".equalsIgnoreCase(nameEn);
    }
}
