# Pokemon Factory

Pokemon Factory 是一个完整的宝可梦图鉴系统，包含数据爬取、存储和后端API等功能。

## 项目结构

```
.
├── pokeDex           # Spring Boot应用
├── scripts           # 数据爬取和处理脚本
├── data              # 爬取的数据文件
└── README.md         # 项目说明文档
```

## 数据库设计

### 表结构优化

为了更好地表示宝可梦数据之间的关系，我们对数据库表结构进行了优化，采用更加规范化的结构：

1. **核心实体表**
   - `pokemon` - 宝可梦基本信息表
   - `pokemon_form` - 宝可梦形态表
   - `ability` - 特性表
   - `move` - 招式表
   - `type` - 属性表
   - `egg_group` - 蛋群表

2. **关联表**
   - `pokemon_form_type` - 宝可梦形态与属性的关联表
   - `pokemon_form_ability` - 宝可梦形态与特性的关联表
   - `pokemon_egg_group` - 宝可梦与蛋群的关联表
   - `pokemon_move` - 宝可梦与招式的关联表
   - `evolution_chain` - 进化链表

### 数据库脚本

在 `pokeDex/src/main/resources/db` 目录下提供了以下脚本：

- `schema.sql` - 原始表结构
- `schema_optimized.sql` - 优化后的表结构
- `init_data.sql` - 清空数据脚本
- `init_basic_data.sql` - 初始化基础数据（属性、蛋群）
- `upgrade_schema.sql` - 升级现有数据库结构的脚本

## 数据爬取

### 爬虫脚本

在 `scripts` 目录下提供了以下爬虫脚本：

1. `setup_data_dirs.py` - 创建数据目录结构
2. `pokemon.py` - 爬取宝可梦详细信息
3. `ability.py` - 爬取特性详细信息
4. `move.py` - 爬取招式详细信息
5. `type.py` - 生成属性数据
6. `egg_group.py` - 生成蛋群数据
7. `import_data.py` - 将JSON数据导入数据库

### 数据处理流程

1. **设置环境**
   ```
   python scripts/setup_data_dirs.py
   ```

2. **爬取数据**
   ```
   # 爬取宝可梦数据
   python scripts/pokemon.py
   
   # 爬取特性数据
   python scripts/ability.py
   
   # 爬取招式数据
   python scripts/move.py
   
   # 生成属性和蛋群数据
   python scripts/type.py
   python scripts/egg_group.py
   ```

3. **导入数据库**
   ```
   python scripts/import_data.py
   ```

## 后端API

后端使用Spring Boot构建RESTful API，主要功能包括：

- 宝可梦列表查询（支持分页和搜索）
- 宝可梦详情查询
- 宝可梦形态信息查询
- 宝可梦技能信息查询
- 宝可梦特性信息查询
- 宝可梦进化链查询

API文档可通过Swagger访问：http://localhost:8080/swagger-ui.html

## 部署运行

### 后端部署

```
cd pokeDex
mvn spring-boot:run
```

## 项目特点

1. **数据完整性** - 通过规范化数据库设计确保数据一致性
2. **高性能** - 使用MyBatis Plus提升数据库访问性能
3. **易扩展** - 模块化设计便于功能扩展
4. **完整功能** - 涵盖宝可梦图鉴的核心功能