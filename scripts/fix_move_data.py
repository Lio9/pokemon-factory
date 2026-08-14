import sqlite3, glob, json, os, shutil, sys, time

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

# 备份
backup = DB + '.bak.movefix'
if not os.path.exists(backup):
    shutil.copy2(DB, backup)
    print(f'备份数据库 -> {backup}')

TYPE_MAP = {'normal':1,'fighting':2,'flying':3,'poison':4,'ground':5,'rock':6,'bug':7,'ghost':8,
            'steel':9,'fire':10,'water':11,'grass':12,'electric':13,'psychic':14,'ice':15,'dragon':16,'dark':17,'fairy':18}
DC_MAP = {'physical':1,'special':2,'status':3}
TGT_MAP = {'adjacent-allies':3,'adjacent-foe':1,'all-adjacent':4,'all-adjacent-foes':2,'all-other-pokemon':10,
           'ally':3,'any':1,'entire-field':10,'random-opponent':1,'self':3,'selected-pokemon':1,'user':3,
           'user-or-ally':1,'users-field':3,'opponents-field':2}

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
conn.execute("PRAGMA synchronous=OFF")
cur = conn.cursor()

files = glob.glob(os.path.join(CACHE, 'move_*.json'))
print(f'缓存文件: {len(files)}')

updated = 0
missing = []
for f in files:
    base = os.path.basename(f)
    # move_{slug}_{hash}.json
    slug = base[len('move_'):].rsplit('_', 1)[0]
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception as e:
        print(f'解析失败 {base}: {e}')
        continue
    mid = d.get('id')
    if mid is None:
        continue
    tid = TYPE_MAP.get((d.get('type') or {}).get('name', ''), 1)
    dcid = DC_MAP.get((d.get('damage_class') or {}).get('name', ''), 3)
    tgt = d.get('target') or {}
    tgtid = TGT_MAP.get(tgt.get('name', ''), 10)
    power = d.get('power')
    pp = d.get('pp')
    accuracy = d.get('accuracy')
    priority = d.get('priority', 0)
    effect_chance = d.get('effect_chance')

    row = cur.execute('SELECT id FROM move WHERE id=?', (mid,)).fetchone()
    if row is None:
        # 缓存中存在但数据库缺失的招式：插入（无中文名则用英文名）
        zh = ''
        for e in d.get('names', []) or []:
            if e.get('language', {}).get('name') == 'zh-Hans':
                zh = e.get('name', '')
                break
        name = zh or d.get('name', '')
        cur.execute("INSERT OR IGNORE INTO move(id,name,name_en,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance) "
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    (mid, name, d.get('name', ''), tid, dcid, tgtid, power, pp, accuracy, priority, effect_chance))
        updated += 1
        continue

    cur.execute("UPDATE move SET type_id=?, damage_class_id=?, target_id=?, power=?, pp=?, accuracy=?, priority=?, effect_chance=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                (tid, dcid, tgtid, power, pp, accuracy, priority, effect_chance, mid))
    updated += 1

    # move_meta 同步
    meta = d.get('meta') or {}
    cur.execute("INSERT OR REPLACE INTO move_meta(move_id,min_hits,max_hits,drain,healing,crit_rate) VALUES(?,?,?,?,?,?)",
                (mid, meta.get('min_hits'), meta.get('max_hits'), meta.get('drain'), meta.get('healing'), meta.get('crit_rate')))

conn.commit()

# 统计
total = cur.execute('SELECT COUNT(*) FROM move').fetchone()[0]
bad = cur.execute("SELECT COUNT(*) FROM move WHERE type_id NOT BETWEEN 1 AND 18").fetchone()[0]
print(f'更新招式: {updated}, move 总数: {total}, 非法 type_id: {bad}')
conn.close()
print('完成')
