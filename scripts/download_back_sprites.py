#!/usr/bin/env python3
"""
下载宝可梦背面精灵图到 data/image/pokemon/back/ 目录。
来源：PokeAPI GitHub sprites repository。

用法：python scripts/download_back_sprites.py
"""
import os, urllib.request, time, sqlite3, sys

DB = os.path.join(os.path.dirname(__file__), '..', 'backend', 'pokemon-factory.db')
OUT_DIR = os.path.join(os.path.dirname(__file__), '..', 'data', 'image', 'pokemon', 'back')
REMOTE_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back"

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    cur.execute("SELECT id FROM pokemon_species ORDER BY id")
    ids = [r[0] for r in cur.fetchall()]
    conn.close()

    print(f"=== Downloading back sprites for {len(ids)} species ===")
    downloaded = 0
    skipped = 0
    failed = 0

    for sid in ids:
        out_path = os.path.join(OUT_DIR, f"{sid}.png")
        if os.path.exists(out_path):
            skipped += 1
            continue

        url = f"{REMOTE_BASE}/{sid}.png"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'PokemonFactory/1.0'})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = resp.read()
                if len(data) > 100:  # valid image
                    with open(out_path, 'wb') as f:
                        f.write(data)
                    downloaded += 1
                else:
                    failed += 1
        except Exception:
            failed += 1

        if (downloaded + failed) % 100 == 0:
            print(f"  ... downloaded={downloaded} skipped={skipped} failed={failed}")
        time.sleep(0.05)

    print(f"Done: downloaded={downloaded} skipped={skipped} failed={failed}")

if __name__ == '__main__':
    main()
