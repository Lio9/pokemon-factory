import os
import sys
import requests
import time
import random
from pathlib import Path

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 配置文件路径
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)

# 配置常量
SAVE_DIR = os.path.join(PROJECT_ROOT, 'public', 'data', 'images', 'home')
OFFICIAL_SAVE_DIR = os.path.join(PROJECT_ROOT, 'public', 'data', 'images', 'official')
DREAM_SAVE_DIR = os.path.join(PROJECT_ROOT, 'public', 'data', 'images', 'dream')

# PokeAPI统一接口
POKEAPI_BASE_URL = "https://pokeapi.co/api/v2/"
POKEAPI_SPRITES_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"

# 代理配置
PROXIES = None  # 可以在这里配置代理

def verify_server_connection(url, timeout=5):
    """验证服务器连接是否正常"""
    try:
        response = requests.head(url, proxies=PROXIES, timeout=timeout, allow_redirects=True)
        return response.status_code == 200
    except Exception as e:
        print(f"❌ 服务器连接失败: {url} - {e}")
        return False

def validate_all_servers():
    """验证所有服务器连接"""
    print("🔍 正在验证服务器连接...")
    
    # 测试PokeAPI服务器
    pokeapi_url = f"{POKEAPI_BASE_URL}pokemon/1"
    pokeapi_ok = verify_server_connection(pokeapi_url)
    print(f"PokeAPI服务器: {'✅ 正常' if pokeapi_ok else '❌ 失败'}")
    
    # 测试家养宝可梦图片服务器
    home_url = f"{POKEAPI_SPRITES_URL}other/home/1.png"
    home_ok = verify_server_connection(home_url)
    print(f"家养宝可梦图片服务器: {'✅ 正常' if home_ok else '❌ 失败'}")
    
    return {
        'pokeapi': pokeapi_ok,
        'home': home_ok
    }

def download_with_fallback(url, filepath, type_name, max_retries=3):
    """带降级机制的下载函数"""
    for attempt in range(max_retries):
        try:
            response = requests.get(url, proxies=PROXIES, timeout=15, stream=True)
            
            if response.status_code == 200:
                with open(filepath, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        f.write(chunk)
                return True
            else:
                print(f"   ⚠️  尝试 {attempt + 1} 失败，状态码: {response.status_code}")
        except Exception as e:
            print(f"   ⚠️  尝试 {attempt + 1} 失败: {e}")
            if attempt < max_retries - 1:
                time.sleep(2)
    
    return False

# 导入clear_directory函数
def clear_directory(path):
    """清空指定目录下的所有文件"""
    if os.path.exists(path):
        for file in os.listdir(path):
            file_path = os.path.join(path, file)
            if os.path.isfile(file_path):
                os.remove(file_path)
        print(f"已清空目录: {path}")
    else:
        os.makedirs(path)

# --- 配置区 ---
# 存储路径
SAVE_DIR = '../pokemon-factory-frontend/public/data/images/home/'
# 代理配置（如果你在中国大陆，强烈建议配置，否则 GitHub 会返回 404 HTML）
PROXIES = None

# 资源链接：GitHub 上的高清 HOME 渲染图 (PNG 格式)
BASE_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/"

# 下载超时设置
TIMEOUT = 30
MAX_RETRIES = 3

def download_file(url, save_path, index, total):
    """下载单个文件并显示进度，支持重试"""
    for attempt in range(MAX_RETRIES):
        try:
            # 确保目录存在
            os.makedirs(os.path.dirname(save_path), exist_ok=True)
            
            # 添加请求头，模拟浏览器访问
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                'Accept': 'image/webp,image/apng,image/*,*/*;q=0.8',
                'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
                'Accept-Encoding': 'gzip, deflate, br',
                'Connection': 'keep-alive',
            }
            
            # 发送请求
            response = requests.get(url, proxies=PROXIES, timeout=TIMEOUT, stream=True, headers=headers)
            
            # 核心检查：如果返回的不是图片类型，说明 URL 错了或者被拦截了
            content_type = response.headers.get('Content-Type', '')
            if 'image' not in content_type:
                print(f"\r[{index}/{total}] ⚠️ 跳过：URL 返回的不是图片 (收到的是 {content_type}) -> {url}")
                return False

            if response.status_code == 200:
                # 下载文件
                total_size = int(response.headers.get('content-length', 0))
                downloaded = 0
                
                with open(save_path, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)
                            downloaded += len(chunk)
                            
                            # 显示下载进度
                            if total_size > 0:
                                percent = (downloaded / total_size) * 100
                                print(f"\r[{index}/{total}] 下载中: {percent:.1f}% ({downloaded}/{total_size} bytes)", end="")
                
                print(f"\r[{index}/{total}] ✅ 下载成功: {os.path.basename(save_path)}")
                return True
            else:
                print(f"\r[{index}/{total}] ❌ 状态码异常: {response.status_code} -> {url}")
                if attempt < MAX_RETRIES - 1:
                    print(f"   🔄 重试 {attempt + 1}/{MAX_RETRIES}...")
                    time.sleep(2)  # 重试前等待
                continue
                
        except requests.exceptions.Timeout:
            print(f"\r[{index}/{total}] ⏰ 下载超时: {url}")
            if attempt < MAX_RETRIES - 1:
                print(f"   🔄 重试 {attempt + 1}/{MAX_RETRIES}...")
                time.sleep(2)
            continue
        except requests.exceptions.ConnectionError as e:
            print(f"\r[{index}/{total}] 🌐 连接错误: {e}")
            if attempt < MAX_RETRIES - 1:
                print(f"   🔄 重试 {attempt + 1}/{MAX_RETRIES}...")
                time.sleep(3)
            continue
        except requests.exceptions.RequestException as e:
            print(f"\r[{index}/{total}] ❌ 请求异常: {e}")
            if attempt < MAX_RETRIES - 1:
                print(f"   🔄 重试 {attempt + 1}/{MAX_RETRIES}...")
                time.sleep(2)
            continue
        except Exception as e:
            print(f"\r[{index}/{total}] ❌ 其他错误: {e}")
            if attempt < MAX_RETRIES - 1:
                print(f"   🔄 重试 {attempt + 1}/{MAX_RETRIES}...")
                time.sleep(2)
            continue
    
    print(f"\r[{index}/{total}] ❌ 下载失败，已重试 {MAX_RETRIES} 次")
    return False


def batch_download(start_num=1, end_num=100, choice='1'):
    """批量下载指定范围的宝可梦图片"""
    print(f">>> 开始批量下载宝可梦图片 ({start_num}-{end_num})...")
    print(f"目标目录: {os.path.abspath(SAVE_DIR)}")
    print(f"代理配置: {'启用' if PROXIES else '禁用'}")
    print(f"最大重试次数: {MAX_RETRIES}")
    print(f"下载模式: {'全量下载' if choice == '1' else '增量下载'}")
    print("-" * 60)

    # 验证服务器连接
    servers_ok = validate_all_servers()
    if not any(servers_ok.values()):
        print("❌ 所有服务器都不可用，无法继续下载")
        return
    
    total_count = end_num - start_num + 1
    success_count = 0
    skip_count = 0
    error_count = 0
    start_time = time.time()

    for i in range(start_num, end_num + 1):
        # 1. 构造文件名 (例如: 0001.png)
        filename = f"{i:04d}.png"
        filepath = os.path.join(SAVE_DIR, filename)

        # 2. 构造 URL
        target_url = f"{BASE_URL}{i}.png"

        # 检查文件是否已存在（增量模式下）
        if choice == '2' and os.path.exists(filepath):
            skip_count += 1
            print(f"\r[{i-start_num+1}/{total_count}] 🔄 跳过已存在文件: {filename}", end="")
            continue

        # 下载文件
        success = download_file(target_url, filepath, i-start_num+1, total_count)

        if success:
            success_count += 1
            # 随机延迟，保护 IP
            time.sleep(random.uniform(0.05, 0.15))
        else:
            error_count += 1

    # 统计结果
    end_time = time.time()
    elapsed_time = end_time - start_time
    
    print("-" * 60)
    print("📊 下载完成统计:")
    print(f"   范围: {start_num}-{end_num}")
    print(f"   总计: {total_count} 个文件")
    print(f"   成功: {success_count} 个文件")
    print(f"   跳过: {skip_count} 个文件")
    print(f"   失败: {error_count} 个文件")
    print(f"   耗时: {elapsed_time:.2f} 秒")
    print(f"   平均速度: {success_count/elapsed_time:.2f} 文件/秒")
    
    if error_count > 0:
        print("\n⚠️  注意: 有部分文件下载失败")
    else:
        print("\n✅ 所有文件下载完成！")


def main():
    # 询问用户选择下载模式
    print("=== 宝可梦图片批量下载工具 ===")
    print("请选择下载模式:")
    print("1. 全量下载 - 清空现有文件夹并重新下载所有图片")
    print("2. 增量下载 - 只下载缺失的图片，保留已下载的文件")
    print("3. 选择图片类型下载")
    
    # 如果有命令行参数，直接使用第一个参数作为选择
    if len(sys.argv) > 1:
        choice = sys.argv[1]
        if choice not in ['1', '2', '3']:
            print("无效选择，使用默认值 1")
            choice = '1'
    else:
        # 非交互式环境，使用默认选择
        if os.environ.get('CI', 'false').lower() == 'true':
            choice = '1'  # CI环境默认全量下载
        else:
            # 交互式环境，等待用户输入
            while True:
                try:
                    choice = input("请输入选择 (1 或 2 或 3): ").strip()
                    if choice in ['1', '2', '3']:
                        break
                    print("无效选择，请输入 1 或 2 或 3")
                except EOFError:
                    # 无终端环境，使用默认选择
                    print("无终端环境，使用默认选择 1")
                    choice = '1'
                    break
    
    # 根据选择处理目录
    if choice == '1':
        # 全量下载 - 清空所有目录
        clear_directory(SAVE_DIR)
        print("✅ 已清空目标目录，开始全量下载...")
        # 直接开始全量下载
        download_pokemon_images(1, 1025, True)
    elif choice == '2':
        # 增量下载 - 检查已有文件
        os.makedirs(SAVE_DIR, exist_ok=True)
        os.makedirs(OFFICIAL_SAVE_DIR, exist_ok=True)
        os.makedirs(DREAM_SAVE_DIR, exist_ok=True)
        existing_files = [f for f in os.listdir(SAVE_DIR) if f.endswith('.png')]
        existing_official = [f for f in os.listdir(OFFICIAL_SAVE_DIR) if f.endswith('.png')]
        existing_dream = [f for f in os.listdir(DREAM_SAVE_DIR) if f.endswith('.png')]
        print(f"✅ 增量下载模式，家养宝可梦已有 {len(existing_files)} 个文件，官方艺术图已有 {len(existing_official)} 个文件，梦幻图片已有 {len(existing_dream)} 个文件")
        
        # 增量下载需要用户输入范围
        if len(sys.argv) >= 4:
            try:
                start_num = int(sys.argv[2])
                end_num = int(sys.argv[3])
                if 1 <= start_num <= end_num <= 1025:
                    batch_download_types_enhanced(start_num, end_num, '4', 'robust')
                else:
                    print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
            except ValueError:
                print("错误: 请输入有效的数字")
        else:
            # 非交互式环境，使用默认范围
            if os.environ.get('CI', 'false').lower() == 'true':
                batch_download_types_enhanced(1, 100, '4', 'robust')
            else:
                # 交互式环境，等待用户输入
                print("请输入要下载的宝可梦编号范围 (1-1025)")
                try:
                    start_num = int(input("起始编号 (默认1): ") or "1")
                    end_num = int(input("结束编号 (默认100): ") or "100")
                    
                    if 1 <= start_num <= end_num <= 1025:
                        batch_download_types_enhanced(start_num, end_num, '4', 'robust')
                    else:
                        print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
                except ValueError:
                    print("错误: 请输入有效的数字")
    else:
        # 选择下载范围
        print("请输入要下载的宝可梦编号范围 (1-1025)")
        
        # 如果有命令行参数，直接使用第二个和第三个参数作为范围
        if len(sys.argv) >= 4:
            try:
                start_num = int(sys.argv[2])
                end_num = int(sys.argv[3])
                if 1 <= start_num <= end_num <= 1025:
                    download_pokemon_images(start_num, end_num, False)
                else:
                    print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
            except ValueError:
                print("错误: 请输入有效的数字")
        else:
            # 非交互式环境，使用默认范围
            if os.environ.get('CI', 'false').lower() == 'true':
                download_pokemon_images(1, 100, False)
            else:
                # 交互式环境，等待用户输入
                try:
                    start_num = int(input("起始编号 (默认1): ") or "1")
                    end_num = int(input("结束编号 (默认100): ") or "100")
                    
                    if 1 <= start_num <= end_num <= 1025:
                        download_pokemon_images(start_num, end_num, False)
                    else:
                        print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
                except ValueError:
                    print("错误: 请输入有效的数字")
    
    if len(sys.argv) >= 4:
        # 从命令行参数获取下载范围
        try:
            start_num = int(sys.argv[2])
            end_num = int(sys.argv[3])
            if 1 <= start_num <= end_num <= 1025:
                download_pokemon_images(start_num, end_num, False)
            else:
                print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
        except ValueError:
            print("错误: 请输入有效的数字")
    else:
        # 交互式输入
        if choice == '1':
            # 全量下载 - 直接下载所有1025个宝可梦
            start_num = 1
            end_num = 1025
            print("全量下载模式 - 将下载所有1025个宝可梦的图片")
        elif choice == '2':
            # 增量下载 - 询问下载范围
            print("请输入要下载的宝可梦编号范围 (1-1025)")
            try:
                start_num = int(input("起始编号 (默认1): ") or "1")
                end_num = int(input("结束编号 (默认100): ") or "100")
                
                if 1 <= start_num <= end_num <= 1025:
                    if choice == '3':
                        batch_download_types(start_num, end_num, type_choice)
                    else:
                        batch_download(start_num, end_num, choice)
                else:
                    print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
            except ValueError:
                print("错误: 请输入有效的数字")
        else:
            # 选择图片类型下载 - 询问下载范围
            print("请输入要下载的宝可梦编号范围 (1-1025)")
            try:
                start_num = int(input("起始编号 (默认1): ") or "1")
                end_num = int(input("结束编号 (默认100): ") or "100")
                
                if 1 <= start_num <= end_num <= 1025:
                    if choice == '3':
                        batch_download_types(start_num, end_num, type_choice)
                    else:
                        batch_download(start_num, end_num, choice)
                else:
                    print("错误: 数字必须在1-1025范围内，且起始数字小于等于结束数字")
            except ValueError:
                print("错误: 请输入有效的数字")


def batch_download_types(start_num=1, end_num=100, type_choice='4'):
    """下载指定类型的宝可梦图片"""
    print(f">>> 开始下载宝可梦图片 ({start_num}-{end_num})...")
    print(f"下载模式: {'全量下载' if type_choice == '4' else '增量下载'}")
    print("-" * 60)
    
    # 定义图片类型和对应的下载函数
    download_functions = [
        ('梦幻图片', 'download_dream_images', download_dream_images),
        ('家养宝可梦图片', 'download_home_images', download_home_images),
        ('官方艺术图', 'download_official_images', download_official_images)
    ]
    
    # 确定要下载的类型
    if type_choice == '1':
        types_to_download = [download_functions[0]]
    elif type_choice == '2':
        types_to_download = [download_functions[1]]
    elif type_choice == '3':
        types_to_download = [download_functions[2]]
    else:
        types_to_download = download_functions
    
    total_success = 0
    total_skip = 0
    total_error = 0
    
    for type_name, func_name, download_func in types_to_download:
        print(f"\n🚀 开始下载 {type_name}...")
        try:
            result = download_func(start_num, end_num, type_choice == '4')
            if result:
                success, skip, error = result
                total_success += success
                total_skip += skip
                total_error += error
            print(f"✅ {type_name}下载完成")
        except Exception as e:
            print(f"❌ {type_name}下载出错: {e}")
            total_error += (end_num - start_num + 1)
    
    print("-" * 60)
    print("📊 所有图片下载完成统计:")
    print(f"   总计: {1025} 个文件")
    print(f"   成功: {total_success} 个文件")
    print(f"   跳过: {total_skip} 个文件")
    print(f"   失败: {total_error} 个文件")
    
    return total_success, total_skip, total_error


def download_dream_images(start_num, end_num, is_full_download=False):
    """下载梦幻图片"""
    print(f"   下载梦幻图片 ({start_num}-{end_num})...")
    # 这里需要实现梦幻图片的下载逻辑
    # 暂时返回模拟数据
    time.sleep(1)  # 模拟下载时间
    return 0, 0, 0


def download_home_images(start_num, end_num, is_full_download=False):
    """下载家养宝可梦图片"""
    print(f"   下载家养宝可梦图片 ({start_num}-{end_num})...")
    # 这里需要实现家养宝可梦图片的下载逻辑
    # 暂时返回模拟数据
    time.sleep(2)  # 模拟下载时间
    return 0, 0, 0


def download_official_images(start_num, end_num, is_full_download=False):
    """下载官方艺术图"""
    print(f"   下载官方艺术图 ({start_num}-{end_num})...")
    # 这里需要实现官方艺术图的下载逻辑
    # 暂时返回模拟数据
    time.sleep(3)  # 模拟下载时间
    return 0, 0, 0


def download_pokemon_images(start_num=1, end_num=1025, is_full_download=False):
    """使用PokeAPI下载宝可梦图片"""
    print(f"   从PokeAPI下载宝可梦图片 ({start_num}-{end_num})...")
    try:
        # 确保目录存在
        os.makedirs(SAVE_DIR, exist_ok=True)
        
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(SAVE_DIR, filename)
            
            # 检查文件是否已存在
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            try:
                # 从PokeAPI获取宝可梦数据
                response = requests.get(f"{POKEAPI_BASE_URL}pokemon/{i}", proxies=PROXIES, timeout=15)
                
                if response.status_code == 200:
                    data = response.json()
                    # 获取家养宝可梦图片URL
                    home_data = data.get('sprites', {}).get('other', {}).get('home', {})
                    if isinstance(home_data, dict):
                        image_url = home_data.get('front_default', '')
                    else:
                        image_url = home_data
                    
                    if image_url:
                        print(f"   下载 {i}: {image_url}")
                        # 下载图片
                        img_response = requests.get(image_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if img_response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in img_response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                            print(f"   ✅ 成功下载 {i}")
                        else:
                            print(f"   ❌ 下载失败 {i}: 状态码 {img_response.status_code}")
                            error_count += 1
                    else:
                        print(f"   ❌ 无图片URL {i}")
                        error_count += 1
                else:
                    print(f"   ❌ PokeAPI失败 {i}: 状态码 {response.status_code}")
                    error_count += 1
                    
            except Exception as e:
                print(f"   ❌ 下载异常 {i}: {e}")
                error_count += 1
            
            # 随机延迟，避免请求过快
            time.sleep(random.uniform(0.05, 0.15))
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ PokeAPI下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
        
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_home_images_improved(start_num, end_num, is_full_download=False):
    """改进版家养宝可梦图片下载"""
    print(f"   下载改进版家养宝可梦图片 ({start_num}-{end_num})...")
    try:
        os.makedirs(SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            try:
                # 从PokeAPI获取宝可梦数据
                response = requests.get(f"{POKEAPI_BASE_URL}pokemon/{i}", proxies=PROXIES, timeout=15)
                
                if response.status_code == 200:
                    data = response.json()
                    # 获取家养宝可梦图片URL
                    image_url = data.get('sprites', {}).get('other', {}).get('home', '')
                    
                    if image_url:
                        # 下载图片
                        img_response = requests.get(image_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if img_response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in img_response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                        else:
                            error_count += 1
                    else:
                        error_count += 1
                else:
                    error_count += 1
                    
            except Exception as e:
                error_count += 1
            
            # 随机延迟，避免请求过快
            time.sleep(random.uniform(0.05, 0.15))
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 家养宝可梦图片下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_official_images_improved(start_num, end_num, is_full_download=False):
    """改进版官方艺术图下载"""
    print(f"   下载改进版官方艺术图 ({start_num}-{end_num})...")
    try:
        os.makedirs(SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            try:
                # 从PokeAPI获取宝可梦数据
                response = requests.get(f"{POKEAPI_BASE_URL}pokemon/{i}", proxies=PROXIES, timeout=15)
                
                if response.status_code == 200:
                    data = response.json()
                    # 获取官方艺术图URL
                    image_url = data.get('sprites', {}).get('other', {}).get('official', {}).get('front_default', '')
                    
                    if image_url:
                        # 下载图片
                        img_response = requests.get(image_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if img_response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in img_response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                        else:
                            error_count += 1
                    else:
                        error_count += 1
                else:
                    error_count += 1
                    
            except Exception as e:
                error_count += 1
            
            # 随机延迟，避免请求过快
            time.sleep(random.uniform(0.05, 0.15))
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 官方艺术图下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_dream_images_robust(start_num, end_num, is_full_download=False):
    """健壮版梦幻图片下载"""
    print(f"   下载健壮版梦幻图片 ({start_num}-{end_num})...")
    try:
        os.makedirs(DREAM_SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(DREAM_SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            target_url = f"{DREAM_BASE_URL}{i}.png"
            success = False
            
            for attempt in range(3):
                try:
                    response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                    
                    if response.status_code == 200:
                        with open(filepath, 'wb') as f:
                            for chunk in response.iter_content(chunk_size=8192):
                                f.write(chunk)
                        success_count += 1
                        success = True
                        break
                    else:
                        print(f"   ⚠️  尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                except Exception as e:
                    print(f"   ⚠️  尝试 {attempt + 1} 失败: {e}")
                    if attempt < 2:
                        time.sleep(2)
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 健壮版梦幻图片下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_home_images_robust(start_num, end_num, is_full_download=False):
    """健壮版家养宝可梦图片下载"""
    print(f"   下载健壮版家养宝可梦图片 ({start_num}-{end_num})...")
    try:
        os.makedirs(SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            target_url = f"{BASE_URL}{i}.png"
            success = False
            
            for attempt in range(3):
                try:
                    response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                    
                    if response.status_code == 200:
                        with open(filepath, 'wb') as f:
                            for chunk in response.iter_content(chunk_size=8192):
                                f.write(chunk)
                        success_count += 1
                        success = True
                        break
                    else:
                        print(f"   ⚠️  尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                except Exception as e:
                    print(f"   ⚠️  尝试 {attempt + 1} 失败: {e}")
                    if attempt < 2:
                        time.sleep(2)
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 健壮版家养宝可梦图片下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_official_images_robust(start_num, end_num, is_full_download=False):
    """健壮版官方艺术图下载"""
    print(f"   下载健壮版官方艺术图 ({start_num}-{end_num})...")
    try:
        os.makedirs(OFFICIAL_SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(OFFICIAL_SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            target_url = f"{OFFICIAL_BASE_URL}{i}.png"
            success = False
            
            for attempt in range(3):
                try:
                    response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                    
                    if response.status_code == 200:
                        with open(filepath, 'wb') as f:
                            for chunk in response.iter_content(chunk_size=8192):
                                f.write(chunk)
                        success_count += 1
                        success = True
                        break
                    else:
                        print(f"   ⚠️  尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                except Exception as e:
                    print(f"   ⚠️  尝试 {attempt + 1} 失败: {e}")
                    if attempt < 2:
                        time.sleep(2)
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 健壮版官方艺术图下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_dream_images_robust_multi_mirror(start_num, end_num, is_full_download=False):
    """多镜像源健壮版梦幻图片下载"""
    print(f"   下载多镜像源健壮版梦幻图片 ({start_num}-{end_num})...")
    try:
        os.makedirs(DREAM_SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(DREAM_SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            success = False
            for attempt in range(3):
                for mirror_index, mirror_url in enumerate(DREAM_MIRROR_URLS):
                    try:
                        target_url = f"{mirror_url}{i}.png"
                        response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                            success = True
                            print(f"   ✅ 从镜像 {mirror_index + 1} 下载成功")
                            break
                        else:
                            print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                    except Exception as e:
                        print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败: {e}")
                        if attempt < 2:
                            time.sleep(2)
                
                if success:
                    break
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 多镜像源健壮版梦幻图片下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_home_images_robust_multi_mirror(start_num, end_num, is_full_download=False):
    """多镜像源健壮版家养宝可梦图片下载"""
    print(f"   下载多镜像源健壮版家养宝可梦图片 ({start_num}-{end_num})...")
    try:
        os.makedirs(SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            success = False
            for attempt in range(3):
                for mirror_index, mirror_url in enumerate(MIRROR_URLS):
                    try:
                        target_url = f"{mirror_url}{i}.png"
                        response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                            success = True
                            print(f"   ✅ 从镜像 {mirror_index + 1} 下载成功")
                            break
                        else:
                            print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                    except Exception as e:
                        print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败: {e}")
                        if attempt < 2:
                            time.sleep(2)
                
                if success:
                    break
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 多镜像源健壮版家养宝可梦图片下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def download_official_images_robust_multi_mirror(start_num, end_num, is_full_download=False):
    """多镜像源健壮版官方艺术图下载"""
    print(f"   下载多镜像源健壮版官方艺术图 ({start_num}-{end_num})...")
    try:
        os.makedirs(OFFICIAL_SAVE_DIR, exist_ok=True)
        total_count = end_num - start_num + 1
        success_count = 0
        skip_count = 0
        error_count = 0
        start_time = time.time()
        
        for i in range(start_num, end_num + 1):
            filename = f"{i:04d}.png"
            filepath = os.path.join(OFFICIAL_SAVE_DIR, filename)
            
            if os.path.exists(filepath):
                skip_count += 1
                continue
            
            success = False
            for attempt in range(3):
                for mirror_index, mirror_url in enumerate(OFFICIAL_MIRROR_URLS):
                    try:
                        target_url = f"{mirror_url}{i}.png"
                        response = requests.get(target_url, proxies=PROXIES, timeout=15, stream=True)
                        
                        if response.status_code == 200:
                            with open(filepath, 'wb') as f:
                                for chunk in response.iter_content(chunk_size=8192):
                                    f.write(chunk)
                            success_count += 1
                            success = True
                            print(f"   ✅ 从镜像 {mirror_index + 1} 下载成功")
                            break
                        else:
                            print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败，状态码: {response.status_code}")
                    except Exception as e:
                        print(f"   ⚠️  镜像 {mirror_index + 1} 尝试 {attempt + 1} 失败: {e}")
                        if attempt < 2:
                            time.sleep(2)
                
                if success:
                    break
            
            if not success:
                error_count += 1
        
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"   ✅ 多镜像源健壮版官方艺术图下载完成: {success_count} 成功, {skip_count} 跳过, {error_count} 失败")
        return success_count, skip_count, error_count
    except Exception as e:
        print(f"   ❌ 下载出错: {e}")
        return 0, 0, (end_num - start_num + 1)


def batch_download_types_enhanced(start_num=1, end_num=100, type_choice='4', quality='robust'):
    """增强版下载指定类型的宝可梦图片"""
    print(f">>> 开始下载宝可梦图片 ({start_num}-{end_num})...")
    print(f"下载模式: {'全量下载' if type_choice == '4' else '增量下载'}")
    print(f"图片质量: {quality}")
    print("-" * 60)
    
    # 验证服务器连接
    servers_ok = validate_all_servers()
    if not any(servers_ok.values()):
        print("❌ 所有服务器都不可用，无法继续下载")
        return 0, 0, (end_num - start_num + 1) * 3
    
    # 定义图片类型和对应的下载函数
    download_functions = {
        'dream': {
            'improved': download_dream_images_improved,
            'robust': download_dream_images_robust,
            'multi_mirror': download_dream_images_robust_multi_mirror
        },
        'home': {
            'improved': download_home_images_improved,
            'robust': download_home_images_robust,
            'multi_mirror': download_home_images_robust_multi_mirror
        },
        'official': {
            'improved': download_official_images_improved,
            'robust': download_official_images_robust,
            'multi_mirror': download_official_images_robust_multi_mirror
        }
    }
    
    # 确定要下载的类型
    if type_choice == '1':
        types_to_download = [('dream', download_functions['dream'].get(quality, download_dream_images_robust))]
    elif type_choice == '2':
        types_to_download = [('home', download_functions['home'].get(quality, download_home_images_robust))]
    elif type_choice == '3':
        types_to_download = [('official', download_functions['official'].get(quality, download_official_images_robust))]
    else:
        types_to_download = [
            ('dream', download_functions['dream'].get(quality, download_dream_images_robust)),
            ('home', download_functions['home'].get(quality, download_home_images_robust)),
            ('official', download_functions['official'].get(quality, download_official_images_robust))
        ]
    
    total_success = 0
    total_skip = 0
    total_error = 0
    
    for type_name, download_func in types_to_download:
        print(f"\n🚀 开始下载 {type_name} 图片...")
        try:
            success, skip, error = download_func(start_num, end_num, type_choice == '4')
            total_success += success
            total_skip += skip
            total_error += error
            print(f"✅ {type_name} 图片下载完成")
        except Exception as e:
            print(f"❌ {type_name} 图片下载出错: {e}")
            total_error += (end_num - start_num + 1)
    
    print("-" * 60)
    print("📊 所有图片下载完成统计:")
    print(f"   总计: {1025 * 3} 个文件 (1025×3)")
    print(f"   成功: {total_success} 个文件")
    print(f"   跳过: {total_skip} 个文件")
    print(f"   失败: {total_error} 个文件")
    
    return total_success, total_skip, total_error


if __name__ == '__main__':
    main()