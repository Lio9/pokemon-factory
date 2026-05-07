package com.lio9.battle.effect;

import java.util.LinkedHashMap;
import java.util.Map;

/** 接触触发上下文（被接触招式命中时） */
public class ContactContext {
    public final Map<String, Object> attacker;
    public final Map<String, Object> target;
    public final Map<String, Object> move;
    public final Map<String, Object> state;
    /** handler 可以在此写入效果描述，引擎读取后执行对应动作 */
    public final Map<String, Object> result;

    public ContactContext(Map<String, Object> attacker, Map<String, Object> target,
                          Map<String, Object> move, Map<String, Object> state) {
        this.attacker = attacker;
        this.target = target;
        this.move = move;
        this.state = state;
        this.result = new LinkedHashMap<>();
    }
}
