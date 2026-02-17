#!/usr/bin/env python3
"""
形态数据导入脚本 - 支持本地文件导入
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
log_file = os.path.join(project_root, "logs", "form_import.log")
logger = init_logger(log_file)

class FormImporter:
    """形态导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.pokemon_dir = os.path.join(project_root, "data", "local", "pokemon")
        self.species_dir = os.path.join(project_root, "data", "local", "species")
        
        for dir_path in [self.pokemon_dir, self.species_dir]:
            os.makedirs(dir_path, exist_ok=True)
    
    async def import_all_forms(self):
        """导入所有形态数据"""
        logger.info("🎭 开始导入所有形态数据...")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 清空表
                await self.clear_forms()
                
                # 获取所有形态ID列表
                form_ids = await self.get_all_form_ids(session)
                if not form_ids:
                    logger.error("无法获取形态ID列表")
                    return False
                
                total = len(form_ids)
                logger.info(f"总共 {total} 个形态，开始导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, 100):  # 100个形态一批
                    batch = form_ids[i : i + 100]
                    logger.info(f"处理批次 {i//100 + 1}/{(total + 99)//100}: 形态 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的形态数据
                    batch_results = await self.process_form_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入形态失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有形态数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                # 验证导入的数据
                logger.info("🔍 验证导入的形态数据...")
                from data_validator import validate_import
                validation_result = validate_import("form")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 形态数据验证通过")
                    return True
                else:
                    logger.error("❌ 形态数据验证失败")
                    return False
                
        except Exception as e:
            logger.error(f"导入形态数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=1025):
        """从本地JSON文件导入形态数据"""
        logger.info(f"🎭 从本地导入形态数据: ID {start_id}-{end_id}")
        
        # 检查本地数据目录
        if not os.path.exists(self.pokemon_dir):
            logger.error(f"本地数据目录不存在: {self.pokemon_dir}")
            return False
        
        # 清空表
        await self.clear_forms()
        
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
                forms = self.convert_form_data(pokemon_data, i)
                if forms:
                    await self.batch_insert_forms(forms)
                    successful_imports += len(forms)
                else:
                    failed_imports += 1
                    logger.error(f"转换形态数据失败: {i}")
                
                if (i - start_id + 1) % 100 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个宝可梦的形态")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入形态 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0
    
    async def get_all_form_ids(self, session):
        """获取所有形态ID列表"""
        try:
            total = 1025
            logger.info(f"总共 {total} 个形态")
            
            # 直接生成1-1025的ID列表
            all_ids = list(range(1, total + 1))
            logger.info(f"使用所有 {len(all_ids)} 个形态ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取形态ID列表失败: {e}")
            return list(range(1, 1026))
    
    async def process_form_batch(self, session, form_ids):
        """处理一批形态"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for form_id in form_ids:
            task = self.import_single_form(session, form_id, semaphore)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
    
    async def import_single_form(self, session, form_id, semaphore):
        """导入单个形态"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取形态数据
                    pokemon_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}pokemon/{form_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    
                    if not pokemon_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(f"形态 {form_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                            await asyncio.sleep(wait_time)
                            continue
                        if retry == self.max_retries - 1:
                            logger.warning(f"形态 {form_id} 数据为空，跳过导入")
                        return False
                    
                    # 转换数据
                    forms = self.convert_form_data(pokemon_data, form_id)
                    if not forms:
                        logger.error(f"形态 {form_id} 数据转换失败")
                        return False
                    
                    # 批量插入数据库
                    await self.batch_insert_forms(forms)
                    return True
                    
                except Exception as e:
                    logger.error(f"形态导入失败: {form_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(f"形态 {form_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒")
                        await asyncio.sleep(wait_time)
                    else:
                        return e
            
            logger.error(f"形态 {form_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False
    
    def convert_form_data(self, pokemon_data, pokemon_id):
        """转换形态数据"""
        try:
            forms = []
            
            # 获取形态数据
            pokemon_forms = pokemon_data.get('forms', [])
            
            for form in pokemon_forms:
                form_name = form.get('name', '')
                
                forms.append({
                    "pokemon_id": pokemon_id,
                    "name": form_name.replace('-', ' ').title(),
                    "index_number": f"{pokemon_id:04d}",
                    "form_name": form_name,
                    "form_name_jp": form_name,
                    "is_default": 1,  # 默认形态
                    "is_battle_only": 0,  # 不是战斗形态
                    "is_mega": 0,  # 不是mega进化
                    "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                    "updated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
                })
            
            return forms
        except Exception as e:
            logger.error(f"转换形态数据失败: {e}")
            return None
    
    async def batch_insert_forms(self, forms):
        """批量插入形态数据"""
        conn = None
        cursor = None
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor(dictionary=True)
            
            insert_sql = """
                INSERT IGNORE INTO pokemon_form 
                (pokemon_id, name, index_number, form_name, form_name_jp, is_default, is_battle_only, is_mega, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            for form in forms:
                values = (
                    form["pokemon_id"],
                    form["name"],
                    form["index_number"],
                    form["form_name"],
                    form["form_name_jp"],
                    form["is_default"],
                    form["is_battle_only"],
                    form["is_mega"],
                    form["created_at"],
                    form["updated_at"],
                )
                cursor.execute(insert_sql, values)
            
            conn.commit()
            logger.debug(f"成功插入 {len(forms)} 个形态")
            
        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入形态数据失败: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    async def clear_forms(self):
        """清空形态表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空形态表
            cursor.execute("TRUNCATE TABLE pokemon_form")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 pokemon_form 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon_form 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = FormImporter()
    
    # 检查是否需要从本地导入
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(project_root, "data", "local", "pokemon")
    
    if os.path.exists(data_dir) and len(os.listdir(data_dir)) > 0:
        logger.info("检测到本地数据，使用本地导入模式")
        await importer.import_from_local_data()
    else:
        logger.info("未检测到本地数据，使用网络导入模式")
        await importer.import_all_forms()

if __name__ == "__main__":
    asyncio.run(main())