package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 物品实体类
 * 创建人: Lio9
 */
@Data
@Accessors(chain = true)
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

    /**
     * 物品编号
     */
    private String indexNumber;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updatedAt;

    // 手动添加setter方法以确保编译通过
    public Item setIndexNumber(String indexNumber) {
        this.indexNumber = indexNumber;
        return this;
    }

    public Item setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Item setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}