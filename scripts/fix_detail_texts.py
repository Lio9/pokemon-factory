"""
从 PokeAPI 缓存修复 move/item 表的中文名与效果描述。

1. move 表: 回填 effect_short（优先 zh-Hans，否则 en）与 description（zh-Hans flavor）。
2. item 表: 回填 name（zh-Hans 中文名，修正当前错位）与 effect_short/description。
3. ability 表: 回填 description（zh-Hans）与 description_en。
"""
import sqlite3, glob, json, os

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()


def pick_zh(names, key='name'):
    for e in names or []:
        if e.get('language', {}).get('name', '').lower() == 'zh-hans':
            return e.get(key, '')
    return ''


def pick_zh_effect(effect_entries):
    for e in effect_entries or []:
        if e.get('language', {}).get('name', '').lower() == 'zh-hans':
            return e.get('short_effect') or e.get('effect') or ''
    for e in effect_entries or []:
        if e.get('language', {}).get('name') == 'en':
            return e.get('short_effect') or e.get('effect') or ''
    return ''


def pick_en_effect(effect_entries):
    for e in effect_entries or []:
        if e.get('language', {}).get('name') == 'en':
            return e.get('short_effect') or e.get('effect') or ''
    return ''


def pick_zh_flavor(flavor_entries):
    for e in flavor_entries or []:
        if e.get('language', {}).get('name', '').lower() == 'zh-hans':
            return (e.get('flavor_text') or '').replace('\n', ' ')
    return ''


# ===== 1. move: effect_short / description =====
move_updated = 0
for f in glob.glob(os.path.join(CACHE, 'move_*.json')):
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    mid = d.get('id')
    if mid is None:
        continue
    effect = pick_zh_effect(d.get('effect_entries') or []) or pick_en_effect(d.get('effect_entries') or [])
    desc = pick_zh_flavor(d.get('flavor_text_entries') or [])
    if not effect and not desc:
        continue
    cur.execute('UPDATE move SET effect_short=?, description=? WHERE id=?', (effect, desc, mid))
    if cur.rowcount > 0:
        move_updated += 1
conn.commit()
print(f'move 效果回填: {move_updated}')

# ===== 2. item: name(zh) / effect_short / description =====
# 注意: item 表 id 与 PokeAPI id 不一致（DB 自增顺序），按 name_en 匹配更新。
item_name_fixed = 0
item_effect_fixed = 0
for f in glob.glob(os.path.join(CACHE, 'item_*.json')):
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    name_en = d.get('name')
    if not name_en:
        continue
    zh_name = pick_zh(d.get('names') or [])
    effect = pick_zh_effect(d.get('effect_entries') or []) or pick_en_effect(d.get('effect_entries') or [])
    desc = pick_zh_flavor(d.get('flavor_text_entries') or [])
    if not zh_name and not effect and not desc:
        continue
    if zh_name:
        cur.execute('UPDATE item SET name=? WHERE name_en=?', (zh_name, name_en))
        if cur.rowcount > 0:
            item_name_fixed += 1
    if effect or desc:
        cur.execute('UPDATE item SET effect_short=?, description=? WHERE name_en=?', (effect, desc or effect, name_en))
        if cur.rowcount > 0:
            item_effect_fixed += 1
conn.commit()
print(f'item 中文名修正: {item_name_fixed}, 效果回填: {item_effect_fixed}')

# ===== 3. ability: description(zh) / description_en =====
ability_fixed = 0
for f in glob.glob(os.path.join(CACHE, 'ability_*.json')):
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception:
        continue
    aid = d.get('id')
    if aid is None:
        continue
    desc = pick_zh_effect(d.get('effect_entries') or [])
    desc_en = pick_en_effect(d.get('effect_entries') or [])
    if not desc and not desc_en:
        continue
    cur.execute('UPDATE ability SET description=?, description_en=? WHERE id=?', (desc, desc_en, aid))
    if cur.rowcount > 0:
        ability_fixed += 1
conn.commit()
print(f'ability 描述回填: {ability_fixed}')

# ===== 验证 =====
print('\n=== 验证 ===')
for sql in [
    "SELECT m.name, substr(m.effect_short,1,50) FROM move m WHERE m.name_en='bite'",
    "SELECT i.name, i.name_en, substr(i.effect_short,1,50) FROM item i WHERE i.name_en='assault-vest'",
    "SELECT a.name, a.name_en, substr(a.description,1,40) FROM ability a WHERE a.name_en='static'"
]:
    r = cur.execute(sql).fetchone()
    print(' ', r)
conn.close()
print('完成')
