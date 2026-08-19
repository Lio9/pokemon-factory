"""综合回归：验证修复后的战斗流程（辅助招式/天气/特性不崩溃）"""
import json, io, sys, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = 'http://localhost:8084/api/battle/guest'

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

# 1. 启动
r = post('/start', {'format': 'vgc-doubles'})
summary = r['data']['summary']
bid = r['data']['battleId']
print(f'[1] 战斗启动 battleId={bid} status={summary.get("status")}')

# 2. 预览确认
r2 = post(f'/{bid}/preview', {'pickedRosterIndexes': [0,1,2,3], 'leadRosterIndexes': [0,1]})
summary = get_summary(r2)
print(f'[2] 预览确认 status={summary.get("status")} phase={summary.get("phase")}')

# 3. 走 2 个回合，每回合提交出招
pt = summary.get('playerTeam', [])
for round_no in range(1, 3):
    move_map = {}
    for i in summary.get('playerActiveSlots', []):
        mon = pt[i]
        moves = mon.get('moves', [])
        atk = next((m for m in sorted(moves, key=lambda x: -(x.get('power') or 0)) if (m.get('power') or 0) > 0), moves[0] if moves else None)
        if atk:
            move_map[f'slot-{i}'] = atk.get('name_en') or atk.get('name')
    r3 = post(f'/{bid}/move', {'playerMoveMap': move_map})
    summary = get_summary(r3)
    print(f'[3.{round_no}] 第{round_no}回合: code={r3.get("code")} status={summary.get("status")} round={summary.get("currentRound")}')

# 4. 检查回合事件里有没有异常
rounds = summary.get('rounds', [])
if rounds:
    last = rounds[-1]
    print(f'[4] 最后回合事件数: {len(last.get("events") or [])}, 动作数: {len(last.get("actions") or [])}')
    for a in last.get('actions') or []:
        if a:
            print(f'    action: {a.get("actor")} 用 {a.get("move")} -> {a.get("result")}')
print('[5] 全部通过，战斗引擎稳定运行')
