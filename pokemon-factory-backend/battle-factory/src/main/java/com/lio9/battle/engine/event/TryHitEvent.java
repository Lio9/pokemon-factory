package com.lio9.battle.engine.event;



/**
 * 招式命中事件 - 当招式尝试命中目标时触发
 */
public class TryHitEvent extends BattleEvent {
    private final boolean isImmune;

    public TryHitEvent(boolean isImmune) {
        super(BattleEventType.ON_HIT);
        this.isImmune = isImmune;
    }

    public static TryHitEvent normal() {
        return new TryHitEvent(false);
    }

    public static TryHitEvent immune() {
        return new TryHitEvent(true);
    }

    public boolean isImmune() {
        return isImmune;
    }
}
