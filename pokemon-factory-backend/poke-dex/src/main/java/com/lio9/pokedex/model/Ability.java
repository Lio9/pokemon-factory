package com.lio9.pokedex.model;



/**
 * Ability 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：领域模型文件。
 * 核心职责：负责表达数据库实体、核心领域对象或计算过程中的数据结构。
 * 阅读建议：建议关注字段语义与上下游使用方式。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 特性表
 */
@Data
@TableName("ability")
public class Ability {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 特性名称(中文)
     */
    private String name;
    
    /**
     * 特性名称(英文)
     */
    private String nameEn;
    
    /**
     * 特性名称(日文)
     */
    private String nameJp;
    
    /**
     * 特性描述(中文)
     */
    private String description;
    
    /**
     * 特性描述(英文)
     */
    private String descriptionEn;
    
    /**
     * 详细效果说明
     */
    private String effectDetail;
    
    /**
     * 首次出现世代
     */
    private Integer generationId;
    
    /**
     * 是否为主系列特性
     */
    private Boolean isMainSeries;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}