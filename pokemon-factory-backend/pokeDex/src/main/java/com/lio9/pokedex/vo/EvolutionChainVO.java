package com.lio9.pokedex.vo;

import lombok.Data;

/**
 * 进化链VO
 */
@Data
public class EvolutionChainVO {
    /**
     * 物种ID
     */
    private Integer speciesId;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 进化触发条件
     */
    private String trigger;
    
    /**
     * 进化等级
     */
    private Integer minLevel;
    
    /**
     * 进化物品
     */
    private String item;
    
    /**
     * 图片URL
     */
    private String spriteUrl;
    
    /**
     * 是否当前物种
     */
    private Boolean isCurrent;
}