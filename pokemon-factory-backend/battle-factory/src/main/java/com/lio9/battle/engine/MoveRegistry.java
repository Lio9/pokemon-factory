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
        "protect", "detect"
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
        "geomancy"
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

    // === 击落类招式 ===
    private static final Set<String> KNOCK_OFF_MOVES = Set.of("knock off", "knock-off");

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
        "snore", "perish song", "perish-song"
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
     * 检查是否为接触技能（通过 flags 或 power 判断）
     */
    public static boolean isContactMove(Map<String, Object> move) {
        Object contact = move.get("contact");
        if (contact instanceof Boolean bool) return bool;
        int power = BattleUtils.toInt(move.get("power"), 0);
        return power > 0;
    }

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
     */
    public static boolean isSpreadMove(Map<String, Object> move) {
        int targetId = BattleUtils.toInt(move.get("target_id"), 10);
        // Target IDs: 9=all adjacent foes, 10=all adjacent, 11=random opponent, etc.
        return targetId == 9 || targetId == 10 || targetId == 11 || targetId == 12 || targetId == 13 || targetId == 14;
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
