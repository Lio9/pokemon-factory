#!/usr/bin/env python3
"""
道具导入脚本
导入宝可梦游戏中的所有道具
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\item_import.log")

class ItemImporter:
    """道具导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def import_items(self):
        """导入道具数据 - 优化版本"""
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
                
                # 按ID顺序获取道具，确保导入顺序正确
                successful_imports = 0
                failed_imports = 0
                
                # 逐个获取每个道具（从1到total）
                for item_id in range(1, total + 1):
                    item_url = f"{self.pokeyapi_base_url}item/{item_id}/"
                    result = await self.import_single_item(session, item_url)
                    if result:
                        successful_imports += 1
                    else:
                        failed_imports += 1
                    
                    # 每100个道具显示一次进度
                    if item_id % 100 == 0:
                        logger.info(f"已导入 {item_id}/{total} 个道具")
                
                conn.commit()
                cursor.close()
                conn.close()
                
                logger.info(f"道具数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                return True
        except Exception as e:
            logger.error(f"导入道具数据失败: {e}")
            return False
    
    async def import_single_item(self, session, item_url):
        """导入单个道具"""
        try:
            item_data = await fetch_with_retry(session, item_url)
            if not item_data:
                return False
            
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
                        elif lang_name == 'zh-hant':
                            # 如果没有简体中文，使用繁体中文
                            if name_cn == name_en.replace('-', '_'):
                                name_cn = name_obj.get('name', name_cn)
                        elif lang_name == 'zh':
                            # 如果没有简体和繁体中文，使用zh作为fallback
                            if name_cn == name_en.replace('-', '_'):
                                name_cn = name_obj.get('name', name_cn)
            
            # 获取日文名称
            name_jp = name_en.replace('_', ' ').title()
            names = item_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取道具描述（从flavor_text_entries获取，优先zh-hans）
            description = "暂无描述"
            flavor_text_entries = item_data.get('flavor_text_entries', [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            description = entry.get('text', '').replace('\n', ' ').strip()
                            break
                        elif lang_name == 'zh-hant' and not description:
                            description = entry.get('text', '').replace('\n', ' ').strip()
                        elif lang_name == 'zh' and not description:
                            description = entry.get('text', '').replace('\n', ' ').strip()
            
            # 如果没有中文，使用effect_entries作为fallback
            if not description:
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
                            elif lang_name == 'zh-hant' and not description:
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
                            elif lang_name == 'zh' and not description:
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
            
            # 如果没有中文，使用英文作为fallback
            if not description:
                effect_entries = item_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for entry in effect_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'en':
                                effect = entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        description = lines[1].strip()
                                        if description.startswith(':'):
                                            description = description[1:].strip()
                                    else:
                                        description = effect.strip()
                                break
            
            # 从flavor_text_entries获取中文描述作为fallback
            if not description:
                flavor_text_entries = item_data.get('flavor_text_entries', [])
                if isinstance(flavor_text_entries, list):
                    for entry in flavor_text_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'zh-hans':
                                description = entry.get('text', '').replace('\n', ' ').strip()
                                break
                            elif lang_name == 'zh-hant' and not description:
                                description = entry.get('text', '').replace('\n', ' ').strip()
                            elif lang_name == 'zh' and not description:
                                description = entry.get('text', '').replace('\n', ' ').strip()
            
            # 获取道具效果（从effect_entries获取，优先zh-hans）
            effect = item_data.get('effect', '')
            effect_entries = item_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for effect_entry in effect_entries:
                    if isinstance(effect_entry, dict):
                        lang_name = effect_entry.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            effect = effect_entry.get('effect', '')
                            if effect:
                                lines = effect.split('\n')
                                if len(lines) >= 2:
                                    effect = lines[1].strip()
                                    if effect.startswith(':'):
                                        effect = effect[1:].strip()
                                else:
                                    effect = effect.strip()
                            break
                        elif lang_name == 'zh-hant' and not effect:
                            effect = effect_entry.get('effect', '')
                            if effect:
                                lines = effect.split('\n')
                                if len(lines) >= 2:
                                    effect = lines[1].strip()
                                    if effect.startswith(':'):
                                        effect = effect[1:].strip()
                                else:
                                    effect = effect.strip()
            
            # 如果没有中文，使用英文作为fallback
            if not effect:
                effect_entries = item_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for effect_entry in effect_entries:
                        if isinstance(effect_entry, dict):
                            lang_name = effect_entry.get('language', {}).get('name', '')
                            if lang_name == 'en':
                                effect = effect_entry.get('effect', '')
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        effect = lines[1].strip()
                                        if effect.startswith(':'):
                                            effect = effect[1:].strip()
                                    else:
                                        effect = effect.strip()
                                break
            
            # 从flavor_text_entries获取中文效果作为fallback
            if not effect:
                flavor_text_entries = item_data.get('flavor_text_entries', [])
                if isinstance(flavor_text_entries, list):
                    for entry in flavor_text_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'zh-hans':
                                effect = entry.get('text', '').replace('\n', ' ').strip()
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        effect = lines[1].strip()
                                        if effect.startswith(':'):
                                            effect = effect[1:].strip()
                                break
                            elif lang_name == 'zh-hant' and not effect:
                                effect = entry.get('text', '').replace('\n', ' ').strip()
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        effect = lines[1].strip()
                                        if effect.startswith(':'):
                                            effect = effect[1:].strip()
                                break
                            elif lang_name == 'zh' and not effect:
                                effect = entry.get('text', '').replace('\n', ' ').strip()
                                if effect:
                                    lines = effect.split('\n')
                                    if len(lines) >= 2:
                                        effect = lines[1].strip()
                                        if effect.startswith(':'):
                                            effect = effect[1:].strip()
                                break
            
            # 获取类别
            category_name = ""
            category_data = item_data.get('category', {})
            if isinstance(category_data, dict):
                category_name = category_data.get('name', '')
            
            # 生成索引编号（使用英文名的ID，即URL中的ID）
            # 从URL中提取ID，例如：https://pokeapi.co/api/v2/item/master-ball/ -> master-ball
            item_id = item_url.rstrip('/').split('/')[-1]
            index_number = item_id
            
            # 连接数据库并保存
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            try:
                cursor.execute("""
                    INSERT IGNORE INTO item 
                    (index_number, name, name_en, name_jp, category, price, effect, description, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, (index_number, name_cn, name_en, name_jp,
                      category_name,
                      item_data.get('cost', 0),
                      effect,
                      description,
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
                
                conn.commit()
                return True
            finally:
                cursor.close()
                conn.close()
        except Exception as e:
            logger.error(f"道具导入失败: {item_url.split('/')[-1]} - {e}")
            return False

async def main():
    """主函数"""
    importer = ItemImporter()
    await importer.import_items()

if __name__ == "__main__":
    asyncio.run(main())