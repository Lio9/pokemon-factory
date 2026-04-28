package com.lio9.pokedex.util;



/**
 * DamageCalculatorUtil 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：工具类文件。
 * 核心职责：负责承载跨模块复用的通用计算或辅助处理逻辑。
 * 阅读建议：建议关注输入输出约束与可复用边界。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

/**
 * 伤害计算工具类
 * 包含各种伤害计算相关的常量和辅助方法
 */
public class DamageCalculatorUtil {
    
    private static final double[] STAGE_MULTIPLIERS = {
        2.0 / 8.0,
        2.0 / 7.0,
        2.0 / 6.0,
        2.0 / 5.0,
        2.0 / 4.0,
        2.0 / 3.0,
        2.0 / 2.0,
        3.0 / 2.0,
        4.0 / 2.0,
        5.0 / 2.0,
        6.0 / 2.0,
        7.0 / 2.0,
        8.0 / 2.0
    };
    
    public static double getStageMultiplier(int stage) {
        if (stage < -6) stage = -6;
        if (stage > 6) stage = 6;
        return STAGE_MULTIPLIERS[stage + 6];
    }
    
    public static final double CRITICAL_MULTIPLIER = 1.5;
    public static final double STAB_MULTIPLIER = 1.5;
    public static final double ADAPTABILITY_STAB_MULTIPLIER = 2.0;
    public static final double BURN_MULTIPLIER = 0.5;
    public static final double SCREEN_MULTIPLIER_DOUBLE = 2.0 / 3.0;
    public static final double SCREEN_MULTIPLIER_SINGLE = 0.5;
    public static final double AURORA_VEIL_MULTIPLIER_DOUBLE = 2.0 / 3.0;
    public static final double AURORA_VEIL_MULTIPLIER_SINGLE = 0.5;
    public static final double MULTI_TARGET_MULTIPLIER = 0.75;
    public static final String WEATHER_SUN = "sun";
    public static final String WEATHER_RAIN = "rain";
    public static final String WEATHER_SANDSTORM = "sandstorm";
    public static final String WEATHER_HAIL = "hail";
    public static final String WEATHER_SNOW = "snow";
    public static final String WEATHER_CLEAR = "clear";
    public static final String TERRAIN_ELECTRIC = "electric";
    public static final String TERRAIN_GRASSY = "grassy";
    public static final String TERRAIN_PSYCHIC = "psychic";
    public static final String TERRAIN_MISTY = "misty";
    public static final String TERRAIN_NONE = "none";
    public static final int DAMAGE_CLASS_PHYSICAL = 1;
    public static final int DAMAGE_CLASS_SPECIAL = 2;
    public static final int DAMAGE_CLASS_STATUS = 3;
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
    
    public static int calculateBaseDamage(int level, int power, int attack, int defense) {
        if (defense == 0) defense = 1;
        
        double part1 = (2.0 * level / 5.0 + 2.0);
        double part2 = part1 * power * attack / defense / 50.0 + 2.0;
        
        return (int) Math.floor(part2);
    }
    
    public static int[] calculateDamageRange(int baseDamage, double modifiers) {
        double min = baseDamage * modifiers * 0.85;
        double max = baseDamage * modifiers * 1.0;
        
        return new int[]{
            (int) Math.floor(min),
            (int) Math.floor(max)
        };
    }
}