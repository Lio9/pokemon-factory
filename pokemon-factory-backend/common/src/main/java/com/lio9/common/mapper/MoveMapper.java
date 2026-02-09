package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Move;
import org.apache.ibatis.annotations.Select;

/**
 * 技能Mapper接口
 */
public interface MoveMapper extends BaseMapper<Move> {
    
    /**
     * 根据名称查询技能
     * @param name 技能名称
     * @return 技能对象
     */
    @Select("SELECT * FROM move WHERE name = #{name}")
    Move selectByName(String name);
}
