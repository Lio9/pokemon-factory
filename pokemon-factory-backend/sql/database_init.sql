-- ============================================================
-- 宝可梦工厂 - 数据库初始化脚本
-- 整合所有表结构和基础数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 一、基础数据表 (无外键依赖)
-- ==========================================

-- 世代表
DROP TABLE IF EXISTS `generation`;
CREATE TABLE `generation` (
    `id` INT PRIMARY KEY COMMENT '世代ID',
    `name` VARCHAR(20) NOT NULL COMMENT '世代名称(中文)',
    `name_en` VARCHAR(20) NOT NULL UNIQUE COMMENT '世代名称(英文)',
    `region` VARCHAR(50) COMMENT '对应地区',
    `release_year` INT COMMENT '发布年份',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世代表';

-- 属性表
DROP TABLE IF EXISTS `type`;
CREATE TABLE `type` (
    `id` INT PRIMARY KEY COMMENT '属性ID',
    `name` VARCHAR(20) NOT NULL UNIQUE COMMENT '属性名称(中文)',
    `name_en` VARCHAR(20) NOT NULL UNIQUE COMMENT '属性名称(英文)',
    `name_jp` VARCHAR(20) COMMENT '属性名称(日文)',
    `color` VARCHAR(20) COMMENT '属性颜色(十六进制)',
    `icon_url` VARCHAR(500) COMMENT '属性图标URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性表';

-- 属性相性表 (伤害倍率)
DROP TABLE IF EXISTS `type_efficacy`;
CREATE TABLE `type_efficacy` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `attacking_type_id` INT NOT NULL COMMENT '攻击方属性ID',
    `defending_type_id` INT NOT NULL COMMENT '防御方属性ID',
    `damage_factor` INT NOT NULL COMMENT '伤害倍率(250=2.5倍, 200=2倍, 100=1倍, 50=0.5倍, 0=无效)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attacking_type_id`) REFERENCES `type`(`id`),
    FOREIGN KEY (`defending_type_id`) REFERENCES `type`(`id`),
    UNIQUE KEY `uk_type_matchup` (`attacking_type_id`, `defending_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性相性表';

-- 特性表
DROP TABLE IF EXISTS `ability`;
CREATE TABLE `ability` (
    `id` INT PRIMARY KEY COMMENT '特性ID',
    `name` VARCHAR(50) NOT NULL COMMENT '特性名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '特性名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '特性名称(日文)',
    `description` TEXT COMMENT '特性描述(中文)',
    `description_en` TEXT COMMENT '特性描述(英文)',
    `effect_detail` TEXT COMMENT '详细效果说明',
    `generation_id` INT COMMENT '首次出现世代',
    `is_main_series` TINYINT(1) DEFAULT 1 COMMENT '是否为主系列特性',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`generation_id`) REFERENCES `generation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特性表';

-- 技能伤害类型表
DROP TABLE IF EXISTS `move_damage_class`;
CREATE TABLE `move_damage_class` (
    `id` INT PRIMARY KEY COMMENT '伤害类型ID',
    `name` VARCHAR(20) NOT NULL COMMENT '伤害类型名称(中文)',
    `name_en` VARCHAR(20) NOT NULL UNIQUE COMMENT '伤害类型名称(英文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能伤害类型表';

-- 种族值类型表
DROP TABLE IF EXISTS `stat`;
CREATE TABLE `stat` (
    `id` INT PRIMARY KEY COMMENT '能力值ID',
    `name` VARCHAR(20) NOT NULL COMMENT '能力值名称(中文)',
    `name_en` VARCHAR(20) NOT NULL UNIQUE COMMENT '能力值名称(英文)',
    `name_jp` VARCHAR(20) COMMENT '能力值名称(日文)',
    `is_battle_only` TINYINT(1) DEFAULT 0 COMMENT '是否仅对战有效',
    `game_index` INT COMMENT '游戏内索引',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='种族值类型表';

-- 性格表
DROP TABLE IF EXISTS `nature`;
CREATE TABLE `nature` (
    `id` INT PRIMARY KEY COMMENT '性格ID',
    `name` VARCHAR(20) NOT NULL COMMENT '性格名称(中文)',
    `name_en` VARCHAR(20) NOT NULL UNIQUE COMMENT '性格名称(英文)',
    `name_jp` VARCHAR(20) COMMENT '性格名称(日文)',
    `increased_stat` VARCHAR(20) COMMENT '提升的能力',
    `decreased_stat` VARCHAR(20) COMMENT '降低的能力',
    `likes_flavor` VARCHAR(20) COMMENT '喜欢的口味',
    `hates_flavor` VARCHAR(20) COMMENT '讨厌的口味',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性格表';

-- 蛋群表
DROP TABLE IF EXISTS `egg_group`;
CREATE TABLE `egg_group` (
    `id` INT PRIMARY KEY COMMENT '蛋群ID',
    `name` VARCHAR(30) NOT NULL COMMENT '蛋群名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '蛋群名称(英文)',
    `name_jp` VARCHAR(30) COMMENT '蛋群名称(日文)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蛋群表';

-- 经验成长类型表
DROP TABLE IF EXISTS `growth_rate`;
CREATE TABLE `growth_rate` (
    `id` INT PRIMARY KEY COMMENT '成长类型ID',
    `name` VARCHAR(30) NOT NULL COMMENT '成长类型名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '成长类型名称(英文)',
    `formula` VARCHAR(200) COMMENT '经验公式',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经验成长类型表';

-- 技能学习方式表
DROP TABLE IF EXISTS `move_learn_method`;
CREATE TABLE `move_learn_method` (
    `id` INT PRIMARY KEY COMMENT '学习方式ID',
    `name` VARCHAR(30) NOT NULL COMMENT '学习方式名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '学习方式名称(英文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能学习方式表';

-- 进化触发条件表
DROP TABLE IF EXISTS `evolution_trigger`;
CREATE TABLE `evolution_trigger` (
    `id` INT PRIMARY KEY COMMENT '进化触发ID',
    `name` VARCHAR(30) NOT NULL COMMENT '触发条件名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '触发条件名称(英文)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进化触发条件表';

-- 异常状态表
DROP TABLE IF EXISTS `move_meta_ailment`;
CREATE TABLE `move_meta_ailment` (
    `id` INT PRIMARY KEY COMMENT '异常状态ID',
    `name` VARCHAR(30) NOT NULL COMMENT '状态名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '状态名称(英文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能异常状态表';

-- 技能元数据类别表
DROP TABLE IF EXISTS `move_meta_category`;
CREATE TABLE `move_meta_category` (
    `id` INT PRIMARY KEY COMMENT '类别ID',
    `name` VARCHAR(50) NOT NULL COMMENT '类别名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '类别名称(英文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能元数据类别表';

-- 技能目标类型表
DROP TABLE IF EXISTS `move_target`;
CREATE TABLE `move_target` (
    `id` INT PRIMARY KEY COMMENT '目标类型ID',
    `name` VARCHAR(50) NOT NULL COMMENT '目标类型名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '目标类型名称(英文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能目标类型表';

-- 物品口袋表
DROP TABLE IF EXISTS `item_pocket`;
CREATE TABLE `item_pocket` (
    `id` INT PRIMARY KEY COMMENT '口袋ID',
    `name` VARCHAR(30) NOT NULL COMMENT '口袋名称(中文)',
    `name_en` VARCHAR(30) NOT NULL UNIQUE COMMENT '口袋名称(英文)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品口袋表';

-- 物品投掷效果表
DROP TABLE IF EXISTS `item_fling_effect`;
CREATE TABLE `item_fling_effect` (
    `id` INT PRIMARY KEY COMMENT '投掷效果ID',
    `name` VARCHAR(50) NOT NULL COMMENT '效果名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '效果名称(英文)',
    `description` TEXT COMMENT '效果描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品投掷效果表';

-- 物品属性表
DROP TABLE IF EXISTS `item_flag`;
CREATE TABLE `item_flag` (
    `id` INT PRIMARY KEY COMMENT '属性ID',
    `name` VARCHAR(50) NOT NULL COMMENT '属性名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '属性名称(英文)',
    `description` TEXT COMMENT '属性描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品属性表';

-- ==========================================
-- 二、版本相关表
-- ==========================================

-- 版本组表
DROP TABLE IF EXISTS `version_group`;
CREATE TABLE `version_group` (
    `id` INT PRIMARY KEY COMMENT '版本组ID',
    `name` VARCHAR(50) NOT NULL COMMENT '版本组名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '版本组名称(英文)',
    `generation_id` INT COMMENT '所属世代',
    `order` INT COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`generation_id`) REFERENCES `generation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本组表';

-- 物品分类表
DROP TABLE IF EXISTS `item_category`;
CREATE TABLE `item_category` (
    `id` INT PRIMARY KEY COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称(英文)',
    `pocket_id` INT COMMENT '所属口袋ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pocket_id`) REFERENCES `item_pocket`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品分类表';

-- ==========================================
-- 三、技能与物品表
-- ==========================================

-- 技能表
DROP TABLE IF EXISTS `move`;
CREATE TABLE `move` (
    `id` INT PRIMARY KEY COMMENT '技能ID',
    `name` VARCHAR(50) NOT NULL COMMENT '技能名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '技能名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '技能名称(日文)',
    `type_id` INT NOT NULL COMMENT '属性ID',
    `damage_class_id` INT COMMENT '伤害类型ID',
    `target_id` INT COMMENT '目标类型ID',
    `power` INT COMMENT '威力',
    `pp` INT COMMENT 'PP值',
    `accuracy` INT COMMENT '命中率',
    `priority` INT DEFAULT 0 COMMENT '优先度',
    `effect_chance` INT COMMENT '追加效果触发率',
    `effect_short` VARCHAR(500) COMMENT '效果简述',
    `effect_detail` TEXT COMMENT '详细效果',
    `description` TEXT COMMENT '技能描述',
    `generation_id` INT COMMENT '首次出现世代',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`type_id`) REFERENCES `type`(`id`),
    FOREIGN KEY (`damage_class_id`) REFERENCES `move_damage_class`(`id`),
    FOREIGN KEY (`target_id`) REFERENCES `move_target`(`id`),
    FOREIGN KEY (`generation_id`) REFERENCES `generation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能表';

-- 技能元数据表 (连击、吸取等)
DROP TABLE IF EXISTS `move_meta`;
CREATE TABLE `move_meta` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `move_id` INT NOT NULL COMMENT '技能ID',
    `min_hits` INT COMMENT '最小连击数',
    `max_hits` INT COMMENT '最大连击数',
    `min_turns` INT COMMENT '最小持续回合',
    `max_turns` INT COMMENT '最大持续回合',
    `drain` INT COMMENT '吸取量',
    `healing` INT COMMENT '回复量',
    `crit_rate` INT COMMENT '暴击率加成',
    `ailment_chance` INT COMMENT '异常状态几率',
    `flinch_chance` INT COMMENT '畏缩几率',
    `stat_chance` INT COMMENT '能力变化几率',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_move_meta` (`move_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能元数据表';

-- 技能能力变化表
DROP TABLE IF EXISTS `move_meta_stat_change`;
CREATE TABLE `move_meta_stat_change` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `move_id` INT NOT NULL COMMENT '技能ID',
    `stat_id` INT NOT NULL COMMENT '能力值ID',
    `change` INT NOT NULL COMMENT '变化量(正为提升,负为降低)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`stat_id`) REFERENCES `stat`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能能力变化表';

-- 技能标记表 (定义技能特性标记)
DROP TABLE IF EXISTS `move_flags`;
CREATE TABLE `move_flags` (
    `id` INT PRIMARY KEY COMMENT '标记ID',
    `identifier` VARCHAR(50) NOT NULL UNIQUE COMMENT '标记标识符',
    `name` VARCHAR(50) COMMENT '标记名称(中文)',
    `description` VARCHAR(200) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能标记表';

-- 技能-标记关联表
DROP TABLE IF EXISTS `move_flag_map`;
CREATE TABLE `move_flag_map` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `move_id` INT NOT NULL COMMENT '技能ID',
    `flag_id` INT NOT NULL COMMENT '标记ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`flag_id`) REFERENCES `move_flags`(`id`),
    UNIQUE KEY `uk_move_flag` (`move_id`, `flag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能-标记关联表';

-- 物品表
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
    `id` INT PRIMARY KEY COMMENT '物品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '物品名称(中文)',
    `name_en` VARCHAR(100) NOT NULL UNIQUE COMMENT '物品名称(英文)',
    `name_jp` VARCHAR(100) COMMENT '物品名称(日文)',
    `category_id` INT COMMENT '分类ID',
    `cost` INT DEFAULT 0 COMMENT '购买价格',
    `fling_power` INT COMMENT '投掷威力',
    `fling_effect_id` INT COMMENT '投掷效果ID',
    `effect_short` VARCHAR(500) COMMENT '效果简述',
    `effect_detail` TEXT COMMENT '详细效果',
    `description` TEXT COMMENT '物品描述',
    `generation_id` INT COMMENT '首次出现世代',
    `sprite_url` VARCHAR(500) COMMENT '图标URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `item_category`(`id`),
    FOREIGN KEY (`fling_effect_id`) REFERENCES `item_fling_effect`(`id`),
    FOREIGN KEY (`generation_id`) REFERENCES `generation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- 物品-属性关联表
DROP TABLE IF EXISTS `item_flag_map`;
CREATE TABLE `item_flag_map` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `item_id` INT NOT NULL COMMENT '物品ID',
    `flag_id` INT NOT NULL COMMENT '属性ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`item_id`) REFERENCES `item`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`flag_id`) REFERENCES `item_flag`(`id`),
    UNIQUE KEY `uk_item_flag` (`item_id`, `flag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品属性关联表';

-- ==========================================
-- 四、宝可梦核心表
-- ==========================================

-- 进化链表
DROP TABLE IF EXISTS `evolution_chain`;
CREATE TABLE `evolution_chain` (
    `id` INT PRIMARY KEY COMMENT '进化链ID',
    `baby_trigger_item_id` INT COMMENT '触发幼崽进化的物品ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`baby_trigger_item_id`) REFERENCES `item`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进化链表';

-- 宝可梦物种表 (图鉴核心)
DROP TABLE IF EXISTS `pokemon_species`;
CREATE TABLE `pokemon_species` (
    `id` INT PRIMARY KEY COMMENT '物种ID(全国图鉴编号)',
    `name` VARCHAR(50) NOT NULL COMMENT '物种名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '物种名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '物种名称(日文)',
    `genus` VARCHAR(50) COMMENT '分类(如"种子宝可梦")',
    `generation_id` INT COMMENT '首次出现世代',
    `evolution_chain_id` INT COMMENT '进化链ID',
    `evolves_from_species_id` INT COMMENT '进化前物种ID',
    `color` VARCHAR(20) COMMENT '主色调',
    `shape` VARCHAR(30) COMMENT '体型',
    `habitat` VARCHAR(30) COMMENT '栖息地',
    `growth_rate_id` INT COMMENT '成长类型ID',
    `gender_rate` INT DEFAULT -1 COMMENT '性别比例(-1无性别,0全雄,8全雌)',
    `capture_rate` INT DEFAULT 0 COMMENT '捕获率',
    `base_happiness` INT DEFAULT 70 COMMENT '基础亲密度',
    `hatch_counter` INT COMMENT '孵化步数',
    `is_baby` TINYINT(1) DEFAULT 0 COMMENT '是否为幼崽',
    `is_legendary` TINYINT(1) DEFAULT 0 COMMENT '是否为传说',
    `is_mythical` TINYINT(1) DEFAULT 0 COMMENT '是否为神话',
    `has_gender_differences` TINYINT(1) DEFAULT 0 COMMENT '是否有性别差异',
    `forms_switchable` TINYINT(1) DEFAULT 0 COMMENT '形态是否可切换',
    `order` INT COMMENT '排序序号',
    `description` TEXT COMMENT '图鉴描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`generation_id`) REFERENCES `generation`(`id`),
    FOREIGN KEY (`evolution_chain_id`) REFERENCES `evolution_chain`(`id`),
    FOREIGN KEY (`evolves_from_species_id`) REFERENCES `pokemon_species`(`id`),
    FOREIGN KEY (`growth_rate_id`) REFERENCES `growth_rate`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦物种表';

-- 宝可梦形态表 (对战核心)
-- 说明: 这是实际参与对战的实体，包含具体属性、特性、种族值
DROP TABLE IF EXISTS `pokemon_form`;
CREATE TABLE `pokemon_form` (
    `id` INT PRIMARY KEY COMMENT '形态ID',
    `species_id` INT NOT NULL COMMENT '物种ID',
    `form_name` VARCHAR(50) COMMENT '形态名称(英文)',
    `form_name_zh` VARCHAR(50) COMMENT '形态名称(中文)',
    `form_name_jp` VARCHAR(50) COMMENT '形态名称(日文)',
    `is_default` TINYINT(1) DEFAULT 1 COMMENT '是否为默认形态',
    `is_battle_only` TINYINT(1) DEFAULT 0 COMMENT '是否仅对战形态',
    `is_mega` TINYINT(1) DEFAULT 0 COMMENT '是否为Mega进化',
    `is_gigantamax` TINYINT(1) DEFAULT 0 COMMENT '是否为极巨化',
    `is_terastal` TINYINT(1) DEFAULT 0 COMMENT '是否为太晶化',
    `height` DECIMAL(5,2) COMMENT '身高(米)',
    `weight` DECIMAL(6,2) COMMENT '体重(公斤)',
    `base_experience` INT COMMENT '基础经验值',
    `order` INT COMMENT '排序序号',
    `sprite_url` VARCHAR(500) COMMENT '正面图片URL',
    `sprite_back_url` VARCHAR(500) COMMENT '背面图片URL',
    `sprite_shiny_url` VARCHAR(500) COMMENT '闪光正面图片URL',
    `sprite_shiny_back_url` VARCHAR(500) COMMENT '闪光背面图片URL',
    `official_artwork_url` VARCHAR(500) COMMENT '官方立绘URL',
    `cry_url` VARCHAR(500) COMMENT '叫声音频URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`species_id`) REFERENCES `pokemon_species`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦形态表';

-- ==========================================
-- 五、宝可梦形态关联表
-- ==========================================

-- 形态-属性关联表
DROP TABLE IF EXISTS `pokemon_form_type`;
CREATE TABLE `pokemon_form_type` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `form_id` INT NOT NULL COMMENT '形态ID',
    `type_id` INT NOT NULL COMMENT '属性ID',
    `slot` TINYINT NOT NULL COMMENT '属性槽位(1或2)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`type_id`) REFERENCES `type`(`id`),
    UNIQUE KEY `uk_form_type_slot` (`form_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='形态-属性关联表';

-- 形态-特性关联表
DROP TABLE IF EXISTS `pokemon_form_ability`;
CREATE TABLE `pokemon_form_ability` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `form_id` INT NOT NULL COMMENT '形态ID',
    `ability_id` INT NOT NULL COMMENT '特性ID',
    `is_hidden` TINYINT(1) DEFAULT 0 COMMENT '是否为隐藏特性',
    `slot` TINYINT NOT NULL COMMENT '特性槽位(1-3)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`ability_id`) REFERENCES `ability`(`id`),
    UNIQUE KEY `uk_form_ability_slot` (`form_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='形态-特性关联表';

-- 形态种族值表
DROP TABLE IF EXISTS `pokemon_form_stat`;
CREATE TABLE `pokemon_form_stat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `form_id` INT NOT NULL COMMENT '形态ID',
    `stat_id` INT NOT NULL COMMENT '能力值ID',
    `base_stat` INT NOT NULL COMMENT '基础种族值',
    `effort` INT DEFAULT 0 COMMENT '击败后获得的努力值',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`stat_id`) REFERENCES `stat`(`id`),
    UNIQUE KEY `uk_form_stat` (`form_id`, `stat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='形态种族值表';

-- 物种-蛋群关联表
DROP TABLE IF EXISTS `pokemon_species_egg_group`;
CREATE TABLE `pokemon_species_egg_group` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `species_id` INT NOT NULL COMMENT '物种ID',
    `egg_group_id` INT NOT NULL COMMENT '蛋群ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`species_id`) REFERENCES `pokemon_species`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`egg_group_id`) REFERENCES `egg_group`(`id`),
    UNIQUE KEY `uk_species_egg_group` (`species_id`, `egg_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种-蛋群关联表';

-- ==========================================
-- 六、进化详情表
-- ==========================================

-- 进化详情表
DROP TABLE IF EXISTS `pokemon_evolution`;
CREATE TABLE `pokemon_evolution` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `evolved_species_id` INT NOT NULL COMMENT '进化后物种ID',
    `evolves_from_species_id` INT NOT NULL COMMENT '进化前物种ID',
    `evolution_trigger_id` INT NOT NULL COMMENT '进化触发ID',
    `min_level` INT COMMENT '最低等级',
    `min_happiness` INT COMMENT '最低亲密度',
    `min_affection` INT COMMENT '最低好感度',
    `min_beauty` INT COMMENT '最低美丽度',
    `time_of_day` VARCHAR(20) COMMENT '时间段(day/night)',
    `held_item_id` INT COMMENT '持有物品ID',
    `evolution_item_id` INT COMMENT '进化物品ID',
    `known_move_id` INT COMMENT '已知技能ID',
    `known_move_type_id` INT COMMENT '已知技能属性ID',
    `location_id` INT COMMENT '地点ID',
    `party_species_id` INT COMMENT '队伍中宝可梦物种ID',
    `party_type_id` INT COMMENT '队伍中宝可梦属性ID',
    `trade_species_id` INT COMMENT '交换宝可梦物种ID',
    `needs_overworld_rain` TINYINT(1) DEFAULT 0 COMMENT '需要下雨天气',
    `needs_multiplayer` TINYINT(1) DEFAULT 0 COMMENT '需要多人连接',
    `turn_upside_down` TINYINT(1) DEFAULT 0 COMMENT '需要倒置设备',
    `relative_physical_stats` INT COMMENT '攻防相对值',
    `gender_id` INT COMMENT '性别要求',
    `region_id` INT COMMENT '地区形态进化区域',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`evolved_species_id`) REFERENCES `pokemon_species`(`id`),
    FOREIGN KEY (`evolves_from_species_id`) REFERENCES `pokemon_species`(`id`),
    FOREIGN KEY (`evolution_trigger_id`) REFERENCES `evolution_trigger`(`id`),
    FOREIGN KEY (`held_item_id`) REFERENCES `item`(`id`),
    FOREIGN KEY (`evolution_item_id`) REFERENCES `item`(`id`),
    FOREIGN KEY (`known_move_id`) REFERENCES `move`(`id`),
    FOREIGN KEY (`known_move_type_id`) REFERENCES `type`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进化详情表';

-- ==========================================
-- 七、技能学习表
-- ==========================================

-- 形态可学技能表
DROP TABLE IF EXISTS `pokemon_form_move`;
CREATE TABLE `pokemon_form_move` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `form_id` INT NOT NULL COMMENT '形态ID',
    `move_id` INT NOT NULL COMMENT '技能ID',
    `learn_method_id` INT NOT NULL COMMENT '学习方式ID',
    `level` INT COMMENT '学习等级(升级学习时)',
    `version_group_id` INT COMMENT '版本组ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`),
    FOREIGN KEY (`learn_method_id`) REFERENCES `move_learn_method`(`id`),
    FOREIGN KEY (`version_group_id`) REFERENCES `version_group`(`id`),
    INDEX `idx_form_move` (`form_id`, `move_id`),
    INDEX `idx_form_learn` (`form_id`, `learn_method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='形态可学技能表';

-- ==========================================
-- 八、对战队伍表
-- ==========================================

-- 队伍表
DROP TABLE IF EXISTS `battle_team`;
CREATE TABLE `battle_team` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT COMMENT '用户ID(预留)',
    `name` VARCHAR(100) COMMENT '队伍名称',
    `format` VARCHAR(50) COMMENT '对战格式(如VGC2024, OU)',
    `description` TEXT COMMENT '队伍描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对战队伍表';

-- 队伍成员表 (具体宝可梦实例)
DROP TABLE IF EXISTS `battle_team_member`;
CREATE TABLE `battle_team_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL COMMENT '队伍ID',
    `form_id` INT NOT NULL COMMENT '形态ID',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `level` INT DEFAULT 50 COMMENT '等级',
    `gender` VARCHAR(10) COMMENT '性别(male/female/none)',
    `shiny` TINYINT(1) DEFAULT 0 COMMENT '是否闪光',
    `nature_id` INT COMMENT '性格ID',
    `ability_id` INT COMMENT '特性ID(可覆盖默认)',
    `held_item_id` INT COMMENT '持有物品ID',
    `tera_type_id` INT COMMENT '太晶属性ID',
    -- 个体值
    `iv_hp` INT DEFAULT 31 COMMENT 'HP个体值',
    `iv_attack` INT DEFAULT 31 COMMENT '攻击个体值',
    `iv_defense` INT DEFAULT 31 COMMENT '防御个体值',
    `iv_sp_attack` INT DEFAULT 31 COMMENT '特攻个体值',
    `iv_sp_defense` INT DEFAULT 31 COMMENT '特防个体值',
    `iv_speed` INT DEFAULT 31 COMMENT '速度个体值',
    -- 努力值
    `ev_hp` INT DEFAULT 0 COMMENT 'HP努力值',
    `ev_attack` INT DEFAULT 0 COMMENT '攻击努力值',
    `ev_defense` INT DEFAULT 0 COMMENT '防御努力值',
    `ev_sp_attack` INT DEFAULT 0 COMMENT '特攻努力值',
    `ev_sp_defense` INT DEFAULT 0 COMMENT '特防努力值',
    `ev_speed` INT DEFAULT 0 COMMENT '速度努力值',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`team_id`) REFERENCES `battle_team`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`),
    FOREIGN KEY (`nature_id`) REFERENCES `nature`(`id`),
    FOREIGN KEY (`ability_id`) REFERENCES `ability`(`id`),
    FOREIGN KEY (`held_item_id`) REFERENCES `item`(`id`),
    FOREIGN KEY (`tera_type_id`) REFERENCES `type`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍成员表';

-- 队伍成员技能表
DROP TABLE IF EXISTS `battle_team_member_move`;
CREATE TABLE `battle_team_member_move` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `member_id` BIGINT NOT NULL COMMENT '成员ID',
    `move_id` INT NOT NULL COMMENT '技能ID',
    `slot` TINYINT NOT NULL COMMENT '技能槽位(1-4)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`member_id`) REFERENCES `battle_team_member`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`),
    UNIQUE KEY `uk_member_move_slot` (`member_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍成员技能表';

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 九、视图
-- ==========================================

-- 属性相性视图 (计算伤害用)
DROP VIEW IF EXISTS `v_type_matchup`;
CREATE VIEW `v_type_matchup` AS
SELECT 
    atk.id as attacking_type_id,
    atk.name as attacking_type_name,
    def.id as defending_type_id,
    def.name as defending_type_name,
    te.damage_factor,
    CASE te.damage_factor
        WHEN 0 THEN '无效'
        WHEN 25 THEN '1/4'
        WHEN 50 THEN '1/2'
        WHEN 100 THEN '1'
        WHEN 200 THEN '2'
        WHEN 400 THEN '4'
        ELSE CONCAT(te.damage_factor/100)
    END as damage_multiplier
FROM `type` atk
CROSS JOIN `type` def
LEFT JOIN `type_efficacy` te ON te.attacking_type_id = atk.id AND te.defending_type_id = def.id
ORDER BY atk.id, def.id;

-- 宝可梦完整信息视图 (图鉴用)
DROP VIEW IF EXISTS `v_pokedex_entry`;
CREATE VIEW `v_pokedex_entry` AS
SELECT 
    ps.id as pokedex_number,
    ps.name as name,
    ps.name_en,
    ps.name_jp,
    ps.genus,
    ps.generation_id,
    ps.is_legendary,
    ps.is_mythical,
    ps.is_baby,
    pf.id as form_id,
    pf.form_name,
    pf.form_name_zh,
    pf.is_default,
    pf.height,
    pf.weight,
    pf.base_experience,
    -- 属性
    GROUP_CONCAT(DISTINCT t.name ORDER BY pft.slot SEPARATOR '/') as types,
    -- 特性
    GROUP_CONCAT(DISTINCT 
        CONCAT(a.name, IF(pfa.is_hidden, '(隐藏)', ''))
        ORDER BY pfa.slot SEPARATOR ', '
    ) as abilities,
    -- 种族值
    MAX(CASE WHEN st.name_en = 'hp' THEN pfs.base_stat END) as hp,
    MAX(CASE WHEN st.name_en = 'attack' THEN pfs.base_stat END) as attack,
    MAX(CASE WHEN st.name_en = 'defense' THEN pfs.base_stat END) as defense,
    MAX(CASE WHEN st.name_en = 'special-attack' THEN pfs.base_stat END) as sp_attack,
    MAX(CASE WHEN st.name_en = 'special-defense' THEN pfs.base_stat END) as sp_defense,
    MAX(CASE WHEN st.name_en = 'speed' THEN pfs.base_stat END) as speed,
    -- 总种族值
    SUM(pfs.base_stat) as base_stat_total,
    -- 图片
    pf.sprite_url,
    pf.official_artwork_url
FROM pokemon_species ps
JOIN pokemon_form pf ON pf.species_id = ps.id
LEFT JOIN pokemon_form_type pft ON pft.form_id = pf.id
LEFT JOIN `type` t ON t.id = pft.type_id
LEFT JOIN pokemon_form_ability pfa ON pfa.form_id = pf.id
LEFT JOIN ability a ON a.id = pfa.ability_id
LEFT JOIN pokemon_form_stat pfs ON pfs.form_id = pf.id
LEFT JOIN stat st ON st.id = pfs.stat_id
GROUP BY ps.id, pf.id;

-- ==========================================
-- 十、基础数据初始化
-- ==========================================

-- 世代数据
INSERT INTO `generation` (`id`, `name`, `name_en`, `region`, `release_year`) VALUES
(1, '第一世代', 'generation-i', 'kanto', 1996),
(2, '第二世代', 'generation-ii', 'johto', 1999),
(3, '第三世代', 'generation-iii', 'hoenn', 2002),
(4, '第四世代', 'generation-iv', 'sinnoh', 2006),
(5, '第五世代', 'generation-v', 'unova', 2010),
(6, '第六世代', 'generation-vi', 'kalos', 2013),
(7, '第七世代', 'generation-vii', 'alola', 2016),
(8, '第八世代', 'generation-viii', 'galar', 2019),
(9, '第九世代', 'generation-ix', 'paldea', 2022);

-- 属性数据
INSERT INTO `type` (`id`, `name`, `name_en`, `name_jp`, `color`) VALUES
(1, '一般', 'normal', 'ノーマル', '#A8A878'),
(2, '格斗', 'fighting', 'かくとう', '#C03028'),
(3, '飞行', 'flying', 'ひこう', '#A890F0'),
(4, '毒', 'poison', 'どく', '#A040A0'),
(5, '地面', 'ground', 'じめん', '#E0C068'),
(6, '岩石', 'rock', 'いわ', '#B8A038'),
(7, '虫', 'bug', 'むし', '#A8B820'),
(8, '幽灵', 'ghost', 'ゴースト', '#705898'),
(9, '钢', 'steel', 'はがね', '#B8B8D0'),
(10, '火', 'fire', 'ほのお', '#F08030'),
(11, '水', 'water', 'みず', '#6890F0'),
(12, '草', 'grass', 'くさ', '#78C850'),
(13, '电', 'electric', 'でんき', '#F8D030'),
(14, '超能力', 'psychic', 'エスパー', '#F85888'),
(15, '冰', 'ice', 'こおり', '#98D8D8'),
(16, '龙', 'dragon', 'ドラゴン', '#7038F8'),
(17, '恶', 'dark', 'あく', '#705848'),
(18, '妖精', 'fairy', 'フェアリー', '#EE99AC');

-- 种族值类型
INSERT INTO `stat` (`id`, `name`, `name_en`, `name_jp`, `is_battle_only`, `game_index`) VALUES
(1, 'HP', 'hp', 'HP', 0, 1),
(2, '攻击', 'attack', 'こうげき', 0, 2),
(3, '防御', 'defense', 'ぼうぎょ', 0, 3),
(4, '特攻', 'special-attack', 'とくこう', 0, 4),
(5, '特防', 'special-defense', 'とくぼう', 0, 5),
(6, '速度', 'speed', 'すばやさ', 0, 6),
(7, '命中率', 'accuracy', 'めいちゅう', 1, 7),
(8, '闪避率', 'evasion', 'かいひ', 1, 8);

-- 技能伤害类型
INSERT INTO `move_damage_class` (`id`, `name`, `name_en`, `description`) VALUES
(1, '物理', 'physical', '伤害取决于攻击和防御'),
(2, '特殊', 'special', '伤害取决于特攻和特防'),
(3, '变化', 'status', '不造成直接伤害');

-- 技能学习方式
INSERT INTO `move_learn_method` (`id`, `name`, `name_en`, `description`) VALUES
(1, '升级', 'level-up', '通过升级学习'),
(2, '技能机', 'machine', '通过技能机学习'),
(3, '传授', 'tutor', '通过传授学习'),
(4, '遗传', 'egg', '通过遗传学习');

-- 进化触发条件
INSERT INTO `evolution_trigger` (`id`, `name`, `name_en`) VALUES
(1, '升级', 'level-up'),
(2, '交换', 'trade'),
(3, '使用物品', 'use-item'),
(4, '其他', 'other');

-- 蛋群数据
INSERT INTO `egg_group` (`id`, `name`, `name_en`, `name_jp`) VALUES
(1, '怪兽组', 'monster', 'かいぶつ'),
(2, '水中1组', 'water1', 'すいちゅう1'),
(3, '水中2组', 'water2', 'すいちゅう2'),
(4, '水中3组', 'water3', 'すいちゅう3'),
(5, '虫组', 'bug', 'むし'),
(6, '飞行组', 'flying', 'ひこう'),
(7, '陆上组', 'ground', 'りくじょう'),
(8, '妖精组', 'fairy', 'フェアリー'),
(9, '植物组', 'plant', 'しょくぶつ'),
(10, '人型组', 'human-like', 'ひとがた'),
(11, '矿物组', 'mineral', 'こうぶつ'),
(12, '不定形组', 'amorphous', 'ふていけい'),
(13, '龙组', 'dragon', 'ドラゴン'),
(14, '百变怪组', 'ditto', 'メタモン'),
(15, '未发现组', 'no-eggs', 'みはっけん');

-- 经验成长类型
INSERT INTO `growth_rate` (`id`, `name`, `name_en`, `formula`, `description`) VALUES
(1, '慢速', 'slow', '5n³/4', '升级所需经验较多'),
(2, '中速', 'medium', 'n³', '标准成长速度'),
(3, '快速', 'fast', '4n³/5', '升级所需经验较少'),
(4, '中慢速', 'medium-slow', '6n³/5 - 15n² + 100n - 140', '前期较快后期较慢'),
(5, '慢速后极快', 'slow-then-very-fast', 'n⁴/2', '初期慢后期快'),
(6, '快速后极慢', 'fast-then-very-slow', '特殊公式', '初期快后期慢');

-- 异常状态数据
INSERT INTO `move_meta_ailment` (`id`, `name`, `name_en`) VALUES
(-1, '未知', 'unknown'),
(0, '无', 'none'),
(1, '麻痹', 'paralysis'),
(2, '睡眠', 'sleep'),
(3, '冰冻', 'freeze'),
(4, '灼伤', 'burn'),
(5, '中毒', 'poison'),
(6, '混乱', 'confusion'),
(7, '着迷', 'infatuation'),
(8, '束缚', 'trap'),
(9, '噩梦', 'nightmare'),
(10, '紧束', 'torment'),
(11, '无防御', 'no-competitive-swagger'),
(12, '回复封锁', 'heal-block'),
(13, '诅咒', 'curse'),
(14, '识破', 'foresight'),
(15, '羽栖', 'perish-song');

-- 技能目标类型数据
INSERT INTO `move_target` (`id`, `name`, `name_en`, `description`) VALUES
(1, '特定宝可梦', 'specific-move', '对特定技能有效'),
(2, '选定目标', 'selected-pokemon', '对选定的宝可梦使用'),
(3, '随机目标', 'random-opponent', '随机选择对方一只宝可梦'),
(4, '全体对手', 'all-opponents', '对所有对手使用'),
(5, '全体宝可梦', 'all-pokemon', '对所有宝可梦使用'),
(6, '使用者', 'user', '对使用者自身使用'),
(7, '使用者与相邻', 'users-field', '使用者及相邻位置'),
(8, '对手场地', 'opponents-field', '对手的场地'),
(9, '使用者队伍', 'user-and-allies', '使用者及队友'),
(10, '相邻宝可梦', 'all-other-pokemon', '除自己外所有宝可梦'),
(11, '单体随机', 'single-pokemon', '单体目标');

COMMIT;

-- ==========================================
-- 七、性能优化索引
-- ==========================================

-- pokemon_species 表索引
CREATE INDEX idx_pokemon_species_name ON pokemon_species(name);
CREATE INDEX idx_pokemon_species_name_en ON pokemon_species(name_en);
CREATE INDEX idx_pokemon_species_generation_id ON pokemon_species(generation_id);
CREATE INDEX idx_pokemon_species_evolution_chain_id ON pokemon_species(evolution_chain_id);
CREATE INDEX idx_pokemon_species_evolves_from ON pokemon_species(evolves_from_species_id);
CREATE INDEX idx_pokemon_species_gen_name ON pokemon_species(generation_id, name);

-- pokemon_form 表索引
CREATE INDEX idx_pokemon_form_species_id ON pokemon_form(species_id);
CREATE INDEX idx_pokemon_form_is_default ON pokemon_form(is_default);
CREATE INDEX idx_pokemon_form_species_default ON pokemon_form(species_id, is_default);

-- pokemon_form_type 表索引
CREATE INDEX idx_pokemon_form_type_form_id ON pokemon_form_type(form_id);
CREATE INDEX idx_pokemon_form_type_type_id ON pokemon_form_type(type_id);
CREATE INDEX idx_pokemon_form_type_form_slot ON pokemon_form_type(form_id, slot);
CREATE INDEX idx_pokemon_form_type_composite ON pokemon_form_type(form_id, type_id, slot);

-- pokemon_form_ability 表索引
CREATE INDEX idx_pokemon_form_ability_form_id ON pokemon_form_ability(form_id);
CREATE INDEX idx_pokemon_form_ability_ability_id ON pokemon_form_ability(ability_id);

-- pokemon_form_stat 表索引
CREATE INDEX idx_pokemon_form_stat_form_id ON pokemon_form_stat(form_id);
CREATE INDEX idx_pokemon_form_stat_stat_id ON pokemon_form_stat(stat_id);

-- move 表索引
CREATE INDEX idx_move_name ON move(name);
CREATE INDEX idx_move_name_en ON move(name_en);
CREATE INDEX idx_move_type_id ON move(type_id);
CREATE INDEX idx_move_damage_class_id ON move(damage_class_id);

-- ability 表索引
CREATE INDEX idx_ability_name ON ability(name);
CREATE INDEX idx_ability_name_en ON ability(name_en);

-- item 表索引
CREATE INDEX idx_item_name ON item(name);
CREATE INDEX idx_item_name_en ON item(name_en);
CREATE INDEX idx_item_category_id ON item(category_id);

-- type 表索引
CREATE INDEX idx_type_name ON type(name);
CREATE INDEX idx_type_name_en ON type(name_en);

-- pokemon_evolution 表索引
CREATE INDEX idx_pokemon_evolution_evolved_species_id ON pokemon_evolution(evolved_species_id);
CREATE INDEX idx_pokemon_evolution_evolution_trigger_id ON pokemon_evolution(evolution_trigger_id);
