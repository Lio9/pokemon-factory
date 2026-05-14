#!/usr/bin/env python3
"""
Pokemon Factory 数据库自动备份脚本

功能：
1. 使用 SQLite 在线备份 API（避免文件锁定问题）
2. 验证备份完整性
3. 清理旧备份（保留最近 30 天）
4. 支持上传到云存储（可选）

使用方法：
    python backup_db.py [--db-path PATH] [--backup-dir DIR] [--retention-days DAYS]
"""

import argparse
import shutil
import sqlite3
import sys
from datetime import datetime, timedelta
from pathlib import Path


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='Pokemon Factory 数据库备份工具')
    parser.add_argument(
        '--db-path',
        type=str,
        default='pokemon-factory.db',
        help='数据库文件路径 (默认: pokemon-factory.db)'
    )
    parser.add_argument(
        '--backup-dir',
        type=str,
        default='backups',
        help='备份目录 (默认: backups)'
    )
    parser.add_argument(
        '--retention-days',
        type=int,
        default=30,
        help='备份保留天数 (默认: 30)'
    )
    return parser.parse_args()


def backup_database(db_path: Path, backup_dir: Path) -> Path:
    """
    创建数据库备份
    
    Args:
        db_path: 源数据库路径
        backup_dir: 备份目录
        
    Returns:
        备份文件路径
    """
    backup_dir.mkdir(parents=True, exist_ok=True)
    
    # 生成带时间戳的备份文件名
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_path = backup_dir / f"pokemon-factory_{timestamp}.db"
    
    print(f"📦 开始备份数据库...")
    print(f"   源文件: {db_path}")
    print(f"   目标文件: {backup_path}")
    
    try:
        # 使用 SQLite 在线备份 API（避免文件锁定问题）
        source_conn = sqlite3.connect(str(db_path))
        backup_conn = sqlite3.connect(str(backup_path))
        
        # 执行备份
        source_conn.backup(backup_conn)
        
        # 关闭连接
        source_conn.close()
        backup_conn.close()
        
        print(f"✅ 备份创建成功")
        return backup_path
        
    except Exception as e:
        print(f"❌ 备份失败: {e}")
        # 清理失败的备份文件
        if backup_path.exists():
            backup_path.unlink()
        raise


def verify_backup(backup_path: Path):
    """
    验证备份数据库完整性
    
    Args:
        backup_path: 备份文件路径
        
    Raises:
        Exception: 如果验证失败
    """
    print(f"🔍 验证备份完整性...")
    
    try:
        conn = sqlite3.connect(str(backup_path))
        cursor = conn.execute("PRAGMA integrity_check")
        result = cursor.fetchone()[0]
        conn.close()
        
        if result != "ok":
            raise Exception(f"备份验证失败: {result}")
        
        print(f"✅ 备份完整性验证通过")
        
    except Exception as e:
        print(f"❌ 备份验证失败: {e}")
        raise


def cleanup_old_backups(backup_dir: Path, retention_days: int):
    """
    清理超过保留期的旧备份
    
    Args:
        backup_dir: 备份目录
        retention_days: 保留天数
    """
    print(f"🧹 清理 {retention_days} 天前的旧备份...")
    
    if not backup_dir.exists():
        print(f"   备份目录不存在，跳过清理")
        return
    
    cutoff_date = datetime.now() - timedelta(days=retention_days)
    deleted_count = 0
    
    for backup_file in backup_dir.glob("pokemon-factory_*.db"):
        # 从文件名提取时间戳
        try:
            timestamp_str = backup_file.stem.replace("pokemon-factory_", "")
            file_date = datetime.strptime(timestamp_str, '%Y%m%d_%H%M%S')
            
            if file_date < cutoff_date:
                backup_file.unlink()
                print(f"   已删除: {backup_file.name}")
                deleted_count += 1
                
        except (ValueError, OSError) as e:
            print(f"   警告: 无法处理文件 {backup_file.name}: {e}")
    
    print(f"✅ 清理完成，共删除 {deleted_count} 个旧备份")


def get_backup_stats(backup_dir: Path):
    """获取备份统计信息"""
    if not backup_dir.exists():
        return 0, 0
    
    backups = list(backup_dir.glob("pokemon-factory_*.db"))
    total_count = len(backups)
    total_size = sum(f.stat().st_size for f in backups)
    
    return total_count, total_size


def main():
    """主函数"""
    args = parse_args()
    
    db_path = Path(args.db_path).resolve()
    backup_dir = Path(args.backup_dir).resolve()
    
    print("=" * 60)
    print("Pokemon Factory 数据库备份工具")
    print("=" * 60)
    print()
    
    # 检查源数据库是否存在
    if not db_path.exists():
        print(f"❌ 错误: 数据库文件不存在: {db_path}")
        sys.exit(1)
    
    try:
        # 1. 创建备份
        backup_path = backup_database(db_path, backup_dir)
        
        # 2. 验证备份
        verify_backup(backup_path)
        
        # 3. 清理旧备份
        cleanup_old_backups(backup_dir, args.retention_days)
        
        # 4. 显示统计信息
        count, size = get_backup_stats(backup_dir)
        size_mb = size / (1024 * 1024)
        
        print()
        print("=" * 60)
        print(f"📊 备份统计:")
        print(f"   备份总数: {count}")
        print(f"   总大小: {size_mb:.2f} MB")
        print(f"   最新备份: {backup_path.name}")
        print("=" * 60)
        print()
        print("✅ 备份流程完成！")
        
    except Exception as e:
        print()
        print("=" * 60)
        print(f"❌ 备份失败: {e}")
        print("=" * 60)
        sys.exit(1)


if __name__ == "__main__":
    main()

