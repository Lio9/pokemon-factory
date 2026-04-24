package com.lio9.pokedex.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 颜色实体类
 */
@Data
@TableName("color")
public class Color {
    /**
     * 颜色ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 颜色名称
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
     * 十六进制颜色代码
     */
    private String hexCode;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}