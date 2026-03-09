#!/usr/bin/env python3
"""导入技能标记数据"""
import mysql.connector
import csv
import os

DB_CONFIG = {
    "host": "10.144.55.168",
    "port": 3306,
    "user": "root",
    "password": "753951",
    "database": "pokemon_factory",
    "charset": "utf8mb4",
}

CSV_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "csv")

def create_tables(cursor):
    """创建技能标记表"""
    print("创建 move_flags 表...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `move_flags` (
            `id` INT PRIMARY KEY COMMENT '标记ID',
            `identifier` VARCHAR(50) NOT NULL UNIQUE COMMENT '标记标识符',
            `name` VARCHAR(50) COMMENT '标记名称(中文)',
            `description` VARCHAR(200) COMMENT '描述',
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能标记表'
    """)
    
    print("创建 move_flag_map 表...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `move_flag_map` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `move_id` INT NOT NULL COMMENT '技能ID',
            `flag_id` INT NOT NULL COMMENT '标记ID',
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (`move_id`) REFERENCES `move`(`id`) ON DELETE CASCADE,
            FOREIGN KEY (`flag_id`) REFERENCES `move_flags`(`id`),
            UNIQUE KEY `uk_move_flag` (`move_id`, `flag_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能-标记关联表'
    """)

def import_move_flags(cursor, conn):
    """导入 move_flags 数据"""
    csv_path = os.path.join(CSV_DIR, "move_flags.csv")
    print(f"导入 {csv_path}...")
    
    # 标记的中文名称映射
    flag_names = {
        'contact': ('接触', '与目标接触的技能'),
        'charge': ('蓄力', '需要蓄力回合'),
        'recharge': ('休息', '使用后需要休息'),
        'protect': ('保护', '可被保护技能阻挡'),
        'reflectable': ('魔反', '可被魔法反射'),
        'snatch': ('抢夺', '可被抢夺'),
        'mirror': ('镜反', '可被镜面反射'),
        'punch': ('拳击', '拳击类技能'),
        'sound': ('声音', '声音类技能'),
        'gravity': ('重力', '受重力影响'),
        'defrost': ('解冻', '可解冻使用者'),
        'distance': ('远距', '远距离技能'),
        'heal': ('治疗', '治疗技能'),
        'authentic': ('穿透', '穿透替身'),
        'powder': ('粉末', '粉末类技能'),
        'bite': ('咬', '咬类技能'),
        'pulse': ('脉冲', '脉冲类技能'),
        'ballistics': ('子弹', '子弹类技能'),
        'mental': ('精神', '精神类技能'),
        'non-sky-battle': ('非空战', '不可用于空中战'),
        'dance': ('舞蹈', '舞蹈类技能'),
    }
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        count = 0
        for row in reader:
            flag_id = int(row['id'])
            identifier = row['identifier']
            name, desc = flag_names.get(identifier, (identifier, ''))
            
            try:
                cursor.execute("""
                    INSERT INTO move_flags (id, identifier, name, description)
                    VALUES (%s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE identifier = VALUES(identifier), name = VALUES(name)
                """, (flag_id, identifier, name, desc))
                count += 1
            except Exception as e:
                print(f"  插入 move_flags ID={flag_id} 失败: {e}")
        
        conn.commit()
        print(f"  导入 {count} 条 move_flags 记录")

def import_move_flag_map(cursor, conn):
    """导入 move_flag_map 数据"""
    csv_path = os.path.join(CSV_DIR, "move_flag_map.csv")
    print(f"导入 {csv_path}...")
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        count = 0
        skipped = 0
        for row in reader:
            move_id = int(row['move_id'])
            flag_id = int(row['move_flag_id'])
            
            try:
                cursor.execute("""
                    INSERT IGNORE INTO move_flag_map (move_id, flag_id)
                    VALUES (%s, %s)
                """, (move_id, flag_id))
                count += 1
            except Exception as e:
                skipped += 1
        
        conn.commit()
        print(f"  导入 {count} 条 move_flag_map 记录")
        if skipped > 0:
            print(f"  跳过 {skipped} 条记录（可能外键约束失败）")

def verify_data(cursor):
    """验证数据"""
    print("\n=== 验证数据 ===")
    
    cursor.execute("SELECT COUNT(*) FROM move_flags")
    flag_count = cursor.fetchone()[0]
    print(f"move_flags 表记录数: {flag_count}")
    
    cursor.execute("SELECT COUNT(*) FROM move_flag_map")
    map_count = cursor.fetchone()[0]
    print(f"move_flag_map 表记录数: {map_count}")
    
    print("\n技能标签示例:")
    cursor.execute("""
        SELECT mf.identifier, mf.name, COUNT(*) as cnt 
        FROM move_flags mf 
        JOIN move_flag_map mfm ON mfm.flag_id = mf.id 
        GROUP BY mf.id 
        ORDER BY cnt DESC 
        LIMIT 10
    """)
    for row in cursor.fetchall():
        print(f"  {row[1]} ({row[0]}): {row[2]} 个技能")
    
    print("\n技能标记示例（皮卡丘的电击）:")
    cursor.execute("""
        SELECT mf.name, mf.identifier 
        FROM move_flags mf 
        JOIN move_flag_map mfm ON mfm.flag_id = mf.id 
        WHERE mfm.move_id = 84
    """)
    for row in cursor.fetchall():
        print(f"  {row[0]} ({row[1]})")

def main():
    print("=== 导入技能标记数据 ===\n")
    
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 创建表
        create_tables(cursor)
        conn.commit()
        
        # 导入数据
        import_move_flags(cursor, conn)
        import_move_flag_map(cursor, conn)
        
        # 验证
        verify_data(cursor)
        
        print("\n=== 导入完成 ===")
    except Exception as e:
        print(f"错误: {e}")
        conn.rollback()
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    main()
