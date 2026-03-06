package com.lio9.common.model;

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
