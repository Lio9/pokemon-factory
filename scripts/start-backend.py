# start-backend 文件说明
# 所属模块：项目脚本目录。
# 文件类型：Python 脚本文件。
# 核心职责：负责数据库初始化、辅助启动或项目维护类自动化任务。
# 阅读建议：建议执行前先确认输入路径与副作用范围。
# 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。

"""跨平台启动 common exec jar，用于手动观察数据库初始化日志。"""
from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAR = ROOT / 'pokemon-factory-backend' / 'common' / 'target' / 'common-0.0.1-SNAPSHOT-exec.jar'
DEFAULT_DB = ROOT / 'pokemon-factory-backend' / 'pokemon-factory.db'


def resolve_path(raw_path: str | None, default_path: Path) -> Path:
    if not raw_path:
        return default_path
    candidate = Path(raw_path)
    if candidate.is_absolute():
        return candidate
    return ROOT / candidate


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description='启动 common exec jar，跨平台观察数据库初始化日志。'
    )
    parser.add_argument(
        '--jar',
        default=os.getenv('JAR_PATH'),
        help='common exec jar 路径；默认读取 JAR_PATH 或 pokemon-factory-backend/common/target/common-0.0.1-SNAPSHOT-exec.jar',
    )
    parser.add_argument(
        '--db',
        default=os.getenv('SQLITE_DB_PATH'),
        help='SQLite 文件路径；默认读取 SQLITE_DB_PATH 或 pokemon-factory-backend/pokemon-factory.db',
    )
    parser.add_argument(
        '--java',
        default=os.getenv('JAVA_CMD', 'java'),
        help='Java 可执行命令，默认使用 java，也可通过 JAVA_CMD 覆盖',
    )
    return parser


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def main() -> int:
    args = build_parser().parse_args()
    jar_path = resolve_path(args.jar, DEFAULT_JAR)
    db_path = resolve_path(args.db, DEFAULT_DB)

    if not jar_path.exists():
        print(f'[start-backend] JAR not found: {jar_path}', file=sys.stderr)
        print('[start-backend] 请先执行 mvn -pl common -DskipTests package', file=sys.stderr)
        return 1

    ensure_parent(db_path)

    env = os.environ.copy()
    env['SQLITE_DB_PATH'] = str(db_path)

    print(f'[start-backend] Project root: {ROOT}')
    print(f'[start-backend] Using JAR: {jar_path}')
    print(f'[start-backend] Using SQLite DB: {db_path}')
    print('[start-backend] Press Ctrl+C to stop the process.')

    process = subprocess.Popen(
        [args.java, '-jar', str(jar_path)],
        cwd=str(ROOT),
        env=env,
    )

    try:
        return process.wait()
    except KeyboardInterrupt:
        print('\n[start-backend] Stopping common process...')
        process.terminate()
        try:
            return process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            if os.name == 'nt':
                process.send_signal(signal.CTRL_BREAK_EVENT)
            else:
                process.kill()
            return process.wait()


if __name__ == '__main__':
    raise SystemExit(main())