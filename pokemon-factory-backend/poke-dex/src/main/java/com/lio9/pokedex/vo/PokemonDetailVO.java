package com.lio9.pokedex.vo;



/**
 * PokemonDetailVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Data;

/**
 * 宝可梦详情VO
 */
@Data
public class PokemonDetailVO {
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
     * 名称(日文)
     */
    private String nameJp;
    
    /**
     * 分类(如"种子宝可梦")
     */
    private String genus;
    
    /**
     * 图鉴描述
     */
    private String description;
    
    /**
     * 世代ID
     */
    private Integer generationId;
    
    /**
     * 是否传说
     */
    private Boolean isLegendary;
    
    /**
     * 是否神话
     */
    private Boolean isMythical;
    
    /**
     * 是否幼崽
     */
    private Boolean isBaby;
    
    /**
     * 捕获率
     */
    private Integer captureRate;
    
    /**
     * 基础亲密度
     */
    private Integer baseHappiness;
    
    /**
     * 性别比例(-1无性别,0全雄,8全雌)
     */
    private Integer genderRate;
    
    /**
     * 孵化步数
     */
    private Integer hatchCounter;
    
    /**
     * 成长类型
     */
    private String growthRate;
    
    /**
     * 蛋群
     */
    private java.util.List<String> eggGroups;
    
    /**
     * 形态列表
     */
    private java.util.List<PokemonFormDetailVO> forms;
    
    /**
     * 进化链
     */
    private java.util.List<EvolutionChainVO> evolutionChain;
}