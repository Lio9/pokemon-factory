package com.lio9.battle.effect;

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

    // ====== 类型常量 ======
    public static final int NORMAL = 1;
    public static final int FIRE = 2;
    public static final int WATER = 3;
    public static final int ELECTRIC = 4;
    public static final int GRASS = 5;
    public static final int ICE = 6;
    public static final int FIGHTING = 7;
    public static final int POISON = 8;
    public static final int GROUND = 9;
    public static final int FLYING = 10;
    public static final int PSYCHIC = 11;
    public static final int BUG = 12;
    public static final int ROCK = 13;
    public static final int GHOST = 14;
    public static final int DRAGON = 15;
    public static final int DARK = 16;
    public static final int STEEL = 17;
    public static final int FAIRY = 18;

    public static final int PHYSICAL = 1;
    public static final int SPECIAL = 2;

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

    /** 攻击方道具：修正伤害倍率 */
    public static double dispatchItemSourceDamage(Map<String, Object> attacker, AttackContext ctx, double mod) {
        ItemHandler h = getItem(heldItem(attacker));
        return h != null ? h.onSourceModifyDamage(ctx, mod) : mod;
    }

    /** 攻击方特性：修正攻击属性值 */
    public static int dispatchSourceAttack(Map<String, Object> attacker, AttackContext ctx, int stat) {
        AbilityHandler h = getAbility(abilityName(attacker));
        return h != null ? h.onSourceModifyAttackStat(ctx, stat) : stat;
    }

    /** 攻击方道具：修正攻击属性值（含 Choice Band/Specs） */
    public static int dispatchItemSourceAttack(Map<String, Object> attacker, AttackContext ctx, int stat) {
        ItemHandler h = getItem(heldItem(attacker));
        return h != null ? h.onSourceModifyAttackStat(ctx, stat) : stat;
    }

    /** 防御方特性：修正防御属性值 */
    public static int dispatchTargetDefense(Map<String, Object> defender, AttackContext ctx, int stat) {
        AbilityHandler h = getAbility(abilityName(defender));
        return h != null ? h.onTargetModifyDefenseStat(ctx, stat) : stat;
    }

    /** 防御方道具：修正防御属性值 */
    public static int dispatchItemTargetDefense(Map<String, Object> defender, AttackContext ctx, int stat) {
        ItemHandler h = getItem(heldItem(defender));
        return h != null ? h.onTargetModifyDefenseStat(ctx, stat) : stat;
    }

    /** 特性：修正速度值 */
    public static int dispatchSpeed(Map<String, Object> mon, SpeedContext ctx, int speed) {
        AbilityHandler h = getAbility(abilityName(mon));
        return h != null ? h.onModifySpeed(ctx, speed) : speed;
    }

    /** 道具：修正速度值 */
    public static int dispatchItemSpeed(Map<String, Object> mon, SpeedContext ctx, int speed) {
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

    /** 道具：修正重量 */
    public static int dispatchItemWeight(Map<String, Object> mon, WeightContext ctx, int weight) {
        ItemHandler h = getItem(heldItem(mon));
        return h != null ? h.onModifyWeight(ctx, weight) : weight;
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

        // === 伤害倍率道具 ===
        regItem(new It() {
            public String id() { return "life-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) { return mod * 1.3; }
        });
        // Metronome: 连续用同一招式每次 +0.2x
        regItem(new It() {
            public String id() { return "metronome"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int count = intVal(ctx.attacker, "metronomeCount");
                return count > 0 ? mod * (1.0 + count * 0.2) : mod;
            }
        });
        // Expert Belt: 克制时 x1.2
        regItem(new It() {
            public String id() { return "expert-belt"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                // 由引擎计算克制关系后传入 mod 已含克制倍率，无法在此直接判断
                // 保留给引擎处理
                return mod;
            }
        });
        // Muscle Band: 物理 x1.1
        regItem(new It() {
            public String id() { return "muscle-band"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 1.1 : mod;
            }
        });
        // Wise Glasses: 特殊 x1.1
        regItem(new It() {
            public String id() { return "wise-glasses"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod * 1.1 : mod;
            }
        });

        // === 属性增伤道具（类型 → 倍率映射） ===
        registerTypeBoostItems();
        // 属性石板（Arceus）、记忆卡带（Silvally）、属性宝石
        registerPlatesMemoriesGems();

        // === Species 限定道具 ===
        // Light Ball (Pikachu): 双攻 x2
        regItem(new It() {
            public String id() { return "light-ball"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return isSpecies(ctx.attacker, "pikachu") ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        // Thick Club (Cubone/Marowak): 物攻 x2
        regItem(new It() {
            public String id() { return "thick-club"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && isSpecies(ctx.attacker, "cubone", "marowak")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        // Deep Sea Tooth (Clamperl): 特攻 x2
        regItem(new It() {
            public String id() { return "deep-sea-tooth"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "clamperl")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        // Deep Sea Scale (Clamperl): 特防 x2
        regItem(new It() {
            public String id() { return "deep-sea-scale"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "clamperl")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        // Soul Dew (Latias/Latios): 特攻特防 x1.5
        regItem(new It() {
            public String id() { return "soul-dew"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "latias", "latios")
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "latias", "latios")
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        // Metal Powder (Ditto): 物防 x2
        regItem(new It() {
            public String id() { return "metal-powder"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && isSpecies(ctx.defender, "ditto")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        // Quick Powder (Ditto): 速度 x2
        regItem(new It() {
            public String id() { return "quick-powder"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return isSpecies(ctx.mon, "ditto") ? speed * 2 : speed;
            }
        });
        // Adamant/Lustrous/Griseous Orb: 1.2x
        regSpeciesOrb();

        // === 攻击/防御/速度修正道具 ===
        regItem(new It() {
            public String id() { return "choice-band"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(new It() {
            public String id() { return "choice-specs"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(new It() {
            public String id() { return "choice-scarf"; }
            public int onModifySpeed(SpeedContext ctx, int speed) { return (int) Math.floor(speed * 1.5); }
        });
        regItem(new It() {
            public String id() { return "assault-vest"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(new It() {
            public String id() { return "eviolite"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return Boolean.TRUE.equals(ctx.defender.get("notFullyEvolved"))
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(new It() {
            public String id() { return "iron-ball"; }
            public int onModifySpeed(SpeedContext ctx, int speed) { return Math.max(1, speed / 2); }
        });
        // Room Service: 空间下速度减半
        regItem(new It() {
            public String id() { return "room-service"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return fieldActive(ctx.state, "trickRoomTurns") ? Math.max(1, speed / 2) : speed;
            }
        });
        // Float Stone: 重量减半（用于重磅冲撞等）
        regItem(new It() {
            public String id() { return "float-stone"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return Math.max(1, weight / 2); }
        });

        // Punching Glove: 拳类招式 x1.1
        regItem(new It() {
            public String id() { return "punching-glove"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("punch", ctx.move) ? mod * 1.1 : mod;
            }
        });

        // ====================================================================
        //  受伤后触发能力
        // ====================================================================

        // 碎裂铠甲：受到物理招式 → 防御 -1，速度 +2
        regAbility(new Ab() {
            public String id() { return "weak-armor"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.damageClassId == PHYSICAL) {
                    ctx.result.put("weakArmor", true);
                }
            }
        });
        // 持久力：受到招式 → 防御 +1
        regAbility(new Ab() {
            public String id() { return "stamina"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                ctx.result.put("stamina", true);
            }
        });
        // 正义之心：受到恶系招式 → 攻击 +1
        regAbility(new Ab() {
            public String id() { return "justified"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == DARK) {
                    ctx.result.put("justified", true);
                }
            }
        });
        // 胆怯：受到虫/鬼/恶 → 速度 +1
        regAbility(new Ab() {
            public String id() { return "rattled"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == BUG || ctx.moveTypeId == GHOST || ctx.moveTypeId == DARK) {
                    ctx.result.put("rattled", true);
                }
            }
        });
        // 蒸汽机：受到火/水 → 速度 +6
        regAbility(new Ab() {
            public String id() { return "steam-engine"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == FIRE || ctx.moveTypeId == WATER) {
                    ctx.result.put("steamEngine", true);
                }
            }
        });
        // 怒火中烧：HP 从 >50% 降至 ≤50% → 特攻 +1
        regAbility(new Ab() {
            public String id() { return "berserk"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.hpBeforeDamage * 2 > ctx.maxHp && ctx.hpAfterDamage * 2 <= ctx.maxHp) {
                    ctx.result.put("berserk", true);
                }
            }
        });
        // 怒壳：HP 从 >50% 降至 ≤50% → 攻/特攻/速 +1，防 -1
        regAbility(new Ab() {
            public String id() { return "anger-shell"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.hpBeforeDamage * 2 > ctx.maxHp && ctx.hpAfterDamage * 2 <= ctx.maxHp) {
                    ctx.result.put("angerShell", true);
                }
            }
        });
        // 毒满地：受到物理招式 → 设置毒菱
        regAbility(new Ab() {
            public String id() { return "toxic-debris"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.damageClassId == PHYSICAL && ctx.state != null) {
                    ctx.result.put("toxicDebris", true);
                }
            }
        });
        // 播撒：受到招式 → 青草场地
        regAbility(new Ab() {
            public String id() { return "seed-sower"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.state != null) {
                    ctx.result.put("seedSower", true);
                }
            }
        });
        // 吐沙：受到招式 → 沙暴
        regAbility(new Ab() {
            public String id() { return "sand-spit"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.state != null) {
                    ctx.result.put("sandSpit", true);
                }
            }
        });
        // 电力转换：受到电系招式 → 充电状态
        regAbility(new Ab() {
            public String id() { return "electromorphosis"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == ELECTRIC) {
                    ctx.result.put("electromorphosis", true);
                }
            }
        });
        // 愤怒穴位：击中要害 → 攻击 +6
        regAbility(new Ab() {
            public String id() { return "anger-point"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.criticalHit) {
                    ctx.result.put("angerPoint", true);
                }
            }
        });

        // ====================================================================
        //  受伤后触发道具（被特定属性招式命中时消耗并提升能力）
        // ====================================================================

        // 充电池：受到电系 → 消耗，攻击 +1
        regItem(new It() {
            public String id() { return "cell-battery"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == ELECTRIC) {
                    ctx.result.put("itemReactive", "cellBattery");
                }
            }
        });
        // 光苔：受到水系 → 消耗，特防 +1
        regItem(new It() {
            public String id() { return "luminous-moss"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == WATER) {
                    ctx.result.put("itemReactive", "luminousMoss");
                }
            }
        });
        // 雪球：受到冰系 → 消耗，攻击 +1
        regItem(new It() {
            public String id() { return "snowball"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == ICE) {
                    ctx.result.put("itemReactive", "snowball");
                }
            }
        });
        // 球根：受到水系 → 消耗，特攻 +1
        regItem(new It() {
            public String id() { return "absorb-bulb"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == WATER) {
                    ctx.result.put("itemReactive", "absorbBulb");
                }
            }
        });

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
    }

    // ========================================================================
    //  辅助注册方法
    // ========================================================================

    /** 避免匿名类中重复写 AbilityHandler 全方法 */
    private abstract static class Ab implements AbilityHandler {}
    private abstract static class It implements ItemHandler {}

    private static void regAbility(AbilityHandler h) { register(h); }
    private static void regItem(ItemHandler h) { register(h); }

    /** 属性增伤道具 */
    private static void registerTypeBoostItems() {
        // (name_en, typeId)
        String[][] typeItems = {
            {"mystic-water", "sea-incense"},    // 水
            {"charcoal", "heat-rock"},           // 火
            {"miracle-seed", "rose-incense"},    // 草
            {"never-melt-ice"},                  // 冰
            {"black-belt", "fighting-gem"},      // 格斗
            {"poison-barb", "black-sludge"},     // 毒
            {"soft-sand"},                       // 地
            {"sharp-beak"},                      // 飞
            {"twisted-spoon", "odd-incense"},    // 超
            {"silver-powder"},                   // 虫
            {"hard-stone", "rock-incense"},      // 岩
            {"spell-tag"},                       // 鬼
            {"dragon-fang", "dragon-scale"},     // 龙
            {"black-glasses"},                   // 恶
            {"metal-coat", "steel-incense"},     // 钢
            {"silk-scarf"},                      // 普
            {"magnet"},                          // 电
            {"never-melt-ice"},                  // 冰
        };
        int[] types = {WATER, FIRE, GRASS, ICE, FIGHTING, POISON, GROUND, FLYING,
                       PSYCHIC, BUG, ROCK, GHOST, DRAGON, DARK, STEEL, NORMAL, ELECTRIC, ICE};
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            for (String name : typeItems[i]) {
                regItem(new It() {
                    public String id() { return name; }
                    public double onSourceModifyDamage(AttackContext ctx, double mod) {
                        return ctx.moveTypeIs(type) ? mod * 1.2 : mod;
                    }
                });
            }
        }
    }

    /** 属性石板（Arceus）、记忆卡带（Silvally）、属性宝石 */
    private static void registerPlatesMemoriesGems() {
        // 石板 + 记忆 + 宝石按类型索引
        String[] typeSuffixes = {"fire", "water", "electric", "grass", "ice", "fighting",
                "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel"};
        int[] typeIds = {FIRE, WATER, ELECTRIC, GRASS, ICE, FIGHTING, POISON, GROUND, FLYING,
                PSYCHIC, BUG, ROCK, GHOST, DRAGON, DARK, STEEL};
        for (int i = 0; i < typeSuffixes.length; i++) {
            String suff = typeSuffixes[i];
            int tid = typeIds[i];
            // Plate: flame-plate, splash-plate, etc.
            regItem(new It() {
                public String id() { return suff + "-plate"; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) ? mod * 1.2 : mod;
                }
            });
            // Memory: fire-memory, water-memory, etc.
            regItem(new It() {
                public String id() { return suff + "-memory"; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) ? mod * 1.2 : mod;
                }
            });
            // Gem: fire-gem, water-gem, etc. 首次使用 1.3x
            regItem(new It() {
                public String id() { return suff + "-gem"; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) && !Boolean.TRUE.equals(ctx.attacker.get("itemConsumed"))
                            ? mod * 1.3 : mod;
                }
            });
        }
    }

    /** 封面神兽属性球 */
    private static void regSpeciesOrb() {
        regItem(new It() {
            public String id() { return "adamant-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "dialga") && ctx.moveTypeIs(STEEL, DRAGON) ? mod * 1.2 : mod;
            }
        });
        regItem(new It() {
            public String id() { return "lustrous-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "palkia") && ctx.moveTypeIs(WATER, DRAGON) ? mod * 1.2 : mod;
            }
        });
        regItem(new It() {
            public String id() { return "griseous-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "giratina") && ctx.moveTypeIs(GHOST, DRAGON) ? mod * 1.2 : mod;
            }
        });
    }

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
        // Booster Energy 触发
        if ("booster-energy".equals(heldItem(ctx.attacker))) return true;
        // 场地激活
        return fieldActive(ctx.state, fieldKey);
    }

    static boolean hasStatus(String condition) {
        return condition != null && !condition.isEmpty()
                && !"ready".equals(condition) && !"fainted".equals(condition);
    }

    static boolean isSpecies(Map<String, Object> mon, String... names) {
        String s = strVal(mon, "name_en");
        for (String n : names) if (n.equalsIgnoreCase(s)) return true;
        return false;
    }

    static boolean isSlicingMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("slash") || n.contains("cut") || n.contains("blade")
            || n.contains("razor") || n.contains("claw") || n.contains("axe")
            || n.contains("night-slash") || n.contains("night slash")
            || n.contains("psycho-cut") || n.contains("leaf-blade");
    }

    static boolean isSoundMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("boomburst") || n.contains("hypervoice") || n.contains("hyper voice")
            || n.contains("bug-buzz") || n.contains("bug buzz")
            || n.contains("snarl") || n.contains("overdrive") || n.contains("clang");
    }

    static boolean isWindMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("gust") || n.contains("twister") || n.contains("hurricane")
            || n.contains("bleakwind") || n.contains("icy-wind") || n.contains("icy wind")
            || n.contains("heat-wave") || n.contains("heat wave")
            || n.contains("tailwind") || n.contains("air-slash");
    }

    static boolean isRecoilMove(Map<String, Object> move) {
        String n = moveName(move);
        return n.contains("double-edge") || n.contains("double edge")
            || n.contains("flare-blitz") || n.contains("flare blitz")
            || n.contains("wood-hammer") || n.contains("wood hammer")
            || n.contains("head-smash") || n.contains("head smash")
            || n.contains("brave-bird") || n.contains("brave bird")
            || n.contains("wild-charge") || n.contains("wild charge")
            || n.contains("volt-tackle") || n.contains("volt tackle");
    }

    static boolean moveCategory(String cat, Map<String, Object> move) {
        return moveName(move).contains(cat);
    }

    static boolean hasEffectChance(Map<String, Object> move) {
        return intVal(move, "effect_chance") > 0;
    }

    /** 检查招式是否带有某 flag（如 sound/bullet/wind/contact/powder） */
    @SuppressWarnings("unchecked")
    public static boolean hasMoveFlag(Map<String, Object> move, String expected) {
        Object flags = move.get("flags");
        if (flags instanceof List<?> list) {
            for (Object flag : list) {
                if (expected.equalsIgnoreCase(String.valueOf(flag))) {
                    return true;
                }
            }
            return false;
        }
        if (flags instanceof String s) {
            for (String part : s.split(",")) {
                if (expected.equalsIgnoreCase(part.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    static String moveName(Map<String, Object> move) {
        Object n = move.get("name_en");
        return n == null ? "" : String.valueOf(n).toLowerCase();
    }

    static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    static String strVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v).toLowerCase();
    }
}
