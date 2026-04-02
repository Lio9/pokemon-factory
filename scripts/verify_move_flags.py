#!/usr/bin/env python3
"""验证关键技能标记"""
import mysql.connector
import os

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "pokemon_factory"),
    "charset": "utf8mb4",
}

conn = mysql.connector.connect(**DB_CONFIG)
cursor = conn.cursor()

print("=== 关键技能标记统计 ===\n")

# 关键标记统计
cursor.execute("""
    SELECT mf.identifier, mf.name, COUNT(*) as cnt 
    FROM move_flags mf 
    JOIN move_flag_map mfm ON mfm.flag_id = mf.id 
    WHERE mf.identifier IN ('punch', 'bite', 'sound', 'pulse', 'ballistics', 'contact')
    GROUP BY mf.id
""")

for row in cursor.fetchall():
    print(f"{row[1]} ({row[0]}): {row[2]} 个技能")

# 显示几个关键标记的技能示例
print("\n=== 拳击类技能示例 ===")
cursor.execute("""
    SELECT m.id, m.name 
    FROM move m 
    JOIN move_flag_map mfm ON mfm.move_id = m.id 
    JOIN move_flags mf ON mf.id = mfm.flag_id 
    WHERE mf.identifier = 'punch' AND m.power > 0
    LIMIT 10
""")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]}")

print("\n=== 咬类技能示例 ===")
cursor.execute("""
    SELECT m.id, m.name, m.power
    FROM move m 
    JOIN move_flag_map mfm ON mfm.move_id = m.id 
    JOIN move_flags mf ON mf.id = mfm.flag_id 
    WHERE mf.identifier = 'bite' AND m.power > 0
""")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]} (威力{row[2]})")

print("\n=== 声音类技能示例 ===")
cursor.execute("""
    SELECT m.id, m.name, m.power
    FROM move m 
    JOIN move_flag_map mfm ON mfm.move_id = m.id 
    JOIN move_flags mf ON mf.id = mfm.flag_id 
    WHERE mf.identifier = 'sound' AND m.power > 0
    LIMIT 10
""")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]} (威力{row[2]})")

cursor.close()
conn.close()
