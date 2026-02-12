#!/usr/bin/env python3
"""
导入调度器
按顺序调用各个导入脚本
"""

import asyncio
import sys
import os
import time
import logging
import argparse

# 添加scripts目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils import init_logger

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\import_scheduler.log")

async def run_import(type_name):
    """运行指定类型的导入"""
    try:
        if type_name == 'types':
            from type_import import TypeImporter
            importer = TypeImporter()
            await importer.import_types()
        elif type_name == 'abilities':
            from ability_import import AbilityImporter
            importer = AbilityImporter()
            await importer.import_abilities()
        elif type_name == 'moves':
            from move_import import MoveImporter
            importer = MoveImporter()
            await importer.import_moves()
        elif type_name == 'items':
            from item_import import ItemImporter
            importer = ItemImporter()
            await importer.import_items()
        elif type_name == 'egg_groups':
            from egg_group_import import EggGroupImporter
            importer = EggGroupImporter()
            await importer.import_egg_groups()
        elif type_name == 'growth_rates':
            from growth_rate_import import GrowthRateImporter
            importer = GrowthRateImporter()
            await importer.import_growth_rates()
        elif type_name == 'evolution_chains':
            from evolution_chain_import import EvolutionChainImporter
            importer = EvolutionChainImporter()
            await importer.import_evolution_chains()
        elif type_name == 'pokemon':
            from pokemon_import import PokemonImporter
            importer = PokemonImporter()
            await importer.import_all_pokemon()
        else:
            logger.error(f"未知的导入类型: {type_name}")
            return False
        
        return True
    except Exception as e:
        logger.error(f"导入 {type_name} 失败: {e}")
        return False

async def main():
    """主函数 - 按顺序执行所有导入"""
    parser = argparse.ArgumentParser(description='导入调度器')
    parser.add_argument('--type', type=str, help='指定导入类型: types, abilities, moves, items, egg_groups, growth_rates, evolution_chains, pokemon')
    args = parser.parse_args()
    
    if args.type:
        # 只导入指定类型
        logger.info(f"🚀 开始导入类型: {args.type}")
        success = await run_import(args.type)
        if success:
            logger.info(f"✅ {args.type} 导入成功")
        else:
            logger.error(f"❌ {args.type} 导入失败")
    else:
        # 导入所有类型
        logger.info("🚀 开始执行导入调度器")
        
        start_time = time.time()
        
        # 导入顺序
        import_sequence = [
            ('types', '导入属性数据'),
            ('abilities', '导入特性数据'),
            ('moves', '导入技能数据'),
            ('items', '导入道具数据'),
            ('egg_groups', '导入蛋群数据'),
            ('growth_rates', '导入经验类型数据'),
            ('pokemon', '导入宝可梦数据'),
            ('evolution_chains', '导入进化链数据')
        ]
        
        success_count = 0
        total_count = len(import_sequence)
        
        for type_name, description in import_sequence:
            logger.info(f"\n{'='*60}")
            logger.info(f"执行: {description}")
            logger.info(f"{'='*60}")
            
            success = await run_import(type_name)
            if success:
                logger.info(f"✅ {description} 成功")
                success_count += 1
            else:
                logger.error(f"❌ {description} 失败")
                break
        
        end_time = time.time()
        duration = end_time - start_time
        
        logger.info(f"\n{'='*60}")
        logger.info(f"导入调度器完成")
        logger.info(f"成功: {success_count}/{total_count}")
        logger.info(f"总耗时: {duration:.2f} 秒")
        logger.info(f"{'='*60}")

if __name__ == "__main__":
    asyncio.run(main())