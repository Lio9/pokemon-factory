package com.lio9.common.vo;

import lombok.Data;
import java.util.List;

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
    
    // ==================== 攻击方参数 ====================
    
    /**
     * 攻击方等级 (默认50)
     */
    private Integer attackerLevel = 50;
    
    /**
     * 攻击方特性ID
     */
    private Integer attackerAbilityId;
    
    /**
     * 攻击方道具ID
     */
    private Integer attackerItemId;
    
    /**
     * 攻击方实际攻击值 (可覆盖计算值)
     */
    private Integer attackerAttack;
    
    /**
     * 攻击方实际特攻值 (可覆盖计算值)
     */
    private Integer attackerSpAttack;
    
    /**
     * 攻击方攻击等级 (-6 到 +6)
     */
    private Integer attackerAttackBoost = 0;
    
    /**
     * 攻击方特攻等级 (-6 到 +6)
     */
    private Integer attackerSpAttackBoost = 0;
    
    /**
     * 攻击方速度等级 (-6 到 +6)
     */
    private Integer attackerSpeedBoost = 0;
    
    /**
     * 攻击方命中等级 (-6 到 +6)
     */
    private Integer attackerAccuracyBoost = 0;
    
    /**
     * 攻击方当前HP百分比 (0-100, 用于某些特性/技能)
     */
    private Integer attackerHpPercent = 100;
    
    /**
     * 攻击方是否处于灼伤状态
     */
    private Boolean attackerBurned = false;
    
    /**
     * 攻击方是否处于中毒/剧毒状态
     */
    private Boolean attackerPoisoned = false;
    
    /**
     * 攻击方是否处于麻痹状态
     */
    private Boolean attackerParalyzed = false;
    
    // ==================== 防御方参数 ====================
    
    /**
     * 防御方特性ID
     */
    private Integer defenderAbilityId;
    
    /**
     * 防御方道具ID
     */
    private Integer defenderItemId;
    
    /**
     * 防御方实际HP值 (用于击杀预估)
     */
    private Integer defenderHp;
    
    /**
     * 防御方实际防御值 (可覆盖计算值)
     */
    private Integer defenderDefense;
    
    /**
     * 防御方实际特防值 (可覆盖计算值)
     */
    private Integer defenderSpDefense;
    
    /**
     * 防御方防御等级 (-6 到 +6)
     */
    private Integer defenderDefenseBoost = 0;
    
    /**
     * 防御方特防等级 (-6 到 +6)
     */
    private Integer defenderSpDefenseBoost = 0;
    
    /**
     * 防御方速度等级 (-6 到 +6)
     */
    private Integer defenderSpeedBoost = 0;
    
    /**
     * 防御方闪避等级 (-6 到 +6)
     */
    private Integer defenderEvasionBoost = 0;
    
    /**
     * 防御方当前HP百分比 (用于某些特性)
     */
    private Integer defenderHpPercent = 100;
    
    /**
     * 防御方是否处于灼伤状态
     */
    private Boolean defenderBurned = false;
    
    /**
     * 防御方是否处于中毒/剧毒状态
     */
    private Boolean defenderPoisoned = false;
    
    /**
     * 防御方是否处于麻痹状态
     */
    private Boolean defenderParalyzed = false;
    
    /**
     * 防御方是否处于睡眠状态
     */
    private Boolean defenderAsleep = false;
    
    /**
     * 防御方是否处于冰冻状态
     */
    private Boolean defenderFrozen = false;
    
    // ==================== 战斗环境参数 ====================
    
    /**
     * 是否暴击
     */
    private Boolean isCritical = false;
    
    /**
     * 天气 (晴天/雨天/沙暴/冰雹/无)
     */
    private String weather;
    
    /**
     * 场地状态列表 (电气场地/草地场地/超能力场地/薄雾场地)
     */
    private String terrain;
    
    /**
     * 是否双打/三打 (影响某些技能威力)
     */
    private Boolean isDoubleBattle = false;
    
    /**
     * 反射壁是否激活 (物理伤害减半)
     */
    private Boolean reflectActive = false;
    
    /**
     * 光墙是否激活 (特殊伤害减半)
     */
    private Boolean lightScreenActive = false;
    
    /**
     * 极光幕是否激活 (双防减半)
     */
    private Boolean auroraVeilActive = false;
    
    /**
     * 世代 (默认第9世代)
     */
    private Integer generation = 9;
}