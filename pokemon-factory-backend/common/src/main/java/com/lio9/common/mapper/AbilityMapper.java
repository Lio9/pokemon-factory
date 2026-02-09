package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Ability;
import org.apache.ibatis.annotations.Select;

/**
 * 特性Mapper接口
 */
public interface AbilityMapper extends BaseMapper<Ability> {
    
    /**
     * 根据名称查询特性
     * @param name 特性名称
     * @return 特性对象
     */
    @Select("SELECT * FROM ability WHERE name = #{name}")
    Ability selectByName(String name);
}
