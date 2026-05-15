package com.lio9.pokedex.service.impl;



import com.lio9.pokedex.util.AbilityEffects;
import com.lio9.pokedex.util.AbilityEffects.AbilityEffect;
import com.lio9.pokedex.util.ItemEffects;
import com.lio9.pokedex.util.ItemEffects.ItemEffect;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 效果计算服务
 * 统一处理特性、道具等效果的计算
 */
@Service
public class EffectCalculatorService {
    
    /**
     * 战斗上下文
     * 包含计算效果所需的所有信息
     */
    public static class BattleContext {
        // 攻击方信息
        public Integer attackerAbilityId;
        public Integer attackerItemId;
        public Integer attackerSpeciesId;
        public Integer attackerFormId;
        public Set<Integer> attackerTypeIds = new HashSet<>();
        public int attackerHpPercent = 100;
        public int attackerLevel = 50;
        public boolean attackerBurned = false;
        public boolean attackerPoisoned = false;
        public boolean attackerParalyzed = false;
        public boolean attackerAsleep = false;
        public boolean attackerFrozen = false;
        public int attackerAttackBoost = 0;
        public int attackerSpAttackBoost = 0;
        public boolean flashFireActivated = false;
        public boolean slowStartActive = false;
        public boolean opponentSwitchedIn = false;
        public boolean movesLast = false;
        public int consecutiveUseCount = 0;
        
        // 防御方信息
        public Integer defenderAbilityId;
        public Integer defenderItemId;
        public Integer defenderSpeciesId;
        public Integer defenderFormId;
        public Set<Integer> defenderTypeIds = new HashSet<>();
        public int defenderHpPercent = 100;
        public int defenderDefenseBoost = 0;
        public int defenderSpDefenseBoost = 0;
        public boolean defenderHasStatus = false;
        public boolean defenderIsFullyEvolved = true;
        
        // 技能信息
        public Integer moveId;
        public Integer moveTypeId;
        public Integer movePower;
        public String damageClass; // "physical", "special", "status"
        public boolean isPunchingMove = false;
        public boolean isBitingMove = false;
        public boolean isSoundMove = false;
        public boolean isContactMove = false;
        public boolean isPulseMove = false;
        public boolean isSlicingMove = false;
        public boolean isBulletMove = false;
        public boolean hasSecondaryEffect = false;
        
        // 战斗状态
        public String weather; // "sunny", "rain", "sandstorm", "hail", ""
        public String terrain; // "electric", "grassy", "psychic", "misty", ""
        public boolean isDoubleBattle = false;
        public boolean isCritical = false;
        public boolean reflectActive = false;
        public boolean lightScreenActive = false;
        public boolean auroraVeilActive = false;
        public boolean allyHasFriendGuard = false;
        
        // 性别
        public String attackerGender;
        public String defenderGender;
        
        // 计算结果缓存
        public double typeEffectiveness = 1.0;
    }
    
    /**
     * 效果计算结果
     */
    public static class EffectResult {
        public double attackMultiplier = 1.0;
        public double spAttackMultiplier = 1.0;
        public double defenseMultiplier = 1.0;
        public double spDefenseMultiplier = 1.0;
        public double powerMultiplier = 1.0;
        public double damageMultiplier = 1.0;
        public double stabMultiplier = 1.5;
        public double criticalMultiplier = 1.0;
        
        public boolean preventCritical = false;
        public boolean ignoreScreens = false;
        public boolean ignoreDefenseBoost = false;
        public boolean ignoreAttackDrop = false;
        public boolean typeImmunity = false;
        public boolean groundImmunity = false;
        
        public List<String> attackerEffects = new ArrayList<>();
        public List<String> defenderEffects = new ArrayList<>();
        public List<String> itemEffects = new ArrayList<>();
        
        public Map<String, Double> allMultipliers = new LinkedHashMap<>();
    }
    
    /**
     * 计算所有效果
     */
    public EffectResult calculateAllEffects(BattleContext ctx) {
        EffectResult result = new EffectResult();
        
        // 1. 计算特性效果
        calculateAbilityEffects(ctx, result);
        
        // 2. 计算道具效果
        calculateItemEffects(ctx, result);
        
        // 3. 计算暴击
        if (ctx.isCritical && !result.preventCritical) {
            result.criticalMultiplier = 1.5;
        }
        
        // 4. 汇总所有修正因子
        result.allMultipliers.put("本系加成", result.stabMultiplier);
        result.allMultipliers.put("特性攻击", result.attackMultiplier * result.spAttackMultiplier);
        result.allMultipliers.put("特性威力", result.powerMultiplier);
        result.allMultipliers.put("特性防御", result.defenseMultiplier * result.spDefenseMultiplier);
        result.allMultipliers.put("道具", result.damageMultiplier);
        result.allMultipliers.put("暴击", result.criticalMultiplier);
        
        return result;
    }
    
    /**
     * 计算特性效果
     */
    private void calculateAbilityEffects(BattleContext ctx, EffectResult result) {
        // 攻击方特性
        if (ctx.attackerAbilityId != null) {
            List<AbilityEffect> effects = AbilityEffects.getEffects(ctx.attackerAbilityId);
            for (AbilityEffect effect : effects) {
                if (effect.affectsAttacker && checkCondition(ctx, effect)) {
                    applyAbilityEffect(effect, ctx, result, true);
                }
            }
        }
        
        // 防御方特性
        if (ctx.defenderAbilityId != null) {
            List<AbilityEffect> effects = AbilityEffects.getEffects(ctx.defenderAbilityId);
            for (AbilityEffect effect : effects) {
                if (effect.affectsDefender && checkCondition(ctx, effect)) {
                    applyAbilityEffect(effect, ctx, result, false);
                }
            }
        }
    }
    
    /**
     * 计算道具效果
     */
    private void calculateItemEffects(BattleContext ctx, EffectResult result) {
        // 攻击方道具
        if (ctx.attackerItemId != null) {
            List<ItemEffect> effects = ItemEffects.getEffects(ctx.attackerItemId);
            for (ItemEffect effect : effects) {
                if (effect.affectsAttacker && checkItemCondition(ctx, effect)) {
                    applyItemEffect(effect, ctx, result, true);
                }
            }
        }
        
        // 防御方道具
        if (ctx.defenderItemId != null) {
            List<ItemEffect> effects = ItemEffects.getEffects(ctx.defenderItemId);
            for (ItemEffect effect : effects) {
                if (effect.affectsDefender && checkItemCondition(ctx, effect)) {
                    applyItemEffect(effect, ctx, result, false);
                }
            }
        }
    }
    
    /**
     * 检查特性条件
     */
    private boolean checkCondition(BattleContext ctx, AbilityEffect effect) {
        if (effect.condition == null || effect.condition.isEmpty()) {
            // 检查属性匹配
            if (effect.typeIds != null && !effect.typeIds.isEmpty()) {
                return effect.typeIds.contains(ctx.moveTypeId);
            }
            return true;
        }
        
        switch (effect.condition) {
            case "hp_le_33":
                return ctx.attackerHpPercent <= 33;
            case "hp_le_50":
                return ctx.attackerHpPercent <= 50;
            case "full_hp":
                return ctx.defenderHpPercent >= 100;
            case "has_status":
                return ctx.attackerBurned || ctx.attackerPoisoned || ctx.attackerParalyzed 
                    || ctx.attackerAsleep || ctx.attackerFrozen;
            case "burned":
                return ctx.attackerBurned;
            case "poisoned":
                return ctx.attackerPoisoned;
            case "sunny":
                return "sunny".equals(ctx.weather) || "晴天".equals(ctx.weather);
            case "rain":
                return "rain".equals(ctx.weather) || "雨天".equals(ctx.weather);
            case "power_le_60":
                return ctx.movePower != null && ctx.movePower <= 60;
            case "super_effective":
                return ctx.typeEffectiveness > 1.0;
            case "not_very_effective":
                return ctx.typeEffectiveness > 0 && ctx.typeEffectiveness < 1.0;
            case "critical_hit":
                return ctx.isCritical;
            case "physical_move":
                return "physical".equals(ctx.damageClass) || "物理".equals(ctx.damageClass);
            case "special_move":
                return "special".equals(ctx.damageClass) || "特殊".equals(ctx.damageClass);
            case "punching_move":
                return ctx.isPunchingMove;
            case "biting_move":
                return ctx.isBitingMove;
            case "sound_move":
                return ctx.isSoundMove;
            case "contact_move":
                return ctx.isContactMove;
            case "pulse_move":
                return ctx.isPulseMove;
            case "slicing_move":
                return ctx.isSlicingMove;
            case "bullet_move":
                return ctx.isBulletMove;
            case "has_secondary_effect":
                return ctx.hasSecondaryEffect;
            case "flash_fire_activated":
                return ctx.flashFireActivated;
            case "slow_start_active":
                return ctx.slowStartActive;
            case "opponent_switched_in":
                return ctx.opponentSwitchedIn;
            case "moves_last":
                return ctx.movesLast;
            case "same_gender":
                return ctx.attackerGender != null && ctx.attackerGender.equals(ctx.defenderGender);
            case "opposite_gender":
                return ctx.attackerGender != null && !ctx.attackerGender.equals(ctx.defenderGender);
            case "normal_move":
                return ctx.moveTypeId != null && ctx.moveTypeId == 1; // Normal type
            case "double_battle_ally":
                return ctx.isDoubleBattle && ctx.allyHasFriendGuard;
            default:
                return true;
        }
    }
    
    /**
     * 检查道具条件
     */
    private boolean checkItemCondition(BattleContext ctx, ItemEffect effect) {
        if (effect.condition == null || effect.condition.isEmpty()) {
            if (effect.typeIds != null && !effect.typeIds.isEmpty()) {
                return effect.typeIds.contains(ctx.moveTypeId);
            }
            return true;
        }
        
        switch (effect.condition) {
            case "first_use":
                return ctx.consecutiveUseCount == 0;
            case "super_effective":
                return ctx.typeEffectiveness > 1.0;
            case "physical_move":
                return "physical".equals(ctx.damageClass) || "物理".equals(ctx.damageClass);
            case "special_move":
                return "special".equals(ctx.damageClass) || "特殊".equals(ctx.damageClass);
            case "contact_move":
                return ctx.isContactMove;
            case "not_fully_evolved":
                return !ctx.defenderIsFullyEvolved;
            case "consecutive_use":
                return ctx.consecutiveUseCount > 0;
            // 宝可梦特定道具
            case "pikachu":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 25;
            case "cubone_or_marowak":
                return ctx.attackerSpeciesId != null && (ctx.attackerSpeciesId == 104 || ctx.attackerSpeciesId == 105);
            case "clamperl":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 366;
            case "ditto":
                return ctx.defenderSpeciesId != null && ctx.defenderSpeciesId == 132;
            case "latios_or_latias":
                return ctx.attackerSpeciesId != null && (ctx.attackerSpeciesId == 381 || ctx.attackerSpeciesId == 380);
            case "dialga":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 483;
            case "palkia":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 484;
            case "giratina":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 487;
            case "farfetchd":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 83;
            case "chansey":
                return ctx.attackerSpeciesId != null && ctx.attackerSpeciesId == 113;
            default:
                return true;
        }
    }
    
    /**
     * 应用特性效果
     */
    private void applyAbilityEffect(AbilityEffect effect, BattleContext ctx, EffectResult result, boolean isAttacker) {
        switch (effect.effectType) {
            case ATTACK_MULTIPLIER:
                if (isAttacker) {
                    result.attackMultiplier *= effect.value;
                    result.attackerEffects.add(effect.description);
                }
                break;
            case SP_ATTACK_MULTIPLIER:
                if (isAttacker) {
                    result.spAttackMultiplier *= effect.value;
                    result.attackerEffects.add(effect.description);
                }
                break;
            case POWER_MULTIPLIER:
                result.powerMultiplier *= effect.value;
                result.attackerEffects.add(effect.description);
                break;
            case STAB_MULTIPLIER:
                result.stabMultiplier = effect.value;
                result.attackerEffects.add(effect.description);
                break;
            case DAMAGE_MULTIPLIER:
                result.damageMultiplier *= effect.value;
                if (isAttacker) {
                    result.attackerEffects.add(effect.description);
                } else {
                    result.defenderEffects.add(effect.description);
                }
                break;
            case DEFENSE_MULTIPLIER:
                result.defenseMultiplier *= effect.value;
                result.defenderEffects.add(effect.description);
                break;
            case SP_DEFENSE_MULTIPLIER:
                result.spDefenseMultiplier *= effect.value;
                result.defenderEffects.add(effect.description);
                break;
            case PREVENT_CRIT:
                result.preventCritical = true;
                result.defenderEffects.add(effect.description);
                break;
            case TYPE_IMMUNITY:
                // 如果技能类型匹配，则免疫
                if (effect.typeIds.contains(ctx.moveTypeId)) {
                    result.typeImmunity = true;
                    result.defenderEffects.add(effect.description);
                }
                break;
            case GROUND_IMMUNITY:
                if (ctx.moveTypeId != null && ctx.moveTypeId == 5) { // Ground type
                    result.groundImmunity = true;
                    result.defenderEffects.add(effect.description);
                }
                break;
            case IGNORE_DEFENSE_BOOST:
                result.ignoreDefenseBoost = true;
                result.attackerEffects.add(effect.description);
                break;
            case IGNORE_ATTACK_DROP:
                result.ignoreAttackDrop = true;
                result.attackerEffects.add(effect.description);
                break;
            default:
                break;
        }
    }
    
    /**
     * 应用道具效果
     */
    private void applyItemEffect(ItemEffect effect, BattleContext ctx, EffectResult result, boolean isAttacker) {
        switch (effect.effectType) {
            case ATTACK_MULTIPLIER:
                if (isAttacker) {
                    result.attackMultiplier *= effect.value;
                    result.itemEffects.add(effect.description);
                }
                break;
            case SP_ATTACK_MULTIPLIER:
                if (isAttacker) {
                    result.spAttackMultiplier *= effect.value;
                    result.itemEffects.add(effect.description);
                }
                break;
            case POWER_MULTIPLIER:
                result.powerMultiplier *= effect.value;
                result.itemEffects.add(effect.description);
                break;
            case SUPER_EFFECTIVE_BOOST:
                if (ctx.typeEffectiveness > 1.0) {
                    result.damageMultiplier *= effect.value;
                    result.itemEffects.add(effect.description);
                }
                break;
            case DAMAGE_MULTIPLIER:
                result.damageMultiplier *= effect.value;
                result.itemEffects.add(effect.description);
                break;
            case DEFENSE_MULTIPLIER:
                result.defenseMultiplier *= effect.value;
                result.itemEffects.add(effect.description);
                break;
            case SP_DEFENSE_MULTIPLIER:
                result.spDefenseMultiplier *= effect.value;
                result.itemEffects.add(effect.description);
                break;
            case CRITICAL_RATE_BOOST:
                // 暴击率提升，在暴击判定中使用
                result.itemEffects.add(effect.description);
                break;
            default:
                break;
        }
    }
    
    /**
     * 计算属性相性
     */
    public double calculateTypeEffectiveness(Integer moveTypeId, Set<Integer> defenderTypeIds, 
            Map<Integer, Map<Integer, Integer>> efficacyMatrix) {
        if (moveTypeId == null || defenderTypeIds == null || defenderTypeIds.isEmpty()) {
            return 1.0;
        }
        
        double effectiveness = 1.0;
        Map<Integer, Integer> moveEfficacy = efficacyMatrix.get(moveTypeId);
        
        if (moveEfficacy == null) {
            return effectiveness;
        }
        
        for (Integer targetTypeId : defenderTypeIds) {
            Integer factor = moveEfficacy.get(targetTypeId);
            if (factor != null) {
                effectiveness *= factor / 100.0;
            }
        }
        
        return effectiveness;
    }
}
