package com.lio9.common.mapper;

import com.lio9.common.model.PokemonEggGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦蛋组关联Mapper接口
 */
@Mapper
public interface PokemonEggGroupMapper extends BaseMapper<PokemonEggGroup> {
}