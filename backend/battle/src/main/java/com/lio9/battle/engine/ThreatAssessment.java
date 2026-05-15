package com.lio9.battle.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 威胁评估结果
 * 
 * 用于AI决策时评估招式的威胁程度
 */
public class ThreatAssessment {
    private final double damagePotential;    // 预期伤害（0-1，相对于目标最大HP）
    private final double typeAdvantage;      // 类型优势（-1到1，负值表示劣势）
    private final boolean canKO;             // 能否击倒目标
    private final int priority;              // 优先级（招式优先度）
    private final List<String> advantages;   // 优势列表
    private final List<String> risks;        // 风险列表
    private final double score;              // 综合评分（越高越好）
    
    public ThreatAssessment(double damagePotential, double typeAdvantage, boolean canKO, 
                           int priority, List<String> advantages, List<String> risks) {
        this.damagePotential = damagePotential;
        this.typeAdvantage = typeAdvantage;
        this.canKO = canKO;
        this.priority = priority;
        this.advantages = advantages != null ? new ArrayList<>(advantages) : new ArrayList<>();
        this.risks = risks != null ? new ArrayList<>(risks) : new ArrayList<>();
        
        // 计算综合评分
        this.score = calculateScore();
    }
    
    /**
     * 计算综合评分
     * 考虑因素：伤害、类型优势、击倒能力、优先级、优势和风险
     */
    private double calculateScore() {
        double score = 0.0;
        
        // 基础伤害分（0-40分）
        score += damagePotential * 40.0;
        
        // 类型优势分（-20到20分）
        score += typeAdvantage * 20.0;
        
        // 击倒奖励（30分）
        if (canKO) {
            score += 30.0;
        }
        
        // 优先级调整（-10到10分）
        score += priority * 5.0;
        
        // 优势加分（每个+5分）
        score += advantages.size() * 5.0;
        
        // 风险减分（每个-10分）
        score -= risks.size() * 10.0;
        
        return Math.max(0, Math.min(100, score));
    }
    
    // Getters
    public double getDamagePotential() { return damagePotential; }
    public double getTypeAdvantage() { return typeAdvantage; }
    public boolean canKO() { return canKO; }
    public int getPriority() { return priority; }
    public List<String> getAdvantages() { return new ArrayList<>(advantages); }
    public List<String> getRisks() { return new ArrayList<>(risks); }
    public double getScore() { return score; }
    
    @Override
    public String toString() {
        return String.format("ThreatAssessment{score=%.1f, damage=%.2f, typeAdv=%.2f, canKO=%b, priority=%d}",
                score, damagePotential, typeAdvantage, canKO, priority);
    }
    
    /**
     * 构建器模式
     */
    public static class Builder {
        private double damagePotential = 0.0;
        private double typeAdvantage = 0.0;
        private boolean canKO = false;
        private int priority = 0;
        private List<String> advantages = new ArrayList<>();
        private List<String> risks = new ArrayList<>();
        
        public Builder damagePotential(double value) {
            this.damagePotential = Math.max(0.0, Math.min(1.0, value));
            return this;
        }
        
        public Builder typeAdvantage(double value) {
            this.typeAdvantage = Math.max(-1.0, Math.min(1.0, value));
            return this;
        }
        
        public Builder canKO(boolean value) {
            this.canKO = value;
            return this;
        }
        
        public Builder priority(int value) {
            this.priority = value;
            return this;
        }
        
        public Builder addAdvantage(String advantage) {
            this.advantages.add(advantage);
            return this;
        }
        
        public Builder addRisk(String risk) {
            this.risks.add(risk);
            return this;
        }
        
        public ThreatAssessment build() {
            return new ThreatAssessment(damagePotential, typeAdvantage, canKO, 
                                      priority, advantages, risks);
        }
    }
}
