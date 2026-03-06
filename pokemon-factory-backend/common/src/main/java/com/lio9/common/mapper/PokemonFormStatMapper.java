package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonFormStat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 形态种族值Mapper
 */
@Mapper
public interface PokemonFormStatMapper extends BaseMapper<PokemonFormStat> {
}
