package com.lio9.pokedex.mapper;



/**
 * AbilityMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.Ability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 特性Mapper接口
 */
@Mapper
public interface AbilityMapper extends BaseMapper<Ability> {
    
    /**
     * 获取形态的特性列表
     */
    @Select("SELECT a.*, pfa.is_hidden, pfa.slot " +
            "FROM ability a " +
            "JOIN pokemon_form_ability pfa ON pfa.ability_id = a.id " +
            "WHERE pfa.form_id = #{formId} " +
            "ORDER BY pfa.slot")
    List<Map<String, Object>> selectAbilitiesByFormId(@Param("formId") Integer formId);
}
