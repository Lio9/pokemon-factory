package com.lio9.pokedex.model;



/**
 * Shape 文件说明
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
 * 形状实体类
 */
@Data
@TableName("shape")
public class Shape {
    /**
     * 形状ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 形状名称
     */
    private String name;
    
    /**
     * 英文名称
     */
    private String nameEn;
    
    /**
     * 日文名称
     */
    private String nameJp;
    
    /**
     * 形状描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}