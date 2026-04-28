package com.lio9.pokedex.model;



/**
 * PokemonEv 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：领域模型文件。
 * 核心职责：负责表达数据库实体、核心领域对象或计算过程中的数据结构。
 * 阅读建议：建议关注字段语义与上下游使用方式。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宝可梦努力值实体类
 */
@Data
@TableName("pokemon_ev")
public class PokemonEv {
    /**
     * 努力值ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦形态ID
     */
    private Long pokemonFormId;
    
    /**
     * HP努力值 (0-252)
     */
    private Integer hp;
    
    /**
     * 攻击努力值 (0-252)
     */
    private Integer attack;
    
    /**
     * 防御努力值 (0-252)
     */
    private Integer defense;
    
    /**
     * 特攻努力值 (0-252)
     */
    private Integer spAttack;
    
    /**
     * 特防努力值 (0-252)
     */
    private Integer spDefense;
    
    /**
     * 速度努力值 (0-252)
     */
    private Integer speed;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}