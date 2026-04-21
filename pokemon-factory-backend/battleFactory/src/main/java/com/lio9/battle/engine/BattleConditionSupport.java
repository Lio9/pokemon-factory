package com.lio9.battle.engine;

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleConditionSupport {
    private final BattleEngine engine;
    private final BattleDamageSupport damageSupport;
    private final BattleFieldEffectSupport fieldEffectSupport;

    BattleConditionSupport(BattleEngine engine, BattleDamageSupport damageSupport,
                           BattleFieldEffectSupport fieldEffectSupport) {
        this.engine = engine;
        this.damageSupport = damageSupport;
        this.fieldEffectSupport = fieldEffectSupport;
    }

    void applyParalysis(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                        Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，无法陷入异常状态");
            return;
        }
        if (engine.targetHasType(target, DamageCalculatorUtil.TYPE_ELECTRIC)) {
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

    void applyBurn(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                   Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，无法陷入异常状态");
            return;
        }
        if (engine.targetHasType(target, DamageCalculatorUtil.TYPE_FIRE)) {
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

    void applySleep(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                    Map<String, Object> move, Map<String, Object> actionLog, List<String> events,
                    Random random, int currentRound) {
        if (electricTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到电气场地保护，无法陷入睡眠");
            return;
        }
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，无法陷入异常状态");
            return;
        }
        if (engine.isPowderImmune(target)) {
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

    void applyPoison(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                     Map<String, Object> move, Map<String, Object> actionLog, List<String> events,
                     boolean badlyPoisoned) {
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，无法陷入异常状态");
            return;
        }
        if (engine.targetHasType(target, DamageCalculatorUtil.TYPE_POISON)
                || engine.targetHasType(target, DamageCalculatorUtil.TYPE_STEEL)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫中毒");
            return;
        }
        if (engine.isPoisonPowder(move) && engine.isPowderImmune(target)) {
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
        target.put("condition", badlyPoisoned ? "toxic" : "poison");
        target.put("toxicCounter", badlyPoisoned ? 1 : 0);
        actionLog.put("result", badlyPoisoned ? "toxic" : "poison");
        actionLog.put("condition", badlyPoisoned ? "toxic" : "poison");
        events.add(source.get("name") + " 让 " + target.get("name") + (badlyPoisoned ? " 陷入了剧毒状态" : " 中毒了"));
    }

    void applyConfusion(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                        List<String> events, Random random) {
        if (Boolean.TRUE.equals(target.get("confused")) && engine.toInt(target.get("confusionTurns"), 0) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经陷入混乱");
            return;
        }
        if (hasAbility(target, "own-tempo", "own tempo")) {
            actionLog.put("result", "status-immune");
            actionLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的特性让其不会混乱");
            return;
        }
        int turns = random.nextInt(4) + 2;
        target.put("confused", true);
        target.put("confusionTurns", turns);
        actionLog.put("result", "confusion");
        actionLog.put("confusionTurns", turns);
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了混乱");
    }

    void applyFreeze(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                     Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，无法陷入异常状态");
            return;
        }
        if (engine.targetHasType(target, DamageCalculatorUtil.TYPE_ICE)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 免疫冰冻");
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
        target.put("condition", "freeze");
        actionLog.put("result", "freeze");
        actionLog.put("condition", "freeze");
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了冰冻");
    }

    void thawFromFireHit(Map<String, Object> target, Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (!"freeze".equals(target.get("condition"))) {
            return;
        }
        if (engine.toInt(move.get("type_id"), 0) != DamageCalculatorUtil.TYPE_FIRE || engine.toInt(move.get("power"), 0) <= 0) {
            return;
        }
        target.put("condition", null);
        actionLog.put("thawed", true);
        events.add(target.get("name") + " 因火属性攻击解冻了");
    }

    void applyDamagingSecondaryEffects(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                       Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                       Random random) {
        if (engine.toInt(target.get("currentHp"), 0) <= 0 || engine.toInt(move.get("power"), 0) <= 0) {
            return;
        }

        String nameEn = String.valueOf(move.get("name_en"));
        if (matches(nameEn, "ice-beam", "ice beam", "blizzard", "freeze-dry", "freeze dry")) {
            tryApplyFreeze(state, actor, target, move, targetLog, events, random, secondaryChance(move, 10));
            return;
        }
        if (matches(nameEn, "thunderbolt", "discharge")) {
            tryApplyParalysis(state, actor, target, move, targetLog, events, random, secondaryChance(move,
                    "discharge".equalsIgnoreCase(nameEn) ? 30 : 10));
            return;
        }
        if (matches(nameEn, "flamethrower", "fire-blast", "fire blast", "heat-wave", "heat wave", "lava-plume", "lava plume", "scald")) {
            int defaultChance = matches(nameEn, "lava-plume", "lava plume", "scald") ? 30 : 10;
            tryApplyBurn(state, actor, target, move, targetLog, events, random, secondaryChance(move, defaultChance));
            return;
        }
        if (matches(nameEn, "sludge-bomb", "sludge bomb", "poison-jab", "poison jab")) {
            tryApplyPoison(state, actor, target, move, targetLog, events, random, secondaryChance(move, 30));
            return;
        }
        if (matches(nameEn, "tri-attack", "tri attack")) {
            tryApplyTriAttackStatus(state, actor, target, move, targetLog, events, random, secondaryChance(move, 20));
            return;
        }
        applyMoveMetaAilment(state, actor, target, move, targetLog, events, random);
        applyMoveMetaFlinch(target, move, targetLog, events, random);
        applyMoveMetaStatDrops(actor, target, move, targetLog, events, random);
    }

    void applyDrainHealing(Map<String, Object> actor, Map<String, Object> move, int actualDamage,
                           Map<String, Object> actionLog, List<String> events) {
        int drain = engine.toInt(move.get("drain"), 0);
        if (drain <= 0 || actualDamage <= 0 || engine.toInt(actor.get("currentHp"), 0) <= 0) {
            return;
        }
        int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), Math.max(1, engine.toInt(actor.get("currentHp"), 0)));
        int currentHp = engine.toInt(actor.get("currentHp"), 0);
        if (currentHp >= maxHp) {
            return;
        }
        int heal = Math.max(1, (actualDamage * drain) / 100);
        int actualHeal = Math.min(heal, maxHp - currentHp);
        actor.put("currentHp", currentHp + actualHeal);
        actionLog.put("drainHeal", actualHeal);
        events.add(actor.get("name") + " 吸取了 " + actualHeal + " 点 HP");
    }

    boolean applyMoveHealing(Map<String, Object> actor, Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        int healing = engine.toInt(move.get("healing"), 0);
        if (healing <= 0) {
            return false;
        }
        int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"), Math.max(1, engine.toInt(actor.get("currentHp"), 0)));
        int currentHp = engine.toInt(actor.get("currentHp"), 0);
        if (currentHp >= maxHp) {
            actionLog.put("result", "failed");
            events.add(actor.get("name") + " 的 HP 已经满了");
            return true;
        }
        int heal = Math.max(1, (maxHp * healing) / 100);
        int actualHeal = Math.min(heal, maxHp - currentHp);
        actor.put("currentHp", currentHp + actualHeal);
        actionLog.put("result", "heal");
        actionLog.put("heal", actualHeal);
        events.add(actor.get("name") + " 回复了 " + actualHeal + " 点 HP");
        return true;
    }

    boolean handleFrozenBeforeAction(Map<String, Object> actor, String side, List<Map<String, Object>> actionLogs,
                                     List<String> events, Random random) {
        if (!"freeze".equals(actor.get("condition"))) {
            return false;
        }
        if (random.nextInt(5) == 0) {
            actor.put("condition", null);
            events.add(actor.get("name") + " 解冻了");
            return false;
        }
        Map<String, Object> freezeLog = new java.util.LinkedHashMap<>();
        freezeLog.put("side", side);
        freezeLog.put("actor", actor.get("name"));
        freezeLog.put("actionType", "freeze");
        freezeLog.put("result", "frozen");
        actionLogs.add(freezeLog);
        events.add(actor.get("name") + " 因冰冻而无法行动");
        return true;
    }

    boolean handleConfusionBeforeAction(Map<String, Object> actor, String side, List<Map<String, Object>> actionLogs,
                                        List<String> events, Random random) {
        if (!Boolean.TRUE.equals(actor.get("confused"))) {
            return false;
        }
        int remaining = engine.toInt(actor.get("confusionTurns"), 0);
        if (remaining <= 0) {
            actor.put("confused", false);
            actor.put("confusionTurns", 0);
            return false;
        }
        remaining -= 1;
        actor.put("confusionTurns", remaining);
        if (remaining == 0) {
            actor.put("confused", false);
            events.add(actor.get("name") + " 不再混乱了");
            return false;
        }
        if (random.nextInt(3) != 0) {
            events.add(actor.get("name") + " 处于混乱中");
            return false;
        }
        int damage = confusionSelfDamage(actor);
        int remainingHp = Math.max(0, engine.toInt(actor.get("currentHp"), 0) - damage);
        actor.put("currentHp", remainingHp);
        if (remainingHp == 0) {
            actor.put("status", "fainted");
        }
        Map<String, Object> confusionLog = new java.util.LinkedHashMap<>();
        confusionLog.put("side", side);
        confusionLog.put("actor", actor.get("name"));
        confusionLog.put("actionType", "confusion");
        confusionLog.put("result", "confused-self-hit");
        confusionLog.put("damage", damage);
        confusionLog.put("targetHpAfter", remainingHp);
        actionLogs.add(confusionLog);
        events.add(actor.get("name") + " 因混乱攻击了自己，损失了 " + damage + " 点 HP");
        if (remainingHp == 0) {
            events.add(actor.get("name") + " 倒下了");
        }
        return true;
    }

    void applyTaunt(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                    List<String> events) {
        if (hasAbility(target, "oblivious")) {
            blockMoveByAbility(actionLog, events, "oblivious", target.get("name") + " 的迟钝让挑衅失效了");
            return;
        }
        if (engine.tauntTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于挑衅状态");
            return;
        }
        target.put("tauntTurns", 3);
        if ("mental-herb".equals(engine.heldItem(target)) && !engine.itemConsumed(target)) {
            target.put("tauntTurns", 0);
            engine.consumeItem(target);
            actionLog.put("mentalHerb", true);
            events.add(target.get("name") + " 的心灵香草解除了挑衅");
            return;
        }
        actionLog.put("result", "taunt");
        actionLog.put("tauntTurns", 3);
        events.add(source.get("name") + " 挑衅了 " + target.get("name"));
    }

    boolean applyDefenderAbilityImmunity(Map<String, Object> target, Map<String, Object> move,
                                         Map<String, Object> actionLog, List<String> events) {
        if (applySharedMoveBlockers(target, move, actionLog, events)) {
            return true;
        }
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        if (moveTypeId <= 0) {
            return false;
        }
        String ability = engine.abilityName(target);
        if (isStatusMove(move)) {
            return false;
        }
        if ("levitate".equalsIgnoreCase(ability) && moveTypeId == DamageCalculatorUtil.TYPE_GROUND) {
            blockMoveByAbility(actionLog, events, "levitate", target.get("name") + " 的漂浮让地面属性招式失效了");
            return true;
        }
        if (("flash-fire".equalsIgnoreCase(ability) || "flash fire".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            target.put("flashFireBoost", true);
            blockMoveByAbility(actionLog, events, "flash-fire", target.get("name") + " 的引火吸收了火属性招式");
            return true;
        }
        if (("lightning-rod".equalsIgnoreCase(ability) || "lightning rod".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int previousStage = damageSupport.statStage(target, "specialAttack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("specialAttack", nextStage);
            blockMoveByAbility(actionLog, events, "lightning-rod", target.get("name") + " 的避雷针吸收了电属性招式，特攻提升了");
            return true;
        }
        if (("storm-drain".equalsIgnoreCase(ability) || "storm drain".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            int previousStage = damageSupport.statStage(target, "specialAttack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("specialAttack", nextStage);
            blockMoveByAbility(actionLog, events, "storm-drain", target.get("name") + " 的引水吸收了水属性招式，特攻提升了");
            return true;
        }
        if (("water-absorb".equalsIgnoreCase(ability) || "water absorb".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            int heal = healFraction(target, 4);
            blockMoveByAbility(actionLog, events, "water-absorb", target.get("name") + " 的储水吸收了水属性招式，回复了 " + heal + " 点 HP");
            actionLog.put("heal", heal);
            return true;
        }
        if (("volt-absorb".equalsIgnoreCase(ability) || "volt absorb".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int heal = healFraction(target, 4);
            blockMoveByAbility(actionLog, events, "volt-absorb", target.get("name") + " 的蓄电吸收了电属性招式，回复了 " + heal + " 点 HP");
            actionLog.put("heal", heal);
            return true;
        }
        if (("motor-drive".equalsIgnoreCase(ability) || "motor drive".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int previousStage = damageSupport.statStage(target, "speed");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("speed", nextStage);
            blockMoveByAbility(actionLog, events, "motor-drive", target.get("name") + " 的电气引擎吸收了电属性招式，速度提升了");
            return true;
        }
        if (("sap-sipper".equalsIgnoreCase(ability) || "sap sipper".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("attack", nextStage);
            blockMoveByAbility(actionLog, events, "sap-sipper", target.get("name") + " 的食草吸收了草属性招式，攻击提升了");
            return true;
        }
        return false;
    }

    boolean isStatusMoveBlockedByAbility(Map<String, Object> state, String actingSide,
                                         Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
                                         Map<String, Object> actionLog, List<String> events) {
        if (applySharedMoveBlockers(target, move, actionLog, events)) {
            return true;
        }
        if (isBlockedByAromaVeil(state, target, move, actionLog, events)) {
            return true;
        }
        if (isStatusMove(move) && actor != target && hasAbility(target, "good-as-gold", "good as gold")) {
            blockMoveByAbility(actionLog, events, "good-as-gold", target.get("name") + " 的黄金之躯挡住了 " + move.get("name"));
            return true;
        }
        return pranksterBlockedByDarkType(state, actingSide, actor, target, move, actionLog, events);
    }

    boolean shouldMagicBounceStatusMove(Map<String, Object> state, String actingSide,
                                        Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
                                        Map<String, Object> actionLog, List<String> events) {
        if (!isStatusMove(move)
                || actor == target
                || !hasAbility(target, "magic-bounce", "magic bounce")
                || !targetsSingleOpponent(move)
                || !isReflectableStatusMove(move)) {
            return false;
        }
        if (!attackerTargetsOpposingSide(state, actingSide, target)) {
            return false;
        }
        actionLog.put("magicBounce", true);
        actionLog.put("ability", "magic-bounce");
        actionLog.put("bouncedFrom", target.get("name"));
        events.add(target.get("name") + " 的魔法镜反弹了 " + move.get("name"));
        return true;
    }

    boolean blocksSecondaryEffects(Map<String, Object> target, String effectName, Map<String, Object> actionLog, List<String> events) {
        if ("covert-cloak".equals(engine.heldItem(target))) {
            actionLog.put("secondaryEffectBlocked", effectName);
            events.add(target.get("name") + " 的密探斗篷挡住了追加效果");
            return true;
        }
        if (hasAbility(target, "shield-dust", "shield dust")) {
            actionLog.put("secondaryEffectBlocked", effectName);
            actionLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的特性挡住了追加效果");
            return true;
        }
        return false;
    }

    void applyReactiveContactEffects(Map<String, Object> attacker, Map<String, Object> target,
                                     Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (!isContactMove(move)
                || !"rocky-helmet".equals(engine.heldItem(target))
                || engine.toInt(attacker.get("currentHp"), 0) <= 0) {
            return;
        }
        int maxHp = engine.toInt(engine.castMap(attacker.get("stats")).get("hp"), 1);
        int recoil = Math.max(1, maxHp / 6);
        int remainingHp = Math.max(0, engine.toInt(attacker.get("currentHp"), 0) - recoil);
        attacker.put("currentHp", remainingHp);
        if (remainingHp == 0) {
            attacker.put("status", "fainted");
        }
        actionLog.put("rockyHelmet", recoil);
        events.add(attacker.get("name") + " 因凸凸头盔损失了 " + recoil + " 点 HP");
    }

    void applySwitchOutEffects(Map<String, Object> mon, List<String> events) {
        if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
            return;
        }
        if (Boolean.TRUE.equals(mon.get("dynamaxed"))) {
            engine.endDynamax(mon, events);
        }
        mon.put("confused", false);
        mon.put("confusionTurns", 0);
        if ("toxic".equals(mon.get("condition"))) {
            mon.put("toxicCounter", 0);
        }
        if ("regenerator".equalsIgnoreCase(engine.abilityName(mon))) {
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int currentHp = engine.toInt(mon.get("currentHp"), 0);
            if (currentHp >= maxHp) {
                return;
            }
            int heal = Math.max(1, maxHp / 3);
            mon.put("currentHp", Math.min(maxHp, currentHp + heal));
            events.add(mon.get("name") + " 通过再生力回复了 " + heal + " 点 HP");
        }
    }

    void applyTailwindWindRiderBoosts(Map<String, Object> state, Map<String, Object> actionLog, List<String> events) {
        applyTailwindWindRiderBoostsOnSide(state, true, actionLog, events);
        applyTailwindWindRiderBoostsOnSide(state, false, actionLog, events);
    }

    void applyEntryAbilities(Map<String, Object> state, boolean player, List<Integer> previousSlots, List<String> events) {
        List<Integer> currentSlots = engine.activeSlots(state, player);
        List<Map<String, Object>> enteringTeam = engine.team(state, player);
        for (Integer slot : currentSlots) {
            if (previousSlots.contains(slot) || slot == null || slot < 0 || slot >= enteringTeam.size()) {
                continue;
            }
            Map<String, Object> source = enteringTeam.get(slot);
            String ability = engine.abilityName(source);
            if ("intimidate".equalsIgnoreCase(ability)) {
                applyIntimidate(state, player, source, events);
                continue;
            }
            if ("drizzle".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateWeather(state, "rain", source, null, events);
                continue;
            }
            if ("drought".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateWeather(state, "sun", source, null, events);
                continue;
            }
            if ("sand-stream".equalsIgnoreCase(ability) || "sand stream".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateWeather(state, "sand", source, null, events);
                continue;
            }
            if ("snow-warning".equalsIgnoreCase(ability) || "snow warning".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateWeather(state, "snow", source, null, events);
                continue;
            }
            if ("electric-surge".equalsIgnoreCase(ability) || "electric surge".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateTerrain(state, "electric", source, null, events);
                continue;
            }
            if ("psychic-surge".equalsIgnoreCase(ability) || "psychic surge".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateTerrain(state, "psychic", source, null, events);
                continue;
            }
            if ("grassy-surge".equalsIgnoreCase(ability) || "grassy surge".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateTerrain(state, "grassy", source, null, events);
                continue;
            }
            if ("misty-surge".equalsIgnoreCase(ability) || "misty surge".equalsIgnoreCase(ability)) {
                fieldEffectSupport.activateTerrain(state, "misty", source, null, events);
            }
        }
    }

    void applyIntimidate(Map<String, Object> state, boolean player, Map<String, Object> source, List<String> events) {
        List<Map<String, Object>> opposingTeam = engine.team(state, !player);
        boolean activated = false;
        for (Integer targetSlot : engine.activeSlots(state, !player)) {
            if (targetSlot == null || targetSlot < 0 || targetSlot >= opposingTeam.size()) {
                continue;
            }
            Map<String, Object> target = opposingTeam.get(targetSlot);
            if (!engine.isAvailableMon(opposingTeam, targetSlot)) {
                continue;
            }
            if ("clear-amulet".equals(engine.heldItem(target))) {
                events.add(target.get("name") + " 的清净护符挡住了威吓");
                continue;
            }
            if (hasAbility(target, "clear-body", "clear body", "white-smoke", "white smoke", "full-metal-body", "full metal body")) {
                events.add(target.get("name") + " 的特性挡住了威吓");
                continue;
            }
            if (hasAbility(target, "inner-focus", "inner focus", "scrappy", "own-tempo", "own tempo", "oblivious")) {
                events.add(target.get("name") + " 的特性不受威吓影响");
                continue;
            }
            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.max(-6, previousStage - 1);
            damageSupport.statStages(target).put("attack", nextStage);
            if (nextStage != previousStage) {
                triggerStatDropAbilities(target, events);
                events.add(source.get("name") + " 的威吓使 " + target.get("name") + " 的攻击下降了");
                activated = true;
            }
        }
        if (activated) {
            source.put("intimidateActivated", true);
        }
    }

    void applySpeedDrop(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                        List<String> events) {
        if (isStatDropBlocked(target, actionLog, events, "speedDropBlocked", "降速")) {
            return;
        }
        int previousStage = damageSupport.statStage(target, "speed");
        int nextStage = Math.max(-6, previousStage - 1);
        damageSupport.statStages(target).put("speed", nextStage);
        if (nextStage != previousStage) {
            actionLog.put("speedStageChange", -1);
            triggerStatDropAbilities(target, events);
            events.add(source.get("name") + " 使 " + target.get("name") + " 的速度下降了");
        }
    }

    boolean applySpecialAttackDrop(Map<String, Object> source, Map<String, Object> target, int stages,
                                   Map<String, Object> actionLog, List<String> events) {
        if (isStatDropBlocked(target, actionLog, events, "specialAttackDropBlocked", "特攻下降")) {
            return false;
        }
        int previousStage = damageSupport.statStage(target, "specialAttack");
        int nextStage = Math.max(-6, previousStage - Math.max(1, stages));
        damageSupport.statStages(target).put("specialAttack", nextStage);
        if (nextStage != previousStage) {
            actionLog.put("specialAttackStageChange", nextStage - previousStage);
            triggerStatDropAbilities(target, events);
            events.add(source.get("name") + " 使 " + target.get("name") + " 的特攻下降了");
            return true;
        }
        return false;
    }

    boolean applySpecialDefenseDrop(Map<String, Object> source, Map<String, Object> target, int stages,
                                    Map<String, Object> actionLog, List<String> events) {
        if (isStatDropBlocked(target, actionLog, events, "specialDefenseDropBlocked", "特防下降")) {
            return false;
        }
        int previousStage = damageSupport.statStage(target, "specialDefense");
        int nextStage = Math.max(-6, previousStage - Math.max(1, stages));
        damageSupport.statStages(target).put("specialDefense", nextStage);
        if (nextStage != previousStage) {
            actionLog.put("specialDefenseStageChange", nextStage - previousStage);
            triggerStatDropAbilities(target, events);
            events.add(source.get("name") + " 使 " + target.get("name") + " 的特防大幅下降了");
            return true;
        }
        return false;
    }

    boolean applyAttackAndSpecialAttackDrop(Map<String, Object> source, Map<String, Object> target,
                                            Map<String, Object> actionLog, List<String> events) {
        if (isStatDropBlocked(target, actionLog, events, "partingShotBlocked", "攻击与特攻下降")) {
            return false;
        }
        boolean attackDropped = false;
        int previousAttack = damageSupport.statStage(target, "attack");
        int nextAttack = Math.max(-6, previousAttack - 1);
        damageSupport.statStages(target).put("attack", nextAttack);
        if (nextAttack != previousAttack) {
            attackDropped = true;
            actionLog.put("attackStageChange", nextAttack - previousAttack);
        }
        boolean specialAttackDropped = false;
        int previousSpecialAttack = damageSupport.statStage(target, "specialAttack");
        int nextSpecialAttack = Math.max(-6, previousSpecialAttack - 1);
        damageSupport.statStages(target).put("specialAttack", nextSpecialAttack);
        if (nextSpecialAttack != previousSpecialAttack) {
            specialAttackDropped = true;
            actionLog.put("specialAttackStageChange", nextSpecialAttack - previousSpecialAttack);
        }
        if (attackDropped || specialAttackDropped) {
            triggerStatDropAbilities(target, events);
            events.add(source.get("name") + " 使 " + target.get("name") + " 的攻击和特攻下降了");
            return true;
        }
        return false;
    }

    void triggerStatDropAbilities(Map<String, Object> target, List<String> events) {
        String ability = engine.abilityName(target);
        if ("defiant".equalsIgnoreCase(ability)) {
            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.min(6, previousStage + 2);
            damageSupport.statStages(target).put("attack", nextStage);
            if (nextStage != previousStage) {
                events.add(target.get("name") + " 因不服输触发，攻击大幅提升了");
            }
            return;
        }
        if ("competitive".equalsIgnoreCase(ability)) {
            int previousStage = damageSupport.statStage(target, "specialAttack");
            int nextStage = Math.min(6, previousStage + 2);
            damageSupport.statStages(target).put("specialAttack", nextStage);
            if (nextStage != previousStage) {
                events.add(target.get("name") + " 因好胜触发，特攻大幅提升了");
            }
        }
    }

    void resetBattleStages(Map<String, Object> mon) {
        damageSupport.statStages(mon).put("attack", 0);
        damageSupport.statStages(mon).put("defense", 0);
        damageSupport.statStages(mon).put("specialAttack", 0);
        damageSupport.statStages(mon).put("specialDefense", 0);
        damageSupport.statStages(mon).put("speed", 0);
        mon.put("tauntTurns", 0);
        mon.put("protectionStreak", 0);
        mon.put("lastProtectionRound", 0);
    }

    private int confusionSelfDamage(Map<String, Object> mon) {
        Map<String, Object> stats = engine.castMap(mon.get("stats"));
        int attack = damageSupport.modifiedAttackStat(mon, engine.toInt(stats.get("attack"), 100),
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, false);
        int defense = Math.max(1, damageSupport.modifiedDefenseStat(mon, engine.toInt(stats.get("defense"), 100),
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, new java.util.LinkedHashMap<>(), false));
        return Math.max(1, DamageCalculatorUtil.calculateBaseDamage(50, 40, attack, defense));
    }

    private void applyMoveMetaFlinch(Map<String, Object> target, Map<String, Object> move, Map<String, Object> targetLog,
                                     List<String> events, Random random) {
        if (targetLog.containsKey("flinch") || targetLog.containsKey("flinchBlocked")) {
            return;
        }
        int chance = engine.toInt(move.get("flinch_chance"), 0);
        if (!rollSecondaryChance(random, chance)) {
            return;
        }
        if ("inner-focus".equalsIgnoreCase(engine.abilityName(target)) || "inner focus".equalsIgnoreCase(engine.abilityName(target))) {
            targetLog.put("flinchBlocked", true);
            targetLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的特性让其不会畏缩");
            return;
        }
        if (blocksSecondaryEffects(target, "flinch", targetLog, events)) {
            return;
        }
        target.put("flinched", true);
        targetLog.put("flinch", true);
        events.add(target.get("name") + " 畏缩了");
    }

    private void applyMoveMetaAilment(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
                                      Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                      Random random) {
        String ailment = String.valueOf(move.getOrDefault("ailment_name_en", "")).trim();
        if (ailment.isBlank()) {
            return;
        }
        int chance = engine.toInt(move.get("ailment_chance"), 0);
        if (chance <= 0) {
            chance = engine.toInt(move.get("effect_chance"), 0);
        }
        if (!rollSecondaryChance(random, chance)) {
            return;
        }
        if (matches(ailment, "burn")) {
            if (!blocksSecondaryEffects(target, "burn", targetLog, events)) {
                applyBurn(state, source, target, move, targetLog, events);
            }
            return;
        }
        if (matches(ailment, "paralysis")) {
            if (!blocksSecondaryEffects(target, "paralysis", targetLog, events)) {
                applyParalysis(state, source, target, move, targetLog, events);
            }
            return;
        }
        if (matches(ailment, "freeze")) {
            if (!blocksSecondaryEffects(target, "freeze", targetLog, events)) {
                applyFreeze(state, source, target, move, targetLog, events);
            }
            return;
        }
        if (matches(ailment, "poison")) {
            if (!blocksSecondaryEffects(target, "poison", targetLog, events)) {
                applyPoison(state, source, target, move, targetLog, events, false);
            }
            return;
        }
        if (matches(ailment, "bad-poison")) {
            if (!blocksSecondaryEffects(target, "toxic", targetLog, events)) {
                applyPoison(state, source, target, move, targetLog, events, true);
            }
            return;
        }
        if (matches(ailment, "sleep")) {
            if (!blocksSecondaryEffects(target, "sleep", targetLog, events)) {
                applySleep(state, source, target, move, targetLog, events, random, engine.toInt(state.get("round"), 1));
            }
            return;
        }
        if (matches(ailment, "confusion")) {
            if (!blocksSecondaryEffects(target, "confusion", targetLog, events)) {
                applyConfusion(source, target, targetLog, events, random);
            }
        }
    }

    private void applyMoveMetaStatDrops(Map<String, Object> source, Map<String, Object> target, Map<String, Object> move,
                                        Map<String, Object> targetLog, List<String> events, Random random) {
        List<Map<String, Object>> statChanges = engine.castList(move.get("metaStatChanges"));
        if (statChanges.isEmpty()) {
            return;
        }
        int chance = engine.toInt(move.get("stat_chance"), 0);
        if (chance <= 0) {
            chance = engine.toInt(move.get("effect_chance"), 0);
        }
        if (!rollSecondaryChance(random, chance)) {
            return;
        }

        boolean needsSecondaryBlock = statChanges.stream().anyMatch(change -> engine.toInt(change.get("change"), 0) < 0);
        if (needsSecondaryBlock && blocksSecondaryEffects(target, "stat-drop", targetLog, events)) {
            return;
        }

        for (Map<String, Object> statChange : statChanges) {
            int delta = engine.toInt(statChange.get("change"), 0);
            int statId = engine.toInt(statChange.get("stat_id"), 0);
            if (delta >= 0 || hasStageChangeLog(targetLog, statId)) {
                continue;
            }
            applyStageChange(source, target, statId, delta, targetLog, events);
        }
    }

    private void tryApplyFreeze(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                Random random, int chance) {
        if (!rollSecondaryChance(random, chance) || blocksSecondaryEffects(target, "freeze", targetLog, events)) {
            return;
        }
        applyFreeze(state, actor, target, move, targetLog, events);
    }

    private void tryApplyParalysis(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                   Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                   Random random, int chance) {
        if (!rollSecondaryChance(random, chance) || blocksSecondaryEffects(target, "paralysis", targetLog, events)) {
            return;
        }
        applyParalysis(state, actor, target, move, targetLog, events);
    }

    private void tryApplyBurn(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                              Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                              Random random, int chance) {
        if (!rollSecondaryChance(random, chance) || blocksSecondaryEffects(target, "burn", targetLog, events)) {
            return;
        }
        applyBurn(state, actor, target, move, targetLog, events);
    }

    private void tryApplyPoison(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                Random random, int chance) {
        if (!rollSecondaryChance(random, chance) || blocksSecondaryEffects(target, "poison", targetLog, events)) {
            return;
        }
        applyPoison(state, actor, target, move, targetLog, events, false);
    }

    private void tryApplyTriAttackStatus(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                                         Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
                                         Random random, int chance) {
        if (!rollSecondaryChance(random, chance) || blocksSecondaryEffects(target, "tri-attack-status", targetLog, events)) {
            return;
        }
        int roll = random.nextInt(3);
        if (roll == 0) {
            applyBurn(state, actor, target, move, targetLog, events);
        } else if (roll == 1) {
            applyParalysis(state, actor, target, move, targetLog, events);
        } else {
            applyFreeze(state, actor, target, move, targetLog, events);
        }
    }

    private int secondaryChance(Map<String, Object> move, int defaultChance) {
        int chance = engine.toInt(move.get("effect_chance"), 0);
        return chance > 0 ? chance : defaultChance;
    }

    private boolean rollSecondaryChance(Random random, int chance) {
        if (chance >= 100) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return random.nextInt(100) < chance;
    }

    private boolean matches(String value, String... names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStageChangeLog(Map<String, Object> actionLog, int statId) {
        String key = stageChangeLogKey(statId);
        return !key.isBlank() && actionLog.containsKey(key);
    }

    private void applyStageChange(Map<String, Object> source, Map<String, Object> target, int statId, int delta,
                                  Map<String, Object> actionLog, List<String> events) {
        String statKey = statFieldKey(statId);
        if (statKey.isBlank() || delta == 0) {
            return;
        }
        if (delta < 0 && isStatDropBlocked(target, actionLog, events, statKey + "DropBlocked", statDisplayName(statId) + "下降")) {
            return;
        }
        int previousStage = damageSupport.statStage(target, statKey);
        int nextStage = Math.max(-6, Math.min(6, previousStage + delta));
        if (nextStage == previousStage) {
            return;
        }
        damageSupport.statStages(target).put(statKey, nextStage);
        String logKey = stageChangeLogKey(statId);
        if (!logKey.isBlank()) {
            actionLog.put(logKey, nextStage - previousStage);
        }
        if (delta < 0) {
            triggerStatDropAbilities(target, events);
        }
        events.add(source.get("name") + " 使 " + target.get("name") + " 的" + statDisplayName(statId)
                + (Math.abs(delta) >= 2 ? "大幅" : "") + (delta > 0 ? "上升了" : "下降了"));
    }

    private String statFieldKey(int statId) {
        return switch (statId) {
            case 2 -> "attack";
            case 3 -> "defense";
            case 4 -> "specialAttack";
            case 5 -> "specialDefense";
            case 6 -> "speed";
            default -> "";
        };
    }

    private String statDisplayName(int statId) {
        return switch (statId) {
            case 2 -> "攻击";
            case 3 -> "防御";
            case 4 -> "特攻";
            case 5 -> "特防";
            case 6 -> "速度";
            default -> "能力";
        };
    }

    private String stageChangeLogKey(int statId) {
        return switch (statId) {
            case 2 -> "attackStageChange";
            case 3 -> "defenseStageChange";
            case 4 -> "specialAttackStageChange";
            case 5 -> "specialDefenseStageChange";
            case 6 -> "speedStageChange";
            default -> "";
        };
    }

    boolean isBlockedByPsychicTerrain(Map<String, Object> state, String actingSide, Map<String, Object> target,
                                      Map<String, Object> move) {
        if (fieldEffectSupport.psychicTerrainTurns(state) <= 0
                || engine.toInt(move.get("priority"), 0) <= 0
                || !engine.isGrounded(target)) {
            return false;
        }
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        return ("player".equals(actingSide) && !targetOnPlayerSide)
                || ("opponent".equals(actingSide) && targetOnPlayerSide);
    }

    boolean isBlockedByPriorityBlockingAbility(Map<String, Object> state, String actingSide, Map<String, Object> target,
                                               Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (engine.toInt(move.get("priority"), 0) <= 0) {
            return false;
        }
        if (targetsFoeSideOnly(move)) {
            return false;
        }
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        boolean attackerTargetsOpposingSide = ("player".equals(actingSide) && !targetOnPlayerSide)
                || ("opponent".equals(actingSide) && targetOnPlayerSide);
        if (!attackerTargetsOpposingSide) {
            return false;
        }
        String blockingAbility = priorityBlockingAbility(state, targetOnPlayerSide);
        if (blockingAbility.isBlank()) {
            return false;
        }
        actionLog.put("result", "ability-priority-blocked");
        actionLog.put("damage", 0);
        actionLog.put("ability", blockingAbility);
        events.add(target.get("name") + " 一侧的特性挡住了先制招式");
        return true;
    }

    private boolean isStatusMove(Map<String, Object> move) {
        return engine.toInt(move.get("damage_class_id"), 0) == DamageCalculatorUtil.DAMAGE_CLASS_STATUS;
    }

    private boolean isStatDropBlocked(Map<String, Object> target, Map<String, Object> actionLog, List<String> events,
                                      String logKey, String effectName) {
        if ("clear-amulet".equals(engine.heldItem(target))) {
            actionLog.put(logKey, true);
            events.add(target.get("name") + " 的清净护符挡住了" + effectName);
            return true;
        }
        if (hasAbility(target, "clear-body", "clear body", "white-smoke", "white smoke", "full-metal-body", "full metal body")) {
            actionLog.put(logKey, true);
            actionLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的特性挡住了" + effectName);
            return true;
        }
        return false;
    }

    private boolean hasAbility(Map<String, Object> target, String... names) {
        String ability = engine.abilityName(target);
        for (String name : names) {
            if (name.equalsIgnoreCase(ability)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContactMove(Map<String, Object> move) {
        if (hasMoveFlag(move, "contact")) {
            return true;
        }
        Object contact = move.get("contact");
        if (contact instanceof Boolean bool) {
            return bool;
        }
        String nameEn = String.valueOf(move.get("name_en"));
        return "strike".equalsIgnoreCase(nameEn)
                || "tackle".equalsIgnoreCase(nameEn)
                || "fake-out".equalsIgnoreCase(nameEn)
                || "fake out".equalsIgnoreCase(nameEn)
                || "vine-whip".equalsIgnoreCase(nameEn)
                || "vine whip".equalsIgnoreCase(nameEn);
    }

    private boolean hasMoveFlag(Map<String, Object> move, String expected) {
        Object flags = move.get("flags");
        if (flags instanceof List<?> list) {
            for (Object flag : list) {
                if (expected.equalsIgnoreCase(String.valueOf(flag))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean applySharedMoveBlockers(Map<String, Object> target, Map<String, Object> move,
                                            Map<String, Object> actionLog, List<String> events) {
        if (hasMoveFlag(move, "powder") && engine.isPowderImmune(target)) {
            actionLog.put("result", "status-immune");
            actionLog.put("damage", 0);
            events.add(powderImmunityMessage(target, move));
            return true;
        }
        if (hasAbility(target, "wind-rider", "wind rider") && isWindMove(move)) {
            applyWindRiderBoost(target, actionLog, events, "乘风");
            blockMoveByAbility(actionLog, events, "wind-rider", target.get("name") + " 的乘风挡住了风类招式");
            return true;
        }
        if (hasAbility(target, "soundproof", "sound proof") && hasMoveFlag(move, "sound")) {
            blockMoveByAbility(actionLog, events, "soundproof", target.get("name") + " 的隔音挡住了声音招式");
            return true;
        }
        if (hasAbility(target, "bulletproof", "bullet proof") && hasMoveFlag(move, "bullet")) {
            blockMoveByAbility(actionLog, events, "bulletproof", target.get("name") + " 的防弹挡住了弹道招式");
            return true;
        }
        return false;
    }

    private void blockMoveByAbility(Map<String, Object> actionLog, List<String> events,
                                    String abilityName, String message) {
        actionLog.put("result", "ability-immune");
        actionLog.put("damage", 0);
        actionLog.put("ability", abilityName);
        events.add(message);
    }

    private boolean isBlockedByAromaVeil(Map<String, Object> state, Map<String, Object> target, Map<String, Object> move,
                                         Map<String, Object> actionLog, List<String> events) {
        if (!isStatusMove(move) || !isAromaVeilProtectedMove(move)) {
            return false;
        }
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        for (Integer activeSlot : engine.activeSlots(state, targetOnPlayerSide)) {
            List<Map<String, Object>> team = engine.team(state, targetOnPlayerSide);
            if (activeSlot == null || activeSlot < 0 || activeSlot >= team.size()) {
                continue;
            }
            Map<String, Object> ally = team.get(activeSlot);
            if (engine.toInt(ally.get("currentHp"), 0) <= 0 || !hasAbility(ally, "aroma-veil", "aroma veil")) {
                continue;
            }
            blockMoveByAbility(actionLog, events, "aroma-veil", target.get("name") + " 一侧的芳香幕挡住了 " + move.get("name"));
            return true;
        }
        return false;
    }

    private boolean isReflectableStatusMove(Map<String, Object> move) {
        if (hasMoveFlag(move, "reflectable")) {
            return true;
        }
        String nameEn = String.valueOf(move.get("name_en"));
        return matches(nameEn,
                "thunder-wave", "thunder wave",
                "will-o-wisp", "will o wisp",
                "toxic",
                "poison-powder", "poison powder",
                "spore",
                "taunt",
                "fake-tears", "fake tears",
                "confuse-ray", "confuse ray");
    }

    private boolean isAromaVeilProtectedMove(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en"));
        return matches(nameEn,
                "attract",
                "disable",
                "encore",
                "heal-block", "heal block",
                "taunt",
                "torment");
    }

    private boolean pranksterBlockedByDarkType(Map<String, Object> state, String actingSide,
                                               Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
                                               Map<String, Object> actionLog, List<String> events) {
        if (!Boolean.TRUE.equals(move.get("pranksterBoosted"))
                || !engine.targetHasType(target, DamageCalculatorUtil.TYPE_DARK)) {
            return false;
        }
        if (!attackerTargetsOpposingSide(state, actingSide, target)) {
            return false;
        }
        actionLog.put("result", "dark-prankster-immune");
        actionLog.put("damage", 0);
        events.add(target.get("name") + " 的恶属性免疫了恶作剧之心强化的变化招式");
        return true;
    }

    private boolean attackerTargetsOpposingSide(Map<String, Object> state, String actingSide, Map<String, Object> target) {
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        return ("player".equals(actingSide) && !targetOnPlayerSide)
                || ("opponent".equals(actingSide) && targetOnPlayerSide);
    }

    private String priorityBlockingAbility(Map<String, Object> state, boolean playerSide) {
        for (Integer activeSlot : engine.activeSlots(state, playerSide)) {
            List<Map<String, Object>> team = engine.team(state, playerSide);
            if (activeSlot == null || activeSlot < 0 || activeSlot >= team.size()) {
                continue;
            }
            Map<String, Object> mon = team.get(activeSlot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            if (hasAbility(mon, "armor-tail", "armor tail")) {
                return "armor-tail";
            }
            if (hasAbility(mon, "dazzling")) {
                return "dazzling";
            }
            if (hasAbility(mon, "queenly-majesty", "queenly majesty")) {
                return "queenly-majesty";
            }
        }
        return "";
    }

    private boolean targetsFoeSideOnly(Map<String, Object> move) {
        int targetId = engine.toInt(move.get("target_id"), 10);
        return targetId == 6;
    }

    private boolean targetsSingleOpponent(Map<String, Object> move) {
        return engine.toInt(move.get("target_id"), 10) == 10;
    }

    private boolean isWindMove(Map<String, Object> move) {
        if (hasMoveFlag(move, "wind")) {
            return true;
        }
        String nameEn = String.valueOf(move.get("name_en"));
        return matches(nameEn, "icy-wind", "icy wind", "heat-wave", "heat wave", "tailwind",
                "twister", "gust", "air-slash", "air slash", "hurricane", "bleakwind-storm", "bleakwind storm",
                "sandsear-storm", "sandsear storm", "springtide-storm", "springtide storm",
                "wildbolt-storm", "wildbolt storm", "fairy-wind", "fairy wind", "razor-wind", "razor wind");
    }

    private void applyTailwindWindRiderBoostsOnSide(Map<String, Object> state, boolean playerSide,
                                                    Map<String, Object> actionLog, List<String> events) {
        for (Integer activeSlot : engine.activeSlots(state, playerSide)) {
            List<Map<String, Object>> team = engine.team(state, playerSide);
            if (activeSlot == null || activeSlot < 0 || activeSlot >= team.size()) {
                continue;
            }
            Map<String, Object> mon = team.get(activeSlot);
            if (!hasAbility(mon, "wind-rider", "wind rider") || engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            applyWindRiderBoost(mon, actionLog, events, "顺风");
        }
    }

    private void applyWindRiderBoost(Map<String, Object> mon, Map<String, Object> actionLog, List<String> events, String trigger) {
        int previousStage = damageSupport.statStage(mon, "attack");
        int nextStage = Math.min(6, previousStage + 1);
        damageSupport.statStages(mon).put("attack", nextStage);
        if (nextStage != previousStage) {
            actionLog.put("windRiderBoost", true);
            events.add(mon.get("name") + " 因" + trigger + "触发乘风，攻击提升了");
        }
    }

    private int healFraction(Map<String, Object> mon, int denominator) {
        int currentHp = engine.toInt(mon.get("currentHp"), 0);
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), Math.max(1, currentHp));
        if (currentHp <= 0 || currentHp >= maxHp) {
            return 0;
        }
        int heal = Math.max(1, maxHp / denominator);
        int actualHeal = Math.min(heal, maxHp - currentHp);
        mon.put("currentHp", currentHp + actualHeal);
        return actualHeal;
    }

    private boolean electricTerrainActiveFor(Map<String, Object> mon, Map<String, Object> state) {
        return state != null && fieldEffectSupport.electricTerrainTurns(state) > 0 && engine.isGrounded(mon);
    }

    private boolean mistyTerrainActiveFor(Map<String, Object> mon, Map<String, Object> state) {
        return state != null && fieldEffectSupport.mistyTerrainTurns(state) > 0 && engine.isGrounded(mon);
    }

    private String powderImmunityMessage(Map<String, Object> mon, Map<String, Object> move) {
        if (engine.targetHasType(mon, DamageCalculatorUtil.TYPE_GRASS)) {
            return mon.get("name") + " 免疫 " + move.get("name");
        }
        if ("safety-goggles".equals(engine.heldItem(mon))) {
            return mon.get("name") + " 的防尘护目镜挡住了 " + move.get("name");
        }
        if ("overcoat".equalsIgnoreCase(engine.abilityName(mon))) {
            return mon.get("name") + " 的防尘使其不受 " + move.get("name") + " 影响";
        }
        return mon.get("name") + " 免疫 " + move.get("name");
    }

    private boolean isTypeImmune(Map<String, Object> move, Map<String, Object> target) {
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        if (moveTypeId <= 0) {
            return false;
        }
        for (Map<String, Object> type : engine.activeTypes(target)) {
            if (engine.typeFactor(moveTypeId, engine.toInt(type.get("type_id"), 0)) == 0) {
                return true;
            }
        }
        return false;
    }
}
