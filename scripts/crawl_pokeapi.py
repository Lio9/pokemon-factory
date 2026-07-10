#!/usr/bin/env python3
"""
多线程 PokeAPI 数据爬虫 — 快速下载 + 失败重试 + 本地缓存 + 自动导入数据库

特点：
  - 多线程并发（默认 8 线程），下载速度提升 5-10 倍
  - 失败池机制：下载失败的任务记录后重试
  - 本地 JSON 缓存，断点续传
  - 先爬取后导入，爬取和导入分离
  - 自动补充 zh-Hans 中文名

用法：
  python scripts/crawl_pokeapi.py              # 完整爬取 + 导入
  python scripts/crawl_pokeapi.py --only-db     # 仅从已有缓存导入数据库
  python scripts/crawl_pokeapi.py --verify      # 仅查看进度
"""

import sqlite3, json, os, sys, time, concurrent.futures, urllib.request, urllib.error, hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = os.getenv('SQLITE_DB_PATH') or str(ROOT / 'backend' / 'pokemon-factory.db')
CACHE_DIR = ROOT / 'data' / 'pokeapi-cache'
POKEAPI = "https://pokeapi.co/api/v2"
WORKERS = 10  # 并发线程数
DELAY = 0.03  # 请求间隔（秒）
MAX_RETRIES = 3

CACHE_DIR.mkdir(parents=True, exist_ok=True)

# ============================================================
# 映射表
# ============================================================
TYPE_MAP = {'normal':1,'fighting':2,'flying':3,'poison':4,'ground':5,'rock':6,'bug':7,'ghost':8,
            'steel':9,'fire':10,'water':11,'grass':12,'electric':13,'psychic':14,'ice':15,'dragon':16,'dark':17,'fairy':18}
DC_MAP = {'physical':1,'special':2,'status':3}
TGT_MAP = {'selected-pokemon':1,'random-opponent':1,'all-opponents':2,'self':3,
           'all-adjacent':4,'all-adjacent-foes':2,'ally':3,'all-allies':7,
           'opponents-field':2,'allies-field':3,'entire-field':10,'user-or-ally':1}
LEARN_METHOD_MAP = {'level-up':1,'egg':2,'tutor':3,'machine':4,'form-change':5}

def log(msg): print(f"[crawl] {msg}")

# ============================================================
# 缓存和 HTTP
# ============================================================

def cache_path(key):
    safe = hashlib.md5(key.encode()).hexdigest()[:16]
    clean = key.replace('/', '_').replace('?', '_').replace('&', '_').replace('=', '_')
    return CACHE_DIR / f"{clean}_{safe}.json"

def fetch(url, cache_key):
    """带缓存的单次请求"""
    cp = cache_path(cache_key)
    if cp.exists():
        with open(cp) as f:
            return json.load(f)
    for attempt in range(MAX_RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "pokemon-factory/3.0"})
            with urllib.request.urlopen(req, timeout=30) as r:
                data = json.loads(r.read().decode())
            with open(cp, 'w') as f:
                json.dump(data, f, ensure_ascii=False)
            return data
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            time.sleep(DELAY * (attempt + 1) * 2)
        except Exception:
            time.sleep(DELAY * (attempt + 1) * 2)
    return None

def api_get(resource, ident):
    return fetch(f"{POKEAPI}/{resource}/{ident}", f"{resource}/{ident}")

def api_get_url(url):
    key = url.replace(f"{POKEAPI}/", "")
    return fetch(url, key)

# ============================================================
# 语言工具
# ============================================================

def lang_name(data, lang='zh-hans'):
    for n in data.get('names', []):
        if n.get('language', {}).get('name').lower() == lang:
            return n['name']
    for n in data.get('names', []):
        if n.get('language', {}).get('name') == 'en':
            return n['name']
    return data.get('name', '')

def gen_num(data):
    g = data.get('generation') or {}
    n = g.get('name', '')
    try: return int(n.replace('generation-', ''))
    except Exception: return 9

# ============================================================
# 爬虫引擎
# ============================================================

class Crawler:
    def __init__(self):
        self.results = {}       # {resource_type: {id: data}}
        self.failed = {}        # {resource_type: [(id, error)]}
        self.list_cache = {}    # {resource: [names]}

    def get_list(self, resource):
        """获取资源列表（分页）"""
        if resource in self.list_cache:
            return self.list_cache[resource]
        all_items = []
        url = f"{POKEAPI}/{resource}?limit=100&offset=0"
        while url:
            data = api_get_url(url)
            if not data:
                break
            all_items.extend(data.get('results', []))
            url = data.get('next')
        self.list_cache[resource] = all_items
        return all_items

    def crawl_batch(self, items, resource_type, fetch_fn, name_label=""):
        """并发爬取一批数据"""
        total = len(items)
        log(f"开始爬取 {name_label or resource_type}：{total} 项，{WORKERS} 线程并发")
        self.results[resource_type] = {}
        self.failed[resource_type] = []
        lock = concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS)
        futures = {}
        for i, item in enumerate(items):
            future = lock.submit(fetch_fn, item)
            futures[future] = (i, item)
        done = 0
        for future in concurrent.futures.as_completed(futures):
            i, item = futures[future]
            done += 1
            try:
                idx, data = future.result()
                if data is not None:
                    self.results[resource_type][idx] = data
                else:
                    self.failed[resource_type].append((idx, "返回空"))
            except Exception as e:
                idx = item if isinstance(item, int) else i
                self.failed[resource_type].append((idx, str(e)))
            if done % 50 == 0 or done == total:
                pct = int(done / total * 100)
                bar = "█" * (pct // 5) + "░" * (20 - pct // 5)
                print(f"\r  [{bar}] {pct}% ({done}/{total}) 失败:{len(self.failed[resource_type])}", end="")
        print()
        lock.shutdown()
        good = len(self.results[resource_type])
        bad = len(self.failed[resource_type])
        log(f"{name_label}: 成功 {good}，失败 {bad}")

    def retry_failed(self, resource_type, fetch_fn, name_label=""):
        """重试失败队列"""
        if not self.failed.get(resource_type):
            return
        fails = self.failed[resource_type]
        log(f"重试 {name_label} 失败项：{len(fails)} 个")
        new_fails = []
        for idx, err in fails:
            try:
                _, data = fetch_fn(idx)
                if data is not None:
                    self.results[resource_type][idx] = data
                else:
                    new_fails.append((idx, "重试仍为空"))
            except Exception as e:
                new_fails.append((idx, str(e)))
        self.failed[resource_type] = new_fails
        if new_fails:
            log(f"{name_label} 重试后仍有 {len(new_fails)} 项失败，已记录")
            for idx, err in new_fails[:5]:
                log(f"  ID {idx}: {err}")

    def import_to_db(self):
        """将爬取结果导入 SQLite"""
        conn = sqlite3.connect(DB)
        conn.execute("PRAGMA foreign_keys = OFF")
        conn.execute("PRAGMA journal_mode = WAL")
        conn.execute("PRAGMA synchronous = OFF")
        cur = conn.cursor()
        total_imported = 0

        # abilities
        if 'ability' in self.results:
            existing = {r[0] for r in cur.execute("SELECT id FROM ability").fetchall()}
            count = 0
            for aid, data in self.results['ability'].items():
                if aid in existing: continue
                zh = lang_name(data)
                en = data['name']
                # 中文描述：优先从 flavor_text_entries 取 zh-hans（按世代优先级）
                desc_zh = ''
                flavor_entries = data.get('flavor_text_entries', [])
                gen_priority = ["scarlet-violet", "sword-shield", "ultra-sun-ultra-moon",
                                "sun-moon", "omega-ruby-alpha-sapphire", "x-y",
                                "black-2-white-2", "black-white"]
                for gen in gen_priority:
                    for entry in flavor_entries:
                        if entry.get('language', {}).get('name') == 'zh-hans' and entry.get('version_group', {}).get('name') == gen:
                            text = entry.get('flavor_text', '').replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                            if text:
                                desc_zh = text
                                break
                    if desc_zh:
                        break
                # fallback: 任何世代的 zh-hans flavor_text
                if not desc_zh:
                    for entry in flavor_entries:
                        if entry.get('language', {}).get('name') == 'zh-hans':
                            text = entry.get('flavor_text', '').replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                            if text:
                                desc_zh = text
                                break
                # fallback: zh-hant flavor_text
                if not desc_zh:
                    for entry in flavor_entries:
                        if entry.get('language', {}).get('name') == 'zh-hant':
                            text = entry.get('flavor_text', '').replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                            if text:
                                desc_zh = text
                                break
                # fallback: effect_entries zh-hans short_effect
                if not desc_zh:
                    for e in data.get('effect_entries', []):
                        if e.get('language', {}).get('name') == 'zh-hans':
                            text = e.get('short_effect', e.get('effect', '')).replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                            if text:
                                desc_zh = text
                                break
                # 英文描述：优先 effect_entries short_effect，fallback flavor_text
                desc_en = ''
                for e in data.get('effect_entries', []):
                    if e.get('language', {}).get('name') == 'en':
                        text = e.get('short_effect', e.get('effect', '')).replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                        if text:
                            desc_en = text
                            break
                if not desc_en:
                    for entry in flavor_entries:
                        if entry.get('language', {}).get('name') == 'en':
                            text = entry.get('flavor_text', '').replace('\n', ' ').replace('\r', '').replace('\f', ' ').strip()
                            if text:
                                desc_en = text
                                break
                gen = gen_num(data)
                cur.execute("INSERT OR IGNORE INTO ability(id,name,name_en,description,description_en,generation_id,is_main_series) VALUES(?,?,?,?,?,?,1)",
                            (aid, zh, en, desc_zh, desc_en, gen))
                count += 1
            conn.commit()
            log(f"  导入 abilities: {count} 条")
            total_imported += count

        # items
        if 'item' in self.results:
            existing = {r[0] for r in cur.execute("SELECT id FROM item").fetchall()}
            count = 0
            for iid, data in self.results['item'].items():
                if iid in existing: continue
                zh = lang_name(data)
                en = data['name']
                gen = gen_num(data)
                cur.execute("INSERT OR IGNORE INTO item(id,name,name_en,cost,fling_power,generation_id) VALUES(?,?,?,?,?,?)",
                            (iid, zh or en, en, data.get('cost', 0), data.get('fling_power'), gen))
                count += 1
            conn.commit()
            log(f"  导入 items: {count} 条")
            total_imported += count

        # moves
        if 'move' in self.results:
            existing = {r[0] for r in cur.execute("SELECT id FROM move").fetchall()}
            count = 0
            for mid, data in self.results['move'].items():
                if mid in existing: continue
                zh = lang_name(data)
                en = data['name']
                tid = TYPE_MAP.get(data.get("type",{}).get("name",""), 1)
                dcid = DC_MAP.get(data.get("damage_class",{}).get("name",""), 3)
                tgt = data.get("target") or {}
                tgtid = TGT_MAP.get(tgt.get("name",""), 10)
                cur.execute("INSERT OR IGNORE INTO move(id,name,name_en,type_id,damage_class_id,target_id,power,pp,accuracy,priority,effect_chance) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    (mid, zh or en, en, tid, dcid, tgtid,
                     data.get('power'), data.get('pp'), data.get('accuracy'),
                     data.get('priority', 0), data.get('effect_chance')))
                meta = data.get('meta')
                if meta:
                    cur.execute("INSERT OR IGNORE INTO move_meta(move_id,min_hits,max_hits,drain,healing,crit_rate) VALUES(?,?,?,?,?,?)",
                        (mid, meta.get('min_hits'), meta.get('max_hits'), meta.get('drain'), meta.get('healing'), meta.get('crit_rate')))
                count += 1
            conn.commit()
            log(f"  导入 moves: {count} 条")
            total_imported += count

        # pokemon species + forms + everything
        if 'pokemon' in self.results:
            existing_sp = {r[0] for r in cur.execute("SELECT id FROM pokemon_species").fetchall()}
            count_sp = 0
            count_fm = 0
            for sid, sp_data in self.results['pokemon'].items():
                if sid in existing_sp: continue
                zh = lang_name(sp_data)
                en = sp_data['name']
                gen = gen_num(sp_data)
                desc = ''
                for e in sp_data.get('flavor_text_entries', []):
                    if e.get('language', {}).get('name') in ('zh-Hans', 'en'):
                        desc = e.get('flavor_text', '')
                        break
                genus_zh = ''
                for g in sp_data.get('genera', []):
                    if g.get('language', {}).get('name') == 'zh-Hans':
                        genus_zh = g['genus']
                        break
                gr = (sp_data.get('growth_rate') or {}).get('name', '')
                gr_map = {'slow':1,'medium-slow':2,'medium':3,'fast':4,'extra-slow':5,'extra-fast':6}
                growth_id = gr_map.get(gr)
                cur.execute("""
                    INSERT OR IGNORE INTO pokemon_species
                    (id,name,name_en,genus,generation_id,evolution_chain_id,evolves_from_species_id,
                     color,shape,habitat,growth_rate_id,gender_rate,capture_rate,base_happiness,
                     hatch_counter,is_baby,is_legendary,is_mythical,has_gender_differences,
                     forms_switchable,"order",description)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, (sid, zh or en, en, genus_zh, gen,
                      (sp_data.get('evolution_chain') or {}).get('id'),
                      (sp_data.get('evolves_from_species') or {}).get('id'),
                      (sp_data.get('color') or {}).get('name',''),
                      (sp_data.get('shape') or {}).get('name',''),
                      (sp_data.get('habitat') or {}).get('name','') if sp_data.get('habitat') else None,
                      growth_id, sp_data.get('gender_rate',-1), sp_data.get('capture_rate',0),
                      sp_data.get('base_happiness',70), sp_data.get('hatch_counter'),
                      int(sp_data.get('is_baby',False)), int(sp_data.get('is_legendary',False)),
                      int(sp_data.get('is_mythical',False)), int(sp_data.get('has_gender_differences',False)),
                      int(sp_data.get('forms_switchable',False)), sp_data.get('order',sid), desc))
                count_sp += 1

                # egg groups
                for eg in sp_data.get('egg_groups', []):
                    eg_data = api_get("egg-group", eg['name'])
                    if eg_data:
                        cur.execute("INSERT OR IGNORE INTO pokemon_species_egg_group(species_id,egg_group_id) VALUES(?,?)",
                                     (sid, eg_data['id']))

                # evolution chain
                evo_url = (sp_data.get('evolution_chain') or {}).get('url', '')
                if evo_url:
                    eid = int(evo_url.rstrip('/').split('/')[-1])
                    cur.execute("INSERT OR IGNORE INTO evolution_chain(id) VALUES(?)", (eid,))
                    evo_data = api_get_url(evo_url)
                    if evo_data:
                        self._import_evo(cur, eid, evo_data.get('chain', {}))

                # forms (pokemon entries)
                for variety in sp_data.get('varieties', []):
                    poke_url = (variety.get('pokemon') or {}).get('url', '')
                    if not poke_url: continue
                    poke_name = (variety.get('pokemon') or {}).get('name', '')
                    poke_data = api_get("pokemon", poke_name)
                    if not poke_data: continue
                    fid = poke_data['id']
                    is_default = 1 if variety.get('is_default') else 0
                    forms_data = poke_data.get('forms', [{}])
                    form_name = forms_data[0].get('name', '') if forms_data else ''
                    form_order = poke_data.get('order', fid)
                    cur.execute("""
                        INSERT OR IGNORE INTO pokemon_form
                        (id,species_id,form_name,is_default,is_mega,is_gigantamax,
                         height,weight,base_experience,"order")
                        VALUES(?,?,?,?,?,?,?,?,?,?)
                    """, (fid, sid, form_name, is_default,
                          int(form_name.endswith('-mega') or '-mega-' in form_name),
                          int(form_name.endswith('-gmax') or '-gigantamax' in form_name),
                          poke_data.get('height'), poke_data.get('weight'),
                          poke_data.get('base_experience'), form_order))
                    # types
                    for t_slot in poke_data.get('types', []):
                        t_name = (t_slot.get('type') or {}).get('name', '')
                        tid = TYPE_MAP.get(t_name)
                        if tid:
                            cur.execute("INSERT OR IGNORE INTO pokemon_form_type(form_id,type_id,slot) VALUES(?,?,?)",
                                         (fid, tid, t_slot.get('slot', 1)))
                    # stats
                    for se in poke_data.get('stats', []):
                        sid_stat = (se.get('stat') or {}).get('id')
                        if sid_stat:
                            cur.execute("INSERT OR IGNORE INTO pokemon_form_stat(form_id,stat_id,base_stat,effort) VALUES(?,?,?,?)",
                                         (fid, sid_stat, se.get('base_stat', 0), se.get('effort', 0)))
                    # abilities
                    for ab_slot in poke_data.get('abilities', []):
                        ab_name = (ab_slot.get('ability') or {}).get('name', '')
                        ab_data = api_get("ability", ab_name)
                        if ab_data:
                            cur.execute("INSERT OR IGNORE INTO pokemon_form_ability(form_id,ability_id,slot,is_hidden) VALUES(?,?,?,?)",
                                         (fid, ab_data['id'], ab_slot.get('slot', 1), int(ab_slot.get('is_hidden', False))))
                    # moves
                    for me in poke_data.get('moves', []):
                        mv_name = (me.get('move') or {}).get('name', '')
                        mv_data = api_get("move", mv_name)
                        if not mv_data: continue
                        mid = mv_data['id']
                        for vgd in me.get('version_group_details', []):
                            vg_name = (vgd.get('version_group') or {}).get('name', '')
                            vg_id = 20 if vg_name in ('scarlet-violet',) else 15
                            lm_name = (vgd.get('move_learn_method') or {}).get('name', 'level-up')
                            lmid = LEARN_METHOD_MAP.get(lm_name, 1)
                            lvl = vgd.get('level_learned_at', 0)
                            cur.execute("INSERT OR IGNORE INTO pokemon_form_move(form_id,move_id,learn_method_id,level,version_group_id) VALUES(?,?,?,?,?)",
                                         (fid, mid, lmid, lvl, vg_id))
                    count_fm += 1

            conn.commit()
            log(f"  导入 pokemon_species: {count_sp} 条, pokemon_form: {count_fm} 条")
            total_imported += count_sp + count_fm

        conn.close()
        log(f"总计导入 {total_imported} 条新数据")

    def _import_evo(self, cur, chain_id, chain_data):
        """递归导入进化链"""
        from_sp = chain_data.get('species', {})
        from_id = from_sp.get('id')
        if not from_id: return
        for evo in chain_data.get('evolves_to', []):
            to_sp = evo.get('species', {})
            to_id = to_sp.get('id')
            if not to_id: continue
            for detail in evo.get('evolution_details', []):
                trigger = detail.get('trigger', {})
                tn = trigger.get('name', '')
                tid_map = {'level-up':1,'item':2,'trade':3,'link-trade':4,'happiness':5,
                           'time-of-day':6,'weather':7,'affection':8,'spin':9,'collect':10,'special':11,'move-learn':12,'other':13}
                tid = tid_map.get(tn, 1)
                ml = detail.get('min_level')
                item = detail.get('item') or {}
                iid = None
                if item:
                    idata = api_get("item", item['name'])
                    if idata: iid = idata['id']
                tod = detail.get('time_of_day', '')
                km = detail.get('known_move') or {}
                km_data = api_get("move", km['name']) if km.get('name') else None
                kmid = km_data['id'] if km_data else None
                mh = detail.get('min_happiness')
                ma = detail.get('min_affection')
                gd = detail.get('gender')
                rs = detail.get('relative_physical_stats')
                rain = int(detail.get('needs_overworld_rain', False))
                tud = int(detail.get('turn_upside_down', False))
                cur.execute("""
                    INSERT OR IGNORE INTO pokemon_evolution
                    (evolved_species_id,evolves_from_species_id,evolution_trigger_id,
                     min_level,evolution_item_id,time_of_day,known_move_id,
                     min_happiness,min_affection,gender_id,relative_physical_stats,
                     needs_overworld_rain,turn_upside_down)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, (to_id, from_id, tid, ml, iid, tod, kmid, mh, ma, gd, rs, rain, tud))
            self._import_evo(cur, chain_id, evo)

    def print_status(self):
        log("--- 当前缓存状态 ---")
        for rt, label in [('ability','特性'),('item','道具'),('move','技能'),('pokemon','宝可梦')]:
            cached = len(list(CACHE_DIR.glob(f"{rt}-*.json"))) if rt != 'pokemon' else len(list(CACHE_DIR.glob("pokemon-species-*.json")))
            imported = len(self.results.get(rt, {}))
            failed = len(self.failed.get(rt, []))
            log(f"  {label}: 缓存 {cached} 项, 已爬取 {imported} 项, 失败 {failed}")


# ============================================================
# 爬取任务
# ============================================================

def crawl_ability(item):
    name = item if isinstance(item, str) else item['name']
    data = api_get("ability", name)
    if data: return data['id'], data
    return None, None

def crawl_item(item):
    name = item if isinstance(item, str) else item['name']
    data = api_get("item", name)
    if data: return data['id'], data
    return None, None

def crawl_move(item):
    name = item if isinstance(item, str) else item['name']
    data = api_get("move", name)
    if data: return data['id'], data
    return None, None

def crawl_pokemon_species(item):
    name = item if isinstance(item, str) else item['name']
    data = api_get("pokemon-species", name)
    if data: return data['id'], data
    return None, None

# ============================================================
# 主流程
# ============================================================

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else 'full'

    crawler = Crawler()

    if mode == '--verify':
        crawler.print_status()
        return

    if mode == '--only-db':
        # 仅从已有缓存导入数据库
        log("仅从缓存导入数据库...")
        for res_type, filename, fetch_fn, label in [
            ('ability', 'ability-*', None, '特性'),
            ('item', 'item-*', None, '道具'),
            ('move', 'move-*', None, '技能'),
        ]:
            files = list(CACHE_DIR.glob(filename))
            files = [f for f in files if 'limit' not in f.name and 'offset' not in f.name]
            data = {}
            for f in files:
                d = json.loads(f.read_text())
                data[d['id']] = d
            crawler.results[res_type] = data
            log(f"  缓存 {label}: {len(data)} 项")
        crawler.import_to_db()
        return

    if mode == '--update-zh':
        """从缓存更新数据库中的中文名"""
        log("更新中文名...")
        conn = sqlite3.connect(DB)
        conn.execute("PRAGMA journal_mode=WAL")
        cur = conn.cursor()

        for prefix, table, name_col in [
            ('pokemon-species_', 'pokemon_species', 'name'),
            ('ability-', 'ability', 'name'),
            ('item-', 'item', 'name'),
        ]:
            updated = 0
            for f in CACHE_DIR.glob(f"{prefix}*.json"):
                if 'limit' in f.name: continue
                try:
                    data = json.loads(f.read_text())
                    did = data['id']
                    zh = lang_name(data)
                    if zh:
                        cur.execute(f"UPDATE [{table}] SET {name_col}=? WHERE id=?", (zh, did))
                        updated += cur.rowcount
                except Exception:
                    pass
            conn.commit()
            log(f"  {table}: 更新 {updated} 条中文名")

        # 处理 moves（不在缓存中）
        # 从 PokeAPI live 获取
        log("  下载 moves 中文名...")
        move_list = [{'name': 'pound'}, {'name': 'karate-chop'}, {'name': 'double-slap'}]
        move_list = crawler.get_list("move")
        for i, mv in enumerate(move_list):
            data = api_get("move", mv['name'])
            if not data: continue
            zh = lang_name(data)
            if zh:
                cur.execute("UPDATE move SET name=? WHERE id=?", (zh, data['id']))
            if (i+1) % 100 == 0:
                conn.commit()
                log(f"    moves: {i+1}/{len(move_list)}")
        conn.commit()

        # 验证
        for t, label in [('pokemon_species','宝可梦'),('ability','特性'),('item','道具'),('move','技能')]:
            cnt = cur.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
            cn = cur.execute(f"SELECT COUNT(*) FROM [{t}] WHERE name GLOB '*[\x80-\xFF]*'").fetchone()[0]
            log(f"  {label}: {cnt} total, {cn} CN")
        conn.close()
        return

    log(f"多线程爬虫启动（{WORKERS} 线程）")

    # 1. abilities
    ability_list = crawler.get_list("ability")
    crawler.crawl_batch(ability_list, 'ability', crawl_ability, "特性")
    crawler.retry_failed('ability', crawl_ability, "特性")

    # 2. items
    item_list = crawler.get_list("item")
    crawler.crawl_batch(item_list, 'item', crawl_item, "道具")
    crawler.retry_failed('item', crawl_item, "道具")

    # 3. moves
    move_list = crawler.get_list("move")
    crawler.crawl_batch(move_list, 'move', crawl_move, "技能")
    crawler.retry_failed('move', crawl_move, "技能")

    # 4. pokemon species
    species_list = crawler.get_list("pokemon-species")
    crawler.crawl_batch(species_list, 'pokemon', crawl_pokemon_species, "宝可梦")
    crawler.retry_failed('pokemon', crawl_pokemon_species, "宝可梦")

    # 导入数据库
    log("导入数据库...")
    crawler.import_to_db()

    # 最终报告
    log("--- 爬取完成 ---")
    crawler.print_status()
    for rt, label in [('ability','特性'),('item','道具'),('move','技能'),('pokemon','宝可梦')]:
        if crawler.failed.get(rt):
            log(f"  {label} 仍有 {len(crawler.failed[rt])} 项永久失败")
    log("重新运行可跳过已缓存项，用 --only-db 可仅导入到数据库")

if __name__ == '__main__':
    main()
