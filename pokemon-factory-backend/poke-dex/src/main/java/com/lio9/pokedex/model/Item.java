package com.lio9.pokedex.model;



/**
 * Item 文件说明
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
 * 物品表
 */
@Data
@TableName("item")
public class Item {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 物品名称(中文)
     */
    private String name;
    
    /**
     * 物品名称(英文)
     */
    private String nameEn;
    
    /**
     * 物品名称(日文)
     */
    private String nameJp;
    
    /**
     * 分类ID
     */
    private Integer categoryId;
    
    /**
     * 购买价格
     */
    private Integer cost;
    
    /**
     * 投掷威力
     */
    private Integer flingPower;
    
    /**
     * 投掷效果ID
     */
    private Integer flingEffectId;
    
    /**
     * 效果简述
     */
    private String effectShort;
    
    /**
     * 详细效果
     */
    private String effectDetail;
    
    /**
     * 物品描述
     */
    private String description;
    
    /**
     * 首次出现世代
     */
    private Integer generationId;
    
    /**
     * 图标URL
     */
    private String spriteUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}