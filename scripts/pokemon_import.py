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

class PokemonImportService:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def import_single_pokemon(self, pokemon_id: str, pokemon_data: Dict[str, Any]):
        """导入单个宝可梦"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取宝可梦数据
            name = pokemon_data.get('name', '').replace('-', '_')
            name_cn = name.replace('_', ' ').title()
            name_en = name
            name_jp = name_en
            
            # 获取中文名称
            names = pokemon_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                        name_cn = name_obj.get('name', name_cn)
                        break
            
            # 获取日文名称
            names = pokemon_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取宝可梦基础信息
            dex_number = pokemon_data.get('id', 0)
            height = pokemon_data.get('height', 0)
            weight = pokemon_data.get('weight', 0)
            
            # 获取类型
            types = []
            type_data = pokemon_data.get('types', [])
            if isinstance(type_data, list):
                for type_info in type_data:
                    if isinstance(type_info, dict):
                        type_name = type_info.get('type', {}).get('name', '')
                        types.append(type_name)
            
            # 获取特性
            abilities = []
            ability_data = pokemon_data.get('abilities', [])
            if isinstance(ability_data, list):
                for ability_info in ability_data:
                    if isinstance(ability_info, dict):
                        ability_name = ability_info.get('ability', {}).get('name', '')
                        abilities.append(ability_name)
            
            # 保存到数据库
            cursor.execute("""
                INSERT IGNORE INTO pokemon 
                (index_number, name, name_en, name_jp, dex_number, height, weight, type1, type2, ability1, ability2, ability3, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (name, name_cn, name_en, name_jp, dex_number, height, weight,
                  types[0] if len(types) > 0 else '', types[1] if len(types) > 1 else '',
                  abilities[0] if len(abilities) > 0 else '', abilities[1] if len(abilities) > 1 else '', abilities[2] if len(abilities) > 2 else '',
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            return True
            
        except Exception as e:
            logger.error(f"宝可梦导入失败: {pokemon_id}")
            return False
    
    async def import_all_pokemon(self):
        """导入所有宝可梦"""
        logger.info("开始导入宝可梦数据...")
        try:
            # 获取宝可梦总数
            response = requests.get('https://pokeapi.co/api/v2/pokemon/?limit=1')
            total = response.json().get('count', 0)
            logger.info(f"宝可梦总数: {total}")
            
            # 分批获取宝可梦
            limit = 20
            offset = 0
            success_count = 0
            fail_count = 0
            
            while offset < total:
                response = requests.get(f'https://pokeapi.co/api/v2/pokemon/?limit={limit}&offset={offset}')
                pokemon_data = response.json().get('results', [])
                
                for pokemon in pokemon_data:
                    try:
                        pokemon_response = requests.get(pokemon['url'])
                        pokemon_detail = pokemon_response.json()
                        
                        if await self.import_single_pokemon(pokemon['name'], pokemon_detail):
                            success_count += 1
                        else:
                            fail_count += 1
                    except Exception as e:
                        logger.error(f"处理宝可梦 {pokemon['name']} 时出错: {e}")
                        fail_count += 1
                
                offset += limit
                logger.info(f"进度: {offset}/{total}")
            
            logger.info(f"宝可梦导入完成 - 成功: {success_count}, 失败: {fail_count}")
            return True
            
        except Exception as e:
            logger.error(f"导入宝可梦数据失败: {e}")
            return False

async def main():
    setup_logging()
    service = PokemonImportService()
    await service.import_all_pokemon()

if __name__ == "__main__":
    asyncio.run(main())