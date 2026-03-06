package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Type;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 属性Mapper接口
 */
@Mapper
public interface TypeMapper extends BaseMapper<Type> {
}