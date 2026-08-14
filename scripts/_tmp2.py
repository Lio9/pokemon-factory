import sqlite3
conn = sqlite3.connect(r'D:\learn\pokemon-factory\backend\pokemon-factory.db')
cur = conn.cursor()
print('=== arm-thrust ===')
for r in cur.execute("SELECT id, name_en, type_id, damage_class_id, target_id, power, pp, accuracy, priority FROM move WHERE name_en='arm-thrust'"):
    print(r)
print('=== annihilape form ===')
for r in cur.execute("SELECT id, name_en, species_id FROM pokemon_form WHERE name_en LIKE '%annihilape%'"):
    print(r)
print('=== annihilape types ===')
for r in cur.execute("SELECT pft.form_id, t.name_en, pft.slot FROM pokemon_form_type pft JOIN type t ON t.id=pft.type_id JOIN pokemon_form f ON f.id=pft.form_id WHERE f.name_en LIKE '%annihilape%'"):
    print(r)
conn.close()
