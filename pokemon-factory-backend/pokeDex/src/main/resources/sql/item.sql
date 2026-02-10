-- 物品表
CREATE TABLE IF NOT EXISTS `item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '物品名称',
  `name_en` varchar(100) DEFAULT NULL COMMENT '物品英文名称',
  `name_jp` varchar(100) DEFAULT NULL COMMENT '物品日文名称',
  `category` varchar(50) DEFAULT NULL COMMENT '物品分类',
  `price` int DEFAULT NULL COMMENT '物品价格',
  `effect` varchar(500) DEFAULT NULL COMMENT '物品效果',
  `description` text COMMENT '物品描述',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- 技能表
CREATE TABLE IF NOT EXISTS `move` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '技能ID',
  `index_number` varchar(20) DEFAULT NULL COMMENT '技能编号',
  `generation` varchar(10) DEFAULT NULL COMMENT '所属世代',
  `name` varchar(100) NOT NULL COMMENT '技能名称(中文)',
  `name_en` varchar(100) NOT NULL COMMENT '技能名称(英文)',
  `name_jp` varchar(100) DEFAULT NULL COMMENT '技能名称(日文)',
  `type_id` bigint DEFAULT NULL COMMENT '属性ID',
  `category` varchar(20) DEFAULT NULL COMMENT '技能分类',
  `power` int DEFAULT NULL COMMENT '威力',
  `accuracy` int DEFAULT NULL COMMENT '命中率',
  `pp` int DEFAULT NULL COMMENT 'PP',
  `description` text COMMENT '技能描述',
  `effect` text COMMENT '技能效果',
  `priority` int DEFAULT NULL COMMENT '优先度',
  `target` varchar(50) DEFAULT NULL COMMENT '作用目标',
  `contest_type` varchar(50) DEFAULT NULL COMMENT '华丽大赛属性',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能表';