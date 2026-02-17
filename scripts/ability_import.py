#!/usr/bin/env python3
"""
特性数据导入脚本 - 支持本地文件导入
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
log_file = os.path.join(project_root, "logs", "ability_import.log")
logger = init_logger(log_file)

class AbilityImporter:
    """特性导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.data_dir = os.path.join(project_root, "data", "local", "ability")
        os.makedirs(self.data_dir, exist_ok=True)
    
    async def import_all_abilities(self):
        """导入所有特性数据"""
        logger.info("💎 开始导入所有特性数据...")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 清空表
                await self.clear_abilities()
                
                # 获取所有特性ID列表
                ability_ids = await self.get_all_ability_ids(session)
                if not ability_ids:
                    logger.error("无法获取特性ID列表")
                    return False
                
                total = len(ability_ids)
                logger.info(f"总共 {total} 个特性，开始导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, 50):  # 50个特性一批
                    batch = ability_ids[i : i + 50]
                    logger.info(f"处理批次 {i//50 + 1}/{(total + 49)//50}: 特性 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的特性数据
                    batch_results = await self.process_ability_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入特性失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有特性数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                # 验证导入的数据
                logger.info("🔍 验证导入的特性数据...")
                from data_validator import validate_import
                validation_result = validate_import("ability")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 特性数据验证通过")
                    return True
                else:
                    logger.error("❌ 特性数据验证失败")
                    return False
                
        except Exception as e:
            logger.error(f"导入特性数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=242):
        """从本地JSON文件导入特性数据"""
        logger.info(f"💎 从本地导入特性数据: ID {start_id}-{end_id}")
        
        # 检查本地数据目录
        if not os.path.exists(self.data_dir):
            logger.error(f"本地数据目录不存在: {self.data_dir}")
            return False
        
        # 清空表
        await self.clear_abilities()
        
        # 导入本地数据
        successful_imports = 0
        failed_imports = 0
        
        for i in range(start_id, min(end_id + 1, 243)):
            try:
                # 构建文件路径
                filename = f"ability_{i:03d}.json"
                filepath = os.path.join(self.data_dir, filename)
                
                # 检查文件是否存在
                if not os.path.exists(filepath):
                    logger.warning(f"本地文件不存在: {filepath}")
                    failed_imports += 1
                    continue
                
                # 读取本地JSON文件
                with open(filepath, 'r', encoding='utf-8') as f:
                    ability_data = json.load(f)
                
                # 转换并导入数据
                ability_info = self.convert_ability_data(ability_data)
                if ability_info:
                    await self.batch_insert_abilities([ability_info])
                    successful_imports += 1
                else:
                    failed_imports += 1
                    logger.error(f"转换特性数据失败: {i}")
                
                if (i - start_id + 1) % 50 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个特性")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入特性 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0
    
    async def get_all_ability_ids(self, session):
        """获取所有特性ID列表"""
        try:
            total = 242
            logger.info(f"总共 {total} 个特性")
            
            # 直接生成1-242的ID列表
            all_ids = list(range(1, total + 1))
            logger.info(f"使用所有 {len(all_ids)} 个特性ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取特性ID列表失败: {e}")
            return list(range(1, 243))
    
    async def process_ability_batch(self, session, ability_ids):
        """处理一批特性"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for ability_id in ability_ids:
            task = self.import_single_ability(session, ability_id, semaphore)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def import_single_ability(self, session, ability_id, semaphore):
        """导入单个特性"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取特性数据
                    ability_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}ability/{ability_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    
                    if not ability_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(f"特性 {ability_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                            await asyncio.sleep(wait_time)
                            continue
                        if retry == self.max_retries - 1:
                            logger.warning(f"特性 {ability_id} 数据为空，跳过导入")
                        return False
                    
                    # 转换数据
                    ability_info = self.convert_ability_data(ability_data)
                    if not ability_info:
                        logger.error(f"特性 {ability_id} 数据转换失败")
                        return False
                    
                    # 批量插入数据库
                    await self.batch_insert_abilities([ability_info])
                    return True
                    
                except Exception as e:
                    logger.error(f"特性导入失败: {ability_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(f"特性 {ability_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                        await asyncio.sleep(wait_time)
                    else:
                        return e
            
            logger.error(f"特性 {ability_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False
    
    def convert_ability_data(self, ability_data):
        """转换特性数据"""
        try:
            # 获取基本信息
            name = ability_data.get('name', '')
            name_en = name
            name_jp = name
            
            # 获取特性描述
            descriptions = ability_data.get('effect_entries', [])
            description = ""
            effect = ""
            
            for desc in descriptions:
                if desc.get('language', {}).get('name') == 'zh-hans':
                    description = desc.get('effect', '')
                    effect = desc.get('short_effect', '')
                    break
            
            return {
                "index_number": f"{ability_data.get('id', 0):04d}",
                "name": name.replace('-', ' ').title(),
                "name_en": name_en,
                "name_jp": name_jp,
                "description": description,
                "effect": effect,
                "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                "updated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            }
        except Exception as e:
            logger.error(f"转换特性数据失败: {e}")
            return None
    
    async def batch_insert_abilities(self, abilities):
        """批量插入特性数据"""
        conn = None
        cursor = None
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor(dictionary=True)
            
            insert_sql = """
                INSERT IGNORE INTO ability 
                (index_number, name, name_en, name_jp, description, effect, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            for ability in abilities:
                values = (
                    ability["index_number"],
                    ability["name"],
                    ability["name_en"],
                    ability["name_jp"],
                    ability["description"],
                    ability["effect"],
                    ability["created_at"],
                    ability["updated_at"],
                )
                cursor.execute(insert_sql, values)
            
            conn.commit()
            logger.debug(f"成功插入 {len(abilities)} 个特性")
            
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入特性数据失败: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    async def clear_abilities(self):
        """清空特性表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空特性表
            cursor.execute("TRUNCATE TABLE ability")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 ability 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 ability 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = AbilityImporter()
    
    # 检查是否需要从本地导入
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(project_root, "data", "local", "ability")
    
    if os.path.exists(data_dir) and len(os.listdir(data_dir)) > 0:
        logger.info("检测到本地数据，使用本地导入模式")
        await importer.import_from_local_data()
    else:
        logger.info("未检测到本地数据，使用网络导入模式")
        await importer.import_all_abilities()

if __name__ == "__main__":
    asyncio.run(main())