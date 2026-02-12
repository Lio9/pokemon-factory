#!/usr/bin/env python3
"""
经验类型导入脚本
导入宝可梦游戏中的所有经验类型
"""

import asyncio
import mysql.connector
import time
import logging
from utils import get_db_config, init_logger

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\growth_rate_import.log")

class GrowthRateImporter:
    """经验类型导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
    
    async def clear_growth_rates(self):
        """清空经验类型表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 清空经验类型表
            cursor.execute("DELETE FROM growth_rate")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 growth_rate 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 growth_rate 失败: {e}")
            return False
    
    async def import_growth_rates(self):
        """导入经验类型数据"""
        logger.info("导入经验类型数据...")
        try:
            # 先清空表
            await self.clear_growth_rates()
            
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 经验类型数据
            growth_rates = [
                ('medium', '普通', 'Medium'),
                ('slow', '缓慢', 'Slow'),
                ('fast', '快速', 'Fast'),
                ('mediumslow', '中等缓慢', 'Medium-Slow'),
                ('mediumfast', '中等快速', 'Medium-Fast'),
                ('erratic', '不规则', 'Erratic')
            ]
            
            for growth_en, growth_cn, growth_jp in growth_rates:
                cursor.execute("""
                    INSERT IGNORE INTO growth_rate (name, name_en, name_jp, formula, description, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """, (growth_cn, growth_en, growth_jp,
                      f"Formula for {growth_en}",
                      f"Description for {growth_en}",
                      time.strftime('%Y-%m-%d %H:%M:%S'),
                      time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            logger.info("✅ 经验类型数据导入完成")
            return True
        except Exception as e:
            logger.error(f"导入经验类型数据失败: {e}")
            return False

async def main():
    """主函数"""
    importer = GrowthRateImporter()
    await importer.import_growth_rates()

if __name__ == "__main__":
    asyncio.run(main())