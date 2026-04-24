package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.EvolutionChain;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进化链Mapper接口
 */
@Mapper
public interface EvolutionChainMapper extends BaseMapper<EvolutionChain> {
}
