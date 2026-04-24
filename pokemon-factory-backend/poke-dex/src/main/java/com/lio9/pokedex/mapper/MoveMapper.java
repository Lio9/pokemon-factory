package com.lio9.pokedex.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.Move;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 技能Mapper接口
 */
@Mapper
public interface MoveMapper extends BaseMapper<Move> {
    
    /**
     * 获取形态可学技能列表
     */
    @Select("SELECT m.*, t.name as type_name, t.color as type_color, " +
            "mdc.name as damage_class_name, mlm.name as learn_method, " +
            "pfm.level, pfm.version_group_id " +
            "FROM move m " +
            "JOIN pokemon_form_move pfm ON pfm.move_id = m.id " +
            "LEFT JOIN type t ON t.id = m.type_id " +
            "LEFT JOIN move_damage_class mdc ON mdc.id = m.damage_class_id " +
            "LEFT JOIN move_learn_method mlm ON mlm.id = pfm.learn_method_id " +
            "WHERE pfm.form_id = #{formId} " +
            "ORDER BY mlm.id, pfm.level, m.id")
    List<Map<String, Object>> selectMovesByFormId(@Param("formId") Integer formId);
    
    /**
     * 搜索技能
     */
    @Select("SELECT m.*, t.name as type_name, t.color as type_color, mdc.name as damage_class_name " +
            "FROM move m " +
            "LEFT JOIN type t ON t.id = m.type_id " +
            "LEFT JOIN move_damage_class mdc ON mdc.id = m.damage_class_id " +
            "WHERE m.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR m.name_en LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY m.id")
    List<Map<String, Object>> searchMoves(@Param("keyword") String keyword);
    
    /**
     * 获取技能详情（包含属性名称和伤害类型）
     */
    @Select("SELECT m.*, t.name as type_name, t.color as type_color, mdc.name as damage_class_name " +
            "FROM move m " +
            "LEFT JOIN type t ON t.id = m.type_id " +
            "LEFT JOIN move_damage_class mdc ON mdc.id = m.damage_class_id " +
            "WHERE m.id = #{moveId}")
    Map<String, Object> selectMoveDetailById(@Param("moveId") Integer moveId);
}
