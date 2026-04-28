package com.lio9.battle.engine.event;



/**
 * TryHitEvent 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战事件机制文件。
 * 核心职责：负责抽象对战事件、事件总线或事件处理契约。
 * 阅读建议：建议结合 BattleEngine 主流程理解触发时机。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */


/**
 * 招式命中事件 - 当招式尝试命中目标时触发
 */
public class TryHitEvent extends BattleEvent {
    private final boolean isImmune;

    public TryHitEvent(boolean isImmune) {
        super(BattleEventType.ON_HIT);
        this.isImmune = isImmune;
    }

    public static TryHitEvent normal() {
        return new TryHitEvent(false);
    }

    public static TryHitEvent immune() {
        return new TryHitEvent(true);
    }

    public boolean isImmune() {
        return isImmune;
    }
}
