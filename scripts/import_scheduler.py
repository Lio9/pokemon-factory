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
try:
    import psutil
except ImportError:
    psutil = None
import gc
from datetime import datetime

# 添加scripts目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils import init_logger, DatabaseManager

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\import_scheduler.log")

class PerformanceMonitor:
    """性能监控器"""
    
    def __init__(self):
        self.start_time = None
        self.checkpoints = []
        self.process = psutil.Process() if psutil else None
    
    def start(self):
        """开始监控"""
        self.start_time = time.time()
        self.checkpoints = []
        logger.info("🚀 性能监控开始")
    
    def checkpoint(self, name):
        """记录检查点"""
        if self.start_time:
            elapsed = time.time() - self.start_time
            self.checkpoints.append((name, elapsed))
            if self.process:
                memory_info = self.process.memory_info()
                logger.info(f"📊 检查点 {name}: 已用时间 {elapsed:.2f}秒, 内存使用 {memory_info.rss/1024/1024:.1f}MB")
            else:
                logger.info(f"📊 检查点 {name}: 已用时间 {elapsed:.2f}秒")
    
    def get_summary(self):
        """获取监控摘要"""
        if self.start_time:
            total_time = time.time() - self.start_time
            logger.info(f"📈 性能监控结束，总耗时: {total_time:.2f}秒")
            return total_time
        return 0

async def run_import(type_name, monitor):
    """运行指定类型的导入"""
    try:
        start_time = time.time()
        logger.info(f"📦 开始导入类型: {type_name}")
        
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
            from item_import import EfficientItemImporter
            importer = EfficientItemImporter()
            await importer.import_items()
        elif type_name == 'pokemon':
            from pokemon_import import EfficientPokemonImporter
            importer = EfficientPokemonImporter()
            # 对于宝可梦导入，增加重试次数和监控
            success = await importer.import_all_pokemon()
            if not success:
                logger.error(f"宝可梦导入失败，尝试备用方案...")
                # 尝试使用标准导入器
                from pokemon_import import PokemonImporter
                standard_importer = PokemonImporter()
                await standard_importer.import_all_pokemon()
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
        else:
            logger.error(f"未知的导入类型: {type_name}")
            return False
        
        elapsed = time.time() - start_time
        logger.info(f"✅ {type_name} 导入完成，耗时: {elapsed:.2f}秒")
        monitor.checkpoint(f"{type_name}_completed")
        
        # 强制垃圾回收
        gc.collect()
        
        return True
    except Exception as e:
        logger.error(f"❌ 导入 {type_name} 失败: {e}")
        import traceback
        logger.error(f"错误堆栈: {traceback.format_exc()}")
        return False

async def main():
    """主函数 - 按顺序执行所有导入"""
    parser = argparse.ArgumentParser(description='导入调度器')
    parser.add_argument('--type', type=str, help='指定导入类型: types, abilities, moves, items, egg_groups, growth_rates, evolution_chains, pokemon')
    parser.add_argument('--monitor', action='store_true', help='启用性能监控')
    args = parser.parse_args()
    
    # 初始化性能监控
    monitor = PerformanceMonitor()
    if args.monitor:
        monitor.start()
    
    try:
        if args.type:
            # 只导入指定类型
            logger.info(f"🚀 开始导入类型: {args.type}")
            success = await run_import(args.type, monitor)
            if success:
                logger.info(f"✅ {args.type} 导入成功")
            else:
                logger.error(f"❌ {args.type} 导入失败")
        else:
            # 导入所有类型
            logger.info("🚀 开始执行导入调度器")
            monitor.start()
            
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
                
                success = await run_import(type_name, monitor)
                if success:
                    logger.info(f"✅ {description} 成功")
                    success_count += 1
                else:
                    logger.error(f"❌ {description} 失败")
                    break
            
            total_time = monitor.get_summary()
            
            logger.info(f"\n{'='*60}")
            logger.info(f"导入调度器完成")
            logger.info(f"成功: {success_count}/{total_count}")
            logger.info(f"总耗时: {total_time:.2f} 秒")
            logger.info(f"{'='*60}")
            
            # 最终垃圾回收
            gc.collect()
            
    except KeyboardInterrupt:
        logger.info("🛑 导入被用户中断")
        if args.monitor:
            monitor.get_summary()
    except Exception as e:
        logger.error(f"❌ 导入调度器发生严重错误: {e}")
        import traceback
        logger.error(f"错误堆栈: {traceback.format_exc()}")
        if args.monitor:
            monitor.get_summary()

if __name__ == "__main__":
    asyncio.run(main())