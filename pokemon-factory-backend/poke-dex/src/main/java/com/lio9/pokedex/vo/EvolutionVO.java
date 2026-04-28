package com.lio9.pokedex.vo;



/**
 * EvolutionVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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