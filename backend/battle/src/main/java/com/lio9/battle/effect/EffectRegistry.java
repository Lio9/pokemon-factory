package com.lio9.battle.effect;

import static com.lio9.battle.effect.MoveUtils.*;
import java.util.*;

/**
 * 特性/道具效果注册中心 + 引擎 dispatch 入口。
 * <p>
 * 仿 Showdown 的中央 map 设计：所有特性/道具的 handler 在此注册，
 * 引擎只调 dispatch 方法，不写 if/else。
 * </p>
 *
 * <pre>
 * // 新增一个特性 = 在这里加一个 register() 调用，引擎不用动
 * register(new AbilityHandler() {
 *     public String id() { return "my-ability"; }
 *     public double onSourceModifyDamage(AttackContext ctx, double modifier) {
 *         return ctx.moveTypeIs(FIRE) ? modifier * 1.5 : modifier;
 *     }
 * });
 * </pre>
 */
public final class EffectRegistry {

    private static final Map<String, AbilityHandler> ABILITIES = new LinkedHashMap<>();
    private static final Map<String, ItemHandler> ITEMS = new LinkedHashMap<>();

    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int NORMAL = PokemonType.NORMAL.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int FIRE = PokemonType.FIRE.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int WATER = PokemonType.WATER.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int ELECTRIC = PokemonType.ELECTRIC.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int GRASS = PokemonType.GRASS.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int ICE = PokemonType.ICE.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int FIGHTING = PokemonType.FIGHTING.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int POISON = PokemonType.POISON.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int GROUND = PokemonType.GROUND.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int FLYING = PokemonType.FLYING.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int PSYCHIC = PokemonType.PSYCHIC.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int BUG = PokemonType.BUG.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int ROCK = PokemonType.ROCK.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int GHOST = PokemonType.GHOST.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int DRAGON = PokemonType.DRAGON.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int DARK = PokemonType.DARK.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int STEEL = PokemonType.STEEL.id();
    /** @deprecated 请使用 {@link PokemonType} 枚举 */
    @Deprecated
    public static final int FAIRY = PokemonType.FAIRY.id();

    /** @deprecated 请使用 {@link PokemonType#PHYSICAL} */
    @Deprecated public static final int PHYSICAL = PokemonType.PHYSICAL;
    /** @deprecated 请使用 {@link PokemonType#SPECIAL} */
    @Deprecated public static final int SPECIAL = PokemonType.SPECIAL;

    static { registerAll(); }
    private EffectRegistry() {}

    // ========================================================================
    //  注册方法
    // ========================================================================

    public static void register(AbilityHandler h) { ABILITIES.put(h.id().toLowerCase(), h); }
    public static void register(ItemHandler h) { ITEMS.put(h.id().toLowerCase(), h); }
    public static AbilityHandler getAbility(String name) { return name == null ? null : ABILITIES.get(name.toLowerCase()); }
    public static ItemHandler getItem(String name) { return name == null ? null : ITEMS.get(name.toLowerCase()); }

    /** 供 EventRegistryBridge 迭代所有已注册特性 handler */
    public static Collection<AbilityHandler> getAllAbilities() { return ABILITIES.values(); }

    /** 供 EventRegistryBridge 迭代所有已注册道具 handler */
    public static Collection<ItemHandler> getAllItems() { return ITEMS.values(); }

    @SuppressWarnings("unchecked")
    public static String abilityName(Map<String, Object> mon) {
        Object a = mon.get("ability");
        if (a instanceof Map) {
            Object n = ((Map<String, Object>) a).get("name_en");
            return n == null ? "" : String.valueOf(n).toLowerCase();
        }
        // 兼容测试/部分场景中以 String 存储的特性名
        return a == null ? "" : String.valueOf(a).toLowerCase();
    }
    public static String heldItem(Map<String, Object> mon) {
        Object i = mon.get("heldItem");
        return i == null ? "" : String.valueOf(i).toLowerCase();
    }

    // ========================================================================
    //  Dispatch 方法 — 引擎只调这些，不写 if/else
    // ========================================================================

    /** 攻击方特性：修正伤害倍率 */
    public static double dispatchSourceDamage(Map<String, Object> attacker, AttackContext ctx, double mod) {
        AbilityHandler h = getAbility(abilityName(attacker));
        return h != null ? h.onSourceModifyDamage(ctx, mod) : mod;
    }

    /** 防御方特性：修正伤害倍率（含免疫返回 0） */
    public static double dispatchTargetDamage(Map<String, Object> defender, AttackContext ctx, double mod) {
        AbilityHandler h = getAbility(abilityName(defender));
        return h != null ? h.onTargetModifyDamage(ctx, mod) : mod;
    }

    /** 攻击方道具：修正伤害倍率（Klutz 阻塞） */
    public static double dispatchItemSourceDamage(Map<String, Object> attacker, AttackContext ctx, double mod) {
        if (hasKlutz(attacker)) return mod;
        ItemHandler h = getItem(heldItem(attacker));
        return h != null ? h.onSourceModifyDamage(ctx, mod) : mod;
    }

    /** 攻击方特性：修正攻击属性值 */
    public static int dispatchSourceAttack(Map<String, Object> attacker, AttackContext ctx, int stat) {
        AbilityHandler h = getAbility(abilityName(attacker));
        return h != null ? h.onSourceModifyAttackStat(ctx, stat) : stat;
    }

    /** 攻击方道具：修正攻击属性值（含 Choice Band/Specs）（Klutz 阻塞） */
    public static int dispatchItemSourceAttack(Map<String, Object> attacker, AttackContext ctx, int stat) {
        if (hasKlutz(attacker)) return stat;
        ItemHandler h = getItem(heldItem(attacker));
        return h != null ? h.onSourceModifyAttackStat(ctx, stat) : stat;
    }

    /** 防御方特性：修正防御属性值 */
    public static int dispatchTargetDefense(Map<String, Object> defender, AttackContext ctx, int stat) {
        AbilityHandler h = getAbility(abilityName(defender));
        return h != null ? h.onTargetModifyDefenseStat(ctx, stat) : stat;
    }

    /** 防御方道具：修正防御属性值（Klutz 阻塞） */
    public static int dispatchItemTargetDefense(Map<String, Object> defender, AttackContext ctx, int stat) {
        if (hasKlutz(defender)) return stat;
        ItemHandler h = getItem(heldItem(defender));
        return h != null ? h.onTargetModifyDefenseStat(ctx, stat) : stat;
    }

    /** 特性：修正速度值 */
    public static int dispatchSpeed(Map<String, Object> mon, SpeedContext ctx, int speed) {
        AbilityHandler h = getAbility(abilityName(mon));
        return h != null ? h.onModifySpeed(ctx, speed) : speed;
    }

    /** 道具：修正速度值（Klutz 阻塞） */
    public static int dispatchItemSpeed(Map<String, Object> mon, SpeedContext ctx, int speed) {
        if (hasKlutz(mon)) return speed;
        ItemHandler h = getItem(heldItem(mon));
        return h != null ? h.onModifySpeed(ctx, speed) : speed;
    }

    /** 特性：修正能力阶级变化（返回反转后的 delta） */
    public static int dispatchStatStage(Map<String, Object> target, StatStageContext ctx, int delta) {
        AbilityHandler h = getAbility(abilityName(target));
        return h != null ? h.onModifyStatStage(ctx, delta) : delta;
    }

    /** 特性：修正重量 */
    public static int dispatchWeight(Map<String, Object> mon, WeightContext ctx, int weight) {
        AbilityHandler h = getAbility(abilityName(mon));
        return h != null ? h.onModifyWeight(ctx, weight) : weight;
    }

    /** 道具：修正重量（Klutz 阻塞） */
    public static int dispatchItemWeight(Map<String, Object> mon, WeightContext ctx, int weight) {
        if (hasKlutz(mon)) return weight;
        ItemHandler h = getItem(heldItem(mon));
        return h != null ? h.onModifyWeight(ctx, weight) : weight;
    }

    /** 检查目标是否持有 Klutz 特性（道具无效化） */
    private static boolean hasKlutz(Map<String, Object> mon) {
        String ab = abilityName(mon);
        return "klutz".equals(ab);
    }

    /** 装备是否在登记表中（用于判断是否为「有效道具」） */
    public static boolean isRegisteredItem(String name) {
        return name != null && ITEMS.containsKey(name.toLowerCase());
    }

    // ====== 状态/精神免疫 ======

    /** 特性：检查目标是否免疫某种状态（paralysis/burn/sleep/poison/freeze/confusion） */
    public static boolean dispatchStatusImmunity(Map<String, Object> target, StatusContext ctx) {
        AbilityHandler h = getAbility(abilityName(target));
        return h != null && h.onStatusImmunity(ctx);
    }

    /** 特性：检查目标是否免疫精神类控制（taunt/attract/disable/encore/torment等） */
    public static boolean dispatchMentalImmunity(Map<String, Object> target, StatusContext ctx) {
        AbilityHandler h = getAbility(abilityName(target));
        return h != null && h.onMentalImmunity(ctx);
    }

    // ====== 接触反制 ======

    /**
     * 特性：接触招式命中后触发。返回非 null 表示触发了反制效果。
     * handler 可以修改 context 中的 mutable 字段来描述效果（如 inflicting status）。
     */
    public static void dispatchContact(Map<String, Object> target, ContactContext ctx) {
        AbilityHandler h = getAbility(abilityName(target));
        if (h != null) h.onContact(ctx);
    }

    // ====== 招式阻挡（声音/弹道/风等） ======

    /** 特性：检查目标是否免疫某个招式（基于 flag 或属性）。返回免疫的特性名，null 表示不免疫 */
    public static String dispatchMoveBlock(Map<String, Object> target, ImmunityContext ctx) {
        AbilityHandler h = getAbility(abilityName(target));
        return h != null && h.onTypeImmunity(ctx) ? h.id() : null;
    }

    /** 特性：修正追加效果发动概率（如天恩翻倍）。返回修正后的 chance */
    public static int dispatchSereneGrace(Map<String, Object> actor, int chance) {
        AbilityHandler h = getAbility(abilityName(actor));
        return h != null ? h.onModifySecondaryEffectChance(chance) : chance;
    }

    /** 特性：检查是否阻挡追加效果（如鳞粉） */
    public static boolean dispatchBlocksSecondaryEffects(Map<String, Object> target) {
        AbilityHandler h = getAbility(abilityName(target));
        return h != null && h.onBlocksSecondaryEffects();
    }

    /** 特性：检查是否阻挡能力下降（恒净之躯/白烟/金属防护） */
    public static boolean dispatchStatDropBlocked(Map<String, Object> mon) {
        AbilityHandler h = getAbility(abilityName(mon));
        return h != null && h.onBlocksStatDrop();
    }

    // ====== 回合流程钩子 ======

    /** 特性：上场时触发 */
    public static void dispatchSwitchIn(SwitchInContext ctx) {
        AbilityHandler h = getAbility(abilityName(ctx.mon));
        if (h != null) h.onSwitchIn(ctx);
    }

    /** 特性：使用招式前触发 */
    public static void dispatchBeforeMove(BeforeMoveContext ctx) {
        AbilityHandler h = getAbility(abilityName(ctx.actor));
        if (h != null) h.onBeforeMove(ctx);
    }

    /** 防御方特性：受到伤害后触发 */
    public static void dispatchDamageReceived(Map<String, Object> target, DamageReceivedContext ctx) {
        AbilityHandler h = getAbility(abilityName(target));
        if (h != null) h.onDamageReceived(ctx);
    }

    /** 防御方道具：受到伤害后触发 */
    public static void dispatchItemDamageReceived(Map<String, Object> target, DamageReceivedContext ctx) {
        ItemHandler h = getItem(heldItem(target));
        if (h != null) h.onDamageReceived(ctx);
    }

    // ========================================================================
    //  全部 handler 注册
    // ========================================================================

    private static void registerAll() {
        // ========== 攻击方伤害倍率特性 ==========

        // Technician: 威力≤60 x1.5
        regAbility(new Ab() {
            public String id() { return "technician"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int p = ctx.movePower();
                return p > 0 && p <= 60 ? mod * 1.5 : mod;
            }
        });
        // Iron Fist: 拳类 x1.2
        regAbility(new Ab() {
            public String id() { return "iron-fist"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("punch", ctx.move) ? mod * 1.2 : mod;
            }
        });
        // Reckless: 反伤招式 x1.2
        regAbility(new Ab() {
            public String id() { return "reckless"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isRecoilMove(ctx.move) ? mod * 1.2 : mod;
            }
        });
        // Hustle: 物理 x1.5
        regAbility(new Ab() {
            public String id() { return "hustle"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });
        // Gorilla Tactics: 物理攻击 x1.5 + 锁定招式（锁定逻辑在 rememberChoiceMove 中）
        regAbility(new Ab() {
            public String id() { return "gorilla-tactics"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        // Strong Jaw: 咬类 x1.5
        regAbility(new Ab() {
            public String id() { return "strong-jaw"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("bite", ctx.move) || moveCategory("fang", ctx.move) ? mod * 1.5 : mod;
            }
        });
        // Mega Launcher: 波导类 x1.5
        regAbility(new Ab() {
            public String id() { return "mega-launcher"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                String n = moveName(ctx.move);
                return n.contains("pulse") || n.contains("aura-sphere") || n.contains("aura sphere") ? mod * 1.5 : mod;
            }
        });
        // Sharpness: 切割类 x1.5
        regAbility(new Ab() {
            public String id() { return "sharpness"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSlicingMove(ctx.move) ? mod * 1.5 : mod;
            }
        });
        // Punk Rock: 声音类 x1.3
        regAbility(new Ab() {
            public String id() { return "punk-rock"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSoundMove(ctx.move) ? mod * 1.3 : mod;
            }
        });
        // Steely Spirit: 钢系 x1.5
        regAbility(new Ab() {
            public String id() { return "steely-spirit"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(STEEL) ? mod * 1.5 : mod;
            }
        });
        // Transistor: 电系 x1.3
        regAbility(new Ab() {
            public String id() { return "transistor"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? mod * 1.3 : mod;
            }
        });
        // Dragon's Maw: 龙系 x1.5
        regAbility(new Ab() {
            public String id() { return "dragons-maw"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(DRAGON) ? mod * 1.5 : mod;
            }
        });
        // Steelworker: 钢系 x1.5
        regAbility(new Ab() {
            public String id() { return "steelworker"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(STEEL) ? mod * 1.5 : mod;
            }
        });
        // Sand Force: 沙暴中岩/地/钢 x1.3
        regAbility(new Ab() {
            public String id() { return "sand-force"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "sandTurns") && ctx.moveTypeIs(ROCK, GROUND, STEEL) ? mod * 1.3 : mod;
            }
        });
        // Tinted Lens: 效果不好时 x2
        regAbility(new Ab() {
            public String id() { return "tinted-lens"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                if (mod > 0 && mod < 1) return mod * 2;
                return mod;
            }
        });
        // Guts: 异常时物理 x1.5
        regAbility(new Ab() {
            public String id() { return "guts"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return hasStatus(ctx.attackerCondition()) && ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });
        // Huge Power / Pure Power: 物理攻击 x2
        regAbility(new Ab() {
            public String id() { return "huge-power"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? stat * 2 : stat;
            }
        });
        regAbility(new Ab() {
            public String id() { return "pure-power"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? stat * 2 : stat;
            }
        });
        // Flare Boost: 灼伤时特殊 x1.5
        regAbility(new Ab() {
            public String id() { return "flare-boost"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return "burn".equals(ctx.attackerCondition()) && ctx.damageClassId == SPECIAL ? mod * 1.5 : mod;
            }
        });
        // Toxic Boost: 中毒时物理 x1.5
        regAbility(new Ab() {
            public String id() { return "toxic-boost"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                String c = ctx.attackerCondition();
                return ("poison".equals(c) || "toxic".equals(c)) && ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });
        // Sheer Force: 有追加效果 x1.3
        regAbility(new Ab() {
            public String id() { return "sheer-force"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return hasEffectChance(ctx.move) ? mod * 1.3 : mod;
            }
        });
        // Solar Power: 晴天时特攻 x1.5
        regAbility(new Ab() {
            public String id() { return "solar-power"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL && fieldActive(ctx.state, "sunTurns") ? mod * 1.5 : mod;
            }
        });
        // Analytic: 最后行动时 x1.3（由上层标记）
        regAbility(new Ab() {
            public String id() { return "analytic"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return Boolean.TRUE.equals(ctx.attacker.get("analyticActive")) ? mod * 1.3 : mod;
            }
        });
        // Supreme Overlord: 每只阵亡 +10%
        regAbility(new Ab() {
            public String id() { return "supreme-overlord"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int fainted = intVal(ctx.attacker, "faintedAllies");
                return fainted > 0 ? mod * (1.0 + fainted * 0.1) : mod;
            }
        });
        // Protosynthesis / Quark Drive: 1.3x
        regProtoQuark();
        // Orichalcum Pulse: 晴天时 x1.3
        regAbility(new Ab() {
            public String id() { return "orichalcum-pulse"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "sunTurns") ? mod * 1.3 : mod;
            }
        });
        // Hadron Engine: 电场时 x1.3
        regAbility(new Ab() {
            public String id() { return "hadron-engine"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "electricTerrainTurns") ? mod * 1.3 : mod;
            }
        });

        // Normalize: 一般系外招式 1.2x（+ 属性变更由引擎 STAB 前处理）
        regAbility(new Ab() {
            public String id() { return "normalize"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeId != NORMAL ? mod * 1.2 : mod;
            }
        });

        // ========== HP < 1/3 时属性增伤 ==========
        regOvergrowLike("overgrow", GRASS);
        regOvergrowLike("blaze", FIRE);
        regOvergrowLike("torrent", WATER);
        regOvergrowLike("swarm", BUG);

        // ========== 防御方伤害倍率特性 ==========

        regAbility(new Ab() {
            public String id() { return "thick-fat"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE, ICE) ? mod * 0.5 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "heatproof"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? mod * 0.5 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "water-bubble"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? mod * 0.5 : mod;
            }
        });
        // Dry Skin: 1.25x Fire, Water immunity
        regAbility(new Ab() {
            public String id() { return "dry-skin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                if (ctx.moveTypeIs(WATER)) return 0;
                return ctx.moveTypeIs(FIRE) ? mod * 1.25 : mod;
            }
        });
        // Filter / Solid Rock / Prism Armor: 克制伤害 x0.75
        regFilterLike("filter");
        regFilterLike("solid-rock");
        regFilterLike("prism-armor");
        // Ice Scales: 特殊减半
        regAbility(new Ab() {
            public String id() { return "ice-scales"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod * 0.5 : mod;
            }
        });
        // Fur Coat: 物理减半
        regAbility(new Ab() {
            public String id() { return "fur-coat"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 0.5 : mod;
            }
        });
        // Purifying Salt: 鬼系减半
        regAbility(new Ab() {
            public String id() { return "purifying-salt"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GHOST) ? mod * 0.5 : mod;
            }
        });
        // Fluffy: 接触伤害减半（×0.5），火系伤害加倍（×2）
        regAbility(new Ab() {
            public String id() { return "fluffy"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                if (ctx.moveTypeIs(FIRE)) return mod * 2;
                return hasMoveFlag(ctx.move, "contact") ? mod * 0.5 : mod;
            }
        });
        // Sword of Ruin (+33% 物理伤害 = 防御方物防 reduced 25%)
        regAbility(new Ab() {
            public String id() { return "sword-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod / 0.75 : mod;
            }
        });
        // Tablets of Ruin (+33% 物理伤害 = 防御方物攻 reduced 25%)
        regAbility(new Ab() {
            public String id() { return "tablets-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod / 0.75 : mod;
            }
        });
        // Vessel of Ruin (+33% 特殊伤害 = 防御方特攻 reduced 25%)
        regAbility(new Ab() {
            public String id() { return "vessel-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod / 0.75 : mod;
            }
        });
        // Beads of Ruin (+33% 特殊伤害 = 防御方特防 reduced 25%)
        regAbility(new Ab() {
            public String id() { return "beads-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod / 0.75 : mod;
            }
        });

        // ========== 防御方免疫（返回 0） ==========

        // Sap Sipper: 草免
        regAbility(new Ab() {
            public String id() { return "sap-sipper"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GRASS) ? 0 : mod;
            }
        });
        // Storm Drain: 水免
        regAbility(new Ab() {
            public String id() { return "storm-drain"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(WATER) ? 0 : mod;
            }
        });
        // Lightning Rod / Volt Absorb: 电免
        regAbility(new Ab() {
            public String id() { return "lightning-rod"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "volt-absorb"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });
        // Water Absorb: 水免
        regAbility(new Ab() {
            public String id() { return "water-absorb"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(WATER) ? 0 : mod;
            }
        });
        // Flash Fire: 火免
        regAbility(new Ab() {
            public String id() { return "flash-fire"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? 0 : mod;
            }
        });
        // Motor Drive: 电免
        regAbility(new Ab() {
            public String id() { return "motor-drive"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });
        // Well-Baked Body: 火免
        regAbility(new Ab() {
            public String id() { return "well-baked-body"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? 0 : mod;
            }
        });
        // Earth Eater: 地免
        regAbility(new Ab() {
            public String id() { return "earth-eater"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GROUND) ? 0 : mod;
            }
        });
        // Wind Rider: 风系招式免
        regAbility(new Ab() {
            public String id() { return "wind-rider"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return isWindMove(ctx.move) ? 0 : mod;
            }
        });
        // Levitate: 地免
        regAbility(new Ab() {
            public String id() { return "levitate"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GROUND) ? 0 : mod;
            }
        });

        // ========== 攻击属性值修正特性 ==========

        // Marvel Scale: 异常时物防 x1.5
        regAbility(new Ab() {
            public String id() { return "marvel-scale"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && hasStatus(ctx.defenderCondition())
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
        });

        // ========== 速度修正特性 ==========

        regAbility(new Ab() {
            public String id() { return "swift-swim"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "rainTurns") ? speed * 2 : speed;
            }
        });
        regAbility(new Ab() {
            public String id() { return "chlorophyll"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "sunTurns") ? speed * 2 : speed;
            }
        });
        regAbility(new Ab() {
            public String id() { return "sand-rush"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "sandTurns") ? speed * 2 : speed;
            }
        });
        regAbility(new Ab() {
            public String id() { return "slush-rush"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "snowTurns") ? speed * 2 : speed;
            }
        });
        regAbility(new Ab() {
            public String id() { return "surge-surfer"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "electricTerrainTurns") ? speed * 2 : speed;
            }
        });
        // Unburden: 消耗道具后速度翻倍
        regAbility(new Ab() {
            public String id() { return "unburden"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return Boolean.TRUE.equals(ctx.mon.get("unburdenActive")) ? speed * 2 : speed;
            }
        });
        // 飞毛腿: 异常时速度 x1.5
        regAbility(new Ab() {
            public String id() { return "quick-feet"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return hasStatus(strVal(ctx.mon, "condition")) ? (int) Math.floor(speed * 1.5) : speed;
            }
        });

        // ========== 重量修正特性 ==========

        regAbility(new Ab() {
            public String id() { return "heavy-metal"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return weight * 2; }
        });
        regAbility(new Ab() {
            public String id() { return "light-metal"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return Math.max(1, weight / 2); }
        });

        // ========== 能力阶级变化特性 ==========

        // Contrary 用独立的类
        register(new com.lio9.battle.effect.handler.ContraryAbility());

        // 单纯：能力变化量翻倍
        regAbility(new Ab() {
            public String id() { return "simple"; }
            public int onModifyStatStage(StatStageContext ctx, int delta) { return delta * 2; }
        });

        // ====================================================================
        //  追加效果修正
        // ====================================================================

        // 天恩：追加效果概率翻倍
        regAbility(new Ab() {
            public String id() { return "serene-grace"; }
            public int onModifySecondaryEffectChance(int chance) { return Math.min(100, chance * 2); }
        });

        // 鳞粉：完全阻挡追加效果
        regAbility(new Ab() {
            public String id() { return "shield-dust"; }
            public boolean onBlocksSecondaryEffects() { return true; }
        });

        // 能力下降阻挡：恒净之躯/白烟/金属防护
        regAbility(new Ab() {
            public String id() { return "clear-body"; }
            public boolean onBlocksStatDrop() { return true; }
        });
        regAbility(new Ab() {
            public String id() { return "white-smoke"; }
            public boolean onBlocksStatDrop() { return true; }
        });
        regAbility(new Ab() {
            public String id() { return "full-metal-body"; }
            public boolean onBlocksStatDrop() { return true; }
        });

        // ====================================================================
        //  状态/精神免疫
        // ====================================================================

        // 麻痹免疫：柔软
        regAbility(new Ab() {
            public String id() { return "limber"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "paralysis".equals(ctx.condition); }
        });
        // 灼伤免疫：水幕、水泡
        regAbility(new Ab() {
            public String id() { return "water-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "burn".equals(ctx.condition); }
        });
        regAbility(new Ab() {
            public String id() { return "water-bubble"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "burn".equals(ctx.condition); }
        });
        // 睡眠免疫：不眠、干劲
        regAbility(new Ab() {
            public String id() { return "insomnia"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "sleep".equals(ctx.condition); }
        });
        regAbility(new Ab() {
            public String id() { return "vital-spirit"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "sleep".equals(ctx.condition); }
        });
        // 混乱免疫：我行我素
        regAbility(new Ab() {
            public String id() { return "own-tempo"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "confusion".equals(ctx.condition); }
        });
        // 冰冻免疫：熔岩铠甲
        regAbility(new Ab() {
            public String id() { return "magma-armor"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "freeze".equals(ctx.condition); }
        });
        // 中毒免疫：免疫
        regAbility(new Ab() {
            public String id() { return "immunity"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "poison".equals(ctx.condition) || "toxic".equals(ctx.condition); }
        });
        // 迟钝：精神类免疫（挑衅/着迷/威吓）
        regAbility(new Ab() {
            public String id() { return "oblivious"; }
            public boolean onMentalImmunity(StatusContext ctx) {
                return switch (ctx.condition) {
                    case "taunt", "attract", "intimidate" -> true;
                    default -> false;
                };
            }
        });
        // 芳香幕（自身）：精神类免疫
        regAbility(new Ab() {
            public String id() { return "aroma-veil"; }
            public boolean onMentalImmunity(StatusContext ctx) {
                return switch (ctx.condition) {
                    case "taunt", "attract", "disable", "encore", "heal-block", "torment" -> true;
                    default -> false;
                };
            }
        });

        // ====================================================================
        //  Phase 1.4 状态/治愈类特性
        // ====================================================================

        // 树枕尾熊的 Comatose：永远视为睡眠但可以正常行动，免疫其他所有异常
        regAbility(new Ab() {
            public String id() { return "comatose"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return !"sleep".equals(ctx.condition);
            }
        });
        // 甜幕（Sweet Veil）：队友睡眠免疫（在引擎中实现团队检查）
        // 仅注册基本免疫，团队范围检查在 BattleConditionSupport 中
        regAbility(new Ab() {
            public String id() { return "sweet-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return "sleep".equals(ctx.condition) || "yawn".equals(ctx.condition);
            }
        });
        // 彩幕（Pastel Veil）：队友中毒免疫，切换时治愈中毒（团队检查在引擎中实现）
        regAbility(new Ab() {
            public String id() { return "pastel-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return "poison".equals(ctx.condition) || "toxic".equals(ctx.condition);
            }
        });
        // 叶子防守（Leaf Guard）：大晴天时免疫异常状态
        regAbility(new Ab() {
            public String id() { return "leaf-guard"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return fieldActive(ctx.state, "sunTurns");
            }
        });
        // 鲜花帷幕（Flower Veil）：草属性队友免疫异常状态和能力下降（引擎中实现团队检查）
        regAbility(new Ab() {
            public String id() { return "flower-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return true; // 用于自身，队友检查在引擎中实现
            }
        });

        // ====================================================================
        //  接触反制
        // ====================================================================

        // 粗糙皮肤：接触时反伤 1/8（引擎通过 ContactContext.result 获取效果类型）
        regAbility(new Ab() {
            public String id() { return "rough-skin"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "rough-skin");
            }
        });
        // 铁刺
        regAbility(new Ab() {
            public String id() { return "iron-barbs"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "iron-barbs");
            }
        });
        // 引爆（倒下时）
        regAbility(new Ab() {
            public String id() { return "aftermath"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "aftermath");
            }
        });
        // 静电：30% 麻痹
        regAbility(new Ab() {
            public String id() { return "static"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "static");
                ctx.result.put("chance", 30);
            }
        });
        // 火焰之躯：30% 灼伤
        regAbility(new Ab() {
            public String id() { return "flame-body"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "flame-body");
                ctx.result.put("chance", 30);
            }
        });
        // 毒刺：30% 中毒
        regAbility(new Ab() {
            public String id() { return "poison-point"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "poison-point");
                ctx.result.put("chance", 30);
            }
        });
        // 孢子：各 10% 麻痹/灼伤/睡眠
        regAbility(new Ab() {
            public String id() { return "effect-spore"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "effect-spore");
                ctx.result.put("chance", 30); // 总概率 30%，分 3 种各 10%
            }
        });
        // 迷人之躯：30% 着迷
        regAbility(new Ab() {
            public String id() { return "cute-charm"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "cute-charm");
                ctx.result.put("chance", 30);
            }
        });
        // 黏滑 / 卷发：接触时降低速度
        regAbility(new Ab() {
            public String id() { return "gooey"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "gooey");
            }
        });
        regAbility(new Ab() {
            public String id() { return "tangling-hair"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "tangling-hair");
            }
        });
        // 幽香气息：接触后对方特性变为幽香气息
        regAbility(new Ab() {
            public String id() { return "lingering-aroma"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "lingering-aroma");
            }
        });
        // 灭亡之躯：接触后双方进入灭亡之歌状态（3回合）
        regAbility(new Ab() {
            public String id() { return "perish-body"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "perish-body");
            }
        });

        // ====================================================================
        //  招式阻挡（声音/弹道/风）
        // ====================================================================

        // 隔音：阻挡声音招式
        regAbility(new Ab() {
            public String id() { return "soundproof"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "sound");
            }
        });
        // 防弹：阻挡弹道招式
        regAbility(new Ab() {
            public String id() { return "bulletproof"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "bullet");
            }
        });
        // 乘风：阻挡风类招式
        regAbility(new Ab() {
            public String id() { return "wind-rider"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "wind");
            }
        });
        // 黄金之躯：完全阻挡变化招式（非攻击）
        regAbility(new Ab() {
            public String id() { return "good-as-gold"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return ctx.moveTypeId == 0; // status move
            }
        });
        // 吸盘（Suction Cups）：不会被强制替换
        regAbility(new Ab() {
            public String id() { return "suction-cups"; }
        });
        // 洁净之盐：阻挡变化招式
        regAbility(new Ab() {
            public String id() { return "purifying-salt"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return ctx.moveTypeId == 0; // status move
            }
        });

        // ====================================================================
        //  道具
        // ====================================================================

        // 道具 handler 委托到 ItemHandlers
        // ====================================================================
        //  Phase 1.8 其他特殊类特性
        // ====================================================================

        // 沙隐：沙暴中闪避率提升（×0.8 对手命中率），由引擎 BATTLE_ROUND_SUPPORT 实现
        regAbility(new Ab() {
            public String id() { return "sand-veil"; }
        });
        // 雪隐：雪天中闪避率提升（×0.8 对手命中率），由引擎 BATTLE_ROUND_SUPPORT 实现
        regAbility(new Ab() {
            public String id() { return "snow-cloak"; }
        });
        // 污泥浆：受到吸取类招式时，攻击者受到等量伤害而非回复
        regAbility(new Ab() {
            public String id() { return "liquid-ooze"; }
        });
        // 笨拙：携带道具无效（由引擎 HEALD_ITEM 检查时判断）
        regAbility(new Ab() {
            public String id() { return "klutz"; }
        });
        // 紧张感：对手无法使用树果（由引擎 BERRY_CONSUME 检查时判断）
        regAbility(new Ab() {
            public String id() { return "unnerve"; }
        });
        // 压迫感：对手招式消耗 2 PP（需 PP 系统支持）
        regAbility(new Ab() {
            public String id() { return "pressure"; }
        });
        // 顺手牵羊：被接触时夺取对手道具
        regAbility(new Ab() {
            public String id() { return "pickpocket"; }
        });
        // 魔术师：攻击时夺取对手道具
        regAbility(new Ab() {
            public String id() { return "magician"; }
        });
        // 颊囊：食用树果时额外回复 HP
        regAbility(new Ab() {
            public String id() { return "cheek-pouch"; }
        });
        // 熟成：树果效果翻倍
        regAbility(new Ab() {
            public String id() { return "ripen"; }
        });
        // 化学变化气体：场上特性失效
        regAbility(new Ab() {
            public String id() { return "neutralizing-gas"; }
        });
        // 螺旋尾羽/坚毅：无视避雷针/引水等改变目标的特性
        regAbility(new Ab() {
            public String id() { return "stalwart"; }
        });
        regAbility(new Ab() {
            public String id() { return "propeller-tail"; }
        });
        // 游离之物：被接触时交换特性
        regAbility(new Ab() {
            public String id() { return "wandering-spirit"; }
        });
        // 捡球：无道具时捡回第一回合使用的精灵球
        regAbility(new Ab() {
            public String id() { return "ball-fetch"; }
        });

        // ========== 补齐缺失的重要特性 ==========

        // 破格：无视对手特性
        regAbility(new Ab() {
            public String id() { return "mold-breaker"; }
        });

        // 魔法镜：反弹变化招式
        regAbility(new Ab() {
            public String id() { return "magic-bounce"; }
        });

        // 治愈之心：每回合结束时随机治愈队友异常状态
        regAbility(new Ab() {
            public String id() { return "healer"; }
        });

        // 自然恢复：交换时治愈异常状态
        regAbility(new Ab() {
            public String id() { return "natural-cure"; }
        });

        // 同步：受到异常状态时传染给攻击者
        regAbility(new Ab() {
            public String id() { return "synchronize"; }
        });

        // 早起：睡眠回合减半
        regAbility(new Ab() {
            public String id() { return "early-bird"; }
        });

        // 快速苏醒：治愈睡眠后速度提升1级
        regAbility(new Ab() {
            public String id() { return "quick-feet"; }
        });

        // 湿润之躯：雨天时治愈异常状态
        regAbility(new Ab() {
            public String id() { return "hydration"; }
        });

        // 再生力：交换时回复1/3最大HP
        regAbility(new Ab() {
            public String id() { return "regenerator"; }
        });

        // 收获：每回合结束时恢复1/8最大HP
        regAbility(new Ab() {
            public String id() { return "harvest"; }
        });

        // 贪吃鬼：HP低于1/2时优先吃树果
        regAbility(new Ab() {
            public String id() { return "gluttony"; }
        });

        // 捡拾：战斗结束后随机获得道具
        regAbility(new Ab() {
            public String id() { return "pickup"; }
        });

        // 偷换：交换时偷走对手道具
        regAbility(new Ab() {
            public String id() { return "switcheroo"; }
        });

        // 顺手牵羊：被接触时偷走对手道具
        regAbility(new Ab() {
            public String id() { return "pickpocket"; }
        });

        // 魔术师：攻击时偷走对手道具
        regAbility(new Ab() {
            public String id() { return "magician"; }
        });

        // 超幸运：会心一击等级+1
        regAbility(new Ab() {
            public String id() { return "super-luck"; }
        });

        // 天恩：追加效果概率翻倍
        // 已在上方注册

        // 狙击手：会心一击倍率提升
        // 已在上方注册

        // 加速：每回合速度+1
        // 已在上方注册

        // 下载：登场时提升攻击或特攻
        // 已在上方注册

        // 异兽提升：击倒对手后提升最高能力
        // 已在上方注册

        // 不服输/好胜：能力下降时提升攻击/特攻
        // 已在上方注册

        // 镜甲：反弹能力下降
        // 已在上方注册

        // 神奇守护：只有效果绝佳的招式才能命中
        // 已在上方注册

        // 清除浓雾：入场时清除天气
        regAbility(new Ab() {
            public String id() { return "cloud-nine"; }
        });

        // 气闸：入场时清除天气
        regAbility(new Ab() {
            public String id() { return "air-lock"; }
        });

        // 魔法防守：不会受到间接伤害
        regAbility(new Ab() {
            public String id() { return "magic-guard"; }
        });

        // 不屈之心：畏缩时攻击+1
        regAbility(new Ab() {
            public String id() { return "steadfast"; }
        });

        // 胆量：不会畏缩
        regAbility(new Ab() {
            public String id() { return "guts"; }
        });

        // 精神力：不会畏缩
        // 已在上方注册

        // 厚脂肪：火/冰系伤害减半
        // 已在上方注册

        // 耐热：火系伤害减半
        // 已在上方注册

        // 水幕：灼伤免疫
        // 已在上方注册

        // 液态水膜：火系伤害减半，灼伤免疫
        // 已在上方注册

        // 干燥皮肤：水系免疫，火系伤害×1.25，雨天回复HP
        // 已在上方注册

        // 避雷针/蓄电：电系免疫，提升特攻
        // 已在上方注册

        // 引水/吸水：水系免疫，提升特攻/回复HP
        // 已在上方注册

        // 闪电引擎：电系免疫，提升速度
        regAbility(new Ab() {
            public String id() { return "motor-drive"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });

        // 火焰之躯：接触时30%灼伤
        // 已在上方注册

        // 毒刺：接触时30%中毒
        // 已在上方注册

        // 静电：接触时30%麻痹
        // 已在上方注册

        // 孢子：接触时各10%麻痹/灼伤/睡眠
        // 已在上方注册

        // 迷人之躯：接触时30%着迷
        // 已在上方注册

        // 粗糙皮肤/铁刺：接触时反伤
        // 已在上方注册

        // 引爆：被击倒时造成伤害
        // 已在上方注册

        // 硬壳盔甲：不会被会心一击
        regAbility(new Ab() {
            public String id() { return "shell-armor"; }
        });

        // 战斗盔甲：不会被会心一击
        regAbility(new Ab() {
            public String id() { return "battle-armor"; }
        });

        // 防尘：不受沙暴伤害，不会陷入沙隐闪避加成
        regAbility(new Ab() {
            public String id() { return "sand-veil"; }
        });

        // 雪隐：雪天时闪避率提升
        // 已在上方注册

        // 污泥浆：受到吸取类招式时反伤
        // 已在上方注册

        // 笨拙：道具无效
        // 已在上方注册

        // 紧张感：对手无法使用树果
        // 已在上方注册

        // 压迫感：对手招式消耗2PP
        // 已在上方注册

        // 化学变化气体：场上特性失效
        // 已在上方注册

        // 螺旋尾羽/坚毅：无视改变目标的特性
        // 已在上方注册

        // 清除之羽：入场时清除对方场地效果
        regAbility(new Ab() {
            public String id() { return "clear-smog"; }
        });

        // 清扫之羽：入场时清除对方场地效果
        regAbility(new Ab() {
            public String id() { return "defog"; }
        });

        // 治愈之愿：退场时治愈队友
        regAbility(new Ab() {
            public String id() { return "heal-pulse"; }
        });

        // 友谊守护：队友受到致命伤害时替其承受
        regAbility(new Ab() {
            public String id() { return "friend-guard"; }
        });

        // 共生：与队友共享树果效果
        regAbility(new Ab() {
            public String id() { return "symbiosis"; }
        });

        // 组队：与队友交换能力等级
        regAbility(new Ab() {
            public String id() { return "entrainment"; }
        });

        // 气场类特性（暗黑气场/妖精气场/气场破坏）
        // 已在上方注册

        // 亲子爱：第二次攻击
        // 已在上方注册

        // 变色：变为受到攻击的属性
        regAbility(new Ab() {
            public String id() { return "color-change"; }
        });

        // 变形：变为对方的样子和属性
        regAbility(new Ab() {
            public String id() { return "imposter"; }
        });

        // 复制：复制对方的特性
        regAbility(new Ab() {
            public String id() { return "trace"; }
        });

        // 模仿：复制对方的特性
        regAbility(new Ab() {
            public String id() { return "mimicry"; }
        });

        // 预知梦：复制对方的特性
        regAbility(new Ab() {
            public String id() { return "forewarn"; }
        });

        // 危险预知：上场时检测对手危险招式
        // 已在上方注册

        // 奇迹皮肤：变化招式命中率×0.5
        // 已在上方注册

        // 纯朴：无视对手能力变化
        // 已在上方注册

        // 适应力：本系加成×2
        // 已在上方注册

        // 技术高手：威力≤60的招式×1.5
        // 已在上方注册

        // 铁拳：拳类招式×1.2
        // 已在上方注册

        // 硬爪：接触招式×1.3
        // 已在上方注册

        // 强壮之颚：咬类招式×1.5
        // 已在上方注册

        // 超级发射器：波导类招式×1.5
        // 已在上方注册

        // 锋利：切割类招式×1.5
        // 已在上方注册

        // 朋克摇滚：声音类招式×1.3
        // 已在上方注册

        // 钢铁之心：钢系招式×1.5
        // 已在上方注册

        // 晶体管：电系招式×1.3
        // 已在上方注册

        // 龙之颚：龙系招式×1.5
        // 已在上方注册

        // 沙之力：沙暴中岩/地/钢系×1.3
        // 已在上方注册

        // 有色透镜：效果不好时×2
        // 已在上方注册

        // 干劲：异常时物理×1.5
        // 已在上方注册

        // 巨大力量/单纯力量：物理攻击×2
        // 已在上方注册

        // 火焰之躯：灼伤时特殊×1.5
        // 已在上方注册

        // 剧毒之躯：中毒时物理×1.5
        // 已在上方注册

        // 全力攻击：有追加效果×1.3
        // 已在上方注册

        // 太阳之力：晴天时特攻×1.5
        // 已在上方注册

        // 分析：最后行动时×1.3
        // 已在上方注册

        // 霸主：每只阵亡队友+10%
        // 已在上方注册

        // 原始之力/夸克驱动：×1.3
        // 已在上方注册

        // 金属脉冲：晴天时×1.3
        // 已在上方注册

        // 强子引擎：电场时×1.3
        // 已在上方注册

        // 普通皮肤：一般系外招式×1.2
        // 已在上方注册

        // 茂盛/猛火/激流/虫之预感：HP<1/3时对应属性×1.5
        // 已在上方注册

        // 过滤/坚实岩石/棱镜装甲：克制伤害×0.75
        // 已在上方注册

        // 冰鳞：特殊伤害×0.5
        // 已在上方注册

        // 毛皮大衣：物理伤害×0.5
        // 已在上方注册

        // 洁净之盐：鬼系伤害×0.5，阻挡变化招式
        // 已在上方注册

        // 蓬松：接触伤害×0.5，火系伤害×2
        // 已在上方注册

        // 毁灭之剑/毁灭之盾/毁灭之瓶/毁灭之珠：对应攻击类型+33%
        // 已在上方注册

        // 吸取力量/引水/避雷针/蓄电/吸水/闪电引擎/吃土：对应属性免疫
        // 已在上方注册

        // 浮游：地系免疫
        // 已在上方注册

        // 乘风：风类招式免疫
        // 已在上方注册

        // 隔音：声音招式免疫
        // 已在上方注册

        // 防弹：弹道招式免疫
        // 已在上方注册

        // 黄金之躯：变化招式免疫
        // 已在上方注册

        // 吸盘：不会被强制替换
        // 已在上方注册

        // 镜面反射：反弹能力下降
        // 已在上方注册

        // 神奇鳞片：异常时物防×1.5
        // 已在上方注册

        // 轻快/叶绿素/沙隐/雪隐/冲浪者：对应场地速度×2
        // 已在上方注册

        // 无负重：消耗道具后速度×2
        // 已在上方注册

        // 飞毛腿：异常时速度×1.5
        // 已在上方注册

        // 重金属/轻金属：重量×2/÷2
        // 已在上方注册

        // 单纯：能力变化量×2
        // 已在上方注册

        // 性情乖僻：能力变化反转
        // 使用独立类注册

        // 天恩：追加效果概率×2
        // 已在上方注册

        // 鳞粉：阻挡追加效果
        // 已在上方注册

        // 恒净之躯/白烟/金属防护：阻挡能力下降
        // 已在上方注册

        // 柔软：麻痹免疫
        // 已在上方注册

        // 不眠/干劲：睡眠免疫
        // 已在上方注册

        // 我行我素：混乱免疫
        // 已在上方注册

        // 熔岩铠甲：冰冻免疫
        // 已在上方注册

        // 免疫：中毒免疫
        // 已在上方注册

        // 迟钝：精神类免疫
        // 已在上方注册

        // 芳香幕：精神类免疫
        // 已在上方注册

        // 树枕尾熊的Comatose：永远视为睡眠但可行动，免疫其他异常
        // 已在上方注册

        // 甜幕：队友睡眠免疫
        // 已在上方注册

        // 彩幕：队友中毒免疫
        // 已在上方注册

        // 叶子防守：晴天时免疫异常
        // 已在上方注册

        // 鲜花帷幕：草属性队友免疫异常和能力下降
        // 已在上方注册

        // 粗糙皮肤/铁刺/引爆/静电/火焰之躯/毒刺/孢子/迷人之躯/黏滑/卷发/幽香气息/灭亡之躯：接触反制
        // 已在上方注册

        // 隔音/防弹/乘风/黄金之躯/洁净之盐：招式阻挡
        // 已在上方注册

        // 黏滑/卷发：接触时降低速度
        // 已在上方注册

        // 灭亡之歌：接触后双方进入灭亡之歌状态
        // 已在上方注册

        // 草之誓约/水之誓约/火之誓约：结合场地产生特殊效果
        regAbility(new Ab() {
            public String id() { return "grassy-surge"; }
        });
        regAbility(new Ab() {
            public String id() { return "misty-surge"; }
        });
        regAbility(new Ab() {
            public String id() { return "electric-surge"; }
        });
        regAbility(new Ab() {
            public String id() { return "psychic-surge"; }
        });

        // 降雪/扬沙/日照/降雨：入场时改变天气
        regAbility(new Ab() {
            public String id() { return "snow-warning"; }
        });
        regAbility(new Ab() {
            public String id() { return "sand-stream"; }
        });
        regAbility(new Ab() {
            public String id() { return "drought"; }
        });
        regAbility(new Ab() {
            public String id() { return "drizzle"; }
        });

        // 超极巨化相关特性
        regAbility(new Ab() {
            public String id() { return "g-max-strike"; }
        });

        // 太晶化相关特性（太晶爆发）
        regAbility(new Ab() {
            public String id() { return "tera-burst"; }
        });

        // ========== P0/P1 缺失特性补齐 ==========

        // 危险预知：上场时检测对手是否拥有克制/秒杀/自爆招式
        regAbility(new Ab() {
            public String id() { return "anticipation"; }
        });

        // 不仁不义：攻击中毒目标必中要害
        regAbility(new Ab() {
            public String id() { return "merciless"; }
        });

        // 奇迹皮肤：变化招式命中率 ×0.5
        regAbility(new Ab() {
            public String id() { return "wonder-skin"; }
        });

        // 飞出的内在物：被击倒时造成等量伤害
        regAbility(new Ab() {
            public String id() { return "innards-out"; }
        });

        // 精神力：不会畏缩
        regAbility(new Ab() {
            public String id() { return "inner-focus"; }
        });

        // 适应力：本系加成从 1.5x → 2.0x
        regAbility(new Ab() {
            public String id() { return "adaptability"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                // STAB 修正由引擎在 calcSTAB 阶段处理，此处标记由引擎读取
                return mod;
            }
        });

        // 硬爪：接触招式 ×1.3
        regAbility(new Ab() {
            public String id() { return "tough-claws"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return hasMoveFlag(ctx.move, "contact") ? mod * 1.3 : mod;
            }
        });

        // 狙击手：会心一击倍率 2.25x（由引擎 damage 公式读取）
        regAbility(new Ab() {
            public String id() { return "sniper"; }
        });

        // 下载：登场时根据对手防御/特防提升攻击或特攻（引擎 switch-in 实现）
        regAbility(new Ab() {
            public String id() { return "download"; }
        });

        // 加速：每回合结束时速度 +1（引擎 turn-cleanup 实现）
        regAbility(new Ab() {
            public String id() { return "speed-boost"; }
        });

        // 纯朴：无视对手能力变化（引擎 damage 公式实现）
        regAbility(new Ab() {
            public String id() { return "unaware"; }
        });

        // 先进医术：回复招式先制度 +3
        regAbility(new Ab() {
            public String id() { return "triage"; }
        });

        // 腐蚀：毒系招式可中毒钢系
        regAbility(new Ab() {
            public String id() { return "corrosion"; }
        });

        // 鲜艳之躯/女王威严/尾甲：阻挡对手先制招式
        regAbility(new Ab() {
            public String id() { return "dazzling"; }
        });
        regAbility(new Ab() {
            public String id() { return "queenly-majesty"; }
        });
        regAbility(new Ab() {
            public String id() { return "armor-tail"; }
        });

        // 亲子爱：第二次攻击 0.25x
        regAbility(new Ab() {
            public String id() { return "parental-bond"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return Boolean.TRUE.equals(ctx.attacker.get("parentalBondSecondHit")) ? mod * 0.25 : mod;
            }
        });

        // 暗黑气场/妖精气场/气场破坏
        regAbility(new Ab() {
            public String id() { return "dark-aura"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(DARK) ? mod * 4.0 / 3.0 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "fairy-aura"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FAIRY) ? mod * 4.0 / 3.0 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "aura-break"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                // 由引擎结合 dark-aura/fairy-aura 计算，此处标记
                return mod;
            }
        });

        // 异兽提升：击倒对手后提升最高能力（引擎 faint 处理实现）
        regAbility(new Ab() {
            public String id() { return "beast-boost"; }
        });

        // 镜甲：反弹能力下降
        regAbility(new Ab() {
            public String id() { return "mirror-armor"; }
        });

        // 好胜/不服输：能力下降时攻击/特攻 +2
        regAbility(new Ab() {
            public String id() { return "competitive"; }
        });
        regAbility(new Ab() {
            public String id() { return "defiant"; }
        });

        // 神奇守护：只有效果绝佳的招式才能命中
        regAbility(new Ab() {
            public String id() { return "wonder-guard"; }
        });

        // ========== 补齐剩余缺失特性 ==========

        // 慢速启动：前5回合速度减半
        regAbility(new Ab() {
            public String id() { return "slow-start"; }
        });

        // 黏着：不会被偷走道具
        regAbility(new Ab() {
            public String id() { return "sticky-hold"; }
        });

        // 顺手牵羊：被接触时偷走对手道具
        regAbility(new Ab() {
            public String id() { return "pickpocket"; }
        });

        // 魔术师：攻击时偷走对手道具
        regAbility(new Ab() {
            public String id() { return "magician"; }
        });

        // 颊囊：食用树果时额外回复HP
        regAbility(new Ab() {
            public String id() { return "cheek-pouch"; }
        });

        // 熟成：树果效果翻倍
        regAbility(new Ab() {
            public String id() { return "ripen"; }
        });

        // 化学变化气体：场上特性失效
        regAbility(new Ab() {
            public String id() { return "neutralizing-gas"; }
        });

        // 螺旋尾羽/坚毅：无视避雷针/引水等改变目标的特性
        regAbility(new Ab() {
            public String id() { return "stalwart"; }
        });

        // 游离之物：被接触时交换特性
        regAbility(new Ab() {
            public String id() { return "wandering-spirit"; }
        });

        // 捡球：无道具时捡回第一回合使用的精灵球
        regAbility(new Ab() {
            public String id() { return "ball-fetch"; }
        });

        // 笨拙：道具无效
        regAbility(new Ab() {
            public String id() { return "klutz"; }
        });

        // 紧张感：对手无法使用树果
        regAbility(new Ab() {
            public String id() { return "unnerve"; }
        });

        // 压迫感：对手招式消耗2PP
        regAbility(new Ab() {
            public String id() { return "pressure"; }
        });

        // 厚脂肪：火/冰系伤害减半
        regAbility(new Ab() {
            public String id() { return "thick-fat"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE, ICE) ? mod * 0.5 : mod;
            }
        });

        // 耐热：火系伤害减半
        regAbility(new Ab() {
            public String id() { return "heatproof"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? mod * 0.5 : mod;
            }
        });

        // 干燥皮肤：水系免疫，火系伤害×1.25，雨天回复HP
        regAbility(new Ab() {
            public String id() { return "dry-skin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                if (ctx.moveTypeIs(WATER)) return 0;
                return ctx.moveTypeIs(FIRE) ? mod * 1.25 : mod;
            }
        });

        // 避雷针：电系免疫，提升特攻
        regAbility(new Ab() {
            public String id() { return "lightning-rod"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });

        // 蓄电：电系免疫，提升特攻
        regAbility(new Ab() {
            public String id() { return "volt-absorb"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });

        // 引水：水系免疫，提升特攻
        regAbility(new Ab() {
            public String id() { return "storm-drain"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(WATER) ? 0 : mod;
            }
        });

        // 吸水：水系免疫，回复HP
        regAbility(new Ab() {
            public String id() { return "water-absorb"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(WATER) ? 0 : mod;
            }
        });

        // 闪电引擎：电系免疫，提升速度
        regAbility(new Ab() {
            public String id() { return "motor-drive"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? 0 : mod;
            }
        });

        // 火焰之躯：接触时30%灼伤
        regAbility(new Ab() {
            public String id() { return "flame-body"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "flame-body");
                ctx.result.put("chance", 30);
            }
        });

        // 毒刺：接触时30%中毒
        regAbility(new Ab() {
            public String id() { return "poison-point"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "poison-point");
                ctx.result.put("chance", 30);
            }
        });

        // 静电：接触时30%麻痹
        regAbility(new Ab() {
            public String id() { return "static"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "static");
                ctx.result.put("chance", 30);
            }
        });

        // 孢子：接触时各10%麻痹/灼伤/睡眠
        regAbility(new Ab() {
            public String id() { return "effect-spore"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "effect-spore");
                ctx.result.put("chance", 30);
            }
        });

        // 迷人之躯：接触时30%着迷
        regAbility(new Ab() {
            public String id() { return "cute-charm"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "cute-charm");
                ctx.result.put("chance", 30);
            }
        });

        // 粗糙皮肤：接触时反伤1/8
        regAbility(new Ab() {
            public String id() { return "rough-skin"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "rough-skin");
            }
        });

        // 铁刺：接触时反伤1/8
        regAbility(new Ab() {
            public String id() { return "iron-barbs"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "iron-barbs");
            }
        });

        // 引爆：被击倒时造成伤害
        regAbility(new Ab() {
            public String id() { return "aftermath"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "aftermath");
            }
        });

        // 黏滑：接触时降低速度
        regAbility(new Ab() {
            public String id() { return "gooey"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "gooey");
            }
        });

        // 卷发：接触时降低速度
        regAbility(new Ab() {
            public String id() { return "tangling-hair"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "tangling-hair");
            }
        });

        // 幽香气息：接触后对方特性变为幽香气息
        regAbility(new Ab() {
            public String id() { return "lingering-aroma"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "lingering-aroma");
            }
        });

        // 灭亡之躯：接触后双方进入灭亡之歌状态
        regAbility(new Ab() {
            public String id() { return "perish-body"; }
            public void onContact(ContactContext ctx) {
                ctx.result.put("effect", "perish-body");
            }
        });

        // 隔音：阻挡声音招式
        regAbility(new Ab() {
            public String id() { return "soundproof"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "sound");
            }
        });

        // 防弹：阻挡弹道招式
        regAbility(new Ab() {
            public String id() { return "bulletproof"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "bullet");
            }
        });

        // 乘风：阻挡风类招式
        regAbility(new Ab() {
            public String id() { return "wind-rider"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return hasMoveFlag(ctx.move, "wind");
            }
        });

        // 黄金之躯：完全阻挡变化招式
        regAbility(new Ab() {
            public String id() { return "good-as-gold"; }
            public boolean onTypeImmunity(ImmunityContext ctx) {
                return ctx.moveTypeId == 0;
            }
        });

        // 洁净之盐：鬼系伤害减半，阻挡变化招式
        regAbility(new Ab() {
            public String id() { return "purifying-salt"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GHOST) ? mod * 0.5 : mod;
            }
        });

        // 蓬松：接触伤害减半，火系伤害翻倍
        regAbility(new Ab() {
            public String id() { return "fluffy"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                if (ctx.moveTypeIs(FIRE)) return mod * 2;
                return hasMoveFlag(ctx.move, "contact") ? mod * 0.5 : mod;
            }
        });

        // 毁灭之剑：物理伤害+33%
        regAbility(new Ab() {
            public String id() { return "sword-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod / 0.75 : mod;
            }
        });

        // 毁灭之盾：物理伤害+33%
        regAbility(new Ab() {
            public String id() { return "tablets-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod / 0.75 : mod;
            }
        });

        // 毁灭之瓶：特殊伤害+33%
        regAbility(new Ab() {
            public String id() { return "vessel-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod / 0.75 : mod;
            }
        });

        // 毁灭之珠：特殊伤害+33%
        regAbility(new Ab() {
            public String id() { return "beads-of-ruin"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod / 0.75 : mod;
            }
        });

        // 吸取力量：草系免疫，提升攻击
        regAbility(new Ab() {
            public String id() { return "sap-sipper"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GRASS) ? 0 : mod;
            }
        });

        // 吃土：地系免疫
        regAbility(new Ab() {
            public String id() { return "earth-eater"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GROUND) ? 0 : mod;
            }
        });

        // 浮游：地系免疫
        regAbility(new Ab() {
            public String id() { return "levitate"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GROUND) ? 0 : mod;
            }
        });

        // 火焰之躯：火系免疫
        regAbility(new Ab() {
            public String id() { return "flash-fire"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? 0 : mod;
            }
        });

        // 耐热：火系免疫
        regAbility(new Ab() {
            public String id() { return "well-baked-body"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) ? 0 : mod;
            }
        });

        // 神奇鳞片：异常时物防×1.5
        regAbility(new Ab() {
            public String id() { return "marvel-scale"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && hasStatus(ctx.defenderCondition())
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
        });

        // 轻快：雨天速度×2
        regAbility(new Ab() {
            public String id() { return "swift-swim"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "rainTurns") ? speed * 2 : speed;
            }
        });

        // 叶绿素：晴天速度×2
        regAbility(new Ab() {
            public String id() { return "chlorophyll"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "sunTurns") ? speed * 2 : speed;
            }
        });

        // 沙隐：沙暴速度×2
        regAbility(new Ab() {
            public String id() { return "sand-rush"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "sandTurns") ? speed * 2 : speed;
            }
        });

        // 雪隐：雪天速度×2
        regAbility(new Ab() {
            public String id() { return "slush-rush"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "snowTurns") ? speed * 2 : speed;
            }
        });

        // 冲浪者：电场速度×2
        regAbility(new Ab() {
            public String id() { return "surge-surfer"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "electricTerrainTurns") ? speed * 2 : speed;
            }
        });

        // 无负重：消耗道具后速度×2
        regAbility(new Ab() {
            public String id() { return "unburden"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return Boolean.TRUE.equals(ctx.mon.get("unburdenActive")) ? speed * 2 : speed;
            }
        });

        // 飞毛腿：异常时速度×1.5
        regAbility(new Ab() {
            public String id() { return "quick-feet"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return hasStatus(strVal(ctx.mon, "condition")) ? (int) Math.floor(speed * 1.5) : speed;
            }
        });

        // 重金属：重量×2
        regAbility(new Ab() {
            public String id() { return "heavy-metal"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return weight * 2; }
        });

        // 轻金属：重量÷2
        regAbility(new Ab() {
            public String id() { return "light-metal"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return Math.max(1, weight / 2); }
        });

        // 单纯：能力变化量×2
        regAbility(new Ab() {
            public String id() { return "simple"; }
            public int onModifyStatStage(StatStageContext ctx, int delta) { return delta * 2; }
        });

        // 天恩：追加效果概率×2
        regAbility(new Ab() {
            public String id() { return "serene-grace"; }
            public int onModifySecondaryEffectChance(int chance) { return Math.min(100, chance * 2); }
        });

        // 鳞粉：阻挡追加效果
        regAbility(new Ab() {
            public String id() { return "shield-dust"; }
            public boolean onBlocksSecondaryEffects() { return true; }
        });

        // 恒净之躯：阻挡能力下降
        regAbility(new Ab() {
            public String id() { return "clear-body"; }
            public boolean onBlocksStatDrop() { return true; }
        });

        // 白烟：阻挡能力下降
        regAbility(new Ab() {
            public String id() { return "white-smoke"; }
            public boolean onBlocksStatDrop() { return true; }
        });

        // 金属防护：阻挡能力下降
        regAbility(new Ab() {
            public String id() { return "full-metal-body"; }
            public boolean onBlocksStatDrop() { return true; }
        });

        // 柔软：麻痹免疫
        regAbility(new Ab() {
            public String id() { return "limber"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "paralysis".equals(ctx.condition); }
        });

        // 水幕：灼伤免疫
        regAbility(new Ab() {
            public String id() { return "water-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "burn".equals(ctx.condition); }
        });

        // 水泡：灼伤免疫
        regAbility(new Ab() {
            public String id() { return "water-bubble"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "burn".equals(ctx.condition); }
        });

        // 不眠：睡眠免疫
        regAbility(new Ab() {
            public String id() { return "insomnia"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "sleep".equals(ctx.condition); }
        });

        // 干劲：睡眠免疫
        regAbility(new Ab() {
            public String id() { return "vital-spirit"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "sleep".equals(ctx.condition); }
        });

        // 我行我素：混乱免疫
        regAbility(new Ab() {
            public String id() { return "own-tempo"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "confusion".equals(ctx.condition); }
        });

        // 熔岩铠甲：冰冻免疫
        regAbility(new Ab() {
            public String id() { return "magma-armor"; }
            public boolean onStatusImmunity(StatusContext ctx) { return "freeze".equals(ctx.condition); }
        });

        // 免疫：中毒免疫
        regAbility(new Ab() {
            public String id() { return "immunity"; }
            public boolean onStatusImmunity(StatusContext ctx) { 
                return "poison".equals(ctx.condition) || "toxic".equals(ctx.condition); 
            }
        });

        // 迟钝：精神类免疫
        regAbility(new Ab() {
            public String id() { return "oblivious"; }
            public boolean onMentalImmunity(StatusContext ctx) {
                return switch (ctx.condition) {
                    case "taunt", "attract", "intimidate" -> true;
                    default -> false;
                };
            }
        });

        // 芳香幕：精神类免疫
        regAbility(new Ab() {
            public String id() { return "aroma-veil"; }
            public boolean onMentalImmunity(StatusContext ctx) {
                return switch (ctx.condition) {
                    case "taunt", "attract", "disable", "encore", "heal-block", "torment" -> true;
                    default -> false;
                };
            }
        });

        // 树枕尾熊的Comatose：永远视为睡眠但可行动，免疫其他异常
        regAbility(new Ab() {
            public String id() { return "comatose"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return !"sleep".equals(ctx.condition);
            }
        });

        // 甜幕：队友睡眠免疫
        regAbility(new Ab() {
            public String id() { return "sweet-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return "sleep".equals(ctx.condition) || "yawn".equals(ctx.condition);
            }
        });

        // 彩幕：队友中毒免疫
        regAbility(new Ab() {
            public String id() { return "pastel-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return "poison".equals(ctx.condition) || "toxic".equals(ctx.condition);
            }
        });

        // 叶子防守：晴天时免疫异常
        regAbility(new Ab() {
            public String id() { return "leaf-guard"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return fieldActive(ctx.state, "sunTurns");
            }
        });

        // 鲜花帷幕：草属性队友免疫异常和能力下降
        regAbility(new Ab() {
            public String id() { return "flower-veil"; }
            public boolean onStatusImmunity(StatusContext ctx) {
                return true;
            }
        });

        // 技术高手：威力≤60的招式×1.5
        regAbility(new Ab() {
            public String id() { return "technician"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int p = ctx.movePower();
                return p > 0 && p <= 60 ? mod * 1.5 : mod;
            }
        });

        // 铁拳：拳类招式×1.2
        regAbility(new Ab() {
            public String id() { return "iron-fist"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("punch", ctx.move) ? mod * 1.2 : mod;
            }
        });

        // 鲁莽：反伤招式×1.2
        regAbility(new Ab() {
            public String id() { return "reckless"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isRecoilMove(ctx.move) ? mod * 1.2 : mod;
            }
        });

        // 干劲：物理攻击×1.5
        regAbility(new Ab() {
            public String id() { return "hustle"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });

        // 长臂猿战术：物理攻击×1.5
        regAbility(new Ab() {
            public String id() { return "gorilla-tactics"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });

        // 强壮之颚：咬类招式×1.5
        regAbility(new Ab() {
            public String id() { return "strong-jaw"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("bite", ctx.move) || moveCategory("fang", ctx.move) ? mod * 1.5 : mod;
            }
        });

        // 超级发射器：波导类招式×1.5
        regAbility(new Ab() {
            public String id() { return "mega-launcher"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                String n = moveName(ctx.move);
                return n.contains("pulse") || n.contains("aura-sphere") || n.contains("aura sphere") ? mod * 1.5 : mod;
            }
        });

        // 锋利：切割类招式×1.5
        regAbility(new Ab() {
            public String id() { return "sharpness"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSlicingMove(ctx.move) ? mod * 1.5 : mod;
            }
        });

        // 朋克摇滚：声音类招式×1.3
        regAbility(new Ab() {
            public String id() { return "punk-rock"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSoundMove(ctx.move) ? mod * 1.3 : mod;
            }
        });

        // 钢铁之心：钢系招式×1.5
        regAbility(new Ab() {
            public String id() { return "steely-spirit"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(STEEL) ? mod * 1.5 : mod;
            }
        });

        // 晶体管：电系招式×1.3
        regAbility(new Ab() {
            public String id() { return "transistor"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(ELECTRIC) ? mod * 1.3 : mod;
            }
        });

        // 龙之颚：龙系招式×1.5
        regAbility(new Ab() {
            public String id() { return "dragons-maw"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(DRAGON) ? mod * 1.5 : mod;
            }
        });

        // 沙之力：沙暴中岩/地/钢系×1.3
        regAbility(new Ab() {
            public String id() { return "sand-force"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "sandTurns") && ctx.moveTypeIs(ROCK, GROUND, STEEL) ? mod * 1.3 : mod;
            }
        });

        // 有色透镜：效果不好时×2
        regAbility(new Ab() {
            public String id() { return "tinted-lens"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                if (mod > 0 && mod < 1) return mod * 2;
                return mod;
            }
        });

        // 干劲：异常时物理×1.5
        regAbility(new Ab() {
            public String id() { return "guts"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return hasStatus(ctx.attackerCondition()) && ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });

        // 巨大力量：物理攻击×2
        regAbility(new Ab() {
            public String id() { return "huge-power"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? stat * 2 : stat;
            }
        });

        // 单纯力量：物理攻击×2
        regAbility(new Ab() {
            public String id() { return "pure-power"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? stat * 2 : stat;
            }
        });

        // 火焰之躯：灼伤时特殊×1.5
        regAbility(new Ab() {
            public String id() { return "flare-boost"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return "burn".equals(ctx.attackerCondition()) && ctx.damageClassId == SPECIAL ? mod * 1.5 : mod;
            }
        });

        // 剧毒之躯：中毒时物理×1.5
        regAbility(new Ab() {
            public String id() { return "toxic-boost"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                String c = ctx.attackerCondition();
                return ("poison".equals(c) || "toxic".equals(c)) && ctx.damageClassId == PHYSICAL ? mod * 1.5 : mod;
            }
        });

        // 全力攻击：有追加效果×1.3
        regAbility(new Ab() {
            public String id() { return "sheer-force"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return hasEffectChance(ctx.move) ? mod * 1.3 : mod;
            }
        });

        // 太阳之力：晴天时特攻×1.5
        regAbility(new Ab() {
            public String id() { return "solar-power"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL && fieldActive(ctx.state, "sunTurns") ? mod * 1.5 : mod;
            }
        });

        // 分析：最后行动时×1.3
        regAbility(new Ab() {
            public String id() { return "analytic"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return Boolean.TRUE.equals(ctx.attacker.get("analyticActive")) ? mod * 1.3 : mod;
            }
        });

        // 霸主：每只阵亡队友+10%
        regAbility(new Ab() {
            public String id() { return "supreme-overlord"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int fainted = intVal(ctx.attacker, "faintedAllies");
                return fainted > 0 ? mod * (1.0 + fainted * 0.1) : mod;
            }
        });

        // 原始之力：晴天时×1.3
        regAbility(new Ab() {
            public String id() { return "protosynthesis"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isBoosterActive(ctx, "protosynthesis", "sunTurns") ? mod * 1.3 : mod;
            }
        });

        // 夸克驱动：电场时×1.3
        regAbility(new Ab() {
            public String id() { return "quark-drive"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isBoosterActive(ctx, "quark-drive", "electricTerrainTurns") ? mod * 1.3 : mod;
            }
        });

        // 金属脉冲：晴天时×1.3
        regAbility(new Ab() {
            public String id() { return "orichalcum-pulse"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "sunTurns") ? mod * 1.3 : mod;
            }
        });

        // 强子引擎：电场时×1.3
        regAbility(new Ab() {
            public String id() { return "hadron-engine"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return fieldActive(ctx.state, "electricTerrainTurns") ? mod * 1.3 : mod;
            }
        });

        // 普通皮肤：一般系外招式×1.2
        regAbility(new Ab() {
            public String id() { return "normalize"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeId != NORMAL ? mod * 1.2 : mod;
            }
        });

        // 茂盛：HP<1/3时草系×1.5
        regAbility(new Ab() {
            public String id() { return "overgrow"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(GRASS) && ctx.attackerHp() <= ctx.attackerMaxHp() / 3 ? mod * 1.5 : mod;
            }
        });

        // 猛火：HP<1/3时火系×1.5
        regAbility(new Ab() {
            public String id() { return "blaze"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(FIRE) && ctx.attackerHp() <= ctx.attackerMaxHp() / 3 ? mod * 1.5 : mod;
            }
        });

        // 激流：HP<1/3时水系×1.5
        regAbility(new Ab() {
            public String id() { return "torrent"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(WATER) && ctx.attackerHp() <= ctx.attackerMaxHp() / 3 ? mod * 1.5 : mod;
            }
        });

        // 虫之预感：HP<1/3时虫系×1.5
        regAbility(new Ab() {
            public String id() { return "swarm"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(BUG) && ctx.attackerHp() <= ctx.attackerMaxHp() / 3 ? mod * 1.5 : mod;
            }
        });

        // 过滤：克制伤害×0.75
        regAbility(new Ab() {
            public String id() { return "filter"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return mod * 0.75;
            }
        });

        // 坚实岩石：克制伤害×0.75
        regAbility(new Ab() {
            public String id() { return "solid-rock"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return mod * 0.75;
            }
        });

        // 棱镜装甲：克制伤害×0.75
        regAbility(new Ab() {
            public String id() { return "prism-armor"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return mod * 0.75;
            }
        });

        // 冰鳞：特殊伤害×0.5
        regAbility(new Ab() {
            public String id() { return "ice-scales"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod * 0.5 : mod;
            }
        });

        // 毛皮大衣：物理伤害×0.5
        regAbility(new Ab() {
            public String id() { return "fur-coat"; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 0.5 : mod;
            }
        });

        // 道具 handler 委托到专用注册器
        ItemHandlers.registerAll(ITEMS);
    }

    // ========================================================================
    //  辅助注册方法
    // ========================================================================

    /** 避免匿名类中重复写 AbilityHandler 全方法 */
    /** 能力 handler 基类——包可见，允许外部 handler 文件继承 */
    abstract static class Ab implements AbilityHandler {}
    /** 道具 handler 基类——包可见 */
    abstract static class It implements ItemHandler {}

    private static void regAbility(AbilityHandler h) { register(h); }
    private static void regItem(ItemHandler h) { register(h); }

    /** 属性增伤道具 */
    /** Overgrow / Blaze / Torrent / Swarm 模式：HP < 1/3 时对应属性增伤 1.5x */
    private static void regOvergrowLike(String id, int typeId) {
        regAbility(new Ab() {
            public String id() { return id; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.moveTypeIs(typeId) && ctx.attackerHp() <= ctx.attackerMaxHp() / 3 ? mod * 1.5 : mod;
            }
        });
    }

    /** Filter / Solid Rock / Prism Armor 模式 */
    private static void regFilterLike(String id) {
        regAbility(new Ab() {
            public String id() { return id; }
            public double onTargetModifyDamage(AttackContext ctx, double mod) {
                // 由引擎计算克制关系，handler 只做乘法（实际克制判断由 calcTypeModifier 做）
                // 这里简化：mod > 原始值表示克制
                return mod * 0.75;
            }
        });
    }

    /** Protosynthesis / Quark Drive */
    private static void regProtoQuark() {
        regAbility(new Ab() {
            public String id() { return "protosynthesis"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isBoosterActive(ctx, "protosynthesis", "sunTurns") ? mod * 1.3 : mod;
            }
        });
        regAbility(new Ab() {
            public String id() { return "quark-drive"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isBoosterActive(ctx, "quark-drive", "electricTerrainTurns") ? mod * 1.3 : mod;
            }
        });
    }

    // ========================================================================
    //  工具方法
    // ========================================================================

    static boolean fieldActive(Map<String, Object> state, String key) {
        if (state == null) return false;
        Object fe = state.get("fieldEffects");
        if (fe instanceof Map) {
            Object v = ((Map<?, ?>) fe).get(key);
            return v instanceof Number n && n.intValue() > 0;
        }
        return false;
    }

    private static boolean isBoosterActive(AttackContext ctx, String ability, String fieldKey) {
        // Booster Energy 触发（首次激活即消耗）
        if ("booster-energy".equals(heldItem(ctx.attacker))) {
            if (!Boolean.TRUE.equals(ctx.attacker.get("boosterEnergyUsed"))) {
                ctx.attacker.put("boosterEnergyUsed", true);
                ctx.attacker.put("heldItem", "");
                ctx.attacker.put("itemConsumed", true);
            }
            return true;
        }
        // 场地激活
        return fieldActive(ctx.state, fieldKey);
    }
}
