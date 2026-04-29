package com.lio9.pokedex.model;



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