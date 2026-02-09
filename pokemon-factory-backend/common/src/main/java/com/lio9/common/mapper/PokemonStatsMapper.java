package com.lio9.common.mapper;

import com.lio9.common.model.PokemonStats;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦种族值Mapper接口
 */
@Mapper
public interface PokemonStatsMapper extends BaseMapper<PokemonStats> {
}