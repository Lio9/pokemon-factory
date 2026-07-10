#!/usr/bin/env python3
"""
快速数据恢复脚本 — 从 PokeAPI GitHub CSV 源导入英文数据 + PokeAPI v2 补充中文名

数据源：
  CSV: https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv/
  PokeAPI v2: https://pokeapi.co/api/v2/ （仅用于中文名）

用法：
  python scripts/restore_data.py              # 恢复 + 中文名
  python scripts/restore_data.py --no-zh      # 仅恢复英文数据
"""

import sqlite3, csv, io, os, sys, time, json, urllib.request, urllib.error, hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB_PATH = os.getenv('SQLITE_DB_PATH') or str(ROOT / 'backend' / 'pokemon-factory.db')
CSV_BASE = "https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv"
CACHE_DIR = ROOT / 'data' / 'pokeapi-cache'
POKEAPI = "https://pokeapi.co/api/v2"
DELAY = 0.1

CACHE_DIR.mkdir(parents=True, exist_ok=True)

def log(msg): print(f"[restore] {msg}")

CSV_FILES = [
    'abilities', 'ability_changelog', 'ability_changelog_prose', 'ability_flavor_text',
    'ability_names', 'ability_prose',
    'berries', 'berry_firmness', 'berry_firmness_names',
    'characteristics',
    'contests', 'contest_effects',
    'egg_groups', 'egg_group_prose',
    'encounters', 'encounter_condition_value_map',
    'evolution_chains', 'evolution_triggers', 'evolution_trigger_prose',
    'genders', 'gender_prose',
    'generations', 'generation_names',
    'growth_rates', 'growth_rate_prose',
    'item_categories', 'item_category_prose',
    'item_flags', 'item_flag_map', 'item_flag_prose',
    'item_fling_effects', 'item_fling_effect_prose',
    'item_pockets', 'item_pocket_names',
    'items', 'item_names', 'item_prose',
    'languages',
    'location_areas', 'locations',
    'moves', 'move_changelog',
    'move_damage_classes', 'move_damage_class_prose',
    'move_effects', 'move_flag_map', 'move_flags', 'move_flag_prose',
    'move_learn_methods', 'move_learn_method_prose',
    'move_meta', 'move_meta_ailments', 'move_meta_ailment_names',
    'move_meta_categories', 'move_meta_category_prose',
    'move_meta_stat_changes',
    'move_names', 'move_targets', 'move_target_prose',
    'natures', 'nature_names',
    'pal_park',
    'pokeathlon_stats',
    'pokedexes',
    'pokemon', 'pokemon_abilities', 'pokemon_color_names',
    'pokemon_colors', 'pokemon_egg_groups',
    'pokemon_evolution', 'pokemon_form_generations',
    'pokemon_form_names', 'pokemon_form_pokeathlon_stats',
    'pokemon_forms', 'pokemon_game_indices',
    'pokemon_habitat_names', 'pokemon_habitats',
    'pokemon_items', 'pokemon_move_methods',
    'pokemon_moves', 'pokemon_moves_new',
    'pokemon_shape_prose', 'pokemon_shape_pokemon', 'pokemon_shapes',
    'pokemon_species', 'pokemon_species_names', 'pokemon_species_prose',
    'pokemon_stats', 'pokemon_types',
    'regions', 'region_names',
    'stats', 'stat_names',
    'super_contest_effects',
    'type_efficacy', 'type_names', 'types',
    'version_groups', 'version_group_regions',
    'versions', 'version_names',
]

def csv_download(table):
    """下载 CSV 文件并返回 (列名, 数据行)"""
    url = f"{CSV_BASE}/{table}.csv"
    cache = CACHE_DIR / f"csv_{table}.csv"
    if cache.exists():
        with open(cache, encoding='utf-8') as f:
            content = f.read()
    else:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory/1.0"})
            with urllib.request.urlopen(req, timeout=30) as r:
                content = r.read().decode('utf-8')
            with open(cache, 'w', encoding='utf-8') as f:
                f.write(content)
        except Exception as e:
            log(f"  CSV 下载失败 {table}: {e}")
            return None, []
    reader = csv.reader(io.StringIO(content))
    headers = next(reader, [])
    rows = list(reader)
    return headers, rows

def col_index(headers, name):
    try: return headers.index(name)
    except ValueError: return -1

def import_csv(cur, table, col_map=None):
    """通用 CSV 导入"""
    headers, rows = csv_download(table)
    if not headers or not rows:
        return 0
    # 检查表是否存在
    try:
        cur.execute(f"SELECT COUNT(*) FROM [{table}]")
        existing = cur.fetchone()[0]
        if existing > 0:
            log(f"  {table}: 已有 {existing} 条，跳过")
            return 0
    except Exception:
        pass
    count = 0
    for row in rows:
        try:
            if col_map:
                mapped = {k: row[headers.index(v)] if v in headers else None for k, v in col_map.items()}
                cols = ', '.join(mapped.keys())
                vals = ', '.join(['?'] * len(mapped))
                cur.execute(f"INSERT INTO [{table}]({cols}) VALUES({vals})", tuple(mapped.values()))
            else:
                cols = ', '.join(headers)
                vals = ', '.join(['?'] * len(headers))
                cur.execute(f"INSERT INTO [{table}]({cols}) VALUES({vals})", tuple(row))
            count += 1
        except Exception as e:
            pass  # 跳过有问题的行
    conn = cur.connection
    conn.commit()
    log(f"  {table}: 导入 {count} 条")
    return count

def main():
    log(f"数据库: {DB_PATH}")
    log("正在从 PokeAPI GitHub CSV 恢复数据...")

    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=OFF")
    cur = conn.cursor()

    # 1. 导入核心表
    core_tables = [
        'types', 'type_efficacy', 'generations', 'stats', 'move_damage_classes',
        'move_targets', 'move_learn_methods', 'evolution_triggers', 'growth_rates',
        'egg_groups', 'genders', 'natures', 'version_groups',
        'abilities', 'ability_names', 'ability_prose',
        'items', 'item_names', 'item_prose',
        'item_categories', 'item_category_prose',
        'item_pockets', 'item_pocket_names',
        'item_flags', 'item_flag_map', 'item_flag_prose',
        'item_fling_effects', 'item_fling_effect_prose',
        'moves', 'move_names', 'move_meta', 'move_meta_stat_changes',
        'move_meta_ailments', 'move_meta_categories',
        'move_flag_map', 'move_flags', 'move_flag_prose',
        'pokemon_species', 'pokemon_species_names', 'pokemon_species_prose',
        'pokemon_forms', 'pokemon_form_names',
        'pokemon', 'pokemon_stats', 'pokemon_types', 'pokemon_abilities',
        'pokemon_moves', 'pokemon_moves_new',
        'pokemon_egg_groups',
        'pokemon_evolution', 'evolution_chains',
        'pokemon_colors', 'pokemon_color_names',
        'pokemon_habitats', 'pokemon_habitat_names',
        'pokemon_shapes', 'pokemon_shape_prose',
    ]

    for table in core_tables:
        if table in ('ability_names', 'item_names', 'move_names', 'pokemon_species_names', 'pokemon_form_names',
                      'type_names', 'generation_names', 'stat_names', 'move_damage_class_prose',
                      'move_target_prose', 'move_learn_method_prose', 'evolution_trigger_prose',
                      'growth_rate_prose', 'egg_group_prose', 'gender_prose',
                      'item_category_prose', 'item_pocket_names', 'item_flag_prose',
                      'item_fling_effect_prose', 'move_meta_ailment_names', 'move_meta_category_prose',
                      'pokemon_color_names', 'pokemon_habitat_names', 'pokemon_shape_prose',
                      'ability_prose', 'item_prose', 'pokemon_species_prose',
                      'nature_names', 'version_group_regions', 'language_names',
                      'pokemon_form_generations'):
            log(f"  跳过不需要的表: {table}")
            continue
        headers, rows = csv_download(table)
        if not rows: continue
        # 检查是否已有数据
        try:
            cur.execute(f"SELECT COUNT(*) FROM [{table}]")
            existing = cur.fetchone()[0]
        except Exception:
            existing = 0
        if existing > 0:
            log(f"  {table}: 已有 {existing} 条")
            continue
        count = 0
        for row in rows:
            try:
                placeholders = ','.join(['?'] * len(row))
                cur.execute(f"INSERT INTO [{table}] VALUES({placeholders})", row)
                count += 1
            except Exception:
                pass
        conn.commit()
        log(f"  {table}: 导入 {count} 条")

    # 2. 补充中文名（从 PokeAPI v2）
    conn.commit()
    log("\n开始补充中文名（从 PokeAPI v2）...")
    
    # 语言 ID 映射（从 CSV languages 表获取）
    lang_en = 9
    lang_zh = None
    try:
        rows = cur.execute("SELECT id, identifier FROM languages WHERE identifier IN ('zh-Hans', 'zh', 'en')").fetchall()
        for lid, ident in rows:
            if ident == 'zh-Hans': lang_zh = lid
            if ident == 'en': lang_en = lid
    except Exception:
        pass

    # 宝可梦中文名
    log("补充宝可梦中文名...")
    rows = cur.execute("SELECT id, name_en FROM pokemon_species WHERE name = name_en OR name IS NULL").fetchall()
    total = len(rows)
    for i, (sid, en_name) in enumerate(rows):
        try:
            url = f"{POKEAPI}/pokemon-species/{sid}"
            cache = CACHE_DIR / f"species_{sid}.json"
            if cache.exists():
                with open(cache) as f: data = json.load(f)
            else:
                req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory/1.0"})
                with urllib.request.urlopen(req, timeout=10) as r:
                    data = json.loads(r.read().decode())
                with open(cache, 'w') as f: json.dump(data, f)
                time.sleep(DELAY)
            zh_name = ''
            for n in data.get('names', []):
                if n.get('language', {}).get('name') == 'zh-Hans':
                    zh_name = n['name']
                    break
            if zh_name:
                cur.execute("UPDATE pokemon_species SET name = ? WHERE id = ?", (zh_name, sid))
        except Exception:
            pass
        if (i + 1) % 100 == 0:
            conn.commit()
            log(f"  宝可梦中文名: {i+1}/{total}")
    conn.commit()

    # 特性中文名
    log("补充特性中文名...")
    rows = cur.execute("SELECT a.id, an.name FROM ability a LEFT JOIN ability_names an ON an.ability_id = a.id AND an.local_language_id = ? WHERE a.name IS NULL OR a.name = a.name_en", (lang_zh,)).fetchall()
    total = len(rows)
    for i, (aid, zh_name) in enumerate(rows):
        if zh_name:
            cur.execute("UPDATE ability SET name = ? WHERE id = ?", (zh_name, aid))
        if (i + 1) % 100 == 0:
            conn.commit()
    conn.commit()
    log(f"  特性中文名: {total} 条")

    # 技能中文名
    log("补充技能中文名...")
    rows = cur.execute("SELECT m.id, mn.name FROM move m LEFT JOIN move_names mn ON mn.move_id = m.id AND mn.local_language_id = ? WHERE m.name IS NULL OR m.name = m.name_en", (lang_zh,)).fetchall()
    total = len(rows)
    for i, (mid, zh_name) in enumerate(rows):
        if zh_name:
            cur.execute("UPDATE move SET name = ? WHERE id = ?", (zh_name, mid))
        if (i + 1) % 100 == 0:
            conn.commit()
    conn.commit()
    log(f"  技能中文名: {total} 条")

    # 物品中文名
    log("补充物品中文名...")
    rows = cur.execute("SELECT i.id, in2.name FROM item i LEFT JOIN item_names in2 ON in2.item_id = i.id AND in2.local_language_id = ? WHERE i.name IS NULL OR i.name = i.name_en", (lang_zh,)).fetchall()
    total = len(rows)
    for i, (iid, zh_name) in enumerate(rows):
        if zh_name:
            cur.execute("UPDATE item SET name = ? WHERE id = ?", (zh_name, iid))
        if (i + 1) % 100 == 0:
            conn.commit()
    conn.commit()
    log(f"  物品中文名: {total} 条")

    # 类型中文名
    log("补充类型中文名...")
    rows = cur.execute("SELECT t.id, tn.name FROM types t LEFT JOIN type_names tn ON tn.type_id = t.id AND tn.local_language_id = ? WHERE t.name IS NULL", (lang_zh,)).fetchall()
    for tid, zh_name in rows:
        if zh_name:
            cur.execute("UPDATE type SET name = ? WHERE id = ?", (zh_name, tid))
    conn.commit()

    log("完成！")
    cur.execute("SELECT COUNT(*) FROM pokemon_species")
    log(f"pokemon_species: {cur.fetchone()[0]}")
    cur.execute("SELECT COUNT(*) FROM ability")
    log(f"ability: {cur.fetchone()[0]}")
    cur.execute("SELECT COUNT(*) FROM move")
    log(f"move: {cur.fetchone()[0]}")
    cur.execute("SELECT COUNT(*) FROM item")
    log(f"item: {cur.fetchone()[0]}")

    conn.close()

if __name__ == '__main__':
    main()
