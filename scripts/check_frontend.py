"""
检查前端所有页面是否能正常渲染
通过 Vite 获取每个页面的编译后的 JS 模块
"""
import http.client
import re
import sys

HOST = 'localhost'
PORT = 7894

pages = [
    ('/src/views/Home.vue', 'Home'),
    ('/src/views/Battle.vue', 'Battle'),
    ('/src/views/PokemonList.vue', 'PokemonList'),
    ('/src/views/MoveList.vue', 'MoveList'),
]

errors = []
for path, name in pages:
    try:
        conn = http.client.HTTPConnection(HOST, PORT, timeout=10)
        conn.request('GET', path)
        r = conn.getresponse()
        body = r.read().decode()
        conn.close()
        
        if r.status != 200:
            errors.append(f'{name}: HTTP {r.status}')
        elif 'throw' in body and 'Error' in body:
            # 检查是否有 Error: 模式
            error_lines = re.findall(r'(?:console\.)?error\([^)]+\)', body)
            if error_lines:
                errors.append(f'{name}: 包含 {len(error_lines)} 个错误引用')
        else:
            print(f'  ✓ {name} ({r.status}, {len(body)}B)')
    except Exception as e:
        errors.append(f'{name}: {str(e)}')

if errors:
    print(f'\n发现 {len(errors)} 个错误:')
    for e in errors:
        print(f'  ✗ {e}')
    sys.exit(1)
else:
    print('\n全部页面正常')
