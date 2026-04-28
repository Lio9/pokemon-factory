package com.lio9.battle.engine.event;



/**
 * DamageEvent 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战事件机制文件。
 * 核心职责：负责抽象对战事件、事件总线或事件处理契约。
 * 阅读建议：建议结合 BattleEngine 主流程理解触发时机。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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
