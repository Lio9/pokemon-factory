#!/usr/bin/env python3
"""
Pokemon Factory 导入管理器 - 精简版
整合所有导入功能，提供统一的命令行接口
"""

import asyncio
import argparse
import sys
import os
import time
from datetime import datetime

# 添加项目根目录到Python路径
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)
sys.path.insert(0, os.path.join(project_root, 'scripts'))

from utils import get_db_config, init_logger
from data_validator import validate_all_imports

# 初始化日志
log_file = os.path.join(project_root, "logs", "import_manager.log")
logger = init_logger(log_file)

class ImportManager:
    """统一导入管理器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.data_dir = os.path.join(project_root, "data", "local")
        
    def check_local_data(self):
        """检查本地数据是否存在"""
        return os.path.exists(self.data_dir) and len(os.listdir(self.data_dir)) > 0
    
    async def download_all_data(self):
        """下载所有本地数据"""
        logger.info("📥 开始下载所有本地数据...")
        
        try:
            from download_local_data import LocalDataDownloader
            downloader = LocalDataDownloader()
            await downloader.download_all_data()
            
            logger.info("✅ 本地数据下载完成")
            return True
            
        except Exception as e:
            logger.error(f"❌ 本地数据下载失败: {e}")
            return False
    
    async def import_all_data(self, use_local=True):
        """导入所有数据"""
        logger.info("🚀 开始导入所有数据...")
        
        import_types = ["pokemon", "ability", "move", "form", "stats", "egg_group"]
        results = {}
        
        for import_type in import_types:
            success = await self.import_single_type(import_type, use_local)
            results[import_type] = success
            
            if not success:
                logger.error(f"❌ {import_type}数据导入失败，停止后续导入")
                break
        
        # 验证所有导入的数据
        logger.info("🔍 验证所有导入的数据...")
        validation_success = validate_all_imports()
        
        # 统计结果
        total = len(import_types)
        successful = sum(1 for success in results.values() if success)
        failed = total - successful
        
        logger.info(f"📊 导入完成: 成功 {successful}/{total}, 失败 {failed}/{total}")
        logger.info(f"✅ 数据验证: {'通过' if validation_success else '失败'}")
        
        return successful == total and validation_success
    
    async def import_single_type(self, import_type, use_local=True):
        """导入单个类型的数据"""
        logger.info(f"📥 开始导入{import_type}数据...")
        
        try:
            # 根据导入类型导入数据
            if import_type == "pokemon":
                from pokemon_import import EfficientPokemonImporter
                importer = EfficientPokemonImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_pokemon()
            
            elif import_type == "ability":
                from ability_import import AbilityImporter
                importer = AbilityImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_abilities()
            
            elif import_type == "move":
                from move_import import MoveImporter
                importer = MoveImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_moves()
            
            elif import_type == "form":
                from form_import import FormImporter
                importer = FormImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_forms()
            
            elif import_type == "stats":
                from stats_import import StatsImporter
                importer = StatsImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_stats()
            
            elif import_type == "egg_group":
                from egg_group_import import EggGroupImporter
                importer = EggGroupImporter()
                if use_local and self.check_local_data():
                    return await importer.import_from_local_data()
                else:
                    return await importer.import_all_egg_groups()
            
            else:
                logger.error(f"❌ 未知的导入类型: {import_type}")
                return False
                
        except Exception as e:
            logger.error(f"❌ {import_type}数据导入失败: {e}")
            return False
    
    async def validate_data(self):
        """验证所有数据"""
        logger.info("🔍 验证所有数据...")
        
        try:
            validation_success = validate_all_imports()
            
            if validation_success:
                logger.info("✅ 所有数据验证通过")
            else:
                logger.error("❌ 部分数据验证失败")
            
            return validation_success
            
        except Exception as e:
            logger.error(f"❌ 数据验证失败: {e}")
            return False
    
    async def cleanup_local_data(self):
        """清理本地数据"""
        logger.info("🧹 开始清理本地数据...")
        
        try:
            if os.path.exists(self.data_dir):
                import shutil
                shutil.rmtree(self.data_dir)
                logger.info("✅ 本地数据清理完成")
                return True
            else:
                logger.info("ℹ️  本地数据目录不存在")
                return True
                
        except Exception as e:
            logger.error(f"❌ 本地数据清理失败: {e}")
            return False
    
    async def run(self, import_type="all", use_local=True):
        """运行导入管理器"""
        logger.info("=" * 60)
        logger.info("🚀 Pokemon Factory 导入管理器")
        logger.info("=" * 60)
        
        start_time = time.time()
        
        try:
            if import_type == "download":
                success = await self.download_all_data()
            elif import_type == "validate":
                success = await self.validate_data()
            elif import_type == "cleanup":
                success = await self.cleanup_local_data()
            elif import_type == "all":
                success = await self.import_all_data(use_local)
            else:
                success = await self.import_single_type(import_type, use_local)
            
            duration = time.time() - start_time
            
            logger.info("=" * 60)
            logger.info(f"⏰ 总耗时: {duration:.2f}秒")
            
            if success:
                logger.info("🎉 导入管理器执行成功")
            else:
                logger.error("💥 导入管理器执行失败")
            
            return success
            
        except Exception as e:
            logger.error(f"💥 导入管理器执行异常: {e}")
            return False

async def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Pokemon Factory 导入管理器")
    parser.add_argument("type", nargs="?", default="all", 
                       choices=["all", "pokemon", "ability", "move", "form", "stats", "egg_group", "download", "validate", "cleanup"],
                       help="导入类型")
    parser.add_argument("--use-local", action="store_true", default=True,
                       help="使用本地数据（默认启用）")
    parser.add_argument("--no-local", dest="use_local", action="store_false",
                       help="不使用本地数据")
    parser.add_argument("--list-types", action="store_true",
                       help="列出可用的导入类型")
    
    args = parser.parse_args()
    
    manager = ImportManager()
    
    if args.list_types:
        print("可用的导入类型:")
        for i, import_type in enumerate(["all", "pokemon", "ability", "move", "form", "stats", "egg_group", "download", "validate", "cleanup"], 1):
            print(f"  {i}. {import_type}")
        return
    
    print(f"选择的导入类型: {args.type}")
    print(f"使用本地数据: {'是' if args.use_local else '否'}")
    
    success = await manager.run(args.type, args.use_local)
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    asyncio.run(main())