#!/usr/bin/env python3
"""
数据维护工具 — 从 PokeAPI 补全中文描述、效果、分类等缺失数据。

用法：
  python scripts/data_maintenance.py                      # 修复所有缺失数据
  python scripts/data_maintenance.py --fix items          # 仅修复道具
  python scripts/data_maintenance.py --fix moves          # 仅修复技能
  python scripts/data_maintenance.py --verify             # 只检查不修改

环境变量: SQLITE_DB_PATH 可覆盖数据库路径
"""

import sqlite3, json, os, sys, time, urllib.request, urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = os.getenv('SQLITE_DB_PATH') or str(ROOT / 'backend' / 'pokemon-factory.db')
POKEAPI = "https://pokeapi.co/api/v2"
DELAY = 0.15

# ============================================================
# 映射表
# ============================================================
TYPE_NAME_TO_LOCAL = {
    "normal": 1, "fighting": 2, "flying": 3, "poison": 4,
    "ground": 5, "rock": 6, "bug": 7, "ghost": 8,
    "steel": 9, "fire": 10, "water": 11, "grass": 12,
    "electric": 13, "psychic": 14, "ice": 15, "dragon": 16,
    "dark": 17, "fairy": 18
}
DC_MAP = {"physical": 1, "special": 2, "status": 3}

def fetch(url, retries=3):
    for i in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory/1.0"})
            with urllib.request.urlopen(req, timeout=15) as resp:
                return json.loads(resp.read())
        except Exception as e:
            if i < retries - 1:
                time.sleep(1)
                continue
            return None

def get_zh_flavor(data):
    """从 flavor_text_entries 取最新世代的中文描述"""
    if not data or "flavor_text_entries" not in data:
        return None
    for vg in ("scarlet-violet", "sword-shield", "ultra-sun-ultra-moon", "sun-moon", "x-y"):
        for fte in data["flavor_text_entries"]:
            if fte.get("language", {}).get("name") == "zh-hans" and fte.get("version_group", {}).get("name") == vg:
                text = fte.get("flavor_text") or fte.get("text") or ""
                if text.strip():
                    return text.replace("\n", " ").replace("\r", "").replace("\f", " ").strip()
    return None

def get_en_effect(data):
    """从 effect_entries 取英文描述"""
    if not data or "effect_entries" not in data:
        return None
    for ee in data["effect_entries"]:
        if ee.get("language", {}).get("name") == "en":
            return ee.get("short_effect", ee.get("effect", "")).replace("\n", " ").replace("\r", "").strip()
    return None

def get_item_category(data):
    """从道具 category 取中文分类名"""
    if data and data.get("category"):
        cat_data = fetch(data["category"]["url"])
        if cat_data:
            for n in cat_data.get("names", []):
                if n.get("language", {}).get("name") == "zh-hans":
                    return n["name"]
            for n in cat_data.get("names", []):
                if n.get("language", {}).get("name") == "en":
                    return n["name"]
    return None

def progress(current, total, label=""):
    pct = int(current / total * 100) if total else 0
    bar = "█" * (pct // 5) + "░" * (20 - pct // 5)
    print(f"\r  {label} [{bar}] {current}/{total} ({pct}%)", end="", flush=True)
    if current == total:
        print()

def fix_move_types_and_damage(cur):
    """修复 move 表中错误的 type_id 和 damage_class_id"""
    cur.execute("SELECT id, name, type_id FROM move WHERE type_id NOT BETWEEN 1 AND 18 ORDER BY id")
    bad_types = cur.fetchall()
    if bad_types:
        print(f"\n修复 type_id: {len(bad_types)} 个")
        for idx, (mid, name, bad_tid) in enumerate(bad_types):
            data = fetch(f"{POKEAPI}/move/{mid}")
            if data and data.get("type"):
                correct = TYPE_NAME_TO_LOCAL.get(data["type"]["name"])
                if correct:
                    cur.execute("UPDATE move SET type_id = ? WHERE id = ?", (correct, mid))
                    cur.connection.commit()
            progress(idx + 1, len(bad_types), "type_id")
    else:
        print("\ntype_id 全部正确")

    cur.execute("SELECT id, name, damage_class_id FROM move WHERE damage_class_id NOT IN (1,2,3) AND damage_class_id IS NOT NULL ORDER BY id")
    bad_dcs = cur.fetchall()
    if bad_dcs:
        print(f"修复 damage_class_id: 共 {len(bad_dcs)} 个")
        for dcid in set(r[2] for r in bad_dcs):
            mid = [r[0] for r in bad_dcs if r[2] == dcid][0]
            data = fetch(f"{POKEAPI}/move/{mid}")
            if data and data.get("damage_class"):
                correct = DC_MAP.get(data["damage_class"]["name"], 3)
                cur.execute("UPDATE move SET damage_class_id = ? WHERE damage_class_id = ?", (correct, dcid))
                cur.connection.commit()
                print(f"  ID {dcid} -> {correct} ({data['damage_class']['name']})")
                time.sleep(DELAY)
    else:
        print("damage_class_id 全部正确")

def fix_move_descriptions(cur):
    """补全 move 表缺失的描述"""
    cur.execute("SELECT id, name FROM move WHERE description IS NULL OR description = '' ORDER BY id")
    moves = cur.fetchall()
    if not moves:
        print("技能描述已全部完整")
        return
    print(f"\n补全技能描述: {len(moves)} 个")
    updated = 0
    for idx, (mid, name) in enumerate(moves):
        if mid > 1000:
            continue  # Shadow moves 没有 PokeAPI 数据
        data = fetch(f"{POKEAPI}/move/{mid}")
        desc = get_zh_flavor(data) or get_en_effect(data)
        if desc:
            cur.execute("UPDATE move SET description = ? WHERE id = ?", (desc, mid))
            cur.connection.commit()
            updated += 1
        progress(idx + 1, len(moves), "moves")
        time.sleep(DELAY)
    print(f"  新增 {updated} 个描述")

def fix_item_data(cur):
    """补全 item 表缺失的描述、效果、分类"""
    cur.execute("SELECT id, name, name_en FROM item WHERE description IS NULL OR description = '' ORDER BY id")
    items = cur.fetchall()
    if not items:
        print("道具数据已全部完整")
        return
    print(f"\n补全道具数据: {len(items)} 个")
    updated = 0
    for idx, (iid, name, name_en) in enumerate(items):
        if iid > 2000:
            continue

        data = fetch(f"{POKEAPI}/item/{iid}")
        if not data:
            progress(idx + 1, len(items), "items")
            continue

        desc = get_zh_flavor(data) or get_en_effect(data)
        effect = get_en_effect(data)
        cat_name = get_item_category(data)

        cur.execute("UPDATE item SET description = ?, effect_short = ? WHERE id = ?", (desc, effect, iid))

        if cat_name:
            cur.execute("SELECT id FROM item_category WHERE name = ? OR name_en = ?", (cat_name, cat_name))
            cat_row = cur.fetchone()
            if cat_row:
                cur.execute("UPDATE item SET category_id = ? WHERE id = ?", (cat_row[0], iid))
            else:
                cur.execute("INSERT INTO item_category(name, name_en) VALUES(?, ?)", (cat_name, cat_name.lower().replace(" ", "-")))
                cur.connection.commit()
                cur.execute("SELECT id FROM item_category WHERE name = ?", (cat_name,))
                new_cat = cur.fetchone()
                if new_cat:
                    cur.execute("UPDATE item SET category_id = ? WHERE id = ?", (new_cat[0], iid))

        cur.connection.commit()
        updated += 1
        progress(idx + 1, len(items), "items")
        time.sleep(DELAY)
    print(f"  更新 {updated} 个道具")

def verify(cur):
    """检查数据完整性"""
    checks = {
        "type 表": ("type", 18),
        "move 表": ("move", 874),
        "item 表": ("item", 2135),
        "ability 表": ("ability", 358),
        "未修复 type_id": ("move", 874, "type_id NOT BETWEEN 1 AND 18", 0),
        "未修复 damage_class_id": ("move", 874, "damage_class_id NOT IN (1,2,3)", 0),
        "move 缺描述": ("move", 874, "description IS NULL OR description = ''", 0),
        "item 缺描述": ("item", 2135, "description IS NULL OR description = ''", 0),
    }
    print("\n=== 数据完整性检查 ===")
    for label, (table, *rest) in checks.items():
        if len(rest) == 1:
            cur.execute(f"SELECT COUNT(*) FROM {table}")
            count = cur.fetchone()[0]
            expected = rest[0]
            status = "✓" if count == expected else "✗"
            print(f"  {status} {label}: {count}/{expected}")
        else:
            expected, condition, want = rest
            cur.execute(f"SELECT COUNT(*) FROM {table} WHERE {condition}")
            count = cur.fetchone()[0]
            status = "✓" if count == want else "✗"
            print(f"  {status} {label}: {count} (期望 {want})")

if __name__ == "__main__":
    mode = "all"
    if "--fix" in sys.argv:
        idx = sys.argv.index("--fix")
        if idx + 1 < len(sys.argv):
            mode = sys.argv[idx + 1]

    conn = sqlite3.connect(DB)
    cur = conn.cursor()

    if "--verify" in sys.argv:
        verify(cur)
        conn.close()
        sys.exit(0)

    print(f"数据库: {DB}")
    print(f"模式: {mode}")

    if mode in ("all", "moves"):
        fix_move_types_and_damage(cur)
        fix_move_descriptions(cur)

    if mode in ("all", "items"):
        fix_item_data(cur)

    verify(cur)
    conn.close()
    print("\n完成！")
