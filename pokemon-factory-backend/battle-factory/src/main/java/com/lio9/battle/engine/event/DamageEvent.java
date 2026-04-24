package com.lio9.battle.engine.event;

import java.util.Map;

/**
 * 伤害事件 - 当宝可梦受到伤害时触发
 */
public class DamageEvent extends BattleEvent {
    private final int baseDamage;
    private int finalDamage;
    private final boolean isCritical;

    public DamageEvent(int baseDamage, int finalDamage, boolean isCritical) {
        super(BattleEventType.ON_DAMAGE);
        this.baseDamage = baseDamage;
        this.finalDamage = finalDamage;
        this.isCritical = isCritical;
    }

    public static DamageEvent of(int baseDamage) {
        return new DamageEvent(baseDamage, baseDamage, false);
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(int finalDamage) {
        this.finalDamage = finalDamage;
    }

    public boolean isCritical() {
        return isCritical;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTarget(Map<String, Object> context) {
        return (Map<String, Object>) context.get("defender");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSource(Map<String, Object> context) {
        return (Map<String, Object>) context.get("attacker");
    }
}
