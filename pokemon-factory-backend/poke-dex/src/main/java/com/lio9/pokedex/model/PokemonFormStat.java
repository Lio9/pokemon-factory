package com.lio9.pokedex.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 形态种族值表
 */
@Data
@TableName("pokemon_form_stat")
public class PokemonFormStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 形态ID
     */
    private Integer formId;
    
    /**
     * 能力值ID
     */
    private Integer statId;
    
    /**
     * 基础种族值
     */
    private Integer baseStat;
    
    /**
     * 击败后获得的努力值
     */
    private Integer effort;
    
    private LocalDateTime createdAt;
}