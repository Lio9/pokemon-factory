package com.lio9.pokedex.mapper;

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
