package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonMove;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦技能Mapper接口
 */
@Mapper
public interface PokemonMoveMapper extends BaseMapper<PokemonMove> {
}
