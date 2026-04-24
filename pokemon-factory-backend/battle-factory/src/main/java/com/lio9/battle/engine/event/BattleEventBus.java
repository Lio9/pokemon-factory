package com.lio9.battle.engine.event;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 战斗事件总线
 * 管理所有事件处理器的注册和触发
 */
@Component
public class BattleEventBus {
    private final Map<BattleEventType, List<BattleEventHandler>> handlers = new HashMap<>();
    private boolean frozen = false;

    /**
     * 注册事件处理器
     */
    public void registerHandler(BattleEventType type, BattleEventHandler handler) {
        if (frozen) {
            throw new IllegalStateException("Cannot register handlers after event bus is frozen");
        }
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 批量注册处理器
     */
    public void registerHandlers(Map<BattleEventType, List<BattleEventHandler>> handlerMap) {
        for (Map.Entry<BattleEventType, List<BattleEventHandler>> entry : handlerMap.entrySet()) {
            for (BattleEventHandler handler : entry.getValue()) {
                registerHandler(entry.getKey(), handler);
            }
        }
    }

    /**
     * 冻结事件总线（防止后续修改）
     */
    public void freeze() {
        // 对所有处理器列表按优先级排序
        for (List<BattleEventHandler> handlerList : handlers.values()) {
            handlerList.sort(Comparator.comparingInt(BattleEventHandler::getPriority).reversed());
        }
        frozen = true;
    }

    /**
     * 触发事件
     * @return 最终的事件结果
     */
    public EventResult fireEvent(BattleEventType type, BattleEvent event, Map<String, Object> context) {
        List<BattleEventHandler> typeHandlers = handlers.getOrDefault(type, Collections.emptyList());

        EventResult result = EventResult.CONTINUE;
        for (BattleEventHandler handler : typeHandlers) {
            if (!handler.appliesTo(event, context)) {
                continue;
            }

            result = handler.handle(event, context);
            if (!result.shouldContinue()) {
                return result; // 中断传播
            }
        }

        return result;
    }

    /**
     * 获取某类型事件的处理器数量
     */
    public int getHandlerCount(BattleEventType type) {
        return handlers.getOrDefault(type, Collections.emptyList()).size();
    }

    /**
     * 清除所有注册的处理器
     */
    public void clear() {
        handlers.clear();
        frozen = false;
    }

    /**
     * 获取所有已注册的事件类型
     */
    public Set<BattleEventType> getRegisteredEventTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        return handlers.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue().size() + " handlers")
            .collect(Collectors.joining(", "));
    }
}
