package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonForm;
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
