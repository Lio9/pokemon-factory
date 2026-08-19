"""验证新增辅助招式（Feather Dance/Eerie Impulse/Scary Face/Cotton Spore/Charm/Noble Roar）真实生效"""
import json, io, sys, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = 'http://localhost:8084/api/battle/guest'
# 我方可用的新增辅助招式（VGC 常见）
CHECK_MOVES = ['feather-dance', 'feather dance', 'eerie-impulse', 'eerie impulse', 'scary-face', 'scary face',
               'cotton-spore', 'cotton spore', 'charm', 'noble-roar', 'noble roar']

def post(path, body):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode(), headers={'Content-Type': 'application/json'}, method='POST')
    return json.loads(urllib.request.urlopen(req, timeout=25).read())

def get_summary(resp):
    s = resp.get('data', {})
    if isinstance(s, dict) and s.get('summary'):
        return s['summary']
    if isinstance(s, dict) and 'battle' in s:
        b = s['battle']
        if isinstance(b, dict) and b.get('summary_json'):
            return b['summary_json'] if isinstance(b['summary_json'], dict) else json.loads(b['summary_json'])
    return None

def sum_stages(mon):
    ss = mon.get('statStages') or {}
    return {k: v for k, v in ss.items() if v}

found = False
for attempt in range(30):
    r = post('/start', {'format': 'vgc-doubles'})
    summary = r['data']['summary']
    bid = r['data']['battleId']
    roster = summary.get('playerRoster', [])
    # 找带新增辅助招式的宝可梦
    holder_slot = None
    chosen_move = None
    for i, mon in enumerate(roster):
        for m in mon.get('moves') or []:
            mn = (m.get('name_en') or '').lower()
            if mn in CHECK_MOVES:
                holder_slot = i
                chosen_move = mn
                break
        if holder_slot is not None:
            break
    if holder_slot is None:
        continue
    # 把这个宝可梦加入首发
    picked = [0, 1, 2, 3]
    if holder_slot not in picked:
        picked[3] = holder_slot
    lead = [0, 1]
    if holder_slot not in lead:
        lead[0] = holder_slot
    r2 = post(f'/{bid}/preview', {'pickedRosterIndexes': picked, 'leadRosterIndexes': lead})
    summary = get_summary(r2)
    pt = summary.get('playerTeam', [])
    # 让 holder 使用该辅助招式（首发时选择）
    found = True
    print(f'找到 {chosen_move} (slot {holder_slot})')
    move_map = {}
    for i in summary.get('playerActiveSlots', []):
        mon = pt[i]
        # 判断这个 slot 是否 holder（通过队内匹配不方便，直接用名字）
        # 简化：让第一个 active slot 用辅助招（如果它有）
        pass
    # 重新精确：用 teamIndex 匹配
    active_slots = summary.get('playerActiveSlots', [])
    # holder 的 teamIndex = holder_slot
    move_map = {}
    for field, tidx in enumerate(active_slots):
        mon = pt[tidx]
        if tidx == holder_slot:
            # 用辅助招
            for m in mon.get('moves') or []:
                if (m.get('name_en') or '').lower() == chosen_move:
                    move_map[f'slot-{tidx}'] = chosen_move
                    break
        else:
            atk = next((m for m in sorted(mon.get('moves', []), key=lambda x: -(x.get('power') or 0)) if (m.get('power') or 0) > 0), None)
            if atk:
                move_map[f'slot-{tidx}'] = atk.get('name_en') or atk.get('name')
    print(f'  出招: {move_map}')
    r3 = post(f'/{bid}/move', {'playerMoveMap': move_map})
    summary = get_summary(r3)
    rounds = summary.get('rounds', [])
    if rounds:
        last = rounds[-1]
        for a in last.get('actions') or []:
            if a and a.get('move') == chosen_move.replace('-', ' '):
                print(f'  动作: {a.get("actor")} 用 {a.get("move")} -> result={a.get("result")}')
    # 检查对手 statStages 是否变化
    ot = summary.get('opponentTeam', [])
    for i in summary.get('opponentActiveSlots', []):
        mon = ot[i]
        ss = sum_stages(mon)
        if ss:
            print(f'  对手 {mon.get("name")} statStages: {ss}')
    break

if not found:
    print('30 场未找到带新增辅助招式的宝可梦（队伍随机，属正常）')
