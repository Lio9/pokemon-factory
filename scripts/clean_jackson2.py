"""从 Spring Boot fat JAR 中移除 Jackson 2.x 兼容包"""
import zipfile, os, re

jar_path = 'backend/battle/target/battle-0.0.1-SNAPSHOT.jar'

with zipfile.ZipFile(jar_path, 'r') as zin:
    entries = {name: zin.read(name) for name in zin.namelist()}

remove = []
for name in entries:
    if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):
        # 匹配 jackson-*-2.*.jar
        m = re.match(r'^BOOT-INF/lib/(jackson-.+)-2\.\d+\.\d+(?:\.\d+)?\.jar$', name)
        if m:
            remove.append(name)

if not remove:
    print('No Jackson 2.x JARs found!')
else:
    print(f'Found {len(remove)} Jackson 2.x JARs to remove:')
    for r in sorted(remove):
        print(f'  {os.path.basename(r)}')

out_path = jar_path + '.new'
with zipfile.ZipFile(out_path, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in entries.items():
        if name not in remove:
            zout.writestr(name, data)

os.replace(out_path, jar_path)
print(f'Done! Output: {jar_path}')
print(f'Original size: {sum(len(d) for d in entries.values()) / 1024 / 1024:.1f} MB')
print(f'New entries: {len(entries) - len(remove)}')
