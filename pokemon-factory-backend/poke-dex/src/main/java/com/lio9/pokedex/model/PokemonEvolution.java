package com.lio9.pokedex.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 进化详情表
 */
@Data
@TableName("pokemon_evolution")
public class PokemonEvolution {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 进化后物种ID
     */
    private Integer evolvedSpeciesId;
    
    /**
     * 进化前物种ID
     */
    private Integer evolvesFromSpeciesId;
    
    /**
     * 进化触发ID
     */
    private Integer evolutionTriggerId;
    
    /**
     * 最低等级
     */
    private Integer minLevel;
    
    /**
     * 最低亲密度
     */
    private Integer minHappiness;
    
    /**
     * 持有物品ID
     */
    private Integer heldItemId;
    
    /**
     * 进化物品ID
     */
    private Integer evolutionItemId;
    
    /**
     * 已知技能ID
     */
    private Integer knownMoveId;
    
    /**
     * 时间段(day/night)
     */
    private String timeOfDay;
    
    /**
     * 性别要求
     */
    private Integer genderId;
    
    private LocalDateTime createdAt;
}