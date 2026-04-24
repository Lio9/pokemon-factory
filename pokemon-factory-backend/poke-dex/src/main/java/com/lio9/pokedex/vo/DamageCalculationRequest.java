package com.lio9.pokedex.vo;

import lombok.Data;

/**
 * 伤害计算请求VO
 * 基于宝可梦标准伤害公式
 */
@Data
public class DamageCalculationRequest {
    /**
     * 攻击方形态ID
     */
    private Integer attackerFormId;
    
    /**
     * 防御方形态ID
     */
    private Integer defenderFormId;
    
    /**
     * 技能ID
     */
    private Integer moveId;
    
    private Integer attackerLevel = 50;
    private Integer attackerTerastalType;
    private String attackerAbilityEffect;
    private String defenderAbilityEffect;
    private String attackerItemEffect;
    private String defenderItemEffect;
    private Integer attackerAbilityId;
    private Integer attackerItemId;
    private Integer attackerAttack;
    private Integer attackerSpAttack;
    private Integer attackerAttackBoost = 0;
    private Integer attackerSpAttackBoost = 0;
    private Integer attackerSpeedBoost = 0;
    private Integer attackerAccuracyBoost = 0;
    private Integer attackerHpPercent = 100;
    private Boolean attackerBurned = false;
    private Boolean attackerPoisoned = false;
    private Boolean attackerParalyzed = false;
    private Integer defenderAbilityId;
    private Integer defenderItemId;
    private Integer defenderHp;
    private Integer defenderDefense;
    private Integer defenderSpDefense;
    private Integer defenderDefenseBoost = 0;
    private Integer defenderSpDefenseBoost = 0;
    private Integer defenderSpeedBoost = 0;
    private Integer defenderEvasionBoost = 0;
    private Integer defenderHpPercent = 100;
    private Boolean defenderBurned = false;
    private Boolean defenderPoisoned = false;
    private Boolean defenderParalyzed = false;
    private Boolean defenderAsleep = false;
    private Boolean defenderFrozen = false;
    private Boolean isCritical = false;
    private String weather;
    private String terrain;
    private Boolean isDoubleBattle = false;
    private Boolean reflectActive = false;
    private Boolean lightScreenActive = false;
    private Boolean auroraVeilActive = false;
    private Integer generation = 9;
}