#!/usr/bin/env python3
"""
从 PokeAPI CSV 文件导入数据到 SQLite 数据库
专为 Pokemon Factory 项目优化
"""

import csv
import sqlite3
import os
import sys
from datetime import datetime

# 数据库配置
DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "pokemon-factory.db")

# CSV 文件目录
CSV_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "csv")

# 语言ID映射
LANGUAGE_MAP = {
    1: 'ja',     # 日文
    4: 'zh-hant', # 繁体中文
    5: 'fr',     # 法文
    6: 'de',     # 德文
    7: 'es',     # 西班牙文
    8: 'it',     # 意大利文
    9: 'en',     # 英文
    11: 'ja',    # 日文
    12: 'zh-hans', # 简体中文
}

# 属性颜色映射
TYPE_COLORS = {
    'normal': '#A8A878', 'fighting': '#C03028', 'flying': '#A890F0',
    'poison': '#A040A0', 'ground': '#E0C068', 'rock': '#B8A038',
    'bug': '#A8B820', 'ghost': '#705898', 'steel': '#B8B8D0',
    'fire': '#F08030', 'water': '#6890F0', 'grass': '#78C850',
    'electric': '#F8D030', 'psychic': '#F85888', 'ice': '#98D8D8',
    'dragon': '#7038F8', 'dark': '#705848', 'fairy': '#EE99AC',
    'stellar': '#68A890', 'unknown': '#68A090', 'shadow': '#604E82',
}

# 蛋群名称映射
EGG_GROUP_NAMES = {
    1: ('怪兽组', 'monster', 'かいぶつ'),
    2: ('水中1组', 'water1', 'すいちゅう1'),
    3: ('虫组', 'bug', 'むし'),
    4: ('飞行组', 'flying', 'ひこう'),
    5: ('陆上组', 'ground', 'りくじょう'),
    6: ('妖精组', 'fairy', 'フェアリー'),
    7: ('植物组', 'plant', 'しょくぶつ'),
    8: ('人型组', 'human-like', 'ひとがた'),
    9: ('水中3组', 'water3', 'すいちゅう3'),
    10: ('矿物组', 'mineral', 'こうぶつ'),
    11: ('不定形组', 'amorphous', 'ふていけい'),
    12: ('水中2组', 'water2', 'すいちゅう2'),
    13: ('百变怪组', 'ditto', 'メタモン'),
    14: ('龙组', 'dragon', 'ドラゴン'),
    15: ('未发现组', 'no-eggs', 'みはっけん'),
}

# 颜色映射
COLOR_NAMES = {
    1: '黑色', 2: '蓝色', 3: '褐色', 4: '灰色',
    5: '绿色', 6: '粉色', 7: '紫色', 8: '红色',
    9: '白色', 10: '黄色'
}

# 形状映射
SHAPE_NAMES = {
    1: '球状', 2: '蛇状', 3: '鱼状', 4: '四足兽状',
    5: '虫状', 6: '有翼的', 7: '多条腿', 8: '人形',
    9: '有触手的', 10: '多身一体', 11: '四肢和两腿', 12: '双翅', 13: '其他'
}

def get_db_connection():
    """获取数据库连接"""
    return sqlite3.connect(DB_PATH)

def load_csv(filename):
    """加载CSV文件"""
    filepath = os.path.join(CSV_DIR, filename)
    if not os.path.exists(filepath):
        print(f"⚠️  文件不存在: {filepath}")
        return []
    
    with open(filepath, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        return list(reader)

def get_localized_name(names_data, preferred_langs=['zh-hans', 'zh-hant', 'en', 'ja']):
    """获取本地化名称"""
    if not names_data:
        return None
    
    name_dict = {}
    for item in names_data:
        lang_id = int(item.get('local_language_id', 0))
        if lang_id in LANGUAGE_MAP:
            lang = LANGUAGE_MAP[lang_id]
            name_dict[lang] = item.get('name')
    
    for lang in preferred_langs:
        if lang in name_dict:
            return name_dict[lang]
    
    return names_data[0].get('name')

def get_localized_text(prose_data, preferred_langs=['zh-hans', 'zh-hant', 'en']):
    """获取本地化文本"""
    if not prose_data:
        return None
    
    text_dict = {}
    for item in prose_data:
        lang_id = int(item.get('local_language_id', 0))
        if lang_id in LANGUAGE_MAP:
            lang = LANGUAGE_MAP[lang_id]
            text_dict[lang] = item.get('flavor_text', item.get('short_effect', item.get('effect', '')))
    
    for lang in preferred_langs:
        if lang in text_dict:
            return text_dict[lang]
    
    return prose_data[0].get('flavor_text', prose_data[0].get('short_effect', prose_data[0].get('effect', '')))

# ==========================================
# 基础数据导入
# ==========================================

def import_genders():
    """导入性别数据"""
    print("📥 导入性别...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('genders.csv')
    for row in data:
        cursor.execute("""
            INSERT INTO gender (id, name)
            VALUES (?, ?)
        """, (row['id'], row['identifier']))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条性别数据")

def import_growth_rates():
    """导入成长类型数据"""
    print("📥 导入成长类型...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 基础数据已在初始化脚本中
    # 这里补充描述和公式
    data = load_csv('growth_rates.csv')
    prose_data = load_csv('growth_rate_prose.csv')
    
    prose_dict = {}
    for p in prose_data:
        prose_dict[int(p['growth_rate_id'])] = p
    
    for row in data:
        growth_rate_id = int(row['id'])
        prose = prose_dict.get(growth_rate_id, {})
        
        cursor.execute("""
            UPDATE growth_rate
            SET formula = ?, description = ?
            WHERE id = ?
        """, (row['formula'], prose.get('description'), growth_rate_id))
    
    conn.commit()
    conn.close()
    print(f"✅ 更新 {len(data)} 条成长类型数据")

def import_egg_groups():
    """导入蛋群数据"""
    print("📥 导入蛋群...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 基础数据已在初始化脚本中
    # 这里补充多语言名称
    data = load_csv('egg_groups.csv')
    prose_data = load_csv('egg_group_prose.csv')
    
    prose_dict = {}
    for p in prose_data:
        prose_dict[int(p['egg_group_id'])] = p
    
    for row in data:
        egg_group_id = int(row['id'])
        prose = prose_dict.get(egg_group_id, {})
        
        cursor.execute("""
            UPDATE egg_group
            SET name = ?
            WHERE id = ?
        """, (prose.get('name'), egg_group_id))
    
    conn.commit()
    conn.close()
    print(f"✅ 更新 {len(data)} 条蛋群数据")

def import_natures():
    """导入性格数据"""
    print("📥 导入性格...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('natures.csv')
    names_data = load_csv('nature_names.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['nature_id']), []).append(n)
    
    for row in data:
        nature_id = int(row['id'])
        names = names_dict.get(nature_id, [])
        
        # 解析喜好/讨厌的口味
        likes_flavor = None
        hates_flavor = None
        if row['likes_flavor_id']:
            flavors = {'1': '辣', '2': '酸', '3': '咸', '4': '苦', '5': '甜'}
            likes_flavor = flavors.get(row['likes_flavor_id'])
        if row['hates_flavor_id']:
            flavors = {'1': '辣', '2': '酸', '3': '咸', '4': '苦', '5': '甜'}
            hates_flavor = flavors.get(row['hates_flavor_id'])
        
        cursor.execute("""
            INSERT OR REPLACE INTO nature (id, name, name_en, name_jp, increased_stat, decreased_stat, likes_flavor, hates_flavor)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            nature_id,
            get_localized_name(names),
            row['identifier'],
            get_localized_name(names, ['ja', 'en']),
            row['increased_stat_id'],
            row['decreased_stat_id'],
            likes_flavor,
            hates_flavor
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条性格数据")

def import_move_learn_methods():
    """导入技能学习方式数据"""
    print("📥 导入技能学习方式...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_move_methods.csv')
    prose_data = load_csv('pokemon_move_method_prose.csv')
    
    prose_dict = {}
    for p in prose_data:
        prose_dict.setdefault(int(p['pokemon_move_method_id']), []).append(p)
    
    for row in data:
        method_id = int(row['id'])
        proses = prose_dict.get(method_id, [])
        
        name = get_localized_name(proses)
        if not name:
            name = row['identifier'].replace('-', ' ')
        
        cursor.execute("""
            INSERT OR REPLACE INTO move_learn_method (id, name, name_en, description)
            VALUES (?, ?, ?, ?)
        """, (
            method_id,
            name,
            row['identifier'],
            get_localized_text(proses)
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条技能学习方式数据")

# ==========================================
# 核心数据导入
# ==========================================

def import_abilities():
    """导入特性数据"""
    print("📥 导入特性...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('abilities.csv')
    names_data = load_csv('ability_names.csv')
    prose_data = load_csv('ability_prose.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['ability_id']), []).append(n)
    
    prose_dict = {}
    for p in prose_data:
        prose_dict.setdefault(int(p['ability_id']), []).append(p)
    
    for row in data:
        ability_id = int(row['id'])
        names = names_dict.get(ability_id, [])
        proses = prose_dict.get(ability_id, [])
        
        cursor.execute("""
            INSERT OR REPLACE INTO ability (id, name, name_en, name_jp, description, generation_id, is_main_series)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (
            ability_id,
            get_localized_name(names),
            row['identifier'],
            get_localized_name(names, ['ja', 'en']),
            get_localized_text(proses),
            int(row['generation_id']) if row['generation_id'] else None,
            1 if row['is_main_series'] == '1' else 0
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条特性数据")

def import_moves():
    """导入技能数据"""
    print("📥 导入技能...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('moves.csv')
    names_data = load_csv('move_names.csv')
    prose_data = load_csv('move_flavor_text.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['move_id']), []).append(n)
    
    # 获取每个技能的描述（按世代获取最新的）
    prose_dict = {}
    for p in prose_data:
        move_id = int(p['move_id'])
        version_group_id = int(p['version_group_id']) if p['version_group_id'] else 0
        if move_id not in prose_dict or version_group_id > prose_dict[move_id][0]:
            prose_dict[move_id] = (version_group_id, p)
    
    for row in data:
        move_id = int(row['id'])
        names = names_dict.get(move_id, [])
        _, latest_prose = prose_dict.get(move_id, (0, {}))
        
        cursor.execute("""
            INSERT OR REPLACE INTO move (
                id, name, name_en, name_jp, type_id, damage_class_id, target_id,
                power, pp, accuracy, priority, effect_chance, generation_id, description
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            move_id,
            get_localized_name(names),
            row['identifier'],
            get_localized_name(names, ['ja', 'en']),
            int(row['type_id']) if row['type_id'] else None,
            int(row['damage_class_id']) if row['damage_class_id'] else None,
            int(row['target_id']) if row['target_id'] else None,
            int(row['power']) if row['power'] else None,
            int(row['pp']) if row['pp'] else None,
            int(row['accuracy']) if row['accuracy'] else None,
            int(row['priority']) if row['priority'] else 0,
            int(row['effect_chance']) if row['effect_chance'] else None,
            int(row['generation_id']) if row['generation_id'] else None,
            latest_prose.get('flavor_text') if latest_prose else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条技能数据")

def import_move_meta():
    """导入技能元数据（连击、吸取等）"""
    print("📥 导入技能元数据...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('move_meta.csv')
    
    for row in data:
        move_id = int(row['move_id'])
        
        cursor.execute("""
            INSERT OR REPLACE INTO move_meta (
                move_id, min_hits, max_hits, min_turns, max_turns,
                drain, healing, crit_rate, ailment_chance, flinch_chance, stat_chance
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            move_id,
            int(row['min_hits']) if row['min_hits'] else None,
            int(row['max_hits']) if row['max_hits'] else None,
            int(row['min_turns']) if row['min_turns'] else None,
            int(row['max_turns']) if row['max_turns'] else None,
            int(row['drain']) if row['drain'] else None,
            int(row['healing']) if row['healing'] else None,
            int(row['crit_rate']) if row['crit_rate'] else None,
            int(row['ailment_chance']) if row['ailment_chance'] else None,
            int(row['flinch_chance']) if row['flinch_chance'] else None,
            int(row['stat_chance']) if row['stat_chance'] else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条技能元数据")

def import_move_flags():
    """导入技能标记"""
    print("📥 导入技能标记...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 导入标记定义
    flags_data = load_csv('move_flags.csv')
    for row in flags_data:
        cursor.execute("""
            INSERT OR REPLACE INTO move_flags (id, identifier, name)
            VALUES (?, ?, ?)
        """, (int(row['id']), row['identifier'], row['identifier']))
    
    # 导入标记关联
    flag_map_data = load_csv('move_flag_map.csv')
    for row in flag_map_data:
        cursor.execute("""
            INSERT OR REPLACE INTO move_flag_map (move_id, flag_id)
            VALUES (?, ?)
        """, (int(row['move_id']), int(row['move_flag_id'])))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(flags_data)} 个标记, {len(flag_map_data)} 个关联")

def import_move_stat_changes():
    """导入技能能力变化"""
    print("📥 导入技能能力变化...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('move_meta_stat_changes.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO move_meta_stat_change (move_id, stat_id, "change")
            VALUES (?, ?, ?)
        """, (
            int(row['move_id']),
            int(row['stat_id']),
            int(row['change'])
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条技能能力变化")

def import_items():
    """导入物品数据"""
    print("📥 导入物品...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('items.csv')
    names_data = load_csv('item_names.csv')
    prose_data = load_csv('item_flavor_text.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['item_id']), []).append(n)
    
    prose_dict = {}
    for p in prose_data:
        item_id = int(p['item_id'])
        version_group_id = int(p['version_group_id']) if p['version_group_id'] else 0
        if item_id not in prose_dict or version_group_id > prose_dict[item_id][0]:
            prose_dict[item_id] = (version_group_id, p)
    
    for row in data:
        item_id = int(row['id'])
        names = names_dict.get(item_id, [])
        _, latest_prose = prose_dict.get(item_id, (0, {}))
        
        cursor.execute("""
            INSERT OR REPLACE INTO item (
                id, name, name_en, name_jp, category_id, cost,
                fling_power, fling_effect_id, description
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            item_id,
            get_localized_name(names) or row['identifier'],
            row['identifier'],
            get_localized_name(names, ['ja', 'en']) or row['identifier'],
            int(row['category_id']) if row['category_id'] else None,
            int(row['cost']) if row['cost'] else 0,
            int(row['fling_power']) if row['fling_power'] else None,
            int(row['fling_effect_id']) if row['fling_effect_id'] else None,
            latest_prose.get('flavor_text') if latest_prose else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条物品数据")

def import_types():
    """导入属性数据"""
    print("📥 导入属性...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 基础数据已在初始化脚本中
    # 这里补充多语言名称
    names_data = load_csv('type_names.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['type_id']), []).append(n)
    
    for type_id in range(1, 19):
        names = names_dict.get(type_id, [])
        
        cursor.execute("""
            UPDATE type
            SET name = ?, name_jp = ?
            WHERE id = ?
        """, (
            get_localized_name(names),
            get_localized_name(names, ['ja', 'en']),
            type_id
        ))
    
    conn.commit()
    conn.close()
    print("✅ 更新属性数据")

def import_type_efficacy():
    """导入属性相性数据"""
    print("📥 导入属性相性...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('type_efficacy.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO type_efficacy (
                attacking_type_id, defending_type_id, damage_factor
            )
            VALUES (?, ?, ?)
        """, (
            int(row['damage_type_id']),
            int(row['target_type_id']),
            int(row['damage_factor'])
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条属性相性数据")

# ==========================================
# 宝可梦数据导入
# ==========================================

def import_pokemon_species():
    """导入宝可梦物种数据"""
    print("📥 导入宝可梦物种...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_species.csv')
    names_data = load_csv('pokemon_species_names.csv')
    prose_data = load_csv('pokemon_species_flavor_text.csv')
    
    names_dict = {}
    for n in names_data:
        names_dict.setdefault(int(n['pokemon_species_id']), []).append(n)
    
    # 获取最新描述
    prose_dict = {}
    for p in prose_data:
        species_id = int(p['species_id'])
        version_id = int(p['version_id']) if p['version_id'] else 0
        if species_id not in prose_dict or version_id > prose_dict[species_id][0]:
            prose_dict[species_id] = (version_id, p)
    
    for row in data:
        species_id = int(row['id'])
        names = names_dict.get(species_id, [])
        _, latest_prose = prose_dict.get(species_id, (0, {}))
        
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_species (
                id, name, name_en, name_jp, genus, generation_id,
                evolution_chain_id, evolves_from_species_id, gender_rate,
                capture_rate, base_happiness, hatch_counter,
                is_baby, is_legendary, is_mythical, description
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            species_id,
            get_localized_name(names),
            row['identifier'],
            get_localized_name(names, ['ja', 'en']),
            row.get('genus'),
            int(row['generation_id']) if row['generation_id'] else None,
            int(row['evolution_chain_id']) if row['evolution_chain_id'] else None,
            int(row['evolves_from_species_id']) if row['evolves_from_species_id'] else None,
            int(row['gender_rate']) if row['gender_rate'] else -1,
            int(row['capture_rate']) if row['capture_rate'] else 0,
            int(row['base_happiness']) if row['base_happiness'] else 70,
            int(row['hatch_counter']) if row['hatch_counter'] else None,
            1 if row['is_baby'] == '1' else 0,
            1 if row['is_legendary'] == '1' else 0,
            1 if row['is_mythical'] == '1' else 0,
            latest_prose.get('flavor_text') if latest_prose else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条宝可梦物种数据")

def import_pokemon_forms():
    """导入宝可梦形态数据"""
    print("📥 导入宝可梦形态...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_form (
                id, species_id, form_name, is_default, is_battle_only,
                height, weight, base_experience, "order"
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            int(row['id']),
            int(row['species_id']),
            row.get('identifier'),
            1 if row['is_default'] == '1' else 0,
            0,
            float(row['height']) / 10 if row['height'] else None,  # 转换为米
            float(row['weight']) / 10 if row['weight'] else None,  # 转换为公斤
            int(row['base_experience']) if row['base_experience'] else None,
            int(row['order']) if row['order'] else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条宝可梦形态数据")

def import_pokemon_types():
    """导入宝可梦属性关联"""
    print("📥 导入宝可梦属性关联...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_types.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_form_type (form_id, type_id, slot)
            VALUES (?, ?, ?)
        """, (
            int(row['pokemon_id']),
            int(row['type_id']),
            int(row['slot'])
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条属性关联")

def import_pokemon_abilities():
    """导入宝可梦特性关联"""
    print("📥 导入宝可梦特性关联...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_abilities.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_form_ability (form_id, ability_id, is_hidden, slot)
            VALUES (?, ?, ?, ?)
        """, (
            int(row['pokemon_id']),
            int(row['ability_id']),
            1 if row['is_hidden'] == '1' else 0,
            int(row['slot'])
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条特性关联")

def import_pokemon_stats():
    """导入宝可梦种族值"""
    print("📥 导入宝可梦种族值...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_stats.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_form_stat (form_id, stat_id, base_stat, effort)
            VALUES (?, ?, ?, ?)
        """, (
            int(row['pokemon_id']),
            int(row['stat_id']),
            int(row['base_stat']),
            int(row['effort']) if row['effort'] else 0
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条种族值")

def import_pokemon_egg_groups():
    """导入宝可梦蛋群关联"""
    print("📥 导入宝可梦蛋群关联...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_egg_groups.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_species_egg_group (species_id, egg_group_id)
            VALUES (?, ?)
        """, (
            int(row['species_id']),
            int(row['egg_group_id'])
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条蛋群关联")

def import_pokemon_moves():
    """导入宝可梦技能学习"""
    print("📥 导入宝可梦技能学习...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_moves.csv')
    
    imported = 0
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_form_move (form_id, move_id, learn_method_id, level, version_group_id)
            VALUES (?, ?, ?, ?, ?)
        """, (
            int(row['pokemon_id']),
            int(row['move_id']),
            int(row['pokemon_move_method_id']),
            int(row['level']) if row['level'] else None,
            int(row['version_group_id'])
        ))
        imported += 1
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {imported} 条技能学习")

def import_evolution_chains():
    """导入进化链数据"""
    print("📥 导入进化链...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('evolution_chains.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO evolution_chain (id, baby_trigger_item_id)
            VALUES (?, ?)
        """, (
            int(row['id']),
            int(row['baby_trigger_item_id']) if row['baby_trigger_item_id'] else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条进化链")

def import_pokemon_evolution():
    """导入进化详情数据"""
    print("📥 导入进化详情...")
    conn = get_db_connection()
    cursor = conn.cursor()
    
    data = load_csv('pokemon_evolution.csv')
    
    for row in data:
        cursor.execute("""
            INSERT OR REPLACE INTO pokemon_evolution (
                evolved_species_id, evolves_from_species_id, evolution_trigger_id,
                min_level, min_happiness, min_affection, time_of_day,
                held_item_id, evolution_item_id, known_move_id,
                known_move_type_id, location_id, party_species_id,
                party_type_id, trade_species_id, needs_overworld_rain,
                turn_upside_down, relative_physical_stats, gender_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            int(row['evolved_species_id']),
            None,  # evolves_from_species_id 需要从其他表推导，暂时设为NULL
            int(row['evolution_trigger_id']),
            int(row['minimum_level']) if row['minimum_level'] else None,
            int(row['minimum_happiness']) if row['minimum_happiness'] else None,
            int(row['minimum_affection']) if row['minimum_affection'] else None,
            row.get('time_of_day'),
            int(row['held_item_id']) if row['held_item_id'] else None,
            int(row['trigger_item_id']) if row['trigger_item_id'] else None,
            int(row['known_move_id']) if row['known_move_id'] else None,
            int(row['known_move_type_id']) if row['known_move_type_id'] else None,
            int(row['location_id']) if row['location_id'] else None,
            int(row['party_species_id']) if row['party_species_id'] else None,
            int(row['party_type_id']) if row['party_type_id'] else None,
            int(row['trade_species_id']) if row['trade_species_id'] else None,
            1 if row['needs_overworld_rain'] == '1' else 0,
            1 if row['turn_upside_down'] == '1' else 0,
            int(row['relative_physical_stats']) if row['relative_physical_stats'] else None,
            int(row['gender_id']) if row['gender_id'] else None
        ))
    
    conn.commit()
    conn.close()
    print(f"✅ 导入 {len(data)} 条进化详情")

# ==========================================
# 主函数
# ==========================================

def main():
    print("=" * 60)
    print("🚀 开始导入数据到 SQLite 数据库")
    print(f"📁 数据库路径: {DB_PATH}")
    print(f"📁 CSV目录: {CSV_DIR}")
    print("=" * 60)
    print()
    
    # 执行初始化脚本
    print("📋 执行数据库初始化脚本...")
    import sqlite3
    conn = sqlite3.connect(DB_PATH)
    with open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'database_init_sqlite.sql'), 'r', encoding='utf-8') as f:
        sql_script = f.read()
        conn.executescript(sql_script)
    conn.commit()
    conn.close()
    print("✅ 数据库初始化完成")
    print()
    
    # 导入数据
    try:
        # 基础数据
        import_genders()
        import_growth_rates()
        import_egg_groups()
        import_natures()
        import_move_learn_methods()
        print()
        
        # 核心数据
        import_types()
        import_type_efficacy()
        import_abilities()
        import_moves()
        import_move_meta()
        import_move_flags()
        import_move_stat_changes()
        import_items()
        print()
        
        # 宝可梦数据
        import_evolution_chains()
        import_pokemon_species()
        import_pokemon_forms()
        import_pokemon_types()
        import_pokemon_abilities()
        import_pokemon_stats()
        import_pokemon_egg_groups()
        import_pokemon_moves()
        import_pokemon_evolution()
        print()
        
        print("=" * 60)
        print("🎉 数据导入完成！")
        print("=" * 60)
        
    except Exception as e:
        print(f"❌ 导入失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()