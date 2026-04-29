package com.lio9.pokedex.model;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 宝可梦颜色关联实体类
 */
@Data
@TableName("pokemon_color")
public class PokemonColor {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宝可梦ID
     */
    private Long pokemonId;
    
    /**
     * 颜色ID
     */
    private Long colorId;
}