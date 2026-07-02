#!/usr/bin/env python3
"""
快速 CSV 数据恢复 — 从 PokeAPI GitHub CSV 导入
比 PokeAPI v2 JSON API 快得多（批量下载，无需逐条请求）
"""
import sqlite3, csv, io, os, sys, time, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = os.getenv('SQLITE_DB_PATH') or str(ROOT / 'backend' / 'pokemon-factory.db')
CSV_BASE = "https://raw.githubusercontent.com/PokeAPI/pokeapi/master/data/v2/csv"
CACHE_DIR = ROOT / 'data' / 'csv-cache'
CACHE_DIR.mkdir(parents=True, exist_ok=True)

def log(msg): print(f"[csv] {msg}")

def download(name):
    cache = CACHE_DIR / f"{name}.csv"
    if cache.exists():
        return cache.read_text(encoding='utf-8')
    url = f"{CSV_BASE}/{name}.csv"
    for i in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory/1.0"})
            with urllib.request.urlopen(req, timeout=60) as r:
                text = r.read().decode('utf-8')
            cache.write_text(text, encoding='utf-8')
            return text
        except Exception as e:
            log(f"  retry {i+1} {name}: {e}")
            time.sleep(2)
    return None

def parse_csv(text):
    reader = csv.reader(io.StringIO(text))
    return list(reader)

def insert(cur, table, rows, batch=2000):
    if not rows: return 0
    conn = cur.connection
    headers = rows[0]
    data_rows = rows[1:]
    cols = ','.join([f'"{h}"' for h in headers])
    placeholders = ','.join(['?'] * len(headers))
    sql = f"INSERT OR IGNORE INTO [{table}]({cols}) VALUES({placeholders})"
    total = 0
    for i in range(0, len(data_rows), batch):
        batch_data = data_rows[i:i+batch]
        conn.executemany(sql, batch_data)
        total += len(batch_data)
        conn.commit()
    return total

def main():
    conn = sqlite3.connect(DB)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=OFF")
    cur = conn.cursor()

    tables_to_restore = [
        'type_efficacy',
        'ability_names', 'ability_prose',
        'item_names', 'item_prose', 'item_categories', 'item_category_prose',
        'item_pockets', 'item_pocket_names', 'item_flags', 'item_flag_map',
        'item_flag_prose', 'item_fling_effects', 'item_fling_effect_prose',
        'move_names', 'move_meta', 'move_meta_stat_changes',
        'move_meta_ailments', 'move_meta_ailment_names',
        'move_meta_categories', 'move_meta_category_prose',
        'move_flag_map', 'move_flags', 'move_flag_prose',
        'pokemon_species', 'pokemon_species_names', 'pokemon_species_prose',
        'pokemon_forms', 'pokemon_form_names',
        'pokemon_stats', 'pokemon_types', 'pokemon_abilities',
        'pokemon_moves', 'pokemon_moves_new',
        'pokemon_egg_groups',
        'pokemon_evolution', 'evolution_chains',
        'pokemon_colors', 'pokemon_color_names',
        'pokemon_habitats', 'pokemon_habitat_names',
        'pokemon_shapes', 'pokemon_shape_prose',
        'pokemon_form_generations',
    ]

    # 先导入主表（不从 CSV 覆盖，保证主键完整）
    main_tables = [
        ('types', ['id','name','name_en','color']),
        ('abilities', ['id','name','name_en','generation_id','is_main_series']),
        ('items', ['id','name','name_en','cost','fling_power','generation_id']),
        ('moves', ['id','name','name_en','type_id','damage_class_id','target_id','power','pp','accuracy','priority','effect_chance','generation_id']),
        ('pokemon', ['id','species_id','height','weight','base_experience','order','is_default']),
    ]

    for table, forced_cols in main_tables:
        try:
            cnt = cur.execute(f"SELECT COUNT(*) FROM [{table}]").fetchone()[0]
            if cnt > 0:
                log(f"{table}: 已有 {cnt} 条，跳过主表导入")
                continue
        except:
            pass

        # 从 CSV 逐行导入，字段映射
        if table == 'types':
            headers, rows = parse_csv(download('types')), []
            _ = headers[0] if isinstance(headers[0], list) else headers
        text = download(table.rstrip('s'))  # types->type, items->item, etc.
        if not text: continue
        csv_rows = parse_csv(text)
        csv_headers = csv_rows[0]
        log(f"{table}: 下载 {len(csv_rows)-1} 行")
        count = 0
        for row in csv_rows[1:]:
            try:
                row_dict = {csv_headers[j]: row[j] for j in range(len(row))}
                vals = tuple(row_dict.get(c) for c in forced_cols)
                cols = ','.join(f'"{c}"' for c in forced_cols)
                ph = ','.join(['?'] * len(forced_cols))
                cur.execute(f"INSERT OR IGNORE INTO [{table}]({cols}) VALUES({ph})", vals)
                count += 1
            except Exception as e:
                pass
        conn.commit()
        log(f"  -> {table}: 导入 {count} 条")

    # 其他 CSV 表直接导入（不强制字段映射）
    for table in tables_to_restore:
        try:
            cur.execute(f"SELECT COUNT(*) FROM [{table}]")
            existing = cur.fetchone()[0]
            if existing > 0:
                log(f"{table}: 已有 {existing} 条，跳过")
                continue
        except:
            pass

        text = download(table)
        if not text: continue
        rows = parse_csv(text)
        if len(rows) < 2: continue
        count = insert(cur, table, rows)
        log(f"{table}: 导入 {count} 条")

    # 验证
    log("--- 验证 ---")
    for t in ['pokemon_species','pokemon_forms','pokemon','moves','items','abilities',
              'pokemon_stats','pokemon_types','pokemon_abilities','pokemon_moves',
              'pokemon_egg_groups','pokemon_evolution']:
        try:
            cnt = cur.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
            log(f"  {t}: {cnt}")
        except:
            log(f"  {t}: ERROR")

    conn.close()
    log("完成！")

if __name__ == '__main__':
    main()
