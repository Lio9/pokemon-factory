package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonFormType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 形态-属性关联Mapper
 */
@Mapper
public interface PokemonFormTypeMapper extends BaseMapper<PokemonFormType> {
}
