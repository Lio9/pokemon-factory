import mysql.connector
import logging
import sys
import os

# 添加上级目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.utils import setup_logging, get_db_config

logger = logging.getLogger(__name__)

class DatabaseInitializer:
    def __init__(self):
        self.db_config = get_db_config()
    
    def initialize_database(self):
        """初始化数据库表结构"""
        logger.info("开始初始化数据库...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 读取初始化SQL文件
            init_sql_path = os.path.join(os.path.dirname(__file__), '..', 'complete_database_init.sql')
            with open(init_sql_path, 'r', encoding='utf-8') as f:
                sql_content = f.read()
            
            # 分割SQL语句并执行
            sql_statements = [stmt.strip() for stmt in sql_content.split(';') if stmt.strip()]
            
            for i, statement in enumerate(sql_statements):
                try:
                    cursor.execute(statement)
                    logger.debug(f"执行SQL语句 {i+1}: {statement[:50]}...")
                except mysql.connector.Error as e:
                    if e.errno != 1050:  # 表已存在错误
                        logger.error(f"执行SQL语句 {i+1} 失败: {e}")
                        raise
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info("数据库初始化完成")
            return True
            
        except Exception as e:
            logger.error(f"数据库初始化失败: {e}")
            return False
    
    def clear_tables(self):
        """清空所有表"""
        logger.info("开始清空数据库表...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取所有表名
            cursor.execute("SHOW TABLES")
            tables = cursor.fetchall()
            
            # 逐个清空表
            for table in tables:
                table_name = table[0]
                try:
                    cursor.execute(f"DELETE FROM {table_name}")
                    logger.info(f"已清空表: {table_name}")
                except Exception as e:
                    logger.error(f"清空表 {table_name} 失败: {e}")
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info("数据库表清空完成")
            return True
            
        except Exception as e:
            logger.error(f"清空数据库表失败: {e}")
            return False

async def main():
    setup_logging()
    initializer = DatabaseInitializer()
    
    import argparse
    parser = argparse.ArgumentParser(description='数据库初始化工具')
    parser.add_argument('--clear', action='store_true', help='清空所有表')
    parser.add_argument('--init', action='store_true', help='初始化数据库表结构')
    
    args = parser.parse_args()
    
    if args.clear:
        initializer.clear_tables()
    elif args.init:
        initializer.initialize_database()
    else:
        initializer.initialize_database()

if __name__ == "__main__":
    import asyncio
    asyncio.run(main())