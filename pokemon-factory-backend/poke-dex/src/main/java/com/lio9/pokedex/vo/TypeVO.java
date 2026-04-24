package com.lio9.pokedex.vo;

import lombok.Data;

/**
 * 属性VO
 */
@Data
public class TypeVO {
    private Integer id;
    private String name;
    private String nameEn;
    private String color;
    private String iconUrl;
}