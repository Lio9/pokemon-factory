package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface SkillMapper {
    @Select("SELECT name, default_cooldown FROM skill_catalog")
    List<Map<String,Object>> findAll();
}
