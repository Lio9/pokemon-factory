package com.lio9.common.util;

/**
 * 伤害计算工具类
 * 包含各种伤害计算相关的常量和辅助方法
 */
public class DamageCalculatorUtil {
    
    // 能力等级修正倍率表（-6到+6）
    private static final double[] STAGE_MULTIPLIERS = {
        2.0 / 8.0,   // -6
        2.0 / 7.0,   // -5
        2.0 / 6.0,   // -4
        2.0 / 5.0,   // -3
        2.0 / 4.0,   // -2
        2.0 / 3.0,   // -1
        2.0 / 2.0,   // 0
        3.0 / 2.0,   // +1
        4.0 / 2.0,   // +2
        5.0 / 2.0,   // +3
        6.0 / 2.0,   // +4
        7.0 / 2.0,   // +5
        8.0 / 2.0    // +6
    };
    
    /**
     * 获取能力等级修正倍率
     * @param stage 能力等级（-6到+6）
     * @return 修正倍率
     */
    public static double getStageMultiplier(int stage) {
        if (stage < -6) stage = -6;
        if (stage > 6) stage = 6;
        return STAGE_MULTIPLIERS[stage + 6];
    }
    
    /**
     * 暴击倍率（第9世代）
     */
    public static final double CRITICAL_MULTIPLIER = 1.5;
    
    /**
     * 默认STAB倍率
     */
    public static final double STAB_MULTIPLIER = 1.5;
    
    /**
     * 适应力特性STAB倍率
     */
    public static final double ADAPTABILITY_STAB_MULTIPLIER = 2.0;
    
    /**
     * 灼伤倍率（物理伤害减半）
     */
    public static final double BURN_MULTIPLIER = 0.5;
    
    /**
     * 反射壁/光墙倍率（双打）
     */
    public static final double SCREEN_MULTIPLIER_DOUBLE = 2.0 / 3.0;
    
    /**
     * 反射壁/光墙倍率（单打）
     */
    public static final double SCREEN_MULTIPLIER_SINGLE = 0.5;
    
    /**
     * 极光幕倍率（双打）
     */
    public static final double AURORA_VEIL_MULTIPLIER_DOUBLE = 2.0 / 3.0;
    
    /**
     * 极光幕倍率（单打）
     */
    public static final double AURORA_VEIL_MULTIPLIER_SINGLE = 0.5;
    
    /**
     * 多目标修正倍率
     */
    public static final double MULTI_TARGET_MULTIPLIER = 0.75;
    
    /**
     * 天气类型
     */
    public static final String WEATHER_SUN = "sun";
    public static final String WEATHER_RAIN = "rain";
    public static final String WEATHER_SANDSTORM = "sandstorm";
    public static final String WEATHER_HAIL = "hail";
    public static final String WEATHER_SNOW = "snow";
    public static final String WEATHER_CLEAR = "clear";
    
    /**
     * 场地类型
     */
    public static final String TERRAIN_ELECTRIC = "electric";
    public static final String TERRAIN_GRASSY = "grassy";
    public static final String TERRAIN_PSYCHIC = "psychic";
    public static final String TERRAIN_MISTY = "misty";
    public static final String TERRAIN_NONE = "none";
    
    /**
     * 伤害类型
     */
    public static final int DAMAGE_CLASS_PHYSICAL = 1;
    public static final int DAMAGE_CLASS_SPECIAL = 2;
    public static final int DAMAGE_CLASS_STATUS = 3;
    
    /**
     * 属性ID常量
     */
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_FIRE = 2;
    public static final int TYPE_WATER = 3;
    public static final int TYPE_ELECTRIC = 4;
    public static final int TYPE_GRASS = 5;
    public static final int TYPE_ICE = 6;
    public static final int TYPE_FIGHTING = 7;
    public static final int TYPE_POISON = 8;
    public static final int TYPE_GROUND = 9;
    public static final int TYPE_FLYING = 10;
    public static final int TYPE_PSYCHIC = 11;
    public static final int TYPE_BUG = 12;
    public static final int TYPE_ROCK = 13;
    public static final int TYPE_GHOST = 14;
    public static final int TYPE_DRAGON = 15;
    public static final int TYPE_DARK = 16;
    public static final int TYPE_STEEL = 17;
    public static final int TYPE_FAIRY = 18;
    
    /**
     * 计算基础伤害
     * 公式：floor(floor(floor(2 * 等级 / 5 + 2) * 威力 * 攻 / 防 / 50 + 2)
     * @param level 等级
     * @param power 威力
     * @param attack 攻击或特攻
     * @param defense 防御或特防
     * @return 基础伤害
     */
    public static int calculateBaseDamage(int level, int power, int attack, int defense) {
        if (defense == 0) defense = 1;
        
        double part1 = (2.0 * level / 5.0 + 2.0);
        double part2 = part1 * power * attack / defense / 50.0 + 2.0;
        
        return (int) Math.floor(part2);
    }
    
    /**
     * 计算修正后的伤害范围
     * @param baseDamage 基础伤害
     * @param modifiers 修正因子乘积
     * @return 最小和最大伤害
     */
    public static int[] calculateDamageRange(int baseDamage, double modifiers) {
        double min = baseDamage * modifiers * 0.85;
        double max = baseDamage * modifiers * 1.0;
        
        return new int[]{
            (int) Math.floor(min),
            (int) Math.floor(max)
        };
    }
}