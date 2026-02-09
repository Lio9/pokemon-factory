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
     * 技能属性
     */
    private String type;
    
    /**
     * 技能类别
     */
    private String category;
    
    /**
     * 技能威力
     */
    private String power;
    
    /**
     * 技能命中率
     */
    private String accuracy;
    
    /**
     * 技能PP值
     */
    private String pp;
    
    /**
     * 技能描述
     */
    private String description;
    
    /**
     * 技能效果
     */
    private String effect;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}