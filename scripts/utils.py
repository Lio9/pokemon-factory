import base64
import json
import os
import requests
import logging

def setup_logging():
    """设置日志配置"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler('D:\\learn\\pokemon-factory\\logs\\import.log', encoding='utf-8'),
            logging.StreamHandler()
        ]
    )

def get_db_config():
    """获取数据库配置"""
    return {
        'host': 'localhost',
        'user': 'root',
        'password': '123456',
        'database': 'pokemon_factory',
        'charset': 'utf8mb4'
    }

def save_to_file(file_path, data):
  # 确保目录存在
  directory = os.path.dirname(file_path)
  if not os.path.exists(directory):
    os.makedirs(directory)
    
  with open(file_path, 'w', encoding="utf8") as file:
    json.dump(data, file, ensure_ascii=False, indent=4)

def file_exists(file_path):
  try:
    with open(file_path) as file:
      return True
  except FileNotFoundError:
    return False

def save_image(file_path, url):
  # 确保目录存在
  directory = os.path.dirname(file_path)
  if not os.path.exists(directory):
    os.makedirs(directory)
    
  img_response = requests.get(url)
  with open(file_path, 'wb') as file:
    file.write(img_response.content)

def save_base64_image(file_path, data):
  # 确保目录存在
  directory = os.path.dirname(file_path)
  if not os.path.exists(directory):
    os.makedirs(directory)
    
  image_data = base64.b64decode(data)
  with open(file_path, 'wb') as file:
    file.write(image_data)