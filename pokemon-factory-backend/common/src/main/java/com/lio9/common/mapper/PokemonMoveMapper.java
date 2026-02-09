package com.lio9.common.mapper;

import com.lio9.common.model.PokemonMove;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦技能Mapper接口
 */
@Mapper
public interface PokemonMoveMapper extends BaseMapper<PokemonMove> {
}