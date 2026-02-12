#!/usr/bin/env python3
"""
工具函数模块
提供数据库连接、日志配置等通用功能
"""

import mysql.connector
import logging
import time
import asyncio
import aiohttp
from multiprocessing import cpu_count
import sys
import os

# 配置日志
def setup_logging(log_file="D:\\learn\\pokemon-factory\\logs\\import.log"):
    """设置日志配置"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file),
            logging.StreamHandler(sys.stdout)
        ]
    )
    return logging.getLogger(__name__)

def get_db_config():
    """获取数据库配置"""
    return {
        'host': '10.144.55.168',
        'port': 3306,
        'user': 'root',
        'password': '753951',
        'database': 'pokemon_factory',
        'charset': 'utf8mb4',
        'autocommit': True,
        'connection_timeout': 30,
        'connect_timeout': 30
    }

def get_default_db_config():
    """获取默认数据库配置"""
    return {
        'host': '10.144.55.168',
        'port': 3306,
        'user': 'root',
        'password': '753951',
        'database': 'pokemon_factory',
        'charset': 'utf8mb4',
        'autocommit': True,
        'connection_timeout': 30,
        'connect_timeout': 30
    }

def get_pokeapi_session(max_concurrent=50):
    """获取PokeAPI会话配置"""
    return aiohttp.TCPConnector(
        limit=max_concurrent,
        limit_per_host=10,
        ttl_dns_cache=300,
        use_dns_cache=True,
    ), aiohttp.ClientTimeout(total=60)

async def fetch_with_retry(session, url, max_retries=3, timeout=30):
    """带重试的网络请求"""
    for attempt in range(max_retries):
        try:
            timeout_config = aiohttp.ClientTimeout(total=timeout)
            async with session.get(url, timeout=timeout_config) as response:
                if response.status == 200:
                    return await response.json()
                elif response.status == 429:
                    # 429 Too Many Requests，等待后重试
                    wait_time = 2 ** attempt
                    logging.warning(f"请求 {url} 被限制，等待 {wait_time} 秒后重试...")
                    await asyncio.sleep(wait_time)
                    continue
                else:
                    logging.error(f"请求 {url} 失败，状态码: {response.status}")
                    return None
        except asyncio.TimeoutError:
            logging.error(f"请求 {url} 超时 (尝试 {attempt + 1}/{max_retries})")
            if attempt < max_retries - 1:
                await asyncio.sleep(2 ** attempt)
            else:
                return None
        except Exception as e:
            logging.error(f"请求 {url} 异常: {e}")
            if attempt < max_retries - 1:
                await asyncio.sleep(2 ** attempt)
            else:
                return None
    return None

class DatabaseManager:
    """数据库管理器"""
    
    def __init__(self, db_config=None):
        self.db_config = db_config or get_default_db_config()
        self.logger = logging.getLogger(__name__)
    
    async def clear_database(self, tables_to_clear=None):
        """清空数据库表"""
        self.logger.info("🧹 开始清空数据库表...")
        
        if tables_to_clear is None:
            tables_to_clear = [
                'pokemon_form_type', 'pokemon_form_ability', 'pokemon_move',
                'pokemon_egg_group', 'evolution_chain', 'pokemon_stats',
                'pokemon_iv', 'pokemon_ev', 'pokemon_form', 'pokemon',
                'move', 'ability', 'type', 'egg_group', 'growth_rate'
            ]
        
        try:
            conn = mysql.connector.connect(**self.db_config)
            conn.autocommit = False
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            for table in tables_to_clear:
                try:
                    cursor.execute(f"TRUNCATE TABLE {table}")
                    self.logger.info(f"清空表 {table} 完成")
                except Exception as e:
                    self.logger.warning(f"TRUNCATE表 {table} 失败，尝试DELETE: {e}")
                    cursor.execute(f"DELETE FROM {table}")
                    self.logger.info(f"清空表 {table} 完成")
            
            # 重置自增ID
            reset_tables = ['pokemon', 'pokemon_form', 'ability', 'move', 'type']
            for table in reset_tables:
                try:
                    cursor.execute(f"ALTER TABLE {table} AUTO_INCREMENT = 1")
                except:
                    pass
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            
            conn.commit()
            cursor.close()
            conn.close()
            
            self.logger.info("✅ 数据库表清空完成")
            return True
            
        except Exception as e:
            self.logger.error(f"清空数据库失败: {e}")
            try:
                if conn:
                    conn.rollback()
                    cursor.close()
                    conn.close()
            except:
                pass
            return False
    
    async def get_connection(self):
        """获取数据库连接"""
        return mysql.connector.connect(**self.db_config)
    
    def get_logger(self):
        """获取日志记录器"""
        return self.logger

# 全局日志记录器
logger = None

def init_logger(log_file="D:\\learn\\pokemon-factory\\logs\\import.log"):
    """初始化日志记录器"""
    global logger
    logger = setup_logging(log_file)
    return logger

# 初始化默认日志
init_logger()
