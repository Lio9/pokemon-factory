#!/usr/bin/env python3
"""
蛋群关联数据导入脚本 - 支持本地文件导入
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
log_file = os.path.join(project_root, "logs", "egg_group_import.log")
logger = init_logger(log_file)

class EggGroupImporter:
    """蛋群关联导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.species_dir = os.path.join(project_root, "data", "local", "species")
        os.makedirs(self.species_dir, exist_ok=True)
    
    async def import_all_egg_groups(self):
        """导入所有蛋群关联数据"""
        logger.info("🥚 开始导入所有蛋群关联数据...")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 清空表
                await self.clear_egg_groups()
                
                # 获取所有蛋群关联ID列表
                egg_group_ids = await self.get_all_egg_group_ids(session)
                if not egg_group_ids:
                    logger.error("无法获取蛋群关联ID列表")
                    return False
                
                total = len(egg_group_ids)
                logger.info(f"总共 {total} 个蛋群关联，开始导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, 100):  # 100个蛋群关联一批
                    batch = egg_group_ids[i : i + 100]
                    logger.info(f"处理批次 {i//100 + 1}/{(total + 99)//100}: 蛋群关联 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的蛋群关联数据
                    batch_results = await self.process_egg_group_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入蛋群关联失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有蛋群关联数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                # 验证导入的数据
                logger.info("🔍 验证导入的蛋群关联数据...")
                from data_validator import validate_import
                validation_result = validate_import("egg_group")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 蛋群关联数据验证通过")
                    return True
                else:
                    logger.error("❌ 蛋群关联数据验证失败")
                    return False
                
        except Exception as e:
            logger.error(f"导入蛋群关联数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=1025):
        """从本地JSON文件导入蛋群关联数据"""
        logger.info(f"🥚 从本地导入蛋群关联数据: ID {start_id}-{end_id}")
        
        # 检查本地数据目录
        if not os.path.exists(self.species_dir):
            logger.error(f"本地数据目录不存在: {self.species_dir}")
            return False
        
        # 清空表
        await self.clear_egg_groups()
        
        # 导入本地数据
        successful_imports = 0
        failed_imports = 0
        
        for i in range(start_id, min(end_id + 1, 1026)):
            try:
                # 构建文件路径
                filename = f"species_{i:04d}.json"
                filepath = os.path.join(self.species_dir, filename)
                
                # 检查文件是否存在
                if not os.path.exists(filepath):
                    logger.warning(f"本地文件不存在: {filepath}")
                    failed_imports += 1
                    continue
                
                # 读取本地JSON文件
                with open(filepath, 'r', encoding='utf-8') as f:
                    species_data = json.load(f)
                
                # 转换并导入数据
                egg_groups = self.convert_egg_group_data(species_data, i)
                if egg_groups:
                    await self.batch_insert_egg_groups(egg_groups)
                    successful_imports += len(egg_groups)
                else:
                    failed_imports += 1
                    logger.error(f"转换蛋群关联数据失败: {i}")
                
                if (i - start_id + 1) % 100 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个宝可梦物种的蛋群关联")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入蛋群关联 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0
    
    async def get_all_egg_group_ids(self, session):
        """获取所有蛋群关联ID列表"""
        try:
            total = 1025
            logger.info(f"总共 {total} 个蛋群关联")
            
            # 直接生成1-1025的ID列表
            all_ids = list(range(1, total + 1))
            logger.info(f"使用所有 {len(all_ids)} 个蛋群关联ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取蛋群关联ID列表失败: {e}")
            return list(range(1, 1026))
    
    async def process_egg_group_batch(self, session, egg_group_ids):
        """处理一批蛋群关联"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for egg_group_id in egg_group_ids:
            task = self.import_single_egg_group(session, egg_group_id, semaphore)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def import_single_egg_group(self, session, egg_group_id, semaphore):
        """导入单个蛋群关联"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取蛋群关联数据
                    species_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}pokemon-species/{egg_group_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    
                    if not species_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(f"蛋群关联 {egg_group_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                            await asyncio.sleep(wait_time)
                            continue
                        if retry == self.max_retries - 1:
                            logger.warning(f"蛋群关联 {egg_group_id} 数据为空，跳过导入")
                        return False
                    
                    # 转换数据
                    egg_groups = self.convert_egg_group_data(species_data, egg_group_id)
                    if not egg_groups:
                        logger.error(f"蛋群关联 {egg_group_id} 数据转换失败")
                        return False
                    
                    # 批量插入数据库
                    await self.batch_insert_egg_groups(egg_groups)
                    return True
                    
                except Exception as e:
                    logger.error(f"蛋群关联导入失败: {egg_group_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(f"蛋群关联 {egg_group_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                        await asyncio.sleep(wait_time)
                    else:
                        return e
            
            logger.error(f"蛋群关联 {egg_group_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False
    
    def convert_egg_group_data(self, species_data, pokemon_id):
        """转换蛋群关联数据"""
        try:
            egg_groups = []
            
            # 获取蛋群数据
            egg_groups_list = species_data.get('egg_groups', [])
            
            for egg_group in egg_groups_list:
                egg_group_name = egg_group.get('name', '')
                
                # 获取蛋群ID
                egg_group_id = None
                if egg_group_name:
                    # 连接数据库获取蛋群ID
                    conn = mysql.connector.connect(**self.db_config)
                    cursor = conn.cursor(dictionary=True)
                    cursor.execute(f"SELECT id FROM egg_group WHERE name_en = '{egg_group_name}'")
                    egg_group_result = cursor.fetchone()
                    if egg_group_result:
                        egg_group_id = egg_group_result['id']
                    cursor.close()
                    conn.close()
                
                if egg_group_id:
                    egg_groups.append({
                        "pokemon_id": pokemon_id,
                        "egg_group_id": egg_group_id,
                        "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                        "updated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                    })
            
            return egg_groups
        except Exception as e:
            logger.error(f"转换蛋群关联数据失败: {e}")
            return None
    
    async def batch_insert_egg_groups(self, egg_groups):
        """批量插入蛋群关联数据"""
        conn = None
        cursor = None
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor(dictionary=True)
            
            insert_sql = """
                INSERT IGNORE INTO pokemon_egg_group 
                (pokemon_id, egg_group_id, created_at, updated_at)
                VALUES (%s, %s, %s, %s)
            """
            
            for egg_group in egg_groups:
                values = (
                    egg_group["pokemon_id"],
                    egg_group["egg_group_id"],
                    egg_group["created_at"],
                    egg_group["updated_at"],
                )
                cursor.execute(insert_sql, values)
            
            conn.commit()
            logger.debug(f"成功插入 {len(egg_groups)} 个蛋群关联")
            
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入蛋群关联数据失败: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    async def clear_egg_groups(self):
        """清空蛋群关联表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空蛋群关联表
            cursor.execute("TRUNCATE TABLE pokemon_egg_group")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 pokemon_egg_group 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon_egg_group 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = EggGroupImporter()
    
    # 检查是否需要从本地导入
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(project_root, "data", "local", "species")
    
    if os.path.exists(data_dir) and len(os.listdir(data_dir)) > 0:
        logger.info("检测到本地数据，使用本地导入模式")
        await importer.import_from_local_data()
    else:
        logger.info("未检测到本地数据，使用网络导入模式")
        await importer.import_all_egg_groups()

if __name__ == "__main__":
    asyncio.run(main())