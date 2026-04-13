package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.Type;
import org.apache.ibatis.annotations.Mapper;

/**
 * 属性Mapper接口
 */
@Mapper
public interface TypeMapper extends BaseMapper<Type> {
}
