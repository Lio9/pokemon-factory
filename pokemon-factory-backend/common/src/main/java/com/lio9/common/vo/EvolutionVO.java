package com.lio9.common.vo;

import lombok.Data;

/**
 * 进化链VO
 */
@Data
public class EvolutionVO {
    /**
     * 物种ID
     */
    private Integer speciesId;
    
    /**
     * 宝可梦ID
     */
    private Long pokemonId;
    
    /**
     * 名称
     */
    private String pokemonName;
    
    /**
     * 图鉴编号
     */
    private String pokemonIndexNumber;
    
    /**
     * 进化触发条件
     */
    private String evolutionMethod;
    
    /**
     * 进化等级/物品
     */
    private String evolutionValue;
    
    /**
     * 进化触发
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
    
    /**
     * 进化前名称
     */
    private String evolvesFromName;
}