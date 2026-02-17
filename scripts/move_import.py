#!/usr/bin/env python3
"""
技能数据导入脚本 - 支持本地文件导入
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
log_file = os.path.join(project_root, "logs", "move_import.log")
logger = init_logger(log_file)

class MoveImporter:
    """技能导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.data_dir = os.path.join(project_root, "data", "local", "move")
        os.makedirs(self.data_dir, exist_ok=True)
    
    async def import_all_moves(self):
        """导入所有技能数据"""
        logger.info("⚔️  开始导入所有技能数据...")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 清空表
                await self.clear_moves()
                
                # 获取所有技能ID列表
                move_ids = await self.get_all_move_ids(session)
                if not move_ids:
                    logger.error("无法获取技能ID列表")
                    return False
                
                total = len(move_ids)
                logger.info(f"总共 {total} 个技能，开始导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, 50):  # 50个技能一批
                    batch = move_ids[i : i + 50]
                    logger.info(f"处理批次 {i//50 + 1}/{(total + 49)//50}: 技能 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的技能数据
                    batch_results = await self.process_move_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入技能失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有技能数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                # 验证导入的数据
                logger.info("🔍 验证导入的技能数据...")
                from data_validator import validate_import
                validation_result = validate_import("move")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 技能数据验证通过")
                    return True
                else:
                    logger.error("❌ 技能数据验证失败")
                    return False
                
        except Exception as e:
            logger.error(f"导入技能数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=826):
        """从本地JSON文件导入技能数据"""
        logger.info(f"⚔️  从本地导入技能数据: ID {start_id}-{end_id}")
        
        # 检查本地数据目录
        if not os.path.exists(self.data_dir):
            logger.error(f"本地数据目录不存在: {self.data_dir}")
            return False
        
        # 清空表
        await self.clear_moves()
        
        # 导入本地数据
        successful_imports = 0
        failed_imports = 0
        
        for i in range(start_id, min(end_id + 1, 827)):
            try:
                # 构建文件路径
                filename = f"move_{i:03d}.json"
                filepath = os.path.join(self.data_dir, filename)
                
                # 检查文件是否存在
                if not os.path.exists(filepath):
                    logger.warning(f"本地文件不存在: {filepath}")
                    failed_imports += 1
                    continue
                
                # 读取本地JSON文件
                with open(filepath, 'r', encoding='utf-8') as f:
                    move_data = json.load(f)
                
                # 转换并导入数据
                move_info = self.convert_move_data(move_data)
                if move_info:
                    await self.batch_insert_moves([move_info])
                    successful_imports += 1
                else:
                    failed_imports += 1
                    logger.error(f"转换技能数据失败: {i}")
                
                if (i - start_id + 1) % 100 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个技能")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入技能 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0
    
    async def get_all_move_ids(self, session):
        """获取所有技能ID列表"""
        try:
            total = 826
            logger.info(f"总共 {total} 个技能")
            
            # 直接生成1-826的ID列表
            all_ids = list(range(1, total + 1))
            logger.info(f"使用所有 {len(all_ids)} 个技能ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取技能ID列表失败: {e}")
            return list(range(1, 827))
    
    async def process_move_batch(self, session, move_ids):
        """处理一批技能"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for move_id in move_ids:
            task = self.import_single_move(session, move_id, semaphore)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def import_single_move(self, session, move_id, semaphore):
        """导入单个技能"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取技能数据
                    move_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}move/{move_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    
                    if not move_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(f"技能 {move_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                            await asyncio.sleep(wait_time)
                            continue
                        if retry == self.max_retries - 1:
                            logger.warning(f"技能 {move_id} 数据为空，跳过导入")
                        return False
                    
                    # 转换数据
                    move_info = self.convert_move_data(move_data)
                    if not move_info:
                        logger.error(f"技能 {move_id} 数据转换失败")
                        return False
                    
                    # 批量插入数据库
                    await self.batch_insert_moves([move_info])
                    return True
                    
                except Exception as e:
                    logger.error(f"技能导入失败: {move_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(f"技能 {move_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                        await asyncio.sleep(wait_time)
                    else:
                        return e
            
            logger.error(f"技能 {move_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False
    
    def convert_move_data(self, move_data):
        """转换技能数据"""
        try:
            # 获取基本信息
            name = move_data.get('name', '')
            name_en = name
            name_jp = name
            
            # 获取技能属性ID
            type_info = move_data.get('type', {})
            type_name = type_info.get('name', '') if type_info else ''
            
            # 获取技能属性ID
            type_id = None
            if type_name:
                # 连接数据库获取类型ID
                conn = mysql.connector.connect(**self.db_config)
                cursor = conn.cursor(dictionary=True)
                cursor.execute(f"SELECT id FROM type WHERE name_en = '{type_name}'")
                type_result = cursor.fetchone()
                if type_result:
                    type_id = type_result['id']
                cursor.close()
                conn.close()
            
            # 获取技能信息
            power = move_data.get('power')
            pp = move_data.get('pp')
            accuracy = move_data.get('accuracy')
            damage_class = move_data.get('damage_class', {}).get('name', '')
            
            # 获取技能描述
            descriptions = move_data.get('effect_entries', [])
            description = ""
            effect = ""
            
            for desc in descriptions:
                if desc.get('language', {}).get('name') == 'zh-hans':
                    description = desc.get('effect', '')
                    effect = desc.get('short_effect', '')
                    break
            
            return {
                "index_number": f"{move_data.get('id', 0):04d}",
                "name": name.replace('-', ' ').title(),
                "name_en": name_en,
                "name_jp": name_jp,
                "type_id": type_id,
                "power": power,
                "pp": pp,
                "accuracy": accuracy,
                "damage_class": damage_class,
                "description": description,
                "effect": effect,
                "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                "updated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            }
        except Exception as e:
            logger.error(f"转换技能数据失败: {e}")
            return None
    
    async def batch_insert_moves(self, moves):
        """批量插入技能数据"""
        conn = None
        cursor = None
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor(dictionary=True)
            
            insert_sql = """
                INSERT IGNORE INTO move 
                (index_number, name, name_en, name_jp, type_id, power, pp, accuracy, damage_class, description, effect, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            for move in moves:
                values = (
                    move["index_number"],
                    move["name"],
                    move["name_en"],
                    move["name_jp"],
                    move["type_id"],
                    move["power"],
                    move["pp"],
                    move["accuracy"],
                    move["damage_class"],
                    move["description"],
                    move["effect"],
                    move["created_at"],
                    move["updated_at"],
                )
                cursor.execute(insert_sql, values)
            
            conn.commit()
            logger.debug(f"成功插入 {len(moves)} 个技能")
            
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入技能数据失败: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    async def clear_moves(self):
        """清空技能表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空技能表
            cursor.execute("TRUNCATE TABLE move")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 move 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 move 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = MoveImporter()
    
    # 检查是否需要从本地导入
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(project_root, "data", "local", "move")
    
    if os.path.exists(data_dir) and len(os.listdir(data_dir)) > 0:
        logger.info("检测到本地数据，使用本地导入模式")
        await importer.import_from_local_data()
    else:
        logger.info("未检测到本地数据，使用网络导入模式")
        await importer.import_all_moves()

if __name__ == "__main__":
    asyncio.run(main())