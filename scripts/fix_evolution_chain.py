"""从 PokeAPI 缓存填充 pokemon_species.evolution_chain_id（当前全为 NULL 导致进化链功能失效）"""
import sqlite3, glob, json, os, re, shutil

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'
CACHE = r'D:\learn\pokemon-factory\data\pokeapi-cache'

backup = DB + '.bak.evolution-chain'
if os.path.exists(backup):
    os.remove(backup)
shutil.copy2(DB, backup)
print(f'备份 -> {backup}')

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

files = glob.glob(os.path.join(CACHE, 'pokemon-species_*.json'))
updated = 0
missing_chain = 0
for f in files:
    try:
        with open(f, encoding='utf-8') as fh:
            d = json.load(fh)
    except Exception as e:
        print(f'解析失败 {os.path.basename(f)}: {e}')
        continue
    sid = d.get('id')
    if sid is None:
        continue
    chain = d.get('evolution_chain') or {}
    chain_url = chain.get('url', '')
    m = re.search(r'/evolution-chain/(\d+)/?$', chain_url)
    chain_id = int(m.group(1)) if m else None
    if chain_id is None:
        missing_chain += 1
        continue
    cur.execute('UPDATE pokemon_species SET evolution_chain_id=? WHERE id=?', (chain_id, sid))
    updated += 1

conn.commit()
print(f'更新 evolution_chain_id: {updated} 条, 无链数据: {missing_chain}')
print('species 含链数量:', cur.execute('SELECT COUNT(*) FROM pokemon_species WHERE evolution_chain_id IS NOT NULL').fetchone()[0])
conn.close()
print('完成')
