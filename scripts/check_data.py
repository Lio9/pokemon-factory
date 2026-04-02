#!/usr/bin/env python3
"""检查数据完整性"""
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

def check_table(cursor, table, columns):
    """检查表的字段空值情况"""
    print(f"\n=== {table} ===")
    for col in columns:
        sql = f"SELECT COUNT(*), SUM(CASE WHEN {col} IS NULL OR {col} = '' THEN 1 ELSE 0 END) FROM {table}"
        cursor.execute(sql)
        row = cursor.fetchone()
        print(f"  {col}: 总数={row[0]}, 空值={row[1]}")

def show_samples(cursor):
    """显示数据示例"""
    print("\n=== 物品示例 ===")
    cursor.execute("SELECT id, name, effect_short, effect_detail FROM item WHERE effect_short != '' LIMIT 3")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 名称: {row[1]}")
        short = (row[2][:60] + '...') if row[2] and len(row[2]) > 60 else row[2]
        detail = (row[3][:60] + '...') if row[3] and len(row[3]) > 60 else row[3]
        print(f"  简述: {short}")
        print(f"  详情: {detail}")
    
    print("\n=== 特性示例 ===")
    cursor.execute("SELECT id, name, description, effect_detail FROM ability WHERE effect_detail != '' LIMIT 2")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 名称: {row[1]}")
        desc = (row[3][:80] + '...') if row[3] and len(row[3]) > 80 else row[3]
        print(f"  效果: {desc}")
    
    print("\n=== 技能示例 ===")
    cursor.execute("SELECT id, name, effect_short, effect_detail FROM move WHERE effect_detail != '' LIMIT 2")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 名称: {row[1]}")
        detail = (row[3][:80] + '...') if row[3] and len(row[3]) > 80 else row[3]
        print(f"  效果: {detail}")

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # 检查各表
    check_table(cursor, 'item', ['effect_short', 'effect_detail', 'description'])
    check_table(cursor, 'ability', ['description', 'description_en', 'effect_detail'])
    check_table(cursor, 'move', ['description', 'effect_detail'])
    check_table(cursor, 'pokemon_species', ['description', 'genus'])
    check_table(cursor, 'pokemon_form', ['form_name_zh'])
    
    # 显示示例
    show_samples(cursor)
    
    cursor.close()
    conn.close()

if __name__ == "__main__":
    main()
