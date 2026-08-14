#!/usr/bin/env python3
"""
Pokemon Sprite Downloader
Downloads pokemon sprites from PokeAPI to data/image/ for local serving.
Usage: python scripts/download_sprites.py [--range 1 151] [--verify]
"""
import os, sys, json, time, urllib.request, concurrent.futures
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IMG = ROOT / "data" / "image"
SRC = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites"
FAILED = IMG / "_failed.json"

def get(url):
    for i in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory"})
            with urllib.request.urlopen(req, timeout=30) as r:
                data = r.read()
                if len(data) > 100 and data[0:4] == b"\x89PNG": return data
        except Exception:
            time.sleep(2 * (i + 1))
    return None

def dl_poke(pid):
    dest = IMG / "pokemon" / (str(pid) + ".png")
    if dest.exists(): return True
    Path(dest.parent).mkdir(parents=True, exist_ok=True)
    url = SRC + "/pokemon/" + str(pid) + ".png"
    data = get(url)
    if data: dest.write_bytes(data); return True
    url2 = SRC + "/pokemon/0" + str(pid) + ".png"
    data = get(url2)
    if data: dest.write_bytes(data); return True
    return False

def dl_poke_back(pid):
    dest = IMG / "pokemon" / "back" / (str(pid) + ".png")
    if dest.exists(): return True
    Path(dest.parent).mkdir(parents=True, exist_ok=True)
    url = SRC + "/pokemon/back/" + str(pid) + ".png"
    data = get(url)
    if data: dest.write_bytes(data); return True
    url2 = SRC + "/pokemon/back/0" + str(pid) + ".png"
    data = get(url2)
    if data: dest.write_bytes(data); return True
    return False

def batch_back(start, end):
    ids = list(range(start, end+1))
    total, ok, fail = len(ids), 0, []
    print("Downloading " + str(total) + " back sprites [" + str(start) + "-" + str(end) + "]")
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
        fs = {ex.submit(dl_poke_back, pid): pid for pid in ids}
        for f in concurrent.futures.as_completed(fs):
            pid = fs[f]
            if f.result(): ok += 1
            else: fail.append(pid)
            done = ok + len(fail)
            pct = int(done * 100 / total)
            bar = "#" * (pct // 5) + "-" * (20 - pct // 5)
            print("\\r  [" + bar + "] " + str(pct) + "% (" + str(done) + "/" + str(total) + ")", end="")
    print()
    if fail:
        with open(IMG / "_failed_back.json", "w") as f: json.dump(dict(pokemon=fail), f)
    print("Done: " + str(ok) + " ok, " + str(len(fail)) + " failed, " + str(total) + " total")
    return len(fail) == 0

def batch(start, end):
    ids = list(range(start, end+1))
    total, ok, fail = len(ids), 0, []
    print("Downloading " + str(total) + " sprites [" + str(start) + "-" + str(end) + "]")
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
        fs = {ex.submit(dl_poke, pid): pid for pid in ids}
        for f in concurrent.futures.as_completed(fs):
            pid = fs[f]
            if f.result(): ok += 1
            else: fail.append(pid)
            done = ok + len(fail)
            pct = int(done * 100 / total)
            bar = "#" * (pct // 5) + "-" * (20 - pct // 5)
            print("\\r  [" + bar + "] " + str(pct) + "% (" + str(done) + "/" + str(total) + ")", end="")
    print()
    if fail:
        with open(FAILED, "w") as f: json.dump(dict(pokemon=fail), f)
    print("Done: " + str(ok) + " ok, " + str(len(fail)) + " failed, " + str(total) + " total")
    return len(fail) == 0

def verify():
    d = IMG / "pokemon"
    if not d.exists(): print("No images directory"); return
    files = list(d.glob("*.png"))
    ok, bad = 0, 0
    for f in files:
        data = f.read_bytes()
        if len(data) > 100 and data[0:4] == b"\x89PNG": ok += 1
        else: bad += 1; print("Corrupt: " + f.name)
    print("Verified: " + str(ok) + " ok, " + str(bad) + " bad, " + str(len(files)) + " total")

def main():
    s, e, args = 1, 1025, sys.argv[1:]
    if "--verify" in args: verify(); return
    if "--back" in args:
        args.remove("--back")
        s, e = 1, 1025
        if "--range" in args:
            i = args.index("--range")
            if i + 2 <= len(args): s, e = int(args[i+1]), int(args[i+2])
        batch_back(s, e)
        return
    if "--retry" in args and FAILED.exists():
        with open(FAILED) as f: fd = json.load(f)
        fl = fd.get("pokemon", [])
        if fl: s, e = min(fl), max(fl)
    if "--range" in args:
        i = args.index("--range")
        if i + 2 <= len(args): s, e = int(args[i+1]), int(args[i+2])
    batch(s, e)

if __name__ == "__main__": main()
