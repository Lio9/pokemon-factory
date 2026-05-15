package com.lio9.battle.effect;

import java.util.Map;

/**
 * 道具处理器接口。
 * 和 AbilityHandler 共享 AttackContext 等上下文。
 */
public interface ItemHandler {

    /** 道具唯一标识（name_en，小写） */
    String id();

    /** 攻击方道具：修正最终伤害倍率 */
    default double onSourceModifyDamage(AttackContext ctx, double modifier) { return modifier; }

    /** 修正攻击属性值（如 Choice Band/Specs） */
    default int onSourceModifyAttackStat(AttackContext ctx, int stat) { return stat; }

    /** 修正防御属性值（如 Eviolite、Assault Vest） */
    default int onTargetModifyDefenseStat(AttackContext ctx, int stat) { return stat; }

    /** 修正速度值（如 Choice Scarf、Iron Ball） */
    default int onModifySpeed(SpeedContext ctx, int speed) { return speed; }

    /** 修正重量（如 Float Stone） */
    default int onModifyWeight(WeightContext ctx, int weight) { return weight; }

    /** 上场时触发 */
    default void onSwitchIn(SwitchInContext ctx) {}

    /** 受到伤害后触发（道具消耗类，如充电池/光苔/雪球/球根） */
    default void onDamageReceived(DamageReceivedContext ctx) {}
}
