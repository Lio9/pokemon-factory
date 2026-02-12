#!/usr/bin/env python3
"""
高效宝可梦导入脚本
使用更粗粒度的批次和更高效的并发处理来大幅提高导入速度
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\efficient_pokemon_import.log")

class EfficientPokemonImporter:
    """高效宝可梦导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.batch_size = 200  # 增加批次大小
        self.max_concurrent = 100  # 增加并发数
    
    async def import_all_pokemon(self):
        """导入所有宝可梦数据 - 高性能版本"""
        logger.info("🚀 开始导入所有宝可梦数据 - 高性能版本")
        
        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=100, limit_per_host=50)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                # 先清空表
                await self.clear_pokemon()
                
                # 获取所有宝可梦ID列表
                pokemon_ids = await self.get_all_pokemon_ids(session)
                if not pokemon_ids:
                    logger.error("无法获取宝可梦ID列表")
                    return False
                
                total = len(pokemon_ids)
                logger.info(f"总共 {total} 个宝可梦，开始并发导入")
                
                # 分批处理
                successful_imports = 0
                failed_imports = 0
                
                for i in range(0, total, self.batch_size):
                    batch = pokemon_ids[i:i + self.batch_size]
                    logger.info(f"处理批次 {i//self.batch_size + 1}/{(total + self.batch_size - 1)//self.batch_size}: 宝可梦 {batch[0]}-{batch[-1]}")
                    
                    # 并发获取当前批次的宝可梦数据
                    batch_results = await self.process_pokemon_batch(session, batch)
                    
                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入宝可梦失败: {result}")
                        elif result:
                            successful_imports += 1
                    
                    logger.info(f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                
                logger.info(f"✅ 所有宝可梦数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                return True
                
        except Exception as e:
            logger.error(f"导入宝可梦数据失败: {e}")
            return False
    
    async def get_all_pokemon_ids(self, session):
        """获取所有宝可梦ID列表"""
        try:
            # 先获取总数
            data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}pokemon/?limit=1")
            if not data:
                return []
            
            total = data.get('count', 0)
            logger.info(f"总共 {total} 个宝可梦")
            
            # 分批获取所有ID
            all_ids = []
            batch_size = 200
            
            for offset in range(0, total, batch_size):
                url = f"{self.pokeyapi_base_url}pokemon/?limit={batch_size}&offset={offset}"
                data = await fetch_with_retry(session, url)
                if data and data.get('results'):
                    for pokemon in data['results']:
                        if isinstance(pokemon, dict) and 'url' in pokemon:
                            pokemon_id = int(pokemon['url'].rstrip('/').split('/')[-1])
                            all_ids.append(pokemon_id)
                
                logger.info(f"已获取 {len(all_ids)} 个宝可梦ID")
            
            return all_ids
        except Exception as e:
            logger.error(f"获取宝可梦ID列表失败: {e}")
            return []
    
    async def process_pokemon_batch(self, session, pokemon_ids):
        """处理一批宝可梦"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []
        
        for pokemon_id in pokemon_ids:
            task = self.import_single_pokemon(session, pokemon_id, semaphore)
            tasks.append(task)
        
        return await asyncio.gather(*tasks, return_exceptions=True)
    
    async def import_single_pokemon(self, session, pokemon_id, semaphore):
        """导入单个宝可梦"""
        async with semaphore:
            try:
                # 获取宝可梦数据
                pokemon_data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}pokemon/{pokemon_id}/")
                if not pokemon_data:
                    return False
                
                # 获取宝可梦物种数据
                species_data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}pokemon-species/{pokemon_id}/")
                if not species_data:
                    return False
                
                # 转换数据
                pokemon_info = self.convert_pokemon_data(pokemon_data, species_data)
                if not pokemon_info:
                    return False
                
                # 批量插入数据库
                await self.batch_insert_pokemon(pokemon_info)
                return True
                
            except Exception as e:
                logger.error(f"宝可梦导入失败: {pokemon_id} - {e}")
                return e
    
    def convert_pokemon_data(self, pokemon_data, species_data):
        """转换宝可梦数据"""
        try:
            # 获取基本信息
            index_number = f"{pokemon_data.get('id'):04d}"
            name = pokemon_data.get('name', '').replace('-', '_')
            name_en = pokemon_data.get('name', '')
            name_jp = self.get_japanese_name(species_data)
            height = pokemon_data.get('height') / 10.0
            weight = pokemon_data.get('weight') / 10.0
            base_experience = pokemon_data.get('base_experience', 0)
            capture_rate = self.get_capture_rate(species_data)
            gender_rate = self.get_gender_rate(species_data)
            evolution_chain_id = self.get_evolution_chain_id(species_data)
            sort_order = species_data.get('order', 0)
            profile = self.get_proper_description(species_data)
            
            # 获取类型
            types = []
            for type_info in pokemon_data.get('types', []):
                type_name = type_info.get('type', {}).get('name', '')
                types.append({'type_id': type_name})
            
            return {
                'index_number': index_number,
                'name': name,
                'name_en': name_en,
                'name_jp': name_jp,
                'height': height,
                'weight': weight,
                'base_experience': base_experience,
                'capture_rate': capture_rate,
                'gender_rate': gender_rate,
                'evolution_chain_id': evolution_chain_id,
                'sort_order': sort_order,
                'profile': profile,
                'types': types
            }
        except Exception as e:
            logger.error(f"转换宝可梦数据失败: {e}")
            return None
    
    def get_japanese_name(self, species_data):
        """获取日文名称"""
        try:
            names = species_data.get('names', [])
            for name_obj in names:
                if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                    return name_obj.get('name', '')
            return name_en.replace('-', ' ').title()
        except Exception as e:
            logger.error(f"获取日文名称失败: {e}")
            return ''
    
    def get_capture_rate(self, species_data):
        """获取捕获率"""
        try:
            return species_data.get('capture_rate', 45)
        except Exception as e:
            logger.error(f"获取捕获率失败: {e}")
            return 45
    
    def get_gender_rate(self, species_data):
        """获取性别比例"""
        try:
            gender_rate = species_data.get('gender_rate', -1)
            if gender_rate == -1:
                return 87.5
            return gender_rate
        except Exception as e:
            logger.error(f"获取性别比例失败: {e}")
            return 87.5
    
    def get_evolution_chain_id(self, species_data):
        """获取进化链ID"""
        try:
            evolution_chain_url = species_data.get('evolution_chain', {}).get('url', '')
            if evolution_chain_url:
                return int(evolution_chain_url.rstrip('/').split('/')[-1])
            return ''
        except Exception as e:
            logger.error(f"获取进化链ID失败: {e}")
            return ''
    
    def get_proper_description(self, species_data):
        """获取正确的描述信息"""
        try:
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'zh':
                        description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                        if description and description != '???':
                            return description
            
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'en':
                        description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                        if description and description != '???':
                            return description
            
            return "暂无描述"
        except Exception as e:
            logger.error(f"获取描述失败: {e}")
            return "暂无描述"
    
    async def batch_insert_pokemon(self, pokemon_info):
        """批量插入宝可梦数据"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 保存到数据库
            cursor.execute("""
                INSERT IGNORE INTO pokemon 
                (id, index_number, name, name_en, name_jp, height, weight, base_experience, capture_rate, gender_rate, evolution_chain_id, `order`, profile, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (int(pokemon_info['index_number']), pokemon_info['index_number'],
                  pokemon_info['name'], pokemon_info['name_en'], pokemon_info['name_jp'],
                  pokemon_info['height'], pokemon_info['weight'], pokemon_info['base_experience'],
                  pokemon_info['capture_rate'], pokemon_info['gender_rate'],
                  pokemon_info['evolution_chain_id'], pokemon_info['sort_order'],
                  pokemon_info['profile'],
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            
        except Exception as e:
            logger.error(f"批量插入宝可梦数据失败: {e}")
    
    async def clear_pokemon(self):
        """清空宝可梦表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查以避免约束冲突
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空宝可梦表
            cursor.execute("TRUNCATE TABLE pokemon")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 pokemon 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon 失败: {e}")
            return False

async def main():
    """主函数"""
    importer = EfficientPokemonImporter()
    await importer.import_all_pokemon()

if __name__ == "__main__":
    asyncio.run(main())