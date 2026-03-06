package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonEvolution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进化详情Mapper
 */
@Mapper
public interface PokemonEvolutionMapper extends BaseMapper<PokemonEvolution> {
}
