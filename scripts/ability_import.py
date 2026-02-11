import asyncio
import mysql.connector
import time
import logging
from typing import Dict, List, Any
import sys
import os

# 添加上级目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.utils import setup_logging, get_db_config

logger = logging.getLogger(__name__)

class AbilityImportService:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def import_single_ability(self, ability_id: str, ability_data: Dict[str, Any]):
        """导入单个特性"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取特性数据
            name = ability_data.get('name', '').replace('-', '_')
            name_cn = name.replace('_', ' ').title()
            name_en = name
            name_jp = name_en
            
            # 获取中文名称
            names = ability_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                        name_cn = name_obj.get('name', name_cn)
                        break
            
            # 获取日文名称
            names = ability_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取特性效果
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
            """, (name, generation, name_cn, name_en, name_jp, "暂无描述", effect,
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            return True
            
        except Exception as e:
            logger.error(f"特性导入失败: {ability_id}")
            return False
    
    async def import_all_abilities(self):
        """导入所有特性"""
        logger.info("开始导入特性数据...")
        try:
            import requests
            
            # 获取特性总数
            response = requests.get('https://pokeapi.co/api/v2/ability/?limit=1')
            total = response.json().get('count', 0)
            logger.info(f"特性总数: {total}")
            
            # 分批获取特性
            limit = 20
            offset = 0
            success_count = 0
            fail_count = 0
            
            while offset < total:
                response = requests.get(f'https://pokeapi.co/api/v2/ability/?limit={limit}&offset={offset}')
                abilities_data = response.json().get('results', [])
                
                for ability in abilities_data:
                    try:
                        ability_response = requests.get(ability['url'])
                        ability_detail = ability_response.json()
                        
                        if await self.import_single_ability(ability['name'], ability_detail):
                            success_count += 1
                        else:
                            fail_count += 1
                    except Exception as e:
                        logger.error(f"处理特性 {ability['name']} 时出错: {e}")
                        fail_count += 1
                
                offset += limit
                logger.info(f"进度: {offset}/{total}")
            
            logger.info(f"特性导入完成 - 成功: {success_count}, 失败: {fail_count}")
            return True
            
        except Exception as e:
            logger.error(f"导入特性数据失败: {e}")
            return False

async def main():
    setup_logging()
    service = AbilityImportService()
    await service.import_all_abilities()

if __name__ == "__main__":
    asyncio.run(main())