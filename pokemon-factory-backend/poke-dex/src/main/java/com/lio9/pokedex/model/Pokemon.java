package com.lio9.pokedex.model;



/**
 * Pokemon 文件说明
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

import java.time.LocalDateTime;

/**
 * 宝可梦物种实体类 (对应pokemon_species表)
 */
@Data
@TableName("pokemon_species")
public class Pokemon {
    /**
     * 物种ID (全国图鉴编号)
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 宝可梦名称(中文)
     */
    private String name;
    
    /**
     * 宝可梦名称(英文)
     */
    private String nameEn;
    
    /**
     * 宝可梦名称(日文)
     */
    private String nameJp;
    
    /**
     * 分类(如"种子宝可梦")
     */
    private String genus;
    
    /**
     * 世代ID
     */
    private Integer generationId;
    
    /**
     * 进化链ID
     */
    private Integer evolutionChainId;
    
    /**
     * 进化前物种ID
     */
    private Integer evolvesFromSpeciesId;
    
    /**
     * 主色调
     */
    private String color;
    
    /**
     * 体型
     */
    private String shape;
    
    /**
     * 栖息地
     */
    private String habitat;
    
    /**
     * 成长类型ID
     */
    private Integer growthRateId;
    
    /**
     * 性别比例(-1=无性别,0=全雄,8=全雌)
     */
    private Integer genderRate;
    
    /**
     * 捕获率
     */
    private Integer captureRate;
    
    /**
     * 基础亲密度
     */
    private Integer baseHappiness;
    
    /**
     * 孵化步数
     */
    private Integer hatchCounter;
    
    /**
     * 是否为幼崽
     */
    private Boolean isBaby;
    
    /**
     * 是否为传说
     */
    private Boolean isLegendary;
    
    /**
     * 是否为神话
     */
    private Boolean isMythical;
    
    /**
     * 是否有性别差异
     */
    private Boolean hasGenderDifferences;
    
    /**
     * 形态是否可切换
     */
    private Boolean formsSwitchable;
    
    /**
     * 排序序号
     */
    @TableField(value = "`order`")
    private Integer order;
    
    /**
     * 图鉴描述
     */
    private String description;
    
    // ===== 兼容性字段 =====
    
    /**
     * 兼容旧字段: 全国图鉴编号
     */
    @TableField(exist = false)
    private String indexNumber;
    
    /**
     * 兼容旧字段: 描述
     */
    @TableField(exist = false)
    private String profile;
    
    /**
     * 兼容旧字段: 身高
     */
    @TableField(exist = false)
    private Double height;
    
    /**
     * 兼容旧字段: 体重
     */
    @TableField(exist = false)
    private Double weight;
    
    /**
     * 兼容旧字段: 基础经验值
     */
    @TableField(exist = false)
    private Integer baseExperience;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // 兼容方法
    public String getIndexNumber() {
        return String.valueOf(id);
    }
    
    public String getProfile() {
        return description;
    }
}