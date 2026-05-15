package com.lio9.pokedex.vo;



import lombok.Data;

import java.util.List;

/**
 * 宝可梦列表项VO
 */
@Data
public class PokemonListVO {
    /**
     * 物种ID(全国图鉴编号)
     */
    private Integer id;
    
    /**
     * 名称(中文)
     */
    private String name;
    
    /**
     * 名称(英文)
     */
    private String nameEn;
    
    /**
     * 分类(如"种子宝可梦")
     */
    private String genus;
    
    /**
     * 属性列表
     */
    private List<TypeVO> types;
    
    /**
     * 默认形态ID
     */
    private Integer defaultFormId;
    
    /**
     * 图片URL
     */
    private String spriteUrl;
    
    /**
     * 官方立绘URL
     */
    private String officialArtworkUrl;
    
    /**
     * 是否传说
     */
    private Boolean isLegendary;
    
    /**
     * 是否神话
     */
    private Boolean isMythical;
    
    /**
     * 世代ID
     */
    private Integer generationId;
}