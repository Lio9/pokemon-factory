"""
从 PokeAPI 缓存完全重建 move 表与 move_meta 表。
原因：离线种子 MOVES 元组字段顺序与 INSERT 列顺序不匹配，
导致 move 表的 id/name/type_id/power/pp/accuracy 等字段全部错位，
直接破坏对战引擎计算。pokemon_form_move 引用 PokeAPI 标准 id，
重建后引用自动正确。
"""
import sqlite3, glob, json, os, shutil, sys

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

backup = DB + '.bak.move-rebuild'
if os.path.exists(backup):
    os.remove(backup)
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
conn.execute("PRAGMA foreign_keys=OFF")
cur = conn.cursor()

files = glob.glob(os.path.join(CACHE, 'move_*.json'))
print(f'缓存文件: {len(files)}')

moves = {}
for f in files:
    base = os.path.basename(f)
    slug = base[len('move_'):].rsplit('_', 1)[0]
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception as e:
        print(f'解析失败 {base}: {e}')
        continue
    if d.get('id') is not None:
        moves[d.get('id')] = d

print(f'缓存解析出招式: {len(moves)}')

# 先清空依赖表（外键关闭）
for t in ['move_meta_stat_change', 'move_meta', 'move_flag_map', 'battle_team_member_move']:
    try:
        cur.execute(f'DELETE FROM {t}')
        print(f'清空 {t}: OK')
    except Exception as e:
        print(f'清空 {t}: {e}')

cur.execute('DELETE FROM move')
print('清空 move: OK')

# 重建 move
TYPE_ID_NAMES = {1:'normal',2:'fighting',3:'flying',4:'poison',5:'ground',6:'rock',7:'bug',8:'ghost',9:'steel',
                 10:'fire',11:'water',12:'grass',13:'electric',14:'psychic',15:'ice',16:'dragon',17:'dark',18:'fairy'}
DC_ID_NAMES = {1:'physical',2:'special',3:'status'}

inserted = 0
for mid, d in sorted(moves.items()):
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

    zh = ''
    jp = ''
    en = d.get('name', '')
    for e in d.get('names', []) or []:
        lang = e.get('language', {}).get('name', '')
        if lang == 'zh-Hans' and not zh:
            zh = e.get('name', '')
        elif lang == 'ja-Hrkt' and not jp:
            jp = e.get('name', '')

    # 描述
    desc = ''
    effect_short = ''
    for e in d.get('effect_entries', []) or []:
        if e.get('language', {}).get('name') == 'zh-Hans':
            effect_short = e.get('short_effect', '') or e.get('effect', '')
            break
    for e in d.get('flavor_text_entries', []) or []:
        if e.get('language', {}).get('name') == 'zh-Hans' and e.get('version_group', {}).get('name') in ('scarlet-violet', 'sword-shield'):
            desc = e.get('flavor_text', '').replace('\n', ' ')
            break
    if not desc:
        for e in d.get('flavor_text_entries', []) or []:
            if e.get('language', {}).get('name') == 'zh-Hans':
                desc = e.get('flavor_text', '').replace('\n', ' ')
                break

    gen = d.get('generation', {})
    gen_id = None
    gen_name = gen.get('name', '') if isinstance(gen, dict) else ''
    if gen_name:
        try:
            gen_id = int(gen_name.replace('generation-', ''))
        except ValueError:
            gen_id = None

    cur.execute("""INSERT OR IGNORE INTO move
        (id, name, name_en, name_jp, type_id, damage_class_id, target_id, power, pp, accuracy, priority,
         effect_chance, effect_short, effect_detail, description, generation_id)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        (mid, zh or en, en, jp or None, tid, dcid, tgtid, power, pp, accuracy, priority,
         effect_chance, effect_short, effect_short, desc, gen_id))
    inserted += 1

conn.commit()
print(f'重建 move: {inserted} 条')

# 重建 move_meta
meta_count = 0
for mid, d in moves.items():
    if mid is None:
        continue
    meta = d.get('meta') or {}
    if not meta:
        continue
    ailment = meta.get('ailment') or {}
    category = meta.get('category') or {}
    cur.execute("""INSERT OR REPLACE INTO move_meta
        (move_id, min_hits, max_hits, min_turns, max_turns, drain, healing, crit_rate,
         ailment_id, category_id, ailment_chance, flinch_chance, stat_chance)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        (mid, meta.get('min_hits'), meta.get('max_hits'), meta.get('min_turns'), meta.get('max_turns'),
         meta.get('drain'), meta.get('healing'), meta.get('crit_rate'),
         ailment.get('id'), category.get('id'),
         meta.get('ailment_chance'), meta.get('flinch_chance'), meta.get('stat_chance')))
    meta_count += 1
conn.commit()
print(f'重建 move_meta: {meta_count} 条')

# 验证
print('\n=== 验证 ===')
total = cur.execute('SELECT COUNT(*) FROM move').fetchone()[0]
bad_type = cur.execute('SELECT COUNT(*) FROM move WHERE type_id NOT BETWEEN 1 AND 18').fetchone()[0]
null_power_status = cur.execute("SELECT COUNT(*) FROM move WHERE damage_class_id=3 AND power IS NOT NULL AND power != 0").fetchone()[0]
print(f'move 总数: {total}, 非法 type_id: {bad_type}, 变化技带威力: {null_power_status}')

for name in ['tackle', 'ember', 'water-gun', 'thunderbolt', 'earthquake', 'swords-dance', 'trick-room', 'surf']:
    r = cur.execute('SELECT id, name_en, type_id, damage_class_id, power, pp, accuracy FROM move WHERE name_en=?', (name,)).fetchone()
    print(f'  {name:14s} id={r[0]:5d} type={r[2]:3d} dc={r[3]} pwr={r[4]} pp={r[5]} acc={r[6]}')

# pokemon_form_move 引用完整性
orphan = cur.execute('SELECT COUNT(*) FROM pokemon_form_move pfm LEFT JOIN move m ON m.id=pfm.move_id WHERE m.id IS NULL').fetchone()[0]
print(f'pokemon_form_move 孤儿引用: {orphan}')
conn.close()
print('\n完成')
