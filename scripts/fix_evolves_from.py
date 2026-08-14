"""从 pokemon_evolution 表反推填充 pokemon_species.evolves_from_species_id（当前全为 NULL 导致进化链 trigger 缺失）"""
import sqlite3, shutil, os

DB = r'D:\learn\pokemon-factory\backend\pokemon-factory.db'

backup = DB + '.bak.evolves-from'
if os.path.exists(backup):
    os.remove(backup)
shutil.copy2(DB, backup)
print(f'备份 -> {backup}')

conn = sqlite3.connect(DB)
conn.execute("PRAGMA journal_mode=WAL")
cur = conn.cursor()

# 从 pokemon_evolution 读取 (evolved -> evolves_from)，优先最小 min_level 那条
rows = cur.execute('''
    SELECT evolved_species_id, evolves_from_species_id, min_level
    FROM pokemon_evolution
    ORDER BY min_level ASC
''').fetchall()

updated = 0
for evolved, evolves_from, min_level in rows:
    if evolved is None or evolves_from is None:
        continue
    # 只填充尚未设置的
    cur.execute('UPDATE pokemon_species SET evolves_from_species_id=? WHERE id=? AND evolves_from_species_id IS NULL',
                (evolves_from, evolved))
    updated += cur.rowcount

conn.commit()
print(f'更新 evolves_from_species_id: {updated} 条')
print('species 含进化前关系数量:', cur.execute('SELECT COUNT(*) FROM pokemon_species WHERE evolves_from_species_id IS NOT NULL').fetchone()[0])
conn.close()
print('完成')
