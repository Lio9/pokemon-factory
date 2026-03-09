package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonFormStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 形态种族值Mapper
 */
@Mapper
public interface PokemonFormStatMapper extends BaseMapper<PokemonFormStat> {
    
    /**
     * 根据形态ID和能力ID查询种族值
     */
    @Select("SELECT pfs.*, s.name as stat_name FROM pokemon_form_stat pfs " +
            "JOIN stat s ON pfs.stat_id = s.id " +
            "WHERE pfs.form_id = #{formId} AND pfs.stat_id = #{statId}")
    Map<String, Object> selectByFormIdAndStatId(@Param("formId") Integer formId, @Param("statId") Integer statId);
}
