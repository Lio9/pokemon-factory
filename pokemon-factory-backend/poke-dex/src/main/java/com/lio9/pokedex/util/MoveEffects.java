package com.lio9.pokedex.util;



import java.util.*;

/**
 * 技能效果配置
 * 定义影响伤害计算的技能特殊效果
 * 
 * 技能ID参考: csv/moves.csv
 */
public class MoveEffects {
    
    /**
     * 技能效果类型枚举
     */
    public enum MoveEffectType {
        // 威力变化
        POWER_VARIABLE,             // 可变威力（根据条件）
        POWER_BOOST_LOW_HP,         // 低血量威力提升
        POWER_BOOST_STATUS,         // 对方有异常状态威力提升
        POWER_BOOST_WEIGHT,         // 根据体重计算威力
        POWER_BOOST_SPEED,          // 根据速度差计算威力
        POWER_BOOST_FRIENDSHIP,     // 根据亲密度计算威力
        POWER_BOOST_COUNTER,        // 反击类（根据受到的伤害）
        
        // 特殊伤害
        FIXED_DAMAGE,               // 固定伤害
        PERCENT_DAMAGE,             // 百分比伤害
        LEVEL_DAMAGE,               // 等级相关伤害
        HP_DIFF_DAMAGE,             // HP差值伤害
        
        // 伤害类型变化
        TYPE_EFFECTIVENESS_OVERRIDE, // 覆盖属性相性
        
        // 多段伤害
        MULTI_HIT,                  // 多段攻击
        
        // 其他
        IGNORE_DEFENSE,             // 忽略防御
        IGNORE_DEFENSE_BOOST,       // 忽略防御等级提升
        USE_DEFENSE_STAT,           // 使用防御方其他能力值
        USE_ATTACKER_DEFENSE,       // 使用攻击方防御值计算
        RECOIL,                     // 反伤
        DRAIN                       // 吸取
    }
    
    /**
     * 技能效果定义
     */
    public static class MoveEffect {
        public int moveId;
        public String moveName;
        public String moveIdentifier;
        public MoveEffectType effectType;
        public double baseValue;            // 基础值
        public double multiplier;           // 倍率
        public String condition;            // 条件
        public String description;
        public List<Integer> targetTypeIds; // 目标属性ID列表（用于属性相性覆盖）
        
        public MoveEffect(int moveId, String moveName, String moveIdentifier,
                         MoveEffectType effectType, String description) {
            this.moveId = moveId;
            this.moveName = moveName;
            this.moveIdentifier = moveIdentifier;
            this.effectType = effectType;
            this.description = description;
            this.baseValue = 0;
            this.multiplier = 1.0;
            this.targetTypeIds = new ArrayList<>();
        }
        
        public MoveEffect withBaseValue(double value) {
            this.baseValue = value;
            return this;
        }
        
        public MoveEffect withMultiplier(double mult) {
            this.multiplier = mult;
            return this;
        }
        
        public MoveEffect withCondition(String condition) {
            this.condition = condition;
            return this;
        }
        
        public MoveEffect withTargetTypes(Integer... types) {
            this.targetTypeIds = Arrays.asList(types);
            return this;
        }
    }
    
    /**
     * 所有技能效果映射表
     */
    private static final Map<Integer, List<MoveEffect>> MOVE_EFFECTS = new HashMap<>();
    
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
        // ==================== 可变威力技能 ====================
        
        // 起风 (Gust) - 对飞翔/弹跳中的对手威力翻倍
        addEffect(new MoveEffect(16, "起风", "gust", MoveEffectType.POWER_VARIABLE, 
                "对飞翔中的对手威力翻倍")
            .withCondition("target_flying"));
        
        // 电光一闪 (Quick Attack) - 优先度+1
        // 已在数据库中处理
        
        // 撞击 (Tackle) - 威力40，普通
        // 无特殊效果
        
        // 舍身冲撞 (Double-Edge) - 反伤1/4
        addEffect(new MoveEffect(38, "舍身冲撞", "double-edge", MoveEffectType.RECOIL, 
                "使用者受到1/4反伤")
            .withMultiplier(0.25));
        
        // ==================== 低血量威力提升 ====================
        
        // 喷射火焰 ( Flamethrower) - 10%灼伤几率
        // 已在move_meta中处理
        
        // 闪焰冲锋 (Flare Blitz) - 反伤1/3 + 10%灼伤
        addEffect(new MoveEffect(394, "闪焰冲锋", "flare-blitz", MoveEffectType.RECOIL, 
                "使用者受到1/3反伤，10%灼伤对手")
            .withMultiplier(0.33));
        
        // 勇鸟猛攻 (Brave Bird) - 反伤1/3
        addEffect(new MoveEffect(413, "勇鸟猛攻", "brave-bird", MoveEffectType.RECOIL, 
                "使用者受到1/3反伤")
            .withMultiplier(0.33));
        
        // 木质锤 (Wood Hammer) - 反伤1/3
        addEffect(new MoveEffect(452, "木质锤", "wood-hammer", MoveEffectType.RECOIL, 
                "使用者受到1/3反伤")
            .withMultiplier(0.33));
        
        // ==================== 根据体重计算威力 ====================
        
        // 草鞭 (Grass Knot) - 根据目标体重计算威力
        addEffect(new MoveEffect(447, "草鞭", "grass-knot", MoveEffectType.POWER_BOOST_WEIGHT, 
                "根据目标体重计算威力：0.1-1kg=20, 1-10kg=40, 10-25kg=60, 25-50kg=80, 50-100kg=100, 100-200kg=120, >200kg=140"));
        
        // 低空踢 (Low Kick) - 根据目标体重计算威力
        addEffect(new MoveEffect(67, "低空踢", "low-kick", MoveEffectType.POWER_BOOST_WEIGHT, 
                "根据目标体重计算威力：同草鞭"));
        
        // ==================== 根据速度计算威力 ====================
        
        // 电球 (Electro Ball) - 根据速度比计算威力
        addEffect(new MoveEffect(486, "电球", "electro-ball", MoveEffectType.POWER_BOOST_SPEED, 
                "速度比>4=150, >3=120, >2=80, >1=60, <=1=40"));
        
        // 回转攻 (Gyro Ball) - 根据速度比计算威力
        addEffect(new MoveEffect(360, "回转攻", "gyro-ball", MoveEffectType.POWER_BOOST_SPEED, 
                "威力 = 25 × 目标速度 / 使用者速度，最大150"));
        
        // ==================== 根据亲密度计算威力 ====================
        
        // 报恩 (Return) - 根据亲密度计算威力
        addEffect(new MoveEffect(216, "报恩", "return", MoveEffectType.POWER_BOOST_FRIENDSHIP, 
                "威力 = 亲密度 × 2 / 5，最高102"));
        
        // 迁怒 (Frustration) - 根据亲密度反向计算
        addEffect(new MoveEffect(218, "迁怒", "frustration", MoveEffectType.POWER_BOOST_FRIENDSHIP, 
                "威力 = (255 - 亲密度) × 2 / 5，最高102"));
        
        // ==================== 对异常状态威力提升 ====================
        
        // 祸不单行 (Hex) - 对异常状态对手威力翻倍
        addEffect(new MoveEffect(506, "祸不单行", "hex", MoveEffectType.POWER_BOOST_STATUS, 
                "对手有异常状态时威力翻倍")
            .withMultiplier(2.0));
        
        // 追打 (Pursuit) - 交换时威力翻倍
        addEffect(new MoveEffect(228, "追打", "pursuit", MoveEffectType.POWER_VARIABLE, 
                "对手交换时威力翻倍")
            .withCondition("target_switching")
            .withMultiplier(2.0));
        
        // 摔打 (Smack Down) - 击落飞行属性
        // 特殊效果：让飞行系/漂浮特性失效
        
        // ==================== 固定伤害技能 ====================
        
        // 龙之怒 (Dragon Rage) - 固定40伤害
        addEffect(new MoveEffect(82, "龙之怒", "dragon-rage", MoveEffectType.FIXED_DAMAGE, 
                "固定造成40点伤害")
            .withBaseValue(40));
        
        // 音爆 (Sonic Boom) - 固定20伤害
        addEffect(new MoveEffect(49, "音爆", "sonic-boom", MoveEffectType.FIXED_DAMAGE, 
                "固定造成20点伤害")
            .withBaseValue(20));
        
        // 地狱滚动 (Bide) - 储存2回合伤害后双倍返还
        // 特殊机制，需要单独处理
        
        // ==================== 百分比伤害 ====================
        
        // 蛮力 (Superpower) - 降低攻击防御各一级
        // 能力变化，不影响伤害计算
        
        // ==================== 等级相关伤害 ====================
        
        // 夜 shade (Night Shade) - 伤害等于使用者等级
        addEffect(new MoveEffect(101, "夜 shade", "night-shade", MoveEffectType.LEVEL_DAMAGE, 
                "伤害等于使用者等级"));
        
        // 寄生种子 (Leech Seed) - 每回合吸取1/8 HP
        // 持续效果，不影响即时伤害
        
        // ==================== HP差值伤害 ====================
        
        // 喷射火焰隐藏效果 - 羽栖 (Roost) 后失去飞行属性
        // 特殊效果
        
        // 电磁波 (Thunder Wave) - 麻痹
        // 变化技能
        
        // ==================== 多段攻击 ====================
        
        // 连续拳 (Comet Punch) - 2-5次攻击
        addEffect(new MoveEffect(4, "连续拳", "comet-punch", MoveEffectType.MULTI_HIT, 
                "攻击2-5次，平均3.167次"));
        
        // 双重攻击 (Double Kick) - 2次攻击
        addEffect(new MoveEffect(24, "双重攻击", "double-kick", MoveEffectType.MULTI_HIT, 
                "攻击2次")
            .withBaseValue(2));
        
        // 骨棒回旋镖 (Bonemerang) - 2次攻击
        addEffect(new MoveEffect(155, "骨棒回旋镖", "bonemerang", MoveEffectType.MULTI_HIT, 
                "攻击2次")
            .withBaseValue(2));
        
        // 三连踢 (Triple Kick) - 3次攻击，威力递增
        addEffect(new MoveEffect(167, "三连踢", "triple-kick", MoveEffectType.MULTI_HIT, 
                "攻击3次，威力分别为10/20/30"));
        
        // 燕返 (Aerial Ace) - 必中
        // 命中率已在数据库中处理
        
        // ==================== 反击类 ====================
        
        // 反击 (Counter) - 双倍返还物理伤害
        addEffect(new MoveEffect(68, "反击", "counter", MoveEffectType.POWER_BOOST_COUNTER, 
                "双倍返还受到的物理伤害")
            .withMultiplier(2.0)
            .withCondition("physical_hit"));
        
        // 镜面反射 (Mirror Coat) - 双倍返还特殊伤害
        addEffect(new MoveEffect(243, "镜面反射", "mirror-coat", MoveEffectType.POWER_BOOST_COUNTER, 
                "双倍返还受到的特殊伤害")
            .withMultiplier(2.0)
            .withCondition("special_hit"));
        
        // 金属爆炸 (Metal Burst) - 1.5倍返还伤害
        addEffect(new MoveEffect(368, "金属爆炸", "metal-burst", MoveEffectType.POWER_BOOST_COUNTER, 
                "1.5倍返还受到的伤害")
            .withMultiplier(1.5));
        
        // ==================== 属性相性覆盖 ====================
        
        // 冷冻干燥 (Freeze-Dry) - 对水属性效果绝佳
        addEffect(new MoveEffect(573, "冷冻干燥", "freeze-dry", MoveEffectType.TYPE_EFFECTIVENESS_OVERRIDE, 
                "对水属性效果绝佳(2x)")
            .withTargetTypes(TYPE_WATER)
            .withMultiplier(2.0));
        
        // 飞翔 (Fly) - 第一回合飞到空中
        // 蓄力技能，已在move_flag中标记
        
        // 千箭 (Thousand Arrows) - 对飞行系有效，可击中地面免疫
        addEffect(new MoveEffect(614, "千箭", "thousand-arrows", MoveEffectType.TYPE_EFFECTIVENESS_OVERRIDE, 
                "对飞行系有效，可击中漂浮特性")
            .withTargetTypes(TYPE_FLYING)
            .withMultiplier(1.0));
        
        // 千波激荡 (Thousand Waves) - 无法交换
        // 特殊效果
        
        // ==================== 忽略防御 ====================
        
        // 心之眼 (Mind Reader) - 必中
        // 命中率
        
        // 锁定 (Lock-On) - 必中
        // 命中率
        
        // 暗影潜袭 (Shadow Force) - 忽略保护
        // 特殊标记
        
        // ==================== 使用其他能力值 ====================
        
        // 精神冲击 (Psystrike) - 使用目标防御计算伤害
        addEffect(new MoveEffect(540, "精神冲击", "psystrike", MoveEffectType.USE_DEFENSE_STAT, 
                "使用目标的防御值而非特防值计算伤害")
            .withCondition("use_defense"));
        
        // 秘密之力 (Secret Sword) - 使用目标防御计算伤害
        addEffect(new MoveEffect(548, "秘密之力", "secret-sword", MoveEffectType.USE_DEFENSE_STAT, 
                "使用目标的防御值而非特防值计算伤害")
            .withCondition("use_defense"));
        
        // 光子喷涌 (Photon Geyser) - 根据攻击/特攻较高值使用
        addEffect(new MoveEffect(662, "光子喷涌", "photon-geyser", MoveEffectType.USE_DEFENSE_STAT, 
                "使用攻击/特攻较高值，使用目标防御/特防对应值")
            .withCondition("use_higher_attack"));
        
        // ==================== 吸取类 ====================
        
        // 百万吸取 (Mega Drain) - 吸取50%伤害
        addEffect(new MoveEffect(72, "百万吸取", "mega-drain", MoveEffectType.DRAIN, 
                "回复造成伤害的50%")
            .withMultiplier(0.5));
        
        // 吸取 (Absorb) - 吸取50%伤害
        addEffect(new MoveEffect(71, "吸取", "absorb", MoveEffectType.DRAIN, 
                "回复造成伤害的50%")
            .withMultiplier(0.5));
        
        // 超级吸取 (Giga Drain) - 吸取50%伤害
        addEffect(new MoveEffect(202, "超级吸取", "giga-drain", MoveEffectType.DRAIN, 
                "回复造成伤害的50%")
            .withMultiplier(0.5));
        
        // 寄生种子 (Leech Seed) - 吸取1/8 HP
        // 持续效果
        
        // ==================== 天气相关 ====================
        
        // 暴风 (Hurricane) - 雨天必中
        // 命中率特殊处理
        
        // 打雷 (Thunder) - 雨天必中
        // 命中率特殊处理
        
        // 日光束 (Solar Beam) - 晴天无需蓄力
        // 蓄力相关
        
        // 气象球 (Weather Ball) - 根据天气变化属性和威力
        addEffect(new MoveEffect(311, "气象球", "weather-ball", MoveEffectType.POWER_VARIABLE, 
                "根据天气变化属性和威力(晴天火/雨天水/沙暴岩/冰雹冰)，威力翻倍"));
        
        // ==================== 其他特殊 ====================
        
        // 觉醒力量 (Hidden Power) - 属性可变
        // 需要根据个体值计算，暂时无法实现
        
        // 自然之怒 (Nature's Madness) - 造成目标当前HP一半伤害
        addEffect(new MoveEffect(226, "自然之怒", "natures-madness", MoveEffectType.PERCENT_DAMAGE, 
                "造成目标当前HP 50%伤害")
            .withMultiplier(0.5));
        
        // 龙之俯冲 (Dragon Rush) - 20%畏缩
        // move_meta中处理
        
        // 头锤 (Headbutt) - 30%畏缩
        // move_meta中处理
        
        // 岩崩 (Rock Slide) - 30%畏缩
        // move_meta中处理
    }
    
    private static void addEffect(MoveEffect effect) {
        MOVE_EFFECTS.computeIfAbsent(effect.moveId, k -> new ArrayList<>()).add(effect);
    }
    
    /**
     * 获取技能的所有效果
     */
    public static List<MoveEffect> getEffects(int moveId) {
        return MOVE_EFFECTS.getOrDefault(moveId, Collections.emptyList());
    }
    
    /**
     * 检查技能是否有特定类型的效果
     */
    public static boolean hasEffectType(int moveId, MoveEffectType type) {
        List<MoveEffect> effects = MOVE_EFFECTS.get(moveId);
        if (effects == null) return false;
        return effects.stream().anyMatch(e -> e.effectType == type);
    }
    
    /**
     * 获取技能的第一个特定类型效果
     */
    public static MoveEffect getEffectByType(int moveId, MoveEffectType type) {
        List<MoveEffect> effects = MOVE_EFFECTS.get(moveId);
        if (effects == null) return null;
        return effects.stream().filter(e -> e.effectType == type).findFirst().orElse(null);
    }
    
    /**
     * 检查是否是反伤技能
     */
    public static boolean isRecoilMove(int moveId) {
        return hasEffectType(moveId, MoveEffectType.RECOIL);
    }
    
    /**
     * 检查是否是吸取技能
     */
    public static boolean isDrainMove(int moveId) {
        return hasEffectType(moveId, MoveEffectType.DRAIN);
    }
    
    /**
     * 检查是否是固定伤害技能
     */
    public static boolean isFixedDamageMove(int moveId) {
        return hasEffectType(moveId, MoveEffectType.FIXED_DAMAGE);
    }
    
    /**
     * 检查是否是多段攻击技能
     */
    public static boolean isMultiHitMove(int moveId) {
        return hasEffectType(moveId, MoveEffectType.MULTI_HIT);
    }
    
    /**
     * 检查是否使用防御值计算特殊伤害
     */
    public static boolean usesDefenseStat(int moveId) {
        return hasEffectType(moveId, MoveEffectType.USE_DEFENSE_STAT);
    }
}
