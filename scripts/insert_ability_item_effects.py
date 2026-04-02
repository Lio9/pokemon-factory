#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
插入特性和道具效果数据到数据库
"""
import sqlite3
import os

def main():
    # 数据库路径
    db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'pokemon-factory.db')
    
    # 连接数据库
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 创建表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ability_effect (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ability_id INTEGER NOT NULL,
            effect_type TEXT NOT NULL,
            effect_value TEXT,
            target TEXT NOT NULL,
            condition TEXT,
            description TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (ability_id) REFERENCES ability(id)
        )
    ''')
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS item_effect (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            item_id INTEGER NOT NULL,
            effect_type TEXT NOT NULL,
            effect_value TEXT,
            target TEXT NOT NULL,
            condition TEXT,
            description TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (item_id) REFERENCES items(id)
        )
    ''')
    
    # 清空旧数据
    cursor.execute('DELETE FROM ability_effect')
    cursor.execute('DELETE FROM item_effect')
    
    # 插入特性效果数据
    ability_effects = [
        (91, 'stab_multiplier', '2.0', 'attacker', 'is_stab', '本系技能威力提升至2.0倍'),
        (66, 'damage_multiplier', '1.5', 'attacker', 'type_id=10 AND hp_percent<=33', 'HP低于1/3时火系技能威力提升50%'),
        (67, 'damage_multiplier', '1.5', 'attacker', 'type_id=11 AND hp_percent<=33', 'HP低于1/3时水系技能威力提升50%'),
        (18, 'damage_multiplier', '1.5', 'attacker', 'type_id=7 AND hp_percent<=33', 'HP低于1/3时虫系技能威力提升50%'),
        (65, 'damage_multiplier', '1.5', 'attacker', 'type_id=12 AND hp_percent<=33', 'HP低于1/3时草系技能威力提升50%'),
        (152, 'damage_multiplier', '1.3', 'attacker', 'is_contact', '接触技能威力提升30%'),
        (232, 'damage_multiplier', '1.2', 'attacker', 'is_punch', '拳类技能威力提升20%'),
        (86, 'damage_multiplier', '1.5', 'attacker', 'power<=60', '威力60以下技能提升50%'),
        (62, 'damage_multiplier', '0.5', 'defender', 'type_id IN (10, 14)', '火系和冰系伤害减半'),
        (153, 'damage_multiplier', '0.5', 'defender', 'hp_percent=100', '满HP时受到伤害减半'),
        (216, 'damage_multiplier', '0.5', 'defender', 'damage_class=physical', '物理伤害减半'),
        (22, 'stat_boost', '-1', 'defender', 'on_switch_in', '出场时降低对方攻击1级'),
        (29, 'status_immunity', 'flinch', 'always', 'always', '免疫畏缩效果'),
        (59, 'weather_effect', 'null', 'self', 'always', '免疫天气效果'),
        (31, 'terrain_set', 'electric', 'self', 'on_switch_in', '出场时设置电气场地'),
        (229, 'terrain_set', 'grassy', 'self', 'on_switch_in', '出场时设置草地场地'),
        (268, 'terrain_set', 'psychic', 'self', 'on_switch_in', '出场时设置超能力场地'),
        (243, 'terrain_set', 'misty', 'self', 'on_switch_in', '出场时设置薄雾场地'),
    ]
    
    cursor.executemany('''
        INSERT INTO ability_effect (ability_id, effect_type, effect_value, target, condition, description)
        VALUES (?, ?, ?, ?, ?, ?)
    ''', ability_effects)
    
    # 插入道具效果数据
    item_effects = [
        (130, 'damage_multiplier', '1.3', 'attacker', 'always', '所有技能威力提升30%'),
        (327, 'damage_multiplier', '1.5', 'attacker', 'damage_class=special', '特攻技能威力提升50%'),
        (299, 'damage_multiplier', '1.5', 'attacker', 'damage_class=physical', '物理技能威力提升50%'),
        (83, 'damage_multiplier', '1.2', 'attacker', 'type_id=10', '火系技能威力提升20%'),
        (171, 'damage_multiplier', '1.2', 'attacker', 'type_id=13', '电系技能威力提升20%'),
        (91, 'damage_multiplier', '1.2', 'attacker', 'type_id=9', '钢系技能威力提升20%'),
        (85, 'damage_multiplier', '1.2', 'attacker', 'type_id=12', '草系技能威力提升20%'),
        (82, 'damage_multiplier', '1.2', 'attacker', 'type_id=11', '水系技能威力提升20%'),
        (239, 'damage_multiplier', '1.2', 'attacker', 'type_id=17', '恶系技能威力提升20%'),
        (267, 'damage_multiplier', '1.2', 'attacker', 'type_id=14', '超能力系技能威力提升20%'),
        (89, 'damage_multiplier', '1.2', 'attacker', 'type_id=5', '地面系技能威力提升20%'),
        (88, 'damage_multiplier', '1.2', 'attacker', 'type_id=6', '岩石系技能威力提升20%'),
        (87, 'damage_multiplier', '1.2', 'attacker', 'type_id=15', '冰系技能威力提升20%'),
        (84, 'damage_multiplier', '1.2', 'attacker', 'type_id=4', '毒系技能威力提升20%'),
        (90, 'damage_multiplier', '1.2', 'attacker', 'type_id=3', '飞行系技能威力提升20%'),
        (279, 'damage_multiplier', '1.2', 'attacker', 'type_id=8', '岩石系技能威力提升20%'),
        (305, 'recoil', '1/6', 'attacker', 'is_contact', '接触技能受到反伤1/6'),
    ]
    
    cursor.executemany('''
        INSERT INTO item_effect (item_id, effect_type, effect_value, target, condition, description)
        VALUES (?, ?, ?, ?, ?, ?)
    ''', item_effects)
    
    # 提交事务
    conn.commit()
    
    # 输出统计信息
    print(f"已插入 {len(ability_effects)} 条特性效果数据")
    print(f"已插入 {len(item_effects)} 条道具效果数据")
    
    # 关闭连接
    conn.close()
    
    print("数据导入完成！")

if __name__ == '__main__':
    main()