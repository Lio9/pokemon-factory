package com.lio9.battle.service;

import java.util.Map;

/**
 * 段位系统服务。
 * <p>
 * 段位层级（从低到高）：
 *   0 = 精灵球（0-1999 分）
 *   1 = 超级球（2000-3999 分）
 *   2 = 高级球（4000-5999 分）
 *   3 = 大师球（6000+ 分）
 * </p>
 */
public class TierService {

    public static final int TIER_POKE_BALL = 0;
    public static final int TIER_GREAT_BALL = 1;
    public static final int TIER_ULTRA_BALL = 2;
    public static final int TIER_MASTER_BALL = 3;

    public static final int POINTS_PER_TIER = 2000;

    private static final String[] TIER_NAMES = {"精灵球", "超级球", "高级球", "大师球"};

    /**
     * 段位名称。
     */
    public static String tierName(int tier) {
        if (tier < 0 || tier >= TIER_NAMES.length) return TIER_NAMES[0];
        return TIER_NAMES[tier];
    }

    /**
     * 完成一次工厂挑战后的积分奖励。
     * 段位越低积分越高，鼓励新手快速晋升。
     * <p>
     * 公式：baseReward = (4 - tier) * 50 + wins * 30 - losses * 10
     * 9胜0负精灵球：4*50 + 9*30 = 200+270 = 470 分
     * 9胜0负大师球：1*50 + 9*30 = 50+270 = 320 分
     * 5胜4负精灵球：200 + 150 - 40 = 310 分
     * 0胜9负精灵球：200 + 0 - 90 = 110 分（仍有一点安慰奖）
     * </p>
     */
    public static int calculateRunReward(int tier, int wins, int losses) {
        int tierMultiplier = Math.max(1, 4 - tier);
        int reward = tierMultiplier * 50 + wins * 30 - losses * 10;
        return Math.max(0, reward);
    }

    /**
     * 单场对战的积分变化（用于非工厂模式的单场对战）。
     */
    public static int calculateSingleBattleReward(int tier, boolean won) {
        if (won) {
            return Math.max(10, (4 - tier) * 15);
        } else {
            return -Math.max(5, tier * 5 + 5);
        }
    }

    /**
     * 根据当前段位积分和变化量，计算新的段位和段位积分。
     *
     * @return {tier, tierPoints, totalPoints, promoted, demoted}
     */
    public static Map<String, Object> applyPoints(int currentTier, int currentTierPoints, int currentTotalPoints, int delta) {
        int newTotalPoints = Math.max(0, currentTotalPoints + delta);
        int newTierPoints = currentTierPoints + delta;
        int newTier = currentTier;
        boolean promoted = false;
        boolean demoted = false;

        // 晋级判定
        if (newTier < TIER_MASTER_BALL && newTierPoints >= POINTS_PER_TIER) {
            newTier++;
            newTierPoints = 0;
            promoted = true;
        }

        // 降级判定
        if (newTierPoints < 0 && newTier > TIER_POKE_BALL) {
            newTier--;
            newTierPoints = POINTS_PER_TIER - 1;
            demoted = true;
        }

        // 精灵球不能再降
        if (newTierPoints < 0 && newTier == TIER_POKE_BALL) {
            newTierPoints = 0;
        }

        // 大师球不重置积分
        if (newTier == TIER_MASTER_BALL && promoted) {
            newTierPoints = newTotalPoints - TIER_MASTER_BALL * POINTS_PER_TIER;
        }

        return Map.of(
                "tier", newTier,
                "tierPoints", newTierPoints,
                "totalPoints", newTotalPoints,
                "promoted", promoted,
                "demoted", demoted
        );
    }
}
