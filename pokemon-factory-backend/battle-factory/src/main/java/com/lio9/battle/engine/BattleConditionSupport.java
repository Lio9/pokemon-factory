package com.lio9.battle.engine;

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleConditionSupport {
    /**
     * 对战中的状态/特性/场地交互支持类。
     * <p>
     * 这里聚合了异常状态、volatile 状态、特性免疫、入场陷阱、接触反制等非纯伤害公式逻辑。
     * 本轮重点是把 taunt / healBlock / torment / disable / encore 逐步迁移到统一 volatile 访问器风格，
     * 同时保留旧字段镜像，确保现有调用点和历史测试数据仍然兼容。
     * </p>
     */
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
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
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
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
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
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
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
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
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
        if (engine.volatileFlag(target, "confused")
                && engine.toInt(engine.volatileValue(target, "confusionTurns", target.get("confusionTurns")), 0) > 0) {
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
        engine.setVolatile(target, "confused", true);
        engine.setVolatile(target, "confusionTurns", turns);
        actionLog.put("result", "confusion");
        actionLog.put("confusionTurns", turns);
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了混乱");
    }

    void applyFreeze(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
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

    void thawFromFireHit(Map<String, Object> target, Map<String, Object> move, Map<String, Object> actionLog,
            List<String> events) {
        if (!"freeze".equals(target.get("condition"))) {
            return;
        }
        if (engine.toInt(move.get("type_id"), 0) != DamageCalculatorUtil.TYPE_FIRE
                || engine.toInt(move.get("power"), 0) <= 0) {
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

        // Max Moves Secondary Effects (100% chance)
        if (Boolean.TRUE.equals(actor.get("dynamaxed"))) {
            String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
            boolean isPlayerSide = engine.isOnSide(state, actor, true);
            if (nameEn.startsWith("max-airstream")) {
                applyTeamStatBoost(state, isPlayerSide, "speed", 1, events);
            } else if (nameEn.startsWith("max-darkness")) {
                applyTeamStatDrop(state, !isPlayerSide, "specialDefense", 1, events);
            } else if (nameEn.startsWith("max-flare")) {
                fieldEffectSupport.activateWeather(state, "sun", actor, null, events);
            } else if (nameEn.startsWith("max-geyser")) {
                fieldEffectSupport.activateWeather(state, "rain", actor, null, events);
            } else if (nameEn.startsWith("max-hailstorm")) {
                fieldEffectSupport.activateWeather(state, "snow", actor, null, events);
            } else if (nameEn.startsWith("max-lightning")) {
                fieldEffectSupport.activateTerrain(state, "electric", actor, null, events);
            } else if (nameEn.startsWith("max-mindstorm")) {
                fieldEffectSupport.activateTerrain(state, "psychic", actor, null, events);
            } else if (nameEn.startsWith("max-overgrowth")) {
                fieldEffectSupport.activateTerrain(state, "grassy", actor, null, events);
            } else if (nameEn.startsWith("max-rockfall")) {
                fieldEffectSupport.activateWeather(state, "sand", actor, null, events);
            } else if (nameEn.startsWith("max-starfall")) {
                fieldEffectSupport.activateTerrain(state, "misty", actor, null, events);
            }
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
        if (matches(nameEn, "flamethrower", "fire-blast", "fire blast", "heat-wave", "heat wave", "lava-plume",
                "lava plume", "scald")) {
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

    void applyDamagingSelfStatChanges(Map<String, Object> actor, Map<String, Object> move,
            Map<String, Object> actionLog, List<String> events, Random random) {
        List<Map<String, Object>> statChanges = engine.castList(move.get("metaStatChanges"));
        if (statChanges.isEmpty() || !moveAppliesOwnStatChanges(move)) {
            return;
        }
        int chance = engine.toInt(move.get("stat_chance"), 0);
        if (chance <= 0) {
            chance = engine.toInt(move.get("effect_chance"), 0);
        }
        if (!rollSecondaryChance(random, chance)) {
            return;
        }
        Map<String, Integer> loweredStages = new java.util.LinkedHashMap<>();
        for (Map<String, Object> statChange : statChanges) {
            int delta = engine.toInt(statChange.get("change"), 0);
            int statId = engine.toInt(statChange.get("stat_id"), 0);
            if (delta == 0) {
                continue;
            }
            String statKey = statFieldKey(statId);
            if (statKey.isBlank()) {
                continue;
            }
            int previousStage = damageSupport.statStage(actor, statKey);
            int nextStage = Math.max(-6, Math.min(6, previousStage + delta));
            if (nextStage == previousStage) {
                continue;
            }
            damageSupport.statStages(actor).put(statKey, nextStage);
            String logKey = stageChangeLogKey(statId);
            if (!logKey.isBlank()) {
                actionLog.put(logKey, nextStage - previousStage);
            }
            if (delta < 0) {
                loweredStages.merge(statKey, previousStage - nextStage, (a, b) -> a + b);
            }
            events.add(actor.get("name") + " 的" + statDisplayName(statId)
                    + (Math.abs(delta) >= 2 ? "大幅" : "") + (delta > 0 ? "上升了" : "下降了"));
        }
        if (!loweredStages.isEmpty()) {
            restoreLoweredStatsWithWhiteHerb(actor, loweredStages, actionLog, events);
        }
    }

    void applyDrainHealing(Map<String, Object> actor, Map<String, Object> move, int actualDamage,
            Map<String, Object> actionLog, List<String> events) {
        int drain = engine.toInt(move.get("drain"), 0);
        if (drain <= 0 || actualDamage <= 0 || engine.toInt(actor.get("currentHp"), 0) <= 0) {
            return;
        }
        if (engine.healBlockTurns(actor) > 0) {
            actionLog.put("drainBlocked", true);
            events.add(actor.get("name") + " 受到回复封锁，无法通过吸取效果回复 HP");
            return;
        }
        int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"),
                Math.max(1, engine.toInt(actor.get("currentHp"), 0)));
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

    boolean applyMoveHealing(Map<String, Object> actor, Map<String, Object> move, Map<String, Object> actionLog,
            List<String> events) {
        int healing = engine.toInt(move.get("healing"), 0);
        if (healing <= 0) {
            return false;
        }
        if (engine.healBlockTurns(actor) > 0) {
            actionLog.put("result", "heal-blocked");
            events.add(actor.get("name") + " 受到回复封锁，无法使用回复招式");
            return true;
        }
        int maxHp = engine.toInt(engine.castMap(actor.get("stats")).get("hp"),
                Math.max(1, engine.toInt(actor.get("currentHp"), 0)));
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
        if (!engine.volatileFlag(actor, "confused")) {
            return false;
        }
        int remaining = engine.toInt(engine.volatileValue(actor, "confusionTurns", actor.get("confusionTurns")), 0);
        if (remaining <= 0) {
            engine.setVolatile(actor, "confused", false);
            engine.setVolatile(actor, "confusionTurns", 0);
            return false;
        }
        remaining -= 1;
        engine.setVolatile(actor, "confusionTurns", remaining);
        if (remaining == 0) {
            engine.setVolatile(actor, "confused", false);
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
        // 统一写入 volatile，并镜像回旧字段，保证历史逻辑兼容。
        engine.setVolatile(target, "tauntTurns", 3);
        actionLog.put("result", "taunt");
        actionLog.put("tauntTurns", 3);
        if ("mental-herb".equals(engine.heldItem(target)) && !engine.itemConsumed(target)) {
            engine.setVolatile(target, "tauntTurns", 0);
            actionLog.put("tauntTurns", 0);
            consumeMentalHerb(target, actionLog, events, "挑衅");
            return;
        }
        events.add(source.get("name") + " 挑衅了 " + target.get("name"));
    }

    void applyEncore(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
            List<String> events) {
        if (engine.encoreTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于再来一次状态");
            return;
        }
        Object lastMoveUsed = target.get("lastMoveUsed");
        if (lastMoveUsed == null || String.valueOf(lastMoveUsed).isBlank()) {
            actionLog.put("result", "failed");
            events.add(source.get("name") + " 的再来一次失败了");
            return;
        }
        // Encore 既要记录剩余回合，也要记录被强制重复的招式名，因此两个字段一起迁移。
        engine.setVolatile(target, "encoreTurns", 3);
        engine.setVolatile(target, "encoreMove", String.valueOf(lastMoveUsed));
        actionLog.put("result", "encore");
        actionLog.put("encoreTurns", 3);
        actionLog.put("encoreMove", lastMoveUsed);
        if (consumeMentalHerb(target, actionLog, events, "再来一次")) {
            engine.setVolatile(target, "encoreTurns", 0);
            engine.setVolatile(target, "encoreMove", null);
            actionLog.put("encoreTurns", 0);
            actionLog.put("encoreMove", null);
            return;
        }
        events.add(source.get("name") + " 让 " + target.get("name") + " 只能继续使用 "
                + String.valueOf(lastMoveUsed));
    }

    void applyDisable(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> actionLog, List<String> events) {
        Object lastMoveUsed = target.get("lastMoveUsed");
        if (lastMoveUsed == null || String.valueOf(lastMoveUsed).isBlank()) {
            actionLog.put("result", "failed");
            events.add(source.get("name") + " 的定身法失败了");
            return;
        }
        applyDisableEffect(state, target, String.valueOf(lastMoveUsed), actionLog, events,
                source.get("name") + " 的定身法失败了",
                source.get("name") + " 用定身法封住了 " + target.get("name") + " 的 " + String.valueOf(lastMoveUsed));
    }

    void applyTorment(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
            List<String> events) {
        if (engine.tormentTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于无理取闹状态");
            return;
        }
        // 无理取闹只需要持续回合，因此只同步一个回合计数字段。
        engine.setVolatile(target, "tormentTurns", 4);
        actionLog.put("result", "torment");
        actionLog.put("tormentTurns", 4);
        if (consumeMentalHerb(target, actionLog, events, "无理取闹")) {
            engine.setVolatile(target, "tormentTurns", 0);
            actionLog.put("tormentTurns", 0);
            return;
        }
        events.add(source.get("name") + " 让 " + target.get("name") + " 陷入了无理取闹");
    }

    void applyHealBlock(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
            List<String> events) {
        if (engine.healBlockTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于回复封锁状态");
            return;
        }
        // 回复封锁后续会被回复招式、剩饭、果实、场地回复等多个入口读取，因此统一接入访问器最稳妥。
        engine.setVolatile(target, "healBlockTurns", 4);
        actionLog.put("result", "heal-block");
        actionLog.put("healBlockTurns", 4);
        if (consumeMentalHerb(target, actionLog, events, "回复封锁")) {
            engine.setVolatile(target, "healBlockTurns", 0);
            actionLog.put("healBlockTurns", 0);
            return;
        }
        events.add(source.get("name") + " 封锁了 " + target.get("name") + " 的回复");
    }

    boolean knockOffGetsBoost(Map<String, Object> target) {
        return hasRemovableHeldItem(target) && !hasAbility(target, "sticky-hold", "sticky hold");
    }

    void applyLeechSeed(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                        List<String> events) {
        if (engine.targetHasType(target, DamageCalculatorUtil.TYPE_GRASS)) {
            actionLog.put("result", "failed");
            events.add(target.get("name") + " 是草属性，寄生种子无效");
            return;
        }
        engine.setVolatile(target, "leechSeed", true);
        actionLog.put("result", "leech-seed");
        events.add(source.get("name") + " 在 " + target.get("name") + " 身上种下了寄生种子");
    }

    void applySubstitute(Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
                         Map<String, Object> actionLog, List<String> events) {
        int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), 1);
        int currentHp = engine.toInt(target.get("currentHp"), 0);
        int cost = Math.max(1, maxHp / 4);
        if (currentHp <= cost) {
            actionLog.put("result", "failed");
            events.add(target.get("name") + " 的 HP 不足以制造替身");
            return;
        }
        target.put("currentHp", currentHp - cost);
        engine.setVolatile(target, "substitute", currentHp - cost);
        actionLog.put("substituteHp", currentHp - cost);
        actionLog.put("result", "substitute");
        events.add(target.get("name") + " 制造了一个替身");
    }

    void applyAttract(Map<String, Object> source, Map<String, Object> target, Map<String, Object> actionLog,
                      List<String> events) {
        if (hasAbility(target, "oblivious", "oblivious") || hasAbility(target, "aroma-veil", "aroma veil")) {
            actionLog.put("result", "failed");
            events.add(target.get("name") + " 的特性阻止了着迷");
            return;
        }
        engine.setVolatile(target, "infatuated", true);
        actionLog.put("result", "attract");
        events.add(source.get("name") + " 使 " + target.get("name") + " 陷入了着迷状态");
    }

    void applyPerishSong(Map<String, Object> state, Map<String, Object> actor, Map<String, Object> target,
                         Map<String, Object> actionLog, List<String> events) {
        engine.setVolatile(target, "perishSongTurns", 3);
        actionLog.put("result", "perish-song");
        events.add(target.get("name") + " 听到了灭亡之歌");
    }

    void applyKnockOff(Map<String, Object> target, Map<String, Object> actionLog, List<String> events) {
        if (engine.toInt(target.get("currentHp"), 0) <= 0 || !hasRemovableHeldItem(target)) {
            return;
        }
        if (hasAbility(target, "sticky-hold", "sticky hold")) {
            actionLog.put("knockOffBlocked", true);
            actionLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的黏着阻止了道具被拍落");
            return;
        }
        String removedItem = engine.heldItem(target);
        engine.removeHeldItem(target);
        actionLog.put("knockedOffItem", removedItem);
        events.add(target.get("name") + " 的道具 " + removedItem + " 被拍落了");
    }

    void applyYawn(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (blockedBySafeguard(state, source, target, move, actionLog, events)) {
            return;
        }
        if (electricTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到电气场地保护，没有变得困倦");
            return;
        }
        if (mistyTerrainActiveFor(target, state)) {
            actionLog.put("result", "status-immune");
            events.add(target.get("name") + " 受到薄雾场地保护，没有变得困倦");
            return;
        }
        if (engine.yawnTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经昏昏欲睡了");
            return;
        }
        if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于异常状态");
            return;
        }
        engine.setVolatile(target, "yawnTurns", 2);
        actionLog.put("result", "yawn");
        actionLog.put("yawnTurns", 2);
        events.add(source.get("name") + " 让 " + target.get("name") + " 昏昏欲睡");
    }

    void resolveYawn(Map<String, Object> state, Map<String, Object> target, List<String> events, Random random,
            int currentRound) {
        if (engine.toInt(target.get("currentHp"), 0) <= 0) {
            engine.setVolatile(target, "yawnTurns", 0);
            return;
        }
        if (target.get("condition") != null && !String.valueOf(target.get("condition")).isBlank()) {
            engine.setVolatile(target, "yawnTurns", 0);
            return;
        }
        if (electricTerrainActiveFor(target, state)) {
            events.add(target.get("name") + " 受到电气场地保护，没有睡着");
            return;
        }
        if (mistyTerrainActiveFor(target, state)) {
            events.add(target.get("name") + " 受到薄雾场地保护，没有睡着");
            return;
        }
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        if (fieldEffectSupport.safeguardTurns(state, targetOnPlayerSide) > 0) {
            events.add(target.get("name") + " 受到神秘守护保护，没有睡着");
            return;
        }
        int turns = random.nextInt(3) + 1;
        target.put("condition", "sleep");
        target.put("sleepTurns", turns);
        target.put("sleepAppliedRound", currentRound + 1);
        events.add(target.get("name") + " 因哈欠陷入了睡眠");
    }

    boolean applyDefenderAbilityImmunity(Map<String, Object> attacker, Map<String, Object> target,
            Map<String, Object> move,
            Map<String, Object> actionLog, List<String> events) {
        if (applySharedMoveBlockers(attacker, target, move, actionLog, events)) {
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
        if (ignoresTargetAbility(attacker)) {
            return false;
        }
        if (hasAbility(target, "wonder-guard", "wonder guard")
                && damageSupport.typeModifier(target, moveTypeId) <= 1.0d) {
            blockMoveByAbility(actionLog, events, "wonder-guard", target.get("name") + " 的神奇守护让攻击失效了");
            return true;
        }
        if ("levitate".equalsIgnoreCase(ability) && moveTypeId == DamageCalculatorUtil.TYPE_GROUND) {
            blockMoveByAbility(actionLog, events, "levitate", target.get("name") + " 的漂浮让地面属性招式失效了");
            return true;
        }
        if ("air-balloon".equals(engine.heldItem(target)) && moveTypeId == DamageCalculatorUtil.TYPE_GROUND) {
            blockMoveByAbility(actionLog, events, "air-balloon", target.get("name") + " 的气球让地面属性招式失效了");
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
            if (engine.healBlockTurns(target) > 0) {
                blockMoveByAbility(actionLog, events, "water-absorb", target.get("name") + " 的储水吸收了水属性招式，但回复封锁阻止了回血");
                actionLog.put("healBlocked", true);
                return true;
            }
            int heal = healFraction(target, 4);
            blockMoveByAbility(actionLog, events, "water-absorb",
                    target.get("name") + " 的储水吸收了水属性招式，回复了 " + heal + " 点 HP");
            actionLog.put("heal", heal);
            return true;
        }
        if (("volt-absorb".equalsIgnoreCase(ability) || "volt absorb".equalsIgnoreCase(ability))
                && moveTypeId == DamageCalculatorUtil.TYPE_ELECTRIC) {
            if (engine.healBlockTurns(target) > 0) {
                blockMoveByAbility(actionLog, events, "volt-absorb", target.get("name") + " 的蓄电吸收了电属性招式，但回复封锁阻止了回血");
                actionLog.put("healBlocked", true);
                return true;
            }
            int heal = healFraction(target, 4);
            blockMoveByAbility(actionLog, events, "volt-absorb",
                    target.get("name") + " 的蓄电吸收了电属性招式，回复了 " + heal + " 点 HP");
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

    private boolean ignoresTargetAbility(Map<String, Object> attacker) {
        return hasAbility(attacker, "mold-breaker", "mold breaker", "teravolt", "turboblaze");
    }

    boolean isStatusMoveBlockedByAbility(Map<String, Object> state, String actingSide,
            Map<String, Object> actor, Map<String, Object> target, Map<String, Object> move,
            Map<String, Object> actionLog, List<String> events) {
        if (applySharedMoveBlockers(actor, target, move, actionLog, events)) {
            return true;
        }
        if (isBlockedByAromaVeil(state, target, move, actionLog, events)) {
            return true;
        }
        if (isStatusMove(move) && actor != target && hasAbility(target, "good-as-gold", "good as gold")) {
            blockMoveByAbility(actionLog, events, "good-as-gold", target.get("name") + " 的黄金之躯挡住了 " + move.get("name"));
            return true;
        }
        if (isStatusMove(move) && actor != target && hasAbility(target, "purifying-salt", "purifying salt")) {
            blockMoveByAbility(actionLog, events, "purifying-salt", target.get("name") + " 的洁净之盐阻挡了变化招式");
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

    boolean blocksSecondaryEffects(Map<String, Object> target, String effectName, Map<String, Object> actionLog,
            List<String> events) {
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

    void applyReactiveContactEffects(Map<String, Object> state, Map<String, Object> attacker,
            Map<String, Object> target,
            Map<String, Object> move, Map<String, Object> actionLog, List<String> events,
            Random random) {
        if (engine.toInt(attacker.get("currentHp"), 0) <= 0) {
            return;
        }
        if (engine.toInt(move.get("power"), 0) > 0
                && hasAbility(target, "cursed-body", "cursed body")
                && rollSecondaryChance(random, 30)
                && applyDisableEffect(state, attacker, String.valueOf(move.get("name_en")), actionLog, events,
                        target.get("name") + " 的诅咒之躯发动失败了",
                        target.get("name") + " 的诅咒之躯封住了 " + attacker.get("name") + " 的 " + move.get("name"))) {
            actionLog.put("cursedBody", true);
        }
        if (!isContactMove(move)) {
            return;
        }

        int maxHp = engine.toInt(engine.castMap(attacker.get("stats")).get("hp"), 1);
        if (hasAbility(target, "rough-skin", "rough skin")) {
            applyContactPunishDamage(attacker, Math.max(1, maxHp / 8), actionLog, events,
                    "roughSkin", target.get("name") + " 的粗糙皮肤反伤了 ");
        }
        if (hasAbility(target, "iron-barbs", "iron barbs")) {
            applyContactPunishDamage(attacker, Math.max(1, maxHp / 8), actionLog, events,
                    "ironBarbs", target.get("name") + " 的铁刺反伤了 ");
        }
        if ("rocky-helmet".equals(engine.heldItem(target))) {
            applyContactPunishDamage(attacker, Math.max(1, maxHp / 6), actionLog, events,
                    "rockyHelmet", target.get("name") + " 的凸凸头盔反伤了 ");
        }
        if (hasAbility(target, "gooey") || hasAbility(target, "tangling-hair", "tangling hair")) {
            applySpeedDrop(target, attacker, actionLog, events);
        }
        if (engine.toInt(target.get("currentHp"), 0) <= 0 && hasAbility(target, "aftermath")) {
            applyContactPunishDamage(attacker, Math.max(1, maxHp / 4), actionLog, events,
                    "aftermath", target.get("name") + " 的引爆伤到了 ");
        }
        if (engine.toInt(attacker.get("currentHp"), 0) <= 0) {
            return;
        }
        applyReactiveContactStatusAbility(state, attacker, target, actionLog, events, random);
    }

    void applyReactiveDamageAbilities(Map<String, Object> attacker, Map<String, Object> target,
            Map<String, Object> move,
            int hpBeforeDamage, int hpAfterDamage, int actualDamage,
            Map<String, Object> actionLog, List<String> events) {
        if (actualDamage <= 0 || hpAfterDamage <= 0) {
            return;
        }
        int moveTypeId = engine.toInt(move.get("type_id"), 0);
        int damageClassId = engine.toInt(move.get("damage_class_id"), 0);
        if (hasAbility(target, "weak-armor", "weak armor")
                && damageClassId == DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL) {
            applyAbilityStageChange(target, 3, -1, actionLog, events, "碎裂铠甲");
            applyAbilityStageChange(target, 6, 2, actionLog, events, "碎裂铠甲");
        }
        if (hasAbility(target, "stamina")) {
            applyAbilityStageChange(target, 3, 1, actionLog, events, "持久力");
        }
        if (hasAbility(target, "justified") && moveTypeId == DamageCalculatorUtil.TYPE_DARK) {
            applyAbilityStageChange(target, 2, 1, actionLog, events, "正义之心");
        }
        if (hasAbility(target, "rattled") && (moveTypeId == DamageCalculatorUtil.TYPE_BUG
                || moveTypeId == DamageCalculatorUtil.TYPE_GHOST
                || moveTypeId == DamageCalculatorUtil.TYPE_DARK)) {
            applyAbilityStageChange(target, 6, 1, actionLog, events, "胆怯");
        }
        if (hasAbility(target, "steam-engine", "steam engine")
                && (moveTypeId == DamageCalculatorUtil.TYPE_FIRE || moveTypeId == DamageCalculatorUtil.TYPE_WATER)) {
            applyAbilityStageChange(target, 6, 6, actionLog, events, "蒸汽机");
        }
        int maxHp = engine.toInt(engine.castMap(target.get("stats")).get("hp"), Math.max(1, hpBeforeDamage));
        if (hasAbility(target, "berserk")
                && hpBeforeDamage * 2 > maxHp
                && hpAfterDamage * 2 <= maxHp) {
            applyAbilityStageChange(target, 4, 1, actionLog, events, "怒火中烧");
        }
    }

    private boolean applyDisableEffect(Map<String, Object> state, Map<String, Object> target, String disabledMoveName,
            Map<String, Object> actionLog, List<String> events, String failMessage,
            String successMessage) {
        if (engine.disableTurns(target) > 0) {
            actionLog.put("result", "status-failed");
            events.add(target.get("name") + " 已经处于定身法状态");
            return false;
        }
        if (disabledMoveName == null || disabledMoveName.isBlank()) {
            actionLog.put("result", "failed");
            events.add(failMessage);
            return false;
        }
        if (state != null && isBlockedByAromaVeil(state, target, disableEffectMove(), actionLog, events)) {
            return false;
        }
        // Disable 与 Encore 类似，需要同时记录持续时间和被锁定的招式名。
        engine.setVolatile(target, "disableTurns", 4);
        engine.setVolatile(target, "disableMove", disabledMoveName);
        actionLog.put("result", "disable");
        actionLog.put("disableTurns", 4);
        actionLog.put("disableMove", disabledMoveName);
        if (consumeMentalHerb(target, actionLog, events, "定身法")) {
            engine.setVolatile(target, "disableTurns", 0);
            engine.setVolatile(target, "disableMove", null);
            actionLog.put("disableTurns", 0);
            actionLog.put("disableMove", null);
            return true;
        }
        events.add(successMessage);
        return true;
    }

    private void applyContactPunishDamage(Map<String, Object> attacker, int damage, Map<String, Object> actionLog,
            List<String> events, String logKey, String messagePrefix) {
        if (engine.isMagicGuard(attacker)) {
            return;
        }
        int currentHp = engine.toInt(attacker.get("currentHp"), 0);
        if (currentHp <= 0 || damage <= 0) {
            return;
        }
        int actualDamage = Math.min(currentHp, damage);
        int remainingHp = currentHp - actualDamage;
        attacker.put("currentHp", remainingHp);
        if (remainingHp == 0) {
            attacker.put("status", "fainted");
        }
        actionLog.put(logKey, actualDamage);
        events.add(messagePrefix + attacker.get("name") + "，损失了 " + actualDamage + " 点 HP");
    }

    private void applyAbilityStageChange(Map<String, Object> target, int statId, int delta,
            Map<String, Object> actionLog, List<String> events, String trigger) {
        String statKey = statFieldKey(statId);
        if (statKey.isBlank() || delta == 0) {
            return;
        }
        if (delta < 0 && isStatDropBlocked(target, actionLog, events, statKey + "DropBlocked",
                statDisplayName(statId) + "下降")) {
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
            restoreLoweredStatsWithWhiteHerb(target, Map.of(statKey, previousStage - nextStage), actionLog, events);
        }
        events.add(target.get("name") + " 因" + trigger + "触发，" + statDisplayName(statId)
                + (Math.abs(nextStage - previousStage) >= 2 ? "大幅" : "")
                + ((nextStage - previousStage) > 0 ? "提升了" : "下降了"));
    }

    private void applyReactiveContactStatusAbility(Map<String, Object> state, Map<String, Object> attacker,
            Map<String, Object> target, Map<String, Object> actionLog,
            List<String> events, Random random) {
        if (hasAbility(target, "static")) {
            tryApplyContactParalysis(state, target, attacker, actionLog, events, random,
                    contactAbilityMove("Static", "static", DamageCalculatorUtil.TYPE_ELECTRIC), "static");
            return;
        }
        if (hasAbility(target, "flame-body", "flame body")) {
            tryApplyContactBurn(state, target, attacker, actionLog, events, random,
                    contactAbilityMove("Flame Body", "flame-body", DamageCalculatorUtil.TYPE_FIRE), "flameBody");
            return;
        }
        if (hasAbility(target, "poison-point", "poison point")) {
            tryApplyContactPoison(state, target, attacker, actionLog, events, random,
                    contactAbilityMove("Poison Point", "poison-point", DamageCalculatorUtil.TYPE_POISON),
                    "poisonPoint");
            return;
        }
        if (hasAbility(target, "effect-spore", "effect spore")) {
            tryApplyEffectSpore(state, target, attacker, actionLog, events, random,
                    contactAbilityMove("Effect Spore", "effect-spore", DamageCalculatorUtil.TYPE_GRASS));
        }
    }

    private void tryApplyContactParalysis(Map<String, Object> state, Map<String, Object> source,
            Map<String, Object> target,
            Map<String, Object> actionLog, List<String> events, Random random,
            Map<String, Object> move, String logKey) {
        if (!rollSecondaryChance(random, 30)) {
            return;
        }
        applyParalysis(state, source, target, move, actionLog, events);
        if ("paralysis".equals(target.get("condition"))) {
            actionLog.put(logKey, true);
        }
    }

    private void tryApplyContactBurn(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> actionLog, List<String> events, Random random,
            Map<String, Object> move, String logKey) {
        if (!rollSecondaryChance(random, 30)) {
            return;
        }
        applyBurn(state, source, target, move, actionLog, events);
        if ("burn".equals(target.get("condition"))) {
            actionLog.put(logKey, true);
        }
    }

    private void tryApplyContactPoison(Map<String, Object> state, Map<String, Object> source,
            Map<String, Object> target,
            Map<String, Object> actionLog, List<String> events, Random random,
            Map<String, Object> move, String logKey) {
        if (!rollSecondaryChance(random, 30)) {
            return;
        }
        applyPoison(state, source, target, move, actionLog, events, false);
        if ("poison".equals(target.get("condition"))) {
            actionLog.put(logKey, true);
        }
    }

    private void tryApplyEffectSpore(Map<String, Object> state, Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> actionLog, List<String> events, Random random,
            Map<String, Object> move) {
        if (!rollSecondaryChance(random, 30)) {
            return;
        }
        if (engine.isPowderImmune(target)) {
            actionLog.put("effectSporeBlocked", true);
            events.add(powderImmunityMessage(target, move));
            return;
        }
        int roll = random.nextInt(3);
        if (roll == 0) {
            applySleep(state, source, target, move, actionLog, events, random,
                    engine.toInt(state.get("currentRound"), 1));
            if ("sleep".equals(target.get("condition"))) {
                actionLog.put("effectSpore", "sleep");
            }
            return;
        }
        if (roll == 1) {
            applyParalysis(state, source, target, move, actionLog, events);
            if ("paralysis".equals(target.get("condition"))) {
                actionLog.put("effectSpore", "paralysis");
            }
            return;
        }
        applyPoison(state, source, target, move, actionLog, events, false);
        if ("poison".equals(target.get("condition"))) {
            actionLog.put("effectSpore", "poison");
        }
    }

    private Map<String, Object> contactAbilityMove(String name, String nameEn, int typeId) {
        return new java.util.LinkedHashMap<>(Map.of(
                "name", name,
                "name_en", nameEn,
                "type_id", typeId));
    }

    private Map<String, Object> disableEffectMove() {
        return new java.util.LinkedHashMap<>(Map.of(
                "name", "Disable",
                "name_en", "disable",
                "damage_class_id", DamageCalculatorUtil.DAMAGE_CLASS_STATUS));
    }

    void applySwitchOutEffects(Map<String, Object> mon, List<String> events) {
        if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
            return;
        }
        boolean healBlocked = engine.healBlockTurns(mon) > 0;

        // Natural Cure: Heal status when switching out
        if ("natural-cure".equalsIgnoreCase(engine.abilityName(mon))
                || "natural cure".equalsIgnoreCase(engine.abilityName(mon))) {
            String condition = String.valueOf(mon.get("condition"));
            if (!"healthy".equals(condition) && !"fainted".equals(condition)) {
                mon.put("condition", "healthy");
                mon.put("status", "");
                events.add(mon.get("name") + " 通过自然回复治愈了状态");
            }
        }

        if (Boolean.TRUE.equals(mon.get("dynamaxed"))) {
            engine.endDynamax(mon, events);
        }
        // 切出时清理会在离场后失效的短期 volatile；这里保持旧字段与新 volatile 双向一致。
        mon.put("confused", false);
        mon.put("confusionTurns", 0);
        engine.setVolatile(mon, "tauntTurns", 0);
        engine.setVolatile(mon, "healBlockTurns", 0);
        mon.put("yawnTurns", 0);
        engine.setVolatile(mon, "tormentTurns", 0);
        engine.setVolatile(mon, "disableTurns", 0);
        engine.setVolatile(mon, "disableMove", null);
        engine.setVolatile(mon, "encoreTurns", 0);
        engine.setVolatile(mon, "encoreMove", null);
        if ("toxic".equals(mon.get("condition"))) {
            mon.put("toxicCounter", 0);
        }
        if ("regenerator".equalsIgnoreCase(engine.abilityName(mon))) {
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int currentHp = engine.toInt(mon.get("currentHp"), 0);
            if (currentHp >= maxHp) {
                return;
            }
            if (healBlocked) {
                events.add(mon.get("name") + " 受到回复封锁，再生力未能生效");
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

    void applyEntryAbilities(Map<String, Object> state, boolean player, List<Integer> previousSlots,
            List<String> events) {
        List<Integer> currentSlots = engine.activeSlots(state, player);
        List<Map<String, Object>> enteringTeam = engine.team(state, player);
        for (Integer slot : currentSlots) {
            if (previousSlots.contains(slot) || slot == null || slot < 0 || slot >= enteringTeam.size()) {
                continue;
            }
            Map<String, Object> source = enteringTeam.get(slot);

            // Apply entry hazards FIRST (before abilities)
            applyEntryHazards(state, player, source, events);

            String ability = engine.abilityName(source);

            // Intimidate: Lower opponent's Attack by 1 stage
            if ("intimidate".equalsIgnoreCase(ability)) {
                applyIntimidate(state, player, source, events);
                continue;
            }

            // Weather starters
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

            // Terrain starters
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
                continue;
            }

            // Download: Raise Attack or Sp. Atk based on opponent's lower defensive stat
            if ("download".equalsIgnoreCase(ability)) {
                applyDownload(state, player, source, events);
                continue;
            }
        }
    }

    void applyIntimidate(Map<String, Object> state, boolean player, Map<String, Object> source, List<String> events) {
        source.put("intimidateActivated", true);
        List<Map<String, Object>> opposingTeam = engine.team(state, !player);
        for (Integer targetSlot : engine.activeSlots(state, !player)) {
            if (targetSlot == null || targetSlot < 0 || targetSlot >= opposingTeam.size())
                continue;
            Map<String, Object> target = opposingTeam.get(targetSlot);
            if (engine.toInt(target.get("currentHp"), 0) <= 0)
                continue;

            // Intimidate fails against Clear Body, White Smoke, Full Metal Body, Inner
            // Focus, Oblivious, Own Tempo, Scrappy, and Guard Dog
            if (engine.hasAbility(target, "clear-body", "white-smoke", "full-metal-body", "inner-focus", "oblivious",
                    "own-tempo", "scrappy", "guard-dog", "guard dog")) {
                events.add(target.get("name") + " 的特性挡住了威吓");
                continue;
            }
            if ("clear-amulet".equals(engine.heldItem(target))) {
                events.add(target.get("name") + " 的清净护符挡住了威吓");
                continue;
            }

            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.max(-6, previousStage - 1);
            damageSupport.statStages(target).put("attack", nextStage);
            if (nextStage != previousStage) {
                events.add(source.get("name") + " 的威吓使 " + target.get("name") + " 的攻击降低了");
                triggerStatDropAbilities(target, events);
                restoreLoweredStatsWithWhiteHerb(target, Map.of("attack", previousStage - nextStage), null, events);
            }
        }
    }

    /**
     * Download ability: Raise Attack or Sp. Atk by 1 stage based on opponent's
     * lower defensive stat
     */
    void applyDownload(Map<String, Object> state, boolean player, Map<String, Object> source, List<String> events) {
        List<Map<String, Object>> opposingTeam = engine.team(state, !player);

        // Calculate average Defense and Sp. Def of all active opponents
        int totalDefense = 0;
        int totalSpDef = 0;
        int count = 0;

        for (Integer targetSlot : engine.activeSlots(state, !player)) {
            if (targetSlot == null || targetSlot < 0 || targetSlot >= opposingTeam.size()) {
                continue;
            }
            Map<String, Object> target = opposingTeam.get(targetSlot);
            if (!engine.isAvailableMon(opposingTeam, targetSlot)) {
                continue;
            }
            Map<String, Object> stats = engine.castMap(target.get("stats"));
            totalDefense += engine.toInt(stats.get("defense"), 100);
            totalSpDef += engine.toInt(stats.get("specialDefense"), 100);
            count++;
        }

        if (count == 0)
            return;

        int avgDefense = totalDefense / count;
        int avgSpDef = totalSpDef / count;

        // Raise the stat that targets the weaker defense
        if (avgDefense <= avgSpDef) {
            // Opponent has lower Defense, raise Attack
            int previousStage = damageSupport.statStage(source, "attack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(source).put("attack", nextStage);
            if (nextStage != previousStage) {
                events.add(source.get("name") + " 的下载特性触发了，攻击提升了！");
            }
        } else {
            // Opponent has lower Sp. Def, raise Sp. Atk
            int previousStage = damageSupport.statStage(source, "specialAttack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(source).put("specialAttack", nextStage);
            if (nextStage != previousStage) {
                events.add(source.get("name") + " 的下载特性触发了，特攻提升了！");
            }
        }
    }

    /**
     * Apply self stat boosts for setup moves (Pokemon Showdown standard)
     */
    boolean applySelfStatBoost(Map<String, Object> actor, String stat, int stages, String moveName,
            List<String> events) {
        int previousStage = damageSupport.statStage(actor, stat);
        int nextStage = Math.min(6, previousStage + stages);
        damageSupport.statStages(actor).put(stat, nextStage);
        if (nextStage != previousStage) {
            String statName = getStatChineseName(stat);
            String stageText = stages >= 2 ? "大幅" : "";
            events.add(actor.get("name") + " 使用了 " + moveName + "，" + stageText + "提升了" + statName + "！");
            return true;
        }
        return false;
    }

    boolean applyMultiStatBoost(Map<String, Object> actor, Map<String, Integer> statChanges, String moveName,
            List<String> events) {
        boolean anyBoosted = false;
        for (Map.Entry<String, Integer> entry : statChanges.entrySet()) {
            String stat = entry.getKey();
            int stages = entry.getValue();
            int previousStage = damageSupport.statStage(actor, stat);
            int nextStage = Math.min(6, previousStage + stages);
            damageSupport.statStages(actor).put(stat, nextStage);
            if (nextStage != previousStage) {
                anyBoosted = true;
            }
        }
        if (anyBoosted) {
            StringBuilder boostText = new StringBuilder();
            for (Map.Entry<String, Integer> entry : statChanges.entrySet()) {
                if (boostText.length() > 0)
                    boostText.append("和");
                boostText.append(getStatChineseName(entry.getKey()));
            }
            events.add(actor.get("name") + " 使用了 " + moveName + "，提升了" + boostText.toString() + "！");
        }
        return anyBoosted;
    }

    private String getStatChineseName(String stat) {
        return switch (stat) {
            case "attack" -> "攻击";
            case "defense" -> "防御";
            case "specialAttack" -> "特攻";
            case "specialDefense" -> "特防";
            case "speed" -> "速度";
            default -> stat;
        };
    }

    /**
     * Apply entry hazard damage when a Pokemon switches in (Pokemon Showdown
     * standard)
     */
    void applyEntryHazards(Map<String, Object> state, boolean playerSide, Map<String, Object> mon,
            List<String> events) {
        if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
            return;
        }
        // Heavy-Duty Boots 让携带者完全免疫所有场地钉伤害
        if ("heavy-duty-boots".equals(engine.heldItem(mon))) {
            events.add(mon.get("name") + " 的厚底靴免疫了场地钉伤害");
            return;
        }
        boolean hasMagicGuard = engine.isMagicGuard(mon);

        // Check if Pokemon is Flying-type or has Levitate (immune to Spikes, Toxic
        // Spikes, Sticky Web)
        boolean isFlyingType = false;
        boolean hasLevitate = "levitate".equalsIgnoreCase(engine.abilityName(mon));

        for (Map<String, Object> type : engine.activeTypes(mon)) {
            if (engine.toInt(type.get("type_id"), 0) == DamageCalculatorUtil.TYPE_FLYING) {
                isFlyingType = true;
                break;
            }
        }

        // Stealth Rock damage (based on type effectiveness)
        if (fieldEffectSupport.hasStealthRock(state, !playerSide)) {
            if (hasMagicGuard) {
                events.add(mon.get("name") + " 的魔法防守免疫了隐形岩伤害");
            } else {
                int rockDamage = calculateStealthRockDamage(mon);
                if (rockDamage > 0) {
                    int currentHp = engine.toInt(mon.get("currentHp"), 0);
                    int newHp = Math.max(0, currentHp - rockDamage);
                    mon.put("currentHp", newHp);
                    events.add(mon.get("name") + " 受到了隐形岩的伤害！（-" + rockDamage + " HP）");
                    if (newHp == 0) {
                        mon.put("status", "fainted");
                        events.add(mon.get("name") + " 倒下了");
                    }
                }
            }
        }

        // Spikes damage (based on layers)
        if (!isFlyingType && !hasLevitate) {
            int spikesLayers = fieldEffectSupport.getSpikesLayers(state, !playerSide);
            if (spikesLayers > 0 && hasMagicGuard) {
                events.add(mon.get("name") + " 的魔法防守免疫了撒菱伤害");
            } else if (spikesLayers > 0) {
                double damageFraction = switch (spikesLayers) {
                    case 1 -> 1.0 / 8.0; // 12.5%
                    case 2 -> 1.0 / 6.0; // 16.67%
                    case 3 -> 1.0 / 4.0; // 25%
                    default -> 0;
                };
                if (damageFraction > 0) {
                    Map<String, Object> stats = engine.castMap(mon.get("stats"));
                    int maxHp = engine.toInt(stats.get("hp"), 1);
                    int spikeDamage = Math.max(1, (int) Math.floor(maxHp * damageFraction));
                    int currentHp = engine.toInt(mon.get("currentHp"), 0);
                    int newHp = Math.max(0, currentHp - spikeDamage);
                    mon.put("currentHp", newHp);
                    events.add(mon.get("name") + " 受到了撒菱的伤害！（-" + spikeDamage + " HP）");
                    if (newHp == 0) {
                        mon.put("status", "fainted");
                        events.add(mon.get("name") + " 倒下了");
                    }
                }
            }

            // Toxic Spikes (poison on entry)
            int toxicLayers = fieldEffectSupport.getToxicSpikesLayers(state, !playerSide);
            if (toxicLayers > 0 && mon.get("condition") == null) {
                // Poison/Steel types are immune
                boolean isPoisonOrSteel = false;
                for (Map<String, Object> type : engine.activeTypes(mon)) {
                    int typeId = engine.toInt(type.get("type_id"), 0);
                    if (typeId == DamageCalculatorUtil.TYPE_POISON || typeId == DamageCalculatorUtil.TYPE_STEEL) {
                        isPoisonOrSteel = true;
                        break;
                    }
                }

                if (!isPoisonOrSteel) {
                    if (toxicLayers == 1) {
                        mon.put("condition", "poison");
                        mon.put("toxicCounter", 0);
                        events.add(mon.get("name") + " 中了毒！");
                    } else {
                        mon.put("condition", "toxic");
                        mon.put("toxicCounter", 1);
                        events.add(mon.get("name") + " 中了剧毒！");
                    }
                }
            }

            // Sticky Web (speed drop)
            if (fieldEffectSupport.hasStickyWeb(state, !playerSide)) {
                int prevStage = damageSupport.statStage(mon, "speed");
                int nextStage = Math.max(-6, prevStage - 1);
                damageSupport.statStages(mon).put("speed", nextStage);
                if (nextStage != prevStage) {
                    events.add(mon.get("name") + " 的速度被黏黏网降低了！");
                }
            }
        }
    }

    private int calculateStealthRockDamage(Map<String, Object> mon) {
        // Stealth Rock deals 1/8 damage multiplied by type effectiveness against Rock
        Map<String, Object> stats = engine.castMap(mon.get("stats"));
        int maxHp = engine.toInt(stats.get("hp"), 1);

        // Calculate type effectiveness against Rock-type moves
        double effectiveness = 1.0;
        for (Map<String, Object> type : engine.activeTypes(mon)) {
            int typeId = engine.toInt(type.get("type_id"), 0);
            effectiveness *= getTypeEffectivenessAgainstRock(typeId);
        }

        if (effectiveness <= 0)
            return 0;

        int baseDamage = Math.max(1, (int) Math.floor(maxHp / 8.0));
        return Math.max(1, (int) Math.floor(baseDamage * effectiveness));
    }

    private double getTypeEffectivenessAgainstRock(int typeId) {
        // Rock is super effective against: Flying, Bug, Fire, Ice
        // Rock is not very effective against: Fighting, Ground, Steel
        return switch (typeId) {
            case DamageCalculatorUtil.TYPE_FLYING, DamageCalculatorUtil.TYPE_BUG,
                    DamageCalculatorUtil.TYPE_FIRE, DamageCalculatorUtil.TYPE_ICE ->
                2.0;
            case DamageCalculatorUtil.TYPE_FIGHTING, DamageCalculatorUtil.TYPE_GROUND,
                    DamageCalculatorUtil.TYPE_STEEL ->
                0.5;
            default -> 1.0;
        };
    }

    /**
     * Apply recovery moves (Pokemon Showdown standard)
     */
    boolean applyRecoveryMove(Map<String, Object> actor, Map<String, Object> move, String moveName,
            List<String> events) {
        Map<String, Object> stats = engine.castMap(actor.get("stats"));
        int maxHp = engine.toInt(stats.get("hp"), 1);
        int currentHp = engine.toInt(actor.get("currentHp"), 0);

        if (currentHp >= maxHp) {
            events.add(actor.get("name") + " 使用了 " + moveName + "，但 HP 已满！");
            return false;
        }

        // Calculate heal amount based on move type and weather
        double healFraction = getHealFraction(move, moveName);
        int healAmount = Math.max(1, (int) Math.floor(maxHp * healFraction));
        int newHp = Math.min(maxHp, currentHp + healAmount);

        actor.put("currentHp", newHp);
        events.add(actor.get("name") + " 使用了 " + moveName + "，恢复了 " + healAmount + " HP！");

        // Special handling for Rest
        if (engine.isRest(move)) {
            actor.put("condition", "sleep");
            actor.put("sleepTurns", 2);
            events.add(actor.get("name") + " 睡着了！");
        }

        // Special handling for Roost (loses Flying type this turn)
        if (engine.isRoost(move)) {
            events.add(actor.get("name") + " 落地了，暂时失去了飞行属性！");
        }

        return true;
    }

    private double getHealFraction(Map<String, Object> move, String moveName) {
        // Weather-dependent moves: Synthesis, Moonlight, Morning Sun
        if (engine.isSynthesis(move) || engine.isMoonlight(move) || engine.isMorningSun(move)) {
            // In sun: 2/3, normal: 1/2, other weather: 1/4
            if (fieldEffectSupport.sunTurns(null) > 0) {
                return 2.0 / 3.0;
            } else if (fieldEffectSupport.weatherTurns(null) > 0) {
                return 1.0 / 4.0;
            } else {
                return 1.0 / 2.0;
            }
        }

        // Standard recovery moves: Recover, Soft-Boiled, Milk Drink, Roost = 1/2
        if (engine.isRecover(move) || engine.isSoftBoiled(move) || engine.isMilkDrink(move) || engine.isRoost(move)) {
            return 1.0 / 2.0;
        }

        // Rest: full heal but causes sleep
        if (engine.isRest(move)) {
            return 1.0;
        }

        return 1.0 / 2.0; // Default
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
            restoreLoweredStatsWithWhiteHerb(target, Map.of("speed", previousStage - nextStage), actionLog, events);
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
            restoreLoweredStatsWithWhiteHerb(target, Map.of("specialAttack", previousStage - nextStage), actionLog,
                    events);
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
            restoreLoweredStatsWithWhiteHerb(target, Map.of("specialDefense", previousStage - nextStage), actionLog,
                    events);
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
        int attackDropAmount = 0;
        int previousAttack = damageSupport.statStage(target, "attack");
        int nextAttack = Math.max(-6, previousAttack - 1);
        damageSupport.statStages(target).put("attack", nextAttack);
        if (nextAttack != previousAttack) {
            attackDropped = true;
            attackDropAmount = previousAttack - nextAttack;
            actionLog.put("attackStageChange", nextAttack - previousAttack);
        }
        boolean specialAttackDropped = false;
        int specialAttackDropAmount = 0;
        int previousSpecialAttack = damageSupport.statStage(target, "specialAttack");
        int nextSpecialAttack = Math.max(-6, previousSpecialAttack - 1);
        damageSupport.statStages(target).put("specialAttack", nextSpecialAttack);
        if (nextSpecialAttack != previousSpecialAttack) {
            specialAttackDropped = true;
            specialAttackDropAmount = previousSpecialAttack - nextSpecialAttack;
            actionLog.put("specialAttackStageChange", nextSpecialAttack - previousSpecialAttack);
        }
        if (attackDropped || specialAttackDropped) {
            Map<String, Integer> droppedStages = new java.util.LinkedHashMap<>();
            if (attackDropped) {
                droppedStages.put("attack", attackDropAmount);
            }
            if (specialAttackDropped) {
                droppedStages.put("specialAttack", specialAttackDropAmount);
            }
            triggerStatDropAbilities(target, events);
            restoreLoweredStatsWithWhiteHerb(target, droppedStages, actionLog, events);
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
            return;
        }
        if ("guard-dog".equalsIgnoreCase(ability) || "guard dog".equalsIgnoreCase(ability)) {
            int previousStage = damageSupport.statStage(target, "attack");
            int nextStage = Math.min(6, previousStage + 1);
            damageSupport.statStages(target).put("attack", nextStage);
            if (nextStage != previousStage) {
                events.add(target.get("name") + " 的看门狗特性触发，攻击提升了");
            }
        }
    }

    void resetBattleStages(Map<String, Object> mon) {
        // 该方法用于“真正离场重置”的场景，只重置会随离场清空的能力阶段与短期控制状态。
        damageSupport.statStages(mon).put("attack", 0);
        damageSupport.statStages(mon).put("defense", 0);
        damageSupport.statStages(mon).put("specialAttack", 0);
        damageSupport.statStages(mon).put("specialDefense", 0);
        damageSupport.statStages(mon).put("speed", 0);
        engine.setVolatile(mon, "tauntTurns", 0);
        engine.setVolatile(mon, "tormentTurns", 0);
        mon.put("protectionStreak", 0);
        mon.put("lastProtectionRound", 0);
    }

    private int confusionSelfDamage(Map<String, Object> mon) {
        Map<String, Object> stats = engine.castMap(mon.get("stats"));
        int attack = damageSupport.modifiedAttackStat(mon, mon, engine.toInt(stats.get("attack"), 100),
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, false);
        int defense = Math.max(1, damageSupport.modifiedDefenseStat(mon, mon, engine.toInt(stats.get("defense"), 100),
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, new java.util.LinkedHashMap<>(), false));
        return Math.max(1, DamageCalculatorUtil.calculateBaseDamage(50, 40, attack, defense));
    }

    private void applyMoveMetaFlinch(Map<String, Object> target, Map<String, Object> move,
            Map<String, Object> targetLog,
            List<String> events, Random random) {
        if (targetLog.containsKey("flinch") || targetLog.containsKey("flinchBlocked")) {
            return;
        }
        int chance = engine.toInt(move.get("flinch_chance"), 0);
        if (!rollSecondaryChance(random, chance)) {
            return;
        }
        if ("inner-focus".equalsIgnoreCase(engine.abilityName(target))
                || "inner focus".equalsIgnoreCase(engine.abilityName(target))) {
            targetLog.put("flinchBlocked", true);
            targetLog.put("ability", engine.abilityName(target));
            events.add(target.get("name") + " 的特性让其不会畏缩");
            return;
        }
        if (blocksSecondaryEffects(target, "flinch", targetLog, events)) {
            return;
        }
        engine.setVolatile(target, "flinch", true);
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

    private void applyMoveMetaStatDrops(Map<String, Object> source, Map<String, Object> target,
            Map<String, Object> move,
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

        boolean needsSecondaryBlock = statChanges.stream()
                .anyMatch(change -> engine.toInt(change.get("change"), 0) < 0);
        if (needsSecondaryBlock && blocksSecondaryEffects(target, "stat-drop", targetLog, events)) {
            return;
        }

        Map<String, Integer> droppedStages = new java.util.LinkedHashMap<>();
        for (Map<String, Object> statChange : statChanges) {
            int delta = engine.toInt(statChange.get("change"), 0);
            int statId = engine.toInt(statChange.get("stat_id"), 0);
            if (delta >= 0 || hasStageChangeLog(targetLog, statId)) {
                continue;
            }
            int appliedDrop = applyStageChange(source, target, statId, delta, targetLog, events);
            if (appliedDrop > 0) {
                String statKey = statFieldKey(statId);
                if (!statKey.isBlank()) {
                    droppedStages.merge(statKey, appliedDrop, (a, b) -> a + b);
                }
            }
        }
        if (!droppedStages.isEmpty()) {
            restoreLoweredStatsWithWhiteHerb(target, droppedStages, targetLog, events);
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

    private void tryApplyTriAttackStatus(Map<String, Object> state, Map<String, Object> actor,
            Map<String, Object> target,
            Map<String, Object> move, Map<String, Object> targetLog, List<String> events,
            Random random, int chance) {
        if (!rollSecondaryChance(random, chance)
                || blocksSecondaryEffects(target, "tri-attack-status", targetLog, events)) {
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

    private int applyStageChange(Map<String, Object> source, Map<String, Object> target, int statId, int delta,
            Map<String, Object> actionLog, List<String> events) {
        String statKey = statFieldKey(statId);
        if (statKey.isBlank() || delta == 0) {
            return 0;
        }
        if (delta < 0 && isStatDropBlocked(target, actionLog, events, statKey + "DropBlocked",
                statDisplayName(statId) + "下降")) {
            return 0;
        }
        int previousStage = damageSupport.statStage(target, statKey);
        int nextStage = Math.max(-6, Math.min(6, previousStage + delta));
        if (nextStage == previousStage) {
            return 0;
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
        return delta < 0 ? previousStage - nextStage : 0;
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
        if (hasAbility(target, "clear-body", "clear body", "white-smoke", "white smoke", "full-metal-body",
                "full metal body")) {
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

    private boolean moveAppliesOwnStatChanges(Map<String, Object> move) {
        String effectShort = String.valueOf(move.getOrDefault("effect_short", "")).toLowerCase();
        if (effectShort.contains("user's")) {
            return true;
        }
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        return matches(nameEn,
                "close-combat", "close combat",
                "draco-meteor", "draco meteor",
                "overheat",
                "leaf-storm", "leaf storm",
                "superpower",
                "make-it-rain", "make it rain",
                "v-create", "v create");
    }

    private boolean consumeMentalHerb(Map<String, Object> target, Map<String, Object> actionLog,
            List<String> events, String effectName) {
        if (!"mental-herb".equals(engine.heldItem(target)) || engine.itemConsumed(target)) {
            return false;
        }
        engine.consumeItem(target);
        actionLog.put("mentalHerb", true);
        events.add(target.get("name") + " 的心灵香草解除了" + effectName);
        return true;
    }

    private void restoreLoweredStatsWithWhiteHerb(Map<String, Object> target, Map<String, Integer> droppedStages,
            Map<String, Object> actionLog, List<String> events) {
        if (!"white-herb".equals(engine.heldItem(target)) || engine.itemConsumed(target) || droppedStages.isEmpty()) {
            return;
        }
        boolean restored = false;
        for (Map.Entry<String, Integer> entry : droppedStages.entrySet()) {
            int amount = Math.max(0, entry.getValue());
            if (amount <= 0) {
                continue;
            }
            int currentStage = damageSupport.statStage(target, entry.getKey());
            int restoredStage = Math.min(6, currentStage + amount);
            if (restoredStage != currentStage) {
                damageSupport.statStages(target).put(entry.getKey(), restoredStage);
                restored = true;
            }
        }
        if (!restored) {
            return;
        }
        engine.consumeItem(target);
        if (actionLog != null) {
            actionLog.put("whiteHerb", true);
        }
        events.add(target.get("name") + " 的白色香草恢复了下降的能力");
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

    private boolean applySharedMoveBlockers(Map<String, Object> attacker, Map<String, Object> target,
            Map<String, Object> move,
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
        if (!ignoresTargetAbility(attacker)
                && hasAbility(target, "soundproof", "sound proof")
                && hasMoveFlag(move, "sound")) {
            blockMoveByAbility(actionLog, events, "soundproof", target.get("name") + " 的隔音挡住了声音招式");
            return true;
        }
        if (!ignoresTargetAbility(attacker)
                && hasAbility(target, "bulletproof", "bullet proof")
                && hasMoveFlag(move, "bullet")) {
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

    private boolean blockedBySafeguard(Map<String, Object> state, Map<String, Object> source,
            Map<String, Object> target,
            Map<String, Object> move, Map<String, Object> actionLog, List<String> events) {
        if (state == null || source == null || target == null || source == target) {
            return false;
        }
        boolean targetOnPlayerSide = engine.isOnSide(state, target, true);
        if (engine.isOnSide(state, source, true) == targetOnPlayerSide
                || fieldEffectSupport.safeguardTurns(state, targetOnPlayerSide) <= 0) {
            return false;
        }
        actionLog.put("result", "safeguard");
        actionLog.put("damage", 0);
        events.add(target.get("name") + " 受到神秘守护保护，避免了异常状态");
        return true;
    }

    private boolean hasRemovableHeldItem(Map<String, Object> target) {
        return !engine.heldItem(target).isBlank() && !engine.itemConsumed(target);
    }

    private boolean isBlockedByAromaVeil(Map<String, Object> state, Map<String, Object> target,
            Map<String, Object> move,
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
                "yawn",
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

    private boolean attackerTargetsOpposingSide(Map<String, Object> state, String actingSide,
            Map<String, Object> target) {
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

    private void applyWindRiderBoost(Map<String, Object> mon, Map<String, Object> actionLog, List<String> events,
            String trigger) {
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

    private void applyTeamStatBoost(Map<String, Object> state, boolean playerSide, String statKey, int stages,
            List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size())
                continue;
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0)
                continue;

            int currentStage = damageSupport.statStage(mon, statKey);
            int nextStage = Math.min(6, currentStage + stages);
            if (nextStage > currentStage) {
                damageSupport.statStages(mon).put(statKey, nextStage);
                events.add(mon.get("name") + " 的 " + statKey + " 提升了");
            }
        }
    }

    private void applyTeamStatDrop(Map<String, Object> state, boolean playerSide, String statKey, int stages,
            List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size())
                continue;
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0)
                continue;

            int currentStage = damageSupport.statStage(mon, statKey);
            int nextStage = Math.max(-6, currentStage - stages);
            if (nextStage < currentStage) {
                damageSupport.statStages(mon).put(statKey, nextStage);
                events.add(mon.get("name") + " 的 " + statKey + " 下降了");
            }
        }
    }
}
