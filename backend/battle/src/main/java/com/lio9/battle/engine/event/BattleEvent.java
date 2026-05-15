package com.lio9.battle.engine.event;

import java.util.Map;

/**
 * 战斗事件基类。
 *
 * <p>所有具体事件都应继承此类，通过 {@link BattleEventBus} 分发。</p>
 */
public abstract class BattleEvent {
    private final BattleEventType type;

    protected BattleEvent(BattleEventType type) {
        this.type = type;
    }

    public BattleEventType getType() {
        return type;
    }

    /** 获取事件的目标宝可梦 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTarget(Map<String, Object> context) {
        return (Map<String, Object>) context.get("target");
    }

    /** 获取事件的来源宝可梦（攻击方） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSource(Map<String, Object> context) {
        return (Map<String, Object>) context.get("source");
    }

    /** 获取事件相关的招式 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMove(Map<String, Object> context) {
        return (Map<String, Object>) context.get("move");
    }
}
