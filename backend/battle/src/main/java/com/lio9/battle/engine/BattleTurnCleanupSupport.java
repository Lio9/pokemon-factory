package com.lio9.battle.engine;

import com.lio9.battle.engine.event.BattleEvent;
import com.lio9.battle.engine.event.BattleEventType;
import com.lio9.pokedex.util.DamageCalculatorUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class BattleTurnCleanupSupport {
/**
 * ============================================================
 * 回合末清算 / End-of-Turn Cleanup
 * ============================================================
 *
 * ## 核心职责 / Core Responsibility
 *
 * 负责每回合结束时所有"持续性效果"的结算：
 * Manages ALL persistent effect resolutions at end of each turn:
 *
 * ## 执行顺序 / Execution Order （严格按此顺序 / Strict Order）
 *
 *   1. ON_TURN_END 事件（特性/道具响应）/ Turn End Event
 *   2. 清除"本回合换入"标记 / Clear "justSwitchedIn" flag
 *   3. 状态伤害 / Status Damage:
 *      - 灼伤 (1/16 max HP) / Burn
 *      - 中毒 (1/8 max HP) / Poison
 *      - 剧毒 (1/16→2/16→...→15/16) / Toxic (escalating)
 *   4. 剩饭回复 / Leftovers healing (1/16 max HP)
 *   5. 青草场地回复 / Grassy Terrain healing (1/16)
 *   6. 特性回合末效果 / Ability End-of-Turn Effects:
 *      - 加速 Speed Boost / 随手变 Moody / 恶梦 Bad Dreams
 *      - 雨盘 Rain Dish / 冰鳞粉 Ice Body / 毒疗 Poison Heal
 *      - 湿润身躯 Hydration / 蜕皮 Shed Skin / 治愈之心 Healer
 *      - 收获 Harvest / 饱了又饿 Hunger Switch
 *   7. 道具回合末效果 / Item End-of-Turn Effects:
 *      - 火焰宝珠 Flame Orb / 剧毒宝珠 Toxic Orb
 *      - 黑色污泥 Black Sludge / 毒针 Sticky Barb
 *      - 状态回复树果 / Status-curing berries
 *   8. 反刍重新触发树果 / Cud Chew re-triggers berry
 *   9. Volatile 状态效果:
 *      - 寄生种子 (1/8) / 束缚绑定 (1/8) / 灭亡之歌 (归零→倒下)
 *      - 水流环 (1/16) / 扎根 (1/16) / 恶梦 (1/8)
 *      - 八爪束缚 (-1 防/特防) / 盐腌 (1/8, 钢/水1/4) / 诅咒 (1/4)
 *   10. 场地伤害 / Field Damage:
 *       - 沙暴 (非岩/地/钢 ×1/16) / Sandstorm
 *       - 冰雹 (非冰 ×1/16) / Hail/Snow
 *       - G-Max 持续伤害 (1/6) / G-Max persistent damage
 *   11. 延迟攻击触发 / Future Sight / Doom Desire
 *   12. 各类封锁递减 / Lock Effect Decrements:
 *       - 挑衅 / 回复封锁 / 再来一次 / 无理取闹 / 定身法
 *       - 哈欠 / 喉斩
 *   13. 场地效果递减 / Field Effect Decrements:
 *       - 所有 weather/terrain/screen/tailwind/room 持续回合
 *   14. 极巨化递减 / Dynamax Decrement
 *
 * @see BattleEngine#playRound() 回合末调用
 * @see BattleFieldEffectSupport#decrementFieldEffects() 场地效果递减
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
        // Fire ON_TURN_END — 回合结束，允许特性/道具响应
        engine.getEventBus().fireEvent(BattleEventType.ON_TURN_END,
            new BattleEvent(BattleEventType.ON_TURN_END) {},
            Map.of("state", state, "currentRound", currentRound));

        // 清除"本回合换入"标记（用于 Stakeout 等特性）
        clearJustSwitchedIn(engine.team(state, true));
        clearJustSwitchedIn(engine.team(state, false));

        // 结算顺序尽量保持稳定：先状态伤害/回复，再能力与场地，再递减各种倒计时。
        // 注意：回合末状态/回复只作用于在场（active）宝可梦，后备宝可梦不承受回合末效果。
        applyEndTurnStatusEffects(state, true, events);
        applyEndTurnStatusEffects(state, false, events);
        applyEndTurnHealing(state, true, events);
        applyEndTurnHealing(state, false, events);
         applyEndTurnItemEffects(state, true, events);
         applyEndTurnItemEffects(state, false, events);
        applyCudChew(engine.team(state, true), events);
        applyCudChew(engine.team(state, false), events);
        applyEndTurnAbilityEffects(state, true, events, random);
        applyEndTurnAbilityEffects(state, false, events, random);
        applyEndTurnFieldEffects(state, events);
        decrementDynamax(engine.team(state, true), events);
        decrementDynamax(engine.team(state, false), events);
        applyVolatileEndTurnEffects(state, true, events);
        applyVolatileEndTurnEffects(state, false, events);
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
        decrementThroatChopEffects(engine.team(state, true), events);
        decrementThroatChopEffects(engine.team(state, false), events);
        fieldEffectSupport.decrementFieldEffects(state, fieldSnapshot, events);
    }

    void clearFlinch(Map<String, Object> state) {
        clearFlinch(engine.team(state, true));
        clearFlinch(engine.team(state, false));
    }

    void clearEndured(Map<String, Object> state) {
        for (Map<String, Object> mon : engine.team(state, true)) {
            engine.setVolatile(mon, "endured", false);
        }
        for (Map<String, Object> mon : engine.team(state, false)) {
            engine.setVolatile(mon, "endured", false);
        }
    }

    void clearDestinyBond(Map<String, Object> state) {
        for (Map<String, Object> mon : engine.team(state, true)) {
            engine.setVolatile(mon, "destinyBond", false);
        }
        for (Map<String, Object> mon : engine.team(state, false)) {
            engine.setVolatile(mon, "destinyBond", false);
        }
    }

    private void clearJustSwitchedIn(List<Map<String, Object>> team) {
        for (Map<String, Object> mon : team) {
            mon.remove("justSwitchedIn");
        }
    }

    private void applyEndTurnFieldEffects(Map<String, Object> state, List<String> events) {
        // 沙暴伤害 (Sandstorm) — Gen 9 只有沙暴造成天气伤害
        if (fieldEffectSupport.sandTurns(state) > 0) {
            applyWeatherDamage(state, true, "sand", events);
            applyWeatherDamage(state, false, "sand", events);
        }
        // 雪天（Gen 9 Snow/Snowscape）不造成任何伤害，仅保留冰鳞粉（Ice Face）再生等效果
        if (fieldEffectSupport.snowTurns(state) > 0) {
            // 冰鳞粉（Ice Face）：雪天时重新生成冰鳞粉
            for (boolean ifSide : new boolean[]{true, false}) {
                for (Integer ifSlot : engine.activeSlots(state, ifSide)) {
                    if (ifSlot == null || ifSlot < 0 || ifSlot >= engine.team(state, ifSide).size()) continue;
                    Map<String, Object> ifMon = engine.team(state, ifSide).get(ifSlot);
                    if (engine.toInt(ifMon.get("currentHp"), 0) <= 0) continue;
                    String ifAb = engine.abilityName(ifMon);
                    if ("ice-face".equalsIgnoreCase(ifAb) || "ice face".equalsIgnoreCase(ifAb)) {
                        ifMon.put("iceFaceActive", true);
                        ifMon.put("icefaceReformed", true);
                        events.add(ifMon.get("name") + " 的冰鳞粉在雪天中重新恢复了！");
                    }
                }
            }
        }
        // 青草场地回血 (Grassy Terrain)
        if (fieldEffectSupport.grassyTerrainTurns(state) > 0) {
            applyGrassyTerrainHealing(state, true, events);
            applyGrassyTerrainHealing(state, false, events);
        }
        // G-Max 持续伤害
        applyGMaxPersistentDamage(state, true, events);
        applyGMaxPersistentDamage(state, false, events);

        // Future Sight / Doom Desire 延迟攻击结算
        fieldEffectSupport.processFutureSightDamage(state, true, events);
        fieldEffectSupport.processFutureSightDamage(state, false, events);
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

    private void applyGMaxPersistentDamage(Map<String, Object> state, boolean playerSide, List<String> events) {
        // 使用分 side 的 key 读取 G-Max 持续伤害回合数
        int wildfireTurns = fieldEffectSupport.gmaxWildfireTurns(state, playerSide);
        int cannonadeTurns = fieldEffectSupport.gmaxCannonadeTurns(state, playerSide);
        int vineLashTurns = fieldEffectSupport.gmaxVineLashTurns(state, playerSide);

        if (wildfireTurns > 0) {
            fieldEffectSupport.fieldEffects(state).put(playerSide ? "playerGmaxWildfireTurns" : "opponentGmaxWildfireTurns", wildfireTurns - 1);
            applyGmaxDamageToSide(state, playerSide, DamageCalculatorUtil.TYPE_FIRE, "烈火燎原", events);
        }
        if (cannonadeTurns > 0) {
            fieldEffectSupport.fieldEffects(state).put(playerSide ? "playerGmaxCannonadeTurns" : "opponentGmaxCannonadeTurns", cannonadeTurns - 1);
            applyGmaxDamageToSide(state, playerSide, DamageCalculatorUtil.TYPE_WATER, "水炮轰射", events);
        }
        if (vineLashTurns > 0) {
            fieldEffectSupport.fieldEffects(state).put(playerSide ? "playerGmaxVineLashTurns" : "opponentGmaxVineLashTurns", vineLashTurns - 1);
            applyGmaxDamageToSide(state, playerSide, DamageCalculatorUtil.TYPE_GRASS, "藤蔓鞭打", events);
        }
    }

    private void applyGmaxDamageToSide(Map<String, Object> state, boolean playerSide, int immuneTypeId, String effectName, List<String> events) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0) continue;
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) continue;
            if (engine.targetHasType(mon, immuneTypeId)) continue;
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int dmg = Math.max(1, maxHp / 6);
            int cur = engine.toInt(mon.get("currentHp"), 0);
            mon.put("currentHp", Math.max(0, cur - dmg));
            events.add(mon.get("name") + " 受到 G-Max " + effectName + "影响，损失了 " + dmg + " 点 HP");
            if (cur - dmg <= 0) {
                mon.put("status", "fainted");
                events.add(mon.get("name") + " 倒下了");
            }
        }
    }

    private int typeIdForName(String name) {
        return switch (name) {
            case "fire" -> DamageCalculatorUtil.TYPE_FIRE;
            case "water" -> DamageCalculatorUtil.TYPE_WATER;
            case "grass" -> DamageCalculatorUtil.TYPE_GRASS;
            default -> 0;
        };
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

    private void decrementThroatChopEffects(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            int before = engine.toInt(engine.volatileValue(mon, "throatChopTurns", 0), 0);
            if (before <= 0) continue;
            int after = Math.max(0, before - 1);
            engine.setVolatile(mon, "throatChopTurns", after);
            if (after == 0 && engine.toInt(mon.get("currentHp"), 0) > 0) {
                events.add(mon.get("name") + " 的喉斩效果消失了，可以重新使用声音类招式");
            }
        }
    }

    private void applyEndTurnStatusEffects(Map<String, Object> state, boolean playerSide, List<String> events) {
        List<Map<String, Object>> team = engine.team(state, playerSide);
        // 只结算在场（active）宝可梦：后备宝可梦不承受回合末状态伤害
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team.size()) {
                continue;
            }
            Map<String, Object> mon = team.get(slot);
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
                // Magic Guard 免疫状态伤害，但 toxicCounter 已在上方递增，此处无需再次递增
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

    private void applyEndTurnHealing(Map<String, Object> state, boolean playerSide, List<String> events) {
        List<Map<String, Object>> team = engine.team(state, playerSide);
        // 只结算在场（active）宝可梦：后备宝可梦不吃剩饭等回合末回复
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team.size()) {
                continue;
            }
            Map<String, Object> mon = team.get(slot);
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

    private void applyVolatileEndTurnEffects(Map<String, Object> state, boolean playerSide, List<String> events) {
        List<Map<String, Object>> team = engine.team(state, playerSide);
        // 只结算在场（active）宝可梦：寄生种子/束缚/灭亡之歌等 volatile 伤害不作用于后备
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= team.size()) {
                continue;
            }
            Map<String, Object> mon = team.get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            // 寄生种子：吸取 1/8 最大 HP，对手回复等量 HP
            if (Boolean.TRUE.equals(mon.get("leechSeed"))) {
                applyLeechSeedDamage(state, playerSide, mon, events);
            }
            // 束缚招式（火焰旋涡/潮旋/绑紧等）：每回合伤害，且递减持续回合
            if (Boolean.TRUE.equals(mon.get("bound"))) {
                int divisor = engine.toInt(engine.volatileValue(mon, "boundDivisor", 8), 8);
                applyFractionalDamage(mon, divisor, events, "因束缚招式损失了");
                // 递减束缚回合数，归零时解除
                int boundTurns = engine.toInt(engine.volatileValue(mon, "boundTurns", mon.get("boundTurns")), 0);
                if (boundTurns > 0) {
                    boundTurns--;
                    engine.setVolatile(mon, "boundTurns", boundTurns);
                    if (boundTurns <= 0) {
                        engine.setVolatile(mon, "bound", false);
                        if (engine.toInt(mon.get("currentHp"), 0) > 0) {
                            events.add(mon.get("name") + " 摆脱了束缚");
                        }
                    }
                }
            }
            // 灭亡之歌：倒计时归零时直接倒下
            int perishTurns = engine.toInt(engine.volatileValue(mon, "perishSongTurns", 0), 0);
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
            // 恶梦：睡眠中每回合损失 1/8 最大 HP
            if (Boolean.TRUE.equals(engine.volatileValue(mon, "nightmare", false))
                    && "sleep".equals(String.valueOf(mon.get("condition")))) {
                int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
                int dmg = Math.max(1, maxHp / 8);
                int curHp = engine.toInt(mon.get("currentHp"), 0);
                mon.put("currentHp", Math.max(0, curHp - dmg));
                events.add(mon.get("name") + " 受到恶梦影响，损失了 " + dmg + " 点 HP");
                if (curHp - dmg <= 0) {
                    mon.put("status", "fainted");
                    events.add(mon.get("name") + " 被恶梦带走了");
                }
            }
            // 八爪束缚：每回合降低防御和特防 1 级
            if (engine.toInt(engine.volatileValue(mon, "octolockTurns", 0), 0) > 0) {
                java.util.Map<String, Object> stages = engine.castMap(mon.get("statStages"));
                int curDef = engine.toInt(stages.get("defense"), 0);
                int curSpD = engine.toInt(stages.get("specialDefense"), 0);
                if (curDef > -6) {
                    stages.put("defense", curDef - 1);
                    events.add(mon.get("name") + " 被八爪束缚压制，防御下降了！");
                }
                if (curSpD > -6) {
                    stages.put("specialDefense", curSpD - 1);
                    events.add(mon.get("name") + " 被八爪束缚压制，特防下降了！");
                }
            }
            // 盐腌（Salt Cure）：每回合损失 1/8 HP，钢/水系损失 1/4
            if (Boolean.TRUE.equals(engine.volatileValue(mon, "saltCured", false))) {
                int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
                int divisor = (engine.targetHasType(mon, DamageCalculatorUtil.TYPE_STEEL)
                        || engine.targetHasType(mon, DamageCalculatorUtil.TYPE_WATER)) ? 4 : 8;
                int saltDmg = Math.max(1, maxHp / divisor);
                int curHp = engine.toInt(mon.get("currentHp"), 0);
                mon.put("currentHp", Math.max(0, curHp - saltDmg));
                events.add(mon.get("name") + " 受到盐腌影响，损失了 " + saltDmg + " 点 HP");
                if (curHp - saltDmg <= 0) { mon.put("status", "fainted"); events.add(mon.get("name") + " 倒下了"); }
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

    private void applyLeechSeedDamage(Map<String, Object> state, boolean playerSide, Map<String, Object> mon, List<String> events) {
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

        // 寄生种子回血：找到种子来源并回复等量 HP
        Object sourceObj = engine.volatileValue(mon, "leechSeedSource", null);
        if (sourceObj instanceof Map<?, ?> sourceMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) sourceMap;
            int sourceHp = engine.toInt(source.get("currentHp"), 0);
            if (sourceHp > 0) {
                int sourceMaxHp = engine.toInt(engine.castMap(source.get("stats")).get("hp"), 1);
                source.put("currentHp", Math.min(sourceMaxHp, sourceHp + actualDamage));
                events.add(source.get("name") + " 通过寄生种子回复了 " + actualDamage + " 点 HP");
            }
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

    /** 回合末按最大 HP 的 1/denominator 对单只宝可梦造成伤害（用于干旱肌肤/太阳之力等天气损耗） */
    private void applyWeatherDamageToMon(Map<String, Object> state, Map<String, Object> mon, int denominator,
            List<String> events, String msg) {
        int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
        int curHp = engine.toInt(mon.get("currentHp"), 0);
        if (curHp <= 0) {
            return;
        }
        int dmg = Math.max(1, maxHp / denominator);
        int remaining = Math.max(0, curHp - dmg);
        mon.put("currentHp", remaining);
        events.add(mon.get("name") + msg + " " + dmg + " 点 HP");
        if (remaining <= 0) {
            mon.put("status", "fainted");
            events.add(mon.get("name") + " 倒下了");
        }
    }

    /** Cud Chew: 回合末重新触发树果效果 */
    private void applyCudChew(List<Map<String, Object>> team, List<String> events) {
        for (Map<String, Object> mon : team) {
            if (!Boolean.TRUE.equals(mon.get("cudChewPending"))) continue;
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) continue;
            mon.put("cudChewPending", false);
            int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
            int curHp = engine.toInt(mon.get("currentHp"), 0);
            if (curHp > 0 && curHp < maxHp) {
                int heal = Math.max(1, maxHp / 4);
                mon.put("currentHp", Math.min(maxHp, curHp + heal));
                events.add(mon.get("name") + " 的反刍特性发动，树果效果再次触发，回复了 " + heal + " 点 HP！");
            }
        }
    }

    /**
     * 回合末道具效果：火焰宝珠/剧毒宝珠/黑色污泥
     */
    private void applyEndTurnItemEffects(Map<String, Object> state, boolean playerSide, List<String> events) {
        List<Map<String, Object>> team = engine.team(state, playerSide);
        List<Integer> activeSlots = engine.activeSlots(state, playerSide);
        for (Integer slotIdx : activeSlots) {
            if (slotIdx == null || slotIdx < 0 || slotIdx >= team.size()) continue;
            Map<String, Object> mon = team.get(slotIdx);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) continue;
            String item = engine.heldItem(mon);
            if (item.isBlank()) continue;

            // 火焰宝珠：回合末有 100% 概率灼伤
            if ("flame-orb".equalsIgnoreCase(item) || "flame orb".equalsIgnoreCase(item)) {
                if (mon.get("condition") == null || String.valueOf(mon.get("condition")).isBlank()) {
                    mon.put("condition", "burn");
                    events.add(mon.get("name") + " 被火焰宝珠灼伤了");
                }
                continue;
            }
            // 剧毒宝珠：回合末有 100% 概率陷入剧毒
            if ("toxic-orb".equalsIgnoreCase(item) || "toxic orb".equalsIgnoreCase(item)) {
                if (mon.get("condition") == null || String.valueOf(mon.get("condition")).isBlank()) {
                    mon.put("condition", "toxic");
                    mon.put("toxicCounter", 1);
                    events.add(mon.get("name") + " 被剧毒宝珠感染了剧毒");
                }
                continue;
            }
            // 黑色污泥：毒属性回复 1/16 HP，非毒属性损失 1/8 HP
            if ("black-sludge".equalsIgnoreCase(item) || "black sludge".equalsIgnoreCase(item)) {
                int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
                boolean isPoison = engine.targetHasType(mon, DamageCalculatorUtil.TYPE_POISON);
                if (isPoison) {
                    int heal = Math.max(1, maxHp / 16);
                    int curHp = engine.toInt(mon.get("currentHp"), 0);
                    mon.put("currentHp", Math.min(maxHp, curHp + heal));
                    events.add(mon.get("name") + " 通过黑色污泥回复了 " + heal + " 点 HP");
                } else {
                    int dmg = Math.max(1, maxHp / 8);
                    int curHp = engine.toInt(mon.get("currentHp"), 0);
                    int remaining = Math.max(0, curHp - dmg);
                    mon.put("currentHp", remaining);
                    events.add(mon.get("name") + " 受到黑色污泥影响损失了 " + dmg + " 点 HP");
                    if (remaining <= 0) {
                        mon.put("status", "fainted");
                        events.add(mon.get("name") + " 因黑色污泥倒下了");
                    }
                }
                continue;
            }
            // 毒针：回合末损失 1/8 最大 HP
            if ("sticky-barb".equalsIgnoreCase(item) || "sticky barb".equalsIgnoreCase(item)) {
                int maxHp = engine.toInt(engine.castMap(mon.get("stats")).get("hp"), 1);
                int dmg = Math.max(1, maxHp / 8);
                int curHp = engine.toInt(mon.get("currentHp"), 0);
                int remaining = Math.max(0, curHp - dmg);
                mon.put("currentHp", remaining);
                events.add(mon.get("name") + " 受到毒针伤害，损失了 " + dmg + " 点 HP");
                if (remaining <= 0) {
                    mon.put("status", "fainted");
                    events.add(mon.get("name") + " 因毒针倒下了");
                }
                continue;
            }
            // 状态回复树果：回合末检查并消费对应的树果治愈异常状态
            conditionSupport.tryConsumeStatusBerry(mon, events);
        }
    }

    private void applyEndTurnAbilityEffects(Map<String, Object> state, boolean playerSide, List<String> events,
                                            Random random) {
        for (Integer slot : engine.activeSlots(state, playerSide)) {
            if (slot == null || slot < 0 || slot >= engine.team(state, playerSide).size()) {
                continue;
            }
            Map<String, Object> mon = engine.team(state, playerSide).get(slot);
            if (engine.toInt(mon.get("currentHp"), 0) <= 0) {
                continue;
            }
            String ability = engine.abilityName(mon);
            // Hunger Switch (饱了又饿): 莫鲁贝可每回合切换形态
            if ("hunger-switch".equalsIgnoreCase(ability) || "hunger switch".equalsIgnoreCase(ability)) {
                boolean hangry = Boolean.TRUE.equals(engine.volatileValue(mon, "hangryMode", false));
                engine.setVolatile(mon, "hangryMode", !hangry);
                events.add(mon.get("name") + (hangry ? " 恢复成了满腹花纹" : " 变成了空腹花纹！"));
            }
            if ("speed-boost".equalsIgnoreCase(ability) || "speed boost".equalsIgnoreCase(ability)) {
                applySpeedBoost(mon, events);
            }
            if ("moody".equalsIgnoreCase(ability)) {
                applyModyBoost(mon, events, random);
            }
            // 恶梦：回合末对睡眠状态的对手造成 1/8 伤害
            if ("bad-dreams".equalsIgnoreCase(ability) || "bad dreams".equalsIgnoreCase(ability)) {
                for (Integer oppSlot : engine.activeSlots(state, !playerSide)) {
                    if (oppSlot == null || oppSlot < 0 || oppSlot >= engine.team(state, !playerSide).size()) continue;
                    Map<String, Object> opp = engine.team(state, !playerSide).get(oppSlot);
                    if (engine.toInt(opp.get("currentHp"), 0) <= 0) continue;
                    if ("sleep".equals(String.valueOf(opp.get("condition")))) {
                        int maxHp = engine.toInt(engine.castMap(opp.get("stats")).get("hp"), 1);
                        int dmg = Math.max(1, maxHp / 8);
                        int curHp = engine.toInt(opp.get("currentHp"), 0);
                        int remaining = Math.max(0, curHp - dmg);
                        opp.put("currentHp", remaining);
                        events.add(opp.get("name") + " 受到" + mon.get("name") + "的恶梦影响，损失了 " + dmg + " 点 HP");
                        if (remaining <= 0) opp.put("status", "fainted");
                    }
                }
            }

            // 雨天/雪天回合末回复
            Map<String, Object> fieldEffects = engine.castMap(state.get("fieldEffects"));
            int rainTurns = engine.toInt(fieldEffects.get("rainTurns"), 0);
            int snowTurns = engine.toInt(fieldEffects.get("snowTurns"), 0);
            int sunTurnsNow = engine.toInt(fieldEffects.get("sunTurns"), 0);
            if (("rain-dish".equalsIgnoreCase(ability) || "rain dish".equalsIgnoreCase(ability))
                    && rainTurns > 0 && engine.healBlockTurns(mon) <= 0) {
                applyFractionalHeal(mon, 16, events, "的雨盘回复了");
            }
            if (("ice-body".equalsIgnoreCase(ability) || "ice body".equalsIgnoreCase(ability))
                    && snowTurns > 0 && engine.healBlockTurns(mon) <= 0) {
                applyFractionalHeal(mon, 16, events, "的冰鳞粉回复了");
            }
            // 干燥肌肤：雨天回复 1/8、晴天损失 1/8 最大 HP（受回复封锁/魔法防守约束）
            if ("dry-skin".equalsIgnoreCase(ability) || "dry skin".equalsIgnoreCase(ability)) {
                if (rainTurns > 0 && engine.healBlockTurns(mon) <= 0) {
                    applyFractionalHeal(mon, 8, events, "的干燥肌肤在雨中回复了");
                } else if (sunTurnsNow > 0 && !engine.isMagicGuard(mon)) {
                    applyWeatherDamageToMon(state, mon, 8, events, "的干燥肌肤受到晴天影响损失了");
                }
            }
            // 太阳之力：晴天每回合损失 1/8 最大 HP（魔法防守免疫）
            if (("solar-power".equalsIgnoreCase(ability) || "solar power".equalsIgnoreCase(ability))
                    && sunTurnsNow > 0 && !engine.isMagicGuard(mon)) {
                applyWeatherDamageToMon(state, mon, 8, events, "的太阳之力受到晴天影响损失了");
            }
            // 毒疗：中毒/剧毒时每回合回复 1/8 HP
            if ("poison-heal".equalsIgnoreCase(ability) || "poison heal".equalsIgnoreCase(ability)) {
                String condition = String.valueOf(mon.getOrDefault("condition", ""));
                if (("poison".equals(condition) || "toxic".equals(condition)) && engine.healBlockTurns(mon) <= 0) {
                    applyFractionalHeal(mon, 8, events, "的毒疗特性回复了");
                }
            }
            // 湿润身躯：雨天解除异常状态
            if ("hydration".equalsIgnoreCase(ability) && rainTurns > 0) {
                if (mon.get("condition") != null && !String.valueOf(mon.get("condition")).isBlank()) {
                    mon.put("condition", null);
                    events.add(mon.get("name") + " 的湿润身躯特性回复了异常状态");
                }
            }
            // 蜕皮：30% 概率解除异常状态
            if ("shed-skin".equalsIgnoreCase(ability) || "shed skin".equalsIgnoreCase(ability)) {
                if (mon.get("condition") != null && !String.valueOf(mon.get("condition")).isBlank()
                        && random.nextInt(100) < 30) {
                    mon.put("condition", null);
                    events.add(mon.get("name") + " 的蜕皮特性回复了异常状态");
                }
            }
            // 治愈之心：30% 概率治愈队友异常状态
            if ("healer".equalsIgnoreCase(ability) && random.nextInt(100) < 30) {
                for (Integer allySlot : engine.activeSlots(state, playerSide)) {
                    if (allySlot == null || allySlot == slot) continue;
                    Map<String, Object> ally = engine.team(state, playerSide).get(allySlot);
                    if (engine.toInt(ally.get("currentHp"), 0) > 0
                            && ally.get("condition") != null && !String.valueOf(ally.get("condition")).isBlank()) {
                        ally.put("condition", null);
                        events.add(mon.get("name") + " 的治愈之心特性回复了 " + ally.get("name") + " 的异常状态");
                        break;
                    }
                }
            }
            // 收获：50% 概率回收树果（晴天 100%）
            if ("harvest".equalsIgnoreCase(ability)) {
                int sunTurns = engine.toInt(fieldEffects.get("sunTurns"), 0);
                boolean shouldHarvest = sunTurns > 0 || random.nextBoolean();
                if (shouldHarvest) {
                    // 检查 mon 是否消耗了树果
                    String consumedItem = String.valueOf(mon.getOrDefault("consumedItem", ""));
                    if (!consumedItem.isBlank() && (consumedItem.endsWith("-berry") || consumedItem.endsWith(" berry"))) {
                        mon.put("heldItem", consumedItem);
                        mon.put("consumedItem", null);
                        events.add(mon.get("name") + " 的收获特性回收了 " + consumedItem);
                    }
                }
            }
        }
    }

    private void applyModyBoost(Map<String, Object> mon, List<String> events, Random random) {
        String[] stats = {"attack", "defense", "specialAttack", "specialDefense", "speed"};
        // Randomly pick one to boost by 2
        int boostIdx = random.nextInt(stats.length);
        int dropIdx = random.nextInt(stats.length - 1);
        if (dropIdx >= boostIdx) dropIdx++;
        Map<String, Object> stages = engine.castMap(mon.get("statStages"));
        int curBoost = engine.toInt(stages.get(stats[boostIdx]), 0);
        stages.put(stats[boostIdx], Math.min(6, curBoost + 2));
        int curDrop = engine.toInt(stages.get(stats[dropIdx]), 0);
        stages.put(stats[dropIdx], Math.max(-6, curDrop - 1));
        events.add(mon.get("name") + " 的随手变发动了，" + stats[boostIdx] + " 大幅提升，" + stats[dropIdx] + " 降低");
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
