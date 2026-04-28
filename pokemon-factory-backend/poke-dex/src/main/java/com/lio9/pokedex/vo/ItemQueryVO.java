package com.lio9.pokedex.vo;



/**
 * ItemQueryVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

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