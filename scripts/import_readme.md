# Pokemon Factory 导入脚本使用说明

## 📋 详细步骤说明

### 🎯 完整导入流程步骤

#### **步骤1: 环境准备**
```bash
# 1. 确保MySQL数据库服务正常运行
# 2. 确保数据库连接配置正确
# 3. 确保Python环境正常
python --version
```

#### **步骤2: 下载本地数据**
```bash
cd scripts
python import.py download
```
- 下载所有PokeAPI数据到本地
- 生成JSON文件存储在 `data/local/` 目录
- 下载完成后可以重复使用，无需再次下载

#### **步骤3: 导入数据到数据库**
```bash
cd scripts
python import.py all
```
- 从本地JSON文件导入所有数据
- 自动检测本地数据是否存在
- 优先使用本地数据，减少网络请求

#### **步骤4: 验证数据完整性**
```bash
cd scripts
python import.py validate
```
- 验证所有导入的数据
- 生成详细的验证报告
- 确保数据质量符合要求

#### **步骤5: 检查导入结果**
```bash
cd scripts
# 查看数据库中的数据量
python import.py validate
```
- 查看验证报告
- 确认所有数据导入成功
- 检查是否有任何错误

### 🔄 更新数据流程步骤

#### **步骤1: 清理旧数据**
```bash
cd scripts
python import.py cleanup
```
- 清理本地下载的数据
- 清理数据库中的旧数据
- 准备重新导入

#### **步骤2: 下载新数据**
```bash
cd scripts
python import.py download
```
- 下载最新的PokeAPI数据
- 更新本地JSON文件

#### **步骤3: 导入新数据**
```bash
cd scripts
python import.py all
```
- 导入最新的数据到数据库
- 验证导入结果

#### **步骤4: 验证新数据**
```bash
cd scripts
python import.py validate
```
- 验证新导入的数据
- 确保数据完整性

### 🧪 测试导入流程步骤

#### **步骤1: 测试宝可梦导入**
```bash
cd scripts
python import.py pokemon
```
- 仅导入宝可梦数据进行测试
- 验证导入功能是否正常

#### **步骤2: 验证宝可梦数据**
```bash
cd scripts
python import.py validate
```
- 验证宝可梦数据的完整性
- 检查是否有数据错误

#### **步骤3: 测试其他类型导入**
```bash
cd scripts
# 测试特性导入
python import.py ability

# 测试技能导入
python import.py move

# 测试形态导入
python import.py form
```

### 🛠️ 故障排除步骤

#### **步骤1: 检查数据库连接**
```bash
cd scripts
# 验证数据库连接
python data_validator.py
```

#### **步骤2: 检查本地数据**
```bash
cd scripts
# 检查本地数据是否存在
dir data\local\
```

#### **步骤3: 重新下载数据**
```bash
cd scripts
# 清理并重新下载
python import.py cleanup
python import.py download
```

#### **步骤4: 强制网络导入**
```bash
cd scripts
# 强制从网络下载并导入
python import.py pokemon --no-local
```

#### **步骤5: 查看详细日志**
```bash
cd scripts
# 查看导入日志
type logs\import_manager.log
```

## 📁 脚本文件夹结构

```
scripts/
├── import_manager.py    # 统一导入管理器（核心）
├── import.py           # 简化的Python启动脚本
├── data_validator.py   # 数据验证器
├── download_local_data.py # 本地数据下载脚本
├── pokemon_import.py   # 宝可梦导入脚本
├── ability_import.py   # 特性导入脚本
├── move_import.py      # 技能导入脚本
├── form_import.py      # 形态导入脚本
├── stats_import.py     # 种族值导入脚本
├── egg_group_import.py # 蛋群关联导入脚本
├── utils.py            # 工具函数
└── import_readme.md    # 脚本使用说明
```

## 🚀 使用方法

### 基本命令格式
```bash
cd scripts
python import.py [导入类型]
```

### 可用的导入类型

| 类型 | 说明 | 命令示例 |
|------|------|----------|
| `all` | 导入所有数据 | `python import.py all` |
| `pokemon` | 导入宝可梦数据 | `python import.py pokemon` |
| `ability` | 导入特性数据 | `python import.py ability` |
| `move` | 导入技能数据 | `python import.py move` |
| `form` | 导入形态数据 | `python import.py form` |
| `stats` | 导入种族值数据 | `python import.py stats` |
| `egg_group` | 导入蛋群关联数据 | `python import.py egg_group` |
| `download` | 仅下载本地数据 | `python import.py download` |
| `validate` | 仅验证数据 | `python import.py validate` |
| `cleanup` | 清理本地数据 | `python import.py cleanup` |

## 🎯 常见使用场景

### 场景1: 首次导入所有数据
```bash
cd scripts
# 1. 下载本地数据
python import.py download

# 2. 导入所有数据
python import.py all

# 3. 验证数据
python import.py validate
```

### 场景2: 更新数据
```bash
cd scripts
# 1. 清理旧的本地数据
python import.py cleanup

# 2. 下载新的本地数据
python import.py download

# 3. 导入更新的数据
python import.py all
```

### 场景3: 调试测试
```bash
cd scripts
# 1. 仅导入宝可梦数据进行测试
python import.py pokemon

# 2. 验证导入结果
python import.py validate
```

### 场景4: 仅下载不导入
```bash
cd scripts
# 下载本地数据但不导入
python import.py download
```

### 场景5: 仅验证不导入
```bash
cd scripts
# 仅验证现有数据
python import.py validate
```

## ⚙️ 参数选项

### 本地数据选项
```bash
# 使用本地数据（默认）
python import.py pokemon

# 不使用本地数据（强制从网络下载）
python import.py pokemon --no-local
```

### 列出可用类型
```bash
# 查看所有可用的导入类型
python import.py --list-types
```

## 📊 数据验证

### 自动验证
每个导入脚本在导入完成后会自动验证数据，确保数据质量。

### 手动验证
```bash
cd scripts
# 验证所有数据
python import.py validate
```

### 验证报告
验证报告会生成在 `logs/` 目录下：
- `validation_report_YYYYMMDD_HHMMSS.json` - 详细验证报告
- `validation_summary.json` - 验证总结

## 🔧 高级功能

### 下载本地数据
```bash
cd scripts
# 下载所有PokeAPI数据到本地
python import.py download
```

### 清理本地数据
```bash
cd scripts
# 清理本地下载的数据
python import.py cleanup
```

### 直接使用各类型导入脚本
```bash
cd scripts
# 直接运行各类型导入脚本（不通过管理器）
python pokemon_import.py
python ability_import.py
python move_import.py
```

## 🛠️ 故障排除

### 常见问题

#### 1. 本地数据不存在
```bash
cd scripts
python import.py download
```

#### 2. 网络连接失败
```bash
cd scripts
# 强制从网络下载（不使用本地数据）
python import.py pokemon --no-local
```

#### 3. 导入失败
```bash
cd scripts
# 验证数据
python import.py validate
```

### 调试技巧
```bash
cd scripts
# 查看详细日志
tail -f logs/import_manager.log

# 手动验证数据
python import.py validate

# 清理并重新开始
python import.py cleanup
python import.py download
python import.py all
```

## 📈 性能优化

### 并发导入
- 支持多线程并发导入
- 自动调整并发数量
- 优化网络请求性能

### 本地数据复用
- 避免重复网络请求
- 加快导入速度
- 减少网络负载

### 增量更新
- 支持部分数据导入
- 快速更新新数据
- 保留已有数据

## 🎉 总结

所有导入功能都通过统一的管理器进行，提供简单易用的命令行接口。支持本地数据缓存、自动验证、智能降级等高级功能，确保数据导入的可靠性。

---
**注意**: 所有导入脚本都需要先连接到MySQL数据库，确保数据库服务正常运行。