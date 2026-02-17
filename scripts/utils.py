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
def setup_logging(log_file="logs\\import.log"):
    """设置日志配置"""
    # 确保在项目根目录创建logs文件夹
    import os
    if not os.path.isabs(log_file):
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        log_file = os.path.join(project_root, log_file)
    
    # 确保日志目录存在
    log_dir = os.path.dirname(log_file)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir, exist_ok=True)
    
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
        handlers=[logging.FileHandler(log_file), logging.StreamHandler(sys.stdout)]
    )
    return logging.getLogger(__name__)


def get_db_config():
    """获取数据库配置"""
    return {
        "host": "10.144.55.168",
        "port": 3306,
        "user": "root",
        "password": "753951",
        "database": "pokemon_factory",
        "charset": "utf8mb4",
        "connection_timeout": 30,
        "connect_timeout": 30,
    }


def get_default_db_config():
    """获取默认数据库配置"""
    return {
        "host": "10.144.55.168",
        "port": 3306,
        "user": "root",
        "password": "753951",
        "database": "pokemon_factory",
        "charset": "utf8mb4",
        "connection_timeout": 30,
        "connect_timeout": 30,
    }


def get_pokeapi_session(max_concurrent=50):
    """获取PokeAPI会话配置"""
    return aiohttp.TCPConnector(
        limit=max_concurrent,
        limit_per_host=10,
        ttl_dns_cache=300,
        use_dns_cache=True,
    ), aiohttp.ClientTimeout(total=60)


async def fetch_with_retry(session, url, max_retries=8, timeout=60):
    """
    带重试机制的网络请求函数
    
    重试策略：
    - 成功状态码(200)：直接返回数据
    - 429限制：指数退避延迟后重试
    - 500+错误：指数退避延迟后重试
    - 其他错误：记录错误但不重试
    
    指数退避延迟计算：delay = random.uniform(1, 2) * (2 ** attempt)
    
    参数：
        session: aiohttp客户端会话
        url: 请求的URL地址
        max_retries: 最大重试次数（默认8次）
        timeout: 请求超时时间（默认60秒）
    
    返回值：
        dict: API返回的JSON数据，失败时返回None
    """
    import random

    # 定义镜像源列表
    mirror_sources = [
        "https://pokeapi.co/api/v2/",  # 主源
        "https://pokeapi.cn/api/v2/",  # 国内镜像
        "https://pokedex-api.vercel.app/api/v2/",  # 备用镜像
        "https://pokeapi.fly.dev/api/v2/",  # 备用镜像
    ]

    # 直接使用直连
    for attempt in range(max_retries):
        try:
            # 创建直连会话
            timeout_config = aiohttp.ClientTimeout(total=timeout)  # 使用原始超时时间

            async with session.get(url, timeout=timeout_config) as response:
                if response.status == 200:
                    logging.debug(f"使用代理成功获取数据: {url}")
                    return await response.json()
                elif response.status == 429:
                    # 429 Too Many Requests，等待后重试
                    wait_time = random.uniform(1, 2) * (2**attempt)
                    logging.warning(
                        f"请求 {url} 被限制，等待 {wait_time:.1f} 秒后重试..."
                    )
                    await asyncio.sleep(wait_time)
                    continue
                elif response.status >= 500:
                    # 服务器错误，增加延迟后重试
                    wait_time = random.uniform(2, 3) * (2**attempt)
                    logging.warning(
                        f"服务器错误，状态码: {response.status}，等待 {wait_time:.1f} 秒后重试..."
                    )
                    await asyncio.sleep(wait_time)
                    continue
                else:
                    logging.error(f"请求 {url} 失败，状态码: {response.status}")
                    if attempt < max_retries - 1:
                        wait_time = random.uniform(1, 2) * (2**attempt)
                        logging.warning(
                            f"请求 {url} 失败，等待 {wait_time:.1f} 秒后重试..."
                        )
                        await asyncio.sleep(wait_time)
                        continue
                    else:
                        logging.error(f"请求 {url} 在 {max_retries} 次尝试后仍然失败")
                        break
        except asyncio.TimeoutError:
            logging.error(f"代理请求 {url} 超时 (尝试 {attempt + 1}/{max_retries})")
            if attempt < max_retries - 1:
                await asyncio.sleep(random.uniform(1, 2) * (2**attempt))
                continue
            else:
                logging.error(f"代理请求 {url} 在 {max_retries} 次尝试后仍然失败")
                break
        except Exception as e:
            logging.warning(f"代理请求 {url} 失败: {e}，尝试直连...")

            # 代理失败，尝试直连
            try:
                timeout_config = aiohttp.ClientTimeout(
                    total=timeout * 2
                )  # 增加超时时间
                async with session.get(url, timeout=timeout_config) as response:
                    if response.status == 200:
                        logging.debug(f"直连成功获取数据: {url}")
                        return await response.json()
                    elif response.status == 429:
                        wait_time = random.uniform(1, 2) * (2**attempt)
                        logging.warning(
                            f"直连请求 {url} 被限制，等待 {wait_time:.1f} 秒后重试..."
                        )
                        await asyncio.sleep(wait_time)
                        continue
                    elif response.status >= 500:
                        wait_time = random.uniform(2, 3) * (2**attempt)
                        logging.warning(
                            f"直连服务器错误，状态码: {response.status}，等待 {wait_time:.1f} 秒后重试..."
                        )
                        await asyncio.sleep(wait_time)
                        continue
                    else:
                        logging.error(f"直连请求 {url} 失败，状态码: {response.status}")
                        break
            except asyncio.TimeoutError:
                logging.error(f"直连请求 {url} 超时 (尝试 {attempt + 1}/{max_retries})")
                if attempt < max_retries - 1:
                    await asyncio.sleep(random.uniform(1, 2) * (2**attempt))
                else:
                    break
            except Exception as e2:
                logging.error(f"直连请求 {url} 异常: {e2}")
                if attempt < max_retries - 1:
                    await asyncio.sleep(random.uniform(1, 2) * (2**attempt))
                else:
                    break

    return None


class DatabaseManager:
    """数据库管理器"""

    def __init__(self, db_config=None):
        self.db_config = db_config or get_default_db_config()
        self.logger = logging.getLogger(__name__)
        self.connection_pool = []
        self.max_pool_size = 20
        self.current_pool_size = 0

    async def clear_database(self, tables_to_clear=None):
        """清空数据库表"""
        self.logger.info("🧹 开始清空数据库表...")

        if tables_to_clear is None:
            tables_to_clear = [
                "pokemon_form_type",
                "pokemon_form_ability",
                "pokemon_move",
                "pokemon_egg_group",
                "evolution_chain",
                "pokemon_stats",
                "pokemon_iv",
                "pokemon_ev",
                "pokemon_form",
                "pokemon",
                "move",
                "ability",
                "type",
                "egg_group",
                "growth_rate",
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
            reset_tables = ["pokemon", "pokemon_form", "ability", "move", "type"]
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
        """获取数据库连接，使用连接池"""
        if self.current_pool_size > 0 and self.connection_pool:
            # 从池中获取连接
            conn = self.connection_pool.pop()
            self.current_pool_size -= 1

            # 验证连接是否仍然有效
            try:
                if conn.is_connected():
                    # 设置 autocommit
                    conn.autocommit = True
                    return conn
                else:
                    conn.close()
            except:
                try:
                    conn.close()
                except:
                    pass

        # 创建新连接
        try:
            # 创建连接副本，避免修改原始配置
            conn_config = self.db_config.copy()
            # 移除可能与连接池冲突的参数
            conn_config.pop("connection_timeout", None)
            conn_config.pop("connect_timeout", None)

            conn = mysql.connector.connect(**conn_config)

            # 设置 autocommit
            conn.autocommit = True

            self.logger.debug("创建新的数据库连接")
            return conn
        except Exception as e:
            self.logger.error(f"创建数据库连接失败: {e}")
            raise

    async def release_connection(self, conn):
        """释放数据库连接回池"""
        if conn and conn.is_connected():
            if self.current_pool_size < self.max_pool_size:
                self.connection_pool.append(conn)
                self.current_pool_size += 1
                self.logger.debug(
                    f"连接已返回池中，当前池大小: {self.current_pool_size}"
                )
            else:
                try:
                    conn.close()
                    self.logger.debug("连接池已满，关闭连接")
                except:
                    pass
        else:
            try:
                if conn:
                    conn.close()
            except:
                pass

    def get_logger(self):
        """获取日志记录器"""
        return self.logger


# 全局日志记录器
logger = None


def init_logger(log_file="logs\\import.log"):
    """初始化日志记录器"""
    # 确保在项目根目录创建logs文件夹
    import os
    if not os.path.isabs(log_file):
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        log_file = os.path.join(project_root, log_file)
    
    global logger
    logger = setup_logging(log_file)
    return logger


# 初始化默认日志
init_logger()
