"""从 PokeAPI 缓存重建 egg_group 表（缺失导致详情页蛋组为空）"""
import sqlite3, glob, json, os, shutil

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

backup = DB + '.bak.egg-group'
if os.path.exists(backup):
    os.remove(backup)
shutil.copy2(DB, backup)
print(f'备份 -> {backup}')

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

# 清空并重建
cur.execute('DELETE FROM egg_group')

files = glob.glob(os.path.join(CACHE, 'egg-group_*.json'))
inserted = 0
for f in files:
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception as e:
        print(f'解析失败 {os.path.basename(f)}: {e}')
        continue
    gid = d.get('id')
    if gid is None:
        continue
    zh = ''
    jp = ''
    en = d.get('name', '')
    for e in d.get('names', []) or []:
        lang = e.get('language', {}).get('name', '')
        if lang == 'zh-Hans' and not zh:
            zh = e.get('name', '')
        elif lang == 'ja-Hrkt' and not jp:
            jp = e.get('name', '')
    cur.execute("INSERT OR IGNORE INTO egg_group(id, name, name_en, name_jp) VALUES(?,?,?,?)",
                (gid, zh or en, en, jp or None))
    inserted += 1

conn.commit()
print(f'重建 egg_group: {inserted} 条')
for r in cur.execute('SELECT id, name, name_en FROM egg_group ORDER BY id'):
    print(' ', r)
conn.close()
print('完成')
