package com.lio9.pokedex.util;



import java.util.*;

/**
 * 特性效果配置
 * 定义所有影响伤害计算的特性的效果
 * 
 * 特性ID参考: csv/abilities.csv
 */
public class AbilityEffects {
    
    /**
     * 特性效果类型枚举
     */
    public enum EffectType {
        // 攻击相关
        ATTACK_MULTIPLIER,          // 攻击倍率
        SP_ATTACK_MULTIPLIER,       // 特攻倍率
        POWER_MULTIPLIER,           // 威力倍率
        STAB_MULTIPLIER,            // 本系加成倍率
        CRITICAL_RATE_BOOST,        // 暴击率提升
        
        // 防御相关
        DAMAGE_MULTIPLIER,          // 受伤倍率
        DEFENSE_MULTIPLIER,         // 防御倍率
        SP_DEFENSE_MULTIPLIER,      // 特防倍率
        IMMUNITY,                   // 免疫某属性
        
        // 特殊效果
        TYPE_IMMUNITY,              // 属性免疫
        TYPE_ABSORB,                // 属性吸收（回血）
        DAMAGE_REDUCTION_HP,        // 基于HP的伤害减免
        IGNORE_DEFENSE_BOOST,       // 忽略防御提升
        IGNORE_ATTACK_DROP,         // 忽略攻击下降
        PREVENT_CRIT,               // 免疫暴击
        RECOIL_IMMUNITY,            // 免疫反伤
        GROUND_IMMUNITY             // 免疫地面
    }
    
    /**
     * 特性效果定义
     */
    public static class AbilityEffect {
        public int abilityId;
        public String abilityName;
        public String abilityIdentifier;
        public EffectType effectType;
        public double value;
        public List<Integer> typeIds;          // 作用的属性ID列表
        public String condition;               // 触发条件
        public String description;
        public boolean affectsAttacker;        // 是否影响攻击方
        public boolean affectsDefender;        // 是否影响防御方
        
        public AbilityEffect(int abilityId, String abilityName, String abilityIdentifier,
                           EffectType effectType, double value, String description) {
            this.abilityId = abilityId;
            this.abilityName = abilityName;
            this.abilityIdentifier = abilityIdentifier;
            this.effectType = effectType;
            this.value = value;
            this.description = description;
            this.typeIds = new ArrayList<>();
            this.affectsAttacker = false;
            this.affectsDefender = false;
        }
        
        public AbilityEffect withTypes(Integer... types) {
            this.typeIds = Arrays.asList(types);
            return this;
        }
        
        public AbilityEffect withCondition(String condition) {
            this.condition = condition;
            return this;
        }
        
        public AbilityEffect attacker() {
            this.affectsAttacker = true;
            return this;
        }
        
        public AbilityEffect defender() {
            this.affectsDefender = true;
            return this;
        }
    }
    
    /**
     * 所有特性效果映射表
     * Key: 特性ID, Value: 效果列表（一个特性可能有多个效果）
     */
    private static final Map<Integer, List<AbilityEffect>> ABILITY_EFFECTS = new HashMap<>();
    
    // 属性ID常量
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_FIGHTING = 2;
    public static final int TYPE_FLYING = 3;
    public static final int TYPE_POISON = 4;
    public static final int TYPE_GROUND = 5;
    public static final int TYPE_ROCK = 6;
    public static final int TYPE_BUG = 7;
    public static final int TYPE_GHOST = 8;
    public static final int TYPE_STEEL = 9;
    public static final int TYPE_FIRE = 10;
    public static final int TYPE_WATER = 11;
    public static final int TYPE_GRASS = 12;
    public static final int TYPE_ELECTRIC = 13;
    public static final int TYPE_PSYCHIC = 14;
    public static final int TYPE_ICE = 15;
    public static final int TYPE_DRAGON = 16;
    public static final int TYPE_DARK = 17;
    public static final int TYPE_FAIRY = 18;
    
    static {
        // ==================== 攻击提升类特性 ====================
        
        // 37: Huge Power / 大力士 - 物理攻击翻倍
        addEffect(new AbilityEffect(37, "大力士", "huge-power",
            EffectType.ATTACK_MULTIPLIER, 2.0, "物理攻击翻倍").attacker());
        
        // 38: Pure Power / 瑜伽之力 - 物理攻击翻倍
        addEffect(new AbilityEffect(38, "瑜伽之力", "pure-power",
            EffectType.ATTACK_MULTIPLIER, 2.0, "物理攻击翻倍").attacker());
        
        // 55: Hustle / 活力 - 物理攻击提升50%，命中率降低20%
        addEffect(new AbilityEffect(55, "活力", "hustle",
            EffectType.ATTACK_MULTIPLIER, 1.5, "物理攻击提升50%").attacker());
        
        // 66: Overgrow / 茂盛 - HP≤1/3时草系技能威力×1.5
        addEffect(new AbilityEffect(66, "茂盛", "overgrow",
            EffectType.POWER_MULTIPLIER, 1.5, "HP≤1/3时草系技能威力提升50%")
            .withTypes(TYPE_GRASS).withCondition("hp_le_33").attacker());
        
        // 67: Blaze / 猛火 - HP≤1/3时火系技能威力×1.5
        addEffect(new AbilityEffect(67, "猛火", "blaze",
            EffectType.POWER_MULTIPLIER, 1.5, "HP≤1/3时火系技能威力提升50%")
            .withTypes(TYPE_FIRE).withCondition("hp_le_33").attacker());
        
        // 68: Torrent / 激流 - HP≤1/3时水系技能威力×1.5
        addEffect(new AbilityEffect(68, "激流", "torrent",
            EffectType.POWER_MULTIPLIER, 1.5, "HP≤1/3时水系技能威力提升50%")
            .withTypes(TYPE_WATER).withCondition("hp_le_33").attacker());
        
        // 69: Swarm / 虫之预感 - HP≤1/3时虫系技能威力×1.5
        addEffect(new AbilityEffect(69, "虫之预感", "swarm",
            EffectType.POWER_MULTIPLIER, 1.5, "HP≤1/3时虫系技能威力提升50%")
            .withTypes(TYPE_BUG).withCondition("hp_le_33").attacker());
        
        // 101: Technician / 技术员 - 威力≤60的技能威力×1.5
        addEffect(new AbilityEffect(101, "技术员", "technician",
            EffectType.POWER_MULTIPLIER, 1.5, "威力≤60的技能威力提升50%")
            .withCondition("power_le_60").attacker());
        
        // 91: Adaptability / 适应力 - 本系技能威力×2（而非×1.5）
        addEffect(new AbilityEffect(91, "适应力", "adaptability",
            EffectType.STAB_MULTIPLIER, 2.0, "本系加成变为2倍").attacker());
        
        // 62: Guts / 毅力 - 异常状态下物理攻击×1.5
        addEffect(new AbilityEffect(62, "毅力", "guts",
            EffectType.ATTACK_MULTIPLIER, 1.5, "异常状态下物理攻击提升50%")
            .withCondition("has_status").attacker());
        
        // 74: Solar Power / 太阳之力 - 晴天时特攻×1.5
        addEffect(new AbilityEffect(74, "太阳之力", "solar-power",
            EffectType.SP_ATTACK_MULTIPLIER, 1.5, "晴天时特攻提升50%")
            .withCondition("sunny").attacker());
        
        // 148: Analytic / 分析 - 后出手时威力×1.3
        addEffect(new AbilityEffect(148, "分析", "analytic",
            EffectType.POWER_MULTIPLIER, 1.3, "后出手时威力提升30%")
            .withCondition("moves_last").attacker());
        
        // 89: Iron Fist / 铁拳 - 拳类技能威力×1.2
        addEffect(new AbilityEffect(89, "铁拳", "iron-fist",
            EffectType.POWER_MULTIPLIER, 1.2, "拳类技能威力提升20%")
            .withCondition("punching_move").attacker());
        
        // 115: Sheer Force / 强行 - 有附加效果的技能威力×1.3
        addEffect(new AbilityEffect(115, "强行", "sheer-force",
            EffectType.POWER_MULTIPLIER, 1.3, "有附加效果的技能威力提升30%")
            .withCondition("has_secondary_effect").attacker());
        
        // 94: Solar Beam相关 - 晴天时日光束无需蓄力
        // (这是特殊机制，不在伤害计算中)
        
        // 97: Rivalry / 争强好胜
        addEffect(new AbilityEffect(97, "争强好胜", "rivalry",
            EffectType.POWER_MULTIPLIER, 1.25, "对同性对手威力提升25%")
            .withCondition("same_gender").attacker());
        addEffect(new AbilityEffect(97, "争强好胜", "rivalry",
            EffectType.POWER_MULTIPLIER, 0.75, "对异性对手威力降低25%")
            .withCondition("opposite_gender").attacker());
        
        // 119: Flare Boost / 引火 - 灼伤状态下特殊技能威力×1.5
        addEffect(new AbilityEffect(119, "引火", "flare-boost",
            EffectType.POWER_MULTIPLIER, 1.5, "灼伤时特殊技能威力提升50%")
            .withCondition("burned").attacker());
        
        // 124: Toxic Boost / 毒暴走 - 中毒状态下物理技能威力×1.5
        addEffect(new AbilityEffect(124, "毒暴走", "toxic-boost",
            EffectType.POWER_MULTIPLIER, 1.5, "中毒时物理技能威力提升50%")
            .withCondition("poisoned").attacker());
        
        // 138: Strong Jaw / 强壮之颚 - 咬类技能威力×1.5
        addEffect(new AbilityEffect(138, "强壮之颚", "strong-jaw",
            EffectType.POWER_MULTIPLIER, 1.5, "咬类技能威力提升50%")
            .withCondition("biting_move").attacker());
        
        // 139: Mega Launcher / 超级发射器 - 波动/波导类技能威力×1.5
        addEffect(new AbilityEffect(139, "超级发射器", "mega-launcher",
            EffectType.POWER_MULTIPLIER, 1.5, "波动类技能威力提升50%")
            .withCondition("pulse_move").attacker());
        
        // 157: Pixilate / 妖精皮肤 - 一般系技能变为妖精系且威力×1.2
        addEffect(new AbilityEffect(157, "妖精皮肤", "pixilate",
            EffectType.POWER_MULTIPLIER, 1.2, "一般系技能变为妖精系且威力提升20%")
            .withCondition("normal_move").attacker());
        
        // 164: Aerilate / 天空皮肤 - 一般系技能变为飞行系且威力×1.2
        addEffect(new AbilityEffect(164, "天空皮肤", "aerilate",
            EffectType.POWER_MULTIPLIER, 1.2, "一般系技能变为飞行系且威力提升20%")
            .withCondition("normal_move").attacker());
        
        // 165: Refrigerate / 冰冻皮肤 - 一般系技能变为冰系且威力×1.2
        addEffect(new AbilityEffect(165, "冰冻皮肤", "refrigerate",
            EffectType.POWER_MULTIPLIER, 1.2, "一般系技能变为冰系且威力提升20%")
            .withCondition("normal_move").attacker());
        
        // 166: Galvanize / 电气皮肤 - 一般系技能变为电系且威力×1.2
        addEffect(new AbilityEffect(166, "电气皮肤", "galvanize",
            EffectType.POWER_MULTIPLIER, 1.2, "一般系技能变为电系且威力提升20%")
            .withCondition("normal_move").attacker());
        
        // 178: Triage / 治愈之心 - 回复技能优先度+3（伤害计算不涉及）
        
        // 184: Steelworker / 钢能力者 - 钢系技能威力×1.5
        addEffect(new AbilityEffect(184, "钢能力者", "steelworker",
            EffectType.POWER_MULTIPLIER, 1.5, "钢系技能威力提升50%")
            .withTypes(TYPE_STEEL).attacker());
        
        // 185: Lingering Aroma / 粘着 - 接触类技能后让对方获得此特性
        
        // 200: Libero / 自由者 - 使用技能前变为该属性
        // 这是特殊机制，不影响伤害计算
        
        // 214: Punk Rock / 庞克摇滚 - 声音技能威力×1.3
        addEffect(new AbilityEffect(214, "庞克摇滚", "punk-rock",
            EffectType.POWER_MULTIPLIER, 1.3, "声音技能威力提升30%")
            .withCondition("sound_move").attacker());
        
        // 221: Transistor / 半导体 - 电系技能威力×1.5
        addEffect(new AbilityEffect(221, "半导体", "transistor",
            EffectType.POWER_MULTIPLIER, 1.5, "电系技能威力提升50%")
            .withTypes(TYPE_ELECTRIC).attacker());
        
        // 222: Dragon's Maw / 龙之颚 - 龙系技能威力×1.5
        addEffect(new AbilityEffect(222, "龙之颚", "dragons-maw",
            EffectType.POWER_MULTIPLIER, 1.5, "龙系技能威力提升50%")
            .withTypes(TYPE_DRAGON).attacker());
        
        // 244: Sharpness / 锋利 - 切割类技能威力×1.5
        addEffect(new AbilityEffect(244, "锋利", "sharpness",
            EffectType.POWER_MULTIPLIER, 1.5, "切割类技能威力提升50%")
            .withCondition("slicing_move").attacker());
        
        // ==================== 防御减免类特性 ====================
        
        // 47: Thick Fat / 厚脂肪 - 火系和冰系技能伤害×0.5
        addEffect(new AbilityEffect(47, "厚脂肪", "thick-fat",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "火系和冰系技能伤害减半")
            .withTypes(TYPE_FIRE, TYPE_ICE).defender());
        
        // 4: Battle Armor / 战斗盔甲 - 免疫暴击
        addEffect(new AbilityEffect(4, "战斗盔甲", "battle-armor",
            EffectType.PREVENT_CRIT, 1.0, "免疫暴击").defender());
        
        // 5: Sturdy / 结实 - 满血时不会被一击击杀（特殊机制）
        
        // 49: Shell Armor / 硬壳盔甲 - 免疫暴击
        addEffect(new AbilityEffect(49, "硬壳盔甲", "shell-armor",
            EffectType.PREVENT_CRIT, 1.0, "免疫暴击").defender());
        
        // 75: Light Metal / 轻金属 - 重量减半（影响特定技能）
        
        // 82: Dry Skin / 干燥皮肤 - 火系伤害×1.25，水系回血
        addEffect(new AbilityEffect(82, "干燥皮肤", "dry-skin",
            EffectType.DAMAGE_MULTIPLIER, 1.25, "火系技能伤害增加25%")
            .withTypes(TYPE_FIRE).defender());
        addEffect(new AbilityEffect(82, "干燥皮肤", "dry-skin",
            EffectType.TYPE_ABSORB, 0.25, "水系技能回复25%HP")
            .withTypes(TYPE_WATER).defender());
        
        // 85: Heatproof / 耐热 - 火系伤害×0.5
        addEffect(new AbilityEffect(85, "耐热", "heatproof",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "火系技能伤害减半")
            .withTypes(TYPE_FIRE).defender());
        
        // 99: Filter / 过滤 - 效果绝佳伤害×0.75
        addEffect(new AbilityEffect(99, "过滤", "filter",
            EffectType.DAMAGE_MULTIPLIER, 0.75, "效果绝佳伤害减少25%")
            .withCondition("super_effective").defender());
        
        // 108: Solid Rock / 坚硬岩石 - 效果绝佳伤害×0.75
        addEffect(new AbilityEffect(108, "坚硬岩石", "solid-rock",
            EffectType.DAMAGE_MULTIPLIER, 0.75, "效果绝佳伤害减少25%")
            .withCondition("super_effective").defender());
        
        // 105: Multiscale / 多重鳞片 - 满血时伤害×0.5
        addEffect(new AbilityEffect(105, "多重鳞片", "multiscale",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "满血时伤害减半")
            .withCondition("full_hp").defender());
        
        // 106: Multiscale / 多重鳞片（可能有不同ID）
        
        // 113: Friend Guard / 朋友防守 - 双打中队友受伤×0.75
        addEffect(new AbilityEffect(113, "朋友防守", "friend-guard",
            EffectType.DAMAGE_MULTIPLIER, 0.75, "双打中队友受伤减少25%")
            .withCondition("double_battle_ally").defender());
        
        // 118: Regenerator / 再生力 - 换下时回复1/3HP
        
        // 144: Wonder Skin / 奇迹皮肤 - 特殊技能命中率×0.5
        addEffect(new AbilityEffect(144, "奇迹皮肤", "wonder-skin",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "特殊技能伤害减半")
            .withCondition("special_move").defender());
        
        // 149: Imposter / 冒充者 - 上场时变身
        
        // 152: Big Pecks / 强壮胸肌 - 防御不会被降低
        // 这是状态效果，不在伤害计算中
        
        // 153: Contrary / 不顺从 - 能力等级变化反转
        // 这是特殊机制
        
        // 163: Bulletproof / 防弹 - 免疫球类/弹类技能
        addEffect(new AbilityEffect(163, "防弹", "bulletproof",
            EffectType.IMMUNITY, 1.0, "免疫球类/弹类技能")
            .withCondition("bullet_move").defender());
        
        // 167: Flower Veil / 花幕 - 草系队友能力不会被降低
        
        // 169: Fur Coat / 毛皮大衣 - 物理伤害×0.5
        addEffect(new AbilityEffect(169, "毛皮大衣", "fur-coat",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "物理伤害减半")
            .withCondition("physical_move").defender());
        
        // 171: Aura Break / 奥拉破除 - 光环特性效果反转
        
        // 191: Prism Armor / 棱镜装甲 - 效果绝佳伤害×0.75
        addEffect(new AbilityEffect(191, "棱镜装甲", "prism-armor",
            EffectType.DAMAGE_MULTIPLIER, 0.75, "效果绝佳伤害减少25%")
            .withCondition("super_effective").defender());
        
        // 212: Fluffy / 蓬松 - 接触类技能伤害×0.5，火系×2
        addEffect(new AbilityEffect(212, "蓬松", "fluffy",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "接触类技能伤害减半")
            .withCondition("contact_move").defender());
        addEffect(new AbilityEffect(212, "蓬松", "fluffy",
            EffectType.DAMAGE_MULTIPLIER, 2.0, "火系技能伤害翻倍")
            .withTypes(TYPE_FIRE).defender());
        
        // 227: Ice Face / 冰头 - 物理攻击后变身，免疫一次物理攻击
        // 这是特殊机制
        
        // 242: Ice Scales / 冰鳞 - 特殊伤害×0.5
        addEffect(new AbilityEffect(242, "冰鳞", "ice-scales",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "特殊伤害减半")
            .withCondition("special_move").defender());
        
        // ==================== 属性免疫类特性 ====================
        
        // 9: Static / 静电 - 接触类技能30%麻痹对手
        
        // 10: Volt Absorb / 蓄电 - 电系技能回血
        addEffect(new AbilityEffect(10, "蓄电", "volt-absorb",
            EffectType.TYPE_ABSORB, 0.25, "电系技能回复25%HP")
            .withTypes(TYPE_ELECTRIC).defender());
        addEffect(new AbilityEffect(10, "蓄电", "volt-absorb",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫电系技能")
            .withTypes(TYPE_ELECTRIC).defender());
        
        // 11: Water Absorb / 储水 - 水系技能回血
        addEffect(new AbilityEffect(11, "储水", "water-absorb",
            EffectType.TYPE_ABSORB, 0.25, "水系技能回复25%HP")
            .withTypes(TYPE_WATER).defender());
        addEffect(new AbilityEffect(11, "储水", "water-absorb",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫水系技能")
            .withTypes(TYPE_WATER).defender());
        
        // 18: Flash Fire / 引火 - 火系技能免疫并提升火系威力
        addEffect(new AbilityEffect(18, "引火", "flash-fire",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫火系技能")
            .withTypes(TYPE_FIRE).defender());
        addEffect(new AbilityEffect(18, "引火", "flash-fire",
            EffectType.POWER_MULTIPLIER, 1.5, "被火系攻击后火系技能威力提升50%")
            .withTypes(TYPE_FIRE).withCondition("flash_fire_activated").attacker());
        
        // 24: Wonder Guard / 奇迹守护 - 只有效果绝佳才能命中
        // 这是特殊机制，需要单独处理
        
        // 26: Levitate / 漂浮 - 免疫地面系
        addEffect(new AbilityEffect(26, "漂浮", "levitate",
            EffectType.GROUND_IMMUNITY, 1.0, "免疫地面系技能")
            .withTypes(TYPE_GROUND).defender());
        
        // 29: Clear Body / 晶莹之躯 - 能力不会被降低
        
        // 34: Chlorophyll / 叶绿素 - 晴天速度翻倍
        
        // 33: Swift Swim / 轻快 - 雨天速度翻倍
        
        // 43: Soundproof / 隔音 - 免疫声音技能
        addEffect(new AbilityEffect(43, "隔音", "soundproof",
            EffectType.IMMUNITY, 1.0, "免疫声音技能")
            .withCondition("sound_move").defender());
        
        // 71: Storm Drain / 引水 - 水系技能引向自己
        addEffect(new AbilityEffect(71, "引水", "storm-drain",
            EffectType.TYPE_ABSORB, 1.0, "水系技能提升特攻")
            .withTypes(TYPE_WATER).defender());
        addEffect(new AbilityEffect(71, "引水", "storm-drain",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫水系技能")
            .withTypes(TYPE_WATER).defender());
        
        // 78: Lightning Rod / 避雷针 - 电系技能引向自己
        addEffect(new AbilityEffect(78, "避雷针", "lightning-rod",
            EffectType.TYPE_ABSORB, 1.0, "电系技能提升特攻")
            .withTypes(TYPE_ELECTRIC).defender());
        addEffect(new AbilityEffect(78, "避雷针", "lightning-rod",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫电系技能")
            .withTypes(TYPE_ELECTRIC).defender());
        
        // 84: Sap Sipper / 食草 - 草系技能免疫并提升攻击
        addEffect(new AbilityEffect(84, "食草", "sap-sipper",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫草系技能")
            .withTypes(TYPE_GRASS).defender());
        
        // 120: Motor Drive / 电气引擎 - 电系技能免疫并提升速度
        addEffect(new AbilityEffect(120, "电气引擎", "motor-drive",
            EffectType.TYPE_IMMUNITY, 1.0, "免疫电系技能")
            .withTypes(TYPE_ELECTRIC).defender());
        
        // ==================== 其他重要特性 ====================
        
        // 7: Limber / 柔软 - 免疫麻痹
        
        // 15: Insomnia / 不眠 - 免疫睡眠
        
        // 17: Immunity / 免疫 - 免疫中毒
        
        // 41: Water Veil / 水之掩护 - 免疫灼伤
        
        // 53: Quick Feet / 飞毛腿 - 异常状态速度×1.5
        
        // 56: Color Change / 变色 - 受攻击后变为该属性
        
        // 60: Slow Start / 慢启动 - 前五回合物攻和速度减半
        addEffect(new AbilityEffect(60, "慢启动", "slow-start",
            EffectType.ATTACK_MULTIPLIER, 0.5, "前五回合物攻减半")
            .withCondition("slow_start_active").attacker());
        
        // 61: Defeatist / 失败主义者 - HP≤50%时攻防减半
        addEffect(new AbilityEffect(61, "失败主义者", "defeatist",
            EffectType.ATTACK_MULTIPLIER, 0.5, "HP≤50%时攻击减半")
            .withCondition("hp_le_50").attacker());
        addEffect(new AbilityEffect(61, "失败主义者", "defeatist",
            EffectType.SP_ATTACK_MULTIPLIER, 0.5, "HP≤50%时特攻减半")
            .withCondition("hp_le_50").attacker());
        
        // 70: Marvel Scale / 奇异鳞片 - 异常状态防御×1.5
        addEffect(new AbilityEffect(70, "奇异鳞片", "marvel-scale",
            EffectType.DEFENSE_MULTIPLIER, 1.5, "异常状态时防御提升50%")
            .withCondition("has_status").defender());
        
        // 92: Poison Heal / 毒疗 - 中毒时回复HP
        
        // 98: Tinted Lens / 有色眼镜 - 效果不佳伤害×2
        addEffect(new AbilityEffect(98, "有色眼镜", "tinted-lens",
            EffectType.DAMAGE_MULTIPLIER, 2.0, "效果不佳伤害翻倍")
            .withCondition("not_very_effective").attacker());
        
        // 100: Sniper / 狙击手 - 暴击伤害×2.25（而非×1.5）
        addEffect(new AbilityEffect(100, "狙击手", "sniper",
            EffectType.DAMAGE_MULTIPLIER, 2.25, "暴击伤害变为2.25倍")
            .withCondition("critical_hit").attacker());
        
        // 102: Magic Guard / 魔法防守 - 不受间接伤害
        
        // 103: No Guard / 无防守 - 双方技能必中
        
        // 114: Stall / 慢出 - 总是后出手
        
        // 116: Unaware / 天真 - 忽略对手能力等级
        addEffect(new AbilityEffect(116, "天真", "unaware",
            EffectType.IGNORE_DEFENSE_BOOST, 1.0, "忽略对手防御等级提升").attacker());
        addEffect(new AbilityEffect(116, "天真", "unaware",
            EffectType.IGNORE_ATTACK_DROP, 1.0, "忽略自己攻击等级下降").attacker());
        
        // 122: Infiltrator / 穿透 - 无视屏幕/替身
        addEffect(new AbilityEffect(122, "穿透", "infiltrator",
            EffectType.DAMAGE_MULTIPLIER, 1.0, "无视反射壁/光墙/极光幕")
            .withCondition("ignore_screens").attacker());
        
        // 146: Symbiosis / 共生 - 道具传递
        
        // 154: Simple / 单纯 - 能力等级变化×2
        
        // 156: Unnerve / 紧张感 - 对手不能使用树果
        
        // 175: Stakeout / 蹲守 - 换上来的对手伤害×2
        addEffect(new AbilityEffect(175, "蹲守", "stakeout",
            EffectType.DAMAGE_MULTIPLIER, 2.0, "对换上来的对手伤害翻倍")
            .withCondition("opponent_switched_in").attacker());
        
        // 176: Water Bubble / 水泡 - 水系技能威力×2，灼伤免疫
        addEffect(new AbilityEffect(176, "水泡", "water-bubble",
            EffectType.POWER_MULTIPLIER, 2.0, "水系技能威力翻倍")
            .withTypes(TYPE_WATER).attacker());
        
        // 177: Steelworker / 钢能力者 - 钢系技能威力×1.5（已在上面定义）
        
        // 189: Psychic Surge / 精神场地 - 开启超能力场地
        
        // 202: Gorilla Tactics / 一意孤行 - 物攻×1.5，但只能使用一个技能
        addEffect(new AbilityEffect(202, "一意孤行", "gorilla-tactics",
            EffectType.ATTACK_MULTIPLIER, 1.5, "物攻提升50%").attacker());
        
        // 205: Pastel Veil / 彩粉蝶 - 免疫异常状态
        
        // 249: Costar / 同台共演 - 复制队友的能力等级
        
        // ==================== 威胁头带/眼镜类特性 ====================
        // 注意：这些是道具，不是特性
        
        // ==================== 其他特性（伤害不直接相关）========================
        
        // 13: Cloud Nine / 气象台 - 无视天气效果
        // 22: Intimidate / 威吓 - 上场时降低对手攻击
        // 23: Shadow Tag / 影子戏法 - 对手无法逃跑
        // 31: Lightning Rod / 避雷针 - 电系技能引向自己
        // 32: Serene Grace / 天恩 - 附加效果概率×2
        // 72: Intimidate / 威吓
        // 96: Cloud Nine / 气象台
        
    }
    
    private static void addEffect(AbilityEffect effect) {
        ABILITY_EFFECTS.computeIfAbsent(effect.abilityId, k -> new ArrayList<>())
                .add(effect);
    }
    
    /**
     * 获取特性的所有效果
     */
    public static List<AbilityEffect> getEffects(int abilityId) {
        return ABILITY_EFFECTS.getOrDefault(abilityId, Collections.emptyList());
    }
    
    /**
     * 获取特性的指定类型效果
     */
    public static List<AbilityEffect> getEffects(int abilityId, EffectType type) {
        List<AbilityEffect> effects = getEffects(abilityId);
        List<AbilityEffect> result = new ArrayList<>();
        for (AbilityEffect effect : effects) {
            if (effect.effectType == type) {
                result.add(effect);
            }
        }
        return result;
    }
    
    /**
     * 检查特性是否影响伤害计算
     */
    public static boolean affectsDamageCalculation(int abilityId) {
        List<AbilityEffect> effects = getEffects(abilityId);
        for (AbilityEffect effect : effects) {
            switch (effect.effectType) {
                case ATTACK_MULTIPLIER:
                case SP_ATTACK_MULTIPLIER:
                case POWER_MULTIPLIER:
                case STAB_MULTIPLIER:
                case DAMAGE_MULTIPLIER:
                case DEFENSE_MULTIPLIER:
                case SP_DEFENSE_MULTIPLIER:
                case TYPE_IMMUNITY:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
    
    /**
     * 获取所有影响伤害的特性ID列表
     */
    public static Set<Integer> getDamageAffectingAbilities() {
        Set<Integer> result = new HashSet<>();
        for (Map.Entry<Integer, List<AbilityEffect>> entry : ABILITY_EFFECTS.entrySet()) {
            if (affectsDamageCalculation(entry.getKey())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
