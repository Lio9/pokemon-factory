package com.lio9.common.model;

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