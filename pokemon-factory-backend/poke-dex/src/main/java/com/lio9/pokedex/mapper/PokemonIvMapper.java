package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonIv;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦个体值Mapper接口
 */
@Mapper
public interface PokemonIvMapper extends BaseMapper<PokemonIv> {
}
