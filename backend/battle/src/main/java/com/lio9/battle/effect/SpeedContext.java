package com.lio9.battle.effect;

import java.util.Map;

/** 速度计算上下文 */
public class SpeedContext {
    public final Map<String, Object> mon;
    public final Map<String, Object> state;
    public final boolean playerSide;

    public SpeedContext(Map<String, Object> mon, Map<String, Object> state, boolean playerSide) {
        this.mon = mon;
        this.state = state;
        this.playerSide = playerSide;
    }
}
