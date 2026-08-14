"""
从 PokeAPI 缓存回填 move 表的中文名（只更新 name 列，不动其他数据）。
原因：早期重建 move 表时缓存缺少 zh-Hans 名称，导致 name 列全部为英文。
只更新 name 列：有 zh-Hans 用中文，否则保留现有值。
"""
import sqlite3, glob, json, os

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

files = glob.glob(os.path.join(CACHE, 'move_*.json'))
updated = 0
missing = 0
for f in files:
    base = os.path.basename(f)
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    mid = d.get('id')
    if mid is None:
        continue
    zh = ''
    for e in d.get('names', []) or []:
        if e.get('language', {}).get('name', '').lower() == 'zh-hans':
            zh = e.get('name', '')
            break
    if not zh:
        missing += 1
        continue
    cur.execute('UPDATE move SET name=? WHERE id=?', (zh, mid))
    if cur.rowcount > 0:
        updated += 1

conn.commit()

# 验证
print(f'已更新中文名: {updated} 条（缓存无中文: {missing} 条）')
for name in ['tackle', 'ember', 'thunder-punch', 'mega-kick', 'thunderbolt', 'swords-dance', 'trick-room', 'surf']:
    r = cur.execute('SELECT id, name, name_en FROM move WHERE name_en=?', (name,)).fetchone()
    if r:
        print(f'  {name:14s} id={r[0]:5d} name={r[1]}')
    else:
        print(f'  {name:14s} 未找到')
conn.close()
print('完成')
