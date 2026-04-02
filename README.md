# Pokemon Factory 宝可梦工厂

一个高性能的宝可梦数据管理系统，支持从PokeAPI导入宝可梦数据并提供Web界面展示。

## 项目简介

Pokemon Factory 是一个完整的宝可梦数据管理平台，包含：
- **后端服务**：基于Spring Boot的Java API服务
- **前端界面**：基于Vue.js的现代化Web界面
- **数据导入**：高性能的Python数据导入脚本

## 功能特性

### 🚀 高性能导入
- 支持并发导入，大幅提升数据导入速度
- 智能重试机制，提高导入成功率
- 批量处理优化，减少数据库压力

### 📊 完整数据支持
- 宝可梦基础信息（名称、身高、体重等）
- 宝可梦形态和特性
- 类型、技能、道具等完整数据
- 进化链信息

### 🎨 现代化界面
- 响应式设计，支持多设备访问
- 实时数据展示和搜索
- 交互式宝可梦详情查看

## 技术栈

### 后端技术
- **框架**：Spring Boot 2.x
- **数据库**：MySQL
- **缓存**：Redis（可选）
- **API**：RESTful API

### 前端技术
- **框架**：Vue.js 3.x
- **构建工具**：Vite
- **UI框架**：Tailwind CSS
- **状态管理**：Pinia

### 数据导入
- **语言**：Python 3.8+
- **并发**：asyncio + aiohttp
- **数据库**：mysql-connector-python

## 环境要求

### 系统要求
- Windows/Linux/macOS
- Java 8+ (后端)
- Node.js 16+ (前端)
- Python 3.8+ (导入脚本)
- MySQL 5.7+ (数据库)

### 网络要求
- 稳定的互联网连接
- 访问PokeAPI (https://pokeapi.co/)

## 快速开始

### 1. 数据库初始化

```bash
# 连接MySQL并执行初始化脚本
mysql -u root -p < complete_database_init.sql
```

### 2. 配置数据库连接

数据库连接请使用环境变量或 .env（推荐）。示例可以参考仓库根目录的 .env.example 文件。

示例 Python 读取方式：
```python
import os

def get_db_config():
    return {
        'host': os.getenv('DB_HOST', '127.0.0.1'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('DB_NAME', 'pokemon_factory'),
        'charset': 'utf8mb4',
        'connection_timeout': 30,
        'connect_timeout': 30
    }
```

在本地开发时，复制 .env.example 为 .env 并填写具体值；不要将 .env 提交到版本库。
### 3. 启动后端服务

```bash
cd pokemon-factory-backend
mvn spring-boot:run
```

### 4. 启动前端服务

```bash
cd pokemon-factory-frontend
npm install
npm run dev
```

### 5. 导入宝可梦数据

```bash
cd scripts
python pokemon_import.py
```

## 导入脚本使用说明

### 脚本特性

- **智能重试**：最多重试8次，指数退避延迟
- **并发处理**：支持50个并发连接
- **批量导入**：每批200个宝可梦
- **错误处理**：详细的日志记录和错误恢复

### 使用方法

```bash
# 导入所有宝可梦数据
python pokemon_import.py

# 查看导入进度
tail -f logs/efficient_pokemon_import.log
```

### 配置说明

脚本配置在 `pokemon_import.py` 的 `EfficientPokemonImporter` 类中：

```python
self.batch_size = 200      # 批次大小
self.max_concurrent = 50   # 最大并发数
self.max_retries = 8       # 最大重试次数
self.retry_delay = 2       # 基础重试延迟（秒）
```

## API接口

### 导入接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/import-optimized/all-fast` | POST | 快速导入所有数据 |
| `/api/import-optimized/all-async` | POST | 异步导入所有数据 |
| `/api/import-optimized/python-scheduler` | POST | 使用Python调度器导入 |
| `/api/import-optimized/java-import/all` | POST | Java直接导入 |

### 数据查询接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pokemon` | GET | 获取宝可梦列表 |
| `/api/pokemon/{id}` | GET | 获取宝可梦详情 |
| `/api/types` | GET | 获取类型列表 |
| `/api/moves` | GET | 获取技能列表 |

## 性能优化

### 数据库优化

1. **索引优化**：为常用查询字段添加索引
2. **批量操作**：使用批量插入减少数据库交互
3. **连接池**：合理配置数据库连接池大小

### 网络优化

1. **并发控制**：限制并发连接数避免服务器压力
2. **超时设置**：合理设置连接和读取超时
3. **重试策略**：指数退避重试提高成功率

### 缓存策略

1. **数据缓存**：缓存常用数据减少API调用
2. **内存管理**：及时清理不需要的缓存数据
3. **缓存失效**：设置合理的缓存过期时间

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接配置是否正确
   - 确认防火墙设置

2. **导入失败**
   - 检查网络连接
   - 查看日志文件了解具体错误
   - 确认PokeAPI服务可用性

3. **前端无法访问**
   - 确认后端服务正常运行
   - 检查端口配置（默认8080）
   - 验证跨域配置

### 日志查看

```bash
# 查看导入日志
tail -f logs/efficient_pokemon_import.log

# 查看后端日志
tail -f logs/pokemon-factory.log

# 查看前端日志
# 在浏览器控制台查看
```

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 致谢

- [PokeAPI](https://pokeapi.co/) - 宝可梦数据来源
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
- [Vue.js](https://vuejs.org/) - 前端框架
- [Tailwind CSS](https://tailwindcss.com/) - 样式框架

## 联系方式

- 项目维护者：Lio9
- 邮箱：[your-email@example.com]
- GitHub：[https://github.com/Lio9/pokemon-factory](https://github.com/Lio9/pokemon-factory)

---

**注意**：本项目仅供学习和研究使用，请勿用于商业用途。