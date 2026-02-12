#!/usr/bin/env python3
"""
蛋群导入脚本
导入宝可梦游戏中的所有蛋群
"""

import asyncio
import mysql.connector
import time
import logging
from utils import get_db_config, init_logger

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\egg_group_import.log")

class EggGroupImporter:
    """蛋群导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
    
    async def clear_egg_groups(self):
        """清空蛋群表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 清空蛋群表
            cursor.execute("DELETE FROM pokemon_egg_group")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 pokemon_egg_group 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon_egg_group 失败: {e}")
            return False
    
    async def import_egg_groups(self):
        """导入蛋群数据"""
        logger.info("导入蛋群数据...")
        try:
            # 先清空表
            await self.clear_egg_groups()
            
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
            return True
        except Exception as e:
            logger.error(f"导入蛋群数据失败: {e}")
            return False

async def main():
    """主函数"""
    importer = EggGroupImporter()
    await importer.import_egg_groups()

if __name__ == "__main__":
    asyncio.run(main())
