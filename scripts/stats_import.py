#!/usr/bin/env python3
"""
种族值数据导入脚本 - 支持本地文件导入
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
import os
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_file = os.path.join(project_root, "logs", "stats_import.log")
logger = init_logger(log_file)

class StatsImporter:
    """种族值导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.pokemon_dir = os.path.join(project_root, "data", "local", "pokemon")
        os.makedirs(self.pokemon_dir, exist_ok=True)
    
    async def import_all_stats(self):
        """导入所有种族值数据"""
        logger.info("📈 开始导入所有种族值数据...")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 清空表
                await self.clear_stats()
                
                # 获取所有种族值ID列表
                stats_ids = await self.get_all_stats_ids(session)
                if not stats_ids:
                    logger.error("无法获取种族值ID列表")
                    return False
                
                total = len(stats_ids)
                logger.info(f"总共 {total} 个种族值，开始导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, 100):  # 100个种族值一批
                    batch = stats_ids[i : i + 100]
                    logger.info(f"处理批次 {i//100 + 1}/{(total + 99)//100}: 种族值 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的种族值数据
                    batch_results = await self.process_stats_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入种族值失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有种族值数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                # 验证导入的数据
                logger.info("🔍 验证导入的种族值数据...")
                from data_validator import validate_import
                validation_result = validate_import("stats")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 种族值数据验证通过")
                    return True
                else:
                    logger.error("❌ 种族值数据验证失败")
                    return False
                
        except Exception as e:
            logger.error(f"导入种族值数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=1025):
        """从本地JSON文件导入种族值数据"""
        logger.info(f"📈 从本地导入种族值数据: ID {start_id}-{end_id}")
        
        # 检查本地数据目录
        if not os.path.exists(self.pokemon_dir):
            logger.error(f"本地数据目录不存在: {self.pokemon_dir}")
            return False
        
        # 清空表
        await self.clear_stats()
        
        # 导入本地数据
        successful_imports = 0
        failed_imports = 0
        
        for i in range(start_id, min(end_id + 1, 1026)):
            try:
                # 构建文件路径
                filename = f"pokemon_{i:04d}.json"
                filepath = os.path.join(self.pokemon_dir, filename)
                
                # 检查文件是否存在
                if not os.path.exists(filepath):
                    logger.warning(f"本地文件不存在: {filepath}")
                    failed_imports += 1
                    continue
                
                # 读取本地JSON文件
                with open(filepath, 'r', encoding='utf-8') as f:
                    pokemon_data = json.load(f)
                
                # 转换并导入数据
                stats = self.convert_stats_data(pokemon_data, i)
                if stats:
                    await self.batch_insert_stats([stats])
                    successful_imports += 1
                else:
                    failed_imports += 1
                    logger.error(f"转换种族值数据失败: {i}")
                
                if (i - start_id + 1) % 100 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个宝可梦的种族值")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入种族值 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0
    
    async def get_all_stats_ids(self, session):
        """获取所有种族值ID列表"""
        try:
            total = 1025
            logger.info(f"总共 {total} 个种族值")
            
            # 直接生成1-1025的ID列表
            all_ids = list(range(1, total + 1))
            logger.info(f"使用所有 {len(all_ids)} 个种族值ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取种族值ID列表失败: {e}")
            return list(range(1, 1026))
    
    async def process_stats_batch(self, session, stats_ids):
        """处理一批种族值"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for stats_id in stats_ids:
            task = self.import_single_stats(session, stats_id, semaphore)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def import_single_stats(self, session, stats_id, semaphore):
        """导入单个种族值"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取种族值数据
                    pokemon_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}pokemon/{stats_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    
                    if not pokemon_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(f"种族值 {stats_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                            await asyncio.sleep(wait_time)
                            continue
                        if retry == self.max_retries - 1:
                            logger.warning(f"种族值 {stats_id} 数据为空，跳过导入")
                        return False
                    
                    # 转换数据
                    stats = self.convert_stats_data(pokemon_data, stats_id)
                    if not stats:
                        logger.error(f"种族值 {stats_id} 数据转换失败")
                        return False
                    
                    # 批量插入数据库
                    await self.batch_insert_stats([stats])
                    return True
                    
                except Exception as e:
                    logger.error(f"种族值导入失败: {stats_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(f"种族值 {stats_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                        await asyncio.sleep(wait_time)
                    else:
                        return e
            
            logger.error(f"种族值 {stats_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False
    
    def convert_stats_data(self, pokemon_data, pokemon_id):
        """转换种族值数据"""
        try:
            # 获取种族值数据
            stats = pokemon_data.get('stats', [])
            
            hp = attack = defense = special_attack = special_defense = speed = 0
            
            for stat in stats:
                stat_name = stat.get('stat', {}).get('name', '')
                base_stat = stat.get('base_stat', 0)
                
                if stat_name == 'hp':
                    hp = base_stat
                elif stat_name == 'attack':
                    attack = base_stat
                elif stat_name == 'defense':
                    defense = base_stat
                elif stat_name == 'special-attack':
                    special_attack = base_stat
                elif stat_name == 'special-defense':
                    special_defense = base_stat
                elif stat_name == 'speed':
                    speed = base_stat
            
            return {
                "form_id": pokemon_id,
                "hp": hp,
                "attack": attack,
                "defense": defense,
                "special_attack": special_attack,
                "special_defense": special_defense,
                "speed": speed,
                "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                "updated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            }
        except Exception as e:
            logger.error(f"转换种族值数据失败: {e}")
            return None
    
    async def batch_insert_stats(self, stats):
        """批量插入种族值数据"""
        conn = None
        cursor = None
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor(dictionary=True)
            
            insert_sql = """
                INSERT IGNORE INTO pokemon_stats 
                (form_id, hp, attack, defense, special_attack, special_defense, speed, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            for stat in stats:
                values = (
                    stat["form_id"],
                    stat["hp"],
                    stat["attack"],
                    stat["defense"],
                    stat["special_attack"],
                    stat["special_defense"],
                    stat["speed"],
                    stat["created_at"],
                    stat["updated_at"],
                )
                cursor.execute(insert_sql, values)
            
            conn.commit()
            logger.debug(f"成功插入 {len(stats)} 个种族值")
            
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入种族值数据失败: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    async def clear_stats(self):
        """清空种族值表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空种族值表
            cursor.execute("TRUNCATE TABLE pokemon_stats")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 pokemon_stats 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon_stats 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = StatsImporter()
    
    # 检查是否需要从本地导入
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(project_root, "data", "local", "pokemon")
    
    if os.path.exists(data_dir) and len(os.listdir(data_dir)) > 0:
        logger.info("检测到本地数据，使用本地导入模式")
        await importer.import_from_local_data()
    else:
        logger.info("未检测到本地数据，使用网络导入模式")
        await importer.import_all_stats()

if __name__ == "__main__":
    asyncio.run(main())