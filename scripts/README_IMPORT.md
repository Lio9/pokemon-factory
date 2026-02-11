# Python导入脚本使用说明

## 脚本结构

将原来的单一导入脚本拆分为多个独立的脚本，便于调试和维护：

### 核心脚本
- `import_scheduler.py` - 主调度脚本，负责协调各个导入任务
- `ability_import.py` - 特性导入脚本
- `move_import.py` - 技能导入脚本  
- `item_import.py` - 道具导入脚本
- `pokemon_import.py` - 宝可梦导入脚本
- `evolution_import.py` - 进化链导入脚本
- `init_database.py` - 数据库初始化脚本

### 工具脚本
- `utils.py` - 通用工具函数
- `efficient_import_backup.py` - 备份的旧导入脚本

## 使用方法

### 1. 使用Java控制器调用（推荐）

#### 导入所有数据
```bash
POST /api/import-optimized/python-scheduler
```

#### 导入特定类型数据
```bash
POST /api/import-optimized/python-scheduler/abilities
POST /api/import-optimized/python-scheduler/moves  
POST /api/import-optimized/python-scheduler/items
POST /api/import-optimized/python-scheduler/pokemon
POST /api/import-optimized/python-scheduler/evolutions
```

### 2. 直接运行Python脚本

#### 初始化数据库
```bash
python import_scheduler.py --type init
```

#### 清空表
```bash
python import_scheduler.py --type clear
```

#### 导入所有数据
```bash
python import_scheduler.py --type all
```

#### 导入特定类型数据
```bash
python import_scheduler.py --type abilities
python import_scheduler.py --type moves
python import_scheduler.py --type items
python import_scheduler.py --type pokemon
python import_scheduler.py --type evolutions
```

#### 数据库初始化
```bash
python init_database.py --init
```

#### 清空数据库表
```bash
python init_database.py --clear
```

## 日志文件

所有导入操作的日志都保存在 `D:\learn\pokemon-factory\logs\` 目录下：

- `python_scheduler.log` - Python调度脚本主日志
- `python_scheduler_abilities.log` - 特性导入日志
- `python_scheduler_moves.log` - 技能导入日志
- `python_scheduler_items.log` - 道具导入日志
- `python_scheduler_pokemon.log` - Pokemon导入日志
- `python_scheduler_evolutions.log` - 进化链导入日志

## 调试优势

### 1. 独立调试
每个导入脚本可以独立运行和调试，便于定位问题：

```bash
# 只调试特性导入
python ability_import.py

# 只调试技能导入  
python move_import.py
```

### 2. 问题定位
通过查看特定的日志文件，可以快速定位问题：

```bash
# 查看特性导入错误
tail -f D:\learn\pokemon-factory\logs\python_scheduler_abilities.log

# 查看技能导入错误
tail -f D:\learn\pokemon-factory\logs\python_scheduler_moves.log
```

### 3. 部分导入
可以只导入需要的数据类型，节省时间：

```bash
# 只导入特性，快速验证
python import_scheduler.py --type abilities
```

## 配置

数据库配置在 `utils.py` 中的 `get_db_config()` 函数：

```python
def get_db_config():
    return {
        'host': 'localhost',
        'user': 'root',
        'password': '123456',
        'database': 'pokemon_factory',
        'charset': 'utf8mb4'
    }
```

## 注意事项

1. **依赖要求**：确保安装了必要的Python包：
   ```bash
   pip install mysql-connector-python requests
   ```

2. **数据库连接**：确保MySQL服务正在运行，数据库已创建。

3. **权限**：确保有写入 `D:\learn\pokemon-factory\logs\` 目录的权限。

4. **超时设置**：Java控制器中的超时时间默认为120分钟，可在 `application.yml` 中配置。

## 故障排除

### 常见错误

1. **数据库连接失败**
   - 检查数据库服务是否运行
   - 检查用户名和密码是否正确
   - 检查数据库是否已创建

2. **网络连接超时**
   - 检查网络连接
   - 检查PokeAPI是否可访问
   - 增加超时时间配置

3. **权限不足**
   - 检查MySQL用户权限
   - 检查日志目录写入权限

### 调试步骤

1. 查看对应的日志文件
2. 确认数据库连接配置
3. 测试单个脚本运行
4. 检查网络连接状态