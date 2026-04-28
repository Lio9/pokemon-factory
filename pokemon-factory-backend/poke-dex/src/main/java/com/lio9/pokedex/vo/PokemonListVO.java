package com.lio9.pokedex.vo;



/**
 * PokemonListVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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