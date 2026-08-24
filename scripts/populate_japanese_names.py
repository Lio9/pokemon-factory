#!/usr/bin/env python3
"""
从 PokeAPI 填充日文名到 SQLite 数据库。
覆盖 pokemon_species.name_jp、move.name_jp、ability.name_jp。

用法：python scripts/populate_japanese_names.py
"""
import json, sqlite3, urllib.request, time, os, sys

DB = os.path.join(os.path.dirname(__file__), '..', 'backend', 'pokemon-factory.db')
CACHE_DIR = os.path.join(os.path.dirname(__file__), '..', 'data', 'pokeapi-cache')

def fetch_json(url, retries=3):
    for i in range(retries):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'PokemonFactory/1.0'})
            with urllib.request.urlopen(req, timeout=10) as resp:
                return json.loads(resp.read())
        except Exception as e:
            if i == retries - 1:
                print(f"  FAIL: {url} -> {e}")
                return None
            time.sleep(1)

def get_ja_name(names_list):
    """从 PokeAPI names 数组中提取日文名（优先 ja，回退 ja-hrkt）"""
    ja = None
    ja_hrkt = None
    for n in (names_list or []):
        lang = n.get('language', {}).get('name', '')
        if lang == 'ja':
            ja = n.get('name', '')
        elif lang == 'ja-hrkt':
            ja_hrkt = n.get('name', '')
    return ja or ja_hrkt or ''

def populate_species(cur):
    cur.execute("SELECT id, name_jp FROM pokemon_species WHERE name_jp IS NULL OR name_jp = ''")
    rows = cur.fetchall()
    print(f"=== pokemon_species: {len(rows)} need JP names ===")
    updated = 0
    for sid, _ in rows:
        data = fetch_json(f"https://pokeapi.co/api/v2/pokemon-species/{sid}")
        if not data:
            continue
        jp = get_ja_name(data.get('names'))
        if jp:
            cur.execute("UPDATE pokemon_species SET name_jp = ? WHERE id = ?", (jp, sid))
            updated += 1
        if updated % 50 == 0:
            print(f"  ... {updated}/{len(rows)}")
        time.sleep(0.1)  # rate limit
    print(f"  Done: {updated}/{len(rows)} species updated")
    return updated

def populate_moves(cur):
    cur.execute("SELECT id, name_jp FROM move WHERE name_jp IS NULL OR name_jp = ''")
    rows = cur.fetchall()
    print(f"=== move: {len(rows)} need JP names ===")
    updated = 0
    for mid, _ in rows:
        data = fetch_json(f"https://pokeapi.co/api/v2/move/{mid}")
        if not data:
            continue
        jp = get_ja_name(data.get('names'))
        if jp:
            cur.execute("UPDATE move SET name_jp = ? WHERE id = ?", (jp, mid))
            updated += 1
        if updated % 50 == 0:
            print(f"  ... {updated}/{len(rows)}")
        time.sleep(0.1)
    print(f"  Done: {updated}/{len(rows)} moves updated")
    return updated

def populate_abilities(cur):
    cur.execute("SELECT id, name_jp FROM ability WHERE name_jp IS NULL OR name_jp = ''")
    rows = cur.fetchall()
    print(f"=== ability: {len(rows)} need JP names ===")
    updated = 0
    for aid, _ in rows:
        data = fetch_json(f"https://pokeapi.co/api/v2/ability/{aid}")
        if not data:
            continue
        jp = get_ja_name(data.get('names'))
        if jp:
            cur.execute("UPDATE ability SET name_jp = ? WHERE id = ?", (jp, aid))
            updated += 1
        if updated % 50 == 0:
            print(f"  ... {updated}/{len(rows)}")
        time.sleep(0.1)
    print(f"  Done: {updated}/{len(rows)} abilities updated")
    return updated

def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    
    s = populate_species(cur)
    m = populate_moves(cur)
    a = populate_abilities(cur)
    
    conn.commit()
    conn.close()
    print(f"\nTotal: {s} species + {m} moves + {a} abilities updated with JP names")

if __name__ == '__main__':
    main()
