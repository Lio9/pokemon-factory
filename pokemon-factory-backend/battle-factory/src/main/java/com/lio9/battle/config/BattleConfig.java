package com.lio9.battle.config;



/**
 * BattleConfig 文件说明
 * 所属模块：battle-factory 后端模块。
 * 文件类型：后端配置文件。
 * 核心职责：负责模块启动时的 Bean、序列化、数据源或异常处理配置。
 * 阅读建议：建议优先关注对运行期行为有全局影响的配置项。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 战斗系统统一配置类
 * 集中管理所有战斗相关的魔法数字和常量
 */
@Component
@ConfigurationProperties(prefix = "pokemon.battle")
public class BattleConfig {

    /**
     * 宝可梦等级 (默认50级)
     */
    private int level = 50;

    /**
     * 工厂挑战队伍大小
     */
    private int factoryTeamSize = 6;

    /**
     * 普通对战队伍大小
     */
    private int battleTeamSize = 4;

    /**
     * 场上活跃槽位数 (双打模式)
     */
    private int activeSlots = 2;

    /**
     * 最大回合数限制
     */
    private int maxRounds = 12;

    /**
     * 工厂挑战最大战斗轮数
     */
    private int maxFactoryBattles = 9;

    /**
     * 积分配置
     */
    private PointsConfig points = new PointsConfig();

    /**
     * 特殊系统概率配置
     */
    private SpecialSystemConfig specialSystems = new SpecialSystemConfig();

    /**
     * Mega进化数值加成配置
     */
    private MegaEvolutionConfig megaEvolution = new MegaEvolutionConfig();

    /**
     * 对手池匹配配置
     */
    private MatchmakingConfig matchmaking = new MatchmakingConfig();

    /**
     * 段位限制配置
     */
    private TierRestrictionConfig tierRestrictions = new TierRestrictionConfig();

    // Getters and Setters

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getFactoryTeamSize() {
        return factoryTeamSize;
    }

    public void setFactoryTeamSize(int factoryTeamSize) {
        this.factoryTeamSize = factoryTeamSize;
    }

    public int getBattleTeamSize() {
        return battleTeamSize;
    }

    public void setBattleTeamSize(int battleTeamSize) {
        this.battleTeamSize = battleTeamSize;
    }

    public int getActiveSlots() {
        return activeSlots;
    }

    public void setActiveSlots(int activeSlots) {
        this.activeSlots = activeSlots;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getMaxFactoryBattles() {
        return maxFactoryBattles;
    }

    public void setMaxFactoryBattles(int maxFactoryBattles) {
        this.maxFactoryBattles = maxFactoryBattles;
    }

    public PointsConfig getPoints() {
        return points;
    }

    public void setPoints(PointsConfig points) {
        this.points = points;
    }

    public SpecialSystemConfig getSpecialSystems() {
        return specialSystems;
    }

    public void setSpecialSystems(SpecialSystemConfig specialSystems) {
        this.specialSystems = specialSystems;
    }

    public MegaEvolutionConfig getMegaEvolution() {
        return megaEvolution;
    }

    public void setMegaEvolution(MegaEvolutionConfig megaEvolution) {
        this.megaEvolution = megaEvolution;
    }

    public MatchmakingConfig getMatchmaking() {
        return matchmaking;
    }

    public void setMatchmaking(MatchmakingConfig matchmaking) {
        this.matchmaking = matchmaking;
    }

    public TierRestrictionConfig getTierRestrictions() {
        return tierRestrictions;
    }

    public void setTierRestrictions(TierRestrictionConfig tierRestrictions) {
        this.tierRestrictions = tierRestrictions;
    }

    /**
     * 积分配置
     */
    public static class PointsConfig {
        /**
         * 胜利获得积分
         */
        private int winPoints = 10;

        /**
         * 失败扣除积分
         */
        private int losePoints = 3;

        /**
         * 每段位所需积分
         */
        private int pointsPerTier = 2000;

        public int getWinPoints() {
            return winPoints;
        }

        public void setWinPoints(int winPoints) {
            this.winPoints = winPoints;
        }

        public int getLosePoints() {
            return losePoints;
        }

        public void setLosePoints(int losePoints) {
            this.losePoints = losePoints;
        }

        public int getPointsPerTier() {
            return pointsPerTier;
        }

        public void setPointsPerTier(int pointsPerTier) {
            this.pointsPerTier = pointsPerTier;
        }
    }

    /**
     * 特殊系统概率配置 (百分比)
     */
    public static class SpecialSystemConfig {
        /**
         * Mega进化概率 (%)
         */
        private int megaChance = 18;

        /**
         * Z招式概率 (%)
         */
        private int zMoveChance = 16;

        /**
         * 极巨化概率 (%)
         */
        private int dynamaxChance = 16;

        /**
         * 太晶化概率 (%)
         */
        private int teraChance = 0; // 暂未启用

        public int getMegaChance() {
            return megaChance;
        }

        public void setMegaChance(int megaChance) {
            this.megaChance = megaChance;
        }

        public int getzMoveChance() {
            return zMoveChance;
        }

        public void setzMoveChance(int zMoveChance) {
            this.zMoveChance = zMoveChance;
        }

        public int getDynamaxChance() {
            return dynamaxChance;
        }

        public void setDynamaxChance(int dynamaxChance) {
            this.dynamaxChance = dynamaxChance;
        }

        public int getTeraChance() {
            return teraChance;
        }

        public void setTeraChance(int teraChance) {
            this.teraChance = teraChance;
        }

        /**
         * 无特殊系统的概率
         */
        public int getNoneChance() {
            return 100 - megaChance - zMoveChance - dynamaxChance - teraChance;
        }
    }

    /**
     * Mega进化数值加成配置
     */
    public static class MegaEvolutionConfig {
        /**
         * 攻击加成系数
         */
        private double attackMultiplier = 1.18;

        /**
         * 防御加成系数
         */
        private double defenseMultiplier = 1.12;

        /**
         * 速度加成系数
         */
        private double speedMultiplier = 1.1;

        public double getAttackMultiplier() {
            return attackMultiplier;
        }

        public void setAttackMultiplier(double attackMultiplier) {
            this.attackMultiplier = attackMultiplier;
        }

        public double getDefenseMultiplier() {
            return defenseMultiplier;
        }

        public void setDefenseMultiplier(double defenseMultiplier) {
            this.defenseMultiplier = defenseMultiplier;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }

        public void setSpeedMultiplier(double speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
        }
    }

    /**
     * 匹配配置
     */
    public static class MatchmakingConfig {
        /**
         * 对手池抽样窗口大小
         */
        private int poolWindow = 5;

        /**
         * 每次抽样数量
         */
        private int poolSampleSize = 3;

        /**
         * AI生成时对手强度加成 (rank+1)
         */
        private int aiOpponentRankBonus = 1;

        public int getPoolWindow() {
            return poolWindow;
        }

        public void setPoolWindow(int poolWindow) {
            this.poolWindow = poolWindow;
        }

        public int getPoolSampleSize() {
            return poolSampleSize;
        }

        public void setPoolSampleSize(int poolSampleSize) {
            this.poolSampleSize = poolSampleSize;
        }

        public int getAiOpponentRankBonus() {
            return aiOpponentRankBonus;
        }

        public void setAiOpponentRankBonus(int aiOpponentRankBonus) {
            this.aiOpponentRankBonus = aiOpponentRankBonus;
        }
    }

    /**
     * 段位限制配置
     */
    public static class TierRestrictionConfig {
        /**
         * 最低允许段位
         */
        private int minTier = 0;

        /**
         * 最高允许段位
         */
        private int maxTier = 4;

        /**
         * 是否启用严格分流
         */
        private boolean strictMode = true;

        public int getMinTier() {
            return minTier;
        }

        public void setMinTier(int minTier) {
            this.minTier = minTier;
        }

        public int getMaxTier() {
            return maxTier;
        }

        public void setMaxTier(int maxTier) {
            this.maxTier = maxTier;
        }

        public boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }
    }
}
