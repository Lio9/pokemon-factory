package com.lio9.pokedex.model;



/**
 * Type 文件说明
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
 * 属性表
 */
@Data
@TableName("type")
public class Type {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 属性名称(中文)
     */
    private String name;
    
    /**
     * 属性名称(英文)
     */
    private String nameEn;
    
    /**
     * 属性名称(日文)
     */
    private String nameJp;
    
    /**
     * 属性颜色(十六进制)
     */
    private String color;
    
    /**
     * 属性图标URL
     */
    private String iconUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}