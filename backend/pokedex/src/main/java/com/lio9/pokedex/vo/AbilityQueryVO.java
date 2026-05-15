package com.lio9.pokedex.vo;



import lombok.Data;

/**
 * 特性查询参数VO
 * 创建人: Lio9
 */
@Data
public class AbilityQueryVO {
    
    private Integer current = 1;
    
    private Integer size = 20;
    
    private String name;
}