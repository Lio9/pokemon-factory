package com.lio9.pokedex.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能力值Mapper接口
 */
@Mapper
public interface PokemonStatsMapper extends BaseMapper<PokemonStats> {
}
