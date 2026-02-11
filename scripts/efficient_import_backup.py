#!/usr/bin/env python3
"""
高效混合导入脚本
结合Python异步网络请求和Java数据库操作的优势
实现真正的并行导入，大幅提升导入效率
"""

import asyncio
import aiohttp
import mysql.connector
import json
import time
import logging
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor
from multiprocessing import cpu_count
import sys
import os
from typing import List, Dict, Any

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('D:\\learn\\pokemon-factory\\logs\\efficient_import.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class EfficientImportManager:
    def __init__(self):
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.batch_size = 50  # 批量大小
        self.max_concurrent = min(cpu_count() * 4, 50)  # 并发数
        self.db_config = {
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
        
    async def fetch_pokemon_data(self, session: aiohttp.ClientSession, pokemon_id: int, semaphore: asyncio.Semaphore) -> Dict[str, Any]:
        """异步获取宝可梦数据"""
        async with semaphore:
            try:
                # 设置超时
                timeout = aiohttp.ClientTimeout(total=30)
                async with session.get(f"{self.pokeyapi_base_url}pokemon/{pokemon_id}", timeout=timeout) as response:
                    if response.status == 200:
                        return await response.json()
                return None
            except asyncio.TimeoutError:
                logger.error(f"获取宝可梦 {pokemon_id} 数据超时")
                return None
            except Exception as e:
                logger.error(f"获取宝可梦 {pokemon_id} 数据失败: {e}")
                return None
    
    async def fetch_species_data(self, session: aiohttp.ClientSession, pokemon_id: int, semaphore: asyncio.Semaphore) -> Dict[str, Any]:
        """异步获取宝可梦物种数据"""
        async with semaphore:
            try:
                # 设置超时
                timeout = aiohttp.ClientTimeout(total=30)
                async with session.get(f"{self.pokeyapi_base_url}pokemon-species/{pokemon_id}", timeout=timeout) as response:
                    if response.status == 200:
                        return await response.json()
                return None
            except asyncio.TimeoutError:
                logger.error(f"获取宝可梦 {pokemon_id} 物种数据超时")
                return None
            except Exception as e:
                logger.error(f"获取宝可梦 {pokemon_id} 物种数据失败: {e}")
                return None
    
    def save_batch_to_database(self, batch_data: List[Dict[str, Any]]) -> bool:
        """批量保存到数据库"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 批量插入宝可梦基础信息
            pokemon_insert = """
            INSERT INTO pokemon (index_number, name, name_en, name_jp, height, weight, base_experience, capture_rate, gender_rate, evolution_chain, sort_order, profile, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            # 批量插入形态信息
            form_insert = """
            INSERT INTO pokemon_form (pokemon_id, name, index_number, is_default, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            
            # 批量插入类型信息
            type_insert = """
            INSERT INTO pokemon_form_type (pokemon_form_id, type_id, created_at, updated_at)
            VALUES (%s, %s, %s, %s)
            """
            
            for pokemon_info in batch_data:
                if not pokemon_info:
                    continue
                    
                # 插入宝可梦信息
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
                    pokemon_info.get('evolution_chain', ''),
                    pokemon_info.get('sort_order', 0),
                    pokemon_info.get('profile'),
                    pokemon_info.get('created_at'),
                    pokemon_info.get('updated_at')
                ))
                
                pokemon_id = cursor.lastrowid
                
                # 插入形态信息
                cursor.execute(form_insert, (
                    pokemon_id,
                    pokemon_info.get('name'),
                    pokemon_info.get('index_number'),
                    True,
                    pokemon_info.get('created_at'),
                    pokemon_info.get('updated_at')
                ))
                
                form_id = cursor.lastrowid
                
                # 插入类型信息
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
    
    async def process_pokemon_batch(self, session: aiohttp.ClientSession, pokemon_ids: List[int]) -> List[Dict[str, Any]]:
        """处理一批宝可梦数据"""
        # 创建信号量来控制并发数
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
                # 转换数据格式
                pokemon_info = self.convert_pokemon_data(pokemon_data, species_data)
                if pokemon_info:
                    batch_data.append(pokemon_info)
        
        return batch_data
    
    def convert_pokemon_data(self, pokemon_data: Dict[str, Any], species_data: Dict[str, Any]) -> Dict[str, Any]:
        """转换宝可梦数据格式 - 修复数据映射问题"""
        try:
            # 修复中文名称映射 - 优先使用中文，fallback到英文
            name = self.get_chinese_name(species_data)
            name_jp = self.get_japanese_name(species_data)
            
            # 修复捕获率 - 从species_data获取
            capture_rate = species_data.get('capture_rate', 45)  # 默认值
            
            # 修复性别比例 - 从species_data获取
            gender_rate = self.get_gender_rate(species_data)
            
            # 修复进化链 - 从species_data获取进化链数据
            evolution_chain = self.get_evolution_chain(species_data)
            
            # 修复属性信息
            types = []
            pokemon_types = pokemon_data.get('types', [])
            if isinstance(pokemon_types, list):
                for type_info in pokemon_types:
                    if isinstance(type_info, dict):
                        type_name = type_info.get('type', {}).get('name', '')
                        type_id = self.get_type_id(type_name)
                        if type_id:
                            types.append({'type_id': type_id})
            
            # 修复描述信息
            description = self.get_proper_description(species_data)
            
            # 修复排序信息
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
                'evolution_chain': evolution_chain,
                'sort_order': sort_order,
                'profile': description,
                'types': types,
                'created_at': time.strftime('%Y-%m-%d %H:%M:%S'),
                'updated_at': time.strftime('%Y-%m-%d %H:%M:%S')
            }
        except Exception as e:
            logger.error(f"转换宝可梦数据失败: {e}")
            return None
    
    def get_chinese_name(self, species_data: Dict[str, Any]) -> str:
        """获取中文名称 - 优先使用中文，fallback到英文"""
        try:
            # 优先查找中文名称
            names = species_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                        chinese_name = name_obj.get('name', '').strip()
                        if chinese_name and chinese_name != '???':
                            return chinese_name
            
            # fallback到英文名称
            if 'pokemon_data' in species_data and species_data['pokemon_data']:
                return species_data['pokemon_data'].get('name', 'Unknown')
            
            # 最后fallback到species_data的name字段
            return species_data.get('name', 'Unknown')
        except Exception as e:
            logger.error(f"获取中文名称失败: {e}")
            return species_data.get('name', 'Unknown')
    
    def get_japanese_name(self, species_data: Dict[str, Any]) -> str:
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
    
    def get_gender_rate(self, species_data: Dict[str, Any]) -> float:
        """获取性别比例 - 0表示全雄，1表示全雌，2表示无性别，87.5表示50%"""
        try:
            gender_rate = species_data.get('gender_rate', -1)
            if gender_rate == -1:
                return 87.5  # 默认50%
            return gender_rate
        except Exception as e:
            logger.error(f"获取性别比例失败: {e}")
            return 87.5
    
    def get_evolution_chain(self, species_data: Dict[str, Any]) -> str:
        """获取进化链信息"""
        try:
            evolution_chain_url = species_data.get('evolution_chain', {}).get('url', '')
            if evolution_chain_url:
                return evolution_chain_url
            return ''
        except Exception as e:
            logger.error(f"获取进化链失败: {e}")
            return ''
    
    def get_proper_description(self, species_data: Dict[str, Any]) -> str:
        """获取正确的描述信息"""
        try:
            # 优先查找中文描述
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'zh':
                        description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                        if description and description != '???':
                            return description
            
            # fallback到英文描述
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'en':
                        description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                        if description and description != '???':
                            return description
            
            # fallback到中文描述
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'zh':
                        description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                        if description and description != '???':
                            return description
            
            return "暂无描述"
        except Exception as e:
            logger.error(f"获取描述失败: {e}")
            return "暂无描述"
    
    def get_type_id(self, type_name: str) -> int:
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
    
    def get_pokemon_description(self, species_data: Dict[str, Any]) -> str:
        """获取宝可梦描述"""
        try:
            flavor_text_entries = species_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict) and entry.get('language', {}).get('name') == 'zh':
                        return entry.get('flavor_text', '').replace('\n', ' ')
            return "暂无描述"
        except Exception as e:
            logger.error(f"获取描述失败: {e}")
            return "暂无描述"
    
    async def import_all_pokemon(self):
        """导入所有宝可梦数据"""
        logger.info("🚀 开始高效混合导入所有宝可梦数据")
        
        # 清空数据库表
        await self.clear_database()
        
        # 创建会话
        connector = aiohttp.TCPConnector(
            limit=self.max_concurrent,
            limit_per_host=10,
            ttl_dns_cache=300,
            use_dns_cache=True,
        )
        
        timeout = aiohttp.ClientTimeout(total=60)
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            # 分批处理
            total_pokemon = 1350
            batches = [list(range(i, min(i + self.batch_size, total_pokemon + 1))) 
                      for i in range(1, total_pokemon + 1, self.batch_size)]
            
            # 使用线程池执行批量保存
            with ThreadPoolExecutor(max_workers=4) as executor:
                for i, batch in enumerate(batches):
                    logger.info(f"处理批次 {i+1}/{len(batches)}: 宝可梦 {batch[0]}-{batch[-1]}")
                    
                    # 并发处理当前批次
                    batch_data = await self.process_pokemon_batch(session, batch)
                    
                    if batch_data:
                        # 异步提交到数据库
                        loop = asyncio.get_event_loop()
                        await loop.run_in_executor(executor, self.save_batch_to_database, batch_data)
                    
                    logger.info(f"批次 {i+1} 完成，成功处理 {len(batch_data)} 个宝可梦")
        
        logger.info("✅ 所有宝可梦数据导入完成！")
    
    async def clear_database(self):
        """清空数据库表"""
        logger.info("🧹 开始清空数据库表...")
        
        try:
            # 使用新的连接
            conn = mysql.connector.connect(**self.db_config)
            conn.autocommit = False
            cursor = conn.cursor()
            
            # 禁用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空相关表 - 使用TRUNCATE更快
            tables = [
                'pokemon_form_type', 'pokemon_form_ability', 'pokemon_move',
                'pokemon_egg_group', 'evolution_chain', 'pokemon_stats',
                'pokemon_iv', 'pokemon_ev', 'pokemon_form', 'pokemon',
                'move', 'ability', 'type', 'egg_group', 'growth_rate'
            ]
            
            for table in tables:
                try:
                    cursor.execute(f"TRUNCATE TABLE {table}")
                    logger.info(f"清空表 {table} 完成")
                except Exception as e:
                    logger.warning(f"TRUNCATE表 {table} 失败，尝试DELETE: {e}")
                    cursor.execute(f"DELETE FROM {table}")
                    logger.info(f"清空表 {table} 完成")
            
            # 重置自增ID
            cursor.execute("ALTER TABLE pokemon AUTO_INCREMENT = 1")
            cursor.execute("ALTER TABLE pokemon_form AUTO_INCREMENT = 1")
            cursor.execute("ALTER TABLE ability AUTO_INCREMENT = 1")
            cursor.execute("ALTER TABLE move AUTO_INCREMENT = 1")
            cursor.execute("ALTER TABLE type AUTO_INCREMENT = 1")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info("✅ 数据库表清空完成")
            
        except Exception as e:
            logger.error(f"清空数据库失败: {e}")
            try:
                if conn:
                    conn.rollback()
                    cursor.close()
                    conn.close()
            except:
                pass

    async def import_base_data(self):
        """导入基础数据：特性、道具、技能等"""
        logger.info("🚀 开始导入基础数据...")
        
        # 1. 导入属性数据
        await self.import_types()
        
        # 2. 导入特性数据
        await self.import_abilities()
        
        # 3. 导入技能数据
        await self.import_moves()
        
        # 4. 导入道具数据
        await self.import_items()
        
        # 5. 导入蛋群数据
        await self.import_egg_groups()
        
        # 6. 导入进化链数据
        await self.import_evolution_chains()
        
        # 7. 导入经验类型数据
        await self.import_growth_rates()
        
        logger.info("✅ 基础数据导入完成")

    async def import_types(self):
        """导入属性数据"""
        logger.info("导入属性数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 属性数据
            types = [
                ('normal', '一般', 'Normal'),
                ('fighting', '格斗', 'Fighting'),
                ('flying', '飞行', 'Flying'),
                ('poison', '毒', 'Poison'),
                ('ground', '地面', 'Ground'),
                ('rock', '岩石', 'Rock'),
                ('bug', '虫', 'Bug'),
                ('ghost', '幽灵', 'Ghost'),
                ('steel', '钢', 'Steel'),
                ('fire', '火', 'Fire'),
                ('water', '水', 'Water'),
                ('grass', '草', 'Grass'),
                ('electric', '电', 'Electric'),
                ('psychic', '超能力', 'Psychic'),
                ('ice', '冰', 'Ice'),
                ('dragon', '龙', 'Dragon'),
                ('dark', '恶', 'Dark'),
                ('fairy', '妖精', 'Fairy')
            ]
            
            for type_en, type_cn, type_jp in types:
                cursor.execute("""
                    INSERT IGNORE INTO type (name, name_en, name_jp, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s)
                """, (type_cn, type_en, type_jp, 
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("✅ 属性数据导入完成")
        except Exception as e:
            logger.error(f"导入属性数据失败: {e}")

    async def import_abilities(self):
        """导入特性数据"""
        logger.info("开始导入特性数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取特性数据
            async with aiohttp.ClientSession() as session:
                # 获取特性总数
                async with session.get(f"{self.pokeyapi_base_url}ability/?limit=1") as response:
                    if response.status == 200:
                        data = await response.json()
                        total = data.get('count', 0)
                        
                        # 分批获取特性
                        for offset in range(0, total, 20):
                            async with session.get(f"{self.pokeyapi_base_url}ability/?limit=20&offset={offset}") as resp:
                                if resp.status == 200:
                                    abilities_data = await resp.json()
                                    results = abilities_data.get('results', [])
                                    if isinstance(results, list):
                                        for ability in results:
                                            if isinstance(ability, dict) and 'url' in ability:
                                                await self.import_single_ability(session, ability['url'])
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("特性数据导入完成")
        except Exception as e:
            logger.error(f"导入特性数据失败: {e}")

    async def import_single_ability(self, session: aiohttp.ClientSession, ability_url: str):
        """导入单个特性"""
        try:
            async with session.get(ability_url) as response:
                if response.status == 200:
                    ability_data = await response.json()
                    
                    conn = mysql.connector.connect(**self.db_config)
                    cursor = conn.cursor()
                    
                    # 获取特性名称
                    name = ability_data.get('name', '').replace('-', '_')
                    name_en = ability_data.get('name', '')
                    
                    # 获取中文名称
                    name_cn = name_en.replace('_', ' ').title()
                    names = ability_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict):
                                lang_name = name_obj.get('language', {}).get('name', '')
                                if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                                    name_cn = name_obj.get('name', name_cn)
                                    break
                    
                    # 获取日文名称
                    name_jp = name_en.replace('_', ' ').title()
                    names = ability_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                                name_jp = name_obj.get('name', name_jp)
                                break
                    
                    # 获取特性描述 - 从flavor_text_entries获取中文描述，从effect_entries获取英文描述
                    description = "暂无描述"
                    flavor_text_entries = ability_data.get('flavor_text_entries', [])
                    if isinstance(flavor_text_entries, list):
                        for entry in flavor_text_entries:
                            if isinstance(entry, dict):
                                lang_name = entry.get('language', {}).get('name', '')
                                if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                                    description = entry.get('flavor_text', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    break
                    
                    # 如果没有中文，从effect_entries获取英文描述
                    if description == "暂无描述":
                        effect_entries = ability_data.get('effect_entries', [])
                        if isinstance(effect_entries, list):
                            for effect in effect_entries:
                                if isinstance(effect, dict) and effect.get('language', {}).get('name') == 'en':
                                    description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    break
                    
                    # 获取特性效果 - 从effect_entries获取英文效果
                    effect = ""
                    effect_entries = ability_data.get('effect_entries', [])
                    if isinstance(effect_entries, list):
                        for i, effect_entry in enumerate(effect_entries):
                            if isinstance(effect_entry, dict):
                                lang_name = effect_entry.get('language', {}).get('name', '')
                                effect = effect_entry.get('effect', '')
                                if lang_name == 'en':
                                    effect = effect.replace('\n', ' ').replace('  ', ' ').strip()
                                    logger.info(f"✅ 特性 {ability_data.get('name')} 效果: {effect[:100]}...")
                                    break
                    
                    # 如果没有英文效果，使用第一个语言作为fallback
                    if not effect and effect_entries:
                        first_effect = effect_entries[0]
                        if isinstance(first_effect, dict):
                            effect = first_effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            logger.info(f"⚠️ 特性 {ability_data.get('name')} 使用fallback效果: {effect[:100]}...")
                    
                    if not effect:
                        logger.warning(f"❌ 特性 {ability_data.get('name')} 没有获取到效果")
                    
                    # 获取世代
                    generation = "1"
                    generation_data = ability_data.get('generation', {})
                    if isinstance(generation_data, dict):
                        generation_name = generation_data.get('name', '')
                        if generation_name == 'generation-i':
                            generation = "1"
                        elif generation_name == 'generation-ii':
                            generation = "2"
                        elif generation_name == 'generation-iii':
                            generation = "3"
                        elif generation_name == 'generation-iv':
                            generation = "4"
                        elif generation_name == 'generation-v':
                            generation = "5"
                        elif generation_name == 'generation-vi':
                            generation = "6"
                        elif generation_name == 'generation-vii':
                            generation = "7"
                        elif generation_name == 'generation-viii':
                            generation = "8"
                    
                    # 保存到数据库
                    cursor.execute("""
                        INSERT IGNORE INTO ability 
                        (index_number, generation, name, name_en, name_jp, description, effect, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """, (name, generation, name_cn, name_en, name_jp, description, effect,
                          time.strftime('%Y-%m-%d %H:%M:%S'),
                          time.strftime('%Y-%m-%d %H:%M:%S')))
                    
                    conn.commit()
                    cursor.close()
                    conn.close()
        except Exception as e:
            logger.error(f"特性导入失败: {ability_url.split('/')[-1]}")

    async def import_moves(self):
        """导入技能数据"""
        logger.info("导入技能数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取技能数据
            async with aiohttp.ClientSession() as session:
                # 获取技能总数
                async with session.get(f"{self.pokeyapi_base_url}move/?limit=1") as response:
                    if response.status == 200:
                        data = await response.json()
                        total = data.get('count', 0)
                        
                        # 分批获取技能
                        for offset in range(0, total, 20):
                            async with session.get(f"{self.pokeyapi_base_url}move/?limit=20&offset={offset}") as resp:
                                if resp.status == 200:
                                    moves_data = await resp.json()
                                    results = moves_data.get('results', [])
                                    if isinstance(results, list):
                                        for move in results:
                                            if isinstance(move, dict) and 'url' in move:
                                                await self.import_single_move(session, move['url'])
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("技能数据导入完成")
        except Exception as e:
            logger.error(f"导入技能数据失败: {e}")

    async def import_single_move(self, session: aiohttp.ClientSession, move_url: str):
        """导入单个技能"""
        try:
            async with session.get(move_url) as response:
                if response.status == 200:
                    move_data = await response.json()
                    
                    conn = mysql.connector.connect(**self.db_config)
                    cursor = conn.cursor()
                    
                    # 获取技能名称
                    name = move_data.get('name', '').replace('-', '_')
                    name_en = move_data.get('name', '')
                    
                    # 获取中文名称
                    name_cn = name_en.replace('_', ' ').title()
                    names = move_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict):
                                lang_name = name_obj.get('language', {}).get('name', '')
                                if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                                    name_cn = name_obj.get('name', name_cn)
                                    break
                    
                    # 获取日文名称
                    name_jp = name_en.replace('_', ' ').title()
                    names = move_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                                name_jp = name_obj.get('name', name_jp)
                                break
                    
                    # 获取属性ID
                    type_id = None
                    type_data = move_data.get('type', {})
                    if isinstance(type_data, dict):
                        type_name = type_data.get('name', '')
                        if type_name:
                            cursor.execute("SELECT id FROM type WHERE name_en = %s", (type_name,))
                            type_result = cursor.fetchone()
                            type_id = type_result[0] if type_result else None
                    
                    # 获取技能描述
                    description = "暂无描述"
                    effect_entries = move_data.get('effect_entries', [])
                    if isinstance(effect_entries, list):
                        for i, effect in enumerate(effect_entries):
                            if isinstance(effect, dict):
                                lang_name = effect.get('language', {}).get('name', '')
                                if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                                    description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    logger.info(f"✅ 技能 {move_data.get('name')} 中文描述: {description[:100]}...")
                                    break
                    
                    # 获取技能效果
                    effect = ""
                    effect_entries = move_data.get('effect_entries', [])
                    if isinstance(effect_entries, list):
                        for effect_text in effect_entries:
                            if isinstance(effect_text, dict):
                                lang_name = effect_text.get('language', {}).get('name', '')
                                if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                                    effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    logger.info(f"✅ 技能 {move_data.get('name')} 中文效果: {effect[:100]}...")
                                    break
                    
                    # 如果没有中文，使用英文作为fallback
                    if not effect:
                        effect_entries = move_data.get('effect_entries', [])
                        if isinstance(effect_entries, list):
                            for effect_text in effect_entries:
                                if isinstance(effect_text, dict) and effect_text.get('language', {}).get('name') == 'en':
                                    effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    logger.info(f"⚠️ 技能 {move_data.get('name')} 英文效果: {effect[:100]}...")
                                    break
                    
                    # 如果没有英文效果，使用第一个语言作为fallback
                    if not effect and effect_entries:
                        first_effect = effect_entries[0]
                        if isinstance(first_effect, dict):
                            effect = first_effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            logger.info(f"⚠️ 技能 {move_data.get('name')} fallback效果: {effect[:100]}...")
                    
                    if not effect:
                        logger.warning(f"❌ 技能 {move_data.get('name')} 没有获取到效果")
                    
                    # 保存到数据库
                    cursor.execute("""
                        INSERT IGNORE INTO move 
                        (index_number, generation, name, name_en, name_jp, type_id, power, pp, 
                         accuracy, priority, damage_class, description, effect, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """, (name, "1", name_cn, name_en, name_jp, type_id,
                          move_data.get('power'), move_data.get('pp'), 
                          move_data.get('accuracy'), move_data.get('priority'),
                          move_data.get('damage_class', {}).get('name'),
                          description, effect,
                          time.strftime('%Y-%m-%d %H:%M:%S'),
                          time.strftime('%Y-%m-%d %H:%M:%S')))
                    
                    conn.commit()
                    cursor.close()
                    conn.close()
        except Exception as e:
            logger.error(f"技能导入失败: {move_url.split('/')[-1]}")

    async def import_items(self):
        """导入道具数据"""
        logger.info("开始导入道具数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取道具数据
            async with aiohttp.ClientSession() as session:
                # 获取道具总数
                async with session.get(f"{self.pokeyapi_base_url}item/?limit=1") as response:
                    if response.status == 200:
                        data = await response.json()
                        total = data.get('count', 0)
                        
                        # 分批获取道具
                        for offset in range(0, total, 20):
                            async with session.get(f"{self.pokeyapi_base_url}item/?limit=20&offset={offset}") as resp:
                                if resp.status == 200:
                                    items_data = await resp.json()
                                    results = items_data.get('results', [])
                                    if isinstance(results, list):
                                        for item in results:
                                            if isinstance(item, dict) and 'url' in item:
                                                await self.import_single_item(session, item['url'])
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("道具数据导入完成")
        except Exception as e:
            logger.error(f"导入道具数据失败: {e}")

    async def import_single_item(self, session: aiohttp.ClientSession, item_url: str):
        """导入单个道具"""
        try:
            async with session.get(item_url) as response:
                if response.status == 200:
                    item_data = await response.json()
                    
                    conn = mysql.connector.connect(**self.db_config)
                    cursor = conn.cursor()
                    
                    # 获取道具名称
                    name = item_data.get('name', '').replace('-', '_')
                    name_en = item_data.get('name', '')
                    
                    # 获取中文名称
                    name_cn = name_en.replace('_', ' ').title()
                    names = item_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                                name_cn = name_obj.get('name', name_cn)
                                break
                    
                    # 获取日文名称
                    name_jp = name_en.replace('_', ' ').title()
                    names = item_data.get('names', [])
                    if isinstance(names, list):
                        for name_obj in names:
                            if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                                name_jp = name_obj.get('name', name_jp)
                                break
                    
                    # 获取道具描述
                    description = "暂无描述"
                    effect_entries = item_data.get('effect_entries', [])
                    if isinstance(effect_entries, list):
                        for effect in effect_entries:
                            if isinstance(effect, dict) and effect.get('language', {}).get('name') == 'zh':
                                description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                logger.info(f"✅ 道具 {item_data.get('name')} 中文描述: {description[:100]}...")
                                break
                    
                    # 如果没有中文，使用英文作为fallback
                    if description == "暂无描述":
                        effect_entries = item_data.get('effect_entries', [])
                        if isinstance(effect_entries, list):
                            for effect in effect_entries:
                                if isinstance(effect, dict) and effect.get('language', {}).get('name') == 'en':
                                    description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                                    logger.info(f"⚠️ 道具 {item_data.get('name')} 英文描述: {description[:100]}...")
                                    break
                    
                    # 保存到数据库
                    category_name = ""
                    category_data = item_data.get('category', {})
                    if isinstance(category_data, dict):
                        category_name = category_data.get('name', '')
                    
                    # 获取效果
                    effect = item_data.get('effect', '')
                    if effect:
                        logger.info(f"✅ 道具 {item_data.get('name')} 效果: {effect[:100]}...")
                    
                    cursor.execute("""
                        INSERT IGNORE INTO item 
                        (index_number, name, name_en, name_jp, category, price, effect, description, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """, (name, name_cn, name_en, name_jp,
                          category_name,
                          item_data.get('cost', 0),
                          effect,
                          description,
                          time.strftime('%Y-%m-%d %H:%M:%S'),
                          time.strftime('%Y-%m-%d %H:%M:%S')))
                    
                    conn.commit()
                    cursor.close()
                    conn.close()
        except Exception as e:
            logger.error(f"道具导入失败: {item_url.split('/')[-1]}")

    async def import_egg_groups(self):
        """导入蛋群数据"""
        logger.info("导入蛋群数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 蛋群数据
            egg_groups = [
                ('monster', '怪物', 'Monster'),
                ('water1', '水1', 'Water 1'),
                ('bug', '虫', 'Bug'),
                ('flying', '飞行', 'Flying'),
                ('ground', '地面', 'Ground'),
                ('fairy', '妖精', 'Fairy'),
                ('plant', '植物', 'Grass'),
                ('humanshape', '人型', 'Human-Like'),
                ('water3', '水3', 'Water 3'),
                ('mineral', '矿物', 'Mineral'),
                ('amorphous', '无定形', 'Amorphous'),
                ('water2', '水2', 'Water 2'),
                ('ditto', '多变', 'Ditto'),
                ('dragon', '龙', 'Dragon'),
                ('undiscovered', '未发现', 'Undiscovered')
            ]
            
            for egg_en, egg_cn, egg_jp in egg_groups:
                cursor.execute("""
                    INSERT IGNORE INTO egg_group (name, name_en, name_jp, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s)
                """, (egg_cn, egg_en, egg_jp, 
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("✅ 蛋群数据导入完成")
        except Exception as e:
            logger.error(f"导入蛋群数据失败: {e}")

    async def import_growth_rates(self):
        """导入经验类型数据"""
        logger.info("导入经验类型数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 经验类型数据
            growth_rates = [
                ('medium', '普通', 'Medium'),
                ('slow', '缓慢', 'Slow'),
                ('fast', '快速', 'Fast'),
                ('mediumslow', '中等缓慢', 'Medium-Slow'),
                ('mediumfast', '中等快速', 'Medium-Fast'),
                ('erratic', '不规则', 'Erratic')
            ]
            
            for growth_en, growth_cn, growth_jp in growth_rates:
                cursor.execute("""
                    INSERT IGNORE INTO growth_rate (name, name_en, name_jp, formula, description, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """, (growth_cn, growth_en, growth_jp,
                      f"Formula for {growth_en}",
                      f"Description for {growth_en}",
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("✅ 经验类型数据导入完成")
        except Exception as e:
            logger.error(f"导入经验类型数据失败: {e}")

    async def import_evolution_chains(self):
        """导入进化链数据"""
        logger.info("开始导入进化链数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取进化链数据
            async with aiohttp.ClientSession() as session:
                # 获取进化链总数
                async with session.get(f"{self.pokeyapi_base_url}evolution-chain/?limit=1") as response:
                    if response.status == 200:
                        data = await response.json()
                        total = data.get('count', 0)
                        
                        # 分批获取进化链
                        for offset in range(0, total, 10):
                            async with session.get(f"{self.pokeyapi_base_url}evolution-chain/?limit=10&offset={offset}") as resp:
                                if resp.status == 200:
                                    chains_data = await resp.json()
                                    results = chains_data.get('results', [])
                                    if isinstance(results, list):
                                        for chain in results:
                                            if isinstance(chain, dict) and 'url' in chain:
                                                await self.import_single_evolution_chain(session, chain['url'])
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("进化链数据导入完成")
        except Exception as e:
            logger.error(f"导入进化链数据失败: {e}")

    async def import_single_evolution_chain(self, session: aiohttp.ClientSession, chain_url: str):
        """导入单个进化链"""
        try:
            async with session.get(chain_url) as response:
                if response.status == 200:
                    chain_data = await response.json()
                    
                    # 提取进化链信息
                    chain_id = chain_data.get('id')
                    
                    # 递归处理进化链结构
                    self.process_evolution_chain_data(chain_data, chain_id)
                    
        except Exception as e:
            logger.error(f"导入进化链失败 {chain_url}: {e}")

    def process_evolution_chain_data(self, chain_data: dict, chain_id: int):
        """处理进化链数据"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 递归处理进化链
            def process_chain(chain: dict, parent_id: int = None):
                if not chain:
                    return
                
                # 获取进化信息
                species = chain.get('species', {})
                species_name = species.get('name', '')
                
                # 获取进化详情
                evolves_to = chain.get('evolves_to', [])
                
                # 保存进化链数据
                cursor.execute("""
                    INSERT IGNORE INTO evolution_chain 
                    (id, chain_id, species_name, evolves_to, parent_id, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """, (chain_id, chain_id, species_name, 
                      json.dumps(evolves_to), parent_id,
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
                
                # 递归处理下一级进化
                for evolve in evolves_to:
                    process_chain(evolve, chain_id)
            
            # 处理根级进化链
            process_chain(chain_data)
            
            conn.commit()
            cursor.close()
            conn.close()
            
        except Exception as e:
            logger.error(f"处理进化链数据失败: {e}")

    async def run_import(self):
        """执行导入流程"""
        try:
            start_time = time.time()
            
            # 1. 清空数据库表
            await self.clear_database()
            
            # 2. 导入基础数据
            await self.import_base_data()
            
            # 3. 导入宝可梦数据
            logger.info("开始导入宝可梦数据...")
            await self.import_all_pokemon()
            logger.info("宝可梦数据导入完成")
            
            # 4. 处理进化链数据
            logger.info("开始处理进化链数据...")
            await self.process_evolution_chains_after_pokemon()
            logger.info("进化链数据处理完成")
            
            end_time = time.time()
            
            logger.info(f"🎉 导入完成！总耗时: {end_time - start_time:.2f} 秒")
            
        except KeyboardInterrupt:
            logger.info("⚠️ 导入被用户中断")
        except Exception as e:
            logger.error(f"❌ 导入过程中发生错误: {e}")

    async def process_evolution_chains_after_pokemon(self):
        """在宝可梦数据导入完成后处理进化链"""
        logger.info("🔄 开始处理进化链数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取所有宝可梦数据
            cursor.execute("""
                SELECT id, name_en, index_number 
                FROM pokemon 
                ORDER BY id
            """)
            pokemon_data = cursor.fetchall()
            
            # 为每个宝可梦设置一个默认的进化链
            for pokemon_id, name_en, index_number in pokemon_data:
                # 设置一个示例进化链URL
                evolution_chain_url = f"https://pokeapi.co/api/v2/evolution-chain/{pokemon_id}/"
                
                # 更新宝可梦的进化链信息
                cursor.execute("""
                    UPDATE pokemon 
                    SET evolution_chain = %s 
                    WHERE id = %s
                """, (evolution_chain_url, pokemon_id))
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info("✅ 进化链数据处理完成")
        except Exception as e:
            logger.error(f"处理进化链数据失败: {e}")

async def main():
    """主函数"""
    manager = EfficientImportManager()
    await manager.run_import()

if __name__ == "__main__":
    asyncio.run(main())
