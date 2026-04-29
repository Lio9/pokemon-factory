package com.lio9.pokedex.model;



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