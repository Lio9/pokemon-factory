#!/usr/bin/env python3
"""
下载宝可梦JSON数据到本地
"""

import asyncio
import aiohttp
import json
import os
import logging
from utils import get_db_config, init_logger

# 初始化日志 - 确保在项目根目录创建logs文件夹
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
log_file = os.path.join(project_root, "logs", "download.log")
logger = init_logger(log_file)

async def download_json_file(session, url, filename, data_dir):
    """
    下载JSON文件到本地
    
    Args:
        session: aiohttp会话
        url: 下载地址
        filename: 本地文件名
        data_dir: 本地数据目录
    """
    # 确保在项目根目录创建数据目录
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    full_data_dir = os.path.join(project_root, data_dir)
    filepath = os.path.join(full_data_dir, filename)
    
    try:
        logger.info(f"下载: {filename}")
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=30)) as response:
            if response.status == 200:
                data = await response.json()
                with open(filepath, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                logger.info(f"成功下载: {filename}")
                return True
            else:
                logger.error(f"下载失败 {filename}: 状态码 {response.status}")
                return False
    except Exception as e:
        logger.error(f"下载 {filename} 失败: {str(e)}")
        return False

async def download_pokemon_data(data_dir="data", 
                               start_id=1, end_id=1025):
    """
    下载指定范围的宝可梦JSON数据
    
    Args:
        data_dir: 本地数据目录
        start_id: 起始ID
        end_id: 结束ID (1-1025)
    """
    # 确保在项目根目录创建数据目录
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    full_data_dir = os.path.join(project_root, data_dir)
    
    # 创建数据目录
    os.makedirs(full_data_dir, exist_ok=True)
    
    # 创建aiohttp会话
    connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
    timeout = aiohttp.ClientTimeout(total=30)
    
    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        base_url = "https://pokeapi.co/api/v2/"
        
        # 下载宝可梦基础数据
        tasks = []
        for i in range(start_id, end_id + 1):
            filename = f"pokemon_{i}.json"
            url = base_url + f"pokemon/{i}"
            tasks.append(download_json_file(session, url, filename, data_dir))
        
        results = await asyncio.gather(*tasks)
        success_count = sum(results)
        logger.info(f"宝可梦基础数据下载完成: 成功 {success_count}/{end_id - start_id + 1}")

async def download_pokemon_species_data(data_dir="data", 
                                       start_id=1, end_id=1025):
    """
    下载指定范围的宝可梦物种JSON数据
    
    Args:
        data_dir: 本地数据目录
        start_id: 起始ID
        end_id: 结束ID (1-1025)
    """
    # 确保在项目根目录创建数据目录
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    full_data_dir = os.path.join(project_root, data_dir)
    
    # 创建数据目录
    os.makedirs(full_data_dir, exist_ok=True)
    
    # 清空目标文件夹内的物种数据文件
    logger.info(f"清空目标文件夹内的物种数据: {full_data_dir}")
    if os.path.exists(full_data_dir):
        import glob
        species_files = glob.glob(os.path.join(full_data_dir, "pokemon_species_*.json"))
        for file in species_files:
            try:
                os.remove(file)
                logger.debug(f"删除旧的物种数据文件: {os.path.basename(file)}")
            except Exception as e:
                logger.error(f"删除文件失败 {file}: {str(e)}")
    
    # 创建aiohttp会话
    connector = aiohttp.TCPConnector(limit=50, limit_per_host=10)
    timeout = aiohttp.ClientTimeout(total=30)
    
    async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
        base_url = "https://pokeapi.co/api/v2/"
        
        # 先下载宝可梦物种数据
        tasks = []
        for i in range(start_id, end_id + 1):
            filename = f"pokemon_species_{i}.json"
            url = base_url + f"pokemon-species/{i}"
            tasks.append(download_json_file(session, url, filename, data_dir))
        
        results = await asyncio.gather(*tasks)
        success_count = sum(results)
        logger.info(f"宝可梦物种数据下载完成: 成功 {success_count}/{end_id - start_id + 1}")

async def main():
    """
    主函数
    """
    logger.info("开始下载宝可梦数据")
    
    # 先下载宝可梦物种数据
    logger.info("开始下载宝可梦物种数据")
    await download_pokemon_species_data()
    
    # 再下载宝可梦基础数据
    logger.info("开始下载宝可梦基础数据")
    await download_pokemon_data()
    
    logger.info("所有数据下载完成!")

if __name__ == "__main__":
    asyncio.run(main())