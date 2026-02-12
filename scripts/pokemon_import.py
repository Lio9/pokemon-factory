#!/usr/bin/env python3
"""
宝可梦导入脚本
导入宝可梦游戏中的所有宝可梦数据
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
from utils import get_db_config, init_logger, get_pokeapi_session

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\pokemon_import.log")

class PokemonImporter:
    """宝可梦导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.batch_size = 50
        self.max_concurrent = 50
    
    async def fetch_pokemon_data(self, session, pokemon_id, semaphore):
        """异步获取宝可梦数据"""
        async with semaphore:
            try:
                timeout = aiohttp.ClientTimeout(total=30)
                async with session.get(f"{self.pokeyapi_base_url}pokemon/{pokemon_id}", timeout=timeout) as response:
                    if response.status == 200:
                        return await response.json()
                return None
            except Exception as e:
                logger.error(f"获取宝可梦 {pokemon_id} 数据失败: {e}")
                return None
    
    async def fetch_species_data(self, session, pokemon_id, semaphore):
        """异步获取宝可梦物种数据"""
        async with semaphore:
            try:
                timeout = aiohttp.ClientTimeout(total=30)
                async with session.get(f"{self.pokeyapi_base_url}pokemon-species/{pokemon_id}", timeout=timeout) as response:
                    if response.status == 200:
                        return await response.json()
                return None
            except Exception as e:
                logger.error(f"获取宝可梦 {pokemon_id} 物种数据失败: {e}")
                return None
    
    def get_type_id(self, type_name):
        """获取属性ID"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            cursor.execute("SELECT id FROM type WHERE name_en = %s", (type_name,))
            result = cursor.fetchone()
            cursor.close()
            conn.close()
            return result[0] if result else None
        except Exception as e:
            logger.error(f"获取属性ID失败: {e}")
            return None
    
    def get_chinese_name(self, species_data):
        """获取中文名称 - 优先使用简体中文"""
        try:
            names = species_data.get('names', [])
            chinese_name = None
            chinese_hant_name = None
            
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get('language', {}).get('name', '')
                        name_text = name_obj.get('name', '').strip()
                        
                        if name_text and name_text != '???':
                            if lang_name == 'zh-hans':
                                chinese_name = name_text
                            elif lang_name == 'zh-hant':
                                chinese_hant_name = name_text
            
            # 优先使用简体中文，如果没有则使用繁体中文
            if chinese_name:
                return chinese_name
            elif chinese_hant_name:
                return chinese_hant_name
            
            return species_data.get('name', 'Unknown')
        except Exception as e:
            logger.error(f"获取中文名称失败: {e}")
            return species_data.get('name', 'Unknown')
    
    def get_japanese_name(self, species_data):
        """获取日文名称"""
        try:
            names = species_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        return name_obj.get('name', '')
            return ''
        except Exception as e:
            logger.error(f"获取日文名称失败: {e}")
            return ''
    
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
    
    def get_evolution_chain(self, species_data):
        """获取进化链信息"""
        try:
            evolution_chain_url = species_data.get('evolution_chain', {}).get('url', '')
            return evolution_chain_url
        except Exception as e:
            logger.error(f"获取进化链失败: {e}")
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
    
    def convert_pokemon_data(self, pokemon_data, species_data):
        """转换宝可梦数据格式"""
        try:
            name = self.get_chinese_name(species_data)
            name_jp = self.get_japanese_name(species_data)
            capture_rate = species_data.get('capture_rate', 45)
            gender_rate = self.get_gender_rate(species_data)
            evolution_chain = self.get_evolution_chain(species_data)
            
            types = []
            pokemon_types = pokemon_data.get('types', [])
            if isinstance(pokemon_types, list):
                for type_info in pokemon_types:
                    if isinstance(type_info, dict):
                        type_name = type_info.get('type', {}).get('name', '')
                        type_id = self.get_type_id(type_name)
                        if type_id:
                            types.append({'type_id': type_id})
            
            description = self.get_proper_description(species_data)
            sort_order = pokemon_data.get('order', 0)
            
            return {
                'index_number': f"{pokemon_data.get('id'):04d}",
                'name': name,
                'name_en': pokemon_data.get('name'),
                'name_jp': name_jp,
                'height': pokemon_data.get('height') / 10.0,
                'weight': pokemon_data.get('weight') / 10.0,
                'base_experience': pokemon_data.get('base_experience', 0),
                'capture_rate': capture_rate,
                'gender_rate': gender_rate,
                'evolution_chain_id': evolution_chain,
                'sort_order': sort_order,
                'profile': description,
                'types': types,
                'created_at': time.strftime('%Y-%m-%d %H:%M:%S'),
                'updated_at': time.strftime('%Y-%m-%d %H:%M:%S')
            }
        except Exception as e:
            logger.error(f"转换宝可梦数据失败: {e}")
            return None
    
    async def save_batch_to_database(self, batch_data):
        """批量保存到数据库"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            pokemon_insert = """
            INSERT INTO pokemon (index_number, name, name_en, name_jp, height, weight, base_experience, capture_rate, gender_rate, evolution_chain_id, sort_order, profile, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            form_insert = """
            INSERT INTO pokemon_form (pokemon_id, name, index_number, is_default, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            
            type_insert = """
            INSERT INTO pokemon_form_type (pokemon_form_id, type_id, created_at, updated_at)
            VALUES (%s, %s, %s, %s)
            """
            
            for pokemon_info in batch_data:
                if not pokemon_info:
                    continue
                    
                cursor.execute(pokemon_insert, (
                    pokemon_info.get('index_number'),
                    pokemon_info.get('name'),
                    pokemon_info.get('name_en'),
                    pokemon_info.get('name_jp'),
                    pokemon_info.get('height'),
                    pokemon_info.get('weight'),
                    pokemon_info.get('base_experience'),
                    pokemon_info.get('capture_rate', 45),
                    pokemon_info.get('gender_rate', 87.5),
                    pokemon_info.get('evolution_chain_id', ''),
                    pokemon_info.get('sort_order', 0),
                    pokemon_info.get('profile'),
                    pokemon_info.get('created_at'),
                    pokemon_info.get('updated_at')
                ))
                
                pokemon_id = cursor.lastrowid
                
                cursor.execute(form_insert, (
                    pokemon_id,
                    pokemon_info.get('name'),
                    pokemon_info.get('index_number'),
                    True,
                    pokemon_info.get('created_at'),
                    pokemon_info.get('updated_at')
                ))
                
                form_id = cursor.lastrowid
                
                types = pokemon_info.get('types', [])
                if isinstance(types, list):
                    for type_info in types:
                        if isinstance(type_info, dict):
                            cursor.execute(type_insert, (
                                form_id,
                                type_info.get('type_id'),
                                pokemon_info.get('created_at'),
                                pokemon_info.get('updated_at')
                            ))
            
            conn.commit()
            cursor.close()
            conn.close()
            return True
        except Exception as e:
            logger.error(f"批量保存数据失败: {e}")
            return False
    
    async def process_pokemon_batch(self, session, pokemon_ids):
        """处理一批宝可梦数据"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        
        tasks = []
        for pokemon_id in pokemon_ids:
            pokemon_task = self.fetch_pokemon_data(session, pokemon_id, semaphore)
            species_task = self.fetch_species_data(session, pokemon_id, semaphore)
            tasks.append(asyncio.gather(pokemon_task, species_task))
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        batch_data = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error(f"处理宝可梦 {pokemon_ids[i]} 时出错: {result}")
                continue
                
            pokemon_data, species_data = result
            if pokemon_data and species_data:
                pokemon_info = self.convert_pokemon_data(pokemon_data, species_data)
                if pokemon_info:
                    batch_data.append(pokemon_info)
        
        return batch_data
    
    async def import_all_pokemon(self):
        """导入所有宝可梦数据"""
        logger.info("🚀 开始导入所有宝可梦数据")
        
        connector, timeout = get_pokeapi_session(self.max_concurrent)
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            total_pokemon = 1350
            batches = [list(range(i, min(i + self.batch_size, total_pokemon + 1))) 
                      for i in range(1, total_pokemon + 1, self.batch_size)]
            
            for i, batch in enumerate(batches):
                logger.info(f"处理批次 {i+1}/{len(batches)}: 宝可梦 {batch[0]}-{batch[-1]}")
                
                batch_data = await self.process_pokemon_batch(session, batch)
                
                if batch_data:
                    await self.save_batch_to_database(batch_data)
                
                logger.info(f"批次 {i+1} 完成，成功处理 {len(batch_data)} 个宝可梦")
        
        logger.info("✅ 所有宝可梦数据导入完成！")

async def main():
    """主函数"""
    importer = PokemonImporter()
    await importer.import_all_pokemon()

if __name__ == "__main__":
    asyncio.run(main())
