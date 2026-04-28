package com.lio9.pokedex.util;



/**
 * ItemEffects 文件说明
 * 所属模块：poke-dex 后端模块。
 * 文件类型：工具类文件。
 * 核心职责：负责承载跨模块复用的通用计算或辅助处理逻辑。
 * 阅读建议：建议关注输入输出约束与可复用边界。
 * 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。
 */

import java.util.*;

/**
 * 道具效果配置
 * 定义所有影响伤害计算的道具的效果
 * 
 * 道具ID参考: csv/items.csv
 * 道具分类ID参考: csv/item_categories.csv
 */
public class ItemEffects {
    
    /**
     * 道具效果类型枚举
     */
    public enum EffectType {
        // 攻击相关
        ATTACK_MULTIPLIER,          // 攻击倍率
        SP_ATTACK_MULTIPLIER,       // 特攻倍率
        POWER_MULTIPLIER,           // 威力倍率
        CRITICAL_RATE_BOOST,        // 暴击率提升
        ACCURACY_BOOST,             // 命中率提升
        
        // 防御相关
        DAMAGE_MULTIPLIER,          // 受伤倍率
        DEFENSE_MULTIPLIER,         // 防御倍率
        SP_DEFENSE_MULTIPLIER,      // 特防倍率
        
        // 特殊效果
        STAB_BOOST,                 // 本系加成
        SUPER_EFFECTIVE_BOOST,      // 效果绝佳加成
        TYPE_POWER_BOOST,           // 特定属性威力提升
        HP_PERCENT_DAMAGE,          // 固定百分比伤害
        RECOIL_DAMAGE,              // 反伤
        STATUS_IMMUNITY,            // 状态免疫
        CONFUSE_ON_HIT,             // 命中时混乱
        
        // 一次性效果
        CONSUME_ON_HIT,             // 命中后消耗
        CONSUME_ON_HIT_WEAKEN,      // 效果绝佳时消耗并减伤
        CONSUME_ON_SUPER_EFFECTIVE, // 效果绝佳时消耗
    }
    
    /**
     * 道具效果定义
     */
    public static class ItemEffect {
        public int itemId;
        public String itemName;
        public String itemIdentifier;
        public EffectType effectType;
        public double value;
        public List<Integer> typeIds;          // 作用的属性ID列表
        public String condition;               // 触发条件
        public String description;
        public boolean affectsAttacker;        // 是否影响攻击方
        public boolean affectsDefender;        // 是否影响防御方
        public Integer categoryId;             // 道具分类ID
        
        public ItemEffect(int itemId, String itemName, String itemIdentifier,
                         EffectType effectType, double value, String description) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemIdentifier = itemIdentifier;
            this.effectType = effectType;
            this.value = value;
            this.description = description;
            this.typeIds = new ArrayList<>();
            this.affectsAttacker = false;
            this.affectsDefender = false;
        }
        
        public ItemEffect withTypes(Integer... types) {
            this.typeIds = Arrays.asList(types);
            return this;
        }
        
        public ItemEffect withCondition(String condition) {
            this.condition = condition;
            return this;
        }
        
        public ItemEffect withCategory(int categoryId) {
            this.categoryId = categoryId;
            return this;
        }
        
        public ItemEffect attacker() {
            this.affectsAttacker = true;
            return this;
        }
        
        public ItemEffect defender() {
            this.affectsDefender = true;
            return this;
        }
    }
    
    /**
     * 所有道具效果映射表
     * Key: 道具ID, Value: 效果列表
     */
    private static final Map<Integer, List<ItemEffect>> ITEM_EFFECTS = new HashMap<>();
    
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
    
    // 道具分类ID常量
    public static final int CAT_TYPE_ENHANCEMENT = 2;  // 属性增强
    public static final int CAT_EVOLUTION = 3;         // 进化道具
    public static final int CAT_PLATES = 4;            // 石板
    public static final int CAT_IN_BATTLE = 5;         // 战斗道具
    public static final int CAT_HELD_ITEMS = 9;        // 携带道具
    public static final int CAT_LOOT = 10;             // 战利品
    
    static {
        // ==================== 属性增强道具（板类）====================
        // 威力提升20%（×1.2）
        
        // 217: Choice Band / 讲究围巾 - 物攻×1.5
        addEffect(new ItemEffect(217, "讲究围巾", "choice-band",
            EffectType.ATTACK_MULTIPLIER, 1.5, "物理攻击提升50%").attacker());
        
        // 218: Choice Specs / 讲究眼镜 - 特攻×1.5
        addEffect(new ItemEffect(218, "讲究眼镜", "choice-specs",
            EffectType.SP_ATTACK_MULTIPLIER, 1.5, "特殊攻击提升50%").attacker());
        
        // 247: Life Orb / 生命宝珠 - 伤害×1.3
        addEffect(new ItemEffect(247, "生命宝珠", "life-orb",
            EffectType.DAMAGE_MULTIPLIER, 1.3, "伤害提升30%，每次攻击损失10%HP").attacker());
        
        // 261: Muscle Band / 肌肉强化 - 物理伤害×1.1
        addEffect(new ItemEffect(261, "肌肉强化", "muscle-band",
            EffectType.POWER_MULTIPLIER, 1.1, "物理技能威力提升10%").attacker());
        
        // 262: Expert Belt / 专家腰带 - 效果绝佳伤害×1.2
        addEffect(new ItemEffect(262, "专家腰带", "expert-belt",
            EffectType.SUPER_EFFECTIVE_BOOST, 1.2, "效果绝佳伤害提升20%").attacker());
        
        // 263: Wise Glasses / 智慧眼镜 - 特殊伤害×1.1
        addEffect(new ItemEffect(263, "智慧眼镜", "wise-glasses",
            EffectType.POWER_MULTIPLIER, 1.1, "特殊技能威力提升10%").attacker());
        
        // 228: Choice Scarf / 讲究头巾 - 速度×1.5（不影响伤害）
        
        // 233: Thick Club / 粗骨头 - 可拉可拉/嘎啦嘎啦物攻×2
        addEffect(new ItemEffect(233, "粗骨头", "thick-club",
            EffectType.ATTACK_MULTIPLIER, 2.0, "可拉可拉/嘎啦嘎啦物攻翻倍")
            .withCondition("cubone_or_marowak").attacker());
        
        // 234: Deep Sea Tooth / 深海之牙 - 珍珠贝特攻×2
        addEffect(new ItemEffect(234, "深海之牙", "deep-sea-tooth",
            EffectType.SP_ATTACK_MULTIPLIER, 2.0, "珍珠贝特攻翻倍")
            .withCondition("clamperl").attacker());
        
        // 235: Deep Sea Scale / 深海之鳞 - 珍珠贝特防×2
        addEffect(new ItemEffect(235, "深海之鳞", "deep-sea-scale",
            EffectType.SP_DEFENSE_MULTIPLIER, 2.0, "珍珠贝特防翻倍")
            .withCondition("clamperl").defender());
        
        // 236: Light Ball / 电气球 - 皮卡丘攻/特攻×2
        addEffect(new ItemEffect(236, "电气球", "light-ball",
            EffectType.ATTACK_MULTIPLIER, 2.0, "皮卡丘攻击翻倍")
            .withCondition("pikachu").attacker());
        addEffect(new ItemEffect(236, "电气球", "light-ball",
            EffectType.SP_ATTACK_MULTIPLIER, 2.0, "皮卡丘特攻翻倍")
            .withCondition("pikachu").attacker());
        
        // 237: Metal Powder / 金属膜 - 百变怪防御×2
        addEffect(new ItemEffect(237, "金属膜", "metal-powder",
            EffectType.DEFENSE_MULTIPLIER, 2.0, "百变怪防御翻倍")
            .withCondition("ditto").defender());
        
        // 238: Quick Powder / 速度粉 - 百变怪速度×2（不影响伤害）
        
        // 248: Power Herb / 能量药草 - 蓄力技能直接发动
        // 特殊机制，不在伤害计算中
        
        // 250: White Herb / 白色香草 - 能力下降时恢复
        
        // 251: Soul Dew / 心之水滴 - 拉帝亚斯/拉帝欧斯特攻/特防×1.5
        addEffect(new ItemEffect(251, "心之水滴", "soul-dew",
            EffectType.SP_ATTACK_MULTIPLIER, 1.5, "拉帝亚斯/拉帝欧斯特攻提升50%")
            .withCondition("latios_or_latias").attacker());
        addEffect(new ItemEffect(251, "心之水滴", "soul-dew",
            EffectType.SP_DEFENSE_MULTIPLIER, 1.5, "拉帝亚斯/拉帝欧斯特防提升50%")
            .withCondition("latios_or_latias").defender());
        
        // 254: Adamant Orb / 白玉宝珠 - 帝牙卢卡钢/龙系威力×1.2
        addEffect(new ItemEffect(254, "白玉宝珠", "adamant-orb",
            EffectType.POWER_MULTIPLIER, 1.2, "帝牙卢卡钢/龙系技能威力提升20%")
            .withTypes(TYPE_STEEL, TYPE_DRAGON).withCondition("dialga").attacker());
        
        // 255: Lustrous Orb / 白金宝珠 - 帕路奇亚水/龙系威力×1.2
        addEffect(new ItemEffect(255, "白金宝珠", "lustrous-orb",
            EffectType.POWER_MULTIPLIER, 1.2, "帕路奇亚水/龙系技能威力提升20%")
            .withTypes(TYPE_WATER, TYPE_DRAGON).withCondition("palkia").attacker());
        
        // 256: Griseous Orb / 白金宝珠 - 骑拉帝纳鬼/龙系威力×1.2
        addEffect(new ItemEffect(256, "白玉宝珠", "griseous-orb",
            EffectType.POWER_MULTIPLIER, 1.2, "骑拉帝纳鬼/龙系技能威力提升20%")
            .withTypes(TYPE_GHOST, TYPE_DRAGON).withCondition("giratina").attacker());
        
        // ==================== 属性强化石板 ====================
        // 所有石板：对应属性技能威力×1.2
        
        // 石板ID（需要根据实际数据库确定）
        // 这里假设石板的ID范围
        addEffect(new ItemEffect(296, "普通石板", "normal-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "普通系技能威力提升20%")
            .withTypes(TYPE_NORMAL).attacker());
        addEffect(new ItemEffect(297, "格斗石板", "fighting-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "格斗系技能威力提升20%")
            .withTypes(TYPE_FIGHTING).attacker());
        addEffect(new ItemEffect(298, "飞行石板", "flying-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "飞行系技能威力提升20%")
            .withTypes(TYPE_FLYING).attacker());
        addEffect(new ItemEffect(299, "毒石板", "poison-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "毒系技能威力提升20%")
            .withTypes(TYPE_POISON).attacker());
        addEffect(new ItemEffect(300, "地面石板", "ground-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "地面系技能威力提升20%")
            .withTypes(TYPE_GROUND).attacker());
        addEffect(new ItemEffect(301, "岩石石板", "rock-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "岩石系技能威力提升20%")
            .withTypes(TYPE_ROCK).attacker());
        addEffect(new ItemEffect(302, "虫石板", "bug-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "虫系技能威力提升20%")
            .withTypes(TYPE_BUG).attacker());
        addEffect(new ItemEffect(303, "幽灵石板", "ghost-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "幽灵系技能威力提升20%")
            .withTypes(TYPE_GHOST).attacker());
        addEffect(new ItemEffect(304, "钢石板", "steel-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "钢系技能威力提升20%")
            .withTypes(TYPE_STEEL).attacker());
        addEffect(new ItemEffect(305, "火石板", "flame-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "火系技能威力提升20%")
            .withTypes(TYPE_FIRE).attacker());
        addEffect(new ItemEffect(306, "水石板", "splash-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "水系技能威力提升20%")
            .withTypes(TYPE_WATER).attacker());
        addEffect(new ItemEffect(307, "草石板", "meadow-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "草系技能威力提升20%")
            .withTypes(TYPE_GRASS).attacker());
        addEffect(new ItemEffect(308, "电石板", "zap-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "电系技能威力提升20%")
            .withTypes(TYPE_ELECTRIC).attacker());
        addEffect(new ItemEffect(309, "超能石板", "mind-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "超能系技能威力提升20%")
            .withTypes(TYPE_PSYCHIC).attacker());
        addEffect(new ItemEffect(310, "冰石板", "icicle-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "冰系技能威力提升20%")
            .withTypes(TYPE_ICE).attacker());
        addEffect(new ItemEffect(311, "龙石板", "draco-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "龙系技能威力提升20%")
            .withTypes(TYPE_DRAGON).attacker());
        addEffect(new ItemEffect(312, "恶石板", "dread-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "恶系技能威力提升20%")
            .withTypes(TYPE_DARK).attacker());
        addEffect(new ItemEffect(313, "妖精石板", "pixie-plate",
            EffectType.POWER_MULTIPLIER, 1.2, "妖精系技能威力提升20%")
            .withTypes(TYPE_FAIRY).attacker());
        
        // ==================== 属性强化香炉 ====================
        // 香炉：对应属性技能威力×1.2
        
        // 265: Silk Scarf / 丝绸围巾 - 普通系×1.2
        addEffect(new ItemEffect(265, "丝绸围巾", "silk-scarf",
            EffectType.POWER_MULTIPLIER, 1.2, "普通系技能威力提升20%")
            .withTypes(TYPE_NORMAL).attacker());
        
        // 266: Black Belt / 黑色带子 - 格斗系×1.2
        addEffect(new ItemEffect(266, "黑色带子", "black-belt",
            EffectType.POWER_MULTIPLIER, 1.2, "格斗系技能威力提升20%")
            .withTypes(TYPE_FIGHTING).attacker());
        
        // 267: Sharp Beak / 尖嘴 - 飞行系×1.2
        addEffect(new ItemEffect(267, "尖嘴", "sharp-beak",
            EffectType.POWER_MULTIPLIER, 1.2, "飞行系技能威力提升20%")
            .withTypes(TYPE_FLYING).attacker());
        
        // 268: Poison Barb / 毒针 - 毒系×1.2
        addEffect(new ItemEffect(268, "毒针", "poison-barb",
            EffectType.POWER_MULTIPLIER, 1.2, "毒系技能威力提升20%")
            .withTypes(TYPE_POISON).attacker());
        
        // 269: Soft Sand / 软沙 - 地面系×1.2
        addEffect(new ItemEffect(269, "软沙", "soft-sand",
            EffectType.POWER_MULTIPLIER, 1.2, "地面系技能威力提升20%")
            .withTypes(TYPE_GROUND).attacker());
        
        // 270: Hard Stone / 硬石 - 岩石系×1.2
        addEffect(new ItemEffect(270, "硬石", "hard-stone",
            EffectType.POWER_MULTIPLIER, 1.2, "岩石系技能威力提升20%")
            .withTypes(TYPE_ROCK).attacker());
        
        // 271: Silver Powder / 银粉 - 虫系×1.2
        addEffect(new ItemEffect(271, "银粉", "silver-powder",
            EffectType.POWER_MULTIPLIER, 1.2, "虫系技能威力提升20%")
            .withTypes(TYPE_BUG).attacker());
        
        // 272: Spell Tag / 咒符 - 幽灵系×1.2
        addEffect(new ItemEffect(272, "咒符", "spell-tag",
            EffectType.POWER_MULTIPLIER, 1.2, "幽灵系技能威力提升20%")
            .withTypes(TYPE_GHOST).attacker());
        
        // 273: Metal Coat / 金属膜 - 钢系×1.2
        addEffect(new ItemEffect(273, "金属膜", "metal-coat",
            EffectType.POWER_MULTIPLIER, 1.2, "钢系技能威力提升20%")
            .withTypes(TYPE_STEEL).attacker());
        
        // 274: Charcoal / 木炭 - 火系×1.2
        addEffect(new ItemEffect(274, "木炭", "charcoal",
            EffectType.POWER_MULTIPLIER, 1.2, "火系技能威力提升20%")
            .withTypes(TYPE_FIRE).attacker());
        
        // 275: Mystic Water / 神秘水滴 - 水系×1.2
        addEffect(new ItemEffect(275, "神秘水滴", "mystic-water",
            EffectType.POWER_MULTIPLIER, 1.2, "水系技能威力提升20%")
            .withTypes(TYPE_WATER).attacker());
        
        // 276: Miracle Seed / 奇迹种子 - 草系×1.2
        addEffect(new ItemEffect(276, "奇迹种子", "miracle-seed",
            EffectType.POWER_MULTIPLIER, 1.2, "草系技能威力提升20%")
            .withTypes(TYPE_GRASS).attacker());
        
        // 277: Magnet / 磁铁 - 电系×1.2
        addEffect(new ItemEffect(277, "磁铁", "magnet",
            EffectType.POWER_MULTIPLIER, 1.2, "电系技能威力提升20%")
            .withTypes(TYPE_ELECTRIC).attacker());
        
        // 278: Twisted Spoon / 弯曲勺子 - 超能系×1.2
        addEffect(new ItemEffect(278, "弯曲勺子", "twisted-spoon",
            EffectType.POWER_MULTIPLIER, 1.2, "超能系技能威力提升20%")
            .withTypes(TYPE_PSYCHIC).attacker());
        
        // 279: Never-Melt Ice / 不融冰 - 冰系×1.2
        addEffect(new ItemEffect(279, "不融冰", "never-melt-ice",
            EffectType.POWER_MULTIPLIER, 1.2, "冰系技能威力提升20%")
            .withTypes(TYPE_ICE).attacker());
        
        // 280: Dragon Fang / 龙牙 - 龙系×1.2
        addEffect(new ItemEffect(280, "龙牙", "dragon-fang",
            EffectType.POWER_MULTIPLIER, 1.2, "龙系技能威力提升20%")
            .withTypes(TYPE_DRAGON).attacker());
        
        // 281: Black Glasses / 黑色眼镜 - 恶系×1.2
        addEffect(new ItemEffect(281, "黑色眼镜", "black-glasses",
            EffectType.POWER_MULTIPLIER, 1.2, "恶系技能威力提升20%")
            .withTypes(TYPE_DARK).attacker());
        
        // 282: Pink Bow / 粉色蝴蝶结 - 妖精系×1.2
        addEffect(new ItemEffect(282, "粉色蝴蝶结", "fairy-feather",
            EffectType.POWER_MULTIPLIER, 1.2, "妖精系技能威力提升20%")
            .withTypes(TYPE_FAIRY).attacker());
        
        // ==================== 属性宝石 ====================
        // 宝石：首次使用对应属性技能威力×1.5，然后消耗
        
        // 宝石ID（需要根据实际数据库确定）
        addEffect(new ItemEffect(656, "普通宝石", "normal-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用普通系技能威力提升50%")
            .withTypes(TYPE_NORMAL).withCondition("first_use").attacker());
        addEffect(new ItemEffect(657, "格斗宝石", "fighting-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用格斗系技能威力提升50%")
            .withTypes(TYPE_FIGHTING).withCondition("first_use").attacker());
        addEffect(new ItemEffect(658, "飞行宝石", "flying-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用飞行系技能威力提升50%")
            .withTypes(TYPE_FLYING).withCondition("first_use").attacker());
        addEffect(new ItemEffect(659, "毒宝石", "poison-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用毒系技能威力提升50%")
            .withTypes(TYPE_POISON).withCondition("first_use").attacker());
        addEffect(new ItemEffect(660, "地面宝石", "ground-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用地面系技能威力提升50%")
            .withTypes(TYPE_GROUND).withCondition("first_use").attacker());
        addEffect(new ItemEffect(661, "岩石宝石", "rock-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用岩石系技能威力提升50%")
            .withTypes(TYPE_ROCK).withCondition("first_use").attacker());
        addEffect(new ItemEffect(662, "虫宝石", "bug-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用虫系技能威力提升50%")
            .withTypes(TYPE_BUG).withCondition("first_use").attacker());
        addEffect(new ItemEffect(663, "幽灵宝石", "ghost-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用幽灵系技能威力提升50%")
            .withTypes(TYPE_GHOST).withCondition("first_use").attacker());
        addEffect(new ItemEffect(664, "钢宝石", "steel-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用钢系技能威力提升50%")
            .withTypes(TYPE_STEEL).withCondition("first_use").attacker());
        addEffect(new ItemEffect(665, "火宝石", "fire-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用火系技能威力提升50%")
            .withTypes(TYPE_FIRE).withCondition("first_use").attacker());
        addEffect(new ItemEffect(666, "水宝石", "water-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用水系技能威力提升50%")
            .withTypes(TYPE_WATER).withCondition("first_use").attacker());
        addEffect(new ItemEffect(667, "草宝石", "grass-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用草系技能威力提升50%")
            .withTypes(TYPE_GRASS).withCondition("first_use").attacker());
        addEffect(new ItemEffect(668, "电宝石", "electric-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用电系技能威力提升50%")
            .withTypes(TYPE_ELECTRIC).withCondition("first_use").attacker());
        addEffect(new ItemEffect(669, "超能宝石", "psychic-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用超能系技能威力提升50%")
            .withTypes(TYPE_PSYCHIC).withCondition("first_use").attacker());
        addEffect(new ItemEffect(670, "冰宝石", "ice-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用冰系技能威力提升50%")
            .withTypes(TYPE_ICE).withCondition("first_use").attacker());
        addEffect(new ItemEffect(671, "龙宝石", "dragon-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用龙系技能威力提升50%")
            .withTypes(TYPE_DRAGON).withCondition("first_use").attacker());
        addEffect(new ItemEffect(672, "恶宝石", "dark-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用恶系技能威力提升50%")
            .withTypes(TYPE_DARK).withCondition("first_use").attacker());
        addEffect(new ItemEffect(673, "妖精宝石", "fairy-gem",
            EffectType.POWER_MULTIPLIER, 1.5, "首次使用妖精系技能威力提升50%")
            .withTypes(TYPE_FAIRY).withCondition("first_use").attacker());
        
        // ==================== 减伤道具 ====================
        
        // 进化奇石 - 未进化宝可梦双防×1.5
        addEffect(new ItemEffect(923, "进化奇石", "eviolite",
            EffectType.DEFENSE_MULTIPLIER, 1.5, "未进化宝可梦防御提升50%")
            .withCondition("not_fully_evolved").defender());
        addEffect(new ItemEffect(923, "进化奇石", "eviolite",
            EffectType.SP_DEFENSE_MULTIPLIER, 1.5, "未进化宝可梦特防提升50%")
            .withCondition("not_fully_evolved").defender());
        
        // ==================== 减伤树果 ====================
        // 效果绝佳时伤害减半，然后消耗
        
        // 树果ID（需要根据实际数据库确定）
        addEffect(new ItemEffect(149, "橘橘果", "chople-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "格斗系效果绝佳伤害减半")
            .withTypes(TYPE_FIGHTING).withCondition("super_effective").defender());
        addEffect(new ItemEffect(150, "桃桃果", "kebia-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "毒系效果绝佳伤害减半")
            .withTypes(TYPE_POISON).withCondition("super_effective").defender());
        addEffect(new ItemEffect(151, "葡萄果", "shuca-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "地面系效果绝佳伤害减半")
            .withTypes(TYPE_GROUND).withCondition("super_effective").defender());
        addEffect(new ItemEffect(152, "番番果", "cob-ya-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "岩石系效果绝佳伤害减半")
            .withTypes(TYPE_ROCK).withCondition("super_effective").defender());
        addEffect(new ItemEffect(153, "勿忘我果", "payapa-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "虫系效果绝佳伤害减半")
            .withTypes(TYPE_BUG).withCondition("super_effective").defender());
        addEffect(new ItemEffect(154, "木木果", "tanga-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "幽灵系效果绝佳伤害减半")
            .withTypes(TYPE_GHOST).withCondition("super_effective").defender());
        addEffect(new ItemEffect(155, "青梨果", "charti-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "钢系效果绝佳伤害减半")
            .withTypes(TYPE_STEEL).withCondition("super_effective").defender());
        addEffect(new ItemEffect(156, "香蕉果", "kasib-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "火系效果绝佳伤害减半")
            .withTypes(TYPE_FIRE).withCondition("super_effective").defender());
        addEffect(new ItemEffect(157, "西雅果", "haban-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "水系效果绝佳伤害减半")
            .withTypes(TYPE_WATER).withCondition("super_effective").defender());
        addEffect(new ItemEffect(158, "西柚果", "colbur-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "草系效果绝佳伤害减半")
            .withTypes(TYPE_GRASS).withCondition("super_effective").defender());
        addEffect(new ItemEffect(159, "罗望果", "babiri-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "电系效果绝佳伤害减半")
            .withTypes(TYPE_ELECTRIC).withCondition("super_effective").defender());
        addEffect(new ItemEffect(160, "亚开果", "charti-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "超能系效果绝佳伤害减半")
            .withTypes(TYPE_PSYCHIC).withCondition("super_effective").defender());
        addEffect(new ItemEffect(161, "辣樱果", "yache-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "冰系效果绝佳伤害减半")
            .withTypes(TYPE_ICE).withCondition("super_effective").defender());
        addEffect(new ItemEffect(162, "墨莓果", "chilan-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "龙系效果绝佳伤害减半")
            .withTypes(TYPE_DRAGON).withCondition("super_effective").defender());
        addEffect(new ItemEffect(163, "圆柚果", "roseli-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "恶系效果绝佳伤害减半")
            .withTypes(TYPE_DARK).withCondition("super_effective").defender());
        addEffect(new ItemEffect(164, "半面果", "keeb-berry",
            EffectType.DAMAGE_MULTIPLIER, 0.5, "妖精系效果绝佳伤害减半")
            .withTypes(TYPE_FAIRY).withCondition("super_effective").defender());
        
        // ==================== 暴击相关道具 ====================
        
        // 283: Scope Lens / 聚焦镜片 - 暴击率+1
        addEffect(new ItemEffect(283, "聚焦镜片", "scope-lens",
            EffectType.CRITICAL_RATE_BOOST, 1.0, "暴击率提升1级").attacker());
        
        // 284: Razor Claw / 锐利之爪 - 暴击率+1
        addEffect(new ItemEffect(284, "锐利之爪", "razor-claw",
            EffectType.CRITICAL_RATE_BOOST, 1.0, "暴击率提升1级").attacker());
        
        // 285: Stick / 大葱 - 大葱鸭暴击率+2
        addEffect(new ItemEffect(285, "大葱", "leek",
            EffectType.CRITICAL_RATE_BOOST, 2.0, "大葱鸭暴击率提升2级")
            .withCondition("farfetchd").attacker());
        
        // 286: Lucky Punch / 幸运拳套 - 吉利蛋暴击率+2
        addEffect(new ItemEffect(286, "幸运拳套", "lucky-punch",
            EffectType.CRITICAL_RATE_BOOST, 2.0, "吉利蛋暴击率提升2级")
            .withCondition("chansey").attacker());
        
        // ==================== 特殊道具 ====================
        
        // 287: Metronome / 节拍器 - 连续使用同一技能威力递增
        addEffect(new ItemEffect(287, "节拍器", "metronome",
            EffectType.POWER_MULTIPLIER, 1.0, "连续使用同一技能威力提升")
            .withCondition("consecutive_use").attacker());
        
        // 288: Rocky Helmet / 凹凸头盔 - 接触攻击者损失1/6HP
        addEffect(new ItemEffect(288, "凹凸头盔", "rocky-helmet",
            EffectType.RECOIL_DAMAGE, 0.1667, "接触攻击者损失1/6HP")
            .withCondition("contact_move").defender());
        
        // 289: Binding Band / 束缚带 - 束缚伤害增加
        
        // 290: Protective Pads / 保护垫 - 免疫接触类技能的效果
        // 特殊机制
        
        // 291: Heavy-Duty Boots / 厚底靴 - 免疫钉子
        
        // 292: Blund Policy / 强行推击
        // 特殊机制
        
        // 293: Eject Button / 逃脱按键
        // 特殊机制
        
        // 294: Eject Pack / 逃脱背包
        // 特殊机制
        
        // 295: Room Service / 场地服务
        // 特殊机制
        
        // ==================== 其他道具 ====================
        
        // 空贝铃铛 - 回复造成伤害的1/8
        addEffect(new ItemEffect(252, "空贝铃铛", "shell-bell",
            EffectType.HP_PERCENT_DAMAGE, 0.125, "回复造成伤害的12.5%").attacker());
        
        // 大根茎 - 吸取类技能回复量增加
        addEffect(new ItemEffect(253, "大根茎", "big-root",
            EffectType.HP_PERCENT_DAMAGE, 1.3, "吸取技能回复量提升30%").attacker());
        
        // 锐利之牙
        addEffect(new ItemEffect(284, "锐利之牙", "razor-fang",
            EffectType.CRITICAL_RATE_BOOST, 1.0, "暴击率提升1级").attacker());
        
    }
    
    private static void addEffect(ItemEffect effect) {
        ITEM_EFFECTS.computeIfAbsent(effect.itemId, k -> new ArrayList<>())
                .add(effect);
    }
    
    /**
     * 获取道具的所有效果
     */
    public static List<ItemEffect> getEffects(int itemId) {
        return ITEM_EFFECTS.getOrDefault(itemId, Collections.emptyList());
    }
    
    /**
     * 获取道具的指定类型效果
     */
    public static List<ItemEffect> getEffects(int itemId, EffectType type) {
        List<ItemEffect> effects = getEffects(itemId);
        List<ItemEffect> result = new ArrayList<>();
        for (ItemEffect effect : effects) {
            if (effect.effectType == type) {
                result.add(effect);
            }
        }
        return result;
    }
    
    /**
     * 检查道具是否影响伤害计算
     */
    public static boolean affectsDamageCalculation(int itemId) {
        List<ItemEffect> effects = getEffects(itemId);
        for (ItemEffect effect : effects) {
            switch (effect.effectType) {
                case ATTACK_MULTIPLIER:
                case SP_ATTACK_MULTIPLIER:
                case POWER_MULTIPLIER:
                case DAMAGE_MULTIPLIER:
                case DEFENSE_MULTIPLIER:
                case SP_DEFENSE_MULTIPLIER:
                case SUPER_EFFECTIVE_BOOST:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
    
    /**
     * 获取所有影响伤害的道具ID列表
     */
    public static Set<Integer> getDamageAffectingItems() {
        Set<Integer> result = new HashSet<>();
        for (Map.Entry<Integer, List<ItemEffect>> entry : ITEM_EFFECTS.entrySet()) {
            if (affectsDamageCalculation(entry.getKey())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
