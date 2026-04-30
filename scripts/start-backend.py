"""一键启动后端：数据库初始化 → 启动服务。

用法：
  python scripts/start-backend.py              # 普通启动
  python scripts/start-backend.py --init        # 首次启动（建表+导入CSV）
"""
from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAR = ROOT / 'pokemon-factory-backend' / 'battle-factory' / 'target' / 'battle-factory-0.0.1-SNAPSHOT.jar'


def resolve_path(raw_path: str | None, default_path: Path) -> Path:
    if not raw_path:
        return default_path
    candidate = Path(raw_path)
    return candidate if candidate.is_absolute() else ROOT / candidate


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='启动对战工厂后端')
    parser.add_argument('--jar', default=os.getenv('JAR_PATH'), help='battle-factory jar 路径')
    parser.add_argument('--port', default=os.getenv('PORT', '8090'), help='服务端口 (default: 8090)')
    parser.add_argument('--init', action='store_true', help='首次启动：启用数据库初始化 + CSV 导入')
    return parser


def main() -> int:
    args = build_parser().parse_args()
    jar_path = resolve_path(args.jar, DEFAULT_JAR)

    if not jar_path.exists():
        print(f'[start-backend] JAR 不存在: {jar_path}', file=sys.stderr)
        print('[start-backend] 请先执行: mvn -pl battle-factory -am -DskipTests package', file=sys.stderr)
        return 1

    env = os.environ.copy()
    env['SERVER_PORT'] = args.port

    if args.init:
        env['POKEMON_FACTORY_DATABASE_INITIALIZE_ON_STARTUP'] = 'true'
        env['POKEMON_FACTORY_DATABASE_IMPORT_CSV_ON_STARTUP'] = 'true'
        print('[start-backend] ⚡ 启用数据库初始化 + CSV 导入模式')

    print(f'[start-backend] Root: {ROOT}')
    print(f'[start-backend] JAR: {jar_path}')
    print(f'[start-backend] Port: {args.port}')
    print(f'[start-backend] 按 Ctrl+C 停止服务')

    process = subprocess.Popen(
        [sys.executable, '-m', 'spring_boot', 'run', '--jar', str(jar_path)] if False
        else ['java', '-jar', str(jar_path)],
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
