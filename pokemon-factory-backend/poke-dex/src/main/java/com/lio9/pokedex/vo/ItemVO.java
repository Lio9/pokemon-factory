package com.lio9.pokedex.vo;



/**
 * ItemVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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