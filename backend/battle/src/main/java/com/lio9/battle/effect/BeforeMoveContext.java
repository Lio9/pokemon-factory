package com.lio9.battle.effect;

import java.util.Map;

/** 使用招式前上下文 */
public class BeforeMoveContext {
    public final Map<String, Object> actor;
    public final Map<String, Object> move;
    public final Map<String, Object> state;
    public final boolean playerSide;

    public BeforeMoveContext(Map<String, Object> actor, Map<String, Object> move,
                             Map<String, Object> state, boolean playerSide) {
        this.actor = actor;
        this.move = move;
        this.state = state;
        this.playerSide = playerSide;
    }
}
