package com.lio9.battle.engine.effect;



/**
 * StandardVolatile 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：对战易失状态文件。
 * 核心职责：负责抽象 volatile 状态、状态规范或状态管理器能力。
 * 阅读建议：建议结合对战状态读写入口一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 标准 Volatile 状态枚举
 * 定义了 Pokemon 中常见的临时状态
 */
public enum StandardVolatile {
    /** 守住/看穿等保护状态 */
    PROTECT("protect", 1),
    
    /**  Broad Guard / Wide Guard */
    WIDE_GUARD("wideguard", 1),
    QUICK_GUARD("quickguard", 1),
    
    /** 混乱状态 */
    CONFUSION("confused", -1), // 实际持续时间为随机(1-4回合)
    
    /** 寄生种子 */
    LEECH_SEED("leechseed", -1),
    
    /** 替身 */
    SUBSTITUTE("substitute", -1),
    
    /** 水流环 */
    AQUA_RING("aquaring", -1),
    
    /** 扎根 */
    INGRAIN("ingrain", -1),
    
    /** 充电 */
    CHARGE("charge", 1),
    
    /** 电磁飘浮 */
    MAGNET_RISE("magnetrise", 5),
    
    /** 奇迹眼 */
    MIRACLE_EYE("miracleeye", -1),
    
    /** 识破 */
    FORESIGHT("foresight", -1),
    
    /** 禁止通行 */
    BLOCKED("blocked", -1),
    
    /** 意念头锤 */
    FLINCH("flinch", 1),
    
    /** 睡觉的回合 */
    SLEEP_TURN("sleepturn", 1),
    
    /** 打瞌睡（即将入睡） */
    YAWN("yawn", 1),
    
    /** 诅咒（幽灵系使用） */
    CURSE("cursed", -1),
    
    /** 灭亡之歌 */
    PERISH_SONG("perishsong", 3),
    
    /** 再来一次 */
    ENCORE("encore", -1),
    
    /** 挑拨 */
    TAUNT("taunt", -1),
    
    /** 无理取闹 */
    TORMENT("torment", -1),
    
    /** 回复封锁 */
    HEAL_BLOCK("healblock", -1),
    
    /** 禁用招式 */
    DISABLE("disabled", -1),
    
    /** 属性改变（如太晶化前的属性变化） */
    TYPE_CHANGE("typechange", -1);

    private final String id;
    private final int defaultDuration;

    StandardVolatile(String id, int defaultDuration) {
        this.id = id;
        this.defaultDuration = defaultDuration;
    }

    public String getId() {
        return id;
    }

    public int getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * 根据ID查找对应的标准Volatile
     */
    public static StandardVolatile fromId(String id) {
        for (StandardVolatile v : values()) {
            if (v.id.equalsIgnoreCase(id)) {
                return v;
            }
        }
        return null;
    }
}
