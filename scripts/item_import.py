#!/usr/bin/env python3
"""
高效道具导入脚本
使用批量获取和批量数据库插入来大幅提高导入速度
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\efficient_item_import.log")

class EfficientItemImporter:
    """高效道具导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def import_items(self):
        """导入道具数据 - 优化版本（参考特性/技能导入模式）"""
        logger.info("开始导入道具数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取道具数据
            async with aiohttp.ClientSession() as session:
                # 获取道具总数
                data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}item/?limit=1")
                if not data:
                    logger.error("无法获取道具总数")
                    return False
                    
                total = data.get('count', 0)
                logger.info(f"总共 {total} 个道具")
                
                # 使用与特性/技能相同的分批获取模式
                batch_size = 50
                successful_imports = 0
                failed_imports = 0
                
                # 分批获取道具
                for offset in range(0, total, batch_size):
                    end_offset = min(offset + batch_size, total)
                    data = await fetch_with_retry(
                        session, 
                        f"{self.pokeyapi_base_url}item/?limit={batch_size}&offset={offset}"
                    )
                    if not data:
                        continue
                        
                    results = data.get('results', [])
                    if isinstance(results, list):
                        # 并发获取所有道具的详细数据
                        tasks = []
                        for item in results:
                            if isinstance(item, dict) and 'url' in item:
                                task = self.import_single_item(session, item['url'], cursor)
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
                
                logger.info(f"道具数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                return True
        except Exception as e:
            logger.error(f"导入道具数据失败: {e}")
            return False
    
    async def import_single_item(self, session, item_url, cursor):
        """导入单个道具（与特性/技能导入模式一致）"""
        try:
            item_data = await fetch_with_retry(session, item_url)
            if not item_data:
                return False
            
            # 从URL中提取ID
            item_id = item_url.rstrip('/').split('/')[-1]
            
            # 获取道具名称
            name = item_data.get('name', '').replace('-', '_')
            name_en = item_data.get('name', '')
            
            # 获取中文名称 - 优先使用简体中文
            name_cn = name_en.replace('-', '_')
            names = item_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            name_cn = name_obj.get('name', name_cn)
                            break
                        elif lang_name == 'zh-hant' and name_cn == name_en.replace('-', '_'):
                            name_cn = name_obj.get('name', name_cn)
                        elif lang_name == 'zh' and name_cn == name_en.replace('-', '_'):
                            name_cn = name_obj.get('name', name_cn)
            
            # 获取日文名称
            name_jp = name_en.replace('_', ' ').title()
            for name_obj in names:
                if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                    name_jp = name_obj.get('name', name_jp)
                    break
            
            # 获取道具描述 - 优先使用简体中文
            description = "暂无描述"
            flavor_text_entries = item_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            description = entry.get('text', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
                        elif lang_name == 'zh-hant' and description == "暂无描述":
                            description = entry.get('text', '').replace('\n', ' ').replace('  ', ' ').strip()
                        elif lang_name == 'zh' and description == "暂无描述":
                            description = entry.get('text', '').replace('\n', ' ').replace('  ', ' ').strip()
            
            # 如果没有从flavor_text_entries获取到描述，尝试从effect_entries获取
            if description == "暂无描述":
                effect_entries = item_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for entry in effect_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'zh-hans':
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
                            elif lang_name == 'zh-hant' and description == "暂无描述":
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
                            elif lang_name == 'zh' and description == "暂无描述":
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
            
            # 如果没有从effect_entries获取到描述，尝试从flavor_text_entries获取
            if description == "暂无描述":
                flavor_text_entries = item_data.get('flavor_text_entries', [])
                if isinstance(flavor_text_entries, list):
                    for entry in flavor_text_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'zh-hans':
                                description = entry.get('text', '').replace('\n', ' ').strip()
                                break
                            elif lang_name == 'zh-hant' and description == "暂无描述":
                                description = entry.get('text', '').replace('\n', ' ').strip()
                            elif lang_name == 'zh' and description == "暂无描述":
                                description = entry.get('text', '').replace('\n', ' ').strip()
            
            # 获取道具效果 - 优先使用简体中文描述中的效果
            effect = ""
            effect_entries = item_data.get('effect_entries', [])
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
            
            # 获取类别
            category_name = ""
            category_data = item_data.get('category', {})
            if isinstance(category_data, dict):
                category_name = category_data.get('name', '')
            
            # 保存到数据库（与特性/技能导入模式一致）
            cursor.execute("""
                INSERT IGNORE INTO item 
                (index_number, name, name_en, name_jp, category, price, effect, description, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (item_id, name_cn, name_en, name_jp,
                  category_name,
                  item_data.get('cost', 0),
                  effect,
                  description,
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            return True
        except Exception as e:
            logger.error(f"道具导入失败: {item_url.split('/')[-1]} - {e}")
            return False

async def main():
    """主函数"""
    importer = EfficientItemImporter()
    await importer.import_items()

if __name__ == "__main__":
    asyncio.run(main())