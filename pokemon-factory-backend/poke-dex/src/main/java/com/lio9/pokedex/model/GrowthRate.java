package com.lio9.pokedex.model;



/**
 * GrowthRate 文件说明
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
 * 经验类型实体类
 */
@Data
@TableName("growth_rate")
public class GrowthRate {
    /**
     * 经验类型ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 经验类型名称
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
     * 经验公式
     */
    private String formula;
    
    /**
     * 经验类型描述
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
    
    // 手动添加setter方法
    public void setName(String name) {
        this.name = name;
    }
    
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }
    
    public void setNameJp(String nameJp) {
        this.nameJp = nameJp;
    }
    
    public void setFormula(String formula) {
        this.formula = formula;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}