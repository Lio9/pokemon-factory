package com.lio9.pokedex.vo;



/**
 * DamageResultVO 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：视图对象文件。
 * 核心职责：负责封装面向接口返回或查询条件的展示层数据结构。
 * 阅读建议：建议结合控制器和 service 的组装逻辑一起阅读。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 伤害计算结果VO
 * 包含详细的计算过程和修正因子
 */
@Data
public class DamageResultVO {
    private Integer minDamage;
    private Integer maxDamage;
    private Double avgDamage;
    private Integer baseDamage;
    private Double typeEffectiveness;
    private String effectivenessDesc;
    private Boolean isStab;
    private Double stabMultiplier;
    private Double weatherMultiplier;
    private Double criticalMultiplier;
    private String randomRange;
    private Double abilityMultiplier;
    private Double itemMultiplier;
    private Double boostMultiplier;
    private Double screenMultiplier;
    private Double burnMultiplier;
    private Double multiTargetMultiplier;
    private Integer baseAccuracy;
    private Double finalAccuracy;
    private String accuracyDesc;
    private Integer priority;
    private Boolean isContact;
    private Integer hits;
    private Integer recoil;
    private Integer movePower;
    private Integer effectivePower;
    private String damageClass;
    private String moveTypeName;
    private Integer usedAttackStat;
    private String usedAttackType;
    private Integer usedDefenseStat;
    private String attackerAbilityEffect;
    private String defenderAbilityEffect;
    private String attackerItemEffect;
    private String defenderItemEffect;
    private KOsEstimate koEstimate;
    private List<CalculationStep> calculationSteps;
    private Map<String, Double> allMultipliers;
    private Map<String, Object> debugInfo;

    @Data
    public static class KOsEstimate {
        private Integer defenderHp;
        private Integer minHits;
        private Integer maxHits;
        private Double avgHits;
        private Double koChance;
        private String koPercentRange;
    }

    @Data
    public static class CalculationStep {
        private String name;
        private String formula;
        private Double value;
        private String description;
        private String category;
    }
}