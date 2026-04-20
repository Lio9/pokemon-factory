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

    void applyTaunt(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                    List<String> events) {
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
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        if (moveTypeId <= 0 || isStatusMove(move)) {
            return false;
        }
        String ability = engine.abilityName(target);
        if ("levitate".equalsIgnoreCase(ability) && moveTypeId == DamageCalculatorUtil.TYPE_GROUND) {
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "levitate");
            events.add(target.get("name") + " 的漂浮让地面属性招式失效了");
            return true;
        }
        if (("flash-fire".equalsIgnoreCase(ability) || "flash fire".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_FIRE) {
            target.put("flashFireBoost", true);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "flash-fire");
            events.add(target.get("name") + " 的引火吸收了火属性招式");
            return true;
        }
        if (("lightning-rod".equalsIgnoreCase(ability) || "lightning rod".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int previousStage = damageSupport.statStage(target, "specialAttack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("specialAttack", nextStage);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "lightning-rod");
            events.add(target.get("name") + " 的避雷针吸收了电属性招式，特攻提升了");
            return true;
        }
        if (("storm-drain".equalsIgnoreCase(ability) || "storm drain".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            int previousStage = damageSupport.statStage(target, "specialAttack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("specialAttack", nextStage);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "storm-drain");
            events.add(target.get("name") + " 的引水吸收了水属性招式，特攻提升了");
            return true;
        }
        if (("water-absorb".equalsIgnoreCase(ability) || "water absorb".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_WATER) {
            int heal = healFraction(target, 4);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "water-absorb");
            actionLog.put("heal", heal);
            events.add(target.get("name") + " 的储水吸收了水属性招式，回复了 " + heal + " 点 HP");
            return true;
        }
        if (("volt-absorb".equalsIgnoreCase(ability) || "volt absorb".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int heal = healFraction(target, 4);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "volt-absorb");
            actionLog.put("heal", heal);
            events.add(target.get("name") + " 的蓄电吸收了电属性招式，回复了 " + heal + " 点 HP");
            return true;
        }
        if (("motor-drive".equalsIgnoreCase(ability) || "motor drive".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            int previousStage = damageSupport.statStage(target, "speed");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("speed", nextStage);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "motor-drive");
            events.add(target.get("name") + " 的电气引擎吸收了电属性招式，速度提升了");
            return true;
        }
        if (("sap-sipper".equalsIgnoreCase(ability) || "sap sipper".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_GRASS) {
            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("attack", nextStage);
            actionLog.put("result", "ability-immune");
            actionLog.put("damage", 0);
            actionLog.put("ability", "sap-sipper");
            events.add(target.get("name") + " 的食草吸收了草属性招式，攻击提升了");
            return true;
        }
        return false;
    }

    boolean isStatusMoveBlockedByAbility(Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
                                         Map<String, Object> actionLog, List<String> events) {
        if (!isStatusMove(move) || actor == target || !hasAbility(target, "good-as-gold", "good as gold")) {
            return false;
        }
        actionLog.put("result", "ability-immune");
        actionLog.put("damage", 0);
        actionLog.put("ability", "good-as-gold");
        events.add(target.get("name") + " 的黄金之躯挡住了 " + move.get("name"));
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
