#!/usr/bin/env python3
"""验证SQLite数据完整性——非致命模式（引擎不依赖数据库运行）"""

import sqlite3, os, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = os.getenv("SQLITE_DB_PATH") or str(ROOT / "backend" / "pokemon-factory.db")

def main():
    conn = sqlite3.connect(DB)
    c = conn.cursor()
    
    print("=" * 60)
    print("SQLite 数据完整性验证")
    print(f"DB: {DB}")
    print("=" * 60)

    c.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = {r[0] for r in c.fetchall()}
    print(f"\n总表数: {len(tables)}")

    key_tables = [
        ("type", 18), ("type_efficacy", 200), ("ability", 1), ("ability_effect", 1),
        ("item", 1), ("item_effect", 1), ("move", 1), ("move_damage_class", 3),
        ("move_target", 1), ("generation", 9), ("stat", 8), ("evolution_trigger", 13),
        ("pokemon_species", 1), ("pokemon_form", 1), ("move_meta", 1),
    ]

    print(f"\n{'表名':25s} {'期望':>6} {'实际':>6} {'状态':>8}")
    print("-" * 55)
    all_ok = True
    for name, expected in key_tables:
        if name in tables:
            cnt = c.execute(f"SELECT COUNT(*) FROM [{name}]").fetchone()[0]
            ok = cnt >= expected
        else:
            cnt = 0
            ok = False
        status = "OK" if ok else ("WARN" if cnt > 0 else "MISS")
        if not ok and cnt > 0:
            status = "LOW"
        print(f"  {name:25s} {expected:>6} {cnt:>6} {status:>8}")
        if not ok:
            all_ok = False

    print(f"\n{'综合':>25s}: {'PASS' if all_ok else 'PARTIAL (引擎不依赖数据库)'}")

    print("\n数据示例:")
    for q, label in [
        ("SELECT id, name, name_en FROM type WHERE id=1", "一般"),
        ("SELECT id, name, name_en FROM ability WHERE name_en='intimidate'", "威吓"),
        ("SELECT id, name, name_en FROM item WHERE name_en='life-orb'", "生命宝珠"),
        ("SELECT id, name, name_en FROM move WHERE name_en='thunderbolt'", "十万伏特"),
        ("SELECT COUNT(*) FROM pokemon_species", "宝可梦总数"),
        ("SELECT COUNT(*) FROM ability_effect", "特性效果"),
        ("SELECT COUNT(*) FROM item_effect", "道具效果"),
    ]:
        row = c.execute(q).fetchone()
        print(f"  {label}: {row[0] if row else 'N/A'}")

    conn.close()
    print("\n" + "=" * 60)

if __name__ == "__main__":
    main()
