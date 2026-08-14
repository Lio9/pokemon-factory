package com.lio9.battle.effect;

import java.util.List;
import java.util.Map;

/**
 * 攻击上下文中用到的只读数据快照。
 * handler 只能读不能写，引擎通过返回值应用变更。
 */
public class AttackContext {
    public final Map<String, Object> attacker;
    public final Map<String, Object> defender;
    public final Map<String, Object> move;
    public final Map<String, Object> state;
    public final int moveTypeId;
    public final int damageClassId;
    public final boolean criticalHit;
    /** 类型克制倍率（0/0.5/1/2/4），供 Tinted Lens / Filter 等依赖克制的特性使用 */
    public double typeModifier = 1.0;

    public AttackContext(Map<String, Object> attacker, Map<String, Object> defender,
                        Map<String, Object> move, Map<String, Object> state,
                        int moveTypeId, int damageClassId, boolean criticalHit) {
        this.attacker = attacker;
        this.defender = defender;
        this.move = move;
        this.state = state;
        this.moveTypeId = moveTypeId;
        this.damageClassId = damageClassId;
        this.criticalHit = criticalHit;
    }

    /** 攻击方特性名（小写） */
    public String attackerAbility() { return str(attacker, "abilityName"); }
    /** 防御方特性名（小写） */
    public String defenderAbility() { return str(defender, "abilityName"); }
    /** 攻击方道具名（小写） */
    public String attackerItem() { return str(attacker, "heldItem"); }
    /** 防御方道具名（小写） */
    public String defenderItem() { return str(defender, "heldItem"); }
    /** 攻击方状态（burn/paralysis/poison等） */
    public String attackerCondition() { return str(attacker, "condition"); }
    /** 防御方状态 */
    public String defenderCondition() { return str(defender, "condition"); }
    /** 攻击方是否太晶化 */
    public boolean attackerTera() { return bool(attacker, "terastallized"); }
    /** 防御方是否太晶化 */
    public boolean defenderTera() { return bool(defender, "terastallized"); }
    /** 攻击方是否极巨化 */
    public boolean attackerDynamax() { return bool(attacker, "dynamaxed"); }
    /** 攻击方当前 HP */
    public int attackerHp() { return val(attacker, "currentHp"); }
    /** 防御方当前 HP */
    public int defenderHp() { return val(defender, "currentHp"); }
    /** 攻击方最大 HP */
    public int attackerMaxHp() { return maxHp(attacker); }
    /** 防御方最大 HP */
    public int defenderMaxHp() { return maxHp(defender); }

    /** 招式威力 */
    public int movePower() { return val(move, "power"); }

    /** 检查招式属性是否匹配 */
    public boolean moveTypeIs(int... typeIds) {
        for (int t : typeIds) if (moveTypeId == t) return true;
        return false;
    }

    /** 检查特性持有者是否在场地某一侧 */
    public boolean isPlayerSide(Map<String, Object> mon) {
        return isOnSide(true, mon);
    }

    @SuppressWarnings("unchecked")
    public boolean isOnSide(boolean player, Map<String, Object> mon) {
        List<Map<String, Object>> team = (List<Map<String, Object>>) state.get(player ? "playerTeam" : "opponentTeam");
        return team != null && team.contains(mon);
    }

    /** 防御方有效属性列表 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> defenderTypes() {
        Object types = defender.get("types");
        if (types instanceof List) return (List<Map<String, Object>>) types;
        return List.of();
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v).toLowerCase();
    }

    private static boolean bool(Map<String, Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    private static int val(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private static int maxHp(Map<String, Object> m) {
        Map<String, Object> stats = (Map<String, Object>) m.get("stats");
        return stats == null ? 1 : val(stats, "hp");
    }
}
