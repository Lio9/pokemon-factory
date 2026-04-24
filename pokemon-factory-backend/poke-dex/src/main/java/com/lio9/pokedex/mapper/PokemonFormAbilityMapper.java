package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonFormAbility;
import org.apache.ibatis.annotations.Mapper;

/**
 * 形态-特性关联Mapper
 */
@Mapper
public interface PokemonFormAbilityMapper extends BaseMapper<PokemonFormAbility> {
}
