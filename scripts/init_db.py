#!/usr/bin/env python3
"""初始化数据库流程：删除旧 SQLite、启动 common 触发数据库初始化、导入 CSV、验证数据。
使用环境变量：
  SQLITE_DB_PATH - sqlite 文件路径（可选）
  JAR_PATH - 后端 jar 路径（可选）
  MIGRATION_TIMEOUT - 等待数据库初始化完成的超时时间（秒，默认 120）

运行示例：
  python scripts\init_db.py

脚本会启动 java -jar <JAR_PATH>，等待 generation / player / app_user 等核心表创建完成，
之后运行 scripts/import_to_sqlite.py 导入 CSV，并运行 scripts/verify_sqlite.py 验证。
"""
from pathlib import Path
import os
import sys
import time
import sqlite3
import shutil
import subprocess
from datetime import datetime

ROOT = Path(__file__).resolve().parent.parent


# 统一打印初始化日志，方便在 CI 或命令行里快速定位初始化阶段。
def log(msg):
    print(f"[init_db] {msg}")


def find_jar(default=None):
    """解析 common 启动 JAR 路径。优先读环境变量，其次使用默认构建产物路径。"""
    jar = os.getenv('JAR_PATH')
    if jar:
        p = Path(jar)
        if not p.is_absolute():
            p = ROOT.joinpath(jar)
        return p
    if default:
        return Path(default)
    return ROOT.joinpath('pokemon-factory-backend','common','target','common-0.0.1-SNAPSHOT.jar')


def find_db(default=None):
    """解析 SQLite 文件路径。允许外部通过环境变量覆盖默认数据库位置。"""
    db = os.getenv('SQLITE_DB_PATH')
    if db:
        p = Path(db)
        if not p.is_absolute():
            p = ROOT.joinpath(db)
        return p
    if default:
        return Path(default)
    return ROOT.joinpath('pokemon-factory-backend','pokemon-factory.db')


def backup_and_remove(db_path: Path):
    """初始化前备份并删除旧数据库，确保冷启动导入使用干净的新库。"""
    if db_path.exists():
        ts = datetime.now().strftime('%Y%m%d%H%M%S')
        bak = db_path.with_suffix(db_path.suffix + f'.bak.{ts}')
        shutil.copy2(db_path, bak)
        log(f"已备份旧数据库到: {bak}")
        db_path.unlink()
        log(f"已删除旧数据库: {db_path}")
    else:
        log("未发现已有 sqlite 文件，跳过删除步骤")


def wait_for_schema(db_path: Path, timeout: int=120):
    """轮询 common 初始化结果，直到核心表和业务表都创建完成。"""
    required_tables = {'generation', 'move', 'pokemon', 'player', 'battle', 'app_user'}
    log(f"等待 common 完成数据库初始化（目标表: {sorted(required_tables)}），超时 {timeout}s...")
    start = time.time()
    while time.time() - start < timeout:
        if db_path.exists():
            try:
                conn = sqlite3.connect(str(db_path))
                cur = conn.cursor()
                cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
                current_tables = {row[0] for row in cur.fetchall()}
                missing_tables = sorted(required_tables - current_tables)
                if not missing_tables:
                    conn.close()
                    return True
                log(f"仍缺少数据表: {missing_tables}")
                conn.close()
            except sqlite3.Error as e:
                log(f"打开 sqlite 文件失败: {e}")
        time.sleep(1)
    return False


def run_import_and_verify(py: str, root: Path):
    """在 common 建库完成后继续执行 CSV 导入与数据校验。"""
    import_script = root.joinpath('scripts','import_to_sqlite.py')
    verify_script = root.joinpath('scripts','verify_sqlite.py')

    if not import_script.exists():
        log(f"找不到导入脚本: {import_script}")
        return False

    env = os.environ.copy()
    # common 已经完成数据库初始化，这里只负责导入数据，不再重复跑 schema。
    env['SKIP_DB_INIT'] = '1'

    log("开始执行数据导入脚本 (import_to_sqlite.py)...")
    r = subprocess.run([py, str(import_script)], env=env, cwd=str(root))
    if r.returncode != 0:
        log(f"导入脚本返回非零退出码: {r.returncode}")
        return False

    log("导入完成，运行数据校验脚本 (verify_sqlite.py) ...")
    if verify_script.exists():
        r2 = subprocess.run([py, str(verify_script)], env=env, cwd=str(root))
        if r2.returncode != 0:
            log(f"校验脚本返回非零退出码: {r2.returncode}")
            return False
    else:
        log("未找到 verify_sqlite.py，跳过校验")

    return True


def main():
    """串起完整的本地初始化流程：建库、导入、校验、清理进程。"""
    jar = find_jar()
    db = find_db()
    timeout = int(os.getenv('MIGRATION_TIMEOUT', '120'))

    log(f"项目根目录: {ROOT}")
    log(f"使用 JAR: {jar}")
    log(f"使用 SQLite DB: {db}")

    if not jar.exists():
        log("错误: 找不到后端 JAR，请先构建后端（mvn package）或设置环境变量 JAR_PATH 指向 jar 文件")
        sys.exit(2)

    # 这里保留“备份后重建”的策略，是为了让导入脚本始终从可预期的干净结构开始跑。
    backup_and_remove(db)

    # 即使数据库文件不存在，也要先确保目录存在，避免 common 因路径缺失无法创建 sqlite。
    db.parent.mkdir(parents=True, exist_ok=True)

    # 启动 common Jar，统一初始化数据库
    env = os.environ.copy()
    env['SQLITE_DB_PATH'] = str(db)
    log('启动 common Jar 以初始化数据库...')
    proc = subprocess.Popen(['java', '-jar', str(jar)], cwd=str(ROOT), env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    log(f'common 进程 PID={proc.pid}')

    try:
        ok = wait_for_schema(db, timeout=timeout)
        if not ok:
            log('错误: common 数据库初始化未在超时时间内完成')
            # 尝试输出简单提示并退出非零
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(3)

        log('数据库结构初始化完成，开始导入数据...')
        py = sys.executable
        success = run_import_and_verify(py, ROOT)
        if not success:
            log('错误: 导入或校验失败')
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(4)

        log('数据导入与校验完成')

    finally:
        # 初始化脚本只把 common 当作一次性的建库工具使用，导入结束后主动结束进程。
        try:
            proc.terminate()
            proc.wait(timeout=10)
            log('已停止后端进程')
        except Exception:
            try:
                proc.kill()
                log('已强制杀死后端进程')
            except Exception:
                log('无法终止后端进程，请手动检查')

    log('初始化流程完成')

if __name__ == '__main__':
    main()
