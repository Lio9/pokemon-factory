package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦形态实体类
 */
@Data
@TableName("pokemon_form")
public class PokemonForm {
    /**
     * 形态ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦ID
     */
    private Long pokemonId;
    
    /**
     * 形态名称
     */
    private String name;
    
    /**
     * 形态编号
     */
    private String indexNumber;
    
    /**
     * 是否为超级进化形态
     */
    private Boolean isMega;
    
    /**
     * 是否为极巨化形态
     */
    private Boolean isGmax;
    
    /**
     * 形态图片URL
     */
    private String imageUrl;
    
    /**
     * 是否为默认形态
     */
    private Boolean isDefault;
    
    /**
     * 排序
     */
    private Integer order;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}