package com.lio9.battle.engine.event;



/**
 * ModifyPowerEvent 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战事件机制文件。
 * 核心职责：负责抽象对战事件、事件总线或事件处理契约。
 * 阅读建议：建议结合 BattleEngine 主流程理解触发时机。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 威力修正事件 - 当招式威力需要被特性或道具修正时触发
 */
public class ModifyPowerEvent extends BattleEvent {
    private final int basePower;
    private int modifiedPower;

    public ModifyPowerEvent(int basePower) {
        super(BattleEventType.ON_BEFORE_MOVE);
        this.basePower = basePower;
        this.modifiedPower = basePower;
    }

    public static ModifyPowerEvent of(int basePower) {
        return new ModifyPowerEvent(basePower);
    }

    public int getBasePower() {
        return basePower;
    }

    public int getModifiedPower() {
        return modifiedPower;
    }

    public void setModifiedPower(int modifiedPower) {
        this.modifiedPower = modifiedPower;
    }

    public double getMultiplier() {
        return basePower > 0 ? (double) modifiedPower / basePower : 1.0;
    }
}
