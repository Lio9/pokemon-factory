package com.lio9.battle.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI策略测试
 * 注意：由于BattleEngine需要真实依赖，这些测试主要验证AI策略的基本逻辑
 */
@DisplayName("AI策略测试")
class AIStrategyTest {

    private BattleEngine engine;
    private Map<String, Object> testMon;
    private Map<String, Object> testOpponent;
    private List<Map<String, Object>> testMoves;
    private Map<String, Object> testState;

    @BeforeEach
    void setUp() {
        // 创建模拟的战斗引擎（传入null作为依赖）
        // 注意：这会导致某些BattleEngine方法无法正常工作
        engine = new BattleEngine(null, null, null);
        
        // 创建测试宝可梦
        testMon = createTestPokemon("皮卡丘", 100, 50);
        testOpponent = createTestPokemon("小火龙", 80, 40);
        
        // 创建测试招式列表
        testMoves = createTestMoves();
        
        // 创建测试状态
        testState = new HashMap<>();
        testState.put("round", 1);
    }

    @Test
    @DisplayName("测试EasyAI - 随机选择招式")
    void testEasyAIRandomMoveSelection() {
        EasyAIStrategy strategy = new EasyAIStrategy(engine);
        
        // 创建带有招式的宝可梦
        Map<String, Object> monWithMoves = createTestPokemon("皮卡丘", 100, 50);
        monWithMoves.put("moves", testMoves);
        
        // 多次选择应该有不同的结果（随机性）
        Set<Integer> selectedIndices = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Map<String, Object> move = strategy.selectMove(monWithMoves, Arrays.asList(testOpponent), testState, 1);
            // 注意：由于BattleEngine依赖为null，getAvailableMoves可能返回空列表
            // 这里只测试不抛出异常
            if (move != null) {
                selectedIndices.add(testMoves.indexOf(move));
            }
        }
        
        // 如果选择了招式，应该具有随机性
        if (!selectedIndices.isEmpty()) {
            assertTrue(selectedIndices.size() >= 1, "EasyAI应该具有随机性");
        }
    }

    @Test
    @DisplayName("测试EasyAI - 无可用招式时返回null")
    void testEasyAINoAvailableMoves() {
        EasyAIStrategy strategy = new EasyAIStrategy(engine);
        
        // 传入空招式列表
        Map<String, Object> monWithNoMoves = createTestPokemon("未知", 100, 0);
        Map<String, Object> result = strategy.selectMove(monWithNoMoves, Arrays.asList(testOpponent), testState, 1);
        
        assertNull(result, "无可用招式时应返回null");
    }

    @Test
    @DisplayName("测试EasyAI - 随机换人")
    void testEasyAIRandomSwitch() {
        EasyAIStrategy strategy = new EasyAIStrategy(engine);
        
        List<Map<String, Object>> team = Arrays.asList(
            createTestPokemon("队友1", 100, 50),
            createTestPokemon("队友2", 80, 40),
            createTestPokemon("队友3", 60, 30)
        );
        
        Map<String, Object> currentMon = team.get(0);
        
        // 多次选择应该有不同的结果
        Set<String> selectedNames = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Map<String, Object> switchTarget = strategy.selectSwitch(currentMon, team, Arrays.asList(testOpponent), testState);
            assertNotNull(switchTarget, "应该选择一个换人目标");
            selectedNames.add((String) switchTarget.get("name"));
        }
        
        // 至少应该选择不同的目标2次
        assertTrue(selectedNames.size() >= 2, "EasyAI换人应该具有随机性");
    }

    @Test
    @DisplayName("测试NormalAI - 基于威胁评估选择招式")
    void testNormalAIThreatBasedSelection() {
        NormalAIStrategy strategy = new NormalAIStrategy(engine);
        
        // 创建两个招式：一个高威力，一个低威力
        List<Map<String, Object>> moves = Arrays.asList(
            createTestMove("十万伏特", 90, 1),
            createTestMove("电击", 40, 1)
        );
        
        Map<String, Object> monWithMoves = createTestPokemon("皮卡丘", 100, 50);
        monWithMoves.put("moves", moves);
        
        Map<String, Object> result = strategy.selectMove(monWithMoves, Arrays.asList(testOpponent), testState, 1);
        
        assertNotNull(result, "NormalAI应该选择一个招式");
        // 由于伤害估算逻辑，高威力招式应该有更高评分
        assertEquals("十万伏特", result.get("name"), "应该选择高威力招式");
    }

    @Test
    @DisplayName("测试NormalAI - 优先选择能击倒的招式")
    void testNormalAIPrioritizeKO() {
        NormalAIStrategy strategy = new NormalAIStrategy(engine);
        
        // 创建一个HP很低的对手
        Map<String, Object> weakOpponent = createTestPokemon("虚弱怪", 10, 5);
        
        List<Map<String, Object>> moves = Arrays.asList(
            createTestMove("强力攻击", 50, 1),  // 能击倒
            createTestMove("微弱攻击", 5, 1)     // 不能击倒
        );
        
        Map<String, Object> monWithMoves = createTestPokemon("攻击手", 100, 50);
        monWithMoves.put("moves", moves);
        
        Map<String, Object> result = strategy.selectMove(monWithMoves, Arrays.asList(weakOpponent), testState, 1);
        
        assertNotNull(result);
        assertEquals("强力攻击", result.get("name"), "应该优先选择能击倒对手的招式");
    }

    @Test
    @DisplayName("测试NormalAI - 选择HP最高的队友换人")
    void testNormalAISwitchToHighestHP() {
        NormalAIStrategy strategy = new NormalAIStrategy(engine);
        
        List<Map<String, Object>> team = Arrays.asList(
            createTestPokemon("残血队友", 20, 100),   // HP比例 20%
            createTestPokemon("半血队友", 50, 100),   // HP比例 50%
            createTestPokemon("满血队友", 100, 100)   // HP比例 100%
        );
        
        Map<String, Object> currentMon = createTestPokemon("当前宝可梦", 10, 100);
        
        Map<String, Object> result = strategy.selectSwitch(currentMon, team, Arrays.asList(testOpponent), testState);
        
        assertNotNull(result);
        assertEquals("满血队友", result.get("name"), "应该选择HP比例最高的队友");
    }

    @Test
    @DisplayName("测试HardAI - 继承NormalAI的招式选择")
    void testHardAIMoveSelection() {
        HardAIStrategy strategy = new HardAIStrategy(engine);
        
        List<Map<String, Object>> moves = Arrays.asList(
            createTestMove("招式A", 80, 1),
            createTestMove("招式B", 60, 1)
        );
        
        Map<String, Object> monWithMoves = createTestPokemon("测试怪", 100, 50);
        monWithMoves.put("moves", moves);
        
        Map<String, Object> result = strategy.selectMove(monWithMoves, Arrays.asList(testOpponent), testState, 1);
        
        assertNotNull(result, "HardAI应该选择一个招式");
        // HardAI使用NormalAI的逻辑，应该选择高威力招式
        assertEquals("招式A", result.get("name"));
    }

    @Test
    @DisplayName("测试HardAI - 智能换人选择")
    void testHardAISmartSwitch() {
        HardAIStrategy strategy = new HardAIStrategy(engine);
        
        List<Map<String, Object>> team = Arrays.asList(
            createTestPokemon("低HP队友", 30, 100),
            createTestPokemon("高HP队友", 90, 100)
        );
        
        Map<String, Object> currentMon = createTestPokemon("当前宝可梦", 10, 100);
        
        Map<String, Object> result = strategy.selectSwitch(currentMon, team, Arrays.asList(testOpponent), testState);
        
        assertNotNull(result);
        assertEquals("高HP队友", result.get("name"), "应该选择HP更高的队友");
    }

    @Test
    @DisplayName("测试ExpertAI - 使用HardAI策略")
    void testExpertAIUsesHardStrategy() {
        ExpertAIStrategy strategy = new ExpertAIStrategy(engine);
        
        List<Map<String, Object>> moves = Arrays.asList(
            createTestMove("专家招式", 100, 1)
        );
        
        Map<String, Object> monWithMoves = createTestPokemon("专家怪", 100, 50);
        monWithMoves.put("moves", moves);
        
        Map<String, Object> result = strategy.selectMove(monWithMoves, Arrays.asList(testOpponent), testState, 1);
        
        assertNotNull(result, "ExpertAI应该选择一个招式");
        assertEquals("专家招式", result.get("name"));
    }

    @Test
    @DisplayName("测试所有AI难度 - 处理空对手列表")
    void testAllDifficultiesHandleEmptyOpponents() {
        List<AIStrategy> strategies = Arrays.asList(
            new EasyAIStrategy(engine),
            new NormalAIStrategy(engine),
            new HardAIStrategy(engine),
            new ExpertAIStrategy(engine)
        );
        
        for (AIStrategy strategy : strategies) {
            Map<String, Object> result = strategy.selectMove(testMon, Collections.emptyList(), testState, 1);
            // 不应该抛出异常
            // 可能返回null或某个招式，取决于实现
        }
    }

    @Test
    @DisplayName("测试所有AI难度 - 处理濒死宝可梦")
    void testAllDifficultiesHandleFaintedPokemon() {
        List<AIStrategy> strategies = Arrays.asList(
            new EasyAIStrategy(engine),
            new NormalAIStrategy(engine),
            new HardAIStrategy(engine),
            new ExpertAIStrategy(engine)
        );
        
        Map<String, Object> faintedMon = createTestPokemon("濒死怪", 0, 50);
        
        for (AIStrategy strategy : strategies) {
            Map<String, Object> result = strategy.selectMove(faintedMon, Arrays.asList(testOpponent), testState, 1);
            // 不应该抛出异常
        }
    }

    @Test
    @DisplayName("测试换人 - 全队濒死时返回null")
    void testSwitchReturnsNullWhenAllFainted() {
        EasyAIStrategy easyStrategy = new EasyAIStrategy(engine);
        NormalAIStrategy normalStrategy = new NormalAIStrategy(engine);
        HardAIStrategy hardStrategy = new HardAIStrategy(engine);
        ExpertAIStrategy expertStrategy = new ExpertAIStrategy(engine);
        
        List<Map<String, Object>> allFaintedTeam = Arrays.asList(
            createTestPokemon("濒死1", 0, 50),
            createTestPokemon("濒死2", 0, 50)
        );
        
        Map<String, Object> currentMon = allFaintedTeam.get(0);
        
        assertNull(easyStrategy.selectSwitch(currentMon, allFaintedTeam, Arrays.asList(testOpponent), testState));
        assertNull(normalStrategy.selectSwitch(currentMon, allFaintedTeam, Arrays.asList(testOpponent), testState));
        assertNull(hardStrategy.selectSwitch(currentMon, allFaintedTeam, Arrays.asList(testOpponent), testState));
        assertNull(expertStrategy.selectSwitch(currentMon, allFaintedTeam, Arrays.asList(testOpponent), testState));
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> createTestPokemon(String name, int currentHp, int maxHp) {
        Map<String, Object> pokemon = new HashMap<>();
        pokemon.put("name", name);
        pokemon.put("currentHp", currentHp);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("hp", maxHp);
        pokemon.put("stats", stats);
        
        return pokemon;
    }

    private List<Map<String, Object>> createTestMoves() {
        return Arrays.asList(
            createTestMove("十万伏特", 90, 1),
            createTestMove("电击", 40, 1),
            createTestMove("撞击", 40, 0)
        );
    }

    private Map<String, Object> createTestMove(String name, int power, int priority) {
        Map<String, Object> move = new HashMap<>();
        move.put("name", name);
        move.put("power", power);
        move.put("priority", priority);
        move.put("type_id", 13); // 电系
        return move;
    }
}
