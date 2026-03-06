package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能表
 */
@Data
@TableName("move")
public class Move {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 技能名称(中文)
     */
    private String name;
    
    /**
     * 技能名称(英文)
     */
    private String nameEn;
    
    /**
     * 技能名称(日文)
     */
    private String nameJp;
    
    /**
     * 属性ID
     */
    private Integer typeId;
    
    /**
     * 伤害类型ID
     */
    private Integer damageClassId;
    
    /**
     * 目标类型ID
     */
    private Integer targetId;
    
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
     * 优先度
     */
    private Integer priority;
    
    /**
     * 追加效果触发率
     */
    private Integer effectChance;
    
    /**
     * 效果简述
     */
    private String effectShort;
    
    /**
     * 详细效果
     */
    private String effectDetail;
    
    /**
     * 技能描述
     */
    private String description;
    
    /**
     * 首次出现世代
     */
    private Integer generationId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
