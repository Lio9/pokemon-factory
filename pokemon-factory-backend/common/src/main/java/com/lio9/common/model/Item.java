package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 物品实体类
 * 创建人: Lio9
 */
@Data
@TableName("item")
public class Item implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 物品名称
     */
    private String name;
    
    /**
     * 物品英文名称
     */
    private String nameEn;
    
    /**
     * 物品日文名称
     */
    private String nameJp;
    
    /**
     * 物品分类
     */
    private String category;
    
    /**
     * 物品价格
     */
    private Integer price;
    
    /**
     * 物品效果
     */
    private String effect;
    
    /**
     * 物品描述
     */
    private String description;
}