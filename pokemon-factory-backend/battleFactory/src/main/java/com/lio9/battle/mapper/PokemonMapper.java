package com.lio9.battle.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("battlePokemonMapper")
@Mapper
public interface PokemonMapper {
    @Select("SELECT id, name, base_experience FROM pokemon ORDER BY RANDOM() LIMIT #{limit}")
    List<Map<String,Object>> sampleLimit(@Param("limit") int limit);
}
