import asyncio
import logging
import sys
import os
import time

# 添加上级目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.utils import setup_logging, get_db_config

logger = logging.getLogger(__name__)

class ImportScheduler:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def run_import(self, import_type: str = "all"):
        """运行导入任务"""
        logger.info(f"开始调度导入任务: {import_type}")
        start_time = time.time()
        
        try:
            if import_type == "all" or import_type == "init":
                # 初始化数据库
                logger.info("步骤1: 初始化数据库")
                from init_database import DatabaseInitializer
                initializer = DatabaseInitializer()
                if not initializer.initialize_database():
                    logger.error("数据库初始化失败")
                    return False
            
            if import_type == "all" or import_type == "clear":
                # 清空表
                logger.info("步骤2: 清空数据库表")
                from init_database import DatabaseInitializer
                initializer = DatabaseInitializer()
                if not initializer.clear_tables():
                    logger.error("清空表失败")
                    return False
            
            if import_type == "all" or import_type == "abilities":
                # 导入特性
                logger.info("步骤3: 导入特性数据")
                from ability_import import AbilityImportService
                ability_service = AbilityImportService()
                if not await ability_service.import_all_abilities():
                    logger.error("特性导入失败")
                    return False
            
            if import_type == "all" or import_type == "moves":
                # 导入技能
                logger.info("步骤4: 导入技能数据")
                from move_import import MoveImportService
                move_service = MoveImportService()
                if not await move_service.import_all_moves():
                    logger.error("技能导入失败")
                    return False
            
            if import_type == "all" or import_type == "items":
                # 导入道具
                logger.info("步骤5: 导入道具数据")
                from item_import import ItemImportService
                item_service = ItemImportService()
                if not await item_service.import_all_items():
                    logger.error("道具导入失败")
                    return False
            
            if import_type == "all" or import_type == "pokemon":
                # 导入Pokemon
                logger.info("步骤6: 导入宝可梦数据")
                from pokemon_import import PokemonImportService
                pokemon_service = PokemonImportService()
                if not await pokemon_service.import_all_pokemon():
                    logger.error("Pokemon导入失败")
                    return False
            
            if import_type == "all" or import_type == "evolutions":
                # 导入进化链
                logger.info("步骤7: 处理进化链数据")
                from evolution_import import EvolutionImportService
                evolution_service = EvolutionImportService()
                if not await evolution_service.process_evolution_chains_after_pokemon():
                    logger.error("进化链处理失败")
                    return False
            
            end_time = time.time()
            duration = end_time - start_time
            
            logger.info(f"✅ 所有导入任务完成！总耗时: {duration:.2f}秒")
            return True
            
        except Exception as e:
            logger.error(f"导入任务执行失败: {e}")
            return False

async def main():
    setup_logging()
    
    import argparse
    parser = argparse.ArgumentParser(description='导入任务调度器')
    parser.add_argument('--type', type=str, default='all', 
                       choices=['all', 'init', 'clear', 'abilities', 'moves', 'items', 'pokemon', 'evolutions'],
                       help='导入类型')
    
    args = parser.parse_args()
    
    scheduler = ImportScheduler()
    success = await scheduler.run_import(args.type)
    
    if success:
        print("导入任务执行成功！")
    else:
        print("导入任务执行失败！")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())