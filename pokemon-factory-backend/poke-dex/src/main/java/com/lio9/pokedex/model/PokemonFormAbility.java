package com.lio9.pokedex.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 形态-特性关联表
 */
@Data
@TableName("pokemon_form_ability")
public class PokemonFormAbility {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 形态ID
     */
    private Integer formId;
    
    /**
     * 特性ID
     */
    private Integer abilityId;
    
    /**
     * 是否为隐藏特性
     */
    private Boolean isHidden;
    
    /**
     * 特性槽位(1-3)
     */
    private Integer slot;
    
    private LocalDateTime createdAt;
}