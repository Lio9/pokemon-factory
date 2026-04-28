package com.lio9.pokedex.mapper;



/**
 * PokemonFormMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.PokemonForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 宝可梦形态Mapper接口
 */
@Mapper
public interface PokemonFormMapper extends BaseMapper<PokemonForm> {
    
    /**
     * 获取物种的所有形态
     */
    @Select("SELECT * FROM pokemon_form WHERE species_id = #{speciesId} ORDER BY is_default DESC, id")
    List<PokemonForm> selectBySpeciesId(@Param("speciesId") Integer speciesId);
    
    /**
     * 批量获取物种的默认形态(优化N+1查询)
     */
    @Select("<script>" +
            "SELECT pf.id, pf.species_id, pf.sprite_url, pf.official_artwork_url " +
            "FROM pokemon_form pf " +
            "WHERE pf.is_default = 1 AND pf.species_id IN " +
            "<foreach item='id' collection='speciesIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> selectDefaultFormsBySpeciesIds(@Param("speciesIds") List<Integer> speciesIds);
    
    /**
     * 获取形态详情(包含属性、特性、种族值)
     */
    @Select("SELECT pf.*, " +
            "GROUP_CONCAT(DISTINCT t.name ORDER BY pft.slot SEPARATOR '/') as types, " +
            "GROUP_CONCAT(DISTINCT t.color ORDER BY pft.slot SEPARATOR '/') as type_colors " +
            "FROM pokemon_form pf " +
            "LEFT JOIN pokemon_form_type pft ON pft.form_id = pf.id " +
            "LEFT JOIN type t ON t.id = pft.type_id " +
            "WHERE pf.id = #{formId} " +
            "GROUP BY pf.id")
    Map<String, Object> selectFormDetail(@Param("formId") Integer formId);
}
