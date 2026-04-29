package com.lio9.pokedex.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lio9.pokedex.model.Pokemon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
