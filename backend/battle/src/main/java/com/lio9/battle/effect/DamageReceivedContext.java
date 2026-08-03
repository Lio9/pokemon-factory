package com.lio9.battle.effect;

import java.util.LinkedHashMap;
import java.util.Map;

/** 受到伤害后触发上下文（用于 applyReactiveDamageAbilities 迁移） */
public class DamageReceivedContext {
    public final Map<String, Object> state;
    public final Map<String, Object> attacker;
    public final Map<String, Object> target;
    public final Map<String, Object> move;
    public final int hpBeforeDamage;
    public final int hpAfterDamage;
    public final int actualDamage;
    public final int moveTypeId;
    public final int damageClassId;
    public final int maxHp;
    public final boolean criticalHit;

    /** handler 写入动作标记，引擎读取后执行对应效果 */
    public final Map<String, Object> result = new LinkedHashMap<>();

    public DamageReceivedContext(Map<String, Object> state,
                                 Map<String, Object> attacker,
                                 Map<String, Object> target,
                                 Map<String, Object> move,
                                 int hpBeforeDamage, int hpAfterDamage, int actualDamage,
                                 int moveTypeId, int damageClassId, int maxHp, boolean criticalHit) {
        this.state = state;
        this.attacker = attacker;
        this.target = target;
        this.move = move;
        this.hpBeforeDamage = hpBeforeDamage;
        this.hpAfterDamage = hpAfterDamage;
        this.actualDamage = actualDamage;
        this.moveTypeId = moveTypeId;
        this.damageClassId = damageClassId;
        this.maxHp = maxHp;
        this.criticalHit = criticalHit;
    }
}
