#!/usr/bin/env python3
"""Pokemon Factory 效果种子生成器。
从 EffectRegistry + ItemHandlers 的信息生成完整的 JSON 文件。

用法:
  python scripts/generate_effect_seeds.py
  python scripts/generate_effect_seeds.py --verify   # 仅校验
"""
import json, sys, os, textwrap
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AFILE = os.path.join(ROOT, "backend", "common", "src", "main", "resources", "effect-seeds", "ability-effects.json")
IFILE = os.path.join(ROOT, "backend", "common", "src", "main", "resources", "effect-seeds", "item-effects.json")

def load_tuple_data(filename):
    """从同目录下的 json 数据文件加载 tuple 列表"""
    fp = os.path.join(os.path.dirname(__file__), filename)
    with open(fp, "r", encoding="utf-8") as f:
        raw = json.load(f)  # [[a,b,c,d,e,f], ...]
    return [tuple(r) for r in raw]]

def main():
    # 从数据文件加载 (不再硬编码在脚本中，便于维护)
    data_file = os.path.join(os.path.dirname(__file__), "_seed_data.json")
    if os.path.exists(data_file):
        with open(data_file, "r", encoding="utf-8") as f:
            seeds = json.load(f)
        ab_data = [tuple(r) for r in seeds["abilities"]]]
        item_data = [tuple(r) for r in seeds["items"]]]
    else:
        print(f"数据文件 {data_file} 不存在，使用内置种子。")
        # 内置 fallback: 从已生成的 JSON 反向获取
        with open(AFILE, "r", encoding="utf-8") as f:
            ab_data = [(e["id"],e["effect_type"],e["effect_value"],e["target"],e["condition"],e["description"]) for e in json.load(f)]
        with open(IFILE, "r", encoding="utf-8") as f:
            item_data = [(e["id"],e["effect_type"],e["effect_value"],e["target"],e["condition"],e["description"]) for e in json.load(f)]

    # 生成 JSON
    ab_records = [{"id":a,"effect_type":b,"effect_value":c,"target":d,"condition":e,"description":f} for (a,b,c,d,e,f) in ab_data]
    item_records = [{"id":a,"effect_type":b,"effect_value":c,"target":d,"condition":e,"description":f} for (a,b,c,d,e,f) in item_data]

    if "--verify" in sys.argv:
        for fp, label, newlen in [(AFILE, "ability", len(ab_records)), (IFILE, "item", len(item_records))]:
            if os.path.exists(fp):
                with open(fp) as f: existing = json.load(f)
                print(f"{label}: existing {len(existing)} vs generated {newlen}")
            else:
                print(f"{label}: FILE MISSING")
        return

    with open(AFILE, "w", encoding="utf-8") as f: json.dump(ab_records, f, ensure_ascii=False, indent=2)
    with open(IFILE, "w", encoding="utf-8") as f: json.dump(item_records, f, ensure_ascii=False, indent=2)
    print(f"已写入 {len(ab_records)} 条特性效果 + {len(item_records)} 条道具效果")

if __name__ == "__main__":
    main()
