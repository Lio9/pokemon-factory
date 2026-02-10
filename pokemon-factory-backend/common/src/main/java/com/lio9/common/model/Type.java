package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 属性类型实体类
 */
@Data
@TableName("type")
public class Type {
    /**
     * 属性ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 属性名称
     */
    private String name;
    
    /**
     * 属性英文名称
     */
    private String nameEn;
    
    /**
     * 属性日文名称
     */
    private String nameJp;
    
    /**
     * 属性颜色代码
     */
    private String color;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}