#!/usr/bin/env python3
"""验证SQLite数据完整性"""

import sqlite3
import os
import sys

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.getenv('SQLITE_DB_PATH') or os.path.join(ROOT_DIR, "pokemon-factory-backend", "pokemon-factory.db")

def ensure(condition, message):
    if not condition:
        raise AssertionError(message)

def verify_data():
    print("=" * 60)
    print("📊 SQLite数据完整性验证")
    print("=" * 60)
    print()
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 获取所有表
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [row[0] for row in cursor.fetchall()]
    
    print(f"📁 数据库表数量: {len(tables)}")
    print()
    
    # 验证关键表的数据
    key_tables = {
        'type': '属性',
        'move': '技能',
        'move_target': '技能目标类型',
        'evolution_trigger': '进化触发条件',
        'ability': '特性',
        'item': '物品',
        'pokemon_species': '宝可梦物种',
        'pokemon_form': '宝可梦形态',
        'pokemon_form_type': '属性关联',
        'pokemon_form_ability': '特性关联',
        'pokemon_form_stat': '种族值',
        'pokemon_form_move': '技能学习',
        'move_meta': '技能元数据',
        'move_flags': '技能标记',
        'move_flag_map': '技能标记关联',
        'type_efficacy': '属性相性',
        'pokemon_evolution': '进化详情',
        'evolution_chain': '进化链'
    }
    counts = {}
    
    print("📋 关键表数据统计:")
    print("-" * 60)
    for table_name, display_name in key_tables.items():
        if table_name in tables:
            cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            count = cursor.fetchone()[0]
            counts[table_name] = count
            print(f"  {display_name:20s}: {count:>10,} 条")
        else:
            print(f"  {display_name:20s}: ❌ 表不存在")
    print()

    ensure('type' in counts, '缺少 type 表')
    ensure(counts['type'] >= 18, f"type 表期望至少 18 条，实际 {counts['type']}")
    for table_name in ('move', 'move_target', 'evolution_trigger', 'ability', 'item', 'pokemon_species', 'pokemon_form', 'pokemon_form_type',
                       'pokemon_form_ability', 'pokemon_form_stat', 'pokemon_form_move', 'move_meta',
                       'move_flag_map', 'type_efficacy', 'pokemon_evolution', 'evolution_chain'):
        ensure(counts.get(table_name, 0) > 0, f"关键表 {table_name} 没有数据")
    
    # 验证技能元数据完整性
    print("🔍 技能元数据验证:")
    print("-" * 60)
    total_moves = counts['move']
    meta_count = counts['move_meta']
    print(f"  总技能数: {total_moves:,}")
    print(f"  有元数据的技能: {meta_count:,}")
    print(f"  覆盖率: {meta_count/total_moves*100:.1f}%")
    print()
    
    # 验证技能标记
    print("🔍 技能标记验证:")
    print("-" * 60)
    cursor.execute("SELECT COUNT(*) FROM move_flags")
    flag_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM move_flag_map")
    flag_map_count = cursor.fetchone()[0]
    print(f"  标记类型: {flag_count}")
    print(f"  标记关联: {flag_map_count:,}")
    print()
    
    # 验证宝可梦数据
    print("🔍 宝可梦数据验证:")
    print("-" * 60)
    species_count = counts['pokemon_species']
    form_count = counts['pokemon_form']
    
    cursor.execute("SELECT COUNT(DISTINCT species_id) FROM pokemon_form")
    species_with_forms = cursor.fetchone()[0]
    
    print(f"  物种数: {species_count:,}")
    print(f"  形态数: {form_count:,}")
    print(f"  平均形态数: {form_count/species_count:.1f}")
    ensure(species_with_forms == species_count, f"存在没有形态的物种：species={species_count}, covered={species_with_forms}")
    print()
    
    # 验证属性相性
    print("🔍 属性相性验证:")
    print("-" * 60)
    type_count = counts['type']
    efficacy_count = counts['type_efficacy']
    
    expected_efficacy = type_count * type_count
    print(f"  属性数量: {type_count}")
    print(f"  属性相性记录: {efficacy_count:,}")
    print(f"  理论满量记录数: {expected_efficacy:,}")
    print(f"  当前覆盖率: {efficacy_count/expected_efficacy*100:.1f}%")
    ensure(efficacy_count > 0, 'type_efficacy 没有数据')
    print()
    
    # 验证进化数据
    print("🔍 进化数据验证:")
    print("-" * 60)
    chain_count = counts['evolution_chain']
    evolution_count = counts['pokemon_evolution']
    
    cursor.execute("SELECT COUNT(DISTINCT evolved_species_id) FROM pokemon_evolution")
    evolved_count = cursor.fetchone()[0]
    
    print(f"  进化链数: {chain_count:,}")
    print(f"  进化详情: {evolution_count:,}")
    print(f"  进化的物种数: {evolved_count:,}")
    print()

    print("🔍 外键完整性验证:")
    print("-" * 60)
    cursor.execute("PRAGMA foreign_key_check")
    fk_errors = cursor.fetchall()
    print(f"  外键异常记录: {len(fk_errors)}")
    ensure(len(fk_errors) == 0, f"发现外键异常: {fk_errors[:5]}")
    print()
    
    # 检查数据示例
    print("🔍 数据示例:")
    print("-" * 60)
    
    # 示例：皮卡丘
    cursor.execute("SELECT id, name, name_en FROM pokemon_species WHERE name_en = 'pikachu'")
    pikachu = cursor.fetchone()
    if pikachu:
        print(f"  皮卡丘: ID={pikachu[0]}, 名称={pikachu[1]}, 英文={pikachu[2]}")
    
    # 示例：十万伏特
    cursor.execute("SELECT id, name, name_en, power, type_id FROM move WHERE name_en = 'thunderbolt'")
    thunderbolt = cursor.fetchone()
    if thunderbolt:
        print(f"  十万伏特: ID={thunderbolt[0]}, 名称={thunderbolt[1]}, 威力={thunderbolt[3]}, 属性ID={thunderbolt[4]}")
    
    # 示例：静电特性
    cursor.execute("SELECT id, name, name_en FROM ability WHERE name_en = 'static'")
    static = cursor.fetchone()
    if static:
        print(f"  静电: ID={static[0]}, 名称={static[1]}, 英文={static[2]}")
    print()
    
    conn.close()
    print("=" * 60)
    print("✅ 数据验证完成")
    print("=" * 60)

if __name__ == '__main__':
    try:
        verify_data()
    except Exception as exc:
        print(f"❌ 数据验证失败: {exc}", file=sys.stderr)
        raise
