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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleConditionSupportTest {

    @Test
    void applySleep_electricTerrainBlocksGroundedTargets() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("fieldEffects", new LinkedHashMap<>(Map.of("electricTerrainTurns", 4)));

        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();
        Map<String, Object> move = move("Spore", "spore", 0, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_STATUS, DamageCalculatorUtil.TYPE_GRASS, 10);
        Map<String, Object> target = pokemon("Grounded-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL);

        support.applySleep(
                state,
                pokemon("Spore-A", 220, 120, "", "", DamageCalculatorUtil.TYPE_GRASS),
            target,
                move,
                actionLog,
                events,
                new Random(42),
                1
        );

        assertEquals("status-immune", actionLog.get("result"));
        assertTrue(events.stream().anyMatch(event -> event.contains("电气场地")));
        assertNull(target.get("condition"));
    }

    @Test
    void applyTaunt_mentalHerbClearsTauntImmediately() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> target = pokemon("Herb-A", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyTaunt(
                pokemon("Taunt-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_DARK),
                target,
                actionLog,
                events
        );

        assertEquals(0, target.get("tauntTurns"));
        assertEquals(Boolean.TRUE, target.get("itemConsumed"));
        assertEquals(Boolean.TRUE, actionLog.get("mentalHerb"));
        assertTrue(events.stream().anyMatch(event -> event.contains("心灵香草")));
    }

    @Test
    void controlMoves_mentalHerbClearsEncoreDisableTormentAndHealBlock() {
        BattleConditionSupport support = createSupport();

        Map<String, Object> encoreTarget = pokemon("Encore-Herb", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        encoreTarget.put("lastMoveUsed", "strike");
        Map<String, Object> encoreLog = new LinkedHashMap<>();
        List<String> encoreEvents = new ArrayList<>();
        support.applyEncore(pokemon("Encore-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL), encoreTarget, encoreLog, encoreEvents);
        assertEquals(0, encoreTarget.get("encoreTurns"));
        assertNull(encoreTarget.get("encoreMove"));
        assertEquals(Boolean.TRUE, encoreTarget.get("itemConsumed"));
        assertEquals(Boolean.TRUE, encoreLog.get("mentalHerb"));

        Map<String, Object> disableTarget = pokemon("Disable-Herb", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        disableTarget.put("lastMoveUsed", "strike");
        Map<String, Object> disableLog = new LinkedHashMap<>();
        List<String> disableEvents = new ArrayList<>();
        support.applyDisable(new LinkedHashMap<>(), pokemon("Disable-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                disableTarget, disableLog, disableEvents);
        assertEquals(0, disableTarget.get("disableTurns"));
        assertNull(disableTarget.get("disableMove"));
        assertEquals(Boolean.TRUE, disableTarget.get("itemConsumed"));
        assertEquals(Boolean.TRUE, disableLog.get("mentalHerb"));

        Map<String, Object> tormentTarget = pokemon("Torment-Herb", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> tormentLog = new LinkedHashMap<>();
        List<String> tormentEvents = new ArrayList<>();
        support.applyTorment(pokemon("Torment-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL), tormentTarget, tormentLog, tormentEvents);
        assertEquals(0, tormentTarget.get("tormentTurns"));
        assertEquals(Boolean.TRUE, tormentTarget.get("itemConsumed"));
        assertEquals(Boolean.TRUE, tormentLog.get("mentalHerb"));

        Map<String, Object> healBlockTarget = pokemon("HealBlock-Herb", 220, 120, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> healBlockLog = new LinkedHashMap<>();
        List<String> healBlockEvents = new ArrayList<>();
        support.applyHealBlock(pokemon("HealBlock-Opp", 220, 80, "", "", DamageCalculatorUtil.TYPE_NORMAL), healBlockTarget, healBlockLog, healBlockEvents);
        assertEquals(0, healBlockTarget.get("healBlockTurns"));
        assertEquals(Boolean.TRUE, healBlockTarget.get("itemConsumed"));
        assertEquals(Boolean.TRUE, healBlockLog.get("mentalHerb"));
    }

    @Test
    void applyDefenderAbilityImmunity_lightningRodBoostsSpecialAttack() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Source-A", 220, 120, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Rod-A", 220, 120, "", "lightning-rod", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        boolean blocked = support.applyDefenderAbilityImmunity(
                attacker,
                target,
                move("Thunderbolt", "thunderbolt", 80, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                actionLog,
                events
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) target.get("statStages");
        assertTrue(blocked);
        assertEquals(1, stages.get("specialAttack"));
        assertEquals("ability-immune", actionLog.get("result"));
        assertEquals("lightning-rod", actionLog.get("ability"));
        assertTrue(events.stream().anyMatch(event -> event.contains("避雷针")));
    }

    @Test
    void applyDefenderAbilityImmunity_moldBreakerBypassesLightningRod() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Breaker-A", 220, 120, "", "mold-breaker", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Rod-A", 220, 120, "", "lightning-rod", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        boolean blocked = support.applyDefenderAbilityImmunity(
                attacker,
                target,
                move("Thunderbolt", "thunderbolt", 80, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                actionLog,
                events
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) target.get("statStages");
        assertTrue(!blocked);
        assertTrue(stages == null || Integer.valueOf(0).equals(stages.get("specialAttack")));
        assertTrue(actionLog.isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void applyDefenderAbilityImmunity_moldBreakerBypassesSoundproof() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Breaker-A", 220, 120, "", "mold-breaker", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Soundproof-Opp", 220, 120, "", "soundproof", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        boolean blocked = support.applyDefenderAbilityImmunity(
                attacker,
                target,
                withFlags(move("Snarl", "snarl", 55, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_DARK, 10), "sound"),
                actionLog,
                events
        );

        assertTrue(!blocked);
        assertTrue(actionLog.isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void applyDefenderAbilityImmunity_moldBreakerBypassesBulletproof() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> attacker = pokemon("Breaker-A", 220, 120, "", "mold-breaker", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Bulletproof-Opp", 220, 120, "", "bulletproof", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        boolean blocked = support.applyDefenderAbilityImmunity(
                attacker,
                target,
                withFlags(move("Aura Sphere", "aura-sphere", 80, 100, 0,
                        DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_FIGHTING, 10), "bullet"),
                actionLog,
                events
        );

        assertTrue(!blocked);
        assertTrue(actionLog.isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void applyIntimidate_triggersDefiantBoost() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>();
        Map<String, Object> target = pokemon("Defiant-Opp", 220, 70, "", "defiant", DamageCalculatorUtil.TYPE_NORMAL);
        state.put("playerTeam", List.of());
        state.put("playerActiveSlots", List.of());
        state.put("opponentTeam", List.of(target));
        state.put("opponentActiveSlots", List.of(0));

        Map<String, Object> source = pokemon("Intimidate-A", 220, 120, "", "intimidate", DamageCalculatorUtil.TYPE_NORMAL);
        List<String> events = new ArrayList<>();

        support.applyIntimidate(state, true, source, events);

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) target.get("statStages");
        assertEquals(1, stages.get("attack"));
        assertEquals(Boolean.TRUE, source.get("intimidateActivated"));
        assertTrue(events.stream().anyMatch(event -> event.contains("不服输")));
    }

    @Test
    void applyIntimidate_whiteHerbRestoresDropAndPreservesDefiantBoost() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>();
        Map<String, Object> herbTarget = pokemon("Herb-Opp", 220, 70, "white-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> defiantTarget = pokemon("Defiant-Herb", 220, 70, "white-herb", "defiant", DamageCalculatorUtil.TYPE_NORMAL);
        state.put("playerTeam", List.of());
        state.put("playerActiveSlots", List.of());
        state.put("opponentTeam", List.of(herbTarget, defiantTarget));
        state.put("opponentActiveSlots", List.of(0, 1));

        Map<String, Object> source = pokemon("Intimidate-A", 220, 120, "", "intimidate", DamageCalculatorUtil.TYPE_NORMAL);
        List<String> events = new ArrayList<>();

        support.applyIntimidate(state, true, source, events);

        @SuppressWarnings("unchecked")
        Map<String, Object> herbStages = (Map<String, Object>) herbTarget.get("statStages");
        @SuppressWarnings("unchecked")
        Map<String, Object> defiantStages = (Map<String, Object>) defiantTarget.get("statStages");
        assertEquals(0, herbStages.get("attack"));
        assertEquals(2, defiantStages.get("attack"));
        assertEquals(Boolean.TRUE, herbTarget.get("itemConsumed"));
        assertEquals(Boolean.TRUE, defiantTarget.get("itemConsumed"));
        assertTrue(events.stream().anyMatch(event -> event.contains("白色香草")));
    }

    @Test
    void applyReactiveContactEffects_stacksRoughSkinAndRockyHelmet() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Punish-Opp", 220, 80, "rocky-helmet", "rough-skin", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(99));

        assertEquals(170, attacker.get("currentHp"));
        assertEquals(30, actionLog.get("roughSkin"));
        assertEquals(40, actionLog.get("rockyHelmet"));
        assertTrue(events.stream().anyMatch(event -> event.contains("粗糙皮肤")));
        assertTrue(events.stream().anyMatch(event -> event.contains("凸凸头盔")));
    }

    @Test
    void applyReactiveContactEffects_staticCanParalyzeAttacker() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Static-Opp", 220, 80, "", "static", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(0));

        assertEquals("paralysis", attacker.get("condition"));
        assertEquals(Boolean.TRUE, actionLog.get("static"));
    }

    @Test
    void applyReactiveContactEffects_flameBodyCanBurnAttacker() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("FlameBody-Opp", 220, 80, "", "flame-body", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(0));

        assertEquals("burn", attacker.get("condition"));
        assertEquals(Boolean.TRUE, actionLog.get("flameBody"));
    }

    @Test
    void applyReactiveContactEffects_poisonPointCanPoisonAttacker() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("PoisonPoint-Opp", 220, 80, "", "poison-point", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(0));

        assertEquals("poison", attacker.get("condition"));
        assertEquals(Boolean.TRUE, actionLog.get("poisonPoint"));
    }

    @Test
    void applyReactiveContactEffects_effectSporeRespectsPowderImmunity() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Goggles-A", 240, 100, "safety-goggles", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Spore-Opp", 220, 80, "", "effect-spore", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(0, 0, 0));

        assertNull(attacker.get("condition"));
        assertEquals(Boolean.TRUE, actionLog.get("effectSporeBlocked"));
        assertTrue(events.stream().anyMatch(event -> event.contains("防尘护目镜")));
    }

    @Test
    void applyReactiveContactEffects_gooeyLowersSpeedAndClearAmuletBlocksIt() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));

        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> gooeyTarget = pokemon("Gooey-Opp", 220, 80, "", "gooey", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> gooeyLog = new LinkedHashMap<>();
        List<String> gooeyEvents = new ArrayList<>();
        support.applyReactiveContactEffects(state, attacker, gooeyTarget,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                gooeyLog, gooeyEvents, new FixedRandom(99));
        @SuppressWarnings("unchecked")
        Map<String, Object> attackerStages = (Map<String, Object>) attacker.get("statStages");
        assertEquals(-1, attackerStages.get("speed"));

        Map<String, Object> amuletAttacker = pokemon("Amulet-A", 240, 100, "clear-amulet", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> tanglingTarget = pokemon("Tangling-Opp", 220, 80, "", "tangling-hair", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> tanglingLog = new LinkedHashMap<>();
        List<String> tanglingEvents = new ArrayList<>();
        support.applyReactiveContactEffects(state, amuletAttacker, tanglingTarget,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                tanglingLog, tanglingEvents, new FixedRandom(99));
        @SuppressWarnings("unchecked")
        Map<String, Object> amuletStages = (Map<String, Object>) amuletAttacker.get("statStages");
        assertTrue(amuletStages == null || !amuletStages.containsKey("speed") || Integer.valueOf(0).equals(amuletStages.get("speed")));
        assertEquals(Boolean.TRUE, tanglingLog.get("speedDropBlocked"));
    }

    @Test
    void applyReactiveContactEffects_aftermathDamagesAttackerWhenHolderFaints() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Aftermath-Opp", 220, 80, "", "aftermath", DamageCalculatorUtil.TYPE_NORMAL);
        target.put("currentHp", 0);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                contactMove("Contact Strike", "contact-strike", 70, DamageCalculatorUtil.TYPE_NORMAL),
                actionLog, events, new FixedRandom(99));

        assertEquals(180, attacker.get("currentHp"));
        assertEquals(60, actionLog.get("aftermath"));
        assertTrue(events.stream().anyMatch(event -> event.contains("引爆")));
    }

    @Test
    void applyReactiveContactEffects_cursedBodyDisablesMoveThatHitHolderAndMentalHerbClearsIt() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));

        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Cursed-Opp", 220, 80, "", "cursed-body", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();
        support.applyReactiveContactEffects(state, attacker, target,
                move("Shadow Ball", "shadow-ball", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_GHOST, 10),
                actionLog, events, new FixedRandom(0));
        assertEquals(4, attacker.get("disableTurns"));
        assertEquals("shadow-ball", attacker.get("disableMove"));
        assertEquals(Boolean.TRUE, actionLog.get("cursedBody"));

        Map<String, Object> herbAttacker = pokemon("Herb-A", 240, 100, "mental-herb", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> herbLog = new LinkedHashMap<>();
        List<String> herbEvents = new ArrayList<>();
        support.applyReactiveContactEffects(state, herbAttacker, target,
                move("Thunderbolt", "thunderbolt", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_ELECTRIC, 10),
                herbLog, herbEvents, new FixedRandom(0));
        assertEquals(0, herbAttacker.get("disableTurns"));
        assertNull(herbAttacker.get("disableMove"));
        assertEquals(Boolean.TRUE, herbLog.get("mentalHerb"));
    }

    @Test
    void applyReactiveContactEffects_cursedBodyCanDisableMoveWhenHolderFaints() {
        BattleConditionSupport support = createSupport();
        Map<String, Object> state = new LinkedHashMap<>(Map.of("currentRound", 1));
        Map<String, Object> attacker = pokemon("Contact-A", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> target = pokemon("Cursed-Opp", 220, 80, "", "cursed-body", DamageCalculatorUtil.TYPE_NORMAL);
        target.put("currentHp", 0);
        Map<String, Object> actionLog = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();

        support.applyReactiveContactEffects(state, attacker, target,
                move("Shadow Ball", "shadow-ball", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_GHOST, 10),
                actionLog, events, new FixedRandom(0));

        assertEquals(4, attacker.get("disableTurns"));
        assertEquals("shadow-ball", attacker.get("disableMove"));
        assertEquals(Boolean.TRUE, actionLog.get("cursedBody"));
    }

    @Test
    void applyReactiveDamageAbilities_supportsWeakArmorStaminaJustifiedRattledSteamEngineAndBerserk() {
        BattleConditionSupport support = createSupport();

        Map<String, Object> weakArmor = pokemon("WeakArmor-A", 240, 100, "", "weak-armor", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> weakArmorLog = new LinkedHashMap<>();
        List<String> weakArmorEvents = new ArrayList<>();
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                weakArmor,
                move("Contact Strike", "contact-strike", 70, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 10),
                240,
                180,
                60,
                weakArmorLog,
                weakArmorEvents
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> weakArmorStages = (Map<String, Object>) weakArmor.get("statStages");
        assertEquals(-1, weakArmorStages.get("defense"));
        assertEquals(2, weakArmorStages.get("speed"));

        Map<String, Object> amuletWeakArmor = pokemon("Amulet-A", 240, 100, "clear-amulet", "weak-armor", DamageCalculatorUtil.TYPE_NORMAL);
        Map<String, Object> amuletLog = new LinkedHashMap<>();
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                amuletWeakArmor,
                move("Contact Strike", "contact-strike", 70, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, DamageCalculatorUtil.TYPE_NORMAL, 10),
                240,
                180,
                60,
                amuletLog,
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> amuletStages = (Map<String, Object>) amuletWeakArmor.get("statStages");
        assertEquals(0, amuletStages.get("defense"));
        assertEquals(2, amuletStages.get("speed"));

        Map<String, Object> stamina = pokemon("Stamina-A", 240, 100, "", "stamina", DamageCalculatorUtil.TYPE_NORMAL);
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                stamina,
                move("Water Pulse", "water-pulse", 60, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10),
                240,
                200,
                40,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> staminaStages = (Map<String, Object>) stamina.get("statStages");
        assertEquals(1, staminaStages.get("defense"));

        Map<String, Object> justified = pokemon("Justified-A", 240, 100, "", "justified", DamageCalculatorUtil.TYPE_NORMAL);
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                justified,
                move("Snarl", "snarl", 55, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_DARK, 11),
                240,
                200,
                40,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> justifiedStages = (Map<String, Object>) justified.get("statStages");
        assertEquals(1, justifiedStages.get("attack"));

        Map<String, Object> rattled = pokemon("Rattled-A", 240, 100, "", "rattled", DamageCalculatorUtil.TYPE_NORMAL);
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                rattled,
                move("Shadow Ball", "shadow-ball", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_GHOST, 10),
                240,
                200,
                40,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> rattledStages = (Map<String, Object>) rattled.get("statStages");
        assertEquals(1, rattledStages.get("speed"));

        Map<String, Object> steamEngine = pokemon("Steam-A", 240, 100, "", "steam-engine", DamageCalculatorUtil.TYPE_NORMAL);
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                steamEngine,
                move("Scald", "scald", 80, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10),
                240,
                200,
                40,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> steamStages = (Map<String, Object>) steamEngine.get("statStages");
        assertEquals(6, steamStages.get("speed"));

        Map<String, Object> berserk = pokemon("Berserk-A", 240, 100, "", "berserk", DamageCalculatorUtil.TYPE_NORMAL);
        support.applyReactiveDamageAbilities(
                pokemon("Attacker", 240, 100, "", "", DamageCalculatorUtil.TYPE_NORMAL),
                berserk,
                move("Water Pulse", "water-pulse", 60, 100, 0, DamageCalculatorUtil.DAMAGE_CLASS_SPECIAL, DamageCalculatorUtil.TYPE_WATER, 10),
                160,
                100,
                60,
                new LinkedHashMap<>(),
                new ArrayList<>()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> berserkStages = (Map<String, Object>) berserk.get("statStages");
        assertEquals(1, berserkStages.get("specialAttack"));
    }

    private static BattleConditionSupport createSupport() {
        BattleFieldEffectSupport fieldEffectSupport = new BattleFieldEffectSupport();
        BattleEngine engine = createEngine();
        BattleDamageSupport damageSupport = new BattleDamageSupport(engine, createTypeMapper(), fieldEffectSupport, 50);
        return new BattleConditionSupport(engine, damageSupport, fieldEffectSupport);
    }

    private static BattleEngine createEngine() {
        return new BattleEngine(new SkillService(new SkillMapper() {
            @Override
            public List<Map<String, Object>> findAll() {
                return List.of();
            }
        }), createTypeMapper(), new ObjectMapper());
    }

    private static TypeEfficacyMapper createTypeMapper() {
        return new TypeEfficacyMapper() {
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
                if (damageTypeId == null || targetTypeId == null) {
                    return 100;
                }
                if (damageTypeId == DamageCalculatorUtil.TYPE_NORMAL && targetTypeId == DamageCalculatorUtil.TYPE_GHOST) {
                    return 0;
                }
                return 100;
            }
        };
    }

    private static Map<String, Object> pokemon(String name, int hp, int speed, String heldItem, String ability, int typeId) {
        Map<String, Object> pokemon = new LinkedHashMap<>();
        pokemon.put("name", name);
        pokemon.put("heldItem", heldItem);
        pokemon.put("itemConsumed", false);
        pokemon.put("currentHp", hp);
        pokemon.put("condition", null);
        pokemon.put("ability", ability);
        pokemon.put("types", List.of(Map.of("type_id", typeId, "name", String.valueOf(typeId))));
        pokemon.put("stats", new LinkedHashMap<>(Map.of(
                "hp", hp,
                "attack", 100,
                "defense", 90,
                "specialAttack", 95,
                "specialDefense", 90,
                "speed", speed
        )));
        return pokemon;
    }

    private static Map<String, Object> move(String name, String nameEn, int power, int accuracy, int priority,
                                            int damageClassId, int typeId, int targetId) {
        return Map.of(
                "name", name,
                "name_en", nameEn,
                "power", power,
                "accuracy", accuracy,
                "priority", priority,
                "damage_class_id", damageClassId,
                "type_id", typeId,
                "target_id", targetId
        );
    }

    private static Map<String, Object> withFlags(Map<String, Object> move, String... flags) {
        Map<String, Object> copied = new LinkedHashMap<>(move);
        copied.put("flags", List.of(flags));
        return copied;
    }

    private static Map<String, Object> contactMove(String name, String nameEn, int power, int typeId) {
        Map<String, Object> move = new LinkedHashMap<>(move(name, nameEn, power, 100, 0,
                DamageCalculatorUtil.DAMAGE_CLASS_PHYSICAL, typeId, 10));
        move.put("contact", true);
        return move;
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index = 0;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (values.length == 0) {
                return 0;
            }
            int value = values[Math.min(index, values.length - 1)];
            index += 1;
            return Math.floorMod(value, bound);
        }
    }
}
