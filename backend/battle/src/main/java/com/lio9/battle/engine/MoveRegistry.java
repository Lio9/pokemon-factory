package com.lio9.battle.engine;



import com.lio9.common.util.BattleUtils;

import java.util.Map;
import java.util.Set;

/**
 * 招式注册表。
 * <p>
 * 统一管理所有招式的分类和判断逻辑，替代 BattleEngine 中分散的大量 isXxx() 硬编码判断。
 * 这一层的核心价值是：
 * <ul>
 *     <li>把“招式名别名/连字符差异”统一归并</li>
 *     <li>让行动、状态、场地、命中等模块复用同一份分类标准</li>
 *     <li>降低后续对齐 PS 规则时的重复修改成本</li>
 * </ul>
 * </p>
 */
public final class MoveRegistry {

    // === 保护类招式 ===
    private static final Set<String> PROTECT_MOVES = Set.of(
        "protect", "detect", "king's shield", "kings-shield",
        "obstruct", "silk trap", "silk-trap",
        "burning bulwark", "burning-bulwark",
        "baneful bunker", "baneful-bunker"
    );

    private static final Set<String> WIDE_GUARD_MOVES = Set.of(
        "wide guard", "wide-guard"
    );

    private static final Set<String> QUICK_GUARD_MOVES = Set.of(
        "quick guard", "quick-guard"
    );

    // === 引导类招式 ===
    private static final Set<String> REDIRECTION_MOVES = Set.of(
        "follow me", "follow-me", "rage powder", "rage-powder"
    );

    // === 辅助类招式 ===
    private static final Set<String> HELPING_HAND_MOVES = Set.of(
        "helping hand", "helping-hand"
    );

    private static final Set<String> ALLY_SWITCH_MOVES = Set.of(
        "ally switch", "ally-switch"
    );

    // === 场地效果类招式 ===
    private static final Set<String> TAILWIND_MOVES = Set.of("tailwind");
    private static final Set<String> TRICK_ROOM_MOVES = Set.of("trick room", "trick-room");

    // === 天气类招式 ===
    private static final Set<String> RAIN_MOVES = Set.of("rain dance", "rain-dance");
    private static final Set<String> SUN_MOVES = Set.of("sunny day", "sunny-day");
    private static final Set<String> SAND_MOVES = Set.of("sandstorm");
    private static final Set<String> SNOW_MOVES = Set.of("snowscape", "hail");

    // === 地形类招式 ===
    private static final Set<String> ELECTRIC_TERRAIN_MOVES = Set.of("electric terrain", "electric-terrain");
    private static final Set<String> PSYCHIC_TERRAIN_MOVES = Set.of("psychic terrain", "psychic-terrain");
    private static final Set<String> GRASSY_TERRAIN_MOVES = Set.of("grassy terrain", "grassy-terrain");
    private static final Set<String> MISTY_TERRAIN_MOVES = Set.of("misty terrain", "misty-terrain");

    // === 屏风类招式 ===
    private static final Set<String> REFLECT_MOVES = Set.of("reflect");
    private static final Set<String> LIGHT_SCREEN_MOVES = Set.of("light screen", "light-screen");
    private static final Set<String> AURORA_VEIL_MOVES = Set.of("aurora veil", "aurora-veil");
    private static final Set<String> SAFEGUARD_MOVES = Set.of("safeguard");

    // === 状态异常类招式 ===
    private static final Set<String> THUNDER_WAVE_MOVES = Set.of("thunder wave", "thunder-wave");
    private static final Set<String> WILL_O_WISP_MOVES = Set.of("will-o-wisp", "will o wisp");
    private static final Set<String> TOXIC_MOVES = Set.of("toxic");
    private static final Set<String> POISON_POWDER_MOVES = Set.of("poison powder", "poison-powder");
    private static final Set<String> SPORE_MOVES = Set.of("spore");
    private static final Set<String> YAWN_MOVES = Set.of("yawn");
    private static final Set<String> CONFUSE_RAY_MOVES = Set.of("confuse ray", "confuse-ray");

    // === 封锁类招式 ===
    private static final Set<String> TAUNT_MOVES = Set.of("taunt");
    private static final Set<String> ENCORE_MOVES = Set.of("encore");
    private static final Set<String> DISABLE_MOVES = Set.of("disable");
    private static final Set<String> TORMENT_MOVES = Set.of("torment");
    private static final Set<String> HEAL_BLOCK_MOVES = Set.of("heal block", "heal-block");

    // === 捕获类招式 ===
    private static final Set<String> TRAPPING_MOVES = Set.of(
        "mean look", "mean-look", "block", "spider web", "spider-web",
        "anchor shot", "anchor-shot", "spirit shackle", "spirit-shackle",
        "sand tomb", "sand-tomb", "whirlpool", "fire spin", "fire-spin",
        "infestation", "magma storm", "magma-storm", "snap trap", "snap-trap",
        "thousand waves", "thousand-waves", "bind", "wrap",
        "fairy lock", "fairy-lock", "octolock", "jaw lock", "jaw-lock"
    );

    // === 束缚持续伤害类招式（每回合造成 1/8 最大 HP 伤害）===
    private static final Set<String> BINDING_MOVES = Set.of(
        "bind", "wrap", "fire spin", "fire-spin", "whirlpool",
        "sand tomb", "sand-tomb", "clamp", "infestation",
        "magma storm", "magma-storm", "snap trap", "snap-trap"
    );

    // === 先制攻击类招式 ===
    private static final Set<String> FAKE_OUT_MOVES = Set.of("fake out", "fake-out");
    private static final Set<String> SUCKER_PUNCH_MOVES = Set.of("sucker punch", "sucker-punch");
    private static final Set<String> FEINT_MOVES = Set.of("feint");

    // === 速度控制类招式 ===
    private static final Set<String> ICY_WIND_MOVES = Set.of("icy wind", "icy-wind");
    private static final Set<String> ELECTROWEB_MOVES = Set.of("electroweb");
    private static final Set<String> SNARL_MOVES = Set.of("snarl");
    private static final Set<String> FAKE_TEARS_MOVES = Set.of("fake tears", "fake-tears");

    // === 轮换类招式 ===
    private static final Set<String> U_TURN_MOVES = Set.of("u turn", "u-turn");
    private static final Set<String> VOLT_SWITCH_MOVES = Set.of("volt switch", "volt-switch");
    private static final Set<String> FLIP_TURN_MOVES = Set.of("flip turn", "flip-turn");
    private static final Set<String> PARTING_SHOT_MOVES = Set.of("parting shot", "parting-shot");

    // === 自我强化类招式 ===
    private static final Set<String> SWORDS_DANCE_MOVES = Set.of("swords dance", "swords-dance");
    private static final Set<String> NASTY_PLOT_MOVES = Set.of("nasty plot", "nasty-plot");
    private static final Set<String> DRAGON_DANCE_MOVES = Set.of("dragon dance", "dragon-dance");
    private static final Set<String> CALM_MIND_MOVES = Set.of("calm mind", "calm-mind");
    private static final Set<String> AGILITY_MOVES = Set.of("agility");
    private static final Set<String> AUTOTOMIZE_MOVES = Set.of("autotomize");
    private static final Set<String> BULK_UP_MOVES = Set.of("bulk up", "bulk-up");
    private static final Set<String> WORK_UP_MOVES = Set.of("work up", "work-up");
    private static final Set<String> QUIVER_DANCE_MOVES = Set.of("quiver dance", "quiver-dance");
    private static final Set<String> COIL_MOVES = Set.of("coil");
    private static final Set<String> SHELL_SMASH_MOVES = Set.of("shell smash", "shell-smash");

    // === 入场 hazards 类招式 ===
    private static final Set<String> STEALTH_ROCK_MOVES = Set.of("stealth rock", "stealth-rock");
    private static final Set<String> SPIKES_MOVES = Set.of("spikes");
    private static final Set<String> TOXIC_SPIKES_MOVES = Set.of("toxic spikes", "toxic-spikes");
    private static final Set<String> STICKY_WEB_MOVES = Set.of("sticky web", "sticky-web");
    private static final Set<String> RAPID_SPIN_MOVES = Set.of("rapid spin", "rapid-spin");
    private static final Set<String> DEFOG_MOVES = Set.of("defog");

    // === 交换道具类招式 ===
    private static final Set<String> TRICK_MOVES = Set.of("trick", "switcheroo");

    // === 回复类招式 ===
    private static final Set<String> LEECH_SEED_MOVES = Set.of("leech seed", "leech-seed");
    private static final Set<String> SUBSTITUTE_MOVES = Set.of("substitute");
    private static final Set<String> ATTRACT_MOVES = Set.of("attract");
    private static final Set<String> PERISH_SONG_MOVES = Set.of("perish song", "perish-song");

    private static final Set<String> RECOVER_MOVES = Set.of("recover");
    private static final Set<String> ROOST_MOVES = Set.of("roost");
    private static final Set<String> REST_MOVES = Set.of("rest");
    private static final Set<String> SOFT_BOILED_MOVES = Set.of("soft boiled", "soft-boiled");
    private static final Set<String> MILK_DRINK_MOVES = Set.of("milk drink", "milk-drink");
    private static final Set<String> SYNTHESIS_MOVES = Set.of("synthesis");
    private static final Set<String> MOONLIGHT_MOVES = Set.of("moonlight");
    private static final Set<String> MORNING_SUN_MOVES = Set.of("morning sun", "morning-sun");

    // === 蓄力类招式 ===
    private static final Set<String> CHARGE_MOVES = Set.of(
        "solar beam", "solar-beam", "solar blade", "solar-blade",
        "sky attack", "sky-attack", "meteor beam", "meteor-beam",
        "skull bash", "skull-bash", "razor wind", "razor-wind",
        "freeze shock", "freeze-shock", "ice burn", "ice-burn",
        "geomancy",
        // 半无敌二回合招式
        "fly", "dig", "dive", "bounce",
        "phantom force", "phantom-force", "shadow force", "shadow-force"
    );

    /** 蓄力期间进入半无敌状态的招式 */
    private static final Set<String> SEMI_INVULNERABLE_CHARGE_MOVES = Set.of(
        "fly", "dig", "dive", "bounce",
        "phantom force", "phantom-force", "shadow force", "shadow-force"
    );

    // === 硬直类招式 ===
    private static final Set<String> RECHARGE_MOVES = Set.of(
        "hyper beam", "hyper-beam", "giga impact", "giga-impact",
        "blast burn", "blast-burn", "hydro cannon", "hydro-cannon",
        "frenzy plant", "frenzy-plant", "rock wrecker", "rock-wrecker",
        "roar of time", "roar-of-time", "prismatic laser", "prismatic-laser",
        "meteor assault", "meteor-assault", "eternabeam"
    );

    // === 太晶爆发 ===
    private static final Set<String> TERA_BLAST_MOVES = Set.of("tera blast", "tera-blast");

    // === 延迟攻击类招式 ===
    private static final Set<String> FUTURE_SIGHT_MOVES = Set.of("future sight", "future-sight");
    private static final Set<String> DOOM_DESIRE_MOVES = Set.of("doom desire", "doom-desire");

    // === 接力类招式 ===
    private static final Set<String> BATON_PASS_MOVES = Set.of("baton pass", "baton-pass");

    // === 窃取类招式 ===
    private static final Set<String> THIEF_MOVES = Set.of("thief", "covet");
    public static boolean isThiefMove(Map<String, Object> move) { return matchesAny(move, THIEF_MOVES); }

    // === 击落类招式 ===
    private static final Set<String> KNOCK_OFF_MOVES = Set.of("knock off", "knock-off");

    // === 使用防御属性计算伤害的招式 ===
    private static final Set<String> BODY_PRESS_MOVES = Set.of("body press", "body-press");

    // === 使用目标攻击属性的招式 ===
    private static final Set<String> FOUL_PLAY_MOVES = Set.of("foul play", "foul-play");

    // === 基于速度比计算威力的招式 ===
    private static final Set<String> ELECTRO_BALL_MOVES = Set.of("electro ball", "electro-ball");
    private static final Set<String> GYRO_BALL_MOVES = Set.of("gyro ball", "gyro-ball");

    // === 体重相关威力招式 ===
    private static final Set<String> WEIGHT_BASED_MOVES = Set.of(
        "grass knot", "grass-knot", "low kick", "low-kick",
        "heavy slam", "heavy-slam", "heat crash", "heat-crash"
    );

    // === HP 比例威力招式 ===
    private static final Set<String> HP_RATIO_MOVES = Set.of(
        "eruption", "water spout", "water-spout"
    );

    // === 反比 HP 威力招式（HP 越少威力越大） ===
    private static final Set<String> REVERSAL_MOVES = Set.of(
        "reversal", "flail"
    );

    // === 后手增伤招式 ===
    private static final Set<String> PAYBACK_MOVES = Set.of("payback");
    private static final Set<String> AVALANCHE_MOVES = Set.of("avalanche");

    // === 无视防御阶级招式 ===
    private static final Set<String> SACRED_SWORD_MOVES = Set.of(
        "sacred sword", "sacred-sword"
    );

    // === 吸取力量类招式 ===
    private static final Set<String> STRENGTH_SAP_MOVES = Set.of(
        "strength sap", "strength-sap"
    );

    // === 妖精之吻（参考，已有 drain 字段支持）===
    private static final Set<String> DRAINING_KISS_MOVES = Set.of(
        "draining kiss", "draining-kiss"
    );

    // === 能力平分/互换类 ===
    private static final Set<String> GUARD_SPLIT_MOVES = Set.of(
        "guard split", "guard-split"
    );
    private static final Set<String> POWER_SPLIT_MOVES = Set.of(
        "power split", "power-split"
    );
    private static final Set<String> GUARD_SWAP_MOVES = Set.of(
        "guard swap", "guard-swap"
    );
    private static final Set<String> POWER_SWAP_MOVES = Set.of(
        "power swap", "power-swap"
    );

    // === 强制换人招式（吼叫/吹飞/龙尾/巴投） ===
    private static final Set<String> FORCED_SWITCH_MOVES = Set.of(
        "roar", "whirlwind",
        "dragon tail", "dragon-tail",
        "circle throw", "circle-throw"
    );

    /** 强制换人招式是否为声音系（吼叫是声音系，吹飞/龙尾/巴投不是） */
    public static boolean isForcedSwitchSoundMove(Map<String, Object> move) {
        return matchesAny(move, Set.of("roar"));
    }

    public static boolean isForcedSwitchMove(Map<String, Object> move) {
        return matchesAny(move, FORCED_SWITCH_MOVES);
    }

    // === 强化类招式 ===
    private static final Set<String> HONE_CLAWS_MOVES = Set.of(
        "hone claws", "hone-claws"
    );
    private static final Set<String> IRON_DEFENSE_MOVES = Set.of(
        "iron defense", "iron-defense"
    );
    private static final Set<String> GROWTH_MOVES = Set.of("growth");
    private static final Set<String> BELLY_DRUM_MOVES = Set.of(
        "belly drum", "belly-drum"
    );
    private static final Set<String> HOWL_MOVES = Set.of("howl");

    public static boolean isHoneClaws(Map<String, Object> move) { return matchesAny(move, HONE_CLAWS_MOVES); }
    public static boolean isIronDefense(Map<String, Object> move) { return matchesAny(move, IRON_DEFENSE_MOVES); }
    public static boolean isGrowth(Map<String, Object> move) { return matchesAny(move, GROWTH_MOVES); }
    public static boolean isBellyDrum(Map<String, Object> move) { return matchesAny(move, BELLY_DRUM_MOVES); }
    public static boolean isHowl(Map<String, Object> move) { return matchesAny(move, HOWL_MOVES); }

    // === 杂技（无道具时威力翻倍） ===
    private static final Set<String> ACROBATICS_MOVES = Set.of("acrobatics");

    public static boolean isAcrobatics(Map<String, Object> move) { return matchesAny(move, ACROBATICS_MOVES); }

    // === 反伤类招式 ===
    private static final Set<String> COUNTER_MOVES = Set.of("counter");
    private static final Set<String> MIRROR_COAT_MOVES = Set.of("mirror coat", "mirror-coat");
    private static final Set<String> METAL_BURST_MOVES = Set.of("metal burst", "metal-burst");

    // === 挺住类招式 ===
    private static final Set<String> ENDURE_MOVES = Set.of("endure");

    // === 治愈类招式 ===
    private static final Set<String> HEAL_BELL_MOVES = Set.of("heal bell", "heal-bell");
    private static final Set<String> AROMATHERAPY_MOVES = Set.of("aromatherapy");
    private static final Set<String> REFRESH_MOVES = Set.of("refresh");

    // === 喉斩（沉默声系）===
    private static final Set<String> THROAT_CHOP_MOVES = Set.of("throat chop", "throat-chop");

    // === 击落（使目标地面化）===
    private static final Set<String> SMACK_DOWN_MOVES = Set.of("smack down", "smack-down");

    // === 八爪束缚（束缚 + 每回合双防 -1）===
    private static final Set<String> OCTOLOCK_MOVES = Set.of("octolock");

    // === 大嚼大嚼（束缚双方）===
    private static final Set<String> JAW_LOCK_MOVES = Set.of("jaw lock", "jaw-lock");

    // === 背水一战（全能力+1，自身束缚）===
    private static final Set<String> NO_RETREAT_MOVES = Set.of("no retreat", "no-retreat");

    // === 焦油覆盖（火系弱点 + 速度 -1）===
    private static final Set<String> TAR_SHOT_MOVES = Set.of("tar shot", "tar-shot");

    // === 精神种子 / 胃酸 / 心灵互换 / 精神转移 ===
    private static final Set<String> WORRY_SEED_MOVES = Set.of("worry seed", "worry-seed");
    private static final Set<String> GASTRO_ACID_MOVES = Set.of("gastro acid", "gastro-acid");
    private static final Set<String> HEART_SWAP_MOVES = Set.of("heart swap", "heart-swap");
    private static final Set<String> PSYCHO_SHIFT_MOVES = Set.of("psycho shift", "psycho-shift");

    // === 睡眠相关招式 ===
    private static final Set<String> SLEEP_TALK_MOVES = Set.of("sleep talk", "sleep-talk");
    private static final Set<String> SNORE_MOVES = Set.of("snore");
    private static final Set<String> NIGHTMARE_MOVES = Set.of("nightmare");
    private static final Set<String> DREAM_EATER_MOVES = Set.of("dream eater", "dream-eater");

    // === 清除能力变化类招式 ===
    private static final Set<String> HAZE_MOVES = Set.of("haze");

    // === 封印类招式 ===
    private static final Set<String> IMPRISON_MOVES = Set.of("imprison");
    public static boolean isImprison(Map<String, Object> move) { return matchesAny(move, IMPRISON_MOVES); }

    // === 亲密度类招式 ===
    private static final Set<String> FRIENDSHIP_MOVES = Set.of("return", "frustration");
    public static boolean isFriendshipMove(Map<String, Object> move) { return matchesAny(move, FRIENDSHIP_MOVES); }

    // === 自杀类招式（付出代价的不攻击招式） ===
    private static final Set<String> SUICIDE_MOVES = Set.of("memento", "healing wish", "healing-wish", "lunar dance", "lunar-dance");
    public static boolean isSuicideMove(Map<String, Object> move) { return matchesAny(move, SUICIDE_MOVES); }

    // === 变身类招式 ===
    private static final Set<String> TRANSFORM_MOVES = Set.of("transform");
    public static boolean isTransform(Map<String, Object> move) { return matchesAny(move, TRANSFORM_MOVES); }

    // === 树果相关招式 ===
    private static final Set<String> BELCH_MOVES = Set.of("belch");
    private static final Set<String> BUG_BITE_MOVES = Set.of("bug bite", "bug-bite");
    private static final Set<String> NATURAL_GIFT_MOVES = Set.of("natural gift", "natural-gift");
    public static boolean isBelch(Map<String, Object> move) { return matchesAny(move, BELCH_MOVES); }
    public static boolean isBugBite(Map<String, Object> move) { return matchesAny(move, BUG_BITE_MOVES); }
    public static boolean isNaturalGift(Map<String, Object> move) { return matchesAny(move, NATURAL_GIFT_MOVES); }

    // === 换场类招式 ===
    private static final Set<String> COURT_CHANGE_MOVES = Set.of("court change", "court-change");
    public static boolean isCourtChange(Map<String, Object> move) { return matchesAny(move, COURT_CHANGE_MOVES); }

    // === 复活类招式 ===
    private static final Set<String> REVIVAL_BLESSING_MOVES = Set.of("revival blessing", "revival-blessing");
    public static boolean isRevivalBlessing(Map<String, Object> move) { return matchesAny(move, REVIVAL_BLESSING_MOVES); }

    // === 蜕尾类招式 ===
    private static final Set<String> SHED_TAIL_MOVES = Set.of("shed tail", "shed-tail");
    public static boolean isShedTail(Map<String, Object> move) { return matchesAny(move, SHED_TAIL_MOVES); }

    // === 毒旋清除类 ===
    private static final Set<String> MORTAL_SPIN_MOVES = Set.of("mortal spin", "mortal-spin");
    public static boolean isMortalSpin(Map<String, Object> move) { return matchesAny(move, MORTAL_SPIN_MOVES); }

    // === 盐腌类 ===
    private static final Set<String> SALT_CURE_MOVES = Set.of("salt cure", "salt-cure");
    public static boolean isSaltCure(Map<String, Object> move) { return matchesAny(move, SALT_CURE_MOVES); }

    // === 蓄力类招式 ===
    private static final Set<String> STOCKPILE_MOVES = Set.of("stockpile");
    private static final Set<String> SPIT_UP_MOVES = Set.of("spit up", "spit-up");
    private static final Set<String> SWALLOW_MOVES = Set.of("swallow");
    public static boolean isStockpile(Map<String, Object> move) { return matchesAny(move, STOCKPILE_MOVES); }
    public static boolean isSpitUp(Map<String, Object> move) { return matchesAny(move, SPIT_UP_MOVES); }
    public static boolean isSwallow(Map<String, Object> move) { return matchesAny(move, SWALLOW_MOVES); }

    // === 同命类招式 ===
    private static final Set<String> DESTINY_BOND_MOVES = Set.of("destiny bond", "destiny-bond");

    // === 分担痛楚类招式 ===
    private static final Set<String> PAIN_SPLIT_MOVES = Set.of("pain split", "pain-split");

    // === 场地改变类招式 ===
    private static final Set<String> GRAVITY_MOVES = Set.of("gravity");
    private static final Set<String> MAGIC_ROOM_MOVES = Set.of("magic room", "magic-room");
    private static final Set<String> WONDER_ROOM_MOVES = Set.of("wonder room", "wonder-room");

    // === 命中规则特殊招式 ===
    private static final Set<String> THUNDER_MOVES = Set.of("thunder");
    private static final Set<String> HURRICANE_MOVES = Set.of("hurricane");
    private static final Set<String> BLIZZARD_MOVES = Set.of("blizzard");

    // ==== 招式分类（替换 BattleDamageSupport 硬编码 name.contains）====
    private static final Set<String> PUNCHING_MOVES = Set.of(
        "punch", "hammer arm", "hammer-arm", "comet punch", "comet-punch",
        "bullet punch", "bullet-punch", "mach punch", "mach-punch", "dizzy punch", "dizzy-punch",
        "drain punch", "drain-punch", "focus punch", "focus-punch", "fire punch", "fire-punch",
        "ice punch", "ice-punch", "thunder punch", "thunder-punch", "mega punch", "mega-punch",
        "sky uppercut", "sky-uppercut", "shadow punch", "shadow-punch", "power-up punch", "power-up-punch",
        "close combat", "close-combat", "superpower", "cross chop", "cross-chop", "dynamic punch", "dynamic-punch"
    );
    private static final Set<String> BITING_MOVES = Set.of(
        "bite", "crunch", "fire fang", "fire-fang", "ice fang", "ice-fang",
        "thunder fang", "thunder-fang", "poison fang", "poison-fang", "psychic fangs", "psychic-fangs",
        "hyper fang", "hyper-fang"
    );
    private static final Set<String> PULSE_MOVES = Set.of(
        "pulse", "aura sphere", "aura-sphere", "dragon pulse", "dragon-pulse",
        "dark pulse", "dark-pulse", "water pulse", "water-pulse"
    );
    private static final Set<String> SOUND_MOVES = Set.of(
        "boomburst", "hypervoice", "hyper voice", "bug buzz", "bug-buzz",
        "snarl", "overdrive", "clang", "clanging scales", "clanging-scales",
        "clangorous soul", "clangorous-soul", "sparkling aria", "sparkling-aria",
        "sing", "growl", "roar", "screech", "supersonic", "metal sound", "metal-sound",
        "uproar", "howl", "noble roar", "noble-roar", "confide", "parting shot", "parting-shot",
        "round", "echoed voice", "echoed-voice", "relic song", "relic-song",
        "snore", "perish song", "perish-song",
        "heal bell", "heal-bell"
    );
    private static final Set<String> SLICING_MOVES = Set.of(
        "slash", "cut", "blade", "razor", "claw", "axe",
        "night slash", "night-slash", "psycho cut", "psycho-cut",
        "cross", "slic", "karate", "leaf blade", "leaf-blade",
        "x-scissor", "sacred sword", "sacred-sword", "swords dance", "swords-dance",
        "fury cutter", "fury-cutter", "aerial ace", "aerial-ace",
        "air cutter", "air-cutter", "crabhammer"
    );
    private static final Set<String> WIND_MOVES = Set.of(
        "gust", "twister", "hurricane", "bleakwind", "bleakwind storm", "bleakwind-storm",
        "springtide storm", "springtide-storm", "wildbolt storm", "wildbolt-storm",
        "icy wind", "icy-wind", "heat wave", "heat-wave", "tailwind",
        "air slash", "air-slash", "defog", "fairy wind", "fairy-wind", "ominous wind", "ominous-wind"
    );
    private static final Set<String> RECOIL_MOVES = Set.of(
        "double-edge", "flare blitz", "flare-blitz", "wood hammer", "wood-hammer",
        "head smash", "head-smash", "brave bird", "brave-bird", "take down", "take-down",
        "wild charge", "wild-charge", "volt tackle", "volt-tackle",
        "submission", "jump kick", "jump-kick", "high jump kick", "high-jump-kick",
        "head charge", "head-charge", "light of ruin", "light-of-ruin",
        "shadow rush", "shadow-rush"
    );

    // ==== 非接触物理招式（Showdown 规则：不触发接触效果）====
    // 声音类、风类、远程弹射/波类、地面震动、精神/岩石/毒弹等远程物理招
    private static final Set<String> NON_CONTACT_PHYSICAL_MOVES = Set.of(
        // 声音物理招
        "boomburst", "bug buzz", "bug-buzz", "clanging scales", "clanging-scales",
        "overdrive", "sparkling aria", "sparkling-aria", "hyper voice", "hyper-voice", "snarl",
        // 风类
        "gust", "twister", "hurricane", "heat-wave", "icy wind", "icy-wind",
        "air slash", "air-slash", "air cutter", "air-cutter", "fairy wind", "fairy-wind",
        "ominous wind", "ominous-wind", "bleakwind storm", "bleakwind-storm", "springtide storm", "springtide-storm",
        "wildbolt storm", "wildbolt-storm", "petal blizzard", "petal-blizzard",
        // 波/脉冲/光线
        "aura sphere", "aura-sphere", "dragon pulse", "dragon-pulse", "dark pulse", "dark-pulse",
        "water pulse", "water-pulse", "earth power", "earth-power", "energy ball", "energy-ball",
        "shadow ball", "shadow-ball", "sludge bomb", "sludge-bomb", "sludge wave", "sludge-wave",
        "flash cannon", "flash-cannon", "power gem", "power-gem", "ancient power", "ancient-power",
        "weather ball", "weather-ball", "terrain pulse", "terrain-pulse", "seed bomb", "seed-bomb",
        "focus blast", "focus-blast", "giga drain", "giga-drain", "mega drain", "mega-drain",
        // 地震/地面冲击
        "earthquake", "magnitude", "bulldoze", "high horsepower", "high-horsepower",
        "stomping tantrum", "stomping-tantrum", "precipice blades", "precipice-blades", "drill run", "drill-run",
        "bonemerang", "bone rush", "bone-rush", "bone club", "bone-club",
        // 远程弹射/投掷
        "rock slide", "rock-slide", "rock throw", "rock-throw", "stone edge", "stone-edge",
        "rock blast", "rock-blast", "smack down", "smack-down",
        "icicle spear", "icicle-spear", "icicle crash", "icicle-crash", "ice shard", "ice-shard",
        "triple axel", "triple-axel", "avalanche",
        // 精神/意念远程
        "psychic", "psybeam", "psyshock", "psycho cut", "psycho-cut", "psychic fangs", "psychic-fangs",
        "extrasensory", "expanding force", "expanding-force", "stored power", "stored-power",
        // 其他远程
        "thunderbolt", "thunder", "thunder shock", "thunder-shock", "volt switch", "volt-switch",
        "discharge", "parabolic charge", "parabolic-charge", "charge beam", "charge-beam",
        "flamethrower", "fire blast", "fire-blast", "heat wave", "overheat", "ember", "flame burst", "flame-burst",
        "hydro pump", "hydro-pump", "surf", "water gun", "water-gun", "water spout", "water-spout",
        "scald", "muddy water", "muddy-water", "octazooka", "origin pulse", "origin-pulse",
        "ice beam", "ice-beam", "blizzard", "freeze-dry", "freeze dry", "aurora beam", "aurora-beam",
        "solar beam", "solar-beam", "solar blade", "solar-blade", "leaf storm", "leaf-storm",
        "razor leaf", "razor-leaf", "magical leaf", "magical-leaf", "bullet seed", "bullet-seed",
        "dragon breath", "dragon-breath", "dragon rush", "dragon-rush", "dragon claw", "dragon-claw",
        "hyper beam", "hyper-beam", "tri attack", "tri-attack", "swift", "mud shot", "mud-shot",
        "mud bomb", "mud-bomb", "venoshock", "acid spray", "acid-spray", "clear smog", "clear-smog",
        "doom desire", "doom-desire", "future sight", "future-sight", "dream eater", "dream-eater",
        "night shade", "night-shade", "hex", "shadow sneak", "shadow-sneak", "phantom force", "phantom-force",
        "iron head", "iron-head", "iron tail", "iron-tail", "steel wing", "steel-wing",
        "gyro ball", "gyro-ball", "heavy slam", "heavy-slam", "heat crash", "heat-crash",
        "body press", "body-press", "power whip", "power-whip", "vine whip", "vine-whip",
        "grass knot", "grass-knot", "razor shell", "razor-shell", "sacred sword", "sacred-sword",
        "secret sword", "secret-sword", "leaf blade", "leaf-blade", "x-scissor", "night slash", "night-slash"
    );

    // ==== 半无敌状态命中招式 ====
    private static final Set<String> SEMI_INVULNERABLE_HITTERS = Set.of(
        "gust", "twister", "thunder", "hurricane", "smack-down", "smack down",
        "thousand-arrows", "thousand arrows", "sky-uppercut", "sky uppercut"
    );
    private static final Set<String> SEMI_INVULNERABLE_GROUND_HITTERS = Set.of(
        "earthquake", "magnitude", "fissure"
    );
    private static final Set<String> SEMI_INVULNERABLE_WATER_HITTERS = Set.of(
        "surf", "whirlpool"
    );

    /**
     * 检查是否为保护类招式
     */
    public static boolean isProtect(Map<String, Object> move) {
        return matchesAny(move, PROTECT_MOVES);
    }

    public static boolean isDetect(Map<String, Object> move) {
        return matchesAny(move, Set.of("detect"));
    }

    public static boolean isWideGuard(Map<String, Object> move) {
        return matchesAny(move, WIDE_GUARD_MOVES);
    }

    public static boolean isQuickGuard(Map<String, Object> move) {
        return matchesAny(move, QUICK_GUARD_MOVES);
    }

    public static boolean isProtectionMove(Map<String, Object> move) {
        return isProtect(move) || isDetect(move) || isWideGuard(move) || isQuickGuard(move);
    }

    public static boolean isEndure(Map<String, Object> move) {
        return matchesAny(move, ENDURE_MOVES);
    }

    public static boolean isHaze(Map<String, Object> move) {
        return matchesAny(move, HAZE_MOVES);
    }

    public static boolean isHealBell(Map<String, Object> move) { return matchesAny(move, HEAL_BELL_MOVES); }
    public static boolean isAromatherapy(Map<String, Object> move) { return matchesAny(move, AROMATHERAPY_MOVES); }
    public static boolean isRefresh(Map<String, Object> move) { return matchesAny(move, REFRESH_MOVES); }
    public static boolean isThroatChop(Map<String, Object> move) { return matchesAny(move, THROAT_CHOP_MOVES); }
    public static boolean isSmackDown(Map<String, Object> move) { return matchesAny(move, SMACK_DOWN_MOVES); }
    public static boolean isOctolock(Map<String, Object> move) { return matchesAny(move, OCTOLOCK_MOVES); }
    public static boolean isJawLock(Map<String, Object> move) { return matchesAny(move, JAW_LOCK_MOVES); }
    public static boolean isNoRetreat(Map<String, Object> move) { return matchesAny(move, NO_RETREAT_MOVES); }
    public static boolean isTarShot(Map<String, Object> move) { return matchesAny(move, TAR_SHOT_MOVES); }
    public static boolean isWorrySeed(Map<String, Object> move) { return matchesAny(move, WORRY_SEED_MOVES); }
    public static boolean isGastroAcid(Map<String, Object> move) { return matchesAny(move, GASTRO_ACID_MOVES); }
    public static boolean isHeartSwap(Map<String, Object> move) { return matchesAny(move, HEART_SWAP_MOVES); }
    public static boolean isPsychoShift(Map<String, Object> move) { return matchesAny(move, PSYCHO_SHIFT_MOVES); }
    public static boolean isSleepTalk(Map<String, Object> move) { return matchesAny(move, SLEEP_TALK_MOVES); }
    public static boolean isSnore(Map<String, Object> move) { return matchesAny(move, SNORE_MOVES); }
    public static boolean isNightmare(Map<String, Object> move) { return matchesAny(move, NIGHTMARE_MOVES); }
    public static boolean isDreamEater(Map<String, Object> move) { return matchesAny(move, DREAM_EATER_MOVES); }

    public static boolean isDestinyBond(Map<String, Object> move) {
        return matchesAny(move, DESTINY_BOND_MOVES);
    }

    public static boolean isPainSplit(Map<String, Object> move) {
        return matchesAny(move, PAIN_SPLIT_MOVES);
    }

    /**
     * 检查是否为引导类招式
     */
    public static boolean isRedirectionMove(Map<String, Object> move) {
        return matchesAny(move, REDIRECTION_MOVES);
    }

    /**
     * 检查是否为辅助类招式
     */
    public static boolean isHelpingHand(Map<String, Object> move) {
        return matchesAny(move, HELPING_HAND_MOVES);
    }

    public static boolean isAllySwitch(Map<String, Object> move) {
        return matchesAny(move, ALLY_SWITCH_MOVES);
    }

    /**
     * 检查是否为场地效果类招式
     */
    public static boolean isTailwind(Map<String, Object> move) {
        return matchesAny(move, TAILWIND_MOVES);
    }

    public static boolean isTrickRoom(Map<String, Object> move) {
        return matchesAny(move, TRICK_ROOM_MOVES);
    }

    /**
     * 检查是否为天气类招式
     */
    public static boolean isRainDance(Map<String, Object> move) {
        return matchesAny(move, RAIN_MOVES);
    }

    public static boolean isSunnyDay(Map<String, Object> move) {
        return matchesAny(move, SUN_MOVES);
    }

    public static boolean isSandstorm(Map<String, Object> move) {
        return matchesAny(move, SAND_MOVES);
    }

    public static boolean isSnowWeather(Map<String, Object> move) {
        return matchesAny(move, SNOW_MOVES);
    }

    /**
     * 检查是否为地形类招式
     */
    public static boolean isElectricTerrain(Map<String, Object> move) {
        return matchesAny(move, ELECTRIC_TERRAIN_MOVES);
    }

    public static boolean isPsychicTerrain(Map<String, Object> move) {
        return matchesAny(move, PSYCHIC_TERRAIN_MOVES);
    }

    public static boolean isGrassyTerrain(Map<String, Object> move) {
        return matchesAny(move, GRASSY_TERRAIN_MOVES);
    }

    public static boolean isMistyTerrain(Map<String, Object> move) {
        return matchesAny(move, MISTY_TERRAIN_MOVES);
    }

    /**
     * 检查是否为屏风类招式
     */
    public static boolean isReflect(Map<String, Object> move) {
        return matchesAny(move, REFLECT_MOVES);
    }

    public static boolean isLightScreen(Map<String, Object> move) {
        return matchesAny(move, LIGHT_SCREEN_MOVES);
    }

    public static boolean isAuroraVeil(Map<String, Object> move) {
        return matchesAny(move, AURORA_VEIL_MOVES);
    }

    public static boolean isSafeguard(Map<String, Object> move) {
        return matchesAny(move, SAFEGUARD_MOVES);
    }

    /**
     * 检查是否为状态异常类招式
     */
    public static boolean isThunderWave(Map<String, Object> move) {
        return matchesAny(move, THUNDER_WAVE_MOVES);
    }

    public static boolean isWillOWisp(Map<String, Object> move) {
        return matchesAny(move, WILL_O_WISP_MOVES);
    }

    public static boolean isToxic(Map<String, Object> move) {
        return matchesAny(move, TOXIC_MOVES);
    }

    public static boolean isPoisonPowder(Map<String, Object> move) {
        return matchesAny(move, POISON_POWDER_MOVES);
    }

    public static boolean isSpore(Map<String, Object> move) {
        return matchesAny(move, SPORE_MOVES);
    }

    public static boolean isYawn(Map<String, Object> move) {
        return matchesAny(move, YAWN_MOVES);
    }

    public static boolean isConfuseRay(Map<String, Object> move) {
        return matchesAny(move, CONFUSE_RAY_MOVES);
    }

    /**
     * 检查是否为封锁类招式
     */
    public static boolean isTaunt(Map<String, Object> move) {
        return matchesAny(move, TAUNT_MOVES);
    }

    public static boolean isEncore(Map<String, Object> move) {
        return matchesAny(move, ENCORE_MOVES);
    }

    public static boolean isDisable(Map<String, Object> move) {
        return matchesAny(move, DISABLE_MOVES);
    }

    public static boolean isTorment(Map<String, Object> move) {
        return matchesAny(move, TORMENT_MOVES);
    }

    public static boolean isHealBlock(Map<String, Object> move) {
        return matchesAny(move, HEAL_BLOCK_MOVES);
    }

    public static boolean isTrappingMove(Map<String, Object> move) {
        return matchesAny(move, TRAPPING_MOVES);
    }

    public static boolean isBindingMove(Map<String, Object> move) {
        return matchesAny(move, BINDING_MOVES);
    }

    /**
     * 检查是否为先制攻击类招式
     */
    public static boolean isFakeOut(Map<String, Object> move) {
        return matchesAny(move, FAKE_OUT_MOVES);
    }

    public static boolean isSuckerPunch(Map<String, Object> move) {
        return matchesAny(move, SUCKER_PUNCH_MOVES);
    }

    public static boolean isFeint(Map<String, Object> move) {
        return matchesAny(move, FEINT_MOVES);
    }

    /**
     * 检查是否为速度控制类招式
     */
    public static boolean isIcyWind(Map<String, Object> move) {
        return matchesAny(move, ICY_WIND_MOVES);
    }

    public static boolean isElectroweb(Map<String, Object> move) {
        return matchesAny(move, ELECTROWEB_MOVES);
    }

    public static boolean isSnarl(Map<String, Object> move) {
        return matchesAny(move, SNARL_MOVES);
    }

    public static boolean isFakeTears(Map<String, Object> move) {
        return matchesAny(move, FAKE_TEARS_MOVES);
    }

    /**
     * 检查是否为轮换类招式
     */
    public static boolean isUTurn(Map<String, Object> move) {
        return matchesAny(move, U_TURN_MOVES);
    }

    public static boolean isVoltSwitch(Map<String, Object> move) {
        return matchesAny(move, VOLT_SWITCH_MOVES);
    }

    public static boolean isFlipTurn(Map<String, Object> move) {
        return matchesAny(move, FLIP_TURN_MOVES);
    }

    public static boolean isPartingShot(Map<String, Object> move) {
        return matchesAny(move, PARTING_SHOT_MOVES);
    }

    public static boolean isPivotSwitchMove(Map<String, Object> move) {
        return isUTurn(move) || isVoltSwitch(move) || isFlipTurn(move);
    }

    /**
     * 检查是否为自我强化类招式
     */
    public static boolean isSwordsDance(Map<String, Object> move) {
        return matchesAny(move, SWORDS_DANCE_MOVES);
    }

    public static boolean isNastyPlot(Map<String, Object> move) {
        return matchesAny(move, NASTY_PLOT_MOVES);
    }

    public static boolean isDragonDance(Map<String, Object> move) {
        return matchesAny(move, DRAGON_DANCE_MOVES);
    }

    public static boolean isCalmMind(Map<String, Object> move) {
        return matchesAny(move, CALM_MIND_MOVES);
    }

    public static boolean isAgility(Map<String, Object> move) {
        return matchesAny(move, AGILITY_MOVES);
    }

    public static boolean isAutotomize(Map<String, Object> move) {
        return matchesAny(move, AUTOTOMIZE_MOVES);
    }

    public static boolean isBulkUp(Map<String, Object> move) {
        return matchesAny(move, BULK_UP_MOVES);
    }

    public static boolean isWorkUp(Map<String, Object> move) {
        return matchesAny(move, WORK_UP_MOVES);
    }

    public static boolean isQuiverDance(Map<String, Object> move) {
        return matchesAny(move, QUIVER_DANCE_MOVES);
    }

    public static boolean isCoil(Map<String, Object> move) {
        return matchesAny(move, COIL_MOVES);
    }

    public static boolean isShellSmash(Map<String, Object> move) {
        return matchesAny(move, SHELL_SMASH_MOVES);
    }

    /**
     * 检查是否为入场 hazards 类招式
     */
    public static boolean isStealthRock(Map<String, Object> move) {
        return matchesAny(move, STEALTH_ROCK_MOVES);
    }

    public static boolean isSpikes(Map<String, Object> move) {
        return matchesAny(move, SPIKES_MOVES);
    }

    public static boolean isToxicSpikes(Map<String, Object> move) {
        return matchesAny(move, TOXIC_SPIKES_MOVES);
    }

    public static boolean isStickyWeb(Map<String, Object> move) {
        return matchesAny(move, STICKY_WEB_MOVES);
    }

    public static boolean isRapidSpin(Map<String, Object> move) {
        return matchesAny(move, RAPID_SPIN_MOVES);
    }

    public static boolean isDefog(Map<String, Object> move) {
        return matchesAny(move, DEFOG_MOVES);
    }

    /**
     * 检查是否为回复类招式
     */
    public static boolean isRecover(Map<String, Object> move) {
        return matchesAny(move, RECOVER_MOVES);
    }

    public static boolean isRoost(Map<String, Object> move) {
        return matchesAny(move, ROOST_MOVES);
    }

    public static boolean isRest(Map<String, Object> move) {
        return matchesAny(move, REST_MOVES);
    }

    public static boolean isSoftBoiled(Map<String, Object> move) {
        return matchesAny(move, SOFT_BOILED_MOVES);
    }

    public static boolean isMilkDrink(Map<String, Object> move) {
        return matchesAny(move, MILK_DRINK_MOVES);
    }

    public static boolean isSynthesis(Map<String, Object> move) {
        return matchesAny(move, SYNTHESIS_MOVES);
    }

    public static boolean isMoonlight(Map<String, Object> move) {
        return matchesAny(move, MOONLIGHT_MOVES);
    }

    public static boolean isMorningSun(Map<String, Object> move) {
        return matchesAny(move, MORNING_SUN_MOVES);
    }

    /**
     * 检查是否为蓄力类招式
     */
    public static boolean isChargeMove(Map<String, Object> move) {
        return matchesAny(move, CHARGE_MOVES);
    }

    /** 判断是否为蓄力期间半无敌的招式（飞空/挖洞/潜水/弹跳/暗影潜袭等） */
    public static boolean isSemiInvulnerableChargeMove(Map<String, Object> move) {
        return matchesAny(move, SEMI_INVULNERABLE_CHARGE_MOVES);
    }

    /** 判断招式能否命中半无敌状态的目标 */
    public static boolean canHitSemiInvulnerable(Map<String, Object> move) {
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        String name = String.valueOf(move.get("name")).toLowerCase();
        // Gust/Twister/Thunder/Hurricane/Smack Down/Thousand Arrows/Sky Uppercut → 命中飞行/弹跳
        if (SEMI_INVULNERABLE_HITTERS.contains(nameEn) || SEMI_INVULNERABLE_HITTERS.contains(name)) {
            return true;
        }
        // Earthquake/Magnitude/Fissure → 命中挖洞
        if (SEMI_INVULNERABLE_GROUND_HITTERS.contains(nameEn) || SEMI_INVULNERABLE_GROUND_HITTERS.contains(name)) {
            return true;
        }
        // Surf/Whirlpool → 命中潜水
        if (SEMI_INVULNERABLE_WATER_HITTERS.contains(nameEn) || SEMI_INVULNERABLE_WATER_HITTERS.contains(name)) {
            return true;
        }
        return false;
    }

    /**
     * 检查是否为硬直类招式
     */
    public static boolean isRechargeMove(Map<String, Object> move) {
        return matchesAny(move, RECHARGE_MOVES);
    }

    /**
     * 检查是否为太晶爆发
     */
    public static boolean isTeraBlast(Map<String, Object> move) {
        return matchesAny(move, TERA_BLAST_MOVES);
    }

    /**
     * 检查是否为击落类招式
     */
    public static boolean isKnockOff(Map<String, Object> move) {
        return matchesAny(move, KNOCK_OFF_MOVES);
    }

    public static boolean isBodyPress(Map<String, Object> move) {
        return matchesAny(move, BODY_PRESS_MOVES);
    }

    public static boolean isFoulPlay(Map<String, Object> move) {
        return matchesAny(move, FOUL_PLAY_MOVES);
    }

    public static boolean isElectroBall(Map<String, Object> move) {
        return matchesAny(move, ELECTRO_BALL_MOVES);
    }

    public static boolean isGyroBall(Map<String, Object> move) {
        return matchesAny(move, GYRO_BALL_MOVES);
    }

    public static boolean isSpeedBasedPowerMove(Map<String, Object> move) {
        return isElectroBall(move) || isGyroBall(move);
    }

    public static boolean isWeightBasedMove(Map<String, Object> move) {
        return matchesAny(move, WEIGHT_BASED_MOVES);
    }

    public static boolean isHpRatioMove(Map<String, Object> move) {
        return matchesAny(move, HP_RATIO_MOVES);
    }

    public static boolean isReversalMove(Map<String, Object> move) {
        return matchesAny(move, REVERSAL_MOVES);
    }

    public static boolean isPayback(Map<String, Object> move) {
        return matchesAny(move, PAYBACK_MOVES);
    }

    public static boolean isAvalanche(Map<String, Object> move) {
        return matchesAny(move, AVALANCHE_MOVES);
    }

    public static boolean isSacredSword(Map<String, Object> move) {
        return matchesAny(move, SACRED_SWORD_MOVES);
    }

    public static boolean isStrengthSap(Map<String, Object> move) {
        return matchesAny(move, STRENGTH_SAP_MOVES);
    }

    public static boolean isDrainingKiss(Map<String, Object> move) {
        return matchesAny(move, DRAINING_KISS_MOVES);
    }

    public static boolean isGuardSplit(Map<String, Object> move) {
        return matchesAny(move, GUARD_SPLIT_MOVES);
    }

    public static boolean isPowerSplit(Map<String, Object> move) {
        return matchesAny(move, POWER_SPLIT_MOVES);
    }

    public static boolean isGuardSwap(Map<String, Object> move) {
        return matchesAny(move, GUARD_SWAP_MOVES);
    }

    public static boolean isPowerSwap(Map<String, Object> move) {
        return matchesAny(move, POWER_SWAP_MOVES);
    }

    public static boolean isWeatherBall(Map<String, Object> move) {
        return matchesAny(move, WEATHER_BALL_MOVES);
    }

    public static boolean isTerrainPulse(Map<String, Object> move) {
        return matchesAny(move, TERRAIN_PULSE_MOVES);
    }

    public static boolean isEndeavor(Map<String, Object> move) {
        return matchesAny(move, ENDEAVOR_MOVES);
    }

    public static boolean isLastResort(Map<String, Object> move) {
        return matchesAny(move, LAST_RESORT_MOVES);
    }

    public static boolean isJudgment(Map<String, Object> move) {
        return matchesAny(move, JUDGMENT_MOVES);
    }

    public static boolean isMultiAttack(Map<String, Object> move) {
        return matchesAny(move, MULTI_ATTACK_MOVES);
    }

    public static boolean isPhotonGeyser(Map<String, Object> move) {
        return matchesAny(move, PHOTON_GEYSER_MOVES);
    }

    public static boolean isMoongeistBeam(Map<String, Object> move) {
        return matchesAny(move, MOONGEIST_BEAM_MOVES);
    }

    /** 无视防御方特性的招式（破格类） */
    public static boolean isUnignorableMove(Map<String, Object> move) {
        return isMoongeistBeam(move) || isPhotonGeyser(move);
    }

    // === 气象球/地形球 ===
    private static final Set<String> WEATHER_BALL_MOVES = Set.of(
        "weather ball", "weather-ball"
    );
    private static final Set<String> TERRAIN_PULSE_MOVES = Set.of(
        "terrain pulse", "terrain-pulse"
    );

    // === 垂死挣扎 ===
    private static final Set<String> ENDEAVOR_MOVES = Set.of("endeavor");

    // === 最终手段 ===
    private static final Set<String> LAST_RESORT_MOVES = Set.of(
        "last resort", "last-resort"
    );

    // === 制裁光砾/多属性攻击 ===
    private static final Set<String> JUDGMENT_MOVES = Set.of("judgment");
    private static final Set<String> MULTI_ATTACK_MOVES = Set.of(
        "multi-attack", "multi attack"
    );

    // === 光子喷涌/暗影之光 ===
    private static final Set<String> PHOTON_GEYSER_MOVES = Set.of(
        "photon geyser", "photon-geyser"
    );
    private static final Set<String> MOONGEIST_BEAM_MOVES = Set.of(
        "moongeist beam", "moongeist-beam"
    );

    public static boolean isFutureSight(Map<String, Object> move) {
        return matchesAny(move, FUTURE_SIGHT_MOVES);
    }

    public static boolean isDoomDesire(Map<String, Object> move) {
        return matchesAny(move, DOOM_DESIRE_MOVES);
    }

    public static boolean isDelayedAttackMove(Map<String, Object> move) {
        return isFutureSight(move) || isDoomDesire(move);
    }

    public static boolean isBatonPass(Map<String, Object> move) {
        return matchesAny(move, BATON_PASS_MOVES);
    }

    public static boolean isCounter(Map<String, Object> move) { return matchesAny(move, COUNTER_MOVES); }
    public static boolean isMirrorCoat(Map<String, Object> move) { return matchesAny(move, MIRROR_COAT_MOVES); }
    public static boolean isMetalBurst(Map<String, Object> move) { return matchesAny(move, METAL_BURST_MOVES); }
    public static boolean isReverseDamageMove(Map<String, Object> move) {
        return isCounter(move) || isMirrorCoat(move) || isMetalBurst(move);
    }

    public static boolean isGravity(Map<String, Object> move) { return matchesAny(move, GRAVITY_MOVES); }
    public static boolean isMagicRoom(Map<String, Object> move) { return matchesAny(move, MAGIC_ROOM_MOVES); }
    public static boolean isWonderRoom(Map<String, Object> move) { return matchesAny(move, WONDER_ROOM_MOVES); }

    public static boolean isThunder(Map<String, Object> move) {
        return matchesAny(move, THUNDER_MOVES);
    }

    public static boolean isHurricane(Map<String, Object> move) {
        return matchesAny(move, HURRICANE_MOVES);
    }

    public static boolean isBlizzard(Map<String, Object> move) {
        return matchesAny(move, BLIZZARD_MOVES);
    }

    public static boolean isPunchingMove(Map<String, Object> move) { return matchesAny(move, PUNCHING_MOVES); }
    public static boolean isBitingMove(Map<String, Object> move) { return matchesAny(move, BITING_MOVES); }
    public static boolean isPulseMove(Map<String, Object> move) { return matchesAny(move, PULSE_MOVES); }
    public static boolean isSoundMove(Map<String, Object> move) { return matchesAny(move, SOUND_MOVES); }
    public static boolean isSlicingMove(Map<String, Object> move) { return matchesAny(move, SLICING_MOVES); }
    public static boolean isWindMove(Map<String, Object> move) { return matchesAny(move, WIND_MOVES); }
    public static boolean hasRecoil(Map<String, Object> move) { return matchesAny(move, RECOIL_MOVES); }

    /**
     * 检查是否为接触技能（Showdown 规则）。
     * 优先读显式 contact 标志；否则已知非接触名单判定为非接触；
     * 兜底：变化技与无威力招非接触，物理/特殊攻击招默认接触（与 PS 一致，绝大多数攻击招是接触）。
     */
    public static boolean isContactMove(Map<String, Object> move) {
        Object contact = move.get("contact");
        if (contact instanceof Boolean bool) return bool;
        String nameEn = String.valueOf(move.get("name_en")).toLowerCase();
        if (NON_CONTACT_PHYSICAL_MOVES.contains(nameEn)) {
            return false;
        }
        // 兜底：变化技/无威力招非接触；有威力的攻击招默认接触
        int power = BattleUtils.toInt(move.get("power"), 0);
        int damageClassId = BattleUtils.toInt(move.get("damage_class_id"), 0);
        if (power <= 0 || damageClassId == 3) {
            return false;
        }
        return true;
    }

    public static boolean isGMaxMove(Map<String, Object> move) {
        String name = String.valueOf(move.get("name_en")).toLowerCase();
        return name.startsWith("g-max") || name.startsWith("gmax");
    }

    public static boolean isTrickMove(Map<String, Object> move) { return matchesAny(move, TRICK_MOVES); }

    public static boolean isLeechSeed(Map<String, Object> move) {
        return matchesAny(move, LEECH_SEED_MOVES);
    }

    public static boolean isSubstitute(Map<String, Object> move) {
        return matchesAny(move, SUBSTITUTE_MOVES);
    }

    public static boolean isAttract(Map<String, Object> move) {
        return matchesAny(move, ATTRACT_MOVES);
    }

    public static boolean isPerishSong(Map<String, Object> move) {
        return matchesAny(move, PERISH_SONG_MOVES);
    }

    /**
     * 检查是否为变化技能
     */
    public static boolean isStatusMove(Map<String, Object> move) {
        int damageClassId = BattleUtils.toInt(move.get("damage_class_id"), 0);
        int power = BattleUtils.toInt(move.get("power"), 0);
        return damageClassId == 3 || power == 0;
    }

    /**
     * 检查是否为群体技能 (双打中攻击多个目标)
     * target_id: 9=all other active, 11=all opponents, 12=all active, 13=all adjacent, 14=all active(field-wide)
     * target_id 10 = single opponent (NOT spread)
     */
    public static boolean isSpreadMove(Map<String, Object> move) {
        int targetId = BattleUtils.toInt(move.get("target_id"), 10);
        return targetId == 9 || targetId == 11 || targetId == 12 || targetId == 13 || targetId == 14;
    }

    /**
     * 检查是否为回复技能
     */
    public static boolean isHealingMove(Map<String, Object> move) {
        return BattleUtils.toInt(move.get("healing"), 0) > 0;
    }

    /**
     * 通用匹配方法
     */
    private static boolean matchesAny(Map<String, Object> move, Set<String> patterns) {
        String nameEn = BattleUtils.toString(move.get("name_en"), "");
        return BattleUtils.matchesMovePattern(nameEn, patterns.toArray(new String[0]));
    }
}
