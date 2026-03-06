package com.lio9.common.vo;

import lombok.Data;

/**
 * 物品VO
 */
@Data
public class ItemVO {
    private Integer id;
    private String name;
    private String nameEn;
    private String categoryName;
    private Integer cost;
    private String description;
    private String spriteUrl;
}
