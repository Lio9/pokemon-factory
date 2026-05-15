package com.lio9.battle.effect;

import java.util.Map;

/** 属性免疫检查上下文 */
public class ImmunityContext {
    public final Map<String, Object> target;
    public final int moveTypeId;
    public final Map<String, Object> move;

    public ImmunityContext(Map<String, Object> target, int moveTypeId, Map<String, Object> move) {
        this.target = target;
        this.moveTypeId = moveTypeId;
        this.move = move;
    }
}
