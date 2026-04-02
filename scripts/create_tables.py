#!/usr/bin/env python3
"""执行增量建表"""
import mysql.connector
import os

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "pokemon_factory"),
    "charset": "utf8mb4",
}

SQL_STATEMENTS = [
    # 异常状态表
    """CREATE TABLE IF NOT EXISTS `move_meta_ailment` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(30) NOT NULL,
        `name_en` VARCHAR(30) NOT NULL UNIQUE,
        `description` VARCHAR(200),
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 技能元数据类别表
    """CREATE TABLE IF NOT EXISTS `move_meta_category` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `name_en` VARCHAR(50) NOT NULL UNIQUE,
        `description` VARCHAR(200),
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 技能目标类型表
    """CREATE TABLE IF NOT EXISTS `move_target` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `name_en` VARCHAR(50) NOT NULL UNIQUE,
        `description` VARCHAR(200),
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 技能能力变化表
    """CREATE TABLE IF NOT EXISTS `move_meta_stat_change` (
        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
        `move_id` INT NOT NULL,
        `stat_id` INT NOT NULL,
        `change` INT NOT NULL,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品口袋表
    """CREATE TABLE IF NOT EXISTS `item_pocket` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(30) NOT NULL,
        `name_en` VARCHAR(30) NOT NULL UNIQUE,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品分类表
    """CREATE TABLE IF NOT EXISTS `item_category` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `name_en` VARCHAR(50) NOT NULL UNIQUE,
        `pocket_id` INT,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品投掷效果表
    """CREATE TABLE IF NOT EXISTS `item_fling_effect` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `name_en` VARCHAR(50) NOT NULL UNIQUE,
        `description` TEXT,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品属性表
    """CREATE TABLE IF NOT EXISTS `item_flag` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(50) NOT NULL,
        `name_en` VARCHAR(50) NOT NULL UNIQUE,
        `description` TEXT,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品表
    """CREATE TABLE IF NOT EXISTS `item` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(100) NOT NULL,
        `name_en` VARCHAR(100) NOT NULL UNIQUE,
        `name_jp` VARCHAR(100),
        `category_id` INT,
        `cost` INT DEFAULT 0,
        `fling_power` INT,
        `fling_effect_id` INT,
        `effect_short` VARCHAR(500),
        `effect_detail` TEXT,
        `description` TEXT,
        `generation_id` INT,
        `sprite_url` VARCHAR(500),
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 物品属性关联表
    """CREATE TABLE IF NOT EXISTS `item_flag_map` (
        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
        `item_id` INT NOT NULL,
        `flag_id` INT NOT NULL,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY `uk_item_flag` (`item_id`, `flag_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 版本组表
    """CREATE TABLE IF NOT EXISTS `version_group` (
        `id` INT PRIMARY KEY,
        `name` VARCHAR(30) NOT NULL,
        `name_en` VARCHAR(30) NOT NULL UNIQUE,
        `generation_id` INT,
        `order` INT,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    
    # 宝可梦可学技能表
    """CREATE TABLE IF NOT EXISTS `pokemon_form_move` (
        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
        `form_id` INT NOT NULL,
        `move_id` INT NOT NULL,
        `learn_method_id` INT NOT NULL,
        `level` INT,
        `version_group_id` INT,
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX `idx_form_move` (`form_id`, `move_id`),
        INDEX `idx_form_learn` (`form_id`, `learn_method_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
]

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    for sql in SQL_STATEMENTS:
        try:
            cursor.execute(sql)
            print(f"✅ {sql.split('`')[1]}")
        except Exception as e:
            print(f"⚠️  {sql.split('`')[1]}: {e}")
    
    conn.commit()
    cursor.close()
    conn.close()
    print("\n✅ 建表完成")

if __name__ == "__main__":
    main()
