package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Pokemon;
import com.lio9.common.model.PokemonForm;
import com.lio9.common.model.PokemonFormType;
import com.lio9.common.model.PokemonFormAbility;
import com.lio9.common.model.PokemonEggGroup;
import com.lio9.common.model.PokemonMove;
import com.lio9.common.model.EvolutionChain;
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
}
