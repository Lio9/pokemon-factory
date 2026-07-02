"""检查 Battle.vue 的 destructure 与 useBattlePageState 的 return 是否匹配"""
import re

# 从 Battle.vue 提取 destructured 属性名
with open('frontend/src/views/Battle.vue', 'r', encoding='utf-8') as f:
    battle_content = f.read()

# 找到 destructure 部分
in_destructure = False
destructured = []
for line in battle_content.split('\n'):
    s = line.strip()
    if s == 'const {':
        in_destructure = True
        continue
    if in_destructure:
        if s == '} = useBattlePageState()':
            break
        # 提取变量名（去掉逗号和注释）
        var = s.rstrip(',').split(':')[0].strip()
        # 处理行内变量如 `  tierBgClass,`
        # 也处理 `  formatTypes: formatPokemonTypes,`
        var = var.split(':')[0].strip()
        if var:
            destructured.append(var)

# 从 useBattlePageState 提取 return 的属性名
with open('frontend/src/composables/useBattlePageState.js', 'r', encoding='utf-8') as f:
    state_content = f.read()

in_return = False
returned = []
for line in state_content.split('\n'):
    s = line.strip()
    if s == 'return {':
        in_return = True
        continue
    if in_return:
        if s == '}':
            break
        var = s.rstrip(',').split(':')[0].strip()
        # 处理 formatTypes: formatPokemonTypes
        var = var.split(':')[0].strip()
        if var:
            returned.append(var)

# 比较
missing_in_return = [d for d in destructured if d not in returned]
extra_in_return = [r for r in returned if r not in destructured]

if missing_in_return:
    print(f'Battle.vue 使用但 useBattlePageState 未返回 ({len(missing_in_return)}):')
    for m in missing_in_return:
        print(f'  - {m}')
else:
    print('✓ Battle.vue 使用的所有属性均在 useBattlePageState 中返回')

if extra_in_return:
    print(f'\nuseBattlePageState 返回但 Battle.vue 未使用 ({len(extra_in_return)}):')
    for e in extra_in_return[:10]:
        print(f'  - {e}')

# 检查 return 中的别名格式
alias_issues = [r for r in returned if ':' in r.split(':')[0].strip() and ',' not in r]
if alias_issues:
    # These are fine (formatTypes: formatPokemonTypes)
    pass

print(f'\nDestructured: {len(destructured)}, Returned: {len(returned)}')
