package com.lio9.pokedex.mapper;

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
