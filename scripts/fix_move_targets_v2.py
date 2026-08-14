"""
从 PokeAPI 缓存重建 move 表的 target_id（引擎 Showdown 风格 id）。
先撤销 fix_move_targets.py 的错误转换（它把 all-pokemon 误当全场），
再按缓存中的真实 target 名称正确映射。

PokeAPI target 名 -> 引擎 id:
  selected-pokemon / single-opponent / specific-move / fainting-pokemon -> 10 (单体)
  all-opponents -> 11 (对手全体)
  all-other-pokemon -> 9 (除自身外全部)
  user / user-and-allies? -> 4 (自身)
  random-opponent -> 8 (随机对手)
  ally -> 3 (队友)
  all-allies -> 13 (我方全体)
  users-field -> 13 (我方场地)
  opponents-field -> 11 (对手场地)
  all-pokemon / entire-field -> 14 (全场)
"""
import sqlite3, glob, json, os

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

TARGET_MAP = {
    'selected-pokemon': 10,
    'single-opponent': 10,
    'specific-move': 10,
    'fainting-pokemon': 10,
    'all-opponents': 11,
    'all-other-pokemon': 9,
    'user': 4,
    'random-opponent': 8,
    'ally': 3,
    'all-allies': 13,
    'users-field': 13,
    'opponents-field': 11,
    'all-pokemon': 14,
    'entire-field': 14,
    # 特殊目标类型
    'user-and-allies': 13,       # 自身与队友（如双打顺风类）
    'user-or-ally': 3,           # 自身或队友
    'selected-pokemon-me-first': 10,  # 模仿类（先制之爪）
}

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

# 1. 重置所有 target_id 为 0（待重建）
cur.execute("UPDATE move SET target_id=0")
conn.commit()

# 2. 从缓存重建
updated = 0
missing = 0
unknown_targets = set()
for f in glob.glob(os.path.join(CACHE, 'move_*.json')):
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    mid = d.get('id')
    if mid is None:
        continue
    t = d.get('target') or {}
    tname = t.get('name', '')
    new_id = TARGET_MAP.get(tname)
    if new_id is None:
        unknown_targets.add(tname)
        missing += 1
        continue
    cur.execute("UPDATE move SET target_id=? WHERE id=?", (new_id, mid))
    if cur.rowcount > 0:
        updated += 1
conn.commit()

print(f'重建 target_id: {updated} 条（无缓存或未知目标 {missing} 条）')
if unknown_targets:
    print('未知目标类型:', sorted(unknown_targets))

# 3. 剩余 target_id=0 的按名称兜底
zero = cur.execute("SELECT COUNT(*) FROM move WHERE target_id=0").fetchone()[0]
print(f'剩余 target_id=0: {zero} 条')

print()
print('=== 验证 ===')
for name in ['earthquake', 'heat-wave', 'surf', 'blizzard', 'dazzling-gleam', 'rock-slide', 'growl', 'swords-dance', 'protect', 'reflect', 'stealth-rock', 'toxic-spikes', 'tailwind', 'thunder-wave', 'bite', 'counter', 'helping-hand']:
    r = cur.execute("SELECT name_en, target_id, type_id, power FROM move WHERE name_en=?", (name,)).fetchone()
    if r:
        print(f'  {r[0]:16s} target_id={r[1]} type={r[2]} power={r[3]}')
    else:
        print(f'  {name}: NOT FOUND')
conn.close()
print('完成')
