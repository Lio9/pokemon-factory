package com.lio9.pokedex.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 物品查询VO
 * 创建人: Lio9
 */
@Data
public class ItemQueryVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页
     */
    private Integer current;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 物品名称
     */
    private String name;
    
    /**
     * 物品分类
     */
    private String category;
}