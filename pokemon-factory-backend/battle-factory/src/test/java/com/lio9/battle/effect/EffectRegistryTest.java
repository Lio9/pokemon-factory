package com.lio9.battle.effect;

import com.lio9.pokedex.util.DamageCalculatorUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EffectRegistry dispatch 方法测试。
 * <p>
 * 验证注册的特性/道具 handler 是否能正确通过 dispatch 方法被调用。
 * 每个测试构造一个带特定特性/道具的幻兽，调用 dispatch 方法并验证返回值。
 * </p>
 */
class EffectRegistryTest {

    // ========================================================================
    //  攻击方伤害倍率
    // ========================================================================

    @Test
    void dispatchSourceDamage_technicianDoublesFor60PowerOrLess() {
        Map<String, Object> mon = abilityMon("technician");
        AttackContext ctx = ctx(mon, dummyMon(), move(60), NORMAL, PHYSICAL);
        assertEquals(1.5, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_technicianDoesNotBoostAbove60() {
        Map<String, Object> mon = abilityMon("technician");
        AttackContext ctx = ctx(mon, dummyMon(), move(65), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_ironFistBoostsPunchMoves() {
        Map<String, Object> mon = abilityMon("iron-fist");
        AttackContext ctx = ctx(mon, dummyMon(), punchMove(), NORMAL, PHYSICAL);
        assertEquals(1.2, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_hustleBoostsPhysical() {
        Map<String, Object> mon = abilityMon("hustle");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.5, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_hustleDoesNotBoostSpecial() {
        Map<String, Object> mon = abilityMon("hustle");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, SPECIAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_sheerForceBoostsIfEffectChanceExists() {
        Map<String, Object> mon = abilityMon("sheer-force");
        AttackContext ctx = ctx(mon, dummyMon(), moveWithChance(80, 20), NORMAL, PHYSICAL);
        assertEquals(1.3, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_sheerForceNoBoostWithoutEffectChance() {
        Map<String, Object> mon = abilityMon("sheer-force");
        AttackContext ctx = ctx(mon, dummyMon(), move(80), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_tintedLensDoublesNotVeryEffective() {
        Map<String, Object> mon = abilityMon("tinted-lens");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        // mod < 1 → 效果不好，double
        assertEquals(1.2, EffectRegistry.dispatchSourceDamage(mon, ctx, 0.6), 0.001);
    }

    @Test
    void dispatchSourceDamage_tintedLensDoesNotBoostNormalEffectiveness() {
        Map<String, Object> mon = abilityMon("tinted-lens");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_gutsBoostsPhysicalWhenStatused() {
        Map<String, Object> mon = abilityMon("guts");
        mon.put("condition", "burn");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.5, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_gutsDoesNotBoostWhenHealthy() {
        Map<String, Object> mon = abilityMon("guts");
        mon.put("condition", "");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_flareBoostBoostsSpecialWhenBurned() {
        Map<String, Object> mon = abilityMon("flare-boost");
        mon.put("condition", "burn");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, SPECIAL);
        assertEquals(1.5, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_unknownAbilityReturnsUnchangedModifier() {
        Map<String, Object> mon = abilityMon("nonexistent-ability");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchSourceDamage_noAbilityReturnsUnchangedModifier() {
        Map<String, Object> mon = abilityMon("");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.0, EffectRegistry.dispatchSourceDamage(mon, ctx, 1.0), 0.001);
    }

    // ========================================================================
    //  防御方伤害倍率
    // ========================================================================

    @Test
    void dispatchTargetDamage_thickFatHalvesFire() {
        Map<String, Object> defender = abilityMon("thick-fat");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), FIRE, SPECIAL);
        assertEquals(0.5, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_thickFatHalvesIce() {
        Map<String, Object> defender = abilityMon("thick-fat");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), ICE, SPECIAL);
        assertEquals(0.5, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_thickFatDoesNotAffectNormal() {
        Map<String, Object> defender = abilityMon("thick-fat");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), NORMAL, SPECIAL);
        assertEquals(1.0, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_drySkinAbsorbsWater() {
        Map<String, Object> defender = abilityMon("dry-skin");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), WATER, SPECIAL);
        assertEquals(0, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_levitateImmuneToGround() {
        Map<String, Object> defender = abilityMon("levitate");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), GROUND, SPECIAL);
        assertEquals(0, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_iceScalesHalvesSpecial() {
        Map<String, Object> defender = abilityMon("ice-scales");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), NORMAL, SPECIAL);
        assertEquals(0.5, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchTargetDamage_furCoatHalvesPhysical() {
        Map<String, Object> defender = abilityMon("fur-coat");
        AttackContext ctx = ctx(dummyMon(), defender, dummyMove(), NORMAL, PHYSICAL);
        assertEquals(0.5, EffectRegistry.dispatchTargetDamage(defender, ctx, 1.0), 0.001);
    }

    // ========================================================================
    //  速度修正
    // ========================================================================

    @Test
    void dispatchSpeed_swiftSwimDoublesInRain() {
        Map<String, Object> mon = abilityMon("swift-swim");
        Map<String, Object> state = rainState();
        SpeedContext ctx = new SpeedContext(mon, state, true);
        assertEquals(200, EffectRegistry.dispatchSpeed(mon, ctx, 100));
    }

    @Test
    void dispatchSpeed_swiftSwimNoBoostWithoutRain() {
        Map<String, Object> mon = abilityMon("swift-swim");
        Map<String, Object> state = emptyState();
        SpeedContext ctx = new SpeedContext(mon, state, true);
        assertEquals(100, EffectRegistry.dispatchSpeed(mon, ctx, 100));
    }

    @Test
    void dispatchSpeed_chlorophyllDoublesInSun() {
        Map<String, Object> mon = abilityMon("chlorophyll");
        Map<String, Object> state = sunState();
        SpeedContext ctx = new SpeedContext(mon, state, true);
        assertEquals(200, EffectRegistry.dispatchSpeed(mon, ctx, 100));
    }

    @Test
    void dispatchSpeed_unknownAbilityNoChange() {
        Map<String, Object> mon = abilityMon("nonexistent");
        SpeedContext ctx = new SpeedContext(mon, emptyState(), true);
        assertEquals(100, EffectRegistry.dispatchSpeed(mon, ctx, 100));
    }

    // ========================================================================
    //  重量修正
    // ========================================================================

    @Test
    void dispatchWeight_heavyMetalDoubles() {
        Map<String, Object> mon = abilityMon("heavy-metal");
        WeightContext ctx = new WeightContext(mon);
        assertEquals(200, EffectRegistry.dispatchWeight(mon, ctx, 100));
    }

    @Test
    void dispatchWeight_lightMetalHalves() {
        Map<String, Object> mon = abilityMon("light-metal");
        WeightContext ctx = new WeightContext(mon);
        assertEquals(5, EffectRegistry.dispatchWeight(mon, ctx, 10));
    }

    // ========================================================================
    //  能力阶级变化
    // ========================================================================

    @Test
    void dispatchStatStage_contraryReversesPositiveDelta() {
        Map<String, Object> mon = abilityMon("contrary");
        StatStageContext ctx = new StatStageContext(mon, 2, 1, "测试");
        assertEquals(-1, EffectRegistry.dispatchStatStage(mon, ctx, 1));
    }

    @Test
    void dispatchStatStage_contraryReversesNegativeDelta() {
        Map<String, Object> mon = abilityMon("contrary");
        StatStageContext ctx = new StatStageContext(mon, 2, -1, "测试");
        assertEquals(1, EffectRegistry.dispatchStatStage(mon, ctx, -1));
    }

    @Test
    void dispatchStatStage_noAbilityNoChange() {
        Map<String, Object> mon = abilityMon("");
        StatStageContext ctx = new StatStageContext(mon, 2, 1, "测试");
        assertEquals(1, EffectRegistry.dispatchStatStage(mon, ctx, 1));
    }

    // ========================================================================
    //  状态免疫
    // ========================================================================

    @Test
    void dispatchStatusImmunity_limberBlocksParalysis() {
        Map<String, Object> mon = abilityMon("limber");
        StatusContext ctx = new StatusContext(mon, "paralysis", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_limberDoesNotBlockBurn() {
        Map<String, Object> mon = abilityMon("limber");
        StatusContext ctx = new StatusContext(mon, "burn", emptyState());
        assertFalse(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_waterVeilBlocksBurn() {
        Map<String, Object> mon = abilityMon("water-veil");
        StatusContext ctx = new StatusContext(mon, "burn", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_waterBubbleBlocksBurn() {
        Map<String, Object> mon = abilityMon("water-bubble");
        StatusContext ctx = new StatusContext(mon, "burn", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_insomniaBlocksSleep() {
        Map<String, Object> mon = abilityMon("insomnia");
        StatusContext ctx = new StatusContext(mon, "sleep", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_vitalSpiritBlocksSleep() {
        Map<String, Object> mon = abilityMon("vital-spirit");
        StatusContext ctx = new StatusContext(mon, "sleep", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_ownTempoBlocksConfusion() {
        Map<String, Object> mon = abilityMon("own-tempo");
        StatusContext ctx = new StatusContext(mon, "confusion", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_magmaArmorBlocksFreeze() {
        Map<String, Object> mon = abilityMon("magma-armor");
        StatusContext ctx = new StatusContext(mon, "freeze", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_immunityBlocksPoison() {
        Map<String, Object> mon = abilityMon("immunity");
        StatusContext ctx = new StatusContext(mon, "poison", emptyState());
        assertTrue(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    @Test
    void dispatchStatusImmunity_noAbilityReturnsFalse() {
        Map<String, Object> mon = abilityMon("");
        StatusContext ctx = new StatusContext(mon, "paralysis", emptyState());
        assertFalse(EffectRegistry.dispatchStatusImmunity(mon, ctx));
    }

    // ========================================================================
    //  精神类免疫
    // ========================================================================

    @Test
    void dispatchMentalImmunity_obliviousBlocksTaunt() {
        Map<String, Object> mon = abilityMon("oblivious");
        StatusContext ctx = new StatusContext(mon, "taunt", emptyState());
        assertTrue(EffectRegistry.dispatchMentalImmunity(mon, ctx));
    }

    @Test
    void dispatchMentalImmunity_obliviousBlocksAttract() {
        Map<String, Object> mon = abilityMon("oblivious");
        StatusContext ctx = new StatusContext(mon, "attract", emptyState());
        assertTrue(EffectRegistry.dispatchMentalImmunity(mon, ctx));
    }

    @Test
    void dispatchMentalImmunity_obliviousDoesNotBlockParalysis() {
        Map<String, Object> mon = abilityMon("oblivious");
        StatusContext ctx = new StatusContext(mon, "paralysis", emptyState());
        assertFalse(EffectRegistry.dispatchMentalImmunity(mon, ctx));
    }

    // ========================================================================
    //  追加效果修正
    // ========================================================================

    @Test
    void dispatchSereneGrace_doublesChance() {
        Map<String, Object> mon = abilityMon("serene-grace");
        assertEquals(40, EffectRegistry.dispatchSereneGrace(mon, 20));
    }

    @Test
    void dispatchSereneGrace_capsAt100() {
        Map<String, Object> mon = abilityMon("serene-grace");
        assertEquals(100, EffectRegistry.dispatchSereneGrace(mon, 60));
    }

    @Test
    void dispatchSereneGrace_noAbilityReturnsChance() {
        Map<String, Object> mon = abilityMon("");
        assertEquals(20, EffectRegistry.dispatchSereneGrace(mon, 20));
    }

    @Test
    void dispatchBlocksSecondaryEffects_shieldDustBlocks() {
        Map<String, Object> mon = abilityMon("shield-dust");
        assertTrue(EffectRegistry.dispatchBlocksSecondaryEffects(mon));
    }

    @Test
    void dispatchBlocksSecondaryEffects_noAbilityDoesNotBlock() {
        Map<String, Object> mon = abilityMon("");
        assertFalse(EffectRegistry.dispatchBlocksSecondaryEffects(mon));
    }

    // ========================================================================
    //  能力下降阻挡
    // ========================================================================

    @Test
    void dispatchStatDropBlocked_clearBodyBlocks() {
        assertTrue(EffectRegistry.dispatchStatDropBlocked(abilityMon("clear-body")));
    }

    @Test
    void dispatchStatDropBlocked_whiteSmokeBlocks() {
        assertTrue(EffectRegistry.dispatchStatDropBlocked(abilityMon("white-smoke")));
    }

    @Test
    void dispatchStatDropBlocked_fullMetalBodyBlocks() {
        assertTrue(EffectRegistry.dispatchStatDropBlocked(abilityMon("full-metal-body")));
    }

    @Test
    void dispatchStatDropBlocked_noAbilityDoesNotBlock() {
        assertFalse(EffectRegistry.dispatchStatDropBlocked(abilityMon("")));
    }

    // ========================================================================
    //  道具修正
    // ========================================================================

    @Test
    void dispatchItemSourceDamage_lifeOrb() {
        Map<String, Object> mon = itemMon("life-orb");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        assertEquals(1.3, EffectRegistry.dispatchItemSourceDamage(mon, ctx, 1.0), 0.001);
    }

    @Test
    void dispatchItemSourceAttack_choiceBandPhysical() {
        Map<String, Object> mon = itemMon("choice-band");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        int result = EffectRegistry.dispatchItemSourceAttack(mon, ctx, 100);
        assertEquals(150, result);
    }

    @Test
    void dispatchItemSourceAttack_choiceSpecsSpecial() {
        Map<String, Object> mon = itemMon("choice-specs");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, SPECIAL);
        int result = EffectRegistry.dispatchItemSourceAttack(mon, ctx, 100);
        assertEquals(150, result);
    }

    @Test
    void dispatchItemSourceAttack_choiceSpecsDoesNotBoostPhysical() {
        Map<String, Object> mon = itemMon("choice-specs");
        AttackContext ctx = ctx(mon, dummyMon(), dummyMove(), NORMAL, PHYSICAL);
        int result = EffectRegistry.dispatchItemSourceAttack(mon, ctx, 100);
        assertEquals(100, result);
    }

    @Test
    void dispatchItemSpeed_choiceScarf() {
        Map<String, Object> mon = itemMon("choice-scarf");
        SpeedContext ctx = new SpeedContext(mon, emptyState(), true);
        assertEquals(150, EffectRegistry.dispatchItemSpeed(mon, ctx, 100));
    }

    @Test
    void dispatchItemWeight_floatStone() {
        Map<String, Object> mon = itemMon("float-stone");
        WeightContext ctx = new WeightContext(mon);
        assertEquals(50, EffectRegistry.dispatchItemWeight(mon, ctx, 100));
    }

    // ========================================================================
    //  类型常量
    // ========================================================================

    @Test
    void typeConstants_haveCorrectValues() {
        assertEquals(1, EffectRegistry.NORMAL);
        assertEquals(2, EffectRegistry.FIRE);
        assertEquals(3, EffectRegistry.WATER);
        assertEquals(7, EffectRegistry.FIGHTING);
        assertEquals(14, EffectRegistry.GHOST);
        assertEquals(18, EffectRegistry.FAIRY);
    }

    // ========================================================================
    //  工具方法
    // ========================================================================

    @Test
    void abilityName_withMapExtractsNameEn() {
        Map<String, Object> mon = new LinkedHashMap<>();
        mon.put("ability", Map.of("name", "Intimidate", "name_en", "intimidate"));
        assertEquals("intimidate", EffectRegistry.abilityName(mon));
    }

    @Test
    void abilityName_withStringReturnsLowercase() {
        Map<String, Object> mon = new LinkedHashMap<>();
        mon.put("ability", "Intimidate");
        assertEquals("intimidate", EffectRegistry.abilityName(mon));
    }

    @Test
    void abilityName_withNullReturnsEmpty() {
        Map<String, Object> mon = new LinkedHashMap<>();
        assertEquals("", EffectRegistry.abilityName(mon));
    }

    @Test
    void isRegisteredItem_knownItemReturnsTrue() {
        assertTrue(EffectRegistry.isRegisteredItem("life-orb"));
    }

    @Test
    void isRegisteredItem_unknownItemReturnsFalse() {
        assertFalse(EffectRegistry.isRegisteredItem("nonexistent-item"));
    }

    @Test
    void getAbility_knownReturnsHandler() {
        assertNotNull(EffectRegistry.getAbility("technician"));
    }

    @Test
    void getAbility_unknownReturnsNull() {
        assertNull(EffectRegistry.getAbility("nonexistent"));
    }

    // ========================================================================
    //  辅助构造方法
    // ========================================================================

    private static final int NORMAL = 1;
    private static final int FIRE = 2;
    private static final int WATER = 3;
    private static final int GROUND = 9;
    private static final int ICE = 6;
    private static final int PHYSICAL = 1;
    private static final int SPECIAL = 2;

    private static Map<String, Object> abilityMon(String ability) {
        Map<String, Object> mon = new LinkedHashMap<>();
        mon.put("ability", ability);
        mon.put("name_en", "test-mon");
        mon.put("condition", "");
        mon.put("stats", Map.of("hp", 100, "attack", 100, "defense", 100,
                "specialAttack", 100, "specialDefense", 100, "speed", 100));
        return mon;
    }

    private static Map<String, Object> itemMon(String item) {
        Map<String, Object> mon = abilityMon("");
        mon.put("heldItem", item);
        return mon;
    }

    private static Map<String, Object> dummyMon() {
        return abilityMon("");
    }

    private static Map<String, Object> dummyMove() {
        return move(80);
    }

    private static Map<String, Object> move(int power) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name_en", "test-move");
        m.put("power", power);
        m.put("type_id", 1);
        m.put("damage_class_id", 1);
        m.put("flags", List.of());
        return m;
    }

    private static Map<String, Object> punchMove() {
        Map<String, Object> m = move(75);
        m.put("name_en", "drain-punch");
        return m;
    }

    private static Map<String, Object> moveWithChance(int power, int chance) {
        Map<String, Object> m = move(power);
        m.put("effect_chance", chance);
        return m;
    }

    private static AttackContext ctx(Map<String, Object> attacker, Map<String, Object> defender,
                                     Map<String, Object> move, int moveTypeId, int damageClassId) {
        // 对于部分需要特性的 test，将 ability name 通过 abilityName/getter 提取
        // AttackContext 不从 mon 里读 ability，它只通过 getter 读 abilityName
        // 所以需要让 Engine.abilityName() 能返回，但这里我们直接传 mon
        // 上下文构造函数不提取 ability，只保存引用
        return new AttackContext(attacker, defender, move, emptyState(), moveTypeId, damageClassId, false);
    }

    private static Map<String, Object> emptyState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>());
        return state;
    }

    private static Map<String, Object> rainState() {
        Map<String, Object> state = emptyState();
        ((Map<String, Object>) state.get("fieldEffects")).put("rainTurns", 4);
        return state;
    }

    private static Map<String, Object> sunState() {
        Map<String, Object> state = emptyState();
        ((Map<String, Object>) state.get("fieldEffects")).put("sunTurns", 4);
        return state;
    }
}
