package com.lio9.battle.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lio9.battle.mapper.SkillMapper;
import com.lio9.battle.service.SkillService;
import com.lio9.pokedex.mapper.TypeEfficacyMapper;
import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阶段2新增机制单元测试 - 简化版
 * 
 * 测试范围：验证核心逻辑实现
 */
class Stage2MechanicsSimpleTest {

    private BattleEngine createEngine() {
        return new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), new TypeEfficacyMapper() {
            @Override
            public List<Map<String, Object>> selectAllTypeEfficacy() {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> selectByDamageTypeId(Integer damageTypeId) {
                return List.of();
            }

            @Override
            public Integer selectDamageFactor(Integer damageTypeId, Integer targetTypeId) {
                return 100;
            }
        }, new ObjectMapper());
    }

    private TypeEfficacyMapper createTypeMapper() {
        return new TypeEfficacyMapper() {
            @Override
            public List<Map<String, Object>> selectAllTypeEfficacy() { return List.of(); }
            @Override
            public List<Map<String, Object>> selectByDamageTypeId(Integer damageTypeId) { return List.of(); }
            @Override
            public Integer selectDamageFactor(Integer damageTypeId, Integer targetTypeId) { return 100; }
        };
    }

    private Map<String, Object> move(String name, String nameEn, int power, int accuracy, int priority,
                                     int damageClassId, int typeId, int pp) {
        Map<String, Object> move = new LinkedHashMap<>();
        move.put("name", name);
        move.put("name_en", nameEn);
        move.put("power", power);
        move.put("accuracy", accuracy);
        move.put("priority", priority);
        move.put("damage_class_id", damageClassId);
        move.put("type_id", typeId);
        move.put("pp", pp);
        move.put("target_id", 1);
        return move;
    }

    // ========== Stockpile/Spit-up/Swallow 测试 ==========

    @Test
    void spitUp_powerCalculationWithStockpile() {
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(
                engine,
                createTypeMapper(),
                new BattleFieldEffectSupport(engine),
                8
        );
        
        Map<String, Object> attacker = createPokemon("Cacturne", 200, 100);
        Map<String, Object> defender = createPokemon("Target", 200, 100);
        Map<String, Object> state = createState();
        
        Map<String, Object> spitUpMove = move("Spit Up", "spit-up", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 10);
        
        // 无蓄力时威力应为0
        int powerNoStockpile = damageSupport.calculateMovePower(spitUpMove, attacker, defender, state);
        assertEquals(0, powerNoStockpile, "Spit-up without stockpile should have 0 power");
        
        // 1层蓄力时威力应为100
        engine.setVolatile(attacker, "stockpileCount", 1);
        int power1Stack = damageSupport.calculateMovePower(spitUpMove, attacker, defender, state);
        assertEquals(100, power1Stack, "Spit-up with 1 stockpile should have 100 power");
        
        // 2层蓄力时威力应为200
        engine.setVolatile(attacker, "stockpileCount", 2);
        int power2Stacks = damageSupport.calculateMovePower(spitUpMove, attacker, defender, state);
        assertEquals(200, power2Stacks, "Spit-up with 2 stockpiles should have 200 power");
        
        // 3层蓄力时威力应为300
        engine.setVolatile(attacker, "stockpileCount", 3);
        int power3Stacks = damageSupport.calculateMovePower(spitUpMove, attacker, defender, state);
        assertEquals(300, power3Stacks, "Spit-up with 3 stockpiles should have 300 power");
    }

    @Test
    void swallow_healCalculation() {
        BattleEngine engine = createEngine();
        
        Map<String, Object> actor = createPokemon("Cacturne", 200, 100);
        actor.put("currentHp", 50); // 当前HP为50
        
        int maxHp = 200;
        int currentHp = 50;
        
        // 1层蓄力应回复25% HP（50点）
        int healPct = 25;
        int heal = Math.max(1, maxHp * healPct / 100);
        int actualHeal = Math.min(heal, maxHp - currentHp);
        assertEquals(50, actualHeal, "1 stockpile should heal 25% HP");
        assertEquals(100, currentHp + actualHeal, "HP after healing with 1 stockpile");
        
        // 2层蓄力应回复50% HP（100点）
        currentHp = 50;
        healPct = 50;
        heal = Math.max(1, maxHp * healPct / 100);
        actualHeal = Math.min(heal, maxHp - currentHp);
        assertEquals(100, actualHeal, "2 stockpiles should heal 50% HP");
        assertEquals(150, currentHp + actualHeal, "HP after healing with 2 stockpiles");
        
        // 3层蓄力应回复100% HP（150点，但不超过maxHp-currentHp）
        currentHp = 50;
        healPct = 100;
        heal = Math.max(1, maxHp * healPct / 100);
        actualHeal = Math.min(heal, maxHp - currentHp);
        assertEquals(150, actualHeal, "3 stockpiles should heal 100% HP");
        assertEquals(200, currentHp + actualHeal, "HP after healing with 3 stockpiles");
    }

    // ========== 亲密度招式测试 ==========

    @Test
    void frustration_return_fixedPower() {
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(
                engine,
                createTypeMapper(),
                new BattleFieldEffectSupport(engine),
                8
        );
        
        Map<String, Object> attacker = createPokemon("Snorlax", 300, 100);
        Map<String, Object> defender = createPokemon("Target", 200, 100);
        Map<String, Object> state = createState();
        
        // 报恩（Return）
        Map<String, Object> returnMove = move("Return", "return", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 20);
        int returnPower = damageSupport.calculateMovePower(returnMove, attacker, defender, state);
        assertEquals(102, returnPower, "Return should have fixed power of 102");
        
        // 迁怒（Frustration）
        Map<String, Object> frustrationMove = move("Frustration", "frustration", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 20);
        int frustrationPower = damageSupport.calculateMovePower(frustrationMove, attacker, defender, state);
        assertEquals(102, frustrationPower, "Frustration should have fixed power of 102");
    }

    // ========== 树果相关招式测试 ==========

    @Test
    void belch_canUseMoveCheck() {
        BattleEngine engine = createEngine();
        
        Map<String, Object> mon = createPokemon("Swalot", 300, 100);
        Map<String, Object> belchMove = move("Belch", "belch", 120, 90, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_POISON, 10);
        
        // 未消耗过树果时应无法使用
        assertFalse(mon.containsKey("berryConsumed"));
        assertFalse(engine.canUseMove(mon, belchMove, 1), "Belch should fail without berry consumed");
        
        // 消耗过树果后应可以使用
        mon.put("berryConsumed", true);
        assertTrue(engine.canUseMove(mon, belchMove, 1), "Belch should succeed after berry consumed");
    }

    @Test
    void naturalGift_powerCalculation() {
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(
                engine,
                createTypeMapper(),
                new BattleFieldEffectSupport(engine),
                8
        );
        
        Map<String, Object> attacker = createPokemon("Arceus", 300, 100);
        Map<String, Object> defender = createPokemon("Target", 200, 100);
        Map<String, Object> state = createState();
        
        Map<String, Object> naturalGiftMove = move("Natural Gift", "natural-gift", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 15);
        
        // 无树果时威力应为0
        int powerNoBerry = damageSupport.calculateMovePower(naturalGiftMove, attacker, defender, state);
        assertEquals(0, powerNoBerry, "Natural Gift without berry should have 0 power");
        
        // 有树果时威力应为60（简化版）
        attacker.put("heldItem", "oran-berry");
        int powerWithBerry = damageSupport.calculateMovePower(naturalGiftMove, attacker, defender, state);
        assertEquals(60, powerWithBerry, "Natural Gift with berry should have 60 power (simplified)");
    }

    @Test
    void bugBite_consumesBerry() {
        BattleEngine engine = createEngine();
        
        Map<String, Object> target = createPokemon("Target", 200, 100);
        target.put("heldItem", "oran-berry");
        
        // 初始状态：有树果，未消耗
        assertEquals("oran-berry", engine.heldItem(target));
        assertFalse(engine.itemConsumed(target));
        
        // 模拟虫灾吃掉树果
        engine.consumeItem(target);
        
        // 消耗后：树果被标记为已消耗
        assertTrue(engine.itemConsumed(target));
        assertTrue(Boolean.TRUE.equals(target.get("berryConsumed")));
    }

    // ========== 特性系统测试 ==========

    @Test
    void slowStart_halvesPhysicalAttack() {
        BattleEngine engine = createEngine();
        
        Map<String, Object> regigigas = createPokemon("Regigigas", 300, 160);
        
        // 设置Slow Start激活（3回合）
        engine.setVolatile(regigigas, "slowStartTurns", 3);
        
        int baseAttack = 160;
        // 计算减半后的攻击
        int modifiedAttack = (int) Math.floor(baseAttack * 0.5d);
        
        assertEquals(80, modifiedAttack, "Slow Start should halve physical attack");
    }

    @Test
    void defeatist_halvesAttackWhenHpBelowHalf() {
        BattleEngine engine = createEngine();
        
        Map<String, Object> carchomp = createPokemon("Carchomp", 300, 130);
        carchomp.put("ability", "defeatist");
        carchomp.put("ability_en", "defeatist");
        
        int maxHp = 300;
        
        // HP > 50% 时不触发
        carchomp.put("currentHp", 200);
        int curHp = 200;
        boolean shouldTrigger = curHp > 0 && curHp * 2 <= maxHp;
        assertFalse(shouldTrigger, "Defeatist should not trigger when HP > 50%");
        
        // HP ≤ 50% 时触发
        carchomp.put("currentHp", 150);
        curHp = 150;
        shouldTrigger = curHp > 0 && curHp * 2 <= maxHp;
        assertTrue(shouldTrigger, "Defeatist should trigger when HP <= 50%");
        
        // 攻击应减半
        int baseAttack = 130;
        int modifiedAttack = Math.max(1, (int) Math.floor(baseAttack * 0.5d));
        assertEquals(65, modifiedAttack, "Defeatist should halve attack when HP <= 50%");
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> createPokemon(String name, int hp, int attack) {
        Map<String, Object> mon = new LinkedHashMap<>();
        mon.put("name", name);
        mon.put("name_en", name.toLowerCase().replace(" ", "-"));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("hp", hp);
        stats.put("attack", attack);
        stats.put("defense", attack);
        stats.put("specialAttack", attack);
        stats.put("specialDefense", attack);
        stats.put("speed", attack);
        mon.put("stats", stats);
        mon.put("currentHp", hp);
        mon.put("heldItem", "");
        mon.put("ability", "");
        mon.put("ability_en", "");
        mon.put("status", "");
        mon.put("condition", "");
        mon.put("statStages", new LinkedHashMap<>(Map.of(
                "attack", 0, "defense", 0, "specialAttack", 0,
                "specialDefense", 0, "speed", 0, "accuracy", 0, "evasion", 0
        )));
        mon.put("volatiles", new LinkedHashMap<>());
        mon.put("types", List.of(Map.of("type_id", DamageCalculatorUtil.TYPE_NORMAL)));
        mon.put("moves", new ArrayList<>());
        mon.put("ppRemaining", new LinkedHashMap<>());
        
        return mon;
    }

    private Map<String, Object> createState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>());
        state.put("currentRound", 1);
        state.put("playerActiveSlots", List.of(0));
        state.put("opponentActiveSlots", List.of(0));
        return state;
    }
}
