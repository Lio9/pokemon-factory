package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Type;
import org.apache.ibatis.annotations.Select;

/**
 * 属性Mapper接口
 */
public interface TypeMapper extends BaseMapper<Type> {
    
    /**
     * 根据名称查询属性
     * @param name 属性名称
     * @return 属性对象
     */
    @Select("SELECT * FROM type WHERE name = #{name}")
    Type selectByName(String name);
}
