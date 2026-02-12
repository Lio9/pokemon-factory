#!/usr/bin/env python3
"""
技能导入脚本
导入宝可梦游戏中的所有技能
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
logger = init_logger("D:\\learn\\pokemon-factory\\logs\\move_import.log")

class MoveImporter:
    """技能导入器"""
    
    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
    
    async def clear_moves(self):
        """清空技能表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 禁用外键检查以避免约束冲突
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            
            # 清空技能表
            cursor.execute("TRUNCATE TABLE move")
            
            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            
            cursor.close()
            conn.close()
            
            logger.info("清空表 move 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 move 失败: {e}")
            return False
    
    async def import_moves(self):
        """导入技能数据 - 优化版本"""
        logger.info("导入技能数据...")
        try:
            # 先清空表
            await self.clear_moves()
            
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 从PokeAPI获取技能数据
            async with aiohttp.ClientSession() as session:
                # 获取技能总数
                data = await fetch_with_retry(session, f"{self.pokeyapi_base_url}move/?limit=1")
                if not data:
                    logger.error("无法获取技能总数")
                    return False
                    
                total = data.get('count', 0)
                logger.info(f"总共 {total} 个技能")
                
                # 使用更大的批次来减少请求数量
                batch_size = 50
                successful_imports = 0
                failed_imports = 0
                
                # 分批获取技能
                for offset in range(0, total, batch_size):
                    end_offset = min(offset + batch_size, total)
                    data = await fetch_with_retry(
                        session, 
                        f"{self.pokeyapi_base_url}move/?limit={batch_size}&offset={offset}"
                    )
                    if not data:
                        continue
                        
                    results = data.get('results', [])
                    if isinstance(results, list):
                        # 并发获取所有技能的详细数据
                        tasks = []
                        for move in results:
                            if isinstance(move, dict) and 'url' in move:
                                task = self.import_single_move(session, move['url'], cursor)
                                tasks.append(task)
                        
                        if tasks:
                            results = await asyncio.gather(*tasks, return_exceptions=True)
                            for result in results:
                                if isinstance(result, Exception):
                                    failed_imports += 1
                                else:
                                    successful_imports += 1
                
                conn.commit()
                cursor.close()
                conn.close()
                
                logger.info(f"技能数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
                return True
        except Exception as e:
            logger.error(f"导入技能数据失败: {e}")
            return False
    
    async def import_single_move(self, session, move_url, cursor):
        """导入单个技能"""
        try:
            move_data = await fetch_with_retry(session, move_url)
            if not move_data:
                return False
            
            # 获取技能名称
            name = move_data.get('name', '').replace('-', '_')
            name_en = move_data.get('name', '')
            
            # 获取中文名称 - 优先使用简体中文
            name_cn = name_en.replace('_', ' ').title()
            names = move_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            name_cn = name_obj.get('name', name_cn)
                            break
                        elif lang_name == 'zh-hant' and name_cn == name_en.replace('_', ' ').title():
                            name_cn = name_obj.get('name', name_cn)
                        elif lang_name == 'zh' and name_cn == name_en.replace('_', ' ').title():
                            name_cn = name_obj.get('name', name_cn)
            
            # 获取日文名称
            name_jp = name_en.replace('_', ' ').title()
            names = move_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取属性ID
            type_id = None
            type_data = move_data.get('type', {})
            if isinstance(type_data, dict):
                type_name = type_data.get('name', '')
                if type_name:
                    cursor.execute("SELECT id FROM type WHERE name_en = %s", (type_name,))
                    type_result = cursor.fetchone()
                    type_id = type_result[0] if type_result else None
            
            # 获取技能描述 - 优先使用简体中文
            description = "暂无描述"
            effect_entries = move_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for i, effect in enumerate(effect_entries):
                    if isinstance(effect, dict):
                        lang_name = effect.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
                        elif lang_name == 'zh-hant' and description == "暂无描述":
                            description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                        elif lang_name == 'zh' and description == "暂无描述":
                            description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
            
            # 如果没有从effect_entries获取到描述，尝试从flavor_text_entries获取
            if description == "暂无描述":
                flavor_text_entries = move_data.get('flavor_text_entries', [])
                if isinstance(flavor_text_entries, list):
                    for entry in flavor_text_entries:
                        if isinstance(entry, dict):
                            lang_name = entry.get('language', {}).get('name', '')
                            if lang_name == 'zh-hans':
                                description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                                break
                            elif lang_name == 'zh-hant' and description == "暂无描述":
                                description = entry.get('flavor_text', '').replace('\n', ' ').strip()
                            elif lang_name == 'zh' and description == "暂无描述":
                                description = entry.get('flavor_text', '').replace('\n', ' ').strip()
            
            # 获取技能效果 - 优先使用简体中文描述中的效果
            effect = ""
            effect_entries = move_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for effect_text in effect_entries:
                    if isinstance(effect_text, dict):
                        lang_name = effect_text.get('language', {}).get('name', '')
                        if lang_name == 'zh-hans':
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
                        elif lang_name == 'zh-hant' and effect == "":
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                        elif lang_name == 'zh' and effect == "":
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
            
            if not effect:
                effect_entries = move_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for effect_text in effect_entries:
                        if isinstance(effect_text, dict) and effect_text.get('language', {}).get('name') == 'en':
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            break
            
            # 从URL中提取ID作为主键
            move_id = int(move_url.rstrip('/').split('/')[-1])
            
            # 保存到数据库，明确指定主键ID
            cursor.execute("""
                INSERT IGNORE INTO move 
                (id, index_number, generation, name, name_en, name_jp, type_id, power, pp, 
                 accuracy, priority, damage_class, description, effect, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (move_id, name, "1", name_cn, name_en, name_jp, type_id,
                  move_data.get('power'), move_data.get('pp'), 
                  move_data.get('accuracy'), move_data.get('priority'),
                  move_data.get('damage_class', {}).get('name'),
                  description, effect,
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            return True
        except Exception as e:
            logger.error(f"技能导入失败: {move_url.split('/')[-1]} - {e}")
            return False

async def main():
    """主函数"""
    importer = MoveImporter()
    await importer.import_moves()

if __name__ == "__main__":
    asyncio.run(main())
