package com.lio9.pokedex.mapper;



/**
 * PokemonMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.EvolutionChain;
import com.lio9.pokedex.model.Move;
import com.lio9.pokedex.model.Pokemon;
import com.lio9.pokedex.model.PokemonEggGroup;
import com.lio9.pokedex.model.PokemonForm;
import com.lio9.pokedex.model.PokemonFormAbility;
import com.lio9.pokedex.model.PokemonFormType;
import com.lio9.pokedex.model.PokemonMove;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宝可梦Mapper接口
 */
@Mapper
public interface PokemonMapper extends BaseMapper<Pokemon> {
    
    /**
     * 查询宝可梦形态信息
     */
    List<PokemonForm> selectPokemonForms(@Param("ew") QueryWrapper<PokemonForm> queryWrapper);
    
    /**
     * 查询宝可梦形态属性
     */
    List<PokemonFormType> selectPokemonFormTypes(@Param("ew") QueryWrapper<PokemonFormType> queryWrapper);
    
    /**
     * 查询宝可梦形态特性
     */
    List<PokemonFormAbility> selectPokemonFormAbilities(@Param("ew") QueryWrapper<PokemonFormAbility> queryWrapper);
    
    /**
     * 查询宝可梦蛋群
     */
    List<PokemonEggGroup> selectPokemonEggGroups(@Param("ew") QueryWrapper<PokemonEggGroup> queryWrapper);
    
    /**
     * 查询宝可梦技能
     */
    List<PokemonMove> selectPokemonMoves(@Param("ew") QueryWrapper<PokemonMove> queryWrapper);
    
    /**
     * 查询进化链
     */
    List<EvolutionChain> selectEvolutionChains(@Param("ew") QueryWrapper<EvolutionChain> queryWrapper);
    
    /**
     * 查询技能
     */
    List<Move> selectMoves(@Param("ew") QueryWrapper<Move> queryWrapper);
}
