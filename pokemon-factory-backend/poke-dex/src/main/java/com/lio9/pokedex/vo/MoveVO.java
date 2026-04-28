package com.lio9.pokedex.vo;



/**
 * MoveVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Data;

/**
 * 技能VO
 */
@Data
public class MoveVO {
    private Integer id;
    private String name;
    private String nameEn;
    private String typeName;
    private String typeColor;
    private String damageClass;
    private Integer power;
    private Integer accuracy;
    private Integer pp;
    private Integer priority;
    private String description;
    private String learnMethod;
    private Integer level;
    
    private Boolean isContact;
    private Integer hits;
    private Integer recoil;
    private Boolean isCharging;
    private Integer chargingTurns;
    private Boolean isZMove;
    private Boolean isGMaxMove;
    private String moveCategory;
}