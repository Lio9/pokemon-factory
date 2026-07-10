#!/usr/bin/env python3
"""
数据库初始化工具（一体化）。
运行模式：
  python scripts/init_data.py              # 离线完整初始化（推荐）
  python scripts/init_data.py --online     # 离线基础 + PokeAPI 在线补全
  python scripts/init_data.py --verify     # 仅验证不修改

环境变量: SQLITE_DB_PATH 可覆盖数据库路径
"""

import sqlite3, json, os, sys, time, urllib.request, urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = os.getenv('SQLITE_DB_PATH') or str(ROOT / 'backend' / 'pokemon-factory.db')
POKEAPI = "https://pokeapi.co/api/v2"
DELAY = 0.06

# ============================================================
# 离线数据
# ============================================================

TYPE_EFFICACY = {
    1:{2:100,3:100,4:100,5:100,6:50,7:100,8:0,9:50,10:100,11:100,12:100,13:100,14:100,15:100,16:100,17:100,18:100},
    2:{1:200,2:100,3:50,4:50,5:100,6:200,7:50,8:0,9:200,10:100,11:100,12:100,13:100,14:50,15:200,16:100,17:200,18:50},
    3:{1:100,2:200,3:100,4:100,5:100,6:50,7:200,8:100,9:50,10:100,11:100,12:200,13:50,14:100,15:100,16:100,17:100,18:100},
    4:{1:100,2:100,3:100,4:50,5:50,6:50,7:100,8:50,9:0,10:100,11:100,12:200,13:100,14:100,15:100,16:100,17:100,18:200},
    5:{1:100,2:100,3:0,4:200,5:100,6:200,7:50,8:100,9:200,10:100,11:100,12:50,13:200,14:100,15:100,16:100,17:100,18:100},
    6:{1:100,2:50,3:200,4:100,5:50,6:100,7:200,8:100,9:50,10:200,11:100,12:100,13:100,14:100,15:200,16:100,17:100,18:100},
    7:{1:100,2:50,3:50,4:50,5:100,6:100,7:100,8:50,9:50,10:50,11:100,12:200,13:100,14:200,15:100,16:100,17:200,18:50},
    8:{1:0,2:100,3:100,4:100,5:100,6:100,7:100,8:200,9:100,10:100,11:100,12:100,13:100,14:200,15:100,16:100,17:50,18:100},
    9:{1:50,2:100,3:50,4:0,5:100,6:200,7:100,8:100,9:50,10:50,11:50,12:100,13:50,14:100,15:200,16:100,17:100,18:200},
    10:{1:100,2:100,3:100,4:100,5:100,6:50,7:200,8:100,9:200,10:50,11:50,12:200,13:100,14:100,15:200,16:50,17:100,18:100},
    11:{1:100,2:100,3:100,4:100,5:200,6:200,7:100,8:100,9:100,10:200,11:50,12:50,13:100,14:100,15:100,16:50,17:100,18:100},
    12:{1:100,2:100,3:50,4:50,5:200,6:200,7:50,8:100,9:50,10:50,11:200,12:50,13:100,14:100,15:100,16:50,17:100,18:100},
    13:{1:100,2:100,3:200,4:100,5:0,6:100,7:100,8:100,9:100,10:100,11:200,12:50,13:50,14:100,15:100,16:50,17:100,18:100},
    14:{1:100,2:200,3:100,4:200,5:100,6:100,7:100,8:100,9:50,10:100,11:100,12:100,13:100,14:50,15:100,16:100,17:0,18:100},
    15:{1:100,2:100,3:200,4:100,5:200,6:100,7:100,8:100,9:50,10:50,11:50,12:200,13:100,14:100,15:50,16:200,17:100,18:100},
    16:{1:100,2:100,3:100,4:100,5:100,6:100,7:100,8:100,9:50,10:100,11:100,12:100,13:100,14:100,15:100,16:200,17:100,18:0},
    17:{1:100,2:50,3:100,4:100,5:100,6:100,7:100,8:200,9:50,10:100,11:100,12:100,13:100,14:200,15:100,16:100,17:50,18:50},
    18:{1:100,2:200,3:100,4:50,5:100,6:100,7:100,8:100,9:50,10:50,11:100,12:100,13:100,14:100,15:100,16:200,17:200,18:100},
}

ABILITIES = [
    (1,"毅力","stench"),(2,"降雨","drizzle"),(3,"加速","speed-boost"),(4,"战斗盔甲","battle-armor"),
    (5,"结实","sturdy"),(6,"湿气","damp"),(7,"柔软","limber"),(8,"沙隐","sand-veil"),
    (9,"静电","static"),(10,"蓄电","volt-absorb"),(11,"储水","water-absorb"),(12,"迟钝","oblivious"),
    (13,"黏着","sticky-hold"),(14,"自然回复","natural-cure"),(15,"避雷针","lightning-rod"),
    (16,"天恩","serene-grace"),(17,"悠游自如","swift-swim"),(18,"叶绿素","chlorophyll"),
    (19,"发光","illuminate"),(20,"追踪","trace"),(21,"大力士","huge-power"),(22,"威吓","intimidate"),
    (23,"雪隐","snow-cloak"),(24,"吸盘","suction-cups"),(25,"厚脂肪","thick-fat"),
    (30,"干燥肌肤","dry-skin"),(31,"恒净之躯","clear-body"),
    (50,"恒净之躯","clear-body"),(51,"火焰之躯","flame-body"),(52,"虫之预感","swarm"),
    (60,"压迫感","pressure"),(61,"降雨","drizzle"),(62,"飞毛腿","quick-feet"),
    (91,"避雷针","lightning-rod"),(92,"神奇守护","wonder-guard"),(93,"沙之力","sand-force"),
    (94,"雪之力","snow-force"),(95,"恶臭","stench"),
]

ITEMS = [
    (83,"木炭","charcoal"),(84,"毒针","poison-barb"),(85,"奇迹种子","miracle-seed"),
    (86,"黑色眼镜","black-glasses"),(87,"不融之冰","never-melt-ice"),(88,"硬石头","hard-stone"),
    (89,"柔软沙子","soft-sand"),(90,"尖锐鸟喙","sharp-beak"),(91,"金属膜","metal-coat"),
    (103,"丝绸围巾","silk-scarf"),(130,"生命宝珠","life-orb"),(131,"剩饭","leftovers"),
    (188,"文柚果","sitrus-berry"),(189,"橙橙果","oran-berry"),(209,"弱点保险","weakness-policy"),
    (213,"气势头带","focus-band"),(214,"气势披带","focus-sash"),(222,"金属粉末","metal-powder"),
    (229,"气息球","air-balloon"),(232,"附着针","sticky-barb"),(234,"突击背心","assault-vest"),
    (236,"红牌","red-card"),(239,"达人带","expert-belt"),(242,"节拍器","big-root"),
    (267,"博识眼镜","wise-glasses"),(268,"奇迹眼镜","wonder-goggles"),(271,"力量头带","muscle-band"),
    (272,"贝壳之铃","shell-bell"),(275,"逃脱按键","eject-button"),(276,"逃脱背包","eject-pack"),
    (279,"火焰珠","flame-orb"),(280,"剧毒宝珠","toxic-orb"),(281,"白色香草","white-herb"),
    (283,"力量草药","power-herb"),(285,"进化奇石","eviolite"),(286,"锋利龙牙","razor-fang"),
    (295,"树果汁","berry-juice"),(299,"讲究头带","choice-band"),(305,"凸凸头盔","rocky-helmet"),
    (309,"驱劲能量","booster-energy"),(311,"环形目标","ring-target"),(315,"轻石","light-clay"),
    (322,"零余果","lum-berry"),(323,"利木果","leppa-berry"),(326,"文柚果","sitrus-berry"),
    (327,"讲究眼镜","choice-specs"),(328,"讲究围巾","choice-scarf"),
]

MOVES = [
    (1,"拍击","pound",40,35,100,1,1),(19,"撞击","tackle",40,35,100,1,1),
    (30,"叫声","growl",1,40,100,1,3),(31,"吼叫","roar",1,20,100,1,3),
    (33,"超音波","supersonic",1,20,55,1,3),(37,"火焰放射","ember",40,25,100,10,2),
    (38,"喷射火焰","flamethrower",90,15,100,10,2),(40,"水枪","water-gun",40,25,100,11,2),
    (41,"水炮","hydro-pump",110,5,80,11,2),(42,"冲浪","surf",90,15,100,11,2),
    (43,"冰冻光束","ice-beam",90,10,100,15,2),(44,"暴风雪","blizzard",110,5,70,15,2),
    (48,"破坏光线","hyper-beam",150,5,90,1,2),(56,"吸取","absorb",20,25,100,12,2),
    (57,"百万吸取","mega-drain",40,15,100,12,2),(58,"寄生种子","leech-seed",1,10,90,12,3),
    (59,"生长","growth",1,20,100,1,3),(61,"日光束","solar-beam",120,10,100,12,2),
    (69,"电击","thunder-shock",40,30,100,13,2),(70,"十万伏特","thunderbolt",90,15,100,13,2),
    (71,"电磁波","thunder-wave",1,20,90,13,3),(72,"打雷","thunder",110,10,70,13,2),
    (74,"地震","earthquake",100,10,100,5,1),(77,"剧毒","toxic",1,10,90,4,3),
    (78,"念力","confusion",50,25,100,14,2),(79,"精神强念","psychic",90,10,100,14,2),
    (82,"高速移动","agility",1,30,100,14,3),(83,"电光一闪","quick-attack",40,30,100,1,1),
    (85,"瞬间移动","teleport",1,20,100,14,3),(89,"变小","minimize",1,10,100,1,3),
    (90,"催眠术","hypnosis",1,20,60,14,3),(91,"瑜珈姿势","meditate",1,40,100,14,3),
    (92,"混乱","confusion",50,25,100,14,2),(93,"叫声","growl",1,40,100,1,3),
    (95,"吐丝","string-shot",1,40,95,7,3),(99,"头锤","headbutt",70,15,100,1,1),
    (103,"铁壁","iron-defense",1,15,100,9,3),(104,"铁壁","iron-defense",1,15,100,9,3),
    (106,"腹鼓","belly-drum",1,10,100,1,3),(115,"长嚎","howl",1,40,100,1,3),
    (116,"磨爪","hone-claws",1,15,100,17,3),(117,"空气利刃","air-cutter",60,25,95,3,2),
    (129,"冥想","calm-mind",1,20,100,14,3),(130,"剑舞","swords-dance",1,20,100,1,3),
    (133,"高速旋转","rapid-spin",50,40,100,1,1),(143,"龙舞","dragon-dance",1,20,100,16,3),
    (144,"龙尾","dragon-tail",60,10,90,16,1),(145,"巴投","circle-throw",60,10,90,2,1),
    (146,"吹飞","whirlwind",1,20,100,1,3),(147,"杂技","acrobatics",55,15,100,3,1),
    (148,"蓄力","stockpile",1,20,100,1,3),(149,"喷出","spit-up",1,10,100,1,2),
    (150,"吞下","swallow",1,10,100,1,3),(151,"报恩","return",1,20,100,1,1),
    (155,"击落","knock-off",65,25,100,17,1),(156,"小偷","thief",60,25,100,17,1),
    (157,"打嗝","belch",120,10,90,4,2),(158,"自然之恩","natural-gift",1,15,100,1,1),
    (159,"虫灾","bug-bite",60,20,100,7,1),(160,"封印","imprison",1,10,100,14,3),
    (161,"临别礼物","memento",1,10,100,17,3),(162,"治愈之愿","healing-wish",1,10,100,14,3),
    (163,"盐腌","salt-cure",40,15,100,6,1),(164,"毒旋陀螺","mortal-spin",30,15,100,4,1),
    (165,"换场","court-change",1,10,100,14,3),(166,"蜕尾","shed-tail",1,10,100,1,3),
    (167,"复活祈愿","revival-blessing",1,1,100,1,3),(168,"新月舞","lunar-dance",1,10,100,14,2),
    (169,"魔法空间","magic-room",1,10,100,14,3),(170,"奇妙空间","wonder-room",1,10,100,14,3),
    (171,"戏法","trick",1,10,100,14,3),(172,"分担痛楚","pain-split",1,20,100,1,3),
    (173,"哈欠","yawn",1,10,100,1,3),(174,"重力","gravity",1,5,100,14,3),
    (175,"戏法空间","trick-room",1,5,100,14,3),(176,"接棒","baton-pass",1,40,100,1,3),
    (177,"蝶舞","quiver-dance",1,20,100,7,3),(178,"破壳","shell-smash",1,15,100,1,3),
    (179,"防守平分","defense-curl",1,40,100,1,3),(180,"身体轻撞","body-slam",85,15,100,1,1),
]

# ============================================================
# 辅助函数
# ============================================================

def log(msg): print(f"[init] {msg}")

def slug(name):
    return name.lower().replace(" ","-").replace("'","").replace(".","").replace("é","e").replace("♀","-f").replace("♂","-m")

def api_get(path, retries=3):
    for a in range(retries):
        try:
            req = urllib.request.Request(f"{POKEAPI}/{path}", headers={"User-Agent":"pokemon-factory/1.0"})
            with urllib.request.urlopen(req, timeout=15) as r:
                return json.loads(r.read().decode())
        except Exception: time.sleep(DELAY*3)
    return None

def api_list(resource, lim=10000):
    d = api_get(f"{resource}?limit={lim}")
    return d.get("results",[]) if d else []

def progress(cur, tot, label=""):
    pct = int(cur/tot*100) if tot>0 else 100
    bar = "█"*(pct//5)+"░"*(20-pct//5)
    print(f"\r  [{bar}] {pct}% {label} ({cur}/{tot})", end="")
    if cur>=tot: print()

# ============================================================
# 模式
# ============================================================

def seed_schema(cur):
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    if cur.fetchall(): return
    sp = ROOT / 'backend' / 'common' / 'src' / 'main' / 'resources' / 'db' / 'init' / '001_core_schema.sql'
    if sp.exists():
        with open(sp) as f: cur.executescript(f.read())
        log("Schema 已创建")

def seed_offline(cur):
    # type_efficacy
    cur.execute("DELETE FROM type_efficacy")
    for atk,tgts in TYPE_EFFICACY.items():
        for dfn,fac in tgts.items():
            if dfn==atk: continue
            cur.execute("INSERT OR IGNORE INTO type_efficacy (attacking_type_id, defending_type_id, damage_factor) VALUES (?,?,?)", (atk,dfn,fac))
    log(f"type_efficacy: {cur.execute('SELECT COUNT(*) FROM type_efficacy').fetchone()[0]}")

    # ability (only if not populated by PokeAPI)
    if cur.execute("SELECT COUNT(*) FROM ability").fetchone()[0] < 200:
        cur.execute("DELETE FROM ability")
        for a in ABILITIES:
            cur.execute("INSERT OR IGNORE INTO ability(id,name,name_en,generation_id,is_main_series) VALUES(?,?,?,9,1)", a)
    log(f"ability: {cur.execute('SELECT COUNT(*) FROM ability').fetchone()[0]}")

    # item
    if cur.execute("SELECT COUNT(*) FROM item").fetchone()[0] < 500:
        cur.execute("DELETE FROM item")
        for i in ITEMS:
            cur.execute("INSERT OR IGNORE INTO item(id,name,name_en,cost,generation_id) VALUES(?,?,?,0,9)", i)
    log(f"item: {cur.execute('SELECT COUNT(*) FROM item').fetchone()[0]}")

    # move
    if cur.execute("SELECT COUNT(*) FROM move").fetchone()[0] < 500:
        cur.execute("DELETE FROM move")
        for m in MOVES:
            cur.execute("INSERT OR IGNORE INTO move(id,name,name_en,type_id,damage_class_id,power,pp,accuracy,target_id) VALUES(?,?,?,?,?,?,?,?,10)", m)
    log(f"move: {cur.execute('SELECT COUNT(*) FROM move').fetchone()[0]}")

    # effect seeds
    for fname, tbl, fk_col in [("ability-effects.json","ability_effect","ability_id"),("item-effects.json","item_effect","item_id")]:
        fp = ROOT / 'backend' / 'common' / 'src' / 'main' / 'resources' / 'effect-seeds' / fname
        if fp.exists():
            for e in json.load(open(fp)):
                cur.execute(f"INSERT OR IGNORE INTO {tbl}(id,{fk_col},effect_type,effect_value,target,condition,description) VALUES(?,?,?,?,?,?,?)",
                    (e['id'],e['id'],e['effect_type'],json.dumps(e['effect_value']) if e['effect_value'] else None,e['target'],e['condition'],e['description']))
    log(f"ability_effect: {cur.execute('SELECT COUNT(*) FROM ability_effect').fetchone()[0]}")
    log(f"item_effect: {cur.execute('SELECT COUNT(*) FROM item_effect').fetchone()[0]}")

def seed_pokeapi(cur):
    """从 PokeAPI 补充完整数据（耗时约 5-10 分钟）"""
    conn = cur.connection

    for t in ['ability','ability_effect','item','item_effect','move','move_meta','move_meta_stat_change',
              'move_flag_map','move_flags','pokemon_species','pokemon_form','pokemon_form_type',
              'pokemon_form_ability','pokemon_form_stat','pokemon_form_move','type_efficacy']:
        pass  # 保留已有数据

    # 1. 特性
    log("PokeAPI: abilities...")
    abilities_list = api_list("ability")
    total_abilities = len(abilities_list)
    for i,ab in enumerate(abilities_list):
        data = api_get(f"ability/{slug(ab['name'])}")
        if not data: continue
        eng = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="en"),{})
        zh = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="zh-Hans"),{})
        eng_eff = next((e for e in data.get("effect_entries",[]) if e.get("language",{}).get("name")=="en"),{})
        cur.execute("INSERT OR IGNORE INTO ability(id,name,name_en,generation_id) VALUES(?,?,?,?)",
            (data['id'],zh.get('name',data['name']),data['name'],
             str(data.get('generation',{}).get('name','')).replace('generation-','')))
        for ef in data.get('effect_entries',[]):
            la=ef.get('language',{}).get('name','')
            if la in ('en','zh-Hans'):
                cur.execute("INSERT OR IGNORE INTO ability_effect(ability_id,effect_type,effect_value,target,condition,description) VALUES(?,?,?,?,?,?)",
                    (data['id'],'description',ef.get('effect',''),la,ef.get('short_effect',''),''))
        progress(i+1, total_abilities, "abilities")
        time.sleep(DELAY)
    conn.commit()

    # 2. 道具
    log("PokeAPI: items...")
    items_list = api_list("item",2000)
    total_items = len(items_list)
    for i,it in enumerate(items_list):
        data = api_get(f"item/{slug(it['name'])}")
        if not data: continue
        eng = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="en"),{})
        zh = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="zh-Hans"),{})
        cur.execute("INSERT OR IGNORE INTO item(id,name,name_en,cost,fling_power,generation_id) VALUES(?,?,?,?,?,?)",
            (data['id'],zh.get('name',data['name']),data['name'],data.get('cost',0),data.get('fling_power'),
             str(data.get('generation',{}).get('name','')).replace('generation-','')))
        for ef in data.get('effect_entries',[]):
            la=ef.get('language',{}).get('name','')
            if la in ('en','zh-Hans'):
                cur.execute("INSERT OR IGNORE INTO item_effect(item_id,effect_type,effect_value,target,condition,description) VALUES(?,?,?,?,?,?)",
                    (data['id'],'effect',ef.get('effect',''),la,ef.get('short_effect',''),''))
        progress(i+1, total_items, "items")
        time.sleep(DELAY)
    conn.commit()

    # 3. 招式
    log("PokeAPI: moves...")
    TYPE_MAP = {'normal':1,'fighting':2,'flying':3,'poison':4,'ground':5,'rock':6,'bug':7,'ghost':8,
                'steel':9,'fire':10,'water':11,'grass':12,'electric':13,'psychic':14,'ice':15,'dragon':16,'dark':17,'fairy':18}
    DC_MAP = {'physical':1,'special':2,'status':3}
    TGT_MAP = {'adjacent-allies':3,'adjacent-foe':1,'all-adjacent':4,'all-adjacent-foes':2,'all-other-pokemon':10,
               'ally':3,'any':1,'entire-field':10,'random-opponent':1,'self':3,'selected-pokemon':1,'user':3,
               'user-or-ally':1,'users-field':3,'opponents-field':2}
    moves_list = api_list("move",2000)
    total_moves = len(moves_list)
    for i,mv in enumerate(moves_list):
        data = api_get(f"move/{slug(mv['name'])}")
        if not data: continue
        eng = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="en"),{})
        zh = next((e for e in data.get("names",[]) if e.get("language",{}).get("name")=="zh-Hans"),{})
        tid = TYPE_MAP.get(data.get("type",{}).get("name",""),1)
        dcid = DC_MAP.get(data.get("damage_class",{}).get("name",""),3)
        tgtid = TGT_MAP.get(data.get("target",{}).get("name","") if data.get("target") else "",10)
        cur.execute("INSERT OR IGNORE INTO move(id,name,name_en,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            (data['id'],zh.get('name',data['name']),data['name'],tid,dcid,tgtid,data.get('power'),data.get('pp'),data.get('accuracy'),data.get('priority',0),data.get('effect_chance')))
        meta = data.get('meta')
        if meta:
            cur.execute("INSERT OR IGNORE INTO move_meta(move_id,min_hits,max_hits,drain,healing,crit_rate) VALUES(?,?,?,?,?,?)",
                (data['id'],meta.get('min_hits'),meta.get('max_hits'),meta.get('drain'),meta.get('healing'),meta.get('crit_rate')))
        progress(i+1, total_moves, "moves")
        time.sleep(DELAY)
    conn.commit()

def verify(cur):
    checks = {'type': 18, 'type_efficacy': 200, 'ability': 1, 'item': 1, 'move': 1,
              'ability_effect': 1, 'item_effect': 1, 'move_damage_class': 3, 'generation': 9, 'stat': 8}
    all_ok = True
    print(f"\n{'表':25s} {'数据量':>8}")
    print('-'*35)
    for t,exp in checks.items():
        cnt = cur.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
        ok = "✅" if cnt >= exp else "⚠️"
        print(f"  {t:25s} {cnt:>8} {ok}")
        if cnt < exp: all_ok = False
    for t in ['pokemon_species','pokemon_form']:
        cnt = cur.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
        ok = "✅" if cnt >= 100 else "⬜"
        print(f"  {t:25s} {cnt:>8} {ok}")
    print(f"\n  总体: {'✅ 通过' if all_ok else '⚠️ 部分缺失'}")

# ============================================================
# 主流程
# ============================================================

def main():
    mode = "--verify" if "--verify" in sys.argv else "--online" if "--online" in sys.argv else "full"
    log(f"模式: {mode}")
    log(f"数据库: {DB}")

    conn = sqlite3.connect(DB)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=OFF")
    cur = conn.cursor()

    if mode != "--verify":
        seed_schema(cur)
        seed_offline(cur)
        conn.commit()

    if mode == "--online":
        seed_pokeapi(cur)
        conn.commit()

    verify(cur)
    conn.close()

    log("完成")

if __name__ == '__main__':
    main()
