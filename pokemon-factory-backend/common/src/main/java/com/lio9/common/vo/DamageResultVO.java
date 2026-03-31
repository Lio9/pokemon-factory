package com.lio9.common.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 伤害计算结果VO
 * 包含详细的计算过程和修正因子
 */
@Data
public class DamageResultVO {
    // ==================== 伤害结果 ====================
    
    /**
     * 最小伤害
     */
    private Integer minDamage;
    
    /**
     * 最大伤害
     */
    private Integer maxDamage;
    
    /**
     * 平均伤害
     */
    private Double avgDamage;
    
    /**
     * 基础伤害 (未应用修正)
     */
    private Integer baseDamage;
    
    // ==================== 修正因子 ====================
    
    /**
     * 属性相性倍率
     */
    private Double typeEffectiveness;
    
    /**
     * 属性相性描述
     */
    private String effectivenessDesc;
    
    /**
     * 是否本系技能(STAB)
     */
    private Boolean isStab;
    
    /**
     * 本系加成倍率
     */
    private Double stabMultiplier;
    
    /**
     * 天气修正倍率
     */
    private Double weatherMultiplier;
    
    /**
     * 暴击倍率
     */
    private Double criticalMultiplier;
    
    /**
     * 随机因子范围
     */
    private String randomRange;
    
    /**
     * 特性修正倍率
     */
    private Double abilityMultiplier;
    
    /**
     * 道具修正倍率
     */
    private Double itemMultiplier;
    
    /**
     * 能力等级修正倍率
     */
    private Double boostMultiplier;
    
    /**
     * 场地修正倍率 (反射壁/光墙/极光幕)
     */
    private Double screenMultiplier;
    
    /**
     * 灼伤修正
     */
    private Double burnMultiplier;
    
    /**
     * 多目标修正
     */
    private Double multiTargetMultiplier;
    
    // ==================== 命中率相关 ====================
    
    /**
     * 基础命中率
     */
    private Integer baseAccuracy;
    
    /**
     * 最终命中率（考虑能力等级、天气等修正）
     */
    private Double finalAccuracy;
    
    /**
     * 命中率描述
     */
    private String accuracyDesc;
    
    /**
     * 技能优先度
     */
    private Integer priority;
    
    /**
     * 是否接触技能
     */
    private Boolean isContact;
    
    /**
     * 连续攻击次数
     */
    private Integer hits;
    
    /**
     * 反伤比例
     */
    private Integer recoil;
    
    // ==================== 技能信息 ====================
    
    /**
     * 技能威力
     */
    private Integer movePower;
    
    /**
     * 实际使用的技能威力 (经过修正后)
     */
    private Integer effectivePower;
    
    /**
     * 技能分类 (物理/特殊/变化)
     */
    private String damageClass;
    
    /**
     * 技能属性名称
     */
    private String moveTypeName;
    
    // ==================== 能力值信息 ====================
    
    /**
     * 使用的能力值 (攻击或特攻)
     */
    private Integer usedAttackStat;
    
    /**
     * 使用的能力值类型 (attack/spAttack)
     */
    private String usedAttackType;
    
    /**
     * 防御方使用的能力值 (防御或特防)
     */
    private Integer usedDefenseStat;
    
    // ==================== 特性影响详情 ====================
    
    /**
     * 攻击方特性效果描述
     */
    private String attackerAbilityEffect;
    
    /**
     * 防御方特性效果描述
     */
    private String defenderAbilityEffect;
    
    // ==================== 道具影响详情 ====================
    
    /**
     * 攻击方道具效果描述
     */
    private String attackerItemEffect;
    
    /**
     * 防御方道具效果描述
     */
    private String defenderItemEffect;
    
    // ==================== 击杀预估 ====================
    
    private KOsEstimate koEstimate;
    
    /**
     * 击杀预估
     */
    @Data
    public static class KOsEstimate {
        private Integer defenderHp;
        private Integer minHits;
        private Integer maxHits;
        private Double avgHits;
        private Double koChance;
        private String koPercentRange;
    }
    
    // ==================== 计算过程详情 ====================
    
    /**
     * 计算过程详情
     */
    private List<CalculationStep> calculationSteps;
    
    /**
     * 所有修正因子的汇总
     */
    private Map<String, Double> allMultipliers;
    
    /**
     * 计算步骤详情
     */
    @Data
    public static class CalculationStep {
        private String name;
        private String formula;
        private Double value;
        private String description;
        private String category; // base, ability, item, weather, terrain, etc.
    }
    
    // ==================== 调试信息 ====================
    
    /**
     * 计算过程中的中间值 (用于调试)
     */
    private Map<String, Object> debugInfo;
}