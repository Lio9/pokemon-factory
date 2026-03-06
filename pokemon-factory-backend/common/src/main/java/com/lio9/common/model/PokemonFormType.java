package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 形态-属性关联表
 */
@Data
@TableName("pokemon_form_type")
public class PokemonFormType {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 形态ID
     */
    private Integer formId;
    
    /**
     * 属性ID
     */
    private Integer typeId;
    
    /**
     * 属性槽位(1或2)
     */
    private Integer slot;
    
    private LocalDateTime createdAt;
}
