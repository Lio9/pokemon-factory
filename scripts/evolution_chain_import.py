#!/usr/bin/env python3
"""
进化链导入脚本
导入宝可梦游戏中的所有进化链
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\evolution_chain_import.log")

class EvolutionChainImporter:
    """进化链导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def clear_evolution_chains(self):
        """清空进化链表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 清空进化链表
            cursor.execute("DELETE FROM evolution_chain")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 evolution_chain 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 evolution_chain 失败: {e}")
            return False
    
    async def import_evolution_chains(self):
        """导入进化链数据"""
        logger.info("开始导入进化链数据...")
        try:
            # 先清空表
            await self.clear_evolution_chains()
            
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取进化链数据
            async with aiohttp.ClientSession() as session:
                # 获取进化链总数
                data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}evolution-chain/?limit=1")
                if not data:
                    logger.error("无法获取进化链总数")
                    return False
                    
                total = data.get('count', 0)
                logger.info(f"总共 {total} 个进化链")
                
                # 分批获取进化链
                for offset in range(0, total, 10):
                    data = await fetch_with_retry(
                        session, 
                        f"{self.pokeyapi_base_url}evolution-chain/?limit=10&offset={offset}"
                    )
                    if not data:
                        continue
                        
                    results = data.get('results', [])
                    if isinstance(results, list):
                        for chain in results:
                            if isinstance(chain, dict) and 'url' in chain:
                                await self.import_single_evolution_chain(session, chain['url'])
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("进化链数据导入完成")
            return True
        except Exception as e:
            logger.error(f"导入进化链数据失败: {e}")
            return False
    
    async def import_single_evolution_chain(self, session, chain_url):
        """导入单个进化链"""
        try:
            chain_data = await fetch_with_retry(session, chain_url)
            if not chain_data:
                return
            
            # 提取进化链信息
            chain_id = chain_data.get('id')
            
            # 递归处理进化链结构
            self.process_evolution_chain_data(chain_data, chain_id)
            
        except Exception as e:
            logger.error(f"导入进化链失败 {chain_url}: {e}")
    
    def process_evolution_chain_data(self, chain_data, chain_id):
        """处理进化链数据"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 递归处理进化链
            def process_chain(chain, parent_id=None):
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
                    (id, chain_data, created_at, updated_at)
                    VALUES (%s, %s, %s, %s)
                """, (chain_id, json.dumps(chain_data), 
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

async def main():
    """主函数"""
    importer = EvolutionChainImporter()
    await importer.import_evolution_chains()

if __name__ == "__main__":
    asyncio.run(main())