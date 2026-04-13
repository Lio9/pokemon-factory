package com.lio9.pokedex.vo;

import lombok.Data;

/**
 * 宝可梦查询参数VO
 * 创建人: Lio9
 */
@Data
public class PokemonQueryVO {
    
    private Integer current = 1;
    
    private Integer size = 20;
    
    private String name;
    
    private Long typeId;
}