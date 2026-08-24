package com.lio9.battle.engine;

import com.lio9.battle.service.TierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对战引擎核心逻辑单元测试。
 * 只测试不依赖数据库的纯逻辑方法。
 */
class BattleEngineTest {

    // === AIDifficulty 测试 ===

    @Test
    @DisplayName("AIDifficulty 枚举完整性")
    void aiDifficultyEnum() {
        assertEquals(4, AIDifficulty.values().length);
        assertEquals(AIDifficulty.EASY, AIDifficulty.valueOf("EASY"));
        assertEquals(AIDifficulty.NORMAL, AIDifficulty.valueOf("NORMAL"));
        assertEquals(AIDifficulty.HARD, AIDifficulty.valueOf("HARD"));
        assertEquals(AIDifficulty.EXPERT, AIDifficulty.valueOf("EXPERT"));
    }

    @Test
    @DisplayName("AIDifficulty 描述非空")
    void aiDifficultyDescription() {
        for (AIDifficulty d : AIDifficulty.values()) {
            assertNotNull(d.getDescription());
            assertFalse(d.getDescription().isEmpty());
        }
    }

    @Test
    @DisplayName("AIDifficulty lookahead 深度")
    void aiDifficultyLookahead() {
        assertEquals(0, AIDifficulty.EASY.getLookaheadDepth());
        assertEquals(0, AIDifficulty.NORMAL.getLookaheadDepth());
        assertEquals(2, AIDifficulty.HARD.getLookaheadDepth());
        assertEquals(3, AIDifficulty.EXPERT.getLookaheadDepth());
    }

    @Test
    @DisplayName("AIDifficulty 伤害预测开关")
    void aiDifficultyDamagePrediction() {
        assertFalse(AIDifficulty.EASY.useDamagePrediction());
        assertTrue(AIDifficulty.NORMAL.useDamagePrediction());
        assertTrue(AIDifficulty.HARD.useDamagePrediction());
        assertTrue(AIDifficulty.EXPERT.useDamagePrediction());
    }

    @Test
    @DisplayName("AIDifficulty 资源管理开关")
    void aiDifficultyResourceManagement() {
        assertFalse(AIDifficulty.EASY.useResourceManagement());
        assertFalse(AIDifficulty.NORMAL.useResourceManagement());
        assertTrue(AIDifficulty.HARD.useResourceManagement());
        assertTrue(AIDifficulty.EXPERT.useResourceManagement());
    }

    // === MoveRegistry 测试 ===

    @Test
    @DisplayName("禁用招式检测")
    void bannedMoves() {
        assertTrue(MoveRegistry.isBannedMove(Map.of("name_en", "dark void")));
        assertTrue(MoveRegistry.isBannedMove(Map.of("name_en", "dark-void")));
        assertFalse(MoveRegistry.isBannedMove(Map.of("name_en", "thunderbolt")));
        assertFalse(MoveRegistry.isBannedMove(Map.of("name_en", "protect")));
    }

    @Test
    @DisplayName("禁用道具检测")
    void bannedItems() {
        assertTrue(MoveRegistry.isBannedItem("soul-dew"));
        assertTrue(MoveRegistry.isBannedItem("soul dew"));
        assertFalse(MoveRegistry.isBannedItem("leftovers"));
        assertFalse(MoveRegistry.isBannedItem(null));
        assertFalse(MoveRegistry.isBannedItem(""));
    }

    @Test
    @DisplayName("保护招式检测")
    void protectMoves() {
        assertTrue(MoveRegistry.isProtect(Map.of("name_en", "protect")));
        assertTrue(MoveRegistry.isProtect(Map.of("name_en", "detect")));
        assertTrue(MoveRegistry.isProtect(Map.of("name_en", "spiky-shield")));
        assertFalse(MoveRegistry.isProtect(Map.of("name_en", "tackle")));
    }

    @Test
    @DisplayName("强化招式检测")
    void setupMoves() {
        assertTrue(MoveRegistry.isSwordsDance(Map.of("name_en", "swords-dance")));
        assertTrue(MoveRegistry.isNastyPlot(Map.of("name_en", "nasty-plot")));
        assertTrue(MoveRegistry.isDragonDance(Map.of("name_en", "dragon-dance")));
        assertTrue(MoveRegistry.isCalmMind(Map.of("name_en", "calm-mind")));
        assertTrue(MoveRegistry.isAgility(Map.of("name_en", "agility")));
        assertTrue(MoveRegistry.isBulkUp(Map.of("name_en", "bulk-up")));
        assertTrue(MoveRegistry.isQuiverDance(Map.of("name_en", "quiver-dance")));
    }

    @Test
    @DisplayName("辅助招式检测")
    void utilityMoves() {
        assertTrue(MoveRegistry.isStringShot(Map.of("name_en", "string-shot")));
        assertTrue(MoveRegistry.isTickle(Map.of("name_en", "tickle")));
        assertTrue(MoveRegistry.isTailwind(Map.of("name_en", "tailwind")));
        assertTrue(MoveRegistry.isTrickRoom(Map.of("name_en", "trick-room")));
    }

    @Test
    @DisplayName("群体招式检测")
    void spreadMoves() {
        assertTrue(MoveRegistry.isSpreadMove(Map.of("target_id", 11))); // all opponents
        assertTrue(MoveRegistry.isSpreadMove(Map.of("target_id", 9)));  // all other active
        assertTrue(MoveRegistry.isSpreadMove(Map.of("target_id", 14))); // all active
        assertFalse(MoveRegistry.isSpreadMove(Map.of("target_id", 10))); // single target
        assertFalse(MoveRegistry.isSpreadMove(Map.of("target_id", 13))); // users-field (NOT spread)
        assertFalse(MoveRegistry.isSpreadMove(Map.of("target_id", 4)));  // self
    }

    @Test
    @DisplayName("状态招式检测")
    void statusMoves() {
        assertTrue(MoveRegistry.isStatusMove(Map.of("damage_class_id", 3, "power", 0)));
        assertFalse(MoveRegistry.isStatusMove(Map.of("damage_class_id", 1, "power", 80)));
        assertFalse(MoveRegistry.isStatusMove(Map.of("damage_class_id", 2, "power", 90)));
        // power=0 means status even if damage_class_id is physical
        assertTrue(MoveRegistry.isStatusMove(Map.of("damage_class_id", 1, "power", 0)));
    }

    // === TierService 测试 ===

    @Test
    @DisplayName("段位名称")
    void tierNames() {
        assertEquals("精灵球", TierService.tierName(0));
        assertEquals("超级球", TierService.tierName(1));
        assertEquals("高级球", TierService.tierName(2));
        assertEquals("大师球", TierService.tierName(3));
        assertEquals("精灵球", TierService.tierName(-1));
        assertEquals("精灵球", TierService.tierName(99));
    }

    @Test
    @DisplayName("工厂挑战积分奖励公式")
    void factoryRewardFormula() {
        assertEquals(470, TierService.calculateRunReward(0, 9, 0));   // 9W0L 精灵球
        assertEquals(320, TierService.calculateRunReward(3, 9, 0));   // 9W0L 大师球
        assertEquals(310, TierService.calculateRunReward(0, 5, 4));   // 5W4L 精灵球
        assertEquals(110, TierService.calculateRunReward(0, 0, 9));   // 0W9L 精灵球
        assertEquals(0, TierService.calculateRunReward(3, 0, 9));     // 0W9L 大师球 (负数→0)
    }

    @Test
    @DisplayName("单场对战积分奖励")
    void singleBattleReward() {
        assertTrue(TierService.calculateSingleBattleReward(0, true) > 0);
        assertTrue(TierService.calculateSingleBattleReward(0, false) < 0);
        assertTrue(TierService.calculateSingleBattleReward(3, true) < TierService.calculateSingleBattleReward(0, true));
    }

    // === BattlePreviewSupport 测试 ===

    @Test
    @DisplayName("受限传说检测")
    void restrictedLegendDetection() {
        assertTrue(BattlePreviewSupport.isRestrictedLegend(Map.of("species_id", 150)));   // Mewtwo
        assertTrue(BattlePreviewSupport.isRestrictedLegend(Map.of("species_id", 1007)));  // Koraidon
        assertTrue(BattlePreviewSupport.isRestrictedLegend(Map.of("species_id", 483)));   // Dialga
        assertFalse(BattlePreviewSupport.isRestrictedLegend(Map.of("species_id", 25)));   // Pikachu
        assertFalse(BattlePreviewSupport.isRestrictedLegend(Map.of("species_id", 6)));    // Charizard
        assertFalse(BattlePreviewSupport.isRestrictedLegend(Map.of()));                   // no id
    }

    @Test
    @DisplayName("受限传说计数")
    void restrictedLegendCount() {
        List<Map<String, Object>> team = List.of(
            Map.of("species_id", 150),  // Mewtwo
            Map.of("species_id", 25),   // Pikachu
            Map.of("species_id", 1007), // Koraidon
            Map.of("species_id", 6)     // Charizard
        );
        assertEquals(2, BattlePreviewSupport.countRestrictedLegends(team));
        assertEquals(0, BattlePreviewSupport.countRestrictedLegends(List.of(
            Map.of("species_id", 25), Map.of("species_id", 6)
        )));
    }

    // === ThreatAssessment 测试 ===

    @Test
    @DisplayName("ThreatAssessment 构建器")
    void threatAssessmentBuilder() {
        ThreatAssessment ta = new ThreatAssessment.Builder()
                .damagePotential(0.8)
                .canKO(true)
                .priority(1)
                .addAdvantage("High damage")
                .build();

        assertEquals(0.8, ta.getDamagePotential(), 0.01);
        assertTrue(ta.canKO());
        assertEquals(1, ta.getPriority());
        assertTrue(ta.getAdvantages().contains("High damage"));
    }
}
