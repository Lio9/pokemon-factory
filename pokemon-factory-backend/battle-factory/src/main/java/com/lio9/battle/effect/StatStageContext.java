package com.lio9.battle.effect;

import java.util.List;
import java.util.Map;

/** 能力阶级变化上下文 */
public class StatStageContext {
    public final Map<String, Object> target;
    public final int statId;
    public final int delta;
    public final String trigger; // 触发源描述（威吓/招式名/特性名等）

    public StatStageContext(Map<String, Object> target, int statId, int delta, String trigger) {
        this.target = target;
        this.statId = statId;
        this.delta = delta;
        this.trigger = trigger;
    }
}
