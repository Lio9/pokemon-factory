package com.lio9.pokedex.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 宝可梦伤害计算工具类
 * 基于官方伤害公式实现
 * 
 * 基础公式: BaseDamage = ((2 * Level / 5 + 2) * Power * Attack / Defense / 50 + 2)
 * 最终伤害 = BaseDamage * Multipliers * Random(0.85-1.0)
 */
public class DamageCalculatorUtil {
    
    // ==================== 能力等级修正表 ====================
    
    /**
     * 获取能力等级修正倍率
     * @param boost 等级变化 (-6 到 +6)
     * @return 修正倍率
     */
    public static double getStatBoostMultiplier(int boost) {
        // 等级修正: (2 + boost) / 2，负数时为 2 / (2 - boost)
        if (boost >= 0) {
            return (2.0 + boost) / 2.0;
        } else {
            return 2.0 / (2.0 - boost);
        }
    }
    
    // ==================== 特性修正 ====================
    
    /**
     * 计算攻击方特性修正
     * @param abilityId 特性ID
     * @param moveTypeId 技能属性ID
     * @param attackerTypeIds 攻击方属性ID列表
     * @param attackerHpPercent 攻击方HP百分比
     * @param isPhysical 是否物理技能
     * @param isCritical 是否暴击
     * @param hasStatusProblem 是否有异常状态
     * @return 修正倍率和效果描述
     */
    public static Map<String, Object> calculateAttackerAbilityModifier(
            Integer abilityId, Integer moveTypeId, 
            java.util.Set<Integer> attackerTypeIds,
            int attackerHpPercent, boolean isPhysical, 
            boolean isCritical, boolean hasStatusProblem) {
        
        Map<String, Object> result = new HashMap<>();
        double multiplier = 1.0;
        String effect = null;
        
        if (abilityId == null) {
            result.put("multiplier", multiplier);
            result.put("effect", effect);
            return result;
        }
        
        // 根据特性ID计算修正
        switch (abilityId) {
            // 攻击提升特性
            case 4: // Huge Power / 纯朴
                if (isPhysical) {
                    multiplier = 2.0;
                    effect = "大力士：物理攻击翻倍";
                }
                break;
            case 5: // Pure Power / 纯朴
                if (isPhysical) {
                    multiplier = 2.0;
                    effect = "瑜伽之力：物理攻击翻倍";
                }
                break;
                
            // 属性强化特性 (HP <= 1/3 时)
            case 66: // Overgrow / 茂盛 (草)
                if (attackerHpPercent <= 33 && moveTypeId != null && moveTypeId == 12) { // 草
                    multiplier = 1.5;
                    effect = "茂盛：草属性技能威力提升50%";
                }
                break;
            case 67: // Blaze / 猛火 (火)
                if (attackerHpPercent <= 33 && moveTypeId != null && moveTypeId == 10) { // 火
                    multiplier = 1.5;
                    effect = "猛火：火属性技能威力提升50%";
                }
                break;
            case 68: // Torrent / 激流 (水)
                if (attackerHpPercent <= 33 && moveTypeId != null && moveTypeId == 11) { // 水
                    multiplier = 1.5;
                    effect = "激流：水属性技能威力提升50%";
                }
                break;
            case 69: // Swarm / 虫之预感 (虫)
                if (attackerHpPercent <= 33 && moveTypeId != null && moveTypeId == 7) { // 虫
                    multiplier = 1.5;
                    effect = "虫之预感：虫属性技能威力提升50%";
                }
                break;
                
            // 状态异常相关特性
            case 62: // Guts / 毅力
                if (hasStatusProblem && isPhysical) {
                    multiplier = 1.5;
                    effect = "毅力：异常状态下物理攻击提升50%";
                }
                break;
            case 53: // Quick Feet / 飞毛腿 (速度相关，不影响伤害)
                break;
                
            // 其他攻击特性
            case 48: // Technician / 技术员
                // 需要技能威力 <= 60，在外部处理
                break;
            case 54: // Hustle / 活力
                if (isPhysical) {
                    multiplier = 1.5;
                    effect = "活力：物理攻击提升50%";
                }
                break;
            case 74: // Solar Power / 太阳之力
                // 需要晴天，在外部处理
                break;
                
            // 适应力 (本系加成变为2x)
            case 91: // Adaptability / 适应力
                // 在STAB计算中处理
                break;
                
            // 分析 (后出手时伤害提升)
            case 148: // Analytic / 分析
                // 需要知道是否后出手，在外部处理
                break;
                
            // 铁拳 (拳类技能)
            case 89: // Iron Fist / 铁拳
                // 需要知道技能是否是拳类，在外部处理
                break;
                
            // 强行 (附加效果技能)
            case 115: // Sheer Force / 强行
                // 需要知道技能是否有附加效果，在外部处理
                break;
                
            // 技术高手 (威力<=60的技能威力x1.5)
            case 101: // Technician / 技术员
                // 在外部处理
                break;
                
            default:
                break;
        }
        
        result.put("multiplier", multiplier);
        result.put("effect", effect);
        return result;
    }
    
    /**
     * 计算防御方特性修正
     * @param abilityId 特性ID
     * @param moveTypeId 技能属性ID
     * @param defenderHpPercent 防御方HP百分比
     * @param isPhysical 是否物理技能
     * @param typeEffectiveness 属性相性倍率
     * @return 修正倍率和效果描述
     */
    public static Map<String, Object> calculateDefenderAbilityModifier(
            Integer abilityId, Integer moveTypeId,
            int defenderHpPercent, boolean isPhysical,
            double typeEffectiveness) {
        
        Map<String, Object> result = new HashMap<>();
        double multiplier = 1.0;
        String effect = null;
        
        if (abilityId == null) {
            result.put("multiplier", multiplier);
            result.put("effect", effect);
            return result;
        }
        
        switch (abilityId) {
            // 厚脂肪 (火/冰伤害减半)
            case 47: // Thick Fat / 厚脂肪
                if (moveTypeId != null && (moveTypeId == 10 || moveTypeId == 15)) { // 火或冰
                    multiplier = 0.5;
                    effect = "厚脂肪：火/冰属性技能伤害减半";
                }
                break;
                
            // 多重鳞片 (满血时伤害减半)
            case 105: // Multiscale / 多重鳞片
                if (defenderHpPercent >= 100) {
                    multiplier = 0.5;
                    effect = "多重鳞片：满血时伤害减半";
                }
                break;
                
            // 坚硬岩石/过滤 (效果绝佳伤害减少25%)
            case 108: // Solid Rock / 坚硬岩石
            case 109: // Filter / 过滤
                if (typeEffectiveness > 1.0) {
                    multiplier = 0.75;
                    effect = "效果绝佳伤害减少25%";
                }
                break;
                
            // 奇迹皮肤 (特殊技能伤害减半)
            case 144: // Wonder Skin / 奇迹皮肤
                if (!isPhysical) {
                    multiplier = 0.5;
                    effect = "奇迹皮肤：特殊技能伤害减半";
                }
                break;
                
            // 硬壳盔甲 (免疫暴击)
            case 49: // Shell Armor / 硬壳盔甲
            case 50: // Battle Armor / 战斗盔甲
                // 在暴击计算中处理
                break;
                
            // 干燥皮肤 (火伤害增加25%)
            case 87: // Dry Skin / 干燥皮肤
                if (moveTypeId != null && moveTypeId == 10) { // 火
                    multiplier = 1.25;
                    effect = "干燥皮肤：火属性技能伤害增加25%";
                }
                break;
                
            // 耐热 (火伤害减半)
            case 85: // Heatproof / 耐热
                if (moveTypeId != null && moveTypeId == 10) { // 火
                    multiplier = 0.5;
                    effect = "耐热：火属性技能伤害减半";
                }
                break;
                
            // 朋友防守 (双打中伤害减少25%)
            case 166: // Friend Guard / 朋友防守
                multiplier = 0.75;
                effect = "朋友防守：伤害减少25%";
                break;
                
            default:
                break;
        }
        
        result.put("multiplier", multiplier);
        result.put("effect", effect);
        return result;
    }
    
    // ==================== 道具修正 ====================
    
    /**
     * 计算攻击方道具修正
     */
    public static Map<String, Object> calculateAttackerItemModifier(
            Integer itemId, Integer moveTypeId, boolean isPhysical,
            boolean isSuperEffective, int movePower) {
        
        Map<String, Object> result = new HashMap<>();
        double multiplier = 1.0;
        String effect = null;
        
        if (itemId == null) {
            result.put("multiplier", multiplier);
            result.put("effect", effect);
            return result;
        }
        
        // 根据道具ID计算修正
        // 注：这里使用常见的道具ID，实际ID需要根据数据库
        
        // 讲究围巾/讲究眼镜 (攻击/特攻提升50%，但只能使用一个技能)
        if (itemId == 217 || itemId == 218) { // Choice Band / Choice Specs
            multiplier = 1.5;
            effect = isPhysical ? "讲究围巾：攻击提升50%" : "讲究眼镜：特攻提升50%";
        }
        
        // 生命宝珠 (伤害提升30%)
        if (itemId == 247) { // Life Orb
            multiplier = 1.3;
            effect = "生命宝珠：伤害提升30%";
        }
        
        // 专家腰带 (效果绝佳伤害提升20%)
        if (itemId == 262) { // Expert Belt
            if (isSuperEffective) {
                multiplier = 1.2;
                effect = "专家腰带：效果绝佳伤害提升20%";
            }
        }
        
        // 属性强化道具 (各属性板、香炉等，伤害提升20%)
        // 这里的ID需要根据数据库实际值
        
        // 力量强化道具
        if (itemId != null && itemId >= 234 && itemId <= 246) { // 各种属性强化道具
            multiplier = 1.2;
            effect = "属性强化道具：伤害提升20%";
        }
        
        // 肌肉强化/智慧眼镜 (物理/特殊伤害提升10%)
        if (itemId == 261) { // Muscle Band
            if (isPhysical) {
                multiplier = 1.1;
                effect = "肌肉强化：物理伤害提升10%";
            }
        }
        if (itemId == 263) { // Wise Glasses
            if (!isPhysical) {
                multiplier = 1.1;
                effect = "智慧眼镜：特殊伤害提升10%";
            }
        }
        
        result.put("multiplier", multiplier);
        result.put("effect", effect);
        return result;
    }
    
    /**
     * 计算防御方道具修正
     */
    public static Map<String, Object> calculateDefenderItemModifier(
            Integer itemId, boolean isPhysical) {
        
        Map<String, Object> result = new HashMap<>();
        double multiplier = 1.0;
        String effect = null;
        
        if (itemId == null) {
            result.put("multiplier", multiplier);
            result.put("effect", effect);
            return result;
        }
        
        // 进化奇石 (未进化宝可梦双防提升50%)
        // 需要在调用时判断是否进化
        
        // 特定属性减伤树果
        // 需要根据技能属性判断
        
        result.put("multiplier", multiplier);
        result.put("effect", effect);
        return result;
    }
    
    // ==================== 天气修正 ====================
    
    /**
     * 计算天气修正
     * @param weather 天气类型
     * @param moveTypeId 技能属性ID
     * @return 修正倍率
     */
    public static double calculateWeatherMultiplier(String weather, Integer moveTypeId) {
        if (weather == null || weather.isEmpty()) return 1.0;
        
        switch (weather) {
            case "晴天":
            case "Harsh Sunlight":
                if (moveTypeId != null && moveTypeId == 10) return 1.5; // 火系增强
                if (moveTypeId != null && moveTypeId == 11) return 0.5; // 水系减弱
                break;
            case "雨天":
            case "Rain":
                if (moveTypeId != null && moveTypeId == 11) return 1.5; // 水系增强
                if (moveTypeId != null && moveTypeId == 10) return 0.5; // 火系减弱
                break;
            case "沙暴":
            case "Sandstorm":
                // 岩石系特防提升1.5x（在防御计算中处理）
                break;
            case "冰雹":
            case "Hail":
                break;
        }
        
        return 1.0;
    }
    
    // ==================== 场地修正 ====================
    
    /**
     * 计算场地修正
     * @param terrain 场地类型
     * @param moveTypeId 技能属性ID
     * @return 修正倍率
     */
    public static double calculateTerrainMultiplier(String terrain, Integer moveTypeId) {
        if (terrain == null || terrain.isEmpty()) return 1.0;
        
        switch (terrain) {
            case "电气场地":
                if (moveTypeId != null && moveTypeId == 13) return 1.3; // 电系增强
                break;
            case "草地场地":
                if (moveTypeId != null && moveTypeId == 12) return 1.3; // 草系增强
                break;
            case "超能力场地":
                if (moveTypeId != null && moveTypeId == 14) return 1.3; // 超能系增强
                break;
            case "薄雾场地":
                // 龙系技能对场上宝可梦无效
                break;
        }
        
        return 1.0;
    }
    
    // ==================== 屏幕修正 ====================
    
    /**
     * 计算反射壁/光墙修正
     */
    public static double calculateScreenMultiplier(
            boolean isPhysical, boolean reflectActive, 
            boolean lightScreenActive, boolean auroraVeilActive,
            boolean isDoubleBattle, boolean isCritical) {
        
        // 暴击无视屏幕
        if (isCritical) return 1.0;
        
        double multiplier = 1.0;
        
        if (auroraVeilActive) {
            // 极光幕：物理和特殊都减半
            multiplier = isDoubleBattle ? 0.667 : 0.5;
        } else if (isPhysical && reflectActive) {
            // 反射壁：物理伤害减半
            multiplier = isDoubleBattle ? 0.667 : 0.5;
        } else if (!isPhysical && lightScreenActive) {
            // 光墙：特殊伤害减半
            multiplier = isDoubleBattle ? 0.667 : 0.5;
        }
        
        return multiplier;
    }
    
    // ==================== 其他修正 ====================
    
    /**
     * 计算灼伤修正 (物理攻击减半)
     */
    public static double calculateBurnMultiplier(boolean isPhysical, boolean isBurned, Integer abilityId) {
        // 毅力特性免疫灼伤减伤
        if (isPhysical && isBurned && abilityId != null && abilityId != 62) { // Guts
            return 0.5;
        }
        return 1.0;
    }
    
    /**
     * 计算多目标修正 (双打/三打中全体技能伤害减少)
     */
    public static double calculateMultiTargetMultiplier(boolean isDoubleBattle, boolean targetsMultiple) {
        if (isDoubleBattle && targetsMultiple) {
            return 0.75;
        }
        return 1.0;
    }
    
    /**
     * 计算基础伤害
     * 公式: ((2 * Level / 5 + 2) * Power * Attack / Defense / 50 + 2)
     */
    public static int calculateBaseDamage(int level, int power, int attack, int defense) {
        // 基础伤害计算
        double baseDamage = ((2.0 * level / 5.0 + 2.0) * power * attack / defense / 50.0 + 2.0);
        return (int) baseDamage;
    }
    
    /**
     * 应用修正因子 (使用游戏中的定点数计算方式)
     * Modifier是4096进制，需要乘以modifier后除以4096并四舍五入
     */
    public static int applyModifier(int damage, double multiplier) {
        return (int) Math.round(damage * multiplier);
    }
    
    /**
     * 计算属性相性描述
     */
    public static String getEffectivenessDesc(double effectiveness) {
        if (effectiveness == 0) return "无效";
        if (effectiveness >= 2.0) return String.format("效果绝佳(%.0fx)", effectiveness);
        if (effectiveness > 1.0) return "效果绝佳";
        if (effectiveness == 1.0) return "效果一般";
        if (effectiveness > 0) return String.format("效果不佳(%.2fx)", effectiveness);
        return "效果一般";
    }
    
    /**
     * 判断是否有效果绝佳
     */
    public static boolean isSuperEffective(double effectiveness) {
        return effectiveness > 1.0;
    }
    
    /**
     * 判断是否效果不佳
     */
    public static boolean isNotVeryEffective(double effectiveness) {
        return effectiveness > 0 && effectiveness < 1.0;
    }
}
