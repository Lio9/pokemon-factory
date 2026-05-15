package com.lio9.pokedex.model;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦努力值实体类
 */
@Data
@TableName("pokemon_ev")
public class PokemonEv {
    /**
     * 努力值ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦形态ID
     */
    private Long pokemonFormId;
    
    /**
     * HP努力值 (0-252)
     */
    private Integer hp;
    
    /**
     * 攻击努力值 (0-252)
     */
    private Integer attack;
    
    /**
     * 防御努力值 (0-252)
     */
    private Integer defense;
    
    /**
     * 特攻努力值 (0-252)
     */
    private Integer spAttack;
    
    /**
     * 特防努力值 (0-252)
     */
    private Integer spDefense;
    
    /**
     * 速度努力值 (0-252)
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