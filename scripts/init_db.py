#!/usr/bin/env python3
"""初始化数据库流程：删除旧 SQLite、启动后端触发 Flyway 迁移、导入 CSV、验证数据。
使用环境变量：
  SQLITE_DB_PATH - sqlite 文件路径（可选）
  JAR_PATH - 后端 jar 路径（可选）
  EXPECTED_MIGRATIONS - 期望的迁移数量（默认 3）
  MIGRATION_TIMEOUT - 等待迁移完成的超时时间（秒，默认 120）

运行示例：
  python scripts\init_db.py

脚本会启动 java -jar <JAR_PATH>，等待 flyway_schema_history 表出现并包含期望数量的迁移记录，
之后运行 scripts/import_to_sqlite.py 导入数据并运行 scripts/verify_sqlite.py 验证。
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

def log(msg):
    print(f"[init_db] {msg}")


def find_jar(default=None):
    jar = os.getenv('JAR_PATH')
    if jar:
        p = Path(jar)
        if not p.is_absolute():
            p = ROOT.joinpath(jar)
        return p
    if default:
        return Path(default)
    return ROOT.joinpath('pokemon-factory-backend','pokeDex','target','pokeDex-0.0.1-SNAPSHOT.jar')


def find_db(default=None):
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
    if db_path.exists():
        ts = datetime.now().strftime('%Y%m%d%H%M%S')
        bak = db_path.with_suffix(db_path.suffix + f'.bak.{ts}')
        shutil.copy2(db_path, bak)
        log(f"已备份旧数据库到: {bak}")
        db_path.unlink()
        log(f"已删除旧数据库: {db_path}")
    else:
        log("未发现已有 sqlite 文件，跳过删除步骤")


def wait_for_migrations(db_path: Path, expected: int=3, timeout: int=120):
    log(f"等待 Flyway 迁移完成（期望 {expected} 条），超时 {timeout}s...")
    start = time.time()
    while time.time() - start < timeout:
        if db_path.exists():
            try:
                conn = sqlite3.connect(str(db_path))
                cur = conn.cursor()
                cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='flyway_schema_history'")
                if cur.fetchone():
                    try:
                        cur.execute('SELECT COUNT(*) FROM flyway_schema_history')
                        n = cur.fetchone()[0]
                        log(f"flyway_schema_history 条目数: {n}")
                        if n >= expected:
                            conn.close()
                            return True
                    except sqlite3.DatabaseError as e:
                        # 可能数据库仍在写入中，继续轮询
                        log(f"查询 flyway_schema_history 时遇到临时错误: {e}")
                conn.close()
            except sqlite3.Error as e:
                log(f"打开 sqlite 文件失败: {e}")
        time.sleep(1)
    return False


def run_import_and_verify(py: str, root: Path):
    import_script = root.joinpath('scripts','import_to_sqlite.py')
    verify_script = root.joinpath('scripts','verify_sqlite.py')

    if not import_script.exists():
        log(f"找不到导入脚本: {import_script}")
        return False

    env = os.environ.copy()
    # 不跳过数据库初始化，确保导入脚本能在缺少表时自行创建（SKIP_DB_INIT=0）
    env['SKIP_DB_INIT'] = '0'

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
    jar = find_jar()
    db = find_db()
    expected = int(os.getenv('EXPECTED_MIGRATIONS', '3'))
    timeout = int(os.getenv('MIGRATION_TIMEOUT', '120'))

    log(f"项目根目录: {ROOT}")
    log(f"使用 JAR: {jar}")
    log(f"使用 SQLite DB: {db}")

    if not jar.exists():
        log("错误: 找不到后端 JAR，请先构建后端（mvn package）或设置环境变量 JAR_PATH 指向 jar 文件")
        sys.exit(2)

    # 备份并删除旧 sqlite
    backup_and_remove(db)

    # 确保目录存在
    db.parent.mkdir(parents=True, exist_ok=True)

    # 启动后端 Jar
    env = os.environ.copy()
    env['SQLITE_DB_PATH'] = str(db)
    log('启动后端 Jar 以触发 Flyway 迁移...')
    proc = subprocess.Popen(['java', '-Dspring.flyway.mixed=true', '-jar', str(jar)], cwd=str(ROOT), env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    log(f'后端进程 PID={proc.pid}')

    try:
        ok = wait_for_migrations(db, expected=expected, timeout=timeout)
        if not ok:
            log('错误: Flyway 迁移未在超时时间内完成')
            # 尝试输出简单提示并退出非零
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(3)

        log('迁移完成，开始导入数据...')
        py = sys.executable
        success = run_import_and_verify(py, ROOT)
        if not success:
            log('错误: 导入或校验失败')
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(4)

        log('数据导入与校验完成')

    finally:
        # 停止后端进程
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
