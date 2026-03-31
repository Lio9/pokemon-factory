package com.lio9.common.vo;

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
    
    // 新增字段
    private Boolean isContact;              // 是否接触技能
    private Integer hits;                    // 连续攻击次数（2-5次等）
    private Integer recoil;                  // 反伤比例（如33表示33%）
    private Boolean isCharging;              // 是否蓄力攻击
    private Integer chargingTurns;           // 蓄力回合数
    private Boolean isZMove;                 // 是否Z招式
    private Boolean isGMaxMove;              // 是否极巨招式
    private String moveCategory;             // 技能分类（连续攻击、蓄力攻击等）
}
