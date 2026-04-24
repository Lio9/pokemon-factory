package com.lio9.battle.engine.event;

import java.util.Map;

/**
 * 战斗事件处理器接口
 * 特性和道具通过实现此接口来响应各种战斗事件
 */
@FunctionalInterface
public interface BattleEventHandler {
    /**
     * 处理事件
     * @param event 事件数据
     * @param context 战斗上下文（包含状态、双方宝可梦等信息）
     * @return 事件处理结果
     */
    EventResult handle(BattleEvent event, Map<String, Object> context);

    /**
     * 处理器优先级（数值越大越先执行）
     * 默认优先级为 0
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 处理器来源标识（用于调试）
     * 格式: "ability:xxx" 或 "item:xxx" 或 "move:xxx"
     */
    default String getSource() {
        return "unknown";
    }

    /**
     * 检查此处理器是否适用于给定的事件
     * 默认实现总是返回 true
     */
    default boolean appliesTo(BattleEvent event, Map<String, Object> context) {
        return true;
    }
}
