package com.lio9.pokedex.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonEggGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦蛋组关联Mapper接口
 */
@Mapper
public interface PokemonEggGroupMapper extends BaseMapper<PokemonEggGroup> {
}
