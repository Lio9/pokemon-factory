package com.lio9.pokedex.model;



/**
 * PokemonForm 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：领域模型文件。
 * 核心职责：负责表达数据库实体、核心领域对象或计算过程中的数据结构。
 * 阅读建议：建议关注字段语义与上下游使用方式。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 宝可梦形态实体类 (对战核心)
 */
@Data
@TableName("pokemon_form")
public class PokemonForm {
    /**
     * 形态ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 物种ID
     */
    private Integer speciesId;
    
    /**
     * 形态名称(英文)
     */
    private String formName;
    
    /**
     * 形态名称(中文)
     */
    private String formNameZh;
    
    /**
     * 形态名称(日文)
     */
    private String formNameJp;
    
    /**
     * 是否为默认形态
     */
    private Boolean isDefault;
    
    /**
     * 是否仅对战形态
     */
    private Boolean isBattleOnly;
    
    /**
     * 是否为Mega进化
     */
    private Boolean isMega;
    
    /**
     * 是否为极巨化
     */
    private Boolean isGigantamax;
    
    /**
     * 是否为太晶化
     */
    private Boolean isTerastal;
    
    /**
     * 身高(米)
     */
    private BigDecimal height;
    
    /**
     * 体重(公斤)
     */
    private BigDecimal weight;
    
    /**
     * 基础经验值
     */
    private Integer baseExperience;
    
    /**
     * 排序序号
     */
    @TableField(value = "`order`")
    private Integer order;
    
    /**
     * 正面图片URL
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
     * 闪光背面图片URL
     */
    private String spriteShinyBackUrl;
    
    /**
     * 官方立绘URL
     */
    private String officialArtworkUrl;
    
    /**
     * 叫声音频URL
     */
    private String cryUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}