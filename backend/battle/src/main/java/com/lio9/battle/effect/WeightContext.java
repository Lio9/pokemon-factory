package com.lio9.battle.effect;

import java.util.Map;

/** 重量修正上下文 */
public class WeightContext {
    public final Map<String, Object> mon;

    public WeightContext(Map<String, Object> mon) {
        this.mon = mon;
    }
}
