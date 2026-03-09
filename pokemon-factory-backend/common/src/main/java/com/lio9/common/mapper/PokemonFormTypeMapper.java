package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.PokemonFormType;
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
     * 批量获取形态的属性信息(优化N+1查询)
     */
    @Select("<script>" +
            "SELECT pft.form_id, pft.slot, t.id as type_id, t.name, t.name_en, t.color " +
            "FROM pokemon_form_type pft " +
            "INNER JOIN type t ON t.id = pft.type_id " +
            "WHERE pft.form_id IN " +
            "<foreach item='id' collection='formIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "ORDER BY pft.form_id, pft.slot" +
            "</script>")
    List<Map<String, Object>> selectTypesByFormIds(@Param("formIds") List<Integer> formIds);
}