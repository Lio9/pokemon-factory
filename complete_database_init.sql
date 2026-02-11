-- 宝可梦数据库完整表结构初始化脚本
-- 基于 PokeAPI 数据结构设计
-- 删除并重新创建所有表

SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 基础数据表
-- ==========================================

-- 物品表
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '物品ID',
    `index_number` VARCHAR(20) COMMENT '物品编号',
    `name` VARCHAR(100) NOT NULL COMMENT '物品名称(中文)',
    `name_en` VARCHAR(100) NOT NULL UNIQUE COMMENT '物品名称(英文)',
    `name_jp` VARCHAR(100) COMMENT '物品名称(日文)',
    `category` VARCHAR(50) COMMENT '物品分类',
    `price` INT DEFAULT 0 COMMENT '物品价格',
    `effect` TEXT COMMENT '物品效果',
    `description` TEXT COMMENT '物品描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- 属性表
DROP TABLE IF EXISTS `type`;
CREATE TABLE `type` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '属性ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '属性名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '属性名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '属性名称(日文)',
    `color` VARCHAR(20) COMMENT '属性颜色',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性表';

-- 特性表
DROP TABLE IF EXISTS `ability`;
CREATE TABLE `ability` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '特性ID',
    `index_number` VARCHAR(20) COMMENT '特性编号',
    `generation` VARCHAR(10) COMMENT '所属世代',
    `name` VARCHAR(100) NOT NULL COMMENT '特性名称(中文)',
    `name_en` VARCHAR(100) NOT NULL UNIQUE COMMENT '特性名称(英文)',
    `name_jp` VARCHAR(100) COMMENT '特性名称(日文)',
    `description` TEXT COMMENT '特性描述',
    `effect` TEXT COMMENT '特性效果',
    `common_count` INT DEFAULT 0 COMMENT '普通特性出现次数',
    `hidden_count` INT DEFAULT 0 COMMENT '隐藏特性出现次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特性表';

-- 技能表
DROP TABLE IF EXISTS `move`;
CREATE TABLE `move` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '技能ID',
    `index_number` VARCHAR(20) COMMENT '技能编号',
    `generation` VARCHAR(10) COMMENT '所属世代',
    `name` VARCHAR(100) NOT NULL COMMENT '技能名称(中文)',
    `name_en` VARCHAR(100) NOT NULL UNIQUE COMMENT '技能名称(英文)',
    `name_jp` VARCHAR(100) COMMENT '技能名称(日文)',
    `type_id` BIGINT COMMENT '属性ID',
    `power` INT COMMENT '威力',
    `pp` INT COMMENT 'PP值',
    `accuracy` INT COMMENT '命中率',
    `priority` INT DEFAULT 0 COMMENT '优先级',
    `damage_class` VARCHAR(20) COMMENT '伤害类型(physical/special/status)',
    `description` TEXT COMMENT '技能描述',
    `effect` TEXT COMMENT '技能效果',
    `effect_chance` INT COMMENT '效果几率',
    `contest_type` VARCHAR(20) COMMENT '华丽大赛类型',
    `contest_effect` TEXT COMMENT '华丽大赛效果',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`type_id`) REFERENCES `type`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能表';

-- 蛋群表
DROP TABLE IF EXISTS `egg_group`;
CREATE TABLE `egg_group` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '蛋群ID',
    `name` VARCHAR(50) NOT NULL COMMENT '蛋群名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '蛋群名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '蛋群名称(日文)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蛋群表';

-- 经验类型表
DROP TABLE IF EXISTS `growth_rate`;
CREATE TABLE `growth_rate` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '经验类型ID',
    `name` VARCHAR(50) NOT NULL COMMENT '经验类型名称(中文)',
    `name_en` VARCHAR(50) NOT NULL UNIQUE COMMENT '经验类型名称(英文)',
    `name_jp` VARCHAR(50) COMMENT '经验类型名称(日文)',
    `formula` VARCHAR(255) COMMENT '经验公式',
    `description` TEXT COMMENT '经验类型描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经验类型表';

-- ==========================================
-- 核心数据表
-- ==========================================

-- 宝可梦主表
DROP TABLE IF EXISTS `pokemon`;
CREATE TABLE `pokemon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '宝可梦ID',
    `index_number` VARCHAR(20) NOT NULL UNIQUE COMMENT '全国图鉴编号',
    `name` VARCHAR(100) NOT NULL COMMENT '宝可梦名称(中文)',
    `name_en` VARCHAR(100) NOT NULL UNIQUE COMMENT '宝可梦名称(英文)',
    `name_jp` VARCHAR(100) COMMENT '宝可梦名称(日文)',
    `height` DECIMAL(5,2) COMMENT '身高(米)',
    `weight` DECIMAL(6,2) COMMENT '体重(公斤)',
    `base_experience` INT DEFAULT 0 COMMENT '基础经验值',
    `base_happiness` INT DEFAULT 70 COMMENT '基础亲密度',
    `capture_rate` INT DEFAULT 0 COMMENT '捕获率',
    `gender_rate` INT DEFAULT -1 COMMENT '性别比率(-1=无性别,0=全雄,8=全雌)',
    `evolution_chain_id` BIGINT COMMENT '进化链ID',
    `generation_id` INT DEFAULT 1 COMMENT '世代ID',
    `order` INT COMMENT '排序',
    `is_baby` TINYINT(1) DEFAULT 0 COMMENT '是否为婴儿宝可梦',
    `is_legendary` TINYINT(1) DEFAULT 0 COMMENT '是否为传说宝可梦',
    `is_mythical` TINYINT(1) DEFAULT 0 COMMENT '是否为神话宝可梦',
    `profile` TEXT COMMENT '宝可梦描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦主表';

-- 宝可梦形态表
DROP TABLE IF EXISTS `pokemon_form`;
CREATE TABLE `pokemon_form` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '形态ID',
    `pokemon_id` BIGINT NOT NULL COMMENT '宝可梦ID',
    `name` VARCHAR(100) NOT NULL COMMENT '形态名称',
    `index_number` VARCHAR(20) NOT NULL COMMENT '形态编号',
    `form_name` VARCHAR(100) COMMENT '形态名称(英文)',
    `form_name_jp` VARCHAR(100) COMMENT '形态名称(日文)',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为默认形态',
    `is_battle_only` TINYINT(1) DEFAULT 0 COMMENT '是否仅为对战形态',
    `is_mega` TINYINT(1) DEFAULT 0 COMMENT '是否为mega进化',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pokemon_id`) REFERENCES `pokemon`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦形态表';

-- ==========================================
-- 关联关系表
-- ==========================================

-- 宝可梦形态-属性关联表
DROP TABLE IF EXISTS `pokemon_form_type`;
CREATE TABLE `pokemon_form_type` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `form_id` BIGINT NOT NULL COMMENT '形态ID',
    `type_id` BIGINT NOT NULL COMMENT '属性ID',
    `slot` INT NOT NULL COMMENT '槽位(1或2)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`type_id`) REFERENCES `type`(`id`),
    UNIQUE KEY `uk_form_type_slot` (`form_id`, `type_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦形态属性关联表';

-- 宝可梦形态-特性关联表
DROP TABLE IF EXISTS `pokemon_form_ability`;
CREATE TABLE `pokemon_form_ability` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `form_id` BIGINT NOT NULL COMMENT '形态ID',
    `ability_id` BIGINT NOT NULL COMMENT '特性ID',
    `is_hidden` TINYINT(1) DEFAULT 0 COMMENT '是否为隐藏特性',
    `slot` INT NOT NULL COMMENT '槽位',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`ability_id`) REFERENCES `ability`(`id`),
    UNIQUE KEY `uk_form_ability_slot` (`form_id`, `ability_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦形态特性关联表';

-- 宝可梦-技能关联表
DROP TABLE IF EXISTS `pokemon_move`;
CREATE TABLE `pokemon_move` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `pokemon_id` BIGINT NOT NULL COMMENT '宝可梦ID(使用形态ID)',
    `move_id` BIGINT NOT NULL COMMENT '技能ID',
    `learn_method` VARCHAR(50) NOT NULL COMMENT '学习方式(level-up,machine,tutor,egg)',
    `level` INT COMMENT '学习等级(仅level-up方式)',
    `version_group` VARCHAR(50) COMMENT '版本组',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pokemon_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`move_id`) REFERENCES `move`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦技能关联表';

-- 宝可梦-蛋群关联表
DROP TABLE IF EXISTS `pokemon_egg_group`;
CREATE TABLE `pokemon_egg_group` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `pokemon_id` BIGINT NOT NULL COMMENT '宝可梦ID',
    `egg_group_id` BIGINT NOT NULL COMMENT '蛋群ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pokemon_id`) REFERENCES `pokemon`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`egg_group_id`) REFERENCES `egg_group`(`id`),
    UNIQUE KEY `uk_pokemon_egg_group` (`pokemon_id`, `egg_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦蛋群关联表';

-- ==========================================
-- 数值统计表
-- ==========================================

-- 种族值表
DROP TABLE IF EXISTS `pokemon_stats`;
CREATE TABLE `pokemon_stats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    `form_id` BIGINT NOT NULL COMMENT '形态ID',
    `hp` INT DEFAULT 0 COMMENT 'HP种族值',
    `attack` INT DEFAULT 0 COMMENT '攻击种族值',
    `defense` INT DEFAULT 0 COMMENT '防御种族值',
    `special_attack` INT DEFAULT 0 COMMENT '特攻种族值',
    `special_defense` INT DEFAULT 0 COMMENT '特防种族值',
    `speed` INT DEFAULT 0 COMMENT '速度种族值',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_form_stats` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦种族值表';

-- 个体值表
DROP TABLE IF EXISTS `pokemon_iv`;
CREATE TABLE `pokemon_iv` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'IV ID',
    `pokemon_form_id` BIGINT NOT NULL COMMENT '宝可梦形态ID',
    `hp` INT DEFAULT 0 COMMENT 'HP个体值(0-31)',
    `attack` INT DEFAULT 0 COMMENT '攻击个体值(0-31)',
    `defense` INT DEFAULT 0 COMMENT '防御个体值(0-31)',
    `sp_attack` INT DEFAULT 0 COMMENT '特攻个体值(0-31)',
    `sp_defense` INT DEFAULT 0 COMMENT '特防个体值(0-31)',
    `speed` INT DEFAULT 0 COMMENT '速度个体值(0-31)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pokemon_form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦个体值表';

-- 努力值表
DROP TABLE IF EXISTS `pokemon_ev`;
CREATE TABLE `pokemon_ev` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'EV ID',
    `pokemon_form_id` BIGINT NOT NULL COMMENT '宝可梦形态ID',
    `hp` INT DEFAULT 0 COMMENT 'HP努力值(0-252)',
    `attack` INT DEFAULT 0 COMMENT '攻击努力值(0-252)',
    `defense` INT DEFAULT 0 COMMENT '防御努力值(0-252)',
    `sp_attack` INT DEFAULT 0 COMMENT '特攻努力值(0-252)',
    `sp_defense` INT DEFAULT 0 COMMENT '特防努力值(0-252)',
    `speed` INT DEFAULT 0 COMMENT '速度努力值(0-252)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`pokemon_form_id`) REFERENCES `pokemon_form`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝可梦努力值表';

-- 进化链表
DROP TABLE IF EXISTS `evolution_chain`;
CREATE TABLE `evolution_chain` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '进化链ID',
    `chain_data` JSON COMMENT '进化链JSON数据',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进化链表';

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 插入基础数据
-- ==========================================

-- 插入属性数据
INSERT INTO `type` (`name`, `name_en`, `name_jp`, `color`) VALUES
('一般', 'normal', 'ノーマル', '#A8A878'),
('火', 'fire', 'ほのお', '#F08030'),
('水', 'water', 'みず', '#6890F0'),
('电', 'electric', 'でんき', '#F8D030'),
('草', 'grass', 'くさ', '#78C850'),
('冰', 'ice', 'こおり', '#98D8D8'),
('格斗', 'fighting', 'かくとう', '#C03028'),
('毒', 'poison', 'どく', '#A040A0'),
('地面', 'ground', 'じめん', '#E0C068'),
('飞行', 'flying', 'ひこう', '#A890F0'),
('超能力', 'psychic', 'エスパー', '#F85888'),
('虫', 'bug', 'むし', '#A8B820'),
('岩石', 'rock', 'いわ', '#B8A038'),
('幽灵', 'ghost', 'ゴースト', '#705898'),
('龙', 'dragon', 'ドラゴン', '#7038F8'),
('恶', 'dark', 'あく', '#705848'),
('钢', 'steel', 'はがね', '#B8B8D0'),
('妖精', 'fairy', 'フェアリー', '#EE99AC');

-- 插入蛋群数据
INSERT INTO `egg_group` (`name`, `name_en`, `name_jp`) VALUES
('怪兽组', 'monster', 'かいぶつグループ'),
('水中1组', 'water1', 'すいちゅう1グループ'),
('虫组', 'bug', 'むしグループ'),
('飞行组', 'flying', 'ひこうグループ'),
('陆上组', 'ground', 'りくじょうグループ'),
('妖精组', 'fairy', 'フェアリーグループ'),
('植物组', 'plant', 'しょくぶつグループ'),
('人型组', 'humanshape', 'ひとがたグループ'),
('水中3组', 'water3', 'すいちゅう3グループ'),
('矿物组', 'mineral', 'こうぶつグループ'),
('不定形组', 'indeterminate', 'ふていけいグループ'),
('水中2组', 'water2', 'すいちゅう2グループ'),
('百变怪组', 'ditto', 'メタモングループ'),
('龙组', 'dragon', 'ドラゴングループ'),
('未发现组', 'no-eggs', 'みはっけんグループ');

-- 插入经验类型数据
INSERT INTO `growth_rate` (`name`, `name_en`, `name_jp`, `formula`) VALUES
('慢速', 'slow', 'おそい', '5t³/4'),
('中速', 'medium', 'ふつう', 't³'),
('快速', 'fast', 'はやい', '4t³/5'),
('中慢速', 'medium-slow', 'ちゅうおそい', '6/5t³ - 15t² + 100t - 140'),
('慢速后极快', 'slow-then-very-fast', 'おそいのちとてもはやい', '特殊计算'),
('快速后极慢', 'fast-then-very-slow', 'はやいのちとてもおそい', '特殊计算');

COMMIT;