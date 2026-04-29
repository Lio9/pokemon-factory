package com.lio9.pokedex.vo;



import lombok.Data;

/**
 * 特性VO
 */
@Data
public class AbilityVO {
    private Integer id;
    private String name;
    private String nameEn;
    private String description;
    private Boolean isHidden;
    private Integer slot;
}