#!/usr/bin/env python3
"""
Pokemon Factory Master Setup Script
宝可梦工厂 一键初始化脚本
========================================

This script runs the complete initialization workflow:
- Creates database schema
- Seeds data (types, abilities, items, moves, pokemon)
- Verifies data integrity

Usage / 用法:
    python scripts/setup.py              # Full setup (recommended / 推荐)
    python scripts/setup.py --quick      # Schema only, skip data seeding (快速模式，仅建表)
    python scripts/setup.py --verify     # Verification only (仅验证)
    python scripts/setup.py --force      # Force rebuild (强制重建数据库)

Environment / 环境变量:
    SQLITE_DB_PATH : Path to SQLite database file / 数据库文件路径
"""
import sys, subprocess, os
from pathlib import Path

ROOT = Path(__file__).resolve().parent
STEPS = [
    ("init_data.py",   "Initialize data / 初始化数据 (schema + seeds)"),
    ("verify_sqlite.py","Verify integrity / 验证数据完整性"),
]

def run(script, label):
    fp = ROOT / script
    if not fp.exists():
        print(f"[setup] SKIP {script} (not found / 未找到)")
        return True
    print(f"\n{'='*60}\n[{label}] Running {script}...\n{'='*60}")
    ret = subprocess.call([sys.executable, str(fp)] + sys.argv[1:])
    if ret != 0:
        print(f"[setup] FAILED: {script} (exit code {ret})")
        return False
    return True

def main():
    args = [a for a in sys.argv[1:] if a.startswith('--')]

    # Verification-only mode / 仅验证模式
    if '--verify' in args:
        run('verify_sqlite.py', 'Verify')
        return

    # Quick mode: skip data seeding / 快速模式：跳过数据导入
    if '--quick' in args:
        # Run just init_data.py which creates schema and seeds basic data
        run('init_data.py', 'Quick init')
        run('verify_sqlite.py', 'Verify')
        return

    # Full setup / 完整初始化
    for script, label in STEPS:
        if not run(script, label):
            print(f"\n[setup] Failed at step: {label}")
            sys.exit(1)

    print(f"\n{'='*60}")
    print("Setup complete! / 初始化完成!")
    print("Start the backend with / 启动后端:")
    print("  cd backend && java -jar battle/target/battle-0.0.1-SNAPSHOT.jar")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
