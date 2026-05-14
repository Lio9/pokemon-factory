package com.lio9.battle.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreatAssessment 威胁评估单元测试
 * 
 * 测试覆盖：
 * - 构造函数和Getter方法
 * - 评分计算逻辑
 * - Builder模式
 * - toString方法
 * - 边界条件处理
 */
@DisplayName("威胁评估测试")
class ThreatAssessmentTest {

    @Test
    @DisplayName("测试基本构造和Getter方法")
    void testBasicConstructionAndGetters() {
        List<String> advantages = Arrays.asList("类型克制", "高威力");
        List<String> risks = Arrays.asList("低命中");
        
        ThreatAssessment assessment = new ThreatAssessment(
            0.8,    // damagePotential
            0.5,    // typeAdvantage
            true,   // canKO
            1,      // priority
            advantages,
            risks
        );
        
        assertEquals(0.8, assessment.getDamagePotential());
        assertEquals(0.5, assessment.getTypeAdvantage());
        assertTrue(assessment.canKO());
        assertEquals(1, assessment.getPriority());
        assertEquals(2, assessment.getAdvantages().size());
        assertEquals(1, assessment.getRisks().size());
    }

    @Test
    @DisplayName("测试null列表处理")
    void testNullListHandling() {
        ThreatAssessment assessment = new ThreatAssessment(
            0.5, 0.3, false, 0, null, null
        );
        
        assertNotNull(assessment.getAdvantages());
        assertNotNull(assessment.getRisks());
        assertTrue(assessment.getAdvantages().isEmpty());
        assertTrue(assessment.getRisks().isEmpty());
    }

    @Test
    @DisplayName("测试防御性复制")
    void testDefensiveCopy() {
        List<String> advantages = new java.util.ArrayList<>(Arrays.asList("优势1"));
        ThreatAssessment assessment = new ThreatAssessment(
            0.5, 0.3, false, 0, advantages, Collections.emptyList()
        );
        
        // 修改原始列表不应影响assessment
        advantages.add("优势2");
        assertEquals(1, assessment.getAdvantages().size());
        
        // 获取的列表应该是副本
        List<String> retrievedAdvantages = assessment.getAdvantages();
        retrievedAdvantages.add("优势3");
        assertEquals(1, assessment.getAdvantages().size());
    }

    @Test
    @DisplayName("测试评分计算 - 基础伤害分")
    void testScoreCalculationBaseDamage() {
        // 仅考虑伤害（0-40分）
        ThreatAssessment assessment = new ThreatAssessment(
            1.0, 0.0, false, 0, Collections.emptyList(), Collections.emptyList()
        );
        
        double score = assessment.getScore();
        assertEquals(40.0, score, 0.1); // 1.0 * 40 = 40
    }

    @Test
    @DisplayName("测试评分计算 - 类型优势分")
    void testScoreCalculationTypeAdvantage() {
        // 仅考虑类型优势（-20到20分）
        ThreatAssessment positive = new ThreatAssessment(
            0.0, 1.0, false, 0, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(20.0, positive.getScore(), 0.1); // 1.0 * 20 = 20
        
        ThreatAssessment negative = new ThreatAssessment(
            0.0, -1.0, false, 0, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(0.0, negative.getScore(), 0.1); // -20，但最小为0
    }

    @Test
    @DisplayName("测试评分计算 - 击倒奖励")
    void testScoreCalculationKOBonus() {
        // 击倒奖励30分
        ThreatAssessment withKO = new ThreatAssessment(
            0.0, 0.0, true, 0, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(30.0, withKO.getScore(), 0.1);
        
        ThreatAssessment withoutKO = new ThreatAssessment(
            0.0, 0.0, false, 0, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(0.0, withoutKO.getScore(), 0.1);
    }

    @Test
    @DisplayName("测试评分计算 - 优先级调整")
    void testScoreCalculationPriority() {
        // 优先级调整（-10到10分，每级5分）
        ThreatAssessment highPriority = new ThreatAssessment(
            0.0, 0.0, false, 2, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(10.0, highPriority.getScore(), 0.1); // 2 * 5 = 10
        
        ThreatAssessment lowPriority = new ThreatAssessment(
            0.0, 0.0, false, -2, Collections.emptyList(), Collections.emptyList()
        );
        assertEquals(0.0, lowPriority.getScore(), 0.1); // -10，但最小为0
    }

    @Test
    @DisplayName("测试评分计算 - 优势加分")
    void testScoreCalculationAdvantages() {
        // 每个优势+5分
        List<String> advantages = Arrays.asList("优势1", "优势2", "优势3");
        ThreatAssessment assessment = new ThreatAssessment(
            0.0, 0.0, false, 0, advantages, Collections.emptyList()
        );
        
        assertEquals(15.0, assessment.getScore(), 0.1); // 3 * 5 = 15
    }

    @Test
    @DisplayName("测试评分计算 - 风险减分")
    void testScoreCalculationRisks() {
        // 每个风险-10分
        List<String> risks = Arrays.asList("风险1", "风险2");
        ThreatAssessment assessment = new ThreatAssessment(
            0.0, 0.0, false, 0, Collections.emptyList(), risks
        );
        
        assertEquals(0.0, assessment.getScore(), 0.1); // -20，但最小为0
    }

    @Test
    @DisplayName("测试综合评分计算")
    void testComprehensiveScoreCalculation() {
        // 综合场景：高伤害 + 类型优势 + 击倒 + 高优先级 + 优势 - 风险
        List<String> advantages = Arrays.asList("类型克制", "STAB");
        List<String> risks = Arrays.asList("低命中");
        
        ThreatAssessment assessment = new ThreatAssessment(
            0.8,    // 伤害分: 0.8 * 40 = 32
            0.5,    // 类型分: 0.5 * 20 = 10
            true,   // 击倒分: 30
            1,      // 优先级: 1 * 5 = 5
            advantages, // 优势分: 2 * 5 = 10
            risks       // 风险分: -1 * 10 = -10
        );
        
        // 总分: 32 + 10 + 30 + 5 + 10 - 10 = 77
        assertEquals(77.0, assessment.getScore(), 0.1);
    }

    @Test
    @DisplayName("测试评分范围限制（0-100）")
    void testScoreRangeClamping() {
        // 极高分应该被限制在100
        List<String> manyAdvantages = Collections.nCopies(20, "优势");
        ThreatAssessment highScore = new ThreatAssessment(
            1.0, 1.0, true, 2, manyAdvantages, Collections.emptyList()
        );
        assertTrue(highScore.getScore() <= 100.0);
        
        // 极低分应该被限制在0
        List<String> manyRisks = Collections.nCopies(20, "风险");
        ThreatAssessment lowScore = new ThreatAssessment(
            0.0, -1.0, false, -2, Collections.emptyList(), manyRisks
        );
        assertTrue(lowScore.getScore() >= 0.0);
    }

    @Test
    @DisplayName("测试Builder模式 - 基本构建")
    void testBuilderBasicConstruction() {
        ThreatAssessment assessment = new ThreatAssessment.Builder()
            .damagePotential(0.7)
            .typeAdvantage(0.3)
            .canKO(true)
            .priority(1)
            .addAdvantage("优势1")
            .addRisk("风险1")
            .build();
        
        assertEquals(0.7, assessment.getDamagePotential());
        assertEquals(0.3, assessment.getTypeAdvantage());
        assertTrue(assessment.canKO());
        assertEquals(1, assessment.getPriority());
        assertEquals(1, assessment.getAdvantages().size());
        assertEquals(1, assessment.getRisks().size());
    }

    @Test
    @DisplayName("测试Builder模式 - 链式调用")
    void testBuilderChaining() {
        ThreatAssessment assessment = new ThreatAssessment.Builder()
            .damagePotential(0.5)
            .typeAdvantage(0.2)
            .canKO(false)
            .priority(0)
            .addAdvantage("优势1")
            .addAdvantage("优势2")
            .addRisk("风险1")
            .build();
        
        assertNotNull(assessment);
        assertEquals(2, assessment.getAdvantages().size());
        assertEquals(1, assessment.getRisks().size());
    }

    @Test
    @DisplayName("测试Builder模式 - 数值范围限制")
    void testBuilderValueClamping() {
        // damagePotential应该在0-1之间
        ThreatAssessment overMax = new ThreatAssessment.Builder()
            .damagePotential(1.5)
            .build();
        assertEquals(1.0, overMax.getDamagePotential());
        
        ThreatAssessment underMin = new ThreatAssessment.Builder()
            .damagePotential(-0.5)
            .build();
        assertEquals(0.0, underMin.getDamagePotential());
        
        // typeAdvantage应该在-1到1之间
        ThreatAssessment typeOverMax = new ThreatAssessment.Builder()
            .typeAdvantage(2.0)
            .build();
        assertEquals(1.0, typeOverMax.getTypeAdvantage());
        
        ThreatAssessment typeUnderMin = new ThreatAssessment.Builder()
            .typeAdvantage(-2.0)
            .build();
        assertEquals(-1.0, typeUnderMin.getTypeAdvantage());
    }

    @Test
    @DisplayName("测试toString方法")
    void testToString() {
        ThreatAssessment assessment = new ThreatAssessment(
            0.8, 0.5, true, 1, 
            Arrays.asList("优势"), 
            Arrays.asList("风险")
        );
        
        String str = assessment.toString();
        assertNotNull(str);
        assertTrue(str.contains("ThreatAssessment"));
        assertTrue(str.contains("score="));
        assertTrue(str.contains("damage="));
        assertTrue(str.contains("typeAdv="));
        assertTrue(str.contains("canKO="));
        assertTrue(str.contains("priority="));
    }

    @Test
    @DisplayName("测试不同难度级别的典型评分")
    void testTypicalScoresForDifferentScenarios() {
        // 强力攻击（高伤害、克制、能击倒）
        ThreatAssessment strongAttack = new ThreatAssessment.Builder()
            .damagePotential(0.9)
            .typeAdvantage(0.8)
            .canKO(true)
            .priority(0)
            .addAdvantage("类型克制")
            .addAdvantage("STAB")
            .build();
        assertTrue(strongAttack.getScore() > 70);
        
        // 弱效攻击（低伤害、被克制、不能击倒）
        ThreatAssessment weakAttack = new ThreatAssessment.Builder()
            .damagePotential(0.2)
            .typeAdvantage(-0.5)
            .canKO(false)
            .priority(0)
            .addRisk("效果不佳")
            .build();
        assertTrue(weakAttack.getScore() < 20);
        
        // 中等攻击
        ThreatAssessment mediumAttack = new ThreatAssessment.Builder()
            .damagePotential(0.5)
            .typeAdvantage(0.0)
            .canKO(false)
            .priority(0)
            .build();
        assertTrue(mediumAttack.getScore() >= 20 && mediumAttack.getScore() <= 50);
    }
}
