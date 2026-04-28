package com.lio9.battle.engine.event;



/**
 * BattleEvent 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战事件机制文件。
 * 核心职责：负责抽象对战事件、事件总线或事件处理契约。
 * 阅读建议：建议结合 BattleEngine 主流程理解触发时机。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.Map;

/**
 * 战斗事件基类
 * 所有具体事件都应继承此类
 */
public abstract class BattleEvent {
    private final BattleEventType type;
    private final long timestamp;

    protected BattleEvent(BattleEventType type) {
        this.type = type;
        this.timestamp = System.nanoTime();
    }

    public BattleEventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取事件的目标宝可梦
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTarget(Map<String, Object> context) {
        return (Map<String, Object>) context.get("target");
    }

    /**
     * 获取事件的来源宝可梦（攻击方）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSource(Map<String, Object> context) {
        return (Map<String, Object>) context.get("source");
    }

    /**
     * 获取事件相关的招式
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMove(Map<String, Object> context) {
        return (Map<String, Object>) context.get("move");
    }
}
