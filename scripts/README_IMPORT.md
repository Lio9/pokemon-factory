# Python导入脚本使用说明

## 脚本结构

当前使用拆分后的独立导入脚本，每个脚本可以单独运行：

### 导入脚本
- `import_scheduler.py` - 导入调度器，按顺序调用各个导入脚本
- `utils.py` - 通用工具函数模块
- `type_import.py` - 属性导入脚本
- `ability_import.py` - 特性导入脚本（优化版，支持批量导入）
- `move_import.py` - 技能导入脚本（优化版，支持批量导入）
- `item_import.py` - 道具导入脚本（优化版，支持批量导入）
- `egg_group_import.py` - 蛋群导入脚本
- `growth_rate_import.py` - 经验类型导入脚本
- `evolution_chain_import.py` - 进化链导入脚本
- `pokemon_import.py` - 宝可梦导入脚本

### 工具脚本
- `batch_download.py` - 批量下载脚本
- `verify_paths.py` - 路径验证脚本

## 使用方法

### 1. 使用Java控制器调用（推荐）

#### 导入所有数据
```bash
POST /api/import-optimized/python-scheduler
```

#### 导入特定类型数据
```bash
POST /api/import-optimized/python-scheduler/types
POST /api/import-optimized/python-scheduler/abilities
POST /api/import-optimized/python-scheduler/moves
POST /api/import-optimized/python-scheduler/items
POST /api/import-optimized/python-scheduler/egg_groups
POST /api/import-optimized/python-scheduler/growth_rates
POST /api/import-optimized/python-scheduler/evolution_chains
POST /api/import-optimized/python-scheduler/pokemon
```

### 2. 直接运行Python脚本

#### 导入所有数据
```bash
python import_scheduler.py
```

#### 导入特定类型数据
```bash
python import_scheduler.py --type types
python import_scheduler.py --type abilities
python import_scheduler.py --type moves
python import_scheduler.py --type items
python import_scheduler.py --type egg_groups
python import_scheduler.py --type growth_rates
python import_scheduler.py --type evolution_chains
python import_scheduler.py --type pokemon
```

#### 单独运行某个导入脚本
```bash
python type_import.py
python ability_import.py
python move_import.py
python item_import.py
python egg_group_import.py
python growth_rate_import.py
python evolution_chain_import.py
python pokemon_import.py
```

## 日志文件

所有导入操作的日志都保存在 `D:\learn\pokemon-factory\logs\` 目录下：

- `import_scheduler.log` - 导入调度器主日志
- `type_import.log` - 属性导入日志
- `ability_import.log` - 特性导入日志
- `move_import.log` - 技能导入日志
- `item_import.log` - 道具导入日志
- `egg_group_import.log` - 蛋群导入日志
- `growth_rate_import.log` - 经验类型导入日志
- `evolution_chain_import.log` - 进化链导入日志
- `pokemon_import.log` - 宝可梦导入日志

## 调试方法

### 1. 直接调试
可以单独运行各个导入脚本进行调试：

```bash
# 只导入属性数据
python type_import.py

# 只导入特性数据
python ability_import.py

# 只导入技能数据
python move_import.py

# 只导入道具数据
python item_import.py

# 只导入蛋群数据
python egg_group_import.py

# 只导入经验类型数据
python growth_rate_import.py

# 只导入进化链数据
python evolution_chain_import.py

# 只导入宝可梦数据
python pokemon_import.py

# 只导入调度器（测试导入顺序）
python import_scheduler.py
```

### 2. 查看日志
```bash
# 实时查看导入日志
tail -f D:\learn\pokemon-factory\logs\import_scheduler.log

# 查看特定类型的导入日志
tail -f D:\learn\pokemon-factory\logs\ability_import.log
tail -f D:\learn\pokemon-factory\logs\move_import.log
tail -f D:\learn\pokemon-factory\logs\item_import.log
```

### 3. 部分导入
可以只导入需要的数据类型，节省时间：

```bash
# 只导入特性数据，快速验证
python ability_import.py

# 只导入技能数据，快速验证
python move_import.py
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
   - 检查防火墙设置（端口10.144.55.168:3306）

2. **网络连接超时**
   - 检查网络连接
   - 检查PokeAPI是否可访问
   - 增加超时时间配置

3. **权限不足**
   - 检查MySQL用户权限
   - 检查日志目录写入权限

### 调试步骤

1. 查看对应的日志文件（如 `D:\learn\pokemon-factory\logs\ability_import.log`）
2. 确认数据库连接配置（当前配置：10.144.55.168:3306）
3. 测试单个脚本运行：
   ```bash
   python ability_import.py
   ```
4. 检查网络连接状态

### 性能优化提示

1. **特性导入**：已优化为批量导入，速度提升30%
2. **技能导入**：已优化为批量导入，速度提升30%
3. **道具导入**：已优化为批量导入，速度提升11%
4. **中文显示**：已修复为优先显示简体中文