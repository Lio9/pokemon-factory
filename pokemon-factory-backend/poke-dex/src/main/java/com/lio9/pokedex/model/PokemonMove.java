package com.lio9.pokedex.model;



/**
 * PokemonMove 文件说明
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
 * 宝可梦技能关联实体类
 */
@Data
@TableName("pokemon_move")
public class PokemonMove {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦ID
     */
    private Long pokemonId;
    
    /**
     * 技能ID
     */
    private Long moveId;
    
    /**
     * 学习方式
     */
    private String learnMethod;
    
    /**
     * 学习等级
     */
    private Integer level;
    
    /**
     * 版本组
     */
    private String versionGroup;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // 手动添加setter方法
    public void setPokemonId(Long pokemonId) {
        this.pokemonId = pokemonId;
    }
    
    public void setMoveId(Long moveId) {
        this.moveId = moveId;
    }
    
    public void setLearnMethod(String learnMethod) {
        this.learnMethod = learnMethod;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public void setVersionGroup(String versionGroup) {
        this.versionGroup = versionGroup;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}