package com.lio9.pokedex.mapper;



/**
 * PokemonFormTypeMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonFormType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 形态-属性关联Mapper
 */
@Mapper
public interface PokemonFormTypeMapper extends BaseMapper<PokemonFormType> {
    
    /**
     * 批量获取形态的属性信息
     */
    @Select("<script>" +
            "SELECT pft.form_id, pft.slot, t.id as type_id, t.name, t.name_en, t.color " +
            "FROM pokemon_form_type pft " +
            "INNER JOIN type t ON t.id = pft.type_id " +
            "WHERE pft.form_id IN " +
            "<foreach item='id' collection='formIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "ORDER BY pft.form_id, pft.slot" +
            "</script>")
    List<Map<String, Object>> selectTypesByFormIds(@Param("formIds") List<Integer> formIds);
}
