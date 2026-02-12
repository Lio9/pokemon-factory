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
                
                # 使用更大的批次来减少请求数量
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
        """导入单个道具"""
        try:
            item_data = await fetch_with_retry(session, item_url)
            if not item_data:
                return False
            
            # 获取道具名称
            name = item_data.get('name', '').replace('-', '_')
            name_en = item_data.get('name', '')
            
            # 获取中文名称 - 优先使用简体中文
            name_cn = name_en.replace('_', ' ').title()
            names = item_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            name_cn = name_obj.get('name', name_cn)
                            break
                        elif lang_name == 'zh-hant' and name_cn == name_en.replace('_', ' ').title():
                            name_cn = name_obj.get('name', name_cn)
                        elif lang_name == 'zh' and name_cn == name_en.replace('_', ' ').title():
                            name_cn = name_obj.get('name', name_cn)
            
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
                        break
            
            # 如果没有中文，使用英文作为fallback
            if description == "暂无描述":
                effect_entries = item_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for effect in effect_entries:
                        if isinstance(effect, dict) and effect.get('language', {}).get('name') == 'en':
                            description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
            
            # 获取效果
            effect = item_data.get('effect', '')
            
            # 获取类别
            category_name = ""
            category_data = item_data.get('category', {})
            if isinstance(category_data, dict):
                category_name = category_data.get('name', '')
            
            # 保存到数据库
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
            
            return True
        except Exception as e:
            logger.error(f"道具导入失败: {item_url.split('/')[-1]} - {e}")
            return False

async def main():
    """主函数"""
    importer = ItemImporter()
    await importer.import_items()

if __name__ == "__main__":
    asyncio.run(main())