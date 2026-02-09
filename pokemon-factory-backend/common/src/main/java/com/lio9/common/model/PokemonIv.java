package com.lio9.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦个体值实体类
 */
@Data
@TableName("pokemon_iv")
public class PokemonIv {
    /**
     * 个体值ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦形态ID
     */
    private Long pokemonFormId;
    
    /**
     * HP个体值 (0-31)
     */
    private Integer hp;
    
    /**
     * 攻击个体值 (0-31)
     */
    private Integer attack;
    
    /**
     * 防御个体值 (0-31)
     */
    private Integer defense;
    
    /**
     * 特攻个体值 (0-31)
     */
    private Integer spAttack;
    
    /**
     * 特防个体值 (0-31)
     */
    private Integer spDefense;
    
    /**
     * 速度个体值 (0-31)
     */
    private Integer speed;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}