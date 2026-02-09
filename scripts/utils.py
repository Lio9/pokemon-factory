import base64
import json
import os
import requests

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