# init_db 文件说明
# 所属模块：项目脚本目录。
# 文件类型：Python 脚本文件。
# 核心职责：负责数据库初始化、辅助启动或项目维护类自动化任务。
# 阅读建议：建议执行前先确认输入路径与副作用范围。
# 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。

"""初始化数据库流程：删除旧 SQLite、启动 common 触发数据库初始化与 CSV 导入、验证数据。
使用环境变量：
  SQLITE_DB_PATH - sqlite 文件路径（可选）
  JAR_PATH - 后端 jar 路径（可选）
  MIGRATION_TIMEOUT - 等待数据库初始化完成的超时时间（秒，默认 600）

运行示例：
    python scripts/init_db.py

脚本会启动 java -jar <JAR_PATH>，等待 common 完成建表与 CSV 导入，
之后运行 scripts/verify_sqlite.py 验证。
"""
from pathlib import Path
import os
import sys
import time
import sqlite3
import shutil
import subprocess
from collections import deque
from datetime import datetime
from shutil import which

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
    return ROOT.joinpath('pokemon-factory-backend','common','target','common-0.0.1-SNAPSHOT-exec.jar')


def common_sources_newer_than_jar(jar_path: Path):
    """判断 common 代码或资源是否比当前 jar 更新。"""
    if not jar_path.exists():
        return True
    jar_mtime = jar_path.stat().st_mtime
    common_root = ROOT.joinpath('pokemon-factory-backend', 'common')
    watch_paths = [
        common_root.joinpath('pom.xml'),
        common_root.joinpath('src', 'main', 'java'),
        common_root.joinpath('src', 'main', 'resources'),
    ]
    for path in watch_paths:
        if not path.exists():
            continue
        if path.is_file() and path.stat().st_mtime > jar_mtime:
            return True
        if path.is_dir():
            for child in path.rglob('*'):
                if child.is_file() and child.stat().st_mtime > jar_mtime:
                    return True
    return False


def ensure_common_jar(jar_path: Path):
    """确保 common 的 jar 为最新构建产物。"""
    if not common_sources_newer_than_jar(jar_path):
        log('common jar 已是最新，跳过构建')
        return True

    log('检测到 common jar 缺失或已过期，开始重新构建...')
    backend_root = ROOT.joinpath('pokemon-factory-backend')
    maven_command = resolve_maven_command()
    result = subprocess.run(
        maven_command + ['-pl', 'common', '-DskipTests', 'package'],
        cwd=str(backend_root)
    )
    if result.returncode != 0:
        log(f'错误: 构建 common jar 失败，退出码 {result.returncode}')
        return False
    if not jar_path.exists():
        log('错误: Maven 构建完成后仍未找到 common jar')
        return False
    return True


def resolve_maven_command():
    """解析当前环境下可用的 Maven 命令。"""
    for candidate in ('mvn.cmd', 'mvn.bat', 'mvn'):
        resolved = which(candidate)
        if resolved:
            return [resolved]
    if os.name == 'nt':
        return ['cmd', '/c', 'mvn']
    return ['mvn']


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


def wait_for_initialization(db_path: Path, timeout: int=120):
    """轮询 common 初始化结果，直到核心表和关键 CSV 数据都已写入。"""
    required_tables = {'generation', 'move', 'pokemon_species', 'player', 'battle', 'app_user'}
    required_counts = {'pokemon_species': 1, 'move': 1, 'item': 1}
    log(f"等待 common 完成数据库初始化与 CSV 导入，超时 {timeout}s...")
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
                    counts_ready = True
                    for table_name, minimum in required_counts.items():
                        cur.execute(f"SELECT COUNT(*) FROM {table_name}")
                        if cur.fetchone()[0] < minimum:
                            counts_ready = False
                            break
                    if counts_ready:
                        conn.close()
                        return True
                if missing_tables:
                    log(f"仍缺少数据表: {missing_tables}")
                else:
                    log("表结构已创建，等待 CSV 数据导入完成...")
                conn.close()
            except sqlite3.Error as e:
                log(f"打开 sqlite 文件失败: {e}")
        time.sleep(1)
    return False


def run_verify(py: str, root: Path):
    """在 common 完成建库与导入后执行数据校验。"""
    verify_script = root.joinpath('scripts','verify_sqlite.py')

    env = os.environ.copy()

    if verify_script.exists():
        log("运行数据校验脚本 (verify_sqlite.py) ...")
        r2 = subprocess.run([py, str(verify_script)], env=env, cwd=str(root))
        if r2.returncode != 0:
            log(f"校验脚本返回非零退出码: {r2.returncode}")
            return False
    else:
        log("未找到 verify_sqlite.py，跳过校验")

    return True


def tail_text_file(path: Path, max_lines: int = 80):
    """读取日志文件尾部，便于初始化失败时快速定位根因。"""
    if not path.exists():
        return []
    lines = deque(maxlen=max_lines)
    with path.open('r', encoding='utf-8', errors='replace') as handle:
        for line in handle:
            lines.append(line.rstrip())
    return list(lines)


def dump_common_log_tail(log_path: Path, title: str):
    tail_lines = tail_text_file(log_path)
    if not tail_lines:
        log(f"{title}: 未捕获到 common 日志")
        return
    log(title)
    print('-' * 80)
    for line in tail_lines:
        print(line)
    print('-' * 80)


def main():
    """串起完整的本地初始化流程：建库、导入、校验、清理进程。"""
    jar = find_jar()
    db = find_db()
    timeout = int(os.getenv('MIGRATION_TIMEOUT', '600'))

    log(f"项目根目录: {ROOT}")
    log(f"使用 JAR: {jar}")
    log(f"使用 SQLite DB: {db}")

    if not ensure_common_jar(jar):
        sys.exit(2)

    # 这里保留“备份后重建”的策略，是为了让导入脚本始终从可预期的干净结构开始跑。
    backup_and_remove(db)

    # 即使数据库文件不存在，也要先确保目录存在，避免 common 因路径缺失无法创建 sqlite。
    db.parent.mkdir(parents=True, exist_ok=True)

    # 启动 common Jar，统一初始化数据库
    env = os.environ.copy()
    env['SQLITE_DB_PATH'] = str(db)
    common_log_path = ROOT.joinpath('pokemon-factory-backend', 'common-init.log')
    common_log_path.parent.mkdir(parents=True, exist_ok=True)
    if common_log_path.exists():
        common_log_path.unlink()
    log(f'启动 common Jar 以初始化数据库，日志输出到: {common_log_path}')
    log_handle = common_log_path.open('w', encoding='utf-8')
    proc = subprocess.Popen(['java', '-jar', str(jar)], cwd=str(ROOT), env=env, stdout=log_handle, stderr=subprocess.STDOUT)
    log(f'common 进程 PID={proc.pid}')

    try:
        ok = wait_for_initialization(db, timeout=timeout)
        if not ok:
            log('错误: common 数据库初始化或 CSV 导入未在超时时间内完成')
            dump_common_log_tail(common_log_path, '超时前的 common 日志尾部如下')
            # 尝试输出简单提示并退出非零
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(3)

        log('数据库结构与 CSV 数据导入完成，开始校验...')
        py = sys.executable
        success = run_verify(py, ROOT)
        if not success:
            log('错误: 数据校验失败')
            dump_common_log_tail(common_log_path, '校验失败时的 common 日志尾部如下')
            proc.terminate()
            proc.wait(timeout=10)
            sys.exit(4)

        log('数据初始化与校验完成')

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
        finally:
            log_handle.close()

    log('初始化流程完成')

if __name__ == '__main__':
    main()
