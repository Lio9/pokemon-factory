import asyncio
import mysql.connector
import time
import logging
import requests
from typing import Dict, List, Any
import sys
import os

# 添加上级目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.utils import setup_logging, get_db_config

logger = logging.getLogger(__name__)

class ItemImportService:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def import_single_item(self, item_id: str, item_data: Dict[str, Any]):
        """导入单个道具"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取道具数据
            name = item_data.get('name', '').replace('-', '_')
            name_cn = name.replace('_', ' ').title()
            name_en = name
            name_jp = name_en
            
            # 获取中文名称
            names = item_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                        name_cn = name_obj.get('name', name_cn)
                        break
            
            # 获取日文名称
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
            
            # 获取效果
            effect = item_data.get('effect', '')
            if effect:
                logger.info(f"✅ 道具 {item_data.get('name')} 效果: {effect[:100]}...")
            
            # 保存到数据库
            category_name = ""
            category_data = item_data.get('category', {})
            if isinstance(category_data, dict):
                category_name = category_data.get('name', '')
            
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
            return True
            
        except Exception as e:
            logger.error(f"道具导入失败: {item_id}")
            return False
    
    async def import_all_items(self):
        """导入所有道具"""
        logger.info("开始导入道具数据...")
        try:
            # 获取道具总数
            response = requests.get('https://pokeapi.co/api/v2/item/?limit=1')
            total = response.json().get('count', 0)
            logger.info(f"道具总数: {total}")
            
            # 分批获取道具
            limit = 20
            offset = 0
            success_count = 0
            fail_count = 0
            
            while offset < total:
                response = requests.get(f'https://pokeapi.co/api/v2/item/?limit={limit}&offset={offset}')
                items_data = response.json().get('results', [])
                
                for item in items_data:
                    try:
                        item_response = requests.get(item['url'])
                        item_detail = item_response.json()
                        
                        if await self.import_single_item(item['name'], item_detail):
                            success_count += 1
                        else:
                            fail_count += 1
                    except Exception as e:
                        logger.error(f"处理道具 {item['name']} 时出错: {e}")
                        fail_count += 1
                
                offset += limit
                logger.info(f"进度: {offset}/{total}")
            
            logger.info(f"道具导入完成 - 成功: {success_count}, 失败: {fail_count}")
            return True
            
        except Exception as e:
            logger.error(f"导入道具数据失败: {e}")
            return False

async def main():
    setup_logging()
    service = ItemImportService()
    await service.import_all_items()

if __name__ == "__main__":
    asyncio.run(main())