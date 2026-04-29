package com.lio9.pokedex.model;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦种族值实体类
 */
@Data
@TableName("pokemon_stats")
public class PokemonStats {
    /**
     * 种族值ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 形态ID
     */
    private Long formId;
    
    /**
     * HP种族值
     */
    private Integer hp;
    
    /**
     * 攻击种族值
     */
    private Integer attack;
    
    /**
     * 防御种族值
     */
    private Integer defense;
    
    /**
     * 特攻种族值
     */
    private Integer specialAttack;
    
    /**
     * 特防种族值
     */
    private Integer specialDefense;
    
    /**
     * 速度种族值
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