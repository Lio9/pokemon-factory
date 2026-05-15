"""一键启动后端服务。

用法：
  python scripts/start-backend.py              # 启动 pokedex（默认端口 8081）
  python scripts/start-backend.py --module battle  # 启动 battle（端口 8090）
  python scripts/start-backend.py --init          # 首次启动（建表+导入CSV，仅 pokedex 支持）
"""
from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent

# module → (directory, default_port, jar_pattern)
MODULES = {
    'pokedex': ('pokedex', '8081', 'pokedex-0.0.1-SNAPSHOT.jar'),
    'battle':  ('battle', '8090', 'battle-0.0.1-SNAPSHOT.jar'),
}


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description='启动后端服务')
    p.add_argument('--module', default='pokedex', choices=list(MODULES),
                    help='服务模块 (default: pokedex)')
    p.add_argument('--port', default=None, help='服务端口')
    p.add_argument('--init', action='store_true',
                    help='首次启动：启用数据库初始化 + CSV 导入')
    return p


def main() -> int:
    args = build_parser().parse_args()

    mod_dir, default_port, jar_name = MODULES[args.module]
    port = args.port or os.getenv('PORT', default_port)
    jar_path = ROOT / 'backend' / mod_dir / 'target' / jar_name

    if not jar_path.exists():
        print(f'[start-backend] JAR 不存在: {jar_path}', file=sys.stderr)
        print(f'[start-backend] 请先执行: mvn -pl {mod_dir} -am -DskipTests package', file=sys.stderr)
        return 1

    env = os.environ.copy()
    env['SERVER_PORT'] = port

    if args.init:
        env['POKEMON_FACTORY_DATABASE_INITIALIZE_ON_STARTUP'] = 'true'
        env['POKEMON_FACTORY_DATABASE_IMPORT_CSV_ON_STARTUP'] = 'true'
        print('[start-backend] ⚡ 启用数据库初始化 + CSV 导入模式')

    print(f'[start-backend] Module: {args.module}')
    print(f'[start-backend] JAR:   {jar_path}')
    print(f'[start-backend] Port:  {port}')
    print('[start-backend] 按 Ctrl+C 停止服务')

    process = subprocess.Popen(
        ['java', '-jar', str(jar_path)],
        cwd=str(ROOT), env=env,
    )
    try:
        return process.wait()
    except KeyboardInterrupt:
        print('\n[start-backend] 停止服务...')
        process.terminate()
        try:
            return process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            return process.wait()


if __name__ == '__main__':
    raise SystemExit(main())
