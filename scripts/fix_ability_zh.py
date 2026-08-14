"""
修复 ability 描述: 用 PokeAPI 缓存的中文 flavor_text 回填 description。
原因: 之前 fix_detail_texts.py 误用英文 effect_entries 覆盖了 DB 原有中文描述。
策略: 仅当缓存有 zh-Hans flavor 时回填（不覆盖已有中文）。
"""
import sqlite3, glob, json, os

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()


def pick_zh_flavor(flavor_entries):
    for e in flavor_entries or []:
        if e.get('language', {}).get('name', '').lower() == 'zh-hans':
            return (e.get('flavor_text') or '').replace('\n', ' ')
    return ''


def has_chinese(text):
    if not text:
        return False
    return any('\u4e00' <= ch <= '\u9fff' for ch in text)


fixed = 0
for f in glob.glob(os.path.join(CACHE, 'ability_*.json')):
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    name_en = d.get('name')
    if not name_en:
        continue
    zh = pick_zh_flavor(d.get('flavor_text_entries') or [])
    if not zh:
        continue
    # 查询现有值，仅当现有值不含中文时才回填
    row = cur.execute('SELECT description FROM ability WHERE name_en=?', (name_en,)).fetchone()
    if row is None:
        continue
    if has_chinese(row[0]):
        continue
    cur.execute('UPDATE ability SET description=? WHERE name_en=?', (zh, name_en))
    if cur.rowcount > 0:
        fixed += 1
conn.commit()
print(f'ability 中文描述恢复: {fixed}')

# 验证
for name in ['static', 'hunger-switch', 'lightning-rod', 'intimidate', 'overgrow']:
    r = cur.execute("SELECT name, name_en, substr(description,1,40) FROM ability WHERE name_en=?", (name,)).fetchone()
    print(' ', r)
conn.close()
print('完成')
