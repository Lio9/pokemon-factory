#!/usr/bin/env python3
"""
统一数据验证器
用于验证导入脚本导入的数据是否正确
"""

import mysql.connector
import time
import logging
import os
from datetime import datetime
from utils import get_db_config, init_logger

# 初始化日志
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_file = os.path.join(project_root, "logs", "data_validator.log")
logger = init_logger(log_file)

class DataValidator:
    """数据验证器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.conn = None
        self.cursor = None
        self.validation_results = {
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "validation_type": "import",
            "checks": {}
        }
        
    def connect(self):
        """连接数据库"""
        try:
            self.conn = mysql.connector.connect(**self.db_config)
            self.cursor = self.conn.cursor(dictionary=True)
            logger.info("✅ 数据库连接成功")
            return True
        except Exception as e:
            logger.error(f"❌ 数据库连接失败: {e}")
            return False
    
    def disconnect(self):
        """断开数据库连接"""
        if self.cursor:
            self.cursor.close()
        if self.conn:
            self.conn.close()
        logger.info("数据库连接已关闭")
    
    def validate_import_data(self, import_type):
        """验证导入的数据"""
        logger.info(f"🔍 验证{import_type}导入数据...")
        start_time = time.time()
        
        validation_results = {}
        
        if import_type == "pokemon":
            validation_results = self.validate_pokemon_data()
        elif import_type == "ability":
            validation_results = self.validate_ability_data()
        elif import_type == "move":
            validation_results = self.validate_move_data()
        elif import_type == "form":
            validation_results = self.validate_form_data()
        elif import_type == "stats":
            validation_results = self.validate_stats_data()
        elif import_type == "egg_group":
            validation_results = self.validate_egg_group_data()
        
        duration = time.time() - start_time
        validation_results["duration"] = duration
        
        # 保存验证结果
        self.validation_results["checks"][import_type] = validation_results
        
        return validation_results
    
    def validate_pokemon_data(self):
        """验证宝可梦数据"""
        logger.info("📊 验证宝可梦数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon WHERE id IS NOT NULL")
            pokemon_count = self.cursor.fetchone()['count']
            
            # 检查必填字段
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon WHERE name IS NULL OR name_en IS NULL")
            null_field_count = self.cursor.fetchone()['count']
            
            # 检查数据范围
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon WHERE height < 0 OR height > 100")
            invalid_height_count = self.cursor.fetchone()['count']
            
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon WHERE weight < 0 OR weight > 1000")
            invalid_weight_count = self.cursor.fetchone()['count']
            
            # 检查关联数据
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon_form WHERE pokemon_id IS NOT NULL")
            form_count = self.cursor.fetchone()['count']
            
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon_stats WHERE form_id IS NOT NULL")
            stats_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if null_field_count == 0 and invalid_height_count == 0 and invalid_weight_count == 0 else "warning",
                "pokemon_count": pokemon_count,
                "null_field_count": null_field_count,
                "invalid_height_count": invalid_height_count,
                "invalid_weight_count": invalid_weight_count,
                "form_count": form_count,
                "stats_count": stats_count,
                "message": f"宝可梦数据验证完成: {pokemon_count} 条记录"
            }
            
            if null_field_count > 0:
                logger.warning(f"⚠️  宝可梦数据存在 {null_field_count} 条必填字段为空的记录")
            if invalid_height_count > 0:
                logger.warning(f"⚠️  宝可梦数据存在 {invalid_height_count} 条高度无效的记录")
            if invalid_weight_count > 0:
                logger.warning(f"⚠️  宝可梦数据存在 {invalid_weight_count} 条重量无效的记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 宝可梦数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_ability_data(self):
        """验证特性数据"""
        logger.info("💎 验证特性数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM ability WHERE id IS NOT NULL")
            ability_count = self.cursor.fetchone()['count']
            
            # 检查必填字段
            self.cursor.execute("SELECT COUNT(*) as count FROM ability WHERE name IS NULL OR name_en IS NULL")
            null_field_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if null_field_count == 0 else "warning",
                "ability_count": ability_count,
                "null_field_count": null_field_count,
                "message": f"特性数据验证完成: {ability_count} 条记录"
            }
            
            if null_field_count > 0:
                logger.warning(f"⚠️  特性数据存在 {null_field_count} 条必填字段为空的记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 特性数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_move_data(self):
        """验证技能数据"""
        logger.info("⚔️  验证技能数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM move WHERE id IS NOT NULL")
            move_count = self.cursor.fetchone()['count']
            
            # 检查必填字段
            self.cursor.execute("SELECT COUNT(*) as count FROM move WHERE name IS NULL OR name_en IS NULL")
            null_field_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if null_field_count == 0 else "warning",
                "move_count": move_count,
                "null_field_count": null_field_count,
                "message": f"技能数据验证完成: {move_count} 条记录"
            }
            
            if null_field_count > 0:
                logger.warning(f"⚠️  技能数据存在 {null_field_count} 条必填字段为空的记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 技能数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_form_data(self):
        """验证形态数据"""
        logger.info("🎭 验证形态数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon_form WHERE pokemon_id IS NOT NULL")
            form_count = self.cursor.fetchone()['count']
            
            # 检查关联关系
            self.cursor.execute("""
                SELECT COUNT(*) as count FROM pokemon_form pf 
                LEFT JOIN pokemon p ON pf.pokemon_id = p.id 
                WHERE p.id IS NULL
            """)
            orphan_form_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if orphan_form_count == 0 else "warning",
                "form_count": form_count,
                "orphan_form_count": orphan_form_count,
                "message": f"形态数据验证完成: {form_count} 条记录"
            }
            
            if orphan_form_count > 0:
                logger.warning(f"⚠️  形态数据存在 {orphan_form_count} 条孤立的形态记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 形态数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_stats_data(self):
        """验证种族值数据"""
        logger.info("📈 验证种族值数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon_stats WHERE form_id IS NOT NULL")
            stats_count = self.cursor.fetchone()['count']
            
            # 检查关联关系
            self.cursor.execute("""
                SELECT COUNT(*) as count FROM pokemon_stats ps 
                LEFT JOIN pokemon_form pf ON ps.form_id = pf.id 
                WHERE pf.id IS NULL
            """)
            orphan_stats_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if orphan_stats_count == 0 else "warning",
                "stats_count": stats_count,
                "orphan_stats_count": orphan_stats_count,
                "message": f"种族值数据验证完成: {stats_count} 条记录"
            }
            
            if orphan_stats_count > 0:
                logger.warning(f"⚠️  种族值数据存在 {orphan_stats_count} 条孤立的种族值记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 种族值数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_egg_group_data(self):
        """验证蛋群关联数据"""
        logger.info("🥚 验证蛋群关联数据...")
        
        try:
            # 检查基本数据
            self.cursor.execute("SELECT COUNT(*) as count FROM pokemon_egg_group WHERE pokemon_id IS NOT NULL")
            egg_group_count = self.cursor.fetchone()['count']
            
            # 检查关联关系
            self.cursor.execute("""
                SELECT COUNT(*) as count FROM pokemon_egg_group peg 
                LEFT JOIN pokemon p ON peg.pokemon_id = p.id 
                WHERE p.id IS NULL
            """)
            orphan_egg_group_count = self.cursor.fetchone()['count']
            
            results = {
                "status": "success" if orphan_egg_group_count == 0 else "warning",
                "egg_group_count": egg_group_count,
                "orphan_egg_group_count": orphan_egg_group_count,
                "message": f"蛋群关联数据验证完成: {egg_group_count} 条记录"
            }
            
            if orphan_egg_group_count > 0:
                logger.warning(f"⚠️  蛋群关联数据存在 {orphan_egg_group_count} 条孤立的蛋群关联记录")
            
            return results
            
        except Exception as e:
            logger.error(f"❌ 蛋群关联数据验证失败: {e}")
            return {"status": "failed", "error": str(e)}
    
    def validate_all_data(self):
        """验证所有数据"""
        logger.info("🔍 验证所有导入数据...")
        
        if not self.connect():
            return False
        
        validation_types = ["pokemon", "ability", "move", "form", "stats", "egg_group"]
        all_valid = True
        
        for import_type in validation_types:
            result = self.validate_import_data(import_type)
            if result.get("status") == "failed":
                all_valid = False
            elif result.get("status") == "warning":
                logger.warning(f"⚠️  {import_type}数据存在警告")
        
        self.disconnect()
        
        # 生成验证报告
        self.generate_validation_report()
        
        return all_valid
    
    def generate_validation_report(self):
        """生成验证报告"""
        logger.info("📋 生成验证报告...")
        
        # 保存JSON报告
        import json
        report_file = os.path.join(project_root, "logs", f"validation_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
        
        with open(report_file, "w", encoding="utf-8") as f:
            json.dump(self.validation_results, f, ensure_ascii=False, indent=2)
        
        # 生成总结
        summary = {
            "timestamp": self.validation_results["timestamp"],
            "overall_status": "success",
            "summary": {}
        }
        
        for check_name, check_result in self.validation_results["checks"].items():
            summary["summary"][check_name] = check_result.get("status", "unknown")
            if check_result.get("status") != "success":
                summary["overall_status"] = "warning"
        
        # 保存总结报告
        summary_file = os.path.join(project_root, "logs", "validation_summary.json")
        with open(summary_file, "w", encoding="utf-8") as f:
            json.dump(summary, f, ensure_ascii=False, indent=2)
        
        logger.info(f"✅ 验证报告已生成: {report_file}")
        logger.info(f"📊 总结报告已生成: {summary_file}")
        
        # 打印验证结果
        print("\n" + "="*60)
        print("📊 数据验证结果")
        print("="*60)
        print(f"生成时间: {summary['timestamp']}")
        print(f"总体状态: {summary['overall_status'].upper()}")
        
        for check_name, status in summary["summary"].items():
            status_icon = "✅" if status == "success" else "⚠️" if status == "warning" else "❌"
            print(f"  {status_icon} {check_name}: {status}")
        
        print("="*60)

def validate_import(import_type):
    """验证导入数据的便捷函数"""
    validator = DataValidator()
    return validator.validate_import_data(import_type)

def validate_all_imports():
    """验证所有导入数据"""
    validator = DataValidator()
    return validator.validate_all_data()

if __name__ == "__main__":
    # 用于测试的验证函数
    import sys
    
    if len(sys.argv) > 1:
        import_type = sys.argv[1]
        validate_import(import_type)
    else:
        validate_all_imports()