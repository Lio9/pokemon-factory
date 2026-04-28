package com.lio9.pokedex.vo;



/**
 * TypeEfficacyVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Data;

/**
 * 属性相性查询结果VO
 */
@Data
public class TypeEfficacyVO {
    /**
     * 攻击属性ID
     */
    private Integer damageTypeId;
    
    /**
     * 攻击属性名称
     */
    private String damageTypeName;
    
    /**
     * 目标属性ID
     */
    private Integer targetTypeId;
    
    /**
     * 目标属性名称
     */
    private String targetTypeName;
    
    /**
     * 伤害倍率 (0=无效, 50=效果不佳, 100=一般, 200=效果绝佳)
     */
    private Integer damageFactor;
    
    /**
     * 获取倍率描述
     */
    public String getFactorDesc() {
        if (damageFactor == 0) return "无效";
        if (damageFactor == 50) return "效果不佳(0.5x)";
        if (damageFactor == 100) return "效果一般(1x)";
        if (damageFactor == 200) return "效果绝佳(2x)";
        if (damageFactor == 400) return "效果绝佳(4x)";
        return String.format("%.2fx", damageFactor / 100.0);
    }
}