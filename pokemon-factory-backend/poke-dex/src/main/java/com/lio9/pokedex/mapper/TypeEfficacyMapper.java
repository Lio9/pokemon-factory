package com.lio9.pokedex.mapper;



/**
 * TypeEfficacyMapper 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：后端数据访问文件。
 * 核心职责：负责声明数据库访问接口或对象映射能力。
 * 阅读建议：建议结合对应 XML 或 SQL 结果结构一起理解。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 属性相性Mapper接口
 */
@Mapper
public interface TypeEfficacyMapper {
    
    /**
     * 获取所有属性相性数据
     */
    @Select("SELECT te.attacking_type_id as damage_type_id, te.defending_type_id as target_type_id, te.damage_factor, " +
            "dt.name as damage_type_name, tt.name as target_type_name " +
            "FROM type_efficacy te " +
            "JOIN type dt ON te.attacking_type_id = dt.id " +
            "JOIN type tt ON te.defending_type_id = tt.id")
    List<Map<String, Object>> selectAllTypeEfficacy();
    
    /**
     * 获取特定攻击属性的相性数据
     */
    @Select("SELECT te.attacking_type_id as damage_type_id, te.defending_type_id as target_type_id, te.damage_factor, " +
            "dt.name as damage_type_name, tt.name as target_type_name " +
            "FROM type_efficacy te " +
            "JOIN type dt ON te.attacking_type_id = dt.id " +
            "JOIN type tt ON te.defending_type_id = tt.id " +
            "WHERE te.attacking_type_id = #{damageTypeId}")
    List<Map<String, Object>> selectByDamageTypeId(@Param("damageTypeId") Integer damageTypeId);
    
    /**
     * 获取特定攻击属性对特定防御属性的相性
     */
    @Select("SELECT damage_factor FROM type_efficacy " +
            "WHERE attacking_type_id = #{damageTypeId} AND defending_type_id = #{targetTypeId}")
    Integer selectDamageFactor(@Param("damageTypeId") Integer damageTypeId, @Param("targetTypeId") Integer targetTypeId);
}