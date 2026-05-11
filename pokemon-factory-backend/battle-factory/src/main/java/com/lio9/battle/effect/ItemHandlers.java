package com.lio9.battle.effect;

import static com.lio9.battle.effect.MoveUtils.*;
import static com.lio9.battle.effect.PokemonType.*;

import java.util.Map;

/**
 * 道具效果 handler 注册中心。
 *
 * <p>从 {@link EffectRegistry#registerAll()} 中提取，
 * 所有道具 handler 在此集中注册，保持 EffectRegistry 仅限于能力 handler 和 dispatch。</p>
 */
public final class ItemHandlers {

    private ItemHandlers() {}

    /** 将全部道具 handler 注册到指定 map 中 */
    public static void registerAll(Map<String, ItemHandler> items) {
        // ========== 伤害倍率道具 ==========

        regItem(items, new ItemHandler() {
            public String id() { return "life-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) { return mod * 1.3; }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "metronome"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                int count = intVal(ctx.attacker, "metronomeCount");
                return count > 0 ? mod * (1.0 + count * 0.2) : mod;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "expert-belt"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) { return mod; }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "muscle-band"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == PHYSICAL ? mod * 1.1 : mod;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "wise-glasses"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return ctx.damageClassId == SPECIAL ? mod * 1.1 : mod;
            }
        });

        // === 属性增伤道具 ===
        registerTypeBoostItems(items);
        registerPlatesMemoriesGems(items);

        // === Species 限定道具 ===
        regItem(items, new ItemHandler() {
            public String id() { return "light-ball"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return isSpecies(ctx.attacker, "pikachu") ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "thick-club"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && isSpecies(ctx.attacker, "cubone", "marowak")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "deep-sea-tooth"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "clamperl")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "deep-sea-scale"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL && isSpecies(ctx.attacker, "clamperl")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        regItem(items, new ItemHandler() {
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
        regItem(items, new ItemHandler() {
            public String id() { return "metal-powder"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL && isSpecies(ctx.defender, "ditto")
                        ? (int) Math.floor(stat * 2.0) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "quick-powder"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return isSpecies(ctx.mon, "ditto") ? speed * 2 : speed;
            }
        });
        regSpeciesOrb(items);

        // === 攻击/防御/速度修正道具 ===
        regItem(items, new ItemHandler() {
            public String id() { return "choice-band"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == PHYSICAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "choice-specs"; }
            public int onSourceModifyAttackStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "choice-scarf"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return (int) Math.floor(speed * 1.5);
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "assault-vest"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return ctx.damageClassId == SPECIAL ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "eviolite"; }
            public int onTargetModifyDefenseStat(AttackContext ctx, int stat) {
                return Boolean.TRUE.equals(ctx.defender.get("notFullyEvolved"))
                        ? (int) Math.floor(stat * 1.5) : stat;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "iron-ball"; }
            public int onModifySpeed(SpeedContext ctx, int speed) { return Math.max(1, speed / 2); }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "room-service"; }
            public int onModifySpeed(SpeedContext ctx, int speed) {
                return EffectRegistry.fieldActive(ctx.state, "trickRoomTurns") ? Math.max(1, speed / 2) : speed;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "float-stone"; }
            public int onModifyWeight(WeightContext ctx, int weight) { return Math.max(1, weight / 2); }
        });

        // Punching Glove
        regItem(items, new ItemHandler() {
            public String id() { return "punching-glove"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return moveCategory("punch", ctx.move) ? mod * 1.1 : mod;
            }
        });

        // === 受伤后触发道具 ===
        regItem(items, new ItemHandler() {
            public String id() { return "cell-battery"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == ELECTRIC.id()) ctx.result.put("itemReactive", "cellBattery");
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "luminous-moss"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == WATER.id()) ctx.result.put("itemReactive", "luminousMoss");
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "snowball"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == ICE.id()) ctx.result.put("itemReactive", "snowball");
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "absorb-bulb"; }
            public void onDamageReceived(DamageReceivedContext ctx) {
                if (ctx.moveTypeId == WATER.id()) ctx.result.put("itemReactive", "absorbBulb");
            }
        });
    }

    // ── 注册快捷方法 ───────────────────────────────────────────────────

    private static void regItem(Map<String, ItemHandler> items, ItemHandler h) {
        items.put(h.id().toLowerCase(), h);
    }

    // ── 辅助方法 ────────────────────────────────────────────────────────

    /** 属性增伤道具（Charcoal / Mystic Water 等） */
    private static void registerTypeBoostItems(Map<String, ItemHandler> items) {
        String[][] typeItems = {
            {"charcoal", "fire"}, {"mystic-water", "water"}, {"magnet", "electric"},
            {"miracle-seed", "grass"}, {"never-melt-ice", "ice"}, {"black-belt", "fighting"},
            {"poison-barb", "poison"}, {"soft-sand", "ground"}, {"sharp-beak", "flying"},
            {"twisted-spoon", "psychic"}, {"silver-powder", "bug"}, {"hard-stone", "rock"},
            {"spell-tag", "ghost"}, {"dragon-fang", "dragon"}, {"black-glasses", "dark"},
            {"metal-coat", "steel"}, {"flame-plate", "fire"}, {"splash-plate", "water"},
            {"zap-plate", "electric"}, {"meadow-plate", "grass"}, {"icicle-plate", "ice"},
            {"fist-plate", "fighting"}, {"toxic-plate", "poison"}, {"earth-plate", "ground"},
            {"sky-plate", "flying"}, {"mind-plate", "psychic"}, {"insect-plate", "bug"},
            {"stone-plate", "rock"}, {"spooky-plate", "ghost"}, {"draco-plate", "dragon"},
            {"dread-plate", "dark"}, {"iron-plate", "steel"}, {"pixie-plate", "fairy"},
            {"fighting-memory", "fighting"}, {"flying-memory", "flying"}, {"poison-memory", "poison"},
            {"ground-memory", "ground"}, {"rock-memory", "rock"}, {"bug-memory", "bug"},
            {"ghost-memory", "ghost"}, {"steel-memory", "steel"}, {"fire-memory", "fire"},
            {"water-memory", "water"}, {"grass-memory", "grass"}, {"electric-memory", "electric"},
            {"psychic-memory", "psychic"}, {"ice-memory", "ice"}, {"dragon-memory", "dragon"},
            {"dark-memory", "dark"}, {"fairy-memory", "fairy"},
        };
        for (String[] pair : typeItems) {
            String id = pair[0];
            int tid = pokemonTypeId(pair[1]);
            regItem(items, new ItemHandler() {
                public String id() { return id; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) ? mod * 1.2 : mod;
                }
            });
        }
    }

    /** 属性石板/记忆卡带/属性宝石 */
    private static void registerPlatesMemoriesGems(Map<String, ItemHandler> items) {
        String[] typeSuffixes = {"fire", "water", "electric", "grass", "ice", "fighting",
                "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"};
        for (String suff : typeSuffixes) {
            int tid = pokemonTypeId(suff);
            // 石板 + 记忆 → 1.2x
            final String id1 = suff + "-plate";
            regItem(items, new ItemHandler() {
                public String id() { return id1; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) ? mod * 1.2 : mod;
                }
            });
            final String id2 = suff + "-memory";
            regItem(items, new ItemHandler() {
                public String id() { return id2; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) ? mod * 1.2 : mod;
                }
            });
            // 宝石 → 1.3x 同属性且未消耗
            final String gemId = suff + "-gem";
            regItem(items, new ItemHandler() {
                public String id() { return gemId; }
                public double onSourceModifyDamage(AttackContext ctx, double mod) {
                    return ctx.moveTypeIs(tid) && !Boolean.TRUE.equals(ctx.attacker.get("itemConsumed"))
                            ? mod * 1.3 : mod;
                }
            });
        }
    }

    /** 封面神兽属性球 */
    private static void regSpeciesOrb(Map<String, ItemHandler> items) {
        regItem(items, new ItemHandler() {
            public String id() { return "adamant-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "dialga") && ctx.moveTypeIs(STEEL.id(), DRAGON.id()) ? mod * 1.2 : mod;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "lustrous-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "palkia") && ctx.moveTypeIs(WATER.id(), DRAGON.id()) ? mod * 1.2 : mod;
            }
        });
        regItem(items, new ItemHandler() {
            public String id() { return "griseous-orb"; }
            public double onSourceModifyDamage(AttackContext ctx, double mod) {
                return isSpecies(ctx.attacker, "giratina") && ctx.moveTypeIs(GHOST.id(), DRAGON.id()) ? mod * 1.2 : mod;
            }
        });
    }

    /** 从 PokemonType 获取类型 ID */
    private static int pokemonTypeId(String eng) {
        for (PokemonType t : PokemonType.values()) {
            if (t.name().equalsIgnoreCase(eng)) return t.id();
        }
        return NORMAL.id(); // fallback
    }
}
