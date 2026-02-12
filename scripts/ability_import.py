#!/usr/bin/env python3
"""
特性导入脚本
导入宝可梦游戏中的所有特性
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\ability_import.log")

class AbilityImporter:
    """特性导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def clear_abilities(self):
        """清空特性表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 清空特性表
            cursor.execute("DELETE FROM ability")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 ability 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 ability 失败: {e}")
            return False
    
    async def import_abilities(self):
        """导入特性数据 - 优化版本"""
        logger.info("开始导入特性数据...")
        try:
            # 先清空表
            await self.clear_abilities()
            
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取特性数据
            async with aiohttp.ClientSession() as session:
                # 获取特性总数
                data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}ability/?limit=1")
                if not data:
                    logger.error("无法获取特性总数")
                    return False
                    
                total = data.get('count', 0)
                logger.info(f"总共 {total} 个特性")
                
                # 使用更大的批次来减少请求数量
                batch_size = 50
                successful_imports = 0
                failed_imports = 0
                
                # 分批获取特性
                for offset in range(0, total, batch_size):
                    end_offset = min(offset + batch_size, total)
                    data = await fetch_with_retry(
                        session, 
                        f"{self.pokeyapi_base_url}ability/?limit={batch_size}&offset={offset}"
                    )
                    if not data:
                        continue
                        
                    results = data.get('results', [])
                    if isinstance(results, list):
                        # 并发获取所有特性的详细数据
                        tasks = []
                        for ability in results:
                            if isinstance(ability, dict) and 'url' in ability:
                                task = self.import_single_ability(session, ability['url'], cursor)
                                tasks.append(task)
                        
                        if tasks:
                            results = await asyncio.gather(*tasks, return_exceptions=True)
                            for result in results:
                                if isinstance(result, Exception):
                                    failed_imports += 1
                                else:
                                    successful_imports += 1
                
                conn.commit()
                cursor.close()
                conn.close()
                
                logger.info(f"特性数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                return True
        except Exception as e:
            logger.error(f"导入特性数据失败: {e}")
            return False
    
    async def import_single_ability(self, session, ability_url, cursor):
        """导入单个特性"""
        try:
            ability_data = await fetch_with_retry(session, ability_url)
            if not ability_data:
                return False
            
            # 获取特性名称
            name = ability_data.get('name', '').replace('-', '_')
            name_en = ability_data.get('name', '')
            
            # 获取中文名称 - 优先使用简体中文
            name_cn = name_en.replace('_', ' ').title()
            names = ability_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get('language', {}).get('name', '')
                        # 优先使用简体中文，然后是繁体中文，最后是通用zh
                        if lang_name == 'zh-hans':
                            name_cn = name_obj.get('name', name_cn)
                            break
                        elif lang_name == 'zh-hant' and name_cn == name_en.replace('_', ' ').title():
                            # 只有在还没有获取到中文名称时才使用繁体
                            name_cn = name_obj.get('name', name_cn)
                        elif lang_name == 'zh' and name_cn == name_en.replace('_', ' ').title():
                            # 只有在还没有获取到中文名称时才使用通用zh
                            name_cn = name_obj.get('name', name_cn)
            
            # 获取日文名称
            name_jp = name_en.replace('_', ' ').title()
            names = ability_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取特性描述 - 优先使用简体中文
            description = "暂无描述"
            flavor_text_entries = ability_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            description = entry.get('flavor_text', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
                        elif lang_name == 'zh-hant' and description == "暂无描述":
                            description = entry.get('flavor_text', '').replace('\n', ' ').replace('  ', ' ').strip()
                        elif lang_name == 'zh' and description == "暂无描述":
                            description = entry.get('flavor_text', '').replace('\n', ' ').replace('  ', ' ').strip()
            
            # 获取特性效果 - 优先使用简体中文描述中的效果
            effect = ""
            effect_entries = ability_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for i, effect_entry in enumerate(effect_entries):
                    if isinstance(effect_entry, dict):
                        lang_name = effect_entry.get('language', {}).get('name', '')
                        effect = effect_entry.get('effect', '')
                        if lang_name == 'en':
                            effect = effect.replace('\n', ' ').replace('  ', ' ').strip()
                            break
            
            if not effect and effect_entries:
                first_effect = effect_entries[0]
                if isinstance(first_effect, dict):
                    effect = first_effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
            
            # 获取世代
            generation = "1"
            generation_data = ability_data.get('generation', {})
            if isinstance(generation_data, dict):
                generation_name = generation_data.get('name', '')
                generation_map = {
                    'generation-i': '1', 'generation-ii': '2', 'generation-iii': '3',
                    'generation-iv': '4', 'generation-v': '5', 'generation-vi': '6',
                    'generation-vii': '7', 'generation-viii': '8'
                }
                generation = generation_map.get(generation_name, '1')
            
            # 从URL中提取ID作为主键
            ability_id = int(ability_url.rstrip('/').split('/')[-1])
            
            # 保存到数据库，明确指定主键ID
            cursor.execute("""
                INSERT IGNORE INTO ability 
                (id, index_number, generation, name, name_en, name_jp, description, effect, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (ability_id, name, generation, name_cn, name_en, name_jp, description, effect,
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            return True
        except Exception as e:
            logger.error(f"特性导入失败: {ability_url.split('/')[-1]} - {e}")
            return False

async def main():
    """主函数"""
    importer = AbilityImporter()
    await importer.import_abilities()

if __name__ == "__main__":
    asyncio.run(main())
