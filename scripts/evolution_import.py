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

class EvolutionImportService:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def process_evolution_chains_after_pokemon(self):
        """处理进化链数据（在Pokemon导入完成后）"""
        logger.info("开始处理进化链数据...")
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取所有宝可梦
            cursor.execute("SELECT index_number, name FROM pokemon")
            pokemon_list = cursor.fetchall()
            
            success_count = 0
            fail_count = 0
            
            for pokemon in pokemon_list:
                pokemon_name = pokemon[0]
                pokemon_display_name = pokemon[1]
                
                try:
                    # 获取宝可梦的进化链信息
                    response = requests.get(f'https://pokeapi.co/api/v2/pokemon-species/{pokemon_name}/')
                    species_data = response.json()
                    
                    # 获取进化链URL
                    evolution_chain_url = species_data.get('evolution_chain', {}).get('url', '')
                    if evolution_chain_url:
                        evolution_response = requests.get(evolution_chain_url)
                        evolution_data = evolution_response.json()
                        
                        # 处理进化链
                        await self.process_evolution_chain(evolution_data, pokemon_name, conn, cursor)
                        success_count += 1
                    else:
                        logger.info(f"宝可梦 {pokemon_display_name} 没有进化链信息")
                        success_count += 1
                        
                except Exception as e:
                    logger.error(f"处理进化链 {pokemon_name} 时出错: {e}")
                    fail_count += 1
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info(f"进化链处理完成 - 成功: {success_count}, 失败: {fail_count}")
            return True
            
        except Exception as e:
            logger.error(f"处理进化链数据失败: {e}")
            return False
    
    async def process_evolution_chain(self, evolution_data: Dict[str, Any], pokemon_name: str, conn, cursor):
        """处理单个进化链"""
        try:
            # 获取进化链信息
            chain = evolution_data.get('chain', {})
            
            # 递归处理进化链
            await self.process_evolution_node(chain, pokemon_name, conn, cursor)
            
        except Exception as e:
            logger.error(f"处理进化链节点时出错: {e}")
    
    async def process_evolution_node(self, node: Dict[str, Any], pokemon_name: str, conn, cursor):
        """处理进化链节点"""
        try:
            # 获取当前进化
            species = node.get('species', {})
            evolution_details = node.get('evolution_details', [])
            
            if species.get('name') == pokemon_name:
                # 找到目标进化，处理进化条件
                if evolution_details:
                    for detail in evolution_details:
                        condition = self.get_evolution_condition(detail)
                        if condition:
                            # 保存进化条件
                            cursor.execute("""
                                INSERT IGNORE INTO evolution_condition 
                                (pokemon_name, condition_type, condition_value, created_at, updated_at)
                                VALUES (%s, %s, %s, %s, %s)
                            """, (pokemon_name, condition['type'], condition['value'],
                                  time.strftime('%Y-%m-%d %H:%M:%S'),
                                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            # 递归处理下一级进化
            for evolution in node.get('evolutions', []):
                await self.process_evolution_node(evolution, pokemon_name, conn, cursor)
                
        except Exception as e:
            logger.error(f"处理进化节点时出错: {e}")
    
    def get_evolution_condition(self, detail: Dict[str, Any]) -> Dict[str, str]:
        """获取进化条件"""
        condition = {}
        
        if detail.get('min_level'):
            condition['type'] = 'level'
            condition['value'] = str(detail['min_level'])
        elif detail.get('item'):
            condition['type'] = 'item'
            condition['value'] = detail['item']['name']
        elif detail.get('trigger'):
            condition['type'] = 'trigger'
            condition['value'] = detail['trigger']['name']
        elif detail.get('min_happiness'):
            condition['type'] = 'happiness'
            condition['value'] = str(detail['min_happiness'])
        elif detail.get('min_affection'):
            condition['type'] = 'affection'
            condition['value'] = str(detail['min_affection'])
        elif detail.get('gender'):
            condition['type'] = 'gender'
            condition['value'] = str(detail['gender'])
        elif detail.get('time_of_day'):
            condition['type'] = 'time'
            condition['value'] = detail['time_of_day']
        elif detail.get('needs_overworld_rain'):
            condition['type'] = 'rain'
            condition['value'] = 'true'
        elif detail.get('turn_upside_down'):
            condition['type'] = 'upsidedown'
            condition['value'] = 'true'
        elif detail.get('relative_physical_stats'):
            condition['type'] = 'stats'
            condition['value'] = str(detail['relative_physical_stats'])
        elif detail.get('party_species'):
            condition['type'] = 'party'
            condition['value'] = detail['party_species']['name']
        elif detail.get('party_type'):
            condition['type'] = 'party_type'
            condition['value'] = detail['party_type']['name']
        elif detail.get('trade_species'):
            condition['type'] = 'trade'
            condition['value'] = detail['trade_species']['name']
        elif detail.get('turn_upside_down'):
            condition['type'] = 'upsidedown'
            condition['value'] = 'true'
        elif detail.get('other_conditions'):
            condition['type'] = 'other'
            condition['value'] = str(detail['other_conditions'])
        else:
            condition['type'] = 'unknown'
            condition['value'] = 'unknown'
        
        return condition

async def main():
    setup_logging()
    service = EvolutionImportService()
    await service.process_evolution_chains_after_pokemon()

if __name__ == "__main__":
    asyncio.run(main())