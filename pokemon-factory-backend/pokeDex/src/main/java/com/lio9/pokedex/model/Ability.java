package com.lio9.pokedex.model;

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