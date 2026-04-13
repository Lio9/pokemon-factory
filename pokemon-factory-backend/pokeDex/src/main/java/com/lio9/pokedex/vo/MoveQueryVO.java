package com.lio9.pokedex.vo;

import lombok.Data;

/**
 * 招式查询参数VO
 * 创建人: Lio9
 */
@Data
public class MoveQueryVO {
    
    private Integer current = 1;
    
    private Integer size = 20;
    
    private String name;
    
    private String typeName;
    
    private String category;
    
    private String generation;
}