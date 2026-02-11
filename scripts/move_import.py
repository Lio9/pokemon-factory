import asyncio
import mysql.connector
import time
import logging
import requests
from typing import Dict, List, Any
import sys
import os

# 添加上级目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.utils import setup_logging, get_db_config

logger = logging.getLogger(__name__)

class MoveImportService:
    def __init__(self):
        self.db_config = get_db_config()
    
    async def import_single_move(self, move_id: str, move_data: Dict[str, Any]):
        """导入单个技能"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()
            
            # 获取技能数据
            name = move_data.get('name', '').replace('-', '_')
            name_cn = name.replace('_', ' ').title()
            name_en = name
            name_jp = name_en
            
            # 获取中文名称
            names = move_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'zh':
                        name_cn = name_obj.get('name', name_cn)
                        break
            
            # 获取日文名称
            names = move_data.get('names', [])
            if isinstance(names, list):
                for name_obj in names:
                    if isinstance(name_obj, dict) and name_obj.get('language', {}).get('name') == 'ja':
                        name_jp = name_obj.get('name', name_jp)
                        break
            
            # 获取类型ID
            type_id = None
            type_data = move_data.get('type', {})
            if isinstance(type_data, dict):
                type_name = type_data.get('name', '')
                type_result = cursor.execute("SELECT id FROM type WHERE name_en = %s", (type_name,))
                type_result = cursor.fetchone()
                type_id = type_result[0] if type_result else None
            
            # 获取技能描述
            description = "暂无描述"
            effect_entries = move_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for i, effect in enumerate(effect_entries):
                    if isinstance(effect, dict):
                        lang_name = effect.get('language', {}).get('name', '')
                        if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                            description = effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            logger.info(f"✅ 技能 {move_data.get('name')} 中文描述: {description[:100]}...")
                            break
            
            # 获取技能效果
            effect = ""
            effect_entries = move_data.get('effect_entries', [])
            if isinstance(effect_entries, list):
                for effect_text in effect_entries:
                    if isinstance(effect_text, dict):
                        lang_name = effect_text.get('language', {}).get('name', '')
                        if lang_name in ['zh-hans', 'zh-hant', 'zh']:
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            logger.info(f"✅ 技能 {move_data.get('name')} 中文效果: {effect[:100]}...")
                            break
            
            # 如果没有中文，使用英文作为fallback
            if not effect:
                effect_entries = move_data.get('effect_entries', [])
                if isinstance(effect_entries, list):
                    for effect_text in effect_entries:
                        if isinstance(effect_text, dict) and effect_text.get('language', {}).get('name') == 'en':
                            effect = effect_text.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                            logger.info(f"⚠️ 技能 {move_data.get('name')} 英文效果: {effect[:100]}...")
                            break
            
            # 如果没有英文效果，使用第一个语言作为fallback
            if not effect and effect_entries:
                first_effect = effect_entries[0]
                if isinstance(first_effect, dict):
                    effect = first_effect.get('effect', '').replace('\n', ' ').replace('  ', ' ').strip()
                    logger.info(f"⚠️ 技能 {move_data.get('name')} fallback效果: {effect[:100]}...")
            
            if not effect:
                logger.warning(f"❌ 技能 {move_data.get('name')} 没有获取到效果")
            
            # 保存到数据库
            cursor.execute("""
                INSERT IGNORE INTO move 
                (index_number, generation, name, name_en, name_jp, type_id, power, pp, 
                 accuracy, priority, damage_class, description, effect, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (name, "1", name_cn, name_en, name_jp, type_id,
                  move_data.get('power'), move_data.get('pp'), 
                  move_data.get('accuracy'), move_data.get('priority'),
                  move_data.get('damage_class', {}).get('name'),
                  description, effect,
                  time.strftime('%Y-%m-%d %H:%M:%S'),
                  time.strftime('%Y-%m-%d %H:%M:%S')))
            
            conn.commit()
            cursor.close()
            conn.close()
            return True
            
        except Exception as e:
            logger.error(f"技能导入失败: {move_id}")
            return False
    
    async def import_all_moves(self):
        """导入所有技能"""
        logger.info("开始导入技能数据...")
        try:
            # 获取技能总数
            response = requests.get('https://pokeapi.co/api/v2/move/?limit=1')
            total = response.json().get('count', 0)
            logger.info(f"技能总数: {total}")
            
            # 分批获取技能
            limit = 20
            offset = 0
            success_count = 0
            fail_count = 0
            
            while offset < total:
                response = requests.get(f'https://pokeapi.co/api/v2/move/?limit={limit}&offset={offset}')
                moves_data = response.json().get('results', [])
                
                for move in moves_data:
                    try:
                        move_response = requests.get(move['url'])
                        move_detail = move_response.json()
                        
                        if await self.import_single_move(move['name'], move_detail):
                            success_count += 1
                        else:
                            fail_count += 1
                    except Exception as e:
                        logger.error(f"处理技能 {move['name']} 时出错: {e}")
                        fail_count += 1
                
                offset += limit
                logger.info(f"进度: {offset}/{total}")
            
            logger.info(f"技能导入完成 - 成功: {success_count}, 失败: {fail_count}")
            return True
            
        except Exception as e:
            logger.error(f"导入技能数据失败: {e}")
            return False

async def main():
    setup_logging()
    service = MoveImportService()
    await service.import_all_moves()

if __name__ == "__main__":
    asyncio.run(main())