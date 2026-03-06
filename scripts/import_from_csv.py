#!/usr/bin/env python3
"""
从 PokeAPI CSV 文件导入数据到 Pokemon Factory 数据库 V2
适配新的表结构设计，专为图鉴和对战功能优化
"""

import csv
import mysql.connector
import os
import sys
from datetime import datetime

# 数据库配置
DB_CONFIG = {
    "host": "10.144.55.168",
    "port": 3306,
    "user": "root",
    "password": "753951",
    "database": "pokemon_factory",
    "charset": "utf8mb4",
}

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
    return mysql.connector.connect(**DB_CONFIG)

def load_csv(filename):
    """加载CSV文件"""
    filepath = os.path.join(CSV_DIR, filename)
    if not os.path.exists(filepath):
        print(f"⚠️  文件不存在: {filepath}")
        return []
    
    with open(filepath, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        return list(reader)


# ==========================================
# 基础数据导入
# ==========================================

def import_generations():
    """导入世代表"""
    print("📥 导入世代表...")
    
    generation_data = {
        1: ('第一世代', 'generation-i', 'kanto', 1996),
        2: ('第二世代', 'generation-ii', 'johto', 1999),
        3: ('第三世代', 'generation-iii', 'hoenn', 2002),
        4: ('第四世代', 'generation-iv', 'sinnoh', 2006),
        5: ('第五世代', 'generation-v', 'unova', 2010),
        6: ('第六世代', 'generation-vi', 'kalos', 2013),
        7: ('第七世代', 'generation-vii', 'alola', 2016),
        8: ('第八世代', 'generation-viii', 'galar', 2019),
        9: ('第九世代', 'generation-ix', 'paldea', 2022),
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `generation`")
    
    for gid, (name, name_en, region, year) in generation_data.items():
        cursor.execute("""
            INSERT INTO `generation` (id, name, name_en, region, release_year, created_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (gid, name, name_en, region, year, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入世代数据: {len(generation_data)} 条")
    return len(generation_data)


def import_types():
    """导入属性数据"""
    print("📥 导入属性数据...")
    
    types_data = load_csv('types.csv')
    type_names = load_csv('type_names.csv')
    
    # 构建名称映射
    name_map = {}
    for row in type_names:
        type_id = int(row['type_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        
        if type_id not in name_map:
            name_map[type_id] = {}
        name_map[type_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `type`")
    
    insert_count = 0
    for row in types_data:
        type_id = int(row['id'])
        identifier = row['identifier']
        
        names = name_map.get(type_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or identifier
        name_jp = names.get('ja') or identifier
        
        color = TYPE_COLORS.get(identifier, '#68A090')
        
        cursor.execute("""
            INSERT INTO `type` (id, name, name_en, name_jp, color, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (type_id, name_zh, identifier, name_jp, color, datetime.now(), datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入属性数据: {insert_count} 条")
    return insert_count


def import_type_efficacy():
    """导入属性相性数据"""
    print("📥 导入属性相性数据...")
    
    efficacy_data = load_csv('type_efficacy.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `type_efficacy`")
    
    insert_count = 0
    for row in efficacy_data:
        attacking_type_id = int(row['damage_type_id'])
        defending_type_id = int(row['target_type_id'])
        damage_factor = int(row['damage_factor'])
        
        cursor.execute("""
            INSERT INTO `type_efficacy` (attacking_type_id, defending_type_id, damage_factor, created_at)
            VALUES (%s, %s, %s, %s)
        """, (attacking_type_id, defending_type_id, damage_factor, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入属性相性数据: {insert_count} 条")
    return insert_count


def import_stats():
    """导入能力值类型"""
    print("📥 导入能力值类型...")
    
    stat_data = {
        1: ('HP', 'hp', 'HP', 0, 1),
        2: ('攻击', 'attack', 'こうげき', 0, 2),
        3: ('防御', 'defense', 'ぼうぎょ', 0, 3),
        4: ('特攻', 'special-attack', 'とくこう', 0, 4),
        5: ('特防', 'special-defense', 'とくぼう', 0, 5),
        6: ('速度', 'speed', 'すばやさ', 0, 6),
        7: ('命中率', 'accuracy', 'めいちゅう', 1, 7),
        8: ('闪避率', 'evasion', 'かいひ', 1, 8),
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `stat`")
    
    for sid, (name, name_en, name_jp, is_battle, game_index) in stat_data.items():
        cursor.execute("""
            INSERT INTO `stat` (id, name, name_en, name_jp, is_battle_only, game_index, created_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (sid, name, name_en, name_jp, is_battle, game_index, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入能力值类型: {len(stat_data)} 条")
    return len(stat_data)


def import_move_damage_classes():
    """导入技能伤害类型"""
    print("📥 导入技能伤害类型...")
    
    class_data = {
        1: ('物理', 'physical', '伤害取决于攻击和防御'),
        2: ('特殊', 'special', '伤害取决于特攻和特防'),
        3: ('变化', 'status', '不造成直接伤害'),
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_damage_class`")
    
    for cid, (name, name_en, desc) in class_data.items():
        cursor.execute("""
            INSERT INTO `move_damage_class` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (cid, name, name_en, desc, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能伤害类型: {len(class_data)} 条")
    return len(class_data)


def import_move_learn_methods():
    """导入技能学习方式"""
    print("📥 导入技能学习方式...")
    
    method_data = {
        1: ('升级', 'level-up', '通过升级学习'),
        2: ('技能机', 'machine', '通过技能机学习'),
        3: ('传授', 'tutor', '通过传授学习'),
        4: ('遗传', 'egg', '通过遗传学习'),
        5: ('其他', 'stadium-surfing-pikachu', '特殊方式学习'),
        6: ('其他', 'colosseum-purification', '净化学习'),
        7: ('其他', 'xd-shadow', '暗影技能'),
        8: ('其他', 'xd-purification', '净化学习'),
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_learn_method`")
    
    for mid, (name, name_en, desc) in method_data.items():
        cursor.execute("""
            INSERT INTO `move_learn_method` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (mid, name, name_en, desc, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能学习方式: {len(method_data)} 条")
    return len(method_data)


def import_abilities():
    """导入特性数据"""
    print("📥 导入特性数据...")
    
    abilities_data = load_csv('abilities.csv')
    ability_names = load_csv('ability_names.csv')
    ability_prose = load_csv('ability_prose.csv')
    ability_flavor_text = load_csv('ability_flavor_text.csv')
    
    # 构建名称映射
    name_map = {}
    for row in ability_names:
        ability_id = int(row['ability_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        
        if ability_id not in name_map:
            name_map[ability_id] = {}
        name_map[ability_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    # 构建效果描述映射 - 分别存储短效果和详细效果
    effect_map = {}
    detail_map = {}
    for row in ability_prose:
        ability_id = int(row['ability_id'])
        lang_id = int(row['local_language_id'])
        short_effect = row.get('short_effect', '') or ''
        effect = row.get('effect', '') or ''
        
        if ability_id not in effect_map:
            effect_map[ability_id] = {}
            detail_map[ability_id] = {}
        
        lang = LANGUAGE_MAP.get(lang_id, lang_id)
        effect_map[ability_id][lang] = short_effect
        detail_map[ability_id][lang] = effect
    
    # 构建游戏描述映射 (flavor_text)
    flavor_map = {}
    for row in ability_flavor_text:
        ability_id = int(row['ability_id'])
        lang_id = int(row['language_id'])
        flavor = row.get('flavor_text', '') or ''
        
        if ability_id not in flavor_map:
            flavor_map[ability_id] = {}
        lang = LANGUAGE_MAP.get(lang_id, lang_id)
        # 只保存第一个找到的描述（通常是最新的）
        if lang not in flavor_map[ability_id]:
            flavor_map[ability_id][lang] = flavor
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `ability`")
    
    insert_count = 0
    for row in abilities_data:
        ability_id = int(row['id'])
            
        identifier = row['identifier']
        generation_id = int(row['generation_id']) if row['generation_id'] else None
        is_main_series = int(row['is_main_series']) if row['is_main_series'] else 1
        
        names = name_map.get(ability_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or identifier
        name_jp = names.get('ja') or identifier
        
        effects = effect_map.get(ability_id, {})
        effect_zh = effects.get('zh-hans') or effects.get('zh-hant') or effects.get('en') or ''
        effect_en = effects.get('en') or ''
        
        details = detail_map.get(ability_id, {})
        detail_zh = details.get('zh-hans') or details.get('zh-hant') or details.get('en') or ''
        detail_en = details.get('en') or ''
        
        # 游戏描述
        flavors = flavor_map.get(ability_id, {})
        flavor_zh = flavors.get('zh-hans') or flavors.get('zh-hant') or flavors.get('en') or ''
        
        cursor.execute("""
            INSERT INTO `ability` (id, name, name_en, name_jp, description, description_en, effect_detail, generation_id, is_main_series, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (ability_id, name_zh, identifier, name_jp, flavor_zh or effect_zh, effect_en, detail_en, generation_id, is_main_series, datetime.now(), datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入特性数据: {insert_count} 条")
    return insert_count


def import_moves():
    """导入技能数据"""
    print("📥 导入技能数据...")
    
    moves_data = load_csv('moves.csv')
    move_names = load_csv('move_names.csv')
    move_effect_prose = load_csv('move_effect_prose.csv')
    move_meta = load_csv('move_meta.csv')
    move_flavor_text = load_csv('move_flavor_text.csv')
    
    # 构建名称映射
    name_map = {}
    for row in move_names:
        move_id = int(row['move_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        
        if move_id not in name_map:
            name_map[move_id] = {}
        name_map[move_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    # 构建效果映射
    effect_map = {}
    for row in move_effect_prose:
        effect_id = int(row['move_effect_id'])
        lang_id = int(row['local_language_id'])
        effect = row.get('effect', '')
        short_effect = row.get('short_effect', '')
        
        if effect_id not in effect_map:
            effect_map[effect_id] = {}
        if lang_id == 9:
            effect_map[effect_id] = {'effect': effect, 'short': short_effect}
    
    # 构建游戏描述映射 (flavor_text)
    flavor_map = {}
    for row in move_flavor_text:
        move_id = int(row['move_id'])
        lang_id = int(row['language_id'])
        flavor = row.get('flavor_text', '') or ''
        
        if move_id not in flavor_map:
            flavor_map[move_id] = {}
        lang = LANGUAGE_MAP.get(lang_id, lang_id)
        if lang not in flavor_map[move_id]:
            flavor_map[move_id][lang] = flavor
    
    # 构建元数据映射
    meta_map = {}
    for row in move_meta:
        move_id = int(row['move_id'])
        meta_map[move_id] = row
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move`")
    cursor.execute("TRUNCATE TABLE `move_meta`")
    
    insert_count = 0
    for row in moves_data:
        move_id = int(row['id'])
        identifier = row['identifier']
        generation_id = int(row['generation_id']) if row['generation_id'] else None
        type_id = int(row['type_id']) if row['type_id'] else None
        damage_class_id = int(row['damage_class_id']) if row['damage_class_id'] else None
        target_id = int(row['target_id']) if row['target_id'] else None
        power = int(row['power']) if row['power'] else None
        pp = int(row['pp']) if row['pp'] else None
        accuracy = int(row['accuracy']) if row['accuracy'] else None
        priority = int(row['priority']) if row['priority'] else 0
        effect_chance = int(row['effect_chance']) if row['effect_chance'] else None
        effect_id = int(row['effect_id']) if row['effect_id'] else None
        
        names = name_map.get(move_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or identifier
        name_jp = names.get('ja') or identifier
        
        # 获取效果
        effect_info = effect_map.get(effect_id, {})
        effect_short = effect_info.get('short', '')
        effect_detail = effect_info.get('effect', '')
        
        # 获取游戏描述
        flavors = flavor_map.get(move_id, {})
        flavor_zh = flavors.get('zh-hans') or flavors.get('zh-hant') or flavors.get('en') or ''
        
        cursor.execute("""
            INSERT INTO `move` (id, name, name_en, name_jp, type_id, damage_class_id, target_id, power, pp, accuracy, priority, effect_chance, description, effect_short, effect_detail, generation_id, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (move_id, name_zh, identifier, name_jp, type_id, damage_class_id, target_id, power, pp, accuracy, priority, effect_chance, flavor_zh, effect_short, effect_detail, generation_id, datetime.now(), datetime.now()))
        
        # 插入元数据
        meta = meta_map.get(move_id)
        if meta:
            def safe_int(val):
                if val is None or val == '':
                    return None
                try:
                    return int(val)
                except:
                    return None
            
            cursor.execute("""
                INSERT INTO `move_meta` (move_id, min_hits, max_hits, min_turns, max_turns, drain, healing, crit_rate, ailment_chance, flinch_chance, stat_chance, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (
                move_id, 
                safe_int(meta.get('min_hits')), safe_int(meta.get('max_hits')),
                safe_int(meta.get('min_turns')), safe_int(meta.get('max_turns')),
                safe_int(meta.get('drain')), safe_int(meta.get('healing')),
                safe_int(meta.get('crit_rate')), safe_int(meta.get('ailment_chance')),
                safe_int(meta.get('flinch_chance')), safe_int(meta.get('stat_chance')),
                datetime.now()
            ))
        
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能数据: {insert_count} 条")
    return insert_count


def import_egg_groups():
    """导入蛋群数据"""
    print("📥 导入蛋群数据...")
    
    egg_groups_data = load_csv('egg_groups.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `egg_group`")
    
    insert_count = 0
    for row in egg_groups_data:
        egg_group_id = int(row['id'])
        identifier = row['identifier']
        
        names = EGG_GROUP_NAMES.get(egg_group_id, (identifier, identifier, identifier))
        
        cursor.execute("""
            INSERT INTO `egg_group` (id, name, name_en, name_jp, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (egg_group_id, names[0], names[1], names[2], datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入蛋群数据: {insert_count} 条")
    return insert_count


def import_growth_rates():
    """导入经验成长类型"""
    print("📥 导入经验成长类型...")
    
    growth_data = load_csv('growth_rates.csv')
    growth_prose = load_csv('growth_rate_prose.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `growth_rate`")
    
    # 预定义的成长类型
    predefined = {
        1: ('慢速', 'slow', '5n³/4', '升级所需经验较多'),
        2: ('快速', 'fast', '4n³/5', '升级所需经验较少'),
        3: ('中速', 'medium', 'n³', '标准成长速度'),
        4: ('中慢速', 'medium-slow', '6n³/5 - 15n² + 100n - 140', '前期较快后期较慢'),
        5: ('慢速后极快', 'slow-then-very-fast', 'n⁴/2', '初期慢后期快'),
        6: ('快速后极慢', 'fast-then-very-slow', '特殊公式', '初期快后期慢'),
    }
    
    for gid, (name, name_en, formula, desc) in predefined.items():
        cursor.execute("""
            INSERT INTO `growth_rate` (id, name, name_en, formula, description, created_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (gid, name, name_en, formula, desc, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入经验成长类型: {len(predefined)} 条")
    return len(predefined)


def import_natures():
    """导入性格数据"""
    print("📥 导入性格数据...")
    
    natures_data = load_csv('natures.csv')
    nature_names = load_csv('nature_names.csv')
    
    # 构建名称映射
    name_map = {}
    for row in nature_names:
        nature_id = int(row['nature_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        
        if nature_id not in name_map:
            name_map[nature_id] = {}
        name_map[nature_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    # 能力值名称映射
    stat_names = {
        1: 'hp', 2: 'attack', 3: 'defense', 
        4: 'special-attack', 5: 'special-defense', 6: 'speed'
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `nature`")
    
    insert_count = 0
    for row in natures_data:
        nature_id = int(row['id'])
        identifier = row['identifier']
        decreased_stat = int(row['decreased_stat_id']) if row['decreased_stat_id'] else None
        increased_stat = int(row['increased_stat_id']) if row['increased_stat_id'] else None
        hates_flavor = int(row['hates_flavor_id']) if row['hates_flavor_id'] else None
        likes_flavor = int(row['likes_flavor_id']) if row['likes_flavor_id'] else None
        
        names = name_map.get(nature_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or identifier
        name_jp = names.get('ja') or identifier
        
        increased = stat_names.get(increased_stat, '')
        decreased = stat_names.get(decreased_stat, '')
        
        cursor.execute("""
            INSERT INTO `nature` (id, name, name_en, name_jp, increased_stat, decreased_stat, created_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (nature_id, name_zh, identifier, name_jp, increased, decreased, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入性格数据: {insert_count} 条")
    return insert_count


def import_evolution_triggers():
    """导入进化触发条件"""
    print("📥 导入进化触发条件...")
    
    trigger_data = {
        1: ('升级', 'level-up'),
        2: ('交换', 'trade'),
        3: ('使用物品', 'use-item'),
        4: ('其他', 'other'),
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `evolution_trigger`")
    
    for tid, (name, name_en) in trigger_data.items():
        cursor.execute("""
            INSERT INTO `evolution_trigger` (id, name, name_en, created_at)
            VALUES (%s, %s, %s, %s)
        """, (tid, name, name_en, datetime.now()))
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入进化触发条件: {len(trigger_data)} 条")
    return len(trigger_data)


# ==========================================
# 宝可梦核心数据导入
# ==========================================

def import_evolution_chains():
    """导入进化链"""
    print("📥 导入进化链...")
    
    evolution_chains_data = load_csv('evolution_chains.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `evolution_chain`")
    
    insert_count = 0
    for row in evolution_chains_data:
        chain_id = int(row['id'])
        baby_trigger_item = int(row['baby_trigger_item_id']) if row['baby_trigger_item_id'] else None
        
        cursor.execute("""
            INSERT INTO `evolution_chain` (id, baby_trigger_item_id, created_at)
            VALUES (%s, %s, %s)
        """, (chain_id, baby_trigger_item, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入进化链: {insert_count} 条")
    return insert_count


def import_pokemon_species():
    """导入宝可梦物种数据"""
    print("📥 导入宝可梦物种数据...")
    
    species_data = load_csv('pokemon_species.csv')
    species_names = load_csv('pokemon_species_names.csv')
    species_prose = load_csv('pokemon_species_prose.csv')
    species_flavor_text = load_csv('pokemon_species_flavor_text.csv')
    
    # 构建名称映射
    name_map = {}
    for row in species_names:
        species_id = int(row['pokemon_species_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        genus = row.get('genus', '')
        
        if species_id not in name_map:
            name_map[species_id] = {}
        name_map[species_id][LANGUAGE_MAP.get(lang_id, lang_id)] = {'name': name, 'genus': genus}
    
    # 构建描述映射
    desc_map = {}
    for row in species_prose:
        species_id = int(row['pokemon_species_id'])
        lang_id = int(row['local_language_id'])
        desc = row.get('description', '') or row.get('genus', '')
        
        if species_id not in desc_map:
            desc_map[species_id] = {}
        desc_map[species_id][LANGUAGE_MAP.get(lang_id, lang_id)] = desc
    
    # 游戏描述映射 (flavor_text)
    flavor_map = {}
    for row in species_flavor_text:
        species_id = int(row['species_id'])
        lang_id = int(row['language_id'])
        flavor = row.get('flavor_text', '') or ''
        
        if species_id not in flavor_map:
            flavor_map[species_id] = {}
        lang = LANGUAGE_MAP.get(lang_id, lang_id)
        if lang not in flavor_map[species_id]:
            flavor_map[species_id][lang] = flavor
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_species`")
    
    insert_count = 0
    for row in species_data:
        species_id = int(row['id'])
            
        identifier = row['identifier']
        generation_id = int(row['generation_id']) if row['generation_id'] else None
        evolves_from = int(row['evolves_from_species_id']) if row['evolves_from_species_id'] else None
        evolution_chain_id = int(row['evolution_chain_id']) if row['evolution_chain_id'] else None
        color_id = int(row['color_id']) if row['color_id'] else None
        shape_id = int(row['shape_id']) if row['shape_id'] else None
        habitat_id = int(row['habitat_id']) if row['habitat_id'] else None
        growth_rate_id = int(row['growth_rate_id']) if row['growth_rate_id'] else None
        gender_rate = int(row['gender_rate']) if row['gender_rate'] else -1
        capture_rate = int(row['capture_rate']) if row['capture_rate'] else 45
        base_happiness = int(row['base_happiness']) if row['base_happiness'] else 70
        hatch_counter = int(row['hatch_counter']) if row['hatch_counter'] else 20
        is_baby = int(row['is_baby']) if row['is_baby'] else 0
        is_legendary = int(row['is_legendary']) if row['is_legendary'] else 0
        is_mythical = int(row['is_mythical']) if row['is_mythical'] else 0
        has_gender_diff = int(row['has_gender_differences']) if row['has_gender_differences'] else 0
        forms_switchable = int(row['forms_switchable']) if row['forms_switchable'] else 0
        order = int(row['order']) if row['order'] else species_id
        
        names = name_map.get(species_id, {})
        name_info = names.get('zh-hans') or names.get('zh-hant') or names.get('en') or {'name': identifier, 'genus': ''}
        name_zh = name_info.get('name', identifier)
        genus = name_info.get('genus', '')
        name_jp = names.get('ja', {}).get('name', identifier)
        
        # 游戏描述优先
        flavors = flavor_map.get(species_id, {})
        flavor_zh = flavors.get('zh-hans') or flavors.get('zh-hant') or flavors.get('en') or ''
        
        color = COLOR_NAMES.get(color_id, '')
        shape = SHAPE_NAMES.get(shape_id, '')
        
        cursor.execute("""
            INSERT INTO `pokemon_species` (
                id, name, name_en, name_jp, genus, description, generation_id, evolution_chain_id, 
                evolves_from_species_id, color, shape, growth_rate_id, gender_rate, 
                capture_rate, base_happiness, hatch_counter, is_baby, is_legendary, 
                is_mythical, has_gender_differences, forms_switchable, `order`, created_at, updated_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            species_id, name_zh, identifier, name_jp, genus, flavor_zh, generation_id, evolution_chain_id,
            evolves_from, color, shape, growth_rate_id, gender_rate,
            capture_rate, base_happiness, hatch_counter, is_baby, is_legendary,
            is_mythical, has_gender_diff, forms_switchable, order, datetime.now(), datetime.now()
        ))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物种数据: {insert_count} 条")
    return insert_count


def import_pokemon_forms():
    """导入宝可梦形态数据"""
    print("📥 导入宝可梦形态数据...")
    
    pokemon_data = load_csv('pokemon.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_form`")
    
    insert_count = 0
    for row in pokemon_data:
        pokemon_id = int(row['id'])
            
        species_id = int(row['species_id'])
        identifier = row['identifier']
        height = float(row['height']) / 10.0 if row['height'] else 0
        weight = float(row['weight']) / 10.0 if row['weight'] else 0
        base_experience = int(row['base_experience']) if row['base_experience'] else 0
        is_default = int(row['is_default']) if row['is_default'] else 1
        order = int(row['order']) if row['order'] else pokemon_id
        
        # 获取物种名称
        cursor.execute("SELECT name FROM `pokemon_species` WHERE id = %s", (species_id,))
        species = cursor.fetchone()
        name_zh = species[0] if species else identifier
        
        cursor.execute("""
            INSERT INTO `pokemon_form` (
                id, species_id, form_name_zh, is_default, height, weight, 
                base_experience, `order`, created_at, updated_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (pokemon_id, species_id, name_zh, is_default, height, weight, base_experience, order, datetime.now(), datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入形态数据: {insert_count} 条")
    return insert_count


def import_pokemon_form_types():
    """导入形态属性关联"""
    print("📥 导入形态属性关联...")
    
    pokemon_types_data = load_csv('pokemon_types.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_form_type`")
    
    insert_count = 0
    for row in pokemon_types_data:
        pokemon_id = int(row['pokemon_id'])
            
        type_id = int(row['type_id'])
        slot = int(row['slot'])
        
        cursor.execute("""
            INSERT INTO `pokemon_form_type` (form_id, type_id, slot, created_at)
            VALUES (%s, %s, %s, %s)
        """, (pokemon_id, type_id, slot, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入形态属性关联: {insert_count} 条")
    return insert_count


def import_pokemon_form_abilities():
    """导入形态特性关联"""
    print("📥 导入形态特性关联...")
    
    pokemon_abilities_data = load_csv('pokemon_abilities.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_form_ability`")
    
    insert_count = 0
    for row in pokemon_abilities_data:
        pokemon_id = int(row['pokemon_id'])
            
        ability_id = int(row['ability_id'])
            
        is_hidden = int(row['is_hidden']) if row['is_hidden'] else 0
        slot = int(row['slot']) if row['slot'] else 1
        
        cursor.execute("""
            INSERT INTO `pokemon_form_ability` (form_id, ability_id, is_hidden, slot, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (pokemon_id, ability_id, is_hidden, slot, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入形态特性关联: {insert_count} 条")
    return insert_count


def import_pokemon_form_stats():
    """导入形态种族值（纵向存储）"""
    print("📥 导入形态种族值...")
    
    pokemon_stats_data = load_csv('pokemon_stats.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_form_stat`")
    
    insert_count = 0
    for row in pokemon_stats_data:
        pokemon_id = int(row['pokemon_id'])
            
        stat_id = int(row['stat_id'])
        base_stat = int(row['base_stat'])
        effort = int(row['effort']) if row['effort'] else 0
        
        cursor.execute("""
            INSERT INTO `pokemon_form_stat` (form_id, stat_id, base_stat, effort, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (pokemon_id, stat_id, base_stat, effort, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入形态种族值: {insert_count} 条")
    return insert_count


def import_pokemon_species_egg_groups():
    """导入物种蛋群关联"""
    print("📥 导入物种蛋群关联...")
    
    egg_group_data = load_csv('pokemon_egg_groups.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_species_egg_group`")
    
    insert_count = 0
    for row in egg_group_data:
        species_id = int(row['species_id'])
        egg_group_id = int(row['egg_group_id'])
        
        cursor.execute("""
            INSERT INTO `pokemon_species_egg_group` (species_id, egg_group_id, created_at)
            VALUES (%s, %s, %s)
        """, (species_id, egg_group_id, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物种蛋群关联: {insert_count} 条")
    return insert_count


def import_pokemon_evolution():
    """导入进化详情"""
    print("📥 导入进化详情...")
    
    evolution_data = load_csv('pokemon_evolution.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_evolution`")
    
    insert_count = 0
    for row in evolution_data:
        evolved_species_id = int(row['evolved_species_id'])
            
        evolution_trigger_id = int(row['evolution_trigger_id']) if row.get('evolution_trigger_id') else None
        min_level = int(row['minimum_level']) if row.get('minimum_level') else None
        min_happiness = int(row['minimum_happiness']) if row.get('minimum_happiness') else None
        min_affection = int(row['minimum_affection']) if row.get('minimum_affection') else None
        min_beauty = int(row['minimum_beauty']) if row.get('minimum_beauty') else None
        time_of_day = row.get('time_of_day') if row.get('time_of_day') else None
        held_item_id = int(row['held_item_id']) if row.get('held_item_id') else None
        evolution_item_id = int(row['trigger_item_id']) if row.get('trigger_item_id') else None
        known_move_id = int(row['known_move_id']) if row.get('known_move_id') else None
        known_move_type_id = int(row['known_move_type_id']) if row.get('known_move_type_id') else None
        location_id = int(row['location_id']) if row.get('location_id') else None
        party_species_id = int(row['party_species_id']) if row.get('party_species_id') else None
        party_type_id = int(row['party_type_id']) if row.get('party_type_id') else None
        trade_species_id = int(row['trade_species_id']) if row.get('trade_species_id') else None
        needs_overworld_rain = int(row['needs_overworld_rain']) if row.get('needs_overworld_rain') else 0
        needs_multiplayer = int(row['needs_multiplayer']) if row.get('needs_multiplayer') else 0
        turn_upside_down = int(row['turn_upside_down']) if row.get('turn_upside_down') else 0
        relative_physical_stats = int(row['relative_physical_stats']) if row.get('relative_physical_stats') else None
        gender_id = int(row['gender_id']) if row.get('gender_id') else None
        
        # 获取进化前物种
        cursor.execute("SELECT evolves_from_species_id FROM `pokemon_species` WHERE id = %s", (evolved_species_id,))
        species = cursor.fetchone()
        evolves_from = species[0] if species else None
        
        if not evolves_from:
            continue
        
        cursor.execute("""
            INSERT INTO `pokemon_evolution` (
                evolved_species_id, evolves_from_species_id, evolution_trigger_id,
                min_level, min_happiness, min_affection, min_beauty, time_of_day,
                held_item_id, evolution_item_id, known_move_id, known_move_type_id,
                location_id, party_species_id, party_type_id, trade_species_id,
                needs_overworld_rain, needs_multiplayer, turn_upside_down,
                relative_physical_stats, gender_id, created_at
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            evolved_species_id, evolves_from, evolution_trigger_id,
            min_level, min_happiness, min_affection, min_beauty, time_of_day,
            held_item_id, evolution_item_id, known_move_id, known_move_type_id,
            location_id, party_species_id, party_type_id, trade_species_id,
            needs_overworld_rain, needs_multiplayer, turn_upside_down,
            relative_physical_stats, gender_id, datetime.now()
        ))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入进化详情: {insert_count} 条")
    return insert_count


# ==========================================
# 道具相关数据导入
# ==========================================

def import_item_pockets():
    """导入物品口袋"""
    print("📥 导入物品口袋...")
    
    pockets_data = load_csv('item_pockets.csv')
    pocket_names = load_csv('item_pocket_names.csv')
    
    name_map = {}
    for row in pocket_names:
        pocket_id = int(row['item_pocket_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        if pocket_id not in name_map:
            name_map[pocket_id] = {}
        name_map[pocket_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item_pocket`")
    
    insert_count = 0
    for row in pockets_data:
        pocket_id = int(row['id'])
        identifier = row['identifier']
        
        names = name_map.get(pocket_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or names.get('en') or identifier
        
        cursor.execute("""
            INSERT INTO `item_pocket` (id, name, name_en, created_at)
            VALUES (%s, %s, %s, %s)
        """, (pocket_id, name_zh, identifier, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品口袋: {insert_count} 条")
    return insert_count


def import_item_categories():
    """导入物品分类"""
    print("📥 导入物品分类...")
    
    categories_data = load_csv('item_categories.csv')
    category_prose = load_csv('item_category_prose.csv')
    
    name_map = {}
    for row in category_prose:
        cat_id = int(row['item_category_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        if cat_id not in name_map:
            name_map[cat_id] = {}
        name_map[cat_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item_category`")
    
    insert_count = 0
    for row in categories_data:
        cat_id = int(row['id'])
        identifier = row['identifier']
        pocket_id = int(row['pocket_id']) if row.get('pocket_id') else None
        
        names = name_map.get(cat_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or names.get('en') or identifier
        
        cursor.execute("""
            INSERT INTO `item_category` (id, name, name_en, pocket_id, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (cat_id, name_zh, identifier, pocket_id, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品分类: {insert_count} 条")
    return insert_count


def import_item_fling_effects():
    """导入物品投掷效果"""
    print("📥 导入物品投掷效果...")
    
    fling_data = load_csv('item_fling_effects.csv')
    fling_prose = load_csv('item_fling_effect_prose.csv')
    
    effect_map = {}
    for row in fling_prose:
        effect_id = int(row['item_fling_effect_id'])
        lang_id = int(row['local_language_id'])
        effect = row.get('effect', '')
        if effect_id not in effect_map:
            effect_map[effect_id] = {}
        effect_map[effect_id][LANGUAGE_MAP.get(lang_id, lang_id)] = effect
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item_fling_effect`")
    
    insert_count = 0
    for row in fling_data:
        effect_id = int(row['id'])
        identifier = row['identifier']
        
        effects = effect_map.get(effect_id, {})
        effect_zh = effects.get('zh-hans') or effects.get('zh-hant') or effects.get('en') or ''
        
        cursor.execute("""
            INSERT INTO `item_fling_effect` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (effect_id, identifier, identifier, effect_zh, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品投掷效果: {insert_count} 条")
    return insert_count


def import_item_flags():
    """导入物品属性"""
    print("📥 导入物品属性...")
    
    flags_data = load_csv('item_flags.csv')
    flag_prose = load_csv('item_flag_prose.csv')
    
    name_map = {}
    for row in flag_prose:
        flag_id = int(row['item_flag_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        desc = row.get('description', '')
        if flag_id not in name_map:
            name_map[flag_id] = {}
        name_map[flag_id][LANGUAGE_MAP.get(lang_id, lang_id)] = {'name': name, 'desc': desc}
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item_flag`")
    
    insert_count = 0
    for row in flags_data:
        flag_id = int(row['id'])
        identifier = row['identifier']
        
        info = name_map.get(flag_id, {})
        info_zh = info.get('zh-hans') or info.get('zh-hant') or info.get('en') or {}
        name_zh = info_zh.get('name', identifier)
        desc_zh = info_zh.get('desc', '')
        
        cursor.execute("""
            INSERT INTO `item_flag` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (flag_id, name_zh, identifier, desc_zh, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品属性: {insert_count} 条")
    return insert_count


def import_items():
    """导入物品数据"""
    print("📥 导入物品数据...")
    
    items_data = load_csv('items.csv')
    item_names = load_csv('item_names.csv')
    item_prose = load_csv('item_prose.csv')
    item_flavor_text = load_csv('item_flavor_text.csv')
    
    name_map = {}
    for row in item_names:
        item_id = int(row['item_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        if item_id not in name_map:
            name_map[item_id] = {}
        name_map[item_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    # 分别存储短效果和详细效果
    effect_map = {}
    detail_map = {}
    for row in item_prose:
        item_id = int(row['item_id'])
        lang_id = int(row['local_language_id'])
        short_effect = row.get('short_effect', '') or ''
        effect = row.get('effect', '') or ''
        
        if item_id not in effect_map:
            effect_map[item_id] = {}
            detail_map[item_id] = {}
        effect_map[item_id][LANGUAGE_MAP.get(lang_id, lang_id)] = short_effect
        detail_map[item_id][LANGUAGE_MAP.get(lang_id, lang_id)] = effect
    
    # 游戏描述映射
    flavor_map = {}
    for row in item_flavor_text:
        item_id = int(row['item_id'])
        lang_id = int(row['language_id'])
        flavor = row.get('flavor_text', '') or ''
        
        if item_id not in flavor_map:
            flavor_map[item_id] = {}
        lang = LANGUAGE_MAP.get(lang_id, lang_id)
        if lang not in flavor_map[item_id]:
            flavor_map[item_id][lang] = flavor
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item`")
    
    insert_count = 0
    for row in items_data:
        item_id = int(row['id'])
        identifier = row['identifier']
        category_id = int(row['category_id']) if row['category_id'] else None
        cost = int(row['cost']) if row['cost'] else 0
        fling_power = int(row['fling_power']) if row['fling_power'] else None
        fling_effect_id = int(row['fling_effect_id']) if row['fling_effect_id'] else None
        
        names = name_map.get(item_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or names.get('en') or identifier
        name_jp = names.get('ja') or identifier
        
        effects = effect_map.get(item_id, {})
        effect_zh = effects.get('zh-hans') or effects.get('zh-hant') or effects.get('en') or ''
        
        details = detail_map.get(item_id, {})
        detail_zh = details.get('zh-hans') or details.get('zh-hant') or details.get('en') or ''
        
        flavors = flavor_map.get(item_id, {})
        flavor_zh = flavors.get('zh-hans') or flavors.get('zh-hant') or flavors.get('en') or ''
        
        cursor.execute("""
            INSERT INTO `item` (id, name, name_en, name_jp, category_id, cost, fling_power, fling_effect_id, description, effect_short, effect_detail, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (item_id, name_zh, identifier, name_jp, category_id, cost, fling_power, fling_effect_id, flavor_zh, effect_zh, detail_zh, datetime.now(), datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品数据: {insert_count} 条")
    return insert_count


def import_item_flag_maps():
    """导入物品属性关联"""
    print("📥 导入物品属性关联...")
    
    flag_map_data = load_csv('item_flag_map.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `item_flag_map`")
    
    insert_count = 0
    for row in flag_map_data:
        item_id = int(row['item_id'])
        flag_id = int(row['item_flag_id'])
        
        cursor.execute("""
            INSERT INTO `item_flag_map` (item_id, flag_id, created_at)
            VALUES (%s, %s, %s)
        """, (item_id, flag_id, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入物品属性关联: {insert_count} 条")
    return insert_count


# ==========================================
# 战斗相关数据导入
# ==========================================

def import_move_targets():
    """导入技能目标类型"""
    print("📥 导入技能目标类型...")
    
    targets_data = load_csv('move_targets.csv')
    target_prose = load_csv('move_target_prose.csv')
    
    name_map = {}
    for row in target_prose:
        target_id = int(row['move_target_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        desc = row.get('description', '')
        if target_id not in name_map:
            name_map[target_id] = {}
        name_map[target_id][LANGUAGE_MAP.get(lang_id, lang_id)] = {'name': name, 'desc': desc}
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_target`")
    
    insert_count = 0
    for row in targets_data:
        target_id = int(row['id'])
        identifier = row['identifier']
        
        info = name_map.get(target_id, {})
        info_zh = info.get('zh-hans') or info.get('zh-hant') or info.get('en') or {}
        name_zh = info_zh.get('name', identifier)
        desc_zh = info_zh.get('desc', '')
        
        cursor.execute("""
            INSERT INTO `move_target` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (target_id, name_zh, identifier, desc_zh, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能目标类型: {insert_count} 条")
    return insert_count


def import_move_meta_ailments():
    """导入技能异常状态"""
    print("📥 导入技能异常状态...")
    
    ailments_data = load_csv('move_meta_ailments.csv')
    ailment_names = load_csv('move_meta_ailment_names.csv')
    
    name_map = {}
    for row in ailment_names:
        ailment_id = int(row['move_meta_ailment_id'])
        lang_id = int(row['local_language_id'])
        name = row['name']
        if ailment_id not in name_map:
            name_map[ailment_id] = {}
        name_map[ailment_id][LANGUAGE_MAP.get(lang_id, lang_id)] = name
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_meta_ailment`")
    
    # 状态中文名映射
    ailment_zh = {
        'paralysis': '麻痹', 'sleep': '睡眠', 'freeze': '冰冻',
        'burn': '灼伤', 'poison': '中毒', 'confusion': '混乱',
        'infatuation': '着迷', 'trap': '束缚', 'nightmare': '噩梦',
        'torment': '紧束', 'heal-block': '回复封锁', 'curse': '诅咒',
        'foresight': '识破', 'perish-song': '灭亡之歌', 'unknown': '未知', 'none': '无'
    }
    
    insert_count = 0
    for row in ailments_data:
        ailment_id = int(row['id'])
        identifier = row['identifier']
        
        names = name_map.get(ailment_id, {})
        name_zh = names.get('zh-hans') or names.get('zh-hant') or ailment_zh.get(identifier, identifier)
        
        cursor.execute("""
            INSERT INTO `move_meta_ailment` (id, name, name_en, created_at)
            VALUES (%s, %s, %s, %s)
        """, (ailment_id, name_zh, identifier, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能异常状态: {insert_count} 条")
    return insert_count


def import_move_meta_categories():
    """导入技能元数据类别"""
    print("📥 导入技能元数据类别...")
    
    categories_data = load_csv('move_meta_categories.csv')
    category_prose = load_csv('move_meta_category_prose.csv')
    
    name_map = {}
    for row in category_prose:
        cat_id = int(row['move_meta_category_id'])
        lang_id = int(row['local_language_id'])
        desc = row.get('description', '')
        if cat_id not in name_map:
            name_map[cat_id] = {}
        name_map[cat_id][LANGUAGE_MAP.get(lang_id, lang_id)] = desc
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_meta_category`")
    
    insert_count = 0
    for row in categories_data:
        cat_id = int(row['id'])
        identifier = row['identifier']
        
        info = name_map.get(cat_id, {})
        desc_zh = info.get('zh-hans') or info.get('zh-hant') or info.get('en') or ''
        
        cursor.execute("""
            INSERT INTO `move_meta_category` (id, name, name_en, description, created_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (cat_id, identifier, identifier, desc_zh, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能元数据类别: {insert_count} 条")
    return insert_count


def import_move_meta_stat_changes():
    """导入技能能力变化"""
    print("📥 导入技能能力变化...")
    
    stat_change_data = load_csv('move_meta_stat_changes.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `move_meta_stat_change`")
    
    insert_count = 0
    for row in stat_change_data:
        move_id = int(row['move_id'])
        stat_id = int(row['stat_id'])
        change = int(row['change'])
        
        cursor.execute("""
            INSERT INTO `move_meta_stat_change` (move_id, stat_id, `change`, created_at)
            VALUES (%s, %s, %s, %s)
        """, (move_id, stat_id, change, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入技能能力变化: {insert_count} 条")
    return insert_count


def import_version_groups():
    """导入版本组"""
    print("📥 导入版本组...")
    
    vg_data = load_csv('version_groups.csv')
    
    # 版本组中文名称映射
    vg_names = {
        'red-blue': '红/蓝', 'yellow': '黄', 'gold-silver': '金/银', 'crystal': '水晶',
        'ruby-sapphire': '红宝石/蓝宝石', 'emerald': '绿宝石', 'firered-leafgreen': '火红/叶绿',
        'diamond-pearl': '钻石/珍珠', 'platinum': '白金', 'heartgold-soulsilver': '心金/魂银',
        'black-white': '黑/白', 'black-2-white-2': '黑2/白2', 'x-y': 'X/Y',
        'omega-ruby-alpha-sapphire': '欧米茄红宝石/阿尔法蓝宝石', 'sun-moon': '太阳/月亮',
        'ultra-sun-ultra-moon': '究极之日/究极之月', 'lets-go-pikachu-eevee': 'Let\'s Go! 皮卡丘/伊布',
        'lets-go-pikachu-lets-go-eevee': 'Let\'s Go! 皮卡丘/伊布',
        'sword-shield': '剑/盾', 'the-isle-of-armor': '铠之孤岛', 'the-crown-tundra': '冠之雪原',
        'brilliant-diamond-and-shining-pearl': '晶灿钻石/明亮珍珠',
        'brilliant-diamond-shining-pear': '晶灿钻石/明亮珍珠',
        'legends-arceus': '阿尔宙斯', 'legends-za': '阿尔宙斯 Z-A',
        'scarlet-violet': '朱/紫', 'the-teal-mask': '碧之假面', 'the-indigo-disk': '蓝之圆盘',
        'colosseum': '圆形竞技场', 'xd': '暗之旋风', 'red-green-japan': '红/绿(日版)',
        'blue-japan': '蓝(日版)', 'mega-dimension': 'Mega维度'
    }
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `version_group`")
    
    insert_count = 0
    for row in vg_data:
        vg_id = int(row['id'])
        identifier = row['identifier']
        generation_id = int(row['generation_id']) if row['generation_id'] else None
        order = int(row['order']) if row['order'] else None
        
        name_zh = vg_names.get(identifier, identifier)
        
        cursor.execute("""
            INSERT INTO `version_group` (id, name, name_en, generation_id, `order`, created_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (vg_id, name_zh, identifier, generation_id, order, datetime.now()))
        insert_count += 1
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入版本组: {insert_count} 条")
    return insert_count


def import_pokemon_moves():
    """导入宝可梦可学技能"""
    print("📥 导入宝可梦可学技能...")
    
    pokemon_moves_data = load_csv('pokemon_moves.csv')
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE `pokemon_form_move`")
    
    insert_count = 0
    batch_size = 10000
    batch = []
    
    for row in pokemon_moves_data:
        pokemon_id = int(row['pokemon_id'])
        move_id = int(row['move_id'])
        method_id = int(row['pokemon_move_method_id'])
        level = int(row['level']) if row['level'] else None
        version_group_id = int(row['version_group_id']) if row['version_group_id'] else None
        
        batch.append((pokemon_id, move_id, method_id, level, version_group_id, datetime.now()))
        
        if len(batch) >= batch_size:
            cursor.executemany("""
                INSERT INTO `pokemon_form_move` (form_id, move_id, learn_method_id, level, version_group_id, created_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, batch)
            insert_count += len(batch)
            batch = []
    
    if batch:
        cursor.executemany("""
            INSERT INTO `pokemon_form_move` (form_id, move_id, learn_method_id, level, version_group_id, created_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, batch)
        insert_count += len(batch)
    
    conn.commit()
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    cursor.close()
    conn.close()
    
    print(f"✅ 导入宝可梦可学技能: {insert_count} 条")
    return insert_count


def main():
    """主函数"""
    print("=" * 60)
    print("🚀 Pokemon Factory CSV 数据导入工具 V2")
    print("=" * 60)
    
    # 检查CSV目录
    if not os.path.exists(CSV_DIR):
        print(f"❌ CSV目录不存在: {CSV_DIR}")
        sys.exit(1)
    
    try:
        results = {}
        
        # 第一阶段：基础数据表（无外键依赖）
        print("\n📦 第一阶段：导入基础数据...")
        results['generation'] = import_generations()
        results['type'] = import_types()
        results['type_efficacy'] = import_type_efficacy()
        results['stat'] = import_stats()
        results['move_damage_class'] = import_move_damage_classes()
        results['move_learn_method'] = import_move_learn_methods()
        results['ability'] = import_abilities()
        results['move_target'] = import_move_targets()
        results['move_meta_ailment'] = import_move_meta_ailments()
        results['move_meta_category'] = import_move_meta_categories()
        results['move'] = import_moves()
        results['move_meta_stat_change'] = import_move_meta_stat_changes()
        results['egg_group'] = import_egg_groups()
        results['growth_rate'] = import_growth_rates()
        results['nature'] = import_natures()
        results['evolution_trigger'] = import_evolution_triggers()
        
        # 第二阶段：道具相关数据
        print("\n📦 第二阶段：导入道具相关数据...")
        results['item_pocket'] = import_item_pockets()
        results['item_category'] = import_item_categories()
        results['item_fling_effect'] = import_item_fling_effects()
        results['item_flag'] = import_item_flags()
        results['item'] = import_items()
        results['item_flag_map'] = import_item_flag_maps()
        
        # 第三阶段：宝可梦核心数据
        print("\n📦 第三阶段：导入宝可梦核心数据...")
        results['evolution_chain'] = import_evolution_chains()
        results['version_group'] = import_version_groups()
        results['pokemon_species'] = import_pokemon_species()
        results['pokemon_form'] = import_pokemon_forms()
        
        # 第四阶段：关联数据
        print("\n📦 第四阶段：导入关联数据...")
        results['pokemon_form_type'] = import_pokemon_form_types()
        results['pokemon_form_ability'] = import_pokemon_form_abilities()
        results['pokemon_form_stat'] = import_pokemon_form_stats()
        results['pokemon_species_egg_group'] = import_pokemon_species_egg_groups()
        results['pokemon_evolution'] = import_pokemon_evolution()
        results['pokemon_form_move'] = import_pokemon_moves()
        
        # 统计结果
        print("\n" + "=" * 60)
        print("📊 导入完成统计")
        print("=" * 60)
        
        total = 0
        for name, count in results.items():
            print(f"  {name}: {count} 条")
            total += count
        
        print("=" * 60)
        print(f"  总计: {total} 条记录")
        print("✅ 所有数据导入完成!")
        
    except Exception as e:
        print(f"❌ 导入失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()