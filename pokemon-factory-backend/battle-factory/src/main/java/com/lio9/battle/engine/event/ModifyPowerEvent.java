package com.lio9.battle.engine.event;

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
