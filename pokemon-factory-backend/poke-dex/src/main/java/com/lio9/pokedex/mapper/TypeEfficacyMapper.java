package com.lio9.pokedex.mapper;



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