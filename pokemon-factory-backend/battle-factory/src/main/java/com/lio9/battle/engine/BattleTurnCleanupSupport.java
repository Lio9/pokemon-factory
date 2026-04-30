package com.lio9.battle.engine;

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleTurnCleanupSupport {
    /**
     * 回合结束清算支持类。
     * <p>
     * 这里负责回合末残余伤害、回复、特性触发、Dynamax 倒计时，以及各类 volatile/控制状态的递减。
     * 本轮特别将 taunt / healBlock / torment / disable / encore 的回合递减统一改为通过 BattleEngine
     * 的
     * volatile 访问器读写，以保证新旧状态结构同步。
     * </p>
     */
    private final BattleEngine engine;
    private final BattleFieldEffectSupport fieldEffectSupport;
    private final BattleConditionSupport conditionSupport;

    BattleTurnCleanupSupport(BattleEngine engine, BattleFieldEffectSupport fieldEffectSupport,
            BattleConditionSupport conditionSupport) {
        this.engine = engine;
        this.fieldEffectSupport = fieldEffectSupport;
        this.conditionSupport = conditionSupport;
    }

    void applyEndTurnEffects(Map<String, Object> state, Map<String, Object> fieldSnapshot, List<String> events,
            Random random, int currentRound) {
        // 结算顺序尽量保持稳定：先状态伤害/回复，再能力与场地，再递减各种倒计时。
        applyEndTurnStatusEffects(engine.team(state, true), events);
        applyEndTurnStatusEffects(engine.team(state, false), events);
        applyEndTurnHealing(engine.team(state, true), events);
        applyEndTurnHealing(engine.team(state, false), events);
        applyEndTurnAbilityEffects(state, true, events);
        applyEndTurnAbilityEffects(state, false, events);
        applyEndTurnFieldEffects(state, events);
        decrementDynamax(engine.team(state, true), events);
        decrementDynamax(engine.team(state, false), events);
        applyVolatileEndTurnEffects(engine.team(state, true), events);
        applyVolatileEndTurnEffects(engine.team(state, false), events);
        decrementTauntEffects(engine.team(state, true), events);
        decrementTauntEffects(engine.team(state, false), events);
        decrementHealBlockEffects(engine.team(state, true), events);
        decrementHealBlockEffects(engine.team(state, false), events);
        decrementTormentEffects(engine.team(state, true), events);
        decrementTormentEffects(engine.team(state, false), events);
        decrementDisableEffects(engine.team(state, true), events);
        decrementDisableEffects(engine.team(state, false), events);
        decrementEncoreEffects(engine.team(state, true), events);
        decrementEncoreEffects(engine.team(state, false), events);
        decrementYawnEffects(state, engine.team(state, true), events, random, currentRound);
        decrementYawnEffects(state, engine.team(state, false), events, random, currentRound);
        fieldEffectSupport.decrementFieldEffects(state, fieldSnapshot, events);
    }

    void clearFlinch(Map<String, Object> state) {
        clearFlinch(engine.team(state, true));
        clearFlinch(engine.team(state, false));
    }

    private void applyEndTurnFieldEffects(Map<String, Object> state, List<String> events) {
        // 沙暴伤害 (Sandstorm)
        if (fieldEffectSupport.sandTurns(state) > 0) {
            applyWeatherDamage(state, true, "sand", events);
            applyWeatherDamage(state, false, "sand", events);
        }
        // 冰雹/雪天伤害 (Hail/Snow)
        if (fieldEffectSupport.snowTurns(state) > 0) {
            applyWeatherDamage(state, true, "snow", events);
            applyWeatherDamage(state, false, "snow", events);
        }
        // 青草场地回血 (Grassy Terrain)
        if (fieldEffectSupport.grassyTerrainTurns(state) > 0) {
            applyGrassyTerrainHealing(state, true, events);
            applyGrassyTerrainHealing(state, false, events);
        }
    }

    private void applyWeatherDamage(Map<String, Object> state, boolean playerSide, String weatherType,
            List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size())
                continue;
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0)
                continue;

            // 免疫判定：岩石/地面/钢系免疫沙暴；冰系免疫雪天；魔法守护特性免疫所有天气伤害
            boolean immune = false;
            if ("sand".equals(weatherType)) {
                immune = engine.targetHasType(mon, DamageCalculatorUtil.TYPE_ROCK) ||
                        engine.targetHasType(mon, DamageCalculatorUtil.TYPE_GROUND) ||
                        engine.targetHasType(mon, DamageCalculatorUtil.TYPE_STEEL);
            } else if ("snow".equals(weatherType)) {
                immune = engine.targetHasType(mon, DamageCalculatorUtil.TYPE_ICE);
            }
            immune = immune || "overcoat".equalsIgnoreCase(engine.abilityName(mon)) ||
                    "safety-goggles".equals(engine.heldItem(mon)) || engine.isMagicGuard(mon);

            if (immune)
                continue;

            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int damage = Math.max(1, maxHp / 16);
            int remainingHp = Math.max(0, engine.toInt(mon.get("currentHp"), 0) - damage);
            mon.put("currentHp", remainingHp);
            if (remainingHp == 0) {
                mon.put("status", "fainted");
                events.add(mon.get("name") + " 被" + ("sand".equals(weatherType) ? "沙暴" : "冰雹") + "击倒了");
            } else {
                events.add(mon.get("name") + " 受到" + ("sand".equals(weatherType) ? "沙暴" : "冰雹") + "影响损失了 " + damage
                        + " 点 HP");
            }
        }
    }

    private void applyGrassyTerrainHealing(Map<String, Object> state, boolean playerSide, List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0 || !grassyTerrainActiveFor(mon, state)) {
                continue;
            }
            if (engine.healBlockTurns(mon) > 0) {
                events.add(mon.get("name") + " 受到回复封锁，无法从青草场地回复 HP");
                continue;
            }
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int currentHp = engine.toInt(mon.get("currentHp"), 0);
            if (currentHp >= maxHp) {
                continue;
            }
            int heal = Math.max(1, maxHp / 16);
            mon.put("currentHp", Math.min(maxHp, currentHp + heal));
            events.add(mon.get("name") + " 受青草场地影响回复了 " + heal + " 点 HP");
        }
    }

    private void decrementTauntEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.tauntTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            // 统一写回 volatile，内部会同步旧字段，保证旧代码读取仍然正确。
            engine.setVolatile(mon, "tauntTurns", after);
            if (after == 0 && engine.toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 不再处于挑衅状态");
            }
        }
    }

    private void decrementHealBlockEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.healBlockTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            engine.setVolatile(mon, "healBlockTurns", after);
            if (after == 0 && engine.toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 不再受回复封锁影响");
            }
        }
    }

    private void decrementEncoreEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.encoreTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            engine.setVolatile(mon, "encoreTurns", after);
            if (after == 0) {
                // Encore 结束时必须同步清空被强制重复的招式名，否则 canUseMove 仍可能错误受限。
                engine.setVolatile(mon, "encoreMove", null);
                if (engine.toInt(mon.get("currentHp"), 0) > 0) {
                    events.add(mon.get("name") + " 不再受再来一次影响");
                }
            }
        }
    }

    private void decrementTormentEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.tormentTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            engine.setVolatile(mon, "tormentTurns", after);
            if (after == 0 && engine.toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 不再受无理取闹影响");
            }
        }
    }

    private void decrementDisableEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.disableTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            engine.setVolatile(mon, "disableTurns", after);
            if (after == 0) {
                // Disable 结束时同理需要清除 disableMove，避免后续招式合法性判断读到脏数据。
                engine.setVolatile(mon, "disableMove", null);
                if (engine.toInt(mon.get("currentHp"), 0) > 0) {
                    events.add(mon.get("name") + " 不再受定身法影响");
                }
            }
        }
    }

    private void decrementYawnEffects(Map<String, Object> state, List<Map<String, Object>> team, List<String> events,
            Random random, int currentRound) {
        for (Map<String, Object> mon : team) {
            int before = engine.yawnTurns(mon);
            if (before <= 0) {
                continue;
            }
            int after = Math.max(0, before - 1);
            // Yawn 依旧通过 volatile 写回，确保回合末触发睡眠前后日志与状态一致。
            engine.setVolatile(mon, "yawnTurns", after);
            if (after == 0) {
                conditionSupport.resolveYawn(state, mon, events, random, currentRound);
            }
        }
    }

    private void applyEndTurnStatusEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            String condition = String.valueOf(mon.get("condition"));
            int damage;
            String eventPrefix;
            if ("burn".equals(condition)) {
                damage = Math.max(1, maxHp / 16);
                eventPrefix = "灼伤";
            } else if ("poison".equals(condition)) {
                damage = Math.max(1, maxHp / 8);
                eventPrefix = "中毒";
            } else if ("toxic".equals(condition)) {
                int toxicCounter = Math.max(1, engine.toInt(mon.get("toxicCounter"), 1));
                damage = Math.max(1, (maxHp * toxicCounter) / 16);
                mon.put("toxicCounter", Math.min(15, toxicCounter + 1));
                eventPrefix = "剧毒";
            } else {
                continue;
            }
            if (engine.isMagicGuard(mon)) {
                if ("toxic".equals(condition)) {
                    int toxicCounter = Math.max(1, engine.toInt(mon.get("toxicCounter"), 1));
                    mon.put("toxicCounter", Math.min(15, toxicCounter + 1));
                }
                continue;
            }
            int currentHp = engine.toInt(mon.get("currentHp"), 0);
            int remainingHp = Math.max(0, currentHp - damage);
            mon.put("currentHp", remainingHp);
            if (remainingHp == 0) {
                mon.put("status", "fainted");
                events.add(mon.get("name") + " 因" + eventPrefix + "倒下了");
            } else {
                events.add(mon.get("name") + " 受到" + eventPrefix + "影响，损失了 " + damage + " 点 HP");
            }
        }
    }

    private void applyEndTurnHealing(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) <= 0 || !"leftovers".equals(engine.heldItem(mon))) {
                continue;
            }
            if (engine.healBlockTurns(mon) > 0) {
                events.add(mon.get("name") + " 受到回复封锁，剩饭无法生效");
                continue;
            }
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int currentHp = engine.toInt(mon.get("currentHp"), 0);
            if (currentHp >= maxHp) {
                continue;
            }
            int heal = Math.max(1, maxHp / 16);
            mon.put("currentHp", Math.min(maxHp, currentHp + heal));
            events.add(mon.get("name") + " 通过剩饭回复了 " + heal + " 点 HP");
        }
    }

    private void applyVolatileEndTurnEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            // 寄生种子：吸取 1/8 最大 HP，对手回复等量 HP
            if (Boolean.TRUE.equals(mon.get("leechSeed"))) {
                applyLeechSeedDamage(mon, events);
            }
            // 束缚招式（火焰旋涡/潮旋/绑紧等）：每回合 1/8 最大 HP
            if (Boolean.TRUE.equals(mon.get("bound"))) {
                applyFractionalDamage(mon, 8, events, "因束缚招式损失了");
            }
            // 灭亡之歌：倒计时归零时直接倒下
            int perishTurns = engine.toInt(mon.get("perishSongTurns"), 0);
            if (perishTurns > 0) {
                int next = perishTurns - 1;
                engine.setVolatile(mon, "perishSongTurns", next);
                if (next <= 0) {
                    mon.put("currentHp", 0);
                    mon.put("status", "fainted");
                    events.add(mon.get("name") + " 被灭亡之歌带走了");
                }
            }
            // 水流环：回复 1/16 最大 HP
            if (Boolean.TRUE.equals(mon.get("aquaRing"))) {
                applyFractionalHeal(mon, 16, events, "的水流环回复了");
            }
            // 扎根：回复 1/16 最大 HP
            if (Boolean.TRUE.equals(mon.get("ingrain"))) {
                applyFractionalHeal(mon, 16, events, "的扎根回复了");
            }
            // 诅咒（幽灵）：每回合损失 1/4 最大 HP
            if (Boolean.TRUE.equals(mon.get("cursed"))) {
                int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
                int curseDmg = Math.max(1, maxHp / 4);
                int curHp = engine.toInt(mon.get("currentHp"), 0);
                mon.put("currentHp", Math.max(0, curHp - curseDmg));
                events.add(mon.get("name") + " 受到诅咒，损失了 " + curseDmg + " 点 HP");
                if (curHp - curseDmg <= 0) {
                    mon.put("status", "fainted");
                    events.add(mon.get("name") + " 倒下了");
                }
            }
        }
    }

    private void applyLeechSeedDamage(Map<String, Object> mon, List<String> events) {
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
        int damage = Math.max(1, maxHp / 8);
        int currentHp = engine.toInt(mon.get("currentHp"), 0);
        if (currentHp <= 0) {
            return;
        }
        int actualDamage = Math.min(damage, currentHp);
        mon.put("currentHp", currentHp - actualDamage);
        events.add(mon.get("name") + " 因寄生种子损失了 " + actualDamage + " 点 HP");
        if (currentHp - actualDamage <= 0) {
            mon.put("status", "fainted");
            events.add(mon.get("name") + " 倒下了");
        }
    }

    private void applyFractionalDamage(Map<String, Object> mon, int denominator, List<String> events, String msg) {
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
        int damage = Math.max(1, maxHp / denominator);
        int curHp = engine.toInt(mon.get("currentHp"), 0);
        if (curHp <= 0) return;
        int actual = Math.min(damage, curHp);
        mon.put("currentHp", curHp - actual);
        events.add(mon.get("name") + " " + msg + " " + actual + " 点 HP");
        if (curHp - actual <= 0) {
            mon.put("status", "fainted");
            events.add(mon.get("name") + " 倒下了");
        }
    }

    private void applyFractionalHeal(Map<String, Object> mon, int denominator, List<String> events, String msg) {
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
        int curHp = engine.toInt(mon.get("currentHp"), 0);
        if (curHp >= maxHp || curHp <= 0) {
            return;
        }
        int heal = Math.max(1, maxHp / denominator);
        mon.put("currentHp", Math.min(maxHp, curHp + heal));
        events.add(mon.get("name") + msg + " " + heal + " 点 HP");
    }

    private void applyEndTurnAbilityEffects(Map<String, Object> state, boolean playerSide, List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            String ability = engine.abilityName(mon);
            if ("speed-boost".equalsIgnoreCase(ability) || "speed boost".equalsIgnoreCase(ability)) {
                applySpeedBoost(mon, events);
            }
        }
    }

    private void applySpeedBoost(Map<String, Object> mon, List<String> events) {
        Map<String, Object> statStages = engine.castMap(mon.get("statStages"));
        int currentStage = engine.toInt(statStages.get("speed"), 0);
        if (currentStage >= 6) {
            return;
        }
        statStages.put("speed", Math.min(6, currentStage + 1));
        events.add(mon.get("name") + " 的加速提升了速度");
    }

    private void clearFlinch(List<Map<String, Object>> team) {
        for (Map<String, Object> mon : team) {
            engine.setVolatile(mon, "flinch", false);
        }
    }

    private void decrementDynamax(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int remaining = engine.toInt(mon.get("dynamaxTurnsRemaining"), 0);
            if (!Boolean.TRUE.equals(mon.get("dynamaxed")) || remaining <= 0) {
                continue;
            }
            remaining -= 1;
            mon.put("dynamaxTurnsRemaining", remaining);
            if (remaining == 0) {
                engine.endDynamax(mon, events);
            }
        }
    }

    private boolean grassyTerrainActiveFor(Map<String, Object> mon, Map<String, Object> state) {
        return state != null && fieldEffectSupport.grassyTerrainTurns(state) > 0 && engine.isGrounded(mon);
    }

}
