#!/usr/bin/env python3
"""
Pokemon Factory 导入启动脚本
简单的导入命令入口
"""

import os
import sys
import subprocess

def run_import(import_type="all", use_local=True):
    """运行导入"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    manager_script = os.path.join(script_dir, "import_manager.py")
    
    cmd = [sys.executable, manager_script, import_type]
    if not use_local:
        cmd.append("--no-local")
    
    print(f"运行命令: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    print(result.stdout)
    if result.stderr:
        print("错误信息:")
        print(result.stderr)
    
    return result.returncode == 0

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Pokemon Factory 导入启动脚本")
    parser.add_argument("type", nargs="?", default="all",
                       choices=["all", "pokemon", "ability", "move", "form", "stats", "egg_group", "download", "validate", "cleanup"],
                       help="导入类型")
    parser.add_argument("--use-local", action="store_true", default=True,
                       help="使用本地数据（默认启用）")
    parser.add_argument("--no-local", dest="use_local", action="store_false",
                       help="不使用本地数据")
    parser.add_argument("--list-types", action="store_true",
                       help="列出可用的导入类型")
    
    args = parser.parse_args()
    
    if args.list_types:
        print("可用的导入类型:")
        print("  all - 导入所有数据")
        print("  pokemon - 导入宝可梦数据")
        print("  ability - 导入特性数据")
        print("  move - 导入技能数据")
        print("  form - 导入形态数据")
        print("  stats - 导入种族值数据")
        print("  egg_group - 导入蛋群关联数据")
        print("  download - 仅下载本地数据")
        print("  validate - 仅验证数据")
        print("  cleanup - 清理本地数据")
        return
    
    print(f"选择的导入类型: {args.type}")
    print(f"使用本地数据: {'是' if args.use_local else '否'}")
    print("-" * 50)
    
    success = run_import(args.type, args.use_local)
    
    if success:
        print("\n🎉 导入成功完成！")
    else:
        print("\n💥 导入失败！")
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
