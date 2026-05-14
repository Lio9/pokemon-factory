package com.lio9.battle.engine;

/**
 * AI难度等级枚举
 * 
 * 定义四种AI难度级别，从简单到专家：
 * - EASY: 随机选择 + 简单启发式，适合新手练习
 * - NORMAL: 基础伤害预测 + 类型克制，标准对战体验
 * - HARD: 多步预测 + 状态评估，具有挑战性
 * - EXPERT: Minimax算法 + 蒙特卡洛树搜索，最高难度
 */
public enum AIDifficulty {
    /**
     * 简单难度
     * - 随机选择招式
     * - 基本的类型克制意识
     * - 不考虑伤害计算
     * - 换人决策简单
     */
    EASY,
    
    /**
     * 普通难度（默认）
     * - 基于伤害预测选择招式
     * - 考虑属性克制和STAB
     * - 简单的威胁评估
     * - 基本的换人判断
     */
    NORMAL,
    
    /**
     * 困难难度
     * - 2步lookahead预测
     * - 对手建模和行动预测
     * - 综合状态评估
     * - 智能换人系统
     * - 资源管理（PP、道具）
     */
    HARD,
    
    /**
     * 专家难度
     * - Minimax算法决策
     * - 蒙特卡洛树搜索（可选）
     * - 深度策略分析
     * - 长期规划能力
     * - 完美的资源管理
     */
    EXPERT;
    
    /**
     * 获取难度的描述信息
     */
    public String getDescription() {
        return switch (this) {
            case EASY -> "简单 - 随机选择 + 基础启发式";
            case NORMAL -> "普通 - 伤害预测 + 类型克制";
            case HARD -> "困难 - 多步预测 + 状态评估";
            case EXPERT -> "专家 - Minimax算法 + 深度策略";
        };
    }
    
    /**
     * 是否启用伤害预测
     */
    public boolean useDamagePrediction() {
        return this != EASY;
    }
    
    /**
     * 是否启用多步预测
     */
    public boolean useLookahead() {
        return this == HARD || this == EXPERT;
    }
    
    /**
     * lookahead的深度
     */
    public int getLookaheadDepth() {
        return switch (this) {
            case EASY, NORMAL -> 0;
            case HARD -> 2;
            case EXPERT -> 3;
        };
    }
    
    /**
     * 是否启用资源管理
     */
    public boolean useResourceManagement() {
        return this == HARD || this == EXPERT;
    }
    
    /**
     * 是否启用长期策略
     */
    public boolean useLongTermStrategy() {
        return this == EXPERT;
    }
}
