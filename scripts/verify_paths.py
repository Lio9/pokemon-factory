import os
import sys

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config import CONFIG

def verify_paths():
    """验证所有路径配置是否正确"""
    print("=== 路径配置验证 ===")
    print(f"项目根目录: {CONFIG['project_root']}")
    print(f"前端public目录: {CONFIG['public_dir']}")
    print(f"图片存储目录: {CONFIG['images_dir']}")
    print(f"家养宝可梦图片目录: {CONFIG['home_images_dir']}")
    print(f"官方艺术图目录: {CONFIG['official_images_dir']}")
    print()
    
    # 检查目录是否存在
    directories = [
        ('项目根目录', CONFIG['project_root']),
        ('前端public目录', CONFIG['public_dir']),
        ('图片存储目录', CONFIG['images_dir']),
        ('家养宝可梦图片目录', CONFIG['home_images_dir']),
        ('官方艺术图目录', CONFIG['official_images_dir'])
    ]
    
    all_exist = True
    for name, path in directories:
        if os.path.exists(path):
            print(f"✅ {name}: {path}")
        else:
            print(f"❌ {name}: {path} (不存在)")
            all_exist = False
    
    print()
    
    # 检查是否可以写入
    if all_exist:
        write_test_dir = CONFIG['home_images_dir']
        test_file = os.path.join(write_test_dir, '.write_test')
        try:
            with open(test_file, 'w') as f:
                f.write('test')
            os.remove(test_file)
            print("✅ 所有目录可写入")
        except Exception as e:
            print(f"❌ 写入测试失败: {e}")
            all_exist = False
    
    print()
    if all_exist:
        print("🎉 路径配置验证通过！")
        print("📁 图片将存储在:")
        print(f"   - 家养宝可梦: {CONFIG['home_images_dir']}")
        print(f"   - 官方艺术图: {CONFIG['official_images_dir']}")
    else:
        print("⚠️  路径配置有问题，请检查")
    
    return all_exist

if __name__ == '__main__':
    verify_paths()