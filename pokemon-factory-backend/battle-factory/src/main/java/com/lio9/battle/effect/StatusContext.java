package com.lio9.battle.effect;

import java.util.Map;

/** 状态免疫检查上下文 */
public class StatusContext {
    public final Map<String, Object> target;
    public final String condition; // "burn", "paralysis", "sleep", "freeze", "poison", "confusion"
    public final Map<String, Object> state;

    public StatusContext(Map<String, Object> target, String condition, Map<String, Object> state) {
        this.target = target;
        this.condition = condition;
        this.state = state;
    }
}
