package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦形态特性关联实体类
 */
@Data
@TableName("pokemon_form_ability")
public class PokemonFormAbility {
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
     * 特性ID
     */
    private Long abilityId;
    
    /**
     * 特性槽位(1=第一特性, 2=第二特性, 3=隐藏特性)
     */
    private Integer slot;
    
    /**
     * 是否为隐藏特性
     */
    private Boolean isHidden;
    
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
    
    public void setAbilityId(Long abilityId) {
        this.abilityId = abilityId;
    }
    
    public void setSlot(Integer slot) {
        this.slot = slot;
    }
    
    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}