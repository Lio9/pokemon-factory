package com.lio9.battle.effect;

import java.util.Map;

/** 上场触发上下文 */
public class SwitchInContext {
    public final Map<String, Object> mon;
    public final Map<String, Object> state;
    public final boolean playerSide;

    public SwitchInContext(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        this.mon = mon;
        this.state = state;
        this.playerSide = playerSide;
    }
}
