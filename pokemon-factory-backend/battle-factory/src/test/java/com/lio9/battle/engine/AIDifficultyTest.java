package com.lio9.battle.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIDifficulty 枚举单元测试
 * 
 * 测试覆盖：
 * - 枚举值存在性
 * - getDescription() 方法
 * - useDamagePrediction() 方法
 * - useLookahead() 方法
 * - getLookaheadDepth() 方法
 * - useResourceManagement() 方法
 * - useLongTermStrategy() 方法
 */
@DisplayName("AI难度等级测试")
class AIDifficultyTest {

    @Test
    @DisplayName("验证所有难度级别存在")
    void testAllDifficultyLevelsExist() {
        assertNotNull(AIDifficulty.EASY);
        assertNotNull(AIDifficulty.NORMAL);
        assertNotNull(AIDifficulty.HARD);
        assertNotNull(AIDifficulty.EXPERT);
        
        assertEquals(4, AIDifficulty.values().length);
    }

    @Test
    @DisplayName("测试EASY难度描述")
    void testEasyDescription() {
        String description = AIDifficulty.EASY.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("简单"));
        assertTrue(description.contains("随机选择"));
    }

    @Test
    @DisplayName("测试NORMAL难度描述")
    void testNormalDescription() {
        String description = AIDifficulty.NORMAL.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("普通"));
        assertTrue(description.contains("伤害预测"));
    }

    @Test
    @DisplayName("测试HARD难度描述")
    void testHardDescription() {
        String description = AIDifficulty.HARD.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("困难"));
        assertTrue(description.contains("多步预测"));
    }

    @Test
    @DisplayName("测试EXPERT难度描述")
    void testExpertDescription() {
        String description = AIDifficulty.EXPERT.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("专家"));
        assertTrue(description.contains("Minimax"));
    }

    @Test
    @DisplayName("测试伤害预测功能 - EASY不启用")
    void testDamagePredictionEasy() {
        assertFalse(AIDifficulty.EASY.useDamagePrediction());
    }

    @Test
    @DisplayName("测试伤害预测功能 - NORMAL及以上启用")
    void testDamagePredictionNormalAndAbove() {
        assertTrue(AIDifficulty.NORMAL.useDamagePrediction());
        assertTrue(AIDifficulty.HARD.useDamagePrediction());
        assertTrue(AIDifficulty.EXPERT.useDamagePrediction());
    }

    @Test
    @DisplayName("测试多步预测功能 - 仅HARD和EXPERT启用")
    void testLookaheadOnlyHardAndExpert() {
        assertFalse(AIDifficulty.EASY.useLookahead());
        assertFalse(AIDifficulty.NORMAL.useLookahead());
        assertTrue(AIDifficulty.HARD.useLookahead());
        assertTrue(AIDifficulty.EXPERT.useLookahead());
    }

    @Test
    @DisplayName("测试lookahead深度 - EASY和NORMAL为0")
    void testLookaheadDepthEasyAndNormal() {
        assertEquals(0, AIDifficulty.EASY.getLookaheadDepth());
        assertEquals(0, AIDifficulty.NORMAL.getLookaheadDepth());
    }

    @Test
    @DisplayName("测试lookahead深度 - HARD为2")
    void testLookaheadDepthHard() {
        assertEquals(2, AIDifficulty.HARD.getLookaheadDepth());
    }

    @Test
    @DisplayName("测试lookahead深度 - EXPERT为3")
    void testLookaheadDepthExpert() {
        assertEquals(3, AIDifficulty.EXPERT.getLookaheadDepth());
    }

    @Test
    @DisplayName("测试资源管理功能 - 仅HARD和EXPERT启用")
    void testResourceManagementOnlyHardAndExpert() {
        assertFalse(AIDifficulty.EASY.useResourceManagement());
        assertFalse(AIDifficulty.NORMAL.useResourceManagement());
        assertTrue(AIDifficulty.HARD.useResourceManagement());
        assertTrue(AIDifficulty.EXPERT.useResourceManagement());
    }

    @Test
    @DisplayName("测试长期策略功能 - 仅EXPERT启用")
    void testLongTermStrategyOnlyExpert() {
        assertFalse(AIDifficulty.EASY.useLongTermStrategy());
        assertFalse(AIDifficulty.NORMAL.useLongTermStrategy());
        assertFalse(AIDifficulty.HARD.useLongTermStrategy());
        assertTrue(AIDifficulty.EXPERT.useLongTermStrategy());
    }

    @Test
    @DisplayName("测试难度递增特性")
    void testDifficultyProgression() {
        // 随着难度增加，功能应该逐渐启用
        
        // 伤害预测：EASY关闭，其他开启
        assertFalse(AIDifficulty.EASY.useDamagePrediction());
        assertTrue(AIDifficulty.NORMAL.useDamagePrediction());
        
        // lookahead：HARD和EXPERT开启
        assertFalse(AIDifficulty.NORMAL.useLookahead());
        assertTrue(AIDifficulty.HARD.useLookahead());
        
        // 资源管理：HARD和EXPERT开启
        assertFalse(AIDifficulty.NORMAL.useResourceManagement());
        assertTrue(AIDifficulty.HARD.useResourceManagement());
        
        // 长期策略：仅EXPERT开启
        assertFalse(AIDifficulty.HARD.useLongTermStrategy());
        assertTrue(AIDifficulty.EXPERT.useLongTermStrategy());
    }

    @Test
    @DisplayName("测试枚举valueOf方法")
    void testValueOf() {
        assertEquals(AIDifficulty.EASY, AIDifficulty.valueOf("EASY"));
        assertEquals(AIDifficulty.NORMAL, AIDifficulty.valueOf("NORMAL"));
        assertEquals(AIDifficulty.HARD, AIDifficulty.valueOf("HARD"));
        assertEquals(AIDifficulty.EXPERT, AIDifficulty.valueOf("EXPERT"));
    }

    @Test
    @DisplayName("测试枚举ordinal值")
    void testOrdinal() {
        assertEquals(0, AIDifficulty.EASY.ordinal());
        assertEquals(1, AIDifficulty.NORMAL.ordinal());
        assertEquals(2, AIDifficulty.HARD.ordinal());
        assertEquals(3, AIDifficulty.EXPERT.ordinal());
    }

    @Test
    @DisplayName("测试枚举name方法")
    void testName() {
        assertEquals("EASY", AIDifficulty.EASY.name());
        assertEquals("NORMAL", AIDifficulty.NORMAL.name());
        assertEquals("HARD", AIDifficulty.HARD.name());
        assertEquals("EXPERT", AIDifficulty.EXPERT.name());
    }
}
