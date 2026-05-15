package com.lio9.pokedex.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonEv;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦努力值Mapper接口
 */
@Mapper
public interface PokemonEvMapper extends BaseMapper<PokemonEv> {
}
