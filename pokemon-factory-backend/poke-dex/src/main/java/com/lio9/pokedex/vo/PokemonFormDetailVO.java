package com.lio9.pokedex.vo;

import lombok.Data;

import java.util.List;

/**
 * 宝可梦形态详情VO
 */
@Data
public class PokemonFormDetailVO {
    /**
     * 形态ID
     */
    private Integer id;
    
    /**
     * 形态名称(中文)
     */
    private String formName;
    
    /**
     * 是否默认形态
     */
    private Boolean isDefault;
    
    /**
     * 是否Mega
     */
    private Boolean isMega;
    
    /**
     * 是否极巨化
     */
    private Boolean isGigantamax;
    
    /**
     * 身高(米)
     */
    private Double height;
    
    /**
     * 体重(公斤)
     */
    private Double weight;
    
    /**
     * 基础经验值
     */
    private Integer baseExperience;
    
    /**
     * 属性列表
     */
    private List<TypeVO> types;
    
    /**
     * 特性列表
     */
    private List<AbilityVO> abilities;
    
    /**
     * 种族值
     */
    private StatVO stats;
    
    /**
     * 图片URL
     */
    private String spriteUrl;
    
    /**
     * 背面图片URL
     */
    private String spriteBackUrl;
    
    /**
     * 闪光正面图片URL
     */
    private String spriteShinyUrl;
    
    /**
     * 官方立绘URL
     */
    private String officialArtworkUrl;
}