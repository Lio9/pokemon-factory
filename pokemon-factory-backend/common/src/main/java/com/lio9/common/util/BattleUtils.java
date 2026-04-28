package com.lio9.common.util;



/**
 * BattleUtils 文件说明
 * 所属模块：common 公共模块。
 * 文件类型：工具类文件。
 * 核心职责：负责承载跨模块复用的通用计算或辅助处理逻辑。
 * 阅读建议：建议关注输入输出约束与可复用边界。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.Map;

/**
 * 战斗系统通用工具类
 * 提供常用的类型转换和数据处理方法
 */
public final class BattleUtils {

    private BattleUtils() {
        // 防止实例化
    }

    /**
     * 安全地将对象转换为 int
     *
     * @param value    要转换的值
     * @param fallback 转换失败时的默认值
     * @return 转换后的整数值
     */
    public static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                // 忽略异常，返回默认值
            }
        }
        return fallback;
    }

    /**
     * 安全地将对象转换为 long
     *
     * @param value    要转换的值
     * @param fallback 转换失败时的默认值
     * @return 转换后的长整数值
     */
    public static long toLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                // 忽略异常，返回默认值
            }
        }
        return fallback;
    }

    /**
     * 安全地将对象转换为 double
     *
     * @param value    要转换的值
     * @param fallback 转换失败时的默认值
     * @return 转换后的双精度浮点值
     */
    public static double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
                // 忽略异常，返回默认值
            }
        }
        return fallback;
    }

    /**
     * 安全地将对象转换为 String
     *
     * @param value    要转换的值
     * @param fallback 转换失败时的默认值
     * @return 转换后的字符串值
     */
    public static String toString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? fallback : str;
    }

    /**
     * 将对象转换为 Map
     *
     * @param value 要转换的值
     * @return 转换后的 Map，如果转换失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    /**
     * 规范化招式名称（去除连字符，转小写）
     *
     * @param nameEn 原始英文名称
     * @return 规范化后的名称
     */
    public static String normalizeMoveName(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            return "";
        }
        return nameEn.toLowerCase().replace("-", " ").trim();
    }

    /**
     * 检查招式名称是否匹配指定类型
     *
     * @param moveName 招式名称
     * @param patterns 要匹配的模式列表
     * @return 是否匹配
     */
    public static boolean matchesMovePattern(String moveName, String... patterns) {
        String normalized = normalizeMoveName(moveName);
        for (String pattern : patterns) {
            if (normalizeMoveName(pattern).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算 HP 百分比
     *
     * @param currentHp 当前 HP
     * @param maxHp     最大 HP
     * @return HP 百分比 (0-100)
     */
    public static int calculateHpPercentage(int currentHp, int maxHp) {
        if (maxHp <= 0) {
            return 0;
        }
        return (currentHp * 100) / maxHp;
    }

    /**
     * 判断是否为满 HP 状态
     *
     * @param currentHp 当前 HP
     * @param maxHp     最大 HP
     * @return 是否满 HP
     */
    public static boolean isFullHp(int currentHp, int maxHp) {
        return currentHp >= maxHp && maxHp > 0;
    }

    /**
     * 限制值在指定范围内
     *
     * @param value 要限制的值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制值在指定范围内 (double)
     *
     * @param value 要限制的值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
