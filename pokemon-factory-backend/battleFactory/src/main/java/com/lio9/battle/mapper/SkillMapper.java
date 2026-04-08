package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 技能目录 Mapper。
 */
@Mapper
public interface SkillMapper {
    /**
     * 读取全部技能默认配置。
     */
    @Select("SELECT name, default_cooldown FROM skill_catalog")
    List<Map<String,Object>> findAll();
}
