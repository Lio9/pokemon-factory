package com.lio9.pokedex.vo;



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