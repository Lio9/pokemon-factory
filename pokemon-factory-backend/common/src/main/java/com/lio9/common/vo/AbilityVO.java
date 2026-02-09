package com.lio9.common.vo;

import lombok.Data;

/**
 * 特性VO
 * 创建人: Lio9
 */
@Data
public class AbilityVO {
    
    private Long id;
    
    private String indexNumber;
    
    private String generation;
    
    private String name;
    
    private String nameJp;
    
    private String nameEn;
    
    private String description;
    
    private String effect;
    
    private Integer commonCount;
    
    private Integer hiddenCount;
    
    private Boolean isHidden;
    
    private Integer slot;
}