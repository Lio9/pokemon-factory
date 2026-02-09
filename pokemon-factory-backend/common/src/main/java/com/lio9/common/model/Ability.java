package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦特性实体类
 */
@Data
@TableName("ability")
public class Ability {
    /**
     * 特性ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 特性编号
     */
    private String indexNumber;
    
    /**
     * 所属世代
     */
    // @TableField(exist = false) // 临时注释，待数据库更新后再启用
    private String generation;
    
    /**
     * 特性名称(中文)
     */
    private String name;
    
    /**
     * 特性名称(日文)
     */
    private String nameJp;
    
    /**
     * 特性名称(英文)
     */
    private String nameEn;
    
    /**
     * 特性描述
     */
    private String description;
    
    /**
     * 特性效果
     */
    private String effect;
    
    /**
     * 普通特性出现次数
     */
    private Integer commonCount;
    
    /**
     * 隐藏特性出现次数
     */
    private Integer hiddenCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}