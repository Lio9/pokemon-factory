package com.lio9.pokedex.vo;



/**
 * PokemonFormDetailVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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