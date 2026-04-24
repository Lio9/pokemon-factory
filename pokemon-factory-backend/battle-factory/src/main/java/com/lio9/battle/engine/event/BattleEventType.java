package com.lio9.battle.engine.event;

/**
 * 战斗事件类型枚举
 * 定义所有可触发的事件点，用于特性和道具的效果注册
 */
public enum BattleEventType {
    // === 招式相关事件 ===
    /** 尝试使用招式前（可用于阻止招式） */
    ON_TRY_MOVE,
    /** 招式执行前（可用于修改招式属性） */
    ON_BEFORE_MOVE,
    /** 招式命中判定后 */
    ON_HIT,
    /** 招式造成伤害时（可修改伤害值） */
    ON_DAMAGE,
    /** 招式执行后 */
    ON_AFTER_MOVE,
    /** 招式失败时 */
    ON_MOVE_FAIL,

    // === 状态变化事件 ===
    /** 设置异常状态前 */
    ON_SET_STATUS,
    /** 恢复HP时 */
    ON_HEAL,
    /** HP减少时（包括伤害和反伤） */
    ON_HP_LOSS,

    // === 回合流程事件 ===
    /** 回合开始 */
    ON_TURN_START,
    /** 回合结束 */
    ON_TURN_END,
    /** 宝可梦上场时 */
    ON_SWITCH_IN,
    /** 宝可梦下场时 */
    ON_SWITCH_OUT,

    // === 场地效果事件 ===
    /** 天气变化时 */
    ON_WEATHER_CHANGE,
    /** 地形变化时 */
    ON_TERRAIN_CHANGE,
    /** 场地效果结束时 */
    ON_FIELD_EFFECT_END,

    // === 特性/道具触发事件 ===
    /** 受到攻击时（用于防守方特性） */
    ON_BEING_HIT,
    /** 攻击时（用于攻击方特性） */
    ON_ATTACKING,
    /** 使用道具时 */
    ON_USE_ITEM,
    /** 道具消耗时 */
    ON_CONSUME_ITEM,

    // === 能力等级变化事件 ===
    /** 能力等级提升时 */
    ON_BOOST,
    /** 能力等级降低时 */
    ON_UNBOOST,

    // === 特殊事件 ===
    /** 濒死时 */
    ON_FAINT,
    /** 太晶化/Mega进化等特殊系统激活时 */
    ON_SPECIAL_SYSTEM_ACTIVATE
}
