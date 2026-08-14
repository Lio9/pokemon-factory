import sqlite3

conn = sqlite3.connect(r'D:\learn\pokemon-factory\backend\pokemon-factory.db')
cur = conn.cursor()

for t in ['pokemon_form_move', 'move_meta', 'battle']:
    print(f'=== {t} schema ===')
    for r in cur.execute(f"SELECT sql FROM sqlite_master WHERE name='{t}'"):
        print(r[0])
    print()

# 检查是否有外键引用 move 的表
print('=== tables with FK to move ===')
rows = cur.execute("SELECT name, sql FROM sqlite_master WHERE type='table' AND sql LIKE '%REFERENCES move%'").fetchall()
for name, sql in rows:
    print(name)

# 检查 foreign_keys 是否开启（默认关闭）
print('\nforeign_keys pragma:', cur.execute('PRAGMA foreign_keys').fetchone()[0])

# pokemon_form_move 里的 move_id 是否都在 1-919 的 PokeAPI id 范围内
print('form_move distinct move_id count:', cur.execute('SELECT COUNT(DISTINCT move_id) FROM pokemon_form_move').fetchone()[0])
conn.close()
