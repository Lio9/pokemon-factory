package com.lio9.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.common.model.Pokemon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 宝可梦物种Mapper接口
 */
@Mapper
public interface PokemonSpeciesMapper extends BaseMapper<Pokemon> {
    
    /**
     * 获取物种总数
     */
    @Select("SELECT COUNT(*) FROM pokemon_species")
    int countSpecies();
}