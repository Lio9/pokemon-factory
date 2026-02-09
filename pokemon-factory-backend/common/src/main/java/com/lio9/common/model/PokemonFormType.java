package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦形态属性关联实体类
 */
@Data
@TableName("pokemon_form_type")
public class PokemonFormType {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦形态ID
     */
    private Long formId;
    
    /**
     * 属性ID
     */
    private Long typeId;
    
    /**
     * 属性槽位(主属性为1,副属性为2)
     */
    private Integer slot;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // 手动添加setter方法
    public void setFormId(Long formId) {
        this.formId = formId;
    }
    
    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }
    
    public void setSlot(Integer slot) {
        this.slot = slot;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}