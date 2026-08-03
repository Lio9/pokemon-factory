package com.lio9.pokedex.model;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 宝可梦技能关联实体类
 */
@Data
@TableName("pokemon_form_move")
public class PokemonMove {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 形态ID
     */
    private Long formId;

    /**
     * 技能ID
     */
    private Long moveId;

    /**
     * 学习方式ID
     */
    private Long learnMethodId;

    /**
     * 学习等级
     */
    private Integer level;

    /**
     * 版本组ID
     */
    private Long versionGroupId;
}
