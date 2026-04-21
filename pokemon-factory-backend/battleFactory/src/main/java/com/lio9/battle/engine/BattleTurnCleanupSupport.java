package com.lio9.battle.engine;

import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;

final class BattleTurnCleanupSupport {
    private final BattleEngine engine;
    private final BattleFieldEffectSupport fieldEffectSupport;

    BattleTurnCleanupSupport(BattleEngine engine, BattleFieldEffectSupport fieldEffectSupport) {
        this.engine = engine;
        this.fieldEffectSupport = fieldEffectSupport;
    }

    void applyEndTurnEffects(Map<String, Object> state, Map<String, Object> fieldSnapshot, List<String> events) {
        applyEndTurnStatusEffects(engine.team(state, true), events);
        applyEndTurnStatusEffects(engine.team(state, false), events);
        applyEndTurnHealing(engine.team(state, true), events);
        applyEndTurnHealing(engine.team(state, false), events);
        applyEndTurnFieldEffects(state, events);
        decrementDynamax(engine.team(state, true), events);
        decrementDynamax(engine.team(state, false), events);
        decrementTauntEffects(engine.team(state, true), events);
        decrementTauntEffects(engine.team(state, false), events);
        fieldEffectSupport.decrementFieldEffects(state, fieldSnapshot, events);
    }

    void clearFlinch(Map<String, Object> state) {
        clearFlinch(engine.team(state, true));
        clearFlinch(engine.team(state, false));
    }

    private void applyEndTurnFieldEffects(Map<String, Object> state, List<String> events) {
        if (fieldEffectSupport.sandTurns(state) > 0) {
            applySandstormDamage(state, true, events);
            applySandstormDamage(state, false, events);
        }
        if (fieldEffectSupport.grassyTerrainTurns(state) > 0) {
            applyGrassyTerrainHealing(state, true, events);
            applyGrassyTerrainHealing(state, false, events);
        }
    }

    private void applySandstormDamage(Map<String, Object> state, boolean playerSide, List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0 || sandstormImmune(mon)) {
                continue;
            }
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int damage = Math.max(1, maxHp / 16);
            int remainingHp = Math.max(0, engine.toInt(mon.get("currentHp"), 0) - damage);
            mon.put("currentHp", remainingHp);
            if (remainingHp == 0) {
                mon.put("status", "fainted");
                events.add(mon.get("name") + " 被沙暴击倒了");
            } else {
                events.add(mon.get("name") + " 受沙暴影响损失了 " + damage + " 点 HP");
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
            mon.put("tauntTurns", after);
            if (after == 0 && engine.toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 不再处于挑衅状态");
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

    private void clearFlinch(List<Map<String, Object>> team) {
        for (Map<String, Object> mon : team) {
            mon.put("flinched", false);
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

    private boolean sandstormImmune(Map<String, Object> mon) {
        return engine.targetHasType(mon, DamageCalculatorUtil.TYPE_ROCK)
                || engine.targetHasType(mon, DamageCalculatorUtil.TYPE_GROUND)
                || engine.targetHasType(mon, DamageCalculatorUtil.TYPE_STEEL)
                || "safety-goggles".equals(engine.heldItem(mon))
                || "overcoat".equalsIgnoreCase(engine.abilityName(mon));
    }
}
