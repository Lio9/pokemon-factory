#!/usr/bin/env python3
"""
Pokemon Factory 数据库初始化入口。

用法:
  python scripts/init_db.py                  # 离线初始化（推荐）
  python scripts/init_db.py --online         # 离线基础 + PokeAPI 在线补全
  python scripts/init_db.py --verify         # 仅验证不修改
  python scripts/init_db.py --force          # 强制重建（危险！会清空数据）

环境变量:
  SQLITE_DB_PATH : 数据库文件路径（默认 backend/pokemon-factory.db）
"""

import os, sys, subprocess

if __name__ == "__main__":
    script = os.path.join(os.path.dirname(__file__), "init_data.py")
    if not os.path.exists(script):
        print(f"[init_db] 错误: 找不到 {script}")
        sys.exit(1)

    args = sys.argv[1:]
    # 将 --verify 转换为 --verify，--force 追加到 init_data 的参数中
    cmd = [sys.executable, script] + args

    print(f"[init_db] 委托给 init_data.py: {' '.join(cmd)}")
    sys.exit(subprocess.call(cmd))
