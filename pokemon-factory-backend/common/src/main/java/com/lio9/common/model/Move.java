package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能实体类
 */
@Data
@TableName("move")
public class Move {
    /**
     * 技能ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 技能编号
     */
    private String indexNumber;
    
    /**
     * 所属世代
     */
    private String generation;
    
    /**
     * 技能名称(中文)
     */
    private String name;
    
    /**
     * 技能名称(日文)
     */
    private String nameJp;
    
    /**
     * 技能名称(英文)
     */
    private String nameEn;
    
    /**
     * 属性ID
     */
    private Long typeId;
    
    /**
     * 威力
     */
    private Integer power;
    
    /**
     * PP值
     */
    private Integer pp;
    
    /**
     * 命中率
     */
    private Integer accuracy;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 伤害类型(physical/special/status)
     */
    private String damageClass;
    
    /**
     * 技能描述
     */
    private String description;
    
    /**
     * 技能效果
     */
    private String effect;
    
    /**
     * 效果几率
     */
    private Integer effectChance;
    
    /**
     * 华丽大赛类型
     */
    private String contestType;
    
    /**
     * 华丽大赛效果
     */
    private String contestEffect;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}