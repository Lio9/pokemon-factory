#!/usr/bin/env python3
"""
高效宝可梦导入脚本
使用更粗粒度的批次和更高效的并发处理来大幅提高导入速度
"""

import asyncio
import aiohttp
import mysql.connector
import time
import logging
import json
import os
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志 - 确保在项目根目录创建logs文件夹
import os
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_file = os.path.join(project_root, "logs", "efficient_pokemon_import.log")
logger = init_logger(log_file)


class EfficientPokemonImporter:
    """
    高效宝可梦导入器

    该类负责从PokeAPI高效导入宝可梦数据到MySQL数据库。
    支持并发处理、智能重试、批量导入等优化功能。

    主要特性：
    - 高并发网络请求（最多50个并发连接）
    - 智能重试机制（最多8次重试）
    - 批量数据库操作（每批200个宝可梦）
    - 详细的进度日志和错误处理

    使用示例：
        importer = EfficientPokemonImporter()
        await importer.import_all_pokemon()
    """

    def __init__(self):
        self.db_config = get_db_config()
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.batch_size = 500  # 大幅增加批次大小
        self.max_concurrent = 100  # 增加并发数提高导入速度
        self.max_retries = 8  # 增加重试次数
        self.retry_delay = 2  # 基础重试延迟（秒）

    async def import_all_pokemon(self):
        """
        导入所有宝可梦数据 - 高性能版本

        执行完整的导入流程：
        1. 清空相关数据库表
        2. 生成所有宝可梦ID列表
        3. 分批并发导入数据
        4. 监控导入进度和错误

        导入流程：
        - 清空pokemon、pokemon_form、ability等表
        - 使用aiohttp创建高效的HTTP会话
        - 并发请求PokeAPI获取数据
        - 批量插入数据库减少交互次数
        - 实时记录导入进度和错误信息

        返回值：
            bool: 导入成功返回True，失败返回False
        """
        logger.info("🚀 开始导入所有宝可梦数据 - 高性能版本")

        try:
            # 创建HTTP会话
            connector = aiohttp.TCPConnector(limit=200, limit_per_host=100)
            timeout = aiohttp.ClientTimeout(total=60)

            async with aiohttp.ClientSession(
                connector=connector, timeout=timeout
            ) as session:
                # 先清空表
                await self.clear_pokemon()

                # 获取所有宝可梦ID列表
                pokemon_ids = await self.get_all_pokemon_ids(session)
                if not pokemon_ids:
                    logger.error("无法获取宝可梦ID列表")
                    return False

                total = len(pokemon_ids)
                logger.info(f"总共 {total} 个宝可梦，开始并发导入")

                # 分批处理
                successful_imports = 0
                failed_imports = 0

                for i in range(0, total, self.batch_size):
                    batch = pokemon_ids[i : i + self.batch_size]
                    logger.info(
                        f"处理批次 {i//self.batch_size + 1}/{(total + self.batch_size - 1)//self.batch_size}: 宝可梦 {batch[0]}-{batch[-1]}"
                    )

                    # 并发获取当前批次的宝可梦数据
                    batch_results = await self.process_pokemon_batch(session, batch)

                    # 统计结果
                    for result in batch_results:
                        if isinstance(result, Exception):
                            failed_imports += 1
                            logger.error(f"导入宝可梦失败: {result}")
                        elif result:
                            successful_imports += 1

                    logger.info(
                        f"批次完成 - 成功: {successful_imports}, 失败: {failed_imports}"
                    )

                logger.info(
                    f"✅ 所有宝可梦数据导入完成 - 成功: {successful_imports}, 失败: {failed_imports}"
                )
                
                # 验证导入的数据
                logger.info("🔍 验证导入的宝可梦数据...")
                from data_validator import validate_import
                validation_result = validate_import("pokemon")
                
                if validation_result.get("status") == "success":
                    logger.info("✅ 宝可梦数据验证通过")
                    return True
                else:
                    logger.error("❌ 宝可梦数据验证失败")
                    return False

        except Exception as e:
            logger.error(f"导入宝可梦数据失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=1025):
        """
        从本地JSON文件导入宝可梦数据
        
        Args:
            start_id: 起始ID
            end_id: 结束ID (1-1025)
        """
        import json
        import os
        
        logger.info(f"开始从本地导入宝可梦数据: ID {start_id}-{end_id}")
        
        # 获取项目根目录
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        data_dir = os.path.join(project_root, "data", "local", "pokemon")
        
        # 检查本地数据目录
        if not os.path.exists(data_dir):
            logger.error(f"本地数据目录不存在: {data_dir}")
            return False
        
        # 清空表
        await self.clear_pokemon()
        
        # 导入本地数据
        successful_imports = 0
        failed_imports = 0
        
        for i in range(start_id, min(end_id + 1, 1026)):
            try:
                # 构建文件路径
                filename = f"pokemon_{i:04d}.json"
                filepath = os.path.join(data_dir, filename)
                
                # 检查文件是否存在
                if not os.path.exists(filepath):
                    logger.warning(f"本地文件不存在: {filepath}")
                    failed_imports += 1
                    continue
                
                # 读取本地JSON文件
                with open(filepath, 'r', encoding='utf-8') as f:
                    pokemon_data = json.load(f)
                
                # 获取宝可梦物种数据
                species_data = None
                species_filename = f"species_{i:04d}.json"
                species_filepath = os.path.join(data_dir.replace("pokemon", "species"), species_filename)
                
                if os.path.exists(species_filepath):
                    with open(species_filepath, 'r', encoding='utf-8') as f:
                        species_data = json.load(f)
                else:
                    # 如果没有物种文件，尝试从网络获取
                    try:
                        species_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}pokemon-species/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                    except:
                        pass
                
                # 转换并导入数据
                pokemon_info = self.convert_pokemon_data(pokemon_data, species_data)
                if pokemon_info:
                    await self.batch_insert_pokemon(pokemon_info, self.max_retries)
                    successful_imports += 1
                else:
                    failed_imports += 1
                    logger.error(f"转换宝可梦数据失败: {i}")
                
                if (i - start_id + 1) % 100 == 0:
                    logger.info(f"已导入 {i - start_id + 1}/{end_id - start_id + 1} 个宝可梦")
                    
            except Exception as e:
                failed_imports += 1
                logger.error(f"导入宝可梦 {i} 失败: {e}")
        
        logger.info(f"✅ 本地导入完成 - 成功: {successful_imports}, 失败: {failed_imports}")
        return successful_imports > 0

    async def get_all_pokemon_ids(self, session):
        """获取所有宝可梦ID列表"""
        try:
            # 宝可梦总数应该是1350
            total = 1350
            logger.info(f"总共 {total} 个宝可梦")

            # 直接生成1-1350的ID列表
            all_ids = list(range(1, total + 1))

            # 由于网络验证可能有问题，直接返回所有ID
            logger.info(f"使用所有 {len(all_ids)} 个宝可梦ID")

            return all_ids
        except Exception as e:
            logger.error(f"获取宝可梦ID列表失败: {e}")
            # 降级方案：使用1-1350
            return list(range(1, 1351))

    async def process_pokemon_batch(self, session, pokemon_ids):
        """处理一批宝可梦"""
        semaphore = asyncio.Semaphore(self.max_concurrent)
        tasks = []

        for pokemon_id in pokemon_ids:
            task = self.import_single_pokemon(session, pokemon_id, semaphore)
            tasks.append(task)

        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 统计成功和失败的数量
        successful = sum(1 for r in results if r is True)
        failed = len(results) - successful

        # 只在有大量失败时才记录日志
        if failed > 50:
            logger.info(f"批次处理完成: 成功 {successful}, 失败 {failed}")
        elif failed > 0:
            logger.debug(f"批次处理完成: 成功 {successful}, 失败 {failed}")

        return results

    async def import_single_pokemon(self, session, pokemon_id, semaphore):
        """导入单个宝可梦"""
        async with semaphore:
            for retry in range(self.max_retries):
                try:
                    # 获取宝可梦数据
                    pokemon_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}pokemon/{pokemon_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    if not pokemon_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(
                                f"宝可梦 {pokemon_id} 数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒"
                            )
                            await asyncio.sleep(wait_time)
                            continue
                        # 对于404错误，只在最后重试时记录警告
                        if retry == self.max_retries - 1:
                            logger.warning(f"宝可梦 {pokemon_id} 数据为空，跳过导入")
                        return False

                    # 获取宝可梦物种数据
                    species_data = await fetch_with_retry(
                        session,
                        f"{self.pokeyapi_base_url}pokemon-species/{pokemon_id}/",
                        max_retries=self.max_retries,
                        timeout=30,
                    )
                    if not species_data:
                        if retry < self.max_retries - 1:
                            wait_time = self.retry_delay * (2**retry)
                            logger.debug(
                                f"宝可梦 {pokemon_id} 物种数据为空，第 {retry + 1} 次重试，等待 {wait_time} 秒"
                            )
                            await asyncio.sleep(wait_time)
                            continue
                        # 对于404错误，只在最后重试时记录警告
                        if retry == self.max_retries - 1:
                            logger.warning(
                                f"宝可梦 {pokemon_id} 物种数据为空，跳过导入"
                            )
                        return False
                        return False

                    # 转换数据
                    pokemon_info = self.convert_pokemon_data(pokemon_data, species_data)
                    if not pokemon_info:
                        logger.error(f"宝可梦 {pokemon_id} 数据转换失败")
                        return False

                    # 批量插入数据库
                    await self.batch_insert_pokemon(pokemon_info, self.max_retries)
                    return True

                except Exception as e:
                    logger.error(f"宝可梦导入失败: {pokemon_id} - {e}")
                    if retry < self.max_retries - 1:
                        wait_time = self.retry_delay * (2**retry)
                        logger.debug(
                            f"宝可梦 {pokemon_id} 导入异常，第 {retry + 1} 次重试，等待 {wait_time} 秒"
                        )
                        await asyncio.sleep(wait_time)
                    else:
                        return e

            logger.error(f"宝可梦 {pokemon_id} 在 {self.max_retries} 次尝试后仍然失败")
            return False

    def convert_pokemon_data(self, pokemon_data, species_data):
        """转换宝可梦数据"""
        try:
            # 获取基本信息 - 使用正确的宝可梦编号
            pokemon_id = pokemon_data.get("id", 0)  # 从URL中提取的ID
            index_number = f"{pokemon_id:04d}"  # 使用URL中的ID作为编号
            name = self.get_proper_name(species_data)  # 获取正确的中文名称
            name_en = pokemon_data.get("name", "")
            name_jp = self.get_japanese_name(species_data)
            height = pokemon_data.get("height") / 10.0
            weight = pokemon_data.get("weight") / 10.0
            base_experience = pokemon_data.get("base_experience", 0)
            capture_rate = self.get_capture_rate(species_data)
            gender_rate = self.get_gender_rate(species_data)
            evolution_chain_id = self.get_evolution_chain_id(species_data)
            sort_order = species_data.get("order", 0)
            profile = self.get_proper_description(species_data)

            # 获取宝可梦形态
            pokemon_forms = []
            for form_info in pokemon_data.get("forms", []):
                form_name = form_info.get("name", "")
                pokemon_forms.append({"form_name": form_name})

            # 获取宝可梦特性
            pokemon_abilities = []
            for ability_info in pokemon_data.get("abilities", []):
                ability_name = ability_info.get("ability", {}).get("name", "")
                pokemon_abilities.append(
                    {
                        "ability_id": 1,  # 占位符
                        "ability_name": ability_name,
                        "ability_description": "",  # 占位符
                        "ability_effect": "",  # 占位符
                        "is_hidden": ability_info.get("is_hidden", False),
                        "slot": ability_info.get("slot", 0),
                    }
                )

            # 获取宝可梦类型
            pokemon_types = []
            for type_info in pokemon_data.get("types", []):
                type_name = type_info.get("type", {}).get("name", "")
                slot = type_info.get("slot", 0)
                pokemon_types.append({"type_name": type_name, "slot": slot})

            # 获取宝可梦个体值
            pokemon_ivs = []
            for iv_info in pokemon_data.get("stats", []):
                stat_name = iv_info.get("stat", {}).get("name", "")
                base_stat = iv_info.get("base_stat", 0)
                pokemon_ivs.append({"stat_name": stat_name, "base_stat": base_stat})

            return {
                "index_number": index_number,
                "name": name,
                "name_en": name_en,
                "name_jp": name_jp,
                "height": height,
                "weight": weight,
                "base_experience": base_experience,
                "capture_rate": capture_rate,
                "gender_rate": gender_rate,
                "evolution_chain_id": evolution_chain_id,
                "sort_order": sort_order,
                "profile": profile,
                "pokemon_forms": pokemon_forms,
                "pokemon_abilities": pokemon_abilities,
                "pokemon_types": pokemon_types,
                "pokemon_ivs": pokemon_ivs,
            }
        except Exception as e:
            logger.error(f"转换宝可梦数据失败: {e}")
            return None

    def get_japanese_name(self, species_data):
        """获取日文名称"""
        try:
            names = species_data.get("names", [])
            for name_obj in names:
                if (
                    isinstance(name_obj, dict)
                    and name_obj.get("language", {}).get("name") == "ja"
                ):
                    return name_obj.get("name", "")
            return name_en.replace("-", " ").title()
        except Exception as e:
            logger.error(f"获取日文名称失败: {e}")
            return ""

    def get_capture_rate(self, species_data):
        """获取捕获率"""
        try:
            return species_data.get("capture_rate", 45)
        except Exception as e:
            logger.error(f"获取捕获率失败: {e}")
            return 45

    def get_gender_rate(self, species_data):
        """获取性别比例"""
        try:
            gender_rate = species_data.get("gender_rate", -1)
            if gender_rate == -1:
                return 87.5
            return gender_rate
        except Exception as e:
            logger.error(f"获取性别比例失败: {e}")
            return 87.5

    def get_evolution_chain_id(self, species_data):
        """获取进化链ID"""
        try:
            evolution_chain_url = species_data.get("evolution_chain", {}).get("url", "")
            if evolution_chain_url:
                return int(evolution_chain_url.rstrip("/").split("/")[-1])
            return ""
        except Exception as e:
            logger.error(f"获取进化链ID失败: {e}")
            return ""

    def get_proper_name(self, species_data):
        """获取正确的中文名称"""
        try:
            names = species_data.get("names", [])
            if isinstance(names, list):
                # 优先使用中文（简体）
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get("language", {}).get("name", "")
                        if lang_name == "zh-hans":
                            return name_obj.get("name", "").replace("-", "_")

                # 备选：繁体中文
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get("language", {}).get("name", "")
                        if lang_name == "zh-hant":
                            return name_obj.get("name", "").replace("-", "_")

                # 备选：通用中文
                for name_obj in names:
                    if isinstance(name_obj, dict):
                        lang_name = name_obj.get("language", {}).get("name", "")
                        if lang_name == "zh":
                            return name_obj.get("name", "").replace("-", "_")

                # 最后尝试英文转中文
                name_en = species_data.get("name", "")
                return name_en.replace("-", "_")

            # 如果没有名称数据，使用物种名称
            name_en = species_data.get("name", "")
            return name_en.replace("-", "_")

        except Exception as e:
            logger.error(f"获取中文名称失败: {e}")
            name_en = species_data.get("name", "")
            return name_en.replace("-", "_")

    def get_proper_description(self, species_data):
        """获取正确的描述信息"""
        try:
            flavor_text_entries = species_data.get("flavor_text_entries", [])
            if isinstance(flavor_text_entries, list):
                # 优先使用中文
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get("language", {}).get("name", "")
                        if lang_name == "zh-hans":
                            description = (
                                entry.get("flavor_text", "").replace("\n", " ").strip()
                            )
                            if description and description != "???":
                                return description

                # 备选：繁体中文
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get("language", {}).get("name", "")
                        if lang_name == "zh-hant":
                            description = (
                                entry.get("flavor_text", "").replace("\n", " ").strip()
                            )
                            if description and description != "???":
                                return description

                # 备选：通用中文
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get("language", {}).get("name", "")
                        if lang_name == "zh":
                            description = (
                                entry.get("flavor_text", "").replace("\n", " ").strip()
                            )
                            if description and description != "???":
                                return description

            # 最后使用英文描述
            flavor_text_entries = species_data.get("flavor_text_entries", [])
            if isinstance(flavor_text_entries, list):
                for entry in flavor_text_entries:
                    if isinstance(entry, dict):
                        lang_name = entry.get("language", {}).get("name", "")
                        if lang_name == "en":
                            description = (
                                entry.get("flavor_text", "").replace("\n", " ").strip()
                            )
                            if description and description != "???":
                                return description

            return "暂无描述"
        except Exception as e:
            logger.error(f"获取描述失败: {e}")
            return "暂无描述"

    async def batch_insert_pokemon(self, pokemon_info, max_retries=8):
        """批量插入宝可梦数据"""
        conn = None
        cursor = None
        try:
            from utils import DatabaseManager

            db_manager = DatabaseManager()
            conn = await db_manager.get_connection()
            cursor = conn.cursor(dictionary=True)

            # 启用批量模式
            cursor.execute("SET autocommit = 0")

            # 插入主表数据
            insert_sql = """
                INSERT IGNORE INTO pokemon 
                (id, index_number, name, name_en, name_jp, height, weight, base_experience, capture_rate, gender_rate, evolution_chain_id, `order`, profile, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """

            # 准备主表数据
            values = (
                int(pokemon_info["index_number"]),
                pokemon_info["index_number"],
                pokemon_info["name"],
                pokemon_info["name_en"],
                pokemon_info["name_jp"],
                pokemon_info["height"],
                pokemon_info["weight"],
                pokemon_info["base_experience"],
                pokemon_info["capture_rate"],
                pokemon_info["gender_rate"],
                pokemon_info["evolution_chain_id"],
                pokemon_info["sort_order"],
                pokemon_info["profile"],
                time.strftime("%Y-%m-%d %H:%M:%S"),
                time.strftime("%Y-%m-%d %H:%M:%S"),
            )

            # 执行主表插入
            cursor.execute(insert_sql, values)
            cursor.execute("COMMIT")
            pokemon_id = int(pokemon_info["index_number"])

            # 插入宝可梦形态表
            for form_info in pokemon_info.get("pokemon_forms", []):
                # 先插入形态到pokemon_form表
                cursor.execute(
                    """
                    INSERT IGNORE INTO pokemon_form 
                    (pokemon_id, name, index_number, form_name, form_name_jp, is_default, is_battle_only, is_mega, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                    (
                        int(pokemon_info["index_number"]),
                        form_info["form_name"],
                        f"{int(pokemon_info['index_number']):04d}",
                        form_info["form_name"],
                        form_info["form_name"],
                        1 if form_info.get("is_default", False) else 0,
                        1 if form_info.get("is_battle_only", False) else 0,
                        1 if form_info.get("is_mega", False) else 0,
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )

                # 获取形态ID
                form_id = cursor.lastrowid

                # 插入形态特性表
                for ability_info in pokemon_info.get("pokemon_abilities", []):
                    # 先插入特性到ability表
                    cursor.execute(
                        """
                        INSERT IGNORE INTO ability 
                        (index_number, name, name_en, name_jp, description, effect, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                        (
                            f"{ability_info.get('ability_id', 1):04d}",
                            ability_info["ability_name"],
                            ability_info["ability_name"],
                            ability_info["ability_name"],
                            ability_info.get("ability_description", ""),
                            ability_info.get("ability_effect", ""),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                        ),
                    )

                    # 获取特性ID
                    ability_id = cursor.lastrowid

                    cursor.execute(
                        """
                        INSERT IGNORE INTO pokemon_form_ability 
                        (form_id, ability_id, is_hidden, slot, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                        (
                            form_id,
                            ability_id,
                            ability_info["is_hidden"],
                            ability_info["slot"],
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                        ),
                    )

                # 插入形态类型表
                for type_info in pokemon_info.get("pokemon_types", []):
                    # 先插入类型到type表
                    cursor.execute(
                        """
                        INSERT IGNORE INTO type 
                        (name, name_en, name_jp, color, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                        (
                            type_info["type_name"],
                            type_info["type_name"],
                            type_info["type_name"],
                            type_info.get("color", "#FFFFFF"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                        ),
                    )

                    # 获取类型ID
                    type_id = cursor.lastrowid

                    cursor.execute(
                        """
                        INSERT IGNORE INTO pokemon_form_type 
                        (form_id, type_id, slot, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s)
                    """,
                        (
                            form_id,
                            type_id,
                            type_info["slot"],
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                        ),
                    )

                # 插入形态个体值表
                for iv_info in pokemon_info.get("pokemon_ivs", []):
                    cursor.execute(
                        """
                        INSERT IGNORE INTO pokemon_iv 
                        (pokemon_form_id, hp, attack, defense, sp_attack, sp_defense, speed, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                        (
                            form_id,
                            iv_info.get("hp", 0),
                            iv_info.get("attack", 0),
                            iv_info.get("defense", 0),
                            iv_info.get("sp_attack", 0),
                            iv_info.get("sp_defense", 0),
                            iv_info.get("speed", 0),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                            time.strftime("%Y-%m-%d %H:%M:%S"),
                        ),
                    )

            # 插入宝可梦特性表
            for ability_info in pokemon_info.get("pokemon_abilities", []):
                # 先插入特性到ability表
                cursor.execute(
                    """
                    INSERT IGNORE INTO ability 
                    (index_number, name, name_en, name_jp, description, effect, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """,
                    (
                        f"{ability_info['ability_id']:04d}",
                        ability_info["ability_name"],
                        ability_info["ability_name"],
                        ability_info["ability_name"],
                        ability_info["ability_description"],
                        ability_info["ability_effect"],
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )

                # 获取特性ID
                ability_id = cursor.lastrowid

                cursor.execute(
                    """
                    INSERT IGNORE INTO pokemon_form_ability 
                    (form_id, ability_id, is_hidden, slot, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """,
                    (
                        int(pokemon_info["index_number"]),
                        ability_id,
                        ability_info["is_hidden"],
                        ability_info["slot"],
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )

            # 插入宝可梦类型表
            for type_info in pokemon_info.get("pokemon_types", []):
                # 先插入类型到type表
                cursor.execute(
                    """
                    INSERT IGNORE INTO type 
                    (name, name_en, name_jp, color, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """,
                    (
                        type_info["type_name"],
                        type_info["type_name"],
                        type_info["type_name"],
                        type_info.get("color", "#FFFFFF"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )

                # 获取类型ID
                type_id = cursor.lastrowid

                cursor.execute(
                    """
                    INSERT IGNORE INTO pokemon_form_type 
                    (form_id, type_id, slot, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s)
                """,
                    (
                        int(pokemon_info["index_number"]),
                        type_id,
                        type_info["slot"],
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )
            # 插入宝可梦个体值表
            for iv_info in pokemon_info.get("pokemon_ivs", []):
                cursor.execute(
                    """
                    INSERT IGNORE INTO pokemon_iv 
                    (pokemon_form_id, hp, attack, defense, sp_attack, sp_defense, speed, created_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                    (
                        int(pokemon_info["index_number"]),
                        iv_info.get("hp", 0),
                        iv_info.get("attack", 0),
                        iv_info.get("defense", 0),
                        iv_info.get("sp_attack", 0),
                        iv_info.get("sp_defense", 0),
                        iv_info.get("speed", 0),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                        time.strftime("%Y-%m-%d %H:%M:%S"),
                    ),
                )

            conn.commit()
            logger.debug(
                f"成功插入宝可梦: {pokemon_info['name']} (ID: {int(pokemon_info['index_number'])})"
            )

        except Exception as e:
            if conn:
                conn.rollback()
            logger.error(f"批量插入宝可梦数据失败: {e}")
            raise
        finally:
            if cursor:
                try:
                    cursor.close()
                except:
                    pass
            if conn:
                try:
                    from utils import DatabaseManager

                    db_manager = DatabaseManager()
                    await db_manager.release_connection(conn)
                except:
                    try:
                        conn.close()
                    except:
                        pass

    async def clear_pokemon(self):
        """清空宝可梦表"""
        try:
            conn = mysql.connector.connect(**self.db_config)
            cursor = conn.cursor()

            # 禁用外键检查以避免约束冲突
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

            # 清空宝可梦表
            cursor.execute("TRUNCATE TABLE pokemon")

            # 重新启用外键检查
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()

            cursor.close()
            conn.close()

            logger.info("清空表 pokemon 完成")
            return True
        except Exception as e:
            logger.error(f"清空表 pokemon 失败: {e}")
            return False
    
    async def import_from_local_data(self, start_id=1, end_id=1025):
        """
        从本地JSON文件导入宝可梦数据
        
        Args:
            start_id: 起始ID
            end_id: 结束ID (1-1025)
        """
        import mysql.connector
        import json
        
        logger.info(f"开始从本地导入宝可梦数据: ID {start_id}-{end_id}")
        
        # 获取项目根目录
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        data_dir = os.path.join(project_root, "data")
        # pokemon_species文件直接在data文件夹下，不在子文件夹中
        species_dir = data_dir
        
        # 修改数据库连接配置为远程数据库
        remote_db_config = {
            'host': '10.144.55.168',
            'port': 3306,
            'user': 'root',
            'password': '753951',
            'database': 'pokemon_factory',
            'charset': 'utf8mb4'
        }
        
        # 检查数据库连接
        try:
            conn = mysql.connector.connect(**remote_db_config)
            cursor = conn.cursor(dictionary=True)
        except Exception as e:
            logger.error(f"数据库连接失败: {str(e)}")
            return False
        
        success_count = 0
        total_count = end_id - start_id + 1
        
        for i in range(start_id, end_id + 1):
            try:
                # 构建文件路径
                species_file = os.path.join(species_dir, f"pokemon_species_{i}.json")
                
                # 检查文件是否存在
                if not os.path.exists(species_file):
                    logger.warning(f"宝可梦物种数据文件不存在: {species_file}")
                    continue
                
                # 读取JSON文件
                with open(species_file, 'r', encoding='utf-8') as f:
                    species_data = json.load(f)
                
                # 提取数据
                name = species_data.get('name', '')
                index_number = f"{i:04d}"
                name_en = name
                height = 0
                weight = 0
                base_experience = 0
                color_name = species_data.get('color', {}).get('name', '') if species_data.get('color') else ''
                shape_name = species_data.get('shape', {}).get('name', '') if species_data.get('shape') else ''
                evolution_chain_id = self.get_evolution_chain_id(species_data)
                
                # 插入或更新pokemon表
                cursor.execute("SELECT id FROM pokemon WHERE id = %s", (i,))
                if cursor.fetchone():
                    # 更新现有记录
                    cursor.execute("""
                        UPDATE pokemon SET 
                            index_number = %s,
                            name = %s, 
                            name_en = %s,
                            height = %s, 
                            weight = %s,
                            base_experience = %s,
                            species_id = %s,
                            evolution_chain_id = %s
                        WHERE id = %s
                    """, (index_number, name, name_en, height, weight, base_experience, i, evolution_chain_id, i))
                else:
                    # 插入新记录
                    cursor.execute("""
                        INSERT INTO pokemon (id, index_number, name, name_en, height, weight, base_experience, species_id, evolution_chain_id)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """, (i, index_number, name, name_en, height, weight, base_experience, i, evolution_chain_id))
                
                # 插入或更新pokemon_species表（如果存在）
                cursor.execute("""
                    SELECT COUNT(*) 
                    FROM information_schema.tables 
                    WHERE table_schema = DATABASE() 
                    AND table_name = 'pokemon_species'
                """)
                count_result = cursor.fetchone()
                table_exists = count_result['COUNT(*)'] > 0 if isinstance(count_result, dict) else count_result[0] > 0
                
                if table_exists:
                    cursor.execute("SELECT id FROM pokemon_species WHERE id = %s", (i,))
                    if cursor.fetchone():
                        # 更新现有记录
                        evolution_chain_id = self.get_evolution_chain_id(species_data)
                        cursor.execute("""
                            UPDATE pokemon_species SET 
                                name = %s, 
                                color_id = %s,
                                shape_id = %s,
                                evolution_chain_id = %s
                            WHERE id = %s
                        """, (name, color_name, shape_name, evolution_chain_id, i))
                    else:
                        # 插入新记录
                        cursor.execute("""
                            INSERT INTO pokemon_species (id, name, color_id, shape_id)
                            VALUES (%s, %s, %s, %s)
                        """, (i, name, color_name, shape_name))
                
                success_count += 1
                
                # 每处理10个宝可梦提交一次事务
                if success_count % 10 == 0:
                    conn.commit()
                    logger.info(f"已导入 {success_count}/{total_count} 个宝可梦")
                
            except Exception as e:
                import traceback
                error_msg = str(e)
                if error_msg == "0":
                    error_msg = "未知错误"
                logger.error(f"导入宝可梦数据失败 {i}: {error_msg}")
                logger.error(f"详细错误信息: {traceback.format_exc()}")
                continue
        
        # 提交剩余事务
        conn.commit()
        cursor.close()
        conn.close()
        
        logger.info(f"从本地导入完成: 成功 {success_count}/{total_count}")
        return True
    
    async def import_pokemon_species_from_local_data(self, start_id=1, end_id=1025):
        """从本地导入宝可梦物种数据"""
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        data_dir = os.path.join(project_root, "data")
        # pokemon_species文件直接在data文件夹下，不在子文件夹中
        species_dir = data_dir
        
        # 修改数据库连接配置为远程数据库
        remote_db_config = {
            'host': '10.144.55.168',
            'port': 3306,
            'user': 'root',
            'password': '753951',
            'database': 'pokemon_factory',
            'charset': 'utf8mb4'
        }
        
        # 检查数据库连接
        try:
            conn = mysql.connector.connect(**remote_db_config)
            cursor = conn.cursor(dictionary=True)
        except Exception as e:
            logger.error(f"数据库连接失败: {str(e)}")
            return False
        
        success_count = 0
        total_count = end_id - start_id + 1
        
        for i in range(start_id, end_id + 1):
            try:
                # 构建文件路径
                species_file = os.path.join(species_dir, f"pokemon_species_{i}.json")
                
                # 检查文件是否存在
                if not os.path.exists(species_file):
                    logger.warning(f"宝可梦物种数据文件不存在: {species_file}")
                    continue
                
                # 读取JSON文件
                with open(species_file, 'r', encoding='utf-8') as f:
                    species_data = json.load(f)
                
                # 提取数据
                name = species_data.get('name', '')
                name_en = name
                color_name = species_data.get('color', {}).get('name', '') if species_data.get('color') else ''
                shape_name = species_data.get('shape', {}).get('name', '') if species_data.get('shape') else ''
                
                # 插入或更新pokemon_species表
                cursor.execute("SELECT id FROM pokemon_species WHERE id = %s", (i,))
                if cursor.fetchone():
                    # 更新现有记录
                    evolution_chain_id = self.get_evolution_chain_id(species_data)
                    cursor.execute("""
                        UPDATE pokemon_species SET 
                            name = %s, 
                            name_en = %s,
                            color_id = %s,
                            shape_id = %s,
                            evolution_chain_id = %s
                        WHERE id = %s
                    """, (name, name_en, color_name, shape_name, evolution_chain_id, i))
                else:
                    # 插入新记录
                    evolution_chain_id = self.get_evolution_chain_id(species_data)
                    cursor.execute("""
                        INSERT INTO pokemon_species (id, name, name_en, color_id, shape_id, evolution_chain_id)
                        VALUES (%s, %s, %s, %s, %s, %s)
                    """, (i, name, name_en, color_name, shape_name, evolution_chain_id))
                
                success_count += 1
                
                # 每处理10个宝可梦提交一次事务
                if success_count % 10 == 0:
                    conn.commit()
                    logger.info(f"已导入 {success_count}/{total_count} 个宝可梦物种")
                
            except Exception as e:
                error_msg = str(e)
                if error_msg == "0":
                    error_msg = "未知错误"
                logger.error(f"导入宝可梦物种数据失败 {i}: {error_msg}")
                continue
        
        # 提交剩余事务
        conn.commit()
        cursor.close()
        conn.close()
        
        logger.info(f"从本地导入完成: 成功 {success_count}/{total_count} 个宝可梦物种")
        return True


async def main():
    """主函数"""
    importer = EfficientPokemonImporter()
    
    # 首先导入pokemon_species数据
    logger.info("开始导入pokemon_species数据...")
    await importer.import_pokemon_species_from_local_data()
    
    # 然后导入pokemon数据
    logger.info("开始导入pokemon数据...")
    await importer.import_from_local_data()


if __name__ == "__main__":
    asyncio.run(main())
