package com.lio9.battle.engine.event;



/**
 * EventResult 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战事件机制文件。
 * 核心职责：负责抽象对战事件、事件总线或事件处理契约。
 * 阅读建议：建议结合 BattleEngine 主流程理解触发时机。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 事件处理结果
 * 用于控制事件传播和传递修改后的值
 */
public class EventResult {
    /** 继续事件传播 */
    public static final EventResult CONTINUE = new EventResult(true, null, null);
    /** 停止事件传播（无副作用） */
    public static final EventResult STOP = new EventResult(false, null, null);
    /** 免疫/无效 */
    public static final EventResult IMMUNE = new EventResult(false, null, "immune");

    private final boolean shouldContinue;
    private final Object modifiedValue;
    private final String effectType;
    private final String message;

    private EventResult(boolean shouldContinue, Object modifiedValue, String effectType) {
        this.shouldContinue = shouldContinue;
        this.modifiedValue = modifiedValue;
        this.effectType = effectType;
        this.message = null;
    }

    private EventResult(boolean shouldContinue, Object modifiedValue, String effectType, String message) {
        this.shouldContinue = shouldContinue;
        this.modifiedValue = modifiedValue;
        this.effectType = effectType;
        this.message = message;
    }

    /**
     * 创建继续传播的结果
     */
    public static EventResult continueWith(Object modifiedValue) {
        return new EventResult(true, modifiedValue, null);
    }

    /**
     * 创建停止传播的结果
     */
    public static EventResult stopWith(String effectType) {
        return new EventResult(false, null, effectType);
    }

    /**
     * 创建带消息的停止结果
     */
    public static EventResult stopWithMessage(String effectType, String message) {
        return new EventResult(false, null, effectType, message);
    }

    /**
     * 创建修改值并继续的结果
     */
    public static EventResult modifyAndContinue(Object modifiedValue) {
        return new EventResult(true, modifiedValue, "modified");
    }

    public boolean shouldContinue() {
        return shouldContinue;
    }

    @SuppressWarnings("unchecked")
    public <T> T getModifiedValue() {
        return (T) modifiedValue;
    }

    public String getEffectType() {
        return effectType;
    }

    public String getMessage() {
        return message;
    }

    public boolean isImmune() {
        return "immune".equals(effectType);
    }

    public boolean isModified() {
        return "modified".equals(effectType);
    }
}
