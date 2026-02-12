#!/usr/bin/env python3
"""
属性导入脚本
导入宝可梦游戏中的所有属性类型
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
from utils import get_db_config, init_logger

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\type_import.log")

class TypeImporter:
    """属性导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def clear_types(self):
        """清空类型表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查以避免约束冲突
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空类型表
            cursor.execute("TRUNCATE TABLE type")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 type 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 type 失败: {e}")
            return False
    
    async def import_types(self):
        """导入属性数据"""
        logger.info("导入属性数据...")
        try:
            # 先清空表
            await self.clear_types()
            
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 属性数据
            types = [
                ('normal', '一般', 'Normal'),
                ('fighting', '格斗', 'Fighting'),
                ('flying', '飞行', 'Flying'),
                ('poison', '毒', 'Poison'),
                ('ground', '地面', 'Ground'),
                ('rock', '岩石', 'Rock'),
                ('bug', '虫', 'Bug'),
                ('ghost', '幽灵', 'Ghost'),
                ('steel', '钢', 'Steel'),
                ('fire', '火', 'Fire'),
                ('water', '水', 'Water'),
                ('grass', '草', 'Grass'),
                ('electric', '电', 'Electric'),
                ('psychic', '超能力', 'Psychic'),
                ('ice', '冰', 'Ice'),
                ('dragon', '龙', 'Dragon'),
                ('dark', '恶', 'Dark'),
                ('fairy', '妖精', 'Fairy')
            ]
            
            for i, (type_en, type_cn, type_jp) in enumerate(types, 1):
                # 保存到数据库，明确指定主键ID
                cursor.execute("""
                    INSERT IGNORE INTO type (id, name, name_en, name_jp, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """, (i, type_cn, type_en, type_jp, 
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("✅ 属性数据导入完成")
            return True
        except Exception as e:
            logger.error(f"导入属性数据失败: {e}")
            return False

async def main():
    """主函数"""
    importer = TypeImporter()
    await importer.import_types()

if __name__ == "__main__":
    asyncio.run(main())