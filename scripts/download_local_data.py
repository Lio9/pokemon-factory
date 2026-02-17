#!/usr/bin/env python3
"""
本地数据下载脚本
将PokeAPI数据下载到本地JSON文件，便于调试和重用
"""

import os
import json
import asyncio
import aiohttp
import time
import logging
from datetime import datetime
from utils import get_db_config, init_logger, fetch_with_retry

# 初始化日志
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_file = os.path.join(project_root, "logs", "local_download.log")
logger = init_logger(log_file)

class LocalDataDownloader:
    """本地数据下载器"""
    
    def __init__(self):
        self.pokeyapi_base_url = "https://pokeapi.co/api/v2/"
        self.max_retries = 8
        self.max_concurrent = 20
        
        # 创建本地数据目录
        self.data_dir = os.path.join(project_root, "data", "local")
        os.makedirs(self.data_dir, exist_ok=True)
        
        self.pokemon_dir = os.path.join(self.data_dir, "pokemon")
        self.ability_dir = os.path.join(self.data_dir, "ability")
        self.move_dir = os.path.join(self.data_dir, "move")
        self.type_dir = os.path.join(self.data_dir, "type")
        self.species_dir = os.path.join(self.data_dir, "species")
        
        for dir_path in [self.pokemon_dir, self.ability_dir, self.move_dir, self.type_dir, self.species_dir]:
            os.makedirs(dir_path, exist_ok=True)
    
    async def download_all_data(self):
        """下载所有数据"""
        logger.info("📥 开始下载所有本地数据...")
        
        # 1. 下载类型数据
        await self.download_types()
        
        # 2. 下载特性数据
        await self.download_abilities()
        
        # 3. 下载技能数据
        await self.download_moves()
        
        # 4. 下载宝可梦数据
        await self.download_pokemon()
        
        # 5. 下载宝可梦物种数据
        await self.download_species()
        
        logger.info("✅ 所有数据下载完成!")
    
    async def download_types(self):
        """下载类型数据"""
        logger.info("🎨 下载类型数据...")
        
        try:
            # 类型总数
            total = 18
            
            # 使用aiohttp创建高效的HTTP会话
            connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(1, total + 1):
                    try:
                        type_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}type/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                        
                        if type_data:
                            # 保存到本地文件
                            filename = f"type_{i:03d}.json"
                            filepath = os.path.join(self.type_dir, filename)
                            
                            with open(filepath, 'w', encoding='utf-8') as f:
                                json.dump(type_data, f, ensure_ascii=False, indent=2)
                            
                            if i % 5 == 0:
                                logger.info(f"已下载 {i}/{total} 个类型数据")
                    except Exception as e:
                        logger.error(f"下载类型 {i} 失败: {e}")
            
            logger.info(f"✅ 类型数据下载完成，共 {total} 个文件")
            
        except Exception as e:
            logger.error(f"下载类型数据失败: {e}")
    
    async def download_abilities(self):
        """下载特性数据"""
        logger.info("💎 下载特性数据...")
        
        try:
            # 特性总数
            total = 242
            
            # 使用aiohttp创建高效的HTTP会话
            connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(1, total + 1):
                    try:
                        ability_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}ability/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                        
                        if ability_data:
                            # 保存到本地文件
                            filename = f"ability_{i:03d}.json"
                            filepath = os.path.join(self.ability_dir, filename)
                            
                            with open(filepath, 'w', encoding='utf-8') as f:
                                json.dump(ability_data, f, ensure_ascii=False, indent=2)
                            
                            if i % 50 == 0:
                                logger.info(f"已下载 {i}/{total} 个特性数据")
                    except Exception as e:
                        logger.error(f"下载特性 {i} 失败: {e}")
            
            logger.info(f"✅ 特性数据下载完成，共 {total} 个文件")
            
        except Exception as e:
            logger.error(f"下载特性数据失败: {e}")
    
    async def download_moves(self):
        """下载技能数据"""
        logger.info("⚔️  下载技能数据...")
        
        try:
            # 技能总数
            total = 826
            
            # 使用aiohttp创建高效的HTTP会话
            connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(1, total + 1):
                    try:
                        move_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}move/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                        
                        if move_data:
                            # 保存到本地文件
                            filename = f"move_{i:03d}.json"
                            filepath = os.path.join(self.move_dir, filename)
                            
                            with open(filepath, 'w', encoding='utf-8') as f:
                                json.dump(move_data, f, ensure_ascii=False, indent=2)
                            
                            if i % 100 == 0:
                                logger.info(f"已下载 {i}/{total} 个技能数据")
                    except Exception as e:
                        logger.error(f"下载技能 {i} 失败: {e}")
            
            logger.info(f"✅ 技能数据下载完成，共 {total} 个文件")
            
        except Exception as e:
            logger.error(f"下载技能数据失败: {e}")
    
    async def download_pokemon(self):
        """下载宝可梦数据"""
        logger.info("🎭 下载宝可梦数据...")
        
        try:
            # 宝可梦总数
            total = 1025
            
            # 使用aiohttp创建高效的HTTP会话
            connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(1, total + 1):
                    try:
                        pokemon_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}pokemon/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                        
                        if pokemon_data:
                            # 保存到本地文件
                            filename = f"pokemon_{i:04d}.json"
                            filepath = os.path.join(self.pokemon_dir, filename)
                            
                            with open(filepath, 'w', encoding='utf-8') as f:
                                json.dump(pokemon_data, f, ensure_ascii=False, indent=2)
                            
                            if i % 100 == 0:
                                logger.info(f"已下载 {i}/{total} 个宝可梦数据")
                    except Exception as e:
                        logger.error(f"下载宝可梦 {i} 失败: {e}")
            
            logger.info(f"✅ 宝可梦数据下载完成，共 {total} 个文件")
            
        except Exception as e:
            logger.error(f"下载宝可梦数据失败: {e}")
    
    async def download_species(self):
        """下载宝可梦物种数据"""
        logger.info("🌱 下载宝可梦物种数据...")
        
        try:
            # 物种总数
            total = 1025
            
            # 使用aiohttp创建高效的HTTP会话
            connector = aiohttp.TCPConnector(limit=10, limit_per_host=5)
            timeout = aiohttp.ClientTimeout(total=60)
            
            async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
                for i in range(1, total + 1):
                    try:
                        species_data = await fetch_with_retry(
                            session,
                            f"{self.pokeyapi_base_url}pokemon-species/{i}/",
                            max_retries=self.max_retries,
                            timeout=30,
                        )
                        
                        if species_data:
                            # 保存到本地文件
                            filename = f"species_{i:04d}.json"
                            filepath = os.path.join(self.species_dir, filename)
                            
                            with open(filepath, 'w', encoding='utf-8') as f:
                                json.dump(species_data, f, ensure_ascii=False, indent=2)
                            
                            if i % 100 == 0:
                                logger.info(f"已下载 {i}/{total} 个物种数据")
                    except Exception as e:
                        logger.error(f"下载物种 {i} 失败: {e}")
            
            logger.info(f"✅ 物种数据下载完成，共 {total} 个文件")
            
        except Exception as e:
            logger.error(f"下载物种数据失败: {e}")
    
    def get_local_data_path(self, data_type, index):
        """获取本地数据文件路径"""
        if data_type == "pokemon":
            return os.path.join(self.pokemon_dir, f"pokemon_{index:04d}.json")
        elif data_type == "ability":
            return os.path.join(self.ability_dir, f"ability_{index:03d}.json")
        elif data_type == "move":
            return os.path.join(self.move_dir, f"move_{index:03d}.json")
        elif data_type == "type":
            return os.path.join(self.type_dir, f"type_{index:03d}.json")
        elif data_type == "species":
            return os.path.join(self.species_dir, f"species_{index:04d}.json")
        return None
    
    def load_local_data(self, data_type, index):
        """从本地加载数据"""
        filepath = self.get_local_data_path(data_type, index)
        if filepath and os.path.exists(filepath):
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"加载本地 {data_type} 数据失败: {e}")
        return None
    
    def list_local_files(self, data_type):
        """列出本地数据文件"""
        if data_type == "pokemon":
            dir_path = self.pokemon_dir
        elif data_type == "ability":
            dir_path = self.ability_dir
        elif data_type == "move":
            dir_path = self.move_dir
        elif data_type == "type":
            dir_path = self.type_dir
        elif data_type == "species":
            dir_path = self.species_dir
        else:
            return []
        
        files = []
        if os.path.exists(dir_path):
            for filename in os.listdir(dir_path):
                if filename.endswith('.json'):
                    files.append(filename)
        
        return sorted(files)

async def main():
    """主函数"""
    downloader = LocalDataDownloader()
    await downloader.download_all_data()
    
    # 显示下载统计
    print("\n📊 下载统计:")
    print("=" * 50)
    
    for data_type in ["pokemon", "ability", "move", "type", "species"]:
        files = downloader.list_local_files(data_type)
        print(f"{data_type:12} : {len(files):3} 个文件")

if __name__ == "__main__":
    asyncio.run(main())